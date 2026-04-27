package gui;

import db.DatabaseManager;
import models.Customer;
import utils.AuditLogger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * Customer Support Panel — Report wrong bill, power outage, submit complaint,
 * support request, and FAQ section.
 */
public class CustomerSupportPanel extends JPanel {

    private final Customer customer;
    private JTable ticketsTable;
    private DefaultTableModel ticketsModel;

    public CustomerSupportPanel(Customer customer) {
        this.customer = customer;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
        loadTickets();
    }

    private void buildUI() {
        JLabel title = new JLabel("\u2709 Support Center");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);
        add(title, BorderLayout.NORTH);

        // ── Split: left = actions + tickets, right = FAQ ──────────────────────
        JPanel center = new JPanel(new GridLayout(1, 2, 16, 0));
        center.setOpaque(false);
        center.add(buildLeftPanel());
        center.add(buildFAQPanel());
        add(center, BorderLayout.CENTER);
    }

    // ── Left panel: quick actions + ticket history ────────────────────────────

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        // Quick action buttons
        JPanel actionsCard = UITheme.createCard("Submit a Request");
        actionsCard.setLayout(new GridLayout(2, 2, 10, 10));

        JButton wrongBillBtn   = makeActionButton("\u26A0 Wrong Bill",      UITheme.WARNING);
        JButton outageBtn      = makeActionButton("\u26A1 Power Outage",    UITheme.DANGER);
        JButton complaintBtn   = makeActionButton("\u270F Complaint",       UITheme.PRIMARY);
        JButton supportBtn     = makeActionButton("\u2709 Support Request", UITheme.ACCENT);

        actionsCard.add(wrongBillBtn);
        actionsCard.add(outageBtn);
        actionsCard.add(complaintBtn);
        actionsCard.add(supportBtn);

        // Ticket history table
        String[] cols = {"Ticket #", "Type", "Subject", "Status", "Date"};
        ticketsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        ticketsTable = new JTable(ticketsModel);
        UITheme.styleTable(ticketsTable);
        ticketsTable.setRowHeight(UITheme.dim(28));

        // Color-code status
        ticketsTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setOpaque(true);
                String s = v != null ? v.toString() : "";
                switch (s) {
                    case "OPEN":         setForeground(UITheme.WARNING); break;
                    case "IN_PROGRESS":  setForeground(UITheme.ACCENT);  break;
                    case "RESOLVED":     setForeground(UITheme.SUCCESS); break;
                    case "CLOSED":       setForeground(UITheme.TEXT_MUTED); break;
                    default:             setForeground(UITheme.TEXT_LIGHT);
                }
                setBackground(sel ? UITheme.PRIMARY : UITheme.TABLE_ROW1);
                return this;
            }
        });

        JPanel ticketsCard = UITheme.createCard("My Support Tickets");
        ticketsCard.setLayout(new BorderLayout(0, 8));

        JPanel ticketActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        ticketActions.setOpaque(false);
        JButton refreshBtn = UITheme.createAccentButton("↻ Refresh");
        refreshBtn.addActionListener(e -> loadTickets());
        ticketActions.add(refreshBtn);
        ticketsCard.add(ticketActions, BorderLayout.NORTH);
        ticketsCard.add(UITheme.createScrollPane(ticketsTable), BorderLayout.CENTER);

        panel.add(actionsCard, BorderLayout.NORTH);
        panel.add(ticketsCard, BorderLayout.CENTER);

        // Button actions
        wrongBillBtn.addActionListener(e -> showTicketDialog("WRONG_BILL",   "Wrong Bill Report"));
        outageBtn.addActionListener(e   -> showTicketDialog("POWER_OUTAGE",  "Power Outage Report"));
        complaintBtn.addActionListener(e -> showTicketDialog("COMPLAINT",    "General Complaint"));
        supportBtn.addActionListener(e  -> showTicketDialog("SUPPORT",       "Support Request"));

        return panel;
    }

    // ── FAQ panel ─────────────────────────────────────────────────────────────

    private JPanel buildFAQPanel() {
        JPanel card = UITheme.createCard("❓ Frequently Asked Questions");
        card.setLayout(new BorderLayout());

        JPanel faqContent = new JPanel();
        faqContent.setOpaque(false);
        faqContent.setLayout(new BoxLayout(faqContent, BoxLayout.Y_AXIS));
        faqContent.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        String[][] faqs = {
            {"How is my bill calculated?",
             "Your bill = (Consumption kWh × Rate per kWh) + Fixed Service Charge + applicable taxes. " +
             "The rate depends on your assigned tariff plan."},
            {"When is my bill due?",
             "Bills are typically due 30 days after generation. Check the due date on your bill or in the Bills section."},
            {"What happens if I pay late?",
             "A late payment penalty (usually 5%) is applied to overdue bills. Pay before the due date to avoid extra charges."},
            {"How do I dispute a bill?",
             "Go to My Bills, select the bill, and click 'Dispute Bill'. Provide the reason and description. " +
             "Our team will review within 3-5 business days."},
            {"How do I submit my meter reading?",
             "Go to Readings section and click 'Submit Reading'. Enter your current meter reading. " +
             "Readings are verified by our team."},
            {"What is Auto-Pay?",
             "Auto-Pay automatically pays your bills on the due date using your preferred payment method. " +
             "Enable it in the Payments section under Auto-Pay Settings."},
            {"How do I set a usage budget?",
             "On the Dashboard, click 'Set Monthly Budget'. Enter your kWh limit and alert threshold percentage."},
            {"Why does my usage seem high?",
             "High usage can be caused by: AC/heating running continuously, old appliances, water heaters, " +
             "or meter faults. Check the AI Insights section for personalized recommendations."},
            {"How do I update my contact information?",
             "Go to My Profile and update your name, email, phone, and address, then click 'Update Profile'."},
            {"How do I contact support?",
             "Use this Support Center to submit a ticket. You can also call " +
             utils.SystemSettings.getCompanyPhone() + " or email " + utils.SystemSettings.getCompanyEmail() + "."}
        };

        for (String[] faq : faqs) {
            faqContent.add(buildFAQItem(faq[0], faq[1]));
            faqContent.add(Box.createVerticalStrut(4));
        }

        JScrollPane scroll = new JScrollPane(faqContent);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        // Contact info footer
        JPanel contactPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        contactPanel.setOpaque(false);
        contactPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JLabel phoneLbl = new JLabel("Tel: " + utils.SystemSettings.getCompanyPhone());
        phoneLbl.setFont(UITheme.FONT_LABEL);
        phoneLbl.setForeground(UITheme.ACCENT);

        JLabel emailLbl = new JLabel("Email: " + utils.SystemSettings.getCompanyEmail());
        emailLbl.setFont(UITheme.FONT_LABEL);
        emailLbl.setForeground(UITheme.ACCENT);

        contactPanel.add(phoneLbl);
        contactPanel.add(emailLbl);
        card.add(contactPanel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildFAQItem(String question, String answer) {
        JPanel item = new JPanel(new BorderLayout(0, 4)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 40, 90, 160));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(0, 180, 255, 50));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
            }
        };
        item.setOpaque(false);
        item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel qLbl = new JLabel("Q: " + question);
        qLbl.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(11)));
        qLbl.setForeground(UITheme.PRIMARY);

        JLabel aLbl = new JLabel("<html><div style='width:300px;color:#c8dcff;font-size:10px;'>" +
            "A: " + answer + "</div></html>");
        aLbl.setFont(UITheme.FONT_SMALL);
        aLbl.setForeground(UITheme.TEXT_LIGHT);

        item.add(qLbl, BorderLayout.NORTH);
        item.add(aLbl, BorderLayout.CENTER);
        return item;
    }

    // ── Submit ticket dialog ──────────────────────────────────────────────────

    private void showTicketDialog(String type, String typeLabel) {
        FormDialog dialog = new FormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            typeLabel, "Describe your issue in detail", 540, 400);

        JTextField subjectF = FormDialog.makeField("");
        JTextArea descArea  = new JTextArea(5, 20);
        descArea.setFont(UITheme.FONT_BODY);
        descArea.setForeground(UITheme.TEXT_WHITE);
        descArea.setBackground(new Color(20, 40, 80, 200));
        descArea.setCaretColor(Color.WHITE);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.ACCENT, 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setOpaque(false);
        descScroll.getViewport().setOpaque(false);
        descScroll.setBorder(null);
        descScroll.setPreferredSize(new Dimension(0, 100));

        JComboBox<String> priorityCb = FormDialog.makeStringCombo(new String[]{"LOW", "MEDIUM", "HIGH", "URGENT"});

        dialog.addField("Subject *", subjectF);
        dialog.addField("Description *", descScroll);
        dialog.addField("Priority", priorityCb);
        dialog.addStatus();
        dialog.addCancelButton();
        JButton submitBtn = dialog.addSaveButton("  Submit Ticket  ");

        submitBtn.addActionListener(e -> {
            String subject = subjectF.getText().trim();
            String desc    = descArea.getText().trim();
            String priority = (String) priorityCb.getSelectedItem();
            if (subject.isEmpty()) { dialog.setStatus("Subject is required.", true); return; }
            if (desc.isEmpty())    { dialog.setStatus("Description is required.", true); return; }

            String sql = "INSERT INTO support_tickets (customer_id, type, subject, description, priority, status) " +
                         "VALUES (?,?,?,?,?,'OPEN')";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, customer.getCustomerId());
                ps.setString(2, type);
                ps.setString(3, subject);
                ps.setString(4, desc);
                ps.setString(5, priority);
                ps.executeUpdate();

                AuditLogger.log(customer.getUserId(), customer.getName(),
                    AuditLogger.Action.SETTINGS_CHANGE,
                    "Customer submitted support ticket: type=" + type + " subject=" + subject);

                // Add notification
                addNotification("Your support ticket '" + subject + "' has been submitted. We'll respond within 24-48 hours.", "SUPPORT");

                dialog.dispose();
                JOptionPane.showMessageDialog(this,
                    "Ticket submitted!\n\nType: " + typeLabel +
                    "\nSubject: " + subject +
                    "\nPriority: " + priority +
                    "\n\nOur team will respond within 24-48 hours.",
                    "Ticket Submitted", JOptionPane.INFORMATION_MESSAGE);
                loadTickets();
            } catch (SQLException ex) {
                dialog.setStatus("Error: " + ex.getMessage(), true);
            }
        });

        dialog.setVisible(true);
    }

    // ── Load tickets ──────────────────────────────────────────────────────────

    private void loadTickets() {
        ticketsModel.setRowCount(0);
        String sql = "SELECT ticket_id, type, subject, status, DATE(created_at) " +
                     "FROM support_tickets WHERE customer_id=? ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ticketsModel.addRow(new Object[]{
                    String.format("#%06d", rs.getInt(1)),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5)
                });
            }
        } catch (SQLException e) {
            // Table may not exist yet — silently ignore
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JButton makeActionButton(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover()
                    ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 80)
                    : new Color(color.getRed(), color.getGreen(), color.getBlue(), 40);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 160));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(12)));
        btn.setForeground(color);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, UITheme.dim(52)));
        return btn;
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
