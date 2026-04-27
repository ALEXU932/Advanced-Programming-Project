package gui;

import db.DatabaseManager;
import models.Bill;
import models.Customer;
import models.Tariff;
import utils.PDFGenerator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class CustomerBillsPanel extends JPanel {
    private final Customer customer;
    private JTable table;
    private DefaultTableModel tableModel;

    public CustomerBillsPanel(Customer customer) {
        this.customer = customer;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("My Bills");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton exportBtn  = UITheme.createAccentButton("Download PDF");
        JButton excelBtn   = UITheme.createAccentButton("Export Excel");
        JButton disputeBtn = UITheme.createDangerButton("Dispute Bill");
        JButton refreshBtn = UITheme.createAccentButton("Refresh");
        actions.add(exportBtn); actions.add(excelBtn); actions.add(disputeBtn); actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] cols = {"Bill ID", "Month", "Consumption (kWh)", "Energy Charge", "Fixed", "Total", "Status", "Due Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);

        JPanel card = UITheme.createCard(null);
        card.setLayout(new BorderLayout());
        card.add(UITheme.createScrollPane(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        exportBtn.addActionListener(e -> exportSelected());
        excelBtn.addActionListener(e -> utils.ExcelExporter.export(this, table, "My Bills"));
        disputeBtn.addActionListener(e -> showDisputeDialog());
        refreshBtn.addActionListener(e -> loadData());
    }

    private void loadData() {
        tableModel.setRowCount(0);
        String sql = "SELECT b.bill_id, b.billing_month, b.consumption_kwh, b.amount, b.fixed_charge, b.total_amount, b.status, b.due_date " +
                     "FROM bills b WHERE b.customer_id=? ORDER BY b.generated_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2),
                    String.format("%.2f", rs.getDouble(3)),
                    String.format("$%.2f", rs.getDouble(4)),
                    String.format("$%.2f", rs.getDouble(5)),
                    String.format("$%.2f", rs.getDouble(6)),
                    rs.getString(7),
                    rs.getString(8) != null ? rs.getString(8) : "N/A"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void showDisputeDialog() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a bill to dispute."); return; }
        int billId = (int) tableModel.getValueAt(row, 0);
        String status = (String) tableModel.getValueAt(row, 6);
        if ("PAID".equals(status)) {
            JOptionPane.showMessageDialog(this, "Paid bills cannot be disputed.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        FormDialog dialog = new FormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Dispute Bill #" + String.format("%06d", billId),
            "Describe the issue with this bill",
            560, 360);

        JComboBox<String> reasonCb = FormDialog.makeStringCombo(new String[]{
            "Incorrect Reading", "Wrong Tariff Applied", "Meter Fault",
            "Billing Error", "Duplicate Bill", "Other"
        });
        JTextField descF = FormDialog.makeField("");

        dialog.addField("Reason *", reasonCb);
        dialog.addField("Description *", descF);
        dialog.addStatus();
        dialog.addCancelButton();
        JButton submitBtn = dialog.addSaveButton("  Submit Dispute  ");

        submitBtn.addActionListener(e -> {
            String reason = (String) reasonCb.getSelectedItem();
            String desc   = descF.getText().trim();
            if (desc.isEmpty()) { dialog.setStatus("Please describe the issue.", true); return; }
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO disputes (bill_id, customer_id, reason, description, status) VALUES (?,?,?,?,'OPEN')")) {
                ps.setInt(1, billId);
                ps.setInt(2, customer.getCustomerId());
                ps.setString(3, reason);
                ps.setString(4, desc);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(dialog,
                    "Dispute submitted successfully!\nOur team will review it shortly.",
                    "Submitted", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (SQLException ex) {
                dialog.setStatus("Error: " + ex.getMessage(), true);
            }
        });
        dialog.setVisible(true);
    }

    private void exportSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a bill first."); return; }
        int billId = (int) tableModel.getValueAt(row, 0);

        String sql = "SELECT b.*, t.name as tname, t.rate_per_kwh, " +
                     "COALESCE(m.meter_number,'—') as meter_number, u.profile_pic " +
                     "FROM bills b " +
                     "JOIN tariffs t ON b.tariff_id=t.tariff_id " +
                     "LEFT JOIN meters m ON m.customer_id=b.customer_id " +
                     "LEFT JOIN users u ON u.user_id=(SELECT user_id FROM customers WHERE customer_id=b.customer_id) " +
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

                // Use the customer object already loaded in this panel
                customer.setMeterNumber(rs.getString("meter_number"));

                Tariff tariff = new Tariff();
                tariff.setName(rs.getString("tname"));
                tariff.setRatePerKwh(rs.getDouble("rate_per_kwh"));
                tariff.setFixedCharge(rs.getDouble("fixed_charge"));

                String picPath = rs.getString("profile_pic");

                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Save Bill as PDF");
                fc.setSelectedFile(new java.io.File("Bill_" + String.format("%06d", billId) + ".pdf"));
                fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    String path = fc.getSelectedFile().getAbsolutePath();
                    if (!path.toLowerCase().endsWith(".pdf")) path += ".pdf";
                    if (PDFGenerator.generatePDF(bill, customer, tariff, path, picPath)) {
                        int open = JOptionPane.showConfirmDialog(this,
                            "Bill exported as PDF!\nOpen the file now?",
                            "Export Successful", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        if (open == JOptionPane.YES_OPTION) {
                            try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); }
                            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Cannot open: " + ex.getMessage()); }
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
}
