package backend.infrastructure.persistence;

import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages database connections
 * Provides connection pooling for repositories
 * Centralized connection management to keep frontend clean
 */
public class ConnectionManager {
    private static final Logger LOGGER = Logger.getLogger(ConnectionManager.class.getName());
    private static final String POKEDEX_DB = "pokedex.db";
    private static final String USUARIOS_DB = "Usuarios.db";

    private static ConnectionManager instance;

    private final Map<String, Connection> connections;

    private ConnectionManager() {
        this.connections = new HashMap<>();
    }

    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    /**
     * Get or create a connection to a database
     */
    public Connection getConnection(String dbName) throws SQLException {
        if (!connections.containsKey(dbName) || connections.get(dbName).isClosed()) {
            Connection conn = DatabaseConnection.connect(dbName);
            connections.put(dbName, conn);
            LOGGER.log(Level.INFO, "Created new connection to: " + dbName);
        }
        return connections.get(dbName);
    }

    /**
     * Get connection to Pokedex database
     */
    public Connection getPokedexConnection() throws SQLException {
        return getConnection(POKEDEX_DB);
    }

    /**
     * Get connection to Users database
     */
    public Connection getUsersConnection() throws SQLException {
        return getConnection(USUARIOS_DB);
    }

    /**
     * Close all connections
     */
    public void closeAll() {
        for (Map.Entry<String, Connection> entry : connections.entrySet()) {
            try {
                if (!entry.getValue().isClosed()) {
                    entry.getValue().close();
                    LOGGER.log(Level.INFO, "Closed connection to: " + entry.getKey());
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection to: " + entry.getKey(), e);
            }
        }
        connections.clear();
    }
}
