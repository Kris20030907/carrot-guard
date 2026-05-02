package com.ktpro.carrotguard;

import java.awt.Color;

public final class Obstacle implements Target {
    private final int col;
    private final int row;
    private final double x;
    private final double y;
    private final double maxHealth;
    private final int reward;
    private final Color bodyColor;
    private final Color borderColor;
    private double health;

    public Obstacle(int col, int row, double health, int reward, Color bodyColor, Color borderColor) {
        this.col = col;
        this.row = row;
        this.x = col * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0;
        this.y = GamePanel.HUD_HEIGHT + row * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0;
        this.maxHealth = health;
        this.health = health;
        this.reward = reward;
        this.bodyColor = bodyColor;
        this.borderColor = borderColor;
    }

    public static Obstacle crate(int col, int row) {
        return new Obstacle(col, row, 90, 35, new Color(151, 105, 61), new Color(96, 65, 40));
    }

    public static Obstacle rock(int col, int row) {
        return new Obstacle(col, row, 145, 55, new Color(117, 121, 111), new Color(70, 75, 68));
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public void damage(double amount) {
        health = Math.max(0, health - amount);
    }

    @Override
    public boolean isDead() {
        return health <= 0;
    }

    public double getHealthRatio() {
        return health / maxHealth;
    }

    public int getReward() {
        return reward;
    }

    public Color getBodyColor() {
        return bodyColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }
}

