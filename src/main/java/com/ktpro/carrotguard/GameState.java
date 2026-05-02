package com.ktpro.carrotguard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class GameState {
    private final LevelConfig config = LevelConfig.firstLevel();
    private final GamePath path = config.getPath();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Tower> towers = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();

    private Tower selectedTower;
    private int selectedBuildCol = -1;
    private int selectedBuildRow = -1;
    private int coins = config.getStartingCoins();
    private int lives = config.getStartingLives();
    private int waveIndex;
    private int enemiesSpawnedInWave;
    private double spawnTimer;
    private double wavePause;
    private boolean gameOver;
    private boolean won;
    private boolean paused;

    public GameState() {
        resetObstacles();
    }

    public void update(double deltaSeconds) {
        if (gameOver || won || paused) {
            return;
        }

        spawnEnemies(deltaSeconds);
        updateEnemies(deltaSeconds);
        updateTowers(deltaSeconds);
        updateProjectiles(deltaSeconds);
        checkWaveFinished();
    }

    public boolean selectMapTile(int col, int row) {
        if (gameOver || won || col < 0 || row < 0 || col >= GamePanel.COLS || row >= GamePanel.ROWS) {
            return false;
        }
        Tower existing = findTower(col, row);
        if (existing != null) {
            selectedTower = existing;
            clearSelectedBuildTile();
            return true;
        }
        selectedTower = null;

        if (path.containsTile(col, row) || findObstacle(col, row) != null) {
            clearSelectedBuildTile();
            return false;
        }
        selectedBuildCol = col;
        selectedBuildRow = row;
        return true;
    }

    public boolean tryBuildSelectedTower(TowerType type) {
        if (!hasSelectedBuildTile() || !canBuildTowerAt(selectedBuildCol, selectedBuildRow, type)) {
            return false;
        }
        selectedTower = new Tower(selectedBuildCol, selectedBuildRow, type);
        towers.add(selectedTower);
        coins -= type.getCost();
        clearSelectedBuildTile();
        return true;
    }

    public boolean canBuildTowerAt(int col, int row, TowerType type) {
        return !gameOver
                && !won
                && col >= 0
                && row >= 0
                && col < GamePanel.COLS
                && row < GamePanel.ROWS
                && coins >= type.getCost()
                && !path.containsTile(col, row)
                && findObstacle(col, row) == null
                && findTower(col, row) == null;
    }

    public boolean canBuildSelectedTower(TowerType type) {
        return hasSelectedBuildTile() && canBuildTowerAt(selectedBuildCol, selectedBuildRow, type);
    }

    public Tower getTowerAt(int col, int row) {
        if (col < 0 || row < 0 || col >= GamePanel.COLS || row >= GamePanel.ROWS) {
            return null;
        }
        return findTower(col, row);
    }

    public Obstacle getObstacleAt(int col, int row) {
        if (col < 0 || row < 0 || col >= GamePanel.COLS || row >= GamePanel.ROWS) {
            return null;
        }
        return findObstacle(col, row);
    }

    public void togglePaused() {
        if (!gameOver && !won) {
            paused = !paused;
        }
    }

    public void restart() {
        enemies.clear();
        resetObstacles();
        towers.clear();
        projectiles.clear();
        selectedTower = null;
        clearSelectedBuildTile();
        coins = config.getStartingCoins();
        lives = config.getStartingLives();
        waveIndex = 0;
        enemiesSpawnedInWave = 0;
        spawnTimer = 0;
        wavePause = 0;
        gameOver = false;
        won = false;
        paused = false;
    }

    public boolean trySelectTower(int col, int row) {
        selectedTower = findTower(col, row);
        if (selectedTower != null) {
            clearSelectedBuildTile();
        }
        return selectedTower != null;
    }

    public boolean tryUpgradeSelectedTower() {
        if (selectedTower == null || !selectedTower.canUpgrade()) {
            return false;
        }
        int cost = selectedTower.getUpgradeCost();
        if (coins < cost) {
            return false;
        }
        coins -= cost;
        selectedTower.upgrade();
        return true;
    }

    public boolean sellSelectedTower() {
        if (selectedTower == null) {
            return false;
        }
        coins += selectedTower.getSellValue();
        towers.remove(selectedTower);
        selectedTower = null;
        return true;
    }

    private void spawnEnemies(double deltaSeconds) {
        if (wavePause > 0) {
            wavePause -= deltaSeconds;
            return;
        }

        WaveDefinition wave = currentWave();
        int totalEnemies = wave.getTotalEnemyCount();
        if (enemiesSpawnedInWave >= totalEnemies) {
            return;
        }

        spawnTimer -= deltaSeconds;
        if (spawnTimer <= 0) {
            EnemyType type = wave.enemyTypeAt(enemiesSpawnedInWave);
            enemies.add(new Enemy(path, waveIndex + 1, type));
            enemiesSpawnedInWave++;
            spawnTimer = wave.getSpawnInterval();
        }
    }

    private void updateEnemies(double deltaSeconds) {
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            enemy.update(deltaSeconds);
            if (enemy.hasReachedGoal()) {
                iterator.remove();
                lives -= enemy.getLifeDamage();
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
                continue;
            }
            Obstacle obstacle = tower.findObstacleTarget(obstacles);
            if (obstacle != null && tower.canFire()) {
                projectiles.add(tower.fireAt(obstacle));
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
            Target target = projectile.getTarget();
            if (target.isDead()) {
                iterator.remove();
                continue;
            }
            if (projectile.hasHitTarget()) {
                applyProjectileHit(projectile);
                iterator.remove();
                collectDefeatedEnemies();
                collectClearedObstacles();
            }
        }
    }

    private void applyProjectileHit(Projectile projectile) {
        TowerType type = projectile.getTowerType();
        Target target = projectile.getTarget();
        target.damage(projectile.getDamage());
        if (target instanceof Enemy enemyTarget && type.hasSlowEffect()) {
            enemyTarget.applySlow(type.getSlowFactor(), type.getSlowDuration());
        }
        if (target instanceof Enemy enemyTarget && type.hasSplashEffect()) {
            double splashDamage = projectile.getDamage() * 0.65;
            for (Enemy enemy : enemies) {
                if (enemy != enemyTarget && !enemy.isDead() && !enemy.hasReachedGoal()
                        && enemy.distanceTo(enemyTarget.getX(), enemyTarget.getY()) <= type.getSplashRadius()) {
                    enemy.damage(splashDamage);
                }
            }
        }
    }

    private void collectDefeatedEnemies() {
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            if (enemy.isDead()) {
                iterator.remove();
                coins += enemy.getReward();
            }
        }
    }

    private void collectClearedObstacles() {
        Iterator<Obstacle> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Obstacle obstacle = iterator.next();
            if (obstacle.isDead()) {
                iterator.remove();
                coins += obstacle.getReward();
            }
        }
    }

    private void checkWaveFinished() {
        WaveDefinition wave = currentWave();
        int totalEnemies = wave.getTotalEnemyCount();
        if (enemiesSpawnedInWave >= totalEnemies && enemies.isEmpty()) {
            coins += wave.getClearBonus();
            if (waveIndex >= config.getWaveCount() - 1) {
                won = true;
                return;
            }
            waveIndex++;
            enemiesSpawnedInWave = 0;
            wavePause = wave.getNextWaveDelay();
            spawnTimer = 0;
        }
    }

    private WaveDefinition currentWave() {
        return config.getWave(waveIndex);
    }

    private Tower findTower(int col, int row) {
        for (Tower tower : towers) {
            if (tower.getCol() == col && tower.getRow() == row) {
                return tower;
            }
        }
        return null;
    }

    private Obstacle findObstacle(int col, int row) {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.getCol() == col && obstacle.getRow() == row) {
                return obstacle;
            }
        }
        return null;
    }

    private void resetObstacles() {
        obstacles.clear();
        obstacles.add(Obstacle.crate(2, 2));
        obstacles.add(Obstacle.rock(8, 3));
        obstacles.add(Obstacle.crate(12, 3));
        obstacles.add(Obstacle.rock(3, 7));
        obstacles.add(Obstacle.crate(7, 8));
        obstacles.add(Obstacle.rock(11, 8));
    }

    private void clearSelectedBuildTile() {
        selectedBuildCol = -1;
        selectedBuildRow = -1;
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

    public List<Obstacle> getObstacles() {
        return obstacles;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public Tower getSelectedTower() {
        return selectedTower;
    }

    public int getTowerCost() {
        return hasSelectedBuildTile() ? TowerType.BASIC.getCost() : 0;
    }

    public boolean hasSelectedBuildTile() {
        return selectedBuildCol >= 0 && selectedBuildRow >= 0;
    }

    public int getSelectedBuildCol() {
        return selectedBuildCol;
    }

    public int getSelectedBuildRow() {
        return selectedBuildRow;
    }

    public int getCoins() {
        return coins;
    }

    public int getLives() {
        return lives;
    }

    public int getWave() {
        return waveIndex + 1;
    }

    public int getMaxWave() {
        return config.getWaveCount();
    }

    public int getEnemiesSpawnedInWave() {
        return enemiesSpawnedInWave;
    }

    public int getWaveEnemyCount() {
        return currentWave().getTotalEnemyCount();
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isWon() {
        return won;
    }

    public boolean isPaused() {
        return paused;
    }
}
