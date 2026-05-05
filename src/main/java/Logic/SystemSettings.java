package Logic;

import database.DatabaseManager;
import java.sql.*;
import java.util.logging.Logger;

/**
 * Central access point for all system_settings values.
 * Caches settings in memory and reloads on demand.
 */
public class SystemSettings {

    private static final Logger logger = Logger.getLogger(SystemSettings.class.getName());

    private static String companyName    = "Electric Utility Co.";
    private static String companyAddress = "123 Power Street";
    private static String companyPhone   = "+1-555-0100";
    private static String companyEmail   = "billing@utility.com";
    private static double taxPercent     = 0.0;

    private static int    billDueDays    = 30;
    private static double latePenaltyPct = 5.0;
    private static double minBillAmount  = 0.0;
    private static String currencySymbol = "$";
    private static String billingCycle   = "Monthly";

    private static int    maxLoginAttempts = 5;
    private static int    lockoutMinutes   = 15;
    private static int    sessionTimeout   = 30;
    private static int    minPasswordLen   = 6;
    private static boolean requireUppercase = false;
    private static boolean requireNumber    = false;
    private static boolean requireSpecial   = false;

    private static boolean loaded = false;

    /**
     * Loads system settings from the database into memory.
     * Only loads once; subsequent calls do nothing.
     */
    public static synchronized void load() {
        String sql = "SELECT setting_key, setting_value FROM system_settings";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String k = rs.getString(1);
                String v = rs.getString(2);
                if (v == null) continue;
                switch (k) {
                    case "company_name":     companyName    = v; break;
                    case "company_address":  companyAddress = v; break;
                    case "company_phone":    companyPhone   = v; break;
                    case "company_email":    companyEmail   = v; break;
                    case "tax_percent":      taxPercent     = parseDouble(v, 0); break;
                    case "bill_due_days":    billDueDays    = parseInt(v, 30); break;
                    case "late_penalty_pct": latePenaltyPct = parseDouble(v, 5); break;
                    case "min_bill_amount":  minBillAmount  = parseDouble(v, 0); break;
                    case "currency_symbol":  currencySymbol = v; break;
                    case "billing_cycle":    billingCycle   = v; break;
                    case "max_login_attempts": maxLoginAttempts = parseInt(v, 5); break;
                    case "lockout_minutes":    lockoutMinutes   = parseInt(v, 15); break;
                    case "session_timeout":    sessionTimeout   = parseInt(v, 30); break;
                    case "min_password_len":   minPasswordLen   = parseInt(v, 6); break;
                    case "require_uppercase":  requireUppercase = "true".equals(v); break;
                    case "require_number":     requireNumber    = "true".equals(v); break;
                    case "require_special":    requireSpecial   = "true".equals(v); break;
                }
            }
            loaded = true;
            logger.info("System settings loaded successfully.");
        } catch (SQLException e) {
            logger.warning("Could not load system settings: " + e.getMessage());
        }
    }

    /**
     * Forces a reload of system settings from the database.
     */
    public static void reload() {
        loaded = false;
        load();
    }

    /**
     * Ensures settings are loaded before accessing them.
     */
    private static void ensureLoaded() {
        if (!loaded) load();
    }

    public static String getCompanyName()    { ensureLoaded(); return companyName; }
    public static String getCompanyAddress() { ensureLoaded(); return companyAddress; }
    public static String getCompanyPhone()   { ensureLoaded(); return companyPhone; }
    public static String getCompanyEmail()   { ensureLoaded(); return companyEmail; }
    public static double getTaxPercent()     { ensureLoaded(); return taxPercent; }

    public static int    getBillDueDays()    { ensureLoaded(); return billDueDays; }
    public static double getLatePenaltyPct() { ensureLoaded(); return latePenaltyPct; }
    public static double getMinBillAmount()  { ensureLoaded(); return minBillAmount; }
    public static String getCurrencySymbol() { ensureLoaded(); return currencySymbol; }
    public static String getBillingCycle()   { ensureLoaded(); return billingCycle; }

    public static int  getMaxLoginAttempts() { ensureLoaded(); return maxLoginAttempts; }
    public static int  getLockoutMinutes()   { ensureLoaded(); return lockoutMinutes; }
    public static int  getSessionTimeout()   { ensureLoaded(); return sessionTimeout; }
    public static int  getMinPasswordLen()   { ensureLoaded(); return minPasswordLen; }
    public static boolean isRequireUppercase(){ ensureLoaded(); return requireUppercase; }
    public static boolean isRequireNumber()  { ensureLoaded(); return requireNumber; }
    public static boolean isRequireSpecial() { ensureLoaded(); return requireSpecial; }

    /**
     * Validates a password against the current system requirements.
     * @param password the password to validate
     * @return null if valid, or an error message if invalid
     */
    public static String validatePassword(String password) {
        ensureLoaded();
        if (password == null || password.length() < minPasswordLen)
            return "Password must be at least " + minPasswordLen + " characters.";
        if (requireUppercase && !password.matches(".*[A-Z].*"))
            return "Password must contain at least one uppercase letter.";
        if (requireNumber && !password.matches(".*[0-9].*"))
            return "Password must contain at least one number.";
        if (requireSpecial && !password.matches(".*[^a-zA-Z0-9].*"))
            return "Password must contain at least one special character.";
        return null;
    }

    /**
     * Formats an amount as currency using the system currency symbol.
     * @param amount the amount to format
     * @return the formatted currency string
     */
    public static String formatCurrency(double amount) {
        ensureLoaded();
        return currencySymbol + String.format("%.2f", amount);
    }

    /**
     * Applies the system tax rate to an amount.
     * @param amount the base amount
     * @return the amount with tax applied
     */
    public static double applyTax(double amount) {
        ensureLoaded();
        return amount * (1 + taxPercent / 100.0);
    }

    /**
     * Safely parses an integer from a string, returning default if invalid.
     * @param v the string to parse
     * @param def the default value if parsing fails
     * @return the parsed integer or default
     */
    private static int    parseInt(String v, int def)    { try { return Integer.parseInt(v.trim()); } catch (Exception e) { return def; } }

    /**
     * Safely parses a double from a string, returning default if invalid.
     * @param v the string to parse
     * @param def the default value if parsing fails
     * @return the parsed double or default
     */
    private static double parseDouble(String v, double def) { try { return Double.parseDouble(v.trim()); } catch (Exception e) { return def; } }
}
