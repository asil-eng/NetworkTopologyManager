package ntm.dao;

import ntm.model.PacketLog;

import java.sql.SQLException;
import java.util.List;

public interface PacketLogDAO {

    void save(PacketLog log) throws SQLException;

    List<PacketLog> findByNetwork(int networkId) throws SQLException;

    void deleteByNetwork(int networkId) throws SQLException;
}
