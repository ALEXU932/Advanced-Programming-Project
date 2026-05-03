import database.DatabaseManager;
import gui.LoginFrame;
import logic.SystemSettings;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Set system look and feel base, then override with custom theme
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("OptionPane.background", new java.awt.Color(10, 20, 55));
            UIManager.put("Panel.background", new java.awt.Color(10, 20, 55));
            UIManager.put("OptionPane.messageForeground", java.awt.Color.WHITE);
            UIManager.put("Button.background", new java.awt.Color(255, 140, 0));
            UIManager.put("Button.foreground", java.awt.Color.WHITE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            // Test DB connection
            if (!DatabaseManager.getInstance().testConnection()) {
                JOptionPane.showMessageDialog(null,
                    "Cannot connect to MySQL database.\n\n" +
                    "Please ensure:\n" +
                    "1. MySQL is running\n" +
                    "2. Database 'electric_billing_db' exists (run schema.sql)\n" +
                    "3. Update DB credentials in DatabaseManager.java\n\n" +
                    "The application will start but database features won't work.",
                    "Database Connection Warning",
                    JOptionPane.WARNING_MESSAGE);
            } else {
                // Auto-migrate schema on every startup (safe, idempotent)
                DatabaseManager.getInstance().runMigrations();
                // Load system settings into memory
                SystemSettings.load();
            }
            new LoginFrame().setVisible(true);
        });
    }
}
