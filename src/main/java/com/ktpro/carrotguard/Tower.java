package com.ktpro.carrotguard;

import java.util.List;

public final class Tower {
    private final int col;
    private final int row;
    private final double x;
    private final double y;
    private final double range = 130;
    private final double fireInterval = 0.55;
    private final double damage = 24;
    private double cooldown;

    public Tower(int col, int row) {
        this.col = col;
        this.row = row;
        this.x = col * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0;
        this.y = GamePanel.HUD_HEIGHT + row * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0;
    }

    public void update(double deltaSeconds) {
        cooldown = Math.max(0, cooldown - deltaSeconds);
    }

    public Enemy findTarget(List<Enemy> enemies) {
        Enemy best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Enemy enemy : enemies) {
            if (enemy.isDead() || enemy.hasReachedGoal()) {
                continue;
            }
            double distance = enemy.distanceTo(x, y);
            if (distance <= range && distance < bestDistance) {
                best = enemy;
                bestDistance = distance;
            }
        }
        return best;
    }

    public boolean canFire() {
        return cooldown <= 0;
    }

    public Projectile fireAt(Enemy target) {
        cooldown = fireInterval;
        return new Projectile(x, y, target, damage);
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public double getRange() {
        return range;
    }
}

