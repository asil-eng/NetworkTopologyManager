package ntm.dao;

import ntm.model.Link;
import ntm.model.Network;

import java.sql.SQLException;
import java.util.List;

public interface LinkDAO extends CrudDAO<Link> {

    /** Loads the cables of a network. Its devices must already be loaded. */
    List<Link> findByNetwork(Network network) throws SQLException;
}
