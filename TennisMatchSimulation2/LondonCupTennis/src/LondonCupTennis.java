import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;

public class LondonCupTennis extends JFrame {
    private GamePanel gamePanel;
    private JPanel controlPanel, scorePanel;
    private JLabel player1NameLabel, player2NameLabel, player1ScoreLabel, player2ScoreLabel;
    private JLabel setScoreLabel, tournamentLabel;
    private JButton serveButton, hitButton, newMatchButton, modeButton;
    private Timer animationTimer, crowdTimer;
    private Random random = new Random();

    // Game state
    private boolean isTwoPlayerMode = true;
    private boolean isComputerTurn = false;
    private int player1Points = 0;
    private int player2Points = 0;
    private int player1Games = 0;
    private int player2Games = 0;
    private int player1Sets = 0;
    private int player2Sets = 0;
    private boolean isPlayer1Turn = true;
    private boolean ballInPlay = false;
    private boolean isTiebreak = false;
    private String[] playerNames = {"🇬🇧 Andy Murray", "🇪🇸 Carlos Alcaraz", "🇷🇸 Novak Djokovic",
            "🇨🇭 Roger Federer", "🇪🇸 Rafael Nadal", "🇬🇧 Tim Henman"};
    private String currentPlayer1Name, currentPlayer2Name;
    private List<String> crowdMessages = Arrays.asList("COME ON!", "GREAT SHOT!", "WOW!", "ACE! 🎾", "LONDON! 🏆");

    public LondonCupTennis() {
        setTitle("🏆 LONDON CUP CHAMPIONSHIP 🏆 - Wimbledon 2026");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(10, 20, 30));

        // Random player names
        currentPlayer1Name = playerNames[random.nextInt(playerNames.length)];
        do {
            currentPlayer2Name = playerNames[random.nextInt(playerNames.length)];
        } while (currentPlayer2Name.equals(currentPlayer1Name));

