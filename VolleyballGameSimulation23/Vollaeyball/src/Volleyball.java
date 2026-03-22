import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.VPos;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.*;

public class VolleyballGame extends Application {

    // Scene and court
    static final int WIDTH = 1280;
    static final int HEIGHT = 720;
    static final double GROUND_Y = HEIGHT - 90.0;
    static final double NET_H = 280.0;
    static final double NET_W = 14.0;
    static final double NET_X = WIDTH * 0.5 - NET_W * 0.5;
    static final double NET_TOP = GROUND_Y - NET_H;

    // Physics
    static final double GRAVITY = 2200.0;
    static final double BALL_RADIUS = 18.0;
    static final double PLAYER_RADIUS = 40.0;
    static final double GROUND_BOUNCE = 0.69;
    static final double WALL_BOUNCE = 0.9;
    static final double PLAYER_BOUNCE = 0.9;
    static final double NET_BOUNCE = 0.7;
    static final double AIR_DRAG = 0.0008; // proportional drag for ball
    static final double PLAYER_MOVE_ACCEL = 9000.0;
    static final double PLAYER_MAX_SPEED = 500.0;
    static final double PLAYER_FRICTION = 13.0; // per second
    static final double JUMP_IMPULSE = 1050.0;
    static final double SERVE_SPEED = 550.0;

    // Visuals
    static final int TRAIL_LENGTH = 30;
    static final int MAX_PARTICLES = 300;
    static final double SHADOW_SCALE_Y = 0.35;
    static final Color SAND = Color.web("#e9cf90");
    static final Color SKY_TOP = Color.web("#65c7ff");
    static final Color SKY_BOT = Color.web("#f9fbff");
    static final Color SUN = Color.web("#ffe28a");
    static final Color NET_COLOR = Color.web("#fafafa");
    static final Color NET_POST = Color.web("#78808a");
    static final Font UI_FONT = Font.font("Arial", 26);
    static final Font SCORE_FONT = Font.font("Arial", 72);

    // Game state
    Canvas canvas;
    GraphicsContext g;
    boolean leftPressed, rightPressed, upPressed;
    boolean aPressed, dPressed, wPressed;
    boolean paused = false;
    boolean vsCPU = true;

    Random rng = new Random(1);

    Ball ball;
    Player p1, p2;
    int scoreL = 0, scoreR = 0;
    boolean awaitingServe = true;
    int serveSide = 0; // 0 = left serve, 1 = right serve
    double lastTime;
    Deque<TrailPoint> ballTrail = new ArrayDeque<>();
    ArrayList<Particle> particles = new ArrayList<>();
    ArrayList<Cloud> clouds = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        canvas = new Canvas(WIDTH, HEIGHT);
        g = canvas.getGraphicsContext2D();

        Group root = new Group(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT, Color.BLACK);

        initGame();

        // Input
        scene.setOnKeyPressed(e -> {
            KeyCode k = e.getCode();
            if (k == KeyCode.A) aPressed = true;
            if (k == KeyCode.D) dPressed = true;
            if (k == KeyCode.W) wPressed = true;
            if (k == KeyCode.LEFT) leftPressed = true;
            if (k == KeyCode.RIGHT) rightPressed = true;
            if (k == KeyCode.UP) upPressed = true;
            if (k == KeyCode.SPACE) if (awaitingServe) doServe();
            if (k == KeyCode.P) paused = !paused;
            if (k == KeyCode.R) resetMatch();
            if (k == KeyCode.M) vsCPU = !vsCPU;
        });
        scene.setOnKeyReleased(e -> {
            KeyCode k = e.getCode();
            if (k == KeyCode.A) aPressed = false;
            if (k == KeyCode.D) dPressed = false;
            if (k == KeyCode.W) wPressed = false;
            if (k == KeyCode.LEFT) leftPressed = false;
            if (k == KeyCode.RIGHT) rightPressed = false;
            if (k == KeyCode.UP) upPressed = false;
        });

        stage.setTitle("Volleyball Game - JavaFX");
        stage.setScene(scene);
        stage.show();

