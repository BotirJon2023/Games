// AvatarArcheryPandora.java
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.awt.image.BufferedImage;

public class AvatarArcheryPandora extends JFrame {
    private GamePanel gamePanel;

    public AvatarArcheryPandora() {
        setTitle("Avatar: Pandora Archery - Floating Mountains Tournament");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AvatarArcheryPandora());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private enum GameState { MENU, PLAYING, GAME_OVER, VICTORY }
    private GameState state = GameState.MENU;

    // Game objects
    private PandoraTarget target;
    private NaViArrow arrow;
    private NaVi currentWarrior;
    private NaVi jakeSully, neytiri, computerRDA;
    private boolean isComputerMode;
    private int currentRound;
    private int maxRounds = 3;
    private List<ShotRecord> shotRecords = new ArrayList<>();

    // Animation states
    private Timer gameTimer;
    private Timer arrowAnimationTimer;
    private Timer floatingTimer;
    private double arrowPower;
    private double arrowAngle;
    private boolean isDrawingArrow;
    private boolean isAnimating;
    private Point2D arrowStartPos;
    private Point2D arrowCurrentPos;
    private int animationDuration = 35;
    private int animationFrame = 0;
    private double currentScore = 0;
    private boolean showSpiritTree = false;
    private String eywaMessage = "";

    // Pandora environment
    private List<FloatingMountain> floatingMountains = new ArrayList<>();
    private List<SpiritParticle> spiritParticles = new ArrayList<>();
    private List<Banshee> banshees = new ArrayList<>();
    private List<GlowWorm> glowWorms = new ArrayList<>();
    private List<Vine> vines = new ArrayList<>();
    private List<Butterfly> butterflies = new ArrayList<>();

    // Nature effects
    private float bioluminescence = 0;
    private int floatingHeight = 0;
    private boolean showEywaBlessing = false;
    private String natureMessage = "";
    private float timeOfDay = 0;

    // UI Components
    private JButton onePlayerButton, twoPlayerButton, restartButton, menuButton;
    private JLabel statusLabel, scoreLabel1, scoreLabel2, roundLabel, natureLabel;
    private JProgressBar powerBar;

    // Pandora colors
    private Color pandoraNight = new Color(10, 20, 45);
    private Color pandoraSky = new Color(30, 40, 70);
    private Color naViBlue = new Color(0, 100, 150);
    private Color bioluminescentCyan = new Color(0, 255, 255);
    private Color bioluminescentPink = new Color(255, 50, 150);
    private Color bioluminescentGreen = new Color(0, 255, 100);
    private Color treeOfSouls = new Color(150, 50, 200);

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        setLayout(null);

        initializeGame();
        initializeUI();
        initializePandora();

        gameTimer = new Timer(30, this);
        gameTimer.start();

        // Create floating mountains
        for (int i = 0; i < 8; i++) {
            floatingMountains.add(new FloatingMountain(100 + i * 150, 200 + (int)(Math.random() * 100)));
        }

        // Create spirit particles (Eywa's energy)
        for (int i = 0; i < 150; i++) {
            spiritParticles.add(new SpiritParticle());
        }

        // Create Banshees (Ikran)
        for (int i = 0; i < 6; i++) {
            banshees.add(new Banshee());
        }

        // Create glow worms
        for (int i = 0; i < 80; i++) {
            glowWorms.add(new GlowWorm());
        }

        // Create vines
        for (int i = 0; i < 20; i++) {
            vines.add(new Vine(50 + i * 70));
        }

        // Create butterflies
        for (int i = 0; i < 40; i++) {
            butterflies.add(new Butterfly());
        }

