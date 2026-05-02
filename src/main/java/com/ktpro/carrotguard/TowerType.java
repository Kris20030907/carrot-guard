package com.ktpro.carrotguard;

import java.awt.Color;

public enum TowerType {
    BASIC(
            "Basic",
            50,
            24,
            130,
            0.55,
            360,
            new Color(50, 80, 126),
            new Color(31, 49, 79),
            0,
            1.0,
            0
    ),
    SLOW(
            "Slow",
            65,
            12,
            120,
            0.78,
            330,
            new Color(66, 142, 154),
            new Color(29, 88, 102),
            0,
            0.48,
            1.45
    ),
    SPLASH(
            "Splash",
            85,
            20,
            115,
            1.05,
            300,
            new Color(152, 84, 67),
            new Color(93, 48, 44),
            58,
            1.0,
            0
    );

    private final String displayName;
    private final int cost;
    private final double baseDamage;
    private final double baseRange;
    private final double fireInterval;
    private final double projectileSpeed;
    private final Color bodyColor;
    private final Color barrelColor;
    private final double splashRadius;
    private final double slowFactor;
    private final double slowDuration;

    TowerType(
            String displayName,
            int cost,
            double baseDamage,
            double baseRange,
            double fireInterval,
            double projectileSpeed,
            Color bodyColor,
            Color barrelColor,
            double splashRadius,
            double slowFactor,
            double slowDuration
    ) {
        this.displayName = displayName;
        this.cost = cost;
        this.baseDamage = baseDamage;
        this.baseRange = baseRange;
        this.fireInterval = fireInterval;
        this.projectileSpeed = projectileSpeed;
        this.bodyColor = bodyColor;
        this.barrelColor = barrelColor;
        this.splashRadius = splashRadius;
        this.slowFactor = slowFactor;
        this.slowDuration = slowDuration;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCost() {
        return cost;
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public double getBaseRange() {
        return baseRange;
    }

    public double getFireInterval() {
        return fireInterval;
    }

    public double getProjectileSpeed() {
        return projectileSpeed;
    }

    public Color getBodyColor() {
        return bodyColor;
    }

    public Color getBarrelColor() {
        return barrelColor;
    }

    public double getSplashRadius() {
        return splashRadius;
    }

    public double getSlowFactor() {
        return slowFactor;
    }

    public double getSlowDuration() {
        return slowDuration;
    }

    public boolean hasSlowEffect() {
        return slowDuration > 0;
    }

    public boolean hasSplashEffect() {
        return splashRadius > 0;
    }
}

