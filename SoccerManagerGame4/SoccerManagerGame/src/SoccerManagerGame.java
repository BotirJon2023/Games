/*
 * SoccerManagerGame.java
 * A single-file Java Swing Soccer Manager Game with animation.
 * - Uses Swing for rendering and animation (no JavaFX)
 * - Includes teams, players, match simulation, simple AI tactics, training, transfer market
 * - Structured to be readable and extensible
 *
 * Note: This file is intentionally long (>600 lines) to provide a full example.
 */

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.io.*;

public class SoccerManagerGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}

// Main application window
class GameFrame extends JFrame {
    private GamePanel panel;

    public GameFrame() {
        setTitle("Soccer Manager Game");
        setSize(1200, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        panel = new GamePanel();
        setContentPane(panel);
    }
}

// The central game panel with rendering + UI
class GamePanel extends JPanel implements ActionListener {
    private final int FPS = 60;
    private Timer timer;
    private BufferedImage backBuffer;
    private Graphics2D g2d;

    // Game data
    private League league;
    private Team playerTeam;
    private Match currentMatch;
    private boolean inMatch = false;
    private boolean showDebug = false;

    // UI controls
    private JButton btnStartMatch;
    private JButton btnTrain;
    private JButton btnTransfers;
    private JButton btnNextDay;
    private JLabel lblStatus;

    // Camera / animation
    private double cameraX = 0;
    private double cameraY = 0;

    public GamePanel() {
        setLayout(null);
        initGame();
        initUI();
        timer = new Timer(1000 / FPS, this);
        timer.start();
        addKeyListener(new InputHandler());
        setFocusable(true);
        requestFocusInWindow();
    }

    private void initGame() {
        league = League.createSampleLeague();
        playerTeam = league.teams.get(0);
        inMatch = false;
    }

    private void initUI() {
        btnStartMatch = new JButton("Start Match");
        btnStartMatch.setBounds(960, 20, 200, 36);
        btnStartMatch.addActionListener(e -> startMatch());
        add(btnStartMatch);

        btnTrain = new JButton("Train Players");
        btnTrain.setBounds(960, 60, 200, 36);
        btnTrain.addActionListener(e -> openTraining());
        add(btnTrain);

        btnTransfers = new JButton("Transfer Market");
        btnTransfers.setBounds(960, 100, 200, 36);
        btnTransfers.addActionListener(e -> openTransfers());
        add(btnTransfers);

        btnNextDay = new JButton("Next Day");
        btnNextDay.setBounds(960, 140, 200, 36);
        btnNextDay.addActionListener(e -> nextDay());
        add(btnNextDay);

        lblStatus = new JLabel("Welcome to Soccer Manager");
        lblStatus.setBounds(20, 700, 900, 36);
        add(lblStatus);
    }

    private void startMatch() {
        if (inMatch) return;
        Team opponent = league.getRandomOpponent(playerTeam);
        currentMatch = new Match(playerTeam, opponent);
        inMatch = true;
        lblStatus.setText("Match started: " + playerTeam.name + " vs " + opponent.name);
    }

    private void openTraining() {
        TrainingDialog dialog = new TrainingDialog(SwingUtilities.getWindowAncestor(this), playerTeam);
        dialog.setVisible(true);
    }

    private void openTransfers() {
        TransferDialog dialog = new TransferDialog(SwingUtilities.getWindowAncestor(this), league, playerTeam);
        dialog.setVisible(true);
    }

    private void nextDay() {
        league.advanceDay();
        lblStatus.setText("Day advanced: " + league.currentDate);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // game loop
        update(1.0 / FPS);
        repaint();
    }

