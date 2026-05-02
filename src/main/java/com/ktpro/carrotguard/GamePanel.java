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
    public static final int HUD_HEIGHT = 72;
    public static final int WIDTH = COLS * TILE_SIZE;
    public static final int HEIGHT = ROWS * TILE_SIZE + HUD_HEIGHT;

    private final GameState state = new GameState();
    private final Rectangle upgradeButton = new Rectangle(WIDTH - 210, 20, 88, 32);
    private final Rectangle sellButton = new Rectangle(WIDTH - 112, 20, 82, 32);
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
            if (upgradeButton.contains(x, y)) {
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
        g.drawString("Coins: " + state.getCoins(), 20, 56);
        g.drawString("Lives: " + state.getLives(), 140, 56);
        g.drawString("Wave: " + state.getWave(), 250, 56);
        g.drawString("Build: " + state.getTowerCost() + " coins", 360, 56);

        drawTowerActions(g);
    }

    private void drawTowerActions(Graphics2D g) {
        Tower selectedTower = state.getSelectedTower();
        boolean hasSelection = selectedTower != null;

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        if (hasSelection) {
            String text = "Tower L" + selectedTower.getLevel()
                    + "  DMG " + (int) selectedTower.getDamage()
                    + "  RNG " + (int) selectedTower.getRange();
            g.setColor(new Color(255, 250, 235));
            g.drawString(text, WIDTH - 315, 15);
        }

        drawButton(g, upgradeButton,
                hasSelection && selectedTower.canUpgrade() ? "Upgrade " + selectedTower.getUpgradeCost() : "Max/None",
                hasSelection && selectedTower.canUpgrade() && state.getCoins() >= selectedTower.getUpgradeCost());
        drawButton(g, sellButton,
                hasSelection ? "Sell " + selectedTower.getSellValue(state.getTowerCost()) : "Sell",
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

            g.setColor(tower == state.getSelectedTower() ? new Color(63, 95, 151) : new Color(50, 80, 126));
            g.fillOval(centerX - 17, centerY - 17, 34, 34);
            g.setColor(new Color(31, 49, 79));
            g.fillRect(centerX - 4, centerY - 28, 8, 22);
            g.setColor(new Color(255, 250, 235));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            drawCenteredAt(g, String.valueOf(tower.getLevel()), centerX, centerY + 5);
        }

        for (Projectile projectile : state.getProjectiles()) {
            g.setColor(new Color(255, 230, 79));
            g.fillOval((int) projectile.getX() - 5, (int) projectile.getY() - 5, 10, 10);
            g.setColor(new Color(171, 123, 36));
            g.drawOval((int) projectile.getX() - 5, (int) projectile.getY() - 5, 10, 10);
        }

        for (Enemy enemy : state.getEnemies()) {
            int x = (int) enemy.getX();
            int y = (int) enemy.getY();
            g.setColor(new Color(143, 76, 57));
            g.fillOval(x - 15, y - 15, 30, 30);
            g.setColor(new Color(95, 49, 41));
            g.drawOval(x - 15, y - 15, 30, 30);

            int barWidth = 32;
            int healthWidth = (int) (barWidth * enemy.getHealthRatio());
            g.setColor(new Color(73, 43, 35));
            g.fillRect(x - barWidth / 2, y - 25, barWidth, 5);
            g.setColor(new Color(97, 201, 98));
            g.fillRect(x - barWidth / 2, y - 25, healthWidth, 5);
        }
    }

    private void drawOverlay(Graphics2D g) {
        if (!state.isGameOver()) {
            return;
        }

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, HUD_HEIGHT, WIDTH, HEIGHT - HUD_HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 42));
        drawCentered(g, "Game Over", HUD_HEIGHT + 220);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        drawCentered(g, "Restart the app to try again", HUD_HEIGHT + 258);
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
