import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class WimbledonVerticalTennis extends JFrame {
    private GamePanel gamePanel;
    private JPanel controlPanel, scorePanel, statsPanel;
    private JLabel player1NameLabel, player2NameLabel, player1ScoreLabel, player2ScoreLabel;
    private JLabel setScoreLabel, tournamentLabel, speedLabel;
    private JButton serveButton, hitButton, newMatchButton, modeButton;
    private Timer animationTimer, crowdTimer;
    private Random random = new Random();

    // Game state
    private boolean isTwoPlayerMode = true;
    private int player1Points = 0;
    private int player2Points = 0;
    private int player1Games = 0;
    private int player2Games = 0;
    private int player1Sets = 0;
    private int player2Sets = 0;
    private boolean isPlayer1Turn = true;
    private boolean ballInPlay = false;
    private boolean isTiebreak = false;
    private double currentBallSpeed = 0;

    // Wimbledon players
    private String[] wimbledonPlayers = {
            "🇬🇧 C. Alcaraz", "🇷🇸 N. Djokovic", "🇮🇹 J. Sinner",
            "🇩🇪 A. Zverev", "🇷🇺 D. Medvedev", "🇬🇧 C. Norrie"
    };
    private String currentPlayer1, currentPlayer2;

    public WimbledonVerticalTennis() {
        setTitle("🎾 WIMBLEDON CHAMPIONSHIPS 2026 - Vertical View 🎾");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(10, 25, 15));

        // Random player selection
        currentPlayer1 = wimbledonPlayers[random.nextInt(wimbledonPlayers.length)];
        do {
            currentPlayer2 = wimbledonPlayers[random.nextInt(wimbledonPlayers.length)];
        } while (currentPlayer2.equals(currentPlayer1));

        // Create game panel (vertical orientation)
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(800, 900));
        gamePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 150, 50), 5),
                BorderFactory.createLineBorder(new Color(255, 215, 0), 2)
        ));

        // Create panels
        scorePanel = createScorePanel();
        controlPanel = createControlPanel();
        statsPanel = createStatsPanel();

        // Add components
        add(scorePanel, BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controlPanel, BorderLayout.CENTER);
        bottomPanel.add(statsPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Setup controls
        setupKeyboardControls();

        // Animation timer
        animationTimer = new Timer(16, e -> {
            if (ballInPlay) {
                gamePanel.updateBall();
                if (gamePanel.isBallOut()) {
                    handlePointEnd();
                }
                updateSpeedDisplay();
                repaint();
            }
        });

        // Crowd animation
        crowdTimer = new Timer(2500, e -> {
            if (ballInPlay && random.nextInt(3) == 0) {
                gamePanel.cheerCrowd();
                repaint();
            }
        });
        crowdTimer.start();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        startNewMatch();
    }

    private JPanel createScorePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 50, 40));
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Wimbledon title
        tournamentLabel = new JLabel("🏆 THE CHAMPIONSHIPS - WIMBLEDON 2026 🏆", SwingConstants.CENTER);
        tournamentLabel.setFont(new Font("Times New Roman", Font.BOLD, 22));
        tournamentLabel.setForeground(new Color(255, 215, 0));
        tournamentLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 215, 0), 2),
                new EmptyBorder(8, 20, 8, 20)
        ));

        // Players score panel
        JPanel playersPanel = new JPanel(new GridLayout(1, 3, 30, 0));
        playersPanel.setBackground(new Color(30, 50, 40));
        playersPanel.setBorder(new EmptyBorder(15, 0, 10, 0));

        // Player 1 (Top on court)
        JPanel p1Panel = createPlayerPanel(currentPlayer1, new Color(0, 150, 100), true);
        player1NameLabel = (JLabel) p1Panel.getComponent(0);
        player1ScoreLabel = (JLabel) p1Panel.getComponent(3);

        // Center set score
        JPanel centerPanel = new JPanel(new GridLayout(3, 1));
        centerPanel.setBackground(new Color(20, 50, 30));
        centerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 200, 0), 2),
                new EmptyBorder(15, 25, 15, 25)
        ));

        setScoreLabel = new JLabel("0 - 0", SwingConstants.CENTER);
        setScoreLabel.setFont(new Font("Arial", Font.BOLD, 32));
        setScoreLabel.setForeground(new Color(255, 215, 0));

        JLabel gamesLabel = new JLabel("GAMES", SwingConstants.CENTER);
        gamesLabel.setFont(new Font("Arial", Font.BOLD, 12));
        gamesLabel.setForeground(Color.WHITE);

        JLabel setsLabel = new JLabel("Best of 5 Sets", SwingConstants.CENTER);
        setsLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        setsLabel.setForeground(new Color(200, 200, 200));

        centerPanel.add(setScoreLabel);
        centerPanel.add(gamesLabel);
        centerPanel.add(setsLabel);

        // Player 2 (Bottom on court)
        JPanel p2Panel = createPlayerPanel(currentPlayer2, new Color(200, 50, 80), false);
        player2NameLabel = (JLabel) p2Panel.getComponent(0);
        player2ScoreLabel = (JLabel) p2Panel.getComponent(3);

        playersPanel.add(p1Panel);
        playersPanel.add(centerPanel);
        playersPanel.add(p2Panel);

        panel.add(tournamentLabel, BorderLayout.NORTH);
        panel.add(playersPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPlayerPanel(String name, Color color, boolean isTop) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(40, 60, 50));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                new EmptyBorder(10, 20, 10, 20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(color);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel rankingLabel = new JLabel("★ Top 10");
        rankingLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        rankingLabel.setForeground(Color.GRAY);

        JLabel scoreLabel = new JLabel("0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 48));
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel pointsLabel = new JLabel("POINTS");
        pointsLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        pointsLabel.setForeground(Color.GRAY);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(nameLabel, gbc);
        gbc.gridy = 1; panel.add(rankingLabel, gbc);
        gbc.gridy = 2; panel.add(scoreLabel, gbc);
        gbc.gridy = 3; panel.add(pointsLabel, gbc);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setBackground(new Color(20, 30, 25));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        serveButton = createWimbledonButton("🎾 SERVE", new Color(0, 120, 80));
        hitButton = createWimbledonButton("🏓 HIT", new Color(180, 100, 40));
        newMatchButton = createWimbledonButton("🔄 NEW MATCH", new Color(80, 120, 80));
        modeButton = createWimbledonButton("👥 TWO PLAYER", new Color(120, 80, 160));

        serveButton.addActionListener(e -> serve());
        hitButton.addActionListener(e -> playerHit());
        newMatchButton.addActionListener(e -> startNewMatch());
        modeButton.addActionListener(e -> toggleMode());

        panel.add(serveButton);
        panel.add(hitButton);
        panel.add(newMatchButton);
        panel.add(modeButton);

        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.setBackground(new Color(20, 30, 25));
        panel.setBorder(new EmptyBorder(10, 10, 10, 20));
        panel.setPreferredSize(new Dimension(150, 70));

        speedLabel = new JLabel("Speed: 0 km/h", SwingConstants.RIGHT);
        speedLabel.setFont(new Font("Arial", Font.BOLD, 14));
        speedLabel.setForeground(new Color(255, 200, 0));

        JLabel turnLabel = new JLabel("Turn: Player 1", SwingConstants.RIGHT);
        turnLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        turnLabel.setForeground(Color.WHITE);

        panel.add(speedLabel);
        panel.add(turnLabel);

        // Update turn label periodically
        new Timer(100, e -> {
            if (ballInPlay) {
                turnLabel.setText("Turn: " + (isPlayer1Turn ? currentPlayer1 : currentPlayer2));
            } else {
                turnLabel.setText("Ready to Serve");
            }
        }).start();

        return panel;
    }

    private JButton createWimbledonButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                new EmptyBorder(8, 15, 8, 15)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.brighter());
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 215, 0), 2),
                        new EmptyBorder(8, 15, 8, 15)
                ));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.WHITE, 1),
                        new EmptyBorder(8, 15, 8, 15)
                ));
            }
        });

        return button;
    }

    private void setupKeyboardControls() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        switch(e.getKeyCode()) {
                            case KeyEvent.VK_SPACE:
                                if (!ballInPlay) serve();
                                return true;
                            case KeyEvent.VK_H:
                                playerHit();
                                return true;
                            case KeyEvent.VK_N:
                                startNewMatch();
                                return true;
                            case KeyEvent.VK_M:
                                toggleMode();
                                return true;
                        }
                    }
                    return false;
                });
    }

    private void toggleMode() {
        isTwoPlayerMode = !isTwoPlayerMode;
        modeButton.setText(isTwoPlayerMode ? "👥 TWO PLAYER" : "🤖 VS COMPUTER");
        startNewMatch();
    }

    private void startNewMatch() {
        player1Points = 0;
        player2Points = 0;
        player1Games = 0;
        player2Games = 0;
        player1Sets = 0;
        player2Sets = 0;
        isPlayer1Turn = true;
        ballInPlay = false;
        isTiebreak = false;

        // Randomize players
        currentPlayer1 = wimbledonPlayers[random.nextInt(wimbledonPlayers.length)];
        do {
            currentPlayer2 = wimbledonPlayers[random.nextInt(wimbledonPlayers.length)];
        } while (currentPlayer2.equals(currentPlayer1));

        player1NameLabel.setText(currentPlayer1);
        player2NameLabel.setText(currentPlayer2);

        updateScoreDisplay();
        gamePanel.resetGame();
        gamePanel.setMatchActive(true);
        gamePanel.showMatchIntro();
        repaint();
        serveButton.setEnabled(true);
        hitButton.setEnabled(true);

        if (!isTwoPlayerMode && !isPlayer1Turn) {
            computerServe();
        }
    }

    private void serve() {
        if (ballInPlay) return;

        ballInPlay = true;
        gamePanel.startServe(isPlayer1Turn);
        animationTimer.start();
        serveButton.setEnabled(false);
        gamePanel.playServeAnimation();

        if (!isTwoPlayerMode && !isPlayer1Turn) {
            Timer timer = new Timer(500, e -> computerHit());
            timer.setRepeats(false);
            timer.start();
        }
    }

    private void computerServe() {
        Timer timer = new Timer(800, e -> serve());
        timer.setRepeats(false);
        timer.start();
    }

    private void computerHit() {
        if (!ballInPlay || isPlayer1Turn) return;

        int skill = 80 + random.nextInt(15);
        if (random.nextInt(100) < skill) {
            gamePanel.computerHit();
            isPlayer1Turn = true;
            gamePanel.showHitFlash(true);
        } else {
            gamePanel.showMissEffect();
            handlePointEnd();
        }
        repaint();
    }

    private void playerHit() {
        if (!ballInPlay) return;
        if (isTwoPlayerMode || isPlayer1Turn) {
            gamePanel.playerHit();
            isPlayer1Turn = !isPlayer1Turn;
            gamePanel.showHitFlash(false);
            repaint();

            if (!isTwoPlayerMode && !isPlayer1Turn) {
                Timer timer = new Timer(400, e -> computerHit());
                timer.setRepeats(false);
                timer.start();
            }
        }
    }

    private void handlePointEnd() {
        ballInPlay = false;
        animationTimer.stop();

        if (gamePanel.isBallOut()) {
            if (isPlayer1Turn) {
                player2Points++;
                showPointMessage(currentPlayer2 + " wins point!", new Color(255, 100, 100));
            } else {
                player1Points++;
                showPointMessage(currentPlayer1 + " wins point!", new Color(100, 255, 100));
            }
        }

        updateScoreDisplay();
        checkGameWin();
        checkSetWin();
        checkMatchWin();

        if (matchActive()) {
            gamePanel.resetBallPosition();
            serveButton.setEnabled(true);

            if (!isTwoPlayerMode && !isPlayer1Turn) {
                computerServe();
            }
        }
    }

    private void checkGameWin() {
        if (!isTiebreak) {
            if (player1Points >= 4 && player1Points - player2Points >= 2) {
                player1Games++;
                showGameMessage(currentPlayer1 + " wins game!");
                player1Points = 0;
                player2Points = 0;
                updateScoreDisplay();
            } else if (player2Points >= 4 && player2Points - player1Points >= 2) {
                player2Games++;
                showGameMessage(currentPlayer2 + " wins game!");
                player1Points = 0;
                player2Points = 0;
                updateScoreDisplay();
            }
        } else {
            if (player1Points >= 7 && player1Points - player2Points >= 2) {
                player1Games++;
                showGameMessage(currentPlayer1 + " wins tiebreak!");
                player1Points = 0;
                player2Points = 0;
                isTiebreak = false;
                updateScoreDisplay();
            } else if (player2Points >= 7 && player2Points - player1Points >= 2) {
                player2Games++;
                showGameMessage(currentPlayer2 + " wins tiebreak!");
                player1Points = 0;
                player2Points = 0;
                isTiebreak = false;
                updateScoreDisplay();
            }
        }
    }

    private void checkSetWin() {
        if (player1Games >= 6 && player1Games - player2Games >= 2) {
            player1Sets++;
            showSetMessage(currentPlayer1 + " wins set!");
            player1Games = 0;
            player2Games = 0;
            updateScoreDisplay();
        } else if (player2Games >= 6 && player2Games - player1Games >= 2) {
            player2Sets++;
            showSetMessage(currentPlayer2 + " wins set!");
            player1Games = 0;
            player2Games = 0;
            updateScoreDisplay();
        } else if (player1Games == 6 && player2Games == 6) {
            isTiebreak = true;
            player1Points = 0;
            player2Points = 0;
            showGameMessage("TIEBREAK - First to 7!");
            updateScoreDisplay();
        }
    }

    private void checkMatchWin() {
        if (player1Sets >= 3) {
            showMatchWinner(currentPlayer1);
            gamePanel.setMatchActive(false);
            serveButton.setEnabled(false);
            hitButton.setEnabled(false);
        } else if (player2Sets >= 3) {
            showMatchWinner(currentPlayer2);
            gamePanel.setMatchActive(false);
            serveButton.setEnabled(false);
            hitButton.setEnabled(false);
        }
    }

    private boolean matchActive() {
        return player1Sets < 3 && player2Sets < 3;
    }

    private void showPointMessage(String message, Color color) {
        JLabel label = new JLabel(message);
        label.setFont(new Font("Arial", Font.BOLD, 18));
        label.setForeground(color);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        JDialog dialog = new JDialog(this, "Point", false);
        dialog.setLayout(new BorderLayout());
        dialog.add(label, BorderLayout.CENTER);
        dialog.setSize(350, 70);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        dialog.getContentPane().setBackground(new Color(0, 0, 0, 200));

        Timer timer = new Timer(1200, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();
        dialog.setVisible(true);
    }

    private void showGameMessage(String message) {
        JLabel label = new JLabel("🎾 " + message + " 🎾");
        label.setFont(new Font("Arial", Font.BOLD, 22));
        label.setForeground(new Color(255, 215, 0));
        label.setHorizontalAlignment(SwingConstants.CENTER);

        JDialog dialog = new JDialog(this, "Game", false);
        dialog.setLayout(new BorderLayout());
        dialog.add(label, BorderLayout.CENTER);
        dialog.setSize(450, 90);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);

        Timer timer = new Timer(1800, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();
        dialog.setVisible(true);
    }

    private void showSetMessage(String message) {
        JLabel label = new JLabel("🏆 " + message + " 🏆");
        label.setFont(new Font("Arial", Font.BOLD, 26));
        label.setForeground(new Color(255, 100, 0));
        label.setHorizontalAlignment(SwingConstants.CENTER);

        JDialog dialog = new JDialog(this, "Set", false);
        dialog.setLayout(new BorderLayout());
        dialog.add(label, BorderLayout.CENTER);
        dialog.setSize(550, 110);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);

        Timer timer = new Timer(2200, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();
        dialog.setVisible(true);
    }

    private void showMatchWinner(String winner) {
        JLabel label = new JLabel("🏆🏆🏆 " + winner + " WINS WIMBLEDON! 🏆🏆🏆");
        label.setFont(new Font("Times New Roman", Font.BOLD, 32));
        label.setForeground(new Color(255, 215, 0));
        label.setHorizontalAlignment(SwingConstants.CENTER);

        JDialog dialog = new JDialog(this, "CHAMPION", false);
        dialog.setLayout(new BorderLayout());
        dialog.add(label, BorderLayout.CENTER);
        dialog.setSize(750, 130);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        dialog.getContentPane().setBackground(new Color(0, 80, 0));

        Timer timer = new Timer(3500, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();
        dialog.setVisible(true);
        gamePanel.showFireworks();
    }

    private void updateScoreDisplay() {
        if (!isTiebreak) {
            player1ScoreLabel.setText(getTennisScore(player1Points));
            player2ScoreLabel.setText(getTennisScore(player2Points));
        } else {
            player1ScoreLabel.setText(String.valueOf(player1Points));
            player2ScoreLabel.setText(String.valueOf(player2Points));
        }
        setScoreLabel.setText(String.format("%d - %d", player1Games, player2Games));
    }

    private void updateSpeedDisplay() {
        double speed = gamePanel.getCurrentSpeed();
        speedLabel.setText(String.format("Speed: %.0f km/h", speed));
    }

    private String getTennisScore(int points) {
        switch(points) {
            case 0: return "0";
            case 1: return "15";
            case 2: return "30";
            case 3: return "40";
            default: return "AD";
        }
    }

    // Main Game Panel - Vertical View
    class GamePanel extends JPanel {
        // Court dimensions (vertical orientation)
        private final int COURT_TOP = 80;
        private final int COURT_BOTTOM = 820;
        private final int COURT_LEFT = 100;
        private final int COURT_RIGHT = 700;
        private final int NET_Y = 450;

        // Ball physics
        private double ballX = 400, ballY = 450;
        private double ballVx = 0, ballVy = 0;
        private double targetY = 450;
        private boolean isMoving = false;
        private boolean ballOut = false;
        private boolean matchActive = true;

        // Animation effects
        private List<Particle> particles = new ArrayList<>();
        private List<CrowdMember> crowd = new ArrayList<>();
        private float hitFlash = 0;
        private float serveFlash = 0;
        private List<Firework> fireworks = new ArrayList<>();
        private double player1Y = 450, player2Y = 450;

        public GamePanel() {
            setBackground(new Color(15, 70, 30));
            initializeCrowd();

            // Player movement animation
            Timer playerTimer = new Timer(16, e -> {
                if (!isMoving) {
                    // Idle animation
                    player1Y = 450 + Math.sin(System.currentTimeMillis() * 0.005) * 3;
                    player2Y = 450 + Math.cos(System.currentTimeMillis() * 0.005) * 3;
                }
                repaint();
            });
            playerTimer.start();

            // Particle updater
            Timer particleTimer = new Timer(16, e -> {
                particles.removeIf(p -> p.life <= 0);
                particles.forEach(p -> p.update());
                fireworks.removeIf(f -> f.life <= 0);
                fireworks.forEach(f -> f.update());
                if (hitFlash > 0) hitFlash -= 0.05;
                if (serveFlash > 0) serveFlash -= 0.05;
                repaint();
            });
            particleTimer.start();
        }

        private void initializeCrowd() {
            for (int i = 0; i < 80; i++) {
                int x = 20 + random.nextInt(760);
                int y = 10 + random.nextInt(60);
                crowd.add(new CrowdMember(x, y));
            }
        }

        public void resetGame() {
            ballX = 400;
            ballY = 450;
            ballVx = 0;
            ballVy = 0;
            isMoving = false;
            ballOut = false;
            repaint();
        }

        public void resetBallPosition() {
            ballX = 400;
            ballY = 450;
            isMoving = false;
        }

        public void startServe(boolean fromTop) {
            if (fromTop) {
                // Player 1 serves from top
                ballX = 400;
                ballY = 120;
                targetY = 700 + random.nextDouble() * 100;
            } else {
                // Player 2 serves from bottom
                ballX = 400;
                ballY = 780;
                targetY = 150 + random.nextDouble() * 100;
            }

            double dy = targetY - ballY;
            double speed = 7;
            ballVy = (dy / Math.abs(dy)) * speed;
            ballVx = (random.nextDouble() - 0.5) * 2;
            isMoving = true;
            ballOut = false;
            serveFlash = 1.0f;

            // Serve particles
            for (int i = 0; i < 20; i++) {
                particles.add(new Particle(ballX, ballY));
            }
        }

        public void playerHit() {
            if (!isMoving || !matchActive) return;

            // Calculate hit position
            double hitY = (ballY < NET_Y) ? 700 + random.nextDouble() * 100 : 100 + random.nextDouble() * 100;
            double dy = hitY - ballY;
            double speed = 10 + random.nextDouble() * 5;
            ballVy = (dy / Math.abs(dy)) * speed;
            ballVx += (random.nextDouble() - 0.5) * 3;

            // Limit angle
            ballVx = Math.min(Math.max(ballVx, -4), 4);

            // Hit particles
            for (int i = 0; i < 15; i++) {
                particles.add(new Particle(ballX, ballY));
            }

            currentBallSpeed = Math.abs(ballVy) * 15;
        }

        public void computerHit() {
            if (!isMoving || !matchActive) return;

            double hitY;
            if (ballY < NET_Y) {
                hitY = 700 + random.nextDouble() * 80;
            } else {
                hitY = 100 + random.nextDouble() * 80;
            }

            double dy = hitY - ballY;
            double speed = 9 + random.nextDouble() * 6;
            ballVy = (dy / Math.abs(dy)) * speed;
            ballVx += (random.nextDouble() - 0.5) * 2;

            for (int i = 0; i < 12; i++) {
                particles.add(new Particle(ballX, ballY));
            }

            currentBallSpeed = Math.abs(ballVy) * 15;
        }

        public void updateBall() {
            if (!isMoving || !matchActive) return;

            ballX += ballVx;
            ballY += ballVy;

            // Air resistance
            ballVx *= 0.998;
            ballVy *= 0.998;

            // Court boundaries
            if (ballX <= COURT_LEFT || ballX >= COURT_RIGHT) {
                ballOut = true;
                isMoving = false;
            }

            // Net collision
            if ((ballY > NET_Y - 10 && ballY < NET_Y + 10 && ballVy > 0) ||
                    (ballY < NET_Y + 10 && ballY > NET_Y - 10 && ballVy < 0)) {
                if (Math.abs(ballX - 400) > 50) {
                    ballOut = true;
                    isMoving = false;
                }
            }

            // Out of bounds
            if (ballY <= COURT_TOP || ballY >= COURT_BOTTOM) {
                ballOut = true;
                isMoving = false;
            }

            // Check if reached target area
            if (Math.abs(ballY - targetY) < 30) {
                isMoving = false;
            }
        }

        public double getCurrentSpeed() {
            return Math.abs(ballVy) * 18;
        }

        public boolean isBallOut() { return ballOut; }
        public void setMatchActive(boolean active) { matchActive = active; }

        public void showHitFlash(boolean isComputer) {
            hitFlash = 0.8f;
        }

        public void showMissEffect() {
            for (int i = 0; i < 30; i++) {
                particles.add(new Particle(ballX, ballY));
            }
        }

        public void playServeAnimation() {
            serveFlash = 1.0f;
        }

        public void showMatchIntro() {
            for (int i = 0; i < 50; i++) {
                particles.add(new Particle(400 + random.nextInt(100) - 50, 450 + random.nextInt(100) - 50));
            }
        }

        public void showFireworks() {
            for (int i = 0; i < 30; i++) {
                fireworks.add(new Firework(400, 450));
            }
        }

        public void cheerCrowd() {
            for (CrowdMember c : crowd) {
                if (random.nextInt(4) == 0) {
                    c.cheer();
                }
            }
        }

        private void drawWimbledonCourt(Graphics2D g2d) {
            // Grass court
            GradientPaint grass = new GradientPaint(0, 0, new Color(20, 80, 30),
                    0, getHeight(), new Color(10, 50, 20));
            g2d.setPaint(grass);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Court lines
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(4));
            g2d.drawRect(COURT_LEFT, COURT_TOP, COURT_RIGHT - COURT_LEFT, COURT_BOTTOM - COURT_TOP);

            // Net
            g2d.setColor(new Color(200, 200, 200));
            g2d.setStroke(new BasicStroke(6));
            g2d.drawLine(COURT_LEFT, NET_Y, COURT_RIGHT, NET_Y);

            // Net posts
            g2d.fillRect(COURT_LEFT - 5, NET_Y - 15, 10, 30);
            g2d.fillRect(COURT_RIGHT - 5, NET_Y - 15, 10, 30);

            // Service lines
            g2d.setStroke(new BasicStroke(3));
            int serviceY = (COURT_TOP + NET_Y) / 2;
            g2d.drawLine(COURT_LEFT, serviceY, COURT_RIGHT, serviceY);
            serviceY = (NET_Y + COURT_BOTTOM) / 2;
            g2d.drawLine(COURT_LEFT, serviceY, COURT_RIGHT, serviceY);

            // Center service line
            g2d.drawLine(400, COURT_TOP, 400, NET_Y);
            g2d.drawLine(400, NET_Y, 400, COURT_BOTTOM);

            // Wimbledon text on court
            g2d.setFont(new Font("Times New Roman", Font.BOLD, 24));
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.drawString("WIMBLEDON", 320, 440);
        }

        private void drawPlayers(Graphics2D g2d) {
            // Player 1 (Top)
            int p1X = 370;
            int p1Y = (int)player1Y - 20;

            // Body
            g2d.setColor(new Color(0, 150, 200));
            g2d.fillOval(p1X, p1Y, 60, 60);
            g2d.setColor(new Color(0, 100, 150));
            g2d.fillOval(p1X + 10, p1Y + 10, 40, 40);

            // Head
            g2d.setColor(new Color(255, 200, 150));
            g2d.fillOval(p1X + 20, p1Y - 15, 20, 20);

            // Racket
            if (ballInPlay && !isPlayer1Turn) {
                g2d.setColor(new Color(180, 100, 50));
                g2d.fillRoundRect(p1X + 50, p1Y + 20, 30, 8, 5, 5);
                g2d.fillRoundRect(p1X + 55, p1Y + 15, 20, 12, 3, 3);
            }

            // Player 2 (Bottom)
            int p2X = 370;
            int p2Y = (int)player2Y + 20;

            g2d.setColor(new Color(200, 50, 100));
            g2d.fillOval(p2X, p2Y - 40, 60, 60);
            g2d.setColor(new Color(150, 30, 70));
            g2d.fillOval(p2X + 10, p2Y - 30, 40, 40);

            // Head
            g2d.setColor(new Color(255, 200, 150));
            g2d.fillOval(p2X + 20, p2Y - 55, 20, 20);

            // Racket
            if (ballInPlay && isPlayer1Turn) {
                g2d.setColor(new Color(180, 100, 50));
                g2d.fillRoundRect(p2X - 20, p2Y - 30, 30, 8, 5, 5);
                g2d.fillRoundRect(p2X - 15, p2Y - 35, 20, 12, 3, 3);
            }
        }

        private void drawBall(Graphics2D g2d) {
            // Ball glow
            if (isMoving) {
                RadialGradientPaint glow = new RadialGradientPaint(
                        (float)ballX, (float)ballY, 20,
                        new float[]{0f, 1f},
                        new Color[]{new Color(255, 255, 200, 100), new Color(255, 255, 200, 0)}
                );
                g2d.setPaint(glow);
                g2d.fillOval((int)ballX - 20, (int)ballY - 20, 40, 40);
            }

            // Ball
            RadialGradientPaint ballGrad = new RadialGradientPaint(
                    (float)ballX, (float)ballY, 12,
                    new float[]{0f, 1f},
                    new Color[]{new Color(240, 250, 120), new Color(160, 180, 50)}
            );
            g2d.setPaint(ballGrad);
            g2d.fillOval((int)ballX - 12, (int)ballY - 12, 24, 24);

            // Seams
            g2d.setColor(new Color(200, 150, 50));
            g2d.drawLine((int)ballX - 8, (int)ballY, (int)ballX + 8, (int)ballY);
            g2d.drawLine((int)ballX, (int)ballY - 8, (int)ballX, (int)ballY + 8);
        }

        private void drawEffects(Graphics2D g2d) {
            // Hit flash
            if (hitFlash > 0) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hitFlash));
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }

            // Serve flash
            if (serveFlash > 0) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, serveFlash * 0.5f));
                g2d.setColor(new Color(255, 255, 100));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }

            // Particles
            for (Particle p : particles) {
                g2d.setColor(new Color(p.r, p.g, p.b, (int)(p.life * 255)));
                g2d.fillOval((int)p.x - 3, (int)p.y - 3, 6, 6);
            }

            // Fireworks
            for (Firework f : fireworks) {
                g2d.setColor(new Color(f.r, f.g, f.b, (int)(f.life * 255)));
                g2d.fillOval((int)f.x - 4, (int)f.y - 4, 8, 8);
            }
        }

        private void drawCrowd(Graphics2D g2d) {
            for (CrowdMember c : crowd) {
                g2d.setColor(c.cheering ? new Color(200, 100, 50) : new Color(70, 70, 100));
                g2d.fillRect(c.x, c.y, 8, 12);
                if (c.cheering) {
                    g2d.setColor(Color.YELLOW);
                    g2d.fillOval(c.x + 1, c.y - 4, 6, 6);
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON));

            drawWimbledonCourt(g2d);
            drawCrowd(g2d);
            drawPlayers(g2d);
            drawBall(g2d);
            drawEffects(g2d);
        }

        // Effect classes
        class Particle {
            double x, y, vx, vy;
            float life = 1.0f;
            int r, g, b;

            Particle(double x, double y) {
                this.x = x;
                this.y = y;
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = random.nextDouble() * 4 + 1;
                this.vx = Math.cos(angle) * speed;
                this.vy = Math.sin(angle) * speed;
                this.r = 255;
                this.g = 200 + random.nextInt(55);
                this.b = 50;
            }

            void update() {
                x += vx;
                y += vy;
                life -= 0.02;
                vy += 0.1;
            }
        }

        class Firework {
            double x, y, vx, vy;
            float life = 1.0f;
            int r, g, b;

            Firework(double x, double y) {
                this.x = x;
                this.y = y;
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = random.nextDouble() * 8 + 2;
                this.vx = Math.cos(angle) * speed;
                this.vy = Math.sin(angle) * speed;
                this.r = 255;
                this.g = random.nextInt(255);
                this.b = random.nextInt(255);
            }

            void update() {
                x += vx;
                y += vy;
                life -= 0.02;
                vy += 0.2;
            }
        }

        class CrowdMember {
            int x, y;
            boolean cheering = false;

            CrowdMember(int x, int y) {
                this.x = x;
                this.y = y;
            }

            void cheer() {
                cheering = true;
                Timer timer = new Timer(1500, e -> cheering = false);
                timer.setRepeats(false);
                timer.start();
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}

        SwingUtilities.invokeLater(() -> new WimbledonVerticalTennis());
    }
}