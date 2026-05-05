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
        require(state.getElapsedSeconds() == 0.0, "new run should start with zero elapsed time");
        require(state.getStarRating() == 0, "unfinished run should not have stars");
        require(state.getHitEventCount() == 0, "new run should start with no hit events");
        require(state.getLeakEventCount() == 0, "new run should start with no leak events");
        require(state.getClearedObstacleEventCount() == 0, "new run should start with no obstacle clear events");
        verifySpeedMultiplierCycle(state);
        state.cycleSpeedMultiplier();
        require(state.getSpeedMultiplier() == 2.0, "speed test should move state away from default");
        int[] goalTile = state.getPath().getGoalTile();
        require(state.selectMapTile(goalTile[0], goalTile[1]), "carrot tile should be selectable");
        require(state.isCarrotSelected(), "clicking carrot should select carrot info");
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
        verifyCombatAnimationState();
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
        require(state.getElapsedSeconds() > 0.0, "running state should track elapsed time");

        state.restart();
        require(state.getWave() == 1, "restart should return to wave one");
        require(state.getSpeedMultiplier() == 1.0, "restart should reset speed to 1x");
        require(state.getElapsedSeconds() == 0.0, "restart should reset elapsed time");
        require(state.getLeakedEnemies() == 0, "restart should reset leaked enemy count");
        require(state.getClearedObstacles() == 0, "restart should reset cleared obstacle count");
        require(state.getHitEventCount() == 0, "restart should reset hit event count");
        require(state.getLeakEventCount() == 0, "restart should reset leak event count");
        require(state.getClearedObstacleEventCount() == 0, "restart should reset obstacle event count");
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
        require(LevelConfig.availableLevelNumbers().size() >= 3, "level select should discover configured levels");
        require(LevelConfig.availableLevelNumbers().contains(1), "available levels should include level one");
        require(LevelConfig.availableLevelNumbers().contains(2), "available levels should include level two");
        require(LevelConfig.availableLevelNumbers().contains(3), "available levels should include level three");
        require(LevelConfig.hasLevel(2), "second level resource should be available");
        LevelConfig secondLevel = LevelConfig.load(2);
        require(secondLevel.getLevelNumber() == 2, "second level should report its level number");
        require(secondLevel.getStartingCoins() == 180, "second level should define starting coins");
        require(secondLevel.getWaveCount() == 6, "second level should define six waves");
        require(secondLevel.getPath().hasOnlyOrthogonalSteps(), "second level path should be orthogonal");
        require(secondLevel.getObstacles().size() == 7, "second level should define seven obstacles");
        require(LevelConfig.hasLevel(3), "third level resource should be available");
        LevelConfig thirdLevel = LevelConfig.load(3);
        require(thirdLevel.getLevelNumber() == 3, "third level should report its level number");
        require(thirdLevel.getStartingCoins() == 200, "third level should define starting coins");
        require(thirdLevel.getWaveCount() == 6, "third level should define six waves");
        require(thirdLevel.getPath().hasOnlyOrthogonalSteps(), "third level path should be orthogonal");
        require(thirdLevel.getObstacles().size() == 8, "third level should define eight obstacles");
        verifyLevelPacing(config, secondLevel, thirdLevel);
    }

    private static void verifyLevelPacing(LevelConfig firstLevel, LevelConfig secondLevel, LevelConfig thirdLevel) {
        require(firstLevel.getWave(0).getEnemyCount(EnemyType.FAST) == 0, "level one first wave should teach normal enemies");
        require(firstLevel.getWave(0).getEnemyCount(EnemyType.TANK) == 0, "level one first wave should not include tanks");
        require(firstLevel.getWave(3).getEnemyCount(EnemyType.TANK) > 0, "level one should introduce tanks before the finale");
        require(hasAllEnemyTypes(firstLevel.getWave(5)), "level one finale should mix all enemy types");
        require(firstLevel.getWave(5).getTotalEnemyCount() > firstLevel.getWave(0).getTotalEnemyCount(),
                "level one finale should be denser than the opener");

        require(secondLevel.getWave(0).getEnemyCount(EnemyType.FAST) > 0, "level two should open with fast pressure");
        require(secondLevel.getWave(1).getEnemyCount(EnemyType.TANK) > 0, "level two should introduce tanks earlier");
        require(hasAllEnemyTypes(secondLevel.getWave(3)), "level two middle waves should overlap enemy roles");
        require(secondLevel.getWave(5).getTotalEnemyCount() > secondLevel.getWave(0).getTotalEnemyCount(),
                "level two finale should be denser than the opener");

        require(hasAllEnemyTypes(thirdLevel.getWave(2)), "level three should combine all roles by wave three");
        require(thirdLevel.getWave(5).getSpawnInterval() < thirdLevel.getWave(0).getSpawnInterval(),
                "level three finale should spawn faster than the opener");
        require(thirdLevel.getWave(5).getEnemyCount(EnemyType.TANK) >= secondLevel.getWave(5).getEnemyCount(EnemyType.TANK),
                "level three finale should keep tank pressure high");
    }

    private static boolean hasAllEnemyTypes(WaveDefinition wave) {
        return wave.getEnemyCount(EnemyType.NORMAL) > 0
                && wave.getEnemyCount(EnemyType.FAST) > 0
                && wave.getEnemyCount(EnemyType.TANK) > 0;
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
        state.startLevel(2);
        require(state.getLevelNumber() == 2, "startLevel should load a requested level");
        require(state.getTowers().isEmpty(), "startLevel should reset towers");
        require(state.getWave() == 1, "startLevel should reset wave progress");
    }

    private static void verifyCombatAnimationState() {
        Tower tower = new Tower(1, 2, TowerType.BASIC);
        Enemy enemy = new Enemy(GamePath.defaultPath(), 1, EnemyType.NORMAL);
        Projectile projectile = tower.fireAt(enemy);
        require(tower.getFirePulse() > 0, "tower fire should create a visual recoil pulse");
        tower.update(0.2);
        require(tower.getFirePulse() == 0, "tower fire pulse should expire");

        double startX = projectile.getX();
        double startY = projectile.getY();
        projectile.update(0.05);
        require(projectile.getPreviousX() == startX, "projectile should remember previous x for trail rendering");
        require(projectile.getPreviousY() == startY, "projectile should remember previous y for trail rendering");
        require(projectile.getAge() > 0, "projectile should expose age for draw pulse");
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
        verifyEnemyReachesGoalAtCarrot(LevelConfig.load(3));
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
        require(state.getLeakedEnemies() > 0, "leaked enemies should be counted");
        require(state.getLeakEventCount() > 0, "leaked enemies should emit leak events");
        require(state.getStarRating() == 0, "failed runs should not report stars");
        require(!state.getFloatingTexts().isEmpty(), "leaked enemies should create carrot damage text");
    }
}
