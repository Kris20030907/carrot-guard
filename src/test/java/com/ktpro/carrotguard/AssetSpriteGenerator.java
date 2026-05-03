package com.ktpro.carrotguard;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AssetSpriteGenerator {
    private static final int SIZE = 96;

    private AssetSpriteGenerator() {
    }

    public static void main(String[] args) {
        Path outputDirectory = args.length > 0 ? Path.of(args[0]) : Path.of("src/main/resources/assets");
        try {
            Files.createDirectories(outputDirectory);
            write(outputDirectory, AssetKey.GRASS, grassTile());
            write(outputDirectory, AssetKey.PATH, pathTile());
            write(outputDirectory, AssetKey.CARROT, carrot());
            write(outputDirectory, AssetKey.TOWER_BASIC, tower(new Color(34, 89, 150), new Color(23, 83, 121)));
            write(outputDirectory, AssetKey.TOWER_SLOW, tower(new Color(20, 151, 167), new Color(42, 124, 129)));
            write(outputDirectory, AssetKey.TOWER_SPLASH, tower(new Color(172, 76, 55), new Color(128, 53, 42)));
            write(outputDirectory, AssetKey.PROJECTILE_BASIC, projectile(new Color(92, 164, 244), new Color(28, 82, 161)));
            write(outputDirectory, AssetKey.PROJECTILE_SLOW, projectile(new Color(63, 220, 225), new Color(13, 135, 154)));
            write(outputDirectory, AssetKey.PROJECTILE_SPLASH, projectile(new Color(255, 181, 82), new Color(172, 76, 55)));
            write(outputDirectory, AssetKey.ENEMY_NORMAL, enemy(new Color(163, 75, 57), new Color(107, 42, 38), "N"));
            write(outputDirectory, AssetKey.ENEMY_FAST, enemy(new Color(194, 103, 44), new Color(126, 61, 30), "F"));
            write(outputDirectory, AssetKey.ENEMY_TANK, enemy(new Color(96, 92, 106), new Color(48, 45, 63), "T"));
            write(outputDirectory, AssetKey.OBSTACLE_CRATE, crate());
            write(outputDirectory, AssetKey.OBSTACLE_ROCK, rock());
            System.out.println("Generated PNG assets in " + outputDirectory.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("could not generate sprite assets", e);
        }
    }

    private static BufferedImage grassTile() {
        BufferedImage image = canvas();
        Graphics2D g = image.createGraphics();
        setup(g);
        g.setPaint(new GradientPaint(0, 0, new Color(133, 190, 111), SIZE, SIZE, new Color(105, 164, 88)));
        g.fillRect(0, 0, SIZE, SIZE);
        g.setColor(new Color(89, 143, 77, 94));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(0, 0, SIZE - 1, SIZE - 1);
        g.setColor(new Color(204, 231, 146, 80));
        for (int i = 0; i < 9; i++) {
            int x = 11 + i * 9;
            int y = 16 + Math.floorMod(i * 17, 56);
            g.drawLine(x, y + 8, x + 4, y);
            g.drawLine(x + 4, y + 8, x + 8, y + 2);
        }
        g.dispose();
        return image;
    }

    private static BufferedImage pathTile() {
        BufferedImage image = canvas();
        Graphics2D g = image.createGraphics();
        setup(g);
        g.setPaint(new GradientPaint(0, 0, new Color(232, 198, 133), SIZE, SIZE, new Color(205, 162, 95)));
        g.fillRect(0, 0, SIZE, SIZE);
        g.setColor(new Color(154, 110, 62, 62));
        for (int i = 0; i < 11; i++) {
            int x = 8 + Math.floorMod(i * 23, 74);
            int y = 9 + Math.floorMod(i * 31, 76);
            g.fillOval(x, y, 5 + i % 3, 3 + i % 2);
        }
        g.setColor(new Color(173, 132, 79, 112));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(0, 0, SIZE - 1, SIZE - 1);
        g.dispose();
        return image;
    }

    private static BufferedImage carrot() {
        BufferedImage image = canvas();
        Graphics2D g = image.createGraphics();
        setup(g);
        g.setPaint(new GradientPaint(31, 28, new Color(255, 189, 79), 66, 78, new Color(236, 92, 28)));
        g.fillOval(27, 25, 42, 56);
        g.setColor(new Color(174, 68, 32, 120));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(35, 45, 26, 11, 190, 130);
        g.drawArc(35, 59, 24, 10, 190, 120);
        g.setColor(new Color(255, 232, 150, 150));
        g.fillOval(37, 35, 10, 20);
        g.setColor(new Color(67, 152, 67));
        g.fillOval(27, 12, 26, 23);
        g.setColor(new Color(43, 122, 58));
        g.fillOval(44, 9, 28, 24);
        g.setColor(new Color(88, 176, 77));
        g.fillOval(38, 5, 20, 25);
        g.dispose();
        return image;
    }

    private static BufferedImage tower(Color body, Color barrel) {
        BufferedImage image = canvas();
        Graphics2D g = image.createGraphics();
        setup(g);
        g.setPaint(new GradientPaint(25, 34, brighten(body, 34), 69, 78, darken(body, 31)));
        g.fillOval(24, 33, 48, 46);
        g.setColor(darken(barrel, 6));
        g.setStroke(new BasicStroke(5f));
        g.drawOval(24, 33, 48, 46);
        g.setPaint(new GradientPaint(40, 12, brighten(barrel, 30), 56, 43, darken(barrel, 18)));
        g.fillRoundRect(40, 13, 16, 35, 8, 8);
        g.setColor(new Color(255, 246, 194, 145));
        g.fillOval(34, 38, 28, 15);
        g.dispose();
        return image;
    }

    private static BufferedImage projectile(Color light, Color dark) {
        BufferedImage image = canvas();
        Graphics2D g = image.createGraphics();
        setup(g);
        g.setPaint(new GradientPaint(34, 32, light, 62, 66, dark));
        g.fillOval(29, 29, 38, 38);
        g.setColor(new Color(255, 255, 230, 150));
        g.fillOval(38, 36, 12, 12);
        g.setColor(new Color(255, 255, 255, 55));
        g.setStroke(new BasicStroke(4f));
        g.drawOval(28, 28, 40, 40);
        g.dispose();
        return image;
    }

    private static BufferedImage enemy(Color body, Color border, String label) {
        BufferedImage image = canvas();
        Graphics2D g = image.createGraphics();
        setup(g);
        g.setPaint(new GradientPaint(23, 22, brighten(body, 34), 73, 76, darken(body, 22)));
        g.fill(new Ellipse2D.Double(20, 20, 56, 56));
        g.setColor(border);
        g.setStroke(new BasicStroke(5f));
        g.drawOval(20, 20, 56, 56);
        g.setColor(new Color(255, 225, 170, 120));
        g.fillOval(34, 31, 10, 12);
        g.fillOval(52, 31, 10, 12);
        g.setColor(new Color(255, 250, 235));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
        drawCentered(g, label, 48, 61);
        g.dispose();
        return image;
    }

    private static BufferedImage crate() {
        BufferedImage image = canvas();
        Graphics2D g = image.createGraphics();
        setup(g);
        g.setPaint(new GradientPaint(22, 22, new Color(181, 113, 58), 76, 76, new Color(128, 75, 39)));
        g.fillRoundRect(22, 22, 52, 52, 10, 10);
        g.setColor(new Color(90, 60, 35));
        g.setStroke(new BasicStroke(5f));
        g.drawRoundRect(22, 22, 52, 52, 10, 10);
        g.setStroke(new BasicStroke(4f));
        g.drawLine(26, 38, 70, 38);
        g.drawLine(26, 58, 70, 58);
        g.drawLine(39, 26, 39, 70);
        g.drawLine(58, 26, 58, 70);
        g.dispose();
        return image;
    }

    private static BufferedImage rock() {
        BufferedImage image = canvas();
        Graphics2D g = image.createGraphics();
        setup(g);
        int[] xs = { 21, 35, 59, 76, 68, 45, 23 };
        int[] ys = { 56, 22, 24, 42, 72, 78, 67 };
        g.setPaint(new GradientPaint(24, 23, new Color(138, 145, 129), 74, 76, new Color(89, 96, 85)));
        g.fillPolygon(xs, ys, xs.length);
        g.setColor(new Color(62, 68, 60));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolygon(xs, ys, xs.length);
        g.setColor(new Color(255, 255, 255, 88));
        g.setStroke(new BasicStroke(3f));
        g.drawLine(36, 32, 54, 29);
        g.dispose();
        return image;
    }

    private static BufferedImage canvas() {
        return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private static void setup(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static void write(Path outputDirectory, AssetKey key, BufferedImage image) throws IOException {
        ImageIO.write(image, "png", outputDirectory.resolve(key.getFileName()).toFile());
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int baselineY) {
        int x = centerX - g.getFontMetrics().stringWidth(text) / 2;
        g.drawString(text, x, baselineY);
    }

    private static Color brighten(Color color, int amount) {
        return new Color(
                Math.min(255, color.getRed() + amount),
                Math.min(255, color.getGreen() + amount),
                Math.min(255, color.getBlue() + amount),
                color.getAlpha()
        );
    }

    private static Color darken(Color color, int amount) {
        return new Color(
                Math.max(0, color.getRed() - amount),
                Math.max(0, color.getGreen() - amount),
                Math.max(0, color.getBlue() - amount),
                color.getAlpha()
        );
    }
}
