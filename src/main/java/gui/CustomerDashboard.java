package gui;

import database.DatabaseManager;
import database.Customer;
import database.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class CustomerDashboard extends JFrame {

    private final User currentUser;
    private Customer customer;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JPanel sidebar;
    private boolean sidebarExpanded = true;

    private static final int SIDEBAR_EXPANDED  = UITheme.dim(200);
    private static final int SIDEBAR_COLLAPSED = UITheme.dim(52);

    private final java.util.List<JToggleButton> navButtons = new java.util.ArrayList<>();

    public CustomerDashboard(User user) {
        this.currentUser = user;
        setTitle("Customer Portal - AI Electric Billing System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.max(1000, (int)(screen.width  * 0.85));
        int h = Math.max(650,  (int)(screen.height * 0.85));
        setSize(w, h);
        setMinimumSize(new Dimension(900, 600));
        setResizable(true);
        setLocationRelativeTo(null);
        loadCustomer();
        buildUI();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) { repaint(); }
        });
    }

    // ── Load customer ─────────────────────────────────────────────────────────

    private void loadCustomer() {
        String sql = "SELECT c.*, COALESCE(m.meter_number,'') AS meter_number " +
                     "FROM customers c " +
                     "LEFT JOIN meters m ON m.customer_id = c.customer_id " +
                     "WHERE c.user_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUser.getUserId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                customer = new Customer();
                customer.setCustomerId(rs.getInt("customer_id"));
                customer.setUserId(rs.getInt("user_id"));
                customer.setName(rs.getString("name"));
                customer.setEmail(rs.getString("email"));
                customer.setPhone(rs.getString("phone"));
                customer.setAddress(rs.getString("address"));
                customer.setMeterNumber(rs.getString("meter_number"));
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(CustomerDashboard.class.getName()).warning(e.getMessage());
        }
    }

    // ── Build UI ──────────────────────────────────────────────────────────────

    private void buildUI() {
        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.setOverlayAlpha(0.45f);
        setContentPane(root);

        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);

        cardLayout    = new CardLayout();
        contentPanel  = new JPanel(cardLayout);
        contentPanel.setOpaque(false);

        if (customer != null) {
            contentPanel.add(new CustomerHomePanel(customer),              "HOME");
            contentPanel.add(new CustomerBillsPanel(customer),            "BILLS");
            contentPanel.add(new CustomerPaymentPanel(customer),          "PAYMENTS");
            contentPanel.add(new CustomerReadingsPanel(customer),         "READINGS");
            contentPanel.add(new CustomerUsageChartPanel(customer),       "CHARTS");
            contentPanel.add(new CustomerAIPanel(customer),               "AI");
            contentPanel.add(new CustomerNotificationsPanel(customer),    "NOTIFICATIONS");
            contentPanel.add(new CustomerSupportPanel(customer),          "SUPPORT");
            contentPanel.add(new CustomerProfilePanel(customer, currentUser), "PROFILE");
        } else {
            JLabel noData = new JLabel("No customer profile found. Contact admin.", SwingConstants.CENTER);
            noData.setFont(UITheme.FONT_SUBTITLE);
            noData.setForeground(UITheme.DANGER);
            contentPanel.add(noData, "HOME");
        }

        root.add(contentPanel, BorderLayout.CENTER);
        cardLayout.show(contentPanel, "HOME");
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(5, 15, 40, 230));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UITheme.ACCENT);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, UITheme.dim(58)));
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel logo = new JLabel("My Electric Portal");
        logo.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(18)));
        logo.setForeground(UITheme.ACCENT);
        logo.setIcon(new javax.swing.Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(new Font("Segoe UI Symbol", Font.BOLD, UITheme.dim(18)));
                g2.setColor(UITheme.ACCENT);
                g2.drawString("\u26A1", x, y + UITheme.dim(16));
                g2.dispose();
            }
            @Override public int getIconWidth()  { return UITheme.dim(24); }
            @Override public int getIconHeight() { return UITheme.dim(22); }
        });
        header.add(logo, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        String name = customer != null ? customer.getName() : currentUser.getUsername();

        AvatarPanel avatar = new AvatarPanel(UITheme.dim(36));
        avatar.setInitials(name);
        avatar.setRingColor(UITheme.ACCENT);
        if (currentUser.getProfilePic() != null)
            avatar.setImage(currentUser.getProfilePic());

        JButton logoutBtn = UITheme.createDangerButton("Logout");
        logoutBtn.addActionListener(e -> {
            Logic.SessionManager.logout();
            dispose();
            new LoginFrame().setVisible(true);
        });
        right.add(avatar);
        right.add(logoutBtn);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8, 18, 45, 220));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0, 180, 255, 55));
                g2.drawLine(getWidth()-1, 0, getWidth()-1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_EXPANDED, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(8, 0, 16, 0));

        // ── Toggle button ─────────────────────────────────────────────────────
        JButton toggleBtn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 15));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, UITheme.dim(13)));
                g2.setColor(UITheme.TEXT_MUTED);
                String arrow = sidebarExpanded ? "\u25C4\u25C4" : "\u25BA\u25BA";
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(arrow)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2.drawString(arrow, x, y);
                g2.dispose();
            }
        };
        toggleBtn.setOpaque(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, UITheme.dim(32)));
        toggleBtn.setPreferredSize(new Dimension(SIDEBAR_EXPANDED, UITheme.dim(32)));
        toggleBtn.setToolTipText("Collapse / Expand sidebar");
        toggleBtn.addActionListener(e -> toggleSidebar(toggleBtn));

        sidebar.add(toggleBtn);
        sidebar.add(Box.createVerticalStrut(UITheme.dim(4)));

        // Thin separator
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(new Color(0, 180, 255, 40));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(UITheme.dim(6)));

        // ── Nav items ─────────────────────────────────────────────────────────
        String[][] items = {
            {"\u2302",           "Dashboard",     "HOME",          "#00B4FF"},
            {"\u20BF",           "My Bills",      "BILLS",         "#32C864"},
            {"\uD83D\uDCB3",     "Payments",      "PAYMENTS",      "#32C864"},
            {"\u25A6",           "Readings",      "READINGS",      "#00B4FF"},
            {"\uD83D\uDCCA",     "Analytics",     "CHARTS",        "#00B4FF"},
            {"\u2605",           "AI Insights",   "AI",            "#FF6464"},
            {"\uD83D\uDD14",     "Alerts",        "NOTIFICATIONS", "#FFC800"},
            {"\uD83C\uDF9F",     "Support",       "SUPPORT",       "#FF6464"},
            {"\u25A3",           "My Profile",    "PROFILE",       "#FFA500"}
        };

        ButtonGroup bg = new ButtonGroup();
        for (String[] item : items) {
            JToggleButton btn = createNavButton(item[0], item[1], item[2], item[3]);
            navButtons.add(btn);
            bg.add(btn);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(UITheme.dim(2)));
        }
        return sidebar;
    }

    // ── Toggle collapse / expand ──────────────────────────────────────────────

    private void toggleSidebar(JButton toggleBtn) {
        sidebarExpanded = !sidebarExpanded;
        int targetWidth = sidebarExpanded ? SIDEBAR_EXPANDED : SIDEBAR_COLLAPSED;

        Timer timer = new Timer(12, null);
        int[] current = {sidebar.getPreferredSize().width};
        int step = sidebarExpanded ? UITheme.dim(8) : -UITheme.dim(8);

        timer.addActionListener(e -> {
            current[0] += step;
            boolean done = sidebarExpanded
                ? current[0] >= targetWidth
                : current[0] <= targetWidth;
            if (done) { current[0] = targetWidth; timer.stop(); }
            sidebar.setPreferredSize(new Dimension(current[0], 0));
            sidebar.revalidate();
            for (JToggleButton btn : navButtons) btn.repaint();
            toggleBtn.repaint();
            getContentPane().revalidate();
        });
        timer.start();
    }

    // ── Nav button ────────────────────────────────────────────────────────────

    private JToggleButton createNavButton(String icon, String label, String card, String iconColorHex) {
        Color iconColor = Color.decode(iconColorHex);

        JToggleButton btn = new JToggleButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Background highlight
                if (isSelected()) {
                    g2.setColor(new Color(0, 180, 255, 40));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(UITheme.ACCENT);
                    g2.fillRect(0, 0, 4, getHeight());
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 12));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                int midY = getHeight() / 2;
                boolean collapsed = !sidebarExpanded;

                // Icon — centred when collapsed, left-aligned when expanded
                int iconX = collapsed
                    ? (getWidth() - UITheme.dim(15)) / 2
                    : UITheme.dim(14);

                g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, UITheme.dim(15)));
                g2.setColor(isSelected() ? UITheme.ACCENT : iconColor);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(icon, iconX, midY + fm.getAscent() / 2 - 1);

                // Label — only when expanded
                if (!collapsed) {
                    g2.setFont(isSelected()
                        ? new Font("Segoe UI", Font.BOLD,  UITheme.dim(12))
                        : new Font("Segoe UI", Font.PLAIN, UITheme.dim(12)));
                    g2.setColor(isSelected() ? UITheme.ACCENT : UITheme.TEXT_LIGHT);
                    FontMetrics fm2 = g2.getFontMetrics();
                    g2.drawString(label, UITheme.dim(38), midY + fm2.getAscent() / 2 - 1);
                }

                g2.dispose();
            }
        };

        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, UITheme.dim(40)));
        btn.setPreferredSize(new Dimension(SIDEBAR_EXPANDED, UITheme.dim(40)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(label);   // shows label as tooltip when collapsed
        btn.addActionListener(e -> cardLayout.show(contentPanel, card));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { btn.repaint(); }
        });

        return btn;
    }
}
