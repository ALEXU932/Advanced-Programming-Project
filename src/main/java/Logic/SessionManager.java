package Logic;

import database.DatabaseManager;
import database.User;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages login sessions, failed attempt tracking, and session timeout.
 */
public class SessionManager {

    private static User currentUser;
    private static long lastActivityTime;
    private static final Map<String, Integer>  failedAttempts  = new HashMap<>();
    private static final Map<String, Long>     lockoutTime     = new HashMap<>();
    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());

    /**
     * Sets the current logged-in user and records activity time.
     * @param user the user to set as current
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
        lastActivityTime = System.currentTimeMillis();
    }

    /**
     * Gets the currently logged-in user.
     * @return the current user, or null if not logged in
     */
    public static User getCurrentUser() { return currentUser; }

    /**
     * Records user activity to reset session timeout.
     */
    public static void recordActivity() {
        lastActivityTime = System.currentTimeMillis();
    }

    /**
     * Checks if the current session has expired based on inactivity.
     * @return true if session is expired, false otherwise
     */
    public static boolean isSessionExpired() {
        if (currentUser == null) return true;
        long elapsed = (System.currentTimeMillis() - lastActivityTime) / 60000;
        return elapsed >= SystemSettings.getSessionTimeout();
    }

    /**
     * Logs out the current user and clears the session.
     */
    public static void logout() {
        if (currentUser != null) {
            AuditLogger.log(currentUser.getUserId(), currentUser.getUsername(),
                AuditLogger.Action.LOGOUT, "User logged out");
        }
        currentUser = null;
    }

    /**
     * Checks if a user can attempt to login (not in lockout period).
     * @param username the username to check
     * @return true if login attempt is allowed, false if locked out
     */
    public static boolean canAttemptLogin(String username) {
        if (!lockoutTime.containsKey(username)) return true;
        int lockoutMins = SystemSettings.getLockoutMinutes();
        long elapsed = (System.currentTimeMillis() - lockoutTime.get(username)) / 60000;
        if (elapsed >= lockoutMins) {
            failedAttempts.remove(username);
            lockoutTime.remove(username);
            return true;
        }
        return false;
    }

    /**
     * Gets the remaining minutes until lockout expires for a user.
     * @param username the username to check
     * @return remaining lockout minutes, or 0 if not locked
     */
    public static int getRemainingLockoutMinutes(String username) {
        if (!lockoutTime.containsKey(username)) return 0;
        int lockoutMins = SystemSettings.getLockoutMinutes();
        long elapsed = (System.currentTimeMillis() - lockoutTime.get(username)) / 60000;
        return (int) Math.max(0, lockoutMins - elapsed);
    }

    /**
     * Records a failed login attempt and potentially locks the account.
     * @param username the username that failed to login
     */
    public static void recordFailedAttempt(String username) {
        int count = failedAttempts.getOrDefault(username, 0) + 1;
        failedAttempts.put(username, count);
        AuditLogger.log(AuditLogger.Action.FAILED_LOGIN, "Failed login for: " + username + " (attempt " + count + ")");
        int maxAttempts = SystemSettings.getMaxLoginAttempts();
        if (count >= maxAttempts) {
            lockoutTime.put(username, System.currentTimeMillis());
            logger.warning("Account locked: " + username + " after " + count + " failed attempts");
        }
    }

    /**
     * Clears failed attempts and lockout for a user.
     * @param username the username to clear
     */
    public static void clearFailedAttempts(String username) {
        failedAttempts.remove(username);
        lockoutTime.remove(username);
    }

    /**
     * Gets the number of failed login attempts for a user.
     * @param username the username to check
     * @return the number of failed attempts
     */
    public static int getFailedAttempts(String username) {
        return failedAttempts.getOrDefault(username, 0);
    }

    /**
     * Gets the maximum allowed login attempts from system settings.
     * @return the maximum attempts
     */
    public static int getMaxAttempts() { return SystemSettings.getMaxLoginAttempts(); }

    /**
     * Ensures the login_attempts table exists in the database.
     * Creates the table if it doesn't exist.
     */
    public static void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS login_attempts (" +
                     "  id INT AUTO_INCREMENT PRIMARY KEY," +
                     "  username VARCHAR(50)," +
                     "  attempt_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                     "  success BOOLEAN DEFAULT FALSE," +
                     "  ip_address VARCHAR(50)" +
                     ")";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            logger.warning("Could not create login_attempts table: " + e.getMessage());
        }
    }
}
