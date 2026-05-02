package com.ktpro.carrotguard;

public record ObstacleDefinition(ObstacleKind kind, int col, int row) {
    public Obstacle createObstacle() {
        return kind.createObstacle(col, row);
    }
}
