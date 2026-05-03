package com.ktpro.carrotguard;

public enum AssetKey {
    GRASS("grass.png"),
    PATH("path.png"),
    CARROT("carrot.png"),
    TOWER_BASIC("tower_basic.png"),
    TOWER_SLOW("tower_slow.png"),
    TOWER_SPLASH("tower_splash.png"),
    PROJECTILE_BASIC("projectile_basic.png"),
    PROJECTILE_SLOW("projectile_slow.png"),
    PROJECTILE_SPLASH("projectile_splash.png"),
    ENEMY_NORMAL("enemy_normal.png"),
    ENEMY_FAST("enemy_fast.png"),
    ENEMY_TANK("enemy_tank.png"),
    OBSTACLE_CRATE("obstacle_crate.png"),
    OBSTACLE_ROCK("obstacle_rock.png");

    private final String fileName;

    AssetKey(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
