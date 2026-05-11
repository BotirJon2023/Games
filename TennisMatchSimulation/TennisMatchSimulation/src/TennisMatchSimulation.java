import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import javax.swing.border.*;

public class TennisMatchSimulation extends JFrame {
    private GamePanel gamePanel;
    private JPanel controlPanel;
    private JLabel scoreLabel, player1ScoreLabel, player2ScoreLabel;
    private JButton serveButton, hitButton, newGameButton, modeButton;
    private Timer animationTimer;
    private boolean isTwoPlayerMode = true;
    private boolean isComputerTurn = false;
    private Random random = new Random();

    private int player1Score = 0;
    private int player2Score = 0;
    private int player1Games = 0;
    private int player2Games = 0;
    private boolean isPlayer1Turn = true;
    private boolean ballInPlay = false;

    public TennisMatchSimulation() {
        setTitle("🎾 Tennis Match Simulation - Grand Slam Edition 🏆");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create game panel
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(1000, 600));

        // Create control panel
        controlPanel = createControlPanel();

        // Add components
        add(gamePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // Setup keyboard controls
        setupKeyboardControls();

        // Animation timer for ball movement
        animationTimer = new Timer(16, e -> {
            if (ballInPlay) {
                gamePanel.updateBall();
                if (gamePanel.isBallOut()) {
                    handlePointEnd();
                }
                repaint();
            }
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        startNewGame();
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 40));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Score labels with gradient background
        player1ScoreLabel = createStyledLabel("Player 1: 0", new Color(0, 200, 100));
        player2ScoreLabel = createStyledLabel("Player 2: 0", new Color(200, 50, 50));
        scoreLabel = createStyledLabel("Game: 0 - 0", new Color(255, 200, 50));

        // Buttons
        serveButton = createStyledButton("🎾 SERVE", new Color(50, 150, 250));
        hitButton = createStyledButton("🏓 HIT", new Color(250, 150, 50));
        newGameButton = createStyledButton("🔄 NEW GAME", new Color(100, 200, 100));
        modeButton = createStyledButton("👥 TWO PLAYER", new Color(150, 100, 200));

        serveButton.addActionListener(e -> serve());
        hitButton.addActionListener(e -> playerHit());
        newGameButton.addActionListener(e -> startNewGame());
        modeButton.addActionListener(e -> toggleGameMode());

        // Add components
        gbc.gridx = 0; gbc.gridy = 0; panel.add(player1ScoreLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0; panel.add(scoreLabel, gbc);
        gbc.gridx = 2; gbc.gridy = 0; panel.add(player2ScoreLabel, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(serveButton, gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(hitButton, gbc);
        gbc.gridx = 2; gbc.gridy = 1; panel.add(newGameButton, gbc);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(modeButton, gbc);

        return panel;
    }

    private JLabel createStyledLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 18));
        label.setForeground(color);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                new EmptyBorder(5, 15, 5, 15)
        ));
        return label;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                new EmptyBorder(10, 20, 10, 20)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.brighter());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
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
                        }
                    }
                    return false;
                });
    }

    private void toggleGameMode() {
        isTwoPlayerMode = !isTwoPlayerMode;
        modeButton.setText(isTwoPlayerMode ? "👥 TWO PLAYER" : "🤖 VS COMPUTER");
        modeButton.setBackground(isTwoPlayerMode ? new Color(150, 100, 200) : new Color(200, 100, 100));
        startNewGame();
    }

    private void startNewGame() {
        player1Score = 0;
        player2Score = 0;
        player1Games = 0;
        player2Games = 0;
        isPlayer1Turn = true;
        ballInPlay = false;
        updateScoreDisplay();
        gamePanel.resetBall();
        gamePanel.setGameActive(true);
        repaint();

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

        if (!isTwoPlayerMode && !isPlayer1Turn) {
            Timer computerServeTimer = new Timer(500, e -> {
                if (ballInPlay && !isPlayer1Turn) {
                    computerHit();
                }
            });
            computerServeTimer.setRepeats(false);
            computerServeTimer.start();
        }
    }

    private void computerServe() {
        Timer timer = new Timer(1000, e -> serve());
        timer.setRepeats(false);
        timer.start();
    }

    private void computerHit() {
        if (!ballInPlay || isPlayer1Turn) return;

        // Computer AI: varying accuracy based on difficulty
        int accuracy = random.nextInt(100);
        double hitX = gamePanel.getBallX();
        double hitY = gamePanel.getBallY();

        if (accuracy > 20) { // 80% success rate
            // Aim towards opponent's side with some variation
            double targetX = 500 + (random.nextDouble() - 0.5) * 200;
            double targetY = 50 + random.nextDouble() * 400;
            gamePanel.hitBall(targetX, targetY);

            // Visual feedback
            gamePanel.showHitEffect(true);
            SwingUtilities.invokeLater(() -> {
                hitButton.setBackground(new Color(250, 150, 50).brighter());
                Timer resetTimer = new Timer(100, e ->
                        hitButton.setBackground(new Color(250, 150, 50)));
                resetTimer.setRepeats(false);
                resetTimer.start();
            });

            isPlayer1Turn = true;
        } else {
            // Computer misses
            handlePointEnd();
        }

        repaint();
    }

    private void playerHit() {
        if (!ballInPlay) return;
        if (isTwoPlayerMode || isPlayer1Turn) {
            gamePanel.showHitEffect(false);
            isPlayer1Turn = !isPlayer1Turn;
            repaint();

            // Flash effect on hit
            hitButton.setBackground(new Color(250, 150, 50).brighter());
            Timer resetTimer = new Timer(100, e ->
                    hitButton.setBackground(new Color(250, 150, 50)));
            resetTimer.setRepeats(false);
            resetTimer.start();

            if (!isTwoPlayerMode && !isPlayer1Turn) {
                Timer computerTimer = new Timer(300, e -> computerHit());
                computerTimer.setRepeats(false);
                computerTimer.start();
            }
        }
    }

    private void handlePointEnd() {
        ballInPlay = false;
        animationTimer.stop();

        if (gamePanel.isBallOut()) {
            if (isPlayer1Turn) {
                player2Score++;
                showPointMessage("Player 2 wins the point!", new Color(200, 50, 50));
            } else {
                player1Score++;
                showPointMessage("Player 1 wins the point!", new Color(0, 200, 100));
            }
        }

        updateScoreDisplay();

        // Check for game win
        if (player1Score >= 4 && player1Score - player2Score >= 2) {
            player1Games++;
            showGameMessage("🏆 Player 1 wins the game! 🏆");
            player1Score = 0;
            player2Score = 0;
            updateScoreDisplay();
        } else if (player2Score >= 4 && player2Score - player1Score >= 2) {
            player2Games++;
            showGameMessage("🏆 Player 2 wins the game! 🏆");
            player1Score = 0;
            player2Score = 0;
            updateScoreDisplay();
        }

        // Check for match win
        if (player1Games >= 6 && player1Games - player2Games >= 2) {
            showGameMessage("🎉🏆 PLAYER 1 WINS THE MATCH! 🏆🎉");
            gamePanel.setGameActive(false);
            serveButton.setEnabled(false);
            hitButton.setEnabled(false);
            return;
        } else if (player2Games >= 6 && player2Games - player1Games >= 2) {
            showGameMessage("🎉🏆 PLAYER 2 WINS THE MATCH! 🏆🎉");
            gamePanel.setGameActive(false);
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
        messageLabel.setFont(new Font("Arial", Font.BOLD, 24));
        messageLabel.setForeground(color);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JDialog dialog = new JDialog(this, "Point", false);
        dialog.setLayout(new BorderLayout());
        dialog.add(messageLabel, BorderLayout.CENTER);
        dialog.setSize(400, 100);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);

        Timer timer = new Timer(1500, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();

        dialog.setVisible(true);
    }

    private void showGameMessage(String message) {
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Arial", Font.BOLD, 28));
        messageLabel.setForeground(new Color(255, 215, 0));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JDialog dialog = new JDialog(this, "Game", false);
        dialog.setLayout(new BorderLayout());
        dialog.add(messageLabel, BorderLayout.CENTER);
        dialog.setSize(500, 120);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);

        Timer timer = new Timer(2000, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();

        dialog.setVisible(true);
    }

    private void updateScoreDisplay() {
        String p1Score = getTennisScore(player1Score);
        String p2Score = getTennisScore(player2Score);
        player1ScoreLabel.setText("Player 1: " + p1Score);
        player2ScoreLabel.setText("Player 2: " + p2Score);
        scoreLabel.setText(String.format("Games: %d - %d", player1Games, player2Games));
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

    // Game Panel class for drawing the tennis court and animations
    class GamePanel extends JPanel {
        private double ballX = 500, ballY = 300;
        private double ballVx = 0, ballVy = 0;
        private double targetX = 500, targetY = 300;
        private boolean isMoving = false;
        private boolean ballOut = false;
        private boolean gameActive = true;
        private float hitEffectAlpha = 0;
        private boolean hitFromRight = false;
        private Random random = new Random();

        public GamePanel() {
            setBackground(new Color(20, 80, 30));
            setBorder(BorderFactory.createLineBorder(Color.WHITE, 3));
        }

        public void resetBall() {
            ballX = 500;
            ballY = 300;
            ballVx = 0;
            ballVy = 0;
            isMoving = false;
            ballOut = false;
            hitEffectAlpha = 0;
            repaint();
        }

        public void startServe(boolean fromLeft) {
            if (fromLeft) {
                ballX = 150;
                ballY = 300;
                targetX = 850;
                targetY = 200 + random.nextDouble() * 200;
            } else {
                ballX = 850;
                ballY = 300;
                targetX = 150;
                targetY = 200 + random.nextDouble() * 200;
            }

            double dx = targetX - ballX;
            double dy = targetY - ballY;
            double distance = Math.sqrt(dx*dx + dy*dy);
            double speed = 8;
            ballVx = (dx / distance) * speed;
            ballVy = (dy / distance) * speed;
            isMoving = true;
            ballOut = false;
        }

        public void hitBall(double tX, double tY) {
            targetX = tX;
            targetY = tY;
            double dx = targetX - ballX;
            double dy = targetY - ballY;
            double distance = Math.sqrt(dx*dx + dy*dy);
            double speed = 10;
            ballVx = (dx / distance) * speed;
            ballVy = (dy / distance) * speed;
            isMoving = true;

            // Add random spin effect
            ballVx += (random.nextDouble() - 0.5) * 1.5;
            ballVy += (random.nextDouble() - 0.5) * 1.5;
        }

        public void updateBall() {
            if (!isMoving || !gameActive) return;

            ballX += ballVx;
            ballY += ballVy;

            // Check boundaries
            if (ballY <= 50 || ballY >= 550) {
                ballOut = true;
                isMoving = false;
            }

            if (ballX <= 50 || ballX >= 950) {
                ballOut = true;
                isMoving = false;
            }

            // Check if ball reached target area
            double dx = Math.abs(ballX - targetX);
            double dy = Math.abs(ballY - targetY);
            if (dx < 20 && dy < 20 && isMoving) {
                isMoving = false;
            }

            repaint();
        }

        public boolean isBallOut() {
            return ballOut;
        }

        public double getBallX() { return ballX; }
        public double getBallY() { return ballY; }
        public void setGameActive(boolean active) { gameActive = active; }

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

        private void drawGradientCourt(Graphics2D g2d) {
            GradientPaint gradient = new GradientPaint(0, 0, new Color(30, 100, 40),
                    0, getHeight(), new Color(15, 60, 20));
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        private void drawCourtLines(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));

            // Outer boundaries
            g2d.drawRect(50, 50, 900, 500);

            // Net
            g2d.setStroke(new BasicStroke(4));
            g2d.drawLine(500, 50, 500, 550);

            // Service lines
            g2d.drawLine(50, 175, 500, 175);
            g2d.drawLine(500, 175, 950, 175);
            g2d.drawLine(50, 425, 500, 425);
            g2d.drawLine(500, 425, 950, 425);

            // Center service lines
            g2d.drawLine(500, 50, 500, 175);
            g2d.drawLine(500, 425, 500, 550);

            // Doubles alleys
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(35, 35, 930, 530);
        }

        private void drawPlayers(Graphics2D g2d) {
            // Player 1 (Left side)
            g2d.setColor(new Color(0, 150, 200));
            g2d.fillOval(80, (int)ballY - 15, 30, 30);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("P1", 88, (int)ballY - 2);

            // Draw player 1 racket
            g2d.setColor(new Color(200, 100, 50));
            g2d.fillOval(105, (int)ballY - 8, 15, 10);

            // Player 2 (Right side)
            g2d.setColor(new Color(200, 50, 100));
            g2d.fillOval(890, (int)ballY - 15, 30, 30);
            g2d.setColor(Color.WHITE);
            g2d.drawString("P2", 898, (int)ballY - 2);

            // Draw player 2 racket
            g2d.setColor(new Color(200, 100, 50));
            g2d.fillOval(880, (int)ballY - 8, 15, 10);
        }

        private void drawBall(Graphics2D g2d) {
            // Ball with gradient and shadow
            g2d.setColor(new Color(100, 200, 50));
            RadialGradientPaint ballGradient = new RadialGradientPaint(
                    (float)ballX, (float)ballY, 12,
                    new float[]{0f, 1f},
                    new Color[]{new Color(220, 240, 100), new Color(180, 200, 50)}
            );
            g2d.setPaint(ballGradient);
            g2d.fillOval((int)ballX - 10, (int)ballY - 10, 20, 20);

            // Ball details
            g2d.setColor(new Color(150, 150, 50));
            g2d.drawLine((int)ballX, (int)ballY - 8, (int)ballX, (int)ballY + 8);
            g2d.drawLine((int)ballX - 8, (int)ballY, (int)ballX + 8, (int)ballY);

            // Motion trail
            if (isMoving) {
                g2d.setColor(new Color(255, 255, 255, 100));
                g2d.fillOval((int)(ballX - ballVx * 2) - 8, (int)(ballY - ballVy * 2) - 8, 16, 16);
            }
        }

        private void drawHitEffect(Graphics2D g2d) {
            if (hitEffectAlpha > 0) {
                int centerX = hitFromRight ? 850 : 150;
                int centerY = (int)ballY;
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hitEffectAlpha));
                g2d.setColor(new Color(255, 255, 100));
                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI * 2 / 8;
                    int x = centerX + (int)(Math.cos(angle) * 30);
                    int y = centerY + (int)(Math.sin(angle) * 30);
                    g2d.fillOval(x - 5, y - 5, 10, 10);
                }
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }
        }

        private void drawScoreboard(Graphics2D g2d) {
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.setColor(new Color(255, 215, 0));
            String scoreText = String.format("%s - %s",
                    getTennisScore(player1Score), getTennisScore(player2Score));
            g2d.drawString(scoreText, 460, 30);

            if (!gameActive) {
                g2d.setFont(new Font("Arial", Font.BOLD, 40));
                g2d.setColor(new Color(255, 215, 0));
                String winner = player1Games > player2Games ? "PLAYER 1 WINS!" : "PLAYER 2 WINS!";
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(winner)) / 2;
                g2d.drawString(winner, x, getHeight() / 2);
            }
        }

        private void drawDecoration(Graphics2D g2d) {
            // Court texture (small dots)
            g2d.setColor(new Color(255, 255, 255, 30));
            for (int i = 0; i < 500; i++) {
                int x = 50 + random.nextInt(900);
                int y = 50 + random.nextInt(500);
                g2d.fillOval(x, y, 2, 2);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawGradientCourt(g2d);
            drawDecoration(g2d);
            drawCourtLines(g2d);
            drawPlayers(g2d);
            drawBall(g2d);
            drawHitEffect(g2d);
            drawScoreboard(g2d);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TennisMatchSimulation();
        });
    }
}