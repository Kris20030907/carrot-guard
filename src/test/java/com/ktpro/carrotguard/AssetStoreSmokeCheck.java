package com.ktpro.carrotguard;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AssetStoreSmokeCheck {
    private AssetStoreSmokeCheck() {
    }

    public static void main(String[] args) {
        AssetStore defaultStore = AssetStore.loadDefault();
        require(defaultStore != null, "default asset store should be constructable");

        Path directory = tempAssetDirectory();
        writePng(directory.resolve(AssetKey.CARROT.getFileName()));
        AssetStore directoryStore = AssetStore.loadFromDirectory(directory);
        require(directoryStore.getLoadedCount() == 1, "directory loader should load one png");
        require(directoryStore.hasAsset(AssetKey.CARROT), "directory loader should load carrot asset");
        require(!directoryStore.hasAsset(AssetKey.ENEMY_FAST), "directory loader should not fake missing assets");

        System.out.println("AssetStore smoke check passed");
    }

    private static Path tempAssetDirectory() {
        try {
            return Files.createTempDirectory("carrot-guard-assets");
        } catch (IOException e) {
            throw new IllegalStateException("could not create temp asset directory", e);
        }
    }

    private static void writePng(Path path) {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(255, 145, 63));
        g.fillOval(1, 1, 6, 6);
        g.dispose();
        try {
            ImageIO.write(image, "png", path.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("could not write temp png", e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
