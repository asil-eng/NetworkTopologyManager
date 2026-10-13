package ntm.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton pattern: the whole application shares ONE database connection.
 * Settings are read from db.properties in the project folder.
 */
public final class DBConnection {

    private static final String CONFIG_FILE = "db.properties";
    private static DBConnection instance;

    private final String url;
    private final String username;
    private final String password;
    private Connection connection;

    private DBConnection() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("Could not read " + CONFIG_FILE + ", using default settings.");
        }
        url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/network_manager");
        username = props.getProperty("db.username", "root");
        password = props.getProperty("db.password", "");
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /** Opens the connection the first time, and reopens it if it was lost. */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || !connection.isValid(2)) {
            connection = DriverManager.getConnection(url, username, password);
        }
        return connection;
    }
}
