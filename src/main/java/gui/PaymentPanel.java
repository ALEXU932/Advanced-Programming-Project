package gui;

import database.DatabaseManager;
import database.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class PaymentPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel totalCollectedLbl, pendingAmountLbl, todayCountLbl;

    public PaymentPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadData();
    }

    private void buildUI() {
        // ── Top: title + action buttons ───────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        JLabel title = new JLabel("Payment Management");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton recordBtn  = UITheme.createPrimaryButton("+ Record Payment");
        JButton refreshBtn = UITheme.createAccentButton("\u21BB  Refresh");
        JButton exportBtn  = UITheme.createAccentButton("Export Excel");
        recordBtn.setPreferredSize(new Dimension(160, 36));
        refreshBtn.setPreferredSize(new Dimension(100, 36));
        exportBtn.setPreferredSize(new Dimension(120, 36));
        actions.add(recordBtn);
        actions.add(refreshBtn);
        actions.add(exportBtn);
        header.add(actions, BorderLayout.EAST);

        // ── Summary cards row ─────────────────────────────────────────────────
        JPanel summaryRow = new JPanel(new GridLayout(1, 3, 14, 0));
        summaryRow.setOpaque(false);
        summaryRow.setPreferredSize(new Dimension(0, 88));

        totalCollectedLbl = new JLabel("$0.00", SwingConstants.CENTER);
        pendingAmountLbl  = new JLabel("$0.00", SwingConstants.CENTER);
        todayCountLbl     = new JLabel("0",     SwingConstants.CENTER);

        summaryRow.add(buildStatCard("Total Collected",  totalCollectedLbl, UITheme.SUCCESS));
        summaryRow.add(buildStatCard("Pending Amount",   pendingAmountLbl,  UITheme.WARNING));
        summaryRow.add(buildStatCard("Payments Today",   todayCountLbl,     UITheme.ACCENT));

        // ── Table ─────────────────────────────────────────────────────────────
        String[] cols = {"Pay ID", "Bill ID", "Customer", "Date & Time", "Amount", "Method", "Reference"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.setRowHeight(32);

        int[] widths = {60, 60, 180, 150, 90, 120, 130};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Center-align numeric/id columns
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        center.setBackground(UITheme.TABLE_ROW1);
        center.setForeground(UITheme.TEXT_WHITE);
        for (int i : new int[]{0, 1, 3, 4, 5, 6})
            table.getColumnModel().getColumn(i).setCellRenderer(center);

        // Color-code Method column
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setOpaque(true);
                String s = v != null ? v.toString() : "";
                switch (s) {
                    case "CASH":          setForeground(UITheme.SUCCESS); break;
                    case "CARD":          setForeground(UITheme.ACCENT);  break;
                    case "MOBILE_MONEY":  setForeground(UITheme.PRIMARY); break;
                    case "BANK_TRANSFER": setForeground(new Color(180,140,255)); break;
                    default:              setForeground(UITheme.TEXT_LIGHT);
                }
                setBackground(sel ? UITheme.PRIMARY : UITheme.TABLE_ROW1);
                return this;
            }
        });

        JPanel tableCard = UITheme.createCard(null);
        tableCard.setLayout(new BorderLayout());
        tableCard.add(UITheme.createScrollPane(table), BorderLayout.CENTER);

        // ── Assemble ──────────────────────────────────────────────────────────
        JPanel topSection = new JPanel(new BorderLayout(0, 12));
        topSection.setOpaque(false);
        topSection.add(header,     BorderLayout.NORTH);
        topSection.add(summaryRow, BorderLayout.SOUTH);

        add(topSection,  BorderLayout.NORTH);
        add(tableCard,   BorderLayout.CENTER);

        // ── Listeners ─────────────────────────────────────────────────────────
        recordBtn.addActionListener(e -> showRecordDialog());
        refreshBtn.addActionListener(e -> loadData());
        exportBtn.addActionListener(e -> report.ExcelExporter.export(this, table, "Payments"));
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        tableModel.setRowCount(0);
        String sql = "SELECT p.payment_id, p.bill_id, c.name, p.payment_date, " +
                     "p.amount, p.payment_method, COALESCE(p.reference_no,'—') " +
                     "FROM payments p JOIN customers c ON p.customer_id=c.customer_id " +
                     "ORDER BY p.payment_date DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt(1), rs.getInt(2), rs.getString(3),
                    rs.getString(4), String.format("$%.2f", rs.getDouble(5)),
                    rs.getString(6), rs.getString(7)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading payments: " + e.getMessage());
        }
        refreshStats();
    }

    private void refreshStats() {
        totalCollectedLbl.setText(querySingle("SELECT COALESCE(SUM(amount),0) FROM payments", "$%.2f"));
        pendingAmountLbl.setText(querySingle("SELECT COALESCE(SUM(total_amount),0) FROM bills WHERE status='PENDING'", "$%.2f"));
        todayCountLbl.setText(querySingle("SELECT COUNT(*) FROM payments WHERE DATE(payment_date)=CURDATE()", "%d"));
    }

    private String querySingle(String sql, String fmt) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                if (fmt.contains("d")) return String.format(fmt, rs.getInt(1));
                return String.format(fmt, rs.getDouble(1));
            }
        } catch (SQLException e) { /* ignore */ }
        return "—";
    }

    private void sendPaymentEmail(int customerId, int billId, double amount, String method) {
        String sql = "SELECT c.name, c.email FROM customers c WHERE c.customer_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String email = rs.getString("email");
                if (email != null && !email.isEmpty()) {
                    report.EmailService.sendPaymentConfirmation(
                        email, rs.getString("name"),
                        String.valueOf(billId), amount, method,
                        new java.text.SimpleDateFormat("dd MMM yyyy HH:mm").format(new java.util.Date()));
                }
            }
        } catch (SQLException e) { /* non-critical */ }
    }

    // ── Record Payment Dialog ─────────────────────────────────────────────────

    private void showRecordDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "Record Payment", true);
        dialog.setSize(600, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.setOverlayAlpha(0.72f);
        dialog.setContentPane(root);

        // Header
        JPanel dh = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8,18,50,240)); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(UITheme.PRIMARY); g2.setStroke(new BasicStroke(2f));
                g2.drawLine(0,getHeight()-1,getWidth(),getHeight()-1); g2.dispose();
            }
        };
        dh.setOpaque(false); dh.setPreferredSize(new Dimension(0,54));
        dh.setBorder(BorderFactory.createEmptyBorder(0,24,0,24));
        JLabel dhTitle = new JLabel("Record Payment");
        dhTitle.setFont(new Font("Segoe UI",Font.BOLD,16)); dhTitle.setForeground(UITheme.PRIMARY);
        JLabel dhSub = new JLabel("Amount is loaded automatically from the selected bill");
        dhSub.setFont(UITheme.FONT_SMALL); dhSub.setForeground(UITheme.TEXT_MUTED);
        dh.add(dhTitle, BorderLayout.WEST); dh.add(dhSub, BorderLayout.EAST);
        root.add(dh, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(16, 24, 10, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        g.insets = new Insets(4, 6, 4, 6);

        // Bill dropdown
        JComboBox<Object> billCb = UITheme.createComboBox();
        loadPendingBills(billCb);
        billCb.setPreferredSize(new Dimension(0, 38));

        // Amount — read-only, auto-filled
        JLabel amountLbl = new JLabel("  $0.00", SwingConstants.LEFT);
        amountLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        amountLbl.setForeground(UITheme.SUCCESS);
        amountLbl.setOpaque(true);
        amountLbl.setBackground(new Color(0, 50, 20, 140));
        amountLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.SUCCESS, 1),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        amountLbl.setPreferredSize(new Dimension(0, 44));

        // Bill summary info
        JLabel summaryLbl = new JLabel("  Select a bill above", SwingConstants.LEFT);
        summaryLbl.setFont(UITheme.FONT_SMALL);
        summaryLbl.setForeground(UITheme.TEXT_MUTED);
        summaryLbl.setOpaque(true);
        summaryLbl.setBackground(new Color(0, 40, 80, 120));
        summaryLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40,80,140), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        summaryLbl.setPreferredSize(new Dimension(0, 32));

        // Payment method
        JComboBox<String> methodCb = new JComboBox<>(
            new String[]{"CASH","BANK_TRANSFER","MOBILE_MONEY","CARD","ONLINE"});
        methodCb.setFont(UITheme.FONT_BODY);
        methodCb.setBackground(new Color(20,40,80));
        methodCb.setForeground(UITheme.TEXT_WHITE);
        methodCb.setBorder(BorderFactory.createLineBorder(UITheme.ACCENT, 1));
        methodCb.setPreferredSize(new Dimension(0, 38));

        // Reference and Notes — plain fields, no ghost text
        JTextField refF   = new JTextField();
        JTextField notesF = new JTextField();
        for (JTextField tf : new JTextField[]{refF, notesF}) {
            tf.setFont(UITheme.FONT_BODY);
            tf.setBackground(new Color(20, 40, 80, 200));
            tf.setForeground(UITheme.TEXT_WHITE);
            tf.setCaretColor(Color.WHITE);
            tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.ACCENT, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            tf.setPreferredSize(new Dimension(0, 38));
        }

        // Status
        JLabel statusLbl = new JLabel(" ", SwingConstants.CENTER);
        statusLbl.setFont(UITheme.FONT_SMALL); statusLbl.setForeground(UITheme.DANGER);

        // Holds selected bill data
        final int[]    selBillId     = {-1};
        final int[]    selCustomerId = {-1};
        final double[] selAmount     = {0.0};

        // Auto-fill amount + summary when bill selected
        billCb.addActionListener(e -> {
            Object sel = billCb.getSelectedItem();
            if (sel == null) return;
            Object[] d = (Object[]) sel;
            selBillId[0]     = (int)    d[0];
            selCustomerId[0] = (int)    d[1];
            selAmount[0]     = (double) d[2];
            amountLbl.setText(String.format("  $%.2f  (auto-loaded from bill)", selAmount[0]));
            summaryLbl.setText(String.format("  Bill #%06d  |  %s  |  Month: %s  |  Due: $%.2f",
                (int)d[0], (String)d[3], (String)d[4], (double)d[2]));
            summaryLbl.setForeground(UITheme.ACCENT);
        });

        // Trigger for first item
        if (billCb.getItemCount() > 0) {
            billCb.setSelectedIndex(0);
        }

        // Layout
        g.gridy=0; g.gridx=0; g.gridwidth=2;
        form.add(fLbl2("Select Pending Bill *"), g);
        g.gridy=1; form.add(billCb, g);

        g.gridy=2; form.add(fLbl2("Bill Summary"), g);
        g.gridy=3; form.add(summaryLbl, g);

        g.gridy=4; form.add(fLbl2("Amount to Pay (auto-loaded)"), g);
        g.gridy=5; form.add(amountLbl, g);

        g.gridy=6; g.gridwidth=1;
        form.add(fLbl2("Payment Method"), g);
        g.gridx=1; form.add(fLbl2("Reference No."), g);
        g.gridy=7; g.gridx=0; form.add(methodCb, g);
        g.gridx=1; form.add(refF, g);

        g.gridy=8; g.gridx=0; g.gridwidth=2;
        form.add(fLbl2("Notes (optional)"), g);
        g.gridy=9; form.add(notesF, g);

        g.gridy=10; g.insets=new Insets(6,6,2,6);
        form.add(statusLbl, g);

        root.add(form, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,10)) {
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
        JButton confirmBtn = UITheme.createPrimaryButton("  Confirm Payment  ");
        confirmBtn.setPreferredSize(new Dimension(170,38));
        footer.add(cancelBtn); footer.add(confirmBtn);
        root.add(footer, BorderLayout.SOUTH);

        cancelBtn.addActionListener(e -> dialog.dispose());

        confirmBtn.addActionListener(e -> {
            if (selBillId[0] < 0) { statusLbl.setText("Please select a bill."); return; }
            double amount = selAmount[0];
            if (amount <= 0) { statusLbl.setText("Invalid bill amount."); return; }
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                conn.setAutoCommit(false);
                PreparedStatement ps1 = conn.prepareStatement(
                    "INSERT INTO payments (bill_id,customer_id,amount,payment_method,reference_no,notes) VALUES (?,?,?,?,?,?)");
                ps1.setInt(1, selBillId[0]); ps1.setInt(2, selCustomerId[0]);
                ps1.setDouble(3, amount); ps1.setString(4, (String) methodCb.getSelectedItem());
                ps1.setString(5, refF.getText().trim()); ps1.setString(6, notesF.getText().trim());
                ps1.executeUpdate();
                PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE bills SET status='PAID', paid_date=NOW() WHERE bill_id=?");
                ps2.setInt(1, selBillId[0]); ps2.executeUpdate();
                conn.commit();
                User au = Logic.SessionManager.getCurrentUser();
                if (au != null) Logic.AuditLogger.log(au.getUserId(), au.getUsername(),
                    Logic.AuditLogger.Action.RECORD_PAYMENT,
                    String.format("Recorded payment of $%.2f for bill #%06d via %s",
                        amount, selBillId[0], methodCb.getSelectedItem()));
                sendPaymentEmail(selCustomerId[0], selBillId[0], amount, (String) methodCb.getSelectedItem());
                JOptionPane.showMessageDialog(dialog,
                    String.format("<html><b>Payment confirmed!</b><br>Bill #%06d marked as PAID.<br>Amount: $%.2f</html>",
                        selBillId[0], amount),
                    "Payment Confirmed", JOptionPane.INFORMATION_MESSAGE);
                loadData(); dialog.dispose();
            } catch (SQLException ex) {
                statusLbl.setText("Error: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }

    private JLabel fLbl2(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_LABEL); l.setForeground(UITheme.TEXT_LIGHT);
        return l;
    }

    private void loadPendingBills(JComboBox<Object> cb) {
        String sql = "SELECT b.bill_id, b.customer_id, b.total_amount, c.name, b.billing_month " +
                     "FROM bills b JOIN customers c ON b.customer_id=c.customer_id " +
                     "WHERE b.status='PENDING' ORDER BY b.due_date ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                cb.addItem(new Object[]{
                    rs.getInt(1), rs.getInt(2), rs.getDouble(3),
                    rs.getString(4),
                    String.format("#%06d — %s — %s — $%.2f",
                        rs.getInt(1), rs.getString(4),
                        rs.getString(5), rs.getDouble(3))
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (cb.getItemCount() == 0) cb.addItem(new Object[]{0, 0, 0.0, "No pending bills", "No pending bills"});

        cb.setRenderer((list, value, index, sel, focus) -> {
            String display = value == null ? "" : (String) ((Object[]) value)[4];
            JLabel lbl = new JLabel(display);
            lbl.setFont(UITheme.FONT_BODY);
            lbl.setForeground(UITheme.TEXT_WHITE);
            lbl.setBackground(sel ? UITheme.PRIMARY : new Color(20, 40, 80));
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            return lbl;
        });
    }

    // ── Stat card ─────────────────────────────────────────────────────────────

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
                // Bottom accent bar
                g2.setColor(color);
                g2.fillRoundRect(0, getHeight()-4, getWidth(), 4, 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 14, 16));

        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLbl.setForeground(color);

        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_MUTED);

        card.add(valueLbl, BorderLayout.CENTER);
        card.add(lbl,      BorderLayout.SOUTH);
        return card;
    }
}
