package com.ktpro.carrotguard;

import java.util.List;

public final class Tower {
    private static final int MAX_LEVEL = 3;

    private final TowerType type;
    private final int col;
    private final int row;
    private final double x;
    private final double y;
    private int level = 1;
    private double cooldown;

    public Tower(int col, int row, TowerType type) {
        this.type = type;
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
            if (distance <= getRange() && distance < bestDistance) {
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
        cooldown = getFireInterval();
        return new Projectile(x, y, target, getDamage(), type, type.getProjectileSpeed());
    }

    public boolean canUpgrade() {
        return level < MAX_LEVEL;
    }

    public void upgrade() {
        if (canUpgrade()) {
            level++;
        }
    }

    public int getUpgradeCost() {
        if (!canUpgrade()) {
            return 0;
        }
        return type.getCost() + level * 30;
    }

    public int getSellValue() {
        int invested = type.getCost();
        for (int currentLevel = 1; currentLevel < level; currentLevel++) {
            invested += type.getCost() + currentLevel * 30;
        }
        return Math.max(25, invested / 2);
    }

    public TowerType getType() {
        return type;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public double getRange() {
        return type.getBaseRange() + (level - 1) * 20;
    }

    public double getDamage() {
        return type.getBaseDamage() + (level - 1) * 14;
    }

    public double getFireInterval() {
        return Math.max(0.28, type.getFireInterval() - (level - 1) * 0.08);
    }

    public int getLevel() {
        return level;
    }
}
