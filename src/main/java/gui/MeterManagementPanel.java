package gui;

import database.DatabaseManager;
import database.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * MeterManagementPanel is a GUI component for managing electric meters in the
 * billing system.
 * It provides functionality to view, add, edit, delete, and search meters, as
 * well as export data to Excel.
 * This panel displays meters in a table format with details like meter number,
 * customer assignment,
 * type, status, location, and installation date.
 */
public class MeterManagementPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    /**
     * Constructs a new MeterManagementPanel.
     * Initializes the UI components and loads the initial meter data.
     */
    public MeterManagementPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadData();
    }

    /**
     * Builds the user interface for the meter management panel.
     * Sets up the header with title and action buttons, configures the table,
     * and adds event listeners for user interactions.
     */
    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("Meter Management");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        searchField = UITheme.createTextField();
        searchField.setPreferredSize(new Dimension(200, 36));
        JButton searchBtn = UITheme.createAccentButton("🔍 Search");
        JButton addBtn = UITheme.createPrimaryButton("\uFF0B Add Meter");
        JButton editBtn = UITheme.createAccentButton("\u270F  Edit");
        JButton deleteBtn = UITheme.createDangerButton("\uD83D\uDDD1  Delete");
        JButton refreshBtn = UITheme.createAccentButton("\u21BB  Refresh");
        JButton exportBtn = UITheme.createAccentButton("Export Excel");
        for (JButton b : new JButton[] { searchBtn, addBtn, editBtn, deleteBtn, refreshBtn, exportBtn })
            b.setPreferredSize(new Dimension(b.getPreferredSize().width, 36));
        actions.add(searchField);
        actions.add(searchBtn);
        actions.add(Box.createHorizontalStrut(6));
        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(deleteBtn);
        actions.add(refreshBtn);
        actions.add(exportBtn);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] cols = { "ID", "Meter Number", "Customer", "Type", "Status", "Location", "Installed" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.setRowHeight(32);
        int[] widths = { 45, 130, 170, 110, 90, 160, 100 };
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        center.setBackground(UITheme.TABLE_ROW1);
        center.setForeground(UITheme.TEXT_WHITE);
        for (int i : new int[] { 0, 3, 6 })
            table.getColumnModel().getColumn(i).setCellRenderer(center);

        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setOpaque(true);
                String s = v != null ? v.toString() : "";
                if ("ACTIVE".equals(s))
                    setForeground(UITheme.SUCCESS);
                else if ("FAULTY".equals(s))
                    setForeground(UITheme.DANGER);
                else if ("INACTIVE".equals(s))
                    setForeground(UITheme.WARNING);
                else
                    setForeground(UITheme.TEXT_MUTED);
                setBackground(sel ? UITheme.PRIMARY : UITheme.TABLE_ROW1);
                return this;
            }
        });

        JPanel card = UITheme.createCard(null);
        card.setLayout(new BorderLayout());
        card.add(UITheme.createScrollPane(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        addBtn.addActionListener(e -> showDialog(null));
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                info("Select a meter to edit.");
                return;
            }
            showDialog(row);
        });
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> loadData());
        searchBtn.addActionListener(e -> loadData(searchField.getText().trim()));
        searchField.addActionListener(e -> loadData(searchField.getText().trim()));
        exportBtn.addActionListener(e -> report.ExcelExporter.export(this, table, "Meters"));
    }

    /**
     * Loads all meter data into the table.
     */
    private void loadData() {
        loadData("");
    }

    /**
     * Loads meter data into the table, optionally filtered by a search term.
     * Searches across meter number, customer name, and location.
     * 
     * @param search the search term to filter meters, or empty string for all
     *               meters
     */
    private void loadData(String search) {
        tableModel.setRowCount(0);
        String sql = "SELECT m.meter_id, m.meter_number, COALESCE(c.name,'— Unassigned —'), " +
                "m.meter_type, m.status, COALESCE(m.location,'—'), " +
                "COALESCE(CAST(m.installed_at AS CHAR),'—') " +
                "FROM meters m LEFT JOIN customers c ON m.customer_id=c.customer_id";
        if (!search.isEmpty())
            sql += " WHERE m.meter_number LIKE ? OR c.name LIKE ? OR m.location LIKE ?";
        sql += " ORDER BY m.meter_id DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!search.isEmpty()) {
                String like = "%" + search + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                tableModel.addRow(new Object[] {
                        rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)
                });
        } catch (SQLException e) {
            error("Error: " + e.getMessage());
        }
    }

    /**
     * Deletes the currently selected meter from the database after user
     * confirmation.
     * Logs the deletion action for audit purposes.
     */
    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            info("Select a meter to delete.");
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        String num = (String) tableModel.getValueAt(row, 1);
        if (JOptionPane.showConfirmDialog(this,
                "<html>Delete meter <b>" + num + "</b>?</html>",
                "Confirm Delete", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
            return;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM meters WHERE meter_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            User au = Logic.SessionManager.getCurrentUser();
            if (au != null)
                Logic.AuditLogger.log(au.getUserId(), au.getUsername(),
                        Logic.AuditLogger.Action.DELETE_METER, "Deleted meter: " + num + " (ID=" + id + ")");
            loadData();
        } catch (SQLException e) {
            error("Error: " + e.getMessage());
        }
    }

    /**
     * Displays a dialog for adding a new meter or editing an existing one.
     * 
     * @param editRow the row index of the meter to edit, or null for adding a new
     *                meter
     */
    private void showDialog(Integer editRow) {
        boolean isEdit = editRow != null;

        FormDialog dialog = new FormDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                isEdit ? "✏  Edit Meter" : "⚡  Add New Meter",
                isEdit ? "Update meter details below" : "Fields marked * are required",
                660, 480);

        JTextField meterNumF = FormDialog.makeField(isEdit ? (String) tableModel.getValueAt(editRow, 1) : "");
        JTextField locationF = FormDialog.makeField(isEdit ? tableModel.getValueAt(editRow, 5).toString() : "");
        JTextField installedF = FormDialog.makeField(
                isEdit ? tableModel.getValueAt(editRow, 6).toString() : java.time.LocalDate.now().toString());

        JComboBox<String> typeCb = FormDialog
                .makeStringCombo(new String[] { "SINGLE_PHASE", "THREE_PHASE", "SMART", "PREPAID" });
        JComboBox<String> statusCb = FormDialog
                .makeStringCombo(new String[] { "ACTIVE", "INACTIVE", "FAULTY", "REPLACED" });

        if (isEdit) {
            typeCb.setSelectedItem(tableModel.getValueAt(editRow, 3));
            statusCb.setSelectedItem(tableModel.getValueAt(editRow, 4));
            meterNumF.setEditable(false);
            meterNumF.setForeground(UITheme.TEXT_MUTED);
        }

        JComboBox<Object> customerCb = FormDialog.makeCombo();
        customerCb.addItem(new Object[] { null, "— Unassigned —" });
        loadCustomers(customerCb);

        dialog.addFieldRow("Meter Number *", meterNumF, "Location", locationF);
        dialog.addFieldRow("Meter Type", typeCb, "Status", statusCb);
        dialog.addFieldRow("Assign to Customer", customerCb, "Install Date (YYYY-MM-DD)", installedF);
        dialog.addStatus();

        dialog.addCancelButton();
        JButton saveBtn = dialog.addSaveButton(isEdit ? "  Update Meter  " : "  Save Meter  ");

        saveBtn.addActionListener(e -> {
            String meterNum = meterNumF.getText().trim();
            if (meterNum.isEmpty()) {
                dialog.setStatus("Meter Number is required.", true);
                return;
            }
            Object sel = customerCb.getSelectedItem();
            Integer customerId = (sel != null && ((Object[]) sel)[0] != null) ? (Integer) ((Object[]) sel)[0] : null;
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                if (isEdit) {
                    int meterId = (int) tableModel.getValueAt(editRow, 0);
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE meters SET customer_id=?,meter_type=?,status=?,location=?,installed_at=? WHERE meter_id=?");
                    if (customerId != null)
                        ps.setInt(1, customerId);
                    else
                        ps.setNull(1, Types.INTEGER);
                    ps.setString(2, (String) typeCb.getSelectedItem());
                    ps.setString(3, (String) statusCb.getSelectedItem());
                    ps.setString(4, locationF.getText().trim());
                    ps.setString(5, installedF.getText().trim());
                    ps.setInt(6, meterId);
                    ps.executeUpdate();
                    User au = Logic.SessionManager.getCurrentUser();
                    if (au != null)
                        Logic.AuditLogger.log(au.getUserId(), au.getUsername(),
                                Logic.AuditLogger.Action.EDIT_METER,
                                "Updated meter ID=" + meterId + ", type=" + typeCb.getSelectedItem() + ", status="
                                        + statusCb.getSelectedItem());
                } else {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO meters (meter_number,customer_id,meter_type,status,location,installed_at) VALUES (?,?,?,?,?,?)");
                    ps.setString(1, meterNum);
                    if (customerId != null)
                        ps.setInt(2, customerId);
                    else
                        ps.setNull(2, Types.INTEGER);
                    ps.setString(3, (String) typeCb.getSelectedItem());
                    ps.setString(4, (String) statusCb.getSelectedItem());
                    ps.setString(5, locationF.getText().trim());
                    ps.setString(6, installedF.getText().trim());
                    ps.executeUpdate();
                    User au = Logic.SessionManager.getCurrentUser();
                    if (au != null)
                        Logic.AuditLogger.log(au.getUserId(), au.getUsername(),
                                Logic.AuditLogger.Action.ADD_METER,
                                "Added meter: " + meterNum + ", type=" + typeCb.getSelectedItem());
                }
                loadData();
                dialog.dispose();
            } catch (SQLIntegrityConstraintViolationException ex) {
                dialog.setStatus("Meter number already exists.", true);
            } catch (SQLException ex) {
                dialog.setStatus("Error: " + ex.getMessage(), true);
            }
        });

        dialog.setVisible(true);
    }

    /**
     * Loads customer data into the provided combo box for meter assignment.
     * 
     * @param cb the combo box to populate with customer options
     */
    private void loadCustomers(JComboBox<Object> cb) {
        String sql = "SELECT customer_id, name FROM customers ORDER BY name";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                cb.addItem(new Object[] { rs.getInt(1), rs.getString(2) });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cb.setRenderer((list, value, index, sel, focus) -> {
            JLabel lbl = new JLabel(value == null ? "" : (String) ((Object[]) value)[1]);
            lbl.setForeground(UITheme.TEXT_WHITE);
            lbl.setBackground(sel ? UITheme.PRIMARY : new Color(20, 40, 80));
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return lbl;
        });
    }

    /**
     * Displays an informational message dialog to the user.
     * 
     * @param msg the message to display
     */
    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Displays an error message dialog to the user.
     * 
     * @param msg the error message to display
     */
    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
