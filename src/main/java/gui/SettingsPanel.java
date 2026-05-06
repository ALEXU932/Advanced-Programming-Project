package gui;

import database.DatabaseManager;
import database.User;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

/**
 * SettingsPanel provides the UI for editing system settings such as
 * company info, billing rules, database settings, and notification options.
 */
public class SettingsPanel extends JPanel {

    private final User currentUser;

    public SettingsPanel(User currentUser) {
        this.currentUser = currentUser;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("System Settings");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        add(title, BorderLayout.NORTH);

        // Two-column layout, each column scrollable
        JPanel cols = new JPanel(new GridLayout(1, 2, 16, 0));
        cols.setOpaque(false);

        // Left column: Company + Security
        JPanel leftCol = new JPanel();
        leftCol.setOpaque(false);
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.add(buildCompanyCard());
        leftCol.add(Box.createVerticalStrut(14));
        leftCol.add(buildSecurityCard());

        // Right column: Billing + Database
        JPanel rightCol = new JPanel();
        rightCol.setOpaque(false);
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.add(buildBillingCard());
        rightCol.add(Box.createVerticalStrut(14));
        rightCol.add(buildDatabaseCard());
        rightCol.add(Box.createVerticalStrut(14));
        rightCol.add(buildEmailCard());

        cols.add(leftCol);
        cols.add(rightCol);

        JScrollPane scroll = new JScrollPane(cols);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    // ── Company Info ──────────────────────────────────────────────────────────

    private JPanel buildCompanyCard() {
        JPanel card = UITheme.createCard("Company Information");
        card.setLayout(new BorderLayout(0, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = gbc();

        JTextField nameF    = field(getSetting("company_name",    "Electric Utility Co."));
        JTextField addressF = field(getSetting("company_address", "123 Power Street"));
        JTextField phoneF   = field(getSetting("company_phone",   "+1-555-0100"));
        JTextField emailF   = field(getSetting("company_email",   "billing@utility.com"));
        JTextField taxF     = field(getSetting("tax_percent",     "0"));

        int r = 0;
        g.gridy=r++; form.add(lbl("Company Name"),   g); g.gridy=r++; form.add(nameF,    g);
        g.gridy=r++; form.add(lbl("Address"),         g); g.gridy=r++; form.add(addressF, g);
        g.gridy=r++; form.add(lbl("Phone"),           g); g.gridy=r++; form.add(phoneF,   g);
        g.gridy=r++; form.add(lbl("Email"),           g); g.gridy=r++; form.add(emailF,   g);
        g.gridy=r++; form.add(lbl("Tax Rate (%)"),    g); g.gridy=r++; form.add(taxF,     g);

        JLabel statusLbl = statusLabel();
        g.gridy=r++; g.insets=new Insets(6,0,2,0); form.add(statusLbl, g);

        JButton saveBtn = UITheme.createPrimaryButton("Save Company Info");
        saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        g.gridy=r++; g.insets=new Insets(6,0,0,0); form.add(saveBtn, g);

        card.add(form, BorderLayout.CENTER);

        saveBtn.addActionListener(e -> {
            saveSetting("company_name",    nameF.getText().trim());
            saveSetting("company_address", addressF.getText().trim());
            saveSetting("company_phone",   phoneF.getText().trim());
            saveSetting("company_email",   emailF.getText().trim());
            saveSetting("tax_percent",     taxF.getText().trim());
            ok(statusLbl, "Saved successfully!");
            audit("Updated Company Information: name=" + nameF.getText().trim()
                + ", email=" + emailF.getText().trim()
                + ", tax=" + taxF.getText().trim() + "%");
            Logic.SystemSettings.reload();
        });
        return card;
    }

    // ── Billing Rules ─────────────────────────────────────────────────────────

    private JPanel buildBillingCard() {
        JPanel card = UITheme.createCard("Billing Rules");
        card.setLayout(new BorderLayout(0, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = gbc();

        JTextField dueDaysF  = field(getSetting("bill_due_days",    "30"));
        JTextField penaltyF  = field(getSetting("late_penalty_pct", "5"));
        JTextField minBillF  = field(getSetting("min_bill_amount",  "0"));
        JTextField currencyF = field(getSetting("currency_symbol",  "$"));

        JComboBox<String> cycleCb = styledCombo(
            new String[]{"Monthly","Bi-Monthly","Quarterly"},
            getSetting("billing_cycle", "Monthly"));

        int r = 0;
        g.gridy=r++; form.add(lbl("Bill Due Days (after generation)"), g);
        g.gridy=r++; form.add(dueDaysF,  g);
        g.gridy=r++; form.add(lbl("Late Payment Penalty (%)"),         g);
        g.gridy=r++; form.add(penaltyF,  g);
        g.gridy=r++; form.add(lbl("Minimum Bill Amount ($)"),          g);
        g.gridy=r++; form.add(minBillF,  g);
        g.gridy=r++; form.add(lbl("Currency Symbol"),                  g);
        g.gridy=r++; form.add(currencyF, g);
        g.gridy=r++; form.add(lbl("Billing Cycle"),                    g);
        g.gridy=r++; form.add(cycleCb,   g);

        JLabel statusLbl = statusLabel();
        g.gridy=r++; g.insets=new Insets(6,0,2,0); form.add(statusLbl, g);

        JButton saveBtn = UITheme.createPrimaryButton("Save Billing Rules");
        saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        g.gridy=r++; g.insets=new Insets(6,0,0,0); form.add(saveBtn, g);

        card.add(form, BorderLayout.CENTER);

        saveBtn.addActionListener(e -> {
            saveSetting("bill_due_days",    dueDaysF.getText().trim());
            saveSetting("late_penalty_pct", penaltyF.getText().trim());
            saveSetting("min_bill_amount",  minBillF.getText().trim());
            saveSetting("currency_symbol",  currencyF.getText().trim());
            saveSetting("billing_cycle",    (String) cycleCb.getSelectedItem());
            ok(statusLbl, "Saved successfully!");
            audit("Updated Billing Rules: due_days=" + dueDaysF.getText().trim()
                + ", penalty=" + penaltyF.getText().trim() + "%"
                + ", cycle=" + cycleCb.getSelectedItem()
                + ", currency=" + currencyF.getText().trim());
            Logic.SystemSettings.reload();
        });
        return card;
    }

    // ── Security ──────────────────────────────────────────────────────────────

    private JPanel buildSecurityCard() {
        JPanel card = UITheme.createCard("Security Settings");
        card.setLayout(new BorderLayout(0, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = gbc();

        JTextField maxAttF   = field(getSetting("max_login_attempts", "5"));
        JTextField lockoutF  = field(getSetting("lockout_minutes",    "15"));
        JTextField sessionF  = field(getSetting("session_timeout",    "30"));
        JTextField minPassF  = field(getSetting("min_password_len",   "6"));

        JCheckBox upperCb = checkbox("Require uppercase letter",
            "true".equals(getSetting("require_uppercase", "false")));
        JCheckBox numCb   = checkbox("Require at least one number",
            "true".equals(getSetting("require_number", "false")));
        JCheckBox specCb  = checkbox("Require special character",
            "true".equals(getSetting("require_special", "false")));

        int r = 0;
        g.gridy=r++; form.add(lbl("Max Login Attempts Before Lockout"), g);
        g.gridy=r++; form.add(maxAttF,  g);
        g.gridy=r++; form.add(lbl("Lockout Duration (minutes)"),        g);
        g.gridy=r++; form.add(lockoutF, g);
        g.gridy=r++; form.add(lbl("Session Timeout (minutes)"),         g);
        g.gridy=r++; form.add(sessionF, g);
        g.gridy=r++; form.add(lbl("Minimum Password Length"),           g);
        g.gridy=r++; form.add(minPassF, g);
        g.gridy=r++; g.insets=new Insets(8,0,2,0);
        form.add(lbl("Password Requirements"), g);
        g.insets=new Insets(2,0,2,0);
        g.gridy=r++; form.add(upperCb, g);
        g.gridy=r++; form.add(numCb,   g);
        g.gridy=r++; form.add(specCb,  g);

        JLabel statusLbl = statusLabel();
        g.gridy=r++; g.insets=new Insets(6,0,2,0); form.add(statusLbl, g);

        JButton saveBtn = UITheme.createPrimaryButton("Save Security Settings");
        saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        g.gridy=r++; g.insets=new Insets(6,0,0,0); form.add(saveBtn, g);

        card.add(form, BorderLayout.CENTER);

        saveBtn.addActionListener(e -> {
            saveSetting("max_login_attempts", maxAttF.getText().trim());
            saveSetting("lockout_minutes",    lockoutF.getText().trim());
            saveSetting("session_timeout",    sessionF.getText().trim());
            saveSetting("min_password_len",   minPassF.getText().trim());
            saveSetting("require_uppercase",  String.valueOf(upperCb.isSelected()));
            saveSetting("require_number",     String.valueOf(numCb.isSelected()));
            saveSetting("require_special",    String.valueOf(specCb.isSelected()));
            ok(statusLbl, "Saved successfully!");
            audit("Updated Security Settings: max_attempts=" + maxAttF.getText().trim()
                + ", lockout=" + lockoutF.getText().trim() + "min"
                + ", session_timeout=" + sessionF.getText().trim() + "min"
                + ", min_pass_len=" + minPassF.getText().trim());
            Logic.SystemSettings.reload();
        });
        return card;
    }

    // ── Database & Maintenance ────────────────────────────────────────────────

    private JPanel buildDatabaseCard() {
        JPanel card = UITheme.createCard("Database & Maintenance");
        card.setLayout(new BorderLayout(0, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = gbc();

        // DB status
        JLabel dbStatusLbl = new JLabel("Checking...", SwingConstants.CENTER);
        dbStatusLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dbStatusLbl.setForeground(UITheme.ACCENT);
        dbStatusLbl.setOpaque(true);
        dbStatusLbl.setBackground(new Color(0, 40, 80, 120));
        dbStatusLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.ACCENT, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JLabel statsLbl = new JLabel(" ", SwingConstants.CENTER);
        statsLbl.setFont(UITheme.FONT_SMALL);
        statsLbl.setForeground(UITheme.TEXT_MUTED);

        JButton testBtn    = UITheme.createAccentButton("Test DB Connection");
        JButton clearBtn   = UITheme.createAccentButton("Clear Audit Logs (30d+)");
        JButton optimizeBtn= UITheme.createAccentButton("Optimize Tables");

        for (JButton b : new JButton[]{testBtn, clearBtn, optimizeBtn})
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(40, 70, 120));

        int r = 0;
        g.gridy=r++; form.add(lbl("Database Connection Status"), g);
        g.gridy=r++; form.add(dbStatusLbl, g);
        g.gridy=r++; form.add(statsLbl, g);
        g.gridy=r++; g.insets=new Insets(8,0,4,0); form.add(sep, g);
        g.insets=new Insets(4,0,4,0);
        g.gridy=r++; form.add(testBtn,     g);
        g.gridy=r++; form.add(clearBtn,    g);
        g.gridy=r++; form.add(optimizeBtn, g);

        JLabel actionStatusLbl = statusLabel();
        g.gridy=r++; g.insets=new Insets(6,0,2,0); form.add(actionStatusLbl, g);

        card.add(form, BorderLayout.CENTER);

        // Load stats immediately
        loadDbStats(dbStatusLbl, statsLbl);

        testBtn.addActionListener(e -> {
            boolean ok = DatabaseManager.getInstance().testConnection();
            dbStatusLbl.setText(ok ? "  Connected" : "  Disconnected");
            dbStatusLbl.setForeground(ok ? UITheme.SUCCESS : UITheme.DANGER);
            dbStatusLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ok ? UITheme.SUCCESS : UITheme.DANGER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            loadDbStats(dbStatusLbl, statsLbl);
        });

        clearBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                "Delete audit logs older than 30 days?",
                "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 Statement st = conn.createStatement()) {
                int n = st.executeUpdate(
                    "DELETE FROM audit_log WHERE performed_at < DATE_SUB(NOW(), INTERVAL 30 DAY)");
                ok(actionStatusLbl, n + " old entries removed.");
                audit("Cleared audit logs older than 30 days (" + n + " entries removed)");
                loadDbStats(dbStatusLbl, statsLbl);
            } catch (SQLException ex) {
                err(actionStatusLbl, "Error: " + ex.getMessage());
            }
        });

        optimizeBtn.addActionListener(e -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 Statement st = conn.createStatement()) {
                for (String t : new String[]{"audit_log","anomalies","meter_readings","bills","payments"})
                    st.executeUpdate("OPTIMIZE TABLE " + t);
                ok(actionStatusLbl, "Tables optimized successfully.");
                audit("Ran database table optimization");
            } catch (SQLException ex) {
                err(actionStatusLbl, "Error: " + ex.getMessage());
            }
        });

        // Backup button
        JButton backupBtn = UITheme.createPrimaryButton("Backup Database");
        backupBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JButton restoreBtn = UITheme.createAccentButton("Restore from Backup");
        restoreBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        g.gridy=r++; g.insets=new Insets(8,0,4,0); form.add(backupBtn, g);
        g.gridy=r++; g.insets=new Insets(4,0,4,0); form.add(restoreBtn, g);

        backupBtn.addActionListener(e -> backupDatabase(actionStatusLbl));
        restoreBtn.addActionListener(e -> restoreDatabase(actionStatusLbl));

        return card;
    }

    private void loadDbStats(JLabel statusLbl, JLabel statsLbl) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement()) {
            statusLbl.setText("  Connected");
            statusLbl.setForeground(UITheme.SUCCESS);
            int c = 0, b = 0, l = 0;
            ResultSet r1 = st.executeQuery("SELECT COUNT(*) FROM customers");
            if (r1.next()) c = r1.getInt(1);
            ResultSet r2 = st.executeQuery("SELECT COUNT(*) FROM bills");
            if (r2.next()) b = r2.getInt(1);
            try {
                ResultSet r3 = st.executeQuery("SELECT COUNT(*) FROM audit_log");
                if (r3.next()) l = r3.getInt(1);
            } catch (SQLException ignored) {}
            statsLbl.setText(c + " customers  |  " + b + " bills  |  " + l + " log entries");
        } catch (SQLException e) {
            statusLbl.setText("  Disconnected");
            statusLbl.setForeground(UITheme.DANGER);
        }
    }

    // ── Email / SMTP Settings ─────────────────────────────────────────────────

    private JPanel buildEmailCard() {
        JPanel card = UITheme.createCard("Email Notifications (SMTP)");
        card.setLayout(new BorderLayout(0, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = gbc();

        report.EmailService.SmtpConfig cfg = report.EmailService.loadConfig();

        JTextField hostF     = field(cfg.host);
        JTextField portF     = field(cfg.port);
        JTextField userF     = field(cfg.username);
        JPasswordField passF = new JPasswordField(cfg.password);
        passF.setFont(UITheme.FONT_BODY);
        passF.setBackground(new Color(20,40,80));
        passF.setForeground(UITheme.TEXT_WHITE);
        passF.setCaretColor(Color.WHITE);
        passF.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.ACCENT,1),
            BorderFactory.createEmptyBorder(6,10,6,10)));
        passF.setPreferredSize(new Dimension(0,36));

        JTextField fromF     = field(cfg.fromName);
        JTextField testEmailF= field("");

        JCheckBox enabledCb = checkbox("Enable Email Notifications", cfg.enabled);
        JCheckBox tlsCb     = checkbox("Use TLS (recommended)", cfg.tls);

        int r = 0;
        g.gridy=r++; form.add(lbl("SMTP Host (e.g. smtp.gmail.com)"), g);
        g.gridy=r++; form.add(hostF, g);
        g.gridy=r++; form.add(lbl("SMTP Port (587=TLS, 465=SSL)"), g);
        g.gridy=r++; form.add(portF, g);
        g.gridy=r++; form.add(lbl("Username / Email"), g);
        g.gridy=r++; form.add(userF, g);
        g.gridy=r++; form.add(lbl("Password / App Password"), g);
        g.gridy=r++; form.add(passF, g);
        g.gridy=r++; form.add(lbl("From Name"), g);
        g.gridy=r++; form.add(fromF, g);
        g.gridy=r++; g.insets=new Insets(6,0,2,0); form.add(enabledCb, g);
        g.gridy=r++; g.insets=new Insets(2,0,6,0); form.add(tlsCb, g);

        JLabel statusLbl = statusLabel();
        g.gridy=r++; g.insets=new Insets(4,0,2,0); form.add(statusLbl, g);

        JPanel btnRow = new JPanel(new GridLayout(1,2,8,0));
        btnRow.setOpaque(false);
        JButton saveBtn = UITheme.createPrimaryButton("Save SMTP Settings");
        JButton testBtn = UITheme.createAccentButton("Send Test Email");
        btnRow.add(saveBtn); btnRow.add(testBtn);
        g.gridy=r++; g.insets=new Insets(6,0,0,0); form.add(btnRow, g);

        g.gridy=r++; g.insets=new Insets(8,0,2,0); form.add(lbl("Test Email Address"), g);
        g.gridy=r++; g.insets=new Insets(0,0,0,0); form.add(testEmailF, g);

        card.add(form, BorderLayout.CENTER);

        saveBtn.addActionListener(e -> {
            saveSetting(report.EmailService.KEY_HOST,    hostF.getText().trim());
            saveSetting(report.EmailService.KEY_PORT,    portF.getText().trim());
            saveSetting(report.EmailService.KEY_USER,    userF.getText().trim());
            saveSetting(report.EmailService.KEY_PASS,    new String(passF.getPassword()));
            saveSetting(report.EmailService.KEY_FROM,    fromF.getText().trim());
            saveSetting(report.EmailService.KEY_ENABLED, String.valueOf(enabledCb.isSelected()));
            saveSetting(report.EmailService.KEY_TLS,     String.valueOf(tlsCb.isSelected()));
            ok(statusLbl, "SMTP settings saved!");
            audit("Updated SMTP email settings: host=" + hostF.getText().trim()
                + ", port=" + portF.getText().trim() + ", enabled=" + enabledCb.isSelected());
            Logic.SystemSettings.reload();
        });

        testBtn.addActionListener(e -> {
            String to = testEmailF.getText().trim();
            if (to.isEmpty()) { err(statusLbl, "Enter a test email address first."); return; }
            statusLbl.setForeground(UITheme.ACCENT); statusLbl.setText("Sending...");
            new Thread(() -> {
                String result = report.EmailService.testConnection(to);
                SwingUtilities.invokeLater(() -> {
                    if (result == null) ok(statusLbl, "Test email sent to " + to + "!");
                    else err(statusLbl, "Failed: " + result);
                });
            }).start();
        });

        return card;
    }

