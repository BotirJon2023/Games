import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Random;

public class VolleyballGame extends Application {

    // Canvas / world
    private final int WIDTH = 960;
    private final int HEIGHT = 540;
    private final double GROUND_Y = HEIGHT - 48;
    private final double NET_X = WIDTH / 2.0;
    private final double NET_WIDTH = 8;
    private final double NET_HEIGHT = 180;

    // Physics
    private final double GRAVITY = 1400;
    private final double PLAYER_SPEED = 270;
    private final double JUMP_SPEED = 650;
    private final double BALL_ELASTICITY = 0.78;
    private final double BALL_NET_ELASTICITY = 0.65;
    private final double AIR_FRICTION = 0.999;
    private final double MAX_BALL_SPEED = 1050;

    // Game objects
    private Player p1;
    private Player p2;
    private Ball ball;
    private Rect net;

    // State
    private boolean aiEnabled = true;
    private boolean paused = false;
    private int server = 0; // 0 = left, 1 = right
    private double roundCooldown = 1.2;
    private final Random rng = new Random(12345);

    // Input
    private boolean aLeft, aRight, aJump, aHit;
    private boolean bLeft, bRight, bJump, bHit;
    private boolean requestToggleAI;
    private boolean requestReset;
    private boolean requestPause;

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();

        initGame();

        Scene scene = new Scene(new StackPane(canvas));
        scene.setOnKeyPressed(e -> {
            KeyCode k = e.getCode();
            if (k == KeyCode.A) aLeft = true;
            if (k == KeyCode.D) aRight = true;
            if (k == KeyCode.W) aJump = true;
            if (k == KeyCode.F) aHit = true;

            if (k == KeyCode.LEFT) bLeft = true;
            if (k == KeyCode.RIGHT) bRight = true;
            if (k == KeyCode.UP) bJump = true;
            if (k == KeyCode.SLASH) bHit = true;

            if (k == KeyCode.M) requestToggleAI = true;
            if (k == KeyCode.R) requestReset = true;
            if (k == KeyCode.P) requestPause = true;
        });
        scene.setOnKeyReleased(e -> {
            KeyCode k = e.getCode();
            if (k == KeyCode.A) aLeft = false;
            if (k == KeyCode.D) aRight = false;
            if (k == KeyCode.W) aJump = false;
            if (k == KeyCode.F) aHit = false;

            if (k == KeyCode.LEFT) bLeft = false;
            if (k == KeyCode.RIGHT) bRight = false;
            if (k == KeyCode.UP) bJump = false;
            if (k == KeyCode.SLASH) bHit = false;
        });

        stage.setTitle("Volleyball - Rio View (2P or vs CPU: press M)");
        stage.setScene(scene);
        stage.show();

        final long[] last = {System.nanoTime()};

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = (now - last[0]) / 1e9;
                last[0] = now;
                dt = Math.min(dt, 1 / 55.0); // clamp

                // Handle toggles
                if (requestToggleAI) {
                    aiEnabled = !aiEnabled;
                    p2.isAI = aiEnabled;
                    requestToggleAI = false;
                }
                if (requestReset) {
                    p1.score = 0;
                    p2.score = 0;
                    server = rng.nextBoolean() ? 0 : 1;
                    resetRound();
                    requestReset = false;
                }
                if (requestPause) {
                    paused = !paused;
                    requestPause = false;
                }

                if (!paused) {
                    update(dt);
                }

