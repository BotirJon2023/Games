// CaribbeanPiratesArchery.java
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class CaribbeanPiratesArchery extends JFrame {
    private GamePanel gamePanel;

    public CaribbeanPiratesArchery() {
        setTitle("Caribbean Pirates Archery - Treasure Island");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CaribbeanPiratesArchery());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private enum GameState { MENU, PLAYING, GAME_OVER, TREASURE }
    private GameState state = GameState.MENU;

    // Game objects
    private PirateTarget target;
    private PirateArrow arrow;
    private Pirate currentPirate;
    private Pirate pirate1, pirate2, computerPirate;
    private boolean isComputerMode;
    private int currentRound;
    private int maxRounds = 3;
    private List<TreasureChest> treasures = new ArrayList<>();
    private List<ShotRecord> shotRecords = new ArrayList<>();

    // Animation states
    private Timer gameTimer;
    private Timer arrowAnimationTimer;
    private Timer waveTimer;
    private double arrowPower;
    private double arrowAngle;
    private boolean isDrawingArrow;
    private boolean isAnimating;
    private Point2D arrowStartPos;
    private Point2D arrowCurrentPos;
    private int animationDuration = 35;
    private int animationFrame = 0;
    private double currentScore = 0;
    private boolean showParrotEffect = false;
    private String parrotMessage = "";

    // Pirate environment
    private List<Wave> waves = new ArrayList<>();
    private List<Cloud> clouds = new ArrayList<>();
    private List<Parrot> parrots = new ArrayList<>();
    private List<CannonBall> cannonBalls = new ArrayList<>();
    private List<PalmTree> palmTrees = new ArrayList<>();
    private List<Fish> fishes = new ArrayList<>();
    private List<Coin> coins = new ArrayList<>();

    // Weather effects
    private float tideLevel = 0;
    private int rumEffect = 0;
    private boolean showTreasureFound = false;
    private String treasureMessage = "";

    // UI Components
    private JButton onePlayerButton, twoPlayerButton, restartButton, menuButton;
    private JLabel statusLabel, scoreLabel1, scoreLabel2, roundLabel, bootyLabel;
    private JProgressBar powerBar;

    // Pirate colors
    private Color pirateRed = new Color(139, 0, 0);
    private Color pirateBlack = new Color(0, 0, 0);
    private Color pirateGold = new Color(255, 215, 0);
    private Color oceanBlue = new Color(0, 105, 148);
    private Color sandColor = new Color(238, 214, 175);
    private Color palmGreen = new Color(34, 139, 34);

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        setLayout(null);

        initializeGame();
        initializeUI();
        initializeCaribbean();

        gameTimer = new Timer(30, this);
        gameTimer.start();

        // Create waves
        for (int i = 0; i < 5; i++) {
            waves.add(new Wave());
        }

        // Create clouds
        for (int i = 0; i < 8; i++) {
            clouds.add(new Cloud());
        }

        // Create parrots
        for (int i = 0; i < 3; i++) {
            parrots.add(new Parrot());
        }

        // Create palm trees
        for (int i = 0; i < 12; i++) {
            palmTrees.add(new PalmTree(50 + i * 120, 600));
        }

        // Create fish
        for (int i = 0; i < 20; i++) {
            fishes.add(new Fish());
        }

        // Create treasure chests
        for (int i = 0; i < 5; i++) {
            treasures.add(new TreasureChest(100 + i * 200, 700));
        }

        // Wave timer
        waveTimer = new Timer(50, e -> {
            tideLevel += 0.02f;
            repaint();
        });
        waveTimer.start();
    }

    private void initializeCaribbean() {
        // Create cannon balls
        for (int i = 0; i < 10; i++) {
            cannonBalls.add(new CannonBall());
        }
    }

    private void initializeUI() {
        // Pirate styled buttons
        onePlayerButton = createPirateButton("VS BLACKBEARD (AI)", 500, 500);
        onePlayerButton.addActionListener(e -> startGame(true));

        twoPlayerButton = createPirateButton("2 PIRATES", 500, 580);
        twoPlayerButton.addActionListener(e -> startGame(false));

        restartButton = createPirateButton("RESTART VOYAGE", 50, 800);
        restartButton.addActionListener(e -> restartGame());
        restartButton.setVisible(false);

        menuButton = createPirateButton("RETURN TO PORT", 250, 800);
        menuButton.addActionListener(e -> showMenu());
        menuButton.setVisible(false);

        // Pirate styled labels
        statusLabel = new JLabel("");
        statusLabel.setBounds(50, 200, 500, 40);
        statusLabel.setFont(new Font("Segoe UI Symbol", Font.BOLD, 22));
        statusLabel.setForeground(pirateGold);

        scoreLabel1 = new JLabel("");
        scoreLabel1.setBounds(50, 250, 400, 30);
        scoreLabel1.setFont(new Font("Monospaced", Font.BOLD, 16));
        scoreLabel1.setForeground(new Color(255, 200, 100));

        scoreLabel2 = new JLabel("");
        scoreLabel2.setBounds(50, 290, 400, 30);
        scoreLabel2.setFont(new Font("Monospaced", Font.BOLD, 16));
        scoreLabel2.setForeground(new Color(200, 150, 50));

        roundLabel = new JLabel("");
        roundLabel.setBounds(50, 330, 400, 30);
        roundLabel.setFont(new Font("Segoe UI Symbol", Font.ITALIC, 14));
        roundLabel.setForeground(Color.WHITE);

        bootyLabel = new JLabel("");
        bootyLabel.setBounds(50, 370, 400, 30);
        bootyLabel.setFont(new Font("Segoe UI Symbol", Font.BOLD, 14));
        bootyLabel.setForeground(pirateGold);

        // Power bar with pirate theme
        powerBar = new JProgressBar(0, 200);
        powerBar.setBounds(50, 450, 300, 20);
        powerBar.setStringPainted(true);
        powerBar.setForeground(new Color(255, 140, 0));
        powerBar.setBackground(new Color(60, 30, 10));
        powerBar.setVisible(false);

        add(onePlayerButton);
        add(twoPlayerButton);
        add(restartButton);
        add(menuButton);
        add(statusLabel);
        add(scoreLabel1);
        add(scoreLabel2);
        add(roundLabel);
        add(bootyLabel);
        add(powerBar);
    }

    private JButton createPirateButton(String text, int x, int y) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Wooden plank background
                GradientPaint gp = new GradientPaint(0, 0, new Color(101, 67, 33), 0, getHeight(), new Color(60, 40, 20));
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Wood grain lines
                g2d.setColor(new Color(80, 50, 20));
                for (int i = 0; i < 5; i++) {
                    g2d.drawLine(5, 10 + i * 10, getWidth() - 5, 10 + i * 10);
                }

                // Skull and crossbones decoration
                g2d.setColor(Color.WHITE);
                g2d.fillOval(getWidth() - 40, 15, 20, 20);
                g2d.fillRect(getWidth() - 35, 35, 10, 15);
                g2d.fillRect(getWidth() - 45, 40, 30, 5);

                // Draw text
                g2d.setColor(pirateGold);
                g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                g2d.drawString(text, getWidth()/2 - textWidth/2, getHeight()/2 + 6);

                super.paintComponent(g);
            }
        };

        button.setBounds(x, y, 280, 70);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(80, 50, 20));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(null);
            }
        });

        return button;
    }

    private void initializeGame() {
        target = new PirateTarget(1100, 400);
        pirate1 = new Pirate("Jack Sparrow", "Black Pearl", pirateRed, "🏴‍☠️", "Captain");
        pirate2 = new Pirate("William Turner", "Flying Dutchman", new Color(0, 100, 150), "⚓", "Commodore");
        computerPirate = new Pirate("Blackbeard", "Queen Anne's Revenge", pirateBlack, "🔥", "Dread Pirate");
        arrow = new PirateArrow();
        arrowAngle = -Math.PI / 4;
        arrowPower = 0;
    }

    private void startGame(boolean vsComputer) {
        isComputerMode = vsComputer;
        currentPirate = pirate1;
        currentRound = 1;
        pirate1.resetScore();
        pirate2.resetScore();
        computerPirate.resetScore();
        shotRecords.clear();
        coins.clear();
        arrowPower = 0;
        arrowAngle = -Math.PI / 4;
        state = GameState.PLAYING;

        onePlayerButton.setVisible(false);
        twoPlayerButton.setVisible(false);
        restartButton.setVisible(true);
        menuButton.setVisible(true);
        powerBar.setVisible(true);

        updateUI();
        requestFocus();

        // Parrot cheers
        parrotMessage = "🎵 Yo ho ho! Let the games begin! 🎵";
        showParrotEffect = true;
        Timer parrotTimer = new Timer(2000, e -> showParrotEffect = false);
        parrotTimer.setRepeats(false);
        parrotTimer.start();
    }

    private void restartGame() {
        if (isComputerMode) {
            startGame(true);
        } else {
            startGame(false);
        }
    }

    private void showMenu() {
        state = GameState.MENU;
        onePlayerButton.setVisible(true);
        twoPlayerButton.setVisible(true);
        restartButton.setVisible(false);
        menuButton.setVisible(false);
        powerBar.setVisible(false);
        statusLabel.setText("");
        scoreLabel1.setText("");
        scoreLabel2.setText("");
        roundLabel.setText("");
        bootyLabel.setText("");
        repaint();
    }

    public void updateUI() {
        if (state == GameState.PLAYING) {
            statusLabel.setText("🏴‍☠️ " + currentPirate.title + " " + currentPirate.name + " - DRAW YER BOW! 🏴‍☠️");
            scoreLabel1.setText(pirate1.name + " (" + pirate1.ship + "): " + pirate1.score + " doubloons");

            if (isComputerMode) {
                scoreLabel2.setText(computerPirate.name + " (" + computerPirate.ship + "): " + computerPirate.score + " doubloons");
                roundLabel.setText("⚓ ROUND " + currentRound + " of " + maxRounds + " ⚓");
            } else {
                scoreLabel2.setText(pirate2.name + " (" + pirate2.ship + "): " + pirate2.score + " doubloons");
                roundLabel.setText("⚓ ROUND " + currentRound + " of " + maxRounds + " ⚓");
            }

            updateBootyCall();
        } else if (state == GameState.GAME_OVER) {
            determineTreasureWinner();
        }
    }

    private void updateBootyCall() {
        int totalScore = isComputerMode ? pirate1.score + computerPirate.score : pirate1.score + pirate2.score;
        if (totalScore > 0) {
            double percentage = (double)currentPirate.score / totalScore * 100;
            if (percentage > 60) {
                bootyLabel.setText("💎 " + currentPirate.name + " be claimin' the treasure! 💎");
                rumEffect = 3;
            } else if (percentage > 40) {
                bootyLabel.setText("🍻 The crew be cheerin'! 🍻");
                rumEffect = 2;
            } else {
                bootyLabel.setText("🐙 Davy Jones be callin'! 🐙");
                rumEffect = 1;
            }
        }
    }

    private void determineTreasureWinner() {
        String winner;
        String treasureBonus = "";

        if (isComputerMode) {
            if (pirate1.score > computerPirate.score) {
                winner = pirate1.name + " the " + pirate1.title;
                treasureBonus = "CLAIMS THE AZTEC GOLD!";
                treasureMessage = "🏆 THE BLACK PEARL SAILS WITH TREASURE! 🏆";
                addTreasureCoins(50);
            } else if (computerPirate.score > pirate1.score) {
                winner = computerPirate.name + " the " + computerPirate.title;
                treasureBonus = "STEALS THE ROYAL TREASURE!";
                treasureMessage = "👑 BLACKBEARD RULES THE CARIBBEAN! 👑";
                addTreasureCoins(30);
            } else {
                winner = "Both Pirates";
                treasureBonus = "SHARE THE BOOTY!";
                treasureMessage = "🤝 A PIRATE'S TRUCE! 🤝";
                addTreasureCoins(20);
            }
        } else {
            if (pirate1.score > pirate2.score) {
                winner = pirate1.name + " the " + pirate1.title;
                treasureBonus = "CAPTAIN OF THE CARIBBEAN!";
                treasureMessage = "🏴‍☠️ JACK SPARROW STRIKES AGAIN! 🏴‍☠️";
                addTreasureCoins(50);
            } else if (pirate2.score > pirate1.score) {
                winner = pirate2.name + " the " + pirate2.title;
                treasureBonus = "HONOR OF THE DUTCHMAN!";
                treasureMessage = "⚓ WILL TURNER PROVES HIS WORTH! ⚓";
                addTreasureCoins(40);
            } else {
                winner = "Both Pirates";
                treasureBonus = "TIE AT THE CROSSROADS!";
                treasureMessage = "🤝 TO DAVY JONES' LOCKER! 🤝";
                addTreasureCoins(25);
            }
        }

        statusLabel.setText("🏆 TREASURE CLAIMED - " + winner + "! " + treasureBonus + " 🏆");

        // Start treasure celebration
        showTreasureFound = true;
        for (int i = 0; i < 30; i++) {
            coins.add(new Coin());
        }

        Timer treasureTimer = new Timer(4000, e -> {
            showTreasureFound = false;
        });
        treasureTimer.setRepeats(false);
        treasureTimer.start();

        state = GameState.TREASURE;
        powerBar.setVisible(false);
    }

    private void addTreasureCoins(int count) {
        for (int i = 0; i < count; i++) {
            coins.add(new Coin());
        }
    }

    private void shootArrow() {
        if (isAnimating) return;

        currentScore = target.calculateScore(arrowAngle, arrowPower);
        currentPirate.addScore((int)currentScore);

        // Add to history
        shotRecords.add(new ShotRecord(currentPirate.name, currentRound, currentScore));

        // Parrot reaction
        if (currentScore >= 80) {
            parrotMessage = "🦜 SQUAWK! DEAD EYE! SQUAWK! 🦜";
            showParrotEffect = true;
            Timer parrotTimer = new Timer(1500, e -> showParrotEffect = false);
            parrotTimer.setRepeats(false);
            parrotTimer.start();
        }

        // Arrow animation
        isAnimating = true;
        animationFrame = 0;
        arrowStartPos = new Point2D.Double(200, 500);

        double endX = 1100 + (arrowPower * Math.cos(arrowAngle)) * 2.5;
        double endY = 400 + (arrowPower * Math.sin(arrowAngle)) * 2.5;
        arrowCurrentPos = arrowStartPos;

        arrowAnimationTimer = new Timer(16, e -> {
            animationFrame++;
            double t = Math.min(1.0, (double) animationFrame / animationDuration);
            double easeOut = 1 - Math.pow(1 - t, 3);
            arrowCurrentPos = new Point2D.Double(
                    arrowStartPos.getX() + (endX - arrowStartPos.getX()) * easeOut,
                    arrowStartPos.getY() + (endY - arrowStartPos.getY()) * easeOut
            );

            if (animationFrame >= animationDuration) {
                arrowAnimationTimer.stop();
                isAnimating = false;

                showScoreAnimation((int)currentScore);
                switchPlayer();

                if (isComputerMode && currentPirate == computerPirate && state == GameState.PLAYING) {
                    computerTurn();
                }
            }
            repaint();
        });
        arrowAnimationTimer.start();

        arrowPower = 0;
        powerBar.setValue(0);
        repaint();
    }

    private void showScoreAnimation(int score) {
        JWindow popup = new JWindow();
        popup.setSize(350, 120);
        popup.setLocationRelativeTo(this);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Treasure map background
                g2d.setColor(new Color(245, 222, 179));
                g2d.fillRoundRect(0, 0, 350, 120, 20, 20);
                g2d.setColor(pirateGold);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRoundRect(5, 5, 340, 110, 15, 15);

                // Score text
                g2d.setColor(pirateRed);
                g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 32));
                String scoreText = "💰 +" + score + " DOUBLOONS! 💰";
                FontMetrics fm = g2d.getFontMetrics();
                int width = fm.stringWidth(scoreText);
                g2d.drawString(scoreText, 175 - width/2, 65);

                // Pirate phrase
                g2d.setFont(new Font("Segoe UI Symbol", Font.ITALIC, 14));
                String phrase = score >= 80 ? "SHIVER ME TIMBERS!" : "A FINE SHOT!";
                width = fm.stringWidth(phrase);
                g2d.drawString(phrase, 175 - width/2, 95);
            }
        };

        popup.add(panel);
        popup.setOpacity(0.95f);
        popup.setVisible(true);

        Timer timer = new Timer(1500, e -> popup.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    private void switchPlayer() {
        if (isComputerMode) {
            if (currentPirate == pirate1) {
                currentPirate = computerPirate;
            } else {
                currentPirate = pirate1;
                currentRound++;
                if (currentRound > maxRounds) {
                    endGame();
                }
            }
        } else {
            if (currentPirate == pirate1) {
                currentPirate = pirate2;
            } else {
                currentPirate = pirate1;
                currentRound++;
                if (currentRound > maxRounds) {
                    endGame();
                }
            }
        }
        updateUI();
    }

    private void computerTurn() {
        Timer timer = new Timer(1000, e -> {
            // Blackbeard's aggressive AI
            if (computerPirate.title.equals("Dread Pirate")) {
                arrowPower = 130 + Math.random() * 70;
                arrowAngle = -Math.PI / 2.7 + (Math.random() * Math.PI / 5);
            } else {
                arrowPower = 90 + Math.random() * 90;
                arrowAngle = -Math.PI / 3 + (Math.random() * Math.PI / 6);
            }
            shootArrow();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void endGame() {
        state = GameState.GAME_OVER;
        updateUI();
        powerBar.setVisible(false);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw Caribbean sky
        drawCaribbeanSky(g2d);

        // Draw ocean
        drawOcean(g2d);

        // Draw ships in background
        drawPirateShips(g2d);

        // Draw island beach
        drawIslandBeach(g2d);

        // Draw palm trees
        for (PalmTree tree : palmTrees) {
            tree.draw(g2d);
        }

        // Draw clouds
        for (Cloud cloud : clouds) {
            cloud.draw(g2d);
            cloud.update();
        }

        // Draw parrots
        for (Parrot parrot : parrots) {
            parrot.draw(g2d);
            parrot.update();
        }

        // Draw fish in water
        for (Fish fish : fishes) {
            fish.draw(g2d);
            fish.update();
        }

        // Draw treasure chests
        for (TreasureChest chest : treasures) {
            chest.draw(g2d);
        }

        // Draw cannon balls
        for (CannonBall ball : cannonBalls) {
            ball.draw(g2d);
            ball.update();
        }

        // Draw target (pirate wheel)
        target.draw(g2d);

        // Draw shot history on parchment
        drawShotHistory(g2d);

        // Draw pirate
        if (state == GameState.PLAYING && !isAnimating && !(isComputerMode && currentPirate == computerPirate)) {
            currentPirate.draw(g2d, 200, 500, arrowAngle, arrowPower);
            drawPirateBowAndArrow(g2d);
        } else if (state == GameState.PLAYING && currentPirate != null) {
            currentPirate.drawIdle(g2d, 200, 500);
        }

        // Draw flying arrow
        if (isAnimating && arrowCurrentPos != null) {
            drawFlyingArrow(g2d);
        }

        // Draw power meter (rum bottle)
        if (state == GameState.PLAYING && !isAnimating && !(isComputerMode && currentPirate == computerPirate)) {
            powerBar.setValue((int)arrowPower);
            drawRumMeter(g2d);
        }

        // Draw treasure celebration
        if (showTreasureFound) {
            drawTreasureCelebration(g2d);
        }

        // Draw coins
        for (Coin coin : coins) {
            coin.draw(g2d);
            coin.update();
        }

        // Draw parrot message
        if (showParrotEffect && !parrotMessage.isEmpty()) {
            drawParrotMessage(g2d);
        }

        // Draw menu
        if (state == GameState.MENU) {
            drawPirateMenu(g2d);
        }
    }

    private void drawCaribbeanSky(Graphics2D g2d) {
        // Tropical sky gradient
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, 400, new Color(255, 200, 100));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, getWidth(), 500);

        // Sun
        g2d.setColor(new Color(255, 220, 100));
        g2d.fillOval(1000, 50, 80, 80);
        g2d.setColor(new Color(255, 255, 200, 150));
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI * 2 / 12;
            int x = 1040 + (int)(60 * Math.cos(angle));
            int y = 90 + (int)(60 * Math.sin(angle));
            g2d.drawLine(1040, 90, x, y);
        }
    }

    private void drawOcean(Graphics2D g2d) {
        // Ocean with waves
        g2d.setColor(oceanBlue);
        g2d.fillRect(0, 500, getWidth(), 400);

        // Waves
        for (Wave wave : waves) {
            wave.draw(g2d);
            wave.update();
        }

        // Wave highlights
        g2d.setColor(new Color(100, 200, 255, 100));
        for (int i = 0; i < 50; i++) {
            int x = (int)(Math.sin(tideLevel + i) * 20 + i * 30);
            g2d.fillOval(x, 520 + (int)(Math.sin(tideLevel * 2 + i) * 10), 30, 5);
        }
    }

    private void drawIslandBeach(Graphics2D g2d) {
        // Sandy beach
        g2d.setColor(sandColor);
        g2d.fillRect(0, 600, getWidth(), 200);

        // Sand texture
        g2d.setColor(new Color(210, 180, 140));
        for (int i = 0; i < 200; i++) {
            int x = (int)(Math.random() * getWidth());
            int y = 600 + (int)(Math.random() * 100);
            g2d.fillOval(x, y, 3, 3);
        }

        // Seashells
        g2d.setColor(new Color(255, 228, 196));
        for (int i = 0; i < 30; i++) {
            int x = (int)(Math.random() * getWidth());
            int y = 650 + (int)(Math.random() * 50);
            g2d.fillOval(x, y, 5, 3);
        }
    }

    private void drawPirateShips(Graphics2D g2d) {
        // Black Pearl in background
        g2d.setColor(new Color(50, 40, 30));
        g2d.fillRect(100, 480, 120, 30);
        g2d.fillPolygon(new int[]{100, 160, 220}, new int[]{480, 450, 480}, 3);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(150, 440, 10, 40);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(140, 435, 30, 5);

        // Flying Dutchman
        g2d.setColor(new Color(0, 80, 100));
        g2d.fillRect(900, 470, 130, 35);
        g2d.fillPolygon(new int[]{900, 965, 1030}, new int[]{470, 440, 470}, 3);
        g2d.setColor(new Color(0, 50, 70));
        g2d.fillRect(955, 430, 10, 40);
    }

    private void drawPirateBowAndArrow(Graphics2D g2d) {
        int bowX = 180;
        int bowY = 500;

        double arrowEndX = bowX + arrowPower * Math.cos(arrowAngle);
        double arrowEndY = bowY + arrowPower * Math.sin(arrowAngle);

        // Pirate bow (curved like a cutlass)
        g2d.setColor(new Color(80, 40, 20));
        g2d.setStroke(new BasicStroke(6));
        g2d.drawArc(bowX - 30, bowY - 45, 60, 90, 0, 180);

        // Gold decorations
        g2d.setColor(pirateGold);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawArc(bowX - 28, bowY - 43, 56, 86, 0, 180);

        // Bow string (rope)
        g2d.setColor(new Color(139, 69, 19));
        g2d.drawLine(bowX - 25, bowY - 40, (int)arrowEndX, (int)arrowEndY);
        g2d.drawLine(bowX + 25, bowY - 40, (int)arrowEndX, (int)arrowEndY);

        // Arrow
        g2d.setColor(new Color(80, 60, 40));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(bowX, bowY, (int)arrowEndX, (int)arrowEndY);

        // Arrowhead (pirate hook shape)
        g2d.setColor(new Color(192, 192, 192));
        double angle = Math.atan2(arrowEndY - bowY, arrowEndX - bowX);
        int arrowheadX = (int)(arrowEndX + 18 * Math.cos(angle));
        int arrowheadY = (int)(arrowEndY + 18 * Math.sin(angle));
        g2d.fillPolygon(
                new int[]{(int)arrowEndX, arrowheadX, (int)arrowEndX},
                new int[]{(int)arrowEndY, arrowheadY, (int)(arrowEndY + 6)},
                3
        );

        // Skull and crossbones fletching
        g2d.setColor(Color.WHITE);
        int featherX = (int)(bowX + arrowPower * 0.6 * Math.cos(angle));
        int featherY = (int)(bowY + arrowPower * 0.6 * Math.sin(angle));
        g2d.fillOval(featherX - 5, featherY - 3, 6, 6);
        g2d.fillRect(featherX - 2, featherY + 2, 4, 8);
    }

    private void drawFlyingArrow(Graphics2D g2d) {
        if (arrowCurrentPos == null) return;

        g2d.setColor(new Color(80, 60, 40));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine((int)arrowCurrentPos.getX(), (int)arrowCurrentPos.getY(),
                (int)(arrowCurrentPos.getX() + 25), (int)(arrowCurrentPos.getY() + 6));

        // Water splash effect
        if (arrowCurrentPos.getY() > 550) {
            g2d.setColor(new Color(100, 200, 255, 150));
            g2d.fillOval((int)arrowCurrentPos.getX() - 10, (int)arrowCurrentPos.getY() - 5, 20, 10);
        }
    }

    private void drawShotHistory(Graphics2D g2d) {
        int y = 550;
        g2d.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2d.setColor(new Color(245, 222, 179));
        g2d.fillRoundRect(1140, 540, 220, 150, 10, 10);
        g2d.setColor(pirateBlack);
        g2d.drawRoundRect(1140, 540, 220, 150, 10, 10);

        g2d.setColor(pirateRed);
        g2d.drawString("🏴‍☠️ SHOT LOG 🏴‍☠️", 1160, 560);

        for (int i = 0; i < Math.min(5, shotRecords.size()); i++) {
            ShotRecord shot = shotRecords.get(shotRecords.size() - 1 - i);
            g2d.setColor(pirateBlack);
            g2d.drawString(shot.pirate + ": " + (int)shot.score + "💰", 1155, 585 + i * 20);
        }
    }

    private void drawTreasureCelebration(Graphics2D g2d) {
        // Treasure chest burst
        g2d.setColor(new Color(255, 215, 0, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(pirateGold);
        g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 48));
        String treasure = treasureMessage;
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(treasure);
        g2d.drawString(treasure, 700 - width/2, 450);

        // Treasure glow
        g2d.setColor(new Color(255, 215, 0, 100));
        for (int i = 0; i < 3; i++) {
            g2d.drawOval(700 - 100 - i*20, 400 - i*10, 200 + i*40, 100 + i*20);
        }
    }

    private void drawParrotMessage(Graphics2D g2d) {
        // Speech bubble
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(800, 100, 350, 60, 20, 20);
        g2d.setColor(pirateBlack);
        g2d.drawRoundRect(800, 100, 350, 60, 20, 20);

        g2d.setColor(pirateRed);
        g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(parrotMessage);
        g2d.drawString(parrotMessage, 975 - width/2, 135);

        // Tail
        int[] xPoints = {800, 780, 800};
        int[] yPoints = {140, 160, 160};
        g2d.fillPolygon(xPoints, yPoints, 3);
    }

    private void drawPirateMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Treasure map background
        g2d.setColor(new Color(245, 222, 179));
        g2d.fillRoundRect(300, 100, 800, 600, 50, 50);
        g2d.setColor(pirateBlack);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(310, 110, 780, 580, 40, 40);

        // Title
        g2d.setColor(pirateRed);
        g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 56));
        String title = "🏴‍☠️ PIRATE ARCHERY 🏴‍☠️";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, 700 - titleWidth/2, 250);

        g2d.setFont(new Font("Segoe UI Symbol", Font.ITALIC, 28));
        String subtitle = "Caribbean Tournament";
        int subWidth = fm.stringWidth(subtitle);
        g2d.drawString(subtitle, 700 - subWidth/2, 320);

        // Skull and crossbones emblem
        g2d.setColor(Color.BLACK);
        g2d.fillOval(650, 350, 100, 80);
        g2d.fillRect(680, 430, 40, 60);
        g2d.fillRect(660, 450, 80, 20);
        g2d.setColor(Color.WHITE);
        g2d.fillOval(675, 375, 15, 15);
        g2d.fillOval(710, 375, 15, 15);
        g2d.fillRect(693, 395, 14, 20);
    }

    private void drawRumMeter(Graphics2D g2d) {
        // Already implemented above
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.PLAYING) {
            repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (state != GameState.PLAYING) return;
        if (isAnimating) return;
        if (isComputerMode && currentPirate == computerPirate) return;

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            isDrawingArrow = true;
        } else if (e.getKeyCode() == KeyEvent.VK_UP && isDrawingArrow && arrowPower < 200) {
            arrowPower = Math.min(arrowPower + 10, 200);
            powerBar.setValue((int)arrowPower);
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN && isDrawingArrow && arrowPower > 0) {
            arrowPower = Math.max(arrowPower - 8, 0);
            powerBar.setValue((int)arrowPower);
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT && isDrawingArrow) {
            arrowAngle = Math.max(arrowAngle - 0.07, -Math.PI / 1.5);
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && isDrawingArrow) {
            arrowAngle = Math.min(arrowAngle + 0.07, -Math.PI / 8);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && isDrawingArrow && state == GameState.PLAYING) {
            isDrawingArrow = false;
            if (!isAnimating) {
                shootArrow();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes for Caribbean theme
    class PirateTarget {
        int x, y;

        PirateTarget(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g2d) {
            // Pirate ship wheel target
            int[] radii = {90, 75, 60, 45, 30};
            Color[] colors = {new Color(80, 40, 20), new Color(60, 30, 10), pirateRed, pirateBlack, pirateGold};

            for (int i = 0; i < radii.length; i++) {
                g2d.setColor(colors[i]);
                g2d.fillOval(x - radii[i], y - radii[i], radii[i] * 2, radii[i] * 2);
            }

            // Wheel spokes
            g2d.setColor(new Color(80, 60, 40));
            g2d.setStroke(new BasicStroke(4));
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI * 2 / 8;
                int x2 = (int)(x + 60 * Math.cos(angle));
                int y2 = (int)(y + 60 * Math.sin(angle));
                g2d.drawLine(x, y, x2, y2);
            }

            // Skull in center
            g2d.setColor(Color.WHITE);
            g2d.fillOval(x - 15, y - 12, 30, 25);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x - 8, y - 8, 5, 5);
            g2d.fillOval(x + 3, y - 8, 5, 5);
            g2d.fillRect(x - 5, y, 10, 8);

            // Target stand (cannon)
            g2d.setColor(new Color(50, 50, 50));
            g2d.fillRect(x - 15, y + 30, 30, 40);
            g2d.fillOval(x - 20, y + 60, 40, 20);
        }

        double calculateScore(double angle, double power) {
            double endX = 1100 + power * Math.cos(angle) * 2.5;
            double endY = 400 + power * Math.sin(angle) * 2.5;
            double distance = Math.sqrt(Math.pow(endX - x, 2) + Math.pow(endY - y, 2));

            if (distance < 30) return 100;
            if (distance < 45) return 80;
            if (distance < 60) return 60;
            if (distance < 75) return 40;
            if (distance < 90) return 20;
            return 0;
        }
    }

    class Pirate {
        String name;
        String ship;
        Color color;
        String emblem;
        String title;
        int score;
        float pegLegWobble = 0;

        Pirate(String name, String ship, Color color, String emblem, String title) {
            this.name = name;
            this.ship = ship;
            this.color = color;
            this.emblem = emblem;
            this.title = title;
            this.score = 0;
        }

        void draw(Graphics2D g2d, int x, int y, double angle, double power) {
            pegLegWobble += 0.1f;

            // Pirate hat
            g2d.setColor(pirateBlack);
            g2d.fillPolygon(new int[]{x - 20, x, x + 20}, new int[]{y - 55, y - 70, y - 55}, 3);
            g2d.fillRect(x - 18, y - 55, 36, 10);

            // Skull on hat
            g2d.setColor(Color.WHITE);
            g2d.fillOval(x - 6, y - 65, 12, 10);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x - 4, y - 63, 3, 3);
            g2d.fillOval(x + 1, y - 63, 3, 3);

            // Body
            g2d.setColor(color);
            g2d.fillRect(x - 12, y - 45, 24, 45);

            // Coat
            g2d.setColor(new Color(40, 20, 10));
            g2d.fillRect(x - 15, y - 40, 30, 35);

            // Emblem
            g2d.setColor(pirateGold);
            g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));
            g2d.drawString(emblem, x - 5, y - 20);

            // Peg leg
            g2d.setColor(new Color(80, 40, 20));
            g2d.fillRect(x - 5, y, 10, 30);
            g2d.fillRect(x - 8, y + 25, 16, 5);

            // Hook hand
            g2d.setColor(new Color(192, 192, 192));
            g2d.fillArc(x + 12, y - 25, 15, 15, 0, 180);

            // Title
            g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD | Font.ITALIC, 11));
            g2d.drawString(title + " " + name, x - 25, y + 35);
            g2d.drawString(ship, x - 20, y + 48);
        }

        void drawIdle(Graphics2D g2d, int x, int y) {
            pegLegWobble += 0.05f;
            int wobble = (int)(Math.sin(pegLegWobble) * 3);

            // Idle animation with rum bottle
            g2d.setColor(pirateBlack);
            g2d.fillPolygon(new int[]{x - 20, x, x + 20}, new int[]{y - 55 + wobble, y - 70 + wobble, y - 55 + wobble}, 3);
            g2d.fillRect(x - 18, y - 55 + wobble, 36, 10);

            g2d.setColor(color);
            g2d.fillRect(x - 12, y - 45 + wobble, 24, 45);

            g2d.setColor(new Color(40, 20, 10));
            g2d.fillRect(x - 15, y - 40 + wobble, 30, 35);

            g2d.setColor(pirateGold);
            g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));
            g2d.drawString(emblem, x - 5, y - 20 + wobble);

            // Drinking rum
            g2d.setColor(new Color(60, 30, 10));
            g2d.fillRect(x + 15, y - 30 + wobble, 8, 20);
            g2d.setColor(new Color(255, 140, 0));
            g2d.fillRect(x + 16, y - 28 + wobble, 6, 15);

            g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD | Font.ITALIC, 11));
            g2d.drawString(title + " " + name + " - DRINKIN' RUM", x - 35, y + 35 + wobble);
        }

        void addScore(int points) {
            score += points;
        }

        void resetScore() {
            score = 0;
        }
    }

    class PirateArrow {
        // Placeholder
    }

    class ShotRecord {
        String pirate;
        int round;
        double score;

        ShotRecord(String pirate, int round, double score) {
            this.pirate = pirate;
            this.round = round;
            this.score = score;
        }
    }

    class Wave {
        int offset;
        float height;

        Wave() {
            offset = (int)(Math.random() * 200);
            height = (float)(Math.random() * 20);
        }

        void update() {
            offset += 2;
            if (offset > 200) offset = 0;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(0, 80, 120, 150));
            for (int i = 0; i < 20; i++) {
                int x = offset + i * 70;
                int y = 520 + (int)(Math.sin((offset + i * 20) * 0.05) * height);
                g2d.fillOval(x, y, 60, 15);
            }
        }
    }

    class Cloud {
        int x, y;
        float speed;

        Cloud() {
            x = (int)(Math.random() * 1500);
            y = (int)(Math.random() * 200);
            speed = 0.5f + (float)Math.random();
        }

        void update() {
            x -= speed;
            if (x < -100) {
                x = 1500;
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.fillOval(x, y, 80, 50);
            g2d.fillOval(x + 30, y - 20, 60, 50);
            g2d.fillOval(x - 20, y - 10, 60, 50);
        }
    }

    class Parrot {
        int x, y;
        float wingFlap;

        Parrot() {
            x = 800 + (int)(Math.random() * 400);
            y = 100 + (int)(Math.random() * 200);
            wingFlap = 0;
        }

        void update() {
            wingFlap += 0.2f;
            x -= 1;
            if (x < 0) {
                x = 1400;
            }
        }

        void draw(Graphics2D g2d) {
            // Body
            g2d.setColor(new Color(34, 139, 34));
            g2d.fillOval(x, y, 20, 15);

            // Head
            g2d.setColor(new Color(50, 205, 50));
            g2d.fillOval(x + 15, y - 5, 12, 12);

            // Beak
            g2d.setColor(Color.ORANGE);
            g2d.fillPolygon(new int[]{x + 25, x + 32, x + 25}, new int[]{y, y + 3, y + 6}, 3);

            // Wing
            int wingY = (int)(Math.sin(wingFlap) * 5);
            g2d.setColor(new Color(0, 100, 0));
            g2d.fillOval(x + 5, y + 5 + wingY, 15, 10);

            // Eye
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x + 20, y - 3, 3, 3);
        }
    }

    class PalmTree {
        int x, y;

        PalmTree(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g2d) {
            // Trunk
            g2d.setColor(new Color(101, 67, 33));
            g2d.fillRect(x, y - 100, 15, 100);

            // Trunk lines
            g2d.setColor(new Color(80, 50, 20));
            for (int i = 0; i < 5; i++) {
                g2d.drawLine(x, y - 20 - i * 20, x + 15, y - 30 - i * 20);
            }

            // Leaves
            g2d.setColor(palmGreen);
            for (int i = 0; i < 6; i++) {
                double angle = i * Math.PI * 2 / 6;
                int x2 = (int)(x + 7 + 40 * Math.cos(angle));
                int y2 = (int)(y - 100 + 20 * Math.sin(angle));
                g2d.drawLine(x + 7, y - 100, x2, y2);
                g2d.fillOval(x2 - 5, y2 - 5, 10, 10);
            }

            // Coconuts
            g2d.setColor(new Color(101, 67, 33));
            g2d.fillOval(x, y - 95, 8, 10);
            g2d.fillOval(x + 7, y - 98, 8, 10);
        }
    }

    class Fish {
        int x, y;
        float swim;

        Fish() {
            x = (int)(Math.random() * 1400);
            y = 550 + (int)(Math.random() * 100);
            swim = (float)Math.random();
        }

        void update() {
            x += 2;
            if (x > 1400) {
                x = -50;
            }
            swim += 0.1f;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(255, 100, 50));
            g2d.fillOval(x, y + (int)(Math.sin(swim) * 5), 20, 10);
            g2d.fillPolygon(new int[]{x + 18, x + 28, x + 18}, new int[]{y + (int)(Math.sin(swim) * 5), y + 5 + (int)(Math.sin(swim) * 5), y + 10 + (int)(Math.sin(swim) * 5)}, 3);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x + 14, y + 2 + (int)(Math.sin(swim) * 5), 2, 2);
        }
    }

    class TreasureChest {
        int x, y;
        boolean isOpen;

        TreasureChest(int x, int y) {
            this.x = x;
            this.y = y;
            isOpen = false;
        }

        void draw(Graphics2D g2d) {
            // Chest body
            g2d.setColor(new Color(101, 67, 33));
            g2d.fillRect(x, y, 40, 30);

            // Gold bands
            g2d.setColor(pirateGold);
            g2d.fillRect(x + 5, y, 5, 30);
            g2d.fillRect(x + 30, y, 5, 30);
            g2d.fillRect(x, y + 10, 40, 5);

            // Lock
            g2d.setColor(pirateGold);
            g2d.fillOval(x + 17, y + 12, 6, 6);

            // Lid
            if (isOpen) {
                g2d.fillPolygon(new int[]{x, x + 20, x + 40}, new int[]{y - 15, y - 25, y - 15}, 3);
                // Treasure glow
                g2d.setColor(new Color(255, 215, 0, 100));
                g2d.fillOval(x + 10, y + 5, 20, 15);
            } else {
                g2d.fillRect(x, y - 10, 40, 10);
            }
        }
    }

    class CannonBall {
        int x, y;
        int life;

        CannonBall() {
            reset();
        }

        void reset() {
            x = (int)(Math.random() * 1400);
            y = 500 + (int)(Math.random() * 100);
            life = 100;
        }

        void update() {
            x += (Math.random() - 0.5) * 10;
            y += (Math.random() - 0.5) * 5;
            life--;
            if (life <= 0) {
                reset();
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x, y, 8, 8);
            // Smoke trail
            g2d.setColor(new Color(100, 100, 100, 50));
            g2d.fillOval(x - 5, y - 3, 15, 15);
        }
    }

    class Coin {
        int x, y;
        int life;
        float spin;

        Coin() {
            x = (int)(Math.random() * 1400);
            y = (int)(Math.random() * 900);
            life = 100;
            spin = 0;
        }

        void update() {
            y += 2;
            spin += 0.2f;
            life--;
        }

        void draw(Graphics2D g2d) {
            if (life <= 0) return;

            g2d.setColor(pirateGold);
            g2d.rotate(spin, x, y);
            g2d.fillOval(x, y, 10, 10);
            g2d.setColor(new Color(255, 200, 0));
            g2d.fillOval(x + 2, y + 2, 6, 6);
            g2d.rotate(-spin, x, y);
        }
    }
}