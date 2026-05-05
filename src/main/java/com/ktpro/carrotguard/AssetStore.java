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
    private final Map<AssetKey, Map<ImageSize, BufferedImage>> scaledImages = new EnumMap<>(AssetKey.class);

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

    int getCachedScaledImageCount() {
        int total = 0;
        for (Map<ImageSize, BufferedImage> cache : scaledImages.values()) {
            total += cache.size();
        }
        return total;
    }

    public boolean draw(Graphics2D g, AssetKey key, int x, int y, int width, int height) {
        BufferedImage image = images.get(key);
        if (image == null || width <= 0 || height <= 0) {
            return false;
        }
        g.drawImage(scaledImage(key, image, width, height), x, y, null);
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
            scaledImages.remove(key);
        }
    }

    private BufferedImage scaledImage(AssetKey key, BufferedImage image, int width, int height) {
        if (image.getWidth() == width && image.getHeight() == height) {
            return image;
        }
        ImageSize size = new ImageSize(width, height);
        Map<ImageSize, BufferedImage> cache = scaledImages.computeIfAbsent(key, ignored -> new java.util.HashMap<>());
        return cache.computeIfAbsent(size, ignored -> createScaledImage(image, width, height));
    }

    private BufferedImage createScaledImage(BufferedImage image, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    private BufferedImage toCompatibleImage(BufferedImage image) {
        BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = converted.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return converted;
    }

    private record ImageSize(int width, int height) {
    }
}
