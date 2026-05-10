package gui;

import database.DatabaseManager;
import database.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;

public class ReportsPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<String> reportTypeCb;
    private JTextField monthField;

    public ReportsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("Reports & Analytics");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Controls card
        JPanel controlCard = UITheme.createCard("Generate Report");
        controlCard.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
        controlCard.setPreferredSize(new Dimension(0, UITheme.dim(72)));

        reportTypeCb = new JComboBox<>(new String[]{
            "Monthly Billing Summary", "Customer Consumption", "Payment Status", "Anomaly Report", "Tariff Usage"
        });
        reportTypeCb.setFont(UITheme.FONT_BODY);
        reportTypeCb.setBackground(new Color(20, 40, 80));
        reportTypeCb.setForeground(UITheme.TEXT_WHITE);
        reportTypeCb.setPreferredSize(new Dimension(UITheme.dim(210), UITheme.dim(32)));

        monthField = UITheme.createTextField();
        monthField.setText(java.time.YearMonth.now().toString());
        monthField.setPreferredSize(new Dimension(UITheme.dim(110), UITheme.dim(32)));

        JButton generateBtn = UITheme.createPrimaryButton("📊 Generate");
        JButton exportCsvBtn = UITheme.createAccentButton("📥 Export CSV");

        controlCard.add(UITheme.createLabel("Report Type:"));
        controlCard.add(reportTypeCb);
        controlCard.add(UITheme.createLabel("Month:"));
        controlCard.add(monthField);
        controlCard.add(generateBtn);
        controlCard.add(exportCsvBtn);

        // Table
        tableModel = new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);

        JPanel tableCard = UITheme.createCard(null);
        tableCard.setLayout(new BorderLayout());
        tableCard.add(UITheme.createScrollPane(table), BorderLayout.CENTER);

        // Summary bar
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        summaryPanel.setOpaque(false);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(controlCard, BorderLayout.NORTH);
        content.add(tableCard, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        generateBtn.addActionListener(e -> generateReport());
        exportCsvBtn.addActionListener(e -> exportCsv());
    }

    private void generateReport() {
        String type = (String) reportTypeCb.getSelectedItem();
        String month = monthField.getText().trim();
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        switch (type) {
            case "Monthly Billing Summary": generateBillingSummary(month); break;
            case "Customer Consumption": generateConsumptionReport(month); break;
            case "Payment Status": generatePaymentStatus(month); break;
            case "Anomaly Report": generateAnomalyReport(); break;
            case "Tariff Usage": generateTariffUsage(); break;
        }
    }

    private void generateBillingSummary(String month) {
        String sql = "SELECT c.name, b.billing_month, b.consumption_kwh, b.total_amount, b.status, b.due_date " +
                     "FROM bills b JOIN customers c ON b.customer_id=c.customer_id " +
                     "WHERE b.billing_month=? ORDER BY c.name";
        String[] cols = {"Customer", "Month", "Consumption (kWh)", "Total ($)", "Status", "Due Date"};
        tableModel.setColumnIdentifiers(cols);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) tableModel.addRow(new Object[]{
                rs.getString(1), rs.getString(2),
                String.format("%.2f", rs.getDouble(3)),
                String.format("$%.2f", rs.getDouble(4)),
                rs.getString(5), rs.getString(6)
            });
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void generateConsumptionReport(String month) {
        String sql = "SELECT c.name, COALESCE(m.meter_number,'—') as meter_number, " +
                     "SUM(mr.consumption_kwh) as total, " +
                     "AVG(mr.consumption_kwh) as avg, MAX(mr.consumption_kwh) as max " +
                     "FROM meter_readings mr " +
                     "JOIN customers c ON mr.customer_id=c.customer_id " +
                     "LEFT JOIN meters m ON m.customer_id=c.customer_id " +
                     "WHERE DATE_FORMAT(mr.reading_date,'%Y-%m')=? " +
                     "GROUP BY c.customer_id ORDER BY total DESC";
        String[] cols = {"Customer", "Meter No.", "Total kWh", "Avg kWh", "Max kWh"};
        tableModel.setColumnIdentifiers(cols);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) tableModel.addRow(new Object[]{
                rs.getString(1), rs.getString(2),
                String.format("%.2f", rs.getDouble(3)),
                String.format("%.2f", rs.getDouble(4)),
                String.format("%.2f", rs.getDouble(5))
            });
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void generatePaymentStatus(String month) {
        String sql = "SELECT b.status, COUNT(*) as count, SUM(b.total_amount) as total " +
                     "FROM bills b WHERE b.billing_month=? GROUP BY b.status";
        String[] cols = {"Status", "Count", "Total Amount ($)"};
        tableModel.setColumnIdentifiers(cols);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) tableModel.addRow(new Object[]{
                rs.getString(1), rs.getInt(2), String.format("$%.2f", rs.getDouble(3))
            });
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void generateAnomalyReport() {
        String sql = "SELECT c.name, a.severity, COUNT(*) as count, a.is_resolved " +
                     "FROM anomalies a JOIN customers c ON a.customer_id=c.customer_id " +
                     "GROUP BY c.customer_id, a.severity, a.is_resolved ORDER BY count DESC";
        String[] cols = {"Customer", "Severity", "Count", "Resolved"};
        tableModel.setColumnIdentifiers(cols);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) tableModel.addRow(new Object[]{
                rs.getString(1), rs.getString(2), rs.getInt(3),
                rs.getBoolean(4) ? "Yes" : "No"
            });
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void generateTariffUsage() {
        String sql = "SELECT t.name, COUNT(b.bill_id) as bills, SUM(b.consumption_kwh) as kwh, SUM(b.total_amount) as revenue " +
                     "FROM bills b JOIN tariffs t ON b.tariff_id=t.tariff_id GROUP BY t.tariff_id ORDER BY revenue DESC";
        String[] cols = {"Tariff", "Bills Count", "Total kWh", "Revenue ($)"};
        tableModel.setColumnIdentifiers(cols);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) tableModel.addRow(new Object[]{
                rs.getString(1), rs.getInt(2),
                String.format("%.2f", rs.getDouble(3)),
                String.format("$%.2f", rs.getDouble(4))
            });
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void exportCsv() {
        if (tableModel.getRowCount() == 0) { JOptionPane.showMessageDialog(this, "Generate a report first."); return; }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("report.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                pw.print(tableModel.getColumnName(c));
                if (c < tableModel.getColumnCount() - 1) pw.print(",");
            }
            pw.println();
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    Object val = tableModel.getValueAt(r, c);
                    pw.print(val != null ? "\"" + val + "\"" : "");
                    if (c < tableModel.getColumnCount() - 1) pw.print(",");
                }
                pw.println();
            }
            JOptionPane.showMessageDialog(this, "Report exported successfully!");
            User au = logic.SessionManager.getCurrentUser();
            if (au != null) logic.AuditLogger.log(au.getUserId(), au.getUsername(),
                logic.AuditLogger.Action.EXPORT_REPORT,
                "Exported report '" + reportTypeCb.getSelectedItem() + "' to CSV: " + fc.getSelectedFile().getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export error: " + e.getMessage());
        }
    }
}
