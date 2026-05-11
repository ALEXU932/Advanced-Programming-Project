package gui;

import database.DatabaseManager;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

/**
 * AuditLogPanel displays administrative audit logs and provides filtering
 * and management tools for system events.
 */
public class AuditLogPanel extends JPanel {

    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    private JPanel gridPanel;
    private JScrollPane scrollPane;

    // Category groupings
    private static final Map<String, String[]> CATEGORIES = new LinkedHashMap<>();
    private static final Map<String, Color> CAT_COLORS = new HashMap<>();

    static {
        CATEGORIES.put("Authentication", new String[] { "LOGIN", "LOGOUT", "FAILED_LOGIN" });
        CATEGORIES.put("Customers", new String[] { "ADD_CUSTOMER", "EDIT_CUSTOMER", "DELETE_CUSTOMER" });
        CATEGORIES.put("Meters", new String[] { "ADD_METER", "EDIT_METER", "DELETE_METER" });
        CATEGORIES.put("Readings", new String[] { "ADD_READING", "DELETE_READING" });
        CATEGORIES.put("Billing", new String[] { "GENERATE_BILL", "MARK_BILL_PAID", "DELETE_BILL", "EXPORT_BILL" });
        CATEGORIES.put("Payments", new String[] { "RECORD_PAYMENT" });
        CATEGORIES.put("Tariffs", new String[] { "ADD_TARIFF", "EDIT_TARIFF", "TOGGLE_TARIFF" });
        CATEGORIES.put("Anomalies", new String[] { "RESOLVE_ANOMALY", "DELETE_ANOMALY" });
        CATEGORIES.put("Admin & Profile", new String[] { "ADD_ADMIN", "DELETE_ADMIN", "UPDATE_PROFILE",
                "CHANGE_PASSWORD", "UPLOAD_PHOTO", "SETTINGS_CHANGE" });
        CATEGORIES.put("Reports", new String[] { "EXPORT_REPORT" });

        CAT_COLORS.put("Authentication", new Color(0, 180, 255));
        CAT_COLORS.put("Customers", new Color(255, 140, 0));
        CAT_COLORS.put("Meters", new Color(255, 200, 0));
        CAT_COLORS.put("Readings", new Color(0, 200, 150));
        CAT_COLORS.put("Billing", new Color(50, 200, 100));
        CAT_COLORS.put("Payments", new Color(100, 220, 100));
        CAT_COLORS.put("Tariffs", new Color(180, 100, 255));
        CAT_COLORS.put("Anomalies", new Color(255, 80, 80));
        CAT_COLORS.put("Admin & Profile", new Color(255, 140, 0));
        CAT_COLORS.put("Reports", new Color(0, 180, 255));
    }

    public AuditLogPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadData(null, null);
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        JLabel title = new JLabel("Audit Log");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        searchField = UITheme.createTextField();
        searchField.setPreferredSize(new Dimension(200, 36));

        categoryFilter = new JComboBox<>(new String[] {
                "All Categories", "Authentication", "Customers", "Meters",
                "Readings", "Billing", "Payments", "Tariffs", "Anomalies",
                "Admin & Profile", "Reports"
        });
        categoryFilter.setFont(UITheme.FONT_BODY);
        categoryFilter.setBackground(new Color(20, 40, 80));
        categoryFilter.setForeground(UITheme.TEXT_WHITE);
        categoryFilter.setPreferredSize(new Dimension(160, 36));

        JButton searchBtn = UITheme.createAccentButton("Search");
        JButton refreshBtn = UITheme.createAccentButton("Refresh");
        JButton clearBtn = UITheme.createDangerButton("Clear Old Logs");
        for (JButton b : new JButton[] { searchBtn, refreshBtn, clearBtn })
            b.setPreferredSize(new Dimension(b.getPreferredSize().width, 36));

