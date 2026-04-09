// GladiatorArcheryColosseum.java
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.awt.image.BufferedImage;

public class GladiatorArcheryColosseum extends JFrame {
    private GamePanel gamePanel;

    public GladiatorArcheryColosseum() {
        setTitle("Gladiator Archery Tournament - Colosseum Arena");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GladiatorArcheryColosseum());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private enum GameState { MENU, PLAYING, GAME_OVER, VICTORY }
    private GameState state = GameState.MENU;

    // Game objects
    private ColosseumTarget target;
    private GladiatorArrow arrow;
    private Gladiator currentGladiator;
    private Gladiator gladiator1, gladiator2, computerGladiator;
    private boolean isComputerMode;
    private int currentRound;
    private int maxRounds = 3;
    private List<ShotHistory> shotHistory = new ArrayList<>();

    // Animation states
    private Timer gameTimer;
    private Timer arrowAnimationTimer;
    private Timer crowdTimer;
    private double arrowPower;
    private double arrowAngle;
    private boolean isDrawingArrow;
    private boolean isAnimating;
    private Point2D arrowStartPos;
    private Point2D arrowCurrentPos;
    private int animationDuration = 35;
    private int animationFrame = 0;
    private double currentScore = 0;
    private boolean showBloodEffect = false;
    private Point bloodPosition;

    // Crowd animations
    private List<RomanCitizen> crowd = new ArrayList<>();
    private List<GladiatorAnimal> animals = new ArrayList<>();
    private List<FireParticle> fireParticles = new ArrayList<>();
    private List<DustCloud> dustClouds = new ArrayList<>();
    private List<LaurelWreath> laurels = new ArrayList<>();

    // Environmental effects
    private float dustLevel = 0;
    private int crowdChant = 0;
    private boolean showVictoryParticles = false;
    private String victoryMessage = "";

    // UI Components
    private JButton onePlayerButton, twoPlayerButton, restartButton, menuButton;
    private JLabel statusLabel, scoreLabel1, scoreLabel2, roundLabel, crowdLabel;
    private JProgressBar powerBar;
    private JPanel romanBorderPanel;

    // Roman colors
    private Color romanGold = new Color(212, 175, 55);
    private Color romanRed = new Color(139, 0, 0);
    private Color romanPurple = new Color(128, 0, 128);
    private Color marbleWhite = new Color(245, 245, 220);
    private Color stoneGray = new Color(169, 169, 169);

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        setLayout(null);

        initializeGame();
        initializeUI();
        initializeRomanEffects();

        gameTimer = new Timer(30, this);
        gameTimer.start();

        // Create crowd
        for (int i = 0; i < 150; i++) {
            crowd.add(new RomanCitizen());
        }

        // Create colosseum animals
        animals.add(new GladiatorAnimal("lion", 200, 700));
        animals.add(new GladiatorAnimal("tiger", 250, 720));
        animals.add(new GladiatorAnimal("eagle", 300, 680));

        // Create dust particles
        for (int i = 0; i < 30; i++) {
            dustClouds.add(new DustCloud());
        }

