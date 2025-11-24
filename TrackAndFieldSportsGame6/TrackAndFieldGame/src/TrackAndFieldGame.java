import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class TrackAndFieldGame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }

}

class GameWindow extends JFrame {
    private GamePanel gamePanel;

    public GameWindow() {
        setTitle("Track & Field Sports - Single File Java Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        gamePanel = new GamePanel(1200, 700);
        add(gamePanel);
        pack();
        setLocationRelativeTo(null);
    }
}

@SuppressWarnings("serial")
class GamePanel extends JPanel implements Runnable {
    // Size
    public final int WIDTH;
    public final int HEIGHT;

    // Game loop
    private Thread gameThread;
    private boolean running = false;
    private boolean paused = false;

    // Timing
    private final int TARGET_FPS = 60;
    private final long TARGET_TIME_BETWEEN_UPDATES = 1000 / TARGET_FPS;

    // Double buffer
    private BufferedImage buffer;
    private Graphics2D g2d;

    // Game entities and world
    private Track track;
    private Athlete player;
    private List<Athlete> aiAthletes = new CopyOnWriteArrayList<>();
    private List<Obstacle> obstacles = new CopyOnWriteArrayList<>();
    private List<PowerUp> powerUps = new CopyOnWriteArrayList<>();

    // Event management
    private EventManager eventManager;
    private HUD hud;
    private InputHandler input;
    private Random rng = new Random();

    // Flags for events
    private boolean sprintEvent = true; // default
    private boolean longJumpEvent = false;
    private boolean hurdlesEvent = false;
    private boolean relayEvent = false;

    // Asset placeholders
    private Sprite runnerSprite;
    private Sprite backgroundSprite;

    // Sound manager stub
    private SoundManager soundManager = new SoundManager();

    public GamePanel(int width, int height) {
        this.WIDTH = width;
        this.HEIGHT = height;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        requestFocus();
        init();
    }

    private void init() {
        buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        g2d = buffer.createGraphics();
        // Enable anti-aliasing for nicer visuals
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Initialize world
        track = new Track(WIDTH, HEIGHT);
        input = new InputHandler();
        addKeyListener(input);
        hud = new HUD();
        eventManager = new EventManager();

        // Player and AI
        player = new Athlete("You", 0, track.getLaneY(2));
        player.setColor(new Color(30, 120, 200));

        for (int i = 0; i < 5; i++) {
            Athlete ai = new Athlete("AI-" + (i + 1), -(150 * (i + 1)), track.getLaneY(1 + (i % 4)));
            ai.setAIControlled(true);
            ai.setColor(new Color(200, 80 + i * 20, 60 + i * 20));
            ai.setMaxSpeed(5.0 + rng.nextDouble() * 3.0);
            aiAthletes.add(ai);
        }

        // Sprites are simple programmatic shapes here
        runnerSprite = Sprite.makeRunnerSprite(48, 48);
        backgroundSprite = Sprite.makeTrackBackground(WIDTH, HEIGHT);

        // create initial obstacles for hurdles event
        generateHurdles(8, 120);

        // Start the game thread
        start();
    }

    private void generateHurdles(int count, int spacing) {
        obstacles.clear();
        for (int i = 0; i < count; i++) {
            int x = 400 + i * spacing;
            Obstacle h = new Obstacle(x, track.getLaneY(2) - 10, Obstacle.Type.HURDLE);
            obstacles.add(h);
        }
    }

    public void start() {
        if (gameThread == null) {
            running = true;
            gameThread = new Thread(this, "GameThread");
            gameThread.start();
        }
    }