        lastTime = System.nanoTime() * 1e-9;
        new AnimationTimer() {
            @Override public void handle(long nowNanos) {
                double now = nowNanos * 1e-9;
                double dt = Math.min(0.033, Math.max(0.0, now - lastTime));
                lastTime = now;
                if (!paused) update(dt);
                render();
            }
        }.start();
    }

    void initGame() {
        p1 = new Player(WIDTH * 0.25, GROUND_Y - PLAYER_RADIUS, true);
        p2 = new Player(WIDTH * 0.75, GROUND_Y - PLAYER_RADIUS, false);
        ball = new Ball(WIDTH * 0.25, GROUND_Y - 300.0);
        awaitingServe = true;
        scoreL = scoreR = 0;
        serveSide = rng.nextBoolean() ? 0 : 1;
        particles.clear();
        ballTrail.clear();
        clouds.clear();
        for (int i = 0; i < 5; i++) {
            clouds.add(new Cloud(rng.nextDouble() * WIDTH, 80 + rng.nextDouble()*120, 60 + rng.nextDouble()*80, 0.8 + rng.nextDouble()*1.2));
        }
    }

    void resetMatch() {
        initGame();
    }

    void doServe() {
        awaitingServe = false;
        double x = (serveSide == 0) ? (WIDTH * 0.25) : (WIDTH * 0.75);
        ball.x = x;
        ball.y = GROUND_Y - 220;
        ball.vx = (serveSide == 0) ? SERVE_SPEED : -SERVE_SPEED;
        ball.vy = -350.0 - rng.nextDouble() * 150.0;
        ballTrail.clear();
        spawnBurst(ball.x, GROUND_Y - 2, 15, SAND.deriveColor(0,1,1,0.8), 300, 250);
    }

    void pointTo(int side) {
        if (side == 0) scoreL++; else scoreR++;
        serveSide = 1 - side; // side that lost serves
        awaitingServe = true;
        ball.reset();
        p1.reset(true);
        p2.reset(false);
        spawnBurst(ball.x, GROUND_Y - 2, 22, Color.web("#ffffff", 0.45), 320, 290);
    }

    void update(double dt) {
        // Clouds drift
        for (Cloud c : clouds) {
            c.x += 12 * dt;
            if (c.x - c.w > WIDTH + 80) c.x = -c.w - 80;
        }

        // Players input
        p1.inputLeft = aPressed;
        p1.inputRight = dPressed;
        p1.inputJump = wPressed;

        if (!vsCPU) {
            p2.inputLeft = leftPressed;
            p2.inputRight = rightPressed;
            p2.inputJump = upPressed;
        } else {
            cpuControl(p2, dt);
        }

        p1.update(dt, true);
        p2.update(dt, false);

        // Ball update
        if (!awaitingServe) {
            ball.update(dt);
        } else {
            // make the ball float above server side for a gentle idle animation
            double btargetX = (serveSide == 0) ? (WIDTH * 0.25) : (WIDTH * 0.75);
            double t = (System.nanoTime() * 1e-9);
            ball.x = btargetX + Math.sin(t*1.6) * 6.0;
            ball.y = GROUND_Y - 260.0 + Math.cos(t*1.2) * 6.0;
            ball.vx = ball.vy = 0;
        }

        // Ball collisions world
        ballCollideWorld(dt);

        // Ball vs players
        collideBallPlayer(ball, p1);
        collideBallPlayer(ball, p2);

        // Ball vs net
        collideBallNet(ball);

        // Scoring: ball touches ground on a side
        if (ball.y + BALL_RADIUS >= GROUND_Y - 0.5 && Math.abs(ball.vy) < 50) { // resting on ground
            if (!awaitingServe) {
                int side = (ball.x < WIDTH * 0.5) ? 1 : 0; // point to opposite side of where it fell
                pointTo(side);
            }
        }

        // Trail
        ballTrail.addFirst(new TrailPoint(ball.x, ball.y, 1.0));
        while (ballTrail.size() > TRAIL_LENGTH) ballTrail.removeLast();
        int i = 0;
        for (TrailPoint tp : ballTrail) {
            tp.life = 1.0 - (i / (double) TRAIL_LENGTH);
            i++;
        }

        // Particles
        for (int k = particles.size()-1; k >= 0; --k) {
            Particle p = particles.get(k);
            p.vy += (p.gravity ? GRAVITY*0.5 : 0) * dt;
            p.vx *= Math.pow(1.0 - 0.8*dt, 1);
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.life -= dt;
            if (p.y > GROUND_Y - 2 && p.vy > 0) {
                p.y = GROUND_Y - 2;
                p.vy *= -0.25;
                p.vx *= 0.6;
                p.life *= 0.75;
            }
            if (p.life <= 0) particles.remove(k);
        }
        while (particles.size() > MAX_PARTICLES) particles.remove(0);
    }

    void ballCollideWorld(double dt) {
        // Gravity and drag integrated in Ball.update

        // Ground
        if (ball.y + BALL_RADIUS > GROUND_Y) {
            ball.y = GROUND_Y - BALL_RADIUS;
            if (ball.vy > 0) {
                if (Math.abs(ball.vy) > 400) {
                    spawnBurst(ball.x, GROUND_Y - 2, 18, SAND.deriveColor(0,1,1,0.65), 280, 220);
                }
                ball.vy *= -GROUND_BOUNCE;
                ball.vx *= 0.96;
            }
        }

        // Ceiling
        if (ball.y - BALL_RADIUS < 0) {
            ball.y = BALL_RADIUS;
            if (ball.vy < 0) ball.vy *= -WALL_BOUNCE;
        }

        // Walls
        if (ball.x - BALL_RADIUS < 0) {
            ball.x = BALL_RADIUS;
            if (ball.vx < 0) ball.vx *= -WALL_BOUNCE;
        } else if (ball.x + BALL_RADIUS > WIDTH) {
            ball.x = WIDTH - BALL_RADIUS;
            if (ball.vx > 0) ball.vx *= -WALL_BOUNCE;
        }
    }

    void collideBallNet(Ball b) {
        // Rect for net
        double rx = NET_X, ry = NET_TOP, rw = NET_W, rh = NET_H;
        // Ball vs AABB
        double cx = clamp(b.x, rx, rx + rw);
        double cy = clamp(b.y, ry, ry + rh);
        double dx = b.x - cx;
        double dy = b.y - cy;
        double dist2 = dx*dx + dy*dy;
        double r = BALL_RADIUS;
        if (dist2 < r*r) {
            double dist = Math.max(1e-6, Math.sqrt(dist2));
            double nx = dx / dist;
            double ny = dy / dist;
            // push out
            double pen = r - dist;
            b.x += nx * pen;
            b.y += ny * pen;

            // reflect velocity on normal
            double vn = b.vx * nx + b.vy * ny;
            if (vn < 0) {
                b.vx -= (1 + NET_BOUNCE) * vn * nx;
                b.vy -= (1 + NET_BOUNCE) * vn * ny;
            }
            spawnSparks(b.x, b.y, 8, Color.web("#ffffff", 0.65));
        }
    }

    void collideBallPlayer(Ball b, Player p) {
        double dx = b.x - p.x;
        double dy = b.y - p.y;
        double rr = BALL_RADIUS + PLAYER_RADIUS * (p.airSquish ? 0.95 : 1.0);
        double dist2 = dx*dx + dy*dy;
        if (dist2 < rr*rr) {
            double dist = Math.max(1e-6, Math.sqrt(dist2));
            double nx = dx / dist;
            double ny = dy / dist;
            double pen = rr - dist;
            // push ball out
            b.x += nx * pen;
            b.y += ny * pen;

            // relative velocity along normal
            double rvx = b.vx - p.vx;
            double rvy = b.vy - p.vy;
            double vn = rvx * nx + rvy * ny;
            if (vn < 0) {
                double j = -(1 + PLAYER_BOUNCE) * vn;
                // add some "control" based on player's movement intent
                double bonus = (p.isLeft ? (p.inputRight ? 1 : (p.inputLeft ? -0.5 : 0))
                        : (p.inputLeft ? -1 : (p.inputRight ? 0.5 : 0)));
                double spinX = bonus * 120.0;
                b.vx += (-j * nx) + spinX;
                b.vy += (-j * ny);
                // transfer part of player's velocity
                b.vx += 0.18 * p.vx;
                b.vy += 0.08 * p.vy;

                // landing particles
                spawnSparks(b.x, b.y, 10, Color.web("#fffbd8", 0.65));
            }
        }
    }

    void cpuControl(Player cpu, double dt) {
        // Simple heuristic: chase predicted landing x when ball is on CPU side or coming over
        cpu.inputLeft = cpu.inputRight = cpu.inputJump = false;

        // Home position
        double homeX = WIDTH * 0.82;

        // Predict landing if ball is moving towards CPU or above net
        boolean ballRightSide = ball.x > WIDTH * 0.5;
        boolean ballComingRight = ball.vx > 0;
        double targetX = homeX;

        if (!awaitingServe) {
            if ((ballComingRight && !ballRightSide) || ballRightSide) {
                // Predict simple ballistic landing ignoring net after current y
                double t = timeToY(ball.y, ball.vy, GROUND_Y - BALL_RADIUS, GRAVITY);
                if (t > 0) {
                    double dragFactor = Math.exp(-AIR_DRAG * t);
                    double landingX = ball.x + (ball.vx / Math.max(1e-6, AIR_DRAG)) * (1 - dragFactor);
                    landingX = clamp(landingX, WIDTH * 0.5 + PLAYER_RADIUS + NET_W, WIDTH - PLAYER_RADIUS);
                    targetX = landingX - 40; // bias left of landing to hit upwards
                }
            }
        }
        targetX = clamp(targetX, WIDTH * 0.5 + PLAYER_RADIUS + NET_W, WIDTH - PLAYER_RADIUS);

        // Move toward target
        if (cpu.x < targetX - 12) cpu.inputRight = true;
        else if (cpu.x > targetX + 12) cpu.inputLeft = true;

        // Jump decision: if ball is near horizontally and descending at hittable height
        boolean inRangeX = Math.abs(cpu.x - ball.x) < (PLAYER_RADIUS + BALL_RADIUS + 14);
        boolean descending = ball.vy > 30;
        boolean goodHeight = ball.y < cpu.y - 20; // above head-ish
        if (!awaitingServe && inRangeX && descending && goodHeight) {
            cpu.inputJump = true;
        }
    }

    double timeToY(double y0, double vy0, double yTarget, double g) {
        // Solve y0 + vy0*t + 0.5*g*t^2 = yTarget  ->  0.5*g*t^2 + vy0*t + (y0 - yTarget) = 0
        double a = 0.5 * g, b = vy0, c = (y0 - yTarget);
        double disc = b*b - 4*a*c;
        if (disc < 0) return -1;
        double sqrt = Math.sqrt(disc);
        double t1 = (-b + sqrt) / (2*a);
        double t2 = (-b - sqrt) / (2*a);
        double t = Double.MAX_VALUE;
        if (t1 > 0) t = Math.min(t, t1);
        if (t2 > 0) t = Math.min(t, t2);
        return t == Double.MAX_VALUE ? -1 : t;
    }

    void spawnBurst(double x, double y, int count, Color col, double spd, double spread) {
        for (int i = 0; i < count; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double r = rng.nextDouble();
            double v = spd * (0.4 + 0.6*r);
            double vx = Math.cos(a) * v;
            double vy = Math.sin(a) * v - spread * 0.6;
            particles.add(new Particle(x, y, vx, vy, col, 0.9 + rng.nextDouble()*0.6, true));
        }
    }
    void spawnSparks(double x, double y, int count, Color col) {
        for (int i = 0; i < count; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double v = 140 + rng.nextDouble()*160;
            double vx = Math.cos(a) * v;
            double vy = Math.sin(a) * v - 60;
            particles.add(new Particle(x, y, vx, vy, col, 0.35 + rng.nextDouble()*0.35, false));
        }
    }

    void render() {
        // Background gradient sky
        Stop[] stops = new Stop[] { new Stop(0, SKY_TOP), new Stop(1, SKY_BOT) };
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Sun
        drawSun();

        // Clouds
        for (Cloud c : clouds) drawCloud(c);

        // Ground
        drawGround();

        // Net
        drawNet();

        // Shadows
        drawShadow(ball.x, GROUND_Y - 2, BALL_RADIUS * 1.2, BALL_RADIUS * SHADOW_SCALE_Y, Color.rgb(0,0,0,50));
        drawShadow(p1.x, GROUND_Y - 4, PLAYER_RADIUS * 1.6, PLAYER_RADIUS * SHADOW_SCALE_Y*1.8, Color.rgb(0,0,0,45));
        drawShadow(p2.x, GROUND_Y - 4, PLAYER_RADIUS * 1.6, PLAYER_RADIUS * SHADOW_SCALE_Y*1.8, Color.rgb(0,0,0,45));

        // Particles behind
        for (Particle p : particles) drawParticle(p);

        // Ball trail
        drawTrail();

        // Players
        drawPlayer(p1, Color.web("#ff6f61"), Color.web("#ffb9b3"));
        drawPlayer(p2, Color.web("#4a90e2"), Color.web("#b9d4ff"));

        // Ball
        drawBall();

        // Foreground particles
        // (already drawn)

        // UI overlay
        drawUI();
    }

    void drawSun() {
        double x = WIDTH * 0.12, y = HEIGHT * 0.16, r = 60;
        RadialGradient rg = new RadialGradient(0, 0, x, y, r*2.2, false, CycleMethod.NO_CYCLE,
                new Stop(0, SUN),
                new Stop(1, Color.TRANSPARENT));
        g.setFill(rg);
        g.fillOval(x - r*2.2, y - r*2.2, r*4.4, r*4.4);
        g.setFill(SUN);
        g.fillOval(x - r, y - r, r*2, r*2);
    }

    void drawCloud(Cloud c) {
        g.setGlobalAlpha(0.8);
        g.setFill(Color.rgb(255,255,255, 0.9));
        double x = c.x, y = c.y, w = c.w, h = c.w * 0.6;
        g.fillOval(x - w*0.6, y - h*0.5, w*0.9, h);
        g.fillOval(x - w*0.1, y - h*0.6, w*0.8, h*1.1);
        g.fillOval(x,           y - h*0.45, w*0.9, h*0.95);
        g.setGlobalAlpha(1.0);
    }

    void drawGround() {
        // Sand gradient
        Stop[] sandStops = new Stop[] { new Stop(0, SAND.brighter()), new Stop(1, SAND) };
        g.setFill(new LinearGradient(0, GROUND_Y-100, 0, HEIGHT, false, CycleMethod.NO_CYCLE, sandStops));
        g.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        // Court line
        g.setStroke(Color.rgb(255,255,255,0.55));
        g.setLineWidth(3.0);
        g.strokeLine(10, GROUND_Y+1.5, WIDTH-10, GROUND_Y+1.5);

        // Subtle texture lines
        g.setStroke(Color.rgb(190,160,90,0.25));
        g.setLineWidth(1.0);
        for (int i=0;i<6;i++) {
            double y = GROUND_Y + 10 + i*12;
            g.strokeLine(0, y, WIDTH, y+2);
        }
    }

    void drawNet() {
        // Posts
        g.setFill(NET_POST);
        g.fillRect(NET_X-6, NET_TOP-14, 8, NET_H+18);
        g.fillRect(NET_X+NET_W-2, NET_TOP-14, 8, NET_H+18);

        // Mesh
        g.setFill(NET_COLOR);
        g.fillRect(NET_X, NET_TOP, NET_W, NET_H);

        g.setStroke(Color.rgb(120,120,120,0.45));
        g.setLineWidth(1.0);
        // Horizontal mesh lines
        for (double y=NET_TOP+8; y<GROUND_Y; y+=16) {
            g.strokeLine(NET_X-12, y, NET_X+NET_W+12, y);
        }
        // Vertical side lines
        g.setStroke(Color.rgb(120,120,120,0.7));
        g.strokeLine(NET_X, NET_TOP-6, NET_X, GROUND_Y);
        g.strokeLine(NET_X+NET_W, NET_TOP-6, NET_X+NET_W, GROUND_Y);

        // Tape on top
        g.setFill(Color.WHITE);
        g.fillRect(NET_X-14, NET_TOP-8, NET_W+28, 10);
    }

    void drawShadow(double x, double y, double rx, double ry, Color c) {
        g.setFill(c);
        g.fillOval(x - rx, y - ry, rx*2, ry*2);
    }

    void drawPlayer(Player p, Color body, Color accent) {
        // Body circle
        g.setFill(body);
        g.fillOval(p.x - PLAYER_RADIUS, p.y - PLAYER_RADIUS, PLAYER_RADIUS*2, PLAYER_RADIUS*2);

        // Accent shine
        g.setFill(accent);
        g.fillOval(p.x - PLAYER_RADIUS*0.6, p.y - PLAYER_RADIUS*0.9, PLAYER_RADIUS*0.9, PLAYER_RADIUS*0.9);

        // Face line
        g.setStroke(Color.rgb(0,0,0,0.25));
        g.setLineWidth(2.0);
        g.strokeArc(p.x - PLAYER_RADIUS*0.7, p.y - PLAYER_RADIUS*0.2, PLAYER_RADIUS*1.4, PLAYER_RADIUS*1.0, 200, 140, ArcType.OPEN);

        // Jersey stripe
        g.setStroke(Color.rgb(255,255,255,0.6));
        g.setLineWidth(3.0);
        g.strokeLine(p.x - PLAYER_RADIUS*0.9, p.y + PLAYER_RADIUS*0.2, p.x + PLAYER_RADIUS*0.9, p.y + PLAYER_RADIUS*0.2);
    }

    void drawBall() {
        // Trail already drawn
        // Ball with radial shading
        RadialGradient rg = new RadialGradient(0, 0, ball.x - BALL_RADIUS*0.4, ball.y - BALL_RADIUS*0.4,
                BALL_RADIUS*1.3, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(1, Color.web("#eeeeee")));
        g.setFill(rg);
        g.fillOval(ball.x - BALL_RADIUS, ball.y - BALL_RADIUS, BALL_RADIUS*2, BALL_RADIUS*2);

        // Subtle panels
        g.setStroke(Color.rgb(0,0,0,0.15));
        g.setLineWidth(1.2);
        for (int i = 0; i < 6; i++) {
            double ang = i * Math.PI / 3.0 + (ball.vx*0.002 + ball.vy*0.002);
            double cx = ball.x + Math.cos(ang) * BALL_RADIUS * 0.4;
            double cy = ball.y + Math.sin(ang) * BALL_RADIUS * 0.4;
            g.strokeOval(cx - BALL_RADIUS*0.6, cy - BALL_RADIUS*0.6, BALL_RADIUS*1.2, BALL_RADIUS*1.2);
        }

        // Outline
        g.setStroke(Color.rgb(0,0,0,0.25));
        g.setLineWidth(1.0);
        g.strokeOval(ball.x - BALL_RADIUS, ball.y - BALL_RADIUS, BALL_RADIUS*2, BALL_RADIUS*2);
    }

    void drawTrail() {
        int i = 0;
        for (TrailPoint tp : ballTrail) {
            double a = tp.life * 0.22;
            double r = BALL_RADIUS * (0.7 + 0.3 * (1 - tp.life));
            g.setFill(Color.rgb(255,255,255, a));
            g.fillOval(tp.x - r, tp.y - r, r*2, r*2);
            i++;
        }
    }

    void drawParticle(Particle p) {
        double a = Math.max(0, Math.min(1.0, p.life));
        g.setFill(Color.color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), a));
        double r = 2.5 + 3.5 * a;
        g.fillOval(p.x - r, p.y - r, r*2, r*2);
    }

    void drawUI() {
        // Scores
        g.setFont(SCORE_FONT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.TOP);
        g.setFill(Color.rgb(255,255,255,0.95));
        g.fillText(String.valueOf(scoreL), WIDTH*0.25, 20);
        g.fillText(String.valueOf(scoreR), WIDTH*0.75, 20);

        // Serve indicator
        if (awaitingServe) {
            g.setFont(UI_FONT);
            g.setFill(Color.rgb(0,0,0,0.55));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("Press SPACE to serve", WIDTH*0.5 + 2, 92 + 2);
            g.setFill(Color.rgb(255,255,255,0.95));
            g.fillText("Press SPACE to serve", WIDTH*0.5, 92);
        }

        // Mode and help
        g.setFont(Font.font("Arial", 18));
        g.setTextAlign(TextAlignment.LEFT);
        g.setFill(Color.rgb(255,255,255,0.9));
        String mode = vsCPU ? "Mode: VS Computer (press M to switch)" : "Mode: 2 Players (press M to switch)";
        g.fillText(mode, 20, 20 + SCORE_FONT.getSize()*0.1);
        g.fillText("P1: A/D move, W jump | P2: Arrow keys | R: Reset | P: Pause", 20, 44);

        if (paused) {
            g.setFont(SCORE_FONT);
            g.setTextAlign(TextAlignment.CENTER);
            g.setFill(Color.rgb(0,0,0,0.5));
            g.fillText("Paused", WIDTH*0.5 + 3, HEIGHT*0.5 + 3);
            g.setFill(Color.rgb(255,255,255,0.95));
            g.fillText("Paused", WIDTH*0.5, HEIGHT*0.5);
        }
    }

    // Entities and helpers

    static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    class Player {
        double x, y, vx, vy;
        boolean isLeft;
        boolean onGround = true;
        boolean inputLeft, inputRight, inputJump;
        boolean airSquish = false;

        Player(double x, double y, boolean isLeft) {
            this.x = x; this.y = y; this.isLeft = isLeft;
        }

        void reset(boolean left) {
            isLeft = left;
            x = left ? WIDTH*0.25 : WIDTH*0.75;
            y = GROUND_Y - PLAYER_RADIUS;
            vx = vy = 0;
            onGround = true;
            inputLeft = inputRight = inputJump = false;
        }

        void update(double dt, boolean clampLeftSide) {
            // Horizontal control
            double ax = 0;
            if (inputLeft && !inputRight) ax -= PLAYER_MOVE_ACCEL;
            if (inputRight && !inputLeft) ax += PLAYER_MOVE_ACCEL;

            vx += ax * dt;
            // Friction
            if (Math.abs(ax) < 1e-3) {
                double f = Math.exp(-PLAYER_FRICTION * dt);
                vx *= f;
            }
            // Clamp speed
            vx = clamp(vx, -PLAYER_MAX_SPEED, PLAYER_MAX_SPEED);

            // Jump
            if (inputJump && onGround) {
                vy = -JUMP_IMPULSE;
                onGround = false;
            }

            // Gravity
            vy += GRAVITY * dt;

            // Integrate
            x += vx * dt;
            y += vy * dt;

            // Ground collision
            if (y + PLAYER_RADIUS > GROUND_Y) {
                y = GROUND_Y - PLAYER_RADIUS;
                if (vy > 0) vy = 0;
                onGround = true;
            } else {
                onGround = false;
            }

            // Walls and half-court restriction
            double minX = clampLeftSide ? PLAYER_RADIUS : WIDTH*0.5 + PLAYER_RADIUS + NET_W;
            double maxX = clampLeftSide ? WIDTH*0.5 - PLAYER_RADIUS - NET_W : WIDTH - PLAYER_RADIUS;
            x = clamp(x, minX, maxX);

            // Ceiling
            if (y - PLAYER_RADIUS < 0) {
                y = PLAYER_RADIUS;
                vy = Math.max(0, vy);
            }

            // Minor squash visual if rising/falling
            airSquish = !onGround && (vy > 200);
        }
    }

    class Ball {
        double x, y, vx, vy;
        Ball(double x, double y) {
            this.x=x; this.y=y;
        }
        void reset() {
            x = WIDTH*0.5;
            y = NET_TOP - 60;
            vx = vy = 0;
            ballTrail.clear();
        }
        void update(double dt) {
            // gravity
            vy += GRAVITY * dt;
            // air drag
            double drag = Math.exp(-AIR_DRAG * dt);
            vx *= drag;
            vy *= drag;

            x += vx * dt;
            y += vy * dt;
        }
    }

    static class TrailPoint {
        double x,y,life;
        TrailPoint(double x,double y,double life){this.x=x;this.y=y;this.life=life;}
    }

    static class Particle {
        double x,y,vx,vy,life;
        boolean gravity;
        Color color;
        Particle(double x,double y,double vx,double vy,Color c,double life,boolean gravity){
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.color=c;this.life=life;this.gravity=gravity;
        }
    }

    static class Cloud {
        double x,y,w,speed;
        Cloud(double x,double y,double w,double speed) { this.x=x;this.y=y;this.w=w;this.speed=speed; }
    }

    public static void main(String[] args) {
        launch(args);
    }