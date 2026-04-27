package gui;

import db.DatabaseManager;
import models.Bill;
import models.Customer;
import models.Tariff;
import models.User;
import utils.BillCalculator;
import utils.PDFGenerator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class BillingPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;

    public BillingPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("Billing Management");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton generateBtn = UITheme.createPrimaryButton("Generate Bill");
        JButton markPaidBtn = UITheme.createAccentButton("Mark Paid");
        JButton exportBtn   = UITheme.createAccentButton("Export Bill PDF");
        JButton excelBtn    = UITheme.createAccentButton("Export Excel");
        JButton refreshBtn  = UITheme.createAccentButton("Refresh");
        actions.add(generateBtn); actions.add(markPaidBtn);
        actions.add(exportBtn); actions.add(excelBtn); actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] cols = {"Bill ID", "Customer", "Month", "Consumption (kWh)", "Energy Charge", "Fixed", "Total", "Status", "Due Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);

        JPanel card = UITheme.createCard(null);
        card.setLayout(new BorderLayout());
        card.add(UITheme.createScrollPane(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        generateBtn.addActionListener(e -> showGenerateDialog());
        markPaidBtn.addActionListener(e -> markSelectedPaid());
        exportBtn.addActionListener(e -> exportSelectedBill());
        excelBtn.addActionListener(e -> utils.ExcelExporter.export(this, table, "Bills"));
        refreshBtn.addActionListener(e -> loadData());
    }

    private void loadData() {
        tableModel.setRowCount(0);
        String sql = "SELECT b.bill_id, c.name, b.billing_month, b.consumption_kwh, b.amount, " +
                     "b.fixed_charge, b.total_amount, b.status, b.due_date " +
                     "FROM bills b JOIN customers c ON b.customer_id=c.customer_id " +
                     "GROUP BY b.customer_id, b.billing_month, b.bill_id " +
                     "ORDER BY b.generated_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    String.format("%.2f", rs.getDouble(4)),
                    String.format("$%.2f", rs.getDouble(5)),
                    String.format("$%.2f", rs.getDouble(6)),
                    String.format("$%.2f", rs.getDouble(7)),
                    rs.getString(8),
                    rs.getString(9) != null ? rs.getString(9) : "N/A"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void showGenerateDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Generate Bill", true);
        dialog.setSize(420, 340);
        dialog.setLocationRelativeTo(this);

        BackgroundPanel bp = new BackgroundPanel(new GridBagLayout());
        bp.setOverlayAlpha(0.65f);
        dialog.setContentPane(bp);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx = 0; gbc.weightx = 1;
        gbc.insets = new Insets(5, 0, 5, 0);

        JComboBox<Object> customerCb = UITheme.createComboBox();
        loadCustomersIntoCombo(customerCb);
        JComboBox<Object> tariffCb = UITheme.createComboBox();
        loadTariffsIntoCombo(tariffCb);

        String currentMonth = java.time.YearMonth.now().toString();
        JTextField monthField = UITheme.createTextField();
        monthField.setText(currentMonth);

        JLabel previewLbl = new JLabel("Select customer to preview", SwingConstants.CENTER);
        previewLbl.setFont(UITheme.FONT_LABEL);
        previewLbl.setForeground(UITheme.ACCENT);

        int r = 0;
        gbc.gridy = r++; form.add(UITheme.createLabel("Customer"), gbc);
        gbc.gridy = r++; form.add(customerCb, gbc);
        gbc.gridy = r++; form.add(UITheme.createLabel("Tariff"), gbc);
        gbc.gridy = r++; form.add(tariffCb, gbc);
        gbc.gridy = r++; form.add(UITheme.createLabel("Billing Month (YYYY-MM)"), gbc);
        gbc.gridy = r++; form.add(monthField, gbc);
        gbc.gridy = r++; form.add(previewLbl, gbc);

        JButton genBtn = UITheme.createPrimaryButton("Generate Bill");
        gbc.gridy = r++; gbc.insets = new Insets(12, 0, 0, 0);
        form.add(genBtn, gbc);
        bp.add(form);

        customerCb.addActionListener(e -> updatePreview(customerCb, tariffCb, monthField, previewLbl));
        tariffCb.addActionListener(e -> updatePreview(customerCb, tariffCb, monthField, previewLbl));

        genBtn.addActionListener(e -> {
            if (customerCb.getSelectedItem() == null || tariffCb.getSelectedItem() == null) return;
            int customerId = (int) ((Object[]) customerCb.getSelectedItem())[0];
            int tariffId = (int) ((Object[]) tariffCb.getSelectedItem())[0];
            String month = monthField.getText().trim();
            generateBill(customerId, tariffId, month);
            loadData();
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private void updatePreview(JComboBox<Object> customerCb, JComboBox<Object> tariffCb,
                                JTextField monthField, JLabel previewLbl) {
        if (customerCb.getSelectedItem() == null || tariffCb.getSelectedItem() == null) return;
        int customerId = (int) ((Object[]) customerCb.getSelectedItem())[0];
        int tariffId = (int) ((Object[]) tariffCb.getSelectedItem())[0];
        String month = monthField.getText().trim();
        double consumption = getMonthlyConsumption(customerId, month);
        Tariff tariff = getTariffById(tariffId);
        if (tariff != null) {
            double total = BillCalculator.calculateTieredAmount(consumption, tariff);
            previewLbl.setText(String.format("Consumption: %.2f kWh | Total: $%.2f", consumption, total));
        }
    }

    private void generateBill(int customerId, int tariffId, String month) {
        double consumption = getMonthlyConsumption(customerId, month);
        Tariff tariff = getTariffById(tariffId);
        if (tariff == null) { JOptionPane.showMessageDialog(this, "Tariff not found."); return; }
        double amount = BillCalculator.calculateAmount(consumption, tariff);
        double total = BillCalculator.calculateTieredAmount(consumption, tariff);

        java.sql.Date dueDate = BillCalculator.calculateDueDate();

        String sql = "INSERT INTO bills (customer_id, tariff_id, billing_month, consumption_kwh, amount, fixed_charge, total_amount, status, due_date) VALUES (?,?,?,?,?,?,?,'PENDING',?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId); ps.setInt(2, tariffId); ps.setString(3, month);
            ps.setDouble(4, consumption); ps.setDouble(5, amount);
            ps.setDouble(6, tariff.getFixedCharge()); ps.setDouble(7, total);
            ps.setDate(8, dueDate);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, String.format("Bill generated!\nConsumption: %.2f kWh\nTotal: $%.2f", consumption, total));
            User au = utils.SessionManager.getCurrentUser();
            if (au != null) utils.AuditLogger.log(au.getUserId(), au.getUsername(),
                utils.AuditLogger.Action.GENERATE_BILL,
                String.format("Generated bill for customer ID=%d, month=%s, consumption=%.2f kWh, total=$%.2f",
                    customerId, month, consumption, total));
            // Send email notification
            sendBillEmail(customerId, month, total);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error generating bill: " + e.getMessage());
        }
    }

    private double getMonthlyConsumption(int customerId, String month) {
        String sql = "SELECT SUM(consumption_kwh) FROM meter_readings WHERE customer_id=? AND DATE_FORMAT(reading_date,'%Y-%m')=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId); ps.setString(2, month);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    private Tariff getTariffById(int id) {
        String sql = "SELECT * FROM tariffs WHERE tariff_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Tariff(rs.getInt(1), rs.getString(2), rs.getDouble(3),
                    rs.getDouble(4), rs.getString(5), rs.getBoolean(7));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private void markSelectedPaid() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a bill first."); return; }
        int billId = (int) tableModel.getValueAt(row, 0);
        String sql = "UPDATE bills SET status='PAID', paid_date=NOW() WHERE bill_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            ps.executeUpdate();
            User au = utils.SessionManager.getCurrentUser();
            if (au != null) utils.AuditLogger.log(au.getUserId(), au.getUsername(),
                utils.AuditLogger.Action.MARK_BILL_PAID,
                "Marked bill #" + String.format("%06d", billId) + " as PAID");
            loadData();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void exportSelectedBill() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a bill first."); return; }
        int billId = (int) tableModel.getValueAt(row, 0);
        exportBill(billId);
    }

    private void exportBill(int billId) {
        String sql = "SELECT b.*, c.name, c.email, c.address, c.phone, c.user_id, " +
                     "COALESCE(m.meter_number,'—') as meter_number, " +
                     "t.name as tname, t.rate_per_kwh, t.fixed_charge " +
                     "FROM bills b " +
                     "JOIN customers c ON b.customer_id=c.customer_id " +
                     "LEFT JOIN meters m ON m.customer_id=c.customer_id " +
                     "JOIN tariffs t ON b.tariff_id=t.tariff_id " +
                     "WHERE b.bill_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Bill bill = new Bill();
                bill.setBillId(rs.getInt("bill_id"));
                bill.setBillingMonth(rs.getString("billing_month"));
                bill.setConsumptionKwh(rs.getDouble("consumption_kwh"));
                bill.setAmount(rs.getDouble("amount"));
                bill.setFixedCharge(rs.getDouble("fixed_charge"));
                bill.setTotalAmount(rs.getDouble("total_amount"));
                bill.setStatus(rs.getString("status"));
                bill.setDueDate(rs.getDate("due_date"));

                Customer customer = new Customer();
                customer.setName(rs.getString("name"));
                customer.setEmail(rs.getString("email"));
                customer.setPhone(rs.getString("phone"));
                customer.setAddress(rs.getString("address"));
                customer.setMeterNumber(rs.getString("meter_number"));

                // Load customer profile picture path
                String picPath = null;
                int userId = rs.getInt("user_id");
                if (userId > 0) {
                    PreparedStatement picPs = conn.prepareStatement(
                        "SELECT profile_pic FROM users WHERE user_id=?");
                    picPs.setInt(1, userId);
                    ResultSet picRs = picPs.executeQuery();
                    if (picRs.next()) picPath = picRs.getString(1);
                }

                Tariff tariff = new Tariff();
                tariff.setName(rs.getString("tname"));
                tariff.setRatePerKwh(rs.getDouble("rate_per_kwh"));
                tariff.setFixedCharge(rs.getDouble("fixed_charge"));

                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Save Bill as PDF");
                fc.setSelectedFile(new java.io.File("Bill_" + String.format("%06d", billId) + ".pdf"));
                fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    String path = fc.getSelectedFile().getAbsolutePath();
                    if (!path.toLowerCase().endsWith(".pdf")) path += ".pdf";
                    if (PDFGenerator.generatePDF(bill, customer, tariff, path, picPath)) {
                        User au = utils.SessionManager.getCurrentUser();
                        if (au != null) utils.AuditLogger.log(au.getUserId(), au.getUsername(),
                            utils.AuditLogger.Action.EXPORT_BILL,
                            "Exported bill #" + String.format("%06d", billId) + " to PDF: " + path);
                        int open = JOptionPane.showConfirmDialog(this,
                            "Bill exported as PDF successfully!\nOpen the file now?",
                            "Export Successful", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        if (open == JOptionPane.YES_OPTION) {
                            try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); }
                            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Cannot open file: " + ex.getMessage()); }
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to generate PDF.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void sendBillEmail(int customerId, String month, double total) {
        String sql = "SELECT c.name, c.email, b.bill_id, b.due_date " +
                     "FROM customers c JOIN bills b ON b.customer_id=c.customer_id " +
                     "WHERE c.customer_id=? ORDER BY b.generated_at DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String email = rs.getString("email");
                if (email != null && !email.isEmpty()) {
                    utils.EmailService.sendBillGenerated(
                        email, rs.getString("name"),
                        String.valueOf(rs.getInt("bill_id")),
                        month, total,
                        rs.getString("due_date") != null ? rs.getString("due_date") : "N/A");
                }
            }
        } catch (SQLException e) { /* non-critical */ }
    }

    private void loadCustomersIntoCombo(JComboBox<Object> cb) {
        String sql = "SELECT customer_id, name FROM customers ORDER BY name";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) cb.addItem(new Object[]{rs.getInt(1), rs.getString(2)});
        } catch (SQLException e) { e.printStackTrace(); }
        cb.setRenderer((list, value, index, sel, focus) -> {
            JLabel lbl = new JLabel(value == null ? "" : (String) ((Object[]) value)[1]);
            lbl.setForeground(UITheme.TEXT_WHITE);
            lbl.setBackground(sel ? UITheme.PRIMARY : new Color(20, 40, 80));
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return lbl;
        });
    }

    private void loadTariffsIntoCombo(JComboBox<Object> cb) {
        String sql = "SELECT tariff_id, name FROM tariffs WHERE is_active=TRUE ORDER BY name";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) cb.addItem(new Object[]{rs.getInt(1), rs.getString(2)});
        } catch (SQLException e) { e.printStackTrace(); }
        cb.setRenderer((list, value, index, sel, focus) -> {
            JLabel lbl = new JLabel(value == null ? "" : (String) ((Object[]) value)[1]);
            lbl.setForeground(UITheme.TEXT_WHITE);
            lbl.setBackground(sel ? UITheme.PRIMARY : new Color(20, 40, 80));
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return lbl;
        });
    }
}
