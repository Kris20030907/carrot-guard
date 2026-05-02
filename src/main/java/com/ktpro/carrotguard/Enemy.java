package com.ktpro.carrotguard;

import java.awt.Point;

public final class Enemy {
    private final GamePath path;
    private final double maxHealth;
    private final double speed;
    private final int reward;

    private int waypointIndex = 1;
    private double x;
    private double y;
    private double health;
    private boolean reachedGoal;

    public Enemy(GamePath path, int wave) {
        this.path = path;
        Point start = path.getWaypoint(0);
        this.x = start.x;
        this.y = start.y;
        this.maxHealth = 42 + wave * 10;
        this.health = maxHealth;
        this.speed = 58 + wave * 3;
        this.reward = 14 + wave;
    }

    public void update(double deltaSeconds) {
        if (reachedGoal || isDead()) {
            return;
        }
        if (waypointIndex >= path.getWaypointCount()) {
            reachedGoal = true;
            return;
        }

        Point target = path.getWaypoint(waypointIndex);
        double dx = target.x - x;
        double dy = target.y - y;
        double distance = Math.hypot(dx, dy);
        double step = speed * deltaSeconds;
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
}

