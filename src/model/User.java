package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class User {
    private static final Logger LOGGER = Logger.getLogger(User.class.getName());
    private final String username;
    private final String password;
    private final boolean isAdmin;

    public User(String username, String password, boolean isAdmin) {
        this.username = username;
        this.password = password;
        this.isAdmin = isAdmin;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAdmin() {
        return isAdmin;
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
        String sql = "SELECT senha FROM usuarios WHERE nome = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getString("senha").equals(password);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao autenticar usuário", ex);
            throw ex;
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
        String sql = "INSERT INTO usuarios (nome, senha, admin) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setInt(3, isAdmin ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException ex) {
            if (ex.getMessage().contains("UNIQUE constraint failed")) {
                throw new SQLException("Este nome de usuário já está em uso.");
            }
            LOGGER.log(Level.SEVERE, "Erro ao registrar usuário", ex);
            throw ex;
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
        String sql = "UPDATE usuarios SET nome = ?, senha = ?, admin = ? WHERE nome = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newUsername);
            ps.setString(2, newPassword.isEmpty() ? getCurrentPassword(conn, currentUsername) : newPassword);
            ps.setInt(3, isAdmin ? 1 : 0);
            ps.setString(4, currentUsername);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Usuário não encontrado.");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao editar usuário", ex);
            throw ex;
        }
    }

    private static String getCurrentPassword(Connection conn, String username) throws SQLException {
        if (conn == null) {
            throw new SQLException("Sem conexão com o banco de dados.");
        }
        String sql = "SELECT senha FROM usuarios WHERE nome = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("senha");
                }
                throw new SQLException("Usuário não encontrado.");
            }
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
        String sql = "SELECT nome, admin FROM usuarios" + (searchTerm != null ? " WHERE nome LIKE ?" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (searchTerm != null) {
                ps.setString(1, "%" + searchTerm + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(rs.getString("nome"), "", rs.getInt("admin") == 1));
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar usuários", ex);
            throw ex;
        }
        return users;
    }
}