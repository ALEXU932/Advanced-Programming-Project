package utils;

import db.DatabaseManager;
import java.sql.*;
import java.util.logging.Logger;

/**
 * Centralized audit trail logger.
 * Records all significant user actions to the audit_log table.
 */
public class AuditLogger {

    private static final Logger logger = Logger.getLogger(AuditLogger.class.getName());

    public enum Action {
        LOGIN, LOGOUT,
        ADD_CUSTOMER, EDIT_CUSTOMER, DELETE_CUSTOMER,
        ADD_METER, EDIT_METER, DELETE_METER,
        ADD_READING, DELETE_READING,
        GENERATE_BILL, MARK_BILL_PAID, DELETE_BILL,
        RECORD_PAYMENT,
        ADD_TARIFF, EDIT_TARIFF, TOGGLE_TARIFF,
        RESOLVE_ANOMALY, DELETE_ANOMALY,
        EXPORT_BILL, EXPORT_REPORT,
        CHANGE_PASSWORD, UPDATE_PROFILE, UPLOAD_PHOTO,
        ADD_ADMIN, DELETE_ADMIN,
        SETTINGS_CHANGE,
        FAILED_LOGIN
    }

    /**
     * Log an action performed by a user.
     */
    public static void log(int userId, String username, Action action, String details) {
        String sql = "INSERT INTO audit_log (user_id, username, action, details, performed_at) VALUES (?,?,?,?,NOW())";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, username);
            ps.setString(3, action.name());
            ps.setString(4, details != null ? details : "");
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Audit log failed: " + e.getMessage());
        }
    }

    /** Convenience overload for system-level actions (no user context). */
    public static void log(Action action, String details) {
        log(0, "SYSTEM", action, details);
    }

    /** Create the audit_log table if it doesn't exist. */
    public static void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS audit_log (" +
                     "  log_id       INT AUTO_INCREMENT PRIMARY KEY," +
                     "  user_id      INT DEFAULT 0," +
                     "  username     VARCHAR(50)," +
                     "  action       VARCHAR(50) NOT NULL," +
                     "  details      TEXT," +
                     "  performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                     "  INDEX idx_user (user_id)," +
                     "  INDEX idx_action (action)," +
                     "  INDEX idx_time (performed_at)" +
                     ")";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            logger.warning("Could not create audit_log table: " + e.getMessage());
        }
    }
}
