package activesupport.visual;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Local pixel comparison used to decide whether Bedrock needs to see a page, and to crop the
 * changed region so only that area is sent.
 */
public final class ImageDiff {

    private static final int CROP_PADDING = 48;

    private final BufferedImage diffImage;
    private final Rectangle changedArea;
    private final double changedRatio;
    private final int width;
    private final int height;

    private ImageDiff(BufferedImage diffImage, Rectangle changedArea, double changedRatio, int width, int height) {
        this.diffImage = diffImage;
        this.changedArea = changedArea;
        this.changedRatio = changedRatio;
        this.width = width;
        this.height = height;
    }

    public static ImageDiff compare(BufferedImage baseline, BufferedImage candidate, int tolerance) {
        int w = Math.max(baseline.getWidth(), candidate.getWidth());
        int h = Math.max(baseline.getHeight(), candidate.getHeight());
        BufferedImage diff = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int minX = w, minY = h, maxX = -1, maxY = -1;
        long changed = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean inBase = x < baseline.getWidth() && y < baseline.getHeight();
                boolean inCand = x < candidate.getWidth() && y < candidate.getHeight();
                int c = inCand ? candidate.getRGB(x, y) : 0xFFFFFF;
                boolean different = inBase != inCand || exceeds(baseline.getRGB(x, y), c, tolerance);
                if (different) {
                    changed++;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    diff.setRGB(x, y, 0xE5004C);
                } else {
                    diff.setRGB(x, y, faded(c));
                }
            }
        }
        Rectangle area = changed == 0 ? new Rectangle() : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        return new ImageDiff(diff, area, (double) changed / ((long) w * h), w, h);
    }

    private static boolean exceeds(int a, int b, int tolerance) {
        return Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF)) > tolerance
                || Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF)) > tolerance
                || Math.abs((a & 0xFF) - (b & 0xFF)) > tolerance;
    }

    private static int faded(int rgb) {
        int grey = (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3;
        int light = 200 + grey * 55 / 255;
        return (light << 16) | (light << 8) | light;
    }

    public BufferedImage getDiffImage() {
        return diffImage;
    }

    public Rectangle getChangedArea() {
        return changedArea;
    }

    public double getChangedRatio() {
        return changedRatio;
    }

    /** Changed area plus padding, clamped to the compared canvas. */
    public Rectangle paddedArea() {
        if (changedArea.isEmpty()) {
            return new Rectangle(0, 0, width, height);
        }
        int x = Math.max(0, changedArea.x - CROP_PADDING);
        int y = Math.max(0, changedArea.y - CROP_PADDING);
        int right = Math.min(width, changedArea.x + changedArea.width + CROP_PADDING);
        int bottom = Math.min(height, changedArea.y + changedArea.height + CROP_PADDING);
        return new Rectangle(x, y, right - x, bottom - y);
    }

    /** Crops {@code image} to {@code area}, filling any part outside the image with white. */
    public static BufferedImage crop(BufferedImage image, Rectangle area) {
        BufferedImage out = new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, area.width, area.height);
        g.drawImage(image, -area.x, -area.y, null);
        g.dispose();
        return out;
    }

    public static BufferedImage fit(BufferedImage image, int maxEdge) {
        int longest = Math.max(image.getWidth(), image.getHeight());
        if (longest <= maxEdge) {
            return image;
        }
        double scale = (double) maxEdge / longest;
        int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image.getScaledInstance(w, h, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return out;
    }

    public static byte[] png(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
