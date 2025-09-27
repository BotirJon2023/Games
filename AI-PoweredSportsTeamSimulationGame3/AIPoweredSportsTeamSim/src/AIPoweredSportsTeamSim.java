import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class AIPoweredSportsTeamSim {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}

/**
 * Game window with panel and menu
 */
class GameFrame extends JFrame {
    public GameFrame() {
        setTitle("AI-Powered Sports Team Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GamePanel panel = new GamePanel();
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }
}

/**
 * Simple 2D vector utility class
 */
class Vec2 {
    public double x, y;
    public Vec2() { this(0,0); }
    public Vec2(double x, double y) { this.x=x; this.y=y; }
    public Vec2(Vec2 o) { this.x=o.x; this.y=o.y; }
    public Vec2 set(double x, double y) { this.x=x; this.y=y; return this; }
    public Vec2 set(Vec2 o) { this.x=o.x; this.y=o.y; return this; }
    public Vec2 add(Vec2 o) { this.x+=o.x; this.y+=o.y; return this; }
    public Vec2 sub(Vec2 o) { this.x-=o.x; this.y-=o.y; return this; }
    public Vec2 mul(double s) { this.x*=s; this.y*=s; return this; }
    public Vec2 div(double s) { this.x/=s; this.y/=s; return this; }
    public double dot(Vec2 o) { return x*o.x + y*o.y; }
    public double len() { return Math.sqrt(x*x + y*y); }
    public double len2() { return x*x + y*y; }
    public Vec2 norm() {
        double l=len();
        if (l>1e-9) { x/=l; y/=l; }
        return this;
    }
    public Vec2 limit(double max) {
        double l=len();
        if (l>max && l>1e-9) {
            x = x*(max/l);
            y = y*(max/l);
        }
        return this;
    }
    public static Vec2 add(Vec2 a, Vec2 b) { return new Vec2(a.x+b.x, a.y+b.y); }
    public static Vec2 sub(Vec2 a, Vec2 b) { return new Vec2(a.x-b.x, a.y-b.y); }
    public static Vec2 mul(Vec2 a, double s) { return new Vec2(a.x*s, a.y*s); }
    public static Vec2 fromAngle(double a) { return new Vec2(Math.cos(a), Math.sin(a)); }
    public String toString(){ return String.format("(%.2f, %.2f)", x, y); }
}

/**
 * Player roles
 */
enum Role { GK, DEF, MID, FWD }

/**
 * Tactics configuration
 */
class Tactics {
    public String name;
    public double lineHeight;      // [-1..1] vertical field position for back line (relative)
    public double pressIntensity;  // [0..1]
    public double passRisk;        // [0..1]
    public double tempo;           // [0.5..2.0] affects decision frequency and speed
    public double compactness;     // [0..1] horizontal compactness (narrow/wide)
    public Tactics(String name, double lineHeight, double pressIntensity, double passRisk, double tempo, double compactness) {
        this.name = name;
        this.lineHeight = lineHeight;
        this.pressIntensity = pressIntensity;
        this.passRisk = passRisk;
        this.tempo = tempo;
        this.compactness = compactness;
    }
    public static Tactics defensive() { return new Tactics("Defensive", -0.1, 0.4, 0.25, 0.9, 0.7); }
    public static Tactics balanced() { return new Tactics("Balanced", 0.0, 0.6, 0.45, 1.0, 0.5); }
    public static Tactics attacking() { return new Tactics("Attacking", 0.2, 0.7, 0.7, 1.1, 0.3); }
}

/**
 * Team structure
 */
class Team {
    public String name;
    public Color baseColor;
    public Color altColor;
    public List<Player> players = new ArrayList<>();
    public int score = 0;
    public int direction = 1; // 1 -> right, -1 -> left (attack direction)
    public Tactics tactics = Tactics.balanced();
    public Formation formation = Formation.F433();
    public AIAgent ai;
    public Random rnd;

    public Team(String name, Color primary, Color alt, int direction, long seed) {
        this.name = name;
        this.baseColor = primary;
        this.altColor = alt;
        this.direction = direction;
        this.ai = new AIAgent();
        this.rnd = new Random(seed);
    }

    public void resetScore() { score = 0; }

    public Player getGoalkeeper() {
        for (Player p : players) if (p.role == Role.GK) return p;
        return null;
    }

    public List<Player> outfieldPlayers() {
        List<Player> out = new ArrayList<>();
        for (Player p : players) if (p.role != Role.GK) out.add(p);
        return out;
    }

    public Color kitColor() {
        return baseColor;
    }

    // Get a base target position for a given player index in the formation.
    // Coordinates are in normalized field coords (-1..1 in x and y), then scaled and oriented.
    public Vec2 formationSpot(Player p) {
        Vec2 n = formation.getNormalizedSlot(p.slotIndex);
        // Compactness influences y spread:
        n.y *= (1.0 - tactics.compactness*0.6);
        // Line height shifts formation forward/backward
        n.x += tactics.lineHeight * 0.25;
        // Apply attack direction
        n.x *= direction;
        return n;
    }
}

/**
 * Formation definition (simple normalized slots)
 */
class Formation {
    public String name;
    public List<Vec2> slots = new ArrayList<>(); // normalized (-1..1) coordinates
    public List<Role> roles = new ArrayList<>();

    public Formation(String name) { this.name=name; }

    public static Formation F433() {
        Formation f = new Formation("4-3-3");
        // GK
        f.add(new Vec2(-0.95, 0.0), Role.GK);
        // DEF line (4)
        f.add(new Vec2(-0.65, -0.55), Role.DEF);
        f.add(new Vec2(-0.68, -0.2), Role.DEF);
        f.add(new Vec2(-0.68,  0.2), Role.DEF);
        f.add(new Vec2(-0.65,  0.55), Role.DEF);
        // MID (3)
        f.add(new Vec2(-0.3, -0.3), Role.MID);
        f.add(new Vec2(-0.28, 0.0), Role.MID);
        f.add(new Vec2(-0.3,  0.3), Role.MID);
        // FWD (3)
        f.add(new Vec2(0.15, -0.4), Role.FWD);
        f.add(new Vec2(0.2,  0.0), Role.FWD);
        f.add(new Vec2(0.15,  0.4), Role.FWD);
        return f;
    }

    public static Formation F442() {
        Formation f = new Formation("4-4-2");
        // GK
        f.add(new Vec2(-0.95, 0.0), Role.GK);
        // DEF (4)
        f.add(new Vec2(-0.68, -0.55), Role.DEF);
        f.add(new Vec2(-0.70, -0.2), Role.DEF);
        f.add(new Vec2(-0.70,  0.2), Role.DEF);
        f.add(new Vec2(-0.68,  0.55), Role.DEF);
        // MID (4)
        f.add(new Vec2(-0.35, -0.45), Role.MID);
        f.add(new Vec2(-0.35, -0.15), Role.MID);
        f.add(new Vec2(-0.35,  0.15), Role.MID);
        f.add(new Vec2(-0.35,  0.45), Role.MID);
        // FWD (2)
        f.add(new Vec2(0.05, -0.2), Role.FWD);
        f.add(new Vec2(0.05,  0.2), Role.FWD);
        return f;
    }

    public void add(Vec2 n, Role r) {
        slots.add(n);
        roles.add(r);
    }

