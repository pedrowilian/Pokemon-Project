package frontend.service;

import java.sql.SQLException;
import java.util.List;

import backend.application.dto.UserDTO;

/**
 * Interface for User Service
 * Abstracts user operations from the frontend
 * Allows future implementation via network (Remote) or local
 */
public interface IUserService {
    
    /**
     * Authenticate user with username and password
     */
    boolean authenticate(String username, String password) throws SQLException;
    
    /**
     * Register a new user
     */
    boolean register(String username, String password, boolean isAdmin) throws SQLException;
    
    /**
     * Check if user is admin
     */
    boolean isAdmin(String username) throws SQLException;
    
    /**
     * Get all users (admin only)
     */
    List<UserDTO> getAllUsers() throws SQLException;
    
    /**
     * Update user information
     */
    boolean updateUser(String username, String newPassword, boolean isAdmin) throws SQLException;
    
    /**
     * Delete user
     */
    boolean deleteUser(String username) throws SQLException;
    
    /**
     * Check if username exists
     */
    boolean userExists(String username) throws SQLException;
    
    /**
     * Get user information as DTO
     */
    UserDTO getUserInfo(String username) throws SQLException;
    
    /**
     * Update last login timestamp
     */
    void updateLastLogin(String username) throws SQLException;
}