    private void backupDatabase(JLabel statusLbl) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Database Backup");
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        fc.setSelectedFile(new java.io.File("electric_billing_backup_" + ts + ".sql"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQL Files (*.sql)", "sql"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String path = fc.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".sql")) path += ".sql";
        final String finalPath = path;

        ok(statusLbl, "Backing up...");
        new Thread(() -> {
            try {
                // Try to find mysqldump
                String[] candidates = {
                    "mysqldump",
                    "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
                    "C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysqldump.exe",
                    "C:\\xampp\\mysql\\bin\\mysqldump.exe"
                };
                String mysqldump = "mysqldump";
                for (String c : candidates) {
                    if (new java.io.File(c).exists()) { mysqldump = c; break; }
                }

                // Read DB credentials from DatabaseManager URL
                String user = "root";
                String pass = "";
                String dbName = "electric_billing_db";

                ProcessBuilder pb = new ProcessBuilder(
                    mysqldump, "-u", user,
                    (pass.isEmpty() ? "--password=" : "-p" + pass),
                    "--single-transaction", "--routines", "--triggers",
                    dbName);
                pb.redirectOutput(new java.io.File(finalPath));
                pb.redirectErrorStream(false);
                Process proc = pb.start();
                int exit = proc.waitFor();

                SwingUtilities.invokeLater(() -> {
                    if (exit == 0) {
                        ok(statusLbl, "Backup saved: " + new java.io.File(finalPath).getName());
                        audit("Database backup created: " + finalPath);
                        JOptionPane.showMessageDialog(this,
                            "Backup completed successfully!\n" + finalPath,
                            "Backup Complete", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        err(statusLbl, "Backup failed (exit code " + exit + "). Check mysqldump path.");
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    err(statusLbl, "Backup error: " + ex.getMessage()));
            }
        }, "BackupThread").start();
    }

    private void restoreDatabase(JLabel statusLbl) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "<html><b>Warning:</b> Restoring will overwrite ALL current data!<br>" +
            "Make sure you have a recent backup before proceeding.<br><br>" +
            "Continue with restore?</html>",
            "Confirm Restore", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Backup File to Restore");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQL Files (*.sql)", "sql"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String path = fc.getSelectedFile().getAbsolutePath();
        ok(statusLbl, "Restoring...");

        new Thread(() -> {
            try {
                String[] candidates = {
                    "mysql",
                    "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe",
                    "C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysql.exe",
                    "C:\\xampp\\mysql\\bin\\mysql.exe"
                };
                String mysql = "mysql";
                for (String c : candidates) {
                    if (new java.io.File(c).exists()) { mysql = c; break; }
                }

                String user   = "root";
                String pass   = "";
                String dbName = "electric_billing_db";

                ProcessBuilder pb = new ProcessBuilder(
                    mysql, "-u", user,
                    (pass.isEmpty() ? "--password=" : "-p" + pass),
                    dbName);
                pb.redirectInput(new java.io.File(path));
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                int exit = proc.waitFor();

                SwingUtilities.invokeLater(() -> {
                    if (exit == 0) {
                        ok(statusLbl, "Restore completed successfully!");
                        audit("Database restored from: " + path);
                        JOptionPane.showMessageDialog(this,
                            "Database restored successfully!\nPlease restart the application.",
                            "Restore Complete", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        err(statusLbl, "Restore failed (exit code " + exit + ").");
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    err(statusLbl, "Restore error: " + ex.getMessage()));
            }
        }, "RestoreThread").start();
    }

    // ── Settings persistence ──────────────────────────────────────────────────

    private String getSetting(String key, String def) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT setting_value FROM system_settings WHERE setting_key=?")) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (SQLException ignored) {}
        return def;
    }

    private void saveSetting(String key, String value) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO system_settings (setting_key, setting_value) VALUES (?,?) " +
                 "ON DUPLICATE KEY UPDATE setting_value=?")) {
            ps.setString(1, key); ps.setString(2, value); ps.setString(3, value);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.weightx = 1;
        g.insets = new Insets(4, 0, 4, 0);
        return g;
    }

    private JTextField field(String val) {
        JTextField f = UITheme.createTextField();
        f.setText(val); f.setPreferredSize(new Dimension(0, 36));
        return f;
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(UITheme.FONT_LABEL); l.setForeground(UITheme.TEXT_LIGHT);
        return l;
    }

    private JLabel statusLabel() {
        JLabel l = new JLabel(" ", SwingConstants.CENTER);
        l.setFont(UITheme.FONT_SMALL);
        return l;
    }

    private JCheckBox checkbox(String text, boolean selected) {
        JCheckBox cb = new JCheckBox(text, selected);
        cb.setFont(UITheme.FONT_BODY);
        cb.setForeground(UITheme.TEXT_LIGHT);
        cb.setOpaque(false); cb.setFocusPainted(false);
        return cb;
    }

    private JComboBox<String> styledCombo(String[] items, String selected) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(UITheme.FONT_BODY);
        cb.setBackground(new Color(20,40,80));
        cb.setForeground(UITheme.TEXT_WHITE);
        cb.setBorder(BorderFactory.createLineBorder(UITheme.ACCENT, 1));
        cb.setPreferredSize(new Dimension(0, 36));
        cb.setSelectedItem(selected);
        return cb;
    }

    private void ok(JLabel l, String msg)  { l.setForeground(UITheme.SUCCESS); l.setText(msg); }
    private void err(JLabel l, String msg) { l.setForeground(UITheme.DANGER);  l.setText(msg); }

    /** Log a settings change to the audit trail. */
    private void audit(String details) {
        Logic.AuditLogger.log(currentUser.getUserId(), currentUser.getUsername(),
            Logic.AuditLogger.Action.SETTINGS_CHANGE, details);
    }
}
