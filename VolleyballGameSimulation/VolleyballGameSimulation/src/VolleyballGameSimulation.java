import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class VolleyballGameSimulation extends JPanel implements ActionListener, KeyListener {

    private static final int WINDOW_WIDTH  = 1200;
    private static final int WINDOW_HEIGHT = 700;
    private static final int GROUND_Y      = WINDOW_HEIGHT - 80;
    private static final int NET_X         = WINDOW_WIDTH / 2;
    private static final int NET_WIDTH     = 20;
    private static final int NET_HEIGHT    = 320;
    private static final int COURT_MARGIN  = 80;

    private static final double GRAVITY          = 0.38;
    private static final double BALL_RADIUS      = 14;
    private static final double BALL_MAX_SPEED   = 28.0;
    private static final double JUMP_POWER       = -14.2;
    private static final double MOVE_SPEED       = 5.8;
    private static final double AIR_CONTROL      = 0.38;
    private static final double FRICTION_GROUND  = 0.84;
    private static final double HIT_POWER_BASE   = 16.5;
    private static final double SPIKE_MULTIPLIER = 1.95;
    private static final double SERVE_POWER      = 19.0;

    private static final Color COLOR_COURT      = new Color(245, 222, 170);
    private static final Color COLOR_LINES      = new Color(220, 40,  40);
    private static final Color COLOR_NET        = new Color(60,  60,  220);
    private static final Color COLOR_BALL       = new Color(255, 245, 120);
    private static final Color COLOR_PLAYER_A   = new Color(30,  100, 220);
    private static final Color COLOR_PLAYER_B   = new Color(220, 40,  60);
    private static final Color COLOR_SHADOW     = new Color(0,0,0,90);

    // ===============================================================
    //   GAME STATE
    // ===============================================================
    private Timer timer;
    private Random random = new Random();

    private Ball ball;
    private Player[] teamA = new Player[6];
    private Player[] teamB = new Player[6];

    private int scoreA = 0;
    private int scoreB = 0;
    private int rallyLength = 0;
    private int setNumber = 1;
    private boolean servingTeamIsA = true;
    private boolean pointInProgress = false;
    private String lastWinner = "";
    private String message = "Press SPACE to serve";

    private boolean gameOver = false;
    private int winningSetPoints = 25;
    private int pointsToWinMatch = 3;

    // Input flags
    private static boolean keyLeftA;
    private static boolean keyRightA;
    private boolean keyJumpA;
    private boolean keySpikeA;
    private boolean keyBlockA;
    private static boolean keyLeftB;
    private static boolean keyRightB;
    private boolean keyJumpB;
    private boolean keySpikeB;
    private boolean keyBlockB;

    // Camera / view
    private double cameraX = 0;

    // Trail effect
    private List<Point2D.Double> ballTrail = new ArrayList<>();
    private static final int MAX_TRAIL = 18;


    static class Ball {
        double x, y;
        double vx, vy;
        double radius;
        boolean inPlay = true;
        int lastHitByTeam = 0;     // 1 = A, 2 = B
        int touchesTeamA = 0;
        int touchesTeamB = 0;
        boolean justServed = false;

        Ball(double startX, double startY) {
            this.x = startX;
            this.y = startY;
            this.radius = BALL_RADIUS;
            vx = vy = 0;
        }

        void applyPhysics() {
            vy += GRAVITY;
            x += vx;
            y += vy;
        }

        void limitSpeed() {
            double speed = Math.hypot(vx, vy);
            if (speed > BALL_MAX_SPEED) {
                double factor = BALL_MAX_SPEED / speed;
                vx *= factor;
                vy *= factor;
            }
        }

        Rectangle2D getBounds() {
            return new Rectangle2D.Double(x - radius, y - radius, radius*2, radius*2);
        }
    }

    static class Player {
        double x, y;
        double vx, vy;
        int team;           // 1 or 2
        boolean onGround = true;
        int jumpCooldown = 0;
        String name;
        int jerseyNumber;
        boolean isAIControlled = true;
        double targetX;       // for AI

        // Animation states
        int frame = 0;
        String currentAction = "stand";

        Player(double startX, int teamId, String playerName, int number) {
            this.x = startX;
            this.y = GROUND_Y;
            this.team = teamId;
            this.name = playerName;
            this.jerseyNumber = number;
            vx = vy = 0;
            targetX = startX;
        }

        void update() {
            if (jumpCooldown > 0) jumpCooldown--;

            if (!onGround) {
                vy += GRAVITY;
                y += vy;
                x += vx;

                // air control (limited)
                if (team == 1) {
                    if (keyLeftA)  vx -= AIR_CONTROL;
                    if (keyRightA) vx += AIR_CONTROL;
                } else {
                    if (keyLeftB)  vx -= AIR_CONTROL;
                    if (keyRightB) vx += AIR_CONTROL;
                }

                if (y >= GROUND_Y) {
                    land();
                }
            } else {
                // ground friction
                vx *= FRICTION_GROUND;

                if (Math.abs(vx) < 0.4) vx = 0;
            }

            // animation frame
            if (Math.abs(vx) > 1.2) {
                frame = (frame + 1) % 24;
                currentAction = "run";
            } else if (!onGround) {
                currentAction = "jump";
            } else {
                currentAction = "stand";
            }
        }

        void jump() {
            if (onGround && jumpCooldown <= 0) {
                vy = JUMP_POWER;
                onGround = false;
                jumpCooldown = 12;
                currentAction = "jump";
            }
        }

        void land() {
            y = GROUND_Y;
            vy = 0;
            onGround = true;
            jumpCooldown = 8;
        }

        Rectangle2D getHitbox() {
            return new Rectangle2D.Double(x-24, y-90, 48, 90);
        }

        Rectangle2D getHandArea() {
            // rough approximation of hand position
            double handX = x + (team==1 ? 22 : -22);
            return new Rectangle2D.Double(handX-18, y-110, 36, 50);
        }
    }

    // ===============================================================
    //   CONSTRUCTOR & INITIALIZATION
    // ===============================================================

    public VolleyballGameSimulation() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(new Color(135, 206, 235));
        setFocusable(true);
        addKeyListener(this);

        initializePlayers();
        resetBallForServe();

        timer = new Timer(1000/60, this);
        timer.start();
    }

    private void initializePlayers() {
        // Team A (left side) - blue
        teamA[0] = new Player(280, 1, "Setter",    1); teamA[0].isAIControlled = false;
        teamA[1] = new Player(380, 1, "Opposite",  2);
        teamA[2] = new Player(480, 1, "Middle",    3);
        teamA[3] = new Player(220, 1, "Libero",    4);
        teamA[4] = new Player(340, 1, "Outside",   5);
        teamA[5] = new Player(440, 1, "Middle",    6);

        // Team B (right side) - red
        teamB[0] = new Player(WINDOW_WIDTH-280, 2, "Setter",    1); teamB[0].isAIControlled = false;
        teamB[1] = new Player(WINDOW_WIDTH-380, 2, "Opposite",  2);
        teamB[2] = new Player(WINDOW_WIDTH-480, 2, "Middle",    3);
        teamB[3] = new Player(WINDOW_WIDTH-220, 2, "Libero",    4);
        teamB[4] = new Player(WINDOW_WIDTH-340, 2, "Outside",   5);
        teamB[5] = new Player(WINDOW_WIDTH-440, 2, "Middle",    6);

        // Place players in more realistic positions at start
        repositionPlayersForServe();
    }

    private void repositionPlayersForServe() {
        // Simple formation - can be extended later
        double[] posA = {260, 340, 420, 220, 380, 460};
        double[] posB = {WINDOW_WIDTH-260, WINDOW_WIDTH-340, WINDOW_WIDTH-420,
                WINDOW_WIDTH-220, WINDOW_WIDTH-380, WINDOW_WIDTH-460};

        for (int i = 0; i < 6; i++) {
            teamA[i].x = posA[i];
            teamA[i].y = GROUND_Y;
            teamA[i].vx = teamA[i].vy = 0;
            teamA[i].onGround = true;

            teamB[i].x = posB[i];
            teamB[i].y = GROUND_Y;
            teamB[i].vx = teamB[i].vy = 0;
            teamB[i].onGround = true;
        }
    }

    private void resetBallForServe() {
        pointInProgress = false;
        rallyLength = 0;
        ball = new Ball(servingTeamIsA ? 320 : WINDOW_WIDTH-320, 180);
        ball.vx = 0;
        ball.vy = 0;
        ball.inPlay = true;
        ball.lastHitByTeam = 0;
        ball.touchesTeamA = 0;
        ball.touchesTeamB = 0;
        ball.justServed = true;

        message = servingTeamIsA ? "Team A to serve - press SPACE" : "Team B to serve - press SPACE / ENTER";
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        updatePhysics();
        updateAI();
        checkScoring();
        updateCamera();
        updateTrail();

        repaint();
    }

    private void checkScoring() {
    }

    private void updatePhysics() {
        if (!pointInProgress && ball.justServed) {
            // waiting for serve
            return;
        }

        ball.applyPhysics();
        ball.limitSpeed();

        // Players movement & physics
        for (Player p : teamA) p.update();
        for (Player p : teamB) p.update();

        // Ball – ground / ceiling / wall collision
        handleBallBoundaries();

        // Ball – net collision
        handleNetCollision();

        // Ball – player hit detection
        handlePlayerBallInteraction();
    }

    private void handleBallBoundaries() {
        // floor
        if (ball.y + ball.radius > GROUND_Y) {
            ball.y = GROUND_Y - ball.radius;
            ball.vy = -ball.vy * 0.68; // bounce ~68%
            ball.vx *= 0.82;

            if (Math.abs(ball.vy) < 1.8) {
                ball.vy = 0;
                ball.inPlay = false;
                // score point to the OTHER team
                givePoint(ball.lastHitByTeam == 1 ? 2 : 1);
            }
        }

        // ceiling (rare)
        if (ball.y - ball.radius < 40) {
            ball.y = 40 + ball.radius;
            ball.vy = -ball.vy * 0.75;
        }

        // left/right out (simplified)
        if (ball.x < -80 || ball.x > WINDOW_WIDTH + 80) {
            ball.inPlay = false;
            givePoint(ball.x < NET_X ? 2 : 1);
        }
    }

    private void handleNetCollision() {
        Rectangle2D netRect = new Rectangle2D.Double(
                NET_X - NET_WIDTH/2, GROUND_Y - NET_HEIGHT, NET_WIDTH, NET_HEIGHT);

        if (ball.getBounds().intersects(netRect)) {
            // very simple net reaction
            if (ball.x < NET_X) {
                ball.x = NET_X - NET_WIDTH/2 - ball.radius - 1;
                ball.vx = -ball.vx * 0.65 - 2.0;
            } else {
                ball.x = NET_X + NET_WIDTH/2 + ball.radius + 1;
                ball.vx = -ball.vx * 0.65 + 2.0;
            }
            ball.vy *= 0.88;
        }
    }

    private void handlePlayerBallInteraction() {
        if (!ball.inPlay) return;

        Player[] hitCandidates = (ball.x < NET_X) ? teamA : teamB;

        for (Player p : hitCandidates) {
            if (p.getHandArea().intersects(ball.getBounds())) {
                performHit(p);
                break;
            }
        }
    }

    private void performHit(Player hitter) {
        rallyLength++;
        ball.justServed = false;

        if (hitter.team == 1) {
            ball.touchesTeamA++;
            ball.touchesTeamB = 0;
            ball.lastHitByTeam = 1;
        } else {
            ball.touchesTeamB++;
            ball.touchesTeamA = 0;
            ball.lastHitByTeam = 2;
        }

        // direction depends on team
        double dirX = (hitter.team == 1) ? 1.0 : -1.0;

        // base power
        double power = HIT_POWER_BASE;

        // spike / attack vs normal hit
        boolean isSpikeAttempt = (hitter.team == 1) ? keySpikeA : keySpikeB;
        if (isSpikeAttempt && !hitter.onGround) {
            power *= SPIKE_MULTIPLIER;
            ball.vy = -16.5 - random.nextDouble()*3.5;
        } else if (!hitter.onGround) {
            // normal air hit
            power *= 1.25;
            ball.vy = -11.0 - random.nextDouble()*4.0;
        } else {
            // standing hit / pass
            ball.vy = -5.5 - random.nextDouble()*3.0;
        }

        // angle depending on position relative to net
        double dxNet = NET_X - hitter.x;
        double angleFactor = dxNet / 400.0; // -1 .. 1 range approx

        // add some randomness
        double angleRandom = (random.nextDouble()-0.5) * 0.7;

        ball.vx = dirX * power * (0.6 + Math.abs(angleFactor)*0.9) + angleRandom*8;
        ball.vy += angleFactor * -6.5 + angleRandom*3.2;

        // block attempt (very simple)
        boolean tryingBlock = (hitter.team == 1) ? keyBlockA : keyBlockB;
        if (tryingBlock && hitter.onGround) {
            ball.vy -= 4.8;
            ball.vx *= 0.55;
        }

        // play sound effect simulation (console)
        if (isSpikeAttempt) {
            System.out.println("WHAM! SPIKE attempt!");
        }
    }

    // ===============================================================
    //   SCORING & MATCH LOGIC
    // ===============================================================

    private void givePoint(int winningTeam) {
        pointInProgress = false;

        if (winningTeam == 1) {
            scoreA++;
            servingTeamIsA = true;
            lastWinner = "Team A";
        } else {
            scoreB++;
            servingTeamIsA = false;
            lastWinner = "Team B";
        }

        String reason = "";
        if (!ball.inPlay) {
            if (ball.y >= GROUND_Y - BALL_RADIUS - 2) {
                reason = "ball hit the ground";
            } else if (ball.touchesTeamA > 3 || ball.touchesTeamB > 3) {
                reason = "more than 3 touches";
            } else if (ball.x < -40 || ball.x > WINDOW_WIDTH+40) {
                reason = "out of bounds";
            } else {
                reason = "unknown";
            }
        }

        message = lastWinner + " wins the rally! (" + reason + ")";

        checkSetEnd();
    }

    private void checkSetEnd() {
        if (scoreA >= winningSetPoints && scoreA >= scoreB + 2) {
            message = "Team A wins set " + setNumber + "!";
            resetSet(1);
        }
        else if (scoreB >= winningSetPoints && scoreB >= scoreA + 2) {
            message = "Team B wins set " + setNumber + "!";
            resetSet(2);
        }
    }

    private void resetSet(int winnerOfPrevious) {
        scoreA = scoreB = 0;
        setNumber++;

        if (setNumber > 5) {
            gameOver = true;
            message = (scoreA > scoreB) ? "Team A WINS THE MATCH!" : "Team B WINS THE MATCH!";
            timer.stop();
            return;
        }

        // tiebreak rules simplified
        if (setNumber == 5) {
            winningSetPoints = 15;
        }

        servingTeamIsA = (winnerOfPrevious == 1);
        resetBallForServe();
        repositionPlayersForServe();
    }

    // ===============================================================
    //   SIMPLE AI
    // ===============================================================

    private void updateAI() {
        updateAIForTeam(teamA, true);
        updateAIForTeam(teamB, false);
    }

    private void updateAIForTeam(Player[] team, boolean isLeftSide) {
        double targetZone = isLeftSide ? 380 : WINDOW_WIDTH - 380;

        for (Player p : team) {
            if (p.isAIControlled) {
                // very basic AI – move toward ball projection
                double projectedX = predictBallLandingX();

                // setter tries to go under ball
                if (p.jerseyNumber == 1) {
                    p.targetX = projectedX + (isLeftSide ? -40 : 40);
                }
                // attackers move forward when ball is high
                else if (p.jerseyNumber == 2 || p.jerseyNumber == 5) {
                    if (ball.vy < -4 && ball.y < 280) {
                        p.targetX = projectedX;
                    }
                }
                // middles / blockers stay near net
                else {
                    p.targetX = NET_X + (isLeftSide ? -80 : 80) + (random.nextDouble()-0.5)*120;
                }

                // move towards target
                double dx = p.targetX - p.x;
                if (Math.abs(dx) > 12) {
                    if (dx > 0) {
                        if (isLeftSide) keyRightA = true; else keyRightB = true;
                    } else {
                        if (isLeftSide) keyLeftA = true; else keyLeftB = true;
                    }

                    // jump when ball is coming and player is close
                    if (Math.abs(p.x - ball.x) < 90 && ball.y < 320 && ball.vy > 0) {
                        if (isLeftSide) keyJumpA = true; else keyJumpB = true;
                    }
                }
            }
        }
    }

    private double predictBallLandingX() {
        if (Math.abs(ball.vy) < 0.1) return ball.x;

        double timeToGround = (-ball.vy + Math.sqrt(ball.vy*ball.vy + 2*GRAVITY*(GROUND_Y - ball.y))) / GRAVITY;
        return ball.x + ball.vx * timeToGround;
    }

    // ===============================================================
    //   INPUT HANDLING
    // ===============================================================

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // Team A (left) - WASD + QERF
        if (code == KeyEvent.VK_A) keyLeftA  = true;
        if (code == KeyEvent.VK_D) keyRightA = true;
        if (code == KeyEvent.VK_W) keyJumpA  = true;
        if (code == KeyEvent.VK_SPACE) {
            if (!pointInProgress) {
                performServe();
            } else {
                keySpikeA = true;
            }
        }
        if (code == KeyEvent.VK_F) keyBlockA = true;

        // Team B (right) - arrows + numpad
        if (code == KeyEvent.VK_LEFT)  keyLeftB  = true;
        if (code == KeyEvent.VK_RIGHT) keyRightB = true;
        if (code == KeyEvent.VK_UP)    keyJumpB  = true;
        if (code == KeyEvent.VK_ENTER) {
            if (!pointInProgress) {
                performServe();
            } else {
                keySpikeB = true;
            }
        }
        if (code == KeyEvent.VK_DOWN) keyBlockB = true;

        // reset / debug
        if (code == KeyEvent.VK_R) resetBallForServe();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_A) keyLeftA  = false;
        if (code == KeyEvent.VK_D) keyRightA = false;
        if (code == KeyEvent.VK_W) keyJumpA  = false;
        if (code == KeyEvent.VK_SPACE) keySpikeA = false;
        if (code == KeyEvent.VK_F) keyBlockA = false;

        if (code == KeyEvent.VK_LEFT)  keyLeftB  = false;
        if (code == KeyEvent.VK_RIGHT) keyRightB = false;
        if (code == KeyEvent.VK_UP)    keyJumpB  = false;
        if (code == KeyEvent.VK_ENTER) keySpikeB = false;
        if (code == KeyEvent.VK_DOWN) keyBlockB = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void performServe() {
        if (pointInProgress) return;

        pointInProgress = true;
        message = "RALLY!";

        Player server = servingTeamIsA ? teamA[0] : teamB[0];

        ball.x = server.x + (servingTeamIsA ? 40 : -40);
        ball.y = server.y - 140;

        double serveDir = servingTeamIsA ? 1.0 : -1.0;
        double power = SERVE_POWER + random.nextDouble() * 5.0;

        ball.vx = serveDir * power;
        ball.vy = -9.5 - random.nextDouble() * 5.0;
        ball.lastHitByTeam = servingTeamIsA ? 1 : 2;
    }

    // ===============================================================
    //   RENDERING
    // ===============================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawCourt(g2);
        drawNet(g2);
        drawPlayers(g2);
        drawBall(g2);
        drawTrail(g2);
        drawUI(g2);
    }

    private void drawCourt(Graphics2D g) {
        g.setColor(COLOR_COURT);
        g.fillRect(0, GROUND_Y, WINDOW_WIDTH, WINDOW_HEIGHT - GROUND_Y);

        g.setColor(COLOR_LINES);
        g.setStroke(new BasicStroke(5f));

        // sidelines
        g.drawLine(COURT_MARGIN, GROUND_Y, WINDOW_WIDTH-COURT_MARGIN, GROUND_Y);
        g.drawLine(COURT_MARGIN, GROUND_Y-400, COURT_MARGIN, GROUND_Y);
        g.drawLine(WINDOW_WIDTH-COURT_MARGIN, GROUND_Y-400, WINDOW_WIDTH-COURT_MARGIN, GROUND_Y);

        // centerline
        g.drawLine(NET_X, GROUND_Y, NET_X, GROUND_Y-400);

        // attack lines
        int attackLineOffset = 300;
        g.drawLine(NET_X - attackLineOffset, GROUND_Y, NET_X - attackLineOffset, GROUND_Y-180);
        g.drawLine(NET_X + attackLineOffset, GROUND_Y, NET_X + attackLineOffset, GROUND_Y-180);
    }

    private void drawNet(Graphics2D g) {
        g.setColor(COLOR_NET);
        g.fillRect(NET_X - NET_WIDTH/2, GROUND_Y - NET_HEIGHT, NET_WIDTH, NET_HEIGHT);

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        for (int y = GROUND_Y - NET_HEIGHT; y < GROUND_Y; y += 18) {
            g.drawLine(NET_X - NET_WIDTH/2, y, NET_X + NET_WIDTH/2, y);
        }
    }

    private void drawPlayers(Graphics2D g) {
        for (Player p : teamA) drawPlayer(g, p, COLOR_PLAYER_A);
        for (Player p : teamB) drawPlayer(g, p, COLOR_PLAYER_B);
    }

    private void drawPlayer(Graphics2D g, Player p, Color teamColor) {
        // shadow
        g.setColor(COLOR_SHADOW);
        g.fillOval((int)(p.x-28), GROUND_Y+6, 56, 18);

        // body
        g.setColor(teamColor);
        g.fillRoundRect((int)(p.x-22), (int)(p.y-80), 44, 80, 20, 20);

        // head
        g.setColor(new Color(255,220,180));
        g.fillOval((int)(p.x-16), (int)(p.y-110), 32, 32);

        // jersey number
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("" + p.jerseyNumber, (int)p.x-8, (int)p.y-85);

        // simple arm / leg animation
        if (p.currentAction.equals("run")) {
            int legPhase = p.frame / 6 % 4;
            g.setColor(teamColor.darker());
            // legs
            if (legPhase == 0 || legPhase == 2) {
                g.fillRect((int)p.x-14, (int)p.y-30, 10, 35);
                g.fillRect((int)p.x+4,  (int)p.y-20, 10, 30);
            } else {
                g.fillRect((int)p.x-14, (int)p.y-20, 10, 30);
                g.fillRect((int)p.x+4,  (int)p.y-30, 10, 35);
            }
        }

        // name above head when close
        if (Math.abs(p.x - (NET_X + (p.team==1?-200:200))) < 320) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.drawString(p.name, (int)p.x - 30, (int)p.y - 125);
        }
    }

    private void drawBall(Graphics2D g) {
        if (!ball.inPlay) return;

        // trail already drawn separately

        // ball shadow
        int shadowX = (int)(ball.x - BALL_RADIUS*1.4);
        int shadowY = GROUND_Y + 8;
        g.setColor(COLOR_SHADOW);
        g.fillOval(shadowX, shadowY, (int)(BALL_RADIUS*2.8), 16);

        // ball
        g.setColor(COLOR_BALL);
        g.fillOval((int)(ball.x - BALL_RADIUS), (int)(ball.y - BALL_RADIUS), (int)(BALL_RADIUS*2), (int)(BALL_RADIUS*2));

        g.setColor(new Color(240, 220, 80));
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval((int)(ball.x - BALL_RADIUS), (int)(ball.y - BALL_RADIUS), (int)(BALL_RADIUS*2), (int)(BALL_RADIUS*2));
    }

    private void updateTrail() {
        if (pointInProgress) {
            ballTrail.add(new Point2D.Double(ball.x, ball.y));
            if (ballTrail.size() > MAX_TRAIL) {
                ballTrail.remove(0);
            }
        } else {
            ballTrail.clear();
        }
    }

    private void drawTrail(Graphics2D g) {
        if (ballTrail.isEmpty()) return;

        for (int i = 0; i < ballTrail.size(); i++) {
            Point2D.Double pt = ballTrail.get(i);
            double alpha = (i + 1.0) / ballTrail.size();
            int size = (int)(BALL_RADIUS * 1.6 * alpha);

            g.setColor(new Color(255, 240, 140, (int)(80 * alpha)));
            g.fillOval((int)(pt.x - size/2), (int)(pt.y - size/2), size, size);
        }
    }

    private void drawUI(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString(scoreA + " : " + scoreB, WINDOW_WIDTH/2 - 60, 60);

        g.setFont(new Font("Arial", Font.PLAIN, 22));
        g.drawString("Set " + setNumber, WINDOW_WIDTH/2 - 40, 100);

        g.setFont(new Font("Arial", Font.ITALIC, 18));
        g.drawString(message, WINDOW_WIDTH/2 - message.length()*5, 140);

        // controls help
        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.drawString("Team A:  A/D move   W jump   SPACE hit/spike   F block", 20, WINDOW_HEIGHT-40);
        g.drawString("Team B: ←/→ move   ↑ jump   ENTER hit/spike   ↓ block", 20, WINDOW_HEIGHT-20);
    }

    private void updateCamera() {
        // very simple smooth follow when ball is far
        double idealCam = ball.x - WINDOW_WIDTH/2;
        cameraX += (idealCam - cameraX) * 0.08;
        cameraX = Math.max(-200, Math.min(WINDOW_WIDTH-1000, cameraX));
    }

    // ===============================================================
    //   MAIN METHOD
    // ===============================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Volleyball Game Simulation 2D");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new VolleyballGameSimulation());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}