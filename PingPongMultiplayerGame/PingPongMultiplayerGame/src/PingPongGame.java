import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Random;
import javax.sound.sampled.*;
import java.util.concurrent.atomic.AtomicBoolean;

// Main game class extending JFrame for the game window
public class PingPongGame extends JFrame {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int PADDLE_WIDTH = 15;
    private static final int PADDLE_HEIGHT = 90;
    private static final int BALL_SIZE = 20;
    private static final int PADDLE_SPEED = 5;
    private static final int BALL_SPEED = 7;
    private static final int MAX_SCORE = 5;

    // Game components
    private GamePanel gamePanel;
    private Timer timer;
    private int paddle1Y = WINDOW_HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private int paddle2Y = WINDOW_HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private int ballX = WINDOW_WIDTH / 2 - BALL_SIZE / 2;
    private int ballY = WINDOW_HEIGHT / 2 - BALL_SIZE / 2;
    private int ballDX = BALL_SPEED;
    private int ballDY = BALL_SPEED;
    private int score1 = 0;
    private int score2 = 0;
    private boolean isServer = false;
    private boolean isClient = false;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private AtomicBoolean running = new AtomicBoolean(true);
    private Clip hitSound;
    private Clip scoreSound;

    // Constructor
    public PingPongGame(boolean isServer, String host, int port) {
        this.isServer = isServer;
        this.isClient = !isServer;
        setTitle("Multiplayer Ping-Pong Game");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        // Initialize game panel
        gamePanel = new GamePanel();
        add(gamePanel);

        // Initialize sound effects
        initializeSounds();

        // Set up keyboard controls
        setupKeyControls();

        // Initialize network
        initializeNetwork(host, port);

        // Start game loop
        timer = new Timer(16, e -> gameLoop());
        timer.start();
    }

