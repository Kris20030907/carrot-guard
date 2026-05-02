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

        GameState state = new GameState();
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
}
