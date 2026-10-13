package ntm.dao;

import ntm.db.DBConnection;
import ntm.model.PacketLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PacketLogDAOImpl implements PacketLogDAO {

    private Connection conn() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    @Override
    public void save(PacketLog log) throws SQLException {
        String sql = "INSERT INTO packet_logs (network_id, source_id, destination_id, path, algorithm, hops, "
                + "total_latency, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, log.getNetworkId());
            ps.setInt(2, log.getSourceId());
            ps.setInt(3, log.getDestinationId());
            ps.setString(4, log.getPath());
            ps.setString(5, log.getAlgorithm());
            ps.setInt(6, log.getHops());
            ps.setInt(7, log.getTotalLatency());
            ps.setString(8, log.getStatus());
            ps.executeUpdate();
        }
    }

    /** Two LEFT JOINs with devices turn the source and destination ids into names. */
    @Override
    public List<PacketLog> findByNetwork(int networkId) throws SQLException {
        String sql = "SELECT p.id, p.network_id, p.source_id, p.destination_id, p.path, p.algorithm, p.hops, "
                + "p.total_latency, p.status, p.sent_at, s.name AS source_name, d.name AS destination_name "
                + "FROM packet_logs p "
                + "LEFT JOIN devices s ON p.source_id = s.id "
                + "LEFT JOIN devices d ON p.destination_id = d.id "
                + "WHERE p.network_id = ? "
                + "ORDER BY p.sent_at DESC, p.id DESC";
        List<PacketLog> logs = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, networkId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new PacketLog(rs.getInt("id"), rs.getInt("network_id"), rs.getInt("source_id"),
                            rs.getInt("destination_id"), rs.getString("source_name"),
                            rs.getString("destination_name"), rs.getString("path"), rs.getString("algorithm"),
                            rs.getInt("hops"), rs.getInt("total_latency"), rs.getString("status"),
                            rs.getTimestamp("sent_at")));
                }
            }
        }
        return logs;
    }

    @Override
    public void deleteByNetwork(int networkId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM packet_logs WHERE network_id = ?")) {
            ps.setInt(1, networkId);
            ps.executeUpdate();
        }
    }
}
