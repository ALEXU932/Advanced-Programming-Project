package gui;

import db.DatabaseManager;
import models.Bill;
import models.Customer;
import models.Tariff;
import utils.AuditLogger;
import utils.EmailService;
import utils.PDFGenerator;
import utils.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Customer Payment Panel — Pay Now, Payment History, Download Receipt, Auto-Pay.
 */
public class CustomerPaymentPanel extends JPanel {

    private final Customer customer;
    private JTable historyTable;
    private DefaultTableModel historyModel;
    private JLabel lastPaymentLbl;
    private JLabel totalPaidLbl;
    private JLabel pendingCountLbl;

    public CustomerPaymentPanel(Customer customer) {
        this.customer = customer;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadHistory();
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("\u25A3 Payments");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton payNowBtn      = UITheme.createPrimaryButton("Pay Now");
        JButton autoPayBtn     = UITheme.createAccentButton("Auto-Pay Settings");
        JButton receiptBtn     = UITheme.createAccentButton("Download Receipt");
        JButton refreshBtn     = UITheme.createAccentButton("↻ Refresh");
        actions.add(payNowBtn); actions.add(autoPayBtn);
        actions.add(receiptBtn); actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Summary cards ─────────────────────────────────────────────────────
        JPanel summaryRow = new JPanel(new GridLayout(1, 3, 16, 0));
        summaryRow.setOpaque(false);

        lastPaymentLbl  = statValue("—");
        totalPaidLbl    = statValue("$0.00");
        pendingCountLbl = statValue("0");

        summaryRow.add(createStatCard("Last Payment", lastPaymentLbl, UITheme.SUCCESS));
        summaryRow.add(createStatCard("Total Paid (All Time)", totalPaidLbl, UITheme.ACCENT));
        summaryRow.add(createStatCard("Pending Bills", pendingCountLbl, UITheme.WARNING));

        // ── History table ─────────────────────────────────────────────────────
        String[] cols = {"Payment ID", "Bill #", "Month", "Amount", "Method", "Reference", "Date", "Status"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        UITheme.styleTable(historyTable);

        JPanel tableCard = UITheme.createCard("Payment History");
        tableCard.setLayout(new BorderLayout());
        tableCard.add(UITheme.createScrollPane(historyTable), BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.add(summaryRow, BorderLayout.NORTH);
        center.add(tableCard, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // ── Button actions ────────────────────────────────────────────────────
        payNowBtn.addActionListener(e -> showPayNowDialog());
        autoPayBtn.addActionListener(e -> showAutoPayDialog());
        receiptBtn.addActionListener(e -> downloadReceipt());
        refreshBtn.addActionListener(e -> loadHistory());
    }

    // ── Load payment history ──────────────────────────────────────────────────

    private void loadHistory() {
        historyModel.setRowCount(0);
        String sql = "SELECT p.payment_id, p.bill_id, b.billing_month, p.amount, " +
                     "p.payment_method, COALESCE(p.reference_no,'—'), p.payment_date, 'PAID' " +
                     "FROM payments p JOIN bills b ON p.bill_id=b.bill_id " +
                     "WHERE p.customer_id=? ORDER BY p.payment_date DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                historyModel.addRow(new Object[]{
                    String.format("#%06d", rs.getInt(1)),
                    String.format("#%06d", rs.getInt(2)),
                    rs.getString(3),
                    String.format("$%.2f", rs.getDouble(4)),
                    rs.getString(5),
                    rs.getString(6),
                    rs.getString(7),
                    rs.getString(8)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading payments: " + e.getMessage());
        }
        updateSummary();
    }

    private void updateSummary() {
        // Last payment date
        String sqlLast = "SELECT MAX(payment_date) FROM payments WHERE customer_id=?";
        // Total paid
        String sqlTotal = "SELECT COALESCE(SUM(amount),0) FROM payments WHERE customer_id=?";
        // Pending bills
        String sqlPending = "SELECT COUNT(*) FROM bills WHERE customer_id=? AND status != 'PAID'";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlLast)) {
                ps.setInt(1, customer.getCustomerId());
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getTimestamp(1) != null) {
                    lastPaymentLbl.setText(new SimpleDateFormat("dd MMM yyyy").format(rs.getTimestamp(1)));
                } else {
                    lastPaymentLbl.setText("No payments yet");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlTotal)) {
                ps.setInt(1, customer.getCustomerId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) totalPaidLbl.setText(String.format("$%.2f", rs.getDouble(1)));
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlPending)) {
                ps.setInt(1, customer.getCustomerId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) pendingCountLbl.setText(String.valueOf(rs.getInt(1)));
            }
        } catch (SQLException e) { /* ignore summary errors */ }
    }

    // ── Pay Now dialog ────────────────────────────────────────────────────────

    private void showPayNowDialog() {
        // Load unpaid bills
        String sql = "SELECT bill_id, billing_month, total_amount, due_date FROM bills " +
                     "WHERE customer_id=? AND status != 'PAID' ORDER BY due_date ASC";
        java.util.List<Object[]> unpaidBills = new java.util.ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                unpaidBills.add(new Object[]{
                    rs.getInt(1), rs.getString(2),
                    rs.getDouble(3), rs.getString(4)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            return;
        }

        if (unpaidBills.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "All your bills are paid! No pending payments.",
                "No Pending Bills", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        FormDialog dialog = new FormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Pay Bill", "Select a bill and payment method", 520, 420);

        // Bill selector
        String[] billLabels = unpaidBills.stream()
            .map(b -> String.format("Bill #%06d — %s — $%.2f (Due: %s)",
                (int)b[0], b[1], (double)b[2], b[3]))
            .toArray(String[]::new);
        JComboBox<String> billCb = FormDialog.makeStringCombo(billLabels);

        // Amount field (auto-filled)
        JTextField amountF = FormDialog.makeField(String.format("%.2f", (double)unpaidBills.get(0)[2]));

        // Payment method
        JComboBox<String> methodCb = FormDialog.makeStringCombo(new String[]{
            "CASH", "BANK_TRANSFER", "MOBILE_MONEY", "CARD", "ONLINE"
        });

        // Reference number
        JTextField refF = FormDialog.makeField(generateReference());

        // Notes
        JTextField notesF = FormDialog.makeField("");

        // Auto-fill amount when bill changes
        billCb.addActionListener(e -> {
            int idx = billCb.getSelectedIndex();
            if (idx >= 0 && idx < unpaidBills.size()) {
                amountF.setText(String.format("%.2f", (double)unpaidBills.get(idx)[2]));
            }
        });

        dialog.addField("Select Bill *", billCb);
        dialog.addField("Amount ($) *", amountF);
        dialog.addField("Payment Method *", methodCb);
        dialog.addField("Reference Number", refF);
        dialog.addField("Notes (optional)", notesF);
        dialog.addStatus();
        dialog.addCancelButton();
        JButton payBtn = dialog.addSaveButton("  Confirm Payment  ");

        payBtn.addActionListener(e -> {
            int idx = billCb.getSelectedIndex();
            if (idx < 0) { dialog.setStatus("Please select a bill.", true); return; }
            int billId = (int) unpaidBills.get(idx)[0];
            String month = (String) unpaidBills.get(idx)[1];
            double amount;
            try {
                amount = Double.parseDouble(amountF.getText().trim());
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                dialog.setStatus("Please enter a valid amount.", true); return;
            }
            String method = (String) methodCb.getSelectedItem();
            String ref    = refF.getText().trim();
            String notes  = notesF.getText().trim();

            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                conn.setAutoCommit(false);
                // Insert payment
                PreparedStatement ps1 = conn.prepareStatement(
                    "INSERT INTO payments (bill_id, customer_id, amount, payment_method, reference_no, notes) " +
                    "VALUES (?,?,?,?,?,?)");
                ps1.setInt(1, billId); ps1.setInt(2, customer.getCustomerId());
                ps1.setDouble(3, amount); ps1.setString(4, method);
                ps1.setString(5, ref.isEmpty() ? null : ref);
                ps1.setString(6, notes.isEmpty() ? null : notes);
                ps1.executeUpdate();

                // Mark bill as PAID
                PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE bills SET status='PAID', paid_date=NOW() WHERE bill_id=?");
                ps2.setInt(1, billId);
                ps2.executeUpdate();

                conn.commit();

                // Audit log
                AuditLogger.log(customer.getUserId(), customer.getName(),
                    AuditLogger.Action.RECORD_PAYMENT,
                    "Customer paid bill #" + billId + " amount=$" + amount + " method=" + method);

                // Send email confirmation (async)
                if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
                    EmailService.sendPaymentConfirmation(
                        customer.getEmail(), customer.getName(),
                        String.valueOf(billId), amount, method,
                        new SimpleDateFormat("dd MMM yyyy").format(new Date()));
                }

                // Add notification
                addNotification("Payment of $" + String.format("%.2f", amount) +
                    " for " + month + " confirmed. Ref: " + (ref.isEmpty() ? "N/A" : ref),
                    "PAYMENT_SUCCESS");

                dialog.dispose();
                JOptionPane.showMessageDialog(this,
                    "Payment successful!\n\nBill #" + String.format("%06d", billId) +
                    "\nAmount: $" + String.format("%.2f", amount) +
                    "\nMethod: " + method +
                    "\nReference: " + (ref.isEmpty() ? "N/A" : ref),
                    "Payment Confirmed", JOptionPane.INFORMATION_MESSAGE);
                loadHistory();

            } catch (SQLException ex) {
                try { /* rollback handled by conn close */ } catch (Exception ignored) {}
                dialog.setStatus("Payment failed: " + ex.getMessage(), true);
            }
        });

        dialog.setVisible(true);
    }

    // ── Auto-Pay dialog ───────────────────────────────────────────────────────

    private void showAutoPayDialog() {
        FormDialog dialog = new FormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Auto-Pay Settings", "Configure automatic bill payment", 500, 380);

        // Load existing settings
        boolean[] autoPayEnabled = {false};
        String[] existingMethod = {"BANK_TRANSFER"};
        String[] existingRef    = {""};
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT is_enabled, payment_method, reference_info FROM customer_autopay WHERE customer_id=?")) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                autoPayEnabled[0] = rs.getBoolean(1);
                existingMethod[0] = rs.getString(2);
                existingRef[0]    = rs.getString(3) != null ? rs.getString(3) : "";
            }
        } catch (SQLException ignored) {}

        JCheckBox enabledCb = new JCheckBox("Enable Auto-Pay (bills paid automatically on due date)");
        enabledCb.setFont(UITheme.FONT_BODY);
        enabledCb.setForeground(UITheme.TEXT_WHITE);
        enabledCb.setOpaque(false);
        enabledCb.setSelected(autoPayEnabled[0]);

        JComboBox<String> methodCb = FormDialog.makeStringCombo(new String[]{
            "BANK_TRANSFER", "MOBILE_MONEY", "CARD", "ONLINE"
        });
        methodCb.setSelectedItem(existingMethod[0]);

        JTextField refF = FormDialog.makeField(existingRef[0]);

        JLabel infoLbl = new JLabel(
            "<html><div style='color:#aac;font-size:10px;'>" +
            "\u26A0 Auto-pay will process payment on the bill due date using your selected method.<br>" +
            "You will receive an email notification before each payment.</div></html>");

        dialog.addField("", enabledCb);
        dialog.addField("Default Payment Method *", methodCb);
        dialog.addField("Account / Reference Info", refF);
        dialog.body.add(infoLbl, new java.awt.GridBagConstraints() {{
            gridx=0; gridy=6; gridwidth=2; fill=HORIZONTAL;
            insets=new java.awt.Insets(8,6,4,6);
        }});
        dialog.addStatus();
        dialog.addCancelButton();
        JButton saveBtn = dialog.addSaveButton("  Save Settings  ");

        saveBtn.addActionListener(e -> {
            String method = (String) methodCb.getSelectedItem();
            String ref    = refF.getText().trim();
            boolean enabled = enabledCb.isSelected();
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO customer_autopay (customer_id, is_enabled, payment_method, reference_info) " +
                     "VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE is_enabled=?, payment_method=?, reference_info=?")) {
                ps.setInt(1, customer.getCustomerId());
                ps.setBoolean(2, enabled); ps.setString(3, method);
                ps.setString(4, ref.isEmpty() ? null : ref);
                ps.setBoolean(5, enabled); ps.setString(6, method);
                ps.setString(7, ref.isEmpty() ? null : ref);
                ps.executeUpdate();
                dialog.setStatus(enabled ? "Auto-pay enabled!" : "Auto-pay disabled.", false);
                addNotification(enabled ?
                    "Auto-pay enabled with method: " + method :
                    "Auto-pay has been disabled.", "AUTOPAY_CHANGE");
                dialog.dispose();
                JOptionPane.showMessageDialog(this,
                    enabled ? "Auto-pay enabled!\nBills will be paid automatically on due date."
                            : "Auto-pay has been disabled.",
                    "Auto-Pay", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                dialog.setStatus("Error: " + ex.getMessage(), true);
            }
        });

        dialog.setVisible(true);
    }

    // ── Download receipt ──────────────────────────────────────────────────────

    private void downloadReceipt() {
        int row = historyTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a payment from the history to download its receipt.");
            return;
        }
        // Get bill_id from selected row (column 1)
        String billIdStr = historyTable.getValueAt(row, 1).toString().replace("#", "");
        int billId;
        try { billId = Integer.parseInt(billIdStr); }
        catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Invalid bill ID."); return; }

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
                customer.setMeterNumber(rs.getString("meter_number"));

                Tariff tariff = new Tariff();
                tariff.setName(rs.getString("tname"));
                tariff.setRatePerKwh(rs.getDouble("rate_per_kwh"));
                tariff.setFixedCharge(rs.getDouble("fixed_charge"));

                String picPath = rs.getString("profile_pic");

                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Save Receipt as PDF");
                fc.setSelectedFile(new java.io.File("Receipt_Bill_" + String.format("%06d", billId) + ".pdf"));
                fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    String path = fc.getSelectedFile().getAbsolutePath();
                    if (!path.toLowerCase().endsWith(".pdf")) path += ".pdf";
                    if (PDFGenerator.generatePDF(bill, customer, tariff, path, picPath)) {
                        AuditLogger.log(customer.getUserId(), customer.getName(),
                            AuditLogger.Action.EXPORT_BILL, "Customer downloaded receipt for bill #" + billId);
                        int open = JOptionPane.showConfirmDialog(this,
                            "Receipt saved!\nOpen now?", "Success",
                            JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        if (open == JOptionPane.YES_OPTION) {
                            try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); }
                            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Cannot open: " + ex.getMessage()); }
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to generate receipt.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JPanel createStatCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 30, 70, 210));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.add(valueLabel, BorderLayout.CENTER);
        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(UITheme.FONT_LABEL);
        titleLbl.setForeground(UITheme.TEXT_LIGHT);
        card.add(titleLbl, BorderLayout.SOUTH);
        return card;
    }

    private JLabel statValue(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lbl.setForeground(UITheme.TEXT_WHITE);
        return lbl;
    }

    private String generateReference() {
        return "PAY-" + System.currentTimeMillis() % 1000000;
    }

    private void addNotification(String message, String type) {
        String sql = "INSERT INTO customer_notifications (customer_id, message, type) VALUES (?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ps.setString(2, message);
            ps.setString(3, type);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }
}
