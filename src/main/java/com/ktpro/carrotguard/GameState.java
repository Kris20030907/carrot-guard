package com.ktpro.carrotguard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class GameState {
    private static final double[] SPEED_MULTIPLIERS = { 0.5, 1.0, 2.0, 4.0 };
    private static final int DEFAULT_SPEED_INDEX = 1;

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Tower> towers = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<HitEffect> hitEffects = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();

    private LevelConfig config;
    private GamePath path;
    private Tower selectedTower;
    private int selectedBuildCol = -1;
    private int selectedBuildRow = -1;
    private int coins;
    private int lives;
    private int waveIndex;
    private int enemiesSpawnedInWave;
    private int speedIndex = DEFAULT_SPEED_INDEX;
    private int leakedEnemies;
    private int clearedObstacles;
    private int hitEventCount;
    private int leakEventCount;
    private int clearedObstacleEventCount;
    private double elapsedSeconds;
    private double spawnTimer;
    private double wavePause;
    private boolean gameOver;
    private boolean won;
    private boolean paused;
    private boolean carrotSelected;

    public GameState() {
        loadLevel(1);
    }

    public void update(double deltaSeconds) {
        if (gameOver || won || paused) {
            return;
        }

        elapsedSeconds += deltaSeconds;
        updateHitEffects(deltaSeconds);
        updateFloatingTexts(deltaSeconds);
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
        if (isGoalTile(col, row)) {
            carrotSelected = true;
            selectedTower = null;
            clearSelectedBuildTile();
            return true;
        }
        carrotSelected = false;
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
        resetRunState();
    }

    public void startLevel(int levelNumber) {
        loadLevel(levelNumber);
    }

    public void cycleSpeedMultiplier() {
        speedIndex = (speedIndex + 1) % SPEED_MULTIPLIERS.length;
    }

    public boolean advanceToNextLevel() {
        int nextLevelNumber = config.getLevelNumber() + 1;
        if (!won || !LevelConfig.hasLevel(nextLevelNumber)) {
            return false;
        }
        loadLevel(nextLevelNumber);
        return true;
    }

    private void loadLevel(int levelNumber) {
        config = LevelConfig.load(levelNumber);
        path = config.getPath();
        resetRunState();
    }

    private void resetRunState() {
        enemies.clear();
        resetObstacles();
        towers.clear();
        projectiles.clear();
        hitEffects.clear();
        floatingTexts.clear();
        selectedTower = null;
        carrotSelected = false;
        clearSelectedBuildTile();
        coins = config.getStartingCoins();
        lives = config.getStartingLives();
        waveIndex = 0;
        enemiesSpawnedInWave = 0;
        speedIndex = DEFAULT_SPEED_INDEX;
        leakedEnemies = 0;
        clearedObstacles = 0;
        hitEventCount = 0;
        leakEventCount = 0;
        clearedObstacleEventCount = 0;
        elapsedSeconds = 0;
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

    public boolean tryUpgradeSelectedTower(TowerUpgradeType upgradeType) {
        if (selectedTower == null || !selectedTower.canUpgrade(upgradeType)) {
            return false;
        }
        int cost = selectedTower.getUpgradeCost(upgradeType);
        if (coins < cost) {
            return false;
        }
        coins -= cost;
        selectedTower.upgrade(upgradeType);
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
                leakedEnemies++;
                leakEventCount++;
                int damage = enemy.getLifeDamage();
                lives -= damage;
                addCarrotDamageFeedback(damage);
                if (lives <= 0) {
                    gameOver = true;
                }
            }
        }
    }

    private void updateFloatingTexts(double deltaSeconds) {
        Iterator<FloatingText> iterator = floatingTexts.iterator();
        while (iterator.hasNext()) {
            FloatingText text = iterator.next();
            text.update(deltaSeconds);
            if (text.isExpired()) {
                iterator.remove();
            }
        }
    }

    private void addCarrotDamageFeedback(int damage) {
        int[] goal = path.getGoalTile();
        double x = goal[0] * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0;
        double y = GamePanel.HUD_HEIGHT + goal[1] * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0 - 28;
        floatingTexts.add(new FloatingText(x, y, "-" + damage, new Color(255, 104, 87)));
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
        hitEventCount++;
        hitEffects.add(new HitEffect(target.getX(), target.getY(), 18, type));
        target.damage(projectile.getDamage());
        if (target instanceof Enemy enemyTarget && type.hasSlowEffect()) {
            enemyTarget.applySlow(type.getSlowFactor(), type.getSlowDuration());
        }
        if (target instanceof Enemy enemyTarget && type.hasSplashEffect()) {
            double splashDamage = projectile.getDamage() * 0.65;
            hitEffects.add(new HitEffect(enemyTarget.getX(), enemyTarget.getY(), type.getSplashRadius(), type));
            for (Enemy enemy : enemies) {
                if (enemy != enemyTarget && !enemy.isDead() && !enemy.hasReachedGoal()
                        && enemy.distanceTo(enemyTarget.getX(), enemyTarget.getY()) <= type.getSplashRadius()) {
                    enemy.damage(splashDamage);
                }
            }
        }
    }

    private void updateHitEffects(double deltaSeconds) {
        Iterator<HitEffect> iterator = hitEffects.iterator();
        while (iterator.hasNext()) {
            HitEffect effect = iterator.next();
            effect.update(deltaSeconds);
            if (effect.isExpired()) {
                iterator.remove();
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
                clearedObstacles++;
                clearedObstacleEventCount++;
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
        for (ObstacleDefinition definition : config.getObstacles()) {
            obstacles.add(definition.createObstacle());
        }
    }

    private void clearSelectedBuildTile() {
        selectedBuildCol = -1;
        selectedBuildRow = -1;
    }

    public boolean isGoalTile(int col, int row) {
        int[] goal = path.getGoalTile();
        return goal[0] == col && goal[1] == row;
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

    public List<HitEffect> getHitEffects() {
        return hitEffects;
    }

    public List<FloatingText> getFloatingTexts() {
        return floatingTexts;
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

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getLeakedEnemies() {
        return leakedEnemies;
    }

    public int getClearedObstacles() {
        return clearedObstacles;
    }

    public int getHitEventCount() {
        return hitEventCount;
    }

    public int getLeakEventCount() {
        return leakEventCount;
    }

    public int getClearedObstacleEventCount() {
        return clearedObstacleEventCount;
    }

    public int getStarRating() {
        if (!won) {
            return 0;
        }
        if (leakedEnemies == 0 && lives == getMaxLives()) {
            return 3;
        }
        if (getLifeRatio() >= 0.5 && leakedEnemies <= 2) {
            return 2;
        }
        return 1;
    }

    public int getMaxLives() {
        return config.getStartingLives();
    }

    public double getLifeRatio() {
        return Math.max(0.0, lives / (double) config.getStartingLives());
    }

    public boolean isCarrotSelected() {
        return carrotSelected;
    }

    public int getWave() {
        return waveIndex + 1;
    }

    public int getLevelNumber() {
        return config.getLevelNumber();
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

    public boolean hasNextLevel() {
        return LevelConfig.hasLevel(config.getLevelNumber() + 1);
    }

    public double getSpeedMultiplier() {
        return SPEED_MULTIPLIERS[speedIndex];
    }

    public String getSpeedLabel() {
        double speed = getSpeedMultiplier();
        return speed == Math.floor(speed) ? (int) speed + "x" : speed + "x";
    }
}
