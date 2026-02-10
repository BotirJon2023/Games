import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Random;
import java.awt.geom.RoundRectangle2D;

public class TableTennisGame extends JPanel implements ActionListener, KeyListener {

    private static final int WIDTH = 1100;
    private static final int HEIGHT = 700;
    private static final int FPS = 60;
    private static final int DELAY = 1000 / FPS;

    // Colors
    private static final Color DARK_GREEN   = new Color(0, 100, 0);
    private static final Color TABLE_GREEN  = new Color(10, 140, 10);
    private static final Color LINE_WHITE  = new Color(240, 240, 240);
    private static final Color BALL_ORANGE  = new Color(255, 140, 0);
    private static final Color PADDLE_BLUE  = new Color(40, 80, 220);
    private static final Color PADDLE_RED   = new Color(220, 40, 60);
    private static final Color SCORE_COLOR  = new Color(255, 255, 200);
    private static final Color SHADOW       = new Color(0, 0, 0, 90);

    // Game objects sizes
    private static final int BALL_DIAMETER   = 38;
    private static final int PADDLE_WIDTH    = 18;
    private static final int PADDLE_HEIGHT   = 110;
    private static final int NET_HEIGHT      = 160;
    private static final int NET_THICKNESS   = 8;

    // Game rules
    private static final int WIN_SCORE       = 11;
    private static final int SCORE_TO_WIN_SET = 2;   // best of 3,5,... but we use difference
    private static final float MAX_BALL_SPEED = 14.8f;
    private static final float MIN_BALL_SPEED = 4.2f;

    // =============================================================
    //   GAME STATE VARIABLES
    // =============================================================
    private Timer timer;

    // Positions & velocities
    private float ballX, ballY;
    private float ballVelX, ballVelY;
    private float paddleLeftY, paddleRightY;

    // Scores & sets
    private int scoreLeft   = 0;
    private int scoreRight  = 0;
    private int setsLeft    = 0;
    private int setsRight   = 0;

    // Game phases
    private boolean gameRunning   = false;
    private boolean showStartMenu = true;
    private boolean showGameOver  = false;
    private boolean paused        = false;

    // Input flags
    private boolean upPressed     = false;
    private boolean downPressed   = false;
    private boolean wPressed      = false;
    private boolean sPressed      = false;

    // Animation / effects
    private float ballSpin        = 0f;     // visual only
    private int   lastHitPaddle   = 0;      // 1=left, 2=right
    private float hitFlashTimer   = 0;
    private int   rallyLength     = 0;
    private int   maxRally        = 0;

    // Particle system (very simple)
    private static final int MAX_PARTICLES = 120;
    private float[] particleX   = new float[MAX_PARTICLES];
    private float[] particleY   = new float[MAX_PARTICLES];
    private float[] particleVX  = new float[MAX_PARTICLES];
    private float[] particleVY  = new float[MAX_PARTICLES];
    private float[] particleLife= new float[MAX_PARTICLES];
    private int     particleCount = 0;

    // Trail effect
    private static final int TRAIL_LENGTH = 12;
    private float[] trailX = new float[TRAIL_LENGTH];
    private float[] trailY = new float[TRAIL_LENGTH];
    private int trailIndex = 0;

    // Random
    private final Random rand = new Random();

    // =============================================================
    //   MAIN METHOD
    // =============================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Table Tennis – Classic 2D");
            TableTennisGame game = new TableTennisGame();
            frame.add(game);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // =============================================================
    //   CONSTRUCTOR
    // =============================================================
    public TableTennisGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        resetGame();

