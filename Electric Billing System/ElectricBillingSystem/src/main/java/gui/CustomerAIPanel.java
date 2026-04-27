package gui;

import ai.AnomalyDetector;
import ai.ConsumptionPredictor;
import db.DatabaseManager;
import models.Customer;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerAIPanel extends JPanel {
    private final Customer customer;

    // CO2 emission factor: ~0.82 kg CO2 per kWh (global average grid)
    private static final double CO2_PER_KWH = 0.82;
    // Average household comparison baseline (kWh/month)
    private static final double AVG_HOUSEHOLD_KWH = 150.0;

    public CustomerAIPanel(Customer customer) {
        this.customer = customer;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("\u2605 AI Energy Insights");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);
        JButton refreshBtn = UITheme.createAccentButton("↻ Refresh");
        refreshBtn.addActionListener(e -> { removeAll(); buildUI(); revalidate(); repaint(); });
        header.add(refreshBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        List<Double> history = getHistory();

        // ── 3×2 grid of cards ─────────────────────────────────────────────────
        JPanel grid = new JPanel(new GridLayout(3, 2, 16, 16));
        grid.setOpaque(false);

        grid.add(buildPredictionCard(history));
        grid.add(buildAnomalyCard(history));
        grid.add(buildCarbonFootprintCard(history));
        grid.add(buildComparisonCard(history));
        grid.add(buildSmartRecommendationsCard(history));
        grid.add(buildStatsCard(history));

        add(grid, BorderLayout.CENTER);
    }

    // ── Prediction card ───────────────────────────────────────────────────────

    private JPanel buildPredictionCard(List<Double> history) {
        JPanel card = UITheme.createCard("\u25B2 Next Month Prediction");
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(6, 0, 6, 0);

        double predicted  = ConsumptionPredictor.predict(history);
        double confidence = ConsumptionPredictor.confidence(history);

        JLabel predLbl = new JLabel(String.format("%.2f kWh", predicted), SwingConstants.CENTER);
        predLbl.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(28)));
        predLbl.setForeground(UITheme.PRIMARY);

        // Estimated cost
        double rate = getActiveTariffRate();
        double estCost = predicted * rate;
        JLabel costLbl = new JLabel(String.format("Est. Cost: $%.2f", estCost), SwingConstants.CENTER);
        costLbl.setFont(UITheme.FONT_LABEL);
        costLbl.setForeground(UITheme.WARNING);

        JLabel confLbl = new JLabel(String.format("Confidence: %.1f%%", confidence), SwingConstants.CENTER);
        confLbl.setFont(UITheme.FONT_LABEL);
        confLbl.setForeground(UITheme.TEXT_LIGHT);

        JLabel dataLbl = new JLabel(ConsumptionPredictor.getConfidenceMessage(history), SwingConstants.CENTER);
        dataLbl.setFont(UITheme.FONT_SMALL);
        dataLbl.setForeground(history.size() < 6 ? UITheme.WARNING : UITheme.TEXT_MUTED);

        // Trend arrow
        String trend = "";
        Color trendColor = UITheme.TEXT_MUTED;
        if (history.size() >= 2) {
            double last = history.get(history.size() - 1);
            if (predicted > last * 1.05)      { trend = "▲ Increasing trend"; trendColor = UITheme.DANGER; }
            else if (predicted < last * 0.95) { trend = "▼ Decreasing trend"; trendColor = UITheme.SUCCESS; }
            else                              { trend = "→ Stable trend";      trendColor = UITheme.ACCENT; }
        }
        JLabel trendLbl = new JLabel(trend, SwingConstants.CENTER);
        trendLbl.setFont(UITheme.FONT_SMALL);
        trendLbl.setForeground(trendColor);

        content.add(predLbl, gbc);
        gbc.gridy++; content.add(costLbl, gbc);
        gbc.gridy++; content.add(confLbl, gbc);
        gbc.gridy++; content.add(dataLbl, gbc);
        gbc.gridy++; content.add(trendLbl, gbc);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ── Anomaly card ──────────────────────────────────────────────────────────

    private JPanel buildAnomalyCard(List<Double> history) {
        JPanel card = UITheme.createCard("\u26A0 Anomaly Detection");
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(6, 0, 6, 0);

        int anomalyCount = getAnomalyCount();
        Color statusColor = anomalyCount == 0 ? UITheme.SUCCESS : UITheme.DANGER;
        String statusText = anomalyCount == 0 ? "\u2714 No Anomalies" : "\u26A0 " + anomalyCount + " Anomaly(ies)";

        JLabel statusLbl = new JLabel(statusText, SwingConstants.CENTER);
        statusLbl.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(16)));
        statusLbl.setForeground(statusColor);

        JLabel descLbl = new JLabel(anomalyCount == 0
            ? "Your consumption pattern is normal."
            : "Unusual patterns detected. Contact support.",
            SwingConstants.CENTER);
        descLbl.setFont(UITheme.FONT_SMALL);
        descLbl.setForeground(UITheme.TEXT_LIGHT);

        content.add(statusLbl, gbc);
        gbc.gridy++; content.add(descLbl, gbc);

        if (history.size() >= 3) {
            double last = history.get(history.size() - 1);
            double z = AnomalyDetector.getZScore(last, history.subList(0, history.size() - 1));
            JLabel zLbl = new JLabel(String.format("Z-Score: %.2f  (threshold: ±2.0)", z), SwingConstants.CENTER);
            zLbl.setFont(UITheme.FONT_SMALL);
            zLbl.setForeground(Math.abs(z) > 2.0 ? UITheme.DANGER : UITheme.TEXT_MUTED);
            gbc.gridy++; content.add(zLbl, gbc);

            // Severity indicator
            String severity;
            Color sevColor;
            if (Math.abs(z) < 1.5)      { severity = "Normal";  sevColor = UITheme.SUCCESS; }
            else if (Math.abs(z) < 2.0) { severity = "Watch";   sevColor = UITheme.WARNING; }
            else if (Math.abs(z) < 3.0) { severity = "High";    sevColor = UITheme.PRIMARY; }
            else                        { severity = "Critical"; sevColor = UITheme.DANGER; }
            JLabel sevLbl = new JLabel("Severity: " + severity, SwingConstants.CENTER);
            sevLbl.setFont(UITheme.FONT_LABEL);
            sevLbl.setForeground(sevColor);
            gbc.gridy++; content.add(sevLbl, gbc);
        }

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ── Carbon footprint card ─────────────────────────────────────────────────

    private JPanel buildCarbonFootprintCard(List<Double> history) {
        JPanel card = UITheme.createCard("\u25CB Carbon Footprint");
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(5, 0, 5, 0);

        double totalKwh = history.stream().mapToDouble(Double::doubleValue).sum();
        double monthlyAvg = history.isEmpty() ? 0 : totalKwh / history.size();
        double co2Monthly = monthlyAvg * CO2_PER_KWH;
        double co2Annual  = co2Monthly * 12;

        // Green score: 100 = 0 kWh, 0 = 500+ kWh/month
        int greenScore = (int) Math.max(0, Math.min(100, 100 - (monthlyAvg / 5.0)));
        Color scoreColor = greenScore >= 70 ? UITheme.SUCCESS
                         : greenScore >= 40 ? UITheme.WARNING : UITheme.DANGER;
        String scoreLabel = greenScore >= 70 ? "Eco-Friendly"
                          : greenScore >= 40 ? "Average"
                          : "High Emitter";

        JLabel co2Lbl = new JLabel(String.format("%.1f kg CO₂/month", co2Monthly), SwingConstants.CENTER);
        co2Lbl.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(18)));
        co2Lbl.setForeground(scoreColor);

        JLabel annualLbl = new JLabel(String.format("Annual: %.0f kg CO₂", co2Annual), SwingConstants.CENTER);
        annualLbl.setFont(UITheme.FONT_LABEL);
        annualLbl.setForeground(UITheme.TEXT_LIGHT);

        // Green score bar
        JProgressBar scoreBar = new JProgressBar(0, 100);
        scoreBar.setValue(greenScore);
        scoreBar.setStringPainted(true);
        scoreBar.setString("Green Score: " + greenScore + "/100");
        scoreBar.setFont(UITheme.FONT_SMALL);
        scoreBar.setForeground(scoreColor);
        scoreBar.setBackground(new Color(20, 40, 80));
        scoreBar.setBorderPainted(false);
        scoreBar.setPreferredSize(new Dimension(180, 18));

        JLabel scoreLbl = new JLabel(scoreLabel, SwingConstants.CENTER);
        scoreLbl.setFont(UITheme.FONT_LABEL);
        scoreLbl.setForeground(scoreColor);

        // Trees equivalent
        double treesNeeded = co2Annual / 21.0; // avg tree absorbs ~21 kg CO2/year
        JLabel treeLbl = new JLabel(String.format("≈ %.1f trees needed to offset", treesNeeded), SwingConstants.CENTER);
        treeLbl.setFont(UITheme.FONT_SMALL);
        treeLbl.setForeground(UITheme.TEXT_MUTED);

        content.add(co2Lbl, gbc);
        gbc.gridy++; content.add(annualLbl, gbc);
        gbc.gridy++; content.add(scoreBar, gbc);
        gbc.gridy++; content.add(scoreLbl, gbc);
        gbc.gridy++; content.add(treeLbl, gbc);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ── Comparison card ───────────────────────────────────────────────────────

    private JPanel buildComparisonCard(List<Double> history) {
        JPanel card = UITheme.createCard("\u25A6 Usage Comparison");
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(5, 0, 5, 0);

        double myAvg = history.isEmpty() ? 0 : history.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        // vs average household
        double diffAvg = myAvg - AVG_HOUSEHOLD_KWH;
        String vsAvgText;
        Color vsAvgColor;
        if (diffAvg > 0) {
            vsAvgText  = String.format("%.1f kWh above average household", diffAvg);
            vsAvgColor = UITheme.DANGER;
        } else if (diffAvg < 0) {
            vsAvgText  = String.format("%.1f kWh below average household", Math.abs(diffAvg));
            vsAvgColor = UITheme.SUCCESS;
        } else {
            vsAvgText  = "Equal to average household";
            vsAvgColor = UITheme.ACCENT;
        }

        // vs previous month
        String vsPrevText  = "—";
        Color  vsPrevColor = UITheme.TEXT_MUTED;
        if (history.size() >= 2) {
            double last = history.get(history.size() - 1);
            double prev = history.get(history.size() - 2);
            double diff = last - prev;
            if (diff > 0) {
                vsPrevText  = String.format("+%.1f kWh vs last month (▲%.0f%%)", diff, (diff/prev)*100);
                vsPrevColor = UITheme.DANGER;
            } else {
                vsPrevText  = String.format("%.1f kWh vs last month (▼%.0f%%)", diff, (Math.abs(diff)/prev)*100);
                vsPrevColor = UITheme.SUCCESS;
            }
        }

        addCompRow(content, gbc, "My Monthly Avg",    String.format("%.1f kWh", myAvg),         UITheme.ACCENT);
        gbc.gridy++;
        addCompRow(content, gbc, "Avg Household",     String.format("%.0f kWh", AVG_HOUSEHOLD_KWH), UITheme.TEXT_MUTED);
        gbc.gridy++;
        JLabel vsAvgLbl = new JLabel(vsAvgText, SwingConstants.CENTER);
        vsAvgLbl.setFont(UITheme.FONT_SMALL);
        vsAvgLbl.setForeground(vsAvgColor);
        content.add(vsAvgLbl, gbc);
        gbc.gridy++;

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(40, 70, 120));
        content.add(sep, gbc);
        gbc.gridy++;

        JLabel vsPrevLbl = new JLabel(vsPrevText, SwingConstants.CENTER);
        vsPrevLbl.setFont(UITheme.FONT_SMALL);
        vsPrevLbl.setForeground(vsPrevColor);
        content.add(vsPrevLbl, gbc);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private void addCompRow(JPanel panel, GridBagConstraints gbc, String label, String value, Color color) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(UITheme.FONT_LABEL);
        val.setForeground(color);
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        panel.add(row, gbc);
    }

    // ── Smart recommendations card ────────────────────────────────────────────

    private JPanel buildSmartRecommendationsCard(List<Double> history) {
        JPanel card = UITheme.createCard("\u2605 Smart Recommendations");
        JTextArea area = new JTextArea();
        area.setFont(UITheme.FONT_BODY);
        area.setForeground(UITheme.TEXT_WHITE);
        area.setBackground(new Color(0, 0, 0, 0));
        area.setEditable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        StringBuilder tips = new StringBuilder();
        double avg = history.isEmpty() ? 0 : history.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        // Personalized tips based on usage level
        if (avg > 300) {
            tips.append("[!] HIGH USAGE DETECTED\n\n");
            tips.append("• Consider energy audit — usage is very high\n");
            tips.append("• Check for faulty appliances drawing power\n");
            tips.append("• Upgrade to inverter AC/refrigerator\n");
        } else if (avg > 150) {
            tips.append("[~] MODERATE USAGE\n\n");
            tips.append("• Set AC thermostat to 24-26 C\n");
            tips.append("• Use timer on water heater\n");
        } else {
            tips.append("[+] EFFICIENT USAGE\n\n");
            tips.append("• Great job! Keep maintaining good habits\n");
            tips.append("• Consider solar panels for further savings\n");
        }

        tips.append("\nGENERAL TIPS\n");
        tips.append("• Use appliances 10pm-6am (off-peak)\n");
        tips.append("• Replace bulbs with LED (saves 75%)\n");
        tips.append("• Unplug chargers when not in use\n");
        tips.append("• Full loads in washing machine\n");
        tips.append("• Natural light during daytime\n");
        tips.append("• Keep fridge away from heat sources\n\n");

        // AI recommendation
        tips.append("AI INSIGHT\n");
        tips.append(ConsumptionPredictor.getRecommendation(history));

        area.setText(tips.toString());
        JScrollPane sp = new JScrollPane(area);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ── Stats card ────────────────────────────────────────────────────────────

    private JPanel buildStatsCard(List<Double> history) {
        JPanel card = UITheme.createCard("\u25BC Consumption Statistics");
        JPanel content = new JPanel(new GridLayout(0, 2, 8, 8));
        content.setOpaque(false);

        if (!history.isEmpty()) {
            double avg  = history.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double max  = history.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double min  = history.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double last = history.get(history.size() - 1);
            double total = history.stream().mapToDouble(Double::doubleValue).sum();

            addStat(content, "Average",    String.format("%.2f kWh", avg),   UITheme.ACCENT);
            addStat(content, "Highest",    String.format("%.2f kWh", max),   UITheme.DANGER);
            addStat(content, "Lowest",     String.format("%.2f kWh", min),   UITheme.SUCCESS);
            addStat(content, "Last Month", String.format("%.2f kWh", last),  UITheme.PRIMARY);
            addStat(content, "Total",      String.format("%.2f kWh", total), UITheme.TEXT_LIGHT);
            addStat(content, "Readings",   String.valueOf(history.size()),    UITheme.TEXT_MUTED);

            // Estimated annual cost
            double rate = getActiveTariffRate();
            double annualCost = avg * 12 * rate;
            addStat(content, "Est. Annual Cost", String.format("$%.2f", annualCost), UITheme.WARNING);

            // Savings potential (if reduced by 15%)
            double savings = avg * 0.15 * rate;
            addStat(content, "15% Saving/mo", String.format("$%.2f", savings), UITheme.SUCCESS);
        } else {
            JLabel noData = new JLabel("No data available", SwingConstants.CENTER);
            noData.setForeground(UITheme.TEXT_MUTED);
            content.add(noData);
        }
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private void addStat(JPanel panel, String label, String value, Color color) {
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(UITheme.FONT_LABEL);
        lbl.setForeground(UITheme.TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(UITheme.FONT_LABEL);
        val.setForeground(color);
        panel.add(lbl);
        panel.add(val);
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    private List<Double> getHistory() {
        List<Double> list = new ArrayList<>();
        String sql = "SELECT consumption_kwh FROM meter_readings WHERE customer_id=? ORDER BY reading_date ASC LIMIT 12";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getDouble(1));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private int getAnomalyCount() {
        String sql = "SELECT COUNT(*) FROM anomalies WHERE customer_id=? AND is_resolved=FALSE";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    private double getActiveTariffRate() {
        String sql = "SELECT rate_per_kwh FROM tariffs WHERE is_active=TRUE ORDER BY tariff_id DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.12;
        } catch (SQLException e) { return 0.12; }
    }
}
