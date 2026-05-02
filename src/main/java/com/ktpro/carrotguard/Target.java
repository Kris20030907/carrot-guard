package com.ktpro.carrotguard;

public interface Target {
    double getX();

    double getY();

    void damage(double amount);

    boolean isDead();

    default boolean hasReachedGoal() {
        return false;
    }

    default double distanceTo(double targetX, double targetY) {
        return Math.hypot(getX() - targetX, getY() - targetY);
    }
}

