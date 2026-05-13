package gui;

import database.DatabaseManager;
import database.Customer;
import database.User;
import Logic.PasswordUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class CustomerManagementPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public CustomerManagementPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("Customer Management");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        searchField = UITheme.createTextField();
        searchField.setPreferredSize(new Dimension(220, 36));
        JButton searchBtn  = UITheme.createAccentButton("Search");
        JButton addBtn     = UITheme.createPrimaryButton("+ Add Customer");
        JButton editBtn    = UITheme.createAccentButton("Edit");
        JButton deleteBtn  = UITheme.createDangerButton("Delete");
        JButton refreshBtn = UITheme.createAccentButton("Refresh");
        JButton exportBtn  = UITheme.createAccentButton("Export Excel");
        for (JButton b : new JButton[]{searchBtn,addBtn,editBtn,deleteBtn,refreshBtn,exportBtn})
            b.setPreferredSize(new Dimension(b.getPreferredSize().width, 36));
        actions.add(searchField); actions.add(searchBtn);
        actions.add(Box.createHorizontalStrut(6));
        actions.add(addBtn); actions.add(editBtn); actions.add(deleteBtn); actions.add(refreshBtn);
        actions.add(exportBtn);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] cols = {"ID","Full Name","Email","Phone","Address","Meter No.","Registered"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.setRowHeight(32);
        int[] widths = {45,160,180,110,200,110,100};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        center.setBackground(UITheme.TABLE_ROW1);
        center.setForeground(UITheme.TEXT_WHITE);
        for (int i : new int[]{0,5,6}) table.getColumnModel().getColumn(i).setCellRenderer(center);

        JPanel card = UITheme.createCard(null);
        card.setLayout(new BorderLayout());
        card.add(UITheme.createScrollPane(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        addBtn.addActionListener(e -> showCustomerDialog(null));
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { info("Select a customer to edit."); return; }
            showCustomerDialog(getCustomerById((int) tableModel.getValueAt(row, 0)));
        });
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> loadData());
        searchBtn.addActionListener(e -> loadData(searchField.getText().trim()));
        searchField.addActionListener(e -> loadData(searchField.getText().trim()));
        exportBtn.addActionListener(e -> report.ExcelExporter.export(this, table, "Customers"));
    }

    private void loadData() { loadData(""); }

    private void loadData(String search) {
        tableModel.setRowCount(0);
        String sql = "SELECT c.customer_id, c.name, c.email, c.phone, c.address, " +
                     "GROUP_CONCAT(m.meter_number ORDER BY m.meter_id SEPARATOR ', ') as meters, " +
                     "DATE(c.created_at) " +
                     "FROM customers c LEFT JOIN meters m ON m.customer_id=c.customer_id";
        if (!search.isEmpty())
            sql += " WHERE c.name LIKE ? OR c.email LIKE ? OR m.meter_number LIKE ?";
        sql += " GROUP BY c.customer_id ORDER BY c.customer_id DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!search.isEmpty()) {
                String like = "%" + search + "%";
                ps.setString(1,like); ps.setString(2,like); ps.setString(3,like);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) tableModel.addRow(new Object[]{
                rs.getInt(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5),
                rs.getString(6) != null ? rs.getString(6) : "—",
                rs.getString(7)
            });
        } catch (SQLException e) { error("Error: " + e.getMessage()); }
    }

    private Customer getCustomerById(int id) {
        String sql = "SELECT c.*, COALESCE(m.meter_number,''), u.user_id " +
                     "FROM customers c LEFT JOIN meters m ON m.customer_id=c.customer_id " +
                     "LEFT JOIN users u ON u.user_id=c.user_id WHERE c.customer_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Customer c = new Customer();
                c.setCustomerId(rs.getInt("customer_id"));
                c.setUserId(rs.getInt("user_id"));
                c.setName(rs.getString("name"));
                c.setEmail(rs.getString("email"));
                c.setPhone(rs.getString("phone"));
                c.setAddress(rs.getString("address"));
                c.setMeterNumber(rs.getString(7));
                return c;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { info("Select a customer to delete."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
            "<html>Delete customer <b>" + name + "</b>?<br><br>" +
            "This will also delete all their:<br>" +
            "• Meter readings<br>• Bills<br>• Payments<br>• AI predictions<br><br>" +
            "<font color='red'>This action cannot be undone.</font></html>",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            // Delete in correct FK order
            exec(conn, "DELETE FROM anomalies       WHERE customer_id=?", id);
            exec(conn, "DELETE FROM usage_log        WHERE customer_id=?", id);
            exec(conn, "DELETE FROM ai_predictions   WHERE customer_id=?", id);
            exec(conn, "DELETE FROM ai_features      WHERE customer_id=?", id);
            // Payments reference bills, bills reference customers
            exec(conn, "DELETE p FROM payments p JOIN bills b ON p.bill_id=b.bill_id WHERE b.customer_id=?", id);
            exec(conn, "DELETE FROM bills             WHERE customer_id=?", id);
            exec(conn, "DELETE FROM meter_readings    WHERE customer_id=?", id);
            exec(conn, "DELETE FROM meters            WHERE customer_id=?", id);
            // Get user_id before deleting customer
            int userId = -1;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT user_id FROM customers WHERE customer_id=?")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) userId = rs.getInt(1);
            }
            exec(conn, "DELETE FROM customers WHERE customer_id=?", id);
            if (userId > 0) {
                exec(conn, "DELETE FROM admins WHERE user_id=?", userId);
                exec(conn, "DELETE FROM users  WHERE user_id=?", userId);
            }
            conn.commit();
            loadData();
            JOptionPane.showMessageDialog(this, "Customer deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            // Audit log
            User u = Logic.SessionManager.getCurrentUser();
            if (u != null) Logic.AuditLogger.log(u.getUserId(), u.getUsername(),
                Logic.AuditLogger.Action.DELETE_CUSTOMER,
                "Deleted customer: " + name + " (ID=" + id + ")");
        } catch (SQLException e) {
            error("Error deleting customer: " + e.getMessage());
        }
    }

    private void exec(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── Add / Edit Dialog ─────────────────────────────────────────────────────

    private void showCustomerDialog(Customer existing) {
        boolean isEdit = existing != null;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            isEdit ? "Edit Customer" : "Add New Customer", true);
        dialog.setSize(800, isEdit ? 500 : 580);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.setOverlayAlpha(0.72f);
        dialog.setContentPane(root);

        // Header bar
        JPanel dh = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8,18,50,240)); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(UITheme.PRIMARY); g2.setStroke(new BasicStroke(2f));
                g2.drawLine(0,getHeight()-1,getWidth(),getHeight()-1); g2.dispose();
            }
        };
        dh.setOpaque(false); dh.setPreferredSize(new Dimension(0,56));
        dh.setBorder(BorderFactory.createEmptyBorder(0,24,0,24));
        JLabel dhTitle = new JLabel(isEdit ? "Edit Customer" : "Add New Customer");
        dhTitle.setFont(new Font("Segoe UI",Font.BOLD,16)); dhTitle.setForeground(UITheme.PRIMARY);
        JLabel dhSub = new JLabel(isEdit ? "Update customer information" : "Fields marked * are required");
        dhSub.setFont(UITheme.FONT_SMALL); dhSub.setForeground(UITheme.TEXT_MUTED);
        dh.add(dhTitle, BorderLayout.WEST); dh.add(dhSub, BorderLayout.EAST);
        root.add(dh, BorderLayout.NORTH);

        // Main content
        JPanel content = new JPanel(new BorderLayout(0,0));
        content.setOpaque(false);

        // LEFT: Avatar side panel
        JPanel avatarSide = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(6,14,40,200)); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(new Color(255,140,0,40));
                g2.drawLine(getWidth()-1,0,getWidth()-1,getHeight()); g2.dispose();
            }
        };
        avatarSide.setOpaque(false);
        avatarSide.setPreferredSize(new Dimension(175,0));
        avatarSide.setLayout(new BoxLayout(avatarSide, BoxLayout.Y_AXIS));
        avatarSide.setBorder(BorderFactory.createEmptyBorder(28,16,16,16));

        AvatarPanel avatarPanel = new AvatarPanel(110);
        avatarPanel.setInitials(isEdit && existing.getName() != null ? existing.getName() : "?");
        avatarPanel.setRingColor(UITheme.PRIMARY);
        avatarPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        final String[] picPath = {null};
        if (isEdit) {
            String p = getUserPic(existing.getUserId());
            if (p != null) { avatarPanel.setImage(p); picPath[0] = p; }
        }

        JLabel picHint = new JLabel("<html><center>Click below<br>to upload photo</center></html>", SwingConstants.CENTER);
        picHint.setFont(UITheme.FONT_SMALL); picHint.setForeground(UITheme.TEXT_MUTED);
        picHint.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton uploadBtn = UITheme.createPrimaryButton("Upload Photo");
        uploadBtn.setFont(new Font("Segoe UI",Font.BOLD,11));
        uploadBtn.setMaximumSize(new Dimension(145,32));
        uploadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton removeBtn = UITheme.createDangerButton("Remove Photo");
        removeBtn.setFont(new Font("Segoe UI",Font.PLAIN,11));
        removeBtn.setMaximumSize(new Dimension(145,28));
        removeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeBtn.setVisible(picPath[0] != null);

        JLabel picStatus = new JLabel(" ", SwingConstants.CENTER);
        picStatus.setFont(new Font("Segoe UI",Font.PLAIN,10));
        picStatus.setForeground(UITheme.SUCCESS);
        picStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        avatarSide.add(avatarPanel);
        avatarSide.add(Box.createVerticalStrut(12));
        avatarSide.add(picHint);
        avatarSide.add(Box.createVerticalStrut(12));
        avatarSide.add(uploadBtn);
        avatarSide.add(Box.createVerticalStrut(7));
        avatarSide.add(removeBtn);
        avatarSide.add(Box.createVerticalStrut(8));
        avatarSide.add(picStatus);
        content.add(avatarSide, BorderLayout.WEST);

        // RIGHT: Form
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(18,20,10,24));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4,6,4,6);
        g.weightx = 1;

        JTextField nameF  = field(isEdit ? existing.getName()    : "");
        JTextField emailF = field(isEdit ? existing.getEmail()   : "");
        JTextField phoneF = field(isEdit ? existing.getPhone()   : "");
        JTextField addrF  = field(isEdit ? existing.getAddress() : "");
        JTextField meterF = field(isEdit ? existing.getMeterNumber() : "");
        JTextField userF  = field("");
        JPasswordField passF = passField();
        if (isEdit) { meterF.setEditable(false); meterF.setForeground(UITheme.TEXT_MUTED); }

        // Live initials update
        nameF.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void u() { avatarPanel.setInitials(nameF.getText().trim()); }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { u(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { u(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { u(); }
        });

        g.gridy=0; g.gridx=0; g.gridwidth=1; body.add(lbl("Full Name *"),g);
        g.gridx=1; body.add(lbl("Email Address"),g);
        g.gridy=1; g.gridx=0; body.add(nameF,g);
        g.gridx=1; body.add(emailF,g);

        g.gridy=2; g.gridx=0; body.add(lbl("Phone Number"),g);
        g.gridx=1; body.add(lbl("Address"),g);
        g.gridy=3; g.gridx=0; body.add(phoneF,g);
        g.gridx=1; body.add(addrF,g);

        g.gridy=4; g.gridx=0; g.gridwidth=2;
        body.add(lbl("Meter Number" + (isEdit ? " (read-only)" : " *")),g);
        g.gridy=5; g.gridx=0; g.gridwidth=2;
        body.add(meterF,g);
        g.gridwidth=1;

        if (!isEdit) {
            g.gridy=6; g.gridx=0; g.gridwidth=1; body.add(lbl("Username *"),g);
            g.gridx=1; body.add(lbl("Password *"),g);
            g.gridy=7; g.gridx=0; body.add(userF,g);
            g.gridx=1; body.add(passF,g);
        }

        JLabel statusLbl = new JLabel(" ", SwingConstants.CENTER);
        statusLbl.setFont(UITheme.FONT_SMALL); statusLbl.setForeground(UITheme.DANGER);
        g.gridy = isEdit ? 6 : 8; g.gridx=0; g.gridwidth=2;
        g.insets = new Insets(6,6,2,6);
        body.add(statusLbl,g);

        content.add(body, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,12)) {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D gd = (Graphics2D) g2.create();
                gd.setColor(new Color(8,18,50,210)); gd.fillRect(0,0,getWidth(),getHeight());
                gd.setColor(new Color(40,60,110)); gd.drawLine(0,0,getWidth(),0); gd.dispose();
            }
        };
        footer.setOpaque(false);
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(UITheme.FONT_BUTTON); cancelBtn.setForeground(UITheme.TEXT_LIGHT);
        cancelBtn.setBackground(new Color(40,60,100)); cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false); cancelBtn.setPreferredSize(new Dimension(110,38));
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JButton saveBtn = UITheme.createPrimaryButton(isEdit ? "  Update  " : "  Save Customer  ");
        saveBtn.setPreferredSize(new Dimension(160,38));
        footer.add(cancelBtn); footer.add(saveBtn);
        root.add(footer, BorderLayout.SOUTH);

        // Upload listener
        uploadBtn.addActionListener(e -> {
            int tempId = isEdit ? existing.getUserId() : -1;
            String p = Logic.ProfilePicUtils.chooseAndSave(dialog, tempId);
            if (p != null) {
                picPath[0] = p;
                avatarPanel.setImage(p);
                removeBtn.setVisible(true);
                picStatus.setText("Photo selected"); picStatus.setForeground(UITheme.SUCCESS);
            }
        });

        removeBtn.addActionListener(e -> {
            picPath[0] = null;
            avatarPanel.setImage(null);
            avatarPanel.setInitials(nameF.getText().trim());
            removeBtn.setVisible(false);
            picStatus.setText("Photo removed"); picStatus.setForeground(UITheme.TEXT_MUTED);
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn.addActionListener(e -> {
            String name  = nameF.getText().trim();
            String email = emailF.getText().trim();
            String phone = phoneF.getText().trim();
            String addr  = addrF.getText().trim();
            String meter = meterF.getText().trim();

            if (name.isEmpty()) { statusLbl.setText("Full Name is required."); return; }
            if (!isEdit && meter.isEmpty()) { statusLbl.setText("Meter Number is required."); return; }

            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                if (isEdit) {
                    PreparedStatement ps = conn.prepareStatement(
                        "UPDATE customers SET name=?,email=?,phone=?,address=? WHERE customer_id=?");
                    ps.setString(1,name); ps.setString(2,email);
                    ps.setString(3,phone); ps.setString(4,addr);
                    ps.setInt(5,existing.getCustomerId()); ps.executeUpdate();
                    // Update pic
                    PreparedStatement pp = conn.prepareStatement(
                        "UPDATE users SET profile_pic=? WHERE user_id=?");
                    if (picPath[0] != null) pp.setString(1,picPath[0]);
                    else pp.setNull(1, Types.VARCHAR);
                    pp.setInt(2,existing.getUserId()); pp.executeUpdate();
                    // Audit log
                    User au = Logic.SessionManager.getCurrentUser();
                    if (au != null) Logic.AuditLogger.log(au.getUserId(), au.getUsername(),
                        Logic.AuditLogger.Action.EDIT_CUSTOMER,
                        "Updated customer: " + name + " (ID=" + existing.getCustomerId() + ")");
                } else {
                    String username = userF.getText().trim();
                    String pass = new String(passF.getPassword());
                    if (username.isEmpty()) { statusLbl.setText("Username is required."); return; }
                    if (pass.length() < 6)  { statusLbl.setText("Password must be at least 6 characters."); return; }

                    conn.setAutoCommit(false);
                    PreparedStatement ps1 = conn.prepareStatement(
                        "INSERT INTO users (username,password_hash,role,profile_pic) VALUES (?,?,'CUSTOMER',?)",
                        Statement.RETURN_GENERATED_KEYS);
                    ps1.setString(1,username); ps1.setString(2,PasswordUtils.hash(pass));
                    if (picPath[0] != null) ps1.setString(3,picPath[0]);
                    else ps1.setNull(3,Types.VARCHAR);
                    ps1.executeUpdate();
                    ResultSet keys = ps1.getGeneratedKeys();
                    int uid = keys.next() ? keys.getInt(1) : -1;

                    // Rename temp pic file to real uid
                    if (picPath[0] != null && uid > 0) {
                        String newPath = Logic.ProfilePicUtils.renameTempPic(-1, uid);
                        if (newPath != null) {
                            PreparedStatement pp = conn.prepareStatement(
                                "UPDATE users SET profile_pic=? WHERE user_id=?");
                            pp.setString(1,newPath); pp.setInt(2,uid); pp.executeUpdate();
                        }
                    }

                    PreparedStatement ps2 = conn.prepareStatement(
                        "INSERT INTO customers (user_id,name,email,phone,address) VALUES (?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                    ps2.setInt(1,uid); ps2.setString(2,name); ps2.setString(3,email);
                    ps2.setString(4,phone); ps2.setString(5,addr); ps2.executeUpdate();
                    ResultSet ckeys = ps2.getGeneratedKeys();
                    int cid = ckeys.next() ? ckeys.getInt(1) : -1;

                    PreparedStatement ps3 = conn.prepareStatement(
                        "INSERT INTO meters (meter_number,customer_id,meter_type,status) VALUES (?,?,?,?)");
                    ps3.setString(1,meter); ps3.setInt(2,cid);
                    ps3.setString(3,"SINGLE_PHASE"); ps3.setString(4,"ACTIVE");
                    ps3.executeUpdate();
                    conn.commit();
                    // Audit log — add customer
                    User au = Logic.SessionManager.getCurrentUser();
                    if (au != null) Logic.AuditLogger.log(au.getUserId(), au.getUsername(),
                        Logic.AuditLogger.Action.ADD_CUSTOMER,
                        "Added customer: " + name + ", meter: " + meter);
                }
                loadData(); dialog.dispose();
            } catch (SQLIntegrityConstraintViolationException ex) {
                statusLbl.setText("Username or Meter Number already exists.");
            } catch (SQLException ex) {
                statusLbl.setText("Error: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }

    private String getUserPic(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT profile_pic FROM users WHERE user_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private JTextField field(String v) {
        JTextField f = UITheme.createTextField();
        f.setText(v != null ? v : "");
        f.setPreferredSize(new Dimension(0,38));
        return f;
    }

    private JPasswordField passField() {
        JPasswordField f = UITheme.createPasswordField();
        f.setPreferredSize(new Dimension(0,38));
        return f;
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(UITheme.FONT_LABEL); l.setForeground(UITheme.TEXT_LIGHT);
        return l;
    }

    private void info(String msg)  { JOptionPane.showMessageDialog(this,msg,"Info",JOptionPane.INFORMATION_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this,msg,"Error",JOptionPane.ERROR_MESSAGE); }
}
