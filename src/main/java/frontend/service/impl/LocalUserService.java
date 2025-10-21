package frontend.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import backend.application.dto.DTOMapper;
import backend.application.dto.UserDTO;
import backend.application.service.UserService;
import backend.domain.model.User;
import backend.infrastructure.ServiceLocator;
import frontend.service.IUserService;

/**
 * Local implementation of User Service
 * Delegates to backend through ServiceLocator
 * Converts between DTOs and Domain Models
 * 
 * This allows frontend to be decoupled from backend domain models
 * In the future, can be replaced with RemoteUserService for client-server
 */
public class LocalUserService implements IUserService {
    private final UserService backendService;
    
    public LocalUserService() {
        this.backendService = ServiceLocator.getInstance().getUserService();
    }
    
    @Override
    public boolean authenticate(String username, String password) throws SQLException {
        return backendService.authenticate(username, password);
    }
    
    @Override
    public boolean register(String username, String password, boolean isAdmin) throws SQLException {
        try {
            backendService.register(username, password, password, isAdmin);
            return true;
        } catch (SQLException e) {
            throw e;
        }
    }
    
    @Override
    public boolean isAdmin(String username) throws SQLException {
        return backendService.isAdmin(username);
    }
    
    @Override
    public List<UserDTO> getAllUsers() throws SQLException {
        List<User> users = backendService.getUsers(null);
        return users.stream()
                .map(DTOMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean updateUser(String username, String newPassword, boolean isAdmin) throws SQLException {
        try {
            backendService.editUser(username, username, newPassword, isAdmin);
            return true;
        } catch (SQLException e) {
            throw e;
        }
    }
    
    @Override
    public boolean deleteUser(String username) throws SQLException {
        try {
            backendService.deleteUser(username);
            return true;
        } catch (SQLException e) {
            throw e;
        }
    }
    
    @Override
    public boolean userExists(String username) throws SQLException {
        // Use getUsers and check if list contains the username
        List<User> users = backendService.getUsers(username);
        return users.stream().anyMatch(u -> u.getUsername().equals(username));
    }
    
    @Override
    public UserDTO getUserInfo(String username) throws SQLException {
        List<User> users = backendService.getUsers(username);
        User user = users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
        return DTOMapper.toDTO(user);
    }
    
    @Override
    public void updateLastLogin(String username) throws SQLException {
        // This is called automatically by authenticate in backend
        // No need to call separately
    }
}
