import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BoxingSimulationGame extends Canvas implements Runnable {

    // Window
    static final int WIDTH = 1280;
    static final int HEIGHT = 720;

    // Ring/world
    static final double FLOOR_Y = HEIGHT * 0.78;
    static final double RING_LEFT = WIDTH * 0.08;
    static final double RING_RIGHT = WIDTH * 0.92;

    private volatile boolean running = true;

    private final boolean[] keys = new boolean[1024];

    private Background background;

    private Boxer p1, p2;
    private AIController cpu2;
    private boolean p2IsCPU = false;

    private double elapsed;
    private double roundTimer = 99.0;
    private boolean pausedForKO = false;
    private double pauseTimer = 0.0;

    private double screenShake = 0.0;
    private final Random rng = new Random();

    private final List<Particle> particles = new ArrayList<>();

    private final Font uiFont = new Font("Arial", Font.PLAIN, 18);
    private final Font bigFont = new Font("Arial", Font.BOLD, 64);

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            JFrame frame = new JFrame("Boxing Simulation Game - Brandenburg Gate (AWT/Swing)");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            BoxingSimulationGame game = new BoxingSimulationGame();
            game.setIgnoreRepaint(true);
            frame.add(game);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            new Thread(game, "GameLoop").start();
        });
    }

    public BoxingSimulationGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int kc = e.getKeyCode();
                if (kc >= 0 && kc < keys.length) keys[kc] = true;
                if (kc == KeyEvent.VK_C) p2IsCPU = !p2IsCPU;
                if (kc == KeyEvent.VK_R) resetMatch();
                if (kc == KeyEvent.VK_ESCAPE) System.exit(0);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int kc = e.getKeyCode();
                if (kc >= 0 && kc < keys.length) keys[kc] = false;
            }
        });

        background = new Background();
        resetMatch();
    }

    private void resetMatch() {
        elapsed = 0;
        roundTimer = 99.0;
        pausedForKO = false;
        pauseTimer = 0.0;
        screenShake = 0.0;
        particles.clear();

        p1 = new Boxer("Player 1", WIDTH * 0.35, FLOOR_Y, true);
        p2 = new Boxer("Player 2", WIDTH * 0.65, FLOOR_Y, false);
        p1.faceRight = true;
        p2.faceRight = false;

        cpu2 = new AIController(p2, p1);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocus();
    }

    @Override
    public void run() {
        createBufferStrategy(3);
        BufferStrategy bs = getBufferStrategy();
        long last = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            double dt = Math.min(0.033, (now - last) / 1_000_000_000.0);
            last = now;

            update(dt);
            render(bs);

            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {}
        }
    }

    private void update(double dt) {
        elapsed += dt;

        if (pausedForKO) {
            pauseTimer -= dt;
            if (pauseTimer <= 0) resetMatch();
            return;
        }

        if (screenShake > 0) screenShake = Math.max(0, screenShake - dt * 2.0);

        background.update(dt, elapsed);

        // Inputs
        readPlayerInput(p1, true, dt);
        if (p2IsCPU) {
            cpu2.update(dt);
        } else {
            readPlayerInput(p2, false, dt);
        }

        // Facing
        p1.faceRight = p1.x <= p2.x;
        p2.faceRight = p2.x >= p1.x;

        // Update boxers
        p1.update(dt);
        p2.update(dt);

        // Ring bounds
        clampToRing(p1);
        clampToRing(p2);

        // Hits
        processHits(p1, p2);
        processHits(p2, p1);

        // Particles
        particles.removeIf(p -> {
            p.update(dt);
            return p.life <= 0;
        });

        // Round timer and KO
        roundTimer -= dt;
        if (roundTimer <= 0) {
            if (p1.health > p2.health) p2.forceKO();
            else if (p2.health > p1.health) p1.forceKO();
            else {
                p1.forceKO(); p2.forceKO();
            }
        }

        if (p1.state == Boxer.State.KO || p2.state == Boxer.State.KO) {
            pausedForKO = true;
            pauseTimer = 3.0;
        }
    }

    private void clampToRing(Boxer b) {
        b.x = Math.max(RING_LEFT + 20, Math.min(RING_RIGHT - 20, b.x));
    }

    private void processHits(Boxer attacker, Boxer defender) {
        if (!attacker.isAttackActive()) return;
        if (defender.state == Boxer.State.KO || defender.invulnTime > 0) return;
        if (attacker.justHitId == defender.id) return; // one hit per attack window

        double reach = attacker.getCurrentAttackReach();
        double hx = attacker.x + (attacker.faceRight ? reach : -reach);
        double hy = attacker.y - 100;
        double hw = 80;
        double hh = 160;

        double dx = defender.x - 30;
        double dy = defender.y - 150;
        double dw = 60;
        double dh = 180;

        if (intersects(hx, hy, hw, hh, dx, dy, dw, dh)) {
            double base = attacker.getCurrentAttackDamage();
            double speedBonus = Math.min(1.3, 1.0 + Math.abs(attacker.vx) * 0.01);
            double counterBonus = 1.0;

            if (defender.state == Boxer.State.ATTACK
                    && defender.attackT < defender.currentAttack.totalTime * 0.4) {
                counterBonus = 1.25;
            }

            double dmg = base * speedBonus * counterBonus;
            boolean guarded = defender.guarding && defender.stamina > 0 && defender.state != Boxer.State.HITSTUN;

            if (guarded) {
                dmg *= 0.3;
                defender.stamina = Math.max(0, defender.stamina - base * 0.6);
                spawnGuardSpark(defender.x, defender.y - 120);
            } else {
                defender.health = Math.max(0, defender.health - dmg);
                defender.hitStun = Math.max(defender.hitStun, 0.14 + base * 0.012);
                defender.vx += (attacker.faceRight ? 1 : -1) * (30 + base * 2.2);
                screenShake += Math.min(1.5, 0.15 + base * 0.02);
                spawnHitSpark(hx, defender.y - 120, attacker.faceRight);
            }

            attacker.registerHit();
            attacker.justHitId = defender.id;

            if (defender.health <= 0 && defender.state != Boxer.State.KO) {
                defender.state = Boxer.State.KO;
                defender.koTimer = 2.5;
                defender.vx = (attacker.faceRight ? 1 : -1) * 140;
                spawnDust(defender.x, FLOOR_Y);
                screenShake = 2.2;
            }
        }
    }

    private boolean intersects(double ax, double ay, double aw, double ah,
                               double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private void readPlayerInput(Boxer b, boolean isP1, double dt) {
        if (b.state == Boxer.State.KO) return;

        boolean left, right, guard, jab, hook, upper, dash;

        if (isP1) {
            left = keys[KeyEvent.VK_A];
            right = keys[KeyEvent.VK_D];
            guard = keys[KeyEvent.VK_S];
            jab = keys[KeyEvent.VK_J];
            hook = keys[KeyEvent.VK_K];
            upper = keys[KeyEvent.VK_L];
            dash = keys[KeyEvent.VK_H];
        } else {
            left = keys[KeyEvent.VK_LEFT];
            right = keys[KeyEvent.VK_RIGHT];
            guard = keys[KeyEvent.VK_DOWN];
            jab = keys[KeyEvent.VK_NUMPAD1] || keys[KeyEvent.VK_1];
            hook = keys[KeyEvent.VK_NUMPAD2] || keys[KeyEvent.VK_2];
            upper = keys[KeyEvent.VK_NUMPAD3] || keys[KeyEvent.VK_3];
            dash = keys[KeyEvent.VK_SHIFT];
        }

        double accel = 1000;
        if (left ^ right) {
            b.vx += (left ? -accel : accel) * dt;
        } else {
            b.vx *= (1.0 - 6.0 * dt);
        }

        if (dash && b.canDash()) {
            b.dash();
        }

        if (guard) b.startGuard(); else b.stopGuard();

        if (jab) b.tryAttack(Boxer.Attack.JAB);
        if (hook) b.tryAttack(Boxer.Attack.HOOK);
        if (upper) b.tryAttack(Boxer.Attack.UPPERCUT);
    }

    private void render(BufferStrategy bs) {
        do {
            do {
                Graphics2D g = (Graphics2D) bs.getDrawGraphics();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                    double shakeX = 0, shakeY = 0;
                    if (screenShake > 0) {
                        shakeX = (rng.nextDouble() - 0.5) * 8 * screenShake;
                        shakeY = (rng.nextDouble() - 0.5) * 6 * screenShake;
                    }

                    background.draw(g, elapsed, shakeX, shakeY);
                    drawRing(g);

                    for (Particle p : particles) if (p.layer == 0) p.draw(g);

                    p1.draw(g);
                    p2.draw(g);

                    for (Particle p : particles) if (p.layer == 1) p.draw(g);

                    drawUI(g);
                } finally {
                    g.dispose();
                }
            } while (bs.contentsRestored());
            bs.show();
            Toolkit.getDefaultToolkit().sync();
        } while (bs.contentsLost());
    }

    private void drawRing(Graphics2D g) {
        float[] fracs = new float[]{0f, 1f};
        Color[] cols = new Color[]{new Color(55,70,90), new Color(30,35,50)};
        LinearGradientPaint matGrad = new LinearGradientPaint(0, (float)(FLOOR_Y-120), 0, (float)(FLOOR_Y+60), fracs, cols, MultipleGradientPaint.CycleMethod.NO_CYCLE);
        g.setPaint(matGrad);
        g.fillRoundRect((int)(RING_LEFT-40), (int)(FLOOR_Y-110), (int)((RING_RIGHT-RING_LEFT)+80), 160, 20, 20);

        g.setColor(new Color(200, 20, 20));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i=0;i<3;i++) {
            double y = FLOOR_Y - 80 - i*18;
            Path2D.Double path = new Path2D.Double();
            path.moveTo(RING_LEFT-35, y+2);
            path.curveTo(WIDTH*0.33, y-6, WIDTH*0.66, y+6, RING_RIGHT+35, y-2);
            g.draw(path);
        }

        g.setColor(new Color(47,79,79));
        g.fillRoundRect((int)(RING_LEFT-45), (int)(FLOOR_Y-140), 20, 160, 8, 8);
        g.fillRoundRect((int)(RING_RIGHT+25), (int)(FLOOR_Y-140), 20, 160, 8, 8);
    }

    private void drawUI(Graphics2D g) {
        g.setFont(uiFont);

        drawBar(g, 40, 30, 500, 20, p1.health / p1.maxHealth, new Color(50,205,50), "P1");
        drawBar(g, WIDTH - 540, 30, 500, 20, p2.health / p2.maxHealth, new Color(255,69,0), "P2");

        drawBar(g, 40, 60, 300, 14, p1.stamina / p1.maxStamina, new Color(0,191,255), null);
        drawBar(g, WIDTH - 340, 60, 300, 14, p2.stamina / p2.maxStamina, new Color(0,191,255), null);

        g.setColor(Color.WHITE);
        g.setFont(bigFont);
        String t = String.format("%02d", Math.max(0, (int)Math.ceil(roundTimer)));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(t, WIDTH/2 - fm.stringWidth(t)/2, 30 + fm.getAscent()/2);

        g.setFont(uiFont);
        String mode = p2IsCPU ? "Mode: P1 vs CPU (C to toggle)" : "Mode: P1 vs P2 (C to toggle)";
        g.setColor(new Color(255,255,255,220));
        g.drawString(mode, WIDTH/2 - 140, 90);

        if (pausedForKO) {
            g.setFont(bigFont);
            String ko = "K.O.";
            FontMetrics fmk = g.getFontMetrics();
            g.setColor(new Color(255,255,255,235));
            g.drawString(ko, WIDTH/2 - fmk.stringWidth(ko)/2, HEIGHT/2);
        }
    }

    private void drawBar(Graphics2D g, int x, int y, int w, int h, double pct, Color color, String label) {
        pct = Math.max(0, Math.min(1, pct));
        g.setColor(new Color(0,0,0,128));
        g.fillRoundRect(x-2, y-2, w+4, h+4, 10, 10);

        float[] fr = new float[]{0f, 1f};
        Color[] cs = new Color[]{new Color(color.getRed(), color.getGreen(), color.getBlue(), 220),
                new Color(Math.min(255, (int)(color.getRed()*0.9)), Math.min(255,(int)(color.getGreen()*0.9)), Math.min(255,(int)(color.getBlue()*0.9)), 240)};
        LinearGradientPaint grad = new LinearGradientPaint(x, y, x, y+h, fr, cs, MultipleGradientPaint.CycleMethod.NO_CYCLE);
        g.setPaint(grad);
        g.fillRoundRect(x, y, (int)(w * pct), h, 8, 8);

        g.setColor(new Color(255,255,255,64));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 8, 8);

        if (label != null) {
            g.setColor(Color.WHITE);
            g.setFont(uiFont);
            g.drawString(label, x + w + 10, y + h - 2);
        }
    }

    private void spawnHitSpark(double x, double y, boolean right) {
        for (int i=0;i<8;i++) {
            double a = (rng.nextDouble()-0.5)*0.8 + (right ? 0 : Math.PI);
            double sp = 160 + rng.nextDouble()*120;
            particles.add(Particle.spark(x, y, Math.cos(a)*sp, -30 + Math.sin(a)*sp*0.2));
        }
    }

    private void spawnGuardSpark(double x, double y) {
        for (int i=0;i<10;i++) {
            double a = rng.nextDouble()*Math.PI*2;
            double sp = 90 + rng.nextDouble()*80;
            particles.add(Particle.guard(x, y, Math.cos(a)*sp, Math.sin(a)*sp));
        }
    }

    private void spawnDust(double x, double y) {
        for (int i=0;i<16;i++) {
            double a = (rng.nextDouble()*0.6 + 0.2)*Math.PI;
            double sp = 120 + rng.nextDouble()*120;
            particles.add(Particle.dust(x, y, Math.cos(a)*sp, -Math.abs(Math.sin(a)*sp*0.7)));
        }
    }

    // ============================= BOXER ===================================
    static int NEXT_ID = 1;

    class Boxer {
        final int id = NEXT_ID++;
        final String name;
        double x, y;
        double vx;

        boolean faceRight = true;

        double health = 100, maxHealth = 100;
        double stamina = 100, maxStamina = 100;
        boolean guarding = false;

        double hitStun = 0;
        double invulnTime = 0;

        enum State { IDLE, MOVE, ATTACK, HITSTUN, KO, DASH }
        State state = State.IDLE;

        double dashTimer = 0, dashCooldown = 0;

        Attack currentAttack = null;
        double attackT = 0;
        int comboSeq = 0;
        int justHitId = -1;

        double koTimer = 0;

        double animT = 0;

        Boxer(String name, double x, double y, boolean leftSide) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.faceRight = !leftSide;
        }

        boolean canDash() { return dashCooldown <= 0 && state != State.ATTACK && hitStun <= 0 && !guarding; }

        void dash() {
            state = State.DASH;
            dashTimer = 0.18;
            dashCooldown = 1.1;
            vx = (faceRight ? 1 : -1) * 580;
            stamina = Math.max(0, stamina - 12);
        }

        void startGuard() { guarding = true; }
        void stopGuard() { guarding = false; }

        void tryAttack(Attack atk) {
            if (state == State.KO || hitStun > 0 || state == State.DASH) return;
            if (stamina < atk.staminaCost) return;
            if (state == State.ATTACK) {
                if (currentAttack.priority <= atk.priority && attackT > currentAttack.windup*0.65) {
                    startAttack(atk);
                }
            } else {
                startAttack(atk);
            }
        }

        void startAttack(Attack atk) {
            currentAttack = atk;
            state = State.ATTACK;
            attackT = 0;
            justHitId = -1;
            stamina = Math.max(0, stamina - atk.staminaCost);
        }

        void registerHit() { comboSeq = (comboSeq + 1) % 3; }

        boolean isAttackActive() {
            if (state != State.ATTACK || currentAttack == null) return false;
            return attackT >= currentAttack.windup && attackT <= (currentAttack.windup + currentAttack.active);
        }

        double getCurrentAttackReach() {
            if (currentAttack == null) return 0;
            double base = currentAttack.reach;
            if (isAttackActive()) base += 12;
            return (faceRight ? base : -base);
        }

        double getCurrentAttackDamage() {
            if (currentAttack == null) return 0;
            double dmg = currentAttack.damage;
            dmg *= (1.0 + 0.06 * comboSeq);
            return dmg;
        }

        void forceKO() {
            if (state == State.KO) return;
            health = 0;
            state = State.KO;
            koTimer = 2.5;
        }

        void update(double dt) {
            animT += dt;

            if (!guarding && state != State.ATTACK) {
                stamina = Math.min(maxStamina, stamina + dt * 12.0);
            } else {
                stamina = Math.min(maxStamina, stamina + dt * 3.0);
            }

            if (dashCooldown > 0) dashCooldown -= dt;
            if (invulnTime > 0) invulnTime -= dt;

            double speedCap = 240;

            switch (state) {
                case KO -> {
                    koTimer -= dt;
                    vx *= (1.0 - 2.5 * dt);
                }
                case HITSTUN -> {
                    hitStun -= dt;
                    if (hitStun <= 0) {
                        state = State.IDLE;
                    }
                }
                case DASH -> {
                    dashTimer -= dt;
                    if (dashTimer <= 0) {
                        state = State.IDLE;
                        vx *= 0.5;
                    }
                }
                case ATTACK -> {
                    attackT += dt;
                    if (isAttackActive()) vx += (faceRight ? 1 : -1) * currentAttack.lunge * dt;

                    if (attackT >= currentAttack.totalTime) {
                        state = State.IDLE;
                        currentAttack = null;
                        attackT = 0;
                        comboSeq = 0;
                    }
                }
                default -> { }
            }

            if (guarding) {
                vx *= (1.0 - 4.0 * dt);
            }

            if (vx > speedCap) vx = speedCap;
            if (vx < -speedCap) vx = -speedCap;

            x += vx * dt;

            if (state != State.ATTACK && state != State.DASH && state != State.KO && hitStun <= 0) {
                state = Math.abs(vx) > 8 ? State.MOVE : State.IDLE;
            }

            if (hitStun > 0 && state != State.KO && state != State.DASH) {
                state = State.HITSTUN;
            }
        }

        void draw(Graphics2D g) {
            Color main = (this == p1) ? new Color(120, 190, 255) : new Color(255, 140, 100);
            Color shorts = (this == p1) ? new Color(20, 60, 150) : new Color(140, 30, 20);
            Color outline = new Color(0,0,0,140);

            double bob = Math.sin(animT * 6 + (this == p1 ? 0 : Math.PI)) * (state == State.MOVE ? 3 : 1.5);

            float alpha = (invulnTime > 0) ? 0.4f : 1.0f;
            if (state == State.KO) alpha = 0.7f;

            Composite oldComp = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            g.setColor(new Color(0,0,0,64));
            g.fillOval((int)(x - 38), (int)(FLOOR_Y - 14), 76, 18);

            double bodyTop = y - 160 + bob;
            double bodyH = 160;
            g.setColor(main);
            g.fillRoundRect((int)(x - 28), (int)bodyTop, 56, (int)bodyH, 18, 18);

            g.setColor(shorts);
            g.fillRoundRect((int)(x - 30), (int)(y - 70 + bob), 60, 55, 14, 14);

            g.setColor(main.brighter());
            g.fillOval((int)(x - 24), (int)(bodyTop - 40), 48, 48);

            double handY = y - 120 + bob;
            double off = guarding ? 12 : 24;
            double gx1 = x + (faceRight ? 14 : -14) - off;
            double gx2 = x + (faceRight ? 40 : -40) - off;

            if (state == State.ATTACK && isAttackActive()) {
                gx2 = x + (faceRight ? 68 : -68);
                handY -= 8;
            }
            if (!faceRight) {
                double t = gx1; gx1 = gx2; gx2 = t;
            }

            g.setColor(new Color(220,40,40));
            g.fillOval((int)(gx1 - 14), (int)(handY - 10), 28, 22);
            g.fillOval((int)(gx2 - 14), (int)(handY - 10), 28, 22);

            g.setColor(new Color(0,0,0,150));
            double eyeX = x + (faceRight ? 8 : -8);
            g.fillOval((int)(eyeX - 3), (int)(bodyTop - 22), 6, 6);

            if (guarding) {
                g.setColor(new Color(204,230,255,153));
                g.setStroke(new BasicStroke(3f));
                g.drawArc((int)(x - 40), (int)(y - 160), 80, 120, faceRight ? 340 : 20, 160);
            }

            g.setColor(outline);
            g.setStroke(new BasicStroke(2.4f));
            g.drawRoundRect((int)(x - 28), (int)bodyTop, 56, (int)bodyH, 18, 18);
            g.drawOval((int)(x - 24), (int)(bodyTop - 40), 48, 48);

            g.setComposite(oldComp);
        }

        enum Attack {
            JAB(0.10, 0.10, 0.15, 24, 85, 7, 1),
            HOOK(0.18, 0.16, 0.22, 40, 95, 12, 2),
            UPPERCUT(0.22, 0.18, 0.26, 48, 80, 14, 3);

            final double windup, active, recover, damage, reach, staminaCost;
            final int priority;
            final double totalTime, lunge;

            Attack(double windup, double active, double recover, double damage,
                   double reach, double staminaCost, int priority) {
                this.windup = windup;
                this.active = active;
                this.recover = recover;
                this.damage = damage;
                this.reach = reach;
                this.staminaCost = staminaCost;
                this.priority = priority;
                this.totalTime = windup + active + recover;
                this.lunge = 120 + priority * 20;
            }
        }
    }

    // ============================= AI ======================================
    static class AIController {
        final BoxingSimulationGame.Boxer self, foe;
        enum Mode { APPROACH, POKE, FLURRY, GUARD, RETREAT, IDLE }
        Mode mode = Mode.APPROACH;
        double modeTimer = 0;
        final Random r = new Random();

        AIController(BoxingSimulationGame.Boxer self, BoxingSimulationGame.Boxer foe) {
            this.self = self;
            this.foe = foe;
        }

        void update(double dt) {
            modeTimer -= dt;

            double dist = Math.abs(foe.x - self.x);
            if (self.state == BoxingSimulationGame.Boxer.State.KO) return;
            if (self.hitStun > 0) {
                holdGuard(true);
                return;
            }

            boolean foeThreat = foe.state == BoxingSimulationGame.Boxer.State.ATTACK
                    && foe.attackT < foe.currentAttack.totalTime * 0.4 + foe.currentAttack.active * 0.2;

            if (foeThreat && dist < 130 && self.stamina > 10) {
                switchMode(Mode.GUARD, 0.25 + r.nextDouble()*0.25);
            } else if (self.stamina < 15) {
                switchMode(Mode.RETREAT, 0.6 + r.nextDouble()*0.6);
            } else if (dist > 260) {
                switchMode(Mode.APPROACH, 0.8 + r.nextDouble()*0.4);
            } else if (dist > 150) {
                switchMode(Mode.POKE, 0.6 + r.nextDouble()*0.4);
            } else {
                switchMode(Mode.FLURRY, 0.5 + r.nextDouble()*0.5);
            }

            switch (mode) {
                case APPROACH -> {
                    moveToward(foe.x, dt);
                    holdGuard(false);
                    if (dist < 180 && r.nextDouble() < 0.02) self.tryAttack(BoxingSimulationGame.Boxer.Attack.JAB);
                }
                case POKE -> {
                    strafe(dt, foe.x);
                    if (dist < 220 && r.nextDouble() < 0.05) self.tryAttack(BoxingSimulationGame.Boxer.Attack.JAB);
                    if (dist < 200 && r.nextDouble() < 0.03) self.tryAttack(BoxingSimulationGame.Boxer.Attack.HOOK);
                }
                case FLURRY -> {
                    if (dist > 140) moveToward(foe.x, dt);
                    holdGuard(false);
                    if (self.state != BoxingSimulationGame.Boxer.State.ATTACK) {
                        if (r.nextDouble() < 0.6) self.tryAttack(BoxingSimulationGame.Boxer.Attack.JAB);
                        else if (r.nextDouble() < 0.5) self.tryAttack(BoxingSimulationGame.Boxer.Attack.HOOK);
                        else self.tryAttack(BoxingSimulationGame.Boxer.Attack.UPPERCUT);
                    }
                }
                case GUARD -> {
                    holdGuard(true);
                    if (dist > 160 && r.nextDouble() < 0.1) moveToward(foe.x, dt);
                }
                case RETREAT -> {
                    moveAway(foe.x, dt);
                    holdGuard(r.nextDouble() < 0.7);
                }
                case IDLE -> holdGuard(false);
            }
        }

        void switchMode(Mode m, double dur) {
            if (modeTimer <= 0 || m != mode) {
                mode = m; modeTimer = dur;
            }
        }
        void moveToward(double x, double dt) {
            boolean left = self.x > x;
            if (left) self.vx -= 900 * dt; else self.vx += 900 * dt;
            if (self.canDash() && Math.abs(self.x - x) > 260 && r.nextDouble() < 0.02) self.dash();
        }
        void moveAway(double x, double dt) {
            boolean left = self.x < x;
            if (left) self.vx -= 900 * dt; else self.vx += 900 * dt;
        }
        void strafe(double dt, double x) {
            if (r.nextDouble() < 0.5) moveToward(x+40, dt); else moveToward(x-40, dt);
        }
        void holdGuard(boolean on) { if (on) self.startGuard(); else self.stopGuard(); }
    }

    // ========================= BACKGROUND (Brandenburg Gate) ===============
    class Background {
        private final List<Cloud> clouds = new ArrayList<>();
        private final List<Bird> birds = new ArrayList<>();
        private final List<Streak> streaks = new ArrayList<>();
        private double camBobT = 0;
        private double flagT = 0;
        private double glowT = 0;

        private final double gateCX = WIDTH * 0.52;
        private final double gateBaseY = FLOOR_Y - 50;

        Background() {
            for (int i=0;i<8;i++) clouds.add(new Cloud(rand(0, WIDTH), rand(60, 220), rand(90, 200), rand(0.05, 0.12)));
            for (int i=0;i<5;i++) birds.add(new Bird(rand(0, WIDTH), rand(140, 300), rand(60, 120), rng.nextBoolean()));
            for (int i=0;i<10;i++) {
                double y = gateBaseY - 28 + (i%2==0? 0 : 10);
                streaks.add(new Streak(rand(-200, WIDTH+200), y, rand(120, 220), rand(60, 140),
                        new Color(255, 220, 140, 120 + rng.nextInt(70))));
            }
        }

        void update(double dt, double elapsed) {
            camBobT += dt * 0.4;
            flagT += dt * 2.0;
            glowT += dt * 1.2;

            for (Cloud c : clouds) c.update(dt);
            for (Bird b : birds) b.update(dt);
            for (Streak s : streaks) s.update(dt);

            for (Cloud c : clouds) if (c.x > WIDTH + 220) { c.x = -220; c.y = rand(60, 240); }
            for (Bird b : birds) {
                if (b.right && b.x > WIDTH + 100) { b.x = -100; b.y = rand(140, 300); }
                if (!b.right && b.x < -100) { b.x = WIDTH + 100; b.y = rand(140, 300); }
            }
            for (Streak s : streaks) if (s.x > WIDTH + 260) { s.x = -260; s.y = gateBaseY - 30 + rng.nextInt(18); }
        }

        void draw(Graphics2D g, double time, double shakeX, double shakeY) {
            double camX = Math.sin(camBobT) * 6 + shakeX;
            double camY = Math.sin(camBobT * 0.7) * 4 + shakeY;

            AffineTransform oldTx = g.getTransform();
            g.translate(camX, camY);

            drawSky(g);
            for (Cloud c : clouds) c.draw(g);
            drawPariserPlatz(g, gateCX, gateBaseY);
            // Headlight streaks pass behind the gate columns
            for (Streak s : streaks) s.draw(g);

            drawBrandenburgGate(g, gateCX, gateBaseY, 620, 260, time);
            drawTreesAndForeground(g);

            for (Bird b : birds) b.draw(g);

            g.setTransform(oldTx);
        }

        private void drawSky(Graphics2D g) {
            float[] skyF = new float[]{0f, 0.55f, 1f};
            Color[] skyC = new Color[]{
                    new Color(18, 28, 55),     // upper twilight
                    new Color(50, 70, 120),    // mid
                    new Color(220, 150, 90)    // horizon glow
            };
            LinearGradientPaint sky = new LinearGradientPaint(0, 0, 0, HEIGHT, skyF, skyC, MultipleGradientPaint.CycleMethod.NO_CYCLE);
            g.setPaint(sky);
            g.fillRect(0,0,WIDTH,HEIGHT);
        }

        private void drawPariserPlatz(Graphics2D g, double cx, double baseY) {
            // Silhouettes of buildings left/right with glowing windows
            int h = 110;
            int top = (int)(baseY - h - 18);
            // Left block
            g.setColor(new Color(30, 40, 60, 200));
            g.fillRect((int)(cx - 560), top + 16, 220, h);
            g.fillRect((int)(cx - 330), top + 30, 140, h - 14);
            // Right block
            g.fillRect((int)(cx + 340), top + 16, 220, h);
            g.fillRect((int)(cx + 180), top + 28, 140, h - 12);

            // Window grid with soft flicker
            Random r = new Random(42);
            for (int side=0; side<2; side++) {
                int bx = (side==0) ? (int)(cx - 560) : (int)(cx + 340);
                for (int yy=top+22; yy<top+h+10; yy+=14) {
                    for (int xx=bx+10; xx<bx+210; xx+=18) {
                        int flicker = 180 + (int)(60 * Math.sin(glowT*1.5 + xx*0.05 + yy*0.03));
                        g.setColor(new Color(255, 220, 140, Math.max(0, Math.min(255, flicker))));
                        g.fillRect(xx, yy, 10, 6);
                    }
                }
            }
        }

        private void drawBrandenburgGate(Graphics2D g, double cx, double baseY, double w, double h, double time) {
            // Plinth (ground step)
            g.setColor(new Color(60, 60, 70, 180));
            g.fillRect((int)(cx - w*0.55), (int)(baseY - 10), (int)(w*1.10), 10);

            // Uplight beams (soft vertical glows)
            for (int i=0;i<6;i++) {
                double x = cx - w*0.45 + i * (w*0.18);
                float a = (float)(0.08 + 0.06*Math.sin(glowT*1.8 + i));
                GradientPaint beam = new GradientPaint((float)x, (float)(baseY-4), new Color(255, 240, 190, (int)(a*255)),
                        (float)x, (float)(baseY - h*0.65), new Color(255, 240, 190, 0));
                Paint old = g.getPaint();
                g.setPaint(beam);
                g.fill(new Rectangle2D.Double(x - 18, baseY - h*0.65, 36, h*0.65));
                g.setPaint(old);
            }

            // Entablature (top)
            float[] fr = new float[]{0f,1f};
            Color[] cs = new Color[]{new Color(200, 190, 150), new Color(150, 135, 100)};
            LinearGradientPaint ent = new LinearGradientPaint((float)cx, (float)(baseY - h*0.7), (float)cx, (float)(baseY - h*0.45),
                    fr, cs, MultipleGradientPaint.CycleMethod.NO_CYCLE);
            g.setPaint(ent);
            g.fillRect((int)(cx - w*0.50), (int)(baseY - h*0.70), (int)(w*1.00), (int)(h*0.26));
            g.setColor(new Color(90, 80, 60, 120));
            g.drawRect((int)(cx - w*0.50), (int)(baseY - h*0.70), (int)(w*1.00), (int)(h*0.26));

            // Columns (6 Doric)
            int cols = 6;
            double left = cx - w*0.48;
            double span = w*0.96;
            double cw = span / (cols*1.0 + (cols-1)*0.4); // include gaps
            double gap = cw*0.4;
            for (int i=0;i<cols;i++) {
                double x = left + i*(cw+gap);
                drawColumn(g, x, baseY, cw, h*0.66);
            }

            // Arches between columns (5 passages)
            g.setColor(new Color(40, 45, 60, 220));
            int passages = 5;
            for (int i=0;i<passages;i++) {
                double ax = left + cw + i*(cw+gap);
                double awid = gap + cw*0.0; // effectively the space
                double archW = gap + cw;
                double archH = h*0.44;
                double ay = baseY - archH;
                // central arch taller
                if (i==2) { archH *= 1.12; ay = baseY - archH; }
                RoundRectangle2D arch = new RoundRectangle2D.Double(ax - cw*0.1, ay, archW + cw*0.2, archH, 24, 24);
                g.fill(arch);
            }

            // Quadriga silhouette on top
            drawQuadriga(g, cx, baseY - h*0.76, w*0.26, time);
        }

        private void drawColumn(Graphics2D g, double x, double baseY, double w, double h) {
            // Shaft gradient
            float[] fr = new float[]{0f,1f};
            Color[] cs = new Color[]{new Color(210, 200, 165), new Color(150, 135, 100)};
            LinearGradientPaint lg = new LinearGradientPaint((float)(x+w/2), (float)(baseY - h), (float)(x+w/2), (float)baseY, fr, cs, MultipleGradientPaint.CycleMethod.NO_CYCLE);
            Paint old = g.getPaint();
            g.setPaint(lg);
            g.fillRoundRect((int)x, (int)(baseY - h), (int)w, (int)h, 12, 12);
            g.setPaint(old);

            // Fluting lines
            g.setColor(new Color(110, 100, 80, 90));
            g.setStroke(new BasicStroke(1.4f));
            int flutes = 6;
            for (int i=1;i<flutes;i++){
                double t = i/(double)flutes;
                double fx = x + t*w;
                g.draw(new Line2D.Double(fx, baseY - h + 6, fx, baseY - 6));
            }

            // Capital and base lines
            g.setColor(new Color(120, 110, 90));
            g.fillRect((int)(x - w*0.06), (int)(baseY - h - 10), (int)(w*1.12), 10);
            g.fillRect((int)(x - w*0.04), (int)baseY, (int)(w*1.08), 8);
        }

        private void drawQuadriga(Graphics2D g, double cx, double y, double w, double time) {
            // Slight bobbing
            double bob = Math.sin(time*1.2) * 1.2;

            // Chariot base
            g.setColor(new Color(20, 30, 40, 210));
            g.fill(new RoundRectangle2D.Double(cx - w*0.26, y - 10 + bob, w*0.52, 16, 10, 10));
            // Wheels (rotate spokes)
            double wheelR = w*0.07;
            double wx1 = cx - w*0.16, wx2 = cx + w*0.16;
            double wy = y + bob + 6;
            double ang = time * 2.0;
            drawWheel(g, wx1, wy, wheelR, ang);
            drawWheel(g, wx2, wy, wheelR, ang + 0.6);

            // Horses (4 simplified silhouettes)
            for (int i=0;i<4;i++) {
                double hx = cx - w*0.34 + i*(w*0.22);
                double ho = Math.sin(time*2 + i*0.8)*1.0;
                drawHorseSilhouette(g, hx, y - 6 + bob + ho, w*0.16);
            }

            // Victory figure and reins (simple)
            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(20, 30, 40, 220));
            g.draw(new Line2D.Double(cx, y - 16 + bob, cx, y - 32 + bob));
            g.fill(new Ellipse2D.Double(cx - 3, y - 38 + bob, 6, 6)); // head
            // Reins
            g.draw(new QuadCurve2D.Double(cx, y - 20 + bob, cx + w*0.06, y - 18 + bob, cx + w*0.14, y - 10 + bob));
            g.draw(new QuadCurve2D.Double(cx, y - 20 + bob, cx - w*0.06, y - 18 + bob, cx - w*0.14, y - 10 + bob));
        }

        private void drawWheel(Graphics2D g, double cx, double cy, double r, double ang) {
            g.setColor(new Color(20, 30, 40, 220));
            g.fill(new Ellipse2D.Double(cx - r, cy - r, r*2, r*2));
            g.setColor(new Color(50, 60, 70));
            g.setStroke(new BasicStroke(2f));
            for (int i=0;i<6;i++) {
                double a = ang + i * Math.PI/3;
                double x = cx + Math.cos(a)*r;
                double y = cy + Math.sin(a)*r;
                g.draw(new Line2D.Double(cx, cy, x, y));
            }
        }

        private void drawHorseSilhouette(Graphics2D g, double x, double y, double s) {
            // Body
            g.setColor(new Color(20, 30, 40, 220));
            g.fill(new RoundRectangle2D.Double(x - s*0.30, y - s*0.16, s*0.60, s*0.24, 8, 8));
            // Neck/head
            Path2D neck = new Path2D.Double();
            neck.moveTo(x + s*0.14, y - s*0.10);
            neck.curveTo(x + s*0.26, y - s*0.26, x + s*0.34, y - s*0.14, x + s*0.36, y - s*0.02);
            neck.lineTo(x + s*0.30, y + s*0.02);
            neck.lineTo(x + s*0.16, y - s*0.04);
            neck.closePath();
            g.fill(neck);
            // Legs (simplified)
            g.fill(new Rectangle2D.Double(x - s*0.22, y + s*0.06, s*0.06, s*0.22));
            g.fill(new Rectangle2D.Double(x - s*0.02, y + s*0.06, s*0.06, s*0.22));
        }

        private void drawTreesAndForeground(Graphics2D g) {
            // Tiergarten tree silhouettes
            g.setColor(new Color(20, 45, 35, 160));
            for (int i=0;i<10;i++) {
                double x = i * WIDTH/10.0 + 20;
                double y = gateBaseY + Math.sin(i)*2;
                g.fill(new Ellipse2D.Double(x, y - 26, 90, 30));
                g.fill(new Ellipse2D.Double(x + 16, y - 38, 70, 24));
            }

            // Ground pavement
            float[] fr = new float[]{0f,1f};
            Color[] cs = new Color[]{new Color(80,80,85, 200), new Color(50,50,60, 240)};
            LinearGradientPaint pave = new LinearGradientPaint(0, (float)FLOOR_Y, 0, HEIGHT, fr, cs, MultipleGradientPaint.CycleMethod.NO_CYCLE);
            g.setPaint(pave);
            g.fillRect(0, (int)FLOOR_Y, WIDTH, (int)(HEIGHT - FLOOR_Y));

            // Flags on both sides (waving)
            drawGermanFlag(g, gateCX - 360, gateBaseY - 140, 46, 22, flagT);
            drawGermanFlag(g, gateCX + 360, gateBaseY - 140, 46, 22, flagT + 0.8);
        }

        private void drawGermanFlag(Graphics2D g, double x, double y, double w, double h, double t) {
            // Pole
            g.setColor(new Color(200,200,200));
            g.fill(new Rectangle2D.Double(x - 2, y - 4, 3, h + 26));
            // Simple waving stripes
            double wave = Math.sin(t) * 6;
            Path2D stripe = new Path2D.Double();
            for (int row=0; row<3; row++) {
                double y0 = y + row*(h/3.0);
                double a = wave * (1 - row*0.2);
                stripe.reset();
                stripe.moveTo(x, y0);
                stripe.curveTo(x + w*0.35, y0 + a*0.3, x + w*0.68, y0 - a*0.4, x + w, y0 + a*0.2);
                stripe.lineTo(x + w, y0 + h/3.0 + a*0.2);
                stripe.curveTo(x + w*0.68, y0 + h/3.0 - a*0.4, x + w*0.35, y0 + h/3.0 + a*0.3, x, y0 + h/3.0);
                stripe.closePath();
                if (row==0) g.setColor(Color.BLACK);
                else if (row==1) g.setColor(new Color(200, 20, 20));
                else g.setColor(new Color(240, 200, 40));
                g.fill(stripe);
            }
        }

        double rand(double a, double b) { return a + Math.random()*(b-a); }

        class Cloud {
            double x,y,w,s;
            Cloud(double x,double y,double w,double s){this.x=x;this.y=y;this.w=w;this.s=s;}
            void update(double dt){ x += s * 40 * dt; }
            void draw(Graphics2D g){
                g.setColor(new Color(255,255,255,200));
                for (int i=0;i<5;i++){
                    double ox = (i-2)*w*0.18;
                    double oy = (i%2==0? -8: 6);
                    g.fill(new Ellipse2D.Double(x+ox, y+oy, w*0.45, w*0.25));
                }
            }
        }
        class Bird {
            double x,y,spd; boolean right; double flap;
            Bird(double x,double y,double spd, boolean right){this.x=x;this.y=y;this.spd=spd;this.right=right;}
            void update(double dt){ x += (right?1:-1)*spd*dt; flap += dt*10; }
            void draw(Graphics2D g){
                g.setColor(new Color(0,0,0,180));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double a = Math.sin(flap)*6 + 10;
                double dir = right?1:-1;
                g.drawLine((int)x, (int)y, (int)(x - dir*10), (int)(y - a));
                g.drawLine((int)x, (int)y, (int)(x - dir*10), (int)(y + a));
            }
        }
        class Streak {
            double x,y,speed,len;
            Color color;
            Streak(double x,double y,double speed,double len, Color color){
                this.x=x;this.y=y;this.speed=speed;this.len=len;this.color=color;
            }
            void update(double dt){ x += speed * dt; }
            void draw(Graphics2D g){
                g.setColor(color);
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(x, y, x + len, y));
            }
        }
    }

    // =========================== PARTICLES =================================
    static class Particle {
        double x,y,vx,vy,life;
        int layer;
        Color color;
        double size;
        int type; // 0 dust, 1 spark, 2 guard

        static Particle dust(double x,double y,double vx,double vy){
            Particle p = new Particle();
            p.x=x; p.y=y; p.vx=vx; p.vy=vy; p.life=0.8+Math.random()*0.5;
            p.layer=0; p.color = new Color(153,128,77,204); p.size=4+Math.random()*5; p.type=0;
            return p;
        }
        static Particle spark(double x,double y,double vx,double vy){
            Particle p = new Particle();
            p.x=x; p.y=y; p.vx=vx; p.vy=vy; p.life=0.3+Math.random()*0.2;
            p.layer=1; p.color = new Color(255,230,128,255); p.size=3+Math.random()*3; p.type=1;
            return p;
        }
        static Particle guard(double x,double y,double vx,double vy){
            Particle p = new Particle();
            p.x=x; p.y=y; p.vx=vx; p.vy=vy; p.life=0.25+Math.random()*0.2;
            p.layer=1; p.color = new Color(179,217,255,255); p.size=2+Math.random()*2; p.type=2;
            return p;
        }

        void update(double dt){
            life -= dt;
            x += vx*dt;
            y += vy*dt;
            if (type==0){
                vy += 200*dt;
            } else {
                vx *= (1.0 - 4*dt);
                vy *= (1.0 - 4*dt);
            }
        }
        void draw(Graphics2D g){
            float a = (float)Math.max(0, Math.min(1, life*2));
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g.setColor(color);
            if (type==1 || type==2) {
                g.fill(new Ellipse2D.Double(x - size*0.5, y - size*0.5, size, size));
            } else {
                g.fill(new Ellipse2D.Double(x - size, y - size*0.4, size*2, size*0.8));
            }
            g.setComposite(old);
        }
    }
}