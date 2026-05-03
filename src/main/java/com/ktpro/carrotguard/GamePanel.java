package com.ktpro.carrotguard;

import javax.swing.JPanel;
import javax.swing.Timer;
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
import java.util.List;

public final class GamePanel extends JPanel {
    public static final int TILE_SIZE = 48;
    public static final int COLS = 15;
    public static final int ROWS = 10;
    public static final int HUD_HEIGHT = 116;
    public static final int WIDTH = COLS * TILE_SIZE;
    public static final int HEIGHT = ROWS * TILE_SIZE + HUD_HEIGHT;

    private enum PanelScreen {
        MENU,
        PLAYING
    }

    private final GameState state = new GameState();
    private final GameProgress progress;
    private final SoundEffects soundEffects = new SoundEffects();
    private final List<Integer> levelNumbers = LevelConfig.availableLevelNumbers();
    private final Rectangle menuButton = new Rectangle(294, 14, 78, 28);
    private final Rectangle speedButton = new Rectangle(380, 14, 78, 28);
    private final Rectangle nextButton = new Rectangle(466, 14, 78, 28);
    private final Rectangle pauseButton = new Rectangle(552, 14, 78, 28);
    private final Rectangle restartButton = new Rectangle(638, 14, 72, 28);
    private final Timer gameTimer = new Timer(16, event -> tick());
    private PanelScreen screen = PanelScreen.MENU;
    private long lastFrameNanos;
    private int hoverCol = -1;
    private int hoverRow = -1;
    private int hoverLevelIndex = -1;
    private int observedHitEvents;
    private int observedLeakEvents;
    private int observedClearedObstacleEvents;
    private boolean victoryRecorded;

    public GamePanel() {
        this(GameProgress.loadDefault());
    }

    GamePanel(GameProgress progress) {
        this.progress = progress;
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
                hoverLevelIndex = -1;
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
        if (screen == PanelScreen.MENU) {
            handleMenuClick(x, y);
            return;
        }
        if (handleContextAction(x, y)) {
            return;
        }
        Tower selectedTower = state.getSelectedTower();
        if (selectedTower != null && towerInfoPanelRect(selectedTower).contains(x, y)) {
            return;
        }
        if (state.isCarrotSelected() && carrotInfoPanelRect().contains(x, y)) {
            return;
        }
        if (y < HUD_HEIGHT) {
            if (menuButton.contains(x, y)) {
                soundEffects.play(SoundEffect.CLICK);
                showMenu();
            } else if (speedButton.contains(x, y)) {
                soundEffects.play(SoundEffect.CLICK);
                state.cycleSpeedMultiplier();
            } else if (nextButton.contains(x, y) && state.isWon() && state.hasNextLevel()) {
                soundEffects.play(SoundEffect.CLICK);
                recordVictoryIfNeeded();
                if (state.advanceToNextLevel()) {
                    victoryRecorded = false;
                    syncSoundCounters();
                }
            } else if (pauseButton.contains(x, y)) {
                soundEffects.play(SoundEffect.CLICK);
                state.togglePaused();
            } else if (restartButton.contains(x, y)) {
                soundEffects.play(SoundEffect.CLICK);
                state.restart();
                victoryRecorded = false;
                syncSoundCounters();
            }
            return;
        }

        int col = x / TILE_SIZE;
        int row = (y - HUD_HEIGHT) / TILE_SIZE;
        state.selectMapTile(col, row);
    }

    private void handleMenuClick(int x, int y) {
        for (int i = 0; i < levelNumbers.size(); i++) {
            int levelNumber = levelNumbers.get(i);
            if (progress.isUnlocked(levelNumber) && levelCardRect(i, levelNumbers.size()).contains(x, y)) {
                soundEffects.play(SoundEffect.CLICK);
                startLevel(levelNumbers.get(i));
                return;
            }
        }
    }

