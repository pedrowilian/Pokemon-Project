package backend.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    /**
     * Open a connection anchored to the resolved project-root database path.
     */
    public static Connection connect(String dbFileName) throws SQLException {
        String resolvedPath = DatabasePathResolver.resolve(dbFileName);
        return DriverManager.getConnection("jdbc:sqlite:" + resolvedPath);
    }
}
