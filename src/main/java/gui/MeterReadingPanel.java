package gui;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import javax.swing.*;

import Logic.AnomalyDetector;
import database.DatabaseManager;
import database.User;

public class MeterReadingPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    

    public MeterReadingPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("Meter Readings");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton addBtn = UITheme.createPrimaryButton("+ Add Reading");
        JButton importBtn = UITheme.createAccentButton("Import CSV");
        JButton exportBtn = UITheme.createAccentButton("Export Excel");
        JButton refreshBtn = UITheme.createAccentButton("↻ Refresh");
        actions.add(addBtn); actions.add(importBtn); actions.add(exportBtn); actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] cols = {"ID", "Customer", "Date", "Prev Reading", "Curr Reading", "Consumption (kWh)", "Anomaly"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);

        JPanel card = UITheme.createCard(null);
        card.setLayout(new BorderLayout());
        card.add(UITheme.createScrollPane(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        addBtn.addActionListener(e -> showAddDialog());
        importBtn.addActionListener(e -> { report.CsvImporter.importReadings(this); loadData(); });
        exportBtn.addActionListener(e -> report.ExcelExporter.export(this, table, "Meter Readings"));
        refreshBtn.addActionListener(e -> loadData());
    }

    private void loadData() {
        tableModel.setRowCount(0);
        String sql = "SELECT mr.reading_id, c.name, mr.reading_date, mr.previous_reading, " +
                     "mr.current_reading, mr.consumption_kwh FROM meter_readings mr " +
                     "JOIN customers c ON mr.customer_id=c.customer_id ORDER BY mr.reading_date DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    String.format("%.2f", rs.getDouble(4)),
                    String.format("%.2f", rs.getDouble(5)),
                    String.format("%.2f", rs.getDouble(6)),
                    checkAnomaly(rs.getInt(1)) ? "⚠ YES" : "✓ Normal"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private boolean checkAnomaly(int readingId) {
        String sql = "SELECT anomaly_id FROM anomalies WHERE reading_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, readingId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) { return false; }
    }

    private void showAddDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "Add Meter Reading", true);
        dialog.setSize(560, 530);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.setOverlayAlpha(0.72f);
        dialog.setContentPane(root);

        // Header
        JPanel dh = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8,18,50,240)); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(UITheme.PRIMARY); g2.setStroke(new BasicStroke(2f));
                g2.drawLine(0,getHeight()-1,getWidth(),getHeight()-1); g2.dispose();
            }
        };
        dh.setOpaque(false); dh.setPreferredSize(new Dimension(0,54));
        dh.setBorder(BorderFactory.createEmptyBorder(0,24,0,24));
        JLabel dhTitle = new JLabel("Add Meter Reading");
        dhTitle.setFont(new Font("Segoe UI",Font.BOLD,16)); dhTitle.setForeground(UITheme.PRIMARY);
        JLabel dhSub = new JLabel("Previous reading is loaded automatically");
        dhSub.setFont(UITheme.FONT_SMALL); dhSub.setForeground(UITheme.TEXT_MUTED);
        dh.add(dhTitle, BorderLayout.WEST); dh.add(dhSub, BorderLayout.EAST);
        root.add(dh, BorderLayout.NORTH);

        // Form — wrapped in scroll pane so nothing clips
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(14, 24, 10, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        g.insets = new Insets(3, 4, 3, 4);

        JComboBox<Object> customerCb = FormDialog.makeCombo();
        loadCustomersIntoCombo(customerCb);
        JTextField dateField = FormDialog.makeField(java.time.LocalDate.now().toString());
        JTextField currField = FormDialog.makeField("");

        // Previous reading — read-only info label (auto-filled)
        JLabel prevInfoLbl = new JLabel("  No previous reading found — starting from 0.00 kWh", SwingConstants.LEFT);
        prevInfoLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        prevInfoLbl.setForeground(UITheme.TEXT_MUTED);
        prevInfoLbl.setOpaque(true);
        prevInfoLbl.setBackground(new Color(10, 30, 70, 160));
        prevInfoLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 80, 140), 1),
            BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        prevInfoLbl.setPreferredSize(new Dimension(0, 34));

        // Holds the actual previous reading value
        final double[] prevReading = {0.0};

        // Consumption display
        JLabel consumptionLbl = new JLabel("  Consumption: — kWh", SwingConstants.LEFT);
        consumptionLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        consumptionLbl.setForeground(UITheme.ACCENT);
        consumptionLbl.setOpaque(true);
        consumptionLbl.setBackground(new Color(0, 50, 90, 140));
        consumptionLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.ACCENT, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        consumptionLbl.setPreferredSize(new Dimension(0, 38));

        // Status
        JLabel statusLbl = new JLabel(" ", SwingConstants.CENTER);
        statusLbl.setFont(UITheme.FONT_SMALL); statusLbl.setForeground(UITheme.DANGER);

        // ── Layout ────────────────────────────────────────────────────────────
        g.gridy=0; g.gridx=0; g.gridwidth=2;
        form.add(fLbl("Customer *"), g);
        g.gridy=1; form.add(customerCb, g);

        g.gridy=2; form.add(fLbl("Reading Date (YYYY-MM-DD) *"), g);
        g.gridy=3; form.add(dateField, g);

        g.gridy=4; form.add(fLbl("Previous Reading (auto-loaded from last entry)"), g);
        g.gridy=5; form.add(prevInfoLbl, g);

        g.gridy=6; form.add(fLbl("Current Reading (kWh) *"), g);
        g.gridy=7; form.add(currField, g);

        g.gridy=8; g.insets=new Insets(8,6,4,6);
        form.add(consumptionLbl, g);

        g.gridy=9; g.insets=new Insets(2,6,2,6);
        form.add(statusLbl, g);

        root.add(form, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,10)) {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D gd = (Graphics2D) g2.create();
                gd.setColor(new Color(8,18,50,210)); gd.fillRect(0,0,getWidth(),getHeight());
                gd.setColor(new Color(40,60,110)); gd.drawLine(0,0,getWidth(),0); gd.dispose();
            }
        };
        footer.setOpaque(false);
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(UITheme.FONT_BUTTON); cancelBtn.setForeground(UITheme.TEXT_LIGHT);
        cancelBtn.setBackground(new Color(40,60,100)); cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false); cancelBtn.setPreferredSize(new Dimension(100,36));
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JButton saveBtn = UITheme.createPrimaryButton("  Save Reading  ");
        saveBtn.setPreferredSize(new Dimension(140,36));
        footer.add(cancelBtn); footer.add(saveBtn);
        root.add(footer, BorderLayout.SOUTH);

        // ── Auto-load previous reading when customer changes ───────────────────
        Runnable loadPrev = () -> {
            Object sel = customerCb.getSelectedItem();
            if (sel == null) return;
            int customerId = (int) ((Object[]) sel)[0];
            double prev = fetchLastReading(customerId);
            prevReading[0] = prev;
            if (prev == 0.0) {
                prevInfoLbl.setText("  No previous reading — starting from 0.00 kWh");
                prevInfoLbl.setForeground(UITheme.TEXT_MUTED);
            } else {
                prevInfoLbl.setText(String.format("  Last reading: %.2f kWh  (auto-loaded)", prev));
                prevInfoLbl.setForeground(UITheme.SUCCESS);
            }
            // Recalculate consumption
            try {
                double curr = Double.parseDouble(currField.getText().trim());
                double diff = curr - prev;
                consumptionLbl.setText(String.format("  Consumption: %.2f kWh", diff));
                consumptionLbl.setForeground(diff < 0 ? UITheme.DANGER : UITheme.ACCENT);
            } catch (NumberFormatException ignored) {
                consumptionLbl.setText("  Consumption: — kWh");
                consumptionLbl.setForeground(UITheme.ACCENT);
            }
        };

        customerCb.addActionListener(e -> loadPrev.run());

        // Load immediately for first customer in list
        if (customerCb.getItemCount() > 0) {
            customerCb.setSelectedIndex(0);
            loadPrev.run();
        }

        // ── Live consumption update as current reading is typed ────────────────
        currField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                try {
                    double curr = Double.parseDouble(currField.getText().trim());
                    double diff = curr - prevReading[0];
                    consumptionLbl.setText(String.format("  Consumption: %.2f kWh", diff));
                    consumptionLbl.setForeground(diff < 0 ? UITheme.DANGER : UITheme.ACCENT);
                    consumptionLbl.setBackground(diff < 0
                        ? new Color(80, 0, 0, 140) : new Color(0, 50, 90, 140));
                    statusLbl.setText(diff < 0 ? "Current reading cannot be less than previous." : " ");
                } catch (NumberFormatException ignored) {
                    consumptionLbl.setText("  Consumption: — kWh");
                    consumptionLbl.setForeground(UITheme.ACCENT);
                    consumptionLbl.setBackground(new Color(0, 50, 90, 140));
                    statusLbl.setText(" ");
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn.addActionListener(e -> {
            if (customerCb.getSelectedItem() == null) {
                statusLbl.setText("Select a customer."); return;
            }
            int customerId = (int) ((Object[]) customerCb.getSelectedItem())[0];
            String date = dateField.getText().trim();
            if (date.isEmpty()) { statusLbl.setText("Reading date is required."); return; }
            String currStr = currField.getText().trim();
            if (currStr.isEmpty()) { statusLbl.setText("Current reading is required."); return; }
            try {
                double curr = Double.parseDouble(currStr);
                double prev = prevReading[0];
                if (curr < prev) {
                    statusLbl.setText(String.format(
                        "Current reading (%.2f) cannot be less than previous (%.2f).", curr, prev));
                    return;
                }
                saveReading(customerId, date, prev, curr, curr - prev);
                loadData();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                statusLbl.setText("Please enter a valid numeric reading.");
            }
        });

        dialog.setVisible(true);
    }

    /** Fetch the most recent current_reading for a customer. Returns 0.0 if none. */
    private double fetchLastReading(int customerId) {
        String sql = "SELECT current_reading FROM meter_readings " +
                     "WHERE customer_id=? ORDER BY reading_date DESC, reading_id DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        } catch (SQLException e) { return 0.0; }
    }

    private JLabel fLbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_LABEL); l.setForeground(UITheme.TEXT_LIGHT);
        return l;
    }

    private void loadCustomersIntoCombo(JComboBox<Object> cb) {
        String sql = "SELECT customer_id, name FROM customers ORDER BY name";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) cb.addItem(new Object[]{rs.getInt(1), rs.getString(2)});
        } catch (SQLException e) { e.printStackTrace(); }
        cb.setRenderer((list, value, index, sel, focus) -> {
            JLabel lbl = new JLabel(value == null ? "" : (String) ((Object[]) value)[1]);
            lbl.setForeground(UITheme.TEXT_WHITE);
            lbl.setBackground(sel ? UITheme.PRIMARY : new Color(20, 40, 80));
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return lbl;
        });
    }

    private void saveReading(int customerId, String date, double prev, double curr, double consumption) {
        String sql = "INSERT INTO meter_readings (customer_id, reading_date, previous_reading, current_reading, consumption_kwh, units) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId); ps.setString(2, date);
            ps.setDouble(3, prev); ps.setDouble(4, curr); ps.setDouble(5, consumption);
            ps.setDouble(6, consumption);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            int readingId = keys.next() ? keys.getInt(1) : -1;
            // Audit log
            User au = Logic.SessionManager.getCurrentUser();
            if (au != null) Logic.AuditLogger.log(au.getUserId(), au.getUsername(),
                Logic.AuditLogger.Action.ADD_READING,
                String.format("Added reading for customer ID=%d: prev=%.2f, curr=%.2f, consumption=%.2f kWh on %s",
                    customerId, prev, curr, consumption, date));
            checkAndSaveAnomaly(customerId, readingId, consumption, conn);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error saving reading: " + e.getMessage());
        }
    }

    private void checkAndSaveAnomaly(int customerId, int readingId, double consumption, Connection conn) throws SQLException {
        List<Double> history = new ArrayList<>();
        String sql = "SELECT consumption_kwh FROM meter_readings WHERE customer_id=? ORDER BY reading_date DESC LIMIT 12";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, customerId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) history.add(rs.getDouble(1));

        if (AnomalyDetector.isAnomaly(consumption, history)) {
            double mean = history.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double z = AnomalyDetector.getZScore(consumption, history);
            String severity = AnomalyDetector.getSeverity(z);
            String desc = AnomalyDetector.getDescription(consumption, mean, z);
            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO anomalies (customer_id, reading_id, description, severity) VALUES (?,?,?,?)");
            ins.setInt(1, customerId); ins.setInt(2, readingId);
            ins.setString(3, desc); ins.setString(4, severity);
            ins.executeUpdate();
            JOptionPane.showMessageDialog(this, "⚠ Anomaly Detected!\n" + desc, "Anomaly Alert", JOptionPane.WARNING_MESSAGE);
        }
    }
}
