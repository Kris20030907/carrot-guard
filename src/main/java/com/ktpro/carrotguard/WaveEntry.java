package com.ktpro.carrotguard;

public record WaveEntry(EnemyType type, int count) {
    public WaveEntry {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
    }
}

