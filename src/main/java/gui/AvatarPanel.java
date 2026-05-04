package gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Circular avatar component. Shows profile picture or initials fallback.
 */
public class AvatarPanel extends JPanel {

    private BufferedImage image;
    private String initials = "?";
    private int size;
    private Color ringColor = UITheme.PRIMARY;

    public AvatarPanel(int size) {
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setOpaque(false);
    }

    public void setImage(String filePath) {
        if (filePath == null || filePath.isEmpty()) { image = null; repaint(); return; }
        try {
            File f = new File(filePath);
            if (f.exists()) {
                image = ImageIO.read(f);
                repaint();
            }
        } catch (Exception e) { image = null; }
    }

    public void setInitials(String name) {
        if (name == null || name.isEmpty()) { initials = "?"; return; }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        else
            initials = name.substring(0, Math.min(2, name.length())).toUpperCase();
        repaint();
    }

    public void setRingColor(Color c) { this.ringColor = c; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);

        int pad  = 3;
        int inner = size - pad * 2;
        Ellipse2D clip = new Ellipse2D.Float(pad, pad, inner, inner);

        if (image != null) {
            // Draw image clipped to circle
            g2.setClip(clip);
            // Scale image to fill circle
            int iw = image.getWidth(), ih = image.getHeight();
            int drawSize = inner;
            int sx = 0, sy = 0, sw = iw, sh = ih;
            if (iw > ih) { sw = ih; sx = (iw - ih) / 2; }
            else         { sh = iw; sy = (ih - iw) / 2; }
            g2.drawImage(image, pad, pad, pad + drawSize, pad + drawSize, sx, sy, sx+sw, sy+sh, null);
            g2.setClip(null);
        } else {
            // Gradient background with initials
            GradientPaint gp = new GradientPaint(0, 0, new Color(20, 50, 110),
                size, size, new Color(10, 30, 70));
            g2.setPaint(gp);
            g2.fill(clip);

            g2.setColor(new Color(255, 255, 255, 30));
            g2.fill(new Ellipse2D.Float(pad + inner/4f, pad + inner/4f, inner/2f, inner/2f));

            g2.setFont(new Font("Segoe UI", Font.BOLD, (int)(inner * 0.38)));
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (size - fm.stringWidth(initials)) / 2;
            int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(initials, tx, ty);
        }

        // Colored ring
        g2.setColor(ringColor);
        g2.setStroke(new BasicStroke(2.5f));
        g2.draw(new Ellipse2D.Float(pad, pad, inner, inner));

        // Subtle outer glow
        g2.setColor(new Color(ringColor.getRed(), ringColor.getGreen(), ringColor.getBlue(), 50));
        g2.setStroke(new BasicStroke(4f));
        g2.draw(new Ellipse2D.Float(1, 1, size-2, size-2));

        g2.dispose();
    }
}
