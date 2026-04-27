package utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ProfilePicUtils {

    private static final String PROFILE_DIR = "resources/profiles/";
    private static final int    TARGET_SIZE  = 256; // save at 256x256

    /** Opens file chooser, copies image to profiles dir, returns saved path or null. */
    public static String chooseAndSave(Component parent, int userId) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Profile Picture");
        fc.setFileFilter(new FileNameExtensionFilter(
            "Image Files (*.jpg, *.jpeg, *.png, *.gif)", "jpg","jpeg","png","gif"));
        fc.setAcceptAllFileFilterUsed(false);

        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null;

        File src = fc.getSelectedFile();
        if (!src.exists()) return null;

        // Ensure profiles directory exists
        File dir = new File(PROFILE_DIR);
        if (!dir.exists()) dir.mkdirs();

        // Resize and save as PNG
        try {
            BufferedImage original = ImageIO.read(src);
            if (original == null) {
                JOptionPane.showMessageDialog(parent, "Cannot read image file.", "Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            BufferedImage resized = resizeSquare(original, TARGET_SIZE);
            String destPath = PROFILE_DIR + "user_" + userId + ".png";
            ImageIO.write(resized, "PNG", new File(destPath));
            return destPath;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, "Failed to save image: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /** Resize image to a square of given size (center-crop). */
    private static BufferedImage resizeSquare(BufferedImage src, int size) {
        int w = src.getWidth(), h = src.getHeight();
        int cropSize = Math.min(w, h);
        int x = (w - cropSize) / 2, y = (h - cropSize) / 2;
        BufferedImage cropped = src.getSubimage(x, y, cropSize, cropSize);

        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(cropped, 0, 0, size, size, null);
        g2.dispose();
        return result;
    }

    /** Delete profile picture file for a user. */
    public static void delete(int userId) {
        File f = new File(PROFILE_DIR + "user_" + userId + ".png");
        if (f.exists()) f.delete();
    }

    /** Rename temp pic (saved with id -1) to real user id after insert. */
    public static String renameTempPic(int tempId, int realId) {
        File temp = new File(PROFILE_DIR + "user_" + tempId + ".png");
        if (!temp.exists()) return null;
        File dest = new File(PROFILE_DIR + "user_" + realId + ".png");
        if (temp.renameTo(dest)) return dest.getPath();
        // fallback: copy then delete
        try {
            java.nio.file.Files.copy(temp.toPath(), dest.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            temp.delete();
            return dest.getPath();
        } catch (java.io.IOException e) { return null; }
    }
}
