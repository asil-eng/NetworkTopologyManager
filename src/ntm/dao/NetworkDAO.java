package ntm.dao;

import ntm.model.Network;

import java.sql.SQLException;
import java.util.List;

public interface NetworkDAO extends CrudDAO<Network> {

    /** Networks whose name or description contains the keyword ("" returns all). */
    List<Network> search(String keyword) throws SQLException;
}
