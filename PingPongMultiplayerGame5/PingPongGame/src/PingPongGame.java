import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class PingPongGame extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PADDLE_WIDTH = 15;
    private static final int PADDLE_HEIGHT = 100;
    private static final int BALL_SIZE = 20;
    private static final int PADDLE_SPEED = 8;
    private static final int BALL_SPEED = 5;
    private static final int WINNING_SCORE = 5;

    private GamePanel gamePanel;
    private Timer timer;
    private boolean gameRunning = false;
    private boolean gamePaused = false;

    public PingPongGame() {
        setTitle("Ping Pong Multiplayer");
        setSize(WIDTH, HEIGHT);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        add(gamePanel);

        setupControls();

        timer = new Timer(16, e -> {
            if (gameRunning && !gamePaused) {
                gamePanel.update();
                gamePanel.repaint();
            }
        });
    }

    private void setupControls() {
        InputMap inputMap = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = gamePanel.getActionMap();

        // Player 1 controls (W/S)
        inputMap.put(KeyStroke.getKeyStroke("W"), "p1Up");
        inputMap.put(KeyStroke.getKeyStroke("S"), "p1Down");

        // Player 2 controls (Up/Down arrows)
        inputMap.put(KeyStroke.getKeyStroke("UP"), "p2Up");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "p2Down");

        // Game controls
        inputMap.put(KeyStroke.getKeyStroke("SPACE"), "startPause");
        inputMap.put(KeyStroke.getKeyStroke("R"), "reset");
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "exit");

        actionMap.put("p1Up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameRunning && !gamePaused) {
                    gamePanel.movePaddle1Up();
                }
            }
        });

        actionMap.put("p1Down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameRunning && !gamePaused) {
                    gamePanel.movePaddle1Down();
                }
            }
        });

        actionMap.put("p2Up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameRunning && !gamePaused) {
                    gamePanel.movePaddle2Up();
                }
            }
        });

        actionMap.put("p2Down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameRunning && !gamePaused) {
                    gamePanel.movePaddle2Down();
                }
            }
        });

        actionMap.put("startPause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gameRunning) {
                    startGame();
                } else {
                    togglePause();
                }
            }
        });

        actionMap.put("reset", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetGame();
            }
        });

        actionMap.put("exit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    public void startGame() {
        gameRunning = true;
        gamePaused = false;
        timer.start();
    }

    public void togglePause() {
        gamePaused = !gamePaused;
        gamePanel.repaint();
    }

    public void resetGame() {
        gameRunning = false;
        gamePaused = false;
        timer.stop();
        gamePanel.reset();
        gamePanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PingPongGame game = new PingPongGame();
            game.setVisible(true);
        });
    }

    private class GamePanel extends JPanel {
        private int paddle1Y, paddle2Y;
        private int ballX, ballY;
        private int ballVelX, ballVelY;
        private int player1Score, player2Score;
        private Random random;
        private boolean gameOver;
        private String winner;

        public GamePanel() {
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            random = new Random();
            reset();
        }

        public void reset() {
            paddle1Y = HEIGHT / 2 - PADDLE_HEIGHT / 2;
            paddle2Y = HEIGHT / 2 - PADDLE_HEIGHT / 2;

            ballX = WIDTH / 2 - BALL_SIZE / 2;
            ballY = HEIGHT / 2 - BALL_SIZE / 2;

            // Random initial direction
            ballVelX = BALL_SPEED * (random.nextBoolean() ? 1 : -1);
            ballVelY = BALL_SPEED * (random.nextBoolean() ? 1 : -1);

            player1Score = 0;
            player2Score = 0;
            gameOver = false;
            winner = null;
        }

        public void movePaddle1Up() {
            if (paddle1Y > 0) {
                paddle1Y = Math.max(0, paddle1Y - PADDLE_SPEED);
            }
        }

        public void movePaddle1Down() {
            if (paddle1Y < HEIGHT - PADDLE_HEIGHT) {
                paddle1Y = Math.min(HEIGHT - PADDLE_HEIGHT, paddle1Y + PADDLE_SPEED);
            }
        }

        public void movePaddle2Up() {
            if (paddle2Y > 0) {
                paddle2Y = Math.max(0, paddle2Y - PADDLE_SPEED);
            }
        }

        public void movePaddle2Down() {
            if (paddle2Y < HEIGHT - PADDLE_HEIGHT) {
                paddle2Y = Math.min(HEIGHT - PADDLE_HEIGHT, paddle2Y + PADDLE_SPEED);
            }
        }

        public void update() {
            if (gameOver) return;

            // Update ball position
            ballX += ballVelX;
            ballY += ballVelY;

            // Ball collision with top and bottom walls
            if (ballY <= 0 || ballY >= HEIGHT - BALL_SIZE) {
                ballVelY = -ballVelY;
                playWallHitSound();
            }

            // Ball collision with paddles
            if (ballX <= PADDLE_WIDTH &&
                    ballY + BALL_SIZE >= paddle1Y &&
                    ballY <= paddle1Y + PADDLE_HEIGHT) {
                ballVelX = -ballVelX;
                // Add some randomness to the bounce
                ballVelY += random.nextInt(3) - 1;
                playPaddleHitSound();
            } else if (ballX >= WIDTH - PADDLE_WIDTH - BALL_SIZE &&
                    ballY + BALL_SIZE >= paddle2Y &&
                    ballY <= paddle2Y + PADDLE_HEIGHT) {
                ballVelX = -ballVelX;
                // Add some randomness to the bounce
                ballVelY += random.nextInt(3) - 1;
                playPaddleHitSound();
            }

            // Ball out of bounds (scoring)
            if (ballX < 0) {
                player2Score++;
                playScoreSound();
                resetBall();
                checkGameOver();
            } else if (ballX > WIDTH) {
                player1Score++;
                playScoreSound();
                resetBall();
                checkGameOver();
            }
        }

        private void resetBall() {
            ballX = WIDTH / 2 - BALL_SIZE / 2;
            ballY = HEIGHT / 2 - BALL_SIZE / 2;
            ballVelX = BALL_SPEED * (random.nextBoolean() ? 1 : -1);
            ballVelY = BALL_SPEED * (random.nextBoolean() ? 1 : -1);
        }

        private void checkGameOver() {
            if (player1Score >= WINNING_SCORE) {
                gameOver = true;
                winner = "Player 1";
            } else if (player2Score >= WINNING_SCORE) {
                gameOver = true;
                winner = "Player 2";
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw paddles
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT);
            g2d.fillRect(WIDTH - PADDLE_WIDTH, paddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT);

            // Draw ball
            g2d.fillOval(ballX, ballY, BALL_SIZE, BALL_SIZE);

            // Draw center line
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10}, 0));
            g2d.drawLine(WIDTH / 2, 0, WIDTH / 2, HEIGHT);

            // Draw scores
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.drawString(String.valueOf(player1Score), WIDTH / 4, 50);
            g2d.drawString(String.valueOf(player2Score), 3 * WIDTH / 4, 50);

            // Draw game messages
            if (!gameRunning) {
                drawCenteredString(g2d, "Press SPACE to Start", new Font("Arial", Font.BOLD, 24));
            } else if (gamePaused) {
                drawCenteredString(g2d, "PAUSED - Press SPACE to Resume", new Font("Arial", Font.BOLD, 24));
            } else if (gameOver) {
                drawCenteredString(g2d, winner + " WINS! Press R to Reset", new Font("Arial", Font.BOLD, 36));
            }

            // Draw controls info
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            g2d.drawString("Player 1: W/S", 10, HEIGHT - 30);
            g2d.drawString("Player 2: UP/DOWN", WIDTH - 120, HEIGHT - 30);
            g2d.drawString("SPACE: Start/Pause | R: Reset | ESC: Exit", WIDTH / 2 - 150, HEIGHT - 10);
        }

        private void drawCenteredString(Graphics2D g, String text, Font font) {
            FontMetrics metrics = g.getFontMetrics(font);
            int x = (WIDTH - metrics.stringWidth(text)) / 2;
            int y = (HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();

            g.setFont(font);
            g.drawString(text, x, y);
        }

        private void playPaddleHitSound() {
            // In a real implementation, you would play an actual sound
            Toolkit.getDefaultToolkit().beep();
        }

        private void playWallHitSound() {
            // In a real implementation, you would play an actual sound
            Toolkit.getDefaultToolkit().beep();
        }

        private void playScoreSound() {
            // In a real implementation, you would play an actual sound
            for (int i = 0; i < 3; i++) {
                Toolkit.getDefaultToolkit().beep();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}