import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class MarioStyleRacing extends Application {

    // --- CONSTANTS ---
    private static final double W = 1000;
    private static final double H = 700;
    private static final double TRACK_LEN = 12000;
    private static final double ROAD_W = 260;
    private static final double MAX_SPEED = 8.0;

    // --- STATE ---
    enum State { MENU, RACING, FINISHED }
    enum ItemType { COIN, QBLOCK, PIPE }

    State state = State.MENU;
    boolean vsAI = true;
    double time = 0;
    double camY = 0;

    // --- ENTITIES ---
    Kart p1, p2;
    ArrayList<Collectible> items = new ArrayList<>();
    ArrayList<Particle> particles = new ArrayList<>();
    ArrayList<Cloud> clouds = new ArrayList<>();
    Set<KeyCode> keys = new HashSet<>();
    Random rand = new Random();

    @Override
    public void start(Stage primaryStage) {
        Canvas canvas = new Canvas(W, H);
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, Color.BLACK);

        scene.setOnKeyPressed(e -> keys.add(e.getCode()));
        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));

        primaryStage.setTitle("Super Cross-Country Adventure");
        primaryStage.setScene(scene);
        primaryStage.show();

        generateWorld();

        // --- GAME LOOP ---
        AnimationTimer timer = new AnimationTimer() {
            private long lastTime = System.nanoTime();
            @Override
            public void handle(long now) {
                double dt = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                if (dt > 0.05) dt = 0.05;

                update(dt);
                render(canvas.getGraphicsContext2D());
            }
        };
        timer.start();
    }

    private void generateWorld() {
        items.clear();
        clouds.clear();

        // Generate Clouds
        for (int i = 0; i < 30; i++) {
            clouds.add(new Cloud(rand.nextDouble() * W, rand.nextDouble() * TRACK_LEN));
        }

        // Generate Items along the track
        for (double d = 300; d < TRACK_LEN - 500; d += 250) {
            double center = getRoadCenterX(d);
            double typeRoll = rand.nextDouble();

            if (typeRoll < 0.5) {
                // Coin line
                for (int i = 0; i < 3; i++) {
                    items.add(new Collectible(center + (i - 1) * 40, d + i * 30, ItemType.COIN));
                }
            } else if (typeRoll < 0.75) {
                // Question Block
                items.add(new Collectible(center, d, ItemType.QBLOCK));
            } else {
                // Pipe on edge
                double side = rand.nextBoolean() ? -1 : 1;
                items.add(new Collectible(center + side * (ROAD_W / 2 - 30), d, ItemType.PIPE));
            }
        }
    }

    private void initRace() {
        p1 = new Kart(W / 2 - 40, 50, Color.RED, true, "P1");
        p2 = new Kart(W / 2 + 40, 50, Color.GREEN, false, vsAI ? "AI" : "P2");
        particles.clear();
        camY = 0;
        state = State.RACING;
        generateWorld();
    }

    // --- UPDATE LOGIC ---

    private void update(double dt) {
        time += dt;

        if (state == State.MENU) {
            if (keys.contains(KeyCode.DIGIT1)) { vsAI = true; initRace(); }
            if (keys.contains(KeyCode.DIGIT2)) { vsAI = false; initRace(); }
            return;
        }
        if (state == State.FINISHED) {
            if (keys.contains(KeyCode.SPACE)) state = State.MENU;
            return;
        }

        // Input
        handleInput(p1, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, KeyCode.SPACE);
        if (!vsAI) handleInput(p2, KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT, KeyCode.SHIFT);
        else updateAI(p2, dt);

        // Physics
        p1.update(dt, this);
        p2.update(dt, this);

        // Collisions
        checkCollisions(p1);
        checkCollisions(p2);

        // Camera & Particles
        camY = p1.dist;
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update(dt);
            if (p.life <= 0) particles.remove(i);
        }

        // Win Condition
        if (p1.dist >= TRACK_LEN || p2.dist >= TRACK_LEN) {
            state = State.FINISHED;
        }
    }

    private void handleInput(Kart k, KeyCode up, KeyCode down, KeyCode left, KeyCode right, KeyCode jump) {
        k.accelInput = keys.contains(up);
        k.brakeInput = keys.contains(down);
        k.steerInput = 0;
        if (keys.contains(left)) k.steerInput = -1;
        if (keys.contains(right)) k.steerInput = 1;
        k.jumpInput = keys.contains(jump);
    }

    private void updateAI(Kart ai, double dt) {
        double lookAhead = ai.dist + 350;
        double targetX = getRoadCenterX(lookAhead);

        boolean shouldJump = false;
        for (Collectible c : items) {
            if (c.type == ItemType.PIPE && Math.abs(c.dist - ai.dist) < 150 && Math.abs(c.x - ai.x) < 60) {
                shouldJump = true;
                break;
            }
        }

        ai.jumpInput = shouldJump && !ai.isJumping;
        ai.steerInput = Math.max(-1, Math.min(1, (targetX - ai.x) / 120.0));
        ai.accelInput = true;

        double futureCenter = getRoadCenterX(ai.dist + 600);
        ai.brakeInput = Math.abs(futureCenter - targetX) > 180;
    }

    private void checkCollisions(Kart k) {
        for (int i = items.size() - 1; i >= 0; i--) {
            Collectible c = items.get(i);
            double dx = k.x - c.x;
            double dy = k.dist - c.dist;
            double distSq = dx * dx + dy * dy;

            if (c.type == ItemType.COIN && distSq < 35 * 35) {
                items.remove(i);
                k.coins++;
                k.speed = Math.min(k.speed * 1.05, MAX_SPEED * 1.2);
                spawnSparkles(c.x, c.dist, Color.GOLD);
            } else if (c.type == ItemType.QBLOCK && distSq < 40 * 40 && !k.isJumping) {
                c.hit = true;
                k.speed = MAX_SPEED * 1.6;
                k.boostTime = 2.0;
                spawnSparkles(c.x, c.dist, Color.YELLOW);
            } else if (c.type == ItemType.PIPE && distSq < 35 * 35 && !k.isJumping) {
                k.speed *= -0.4;
                k.stunTime = 1.0;
                spawnSparkles(k.x, k.dist, Color.WHITE);
            }
        }
    }

    // --- RENDERING ---

    private void render(GraphicsContext gc) {
        gc.clearRect(0, 0, W, H);

        if (state == State.MENU) { drawMenu(gc); return; }

        drawBackground(gc);
        drawTrack(gc);
        drawClouds(gc);
        drawItems(gc);
        drawParticles(gc);
        drawKart(gc, p1);
        drawKart(gc, p2);
        drawHUD(gc);

        if (state == State.FINISHED) drawFinish(gc);
    }

    private void drawMenu(GraphicsContext gc) {
        LinearGradient bg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.SKYBLUE), new Stop(1, Color.LIGHTGREEN));
        gc.setFill(bg);
        gc.fillRect(0, 0, W, H);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 60));
        gc.fillText("SUPER CROSS-COUNTRY", 180, 200);

        gc.setFill(Color.RED);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 80));
        gc.fillText("ADVENTURE!", 300, 290);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 30));
        gc.fillText("Press [1] for 1 Player (vs Computer)", 250, 420);
        gc.fillText("Press [2] for 2 Players (Local)", 250, 480);

        gc.setFont(Font.font("Arial", 20));
        gc.setFill(Color.YELLOW);
        gc.fillText("P1: WASD + SPACE(Jump)  |  P2: Arrows + SHIFT(Jump)", 200, 600);
    }

    private void drawBackground(GraphicsContext gc) {
        // Vibrant Grass
        gc.setFill(Color.rgb(85, 180, 40));
        gc.fillRect(0, 0, W, H);

        // Grass texture dots
        gc.setFill(Color.rgb(65, 150, 30));
        for (int i = 0; i < 150; i++) {
            double x = (i * 137.5) % W;
            double y = ((i * 241.1 - camY * 0.5) % H + H) % H;
            gc.fillOval(x, y, 6, 6);
        }
    }

    private void drawTrack(GraphicsContext gc) {
        for (double screenY = 0; screenY < H; screenY += 4) {
            double worldDist = camY + (H * 0.7 - screenY);
            double centerX = getRoadCenterX(worldDist);

            // Dirt Track
            gc.setFill(Color.rgb(210, 180, 140));
            gc.fillRect(centerX - ROAD_W / 2, screenY, ROAD_W, 5);

            // Darker edges
            gc.setFill(Color.rgb(160, 120, 80));
            gc.fillRect(centerX - ROAD_W / 2, screenY, 15, 5);
            gc.fillRect(centerX + ROAD_W / 2 - 15, screenY, 15, 5);
        }
    }

    private void drawClouds(GraphicsContext gc) {
        for (Cloud c : clouds) {
            double screenY = H * 0.7 - (c.dist - camY) * 0.3; // Parallax
            if (screenY > -100 && screenY < H + 100) {
                gc.setFill(Color.rgb(255, 255, 255, 0.8));
                gc.fillOval(c.x, screenY, 60, 40);
                gc.fillOval(c.x + 30, screenY - 20, 80, 60);
                gc.fillOval(c.x + 70, screenY, 60, 40);
            }
        }
    }

    private void drawItems(GraphicsContext gc) {
        for (Collectible c : items) {
            double screenY = H * 0.7 - (c.dist - camY);
            if (screenY > -50 && screenY < H + 50) {
                if (c.type == ItemType.COIN) drawCoin(gc, c.x, screenY);
                else if (c.type == ItemType.QBLOCK) drawQBlock(gc, c.x, screenY, c.hit);
                else if (c.type == ItemType.PIPE) drawPipe(gc, c.x, screenY);
            }
        }
    }

    private void drawCoin(GraphicsContext gc, double x, double y) {
        double bounce = Math.sin(time * 6 + x) * 4;
        y += bounce;
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillOval(x - 12, y + 12, 24, 12);

        gc.setFill(Color.GOLD);
        gc.fillOval(x - 14, y - 14, 28, 28);
        gc.setStroke(Color.DARKGOLDENROD);
        gc.setLineWidth(2);
        gc.strokeOval(x - 14, y - 14, 28, 28);

        gc.setFill(Color.YELLOW);
        gc.fillOval(x - 6, y - 8, 10, 10);
    }

    private void drawQBlock(GraphicsContext gc, double x, double y, boolean hit) {
        double bounce = Math.abs(Math.sin(time * 4 + x)) * 6;
        y -= bounce;

        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillRect(x - 18, y + 22, 36, 12);

        gc.setFill(hit ? Color.DARKGRAY : Color.SADDLEBROWN);
        gc.fillRect(x - 22, y - 22, 44, 44);
        gc.setFill(hit ? Color.GRAY : Color.PERU);
        gc.fillRect(x - 18, y - 18, 36, 36);

        if (!hit) {
            gc.setFill(Color.YELLOW);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 28));
            gc.fillText("?", x - 9, y + 10);
        }
    }

    private void drawPipe(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillOval(x - 28, y + 22, 56, 20);

        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(x - 22, y - 10, 44, 45);
        gc.setFill(Color.GREEN);
        gc.fillRect(x - 28, y - 25, 56, 20);

        gc.setFill(Color.rgb(255, 255, 255, 0.4));
        gc.fillRect(x - 18, y - 22, 12, 14);
        gc.fillRect(x - 15, y - 5, 10, 35);
    }

    private void drawKart(GraphicsContext gc, Kart k) {
        double screenY = H * 0.7 - (k.dist - camY);
        double screenX = k.x;
        if (screenY < -100 || screenY > H + 100) return;

        gc.save();
        gc.translate(screenX, screenY - k.jumpHeight);
        gc.rotate(k.angle);

        // Squash and Stretch Animation
        double squash = 1.0, stretch = 1.0;
        if (k.isJumping) {
            stretch = 1.0 + (k.jumpVel * 0.04);
            squash = 1.0 - (k.jumpVel * 0.02);
        }
        gc.scale(squash, stretch);

        // Shadow
        gc.setFill(Color.rgb(0, 0, 0, 0.4));
        gc.fillOval(-25, -15 + k.jumpHeight, 50, 40);

        // Wheels
        gc.setFill(Color.BLACK);
        gc.fillRect(-30, -22, 10, 18);
        gc.fillRect(20, -22, 10, 18);
        gc.fillRect(-30, 10, 10, 18);
        gc.fillRect(20, 10, 10, 18);

        // Kart Body (Gradient for 3D effect)
        LinearGradient bodyGrad = new LinearGradient(0, -25, 0, 25, false, CycleMethod.NO_CYCLE,
                new Stop(0, k.color.brighter()), new Stop(1, k.color.darker()));
        gc.setFill(bodyGrad);
        gc.fillRoundRect(-22, -28, 44, 56, 12, 12);

        // Driver (Mario/Luigi style)
        gc.setFill(Color.rgb(255, 220, 180)); // Face
        gc.fillOval(-12, -22, 24, 24);

        gc.setFill(k.isMario ? Color.RED : Color.GREEN); // Hat
        gc.fillOval(-14, -28, 28, 18);
        gc.fillRect(-16, -16, 32, 6); // Brim

        gc.setFill(Color.BLACK); // Eyes
        gc.fillOval(-6, -14, 4, 6);
        gc.fillOval(2, -14, 4, 6);

        gc.fillOval(-9, -6, 18, 6); // Mustache

        // Boost Effect
        if (k.boostTime > 0) {
            gc.setFill(Color.rgb(255, 255, 0, 0.5));
            gc.fillOval(-30, 20, 60, 30);
        }

        gc.restore();
    }

    private void drawParticles(GraphicsContext gc) {
        for (Particle p : particles) {
            double screenY = H * 0.7 - (p.dist - camY);
            double alpha = p.life / p.maxLife;
            gc.setFill(p.color.deriveColor(0, 1, 1, alpha));
            gc.fillOval(p.x - p.size / 2, screenY - p.size / 2, p.size, p.size);
        }
    }

    private void drawHUD(GraphicsContext gc) {
        gc.setFill(Color.rgb(0, 0, 0, 0.6));
        gc.fillRect(20, 20, 300, 25);
        gc.fillRect(20, 55, 300, 25);

        gc.setFill(p1.color);
        gc.fillRect(20, 20, (p1.dist / TRACK_LEN) * 300, 25);
        gc.setFill(p2.color);
        gc.fillRect(20, 55, (p2.dist / TRACK_LEN) * 300, 25);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 16));
        gc.fillText("P1 Coins: " + p1.coins, 330, 38);
        gc.fillText((vsAI ? "AI" : "P2") + " Coins: " + p2.coins, 330, 73);
    }

    private void drawFinish(GraphicsContext gc) {
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, W, H);
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial", 60));
        String winner = p1.dist >= p2.dist ? "PLAYER 1 WINS!" : (vsAI ? "COMPUTER WINS!" : "PLAYER 2 WINS!");
        gc.fillText(winner, 250, 350);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 30));
        gc.fillText("Press SPACE for Menu", 320, 450);
    }

    // --- HELPERS & MATH ---

    public double getRoadCenterX(double dist) {
        return W / 2 + Math.sin(dist * 0.002) * 300 + Math.sin(dist * 0.005) * 150;
    }

    public void spawnDriftSmoke(double x, double dist, double angle) {
        particles.add(new Particle(x, dist, Color.GRAY, 0.5, 15));
    }

    public void spawnSparkles(double x, double dist, Color c) {
        for (int i = 0; i < 10; i++) {
            particles.add(new Particle(x + rand.nextGaussian() * 10, dist + rand.nextGaussian() * 10, c, 1.0, 8));
        }
    }

    // --- INNER CLASSES ---

    class Kart {
        double x, dist, speed, angle, steerInput;
        double jumpHeight, jumpVel;
        boolean isJumping, accelInput, brakeInput, jumpInput;
        double boostTime, stunTime;
        int coins;
        Color color;
        boolean isMario;
        String name;

        public Kart(double x, double dist, Color color, boolean isMario, String name) {
            this.x = x; this.dist = dist; this.color = color;
            this.isMario = isMario; this.name = name;
        }

        public void update(double dt, MarioStyleRacing game) {
            if (stunTime > 0) { stunTime -= dt; speed *= 0.9; }
            else {
                if (accelInput) speed += 6.0 * dt;
                if (brakeInput) speed -= 8.0 * dt;
            }

            if (boostTime > 0) {
                boostTime -= dt;
                speed = Math.max(speed, MAX_SPEED * 1.4);
            }

            speed *= 0.98;
            double maxSpd = boostTime > 0 ? MAX_SPEED * 1.6 : MAX_SPEED;
            speed = Math.max(-MAX_SPEED / 2, Math.min(speed, maxSpd));

            double turnRate = steerInput * 140 * dt * (speed / MAX_SPEED);
            angle += turnRate;
            angle = Math.max(-60, Math.min(60, angle));
            if (steerInput == 0) angle *= 0.9;

            if (jumpInput && !isJumping && stunTime <= 0) {
                isJumping = true;
                jumpVel = 250;
            }
            if (isJumping) {
                jumpHeight += jumpVel * dt;
                jumpVel -= 600 * dt;
                if (jumpHeight <= 0) {
                    jumpHeight = 0;
                    isJumping = false;
                    speed *= 1.1;
                }
            }

            double rad = Math.toRadians(angle);
            x += Math.sin(rad) * speed * dt * 60;
            dist += Math.cos(rad) * speed * dt * 60;

            double center = game.getRoadCenterX(dist);
            if (Math.abs(x - center) > ROAD_W / 2 && !isJumping) {
                speed *= 0.96;
                if (Math.abs(steerInput) > 0.5) game.spawnDriftSmoke(x, dist - 20, angle);
            }
        }
    }

    class Collectible {
        double x, dist;
        ItemType type;
        boolean hit;
        public Collectible(double x, double dist, ItemType type) {
            this.x = x; this.dist = dist; this.type = type;
        }
    }

    class Particle {
        double x, dist, vx, vy, life, maxLife, size;
        Color color;
        public Particle(double x, double dist, Color color, double maxLife, double size) {
            this.x = x; this.dist = dist; this.color = color;
            this.maxLife = maxLife; this.life = maxLife; this.size = size;
            this.vx = (rand.nextDouble() - 0.5) * 50;
            this.vy = (rand.nextDouble() - 0.5) * 50;
        }
        public void update(double dt) {
            life -= dt;
            x += vx * dt;
            dist += vy * dt;
            size += dt * 20;
        }
    }

    class Cloud {
        double x, dist;
        public Cloud(double x, double dist) { this.x = x; this.dist = dist; }
    }

    public static void main(String[] args) { launch(args); }
}