    private boolean handleContextAction(int x, int y) {
        if (state.hasSelectedBuildTile()) {
            TowerType[] types = TowerType.values();
            for (int i = 0; i < types.length; i++) {
                if (buildOptionRect(i, types.length).contains(x, y)) {
                    soundEffects.play(state.tryBuildSelectedTower(types[i]) ? SoundEffect.BUILD : SoundEffect.CLICK);
                    return true;
                }
            }
        }

        Tower selectedTower = state.getSelectedTower();
        if (selectedTower != null) {
            TowerUpgradeType[] upgrades = TowerUpgradeType.values();
            for (int i = 0; i < upgrades.length; i++) {
                if (upgradeOptionRect(i, upgrades.length).contains(x, y)) {
                    soundEffects.play(state.tryUpgradeSelectedTower(upgrades[i]) ? SoundEffect.UPGRADE : SoundEffect.CLICK);
                    return true;
                }
            }
            if (sellOptionRect().contains(x, y)) {
                soundEffects.play(state.sellSelectedTower() ? SoundEffect.SELL : SoundEffect.CLICK);
                return true;
            }
        }
        return false;
    }

    private void updateHoverTile(int x, int y) {
        if (screen == PanelScreen.MENU) {
            updateMenuHover(x, y);
            return;
        }
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

    private void updateMenuHover(int x, int y) {
        int nextHoverLevel = -1;
        for (int i = 0; i < levelNumbers.size(); i++) {
            if (levelCardRect(i, levelNumbers.size()).contains(x, y)) {
                nextHoverLevel = i;
                break;
            }
        }
        if (hoverLevelIndex != nextHoverLevel) {
            hoverLevelIndex = nextHoverLevel;
            repaint();
        }
    }

    public void start() {
        if (gameTimer.isRunning()) {
            return;
        }
        lastFrameNanos = System.nanoTime();
        gameTimer.start();
    }

    private void tick() {
        long now = System.nanoTime();
        double deltaSeconds = (now - lastFrameNanos) / 1_000_000_000.0;
        lastFrameNanos = now;
        if (screen == PanelScreen.PLAYING) {
            boolean wasWon = state.isWon();
            boolean wasGameOver = state.isGameOver();
            state.update(Math.min(deltaSeconds, 0.05) * state.getSpeedMultiplier());
            playStateSounds();
            if (!wasWon && state.isWon()) {
                recordVictoryIfNeeded();
                soundEffects.play(SoundEffect.VICTORY);
            }
            if (!wasGameOver && state.isGameOver()) {
                soundEffects.play(SoundEffect.DEFEAT);
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (screen == PanelScreen.MENU) {
            drawMainMenu(g);
            g.dispose();
            return;
        }
        drawHud(g);
        drawMap(g);
        drawBuildPreview(g);
        drawEntities(g);
        drawTowerInfoPanel(g);
        drawCarrotInfoPanel(g);
        drawContextMenu(g);
        drawOverlay(g);
        g.dispose();
    }

    void startLevel(int levelNumber) {
        state.startLevel(levelNumber);
        screen = PanelScreen.PLAYING;
        victoryRecorded = false;
        syncSoundCounters();
        hoverLevelIndex = -1;
        hoverCol = -1;
        hoverRow = -1;
    }

    void showMenu() {
        screen = PanelScreen.MENU;
        hoverCol = -1;
        hoverRow = -1;
        hoverLevelIndex = -1;
    }

    boolean isShowingMenu() {
        return screen == PanelScreen.MENU;
    }

    int getCurrentLevelNumber() {
        return state.getLevelNumber();
    }

    int getBestStars(int levelNumber) {
        return progress.getBestStars(levelNumber);
    }

    boolean isLevelUnlocked(int levelNumber) {
        return progress.isUnlocked(levelNumber);
    }

    private void recordVictoryIfNeeded() {
        if (!victoryRecorded && state.isWon()) {
            progress.recordVictory(state.getLevelNumber(), state.getStarRating(), state.hasNextLevel());
            victoryRecorded = true;
        }
    }

    private void playStateSounds() {
        if (state.getHitEventCount() > observedHitEvents) {
            soundEffects.play(SoundEffect.HIT);
            observedHitEvents = state.getHitEventCount();
        }
        if (state.getLeakEventCount() > observedLeakEvents) {
            soundEffects.play(SoundEffect.LEAK);
            observedLeakEvents = state.getLeakEventCount();
        }
        if (state.getClearedObstacleEventCount() > observedClearedObstacleEvents) {
            soundEffects.play(SoundEffect.SELL);
            observedClearedObstacleEvents = state.getClearedObstacleEventCount();
        }
    }

    private void syncSoundCounters() {
        observedHitEvents = state.getHitEventCount();
        observedLeakEvents = state.getLeakEventCount();
        observedClearedObstacleEvents = state.getClearedObstacleEventCount();
    }

    private void drawMainMenu(Graphics2D g) {
        for (int row = 0; row < ROWS + 3; row++) {
            for (int col = 0; col < COLS; col++) {
                GameArt.drawGrassTile(g, col * TILE_SIZE, row * TILE_SIZE, col, row);
            }
        }

        g.setColor(new Color(47, 68, 55, 238));
        g.fillRoundRect(64, 62, WIDTH - 128, 104, 14, 14);
        g.setColor(new Color(255, 250, 235, 210));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(64, 62, WIDTH - 128, 104, 14, 14);

        g.setColor(new Color(255, 250, 235));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 38));
        drawCentered(g, "Carrot Guard", 112);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        drawCentered(g, "Level Select", 143);

        drawLevelRoute(g);
        for (int i = 0; i < levelNumbers.size(); i++) {
            drawLevelCard(g, i, levelNumbers.get(i));
        }
    }

    private void drawLevelRoute(Graphics2D g) {
        if (levelNumbers.size() < 2) {
            return;
        }
        for (int i = 0; i < levelNumbers.size() - 1; i++) {
            Rectangle from = levelCardRect(i, levelNumbers.size());
            Rectangle to = levelCardRect(i + 1, levelNumbers.size());
            int x1 = from.x + from.width / 2;
            int y1 = from.y + from.height / 2;
            int x2 = to.x + to.width / 2;
            int y2 = to.y + to.height / 2;
            boolean unlockedPath = progress.isUnlocked(levelNumbers.get(i + 1));

            g.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(unlockedPath ? new Color(221, 181, 103, 185) : new Color(101, 111, 94, 150));
            g.drawLine(x1, y1, x2, y2);
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(unlockedPath ? new Color(255, 235, 169, 190) : new Color(169, 176, 159, 160));
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawLevelCard(Graphics2D g, int index, int levelNumber) {
        Rectangle rect = levelCardRect(index, levelNumbers.size());
        boolean hovered = hoverLevelIndex == index;
        boolean unlocked = progress.isUnlocked(levelNumber);
        LevelConfig config = LevelConfig.load(levelNumber);

        g.setColor(new Color(57, 43, 35, hovered ? 95 : 65));
        g.fillOval(rect.x + 12, rect.y + rect.height - 5, rect.width - 24, 18);
        g.setColor(unlocked
                ? (hovered ? new Color(250, 216, 114) : new Color(255, 250, 235, 236))
                : new Color(151, 158, 142, 236));
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
        g.setColor(unlocked ? new Color(83, 96, 70, hovered ? 210 : 150) : new Color(85, 91, 82, 180));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);

        g.setColor(unlocked ? new Color(58, 79, 65) : new Color(71, 76, 69));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        drawCenteredAt(g, "Level " + levelNumber, rect.x + rect.width / 2, rect.y + 34);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        drawCenteredAt(g, "Waves " + config.getWaveCount(), rect.x + rect.width / 2, rect.y + 62);
        drawCenteredAt(g, "Coins " + config.getStartingCoins() + "  HP " + config.getStartingLives(),
                rect.x + rect.width / 2, rect.y + 84);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        drawCenteredAt(g, "Stars " + progress.getBestStars(levelNumber) + "/3", rect.x + rect.width / 2, rect.y + 106);

        g.setColor(unlocked ? new Color(58, 79, 65) : new Color(88, 93, 84));
        g.fillRoundRect(rect.x + 38, rect.y + rect.height - 34, rect.width - 76, 24, 8, 8);
        g.setColor(new Color(255, 250, 235));
        drawCenteredAt(g, unlocked ? "Start" : "Locked", rect.x + rect.width / 2, rect.y + rect.height - 17);
    }

    private Rectangle levelCardRect(int index, int count) {
        int cardWidth = 178;
        int cardHeight = 128;
        int gap = 20;
        int columns = Math.min(3, Math.max(1, count));
        int row = index / columns;
        int col = index % columns;
        int gridWidth = columns * cardWidth + (columns - 1) * gap;
        int x = (WIDTH - gridWidth) / 2 + col * (cardWidth + gap);
        int y = 220 + row * (cardHeight + gap);
        if (columns > 1 && row == 0 && (col & 1) == 1) {
            y += 58;
        }
        return new Rectangle(x, y, cardWidth, cardHeight);
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
        g.drawString("Level: " + state.getLevelNumber(), 250, 62);
        g.drawString("Wave: " + state.getWave() + "/" + state.getMaxWave(), 350, 62);

        drawSelectionHint(g);
        drawButton(g, menuButton, "Menu", true);
        drawButton(g, speedButton, state.getSpeedLabel(), true);
        drawButton(g, nextButton, "Next", state.isWon() && state.hasNextLevel());
        drawButton(g, pauseButton, state.isPaused() ? "Resume" : "Pause", !state.isGameOver() && !state.isWon());
        drawButton(g, restartButton, "Restart", true);
    }

    private void drawSelectionHint(Graphics2D g) {
        g.setColor(new Color(255, 250, 235));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        String progress = state.getEnemiesSpawnedInWave() + "/" + state.getWaveEnemyCount();
        Tower selectedTower = state.getSelectedTower();
        if (state.hasSelectedBuildTile()) {
            g.drawString("Build at: " + (state.getSelectedBuildCol() + 1) + "," + (state.getSelectedBuildRow() + 1), 20, 96);
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
            g.drawString("Build: select a grass tile", 20, 96);
        }
        g.drawString("Enemies: " + progress, 470, 62);
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
                if (path) {
                    GameArt.drawPathTile(g, x, y, col, row);
                } else {
                    GameArt.drawGrassTile(g, x, y, col, row);
                }
            }
        }

        GameArt.drawPathRibbon(g, state.getPath());

        int[] goal = state.getPath().getGoalTile();
        int carrotX = goal[0] * TILE_SIZE + TILE_SIZE / 2;
        int carrotY = HUD_HEIGHT + goal[1] * TILE_SIZE + TILE_SIZE / 2;
        GameArt.drawCarrot(g, carrotX, carrotY, state.isCarrotSelected());
        drawCarrotHealthBar(g, carrotX, carrotY);
    }

