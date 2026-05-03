package com.ktpro.carrotguard;

public final class GameStateSmokeCheck {
    private GameStateSmokeCheck() {
    }

    public static void main(String[] args) {
        GamePath path = GamePath.defaultPath();
        require(path.hasOnlyOrthogonalSteps(), "default path should not skip or diagonal between tiles");
        require(path.containsTile(5, 4), "default path should include the first turn connector");
        require(path.containsTile(10, 2), "default path should include the second turn connector");
        require(path.containsTile(14, 6), "default path should include the last turn connector");
        verifyLevelConfigLoaded();

        GameState state = new GameState();
        require(state.getLevelNumber() == 1, "new game should start on level one");
        verifySpeedMultiplierCycle(state);
        state.cycleSpeedMultiplier();
        require(state.getSpeedMultiplier() == 2.0, "speed test should move state away from default");
        require(state.hasNextLevel(), "level one should have a next level available");
        require(!state.advanceToNextLevel(), "next level should require victory");
        require(state.canBuildTowerAt(1, 2, TowerType.BASIC), "empty grass tile should be buildable");
        require(!state.canBuildTowerAt(2, 2, TowerType.BASIC), "obstacle tile should not be buildable");
        require(state.getObstacleAt(2, 2) != null, "obstacle should be queryable");
        require(!state.canBuildTowerAt(0, 5, TowerType.BASIC), "path tile should not be buildable");
        require(!state.canBuildTowerAt(-1, 2, TowerType.BASIC), "out-of-bounds tile should not be buildable");
        require(state.selectMapTile(1, 2), "empty grass tile should be selectable for building");
        require(state.canBuildSelectedTower(TowerType.BASIC), "basic tower should be buildable on selected tile");
        require(state.tryBuildSelectedTower(TowerType.BASIC), "basic tower should be built from selected tile");
        require(!state.canBuildTowerAt(1, 2, TowerType.BASIC), "occupied tile should not be buildable");
        Tower basicTower = state.getTowerAt(1, 2);
        require(basicTower != null, "built tower should be queryable");
        require(state.selectMapTile(7, 3), "second empty grass tile should be selectable");
        require(state.tryBuildSelectedTower(TowerType.SLOW), "slow tower should be buildable");
        require(state.selectMapTile(3, 3), "third empty grass tile should be selectable");
        require(!state.tryBuildSelectedTower(TowerType.SPLASH), "splash tower should be unaffordable after two builds");
        verifyTowerUpgradeFlow();
        verifyTowerUpgradeStats();
        verifyEnemyReachesGoalAtCarrot();
        verifyEnemiesCanReachGoalOnConfiguredLevels();
        verifyLeaksCanEndGame();

        state.togglePaused();
        for (int i = 0; i < 120; i++) {
            state.update(0.1);
        }
        require(state.getEnemiesSpawnedInWave() == 0, "paused state should not spawn enemies");

        state.togglePaused();
        for (int i = 0; i < 80; i++) {
            state.update(0.1);
        }
        require(state.getEnemiesSpawnedInWave() > 0, "running state should spawn enemies");

        state.restart();
        require(state.getWave() == 1, "restart should return to wave one");
        require(state.getSpeedMultiplier() == 1.0, "restart should reset speed to 1x");
        require(state.getEnemies().isEmpty(), "restart should clear enemies");
        require(state.getTowers().isEmpty(), "restart should clear towers");
        require(!state.getObstacles().isEmpty(), "restart should restore obstacles");
        require(!state.isPaused() && !state.isGameOver() && !state.isWon(), "restart should clear end states");

        System.out.println("GameState smoke check passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void verifySpeedMultiplierCycle(GameState state) {
        require(state.getSpeedMultiplier() == 1.0, "default speed should be 1x");
        require("1x".equals(state.getSpeedLabel()), "default speed label should be 1x");
        state.cycleSpeedMultiplier();
        require(state.getSpeedMultiplier() == 2.0, "second speed should be 2x");
        state.cycleSpeedMultiplier();
        require(state.getSpeedMultiplier() == 4.0, "third speed should be 4x");
        state.cycleSpeedMultiplier();
        require(state.getSpeedMultiplier() == 0.5, "fourth speed should be 0.5x");
        require("0.5x".equals(state.getSpeedLabel()), "fractional speed label should show 0.5x");
        state.cycleSpeedMultiplier();
        require(state.getSpeedMultiplier() == 1.0, "speed should cycle back to 1x");
    }

    private static void verifyLevelConfigLoaded() {
        LevelConfig config = LevelConfig.firstLevel();
        require(config.getStartingCoins() == 160, "resource level should define starting coins");
        require(config.getStartingLives() == 10, "resource level should define starting lives");
        require(config.getWaveCount() == 6, "resource level should define six waves");
        require(config.getObstacles().size() == 6, "resource level should define six obstacles");
        require(config.getPath().hasOnlyOrthogonalSteps(), "resource path should be orthogonal");
        require(config.getPath().containsTile(5, 4), "resource path should include the first turn connector");
        require(LevelConfig.hasLevel(2), "second level resource should be available");
        LevelConfig secondLevel = LevelConfig.load(2);
        require(secondLevel.getLevelNumber() == 2, "second level should report its level number");
        require(secondLevel.getStartingCoins() == 180, "second level should define starting coins");
        require(secondLevel.getWaveCount() == 6, "second level should define six waves");
        require(secondLevel.getPath().hasOnlyOrthogonalSteps(), "second level path should be orthogonal");
        require(secondLevel.getObstacles().size() == 7, "second level should define seven obstacles");
    }

    private static void verifyTowerUpgradeStats() {
        Tower tower = new Tower(0, 0, TowerType.BASIC);
        double baseDamage = tower.getDamage();
        double baseInterval = tower.getFireInterval();
        double baseRange = tower.getRange();
        tower.upgrade(TowerUpgradeType.DAMAGE);
        tower.upgrade(TowerUpgradeType.SPEED);
        tower.upgrade(TowerUpgradeType.RANGE);
        require(tower.getDamage() > baseDamage, "damage upgrade should increase damage");
        require(tower.getFireInterval() < baseInterval, "speed upgrade should lower fire interval");
        require(tower.getRange() > baseRange, "range upgrade should increase range");
        require(tower.getUpgradePulse() > 0, "tower upgrade should create visual pulse");
        require(tower.getPreviewDamage(TowerUpgradeType.DAMAGE) > tower.getDamage(), "damage preview should increase damage");
        require(tower.getPreviewFireInterval(TowerUpgradeType.SPEED) < tower.getFireInterval(), "speed preview should lower interval");
        require(tower.getPreviewRange(TowerUpgradeType.RANGE) > tower.getRange(), "range preview should increase range");
    }

    private static void verifyTowerUpgradeFlow() {
        GameState state = new GameState();
        require(state.selectMapTile(1, 2), "upgrade test tile should be selectable");
        require(state.tryBuildSelectedTower(TowerType.BASIC), "upgrade test tower should be buildable");
        require(state.tryUpgradeSelectedTower(TowerUpgradeType.DAMAGE), "damage upgrade should be affordable");
        Tower tower = state.getTowerAt(1, 2);
        require(tower != null && tower.getUpgradeLevel(TowerUpgradeType.DAMAGE) == 1, "damage upgrade should be applied");
    }

    private static void verifyEnemyReachesGoalAtCarrot() {
        GamePath path = GamePath.defaultPath();
        Enemy enemy = new Enemy(path, 1, EnemyType.FAST);
        for (int i = 0; i < 600 && !enemy.hasReachedGoal(); i++) {
            enemy.update(0.05);
        }
        int[] goalTile = path.getGoalTile();
        double goalX = goalTile[0] * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0;
        double goalY = goalTile[1] * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0 + GamePanel.HUD_HEIGHT;
        require(enemy.hasReachedGoal(), "enemy should reach goal before leaving the map");
        require(Math.abs(enemy.getX() - goalX) < 0.01, "enemy should stop at carrot x when reaching goal");
        require(Math.abs(enemy.getY() - goalY) < 0.01, "enemy should stop at carrot y when reaching goal");
    }

    private static void verifyEnemiesCanReachGoalOnConfiguredLevels() {
        verifyEnemyReachesGoalAtCarrot(LevelConfig.load(1));
        verifyEnemyReachesGoalAtCarrot(LevelConfig.load(2));
    }

    private static void verifyEnemyReachesGoalAtCarrot(LevelConfig config) {
        GamePath path = config.getPath();
        Enemy enemy = new Enemy(path, 1, EnemyType.FAST);
        enemy.update(20.0);
        int[] goalTile = path.getGoalTile();
        double goalX = goalTile[0] * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0;
        double goalY = goalTile[1] * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0 + GamePanel.HUD_HEIGHT;
        require(enemy.hasReachedGoal(), "enemy should reach level " + config.getLevelNumber() + " goal with large delta");
        require(Math.abs(enemy.getX() - goalX) < 0.01, "enemy should stop at level " + config.getLevelNumber() + " carrot x");
        require(Math.abs(enemy.getY() - goalY) < 0.01, "enemy should stop at level " + config.getLevelNumber() + " carrot y");
    }

    private static void verifyLeaksCanEndGame() {
        GameState state = new GameState();
        for (int i = 0; i < 80 && !state.isGameOver(); i++) {
            state.update(10.0);
        }
        require(state.isGameOver(), "enough leaked enemies should end the game");
        require(state.getLives() <= 0, "game over from leaks should exhaust lives");
    }
}
