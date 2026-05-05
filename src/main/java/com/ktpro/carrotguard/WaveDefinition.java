package com.ktpro.carrotguard;

import java.util.List;

public final class WaveDefinition {
    private final List<WaveEntry> entries;
    private final double spawnInterval;
    private final double nextWaveDelay;
    private final int clearBonus;

    private WaveDefinition(double spawnInterval, double nextWaveDelay, int clearBonus, List<WaveEntry> entries) {
        this.spawnInterval = spawnInterval;
        this.nextWaveDelay = nextWaveDelay;
        this.clearBonus = clearBonus;
        this.entries = List.copyOf(entries);
    }

    public static WaveDefinition of(double spawnInterval, double nextWaveDelay, int clearBonus, WaveEntry... entries) {
        return new WaveDefinition(spawnInterval, nextWaveDelay, clearBonus, List.of(entries));
    }

    public int getTotalEnemyCount() {
        int total = 0;
        for (WaveEntry entry : entries) {
            total += entry.count();
        }
        return total;
    }

    public int getEnemyCount(EnemyType type) {
        int total = 0;
        for (WaveEntry entry : entries) {
            if (entry.type() == type) {
                total += entry.count();
            }
        }
        return total;
    }

    public EnemyType enemyTypeAt(int spawnIndex) {
        int remaining = spawnIndex;
        for (WaveEntry entry : entries) {
            if (remaining < entry.count()) {
                return entry.type();
            }
            remaining -= entry.count();
        }
        return entries.get(entries.size() - 1).type();
    }

    public double getSpawnInterval() {
        return spawnInterval;
    }

    public double getNextWaveDelay() {
        return nextWaveDelay;
    }

    public int getClearBonus() {
        return clearBonus;
    }
}