    public Vec2 getNormalizedSlot(int idx) {
        if (idx<0 || idx>=slots.size()) return new Vec2(0,0);
        return slots.get(idx);
    }
    public Role getRole(int idx) {
        if (idx<0 || idx>=roles.size()) return Role.MID;
        return roles.get(idx);
    }
}

/**
 * Player object
 */
class Player {
    public int id;
    public int number;
    public String name;
    public Role role;
    public int slotIndex;
    public Team team;
    public Vec2 pos = new Vec2();
    public Vec2 vel = new Vec2();
    public Vec2 acc = new Vec2();
    public boolean hasBall = false;
    public double heading = 0; // radians
    public double radius = 0.42; // meters (approx)
    public double maxSpeed = 6.4; // m/s
    public double accel = 18.0;    // m/s^2 burst
    public double turnRate = 6.0; // rad/s
    public double controlSkill = 0.65; // 0..1 influences ball control touches
    public double passSkill = 0.65;
    public double shootSkill = 0.60;
    public double stamina = 1.0; // 0..1
    public double fatigueRate = 0.0008; // per second baseline
    public double recovery = 0.0002; // when idle
    public double decisionCooldown = 0.0; // time until next decision
    public double awareness = 0.6; // scanning skill
    public double aggression = 0.5; // pressing
    public double tacklePower = 0.5;
    public double reaction = 1.0; // influences cooldown
    public Color colorPrimary;
    public Color colorAlt;
    public boolean isSelectedVisual = false; // for highlight
    public double lastActionTime = 0;

    public Player(int id, int number, String name, Team team, Role role, int slotIndex, Color p, Color a) {
        this.id = id;
        this.number = number;
        this.name = name;
        this.team = team;
        this.role = role;
        this.slotIndex = slotIndex;
        this.colorPrimary = p;
        this.colorAlt = a;
        tuneByRole();
    }

    private void tuneByRole() {
        switch (role) {
            case GK:
                controlSkill = 0.55; passSkill = 0.55; shootSkill = 0.35;
                maxSpeed = 6.2; accel=15; tacklePower=0.35; awareness=0.75; reaction=0.9;
                break;
            case DEF:
                controlSkill = 0.65; passSkill = 0.62; shootSkill = 0.45;
                maxSpeed = 6.6; accel=18; tacklePower=0.70; aggression=0.65; awareness=0.7; reaction=1.0;
                break;
            case MID:
                controlSkill = 0.75; passSkill = 0.78; shootSkill = 0.55;
                maxSpeed = 6.8; accel=19; tacklePower=0.55; aggression=0.55; awareness=0.8; reaction=1.1;
                break;
            case FWD:
                controlSkill = 0.80; passSkill = 0.70; shootSkill = 0.80;
                maxSpeed = 7.2; accel=20; tacklePower=0.45; aggression=0.45; awareness=0.75; reaction=1.2;
                break;
        }
    }

    public void applyStamina(double dt, boolean sprinting) {
        double rate = fatigueRate*(sprinting ? 2.0 : 1.0);
        stamina = clamp(stamina - rate*dt + recovery*(1.0 - (sprinting ? 1.0 : 0.0))*dt, 0, 1);
        // Speed penalty with low stamina
        double fatiguePenalty = 0.5 + 0.5*stamina;
        maxSpeed = role==Role.FWD? 7.2 : (role==Role.MID?6.8:(role==Role.DEF?6.6:6.2));
        maxSpeed *= fatiguePenalty;
    }

    public static double clamp(double val, double lo, double hi){
        return Math.max(lo, Math.min(hi, val));
    }
}

/**
 * Ball object
 */
class Ball {
    public Vec2 pos = new Vec2();
    public Vec2 vel = new Vec2();
    public double radius = 0.22; // meters
    public int lastTouchTeam = -1;
    public int lastTouchPlayerId = -1;
    public double spin = 0.0; // not used extensively
    public double restitution = 0.3; // bounciness to walls (used if needed)
    public boolean inPlay = true;

    public void reset(Vec2 p) {
        pos.set(p);
        vel.set(0,0);
        spin = 0;
        lastTouchTeam = -1;
        lastTouchPlayerId = -1;
    }

    public void kick(Vec2 direction, double speed, Player by) {
        Vec2 dir = new Vec2(direction).norm();
        vel = dir.mul(speed);
        lastTouchTeam = by.team.direction > 0 ? 0 : 1; // not perfect; set by game controller as needed
        lastTouchPlayerId = by.id;
    }
}

/**
 * Commentary manager
 */
class Commentary {
    private Deque<String> lines = new ArrayDeque<>();
    private int maxLines = 10;
    public void add(String s) {
        if (lines.size() >= maxLines) lines.removeFirst();
        lines.addLast(s);
    }
    public List<String> get() {
        return new ArrayList<>(lines);
    }
    public void clear(){ lines.clear(); }
}

/**
 * AI agent that controls team behavior
 */
class AIAgent {

    /**
     * Update one team: decide movement targets and actions for players
     */
    public void updateAI(Team team, Team opp, Ball ball, Field field, double dt, double time, Commentary comms) {
        // Determine ball ownership probabilities and who is closest to ball
        Player closestToBallTeam = findClosest(team.players, ball.pos);
        Player closestToBallOpp = findClosest(opp.players, ball.pos);
        Player possessor = whoPossesses(ball, team, opp);

        // Tactical shape anchors
        for (Player p : team.players) {
            updatePlayerAI(p, team, opp, ball, possessor, closestToBallTeam, closestToBallOpp, field, dt, time, comms);
        }
    }

    private Player whoPossesses(Ball ball, Team a, Team b) {
        Player pa = findClosest(a.players, ball.pos);
        Player pb = findClosest(b.players, ball.pos);
        double da = pa==null?1e9:dist(pa.pos, ball.pos);
        double db = pb==null?1e9:dist(pb.pos, ball.pos);
        double controlR = 0.6; // m
        if (da < db && da < controlR) return pa;
        if (db < da && db < controlR) return pb;
        return null;
    }

    private double dist(Vec2 a, Vec2 b) {
        double dx=a.x-b.x, dy=a.y-b.y;
        return Math.sqrt(dx*dx+dy*dy);
    }

