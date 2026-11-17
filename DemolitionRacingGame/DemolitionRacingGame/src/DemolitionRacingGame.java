// DemolitionRacingGame.java  —  JavaFX, top-down demolition derby
// Requires JavaFX (JDK 17+: add --module-path & --add-modules javafx.controls,javafx.graphics)
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import java.util.*;

public class DemolitionRacingGame extends Application {

    static final int W = 1280, H = 720;
    static final double FRICTION = 0.92, TURN = 0.06, ACCEL = 0.35;

    // ---------- Entities ----------
    static class Car {
        double x, y, angle, vx, vy, speed, health = 100;
        boolean alive = true, isAI;
        Color color;
        // visual smoothing
        double drawX, drawY, drawAngle;
        // AI wander
        double aiTimer = 0;

        Car(double x, double y, Color c, boolean ai) {
            this.x = drawX = x; this.y = drawY = y;
            this.angle = drawAngle = Math.random() * Math.PI * 2;
            this.color = c; this.isAI = ai;
        }

        void update(double dt) {
            if (!alive) return;
            // smooth draw interpolation
            drawX += (x - drawX) * 0.35;
            drawY += (y - drawY) * 0.35;
            drawAngle += (angle - drawAngle) * 0.35;

            double fwdx = Math.cos(angle), fwdy = Math.sin(angle);
            // apply velocity
            x += vx * dt; y += vy * dt;
            // drift friction
            vx *= FRICTION; vy *= FRICTION;
            speed = Math.hypot(vx, vy);
        }

        void accelerate(double amount) {
            vx += Math.cos(angle) * amount;
            vy += Math.sin(angle) * amount;
            double max = 9.0;
            double s = Math.hypot(vx, vy);
            if (s > max) { vx = vx / s * max; vy = vy / s * max; }
        }

        void turn(double d) { angle += d * (0.4 + speed * 0.08); }

        void draw(GraphicsContext g) {
            if (!alive) return;
            g.save();
            g.translate(drawX, drawY);
            g.rotate(Math.toDegrees(drawAngle));
            // shadow
            g.setFill(Color.rgb(0,0,0,0.35));
            g.fillRoundRect(-27, -14, 54, 30, 8, 8);
            // body
            g.setFill(color);
            g.fillRoundRect(-26, -13, 52, 26, 8, 8);
            // windshield
            g.setFill(Color.rgb(180,220,255,0.85));
            g.fillRoundRect(2, -10, 14, 20, 4, 4);
            // headlights
            g.setFill(Color.YELLOW);
            g.fillOval(-24, -11, 6, 6); g.fillOval(-24, 5, 6, 6);
            g.restore();
        }
    }

    static class Particle {
        double x,y,vx,vy,life,maxLife; Color c; double size;
        Particle(double x,double y,double vx,double vy,double life,Color c,double size){
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=life;this.maxLife=life;this.c=c;this.size=size;
        }
        void update(double dt){ x+=vx*dt; y+=vy*dt; vx*=0.94; vy*=0.94; life-=dt; }
    }

    static class SkidMark { double x,y,angle; double life = 6; }

    // ---------- Game state ----------
    List<Car> cars = new ArrayList<>();
    List<Particle> particles = new ArrayList<>();
    List<SkidMark> skids = new ArrayList<>();
    Set<KeyCode> keys = new HashSet<>();
    double camX, camY, camShake;
    boolean gameOver;
    String winnerText = "";

    Car p1, p2;

    @Override public void start(Stage stage) {
        Canvas canvas = new Canvas(W, H);
        GraphicsContext g = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);

        // Player 1 (WASD)
        p1 = new Car(400, 360, Color.rgb(220,40,40), false);
        // Player 2 (Arrows) — to play vs AI, set isAI=true and skip input
        p2 = new Car(880, 360, Color.rgb(40,120,220), true);

        cars.add(p1); cars.add(p2);
        // some AI opponents
        for (int i = 0; i < 3; i++) {
            Car ai = new Car(200 + Math.random()*880, 100 + Math.random()*520,
                    Color.hsb(Math.random()*360, 0.7, 0.8), true);
            cars.add(ai);
        }

