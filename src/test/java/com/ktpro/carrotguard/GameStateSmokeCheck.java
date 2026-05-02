package com.ktpro.carrotguard;

public final class GameStateSmokeCheck {
    private GameStateSmokeCheck() {
    }

    public static void main(String[] args) {
        GameState state = new GameState();
        require(state.tryBuildTower(2, 2), "basic tower should be buildable");
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
        require(!state.isPaused() && !state.isGameOver() && !state.isWon(), "restart should clear end states");

        System.out.println("GameState smoke check passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

