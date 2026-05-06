import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VirtualGolfGame extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VirtualGolfGame game = new VirtualGolfGame();
            game.setVisible(true);
        });
    }

    public VirtualGolfGame() {
        setTitle("⛳ Virtual Golf Challenge — Seaside Championship");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
    }
}

// ─────────────────────────────────────────────
//  GAME PANEL
// ─────────────────────────────────────────────
class GamePanel extends JPanel implements ActionListener, KeyListener {

    static final int W = 900, H = 520;

    // Game state
    enum Screen { START, PLAYING, HOLE_COMPLETE, GAME_OVER }
    Screen screen = Screen.START;
    int gameMode = 0; // 1 = vs AI, 2 = 2 players
    int currentPlayer = 1, hole = 1, totalHoles = 9;
    int[] scores = {0, 0};
    int[] strokes = {0, 0};
    int par = 3;

    // Ball
    double ballX, ballY, ballVX, ballVY;
    boolean ballInFlight = false;

    // Hole
    double holeX, holeY;
    double windSpeed = 0;

    // Aiming
    double aimAngle = -45;
    double power = 50;
    boolean charging = false;
    int chargeDir = 1;

    // Scenery
    List<double[]> stars = new ArrayList<>();
    List<double[]> clouds = new ArrayList<>();
    List<double[]> birds = new ArrayList<>();
    List<double[]> waves = new ArrayList<>();
    List<double[]> palmTrees = new ArrayList<>();
    List<Obstacle> obstacles = new ArrayList<>();

    // Particles
    List<Particle> particles = new ArrayList<>();

    // Animation
    int frame = 0;
    double sunAngle = 0;
    Timer timer;
    Random rng = new Random();

    // Message overlay
    String message = "";
    int messageTimer = 0;