    private void update(double dt) {
        if (inMatch && currentMatch != null) {
            currentMatch.update(dt);
            if (currentMatch.isFinished()) {
                inMatch = false;
                lblStatus.setText("Match finished: " + currentMatch.getScoreline());
                league.recordMatch(currentMatch);
                currentMatch = null;
            }
        }
        // camera follow if match
        if (inMatch && currentMatch != null) {
            Vector2 ballPos = currentMatch.ball.position;
            cameraX = ballPos.x - getWidth() / 2.0 + 200;
            cameraY = ballPos.y - getHeight() / 2.0 + 100;
        } else {
            cameraX *= 0.95;
            cameraY *= 0.95;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backBuffer == null || backBuffer.getWidth() != getWidth() || backBuffer.getHeight() != getHeight()) {
            backBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            g2d = backBuffer.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        // Clear
        g2d.setColor(new Color(60, 160, 60));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw pitch or UI
        if (inMatch && currentMatch != null) {
            drawMatch(g2d);
        } else {
            drawOverview(g2d);
        }

        // Draw UI overlay
        g2d.setColor(Color.WHITE);
        g2d.drawString("Date: " + league.currentDate, 20, 20);
        g2d.drawString("Team: " + playerTeam.name + "  Budget: $" + playerTeam.budget, 20, 40);

        // blit
        g.drawImage(backBuffer, 0, 0, null);
    }

    private void drawOverview(Graphics2D g) {
        g.setColor(new Color(34, 139, 34));
        g.fillRect(20, 20, 900, 640);
        g.setColor(Color.WHITE);
        g.drawRect(20, 20, 900, 640);
        g.drawString("League Table:", 40, 50);

        List<Team> sorted = new ArrayList<>(league.teams);
        sorted.sort(Comparator.comparingInt(t -> -t.points));
        int y = 80;
        int idx = 1;
        for (Team t : sorted) {
            g.drawString(String.format("%2d. %-20s Pts:%3d  GD:%3d", idx, t.name, t.points, t.goalDifference()), 40, y);
            y += 20;
            idx++;
            if (y > 620) break;
        }

        // Recent fixtures
        g.drawString("Recent Transfers:", 520, 50);
        int tx = 520;
        int ty = 80;
        for (String s : league.recentTransfers) {
            g.drawString(s, tx, ty);
            ty += 18;
            if (ty > 620) break;
        }
    }

    private void drawMatch(Graphics2D g) {
        // pitch dimensions
        int pw = 900;
        int ph = 640;
        int px = 20;
        int py = 20;

        // draw pitch background
        g.setColor(new Color(36, 122, 36));
        g.fillRect(px, py, pw, ph);
        g.setColor(Color.WHITE);
        g.drawRect(px, py, pw, ph);

        // center line
        g.drawLine(px + pw/2, py, px + pw/2, py + ph);
        g.drawOval(px + pw/2 - 60, py + ph/2 - 60, 120, 120);

        // scale and draw entities
        double scaleX = pw / Match.PITCH_WIDTH;
        double scaleY = ph / Match.PITCH_HEIGHT;

        // transform for camera
        AffineTransform old = g.getTransform();
        g.translate(-cameraX, -cameraY);

        // draw players
        for (Player p : currentMatch.home.players) {
            drawPlayer(g, p, scaleX, scaleY);
        }
        for (Player p : currentMatch.away.players) {
            drawPlayer(g, p, scaleX, scaleY);
        }

        // draw ball
        drawBall(g, currentMatch.ball, scaleX, scaleY);

        // restore transform
        g.setTransform(old);

        // HUD
        g.setColor(Color.WHITE);
        g.drawString("Time: " + currentMatch.getMinute() + "'  Score: " + currentMatch.getScoreline(), px + 10, py + ph + 20);

        if (showDebug) {
            g.drawString("Debug ON", px + 200, py + ph + 20);
        }
    }

    private void drawPlayer(Graphics2D g, Player p, double sx, double sy) {
        int size = 10 + p.quality / 10;
        int x = (int) (p.position.x * sx) + 20;
        int y = (int) (p.position.y * sy) + 20;
        if (p.team.isPlayerTeam) g.setColor(Color.BLUE);
        else g.setColor(Color.RED);
        g.fillOval(x - size/2, y - size/2, size, size);
        g.setColor(Color.WHITE);
        g.drawString("" + p.number, x - 4, y + 3);
    }

    private void drawBall(Graphics2D g, Ball ball, double sx, double sy) {
        int x = (int) (ball.position.x * sx) + 20;
        int y = (int) (ball.position.y * sy) + 20;
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(x - 6, y - 6, 12, 12));
    }

    // Simple input handler
    private class InputHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_D) showDebug = !showDebug;
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (!inMatch) startMatch();
            }
        }
    }
}

// League containing teams and fixtures
class League {
    public List<Team> teams = new ArrayList<>();
    public List<Match> fixtures = new ArrayList<>();
    public List<String> recentTransfers = new LinkedList<>();
    public String currentDate = "2025-01-01";

