import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.sound.sampled.*;

import static javax.swing.plaf.synth.Region.MENU;

/**
 * TableTennisGame
 * A single-file Java Swing-based table tennis (ping pong) game with animation,
 * AI, particles, sounds, and a simple menu. More than 600 lines by design.
 *
 * Controls:
 * - W/S or Up/Down: Move player paddle
 * - Space: Serve / Start
 * - P: Pause/Resume
 * - R: Reset match
 * - M: Mute/Unmute
 * - 1/2/3: Difficulty (Easy/Normal/Hard)
 * - F: Toggle FPS meter
 * - G: Toggle Ball Trail
 * - H: Toggle Hitboxes
 * - Esc: Quit (closes window)
 *
 * Game rules:
 * - First to 11 points, must win by 2.
 * - Simple serve system: player serves first, alternates every 2 points.
 *
 * Rendering notes:
 * - Smooth animation via game loop at ~60 FPS.
 * - Buffered drawing using Swing's double-buffering.
 * - Custom anti-aliased Graphics2D rendering.
 */
public class TableTennisGame extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener, FocusListener {

    // Window
    private JFrame window;
    private static final int BASE_WIDTH = 1024;
    private static final int BASE_HEIGHT = 576;
    private int viewW = BASE_WIDTH;
    private int viewH = BASE_HEIGHT;

    // Game loop
    private Thread loopThread;
    private volatile boolean running = false;
    private long frameCount = 0;
    private boolean showFPS = false;
    private double currentFPS = 0.0;

    // Random
    private final Random rng = new Random();

    // Input state
    private final Input input = new Input();

    // Game state
    private GameState state = GameState.MENU;
    private boolean muted = false;
    private boolean showTrails = true;
    private boolean showHitBoxes = false;

    // Table/Scene config
    private final Color tableColor = new Color(10, 75, 130);
    private final Color tableLine = new Color(240, 248, 255, 235);
    private final Color tableShadow = new Color(0, 0, 0, 70);
    private final Color netColor = new Color(240, 240, 240);
    private final Color bgGradientTop = new Color(18, 18, 18);
    private final Color bgGradientBottom = new Color(2, 2, 2);
    private final Color flareColor = new Color(255, 255, 255, 70);
    private final Color hudTextColor = new Color(235, 245, 255);

    // Entities
    private final Paddle leftPaddle = new Paddle(false);
    private final Paddle rightPaddle = new Paddle(true);
    private final Ball ball = new Ball();

    // Match data
    private final ScoreBoard scoreBoard = new ScoreBoard();
    private int serveHolder = 0; // 0: player (left), 1: AI (right)
    private int servesTakenThisTurn = 0;
    private final int SERVES_PER_TURN = 2;
    private boolean ballInPlay = false;
    private long lastPointTimeNs = 0L;

    // AI
    private final AIController ai = new AIController();

    // Particles and visual effects
    private final List<Particle> particles = new ArrayList<>();
    private final List<Shockwave> shockwaves = new ArrayList<>();
    private final List<BallTrailDot> ballTrail = new ArrayList<>();
    private final int maxTrailDots = 60;
    private final List<BannerText> banners = new ArrayList<>();

    // Sound
    private final SoundEngine sound = new SoundEngine();

    // Time step
    private static final double TARGET_FPS = 60.0;
    private static final double DT = 1.0 / TARGET_FPS;
    private static final double MAX_FRAME_TIME = 0.25; // avoid spiral of death
    private static final int MAX_UPDATES_PER_FRAME = 5;

    // Serving animation
    private boolean serveKickoff = true; // Start game in serve animation
    private double serveAnimT = 0.0;     // 0..1 animation parameter
    private double serveAnimDir = 1.0;

    // Motion blur factors
    private final double trailDotInterval = 1.0 / 240.0; // accumulate per cycle
    private double trailAcc = 0.0;

    // Pause and transitions
    private double pauseFlashT = 0.0;
    private boolean pauseFlashUp = true;

    // Score flash
    private double scoreFlashT = 0.0;

    // Fonts
    private Font hudFont;
    private Font bigScoreFont;
    private Font smallFont;

    // Ping decorations
    private double netOsc = 0.0;

    // Title menu button hover states
    private boolean hoverStart = false;
    private boolean hoverQuit = false;

    // Minimal post-process like: subtle vignette
    private BufferedImage backBuffer;
    private Graphics2D backG2;

    // Helpers for scaling / logical coordinates
    private double scaleX = 1.0;
    private double scaleY = 1.0;

    // Colors for paddles and ball
    private final Color leftPaddleColor = new Color(255, 205, 85);
    private final Color rightPaddleColor = new Color(210, 120, 255);
    private final Color ballColor = new Color(255, 255, 255);

