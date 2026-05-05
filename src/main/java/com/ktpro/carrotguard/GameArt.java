package com.ktpro.carrotguard;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

final class GameArt {
    private static final Color GRASS_A = new Color(126, 181, 104);
    private static final Color GRASS_B = new Color(116, 171, 96);
    private static final Color GRASS_LINE = new Color(91, 144, 79, 94);
    private static final Color PATH_A = new Color(216, 178, 111);
    private static final Color PATH_B = new Color(228, 193, 128);
    private static final Color PATH_EDGE = new Color(174, 132, 79, 115);
    private static final Color PATH_RIBBON = new Color(250, 220, 157, 205);
    private static final Color SHADOW = new Color(57, 43, 35, 88);
    private static final Color CREAM = new Color(255, 250, 235);
    private static final Color HEALTH_BG = new Color(73, 43, 35);
    private static final Color HEALTH_OK = new Color(91, 204, 100);
    private static final Color HEALTH_LOW = new Color(232, 88, 74);

    private GameArt() {
    }

    static void drawGrassTile(Graphics2D g, AssetStore assets, int x, int y, int col, int row) {
        if (assets.draw(g, AssetKey.GRASS, x, y, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE)) {
            g.setColor(GRASS_LINE);
            g.drawRect(x, y, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);
            return;
        }
        Color base = ((col + row) & 1) == 0 ? GRASS_A : GRASS_B;
        g.setPaint(new GradientPaint(x, y, brighten(base, 13), x + GamePanel.TILE_SIZE, y + GamePanel.TILE_SIZE, darken(base, 11)));
        g.fillRect(x, y, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);
        g.setColor(GRASS_LINE);
        g.drawRect(x, y, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);

        int seed = col * 37 + row * 53;
        g.setColor(new Color(198, 230, 145, 55));
        for (int i = 0; i < 2; i++) {
            int gx = x + 8 + Math.floorMod(seed + i * 19, 29);
            int gy = y + 9 + Math.floorMod(seed * 3 + i * 11, 28);
            g.drawLine(gx, gy + 5, gx + 3, gy);
            g.drawLine(gx + 3, gy + 5, gx + 6, gy + 1);
        }
    }

    static void drawPathTile(Graphics2D g, AssetStore assets, int x, int y, int col, int row) {
        if (assets.draw(g, AssetKey.PATH, x, y, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE)) {
            g.setColor(PATH_EDGE);
            g.drawRect(x, y, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);
            return;
        }
        Color base = ((col + row) & 1) == 0 ? PATH_A : PATH_B;
        g.setPaint(new GradientPaint(x, y, brighten(base, 12), x + GamePanel.TILE_SIZE, y + GamePanel.TILE_SIZE, darken(base, 12)));
        g.fillRect(x, y, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);
        g.setColor(PATH_EDGE);
        g.drawRect(x, y, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);

        int seed = col * 41 + row * 29;
        g.setColor(new Color(139, 100, 61, 42));
        for (int i = 0; i < 2; i++) {
            int px = x + 9 + Math.floorMod(seed + i * 17, 28);
            int py = y + 10 + Math.floorMod(seed * 5 + i * 13, 27);
            g.fillOval(px, py, 3 + i, 2 + i);
        }
    }