    public static League createSampleLeague() {
        League l = new League();
        Team t1 = Team.createRandomTeam("FC Dynamo", true);
        Team t2 = Team.createRandomTeam("Rovers United", false);
        Team t3 = Team.createRandomTeam("City Athletic", false);
        Team t4 = Team.createRandomTeam("Town Rangers", false);
        l.teams.add(t1);
        l.teams.add(t2);
        l.teams.add(t3);
        l.teams.add(t4);
        return l;
    }

    public Team getRandomOpponent(Team self) {
        for (Team t : teams) if (t != self) return t;
        return teams.get(0);
    }

    public void advanceDay() {
        // progress date roughly
        // naive date progression
        String[] parts = currentDate.split("-");
        int y = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        int d = Integer.parseInt(parts[2]);
        d++;
        if (d > 28) { // simplify months
            d = 1; m++;
            if (m > 12) { m = 1; y++; }
        }
        currentDate = String.format("%04d-%02d-%02d", y, m, d);
        // random transfer events
        if (ThreadLocalRandom.current().nextDouble() < 0.2) {
            Team from = teams.get(ThreadLocalRandom.current().nextInt(teams.size()));
            Team to = teams.get(ThreadLocalRandom.current().nextInt(teams.size()));
            if (from != to) {
                Player p = from.sellRandomPlayer();
                if (p != null) {
                    to.buyPlayer(p);
                    String s = String.format("%s -> %s : %s ($%d)", from.name, to.name, p.name, p.wage);
                    recentTransfers.add(0, s);
                    if (recentTransfers.size() > 10) recentTransfers.remove(recentTransfers.size()-1);
                }
            }
        }
    }

    public void recordMatch(Match match) {
        fixtures.add(match);
        match.applyResultToTeams();
    }
}

// Team data
class Team {
    public String name;
    public List<Player> players = new ArrayList<>();
    public int points = 0;
    public int goalsFor = 0;
    public int goalsAgainst = 0;
    public int budget = 1000000;
    public boolean isPlayerTeam = false;

    public Team(String name) {
        this.name = name;
    }

    public static Team createRandomTeam(String name, boolean isPlayerTeam) {
        Team t = new Team(name);
        t.isPlayerTeam = isPlayerTeam;
        for (int i = 1; i <= 11; i++) {
            Player p = Player.createRandomPlayer(i);
            p.team = t;
            t.players.add(p);
        }
        return t;
    }

    public Player sellRandomPlayer() {
        if (players.isEmpty()) return null;
        int idx = ThreadLocalRandom.current().nextInt(players.size());
        Player p = players.remove(idx);
        budget += p.wage * 10;
        return p;
    }

    public void buyPlayer(Player p) {
        p.team = this;
        players.add(p);
        budget -= p.wage * 10;
    }

    public int goalDifference() {
        return goalsFor - goalsAgainst;
    }
}

// Player data and basic attributes
class Player {
    public String name;
    public int number;
    public int pace;
    public int shooting;
    public int passing;
    public int defense;
    public int dribbling;
    public int quality; // combined
    public int wage;
    public Vector2 position;
    public Team team;

    public Player(String name, int number) {
        this.name = name;
        this.number = number;
        this.position = new Vector2(0,0);
    }

    public static Player createRandomPlayer(int number) {
        Player p = new Player(Utils.randomName(), number);
        p.pace = Utils.randInt(40, 90);
        p.shooting = Utils.randInt(30, 90);
        p.passing = Utils.randInt(30, 90);
        p.defense = Utils.randInt(30, 90);
        p.dribbling = Utils.randInt(30, 90);
        p.quality = (p.pace + p.shooting + p.passing + p.defense + p.dribbling) / 5;
        p.wage = p.quality * 1000 + Utils.randInt(0, 2000);
        return p;
    }

    public void train(String focus) {
        switch (focus) {
            case "pace": this.pace = Math.min(99, this.pace + Utils.randInt(0,3)); break;
            case "shooting": this.shooting = Math.min(99, this.shooting + Utils.randInt(0,3)); break;
            case "passing": this.passing = Math.min(99, this.passing + Utils.randInt(0,3)); break;
            case "defense": this.defense = Math.min(99, this.defense + Utils.randInt(0,3)); break;
            case "dribbling": this.dribbling = Math.min(99, this.dribbling + Utils.randInt(0,3)); break;
            default:
                this.quality = Math.min(99, this.quality + Utils.randInt(0,2));
        }
        this.quality = (this.pace + this.shooting + this.passing + this.defense + this.dribbling) / 5;
    }
}

