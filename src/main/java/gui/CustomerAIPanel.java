package gui;

import Logic.AnomalyDetector;
import Logic.ConsumptionPredictor;
import database.DatabaseManager;
import database.Customer;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerAIPanel extends JPanel {
    private final Customer customer;

    public CustomerAIPanel(Customer customer) {
        this.customer = customer;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("🤖 AI Energy Insights");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setOpaque(false);

        List<Double> history = getHistory();

        grid.add(buildPredictionCard(history));
        grid.add(buildAnomalyCard(history));
        grid.add(buildTipsCard());
        grid.add(buildStatsCard(history));

        add(grid, BorderLayout.CENTER);
    }

    private JPanel buildPredictionCard(List<Double> history) {
        JPanel card = UITheme.createCard("Next Month Prediction");
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(8, 0, 8, 0);

        double predicted  = ConsumptionPredictor.predict(history);
        double confidence = ConsumptionPredictor.confidence(history);

        JLabel predLbl = new JLabel(String.format("%.2f kWh", predicted), SwingConstants.CENTER);
        predLbl.setFont(new Font("Segoe UI", Font.BOLD, 32));
        predLbl.setForeground(UITheme.PRIMARY);

        JLabel confLbl = new JLabel(String.format("Confidence: %.1f%%", confidence), SwingConstants.CENTER);
        confLbl.setFont(UITheme.FONT_LABEL);
        confLbl.setForeground(UITheme.TEXT_LIGHT);

        JLabel dataLbl = new JLabel(ConsumptionPredictor.getConfidenceMessage(history), SwingConstants.CENTER);
        dataLbl.setFont(UITheme.FONT_SMALL);
        dataLbl.setForeground(history.size() < 6 ? UITheme.WARNING : UITheme.TEXT_MUTED);

        String rec = ConsumptionPredictor.getRecommendation(history);
        // Show first line only in card
        String recShort = rec.contains("\n") ? rec.substring(0, rec.indexOf("\n")) : rec;
        JLabel recLbl = new JLabel("<html><div style='text-align:center;width:200px'>" + recShort + "</div></html>", SwingConstants.CENTER);
        recLbl.setFont(UITheme.FONT_SMALL);
        recLbl.setForeground(UITheme.TEXT_LIGHT);

        content.add(predLbl, gbc);
        gbc.gridy++; content.add(confLbl, gbc);
        gbc.gridy++; content.add(dataLbl, gbc);
        gbc.gridy++; content.add(recLbl, gbc);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildAnomalyCard(List<Double> history) {
        JPanel card = UITheme.createCard("⚠ Anomaly Status");
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(8, 0, 8, 0);

        int anomalyCount = getAnomalyCount();
        Color statusColor = anomalyCount == 0 ? UITheme.SUCCESS : UITheme.DANGER;
        String statusText = anomalyCount == 0 ? "✅ No Anomalies" : "⚠ " + anomalyCount + " Anomaly(ies)";

        JLabel statusLbl = new JLabel(statusText, SwingConstants.CENTER);
        statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        statusLbl.setForeground(statusColor);

        JLabel descLbl = new JLabel(anomalyCount == 0 ?
            "Your consumption is normal." : "Unusual patterns detected. Contact admin.",
            SwingConstants.CENTER);
        descLbl.setFont(UITheme.FONT_SMALL);
        descLbl.setForeground(UITheme.TEXT_LIGHT);

        if (history.size() >= 3) {
            double last = history.get(history.size() - 1);
            double z = AnomalyDetector.getZScore(last, history.subList(0, history.size() - 1));
            JLabel zLbl = new JLabel(String.format("Last Z-Score: %.2f", z), SwingConstants.CENTER);
            zLbl.setFont(UITheme.FONT_SMALL);
            zLbl.setForeground(UITheme.TEXT_MUTED);
            content.add(zLbl, gbc);
            gbc.gridy++;
        }

        content.add(statusLbl, gbc);
        gbc.gridy++; content.add(descLbl, gbc);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildTipsCard() {
        JPanel card = UITheme.createCard("💡 Energy Saving Tips");
        JTextArea area = new JTextArea();
        area.setFont(UITheme.FONT_BODY);
        area.setForeground(UITheme.TEXT_WHITE);
        area.setBackground(new Color(0, 0, 0, 0));
        area.setEditable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setText(
            "🌡 Set AC to 24-26°C for optimal efficiency\n\n" +
            "💡 Replace bulbs with LED (saves up to 75%)\n\n" +
            "🔌 Unplug chargers and devices when not in use\n\n" +
            "🌅 Use natural light during daytime\n\n" +
            "🧺 Run washing machines with full loads\n\n" +
            "❄ Keep fridge away from heat sources\n\n" +
            "⏰ Use appliances during off-peak hours (10pm-6am)"
        );
        card.add(area, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildStatsCard(List<Double> history) {
        JPanel card = UITheme.createCard("📊 Consumption Statistics");
        JPanel content = new JPanel(new GridLayout(0, 2, 8, 8));
        content.setOpaque(false);

        if (!history.isEmpty()) {
            double avg = history.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double max = history.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double min = history.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double last = history.get(history.size() - 1);

            addStat(content, "Average", String.format("%.2f kWh", avg), UITheme.ACCENT);
            addStat(content, "Highest", String.format("%.2f kWh", max), UITheme.DANGER);
            addStat(content, "Lowest", String.format("%.2f kWh", min), UITheme.SUCCESS);
            addStat(content, "Last Month", String.format("%.2f kWh", last), UITheme.PRIMARY);
            addStat(content, "Readings", String.valueOf(history.size()), UITheme.TEXT_LIGHT);
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
}
