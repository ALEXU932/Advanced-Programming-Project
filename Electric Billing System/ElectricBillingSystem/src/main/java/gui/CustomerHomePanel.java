package gui;

import ai.ConsumptionPredictor;
import db.DatabaseManager;
import models.Customer;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CustomerHomePanel extends JPanel {
    private final Customer customer;

    public CustomerHomePanel(Customer customer) {
        this.customer = customer;
        setOpaque(false);
        setLayout(new BorderLayout(0, UITheme.dim(16)));
        setBorder(BorderFactory.createEmptyBorder(UITheme.dim(34), UITheme.dim(24), UITheme.dim(24), UITheme.dim(24)));
        buildUI();
    }

    private void buildUI() {
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setOpaque(false);
        JLabel welcome = new JLabel("Hello, " + customer.getName() + "  :)");
        welcome.setFont(UITheme.FONT_TITLE);
        welcome.setForeground(UITheme.TEXT_WHITE);
        JButton budgetBtn = UITheme.createAccentButton("Set Monthly Budget");
        budgetBtn.setPreferredSize(new Dimension(180, 34));
        budgetBtn.addActionListener(e -> showBudgetDialog());
        topBar.add(welcome, BorderLayout.WEST);
        topBar.add(budgetBtn, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        JPanel bannerWrapper = new JPanel(new BorderLayout());
        bannerWrapper.setOpaque(false);
        JPanel budgetBanner = buildBudgetBanner();
        if (budgetBanner != null)
            bannerWrapper.add(budgetBanner, BorderLayout.CENTER);

        // Row 1: 4 primary stat cards
        JPanel statsRow1 = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow1.setOpaque(false);
        statsRow1.add(createStatCard("\u25A6 This Month", getThisMonthConsumption() + " kWh", UITheme.ACCENT));
        statsRow1.add(createStatCard("\u20BF Outstanding", "$" + getOutstandingAmount(), UITheme.WARNING));
        statsRow1.add(createStatCard("\u25B2 AI Prediction", getPrediction() + " kWh", UITheme.PRIMARY));
        statsRow1.add(createStatCard("\u2261 Pending Bills", String.valueOf(getPendingBillCount()), UITheme.DANGER));

        // Row 2: 4 secondary stat cards
        JPanel statsRow2 = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow2.setOpaque(false);
        statsRow2.add(createStatCard("\u2714 Last Payment", getLastPaymentDate(), UITheme.SUCCESS));
        statsRow2.add(createStatCard("\u25B6 Tariff Rate", getCurrentTariffRate(), UITheme.ACCENT));
        statsRow2.add(createStatCard("\u25C6 Days to Due", String.valueOf(getDaysToDue()), UITheme.WARNING));
        statsRow2.add(createStatCard("\u2248 Avg Monthly", getAvgMonthly() + " kWh", UITheme.TEXT_LIGHT));

        JPanel middle = new JPanel(new GridLayout(1, 2, 16, 0));
        middle.setOpaque(false);
        middle.add(buildRecentBillsCard());
        middle.add(buildAIRecommendationCard());

        JPanel statsRows = new JPanel(new GridLayout(2, 1, 0, 10));
        statsRows.setOpaque(false);
        statsRows.add(statsRow1);
        statsRows.add(statsRow2);

        JPanel statsAndMiddle = new JPanel(new BorderLayout(0, 12));
        statsAndMiddle.setOpaque(false);
        statsAndMiddle.add(statsRows, BorderLayout.NORTH);
        statsAndMiddle.add(middle, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        if (budgetBanner != null)
            center.add(bannerWrapper, BorderLayout.NORTH);
        center.add(statsAndMiddle, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private JPanel buildBudgetBanner() {
        String sql = "SELECT cb.monthly_budget_kwh, cb.alert_threshold, " +
                "COALESCE(SUM(mr.consumption_kwh),0) as used " +
                "FROM customer_budgets cb " +
                "LEFT JOIN meter_readings mr ON mr.customer_id=cb.customer_id " +
                "  AND MONTH(mr.reading_date)=MONTH(NOW()) AND YEAR(mr.reading_date)=YEAR(NOW()) " +
                "WHERE cb.customer_id=? AND cb.is_active=TRUE GROUP BY cb.budget_id";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double budget = rs.getDouble("monthly_budget_kwh");
                int threshold = rs.getInt("alert_threshold");
                double used = rs.getDouble("used");
                double pct = budget > 0 ? (used / budget) * 100 : 0;
                if (pct >= threshold) {
                    JPanel banner = new JPanel(new BorderLayout(10, 0)) {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setColor(new Color(180, 60, 0, 200));
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                            g2.dispose();
                        }
                    };
                    banner.setOpaque(false);
                    banner.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
                    JLabel msg = new JLabel(String.format(
                            "\u26A0  Budget Alert: You've used %.1f kWh (%.0f%%) of your %.0f kWh monthly budget!",
                            used, pct, budget));
                    msg.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(12)));
                    msg.setForeground(Color.WHITE);
                    banner.add(msg, BorderLayout.CENTER);
                    return banner;
                }
            }
        } catch (SQLException e) {
            /* ignore */ }
        return null;
    }

    private void showBudgetDialog() {
        FormDialog dialog = new FormDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Set Monthly Budget", "Set your monthly electricity usage budget", 480, 300);
        String existing = "150", existingThreshold = "80";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT monthly_budget_kwh, alert_threshold FROM customer_budgets WHERE customer_id=?")) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                existing = String.valueOf((int) rs.getDouble(1));
                existingThreshold = String.valueOf(rs.getInt(2));
            }
        } catch (SQLException ignored) {
        }
        JTextField budgetF = FormDialog.makeField(existing);
        JTextField thresholdF = FormDialog.makeField(existingThreshold);
        dialog.addField("Monthly Budget (kWh) *", budgetF);
        dialog.addField("Alert Threshold (% of budget, e.g. 80)", thresholdF);
        dialog.addStatus();
        dialog.addCancelButton();
        JButton saveBtn = dialog.addSaveButton("  Save Budget  ");
        saveBtn.addActionListener(e -> {
            try {
                double budget = Double.parseDouble(budgetF.getText().trim());
                int threshold = Integer.parseInt(thresholdF.getText().trim());
                if (budget <= 0) {
                    dialog.setStatus("Budget must be > 0.", true);
                    return;
                }
                if (threshold < 1 || threshold > 100) {
                    dialog.setStatus("Threshold must be 1-100.", true);
                    return;
                }
                try (Connection conn = DatabaseManager.getInstance().getConnection();
                        PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO customer_budgets (customer_id,monthly_budget_kwh,alert_threshold,is_active) "
                                        +
                                        "VALUES (?,?,?,TRUE) ON DUPLICATE KEY UPDATE monthly_budget_kwh=?,alert_threshold=?,is_active=TRUE")) {
                    ps.setInt(1, customer.getCustomerId());
                    ps.setDouble(2, budget);
                    ps.setInt(3, threshold);
                    ps.setDouble(4, budget);
                    ps.setInt(5, threshold);
                    ps.executeUpdate();
                    dialog.setStatus("Budget saved!", false);
                    dialog.dispose();
                    removeAll();
                    buildUI();
                    revalidate();
                    repaint();
                }
            } catch (NumberFormatException ex) {
                dialog.setStatus("Please enter valid numbers.", true);
            } catch (SQLException ex) {
                dialog.setStatus("Error: " + ex.getMessage(), true);
            }
        });
        dialog.setVisible(true);
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 30, 70, 210));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JLabel valLbl = new JLabel(value, SwingConstants.CENTER);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(16)));
        valLbl.setForeground(color);
        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(UITheme.FONT_SMALL);
        titleLbl.setForeground(UITheme.TEXT_LIGHT);
        card.add(valLbl, BorderLayout.CENTER);
        card.add(titleLbl, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildRecentBillsCard() {
        JPanel card = UITheme.createCard("Recent Bills");
        String[] cols = { "Month", "Consumption", "Amount", "Status", "Due Date" };
        Object[][] data = getRecentBills();
        JTable table = new JTable(data, cols);
        UITheme.styleTable(table);
        card.add(UITheme.createScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildAIRecommendationCard() {
        JPanel card = UITheme.createCard("AI Energy Insights");
        JTextArea area = new JTextArea();
        area.setFont(UITheme.FONT_BODY);
        area.setForeground(UITheme.TEXT_WHITE);
        area.setBackground(new Color(10, 25, 60, 180));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        area.setText(getAIInsights());
        JScrollPane sp = new JScrollPane(area);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ── Data queries ──────────────────────────────────────────────────────────

    private String getThisMonthConsumption() {
        String sql = "SELECT SUM(consumption_kwh) FROM meter_readings WHERE customer_id=? AND MONTH(reading_date)=MONTH(NOW()) AND YEAR(reading_date)=YEAR(NOW())";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? String.format("%.2f", rs.getDouble(1)) : "0.00";
        } catch (SQLException e) {
            return "N/A";
        }
    }

    private String getOutstandingAmount() {
        String sql = "SELECT COALESCE(SUM(total_amount),0) FROM bills WHERE customer_id=? AND status != 'PAID'";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? String.format("%.2f", rs.getDouble(1)) : "0.00";
        } catch (SQLException e) {
            return "N/A";
        }
    }

    private String getPrediction() {
        List<Double> history = getHistory();
        return String.format("%.2f", ConsumptionPredictor.predict(history));
    }

    private int getPendingBillCount() {
        String sql = "SELECT COUNT(*) FROM bills WHERE customer_id=? AND status != 'PAID'";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    private String getLastPaymentDate() {
        String sql = "SELECT MAX(payment_date) FROM payments WHERE customer_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getTimestamp(1) != null)
                return new SimpleDateFormat("dd MMM yyyy").format(rs.getTimestamp(1));
        } catch (SQLException e) {
            /* ignore */ }
        return "No payments";
    }

    private String getCurrentTariffRate() {
        String sql = "SELECT rate_per_kwh FROM tariffs WHERE is_active=TRUE ORDER BY tariff_id DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? String.format("$%.4f/kWh", rs.getDouble(1)) : "N/A";
        } catch (SQLException e) {
            return "N/A";
        }
    }

    private int getDaysToDue() {
        String sql = "SELECT MIN(DATEDIFF(due_date, NOW())) FROM bills WHERE customer_id=? AND status='PENDING' AND due_date >= NOW()";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getObject(1) != null)
                return rs.getInt(1);
        } catch (SQLException e) {
            /* ignore */ }
        return 0;
    }

    private String getAvgMonthly() {
        String sql = "SELECT AVG(consumption_kwh) FROM meter_readings WHERE customer_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? String.format("%.2f", rs.getDouble(1)) : "0.00";
        } catch (SQLException e) {
            return "N/A";
        }
    }

    private String getAIInsights() {
        List<Double> history = getHistory();
        StringBuilder sb = new StringBuilder();
        sb.append(ConsumptionPredictor.getRecommendation(history)).append("\n\n");
        double predicted = ConsumptionPredictor.predict(history);
        double confidence = ConsumptionPredictor.confidence(history);
        String dataLabel = ConsumptionPredictor.getDataQualityLabel(history);
        sb.append(String.format("Next Month Prediction: %.2f kWh%n", predicted));
        sb.append(String.format("Confidence: %.1f%%%n", confidence));
        sb.append("Data Quality: ").append(dataLabel).append("\n\n");
        sb.append("Energy Saving Tips:\n");
        sb.append("• Use appliances during off-peak hours\n");
        sb.append("• Switch to LED lighting\n");
        sb.append("• Set AC to 24-26 C for efficiency\n");
        sb.append("• Unplug devices when not in use");
        return sb.toString();
    }

    private List<Double> getHistory() {
        List<Double> list = new ArrayList<>();
        String sql = "SELECT consumption_kwh FROM meter_readings WHERE customer_id=? ORDER BY reading_date ASC LIMIT 12";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(rs.getDouble(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Object[][] getRecentBills() {
        String sql = "SELECT billing_month, consumption_kwh, total_amount, status, COALESCE(due_date,'N/A') FROM bills WHERE customer_id=? ORDER BY generated_at DESC LIMIT 6";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            List<Object[]> rows = new ArrayList<>();
            while (rs.next())
                rows.add(new Object[] {
                        rs.getString(1), String.format("%.2f kWh", rs.getDouble(2)),
                        String.format("$%.2f", rs.getDouble(3)), rs.getString(4), rs.getString(5)
                });
            return rows.toArray(new Object[0][]);
        } catch (SQLException e) {
            return new Object[0][];
        }
    }
}
