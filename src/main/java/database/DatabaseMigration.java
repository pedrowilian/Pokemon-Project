package database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe responsável por atualizar o esquema do banco de dados
 * Adiciona as novas colunas necessárias para manipulação de senhas e datas
 */
public class DatabaseMigration {
    private static final Logger LOGGER = Logger.getLogger(DatabaseMigration.class.getName());

    /**
     * Executa as migrações necessárias no banco de dados de usuários
     * @param conn Conexão com o banco de dados
     */
    public static void migrateUsersDatabase(Connection conn) {
        if (conn == null) {
            LOGGER.log(Level.SEVERE, "Conexão nula fornecida para migração");
            return;
        }

        try {
            // Adiciona coluna de último login
            addColumnIfNotExists(conn, "usuarios", "ultimo_login", "TEXT");
            LOGGER.log(Level.INFO, "Coluna 'ultimo_login' verificada/adicionada");

            // Adiciona coluna de data de criação da conta
            addColumnIfNotExists(conn, "usuarios", "data_criacao", "TEXT");
            LOGGER.log(Level.INFO, "Coluna 'data_criacao' verificada/adicionada");

            LOGGER.log(Level.INFO, "Migração do banco de dados concluída com sucesso");
            LOGGER.log(Level.INFO, "CryptoDummy será usado para criptografia de senhas com chaves individuais");

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao executar migrações do banco de dados", ex);
        }
    }

    /**
     * Adiciona uma coluna à tabela se ela não existir
     * @param conn Conexão com o banco de dados
     * @param tableName Nome da tabela
     * @param columnName Nome da coluna
     * @param columnType Tipo da coluna (ex: TEXT, INTEGER)
     * @throws SQLException se houver erro na operação
     */
    private static void addColumnIfNotExists(Connection conn, String tableName, String columnName, String columnType) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Tenta adicionar a coluna - se já existir, SQLite retornará erro que ignoramos
            String sql = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnType);
            stmt.executeUpdate(sql);
            LOGGER.log(Level.INFO, String.format("Coluna %s adicionada à tabela %s", columnName, tableName));
        } catch (SQLException ex) {
            // Se a coluna já existe, ignora o erro
            if (ex.getMessage().contains("duplicate column name")) {
                LOGGER.log(Level.FINE, String.format("Coluna %s já existe na tabela %s", columnName, tableName));
            } else {
                throw ex;
            }
        }
    }

    /**
     * Verifica se o banco de dados precisa de migração
     * @param conn Conexão com o banco de dados
     * @return true se a migração é necessária
     */
    public static boolean needsMigration(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Tenta selecionar as novas colunas
            stmt.executeQuery("SELECT ultimo_login, data_criacao FROM usuarios LIMIT 1");
            return false; // Se não houver erro, as colunas existem
        } catch (SQLException ex) {
            return true; // Se houver erro, precisa migrar
        }
    }
}
