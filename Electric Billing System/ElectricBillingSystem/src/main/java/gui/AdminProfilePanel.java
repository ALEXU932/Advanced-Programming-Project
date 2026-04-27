package gui;

import db.DatabaseManager;
import models.User;
import utils.PasswordUtils;
import utils.ProfilePicUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AdminProfilePanel extends JPanel {

    private final User currentUser;
    private AvatarPanel avatarPanel;

    public AdminProfilePanel(User currentUser) {
        this.currentUser = currentUser;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("Admin Settings");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 3, 16, 0));
        grid.setOpaque(false);
        grid.add(buildAvatarCard());
        grid.add(buildProfileCard());
        grid.add(buildAdminManagementCard());
        add(grid, BorderLayout.CENTER);
    }

    // ── Avatar card ───────────────────────────────────────────────────────────

    private JPanel buildAvatarCard() {
        JPanel card = UITheme.createCard("Profile Picture");
        card.setLayout(new BorderLayout(0, 10));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        avatarPanel = new AvatarPanel(110);
        avatarPanel.setInitials(currentUser.getUsername());
        avatarPanel.setRingColor(UITheme.PRIMARY);
        if (currentUser.getProfilePic() != null)
            avatarPanel.setImage(currentUser.getProfilePic());
        avatarPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel userLbl = new JLabel("@" + currentUser.getUsername(), SwingConstants.CENTER);
        userLbl.setFont(UITheme.FONT_LABEL);
        userLbl.setForeground(UITheme.TEXT_MUTED);
        userLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel roleLbl = new JLabel("ADMINISTRATOR", SwingConstants.CENTER);
        roleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        roleLbl.setForeground(UITheme.PRIMARY);
        roleLbl.setOpaque(true);
        roleLbl.setBackground(new Color(255, 140, 0, 30));
        roleLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.PRIMARY, 1),
            BorderFactory.createEmptyBorder(3, 12, 3, 12)));
        roleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel picStatus = new JLabel(" ", SwingConstants.CENTER);
        picStatus.setFont(UITheme.FONT_SMALL);
        picStatus.setForeground(UITheme.SUCCESS);
        picStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton uploadBtn = UITheme.createPrimaryButton("Upload Photo");
        uploadBtn.setMaximumSize(new Dimension(150, 34));
        uploadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton removeBtn = UITheme.createDangerButton("Remove Photo");
        removeBtn.setMaximumSize(new Dimension(150, 30));
        removeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeBtn.setVisible(currentUser.getProfilePic() != null);

        center.add(avatarPanel);
        center.add(Box.createVerticalStrut(10));
        center.add(userLbl);
        center.add(Box.createVerticalStrut(6));
        center.add(roleLbl);
        center.add(Box.createVerticalStrut(14));
        center.add(uploadBtn);
        center.add(Box.createVerticalStrut(7));
        center.add(removeBtn);
        center.add(Box.createVerticalStrut(8));
        center.add(picStatus);
        card.add(center, BorderLayout.CENTER);

        uploadBtn.addActionListener(e -> {
            String path = ProfilePicUtils.chooseAndSave(this, currentUser.getUserId());
            if (path == null) return;
            if (savePic(path)) {
                currentUser.setProfilePic(path);
                avatarPanel.setImage(path);
                removeBtn.setVisible(true);
                picStatus.setForeground(UITheme.SUCCESS);
                picStatus.setText("Photo updated!");
                refreshAllAvatars(path);
                utils.AuditLogger.log(currentUser.getUserId(), currentUser.getUsername(),
                    utils.AuditLogger.Action.UPLOAD_PHOTO, "Admin uploaded profile picture");
            } else {
                picStatus.setForeground(UITheme.DANGER);
                picStatus.setText("Failed to save.");
            }
        });

        removeBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Remove profile picture?",
                "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            if (savePic(null)) {
                ProfilePicUtils.delete(currentUser.getUserId());
                currentUser.setProfilePic(null);
                avatarPanel.setImage(null);
                avatarPanel.setInitials(currentUser.getUsername());
                removeBtn.setVisible(false);
                picStatus.setForeground(UITheme.TEXT_MUTED);
                picStatus.setText("Photo removed.");
                refreshAllAvatars(null);
                utils.AuditLogger.log(currentUser.getUserId(), currentUser.getUsername(),
                    utils.AuditLogger.Action.UPLOAD_PHOTO, "Admin removed profile picture");
            }
        });

        return card;
    }

    // ── Profile / credentials card ────────────────────────────────────────────

    private JPanel buildProfileCard() {
        JPanel card = UITheme.createCard("My Credentials");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.weightx = 1;
        gbc.insets = new Insets(5, 0, 5, 0);

        JTextField usernameF = UITheme.createTextField();
        usernameF.setText(currentUser.getUsername());
        usernameF.setPreferredSize(new Dimension(0, 36));

        JPasswordField currPassF    = passField();
        JPasswordField newPassF     = passField();
        JPasswordField confirmPassF = passField();

        // Password strength bar
        JProgressBar strengthBar = new JProgressBar(0, 100);
        strengthBar.setPreferredSize(new Dimension(0, 7));
        strengthBar.setBorderPainted(false);
        strengthBar.setBackground(new Color(30, 50, 90));
        strengthBar.setForeground(UITheme.DANGER);

        JLabel strengthLbl = new JLabel("Password strength", SwingConstants.RIGHT);
        strengthLbl.setFont(UITheme.FONT_SMALL);
        strengthLbl.setForeground(UITheme.TEXT_MUTED);

        newPassF.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                String p = new String(newPassF.getPassword());
                int score = passwordScore(p);
                strengthBar.setValue(score);
                if      (score < 30) { strengthBar.setForeground(UITheme.DANGER);  strengthLbl.setText("Weak"); }
                else if (score < 60) { strengthBar.setForeground(UITheme.WARNING); strengthLbl.setText("Fair"); }
                else if (score < 80) { strengthBar.setForeground(UITheme.ACCENT);  strengthLbl.setText("Good"); }
                else                 { strengthBar.setForeground(UITheme.SUCCESS);  strengthLbl.setText("Strong"); }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        JLabel statusLbl = new JLabel(" ", SwingConstants.CENTER);
        statusLbl.setFont(UITheme.FONT_SMALL);

        int r = 0;
        gbc.gridy = r++; form.add(lbl("Username"), gbc);
        gbc.gridy = r++; form.add(usernameF, gbc);
        gbc.gridy = r++; form.add(lbl("Current Password *"), gbc);
        gbc.gridy = r++; form.add(currPassF, gbc);
        gbc.gridy = r++; form.add(lbl("New Password"), gbc);
        gbc.gridy = r++; form.add(newPassF, gbc);
        gbc.gridy = r++; form.add(strengthBar, gbc);
        gbc.gridy = r++; form.add(strengthLbl, gbc);
        gbc.gridy = r++; form.add(lbl("Confirm New Password"), gbc);
        gbc.gridy = r++; form.add(confirmPassF, gbc);
        gbc.gridy = r++; gbc.insets = new Insets(6,0,2,0); form.add(statusLbl, gbc);

        JButton saveBtn = UITheme.createPrimaryButton("Update Credentials");
        gbc.gridy = r++; gbc.insets = new Insets(8,0,0,0);
        form.add(saveBtn, gbc);
        card.add(form, BorderLayout.CENTER);

        saveBtn.addActionListener(e -> {
            String newUsername = usernameF.getText().trim();
            String curr = new String(currPassF.getPassword());
            String newP = new String(newPassF.getPassword());
            String conf = new String(confirmPassF.getPassword());

            if (newUsername.isEmpty()) { status(statusLbl, "Username cannot be empty.", false); return; }
            if (curr.isEmpty())        { status(statusLbl, "Current password is required.", false); return; }

            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                // Verify current password
                PreparedStatement check = conn.prepareStatement(
                    "SELECT password_hash FROM users WHERE user_id=?");
                check.setInt(1, currentUser.getUserId());
                ResultSet rs = check.executeQuery();
                if (!rs.next() || !PasswordUtils.verify(curr, rs.getString(1))) {
                    status(statusLbl, "Current password is incorrect.", false); return;
                }

                // Build update query
                boolean changePass = !newP.isEmpty();
                if (changePass) {
                    if (!newP.equals(conf)) { status(statusLbl, "New passwords don't match.", false); return; }
                    String vmsg = utils.SystemSettings.validatePassword(newP);
                    if (vmsg != null) { status(statusLbl, vmsg, false); return; }
                }

                PreparedStatement upd = conn.prepareStatement(changePass
                    ? "UPDATE users SET username=?, password_hash=? WHERE user_id=?"
                    : "UPDATE users SET username=? WHERE user_id=?");
                upd.setString(1, newUsername);
                if (changePass) { upd.setString(2, PasswordUtils.hash(newP)); upd.setInt(3, currentUser.getUserId()); }
                else            { upd.setInt(2, currentUser.getUserId()); }
                upd.executeUpdate();

                currentUser.setUsername(newUsername);
                status(statusLbl, "Credentials updated successfully!", true);
                currPassF.setText(""); newPassF.setText(""); confirmPassF.setText("");
                strengthBar.setValue(0);
                // Audit log
                utils.AuditLogger.log(currentUser.getUserId(), currentUser.getUsername(),
                    changePass ? utils.AuditLogger.Action.CHANGE_PASSWORD : utils.AuditLogger.Action.UPDATE_PROFILE,
                    changePass ? "Admin changed password and/or username" : "Admin updated username to: " + newUsername);

            } catch (SQLIntegrityConstraintViolationException ex) {
                status(statusLbl, "Username already taken.", false);
            } catch (SQLException ex) {
                status(statusLbl, "Error: " + ex.getMessage(), false);
            }
        });

        return card;
    }

    // ── Admin user management card ────────────────────────────────────────────

    private JPanel buildAdminManagementCard() {
        JPanel card = UITheme.createCard("Admin Users");
        card.setLayout(new BorderLayout(0, 10));

        String[] cols = {"ID", "Username", "Created"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable adminTable = new JTable(model);
        UITheme.styleTable(adminTable);
        adminTable.setRowHeight(28);
        adminTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        adminTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        adminTable.getColumnModel().getColumn(2).setPreferredWidth(90);

        loadAdmins(model);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        JButton addAdminBtn    = UITheme.createPrimaryButton("+ Add Admin");
        JButton deleteAdminBtn = UITheme.createDangerButton("Delete");
        addAdminBtn.setPreferredSize(new Dimension(110, 32));
        deleteAdminBtn.setPreferredSize(new Dimension(80, 32));
        btnRow.add(addAdminBtn);
        btnRow.add(deleteAdminBtn);

        card.add(UITheme.createScrollPane(adminTable), BorderLayout.CENTER);
        card.add(btnRow, BorderLayout.SOUTH);

        addAdminBtn.addActionListener(e -> showAddAdminDialog(model));
        deleteAdminBtn.addActionListener(e -> {
            int row = adminTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select an admin to delete."); return; }
            int uid = (int) model.getValueAt(row, 0);
            if (uid == currentUser.getUserId()) {
                JOptionPane.showMessageDialog(this, "You cannot delete your own account.");
                return;
            }
            String uname = (String) model.getValueAt(row, 1);
            if (JOptionPane.showConfirmDialog(this,
                "<html>Delete admin <b>" + uname + "</b>?</html>",
                "Confirm", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE user_id=?")) {
                ps.setInt(1, uid); ps.executeUpdate();
                utils.AuditLogger.log(currentUser.getUserId(), currentUser.getUsername(),
                    utils.AuditLogger.Action.DELETE_ADMIN,
                    "Deleted admin account: " + uname + " (ID=" + uid + ")");
                loadAdmins(model);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        return card;
    }

    private void loadAdmins(DefaultTableModel model) {
        model.setRowCount(0);
        String sql = "SELECT user_id, username, DATE(created_at) FROM users WHERE role='ADMIN' ORDER BY user_id";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) model.addRow(new Object[]{
                rs.getInt(1), rs.getString(2), rs.getString(3)
            });
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void showAddAdminDialog(DefaultTableModel model) {
        FormDialog dialog = new FormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "+ Add New Admin",
            "Create a new administrator account",
            560, 340);

        JTextField usernameF = FormDialog.makeField("");
        JPasswordField passF = FormDialog.makePassField();
        JPasswordField confF = FormDialog.makePassField();

        dialog.addFieldRow("Username *", usernameF, "Password *", passF);
        dialog.addField("Confirm Password *", confF);
        dialog.addStatus();
        dialog.addCancelButton();
        JButton saveBtn = dialog.addSaveButton("  Create Admin  ");

        saveBtn.addActionListener(e -> {
            String username = usernameF.getText().trim();
            String pass = new String(passF.getPassword());
            String conf = new String(confF.getPassword());
            if (username.isEmpty()) { dialog.setStatus("Username is required.", true); return; }
            String vmsg = utils.SystemSettings.validatePassword(pass);
            if (vmsg != null) { dialog.setStatus(vmsg, true); return; }
            if (!pass.equals(conf)) { dialog.setStatus("Passwords don't match.", true); return; }

            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                conn.setAutoCommit(false);
                PreparedStatement ps1 = conn.prepareStatement(
                    "INSERT INTO users (username,password_hash,role) VALUES (?,?,'ADMIN')",
                    Statement.RETURN_GENERATED_KEYS);
                ps1.setString(1, username); ps1.setString(2, PasswordUtils.hash(pass));
                ps1.executeUpdate();
                ResultSet keys = ps1.getGeneratedKeys();
                int uid = keys.next() ? keys.getInt(1) : -1;
                PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO admins (user_id, role) VALUES (?, 'ADMIN')");
                ps2.setInt(1, uid); ps2.executeUpdate();
                conn.commit();
                loadAdmins(model);
                dialog.dispose();
                utils.AuditLogger.log(currentUser.getUserId(), currentUser.getUsername(),
                    utils.AuditLogger.Action.ADD_ADMIN,
                    "Created new admin account: " + username);
            } catch (SQLIntegrityConstraintViolationException ex) {
                dialog.setStatus("Username already exists.", true);
            } catch (SQLException ex) {
                dialog.setStatus("Error: " + ex.getMessage(), true);
            }
        });

        dialog.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean savePic(String path) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE users SET profile_pic=? WHERE user_id=?")) {
            if (path != null) ps.setString(1, path); else ps.setNull(1, Types.VARCHAR);
            ps.setInt(2, currentUser.getUserId());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    private void refreshAllAvatars(String path) {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) updateAvatars(w, path);
    }

    private void updateAvatars(Container c, String path) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof AvatarPanel && comp != avatarPanel) {
                AvatarPanel ap = (AvatarPanel) comp;
                if (path != null) ap.setImage(path);
                else { ap.setImage(null); ap.setInitials(currentUser.getUsername()); }
                ap.repaint();
            }
            if (comp instanceof Container) updateAvatars((Container) comp, path);
        }
    }

    private int passwordScore(String p) {
        int s = 0;
        if (p.length() >= 6)  s += 20;
        if (p.length() >= 10) s += 20;
        if (p.matches(".*[A-Z].*")) s += 20;
        if (p.matches(".*[0-9].*")) s += 20;
        if (p.matches(".*[^a-zA-Z0-9].*")) s += 20;
        return s;
    }

    private JPasswordField passField() {
        JPasswordField f = UITheme.createPasswordField();
        f.setPreferredSize(new Dimension(0, 36));
        return f;
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(UITheme.FONT_LABEL); l.setForeground(UITheme.TEXT_LIGHT);
        return l;
    }

    private void status(JLabel l, String msg, boolean ok) {
        l.setText(msg);
        l.setForeground(ok ? UITheme.SUCCESS : UITheme.DANGER);
    }
}
