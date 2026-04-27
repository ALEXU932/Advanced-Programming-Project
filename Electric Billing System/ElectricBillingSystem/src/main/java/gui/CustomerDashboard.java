package gui;

import db.DatabaseManager;
import models.User;
import models.Customer;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CustomerDashboard extends JFrame {
    private final User currentUser;
    private Customer customer;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    // ── Multi-language support ────────────────────────────────────────────────
    private static String currentLang = "EN";
    private static final Map<String, Map<String, String>> LANG = new HashMap<>();
    static {
        Map<String, String> en = new HashMap<>();
        en.put("portal",      "My Electric Portal");
        en.put("dashboard",   "Dashboard");
        en.put("bills",       "My Bills");
        en.put("payments",    "Payments");
        en.put("readings",    "Readings");
        en.put("ai",          "AI Insights");
        en.put("support",     "Support");
        en.put("notif",       "Notifications");
        en.put("profile",     "My Profile");
        en.put("logout",      "Logout");
        LANG.put("EN", en);

        Map<String, String> am = new HashMap<>();
        am.put("portal",      "የኔ ኤሌክትሪክ ፖርታል");
        am.put("dashboard",   "ዳሽቦርድ");
        am.put("bills",       "ሂሳቦቼ");
        am.put("payments",    "ክፍያዎች");
        am.put("readings",    "ንባቦች");
        am.put("ai",          "AI ትንታኔ");
        am.put("support",     "ድጋፍ");
        am.put("notif",       "ማሳወቂያዎች");
        am.put("profile",     "መገለጫዬ");
        am.put("logout",      "ውጣ");
        LANG.put("AM", am);

        Map<String, String> ar = new HashMap<>();
        ar.put("portal",      "بوابتي الكهربائية");
        ar.put("dashboard",   "لوحة التحكم");
        ar.put("bills",       "فواتيري");
        ar.put("payments",    "المدفوعات");
        ar.put("readings",    "القراءات");
        ar.put("ai",          "رؤى الذكاء الاصطناعي");
        ar.put("support",     "الدعم");
        ar.put("notif",       "الإشعارات");
        ar.put("profile",     "ملفي الشخصي");
        ar.put("logout",      "تسجيل الخروج");
        LANG.put("AR", ar);
    }

    private static String t(String key) {
        Map<String, String> map = LANG.getOrDefault(currentLang, LANG.get("EN"));
        return map.getOrDefault(key, LANG.get("EN").getOrDefault(key, key));
    }

    public CustomerDashboard(User user) {
        this.currentUser = user;
        setTitle("Customer Portal - AI Electric Billing System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.max(1100, (int)(screen.width  * 0.88));
        int h = Math.max(700,  (int)(screen.height * 0.88));
        setSize(w, h);
        setMinimumSize(new Dimension(950, 650));
        setResizable(true);
        setLocationRelativeTo(null);
        loadCustomer();
        buildUI();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) { repaint(); }
        });
    }

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
        } catch (SQLException e) { e.printStackTrace(); }
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

        if (customer != null) {
            contentPanel.add(new CustomerHomePanel(customer),          "HOME");
            contentPanel.add(new CustomerBillsPanel(customer),         "BILLS");
            contentPanel.add(new CustomerPaymentPanel(customer),       "PAYMENTS");
            contentPanel.add(new CustomerReadingsPanel(customer),      "READINGS");
            contentPanel.add(new CustomerAIPanel(customer),            "AI");
            contentPanel.add(new CustomerSupportPanel(customer),       "SUPPORT");
            contentPanel.add(new CustomerNotificationsPanel(customer), "NOTIF");
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

        JLabel logo = new JLabel(t("portal"));
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setForeground(UITheme.ACCENT);
        logo.setIcon(new javax.swing.Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setFont(new Font("Segoe UI Symbol", Font.BOLD, 20));
                g2.setColor(UITheme.ACCENT);
                g2.drawString("\u26A1", x, y + 18);
                g2.dispose();
            }
            public int getIconWidth()  { return 26; }
            public int getIconHeight() { return 24; }
        });
        header.add(logo, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        String name = customer != null ? customer.getName() : currentUser.getUsername();

        AvatarPanel avatar = new AvatarPanel(38);
        avatar.setInitials(name);
        avatar.setRingColor(UITheme.ACCENT);
        if (currentUser.getProfilePic() != null)
            avatar.setImage(currentUser.getProfilePic());

        // Language selector
        JComboBox<String> langCb = new JComboBox<>(new String[]{"EN", "AM", "AR"});
        langCb.setSelectedItem(currentLang);
        langCb.setFont(UITheme.FONT_SMALL);
        langCb.setBackground(new Color(20, 40, 80));
        langCb.setForeground(UITheme.TEXT_WHITE);
        langCb.setPreferredSize(new Dimension(60, 28));
        langCb.setToolTipText("Language / ቋንቋ / اللغة");
        langCb.addActionListener(e -> {
            currentLang = (String) langCb.getSelectedItem();
            // Rebuild UI with new language
            getContentPane().removeAll();
            buildUI();
            revalidate();
            repaint();
        });

        JButton logoutBtn = UITheme.createDangerButton(t("logout"));
        logoutBtn.addActionListener(e -> {
            utils.SessionManager.logout();
            dispose();
            new LoginFrame().setVisible(true);
        });
        right.add(langCb);
        right.add(avatar);
        right.add(logoutBtn);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8, 18, 45, 220));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0, 180, 255, 60));
                g2.drawLine(getWidth()-1, 0, getWidth()-1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(UITheme.dim(195), 0));
        sidebar.setMinimumSize(new Dimension(UITheme.dim(160), 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // icon, label-key, card-name, color
        // Single-codepoint Segoe UI Symbol chars — no surrogate pairs, all render correctly
        String[][] items = {
            {"\u2302", "dashboard", "HOME",     "#00B4FF"},  // ⌂ house
            {"\u2261", "bills",     "BILLS",    "#32C864"},  // ≡ list
            {"\u25CB", "payments",  "PAYMENTS", "#FFA500"},  // ○ circle/coin
            {"\u25A6", "readings",  "READINGS", "#00B4FF"},  // ▦ grid
            {"\u2605", "ai",        "AI",       "#FF6464"},  // ★ star
            {"\u2709", "support",   "SUPPORT",  "#00B4FF"},  // ✉ envelope
            {"\u25C6", "notif",     "NOTIF",    "#FFD700"},  // ◆ diamond
            {"\u25A3", "profile",   "PROFILE",  "#FFA500"}   // ▣ square
        };

        ButtonGroup bg = new ButtonGroup();
        for (String[] item : items) {
            JToggleButton btn = createNavButton(item[0], t(item[1]), item[2], item[3]);
            bg.add(btn);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(2));
        }
        return sidebar;
    }

    private JToggleButton createNavButton(String icon, String label, String card, String iconColorHex) {
        Color iconColor = Color.decode(iconColorHex);
        JToggleButton btn = new JToggleButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
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
                g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, UITheme.dim(14)));
                g2.setColor(isSelected() ? UITheme.ACCENT : iconColor);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(icon, 18, midY + fm.getAscent() / 2 - 2);
                g2.setFont(isSelected()
                    ? new Font("Segoe UI", Font.BOLD,  UITheme.dim(11))
                    : new Font("Segoe UI", Font.PLAIN, UITheme.dim(11)));
                g2.setColor(isSelected() ? UITheme.ACCENT : UITheme.TEXT_LIGHT);
                FontMetrics fm2 = g2.getFontMetrics();
                g2.drawString(label, UITheme.dim(40), midY + fm2.getAscent() / 2 - 2);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(UITheme.dim(200), UITheme.dim(40)));
        btn.setPreferredSize(new Dimension(UITheme.dim(200), UITheme.dim(40)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> cardLayout.show(contentPanel, card));
        return btn;
    }
}
