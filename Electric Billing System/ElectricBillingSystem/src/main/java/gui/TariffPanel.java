package gui;

import db.DatabaseManager;
import models.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class TariffPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;

    public TariffPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadData();
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        JLabel title = new JLabel("Tariff Management");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton addBtn     = UITheme.createPrimaryButton("+ Add Tariff");
        JButton editBtn    = UITheme.createAccentButton("✏  Edit");
        JButton toggleBtn  = UITheme.createAccentButton("Toggle Active");
        JButton refreshBtn = UITheme.createAccentButton("↻  Refresh");
        for (JButton b : new JButton[]{addBtn, editBtn, toggleBtn, refreshBtn})
            b.setPreferredSize(new Dimension(b.getPreferredSize().width, 36));
        actions.add(addBtn); actions.add(editBtn);
        actions.add(toggleBtn); actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────────
        String[] cols = {"ID", "Tariff Name", "Rate ($/kWh)", "Fixed Charge ($)", "Start Date", "End Date", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.setRowHeight(32);

        int[] widths = {45, 200, 110, 120, 110, 110, 90};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Center-align numeric columns
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        center.setBackground(UITheme.TABLE_ROW1);
        center.setForeground(UITheme.TEXT_WHITE);
        for (int i : new int[]{0, 2, 3, 4, 5})
            table.getColumnModel().getColumn(i).setCellRenderer(center);

        // Color-code Status column
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setOpaque(true);
                String s = v != null ? v.toString() : "";
                setForeground(s.contains("Active") ? UITheme.SUCCESS : UITheme.TEXT_MUTED);
                setBackground(sel ? UITheme.PRIMARY : UITheme.TABLE_ROW1);
                return this;
            }
        });

        JPanel card = UITheme.createCard(null);
        card.setLayout(new BorderLayout());
        card.add(UITheme.createScrollPane(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        // ── Listeners ─────────────────────────────────────────────────────────
        addBtn.addActionListener(e -> showDialog(null));
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { info("Select a tariff to edit."); return; }
            showDialog(row);
        });
        toggleBtn.addActionListener(e -> toggleActive());
        refreshBtn.addActionListener(e -> loadData());
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        tableModel.setRowCount(0);
        String sql = "SELECT tariff_id, name, rate_per_kwh, fixed_charge, " +
                     "start_date, COALESCE(CAST(end_date AS CHAR),'—'), is_active " +
                     "FROM tariffs ORDER BY tariff_id DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2),
                    String.format("$%.4f", rs.getDouble(3)),
                    String.format("$%.2f",  rs.getDouble(4)),
                    rs.getString(5), rs.getString(6),
                    rs.getBoolean(7) ? "✓ Active" : "✗ Inactive"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void toggleActive() {
        int row = table.getSelectedRow();
        if (row < 0) { info("Select a tariff first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE tariffs SET is_active = NOT is_active WHERE tariff_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            User au = utils.SessionManager.getCurrentUser();
            if (au != null) utils.AuditLogger.log(au.getUserId(), au.getUsername(),
                utils.AuditLogger.Action.TOGGLE_TARIFF,
                "Toggled tariff ID=" + id + " (" + tableModel.getValueAt(row, 1) + ")");
            loadData();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ── Dialog ────────────────────────────────────────────────────────────────

    private void showDialog(Integer editRow) {
        boolean isEdit = editRow != null;

        FormDialog dialog = new FormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            isEdit ? "✏  Edit Tariff" : "+  Add New Tariff",
            isEdit ? "Update tariff details below" : "Fields marked * are required",
            620, 400);

        // Pre-fill values if editing
        JTextField nameF  = FormDialog.makeField(isEdit ? (String) tableModel.getValueAt(editRow, 1) : "");
        JTextField rateF  = FormDialog.makeField(isEdit ?
            tableModel.getValueAt(editRow, 2).toString().replace("$", "") : "");
        JTextField fixedF = FormDialog.makeField(isEdit ?
            tableModel.getValueAt(editRow, 3).toString().replace("$", "") : "");
        JTextField startF = FormDialog.makeField(isEdit ?
            tableModel.getValueAt(editRow, 4).toString() :
            java.time.LocalDate.now().toString());
        JTextField endF   = FormDialog.makeField(
            isEdit && !"—".equals(tableModel.getValueAt(editRow, 5)) ?
            tableModel.getValueAt(editRow, 5).toString() : "");

        // Live preview label
        JLabel previewLbl = new JLabel("  Enter rate and fixed charge to preview", SwingConstants.LEFT);
        previewLbl.setFont(UITheme.FONT_LABEL);
        previewLbl.setForeground(UITheme.ACCENT);
        previewLbl.setOpaque(true);
        previewLbl.setBackground(new Color(0, 50, 90, 110));
        previewLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.ACCENT, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        // Update preview on input
        javax.swing.event.DocumentListener previewUpdater = new javax.swing.event.DocumentListener() {
            void update() {
                try {
                    double rate  = Double.parseDouble(rateF.getText().trim());
                    double fixed = Double.parseDouble(fixedF.getText().trim());
                    // Example: 200 kWh bill
                    double sample = 200 * rate + fixed;
                    previewLbl.setText(String.format(
                        "  Preview: 200 kWh × $%.4f + $%.2f fixed = $%.2f total",
                        rate, fixed, sample));
                    previewLbl.setForeground(UITheme.SUCCESS);
                } catch (NumberFormatException ex) {
                    previewLbl.setText("  Enter rate and fixed charge to preview");
                    previewLbl.setForeground(UITheme.ACCENT);
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        };
        rateF.getDocument().addDocumentListener(previewUpdater);
        fixedF.getDocument().addDocumentListener(previewUpdater);

        // Layout: 2-column grid
        dialog.addField("Tariff Name *", nameF);
        dialog.addFieldRow("Rate per kWh ($) *", rateF, "Fixed Charge ($) *", fixedF);
        dialog.addFieldRow("Start Date (YYYY-MM-DD) *", startF, "End Date (YYYY-MM-DD)", endF);
        dialog.addField("Billing Preview (200 kWh sample)", previewLbl);
        dialog.addStatus();

        dialog.addCancelButton();
        JButton saveBtn = dialog.addSaveButton(isEdit ? "  Update Tariff  " : "  Save Tariff  ");

        saveBtn.addActionListener(e -> {
            String name  = nameF.getText().trim();
            String rateS = rateF.getText().trim();
            String fixS  = fixedF.getText().trim();
            String start = startF.getText().trim();
            String end   = endF.getText().trim();

            if (name.isEmpty())  { dialog.setStatus("Tariff Name is required.", true);  return; }
            if (rateS.isEmpty()) { dialog.setStatus("Rate per kWh is required.", true); return; }
            if (fixS.isEmpty())  { dialog.setStatus("Fixed Charge is required.", true); return; }
            if (start.isEmpty()) { dialog.setStatus("Start Date is required.", true);   return; }

            try {
                double rate  = Double.parseDouble(rateS);
                double fixed = Double.parseDouble(fixS);
                if (rate < 0 || fixed < 0) { dialog.setStatus("Values cannot be negative.", true); return; }

                try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                    if (isEdit) {
                        int tariffId = (int) tableModel.getValueAt(editRow, 0);
                        PreparedStatement ps = conn.prepareStatement(
                            "UPDATE tariffs SET name=?,rate_per_kwh=?,fixed_charge=?,start_date=?,end_date=? WHERE tariff_id=?");
                        ps.setString(1, name);
                        ps.setDouble(2, rate);
                        ps.setDouble(3, fixed);
                        ps.setString(4, start);
                        if (!end.isEmpty()) ps.setString(5, end); else ps.setNull(5, Types.DATE);
                        ps.setInt(6, tariffId);
                        ps.executeUpdate();
                        User au = utils.SessionManager.getCurrentUser();
                        if (au != null) utils.AuditLogger.log(au.getUserId(), au.getUsername(),
                            utils.AuditLogger.Action.EDIT_TARIFF,
                            "Updated tariff: " + name + " (ID=" + tariffId + "), rate=$" + rate + "/kWh");
                    } else {
                        PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO tariffs (name,rate_per_kwh,fixed_charge,start_date,end_date,is_active) VALUES (?,?,?,?,?,TRUE)");
                        ps.setString(1, name);
                        ps.setDouble(2, rate);
                        ps.setDouble(3, fixed);
                        ps.setString(4, start);
                        if (!end.isEmpty()) ps.setString(5, end); else ps.setNull(5, Types.DATE);
                        ps.executeUpdate();
                        User au = utils.SessionManager.getCurrentUser();
                        if (au != null) utils.AuditLogger.log(au.getUserId(), au.getUsername(),
                            utils.AuditLogger.Action.ADD_TARIFF,
                            "Added tariff: " + name + ", rate=$" + rate + "/kWh, fixed=$" + fixed);
                    }
                }
                loadData();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                dialog.setStatus("Rate and Fixed Charge must be valid numbers.", true);
            } catch (SQLException ex) {
                dialog.setStatus("Error: " + ex.getMessage(), true);
            }
        });

        dialog.setVisible(true);
    }

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}
