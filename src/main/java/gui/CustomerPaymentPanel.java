package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import database.DatabaseManager;
import database.Bill;
import database.Customer;
import database.Tariff;
import report.PDFGenerator;

public class CustomerPaymentPanel extends JPanel {

    private final Customer customer;
    private JTable historyTable;
    private DefaultTableModel historyModel;
    private JLabel totalPaidLbl, pendingLbl, lastPayLbl;

    public CustomerPaymentPanel(Customer customer) {
        this.customer = customer;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadHistory();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("\uD83D\uDCB3 Payments");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton payNowBtn  = UITheme.createPrimaryButton("Pay Now");
        JButton receiptBtn = UITheme.createAccentButton("Download Receipt");
        JButton refreshBtn = UITheme.createAccentButton("\u21BB Refresh");
        actions.add(payNowBtn); actions.add(receiptBtn); actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);

        JPanel summaryRow = new JPanel(new GridLayout(1, 3, 14, 0));
        summaryRow.setOpaque(false);
        summaryRow.setPreferredSize(new Dimension(0, UITheme.dim(90)));
        totalPaidLbl = new JLabel("$0.00", SwingConstants.CENTER);
        pendingLbl   = new JLabel("$0.00", SwingConstants.CENTER);
        lastPayLbl   = new JLabel("\u2014",  SwingConstants.CENTER);
        summaryRow.add(buildStatCard("Total Paid",     totalPaidLbl, UITheme.SUCCESS));
        summaryRow.add(buildStatCard("Pending Amount", pendingLbl,   UITheme.WARNING));
        summaryRow.add(buildStatCard("Last Payment",   lastPayLbl,   UITheme.ACCENT));

