package com.ktpro.carrotguard;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public final class AssetStore {
    private static final String RESOURCE_ROOT = "/assets/";

    private final Map<AssetKey, BufferedImage> images = new EnumMap<>(AssetKey.class);

    private AssetStore() {
    }

    public static AssetStore loadDefault() {
        AssetStore store = new AssetStore();
        for (AssetKey key : AssetKey.values()) {
            store.loadResource(key);
        }
        return store;
    }

    static AssetStore loadFromDirectory(Path directory) {
        AssetStore store = new AssetStore();
        for (AssetKey key : AssetKey.values()) {
            Path file = directory.resolve(key.getFileName());
            if (Files.isRegularFile(file)) {
                try (InputStream input = Files.newInputStream(file)) {
                    store.loadImage(key, input);
                } catch (IOException e) {
                    System.err.println("Could not load asset " + file + ": " + e.getMessage());
                }
            }
        }
        return store;
    }

    public boolean hasAsset(AssetKey key) {
        return images.containsKey(key);
    }

    public int getLoadedCount() {
        return images.size();
    }

    public boolean draw(Graphics2D g, AssetKey key, int x, int y, int width, int height) {
        BufferedImage image = images.get(key);
        if (image == null) {
            return false;
        }
        Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, x, y, width, height, null);
        if (oldHint != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
        }
        return true;
    }

    private void loadResource(AssetKey key) {
        try (InputStream input = AssetStore.class.getResourceAsStream(RESOURCE_ROOT + key.getFileName())) {
            if (input != null) {
                loadImage(key, input);
            }
        } catch (IOException e) {
            System.err.println("Could not load asset " + key.getFileName() + ": " + e.getMessage());
        }
    }

    private void loadImage(AssetKey key, InputStream input) throws IOException {
        BufferedImage image = ImageIO.read(input);
        if (image != null) {
            images.put(key, toCompatibleImage(image));
        }
    }

    private BufferedImage toCompatibleImage(BufferedImage image) {
        BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = converted.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return converted;
    }
}
