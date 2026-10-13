package ntm.dao;

import ntm.db.DBConnection;
import ntm.model.DeviceType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only queries for the Reports window: GROUP BY, COUNT, SUM, AVG and JOINs.
 */
public class ReportDAO {

    private Connection conn() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    /** Number of devices of each type. Types with no devices are shown as 0. */
    public Map<String, Integer> countDevicesByType(int networkId) throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (DeviceType type : DeviceType.values()) {
            counts.put(type.getLabel(), 0);
        }
        String sql = "SELECT type, COUNT(*) AS total FROM devices WHERE network_id = ? GROUP BY type";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, networkId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(DeviceType.valueOf(rs.getString("type")).getLabel(), rs.getInt("total"));
                }
            }
        }
        return counts;
    }

    /** Devices with the most cables: {name, type, ip, cable count}. */
    public List<Object[]> mostConnectedDevices(int networkId, int limit) throws SQLException {
        String sql = "SELECT d.name, d.type, d.ip_address, COUNT(c.id) AS cables "
                + "FROM devices d "
                + "LEFT JOIN connections c ON c.device_a = d.id OR c.device_b = d.id "
                + "WHERE d.network_id = ? "
                + "GROUP BY d.id, d.name, d.type, d.ip_address "
                + "ORDER BY cables DESC, d.name "
                + "LIMIT ?";
        List<Object[]> rows = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, networkId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{rs.getString("name"), DeviceType.valueOf(rs.getString("type")).getLabel(),
                            rs.getString("ip_address"), rs.getInt("cables")});
                }
            }
        }
        return rows;
    }

    /** Summary of the packet history: label -> value. */
    public Map<String, String> packetStatistics(int networkId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total, "
                + "SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) AS delivered, "
                + "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed, "
                + "AVG(CASE WHEN status = 'DELIVERED' THEN hops END) AS avg_hops, "
                + "AVG(CASE WHEN status = 'DELIVERED' THEN total_latency END) AS avg_latency "
                + "FROM packet_logs WHERE network_id = ?";
        Map<String, String> stats = new LinkedHashMap<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, networkId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int delivered = rs.getInt("delivered");
                    stats.put("Packets sent", String.valueOf(total));
                    stats.put("Delivered", String.valueOf(delivered));
                    stats.put("Failed", String.valueOf(rs.getInt("failed")));
                    stats.put("Success rate", total == 0 ? "-" : String.format("%.1f %%", delivered * 100.0 / total));
                    stats.put("Average hops", delivered == 0 ? "-" : String.format("%.1f", rs.getDouble("avg_hops")));
                    stats.put("Average latency", delivered == 0 ? "-" : String.format("%.1f ms", rs.getDouble("avg_latency")));
                }
            }
        }
        return stats;
    }
}
