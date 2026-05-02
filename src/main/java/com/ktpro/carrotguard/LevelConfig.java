package com.ktpro.carrotguard;

public final class LevelConfig {
    private final GamePath path;
    private final int startingCoins;
    private final int startingLives;
    private final int towerCost;
    private final int waveBaseEnemyCount;
    private final int waveEnemyGrowth;
    private final int waveClearBonus;
    private final double wavePauseSeconds;

    private LevelConfig(
            GamePath path,
            int startingCoins,
            int startingLives,
            int towerCost,
            int waveBaseEnemyCount,
            int waveEnemyGrowth,
            int waveClearBonus,
            double wavePauseSeconds
    ) {
        this.path = path;
        this.startingCoins = startingCoins;
        this.startingLives = startingLives;
        this.towerCost = towerCost;
        this.waveBaseEnemyCount = waveBaseEnemyCount;
        this.waveEnemyGrowth = waveEnemyGrowth;
        this.waveClearBonus = waveClearBonus;
        this.wavePauseSeconds = wavePauseSeconds;
    }

    public static LevelConfig firstLevel() {
        return new LevelConfig(
                GamePath.defaultPath(),
                120,
                10,
                50,
                8,
                2,
                20,
                2.0
        );
    }

    public int enemyCountForWave(int wave) {
        return waveBaseEnemyCount + wave * waveEnemyGrowth;
    }

    public double spawnIntervalForWave(int wave) {
        return Math.max(0.38, 1.0 - wave * 0.04);
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

    public int getTowerCost() {
        return towerCost;
    }

    public int getWaveClearBonus() {
        return waveClearBonus;
    }

    public double getWavePauseSeconds() {
        return wavePauseSeconds;
    }
}

