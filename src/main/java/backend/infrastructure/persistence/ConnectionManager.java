package backend.infrastructure.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Manages database connections with HikariCP connection pooling
 * Provides efficient connection pooling for repositories
 * Centralized connection management to keep frontend clean
 * 
 * Performance improvements:
 * - True connection pooling (10 connections per database)
 * - Connection timeout and lifecycle management
 * - Thread-safe concurrent access
 * - Automatic connection health checks
 */
public class ConnectionManager {
    private static final Logger LOGGER = Logger.getLogger(ConnectionManager.class.getName());
    private static final String POKEDEX_DB = "pokedex.db";
    private static final String USUARIOS_DB = "Usuarios.db";

    // Connection pool configuration
    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_IDLE = 2;
    private static final long CONNECTION_TIMEOUT_MS = 5000; // 5 seconds
    private static final long IDLE_TIMEOUT_MS = 600000; // 10 minutes
    private static final long MAX_LIFETIME_MS = 1800000; // 30 minutes

    private static ConnectionManager instance;

    private final Map<String, HikariDataSource> dataSources;

    private ConnectionManager() {
        this.dataSources = new HashMap<>();
        LOGGER.log(Level.INFO, "ConnectionManager initialized with HikariCP pooling");
    }

    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    /**
     * Creates a HikariCP DataSource for the specified database
     */
    private HikariDataSource createDataSource(String dbName) {
        HikariConfig config = new HikariConfig();
        
        // SQLite JDBC URL
        config.setJdbcUrl("jdbc:sqlite:" + dbName);
        config.setDriverClassName("org.sqlite.JDBC");
        
        // Pool configuration
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE);
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        config.setIdleTimeout(IDLE_TIMEOUT_MS);
        config.setMaxLifetime(MAX_LIFETIME_MS);
        
        // Performance tuning
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        // Pool name for monitoring
        config.setPoolName("SQLitePool-" + dbName);
        
        LOGGER.log(Level.INFO, "Created HikariCP pool for: {0} (max={1}, min={2})", 
                   new Object[]{dbName, MAX_POOL_SIZE, MIN_IDLE});
        
        return new HikariDataSource(config);
    }

    /**
     * Get a connection from the pool for a database
     * Automatically creates pool on first access
     */
    public Connection getConnection(String dbName) throws SQLException {
        if (!dataSources.containsKey(dbName)) {
            synchronized (this) {
                // Double-check locking
                if (!dataSources.containsKey(dbName)) {
                    dataSources.put(dbName, createDataSource(dbName));
                }
            }
        }
        
        return dataSources.get(dbName).getConnection();
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
     * Close all connection pools
     */
    public void closeAll() {
        for (Map.Entry<String, HikariDataSource> entry : dataSources.entrySet()) {
            try {
                entry.getValue().close();
                LOGGER.log(Level.INFO, "Closed connection pool for: {0}", entry.getKey());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing pool for: " + entry.getKey(), e);
            }
        }
        dataSources.clear();
    }

    /**
     * Get pool statistics for monitoring (optional)
     */
    public String getPoolStats(String dbName) {
        HikariDataSource ds = dataSources.get(dbName);
        if (ds != null) {
            return String.format("Pool: %s | Active: %d | Idle: %d | Total: %d",
                               dbName,
                               ds.getHikariPoolMXBean().getActiveConnections(),
                               ds.getHikariPoolMXBean().getIdleConnections(),
                               ds.getHikariPoolMXBean().getTotalConnections());
        }
        return "Pool not initialized: " + dbName;
    }
}
