package com.ktpro.carrotguard;

public enum ObstacleKind {
    CRATE {
        @Override
        public Obstacle createObstacle(int col, int row) {
            return Obstacle.crate(col, row);
        }
    },
    ROCK {
        @Override
        public Obstacle createObstacle(int col, int row) {
            return Obstacle.rock(col, row);
        }
    };

    public abstract Obstacle createObstacle(int col, int row);
}