    private Player findClosest(List<Player> players, Vec2 pos) {
        Player best=null; double bestD=1e9;
        for (Player p : players) {
            double d = dist(p.pos, pos);
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }

    private void updatePlayerAI(Player p, Team team, Team opp, Ball ball, Player possessor, Player closestToBallTeam, Player closestToBallOpp, Field field, double dt, double time, Commentary comms) {
        // Cooldown
        if (p.decisionCooldown>0) p.decisionCooldown -= dt;

        // Desired position
        Vec2 targetPos = computeTargetPosition(p, team, opp, ball, possessor, field);

        // If we have the ball: decide to dribble, pass, or shoot
        boolean wePossess = possessor != null && possessor.team == team && possessor == p;
        if (wePossess && p.decisionCooldown <= 0) {
            decideWithBall(p, team, opp, ball, field, time, comms);
        } else if (!wePossess && p.role != Role.GK) {
            // Off the ball: either press, mark, or get into shape
            Vec2 pressTarget = maybePressTarget(p, team, opp, ball, possessor, field);
            if (pressTarget != null) targetPos = pressTarget;
        }

        // Move towards targetPos with basic steering
        steerTo(p, targetPos, dt, field);

        // Apply stamina cost
        p.applyStamina(dt, p.vel.len() > 0.8*p.maxSpeed);

        // Head turn toward direction of movement or ball
        double desiredHeading;
        if (wePossess) desiredHeading = Math.atan2(ball.pos.y - p.pos.y, ball.pos.x - p.pos.x);
        else if (p.vel.len2() > 1e-6) desiredHeading = Math.atan2(p.vel.y, p.vel.x);
        else desiredHeading = Math.atan2(ball.pos.y - p.pos.y, ball.pos.x - p.pos.x);
        double diff = angleDiff(p.heading, desiredHeading);
        double maxTurn = p.turnRate * dt;
        if (Math.abs(diff) <= maxTurn) p.heading = desiredHeading;
        else p.heading += Math.signum(diff)*maxTurn;
    }

    private double angleDiff(double a, double b) {
        double d = b - a;
        while (d > Math.PI) d -= Math.PI*2;
        while (d < -Math.PI) d += Math.PI*2;
        return d;
    }

    private Vec2 computeTargetPosition(Player p, Team team, Team opp, Ball ball, Player possessor, Field field) {
        // Base formation spot
        Vec2 n = team.formationSpot(p);
        Vec2 target = field.normalizedToWorld(n.x, n.y);

        // If we possess the ball, advance shape slightly
        boolean wePossess = possessor != null && possessor.team == team;
        if (wePossess) {
            double push = 5.0;
            target.x += team.direction * push;
        }

        // GK: use a different positioning: 20m from goal or line height
        if (p.role == Role.GK) {
            double goalX = team.direction < 0 ? field.rightGoalCenter().x : field.leftGoalCenter().x;
            double guardX = (goalX + field.center().x) / 2.0;
            // Also adjust to ball y slightly
            double clampY = Math.max(field.top + 3, Math.min(field.bottom - 3, ball.pos.y));
            return new Vec2(guardX, clampY);
        }

        // Support behavior: if teammate has ball, adjust to provide passing lane
        if (wePossess && possessor != p) {
            // Off-ball support: move to triangle: ahead and at an angle
            Vec2 dirToGoal = new Vec2(team.direction, 0);
            Vec2 right = new Vec2(0, 1);
            double offsetX = 6 + (p.role == Role.FWD ? 10 : (p.role == Role.MID ? 6 : 3));
            double offsetY = (p.slotIndex % 2 == 0? -1 : 1) * 4.5 * (0.6 + Math.random()*0.8);
            Vec2 support = new Vec2(possessor.pos.x + team.direction*offsetX, possessor.pos.y + offsetY);
            // Blend with formation target
            target.x = 0.6*target.x + 0.4*support.x;
            target.y = 0.7*target.y + 0.3*support.y;
            return clampToField(target, field, 1.0);
        }

        // If we don't have the ball: maybe shift toward ball side for compactness
        double sideBias = 0.35; // how much to shift horizontally towards ball lane
        target.y = 0.7*target.y + 0.3*(ball.pos.y * sideBias);

        return clampToField(target, field, 1.0);
    }

    private Vec2 clampToField(Vec2 p, Field field, double margin) {
        p.x = Math.max(field.left + margin, Math.min(field.right - margin, p.x));
        p.y = Math.max(field.top + margin, Math.min(field.bottom - margin, p.y));
        return p;
    }

    private Vec2 maybePressTarget(Player p, Team team, Team opp, Ball ball, Player possessor, Field field) {
        if (possessor == null) {
            // Loose ball: nearest two sprint towards it
            Player ourClosest = findClosest(team.players, ball.pos);
            double d = dist(p.pos, ball.pos);
            if (p == ourClosest || d < 12) {
                return new Vec2(ball.pos.x, ball.pos.y);
            }
            return null;
        } else {
            if (possessor.team == opp) {
                // Decide to press based on distance and team press intensity
                double d = dist(p.pos, possessor.pos);
                double threshold = 5 + 15*(1.0 - team.tactics.pressIntensity);
                if (d < threshold) {
                    // press towards a point slightly ahead of the possessor (cutting lane)
                    Vec2 toGoal = new Vec2(team.direction, 0); // opp attacks opposite
                    Vec2 intercept = new Vec2(possessor.pos.x + (oppDirection(opp)*2.0), possessor.pos.y);
                    return intercept;
                }
            }
        }
        return null;
    }

    private int oppDirection(Team opp) {
        return opp.direction;
    }

    private void steerTo(Player p, Vec2 targetPos, double dt, Field field) {
        // Seek behavior
        Vec2 desired = Vec2.sub(targetPos, p.pos);
        double d = desired.len();
        if (d < 0.2) {
            // slow down
            p.acc.set(0,0);
            p.vel.mul(0.85);
            return;
        }
        desired.norm().mul(p.maxSpeed);
        // Acceleration
        Vec2 steering = Vec2.sub(desired, p.vel);
        double maxAcc = p.accel;
        steering.limit(maxAcc);

        // Update velocity and position
        p.vel.add(Vec2.mul(steering, dt));
        // Friction
        p.vel.mul(1.0 - 0.6*dt);
        // Limit speed
        p.vel.limit(p.maxSpeed);

        // Collision with field bounds
        p.pos.add(Vec2.mul(p.vel, dt));
        if (p.pos.x < field.left + p.radius) { p.pos.x = field.left + p.radius; p.vel.x = 0; }
        if (p.pos.x > field.right - p.radius) { p.pos.x = field.right - p.radius; p.vel.x = 0; }
        if (p.pos.y < field.top + p.radius) { p.pos.y = field.top + p.radius; p.vel.y = 0; }
        if (p.pos.y > field.bottom - p.radius) { p.pos.y = field.bottom - p.radius; p.vel.y = 0; }
    }

    private void decideWithBall(Player p, Team team, Team opp, Ball ball, Field field, double time, Commentary comms) {
        // Compute shoot, dribble, pass options. Score them and choose.
        // Shooting is considered if within 25m and reasonable angle.
        Vec2 goalCenter = team.direction > 0 ? field.rightGoalCenter() : field.leftGoalCenter();
        double distToGoal = ballDistance(ball.pos, goalCenter);
        double shootScore = 0;
        if (distToGoal < 30) {
            // Angle factor: favor central positions
            double angleScore = 1.0 - Math.abs((ball.pos.y - goalCenter.y)/(field.goalWidth/2.0));
            angleScore = clamp(angleScore, 0, 1);
            shootScore = (1.3*angleScore + 0.4) * (1.0 - distToGoal/35.0) * (0.5 + 0.6*p.shootSkill);
        }

        // Dribble score: if space ahead
        double spaceAhead = estimateSpaceAhead(p, opp, field);
        double dribbleScore = 0.4 + 0.6*spaceAhead;
        dribbleScore *= 0.6 + 0.6*p.controlSkill;
        dribbleScore *= 0.9 + 0.2*team.tactics.tempo;

        // Pass options
        PassOption bestPass = findBestPass(p, team, opp, ball, field);
        double passScore = bestPass != null ? bestPass.score : 0;

        // Risk tolerance influences weighting
        double risk = team.tactics.passRisk;
        double shootWeight = 0.9*(0.4 + 0.6*p.shootSkill);
        double passWeight = 1.1*(0.6 + 0.4*p.passSkill)*(0.6 + 0.8*(1-risk));
        double dribbleWeight = 1.0*(0.6 + 0.4*team.tactics.tempo);

        double sShoot = shootScore * shootWeight;
        double sPass = passScore * passWeight;
        double sDribble = dribbleScore * dribbleWeight;

        // Make decision
        if (sShoot > sPass && sShoot > sDribble) {
            // Shoot!
            double basePower = 24 + 10*p.shootSkill;
            double power = basePower * (0.75 + 0.5*Math.random());
            Vec2 dir = Vec2.sub(goalCenter, ball.pos).norm();
            ball.kick(dir, power, p);
            p.decisionCooldown = 1.2*(1.0/p.reaction);
            p.lastActionTime = time;
        } else if (sPass > sDribble) {
            // Pass to target
            if (bestPass != null) {
                Vec2 dir = Vec2.sub(bestPass.targetPoint, ball.pos);
                double distance = dir.len();
                dir.norm();
                // Speed based on distance and pass type
                double speed = Math.min(28, 12 + distance*0.9*(0.8 + 0.4*p.passSkill));
                ball.kick(dir, speed, p);
                p.decisionCooldown = 0.9*(1.0/p.reaction);
                p.lastActionTime = time;
            } else {
                // fallback dribble
                dribbleForward(p, ball, team, field);
            }
        } else {
            // Dribble
            dribbleForward(p, ball, team, field);
        }
    }

    private void dribbleForward(Player p, Ball ball, Team team, Field field) {
        // Dribble by touching ball forward along heading towards goal
        Vec2 toGoal = Vec2.sub(team.direction > 0 ? field.rightGoalCenter() : field.leftGoalCenter(), p.pos).norm();
        // Slight lateral wobble
        double lateral = (Math.random()-0.5)*0.25;
        Vec2 dr = new Vec2(toGoal.x, toGoal.y + lateral).norm();
        double push = 6 + 4*p.controlSkill;
        ball.kick(dr, push, p);
        p.decisionCooldown = 0.6*(1.0/p.reaction);
    }

    static class PassOption {
        public Player receiver;
        public Vec2 targetPoint;
        public double score;
        public PassOption(Player r, Vec2 p, double s){ receiver=r; targetPoint=p; score=s; }
    }

    private PassOption findBestPass(Player p, Team team, Team opp, Ball ball, Field field) {
        PassOption best = null;
        for (Player r : team.players) {
            if (r == p) continue;
            // Evaluate line-of-sight, distance, angle to goal, opponent proximity
            Vec2 target = estimateReceivePoint(r, team, field);
            double distance = ballDistance(ball.pos, target);
            if (distance < 5) continue; // too close; better dribble
            double base = 1.0 - (distance/45.0); // prefer shorter passes
            base = clamp(base, 0, 1);
            // Angle to goal boost
            Vec2 goal = team.direction > 0 ? field.rightGoalCenter() : field.leftGoalCenter();
            double angleBoost = angleAlignment(target, goal, p.pos);
            // Opponent pressure: compute minimum distance from line to opponents
            double safety = lineSafety(ball.pos, target, opp.players);
            safety = clamp(safety/8.0, 0, 1); // >8m clearance is safe
            // Receiver pressure at target
            double receiverSpace = spaceAround(target, opp.players);
            receiverSpace = clamp(receiverSpace/6.0, 0, 1);
            // Receiver alignment: if receiver ahead (towards goal)
            double ahead = Math.signum((team.direction)*(target.x - p.pos.x));
            double aheadBoost = ahead > 0 ? 0.2 : -0.1;

            // Score
            double score = 0.35*base + 0.25*angleBoost + 0.25*safety + 0.20*receiverSpace + aheadBoost;
            // Add random minor jitter
            score += (Math.random()-0.5)*0.06;
            // Adjust by receiver pass lane distance vs team risk profile
            score *= (0.7 + 0.6*team.tactics.passRisk);
            // Increase score for forwards or central mids
            if (r.role == Role.FWD) score += 0.08;
            if (r.role == Role.MID) score += 0.04;

            if (best == null || score > best.score) {
                best = new PassOption(r, target, score);
            }
        }
        return best;
    }

    private Vec2 estimateReceivePoint(Player r, Team team, Field field) {
        // Blend formation spot with current position to avoid passing into crowded zones
        Vec2 form = team.formationSpot(r);
        Vec2 target = field.normalizedToWorld(form.x, form.y);
        // If receiver is moving, lead the pass slightly
        Vec2 lead = Vec2.add(r.pos, Vec2.mul(r.vel, 0.4));
        // Choose between target and lead based on how far from formation
        double weight = 0.4;
        return new Vec2(lead.x*weight + target.x*(1-weight), lead.y*weight + target.y*(1-weight));
    }

    private double angleAlignment(Vec2 a, Vec2 goal, Vec2 from) {
        Vec2 v1 = Vec2.sub(a, from).norm();
        Vec2 v2 = Vec2.sub(goal, a).norm();
        double d = v1.dot(v2);
        return clamp((d+1)/2, 0, 1);
    }

    private double lineSafety(Vec2 a, Vec2 b, List<Player> opps) {
        double min = 1e9;
        for (Player o : opps) {
            double d = distanceToSegment(o.pos, a, b);
            if (d < min) min = d;
        }
        return min;
    }

    private double distanceToSegment(Vec2 p, Vec2 a, Vec2 b) {
        Vec2 ab = Vec2.sub(b, a);
        Vec2 ap = Vec2.sub(p, a);
        double t = ap.dot(ab) / (ab.len2() + 1e-9);
        t = clamp(t, 0, 1);
        Vec2 proj = Vec2.add(a, Vec2.mul(ab, t));
        return Vec2.sub(p, proj).len();
    }

    private double spaceAround(Vec2 point, List<Player> opps) {
        double min = 1e9;
        for (Player o : opps) {
            double d = Vec2.sub(o.pos, point).len();
            if (d < min) min = d;
        }
        return min;
    }

    private double estimateSpaceAhead(Player p, Team opp, Field field) {
        // Sample points ahead of player
        double sum=0;
        Vec2 dirToGoal = new Vec2(p.team.direction, 0);
        for (int i=1;i<=3;i++) {
            Vec2 probe = Vec2.add(p.pos, Vec2.mul(dirToGoal, 3.0*i));
            sum += spaceAround(probe, opp.players);
        }
        sum /= 3.0;
        return clamp(sum/6.0, 0, 1);
    }

    private double ballDistance(Vec2 a, Vec2 b) {
        return Vec2.sub(a,b).len();
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

/**
 * Field (pitch) definition in world meters
 */
class Field {
    // Dimensions approximate to a real pitch size (105x68m)
    public double width = 105;
    public double height = 68;
    public double left, right, top, bottom;
    public double goalWidth = 7.32;
    public double penaltyAreaWidth = 40.32; // 40.32m
    public double penaltyAreaDepth = 16.5; // 16.5m
    public double boxWidth = 18.32;
    public double boxDepth = 5.5;
    public double centerCircleR = 9.15;

    public Field() {
        left = -width/2.0;
        right = width/2.0;
        top = -height/2.0;
        bottom = height/2.0;
    }

    public Vec2 center() { return new Vec2(0,0); }
    public Vec2 leftGoalCenter() { return new Vec2(left, 0); }
    public Vec2 rightGoalCenter() { return new Vec2(right, 0); }

    public Vec2 normalizedToWorld(double nx, double ny) {
        double x = nx*(width/2.0);
        double y = ny*(height/2.0);
        return new Vec2(x, y);
    }
}

/**
 * Match engine: updates physics and AI, evaluates goals and resets
 */
class MatchEngine {
    public Field field = new Field();
    public Team teamA, teamB;
    public Ball ball = new Ball();
    public Commentary comms = new Commentary();
    public Random rnd = new Random(42);
    public double timeElapsed = 0; // seconds
    public int half = 1;
    public double halfDuration = 45*60; // 45 minutes
    public boolean paused = false;
    public double speedMultiplier = 1.0;
    public boolean showHUD = true;

    // Physics parameters
    double ballFriction = 0.995;
    double ballGroundDrag = 0.3;
    double ballMaxSpeed = 32.0;

    // Control
    double kickoffCooldown = 0;

    public MatchEngine() {
        initTeams();
        resetKickoff(true);
    }

    private void initTeams() {
        teamA = new Team("Blue Hawks", new Color(58, 102, 182), new Color(200,220,255), 1, 101);
        teamB = new Team("Red Lions", new Color(200, 70, 70), new Color(255, 220, 220), -1, 202);

        // Optionally use different formations
        teamA.formation = Formation.F433();
        teamB.formation = Formation.F433();

        // Create players
        int idCounter = 1;
        makeTeamPlayers(teamA, idCounter); idCounter += teamA.formation.slots.size();
        makeTeamPlayers(teamB, idCounter); idCounter += teamB.formation.slots.size();
    }

    private void makeTeamPlayers(Team team, int startId) {
        int sz = team.formation.slots.size();
        String[] names = generateNames(team, sz);
        for (int i=0;i<sz;i++) {
            Role role = team.formation.getRole(i);
            int number = pickNumber(role, i);
            Color primary = team.kitColor();
            Color alt = team.altColor;
            Player p = new Player(startId+i, number, names[i], team, role, i, primary, alt);
            // Place on formation spot
            Vec2 n = team.formationSpot(p);
            Vec2 w = field.normalizedToWorld(n.x, n.y);
            p.pos.set(w);
            team.players.add(p);
        }
    }

    private int pickNumber(Role role, int idx) {
        if (role == Role.GK) return 1;
        return 2 + idx + new Random(200+idx).nextInt(20);
    }

    private String[] generateNames(Team team, int count) {
        String[] first = {"Alex","Ben","Chris","Dylan","Evan","Felix","Gabe","Harry","Ivan","Jake","Kai","Leo","Mason","Noah","Owen","Parker","Quinn","Ryan","Sam","Theo","Uri","Vince","Will","Xavi","Yuri","Zane"};
        String[] last = {"Adams","Baker","Clark","Diaz","Evans","Fisher","Gray","Hill","Ingram","Jones","Knight","Lopez","Miller","Ng","Owens","Perez","Quinn","Reid","Smith","Taylor","Usman","Vega","Woods","Xu","Young","Zimmer"};
        Random r = team.rnd;
        String[] names = new String[count];
        for (int i=0;i<count;i++) {
            names[i] = first[r.nextInt(first.length)] + " " + last[r.nextInt(last.length)];
        }
        return names;
    }

    public void resetKickoff(boolean starting) {
        // Reset positions to formation
        for (Team t : new Team[]{teamA, teamB}) {
            for (Player p : t.players) {
                Vec2 n = t.formationSpot(p);
                Vec2 w = field.normalizedToWorld(n.x, n.y);
                p.pos.set(w);
                p.vel.set(0,0);
                p.stamina = 1.0;
                p.decisionCooldown = 0;
                p.hasBall = false;
            }
        }
        ball.reset(field.center());
        // Pick kickoff team; at start of match, teamA kicks off; after goals, conceding team kicks off
        int dirA = teamA.direction;
        int dirB = teamB.direction;
        Team kickoffTeam = starting ? teamA : (ball.lastTouchTeam==0 ? teamB : teamA);
        // Put kickoff to a forward in center
        Player kicker = findKicker(kickoffTeam);
        if (kicker != null) {
            kicker.pos.set(field.center().x - kickoffTeam.direction*0.5, 0);
            ball.pos.set(field.center().x - kickoffTeam.direction*0.2, 0);
        }
        comms.add("Kickoff: " + kickoffTeam.name + " to start.");
        kickoffCooldown = 2.0;
    }

    private Player findKicker(Team team) {
        // Choose central forward or CAM
        Player best=null; double bestScore=-1;
        for (Player p : team.players) {
            double score = 0;
            if (p.role == Role.FWD) score += 2;
            if (p.role == Role.MID) score += 1;
            score += p.passSkill + 0.5*p.controlSkill;
            if (score > bestScore) { bestScore=score; best=p; }
        }
        return best;
    }

    public void update(double dt) {
        if (paused) return;
        dt *= speedMultiplier;
        // Cap dt to avoid physics blowups
        dt = Math.min(dt, 0.033);

        if (kickoffCooldown > 0) {
            kickoffCooldown -= dt;
        }

        // Update AI
        Player possessor = getPossessor();
        teamA.ai.updateAI(teamA, teamB, ball, field, dt, timeElapsed, comms);
        teamB.ai.updateAI(teamB, teamA, ball, field, dt, timeElapsed, comms);

        // Avoid players overlapping too much (simple repulsion)
        resolvePlayerCollisions(teamA.players, teamB.players);

        // Update ball physics and interactions
        updateBall(dt);

        // Check goals
        checkGoals();

        // Update time
        timeElapsed += dt;

        // Half-time and full-time
        handleHalves();
    }

    private void resolvePlayerCollisions(List<Player> a, List<Player> b) {
        // Within-team separation
        separateList(a);
        separateList(b);
        // Cross-team minimal separation
        for (Player p1 : a) {
            for (Player p2 : b) {
                separatePair(p1, p2, 0.84);
            }
        }
    }

    private void separateList(List<Player> list) {
        int n=list.size();
        for (int i=0;i<n;i++){
            for (int j=i+1;j<n;j++){
                separatePair(list.get(i), list.get(j), 0.84);
            }
        }
    }

    private void separatePair(Player p1, Player p2, double minDist) {
        Vec2 d = Vec2.sub(p2.pos, p1.pos);
        double dist2 = d.len2();
        double min2 = (minDist)*(minDist);
        if (dist2 < min2 && dist2 > 1e-6) {
            double dist = Math.sqrt(dist2);
            double overlap = (minDist - dist);
            d.div(dist);
            p1.pos.add(Vec2.mul(d, -overlap/2));
            p2.pos.add(Vec2.mul(d, overlap/2));
            // Damp velocities a touch
            p1.vel.mul(0.95);
            p2.vel.mul(0.95);
        }
    }

    Player getPossessor() {
        double controlR=0.6;
        Player possessor=null; double best=1e9;
        for (Player p : allPlayers()) {
            double d = Vec2.sub(p.pos, ball.pos).len();
            if (d < controlR && d<best) { best=d; possessor=p; }
        }
        return possessor;
    }

    private List<Player> allPlayers() {
        List<Player> all = new ArrayList<>(teamA.players);
        all.addAll(teamB.players);
        return all;
    }

    private void updateBall(double dt) {
        // If kickoff cooldown, allow only small touches
        if (kickoffCooldown > 0) {
            // Keep ball at kicker's feet if nearby
            Player closest = closestToBall(allPlayers());
            if (closest != null) {
                keepBallNear(closest, dt);
            }
            // Slow ball
            ball.vel.mul(0.9);
        } else {
            // Ball dynamics
            ball.vel.mul(Math.pow(ballFriction, dt*60.0));
            // Ground drag
            double drag = Math.max(0, 1.0 - ballGroundDrag*dt);
            ball.vel.mul(drag);
            // Limit speed
            double vlen=ball.vel.len();
            if (vlen > ballMaxSpeed) ball.vel.mul(ballMaxSpeed/vlen);

            // Integrate
            ball.pos.add(Vec2.mul(ball.vel, dt));

            // Collide with bounds; keep ball in play (except goals)
            if (ball.pos.y < field.top + ball.radius) {
                ball.pos.y = field.top + ball.radius; ball.vel.y = -ball.vel.y*0.6;
            }
            if (ball.pos.y > field.bottom - ball.radius) {
                ball.pos.y = field.bottom - ball.radius; ball.vel.y = -ball.vel.y*0.6;
            }
            if (ball.pos.x < field.left + ball.radius) {
                ball.pos.x = field.left + ball.radius; ball.vel.x = -ball.vel.x*0.6;
            }
            if (ball.pos.x > field.right - ball.radius) {
                ball.pos.x = field.right - ball.radius; ball.vel.x = -ball.vel.x*0.6;
            }

            // Interact with players: take control if within control radius and relative speed low
            Player closest = closestToBall(allPlayers());
            if (closest != null) {
                double d = Vec2.sub(closest.pos, ball.pos).len();
                double relSpeed = Vec2.sub(closest.vel, ball.vel).len();
                if (d < 0.55 && relSpeed < 9.0) {
                    // Slightly stick the ball ahead of player to simulate dribble touch
                    keepBallNear(closest, dt);
                }
            }
        }
    }

    private void keepBallNear(Player p, double dt) {
        Vec2 forward = Vec2.fromAngle(p.heading);
        double touch = 0.45;
        Vec2 target = Vec2.add(p.pos, Vec2.mul(forward, p.radius + ball.radius + 0.05));
        Vec2 to = Vec2.sub(target, ball.pos);
        // Kick gently towards target
        double speed = 6.0 + 6.0*p.controlSkill;
        ball.kick(to, speed, p);
    }

    private Player closestToBall(List<Player> players) {
        Player best=null; double bestD=1e9;
        for (Player p : players) {
            double d = Vec2.sub(p.pos, ball.pos).len();
            if (d < bestD) { bestD=d; best=p; }
        }
        return best;
    }

    private void checkGoals() {
        // Left goal: if ball crosses x < left and y within goal range
        double goalHalf = field.goalWidth/2.0;
        if (ball.pos.x <= field.left && Math.abs(ball.pos.y) <= goalHalf) {
            // Goal for team A (attacking right)
            teamA.score++;
            comms.add("GOAL! " + teamA.name + " scores. " + scoreLine());
            afterGoal(teamA);
        } else if (ball.pos.x >= field.right && Math.abs(ball.pos.y) <= goalHalf) {
            // Goal for team B
            teamB.score++;
            comms.add("GOAL! " + teamB.name + " scores. " + scoreLine());
            afterGoal(teamB);
        }
    }

    private String scoreLine() {
        return teamA.name + " " + teamA.score + " - " + teamB.score + " " + teamB.name;
    }

    private void afterGoal(Team scoringTeam) {
        // Small celebration: slow ball, pause, reset
        ball.vel.set(0,0);
        kickoffCooldown = 2.0;
        // Team that conceded kicks off
        ball.lastTouchTeam = (scoringTeam == teamA) ? 1 : 0;
        resetKickoff(false);
    }

    private void handleHalves() {
        if (timeElapsed >= halfDuration && half == 1) {
            half = 2;
            timeElapsed = 0;
            // Swap directions
            int tmp = teamA.direction;
            teamA.direction = teamB.direction;
            teamB.direction = tmp;
            comms.add("Half-time. Teams switch sides.");
            resetKickoff(true);
        } else if (timeElapsed >= halfDuration && half == 2) {
            paused = true;
            comms.add("Full-time. Final: " + scoreLine());
        }
    }

    public void restartMatch() {
        teamA.resetScore();
        teamB.resetScore();
        half = 1;
        timeElapsed = 0;
        teamA.direction = 1;
        teamB.direction = -1;
        comms.clear();
        resetKickoff(true);
        paused = false;
    }
}

/**
 * Rendering panel and input handling
 */
class GamePanel extends JPanel implements ActionListener, KeyListener, MouseWheelListener {
    private MatchEngine engine = new MatchEngine();
    private Timer timer;
    private long lastNanos;
    private double zoom = 8; // pixels per meter
    private double targetZoom = 8;
    private boolean fastForward = false;

    // UI layout
    private int sidebarWidth = 320;
    private Font fontSmall = new Font("SansSerif", Font.PLAIN, 12);
    private Font fontNormal = new Font("SansSerif", Font.BOLD, 14);
    private Font fontLarge = new Font("SansSerif", Font.BOLD, 18);
    private Font fontXL = new Font("SansSerif", Font.BOLD, 24);

    public GamePanel() {
        setPreferredSize(new Dimension(1280, 760));
        setBackground(new Color(18, 95, 25));
        setFocusable(true);
        addKeyListener(this);
        addMouseWheelListener(this);
        timer = new Timer(1000/60, this);
        timer.start();
        lastNanos = System.nanoTime();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - lastNanos)/1e9;
        lastNanos = now;
        // Smooth zoom
        zoom = zoom + (targetZoom - zoom)*0.15;
        engine.update(dt);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);
        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Draw pitch with proper aspect to field
        Rectangle pitchArea = new Rectangle(0, 0, w - sidebarWidth, h);
        drawPitch(g, pitchArea, engine.field);

        // Draw teams and ball
        drawEntities(g, pitchArea);

        // Sidebar HUD
        if (engine.showHUD) drawSidebar(g, new Rectangle(w - sidebarWidth, 0, sidebarWidth, h));
        drawTopBanner(g, pitchArea);
        drawInstructions(g, pitchArea);
    }

    private Point worldToScreen(Rectangle area, Vec2 world) {
        // Centered mapping: center of area is (0,0) world
        double cx = area.getCenterX();
        double cy = area.getCenterY();
        int sx = (int)Math.round(cx + world.x*zoom);
        int sy = (int)Math.round(cy + world.y*zoom);
        return new Point(sx, sy);
    }

    private int metersToPixels(double m) {
        return (int)Math.round(m*zoom);
    }

    private void drawPitch(Graphics2D g, Rectangle area, Field f) {
        // Grass base
        g.setColor(new Color(30, 130, 40));
        g.fillRect(area.x, area.y, area.width, area.height);

        // Stripes
        int stripes = 12;
        for (int i=0;i<stripes;i++){
            Color c = (i%2==0) ? new Color(26, 115, 35) : new Color(34, 140, 45);
            g.setColor(c);
            int stripeX = area.x + i*area.width/stripes;
            g.fillRect(stripeX, area.y, area.width/stripes, area.height);
        }

        // Lines (white)
        g.setStroke(new BasicStroke(2f));
        g.setColor(Color.WHITE);

        // Outer boundaries
        Point tl = worldToScreen(area, new Vec2(f.left, f.top));
        Point br = worldToScreen(area, new Vec2(f.right, f.bottom));
        g.drawRect(Math.min(tl.x, br.x), Math.min(tl.y, br.y), Math.abs(br.x-tl.x), Math.abs(br.y-tl.y));

        // Halfway line
        Point topMid = worldToScreen(area, new Vec2(0, f.top));
        Point botMid = worldToScreen(area, new Vec2(0, f.bottom));
        g.drawLine(topMid.x, topMid.y, botMid.x, botMid.y);

        // Center circle
        Point c = worldToScreen(area, f.center());
        int r = metersToPixels(f.centerCircleR);
        g.drawOval(c.x - r, c.y - r, 2*r, 2*r);

        // Center spot
        g.fillOval(c.x-3, c.y-3, 6, 6);

        // Penalty areas and 6-yard boxes, spots, arcs for both sides
        drawBoxes(g, area, f, true);
        drawBoxes(g, area, f, false);

        // Goals
        drawGoals(g, area, f);
    }

    private void drawBoxes(Graphics2D g, Rectangle area, Field f, boolean leftSide) {
        double sign = leftSide ? 1 : -1;
        double x = leftSide ? f.left : f.right;

        // Penalty area
        Vec2 pa1 = new Vec2(x + sign*f.penaltyAreaDepth, -f.penaltyAreaWidth/2.0);
        Vec2 pa2 = new Vec2(x, f.penaltyAreaWidth/2.0);
        // Draw rectangle lines
        Point p1 = worldToScreen(area, new Vec2(pa1.x, pa1.y));
        Point p2 = worldToScreen(area, new Vec2(pa1.x, -pa1.y));
        Point edge1 = worldToScreen(area, new Vec2(x, pa1.y));
        Point edge2 = worldToScreen(area, new Vec2(x, -pa1.y));
        g.drawLine(p1.x, p1.y, p2.x, p2.y);
        g.drawLine(p1.x, p1.y, edge1.x, edge1.y);
        g.drawLine(p2.x, p2.y, edge2.x, edge2.y);

        // Six-yard box
        Vec2 b1 = new Vec2(x + sign*f.boxDepth, -f.boxWidth/2.0);
        Point bp1 = worldToScreen(area, new Vec2(b1.x, b1.y));
        Point bp2 = worldToScreen(area, new Vec2(b1.x, -b1.y));
        Point bedge1 = worldToScreen(area, new Vec2(x, b1.y));
        Point bedge2 = worldToScreen(area, new Vec2(x, -b1.y));
        g.drawLine(bp1.x, bp1.y, bp2.x, bp2.y);
        g.drawLine(bp1.x, bp1.y, bedge1.x, bedge1.y);
        g.drawLine(bp2.x, bp2.y, bedge2.x, bedge2.y);

        // Penalty spot
        double penSpotX = x + sign*11.0;
        Point pen = worldToScreen(area, new Vec2(penSpotX, 0));
        g.fillOval(pen.x-3, pen.y-3, 6, 6);

        // Penalty arc (approximate)
        drawPenaltyArc(g, area, f, leftSide, penSpotX);
    }

    private void drawPenaltyArc(Graphics2D g, Rectangle area, Field f, boolean leftSide, double penSpotX) {
        // Center at penalty spot, radius 9.15, draw outside area only
        double sign = leftSide ? 1 : -1;
        double r = f.centerCircleR;
        // We'll draw arc around the penalty spot but only outside the box; approximate by arc from -35 to +35 degrees
        int deg = 90;
        int start = leftSide ? 90 - deg/2 : -90 - deg/2;
        Point pen = worldToScreen(area, new Vec2(penSpotX, 0));
        int R = metersToPixels(r);
        // Use drawArc bounding rectangle; but this would draw symmetric; acceptable for demo
        g.drawArc(pen.x - R, pen.y - R, 2*R, 2*R, start, deg);
    }

    private void drawGoals(Graphics2D g, Rectangle area, Field f) {
        g.setColor(Color.WHITE);
        // Left goal
        Vec2 gl = new Vec2(f.left, 0);
        Point glp = worldToScreen(area, gl);
        int hw = metersToPixels(f.goalWidth/2.0);
        g.drawRect(glp.x - 8, glp.y - hw, 8, 2*hw);
        // Right goal
        Vec2 gr = new Vec2(f.right, 0);
        Point grp = worldToScreen(area, gr);
        g.drawRect(grp.x, grp.y - hw, 8, 2*hw);
    }

    private void drawEntities(Graphics2D g, Rectangle area) {
        // Ball shadow
        Point bp = worldToScreen(area, engine.ball.pos);
        g.setColor(new Color(0,0,0,60));
        g.fillOval(bp.x-6, bp.y-6, 12, 12);

        // Teams
        drawTeam(g, area, engine.teamA, true);
        drawTeam(g, area, engine.teamB, true);

        // Ball
        int r = metersToPixels(engine.ball.radius);
        g.setColor(Color.WHITE);
        g.fillOval(bp.x - r, bp.y - r, 2*r, 2*r);
        g.setColor(new Color(0,0,0,50));
        g.drawOval(bp.x - r, bp.y - r, 2*r, 2*r);
    }

    private void drawTeam(Graphics2D g, Rectangle area, Team team, boolean showStamina) {
        for (Player p : team.players) {
            Point sp = worldToScreen(area, p.pos);
            int pr = metersToPixels(p.radius);
            // Shadow
            g.setColor(new Color(0,0,0,60));
            g.fillOval(sp.x - pr, sp.y - pr/2, 2*pr, pr);

            // Body
            g.setColor(p.colorPrimary);
            g.fillOval(sp.x - pr, sp.y - pr, 2*pr, 2*pr);
            g.setColor(new Color(255,255,255,180));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(sp.x - pr, sp.y - pr, 2*pr, 2*pr);

            // Heading triangle
            double ang = p.heading;
            int headLen = pr + 8;
            int fx = (int)Math.round(sp.x + Math.cos(ang)*headLen);
            int fy = (int)Math.round(sp.y + Math.sin(ang)*headLen);
            g.setColor(new Color(255,255,255,140));
            g.drawLine(sp.x, sp.y, fx, fy);

            // Number
            g.setFont(fontSmall);
            String num = String.valueOf(p.number);
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(num);
            g.setColor(new Color(0,0,0,200));
            g.drawString(num, sp.x - tw/2 + 1, sp.y + fm.getAscent()/2 + 1);
            g.setColor(Color.WHITE);
            g.drawString(num, sp.x - tw/2, sp.y + fm.getAscent()/2);

            // If ball possessor, draw glow
            Player poss = engine.getPossessor();
            if (poss == p) {
                g.setColor(new Color(255,255,120,90));
                g.setStroke(new BasicStroke(3f));
                g.drawOval(sp.x - pr - 4, sp.y - pr - 4, 2*(pr + 4), 2*(pr + 4));
            }

            // Stamina bar
            if (showStamina) {
                int bw = Math.max(24, pr*3);
                int bh = 5;
                int bx = sp.x - bw/2;
                int by = sp.y + pr + 8;
                g.setColor(new Color(0,0,0,160));
                g.fillRoundRect(bx, by, bw, bh, 6, 6);
                int fill = (int)Math.round(bw * p.stamina);
                g.setColor(new Color(0, 200, 80, 200));
                g.fillRoundRect(bx, by, fill, bh, 6, 6);
            }
        }
    }

    private void drawTopBanner(Graphics2D g, Rectangle area) {
        // Score and clock at top center
        g.setFont(fontXL);
        String score = engine.teamA.name + " " + engine.teamA.score + " - " + engine.teamB.score + " " + engine.teamB.name;
        double totalSeconds = engine.timeElapsed + (engine.half==2? 45*60 : 0);
        int minutes = (int)(totalSeconds / 60);
        int seconds = (int)(totalSeconds % 60);
        String clock = String.format("%s  %02d:%02d  H%d %s", engine.paused? "[PAUSED]" : "", minutes, seconds, engine.half, (fastForward? ">>" : ""));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(score);
        int cw = fm.stringWidth(clock);
        int x = (int)area.getCenterX();
        int y = 36;

        // backdrop
        g.setColor(new Color(0,0,0,120));
        g.fillRoundRect(x - Math.max(tw,cw)/2 - 16, 8, Math.max(tw,cw) + 32, 60, 10,10);

        g.setColor(Color.WHITE);
        g.drawString(score, x - tw/2, y);
        g.setFont(fontLarge);
        g.drawString(clock, x - cw/2, y + 24);
    }

    private void drawSidebar(Graphics2D g, Rectangle area) {
        // Sidebar background
        g.setColor(new Color(20, 20, 25, 230));
        g.fillRect(area.x, area.y, area.width, area.height);

        int x = area.x + 16;
        int y = area.y + 20;

        // Team A header
        g.setColor(engine.teamA.kitColor());
        g.fillRoundRect(x, y, area.width - 32, 40, 8, 8);
        g.setColor(Color.WHITE);
        g.setFont(fontLarge);
        g.drawString(engine.teamA.name, x + 10, y + 26);
        y += 48;

        // Tactics A
        g.setFont(fontNormal);
        g.setColor(Color.WHITE);
        g.drawString("Tactics: " + engine.teamA.tactics.name, x, y); y+=20;
        g.setFont(fontSmall);
        g.drawString(String.format("Line Height: %.2f  Press: %.2f  Risk: %.2f  Tempo: %.2f", engine.teamA.tactics.lineHeight, engine.teamA.tactics.pressIntensity, engine.teamA.tactics.passRisk, engine.teamA.tactics.tempo), x, y); y+=20;

        // Team B header
        g.setColor(engine.teamB.kitColor());
        g.fillRoundRect(x, y, area.width - 32, 40, 8, 8);
        g.setColor(Color.WHITE);
        g.setFont(fontLarge);
        g.drawString(engine.teamB.name, x + 10, y + 26);
        y += 48;

        // Tactics B
        g.setFont(fontNormal);
        g.setColor(Color.WHITE);
        g.drawString("Tactics: " + engine.teamB.tactics.name, x, y); y+=20;
        g.setFont(fontSmall);
        g.drawString(String.format("Line Height: %.2f  Press: %.2f  Risk: %.2f  Tempo: %.2f", engine.teamB.tactics.lineHeight, engine.teamB.tactics.pressIntensity, engine.teamB.tactics.passRisk, engine.teamB.tactics.tempo), x, y); y+=24;

        // Possession bar: estimate via last touch or proximity heuristic
        double aPos = estimatePossession(engine.teamA, engine.teamB, engine.ball);
        int barW = area.width - 32;
        int barH = 16;
        g.setColor(Color.WHITE);
        g.drawString("Possession (approx.):", x, y); y+=16;
        g.setColor(new Color(0,0,0,120));
        g.fillRoundRect(x, y, barW, barH, 8, 8);
        int aW = (int)Math.round(barW*aPos);
        g.setColor(engine.teamA.kitColor());
        g.fillRoundRect(x, y, aW, barH, 8, 8);
        g.setColor(engine.teamB.kitColor());
        g.fillRoundRect(x + aW, y, barW - aW, barH, 8, 8);
        g.setColor(Color.WHITE);
        g.drawString(String.format("%s %d%%  |  %d%% %s", engine.teamA.name, (int)Math.round(aPos*100), 100-(int)Math.round(aPos*100), engine.teamB.name), x, y + barH + 14);
        y += barH + 30;

        // Commentary
        g.setFont(fontNormal);
        g.drawString("Commentary:", x, y); y+=18;
        g.setFont(fontSmall);
        List<String> lines = engine.comms.get();
        int maxLines = (area.height - y - 16)/18;
        int start = Math.max(0, lines.size() - maxLines);
        g.setColor(new Color(220,230,245));
        for (int i=start;i<lines.size();i++){
            g.drawString(lines.get(i), x, y + (i-start)*18);
        }

        // Footer
        g.setColor(new Color(255,255,255,140));
        g.drawString("Speed: " + String.format("%.1fx", engine.speedMultiplier), x, area.height - 28);
    }

    private double estimatePossession(Team a, Team b, Ball ball) {
        // Simple: pick last toucher if recent, else based on nearest players
        if (engine.kickoffCooldown > 0) {
            return a.direction > 0 ? 1.0 : 0.0;
        }
        Player ca = closestTo(ball.pos, a.players);
        Player cb = closestTo(ball.pos, b.players);
        double da = ca != null ? Vec2.sub(ca.pos, ball.pos).len() : 1000;
        double db = cb != null ? Vec2.sub(cb.pos, ball.pos).len() : 1000;
        double pos = db + da > 0 ? db/(db+da) : 0.5;
        return clamp(pos, 0, 1);
    }

    private Player closestTo(Vec2 pos, List<Player> ps) {
        Player best=null; double bd=1e9;
        for (Player p : ps) {
            double d = Vec2.sub(p.pos, pos).len();
            if (d < bd) { bd=d; best=p; }
        }
        return best;
    }

    private double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private void drawInstructions(Graphics2D g, Rectangle area) {
        g.setFont(fontSmall);
        g.setColor(new Color(255,255,255,180));
        int x = (int)area.getMinX() + 12;
        int y = (int)area.getMaxY() - 12;
        String s1 = "[P]ause  [F]ast-forward  [R]estart  [H]UD  +/- Zoom  Team A tactics: 1/2/3  Team B: 7/8/9";
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(s1);
        g.drawString(s1, x, y);
    }

    // Key handling: controls
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_P:
                engine.paused = !engine.paused; break;
            case KeyEvent.VK_F:
                fastForward = !fastForward;
                engine.speedMultiplier = fastForward ? 2.0 : 1.0; break;
            case KeyEvent.VK_R:
                engine.restartMatch(); break;
            case KeyEvent.VK_H:
                engine.showHUD = !engine.showHUD; break;
            case KeyEvent.VK_1:
                engine.teamA.tactics = Tactics.defensive(); break;
            case KeyEvent.VK_2:
                engine.teamA.tactics = Tactics.balanced(); break;
            case KeyEvent.VK_3:
                engine.teamA.tactics = Tactics.attacking(); break;
            case KeyEvent.VK_7:
                engine.teamB.tactics = Tactics.defensive(); break;
            case KeyEvent.VK_8:
                engine.teamB.tactics = Tactics.balanced(); break;
            case KeyEvent.VK_9:
                engine.teamB.tactics = Tactics.attacking(); break;
            case KeyEvent.VK_EQUALS: case KeyEvent.VK_PLUS:
                targetZoom = Math.min(16, targetZoom + 0.5); break;
            case KeyEvent.VK_MINUS:
                targetZoom = Math.max(4, targetZoom - 0.5); break;
        }
    }
    @Override public void keyReleased(KeyEvent e) {}

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int rot = e.getWheelRotation();
        if (rot < 0) targetZoom = Math.min(16, targetZoom + 0.5);
        else targetZoom = Math.max(4, targetZoom - 0.5);
    }
}