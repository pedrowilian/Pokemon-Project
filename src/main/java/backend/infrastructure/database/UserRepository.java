package backend.infrastructure.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import backend.domain.model.User;
import backend.domain.service.IUserRepository;

/**
 * User repository implementation using SQLite
 * Extracted from User model static methods
 */
public class UserRepository implements IUserRepository {
    private static final Logger LOGGER = Logger.getLogger(UserRepository.class.getName());
    private final Connection connection;

    public UserRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT nome, admin, ultimo_login, data_criacao FROM usuarios WHERE nome = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<User> findAll(String searchTerm) throws SQLException {
        ArrayList<User> users = new ArrayList<>();
        String sql = "SELECT nome, admin, ultimo_login, data_criacao FROM usuarios" +
                     (searchTerm != null ? " WHERE nome LIKE ?" : "");

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (searchTerm != null) {
                ps.setString(1, "%" + searchTerm + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        }
        return users;
    }

    @Override
    public void create(String username, byte[] encryptedPassword, boolean isAdmin) throws SQLException {
        String sql = "INSERT INTO usuarios (nome, senha, admin, data_criacao) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setBytes(2, encryptedPassword);
            ps.setInt(3, isAdmin ? 1 : 0);
            ps.setString(4, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    @Override
    public void update(String currentUsername, String newUsername, byte[] encryptedPassword, boolean isAdmin) throws SQLException {
        String sql;
        PreparedStatement ps;

        if (encryptedPassword != null) {
            sql = "UPDATE usuarios SET nome = ?, senha = ?, admin = ? WHERE nome = ?";
            ps = connection.prepareStatement(sql);
            ps.setString(1, newUsername);
            ps.setBytes(2, encryptedPassword);
            ps.setInt(3, isAdmin ? 1 : 0);
            ps.setString(4, currentUsername);
        } else {
            sql = "UPDATE usuarios SET nome = ?, admin = ? WHERE nome = ?";
            ps = connection.prepareStatement(sql);
            ps.setString(1, newUsername);
            ps.setInt(2, isAdmin ? 1 : 0);
            ps.setString(3, currentUsername);
        }

        try {
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Usuário não encontrado.");
            }
        } finally {
            ps.close();
        }
    }

    @Override
    public void delete(String username) throws SQLException {
        String sql = "DELETE FROM usuarios WHERE nome = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Usuário não encontrado.");
            }
        }
    }

    @Override
    public void updateLastLogin(String username) throws SQLException {
        String sql = "UPDATE usuarios SET ultimo_login = ? WHERE nome = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Erro ao atualizar último login", ex);
        }
    }

    @Override
    public boolean exists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE nome = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public byte[] getEncryptedPassword(String username) throws SQLException {
        String sql = "SELECT senha FROM usuarios WHERE nome = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("senha");
                }
            }
        }
        return null;
    }

    @Override
    public boolean isAdmin(String username) throws SQLException {
        String sql = "SELECT admin FROM usuarios WHERE nome = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("admin") == 1;
                }
            }
        }
        return false;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        String username = rs.getString("nome");
        boolean isAdmin = rs.getInt("admin") == 1;

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

        return new User(username, "", isAdmin, lastLogin, accountCreated);
    }
}
