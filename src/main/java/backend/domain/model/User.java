package backend.domain.model;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import shared.util.CryptoDummy;
import shared.util.DateUtils;

public class User {
    private static final Logger LOGGER = Logger.getLogger(User.class.getName());
    private final String username;
    @SuppressWarnings("unused")
    private final String password;
    private final boolean isAdmin;
    private LocalDateTime lastLogin;
    private LocalDateTime accountCreated;

    public User(String username, String password, boolean isAdmin) {
        this.username = username;
        this.password = password;
        this.isAdmin = isAdmin;
    }

    public User(String username, String password, boolean isAdmin, LocalDateTime lastLogin, LocalDateTime accountCreated) {
        this.username = username;
        this.password = password;
        this.isAdmin = isAdmin;
        this.lastLogin = lastLogin;
        this.accountCreated = accountCreated;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public LocalDateTime getAccountCreated() {
        return accountCreated;
    }

    public String getLastLoginFormatted() {
        return lastLogin != null ? DateUtils.formatDateTimeBR(lastLogin) : "Nunca";
    }

    public String getAccountCreatedFormatted() {
        return accountCreated != null ? DateUtils.formatDateBR(accountCreated.toLocalDate()) : "Desconhecido";
    }

    public String getTimeSinceLastLogin() {
        return lastLogin != null ? DateUtils.getTimeAgo(lastLogin) : "nunca";
    }

    public static boolean validateUsername(String username) {
        return username != null && !username.trim().isEmpty() && username.trim().length() >= 3;
    }

    public static boolean validatePassword(String password, boolean isNewUser) {
        if (password == null) return false;
        if (isNewUser) {
            return !password.trim().isEmpty() && password.trim().length() >= 6;
        }
        return password.trim().isEmpty() || password.trim().length() >= 6;
    }

    public static boolean authenticate(Connection conn, String username, String password) throws SQLException {
        if (conn == null) {
            throw new SQLException("Sem conexão com o banco de dados.");
        }
        if (!validateUsername(username) || password.isEmpty()) {
            return false;
        }

        // Busca senha do banco
        String sql = "SELECT senha FROM usuarios WHERE nome = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Primeiro tenta como string (senha antiga em texto plano)
                    String storedPasswordText = rs.getString("senha");

                    // Se a senha for uma string normal (não bytes), é senha antiga
                    File keyFile = getCryptoKeyFile(username);
                    if (!keyFile.exists()) {
                        // Não tem chave = usuário antigo com senha em texto plano
                        if (storedPasswordText != null && storedPasswordText.equals(password)) {
                            // Atualiza para senha criptografada
                            LOGGER.log(Level.INFO, "Migrando senha antiga para CryptoDummy: {0}", username);
                            updatePasswordWithEncryption(conn, username, password);
                            updateLastLogin(conn, username);
                            return true;
                        }
                        return false;
                    }

                    // Tem chave = tenta decifrar como senha criptografada
                    byte[] storedPasswordBytes = rs.getBytes("senha");
                    if (storedPasswordBytes != null && storedPasswordBytes.length > 0) {
                        try {
                            CryptoDummy crypto = new CryptoDummy();
                            crypto.geraDecifra(storedPasswordBytes, keyFile);
                            byte[] decryptedPassword = crypto.getTextoDecifrado();
                            String decryptedPasswordStr = new String(decryptedPassword);

                            boolean authenticated = decryptedPasswordStr.equals(password);
                            if (authenticated) {
                                updateLastLogin(conn, username);
                            }
                            return authenticated;
                        } catch (Exception ex) {
                            LOGGER.log(Level.WARNING, "Erro ao decifrar senha, tentando como texto plano", ex);
                            // Se falhar, tenta como texto plano (fallback)
                            if (storedPasswordText != null && storedPasswordText.equals(password)) {
                                updatePasswordWithEncryption(conn, username, password);
                                updateLastLogin(conn, username);
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao autenticar usuário", ex);
            throw ex;
        }
    }

    /**
     * Atualiza o último login do usuário
     */
    private static void updateLastLogin(Connection conn, String username) {
        String sql = "UPDATE usuarios SET ultimo_login = ? WHERE nome = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Erro ao atualizar último login", ex);
        }
    }

    /**
     * Retorna o arquivo de chave do CryptoDummy para um usuário
     */
    private static File getCryptoKeyFile(String username) {
        File keysDir = new File("keys");
        if (!keysDir.exists()) {
            keysDir.mkdirs();
        }
        return new File(keysDir, username + ".key");
    }

    /**
     * Atualiza senha antiga para formato criptografado
     */
    private static void updatePasswordWithEncryption(Connection conn, String username, String password) {
        try {
            File keyFile = getCryptoKeyFile(username);

            // Gera chave CryptoDummy se não existir
            if (!keyFile.exists()) {
                CryptoDummy crypto = new CryptoDummy();
                crypto.geraChave(keyFile);
            }

            // Criptografa a senha
            CryptoDummy crypto = new CryptoDummy();
            crypto.geraCifra(password.getBytes(), keyFile);
            byte[] encryptedPassword = crypto.getTextoCifrado();

            String sql = "UPDATE usuarios SET senha = ? WHERE nome = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBytes(1, encryptedPassword);
                ps.setString(2, username);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Erro ao atualizar senha com criptografia", ex);
        }
    }

    public static void register(Connection conn, String username, String password, String confirmPassword, boolean isAdmin) throws SQLException {
        if (conn == null) {
            throw new SQLException("Sem conexão com o banco de dados.");
        }
        if (!validateUsername(username)) {
            throw new SQLException("O nome de usuário deve ter pelo menos 3 caracteres.");
        }
        if (!validatePassword(password, true)) {
            throw new SQLException("A senha deve ter pelo menos 6 caracteres.");
        }
        if (!password.equals(confirmPassword)) {
            throw new SQLException("As senhas não coincidem.");
        }

        // IMPORTANTE: Verifica se usuário já existe ANTES de gerar chave
        String checkSql = "SELECT COUNT(*) FROM usuarios WHERE nome = ?";
        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, username);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Este nome de usuário já está em uso.");
                }
            }
        }

        // Apenas gera chave depois de confirmar que usuário não existe
        File keyFile = getCryptoKeyFile(username);

        try {
            // Gera chave e criptografa a senha usando CryptoDummy
            CryptoDummy crypto = new CryptoDummy();
            crypto.geraChave(keyFile);
            crypto.geraCifra(password.getBytes(), keyFile);
            byte[] encryptedPassword = crypto.getTextoCifrado();

            String sql = "INSERT INTO usuarios (nome, senha, admin, data_criacao) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setBytes(2, encryptedPassword);
                ps.setInt(3, isAdmin ? 1 : 0);
                ps.setString(4, LocalDateTime.now().toString());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            // Se falhou ao inserir, deleta a chave criada
            if (keyFile.exists()) {
                keyFile.delete();
                LOGGER.log(Level.WARNING, "Chave deletada após falha no registro");
            }

            if (ex.getMessage().contains("UNIQUE constraint failed")) {
                throw new SQLException("Este nome de usuário já está em uso.");
            }
            LOGGER.log(Level.SEVERE, "Erro ao registrar usuário", ex);
            throw ex;
        } catch (Exception ex) {
            // Se falhou na criptografia, deleta a chave criada
            if (keyFile.exists()) {
                keyFile.delete();
                LOGGER.log(Level.WARNING, "Chave deletada após falha na criptografia");
            }
            LOGGER.log(Level.SEVERE, "Erro ao criptografar senha", ex);
            throw new SQLException("Erro ao processar senha.");
        }
    }

    public static void addUser(Connection conn, String username, String password, boolean isAdmin) throws SQLException {
        register(conn, username, password, password, isAdmin);
    }

    public static void editUser(Connection conn, String currentUsername, String newUsername, String newPassword, boolean isAdmin) throws SQLException {
        if (conn == null) {
            throw new SQLException("Sem conexão com o banco de dados.");
        }
        if (!validateUsername(newUsername)) {
            throw new SQLException("O nome de usuário deve ter pelo menos 3 caracteres.");
        }
        if (!validatePassword(newPassword, false)) {
            throw new SQLException("A senha deve ter pelo menos 6 caracteres.");
        }
        if (!currentUsername.equals(newUsername)) {
            String sqlCheck = "SELECT COUNT(*) FROM usuarios WHERE nome = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setString(1, newUsername);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new SQLException("Novo nome de usuário já existe.");
                    }
                }
            }
        }

        try {
            String sql;
            PreparedStatement ps;

            // Se a senha foi alterada, criptografa a nova senha
            if (!newPassword.isEmpty()) {
                File keyFile = getCryptoKeyFile(newUsername);

                // Gera nova chave se usuário foi renomeado ou chave não existe
                if (!currentUsername.equals(newUsername) || !keyFile.exists()) {
                    CryptoDummy crypto = new CryptoDummy();
                    crypto.geraChave(keyFile);
                }

                CryptoDummy crypto = new CryptoDummy();
                crypto.geraCifra(newPassword.getBytes(), keyFile);
                byte[] encryptedPassword = crypto.getTextoCifrado();

                sql = "UPDATE usuarios SET nome = ?, senha = ?, admin = ? WHERE nome = ?";
                ps = conn.prepareStatement(sql);
                ps.setString(1, newUsername);
                ps.setBytes(2, encryptedPassword);
                ps.setInt(3, isAdmin ? 1 : 0);
                ps.setString(4, currentUsername);
            } else {
                // Mantém a senha atual
                sql = "UPDATE usuarios SET nome = ?, admin = ? WHERE nome = ?";
                ps = conn.prepareStatement(sql);
                ps.setString(1, newUsername);
                ps.setInt(2, isAdmin ? 1 : 0);
                ps.setString(3, currentUsername);
            }

            try {
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("Usuário não encontrado.");
                }

                // Se usuário foi renomeado, renomeia o arquivo de chave
                if (!currentUsername.equals(newUsername) && newPassword.isEmpty()) {
                    File oldKeyFile = getCryptoKeyFile(currentUsername);
                    File newKeyFile = getCryptoKeyFile(newUsername);
                    if (oldKeyFile.exists()) {
                        oldKeyFile.renameTo(newKeyFile);
                    }
                }
            } finally {
                ps.close();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao editar usuário", ex);
            throw ex;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Erro ao criptografar senha", ex);
            throw new SQLException("Erro ao processar senha.");
        }
    }


    public static void deleteUser(Connection conn, String username) throws SQLException {
        if (conn == null) {
            throw new SQLException("Sem conexão com o banco de dados.");
        }
        String sql = "DELETE FROM usuarios WHERE nome = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Usuário não encontrado.");
            }

            // Delete the user's encryption key file
            File keyFile = getCryptoKeyFile(username);
            if (keyFile.exists()) {
                boolean deleted = keyFile.delete();
                if (deleted) {
                    LOGGER.log(Level.INFO, "Chave de criptografia deletada: {0}", keyFile.getName());
                } else {
                    LOGGER.log(Level.WARNING, "Falha ao deletar chave de criptografia: {0}", keyFile.getName());
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao excluir usuário", ex);
            throw ex;
        }
    }

    public static ArrayList<User> getUsers(Connection conn, String searchTerm) throws SQLException {
        if (conn == null) {
            throw new SQLException("Sem conexão com o banco de dados.");
        }
        ArrayList<User> users = new ArrayList<>();
        String sql = "SELECT nome, admin, ultimo_login, data_criacao FROM usuarios" +
                     (searchTerm != null ? " WHERE nome LIKE ?" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (searchTerm != null) {
                ps.setString(1, "%" + searchTerm + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("nome");
                    boolean isAdmin = rs.getInt("admin") == 1;

                    // Parse datas (pode ser null para usuários antigos)
                    LocalDateTime lastLogin = null;
                    LocalDateTime accountCreated = null;

                    String lastLoginStr = rs.getString("ultimo_login");
                    if (lastLoginStr != null && !lastLoginStr.isEmpty()) {
                        try {
                            lastLogin = LocalDateTime.parse(lastLoginStr);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Erro ao parsear data de último login", e);
                        }
                    }

                    String accountCreatedStr = rs.getString("data_criacao");
                    if (accountCreatedStr != null && !accountCreatedStr.isEmpty()) {
                        try {
                            accountCreated = LocalDateTime.parse(accountCreatedStr);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Erro ao parsear data de criação", e);
                        }
                    }

                    users.add(new User(username, "", isAdmin, lastLogin, accountCreated));
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar usuários", ex);
            throw ex;
        }
        return users;
    }
}