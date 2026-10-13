package ntm.dao;

import ntm.model.User;

import java.sql.SQLException;
import java.util.List;

public interface UserDAO {

    /** Returns the user if the username and password match, otherwise null. */
    User authenticate(String username, String password) throws SQLException;

    List<User> findAll() throws SQLException;

    void create(String username, String password, String role) throws SQLException;

    void delete(int id) throws SQLException;
}
