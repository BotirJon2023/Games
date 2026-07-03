import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.Timer;
import java.util.Random;

public class BasketballGame3D extends JPanel implements ActionListener, KeyListener {
    // Game states
    private static final int MENU = 0, PLAYING = 1, GAME_OVER = 2;
    private int gameState = MENU;

    // Game modes
    private static final int TWO_PLAYER = 0, VS_COMPUTER = 1;
    private int gameMode = TWO_PLAYER;
    private int difficulty = 1; // 1-3

    // Players
    private Player player1, player2, computer;
    private Ball ball;
    private Hoop hoop;
    private Backboard backboard;

    // Score
    private int score1 = 0, score2 = 0;
    private int targetScore = 5; // First to 5 wins

    // Animation
    private Timer timer;
    private int frameCount = 0;
    private boolean gamePaused = false;
    private String message = "";
    private int messageTimer = 0;

    // Mouse control for aiming
    private Point mousePosition = new Point(0, 0);
    private boolean mousePressed = false;

    // 3D rendering components
    private double rotationAngle = 0;
    private double courtZoom = 1.0;

    // Shadow and lighting
    private double ambientLight = 0.6;
    private double[] lightDirection = {0.5, -0.8, 0.3};

    private Random random = new Random();

    public BasketballGame3D() {
        setPreferredSize(new Dimension(1024, 768));
        setBackground(new Color(30, 30, 40));
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                mousePressed = true;
                mousePosition = e.getPoint();
                if (gameState == MENU) {
                    handleMenuClick(e.getX(), e.getY());
                }
            }
            public void mouseReleased(MouseEvent e) {
                mousePressed = false;
                if (gameState == PLAYING) {
                    attemptShot(e.getX(), e.getY());
                }
            }
            public void mouseMoved(MouseEvent e) {
                mousePosition = e.getPoint();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                mousePosition = e.getPoint();
            }
        });

        initGame();
        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    private void initGame() {
        // Initialize players
        player1 = new Player(200, 350, Color.WHITE, "Player 1");
        player2 = new Player(700, 350, Color.RED, "Player 2");
        computer = new Player(700, 350, Color.BLUE, "Computer");

        // Initialize ball
        ball = new Ball(400, 300);

        // Initialize hoop and backboard
        hoop = new Hoop(400, 150);
        backboard = new Backboard(400, 130);

        // Set initial positions
        resetPositions();
    }

    private void resetPositions() {
        ball.x = 400;
        ball.y = 300;
        ball.vx = 0;
        ball.vy = 0;
        ball.vz = 0;
        ball.isHeld = false;

        if (gameMode == TWO_PLAYER) {
            player1.x = 200;
            player1.y = 350;
            player2.x = 700;
            player2.y = 350;
        } else {
            player1.x = 200;
            player1.y = 350;
            computer.x = 700;
            computer.y = 350;
        }
    }

    private void handleMenuClick(int x, int y) {
        // Menu buttons
        if (x > 362 && x < 662) {
            if (y > 250 && y < 310) {
                gameMode = TWO_PLAYER;
                startGame();
            } else if (y > 330 && y < 390) {
                gameMode = VS_COMPUTER;
                startGame();
            } else if (y > 410 && y < 470) {
                // Difficulty selection
                difficulty = (difficulty % 3) + 1;
                updateDifficultyDisplay();
            }
        }
    }

    private void startGame() {
        gameState = PLAYING;
        score1 = 0;
        score2 = 0;
        resetPositions();
        message = "Game Started!";
        messageTimer = 120;
    }

    private void updateDifficultyDisplay() {
        String[] diffNames = {"Easy", "Medium", "Hard"};
        message = "Difficulty: " + diffNames[difficulty-1];
        messageTimer = 60;
    }

    private void attemptShot(int mouseX, int mouseY) {
        if (gameState != PLAYING || gamePaused) return;

        Player currentPlayer = getCurrentPlayer();
        if (currentPlayer == null || ball.isHeld) return;

        // Calculate shot angle based on mouse position
        double dx = mouseX - currentPlayer.x;
        double dy = mouseY - currentPlayer.y;
        double distance = Math.sqrt(dx*dx + dy*dy);

        if (distance > 50) {
            double power = Math.min(distance / 300.0, 1.0);
            double angle = Math.atan2(dy, dx);

            ball.vx = Math.cos(angle) * power * 12;
            ball.vy = Math.sin(angle) * power * 12 - 3;
            ball.vz = (random.nextDouble() - 0.5) * 2;
            ball.isHeld = false;

            // Add arc to the shot
            ball.vy += 2; // Gravity effect
        }
    }

    private Player getCurrentPlayer() {
        if (gameMode == TWO_PLAYER) {
            return (ball.x < 400) ? player1 : player2;
        } else {
            return (ball.x < 400) ? player1 : computer;
        }
    }

    private void updateGame() {
        if (gameState != PLAYING || gamePaused) return;

        // Update physics
        updatePhysics();

        // Update AI
        if (gameMode == VS_COMPUTER && ball.x > 400) {
            updateAI();
        }

        // Update players
        updatePlayers();

        // Check for scoring
        checkScore();

        // Update rotation for 3D effect
        rotationAngle += 0.01;

        // Update message timer
        if (messageTimer > 0) messageTimer--;
        else message = "";

        // Check game over
        if (score1 >= targetScore || score2 >= targetScore) {
            gameState = GAME_OVER;
            if (score1 >= targetScore) {
                message = "Player 1 Wins!";
            } else {
                message = gameMode == TWO_PLAYER ? "Player 2 Wins!" : "Computer Wins!";
            }
        }
    }

    private void updatePhysics() {
        // Update ball physics
        ball.vy += 0.3; // Gravity

        ball.x += ball.vx;
        ball.y += ball.vy;
        ball.z += ball.vz;

        // Ball friction
        ball.vx *= 0.99;
        ball.vz *= 0.98;

        // Bounce off walls
        if (ball.x < 50 || ball.x > 750) {
            ball.vx *= -0.8;
            ball.x = Math.max(50, Math.min(750, ball.x));
        }
        if (ball.y > 600) {
            ball.vy *= -0.7;
            ball.y = 600;
            if (Math.abs(ball.vy) < 0.5) {
                ball.vy = 0;
            }
        }

        // Check hoop collision
        if (ball.x > hoop.x - 30 && ball.x < hoop.x + 30 &&
                ball.y > hoop.y - 10 && ball.y < hoop.y + 10 &&
                ball.z > -10 && ball.z < 10) {
            // Score!
            if (ball.vy < 0) {
                scorePoint();
            }
        }
    }

    private void updateAI() {
        // AI difficulty levels
        double aiSpeed;
        double shotAccuracy;
        switch(difficulty) {
            case 1: // Easy
                aiSpeed = 2.0;
                shotAccuracy = 0.4;
                break;
            case 2: // Medium
                aiSpeed = 3.0;
                shotAccuracy = 0.6;
                break;
            default: // Hard
                aiSpeed = 4.0;
                shotAccuracy = 0.85;
                break;
        }

        // Move towards ball
        if (!computer.hasBall) {
            double dx = ball.x - computer.x;
            double dy = ball.y - computer.y;
            double dist = Math.sqrt(dx*dx + dy*dy);

            if (dist > 50) {
                computer.x += (dx / dist) * aiSpeed;
                computer.y += (dy / dist) * aiSpeed;
            } else {
                // Steal ball
                if (!ball.isHeld) {
                    computer.hasBall = true;
                    ball.isHeld = true;
                    ball.x = computer.x + 30;
                    ball.y = computer.y - 20;
                }
            }
        } else {
            // Move towards hoop
            double dx = hoop.x - computer.x;
            double dy = hoop.y - computer.y;
            double dist = Math.sqrt(dx*dx + dy*dy);

            if (dist > 100) {
                computer.x += (dx / dist) * aiSpeed * 0.5;
                computer.y += (dy / dist) * aiSpeed * 0.5;
                ball.x = computer.x + 30;
                ball.y = computer.y - 20;
            } else {
                // Shoot with accuracy based on difficulty
                if (random.nextDouble() < shotAccuracy) {
                    shootBall(computer);
                } else {
                    // Miss shot
                    computer.hasBall = false;
                    ball.isHeld = false;
                    ball.vx = random.nextDouble() * 10 - 5;
                    ball.vy = random.nextDouble() * 5 - 10;
                }
            }
        }
    }

    private void shootBall(Player player) {
        double power = 8 + random.nextDouble() * 4;
        double angle = -Math.PI / 2 + random.nextDouble() * 0.5 - 0.25;

        ball.vx = Math.cos(angle) * power * 0.5;
        ball.vy = Math.sin(angle) * power * 0.3 - 2;
        ball.vz = (random.nextDouble() - 0.5) * 3;
        ball.isHeld = false;
        player.hasBall = false;
    }

    private void updatePlayers() {
        if (gameMode == TWO_PLAYER) {
            // Player 1 controls - Keyboard
            // Player 2 controls - Mouse based
            updatePlayerMovement(player1, '1');
            updatePlayerMovement(player2, '2');
        } else {
            updatePlayerMovement(player1, '1');
            // Computer controlled
        }
    }

    private void updatePlayerMovement(Player player, char id) {
        // AI movement for computer
        if (id == '2' && gameMode == VS_COMPUTER) return;

        // Manual control
        if (id == '1') {
            // Player 1: WASD controls
            // Implemented in keyPressed/Released
        } else {
            // Player 2: Arrow keys or mouse
            // Implemented in keyPressed/Released
        }
    }

    private void checkScore() {
        // Check if ball passed through hoop
        if (ball.y < hoop.y && ball.y > hoop.y - 30 &&
                ball.x > hoop.x - 25 && ball.x < hoop.x + 25) {
            // Ball went through hoop
            if (ball.vy < -2) {
                scorePoint();
            }
        }
    }

    private void scorePoint() {
        if (ball.x < 400) {
            score1++;
        } else {
            score2++;
        }

        message = "Score! " + score1 + " - " + score2;
        messageTimer = 90;
        resetPositions();

        // Animate celebration
        if (score1 >= targetScore || score2 >= targetScore) {
            gameState = GAME_OVER;
        }
    }

    private void renderMenu(Graphics2D g) {
        // Background
        g.setColor(new Color(30, 30, 40));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Title with 3D effect
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Title shadow
        g.setColor(new Color(0, 0, 0, 100));
        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.drawString("3D BASKETBALL", 175, 185);

        // Title
        GradientPaint grad = new GradientPaint(0, 100, Color.ORANGE,
                0, 180, Color.RED);
        g.setPaint(grad);
        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.drawString("3D BASKETBALL", 170, 180);

        // Subtitle
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("Beautiful 3D Basketball Game with Realistic Physics", 280, 220);

        // Menu buttons with hover effects
        drawMenuButton(g, "2 Player Mode", 362, 250, 300, 60,
                gameMode == TWO_PLAYER && gameState == PLAYING);
        drawMenuButton(g, "vs Computer", 362, 330, 300, 60,
                gameMode == VS_COMPUTER && gameState == PLAYING);

        // Difficulty selector
        String[] diffNames = {"Easy", "Medium", "Hard"};
        String diffText = "Difficulty: " + diffNames[difficulty-1] + " (Click to change)";
        drawMenuButton(g, diffText, 362, 410, 300, 50, false);

        // Instructions
        g.setColor(new Color(200, 200, 200));
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("Player 1: WASD to move, Click to shoot", 350, 520);
        g.drawString("Player 2: Arrow keys to move, Click to shoot", 350, 550);
        g.drawString("First to " + targetScore + " wins!", 380, 580);

        // 3D court preview
        draw3DCourtPreview(g);
    }

    private void drawMenuButton(Graphics2D g, String text, int x, int y,
    int width, int height, boolean active) {
        // Shadow
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(x + 3, y + 3, width, height, 15, 15);

        // Button
        GradientPaint grad;
        if (active) {
            grad = new GradientPaint(x, y, new Color(50, 200, 50),
                    x, y+height, new Color(30, 150, 30));
        } else {
            grad = new GradientPaint(x, y, new Color(70, 70, 80),
                    x, y+height, new Color(40, 40, 50));
        }
        g.setPaint(grad);
        g.fillRoundRect(x, y, width, height, 15, 15);

        // Border
        g.setColor(new Color(200, 200, 200, 50));
        g.drawRoundRect(x, y, width, height, 15, 15);

        // Text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        int textX = x + (width - fm.stringWidth(text)) / 2;
        int textY = y + (height + fm.getHeight()) / 2 - 5;
        g.drawString(text, textX, textY);
    }

    private void draw3DCourtPreview(Graphics2D g) {
        // Draw a small 3D court in the corner
        int x = 750, y = 650, size = 200;

        // Perspective
        g.setColor(new Color(100, 100, 120, 50));
        g.fillRect(x, y, size, size/2);

        // Court lines
        g.setColor(new Color(200, 200, 200, 80));
        g.drawRect(x, y, size, size/2);

        // Center circle
        g.drawOval(x + size/2 - 20, y + size/4 - 20, 40, 20);

        // Hoop
        g.setColor(new Color(255, 100, 100, 100));
        g.fillOval(x + size/2 - 10, y - 5, 20, 10);
        g.drawLine(x + size/2 - 15, y + 5, x + size/2 + 15, y + 5);
    }

    private void renderGame(Graphics2D g) {
        // Clear
        g.setColor(new Color(40, 40, 60));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw 3D court
        draw3DCourt(g);

        // Draw backboard and hoop
        drawBackboard(g);
        drawHoop(g);

        // Draw players
        if (gameMode == TWO_PLAYER) {
            drawPlayer(g, player1);
            drawPlayer(g, player2);
        } else {
            drawPlayer(g, player1);
            drawPlayer(g, computer);
        }

        // Draw ball
        drawBall(g);

        // Draw HUD
        drawHUD(g);

        // Draw shadows
        drawShadows(g);

        // Draw messages
        if (!message.isEmpty()) {
            drawMessage(g);
        }

        // Draw controls hint
        drawControlsHint(g);
    }

    private void draw3DCourt(Graphics2D g) {
        // 3D perspective court
        int width = getWidth();
        int height = getHeight();

        // Court floor with perspective
        int[] xPoints = {100, width-100, width-200, 200};
        int[] yPoints = {height-100, height-100, height-20, height-20};

        // Gradient floor
        GradientPaint floorGrad = new GradientPaint(
                0, height-100, new Color(139, 69, 19),
                0, height-20, new Color(101, 67, 33));
        g.setPaint(floorGrad);
        g.fillPolygon(xPoints, yPoints, 4);

        // Court lines
        g.setColor(new Color(255, 255, 255, 100));

        // Center line
        int midX = width/2;
        int topY = height-20;
        int bottomY = height-100;
        double slope = (double)(topY - bottomY) / (width - 200 - 100);
        int centerTopX = (int)(midX - 100) + (int)((midX - 200) * slope);
        g.drawLine(midX, bottomY, centerTopX, topY);

        // Draw 3D effect lines
        for (int i = 0; i < 10; i++) {
            double t = i / 10.0;
            int x1 = (int)(100 + t * (width - 200));
            int y1 = (int)(height - 100 + t * 80);
            int x2 = (int)(200 + t * (width - 400));
            int y2 = (int)(height - 20 + t * 0);
            g.setColor(new Color(255, 255, 255, 20 + (int)(t * 30)));
            g.drawLine(x1, y1, x2, y2);
        }

        // Center circle (perspective)
        g.setColor(new Color(255, 255, 255, 60));
        g.drawOval(midX - 80, height - 70, 160, 50);
    }

    private void drawBackboard(Graphics2D g) {
        // 3D backboard
        int x = backboard.x - 30;
        int y = backboard.y - 25;
        int width = 60;
        int height = 50;

        // Shadow
        g.setColor(new Color(0, 0, 0, 30));
        g.fillRect(x + 5, y + 5, width, height);

        // Backboard
        GradientPaint grad = new GradientPaint(x, y, new Color(200, 200, 220),
                x+width, y+height, new Color(150, 150, 180));
        g.setPaint(grad);
        g.fillRect(x, y, width, height);
        g.setColor(new Color(100, 100, 120));
        g.drawRect(x, y, width, height);

        // Square on backboard
        g.setColor(new Color(255, 0, 0, 100));
        g.drawRect(x + 15, y + 5, 30, 40);

        // 3D effect
        g.setColor(new Color(255, 255, 255, 30));
        g.drawLine(x, y, x+width/2, y+height/2);
    }

    private void drawHoop(Graphics2D g) {
        // 3D hoop with realistic shading
        int x = hoop.x - 30;
        int y = hoop.y - 5;
        int width = 60;
        int height = 10;

        // Rim shadow
        g.setColor(new Color(0, 0, 0, 50));
        g.fillOval(x + 3, y + 5, width, height);

        // Rim
        GradientPaint grad = new GradientPaint(x, y, new Color(255, 150, 50),
                x, y+height, new Color(200, 100, 0));
        g.setPaint(grad);
        g.fillOval(x, y, width, height);

        // Rim highlight
        g.setColor(new Color(255, 200, 150, 100));
        g.drawOval(x + 5, y - 2, width - 10, 6);

        // Net with 3D effect
        g.setColor(new Color(255, 255, 255, 100));
        for (int i = 0; i < 8; i++) {
            int netX = x + 5 + i * 7;
            g.drawLine(netX, y + height/2, netX - 3 + i * 2, y + height + 20);
        }
    }

    private void drawPlayer(Graphics2D g, Player player) {
        // 3D player with realistic animation
        int x = (int)player.x;
        int y = (int)player.y;
        int size = 30;

        // Shadow
        g.setColor(new Color(0, 0, 0, 50));
        g.fillOval(x - size/2 + 5, y + size - 5, size, size/3);

        // Body
        GradientPaint grad = new GradientPaint(x - size/2, y - size/2,
                player.color.brighter(),
                x + size/2, y + size/2,
                player.color.darker());
        g.setPaint(grad);

        // Animate movement
        double bob = player.isMoving ? Math.sin(frameCount * 0.1) * 2 : 0;

        // Head
        g.fillOval(x - size/3, (int) (y- size + bob), size*2/3, size*2/3);

        // Body
        int[] bodyX = {x - size/2, x + size/2, x + size/3, x - size/3};
        int[] bodyY = {(int) (y - size/3 + bob), (int) (y - size/3 + bob), y + size/3, y + size/3};
        g.fillPolygon(bodyX, bodyY, 4);

        // Arms
        g.setStroke(new BasicStroke(3));
        if (player.hasBall) {
            // Shooting pose
            g.drawLine(x - size/2, (int) (y - size/4 + bob), x - size, (int) (y - size + bob));
            g.drawLine(x + size/2, (int) (y - size/4 + bob), x + size, (int) (y - size + bob));
        } else {
            // Rest pose with arm swing
            double armSwing = player.isMoving ? Math.sin(frameCount * 0.1) * 5 : 0;
            g.drawLine(x - size/2, (int) (y - size/4 + bob), x - size/2 - 10, (int) (y + armSwing));
            g.drawLine(x + size/2, (int) (y - size/4 + bob), x + size/2 + 10, (int) (y - armSwing));
        }

        // Legs
        double legSwing = player.isMoving ? Math.sin(frameCount * 0.1) * 5 : 0;
        g.drawLine(x - size/4, y + size/3, x - size/4 - 5, (int) (y + size/2 + legSwing));
        g.drawLine(x + size/4, y + size/3, x + size/4 + 5, (int) (y + size/2 - legSwing));

        // Player name
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        String name = player == player1 ? "P1" : (gameMode == TWO_PLAYER ? "P2" : "CPU");
        g.drawString(name, x - 10, y + size + 20);

        // Ball possession indicator
        if (player.hasBall) {
            g.setColor(new Color(255, 255, 0, 100));
            g.drawOval(x - size, y - size, size*2, size*2);
        }
    }

    private void drawBall(Graphics2D g) {
        // 3D ball with rotation
        int x = (int)ball.x;
        int y = (int)ball.y;
        int size = 20;

        // Shadow
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(x - size/2 + 5, y + size - 10, size, size/3);

        // Ball gradient
        GradientPaint grad = new GradientPaint(x - size/2, y - size/2,
                new Color(255, 160, 50),
                x + size/2, y + size/2,
                new Color(200, 80, 0));
        g.setPaint(grad);
        g.fillOval(x - size/2, y - size/2, size, size);

        // Ball lines (rotation animation)
        g.setColor(new Color(0, 0, 0, 80));
        double angle = frameCount * 0.05;

        // Horizontal line
        g.drawArc(x - size/2, y - size/4, size, size/2,
                (int)Math.toDegrees(angle), 180);
        g.drawArc(x - size/2, y - size/4, size, size/2,
                (int)Math.toDegrees(angle + Math.PI), 180);

        // Vertical line
        g.drawArc(x - size/4, y - size/2, size/2, size,
                (int)Math.toDegrees(angle + Math.PI/2), 180);
        g.drawArc(x - size/4, y - size/2, size/2, size,
                (int)Math.toDegrees(angle + 3*Math.PI/2), 180);

        // Highlight
        g.setColor(new Color(255, 255, 255, 80));
        g.fillOval(x - size/4, y - size/3, size/6, size/6);
    }

    private void drawShadows(Graphics2D g) {
        // Ambient shadow effect
        g.setColor(new Color(0, 0, 0, 20));
        int width = getWidth();
        int height = getHeight();

        // Gradient shadow from top
        GradientPaint shadowGrad = new GradientPaint(
                0, 0, new Color(0, 0, 0, 50),
                0, height/2, new Color(0, 0, 0, 0));
        g.setPaint(shadowGrad);
        g.fillRect(0, 0, width, height/2);
    }

    private void drawHUD(Graphics2D g) {
        // Score display with 3D effect
        g.setColor(new Color(0, 0, 0, 50));
        g.fillRoundRect(20, 20, 200, 80, 15, 15);
        g.setColor(new Color(255, 255, 255, 30));
        g.drawRoundRect(20, 20, 200, 80, 15, 15);

        // Scores
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        String scoreText = score1 + " - " + score2;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(scoreText, 120 - fm.stringWidth(scoreText)/2, 75);

        // Player labels
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(player1.color);
        g.drawString("P1", 45, 45);
        g.setColor(gameMode == TWO_PLAYER ? player2.color : computer.color);
        g.drawString(gameMode == TWO_PLAYER ? "P2" : "CPU", 165, 45);

        // Target score
        g.setColor(new Color(200, 200, 200));
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString("First to " + targetScore, 70, 95);

        // Difficulty indicator (vs computer)
        if (gameMode == VS_COMPUTER) {
            String[] diffNames = {"Easy", "Medium", "Hard"};
            g.setColor(new Color(200, 200, 200, 100));
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.drawString("Difficulty: " + diffNames[difficulty-1], 830, 40);
        }
    }

    private void drawMessage(Graphics2D g) {
        // Message with animation
        int alpha = Math.min(255, messageTimer * 4);
        float scale = 1.0f + (float)(120 - messageTimer) / 240;

        g.setColor(new Color(0, 0, 0, alpha / 2));
        g.fillRoundRect(getWidth()/2 - 150, 100, 300, 60, 20, 20);

        g.setColor(new Color(255, 255, 255, alpha));
        g.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        int x = getWidth()/2 - fm.stringWidth(message)/2;
        g.drawString(message, x, 140);
    }

    private void drawControlsHint(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, 30));
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.drawString("P1: WASD | P2: Arrows | Click to shoot", 10, getHeight() - 10);
    }

    private void renderGameOver(Graphics2D g) {
        // Render game over overlay
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 72));
        String title = "Game Over!";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, getWidth()/2 - fm.stringWidth(title)/2, 250);

        // Winner
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        String winner = score1 >= targetScore ? "Player 1 Wins!" :
                (gameMode == TWO_PLAYER ? "Player 2 Wins!" : "Computer Wins!");
        fm = g.getFontMetrics();
        g.drawString(winner, getWidth()/2 - fm.stringWidth(winner)/2, 350);

        // Final score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        String score = score1 + " - " + score2;
        fm = g.getFontMetrics();
        g.drawString(score, getWidth()/2 - fm.stringWidth(score)/2, 420);

        // Restart button
        drawMenuButton(g, "Play Again", 362, 480, 300, 60, false);
        drawMenuButton(g, "Main Menu", 362, 560, 300, 60, false);
    }

    private void drawLights(Graphics2D g) {
        // Lighting effect
        int width = getWidth();
        int height = getHeight();

        // Light source
        g.setColor(new Color(255, 255, 200, 20));
        int lightX = width/2 + (int)(Math.sin(rotationAngle) * 100);
        int lightY = height/4;
        RadialGradientPaint lightGrad = new RadialGradientPaint(
                lightX, lightY, 400,
                new float[]{0, 0.5f, 1},
                new Color[]{new Color(255, 255, 200, 60),
                        new Color(255, 255, 200, 20),
                        new Color(255, 255, 200, 0)});
        g.setPaint(lightGrad);
        g.fillRect(0, 0, width, height);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        // Draw lighting
        drawLights(g2d);

        switch(gameState) {
            case MENU:
                renderMenu(g2d);
                break;
            case PLAYING:
                renderGame(g2d);
                break;
            case GAME_OVER:
                renderGame(g2d);
                renderGameOver(g2d);
                break;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == PLAYING && !gamePaused) {
            updateGame();
            frameCount++;
        }
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (gameState == PLAYING && !gamePaused) {
            // Player 1 controls (WASD)
            switch(key) {
                case KeyEvent.VK_W:
                    player1.vy = -3;
                    player1.isMoving = true;
                    break;
                case KeyEvent.VK_S:
                    player1.vy = 3;
                    player1.isMoving = true;
                    break;
                case KeyEvent.VK_A:
                    player1.vx = -3;
                    player1.isMoving = true;
                    break;
                case KeyEvent.VK_D:
                    player1.vx = 3;
                    player1.isMoving = true;
                    break;
                case KeyEvent.VK_SPACE:
                    if (!ball.isHeld && player1.hasBall) {
                        shootBall(player1);
                    }
                    break;

                // Player 2 controls (Arrow keys)
                case KeyEvent.VK_UP:
                    if (gameMode == TWO_PLAYER) {
                        player2.vy = -3;
                        player2.isMoving = true;
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if (gameMode == TWO_PLAYER) {
                        player2.vy = 3;
                        player2.isMoving = true;
                    }
                    break;
                case KeyEvent.VK_LEFT:
                    if (gameMode == TWO_PLAYER) {
                        player2.vx = -3;
                        player2.isMoving = true;
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (gameMode == TWO_PLAYER) {
                        player2.vx = 3;
                        player2.isMoving = true;
                    }
                    break;
                case KeyEvent.VK_ENTER:
                    if (gameMode == TWO_PLAYER && player2.hasBall) {
                        shootBall(player2);
                    }
                    break;
            }

            // Pause
            if (key == KeyEvent.VK_P) {
                gamePaused = !gamePaused;
            }
        }

        // Restart
        if (key == KeyEvent.VK_R && (gameState == GAME_OVER || gameState == PLAYING)) {
            if (gameState == GAME_OVER) {
                startGame();
            } else {
                resetPositions();
                score1 = 0;
                score2 = 0;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        // Player 1
        if (key == KeyEvent.VK_W || key == KeyEvent.VK_S) {
            player1.vy = 0;
            player1.isMoving = false;
        }
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_D) {
            player1.vx = 0;
            player1.isMoving = false;
        }

        // Player 2
        if (gameMode == TWO_PLAYER) {
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) {
                player2.vy = 0;
                player2.isMoving = false;
            }
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                player2.vx = 0;
                player2.isMoving = false;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes
    class Player {
        double x, y;
        double vx = 0, vy = 0;
        Color color;
        String name;
        boolean hasBall = false;
        boolean isMoving = false;

        Player(double x, double y, Color color, String name) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.name = name;
        }
    }

    class Ball {
        double x, y, z;
        double vx, vy, vz;
        boolean isHeld = false;

        Ball(double x, double y) {
            this.x = x;
            this.y = y;
            this.z = 0;
        }
    }

    class Hoop {
        int x, y;

        Hoop(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    class Backboard {
        int x, y;

        Backboard(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("3D Basketball Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new BasketballGame3D());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}