// GothamArcheryBatman.java
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.awt.image.BufferedImage;

public class GothamArcheryBatman extends JFrame {
    private GamePanel gamePanel;

    public GothamArcheryBatman() {
        setTitle("Gotham Archery - Batman: Dark Knight Tournament");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GothamArcheryBatman());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private enum GameState { MENU, PLAYING, GAME_OVER, VICTORY }
    private GameState state = GameState.MENU;

    // Game objects
    private GothamTarget target;
    private Batarang arrow;
    private DarkKnight currentHero;
    private DarkKnight batman, robin, computerVillain;
    private boolean isComputerMode;
    private int currentRound;
    private int maxRounds = 3;
    private List<ShotRecord> shotRecords = new ArrayList<>();

    // Animation states
    private Timer gameTimer;
    private Timer arrowAnimationTimer;
    private Timer lightningTimer;
    private double arrowPower;
    private double arrowAngle;
    private boolean isDrawingArrow;
    private boolean isAnimating;
    private Point2D arrowStartPos;
    private Point2D arrowCurrentPos;
    private int animationDuration = 35;
    private int animationFrame = 0;
    private double currentScore = 0;
    private boolean showBatSignal = false;
    private String batMessage = "";

    // Gotham environment
    private List<Building> buildings = new ArrayList<>();
    private List<RainDrop> rainDrops = new ArrayList<>();
    private List<Lightning> lightnings = new ArrayList<>();
    private List<Bat> bats = new ArrayList<>();
    private List<Smoke> smokeClouds = new ArrayList<>();
    private List<Spotlight> spotlights = new ArrayList<>();

    // Weather effects
    private float lightningFlash = 0;
    private int fogIntensity = 0;
    private boolean showExplosion = false;
    private Point explosionPoint;
    private String villainMessage = "";

    // UI Components
    private JButton onePlayerButton, twoPlayerButton, restartButton, menuButton;
    private JLabel statusLabel, scoreLabel1, scoreLabel2, roundLabel, crimeLabel;
    private JProgressBar powerBar;

    // Gotham colors
    private Color gothamDark = new Color(20, 20, 30);
    private Color gothamNight = new Color(10, 10, 20);
    private Color batYellow = new Color(255, 215, 0);
    private Color batGray = new Color(169, 169, 169);
    private Color jokerPurple = new Color(128, 0, 128);
    private Color villainGreen = new Color(0, 100, 0);

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        setLayout(null);

        initializeGame();
        initializeUI();
        initializeGotham();

        gameTimer = new Timer(30, this);
        gameTimer.start();

        // Create Gotham skyline
        for (int i = 0; i < 20; i++) {
            buildings.add(new Building(50 + i * 70, 300 + (int)(Math.random() * 200)));
        }

        // Create rain
        for (int i = 0; i < 200; i++) {
            rainDrops.add(new RainDrop());
        }

        // Create bats
        for (int i = 0; i < 15; i++) {
            bats.add(new Bat());
        }

        // Create smoke
        for (int i = 0; i < 30; i++) {
            smokeClouds.add(new Smoke());
        }

        // Create spotlights
        for (int i = 0; i < 5; i++) {
            spotlights.add(new Spotlight(200 + i * 200));
        }

