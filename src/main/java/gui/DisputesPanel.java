package gui;

import database.DatabaseManager;
import database.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * DisputesPanel allows administrators to view and manage customer bill
 * dispute cases, including status filtering and resolution actions.
 */
public class DisputesPanel extends JPanel {

    private final User currentUser;
    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<String> statusFilter;

    public DisputesPanel(User currentUser) {
        this.currentUser = currentUser;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("Bill Disputes");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        statusFilter = new JComboBox<>(new String[]{"All","OPEN","UNDER_REVIEW","RESOLVED","REJECTED"});
        statusFilter.setFont(UITheme.FONT_BODY);
        statusFilter.setBackground(new Color(20,40,80));
        statusFilter.setForeground(UITheme.TEXT_WHITE);
        statusFilter.setPreferredSize(new Dimension(140, 36));

        JButton resolveBtn = UITheme.createPrimaryButton("Resolve");
        JButton rejectBtn  = UITheme.createDangerButton("Reject");
        JButton reviewBtn  = UITheme.createAccentButton("Mark Under Review");
        JButton refreshBtn = UITheme.createAccentButton("Refresh");
        for (JButton b : new JButton[]{resolveBtn, rejectBtn, reviewBtn, refreshBtn})
            b.setPreferredSize(new Dimension(b.getPreferredSize().width, 36));

        actions.add(statusFilter); actions.add(reviewBtn);
        actions.add(resolveBtn); actions.add(rejectBtn); actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] cols = {"ID","Bill #","Customer","Reason","Description","Status","Created","Resolved"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.setRowHeight(30);
        int[] widths = {45,70,150,120,250,100,110,110};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Color-code status
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                setHorizontalAlignment(SwingConstants.CENTER); setOpaque(true);
                String s = v != null ? v.toString() : "";
                switch (s) {
                    case "OPEN":         setForeground(UITheme.WARNING); break;
                    case "UNDER_REVIEW": setForeground(UITheme.ACCENT);  break;
                    case "RESOLVED":     setForeground(UITheme.SUCCESS); break;
                    case "REJECTED":     setForeground(UITheme.DANGER);  break;
                    default:             setForeground(UITheme.TEXT_LIGHT);
                }
                setBackground(sel ? UITheme.PRIMARY : UITheme.TABLE_ROW1);
                return this;
            }
        });

        JPanel card = UITheme.createCard(null);
        card.setLayout(new BorderLayout());
        card.add(UITheme.createScrollPane(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        statusFilter.addActionListener(e -> loadData());
        refreshBtn.addActionListener(e -> loadData());
        reviewBtn.addActionListener(e -> updateStatus("UNDER_REVIEW"));
        resolveBtn.addActionListener(e -> showResolveDialog());
        rejectBtn.addActionListener(e -> updateStatus("REJECTED"));
    }

    private void loadData() {
        tableModel.setRowCount(0);
        String filter = (String) statusFilter.getSelectedItem();
        String sql = "SELECT d.dispute_id, d.bill_id, c.name, d.reason, d.description, " +
                     "d.status, DATE(d.created_at), DATE(d.resolved_at) " +
                     "FROM disputes d JOIN customers c ON d.customer_id=c.customer_id";
        if (!"All".equals(filter)) sql += " WHERE d.status='" + filter + "'";
        sql += " ORDER BY d.created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) tableModel.addRow(new Object[]{
                rs.getInt(1), String.format("#%06d", rs.getInt(2)),
                rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getString(7),
                rs.getString(8) != null ? rs.getString(8) : "—"
            });
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void updateStatus(String newStatus) {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a dispute first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE disputes SET status=? WHERE dispute_id=?")) {
            ps.setString(1, newStatus); ps.setInt(2, id);
            ps.executeUpdate();
            Logic.AuditLogger.log(currentUser.getUserId(), currentUser.getUsername(),
                Logic.AuditLogger.Action.SETTINGS_CHANGE,
                "Dispute #" + id + " status changed to " + newStatus);
            loadData();
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void showResolveDialog() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a dispute first."); return; }
        int id     = (int) tableModel.getValueAt(row, 0);
        int billId = Integer.parseInt(tableModel.getValueAt(row, 1).toString().replace("#",""));

        FormDialog dialog = new FormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Resolve Dispute #" + String.format("%06d", id),
            "Enter resolution details",
            560, 340);

        JTextField resolutionF = FormDialog.makeField("");
        JTextField adjustedF   = FormDialog.makeField("");

        dialog.addField("Resolution Notes *", resolutionF);
        dialog.addField("Adjusted Bill Amount ($ — leave blank to keep original)", adjustedF);
        dialog.addStatus();
        dialog.addCancelButton();
        JButton saveBtn = dialog.addSaveButton("  Resolve Dispute  ");

        saveBtn.addActionListener(e -> {
            String resolution = resolutionF.getText().trim();
            if (resolution.isEmpty()) { dialog.setStatus("Resolution notes are required.", true); return; }
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                conn.setAutoCommit(false);
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE disputes SET status='RESOLVED', resolution=?, resolved_at=NOW(), resolved_by=? WHERE dispute_id=?");
                ps.setString(1, resolution); ps.setInt(2, currentUser.getUserId()); ps.setInt(3, id);
                ps.executeUpdate();

                String adjStr = adjustedF.getText().trim();
                if (!adjStr.isEmpty()) {
                    try {
                        double adj = Double.parseDouble(adjStr);
                        PreparedStatement ps2 = conn.prepareStatement(
                            "UPDATE bills SET total_amount=? WHERE bill_id=?");
                        ps2.setDouble(1, adj); ps2.setInt(2, billId);
                        ps2.executeUpdate();
                        PreparedStatement ps3 = conn.prepareStatement(
                            "UPDATE disputes SET adjusted_amount=? WHERE dispute_id=?");
                        ps3.setDouble(1, adj); ps3.setInt(2, id);
                        ps3.executeUpdate();
                    } catch (NumberFormatException ex) {
                        dialog.setStatus("Adjusted amount must be a number.", true); return;
                    }
                }
                conn.commit();
                Logic.AuditLogger.log(currentUser.getUserId(), currentUser.getUsername(),
                    Logic.AuditLogger.Action.SETTINGS_CHANGE,
                    "Resolved dispute #" + id + ": " + resolution);
                loadData(); dialog.dispose();
            } catch (SQLException ex) { dialog.setStatus("Error: " + ex.getMessage(), true); }
        });
        dialog.setVisible(true);
    }
}
