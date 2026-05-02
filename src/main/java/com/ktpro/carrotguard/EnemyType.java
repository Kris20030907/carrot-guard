package com.ktpro.carrotguard;

import java.awt.Color;

public enum EnemyType {
    NORMAL("Normal", 42, 10, 58, 3, 14, 1, 15, 1, new Color(143, 76, 57), new Color(95, 49, 41)),
    FAST("Fast", 30, 7, 88, 5, 16, 1, 12, 1, new Color(177, 111, 59), new Color(112, 70, 42)),
    TANK("Tank", 92, 18, 42, 2, 26, 2, 18, 2, new Color(96, 92, 102), new Color(52, 50, 58));

    private final String displayName;
    private final double baseHealth;
    private final double healthGrowth;
    private final double baseSpeed;
    private final double speedGrowth;
    private final int baseReward;
    private final int rewardGrowth;
    private final int radius;
    private final int lifeDamage;
    private final Color bodyColor;
    private final Color borderColor;

    EnemyType(
            String displayName,
            double baseHealth,
            double healthGrowth,
            double baseSpeed,
            double speedGrowth,
            int baseReward,
            int rewardGrowth,
            int radius,
            int lifeDamage,
            Color bodyColor,
            Color borderColor
    ) {
        this.displayName = displayName;
        this.baseHealth = baseHealth;
        this.healthGrowth = healthGrowth;
        this.baseSpeed = baseSpeed;
        this.speedGrowth = speedGrowth;
        this.baseReward = baseReward;
        this.rewardGrowth = rewardGrowth;
        this.radius = radius;
        this.lifeDamage = lifeDamage;
        this.bodyColor = bodyColor;
        this.borderColor = borderColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double healthForWave(int wave) {
        return baseHealth + wave * healthGrowth;
    }

    public double speedForWave(int wave) {
        return baseSpeed + wave * speedGrowth;
    }

    public int rewardForWave(int wave) {
        return baseReward + wave * rewardGrowth;
    }

    public int getRadius() {
        return radius;
    }

    public int getLifeDamage() {
        return lifeDamage;
    }

    public Color getBodyColor() {
        return bodyColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }
}