                render(g);
            }
        }.start();
    }

    private void initGame() {
        p1 = new Player("LEFT", WIDTH * 0.23, PLAYER_SPEED, Color.web("#2E86DE"));
        p2 = new Player("RIGHT", WIDTH * 0.77, PLAYER_SPEED, Color.web("#E74C3C"));
        p2.isAI = aiEnabled;

        net = new Rect(NET_X - NET_WIDTH / 2, GROUND_Y - NET_HEIGHT, NET_WIDTH, NET_HEIGHT);

        ball = new Ball(WIDTH * 0.25, GROUND_Y - 200, 0, 0, 10);

        server = rng.nextBoolean() ? 0 : 1;
        roundCooldown = 1.1;
        attachBallToServer();
    }

    private void update(double dt) {
        // Countdown
        if (roundCooldown > 0) {
            roundCooldown -= dt;
            attachBallToServer();
            // Allow players to reposition during cooldown
            p1.updateHorizontal(dt, aLeft, aRight);
            p2.updateHorizontal(dt, p2.isAI ? aiMoveLeft(p2, ball) : bLeft,
                    p2.isAI ? aiMoveRight(p2, ball) : bRight);
            // Ground lock
            p1.applyGravityAndGround(dt, GROUND_Y);
            p2.applyGravityAndGround(dt, GROUND_Y);
            if (roundCooldown <= 0) serveBall();
            return;
        }

        // Inputs -> players
        p1.updateHorizontal(dt, aLeft, aRight);
        if (aJump) p1.tryJump(JUMP_SPEED, GROUND_Y);
        p1.applyGravityAndGround(dt, GROUND_Y);

        if (p2.isAI) {
            // Simple CPU control
            boolean left = aiMoveLeft(p2, ball);
            boolean right = aiMoveRight(p2, ball);
            p2.updateHorizontal(dt, left, right);
            if (aiShouldJump(p2, ball)) p2.tryJump(JUMP_SPEED, GROUND_Y);
            p2.hitPressed = aiShouldHit(p2, ball);
        } else {
            p2.updateHorizontal(dt, bLeft, bRight);
            if (bJump) p2.tryJump(JUMP_SPEED, GROUND_Y);
            p2.hitPressed = bHit;
        }
        p1.hitPressed = aHit;
        p2.applyGravityAndGround(dt, GROUND_Y);

        // Update ball physics
        ball.vy += GRAVITY * dt;
        ball.vx *= AIR_FRICTION;
        ball.vy *= AIR_FRICTION;

        ball.x += ball.vx * dt;
        ball.y += ball.vy * dt;

        clampBallSpeed();

        // Collide with walls and ceiling
        if (ball.x - ball.r < 0) {
            ball.x = ball.r;
            ball.vx = -ball.vx * BALL_ELASTICITY;
        } else if (ball.x + ball.r > WIDTH) {
            ball.x = WIDTH - ball.r;
            ball.vx = -ball.vx * BALL_ELASTICITY;
        }
        if (ball.y - ball.r < 0) {
            ball.y = ball.r;
            ball.vy = -ball.vy * BALL_ELASTICITY;
        }

        // Net collision
        collideBallWithRect(ball, net, BALL_NET_ELASTICITY, null);

        // Player collisions
        collideBallWithRect(ball, p1.getRect(), BALL_ELASTICITY, p1);
        collideBallWithRect(ball, p2.getRect(), BALL_ELASTICITY, p2);

        // Scoring: ball touches ground
        if (ball.y + ball.r >= GROUND_Y) {
            boolean leftSide = ball.x < NET_X;
            if (leftSide) {
                p2.score++;
                server = 1;
            } else {
                p1.score++;
                server = 0;
            }
            resetRound();
        }
    }

    private void resetRound() {
        // Reset positions
        p1.x = WIDTH * 0.23;
        p2.x = WIDTH * 0.77;
        p1.vx = p1.vy = 0;
        p2.vx = p2.vy = 0;
        p1.onGround = p2.onGround = true;

        roundCooldown = 1.1;
        attachBallToServer();
    }

    private void attachBallToServer() {
        Player s = server == 0 ? p1 : p2;
        double offset = server == 0 ? 28 : -28;
        ball.x = clamp(s.x + offset, ball.r, WIDTH - ball.r);
        ball.y = GROUND_Y - s.h - 28;
        ball.vx = 0;
        ball.vy = 0;
    }

    private void serveBall() {
        double dir = server == 0 ? 1 : -1;
        ball.vx = dir * (320 + rng.nextDouble() * 80);
        ball.vy = -(530 + rng.nextDouble() * 90);
    }

    private void clampBallSpeed() {
        double s2 = ball.vx * ball.vx + ball.vy * ball.vy;
        double max2 = MAX_BALL_SPEED * MAX_BALL_SPEED;
        if (s2 > max2) {
            double s = Math.sqrt(s2);
            ball.vx = (ball.vx / s) * MAX_BALL_SPEED;
            ball.vy = (ball.vy / s) * MAX_BALL_SPEED;
        }
    }

    // CPU behavior: simple heuristics
    private boolean aiMoveLeft(Player ai, Ball b) {
        double courtLeft = NET_X + 20;
        double courtRight = WIDTH - 20;
        double targetX;

        // Predict/choose a target
        if (b.x > NET_X + 10) {
            // On our side
            targetX = clamp(b.x + (b.vx * 0.25), courtLeft + 40, courtRight - 40);
        } else {
            // Prepare center-right when ball is on opponent side
            targetX = clamp(WIDTH * 0.78, courtLeft + 40, courtRight - 40);
        }
        return ai.centerX() > targetX + 12;
    }

    private boolean aiMoveRight(Player ai, Ball b) {
        double courtLeft = NET_X + 20;
        double courtRight = WIDTH - 20;
        double targetX;

        if (b.x > NET_X + 10) {
            targetX = clamp(b.x + (b.vx * 0.25), courtLeft + 40, courtRight - 40);
        } else {
            targetX = clamp(WIDTH * 0.78, courtLeft + 40, courtRight - 40);
        }
        return ai.centerX() < targetX - 12;
    }

    private boolean aiShouldJump(Player ai, Ball b) {
        // Jump when the ball is descending near and above net/top of player
        boolean closeX = Math.abs(b.x - ai.centerX()) < 62;
        boolean highBall = b.y < (GROUND_Y - ai.h - 10) + 30;
        boolean descending = b.vy > 0;
        boolean onOurSide = b.x > NET_X + 2;
        return onOurSide && closeX && highBall && descending && ai.onGround;
    }

    private boolean aiShouldHit(Player ai, Ball b) {
        double dx = b.x - ai.getRect().cx();
        double dy = b.y - ai.getRect().cy();
        double d2 = dx * dx + dy * dy;
        // Hit when close and ball is above waist height
        return d2 < 150 * 150 && b.y < (GROUND_Y - ai.h / 2.0);
    }

    private void collideBallWithRect(Ball ball, Rect rect, double elasticity, Player ownerOrNull) {
        // Find closest point on rect to ball center
        double closestX = clamp(ball.x, rect.x, rect.x + rect.w);
        double closestY = clamp(ball.y, rect.y, rect.y + rect.h);

        double dx = ball.x - closestX;
        double dy = ball.y - closestY;
        double dist2 = dx * dx + dy * dy;

        if (dist2 < ball.r * ball.r) {
            double dist = Math.sqrt(Math.max(1e-9, dist2));
            double nx, ny;
            if (dist < 1e-6) {
                // Center is exactly at corner; choose normal based on velocity
                if (Math.abs(ball.vx) > Math.abs(ball.vy)) {
                    nx = Math.signum(ball.vx);
                    ny = 0;
                } else {
                    nx = 0;
                    ny = Math.signum(ball.vy);
                }
            } else {
                nx = dx / dist;
                ny = dy / dist;
            }

            // Push ball out of penetration
            double overlap = ball.r - dist;
            ball.x += nx * overlap;
            ball.y += ny * overlap;

            // Relative velocity (consider owner's movement a bit)
            double rvx = ball.vx;
            double rvy = ball.vy;
            if (ownerOrNull != null) {
                rvx -= ownerOrNull.vx * 0.50;
                rvy -= ownerOrNull.vy * 0.15;
            }

            // Reflect relative velocity along normal
            double vn = rvx * nx + rvy * ny;
            if (vn < 0) {
                double j = -(1 + elasticity) * vn;
                rvx += j * nx;
                rvy += j * ny;
            }

            // Apply owner influence and "hit" boost
            if (ownerOrNull != null) {
                // Re-apply base owner motion
                rvx += ownerOrNull.vx * 0.30;
                rvy += ownerOrNull.vy * 0.12;

                if (ownerOrNull.hitPressed) {
                    // Directional boost (mostly outward and up)
                    rvx += nx * 340;
                    rvy += ny * 340 - 80;
                }
            }

            ball.vx = rvx;
            ball.vy = rvy;

            clampBallSpeed();
        }
    }

    private void render(GraphicsContext g) {
        // Sky gradient
        drawSky(g);
        // Distant mountains and Corcovado + statue silhouette
        drawRioSilhouette(g);

        // Ground
        g.setFill(Color.web("#2E7D32"));
        g.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        // Court line
        g.setStroke(Color.color(1, 1, 1, 0.4));
        g.setLineWidth(2);
        g.strokeLine(0, GROUND_Y, WIDTH, GROUND_Y);

        // Net
        g.setFill(Color.web("#DDDDDD"));
        g.fillRect(net.x, net.y, net.w, net.h);
        // Net top tape
        g.setFill(Color.WHITE);
        g.fillRect(net.x - 2, net.y - 6, net.w + 4, 6);

        // Players
        p1.draw(g);
        p2.draw(g);

        // Ball
        g.setFill(Color.WHITE);
        g.fillOval(ball.x - ball.r, ball.y - ball.r, ball.r * 2, ball.r * 2);
        g.setStroke(Color.color(1, 0.4, 0.2));
        g.strokeOval(ball.x - ball.r, ball.y - ball.r, ball.r * 2, ball.r * 2);

        // HUD
        g.setFill(Color.color(0, 0, 0, 0.35));
        g.fillRoundRect(WIDTH / 2.0 - 130, 10, 260, 40, 10, 10);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Arial", 22));
        g.fillText("LEFT " + p1.score + "  :  " + p2.score + " RIGHT", WIDTH / 2.0 - 105, 36);

        g.setFont(Font.font("Arial", 14));
        g.setFill(Color.color(1, 1, 1, 0.9));
        g.fillText("W/A/D jump/move, F hit | Arrows jump/move, / hit | M: toggle CPU | R: reset | P: pause",
                12, 24);

        if (aiEnabled) {
            g.setFill(Color.color(1, 1, 1, 0.9));
            g.fillText("Mode: 1P vs CPU", 12, 44);
        } else {
            g.setFill(Color.color(1, 1, 1, 0.9));
            g.fillText("Mode: 2 Players (local)", 12, 44);
        }

        if (paused) {
            g.setFill(Color.color(0, 0, 0, 0.5));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("Arial", 36));
            g.fillText("Paused", WIDTH / 2.0 - 60, HEIGHT / 2.0);
        }

        if (roundCooldown > 0) {
            g.setFill(Color.color(0, 0, 0, 0.35));
            g.fillRoundRect(WIDTH / 2.0 - 100, HEIGHT / 2.0 - 30, 200, 60, 10, 10);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("Arial", 20));
            g.fillText("Get Ready...", WIDTH / 2.0 - 60, HEIGHT / 2.0 + 5);
        }
    }

    private void drawSky(GraphicsContext g) {
        // Simple vertical gradient
        for (int i = 0; i < HEIGHT; i++) {
            double t = i / (double) HEIGHT;
            Color c = lerp(Color.web("#87CEFA"), Color.web("#FDEBD0"), t * 0.9);
            g.setStroke(c);
            g.strokeLine(0, i, WIDTH, i);
        }
    }

    private void drawRioSilhouette(GraphicsContext g) {
        // Distant mountains
        g.setFill(Color.web("#5D6D7E", 0.6));
        g.fillPolygon(
                new double[]{0, 140, 240, 400, 540, 760, 900, WIDTH, WIDTH, 0},
                new double[]{HEIGHT * 0.70, HEIGHT * 0.58, HEIGHT * 0.62, HEIGHT * 0.55, HEIGHT * 0.60, HEIGHT * 0.54, HEIGHT * 0.59, HEIGHT * 0.57, HEIGHT, HEIGHT},
                10);

        // Corcovado mountain (foreground silhouette)
        g.setFill(Color.web("#2C3E50", 0.85));
        double mBaseY = HEIGHT * 0.78;
        g.fillPolygon(
                new double[]{WIDTH * 0.58, WIDTH * 0.64, WIDTH * 0.68, WIDTH * 0.73, WIDTH * 0.80, WIDTH},
                new double[]{mBaseY, HEIGHT * 0.58, HEIGHT * 0.54, HEIGHT * 0.60, mBaseY, mBaseY},
                6);

        // Christ the Redeemer silhouette on ridge
        double cx = WIDTH * 0.66;
        double cy = HEIGHT * 0.505;
        double bodyH = 42;
        double bodyW = 12;
        double armW = 70;
        double armH = 8;
        g.setFill(Color.web("#1B2631", 0.95));
        // body
        g.fillRect(cx - bodyW / 2, cy - bodyH, bodyW, bodyH);
        // head
        g.fillOval(cx - 6, cy - bodyH - 10, 12, 12);
        // arms (outstretched)
        g.fillRect(cx - armW / 2, cy - bodyH + 8, armW, armH);
        // small pedestal
        g.fillRect(cx - 10, cy, 20, 12);
    }

    // Utils
    private static double clamp(double v, double a, double b) { return Math.max(a, Math.min(b, v)); }
    private static Color lerp(Color a, Color b, double t) {
        t = clamp(t, 0, 1);
        return new Color(
                a.getRed() + (b.getRed() - a.getRed()) * t,
                a.getGreen() + (b.getGreen() - a.getGreen()) * t,
                a.getBlue() + (b.getBlue() - a.getBlue()) * t,
                1.0
        );
    }

    // Classes
    private class Player {
        String name;
        double x; // left
        double y; // top computed from ground
        double vx, vy;
        double w = 44;
        double h = 96;
        double speed;
        boolean onGround = true;
        boolean isAI = false;
        boolean hitPressed = false;
        int score = 0;
        Color color;

        Player(String name, double x, double speed, Color color) {
            this.name = name;
            this.x = x;
            this.speed = speed;
            this.color = color;
            this.y = GROUND_Y - h;
        }

        void updateHorizontal(double dt, boolean moveLeft, boolean moveRight) {
            double dir = 0;
            if (moveLeft) dir -= 1;
            if (moveRight) dir += 1;
            vx = dir * speed;
            x += vx * dt;

            // Keep within half court
            double leftBound = name.equals("LEFT") ? 8 : NET_X + 8;
            double rightBound = name.equals("LEFT") ? NET_X - w - 8 : WIDTH - w - 8;
            x = clamp(x, leftBound, rightBound);
        }

        void tryJump(double jumpSpeed, double groundY) {
            if (onGround) {
                vy = -jumpSpeed;
                onGround = false;
            }
        }

        void applyGravityAndGround(double dt, double groundY) {
            vy += GRAVITY * dt;
            y += vy * dt;

            if (y + h >= groundY) {
                y = groundY - h;
                vy = 0;
                onGround = true;
            }
        }

        Rect getRect() { return new Rect(x, y, w, h); }
        double centerX() { return x + w / 2.0; }

        void draw(GraphicsContext g) {
            // Shadow
            g.setFill(Color.color(0, 0, 0, 0.15));
            g.fillOval(x + w * 0.1, GROUND_Y - 8, w * 0.8, 10);

            // Body
            g.setFill(color);
            g.fillRoundRect(x, y, w, h, 10, 10);

            // Jersey stripe
            g.setFill(Color.color(1, 1, 1, 0.6));
            g.fillRect(x + 6, y + h * 0.35, w - 12, 8);

            // Head
            g.setFill(Color.color(1, 0.9, 0.8, 1.0));
            g.fillOval(x + w * 0.22, y - 16, w * 0.56, 24);

            // Name tag
            g.setFill(Color.color(1, 1, 1, 0.8));
            g.setFont(Font.font("Arial", 12));
            double nx = clamp(x + w / 2 - 20, 2, WIDTH - 60);
            g.fillText(name, nx, y - 20);
        }
    }

    private static class Rect {
        double x, y, w, h;
        Rect(double x, double y, double w, double h) { this.x = x; this.y = y; this.w = w; this.h = h; }
        double cx() { return x + w / 2.0; }
        double cy() { return y + h / 2.0; }
    }

    private static class Ball {
        double x, y, vx, vy, r;
        Ball(double x, double y, double vx, double vy, double r) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.r = r;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}