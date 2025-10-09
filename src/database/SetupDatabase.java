package database;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SetupDatabase {
    private static final Logger LOGGER = Logger.getLogger(SetupDatabase.class.getName());

    public static void setupDatabases(Connection pokedexConn, Connection usuariosConn) {
        if (pokedexConn != null) {
            createPokedexTable(pokedexConn);
        }
        if (usuariosConn != null) {
            createUsuariosTable(usuariosConn);
        }
    }

    private static void createPokedexTable(Connection conn) {
        String sql = """
            CREATE TABLE IF NOT EXISTS pokedex (
                ID INTEGER PRIMARY KEY,
                Name TEXT NOT NULL,
                Form TEXT,
                Type1 TEXT,
                Type2 TEXT,
                Total INTEGER,
                HP INTEGER,
                Attack INTEGER,
                Defense INTEGER,
                SpAtk INTEGER,
                SpDef INTEGER,
                Speed INTEGER,
                Generation INTEGER
            );
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            LOGGER.info("Tabela 'pokedex' criada com sucesso.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao criar tabela 'pokedex': " + e.getMessage(), e);
        }
    }

    private static void createUsuariosTable(Connection conn) {
        String sql = """
            CREATE TABLE IF NOT EXISTS usuarios (
                nome TEXT PRIMARY KEY,
                senha TEXT NOT NULL,
                admin INTEGER NOT NULL DEFAULT 0
            );
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            LOGGER.info("Tabela 'usuarios' criada com sucesso.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao criar tabela 'usuarios': " + e.getMessage(), e);
        }
    }
}