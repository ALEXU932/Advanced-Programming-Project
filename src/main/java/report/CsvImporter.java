package report;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV importer for meter readings.
 * Expected format: customer_id, reading_date (YYYY-MM-DD), current_reading
 */
public class CsvImporter {

    public static void importReadings(Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import Meter Readings from CSV");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        String path = fc.getSelectedFile().getAbsolutePath();
        List<String[]> valid   = new ArrayList<>();
        List<String>   errors  = new ArrayList<>();
        int lineNum = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (lineNum == 1 && line.toLowerCase().contains("customer")) continue;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length < 3) {
                    errors.add("Line " + lineNum + ": Expected 3 columns, got " + parts.length);
                    continue;
                }

                String custIdStr  = parts[0].trim();
                String dateStr    = parts[1].trim();
                String currStr    = parts[2].trim();

                int customerId;
                try { customerId = Integer.parseInt(custIdStr); }
                catch (NumberFormatException e) {
                    errors.add("Line " + lineNum + ": Invalid customer_id '" + custIdStr + "'");
                    continue;
                }

                if (!dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    errors.add("Line " + lineNum + ": Invalid date '" + dateStr + "' (use YYYY-MM-DD)");
                    continue;
                }

                double currReading;
                try { currReading = Double.parseDouble(currStr); }
                catch (NumberFormatException e) {
                    errors.add("Line " + lineNum + ": Invalid reading '" + currStr + "'");
                    continue;
                }
                if (currReading < 0) {
                    errors.add("Line " + lineNum + ": Reading cannot be negative");
                    continue;
                }

                if (!customerExists(customerId)) {
                    errors.add("Line " + lineNum + ": Customer ID " + customerId + " not found");
                    continue;
                }

                valid.add(new String[]{custIdStr, dateStr, currStr});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Error reading file: " + e.getMessage(),
                "Import Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (valid.isEmpty() && errors.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "CSV file is empty.", "Import", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        showPreviewDialog(parent, valid, errors);
    }

    private static void showPreviewDialog(Component parent, List<String[]> valid, List<String> errors) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent),
            "CSV Import Preview", true);
        dialog.setSize(700, 520);
        dialog.setLocationRelativeTo(parent);

        gui.BackgroundPanel bp = new gui.BackgroundPanel(new BorderLayout(0, 10));
        bp.setOverlayAlpha(0.72f);
        dialog.setContentPane(bp);

        JPanel dh = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8,18,50,240)); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(gui.UITheme.PRIMARY); g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawLine(0,getHeight()-1,getWidth(),getHeight()-1); g2.dispose();
            }
        };
        dh.setOpaque(false); dh.setPreferredSize(new Dimension(0,52));
        dh.setBorder(BorderFactory.createEmptyBorder(0,20,0,20));
        JLabel dhTitle = new JLabel("CSV Import — Meter Readings");
        dhTitle.setFont(new Font("Segoe UI",Font.BOLD,15)); dhTitle.setForeground(gui.UITheme.PRIMARY);
        JLabel dhSub = new JLabel(valid.size() + " valid  |  " + errors.size() + " errors");
        dhSub.setFont(gui.UITheme.FONT_SMALL);
        dhSub.setForeground(errors.isEmpty() ? gui.UITheme.SUCCESS : gui.UITheme.WARNING);
        dh.add(dhTitle, BorderLayout.WEST); dh.add(dhSub, BorderLayout.EAST);
        bp.add(dh, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        tabs.setFont(gui.UITheme.FONT_LABEL);

        String[] cols = {"Customer ID","Reading Date","Current Reading"};
        Object[][] validData = valid.stream()
            .map(r -> new Object[]{r[0], r[1], r[2]})
            .toArray(Object[][]::new);
        JTable validTable = new JTable(validData, cols);
        gui.UITheme.styleTable(validTable);
        tabs.addTab("Valid Rows (" + valid.size() + ")", gui.UITheme.createScrollPane(validTable));

        JTextArea errArea = new JTextArea(String.join("\n", errors));
        errArea.setFont(gui.UITheme.FONT_SMALL);
        errArea.setForeground(gui.UITheme.DANGER);
        errArea.setBackground(new Color(10,25,60,200));
        errArea.setEditable(false);
        JScrollPane errScroll = new JScrollPane(errArea);
        errScroll.setOpaque(false); errScroll.getViewport().setOpaque(false);
        tabs.addTab("Errors (" + errors.size() + ")", errScroll);

        bp.add(tabs, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8,18,50,210)); g2.fillRect(0,0,getWidth(),getHeight());
                g2.dispose();
            }
        };
        footer.setOpaque(false);

        JLabel infoLbl = new JLabel(valid.isEmpty() ? "No valid rows to import." : "");
        infoLbl.setFont(gui.UITheme.FONT_SMALL); infoLbl.setForeground(gui.UITheme.TEXT_MUTED);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(gui.UITheme.FONT_BUTTON); cancelBtn.setForeground(gui.UITheme.TEXT_LIGHT);
        cancelBtn.setBackground(new Color(40,60,100)); cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false); cancelBtn.setPreferredSize(new Dimension(100,36));
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton importBtn = gui.UITheme.createPrimaryButton("Import " + valid.size() + " Rows");
        importBtn.setPreferredSize(new Dimension(160,36));
        importBtn.setEnabled(!valid.isEmpty());

        footer.add(infoLbl); footer.add(cancelBtn); footer.add(importBtn);
        bp.add(footer, BorderLayout.SOUTH);

        importBtn.addActionListener(e -> {
            int inserted = 0, failed = 0;
            try (Connection conn = database.DatabaseManager.getInstance().getConnection()) {
                conn.setAutoCommit(false);
                for (String[] row : valid) {
                    try {
                        int custId = Integer.parseInt(row[0]);
                        double curr = Double.parseDouble(row[2]);
                        double prev = getPreviousReading(conn, custId);
                        double consumption = Math.max(0, curr - prev);

                        PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO meter_readings (customer_id,reading_date,previous_reading,current_reading,consumption_kwh) VALUES (?,?,?,?,?)");
                        ps.setInt(1, custId); ps.setString(2, row[1]);
                        ps.setDouble(3, prev); ps.setDouble(4, curr); ps.setDouble(5, consumption);
                        ps.executeUpdate();
                        inserted++;
                    } catch (SQLException ex) { failed++; }
                }
                conn.commit();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Import error: " + ex.getMessage());
                return;
            }
            dialog.dispose();
            JOptionPane.showMessageDialog(parent,
                String.format("Import complete!\n%d rows inserted\n%d rows failed", inserted, failed),
                "Import Result", JOptionPane.INFORMATION_MESSAGE);
        });

        dialog.setVisible(true);
    }

    private static boolean customerExists(int id) {
        try (Connection conn = database.DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM customers WHERE customer_id=?")) {
            ps.setInt(1, id);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    private static double getPreviousReading(Connection conn, int customerId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "SELECT current_reading FROM meter_readings WHERE customer_id=? ORDER BY reading_date DESC LIMIT 1");
        ps.setInt(1, customerId);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getDouble(1) : 0;
    }
}