        // Crowd chant timer
        crowdTimer = new Timer(2000, e -> {
            if (state == GameState.PLAYING) {
                crowdChant = (int)(Math.random() * 5);
            }
        });
        crowdTimer.start();
    }

    private void initializeRomanEffects() {
        // Create fire particles for torches
        for (int i = 0; i < 50; i++) {
            fireParticles.add(new FireParticle());
        }
    }

    private void initializeUI() {
        // Roman styled buttons
        onePlayerButton = createRomanButton("VS LEGIONARY (AI)", 500, 500);
        onePlayerButton.addActionListener(e -> startGame(true));

        twoPlayerButton = createRomanButton("2 GLADIATORS", 500, 580);
        twoPlayerButton.addActionListener(e -> startGame(false));

        restartButton = createRomanButton("RESTART BATTLE", 50, 800);
        restartButton.addActionListener(e -> restartGame());
        restartButton.setVisible(false);

        menuButton = createRomanButton("RETURN TO ROME", 250, 800);
        menuButton.addActionListener(e -> showMenu());
        menuButton.setVisible(false);

        // Roman styled labels
        statusLabel = new JLabel("");
        statusLabel.setBounds(50, 200, 500, 40);
        statusLabel.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 22));
        statusLabel.setForeground(romanGold);

        scoreLabel1 = new JLabel("");
        scoreLabel1.setBounds(50, 250, 400, 30);
        scoreLabel1.setFont(new Font("Monospaced", Font.BOLD, 16));
        scoreLabel1.setForeground(romanRed);

        scoreLabel2 = new JLabel("");
        scoreLabel2.setBounds(50, 290, 400, 30);
        scoreLabel2.setFont(new Font("Monospaced", Font.BOLD, 16));
        scoreLabel2.setForeground(romanGold);

        roundLabel = new JLabel("");
        roundLabel.setBounds(50, 330, 400, 30);
        roundLabel.setFont(new Font("Serif", Font.ITALIC, 14));
        roundLabel.setForeground(marbleWhite);

        crowdLabel = new JLabel("");
        crowdLabel.setBounds(50, 370, 400, 30);
        crowdLabel.setFont(new Font("Serif", Font.ITALIC, 14));
        crowdLabel.setForeground(new Color(200, 150, 100));

        // Power bar with Roman styling
        powerBar = new JProgressBar(0, 200);
        powerBar.setBounds(50, 450, 300, 20);
        powerBar.setStringPainted(true);
        powerBar.setForeground(romanRed);
        powerBar.setBackground(stoneGray);
        powerBar.setVisible(false);

        add(onePlayerButton);
        add(twoPlayerButton);
        add(restartButton);
        add(menuButton);
        add(statusLabel);
        add(scoreLabel1);
        add(scoreLabel2);
        add(roundLabel);
        add(crowdLabel);
        add(powerBar);
    }

    private JButton createRomanButton(String text, int x, int y) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient background
                GradientPaint gp = new GradientPaint(0, 0, romanRed, 0, getHeight(), new Color(100, 0, 0));
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Roman border pattern
                g2d.setColor(romanGold);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 12, 12);

                // Draw text
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Serif", Font.BOLD, 18));
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                g2d.drawString(text, getWidth()/2 - textWidth/2, getHeight()/2 + 6);

                super.paintComponent(g);
            }
        };

        button.setBounds(x, y, 280, 60);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(180, 0, 0));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(null);
            }
        });

        return button;
    }

    private void initializeGame() {
        target = new ColosseumTarget(1100, 400);
        gladiator1 = new Gladiator("Maximus", "Roman Legion", romanRed, "⚔️", "The General");
        gladiator2 = new Gladiator("Spartacus", "Thracian Warrior", romanGold, "🛡️", "The Rebel");
        computerGladiator = new Gladiator("Commodus", "Roman Emperor", romanPurple, "👑", "The Tyrant");
        arrow = new GladiatorArrow();
        arrowAngle = -Math.PI / 4;
        arrowPower = 0;
    }

    private void startGame(boolean vsComputer) {
        isComputerMode = vsComputer;
        currentGladiator = gladiator1;
        currentRound = 1;
        gladiator1.resetScore();
        gladiator2.resetScore();
        computerGladiator.resetScore();
        shotHistory.clear();
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

        // Play crowd sound effect (simulated)
        crowdChant = 1;
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
        crowdLabel.setText("");
        repaint();
    }

    public void updateUI() {
        if (state == GameState.PLAYING) {
            statusLabel.setText("🏛️ " + currentGladiator.title + " " + currentGladiator.name + " - DRAW YOUR BOW! 🏛️");
            scoreLabel1.setText(gladiator1.name + " (" + gladiator1.faction + "): " + gladiator1.score + " pts");

            if (isComputerMode) {
                scoreLabel2.setText(computerGladiator.name + " (" + computerGladiator.faction + "): " + computerGladiator.score + " pts");
                roundLabel.setText("⚔️ BATTLE ROUND " + currentRound + " of " + maxRounds + " ⚔️");
            } else {
                scoreLabel2.setText(gladiator2.name + " (" + gladiator2.faction + "): " + gladiator2.score + " pts");
                roundLabel.setText("⚔️ BATTLE ROUND " + currentRound + " of " + maxRounds + " ⚔️");
            }

            // Crowd reactions
            updateCrowdReaction();
        } else if (state == GameState.GAME_OVER) {
            determineVictor();
        }
    }

    private void updateCrowdReaction() {
        int totalScore = isComputerMode ? gladiator1.score + computerGladiator.score : gladiator1.score + gladiator2.score;
        if (totalScore > 0) {
            double percentage = (double)currentGladiator.score / totalScore * 100;
            if (percentage > 60) {
                crowdLabel.setText("🗣️ CROWD: " + currentGladiator.name + "! " + currentGladiator.name + "! 🗣️");
                crowdChant = 3;
            } else if (percentage > 40) {
                crowdLabel.setText("👏 The Colosseum applauds! 👏");
                crowdChant = 2;
            } else {
                crowdLabel.setText("😲 The crowd gasps! 😲");
                crowdChant = 1;
            }
        }
    }

    private void determineVictor() {
        String victor;
        String victoryType = "";

        if (isComputerMode) {
            if (gladiator1.score > computerGladiator.score) {
                victor = gladiator1.name + " the " + gladiator1.title;
                victoryType = "CAESAR APPROVES!";
                victoryMessage = "🏆 THE CROWD CARRIES YOU IN TRIUMPH! 🏆";
            } else if (computerGladiator.score > gladiator1.score) {
                victor = computerGladiator.name + " the " + computerGladiator.title;
                victoryType = "THE EMPEROR'S CHAMPION!";
                victoryMessage = "👑 HAIL THE NEW CHAMPION OF ROME! 👑";
            } else {
                victor = "Both Gladiators";
                victoryType = "A HONORABLE DRAW!";
                victoryMessage = "🤝 THE ARENA HONORS BOTH WARRIORS! 🤝";
            }
        } else {
            if (gladiator1.score > gladiator2.score) {
                victor = gladiator1.name + " the " + gladiator1.title;
                victoryType = "ARE YOU NOT ENTERTAINED?!";
                victoryMessage = "🏛️ THE LEGION CELEBRATES YOUR VICTORY! 🏛️";
            } else if (gladiator2.score > gladiator1.score) {
                victor = gladiator2.name + " the " + gladiator2.title;
                victoryType = "THE REBEL RISES!";
                victoryMessage = "🛡️ SPARTACUS STYLE VICTORY! 🛡️";
            } else {
                victor = "Both Warriors";
                victoryType = "A GLORIOUS TIE!";
                victoryMessage = "🤝 HONOR TO BOTH GLADIATORS! 🤝";
            }
        }

        statusLabel.setText("⚔️ VICTORY - " + victor + "! " + victoryType + " ⚔️");

        // Start victory celebration
        showVictoryParticles = true;
        for (int i = 0; i < 20; i++) {
            laurels.add(new LaurelWreath());
        }

        Timer victoryTimer = new Timer(4000, e -> {
            showVictoryParticles = false;
            laurels.clear();
        });
        victoryTimer.setRepeats(false);
        victoryTimer.start();

        state = GameState.VICTORY;
        powerBar.setVisible(false);
    }

    private void shootArrow() {
        if (isAnimating) return;

        currentScore = target.calculateScore(arrowAngle, arrowPower);
        currentGladiator.addScore((int)currentScore);

        // Add to history
        shotHistory.add(new ShotHistory(currentGladiator.name, currentRound, currentScore));

        // Blood effect on good hit
        if (currentScore >= 80) {
            showBloodEffect = true;
            bloodPosition = new Point(target.x + (int)(Math.random() * 60 - 30),
                    target.y + (int)(Math.random() * 60 - 30));
            Timer bloodTimer = new Timer(500, e -> showBloodEffect = false);
            bloodTimer.setRepeats(false);
            bloodTimer.start();
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
            double easeInOut = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
            arrowCurrentPos = new Point2D.Double(
                    arrowStartPos.getX() + (endX - arrowStartPos.getX()) * easeInOut,
                    arrowStartPos.getY() + (endY - arrowStartPos.getY()) * easeInOut
            );

            if (animationFrame >= animationDuration) {
                arrowAnimationTimer.stop();
                isAnimating = false;

                showScoreAnimation((int)currentScore);
                switchPlayer();

                if (isComputerMode && currentGladiator == computerGladiator && state == GameState.PLAYING) {
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

                // Roman scroll background
                g2d.setColor(new Color(245, 222, 179));
                g2d.fillRoundRect(0, 0, 350, 120, 20, 20);
                g2d.setColor(romanGold);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRoundRect(5, 5, 340, 110, 15, 15);

                // Score text
                g2d.setColor(romanRed);
                g2d.setFont(new Font("Serif", Font.BOLD, 32));
                String scoreText = "WOUND: " + score;
                FontMetrics fm = g2d.getFontMetrics();
                int width = fm.stringWidth(scoreText);
                g2d.drawString(scoreText, 175 - width/2, 65);

                // Latin subtitle
                g2d.setFont(new Font("Serif", Font.ITALIC, 14));
                String latin = score >= 80 ? "GLORIA VICTORIA!" : "BENE FACTUM!";
                width = fm.stringWidth(latin);
                g2d.drawString(latin, 175 - width/2, 95);
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
            if (currentGladiator == gladiator1) {
                currentGladiator = computerGladiator;
            } else {
                currentGladiator = gladiator1;
                currentRound++;
                if (currentRound > maxRounds) {
                    endGame();
                }
            }
        } else {
            if (currentGladiator == gladiator1) {
                currentGladiator = gladiator2;
            } else {
                currentGladiator = gladiator1;
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
            // AI with personality
            if (computerGladiator.title.equals("The Tyrant")) {
                // Aggressive AI
                arrowPower = 120 + Math.random() * 80;
                arrowAngle = -Math.PI / 2.8 + (Math.random() * Math.PI / 5);
            } else {
                // Balanced AI
                arrowPower = 80 + Math.random() * 100;
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

        // Draw colosseum background
        drawColosseumBackground(g2d);

        // Draw sand arena
        drawArenaSand(g2d);

        // Draw colosseum architecture
        drawColosseumArchitecture(g2d);

        // Draw crowd
        for (RomanCitizen citizen : crowd) {
            citizen.draw(g2d);
        }

        // Draw animals
        for (GladiatorAnimal animal : animals) {
            animal.draw(g2d);
        }

        // Draw torches and fire
        drawTorches(g2d);
        for (FireParticle fire : fireParticles) {
            fire.draw(g2d);
            fire.update();
        }

        // Draw dust clouds
        for (DustCloud dust : dustClouds) {
            dust.draw(g2d);
            dust.update();
        }

        // Draw target
        target.draw(g2d);

        // Draw blood effect
        if (showBloodEffect && bloodPosition != null) {
            drawBloodSplatter(g2d, bloodPosition.x, bloodPosition.y);
        }

        // Draw shot history
        drawShotHistory(g2d);

        // Draw gladiator
        if (state == GameState.PLAYING && !isAnimating && !(isComputerMode && currentGladiator == computerGladiator)) {
            currentGladiator.draw(g2d, 200, 500, arrowAngle, arrowPower);
            drawBowAndArrow(g2d);
        } else if (state == GameState.PLAYING && currentGladiator != null) {
            currentGladiator.drawIdle(g2d, 200, 500);
        }

        // Draw flying arrow
        if (isAnimating && arrowCurrentPos != null) {
            drawFlyingArrow(g2d);
        }

        // Draw power meter
        if (state == GameState.PLAYING && !isAnimating && !(isComputerMode && currentGladiator == computerGladiator)) {
            powerBar.setValue((int)arrowPower);
        }

        // Draw victory particles
        if (showVictoryParticles) {
            drawVictoryCelebration(g2d);
        }

        for (LaurelWreath laurel : laurels) {
            laurel.draw(g2d);
            laurel.update();
        }

        // Draw menu
        if (state == GameState.MENU) {
            drawRomanMenu(g2d);
        }
    }

    private void drawColosseumBackground(Graphics2D g2d) {
        // Sky gradient (Roman sunset)
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(255, 94, 77),
                0, 600, new Color(139, 0, 0));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Clouds
        g2d.setColor(new Color(255, 200, 150, 100));
        g2d.fillOval(200, 80, 120, 60);
        g2d.fillOval(260, 60, 100, 50);
        g2d.fillOval(800, 100, 150, 70);
    }

    private void drawArenaSand(Graphics2D g2d) {
        // Sand floor
        g2d.setColor(new Color(194, 178, 128));
        g2d.fillRect(0, 600, getWidth(), 300);

        // Sand texture
        g2d.setColor(new Color(160, 140, 100));
        for (int i = 0; i < 500; i++) {
            int x = (int)(Math.random() * getWidth());
            int y = 600 + (int)(Math.random() * 200);
            g2d.fillOval(x, y, 2, 2);
        }

        // Arena markings
        g2d.setColor(marbleWhite);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(100, 620, 200, 100);
        g2d.drawOval(1000, 620, 200, 100);
    }

    private void drawColosseumArchitecture(Graphics2D g2d) {
        // Colosseum arches background
        for (int i = 0; i < 12; i++) {
            int x = 50 + i * 110;
            g2d.setColor(stoneGray);
            g2d.fillRoundRect(x, 100, 90, 500, 30, 30);
            g2d.setColor(new Color(100, 100, 100));
            g2d.fillRoundRect(x + 15, 150, 60, 400, 20, 20);
            g2d.setColor(Color.BLACK);
            g2d.fillRoundRect(x + 25, 200, 40, 300, 15, 15);
        }

        // Top level
        g2d.setColor(marbleWhite);
        for (int i = 0; i < 60; i++) {
            int x = i * 25;
            g2d.fillRect(x, 90, 15, 20);
        }

        // Roman columns
        for (int i = 0; i < 8; i++) {
            int x = 100 + i * 160;
            g2d.setColor(marbleWhite);
            g2d.fillRect(x, 150, 30, 450);
            g2d.setColor(romanGold);
            g2d.fillRect(x + 5, 150, 20, 450);
        }
    }

    private void drawTorches(Graphics2D g2d) {
        for (int i = 0; i < 20; i++) {
            int x = 50 + i * 70;
            int y = 580;

            // Torch pole
            g2d.setColor(new Color(101, 67, 33));
            g2d.fillRect(x, y - 40, 8, 60);

            // Flame
            int flameHeight = 30 + (int)(Math.random() * 20);
            g2d.setColor(new Color(255, 100, 0, 200));
            g2d.fillOval(x - 5, y - 45 - flameHeight, 18, flameHeight);
            g2d.setColor(new Color(255, 200, 0, 200));
            g2d.fillOval(x - 2, (int) (y - 40 - flameHeight * 0.7f), 12, (int)(flameHeight * 0.7f));
        }
    }

    private void drawBowAndArrow(Graphics2D g2d) {
        int bowX = 180;
        int bowY = 500;

        double arrowEndX = bowX + arrowPower * Math.cos(arrowAngle);
        double arrowEndY = bowY + arrowPower * Math.sin(arrowAngle);

        // Roman bow
        g2d.setColor(new Color(101, 67, 33));
        g2d.setStroke(new BasicStroke(6));
        g2d.drawArc(bowX - 30, bowY - 45, 60, 90, 0, 180);

        // Gold decorations
        g2d.setColor(romanGold);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawArc(bowX - 28, bowY - 43, 56, 86, 0, 180);

        // Bow string
        g2d.setColor(new Color(200, 180, 100));
        g2d.drawLine(bowX - 25, bowY - 40, (int)arrowEndX, (int)arrowEndY);
        g2d.drawLine(bowX + 25, bowY - 40, (int)arrowEndX, (int)arrowEndY);

        // Arrow
        g2d.setColor(new Color(80, 60, 40));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(bowX, bowY, (int)arrowEndX, (int)arrowEndY);

        // Arrowhead (roman style)
        g2d.setColor(new Color(192, 192, 192));
        double angle = Math.atan2(arrowEndY - bowY, arrowEndX - bowX);
        int arrowheadX = (int)(arrowEndX + 18 * Math.cos(angle));
        int arrowheadY = (int)(arrowEndY + 18 * Math.sin(angle));
        g2d.fillPolygon(
                new int[]{(int)arrowEndX, arrowheadX, (int)arrowEndX},
                new int[]{(int)arrowEndY, arrowheadY, (int)(arrowEndY + 6)},
                3
        );

        // Roman eagle feather fletching
        g2d.setColor(new Color(255, 215, 0));
        int featherX = (int)(bowX + arrowPower * 0.6 * Math.cos(angle));
        int featherY = (int)(bowY + arrowPower * 0.6 * Math.sin(angle));
        g2d.drawLine(featherX, featherY, featherX - 12, featherY - 8);
        g2d.drawLine(featherX, featherY, featherX - 12, featherY + 8);
        g2d.drawLine(featherX, featherY, featherX - 8, featherY);
    }

    private void drawFlyingArrow(Graphics2D g2d) {
        if (arrowCurrentPos == null) return;

        g2d.setColor(new Color(80, 60, 40));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine((int)arrowCurrentPos.getX(), (int)arrowCurrentPos.getY(),
                (int)(arrowCurrentPos.getX() + 25), (int)(arrowCurrentPos.getY() + 6));

        // Motion trail
        for (int i = 1; i <= 4; i++) {
            g2d.setColor(new Color(80, 60, 40, 100 - i * 20));
            g2d.drawLine((int)(arrowCurrentPos.getX() - i * 6), (int)(arrowCurrentPos.getY() - i * 2),
                    (int)(arrowCurrentPos.getX() + 25 - i * 6), (int)(arrowCurrentPos.getY() + 6 - i * 2));
        }
    }

    private void drawBloodSplatter(Graphics2D g2d, int x, int y) {
        g2d.setColor(new Color(139, 0, 0, 180));
        for (int i = 0; i < 15; i++) {
            int offsetX = (int)(Math.random() * 30 - 15);
            int offsetY = (int)(Math.random() * 30 - 15);
            g2d.fillOval(x + offsetX, y + offsetY, 5 + (int)(Math.random() * 8), 5 + (int)(Math.random() * 8));
        }
    }

    private void drawShotHistory(Graphics2D g2d) {
        int y = 550;
        g2d.setFont(new Font("Serif", Font.BOLD, 12));
        for (ShotHistory shot : shotHistory) {
            g2d.setColor(romanGold);
            g2d.drawString("⚔️ " + shot.gladiator + ": " + (int)shot.score + " damage", 1150, y);
            y += 20;
            if (y > 750) break;
        }
    }

    private void drawVictoryCelebration(Graphics2D g2d) {
        // Victory banner
        g2d.setColor(romanRed);
        g2d.fillRect(400, 300, 600, 100);
        g2d.setColor(romanGold);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(405, 305, 590, 90);

        g2d.setFont(new Font("Serif", Font.BOLD, 36));
        String victory = victoryMessage;
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(victory);
        g2d.drawString(victory, 700 - width/2, 365);

        // Falling rose petals effect
        for (int i = 0; i < 50; i++) {
            g2d.setColor(new Color(139, 0, 0, 150));
            int x = 400 + (int)(Math.random() * 600);
            int y = 300 + (int)(Math.random() * 200);
            g2d.fillOval(x, y, 8, 5);
        }
    }

    private void drawRomanMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Roman archway
        g2d.setColor(marbleWhite);
        g2d.fillRoundRect(400, 100, 600, 600, 100, 100);
        g2d.setColor(stoneGray);
        g2d.fillRoundRect(420, 120, 560, 560, 80, 80);

        // Title
        g2d.setColor(romanGold);
        g2d.setFont(new Font("Serif", Font.BOLD, 56));
        String title = "GLADIATOR ARCHERY";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, 700 - titleWidth/2, 250);

        g2d.setFont(new Font("Serif", Font.ITALIC, 32));
        String subtitle = "Colosseum Tournament";
        int subWidth = fm.stringWidth(subtitle);
        g2d.drawString(subtitle, 700 - subWidth/2, 320);

        // Roman eagle emblem
        g2d.setColor(romanGold);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(650, 350, 100, 80);
        g2d.drawLine(700, 350, 700, 390);
        g2d.drawLine(700, 390, 680, 410);
        g2d.drawLine(700, 390, 720, 410);
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
        if (isComputerMode && currentGladiator == computerGladiator) return;

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            isDrawingArrow = true;
        } else if (e.getKeyCode() == KeyEvent.VK_UP && isDrawingArrow && arrowPower < 200) {
            arrowPower = Math.min(arrowPower + 10, 200);
            powerBar.setValue((int)arrowPower);
            // Dust effect
            dustLevel = (float) (arrowPower / 100f);
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

    // Inner classes
    class ColosseumTarget {
        int x, y;

        ColosseumTarget(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g2d) {
            // Roman shield target
            int[] radii = {95, 80, 65, 50, 35};
            Color[] colors = {romanRed, romanGold, romanPurple, new Color(200, 0, 0), Color.YELLOW};

            for (int i = 0; i < radii.length; i++) {
                g2d.setColor(colors[i]);
                g2d.fillOval(x - radii[i], y - radii[i], radii[i] * 2, radii[i] * 2);
            }

            // Roman eagle in center
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Serif", Font.BOLD, 20));
            g2d.drawString("SPQR", x - 18, y + 8);

            // Shield rim
            g2d.setColor(romanGold);
            g2d.setStroke(new BasicStroke(4));
            g2d.drawOval(x - 95, y - 95, 190, 190);

            // Target stand (Roman pilum)
            g2d.setColor(new Color(101, 67, 33));
            g2d.fillRect(x - 10, y + 20, 20, 80);
            g2d.fillRect(x - 50, y + 90, 100, 15);
        }

        double calculateScore(double angle, double power) {
            double endX = 1100 + power * Math.cos(angle) * 2.5;
            double endY = 400 + power * Math.sin(angle) * 2.5;
            double distance = Math.sqrt(Math.pow(endX - x, 2) + Math.pow(endY - y, 2));

            if (distance < 35) return 100;
            if (distance < 50) return 80;
            if (distance < 65) return 60;
            if (distance < 80) return 40;
            if (distance < 95) return 20;
            return 0;
        }
    }

    class Gladiator {
        String name;
        String faction;
        Color color;
        String emblem;
        String title;
        int score;
        float battleCry = 0;

        Gladiator(String name, String faction, Color color, String emblem, String title) {
            this.name = name;
            this.faction = faction;
            this.color = color;
            this.emblem = emblem;
            this.title = title;
            this.score = 0;
        }

        void draw(Graphics2D g2d, int x, int y, double angle, double power) {
            battleCry += 0.1f;

            // Draw gladiator body
            g2d.setColor(color);
            g2d.fillOval(x - 18, y - 45, 36, 45); // Helmet
            g2d.fillRect(x - 12, y - 25, 24, 45); // Body

            // Armor details
            g2d.setColor(romanGold);
            g2d.fillRect(x - 15, y - 20, 30, 15);
            g2d.fillRect(x - 8, y - 10, 16, 25);

            // Emblem on chest
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Serif", Font.BOLD, 14));
            g2d.drawString(emblem, x - 5, y - 8);

            // Sword on belt
            g2d.setColor(new Color(192, 192, 192));
            g2d.fillRect(x + 10, y - 5, 25, 5);
            g2d.fillOval(x + 32, y - 8, 8, 11);

            // Arm drawing bow
            int armX = (int)(x + 25 * Math.cos(battleCry));
            int armY = (int)(y - 15 * Math.sin(battleCry));
            g2d.drawLine(x + 12, y - 10, armX, armY);

            // Title
            g2d.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 11));
            g2d.drawString(title, x - 25, y + 25);
            g2d.drawString(name, x - 20, y + 40);
        }

        void drawIdle(Graphics2D g2d, int x, int y) {
            battleCry += 0.05f;
            int bounceY = (int)(Math.sin(battleCry) * 3);

            g2d.setColor(color);
            g2d.fillOval(x - 18, y - 45 + bounceY, 36, 45);
            g2d.fillRect(x - 12, y - 25 + bounceY, 24, 45);

            g2d.setColor(romanGold);
            g2d.fillRect(x - 15, y - 20 + bounceY, 30, 15);
            g2d.fillRect(x - 8, y - 10 + bounceY, 16, 25);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Serif", Font.BOLD, 14));
            g2d.drawString(emblem, x - 5, y - 8 + bounceY);

            g2d.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 11));
            g2d.drawString(title + " - READY", x - 35, y + 25 + bounceY);
        }

        void addScore(int points) {
            score += points;
        }

        void resetScore() {
            score = 0;
        }
    }

    class GladiatorArrow {
        // Placeholder
    }

    class ShotHistory {
        String gladiator;
        int round;
        double score;

        ShotHistory(String gladiator, int round, double score) {
            this.gladiator = gladiator;
            this.round = round;
            this.score = score;
        }
    }

    class RomanCitizen {
        int x, y, color, animation;

        RomanCitizen() {
            x = (int)(Math.random() * 1400);
            y = 500 + (int)(Math.random() * 100);
            color = (int)(Math.random() * 3);
            animation = (int)(Math.random() * 20);
        }

        void draw(Graphics2D g2d) {
            Color[] colors = {Color.WHITE, new Color(200, 180, 150), new Color(180, 160, 130)};
            g2d.setColor(colors[color]);
            g2d.fillOval(x, y, 8, 10);
            g2d.fillRect(x + 2, y + 8, 4, 8);

            // Toga
            g2d.setColor(romanRed);
            g2d.fillRect(x - 2, y + 10, 12, 6);

            // Animation (waving)
            if (crowdChant > 0) {
                g2d.setColor(romanGold);
                g2d.drawLine(x + 8, y + 5, x + 12 + (int)(Math.sin(animation) * 2), y + 2);
            }
        }
    }

    class GladiatorAnimal {
        String type;
        int x, y;
        float animation;

        GladiatorAnimal(String type, int x, int y) {
            this.type = type;
            this.x = x;
            this.y = y;
            animation = 0;
        }

        void draw(Graphics2D g2d) {
            animation += 0.1f;
            int moveX = (int)(Math.sin(animation) * 5);

            if (type.equals("lion")) {
                g2d.setColor(new Color(205, 133, 63));
                g2d.fillOval(x + moveX, y, 40, 25);
                g2d.fillOval(x + 25 + moveX, y - 10, 20, 20);
                g2d.setColor(Color.BLACK);
                g2d.fillOval(x + 30 + moveX, y - 8, 3, 3);
                g2d.fillOval(x + 38 + moveX, y - 8, 3, 3);
            } else if (type.equals("tiger")) {
                g2d.setColor(new Color(255, 140, 0));
                g2d.fillOval(x + moveX, y, 45, 28);
                g2d.fillOval(x + 28 + moveX, y - 12, 22, 22);
                g2d.setColor(Color.BLACK);
                for (int i = 0; i < 3; i++) {
                    g2d.fillRect(x + 10 + i * 10 + moveX, y + 5, 3, 15);
                }
            } else if (type.equals("eagle")) {
                g2d.setColor(new Color(139, 69, 19));
                g2d.fillOval(x + moveX, y, 30, 20);
                g2d.fillRect(x + 25 + moveX, y - 5, 15, 8);
                g2d.setColor(Color.YELLOW);
                g2d.fillPolygon(
                        new int[]{x + 35 + moveX, x + 45 + moveX, x + 38 + moveX},
                        new int[]{y, y - 5, y + 5},
                        3
                );
            }
        }
    }

    class FireParticle {
        int x, y, life;

        FireParticle() {
            reset();
        }

        void reset() {
            x = 50 + (int)(Math.random() * 1300);
            y = 550 + (int)(Math.random() * 50);
            life = 50 + (int)(Math.random() * 100);
        }

        void update() {
            life--;
            y -= 2;
            if (life <= 0 || y < 500) {
                reset();
            }
        }

        void draw(Graphics2D g2d) {
            int alpha = Math.min(255, life * 5);
            g2d.setColor(new Color(255, 100 + (int)(Math.random() * 100), 0, alpha));
            g2d.fillOval(x, y, 4, 8);
        }
    }

    class DustCloud {
        int x, y, size, life;

        DustCloud() {
            reset();
        }

        void reset() {
            x = (int)(Math.random() * 1400);
            y = 600 + (int)(Math.random() * 100);
            size = 5 + (int)(Math.random() * 15);
            life = 30 + (int)(Math.random() * 50);
        }

        void update() {
            life--;
            size += 1;
            if (life <= 0) {
                reset();
            }
        }

        void draw(Graphics2D g2d) {
            int alpha = Math.min(100, life * 3);
            g2d.setColor(new Color(160, 140, 100, alpha));
            g2d.fillOval(x, y, size, size);
        }
    }

    class LaurelWreath {
        int x, y, rotation, life;

        LaurelWreath() {
            x = (int)(Math.random() * 1400);
            y = (int)(Math.random() * 900);
            rotation = (int)(Math.random() * 360);
            life = 100;
        }

        void update() {
            y += 2;
            rotation += 5;
            life--;
        }

        void draw(Graphics2D g2d) {
            if (life <= 0) return;

            g2d.setColor(new Color(34, 139, 34, Math.min(255, life * 3)));
            g2d.rotate(Math.toRadians(rotation), x, y);
            g2d.drawOval(x - 10, y - 5, 20, 10);
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI * 2 / 8;
                int leafX = (int)(x + 8 * Math.cos(angle));
                int leafY = (int)(y + 4 * Math.sin(angle));
                g2d.fillOval(leafX, leafY, 4, 6);
            }
            g2d.rotate(-Math.toRadians(rotation), x, y);
        }
    }
}