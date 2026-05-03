package com.ktpro.carrotguard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class GameProgress {
    private static final String HIGHEST_UNLOCKED_LEVEL = "highestUnlockedLevel";
    private static final String BEST_STARS_PREFIX = "level.";
    private static final String BEST_STARS_SUFFIX = ".stars";

    private final Path savePath;
    private int highestUnlockedLevel = 1;
    private final Properties bestStars = new Properties();

    public GameProgress(Path savePath) {
        this.savePath = savePath;
        load();
    }

    public static GameProgress loadDefault() {
        String home = System.getProperty("user.home", ".");
        return new GameProgress(Path.of(home, ".carrot-guard", "progress.properties"));
    }

    public boolean isUnlocked(int levelNumber) {
        return levelNumber <= highestUnlockedLevel;
    }

    public int getBestStars(int levelNumber) {
        String value = bestStars.getProperty(starKey(levelNumber));
        if (value == null) {
            return 0;
        }
        try {
            return Math.max(0, Math.min(3, Integer.parseInt(value)));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getHighestUnlockedLevel() {
        return highestUnlockedLevel;
    }

    public void recordVictory(int levelNumber, int stars, boolean hasNextLevel) {
        if (levelNumber <= 0 || stars <= 0) {
            return;
        }
        int safeStars = Math.min(3, stars);
        if (safeStars > getBestStars(levelNumber)) {
            bestStars.setProperty(starKey(levelNumber), String.valueOf(safeStars));
        }
        if (hasNextLevel) {
            highestUnlockedLevel = Math.max(highestUnlockedLevel, levelNumber + 1);
        } else {
            highestUnlockedLevel = Math.max(highestUnlockedLevel, levelNumber);
        }
        save();
    }

    private void load() {
        if (!Files.isRegularFile(savePath)) {
            return;
        }
        Properties data = new Properties();
        try (InputStream input = Files.newInputStream(savePath)) {
            data.load(input);
        } catch (IOException e) {
            return;
        }
        highestUnlockedLevel = parseUnlockedLevel(data.getProperty(HIGHEST_UNLOCKED_LEVEL));
        for (String key : data.stringPropertyNames()) {
            if (key.startsWith(BEST_STARS_PREFIX) && key.endsWith(BEST_STARS_SUFFIX)) {
                bestStars.setProperty(key, data.getProperty(key));
            }
        }
    }

    private void save() {
        Properties data = new Properties();
        data.setProperty(HIGHEST_UNLOCKED_LEVEL, String.valueOf(Math.max(1, highestUnlockedLevel)));
        for (String key : bestStars.stringPropertyNames()) {
            data.setProperty(key, bestStars.getProperty(key));
        }

        try {
            Path parent = savePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(savePath)) {
                data.store(output, "Carrot Guard progress");
            }
        } catch (IOException e) {
            System.err.println("Could not save progress: " + e.getMessage());
        }
    }

    private int parseUnlockedLevel(String value) {
        if (value == null) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private String starKey(int levelNumber) {
        return BEST_STARS_PREFIX + levelNumber + BEST_STARS_SUFFIX;
    }
}