        String[] cols = {"Pay #", "Bill #", "Date & Time", "Amount", "Method", "Reference", "Status"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        UITheme.styleTable(historyTable);
        historyTable.setRowHeight(UITheme.dim(30));

        historyTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER); setOpaque(true);
                String s = v != null ? v.toString() : "";
                if      ("CASH".equals(s))          setForeground(UITheme.SUCCESS);
                else if ("CARD".equals(s))           setForeground(UITheme.ACCENT);
                else if ("MOBILE_MONEY".equals(s))   setForeground(UITheme.PRIMARY);
                else if ("BANK_TRANSFER".equals(s))  setForeground(new Color(180, 140, 255));
                else                                 setForeground(UITheme.TEXT_LIGHT);
                setBackground(sel ? UITheme.PRIMARY : UITheme.TABLE_ROW1);
                return this;
            }
        });

        JPanel tableCard = UITheme.createCard("Payment History");
        tableCard.setLayout(new BorderLayout());
        tableCard.add(UITheme.createScrollPane(historyTable), BorderLayout.CENTER);

        JPanel topSection = new JPanel(new BorderLayout(0, 12));
        topSection.setOpaque(false);
        topSection.add(header,     BorderLayout.NORTH);
        topSection.add(summaryRow, BorderLayout.SOUTH);

        add(topSection, BorderLayout.NORTH);
        add(tableCard,  BorderLayout.CENTER);

        payNowBtn.addActionListener(e -> showPayNowDialog());
        receiptBtn.addActionListener(e -> downloadReceipt());
        refreshBtn.addActionListener(e -> loadHistory());
    }

    private void loadHistory() {
        historyModel.setRowCount(0);
        String sql = "SELECT p.payment_id, p.bill_id, p.payment_date, p.amount, " +
                     "p.payment_method, COALESCE(p.reference_no,'\u2014'), b.status " +
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
                    rs.getString(5), rs.getString(6), rs.getString(7)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
        refreshStats();
    }

    private void refreshStats() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(amount),0) FROM payments WHERE customer_id=?")) {
                ps.setInt(1, customer.getCustomerId());
                ResultSet rs = ps.executeQuery();
                totalPaidLbl.setText(rs.next() ? String.format("$%.2f", rs.getDouble(1)) : "$0.00");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(total_amount),0) FROM bills WHERE customer_id=? AND status='PENDING'")) {
                ps.setInt(1, customer.getCustomerId());
                ResultSet rs = ps.executeQuery();
                pendingLbl.setText(rs.next() ? String.format("$%.2f", rs.getDouble(1)) : "$0.00");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT MAX(payment_date) FROM payments WHERE customer_id=?")) {
                ps.setInt(1, customer.getCustomerId());
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getTimestamp(1) != null)
                    lastPayLbl.setText(new SimpleDateFormat("dd MMM yyyy").format(rs.getTimestamp(1)));
                else lastPayLbl.setText("No payments yet");
            }
        } catch (SQLException e) { /* ignore */ }
    }

    private void showPayNowDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Pay Bill", true);
        dialog.setSize(UITheme.dim(560), UITheme.dim(420));
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.setOverlayAlpha(0.72f);
        dialog.setContentPane(root);

        JPanel dh = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8, 18, 50, 240)); g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UITheme.PRIMARY); g2.setStroke(new BasicStroke(2f));
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1); g2.dispose();
            }
        };
        dh.setOpaque(false); dh.setPreferredSize(new Dimension(0, UITheme.dim(52)));
        dh.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
        JLabel dhTitle = new JLabel("\uD83D\uDCB3 Pay Your Bill");
        dhTitle.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(15)));
        dhTitle.setForeground(UITheme.PRIMARY);
        dh.add(dhTitle, BorderLayout.WEST);
        root.add(dh, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(16, 24, 10, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        gbc.insets = new Insets(4, 6, 4, 6);

        JComboBox<Object> billCb = UITheme.createComboBox();
        loadPendingBills(billCb);
        billCb.setPreferredSize(new Dimension(0, UITheme.dim(36)));

        JLabel amountLbl = new JLabel("  Select a bill above", SwingConstants.LEFT);
        amountLbl.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(16)));
        amountLbl.setForeground(UITheme.SUCCESS); amountLbl.setOpaque(true);
        amountLbl.setBackground(new Color(0, 50, 20, 140));
        amountLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.SUCCESS, 1),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        amountLbl.setPreferredSize(new Dimension(0, UITheme.dim(44)));

        JComboBox<String> methodCb = new JComboBox<>(
            new String[]{"CASH", "BANK_TRANSFER", "MOBILE_MONEY", "CARD", "ONLINE"});
        methodCb.setFont(UITheme.FONT_BODY); methodCb.setBackground(new Color(20, 40, 80));
        methodCb.setForeground(UITheme.TEXT_WHITE);
        methodCb.setBorder(BorderFactory.createLineBorder(UITheme.ACCENT, 1));
        methodCb.setPreferredSize(new Dimension(0, UITheme.dim(36)));

        JTextField refF = UITheme.createTextField();
        refF.setPreferredSize(new Dimension(0, UITheme.dim(36)));

        JLabel statusLbl = new JLabel(" ", SwingConstants.CENTER);
        statusLbl.setFont(UITheme.FONT_SMALL); statusLbl.setForeground(UITheme.DANGER);

        final int[]    selBillId = {-1};
        final double[] selAmount = {0.0};

        billCb.addActionListener(e -> {
            Object sel = billCb.getSelectedItem();
            if (sel == null) return;
            Object[] d = (Object[]) sel;
            selBillId[0] = (int) d[0]; selAmount[0] = (double) d[2];
            amountLbl.setText(String.format("  $%.2f  (auto-loaded)", selAmount[0]));
        });
        if (billCb.getItemCount() > 0) billCb.setSelectedIndex(0);

        gbc.gridy = 0; gbc.gridwidth = 2; form.add(fLbl("Select Pending Bill *"), gbc);
        gbc.gridy = 1; form.add(billCb, gbc);
        gbc.gridy = 2; form.add(fLbl("Amount to Pay"), gbc);
        gbc.gridy = 3; form.add(amountLbl, gbc);
        gbc.gridy = 4; gbc.gridwidth = 1; form.add(fLbl("Payment Method"), gbc);
        gbc.gridx = 1; form.add(fLbl("Reference No. (optional)"), gbc);
        gbc.gridy = 5; gbc.gridx = 0; form.add(methodCb, gbc);
        gbc.gridx = 1; form.add(refF, gbc);
        gbc.gridy = 6; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(6, 6, 2, 6); form.add(statusLbl, gbc);
        root.add(form, BorderLayout.CENTER);

        JPanel footer = buildDialogFooter();
        JButton cancelBtn  = (JButton) footer.getComponent(0);
        JButton confirmBtn = (JButton) footer.getComponent(1);
        confirmBtn.setText("  Confirm Payment  ");
        root.add(footer, BorderLayout.SOUTH);

        cancelBtn.addActionListener(e -> dialog.dispose());
        confirmBtn.addActionListener(e -> {
            if (selBillId[0] < 0) { statusLbl.setText("Please select a bill."); return; }
            if (selAmount[0] <= 0) { statusLbl.setText("Invalid amount."); return; }
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                conn.setAutoCommit(false);
                PreparedStatement ps1 = conn.prepareStatement(
                    "INSERT INTO payments (bill_id,customer_id,amount,payment_method,reference_no) VALUES (?,?,?,?,?)");
                ps1.setInt(1, selBillId[0]); ps1.setInt(2, customer.getCustomerId());
                ps1.setDouble(3, selAmount[0]); ps1.setString(4, (String) methodCb.getSelectedItem());
                ps1.setString(5, refF.getText().trim()); ps1.executeUpdate();
                PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE bills SET status='PAID', paid_date=NOW() WHERE bill_id=?");
                ps2.setInt(1, selBillId[0]); ps2.executeUpdate();
                conn.commit();
                JOptionPane.showMessageDialog(dialog,
                    String.format("<html><b>\u2705 Payment Confirmed!</b><br>Bill #%06d marked as PAID.<br>Amount: $%.2f</html>",
                        selBillId[0], selAmount[0]),
                    "Payment Confirmed", JOptionPane.INFORMATION_MESSAGE);
                loadHistory(); dialog.dispose();
            } catch (SQLException ex) { statusLbl.setText("Error: " + ex.getMessage()); }
        });
        dialog.setVisible(true);
    }

    private void downloadReceipt() {
        int row = historyTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a payment from the history table first."); return; }
        String billIdStr = historyTable.getValueAt(row, 1).toString().replace("#", "");
        int billId;
        try { billId = Integer.parseInt(billIdStr); }
        catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Invalid bill ID."); return; }

        String sql = "SELECT b.*, t.name as tname, t.rate_per_kwh, t.fixed_charge as t_fixed, " +
                     "COALESCE(m.meter_number,'\u2014') as meter_number, u.profile_pic " +
                     "FROM bills b JOIN tariffs t ON b.tariff_id=t.tariff_id " +
                     "LEFT JOIN meters m ON m.customer_id=b.customer_id " +
                     "LEFT JOIN users u ON u.user_id=(SELECT user_id FROM customers WHERE customer_id=b.customer_id) " +
                     "WHERE b.bill_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { JOptionPane.showMessageDialog(this, "Bill not found."); return; }

            Bill bill = new Bill();
            bill.setBillId(rs.getInt("bill_id")); bill.setBillingMonth(rs.getString("billing_month"));
            bill.setConsumptionKwh(rs.getDouble("consumption_kwh")); bill.setAmount(rs.getDouble("amount"));
            bill.setFixedCharge(rs.getDouble("fixed_charge")); bill.setTotalAmount(rs.getDouble("total_amount"));
            bill.setStatus(rs.getString("status")); bill.setDueDate(rs.getDate("due_date"));
            customer.setMeterNumber(rs.getString("meter_number"));

            Tariff tariff = new Tariff();
            tariff.setName(rs.getString("tname")); tariff.setRatePerKwh(rs.getDouble("rate_per_kwh"));
            tariff.setFixedCharge(rs.getDouble("t_fixed"));

            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Save Receipt as PDF");
            fc.setSelectedFile(new java.io.File("Receipt_Bill" + billIdStr + ".pdf"));
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String path = fc.getSelectedFile().getAbsolutePath();
                if (!path.toLowerCase().endsWith(".pdf")) path += ".pdf";
                if (PDFGenerator.generatePDF(bill, customer, tariff, path, rs.getString("profile_pic"))) {
                    int open = JOptionPane.showConfirmDialog(this, "Receipt saved!\nOpen now?",
                        "Success", JOptionPane.YES_NO_OPTION);
                    if (open == JOptionPane.YES_OPTION) {
                        try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); }
                        catch (java.io.IOException | UnsupportedOperationException ex) {
                            JOptionPane.showMessageDialog(this, "Cannot open: " + ex.getMessage());
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to generate receipt.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void loadPendingBills(JComboBox<Object> cb) {
        String sql = "SELECT b.bill_id, b.customer_id, b.total_amount, b.billing_month, " +
                     "COALESCE(DATE_FORMAT(b.due_date,'%d %b %Y'),'N/A') " +
                     "FROM bills b WHERE b.customer_id=? AND b.status='PENDING' ORDER BY b.due_date ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cb.addItem(new Object[]{
                    rs.getInt(1), rs.getInt(2), rs.getDouble(3), rs.getString(4), rs.getString(5),
                    String.format("#%06d \u2014 %s \u2014 Due: %s \u2014 $%.2f",
                        rs.getInt(1), rs.getString(4), rs.getString(5), rs.getDouble(3))
                });
            }
        } catch (SQLException e) { /* ignore */ }
        if (cb.getItemCount() == 0)
            cb.addItem(new Object[]{0, 0, 0.0, "No pending bills", "\u2014", "No pending bills"});
        cb.setRenderer((list, value, index, sel, focus) -> {
            String display = value == null ? "" : (String) ((Object[]) value)[5];
            JLabel lbl = new JLabel(display);
            lbl.setFont(UITheme.FONT_BODY); lbl.setForeground(UITheme.TEXT_WHITE);
            lbl.setBackground(sel ? UITheme.PRIMARY : new Color(20, 40, 80));
            lbl.setOpaque(true); lbl.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            return lbl;
        });
    }

    private JPanel buildDialogFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8, 18, 50, 210)); g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(40, 60, 110)); g2.drawLine(0, 0, getWidth(), 0); g2.dispose();
            }
        };
        footer.setOpaque(false);
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(UITheme.FONT_BUTTON); cancelBtn.setForeground(UITheme.TEXT_LIGHT);
        cancelBtn.setBackground(new Color(40, 60, 100)); cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false); cancelBtn.setPreferredSize(new Dimension(UITheme.dim(100), UITheme.dim(36)));
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JButton actionBtn = UITheme.createPrimaryButton("Action");
        actionBtn.setPreferredSize(new Dimension(UITheme.dim(160), UITheme.dim(36)));
        footer.add(cancelBtn); footer.add(actionBtn);
        return footer;
    }

    private JLabel fLbl(String text) {
        JLabel l = new JLabel(text); l.setFont(UITheme.FONT_LABEL); l.setForeground(UITheme.TEXT_LIGHT); return l;
    }

    private JPanel buildStatCard(String label, JLabel valueLbl, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 30, 70, 215));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 90));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.setColor(color); g2.fillRoundRect(0, getHeight()-4, getWidth(), 4, 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false); card.setBorder(BorderFactory.createEmptyBorder(12, 16, 14, 16));
        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(20))); valueLbl.setForeground(color);
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(UITheme.FONT_SMALL); lbl.setForeground(UITheme.TEXT_MUTED);
        card.add(valueLbl, BorderLayout.CENTER); card.add(lbl, BorderLayout.SOUTH);
        return card;
    }
}
