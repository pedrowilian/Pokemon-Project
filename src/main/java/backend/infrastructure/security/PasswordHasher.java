package backend.infrastructure.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Secure password hashing using BCrypt
 * Replaces the insecure CryptoDummy implementation
 */
public class PasswordHasher {
    private static final int BCRYPT_COST = 12;

    /**
     * Hash a password using BCrypt
     * @param password Plain text password
     * @return Hashed password
     */
    public static String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());
    }

    /**
     * Verify a password against a BCrypt hash
     * @param password Plain text password to verify
     * @param hash BCrypt hash to verify against
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String hash) {
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
        return result.verified;
    }

    /**
     * Check if a string is a BCrypt hash
     * @param possibleHash String to check
     * @return true if it's a BCrypt hash
     */
    public static boolean isBCryptHash(String possibleHash) {
        return possibleHash != null && possibleHash.startsWith("$2a$");
    }
}