    public void stop() {
        running = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastUpdate = System.nanoTime();
        long accumulator = 0L;
        while (running) {
            long now = System.nanoTime();
            long delta = (now - lastUpdate) / 1_000_000; // ms
            lastUpdate = now;

            if (!paused) {
                update(delta);
            }

            render();
            repaint();

            try {
                Thread.sleep(TARGET_TIME_BETWEEN_UPDATES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void update(long deltaMs) {
        // process input
        processGlobalInput();

        // Update event manager logic
        eventManager.update(deltaMs);

        // Update athletes
        player.update(deltaMs);
        for (Athlete ai : aiAthletes) {
            ai.update(deltaMs);
        }

        // Update obstacles and collisions
        for (Obstacle o : obstacles) {
            o.update(deltaMs);
            // simple collision with player for hurdles
            if (o.type == Obstacle.Type.HURDLE && player.getBounds().intersects(o.getBounds())) {
                if (!player.isJumping()) {
                    player.trip();
                    soundManager.playClip(SoundManager.Clip.TRIP);
                }
            }
        }

        // Manage relay exchanges
        if (relayEvent) {
            // a simple relay exchange system
            eventManager.handleRelay(deltaMs, player, aiAthletes);
        }

        // Ensure camera follows player (we keep world stationary but draw offsets)
        // TODO: camera logic could be added here for panning long tracks

        // Update HUD
        hud.update(deltaMs, player, aiAthletes, eventManager);
    }

    private void render() {
        // clear
        g2d.setColor(new Color(140, 200, 255)); // sky
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // draw background track
        g2d.drawImage(backgroundSprite.image, 0, 0, null);

        // draw lane separators
        track.draw(g2d);

        // draw obstacles
        for (Obstacle o : obstacles) {
            o.draw(g2d);
        }

        // draw powerups
        for (PowerUp p : powerUps) {
            p.draw(g2d);
        }

        // draw AI athletes
        for (Athlete ai : aiAthletes) {
            ai.draw(g2d, runnerSprite);
        }

        // draw player
        player.draw(g2d, runnerSprite);

        // draw HUD on top
        hud.draw(g2d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(buffer, 0, 0, null);
    }

    private void processGlobalInput() {
        if (input.isJustPressed(KeyEvent.VK_P)) {
            paused = !paused;
        }
        if (input.isJustPressed(KeyEvent.VK_M)) {
            soundManager.toggleMute();
        }
        if (input.isJustPressed(KeyEvent.VK_H)) {
            hurdlesEvent = !hurdlesEvent;
            if (hurdlesEvent) {
                generateHurdles(8, 140);
                eventManager.startHurdles(player, aiAthletes, obstacles);
            } else {
                obstacles.clear();
            }
        }
        if (input.isJustPressed(KeyEvent.VK_L)) {
            longJumpEvent = !longJumpEvent;
            if (longJumpEvent) {
                eventManager.startLongJump(player, aiAthletes);
            }
        }
        if (input.isJustPressed(KeyEvent.VK_S)) {
            sprintEvent = !sprintEvent;
            if (sprintEvent) {
                eventManager.startSprint(player, aiAthletes);
            }
        }
        if (input.isJustPressed(KeyEvent.VK_R)) {
            relayEvent = !relayEvent;
            eventManager.startRelay(player, aiAthletes);
        }

        // Player controls (continuous press handling)
        if (input.isDown(KeyEvent.VK_RIGHT)) {
            player.applyInput(Athlete.Input.MOVE_RIGHT);
        } else if (input.isDown(KeyEvent.VK_LEFT)) {
            player.applyInput(Athlete.Input.MOVE_LEFT);
        } else {
            player.applyInput(Athlete.Input.NONE);
        }
        if (input.isJustPressed(KeyEvent.VK_UP) || input.isDown(KeyEvent.VK_W)) {
            player.applyInput(Athlete.Input.INCREASE_POWER);
        }
        if (input.isJustPressed(KeyEvent.VK_SPACE)) {
            player.applyInput(Athlete.Input.JUMP);
        }

        input.update(); // advance input state
    }

    // ------------ Inner classes for game systems ------------

    static class Track {
        private int width, height;
        private int lanes = 6;
        private int laneHeight;
        private int trackTop;

        public Track(int width, int height) {
            this.width = width;
            this.height = height;
            this.laneHeight = 80;
            this.trackTop = height / 2 - (lanes * laneHeight) / 2;
        }

        public int getLaneY(int laneIndex) {
            laneIndex = Math.max(0, Math.min(laneIndex, lanes - 1));
            return trackTop + laneIndex * laneHeight + laneHeight / 2;
        }

        public void draw(Graphics2D g) {
            int y = trackTop;
            for (int i = 0; i < lanes; i++) {
                g.setColor(new Color(120, 80, 40));
                g.fillRect(0, y, width, laneHeight - 4);
                g.setColor(Color.WHITE);
                g.drawLine(0, y + laneHeight - 8, width, y + laneHeight - 8);
                y += laneHeight;
            }
        }
    }

    static class Sprite {
        public BufferedImage image;

        private Sprite(BufferedImage i) {
            this.image = i;
        }

        public static Sprite makeRunnerSprite(int w, int h) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // body
            g.setColor(Color.BLACK);
            g.fillOval(w/4, h/5, w/2, h/2);
            // legs
            g.drawLine(w/2, h/2, w/4, h-4);
            g.drawLine(w/2, h/2, w-4, h-4);
            g.dispose();
            return new Sprite(img);
        }

        public static Sprite makeTrackBackground(int width, int height) {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            // grassy area
            g.setColor(new Color(40, 160, 60));
            g.fillRect(0, 0, width, height);

            // distant hills
            g.setColor(new Color(30, 120, 30));
            for (int i = 0; i < 6; i++) {
                int x = i * 200 - 50;
                int y = height/3 + (i%2==0?10:0);
                g.fillOval(x, y, 300, 80);
            }

            g.dispose();
            return new Sprite(img);
        }
    }

    static class Athlete {
        public enum Input { NONE, MOVE_LEFT, MOVE_RIGHT, INCREASE_POWER, JUMP }

        private String name;
        private double x, y;
        private double vx, vy;
        private double width = 34;
        private double height = 48;
        private Color color = Color.BLUE;

        // motion parameters
        private double speed = 0.0; // current forward speed
        private double maxSpeed = 7.5; // how fast they can run
        private double acceleration = 0.2;
        private double deceleration = 0.15;

        // jumping
        private boolean jumping = false;
        private double jumpVelocity = -9.5;
        private double gravity = 0.45;
        private double groundY;
        private double verticalOffset = 0.0;

        // stamina/power for sprinting
        private double power = 0.0; // accumulates when pressing power key
        private double powerDecay = 0.01;

        // AI
        private boolean aiControlled = false;
        private double aiAggression = 0.5;

        // state
        private boolean tripped = false;

        // relay baton / team
        private boolean hasBaton = false;

        public Athlete(String name, double x, double y) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.groundY = y;
        }

        public void setColor(Color c) { this.color = c; }

        public void setAIControlled(boolean b) { aiControlled = b; }

        public void setMaxSpeed(double s) { maxSpeed = s; }

        public boolean isJumping() { return jumping; }

        public Rectangle getBounds() {
            return new Rectangle((int)x, (int)(y + verticalOffset - height/2), (int)width, (int)height);
        }

        public double getX() { return x; }
        public double getY() { return y + verticalOffset; }

        public void update(long deltaMs) {
            double dt = deltaMs / 16.0; // normalize to ~60FPS

            if (tripped) {
                // slow recovery
                speed *= 0.96;
                if (Math.abs(speed) < 0.2) tripped = false;
            }

            // AI behavior
            if (aiControlled) {
                actAsAI(dt);
            }

            // Apply simple physics for vertical movement
            if (jumping) {
                vy += gravity * dt;
                verticalOffset += vy * dt;
                if (verticalOffset >= 0) {
                    verticalOffset = 0;
                    vy = 0;
                    jumping = false;
                }
            }

            // forward motion affects position
            x += speed * dt;

            // natural decay of power
            power = Math.max(0, power - powerDecay * dt);

            // friction
            if (!jumping && !tripped) {
                speed = Math.max(0, speed - deceleration * dt);
            }

            // clamp x to reasonable bounds (not strictly necessary)
            if (x < -500) x = -500;
            if (x > 1_000_000) x = 1_000_000; // huge limit for long tracks
        }

        private void actAsAI(double dt) {
            // simple AI: increase power randomly and maintain speed near max
            if (rngDouble() < 0.02 * (1 + aiAggression)) {
                power += 0.6 * dt;
            }
            if (speed < maxSpeed * (0.6 + aiAggression * 0.4)) {
                speed += acceleration * dt * (0.8 + aiAggression);
            } else {
                speed = Math.max(0.0, speed - deceleration * dt * 0.3);
            }

            // occasionally jump for hurdles
            if (Math.random() < 0.001 && !jumping) {
                jump();
            }
        }

        public void applyInput(Input in) {
            switch (in) {
                case MOVE_LEFT:
                    x -= 3; break;
                case MOVE_RIGHT:
                    x += 3; break;
                case INCREASE_POWER:
                    // pressing power increases stride and speed if not at max
                    power += 1.0;
                    speed += acceleration * (1.0 + power * 0.02);
                    if (speed > maxSpeed + power * 0.05) speed = maxSpeed + power * 0.05;
                    break;
                case JUMP:
                    jump(); break;
                case NONE:
                default:
                    // relax
            }
        }

        private void jump() {
            if (!jumping) {
                jumping = true;
                vy = jumpVelocity;
                verticalOffset = 0;
                soundPlayAsync(SoundManager.Clip.JUMP);
            }
        }

        public void trip() {
            tripped = true;
            speed = Math.max(0, speed - 2.2);
        }

        public void draw(Graphics2D g, Sprite sprite) {
            int drawX = (int) (x % 2000 + 300); // temporary camera offset for variety
            int drawY = (int) (groundY + verticalOffset - height/2);

            AffineTransform old = g.getTransform();
            g.translate(drawX, drawY);

            // body
            g.setColor(color);
            g.fillOval(-16, -24, 32, 32);
            // legs
            g.setColor(new Color(30, 30, 30));
            g.fillRect(-8, 8, 6, 18);
            g.fillRect(2, 8, 6, 18);

            // name tag
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(Color.WHITE);
            g.drawString(name, -16, -30);

            g.setTransform(old);
        }

        public void setHasBaton(boolean b) { hasBaton = b; }
        public boolean hasBaton() { return hasBaton; }

        private double rngDouble() { return Math.random(); }

        private void soundPlayAsync(SoundManager.Clip clip) {
            // delegate to manager (non-blocking)
            // In this stub we simply call manager which does nothing heavy
            // In a full implementation, sound playback should be offloaded to a separate thread
            // to avoid blocking the update/render loop.
            // Provided as a convenience helper here.
            // This is intentionally simple.
            //noinspection ResultOfMethodCallIgnored
            new Thread(() -> {
                SoundManager.getInstance().playClip(clip);
            }).start();
        }
    }

    static class Obstacle {
        public enum Type { HURDLE, WATER }
        public Type type;
        public double x, y;
        public int w = 50, h = 20;

        public Obstacle(double x, double y, Type t) {
            this.x = x;
            this.y = y;
            this.type = t;
        }

        public void update(long dt) {
            // In an extended game we might move obstacles, but they are static here
        }

        public Rectangle getBounds() {
            return new Rectangle((int)x, (int)y, w, h);
        }

        public void draw(Graphics2D g) {
            if (type == Type.HURDLE) {
                g.setColor(new Color(180, 80, 40));
                g.fillRect((int)x, (int)y, w, h);
                g.setColor(Color.WHITE);
                g.fillRect((int)x + 8, (int)y - 6, w - 16, 6);
            } else {
                g.setColor(new Color(60, 160, 200));
                g.fillOval((int)x, (int)y, w, h);
            }
        }
    }

    static class PowerUp {
        public enum Type { TURBO, STAMINA }
        public Type type;
        public double x, y;
        private int size = 16;

        public PowerUp(Type t, double x, double y) {
            this.type = t; this.x = x; this.y = y;
        }

        public void draw(Graphics2D g) {
            if (type == Type.TURBO) {
                g.setColor(new Color(255, 200, 0));
                g.fillOval((int)x, (int)y, size, size);
                g.setColor(Color.BLACK);
                g.drawString("T", (int)x+4, (int)y+12);
            } else {
                g.setColor(new Color(0, 200, 160));
                g.fillOval((int)x, (int)y, size, size);
                g.setColor(Color.BLACK);
                g.drawString("S", (int)x+4, (int)y+12);
            }
        }
    }

    static class EventManager {
        private enum Phase { IDLE, READY, RUNNING, FINISHED }
        private Phase phase = Phase.IDLE;
        private long elapsed = 0;
        private long raceDuration = 0;

        // Sprint specifics
        private double finishLineX = 1000;

        public EventManager() { }

        public void update(long dt) {
            if (phase == Phase.RUNNING) {
                elapsed += dt;
                // check finish conditions
            }
        }

        public void startSprint(Athlete player, List<Athlete> ais) {
            phase = Phase.READY;
            elapsed = 0;
            raceDuration = 0;
            // reset positions
            player.x = 0;
            for (int i = 0; i < ais.size(); i++) {
                ais.get(i).x = -120 * (i + 1);
            }
            phase = Phase.RUNNING;
        }

        public void startLongJump(Athlete player, List<Athlete> ai) {
            phase = Phase.READY;
            // set up runway and sand pit
            player.x = 0;
            phase = Phase.RUNNING;
        }

        public void startHurdles(Athlete player, List<Athlete> ais, List<Obstacle> hurdles) {
            phase = Phase.READY;
            player.x = 0;
            for (int i = 0; i < ais.size(); i++) {
                ais.get(i).x = -100 * (i + 1);
            }
            phase = Phase.RUNNING;
        }

        public void startRelay(Athlete player, List<Athlete> ais) {
            phase = Phase.READY;
            // Simplified: the player is first runner with baton
            player.setHasBaton(true);
            for (int i = 0; i < ais.size(); i++) {
                ais.get(i).setHasBaton(false);
                ais.get(i).x = -150 * (i + 1);
            }
            phase = Phase.RUNNING;
        }

        public void handleRelay(long dt, Athlete player, List<Athlete> ais) {
            // simple exchange: when teammate close to player, pass baton
            for (Athlete teammate : ais) {
                if (player.hasBaton() && Math.abs(player.getX() - teammate.getX()) < 20) {
                    teammate.setHasBaton(true);
                    player.setHasBaton(false);
                }
            }
        }

        public boolean isRunning() { return phase == Phase.RUNNING; }

        public String getPhaseName() { return phase.name(); }
    }

    static class HUD {
        private Font font = new Font("SansSerif", Font.BOLD, 14);
        private int fps = 60;

        public HUD() {}

        public void update(long dt, Athlete player, List<Athlete> ais, EventManager em) {
            // Could compute times, leaderboards
        }

        public void draw(Graphics2D g) {
            g.setFont(font);
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(8, 8, 320, 88);
            g.setColor(Color.WHITE);
            g.drawString("Track & Field - Events: S(sprint) L(long jump) H(hurdles) R(relay)", 16, 28);
            g.drawString("Controls: arrows to move, up/W to power, space to jump, P pause", 16, 48);
            g.drawString("Press M to mute sound", 16, 68);
            g.drawString("FPS: " + fps, 16, 88);
        }
    }

    static class InputHandler implements KeyListener {
        private boolean[] keys = new boolean[256];
        private boolean[] prev = new boolean[256];

        // track justPressed events
        public void update() {
            System.arraycopy(keys, 0, prev, 0, keys.length);
        }

        public boolean isDown(int keyCode) {
            if (keyCode < 0 || keyCode >= keys.length) return false;
            return keys[keyCode];
        }

        public boolean isJustPressed(int keyCode) {
            if (keyCode < 0 || keyCode >= keys.length) return false;
            return keys[keyCode] && !prev[keyCode];
        }

        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            if (code >= 0 && code < keys.length) keys[code] = true;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();
            if (code >= 0 && code < keys.length) keys[code] = false;
        }
    }

    static class SoundManager {
        public enum Clip { JUMP, TRIP, START, FINISH }
        private boolean muted = false;
        private static SoundManager instance = new SoundManager();

        public static SoundManager getInstance() { return instance; }

        public void toggleMute() { muted = !muted; }

        public void playClip(Clip clip) {
            if (muted) return;
            // stub: printing to console simulates sound event
            System.out.println("[Sound] playing: " + clip.name());
        }

        // convenience methods
        public void playStart() { playClip(Clip.START); }
        public void playFinish() { playClip(Clip.FINISH); }
    }

}