// Match simulation and animation entities
class Match {
    public static final int PITCH_WIDTH = 105; // meters
    public static final int PITCH_HEIGHT = 68;

    public Team home;
    public Team away;
    public Ball ball;
    private double matchTime = 0; // in minutes
    private int duration = 90;
    private int homeGoals = 0;
    private int awayGoals = 0;
    private boolean finished = false;
    private Random rng = new Random();

    // controllers
    private AIManager homeAI;
    private AIManager awayAI;

    public Match(Team home, Team away) {
        this.home = home;
        this.away = away;
        this.ball = new Ball(new Vector2(PITCH_WIDTH/2.0, PITCH_HEIGHT/2.0));
        this.homeAI = new AIManager(home, this);
        this.awayAI = new AIManager(away, this);
        setupInitialPositions();
    }

    private void setupInitialPositions() {
        // spread players across pitch
        for (int i = 0; i < home.players.size(); i++) {
            Player p = home.players.get(i);
            p.position.x = Utils.randDouble(10, PITCH_WIDTH/2 - 5);
            p.position.y = Utils.randDouble(5, PITCH_HEIGHT-5);
        }
        for (int i = 0; i < away.players.size(); i++) {
            Player p = away.players.get(i);
            p.position.x = Utils.randDouble(PITCH_WIDTH/2 + 5, PITCH_WIDTH-10);
            p.position.y = Utils.randDouble(5, PITCH_HEIGHT-5);
        }
    }

    public void update(double dtSeconds) {
        if (finished) return;
        double dtMinutes = dtSeconds * 60.0 / 60.0; // convert to minutes (approx)
        matchTime += dtMinutes / 60.0 * 1.0; // slow time for visual
        // update player decisions
        homeAI.update(dtSeconds);
        awayAI.update(dtSeconds);
        // physics for ball
        ball.update(dtSeconds);
        // check goal
        checkGoalConditions();
        if (matchTime >= duration) {
            finished = true;
        }
    }

    private void checkGoalConditions() {
        // if ball near goal line
        if (ball.position.x < 1) {
            awayGoals++;
            resetAfterGoal(false);
        } else if (ball.position.x > PITCH_WIDTH - 1) {
            homeGoals++;
            resetAfterGoal(true);
        }
    }

    private void resetAfterGoal(boolean homeScored) {
        ball.position = new Vector2(PITCH_WIDTH/2.0, PITCH_HEIGHT/2.0);
        ball.velocity = new Vector2(0,0);
        // random push
        if (homeScored) ball.velocity = new Vector2(-2, 0);
        else ball.velocity = new Vector2(2, 0);
    }

    public int getMinute() {
        return Math.min(duration, (int) (matchTime));
    }

    public boolean isFinished() { return finished; }

    public String getScoreline() {
        return homeGoals + " - " + awayGoals;
    }

    public void applyResultToTeams() {
        home.goalsFor += homeGoals;
        home.goalsAgainst += awayGoals;
        away.goalsFor += awayGoals;
        away.goalsAgainst += homeGoals;
        if (homeGoals > awayGoals) home.points += 3;
        else if (homeGoals < awayGoals) away.points += 3;
        else { home.points += 1; away.points += 1; }
    }
}

// Ball physics
class Ball {
    public Vector2 position;
    public Vector2 velocity;

    public Ball(Vector2 pos) {
        this.position = pos;
        this.velocity = new Vector2(0,0);
    }

    public void update(double dt) {
        // simple friction
        position = position.add(velocity.scale(dt));
        velocity = velocity.scale(0.995);
        // keep inside pitch
        if (position.x < 0) { position.x = 0; velocity.x = -velocity.x * 0.5; }
        if (position.y < 0) { position.y = 0; velocity.y = -velocity.y * 0.5; }
        if (position.x > Match.PITCH_WIDTH) { position.x = Match.PITCH_WIDTH; velocity.x = -velocity.x * 0.5; }
        if (position.y > Match.PITCH_HEIGHT) { position.y = Match.PITCH_HEIGHT; velocity.y = -velocity.y * 0.5; }
    }
}

