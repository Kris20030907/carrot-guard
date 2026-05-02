package com.ktpro.carrotguard;

public enum TowerUpgradeType {
    DAMAGE("DMG"),
    SPEED("SPD"),
    RANGE("RNG");

    private final String displayName;

    TowerUpgradeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

