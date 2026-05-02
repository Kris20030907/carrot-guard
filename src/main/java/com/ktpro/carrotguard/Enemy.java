package com.ktpro.carrotguard;

import java.awt.Point;

public final class Enemy {
    private final GamePath path;
    private final EnemyType type;
    private final double maxHealth;
    private final double speed;
    private final int reward;

    private int waypointIndex = 1;
    private double x;
    private double y;
    private double health;
    private double slowFactor = 1.0;
    private double slowTimer;
    private boolean reachedGoal;

    public Enemy(GamePath path, int wave, EnemyType type) {
        this.path = path;
        this.type = type;
        Point start = path.getWaypoint(0);
        this.x = start.x;
        this.y = start.y;
        this.maxHealth = type.healthForWave(wave);
        this.health = maxHealth;
        this.speed = type.speedForWave(wave);
        this.reward = type.rewardForWave(wave);
    }

    public void update(double deltaSeconds) {
        if (reachedGoal || isDead()) {
            return;
        }
        if (waypointIndex >= path.getWaypointCount()) {
            reachedGoal = true;
            return;
        }

        if (slowTimer > 0) {
            slowTimer = Math.max(0, slowTimer - deltaSeconds);
            if (slowTimer == 0) {
                slowFactor = 1.0;
            }
        }

        Point target = path.getWaypoint(waypointIndex);
        double dx = target.x - x;
        double dy = target.y - y;
        double distance = Math.hypot(dx, dy);
        double step = speed * slowFactor * deltaSeconds;
        if (distance <= step) {
            x = target.x;
            y = target.y;
            waypointIndex++;
            if (waypointIndex >= path.getWaypointCount()) {
                reachedGoal = true;
            }
        } else {
            x += dx / distance * step;
            y += dy / distance * step;
        }
    }

    public void damage(double amount) {
        health = Math.max(0, health - amount);
    }

    public void applySlow(double factor, double seconds) {
        if (seconds <= 0 || isDead() || reachedGoal) {
            return;
        }
        slowFactor = Math.min(slowFactor, factor);
        slowTimer = Math.max(slowTimer, seconds);
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean hasReachedGoal() {
        return reachedGoal;
    }

    public double distanceTo(double targetX, double targetY) {
        return Math.hypot(x - targetX, y - targetY);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getHealthRatio() {
        return health / maxHealth;
    }

    public int getReward() {
        return reward;
    }

    public int getLifeDamage() {
        return type.getLifeDamage();
    }

    public EnemyType getType() {
        return type;
    }

    public boolean isSlowed() {
        return slowTimer > 0;
    }
}