    private void drawCarrotHealthBar(Graphics2D g, int carrotX, int carrotY) {
        int barWidth = 52;
        int barHeight = 7;
        int x = carrotX - barWidth / 2;
        int y = carrotY + 28;
        GameArt.drawHealthBar(g, x, y, barWidth, barHeight, state.getLifeRatio(), GameArt.healthColor(state.getLifeRatio()));
        g.setColor(new Color(255, 250, 235));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        drawCenteredAt(g, state.getLives() + "/" + state.getMaxLives(), carrotX, y - 3);
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

    private void drawCarrotInfoPanel(Graphics2D g) {
        if (!state.isCarrotSelected()) {
            return;
        }
        Rectangle rect = carrotInfoPanelRect();
        g.setColor(new Color(47, 68, 55, 232));
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);
        g.setColor(new Color(255, 250, 235, 190));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);

        int left = rect.x + 12;
        g.setColor(new Color(255, 250, 235));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g.drawString("Carrot HP", left, rect.y + 22);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.drawString(state.getLives() + " / " + state.getMaxLives(), left, rect.y + 45);

        int barWidth = rect.width - 24;
        int barY = rect.y + 58;
        g.setColor(new Color(73, 43, 35));
        g.fillRoundRect(left, barY, barWidth, 8, 6, 6);
        g.setColor(state.getLifeRatio() > 0.35 ? new Color(97, 201, 98) : new Color(232, 88, 74));
        g.fillRoundRect(left, barY, (int) (barWidth * state.getLifeRatio()), 8, 6, 6);
    }

    private void drawTowerInfoPanel(Graphics2D g) {
        Tower tower = state.getSelectedTower();
        if (tower == null) {
            return;
        }

        Rectangle rect = towerInfoPanelRect(tower);
        g.setColor(new Color(47, 68, 55, 232));
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);
        g.setColor(new Color(255, 250, 235, 190));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);

        int left = rect.x + 12;
        int right = rect.x + rect.width - 12;
        int y = rect.y + 21;
        g.setColor(new Color(255, 250, 235));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g.drawString(tower.getType().getDisplayName() + " Tower L" + tower.getLevel(), left, y);
        g.setColor(tower.getType().getBodyColor());
        g.fillOval(right - 16, rect.y + 10, 13, 13);

        y += 22;
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.setColor(new Color(226, 233, 215));
        g.drawString("DMG " + (int) tower.getDamage(), left, y);
        g.drawString("SPD " + String.format("%.2f", tower.getFireInterval()), left + 62, y);
        g.drawString("RNG " + (int) tower.getRange(), left + 128, y);

        y += 23;
        for (TowerUpgradeType upgradeType : TowerUpgradeType.values()) {
            drawUpgradeInfoRow(g, tower, upgradeType, left, right, y);
            y += 23;
        }

        g.setColor(new Color(226, 233, 215));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.drawString("Sell", left, rect.y + rect.height - 11);
        drawRightAligned(g, String.valueOf(tower.getSellValue()), right, rect.y + rect.height - 11);
    }

    private void drawUpgradeInfoRow(Graphics2D g, Tower tower, TowerUpgradeType upgradeType, int left, int right, int y) {
        boolean canUpgrade = tower.canUpgrade(upgradeType);
        boolean canAfford = canUpgrade && state.getCoins() >= tower.getUpgradeCost(upgradeType);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.setColor(canUpgrade ? new Color(255, 250, 235) : new Color(185, 195, 180));
        g.drawString(upgradeType.getDisplayName() + " L" + tower.getUpgradeLevel(upgradeType), left, y);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.setColor(canUpgrade ? new Color(226, 233, 215) : new Color(185, 195, 180));
        g.drawString(upgradePreviewText(tower, upgradeType), left + 54, y);

        g.setColor(canUpgrade ? (canAfford ? new Color(247, 216, 112) : new Color(255, 130, 120)) : new Color(185, 195, 180));
        drawRightAligned(g, canUpgrade ? String.valueOf(tower.getUpgradeCost(upgradeType)) : "MAX", right, y);
    }

    private String upgradePreviewText(Tower tower, TowerUpgradeType upgradeType) {
        if (!tower.canUpgrade(upgradeType)) {
            return "Maxed";
        }
        return switch (upgradeType) {
            case DAMAGE -> (int) tower.getDamage() + " -> " + (int) tower.getPreviewDamage(upgradeType);
            case SPEED -> String.format("%.2f -> %.2f", tower.getFireInterval(), tower.getPreviewFireInterval(upgradeType));
            case RANGE -> (int) tower.getRange() + " -> " + (int) tower.getPreviewRange(upgradeType);
        };
    }

    private Rectangle towerInfoPanelRect(Tower tower) {
        int width = 206;
        int height = 138;
        Rectangle contextArea = towerActionAreaRect(tower);
        Rectangle[] candidates = {
                new Rectangle(10, HUD_HEIGHT + 10, width, height),
                new Rectangle(WIDTH - width - 10, HUD_HEIGHT + 10, width, height),
                new Rectangle(10, HEIGHT - height - 10, width, height),
                new Rectangle(WIDTH - width - 10, HEIGHT - height - 10, width, height)
        };

        Rectangle best = candidates[0];
        double bestScore = -1;
        double towerX = tower.getCol() * TILE_SIZE + TILE_SIZE / 2.0;
        double towerY = HUD_HEIGHT + tower.getRow() * TILE_SIZE + TILE_SIZE / 2.0;
        for (Rectangle candidate : candidates) {
            if (candidate.intersects(contextArea)) {
                continue;
            }
            double centerX = candidate.getCenterX();
            double centerY = candidate.getCenterY();
            double score = Math.hypot(centerX - towerX, centerY - towerY);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private Rectangle carrotInfoPanelRect() {
        int[] goal = state.getPath().getGoalTile();
        int width = 150;
        int height = 82;
        Rectangle[] candidates = {
                new Rectangle(10, HUD_HEIGHT + 10, width, height),
                new Rectangle(WIDTH - width - 10, HUD_HEIGHT + 10, width, height),
                new Rectangle(10, HEIGHT - height - 10, width, height),
                new Rectangle(WIDTH - width - 10, HEIGHT - height - 10, width, height)
        };
        double carrotX = goal[0] * TILE_SIZE + TILE_SIZE / 2.0;
        double carrotY = HUD_HEIGHT + goal[1] * TILE_SIZE + TILE_SIZE / 2.0;
        Rectangle best = candidates[0];
        double bestScore = -1;
        for (Rectangle candidate : candidates) {
            double score = Math.hypot(candidate.getCenterX() - carrotX, candidate.getCenterY() - carrotY);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private Rectangle towerActionAreaRect(Tower tower) {
        int upgradeWidth = TowerUpgradeType.values().length * 70 + (TowerUpgradeType.values().length - 1) * 6;
        Point origin = contextMenuOrigin(tower.getCol(), tower.getRow(), upgradeWidth, 70);
        return new Rectangle(origin.x, origin.y, upgradeWidth, 70);
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
            GameArt.drawObstacle(g, obstacle);
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

            GameArt.drawTower(g, tower, centerX, centerY, tower == state.getSelectedTower());
        }

        for (Projectile projectile : state.getProjectiles()) {
            GameArt.drawProjectile(g, projectile);
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

        for (FloatingText text : state.getFloatingTexts()) {
            int alpha = Math.max(0, (int) (255 * (1.0 - text.getProgress())));
            Color color = text.getColor();
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            drawCenteredAt(g, text.getText(), (int) text.getX(), (int) text.getY());
        }

        for (Enemy enemy : state.getEnemies()) {
            GameArt.drawEnemy(g, enemy);
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
            drawVictorySummary(g);
        } else if (state.isGameOver()) {
            drawCentered(g, "Game Over", HUD_HEIGHT + 220);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            drawCentered(g, "Use Restart to replay this level", HUD_HEIGHT + 258);
        } else {
            drawCentered(g, "Paused", HUD_HEIGHT + 220);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            drawCentered(g, "Use the top buttons to resume or restart", HUD_HEIGHT + 258);
        }
    }

    private void drawVictorySummary(Graphics2D g) {
        int panelWidth = 316;
        int panelHeight = 184;
        int panelX = (WIDTH - panelWidth) / 2;
        int panelY = HUD_HEIGHT + 128;

        drawCentered(g, "Victory", HUD_HEIGHT + 105);
        g.setColor(new Color(47, 68, 55, 236));
        g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);
        g.setColor(new Color(255, 250, 235, 205));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.setColor(new Color(247, 216, 112));
        drawCentered(g, "Stars: " + state.getStarRating() + "/3", panelY + 35);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.setColor(new Color(255, 250, 235));
        int leftX = panelX + 34;
        int rightX = panelX + panelWidth - 34;
        int rowY = panelY + 70;
        drawResultRow(g, "Time", formatTime(state.getElapsedSeconds()), leftX, rightX, rowY);
        drawResultRow(g, "HP", state.getLives() + "/" + state.getMaxLives(), leftX, rightX, rowY + 24);
        drawResultRow(g, "Coins", String.valueOf(state.getCoins()), leftX, rightX, rowY + 48);
        drawResultRow(g, "Leaks", String.valueOf(state.getLeakedEnemies()), leftX, rightX, rowY + 72);
        drawResultRow(g, "Obstacles", String.valueOf(state.getClearedObstacles()), leftX, rightX, rowY + 96);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g.setColor(Color.WHITE);
        if (state.hasNextLevel()) {
            drawCentered(g, "Use Next to continue or Restart to replay", panelY + panelHeight + 38);
        } else {
            drawCentered(g, "Use Restart to replay this level", panelY + panelHeight + 38);
        }
    }

    private void drawResultRow(Graphics2D g, String label, String value, int leftX, int rightX, int y) {
        g.drawString(label, leftX, y);
        drawRightAligned(g, value, rightX, y);
    }

    private String formatTime(double seconds) {
        int totalSeconds = Math.max(0, (int) Math.round(seconds));
        int minutes = totalSeconds / 60;
        int remainder = totalSeconds % 60;
        return String.format("%d:%02d", minutes, remainder);
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

    private void drawRightAligned(Graphics2D g, String text, int rightX, int baselineY) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, rightX - metrics.stringWidth(text), baselineY);
    }

    private void drawRange(Graphics2D g, int centerX, int centerY, int range, Color fill, Color border) {
        g.setColor(fill);
        g.fillOval(centerX - range, centerY - range, range * 2, range * 2);
        g.setColor(border);
        g.setStroke(new BasicStroke(2f));
        g.drawOval(centerX - range, centerY - range, range * 2, range * 2);
    }
}
