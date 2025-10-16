package backend.application.service;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import backend.domain.model.User;
import backend.domain.service.IUserRepository;
import backend.infrastructure.security.PasswordHasher;

/**
 * User service - handles all user-related business logic
 * Extracted from User model and GUI classes
 */
public class UserService {
    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());
    private final IUserRepository userRepository;

    public UserService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Authenticate a user
     * Handles both legacy (CryptoDummy) and new (BCrypt) password formats
     */
    public boolean authenticate(String username, String password) throws SQLException {
        if (!validateUsername(username) || password.isEmpty()) {
            return false;
        }

        // Get stored password
        byte[] storedPasswordBytes = userRepository.getEncryptedPassword(username);
        if (storedPasswordBytes == null) {
            return false;
        }

        // Try as BCrypt hash first (new format)
        String storedPasswordStr = new String(storedPasswordBytes);
        if (PasswordHasher.isBCryptHash(storedPasswordStr)) {
            boolean authenticated = PasswordHasher.verifyPassword(password, storedPasswordStr);
            if (authenticated) {
                userRepository.updateLastLogin(username);
            }
            return authenticated;
        }

        // Legacy support: Try CryptoDummy format
        File keyFile = getLegacyCryptoKeyFile(username);
        if (keyFile.exists()) {
            try {
                shared.util.CryptoDummy crypto = new shared.util.CryptoDummy();
                crypto.geraDecifra(storedPasswordBytes, keyFile);
                byte[] decryptedPassword = crypto.getTextoDecifrado();
                String decryptedPasswordStr = new String(decryptedPassword);

                boolean authenticated = decryptedPasswordStr.equals(password);
                if (authenticated) {
                    // Migrate to BCrypt
                    migrateToBCrypt(username, password);
                    userRepository.updateLastLogin(username);
                }
                return authenticated;
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Erro ao decifrar senha legada", ex);
            }
        }

        // Fallback: plain text (oldest format)
        if (storedPasswordStr.equals(password)) {
            migrateToBCrypt(username, password);
            userRepository.updateLastLogin(username);
            return true;
        }

        return false;
    }

    /**
     * Register a new user
     */
    public void register(String username, String password, String confirmPassword, boolean isAdmin) throws SQLException {
        if (!validateUsername(username)) {
            throw new SQLException("O nome de usuário deve ter pelo menos 3 caracteres.");
        }
        if (!validatePassword(password, true)) {
            throw new SQLException("A senha deve ter pelo menos 6 caracteres.");
        }
        if (!password.equals(confirmPassword)) {
            throw new SQLException("As senhas não coincidem.");
        }

        if (userRepository.exists(username)) {
            throw new SQLException("Este nome de usuário já está em uso.");
        }

        // Hash password with BCrypt
        String hashedPassword = PasswordHasher.hashPassword(password);
        userRepository.create(username, hashedPassword.getBytes(), isAdmin);
    }

    /**
     * Add user (admin function)
     */
    public void addUser(String username, String password, boolean isAdmin) throws SQLException {
        register(username, password, password, isAdmin);
    }

    /**
     * Edit user information
     */
    public void editUser(String currentUsername, String newUsername, String newPassword, boolean isAdmin) throws SQLException {
        if (!validateUsername(newUsername)) {
            throw new SQLException("O nome de usuário deve ter pelo menos 3 caracteres.");
        }
        if (!validatePassword(newPassword, false)) {
            throw new SQLException("A senha deve ter pelo menos 6 caracteres.");
        }

        // Check if new username already exists (if changing username)
        if (!currentUsername.equals(newUsername) && userRepository.exists(newUsername)) {
            throw new SQLException("Novo nome de usuário já existe.");
        }

        byte[] hashedPassword = null;
        if (newPassword != null && !newPassword.isEmpty()) {
            hashedPassword = PasswordHasher.hashPassword(newPassword).getBytes();
        }

        userRepository.update(currentUsername, newUsername, hashedPassword, isAdmin);

        // Clean up legacy key file if username changed
        if (!currentUsername.equals(newUsername)) {
            cleanupLegacyKeyFile(currentUsername);
        }
    }

    /**
     * Delete a user
     */
    public void deleteUser(String username) throws SQLException {
        userRepository.delete(username);
        cleanupLegacyKeyFile(username);
    }

    /**
     * Get all users or search by term
     */
    public List<User> getUsers(String searchTerm) throws SQLException {
        return userRepository.findAll(searchTerm);
    }

    /**
     * Check if a user is an administrator
     */
    public boolean isAdmin(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return userRepository.isAdmin(username);
    }

    /**
     * Validate username
     */
    public static boolean validateUsername(String username) {
        return username != null && !username.trim().isEmpty() && username.trim().length() >= 3;
    }

    /**
     * Validate password
     */
    public static boolean validatePassword(String password, boolean isNewUser) {
        if (password == null) return false;
        if (isNewUser) {
            return !password.trim().isEmpty() && password.trim().length() >= 6;
        }
        return password.trim().isEmpty() || password.trim().length() >= 6;
    }

    /**
     * Migrate legacy password to BCrypt
     */
    private void migrateToBCrypt(String username, String password) {
        try {
            String hashedPassword = PasswordHasher.hashPassword(password);
            userRepository.update(username, username, hashedPassword.getBytes(), false);
            cleanupLegacyKeyFile(username);
            LOGGER.log(Level.INFO, "Migrated password to BCrypt for user: " + username);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to migrate password to BCrypt", ex);
        }
    }

    /**
     * Get legacy CryptoDummy key file
     */
    private File getLegacyCryptoKeyFile(String username) {
        File keysDir = new File("keys");
        if (!keysDir.exists()) {
            keysDir.mkdirs();
        }
        return new File(keysDir, username + ".key");
    }

    /**
     * Clean up legacy key file
     */
    private void cleanupLegacyKeyFile(String username) {
        File keyFile = getLegacyCryptoKeyFile(username);
        if (keyFile.exists()) {
            boolean deleted = keyFile.delete();
            if (deleted) {
                LOGGER.log(Level.INFO, "Deleted legacy key file: " + keyFile.getName());
            }
        }
    }
}