        // Create game panel
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(1200, 700));
        gamePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 215, 0), 5),
                BorderFactory.createLineBorder(new Color(0, 100, 0), 3)
        ));

        // Create components
        controlPanel = createControlPanel();
        scorePanel = createScorePanel();

        // Add components
        add(scorePanel, BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // Setup keyboard controls
        setupKeyboardControls();

        // Animation timers
        animationTimer = new Timer(16, e -> {
            if (ballInPlay) {
                gamePanel.updateBall();
                if (gamePanel.isBallOut()) {
                    handlePointEnd();
                }
                repaint();
            }
        });

        // Crowd animation
        crowdTimer = new Timer(3000, e -> {
            if (ballInPlay) {
                gamePanel.showCrowdEffect();
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
        panel.setBackground(new Color(0, 50, 30));
        panel.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Tournament title
        tournamentLabel = new JLabel("🏆 THE LONDON CUP CHAMPIONSHIP 2026 🏆", SwingConstants.CENTER);
        tournamentLabel.setFont(new Font("Arial", Font.BOLD, 20));
        tournamentLabel.setForeground(new Color(255, 215, 0));
        tournamentLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 215, 0), 2),
                new EmptyBorder(5, 20, 5, 20)
        ));

        // Player panels
        JPanel playersPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        playersPanel.setBackground(new Color(0, 50, 30));
        playersPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        // Player 1
        JPanel p1Panel = new JPanel(new GridBagLayout());
        p1Panel.setBackground(new Color(0, 70, 40));
        p1Panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 200, 100), 2),
                new EmptyBorder(10, 20, 10, 20)
        ));
        player1NameLabel = new JLabel(currentPlayer1Name);
        player1NameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        player1NameLabel.setForeground(new Color(0, 255, 150));
        player1ScoreLabel = new JLabel("0");
        player1ScoreLabel.setFont(new Font("Arial", Font.BOLD, 48));
        player1ScoreLabel.setForeground(new Color(255, 255, 255));
        p1Panel.add(player1NameLabel);
        p1Panel.add(player1ScoreLabel);

        // Center score
        JPanel centerPanel = new JPanel(new GridLayout(3, 1));
        centerPanel.setBackground(new Color(0, 60, 35));
        centerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        setScoreLabel = new JLabel("0 - 0", SwingConstants.CENTER);
        setScoreLabel.setFont(new Font("Arial", Font.BOLD, 30));
        setScoreLabel.setForeground(new Color(255, 200, 0));
        JLabel gamesLabel = new JLabel("Games", SwingConstants.CENTER);
        gamesLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gamesLabel.setForeground(Color.WHITE);
        JLabel setsLabel = new JLabel("Best of 3 Sets", SwingConstants.CENTER);
        setsLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        setsLabel.setForeground(new Color(200, 200, 200));
        centerPanel.add(setScoreLabel);
        centerPanel.add(gamesLabel);
        centerPanel.add(setsLabel);

        // Player 2
        JPanel p2Panel = new JPanel(new GridBagLayout());
        p2Panel.setBackground(new Color(0, 70, 40));
        p2Panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 50, 50), 2),
                new EmptyBorder(10, 20, 10, 20)
        ));
        player2NameLabel = new JLabel(currentPlayer2Name);
        player2NameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        player2NameLabel.setForeground(new Color(255, 100, 100));
        player2ScoreLabel = new JLabel("0");
        player2ScoreLabel.setFont(new Font("Arial", Font.BOLD, 48));
        player2ScoreLabel.setForeground(Color.WHITE);
        p2Panel.add(player2NameLabel);
        p2Panel.add(player2ScoreLabel);

        playersPanel.add(p1Panel);
        playersPanel.add(centerPanel);
        playersPanel.add(p2Panel);

        panel.add(tournamentLabel, BorderLayout.NORTH);
        panel.add(playersPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(new Color(20, 30, 40));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);

        // Create styled buttons
        serveButton = createStyledButton("🎾 SERVE", new Color(0, 150, 200), new Color(0, 100, 150));
        hitButton = createStyledButton("🏓 HIT BALL", new Color(200, 100, 50), new Color(150, 70, 30));
        newMatchButton = createStyledButton("🔄 NEW MATCH", new Color(100, 200, 100), new Color(50, 150, 50));
        modeButton = createStyledButton("👥 TWO PLAYER", new Color(150, 50, 200), new Color(100, 30, 150));

        // Action listeners
        serveButton.addActionListener(e -> serve());
        hitButton.addActionListener(e -> playerHit());
        newMatchButton.addActionListener(e -> startNewMatch());
        modeButton.addActionListener(e -> toggleGameMode());

        // Add components
        gbc.gridx = 0; gbc.gridy = 0; panel.add(serveButton, gbc);
        gbc.gridx = 1; gbc.gridy = 0; panel.add(hitButton, gbc);
        gbc.gridx = 2; gbc.gridy = 0; panel.add(newMatchButton, gbc);
        gbc.gridx = 3; gbc.gridy = 0; panel.add(modeButton, gbc);

        // Instructions
        JLabel instructions = new JLabel("Controls: SPACE = Serve | H = Hit | ESC = New Match");
        instructions.setFont(new Font("Arial", Font.PLAIN, 12));
        instructions.setForeground(Color.GRAY);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4;
        panel.add(instructions, gbc);

        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor, Color hoverColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                new EmptyBorder(10, 20, 10, 20)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 215, 0), 2),
                        new EmptyBorder(10, 20, 10, 20)
                ));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.WHITE, 1),
                        new EmptyBorder(10, 20, 10, 20)
                ));
            }
        });

        return button;
    }

    private void setupKeyboardControls() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                            if (!ballInPlay) serve();
                            return true;
                        } else if (e.getKeyCode() == KeyEvent.VK_H) {
                            playerHit();
                            return true;
                        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                            startNewMatch();
                            return true;
                        }
                    }
                    return false;
                });
    }

    private void toggleGameMode() {
        isTwoPlayerMode = !isTwoPlayerMode;
        modeButton.setText(isTwoPlayerMode ? "👥 TWO PLAYER" : "🤖 VS COMPUTER");
        modeButton.setBackground(isTwoPlayerMode ? new Color(150, 50, 200) : new Color(200, 50, 100));
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

        // Randomize player names for new match
        currentPlayer1Name = playerNames[random.nextInt(playerNames.length)];
        do {
            currentPlayer2Name = playerNames[random.nextInt(playerNames.length)];
        } while (currentPlayer2Name.equals(currentPlayer1Name));
        player1NameLabel.setText(currentPlayer1Name);
        player2NameLabel.setText(currentPlayer2Name);

        updateScoreDisplay();
        gamePanel.resetBall();
        gamePanel.setMatchActive(true);
        gamePanel.showMatchStart();
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
        gamePanel.playServeSound();

        if (!isTwoPlayerMode && !isPlayer1Turn) {
            Timer computerServeTimer = new Timer(600, e -> {
                if (ballInPlay && !isPlayer1Turn) {
                    computerHit();
                }
            });
            computerServeTimer.setRepeats(false);
            computerServeTimer.start();
        }
    }

    private void computerServe() {
        Timer timer = new Timer(800, e -> serve());
        timer.setRepeats(false);
        timer.start();
    }

    private void computerHit() {
        if (!ballInPlay || isPlayer1Turn) return;

        int skillLevel = 85; // 85% success rate for London Cup level
        int accuracy = random.nextInt(100);

        if (accuracy < skillLevel) {
            double targetX = 200 + random.nextDouble() * 800;
            double targetY = 100 + random.nextDouble() * 500;
            gamePanel.hitBall(targetX, targetY, true);
            gamePanel.showHitEffect(true);

            // Flash effect
            hitButton.setBackground(new Color(200, 100, 50));
            Timer resetTimer = new Timer(100, e ->
                    hitButton.setBackground(new Color(100, 50, 25)));
            resetTimer.setRepeats(false);
            resetTimer.start();

            isPlayer1Turn = true;
        } else {
            gamePanel.showMissEffect();
            handlePointEnd();
        }

        repaint();
    }

    private void playerHit() {
        if (!ballInPlay) return;
        if (isTwoPlayerMode || isPlayer1Turn) {
            gamePanel.hitBall(0, 0, false);
            gamePanel.showHitEffect(false);
            isPlayer1Turn = !isPlayer1Turn;
            repaint();

            // Flash effect
            hitButton.setBackground(new Color(200, 100, 50));
            Timer resetTimer = new Timer(100, e ->
                    hitButton.setBackground(new Color(100, 50, 25)));
            resetTimer.setRepeats(false);
            resetTimer.start();

            if (!isTwoPlayerMode && !isPlayer1Turn) {
                Timer computerTimer = new Timer(400, e -> computerHit());
                computerTimer.setRepeats(false);
                computerTimer.start();
            }
        }
    }

    private void handlePointEnd() {
        ballInPlay = false;
        animationTimer.stop();

        boolean player1WonPoint = false;

        if (gamePanel.isBallOut()) {
            if (isPlayer1Turn) {
                player2Points++;
                showPointMessage(currentPlayer2Name + " wins the point! 🎾", new Color(255, 100, 100));
                player1WonPoint = false;
            } else {
                player1Points++;
                showPointMessage(currentPlayer1Name + " wins the point! 🎾", new Color(100, 255, 100));
                player1WonPoint = true;
            }
        }

        updateScoreDisplay();

        // Check for game win
        if (!isTiebreak) {
            if (player1Points >= 4 && player1Points - player2Points >= 2) {
                player1Games++;
                showGameMessage(currentPlayer1Name + " wins the game! 🎉");
                player1Points = 0;
                player2Points = 0;
                updateScoreDisplay();
            } else if (player2Points >= 4 && player2Points - player1Points >= 2) {
                player2Games++;
                showGameMessage(currentPlayer2Name + " wins the game! 🎉");
                player1Points = 0;
                player2Points = 0;
                updateScoreDisplay();
            }
        } else {
            // Tiebreak scoring
            if (player1Points >= 7 && player1Points - player2Points >= 2) {
                player1Games++;
                showGameMessage(currentPlayer1Name + " wins the tiebreak! 🏆");
                player1Points = 0;
                player2Points = 0;
                isTiebreak = false;
                updateScoreDisplay();
            } else if (player2Points >= 7 && player2Points - player1Points >= 2) {
                player2Games++;
                showGameMessage(currentPlayer2Name + " wins the tiebreak! 🏆");
                player1Points = 0;
                player2Points = 0;
                isTiebreak = false;
                updateScoreDisplay();
            }
        }

        // Check for set win
        if (player1Games >= 6 && player1Games - player2Games >= 2) {
            player1Sets++;
            showSetMessage(currentPlayer1Name + " wins the set! 🎾🎾");
            player1Games = 0;
            player2Games = 0;
            updateScoreDisplay();
        } else if (player2Games >= 6 && player2Games - player1Games >= 2) {
            player2Sets++;
            showSetMessage(currentPlayer2Name + " wins the set! 🎾🎾");
            player1Games = 0;
            player2Games = 0;
            updateScoreDisplay();
        } else if (player1Games == 6 && player2Games == 6) {
            isTiebreak = true;
            player1Points = 0;
            player2Points = 0;
            showGameMessage("TIEBREAK! First to 7 points! 🎯");
            updateScoreDisplay();
        }

        // Check for match win
        if (player1Sets >= 2) {
            showMatchWinner(currentPlayer1Name);
            gamePanel.setMatchActive(false);
            serveButton.setEnabled(false);
            hitButton.setEnabled(false);
            return;
        } else if (player2Sets >= 2) {
            showMatchWinner(currentPlayer2Name);
            gamePanel.setMatchActive(false);
            serveButton.setEnabled(false);
            hitButton.setEnabled(false);
            return;
        }

        gamePanel.resetBall();
        serveButton.setEnabled(true);

        if (!isTwoPlayerMode && !isPlayer1Turn) {
            computerServe();
        }
    }

    private void showPointMessage(String message, Color color) {
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Arial", Font.BOLD, 20));
        messageLabel.setForeground(color);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JDialog dialog = new JDialog(this, "Point", false);
        dialog.setLayout(new BorderLayout());
        dialog.add(messageLabel, BorderLayout.CENTER);
        dialog.setSize(400, 80);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        dialog.getContentPane().setBackground(new Color(0, 0, 0, 200));

        Timer timer = new Timer(1500, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();

        dialog.setVisible(true);
    }

    private void showGameMessage(String message) {
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Arial", Font.BOLD, 24));
        messageLabel.setForeground(new Color(255, 215, 0));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JDialog dialog = new JDialog(this, "Game", false);
        dialog.setLayout(new BorderLayout());
        dialog.add(messageLabel, BorderLayout.CENTER);
        dialog.setSize(500, 100);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        dialog.getContentPane().setBackground(new Color(0, 0, 0, 200));

        Timer timer = new Timer(2000, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();

        dialog.setVisible(true);
    }

    private void showSetMessage(String message) {
        JLabel messageLabel = new JLabel("🎾 " + message + " 🎾");
        messageLabel.setFont(new Font("Arial", Font.BOLD, 28));
        messageLabel.setForeground(new Color(255, 100, 0));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JDialog dialog = new JDialog(this, "Set", false);
        dialog.setLayout(new BorderLayout());
        dialog.add(messageLabel, BorderLayout.CENTER);
        dialog.setSize(600, 120);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        dialog.getContentPane().setBackground(new Color(0, 0, 0, 200));

        Timer timer = new Timer(2500, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();

        dialog.setVisible(true);
    }

    private void showMatchWinner(String winner) {
        JLabel messageLabel = new JLabel("🏆🏆🏆 " + winner + " WINS THE LONDON CUP! 🏆🏆🏆");
        messageLabel.setFont(new Font("Arial", Font.BOLD, 30));
        messageLabel.setForeground(new Color(255, 215, 0));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JDialog dialog = new JDialog(this, "CHAMPIONSHIP", false);
        dialog.setLayout(new BorderLayout());
        dialog.add(messageLabel, BorderLayout.CENTER);
        dialog.setSize(800, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        dialog.getContentPane().setBackground(new Color(0, 50, 0));

        Timer timer = new Timer(4000, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();

        dialog.setVisible(true);
        gamePanel.showChampionshipEffect();
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

    private String getTennisScore(int points) {
        switch(points) {
            case 0: return "0";
            case 1: return "15";
            case 2: return "30";
            case 3: return "40";
            default: return "AD";
        }
    }

    // Main Game Panel with London Cup Theme
    class GamePanel extends JPanel {
        private double ballX = 600, ballY = 350;
        private double ballVx = 0, ballVy = 0;
        private double targetX = 600, targetY = 350;
        private boolean isMoving = false;
        private boolean ballOut = false;
        private boolean matchActive = true;
        private float hitEffectAlpha = 0;
        private boolean hitFromRight = false;
        private List<Particle> particles = new ArrayList<>();
        private List<CrowdMember> crowd = new ArrayList<>();
        private Random random = new Random();
        private float crownRotation = 0;
        private int championshipStars = 0;

        public GamePanel() {
            setBackground(new Color(15, 60, 25));
            initializeCrowd();

            // Particle animation
            Timer particleTimer = new Timer(16, e -> {
                particles.removeIf(p -> p.life <= 0);
                for (Particle p : particles) {
                    p.update();
                }
                crownRotation += 0.05;
                repaint();
            });
            particleTimer.start();
        }

        private void initializeCrowd() {
            for (int i = 0; i < 50; i++) {
                crowd.add(new CrowdMember(
                        50 + random.nextInt(1100),
                        20 + random.nextInt(30),
                        random.nextBoolean()
                ));
            }
        }

        public void resetBall() {
            ballX = 600;
            ballY = 350;
            ballVx = 0;
            ballVy = 0;
            isMoving = false;
            ballOut = false;
            hitEffectAlpha = 0;
            repaint();
        }

        public void startServe(boolean fromLeft) {
            if (fromLeft) {
                ballX = 180;
                ballY = 350;
                targetX = 1020;
                targetY = 200 + random.nextDouble() * 300;
            } else {
                ballX = 1020;
                ballY = 350;
                targetX = 180;
                targetY = 200 + random.nextDouble() * 300;
            }

            double dx = targetX - ballX;
            double dy = targetY - ballY;
            double distance = Math.sqrt(dx*dx + dy*dy);
            double speed = 9;
            ballVx = (dx / distance) * speed;
            ballVy = (dy / distance) * speed;
            isMoving = true;
            ballOut = false;

            // Add serve particles
            for (int i = 0; i < 15; i++) {
                particles.add(new Particle(ballX, ballY, true));
            }
        }

        public void hitBall(double tX, double tY, boolean isComputer) {
            if (tX == 0 && tY == 0) {
                // Player hit - random placement
                targetX = 300 + random.nextDouble() * 900;
                targetY = 100 + random.nextDouble() * 500;
            } else {
                targetX = tX;
                targetY = tY;
            }

            double dx = targetX - ballX;
            double dy = targetY - ballY;
            double distance = Math.sqrt(dx*dx + dy*dy);
            double speed = 12;
            ballVx = (dx / distance) * speed;
            ballVy = (dy / distance) * speed;
            isMoving = true;

            // Add hit particles
            for (int i = 0; i < 20; i++) {
                particles.add(new Particle(ballX, ballY, false));
            }
        }

        public void updateBall() {
            if (!isMoving || !matchActive) return;

            ballX += ballVx;
            ballY += ballVy;

            // Air resistance
            ballVx *= 0.999;
            ballVy *= 0.999;

            // Check boundaries (court limits)
            if (ballY <= 60 || ballY >= 640) {
                ballOut = true;
                isMoving = false;
            }

            if (ballX <= 60 || ballX >= 1140) {
                ballOut = true;
                isMoving = false;
            }

            // Check if ball reached target area
            double dx = Math.abs(ballX - targetX);
            double dy = Math.abs(ballY - targetY);
            if (dx < 25 && dy < 25 && isMoving) {
                isMoving = false;
            }

            repaint();
        }

        public boolean isBallOut() { return ballOut; }
        public void setMatchActive(boolean active) { matchActive = active; }

        public void showHitEffect(boolean fromRight) {
            hitFromRight = fromRight;
            hitEffectAlpha = 1.0f;
            Timer timer = new Timer(100, e -> {
                hitEffectAlpha -= 0.1f;
                if (hitEffectAlpha <= 0) {
                    hitEffectAlpha = 0;
                    ((Timer)e.getSource()).stop();
                }
                repaint();
            });
            timer.start();
        }

        public void showMissEffect() {
            for (int i = 0; i < 30; i++) {
                particles.add(new Particle(ballX, ballY, true));
            }
        }

        public void showCrowdEffect() {
            for (CrowdMember c : crowd) {
                if (random.nextInt(3) == 0) {
                    c.cheer();
                }
            }
        }

        public void showMatchStart() {
            championshipStars = 50;
            Timer starTimer = new Timer(50, e -> {
                championshipStars--;
                repaint();
                if (championshipStars <= 0) {
                    ((Timer)e.getSource()).stop();
                }
            });
            starTimer.start();
        }

        public void showChampionshipEffect() {
            for (int i = 0; i < 100; i++) {
                particles.add(new Particle(600, 350, true));
            }
        }

        public void playServeSound() {
            // Visual feedback only (sound would require audio files)
            showHitEffect(false);
        }

        private void drawWimbledonCourt(Graphics2D g2d) {
            // Grass court gradient
            GradientPaint grassGradient = new GradientPaint(0, 0, new Color(20, 80, 30),
                    0, getHeight(), new Color(10, 50, 20));
            g2d.setPaint(grassGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Court markings
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(4));
            g2d.drawRect(60, 60, 1080, 580);

            // Net with 3D effect
            g2d.setStroke(new BasicStroke(5));
            g2d.setColor(new Color(200, 200, 200));
            g2d.drawLine(600, 60, 600, 640);

            // Net detail
            g2d.setStroke(new BasicStroke(1));
            for (int i = 60; i <= 640; i += 20) {
                g2d.drawLine(595, i, 605, i);
            }

            // Service lines
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(60, 200, 600, 200);
            g2d.drawLine(600, 200, 1140, 200);
            g2d.drawLine(60, 500, 600, 500);
            g2d.drawLine(600, 500, 1140, 500);

            // Center service lines
            g2d.drawLine(600, 200, 600, 500);

            // London Cup logo at center
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.setColor(new Color(255, 215, 0, 100));
            g2d.drawString("🏆 LONDON CUP 🏆", 520, 350);
        }

        private void drawPlayers(Graphics2D g2d) {
            // Player 1 (Left) - Modern tennis player design
            g2d.setColor(new Color(0, 150, 200));
            g2d.fillOval(100, (int)ballY - 20, 40, 40);
            g2d.setColor(new Color(0, 100, 150));
            g2d.fillOval(105, (int)ballY - 15, 30, 30);

            // Player 1 racket
            g2d.setColor(new Color(180, 100, 50));
            g2d.fillRoundRect(130, (int)ballY - 10, 25, 8, 5, 5);
            g2d.setColor(new Color(150, 80, 30));
            g2d.fillRoundRect(135, (int)ballY - 12, 15, 12, 3, 3);

            // Player 1 name
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.setColor(Color.WHITE);
            g2d.drawString(currentPlayer1Name.substring(0, Math.min(10, currentPlayer1Name.length())),
                    100, (int)ballY - 25);

            // Player 2 (Right)
            g2d.setColor(new Color(200, 50, 100));
            g2d.fillOval(1060, (int)ballY - 20, 40, 40);
            g2d.setColor(new Color(150, 30, 70));
            g2d.fillOval(1065, (int)ballY - 15, 30, 30);

            // Player 2 racket
            g2d.setColor(new Color(180, 100, 50));
            g2d.fillRoundRect(1045, (int)ballY - 10, 25, 8, 5, 5);
            g2d.setColor(new Color(150, 80, 30));
            g2d.fillRoundRect(1050, (int)ballY - 12, 15, 12, 3, 3);

            // Player 2 name
            g2d.setColor(Color.WHITE);
            g2d.drawString(currentPlayer2Name.substring(0, Math.min(10, currentPlayer2Name.length())),
                    1060, (int)ballY - 25);
        }

        private void drawBall(Graphics2D g2d) {
            // 3D Ball effect
            RadialGradientPaint ballGradient = new RadialGradientPaint(
                    (float)ballX, (float)ballY, 15,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(220, 240, 100), new Color(180, 200, 50), new Color(140, 160, 30)}
            );
            g2d.setPaint(ballGradient);
            g2d.fillOval((int)ballX - 12, (int)ballY - 12, 24, 24);

            // Ball seams
            g2d.setColor(new Color(200, 150, 50));
            g2d.drawLine((int)ballX - 8, (int)ballY, (int)ballX + 8, (int)ballY);
            g2d.drawLine((int)ballX, (int)ballY - 8, (int)ballX, (int)ballY + 8);

            // Ball glow when moving
            if (isMoving) {
                g2d.setColor(new Color(255, 255, 200, 80));
                g2d.fillOval((int)ballX - 18, (int)ballY - 18, 36, 36);
            }
        }

        private void drawParticles(Graphics2D g2d) {
            for (Particle p : particles) {
                g2d.setColor(new Color(p.r, p.g, p.b, (int)(p.life * 255)));
                g2d.fillOval((int)p.x - 3, (int)p.y - 3, 6, 6);
            }
        }

        private void drawCrowd(Graphics2D g2d) {
            for (CrowdMember c : crowd) {
                g2d.setColor(c.cheering ? new Color(200, 100, 50) : new Color(100, 100, 150));
                g2d.fillRect(c.x, c.y, 8, 15);
                if (c.cheering) {
                    g2d.setColor(Color.YELLOW);
                    g2d.fillOval(c.x + 1, c.y - 5, 6, 6);
                }
            }
        }

        private void drawHitEffect(Graphics2D g2d) {
            if (hitEffectAlpha > 0) {
                int centerX = hitFromRight ? 1080 : 120;
                int centerY = (int)ballY;
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hitEffectAlpha));
                g2d.setColor(new Color(255, 255, 100));
                for (int i = 0; i < 12; i++) {
                    double angle = i * Math.PI * 2 / 12;
                    int x = centerX + (int)(Math.cos(angle) * 40);
                    int y = centerY + (int)(Math.sin(angle) * 40);
                    g2d.fillOval(x - 5, y - 5, 10, 10);
                }
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }
        }

        private void drawChampionshipStars(Graphics2D g2d) {
            if (championshipStars > 0) {
                for (int i = 0; i < championshipStars; i++) {
                    int x = 100 + random.nextInt(1000);
                    int y = 100 + random.nextInt(500);
                    g2d.setColor(new Color(255, 215, 0, championshipStars * 5));
                    g2d.fillOval(x, y, 8, 8);
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
            drawParticles(g2d);
            drawHitEffect(g2d);
            drawChampionshipStars(g2d);
        }

        // Inner classes for effects
        class Particle {
            double x, y, vx, vy;
            float life = 1.0f;
            int r, g, b;

            Particle(double x, double y, boolean isExplosion) {
                this.x = x;
                this.y = y;
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = random.nextDouble() * 5 + 2;
                this.vx = Math.cos(angle) * speed;
                this.vy = Math.sin(angle) * speed;
                if (isExplosion) {
                    this.vx *= 2;
                    this.vy *= 2;
                }
                this.r = 255;
                this.g = 200 + random.nextInt(55);
                this.b = 50 + random.nextInt(50);
            }

            void update() {
                x += vx;
                y += vy;
                life -= 0.02;
                vy += 0.2; // gravity
            }
        }

        class CrowdMember {
            int x, y;
            boolean cheering;
            int cheerTimer = 0;

            CrowdMember(int x, int y, boolean side) {
                this.x = x;
                this.y = y;
            }

            void cheer() {
                cheering = true;
                cheerTimer = 50;
                Timer timer = new Timer(2000, e -> {
                    cheering = false;
                    ((Timer)e.getSource()).stop();
                });
                timer.setRepeats(false);
                timer.start();
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new LondonCupTennis();
        });
    }
}