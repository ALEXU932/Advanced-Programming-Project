package gui;

import javax.swing.*;
import java.awt.*;

/**
 * Reusable professional dialog base with dark header, body, and footer.
 */
public class FormDialog extends JDialog {

    protected JPanel body;
    protected JPanel footer;
    protected JLabel statusLabel;
    private   GridBagConstraints gbc;
    private   int currentRow = 0;

    public FormDialog(Frame parent, String title, String subtitle, int width, int height) {
        super(parent, title, true);
        setSize(width, height);
        setLocationRelativeTo(parent);
        setResizable(true);

        BackgroundPanel root = new BackgroundPanel(new BorderLayout());
        root.setOverlayAlpha(0.72f);
        setContentPane(root);

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8, 18, 50, 245));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UITheme.PRIMARY);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 58));
        header.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setForeground(UITheme.PRIMARY);
        header.add(titleLbl, BorderLayout.WEST);

        if (subtitle != null) {
            JLabel subLbl = new JLabel(subtitle);
            subLbl.setFont(UITheme.FONT_SMALL);
            subLbl.setForeground(UITheme.TEXT_MUTED);
            header.add(subLbl, BorderLayout.EAST);
        }
        root.add(header, BorderLayout.NORTH);

        // ── Body ──────────────────────────────────────────────────────────────
        body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(18, 28, 8, 28));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(4, 6, 4, 6);

        // Status label always at bottom of body
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.DANGER);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        root.add(scroll, BorderLayout.CENTER);

        // ── Footer ────────────────────────────────────────────────────────────
        footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8, 18, 50, 210));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(40, 60, 110));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        footer.setOpaque(false);
        root.add(footer, BorderLayout.SOUTH);
    }

    /** Add a full-width label + field pair (2 rows) */
    public void addField(String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = currentRow++; gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 6, 1, 6);
        body.add(fieldLabel(label), gbc);
        gbc.gridy = currentRow++; gbc.insets = new Insets(0, 6, 6, 6);
        body.add(field, gbc);
    }

    /** Add two label+field pairs side by side */
    public void addFieldRow(String lbl1, JComponent f1, String lbl2, JComponent f2) {
        gbc.gridwidth = 1; gbc.insets = new Insets(8, 6, 1, 6);
        gbc.gridx = 0; gbc.gridy = currentRow;
        body.add(fieldLabel(lbl1), gbc);
        gbc.gridx = 1;
        body.add(fieldLabel(lbl2), gbc);
        currentRow++;
        gbc.insets = new Insets(0, 6, 6, 6);
        gbc.gridx = 0; gbc.gridy = currentRow;
        body.add(f1, gbc);
        gbc.gridx = 1;
        body.add(f2, gbc);
        currentRow++;
    }

    /** Add status label */
    public void addStatus() {
        gbc.gridx = 0; gbc.gridy = currentRow++; gbc.gridwidth = 2;
        gbc.insets = new Insets(6, 6, 2, 6);
        body.add(statusLabel, gbc);
    }

    /** Add a separator line */
    public void addSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(40, 70, 120));
        gbc.gridx = 0; gbc.gridy = currentRow++; gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 0, 8, 0);
        body.add(sep, gbc);
    }

    public JButton addCancelButton() {
        JButton btn = new JButton("Cancel");
        btn.setFont(UITheme.FONT_BUTTON);
        btn.setForeground(UITheme.TEXT_LIGHT);
        btn.setBackground(new Color(40, 60, 100));
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(110, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> dispose());
        footer.add(btn);
        return btn;
    }

    public JButton addSaveButton(String text) {
        JButton btn = UITheme.createPrimaryButton(text);
        btn.setPreferredSize(new Dimension(160, 38));
        footer.add(btn);
        return btn;
    }

    public void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setForeground(error ? UITheme.DANGER : UITheme.SUCCESS);
    }

    public static JTextField makeField(String value) {
        JTextField f = UITheme.createTextField();
        f.setText(value != null ? value : "");
        f.setPreferredSize(new Dimension(0, 38));
        return f;
    }

    public static JPasswordField makePassField() {
        JPasswordField f = UITheme.createPasswordField();
        f.setPreferredSize(new Dimension(0, 38));
        return f;
    }

    public static JComboBox<Object> makeCombo() {
        JComboBox<Object> cb = UITheme.createComboBox();
        cb.setPreferredSize(new Dimension(0, 38));
        return cb;
    }

    public static JComboBox<String> makeStringCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(UITheme.FONT_BODY);
        cb.setBackground(new Color(20, 40, 80));
        cb.setForeground(UITheme.TEXT_WHITE);
        cb.setBorder(BorderFactory.createLineBorder(UITheme.ACCENT, 1));
        cb.setPreferredSize(new Dimension(0, 38));
        return cb;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_LABEL);
        l.setForeground(UITheme.TEXT_LIGHT);
        return l;
    }
}
