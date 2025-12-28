import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class RugbyGameSimulation extends JPanel implements ActionListener, KeyListener {

    // ----------- Core constants -----------
    public static final int FIELD_WIDTH = 1000;
    public static final int FIELD_HEIGHT = 600;

    public static final int WINDOW_WIDTH = 1100;
    public static final int WINDOW_HEIGHT = 700;

    private static final int TICKS_PER_SECOND = 60;
    private static final int TIMER_DELAY_MS = 1000 / TICKS_PER_SECOND;

    private static final int MATCH_LENGTH_SECONDS = 3 * 60; // 3-minute demo

    private static final Color FIELD_COLOR = new Color(34, 139, 34);
    private static final Color LINE_COLOR = new Color(250, 250, 245);

    private static final int PLAYER_RADIUS = 15;
    private static final int PLAYER_MAX_SPEED = 4;
    private static final int PLAYER_ACCEL = 1;
    private static final double FRICTION = 0.80;

    private static final int TEAM_SIZE = 7; // Rugby sevens feel

    private static final int BALL_RADIUS = 10;
    private static final double BALL_FRICTION = 0.96;
    private static final double BALL_PASS_SPEED = 10.0;
    private static final double BALL_KICK_SPEED = 13.0;

    private static final int GOAL_AREA_HEIGHT = 70;

    private static final int HUD_HEIGHT = 80;

    private static final Font FONT_HUD_LARGE = new Font("SansSerif", Font.BOLD, 22);
    private static final Font FONT_HUD_SMALL = new Font("SansSerif", Font.PLAIN, 14);

    // ----------- Game state -----------
    private GameState state;
    private Timer timer;

    private boolean upPressed, downPressed, leftPressed, rightPressed;
    private boolean debugOverlay = false;

    private boolean passPressed;
    private boolean passHandledThisPress = false; // for edge detection

    private boolean paused = false;

    private Random rnd = new Random();

    public RugbyGameSimulation() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(this);

        state = new GameState();
        timer = new Timer(TIMER_DELAY_MS, this);
        timer.start();
    }

    // ----------- Main game loop tick -----------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!paused) {
            stepGame();
        }
        repaint();
    }

    private void stepGame() {
        double dt = 1.0 / TICKS_PER_SECOND;

        // update game clock
        if (!state.isGameOver()) {
            state.elapsedSeconds += dt;
        }

        // check for end of match
        if (state.elapsedSeconds >= MATCH_LENGTH_SECONDS) {
            state.elapsedSeconds = MATCH_LENGTH_SECONDS;
            state.gameOver = true;
        }

        // user input for controlled player
        handleUserMovement();

        // AI movement
        updateAI();

        // physics
        updatePlayers(dt);
        updateBall(dt);

        // collisions, possession and scoring
        handleBallPossession();
        handleScoring();

        // clear pass flag edge detection
        if (!passPressed) {
            passHandledThisPress = false;
        }
    }

    // ----------- User input and movement -----------
    private void handleUserMovement() {
        Player p = state.getControlledPlayer();
        if (p == null) return;

        int ax = 0;
        int ay = 0;

        if (upPressed) ay -= PLAYER_ACCEL;
        if (downPressed) ay += PLAYER_ACCEL;
        if (leftPressed) ax -= PLAYER_ACCEL;
        if (rightPressed) ax += PLAYER_ACCEL;

        p.vx += ax;
        p.vy += ay;

        // clamp speed
        double speed = Math.sqrt(p.vx * p.vx + p.vy * p.vy);
        if (speed > PLAYER_MAX_SPEED) {
            double scale = PLAYER_MAX_SPEED / speed;
            p.vx *= scale;
            p.vy *= scale;
        }

        // pass action (space)
        if (passPressed && !passHandledThisPress) {
            attemptPass(p);
            passHandledThisPress = true;
        }
    }

    private void attemptPass(Player passer) {
        if (!state.ball.isHeld() || state.ball.holder != passer) {
            // only allow pass if player holds ball
            return;
        }

        Player target = findBestPassTarget(passer);
        if (target == null) {
            // small chance of "random" kick forward if no target
            kickForward(passer);
            return;
        }

        // compute direction from passer to target
        double dx = target.x - passer.x;
        double dy = target.y - passer.y;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-3) return;

        double vx = (dx / len) * BALL_PASS_SPEED;
        double vy = (dy / len) * BALL_PASS_SPEED;

        // release ball
        state.ball.holder = null;
        state.ball.vx = vx;
        state.ball.vy = vy;
        state.ball.x = passer.x;
        state.ball.y = passer.y;
        state.ball.lastTeamWithPossession = passer.team;
    }

    private void kickForward(Player kicker) {
        // simple forward kick depending on team side
        double dir = kicker.team == TeamSide.LEFT ? 1.0 : -1.0;
        state.ball.holder = null;
        state.ball.x = kicker.x;
        state.ball.y = kicker.y;
        state.ball.vx = dir * BALL_KICK_SPEED;
        state.ball.vy = (rnd.nextDouble() - 0.5) * BALL_KICK_SPEED * 0.5;
        state.ball.lastTeamWithPossession = kicker.team;
    }

    private Player findBestPassTarget(Player passer) {
        List<Player> team = passer.team == TeamSide.LEFT ? state.leftTeam : state.rightTeam;
        Player best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Player candidate : team) {
            if (candidate == passer) continue;

            double dx = candidate.x - passer.x;
            // enforce "forward pass" relative to passer's orientation (rugby: no forward passes)
            // so this is slightly unrealistic: we allow sideways and backward only
            if (passer.team == TeamSide.LEFT && dx > 5) continue;
            if (passer.team == TeamSide.RIGHT && dx < -5) continue;

            double dy = candidate.y - passer.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 30 || dist > 300) continue; // ignore too close or too far

            // simple scoring: prefer closer and slightly forward/backwards
            double score = -dist;
            if (passer.team == TeamSide.LEFT) {
                score += dx * 0.1;
            } else {
                score -= dx * 0.1;
            }

            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private void updateAI() {
        // Right team AI is fully automatic; left has only AI on non-controlled players.
        updateTeamAI(state.leftTeam, TeamSide.LEFT, true);
        updateTeamAI(state.rightTeam, TeamSide.RIGHT, false);
    }

    private void updateTeamAI(List<Player> team, TeamSide side, boolean userTeam) {
        for (Player p : team) {
            if (userTeam && p == state.getControlledPlayer()) {
                // user-controlled: skip AI for movement
                continue;
            }

            // base tactic: player that holds ball tries to run towards opponent in-goal
            if (state.ball.isHeld() && state.ball.holder == p) {
                runWithBallAI(p, side);
            } else {
                // support running & defensive AI
                supportOrDefendAI(p, side);
            }
        }
    }

    private void runWithBallAI(Player p, TeamSide side) {
        double targetX;
        if (side == TeamSide.LEFT) {
            targetX = FIELD_WIDTH - 40;
        } else {
            targetX = 40;
        }

        double targetY = FIELD_HEIGHT / 2.0;

        steerTowards(p, targetX, targetY, 0.6);
    }

    private void supportOrDefendAI(Player p, TeamSide side) {
        double tx, ty;

        // defense: move toward ball if far away
        double distToBall = distance(p.x, p.y, state.ball.x, state.ball.y);

        if (distToBall > 200) {
            tx = state.ball.x;
            ty = state.ball.y;
        } else {
            // maintain lateral spread
            int idx = p.indexInTeam;
            double spacing = FIELD_HEIGHT / (TEAM_SIZE + 1);
            ty = spacing * (idx + 1);
            // offset horizontally around ball
            double offsetX = (idx - TEAM_SIZE / 2) * 30;
            tx = state.ball.x + offsetX * (side == TeamSide.LEFT ? -0.2 : 0.2);
        }

        steerTowards(p, tx, ty, 0.5);
    }

    private void steerTowards(Player p, double tx, double ty, double intensity) {
        double dx = tx - p.x;
        double dy = ty - p.y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 1e-3) return;

        dx /= dist;
        dy /= dist;

        p.vx += dx * PLAYER_ACCEL * intensity;
        p.vy += dy * PLAYER_ACCEL * intensity;

        double speed = Math.sqrt(p.vx * p.vx + p.vy * p.vy);
        if (speed > PLAYER_MAX_SPEED) {
            double scale = PLAYER_MAX_SPEED / speed;
            p.vx *= scale;
            p.vy *= scale;
        }
    }

    // ----------- Physics updates -----------
    private void updatePlayers(double dt) {
        for (Player p : state.leftTeam) {
            integratePlayer(p);
        }
        for (Player p : state.rightTeam) {
            integratePlayer(p);
        }
    }

    private void integratePlayer(Player p) {
        p.x += p.vx;
        p.y += p.vy;

        // friction
        p.vx *= FRICTION;
        p.vy *= FRICTION;

        // clamp inside field
        if (p.x < PLAYER_RADIUS) {
            p.x = PLAYER_RADIUS;
            p.vx = 0;
        }
        if (p.x > FIELD_WIDTH - PLAYER_RADIUS) {
            p.x = FIELD_WIDTH - PLAYER_RADIUS;
            p.vx = 0;
        }
        if (p.y < PLAYER_RADIUS) {
            p.y = PLAYER_RADIUS;
            p.vy = 0;
        }
        if (p.y > FIELD_HEIGHT - PLAYER_RADIUS) {
            p.y = FIELD_HEIGHT - PLAYER_RADIUS;
            p.vy = 0;
        }
    }

    private void updateBall(double dt) {
        Ball b = state.ball;

        if (b.isHeld()) {
            Player holder = b.holder;
            b.x = holder.x;
            b.y = holder.y;
        } else {
            b.x += b.vx;
            b.y += b.vy;

            b.vx *= BALL_FRICTION;
            b.vy *= BALL_FRICTION;

            // collisions with field boundaries
            if (b.x < BALL_RADIUS) {
                b.x = BALL_RADIUS;
                b.vx = Math.abs(b.vx);
            }
            if (b.x > FIELD_WIDTH - BALL_RADIUS) {
                b.x = FIELD_WIDTH - BALL_RADIUS;
                b.vx = -Math.abs(b.vx);
            }

            if (b.y < BALL_RADIUS) {
                b.y = BALL_RADIUS;
                b.vy = Math.abs(b.vy);
            }
            if (b.y > FIELD_HEIGHT - BALL_RADIUS) {
                b.y = FIELD_HEIGHT - BALL_RADIUS;
                b.vy = -Math.abs(b.vy);
            }
        }
    }

    // ----------- Possession, collisions and scoring -----------
    private void handleBallPossession() {
        Ball b = state.ball;
        if (b.isHeld()) return;

        // players can pick up ball if close and ball is slow
        double speed = Math.sqrt(b.vx * b.vx + b.vy * b.vy);

        for (Player p : state.allPlayers()) {
            double dist = distance(p.x, p.y, b.x, b.y);
            if (dist <= PLAYER_RADIUS + BALL_RADIUS + 4) {
                if (speed < 2.5) {
                    // possession change
                    b.holder = p;
                    b.vx = 0;
                    b.vy = 0;
                    b.lastTeamWithPossession = p.team;
                    state.possession = p.team;
                    return;
                }
            }
        }
    }

    private void handleScoring() {
        Ball b = state.ball;

        // determine if ball (or holder) entered in-goal area
        double x = b.x;
        if (b.isHeld()) {
            x = b.holder.x;
        }

        if (x <= 5) {
            // crossed left edge
            if (b.lastTeamWithPossession == TeamSide.RIGHT) {
                // right team scored try against left
                state.scoreRight += 5;
                resetAfterScore(TeamSide.LEFT);
            } else if (b.lastTeamWithPossession == TeamSide.LEFT) {
                // left team "touch down" in own goal: optional penalty to right
                state.scoreRight += 3;
                resetAfterScore(TeamSide.LEFT);
            }
        } else if (x >= FIELD_WIDTH - 5) {
            // crossed right edge
            if (b.lastTeamWithPossession == TeamSide.LEFT) {
                state.scoreLeft += 5;
                resetAfterScore(TeamSide.RIGHT);
            } else if (b.lastTeamWithPossession == TeamSide.RIGHT) {
                state.scoreLeft += 3;
                resetAfterScore(TeamSide.RIGHT);
            }
        }
    }

    private void resetAfterScore(TeamSide teamToKickOff) {
        // re-center players roughly, ball at center
        arrangeTeamsAtKickoff(teamToKickOff);
        state.ball.holder = null;
        state.ball.x = FIELD_WIDTH / 2.0;
        state.ball.y = FIELD_HEIGHT / 2.0;
        state.ball.vx = 0;
        state.ball.vy = 0;
        state.possession = null;
    }

    // ----------- Rendering -----------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // use Graphics2D for better rendering
        Graphics2D g2d = (Graphics2D) g.create();

        // smooth edges
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawField(g2d);
        drawPlayers(g2d);
        drawBall(g2d);
        drawHUD(g2d);

        if (debugOverlay) {
            drawDebugInfo(g2d);
        }

        if (paused) {
            drawPauseOverlay(g2d);
        }

        if (state.isGameOver()) {
            drawGameOverOverlay(g2d);
        }

        g2d.dispose();
    }

    private void drawField(Graphics2D g2d) {
        int fieldX = (WINDOW_WIDTH - FIELD_WIDTH) / 2;
        int fieldY = (WINDOW_HEIGHT - FIELD_HEIGHT - HUD_HEIGHT) / 2 + HUD_HEIGHT;

        // transform world coordinates to screen coordinates via translation
        g2d.translate(fieldX, fieldY);

        // field background
        g2d.setColor(FIELD_COLOR);
        g2d.fillRect(0, 0, FIELD_WIDTH, FIELD_HEIGHT);

        // horizontal stripes for effect
        g2d.setColor(new Color(0, 120, 0, 80));
        int stripeHeight = 40;
        for (int y = 0; y < FIELD_HEIGHT; y += stripeHeight * 2) {
            g2d.fillRect(0, y, FIELD_WIDTH, stripeHeight);
        }

        // boundary lines
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(LINE_COLOR);
        g2d.drawRect(0, 0, FIELD_WIDTH, FIELD_HEIGHT);

        // halfway line
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 10}, 0));
        g2d.drawLine(FIELD_WIDTH / 2, 0, FIELD_WIDTH / 2, FIELD_HEIGHT);

        // goal (try) lines
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(0, 0, 0, FIELD_HEIGHT);
        g2d.drawLine(FIELD_WIDTH - 1, 0, FIELD_WIDTH - 1, FIELD_HEIGHT);

        // in-goal shading
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.fillRect(0, 0, 25, FIELD_HEIGHT);
        g2d.fillRect(FIELD_WIDTH - 25, 0, 25, FIELD_HEIGHT);

        // 22-meter lines (just for style, proportionally)
        g2d.setColor(new Color(230, 230, 230, 160));
        int line22 = (int) (FIELD_WIDTH * 0.22);
        g2d.drawLine(line22, 0, line22, FIELD_HEIGHT);
        g2d.drawLine(FIELD_WIDTH - line22, 0, FIELD_WIDTH - line22, FIELD_HEIGHT);

        // restore translation for all subsequent world->screen transforms
        g2d.translate(-fieldX, -fieldY);
    }

    private Point worldToScreen(double wx, double wy) {
        int fieldX = (WINDOW_WIDTH - FIELD_WIDTH) / 2;
        int fieldY = (WINDOW_HEIGHT - FIELD_HEIGHT - HUD_HEIGHT) / 2 + HUD_HEIGHT;
        int sx = (int) Math.round(fieldX + wx);
        int sy = (int) Math.round(fieldY + wy);
        return new Point(sx, sy);
    }

    private void drawPlayers(Graphics2D g2d) {
        for (Player p : state.leftTeam) {
            drawPlayer(g2d, p);
        }
        for (Player p : state.rightTeam) {
            drawPlayer(g2d, p);
        }
    }

    private void drawPlayer(Graphics2D g2d, Player p) {
        Point s = worldToScreen(p.x, p.y);

        // circle for body
        int r = PLAYER_RADIUS;
        int x = s.x - r;
        int y = s.y - r;

        // base color per team
        Color base = p.team == TeamSide.LEFT ? new Color(0, 102, 204) : new Color(220, 50, 50);
        g2d.setColor(base);
        g2d.fill(new Ellipse2D.Double(x, y, r * 2, r * 2));

        // outline
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(new Ellipse2D.Double(x, y, r * 2, r * 2));

        // highlight controlled player with thicker outline
        if (p == state.getControlledPlayer()) {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(3));
            g2d.draw(new Ellipse2D.Double(x - 2, y - 2, r * 2 + 4, r * 2 + 4));
        }

        // jersey number
        g2d.setFont(FONT_HUD_SMALL);
        g2d.setColor(Color.WHITE);
        String label = Integer.toString(p.indexInTeam + 1);
        FontMetrics fm = g2d.getFontMetrics();
        int w = fm.stringWidth(label);
        int h = fm.getAscent();
        g2d.drawString(label, s.x - w / 2, s.y + h / 2);

        // optional: small direction indicator
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.BLACK);
        double speed = Math.sqrt(p.vx * p.vx + p.vy * p.vy);
        if (speed > 0.2) {
            double nx = p.vx / speed;
            double ny = p.vy / speed;
            int ex = (int) (s.x + nx * (r + 6));
            int ey = (int) (s.y + ny * (r + 6));
            g2d.drawLine(s.x, s.y, ex, ey);
        }
    }

    private void drawBall(Graphics2D g2d) {
        Ball b = state.ball;
        Point s = worldToScreen(b.x, b.y);
        int r = BALL_RADIUS;
        int x = s.x - r;
        int y = s.y - r;

        // rugby ball shape: small rotated ellipse approximation
        g2d.setColor(new Color(245, 245, 220));
        Shape oval = new Ellipse2D.Double(x - 4, y, r * 2 + 8, r * 2 * 0.8);
        g2d.fill(oval);
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(oval);

        // small seam
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(s.x - r, s.y, s.x + r, s.y);
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WINDOW_WIDTH, HUD_HEIGHT);

        // scores
        g2d.setFont(FONT_HUD_LARGE);
        g2d.setColor(Color.WHITE);
        String leftScore = "LEFT: " + state.scoreLeft;
        String rightScore = "RIGHT: " + state.scoreRight;

        FontMetrics fmLarge = g2d.getFontMetrics();
        int leftWidth = fmLarge.stringWidth(leftScore);
        int rightWidth = fmLarge.stringWidth(rightScore);

        int margin = 30;
        g2d.drawString(leftScore, margin, HUD_HEIGHT / 2 + fmLarge.getAscent() / 2 - 8);
        g2d.drawString(rightScore, WINDOW_WIDTH - margin - rightWidth, HUD_HEIGHT / 2 + fmLarge.getAscent() / 2 - 8);

        // middle info (time, possession, control)
        g2d.setFont(FONT_HUD_SMALL);
        g2d.setColor(Color.LIGHT_GRAY);

        String timeStr = formatTime((int) Math.round(MATCH_LENGTH_SECONDS - state.elapsedSeconds));
        String possessionStr = "Possession: ";
        if (state.possession == TeamSide.LEFT) {
            possessionStr += "LEFT";
        } else if (state.possession == TeamSide.RIGHT) {
            possessionStr += "RIGHT";
        } else {
            possessionStr += "None";
        }

        String controlStr = "Control: " +
                (state.getControlledPlayer() != null
                        ? (state.getControlledPlayer().team == TeamSide.LEFT ? "LEFT #" : "RIGHT #")
                        + (state.getControlledPlayer().indexInTeam + 1)
                        : "None");

        String hintStr = "Controls: Arrows move, A/D switch, SPACE pass, P pause, R reset, F1 debug";

        int centerX = WINDOW_WIDTH / 2;
        int y1 = 22;
        int y2 = 42;
        int y3 = 60;

        drawCenteredString(g2d, "Time left: " + timeStr, centerX, y1);
        drawCenteredString(g2d, possessionStr + " | " + controlStr, centerX, y2);
        drawCenteredString(g2d, hintStr, centerX, y3);
    }

    private void drawCenteredString(Graphics2D g2d, String text, int cx, int cy) {
        FontMetrics fm = g2d.getFontMetrics();
        int w = fm.stringWidth(text);
        int h = fm.getAscent();
        g2d.drawString(text, cx - w / 2, cy + h / 2 - 4);
    }

    private String formatTime(int sec) {
        if (sec < 0) sec = 0;
        int m = sec / 60;
        int s = sec % 60;
        return String.format("%d:%02d", m, s);
    }

    private void drawPauseOverlay(Graphics2D g2d) {
        Composite old = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        g2d.setComposite(old);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 40));
        String text = "PAUSED";
        drawCenteredString(g2d, text, WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2);

        g2d.setFont(FONT_HUD_SMALL);
        drawCenteredString(g2d, "Press P to resume", WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2 + 40);
    }

    private void drawGameOverOverlay(Graphics2D g2d) {
        Composite old = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.60f));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        g2d.setComposite(old);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 40));
        g2d.setColor(Color.WHITE);

        String text = "FULL TIME";
        drawCenteredString(g2d, text, WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2 - 40);

        String result;
        if (state.scoreLeft > state.scoreRight) {
            result = "Left team wins!";
        } else if (state.scoreRight > state.scoreLeft) {
            result = "Right team wins!";
        } else {
            result = "Draw.";
        }

        g2d.setFont(new Font("SansSerif", Font.BOLD, 26));
        drawCenteredString(g2d, result, WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2);

        g2d.setFont(FONT_HUD_SMALL);
        drawCenteredString(g2d, "Press R to restart match", WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2 + 40);
    }

    private void drawDebugInfo(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 230));
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));

        int x = 10;
        int y = WINDOW_HEIGHT - 10;
        int line = 14;

        // debug: FPS approximate (just timer-based)
        String s1 = String.format("Tick dt=%.3f s | Players=%d | Ball=(%.1f, %.1f) v=(%.2f, %.2f)",
                1.0 / TICKS_PER_SECOND,
                state.allPlayers().size(),
                state.ball.x, state.ball.y,
                state.ball.vx, state.ball.vy);

        String s2 = String.format("Left score=%d Right score=%d Possession=%s Holder=%s",
                state.scoreLeft, state.scoreRight,
                state.possession,
                state.ball.holder != null ? state.ball.holder.team + "#" + (state.ball.holder.indexInTeam + 1) : "None");

        g2d.drawString(s1, x, y - line * 2);
        g2d.drawString(s2, x, y - line);

        // draw bounding boxes around players and ball
        g2d.setColor(new Color(255, 255, 0, 150));
        for (Player p : state.allPlayers()) {
            Point s = worldToScreen(p.x, p.y);
            int r = PLAYER_RADIUS;
            Shape rect = new Rectangle2D.Double(s.x - r, s.y - r, r * 2, r * 2);
            g2d.draw(rect);
        }

        Point b = worldToScreen(state.ball.x, state.ball.y);
        int br = BALL_RADIUS;
        Shape rect = new Rectangle2D.Double(b.x - br, b.y - br, br * 2, br * 2);
        g2d.setColor(new Color(0, 255, 255, 150));
        g2d.draw(rect);
    }

    // ----------- Keyboard handling -----------
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                upPressed = true;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                downPressed = true;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                // 'A' also used for switching, but arrow left is movement
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    leftPressed = true;
                }
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    rightPressed = true;
                }
                break;
            case KeyEvent.VK_SPACE:
                passPressed = true;
                break;
            case KeyEvent.VK_P:
                paused = !paused;
                break;
            case KeyEvent.VK_R:
                resetMatch();
                break;
            case KeyEvent.VK_F1:
                debugOverlay = !debugOverlay;
                break;
            default:
                // letter A / D to switch players when not used as movement
                if (e.getKeyCode() == KeyEvent.VK_A) {
                    cycleControlledPlayer(-1);
                } else if (e.getKeyCode() == KeyEvent.VK_D) {
                    cycleControlledPlayer(1);
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                upPressed = false;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                downPressed = false;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    leftPressed = false;
                }
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    rightPressed = false;
                }
                break;
            case KeyEvent.VK_SPACE:
                passPressed = false;
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // not used
    }

    private void cycleControlledPlayer(int delta) {
        if (state.leftTeam.isEmpty()) return;

        int idx = state.controlledPlayerIndex;
        idx = (idx + delta) % TEAM_SIZE;
        if (idx < 0) idx += TEAM_SIZE;
        state.controlledPlayerIndex = idx;
    }

    // ----------- Game state management -----------
    private void resetMatch() {
        state = new GameState();
        paused = false;
    }

    // ----------- Utility -----------
    private double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ----------- Inner enums and data classes -----------

    /** Team side. */
    private enum TeamSide {
        LEFT, RIGHT
    }

    /** Player entity. */
    private static class Player {
        double x, y;
        double vx, vy;
        TeamSide team;
        int indexInTeam; // jersey number index (0-based)

        Player(TeamSide team, int idx, double x, double y) {
            this.team = team;
            this.indexInTeam = idx;
            this.x = x;
            this.y = y;
        }
    }

    /** Ball entity. */
    private static class Ball {
        double x, y;
        double vx, vy;
        Player holder;
        TeamSide lastTeamWithPossession;

        boolean isHeld() {
            return holder != null;
        }
    }

    /** Global game state container. */
    private static class GameState {
        List<Player> leftTeam = new ArrayList<>();
        List<Player> rightTeam = new ArrayList<>();
        Ball ball = new Ball();

        int scoreLeft = 0;
        int scoreRight = 0;

        double elapsedSeconds = 0;
        boolean gameOver = false;

        TeamSide possession = null;

        int controlledPlayerIndex = 0;

        GameState() {
            initTeams();
        }

        private void initTeams() {
            // create players for each team
            double spacing = FIELD_HEIGHT / (TEAM_SIZE + 1);

            // left team initially on left half
            for (int i = 0; i < TEAM_SIZE; i++) {
                double x = FIELD_WIDTH * 0.25;
                double y = spacing * (i + 1);
                Player p = new Player(TeamSide.LEFT, i, x, y);
                leftTeam.add(p);
            }

            // right team on right half
            for (int i = 0; i < TEAM_SIZE; i++) {
                double x = FIELD_WIDTH * 0.75;
                double y = spacing * (i + 1);
                Player p = new Player(TeamSide.RIGHT, i, x, y);
                rightTeam.add(p);
            }

            // ball at center, possessed by left #1 for kickoff
            ball.x = FIELD_WIDTH / 2.0;
            ball.y = FIELD_HEIGHT / 2.0;
            Player holder = leftTeam.get(0);
            ball.holder = holder;
            ball.lastTeamWithPossession = holder.team;
            possession = holder.team;
            ball.vx = 0;
            ball.vy = 0;
        }

        List<Player> allPlayers() {
            List<Player> all = new ArrayList<>(leftTeam.size() + rightTeam.size());
            all.addAll(leftTeam);
            all.addAll(rightTeam);
            return all;
        }

        Player getControlledPlayer() {
            if (controlledPlayerIndex < 0 || controlledPlayerIndex >= leftTeam.size()) return null;
            return leftTeam.get(controlledPlayerIndex);
        }

        boolean isGameOver() {
            return gameOver;
        }
    }

    // arrange teams for a new kickoff after score
    private void arrangeTeamsAtKickoff(TeamSide teamToKickOff) {
        double spacing = FIELD_HEIGHT / (TEAM_SIZE + 1);

        if (teamToKickOff == TeamSide.LEFT) {
            // left team at center line, right team spread
            for (int i = 0; i < TEAM_SIZE; i++) {
                Player p = state.leftTeam.get(i);
                p.x = FIELD_WIDTH * 0.3;
                p.y = spacing * (i + 1);
                p.vx = p.vy = 0;
            }
            for (int i = 0; i < TEAM_SIZE; i++) {
                Player p = state.rightTeam.get(i);
                p.x = FIELD_WIDTH * 0.7;
                p.y = spacing * (i + 1);
                p.vx = p.vy = 0;
            }
        } else {
            // right kickoff
            for (int i = 0; i < TEAM_SIZE; i++) {
                Player p = state.rightTeam.get(i);
                p.x = FIELD_WIDTH * 0.7;
                p.y = spacing * (i + 1);
                p.vx = p.vy = 0;
            }
            for (int i = 0; i < TEAM_SIZE; i++) {
                Player p = state.leftTeam.get(i);
                p.x = FIELD_WIDTH * 0.3;
                p.y = spacing * (i + 1);
                p.vx = p.vy = 0;
            }
        }

        state.controlledPlayerIndex = 0;
    }

    // ----------- Main entry point -----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Rugby Game Simulation");
            RugbyGameSimulation gamePanel = new RugbyGameSimulation();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(gamePanel);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
