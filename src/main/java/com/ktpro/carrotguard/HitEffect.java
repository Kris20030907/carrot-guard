package com.ktpro.carrotguard;

public final class HitEffect {
    private static final double LIFETIME = 0.28;

    private final double x;
    private final double y;
    private final double radius;
    private final TowerType towerType;
    private double age;

    public HitEffect(double x, double y, double radius, TowerType towerType) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.towerType = towerType;
    }

    public void update(double deltaSeconds) {
        age += deltaSeconds;
    }

    public boolean isExpired() {
        return age >= LIFETIME;
    }

    public double getProgress() {
        return Math.min(1.0, age / LIFETIME);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getRadius() {
        return radius;
    }

    public TowerType getTowerType() {
        return towerType;
    }
}
