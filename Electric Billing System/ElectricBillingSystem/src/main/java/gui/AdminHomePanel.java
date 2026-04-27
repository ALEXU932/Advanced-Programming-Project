package gui;

import db.DatabaseManager;
import models.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AdminHomePanel extends JPanel {

    private final User user;
    private JLabel timeLabel;
    private JLabel dbStatusLabel;

    // KPI labels — top row
    private JLabel kpiCustomers, kpiMeters, kpiRevenue, kpiCollectedToday;
    // KPI labels — bottom row
    private JLabel kpiPending, kpiOverdue, kpiPaidBills, kpiAnomalies;

    // Tables
    private DefaultTableModel billsModel, anomalyModel;

    // Chart data
    private double[] monthlyRevenue = new double[6];
    private String[] monthLabels    = new String[6];
    private int paidCount, pendingCount;

    public AdminHomePanel(User user) {
        this.user = user;
        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        buildUI();
        loadAllData();
        startClock();
        startAutoRefresh();
    }

    // ── Build UI ──────────────────────────────────────────────────────────────

    private void buildUI() {
        add(buildTopBar(),     BorderLayout.NORTH);
        add(buildCenter(),     BorderLayout.CENTER);
    }

    // ── Top bar: greeting + date/time + db status ─────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 4));
        bar.setOpaque(false);

        // Left: greeting
        String hour = new SimpleDateFormat("HH").format(new Date());
        int h = Integer.parseInt(hour);
        String greeting = h < 12 ? "Good Morning" : h < 17 ? "Good Afternoon" : "Good Evening";
        JLabel welcome = new JLabel(greeting + ", " + user.getUsername() + "  \u2600");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcome.setForeground(UITheme.TEXT_WHITE);

        JLabel sub = new JLabel("Here's your system overview for today");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(UITheme.TEXT_MUTED);

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);
        left.add(welcome); left.add(sub);

        // Right: time + db status + export btn
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        dbStatusLabel = new JLabel("DB: Connected");
        dbStatusLabel.setFont(UITheme.FONT_SMALL);
        dbStatusLabel.setForeground(UITheme.SUCCESS);

        timeLabel = new JLabel();
        timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        timeLabel.setForeground(UITheme.TEXT_LIGHT);

        JButton exportBtn = UITheme.createAccentButton("Export Dashboard PDF");
        exportBtn.setPreferredSize(new Dimension(170, 32));
        exportBtn.addActionListener(e -> exportDashboard());

        right.add(dbStatusLabel);
        right.add(new JSeparator(SwingConstants.VERTICAL) {{ setPreferredSize(new Dimension(1, 20)); setForeground(new Color(60,80,120)); }});
        right.add(timeLabel);
        right.add(exportBtn);

        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Center: KPIs + Charts + Tables + Quick Actions ────────────────────────

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        // Row 1: 8 KPI cards
        center.add(buildKpiRows(), BorderLayout.NORTH);

        // Row 2: Charts + Quick Actions
        JPanel midRow = new JPanel(new GridLayout(1, 3, 12, 0));
        midRow.setOpaque(false);
        // Height adapts to available space
        midRow.add(buildRevenueChart());
        midRow.add(buildBillsDonut());
        midRow.add(buildQuickActions());

        // Row 3: Recent Bills + Recent Anomalies
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 12, 0));
        bottomRow.setOpaque(false);
        bottomRow.add(buildRecentBillsCard());
        bottomRow.add(buildRecentAnomaliesCard());

        JPanel rows = new JPanel(new BorderLayout(0, 12));
        rows.setOpaque(false);
        rows.add(midRow,    BorderLayout.NORTH);
        rows.add(bottomRow, BorderLayout.CENTER);

        center.add(rows, BorderLayout.CENTER);
        return center;
    }

    // ── KPI rows ──────────────────────────────────────────────────────────────

    private JPanel buildKpiRows() {
        JPanel wrapper = new JPanel(new GridLayout(2, 4, 10, 10));
        wrapper.setOpaque(false);
        // No fixed height — let layout manager decide based on window size

        kpiCustomers     = kpiValue("0");
        kpiMeters        = kpiValue("0");
        kpiRevenue       = kpiValue("$0");
        kpiCollectedToday= kpiValue("$0");
        kpiPending       = kpiValue("0");
        kpiOverdue       = kpiValue("$0");
        kpiPaidBills     = kpiValue("0");
        kpiAnomalies     = kpiValue("0");

        wrapper.add(kpiCard("\u25A3 Total Customers",    kpiCustomers,      UITheme.ACCENT));
        wrapper.add(kpiCard("\u26A1 Active Meters",      kpiMeters,         UITheme.PRIMARY));
        wrapper.add(kpiCard("\u20BF Monthly Revenue",    kpiRevenue,        UITheme.SUCCESS));
        wrapper.add(kpiCard("\u2714 Collected Today",    kpiCollectedToday, UITheme.SUCCESS));
        wrapper.add(kpiCard("\u23F3 Pending Bills",      kpiPending,        UITheme.WARNING));
        wrapper.add(kpiCard("\u26A0 Overdue Amount",     kpiOverdue,        UITheme.DANGER));
        wrapper.add(kpiCard("\u2705 Paid Bills",         kpiPaidBills,      new Color(50,200,100)));
        wrapper.add(kpiCard("\u2605 Anomalies",          kpiAnomalies,      UITheme.DANGER));

        return wrapper;
    }

    private JPanel kpiCard(String title, JLabel valueLbl, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 30, 70, 215));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                // Left accent bar
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 10));

        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLbl.setForeground(color);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 10));
        titleLbl.setForeground(UITheme.TEXT_MUTED);

        card.add(valueLbl,  BorderLayout.CENTER);
        card.add(titleLbl,  BorderLayout.SOUTH);
        return card;
    }

    private JLabel kpiValue(String v) {
        JLabel l = new JLabel(v, SwingConstants.LEFT);
        l.setFont(new Font("Segoe UI", Font.BOLD, 22));
        return l;
    }

    // ── Revenue bar chart ─────────────────────────────────────────────────────

    private JPanel buildRevenueChart() {
        JPanel card = UITheme.createCard("Monthly Revenue (Last 6 Months)");
        JPanel chart = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBarChart((Graphics2D) g);
            }
        };
        chart.setOpaque(false);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private void drawBarChart(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() / 3 - 24, h = 140;
        if (w <= 0) return;
        int pad = 30, chartW = w - pad * 2, chartH = h - 30;
        double max = 1;
        for (double v : monthlyRevenue) if (v > max) max = v;

        int n = monthlyRevenue.length;
        int barW = Math.max(4, chartW / n - 6);

        // Grid lines
        g2.setColor(new Color(255,255,255,20));
        g2.setStroke(new BasicStroke(1f));
        for (int i = 1; i <= 4; i++) {
            int y = pad + (int)(chartH * (1 - i / 4.0));
            g2.drawLine(pad, y, pad + chartW, y);
        }

        // Bars
        for (int i = 0; i < n; i++) {
            int bh = (int)(monthlyRevenue[i] / max * chartH);
            int x  = pad + i * (barW + 6);
            int y  = pad + chartH - bh;
            GradientPaint gp = new GradientPaint(x, y, UITheme.SUCCESS, x, pad + chartH, new Color(0, 80, 40));
            g2.setPaint(gp);
            g2.fillRoundRect(x, y, barW, bh, 4, 4);
            // Label
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            if (monthLabels[i] != null)
                g2.drawString(monthLabels[i], x, pad + chartH + 12);
            if (monthlyRevenue[i] > 0) {
                g2.setColor(UITheme.SUCCESS);
                g2.drawString("$" + (int)monthlyRevenue[i], x, y - 3);
            }
        }
    }

    // ── Bills donut chart ─────────────────────────────────────────────────────

    private JPanel buildBillsDonut() {
        JPanel card = UITheme.createCard("Bills Status");
        JPanel chart = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawDonut((Graphics2D) g);
            }
        };
        chart.setOpaque(false);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private void drawDonut(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() / 3 - 24, h = 140;
        if (w <= 0 || (paidCount + pendingCount) == 0) {
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.FONT_SMALL);
            g2.drawString("No bills yet", 20, h / 2);
            return;
        }
        int total = paidCount + pendingCount;
        int cx = w / 2, cy = h / 2 - 10, r = Math.min(cx, cy) - 10;
        int paidAngle = (int)(360.0 * paidCount / total);

        // Paid arc
        g2.setColor(UITheme.SUCCESS);
        g2.fillArc(cx - r, cy - r, r*2, r*2, 90, -paidAngle);
        // Pending arc
        g2.setColor(UITheme.WARNING);
        g2.fillArc(cx - r, cy - r, r*2, r*2, 90 - paidAngle, -(360 - paidAngle));
        // Hole
        g2.setColor(new Color(15, 30, 70));
        int hole = r / 2;
        g2.fillOval(cx - hole, cy - hole, hole*2, hole*2);
        // Center text
        g2.setColor(UITheme.TEXT_WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        String ct = total + " Bills";
        g2.drawString(ct, cx - g2.getFontMetrics().stringWidth(ct)/2, cy + 5);
        // Legend
        g2.setColor(UITheme.SUCCESS);
        g2.fillRect(10, h - 28, 10, 10);
        g2.setColor(UITheme.TEXT_LIGHT);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.drawString("Paid: " + paidCount, 24, h - 19);
        g2.setColor(UITheme.WARNING);
        g2.fillRect(w/2, h - 28, 10, 10);
        g2.setColor(UITheme.TEXT_LIGHT);
        g2.drawString("Pending: " + pendingCount, w/2 + 14, h - 19);
    }

    // ── Quick Actions ─────────────────────────────────────────────────────────

    private JPanel buildQuickActions() {
        JPanel card = UITheme.createCard("Quick Actions");
        card.setLayout(new BorderLayout(0, 10));

        JPanel grid = new JPanel(new GridLayout(3, 2, 10, 10));
        grid.setOpaque(false);

        // [label, card key, icon char, hex color]
        Object[][] actions = {
            {"Add Customer",    "CUSTOMERS", "\u25A3", "#00B4FF"},
            {"New Reading",     "READINGS",  "\u25A6", "#FFA500"},
            {"Generate Bill",   "BILLING",   "\u20BF", "#32C864"},
            {"Record Payment",  "PAYMENTS",  "\u25A4", "#00B4FF"},
            {"Run AI Scan",     "AI",        "\u2605", "#FF6464"},
            {"View Anomalies",  "ANOMALIES", "\u26A0", "#FF8C00"}
        };

        for (Object[] a : actions) {
            grid.add(buildActionTile((String)a[0], (String)a[1], (String)a[2], (String)a[3]));
        }

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildActionTile(String label, String cardKey, String icon, String hexColor) {
        Color color = Color.decode(hexColor);

        JPanel tile = new JPanel(new BorderLayout(0, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Background
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 18));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Border
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 90));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                // Bottom accent bar
                g2.setColor(color);
                g2.fillRoundRect(0, getHeight()-3, getWidth(), 3, 3, 3);
                g2.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
        tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Icon
        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 22));
        iconLbl.setForeground(color);

        // Label
        JLabel textLbl = new JLabel(label, SwingConstants.CENTER);
        textLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        textLbl.setForeground(UITheme.TEXT_WHITE);

        tile.add(iconLbl, BorderLayout.CENTER);
        tile.add(textLbl, BorderLayout.SOUTH);

        // Hover effect
        tile.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                tile.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(color, 1),
                    BorderFactory.createEmptyBorder(11, 13, 13, 13)));
                textLbl.setForeground(color);
                tile.repaint();
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                tile.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
                textLbl.setForeground(UITheme.TEXT_WHITE);
                tile.repaint();
            }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                navigateTo(cardKey);
            }
        });

        return tile;
    }

    private void navigateTo(String card) {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof AdminDashboard) {
                ((AdminDashboard) parent).navigateTo(card);
                return;
            }
            parent = parent.getParent();
        }
    }

    // ── Recent Bills ──────────────────────────────────────────────────────────

    private JPanel buildRecentBillsCard() {
        JPanel card = UITheme.createCard("Recent Bills");
        card.setLayout(new BorderLayout(0, 6));

        // Filter row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All","PENDING","PAID","OVERDUE"});
        statusFilter.setFont(UITheme.FONT_SMALL);
        statusFilter.setBackground(new Color(20,40,80));
        statusFilter.setForeground(UITheme.TEXT_WHITE);
        statusFilter.setPreferredSize(new Dimension(110, 26));
        JLabel filterLbl = new JLabel("Filter:");
        filterLbl.setFont(UITheme.FONT_SMALL); filterLbl.setForeground(UITheme.TEXT_MUTED);
        filterRow.add(filterLbl); filterRow.add(statusFilter);
        card.add(filterRow, BorderLayout.NORTH);

        String[] cols = {"Customer","Month","Amount","Status"};
        billsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(billsModel);
        UITheme.styleTable(table);
        table.setRowHeight(26);

        // Color-code status
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                setHorizontalAlignment(SwingConstants.CENTER); setOpaque(true);
                String s = v != null ? v.toString() : "";
                setForeground("PAID".equals(s) ? UITheme.SUCCESS : "OVERDUE".equals(s) ? UITheme.DANGER : UITheme.WARNING);
                setBackground(sel ? UITheme.PRIMARY : UITheme.TABLE_ROW1);
                return this;
            }
        });

        card.add(UITheme.createScrollPane(table), BorderLayout.CENTER);

        statusFilter.addActionListener(e -> {
            String sel = (String) statusFilter.getSelectedItem();
            loadRecentBills("All".equals(sel) ? null : sel);
        });
        return card;
    }

    // ── Recent Anomalies ──────────────────────────────────────────────────────

    private JPanel buildRecentAnomaliesCard() {
        JPanel card = UITheme.createCard("Recent Anomalies");
        card.setLayout(new BorderLayout(0, 6));

        String[] cols = {"Customer","Detected","Severity","Resolved"};
        anomalyModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(anomalyModel);
        UITheme.styleTable(table);
        table.setRowHeight(26);

        // Color-code severity
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                setHorizontalAlignment(SwingConstants.CENTER); setOpaque(true);
                String s = v != null ? v.toString() : "";
                setForeground("HIGH".equals(s) ? UITheme.DANGER : "MEDIUM".equals(s) ? UITheme.WARNING : UITheme.ACCENT);
                setBackground(sel ? UITheme.PRIMARY : UITheme.TABLE_ROW1);
                return this;
            }
        });

        // Empty state panel
        JPanel emptyState = new JPanel(new GridBagLayout());
        emptyState.setOpaque(false);
        JLabel emptyLbl = new JLabel("<html><center>No anomalies detected this month<br><font color='#32C864'>System is healthy</font></center></html>", SwingConstants.CENTER);
        emptyLbl.setFont(UITheme.FONT_BODY);
        emptyLbl.setForeground(UITheme.TEXT_MUTED);
        emptyState.add(emptyLbl);

        JPanel tableWrapper = new JPanel(new CardLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.add(UITheme.createScrollPane(table), "TABLE");
        tableWrapper.add(emptyState, "EMPTY");

        card.add(tableWrapper, BorderLayout.CENTER);

        // Store reference to switch between empty/table
        card.putClientProperty("tableWrapper", tableWrapper);
        card.putClientProperty("anomalyTable", table);

        return card;
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadAllData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() {
                loadKpis();
                loadChartData();
                loadRecentBills(null);
                loadRecentAnomalies();
                return null;
            }
            @Override protected void done() {
                repaint();
            }
        };
        worker.execute();
    }

    private void loadKpis() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement()) {

            set(kpiCustomers,      query(st, "SELECT COUNT(*) FROM customers"));
            set(kpiMeters,         query(st, "SELECT COUNT(*) FROM meters WHERE status='ACTIVE'"));
            set(kpiRevenue,        "$" + queryDouble(st,
                "SELECT COALESCE(SUM(total_amount),0) FROM bills WHERE DATE_FORMAT(generated_at,'%Y-%m')=DATE_FORMAT(NOW(),'%Y-%m')"));
            set(kpiCollectedToday, "$" + queryDouble(st,
                "SELECT COALESCE(SUM(amount),0) FROM payments WHERE DATE(payment_date)=CURDATE()"));
            set(kpiPending,        query(st, "SELECT COUNT(*) FROM bills WHERE status='PENDING'"));
            set(kpiOverdue,        "$" + queryDouble(st,
                "SELECT COALESCE(SUM(total_amount),0) FROM bills WHERE status='OVERDUE'"));
            set(kpiPaidBills,      query(st, "SELECT COUNT(*) FROM bills WHERE status='PAID'"));
            set(kpiAnomalies,      query(st, "SELECT COUNT(*) FROM anomalies WHERE is_resolved=FALSE"));

            // Bill counts for donut
            ResultSet r1 = st.executeQuery("SELECT COUNT(*) FROM bills WHERE status='PAID'");
            paidCount = r1.next() ? r1.getInt(1) : 0;
            ResultSet r2 = st.executeQuery("SELECT COUNT(*) FROM bills WHERE status='PENDING'");
            pendingCount = r2.next() ? r2.getInt(1) : 0;

        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadChartData() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(
                "SELECT DATE_FORMAT(generated_at,'%b') as mon, " +
                "COALESCE(SUM(total_amount),0) as rev " +
                "FROM bills WHERE generated_at >= DATE_SUB(NOW(), INTERVAL 6 MONTH) " +
                "GROUP BY DATE_FORMAT(generated_at,'%Y-%m') " +
                "ORDER BY DATE_FORMAT(generated_at,'%Y-%m') ASC LIMIT 6");
            int i = 0;
            while (rs.next() && i < 6) {
                monthLabels[i]    = rs.getString(1);
                monthlyRevenue[i] = rs.getDouble(2);
                i++;
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadRecentBills(String statusFilter) {
        if (billsModel == null) return;
        SwingUtilities.invokeLater(() -> {
            billsModel.setRowCount(0);
            String sql = "SELECT c.name, b.billing_month, b.total_amount, b.status " +
                         "FROM bills b JOIN customers c ON b.customer_id=c.customer_id";
            if (statusFilter != null) sql += " WHERE b.status='" + statusFilter + "'";
            sql += " ORDER BY b.generated_at DESC LIMIT 10";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) billsModel.addRow(new Object[]{
                    rs.getString(1), rs.getString(2),
                    String.format("$%.2f", rs.getDouble(3)), rs.getString(4)
                });
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    private void loadRecentAnomalies() {
        if (anomalyModel == null) return;
        SwingUtilities.invokeLater(() -> {
            anomalyModel.setRowCount(0);
            String sql = "SELECT c.name, DATE(a.detected_at), a.severity, a.is_resolved " +
                         "FROM anomalies a JOIN customers c ON a.customer_id=c.customer_id " +
                         "ORDER BY a.detected_at DESC LIMIT 8";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) anomalyModel.addRow(new Object[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getBoolean(4) ? "Yes" : "No"
                });
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    // ── Clock ─────────────────────────────────────────────────────────────────

    private void startClock() {
        Timer clock = new Timer(1000, e -> {
            timeLabel.setText(new SimpleDateFormat("EEE, dd MMM yyyy  HH:mm:ss").format(new Date()));
        });
        clock.start();
        clock.getActionListeners()[0].actionPerformed(null);
    }

    // ── Auto-refresh every 30 seconds ─────────────────────────────────────────

    private void startAutoRefresh() {
        Timer refresh = new Timer(30000, e -> loadAllData());
        refresh.start();
    }

    // ── Export dashboard as simple text summary ───────────────────────────────

    private void exportDashboard() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("Dashboard_" +
            new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(fc.getSelectedFile())) {
            pw.println("=== ADMIN DASHBOARD SUMMARY ===");
            pw.println("Generated: " + new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date()));
            pw.println("Admin: " + user.getUsername());
            pw.println();
            pw.println("--- KEY METRICS ---");
            pw.println("Total Customers  : " + kpiCustomers.getText());
            pw.println("Active Meters    : " + kpiMeters.getText());
            pw.println("Monthly Revenue  : " + kpiRevenue.getText());
            pw.println("Collected Today  : " + kpiCollectedToday.getText());
            pw.println("Pending Bills    : " + kpiPending.getText());
            pw.println("Overdue Amount   : " + kpiOverdue.getText());
            pw.println("Paid Bills       : " + kpiPaidBills.getText());
            pw.println("Open Anomalies   : " + kpiAnomalies.getText());
            JOptionPane.showMessageDialog(this, "Dashboard exported successfully!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String query(Statement st, String sql) {
        try { ResultSet rs = st.executeQuery(sql); return rs.next() ? String.valueOf(rs.getInt(1)) : "0"; }
        catch (SQLException e) { return "0"; }
    }

    private String queryDouble(Statement st, String sql) {
        try { ResultSet rs = st.executeQuery(sql); return rs.next() ? String.format("%.2f", rs.getDouble(1)) : "0.00"; }
        catch (SQLException e) { return "0.00"; }
    }

    private void set(JLabel lbl, String val) {
        SwingUtilities.invokeLater(() -> lbl.setText(val));
    }
}
