package ntm.dao;

import ntm.db.DBConnection;
import ntm.exception.InvalidIPException;
import ntm.model.Device;
import ntm.model.DeviceFactory;
import ntm.model.DeviceType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DeviceDAOImpl implements DeviceDAO {

    private static final String SELECT = "SELECT id, network_id, name, type, ip_address, x, y, online FROM devices ";

    private Connection conn() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    /** Turns one database row into the correct Device subclass using the factory. */
    private Device map(ResultSet rs) throws SQLException {
        try {
            Device device = DeviceFactory.create(DeviceType.valueOf(rs.getString("type")), rs.getInt("id"),
                    rs.getInt("network_id"), rs.getString("name"), rs.getString("ip_address"),
                    rs.getInt("x"), rs.getInt("y"));
            device.setOnline(rs.getBoolean("online"));
            return device;
        } catch (InvalidIPException e) {
            throw new SQLException("Device " + rs.getString("name") + " has an invalid IP address in the database.", e);
        }
    }

    private List<Device> mapAll(PreparedStatement ps) throws SQLException {
        List<Device> devices = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                devices.add(map(rs));
            }
        }
        return devices;
    }

    @Override
    public Device save(Device device) throws SQLException {
        String sql = "INSERT INTO devices (network_id, name, type, ip_address, x, y, online) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, device.getNetworkId());
            ps.setString(2, device.getName());
            ps.setString(3, device.getType().name());
            ps.setString(4, device.getIpAddress());
            ps.setInt(5, device.getX());
            ps.setInt(6, device.getY());
            ps.setBoolean(7, device.isOnline());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    device.setId(keys.getInt(1));
                }
            }
        }
        return device;
    }

    @Override
    public void update(Device device) throws SQLException {
        String sql = "UPDATE devices SET name = ?, ip_address = ?, x = ?, y = ?, online = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, device.getName());
            ps.setString(2, device.getIpAddress());
            ps.setInt(3, device.getX());
            ps.setInt(4, device.getY());
            ps.setBoolean(5, device.isOnline());
            ps.setInt(6, device.getId());
            ps.executeUpdate();
        }
    }

    /** Its cables are deleted automatically by ON DELETE CASCADE. */
    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM devices WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Device> findByNetwork(int networkId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(SELECT + "WHERE network_id = ? ORDER BY id")) {
            ps.setInt(1, networkId);
            return mapAll(ps);
        }
    }

    @Override
    public List<Device> search(int networkId, String keyword) throws SQLException {
        String sql = SELECT + "WHERE network_id = ? AND (name LIKE ? OR ip_address LIKE ? OR type LIKE ?) ORDER BY name";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            String pattern = "%" + keyword.trim() + "%";
            ps.setInt(1, networkId);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            return mapAll(ps);
        }
    }

    @Override
    public void updatePosition(Device device) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("UPDATE devices SET x = ?, y = ? WHERE id = ?")) {
            ps.setInt(1, device.getX());
            ps.setInt(2, device.getY());
            ps.setInt(3, device.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateStatus(Device device) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("UPDATE devices SET online = ? WHERE id = ?")) {
            ps.setBoolean(1, device.isOnline());
            ps.setInt(2, device.getId());
            ps.executeUpdate();
        }
    }
}
