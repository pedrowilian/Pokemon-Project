package backend.infrastructure.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves database file locations to keep them anchored at the project root.
 */
public final class DatabasePathResolver {
    private static final Logger LOGGER = Logger.getLogger(DatabasePathResolver.class.getName());

    private DatabasePathResolver() {
        // Utility class
    }

    public static String resolve(String dbFileName) {
        Path projectRoot = locateProjectRoot(Paths.get("").toAbsolutePath());
        Path dbPath = projectRoot.resolve(dbFileName).normalize();
        createParentDirectory(dbPath);
        return dbPath.toString();
    }

    private static Path locateProjectRoot(Path start) {
        Path current = start;
        while (current != null) {
            if (Files.exists(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        return start;
    }

    private static void createParentDirectory(Path dbPath) {
        Path parent = dbPath.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not create directories for DB path: " + dbPath, e);
        }
    }
}
