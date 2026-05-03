package database;

import java.sql.*;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/electric_billing_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // Change to your MySQL password
    private static DatabaseManager instance;
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            logger.severe("DB connection failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Runs automatic schema migrations on startup.
     * Safe to run multiple times — uses IF EXISTS / IF NOT EXISTS guards.
     */
    public void runMigrations() {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {

            // 1. Add profile_pic to users
            st.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_pic VARCHAR(500) DEFAULT NULL");

            // 2. Create meters table
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS meters (" +
                "  meter_id     INT AUTO_INCREMENT PRIMARY KEY," +
                "  meter_number VARCHAR(50) UNIQUE NOT NULL," +
                "  customer_id  INT," +
                "  meter_type   ENUM('SINGLE_PHASE','THREE_PHASE','SMART','PREPAID') DEFAULT 'SINGLE_PHASE'," +
                "  status       ENUM('ACTIVE','INACTIVE','FAULTY','REPLACED') DEFAULT 'ACTIVE'," +
                "  location     VARCHAR(255)," +
                "  installed_at DATE," +
                "  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL" +
                ")");

            // 3. Migrate meter_number from customers → meters (if column still exists)
            ResultSet rs = conn.getMetaData().getColumns(null, null, "customers", "meter_number");
            if (rs.next()) {
                st.executeUpdate(
                    "INSERT IGNORE INTO meters (meter_number, customer_id, meter_type, status) " +
                    "SELECT meter_number, customer_id, 'SINGLE_PHASE', 'ACTIVE' " +
                    "FROM customers WHERE meter_number IS NOT NULL AND meter_number != ''");
                st.executeUpdate("ALTER TABLE customers DROP COLUMN meter_number");
                logger.info("Migrated meter_number from customers to meters table.");
            }

            // 4. Create payments table
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS payments (" +
                "  payment_id INT AUTO_INCREMENT PRIMARY KEY," +
                "  bill_id INT NOT NULL," +
                "  customer_id INT NOT NULL," +
                "  payment_date DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "  amount DECIMAL(10,2) NOT NULL," +
                "  payment_method ENUM('CASH','BANK_TRANSFER','MOBILE_MONEY','CARD','ONLINE') DEFAULT 'CASH'," +
                "  reference_no VARCHAR(100)," +
                "  notes TEXT," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (bill_id) REFERENCES bills(bill_id)," +
                "  FOREIGN KEY (customer_id) REFERENCES customers(customer_id)" +
                ")");

            // 5. Create admins table
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS admins (" +
                "  admin_id INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id INT NOT NULL UNIQUE," +
                "  role VARCHAR(50) DEFAULT 'ADMIN'," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                ")");

            st.executeUpdate(
                "INSERT IGNORE INTO admins (user_id, role) " +
                "SELECT user_id, 'ADMIN' FROM users WHERE role='ADMIN'");

            // 7. Create usage_log table
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS usage_log (" +
                "  log_id INT AUTO_INCREMENT PRIMARY KEY," +
                "  customer_id INT NOT NULL," +
                "  reading_id INT," +
                "  log_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  usage_kwh DECIMAL(10,2) NOT NULL," +
                "  anomaly_detected BOOLEAN DEFAULT FALSE," +
                "  anomaly_score DECIMAL(6,3) DEFAULT 0," +
                "  severity ENUM('NONE','LOW','MEDIUM','HIGH') DEFAULT 'NONE'," +
                "  description TEXT," +
                "  is_resolved BOOLEAN DEFAULT FALSE," +
                "  FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE" +
                ")");

            // 8. Create audit_log table
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS audit_log (" +
                "  log_id INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id INT DEFAULT 0," +
                "  username VARCHAR(50)," +
                "  action VARCHAR(50) NOT NULL," +
                "  details TEXT," +
                "  performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  INDEX idx_user (user_id)," +
                "  INDEX idx_action (action)," +
                "  INDEX idx_time (performed_at)" +
                ")");

            // 9. Create system_settings table
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS system_settings (" +
                "  setting_key   VARCHAR(100) PRIMARY KEY," +
                "  setting_value TEXT NOT NULL," +
                "  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")");

            String[] defaults = {
                "('company_name','Electric Utility Co.')",
                "('company_address','123 Power Street')",
                "('company_phone','+1-555-0100')",
                "('company_email','billing@utility.com')",
                "('tax_percent','0')",
                "('bill_due_days','30')",
                "('late_penalty_pct','5')",
                "('min_bill_amount','0')",
                "('currency_symbol','$')",
                "('billing_cycle','Monthly')",
                "('max_login_attempts','5')",
                "('lockout_minutes','15')",
                "('session_timeout','30')",
                "('min_password_len','6')",
                "('require_uppercase','false')",
                "('require_number','false')"
            };
            for (String d : defaults)
                st.executeUpdate("INSERT IGNORE INTO system_settings (setting_key, setting_value) VALUES " + d);

            // 10. Create login_attempts table
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS login_attempts (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  username VARCHAR(50)," +
                "  attempt_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  success BOOLEAN DEFAULT FALSE" +
                ")");

            // 11. Create disputes table
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS disputes (" +
                "  dispute_id INT AUTO_INCREMENT PRIMARY KEY," +
                "  bill_id INT NOT NULL," +
                "  customer_id INT NOT NULL," +
                "  reason VARCHAR(100) NOT NULL," +
                "  description TEXT," +
                "  status ENUM('OPEN','UNDER_REVIEW','RESOLVED','REJECTED') DEFAULT 'OPEN'," +
                "  resolution TEXT," +
                "  adjusted_amount DECIMAL(10,2)," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  resolved_at TIMESTAMP NULL," +
                "  resolved_by INT," +
                "  FOREIGN KEY (bill_id) REFERENCES bills(bill_id)," +
                "  FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE" +
                ")");

            // 12. Create customer_budgets table
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS customer_budgets (" +
                "  budget_id INT AUTO_INCREMENT PRIMARY KEY," +
                "  customer_id INT NOT NULL UNIQUE," +
                "  monthly_budget_kwh DECIMAL(10,2) NOT NULL," +
                "  alert_threshold INT DEFAULT 80," +
                "  is_active BOOLEAN DEFAULT TRUE," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE" +
                ")");

            // 13. SMTP settings defaults
            String[] smtpDefaults = {
                "('smtp_host','smtp.gmail.com')",
                "('smtp_port','587')",
                "('smtp_username','')",
                "('smtp_password','')",
                "('smtp_from_name','Electric Billing System')",
                "('smtp_enabled','false')",
                "('smtp_tls','true')"
            };
            for (String d : smtpDefaults)
                st.executeUpdate("INSERT IGNORE INTO system_settings (setting_key,setting_value) VALUES " + d);

            logger.info("Database migrations completed successfully.");

        } catch (SQLException e) {
            logger.warning("Migration warning (may be safe to ignore): " + e.getMessage());
        }
    }

    public void closeQuietly(AutoCloseable... resources) {
        for (AutoCloseable r : resources) {
            if (r != null) try { r.close(); } catch (Exception ignored) {}
        }
    }
}