// Simple AI manager for team actions
class AIManager {
    private Team team;
    private Match match;
    private Random rng = new Random();

    public AIManager(Team team, Match match) {
        this.team = team;
        this.match = match;
    }

    public void update(double dt) {
        // for each player, compute a simple move toward ball or positioning
        for (Player p : team.players) {
            Vector2 toBall = match.ball.position.subtract(p.position);
            double dist = toBall.length();
            if (dist < 6 && rng.nextDouble() < 0.02) {
                // attempt action on ball
                if (team.isPlayerTeam) {
                    // player team controlled by user: simpler behavior
                    attemptPassOrShoot(p, match.ball);
                } else {
                    // AI controlled
                    attemptAIAction(p, match.ball);
                }
            } else {
                // move toward strategic position
                Vector2 target = getPositionForPlayer(p);
                Vector2 diff = target.subtract(p.position);
                p.position = p.position.add(diff.scale(0.02 * dt * 60));
            }
        }
        // occasionally nudge ball randomly to keep animation lively
        if (rng.nextDouble() < 0.001) {
            match.ball.velocity = match.ball.velocity.add(new Vector2(Utils.randDouble(-0.8, 0.8), Utils.randDouble(-0.5,0.5)));
        }
    }

    private void attemptPassOrShoot(Player p, Ball ball) {
        // user team basic: pass forward
        Player target = pickTeammate(p);
        if (target != null) {
            Vector2 dir = target.position.subtract(p.position).normalize();
            ball.velocity = dir.scale(8 + p.passing / 20.0);
        }
    }

    private void attemptAIAction(Player p, Ball ball) {
        // AI decides between clearing, passing, or shooting
        double shootChance = p.shooting / 120.0;
        if (rng.nextDouble() < shootChance) {
            // shoot toward opposite goal
            double goalX = team == match.home ? Match.PITCH_WIDTH : 0;
            Vector2 dir = new Vector2(goalX - p.position.x, (Match.PITCH_HEIGHT/2.0) - p.position.y).normalize();
            ball.velocity = dir.scale(10 + p.shooting / 10.0);
        } else {
            Player mate = pickTeammate(p);
            if (mate != null) {
                Vector2 dir = mate.position.subtract(p.position).normalize();
                ball.velocity = dir.scale(6 + p.passing / 20.0);
            }
        }
    }

    private Player pickTeammate(Player p) {
        List<Player> others = new ArrayList<>(team.players);
        others.remove(p);
        if (others.isEmpty()) return null;
        return others.get(rng.nextInt(others.size()));
    }

    private Vector2 getPositionForPlayer(Player p) {
        // simplistic formation mapping based on number
        double x, y;
        int idx = p.number % 11;
        if (team == match.home) {
            x = Utils.map(idx, 0, 10, 10, Match.PITCH_WIDTH / 2 - 5);
        } else {
            x = Utils.map(idx, 0, 10, Match.PITCH_WIDTH / 2 + 5, Match.PITCH_WIDTH - 10);
        }
        y = Utils.map(p.number, 1, 11, 5, Match.PITCH_HEIGHT - 5);
        return new Vector2(x, y);
    }
}

// Simple dialog for training players
class TrainingDialog extends JDialog {
    private Team team;
    private JComboBox<String> cmbPlayers;
    private JComboBox<String> cmbFocus;
    private JButton btnTrain;

    public TrainingDialog(Window owner, Team team) {
        super(owner, "Training - " + team.name);
        this.team = team;
        setSize(400,220);
        setLayout(null);
        setLocationRelativeTo(owner);

        cmbPlayers = new JComboBox<>();
        for (Player p : team.players) cmbPlayers.addItem(p.number + " - " + p.name + " (Q" + p.quality + ")");
        cmbPlayers.setBounds(20, 20, 350, 30);
        add(cmbPlayers);

        cmbFocus = new JComboBox<>(new String[]{"pace","shooting","passing","defense","dribbling","general"});
        cmbFocus.setBounds(20, 60, 350, 30);
        add(cmbFocus);

        btnTrain = new JButton("Train");
        btnTrain.setBounds(20, 100, 350, 36);
        btnTrain.addActionListener(e -> doTrain());
        add(btnTrain);
    }