    // Initialize sound effects
    private void initializeSounds() {
        try {
            AudioInputStream hitStream = AudioSystem.getAudioInputStream(
                    getClass().getResourceAsStream("/sounds/hit.wav"));
            hitSound = AudioSystem.getClip();
            hitSound.open(hitStream);

            AudioInputStream scoreStream = AudioSystem.getAudioInputStream(
                    getClass().getResourceAsStream("/sounds/score.wav"));
            scoreSound = AudioSystem.getClip();
            scoreSound.open(scoreStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Set up keyboard controls for paddle movement
    private void setupKeyControls() {
        InputMap inputMap = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = gamePanel.getActionMap();

        // Player 1 controls (W/S)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "moveUp1");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, true), "stopUp1");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "moveDown1");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, true), "stopDown1");

        // Player 2 controls (Up/Down arrows)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "moveUp2");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), "stopUp2");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "moveDown2");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "stopDown2");

        // Actions for Player 1
        actionMap.put("moveUp1", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isServer) paddle1Y -= PADDLE_SPEED;
            }
        });
        actionMap.put("stopUp1", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // No action needed for stopping
            }
        });
        actionMap.put("moveDown1", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isServer) paddle1Y += PADDLE_SPEED;
            }
        });
        actionMap.put("stopDown1", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // No action needed for stopping
            }
        });

        // Actions for Player 2
        actionMap.put("moveUp2", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isClient) paddle2Y -= PADDLE_SPEED;
            }
        });
        actionMap.put("stopUp2", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // No action needed for stopping
            }
        });
        actionMap.put("moveDown2", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isClient) paddle2Y += PADDLE_SPEED;
            }
        });
        actionMap.put("stopDown2", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // No action needed for stopping
            }
        });
    }

    // Initialize network connection
    private void initializeNetwork(String host, int port) {
        try {
            if (isServer) {
                serverSocket = new ServerSocket(port);
                System.out.println("Server started on port " + port);
                clientSocket = serverSocket.accept();
                System.out.println("Client connected");
                outputStream = new DataOutputStream(clientSocket.getOutputStream());
                inputStream = new DataInputStream(clientSocket.getInputStream());
                new Thread(this::receiveClientData).start();
            } else {
                clientSocket = new Socket(host, port);
                System.out.println("Connected to server");
                outputStream = new DataOutputStream(clientSocket.getOutputStream());
                inputStream = new DataInputStream(clientSocket.getInputStream());
                new Thread(this::receiveServerData).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Network error: " + e.getMessage());
            System.exit(1);
        }
    }

    // Receive data from client (server-side)
    private void receiveClientData() {
        try {
            while (running.get()) {
                paddle2Y = inputStream.readInt();
                if (isServer) {
                    sendGameState();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Receive data from server (client-side)
    private void receiveServerData() {
        try {
            while (running.get()) {
                paddle1Y = inputStream.readInt();
                ballX = inputStream.readInt();
                ballY = inputStream.readInt();
                ballDX = inputStream.readInt();
                ballDY = inputStream.readInt();
                score1 = inputStream.readInt();
                score2 = inputStream.readInt();
                gamePanel.repaint();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Send game state to client (server-side)
    private void sendGameState() {
        try {
            outputStream.writeInt(paddle1Y);
            outputStream.writeInt(ballX);
            outputStream.writeInt(ballY);
            outputStream.writeInt(ballDX);
            outputStream.writeInt(ballDY);
            outputStream.writeInt(score1);
            outputStream.writeInt(score2);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Send paddle position to server (client-side)
    private void sendPaddlePosition() {
        try {
            outputStream.writeInt(paddle2Y);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Game loop
    private void gameLoop() {
        if (isServer) {
            updateGame();
            sendGameState();
        } else {
            sendPaddlePosition();
        }
        keepPaddlesInBounds();
        gamePanel.repaint();

        // Check for game over
        if (score1 >= MAX_SCORE || score2 >= MAX_SCORE) {
            running.set(false);
            timer.stop();
            String winner = score1 >= MAX_SCORE ? "Player 1" : "Player 2";
            JOptionPane.showMessageDialog(this, winner + " wins!");
            System.exit(0);
        }
    }

    // Update game state (server-side)
    private void updateGame() {
        // Update ball position
        ballX += ballDX;
        ballY += ballDY;

        // Ball collision with top and bottom
        if (ballY <= 0 || ballY >= WINDOW_HEIGHT - BALL_SIZE) {
            ballDY = -ballDY;
            playHitSound();
        }

        // Ball collision with paddles
        Rectangle ballRect = new Rectangle(ballX, ballY, BALL_SIZE, BALL_SIZE);
        Rectangle paddle1Rect = new Rectangle(50, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT);
        Rectangle paddle2Rect = new Rectangle(WINDOW_WIDTH - 50 - PADDLE_WIDTH, paddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT);

        if (ballRect.intersects(paddle1Rect) && ballDX < 0) {
            ballDX = -ballDX;
            adjustBallAngle(paddle1Y);
            playHitSound();
        } else if (ballRect.intersects(paddle2Rect) && ballDX > 0) {
            ballDX = -ballDX;
            adjustBallAngle(paddle2Y);
            playHitSound();
        }

        // Ball out of bounds (scoring)
        if (ballX <= 0) {
            score2++;
            playScoreSound();
            resetBall();
        } else if (ballX >= WINDOW_WIDTH - BALL_SIZE) {
            score1++;
            playScoreSound();
            resetBall();
        }
    }

    // Adjust ball angle based on paddle hit position
    private void adjustBallAngle(int paddleY) {
        int relativeHitPoint = (ballY + BALL_SIZE / 2) - (paddleY + PADDLE_HEIGHT / 2);
        ballDY = relativeHitPoint / (PADDLE_HEIGHT / 8);
        ballDY = Math.max(-BALL_SPEED, Math.min(BALL_SPEED, ballDY));
    }

    // Keep paddles within window bounds
    private void keepPaddlesInBounds() {
        paddle1Y = Math.max(0, Math.min(WINDOW_HEIGHT - PADDLE_HEIGHT, paddle1Y));
        paddle2Y = Math.max(0, Math.min(WINDOW_HEIGHT - PADDLE_HEIGHT, paddle2Y));
    }

    // Reset ball to center
    private void resetBall() {
        ballX = WINDOW_WIDTH / 2 - BALL_SIZE / 2;
        ballY = WINDOW_HEIGHT / 2 - BALL_SIZE / 2;
        Random rand = new Random();
        ballDX = (rand.nextBoolean() ? 1 : -1) * BALL_SPEED;
        ballDY = (rand.nextBoolean() ? 1 : -1) * BALL_SPEED;
    }

    // Play paddle hit sound
    private void playHitSound() {
        if (hitSound != null) {
            hitSound.setFramePosition(0);
            hitSound.start();
        }
    }

    // Play score sound
    private void playScoreSound() {
        if (scoreSound != null) {
            scoreSound.setFramePosition(0);
            scoreSound.start();
        }
    }

    // Game panel for rendering
    private class GamePanel extends JPanel {
        public GamePanel() {
            setBackground(Color.BLACK);
            setFocusable(true);
            requestFocusInWindow();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw center line
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            for (int y = 0; y < WINDOW_HEIGHT; y += 40) {
                g2d.drawLine(WINDOW_WIDTH / 2, y, WINDOW_WIDTH / 2, y + 20);
            }

            // Draw paddles
            g2d.fillRect(50, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT);
            g2d.fillRect(WINDOW_WIDTH - 50 - PADDLE_WIDTH, paddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT);

            // Draw ball
            g2d.fillOval(ballX, ballY, BALL_SIZE, BALL_SIZE);

            // Draw scores
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            g2d.drawString(String.valueOf(score1), WINDOW_WIDTH / 4, 50);
            g2d.drawString(String.valueOf(score2), 3 * WINDOW_WIDTH / 4, 50);

            // Draw instructions
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            g2d.drawString(isServer ? "W/S to move" : "Up/Down to move", 10, WINDOW_HEIGHT - 10);
        }
    }

    // Main method to start the game
    public static void main(String[] args) {
        String[] options = {"Server", "Client"};
        int choice = JOptionPane.showOptionDialog(null, "Run as server or client?", "Ping-Pong Game",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        String host;
        int port = 12345;

        if (choice == 1) {
            host = JOptionPane.showInputDialog("Enter server IP:", "localhost");
        } else {
            host = "localhost";
        }

        SwingUtilities.invokeLater(() -> {
            PingPongGame game = new PingPongGame(choice == 0, host, port);
            game.setVisible(true);
        });
    }
}