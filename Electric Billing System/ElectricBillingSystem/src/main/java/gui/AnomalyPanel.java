package gui;

import ai.AnomalyDetector;
import db.DatabaseManager;
import models.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnomalyPanel extends JPanel {

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel totalLbl, highLbl, unresolvedLbl;

    public AnomalyPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadData();
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("Anomaly Detection");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton scanBtn    = UITheme.createPrimaryButton("Scan All Customers");
        JButton resolveBtn = UITheme.createAccentButton("Mark Resolved");
        JButton deleteBtn  = UITheme.createDangerButton("Delete");
        JButton refreshBtn = UITheme.createAccentButton("Refresh");
        for (JButton b : new JButton[]{scanBtn, resolveBtn, deleteBtn, refreshBtn})
            b.setPreferredSize(new Dimension(b.getPreferredSize().width, 36));
        actions.add(scanBtn); actions.add(resolveBtn);
        actions.add(deleteBtn); actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);

        // ── Stats row ─────────────────────────────────────────────────────────
        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 80));

        totalLbl      = statValue("0");
        highLbl       = statValue("0");
        unresolvedLbl = statValue("0");
        stats.add(buildStatCard("Total Anomalies",    totalLbl,      UITheme.WARNING));
        stats.add(buildStatCard("High Severity",      highLbl,       UITheme.DANGER));
        stats.add(buildStatCard("Unresolved",         unresolvedLbl, UITheme.PRIMARY));

        // ── Table ─────────────────────────────────────────────────────────────
        String[] cols = {"ID", "Customer", "Detected At", "Severity", "Z-Score", "Description", "Resolved"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.setRowHeight(32);

        int[] widths = {45, 150, 130, 80, 70, 350, 80};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Color-code severity
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER); setOpaque(true);
                String s = v != null ? v.toString() : "";
                switch (s) {
                    case "HIGH":   setForeground(UITheme.DANGER);  break;
                    case "MEDIUM": setForeground(UITheme.WARNING); break;
                    default:       setForeground(UITheme.ACCENT);
                }
                setBackground(sel ? UITheme.PRIMARY : UITheme.TABLE_ROW1);
                return this;
            }
        });

        // Color-code Resolved column
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER); setOpaque(true);
                boolean resolved = "Yes".equals(v != null ? v.toString() : "");
                setForeground(resolved ? UITheme.SUCCESS : UITheme.DANGER);
                setBackground(sel ? UITheme.PRIMARY : UITheme.TABLE_ROW1);
                return this;
            }
        });

        JPanel tableCard = UITheme.createCard(null);
        tableCard.setLayout(new BorderLayout());
        tableCard.add(UITheme.createScrollPane(table), BorderLayout.CENTER);

        // ── How it works info card ────────────────────────────────────────────
        JPanel infoCard = UITheme.createCard("How Anomaly Detection Works");
        infoCard.setPreferredSize(new Dimension(0, 110));
        JTextArea info = new JTextArea(
            "The system uses Z-Score analysis to detect unusual electricity consumption patterns.\n" +
            "A reading is flagged as anomalous when it deviates more than 2 standard deviations from the customer's historical average.\n" +
            "Anomalies are detected automatically when meter readings are added, or you can run a full scan using 'Scan All Customers'.\n" +
            "Severity: LOW (z>2.0)  |  MEDIUM (z>2.5)  |  HIGH (z>3.5)");
        info.setFont(UITheme.FONT_SMALL);
        info.setForeground(UITheme.TEXT_LIGHT);
        info.setBackground(new Color(0,0,0,0));
        info.setOpaque(false);
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        infoCard.add(info, BorderLayout.CENTER);

        // ── Assemble ──────────────────────────────────────────────────────────
        JPanel topSection = new JPanel(new BorderLayout(0, 12));
        topSection.setOpaque(false);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(stats,  BorderLayout.SOUTH);

        add(topSection,  BorderLayout.NORTH);
        add(tableCard,   BorderLayout.CENTER);
        add(infoCard,    BorderLayout.SOUTH);

        // ── Listeners ─────────────────────────────────────────────────────────
        scanBtn.addActionListener(e -> runFullScan());
        resolveBtn.addActionListener(e -> markResolved());
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> loadData());
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        tableModel.setRowCount(0);
        String sql = "SELECT a.anomaly_id, c.name, a.detected_at, a.severity, " +
                     "a.description, a.is_resolved " +
                     "FROM anomalies a JOIN customers c ON a.customer_id=c.customer_id " +
                     "ORDER BY a.detected_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String desc = rs.getString(5);
                // Extract z-score from description if present
                String zScore = "—";
                if (desc != null && desc.contains("standard deviations")) {
                    try {
                        int idx = desc.indexOf("is ") + 3;
                        int end = desc.indexOf(" standard");
                        if (idx > 3 && end > idx)
                            zScore = desc.substring(idx, end);
                    } catch (Exception ignored) {}
                }
                tableModel.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2),
                    rs.getString(3) != null ? rs.getString(3).substring(0, 16) : "—",
                    rs.getString(4), zScore,
                    desc != null ? (desc.length() > 80 ? desc.substring(0, 80) + "..." : desc) : "—",
                    rs.getBoolean(6) ? "Yes" : "No"
                });
            }
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        refreshStats();
    }

    private void refreshStats() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement()) {
            ResultSet r1 = st.executeQuery("SELECT COUNT(*) FROM anomalies");
            if (r1.next()) totalLbl.setText(String.valueOf(r1.getInt(1)));
            ResultSet r2 = st.executeQuery("SELECT COUNT(*) FROM anomalies WHERE severity='HIGH'");
            if (r2.next()) highLbl.setText(String.valueOf(r2.getInt(1)));
            ResultSet r3 = st.executeQuery("SELECT COUNT(*) FROM anomalies WHERE is_resolved=FALSE");
            if (r3.next()) unresolvedLbl.setText(String.valueOf(r3.getInt(1)));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Full scan ─────────────────────────────────────────────────────────────

    private void runFullScan() {
        int found = 0;
        String custSql = "SELECT customer_id FROM customers";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(custSql)) {

            while (rs.next()) {
                int customerId = rs.getInt(1);
                List<Double> history = getHistory(conn, customerId);
                if (history.size() < 3) continue;

                // Check each reading against the rest
                for (int i = 2; i < history.size(); i++) {
                    double val = history.get(i);
                    List<Double> prior = history.subList(0, i);
                    if (AnomalyDetector.isAnomaly(val, prior)) {
                        double mean = prior.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                        double z    = AnomalyDetector.getZScore(val, prior);
                        String sev  = AnomalyDetector.getSeverity(z);
                        String desc = AnomalyDetector.getDescription(val, mean, z);

                        // Insert only if not already recorded
                        PreparedStatement ins = conn.prepareStatement(
                            "INSERT IGNORE INTO anomalies (customer_id, description, severity) VALUES (?,?,?)");
                        ins.setInt(1, customerId);
                        ins.setString(2, desc);
                        ins.setString(3, sev);
                        ins.executeUpdate();
                        found++;
                    }
                }
            }
            loadData();
            JOptionPane.showMessageDialog(this,
                found > 0
                    ? "Scan complete. Found " + found + " new anomaly(ies)."
                    : "Scan complete. No new anomalies detected.",
                "Scan Result", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Scan error: " + e.getMessage());
        }
    }

    private List<Double> getHistory(Connection conn, int customerId) throws SQLException {
        List<Double> list = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT consumption_kwh FROM meter_readings WHERE customer_id=? ORDER BY reading_date ASC");
        ps.setInt(1, customerId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(rs.getDouble(1));
        return list;
    }

    private void markResolved() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an anomaly first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE anomalies SET is_resolved=TRUE WHERE anomaly_id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
            User au = utils.SessionManager.getCurrentUser();
            if (au != null) utils.AuditLogger.log(au.getUserId(), au.getUsername(),
                utils.AuditLogger.Action.RESOLVE_ANOMALY,
                "Resolved anomaly ID=" + id);
            loadData();
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an anomaly first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Delete this anomaly record?",
            "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM anomalies WHERE anomaly_id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
            User au = utils.SessionManager.getCurrentUser();
            if (au != null) utils.AuditLogger.log(au.getUserId(), au.getUsername(),
                utils.AuditLogger.Action.DELETE_ANOMALY,
                "Deleted anomaly ID=" + id);
            loadData();
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JPanel buildStatCard(String label, JLabel valueLbl, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 30, 70, 215));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 90));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.setColor(color);
                g2.fillRoundRect(0, getHeight()-4, getWidth(), 4, 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 14, 16));
        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLbl.setForeground(color);
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_MUTED);
        card.add(valueLbl, BorderLayout.CENTER);
        card.add(lbl,      BorderLayout.SOUTH);
        return card;
    }

    private JLabel statValue(String v) {
        JLabel l = new JLabel(v, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 28));
        return l;
    }
}
