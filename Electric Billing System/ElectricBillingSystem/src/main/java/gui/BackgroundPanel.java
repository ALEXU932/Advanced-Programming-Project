package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class BackgroundPanel extends JPanel {
    private BufferedImage backgroundImage;
    private float overlayAlpha = 0.55f; // dark overlay for readability

    public BackgroundPanel() {
        loadBackground();
        setLayout(new BorderLayout());
    }

    public BackgroundPanel(LayoutManager layout) {
        loadBackground();
        setLayout(layout);
    }

    private void loadBackground() {
        try {
            File f = new File("resources/background.jpg");
            if (f.exists()) {
                backgroundImage = ImageIO.read(f);
            }
        } catch (IOException e) {
            backgroundImage = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            // Fallback gradient
            GradientPaint gp = new GradientPaint(0, 0, new Color(10, 20, 50),
                getWidth(), getHeight(), new Color(20, 60, 100));
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
        // Dark overlay for text readability
        g2d.setColor(new Color(0, 0, 0, (int)(overlayAlpha * 255)));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.dispose();
    }

    public void setOverlayAlpha(float alpha) {
        this.overlayAlpha = alpha;
        repaint();
    }
}
