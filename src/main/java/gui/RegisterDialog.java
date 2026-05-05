package gui;

import database.DatabaseManager;
import Logic.PasswordUtils;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class RegisterDialog extends JDialog {
    private JTextField nameField, emailField, phoneField, addressField, meterField, usernameField;
    private JPasswordField passField, confirmPassField;
    private JComboBox<String> roleCombo;
    private JLabel statusLabel;
    private JLabel meterLabel;

    public RegisterDialog(Frame parent) {
        super(parent, "User Registration", true);
        setSize(500, 680);
        setLocationRelativeTo(parent);
        setResizable(true);
        buildUI();
    }

    private void buildUI() {
        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.setOverlayAlpha(0.6f);
        setContentPane(root);

        JPanel card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(10, 20, 55, 230));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(255, 140, 0, 80));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);
        gbc.gridx = 0; gbc.weightx = 1;

        // Title
        JLabel title = new JLabel("User Registration", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.PRIMARY);
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 16, 0);
        card.add(title, gbc);

        // Role selector
        gbc.gridy = 1; gbc.insets = new Insets(4, 0, 1, 0);
        card.add(UITheme.createLabel("Register As"), gbc);

        roleCombo = new JComboBox<>(new String[]{"CUSTOMER", "ADMIN"});
        roleCombo.setFont(UITheme.FONT_BODY);
        roleCombo.setBackground(new Color(20, 40, 80));
        roleCombo.setForeground(UITheme.TEXT_WHITE);
        roleCombo.setBorder(BorderFactory.createLineBorder(UITheme.PRIMARY, 1));
        roleCombo.setRenderer((list, value, index, sel, focus) -> {
            JLabel lbl = new JLabel(value == null ? "" : (String) value);
            lbl.setFont(UITheme.FONT_BODY);
            lbl.setForeground(UITheme.TEXT_WHITE);
            lbl.setBackground(sel ? UITheme.PRIMARY : new Color(20, 40, 80));
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            // Show icon per role
            lbl.setText(value != null && value.equals("ADMIN") ? "🔑  ADMIN" : "👤  CUSTOMER");
            return lbl;
        });
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 8, 0);
        card.add(roleCombo, gbc);

        // Role description label
        JLabel roleDescLbl = new JLabel("👤 Customer: Can view bills, readings and AI insights", SwingConstants.CENTER);
        roleDescLbl.setFont(UITheme.FONT_SMALL);
        roleDescLbl.setForeground(UITheme.ACCENT);
        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 10, 0);
        card.add(roleDescLbl, gbc);

        // Fields
        nameField      = UITheme.createTextField();
        emailField     = UITheme.createTextField();
        phoneField     = UITheme.createTextField();
        addressField   = UITheme.createTextField();
        meterField     = UITheme.createTextField();
        usernameField  = UITheme.createTextField();
        passField      = UITheme.createPasswordField();
        confirmPassField = UITheme.createPasswordField();

        int row = 4;

        gbc.gridy = row++; gbc.insets = new Insets(4, 0, 1, 0);
        card.add(UITheme.createLabel("Full Name *"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 4, 0);
        card.add(nameField, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(4, 0, 1, 0);
        card.add(UITheme.createLabel("Email"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 4, 0);
        card.add(emailField, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(4, 0, 1, 0);
        card.add(UITheme.createLabel("Phone"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 4, 0);
        card.add(phoneField, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(4, 0, 1, 0);
        card.add(UITheme.createLabel("Address"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 4, 0);
        card.add(addressField, gbc);

        // Meter number — only visible for CUSTOMER
        meterLabel = UITheme.createLabel("Meter Number *");
        gbc.gridy = row++; gbc.insets = new Insets(4, 0, 1, 0);
        card.add(meterLabel, gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 4, 0);
        card.add(meterField, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(4, 0, 1, 0);
        card.add(UITheme.createLabel("Username *"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 4, 0);
        card.add(usernameField, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(4, 0, 1, 0);
        card.add(UITheme.createLabel("Password *"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 4, 0);
        card.add(passField, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(4, 0, 1, 0);
        card.add(UITheme.createLabel("Confirm Password *"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 4, 0);
        card.add(confirmPassField, gbc);

        // Status
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.DANGER);
        gbc.gridy = row++; gbc.insets = new Insets(4, 0, 4, 0);
        card.add(statusLabel, gbc);

        // Register button
        JButton registerBtn = UITheme.createPrimaryButton("REGISTER");
        gbc.gridy = row++;
        card.add(registerBtn, gbc);

        JScrollPane scroll = new JScrollPane(card);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        root.add(scroll, BorderLayout.CENTER);

        // Role change listener — update description and meter visibility
        roleCombo.addActionListener(e -> {
            boolean isAdmin = "ADMIN".equals(roleCombo.getSelectedItem());
            meterLabel.setVisible(!isAdmin);
            meterField.setVisible(!isAdmin);
            if (isAdmin) {
                roleDescLbl.setText("🔑 Admin: Full access to manage customers, billing & AI analytics");
                roleDescLbl.setForeground(UITheme.PRIMARY);
                meterField.setText("ADMIN-" + System.currentTimeMillis()); // placeholder
            } else {
                roleDescLbl.setText("👤 Customer: Can view bills, readings and AI insights");
                roleDescLbl.setForeground(UITheme.ACCENT);
                meterField.setText("");
            }
            card.revalidate();
            card.repaint();
        });

        registerBtn.addActionListener(e -> doRegister());
    }

    private void doRegister() {
        String role    = (String) roleCombo.getSelectedItem();
        String name    = nameField.getText().trim();
        String email   = emailField.getText().trim();
        String phone   = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String meter   = meterField.getText().trim();
        String username = usernameField.getText().trim();
        String pass    = new String(passField.getPassword());
        String confirm = new String(confirmPassField.getPassword());

        // Validation
        if (name.isEmpty() || username.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Name, Username and Password are required.");
            return;
        }
        if ("CUSTOMER".equals(role) && meter.isEmpty()) {
            statusLabel.setText("Meter Number is required for customers.");
            return;
        }
        if (!pass.equals(confirm)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }
        String passErr = Logic.SystemSettings.validatePassword(pass);
        if (passErr != null) { statusLabel.setText(passErr); return; }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            // Insert user
            PreparedStatement ps1 = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            ps1.setString(1, username);
            ps1.setString(2, PasswordUtils.hash(pass));
            ps1.setString(3, role);
            ps1.executeUpdate();
            ResultSet keys = ps1.getGeneratedKeys();
            int userId = keys.next() ? keys.getInt(1) : -1;

            // For CUSTOMER role, also insert into customers + meters tables
            if ("CUSTOMER".equals(role)) {
                PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO customers (user_id, name, email, phone, address) VALUES (?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps2.setInt(1, userId);
                ps2.setString(2, name);
                ps2.setString(3, email);
                ps2.setString(4, phone);
                ps2.setString(5, address);
                ps2.executeUpdate();
                ResultSet ckeys = ps2.getGeneratedKeys();
                int customerId = ckeys.next() ? ckeys.getInt(1) : -1;

                // Insert meter record
                PreparedStatement ps3 = conn.prepareStatement(
                    "INSERT INTO meters (meter_number, customer_id, meter_type, status) VALUES (?,?,?,?)");
                ps3.setString(1, meter);
                ps3.setInt(2, customerId);
                ps3.setString(3, "SINGLE_PHASE");
                ps3.setString(4, "ACTIVE");
                ps3.executeUpdate();
            }

            conn.commit();

            String msg = "ADMIN".equals(role)
                ? "Admin account created! You can now login with admin privileges."
                : "Registration successful! You can now login.";
            JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (SQLIntegrityConstraintViolationException ex) {
            try { DatabaseManager.getInstance().getConnection().rollback(); } catch (Exception ignored) {}
            statusLabel.setText("Username or Meter Number already exists.");
        } catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }
}
