package com.ktpro.carrotguard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GameProgressSmokeCheck {
    private GameProgressSmokeCheck() {
    }

    public static void main(String[] args) {
        Path savePath = tempProgressPath();
        GameProgress progress = new GameProgress(savePath);

        require(progress.isUnlocked(1), "level one should be unlocked by default");
        require(!progress.isUnlocked(2), "level two should start locked");
        require(progress.getBestStars(1) == 0, "level one should start with zero stars");

        progress.recordVictory(1, 2, true);
        require(progress.isUnlocked(2), "winning level one should unlock level two");
        require(progress.getBestStars(1) == 2, "winning should store best stars");

        progress.recordVictory(1, 1, true);
        require(progress.getBestStars(1) == 2, "lower stars should not overwrite best stars");

        progress.recordVictory(1, 3, true);
        require(progress.getBestStars(1) == 3, "higher stars should overwrite best stars");

        GameProgress reloaded = new GameProgress(savePath);
        require(reloaded.isUnlocked(2), "unlocked level should survive reload");
        require(reloaded.getBestStars(1) == 3, "best stars should survive reload");

        System.out.println("GameProgress smoke check passed");
    }

    private static Path tempProgressPath() {
        try {
            return Files.createTempDirectory("carrot-guard-progress").resolve("progress.properties");
        } catch (IOException e) {
            throw new IllegalStateException("could not create temp progress file", e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