    // Input
    boolean keyLeft, keyRight, keySpace;
    double mouseX, mouseY;

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX(); mouseY = e.getY();
                if (screen == Screen.PLAYING && !ballInFlight) {
                    aimAngle = Math.toDegrees(Math.atan2(mouseY - ballY, mouseX - ballX));
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (screen == Screen.START) return;
                if (screen == Screen.PLAYING && !ballInFlight && !isAITurn()) {
                    aimAngle = Math.toDegrees(Math.atan2(mouseY - ballY, mouseX - ballX));
                    charging = true;
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (charging) { charging = false; shoot(); }
            }
        });
        initScenery();
        timer = new Timer(16, this); // ~60fps
        timer.start();
    }

    boolean isAITurn() { return gameMode == 1 && currentPlayer == 2; }

    // ── INIT ──────────────────────────────────
    void initScenery() {
        stars.clear(); clouds.clear(); birds.clear(); waves.clear(); palmTrees.clear();
        for (int i = 0; i < 70; i++)
            stars.add(new double[]{rng.nextDouble()*W, rng.nextDouble()*(H*0.38), rng.nextDouble()*1.5+0.3, rng.nextDouble()*Math.PI*2});
        for (int i = 0; i < 5; i++)
            clouds.add(new double[]{rng.nextDouble()*W, 35+rng.nextDouble()*55, 70+rng.nextDouble()*110, 0.15+rng.nextDouble()*0.2});
        for (int i = 0; i < 4; i++)
            birds.add(new double[]{rng.nextDouble()*W, 30+rng.nextDouble()*70, 0.4+rng.nextDouble()*0.3, rng.nextDouble()*Math.PI*2});
        for (int i = 0; i < 6; i++)
            waves.add(new double[]{rng.nextDouble()*W, 0.5+rng.nextDouble()*0.5, 6+rng.nextDouble()*8, rng.nextDouble()*Math.PI*2});
        for (int i = 0; i < 5; i++)
            palmTrees.add(new double[]{50+rng.nextDouble()*(W-100), H*0.62+rng.nextDouble()*20});
    }

    void generateHole() {
        ballInFlight = false; particles.clear();
        int margin = 80;
        ballX = margin + rng.nextInt(60);
        ballY = H*0.67 + rng.nextInt(20);
        holeX = W - margin - rng.nextInt(80);
        holeY = H*0.62 + rng.nextInt(25);
        obstacles.clear();
        int numObs = 1 + hole/3;
        for (int i = 0; i < numObs; i++) {
            double ox = ballX + 100 + rng.nextDouble()*(holeX - ballX - 150);
            int type = rng.nextInt(3);
            obstacles.add(new Obstacle(ox, H*0.62+rng.nextInt(25), type, 28+rng.nextInt(22), 16+rng.nextInt(14)));
        }
        windSpeed = (rng.nextDouble() - 0.5) * 2.5;
        par = 2 + rng.nextInt(2);
        aimAngle = idealAngle() - 15 - rng.nextInt(15);
    }

    double idealAngle() {
        return Math.toDegrees(Math.atan2(holeY - ballY, holeX - ballX));
    }

    // ── GAME LOOP ─────────────────────────────
    public void actionPerformed(ActionEvent e) {
        frame++;
        sunAngle += 0.001;

        // Move clouds
        for (double[] c : clouds) c[0] = (c[0] + c[3]) % (W + 120) - 60;
        // Move birds
        for (double[] b : birds) { b[0] = (b[0] + b[2]) % (W + 60) - 30; b[3] += 0.07; }

        if (screen == Screen.PLAYING) {
            // Key aim
            if (keyLeft && !ballInFlight) aimAngle -= 2;
            if (keyRight && !ballInFlight) aimAngle += 2;
            // Charge
            if (charging && !ballInFlight) {
                power += chargeDir * 1.5;
                if (power >= 100) { power = 100; chargeDir = -1; }
                if (power <= 5) { power = 5; chargeDir = 1; }
            }
            updateBall();
            updateParticles();
            if (messageTimer > 0) messageTimer--;
            // AI turn
            if (isAITurn() && !ballInFlight) scheduleAI();
        }
        repaint();
    }

    int aiDelay = 0;
    boolean aiScheduled = false;
    void scheduleAI() {
        if (aiScheduled) return;
        aiScheduled = true;
        Timer aiTimer = new Timer(900, ev -> {
            if (screen == Screen.PLAYING && isAITurn() && !ballInFlight) {
                aimAngle = idealAngle() + (rng.nextDouble()-0.5)*22;
                power = 55 + rng.nextDouble()*25;
                shoot();
            }
            aiScheduled = false;
            ((Timer)ev.getSource()).stop();
        });
        aiTimer.setRepeats(false);
        aiTimer.start();
    }

    void updateBall() {
        if (!ballInFlight) return;
        ballVY += 0.18;
        ballVX += windSpeed * 0.008;
        ballX += ballVX; ballY += ballVY;

        // Hole check
        double dx = ballX - holeX, dy = ballY - holeY;
        if (Math.sqrt(dx*dx+dy*dy) < 18) {
            ballX = holeX; ballY = holeY; ballInFlight = false;
            emitParticles(holeX, holeY, "goal");
            onHoleIn(); return;
        }

        // Obstacle collision
        for (Obstacle o : obstacles) {
            if (Math.abs(ballX-o.x) < o.w/2+9 && Math.abs(ballY-o.y) < o.h/2+9) {
                if (o.type == 2) emitParticles(ballX, ballY, "splash");
                ballVX *= -0.5; ballVY *= -0.55;
            }
        }

        // Ground
        double groundY = H*0.62 + Math.sin(ballX*0.05)*4;
        if (ballY > groundY) {
            ballY = groundY;
            ballVY *= -0.45; ballVX *= 0.85;
            emitParticles(ballX, ballY, "bounce");
            if (Math.abs(ballVY) < 0.4 && Math.abs(ballVX) < 0.3) {
                ballVX = 0; ballVY = 0; ballInFlight = false;
                onBallStopped();
            }
        }
        // OOB
        if (ballX < 0 || ballX > W || ballY > H*0.84) {
            ballInFlight = false;
            ballX = Math.max(50, Math.min(W-50, ballX));
            ballY = groundY;
            onBallStopped();
        }
    }

    void shoot() {
        if (ballInFlight) return;
        double rad = Math.toRadians(aimAngle);
        double spd = (power/100.0) * (8 + hole*0.25);
        ballVX = Math.cos(rad)*spd; ballVY = Math.sin(rad)*spd;
        ballInFlight = true;
        power = 50; chargeDir = 1;
    }

    void onHoleIn() {
        strokes[currentPlayer-1]++;
        scores[currentPlayer-1]++;
        message = strokes[currentPlayer-1] <= par ? "🎉 BIRDIE!" : "⛳ IN THE HOLE!";
        messageTimer = 130;
        Timer t = new Timer(2200, e -> { advanceTurn(); ((Timer)e.getSource()).stop(); });
        t.setRepeats(false); t.start();
    }

    void onBallStopped() {
        strokes[currentPlayer-1]++;
        if (strokes[currentPlayer-1] > par+3) {
            message = "Out of shots! Penalty...";
            messageTimer = 90;
            Timer t = new Timer(1500, e -> { penaltyMove(); ((Timer)e.getSource()).stop(); });
            t.setRepeats(false); t.start();
        }
    }

    void penaltyMove() {
        ballX = ballX < holeX ? ballX+60 : ballX-60;
        ballY = holeY+20; message = ""; messageTimer = 0;
    }

    void advanceTurn() {
        if (hole >= totalHoles && (gameMode==1 || currentPlayer==2)) { endGame(); return; }
        if (gameMode == 2 && currentPlayer == 1) {
            currentPlayer = 2; strokes[1] = 0; generateHole(); message = "Player 2's Turn"; messageTimer = 80;
        } else {
            hole++; currentPlayer = 1; strokes[0] = 0; strokes[1] = 0; generateHole();
            message = "Hole " + hole; messageTimer = 60;
        }
    }

    void endGame() {
        screen = Screen.GAME_OVER;
        String winner = scores[0] > scores[1] ? "Player 1 Wins! 🏆"
                : scores[1] > scores[0] ? (gameMode==1?"Computer Wins! 🤖":"Player 2 Wins! 🏆")
                : "It's a Tie! 🤝";
        message = winner + "\n" + scores[0] + " – " + scores[1];
        messageTimer = 9999;
    }

    void startGame(int mode) {
        gameMode = mode; hole = 1; scores[0]=0; scores[1]=0;
        strokes[0]=0; strokes[1]=0; currentPlayer=1;
        screen = Screen.PLAYING;
        initScenery(); generateHole();
        power = 50; message = "Hole 1 — GO!"; messageTimer = 70;
    }

    // ── PARTICLES ─────────────────────────────
    void emitParticles(double x, double y, String type) {
        int n = type.equals("goal") ? 30 : 10;
        for (int i = 0; i < n; i++) {
            double a = rng.nextDouble()*Math.PI*2;
            double sp = type.equals("goal") ? 3+rng.nextDouble()*5 : 1+rng.nextDouble()*3;
            particles.add(new Particle(x,y, Math.cos(a)*sp, Math.sin(a)*sp-2, type));
        }
    }

    void updateParticles() {
        particles.removeIf(p -> { p.update(); return p.life<=0; });
    }

    // ── RENDERING ─────────────────────────────
    @Override
    protected void paintComponent(Graphics g2d) {
        super.paintComponent(g2d);
        Graphics2D g = (Graphics2D) g2d;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawSky(g);
        drawOcean(g);
        drawGround(g);
        drawPalmTrees(g);
        drawObstacles(g);
        drawHoleFlag(g);
        if (screen == Screen.PLAYING || screen == Screen.GAME_OVER) {
            drawAimLine(g);
            drawParticles(g);
            drawBall(g);
        }
        drawHUD(g);
        if (!message.isEmpty() && messageTimer > 0) drawMessage(g);
        if (screen == Screen.START) drawStartScreen(g);
        if (screen == Screen.GAME_OVER) drawGameOverScreen(g);
    }

    void drawSky(Graphics2D g) {
        GradientPaint sky = new GradientPaint(0,0, new Color(13,27,75),
                0,(int)(H*0.55), new Color(230,81,0));
        g.setPaint(sky); g.fillRect(0,0,W,(int)(H*0.55));

        // Stars
        for (double[] s : stars) {
            s[3] += 0.04;
            float alpha = (float)(0.3 + 0.4*Math.sin(s[3]));
            g.setColor(new Color(1f,1f,0.85f,alpha));
            g.fill(new Ellipse2D.Double(s[0]-s[2], s[1]-s[2], s[2]*2, s[2]*2));
        }

        // Sun
        double sx = W*0.5+Math.cos(sunAngle)*50, sy = H*0.2+Math.sin(sunAngle)*10;
        RadialGradientPaint sun = new RadialGradientPaint(
                new Point2D.Double(sx,sy), 60,
                new float[]{0f,0.35f,1f},
                new Color[]{new Color(255,248,100,240), new Color(255,150,40,120), new Color(255,80,0,0)}
        );
        g.setPaint(sun); g.fill(new Ellipse2D.Double(sx-60,sy-60,120,120));
        g.setColor(new Color(255,252,180)); g.fill(new Ellipse2D.Double(sx-14,sy-14,28,28));

        // Clouds
        for (double[] c : clouds) drawCloud(g, c[0], c[1], c[2]);

        // Birds
        for (double[] b : birds) drawBird(g, b[0], b[1], b[3]);

        // Wind indicator
        g.setColor(new Color(255,255,255,160));
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String windTxt = windSpeed > 0 ? String.format("→ Wind %.1f", windSpeed) : String.format("← Wind %.1f", Math.abs(windSpeed));
        g.drawString(windTxt, W-110, 22);
    }

    void drawCloud(Graphics2D g, double x, double y, double w) {
        g.setColor(new Color(255,255,255,35));
        int[] sizes = {14,20,24,18,12};
        double[] offsets = {0, 0.25, 0.5, 0.75, 1.0};
        for (int i=0;i<5;i++) {
            double cx = x+offsets[i]*w, cy = y+(i%2)*6;
            g.fill(new Ellipse2D.Double(cx-sizes[i],cy-sizes[i],sizes[i]*2,sizes[i]*2));
        }
    }

    void drawBird(Graphics2D g, double x, double y, double phase) {
        g.setColor(new Color(180,220,255,130));
        g.setStroke(new BasicStroke(1.5f));
        double flap = Math.sin(phase)*8;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x-10, y+flap);
        p.quadTo(x, y-flap*0.5, x+10, y+flap);
        g.draw(p);
        g.setStroke(new BasicStroke(1f));
    }

    void drawOcean(Graphics2D g) {
        GradientPaint ocean = new GradientPaint(0,(int)(H*0.55), new Color(13,71,161),
                0,(int)(H*0.78), new Color(25,118,210));
        g.setPaint(ocean);
        g.fillRect(0,(int)(H*0.55),W,(int)(H*0.25));

        // Waves
        for (int i=0;i<waves.size();i++) {
            double[] w = waves.get(i);
            Path2D.Double wave = new Path2D.Double();
            for (int x=0;x<=W;x+=4) {
                double wy = H*0.57 + i*8 + Math.sin((x/60.0)+frame*0.015*w[1]+w[3])*w[2];
                if (x==0) wave.moveTo(x,wy); else wave.lineTo(x,wy);
            }
            float alpha = 0.12f + i*0.04f;
            g.setColor(new Color(100,181,246,(int)(alpha*255)));
            g.setStroke(new BasicStroke(1.5f)); g.draw(wave);
        }
        g.setStroke(new BasicStroke(1f));
    }

    void drawGround(Graphics2D g) {
        // Sand
        GradientPaint sand = new GradientPaint(0,(int)(H*0.78), new Color(249,228,183),
                0,H, new Color(232,201,126));
        g.setPaint(sand); g.fillRect(0,(int)(H*0.78),W,(int)(H*0.22));

        // Grass fairway
        GradientPaint grass = new GradientPaint(0,(int)(H*0.6), new Color(46,125,50),
                0,(int)(H*0.8), new Color(67,160,71));
        g.setPaint(grass);
        Path2D.Double fairway = new Path2D.Double();
        fairway.moveTo(0, H*0.78);
        for (int x=0;x<=W;x+=12) {
            double gy = H*0.62 + Math.sin(x*0.05)*4;
            fairway.lineTo(x,gy);
        }
        fairway.lineTo(W,H*0.78); fairway.closePath();
        g.fill(fairway);

        // Grass lines
        g.setColor(new Color(46,125,50,60));
        g.setStroke(new BasicStroke(1f));
        for (int x=15;x<W;x+=18) {
            g.drawLine(x,(int)(H*0.63),x+4,(int)(H*0.77));
        }
        g.setStroke(new BasicStroke(1f));
    }

    void drawPalmTrees(Graphics2D g) {
        for (double[] pt : palmTrees) drawPalmTree(g, pt[0], pt[1]);
    }

    void drawPalmTree(Graphics2D g, double x, double y) {
        // Trunk
        g.setColor(new Color(121,85,72));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double trunk = new Path2D.Double();
        trunk.moveTo(x,y);
        trunk.quadTo(x+8,y-30,x+4,y-58);
        g.draw(trunk);
        // Leaves
        Color[] leafCols = {new Color(46,125,50),new Color(56,142,60),new Color(27,94,32),new Color(76,175,80)};
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i=0;i<5;i++) {
            double la = (i/5.0)*Math.PI*2 + frame*0.008;
            g.setColor(leafCols[i%4]);
            Path2D.Double leaf = new Path2D.Double();
            leaf.moveTo(x+4, y-58);
            leaf.quadTo(x+4+Math.cos(la)*22, y-58+Math.sin(la)*10,
                    x+4+Math.cos(la)*42, y-58+Math.sin(la)*18);
            g.draw(leaf);
        }
        g.setStroke(new BasicStroke(1f));
    }

    void drawObstacles(Graphics2D g) {
        for (Obstacle o : obstacles) {
            if (o.type == 0) {
                // Sand bunker
                g.setColor(new Color(255,224,130));
                g.fill(new Ellipse2D.Double(o.x-o.w/2, o.y-o.h/4, o.w, o.h/2));
                g.setColor(new Color(249,168,37)); g.setStroke(new BasicStroke(2f));
                g.draw(new Ellipse2D.Double(o.x-o.w/2, o.y-o.h/4, o.w, o.h/2));
                g.setFont(new Font("SansSerif",Font.PLAIN,9)); g.setColor(new Color(100,70,0));
                g.drawString("SAND", (int)o.x-13, (int)o.y+4);
            } else if (o.type == 1) {
                // Rock
                g.setColor(new Color(84,110,122));
                int[] rx = {(int)(o.x-o.w/2),(int)(o.x-o.w/3),(int)(o.x+o.w/3),(int)(o.x+o.w/2)};
                int[] ry = {(int)(o.y+o.h/2),(int)(o.y-o.h/2),(int)(o.y-o.h/2),(int)(o.y+o.h/2)};
                g.fillPolygon(rx,ry,4);
                g.setColor(new Color(55,71,79)); g.setStroke(new BasicStroke(1.5f));
                g.drawPolygon(rx,ry,4);
            } else {
                // Water hazard
                RadialGradientPaint wg = new RadialGradientPaint(
                        new Point2D.Double(o.x-4,o.y-3),(float)o.w,
                        new float[]{0f,1f}, new Color[]{new Color(100,181,246),new Color(21,101,192)}
                );
                g.setPaint(wg);
                g.fill(new Ellipse2D.Double(o.x-o.w/2, o.y-o.h/4, o.w, o.h/2));
                g.setColor(new Color(144,202,249)); g.setStroke(new BasicStroke(1.5f));
                g.draw(new Ellipse2D.Double(o.x-o.w/2, o.y-o.h/4, o.w, o.h/2));
                g.setFont(new Font("SansSerif",Font.PLAIN,9)); g.setColor(new Color(200,230,255));
                g.drawString("WATER", (int)o.x-14, (int)o.y+4);
            }
            g.setStroke(new BasicStroke(1f));
        }
    }

    void drawHoleFlag(Graphics2D g) {
        // Shadow
        g.setColor(new Color(0,0,0,60));
        g.fill(new Ellipse2D.Double(holeX-12,holeY-3,24,8));
        // Cup
        g.setColor(new Color(27,94,32));
        g.fill(new Ellipse2D.Double(holeX-9,holeY-3,18,7));
        g.setColor(new Color(0,0,0,90));
        g.fill(new Ellipse2D.Double(holeX-7,holeY-2,14,5));
        // Pole
        g.setColor(new Color(200,200,200));
        g.setStroke(new BasicStroke(2f)); g.drawLine((int)holeX,(int)holeY,(int)holeX,(int)(holeY-44));
        // Flag
        double fw = Math.sin(frame*0.06)*8;
        int[] fx = {(int)holeX, (int)(holeX+20+fw), (int)holeX};
        int[] fy = {(int)(holeY-44), (int)(holeY-36), (int)(holeY-29)};
        g.setColor(new Color(244,67,54)); g.fillPolygon(fx,fy,3);
        g.setStroke(new BasicStroke(1f));
        // Distance label
        double dist = Math.sqrt((holeX-ballX)*(holeX-ballX)+(holeY-ballY)*(holeY-ballY));
        g.setColor(new Color(255,255,255,160));
        g.setFont(new Font("SansSerif",Font.PLAIN,10));
        g.drawString(String.format("%.0f", dist)+"px", (int)holeX-12, (int)(holeY-50));
    }

    void drawBall(Graphics2D g) {
        // Shadow
        g.setColor(new Color(0,0,0,50));
        g.fill(new Ellipse2D.Double(ballX-8,ballY+2,16,6));
        // Ball gradient
        RadialGradientPaint bg = new RadialGradientPaint(
                new Point2D.Double(ballX-3,ballY-3), 9,
                new float[]{0f,0.6f,1f},
                new Color[]{Color.WHITE, new Color(220,220,220), new Color(160,160,160)}
        );
        g.setPaint(bg); g.fill(new Ellipse2D.Double(ballX-9,ballY-9,18,18));
        g.setColor(new Color(0,0,0,30)); g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(ballX-9,ballY-9,18,18));
        // Dimples
        g.setColor(new Color(0,0,0,40));
        int[][] dimples = {{3,0},{-3,-2},{0,3},{4,-3}};
        for (int[] d : dimples) g.fill(new Ellipse2D.Double(ballX+d[0]-2,ballY+d[1]-2,4,4));
        g.setStroke(new BasicStroke(1f));
    }

    void drawAimLine(Graphics2D g) {
        if (ballInFlight) return;
        double rad = Math.toRadians(aimAngle);
        double ex = ballX+Math.cos(rad)*55, ey = ballY+Math.sin(rad)*55;
        g.setColor(new Color(255,213,79,120));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{6f,6f}, 0f));
        g.drawLine((int)ballX,(int)ballY,(int)ex,(int)ey);
        g.setStroke(new BasicStroke(1f));
    }

    void drawParticles(Graphics2D g) {
        for (Particle p : particles) {
            float alpha = p.life/(float)p.maxLife;
            if (p.type.equals("goal"))
                g.setColor(new Color(1f,0.84f,0.31f,alpha));
            else
                g.setColor(new Color(0.59f,0.85f,1f,alpha*0.85f));
            g.fill(new Ellipse2D.Double(p.x-p.r,p.y-p.r,p.r*2,p.r*2));
        }
    }

    void drawHUD(Graphics2D g) {
        // Top bar
        g.setColor(new Color(0,0,0,100));
        g.fillRoundRect(W/2-120, 6, 240, 28, 20, 20);
        g.setColor(new Color(255,255,255,180));
        g.setFont(new Font("Serif",Font.BOLD,14));
        String holeText = screen==Screen.START ? "⛳ VIRTUAL GOLF" : "HOLE " + hole + " / " + totalHoles + "   PAR " + par;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(holeText, W/2 - fm.stringWidth(holeText)/2, 25);

        if (screen == Screen.PLAYING || screen == Screen.GAME_OVER) {
            // Player 1 card
            drawPlayerCard(g, 10, H-90, "Player 1", scores[0], strokes[0], currentPlayer==1);
            // Player 2 card
            String p2n = gameMode==1 ? "Computer" : "Player 2";
            drawPlayerCard(g, W-190, H-90, p2n, scores[1], strokes[1], currentPlayer==2);
            // Power bar
            drawPowerBar(g);
            // Aim compass
            drawCompass(g);
        }
    }

    void drawPlayerCard(Graphics2D g, int x, int y, String name, int score, int strk, boolean active) {
        Color border = active ? new Color(255,213,79,200) : new Color(255,255,255,50);
        Color bg = active ? new Color(255,213,79,30) : new Color(0,0,0,80);
        g.setColor(bg); g.fillRoundRect(x,y,178,78,14,14);
        g.setColor(border); g.setStroke(new BasicStroke(active?2f:1f));
        g.drawRoundRect(x,y,178,78,14,14);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(179,229,252,150));
        g.setFont(new Font("SansSerif",Font.PLAIN,10));
        g.drawString(name.toUpperCase(), x+12, y+18);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif",Font.BOLD,16));
        g.drawString("⛳ " + (name.contains("Computer")?"AI":name.split(" ")[1]), x+12, y+38);
        g.setColor(new Color(255,213,79));
        g.setFont(new Font("Serif",Font.BOLD,22));
        g.drawString("" + score, x+12, y+62);
        g.setColor(new Color(200,220,255,160));
        g.setFont(new Font("SansSerif",Font.PLAIN,10));
        g.drawString("SCORE   Shots: "+strk, x+38, y+62);
    }

    void drawPowerBar(Graphics2D g) {
        int bx = W/2-80, by = H-40, bw = 160, bh = 14;
        g.setColor(new Color(255,255,255,160));
        g.setFont(new Font("SansSerif",Font.PLAIN,10));
        g.drawString("POWER", bx, by-4);
        g.setColor(new Color(0,0,0,100)); g.fillRoundRect(bx,by,bw,bh,7,7);
        // Power fill gradient
        float pct = (float)(power/100.0);
        GradientPaint pg = new GradientPaint(bx,by,new Color(102,187,106),bx+bw,by,new Color(244,67,54));
        g.setPaint(pg);
        g.fillRoundRect(bx,by,(int)(bw*pct),bh,7,7);
        g.setColor(new Color(255,255,255,80)); g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(bx,by,bw,bh,7,7);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,10));
        g.drawString(Math.round(power)+"%", bx+bw+6, by+11);
        // Swing hint
        g.setColor(new Color(255,255,255,110));
        g.setFont(new Font("SansSerif",Font.PLAIN,10));
        String hint = isAITurn() ? "AI is thinking..." : "CLICK/SPACE to swing";
        g.drawString(hint, bx, by+bh+14);
    }

    void drawCompass(Graphics2D g) {
        int cx = W/2, cy = H-95, cr = 32;
        // BG
        RadialGradientPaint cbg = new RadialGradientPaint(
                new Point2D.Double(cx,cy),(float)cr,
                new float[]{0f,1f},
                new Color[]{new Color(21,101,192,100),new Color(10,42,74,180)}
        );
        g.setPaint(cbg); g.fill(new Ellipse2D.Double(cx-cr,cy-cr,cr*2,cr*2));
        g.setColor(new Color(255,255,255,40)); g.setStroke(new BasicStroke(1.5f));
        g.draw(new Ellipse2D.Double(cx-cr,cy-cr,cr*2,cr*2));
        // Rings
        g.setColor(new Color(255,255,255,20));
        for (int r : new int[]{12,22}) g.draw(new Ellipse2D.Double(cx-r,cy-r,r*2,r*2));
        // Arrow
        double rad = Math.toRadians(aimAngle);
        int ax = (int)(cx+Math.cos(rad)*25), ay = (int)(cy+Math.sin(rad)*25);
        g.setColor(new Color(255,213,79)); g.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.drawLine(cx,cy,ax,ay);
        // Arrowhead
        double ha = Math.atan2(ay-cy,ax-cx);
        int[] hx = {ax,(int)(ax-10*Math.cos(ha-0.4)),(int)(ax-10*Math.cos(ha+0.4))};
        int[] hy = {ay,(int)(ay-10*Math.sin(ha-0.4)),(int)(ay-10*Math.sin(ha+0.4))};
        g.fillPolygon(hx,hy,3);
        // Center dot
        g.setColor(new Color(255,255,255,220));
        g.fill(new Ellipse2D.Double(cx-4,cy-4,8,8));
        // Angle label
        g.setColor(new Color(255,255,255,160));
        g.setFont(new Font("SansSerif",Font.PLAIN,9));
        g.drawString((int)aimAngle+"°", cx-12, cy+cr+12);
        g.setStroke(new BasicStroke(1f));
    }

    void drawMessage(Graphics2D g) {
        String[] lines = message.split("\n");
        g.setFont(new Font("Serif",Font.BOLD,36));
        int lineH = 44;
        int totalH = lines.length * lineH + 20;
        int maxW = 0;
        FontMetrics fm = g.getFontMetrics();
        for (String l : lines) maxW = Math.max(maxW, fm.stringWidth(l));
        int bx = W/2-maxW/2-20, by = H/2-totalH/2-10;
        g.setColor(new Color(0,0,0,160));
        g.fillRoundRect(bx,by,maxW+40,totalH,20,20);
        for (int i=0;i<lines.length;i++) {
            String l = lines[i];
            int tx = W/2-fm.stringWidth(l)/2;
            int ty = H/2-totalH/2+lineH*(i+1)-6;
            // Shadow
            g.setColor(new Color(0,0,0,120)); g.drawString(l,tx+2,ty+2);
            // Gold text
            g.setColor(new Color(255,213,79)); g.drawString(l,tx,ty);
        }
    }

    void drawStartScreen(Graphics2D g) {
        g.setColor(new Color(10,22,40,220));
        g.fillRoundRect(W/2-230, H/2-180, 460, 340, 24, 24);
        g.setColor(new Color(255,213,79,80)); g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(W/2-230, H/2-180, 460, 340, 24, 24);
        g.setStroke(new BasicStroke(1f));

        g.setFont(new Font("Serif",Font.BOLD,32));
        String t1 = "⛳ Virtual Golf Challenge";
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(255,213,79)); g.drawString(t1, W/2-fm.stringWidth(t1)/2, H/2-110);

        g.setFont(new Font("SansSerif",Font.PLAIN,13));
        g.setColor(new Color(179,229,252));
        String[] info = {
                "Play 9 holes on a beautiful coastal course.",
                "Mouse to aim  •  Click or SPACE to swing",
                "← → Arrow keys for fine angle adjustment",
                "Hold to charge power — release to shoot!"
        };
        for (int i=0;i<info.length;i++) {
            fm = g.getFontMetrics();
            g.drawString(info[i], W/2-fm.stringWidth(info[i])/2, H/2-60+i*22);
        }

        // Buttons
        drawButton(g, W/2-160, H/2+50, 140, 44, "👥  2 Players", new Color(255,213,79), new Color(26,10,0), true);
        drawButton(g, W/2+20, H/2+50, 140, 44, "🤖  vs AI", new Color(255,255,255,30), new Color(255,255,255), false);

        // Click detection via mouse
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (screen != Screen.START) return;
                int mx=e.getX(), my=e.getY();
                if (mx>=W/2-160&&mx<=W/2-20&&my>=H/2+50&&my<=H/2+94) startGame(2);
                else if (mx>=W/2+20&&mx<=W/2+160&&my>=H/2+50&&my<=H/2+94) startGame(1);
            }
        });
    }

    void drawGameOverScreen(Graphics2D g) {
        // Already shown via drawMessage
        g.setColor(new Color(179,229,252,140));
        g.setFont(new Font("SansSerif",Font.PLAIN,12));
        String restart = "Click anywhere to play again";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(restart, W/2-fm.stringWidth(restart)/2, H/2+70);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (screen == Screen.GAME_OVER) {
                    screen = Screen.START; message=""; messageTimer=0; repaint();
                }
            }
        });
    }

    void drawButton(Graphics2D g, int x, int y, int w, int h, String label, Color bg, Color fg, boolean filled) {
        if (filled) { g.setColor(bg); g.fillRoundRect(x,y,w,h,30,30); }
        else { g.setColor(bg); g.fillRoundRect(x,y,w,h,30,30); }
        g.setColor(new Color(255,255,255,30)); g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x,y,w,h,30,30); g.setStroke(new BasicStroke(1f));
        g.setColor(fg); g.setFont(new Font("SansSerif",Font.BOLD,13));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, x+w/2-fm.stringWidth(label)/2, y+h/2+5);
    }

    // ── KEY EVENTS ────────────────────────────
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode()==KeyEvent.VK_LEFT) keyLeft=true;
        if (e.getKeyCode()==KeyEvent.VK_RIGHT) keyRight=true;
        if (e.getKeyCode()==KeyEvent.VK_SPACE && !ballInFlight && !isAITurn()) {
            if (!keySpace) { keySpace=true; charging=true; }
        }
    }
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode()==KeyEvent.VK_LEFT) keyLeft=false;
        if (e.getKeyCode()==KeyEvent.VK_RIGHT) keyRight=false;
        if (e.getKeyCode()==KeyEvent.VK_SPACE) { keySpace=false; if(charging){charging=false;shoot();} }
    }
    public void keyTyped(KeyEvent e) {}
}

// ─────────────────────────────────────────────
//  OBSTACLE
// ─────────────────────────────────────────────
class Obstacle {
    double x, y, w, h; int type;
    Obstacle(double x, double y, int type, double w, double h) {
        this.x=x; this.y=y; this.type=type; this.w=w; this.h=h;
    }
}

// ─────────────────────────────────────────────
//  PARTICLE
// ─────────────────────────────────────────────
class Particle {
    double x, y, vx, vy, r;
    int life, maxLife;
    String type;
    Particle(double x, double y, double vx, double vy, String type) {
        this.x=x; this.y=y; this.vx=vx; this.vy=vy; this.type=type;
        r = type.equals("goal") ? 2+Math.random()*3 : 1.5+Math.random()*2;
        life = maxLife = type.equals("goal") ? 40 : 25;
    }
    void update() { x+=vx; y+=vy; vy+=0.2; vx*=0.98; life--; }
}