        // Floating animation timer
        floatingTimer = new Timer(50, e -> {
            floatingHeight = (int)(Math.sin(System.currentTimeMillis() * 0.002) * 10);
            bioluminescence = (float)(Math.sin(System.currentTimeMillis() * 0.003) * 0.3 + 0.7);
            timeOfDay += 0.005;
            repaint();
        });
        floatingTimer.start();
    }

    private void initializePandora() {
        natureMessage = "🌀 I see you, warrior of Pandora 🌀";
        eywaMessage = "🌀 Eywa has heard you! 🌀";
    }

    private void initializeUI() {
        // Pandora styled buttons
        onePlayerButton = createPandoraButton("VS RDA (AI)", 500, 500);
        onePlayerButton.addActionListener(e -> startGame(true));

        twoPlayerButton = createPandoraButton("2 NA'VI WARRIORS", 500, 580);
        twoPlayerButton.addActionListener(e -> startGame(false));

        restartButton = createPandoraButton("RESTART JOURNEY", 50, 800);
        restartButton.addActionListener(e -> restartGame());
        restartButton.setVisible(false);

        menuButton = createPandoraButton("RETURN TO TREE OF SOULS", 250, 800);
        menuButton.addActionListener(e -> showMenu());
        menuButton.setVisible(false);

        // Pandora styled labels
        statusLabel = new JLabel("");
        statusLabel.setBounds(50, 200, 600, 40);
        statusLabel.setFont(new Font("Segoe UI Symbol", Font.BOLD, 20));
        statusLabel.setForeground(bioluminescentCyan);

        scoreLabel1 = new JLabel("");
        scoreLabel1.setBounds(50, 250, 450, 30);
        scoreLabel1.setFont(new Font("Monospaced", Font.BOLD, 16));
        scoreLabel1.setForeground(bioluminescentGreen);

        scoreLabel2 = new JLabel("");
        scoreLabel2.setBounds(50, 290, 450, 30);
        scoreLabel2.setFont(new Font("Monospaced", Font.BOLD, 16));
        scoreLabel2.setForeground(bioluminescentPink);

        roundLabel = new JLabel("");
        roundLabel.setBounds(50, 330, 450, 30);
        roundLabel.setFont(new Font("Segoe UI Symbol", Font.ITALIC, 14));
        roundLabel.setForeground(new Color(200, 200, 255));

        natureLabel = new JLabel("");
        natureLabel.setBounds(50, 370, 500, 30);
        natureLabel.setFont(new Font("Segoe UI Symbol", Font.BOLD | Font.ITALIC, 13));
        natureLabel.setForeground(bioluminescentCyan);

        // Power bar with Pandora theme
        powerBar = new JProgressBar(0, 200);
        powerBar.setBounds(50, 450, 300, 20);
        powerBar.setStringPainted(true);
        powerBar.setForeground(bioluminescentCyan);
        powerBar.setBackground(new Color(20, 30, 60));
        powerBar.setVisible(false);

        add(onePlayerButton);
        add(twoPlayerButton);
        add(restartButton);
        add(menuButton);
        add(statusLabel);
        add(scoreLabel1);
        add(scoreLabel2);
        add(roundLabel);
        add(natureLabel);
        add(powerBar);
    }

    private JButton createPandoraButton(String text, int x, int y) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Bioluminescent background
                GradientPaint gp = new GradientPaint(0, 0, new Color(20, 40, 80), 0, getHeight(), new Color(10, 20, 50));
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Glowing border
                g2d.setColor(bioluminescentCyan);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 12, 12);

                // Decorative vines
                g2d.setColor(bioluminescentGreen);
                g2d.drawLine(5, getHeight() - 10, 20, getHeight() - 20);
                g2d.drawLine(getWidth() - 5, getHeight() - 10, getWidth() - 20, getHeight() - 20);

                // Draw text
                g2d.setColor(bioluminescentCyan);
                g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                g2d.drawString(text, getWidth()/2 - textWidth/2, getHeight()/2 + 6);

                super.paintComponent(g);
            }
        };

        button.setBounds(x, y, 300, 60);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(30, 50, 90));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(null);
            }
        });

        return button;
    }

    private void initializeGame() {
        target = new PandoraTarget(1100, 400);
        jakeSully = new NaVi("Jake Sully", "Omaticaya", naViBlue, "🏹", "Toruk Makto", 100);
        neytiri = new NaVi("Neytiri", "Princess", new Color(0, 150, 200), "🌿", "Tsahìk", 90);
        computerRDA = new NaVi("Quaritch", "RDA", new Color(150, 50, 50), "🔫", "Colonel", 85);
        arrow = new NaViArrow();
        arrowAngle = -Math.PI / 4;
        arrowPower = 0;
    }

    private void startGame(boolean vsComputer) {
        isComputerMode = vsComputer;
        currentWarrior = jakeSully;
        currentRound = 1;
        jakeSully.resetScore();
        neytiri.resetScore();
        computerRDA.resetScore();
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

        // Eywa's blessing
        showEywaBlessing = true;
        natureMessage = "🌀 Eywa guides your arrow! 🌀";
        Timer blessingTimer = new Timer(2000, e -> showEywaBlessing = false);
        blessingTimer.setRepeats(false);
        blessingTimer.start();
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
        natureLabel.setText("");
        repaint();
    }

    public void updateUI() {
        if (state == GameState.PLAYING) {
            statusLabel.setText("🌿 " + currentWarrior.title + " " + currentWarrior.name + " - EYWA'S CHOSEN! 🌿");
            scoreLabel1.setText(jakeSully.name + " (" + jakeSully.clan + "): " + jakeSully.score + " nature points");

            if (isComputerMode) {
                scoreLabel2.setText(computerRDA.name + " (" + computerRDA.clan + "): " + computerRDA.score + " industrial points");
                roundLabel.setText("🌙 CONNECTION ROUND " + currentRound + " of " + maxRounds + " 🌙");
            } else {
                scoreLabel2.setText(neytiri.name + " (" + neytiri.clan + "): " + neytiri.score + " nature points");
                roundLabel.setText("🌙 CONNECTION ROUND " + currentRound + " of " + maxRounds + " 🌙");
            }

            updateNatureConnection();
        } else if (state == GameState.GAME_OVER) {
            determinePandoraWinner();
        }
    }

    private void updateNatureConnection() {
        int totalScore = isComputerMode ? jakeSully.score + computerRDA.score : jakeSully.score + neytiri.score;
        if (totalScore > 0) {
            double percentage = (double)currentWarrior.score / totalScore * 100;
            if (percentage > 60) {
                natureLabel.setText("🌿 EYWA SINGS FOR " + currentWarrior.name + "! 🌿");
                eywaMessage = "🌀 THE SPIRIT TREE BLESSES YOU! 🌀";
            } else if (percentage > 40) {
                natureLabel.setText("🍃 Pandora embraces your spirit 🍃");
                eywaMessage = "🌀 Feel the energy flow! 🌀";
            } else {
                natureLabel.setText("⚡ The balance of nature shifts ⚡");
                eywaMessage = "🌀 Connect deeper with Eywa! 🌀";
            }
        }
    }

    private void determinePandoraWinner() {
        String winner;
        String victoryType = "";

        if (isComputerMode) {
            if (jakeSully.score > computerRDA.score) {
                winner = jakeSully.name + " the " + jakeSully.title;
                victoryType = "PANDORA IS PROTECTED!";
                eywaMessage = "🌿 THE PEOPLE REJOICE! 🌿";
                showNatureExplosion();
            } else if (computerRDA.score > jakeSully.score) {
                winner = computerRDA.name + " the " + computerRDA.title;
                victoryType = "THE SKY PEOPLE ADVANCE!";
                eywaMessage = "💔 THE FOREST WEEPS 💔";
                showNatureExplosion();
            } else {
                winner = "Pandora";
                victoryType = "THE BALANCE HOLDS!";
                eywaMessage = "🌀 EYWA WATCHES OVER US 🌀";
            }
        } else {
            if (jakeSully.score > neytiri.score) {
                winner = jakeSully.name + " the " + jakeSully.title;
                victoryType = "TORUK MAKTO RISES!";
                eywaMessage = "🏹 THE TARONYU PROVES WORTHY! 🏹";
                showNatureExplosion();
            } else if (neytiri.score > jakeSully.score) {
                winner = neytiri.name + " the " + neytiri.title;
                victoryType = "THE PRINCESS SHINES!";
                eywaMessage = "🌺 TSAHÌK'S POWER REVEALED! 🌺";
                showNatureExplosion();
            } else {
                winner = "The Na'vi";
                victoryType = "HARMONY RESTORED!";
                eywaMessage = "🤝 TOGETHER AS ONE PEOPLE! 🤝";
            }
        }

        statusLabel.setText("🏆 VICTORY - " + winner + "! " + victoryType + " 🏆");

        state = GameState.VICTORY;
        powerBar.setVisible(false);

        Timer victoryTimer = new Timer(4000, e -> {});
        victoryTimer.setRepeats(false);
        victoryTimer.start();
    }

    private void showNatureExplosion() {
        for (int i = 0; i < 100; i++) {
            spiritParticles.add(new SpiritParticle());
        }
        showSpiritTree = true;
        Timer spiritTimer = new Timer(2000, e -> showSpiritTree = false);
        spiritTimer.setRepeats(false);
        spiritTimer.start();
    }

    private void shootArrow() {
        if (isAnimating) return;

        currentScore = target.calculateScore(arrowAngle, arrowPower);
        currentWarrior.addScore((int)currentScore);

        // Add to history
        shotRecords.add(new ShotRecord(currentWarrior.name, currentRound, currentScore));

        // Nature reaction
        if (currentScore >= 80) {
            natureMessage = "🌀 EYWA BLESSES YOUR SHOT! 🌀";
            showEywaBlessing = true;
            Timer blessingTimer = new Timer(1500, e -> showEywaBlessing = false);
            blessingTimer.setRepeats(false);
            blessingTimer.start();
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

                if (isComputerMode && currentWarrior == computerRDA && state == GameState.PLAYING) {
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

                // Spirit tree background
                g2d.setColor(new Color(20, 30, 70));
                g2d.fillRoundRect(0, 0, 350, 120, 20, 20);
                g2d.setColor(bioluminescentCyan);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(5, 5, 340, 110, 15, 15);

                // Score text
                g2d.setColor(bioluminescentCyan);
                g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 28));
                String scoreText = "🌿 +" + score + " NATURE POINTS! 🌿";
                FontMetrics fm = g2d.getFontMetrics();
                int width = fm.stringWidth(scoreText);
                g2d.drawString(scoreText, 175 - width/2, 65);

                // Na'vi phrase
                g2d.setFont(new Font("Segoe UI Symbol", Font.ITALIC, 12));
                String phrase = score >= 80 ? "I SEE YOU!" : "THE SPIRIT TREE GUIDES YOU";
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
            if (currentWarrior == jakeSully) {
                currentWarrior = computerRDA;
                natureLabel.setText("🔫 RDA forces take aim! 🔫");
            } else {
                currentWarrior = jakeSully;
                currentRound++;
                if (currentRound > maxRounds) {
                    endGame();
                }
                natureLabel.setText("🏹 Jake Sully returns to the forest! 🏹");
            }
        } else {
            if (currentWarrior == jakeSully) {
                currentWarrior = neytiri;
                natureLabel.setText("🌺 Neytiri calls upon nature! 🌺");
            } else {
                currentWarrior = jakeSully;
                currentRound++;
                if (currentRound > maxRounds) {
                    endGame();
                }
                natureLabel.setText("🏹 Toruk Makto takes the bow! 🏹");
            }
        }
        updateUI();

        Timer messageTimer = new Timer(2000, e -> updateNatureConnection());
        messageTimer.setRepeats(false);
        messageTimer.start();
    }

    private void computerTurn() {
        Timer timer = new Timer(1000, e -> {
            // RDA's aggressive AI
            if (computerRDA.title.equals("Colonel")) {
                arrowPower = 120 + Math.random() * 80;
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

        // Pandora sky with bioluminescence
        float r = 0.1f + (float)(Math.sin(timeOfDay) * 0.05f);
        float gVal = 0.2f + (float)(Math.sin(timeOfDay + 1) * 0.1f);
        float b = 0.4f + (float)(Math.sin(timeOfDay + 2) * 0.2f);
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(r, gVal, b),
                0, 600, new Color(0.05f, 0.1f, 0.3f));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw floating mountains
        for (FloatingMountain mountain : floatingMountains) {
            mountain.draw(g2d);
            mountain.update();
        }

        // Draw Banshees (Ikran)
        for (Banshee banshee : banshees) {
            banshee.draw(g2d);
            banshee.update();
        }

        // Draw glow worms
        for (GlowWorm worm : glowWorms) {
            worm.draw(g2d);
            worm.update();
        }

        // Draw vines
        for (Vine vine : vines) {
            vine.draw(g2d);
        }

        // Draw butterflies
        for (Butterfly butterfly : butterflies) {
            butterfly.draw(g2d);
            butterfly.update();
        }

        // Draw spirit particles (Eywa's energy)
        for (SpiritParticle particle : spiritParticles) {
            particle.draw(g2d);
            particle.update();
        }

        // Draw target (Tree of Souls)
        target.draw(g2d);

        // Draw shot history on leaf
        drawShotHistory(g2d);

        // Draw Na'vi warrior
        if (state == GameState.PLAYING && !isAnimating && !(isComputerMode && currentWarrior == computerRDA)) {
            currentWarrior.draw(g2d, 200, 500, arrowAngle, arrowPower);
            drawNaViBow(g2d);
        } else if (state == GameState.PLAYING && currentWarrior != null) {
            currentWarrior.drawIdle(g2d, 200, 500);
        }

        // Draw flying arrow
        if (isAnimating && arrowCurrentPos != null) {
            drawFlyingArrow(g2d);
        }

        // Draw power meter (Nature energy)
        if (state == GameState.PLAYING && !isAnimating && !(isComputerMode && currentWarrior == computerRDA)) {
            powerBar.setValue((int)arrowPower);
            drawNatureMeter(g2d);
        }

        // Draw Eywa's blessing
        if (showEywaBlessing && !eywaMessage.isEmpty()) {
            drawEywaBlessing(g2d);
        }

        // Draw spirit tree effect
        if (showSpiritTree) {
            drawSpiritTreeEffect(g2d);
        }

        // Draw menu
        if (state == GameState.MENU) {
            drawPandoraMenu(g2d);
        }
    }

    private void drawNaViBow(Graphics2D g2d) {
        int bowX = 180;
        int bowY = 500;

        double arrowEndX = bowX + arrowPower * Math.cos(arrowAngle);
        double arrowEndY = bowY + arrowPower * Math.sin(arrowAngle);

        // Na'vi wooden bow
        g2d.setColor(new Color(80, 50, 30));
        g2d.setStroke(new BasicStroke(6));
        g2d.drawArc(bowX - 30, bowY - 45, 60, 90, 0, 180);

        // Bioluminescent patterns on bow
        g2d.setColor(bioluminescentCyan);
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < 5; i++) {
            double angle = Math.PI * i / 4;
            int x = (int)(bowX - 15 + 20 * Math.cos(angle));
            int y = (int)(bowY - 20 + 20 * Math.sin(angle));
            g2d.fillOval(x, y, 3, 3);
        }

        // Bow string (woven vine)
        g2d.setColor(new Color(100, 150, 100));
        g2d.drawLine(bowX - 25, bowY - 40, (int)arrowEndX, (int)arrowEndY);
        g2d.drawLine(bowX + 25, bowY - 40, (int)arrowEndX, (int)arrowEndY);

        // Na'vi arrow
        g2d.setColor(new Color(80, 60, 40));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(bowX, bowY, (int)arrowEndX, (int)arrowEndY);

        // Obsidian arrowhead
        g2d.setColor(new Color(50, 50, 80));
        double angle = Math.atan2(arrowEndY - bowY, arrowEndX - bowX);
        int arrowheadX = (int)(arrowEndX + 18 * Math.cos(angle));
        int arrowheadY = (int)(arrowEndY + 18 * Math.sin(angle));
        g2d.fillPolygon(
                new int[]{(int)arrowEndX, arrowheadX, (int)arrowEndX},
                new int[]{(int)arrowEndY, arrowheadY, (int)(arrowEndY + 6)},
                3
        );

        // Feather fletching
        g2d.setColor(new Color(0, 150, 200));
        int featherX = (int)(bowX + arrowPower * 0.6 * Math.cos(angle));
        int featherY = (int)(bowY + arrowPower * 0.6 * Math.sin(angle));
        g2d.drawLine(featherX, featherY, featherX - 12, featherY - 8);
        g2d.drawLine(featherX, featherY, featherX - 12, featherY + 8);
    }

    private void drawFlyingArrow(Graphics2D g2d) {
        if (arrowCurrentPos == null) return;

        g2d.setColor(new Color(80, 60, 40));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine((int)arrowCurrentPos.getX(), (int)arrowCurrentPos.getY(),
                (int)(arrowCurrentPos.getX() + 25), (int)(arrowCurrentPos.getY() + 6));

        // Bioluminescent trail
        for (int i = 1; i <= 5; i++) {
            g2d.setColor(new Color(0, 255, 255, 100 - i * 15));
            g2d.drawLine((int)(arrowCurrentPos.getX() - i * 6), (int)(arrowCurrentPos.getY() - i * 2),
                    (int)(arrowCurrentPos.getX() + 25 - i * 6), (int)(arrowCurrentPos.getY() + 6 - i * 2));
        }
    }

    private void drawNatureMeter(Graphics2D g2d) {
        int meterX = 50;
        int meterY = 430;

        g2d.setColor(new Color(20, 30, 60));
        g2d.fillRect(meterX, meterY, 300, 15);
        g2d.setColor(bioluminescentGreen);
        g2d.fillRect(meterX, meterY, (int)(300 * arrowPower / 200), 15);

        g2d.setColor(bioluminescentCyan);
        g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 12));
        g2d.drawString("🌿 NATURE ENERGY: " + (int)arrowPower + "% 🌿", meterX, meterY - 5);
        g2d.drawString("🎯 PANDORA SPIRIT: " + (int)Math.toDegrees(arrowAngle) + "°", meterX + 150, meterY - 5);
    }

    private void drawShotHistory(Graphics2D g2d) {
        int y = 550;
        g2d.setColor(new Color(20, 40, 20, 200));
        g2d.fillRoundRect(1130, 540, 230, 160, 15, 15);
        g2d.setColor(bioluminescentGreen);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(1130, 540, 230, 160, 15, 15);

        g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 12));
        g2d.drawString("🌿 EYWA'S MEMORY 🌿", 1160, 560);

        for (int i = 0; i < Math.min(6, shotRecords.size()); i++) {
            ShotRecord shot = shotRecords.get(shotRecords.size() - 1 - i);
            g2d.setColor(bioluminescentCyan);
            g2d.drawString(shot.warrior + ": " + (int)shot.score + " pts", 1145, 585 + i * 20);
        }
    }

    private void drawEywaBlessing(Graphics2D g2d) {
        // Ethereal glow
        g2d.setColor(new Color(0, 255, 200, 50));
        g2d.fillRoundRect(750, 80, 500, 60, 25, 25);
        g2d.setColor(bioluminescentCyan);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(750, 80, 500, 60, 25, 25);

        g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD | Font.ITALIC, 16));
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(eywaMessage);
        g2d.drawString(eywaMessage, 1000 - width/2, 118);
    }

    private void drawSpiritTreeEffect(Graphics2D g2d) {
        g2d.setColor(new Color(150, 50, 200, 100));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(bioluminescentPink);
        g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 48));
        String spirit = "🌀 THE SPIRIT TREE BLESSES PANDORA 🌀";
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(spirit);
        g2d.drawString(spirit, 700 - width/2, 450);
    }

    private void drawPandoraMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 230));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Tree of Souls silhouette
        g2d.setColor(new Color(150, 50, 200, 100));
        for (int i = 0; i < 20; i++) {
            int x = 700 + (int)(Math.sin(i) * 100);
            int y = 200 + i * 20;
            g2d.fillOval(x, y, 10, 10);
        }

        g2d.setColor(bioluminescentCyan);
        g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 56));
        String title = "🌿 AVATAR: PANDORA ARCHERY 🌿";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, 700 - titleWidth/2, 350);

        g2d.setFont(new Font("Segoe UI Symbol", Font.ITALIC, 24));
        String subtitle = "Floating Mountains Tournament";
        int subWidth = fm.stringWidth(subtitle);
        g2d.drawString(subtitle, 700 - subWidth/2, 420);

        g2d.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
        String quote = "\"I see you, warrior of Pandora\"";
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
        if (isComputerMode && currentWarrior == computerRDA) return;

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

    // Inner classes for Pandora theme
    class PandoraTarget {
        int x, y;

        PandoraTarget(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g2d) {
            // Tree of Souls target
            int[] radii = {95, 80, 65, 50, 35};
            Color[] colors = {new Color(80, 40, 100), new Color(100, 50, 120),
                    new Color(120, 60, 140), treeOfSouls, bioluminescentPink};

            for (int i = 0; i < radii.length; i++) {
                g2d.setColor(colors[i]);
                g2d.fillOval(x - radii[i], y - radii[i], radii[i] * 2, radii[i] * 2);
            }

            // Glowing center
            g2d.setColor(bioluminescentCyan);
            g2d.fillOval(x - 15, y - 15, 30, 30);

            // Spirit tree branches
            g2d.setColor(bioluminescentPink);
            g2d.setStroke(new BasicStroke(3));
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI * 2 / 8;
                int x2 = (int)(x + 40 * Math.cos(angle));
                int y2 = (int)(y + 40 * Math.sin(angle));
                g2d.drawLine(x, y, x2, y2);
                g2d.fillOval(x2 - 3, y2 - 3, 6, 6);
            }

            // Target stand (floating root)
            g2d.setColor(new Color(60, 40, 30));
            for (int i = 0; i < 3; i++) {
                g2d.fillOval(x - 20 + i * 20, y + 30, 10, 40);
            }
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

    class NaVi {
        String name;
        String clan;
        Color color;
        String emblem;
        String title;
        int score;
        float tailSway = 0;

        NaVi(String name, String clan, Color color, String emblem, String title, int baseScore) {
            this.name = name;
            this.clan = clan;
            this.color = color;
            this.emblem = emblem;
            this.title = title;
            this.score = 0;
        }

        void draw(Graphics2D g2d, int x, int y, double angle, double power) {
            tailSway += 0.1f;

            // Tail
            g2d.setColor(color);
            int[] tailX = {x + 15, x + 30 + (int)(Math.sin(tailSway) * 5), x + 25};
            int[] tailY = {y + 10, y + 25, y + 30};
            g2d.fillPolygon(tailX, tailY, 3);

            // Body
            g2d.setColor(color);
            g2d.fillRect(x - 12, y - 45, 24, 50);

            // Head
            g2d.fillOval(x - 15, y - 60, 30, 30);

            // Braid (Queue - neural connection)
            g2d.setColor(new Color(100, 50, 30));
            g2d.drawLine(x + 12, y - 50, x + 25, y - 40);
            g2d.fillOval(x + 23, y - 43, 6, 6);

            // Eyes (big Na'vi eyes)
            g2d.setColor(Color.YELLOW);
            g2d.fillOval(x - 10, y - 58, 8, 8);
            g2d.fillOval(x + 2, y - 58, 8, 8);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x - 8, y - 57, 4, 4);
            g2d.fillOval(x + 4, y - 57, 4, 4);

            // Bow arm
            int armX = (int)(x + 20 * Math.cos(tailSway));
            int armY = (int)(y - 20 * Math.sin(tailSway));
            g2d.drawLine(x + 12, y - 25, armX, armY);

            // Title and name
            g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD, 11));
            g2d.drawString(title, x - 25, y + 15);
            g2d.drawString(name, x - 20, y + 27);
            g2d.drawString(clan, x - 18, y + 39);
        }

        void drawIdle(Graphics2D g2d, int x, int y) {
            tailSway += 0.05f;
            int idleMove = (int)(Math.sin(tailSway) * 2);

            g2d.setColor(color);
            int[] tailX = {x + 15, x + 30 + (int)(Math.sin(tailSway) * 5), x + 25};
            int[] tailY = {y + 10 + idleMove, y + 25 + idleMove, y + 30 + idleMove};
            g2d.fillPolygon(tailX, tailY, 3);

            g2d.fillRect(x - 12, y - 45 + idleMove, 24, 50);
            g2d.fillOval(x - 15, y - 60 + idleMove, 30, 30);

            g2d.setColor(Color.YELLOW);
            g2d.fillOval(x - 10, y - 58 + idleMove, 8, 8);
            g2d.fillOval(x + 2, y - 58 + idleMove, 8, 8);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x - 8, y - 57 + idleMove, 4, 4);
            g2d.fillOval(x + 4, y - 57 + idleMove, 4, 4);

            g2d.setFont(new Font("Segoe UI Symbol", Font.BOLD | Font.ITALIC, 11));
            g2d.drawString(title + " - CONNECTED TO EYWA", x - 45, y + 25 + idleMove);
        }

        void addScore(int points) {
            score += points;
        }

        void resetScore() {
            score = 0;
        }
    }

    class NaViArrow {
        // Placeholder
    }

    class ShotRecord {
        String warrior;
        int round;
        double score;

        ShotRecord(String warrior, int round, double score) {
            this.warrior = warrior;
            this.round = round;
            this.score = score;
        }
    }

    class FloatingMountain {
        int x, baseY;
        float floatOffset;

        FloatingMountain(int x, int baseY) {
            this.x = x;
            this.baseY = baseY;
            this.floatOffset = (float)Math.random() * 100;
        }

        void update() {
            floatOffset += 0.02f;
        }

        void draw(Graphics2D g2d) {
            int y = baseY + (int)(Math.sin(floatOffset) * 15);

            // Mountain body
            g2d.setColor(new Color(60, 70, 90));
            int[] xPoints = {x, x + 30, x + 60, x + 90, x + 80, x + 60, x + 40};
            int[] yPoints = {y, y + 40, y + 30, y + 50, y + 80, y + 60, y + 70};
            g2d.fillPolygon(xPoints, yPoints, 7);

            // Glowing vines on mountain
            g2d.setColor(bioluminescentGreen);
            for (int i = 0; i < 5; i++) {
                g2d.drawLine(x + 20 + i * 10, y + 30, x + 30 + i * 8, y + 60);
            }

            // Floating rocks
            g2d.setColor(new Color(50, 60, 80));
            g2d.fillOval(x - 20, y - 10 + (int)(Math.sin(floatOffset + 1) * 5), 15, 10);
            g2d.fillOval(x + 70, y + 20 + (int)(Math.sin(floatOffset + 2) * 5), 12, 8);
        }
    }

    class Banshee {
        int x, y;
        float wingFlap;

        Banshee() {
            x = (int)(Math.random() * 1500);
            y = (int)(Math.random() * 400);
            wingFlap = 0;
        }

        void update() {
            x -= 2;
            wingFlap += 0.2f;
            if (x < -100) {
                x = 1500;
                y = (int)(Math.random() * 400);
            }
        }

        void draw(Graphics2D g2d) {
            // Body
            g2d.setColor(new Color(50, 150, 200));
            g2d.fillOval(x, y, 30, 15);

            // Wings
            int wingY = (int)(Math.sin(wingFlap) * 15);
            g2d.fillPolygon(new int[]{x + 10, x - 20, x + 5},
                    new int[]{y + 5, y + 10 + wingY, y + 15}, 3);
            g2d.fillPolygon(new int[]{x + 20, x + 50, x + 25},
                    new int[]{y + 5, y + 10 + wingY, y + 15}, 3);

            // Head
            g2d.fillOval(x + 25, y - 5, 12, 12);
            g2d.setColor(Color.YELLOW);
            g2d.fillOval(x + 28, y - 3, 3, 3);
        }
    }

    class SpiritParticle {
        int x, y;
        int life;

        SpiritParticle() {
            x = (int)(Math.random() * 1400);
            y = (int)(Math.random() * 900);
            life = 100 + (int)(Math.random() * 100);
        }

        void update() {
            y -= 1;
            life--;
            if (life <= 0) {
                x = (int)(Math.random() * 1400);
                y = 900;
                life = 100;
            }
        }

        void draw(Graphics2D g2d) {
            int alpha = Math.min(255, life * 3);
            g2d.setColor(new Color(0, 255, 200, alpha));
            g2d.fillOval(x, y, 4, 4);
        }
    }

    class GlowWorm {
        int x, y;
        float glow;

        GlowWorm() {
            x = (int)(Math.random() * 1400);
            y = (int)(Math.random() * 300 + 500);
            glow = (float)Math.random();
        }

        void update() {
            glow += 0.05f;
            x += 1;
            if (x > 1400) {
                x = 0;
            }
        }

        void draw(Graphics2D g2d) {
            float intensity = (float)(Math.sin(glow) * 0.5 + 0.5);
            g2d.setColor(new Color(0, 200, 255, (int)(intensity * 150)));
            g2d.fillOval(x, y, 3, 3);
        }
    }

    class Vine {
        int x;

        Vine(int x) {
            this.x = x;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(30, 80, 30));
            for (int i = 0; i < 5; i++) {
                g2d.drawLine(x + i * 3, 600, x + (int)(Math.sin(i) * 10), 700);
            }
            // Glowing flowers
            g2d.setColor(bioluminescentPink);
            g2d.fillOval(x + 5, 590, 5, 5);
            g2d.fillOval(x + 15, 585, 4, 4);
        }
    }

    class Butterfly {
        int x, y;
        float wing;

        Butterfly() {
            x = (int)(Math.random() * 1400);
            y = (int)(Math.random() * 400 + 500);
            wing = 0;
        }

        void update() {
            x += (Math.random() - 0.5) * 3;
            y += (Math.random() - 0.5) * 2;
            wing += 0.3f;
            if (x > 1400) x = 0;
            if (x < 0) x = 1400;
        }

        void draw(Graphics2D g2d) {
            int wingSize = (int)(Math.sin(wing) * 5 + 8);
            g2d.setColor(bioluminescentCyan);
            g2d.fillOval(x - wingSize, y, wingSize, wingSize / 2);
            g2d.fillOval(x, y, wingSize, wingSize / 2);
            g2d.setColor(bioluminescentPink);
            g2d.fillOval(x - 2, y + 2, 4, 4);
        }
    }
}