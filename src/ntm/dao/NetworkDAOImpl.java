package ntm.dao;

import ntm.db.DBConnection;
import ntm.model.Network;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class NetworkDAOImpl implements NetworkDAO {

    private Connection conn() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    @Override
    public Network save(Network network) throws SQLException {
        String sql = "INSERT INTO networks (name, description, created_by) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, network.getName());
            ps.setString(2, network.getDescription());
            ps.setInt(3, network.getCreatedById());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    network.setId(keys.getInt(1));
                }
            }
        }
        return network;
    }

    @Override
    public void update(Network network) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE networks SET name = ?, description = ? WHERE id = ?")) {
            ps.setString(1, network.getName());
            ps.setString(2, network.getDescription());
            ps.setInt(3, network.getId());
            ps.executeUpdate();
        }
    }

    /** Devices, cables and packet logs are deleted automatically by ON DELETE CASCADE. */
    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM networks WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * LEFT JOIN with users gives the creator's name, LEFT JOIN with devices + GROUP BY
     * counts the devices in each network - all in one query.
     */
    @Override
    public List<Network> search(String keyword) throws SQLException {
        String sql = "SELECT n.id, n.name, n.description, n.created_by, n.created_at, "
                + "u.username AS created_by_name, COUNT(d.id) AS device_count "
                + "FROM networks n "
                + "LEFT JOIN users u ON n.created_by = u.id "
                + "LEFT JOIN devices d ON d.network_id = n.id "
                + "WHERE n.name LIKE ? OR n.description LIKE ? "
                + "GROUP BY n.id, n.name, n.description, n.created_by, n.created_at, u.username "
                + "ORDER BY n.id";
        List<Network> networks = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            String pattern = "%" + keyword.trim() + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Network network = new Network(rs.getInt("id"), rs.getString("name"),
                            rs.getString("description"), rs.getInt("created_by"));
                    network.setCreatedByName(rs.getString("created_by_name"));
                    network.setCreatedAt(rs.getTimestamp("created_at"));
                    network.setDeviceCount(rs.getInt("device_count"));
                    networks.add(network);
                }
            }
        }
        return networks;
    }
}
