package backend.domain.service;

import java.sql.SQLException;
import java.util.List;

import backend.domain.model.User;

/**
 * Repository interface for User data access
 * Defines contract for User persistence operations
 */
public interface IUserRepository {

    /**
     * Find user by username
     */
    User findByUsername(String username) throws SQLException;

    /**
     * Get all users or search by term
     * @param searchTerm Optional search term (null for all users)
     */
    List<User> findAll(String searchTerm) throws SQLException;

    /**
     * Create a new user
     */
    void create(String username, byte[] encryptedPassword, boolean isAdmin) throws SQLException;

    /**
     * Update user information
     */
    void update(String currentUsername, String newUsername, byte[] encryptedPassword, boolean isAdmin) throws SQLException;

    /**
     * Delete user by username
     */
    void delete(String username) throws SQLException;

    /**
     * Update last login timestamp
     */
    void updateLastLogin(String username) throws SQLException;

    /**
     * Check if username exists
     */
    boolean exists(String username) throws SQLException;

    /**
     * Get encrypted password for authentication
     */
    byte[] getEncryptedPassword(String username) throws SQLException;

    /**
     * Check if user is an administrator
     */
    boolean isAdmin(String username) throws SQLException;
}
