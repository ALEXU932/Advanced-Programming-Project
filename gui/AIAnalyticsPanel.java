package gui;
import logic.AnomalyDetector;
import logic.ConsumptionPredictor;
import database.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AIAnalyticsPanel extends JPanel {
    private JComboBox<Object> customerCb;
    private JTextArea resultArea;
    private JPanel chartPanel;
    private List<Double> chartData = new ArrayList<>();
    private double predictedValue = 0;

    public AIAnalyticsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("🤖 AI Analytics & Predictions");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 16, 0));
        center.setOpaque(false);

        // Left: controls + results
        JPanel left = UITheme.createCard("Prediction Engine");
        left.setLayout(new BorderLayout(0, 12));

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx = 0; gbc.weightx = 1;
        gbc.insets = new Insets(5, 0, 5, 0);

        customerCb = UITheme.createComboBox();
        loadCustomers();
        JButton analyzeBtn = UITheme.createPrimaryButton("🔍 Analyze Customer");
        JButton anomalyBtn = UITheme.createAccentButton("⚠ Detect All Anomalies");

        gbc.gridy = 0; controls.add(UITheme.createLabel("Select Customer"), gbc);
        gbc.gridy = 1; controls.add(customerCb, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(10, 0, 5, 0); controls.add(analyzeBtn, gbc);
        gbc.gridy = 3; gbc.insets = new Insets(5, 0, 5, 0); controls.add(anomalyBtn, gbc);

        resultArea = new JTextArea(12, 30);
        resultArea.setFont(UITheme.FONT_BODY);
        resultArea.setForeground(UITheme.TEXT_WHITE);
        resultArea.setBackground(new Color(10, 25, 60, 200));
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane sp = new JScrollPane(resultArea);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createLineBorder(UITheme.ACCENT, 1));

        left.add(controls, BorderLayout.NORTH);
        left.add(sp, BorderLayout.CENTER);

        // Right: chart
        chartPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawChart((Graphics2D) g);
            }
        };
        chartPanel.setOpaque(false);
        JPanel rightCard = UITheme.createCard("Consumption Chart");
        rightCard.setLayout(new BorderLayout());
        rightCard.add(chartPanel, BorderLayout.CENTER);

        center.add(left);
        center.add(rightCard);
        add(center, BorderLayout.CENTER);

        // Bottom: anomalies table
        JPanel bottom = UITheme.createCard("Anomaly Log");
        bottom.setLayout(new BorderLayout());
        bottom.setPreferredSize(new Dimension(0, 200));
        String[] cols = {"Customer", "Detected At", "Severity", "Description", "Resolved"};
        DefaultTableModel anomalyModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable anomalyTable = new JTable(anomalyModel);
        UITheme.styleTable(anomalyTable);
        bottom.add(UITheme.createScrollPane(anomalyTable), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        analyzeBtn.addActionListener(e -> analyzeCustomer(anomalyModel));
        anomalyBtn.addActionListener(e -> loadAllAnomalies(anomalyModel));
        loadAllAnomalies(anomalyModel);
    }

    private void analyzeCustomer(DefaultTableModel anomalyModel) {
        if (customerCb.getSelectedItem() == null) return;
        int customerId = (int) ((Object[]) customerCb.getSelectedItem())[0];
        String customerName = (String) ((Object[]) customerCb.getSelectedItem())[1];

        List<Double> history = getHistory(customerId);
        chartData = new ArrayList<>(history);

        double predicted   = ConsumptionPredictor.predict(history);
        double confidence  = ConsumptionPredictor.confidence(history);
        String recommendation = ConsumptionPredictor.getRecommendation(history);
        String dataQuality = ConsumptionPredictor.getDataQualityLabel(history);
        predictedValue = predicted;
        chartPanel.repaint();

        StringBuilder sb = new StringBuilder();
        sb.append("=== AI Analysis: ").append(customerName).append(" ===\n\n");
        sb.append("Data Quality  : ").append(dataQuality).append("\n");

        if (!history.isEmpty()) {
            double mean = history.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double max  = history.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double min  = history.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double last = history.get(history.size() - 1);
            sb.append(String.format("Readings      : %d months%n", history.size()));
            sb.append(String.format("Average       : %.2f kWh%n", mean));
            sb.append(String.format("Min / Max     : %.2f / %.2f kWh%n", min, max));
            sb.append(String.format("Last Month    : %.2f kWh%n%n", last));
        } else {
            sb.append("Readings      : None yet\n");
            sb.append(String.format("Baseline Avg  : %.2f kWh (industry average)%n%n", 150.0));
        }

        sb.append(String.format("Predicted Next Month : %.2f kWh%n", predicted));
        sb.append(String.format("Confidence           : %.1f%%%n%n", confidence));
        sb.append("Recommendation:\n").append(recommendation).append("\n");

        // Anomaly check (only if enough data)
        if (history.size() >= 3) {
            double last = history.get(history.size() - 1);
            if (AnomalyDetector.isAnomaly(last, history.subList(0, history.size() - 1))) {
                double z = AnomalyDetector.getZScore(last, history.subList(0, history.size() - 1));
                sb.append("\nANOMALY DETECTED in last reading!\n");
                sb.append("Z-Score  : ").append(String.format("%.2f", z)).append("\n");
                sb.append("Severity : ").append(AnomalyDetector.getSeverity(z)).append("\n");
            } else {
                sb.append("\nNo anomaly in last reading.\n");
            }
        }

        resultArea.setText(sb.toString());
        loadAllAnomalies(anomalyModel);
    }

    private void loadAllAnomalies(DefaultTableModel model) {
        model.setRowCount(0);
        String sql = "SELECT c.name, a.detected_at, a.severity, a.description, a.is_resolved " +
                     "FROM anomalies a JOIN customers c ON a.customer_id=c.customer_id ORDER BY a.detected_at DESC LIMIT 50";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4) != null ? rs.getString(4).substring(0, Math.min(60, rs.getString(4).length())) + "..." : "",
                    rs.getBoolean(5) ? "✓ Yes" : "✗ No"
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private List<Double> getHistory(int customerId) {
        List<Double> list = new ArrayList<>();
        String sql = "SELECT consumption_kwh FROM meter_readings WHERE customer_id=? ORDER BY reading_date ASC LIMIT 24";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getDouble(1));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private void loadCustomers() {
        String sql = "SELECT customer_id, name FROM customers ORDER BY name";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) customerCb.addItem(new Object[]{rs.getInt(1), rs.getString(2)});
        } catch (SQLException e) { e.printStackTrace(); }
        customerCb.setRenderer((list, value, index, sel, focus) -> {
            JLabel lbl = new JLabel(value == null ? "" : (String) ((Object[]) value)[1]);
            lbl.setForeground(UITheme.TEXT_WHITE);
            lbl.setBackground(sel ? UITheme.PRIMARY : new Color(20, 40, 80));
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return lbl;
        });
    }

    private void drawChart(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = chartPanel.getWidth(), h = chartPanel.getHeight();
        int pad = 50, chartW = w - pad * 2, chartH = h - pad * 2;
        if (chartData.isEmpty() || chartW <= 0 || chartH <= 0) {
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.FONT_BODY);
            g2.drawString("Select a customer to view chart", w / 2 - 120, h / 2);
            return;
        }

        List<Double> allData = new ArrayList<>(chartData);
        if (predictedValue > 0) allData.add(predictedValue);
        double maxVal = allData.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double minVal = allData.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double range = maxVal - minVal == 0 ? 1 : maxVal - minVal;

        // Grid lines
        g2.setColor(new Color(255, 255, 255, 30));
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0));
        for (int i = 0; i <= 5; i++) {
            int y = pad + (int) (chartH * i / 5.0);
            g2.drawLine(pad, y, pad + chartW, y);
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.FONT_SMALL);
            g2.drawString(String.format("%.0f", maxVal - range * i / 5), 5, y + 4);
            g2.setColor(new Color(255, 255, 255, 30));
        }

        // Axes
        g2.setColor(UITheme.TEXT_MUTED);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(pad, pad, pad, pad + chartH);
        g2.drawLine(pad, pad + chartH, pad + chartW, pad + chartH);

        if (chartData.size() < 2) return;
        int n = chartData.size();
        int stepX = chartW / (n + 1);

        // Historical line
        g2.setColor(UITheme.ACCENT);
        g2.setStroke(new BasicStroke(2.5f));
        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = pad + stepX * (i + 1);
            ys[i] = pad + chartH - (int) ((chartData.get(i) - minVal) / range * chartH);
        }
        for (int i = 0; i < n - 1; i++) g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);

        // Dots
        g2.setColor(UITheme.ACCENT);
        for (int i = 0; i < n; i++) {
            g2.fillOval(xs[i] - 4, ys[i] - 4, 8, 8);
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.FONT_SMALL);
            g2.drawString(String.format("%.0f", chartData.get(i)), xs[i] - 12, ys[i] - 8);
            g2.setColor(UITheme.ACCENT);
        }

        // Predicted point
        if (predictedValue > 0) {
            int px = pad + stepX * (n + 1);
            int py = pad + chartH - (int) ((predictedValue - minVal) / range * chartH);
            g2.setColor(UITheme.PRIMARY);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0));
            g2.drawLine(xs[n - 1], ys[n - 1], px, py);
            g2.setStroke(new BasicStroke(2f));
            g2.fillOval(px - 6, py - 6, 12, 12);
            g2.setColor(UITheme.TEXT_WHITE);
            g2.setFont(UITheme.FONT_SMALL);
            g2.drawString("Pred: " + String.format("%.0f", predictedValue), px - 20, py - 10);
        }

        // Legend
        g2.setColor(UITheme.ACCENT);
        g2.fillRect(pad, h - 20, 12, 3);
        g2.setColor(UITheme.TEXT_LIGHT);
        g2.setFont(UITheme.FONT_SMALL);
        g2.drawString("Historical", pad + 16, h - 16);
        g2.setColor(UITheme.PRIMARY);
        g2.fillRect(pad + 100, h - 20, 12, 3);
        g2.setColor(UITheme.TEXT_LIGHT);
        g2.drawString("Predicted", pad + 116, h - 16);
    }
}
