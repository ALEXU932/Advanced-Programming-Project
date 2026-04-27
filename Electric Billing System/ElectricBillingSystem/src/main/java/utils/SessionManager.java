package utils;

import db.DatabaseManager;
import models.User;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages login sessions, failed attempt tracking, and session timeout.
 */
public class SessionManager {

    // All security values now come from SystemSettings (system_settings table)

    private static User currentUser;
    private static long lastActivityTime;
    private static final Map<String, Integer>  failedAttempts  = new HashMap<>();
    private static final Map<String, Long>     lockoutTime     = new HashMap<>();
    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());

    public static void setCurrentUser(User user) {
        currentUser = user;
        lastActivityTime = System.currentTimeMillis();
    }

    public static User getCurrentUser() { return currentUser; }

    public static void recordActivity() {
        lastActivityTime = System.currentTimeMillis();
    }

    public static boolean isSessionExpired() {
        if (currentUser == null) return true;
        long elapsed = (System.currentTimeMillis() - lastActivityTime) / 60000;
        return elapsed >= utils.SystemSettings.getSessionTimeout();
    }

    public static void logout() {
        if (currentUser != null) {
            AuditLogger.log(currentUser.getUserId(), currentUser.getUsername(),
                AuditLogger.Action.LOGOUT, "User logged out");
        }
        currentUser = null;
    }

    /** Returns true if login is allowed, false if locked out. */
    public static boolean canAttemptLogin(String username) {
        if (!lockoutTime.containsKey(username)) return true;
        int lockoutMins = utils.SystemSettings.getLockoutMinutes();
        long elapsed = (System.currentTimeMillis() - lockoutTime.get(username)) / 60000;
        if (elapsed >= lockoutMins) {
            failedAttempts.remove(username);
            lockoutTime.remove(username);
            return true;
        }
        return false;
    }

    public static int getRemainingLockoutMinutes(String username) {
        if (!lockoutTime.containsKey(username)) return 0;
        int lockoutMins = utils.SystemSettings.getLockoutMinutes();
        long elapsed = (System.currentTimeMillis() - lockoutTime.get(username)) / 60000;
        return (int) Math.max(0, lockoutMins - elapsed);
    }

    public static void recordFailedAttempt(String username) {
        int count = failedAttempts.getOrDefault(username, 0) + 1;
        failedAttempts.put(username, count);
        AuditLogger.log(AuditLogger.Action.FAILED_LOGIN, "Failed login for: " + username + " (attempt " + count + ")");
        int maxAttempts = utils.SystemSettings.getMaxLoginAttempts();
        if (count >= maxAttempts) {
            lockoutTime.put(username, System.currentTimeMillis());
            logger.warning("Account locked: " + username + " after " + count + " failed attempts");
        }
    }

    public static void clearFailedAttempts(String username) {
        failedAttempts.remove(username);
        lockoutTime.remove(username);
    }

    public static int getFailedAttempts(String username) {
        return failedAttempts.getOrDefault(username, 0);
    }

    public static int getMaxAttempts() { return utils.SystemSettings.getMaxLoginAttempts(); }

    /** Ensure login_attempts table exists for persistent tracking. */
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
