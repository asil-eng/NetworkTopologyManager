package ntm.dao;

import ntm.db.DBConnection;
import ntm.model.Device;
import ntm.model.Link;
import ntm.model.Network;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LinkDAOImpl implements LinkDAO {

    private Connection conn() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    @Override
    public Link save(Link link) throws SQLException {
        String sql = "INSERT INTO connections (network_id, device_a, device_b, latency_ms) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, link.getNetworkId());
            ps.setInt(2, link.getDeviceA().getId());
            ps.setInt(3, link.getDeviceB().getId());
            ps.setInt(4, link.getLatencyMs());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    link.setId(keys.getInt(1));
                }
            }
        }
        return link;
    }

    @Override
    public void update(Link link) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("UPDATE connections SET latency_ms = ? WHERE id = ?")) {
            ps.setInt(1, link.getLatencyMs());
            ps.setInt(2, link.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM connections WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Link> findByNetwork(Network network) throws SQLException {
        String sql = "SELECT id, device_a, device_b, latency_ms FROM connections WHERE network_id = ? ORDER BY id";
        List<Link> links = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, network.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Use the SAME Device objects that are already in the network.
                    Device a = network.findById(rs.getInt("device_a"));
                    Device b = network.findById(rs.getInt("device_b"));
                    if (a != null && b != null) {
                        links.add(new Link(rs.getInt("id"), network.getId(), a, b, rs.getInt("latency_ms")));
                    }
                }
            }
        }
        return links;
    }
}
