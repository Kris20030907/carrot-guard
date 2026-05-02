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
        require(state.canBuildTowerAt(1, 2), "empty grass tile should be buildable");
        require(!state.canBuildTowerAt(2, 2), "obstacle tile should not be buildable");
        require(state.getObstacleAt(2, 2) != null, "obstacle should be queryable");
        require(!state.canBuildTowerAt(0, 5), "path tile should not be buildable");
        require(!state.canBuildTowerAt(-1, 2), "out-of-bounds tile should not be buildable");
        require(state.tryBuildTower(1, 2), "basic tower should be buildable");
        require(!state.canBuildTowerAt(1, 2), "occupied tile should not be buildable");
        require(state.getTowerAt(1, 2) != null, "built tower should be queryable");
        state.selectTowerType(TowerType.SLOW);
        require(state.tryBuildTower(7, 3), "slow tower should be buildable");
        state.selectTowerType(TowerType.SPLASH);
        require(!state.tryBuildTower(3, 3), "splash tower should be unaffordable after two builds");

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
}
