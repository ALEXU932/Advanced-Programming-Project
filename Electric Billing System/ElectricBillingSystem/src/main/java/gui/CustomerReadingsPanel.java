package gui;

import db.DatabaseManager;
import models.Customer;

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
    private List<Double> chartData  = new ArrayList<>();
    private List<String> chartLabels = new ArrayList<>();

    public CustomerReadingsPanel(Customer customer) {
        this.customer = customer;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadData();
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("\u25A6 My Meter Readings");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton submitBtn  = UITheme.createPrimaryButton("Submit Reading");
        JButton exportBtn  = UITheme.createAccentButton("Export to Excel");
        JButton refreshBtn = UITheme.createAccentButton("↻ Refresh");
        actions.add(submitBtn); actions.add(exportBtn); actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Center: table + chart ─────────────────────────────────────────────
        JPanel center = new JPanel(new GridLayout(1, 2, 16, 0));
        center.setOpaque(false);

        // Table card
        String[] cols = {"Date", "Prev Reading", "Curr Reading", "Consumption (kWh)", "Recorded By"};
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
        JPanel chartCard = UITheme.createCard("Monthly Consumption Trend");
        chartCard.setLayout(new BorderLayout());
        chartCard.add(chartPanel, BorderLayout.CENTER);

        center.add(tableCard);
        center.add(chartCard);
        add(center, BorderLayout.CENTER);

        // Button actions
        submitBtn.addActionListener(e -> showSubmitReadingDialog());
        exportBtn.addActionListener(e -> utils.ExcelExporter.export(this, table, "Meter Readings"));
        refreshBtn.addActionListener(e -> loadData());
    }

    private void loadData() {
        tableModel.setRowCount(0);
        chartData.clear();
        chartLabels.clear();
        String sql = "SELECT mr.reading_date, mr.previous_reading, mr.current_reading, " +
                     "mr.consumption_kwh, COALESCE(a.role,'Customer') as recorded_by " +
                     "FROM meter_readings mr " +
                     "LEFT JOIN admins a ON a.admin_id = mr.recorded_by " +
                     "WHERE mr.customer_id=? ORDER BY mr.reading_date DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            List<Double> tempData   = new ArrayList<>();
            List<String> tempLabels = new ArrayList<>();
            while (rs.next()) {
                String date = rs.getString(1);
                tableModel.addRow(new Object[]{
                    date,
                    String.format("%.2f", rs.getDouble(2)),
                    String.format("%.2f", rs.getDouble(3)),
                    String.format("%.2f", rs.getDouble(4)),
                    rs.getString(5)
                });
                tempData.add(0, rs.getDouble(4));
                // Short month label
                String label = date != null && date.length() >= 7 ? date.substring(0, 7) : date;
                tempLabels.add(0, label);
            }
            chartData   = tempData;
            chartLabels = tempLabels;
            chartPanel.repaint();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ── Submit self-reading dialog ────────────────────────────────────────────

    private void showSubmitReadingDialog() {
        // Get last reading value
        double lastReading = 0;
        String lastDate    = "N/A";
        String sql = "SELECT current_reading, reading_date FROM meter_readings " +
                     "WHERE customer_id=? ORDER BY reading_date DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                lastReading = rs.getDouble(1);
                lastDate    = rs.getString(2);
            }
        } catch (SQLException ignored) {}

        final double prevReading = lastReading;

        FormDialog dialog = new FormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Submit Meter Reading",
            "Enter your current meter reading",
            500, 380);

        // Info label
        JLabel infoLbl = new JLabel(String.format(
            "<html><div style='color:#aac;'>Last reading: <b style='color:#00b4ff;'>%.2f</b> on %s</div></html>",
            prevReading, lastDate));
        infoLbl.setFont(UITheme.FONT_BODY);

        JTextField currentF = FormDialog.makeField("");
        JTextField dateF    = FormDialog.makeField(
            new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
        JTextField notesF   = FormDialog.makeField("");

        // Live consumption preview
        JLabel previewLbl = new JLabel("Consumption: — kWh", SwingConstants.CENTER);
        previewLbl.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(14)));
        previewLbl.setForeground(UITheme.ACCENT);

        currentF.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                try {
                    double curr = Double.parseDouble(currentF.getText().trim());
                    double consumption = curr - prevReading;
                    if (consumption < 0) {
                        previewLbl.setText("\u26A0 Reading must be >= previous (" + String.format("%.2f", prevReading) + ")");
                        previewLbl.setForeground(UITheme.DANGER);
                    } else {
                        previewLbl.setText(String.format("Consumption: %.2f kWh", consumption));
                        previewLbl.setForeground(UITheme.ACCENT);
                    }
                } catch (NumberFormatException e) {
                    previewLbl.setText("Consumption: — kWh");
                    previewLbl.setForeground(UITheme.TEXT_MUTED);
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        // Add info label manually to body
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 8, 6);
        dialog.body.add(infoLbl, gbc);

        dialog.addField("Current Meter Reading *", currentF);
        dialog.addField("Reading Date *", dateF);

        // Preview label
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.gridx = 0; gbc2.gridy = 6; gbc2.gridwidth = 2;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.insets = new Insets(4, 6, 4, 6);
        dialog.body.add(previewLbl, gbc2);

        dialog.addField("Notes (optional)", notesF);
        dialog.addStatus();
        dialog.addCancelButton();
        JButton submitBtn = dialog.addSaveButton("  Submit Reading  ");

        submitBtn.addActionListener(e -> {
            String currStr = currentF.getText().trim();
            String dateStr = dateF.getText().trim();
            if (currStr.isEmpty()) { dialog.setStatus("Current reading is required.", true); return; }
            if (dateStr.isEmpty()) { dialog.setStatus("Date is required.", true); return; }

            double currVal;
            try {
                currVal = Double.parseDouble(currStr);
            } catch (NumberFormatException ex) {
                dialog.setStatus("Reading must be a valid number.", true); return;
            }

            if (currVal < prevReading) {
                dialog.setStatus(String.format("Reading must be ≥ previous reading (%.2f).", prevReading), true);
                return;
            }

            double consumption = currVal - prevReading;

            // Get meter_id
            int meterId = 0;
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT meter_id FROM meters WHERE customer_id=? LIMIT 1")) {
                ps.setInt(1, customer.getCustomerId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) meterId = rs.getInt(1);
            } catch (SQLException ignored) {}

            String insertSql = "INSERT INTO meter_readings " +
                "(meter_id, customer_id, reading_date, units, consumption_kwh, previous_reading, current_reading) " +
                "VALUES (?,?,?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {
                if (meterId > 0) ps.setInt(1, meterId); else ps.setNull(1, Types.INTEGER);
                ps.setInt(2, customer.getCustomerId());
                ps.setString(3, dateStr);
                ps.setDouble(4, consumption);
                ps.setDouble(5, consumption);
                ps.setDouble(6, prevReading);
                ps.setDouble(7, currVal);
                ps.executeUpdate();

                utils.AuditLogger.log(customer.getUserId(), customer.getName(),
                    utils.AuditLogger.Action.ADD_READING,
                    "Customer submitted reading: current=" + currVal + " consumption=" + consumption);

                // Add notification
                addNotification(String.format("Meter reading submitted: %.2f kWh consumption recorded.", consumption), "READING_MISSING");

                dialog.dispose();
                JOptionPane.showMessageDialog(this,
                    String.format("Reading submitted!\n\nCurrent: %.2f\nPrevious: %.2f\nConsumption: %.2f kWh",
                        currVal, prevReading, consumption),
                    "Reading Submitted", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            } catch (SQLException ex) {
                dialog.setStatus("Error: " + ex.getMessage(), true);
            }
        });

        dialog.setVisible(true);
    }

    // ── Chart drawing ─────────────────────────────────────────────────────────

    private void drawChart(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = chartPanel.getWidth(), h = chartPanel.getHeight();
        int padL = 55, padR = 20, padT = 30, padB = 50;
        int chartW = w - padL - padR;
        int chartH = h - padT - padB;

        if (chartData.size() < 1 || chartW <= 0 || chartH <= 0) {
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.FONT_BODY);
            FontMetrics fm = g2.getFontMetrics();
            String msg = "No reading data available";
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
            return;
        }

        double maxVal = chartData.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double minVal = 0;
        double range  = maxVal - minVal == 0 ? 1 : maxVal - minVal;
        int n = chartData.size();

        // Grid lines
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0));
        for (int i = 0; i <= 5; i++) {
            int y = padT + (int)(chartH * i / 5.0);
            g2.setColor(new Color(255, 255, 255, 20));
            g2.drawLine(padL, y, padL + chartW, y);
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.FONT_SMALL);
            String yLabel = String.format("%.0f", maxVal - range * i / 5.0);
            g2.drawString(yLabel, padL - g2.getFontMetrics().stringWidth(yLabel) - 4, y + 4);
        }

        // Axes
        g2.setColor(UITheme.TEXT_MUTED);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(padL, padT, padL, padT + chartH);
        g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

        // Bars
        int barW = Math.max(4, chartW / (n + 1) - 6);
        int stepX = chartW / (n + 1);

        for (int i = 0; i < n; i++) {
            int x    = padL + stepX * (i + 1) - barW / 2;
            int barH = (int)((chartData.get(i) - minVal) / range * chartH);
            int y    = padT + chartH - barH;

            // Gradient bar
            GradientPaint gp = new GradientPaint(x, y, UITheme.ACCENT, x, padT + chartH, new Color(0, 60, 120));
            g2.setPaint(gp);
            g2.fillRoundRect(x, y, barW, barH, 4, 4);

            // Value label on top
            g2.setColor(UITheme.TEXT_WHITE);
            g2.setFont(UITheme.FONT_SMALL);
            String val = String.format("%.0f", chartData.get(i));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(val, x + (barW - fm.stringWidth(val)) / 2, y - 3);

            // Month label below axis
            if (i < chartLabels.size()) {
                g2.setColor(UITheme.TEXT_MUTED);
                String lbl = chartLabels.get(i);
                if (lbl != null && lbl.length() > 7) lbl = lbl.substring(2); // "2024-03" → "24-03"
                int lx = x + (barW - fm.stringWidth(lbl != null ? lbl : "")) / 2;
                g2.drawString(lbl != null ? lbl : "", lx, padT + chartH + 16);
            }
        }

        // Y-axis label
        g2.setColor(UITheme.TEXT_MUTED);
        g2.setFont(UITheme.FONT_SMALL);
        Graphics2D g2r = (Graphics2D) g2.create();
        g2r.rotate(-Math.PI / 2, 12, padT + chartH / 2);
        g2r.drawString("kWh", 12 - 12, padT + chartH / 2);
        g2r.dispose();
    }

    private void addNotification(String message, String type) {
        String sql = "INSERT INTO customer_notifications (customer_id, message, type) VALUES (?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ps.setString(2, message);
            ps.setString(3, type);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }
}
