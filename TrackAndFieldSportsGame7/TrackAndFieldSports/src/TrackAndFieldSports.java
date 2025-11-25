import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TrackAndFieldSports extends Canvas implements Runnable {

    // -----------------------------
    // Core game state & constants
    // -----------------------------

    private JFrame frame;
    private Thread gameThread;
    private boolean running = false;

    private BufferedImage backBuffer;
    private Graphics2D g2;

    private final int WIDTH = 1024;
    private final int HEIGHT = 576;

    private final double DT = 1.0 / 60.0; // 60 FPS target
    private double accumulator = 0;

    private long lastNanoTime;

    private GameState gameState = GameState.MENU;
    private EventType currentEvent = EventType.SPRINT_100M;

    private final DecimalFormat timeFmt = new DecimalFormat("0.00");
    private final DecimalFormat distFmt = new DecimalFormat("0.00");

    // Randomization helper
    private final Random rng = new Random();

    // Input state
    private boolean keyLeft, keyRight, keyUp, keyDown, keyJump, keyStart, keyRestart, keyPause;
    private boolean pressedA, pressedD; // alt tapping for speed

    // Session data
    private final SessionScores sessionScores = new SessionScores();

    // Event instances
    private SprintEvent sprintEvent;
    private HurdleEvent hurdleEvent;
    private LongJumpEvent longJumpEvent;
    private JavelinEvent javelinEvent;

    // UI states
    private boolean paused = false;

    // Visual effects container
    private final List<Particle> particles = new ArrayList<>();

    // Fonts
    private Font uiFont;
    private Font titleFont;
    private Font monoFont;

    // Camera / viewport offset for track events
    private double cameraX = 0;

    // -----------------------------
    // Enumerations
    // -----------------------------

    private enum GameState {
        MENU, IN_EVENT, EVENT_OVER
    }

    private enum EventType {
        SPRINT_100M, HURDLES_110M, LONG_JUMP, JAVELIN
    }

    // -----------------------------
    // Entry point
    // -----------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TrackAndFieldSports game = new TrackAndFieldSports();
            game.start();
        });
    }

    // -----------------------------
    // Constructor & setup
    // -----------------------------

    public TrackAndFieldSports() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setIgnoreRepaint(true);
        setupFrame();
        setupInput();
        setupResources();
        setupEvents();
    }

    private void setupFrame() {
        frame = new JFrame("Track and Field Sports - Single File Java Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(this, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void setupInput() {
        setFocusable(true);
        requestFocus();

        // Key listener for control state
        addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                switch (k) {
                    case KeyEvent.VK_LEFT:
                        keyLeft = true;
                        break;
                    case KeyEvent.VK_RIGHT:
                        keyRight = true;
                        break;
                    case KeyEvent.VK_UP:
                        keyUp = true;
                        break;
                    case KeyEvent.VK_DOWN:
                        keyDown = true;
                        break;
                    case KeyEvent.VK_SPACE:
                        keyJump = true;
                        break;
                    case KeyEvent.VK_ENTER:
                        keyStart = true;
                        break;
                    case KeyEvent.VK_R:
                        keyRestart = true;
                        break;
                    case KeyEvent.VK_ESCAPE:
                        keyPause = true;
                        break;
                    case KeyEvent.VK_A:
                        pressedA = true;
                        break;
                    case KeyEvent.VK_D:
                        pressedD = true;
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int k = e.getKeyCode();
                switch (k) {
                    case KeyEvent.VK_LEFT:
                        keyLeft = false;
                        break;
                    case KeyEvent.VK_RIGHT:
                        keyRight = false;
                        break;
                    case KeyEvent.VK_UP:
                        keyUp = false;
                        break;
                    case KeyEvent.VK_DOWN:
                        keyDown = false;
                        break;
                    case KeyEvent.VK_SPACE:
                        keyJump = false;
                        break;
                    case KeyEvent.VK_ENTER:
                        keyStart = false;
                        break;
                    case KeyEvent.VK_R:
                        keyRestart = false;
                        break;
                    case KeyEvent.VK_ESCAPE:
                        keyPause = false;
                        paused = !paused; // toggle on release for a crisp feel
                        break;
                    case KeyEvent.VK_A:
                        pressedA = false;
                        break;
                    case KeyEvent.VK_D:
                        pressedD = false;
                        break;
                }
            }
        });
    }

    private void setupResources() {
        backBuffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        g2 = backBuffer.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        uiFont = new Font("SansSerif", Font.BOLD, 18);
        titleFont = new Font("SansSerif", Font.BOLD, 48);
        monoFont = new Font("Monospaced", Font.PLAIN, 14);
    }

    private void setupEvents() {
        sprintEvent = new SprintEvent();
        hurdleEvent = new HurdleEvent();
        longJumpEvent = new LongJumpEvent();
        javelinEvent = new JavelinEvent();
    }

    // -----------------------------
    // Game loop
    // -----------------------------

    public void start() {
        if (running) return;
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void stop() {
        running = false;
        try {
            if (gameThread != null) gameThread.join();
        } catch (InterruptedException ignored) {}
    }

    @Override
    public void run() {
        lastNanoTime = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            double delta = (now - lastNanoTime) / 1_000_000_000.0;
            lastNanoTime = now;
            accumulator += delta;

            // Fixed timestep update
            while (accumulator >= DT) {
                update(DT);
                accumulator -= DT;
            }

            // Render
            render();
            drawToScreen();

            // Yield a bit
            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {}
        }
    }

    // -----------------------------
    // Update & Render
    // -----------------------------

    private void update(double dt) {
        if (paused && gameState == GameState.IN_EVENT) {
            // While paused, still animate particles lightly
            updateParticles(dt * 0.5);
            return;
        }

        switch (gameState) {
            case MENU:
                updateMenu(dt);
                break;
            case IN_EVENT:
                updateEvent(dt);
                break;
            case EVENT_OVER:
                updateEventOver(dt);
                break;
        }

        updateParticles(dt);
    }

    private void updateMenu(double dt) {
        // Start an event when ENTER is pressed or number keys
        if (keyStart) {
            // Cycle to next event each time as a playful interaction
            currentEvent = cycleEvent(currentEvent);
            startEvent(currentEvent);
            keyStart = false;
        }

        // Number keys for direct selection
        // 1: Sprint, 2: Hurdles, 3: Long Jump, 4: Javelin
        // Keep light input mapping to avoid dense code
        // Key detection in AWT uses key codes; we can add a simple check via getToolkit for mapped keys if desired.
    }

    private EventType cycleEvent(EventType evt) {
        switch (evt) {
            case SPRINT_100M: return EventType.HURDLES_110M;
            case HURDLES_110M: return EventType.LONG_JUMP;
            case LONG_JUMP: return EventType.JAVELIN;
            case JAVELIN: return EventType.SPRINT_100M;
        }
        return EventType.SPRINT_100M;
    }

    private void startEvent(EventType evt) {
        gameState = GameState.IN_EVENT;
        paused = false;
        cameraX = 0;
        pressedA = false;
        pressedD = false;

        switch (evt) {
            case SPRINT_100M:
                sprintEvent.reset();
                break;
            case HURDLES_110M:
                hurdleEvent.reset();
                break;
            case LONG_JUMP:
                longJumpEvent.reset();
                break;
            case JAVELIN:
                javelinEvent.reset();
                break;
        }
    }

    private void updateEvent(double dt) {
        // Global restart control
        if (keyRestart) {
            startEvent(currentEvent);
            keyRestart = false;
        }

        // All events support ESC pause (handled on keyReleased), ENTER continues if event is over
        switch (currentEvent) {
            case SPRINT_100M:
                sprintEvent.update(dt);
                if (sprintEvent.finished) {
                    onEventFinished();
                } else {
                    cameraX = Math.max(0, sprintEvent.runnerX - WIDTH * 0.3);
                }
                break;

            case HURDLES_110M:
                hurdleEvent.update(dt);
                if (hurdleEvent.finished) {
                    onEventFinished();
                } else {
                    cameraX = Math.max(0, hurdleEvent.runnerX - WIDTH * 0.3);
                }
                break;

            case LONG_JUMP:
                longJumpEvent.update(dt);
                if (longJumpEvent.finished) {
                    onEventFinished();
                } else {
                    cameraX = Math.max(0, longJumpEvent.runnerX - WIDTH * 0.3);
                }
                break;

            case JAVELIN:
                javelinEvent.update(dt);
                if (javelinEvent.finished) {
                    onEventFinished();
                } else {
                    cameraX = Math.max(0, javelinEvent.runnerX - WIDTH * 0.3);
                }
                break;
        }
    }

    private void onEventFinished() {
        gameState = GameState.EVENT_OVER;
        // Persist best scores
        switch (currentEvent) {
            case SPRINT_100M:
                sessionScores.bestSprint = (sessionScores.bestSprint == 0)
                        ? sprintEvent.time
                        : Math.min(sessionScores.bestSprint, sprintEvent.time);
                break;
            case HURDLES_110M:
                sessionScores.bestHurdles = (sessionScores.bestHurdles == 0)
                        ? hurdleEvent.time
                        : Math.min(sessionScores.bestHurdles, hurdleEvent.time);
                break;
            case LONG_JUMP:
                sessionScores.bestLongJump = Math.max(sessionScores.bestLongJump, longJumpEvent.bestDistance);
                break;
            case JAVELIN:
                sessionScores.bestJavelin = Math.max(sessionScores.bestJavelin, javelinEvent.bestDistance);
                break;
        }
    }

    private void updateEventOver(double dt) {
        // ENTER returns to menu
        if (keyStart) {
            gameState = GameState.MENU;
            keyStart = false;
        }
    }

    private void updateParticles(double dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            p.vx *= 0.98;
            p.vy += 50 * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            if (p.life <= 0) particles.remove(i);
        }
    }

    private void render() {
        // Clear
        g2.setColor(new Color(24, 26, 32));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Background gradient sky
        GradientPaint sky = new GradientPaint(0, 0, new Color(30, 50, 80), 0, HEIGHT, new Color(15, 20, 35));
        g2.setPaint(sky);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw depending on state
        switch (gameState) {
            case MENU:
                renderMenu();
                break;
            case IN_EVENT:
                renderEvent();
                break;
            case EVENT_OVER:
                renderEventOver();
                break;
        }

        // Particles on top
        renderParticles();

        // UI overlays
        if (paused && gameState == GameState.IN_EVENT) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            drawCenteredText("Paused", titleFont, WIDTH / 2, HEIGHT / 2 - 20, Color.WHITE);
            drawCenteredText("Press ESC to resume", uiFont, WIDTH / 2, HEIGHT / 2 + 20, Color.LIGHT_GRAY);
        }

        // Footer instructions
        g2.setFont(monoFont);
        g2.setColor(new Color(200, 210, 220));
        g2.drawString("ESC: Pause | ENTER: Start/Continue | R: Restart | A/D: Alternate taps | SPACE: Jump/Throw | W/S or Up/Down: Angle", 12, HEIGHT - 12);
    }

    private void renderMenu() {
        drawCenteredText("Track and Field Sports", titleFont, WIDTH / 2, 120, new Color(230, 240, 255));

        // Simple animated ornament
        double t = (System.nanoTime() / 1_000_000_000.0) * 1.0;
        int bob = (int) (Math.sin(t * 2.0) * 8);
        drawCenteredText("Press ENTER to cycle and start an event", uiFont, WIDTH / 2, 180 + bob, new Color(180, 200, 220));

        // Event selection preview
        g2.setFont(uiFont);
        g2.setColor(new Color(220, 230, 255));
        g2.drawString("Selected Event:", 100, 240);
        g2.setColor(new Color(140, 210, 250));
        g2.drawString(eventName(currentEvent), 280, 240);

        // Draw fake track lanes preview
        int baseY = 300;
        for (int i = 0; i < 4; i++) {
            g2.setColor(new Color(140, 60, 40));
            g2.fillRoundRect(100, baseY + i * 40, WIDTH - 200, 28, 18, 18);
            g2.setColor(new Color(220, 220, 220));
            g2.drawLine(100, baseY + i * 40 + 14, WIDTH - 100, baseY + i * 40 + 14);
        }

        // Session bests
        g2.setFont(uiFont);
        g2.setColor(new Color(230, 230, 240));
        g2.drawString("Session Bests:", 100, HEIGHT - 220);
        g2.setFont(monoFont);
        g2.setColor(new Color(200, 210, 220));
        g2.drawString("100m Sprint: " + (sessionScores.bestSprint == 0 ? "-" : timeFmt.format(sessionScores.bestSprint) + " s"), 100, HEIGHT - 195);
        g2.drawString("110m Hurdles: " + (sessionScores.bestHurdles == 0 ? "-" : timeFmt.format(sessionScores.bestHurdles) + " s"), 100, HEIGHT - 175);
        g2.drawString("Long Jump: " + (sessionScores.bestLongJump == 0 ? "-" : distFmt.format(sessionScores.bestLongJump) + " m"), 100, HEIGHT - 155);
        g2.drawString("Javelin: " + (sessionScores.bestJavelin == 0 ? "-" : distFmt.format(sessionScores.bestJavelin) + " m"), 100, HEIGHT - 135);

        // Tips
        g2.setFont(monoFont);
        g2.setColor(new Color(170, 190, 210));
        g2.drawString("Tip: For run-up events, hold SPACE to accelerate, release to perform the action. Angle with W/S or Up/Down.", 100, HEIGHT - 100);
    }

    private void renderEvent() {
        switch (currentEvent) {
            case SPRINT_100M:
                renderTrackScene(sprintEvent.trackLength);
                sprintEvent.render(g2, cameraX);
                renderHUD();
                break;
            case HURDLES_110M:
                renderTrackScene(hurdleEvent.trackLength);
                hurdleEvent.render(g2, cameraX);
                renderHUD();
                break;
            case LONG_JUMP:
                renderRunwayScene(longJumpEvent.runwayLength);
                longJumpEvent.render(g2, cameraX);
                renderHUD();
                break;
            case JAVELIN:
                renderRunwayScene(javelinEvent.runwayLength);
                javelinEvent.render(g2, cameraX);
                renderHUD();
                break;
        }
    }

    private void renderEventOver() {
        // Draw background scene still (for context)
        switch (currentEvent) {
            case SPRINT_100M:
                renderTrackScene(sprintEvent.trackLength);
                sprintEvent.render(g2, cameraX);
                break;
            case HURDLES_110M:
                renderTrackScene(hurdleEvent.trackLength);
                hurdleEvent.render(g2, cameraX);
                break;
            case LONG_JUMP:
                renderRunwayScene(longJumpEvent.runwayLength);
                longJumpEvent.render(g2, cameraX);
                break;
            case JAVELIN:
                renderRunwayScene(javelinEvent.runwayLength);
                javelinEvent.render(g2, cameraX);
                break;
        }

        // Overlay result panel
        int panelW = 520;
        int panelH = 220;
        int px = (WIDTH - panelW) / 2;
        int py = (HEIGHT - panelH) / 2;

        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(px, py, panelW, panelH, 16, 16);

        g2.setColor(new Color(240, 250, 255));
        g2.setFont(uiFont);
        g2.drawString("Event Result", px + 20, py + 36);
        g2.setFont(monoFont);

        switch (currentEvent) {
            case SPRINT_100M:
                g2.drawString("100m Sprint Time: " + timeFmt.format(sprintEvent.time) + " s", px + 20, py + 70);
                g2.drawString("Best (session): " + (sessionScores.bestSprint == 0 ? "-" : timeFmt.format(sessionScores.bestSprint) + " s"), px + 20, py + 95);
                break;
            case HURDLES_110M:
                g2.drawString("110m Hurdles Time: " + timeFmt.format(hurdleEvent.time) + " s", px + 20, py + 70);
                g2.drawString("Hurdles Cleared: " + hurdleEvent.hurdlesCleared + " / " + hurdleEvent.hurdleCount, px + 20, py + 95);
                g2.drawString("Best (session): " + (sessionScores.bestHurdles == 0 ? "-" : timeFmt.format(sessionScores.bestHurdles) + " s"), px + 20, py + 120);
                break;
            case LONG_JUMP:
                g2.drawString("Best Distance: " + distFmt.format(longJumpEvent.bestDistance) + " m", px + 20, py + 70);
                g2.drawString("Last Jump: " + distFmt.format(longJumpEvent.lastDistance) + " m", px + 20, py + 95);
                g2.drawString("Fouls: " + longJumpEvent.fouls, px + 20, py + 120);
                break;
            case JAVELIN:
                g2.drawString("Best Distance: " + distFmt.format(javelinEvent.bestDistance) + " m", px + 20, py + 70);
                g2.drawString("Last Throw: " + distFmt.format(javelinEvent.lastDistance) + " m", px + 20, py + 95);
                g2.drawString("Fouls: " + javelinEvent.fouls, px + 20, py + 120);
                break;
        }

        g2.setColor(new Color(180, 200, 220));
        g2.drawString("Press ENTER to return to menu", px + 20, py + panelH - 36);
        g2.drawString("Press R to retry", px + 320, py + panelH - 36);
    }

    private void renderTrackScene(double trackLength) {
        // Track lanes
        int groundY = HEIGHT - 180;
        // Grass
        g2.setColor(new Color(20, 80, 40));
        g2.fillRect(0, groundY + 70, WIDTH, HEIGHT - (groundY + 70));

        // Track
        for (int lane = 0; lane < 4; lane++) {
            int y = groundY + lane * 30;
            g2.setColor(new Color(150, 60, 40));
            g2.fillRect(0, y, WIDTH, 24);
            g2.setColor(new Color(220, 220, 220));
            // dashed line
            for (int i = 0; i < WIDTH; i += 40) {
                g2.fillRect(i, y + 12, 20, 2);
            }
        }

        // Horizon / stadium silhouettes
        g2.setColor(new Color(35, 45, 60));
        g2.fillRect(0, groundY - 80, WIDTH, 60);
        drawStands();

        // Start and finish markers
        int xStart = (int) (50 - cameraX);
        int xFinish = (int) (trackLength - cameraX + 50);
        g2.setColor(new Color(230, 230, 240));
        g2.fillRect(xStart, groundY - 10, 4, 110);
        g2.fillRect(xFinish, groundY - 10, 4, 110);

        g2.setColor(new Color(200, 210, 220));
        g2.setFont(monoFont);
        g2.drawString("START", xStart - 20, groundY - 20);
        g2.drawString("FINISH", xFinish - 24, groundY - 20);
    }

    private void renderRunwayScene(double runwayLength) {
        int groundY = HEIGHT - 180;

        // Grass
        g2.setColor(new Color(20, 80, 40));
        g2.fillRect(0, groundY + 70, WIDTH, HEIGHT - (groundY + 70));

        // Runway (long jump / javelin)
        g2.setColor(new Color(40, 40, 40));
        g2.fillRect(0, groundY, WIDTH, 80);

        // Takeoff board / foul line
        int boardX = (int) (runwayLength * 0.7 - cameraX + 50);
        g2.setColor(new Color(230, 230, 230));
        g2.fillRect(boardX, groundY, 12, 80);

        // Sand pit or field boundary
        g2.setColor(new Color(180, 160, 110));
        g2.fillRect(boardX + 120, groundY, WIDTH - (boardX + 120), 80);

        drawStands();
    }

    private void drawStands() {
        // Simple layered rectangles to simulate audience stands
        g2.setColor(new Color(30, 35, 50));
        g2.fillRect(0, 110, WIDTH, 40);
        g2.setColor(new Color(25, 30, 45));
        g2.fillRect(0, 85, WIDTH, 30);

        // Poles / lights
        for (int i = 0; i < WIDTH; i += 160) {
            g2.setColor(new Color(50, 60, 80));
            g2.fillRect(i + 10, 30, 8, 80);
            g2.setColor(new Color(220, 230, 255, 160));
            g2.fillRoundRect(i, 20, 28, 16, 8, 8);
        }
    }

    private void renderHUD() {
        g2.setFont(uiFont);
        g2.setColor(new Color(230, 240, 255));
        g2.drawString(eventName(currentEvent), 14, 28);

        g2.setFont(monoFont);
        g2.setColor(new Color(200, 210, 220));

        switch (currentEvent) {
            case SPRINT_100M:
                g2.drawString("Time: " + timeFmt.format(sprintEvent.time) + " s", 16, 52);
                g2.drawString("Speed: " + distFmt.format(sprintEvent.speed) + " m/s", 16, 72);
                g2.drawString("Tap A/D or Left/Right quickly to run!", 16, 92);
                break;
            case HURDLES_110M:
                g2.drawString("Time: " + timeFmt.format(hurdleEvent.time) + " s", 16, 52);
                g2.drawString("Speed: " + distFmt.format(hurdleEvent.speed) + " m/s", 16, 72);
                g2.drawString("Hurdles: " + hurdleEvent.hurdlesCleared + " / " + hurdleEvent.hurdleCount, 16, 92);
                g2.drawString("Tap A/D to run. SPACE to jump.", 16, 112);
                break;
            case LONG_JUMP:
                g2.drawString("Run-up speed: " + distFmt.format(longJumpEvent.speed) + " m/s", 16, 52);
                g2.drawString("Angle: " + distFmt.format(longJumpEvent.angleDeg) + " deg", 16, 72);
                g2.drawString("Best: " + distFmt.format(longJumpEvent.bestDistance) + " m | Last: " + distFmt.format(longJumpEvent.lastDistance) + " m", 16, 92);
                g2.drawString("Hold SPACE to run. Release to jump. Adjust angle with W/S.", 16, 112);
                break;
            case JAVELIN:
                g2.drawString("Run-up speed: " + distFmt.format(javelinEvent.speed) + " m/s", 16, 52);
                g2.drawString("Angle: " + distFmt.format(javelinEvent.angleDeg) + " deg", 16, 72);
                g2.drawString("Best: " + distFmt.format(javelinEvent.bestDistance) + " m | Last: " + distFmt.format(javelinEvent.lastDistance) + " m", 16, 92);
                g2.drawString("Hold SPACE to run. Release to throw. Adjust angle with W/S.", 16, 112);
                break;
        }
    }

    private void renderParticles() {
        for (Particle p : particles) {
            int alpha = (int) (Math.max(0, p.life) / p.maxLife * 255);
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha));
            g2.fillOval((int) p.x, (int) p.y, (int) p.size, (int) p.size);
        }
    }

    private void drawCenteredText(String text, Font font, int cx, int cy, Color color) {
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text);
        int h = fm.getAscent();
        g2.setColor(color);
        g2.drawString(text, cx - w / 2, cy + h / 2);
    }

    private void drawToScreen() {
        BufferStrategy bs = getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(2);
            return;
        }
        Graphics g = bs.getDrawGraphics();
        g.drawImage(backBuffer, 0, 0, null);
        g.dispose();
        bs.show();
        Toolkit.getDefaultToolkit().sync();
    }

    private String eventName(EventType evt) {
        switch (evt) {
            case SPRINT_100M: return "100m Sprint";
            case HURDLES_110M: return "110m Hurdles";
            case LONG_JUMP: return "Long Jump";
            case JAVELIN: return "Javelin Throw";
        }
        return "Event";
    }

    // -----------------------------
    // Events implementations
    // -----------------------------

    private class SprintEvent {
        double runnerX = 50;
        double runnerY = HEIGHT - 180 + 12;
        double speed = 0;
        double time = 0;

        double trackLength = 1000; // pixels -> scaled length (approx)

        boolean finished = false;
        double maxSpeed = 12.0; // m/s equivalent scaled
        double accelTap = 3.8;  // speed boost per successful alternation
        double speedDecay = 3.0; // per second

        boolean lastTapA = false;
        double strideAnim = 0;

        void reset() {
            runnerX = 50;
            speed = 0;
            time = 0;
            finished = false;
            lastTapA = false;
            strideAnim = 0;
        }

        void update(double dt) {
            // Input: alternating taps with A/D or Left/Right
            boolean tapLeft = pressedA || keyLeft;
            boolean tapRight = pressedD || keyRight;

            // alternation detection
            boolean tapped = false;
            if (tapLeft && !lastTapA) {
                speed += accelTap * dt * 10.0;
                lastTapA = true;
                tapped = true;
            } else if (tapRight && lastTapA) {
                speed += accelTap * dt * 10.0;
                lastTapA = false;
                tapped = true;
            }

            if (tapped) {
                // Kick a small particle puff
                addDust(runnerX - cameraX, runnerY + 8);
            }

            // Decay and clamp
            speed -= speedDecay * dt;
            speed = Math.max(0, Math.min(speed, maxSpeed));

            // Advance
            runnerX += speed * 60 * dt; // scale pixel movement
            time += dt;

            // Animation stride cycle
            strideAnim += speed * dt * 4.0;

            // Finish line
            if (runnerX >= trackLength + 50) {
                finished = true;
                // celebratory particles
                for (int i = 0; i < 22; i++) {
                    addSpark(runnerX - cameraX, runnerY - 30);
                }
            }
        }

        void render(Graphics2D g, double camX) {
            // Runner representation: a simple figure with animated legs
            int x = (int) (runnerX - camX);
            int y = (int) runnerY;

            // Shadow
            g.setColor(new Color(0, 0, 0, 80));
            g.fillOval(x - 10, y + 10, 20, 6);

            // Body
            g.setColor(new Color(255, 235, 160));
            g.fillOval(x - 6, y - 30, 12, 12); // head
            g.setColor(new Color(180, 210, 250));
            g.fillRect(x - 5, y - 20, 10, 20); // torso

            // Arms swinging
            double swing = Math.sin(strideAnim * 6.0) * 8;
            g.setColor(new Color(160, 200, 240));
            g.fillRect(x - 14, y - 16, 8, (int) (14 + swing));
            g.fillRect(x + 6, y - 16, 8, (int) (14 - swing));

            // Legs
            double leg = Math.sin(strideAnim * 9.0);
            g.setColor(new Color(60, 60, 60));
            drawRotatedRect(g, x - 6, y, 6, 18, leg * 0.3);
            drawRotatedRect(g, x + 2, y, 6, 18, -leg * 0.3);
        }
    }

    private class HurdleEvent {
        double runnerX = 50;
        double runnerY = HEIGHT - 180 + 12;
        double speed = 0;
        double time = 0;

        double trackLength = 1100;
        boolean finished = false;

        int hurdleCount = 10;
        double hurdleSpacing = 90;
        List<Double> hurdles = new ArrayList<>();
        double gravity = 35;
        boolean inAir = false;
        double velY = 0;
        double jumpPower = -16;

        int hurdlesCleared = 0;
        boolean lastTapA = false;
        double strideAnim = 0;

        void reset() {
            runnerX = 50;
            speed = 0;
            time = 0;
            finished = false;
            hurdles.clear();
            for (int i = 0; i < hurdleCount; i++) {
                hurdles.add(200 + i * hurdleSpacing + rng.nextDouble() * 10);
            }
            inAir = false;
            velY = 0;
            hurdlesCleared = 0;
            lastTapA = false;
            strideAnim = 0;
        }

        void update(double dt) {
            // Alternating taps for speed
            boolean tapLeft = pressedA || keyLeft;
            boolean tapRight = pressedD || keyRight;
            double maxSpeed = 11.5;
            double accelTap = 3.6;
            double speedDecay = 3.2;

            boolean tapped = false;
            if (tapLeft && !lastTapA) {
                speed += accelTap * dt * 10.0;
                lastTapA = true;
                tapped = true;
            } else if (tapRight && lastTapA) {
                speed += accelTap * dt * 10.0;
                lastTapA = false;
                tapped = true;
            }
            if (tapped) addDust(runnerX - cameraX, runnerY + 8);

            speed -= speedDecay * dt;
            speed = Math.max(0, Math.min(speed, maxSpeed));

            // Jump input
            if (!inAir && keyJump) {
                inAir = true;
                velY = jumpPower;
                addSpark(runnerX - cameraX, runnerY - 12);
            }

            // Physics
            if (inAir) {
                velY += gravity * dt;
                runnerY += velY;
                if (runnerY >= HEIGHT - 180 + 12) {
                    runnerY = HEIGHT - 180 + 12;
                    inAir = false;
                    velY = 0;
                }
            }

            // Forward motion
            runnerX += speed * 60 * dt;
            time += dt;
            strideAnim += speed * dt * 4.0;

            // Hurdle collisions / clears
            for (int i = 0; i < hurdles.size(); i++) {
                double hx = hurdles.get(i);
                if (runnerX > hx && runnerX - speed * 60 * dt <= hx) {
                    // Passing a hurdle; check if airborne during crossing
                    double hurdleTop = HEIGHT - 180 - 10;
                    if (runnerY < hurdleTop) {
                        hurdlesCleared++;
                        addSpark(hx - cameraX, hurdleTop);
                    } else {
                        // Hit hurdle -> speed penalty
                        speed *= 0.6;
                        addDust(hx - cameraX, hurdleTop + 20);
                    }
                }
            }

            if (runnerX >= trackLength + 50) {
                finished = true;
                for (int i = 0; i < 24; i++) addSpark(runnerX - cameraX, runnerY - 30);
            }
        }

        void render(Graphics2D g, double camX) {
            int x = (int) (runnerX - camX);
            int y = (int) runnerY;

            // Hurdles
            for (double hx : hurdles) {
                int sx = (int) (hx - camX);
                int gy = HEIGHT - 180 + 12;
                g.setColor(new Color(240, 240, 240));
                g.fillRect(sx - 12, gy - 40, 24, 6);
                g.fillRect(sx - 8, gy - 34, 16, 4);
                g.setColor(new Color(140, 140, 140));
                g.fillRect(sx - 10, gy - 30, 20, 30);
            }

            // Runner
            g.setColor(new Color(0, 0, 0, 80));
            g.fillOval(x - 10, y + 10, 20, 6);
            g.setColor(new Color(255, 235, 160));
            g.fillOval(x - 6, y - 30, 12, 12);
            g.setColor(new Color(180, 210, 250));
            g.fillRect(x - 5, y - 20, 10, 20);

            double swing = Math.sin(strideAnim * 6.0) * 8;
            g.setColor(new Color(160, 200, 240));
            g.fillRect(x - 14, y - 16, 8, (int) (14 + swing));
            g.fillRect(x + 6, y - 16, 8, (int) (14 - swing));

            double leg = Math.sin(strideAnim * 9.0);
            g.setColor(new Color(60, 60, 60));
            drawRotatedRect(g, x - 6, y, 6, 18, leg * 0.3);
            drawRotatedRect(g, x + 2, y, 6, 18, -leg * 0.3);

            // If in air, adjust leg visuals to look "jumping"
            if (inAir) {
                g.setColor(new Color(220, 220, 220, 70));
                g.drawOval(x - 16, y - 36, 32, 24);
            }
        }
    }

    private class LongJumpEvent {
        double runnerX = 50;
        double runnerY = HEIGHT - 180 + 12;
        double speed = 0;
        double runwayLength = 900;

        double angleDeg = 20;
        double gravity = 38;

        boolean inRunUp = true;
        boolean inJump = false;
        boolean finished = false;

        double velX = 0, velY = 0;
        double bestDistance = 0;
        double lastDistance = 0;
        int fouls = 0;

        double boardX() { return runwayLength * 0.7; }

        void reset() {
            runnerX = 50;
            runnerY = HEIGHT - 180 + 12;
            speed = 0;
            angleDeg = 20;
            inRunUp = true;
            inJump = false;
            finished = false;
            velX = 0;
            velY = 0;
            // bestDistance persists across attempts within session
        }

        void update(double dt) {
            if (finished) return;

            // Angle control
            if (keyUp || keyDown) {
                double d = (keyUp ? 1 : 0) - (keyDown ? 1 : 0);
                angleDeg += d * 40 * dt;
                angleDeg = Math.max(5, Math.min(45, angleDeg));
            }

            if (inRunUp) {
                // Hold SPACE to accelerate
                if (keyJump) {
                    speed += 10 * dt;
                    speed = Math.min(speed, 11.0);
                    addDust(runnerX - cameraX, runnerY + 8);
                } else {
                    speed -= 5 * dt;
                    speed = Math.max(speed, 0);
                }

                runnerX += speed * 60 * dt;
                // Takeoff when releasing SPACE near board
                double board = boardX();
                if (!keyJump && runnerX > board - 20 && runnerX < board + 30) {
                    inRunUp = false;
                    inJump = true;
                    double rad = Math.toRadians(angleDeg);
                    velX = speed * 60 * Math.cos(rad);
                    velY = -speed * 35 * Math.sin(rad);
                    addSpark(runnerX - cameraX, runnerY - 10);
                }

                // Foul if takeoff after board by too much
                if (!keyJump && runnerX >= board + 30 && !inJump) {
                    // foul: no valid jump
                    fouls++;
                    lastDistance = 0;
                    finished = true;
                    for (int i = 0; i < 10; i++) addDust(runnerX - cameraX, runnerY + 20);
                }
            } else if (inJump) {
                runnerX += velX * dt;
                velY += gravity * dt;
                runnerY += velY * dt;

                if (runnerY >= HEIGHT - 180 + 12) {
                    runnerY = HEIGHT - 180 + 12;
                    inJump = false;
                    finished = true;

                    double board = boardX();
                    double distancePixels = runnerX - board - 12;
                    double meters = Math.max(0, distancePixels / 22.0);
                    lastDistance = meters;
                    bestDistance = Math.max(bestDistance, meters);

                    for (int i = 0; i < 18; i++) addSpark(runnerX - cameraX, runnerY - 4);
                }
            }
        }

        void render(Graphics2D g, double camX) {
            // Board draw handled by runway scene
            int x = (int) (runnerX - camX);
            int y = (int) runnerY;

            // Runner
            g.setColor(new Color(0, 0, 0, 80));
            g.fillOval(x - 10, y + 10, 20, 6);
            g.setColor(new Color(255, 235, 160));
            g.fillOval(x - 6, y - 30, 12, 12);
            g.setColor(new Color(180, 210, 250));
            g.fillRect(x - 5, y - 20, 10, 20);

            // Arms/legs
            g.setColor(new Color(160, 200, 240));
            g.fillRect(x - 14, y - 16, 8, 14);
            g.fillRect(x + 6, y - 16, 8, 14);

            g.setColor(new Color(60, 60, 60));
            drawRotatedRect(g, x - 6, y, 6, 18, inJump ? -0.6 : 0.2);
            drawRotatedRect(g, x + 2, y, 6, 18, inJump ? 0.6 : -0.2);

            // Angle indicator near player
            g.setColor(new Color(220, 230, 250));
            g.setFont(monoFont);
            g.drawString("Angle: " + distFmt.format(angleDeg) + "°", x - 30, y - 45);
        }
    }

    private class JavelinEvent {
        double runnerX = 50;
        double runnerY = HEIGHT - 180 + 12;
        double speed = 0;
        double runwayLength = 1000;

        double angleDeg = 30;
        double gravity = 36;

        boolean inRunUp = true;
        boolean inThrow = false;
        boolean finished = false;

        double velX = 0, velY = 0;
        double javX = 0, javY = 0;
        double bestDistance = 0;
        double lastDistance = 0;
        int fouls = 0;

        double boardX() { return runwayLength * 0.7; }

        void reset() {
            runnerX = 50;
            runnerY = HEIGHT - 180 + 12;
            speed = 0;
            angleDeg = 30;
            inRunUp = true;
            inThrow = false;
            finished = false;
            velX = 0;
            velY = 0;
            javX = runnerX;
            javY = runnerY - 20;
        }

        void update(double dt) {
            if (finished) return;

            // Angle
            if (keyUp || keyDown) {
                double d = (keyUp ? 1 : 0) - (keyDown ? 1 : 0);
                angleDeg += d * 40 * dt;
                angleDeg = Math.max(5, Math.min(50, angleDeg));
            }

            if (inRunUp) {
                if (keyJump) {
                    speed += 10 * dt;
                    speed = Math.min(speed, 10.5);
                    addDust(runnerX - cameraX, runnerY + 8);
                } else {
                    speed -= 5 * dt;
                    speed = Math.max(speed, 0);
                }

                runnerX += speed * 60 * dt;
                javX = runnerX + 10;
                javY = runnerY - 20;

                double board = boardX();

                // Throw on release near board
                if (!keyJump && runnerX > board - 20 && runnerX < board + 30) {
                    inRunUp = false;
                    inThrow = true;
                    double rad = Math.toRadians(angleDeg);
                    velX = (speed * 60) * Math.cos(rad) + 80;
                    velY = -(speed * 34) * Math.sin(rad) - 40;
                    addSpark(runnerX - cameraX, runnerY - 10);
                }

                if (!keyJump && runnerX >= board + 30 && !inThrow) {
                    fouls++;
                    lastDistance = 0;
                    finished = true;
                    for (int i = 0; i < 10; i++) addDust(runnerX - cameraX, runnerY + 20);
                }
            } else if (inThrow) {
                // Javelin flight
                javX += velX * dt;
                velY += gravity * dt;
                javY += velY * dt;

                // Runner slows/stops after throw
                runnerX += Math.max(0, speed * 25 * dt);
                speed *= 0.96;

                // Hit ground
                if (javY >= HEIGHT - 180 + 12) {
                    javY = HEIGHT - 180 + 12;
                    inThrow = false;
                    finished = true;

                    double board = boardX();
                    double distancePixels = javX - board - 12;
                    double meters = Math.max(0, distancePixels / 22.0);
                    lastDistance = meters;
                    bestDistance = Math.max(bestDistance, meters);

                    for (int i = 0; i < 20; i++) addSpark(javX - cameraX, javY - 2);
                }
            }
        }

        void render(Graphics2D g, double camX) {
            int x = (int) (runnerX - camX);
            int y = (int) runnerY;
            int jx = (int) (javX - camX);
            int jy = (int) javY;

            // Runner
            g.setColor(new Color(0, 0, 0, 80));
            g.fillOval(x - 10, y + 10, 20, 6);
            g.setColor(new Color(255, 235, 160));
            g.fillOval(x - 6, y - 30, 12, 12);
            g.setColor(new Color(180, 210, 250));
            g.fillRect(x - 5, y - 20, 10, 20);

            // Arms holding javelin (pre-throw)
            g.setColor(new Color(160, 200, 240));
            g.fillRect(x - 14, y - 16, 8, 14);
            g.fillRect(x + 6, y - 16, 8, 14);

            // Legs
            g.setColor(new Color(60, 60, 60));
            drawRotatedRect(g, x - 6, y, 6, 18, inThrow ? -0.1 : 0.2);
            drawRotatedRect(g, x + 2, y, 6, 18, inThrow ? 0.1 : -0.2);

            // Javelin representation
            g.setColor(new Color(220, 220, 230));
            if (!inThrow) {
                // Carry javelin at angle
                drawLineWithAngle(g, x + 8, y - 20, Math.toRadians(angleDeg - 10), 36);
            } else {
                drawLineWithAngle(g, jx, jy, Math.toRadians(angleDeg), 48);
            }

            // Angle indicator
            g.setColor(new Color(220, 230, 250));
            g.setFont(monoFont);
            g.drawString("Angle: " + distFmt.format(angleDeg) + "°", x - 30, y - 45);
        }
    }

    // -----------------------------
    // Particles and helpers
    // -----------------------------

    private static class Particle {
        double x, y;
        double vx, vy;
        double size;
        double life, maxLife;
        Color color;

        Particle(double x, double y, double vx, double vy, double size, double life, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.life = life;
            this.maxLife = life;
            this.color = color;
        }
    }

    private void addDust(double x, double y) {
        for (int i = 0; i < 4; i++) {
            double vx = (rng.nextDouble() - 0.5) * 60;
            double vy = -(rng.nextDouble() * 40);
            double size = 3 + rng.nextDouble() * 3;
            particles.add(new Particle(x, y, vx, vy, size, 0.6 + rng.nextDouble() * 0.4, new Color(180, 160, 120)));
        }
    }

    private void addSpark(double x, double y) {
        for (int i = 0; i < 8; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double s = 120 + rng.nextDouble() * 120;
            double vx = Math.cos(a) * s;
            double vy = Math.sin(a) * s - 60;
            double size = 2 + rng.nextDouble() * 2;
            particles.add(new Particle(x, y, vx, vy, size, 0.5 + rng.nextDouble() * 0.45, new Color(250, 230, 120)));
        }
    }

    private void drawRotatedRect(Graphics2D g, int cx, int cy, int w, int h, double angle) {
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.rotate(angle);
        g.fillRect(-w / 2, 0, w, h);
        g.setTransform(old);
    }

    private void drawLineWithAngle(Graphics2D g, int x, int y, double angle, int length) {
        int x2 = x + (int) (Math.cos(angle) * length);
        int y2 = y + (int) (Math.sin(angle) * length);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(x, y, x2, y2);
        g.setStroke(new BasicStroke(1f));
    }

    // -----------------------------
    // Session data
    // -----------------------------

    private static class SessionScores {
        double bestSprint = 0;
        double bestHurdles = 0;
        double bestLongJump = 0;
        double bestJavelin = 0;
    }
}
