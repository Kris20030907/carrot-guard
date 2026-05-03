package com.ktpro.carrotguard;

import java.awt.Point;

public final class Enemy implements Target {
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

        double remainingStep = speed * slowFactor * deltaSeconds;
        while (remainingStep > 0 && !reachedGoal && !isDead()) {
            Point target = path.getWaypoint(waypointIndex);
            double dx = target.x - x;
            double dy = target.y - y;
            double distance = Math.hypot(dx, dy);
            if (distance <= remainingStep) {
                x = target.x;
                y = target.y;
                remainingStep -= distance;
                if (waypointIndex == path.getGoalWaypointIndex()) {
                    reachedGoal = true;
                    return;
                }
                waypointIndex++;
                if (waypointIndex >= path.getWaypointCount()) {
                    reachedGoal = true;
                    return;
                }
            } else {
                x += dx / distance * remainingStep;
                y += dy / distance * remainingStep;
                return;
            }
        }
    }

    @Override
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

    @Override
    public boolean isDead() {
        return health <= 0;
    }

    @Override
    public boolean hasReachedGoal() {
        return reachedGoal;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
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
