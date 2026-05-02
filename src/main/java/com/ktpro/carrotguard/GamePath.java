package com.ktpro.carrotguard;

import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public final class GamePath {
    private final List<Point> tiles;
    private final List<Point> waypoints;

    private GamePath(List<Point> tiles) {
        this.tiles = List.copyOf(tiles);
        this.waypoints = new ArrayList<>();
        for (Point tile : tiles) {
            int x = tile.x * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2;
            int y = tile.y * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2 + GamePanel.HUD_HEIGHT;
            waypoints.add(new Point(x, y));
        }
    }

    public static GamePath defaultPath() {
        List<Point> path = new ArrayList<>();
        addHorizontal(path, -1, 5, 5);
        addVertical(path, 5, 5, 1);
        addHorizontal(path, 6, 10, 1);
        addVertical(path, 10, 1, 7);
        addHorizontal(path, 11, 14, 7);
        addVertical(path, 14, 7, 4);
        addHorizontal(path, 15, 15, 4);
        return new GamePath(path);
    }

    private static void addHorizontal(List<Point> path, int startCol, int endCol, int row) {
        int step = startCol <= endCol ? 1 : -1;
        for (int col = startCol; col != endCol + step; col += step) {
            path.add(new Point(col, row));
        }
    }

    private static void addVertical(List<Point> path, int col, int startRow, int endRow) {
        int step = startRow <= endRow ? 1 : -1;
        for (int row = startRow + step; row != endRow + step; row += step) {
            path.add(new Point(col, row));
        }
    }

    public boolean containsTile(int col, int row) {
        for (Point tile : tiles) {
            if (tile.x == col && tile.y == row) {
                return true;
            }
        }
        return false;
    }

    public Point getWaypoint(int index) {
        return waypoints.get(index);
    }

    public int getWaypointCount() {
        return waypoints.size();
    }

    public int getGoalWaypointIndex() {
        return waypoints.size() - 2;
    }

    public boolean hasOnlyOrthogonalSteps() {
        for (int i = 1; i < tiles.size(); i++) {
            Point previous = tiles.get(i - 1);
            Point current = tiles.get(i);
            int dx = Math.abs(current.x - previous.x);
            int dy = Math.abs(current.y - previous.y);
            if (dx + dy != 1) {
                return false;
            }
        }
        return true;
    }

    public int[] getGoalTile() {
        Point goal = tiles.get(getGoalWaypointIndex());
        return new int[] { goal.x, goal.y };
    }

    public void draw(Graphics2D g, int topOffset) {
        for (int i = 1; i < waypoints.size(); i++) {
            Point previous = waypoints.get(i - 1);
            Point current = waypoints.get(i);
            g.drawLine(previous.x, previous.y, current.x, current.y);
        }
    }
}
