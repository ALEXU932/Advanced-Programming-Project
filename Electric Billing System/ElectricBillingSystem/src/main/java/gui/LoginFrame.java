package gui;

import db.DatabaseManager;
import models.User;
import utils.AuditLogger;
import utils.PasswordUtils;
import utils.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {

    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JLabel         statusLabel;
    private JLabel         attemptsLabel;

    public LoginFrame() {
        setTitle("AI-Enhanced Electric Billing System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.min(1000, (int)(screen.width  * 0.65));
        int h = Math.min(680,  (int)(screen.height * 0.80));
        setSize(Math.max(860, w), Math.max(560, h));
        setMinimumSize(new Dimension(800, 520));
        setResizable(true);
        setLocationRelativeTo(null);
        buildUI();
    }

    private void buildUI() {
        BackgroundPanel root = new BackgroundPanel(new GridBagLayout());
        root.setOverlayAlpha(0.5f);
        setContentPane(root);

        // ── Outer card ────────────────────────────────────────────────────────
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(10, 20, 55, 235));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 140, 0, 90));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(UITheme.dim(760), UITheme.dim(420)));

        GridBagConstraints cc = new GridBagConstraints();
        cc.fill = GridBagConstraints.BOTH;
        cc.weightx = 1; cc.weighty = 1;

        // ── LEFT: Avatar panel ────────────────────────────────────────────────
        cc.gridx = 0; cc.gridy = 0;
        cc.weightx = 0.38;
        card.add(buildAvatarPanel(), cc);

        // Vertical divider
        JSeparator divider = new JSeparator(SwingConstants.VERTICAL);
        divider.setForeground(new Color(255, 140, 0, 50));
        divider.setPreferredSize(new Dimension(1, 0));
        cc.gridx = 1; cc.weightx = 0;
        card.add(divider, cc);

        // ── RIGHT: Form panel ─────────────────────────────────────────────────
        cc.gridx = 2; cc.weightx = 0.62;
        card.add(buildFormPanel(), cc);

        root.add(card);
    }

    // ── Left avatar panel ─────────────────────────────────────────────────────

    private JPanel buildAvatarPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 20));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        g.insets = new Insets(6, 0, 6, 0);

        // Large avatar
        AvatarPanel avatar = new AvatarPanel(UITheme.dim(110));
        avatar.setInitials("AD");
        avatar.setRingColor(UITheme.PRIMARY);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel avatarWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        avatarWrapper.setOpaque(false);
        avatarWrapper.add(avatar);

        // System title
        JLabel sysTitle = new JLabel("AI Electric", SwingConstants.CENTER);
        sysTitle.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(20)));
        sysTitle.setForeground(UITheme.PRIMARY);

        JLabel sysSub = new JLabel("Billing System", SwingConstants.CENTER);
        sysSub.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(16)));
        sysSub.setForeground(UITheme.PRIMARY);

        JLabel tagLine = new JLabel("AI-Enhanced Management", SwingConstants.CENTER);
        tagLine.setFont(UITheme.FONT_SMALL);
        tagLine.setForeground(UITheme.TEXT_MUTED);

        // Lightning icon
        JLabel iconLbl = new JLabel("\u26A1", SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, UITheme.dim(36)));
        iconLbl.setForeground(UITheme.PRIMARY);

        g.gridy = 0; panel.add(iconLbl, g);
        g.gridy = 1; panel.add(avatarWrapper, g);
        g.gridy = 2; g.insets = new Insets(10, 0, 2, 0); panel.add(sysTitle, g);
        g.gridy = 3; g.insets = new Insets(0, 0, 4, 0);  panel.add(sysSub, g);
        g.gridy = 4; g.insets = new Insets(0, 0, 0, 0);  panel.add(tagLine, g);

        return panel;
    }

    // ── Right form panel ──────────────────────────────────────────────────────

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 24, 30, 36));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets = new Insets(4, 0, 4, 0);

        // Welcome header
        JLabel welcome = new JLabel("Welcome Back");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(22)));
        welcome.setForeground(UITheme.TEXT_WHITE);

        JLabel subLbl = new JLabel("Sign in to your account");
        subLbl.setFont(UITheme.FONT_SMALL);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        // Fields
        usernameField = UITheme.createTextField();
        usernameField.setPreferredSize(new Dimension(0, UITheme.dim(40)));

        passwordField = UITheme.createPasswordField();
        passwordField.setPreferredSize(new Dimension(0, UITheme.dim(40)));

        // Status labels
        attemptsLabel = new JLabel(" ", SwingConstants.CENTER);
        attemptsLabel.setFont(UITheme.FONT_SMALL);
        attemptsLabel.setForeground(UITheme.WARNING);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.DANGER);

        // Buttons row
        JButton loginBtn  = UITheme.createPrimaryButton("\u2192  Login");
        JButton cancelBtn = UITheme.createDangerButton("\u2715  Cancel");
        loginBtn.setPreferredSize(new Dimension(UITheme.dim(130), UITheme.dim(40)));
        cancelBtn.setPreferredSize(new Dimension(UITheme.dim(110), UITheme.dim(40)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);
        btnRow.add(loginBtn);
        btnRow.add(cancelBtn);

        // Register link
        JButton registerBtn = new JButton("New customer? Register here");
        registerBtn.setFont(UITheme.FONT_SMALL);
        registerBtn.setForeground(UITheme.ACCENT);
        registerBtn.setBackground(new Color(0,0,0,0));
        registerBtn.setBorderPainted(false);
        registerBtn.setContentAreaFilled(false);
        registerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Layout
        int r = 0;
        g.gridy = r++; g.insets = new Insets(0,0,2,0); panel.add(welcome, g);
        g.gridy = r++; g.insets = new Insets(0,0,18,0); panel.add(subLbl, g);

        g.gridy = r++; g.insets = new Insets(4,0,2,0); panel.add(fieldRow("User Name: *", usernameField), g);
        g.gridy = r++; g.insets = new Insets(4,0,2,0); panel.add(fieldRow("Password: *",  passwordField), g);

        g.gridy = r++; g.insets = new Insets(4,0,2,0); panel.add(attemptsLabel, g);
        g.gridy = r++; g.insets = new Insets(0,0,10,0); panel.add(statusLabel, g);

        g.gridy = r++; g.insets = new Insets(4,0,8,0); panel.add(btnRow, g);
        g.gridy = r++; g.insets = new Insets(0,0,0,0); panel.add(registerBtn, g);

        // Actions
        loginBtn.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());
        usernameField.addActionListener(e -> doLogin());
        cancelBtn.addActionListener(e -> {
            usernameField.setText("");
            passwordField.setText("");
            statusLabel.setText(" ");
            attemptsLabel.setText(" ");
            usernameField.requestFocus();
        });
        registerBtn.addActionListener(e -> openRegister());

        return panel;
    }

    // ── Field row helper: label on left, field on right ───────────────────────

    private JPanel fieldRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 8);

        // Label with red asterisk
        JPanel lblPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        lblPanel.setOpaque(false);
        String base = labelText.replace("*", "").trim();
        boolean required = labelText.contains("*");

        JLabel lbl = new JLabel(base);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(12)));
        lbl.setForeground(UITheme.TEXT_LIGHT);
        lblPanel.add(lbl);

        if (required) {
            JLabel star = new JLabel(" *");
            star.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(12)));
            star.setForeground(UITheme.DANGER);
            lblPanel.add(star);
        }

        g.gridx = 0; g.weightx = 0.32; g.gridy = 0;
        row.add(lblPanel, g);

        g.gridx = 1; g.weightx = 0.68; g.insets = new Insets(0,0,0,0);
        row.add(field, g);

        return row;
    }

    // ── Login logic ───────────────────────────────────────────────────────────

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username and password.");
            return;
        }

        if (!SessionManager.canAttemptLogin(username)) {
            int mins = SessionManager.getRemainingLockoutMinutes(username);
            statusLabel.setText("Account locked. Try again in " + mins + " minute(s).");
            return;
        }

        User user = authenticate(username, password);
        if (user != null) {
            SessionManager.clearFailedAttempts(username);
            SessionManager.setCurrentUser(user);
            AuditLogger.log(user.getUserId(), user.getUsername(), AuditLogger.Action.LOGIN,
                "Successful login as " + user.getRole());
            dispose();
            if (user.isAdmin()) new AdminDashboard(user).setVisible(true);
            else                new CustomerDashboard(user).setVisible(true);
        } else {
            SessionManager.recordFailedAttempt(username);
            int failed = SessionManager.getFailedAttempts(username);
            int max    = SessionManager.getMaxAttempts();
            int left   = max - failed;
            if (left > 0) {
                statusLabel.setText("Invalid username or password.");
                attemptsLabel.setText(left + " attempt(s) remaining before lockout.");
            } else {
                int mins = utils.SystemSettings.getLockoutMinutes();
                statusLabel.setText("Account locked for " + mins + " minute(s).");
                attemptsLabel.setText("Too many failed attempts.");
            }
            passwordField.setText("");
        }
    }

    private User authenticate(String username, String password) {
        String sql = "SELECT user_id, username, password_hash, role, profile_pic FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password_hash");
                String role = rs.getString("role");
                if (PasswordUtils.verify(password, hash)) {
                    User u = new User(rs.getInt("user_id"), rs.getString("username"), role);
                    u.setProfilePic(rs.getString("profile_pic"));
                    return u;
                }
            }
        } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
        }
        return null;
    }

    private void openRegister() {
        new RegisterDialog(this).setVisible(true);
    }
}
