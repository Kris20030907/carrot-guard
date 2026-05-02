package com.ktpro.carrotguard;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public final class GamePanel extends JPanel implements Runnable {
    public static final int TILE_SIZE = 48;
    public static final int COLS = 15;
    public static final int ROWS = 10;
    public static final int HUD_HEIGHT = 116;
    public static final int WIDTH = COLS * TILE_SIZE;
    public static final int HEIGHT = ROWS * TILE_SIZE + HUD_HEIGHT;

    private final GameState state = new GameState();
    private final Rectangle pauseButton = new Rectangle(552, 14, 78, 28);
    private final Rectangle restartButton = new Rectangle(638, 14, 72, 28);
    private Thread gameThread;
    private volatile boolean running;
    private int hoverCol = -1;
    private int hoverRow = -1;

    public GamePanel() {
        setBackground(new Color(247, 239, 218));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1) {
                    handlePrimaryClick(event.getX(), event.getY());
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hoverCol = -1;
                hoverRow = -1;
                repaint();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                updateHoverTile(event.getX(), event.getY());
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                updateHoverTile(event.getX(), event.getY());
            }
        });
    }

    private void handlePrimaryClick(int x, int y) {
        if (handleContextAction(x, y)) {
            return;
        }
        if (y < HUD_HEIGHT) {
            if (pauseButton.contains(x, y)) {
                state.togglePaused();
            } else if (restartButton.contains(x, y)) {
                state.restart();
            }
            return;
        }

        int col = x / TILE_SIZE;
        int row = (y - HUD_HEIGHT) / TILE_SIZE;
        state.selectMapTile(col, row);
    }

    private boolean handleContextAction(int x, int y) {
        if (state.hasSelectedBuildTile()) {
            TowerType[] types = TowerType.values();
            for (int i = 0; i < types.length; i++) {
                if (buildOptionRect(i, types.length).contains(x, y)) {
                    state.tryBuildSelectedTower(types[i]);
                    return true;
                }
            }
        }

        Tower selectedTower = state.getSelectedTower();
        if (selectedTower != null) {
            TowerUpgradeType[] upgrades = TowerUpgradeType.values();
            for (int i = 0; i < upgrades.length; i++) {
                if (upgradeOptionRect(i, upgrades.length).contains(x, y)) {
                    state.tryUpgradeSelectedTower(upgrades[i]);
                    return true;
                }
            }
            if (sellOptionRect().contains(x, y)) {
                state.sellSelectedTower();
                return true;
            }
        }
        return false;
    }

    private void updateHoverTile(int x, int y) {
        int nextCol = -1;
        int nextRow = -1;
        if (y >= HUD_HEIGHT) {
            nextCol = x / TILE_SIZE;
            nextRow = (y - HUD_HEIGHT) / TILE_SIZE;
            if (nextCol < 0 || nextCol >= COLS || nextRow < 0 || nextRow >= ROWS) {
                nextCol = -1;
                nextRow = -1;
            }
        }
        if (hoverCol != nextCol || hoverRow != nextRow) {
            hoverCol = nextCol;
            hoverRow = nextRow;
            repaint();
        }
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        gameThread = new Thread(this, "carrot-guard-loop");
        gameThread.start();
    }

    @Override
    public void run() {
        long lastFrame = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            double deltaSeconds = (now - lastFrame) / 1_000_000_000.0;
            lastFrame = now;

            state.update(Math.min(deltaSeconds, 0.05));
            repaint();

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawHud(g);
        drawMap(g);
        drawBuildPreview(g);
        drawEntities(g);
        drawContextMenu(g);
        drawOverlay(g);
        g.dispose();
    }

    private void drawHud(Graphics2D g) {
        g.setColor(new Color(58, 79, 65));
        g.fillRect(0, 0, WIDTH, HUD_HEIGHT);

        g.setColor(new Color(255, 250, 235));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString("Carrot Guard", 20, 30);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        g.drawString("Coins: " + state.getCoins(), 20, 62);
        g.drawString("Lives: " + state.getLives(), 140, 62);
        g.drawString("Wave: " + state.getWave() + "/" + state.getMaxWave(), 250, 62);

        drawSelectionHint(g);
        drawButton(g, pauseButton, state.isPaused() ? "Resume" : "Pause", !state.isGameOver() && !state.isWon());
        drawButton(g, restartButton, "Restart", true);
    }

    private void drawSelectionHint(Graphics2D g) {
        g.setColor(new Color(255, 250, 235));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        String progress = state.getEnemiesSpawnedInWave() + "/" + state.getWaveEnemyCount();
        Tower selectedTower = state.getSelectedTower();
        if (state.hasSelectedBuildTile()) {
            g.drawString("Build at: " + (state.getSelectedBuildCol() + 1) + "," + (state.getSelectedBuildRow() + 1), 330, 62);
        } else if (selectedTower != null) {
            String text = selectedTower.getType().getDisplayName() + " L" + selectedTower.getLevel()
                    + "  DMG " + (int) selectedTower.getDamage()
                    + "  SPD " + String.format("%.2f", selectedTower.getFireInterval())
                    + "  RNG " + (int) selectedTower.getRange()
                    + "  [" + selectedTower.getUpgradeLevel(TowerUpgradeType.DAMAGE)
                    + "/" + selectedTower.getUpgradeLevel(TowerUpgradeType.SPEED)
                    + "/" + selectedTower.getUpgradeLevel(TowerUpgradeType.RANGE) + "]";
            g.drawString(text, 20, 96);
        } else {
            g.drawString("Build: select a grass tile", 330, 62);
        }
        g.drawString("Enemies: " + progress, 500, 62);
    }

    private void drawButton(Graphics2D g, Rectangle rect, String label, boolean enabled) {
        g.setColor(enabled ? new Color(238, 197, 92) : new Color(104, 120, 107));
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);
        g.setColor(enabled ? new Color(70, 59, 42) : new Color(201, 207, 194));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        FontMetrics metrics = g.getFontMetrics();
        int textX = rect.x + (rect.width - metrics.stringWidth(label)) / 2;
        int textY = rect.y + (rect.height + metrics.getAscent() - metrics.getDescent()) / 2;
        g.drawString(label, textX, textY);
    }

    private void drawMap(Graphics2D g) {
        int top = HUD_HEIGHT;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = col * TILE_SIZE;
                int y = top + row * TILE_SIZE;
                boolean path = state.getPath().containsTile(col, row);
                g.setColor(path ? new Color(211, 178, 119) : new Color(133, 184, 113));
                g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                g.setColor(path ? new Color(181, 147, 93) : new Color(108, 157, 93));
                g.drawRect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }

        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(238, 211, 153));
        state.getPath().draw(g, HUD_HEIGHT);

        int[] goal = state.getPath().getGoalTile();
        int carrotX = goal[0] * TILE_SIZE + TILE_SIZE / 2;
        int carrotY = HUD_HEIGHT + goal[1] * TILE_SIZE + TILE_SIZE / 2;
        g.setColor(new Color(255, 145, 63));
        g.fillOval(carrotX - 16, carrotY - 18, 32, 38);
        g.setColor(new Color(76, 151, 69));
        g.fillOval(carrotX - 6, carrotY - 29, 18, 16);
    }

    private void drawBuildPreview(Graphics2D g) {
        if (hoverCol < 0 || hoverRow < 0) {
            if (state.hasSelectedBuildTile()) {
                drawSelectedBuildTile(g);
            }
            return;
        }

        int x = hoverCol * TILE_SIZE;
        int y = HUD_HEIGHT + hoverRow * TILE_SIZE;
        int centerX = x + TILE_SIZE / 2;
        int centerY = y + TILE_SIZE / 2;
        Tower hoverTower = state.getTowerAt(hoverCol, hoverRow);
        Obstacle hoverObstacle = state.getObstacleAt(hoverCol, hoverRow);

        if (hoverTower != null) {
            drawRange(g, centerX, centerY, (int) hoverTower.getRange(), new Color(255, 246, 164, 80), new Color(255, 246, 164));
            g.setColor(new Color(255, 246, 164, 80));
            g.fillRect(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4);
            return;
        }
        if (hoverObstacle != null) {
            g.setColor(new Color(255, 246, 164, 70));
            g.fillRect(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4);
            g.setColor(new Color(255, 246, 164));
            g.setStroke(new BasicStroke(2f));
            g.drawRect(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4);
            return;
        }

        boolean selectedTile = state.hasSelectedBuildTile()
                && state.getSelectedBuildCol() == hoverCol
                && state.getSelectedBuildRow() == hoverRow;
        boolean canBuild = canAnyTowerBuildAt(hoverCol, hoverRow);
        Color fill = canBuild ? new Color(91, 207, 117, selectedTile ? 135 : 70) : new Color(220, 76, 72, 110);
        Color border = canBuild ? new Color(111, 242, 140) : new Color(255, 116, 108);

        g.setColor(fill);
        g.fillRect(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4);
        g.setColor(border);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4);

        if (canBuild) {
            g.setColor(new Color(255, 250, 235));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            drawCenteredAt(g, "+", centerX, centerY + 4);
        } else {
            g.setColor(new Color(255, 245, 238));
            g.setStroke(new BasicStroke(3f));
            g.drawLine(x + 14, y + 14, x + TILE_SIZE - 14, y + TILE_SIZE - 14);
            g.drawLine(x + TILE_SIZE - 14, y + 14, x + 14, y + TILE_SIZE - 14);
        }

        if (state.hasSelectedBuildTile() && !selectedTile) {
            drawSelectedBuildTile(g);
        }
    }

    private void drawSelectedBuildTile(Graphics2D g) {
        int col = state.getSelectedBuildCol();
        int row = state.getSelectedBuildRow();
        int x = col * TILE_SIZE;
        int y = HUD_HEIGHT + row * TILE_SIZE;
        boolean canBuildAny = canAnyTowerBuildAt(col, row);
        g.setColor(canBuildAny ? new Color(91, 207, 117, 130) : new Color(220, 76, 72, 130));
        g.fillRect(x + 3, y + 3, TILE_SIZE - 6, TILE_SIZE - 6);
        g.setColor(canBuildAny ? new Color(111, 242, 140) : new Color(255, 116, 108));
        g.setStroke(new BasicStroke(3f));
        g.drawRect(x + 3, y + 3, TILE_SIZE - 6, TILE_SIZE - 6);
    }

    private boolean canAnyTowerBuildAt(int col, int row) {
        for (TowerType type : TowerType.values()) {
            if (state.canBuildTowerAt(col, row, type)) {
                return true;
            }
        }
        return false;
    }

    private void drawContextMenu(Graphics2D g) {
        if (state.hasSelectedBuildTile()) {
            TowerType[] types = TowerType.values();
            for (int i = 0; i < types.length; i++) {
                TowerType type = types[i];
                drawContextButton(g, buildOptionRect(i, types.length), type.getDisplayName(), String.valueOf(type.getCost()),
                        state.canBuildSelectedTower(type), type.getBodyColor());
            }
            return;
        }

        Tower selectedTower = state.getSelectedTower();
        if (selectedTower == null) {
            return;
        }

        TowerUpgradeType[] upgrades = TowerUpgradeType.values();
        for (int i = 0; i < upgrades.length; i++) {
            TowerUpgradeType upgradeType = upgrades[i];
            boolean canUpgrade = selectedTower.canUpgrade(upgradeType);
            boolean canAffordUpgrade = canUpgrade && state.getCoins() >= selectedTower.getUpgradeCost(upgradeType);
            drawContextButton(g, upgradeOptionRect(i, upgrades.length),
                    upgradeType.getDisplayName() + " " + selectedTower.getUpgradeLevel(upgradeType),
                    canUpgrade ? String.valueOf(selectedTower.getUpgradeCost(upgradeType)) : "MAX",
                    canAffordUpgrade,
                    selectedTower.getType().getBodyColor());
        }
        drawContextButton(g, sellOptionRect(), "Sell", String.valueOf(selectedTower.getSellValue()), true, new Color(238, 197, 92));
    }

    private void drawContextButton(Graphics2D g, Rectangle rect, String label, String detail, boolean enabled, Color accent) {
        g.setColor(enabled ? new Color(247, 216, 112) : new Color(157, 82, 76));
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);
        g.setColor(accent);
        g.fillOval(rect.x + 7, rect.y + 8, 14, 14);
        g.setColor(enabled ? new Color(61, 55, 39) : new Color(236, 240, 224));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.drawString(label, rect.x + 25, rect.y + 14);
        if (!detail.isEmpty()) {
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g.drawString(detail, rect.x + 25, rect.y + 26);
        }
    }

    private Rectangle buildOptionRect(int index, int count) {
        Point origin = contextMenuOrigin(state.getSelectedBuildCol(), state.getSelectedBuildRow(), count * 78 + (count - 1) * 6, 32);
        return new Rectangle(origin.x + index * 84, origin.y, 78, 32);
    }

    private Rectangle upgradeOptionRect(int index, int count) {
        Tower tower = state.getSelectedTower();
        Point origin = contextMenuOrigin(tower.getCol(), tower.getRow(), count * 70 + (count - 1) * 6, 70);
        return new Rectangle(origin.x + index * 76, origin.y, 70, 32);
    }

    private Rectangle sellOptionRect() {
        Tower tower = state.getSelectedTower();
        Point origin = contextMenuOrigin(tower.getCol(), tower.getRow(), 72, 70);
        return new Rectangle(origin.x, origin.y + 38, 72, 32);
    }

    private Point contextMenuOrigin(int col, int row, int width, int height) {
        int tileX = col * TILE_SIZE;
        int tileY = HUD_HEIGHT + row * TILE_SIZE;
        int x = tileX + TILE_SIZE + 8;
        if (x + width > WIDTH - 8) {
            x = tileX - width - 8;
        }
        x = Math.max(8, Math.min(x, WIDTH - width - 8));

        int y = tileY + 8;
        if (y + height > HEIGHT - 8) {
            y = tileY - height - 8;
        }
        y = Math.max(HUD_HEIGHT + 8, Math.min(y, HEIGHT - height - 8));
        return new Point(x, y);
    }

    private void drawEntities(Graphics2D g) {
        for (Obstacle obstacle : state.getObstacles()) {
            int x = (int) obstacle.getX();
            int y = (int) obstacle.getY();
            g.setColor(obstacle.getBodyColor());
            g.fillRoundRect(x - 16, y - 16, 32, 32, 8, 8);
            g.setColor(obstacle.getBorderColor());
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(x - 16, y - 16, 32, 32, 8, 8);

            int barWidth = 34;
            int healthWidth = (int) (barWidth * obstacle.getHealthRatio());
            g.setColor(new Color(73, 43, 35));
            g.fillRect(x - barWidth / 2, y - 27, barWidth, 5);
            g.setColor(new Color(247, 197, 74));
            g.fillRect(x - barWidth / 2, y - 27, healthWidth, 5);
        }

        for (Tower tower : state.getTowers()) {
            int centerX = tower.getCol() * TILE_SIZE + TILE_SIZE / 2;
            int centerY = HUD_HEIGHT + tower.getRow() * TILE_SIZE + TILE_SIZE / 2;

            if (tower == state.getSelectedTower()) {
                drawRange(g, centerX, centerY, (int) tower.getRange(), new Color(255, 246, 164, 90), new Color(255, 246, 164));
            }
            if (tower.getUpgradePulse() > 0) {
                float pulse = (float) Math.min(1.0, tower.getUpgradePulse() / 0.55);
                int glow = 28 + (int) ((1.0f - pulse) * 14);
                g.setColor(new Color(255, 248, 164, (int) (150 * pulse)));
                g.setStroke(new BasicStroke(3f));
                g.drawOval(centerX - glow, centerY - glow, glow * 2, glow * 2);
            }

            g.setColor(tower == state.getSelectedTower() ? tower.getType().getBodyColor().brighter() : tower.getType().getBodyColor());
            g.fillOval(centerX - 17, centerY - 17, 34, 34);
            g.setColor(tower.getType().getBarrelColor());
            g.fillRect(centerX - 4, centerY - 28, 8, 22);
            g.setColor(new Color(255, 250, 235));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            drawCenteredAt(g, String.valueOf(tower.getLevel()), centerX, centerY + 5);
        }

        for (Projectile projectile : state.getProjectiles()) {
            int size = projectile.getTowerType().hasSplashEffect() ? 13 : 10;
            g.setColor(projectile.getTowerType().getBodyColor().brighter());
            g.fillOval((int) projectile.getX() - size / 2, (int) projectile.getY() - size / 2, size, size);
            g.setColor(projectile.getTowerType().getBarrelColor());
            g.drawOval((int) projectile.getX() - size / 2, (int) projectile.getY() - size / 2, size, size);
        }

        for (HitEffect effect : state.getHitEffects()) {
            double progress = effect.getProgress();
            int radius = (int) (8 + effect.getRadius() * progress);
            int alpha = Math.max(0, (int) (155 * (1.0 - progress)));
            Color effectColor = effect.getTowerType().getBodyColor();
            g.setColor(new Color(effectColor.getRed(), effectColor.getGreen(), effectColor.getBlue(), alpha));
            g.setStroke(new BasicStroke(effect.getTowerType().hasSplashEffect() ? 3f : 2f));
            g.drawOval((int) effect.getX() - radius, (int) effect.getY() - radius, radius * 2, radius * 2);
        }

        for (Enemy enemy : state.getEnemies()) {
            int x = (int) enemy.getX();
            int y = (int) enemy.getY();
            int radius = enemy.getType().getRadius();
            g.setColor(enemy.getType().getBodyColor());
            g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            g.setColor(enemy.getType().getBorderColor());
            g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            if (enemy.isSlowed()) {
                g.setColor(new Color(148, 222, 233, 160));
                g.drawOval(x - radius - 4, y - radius - 4, (radius + 4) * 2, (radius + 4) * 2);
            }
            g.setColor(new Color(255, 250, 235));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            drawCenteredAt(g, enemy.getType().getDisplayName().substring(0, 1), x, y + 4);

            int barWidth = radius * 2 + 4;
            int healthWidth = (int) (barWidth * enemy.getHealthRatio());
            g.setColor(new Color(73, 43, 35));
            g.fillRect(x - barWidth / 2, y - radius - 10, barWidth, 5);
            g.setColor(new Color(97, 201, 98));
            g.fillRect(x - barWidth / 2, y - radius - 10, healthWidth, 5);
        }
    }

    private void drawOverlay(Graphics2D g) {
        if (!state.isGameOver() && !state.isWon() && !state.isPaused()) {
            return;
        }

        g.setColor(new Color(0, 0, 0, state.isPaused() ? 95 : 150));
        g.fillRect(0, HUD_HEIGHT, WIDTH, HEIGHT - HUD_HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 38));
        if (state.isWon()) {
            drawCentered(g, "Victory", HUD_HEIGHT + 220);
        } else if (state.isGameOver()) {
            drawCentered(g, "Game Over", HUD_HEIGHT + 220);
        } else {
            drawCentered(g, "Paused", HUD_HEIGHT + 220);
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        drawCentered(g, "Use the top buttons to resume or restart", HUD_HEIGHT + 258);
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics metrics = g.getFontMetrics();
        int x = (WIDTH - metrics.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    private void drawCenteredAt(Graphics2D g, String text, int centerX, int baselineY) {
        FontMetrics metrics = g.getFontMetrics();
        int x = centerX - metrics.stringWidth(text) / 2;
        g.drawString(text, x, baselineY);
    }

    private void drawRange(Graphics2D g, int centerX, int centerY, int range, Color fill, Color border) {
        g.setColor(fill);
        g.fillOval(centerX - range, centerY - range, range * 2, range * 2);
        g.setColor(border);
        g.setStroke(new BasicStroke(2f));
        g.drawOval(centerX - range, centerY - range, range * 2, range * 2);
    }
}
