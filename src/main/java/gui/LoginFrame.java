package gui;

import Logic.AuditLogger;
import Logic.PasswordUtils;
import Logic.SessionManager;
import database.DatabaseManager;
import database.User;
import java.awt.*;
import java.sql.*;
import javax.swing.*;

/**
 * LoginFrame is the application entry point for user authentication.
 * It presents the login form and handles credentials validation.
 */
public class LoginFrame extends JFrame {

    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JLabel         statusLabel;
    private JLabel         attemptsLabel;

    /**
     * Constructs the login frame with default settings and builds the UI.
     */
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

    /**
     * Builds the main user interface components and layout.
     */
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

    /**
     * Builds the left panel containing the avatar and system branding.
     * @return the configured avatar panel
     */
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

    /**
     * Builds the right panel containing the login form fields and buttons.
     * @return the configured form panel
     */
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

        // Username wrapper with rounded corners
        JPanel userWrapper = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 40, 80, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(UITheme.ACCENT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
            }
        };
        userWrapper.setOpaque(false);
        userWrapper.setPreferredSize(new Dimension(0, UITheme.dim(40)));

        // Remove border from usernameField since wrapper handles it
        usernameField.setBorder(BorderFactory.createEmptyBorder(0, UITheme.dim(10), 0, UITheme.dim(10)));
        usernameField.setOpaque(false);

        GridBagConstraints uw = new GridBagConstraints();
        uw.fill = GridBagConstraints.BOTH; uw.weighty = 1;
        uw.gridy = 0; uw.gridx = 0; uw.weightx = 1;
        userWrapper.add(usernameField, uw);

        passwordField = UITheme.createPasswordField();
        passwordField.setPreferredSize(new Dimension(0, UITheme.dim(40)));

        // Password wrapper: lock icon | password field | eye toggle
        final boolean[] passFocused = {false};
        JPanel passWrapper = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (passFocused[0]) {
                    // Active state: lighter inner fill + brighter border
                    g2.setColor(new Color(30, 60, 120, 220));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    // Inner glow layer
                    g2.setColor(new Color(0, 180, 255, 30));
                    g2.fillRoundRect(3, 3, getWidth()-6, getHeight()-6, 16, 16);
                    g2.setColor(UITheme.ACCENT);
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                } else {
                    g2.setColor(new Color(20, 40, 80, 200));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    g2.setColor(UITheme.ACCENT);
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                }
                g2.dispose();
            }
        };
        passWrapper.setOpaque(false);
        passWrapper.setPreferredSize(new Dimension(0, UITheme.dim(40)));

        // Lock icon label
        JLabel lockIcon = new JLabel("\uD83D\uDD12");
        lockIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, UITheme.dim(14)));
        lockIcon.setForeground(UITheme.TEXT_MUTED);
        lockIcon.setBorder(BorderFactory.createEmptyBorder(0, UITheme.dim(8), 0, UITheme.dim(4)));

        // Remove border from passwordField since wrapper handles it
        passwordField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        passwordField.setOpaque(false);

        // Eye toggle button — drawn with Graphics2D (no emoji font dependency)
        final boolean[] visible = {false};
        JButton eyeBtn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Hover highlight
                if (getModel().isRollover()) {
                    g2.setColor(new Color(0, 180, 255, 30));
                    g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 6, 6);
                }
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                Color eyeColor = visible[0] ? UITheme.ACCENT : UITheme.TEXT_MUTED;
                g2.setColor(eyeColor);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // Outer eye shape (almond)
                int ew = UITheme.dim(14), eh = UITheme.dim(8);
                g2.drawArc(cx - ew/2, cy - eh/2, ew, eh, 0, 180);
                g2.drawArc(cx - ew/2, cy - eh/2, ew, eh, 180, 180);
                // Pupil circle
                int pr = UITheme.dim(3);
                g2.fillOval(cx - pr, cy - pr, pr*2, pr*2);
                // Slash when hidden
                if (!visible[0]) {
                    g2.setColor(UITheme.TEXT_MUTED);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx - UITheme.dim(7), cy + UITheme.dim(5),
                                cx + UITheme.dim(7), cy - UITheme.dim(5));
                }
                g2.dispose();
            }
        };
        eyeBtn.setContentAreaFilled(false);
        eyeBtn.setBorderPainted(false);
        eyeBtn.setFocusPainted(false);
        eyeBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, UITheme.dim(14)));
        eyeBtn.setForeground(UITheme.TEXT_LIGHT);
        eyeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        int eyeSize = UITheme.dim(28);
        eyeBtn.setPreferredSize(new Dimension(eyeSize, eyeSize));
        eyeBtn.setMinimumSize(new Dimension(eyeSize, eyeSize));
        eyeBtn.setMaximumSize(new Dimension(eyeSize, eyeSize));
        eyeBtn.setBorder(BorderFactory.createEmptyBorder(0, UITheme.dim(4), 0, UITheme.dim(6)));
        eyeBtn.setToolTipText("Show / Hide password");

        // Toggle show/hide on click
        eyeBtn.addActionListener(e -> {
            visible[0] = !visible[0];
            passwordField.setEchoChar(visible[0] ? (char) 0 : '\u2022');
            eyeBtn.repaint();
        });

        // Active/inactive state for password wrapper
        passwordField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                passFocused[0] = true;
                passWrapper.repaint();
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                passFocused[0] = false;
                passWrapper.repaint();
            }
        });

        GridBagConstraints pw = new GridBagConstraints();
        pw.fill = GridBagConstraints.BOTH; pw.weighty = 1;
        pw.gridy = 0;

        pw.gridx = 0; pw.weightx = 0; passWrapper.add(lockIcon, pw);
        pw.gridx = 1; pw.weightx = 1; passWrapper.add(passwordField, pw);
        pw.gridx = 2; pw.weightx = 0; passWrapper.add(eyeBtn, pw);

        // Status labels
        attemptsLabel = new JLabel(" ", SwingConstants.CENTER);
        attemptsLabel.setFont(UITheme.FONT_SMALL);
        attemptsLabel.setForeground(UITheme.WARNING);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.DANGER);

        // Buttons row
        JButton loginBtn  = UITheme.createAccentButton("\u2192  Login");
        loginBtn.setPreferredSize(new Dimension(0, UITheme.dim(44)));

        // Align button to the field column (same 32/68 split as fieldRow)
        JPanel btnRow = new JPanel(new GridBagLayout());
        btnRow.setOpaque(false);
        GridBagConstraints br = new GridBagConstraints();
        br.fill = GridBagConstraints.HORIZONTAL; br.gridy = 0;
        br.gridx = 0; br.weightx = 0.32; btnRow.add(Box.createHorizontalGlue(), br);
        br.gridx = 1; br.weightx = 0.68; btnRow.add(loginBtn, br);

        // Forgot password link
        JButton forgotBtn = new JButton("Forgot password?");
        forgotBtn.setFont(UITheme.FONT_SMALL);
        forgotBtn.setForeground(UITheme.ACCENT);
        forgotBtn.setBackground(new Color(0, 0, 0, 0));
        forgotBtn.setBorderPainted(false);
        forgotBtn.setContentAreaFilled(false);
        forgotBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel forgotRow = new JPanel(new GridBagLayout());
        forgotRow.setOpaque(false);
        GridBagConstraints fr = new GridBagConstraints();
        fr.fill = GridBagConstraints.HORIZONTAL; fr.gridy = 0;
        fr.gridx = 0; fr.weightx = 0.32; forgotRow.add(Box.createHorizontalGlue(), fr);
        fr.gridx = 1; fr.weightx = 0.68; forgotRow.add(forgotBtn, fr);

        // Layout
        int r = 0;
        g.gridy = r++; g.insets = new Insets(0,0,2,0); panel.add(welcome, g);
        g.gridy = r++; g.insets = new Insets(0,0,18,0); panel.add(subLbl, g);

        g.gridy = r++; g.insets = new Insets(4,0,2,0); panel.add(fieldRow("User Name: *", userWrapper), g);
        g.gridy = r++; g.insets = new Insets(4,0,2,0); panel.add(fieldRow("Password: *",  passWrapper), g);

        g.gridy = r++; g.insets = new Insets(4,0,2,0); panel.add(attemptsLabel, g);
        g.gridy = r++; g.insets = new Insets(0,0,10,0); panel.add(statusLabel, g);

        g.gridy = r++; g.insets = new Insets(4,0,8,0); panel.add(btnRow, g);
        g.gridy = r++; g.insets = new Insets(0,0,0,0); panel.add(forgotRow, g);

        // Actions
        loginBtn.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());
        usernameField.addActionListener(e -> doLogin());
        forgotBtn.addActionListener(e -> openForgotPassword());

        return panel;
    }

    // ── Field row helper: label on left, field on right ───────────────────────

    /**
     * Creates a row panel with a label on the left and a field on the right.
     * Handles required field indicators with asterisks.
     * @param labelText the label text, with '*' for required fields
     * @param field the input field component
     * @return the configured row panel
     */
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

    /**
     * Handles the login process: validates input, checks lockout status,
     * authenticates user, and opens the appropriate dashboard.
     */
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
                int mins = Logic.SystemSettings.getLockoutMinutes();
                statusLabel.setText("Account locked for " + mins + " minute(s).");
                attemptsLabel.setText("Too many failed attempts.");
            }
            passwordField.setText("");
        }
    }

    /**
     * Authenticates the user by querying the database and verifying the password.
     * @param username the username to authenticate
     * @param password the plain text password
     * @return the User object if authentication succeeds, null otherwise
     */
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

    /**
     * Opens the forgot password dialog to allow users to reset their password.
     * Verifies identity via username and email, then allows setting a new password.
     */
    private void openForgotPassword() {
        // Step 1: ask for username + email to verify identity
        JTextField userF  = UITheme.createTextField();
        JTextField emailF = UITheme.createTextField();
        userF.setPreferredSize(new Dimension(UITheme.dim(220), UITheme.dim(36)));
        emailF.setPreferredSize(new Dimension(UITheme.dim(220), UITheme.dim(36)));

        JPanel verifyPanel = new JPanel(new GridBagLayout());
        verifyPanel.setBackground(new Color(10, 20, 55));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        g.insets = new Insets(5, 8, 5, 8);

        JLabel info = new JLabel("<html><div style='width:280px;color:#C8DCFF'>" +
            "Enter your username and the email address on your account.<br>" +
            "If they match, you can set a new password.</div></html>");
        info.setFont(UITheme.FONT_SMALL);

        g.gridy = 0; g.gridwidth = 2; verifyPanel.add(info, g);
        g.gridy = 1; g.gridwidth = 1; verifyPanel.add(mkLbl("Username:"), g);
        g.gridx = 1; verifyPanel.add(userF, g);
        g.gridy = 2; g.gridx = 0; verifyPanel.add(mkLbl("Email on account:"), g);
        g.gridx = 1; verifyPanel.add(emailF, g);

        int result = javax.swing.JOptionPane.showConfirmDialog(
            this, verifyPanel, "Forgot Password \u2014 Verify Identity",
            javax.swing.JOptionPane.OK_CANCEL_OPTION,
            javax.swing.JOptionPane.PLAIN_MESSAGE);

        if (result != javax.swing.JOptionPane.OK_OPTION) return;

        String username = userF.getText().trim();
        String email    = emailF.getText().trim();
        if (username.isEmpty() || email.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Please enter both username and email.", "Error",
                javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Verify username + email match in DB
        int[] userId = {-1};
        String checkSql = "SELECT u.user_id FROM users u " +
                          "JOIN customers c ON c.user_id = u.user_id " +
                          "WHERE u.username = ? AND LOWER(c.email) = LOWER(?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, username); ps.setString(2, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) userId[0] = rs.getInt(1);
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Database error: " + ex.getMessage(), "Error",
                javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (userId[0] < 0) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "No account found with that username and email combination.\n" +
                "Please contact your administrator.",
                "Not Found", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Step 2: ask for new password
        JPasswordField newPassF    = UITheme.createPasswordField();
        JPasswordField confirmPassF = UITheme.createPasswordField();
        newPassF.setPreferredSize(new Dimension(UITheme.dim(220), UITheme.dim(36)));
        confirmPassF.setPreferredSize(new Dimension(UITheme.dim(220), UITheme.dim(36)));

        JPanel resetPanel = new JPanel(new GridBagLayout());
        resetPanel.setBackground(new Color(10, 20, 55));
        GridBagConstraints g2 = new GridBagConstraints();
        g2.fill = GridBagConstraints.HORIZONTAL; g2.weightx = 1;
        g2.insets = new Insets(5, 8, 5, 8);

        JLabel info2 = new JLabel("<html><div style='width:280px;color:#C8DCFF'>" +
            "Identity verified. Enter your new password below.</div></html>");
        info2.setFont(UITheme.FONT_SMALL);

        g2.gridy = 0; g2.gridwidth = 2; resetPanel.add(info2, g2);
        g2.gridy = 1; g2.gridwidth = 1; resetPanel.add(mkLbl("New Password:"), g2);
        g2.gridx = 1; resetPanel.add(newPassF, g2);
        g2.gridy = 2; g2.gridx = 0; resetPanel.add(mkLbl("Confirm Password:"), g2);
        g2.gridx = 1; resetPanel.add(confirmPassF, g2);

        int result2 = javax.swing.JOptionPane.showConfirmDialog(
            this, resetPanel, "Forgot Password \u2014 Set New Password",
            javax.swing.JOptionPane.OK_CANCEL_OPTION,
            javax.swing.JOptionPane.PLAIN_MESSAGE);

        if (result2 != javax.swing.JOptionPane.OK_OPTION) return;

        String newPass  = new String(newPassF.getPassword());
        String confirm  = new String(confirmPassF.getPassword());

        if (newPass.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Password cannot be empty.", "Error",
                javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!newPass.equals(confirm)) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Passwords do not match.", "Error",
                javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (newPass.length() < 6) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Password must be at least 6 characters.", "Error",
                javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Save new password hash
        String updateSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, Logic.PasswordUtils.hash(newPass));
            ps.setInt(2, userId[0]);
            ps.executeUpdate();
            javax.swing.JOptionPane.showMessageDialog(this,
                "\u2705 Password reset successfully!\nYou can now log in with your new password.",
                "Success", javax.swing.JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Failed to update password: " + ex.getMessage(), "Error",
                javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Creates a styled label with default font and color for form labels.
     * @param text the label text
     * @return the configured label
     */
    private JLabel mkLbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_LABEL);
        l.setForeground(UITheme.TEXT_LIGHT);
        return l;
    }
}
