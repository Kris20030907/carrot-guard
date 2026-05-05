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
        for (AssetKey key : AssetKey.values()) {
            require(defaultStore.hasAsset(key), "default asset pack should include " + key.getFileName());
        }

        Path directory = tempAssetDirectory();
        writePng(directory.resolve(AssetKey.CARROT.getFileName()));
        AssetStore directoryStore = AssetStore.loadFromDirectory(directory);
        require(directoryStore.getLoadedCount() == 1, "directory loader should load one png");
        require(directoryStore.hasAsset(AssetKey.CARROT), "directory loader should load carrot asset");
        require(!directoryStore.hasAsset(AssetKey.ENEMY_FAST), "directory loader should not fake missing assets");
        verifyScaledDrawCache(directoryStore);

        System.out.println("AssetStore smoke check passed");
    }

    private static void verifyScaledDrawCache(AssetStore store) {
        BufferedImage target = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        require(store.getCachedScaledImageCount() == 0, "scaled cache should start empty");
        require(store.draw(g, AssetKey.CARROT, 0, 0, 32, 40), "draw should render loaded asset");
        require(store.getCachedScaledImageCount() == 1, "first scaled draw should cache one variant");
        require(store.draw(g, AssetKey.CARROT, 2, 2, 32, 40), "draw should render cached asset");
        require(store.getCachedScaledImageCount() == 1, "same size draw should reuse cached variant");
        require(store.draw(g, AssetKey.CARROT, 4, 4, 34, 42), "draw should render another size");
        require(store.getCachedScaledImageCount() == 2, "new size draw should cache another variant");
        g.dispose();
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
