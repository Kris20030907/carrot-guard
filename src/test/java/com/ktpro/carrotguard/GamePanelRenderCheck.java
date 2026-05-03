package com.ktpro.carrotguard;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GamePanelRenderCheck {
    private GamePanelRenderCheck() {
    }

    public static void main(String[] args) {
        GameProgress progress = new GameProgress(tempProgressPath());
        GamePanel panel = new GamePanel(progress);
        panel.setSize(GamePanel.WIDTH, GamePanel.HEIGHT);
        if (!panel.isShowingMenu()) {
            throw new IllegalStateException("panel should start on the main menu");
        }
        if (!panel.isLevelUnlocked(1) || panel.isLevelUnlocked(2)) {
            throw new IllegalStateException("fresh progress should only unlock level one");
        }
        if (panel.isLevelUnlocked(3)) {
            throw new IllegalStateException("fresh progress should lock level three");
        }

        BufferedImage image = new BufferedImage(GamePanel.WIDTH, GamePanel.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();

        int distinctSamples = countDistinctSamples(image);
        if (distinctSamples < 8) {
            throw new IllegalStateException("rendered menu should contain varied visual content");
        }

        progress.recordVictory(1, 2, true);
        if (!panel.isLevelUnlocked(2) || panel.getBestStars(1) != 2) {
            throw new IllegalStateException("recorded progress should unlock level two and keep stars");
        }
        if (panel.isLevelUnlocked(3)) {
            throw new IllegalStateException("level three should stay locked until level two is won");
        }

        progress.recordVictory(2, 1, true);
        if (!panel.isLevelUnlocked(3)) {
            throw new IllegalStateException("recorded level two victory should unlock level three");
        }

        panel.startLevel(3);
        if (panel.isShowingMenu() || panel.getCurrentLevelNumber() != 3) {
            throw new IllegalStateException("panel should enter a selected level");
        }

        BufferedImage gameplayImage = new BufferedImage(GamePanel.WIDTH, GamePanel.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        graphics = gameplayImage.createGraphics();
        panel.paint(graphics);
        graphics.dispose();
        if (countDistinctSamples(gameplayImage) < 8) {
            throw new IllegalStateException("rendered gameplay should contain varied visual content");
        }

        panel.showMenu();
        if (!panel.isShowingMenu()) {
            throw new IllegalStateException("panel should return to the main menu");
        }

        System.out.println("GamePanel render check passed");
    }

    private static Path tempProgressPath() {
        try {
            return Files.createTempDirectory("carrot-guard-render-progress").resolve("progress.properties");
        } catch (IOException e) {
            throw new IllegalStateException("could not create temp progress file", e);
        }
    }

    private static int countDistinctSamples(BufferedImage image) {
        int[] samples = new int[32];
        int count = 0;
        for (int y = 16; y < image.getHeight(); y += 53) {
            for (int x = 16; x < image.getWidth(); x += 47) {
                int rgb = image.getRGB(x, y);
                if (!contains(samples, count, rgb)) {
                    samples[count] = rgb;
                    count++;
                    if (count == samples.length) {
                        return count;
                    }
                }
            }
        }
        return count;
    }

    private static boolean contains(int[] samples, int count, int value) {
        for (int i = 0; i < count; i++) {
            if (samples[i] == value) {
                return true;
            }
        }
        return false;
    }
}
