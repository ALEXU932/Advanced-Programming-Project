package gui;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import database.User;

public class AdminDashboard extends JFrame {

    private final User currentUser;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JPanel sidebar;
    private boolean sidebarExpanded = true;

    private static final int SIDEBAR_EXPANDED  = UITheme.dim(200);
    private static final int SIDEBAR_COLLAPSED = UITheme.dim(52);

    // Store nav buttons to repaint on collapse/expand
    private final java.util.List<JToggleButton> navButtons = new java.util.ArrayList<>();
    private final java.util.List<String[]>      navItems   = new java.util.ArrayList<>();

    public AdminDashboard(User user) {
        this.currentUser = user;
        setTitle("Admin Dashboard - AI Electric Billing System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.max(1100, (int)(screen.width  * 0.90));
        int h = Math.max(700,  (int)(screen.height * 0.90));
        setSize(w, h);
        setMinimumSize(new Dimension(900, 600));
        setResizable(true);
        setLocationRelativeTo(null);
        buildUI();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) { repaint(); }
        });
    }

    private void buildUI() {
        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.setOverlayAlpha(0.45f);
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);

        contentPanel.add(new AdminHomePanel(currentUser),       "HOME");
        contentPanel.add(new CustomerManagementPanel(),         "CUSTOMERS");
        contentPanel.add(new MeterManagementPanel(),            "METERS");
        contentPanel.add(new MeterReadingPanel(),               "READINGS");
        contentPanel.add(new BillingPanel(),                    "BILLING");
        contentPanel.add(new PaymentPanel(),                    "PAYMENTS");
        contentPanel.add(new TariffPanel(),                     "TARIFFS");
        contentPanel.add(new AIAnalyticsPanel(),                "AI");
        contentPanel.add(new AnomalyPanel(),                    "ANOMALIES");
        contentPanel.add(new DisputesPanel(currentUser),        "DISPUTES");
        contentPanel.add(new ReportsPanel(),                    "REPORTS");
        contentPanel.add(new AuditLogPanel(),                   "AUDIT");
        contentPanel.add(new SettingsPanel(currentUser),        "SETTINGS_SYS");
        contentPanel.add(new AdminProfilePanel(currentUser),    "SETTINGS");

        root.add(contentPanel, BorderLayout.CENTER);
        cardLayout.show(contentPanel, "HOME");
    }

    public void navigateTo(String card) {
        cardLayout.show(contentPanel, card);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(5, 15, 40, 230));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UITheme.PRIMARY);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, UITheme.dim(58)));
        header.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JLabel logo = new JLabel("AI Electric Billing System");
        logo.setFont(new Font("Segoe UI", Font.BOLD, UITheme.dim(18)));
        logo.setForeground(UITheme.PRIMARY);
        logo.setIcon(new javax.swing.Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(new Font("Segoe UI Symbol", Font.BOLD, UITheme.dim(18)));
                g2.setColor(UITheme.PRIMARY);
                g2.drawString("\u26A1", x, y + UITheme.dim(16));
                g2.dispose();
            }
            public int getIconWidth()  { return UITheme.dim(24); }
            public int getIconHeight() { return UITheme.dim(22); }
        });
        header.add(logo, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        AvatarPanel avatar = new AvatarPanel(UITheme.dim(36));
        avatar.setInitials(currentUser.getUsername());
        avatar.setRingColor(UITheme.PRIMARY);
        if (currentUser.getProfilePic() != null) avatar.setImage(currentUser.getProfilePic());
        JButton logoutBtn = UITheme.createDangerButton("Logout");
        logoutBtn.addActionListener(e -> { Logic.SessionManager.logout(); dispose(); new LoginFrame().setVisible(true); });
        right.add(avatar); right.add(logoutBtn);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8, 18, 45, 225));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 140, 0, 55));
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
                    g2.setColor(new Color(255,255,255,15));
                    g2.fillRect(0,0,getWidth(),getHeight());
                }
                g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, UITheme.dim(14)));
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
        toggleBtn.setToolTipText("Collapse/Expand sidebar");
        toggleBtn.addActionListener(e -> toggleSidebar(toggleBtn));

        sidebar.add(toggleBtn);
        sidebar.add(Box.createVerticalStrut(UITheme.dim(6)));

        // Thin separator
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(new Color(255,140,0,40));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(UITheme.dim(8)));

        // ── Nav items ─────────────────────────────────────────────────────────
        String[][] items = {
            {"\u2302", "Dashboard",      "HOME",        "#FFA500"},
            {"\u25A3", "Customers",      "CUSTOMERS",   "#00B4FF"},
            {"\u26A1", "Meters",         "METERS",      "#FFA500"},
            {"\u25A6", "Meter Readings", "READINGS",    "#00B4FF"},
            {"\u20BF", "Billing",        "BILLING",     "#32C864"},
            {"\u25A4", "Payments",       "PAYMENTS",    "#00B4FF"},
            {"\u2630", "Tariffs",        "TARIFFS",     "#FFA500"},
            {"\u2605", "AI Analytics",   "AI",          "#FF6464"},
            {"\u26A0", "Anomalies",      "ANOMALIES",   "#FF6464"},
            {"\u2709", "Disputes",       "DISPUTES",    "#FF6464"},
            {"\u2750", "Reports",        "REPORTS",     "#00B4FF"},
            {"\u2393", "Audit Log",      "AUDIT",       "#00B4FF"},
            {"\u2692", "Sys Settings",   "SETTINGS_SYS","#FFA500"},
            {"\u2699", "My Profile",     "SETTINGS",    "#FFA500"}
        };

        ButtonGroup bg = new ButtonGroup();
        for (String[] item : items) {
            navItems.add(item);
            JToggleButton btn = createNavButton(item[0], item[1], item[2], item[3]);
            navButtons.add(btn);
            bg.add(btn);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(UITheme.dim(2)));
        }
        return sidebar;
    }

    // ── Toggle collapse/expand ────────────────────────────────────────────────

    private void toggleSidebar(JButton toggleBtn) {
        sidebarExpanded = !sidebarExpanded;
        int targetWidth = sidebarExpanded ? SIDEBAR_EXPANDED : SIDEBAR_COLLAPSED;

        // Animate smoothly
        Timer timer = new Timer(12, null);
        int[] current = {sidebar.getPreferredSize().width};
        int step = sidebarExpanded ? UITheme.dim(8) : -UITheme.dim(8);

        timer.addActionListener(e -> {
            current[0] += step;
            boolean done = sidebarExpanded ? current[0] >= targetWidth : current[0] <= targetWidth;
            if (done) { current[0] = targetWidth; timer.stop(); }
            sidebar.setPreferredSize(new Dimension(current[0], 0));
            sidebar.revalidate();
            // Repaint all nav buttons to show/hide labels
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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,     RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Background
                if (isSelected()) {
                    g2.setColor(new Color(255, 140, 0, 45));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(UITheme.PRIMARY);
                    g2.fillRect(0, 0, 4, getHeight());
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 12));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                int midY = getHeight() / 2;
                boolean collapsed = !sidebarExpanded;

                // Icon — centered when collapsed, left-aligned when expanded
                int iconX = collapsed
                    ? (getWidth() - UITheme.dim(15)) / 2
                    : UITheme.dim(14);

                g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, UITheme.dim(15)));
                g2.setColor(isSelected() ? UITheme.PRIMARY : iconColor);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(icon, iconX, midY + fm.getAscent() / 2 - 1);

                // Label — only when expanded
                if (!collapsed) {
                    g2.setFont(isSelected()
                        ? new Font("Segoe UI", Font.BOLD,  UITheme.dim(12))
                        : new Font("Segoe UI", Font.PLAIN, UITheme.dim(12)));
                    g2.setColor(isSelected() ? UITheme.PRIMARY : UITheme.TEXT_LIGHT);
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

        // Tooltip shows label when collapsed
        btn.setToolTipText(label);

        btn.addActionListener(e -> cardLayout.show(contentPanel, card));

        // Hover repaint
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { btn.repaint(); }
        });

        return btn;
    }
}
