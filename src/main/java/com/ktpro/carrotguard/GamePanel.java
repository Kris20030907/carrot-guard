package com.ktpro.carrotguard;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class GamePanel extends JPanel implements Runnable {
    public static final int TILE_SIZE = 48;
    public static final int COLS = 15;
    public static final int ROWS = 10;
    public static final int HUD_HEIGHT = 116;
    public static final int WIDTH = COLS * TILE_SIZE;
    public static final int HEIGHT = ROWS * TILE_SIZE + HUD_HEIGHT;

    private final GameState state = new GameState();
    private final Rectangle basicButton = new Rectangle(300, 14, 72, 28);
    private final Rectangle slowButton = new Rectangle(378, 14, 72, 28);
    private final Rectangle splashButton = new Rectangle(456, 14, 78, 28);
    private final Rectangle pauseButton = new Rectangle(552, 14, 78, 28);
    private final Rectangle restartButton = new Rectangle(638, 14, 72, 28);
    private final Rectangle upgradeButton = new Rectangle(WIDTH - 194, 76, 88, 30);
    private final Rectangle sellButton = new Rectangle(WIDTH - 96, 76, 76, 30);
    private Thread gameThread;
    private volatile boolean running;

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
        });
    }

    private void handlePrimaryClick(int x, int y) {
        if (y < HUD_HEIGHT) {
            if (basicButton.contains(x, y)) {
                state.selectTowerType(TowerType.BASIC);
            } else if (slowButton.contains(x, y)) {
                state.selectTowerType(TowerType.SLOW);
            } else if (splashButton.contains(x, y)) {
                state.selectTowerType(TowerType.SPLASH);
            } else if (pauseButton.contains(x, y)) {
                state.togglePaused();
            } else if (restartButton.contains(x, y)) {
                state.restart();
            } else if (upgradeButton.contains(x, y)) {
                state.tryUpgradeSelectedTower();
            } else if (sellButton.contains(x, y)) {
                state.sellSelectedTower();
            }
            return;
        }

        int col = x / TILE_SIZE;
        int row = (y - HUD_HEIGHT) / TILE_SIZE;
        state.tryBuildTower(col, row);
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
        drawEntities(g);
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

        drawBuildButtons(g);
        drawButton(g, pauseButton, state.isPaused() ? "Resume" : "Pause", !state.isGameOver() && !state.isWon());
        drawButton(g, restartButton, "Restart", true);

        drawTowerActions(g);
    }

    private void drawBuildButtons(Graphics2D g) {
        drawTypeButton(g, basicButton, TowerType.BASIC);
        drawTypeButton(g, slowButton, TowerType.SLOW);
        drawTypeButton(g, splashButton, TowerType.SPLASH);

        TowerType selectedType = state.getSelectedTowerType();
        g.setColor(new Color(255, 250, 235));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        String progress = state.getEnemiesSpawnedInWave() + "/" + state.getWaveEnemyCount();
        g.drawString("Build: " + selectedType.getDisplayName() + " / " + selectedType.getCost(), 330, 62);
        g.drawString("Enemies: " + progress, 500, 62);
    }

    private void drawTypeButton(Graphics2D g, Rectangle rect, TowerType type) {
        boolean selected = state.getSelectedTowerType() == type;
        g.setColor(selected ? new Color(247, 216, 112) : new Color(82, 105, 86));
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);
        g.setColor(type.getBodyColor());
        g.fillOval(rect.x + 7, rect.y + 7, 14, 14);
        g.setColor(selected ? new Color(61, 55, 39) : new Color(236, 240, 224));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.drawString(type.getDisplayName(), rect.x + 25, rect.y + 18);
    }

    private void drawTowerActions(Graphics2D g) {
        Tower selectedTower = state.getSelectedTower();
        boolean hasSelection = selectedTower != null;

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        if (hasSelection) {
            String text = selectedTower.getType().getDisplayName() + " L" + selectedTower.getLevel()
                    + "  DMG " + (int) selectedTower.getDamage()
                    + "  RNG " + (int) selectedTower.getRange();
            g.setColor(new Color(255, 250, 235));
            g.drawString(text, 20, 96);
        }

        drawButton(g, upgradeButton,
                hasSelection && selectedTower.canUpgrade() ? "Upgrade " + selectedTower.getUpgradeCost() : "Max/None",
                hasSelection && selectedTower.canUpgrade() && state.getCoins() >= selectedTower.getUpgradeCost());
        drawButton(g, sellButton,
                hasSelection ? "Sell " + selectedTower.getSellValue() : "Sell",
                hasSelection);
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

    private void drawEntities(Graphics2D g) {
        for (Tower tower : state.getTowers()) {
            int centerX = tower.getCol() * TILE_SIZE + TILE_SIZE / 2;
            int centerY = HUD_HEIGHT + tower.getRow() * TILE_SIZE + TILE_SIZE / 2;

            if (tower == state.getSelectedTower()) {
                g.setColor(new Color(255, 246, 164, 90));
                int range = (int) tower.getRange();
                g.fillOval(centerX - range, centerY - range, range * 2, range * 2);
                g.setColor(new Color(255, 246, 164));
                g.setStroke(new BasicStroke(2f));
                g.drawOval(centerX - range, centerY - range, range * 2, range * 2);
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
            g.setColor(projectile.getTowerType().getBodyColor().brighter());
            g.fillOval((int) projectile.getX() - 5, (int) projectile.getY() - 5, 10, 10);
            g.setColor(projectile.getTowerType().getBarrelColor());
            g.drawOval((int) projectile.getX() - 5, (int) projectile.getY() - 5, 10, 10);
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
}