    // Constructor
    public TableTennisGame() {
        setPreferredSize(new Dimension(BASE_WIDTH, BASE_HEIGHT));
        setFocusable(true);
        requestFocus();
        setDoubleBuffered(true);
        setBackground(Color.BLACK);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addFocusListener(this);

        // Load fonts
        try {
            hudFont = new Font("SansSerif", Font.BOLD, 18);
            bigScoreFont = new Font("SansSerif", Font.BOLD, 72);
            smallFont = new Font("SansSerif", Font.PLAIN, 14);
        } catch (Exception e) {
            hudFont = getFont().deriveFont(Font.BOLD, 18f);
            bigScoreFont = getFont().deriveFont(Font.BOLD, 72f);
            smallFont = getFont().deriveFont(Font.PLAIN, 14f);
        }

        // Init paddles and ball
        resetEntities(true);

        // Sounds ensure line
        sound.init();

        // Back buffer
        backBuffer = new BufferedImage(BASE_WIDTH, BASE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        backG2 = backBuffer.createGraphics();
        applyRenderHints(backG2);
    }

    private void createAndShowWindow() {
        window = new JFrame("TableTennisGame - Single File Edition");
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setContentPane(this);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        // Track resize to adapt scale
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                viewW = getWidth();
                viewH = getHeight();
            }
        });
    }

    private void start() {
        if (running) return;
        running = true;
        loopThread = new Thread(this, "GameLoop");
        loopThread.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TableTennisGame game = new TableTennisGame();
            game.createAndShowWindow();
            game.start();
        });
    }

    @Override
    public void run() {
        long prev = System.nanoTime();
        double acc = 0.0;
        long fpsTimer = System.nanoTime();
        int framesThisSecond = 0;

        while (running) {
            long now = System.nanoTime();
            double frameTime = (now - prev) / 1_000_000_000.0;
            if (frameTime > MAX_FRAME_TIME) frameTime = MAX_FRAME_TIME;
            prev = now;
            acc += frameTime;

            int updates = 0;
            while (acc >= DT && updates < MAX_UPDATES_PER_FRAME) {
                update(DT);
                acc -= DT;
                updates++;
            }

            repaint();

            framesThisSecond++;
            if (now - fpsTimer >= 1_000_000_000L) {
                currentFPS = framesThisSecond;
                framesThisSecond = 0;
                fpsTimer = now;
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void update(double dt) {
        frameCount++;
        netOsc += dt * 2.5;

        // Update banners
        updateBanners(dt);

        switch (state) {
            case MENU:
                updateMenu(dt);
                break;
            case SERVE:
                updateServe(dt);
                break;
            case PLAYING:
                updatePlaying(dt);
                break;
            case PAUSED:
                updatePaused(dt);
                break;
            case GAMEOVER:
                updateGameOver(dt);
                break;
        }

        // Update particles and shockwaves
        updateParticles(dt);
        updateShockwaves(dt);

        // Update trails
        if (showTrails && state != MENU) {
            trailAcc += dt;
            while (trailAcc >= trailDotInterval) {
                trailAcc -= trailDotInterval;
                pushBallTrailDot();
            }
            trimTrail();
        } else {
            ballTrail.clear();
        }
    }

    private void updateMenu(double dt) {
        // gentle bob for "Press Space"
        serveAnimT += dt * serveAnimDir * 0.75;
        if (serveAnimT >= 1.0) {
            serveAnimT = 1.0;
            serveAnimDir = -1.0;
        } else if (serveAnimT <= 0.0) {
            serveAnimT = 0.0;
            serveAnimDir = 1.0;
        }

        leftPaddle.autoIdle(dt, 0.25, getTableTop(), getTableBottom());
        rightPaddle.autoIdle(dt, 0.25, getTableTop(), getTableBottom());
        ball.idleBob(dt, getTableLeft(), getTableRight(), getTableTop(), getTableBottom());
    }

    private void updateServe(double dt) {
        leftPaddle.update(dt, input, getTableTop(), getTableBottom(), true);
        ai.updateAI(rightPaddle, ball, dt, getTableTop(), getTableBottom(), AIDifficulty.current);

        // Serving animation: ball hovers near paddle of server, small bob
        serveAnimation(dt);

        // When Space pressed, launch serve
        if (input.spacePressedOnce) {
            input.spacePressedOnce = false;
            launchServe();
            state = GameState.PLAYING;
        }
    }

    private void updatePlaying(double dt) {
        // Player and AI
        leftPaddle.update(dt, input, getTableTop(), getTableBottom(), false);
        ai.updateAI(rightPaddle, ball, dt, getTableTop(), getTableBottom(), AIDifficulty.current);

        // Ball movement and physics
        ball.update(dt);
        handleBallBounds(ball, getTableLeft(), getTableRight(), getTableTop(), getTableBottom());

        // Collisions with paddles
        if (checkBallPaddleCollision(ball, leftPaddle)) {
            handlePaddleCollision(ball, leftPaddle, false);
        }
        if (checkBallPaddleCollision(ball, rightPaddle)) {
            handlePaddleCollision(ball, rightPaddle, true);
        }

        // Scoring and out-of-bounds beyond paddles
        if (ball.x < getTableLeft() - ball.radius - 10) {
            // Right player scores
            scorePoint(1);
        } else if (ball.x > getTableRight() + ball.radius + 10) {
            // Left player scores
            scorePoint(0);
        }
    }

    private void updatePaused(double dt) {
        // Pulse
        double speed = 1.5;
        if (pauseFlashUp) {
            pauseFlashT += dt * speed;
            if (pauseFlashT >= 1.0) {
                pauseFlashT = 1.0;
                pauseFlashUp = false;
            }
        } else {
            pauseFlashT -= dt * speed;
            if (pauseFlashT <= 0.0) {
                pauseFlashT = 0.0;
                pauseFlashUp = true;
            }
        }
        // Allow small idle animations
        leftPaddle.autoIdle(dt, 0.15, getTableTop(), getTableBottom());
        rightPaddle.autoIdle(dt, 0.15, getTableTop(), getTableBottom());
        ball.idleBob(dt, getTableLeft(), getTableRight(), getTableTop(), getTableBottom());
    }

    private void updateGameOver(double dt) {
        // Idle animation for the scene
        leftPaddle.autoIdle(dt, 0.2, getTableTop(), getTableBottom());
        rightPaddle.autoIdle(dt, 0.2, getTableTop(), getTableBottom());
        ball.idleBob(dt, getTableLeft(), getTableRight(), getTableTop(), getTableBottom());
    }

    private void serveAnimation(double dt) {
        if (serveKickoff) {
            serveAnimT += dt * 1.2;
            if (serveAnimT >= 1.0) {
                serveAnimT = 1.0;
                serveKickoff = false;
            }
        } else {
            serveAnimT += dt * serveAnimDir * 0.5;
            if (serveAnimT >= 1.0) {
                serveAnimT = 1.0;
                serveAnimDir = -1.0;
            } else if (serveAnimT <= 0.0) {
                serveAnimT = 0.0;
                serveAnimDir = 1.0;
            }
        }
        // Position ball near server paddle
        if (serveHolder == 0) {
            ball.x = leftPaddle.x + leftPaddle.w + 25 + Math.sin(serveAnimT * Math.PI) * 10;
            ball.y = leftPaddle.y + leftPaddle.h * 0.5 + Math.cos(serveAnimT * Math.PI) * 6;
            ball.vx = 0;
            ball.vy = 0;
        } else {
            ball.x = rightPaddle.x - 25 - Math.sin(serveAnimT * Math.PI) * 10;
            ball.y = rightPaddle.y + rightPaddle.h * 0.5 + Math.cos(serveAnimT * Math.PI) * 6;
            ball.vx = 0;
            ball.vy = 0;
        }
    }

    private void launchServe() {
        ballInPlay = true;
        double speed = 460 + rng.nextDouble() * 80;
        double angle = Math.toRadians(rng.nextDouble() * 16 - 8);
        double dir = (serveHolder == 0) ? 1.0 : -1.0;
        ball.vx = Math.cos(angle) * speed * dir;
        ball.vy = Math.sin(angle) * speed;
        ball.spin = (rng.nextDouble() * 2 - 1) * 120; // random spin
        ball.lastHitByLeft = (serveHolder == 0);
        playServeSound();
    }

    private void scorePoint(int scorer) {
        if (state == GameState.GAMEOVER) return;
        ballInPlay = false;
        ball.resetToCenter();

        // Update score
        if (scorer == 0) {
            scoreBoard.scoreLeft++;
            pushBanner("Point: Player", new Color(180, 255, 180), 1.2);
            playScoreSound(true);
        } else {
            scoreBoard.scoreRight++;
            pushBanner("Point: AI", new Color(255, 180, 180), 1.2);
            playScoreSound(false);
        }
        scoreFlashT = 1.0;
        lastPointTimeNs = System.nanoTime();

        // Change serve after SERVES_PER_TURN points, except initial advantage
        servesTakenThisTurn++;
        if (servesTakenThisTurn >= SERVES_PER_TURN) {
            serveHolder = 1 - serveHolder;
            servesTakenThisTurn = 0;
        }

        // Check for win condition: first to 11, win by 2
        if (scoreBoard.hasWinner()) {
            state = GameState.GAMEOVER;
            if (scoreBoard.scoreLeft > scoreBoard.scoreRight) {
                pushBanner("YOU WIN!", new Color(135, 235, 175), 2.5);
                playWinSound();
            } else {
                pushBanner("YOU LOSE", new Color(235, 135, 135), 2.5);
                playLoseSound();
            }
        } else {
            // Transition to serve state
            serveKickoff = true;
            serveAnimT = 0.0;
            state = GameState.SERVE;
        }

        // Explosion of particles at center
        spawnScoreParticles(scorer == 0);
        shockwaves.add(new Shockwave(getCenterX(), getCenterY()));
    }

    private void resetMatch() {
        scoreBoard.reset();
        serveHolder = 0;
        servesTakenThisTurn = 0;
        state = GameState.SERVE;
        serveKickoff = true;
        serveAnimT = 0.0;
        ballInPlay = false;
        resetEntities(false);
        banners.clear();
    }

    private void resetEntities(boolean initial) {
        leftPaddle.w = 16;
        leftPaddle.h = 95;
        rightPaddle.w = 16;
        rightPaddle.h = 95;

        leftPaddle.x = getTableLeft() + 32;
        rightPaddle.x = getTableRight() - rightPaddle.w - 32;

        leftPaddle.y = getTableMidY() - leftPaddle.h * 0.5;
        rightPaddle.y = getTableMidY() - rightPaddle.h * 0.5;

        leftPaddle.vy = 0;
        rightPaddle.vy = 0;

        ball.resetToCenter();
        ball.vx = 0;
        ball.vy = 0;
        ball.spin = 0;
        ball.radius = 8;
        ball.elasticity = 1.0;

        if (initial) {
            state = GameState.MENU;
        } else {
            state = GameState.SERVE;
        }
    }

    private void handleBallBounds(Ball b, double left, double right, double top, double bottom) {
        // Top/Bottom bounce
        if (b.y - b.radius < top) {
            b.y = top + b.radius;
            b.vy = -b.vy * 1.0;
            b.spin = b.spin * 0.97;
            playWallSound();
            spawnWallParticles(b.x, top + 2, false);
            shockwaves.add(new Shockwave(b.x, top + 1));
        } else if (b.y + b.radius > bottom) {
            b.y = bottom - b.radius;
            b.vy = -b.vy * 1.0;
            b.spin = b.spin * 0.97;
            playWallSound();
            spawnWallParticles(b.x, bottom - 2, false);
            shockwaves.add(new Shockwave(b.x, bottom - 1));
        }

        // Net center slight "visual" effect (not physical barrier)
        // Use to add mild color flash when crossing center
        if (Math.abs(b.x - getTableMidX()) < 2.0) {
            spawnNetDust(b.y);
        }
    }

    private boolean checkBallPaddleCollision(Ball b, Paddle p) {
        // AABB vs Circle approximate
        double closestX = clamp(b.x, p.x, p.x + p.w);
        double closestY = clamp(b.y, p.y, p.y + p.h);
        double dx = b.x - closestX;
        double dy = b.y - closestY;
        double dist2 = dx * dx + dy * dy;
        return dist2 <= (b.radius * b.radius);
    }

    private void handlePaddleCollision(Ball b, Paddle p, boolean isRightPaddle) {
        // Move ball outside paddle
        if (isRightPaddle) {
            b.x = p.x - b.radius - 0.1;
        } else {
            b.x = p.x + p.w + b.radius + 0.1;
        }

        // Compute bounce angle depending on hit position
        double paddleCenterY = p.y + p.h * 0.5;
        double rel = (b.y - paddleCenterY) / (p.h * 0.5);
        rel = clamp(rel, -1.0, 1.0);

        double speed = Math.hypot(b.vx, b.vy);
        double baseSpeed = 520.0;

        // Accelerate ball slightly
        speed = Math.max(baseSpeed, speed * 1.04 + 3.5);

        // Angle spread
        double maxAngle = Math.toRadians(44);
        double angle = rel * maxAngle;
        double dir = isRightPaddle ? -1.0 : 1.0;

        // Add some influence of paddle velocity (impart spin and vertical)
        double vyAdd = p.vy * 0.18;
        double spinAdd = clamp(p.vy * 0.8 + (rng.nextDouble() - 0.5) * 50, -240, 240);
        b.spin = clamp(b.spin * 0.75 + spinAdd, -300, 300);

        b.vx = Math.cos(angle) * speed * dir;
        b.vy = Math.sin(angle) * speed + vyAdd;

        b.lastHitByLeft = !isRightPaddle;

        // Hit effects
        playPaddleSound();
        spawnHitParticles(b.x, b.y, isRightPaddle ? rightPaddleColor : leftPaddleColor, b.vx, b.vy);
        shockwaves.add(new Shockwave(b.x, b.y));

        // Shorten/lengthen paddles slightly for next hit (subtle difficulty)
        double padChange = rng.nextDouble() * 4 - 2;
        leftPaddle.h = clamp(leftPaddle.h + padChange, 85, 120);
        rightPaddle.h = clamp(rightPaddle.h - padChange, 85, 120);
    }

    // Rendering ----------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Compute scale from base resolution to current view
        scaleX = viewW / (double) BASE_WIDTH;
        scaleY = viewH / (double) BASE_HEIGHT;

        // Draw onto back buffer at base resolution to keep crisp layout
        if (backBuffer.getWidth() != BASE_WIDTH || backBuffer.getHeight() != BASE_HEIGHT) {
            backBuffer = new BufferedImage(BASE_WIDTH, BASE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            backG2 = backBuffer.createGraphics();
            applyRenderHints(backG2);
        }

        Graphics2D g2 = backG2;
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setTransform(new AffineTransform());
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);

        // Background gradient
        Paint old = g2.getPaint();
        g2.setPaint(new GradientPaint(0, 0, bgGradientTop, 0, BASE_HEIGHT, bgGradientBottom));
        g2.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);
        g2.setPaint(old);

        // Table and net
        drawTable(g2);

        // Trails behind ball
        if (showTrails) {
            drawBallTrail(g2);
        }

        // Particles below ball for depth feel
        drawParticles(g2, true);

        // Paddles
        leftPaddle.draw(g2, leftPaddleColor);
        rightPaddle.draw(g2, rightPaddleColor);

        // Ball
        ball.draw(g2, ballColor);

        // Particles above ball
        drawParticles(g2, false);

        // Shockwaves
        drawShockwaves(g2);

        // HUD
        drawHUD(g2);

        // Menus and states UI
        switch (state) {
            case MENU: drawMenu(g2); break;
            case SERVE: drawServeUI(g2); break;
            case PAUSED: drawPauseOverlay(g2); break;
            case GAMEOVER: drawGameOverUI(g2); break;
        }

        // Subtle vignette
        drawVignette(g2);

        // Flip to screen scaling up/down
        Graphics2D gOut = (Graphics2D) g;
        applyRenderHints(gOut);
        AffineTransform at = new AffineTransform();
        at.scale(scaleX, scaleY);
        gOut.drawImage(backBuffer, at, null);
    }

    private void drawTable(Graphics2D g2) {
        // Table area margins
        double left = getTableLeft();
        double right = getTableRight();
        double top = getTableTop();
        double bottom = getTableBottom();
        double w = right - left;
        double h = bottom - top;

        // Shadow
        g2.setColor(tableShadow);
        g2.fillRoundRect((int) (left - 10), (int) (top - 8), (int) (w + 20), (int) (h + 16), 24, 24);

        // Table main
        g2.setColor(tableColor);
        g2.fillRoundRect((int) left, (int) top, (int) w, (int) h, 18, 18);

        // Lines
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(tableLine);
        g2.drawRoundRect((int) left, (int) top, (int) w, (int) h, 18, 18);

        // Center line and net
        g2.setColor(netColor);
        float dashPhase = (float) ((Math.sin(netOsc) * 4));
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 4f, new float[]{8f, 7f}, dashPhase));
        g2.drawLine((int) getTableMidX(), (int) top, (int) getTableMidX(), (int) bottom);

        // Net subtle flare
        g2.setColor(flareColor);
        g2.setStroke(new BasicStroke(12f));
        g2.drawLine((int) getTableMidX(), (int) (top + 4), (int) getTableMidX(), (int) (bottom - 4));

        // Little corner marks
        g2.setColor(new Color(255, 255, 255, 80));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine((int) left + 12, (int) top + 12, (int) left + 52, (int) top + 12);
        g2.drawLine((int) left + 12, (int) top + 12, (int) left + 12, (int) top + 52);
        g2.drawLine((int) right - 12, (int) top + 12, (int) right - 52, (int) top + 12);
        g2.drawLine((int) right - 12, (int) top + 12, (int) right - 12, (int) top + 52);
        g2.drawLine((int) left + 12, (int) bottom - 12, (int) left + 52, (int) bottom - 12);
        g2.drawLine((int) left + 12, (int) bottom - 12, (int) left + 12, (int) bottom - 52);
        g2.drawLine((int) right - 12, (int) bottom - 12, (int) right - 52, (int) bottom - 12);
        g2.drawLine((int) right - 12, (int) bottom - 12, (int) right - 12, (int) bottom - 52);
    }

    private void drawHUD(Graphics2D g2) {
        g2.setFont(hudFont);
        g2.setColor(hudTextColor);

        String leftName = "Player";
        String rightName = "AI (" + AIDifficulty.current.name + ")";

        int pad = 16;
        int y = (int) (getTableTop() - 24);
        y = Math.max(24, y);

        // Scores
        double flash = scoreFlashT;
        if (scoreFlashT > 0.0) {
            scoreFlashT = Math.max(0.0, scoreFlashT - 0.02);
        }
        Color scoreLeftCol = blendColors(hudTextColor, new Color(185, 255, 200), (float) flash);
        Color scoreRightCol = blendColors(hudTextColor, new Color(255, 195, 215), (float) flash);

        g2.setFont(bigScoreFont);
        g2.setColor(scoreLeftCol);
        drawTextWithShadow(g2, String.valueOf(scoreBoard.scoreLeft), (int) (getTableLeft() + 20), (int) (getTableTop() - 30), 2);
        g2.setColor(scoreRightCol);
        drawTextWithShadow(g2, String.valueOf(scoreBoard.scoreRight), (int) (getTableRight() - 60), (int) (getTableTop() - 30), 2);

        // Names
        g2.setFont(hudFont);
        g2.setColor(new Color(235, 245, 255, 200));
        drawTextWithShadow(g2, leftName, (int) (getTableLeft() + pad), y, 1);
        int rnX = (int) (getTableRight() - pad - g2.getFontMetrics().stringWidth(rightName));
        drawTextWithShadow(g2, rightName, rnX, y, 1);

        // Serve indicator
        String serveTxt = "Serve: " + ((serveHolder == 0) ? "Player" : "AI");
        int sw = g2.getFontMetrics().stringWidth(serveTxt);
        drawLabel(g2, serveTxt, (int) (getCenterX() - sw / 2), y);

        // Pause indicator or instructions
        g2.setFont(smallFont);
        g2.setColor(new Color(235, 245, 255, 180));
        int btmY = (int) (getTableBottom() + 24);
        List<String> hints = new ArrayList<>();
        hints.add("W/S or Up/Down: Move");
        hints.add("Space: Serve/Start  P: Pause  R: Reset  1/2/3: Diff  M: Mute  F: FPS  G: Trails  H: Hitboxes");
        int offsetY = 0;
        for (String h : hints) {
            int tw = g2.getFontMetrics().stringWidth(h);
            int tx = (int) (getCenterX() - tw / 2);
            drawTextWithShadow(g2, h, tx, btmY + offsetY, 1);
            offsetY += 16;
        }

        // FPS meter
        if (showFPS) {
            String fps = String.format(Locale.US, "FPS: %.0f", currentFPS);
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setFont(smallFont);
            drawTextWithShadow(g2, fps, 10, 18, 1);
        }

        // Banners
        drawBanners(g2);

        // Hitboxes debug
        if (showHitBoxes) {
            g2.setColor(new Color(255, 255, 0, 140));
            g2.drawRect((int) leftPaddle.x, (int) leftPaddle.y, (int) leftPaddle.w, (int) leftPaddle.h);
            g2.drawRect((int) rightPaddle.x, (int) rightPaddle.y, (int) rightPaddle.w, (int) rightPaddle.h);
            g2.drawOval((int) (ball.x - ball.radius), (int) (ball.y - ball.radius), (int) (2 * ball.radius), (int) (2 * ball.radius));
        }
    }

    private void drawMenu(Graphics2D g2) {
        // Title
        g2.setFont(bigScoreFont.deriveFont(56f));
        String title = "TableTennisGame";
        int tw = g2.getFontMetrics().stringWidth(title);
        int tx = (int) (getCenterX() - tw / 2);
        int ty = (int) (getCenterY() - 140);
        g2.setColor(new Color(245, 252, 255));
        drawTextWithShadow(g2, title, tx, ty, 3);

        // Buttons
        int bw = 280;
        int bh = 54;
        int bx = (int) (getCenterX() - bw / 2);
        int by = (int) (getCenterY() - 30);
        hoverStart = inRect(input.mouseX, input.mouseY, bx, by, bw, bh);
        hoverQuit = inRect(input.mouseX, input.mouseY, bx, by + 72, bw, bh);

        drawButton(g2, bx, by, bw, bh, "Start (Space)", hoverStart);
        drawButton(g2, bx, by + 72, bw, bh, "Quit (Esc)", hoverQuit);

        // Subtle instruction
        g2.setFont(smallFont);
        g2.setColor(new Color(235, 245, 255, 190));
        String it = "Choose difficulty: 1=Easy  2=Normal  3=Hard";
        int itw = g2.getFontMetrics().stringWidth(it);
        drawTextWithShadow(g2, it, (int) (getCenterX() - itw / 2), by + 160, 1);

        // Difficulty highlight
        String curr = "Current: " + AIDifficulty.current.name;
        int cw = g2.getFontMetrics().stringWidth(curr);
        drawTextWithShadow(g2, curr, (int) (getCenterX() - cw / 2), by + 182, 1);
    }

    private void drawServeUI(Graphics2D g2) {
        g2.setFont(hudFont.deriveFont(24f));
        String s = (serveHolder == 0) ? "Your Serve - Space to Serve" : "AI Serving - Space to Continue";
        int sw = g2.getFontMetrics().stringWidth(s);
        int sx = (int) (getCenterX() - sw / 2);
        int sy = (int) (getTableTop() - 22);
        g2.setColor(new Color(235, 245, 255, 230));
        drawTextWithShadow(g2, s, sx, sy, 2);
    }

    private void drawPauseOverlay(Graphics2D g2) {
        // Dim background
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);
        // Pulse text
        double a = 0.4 + pauseFlashT * 0.6;
        g2.setFont(bigScoreFont.deriveFont(48f));
        String t = "Paused";
        int w = g2.getFontMetrics().stringWidth(t);
        int x = (int) (getCenterX() - w / 2);
        int y = (int) (getCenterY() - 16);
        g2.setColor(new Color(255, 255, 255, (int) (a * 255)));
        drawTextWithShadow(g2, t, x, y, 3);
        g2.setFont(hudFont);
        String tip = "Press P to Resume";
        int tw = g2.getFontMetrics().stringWidth(tip);
        drawTextWithShadow(g2, tip, (int) (getCenterX() - tw / 2), (int) (getCenterY() + 20), 1);
    }

    private void drawGameOverUI(Graphics2D g2) {
        g2.setFont(bigScoreFont.deriveFont(48f));
        String t = (scoreBoard.scoreLeft > scoreBoard.scoreRight) ? "You Win!" : "You Lose";
        int w = g2.getFontMetrics().stringWidth(t);
        int x = (int) (getCenterX() - w / 2);
        int y = (int) (getCenterY() - 16);
        g2.setColor(new Color(255, 255, 255));
        drawTextWithShadow(g2, t, x, y, 3);

        g2.setFont(hudFont);
        String r = "Press R to Restart • Esc to Quit";
        int rw = g2.getFontMetrics().stringWidth(r);
        drawTextWithShadow(g2, r, (int) (getCenterX() - rw / 2), (int) (getCenterY() + 24), 1);
    }

    private void drawParticles(Graphics2D g2, boolean belowBall) {
        for (Particle p : particles) {
            if (p.belowBall == belowBall) {
                p.draw(g2);
            }
        }
    }

    private void drawShockwaves(Graphics2D g2) {
        for (Shockwave s : shockwaves) {
            s.draw(g2);
        }
    }

    private void drawBallTrail(Graphics2D g2) {
        for (BallTrailDot d : ballTrail) {
            d.draw(g2);
        }
    }

    private void drawVignette(Graphics2D g2) {
        // Subtle radial gradient darkening
        int w = BASE_WIDTH;
        int h = BASE_HEIGHT;
        BufferedImage vignette = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D vg = vignette.createGraphics();
        applyRenderHints(vg);
        for (int y = 0; y < h; y += 4) {
            float t = (float) y / (float) h;
            int alpha = (int) (30 + 50 * Math.pow(t - 0.5, 2));
            vg.setColor(new Color(0, 0, 0, clamp(alpha, 0, 100)));
            vg.fillRect(0, y, w, 4);
        }
        vg.dispose();
        g2.setComposite(AlphaComposite.SrcOver.derive(0.5f));
        g2.drawImage(vignette, 0, 0, null);
        g2.setComposite(AlphaComposite.SrcOver);
    }

    private void drawButton(Graphics2D g2, int x, int y, int w, int h, String text, boolean hover) {
        Color bg = hover ? new Color(80, 180, 255, 200) : new Color(60, 120, 190, 160);
        Color border = hover ? new Color(255, 255, 255, 220) : new Color(220, 230, 240, 200);
        Shape r = new RoundRectangle2D.Double(x, y, w, h, 14, 14);
        g2.setColor(bg);
        g2.fill(r);
        g2.setStroke(new BasicStroke(2.0f));
        g2.setColor(border);
        g2.draw(r);

        g2.setFont(hudFont);
        int tw = g2.getFontMetrics().stringWidth(text);
        int tx = x + w / 2 - tw / 2;
        int ty = y + h / 2 + g2.getFontMetrics().getAscent() / 2 - 6;
        g2.setColor(new Color(15, 20, 25));
        drawTextWithShadow(g2, text, tx, ty, 1);
    }

    private void drawLabel(Graphics2D g2, String text, int x, int y) {
        int pad = 6;
        int tw = g2.getFontMetrics().stringWidth(text);
        int th = g2.getFontMetrics().getAscent();
        Shape r = new RoundRectangle2D.Double(x - pad, y - th, tw + pad * 2, th + 8, 10, 10);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fill(r);
        g2.setColor(new Color(255, 255, 255, 220));
        g2.draw(r);
        drawTextWithShadow(g2, text, x, y + 2, 1);
    }

    private void drawTextWithShadow(Graphics2D g2, String s, int x, int y, int shadow) {
        Color c = g2.getColor();
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(s, x + shadow, y + shadow);
        g2.setColor(c);
        g2.drawString(s, x, y);
    }

    // Particle and visual effects ---------------------------------------------

    private void spawnHitParticles(double x, double y, Color base, double vx, double vy) {
        int n = 18 + rng.nextInt(12);
        for (int i = 0; i < n; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double sp = 80 + rng.nextDouble() * 220;
            double px = x + Math.cos(a) * rng.nextDouble() * 6;
            double py = y + Math.sin(a) * rng.nextDouble() * 6;
            double pvx = Math.cos(a) * sp + vx * 0.1;
            double pvy = Math.sin(a) * sp + vy * 0.1;
            double life = 0.3 + rng.nextDouble() * 0.6;
            Color c = new Color(
                    clamp(base.getRed() + rng.nextInt(40) - 20, 0, 255),
                    clamp(base.getGreen() + rng.nextInt(40) - 20, 0, 255),
                    clamp(base.getBlue() + rng.nextInt(40) - 20, 0, 255),
                    220
            );
            boolean below = rng.nextBoolean();
            particles.add(new Particle(px, py, pvx, pvy, life, c, below));
        }
    }

    private void spawnWallParticles(double x, double y, boolean below) {
        int n = 8 + rng.nextInt(6);
        for (int i = 0; i < n; i++) {
            double a = rng.nextDouble() * Math.PI;
            if (y > getTableMidY()) a += Math.PI;
            double sp = 100 + rng.nextDouble() * 120;
            double pvx = Math.cos(a) * sp;
            double pvy = Math.sin(a) * sp;
            double life = 0.2 + rng.nextDouble() * 0.5;
            Color c = new Color(255, 255, 255, 200);
            particles.add(new Particle(x, y, pvx, pvy, life, c, below));
        }
    }

    private void spawnNetDust(double y) {
        if (rng.nextDouble() < 0.3) {
            double x = getTableMidX();
            for (int i = 0; i < 3; i++) {
                double a = (rng.nextBoolean() ? 0 : Math.PI);
                double sp = 30 + rng.nextDouble() * 60;
                double pvx = Math.cos(a) * sp * (rng.nextDouble() * 0.5 + 0.5);
                double pvy = (rng.nextDouble() * 40 - 20);
                particles.add(new Particle(x, y, pvx, pvy, 0.25 + rng.nextDouble() * 0.3, new Color(240, 240, 240, 180), true));
            }
        }
    }

    private void spawnScoreParticles(boolean player) {
        double cx = getCenterX();
        double cy = getCenterY();
        Color base = player ? new Color(180, 255, 180) : new Color(255, 180, 180);
        for (int i = 0; i < 120; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double sp = rng.nextDouble() * 340 + 40;
            double pvx = Math.cos(a) * sp;
            double pvy = Math.sin(a) * sp;
            double life = 0.5 + rng.nextDouble() * 0.9;
            Color c = new Color(
                    clamp(base.getRed() + rng.nextInt(50) - 25, 0, 255),
                    clamp(base.getGreen() + rng.nextInt(50) - 25, 0, 255),
                    clamp(base.getBlue() + rng.nextInt(50) - 25, 0, 255),
                    200
            );
            particles.add(new Particle(cx, cy, pvx, pvy, life, c, rng.nextBoolean()));
        }
    }

    private void updateParticles(double dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update(dt);
            // Gravity slight
            p.vy += 30 * dt;
            // Fade
            if (p.life <= 0) {
                particles.remove(i);
            }
        }
    }

    private void updateShockwaves(double dt) {
        for (int i = shockwaves.size() - 1; i >= 0; i--) {
            Shockwave s = shockwaves.get(i);
            s.update(dt);
            if (s.t >= 1.0) {
                shockwaves.remove(i);
            }
        }
    }

    private void pushBallTrailDot() {
        BallTrailDot d = new BallTrailDot(ball.x, ball.y, ball.vx, ball.vy, ballColor);
        ballTrail.add(d);
    }

    private void trimTrail() {
        while (ballTrail.size() > maxTrailDots) {
            ballTrail.remove(0);
        }
        // Update alpha or size fade
        for (int i = 0; i < ballTrail.size(); i++) {
            BallTrailDot d = ballTrail.get(i);
            double t = (double) i / (double) ballTrail.size();
            d.alpha = (float) (t * 0.35);
            d.size = (float) (ball.radius * (0.6 + 0.4 * t));
        }
    }

    private void pushBanner(String text, Color color, double life) {
        banners.add(new BannerText(text, color, life));
    }

    private void updateBanners(double dt) {
        for (int i = banners.size() - 1; i >= 0; i--) {
            BannerText b = banners.get(i);
            b.update(dt);
            if (b.t >= 1.0) banners.remove(i);
        }
    }

    private void drawBanners(Graphics2D g2) {
        int baseY = (int) (getTableTop() + 20);
        int spacing = 22;
        for (int i = 0; i < banners.size(); i++) {
            BannerText b = banners.get(i);
            double t = b.t;
            int alpha = (int) (255 * Math.min(1.0, Math.max(0.0, 1.0 - Math.abs(2 * t - 1.0))));
            int y = baseY + i * spacing - (int) (t * 20);
            g2.setFont(hudFont);
            Color c = new Color(b.col.getRed(), b.col.getGreen(), b.col.getBlue(), alpha);
            g2.setColor(c);
            int w = g2.getFontMetrics().stringWidth(b.text);
            drawTextWithShadow(g2, b.text, (int) (getCenterX() - w / 2), y, 1);
        }
    }

    // Sound --------------------------------------------------------------------

    private void playPaddleSound() {
        if (!muted) sound.playBeep(700, 35, 0.7f);
    }

    private void playWallSound() {
        if (!muted) sound.playBeep(520, 28, 0.55f);
    }

    private void playServeSound() {
        if (!muted) sound.playBeep(420, 70, 0.5f);
    }

    private void playScoreSound(boolean player) {
        if (!muted) {
            if (player) {
                sound.playChord(new int[]{660, 880}, 120, 0.55f);
            } else {
                sound.playChord(new int[]{330, 220}, 120, 0.55f);
            }
        }
    }

    private void playWinSound() {
        if (!muted) sound.playMelody(new int[]{660, 880, 990}, new int[]{150, 150, 200}, 0.6f);
    }

    private void playLoseSound() {
        if (!muted) sound.playMelody(new int[]{440, 330, 220}, new int[]{150, 150, 200}, 0.6f);
    }

    // Input --------------------------------------------------------------------

    @Override
    public void keyTyped(KeyEvent e) {
        // no-op
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch (k) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                input.up = true; break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                input.down = true; break;
            case KeyEvent.VK_SPACE:
                if (!input.space) input.spacePressedOnce = true;
                input.space = true;
                if (state == GameState.MENU) {
                    state = GameState.SERVE;
                    serveKickoff = true;
                    serveAnimT = 0.0;
                    playServeSound();
                }
                break;
            case KeyEvent.VK_P:
                togglePause(); break;
            case KeyEvent.VK_R:
                resetMatch(); break;
            case KeyEvent.VK_M:
                muted = !muted; break;
            case KeyEvent.VK_F:
                showFPS = !showFPS; break;
            case KeyEvent.VK_G:
                showTrails = !showTrails; break;
            case KeyEvent.VK_H:
                showHitBoxes = !showHitBoxes; break;
            case KeyEvent.VK_1:
                AIDifficulty.current = AIDifficulty.EASY;
                pushBanner("Difficulty: Easy", new Color(200, 240, 200), 1.2);
                break;
            case KeyEvent.VK_2:
                AIDifficulty.current = AIDifficulty.NORMAL;
                pushBanner("Difficulty: Normal", new Color(200, 220, 240), 1.2);
                break;
            case KeyEvent.VK_3:
                AIDifficulty.current = AIDifficulty.HARD;
                pushBanner("Difficulty: Hard", new Color(240, 200, 200), 1.2);
                break;
            case KeyEvent.VK_ESCAPE:
                if (window != null) {
                    window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
                } else {
                    System.exit(0);
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        switch (k) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                input.up = false; break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                input.down = false; break;
            case KeyEvent.VK_SPACE:
                input.space = false; break;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int mx = logicalX(e.getX());
        int my = logicalY(e.getY());
        if (state == GameState.MENU) {
            int bw = 280;
            int bh = 54;
            int bx = (int) (getCenterX() - bw / 2);
            int by = (int) (getCenterY() - 30);
            if (inRect(mx, my, bx, by, bw, bh)) {
                state = GameState.SERVE;
                serveKickoff = true;
                serveAnimT = 0.0;
                playServeSound();
            } else if (inRect(mx, my, bx, by + 72, bw, bh)) {
                if (window != null) {
                    window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // no-op
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // no-op
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // no-op
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // no-op
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        input.mouseX = logicalX(e.getX());
        input.mouseY = logicalY(e.getY());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        input.mouseX = logicalX(e.getX());
        input.mouseY = logicalY(e.getY());
    }

    @Override
    public void focusGained(FocusEvent e) {
        // no-op
    }

    @Override
    public void focusLost(FocusEvent e) {
        // Pause on focus lost
        if (state == GameState.PLAYING) {
            togglePause(true);
        }
        input.reset();
    }

    private void togglePause() {
        togglePause(false);
    }

    private void togglePause(boolean forcePause) {
        if (state == GameState.PLAYING && (forcePause || !forcePause)) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED && !forcePause) {
            state = GameState.PLAYING;
        }
    }

    // Helpers ------------------------------------------------------------------

    private void applyRenderHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private int logicalX(int screenX) {
        return (int) Math.round(screenX / scaleX);
    }

    private int logicalY(int screenY) {
        return (int) Math.round(screenY / scaleY);
    }

    private double getTableLeft() {
        return 64;
    }

    private double getTableRight() {
        return BASE_WIDTH - 64;
    }

    private double getTableTop() {
        return 96;
    }

    private double getTableBottom() {
        return BASE_HEIGHT - 72;
    }

    private double getTableMidX() {
        return (getTableLeft() + getTableRight()) * 0.5;
    }

    private double getTableMidY() {
        return (getTableTop() + getTableBottom()) * 0.5;
    }

    private double getCenterX() {
        return BASE_WIDTH * 0.5;
    }

    private double getCenterY() {
        return BASE_HEIGHT * 0.5;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private boolean inRect(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    private Color blendColors(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (a.getRed() * (1 - t) + b.getRed() * t);
        int g = (int) (a.getGreen() * (1 - t) + b.getGreen() * t);
        int bb = (int) (a.getBlue() * (1 - t) + b.getBlue() * t);
        int al = (int) (a.getAlpha() * (1 - t) + b.getAlpha() * t);
        return new Color(r, g, bb, al);
    }

    // Inner classes ------------------------------------------------------------

    private enum GameState {
        MENU,
        SERVE,
        PLAYING,
        PAUSED,
        GAMEOVER
    }

    private static class Input {
        boolean up = false;
        boolean down = false;
        boolean space = false;
        boolean spacePressedOnce = false;
        int mouseX = 0;
        int mouseY = 0;

        void reset() {
            up = false; down = false; space = false; spacePressedOnce = false;
        }
    }

    private static class ScoreBoard {
        int scoreLeft = 0;
        int scoreRight = 0;

        void reset() {
            scoreLeft = 0;
            scoreRight = 0;
        }

        boolean hasWinner() {
            if (scoreLeft >= 11 || scoreRight >= 11) {
                return Math.abs(scoreLeft - scoreRight) >= 2;
            }
            return false;
        }
    }

    private static class Paddle {
        double x, y, w, h;
        double vy = 0;
        double speed = 540;
        double friction = 0.85;

        boolean isAI = false;

        Paddle(boolean isAI) {
            this.isAI = isAI;
            this.w = 16;
            this.h = 95;
        }

        void update(double dt, Input input, double minY, double maxY, boolean serving) {
            if (!isAI) {
                double accel = 2000;
                if (input.up) vy -= accel * dt;
                if (input.down) vy += accel * dt;
                if (!input.up && !input.down) vy *= Math.pow(friction, dt * 60.0);

                vy = clamp(vy, -speed, speed);
                y += vy * dt;
                clampY(minY, maxY);
            } else {
                // AI is handled externally
                clampY(minY, maxY);
            }
        }

        void autoIdle(double dt, double sway, double minY, double maxY) {
            double targetY = (minY + maxY - h) * 0.5 + Math.sin(System.nanoTime() / 1e9 * 1.2 + (isAI ? 3.14 : 0)) * 40 * sway;
            double diff = targetY - y;
            vy += diff * dt * 15;
            vy = clamp(vy, -300, 300);
            y += vy * dt;
            clampY(minY, maxY);
        }

        private void clampY(double minY, double maxY) {
            if (y < minY) { y = minY; vy = 0; }
            if (y + h > maxY) { y = maxY - h; vy = 0; }
        }

        void draw(Graphics2D g2, Color color) {
            // Subtle glow
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
            g2.fillRoundRect((int) (x - 6), (int) (y - 6), (int) (w + 12), (int) (h + 12), 18, 18);
            // Main
            g2.setColor(color);
            g2.fillRoundRect((int) x, (int) y, (int) w, (int) h, 12, 12);

            // Edge line
            g2.setColor(new Color(255, 255, 255, 120));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect((int) x, (int) y, (int) w, (int) h, 12, 12);
        }
    }

    private class Ball {
        double x, y, vx, vy, radius = 8.0;
        double spin = 0.0; // degrees/s like effect
        double elasticity = 1.0;
        boolean lastHitByLeft = true;

        void update(double dt) {
            // Magnus effect: spin imparts perpendicular velocity
            // v_perp = k * spin cross v direction (here 2D simplified)
            double magnus = spin * 0.08;
            // Add small curve
            vy += (vx > 0 ? 1 : -1) * magnus * dt;

            x += vx * dt;
            y += vy * dt;

            // Air damping
            vx *= Math.pow(0.9995, dt * 60.0);
            vy *= Math.pow(0.9995, dt * 60.0);
            spin *= Math.pow(0.999, dt * 60.0);
        }

        void resetToCenter() {
            x = getCenterX();
            y = getCenterY();
            vx = 0;
            vy = 0;
            spin = 0;
        }

        void idleBob(double dt, double left, double right, double top, double bottom) {
            // small Lissajous around center
            double t = System.nanoTime() / 1e9;
            x = getCenterX() + Math.sin(t * 1.2) * 60;
            y = getCenterY() + Math.cos(t * 1.4) * 40;
        }

        void draw(Graphics2D g2, Color color) {
            // subtle blur/shine based on speed
            double sp = Math.hypot(vx, vy);
            int a = (int) clamp(60 + sp * 0.08, 60, 160);
            g2.setColor(new Color(255, 255, 255, a));
            g2.fillOval((int) (x - radius - 3), (int) (y - radius - 3), (int) (2 * radius + 6), (int) (2 * radius + 6));

            // Main
            g2.setColor(color);
            g2.fillOval((int) (x - radius), (int) (y - radius), (int) (2 * radius), (int) (2 * radius));

            // Stripe to show spin
            g2.setColor(new Color(220, 220, 220, 220));
            double ang = Math.toRadians((System.nanoTime() / 1e7) % 360 + spin * 0.02);
            int sr = (int) radius;
            int cx = (int) x;
            int cy = (int) y;
            int x1 = cx + (int) (Math.cos(ang) * (sr));
            int y1 = cy + (int) (Math.sin(ang) * (sr));
            int x2 = cx - (int) (Math.cos(ang) * (sr));
            int y2 = cy - (int) (Math.sin(ang) * (sr));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(x1, y1, x2, y2);

            // Outline
            g2.setColor(new Color(0, 0, 0, 100));
            g2.drawOval((int) (x - radius), (int) (y - radius), (int) (2 * radius), (int) (2 * radius));
        }
    }

    private static class Particle {
        double x, y, vx, vy;
        double life;
        double totalLife;
        Color col;
        boolean belowBall;
        float size;

        Particle(double x, double y, double vx, double vy, double life, Color col, boolean below) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.life = life; this.totalLife = life;
            this.col = col; this.belowBall = below;
            this.size = 3.5f + (float) (Math.random() * 3.5);
        }

        void update(double dt) {
            x += vx * dt;
            y += vy * dt;
            vx *= Math.pow(0.99, dt * 60.0);
            vy *= Math.pow(0.99, dt * 60.0);
            life -= dt;
        }

        void draw(Graphics2D g2) {
            float t = (float) Math.max(0, Math.min(1, life / totalLife));
            int a = (int) (col.getAlpha() * t);
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), a));
            int r = (int) (size * (0.6 + 0.4 * t));
            g2.fillOval((int) (x - r), (int) (y - r), r * 2, r * 2);
        }
    }

    private static class Shockwave {
        double x, y;
        double t = 0.0; // 0..1
        double speed = 1.4;

        Shockwave(double x, double y) { this.x = x; this.y = y; }

        void update(double dt) {
            t += dt * speed;
        }

        void draw(Graphics2D g2) {
            if (t >= 1.0) return;
            float alpha = (float) (1.0 - t);
            int r = (int) (12 + t * 70);
            g2.setColor(new Color(255, 255, 255, (int) (150 * alpha)));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawOval((int) (x - r), (int) (y - r), r * 2, r * 2);
        }
    }

    private static class BallTrailDot {
        double x, y;
        float alpha = 0.25f;
        float size;
        Color color;

        BallTrailDot(double x, double y, double vx, double vy, Color base) {
            this.x = x;
            this.y = y;
            this.size = 6;
            int a = (int) (60 + Math.min(140, Math.hypot(vx, vy) * 0.08));
            this.color = new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
        }

        void draw(Graphics2D g2) {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * alpha)));
            int r = (int) (size);
            g2.fillOval((int) (x - r), (int) (y - r), r * 2, r * 2);
        }
    }

    private static class BannerText {
        String text;
        Color col;
        double t = 0.0; // 0..1 across life
        double life = 1.0;

        BannerText(String text, Color col, double life) {
            this.text = text; this.col = col; this.life = life;
        }

        void update(double dt) {
            t += dt / life;
            if (t > 1.0) t = 1.0;
        }
    }

    private static class AIProfile {
        double reactionDelay; // seconds before reacting
        double trackSpeed;    // max speed
        double error;         // positional error standard deviation
        double spinBias;      // not used deeply but can affect decisions

        AIProfile(double reactionDelay, double trackSpeed, double error, double spinBias) {
            this.reactionDelay = reactionDelay;
            this.trackSpeed = trackSpeed;
            this.error = error;
            this.spinBias = spinBias;
        }
    }

    private static class AIDifficulty {
        static final AIDifficulty EASY = new AIDifficulty("Easy", new AIProfile(0.18, 430, 24, 0.4));
        static final AIDifficulty NORMAL = new AIDifficulty("Normal", new AIProfile(0.10, 560, 14, 0.5));
        static final AIDifficulty HARD = new AIDifficulty("Hard", new AIProfile(0.04, 760, 6, 0.6));

        static AIDifficulty current = NORMAL;

        final String name;
        final AIProfile profile;
        AIDifficulty(String name, AIProfile profile) {
            this.name = name;
            this.profile = profile;
        }
    }

    private class AIController {
        // Reaction memory
        double timeSinceLastObs = 0.0;
        double targetY = getTableMidY();
        boolean tracking = false;

        void updateAI(Paddle aiPaddle, Ball ball, double dt, double minY, double maxY, AIDifficulty diff) {
            AIProfile p = diff.profile;

            timeSinceLastObs += dt;

            // Only react when ball moves towards AI
            boolean ballToAI = ball.vx > 0;

            if (ballToAI) {
                // React after some delay
                if (timeSinceLastObs >= p.reactionDelay) {
                    // Predict intercept y: estimate time until reach AI x
                    double timeToReach = (aiPaddle.x - ball.x) / ball.vx;
                    if (timeToReach > 0) {
                        // Predict y with bounces off top/bottom (mirror reflection)
                        double predictedY = predictY(ball, timeToReach, minY, maxY);
                        // Add error
                        predictedY += (rng.nextGaussian() * p.error);
                        // Center of paddle aims at predictedY:
                        targetY = predictedY - aiPaddle.h * 0.5;
                        tracking = true;
                    }
                    timeSinceLastObs = 0.0 * rng.nextDouble();
                }
            } else {
                // Move towards table center when ball going away
                targetY = (minY + maxY - aiPaddle.h) * 0.5 + Math.sin(System.nanoTime() / 1e9 * 1.0) * 10;
                tracking = false;
            }

            // Move paddle towards target
            double dir = Math.signum(targetY - aiPaddle.y);
            double accel = 2200;
            aiPaddle.vy += dir * accel * dt;
            double maxSpeed = p.trackSpeed;
            aiPaddle.vy = clamp(aiPaddle.vy, -maxSpeed, maxSpeed);
            aiPaddle.y += aiPaddle.vy * dt;

            // Damping
            aiPaddle.vy *= Math.pow(0.985, dt * 60.0);

            // Clamp
            if (aiPaddle.y < minY) { aiPaddle.y = minY; aiPaddle.vy = 0; }
            if (aiPaddle.y + aiPaddle.h > maxY) { aiPaddle.y = maxY - aiPaddle.h; aiPaddle.vy = 0; }
        }

        private double predictY(Ball b, double t, double minY, double maxY) {
            // Simulate y with reflections
            double y = b.y;
            double vy = b.vy;
            double top = minY + b.radius;
            double bottom = maxY - b.radius;

            double dt = 1.0 / 120.0;
            double time = 0.0;
            while (time < t) {
                double step = Math.min(dt, t - time);
                // Simple magnus approximate along x-direction
                double magnus = b.spin * 0.08;
                vy += (b.vx > 0 ? 1 : -1) * magnus * step;

                y += vy * step;
                if (y < top) {
                    y = top + (top - y);
                    vy = -vy;
                } else if (y > bottom) {
                    y = bottom - (y - bottom);
                    vy = -vy;
                }
                time += step;
            }
            return y;
        }
    }

    // Sound Engine: simple generated tones via SourceDataLine -------------------

    private static class SoundEngine {
        private final int sampleRate = 22050;
        private volatile boolean initialized = false;
        private final Queue<SoundJob> queue = new ConcurrentLinkedQueue<>();
        private Thread audioThread;
        private volatile boolean audioRunning = true;

        void init() {
            if (initialized) return;
            initialized = true;
            audioThread = new Thread(this::audioLoop, "AudioThread");
            audioThread.setDaemon(true);
            audioThread.start();
        }

        void stop() {
            audioRunning = false;
        }

        void playBeep(int freq, int ms, float vol) {
            enqueue(new SoundJob(new int[]{freq}, new int[]{ms}, vol));
        }

        void playChord(int[] freqs, int ms, float vol) {
            int[] lens = new int[freqs.length];
            Arrays.fill(lens, ms);
            enqueue(new SoundJob(freqs, lens, vol));
        }

        void playMelody(int[] freqs, int[] lens, float vol) {
            enqueue(new SoundJob(freqs, lens, vol));
        }

        private void enqueue(SoundJob job) {
            if (!initialized) return;
            queue.offer(job);
        }

        private void audioLoop() {
            SourceDataLine line = null;
            try {
                AudioFormat fmt = new AudioFormat(sampleRate, 16, 1, true, false);
                line = AudioSystem.getSourceDataLine(fmt);
                line.open(fmt, sampleRate);
                line.start();

                while (audioRunning) {
                    SoundJob job = queue.poll();
                    if (job == null) {
                        try { Thread.sleep(4); } catch (InterruptedException ignored) {}
                        continue;
                    }
                    for (int i = 0; i < job.freqs.length; i++) {
                        int f = job.freqs[i];
                        int ms = job.lens[i];
                        byte[] data = genTone(f, ms, job.vol);
                        line.write(data, 0, data.length);
                        // small separation
                        try { Thread.sleep(2); } catch (InterruptedException ignored) {}
                    }
                }
            } catch (LineUnavailableException e) {
                // Audio not available; ignore sound
            } finally {
                if (line != null) {
                    line.drain();
                    line.stop();
                    line.close();
                }
            }
        }

        private byte[] genTone(int freq, int ms, float vol) {
            int samples = (int) ((ms / 1000.0) * sampleRate);
            byte[] data = new byte[samples * 2];

            // Simple sine with quick attack/decay envelope
            for (int i = 0; i < samples; i++) {
                double t = i / (double) sampleRate;
                double env = envelope(i, samples);
                double s = Math.sin(2 * Math.PI * freq * t) * env;
                short val = (short) (s * 32767 * vol);
                data[i * 2] = (byte) (val & 0xff);
                data[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
            }
            return data;
        }

        private double envelope(int i, int total) {
            int a = Math.max(1, (int) (total * 0.04));
            int d = Math.max(1, (int) (total * 0.8));
            if (i < a) {
                return i / (double) a;
            } else if (i > d) {
                return Math.max(0, 1.0 - (i - d) / (double) (total - d));
            }
            return 1.0;
        }

        private static class SoundJob {
            int[] freqs;
            int[] lens;
            float vol;

            SoundJob(int[] freqs, int[] lens, float vol) {
                this.freqs = freqs; this.lens = lens; this.vol = vol;
            }
        }
    }
}