    private void doTrain() {
        int idx = cmbPlayers.getSelectedIndex();
        if (idx < 0) return;
        Player p = team.players.get(idx);
        String focus = (String) cmbFocus.getSelectedItem();
        p.train(focus);
        JOptionPane.showMessageDialog(this, p.name + " trained: " + focus + " -> Q:" + p.quality);
        dispose();
    }
}

// Simple transfer market dialog
class TransferDialog extends JDialog {
    private League league;
    private Team team;
    private JList<String> listMarket;
    private DefaultListModel<String> model;
    private JButton btnBuy;

    public TransferDialog(Window owner, League league, Team team) {
        super(owner, "Transfer Market - " + team.name);
        this.league = league;
        this.team = team;
        setSize(600,400);
        setLayout(null);
        setLocationRelativeTo(owner);

        model = new DefaultListModel<>();
        listMarket = new JList<>(model);
        JScrollPane sp = new JScrollPane(listMarket);
        sp.setBounds(20,20,540,280);
        add(sp);

        btnBuy = new JButton("Buy Player");
        btnBuy.setBounds(20,310,540,36);
        btnBuy.addActionListener(e -> buySelected());
        add(btnBuy);

        refreshMarket();
    }

    private void refreshMarket() {
        model.clear();
        for (Team t : league.teams) {
            for (Player p : t.players) {
                model.addElement(t.name + " - " + p.number + " " + p.name + " (Q" + p.quality + ") $" + (p.wage*10));
            }
        }
    }

    private void buySelected() {
        int idx = listMarket.getSelectedIndex();
        if (idx < 0) return;
        // compute which player
        int count = 0;
        for (Team t : league.teams) {
            for (Player p : t.players) {
                if (count == idx) {
                    if (team.budget < p.wage * 10) {
                        JOptionPane.showMessageDialog(this, "Not enough budget");
                        return;
                    }
                    Team seller = p.team;
                    if (seller == team) {
                        JOptionPane.showMessageDialog(this, "You already own this player");
                        return;
                    }
                    seller.players.remove(p);
                    team.buyPlayer(p);
                    JOptionPane.showMessageDialog(this, "Bought " + p.name + " from " + seller.name);
                    refreshMarket();
                    return;
                }
                count++;
            }
        }
    }
}

// Utility vector
class Vector2 {
    public double x, y;

    public Vector2(double x, double y) { this.x = x; this.y = y; }

    public Vector2 add(Vector2 o) { return new Vector2(x + o.x, y + o.y); }
    public Vector2 subtract(Vector2 o) { return new Vector2(x - o.x, y - o.y); }
    public Vector2 scale(double s) { return new Vector2(x * s, y * s); }
    public double length() { return Math.sqrt(x*x + y*y); }
    public Vector2 normalize() { double l = length(); return l==0? new Vector2(0,0): new Vector2(x/l, y/l); }

    @Override
    public String toString() { return String.format("(%.2f,%.2f)", x, y); }
}

// Utilities and random name generator
class Utils {
    private static final String[] first = {"Liam","Noah","Oliver","Ethan","Lucas","Mason","Logan","James","Aiden","Jackson","Leo","Hugo","Oscar","Mateo","Elias"};
    private static final String[] last = {"Smith","Jones","Garcia","Brown","Taylor","Anderson","Thomas","Martinez","Lee","Perez","Santos","Khan","Wang","Novak","Silva"};
    private static final Random rng = new Random();

    public static String randomName() {
        return first[rng.nextInt(first.length)] + " " + last[rng.nextInt(last.length)];
    }

    public static int randInt(int a, int b) { return a + rng.nextInt(Math.max(1, b-a+1)); }
    public static double randDouble(double a, double b) { return a + rng.nextDouble() * (b-a); }

    public static double map(double v, double a, double b, double c, double d) {
        double t = (v - a) / (b - a);
        return c + t * (d - c);
    }
}

// Small helper for generating sample data
class SampleGenerator {
    public static Team sampleTeam(String name, boolean isPlayer) {
        Team t = new Team(name);
        t.isPlayerTeam = isPlayer;
        for (int i = 1; i <= 11; i++) {
            Player p = Player.createRandomPlayer(i);
            p.team = t;
            t.players.add(p);
        }
        return t;
    }
}

// End of file
