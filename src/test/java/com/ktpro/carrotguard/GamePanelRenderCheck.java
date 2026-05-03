package com.ktpro.carrotguard;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class GamePanelRenderCheck {
    private GamePanelRenderCheck() {
    }

    public static void main(String[] args) {
        GamePanel panel = new GamePanel();
        panel.setSize(GamePanel.WIDTH, GamePanel.HEIGHT);

        BufferedImage image = new BufferedImage(GamePanel.WIDTH, GamePanel.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();

        int distinctSamples = countDistinctSamples(image);
        if (distinctSamples < 8) {
            throw new IllegalStateException("rendered panel should contain varied visual content");
        }

        System.out.println("GamePanel render check passed");
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
