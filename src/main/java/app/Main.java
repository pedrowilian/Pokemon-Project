package app;
import javax.swing.SwingUtilities;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import GUI.LoginFrame;
import database.DatabaseConnection;
import database.DatabaseMigration;

public class Main{
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Executa migração do banco de dados antes de abrir a interface
        try (Connection conn = DatabaseConnection.connect("Usuarios.db")) {
            LOGGER.log(Level.INFO, "Verificando necessidade de migração do banco de dados...");
            if (DatabaseMigration.needsMigration(conn)) {
                LOGGER.log(Level.INFO, "Executando migração do banco de dados...");
                DatabaseMigration.migrateUsersDatabase(conn);
            } else {
                LOGGER.log(Level.INFO, "Banco de dados já está atualizado");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao executar migração do banco de dados", ex);
        }

        // Inicia a interface gráfica
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
