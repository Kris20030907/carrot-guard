package com.ktpro.carrotguard;

import java.util.List;

public final class Tower {
    private static final int MAX_UPGRADE_LEVEL = 2;

    private final TowerType type;
    private final int col;
    private final int row;
    private final double x;
    private final double y;
    private int damageLevel;
    private int speedLevel;
    private int rangeLevel;
    private double cooldown;
    private double upgradePulse;

    public Tower(int col, int row, TowerType type) {
        this.type = type;
        this.col = col;
        this.row = row;
        this.x = col * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0;
        this.y = GamePanel.HUD_HEIGHT + row * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0;
    }

    public void update(double deltaSeconds) {
        cooldown = Math.max(0, cooldown - deltaSeconds);
        upgradePulse = Math.max(0, upgradePulse - deltaSeconds);
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

    public Obstacle findObstacleTarget(List<Obstacle> obstacles) {
        Obstacle best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Obstacle obstacle : obstacles) {
            if (obstacle.isDead()) {
                continue;
            }
            double distance = obstacle.distanceTo(x, y);
            if (distance <= getRange() && distance < bestDistance) {
                best = obstacle;
                bestDistance = distance;
            }
        }
        return best;
    }

    public boolean canFire() {
        return cooldown <= 0;
    }

    public Projectile fireAt(Target target) {
        cooldown = getFireInterval();
        return new Projectile(x, y, target, getDamage(), type, type.getProjectileSpeed());
    }

    public boolean canUpgrade(TowerUpgradeType upgradeType) {
        return getUpgradeLevel(upgradeType) < MAX_UPGRADE_LEVEL;
    }

    public void upgrade(TowerUpgradeType upgradeType) {
        if (!canUpgrade(upgradeType)) {
            return;
        }
        switch (upgradeType) {
            case DAMAGE -> damageLevel++;
            case SPEED -> speedLevel++;
            case RANGE -> rangeLevel++;
        }
        upgradePulse = 0.55;
    }

    public int getUpgradeCost(TowerUpgradeType upgradeType) {
        if (!canUpgrade(upgradeType)) {
            return 0;
        }
        return type.getCost() / 2 + (getUpgradeLevel(upgradeType) + 1) * 35;
    }

    public int getSellValue() {
        int invested = type.getCost();
        for (TowerUpgradeType upgradeType : TowerUpgradeType.values()) {
            for (int currentLevel = 0; currentLevel < getUpgradeLevel(upgradeType); currentLevel++) {
                invested += type.getCost() / 2 + (currentLevel + 1) * 35;
            }
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
        return type.getBaseRange() + rangeLevel * 24;
    }

    public double getDamage() {
        return type.getBaseDamage() + damageLevel * 18;
    }

    public double getFireInterval() {
        return Math.max(0.24, type.getFireInterval() - speedLevel * 0.1);
    }

    public int getLevel() {
        return 1 + damageLevel + speedLevel + rangeLevel;
    }

    public int getUpgradeLevel(TowerUpgradeType upgradeType) {
        return switch (upgradeType) {
            case DAMAGE -> damageLevel;
            case SPEED -> speedLevel;
            case RANGE -> rangeLevel;
        };
    }

    public double getUpgradePulse() {
        return upgradePulse;
    }
}
