package ntm.dao;

import java.sql.SQLException;

/**
 * Generic interface for the basic database operations (Create, Update, Delete).
 * T is the type of object stored, e.g. CrudDAO&lt;Device&gt;.
 */
public interface CrudDAO<T> {

    /** Inserts the item and sets its new id. */
    T save(T item) throws SQLException;

    void update(T item) throws SQLException;

    void delete(int id) throws SQLException;
}
