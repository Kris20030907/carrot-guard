package com.ktpro.carrotguard;

import java.util.List;

public final class LevelConfig {
    private final GamePath path;
    private final int startingCoins;
    private final int startingLives;
    private final List<WaveDefinition> waves;

    private LevelConfig(
            GamePath path,
            int startingCoins,
            int startingLives,
            List<WaveDefinition> waves
    ) {
        this.path = path;
        this.startingCoins = startingCoins;
        this.startingLives = startingLives;
        this.waves = List.copyOf(waves);
    }

    public static LevelConfig firstLevel() {
        return new LevelConfig(
                GamePath.defaultPath(),
                160,
                10,
                List.of(
                        WaveDefinition.of(0.82, 2.0, 20,
                                new WaveEntry(EnemyType.NORMAL, 8)),
                        WaveDefinition.of(0.74, 2.0, 25,
                                new WaveEntry(EnemyType.NORMAL, 8),
                                new WaveEntry(EnemyType.FAST, 4)),
                        WaveDefinition.of(0.70, 2.2, 30,
                                new WaveEntry(EnemyType.FAST, 8),
                                new WaveEntry(EnemyType.NORMAL, 6)),
                        WaveDefinition.of(0.82, 2.3, 35,
                                new WaveEntry(EnemyType.NORMAL, 8),
                                new WaveEntry(EnemyType.TANK, 3),
                                new WaveEntry(EnemyType.FAST, 5)),
                        WaveDefinition.of(0.66, 2.4, 45,
                                new WaveEntry(EnemyType.FAST, 10),
                                new WaveEntry(EnemyType.TANK, 5)),
                        WaveDefinition.of(0.60, 0, 70,
                                new WaveEntry(EnemyType.NORMAL, 8),
                                new WaveEntry(EnemyType.FAST, 8),
                                new WaveEntry(EnemyType.TANK, 6))
                )
        );
    }

    public GamePath getPath() {
        return path;
    }

    public int getStartingCoins() {
        return startingCoins;
    }

    public int getStartingLives() {
        return startingLives;
    }

    public WaveDefinition getWave(int index) {
        return waves.get(index);
    }

    public int getWaveCount() {
        return waves.size();
    }
}
