package backend.application.dto;

import java.io.Serializable;

/**
 * Data Transfer Object for User
 * Used for network communication and serialization
 */
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private boolean isAdmin;
    private String lastLogin;
    private String accountCreated;

    public UserDTO() {
    }

    public UserDTO(String username, boolean isAdmin, String lastLogin, String accountCreated) {
        this.username = username;
        this.isAdmin = isAdmin;
        this.lastLogin = lastLogin;
        this.accountCreated = accountCreated;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getAccountCreated() {
        return accountCreated;
    }

    public void setAccountCreated(String accountCreated) {
        this.accountCreated = accountCreated;
    }
}
