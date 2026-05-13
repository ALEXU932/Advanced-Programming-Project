package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import database.DatabaseManager;
import database.Customer;

public class CustomerUsageChartPanel extends JPanel {

    private final Customer customer;
    private JPanel chartArea;
    private JComboBox<String> chartTypeCb;
    private JTable dataTable;
    private DefaultTableModel tableModel;

    private final List<String> labels   = new ArrayList<>();
    private final List<Double> current  = new ArrayList<>();
    private final List<Double> previous = new ArrayList<>();
    private String currentChartType = "Monthly Consumption";

    public CustomerUsageChartPanel(Customer customer) {
        this.customer = customer;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadData("Monthly Consumption");
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("\uD83D\uDCCA Usage Analytics");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        chartTypeCb = new JComboBox<>(new String[]{
            "Monthly Consumption", "Compare Months", "Consumption Trend", "Bill Amounts"
        });
        chartTypeCb.setFont(UITheme.FONT_BODY);
        chartTypeCb.setBackground(new Color(20, 40, 80));
        chartTypeCb.setForeground(UITheme.TEXT_WHITE);
        chartTypeCb.setBorder(BorderFactory.createLineBorder(UITheme.ACCENT, 1));
        chartTypeCb.setPreferredSize(new Dimension(UITheme.dim(200), UITheme.dim(32)));

        JButton refreshBtn = UITheme.createAccentButton("\u21BB Refresh");
        refreshBtn.setPreferredSize(new Dimension(UITheme.dim(90), UITheme.dim(32)));
        JLabel chartLbl = new JLabel("Chart: ");
        chartLbl.setForeground(UITheme.TEXT_LIGHT); chartLbl.setFont(UITheme.FONT_LABEL);
        controls.add(chartLbl); controls.add(chartTypeCb); controls.add(refreshBtn);
        header.add(controls, BorderLayout.EAST);

        // Chart area
        chartArea = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawChart((Graphics2D) g);
            }
        };
        chartArea.setOpaque(false);

        JPanel chartCard = UITheme.createCard(null);
        chartCard.setLayout(new BorderLayout());
        chartCard.add(chartArea, BorderLayout.CENTER);

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setName("statsRow");
        statsRow.setPreferredSize(new Dimension(0, UITheme.dim(78)));

        // Data table
        tableModel = new DefaultTableModel(new String[]{"Period", "Consumption (kWh)", "vs Previous", "Trend"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        dataTable = new JTable(tableModel);
        UITheme.styleTable(dataTable);
        dataTable.setRowHeight(UITheme.dim(26));

        JPanel tableCard = UITheme.createCard("Data Table");
        tableCard.setLayout(new BorderLayout());
        tableCard.setPreferredSize(new Dimension(0, UITheme.dim(170)));
        tableCard.add(UITheme.createScrollPane(dataTable), BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setOpaque(false);
        centerPanel.add(statsRow,  BorderLayout.NORTH);
        centerPanel.add(chartCard, BorderLayout.CENTER);
        centerPanel.add(tableCard, BorderLayout.SOUTH);

        add(header,      BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        chartTypeCb.addActionListener(e -> {
            currentChartType = (String) chartTypeCb.getSelectedItem();
            loadData(currentChartType);
        });
        refreshBtn.addActionListener(e -> loadData(currentChartType));
    }

    private void loadData(String chartType) {
        labels.clear(); current.clear(); previous.clear();
        tableModel.setRowCount(0);
        switch (chartType) {
            case "Monthly Consumption": loadMonthlyConsumption(); break;
            case "Compare Months":      loadCompareMonths();      break;
            case "Consumption Trend":   loadConsumptionTrend();   break;
            case "Bill Amounts":        loadBillAmounts();        break;
        }
        updateStatsRow();
        chartArea.repaint();
    }

    private void updateStatsRow() {
        JPanel statsRow = findStatsRow(this);
        if (statsRow == null || current.isEmpty()) return;
        statsRow.removeAll();
        double avg  = current.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double max  = current.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double min  = current.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        Double lastObj = current.get(current.size() - 1);
        double last = lastObj != null ? lastObj : 0.0;
        Double prevObj = current.size() > 1 ? current.get(current.size() - 2) : lastObj;
        double prev = prevObj != null ? prevObj : last;
        double change = prev > 0 ? ((last - prev) / prev) * 100 : 0;
        statsRow.add(buildMiniStat("Average", String.format("%.1f kWh", avg), UITheme.ACCENT));
        statsRow.add(buildMiniStat("Peak",    String.format("%.1f kWh", max), UITheme.DANGER));
        statsRow.add(buildMiniStat("Lowest",  String.format("%.1f kWh", min), UITheme.SUCCESS));
        statsRow.add(buildMiniStat("vs Last", String.format("%+.1f%%", change), change <= 0 ? UITheme.SUCCESS : UITheme.DANGER));
        statsRow.revalidate(); statsRow.repaint();
    }

    private JPanel findStatsRow(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel p = (JPanel) comp;
                if ("statsRow".equals(p.getName())) return p;
                JPanel found = findStatsRow(p);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JPanel buildMiniStat(String label, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 30, 70, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false); card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel valLbl = new JLabel(value, SwingConstants.CENTER);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(16))); valLbl.setForeground(color);
        JLabel lblLbl = new JLabel(label, SwingConstants.CENTER);
        lblLbl.setFont(UITheme.FONT_SMALL); lblLbl.setForeground(UITheme.TEXT_MUTED);
        card.add(valLbl, BorderLayout.CENTER); card.add(lblLbl, BorderLayout.SOUTH);
        return card;
    }

    private void loadMonthlyConsumption() {
        String sql = "SELECT DATE_FORMAT(reading_date,'%b %Y'), SUM(consumption_kwh) " +
                     "FROM meter_readings WHERE customer_id=? " +
                     "GROUP BY YEAR(reading_date), MONTH(reading_date) " +
                     "ORDER BY YEAR(reading_date), MONTH(reading_date) DESC LIMIT 12";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            List<String> tl = new ArrayList<>(); List<Double> td = new ArrayList<>();
            while (rs.next()) { tl.add(0, rs.getString(1)); td.add(0, rs.getDouble(2)); }
            labels.addAll(tl); current.addAll(td);
        } catch (SQLException e) { /* ignore */ }
        populateTable();
    }

    private void loadCompareMonths() {
        String sql = "SELECT MONTH(reading_date), " +
                     "SUM(CASE WHEN YEAR(reading_date)=YEAR(NOW()) THEN consumption_kwh ELSE 0 END), " +
                     "SUM(CASE WHEN YEAR(reading_date)=YEAR(NOW())-1 THEN consumption_kwh ELSE 0 END) " +
                     "FROM meter_readings WHERE customer_id=? " +
                     "AND YEAR(reading_date) IN (YEAR(NOW()), YEAR(NOW())-1) " +
                     "GROUP BY MONTH(reading_date) ORDER BY MONTH(reading_date)";
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                labels.add(months[rs.getInt(1) - 1]);
                current.add(rs.getDouble(2)); previous.add(rs.getDouble(3));
            }
        } catch (SQLException e) { /* ignore */ }
        populateCompareTable();
    }

    private void loadConsumptionTrend() {
        String sql = "SELECT DATE_FORMAT(reading_date,'%d %b'), consumption_kwh " +
                     "FROM meter_readings WHERE customer_id=? ORDER BY reading_date DESC LIMIT 12";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            List<String> tl = new ArrayList<>(); List<Double> td = new ArrayList<>();
            while (rs.next()) { tl.add(0, rs.getString(1)); td.add(0, rs.getDouble(2)); }
            labels.addAll(tl); current.addAll(td);
        } catch (SQLException e) { /* ignore */ }
        populateTable();
    }

    private void loadBillAmounts() {
        String sql = "SELECT billing_month, total_amount FROM bills WHERE customer_id=? ORDER BY generated_at DESC LIMIT 12";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            List<String> tl = new ArrayList<>(); List<Double> td = new ArrayList<>();
            while (rs.next()) { tl.add(0, rs.getString(1)); td.add(0, rs.getDouble(2)); }
            labels.addAll(tl); current.addAll(td);
        } catch (SQLException e) { /* ignore */ }
        populateTable();
    }

    private void populateTable() {
        for (int i = 0; i < labels.size(); i++) {
            Double valObj = current.get(i); double val = valObj != null ? valObj : 0.0;
            Double prevObj = i > 0 ? current.get(i - 1) : valObj; double prev = prevObj != null ? prevObj : val;
            double diff = val - prev;
            String trend = diff > 0 ? "\u2191 +" + String.format("%.1f", diff)
                         : diff < 0 ? "\u2193 " + String.format("%.1f", diff) : "\u2192 0.0";
            String vs = i > 0 ? String.format("%+.1f%%", prev > 0 ? (diff / prev) * 100 : 0) : "—";
            tableModel.addRow(new Object[]{labels.get(i), String.format("%.2f", val), vs, trend});
        }
    }

    private void populateCompareTable() {
        for (int i = 0; i < labels.size(); i++) {
            Double cObj = current.get(i); double c = cObj != null ? cObj : 0.0;
            Double pObj = i < previous.size() ? previous.get(i) : null; double p = pObj != null ? pObj : 0.0;
            double diff = c - p;
            String vs = p > 0 ? String.format("%+.1f%%", (diff / p) * 100) : "—";
            String trend = diff > 0 ? "\u2191 +" + String.format("%.1f", diff)
                         : diff < 0 ? "\u2193 " + String.format("%.1f", diff) : "\u2192 0.0";
            tableModel.addRow(new Object[]{labels.get(i), String.format("%.2f", c), vs, trend});
        }
    }

    // ── Chart drawing ─────────────────────────────────────────────────────────

    private void drawChart(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = chartArea.getWidth(), h = chartArea.getHeight();
        int padL = 55, padR = 20, padT = 30, padB = 50;
        int chartW = w - padL - padR, chartH = h - padT - padB;

        if (current.isEmpty() || chartW <= 0 || chartH <= 0) {
            g2.setColor(UITheme.TEXT_MUTED); g2.setFont(UITheme.FONT_BODY);
            String msg = "No data available. Readings will appear here once recorded.";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
            return;
        }

        double maxVal = current.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        if (!previous.isEmpty())
            maxVal = Math.max(maxVal, previous.stream().mapToDouble(Double::doubleValue).max().orElse(0));
        maxVal = maxVal * 1.1; if (maxVal == 0) maxVal = 1;
        int n = labels.size();

        // Grid
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0));
        for (int i = 0; i <= 5; i++) {
            int y = padT + (int)(chartH * i / 5.0);
            g2.setColor(new Color(255, 255, 255, 20)); g2.drawLine(padL, y, padL + chartW, y);
            g2.setColor(UITheme.TEXT_MUTED); g2.setFont(UITheme.FONT_SMALL);
            String yLbl = "Bill Amounts".equals(currentChartType)
                ? String.format("$%.0f", maxVal * (5 - i) / 5)
                : String.format("%.0f", maxVal * (5 - i) / 5);
            g2.drawString(yLbl, 2, y + 4);
        }

        // Axes
        g2.setColor(UITheme.TEXT_MUTED); g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(padL, padT, padL, padT + chartH);
        g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

        switch (currentChartType) {
            case "Consumption Trend": drawLineChart(g2, padL, padT, chartW, chartH, maxVal, n); break;
            case "Compare Months":   drawGroupedBars(g2, padL, padT, chartW, chartH, maxVal, n); break;
            default:                 drawBars(g2, padL, padT, chartW, chartH, maxVal, n, UITheme.ACCENT); break;
        }

        // X-axis labels
        g2.setColor(UITheme.TEXT_MUTED); g2.setFont(UITheme.FONT_SMALL);
        int stepX = n > 0 ? chartW / n : chartW;
        for (int i = 0; i < n; i++) {
            String lbl = labels.get(i);
            FontMetrics fm = g2.getFontMetrics();
            int x = padL + stepX * i + stepX / 2 - fm.stringWidth(lbl) / 2;
            g2.drawString(lbl, x, padT + chartH + 16);
        }

        g2.setColor(UITheme.PRIMARY); g2.setFont(UITheme.FONT_LABEL);
        g2.drawString(currentChartType, padL + 4, padT - 8);
    }

    private void drawBars(Graphics2D g2, int padL, int padT, int chartW, int chartH,
                           double maxVal, int n, Color color) {
        int stepX = n > 0 ? chartW / n : chartW;
        int barW  = Math.max(4, stepX - UITheme.dim(10));
        for (int i = 0; i < n; i++) {
            Double valObj = current.get(i); double val = valObj != null ? valObj : 0.0;
            int barH = (int)(val / maxVal * chartH);
            int x = padL + stepX * i + (stepX - barW) / 2;
            int y = padT + chartH - barH;
            GradientPaint gp = new GradientPaint(x, y, color, x, padT + chartH,
                new Color(color.getRed()/3, color.getGreen()/3, color.getBlue()/3));
            g2.setPaint(gp); g2.fillRoundRect(x, y, barW, barH, 4, 4);
            g2.setColor(UITheme.TEXT_WHITE); g2.setFont(UITheme.FONT_SMALL);
            String valStr = "Bill Amounts".equals(currentChartType)
                ? String.format("$%.0f", val) : String.format("%.0f", val);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(valStr, x + (barW - fm.stringWidth(valStr)) / 2, Math.max(y - 3, padT + 10));
        }
    }

    private void drawGroupedBars(Graphics2D g2, int padL, int padT, int chartW, int chartH,
                                  double maxVal, int n) {
        int groupW = n > 0 ? chartW / n : chartW;
        int barW = Math.max(3, groupW / 3);
        for (int i = 0; i < n; i++) {
            Double cObj = current.get(i); double c = cObj != null ? cObj : 0.0;
            int barH1 = (int)(c / maxVal * chartH);
            int x1 = padL + groupW * i + (groupW - barW * 2 - 2) / 2;
            GradientPaint gp1 = new GradientPaint(x1, padT + chartH - barH1, UITheme.ACCENT, x1, padT + chartH, new Color(0, 60, 120));
            g2.setPaint(gp1); g2.fillRoundRect(x1, padT + chartH - barH1, barW, barH1, 3, 3);
            if (i < previous.size()) {
                Double pObj = previous.get(i); double p = pObj != null ? pObj : 0.0;
                int barH2 = (int)(p / maxVal * chartH);
                int x2 = x1 + barW + 2;
                GradientPaint gp2 = new GradientPaint(x2, padT + chartH - barH2, UITheme.PRIMARY, x2, padT + chartH, new Color(120, 60, 0));
                g2.setPaint(gp2); g2.fillRoundRect(x2, padT + chartH - barH2, barW, barH2, 3, 3);
            }
        }
        g2.setFont(UITheme.FONT_SMALL);
        g2.setColor(UITheme.ACCENT); g2.fillRect(padL + chartW - 120, padT + 4, 12, 10);
        g2.setColor(UITheme.TEXT_LIGHT); g2.drawString("This Year", padL + chartW - 104, padT + 13);
        g2.setColor(UITheme.PRIMARY); g2.fillRect(padL + chartW - 120, padT + 18, 12, 10);
        g2.setColor(UITheme.TEXT_LIGHT); g2.drawString("Last Year", padL + chartW - 104, padT + 27);
    }

    private void drawLineChart(Graphics2D g2, int padL, int padT, int chartW, int chartH,
                                double maxVal, int n) {
        if (n < 2) { drawBars(g2, padL, padT, chartW, chartH, maxVal, n, UITheme.ACCENT); return; }
        int stepX = chartW / (n - 1);

        Path2D area = new Path2D.Double();
        area.moveTo(padL, padT + chartH);
        for (int i = 0; i < n; i++) {
            Double valObj = current.get(i); double val = valObj != null ? valObj : 0.0;
            area.lineTo(padL + stepX * i, padT + chartH - (int)(val / maxVal * chartH));
        }
        area.lineTo(padL + stepX * (n - 1), padT + chartH); area.closePath();
        g2.setPaint(new GradientPaint(0, padT, new Color(0, 180, 255, 80), 0, padT + chartH, new Color(0, 80, 150, 10)));
        g2.fill(area);

        g2.setColor(UITheme.ACCENT); g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < n - 1; i++) {
            Double v1 = current.get(i); Double v2 = current.get(i + 1);
            double d1 = v1 != null ? v1 : 0.0, d2 = v2 != null ? v2 : 0.0;
            g2.drawLine(padL + stepX * i, padT + chartH - (int)(d1 / maxVal * chartH),
                        padL + stepX * (i + 1), padT + chartH - (int)(d2 / maxVal * chartH));
        }
        for (int i = 0; i < n; i++) {
            Double valObj = current.get(i); double val = valObj != null ? valObj : 0.0;
            int x = padL + stepX * i, y = padT + chartH - (int)(val / maxVal * chartH);
            g2.setColor(UITheme.ACCENT); g2.fillOval(x - 4, y - 4, 8, 8);
            g2.setColor(UITheme.TEXT_WHITE); g2.setFont(UITheme.FONT_SMALL);
            g2.drawString(String.format("%.0f", val), x - 10, y - 8);
        }
    }
}