    static void drawPathRibbon(Graphics2D g, GamePath path) {
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(PATH_RIBBON);
        path.draw(g, GamePanel.HUD_HEIGHT);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 239, 186, 145));
        path.draw(g, GamePanel.HUD_HEIGHT);
    }

    static void drawCarrot(Graphics2D g, AssetStore assets, int centerX, int centerY, boolean selected, double hitFlash) {
        g.setColor(new Color(80, 48, 33, 84));
        g.fillOval(centerX - 21, centerY + 12, 42, 13);
        if (assets.draw(g, AssetKey.CARROT, centerX - 37, centerY - 53, 74, 96)) {
            drawCarrotHitFlash(g, centerX, centerY, hitFlash);
            if (selected) {
                g.setColor(new Color(255, 246, 164, 130));
                g.setStroke(new BasicStroke(3f));
                g.drawOval(centerX - 35, centerY - 48, 70, 88);
            }
            return;
        }

        g.setColor(new Color(67, 150, 66));
        g.fillOval(centerX - 13, centerY - 31, 17, 16);
        g.setColor(new Color(47, 124, 58));
        g.fillOval(centerX - 1, centerY - 34, 20, 17);

        g.setPaint(new GradientPaint(centerX - 18, centerY - 18, new Color(255, 177, 72),
                centerX + 18, centerY + 20, new Color(238, 96, 32)));
        g.fillOval(centerX - 17, centerY - 19, 34, 41);
        g.setColor(new Color(173, 70, 34, 105));
        g.setStroke(new BasicStroke(2f));
        g.drawArc(centerX - 12, centerY - 4, 22, 11, 190, 126);
        g.drawArc(centerX - 9, centerY + 7, 19, 9, 190, 116);

        g.setColor(new Color(255, 222, 137, 135));
        g.fillOval(centerX - 9, centerY - 12, 8, 16);
        drawCarrotHitFlash(g, centerX, centerY, hitFlash);
        if (selected) {
            g.setColor(new Color(255, 246, 164, 130));
            g.setStroke(new BasicStroke(3f));
            g.drawOval(centerX - 25, centerY - 29, 50, 57);
        }
    }

    static void drawObstacle(Graphics2D g, AssetStore assets, Obstacle obstacle) {
        int x = (int) obstacle.getX();
        int y = (int) obstacle.getY();
        drawShadow(g, x, y + 14, 35, 10);
        boolean rock = isRock(obstacle.getBodyColor());
        AssetKey assetKey = rock ? AssetKey.OBSTACLE_ROCK : AssetKey.OBSTACLE_CRATE;
        if (!assets.draw(g, assetKey, x - 22, y - 24, 44, 44)) {
            if (rock) {
                drawRock(g, obstacle, x, y);
            } else {
                drawCrate(g, obstacle, x, y);
            }
        }
        drawHealthBar(g, x - 18, y - 30, 36, 6, obstacle.getHealthRatio(), new Color(247, 197, 74));
    }

    static void drawTower(Graphics2D g, AssetStore assets, Tower tower, int centerX, int centerY, boolean selected, double animationSeconds) {
        drawShadow(g, centerX, centerY + 17, 41, 12);
        if (selected) {
            g.setColor(new Color(255, 246, 164, 85));
            g.fillOval(centerX - 25, centerY - 25, 50, 50);
        }

        int recoil = (int) Math.round(5 * Math.min(1.0, tower.getFirePulse() / 0.16));
        int drawY = centerY - 43 + recoil;
        if (assets.draw(g, towerAssetKey(tower.getType()), centerX - 30, drawY, 60, 76)) {
            drawMuzzleFlash(g, tower, centerX, centerY + recoil, animationSeconds);
            drawTowerLevelBadge(g, tower.getLevel(), centerX + 18, centerY - 24 + recoil);
            return;
        }

        int bodyY = centerY + recoil;
        g.setPaint(new GradientPaint(centerX - 19, bodyY - 19, brighten(tower.getType().getBodyColor(), selected ? 38 : 24),
                centerX + 18, bodyY + 20, darken(tower.getType().getBodyColor(), 26)));
        g.fillOval(centerX - 18, bodyY - 16, 36, 35);
        g.setColor(darken(tower.getType().getBarrelColor(), 8));
        g.setStroke(new BasicStroke(3f));
        g.drawOval(centerX - 18, bodyY - 16, 36, 35);

        if (tower.getType() == TowerType.SLOW) {
            drawSlowTowerTop(g, tower, centerX, bodyY);
        } else if (tower.getType() == TowerType.SPLASH) {
            drawSplashTowerTop(g, tower, centerX, bodyY);
        } else {
            drawBasicTowerTop(g, tower, centerX, bodyY);
        }
        drawMuzzleFlash(g, tower, centerX, bodyY, animationSeconds);

        g.setColor(CREAM);
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 12));
        drawCenteredAt(g, String.valueOf(tower.getLevel()), centerX, bodyY + 7);
    }

    static void drawProjectile(Graphics2D g, AssetStore assets, Projectile projectile, double animationSeconds) {
        int x = (int) projectile.getX();
        int y = (int) projectile.getY();
        int size = projectile.getTowerType().hasSplashEffect() ? 13 : 10;
        drawProjectileTrail(g, projectile, size);
        drawShadow(g, x, y + 4, size + 4, 5);
        int pulse = (int) Math.round(2 * Math.sin(animationSeconds * 18 + projectile.getAge() * 8));
        if (assets.draw(g, projectileAssetKey(projectile.getTowerType()), x - size - pulse / 2, y - size - pulse / 2,
                size * 2 + pulse, size * 2 + pulse)) {
            return;
        }
        g.setPaint(new GradientPaint(x - size / 2, y - size / 2, brighten(projectile.getTowerType().getBodyColor(), 48),
                x + size / 2, y + size / 2, projectile.getTowerType().getBarrelColor()));
        g.fillOval(x - size / 2, y - size / 2, size, size);
        g.setColor(new Color(255, 250, 235, 135));
        g.fillOval(x - size / 4, y - size / 3, Math.max(3, size / 3), Math.max(3, size / 3));
    }

    static void drawEnemy(Graphics2D g, AssetStore assets, Enemy enemy, double animationSeconds) {
        int x = (int) enemy.getX();
        int y = (int) enemy.getY();
        int radius = enemy.getType().getRadius();
        double phase = animationSeconds * 8.0 + enemy.getX() * 0.035 + enemy.getY() * 0.019;
        int bob = (int) Math.round(Math.sin(phase) * (enemy.isSlowed() ? 1.1 : 2.0));
        int squash = (int) Math.round(Math.abs(Math.sin(phase)) * 3);
        drawShadow(g, x, y + radius - 1, radius * 2 + 7 + squash, 11);
        int assetWidth = radius * 2 + 24;
        int assetHeight = radius * 2 + 28;
        if (assets.draw(g, enemyAssetKey(enemy.getType()), x - (assetWidth + squash) / 2,
                y - (assetHeight - squash) / 2 - 2 + bob, assetWidth + squash, assetHeight - squash)) {
            if (enemy.isSlowed()) {
                g.setColor(new Color(104, 220, 235, 170));
                g.setStroke(new BasicStroke(2f));
                g.drawOval(x - radius - 5, y - radius - 5, (radius + 5) * 2, (radius + 5) * 2);
            }
            drawHealthBar(g, x - radius - 2, y - radius - 11, radius * 2 + 4, 5, enemy.getHealthRatio(), HEALTH_OK);
            return;
        }

        Color body = enemy.getType().getBodyColor();
        g.setPaint(new GradientPaint(x - radius, y - radius + bob, brighten(body, 32), x + radius, y + radius + bob, darken(body, 20)));
        g.fillOval(x - radius - squash / 2, y - radius + bob, radius * 2 + squash, radius * 2 - squash);
        g.setColor(enemy.getType().getBorderColor());
        g.setStroke(new BasicStroke(2f));
        g.drawOval(x - radius - squash / 2, y - radius + bob, radius * 2 + squash, radius * 2 - squash);

        if (enemy.getType() == EnemyType.FAST) {
            g.setColor(new Color(255, 230, 140, 150));
            g.fillOval(x + radius - 7, y - radius + 4, 8, 8);
        } else if (enemy.getType() == EnemyType.TANK) {
            g.setColor(new Color(210, 211, 202, 130));
            g.fillRoundRect(x - radius + 6, y - 5, radius * 2 - 12, 10, 4, 4);
        }

        if (enemy.isSlowed()) {
            g.setColor(new Color(104, 220, 235, 170));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(x - radius - 5, y - radius - 5, (radius + 5) * 2, (radius + 5) * 2);
        }

        g.setColor(CREAM);
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 10));
        drawCenteredAt(g, enemy.getType().getDisplayName().substring(0, 1), x, y + 4);
        drawHealthBar(g, x - radius - 2, y - radius - 11, radius * 2 + 4, 5, enemy.getHealthRatio(), HEALTH_OK);
    }

    static void drawHealthBar(Graphics2D g, int x, int y, int width, int height, double ratio, Color fill) {
        g.setColor(HEALTH_BG);
        g.fillRoundRect(x, y, width, height, height, height);
        int fillWidth = (int) (width * Math.max(0.0, Math.min(1.0, ratio)));
        g.setColor(ratio > 0.35 ? fill : HEALTH_LOW);
        g.fillRoundRect(x, y, fillWidth, height, height, height);
    }

    static Color healthColor(double ratio) {
        return ratio > 0.35 ? HEALTH_OK : HEALTH_LOW;
    }

    private static AssetKey towerAssetKey(TowerType type) {
        return switch (type) {
            case BASIC -> AssetKey.TOWER_BASIC;
            case SLOW -> AssetKey.TOWER_SLOW;
            case SPLASH -> AssetKey.TOWER_SPLASH;
        };
    }

    private static AssetKey projectileAssetKey(TowerType type) {
        return switch (type) {
            case BASIC -> AssetKey.PROJECTILE_BASIC;
            case SLOW -> AssetKey.PROJECTILE_SLOW;
            case SPLASH -> AssetKey.PROJECTILE_SPLASH;
        };
    }

    private static AssetKey enemyAssetKey(EnemyType type) {
        return switch (type) {
            case NORMAL -> AssetKey.ENEMY_NORMAL;
            case FAST -> AssetKey.ENEMY_FAST;
            case TANK -> AssetKey.ENEMY_TANK;
        };
    }

    private static void drawBasicTowerTop(Graphics2D g, Tower tower, int centerX, int centerY) {
        g.setColor(tower.getType().getBarrelColor());
        g.fillRoundRect(centerX - 5, centerY - 31, 10, 25, 5, 5);
        g.setColor(brighten(tower.getType().getBodyColor(), 55));
        g.fillOval(centerX - 11, centerY - 16, 22, 14);
    }

    private static void drawSlowTowerTop(Graphics2D g, Tower tower, int centerX, int centerY) {
        g.setColor(tower.getType().getBarrelColor());
        g.fillRoundRect(centerX - 4, centerY - 28, 8, 18, 5, 5);
        g.setColor(new Color(123, 224, 226, 185));
        int[] xs = { centerX, centerX + 11, centerX, centerX - 11 };
        int[] ys = { centerY - 30, centerY - 18, centerY - 6, centerY - 18 };
        g.fillPolygon(xs, ys, xs.length);
    }

    private static void drawSplashTowerTop(Graphics2D g, Tower tower, int centerX, int centerY) {
        g.setColor(tower.getType().getBarrelColor());
        g.fillRoundRect(centerX - 9, centerY - 27, 18, 22, 8, 8);
        g.setColor(new Color(255, 191, 100, 145));
        g.fillOval(centerX - 7, centerY - 31, 14, 10);
    }

    private static void drawCarrotHitFlash(Graphics2D g, int centerX, int centerY, double hitFlash) {
        if (hitFlash <= 0) {
            return;
        }
        double progress = Math.min(1.0, hitFlash / 0.34);
        int alpha = (int) (150 * progress);
        g.setColor(new Color(255, 104, 87, alpha));
        g.fillOval(centerX - 31, centerY - 38, 62, 72);
        g.setColor(new Color(255, 250, 235, (int) (105 * progress)));
        g.setStroke(new BasicStroke(3f));
        g.drawOval(centerX - 37, centerY - 48, 74, 90);
    }

    private static void drawMuzzleFlash(Graphics2D g, Tower tower, int centerX, int centerY, double animationSeconds) {
        if (tower.getFirePulse() <= 0) {
            return;
        }
        double progress = Math.min(1.0, tower.getFirePulse() / 0.16);
        int alpha = (int) (220 * progress);
        int flashSize = 7 + (int) (5 * progress);
        int flashX = centerX;
        int flashY = centerY - 42;
        if (tower.getType() == TowerType.BASIC) {
            flashX -= 9;
            flashY += 1;
        } else if (tower.getType() == TowerType.SPLASH) {
            flashY += 3;
        }
        int flicker = (int) Math.round(Math.sin(animationSeconds * 42) * 2);
        g.setColor(new Color(255, 242, 143, alpha));
        g.fillOval(flashX - flashSize / 2, flashY - flashSize / 2, flashSize + flicker, flashSize + flicker);
        g.setColor(new Color(255, 130, 62, Math.max(0, alpha - 60)));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(flashX - flashSize, flashY, flashX + flashSize, flashY);
        g.drawLine(flashX, flashY - flashSize, flashX, flashY + flashSize);
    }

    private static void drawProjectileTrail(Graphics2D g, Projectile projectile, int size) {
        double previousX = projectile.getPreviousX();
        double previousY = projectile.getPreviousY();
        double x = projectile.getX();
        double y = projectile.getY();
        double distance = Math.hypot(x - previousX, y - previousY);
        if (distance < 2) {
            return;
        }
        Color color = projectile.getTowerType().getBodyColor();
        g.setStroke(new BasicStroke(Math.max(3f, size * 0.55f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 95));
        g.drawLine((int) previousX, (int) previousY, (int) x, (int) y);

        int midX = (int) ((previousX + x) / 2);
        int midY = (int) ((previousY + y) / 2);
        g.setColor(new Color(255, 250, 235, 75));
        g.fillOval(midX - size / 3, midY - size / 3, Math.max(3, size * 2 / 3), Math.max(3, size * 2 / 3));
    }

    private static void drawTowerLevelBadge(Graphics2D g, int level, int centerX, int centerY) {
        g.setColor(new Color(51, 59, 69, 210));
        g.fillOval(centerX - 10, centerY - 10, 20, 20);
        g.setColor(new Color(255, 250, 235));
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 12));
        drawCenteredAt(g, String.valueOf(level), centerX, centerY + 4);
    }

    private static void drawCrate(Graphics2D g, Obstacle obstacle, int x, int y) {
        g.setPaint(new GradientPaint(x - 17, y - 17, brighten(obstacle.getBodyColor(), 24),
                x + 18, y + 18, darken(obstacle.getBodyColor(), 20)));
        g.fillRoundRect(x - 17, y - 17, 34, 34, 8, 8);
        g.setColor(obstacle.getBorderColor());
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(x - 17, y - 17, 34, 34, 8, 8);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(x - 14, y - 4, x + 14, y - 4);
        g.drawLine(x - 14, y + 7, x + 14, y + 7);
        g.drawLine(x - 5, y - 15, x - 5, y + 15);
        g.drawLine(x + 7, y - 15, x + 7, y + 15);
    }

    private static void drawRock(Graphics2D g, Obstacle obstacle, int x, int y) {
        g.setPaint(new GradientPaint(x - 19, y - 18, brighten(obstacle.getBodyColor(), 22),
                x + 17, y + 16, darken(obstacle.getBodyColor(), 18)));
        int[] xs = { x - 19, x - 8, x + 9, x + 19, x + 13, x - 4, x - 18 };
        int[] ys = { y + 5, y - 17, y - 16, y - 3, y + 17, y + 19, y + 12 };
        g.fillPolygon(xs, ys, xs.length);
        g.setColor(obstacle.getBorderColor());
        g.setStroke(new BasicStroke(3f));
        g.drawPolygon(xs, ys, xs.length);
        g.setColor(new Color(255, 255, 255, 68));
        g.drawLine(x - 7, y - 11, x + 5, y - 13);
    }

    private static void drawShadow(Graphics2D g, int centerX, int centerY, int width, int height) {
        g.setColor(SHADOW);
        g.fillOval(centerX - width / 2, centerY - height / 2, width, height);
    }

    private static boolean isRock(Color color) {
        int spread = Math.max(Math.max(color.getRed(), color.getGreen()), color.getBlue())
                - Math.min(Math.min(color.getRed(), color.getGreen()), color.getBlue());
        return spread < 24;
    }

    private static Color brighten(Color color, int amount) {
        return new Color(
                Math.min(255, color.getRed() + amount),
                Math.min(255, color.getGreen() + amount),
                Math.min(255, color.getBlue() + amount),
                color.getAlpha()
        );
    }

    private static Color darken(Color color, int amount) {
        return new Color(
                Math.max(0, color.getRed() - amount),
                Math.max(0, color.getGreen() - amount),
                Math.max(0, color.getBlue() - amount),
                color.getAlpha()
        );
    }

    private static void drawCenteredAt(Graphics2D g, String text, int centerX, int baselineY) {
        int x = centerX - g.getFontMetrics().stringWidth(text) / 2;
        g.drawString(text, x, baselineY);
    }
}
