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
        require(progress.isSoundEnabled(), "sound should be enabled by default");
        require(progress.getSoundVolume() == 70, "sound should start at default volume");

        progress.recordVictory(1, 2, true);
        require(progress.isUnlocked(2), "winning level one should unlock level two");
        require(!progress.isUnlocked(3), "level three should stay locked after winning level one");
        require(progress.getBestStars(1) == 2, "winning should store best stars");
        progress.setSoundEnabled(false);
        progress.setSoundVolume(35);
        require(!progress.isSoundEnabled(), "sound setting should be mutable");
        require(progress.getSoundVolume() == 35, "volume setting should be mutable");

        progress.recordVictory(1, 1, true);
        require(progress.getBestStars(1) == 2, "lower stars should not overwrite best stars");

        progress.recordVictory(1, 3, true);
        require(progress.getBestStars(1) == 3, "higher stars should overwrite best stars");
        progress.recordVictory(2, 1, true);
        require(progress.isUnlocked(3), "winning level two should unlock level three");

        GameProgress reloaded = new GameProgress(savePath);
        require(reloaded.isUnlocked(3), "unlocked level should survive reload");
        require(reloaded.getBestStars(1) == 3, "best stars should survive reload");
        require(!reloaded.isSoundEnabled(), "sound setting should survive reload");
        require(reloaded.getSoundVolume() == 35, "volume setting should survive reload");
        reloaded.clearLevelProgress();
        require(reloaded.isUnlocked(1), "clear progress should keep level one unlocked");
        require(!reloaded.isUnlocked(2), "clear progress should lock level two");
        require(reloaded.getBestStars(1) == 0, "clear progress should reset stars");
        require(!reloaded.isSoundEnabled(), "clear progress should keep sound setting");
        require(reloaded.getSoundVolume() == 35, "clear progress should keep volume setting");

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
