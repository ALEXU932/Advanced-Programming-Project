package gui;

import database.DatabaseManager;
import database.Customer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerReadingsPanel extends JPanel {
    private final Customer customer;
    private JTable table;
    private DefaultTableModel tableModel;
    private JPanel chartPanel;
    private List<Double> chartData = new ArrayList<>();

    public CustomerReadingsPanel(Customer customer) {
        this.customer = customer;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadData();
    }

    private void buildUI() {
        JLabel title = new JLabel("My Meter Readings");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 16, 0));
        center.setOpaque(false);

        // Table card
        String[] cols = {"Date", "Prev Reading", "Curr Reading", "Consumption (kWh)"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        JPanel tableCard = UITheme.createCard("Reading History");
        tableCard.setLayout(new BorderLayout());
        tableCard.add(UITheme.createScrollPane(table), BorderLayout.CENTER);

        // Chart card
        chartPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawChart((Graphics2D) g);
            }
        };
        chartPanel.setOpaque(false);
        JPanel chartCard = UITheme.createCard("Consumption Trend");
        chartCard.setLayout(new BorderLayout());
        chartCard.add(chartPanel, BorderLayout.CENTER);

        center.add(tableCard);
        center.add(chartCard);
        add(center, BorderLayout.CENTER);
    }

    private void loadData() {
        tableModel.setRowCount(0);
        chartData.clear();
        String sql = "SELECT reading_date, previous_reading, current_reading, consumption_kwh " +
                     "FROM meter_readings WHERE customer_id=? ORDER BY reading_date DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            List<Double> tempData = new ArrayList<>();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString(1),
                    String.format("%.2f", rs.getDouble(2)),
                    String.format("%.2f", rs.getDouble(3)),
                    String.format("%.2f", rs.getDouble(4))
                });
                tempData.add(0, rs.getDouble(4));
            }
            chartData = tempData;
            chartPanel.repaint();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void drawChart(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = chartPanel.getWidth(), h = chartPanel.getHeight();
        int pad = 45, chartW = w - pad * 2, chartH = h - pad * 2;
        if (chartData.size() < 2 || chartW <= 0 || chartH <= 0) {
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.FONT_BODY);
            g2.drawString("No data available", w / 2 - 60, h / 2);
            return;
        }
        double maxVal = chartData.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double minVal = chartData.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double range = maxVal - minVal == 0 ? 1 : maxVal - minVal;
        int n = chartData.size();
        int stepX = chartW / (n + 1);

        // Grid
        g2.setColor(new Color(255, 255, 255, 25));
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0));
        for (int i = 0; i <= 4; i++) {
            int y = pad + (int) (chartH * i / 4.0);
            g2.drawLine(pad, y, pad + chartW, y);
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.FONT_SMALL);
            g2.drawString(String.format("%.0f", maxVal - range * i / 4), 2, y + 4);
            g2.setColor(new Color(255, 255, 255, 25));
        }

        // Axes
        g2.setColor(UITheme.TEXT_MUTED);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(pad, pad, pad, pad + chartH);
        g2.drawLine(pad, pad + chartH, pad + chartW, pad + chartH);

        // Bars
        int barW = Math.max(4, stepX - 8);
        for (int i = 0; i < n; i++) {
            int x = pad + stepX * (i + 1) - barW / 2;
            int barH = (int) ((chartData.get(i) - minVal) / range * chartH);
            int y = pad + chartH - barH;
            GradientPaint gp = new GradientPaint(x, y, UITheme.ACCENT, x, pad + chartH, new Color(0, 80, 150));
            g2.setPaint(gp);
            g2.fillRoundRect(x, y, barW, barH, 4, 4);
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.FONT_SMALL);
            g2.drawString(String.format("%.0f", chartData.get(i)), x, y - 4);
        }
    }
}
