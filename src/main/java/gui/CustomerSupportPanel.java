package gui;

import java.awt.*;
import java.sql.*;
import javax.swing.*;

import database.DatabaseManager;
import database.Customer;

public class CustomerSupportPanel extends JPanel {

    private final Customer customer;
    private JTable disputeTable;
    private DefaultTableModel disputeModel;

    public CustomerSupportPanel(Customer customer) {
        this.customer = customer;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadDisputes();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("\uD83C\uDF9F Support Center");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton complaintBtn = UITheme.createDangerButton("Report Wrong Bill");
        JButton outageBtn    = UITheme.createPrimaryButton("Report Outage");
        JButton faqBtn       = UITheme.createAccentButton("FAQ");
        JButton refreshBtn   = UITheme.createAccentButton("\u21BB Refresh");
        actions.add(complaintBtn); actions.add(outageBtn); actions.add(faqBtn); actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);

        // Quick action cards
        JPanel topRow = new JPanel(new GridLayout(1, 3, 16, 0));
        topRow.setOpaque(false);
        topRow.setPreferredSize(new Dimension(0, UITheme.dim(150)));
        topRow.add(buildQuickCard("\uD83D\uDCCB", "Submit Complaint",
            "Report billing errors, meter issues, or service problems.",
            UITheme.WARNING, () -> showComplaintDialog("Billing Complaint")));
        topRow.add(buildQuickCard("\u26A1", "Power Outage",
            "Report a power outage or service interruption in your area.",
            UITheme.DANGER, () -> showComplaintDialog("Power Outage")));
        topRow.add(buildQuickCard("\u2753", "FAQ",
            "Find answers to common questions about billing and services.",
            UITheme.ACCENT, this::showFAQ));

        // Dispute history table
        String[] cols = {"#", "Bill #", "Reason", "Status", "Submitted", "Resolved"};
        disputeModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        disputeTable = new JTable(disputeModel);
        UITheme.styleTable(disputeTable);
        disputeTable.setRowHeight(UITheme.dim(28));

        disputeTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER); setOpaque(true);
                String s = v != null ? v.toString() : "";
                if      ("OPEN".equals(s))         setForeground(UITheme.WARNING);
                else if ("UNDER_REVIEW".equals(s)) setForeground(UITheme.ACCENT);
                else if ("RESOLVED".equals(s))     setForeground(UITheme.SUCCESS);
                else if ("REJECTED".equals(s))     setForeground(UITheme.DANGER);
                else                               setForeground(UITheme.TEXT_LIGHT);
                setBackground(sel ? UITheme.PRIMARY : UITheme.TABLE_ROW1);
                return this;
            }
        });

        JPanel tableCard = UITheme.createCard("My Complaints & Disputes");
        tableCard.setLayout(new BorderLayout());
        tableCard.add(UITheme.createScrollPane(disputeTable), BorderLayout.CENTER);

        JPanel topSection = new JPanel(new BorderLayout(0, 12));
        topSection.setOpaque(false);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(topRow, BorderLayout.SOUTH);

        add(topSection, BorderLayout.NORTH);
        add(tableCard,  BorderLayout.CENTER);

        complaintBtn.addActionListener(e -> showComplaintDialog("Billing Complaint"));
        outageBtn.addActionListener(e -> showComplaintDialog("Power Outage"));
        faqBtn.addActionListener(e -> showFAQ());
        refreshBtn.addActionListener(e -> loadDisputes());
    }

    private void loadDisputes() {
        disputeModel.setRowCount(0);
        String sql = "SELECT dispute_id, bill_id, reason, status, DATE(created_at), COALESCE(DATE(resolved_at),'—') " +
                     "FROM disputes WHERE customer_id=? ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                disputeModel.addRow(new Object[]{
                    String.format("#%06d", rs.getInt(1)),
                    String.format("#%06d", rs.getInt(2)),
                    rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)
                });
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void showComplaintDialog(String defaultReason) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Submit Support Request", true);
        dialog.setSize(UITheme.dim(540), UITheme.dim(400));
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.setOverlayAlpha(0.72f);
        dialog.setContentPane(root);

        JPanel dh = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8, 18, 50, 240)); g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UITheme.DANGER); g2.setStroke(new BasicStroke(2f));
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1); g2.dispose();
            }
        };
        dh.setOpaque(false); dh.setPreferredSize(new Dimension(0, UITheme.dim(50)));
        dh.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
        JLabel dhTitle = new JLabel("\uD83D\uDCCB Submit Support Request");
        dhTitle.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(14)));
        dhTitle.setForeground(UITheme.DANGER);
        dh.add(dhTitle, BorderLayout.WEST);
        root.add(dh, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(16, 24, 10, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        gbc.insets = new Insets(5, 6, 5, 6);

        JComboBox<String> categoryCb = new JComboBox<>(new String[]{
            "Billing Complaint", "Power Outage", "Meter Fault", "Wrong Reading", "Service Request", "Other"
        });
        categoryCb.setFont(UITheme.FONT_BODY); categoryCb.setBackground(new Color(20, 40, 80));
        categoryCb.setForeground(UITheme.TEXT_WHITE); categoryCb.setBorder(BorderFactory.createLineBorder(UITheme.ACCENT, 1));
        categoryCb.setPreferredSize(new Dimension(0, UITheme.dim(34)));
        categoryCb.setSelectedItem(defaultReason);

        JTextArea descArea = new JTextArea(4, 30);
        descArea.setFont(UITheme.FONT_BODY); descArea.setForeground(UITheme.TEXT_WHITE);
        descArea.setBackground(new Color(20, 40, 80, 200)); descArea.setCaretColor(Color.WHITE);
        descArea.setLineWrap(true); descArea.setWrapStyleWord(true);
        descArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.ACCENT, 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setOpaque(false); descScroll.getViewport().setOpaque(false);
        descScroll.setBorder(null); descScroll.setPreferredSize(new Dimension(0, UITheme.dim(90)));

        JLabel statusLbl = new JLabel(" ", SwingConstants.CENTER);
        statusLbl.setFont(UITheme.FONT_SMALL); statusLbl.setForeground(UITheme.DANGER);

        gbc.gridy = 0; gbc.gridwidth = 2; form.add(fLbl("Category *"), gbc);
        gbc.gridy = 1; form.add(categoryCb, gbc);
        gbc.gridy = 2; form.add(fLbl("Description *"), gbc);
        gbc.gridy = 3; form.add(descScroll, gbc);
        gbc.gridy = 4; gbc.insets = new Insets(4, 6, 2, 6); form.add(statusLbl, gbc);
        root.add(form, BorderLayout.CENTER);

        JPanel footer = buildDialogFooter();
        JButton cancelBtn = (JButton) footer.getComponent(0);
        JButton submitBtn = (JButton) footer.getComponent(1);
        submitBtn.setText("  Submit Request  ");
        root.add(footer, BorderLayout.SOUTH);

        cancelBtn.addActionListener(e -> dialog.dispose());
        submitBtn.addActionListener(e -> {
            String desc = descArea.getText().trim();
            if (desc.isEmpty()) { statusLbl.setText("Please describe the issue."); return; }
            String reason = (String) categoryCb.getSelectedItem();
            // Find any bill for this customer to satisfy FK
            String sql = "INSERT INTO disputes (bill_id, customer_id, reason, description, status) " +
                         "SELECT COALESCE((SELECT bill_id FROM bills WHERE customer_id=? LIMIT 1),1), ?, ?, ?, 'OPEN'";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, customer.getCustomerId()); ps.setInt(2, customer.getCustomerId());
                ps.setString(3, reason); ps.setString(4, desc);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(dialog,
                    "<html><b>\u2705 Request Submitted!</b><br>Our support team will review it shortly.</html>",
                    "Submitted", JOptionPane.INFORMATION_MESSAGE);
                loadDisputes(); dialog.dispose();
            } catch (SQLException ex) { statusLbl.setText("Error: " + ex.getMessage()); }
        });
        dialog.setVisible(true);
    }

    private void showFAQ() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "FAQ", true);
        dialog.setSize(UITheme.dim(600), UITheme.dim(500));
        dialog.setLocationRelativeTo(this);

        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.setOverlayAlpha(0.72f);
        dialog.setContentPane(root);

        JPanel dh = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8, 18, 50, 240)); g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UITheme.ACCENT); g2.setStroke(new BasicStroke(2f));
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1); g2.dispose();
            }
        };
        dh.setOpaque(false); dh.setPreferredSize(new Dimension(0, UITheme.dim(48)));
        dh.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
        JLabel dhTitle = new JLabel("\u2753 Frequently Asked Questions");
        dhTitle.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(14)));
        dhTitle.setForeground(UITheme.ACCENT);
        dh.add(dhTitle, BorderLayout.WEST);
        root.add(dh, BorderLayout.NORTH);

        JTextArea faqArea = new JTextArea();
        faqArea.setFont(UITheme.FONT_BODY); faqArea.setForeground(UITheme.TEXT_WHITE);
        faqArea.setBackground(new Color(10, 25, 60, 200)); faqArea.setEditable(false);
        faqArea.setLineWrap(true); faqArea.setWrapStyleWord(true);
        faqArea.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        faqArea.setText(
            "Q: How is my electricity bill calculated?\n" +
            "A: Your bill is based on meter readings (kWh) multiplied by the tariff rate, plus a fixed service charge and taxes.\n\n" +
            "Q: When is my bill due?\n" +
            "A: Bills are typically due 30 days after generation. Check the 'Due Date' column in My Bills.\n\n" +
            "Q: What payment methods are accepted?\n" +
            "A: Cash, Bank Transfer, Mobile Money, Card, and Online payments.\n\n" +
            "Q: How do I dispute a bill?\n" +
            "A: Go to My Bills, select the bill, and click 'Dispute Bill'. Or use 'Report Wrong Bill' here.\n\n" +
            "Q: What happens if I miss a payment?\n" +
            "A: Overdue bills may incur a late penalty. Contact support immediately if you cannot pay on time.\n\n" +
            "Q: How do I read my meter?\n" +
            "A: Your meter shows a 5-6 digit number in kWh. Read from left to right, ignoring any red digits.\n\n" +
            "Q: Why is my consumption higher than usual?\n" +
            "A: Check the AI Insights tab for anomaly detection. Common causes: new appliances, seasonal changes, or meter faults.\n\n" +
            "Q: How do I update my contact information?\n" +
            "A: Go to My Profile and update your details there.\n\n" +
            "Q: Can I set a usage budget?\n" +
            "A: Yes! On the Dashboard, click 'Set Monthly Budget' to set a kWh limit and get alerts.\n\n" +
            "Q: How do I report a power outage?\n" +
            "A: Use the 'Report Outage' button above. Our team will be notified immediately.\n\n" +
            "Q: How long does dispute resolution take?\n" +
            "A: Most disputes are reviewed within 3-5 business days. Track status in the table below.\n\n" +
            "Q: Is my data secure?\n" +
            "A: Yes. All data is encrypted and access is controlled by role-based authentication.");

        JScrollPane sp = new JScrollPane(faqArea);
        sp.setOpaque(false); sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(sp, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        footer.setOpaque(false);
        JButton closeBtn = UITheme.createAccentButton("Close");
        closeBtn.setPreferredSize(new Dimension(UITheme.dim(100), UITheme.dim(34)));
        footer.add(closeBtn);
        root.add(footer, BorderLayout.SOUTH);
        closeBtn.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private JPanel buildQuickCard(String icon, String title, String desc, Color color, Runnable action) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 30, 70, 215));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false); card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLbl = new JLabel(icon + "  " + title);
        iconLbl.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(12)));
        iconLbl.setForeground(color);

        JLabel descLbl = new JLabel("<html><div style='width:160px'>" + desc + "</div></html>");
        descLbl.setFont(UITheme.FONT_SMALL); descLbl.setForeground(UITheme.TEXT_MUTED);

        JButton btn = new JButton("Open \u2192");
        btn.setFont(UITheme.FONT_SMALL); btn.setForeground(color);
        btn.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());

        card.add(iconLbl, BorderLayout.NORTH);
        card.add(descLbl, BorderLayout.CENTER);
        card.add(btn, BorderLayout.SOUTH);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { action.run(); }
        });
        return card;
    }

    private JPanel buildDialogFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8, 18, 50, 210)); g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(40, 60, 110)); g2.drawLine(0, 0, getWidth(), 0); g2.dispose();
            }
        };
        footer.setOpaque(false);
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(UITheme.FONT_BUTTON); cancelBtn.setForeground(UITheme.TEXT_LIGHT);
        cancelBtn.setBackground(new Color(40, 60, 100)); cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false); cancelBtn.setPreferredSize(new Dimension(UITheme.dim(100), UITheme.dim(34)));
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JButton submitBtn = UITheme.createDangerButton("Submit");
        submitBtn.setPreferredSize(new Dimension(UITheme.dim(150), UITheme.dim(34)));
        footer.add(cancelBtn); footer.add(submitBtn);
        return footer;
    }

    private JLabel fLbl(String text) {
        JLabel l = new JLabel(text); l.setFont(UITheme.FONT_LABEL); l.setForeground(UITheme.TEXT_LIGHT); return l;
    }
}
