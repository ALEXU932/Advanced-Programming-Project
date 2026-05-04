package gui;

import javax.swing.*;
import java.awt.*;

public class UITheme {
    // Color palette
    public static final Color PRIMARY       = new Color(255, 140, 0);
    public static final Color PRIMARY_DARK  = new Color(200, 100, 0);
    public static final Color ACCENT        = new Color(0, 180, 255);
    public static final Color BG_DARK       = new Color(10, 20, 50, 220);
    public static final Color BG_CARD       = new Color(15, 30, 70, 200);
    public static final Color BG_CARD2      = new Color(20, 40, 90, 210);
    public static final Color TEXT_WHITE    = Color.WHITE;
    public static final Color TEXT_LIGHT    = new Color(200, 220, 255);
    public static final Color TEXT_MUTED    = new Color(150, 170, 200);
    public static final Color SUCCESS       = new Color(50, 200, 100);
    public static final Color WARNING       = new Color(255, 200, 0);
    public static final Color DANGER        = new Color(255, 80, 80);
    public static final Color TABLE_HEADER  = new Color(20, 50, 100, 230);
    public static final Color TABLE_ROW1    = new Color(15, 35, 75, 200);
    public static final Color TABLE_ROW2    = new Color(20, 45, 90, 200);

    // ── Responsive font scale ─────────────────────────────────────────────────
    // Base design is for 1920×1080. Scale down proportionally for smaller screens.
    private static final float SCALE;
    static {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        // Use width as primary scale factor, clamped between 0.65 and 1.0
        float raw = screen.width / 1920f;
        SCALE = Math.max(0.65f, Math.min(1.0f, raw));
    }

    /** Scale a font size proportionally to screen width. */
    private static int fs(int base) {
        return Math.max(9, Math.round(base * SCALE));
    }

    // ── Scaled fonts ──────────────────────────────────────────────────────────
    public static final Font FONT_TITLE    = new Font("Segoe UI", Font.BOLD,  fs(24));
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD,  fs(15));
    public static final Font FONT_BODY     = new Font("Segoe UI", Font.PLAIN, fs(12));
    public static final Font FONT_SMALL    = new Font("Segoe UI", Font.PLAIN, fs(10));
    public static final Font FONT_BUTTON   = new Font("Segoe UI", Font.BOLD,  fs(12));
    public static final Font FONT_LABEL    = new Font("Segoe UI", Font.BOLD,  fs(11));
    public static final Font FONT_TABLE    = new Font("Segoe UI", Font.PLAIN, fs(11));

    // ── Scaled dimensions ─────────────────────────────────────────────────────
    /** Scale a pixel dimension proportionally. */
    public static int dim(int base) {
        return Math.max(1, Math.round(base * SCALE));
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(dim(8), dim(16), dim(8), dim(16)));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY_DARK); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(PRIMARY); }
        });
        return btn;
    }

    public static JButton createAccentButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(dim(6), dim(12), dim(6), dim(12)));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(new Color(0, 140, 200)); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(ACCENT); }
        });
        return btn;
    }

    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(DANGER);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(dim(6), dim(12), dim(6), dim(12)));
        return btn;
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    public static JTextField createTextField() {
        JTextField tf = new JTextField();
        tf.setFont(FONT_BODY);
        tf.setBackground(new Color(20, 40, 80, 200));
        tf.setForeground(TEXT_WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1),
            BorderFactory.createEmptyBorder(dim(5), dim(8), dim(5), dim(8))));
        return tf;
    }

    public static JPasswordField createPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(FONT_BODY);
        pf.setBackground(new Color(20, 40, 80, 200));
        pf.setForeground(TEXT_WHITE);
        pf.setCaretColor(Color.WHITE);
        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1),
            BorderFactory.createEmptyBorder(dim(5), dim(8), dim(5), dim(8))));
        return pf;
    }

    public static JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_LIGHT);
        return lbl;
    }

    // ── Card ──────────────────────────────────────────────────────────────────

    public static JPanel createCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, dim(6))) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(255, 140, 0, 80));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        int p = dim(12);
        card.setBorder(BorderFactory.createEmptyBorder(p, p, p, p));
        if (title != null && !title.isEmpty()) {
            JLabel titleLbl = new JLabel(title);
            titleLbl.setFont(FONT_SUBTITLE);
            titleLbl.setForeground(PRIMARY);
            card.add(titleLbl, BorderLayout.NORTH);
        }
        return card;
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    public static void styleTable(JTable table) {
        table.setFont(FONT_TABLE);
        table.setForeground(TEXT_WHITE);
        table.setBackground(TABLE_ROW1);
        table.setGridColor(new Color(40, 70, 120));
        table.setRowHeight(dim(26));
        table.setSelectionBackground(new Color(255, 140, 0, 120));
        table.setSelectionForeground(Color.WHITE);
        table.setShowGrid(true);
        table.getTableHeader().setFont(FONT_LABEL);
        table.getTableHeader().setBackground(TABLE_HEADER);
        table.getTableHeader().setForeground(PRIMARY);
        table.getTableHeader().setBorder(BorderFactory.createLineBorder(new Color(40, 80, 150)));
        table.setOpaque(false);
        ((javax.swing.table.DefaultTableCellRenderer) table.getDefaultRenderer(Object.class)).setOpaque(true);
    }

    public static JScrollPane createScrollPane(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createLineBorder(new Color(40, 80, 150), 1));
        sp.getViewport().setBackground(TABLE_ROW1);
        return sp;
    }

    public static JComboBox<Object> createComboBox() {
        JComboBox<Object> cb = new JComboBox<>();
        cb.setFont(FONT_BODY);
        cb.setBackground(new Color(20, 40, 80));
        cb.setForeground(TEXT_WHITE);
        cb.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        return cb;
    }
}
