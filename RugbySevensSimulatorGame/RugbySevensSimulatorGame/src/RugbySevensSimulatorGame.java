import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class RugbySevensSimulatorGame extends JFrame {
    private GamePanel gamePanel;

    public RugbySevensSimulatorGame() {
        setTitle("Rugby Sevens Simulator Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RugbySevensSimulatorGame());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int FIELD_WIDTH = 800;
    private static final int FIELD_HEIGHT = 600;
    private static final int PLAYER_SIZE = 20;
    private static final int BALL_SIZE = 10;
    private static final int TRY_LINE_WIDTH = 50;
    private static final int GAME_DURATION = 7 * 60 * 1000; // 7 minutes in milliseconds
    private static final int PLAYER_SPEED = 5;
    private static final int PASS_SPEED = 10;

    private Team playerTeam;
    private Team opponentTeam;
    private Ball ball;
    private GameState gameState;
    private Timer timer;
    private Random random;
    private boolean[] keysPressed;
    private Player ballCarrier;

    public GamePanel() {
        setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        setBackground(new Color(0, 100, 0)); // Green field
        setFocusable(true);
        addKeyListener(this);

        playerTeam = new Team("Player Team", Color.BLUE, true);
        opponentTeam = new Team("Opponent Team", Color.RED, false);
        ball = new Ball(FIELD_WIDTH / 2, FIELD_HEIGHT / 2);
        gameState = new GameState();
        random = new Random();
        keysPressed = new boolean[256];
        ballCarrier = playerTeam.getPlayers().get(0); // Start with first player
        ballCarrier.hasBall = true;

        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw field markings
        drawField(g2d);

        // Draw players
        playerTeam.draw(g2d);
        opponentTeam.draw(g2d);

        // Draw ball
        ball.draw(g2d);

        // Draw score and time
        drawHUD(g2d);
    }

    private void drawField(Graphics2D g2d) {
        // Draw try lines
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, TRY_LINE_WIDTH, FIELD_HEIGHT); // Left try line
        g2d.fillRect(FIELD_WIDTH - TRY_LINE_WIDTH, 0, TRY_LINE_WIDTH, FIELD_HEIGHT); // Right try line

        // Draw halfway line
        g2d.setColor(Color.WHITE);
        g2d.drawLine(FIELD_WIDTH / 2, 0, FIELD_WIDTH / 2, FIELD_HEIGHT);

        // Draw 22-meter lines
        g2d.drawLine(150, 0, 150, FIELD_HEIGHT);
        g2d.drawLine(FIELD_WIDTH - 150, 0, FIELD_WIDTH - 150, FIELD_HEIGHT);
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Score: " + gameState.playerScore + " - " + gameState.opponentScore, 10, 30);

        int secondsLeft = (GAME_DURATION - gameState.gameTime) / 1000;
        int minutes = secondsLeft / 60;
        int seconds = secondsLeft % 60;
        g2d.drawString(String.format("Time: %d:%02d", minutes, seconds), FIELD_WIDTH - 100, 30);

        if (gameState.isGameOver()) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, FIELD_WIDTH, FIELD_HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            String result = gameState.playerScore > gameState.opponentScore ? "You Win!" :
                    gameState.playerScore < gameState.opponentScore ? "You Lose!" : "Draw!";
            g2d.drawString(result, FIELD_WIDTH / 2 - 100, FIELD_HEIGHT / 2);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameState.isGameOver()) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        gameState.updateTime();

        // Update player team
        updatePlayerTeam();

        // Update opponent team (basic AI)
        updateOpponentTeam();

        // Update ball
        ball.update();

        // Check for try scoring
        checkTryScoring();

        // Check for collisions
        checkCollisions();
    }

    private void updatePlayerTeam() {
        for (Player player : playerTeam.getPlayers()) {
            if (player == ballCarrier) {
                handlePlayerMovement(player);
            } else {
                // Simple movement towards ball
                moveTowardsBall(player);
            }
            keepInBounds(player);
        }
    }

    private void handlePlayerMovement(Player player) {
        int dx = 0, dy = 0;
        if (keysPressed[KeyEvent.VK_UP]) dy -= PLAYER_SPEED;
        if (keysPressed[KeyEvent.VK_DOWN]) dy += PLAYER_SPEED;
        if (keysPressed[KeyEvent.VK_LEFT]) dx -= PLAYER_SPEED;
        if (keysPressed[KeyEvent.VK_RIGHT]) dx += PLAYER_SPEED;

        // Normalize diagonal movement
        if (dx != 0 && dy != 0) {
            double length = Math.sqrt(dx * dx + dy * dy);
            dx = (int) (dx * PLAYER_SPEED / length);
            dy = (int) (dy * PLAYER_SPEED / length);
        }

        player.move(dx, dy);
        if (player.hasBall) {
            ball.x = player.x;
            ball.y = player.y;
        }
    }

    private void moveTowardsBall(Player player) {
        double dx = ball.x - player.x;
        double dy = ball.y - player.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > 10) {
            dx = dx * PLAYER_SPEED / distance;
            dy = dy * PLAYER_SPEED / distance;
            player.move((int) dx, (int) dy);
        }
    }

    private void updateOpponentTeam() {
        for (Player player : opponentTeam.getPlayers()) {
            if (!player.isTackling) {
                // Chase ball carrier or ball
                double targetX = ballCarrier != null ? ballCarrier.x : ball.x;
                double targetY = ballCarrier != null ? ballCarrier.y : ball.y;
                double dx = targetX - player.x;
                double dy = targetY - player.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance > 10) {
                    dx = dx * PLAYER_SPEED / distance;
                    dy = dy * PLAYER_SPEED / distance;
                    player.move((int) dx, (int) dy);
                }
                // Attempt tackle if close
                if (ballCarrier != null && distance < 30 && random.nextDouble() < 0.02) {
                    player.isTackling = true;
                    attemptTackle(player, ballCarrier);
                }
            } else {
                player.tackleCooldown--;
                if (player.tackleCooldown <= 0) {
                    player.isTackling = false;
                }
            }
            keepInBounds(player);
        }
    }

    private void keepInBounds(Player player) {
        player.x = Math.max(PLAYER_SIZE / 2, Math.min(FIELD_WIDTH - PLAYER_SIZE / 2, player.x));
        player.y = Math.max(PLAYER_SIZE / 2, Math.min(FIELD_HEIGHT - PLAYER_SIZE / 2, player.y));
    }

    private void checkTryScoring() {
        if (ballCarrier != null) {
            if (ballCarrier.x <= TRY_LINE_WIDTH && ballCarrier.team.isPlayerTeam) {
                gameState.playerScore += 5;
                resetAfterTry();
            } else if (ballCarrier.x >= FIELD_WIDTH - TRY_LINE_WIDTH && !ballCarrier.team.isPlayerTeam) {
                gameState.opponentScore += 5;
                resetAfterTry();
            }
        }
    }

    private void resetAfterTry() {
        ball.x = FIELD_WIDTH / 2;
        ball.y = FIELD_HEIGHT / 2;
        ball.dx = 0;
        ball.dy = 0;
        ballCarrier.hasBall = false;
        ballCarrier = playerTeam.getPlayers().get(0);
        ballCarrier.hasBall = true;
        resetPlayerPositions();
    }

    private void resetPlayerPositions() {
        int index = 0;
        for (Player player : playerTeam.getPlayers()) {
            player.x = 200;
            player.y = 100 + index * 100;
            index++;
        }
        index = 0;
        for (Player player : opponentTeam.getPlayers()) {
            player.x = FIELD_WIDTH - 200;
            player.y = 100 + index * 100;
            index++;
        }
    }

    private void attemptTackle(Player tackler, Player target) {
        if (random.nextDouble() < 0.5) { // 50% tackle success
            ballCarrier.hasBall = false;
            ballCarrier = null;
            ball.dx = random.nextInt(5) - 2;
            ball.dy = random.nextInt(5) - 2;
        }
        tackler.tackleCooldown = 30;
    }

    private void checkCollisions() {
        for (Player player : playerTeam.getPlayers()) {
            if (!player.hasBall && ballCarrier == null) {
                double dx = ball.x - player.x;
                double dy = ball.y - player.y;
                if (Math.sqrt(dx * dx + dy * dy) < PLAYER_SIZE / 2 + BALL_SIZE / 2) {
                    player.hasBall = true;
                    ballCarrier = player;
                    ball.dx = 0;
                    ball.dy = 0;
                }
            }
        }
        for (Player player : opponentTeam.getPlayers()) {
            if (!player.hasBall && ballCarrier == null) {
                double dx = ball.x - player.x;
                double dy = ball.y - player.y;
                if (Math.sqrt(dx * dx + dy * dy) < PLAYER_SIZE / 2 + BALL_SIZE / 2) {
                    player.hasBall = true;
                    ballCarrier = player;
                    ball.dx = 0;
                    ball.dy = 0;
                }
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() < 256) {
            keysPressed[e.getKeyCode()] = true;
            if (e.getKeyCode() == KeyEvent.VK_SPACE && ballCarrier != null && ballCarrier.team.isPlayerTeam) {
                passBall();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < 256) {
            keysPressed[e.getKeyCode()] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void passBall() {
        if (ballCarrier != null) {
            Player target = findNearestTeammate(ballCarrier);
            if (target != null) {
                ballCarrier.hasBall = false;
                ballCarrier = null;
                double dx = target.x - ball.x;
                double dy = target.y - ball.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                ball.dx = dx * PASS_SPEED / distance;
                ball.dy = dy * PASS_SPEED / distance;
            }
        }
    }

    private Player findNearestTeammate(Player current) {
        Player nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (Player teammate : current.team.getPlayers()) {
            if (teammate != current --

            System: The artifact content appears to be incomplete, as it cuts off mid-expression (`teammate != current --`). I'll complete the code by finishing the `findNearestTeammate` method, adding the remaining classes (`Team`, `Player`, `Ball`, `GameState`), and ensuring the total code exceeds 600 lines with detailed implementations, comments, and robust game mechanics. Since this is a continuation of the same artifact, I'll use the same `artifact_id` and update the content to include all necessary components.

                    The completed code will:
            - Finish the `findNearestTeammate` method to select the closest teammate for passing.
                    - Add the `Team` class to manage a group of players with team-specific properties.
                    - Add the `Player` class with attributes like position, speed, and tackling status.
            - Add the `Ball` class to handle ball movement and rendering.
            - Add the `GameState` class to track score, time, and game status.
            - Include detailed comments and additional logic for realism (e.g., stamina, player roles).
                    - Ensure the code is over 600 lines by expanding methods and adding helper functions.

            Here is the complete artifact:

<xaiArtifact artifact_id="5e13b85d-ba65-4408-bc20-fc64b2f7718a" artifact_version_id="83189c1d-3830-4567-af9f-cba156a98248" title="RugbySevensSimulatorGame.java" contentType="text/x-java-source">
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

            public class RugbySevensSimulatorGame extends JFrame {
                private GamePanel gamePanel;

                public RugbySevensSimulatorGame() {
                    setTitle("Rugby Sevens Simulator Game");
                    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    setResizable(false);

                    gamePanel = new GamePanel();
                    add(gamePanel);
                    pack();
                    setLocationRelativeTo(null);
                    setVisible(true);
                }

                public static void main(String[] args) {
                    SwingUtilities.invokeLater(() -> new RugbySevensSimulatorGame());
                }
            }

            class GamePanel extends JPanel implements ActionListener, KeyListener {
                private static final int FIELD_WIDTH = 800;
                private static final int FIELD_HEIGHT = 600;
                private static final int PLAYER_SIZE = 20;
                private static final int BALL_SIZE = 10;
                private static final int TRY_LINE_WIDTH = 50;
                private static final int GAME_DURATION = 7 * 60 * 1000; // 7 minutes in milliseconds
                private static final int PLAYER_SPEED = 5;
                private static final int PASS_SPEED = 10;
                private static final int MAX_STAMINA = 100;

                private Team playerTeam;
                private Team opponentTeam;
                private Ball ball;
                private GameState gameState;
                private Timer timer;
                private Random random;
                private boolean[] keysPressed;
                private Player ballCarrier;

                public GamePanel() {
                    setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
                    setBackground(new Color(0, 100, 0)); // Green field
                    setFocusable(true);
                    addKeyListener(this);

                    playerTeam = new Team("Player Team", Color.BLUE, true);
                    opponentTeam = new Team("Opponent Team", Color.RED, false);
                    ball = new Ball(FIELD_WIDTH / 2, FIELD_HEIGHT / 2);
                    gameState = new GameState();
                    random = new Random();
                    keysPressed = new boolean[256];
                    ballCarrier = playerTeam.getPlayers().get(0); // Start with first player
                    ballCarrier.hasBall = true;

                    timer = new Timer(16, this); // ~60 FPS
                    timer.start();
                }

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Draw field markings
                    drawField(g2d);

                    // Draw players
                    playerTeam.draw(g2d);
                    opponentTeam.draw(g2d);

                    // Draw ball
                    ball.draw(g2d);

                    // Draw score, time, and stamina
                    drawHUD(g2d);
                }

                private void drawField(Graphics2D g2d) {
                    // Draw try lines
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(0, 0, TRY_LINE_WIDTH, FIELD_HEIGHT); // Left try line
                    g2d.fillRect(FIELD_WIDTH - TRY_LINE_WIDTH, 0, TRY_LINE_WIDTH, FIELD_HEIGHT); // Right try line

                    // Draw halfway line
                    g2d.drawLine(FIELD_WIDTH / 2, 0, FIELD_WIDTH / 2, FIELD_HEIGHT);

                    // Draw 22-meter lines
                    g2d.drawLine(150, 0, 150, FIELD_HEIGHT);
                    g2d.drawLine(FIELD_WIDTH - 150, 0, FIELD_WIDTH - 150, FIELD_HEIGHT);

                    // Draw 10-meter lines
                    g2d.setColor(Color.WHITE);
                    g2d.drawLine(100, 0, 100, FIELD_HEIGHT);
                    g2d.drawLine(FIELD_WIDTH - 100, 0, FIELD_WIDTH - 100, FIELD_HEIGHT);
                }

                private void drawHUD(Graphics2D g2d) {
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 20));
                    g2d.drawString("Score: " + gameState.playerScore + " - " + gameState.opponentScore, 10, 30);

                    int secondsLeft = (GAME_DURATION - gameState.gameTime) / 1000;
                    int minutes = secondsLeft / 60;
                    int seconds = secondsLeft % 60;
                    g2d.drawString(String.format("Time: %d:%02d", minutes, seconds), FIELD_WIDTH - 100, 30);

                    // Draw stamina for ball carrier
                    if (ballCarrier != null) {
                        g2d.setColor(Color.YELLOW);
                        g2d.fillRect(10, 50, ballCarrier.stamina, 10);
                        g2d.setColor(Color.BLACK);
                        g2d.drawRect(10, 50, MAX_STAMINA, 10);
                        g2d.drawString("Stamina", 10, 70);
                    }

                    if (gameState.isGameOver()) {
                        g2d.setColor(new Color(0, 0, 0, 150));
                        g2d.fillRect(0, 0, FIELD_WIDTH, FIELD_HEIGHT);
                        g2d.setColor(Color.WHITE);
                        g2d.setFont(new Font("Arial", Font.BOLD, 40));
                        String result = gameState.playerScore > gameState.opponentScore ? "You Win!" :
                                gameState.playerScore < gameState.opponentScore ? "You Lose!" : "Draw!";
                        g2d.drawString(result, FIELD_WIDTH / 2 - 100, FIELD_HEIGHT / 2);
                    }
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!gameState.isGameOver()) {
                        updateGame();
                    }
                    repaint();
                }

                private void updateGame() {
                    gameState.updateTime();

                    // Update player team
                    updatePlayerTeam();

                    // Update opponent team (basic AI)
                    updateOpponentTeam();

                    // Update ball
                    ball.update();

                    // Check for try scoring
                    checkTryScoring();

                    // Check for collisions
                    checkCollisions();
                }

                private void updatePlayerTeam() {
                    for (Player player : playerTeam.getPlayers()) {
                        if (player == ballCarrier) {
                            handlePlayerMovement(player);
                        } else {
                            // Simple movement towards ball
                            moveTowardsBall(player);
                        }
                        player.updateStamina();
                        keepInBounds(player);
                    }
                }

                private void handlePlayerMovement(Player player) {
                    int dx = 0, dy = 0;
                    if (keysPressed[KeyEvent.VK_UP]) dy -= PLAYER_SPEED;
                    if (keysPressed[KeyEvent.VK_DOWN]) dy += PLAYER_SPEED;
                    if (keysPressed[KeyEvent.VK_LEFT]) dx -= PLAYER_SPEED;
                    if (keysPressed[KeyEvent.VK_RIGHT]) dx += PLAYER_SPEED;

                    // Normalize diagonal movement
                    if (dx != 0 && dy != 0) {
                        double length = Math.sqrt(dx * dx + dy * dy);
                        dx = (int) (dx * PLAYER_SPEED / length);
                        dy = (int) (dy * PLAYER_SPEED / length);
                    }

                    // Apply stamina effect
                    if (player.stamina > 0) {
                        player.move(dx, dy);
                        if (dx != 0 || dy != 0) {
                            player.stamina -= 0.5; // Consume stamina when moving
                        }
                    }

                    if (player.hasBall) {
                        ball.x = player.x;
                        ball.y = player.y;
                    }
                }

                private void moveTowardsBall(Player player) {
                    double dx = ball.x - player.x;
                    double dy = ball.y - player.y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance > 10 && player.stamina > 0) {
                        dx = dx * PLAYER_SPEED / distance;
                        dy = dy * PLAYER_SPEED / distance;
                        player.move((int) dx, (int) dy);
                        player.stamina -= 0.3; // Less stamina for non-ball carriers
                    }
                }

                private void updateOpponentTeam() {
                    for (Player player : opponentTeam.getPlayers()) {
                        if (!player.isTackling) {
                            // Chase ball carrier or ball
                            double targetX = ballCarrier != null ? ballCarrier.x : ball.x;
                            double targetY = ballCarrier != null ? ballCarrier.y : ball.y;
                            double dx = targetX - player.x;
                            double dy = targetY - player.y;
                            double distance = Math.sqrt(dx * dx + dy * dy);
                            if (distance > 10 && player.stamina > 0) {
                                dx = dx * PLAYER_SPEED / distance;
                                dy = dy * PLAYER_SPEED / distance;
                                player.move((int) dx, (int) dy);
                                player.stamina -= 0.3;
                            }
                            // Attempt tackle if close
                            if (ballCarrier != null && distance < 30 && random.nextDouble() < 0.02) {
                                player.isTackling = true;
                                attemptTackle(player, ballCarrier);
                            }
                        } else {
                            player.tackleCooldown--;
                            if (player.tackleCooldown <= 0) {
                                player.isTackling = false;
                            }
                        }
                        player.updateStamina();
                        keepInBounds(player);
                    }
                }

                private void keepInBounds(Player player) {
                    player.x = Math.max(PLAYER_SIZE / 2, Math.min(FIELD_WIDTH - PLAYER_SIZE / 2, player.x));
                    player.y = Math.max(PLAYER_SIZE / 2, Math.min(FIELD_HEIGHT - PLAYER_SIZE / 2, player.y));
                }

                private void checkTryScoring() {
                    if (ballCarrier != null) {
                        if (ballCarrier.x <= TRY_LINE_WIDTH && ballCarrier.team.isPlayerTeam) {
                            gameState.playerScore += 5;
                            resetAfterTry();
                        } else if (ballCarrier.x >= FIELD_WIDTH - TRY_LINE_WIDTH && !ballCarrier.team.isPlayerTeam) {
                            gameState.opponentScore += 5;
                            resetAfterTry();
                        }
                    }
                }

                private void resetAfterTry() {
                    ball.x = FIELD_WIDTH / 2;
                    ball.y = FIELD_HEIGHT / 2;
                    ball.dx = 0;
                    ball.dy = 0;
                    if (ballCarrier != null) {
                        ballCarrier.hasBall = false;
                        ballCarrier = null;
                    }
                    ballCarrier = playerTeam.getPlayers().get(0);
                    ballCarrier.hasBall = true;
                    resetPlayerPositions();
                    // Restore some stamina after try
                    for (Player player : playerTeam.getPlayers()) {
                        player.stamina = Math.min(MAX_STAMINA, player.stamina + 20);
                    }
                    for (Player player : opponentTeam.getPlayers()) {
                        player.stamina = Math.min(MAX_STAMINA, player.stamina + 20);
                    }
                }

                private void resetPlayerPositions() {
                    int index = 0;
                    for (Player player : playerTeam.getPlayers()) {
                        player.x = 200;
                        player.y = 100 + index * 100;
                        player.isTackling = false;
                        player.tackleCooldown = 0;
                        index++;
                    }
                    index = 0;
                    for (Player player : opponentTeam.getPlayers()) {
                        player.x = FIELD_WIDTH - 200;
                        player.y = 100 + index * 100;
                        player.isTackling = false;
                        player.tackleCooldown = 0;
                        index++;
                    }
                }

                private void attemptTackle(Player tackler, Player target) {
                    if (random.nextDouble() < 0.5) { // 50% tackle success
                        if (ballCarrier != null) {
                            ballCarrier.hasBall = false;
                            ballCarrier = null;
                            ball.dx = random.nextInt(5) - 2;
                            ball.dy = random.nextInt(5) - 2;
                        }
                    }
                    tackler.tackleCooldown = 30;
                    tackler.stamina -= 10; // Tackling consumes stamina
                }

                private void checkCollisions() {
                    // Check player picking up loose ball
                    for (Player player : playerTeam.getPlayers()) {
                        if (!player.hasBall && ballCarrier == null) ,
                        double dx = ball.x - player.x;
                        double dy = ball.y - player.y;
                        if (Math.sqrt(dx * dx + dy * dy) < PLAYER_SIZE / 2 + BALL_SIZE / 2) {
                            player.hasBall = true;
                            ballCarrier = player;
                            ball.dx = 0;
                            ball.dy = 0;
                        }
                    }
                }
        for (Player player : opponentTeam.getPlayers()) {
                    if (!player.hasBall && ballCarrier == null) {
                        double dx = ball.x - player.x;
                        double dy = ball.y - player.y;
                        if (Math.sqrt(dx * dx + dy * dy) < PLAYER_SIZE / 2 + BALL_SIZE / 2) {
                            player.hasBall = true;
                            ballCarrier = player;
                            ball.dx = 0;
                            ball.dy = 0;
                        }
                    }
                }

                // Check player collisions to avoid overlap
        for (Player p1 : playerTeam.getPlayers()) {
                    for (Player p2 : playerTeam.getPlayers()) {
                        if (p1 != p2) {
                            resolvePlayerCollision(p1, p2);
                        }
                    }
                    for (Player p2 : opponentTeam.getPlayers()) {
                        resolvePlayerCollision(p1, p2);
                    }
                }
        for (Player p1 : opponentTeam.getPlayers()) {
                    for (Player p2 : opponentTeam.getPlayers()) {
                        if (p1 != p2) {
                            resolvePlayerCollision(p1, p2);
                        }
                    }
                }
            }

            private void resolvePlayerCollision(Player p1, Player p2) {
                double dx = p2.x - p1.x;
                double dy = p2.y - p1.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                double minDistance = PLAYER_SIZE;
                if (distance < minDistance && distance > 0) {
                    double overlap = (minDistance - distance) / 2;
                    dx = dx / distance * overlap;
                    dy = dy / distance * overlap;
                    p1.x -= dx;
                    p1.y -= dy;
                    p2.x += dx;
                    p2.y += dy;
                    keepInBounds(p1);
                    keepInBounds(p2);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() < 256) {
                    keysPressed[e.getKeyCode()] = true;
                    if (e.getKeyCode() == KeyEvent.VK_SPACE && ballCarrier != null && ballCarrier.team.isPlayerTeam) {
                        passBall();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() < 256) {
                    keysPressed[e.getKeyCode()] = false;
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {}

            private void passBall() {
                if (ballCarrier != null && ballCarrier.stamina > 10) {
                    Player target = findNearestTeammate(ballCarrier);
                    if (target != null) {
                        ballCarrier.hasBall = false;
                        ballCarrier = null;
                        double dx = target.x - ball.x;
                        double dy = target.y - ball.y;
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        if (distance > 0) {
                            ball.dx = dx * PASS_SPEED / distance;
                            ball.dy = dy * PASS_SPEED / distance;
                            ballCarrier.stamina -= 5; // Passing consumes stamina
                        }
                    }
                }
            }

            private Player findNearestTeammate(Player current) {
                Player nearest = null;
                double minDistance = Double.MAX_VALUE;
                for (Player teammate : current.team.getPlayers()) {
                    if (teammate != current) {
                        double dx = teammate.x - current.x;
                        double dy = teammate.y - current.y;
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        if (distance < minDistance && distance > 10) { // Avoid very close players
                            minDistance = distance;
                            nearest = teammate;
                        }
                    }
                }
                return nearest;
            }
        }

        class Team {
            private String name;
            private Color color;
            private ArrayList<Player> players;
            public boolean isPlayerTeam;

            public Team(String name, Color color, boolean isPlayerTeam) {
                this.name = name;
                this.color = color;
                this.isPlayerTeam = isPlayerTeam;
                players = new ArrayList<>();
                initializePlayers();
            }

            private void initializePlayers() {
                // Rugby Sevens has 7 players per team
                for (int i = 0; i < 7; i++) {
                    int x = isPlayerTeam ? 200 : GamePanel.FIELD_WIDTH - 200;
                    int y = 100 + i * 100;
                    players.add(new Player(x, y, this));
                }
            }

            public ArrayList<Player> getPlayers() {
                return players;
            }

            public void draw(Graphics2D g2d) {
                for (Player player : players) {
                    player.draw(g2d, color);
                }
            }
        }

        class Player {
            public double x, y;
            public boolean hasBall;
            public boolean isTackling;
            public int tackleCooldown;
            public double stamina;
            public Team team;

            public Player(double x, double y, Team team) {
                this.x = x;
                this.y = y;
                this.team = team;
                this.hasBall = false;
                this.isTackling = false;
                this.tackleCooldown = 0;
                this.stamina = GamePanel.MAX_STAMINA;
            }

            public void move(int dx, int dy) {
                x += dx;
                y += dy;
            }

            public void updateStamina() {
                // Regenerate stamina slowly when not moving
                if (!hasBall && !isTackling) {
                    stamina = Math.min(GamePanel.MAX_STAMINA, stamina + 0.2);
                }
            }

            public void draw(Graphics2D g2d, Color color) {
                g2d.setColor(color);
                g2d.fillOval((int) x - GamePanel.PLAYER_SIZE / 2, (int) y - GamePanel.PLAYER_SIZE / 2,
                        GamePanel.PLAYER_SIZE, GamePanel.PLAYER_SIZE);
                if (isTackling) {
                    g2d.setColor(Color.YELLOW);
                    g2d.drawOval((int) x - GamePanel.PLAYER_SIZE / 2 - 5, (int) y - GamePanel.PLAYER_SIZE / 2 - 5,
                            GamePanel.PLAYER_SIZE + 10, GamePanel.PLAYER_SIZE + 10);
                }
                if (hasBall) {
                    g2d.setColor(Color.BLACK);
                    g2d.drawOval((int) x - GamePanel.PLAYER_SIZE / 2, (int) y - GamePanel.PLAYER_SIZE / 2,
                            GamePanel.PLAYER_SIZE, GamePanel.PLAYER_SIZE);
                }
            }
        }

        class Ball {
            public double x, y;
            public double dx, dy;

            public Ball(double x, double y) {
                this.x = x;
                this.y = y;
                this.dx = 0;
                this.dy = 0;
            }

            public void update() {
                x += dx;
                y += dy;
                // Slow down ball due to friction
                dx *= 0.98;
                dy *= 0.98;
                // Keep ball in bounds
                x = Math.max(GamePanel.BALL_SIZE / 2, Math.min(GamePan
                        el.FIELD_WIDTH - GamePanel.BALL_SIZE / 2, x));
                y = Math.max(GamePanel.BALL_SIZE / Questions?