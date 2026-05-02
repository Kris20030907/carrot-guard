package com.ktpro.carrotguard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class GameState {
    private static final int STARTING_COINS = 120;
    private static final int STARTING_LIVES = 10;
    private static final int TOWER_COST = 50;

    private final GamePath path = GamePath.defaultPath();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Tower> towers = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();

    private int coins = STARTING_COINS;
    private int lives = STARTING_LIVES;
    private int wave = 1;
    private int enemiesSpawnedInWave;
    private double spawnTimer;
    private double wavePause;
    private boolean gameOver;

    public void update(double deltaSeconds) {
        if (gameOver) {
            return;
        }

        spawnEnemies(deltaSeconds);
        updateEnemies(deltaSeconds);
        updateTowers(deltaSeconds);
        updateProjectiles(deltaSeconds);
        checkWaveFinished();
    }

    public boolean tryBuildTower(int col, int row) {
        if (gameOver || col < 0 || row < 0 || col >= GamePanel.COLS || row >= GamePanel.ROWS) {
            return false;
        }
        if (coins < TOWER_COST || path.containsTile(col, row) || hasTower(col, row)) {
            return false;
        }
        towers.add(new Tower(col, row));
        coins -= TOWER_COST;
        return true;
    }

    private void spawnEnemies(double deltaSeconds) {
        if (wavePause > 0) {
            wavePause -= deltaSeconds;
            return;
        }

        int totalEnemies = 8 + wave * 2;
        if (enemiesSpawnedInWave >= totalEnemies) {
            return;
        }

        spawnTimer -= deltaSeconds;
        if (spawnTimer <= 0) {
            enemies.add(new Enemy(path, wave));
            enemiesSpawnedInWave++;
            spawnTimer = Math.max(0.38, 1.0 - wave * 0.04);
        }
    }

    private void updateEnemies(double deltaSeconds) {
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            enemy.update(deltaSeconds);
            if (enemy.hasReachedGoal()) {
                iterator.remove();
                lives--;
                if (lives <= 0) {
                    gameOver = true;
                }
            }
        }
    }

    private void updateTowers(double deltaSeconds) {
        for (Tower tower : towers) {
            tower.update(deltaSeconds);
            Enemy target = tower.findTarget(enemies);
            if (target != null && tower.canFire()) {
                projectiles.add(tower.fireAt(target));
            }
        }
    }

    private void updateProjectiles(double deltaSeconds) {
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.update(deltaSeconds);
            if (projectile.isExpired()) {
                iterator.remove();
                continue;
            }
            Enemy target = projectile.getTarget();
            if (target.isDead()) {
                iterator.remove();
                continue;
            }
            if (projectile.hasHitTarget()) {
                target.damage(projectile.getDamage());
                iterator.remove();
                if (target.isDead()) {
                    enemies.remove(target);
                    coins += target.getReward();
                }
            }
        }
    }

    private void checkWaveFinished() {
        int totalEnemies = 8 + wave * 2;
        if (enemiesSpawnedInWave >= totalEnemies && enemies.isEmpty()) {
            wave++;
            enemiesSpawnedInWave = 0;
            wavePause = 2.0;
            coins += 20;
        }
    }

    private boolean hasTower(int col, int row) {
        for (Tower tower : towers) {
            if (tower.getCol() == col && tower.getRow() == row) {
                return true;
            }
        }
        return false;
    }

    public GamePath getPath() {
        return path;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Tower> getTowers() {
        return towers;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public int getCoins() {
        return coins;
    }

    public int getLives() {
        return lives;
    }

    public int getWave() {
        return wave;
    }

    public boolean isGameOver() {
        return gameOver;
    }
}

