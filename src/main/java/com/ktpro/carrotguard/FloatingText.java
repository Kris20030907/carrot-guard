package com.ktpro.carrotguard;

import java.awt.Color;

public final class FloatingText {
    private static final double LIFETIME = 0.85;

    private final double x;
    private final double y;
    private final String text;
    private final Color color;
    private double age;

    public FloatingText(double x, double y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
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
        return y - 22 * getProgress();
    }

    public String getText() {
        return text;
    }

    public Color getColor() {
        return color;
    }
}
