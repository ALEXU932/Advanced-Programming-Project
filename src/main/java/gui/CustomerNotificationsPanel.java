package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import database.DatabaseManager;
import database.Customer;

public class CustomerNotificationsPanel extends JPanel {

    private final Customer customer;
    private JPanel notifContainer;
    private JLabel unreadBadge;

    public CustomerNotificationsPanel(Customer customer) {
        this.customer = customer;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadNotifications();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        JLabel title = new JLabel("\uD83D\uDD14 Notifications");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        unreadBadge = new JLabel("0");
        unreadBadge.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(11)));
        unreadBadge.setForeground(Color.WHITE);
        unreadBadge.setOpaque(true);
        unreadBadge.setBackground(UITheme.DANGER);
        unreadBadge.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
        titleRow.add(title); titleRow.add(unreadBadge);
        header.add(titleRow, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton markAllBtn = UITheme.createAccentButton("Mark All Read");
        JButton refreshBtn = UITheme.createAccentButton("\u21BB Refresh");
        JButton clearBtn   = UITheme.createDangerButton("Clear All");
        markAllBtn.setPreferredSize(new Dimension(UITheme.dim(130), UITheme.dim(32)));
        refreshBtn.setPreferredSize(new Dimension(UITheme.dim(90),  UITheme.dim(32)));
        clearBtn.setPreferredSize(new Dimension(UITheme.dim(90),    UITheme.dim(32)));
        actions.add(markAllBtn); actions.add(refreshBtn); actions.add(clearBtn);
        header.add(actions, BorderLayout.EAST);

        notifContainer = new JPanel();
        notifContainer.setOpaque(false);
        notifContainer.setLayout(new BoxLayout(notifContainer, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(notifContainer);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(40, 80, 150), 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        markAllBtn.addActionListener(e -> { unreadBadge.setText("0"); unreadBadge.setVisible(false); });
        refreshBtn.addActionListener(e -> loadNotifications());
        clearBtn.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this, "Clear all notifications?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) clearAll();
        });
    }

    private void loadNotifications() {
        notifContainer.removeAll();
        List<NotifItem> items = new ArrayList<>();
        items.addAll(getOverdueBillNotifs());
        items.addAll(getDueSoonNotifs());
        items.addAll(getRecentPaymentNotifs());
        items.addAll(getBudgetNotifs());
        items.addAll(getAnomalyNotifs());
        items.addAll(getDisputeNotifs());

        int unread = 0;
        if (items.isEmpty()) {
            JPanel empty = new JPanel(new BorderLayout());
            empty.setOpaque(false);
            empty.setBorder(BorderFactory.createEmptyBorder(40, 0, 40, 0));
            JLabel emptyLbl = new JLabel("\u2705 You're all caught up! No new notifications.", SwingConstants.CENTER);
            emptyLbl.setFont(UITheme.FONT_SUBTITLE);
            emptyLbl.setForeground(UITheme.TEXT_MUTED);
            empty.add(emptyLbl, BorderLayout.CENTER);
            notifContainer.add(empty);
        } else {
            for (NotifItem item : items) {
                notifContainer.add(buildNotifCard(item));
                notifContainer.add(Box.createVerticalStrut(UITheme.dim(6)));
                if (!item.read) unread++;
            }
        }
        unreadBadge.setText(String.valueOf(unread));
        unreadBadge.setVisible(unread > 0);
        notifContainer.revalidate(); notifContainer.repaint();
    }

    private List<NotifItem> getOverdueBillNotifs() {
        List<NotifItem> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT bill_id, billing_month, total_amount, due_date FROM bills WHERE customer_id=? AND status='OVERDUE'")) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new NotifItem("\uD83D\uDEA8 Overdue Bill",
                    String.format("Bill #%06d for %s is OVERDUE! Amount: $%.2f. Please pay immediately.",
                        rs.getInt(1), rs.getString(2), rs.getDouble(3)),
                    "DANGER", rs.getString(4), false));
        } catch (SQLException e) { /* ignore */ }
        return list;
    }

    private List<NotifItem> getDueSoonNotifs() {
        List<NotifItem> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT bill_id, billing_month, total_amount, due_date FROM bills WHERE customer_id=? " +
                 "AND status='PENDING' AND due_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY)")) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new NotifItem("\u23F0 Bill Due Soon",
                    String.format("Bill #%06d for %s is due on %s. Amount: $%.2f.",
                        rs.getInt(1), rs.getString(2), rs.getString(4), rs.getDouble(3)),
                    "WARNING", rs.getString(4), false));
        } catch (SQLException e) { /* ignore */ }
        return list;
    }

    private List<NotifItem> getRecentPaymentNotifs() {
        List<NotifItem> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT payment_id, bill_id, amount, payment_method, payment_date FROM payments " +
                 "WHERE customer_id=? AND payment_date >= DATE_SUB(NOW(), INTERVAL 30 DAY) ORDER BY payment_date DESC LIMIT 5")) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new NotifItem("\u2705 Payment Confirmed",
                    String.format("Payment of $%.2f for Bill #%06d via %s on %s.",
                        rs.getDouble(3), rs.getInt(2), rs.getString(4), rs.getString(5)),
                    "SUCCESS", rs.getString(5), true));
        } catch (SQLException e) { /* ignore */ }
        return list;
    }

    private List<NotifItem> getBudgetNotifs() {
        List<NotifItem> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT cb.monthly_budget_kwh, cb.alert_threshold, COALESCE(SUM(mr.consumption_kwh),0) as used " +
                 "FROM customer_budgets cb LEFT JOIN meter_readings mr ON mr.customer_id=cb.customer_id " +
                 "AND MONTH(mr.reading_date)=MONTH(NOW()) AND YEAR(mr.reading_date)=YEAR(NOW()) " +
                 "WHERE cb.customer_id=? AND cb.is_active=TRUE GROUP BY cb.budget_id")) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double budget = rs.getDouble(1), used = rs.getDouble(3);
                int threshold = rs.getInt(2);
                double pct = budget > 0 ? (used / budget) * 100 : 0;
                if (pct >= 100)
                    list.add(new NotifItem("\uD83D\uDEA8 Budget Exceeded!",
                        String.format("You've used %.1f kWh (%.0f%%) of your %.0f kWh budget!", used, pct, budget),
                        "DANGER", new SimpleDateFormat("yyyy-MM-dd").format(new Date()), false));
                else if (pct >= threshold)
                    list.add(new NotifItem("\u26A0 Budget Warning",
                        String.format("You've used %.1f kWh (%.0f%%) of your %.0f kWh monthly budget.", used, pct, budget),
                        "WARNING", new SimpleDateFormat("yyyy-MM-dd").format(new Date()), false));
            }
        } catch (SQLException e) { /* ignore */ }
        return list;
    }

    private List<NotifItem> getAnomalyNotifs() {
        List<NotifItem> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT anomaly_id, description, severity, detected_at FROM anomalies " +
                 "WHERE customer_id=? AND is_resolved=FALSE ORDER BY detected_at DESC LIMIT 3")) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new NotifItem("\uD83E\uDD16 Unusual Usage Detected",
                    String.format("Anomaly (%s severity): %s", rs.getString(3),
                        rs.getString(2) != null ? rs.getString(2) : "Unusual consumption pattern."),
                    "DANGER", rs.getString(4), false));
        } catch (SQLException e) { /* ignore */ }
        return list;
    }

    private List<NotifItem> getDisputeNotifs() {
        List<NotifItem> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT dispute_id, reason, status, resolved_at FROM disputes WHERE customer_id=? " +
                 "AND status IN ('RESOLVED','REJECTED') AND resolved_at >= DATE_SUB(NOW(), INTERVAL 14 DAY)")) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String status = rs.getString(3);
                list.add(new NotifItem(("RESOLVED".equals(status) ? "\u2705" : "\u274C") + " Dispute " + status,
                    String.format("Your dispute #%06d (%s) has been %s.", rs.getInt(1), rs.getString(2), status.toLowerCase()),
                    "RESOLVED".equals(status) ? "SUCCESS" : "DANGER", rs.getString(4), true));
            }
        } catch (SQLException e) { /* ignore */ }
        return list;
    }

    private JPanel buildNotifCard(NotifItem item) {
        Color accentColor;
        if      ("DANGER".equals(item.type))  accentColor = UITheme.DANGER;
        else if ("WARNING".equals(item.type)) accentColor = UITheme.WARNING;
        else if ("SUCCESS".equals(item.type)) accentColor = UITheme.SUCCESS;
        else                                  accentColor = UITheme.ACCENT;
        JPanel card = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(item.read ? new Color(15, 30, 70, 160) : new Color(20, 40, 90, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 180));
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, UITheme.dim(70)));

        JPanel textPanel = new JPanel(new BorderLayout(0, 4));
        textPanel.setOpaque(false);
        JLabel titleLbl = new JLabel(item.title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(12)));
        titleLbl.setForeground(accentColor);
        JLabel msgLbl = new JLabel("<html><div style='width:500px'>" + item.message + "</div></html>");
        msgLbl.setFont(UITheme.FONT_SMALL);
        msgLbl.setForeground(item.read ? UITheme.TEXT_MUTED : UITheme.TEXT_LIGHT);
        textPanel.add(titleLbl, BorderLayout.NORTH);
        textPanel.add(msgLbl, BorderLayout.CENTER);

        JLabel timeLbl = new JLabel(item.time != null ? item.time : "");
        timeLbl.setFont(UITheme.FONT_SMALL);
        timeLbl.setForeground(UITheme.TEXT_MUTED);

        card.add(textPanel, BorderLayout.CENTER);
        card.add(timeLbl, BorderLayout.EAST);
        return card;
    }

    private void clearAll() {
        notifContainer.removeAll();
        JPanel empty = new JPanel(new BorderLayout());
        empty.setOpaque(false);
        empty.setBorder(BorderFactory.createEmptyBorder(40, 0, 40, 0));
        JLabel emptyLbl = new JLabel("\u2705 All notifications cleared.", SwingConstants.CENTER);
        emptyLbl.setFont(UITheme.FONT_SUBTITLE);
        emptyLbl.setForeground(UITheme.TEXT_MUTED);
        empty.add(emptyLbl, BorderLayout.CENTER);
        notifContainer.add(empty);
        unreadBadge.setText("0"); unreadBadge.setVisible(false);
        notifContainer.revalidate(); notifContainer.repaint();
    }

    private static class NotifItem {
        final String title, message, type, time;
        final boolean read;
        NotifItem(String title, String message, String type, String time, boolean read) {
            this.title = title; this.message = message;
            this.type = type; this.time = time; this.read = read;
        }
    }
}
