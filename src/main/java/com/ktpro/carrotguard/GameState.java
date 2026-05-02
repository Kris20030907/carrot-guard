package com.ktpro.carrotguard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class GameState {
    private final LevelConfig config = LevelConfig.firstLevel();
    private final GamePath path = config.getPath();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Tower> towers = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();

    private Tower selectedTower;
    private TowerType selectedTowerType = TowerType.BASIC;
    private int coins = config.getStartingCoins();
    private int lives = config.getStartingLives();
    private int waveIndex;
    private int enemiesSpawnedInWave;
    private double spawnTimer;
    private double wavePause;
    private boolean gameOver;
    private boolean won;
    private boolean paused;

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

    public boolean tryBuildTower(int col, int row) {
        if (gameOver || won || col < 0 || row < 0 || col >= GamePanel.COLS || row >= GamePanel.ROWS) {
            return false;
        }
        Tower existing = findTower(col, row);
        if (existing != null) {
            selectedTower = existing;
            return true;
        }
        selectedTower = null;
        if (coins < selectedTowerType.getCost() || path.containsTile(col, row)) {
            return false;
        }
        selectedTower = new Tower(col, row, selectedTowerType);
        towers.add(selectedTower);
        coins -= selectedTowerType.getCost();
        return true;
    }

    public void selectTowerType(TowerType type) {
        selectedTowerType = type;
    }

    public void togglePaused() {
        if (!gameOver && !won) {
            paused = !paused;
        }
    }

    public void restart() {
        enemies.clear();
        towers.clear();
        projectiles.clear();
        selectedTower = null;
        selectedTowerType = TowerType.BASIC;
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
                applyProjectileHit(projectile);
                iterator.remove();
                collectDefeatedEnemies();
            }
        }
    }

    private void applyProjectileHit(Projectile projectile) {
        TowerType type = projectile.getTowerType();
        Enemy target = projectile.getTarget();
        target.damage(projectile.getDamage());
        if (type.hasSlowEffect()) {
            target.applySlow(type.getSlowFactor(), type.getSlowDuration());
        }
        if (type.hasSplashEffect()) {
            double splashDamage = projectile.getDamage() * 0.65;
            for (Enemy enemy : enemies) {
                if (enemy != target && !enemy.isDead() && !enemy.hasReachedGoal()
                        && enemy.distanceTo(target.getX(), target.getY()) <= type.getSplashRadius()) {
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

    public Tower getSelectedTower() {
        return selectedTower;
    }

    public int getTowerCost() {
        return selectedTowerType.getCost();
    }

    public TowerType getSelectedTowerType() {
        return selectedTowerType;
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
