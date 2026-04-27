package gui;

import db.DatabaseManager;
import models.Customer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * Customer Notifications Panel — Bill due reminders, high usage alerts,
 * payment success notifications, budget exceeded warnings, missing reading alerts.
 */
public class CustomerNotificationsPanel extends JPanel {

    private final Customer customer;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel unreadCountLbl;

    public CustomerNotificationsPanel(Customer customer) {
        this.customer = customer;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadNotifications();
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        JLabel title = new JLabel("\u25C6 Notifications");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        unreadCountLbl = new JLabel("0 unread");
        unreadCountLbl.setFont(UITheme.FONT_LABEL);
        unreadCountLbl.setForeground(UITheme.WARNING);
        unreadCountLbl.setOpaque(true);
        unreadCountLbl.setBackground(new Color(255, 200, 0, 40));
        unreadCountLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.WARNING, 1),
            BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        titleRow.add(title);
        titleRow.add(unreadCountLbl);
        header.add(titleRow, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton markAllBtn  = UITheme.createAccentButton("Mark All Read");
        JButton clearBtn    = UITheme.createDangerButton("Clear Read");
        JButton refreshBtn  = UITheme.createAccentButton("↻ Refresh");
        actions.add(markAllBtn); actions.add(clearBtn); actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Alert type legend ─────────────────────────────────────────────────
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        legend.setOpaque(false);
        addLegendItem(legend, "\u25CB Payment",    UITheme.SUCCESS);
        addLegendItem(legend, "\u2261 Bill Due",   UITheme.WARNING);
        addLegendItem(legend, "\u26A1 High Usage", UITheme.DANGER);
        addLegendItem(legend, "\u25A6 Budget",     UITheme.PRIMARY);
        addLegendItem(legend, "\u25A6 Reading",    UITheme.ACCENT);
        addLegendItem(legend, "\u2139 System",     UITheme.TEXT_MUTED);

        // ── Table ─────────────────────────────────────────────────────────────
        String[] cols = {"", "Type", "Message", "Date", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.setRowHeight(UITheme.dim(32));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(0).setMaxWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(400);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);

        // Color-code status column
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setOpaque(true);
                String s = v != null ? v.toString() : "";
                if ("UNREAD".equals(s)) {
                    setForeground(UITheme.WARNING);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(UITheme.TEXT_MUTED);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                setBackground(sel ? UITheme.PRIMARY : UITheme.TABLE_ROW1);
                return this;
            }
        });

        // Color-code type column
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setOpaque(true);
                String s = v != null ? v.toString() : "";
                Color color;
                switch (s) {
                    case "PAYMENT_SUCCESS": color = UITheme.SUCCESS; break;
                    case "BILL_DUE":        color = UITheme.WARNING; break;
                    case "HIGH_USAGE":      color = UITheme.DANGER;  break;
                    case "BUDGET_EXCEEDED": color = UITheme.PRIMARY; break;
                    case "READING_MISSING": color = UITheme.ACCENT;  break;
                    case "AUTOPAY_CHANGE":  color = UITheme.ACCENT;  break;
                    default:                color = UITheme.TEXT_MUTED;
                }
                setForeground(color);
                setBackground(sel ? UITheme.PRIMARY : UITheme.TABLE_ROW1);
                return this;
            }
        });

        JPanel tableCard = UITheme.createCard("All Notifications");
        tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.add(legend, BorderLayout.NORTH);
        tableCard.add(UITheme.createScrollPane(table), BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        // ── Button actions ────────────────────────────────────────────────────
        markAllBtn.addActionListener(e -> markAllRead());
        clearBtn.addActionListener(e -> clearRead());
        refreshBtn.addActionListener(e -> {
            generateSystemNotifications();
            loadNotifications();
        });

        // Mark as read on click
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) markRowRead(row);
            }
        });
    }

    // ── Load notifications ────────────────────────────────────────────────────

    private void loadNotifications() {
        generateSystemNotifications();
        tableModel.setRowCount(0);
        String sql = "SELECT notification_id, type, message, created_at, is_read " +
                     "FROM customer_notifications WHERE customer_id=? " +
                     "ORDER BY created_at DESC LIMIT 100";
        int unread = 0;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                boolean read = rs.getBoolean(5);
                if (!read) unread++;
                tableModel.addRow(new Object[]{
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    read ? "READ" : "UNREAD"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
        unreadCountLbl.setText(unread + " unread");
        unreadCountLbl.setForeground(unread > 0 ? UITheme.WARNING : UITheme.SUCCESS);
    }

    /**
     * Auto-generate system notifications based on current account state.
     */
    private void generateSystemNotifications() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // 1. Bill due reminder — bills due within 7 days
            String sqlDue = "SELECT bill_id, billing_month, total_amount, due_date FROM bills " +
                            "WHERE customer_id=? AND status='PENDING' AND due_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 7 DAY)";
            try (PreparedStatement ps = conn.prepareStatement(sqlDue)) {
                ps.setInt(1, customer.getCustomerId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String msg = String.format("[Due] Bill #%06d for %s ($%.2f) is due on %s. Please pay to avoid late fees.",
                        rs.getInt(1), rs.getString(2), rs.getDouble(3), rs.getString(4));
                    insertNotificationIfNew(conn, msg, "BILL_DUE");
                }
            }

            // 2. Overdue bills
            String sqlOverdue = "SELECT bill_id, billing_month, total_amount FROM bills " +
                                "WHERE customer_id=? AND status='OVERDUE'";
            try (PreparedStatement ps = conn.prepareStatement(sqlOverdue)) {
                ps.setInt(1, customer.getCustomerId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String msg = String.format("[OVERDUE] Bill #%06d for %s ($%.2f) is OVERDUE. Please pay immediately.",
                        rs.getInt(1), rs.getString(2), rs.getDouble(3));
                    insertNotificationIfNew(conn, msg, "BILL_DUE");
                }
            }

            // 3. High usage alert — last reading > 2x average
            String sqlUsage = "SELECT consumption_kwh FROM meter_readings WHERE customer_id=? ORDER BY reading_date DESC LIMIT 6";
            try (PreparedStatement ps = conn.prepareStatement(sqlUsage)) {
                ps.setInt(1, customer.getCustomerId());
                ResultSet rs = ps.executeQuery();
                java.util.List<Double> readings = new java.util.ArrayList<>();
                while (rs.next()) readings.add(rs.getDouble(1));
                if (readings.size() >= 3) {
                    double last = readings.get(0);
                    double avg  = readings.subList(1, readings.size()).stream()
                                         .mapToDouble(Double::doubleValue).average().orElse(0);
                    if (avg > 0 && last > avg * 1.8) {
                        String msg = String.format("[High Usage] Last reading: %.1f kWh (%.0f%% above your average of %.1f kWh).",
                            last, ((last - avg) / avg) * 100, avg);
                        insertNotificationIfNew(conn, msg, "HIGH_USAGE");
                    }
                }
            }

            // 4. Budget exceeded warning
            String sqlBudget = "SELECT cb.monthly_budget_kwh, cb.alert_threshold, " +
                               "COALESCE(SUM(mr.consumption_kwh),0) as used " +
                               "FROM customer_budgets cb " +
                               "LEFT JOIN meter_readings mr ON mr.customer_id=cb.customer_id " +
                               "  AND MONTH(mr.reading_date)=MONTH(NOW()) AND YEAR(mr.reading_date)=YEAR(NOW()) " +
                               "WHERE cb.customer_id=? AND cb.is_active=TRUE GROUP BY cb.budget_id";
            try (PreparedStatement ps = conn.prepareStatement(sqlBudget)) {
                ps.setInt(1, customer.getCustomerId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    double budget = rs.getDouble(1);
                    int threshold = rs.getInt(2);
                    double used   = rs.getDouble(3);
                    double pct    = budget > 0 ? (used / budget) * 100 : 0;
                    if (pct >= threshold) {
                        String msg = String.format("\u26A0 Budget alert! You've used %.1f kWh (%.0f%%) of your %.0f kWh monthly budget.",
                            used, pct, budget);
                        insertNotificationIfNew(conn, msg, "BUDGET_EXCEEDED");
                    }
                }
            }

            // 5. Missing reading alert — no reading this month
            String sqlReading = "SELECT COUNT(*) FROM meter_readings WHERE customer_id=? " +
                                "AND MONTH(reading_date)=MONTH(NOW()) AND YEAR(reading_date)=YEAR(NOW())";
            try (PreparedStatement ps = conn.prepareStatement(sqlReading)) {
                ps.setInt(1, customer.getCustomerId());
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    String msg = "\u26A0 No meter reading recorded for this month. Please submit your reading or contact support.";
                    insertNotificationIfNew(conn, msg, "READING_MISSING");
                }
            }

        } catch (SQLException e) { /* silent — notifications are non-critical */ }
    }

    private void insertNotificationIfNew(Connection conn, String message, String type) throws SQLException {
        // Check if same message exists in last 24 hours to avoid duplicates
        String checkSql = "SELECT COUNT(*) FROM customer_notifications WHERE customer_id=? AND type=? " +
                          "AND message=? AND created_at > DATE_SUB(NOW(), INTERVAL 24 HOUR)";
        try (PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setInt(1, customer.getCustomerId());
            check.setString(2, type);
            check.setString(3, message);
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return; // already exists
        }
        String insertSql = "INSERT INTO customer_notifications (customer_id, message, type) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, customer.getCustomerId());
            ps.setString(2, message);
            ps.setString(3, type);
            ps.executeUpdate();
        }
    }

    private void markRowRead(int row) {
        Object idObj = tableModel.getValueAt(row, 0);
        if (idObj == null) return;
        int notifId = (int) idObj;
        String sql = "UPDATE customer_notifications SET is_read=TRUE WHERE notification_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notifId);
            ps.executeUpdate();
            tableModel.setValueAt("READ", row, 4);
            // Update unread count
            long unread = 0;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if ("UNREAD".equals(tableModel.getValueAt(i, 4))) unread++;
            }
            unreadCountLbl.setText(unread + " unread");
            unreadCountLbl.setForeground(unread > 0 ? UITheme.WARNING : UITheme.SUCCESS);
        } catch (SQLException ignored) {}
    }

    private void markAllRead() {
        String sql = "UPDATE customer_notifications SET is_read=TRUE WHERE customer_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ps.executeUpdate();
            loadNotifications();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void clearRead() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete all read notifications?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        String sql = "DELETE FROM customer_notifications WHERE customer_id=? AND is_read=TRUE";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ps.executeUpdate();
            loadNotifications();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void addLegendItem(JPanel panel, String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(color);
        panel.add(lbl);
    }
}
