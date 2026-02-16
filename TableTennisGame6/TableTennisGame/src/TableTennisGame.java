import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class TableTennisGame extends JPanel implements ActionListener, KeyListener, MouseMotionListener {

    private static final int WIDTH = 1100;
    private static final int HEIGHT = 700;
    private static final int FPS = 60;
    private static final int TABLE_WIDTH = 500;
    private static final int TABLE_LENGTH = 900;
    private static final int TABLE_HEIGHT = 40;
    private static final int NET_HEIGHT = 60;
    private static final int PADDLE_WIDTH = 110;
    private static final int PADDLE_DEPTH = 40;
    private static final int BALL_RADIUS = 18;

    // Colors
    private static final Color DARK_GREEN_TABLE = new Color(0, 120, 40);
    private static final Color LIGHT_GREEN_TABLE = new Color(0, 180, 60);
    private static final Color NET_COLOR = new Color(220, 220, 220);
    private static final Color WHITE_LINES = new Color(255, 255, 240);
    private static final Color PLAYER_PADDLE = new Color(200, 40, 40);
    private static final Color OPPONENT_PADDLE = new Color(40, 40, 200);
    private static final Color BALL_COLOR = new Color(255, 245, 180);
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 90);

    // Game state
    private double ballX, ballY, ballZ;
    private double ballVelX, ballVelY, ballVelZ;
    private boolean ballInPlay = false;
    private int playerScore = 0;
    private int opponentScore = 0;
    private boolean playerServing = true;
    private int rallyLength = 0;

    // Player paddle (bottom side)
    private double playerPaddleX;
    private double playerPaddleAngle = 0;
    private double playerPaddleSwingSpeed = 0;
    private boolean playerSwinging = false;

    // Opponent paddle (top side) - simple AI
    private double opponentPaddleX;
    private double opponentPaddleAngle = 0;
    private double opponentTargetX;

    // Animation & timing
    private Timer gameTimer;
    private long lastTime;
    private Random random = new Random();

    // Mouse / keyboard control
    private int mouseX = WIDTH / 2;
    private boolean keyLeft, keyRight, keySpace;

    // =================================================================
    //  MAIN METHOD - program entry point
    // =================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Table Tennis 2.5D - Swing Edition");
            TableTennisGame game = new TableTennisGame();
            frame.add(game);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(WIDTH, HEIGHT);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }

    // =================================================================
    //  CONSTRUCTOR - initialize game
    // =================================================================
    public TableTennisGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(15, 25, 45));
        setFocusable(true);

        addKeyListener(this);
        addMouseMotionListener(this);

        resetBallToServe();

        gameTimer = new Timer(1000 / FPS, this);
        gameTimer.start();

        lastTime = System.nanoTime();
    }

    // =============================================
    //  Reset ball position for new serve
    // =============================================
    private void resetBallToServe() {
        ballInPlay = false;
        rallyLength = 0;

        if (playerServing) {
            ballX = playerPaddleX;
            ballY = 120;
            ballZ = 80;
            ballVelX = 0;
            ballVelY = -1.2;
            ballVelZ = -3.8 + random.nextDouble() * 0.6;
        } else {
            ballX = opponentPaddleX;
            ballY = 120;
            ballZ = TABLE_LENGTH - 80;
            ballVelX = 0;
            ballVelY = -1.2;
            ballVelZ = 3.8 - random.nextDouble() * 0.6;
        }
    }

    // =============================================
    //  Core game loop (every frame)
    // =============================================
    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double delta = (now - lastTime) / 1_000_000_000.0;
        lastTime = now;

        if (delta > 0.1) delta = 0.1; // prevent huge jumps after pause

        updatePhysics(delta);
        updateAI();
        updatePaddleSwing(delta);

        repaint();
    }

    private void updatePhysics(double dt) {
        if (!ballInPlay) return;

        // Gravity
        ballVelY += 280 * dt;   // quite strong gravity for dramatic bounce

        ballX += ballVelX * dt;
        ballY += ballVelY * dt;
        ballZ += ballVelZ * dt;

        // Bounce on table surface (Y=0)
        if (ballY <= BALL_RADIUS && ballVelY > 0) {
            ballY = BALL_RADIUS;
            ballVelY = -ballVelY * 0.82;   // energy loss ~18%
            ballVelZ *= 0.94;
            ballVelX *= 0.96;

            // spin effect simulation (very fake)
            if (Math.abs(ballVelZ) > 1) {
                ballVelX += (random.nextDouble() - 0.5) * 1.2;
            }

            rallyLength++;
            playBounceSoundFake();
        }

        // Net collision (very simplified)
        if (Math.abs(ballZ - TABLE_LENGTH/2) < BALL_RADIUS + 8 &&
                ballY < NET_HEIGHT + BALL_RADIUS + 10 &&
                ballY > NET_HEIGHT - 30) {
            // hit net → point to opponent
            if (ballVelZ > 0) {
                playerScore++;
            } else {
                opponentScore++;
            }
            resetBallToServe();
            playerServing = !playerServing;
            return;
        }

        // Out of bounds - side lines
        if (Math.abs(ballX) > TABLE_WIDTH/2 + 40) {
            if (ballZ > 0 && ballZ < TABLE_LENGTH) {
                // out → point to the other player
                if (ballVelZ > 0) opponentScore++;
                else playerScore++;
                resetBallToServe();
                playerServing = !playerServing;
            }
        }

        // Paddle hit zone - player
        checkPlayerPaddleHit();

        // Paddle hit zone - opponent
        checkOpponentPaddleHit();

        // Ball passed the end (scored)
        if (ballZ < -60) {
            opponentScore++;
            playerServing = false;
            resetBallToServe();
        }
        if (ballZ > TABLE_LENGTH + 60) {
            playerScore++;
            playerServing = true;
            resetBallToServe();
        }
    }

    private void checkPlayerPaddleHit() {
        double dz = ballZ - 80;
        if (dz > -50 && dz < 80 && ballVelZ < 0) {   // coming towards player
            double distX = ballX - playerPaddleX;
            double distY = ballY - 60;
            double horizontalDist = Math.sqrt(distX*distX + distY*distY);

            if (horizontalDist < PADDLE_WIDTH/2 + BALL_RADIUS + 20 &&
                    ballY < 220) {

                // Hit!
                ballVelZ = - (5.8 + Math.abs(ballVelZ)*0.4);
                ballVelY = -Math.abs(ballVelY)*0.3 - 4.2 - playerPaddleSwingSpeed*1.6;

                // Angle depending on hit position
                double hitAngle = distX / (PADDLE_WIDTH/2.0);
                ballVelX = hitAngle * 9.0 + (random.nextGaussian() * 0.7);

                // Swing effect
                if (playerSwinging) {
                    ballVelX += (mouseX - WIDTH/2) * 0.03;
                    ballVelZ -= 2.5;
                }

                rallyLength = Math.max(rallyLength, 1);
                playerSwinging = false;
            }
        }
    }

    private void checkOpponentPaddleHit() {
        double dz = ballZ - (TABLE_LENGTH - 80);
        if (dz < 50 && dz > -80 && ballVelZ > 0) {
            double distX = ballX - opponentPaddleX;
            double distY = ballY - 60;
            double horizontalDist = Math.sqrt(distX*distX + distY*distY);

            if (horizontalDist < PADDLE_WIDTH/2 + BALL_RADIUS + 25 &&
                    ballY < 220) {

                ballVelZ = 5.2 + Math.abs(ballVelZ)*0.35;
                ballVelY = -Math.abs(ballVelY)*0.25 - 3.8;

                // Opponent "aims" a bit randomly
                double error = (random.nextDouble()-0.5) * (80 - opponentScore*1.2);
                ballVelX = (opponentTargetX - ballX) * 0.09 + error*0.03;

                if (opponentScore > 8) {
                    ballVelZ += 0.8; // gets harder
                }
            }
        }
    }

    private void updateAI() {
        if (!ballInPlay) {
            opponentTargetX = opponentPaddleX;
            return;
        }

        // Predict where ball will land
        double timeToReach = (TABLE_LENGTH - 80 - ballZ) / ballVelZ;
        if (timeToReach < 0) timeToReach = 0.1;

        double predictedX = ballX + ballVelX * timeToReach;
        double difficultyFactor = 0.68 + opponentScore * 0.008;

        opponentTargetX += (predictedX - opponentTargetX) * difficultyFactor;

        // Clamp
        opponentTargetX = Math.max(-TABLE_WIDTH/2 + 80, Math.min(TABLE_WIDTH/2 - 80, opponentTargetX));

        // Move paddle
        opponentPaddleX += (opponentTargetX - opponentPaddleX) * 8.5 * (1.0/60.0);
    }

    private void updatePaddleSwing(double dt) {
        if (playerSwinging) {
            playerPaddleAngle += 380 * dt;
            playerPaddleSwingSpeed = 380;
            if (playerPaddleAngle > 110) {
                playerSwinging = false;
                playerPaddleAngle = 110;
            }
        } else {
            playerPaddleAngle *= 0.84;
            playerPaddleSwingSpeed *= 0.7;
        }
    }

    // Fake sound (just for fun, can be replaced with real audio)
    private void playBounceSoundFake() {
        // in real app → play .wav or use Clip
        System.out.print("\rBOING!   ");
    }

    // =============================================
    //  RENDERING - pseudo-3D projection
    // =============================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2);
        drawTableAndNet(g2);
        drawShadowUnderBall(g2);
        drawBall(g2);
        drawPlayerPaddle(g2);
        drawOpponentPaddle(g2);
        drawScoreAndInfo(g2);
    }

    private void drawBackground(Graphics2D g) {
        GradientPaint grad = new GradientPaint(0, 0, new Color(20,35,70), 0, HEIGHT, new Color(5,10,30));
        g.setPaint(grad);
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawTableAndNet(Graphics2D g) {
        // Table surface
        int[] tableXs = projectPointsX(-TABLE_WIDTH/2, TABLE_WIDTH/2, TABLE_WIDTH/2, -TABLE_WIDTH/2);
        int[] tableYs = projectPointsY(0, 0, TABLE_LENGTH, TABLE_LENGTH);

        g.setColor(DARK_GREEN_TABLE);
        g.fillPolygon(tableXs, tableYs, 4);

        // Light reflection fake
        g.setColor(LIGHT_GREEN_TABLE);
        Polygon shine = new Polygon(
                new int[]{tableXs[0]+60, tableXs[1]-60, tableXs[1]-100, tableXs[0]+100},
                new int[]{tableYs[0]+40, tableYs[1]+40, tableYs[2]+140, tableYs[3]+140},
                4
        );
        g.fill(shine);

        // White lines
        g.setColor(WHITE_LINES);
        g.setStroke(new BasicStroke(6));
        g.drawLine(tableXs[0], tableYs[0], tableXs[1], tableYs[1]); // left
        g.drawLine(tableXs[3], tableYs[3], tableXs[2], tableYs[2]); // right
        g.drawLine(tableXs[0], tableYs[0], tableXs[3], tableYs[3]); // bottom
        g.drawLine(tableXs[1], tableYs[1], tableXs[2], tableYs[2]); // top

        // Center line
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{20,20}, 0));
        g.drawLine((tableXs[0]+tableXs[1])/2, (tableYs[0]+tableYs[1])/2,
                (tableXs[3]+tableXs[2])/2, (tableYs[3]+tableYs[2])/2);

        // Net
        int netZ = TABLE_LENGTH/2;
        int[] netX = projectPointsX(-TABLE_WIDTH/2-10, TABLE_WIDTH/2+10, TABLE_WIDTH/2+10, -TABLE_WIDTH/2-10);
        int[] netYlow  = projectPointsY(TABLE_HEIGHT, TABLE_HEIGHT, TABLE_LENGTH, TABLE_LENGTH);
        int[] netYhigh = projectPointsY(NET_HEIGHT, NET_HEIGHT, TABLE_LENGTH, TABLE_LENGTH);

        g.setColor(NET_COLOR);
        g.setStroke(new BasicStroke(3));
        for (int i = 0; i < 4; i++) {
            g.drawLine(netX[i], netYlow[i], netX[i], netYhigh[i]);
        }
        g.drawPolygon(netX, netYhigh, 4);
    }

    private void drawShadowUnderBall(Graphics2D g) {
        int shadowX = projectX(ballX, ballZ + 10);
        int shadowY = projectY(0, ballZ + 10);
        int shadowSize = (int)(BALL_RADIUS * 2.2 * (1.0 - ballY/800.0));

        g.setColor(SHADOW_COLOR);
        g.fillOval(shadowX - shadowSize/2, shadowY - shadowSize/4, shadowSize, shadowSize/2);
    }

    private void drawBall(Graphics2D g) {
        int bx = projectX(ballX, ballZ);
        int by = projectY(ballY, ballZ);
        int size = (int)(BALL_RADIUS * 2 * (1.4 - ballZ*0.0008));

        // ball body
        g.setColor(BALL_COLOR);
        g.fillOval(bx - size/2, by - size/2, size, size);

        // highlight
        g.setColor(new Color(255,255,255,180));
        g.fillOval(bx - size/3, by - size/3, size/3, size/3);
    }

    private void drawPlayerPaddle(Graphics2D g) {
        AffineTransform old = g.getTransform();

        int px = projectX(playerPaddleX, 80);
        int py = projectY(60, 80);

        g.translate(px, py);
        g.rotate(Math.toRadians(playerPaddleAngle * 0.7), 0, 40);

        // handle
        g.setColor(new Color(80,40,20));
        g.fillRoundRect(-18, 40, 36, 140, 20, 20);

        // rubber surface
        g.setColor(PLAYER_PADDLE);
        g.fillRoundRect(-PADDLE_WIDTH/2, -PADDLE_DEPTH/2, PADDLE_WIDTH, PADDLE_DEPTH*2, 30, 30);

        // edge
        g.setColor(new Color(150,20,20));
        g.setStroke(new BasicStroke(6));
        g.drawRoundRect(-PADDLE_WIDTH/2, -PADDLE_DEPTH/2, PADDLE_WIDTH, PADDLE_DEPTH*2, 30, 30);

        g.setTransform(old);
    }

    private void drawOpponentPaddle(Graphics2D g) {
        AffineTransform old = g.getTransform();

        int px = projectX(opponentPaddleX, TABLE_LENGTH - 80);
        int py = projectY(60, TABLE_LENGTH - 80);

        g.translate(px, py);
        g.rotate(Math.toRadians(-opponentPaddleAngle * 0.4), 0, 40);

        g.setColor(new Color(20,20,80));
        g.fillRoundRect(-18, 40, 36, 140, 20, 20);

        g.setColor(OPPONENT_PADDLE);
        g.fillRoundRect(-PADDLE_WIDTH/2, -PADDLE_DEPTH/2, PADDLE_WIDTH, PADDLE_DEPTH*2, 30, 30);

        g.setColor(new Color(20,20,150));
        g.setStroke(new BasicStroke(6));
        g.drawRoundRect(-PADDLE_WIDTH/2, -PADDLE_DEPTH/2, PADDLE_WIDTH, PADDLE_DEPTH*2, 30, 30);

        g.setTransform(old);
    }

    private void drawScoreAndInfo(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 48));

        String score = playerScore + "  :  " + opponentScore;
        FontMetrics fm = g.getFontMetrics();
        int sw = fm.stringWidth(score);
        g.drawString(score, (WIDTH - sw)/2, 80);

        g.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        String status = playerServing ? "YOU serve" : "Opponent serves";
        if (ballInPlay) status = "Rally: " + rallyLength;
        g.drawString(status, (WIDTH - g.getFontMetrics().stringWidth(status))/2, 130);

        g.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        g.drawString("Move mouse or ← → keys", 30, HEIGHT-40);
        g.drawString("Click / SPACE = swing!", WIDTH-280, HEIGHT-40);
    }

    // =============================================
    //  Very naive 3D → 2D projection
    // =============================================
    private int projectX(double worldX, double worldZ) {
        double scale = 800.0 / (worldZ + 900);
        return (int)(WIDTH/2 + worldX * scale * 1.1);
    }

    private int projectY(double worldY, double worldZ) {
        double scale = 800.0 / (worldZ + 900);
        return (int)(HEIGHT - 140 - worldY * scale * 1.4);
    }

    private int[] projectPointsX(double x1, double x2, double x3, double x4) {
        return new int[]{
                projectX(x1, 0),
                projectX(x2, 0),
                projectX(x3, TABLE_LENGTH),
                projectX(x4, TABLE_LENGTH)
        };
    }

    private int[] projectPointsY(double y1, double y2, double z3, double z4) {
        return new int[]{
                projectY(y1, 0),
                projectY(y2, 0),
                projectY(y1, z3),
                projectY(y2, z4)
        };
    }

    // =============================================
    //  INPUT HANDLING
    // =============================================
    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        playerPaddleX = (mouseX - WIDTH/2.0) * 1.6;
        playerPaddleX = Math.max(-TABLE_WIDTH/2 + 100, Math.min(TABLE_WIDTH/2 - 100, playerPaddleX));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A)    keyLeft = true;
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D)   keyRight = true;
        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_UP)  keySpace = true;

        if (keySpace && !ballInPlay) {
            ballInPlay = true;
        }
        if (keySpace && !playerSwinging) {
            playerSwinging = true;
            playerPaddleAngle = 0;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A)    keyLeft = false;
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D)   keyRight = false;
        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_UP)  keySpace = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // =============================================
    //  Keyboard movement (alternative to mouse)
    // =============================================
    private void handleKeyboardMovement() {
        if (keyLeft)  playerPaddleX -= 14;
        if (keyRight) playerPaddleX += 14;

        playerPaddleX = Math.max(-TABLE_WIDTH/2 + 100, Math.min(TABLE_WIDTH/2 - 100, playerPaddleX));
    }
}