        controls.add(searchField);
        controls.add(categoryFilter);
        controls.add(searchBtn);
        controls.add(refreshBtn);
        controls.add(clearBtn);
        header.add(controls, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Grid panel ────────────────────────────────────────────────────────
        gridPanel = new JPanel();
        gridPanel.setOpaque(false);
        gridPanel.setLayout(new GridLayout(0, 2, 14, 14));

        scrollPane = new JScrollPane(gridPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        add(scrollPane, BorderLayout.CENTER);

        // ── Listeners ─────────────────────────────────────────────────────────
        searchBtn.addActionListener(e -> applyFilter());
        refreshBtn.addActionListener(e -> applyFilter());
        searchField.addActionListener(e -> applyFilter());
        categoryFilter.addActionListener(e -> applyFilter());
        clearBtn.addActionListener(e -> clearOldLogs());
    }

    private void applyFilter() {
        String search = searchField.getText().trim();
        String cat = (String) categoryFilter.getSelectedItem();
        loadData(search.isEmpty() ? null : search,
                "All Categories".equals(cat) ? null : cat);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadData(String search, String categoryName) {
        // Fetch all matching logs grouped by category
        Map<String, List<String[]>> grouped = new LinkedHashMap<>();
        for (String cat : CATEGORIES.keySet())
            grouped.put(cat, new ArrayList<>());

        String sql = "SELECT log_id, username, action, details, performed_at " +
                "FROM audit_log WHERE 1=1";
        if (search != null)
            sql += " AND (username LIKE ? OR details LIKE ? OR action LIKE ?)";
        sql += " ORDER BY performed_at DESC LIMIT 300";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (search != null) {
                String like = "%" + search + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String action = rs.getString("action");
                String logId = String.valueOf(rs.getInt("log_id"));
                String user = rs.getString("username");
                String details = rs.getString("details");
                String time = rs.getString("performed_at");
                if (time != null && time.length() > 16)
                    time = time.substring(0, 16);

                String cat = getCategoryForAction(action);
                if (cat != null)
                    grouped.get(cat).add(new String[] { logId, user, action, details, time });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            return;
        }

        // Rebuild grid
        gridPanel.removeAll();

        for (Map.Entry<String, List<String[]>> entry : grouped.entrySet()) {
            String cat = entry.getKey();
            List<String[]> logs = entry.getValue();

            // Skip if filtering by category and this isn't it
            if (categoryName != null && !cat.equals(categoryName))
                continue;
            // Skip empty categories unless searching
            if (logs.isEmpty() && search == null && categoryName == null)
                continue;

            gridPanel.add(buildCategoryCard(cat, logs));
        }

        // If odd number of cards, add empty filler
        int count = gridPanel.getComponentCount();
        if (count % 2 != 0)
            gridPanel.add(new JPanel() {
                {
                    setOpaque(false);
                }
            });

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    // ── Category card ─────────────────────────────────────────────────────────

    private JPanel buildCategoryCard(String category, List<String[]> logs) {
        Color color = CAT_COLORS.getOrDefault(category, UITheme.ACCENT);

        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12, 25, 60, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                // Left accent bar
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                // Border
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 14));

        // ── Card header ───────────────────────────────────────────────────────
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);

        JLabel catLabel = new JLabel(getCategoryIcon(category) + "  " + category);
        catLabel.setFont(new Font("Segoe UI Symbol", Font.BOLD, 13));
        catLabel.setForeground(color);

        JLabel countLabel = new JLabel(logs.size() + " events");
        countLabel.setFont(UITheme.FONT_SMALL);
        countLabel.setForeground(logs.isEmpty() ? UITheme.TEXT_MUTED : color);
        countLabel.setOpaque(true);
        countLabel.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25));
        countLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80), 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));

        cardHeader.add(catLabel, BorderLayout.WEST);
        cardHeader.add(countLabel, BorderLayout.EAST);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));

        card.add(cardHeader, BorderLayout.NORTH);
        card.add(sep, BorderLayout.CENTER);

        // ── Log entries ───────────────────────────────────────────────────────
        JPanel entriesPanel = new JPanel();
        entriesPanel.setOpaque(false);
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));

        if (logs.isEmpty()) {
            JLabel emptyLbl = new JLabel("No activity recorded");
            emptyLbl.setFont(UITheme.FONT_SMALL);
            emptyLbl.setForeground(UITheme.TEXT_MUTED);
            emptyLbl.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
            entriesPanel.add(emptyLbl);
        } else if (logs.size() <= 6) {
            // Show all entries directly (no expand button needed)
            for (int i = 0; i < logs.size(); i++) {
                entriesPanel.add(buildLogEntry(logs.get(i), color, i % 2 == 0));
                if (i < logs.size() - 1)
                    entriesPanel.add(Box.createVerticalStrut(3));
            }
        } else {
            // More than 6 entries — show first 6 + expand button
            final boolean[] expanded = { false };

            // Container that switches between collapsed and expanded views
            JPanel viewContainer = new JPanel(new CardLayout());
            viewContainer.setOpaque(false);

            // ── Collapsed view: first 6 entries + expand button ──────────────
            JPanel collapsedView = new JPanel();
            collapsedView.setOpaque(false);
            collapsedView.setLayout(new BoxLayout(collapsedView, BoxLayout.Y_AXIS));

            for (int i = 0; i < 6; i++) {
                collapsedView.add(buildLogEntry(logs.get(i), color, i % 2 == 0));
                if (i < 5)
                    collapsedView.add(Box.createVerticalStrut(3));
            }

            JButton expandBtn = createTransparentButton("+ " + (logs.size() - 6) + " more entries...", color);
            collapsedView.add(Box.createVerticalStrut(4));
            collapsedView.add(expandBtn);

            // ── Expanded view: all entries in a scrollpane ───────────────────
            JPanel allEntriesPanel = new JPanel();
            allEntriesPanel.setOpaque(false);
            allEntriesPanel.setLayout(new BoxLayout(allEntriesPanel, BoxLayout.Y_AXIS));

            for (int i = 0; i < logs.size(); i++) {
                allEntriesPanel.add(buildLogEntry(logs.get(i), color, i % 2 == 0));
                if (i < logs.size() - 1)
                    allEntriesPanel.add(Box.createVerticalStrut(3));
            }

            JScrollPane expandedScroll = new JScrollPane(allEntriesPanel);
            expandedScroll.setOpaque(false);
            expandedScroll.getViewport().setOpaque(false);
            expandedScroll.setBorder(null);
            expandedScroll.setPreferredSize(new Dimension(0, 280));
            expandedScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
            expandedScroll.getVerticalScrollBar().setUnitIncrement(16);
            expandedScroll.getVerticalScrollBar().setBackground(new Color(10, 25, 60));

            JPanel expandedView = new JPanel(new BorderLayout());
            expandedView.setOpaque(false);
            JButton collapseBtn = createTransparentButton("\u25B2  Show less", UITheme.TEXT_MUTED);
            expandedView.add(expandedScroll, BorderLayout.CENTER);
            expandedView.add(collapseBtn, BorderLayout.SOUTH);

            viewContainer.add(collapsedView, "COLLAPSED");
            viewContainer.add(expandedView, "EXPANDED");

            // Toggle between views
            CardLayout cl = (CardLayout) viewContainer.getLayout();
            expandBtn.addActionListener(e -> {
                expanded[0] = true;
                cl.show(viewContainer, "EXPANDED");
                viewContainer.revalidate();
                viewContainer.repaint();
            });

            collapseBtn.addActionListener(e -> {
                expanded[0] = false;
                cl.show(viewContainer, "COLLAPSED");
                viewContainer.revalidate();
                viewContainer.repaint();
            });

            entriesPanel.add(viewContainer);
        }

        card.add(entriesPanel, BorderLayout.SOUTH);
        return card;
    }

    // ── Single log entry row ──────────────────────────────────────────────────

    private JPanel buildLogEntry(String[] log, Color color, boolean shaded) {
        // log = [logId, user, action, details, time]
        JPanel row = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                if (shaded) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(255, 255, 255, 8));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                }
            }
        };
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        // Left: dot + action badge
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);

        // Colored dot
        JLabel dot = new JLabel("\u25CF");
        dot.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 10));
        dot.setForeground(getActionColor(log[2]));

        // Action badge
        JLabel actionLbl = new JLabel(formatAction(log[2]));
        actionLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        actionLbl.setForeground(getActionColor(log[2]));
        actionLbl.setOpaque(true);
        actionLbl.setBackground(new Color(getActionColor(log[2]).getRed(),
                getActionColor(log[2]).getGreen(), getActionColor(log[2]).getBlue(), 20));
        actionLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(getActionColor(log[2]).getRed(),
                        getActionColor(log[2]).getGreen(), getActionColor(log[2]).getBlue(), 60), 1),
                BorderFactory.createEmptyBorder(1, 6, 1, 6)));

        left.add(dot);
        left.add(actionLbl);

        // Center: details (truncated)
        String details = log[3] != null ? log[3] : "";
        if (details.length() > 55)
            details = details.substring(0, 55) + "...";
        JLabel detailsLbl = new JLabel(details);
        detailsLbl.setFont(UITheme.FONT_SMALL);
        detailsLbl.setForeground(UITheme.TEXT_LIGHT);
        detailsLbl.setToolTipText(log[3]); // full text on hover

        // Right: user + time
        JPanel right = new JPanel(new GridLayout(2, 1, 0, 0));
        right.setOpaque(false);

        JLabel userLbl = new JLabel(log[1], SwingConstants.RIGHT);
        userLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        userLbl.setForeground(UITheme.PRIMARY);

        JLabel timeLbl = new JLabel(log[4] != null ? log[4] : "", SwingConstants.RIGHT);
        timeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        timeLbl.setForeground(UITheme.TEXT_MUTED);

        right.add(userLbl);
        right.add(timeLbl);
        right.setPreferredSize(new Dimension(130, 36));

        row.add(left, BorderLayout.WEST);
        row.add(detailsLbl, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    // ── Clear old logs ────────────────────────────────────────────────────────

    private void clearOldLogs() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete audit logs older than 30 days?",
                "Confirm Clear", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION)
            return;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                Statement st = conn.createStatement()) {
            int n = st.executeUpdate(
                    "DELETE FROM audit_log WHERE performed_at < DATE_SUB(NOW(), INTERVAL 30 DAY)");
            applyFilter();
            JOptionPane.showMessageDialog(this, n + " old log entries removed.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JButton createTransparentButton(String text, Color textColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 8));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                }
                g2.setFont(getFont());
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                int x = getHorizontalAlignment() == SwingConstants.CENTER
                        ? (getWidth() - fm.stringWidth(getText())) / 2
                        : 4;
                g2.drawString(getText(), x, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(textColor);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 0));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return btn;
    }

    private String getCategoryForAction(String action) {
        for (Map.Entry<String, String[]> e : CATEGORIES.entrySet())
            for (String a : e.getValue())
                if (a.equals(action))
                    return e.getKey();
        return null;
    }

    private String getCategoryIcon(String cat) {
        if (cat == null)
            return "\u25CF";
        switch (cat) {
            case "Authentication":
                return "\u26BF";
            case "Customers":
                return "\u25A3";
            case "Meters":
                return "\u26A1";
            case "Readings":
                return "\u25A6";
            case "Billing":
                return "\u20BF";
            case "Payments":
                return "\u25A4";
            case "Tariffs":
                return "\u2630";
            case "Anomalies":
                return "\u26A0";
            case "Admin & Profile":
                return "\u2699";
            case "Reports":
                return "\u2750";
            default:
                return "\u25CF";
        }
    }

    private String formatAction(String action) {
        if (action == null)
            return "";
        return action.replace("_", " ");
    }

    private Color getActionColor(String action) {
        if (action == null)
            return UITheme.TEXT_MUTED;
        if (action.startsWith("DELETE") || action.equals("FAILED_LOGIN"))
            return UITheme.DANGER;
        if (action.equals("LOGIN") || action.startsWith("ADD") || action.equals("RECORD_PAYMENT"))
            return UITheme.SUCCESS;
        if (action.startsWith("EDIT") || action.startsWith("UPDATE") || action.startsWith("CHANGE")
                || action.equals("TOGGLE_TARIFF") || action.equals("MARK_BILL_PAID"))
            return UITheme.WARNING;
        if (action.startsWith("EXPORT") || action.equals("LOGOUT"))
            return UITheme.ACCENT;
        if (action.equals("RESOLVE_ANOMALY"))
            return new Color(50, 200, 100);
        return UITheme.TEXT_LIGHT;
    }
}
