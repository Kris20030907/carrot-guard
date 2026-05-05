package com.ktpro.carrotguard;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class LevelConfig {
    private final int levelNumber;
    private final GamePath path;
    private final int startingCoins;
    private final int startingLives;
    private final List<ObstacleDefinition> obstacles;
    private final List<WaveDefinition> waves;

    private LevelConfig(
            int levelNumber,
            GamePath path,
            int startingCoins,
            int startingLives,
            List<ObstacleDefinition> obstacles,
            List<WaveDefinition> waves
    ) {
        this.levelNumber = levelNumber;
        this.path = path;
        this.startingCoins = startingCoins;
        this.startingLives = startingLives;
        this.obstacles = List.copyOf(obstacles);
        this.waves = List.copyOf(waves);
    }

    public static LevelConfig firstLevel() {
        return load(1);
    }

    public static LevelConfig load(int levelNumber) {
        if (levelNumber <= 0) {
            throw new IllegalArgumentException("level number must be positive");
        }
        String resourcePath = resourcePath(levelNumber);
        try {
            return loadFromResource(resourcePath, levelNumber);
        } catch (IllegalArgumentException e) {
            if (levelNumber == 1) {
                System.err.println("Could not load " + resourcePath + ": " + e.getMessage());
                return defaultFirstLevel();
            }
            throw e;
        }
    }

    public static boolean hasLevel(int levelNumber) {
        if (levelNumber <= 0) {
            return false;
        }
        try (InputStream input = LevelConfig.class.getResourceAsStream(resourcePath(levelNumber))) {
            return input != null;
        } catch (IOException e) {
            return false;
        }
    }

    public static List<Integer> availableLevelNumbers() {
        List<Integer> levels = new ArrayList<>();
        for (int levelNumber = 1; hasLevel(levelNumber); levelNumber++) {
            levels.add(levelNumber);
        }
        if (levels.isEmpty()) {
            levels.add(1);
        }
        return List.copyOf(levels);
    }

    private static String resourcePath(int levelNumber) {
        return "/levels/level" + levelNumber + ".properties";
    }

    static LevelConfig defaultFirstLevel() {
        return new LevelConfig(
                1,
                GamePath.defaultPath(),
                160,
                10,
                List.of(
                        new ObstacleDefinition(ObstacleKind.CRATE, 2, 2),
                        new ObstacleDefinition(ObstacleKind.ROCK, 8, 3),
                        new ObstacleDefinition(ObstacleKind.CRATE, 12, 3),
                        new ObstacleDefinition(ObstacleKind.ROCK, 3, 7),
                        new ObstacleDefinition(ObstacleKind.CRATE, 7, 8),
                        new ObstacleDefinition(ObstacleKind.ROCK, 11, 8)
                ),
                List.of(
                        WaveDefinition.of("Warmup", 0.92, 2.3, 24,
                                new WaveEntry(EnemyType.NORMAL, 7)),
                        WaveDefinition.of("First Speed Check", 0.82, 2.2, 28,
                                new WaveEntry(EnemyType.NORMAL, 8),
                                new WaveEntry(EnemyType.FAST, 3)),
                        WaveDefinition.of("Slow Tower Cue", 0.76, 2.2, 34,
                                new WaveEntry(EnemyType.NORMAL, 6),
                                new WaveEntry(EnemyType.FAST, 7)),
                        WaveDefinition.of("Armor Check", 0.84, 2.4, 42,
                                new WaveEntry(EnemyType.NORMAL, 8),
                                new WaveEntry(EnemyType.FAST, 4),
                                new WaveEntry(EnemyType.TANK, 2)),
                        WaveDefinition.of("Mixed Pressure", 0.70, 2.5, 52,
                                new WaveEntry(EnemyType.FAST, 8),
                                new WaveEntry(EnemyType.NORMAL, 6),
                                new WaveEntry(EnemyType.TANK, 3)),
                        WaveDefinition.of("Final Exam", 0.64, 0, 78,
                                new WaveEntry(EnemyType.NORMAL, 8),
                                new WaveEntry(EnemyType.FAST, 8),
                                new WaveEntry(EnemyType.TANK, 5))
                )
        );
    }

    static LevelConfig loadFromResource(String resourcePath, int levelNumber) {
        Properties properties = new Properties();
        try (InputStream input = LevelConfig.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("resource not found");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalArgumentException("could not read resource", e);
        }

        int startingCoins = parseInt(properties, "startingCoins");
        int startingLives = parseInt(properties, "startingLives");
        GamePath path = GamePath.fromTiles(parsePath(requireValue(properties, "path")));
        List<ObstacleDefinition> obstacles = parseObstacles(requireValue(properties, "obstacles"));
        List<WaveDefinition> waves = parseWaves(properties);
        return new LevelConfig(levelNumber, path, startingCoins, startingLives, obstacles, waves);
    }

    private static int parseInt(Properties properties, String key) {
        String value = requireValue(properties, key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be an integer", e);
        }
    }

    private static String requireValue(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value.trim();
    }

    private static List<Point> parsePath(String value) {
        List<Point> tiles = new ArrayList<>();
        for (String token : value.split(";")) {
            String[] parts = token.trim().split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("path tile must be col,row: " + token);
            }
            tiles.add(new Point(parseNumber(parts[0], "path col"), parseNumber(parts[1], "path row")));
        }
        return tiles;
    }

    private static List<ObstacleDefinition> parseObstacles(String value) {
        List<ObstacleDefinition> definitions = new ArrayList<>();
        for (String token : value.split(";")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] kindAndPosition = trimmed.split(":");
            if (kindAndPosition.length != 2) {
                throw new IllegalArgumentException("obstacle must be KIND:col,row: " + token);
            }
            ObstacleKind kind = parseEnum(ObstacleKind.class, kindAndPosition[0], "obstacle kind");
            String[] position = kindAndPosition[1].split(",");
            if (position.length != 2) {
                throw new IllegalArgumentException("obstacle position must be col,row: " + token);
            }
            definitions.add(new ObstacleDefinition(
                    kind,
                    parseNumber(position[0], "obstacle col"),
                    parseNumber(position[1], "obstacle row")));
        }
        return definitions;
    }

    private static List<WaveDefinition> parseWaves(Properties properties) {
        List<WaveDefinition> waves = new ArrayList<>();
        for (int index = 1; ; index++) {
            String value = properties.getProperty("wave." + index);
            if (value == null) {
                break;
            }
            waves.add(parseWave(labelForWave(properties, index), value));
        }
        if (waves.isEmpty()) {
            throw new IllegalArgumentException("at least one wave is required");
        }
        return waves;
    }

    private static String labelForWave(Properties properties, int index) {
        String label = properties.getProperty("wave." + index + ".label");
        return label == null || label.isBlank() ? "Wave " + index : label.trim();
    }

    private static WaveDefinition parseWave(String label, String value) {
        String[] parts = value.split(",", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("wave must be spawnInterval,nextWaveDelay,clearBonus,entries: " + value);
        }
        double spawnInterval = parseDouble(parts[0], "wave spawn interval");
        double nextWaveDelay = parseDouble(parts[1], "wave next delay");
        int clearBonus = parseNumber(parts[2], "wave clear bonus");
        List<WaveEntry> entries = new ArrayList<>();
        for (String token : parts[3].split(";")) {
            String[] entry = token.trim().split(":");
            if (entry.length != 2) {
                throw new IllegalArgumentException("wave entry must be ENEMY:count: " + token);
            }
            entries.add(new WaveEntry(
                    parseEnum(EnemyType.class, entry[0], "enemy type"),
                    parseNumber(entry[1], "enemy count")));
        }
        return WaveDefinition.of(label, spawnInterval, nextWaveDelay, clearBonus, entries.toArray(WaveEntry[]::new));
    }

    private static int parseNumber(String value, String label) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be an integer: " + value, e);
        }
    }

    private static double parseDouble(String value, String label) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a number: " + value, e);
        }
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value, String label) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown " + label + ": " + value, e);
        }
    }

    public GamePath getPath() {
        return path;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public int getStartingCoins() {
        return startingCoins;
    }

    public int getStartingLives() {
        return startingLives;
    }

    public List<ObstacleDefinition> getObstacles() {
        return obstacles;
    }

    public WaveDefinition getWave(int index) {
        return waves.get(index);
    }

    public int getWaveCount() {
        return waves.size();
    }
}