        scene.setOnKeyPressed(e -> keys.add(e.getCode()));
        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));

        camX = W/2.0; camY = H/2.0;

        AnimationTimer timer = new AnimationTimer() {
            long last = 0;
            @Override public void handle(long now) {
                if (last == 0) { last = now; return; }
                double dt = Math.min((now - last) / 1e9, 0.033);
                last = now;
                update(dt);
                render(g);
            }
        };
        timer.start();

        stage.setTitle("Demolition Racing — 2P / AI demo");
        stage.setScene(scene);
        stage.show();
        canvas.requestFocus();
    }

    // ---------- Update ----------
    void update(double dt) {
        if (gameOver) return;

        // ---- Player 1 input ----
        if (p1.alive) {
            if (keys.contains(KeyCode.W)) p1.accelerate(ACCEL);
            if (keys.contains(KeyCode.S)) p1.accelerate(-ACCEL * 0.7);
            if (keys.contains(KeyCode.A)) p1.turn(-TURN);
            if (keys.contains(KeyCode.D)) p1.turn(TURN);
            // skid marks when fast
            if (p1.speed > 3 && Math.random() < 0.6)
                skids.add(new SkidMark(p1.x - Math.cos(p1.angle)*20,
                        p1.y - Math.sin(p1.angle)*20, p1.angle));
        }

        // ---- Player 2 / AI input ----
        if (p2.alive) {
            if (p2.isAI) driveAI(p2, dt);
            else {
                if (keys.contains(KeyCode.UP)) p2.accelerate(ACCEL);
                if (keys.contains(KeyCode.DOWN)) p2.accelerate(-ACCEL * 0.7);
                if (keys.contains(KeyCode.LEFT)) p2.turn(-TURN);
                if (keys.contains(KeyCode.RIGHT)) p2.turn(TURN);
            }
        }

        for (Car c : cars) {
            if (c.isAI && c != p2 && c.alive) driveAI(c, dt);
            c.update(dt);
            // boundary
            if (c.x < 20) { c.x = 20; c.vx = Math.abs(c.vx)*0.5; camShake = 8; }
            if (c.x > W-20) { c.x = W-20; c.vx = -Math.abs(c.vx)*0.5; camShake = 8; }
            if (c.y < 20) { c.y = 20; c.vy = Math.abs(c.vy)*0.5; camShake = 8; }
            if (c.y > H-20) { c.y = H-20; c.vy = -Math.abs(c.vy)*0.5; camShake = 8; }
        }

        // ---- Collisions ----
        for (int i = 0; i < cars.size(); i++) {
            for (int j = i+1; j < cars.size(); j++) {
                Car a = cars.get(i), b = cars.get(j);
                if (!a.alive || !b.alive) continue;
                double dx = b.x - a.x, dy = b.y - a.y;
                double dist = Math.hypot(dx, dy);
                double minDist = 42;
                if (dist < minDist && dist > 0.01) {
                    double nx = dx/dist, ny = dy/dist;
                    double overlap = minDist - dist;
                    a.x -= nx*overlap/2; a.y -= ny*overlap/2;
                    b.x += nx*overlap/2; b.y += ny*overlap/2;
                    // impulse
                    double relVx = b.vx - a.vx, relVy = b.vy - a.vy;
                    double rel = relVx*nx + relVy*ny;
                    if (rel < 0) {
                        double imp = -rel * 0.9;
                        a.vx -= nx*imp; a.vy -= ny*imp;
                        b.vx += nx*imp; b.vy += ny*imp;
                        double dmg = Math.abs(imp) * 12;
                        a.health -= dmg; b.health -= dmg;
                        camShake = Math.min(14, camShake + dmg * 0.4);
                        spawnSparks((a.x+b.x)/2, (a.y+b.y)/2, imp);
                        if (a.health <= 0) { a.alive = false; explode(a.x, a.y, a.color); }
                        if (b.health <= 0) { b.alive = false; explode(b.x, b.y, b.color); }
                    }
                }
            }
        }

        // particles & skids
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(dt);
            if (p.life <= 0) it.remove();
        }
        Iterator<SkidMark> sit = skids.iterator();
        while (sit.hasNext()) { SkidMark s = sit.next(); s.life -= dt; if (s.life <= 0) sit.remove(); }

        camShake *= 0.85;

        // win check
        long alive = cars.stream().filter(c -> c.alive).count();
        if (alive <= 1) {
            gameOver = true;
            Car w = cars.stream().filter(c -> c.alive).findFirst().orElse(null);
            winnerText = w == null ? "Total demolition!" : (w == p1 ? "PLAYER 1 WINS!" : w == p2 ? "PLAYER 2 WINS!" : "AI WINS!");
        }
    }

    void driveAI(Car c, double dt) {
        // simple wander + seek nearest car
        c.aiTimer -= dt;
        Car target = null; double best = 1e9;
        for (Car o : cars) {
            if (o == c || !o.alive) continue;
            double d = Math.hypot(o.x-c.x, o.y-c.y);
            if (d < best) { best = d; target = o; }
        }
        if (target != null) {
            double desired = Math.atan2(target.y-c.y, target.x-c.x);
            double diff = Math.atan2(Math.sin(desired-c.angle), Math.cos(desired-c.angle));
            c.turn(Math.signum(diff) * 0.8);
            if (c.speed < 6) c.accelerate(ACCEL * 0.9);
        }
    }

    void spawnSparks(double x, double y, double power) {
        int n = (int)(6 + power * 4);
        for (int i = 0; i < n; i++) {
            double a = Math.random()*Math.PI*2;
            double sp = 60 + Math.random()*180;
            particles.add(new Particle(x, y, Math.cos(a)*sp, Math.sin(a)*sp,
                    0.4 + Math.random()*0.5, Color.ORANGE, 2 + Math.random()*2));
        }
    }

    void explode(double x, double y, Color c) {
        for (int i = 0; i < 120; i++) {
            double a = Math.random()*Math.PI*2;
            double sp = 40 + Math.random()*350;
            Color col = Math.random() < 0.5 ? Color.ORANGE : (Math.random()<0.5?Color.YELLOW:Color.DARKGRAY);
            particles.add(new Particle(x, y, Math.cos(a)*sp, Math.sin(a)*sp,
                    0.6 + Math.random()*1.2, col, 3 + Math.random()*4));
        }
        camShake = 20;
    }

    // ---------- Render ----------
    void render(GraphicsContext g) {
        // camera
        double shakeX = (Math.random()-0.5) * camShake;
        double shakeY = (Math.random()-0.5) * camShake;

        // ground gradient
        g.setFill(new LinearGradient(0,0,0,1,true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(35,38,45)), new Stop(1, Color.rgb(20,22,28))));
        g.fillRect(0, 0, W, H);

        g.save();
        g.translate(shakeX, shakeY);

        // arena border glow
        g.setStroke(Color.rgb(120,180,255,0.6));
        g.setLineWidth(4);
        g.strokeRoundRect(8, 8, W-16, H-16, 24, 24);

        // grid
        g.setStroke(Color.rgb(255,255,255,0.04));
        g.setLineWidth(1);
        for (int x = 0; x < W; x += 64) g.strokeLine(x, 0, x, H);
        for (int y = 0; y < H; y += 64) g.strokeLine(0, y, W, y);

        // skid marks
        for (SkidMark s : skids) {
            g.setStroke(Color.rgb(0,0,0, Math.min(0.5, s.life/6 * 0.5)));
            g.setLineWidth(3);
            g.strokeLine(s.x, s.y, s.x - Math.cos(s.angle)*10, s.y - Math.sin(s.angle)*10);
        }

        // cars
        for (Car c : cars) c.draw(g);

        // particles
        for (Particle p : particles) {
            double a = Math.max(0, p.life / p.maxLife);
            g.setFill(Color.color(p.c.getRed(), p.c.getGreen(), p.c.getBlue(), a));
            g.fillOval(p.x - p.size/2, p.y - p.size/2, p.size, p.size);
        }

        g.restore();

        // HUD
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        g.setFill(Color.WHITE);
        drawHealthBar(g, 20, 20, p1, "PLAYER 1 (WASD)");
        drawHealthBar(g, W-320, 20, p2, p2.isAI ? "CPU" : "PLAYER 2 (ARROWS)");

        if (gameOver) {
            g.setFill(Color.rgb(0,0,0,0.6));
            g.fillRect(0, 0, W, H);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(winnerText, W/2, H/2);
            g.setFont(Font.font("Segoe UI", 22));
            g.fillText("Press R to restart", W/2, H/2 + 60);
            g.setTextAlign(TextAlignment.LEFT);
        }
    }

    void drawHealthBar(GraphicsContext g, double x, double y, Car c, String label) {
        g.setFill(Color.rgb(0,0,0,0.5));
        g.fillRoundRect(x, y, 300, 46, 10, 10);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        g.fillText(label, x+10, y+18);
        double w = Math.max(0, c.health/100.0) * 280;
        g.setFill(c.health > 50 ? Color.LIME : c.health > 25 ? Color.ORANGE : Color.RED);
        g.fillRoundRect(x+10, y+24, w, 14, 6, 6);
    }

    public static void main(String[] args) { launch(args); }
}