package com.ktpro.carrotguard;

public final class Projectile {
    private final Enemy target;
    private final double damage;
    private final TowerType towerType;
    private final double speed;
    private double x;
    private double y;
    private double age;
    private boolean expired;

    public Projectile(double x, double y, Enemy target, double damage, TowerType towerType, double speed) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.damage = damage;
        this.towerType = towerType;
        this.speed = speed;
    }

    public void update(double deltaSeconds) {
        age += deltaSeconds;
        if (age > 3 || target.isDead() || target.hasReachedGoal()) {
            expired = true;
            return;
        }

        double dx = target.getX() - x;
        double dy = target.getY() - y;
        double distance = Math.hypot(dx, dy);
        double step = speed * deltaSeconds;
        if (distance <= step || distance < 4) {
            x = target.getX();
            y = target.getY();
        } else {
            x += dx / distance * step;
            y += dy / distance * step;
        }
    }

    public boolean hasHitTarget() {
        return !expired && target.distanceTo(x, y) <= 8;
    }

    public boolean isExpired() {
        return expired;
    }

    public Enemy getTarget() {
        return target;
    }

    public double getDamage() {
        return damage;
    }

    public TowerType getTowerType() {
        return towerType;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
