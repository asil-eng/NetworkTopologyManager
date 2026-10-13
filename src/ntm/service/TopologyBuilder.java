package ntm.service;

import ntm.dao.DeviceDAO;
import ntm.dao.LinkDAO;
import ntm.db.DBConnection;
import ntm.exception.InvalidIPException;
import ntm.model.Device;
import ntm.model.DeviceFactory;
import ntm.model.Link;
import ntm.model.Network;
import ntm.topology.TopologyPlan;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a TopologyPlan into real devices and cables, saved to the database.
 *
 * Everything is saved in ONE transaction: if anything fails half-way, the
 * database is rolled back and nothing is left half-built.
 */
public class TopologyBuilder {

    private final DeviceDAO deviceDAO;
    private final LinkDAO linkDAO;

    public TopologyBuilder(DeviceDAO deviceDAO, LinkDAO linkDAO) {
        this.deviceDAO = deviceDAO;
        this.linkDAO = linkDAO;
    }

    /** Creates the plan's devices with their top-left corner at (originX, originY). */
    public List<Device> build(Network network, TopologyPlan plan, int originX, int originY)
            throws SQLException, InvalidIPException {
        Connection conn = DBConnection.getInstance().getConnection();
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        List<Device> created = new ArrayList<>();
        try {
            for (TopologyPlan.Node node : plan.getNodes()) {
                Device device = DeviceFactory.create(node.type(), 0, network.getId(),
                        network.suggestName(node.type()), network.suggestIp(),
                        originX + node.x(), originY + node.y());
                deviceDAO.save(device);
                network.addDevice(device);
                created.add(device);
            }
            for (TopologyPlan.Edge edge : plan.getEdges()) {
                Device a = created.get(edge.from());
                Device b = created.get(edge.to());
                network.validateNewLink(a, b);
                Link link = new Link(0, network.getId(), a, b, edge.latencyMs());
                linkDAO.save(link);
                network.addLink(link);
            }
            conn.commit();
            return created;
        } catch (SQLException | InvalidIPException | RuntimeException ex) {
            conn.rollback();
            for (Device device : created) {
                network.removeDevice(device); // also removes their cables
            }
            throw ex;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }
}
