package gui;

import db.DatabaseManager;
import models.Customer;
import models.User;
import utils.PasswordUtils;
import utils.ProfilePicUtils;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class CustomerProfilePanel extends JPanel {
    private final Customer customer;
    private final User     user;
    private AvatarPanel    avatarPanel;
    private JLabel         picStatusLbl;

    public CustomerProfilePanel(Customer customer, User user) {
        this.customer = customer;
        this.user     = user;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("My Profile");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 3, 16, 0));
        grid.setOpaque(false);
        grid.add(buildAvatarCard());
        grid.add(buildProfileCard());
        grid.add(buildPasswordCard());
        add(grid, BorderLayout.CENTER);
    }

    // ── Avatar card ───────────────────────────────────────────────────────────

    private JPanel buildAvatarCard() {
        JPanel card = UITheme.createCard("Profile Picture");
        card.setLayout(new BorderLayout(0, 12));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Large avatar
        avatarPanel = new AvatarPanel(120);
        avatarPanel.setInitials(customer.getName());
        avatarPanel.setRingColor(UITheme.ACCENT);
        if (user.getProfilePic() != null)
            avatarPanel.setImage(user.getProfilePic());
        avatarPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Username label
        JLabel usernameLbl = new JLabel("@" + user.getUsername(), SwingConstants.CENTER);
        usernameLbl.setFont(UITheme.FONT_LABEL);
        usernameLbl.setForeground(UITheme.TEXT_MUTED);
        usernameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Role badge
        JLabel roleLbl = new JLabel(user.getRole(), SwingConstants.CENTER);
        roleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        roleLbl.setForeground(UITheme.PRIMARY);
        roleLbl.setOpaque(true);
        roleLbl.setBackground(new Color(255, 140, 0, 30));
        roleLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.PRIMARY, 1),
            BorderFactory.createEmptyBorder(3, 12, 3, 12)));
        roleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Status label
        picStatusLbl = new JLabel(" ", SwingConstants.CENTER);
        picStatusLbl.setFont(UITheme.FONT_SMALL);
        picStatusLbl.setForeground(UITheme.SUCCESS);
        picStatusLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Upload button
        JButton uploadBtn = UITheme.createPrimaryButton("Upload Photo");
        uploadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        uploadBtn.setMaximumSize(new Dimension(160, 36));

        // Remove button
        JButton removeBtn = UITheme.createDangerButton("Remove Photo");
        removeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeBtn.setMaximumSize(new Dimension(160, 36));
        removeBtn.setVisible(user.getProfilePic() != null);

        center.add(avatarPanel);
        center.add(Box.createVerticalStrut(12));
        center.add(usernameLbl);
        center.add(Box.createVerticalStrut(6));
        center.add(roleLbl);
        center.add(Box.createVerticalStrut(14));
        center.add(uploadBtn);
        center.add(Box.createVerticalStrut(8));
        center.add(removeBtn);
        center.add(Box.createVerticalStrut(8));
        center.add(picStatusLbl);

        card.add(center, BorderLayout.CENTER);

        // ── Upload action ─────────────────────────────────────────────────────
        uploadBtn.addActionListener(e -> {
            String path = ProfilePicUtils.chooseAndSave(this, user.getUserId());
            if (path == null) return;
            if (savePicPath(path)) {
                user.setProfilePic(path);
                avatarPanel.setImage(path);
                removeBtn.setVisible(true);
                picStatusLbl.setForeground(UITheme.SUCCESS);
                picStatusLbl.setText("Profile picture updated!");
                // Refresh header avatar in parent dashboard
                refreshHeaderAvatar(path);
                utils.AuditLogger.log(user.getUserId(), user.getUsername(),
                    utils.AuditLogger.Action.UPLOAD_PHOTO, "Customer uploaded profile picture");
            } else {
                picStatusLbl.setForeground(UITheme.DANGER);
                picStatusLbl.setText("Failed to save. Try again.");
            }
        });

        // ── Remove action ─────────────────────────────────────────────────────
        removeBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Remove your profile picture?", "Confirm",
                JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            if (savePicPath(null)) {
                ProfilePicUtils.delete(user.getUserId());
                user.setProfilePic(null);
                avatarPanel.setImage(null);
                removeBtn.setVisible(false);
                picStatusLbl.setForeground(UITheme.TEXT_MUTED);
                picStatusLbl.setText("Profile picture removed.");
                refreshHeaderAvatar(null);
            }
        });

        return card;
    }

    // ── Profile info card ─────────────────────────────────────────────────────

    private JPanel buildProfileCard() {
        JPanel card = UITheme.createCard("Account Information");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx = 0; gbc.weightx = 1;
        gbc.insets = new Insets(5, 0, 5, 0);

        JTextField nameF  = field(customer.getName());
        JTextField emailF = field(nvl(customer.getEmail()));
        JTextField phoneF = field(nvl(customer.getPhone()));
        JTextField addrF  = field(nvl(customer.getAddress()));
        JTextField meterF = field(nvl(customer.getMeterNumber()));
        meterF.setEditable(false);
        meterF.setForeground(UITheme.TEXT_MUTED);

        int r = 0;
        String[][] rows = {{"Full Name *",""}, {"Email",""}, {"Phone",""}, {"Address",""}, {"Meter No. (read-only)",""}};
        JTextField[] inputs = {nameF, emailF, phoneF, addrF, meterF};
        for (int i = 0; i < rows.length; i++) {
            gbc.gridy = r++; form.add(lbl(rows[i][0]), gbc);
            gbc.gridy = r++; form.add(inputs[i], gbc);
        }

        JLabel statusLbl = new JLabel(" ", SwingConstants.CENTER);
        statusLbl.setFont(UITheme.FONT_SMALL);
        gbc.gridy = r++; gbc.insets = new Insets(6,0,2,0);
        form.add(statusLbl, gbc);

        JButton saveBtn = UITheme.createPrimaryButton("Update Profile");
        gbc.gridy = r++; gbc.insets = new Insets(8,0,0,0);
        form.add(saveBtn, gbc);

        card.add(form, BorderLayout.CENTER);

        saveBtn.addActionListener(e -> {
            String name = nameF.getText().trim();
            if (name.isEmpty()) { status(statusLbl, "Name is required.", false); return; }
            String sql = "UPDATE customers SET name=?,email=?,phone=?,address=? WHERE customer_id=?";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name); ps.setString(2, emailF.getText().trim());
                ps.setString(3, phoneF.getText().trim()); ps.setString(4, addrF.getText().trim());
                ps.setInt(5, customer.getCustomerId());
                ps.executeUpdate();
                customer.setName(name);
                avatarPanel.setInitials(name);
                status(statusLbl, "Profile updated successfully!", true);
                utils.AuditLogger.log(user.getUserId(), user.getUsername(),
                    utils.AuditLogger.Action.UPDATE_PROFILE,
                    "Customer updated profile: name=" + name);
            } catch (SQLException ex) {
                status(statusLbl, "Error: " + ex.getMessage(), false);
            }
        });

        return card;
    }

    // ── Password card ─────────────────────────────────────────────────────────

    private JPanel buildPasswordCard() {
        JPanel card = UITheme.createCard("Change Password");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx = 0; gbc.weightx = 1;
        gbc.insets = new Insets(5, 0, 5, 0);

        JPasswordField currF    = passField();
        JPasswordField newF     = passField();
        JPasswordField confirmF = passField();

        // Password strength bar
        JProgressBar strengthBar = new JProgressBar(0, 100);
        strengthBar.setPreferredSize(new Dimension(0, 8));
        strengthBar.setBorderPainted(false);
        strengthBar.setBackground(new Color(30, 50, 90));
        strengthBar.setForeground(UITheme.DANGER);

        JLabel strengthLbl = new JLabel("Password strength", SwingConstants.RIGHT);
        strengthLbl.setFont(UITheme.FONT_SMALL);
        strengthLbl.setForeground(UITheme.TEXT_MUTED);

        newF.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                String p = new String(newF.getPassword());
                int score = passwordScore(p);
                strengthBar.setValue(score);
                if (score < 30)      { strengthBar.setForeground(UITheme.DANGER);  strengthLbl.setText("Weak"); }
                else if (score < 60) { strengthBar.setForeground(UITheme.WARNING); strengthLbl.setText("Fair"); }
                else if (score < 80) { strengthBar.setForeground(UITheme.ACCENT);  strengthLbl.setText("Good"); }
                else                 { strengthBar.setForeground(UITheme.SUCCESS);  strengthLbl.setText("Strong"); }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        int r = 0;
        gbc.gridy = r++; form.add(lbl("Current Password"), gbc);
        gbc.gridy = r++; form.add(currF, gbc);
        gbc.gridy = r++; form.add(lbl("New Password"), gbc);
        gbc.gridy = r++; form.add(newF, gbc);
        gbc.gridy = r++; form.add(strengthBar, gbc);
        gbc.gridy = r++; form.add(strengthLbl, gbc);
        gbc.gridy = r++; form.add(lbl("Confirm New Password"), gbc);
        gbc.gridy = r++; form.add(confirmF, gbc);

        JLabel statusLbl = new JLabel(" ", SwingConstants.CENTER);
        statusLbl.setFont(UITheme.FONT_SMALL);
        gbc.gridy = r++; gbc.insets = new Insets(6,0,2,0);
        form.add(statusLbl, gbc);

        JButton changeBtn = UITheme.createPrimaryButton("Change Password");
        gbc.gridy = r++; gbc.insets = new Insets(8,0,0,0);
        form.add(changeBtn, gbc);

        card.add(form, BorderLayout.CENTER);

        changeBtn.addActionListener(e -> {
            String curr = new String(currF.getPassword());
            String newP = new String(newF.getPassword());
            String conf = new String(confirmF.getPassword());
            if (curr.isEmpty() || newP.isEmpty()) { status(statusLbl, "All fields required.", false); return; }
            if (!newP.equals(conf))               { status(statusLbl, "Passwords don't match.", false); return; }
            if (newP.length() < 6)                { status(statusLbl, "Minimum 6 characters.", false); return; }

            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                PreparedStatement check = conn.prepareStatement(
                    "SELECT password_hash FROM users WHERE user_id=?");
                check.setInt(1, user.getUserId());
                ResultSet rs = check.executeQuery();
                if (rs.next() && PasswordUtils.verify(curr, rs.getString(1))) {
                    PreparedStatement upd = conn.prepareStatement(
                        "UPDATE users SET password_hash=? WHERE user_id=?");
                    upd.setString(1, PasswordUtils.hash(newP));
                    upd.setInt(2, user.getUserId());
                    upd.executeUpdate();
                    status(statusLbl, "Password changed successfully!", true);
                    utils.AuditLogger.log(user.getUserId(), user.getUsername(),
                        utils.AuditLogger.Action.CHANGE_PASSWORD, "Customer changed password");
                    currF.setText(""); newF.setText(""); confirmF.setText("");
                    strengthBar.setValue(0);
                } else {
                    status(statusLbl, "Current password is incorrect.", false);
                }
            } catch (SQLException ex) {
                status(statusLbl, "Error: " + ex.getMessage(), false);
            }
        });

        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean savePicPath(String path) {
        String sql = "UPDATE users SET profile_pic=? WHERE user_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (path != null) ps.setString(1, path); else ps.setNull(1, java.sql.Types.VARCHAR);
            ps.setInt(2, user.getUserId());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    /** Refresh the avatar in the parent dashboard header. */
    private void refreshHeaderAvatar(String path) {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w == null) return;
        // Walk component tree to find AvatarPanel in header
        updateAvatars(w, path);
    }

    private void updateAvatars(Container c, String path) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof AvatarPanel) {
                AvatarPanel ap = (AvatarPanel) comp;
                if (path != null) ap.setImage(path);
                else { ap.setImage(null); ap.setInitials(customer.getName()); }
                ap.repaint();
            }
            if (comp instanceof Container) updateAvatars((Container) comp, path);
        }
    }

    private int passwordScore(String p) {
        int score = 0;
        if (p.length() >= 6)  score += 20;
        if (p.length() >= 10) score += 20;
        if (p.matches(".*[A-Z].*")) score += 20;
        if (p.matches(".*[0-9].*")) score += 20;
        if (p.matches(".*[^a-zA-Z0-9].*")) score += 20;
        return score;
    }

    private JTextField field(String val) {
        JTextField f = UITheme.createTextField();
        f.setText(val != null ? val : "");
        f.setPreferredSize(new Dimension(0, 36));
        return f;
    }

    private JPasswordField passField() {
        JPasswordField f = UITheme.createPasswordField();
        f.setPreferredSize(new Dimension(0, 36));
        return f;
    }

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_LABEL);
        l.setForeground(UITheme.TEXT_LIGHT);
        return l;
    }

    private void status(JLabel lbl, String msg, boolean ok) {
        lbl.setText(msg);
        lbl.setForeground(ok ? UITheme.SUCCESS : UITheme.DANGER);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
