package ntm.dao;

import ntm.model.Device;

import java.sql.SQLException;
import java.util.List;

public interface DeviceDAO extends CrudDAO<Device> {

    List<Device> findByNetwork(int networkId) throws SQLException;

    /** Devices whose name, IP address or type contains the keyword. */
    List<Device> search(int networkId, String keyword) throws SQLException;

    void updatePosition(Device device) throws SQLException;

    void updateStatus(Device device) throws SQLException;
}
