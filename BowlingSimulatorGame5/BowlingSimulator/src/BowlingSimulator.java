/*
 BowlingSimulator.java
 Single-file Java Bowling Simulator with animation and simple physics.
 - Uses Swing for UI and animation (javax.swing.Timer)
 - One-file, self-contained, intended to compile with javac and run with java
 - Controls: click-drag to aim and set power, release to throw; space to throw from current power; R to reset game; P to pause; N to start a new game/frame

 Note: This is a simulation built for clarity and playability, not a physically-perfect model.
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class BowlingSimulator {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bowling Simulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            AnimationPanel panel = new AnimationPanel(1000, 800);
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.start();
        });
    }

}

// Vector2D: simple 2D vector utility
class Vector2D {
    double x, y;

    Vector2D(double x, double y) { this.x = x; this.y = y; }
    Vector2D() { this(0,0); }

    Vector2D add(Vector2D v) { return new Vector2D(x+v.x, y+v.y); }
    Vector2D sub(Vector2D v) { return new Vector2D(x-v.x, y-v.y); }
    Vector2D mul(double s) { return new Vector2D(x*s, y*s); }
    double dot(Vector2D v) { return x*v.x + y*v.y; }
    double len() { return Math.sqrt(x*x + y*y); }
    Vector2D norm() { double l = len(); return l==0?new Vector2D(0,0):new Vector2D(x/l, y/l); }
    void set(Vector2D v) { this.x = v.x; this.y = v.y; }
}

// Ball: represents the bowling ball
class Ball {
    Vector2D pos;
    Vector2D vel;
    double radius;
    double mass;
    double spin; // angular spin (for small curve)
    boolean moving;
    Color color;

    Ball(double x, double y, double r) {
        pos = new Vector2D(x,y);
        vel = new Vector2D(0,0);
        radius = r;
        mass = 6.0; // arbitrary
        spin = 0;
        moving = false;
        color = new Color(30, 60, 140);
    }

    void update(double dt, double friction) {
        if(!moving) return;
        // simple friction
        vel.x *= Math.max(0, 1 - friction*dt);
        vel.y *= Math.max(0, 1 - friction*dt);
        // spin causes slight sideways acceleration (curve)
        Vector2D lateral = new Vector2D(-vel.y, vel.x).norm();
        vel.x += lateral.x * spin * dt * 0.05;
        vel.y += lateral.y * spin * dt * 0.05;

        pos.x += vel.x*dt;
        pos.y += vel.y*dt;

        if(vel.len() < 1e-2) {
            moving = false;
            vel.x = vel.y = 0;
            spin = 0;
        }
    }

    void applyImpulse(Vector2D impulse) {
        vel = vel.add(impulse.mul(1.0/mass));
        moving = true;
    }

    void reset(double x, double y) {
        pos.set(new Vector2D(x,y));
        vel = new Vector2D(0,0);
        spin = 0;
        moving = false;
    }
}

// Pin: represents a bowling pin
class Pin {
    Vector2D pos;
    Vector2D vel;
    double radius;
    double mass;
    boolean knocked;
    double angle; // tilt angle
    double angularVel;

    Pin(double x, double y) {
        pos = new Vector2D(x,y);
        vel = new Vector2D(0,0);
        radius = 12;
        mass = 1.5;
        knocked = false;
        angle = 0;
        angularVel = 0;
    }

    void update(double dt) {
        if(knocked) {
            // simple tumbling
            vel.x *= 0.99;
            vel.y *= 0.99;
            pos.x += vel.x*dt;
            pos.y += vel.y*dt;
            angularVel *= 0.99;
            angle += angularVel*dt;
            // if heavily tilted and slow, mark as fallen
            if(Math.abs(angle) > Math.PI/2 && vel.len() < 5) {
                angularVel = 0;
            }
        }
    }

    void knock(Vector2D impulse, double angImpulse) {
        knocked = true;
        vel = vel.add(impulse.mul(1.0/mass));
        angularVel += angImpulse;
    }

    void reset(double x, double y) {
        pos.set(new Vector2D(x,y));
        vel = new Vector2D(0,0);
        knocked = false;
        angle = 0;
        angularVel = 0;
    }
}

// Lane: handles lane dimensions and drawing utilities
class Lane {
    int x, y, width, height;
    int gutters; // gutter width
    double friction; // friction coefficient

    Lane(int x, int y, int w, int h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
        gutters = 60;
        friction = 0.4; // arbitrary
    }

    Rectangle getPlayableArea() {
        return new Rectangle(x+gutters, y, width-2*gutters, height);
    }

}

// GameState: encapsulates frames, scoring and game rules
class GameState {
    int currentFrame; // 1-10
    int rollInFrame; // 1 or 2 (or 3 in 10th)
    int[][] rolls; // [frame][rollIndex]
    int[] scoreByFrame; // cumulative score by frame
    int pinsRemainingThisFrame;
    boolean ballThrown;
    int totalPinsKnockedThisRoll;
    boolean gameOver;

    GameState() {
        rolls = new int[11][3]; // 1-based frames, up to 3 rolls in 10th
        scoreByFrame = new int[11];
        reset();
    }

    void reset() {
        currentFrame = 1;
        rollInFrame = 1;
        for(int i=0;i<11;i++) Arrays.fill(rolls[i], -1);
        Arrays.fill(scoreByFrame, 0);
        pinsRemainingThisFrame = 10;
        ballThrown = false;
        totalPinsKnockedThisRoll = 0;
        gameOver = false;
    }

    void registerRoll(int pins) {
        if(gameOver) return;
        rolls[currentFrame][rollInFrame-1] = pins;
        computeScores();
        // manage frame/roll progression
        if(currentFrame < 10) {
            if(rollInFrame == 1) {
                if(pins == 10) {
                    // strike
                    currentFrame++;
                    rollInFrame = 1;
                } else {
                    pinsRemainingThisFrame = 10 - pins;
                    rollInFrame = 2;
                }
            } else {
                // second roll
                currentFrame++;
                rollInFrame = 1;
                pinsRemainingThisFrame = 10;
            }
        } else {
            // 10th frame rules
            if(rollInFrame == 1) {
                if(pins == 10) {
                    rollInFrame = 2; // allow second roll
                    pinsRemainingThisFrame = 10;
                } else {
                    pinsRemainingThisFrame = 10 - pins;
                    rollInFrame = 2;
                }
            } else if(rollInFrame == 2) {
                int first = rolls[10][0];
                if(first == 10 || first + pins == 10) {
                    // allow third roll
                    rollInFrame = 3;
                    pinsRemainingThisFrame = (pins==10?10:(10-pins));
                } else {
                    // end of game
                    gameOver = true;
                }
            } else {
                // after third roll
                gameOver = true;
            }
        }
    }

    void computeScores() {
        Arrays.fill(scoreByFrame, 0);
        int running = 0;
        for(int f=1; f<=10; f++) {
            Integer r1 = rolls[f][0] >=0 ? rolls[f][0] : null;
            Integer r2 = rolls[f][1] >=0 ? rolls[f][1] : null;
            Integer r3 = rolls[f][2] >=0 ? rolls[f][2] : null;

            if(f < 10) {
                if(r1 != null && r1 == 10) {
                    // strike: add next two rolls
                    Integer n1 = nextRollValue(f,1);
                    Integer n2 = nextRollValue(f,2);
                    if(n1 != null && n2 != null) {
                        running += 10 + n1 + n2;
                        scoreByFrame[f] = running;
                    }
                } else if(r1 != null && r2 != null && r1 + r2 == 10) {
                    // spare: add next roll
                    Integer n1 = nextRollValue(f,1);
                    if(n1 != null) {
                        running += 10 + n1;
                        scoreByFrame[f] = running;
                    }
                } else if(r1 != null && r2 != null) {
                    running += r1 + r2;
                    scoreByFrame[f] = running;
                }
            } else {
                // 10th frame
                if(r1 != null) running += r1;
                if(r2 != null) running += r2;
                if(r3 != null) running += r3;
                scoreByFrame[f] = running;
            }
        }
    }

    private Integer nextRollValue(int frame, int offset) {
        // returns the roll value offset rolls ahead (offset 1 or 2)
        List<Integer> all = new ArrayList<>();
        for(int f=frame+1; f<=10; f++) {
            for(int r=0;r<3;r++) {
                if(rolls[f][r] >= 0) all.add(rolls[f][r]);
            }
        }
        // include current frame's later rolls if needed
        for(int r=frame==0?0:1; r<3; r++) {
            if(rolls[frame][r] >= 0) all.add(rolls[frame][r]);
        }
        if(all.size() >= offset) return all.get(offset-1);
        return null;
    }

    int getTotalScore() {
        return scoreByFrame[10];
    }
}

// AnimationPanel: main game UI and animation loop
class AnimationPanel extends JPanel implements MouseListener, MouseMotionListener, KeyListener, ActionListener {

    int panelWidth, panelHeight;
    Lane lane;
    Ball ball;
    List<Pin> pins;
    GameState gameState;
    javax.swing.Timer timer;
    double lastTime;

    // aiming
    boolean aiming;
    Point aimStart;
    Point aimEnd;
    double power; // 0..1
    double maxImpulse = 2200; // tune

    // UI images
    BufferedImage offscreen;

    // state flags
    boolean paused = false;
    boolean debug = false;

    // sound stub (no sound included) - placeholder for future

    AnimationPanel(int w, int h) {
        this.panelWidth = w;
        this.panelHeight = h;
        setPreferredSize(new Dimension(w,h));

        lane = new Lane(40, 40, 520, 720); // lane left/top and size

        // ball starting location at bottom center of playable area
        Rectangle play = lane.getPlayableArea();
        double bx = play.x + play.width/2.0;
        double by = play.y + play.height - 60;
        ball = new Ball(bx, by, 16);

        pins = new ArrayList<>();
        setupPins();

        gameState = new GameState();

        addMouseListener(this);
        addMouseMotionListener(this);
        setFocusable(true);
        addKeyListener(this);

        timer = new javax.swing.Timer(16, this); // ~60 FPS
        offscreen = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    }

    void setupPins() {
        pins.clear();
        Rectangle p = lane.getPlayableArea();
        // standard triangular arrangement: 1 in front, then 2, then 3, then 4
        double startX = p.x + p.width/2.0;
        double startY = p.y + 80;
        double spacing = 28; // space between pins
        // row 1
        pins.add(new Pin(startX, startY));
        // row 2
        pins.add(new Pin(startX - spacing/2.0, startY + spacing));
        pins.add(new Pin(startX + spacing/2.0, startY + spacing));
        // row 3
        pins.add(new Pin(startX - spacing, startY + spacing*2));
        pins.add(new Pin(startX, startY + spacing*2));
        pins.add(new Pin(startX + spacing, startY + spacing*2));
        // row 4
        pins.add(new Pin(startX - 1.5*spacing, startY + spacing*3));
        pins.add(new Pin(startX - 0.5*spacing, startY + spacing*3));
        pins.add(new Pin(startX + 0.5*spacing, startY + spacing*3));
        pins.add(new Pin(startX + 1.5*spacing, startY + spacing*3));
    }

    void start() {
        lastTime = System.nanoTime()/1e9;
        timer.start();
    }

    void restartBallAndPinsForNextRoll() {
        Rectangle play = lane.getPlayableArea();
        double bx = play.x + play.width/2.0;
        double by = play.y + play.height - 60;
        ball.reset(bx, by);
        // only reset pins if frame started fresh
        // for simplicity, in this simulation we reset pins after each frame or if strike
    }

    void resetAll() {
        gameState.reset();
        setupPins();
        restartBallAndPinsForNextRoll();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(paused) return;
        double now = System.nanoTime()/1e9;
        double dt = Math.min(0.033, now - lastTime);
        lastTime = now;
        step(dt);
        repaint();
    }

    void step(double dt) {
        // update ball
        ball.update(dt, lane.friction*0.6);

        // update pins
        for(Pin p : pins) p.update(dt);

        // handle collisions
        handleCollisions(dt);

        // if the ball stopped moving and it was thrown, we need to count pins and advance the game state
        if(!ball.moving && gameState.ballThrown) {
            int knocked = countKnockedPins();
            gameState.registerRoll(knocked);
            gameState.ballThrown = false;
            // prepare next state: if we completed frame, reset pins; otherwise remove knocked pins
            boolean shouldResetPins = shouldResetPinsForNewFrame();
            if(shouldResetPins) {
                setupPins();
            } else {
                // remove knocked pins from play
                // for simplicity, we won't remove visually but will mark them fallen
            }
            // restart ball
            restartBallAndPinsForNextRoll();
        }
    }

    boolean shouldResetPinsForNewFrame() {
        // simple logic: if strike or frame finished, reset for next frame
        // If strike (10 pins in first roll) -> reset pins
        // if second roll then reset pins
        int f = gameState.currentFrame;
        int r = gameState.rollInFrame;
        if(f < 10) {
            // if previous roll caused a strike, then next frame
            // else if we were on second roll -> next frame
            // Implementation: if rolls[f][0]==10, reset. If rollInFrame==1 means new frame - so reset too.
            if(gameState.rolls[f][0] == 10) return true;
            if(gameState.rollInFrame == 1) return true;
            return false;
        } else {
            // 10th frame simpler: if frame advanced or game over, reset
            return gameState.rollInFrame == 1 || gameState.gameOver;
        }
    }

    int countKnockedPins() {
        int count = 0;
        for(Pin p : pins) {
            if(p.knocked || Math.abs(p.angle) > Math.PI/6 || p.pos.y > lane.y + lane.height - 10) count++;
        }
        return count;
    }

    void handleCollisions(double dt) {
        // Ball vs pins
        for(Pin p : pins) {
            if(p.knocked) continue; // already knocked
            Vector2D diff = new Vector2D(p.pos.x - ball.pos.x, p.pos.y - ball.pos.y);
            double dist = diff.len();
            double minD = p.radius + ball.radius;
            if(dist < minD && dist > 1e-6) {
                // collision occurred
                Vector2D normal = diff.mul(1.0/dist);
                double relVel = (ball.vel.x - p.vel.x)*normal.x + (ball.vel.y - p.vel.y)*normal.y;
                if(relVel > 0) relVel = relVel; // moving towards each other

                // impulse scalar
                double e = 0.4; // restitution
                double j = -(1+e)*relVel / (1.0/ball.mass + 1.0/p.mass);
                Vector2D impulse = normal.mul(j);
                ball.vel = ball.vel.add(impulse.mul(1.0/ball.mass));
                p.knock(impulse, j*0.02);
                // small random angular spin addition
                ball.spin += (Math.random()-0.5)*0.5;
            }
        }

        // pin-pin collisions
        for(int i=0;i<pins.size();i++) {
            for(int j=i+1;j<pins.size();j++) {
                Pin a = pins.get(i);
                Pin b = pins.get(j);
                if(!a.knocked && !b.knocked) continue; // pins collide only after knocked (they tumble into each other)
                Vector2D diff = new Vector2D(b.pos.x - a.pos.x, b.pos.y - a.pos.y);
                double dist = diff.len();
                double minD = a.radius + b.radius;
                if(dist < minD && dist > 1e-6) {
                    Vector2D n = diff.mul(1.0/dist);
                    double rel = (a.vel.x - b.vel.x)*n.x + (a.vel.y - b.vel.y)*n.y;
                    if(rel > 0) {
                        double juk;
                        juk = -(1+0.2)*rel / (1.0/a.mass + 1.0/b.mass);
                        Vector2D imp = n.mul(j);
                        a.vel = a.vel.add(imp.mul(1.0/a.mass));
                        b.vel = b.vel.add(imp.mul(-1.0/b.mass));
                    }
                    // separate overlapping
                    double overlap = minD - dist;
                    a.pos.x -= n.x*overlap*0.5;
                    a.pos.y -= n.y*overlap*0.5;
                    b.pos.x += n.x*overlap*0.5;
                    b.pos.y += n.y*overlap*0.5;
                }
            }
        }

        // ball vs gutters / walls
        Rectangle play = lane.getPlayableArea();
        if(ball.pos.x - ball.radius < play.x) {
            ball.pos.x = play.x + ball.radius;
            ball.vel.x *= -0.4;
        }
        if(ball.pos.x + ball.radius > play.x + play.width) {
            ball.pos.x = play.x + play.width - ball.radius;
            ball.vel.x *= -0.4;
        }
        if(ball.pos.y - ball.radius < play.y) {
            ball.pos.y = play.y + ball.radius;
            ball.vel.y *= -0.4;
        }
        if(ball.pos.y + ball.radius > play.y + play.height) {
            ball.pos.y = play.y + play.height - ball.radius;
            ball.vel.y *= -0.4;
        }

    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        Graphics2D gOff = offscreen.createGraphics();
        // clear
        gOff.setColor(new Color(18,18,18));
        gOff.fillRect(0,0,panelWidth,panelHeight);

        drawLane(gOff);
        drawPins(gOff);
        drawBall(gOff);
        drawHUD(gOff);
        drawAim(gOff);

        g2.drawImage(offscreen, 0, 0, null);
        gOff.dispose();
    }

    void drawLane(Graphics2D g) {
        // background
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(40,20,0));
        g.fillRect(lane.x, lane.y, lane.width, lane.height);

        // playable wood
        Rectangle p = lane.getPlayableArea();
        g.setColor(new Color(200,170,120));
        g.fillRect(p.x, p.y, p.width, p.height);

        // highlight center line
        int cx = p.x + p.width/2;
        g.setColor(new Color(255,255,255,60));
        for(int i=0;i<p.height;i+=30) g.fillRect(cx-1, p.y+i, 2, 20);

        // gutters
        g.setColor(new Color(60,60,60));
        g.fillRect(lane.x, lane.y, lane.gutters, lane.height);
        g.fillRect(lane.x+lane.width-lane.gutters, lane.y, lane.gutters, lane.height);

        // foul line
        g.setColor(Color.WHITE);
        int fy = p.y + p.height - 140;
        g.fillRect(p.x, fy, p.width, 4);

        // arrows
        for(int i=0;i<7;i++) {
            int ax = p.x + p.width/2 - 60 + i*20;
            int ay = p.y + p.height - 200;
            Polygon arrow = new Polygon();
            arrow.addPoint(ax, ay);
            arrow.addPoint(ax+8, ay+16);
            arrow.addPoint(ax-8, ay+16);
            g.fill(arrow);
        }
    }

    void drawPins(Graphics2D g) {
        for(int i=0;i<pins.size();i++) {
            Pin p = pins.get(i);
            AffineTransform old = g.getTransform();
            g.translate(p.pos.x, p.pos.y);
            g.rotate(p.angle);
            if(p.knocked || Math.abs(p.angle) > 0.2) {
                g.setColor(new Color(220,220,220,200));
            } else {
                g.setColor(Color.WHITE);
            }
            // draw pin body
            g.fillOval((int)-p.radius, (int)-p.radius, (int)(p.radius*2), (int)(p.radius*2));
            // cap
            g.setColor(Color.RED);
            g.fillRect((int)(-p.radius*0.4), (int)(-p.radius*0.8), (int)(p.radius*0.8), (int)(p.radius*0.4));
            g.setTransform(old);
        }
    }

    void drawBall(Graphics2D g) {
        g.setColor(ball.color);
        g.fillOval((int)(ball.pos.x - ball.radius), (int)(ball.pos.y - ball.radius), (int)(ball.radius*2), (int)(ball.radius*2));
        // shine
        g.setColor(new Color(255,255,255,100));
        g.fillOval((int)(ball.pos.x - ball.radius*0.4), (int)(ball.pos.y - ball.radius*0.8), (int)(ball.radius*0.6), (int)(ball.radius*0.6));
    }

    void drawHUD(Graphics2D g) {
        // score box
        int sx = lane.x + lane.width + 20;
        int sy = lane.y;
        g.setColor(new Color(0,0,0,160));
        g.fillRoundRect(sx, sy, 420, 380, 12, 12);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("Bowling Simulator", sx+16, sy+28);

        // frame scores
        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        for(int f=1; f<=10; f++) {
            int fx = sx + 10 + (f-1)%5 * 80;
            int fy = sy + 60 + ((f-1)/5)*90;
            g.setColor(new Color(30,30,30,220));
            g.fillRect(fx, fy, 72, 72);
            g.setColor(Color.WHITE);
            g.drawRect(fx, fy, 72, 72);
            g.drawString("F"+f, fx+6, fy+18);
            // draw rolls inside
            String r1 = gameState.rolls[f][0] >=0 ? String.valueOf(gameState.rolls[f][0]) : "-";
            String r2 = gameState.rolls[f][1] >=0 ? String.valueOf(gameState.rolls[f][1]) : "-";
            String r3 = gameState.rolls[f][2] >=0 ? String.valueOf(gameState.rolls[f][2]) : "-";
            g.drawString(r1, fx+6, fy+36);
            g.drawString(r2, fx+28, fy+36);
            g.drawString(r3, fx+50, fy+36);
            g.drawString(String.valueOf(gameState.scoreByFrame[f]), fx+6, fy+60);
        }

        // total
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString("Total: " + gameState.getTotalScore(), sx+20, sy+340);

        // controls help
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Controls:", sx+20, sy+380 - 8);
        g.drawString("Click-drag to aim; release to throw. Space to throw at current power.", sx+20, sy+400 - 8);
        g.drawString("P = Pause, R = Reset, N = New Game, D = Toggle Debug", sx+20, sy+420 - 8);

        if(paused) {
            g.setFont(new Font("SansSerif", Font.BOLD, 36));
            g.setColor(new Color(255,255,255,180));
            g.drawString("PAUSED", panelWidth/2 - 70, panelHeight/2);
        }

        if(gameState.gameOver) {
            g.setFont(new Font("SansSerif", Font.BOLD, 36));
            g.setColor(new Color(255,255,255,220));
            g.drawString("GAME OVER", panelWidth/2 - 110, panelHeight/2 - 20);
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.drawString("Final Score: " + gameState.getTotalScore(), panelWidth/2 - 110, panelHeight/2 + 14);
        }

        if(debug) {
            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g.setColor(Color.GREEN);
            g.drawString(String.format("Ball pos: %.2f, %.2f", ball.pos.x, ball.pos.y), sx+220, sy+28);
            g.drawString(String.format("Ball vel: %.2f, %.2f", ball.vel.x, ball.vel.y), sx+220, sy+44);
            g.drawString("Frame: " + gameState.currentFrame + " Roll: " + gameState.rollInFrame, sx+220, sy+60);
        }
    }

    void drawAim(Graphics2D g) {
        if(aiming && aimStart != null && aimEnd != null) {
            g.setColor(new Color(255,255,255,150));
            g.setStroke(new BasicStroke(2));
            g.drawLine(aimStart.x, aimStart.y, aimEnd.x, aimEnd.y);
            // power bar
            int sx = lane.x + lane.width + 20;
            int sy = lane.y + 420;
            g.setColor(new Color(0,0,0,160));
            g.fillRect(sx, sy, 300, 22);
            g.setColor(new Color(200,80,80));
            g.fillRect(sx+2, sy+2, (int)(296*power), 18);
            g.setColor(Color.WHITE);
            g.drawRect(sx, sy, 300, 22);
            g.drawString(String.format("Power: %.0f%%", power*100), sx+310, sy+16);
        }
    }

    // Mouse events for aiming
    @Override
    public void mousePressed(MouseEvent e) {
        if(gameState.gameOver) return;
        Rectangle play = lane.getPlayableArea();
        if(ball.moving) return;
        if(new Rectangle((int)(ball.pos.x-ball.radius),(int)(ball.pos.y-ball.radius),(int)(ball.radius*2),(int)(ball.radius*2)).contains(e.getPoint())) {
            aiming = true;
            aimStart = e.getPoint();
            aimEnd = e.getPoint();
            power = 0.3;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(!aiming) return;
        aiming = false;
        aimEnd = e.getPoint();
        // compute impulse
        Vector2D dir = new Vector2D(aimStart.x - aimEnd.x, aimStart.y - aimEnd.y);
        double dist = dir.len();
        if(dist < 5) return; // too little
        dir = dir.norm();
        double pwr = Math.min(1.0, Math.max(0.01, dist/200.0));
        Vector2D impulse = dir.mul(maxImpulse * pwr);
        ball.applyImpulse(impulse);
        // spin from horizontal offset
        double side = (aimStart.x - aimEnd.x) / 200.0;
        ball.spin = side * 2.0;
        gameState.ballThrown = true;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if(!aiming) return;
        aimEnd = e.getPoint();
        double dist = new Vector2D(aimStart.x - aimEnd.x, aimStart.y - aimEnd.y).len();
        power = Math.min(1.0, dist/200.0);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // allow click to set power quick-throw if not moving
        if(ball.moving || gameState.gameOver) return;
        // quick tap to throw straight with small power
    }

    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}

    // Key events for controls
    @Override
    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if(c == 'p' || c == 'P') {
            paused = !paused;
        } else if(c == 'r' || c=='R') {
            resetAll();
        } else if(c == 'n' || c=='N') {
            resetAll();
        } else if(c == 'd' || c=='D') {
            debug = !debug;
        } else if(c == ' ') {
            // space: quick throw using current power and straight direction
            if(!ball.moving && !gameState.gameOver) {
                Rectangle play = lane.getPlayableArea();
                Vector2D dir = new Vector2D(0, -1);
                Vector2D impulse = dir.mul(maxImpulse * power);
                ball.applyImpulse(impulse);
                ball.spin = 0;
                gameState.ballThrown = true;
            }
        }
    }

    @Override public void keyPressed(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

}