        // Lightning timer
        lightningTimer = new Timer(3000, e -> {
            if (state == GameState.PLAYING && Math.random() > 0.7) {
                lightnings.add(new Lightning());
                lightningFlash = 1.0f;
                Timer flashTimer = new Timer(100, ev -> lightningFlash = 0);
                flashTimer.setRepeats(false);
                flashTimer.start();
            }
        });
        lightningTimer.start();
    }

    private void initializeGotham() {
        // Initialize crime messages
        String[] crimes = {"RIDDLER STRIKES!", "JOKER LAUGHS!", "TWO-FACE GAMBLES!", "PENGUIN RISES!"};
        villainMessage = crimes[(int)(Math.random() * crimes.length)];
    }

    private void initializeUI() {
        // Gotham styled buttons
        onePlayerButton = createGothamButton("VS JOKER (AI)", 500, 500);
        onePlayerButton.addActionListener(e -> startGame(true));

        twoPlayerButton = createGothamButton("2 HEROES", 500, 580);
        twoPlayerButton.addActionListener(e -> startGame(false));

        restartButton = createGothamButton("RESTART PATROL", 50, 800);
        restartButton.addActionListener(e -> restartGame());
        restartButton.setVisible(false);

        menuButton = createGothamButton("RETURN TO BATCAVE", 250, 800);
        menuButton.addActionListener(e -> showMenu());
        menuButton.setVisible(false);

        // Gotham styled labels
        statusLabel = new JLabel("");
        statusLabel.setBounds(50, 200, 600, 40);
        statusLabel.setFont(new Font("Arial Black", Font.BOLD, 20));
        statusLabel.setForeground(batYellow);

        scoreLabel1 = new JLabel("");
        scoreLabel1.setBounds(50, 250, 450, 30);
        scoreLabel1.setFont(new Font("Monospaced", Font.BOLD, 16));
        scoreLabel1.setForeground(new Color(100, 100, 255));

        scoreLabel2 = new JLabel("");
        scoreLabel2.setBounds(50, 290, 450, 30);
        scoreLabel2.setFont(new Font("Monospaced", Font.BOLD, 16));
        scoreLabel2.setForeground(new Color(255, 100, 100));

        roundLabel = new JLabel("");
        roundLabel.setBounds(50, 330, 450, 30);
        roundLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        roundLabel.setForeground(Color.LIGHT_GRAY);

        crimeLabel = new JLabel("");
        crimeLabel.setBounds(50, 370, 450, 30);
        crimeLabel.setFont(new Font("Arial Black", Font.BOLD, 14));
        crimeLabel.setForeground(new Color(255, 0, 0));

        // Power bar with Bat-tech theme
        powerBar = new JProgressBar(0, 200);
        powerBar.setBounds(50, 450, 300, 20);
        powerBar.setStringPainted(true);
        powerBar.setForeground(batYellow);
        powerBar.setBackground(Color.DARK_GRAY);
        powerBar.setVisible(false);

        add(onePlayerButton);
        add(twoPlayerButton);
        add(restartButton);
        add(menuButton);
        add(statusLabel);
        add(scoreLabel1);
        add(scoreLabel2);
        add(roundLabel);
        add(crimeLabel);
        add(powerBar);
    }

    private JButton createGothamButton(String text, int x, int y) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Dark metallic background
                GradientPaint gp = new GradientPaint(0, 0, gothamDark, 0, getHeight(), Color.BLACK);
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Bat symbol decoration
                g2d.setColor(batYellow);
                g2d.fillPolygon(new int[]{getWidth() - 35, getWidth() - 30, getWidth() - 25, getWidth() - 35, getWidth() - 45},
                        new int[]{20, 30, 20, 35, 35}, 5);

                // Border
                g2d.setColor(batYellow);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 12, 12);

                // Draw text
                g2d.setColor(batYellow);
                g2d.setFont(new Font("Arial Black", Font.BOLD, 16));
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
                button.setBackground(new Color(30, 30, 40));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(null);
            }
        });

        return button;
    }

    private void initializeGame() {
        target = new GothamTarget(1100, 400);
        batman = new DarkKnight("Batman", "Bruce Wayne", batGray, "🦇", "The Dark Knight", 100);
        robin = new DarkKnight("Robin", "Tim Drake", new Color(0, 150, 0), "R", "Boy Wonder", 80);
        computerVillain = new DarkKnight("Joker", "Unknown", jokerPurple, "🎭", "Clown Prince", 90);
        arrow = new Batarang();
        arrowAngle = -Math.PI / 4;
        arrowPower = 0;
    }

    private void startGame(boolean vsComputer) {
        isComputerMode = vsComputer;
        currentHero = batman;
        currentRound = 1;
        batman.resetScore();
        robin.resetScore();
        computerVillain.resetScore();
        shotRecords.clear();
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

        // Bat signal appears
        showBatSignal = true;
        batMessage = "🦇 GOTHAM NEEDS YOU! 🦇";
        Timer signalTimer = new Timer(2000, e -> showBatSignal = false);
        signalTimer.setRepeats(false);
        signalTimer.start();
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
        crimeLabel.setText("");
        repaint();
    }

    public void updateUI() {
        if (state == GameState.PLAYING) {
            statusLabel.setText("🦇 " + currentHero.title + " " + currentHero.name + " - GOTHAM'S HOPE! 🦇");
            scoreLabel1.setText(batman.name + " (" + batman.identity + "): " + batman.score + " justice points");

            if (isComputerMode) {
                scoreLabel2.setText(computerVillain.name + " (" + computerVillain.identity + "): " + computerVillain.score + " chaos points");
                roundLabel.setText("🌙 CRIME ROUND " + currentRound + " of " + maxRounds + " 🌙");
            } else {
                scoreLabel2.setText(robin.name + " (" + robin.identity + "): " + robin.score + " justice points");
                roundLabel.setText("🌙 CRIME ROUND " + currentRound + " of " + maxRounds + " 🌙");
            }

            updateCrimeAlert();
        } else if (state == GameState.GAME_OVER) {
            determineGothamWinner();
        }
    }

    private void updateCrimeAlert() {
        int totalScore = isComputerMode ? batman.score + computerVillain.score : batman.score + robin.score;
        if (totalScore > 0) {
            double percentage = (double)currentHero.score / totalScore * 100;
            if (percentage > 60) {
                crimeLabel.setText("🦇 BATMAN SAVES GOTHAM! 🦇");
                batMessage = "I AM VENGEANCE!";
            } else if (percentage > 40) {
                crimeLabel.setText("⚡ GOTHAM IS FIGHTING BACK! ⚡");
                batMessage = "NEVER GIVE UP!";
            } else {
                crimeLabel.setText("💀 CHAOS RISES IN GOTHAM! 💀");
                batMessage = "THE NIGHT IS DARKEST...";
            }
        }
    }

    private void determineGothamWinner() {
        String winner;
        String victoryType = "";

        if (isComputerMode) {
            if (batman.score > computerVillain.score) {
                winner = batman.name + " the " + batman.title;
                victoryType = "GOTHAM IS SAFE!";
                batMessage = "🦇 JUSTICE PREVAILS! 🦇";
                showExplosionEffect(700, 400);
            } else if (computerVillain.score > batman.score) {
                winner = computerVillain.name + " the " + computerVillain.title;
                victoryType = "CHAOS REIGNS!";
                batMessage = "💀 WHY SO SERIOUS? 💀";
                showExplosionEffect(700, 400);
            } else {
                winner = "Gotham";
                victoryType = "A DARK NIGHT!";
                batMessage = "🌙 THE BATMAN ENDURES 🌙";
            }
        } else {
            if (batman.score > robin.score) {
                winner = batman.name + " the " + batman.title;
                victoryType = "THE DARK KNIGHT RISES!";
                batMessage = "🦇 I'M BATMAN! 🦇";
                showExplosionEffect(700, 400);
            } else if (robin.score > batman.score) {
                winner = robin.name + " the " + robin.title;
                victoryType = "THE BOY WONDER SHINES!";
                batMessage = "⚡ ROBIN SAVES THE DAY! ⚡";
                showExplosionEffect(700, 400);
            } else {
                winner = "The Dynamic Duo";
                victoryType = "A HEROIC TIE!";
                batMessage = "🦇 PARTNERS IN JUSTICE! 🦇";
            }
        }

        statusLabel.setText("⚔️ VICTORY - " + winner + "! " + victoryType + " ⚔️");

        state = GameState.VICTORY;
        powerBar.setVisible(false);

        Timer victoryTimer = new Timer(4000, e -> {
            // Reset effects
        });
        victoryTimer.setRepeats(false);
        victoryTimer.start();
    }

    private void showExplosionEffect(int x, int y) {
        showExplosion = true;
        explosionPoint = new Point(x, y);
        Timer explosionTimer = new Timer(500, e -> showExplosion = false);
        explosionTimer.setRepeats(false);
        explosionTimer.start();
    }

    private void shootArrow() {
        if (isAnimating) return;

        currentScore = target.calculateScore(arrowAngle, arrowPower);
        currentHero.addScore((int)currentScore);

        // Add to history
        shotRecords.add(new ShotRecord(currentHero.name, currentRound, currentScore));

        // Joker laugh on villain hit
        if (currentScore >= 80 && currentHero == computerVillain) {
            batMessage = "🤡 HAHAHAHA! 🤡";
            showBatSignal = true;
            Timer laughTimer = new Timer(1500, e -> showBatSignal = false);
            laughTimer.setRepeats(false);
            laughTimer.start();
        }

        // Batarang animation
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

                if (isComputerMode && currentHero == computerVillain && state == GameState.PLAYING) {
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

                // Bat-signal background
                g2d.setColor(gothamDark);
                g2d.fillRoundRect(0, 0, 350, 120, 20, 20);
                g2d.setColor(batYellow);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRoundRect(5, 5, 340, 110, 15, 15);

                // Score text
                g2d.setColor(batYellow);
                g2d.setFont(new Font("Arial Black", Font.BOLD, 28));
                String scoreText = "🦇 +" + score + " JUSTICE! 🦇";
                FontMetrics fm = g2d.getFontMetrics();
                int width = fm.stringWidth(scoreText);
                g2d.drawString(scoreText, 175 - width/2, 65);

                // Batman phrase
                g2d.setFont(new Font("Arial", Font.ITALIC, 12));
                String phrase = score >= 80 ? "I'M BATMAN!" : "GOTHAM BELIEVES IN YOU";
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
            if (currentHero == batman) {
                currentHero = computerVillain;
                crimeLabel.setText("💀 JOKER'S TURN - WHY SO SERIOUS? 💀");
            } else {
                currentHero = batman;
                currentRound++;
                if (currentRound > maxRounds) {
                    endGame();
                }
                crimeLabel.setText("🦇 BATMAN RETURNS TO THE SHADOWS 🦇");
            }
        } else {
            if (currentHero == batman) {
                currentHero = robin;
                crimeLabel.setText("⚡ ROBIN - THE BOY WONDER TAKES AIM ⚡");
            } else {
                currentHero = batman;
                currentRound++;
                if (currentRound > maxRounds) {
                    endGame();
                }
                crimeLabel.setText("🦇 THE DARK KNIGHT RISES AGAIN 🦇");
            }
        }
        updateUI();

        Timer messageTimer = new Timer(2000, e -> updateCrimeAlert());
        messageTimer.setRepeats(false);
        messageTimer.start();
    }

    private void computerTurn() {
        Timer timer = new Timer(1000, e -> {
            // Joker's chaotic AI
            if (computerVillain.title.equals("Clown Prince")) {
                arrowPower = 100 + Math.random() * 100;
                arrowAngle = -Math.PI / 2.5 + (Math.random() * Math.PI / 4);
            } else {
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

        // Lightning flash effect
        if (lightningFlash > 0) {
            g2d.setColor(new Color(1, 1, 1, lightningFlash));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        // Dark Gotham sky
        GradientPaint skyGradient = new GradientPaint(0, 0, gothamNight, 0, 600, new Color(5, 5, 15));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Moon
        g2d.setColor(new Color(200, 200, 220));
        g2d.fillOval(1000, 50, 80, 80);
        g2d.setColor(gothamNight);
        g2d.fillOval(1015, 60, 65, 65);

        // Draw buildings
        for (Building building : buildings) {
            building.draw(g2d);
        }

        // Draw windows (lighted)
        drawCityWindows(g2d);

        // Draw rain
        for (RainDrop rain : rainDrops) {
            rain.draw(g2d);
            rain.update();
        }

        // Draw lightning
        for (Lightning lightning : lightnings) {
            lightning.draw(g2d);
            lightning.update();
        }

        // Draw bats
        for (Bat bat : bats) {
            bat.draw(g2d);
            bat.update();
        }

        // Draw smoke
        for (Smoke smoke : smokeClouds) {
            smoke.draw(g2d);
            smoke.update();
        }

        // Draw spotlights
        for (Spotlight light : spotlights) {
            light.draw(g2d);
        }

        // Draw Bat-signal
        if (showBatSignal) {
            drawBatSignal(g2d);
        }

        // Draw target (Gotham City Bank)
        target.draw(g2d);

        // Draw shot history
        drawShotHistory(g2d);

        // Draw hero/villain
        if (state == GameState.PLAYING && !isAnimating && !(isComputerMode && currentHero == computerVillain)) {
            currentHero.draw(g2d, 200, 500, arrowAngle, arrowPower);
            drawBatBowAndArrow(g2d);
        } else if (state == GameState.PLAYING && currentHero != null) {
            currentHero.drawIdle(g2d, 200, 500);
        }

        // Draw flying batarang
        if (isAnimating && arrowCurrentPos != null) {
            drawFlyingBatarang(g2d);
        }

        // Draw power meter (Bat-tech)
        if (state == GameState.PLAYING && !isAnimating && !(isComputerMode && currentHero == computerVillain)) {
            powerBar.setValue((int)arrowPower);
            drawBatMeter(g2d);
        }

        // Draw explosion
        if (showExplosion && explosionPoint != null) {
            drawExplosion(g2d, explosionPoint.x, explosionPoint.y);
        }

        // Draw bat message
        if (!batMessage.isEmpty() && state == GameState.PLAYING) {
            drawBatMessage(g2d);
        }

        // Draw menu
        if (state == GameState.MENU) {
            drawGothamMenu(g2d);
        }
    }

    private void drawCityWindows(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 100, 150));
        for (int i = 0; i < 200; i++) {
            int x = (int)(Math.random() * 1400);
            int y = (int)(Math.random() * 500);
            if (Math.random() > 0.7) {
                g2d.fillRect(x, y, 3, 5);
            }
        }
    }

    private void drawBatSignal(Graphics2D g2d) {
        // Bat-signal beam
        g2d.setColor(new Color(255, 215, 0, 50));
        int[] xPoints = {600, 800, 800, 600};
        int[] yPoints = {0, 200, 500, 300};
        g2d.fillPolygon(xPoints, yPoints, 4);

        // Bat symbol
        g2d.setColor(batYellow);
        g2d.fillPolygon(new int[]{700, 710, 695, 700, 690, 700, 710, 700, 720, 705, 700},
                new int[]{100, 110, 115, 120, 115, 125, 115, 120, 115, 110, 100}, 11);
    }

    private void drawBatBowAndArrow(Graphics2D g2d) {
        int bowX = 180;
        int bowY = 500;

        double arrowEndX = bowX + arrowPower * Math.cos(arrowAngle);
        double arrowEndY = bowY + arrowPower * Math.sin(arrowAngle);

        // Bat-tech bow
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(6));
        g2d.drawArc(bowX - 30, bowY - 45, 60, 90, 0, 180);

        // Carbon fiber details
        g2d.setColor(batGray);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawArc(bowX - 28, bowY - 43, 56, 86, 0, 180);

        // High-tech string
        g2d.setColor(new Color(200, 200, 255));
        g2d.drawLine(bowX - 25, bowY - 40, (int)arrowEndX, (int)arrowEndY);
        g2d.drawLine(bowX + 25, bowY - 40, (int)arrowEndX, (int)arrowEndY);

        // Batarang
        g2d.setColor(Color.BLACK);
        g2d.fillPolygon(new int[]{(int)arrowEndX, (int)(arrowEndX + 15), (int)arrowEndX, (int)(arrowEndX - 15)},
                new int[]{(int)arrowEndY, (int)(arrowEndY + 5), (int)(arrowEndY + 15), (int)(arrowEndY + 5)}, 4);
    }

    private void drawFlyingBatarang(Graphics2D g2d) {
        if (arrowCurrentPos == null) return;

        g2d.setColor(Color.BLACK);
        g2d.fillPolygon(new int[]{(int)arrowCurrentPos.getX(), (int)(arrowCurrentPos.getX() + 20),
                        (int)arrowCurrentPos.getX(), (int)(arrowCurrentPos.getX() - 20)},
                new int[]{(int)arrowCurrentPos.getY(), (int)(arrowCurrentPos.getY() + 8),
                        (int)(arrowCurrentPos.getY() + 20), (int)(arrowCurrentPos.getY() + 8)}, 4);

        // Motion trail
        g2d.setColor(new Color(0, 0, 0, 100));
        for (int i = 1; i <= 3; i++) {
            g2d.fillPolygon(new int[]{(int)(arrowCurrentPos.getX() - i * 8), (int)(arrowCurrentPos.getX() + 20 - i * 8),
                            (int)(arrowCurrentPos.getX() - i * 8), (int)(arrowCurrentPos.getX() - 20 - i * 8)},
                    new int[]{(int)(arrowCurrentPos.getY()), (int)(arrowCurrentPos.getY() + 8),
                            (int)(arrowCurrentPos.getY() + 20), (int)(arrowCurrentPos.getY() + 8)}, 4);
        }
    }

    private void drawBatMeter(Graphics2D g2d) {
        int meterX = 50;
        int meterY = 430;

        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(meterX, meterY, 300, 15);
        g2d.setColor(batYellow);
        g2d.fillRect(meterX, meterY, (int)(300 * arrowPower / 200), 15);

        g2d.setColor(batYellow);
        g2d.setFont(new Font("Arial Black", Font.BOLD, 12));
        g2d.drawString("🦇 BAT-TECH POWER: " + (int)arrowPower + "% 🦇", meterX, meterY - 5);
        g2d.drawString("🎯 TARGETING: " + (int)Math.toDegrees(arrowAngle) + "°", meterX + 150, meterY - 5);
    }

    private void drawShotHistory(Graphics2D g2d) {
        int y = 550;
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRoundRect(1130, 540, 230, 160, 10, 10);
        g2d.setColor(batYellow);
        g2d.drawRoundRect(1130, 540, 230, 160, 10, 10);

        g2d.setFont(new Font("Arial Black", Font.BOLD, 12));
        g2d.drawString("🦇 CRIME LOG 🦇", 1160, 560);

        for (int i = 0; i < Math.min(6, shotRecords.size()); i++) {
            ShotRecord shot = shotRecords.get(shotRecords.size() - 1 - i);
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawString(shot.hero + ": " + (int)shot.score + " pts", 1145, 585 + i * 20);
        }
    }

    private void drawExplosion(Graphics2D g2d, int x, int y) {
        for (int i = 0; i < 30; i++) {
            int radius = (int)(Math.random() * 30);
            g2d.setColor(new Color(255, 100 + (int)(Math.random() * 155), 0, 200 - i * 5));
            g2d.fillOval(x - radius/2 + (int)(Math.random() * radius),
                    y - radius/2 + (int)(Math.random() * radius),
                    radius, radius);
        }
    }

    private void drawBatMessage(Graphics2D g2d) {
        // Speech bubble
        g2d.setColor(Color.BLACK);
        g2d.fillRoundRect(800, 80, 400, 60, 20, 20);
        g2d.setColor(batYellow);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(800, 80, 400, 60, 20, 20);

        g2d.setFont(new Font("Arial Black", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(batMessage);
        g2d.drawString(batMessage, 1000 - width/2, 118);
    }

    private void drawGothamMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 230));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Bat-signal in menu
        g2d.setColor(batYellow);
        g2d.fillPolygon(new int[]{700, 720, 695, 700, 685, 700, 720, 700, 730, 710, 700},
                new int[]{250, 265, 270, 280, 270, 285, 265, 280, 270, 265, 250}, 11);

        g2d.setFont(new Font("Arial Black", Font.BOLD, 56));
        String title = "BATMAN: GOTHAM ARCHERY";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, 700 - titleWidth/2, 350);

        g2d.setFont(new Font("Arial", Font.ITALIC, 24));
        String subtitle = "Dark Knight Tournament";
        int subWidth = fm.stringWidth(subtitle);
        g2d.drawString(subtitle, 700 - subWidth/2, 420);

        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        String quote = "\"I am vengeance. I am the night. I am BATMAN!\"";
        int quoteWidth = fm.stringWidth(quote);
        g2d.drawString(quote, 700 - quoteWidth/2, 480);
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
        if (isComputerMode && currentHero == computerVillain) return;

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

    // Inner classes for Gotham theme
    class GothamTarget {
        int x, y;

        GothamTarget(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g2d) {
            // Gotham City Bank target
            int[] radii = {90, 75, 60, 45, 30};
            Color[] colors = {Color.DARK_GRAY, new Color(60, 60, 70), batGray, batYellow, Color.BLACK};

            for (int i = 0; i < radii.length; i++) {
                g2d.setColor(colors[i]);
                g2d.fillOval(x - radii[i], y - radii[i], radii[i] * 2, radii[i] * 2);
            }

            // Bat symbol in center
            g2d.setColor(batYellow);
            g2d.fillPolygon(new int[]{x - 10, x, x + 10, x, x - 5, x, x + 5, x},
                    new int[]{y - 8, y - 15, y - 8, y - 3, y + 2, y - 1, y + 2, y - 1}, 8);

            // Bank vault door details
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x - 20, y - 20, 40, 40);
            g2d.fillOval(x - 3, y - 3, 6, 6);

            // Target stand (Gotham street lamp)
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(x - 10, y + 30, 20, 70);
            g2d.fillOval(x - 15, y + 90, 30, 15);
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

    class DarkKnight {
        String name;
        String identity;
        Color color;
        String emblem;
        String title;
        int score;
        float capeFlow = 0;

        DarkKnight(String name, String identity, Color color, String emblem, String title, int baseScore) {
            this.name = name;
            this.identity = identity;
            this.color = color;
            this.emblem = emblem;
            this.title = title;
            this.score = 0;
        }

        void draw(Graphics2D g2d, int x, int y, double angle, double power) {
            capeFlow += 0.1f;

            // Cape
            g2d.setColor(Color.BLACK);
            int[] capeX = {x - 15, x - 40 + (int)(Math.sin(capeFlow) * 5), x - 30, x, x + 30, x + 40 + (int)(Math.sin(capeFlow) * 5), x + 15};
            int[] capeY = {y - 30, y - 10, y + 20, y + 30, y + 20, y - 10, y - 30};
            g2d.fillPolygon(capeX, capeY, 7);

            // Cowl/Head
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x - 18, y - 50, 36, 40);

            // Bat ears
            g2d.fillPolygon(new int[]{x - 18, x - 25, x - 12}, new int[]{y - 50, y - 65, y - 55}, 3);
            g2d.fillPolygon(new int[]{x + 18, x + 25, x + 12}, new int[]{y - 50, y - 65, y - 55}, 3);

            // Body armor
            g2d.setColor(color);
            g2d.fillRect(x - 15, y - 25, 30, 45);

            // Bat emblem on chest
            g2d.setColor(batYellow);
            g2d.fillPolygon(new int[]{x - 5, x, x + 5, x, x - 2, x, x + 2, x},
                    new int[]{y - 15, y - 22, y - 15, y - 10, y - 5, y - 8, y - 5, y - 8}, 8);

            // Utility belt
            g2d.setColor(batYellow);
            g2d.fillRect(x - 12, y - 5, 24, 6);
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(x - 10, y - 4, 20, 4);

            // Title and name
            g2d.setFont(new Font("Arial Black", Font.BOLD, 11));
            g2d.drawString(title, x - 25, y + 30);
            g2d.drawString(name, x - 20, y + 42);
        }

        void drawIdle(Graphics2D g2d, int x, int y) {
            capeFlow += 0.05f;
            int stanceY = (int)(Math.sin(capeFlow) * 2);

            g2d.setColor(Color.BLACK);
            int[] capeX = {x - 15, x - 40 + (int)(Math.sin(capeFlow) * 5), x - 30, x, x + 30, x + 40 + (int)(Math.sin(capeFlow) * 5), x + 15};
            int[] capeY = {y - 30 + stanceY, y - 10 + stanceY, y + 20 + stanceY, y + 30 + stanceY, y + 20 + stanceY, y - 10 + stanceY, y - 30 + stanceY};
            g2d.fillPolygon(capeX, capeY, 7);

            g2d.setColor(Color.BLACK);
            g2d.fillOval(x - 18, y - 50 + stanceY, 36, 40);
            g2d.fillPolygon(new int[]{x - 18, x - 25, x - 12}, new int[]{y - 50 + stanceY, y - 65 + stanceY, y - 55 + stanceY}, 3);
            g2d.fillPolygon(new int[]{x + 18, x + 25, x + 12}, new int[]{y - 50 + stanceY, y - 65 + stanceY, y - 55 + stanceY}, 3);

            g2d.setColor(color);
            g2d.fillRect(x - 15, y - 25 + stanceY, 30, 45);

            g2d.setColor(batYellow);
            g2d.fillPolygon(new int[]{x - 5, x, x + 5, x, x - 2, x, x + 2, x},
                    new int[]{y - 15 + stanceY, y - 22 + stanceY, y - 15 + stanceY, y - 10 + stanceY, y - 5 + stanceY, y - 8 + stanceY, y - 5 + stanceY, y - 8 + stanceY}, 8);

            g2d.setFont(new Font("Arial Black", Font.BOLD, 11));
            g2d.drawString(title + " - WATCHING OVER GOTHAM", x - 45, y + 35 + stanceY);
        }

        void addScore(int points) {
            score += points;
        }

        void resetScore() {
            score = 0;
        }
    }

    class Batarang {
        // Placeholder
    }

    class ShotRecord {
        String hero;
        int round;
        double score;

        ShotRecord(String hero, int round, double score) {
            this.hero = hero;
            this.round = round;
            this.score = score;
        }
    }

    class Building {
        int x, height;

        Building(int x, int height) {
            this.x = x;
            this.height = height;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(15, 15, 25));
            g2d.fillRect(x, height, 60, 600 - height);
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawRect(x, height, 60, 600 - height);

            // Windows
            g2d.setColor(new Color(255, 255, 150, 100));
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 4; j++) {
                    if (Math.random() > 0.7) {
                        g2d.fillRect(x + 10 + j * 12, height + 20 + i * 30, 5, 8);
                    }
                }
            }
        }
    }

    class RainDrop {
        int x, y;

        RainDrop() {
            x = (int)(Math.random() * 1400);
            y = (int)(Math.random() * 900);
        }

        void update() {
            y += 5;
            if (y > 900) {
                y = 0;
                x = (int)(Math.random() * 1400);
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(100, 150, 255, 100));
            g2d.drawLine(x, y, x, y + 5);
        }
    }

    class Lightning {
        int x, life;

        Lightning() {
            x = (int)(Math.random() * 1400);
            life = 5;
        }

        void update() {
            life--;
        }

        void draw(Graphics2D g2d) {
            if (life <= 0) return;
            g2d.setColor(new Color(255, 255, 100, life * 50));
            g2d.setStroke(new BasicStroke(3));
            for (int i = 0; i < 5; i++) {
                int y1 = (int)(Math.random() * 300);
                int y2 = y1 + (int)(Math.random() * 100);
                g2d.drawLine(x, y1, x + (int)(Math.random() * 50 - 25), y2);
            }
        }
    }

    class Bat {
        int x, y;
        float wing;

        Bat() {
            x = (int)(Math.random() * 1400);
            y = (int)(Math.random() * 400);
            wing = 0;
        }

        void update() {
            x -= 2;
            wing += 0.2f;
            if (x < 0) {
                x = 1400;
                y = (int)(Math.random() * 400);
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(Color.BLACK);
            int wingSpan = (int)(Math.sin(wing) * 10 + 15);
            g2d.fillPolygon(new int[]{x, x - wingSpan, x - 5, x, x + 5, x + wingSpan},
                    new int[]{y, y - 8, y + 5, y + 3, y + 5, y - 8}, 6);
        }
    }

    class Smoke {
        int x, y, size;

        Smoke() {
            x = (int)(Math.random() * 1400);
            y = (int)(Math.random() * 900);
            size = 10 + (int)(Math.random() * 20);
        }

        void update() {
            size += 1;
            if (size > 50) {
                size = 5;
                x = (int)(Math.random() * 1400);
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(50, 50, 50, 50));
            g2d.fillOval(x, y, size, size);
        }
    }

    class Spotlight {
        int x;

        Spotlight(int x) {
            this.x = x;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(255, 255, 200, 30));
            g2d.fillPolygon(new int[]{x, x + 20, x + 100, x + 80},
                    new int[]{0, 0, 200, 200}, 4);
        }
    }
}