        timer = new Timer(DELAY, this);
        timer.start();
    }

    private void resetGame() {
        scoreLeft = 0;
        scoreRight = 0;
        setsLeft = 0;
        setsRight = 0;
        resetRound();
        gameRunning = false;
        showStartMenu = true;
        showGameOver = false;
        paused = false;
        rallyLength = 0;
        maxRally = 0;
        particleCount = 0;
    }

    private void resetRound() {
        ballX = WIDTH / 2f;
        ballY = HEIGHT / 2f;
        ballSpin = 0;

        // serve direction alternates – simple version
        boolean leftServes = (scoreLeft + scoreRight) % 2 == 0;
        float serveSpeed = 5.8f + rand.nextFloat() * 1.4f;

        if (leftServes) {
            ballVelX = serveSpeed;
            ballVelY = (rand.nextBoolean() ? 1 : -1) * (2.0f + rand.nextFloat() * 3.2f);
        } else {
            ballVelX = -serveSpeed;
            ballVelY = (rand.nextBoolean() ? 1 : -1) * (2.0f + rand.nextFloat() * 3.2f);
        }

        paddleLeftY  = HEIGHT / 2f - PADDLE_HEIGHT / 2f;
        paddleRightY = HEIGHT / 2f - PADDLE_HEIGHT / 2f;

        trailIndex = 0;
        for (int i = 0; i < TRAIL_LENGTH; i++) {
            trailX[i] = ballX;
            trailY[i] = ballY;
        }

        hitFlashTimer = 0;
        lastHitPaddle = 0;
        rallyLength = 0;
    }

    // =============================================================
    //   GAME LOOP (ActionListener)
    // =============================================================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning || paused || showStartMenu || showGameOver) {
            repaint();
            return;
        }

        updatePhysics();
        updateParticles();
        updateTrail();

        if (hitFlashTimer > 0) hitFlashTimer -= 0.08f;

        repaint();
    }

    private void updatePhysics() {
        // ───────────────────────────────────────────────
        // Move paddles
        // ───────────────────────────────────────────────
        float paddleSpeed = 9.2f;

        if (wPressed) paddleLeftY -= paddleSpeed;
        if (sPressed) paddleLeftY += paddleSpeed;
        if (upPressed)   paddleRightY -= paddleSpeed;
        if (downPressed) paddleRightY += paddleSpeed;

        // Keep paddles inside screen
        paddleLeftY  = Math.max(20, Math.min(HEIGHT - PADDLE_HEIGHT - 20, paddleLeftY));
        paddleRightY = Math.max(20, Math.min(HEIGHT - PADDLE_HEIGHT - 20, paddleRightY));

        // ───────────────────────────────────────────────
        // Move ball
        // ───────────────────────────────────────────────
        ballX += ballVelX;
        ballY += ballVelY;

        // Spin visual effect (fake)
        ballSpin += ballVelX * 0.08f;

        // ───────────────────────────────────────────────
        // Wall bounce – top & bottom
        // ───────────────────────────────────────────────
        if (ballY < BALL_DIAMETER/2f || ballY > HEIGHT - BALL_DIAMETER/2f) {
            ballVelY = -ballVelY * 1.02f;   // very small speedup
            ballY = Math.max(BALL_DIAMETER/2f, Math.min(HEIGHT - BALL_DIAMETER/2f, ballY));
            spawnParticles(ballX, ballY, 12, 2.8f, new Color(220,220,255,160));
        }

        // ───────────────────────────────────────────────
        // Paddle collision – left paddle
        // ───────────────────────────────────────────────
        if (ballVelX < 0 && ballX - BALL_DIAMETER/2f <= 80 + PADDLE_WIDTH) {
            if (ballY + BALL_DIAMETER/2f >= paddleLeftY &&
                    ballY - BALL_DIAMETER/2f <= paddleLeftY + PADDLE_HEIGHT) {

                float hitPos = (ballY - (paddleLeftY + PADDLE_HEIGHT/2f)) / (PADDLE_HEIGHT/2f);
                hitPos = Math.max(-1f, Math.min(1f, hitPos));

                float speed = (float) Math.hypot(ballVelX, ballVelY);
                speed = Math.min(MAX_BALL_SPEED, speed * 1.06f);

                float angle = hitPos * 1.1f; // radians-ish
                ballVelX =  Math.abs(speed * (float)Math.cos(angle));
                ballVelY = -speed * (float)Math.sin(angle);

                lastHitPaddle = 1;
                hitFlashTimer = 1.4f;
                rallyLength++;
                if (rallyLength > maxRally) maxRally = rallyLength;

                spawnParticles(ballX, ballY, 28, 5.2f, PADDLE_BLUE);

                // spin visual
                ballSpin += 12 + Math.abs(hitPos * 18);
            }
        }

        // ───────────────────────────────────────────────
        // Paddle collision – right paddle
        // ───────────────────────────────────────────────
        if (ballVelX > 0 && ballX + BALL_DIAMETER/2f >= WIDTH - 80 - PADDLE_WIDTH) {
            if (ballY + BALL_DIAMETER/2f >= paddleRightY &&
                    ballY - BALL_DIAMETER/2f <= paddleRightY + PADDLE_HEIGHT) {

                float hitPos = (ballY - (paddleRightY + PADDLE_HEIGHT/2f)) / (PADDLE_HEIGHT/2f);
                hitPos = Math.max(-1f, Math.min(1f, hitPos));

                float speed = (float) Math.hypot(ballVelX, ballVelY);
                speed = Math.min(MAX_BALL_SPEED, speed * 1.06f);

                float angle = hitPos * 1.1f;
                ballVelX = -Math.abs(speed * (float)Math.cos(angle));
                ballVelY = -speed * (float)Math.sin(angle);

                lastHitPaddle = 2;
                hitFlashTimer = 1.4f;
                rallyLength++;
                if (rallyLength > maxRally) maxRally = rallyLength;

                spawnParticles(ballX, ballY, 28, 5.2f, PADDLE_RED);

                ballSpin -= 12 + Math.abs(hitPos * 18);
            }
        }

        // ───────────────────────────────────────────────
        // Score – ball out left / right
        // ───────────────────────────────────────────────
        if (ballX < -40) {
            scoreRight++;
            pointScored(2);
        }
        else if (ballX > WIDTH + 40) {
            scoreLeft++;
            pointScored(1);
        }
    }

    private void pointScored(int winner) {
        if (winner == 1) {
            spawnParticles(WIDTH/2f - 80, HEIGHT/2f, 60, 7.5f, PADDLE_BLUE.brighter());
        } else {
            spawnParticles(WIDTH/2f + 80, HEIGHT/2f, 60, 7.5f, PADDLE_RED.brighter());
        }

        if (scoreLeft >= WIN_SCORE || scoreRight >= WIN_SCORE) {
            if (Math.abs(scoreLeft - scoreRight) >= 2) {
                if (scoreLeft > scoreRight) setsLeft++;
                else                         setsRight++;

                if (setsLeft >= SCORE_TO_WIN_SET || setsRight >= SCORE_TO_WIN_SET) {
                    showGameOver = true;
                    gameRunning = false;
                } else {
                    resetRound();
                    scoreLeft = 0;
                    scoreRight = 0;
                }
            } else {
                // deuce / advantage – continue
            }
        } else {
            resetRound();
        }
    }

    // =============================================================
    //   PARTICLE SYSTEM – very simple
    // =============================================================
    private void spawnParticles(float x, float y, int count, float maxSpd, Color baseColor) {
        for (int i = 0; i < count && particleCount < MAX_PARTICLES; i++) {
            int idx = particleCount++;
            particleX[idx]  = x;
            particleY[idx]  = y;
            float angle = rand.nextFloat() * (float)(Math.PI * 2);
            float spd   = 1.5f + rand.nextFloat() * maxSpd;
            particleVX[idx] = (float)Math.cos(angle) * spd;
            particleVY[idx] = (float)Math.sin(angle) * spd;
            particleLife[idx] = 0.6f + rand.nextFloat() * 0.9f;

            // sometimes bigger "spark"
            if (rand.nextInt(5) == 0) {
                particleVX[idx] *= 1.6f;
                particleVY[idx] *= 1.6f;
                particleLife[idx] *= 1.4f;
            }
        }
    }

    private void updateParticles() {
        for (int i = 0; i < particleCount; i++) {
            particleX[i]  += particleVX[i];
            particleY[i]  += particleVY[i];
            particleLife[i] -= 0.024f;

            // gravity lite
            particleVY[i] += 0.14f;

            if (particleLife[i] <= 0) {
                // remove by swapping with last
                particleCount--;
                particleX[i]   = particleX[particleCount];
                particleY[i]   = particleY[particleCount];
                particleVX[i]  = particleVX[particleCount];
                particleVY[i]  = particleVY[particleCount];
                particleLife[i]= particleLife[particleCount];
                i--; // check same index again
            }
        }
    }

    private void updateTrail() {
        trailX[trailIndex] = ballX;
        trailY[trailIndex] = ballY;
        trailIndex = (trailIndex + 1) % TRAIL_LENGTH;
    }

    // =============================================================
    //   RENDERING
    // =============================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawTable(g2);
        drawNetAndCenterLine(g2);

        if (showStartMenu) {
            drawStartMenu(g2);
        }
        else if (showGameOver) {
            drawGameOver(g2);
        }
        else {
            drawParticles(g2);
            drawTrail(g2);
            drawBall(g2);
            drawPaddles(g2);
            drawScoresAndInfo(g2);
            if (paused) drawPausedOverlay(g2);
        }
    }

    private void drawTable(Graphics2D g2) {
        // gradient background
        GradientPaint grad = new GradientPaint(0, 0, new Color(0,40,0), 0, HEIGHT, new Color(0,90,0));
        g2.setPaint(grad);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // main table
        g2.setColor(TABLE_GREEN);
        g2.fillRoundRect(40, 60, WIDTH-80, HEIGHT-120, 40, 40);

        // border glow
        g2.setStroke(new BasicStroke(14));
        g2.setColor(new Color(255,255,180,70));
        g2.drawRoundRect(45, 65, WIDTH-90, HEIGHT-130, 36, 36);
    }

    private void drawNetAndCenterLine(Graphics2D g2) {
        // center line
        g2.setColor(LINE_WHITE);
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{18,12}, 0));
        g2.drawLine(WIDTH/2, 70, WIDTH/2, HEIGHT-70);

        // net
        g2.setStroke(new BasicStroke(NET_THICKNESS));
        g2.setColor(new Color(240,240,240));
        g2.drawLine(WIDTH/2, HEIGHT/2 - NET_HEIGHT/2, WIDTH/2, HEIGHT/2 + NET_HEIGHT/2);

        // net top white band
        g2.setStroke(new BasicStroke(14));
        g2.setColor(new Color(255,255,220));
        g2.drawLine(WIDTH/2-6, HEIGHT/2 - NET_HEIGHT/2, WIDTH/2+6, HEIGHT/2 - NET_HEIGHT/2);
    }

    private void drawBall(Graphics2D g2) {
        // shadow
        g2.setColor(SHADOW);
        g2.fillOval((int)(ballX - BALL_DIAMETER*0.5f + 6),
                (int)(ballY - BALL_DIAMETER*0.5f + 10),
                BALL_DIAMETER, BALL_DIAMETER);

        // ball body
        g2.setColor(BALL_ORANGE);
        g2.fillOval((int)(ballX - BALL_DIAMETER/2f),
                (int)(ballY - BALL_DIAMETER/2f),
                BALL_DIAMETER, BALL_DIAMETER);

        // shine
        g2.setColor(new Color(255,255,255,180));
        g2.fillOval((int)(ballX - BALL_DIAMETER*0.38f),
                (int)(ballY - BALL_DIAMETER*0.38f),
                BALL_DIAMETER/3, BALL_DIAMETER/3);

        // spin lines (fake)
        g2.setStroke(new BasicStroke(3.2f));
        g2.setColor(new Color(255,220,160,140));
        int spinDeg = (int)(ballSpin * 8) % 360;
        for (int i = 0; i < 4; i++) {
            int a = spinDeg + i*90;
            int x1 = (int)(ballX + Math.cos(Math.toRadians(a))   * BALL_DIAMETER*0.38f);
            int y1 = (int)(ballY + Math.sin(Math.toRadians(a))   * BALL_DIAMETER*0.38f);
            int x2 = (int)(ballX + Math.cos(Math.toRadians(a+25))* BALL_DIAMETER*0.44f);
            int y2 = (int)(ballY + Math.sin(Math.toRadians(a+25))* BALL_DIAMETER*0.44f);
            g2.drawLine(x1,y1,x2,y2);
        }

        // hit flash
        if (hitFlashTimer > 0.1f) {
            float alpha = hitFlashTimer * 0.7f;
            g2.setColor(new Color(255,255,180,(int)(alpha*255)));
            g2.setStroke(new BasicStroke(8 + hitFlashTimer*12));
            g2.drawOval((int)(ballX - BALL_DIAMETER*0.8f),
                    (int)(ballY - BALL_DIAMETER*0.8f),
                    (int)(BALL_DIAMETER*1.6f), (int)(BALL_DIAMETER*1.6f));
        }
    }

    private void drawPaddles(Graphics2D g2) {
        // Left paddle shadow
        g2.setColor(SHADOW);
        RoundRectangle2D leftShadow = new RoundRectangle2D.Float(
                70, paddleLeftY + 8, PADDLE_WIDTH, PADDLE_HEIGHT, 24, 24);
        g2.fill(leftShadow);

        // Right paddle shadow
        RoundRectangle2D rightShadow = new RoundRectangle2D.Float(
                WIDTH - 70 - PADDLE_WIDTH, paddleRightY + 8, PADDLE_WIDTH, PADDLE_HEIGHT, 24, 24);
        g2.fill(rightShadow);

        // Left paddle
        g2.setColor(PADDLE_BLUE);
        RoundRectangle2D leftPaddle = new RoundRectangle2D.Float(
                80, paddleLeftY, PADDLE_WIDTH, PADDLE_HEIGHT, 28, 28);
        g2.fill(leftPaddle);

        // Right paddle
        g2.setColor(PADDLE_RED);
        RoundRectangle2D rightPaddle = new RoundRectangle2D.Float(
                WIDTH - 80 - PADDLE_WIDTH, paddleRightY, PADDLE_WIDTH, PADDLE_HEIGHT, 28, 28);
        g2.fill(rightPaddle);

        // Glow when hit
        if (hitFlashTimer > 0.2f && lastHitPaddle == 1) {
            g2.setColor(new Color(120,180,255, (int)(hitFlashTimer*140)));
            g2.setStroke(new BasicStroke(14));
            g2.draw(leftPaddle);
        }
        if (hitFlashTimer > 0.2f && lastHitPaddle == 2) {
            g2.setColor(new Color(255,120,140, (int)(hitFlashTimer*140)));
            g2.setStroke(new BasicStroke(14));
            g2.draw(rightPaddle);
        }
    }

    private void drawTrail(Graphics2D g2) {
        for (int i = 0; i < TRAIL_LENGTH; i++) {
            int idx = (trailIndex - 1 - i + TRAIL_LENGTH) % TRAIL_LENGTH;
            float size = BALL_DIAMETER * (0.4f + 0.6f * (TRAIL_LENGTH - i)/(float)TRAIL_LENGTH);
            int alpha = (int)(40 + 110 * (TRAIL_LENGTH - i)/(float)TRAIL_LENGTH);

            g2.setColor(new Color(255, 180, 60, alpha));
            g2.fillOval((int)(trailX[idx] - size/2), (int)(trailY[idx] - size/2), (int)size, (int)size);
        }
    }

    private void drawParticles(Graphics2D g2) {
        for (int i = 0; i < particleCount; i++) {
            float life = particleLife[i];
            if (life <= 0) continue;

            int alpha = (int)(life * 220);
            int r = (int)(220 + 35 * (1-life));
            int g = (int)(180 + 60 * (1-life));
            int b = (int)(100 + 100 * (1-life));

            g2.setColor(new Color(r, g, b, alpha));
            float sz = 3.2f + life * 5.4f;
            g2.fillOval((int)(particleX[i] - sz/2), (int)(particleY[i] - sz/2), (int)sz, (int)sz);
        }
    }

    private void drawScoresAndInfo(Graphics2D g2) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 72));
        g2.setColor(SCORE_COLOR);

        String leftScore  = String.valueOf(scoreLeft);
        String rightScore = String.valueOf(scoreRight);

        FontMetrics fm = g2.getFontMetrics();
        int w1 = fm.stringWidth(leftScore);
        int w2 = fm.stringWidth(rightScore);

        g2.drawString(leftScore,  WIDTH/2 - 140 - w1, 140);
        g2.drawString(rightScore, WIDTH/2 + 140      , 140);

        // sets
        g2.setFont(new Font("SansSerif", Font.BOLD, 36));
        g2.drawString(setsLeft  + " sets", WIDTH/2 - 220, 60);
        g2.drawString(setsRight + " sets", WIDTH/2 + 120, 60);

        // rally counter
        if (rallyLength >= 4) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 28));
            g2.setColor(new Color(255, 240, 180, 180));
            String rallyText = "RALLY: " + rallyLength;
            g2.drawString(rallyText, WIDTH/2 - g2.getFontMetrics().stringWidth(rallyText)/2, HEIGHT - 60);
        }

        // controls reminder (small)
        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g2.setColor(new Color(220,220,220,140));
        g2.drawString("W/S ← Left player     ↑/↓ → Right player     P – pause     R – restart", 20, HEIGHT-20);
    }

    private void drawStartMenu(Graphics2D g2) {
        g2.setColor(new Color(0,0,0,160));
        g2.fillRect(0,0,WIDTH,HEIGHT);

        g2.setFont(new Font("SansSerif", Font.BOLD, 88));
        g2.setColor(new Color(255,240,100));
        String title = "TABLE TENNIS";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, WIDTH/2 - fm.stringWidth(title)/2, 180);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 42));
        g2.setColor(Color.WHITE);
        g2.drawString("Press SPACE to start", WIDTH/2 - 220, 340);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 28));
        g2.drawString("First to " + WIN_SCORE + " points • Best of " + (SCORE_TO_WIN_SET*2-1), WIDTH/2 - 220, 420);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g2.setColor(new Color(200,220,255));
        g2.drawString("Controls:  W / S     ← Left player", 180, 520);
        g2.drawString("           ↑ / ↓     ← Right player", 180, 560);
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0,0,0,180));
        g2.fillRect(0,0,WIDTH,HEIGHT);

        g2.setFont(new Font("SansSerif", Font.BOLD, 92));
        g2.setColor(new Color(255,220,80));
        String winner = (setsLeft > setsRight) ? "LEFT WINS!" : "RIGHT WINS!";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(winner, WIDTH/2 - fm.stringWidth(winner)/2, 220);

        g2.setFont(new Font("SansSerif", Font.BOLD, 54));
        g2.setColor(SCORE_COLOR);
        g2.drawString(setsLeft + " : " + setsRight, WIDTH/2 - 100, 340);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 36));
        g2.setColor(new Color(220,240,255));
        g2.drawString("Longest rally: " + maxRally, WIDTH/2 - 160, 420);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 32));
        g2.setColor(Color.WHITE);
        g2.drawString("Press R to restart   or   ESC to quit", WIDTH/2 - 260, 520);
    }

    private void drawPausedOverlay(Graphics2D g2) {
        g2.setColor(new Color(0,0,0,140));
        g2.fillRect(0,0,WIDTH,HEIGHT);

        g2.setFont(new Font("SansSerif", Font.BOLD, 88));
        g2.setColor(new Color(180,220,255));
        String txt = "PAUSED";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(txt, WIDTH/2 - fm.stringWidth(txt)/2, HEIGHT/2 + 20);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 32));
        g2.drawString("Press P to continue", WIDTH/2 - 160, HEIGHT/2 + 100);
    }

    // =============================================================
    //   INPUT
    // =============================================================
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_ESCAPE) {
            System.exit(0);
        }

        if (showStartMenu || showGameOver) {
            if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_ENTER) {
                if (showStartMenu) {
                    showStartMenu = false;
                    gameRunning = true;
                    resetGame();
                }
                if (showGameOver) {
                    resetGame();
                    showGameOver = false;
                    showStartMenu = true;
                }
            }
            if (showGameOver && key == KeyEvent.VK_R) {
                resetGame();
                showGameOver = false;
                showStartMenu = true;
            }
            return;
        }

        switch (key) {
            case KeyEvent.VK_W -> wPressed = true;
            case KeyEvent.VK_S -> sPressed = true;
            case KeyEvent.VK_UP -> upPressed = true;
            case KeyEvent.VK_DOWN -> downPressed = true;
            case KeyEvent.VK_P -> paused = !paused;
            case KeyEvent.VK_R -> {
                resetGame();
                showStartMenu = true;
            }
            case KeyEvent.VK_SPACE -> {
                if (paused) paused = false;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        switch (key) {
            case KeyEvent.VK_W -> wPressed = false;
            case KeyEvent.VK_S -> sPressed = false;
            case KeyEvent.VK_UP -> upPressed = false;
            case KeyEvent.VK_DOWN -> downPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}