import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class GolfResortManager {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}

/* --------------------------------------------------------------------------
   GameFrame - main window
   -------------------------------------------------------------------------- */
class GameFrame extends JFrame {
    private GamePanel gamePanel;
    private ControlPanel controlPanel;

    public GameFrame() {
        setTitle("Golf Resort Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        GameState state = new GameState();
        gamePanel = new GamePanel(state);
        controlPanel = new ControlPanel(state, gamePanel);

        add(gamePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);
    }
}

class GameState implements Serializable {
    Course course;
    List<Player> players = new ArrayList<>();
    int currentHoleIndex = 0;
    int day = 1;
    int money = 50000; // starting capital
    WeatherSystem weather;
    transient Random rand = new Random();

    public GameState() {
        course = CourseGenerator.generateSampleCourse();
        players.add(new Player("You", true));
        players.add(new Player("AI - Riviera", false));
        players.add(new Player("AI - Links", false));
        weather = new WeatherSystem();
    }

    public Hole getCurrentHole() {
        return course.holes.get(currentHoleIndex);
    }

    public void nextHole() {
        currentHoleIndex++;
        if (currentHoleIndex >= course.holes.size()) {
            currentHoleIndex = 0;
            day++;
            // simulate resort income
            money += 2000 + rand.nextInt(3000);
            weather.advanceDay();
        }
        // reset players for next hole
        for (Player p : players) {
            p.resetForHole(getCurrentHole());
        }
    }
}

class Course implements Serializable {
    String name;
    List<Hole> holes = new ArrayList<>();
    Dimension size = new Dimension(1800, 1200); // virtual size

    public Course(String name) {
        this.name = name;
    }
}

class Hole implements Serializable {
    int number;
    int par;
    Point2D tee;
    Point2D hole;
    List<Obstacle> obstacles = new ArrayList<>();
    double length; // meters

    public Hole(int number, int par, Point2D tee, Point2D hole) {
        this.number = number;
        this.par = par;
        this.tee = tee;
        this.hole = hole;
        this.length = tee.distance(hole);
    }
}

abstract class Obstacle implements Serializable {
    abstract boolean contains(Point2D p);
}

class SandTrap extends Obstacle {
    Ellipse2D shape;

    public SandTrap(double x, double y, double w, double h) {
        shape = new Ellipse2D.Double(x, y, w, h);
    }

    @Override
    boolean contains(Point2D p) {
        return shape.contains(p);
    }
}

class WaterHazard extends Obstacle {
    Shape shape;

    public WaterHazard(Shape s) {
        this.shape = s;
    }

    @Override
    boolean contains(Point2D p) {
        return shape.contains(p);
    }
}

class CourseGenerator {
    public static Course generateSampleCourse() {
        Course c = new Course("Emerald Pines Resort");
        // create 9 holes for demo
        for (int i = 1; i <= 9; i++) {
            int par = (i % 3 == 0) ? 5 : 4;
            double tx = 100 + i * 150;
            double ty = 100 + (i % 2) * 200;
            double hx = tx + 300 + (i * 20);
            double hy = ty + (i % 4) * 50;
            Hole h = new Hole(i, par, new Point2D.Double(tx, ty), new Point2D.Double(hx, hy));
            // add obstacles
            if (i % 4 == 0) {
                h.obstacles.add(new SandTrap(tx + 60, ty + 30, 100, 60));
            }
            if (i % 5 == 0) {
                Shape w = new Rectangle2D.Double(tx + 120, ty - 40, 80, 120);
                h.obstacles.add(new WaterHazard(w));
            }
            c.holes.add(h);
        }
        return c;
    }
}

/* --------------------------------------------------------------------------
   Player and caddy
   -------------------------------------------------------------------------- */
class Player implements Serializable {
    String name;
    boolean human;
    int score = 0;
    int strokesOnHole = 0;
    GolfBall ball;
    transient Caddy caddy;
    transient boolean finishedHole = false;

    public Player(String name, boolean human) {
        this.name = name;
        this.human = human;
        this.ball = new GolfBall();
        this.caddy = new Caddy();
    }

    public void resetForHole(Hole hole) {
        strokesOnHole = 0;
        finishedHole = false;
        ball.resetAt(hole.tee);
    }
}

class Caddy {
    // simple helper to recommend clubs
    public String recommendClub(double distance) {
        if (distance > 220) return "Driver";
        if (distance > 170) return "3-Wood";
        if (distance > 140) return "5-Wood";
        if (distance > 100) return "7-Iron";
        if (distance > 60) return "9-Iron";
        return "Pitching Wedge";
    }
}

/* --------------------------------------------------------------------------
   GolfBall and physics
   -------------------------------------------------------------------------- */
class GolfBall implements Serializable {
    transient Point2D position;
    transient Vector2D velocity;
    boolean inHole = false;

    public GolfBall() {
        position = new Point2D.Double(0, 0);
        velocity = new Vector2D(0, 0);
    }

    public void resetAt(Point2D start) {
        position = (Point2D) ((Point2D) start).clone();
        velocity = new Vector2D(0, 0);
        inHole = false;
    }

    public void applyStroke(double angleDeg, double speed) {
        double rad = Math.toRadians(angleDeg);
        double vx = Math.cos(rad) * speed;
        double vy = Math.sin(rad) * speed;
        this.velocity = new Vector2D(vx, vy);
    }

    public void update(double friction, double windX, double windY, double dt) {
        if (inHole) return;
        // wind affects velocity slightly
        velocity.x += windX * dt * 0.2;
        velocity.y += windY * dt * 0.2;
        // apply friction
        velocity.x *= Math.pow(1 - friction, dt * 60);
        velocity.y *= Math.pow(1 - friction, dt * 60);
        position.setLocation(position.getX() + velocity.x * dt, position.getY() + velocity.y * dt);

        // if very slow, stop
        if (velocity.length() < 0.01) {
            velocity.x = 0;
            velocity.y = 0;
        }
    }
}

class Vector2D implements Serializable {
    double x, y;

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }
}

/* --------------------------------------------------------------------------
   WeatherSystem - affects ball movement and visuals
   -------------------------------------------------------------------------- */
class WeatherSystem implements Serializable {
    enum Condition { SUNNY, CLOUDY, WINDY, RAINY }

    Condition condition = Condition.SUNNY;
    double windX = 0;
    double windY = 0;
    transient Random rand = new Random();

    public WeatherSystem() {
        randomize();
    }

    public void randomize() {
        int r = rand.nextInt(100);
        if (r < 60) {
            condition = Condition.SUNNY;
            windX = (rand.nextDouble() - 0.5) * 20;
            windY = (rand.nextDouble() - 0.5) * 20;
        } else if (r < 80) {
            condition = Condition.CLOUDY;
            windX = (rand.nextDouble() - 0.5) * 10;
            windY = (rand.nextDouble() - 0.5) * 10;
        } else if (r < 95) {
            condition = Condition.WINDY;
            windX = (rand.nextDouble() - 0.5) * 40;
            windY = (rand.nextDouble() - 0.5) * 40;
        } else {
            condition = Condition.RAINY;
            windX = (rand.nextDouble() - 0.5) * 25;
            windY = (rand.nextDouble() - 0.5) * 25;
        }
    }

    public void advanceDay() {
        randomize();
    }
}

/* --------------------------------------------------------------------------
   GamePanel - main rendering and animation loop
   -------------------------------------------------------------------------- */
class GamePanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener, KeyListener {
    GameState state;
    javax.swing.Timer timer;
    long lastTime;
    double accumulator = 0;
    Player activePlayer;
    double aimAngle = 0; // degrees
    double aimPower = 0; // 0..100
    boolean aiming = false;
    Point mousePos = new Point(0, 0);
    boolean showTrajectory = true;
    Font uiFont = new Font("SansSerif", Font.PLAIN, 12);

    public GamePanel(GameState state) {
        this.state = state;
        this.setBackground(new Color(95, 170, 85));
        this.setPreferredSize(new Dimension(1000, 800));
        setFocusable(true);
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);

        timer = new javax.swing.Timer(16, this); // ~60 FPS
        lastTime = System.nanoTime();
        timer.start();

        // assign active player to human player
        for (Player p : state.players) {
            if (p.human) {
                activePlayer = p;
                break;
            }
        }
        // set ball to correct tee position
        activePlayer.resetForHole(state.getCurrentHole());
        for (Player p : state.players) {
            if (!p.human) p.resetForHole(state.getCurrentHole());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHints(rh);

        // draw course background
        drawCourse(g2);

        // draw players' balls
        for (Player p : state.players) {
            drawBall(g2, p);
        }

        // draw UI overlay
        drawUI(g2);

        g2.dispose();
    }

    private void drawCourse(Graphics2D g2) {
        Hole h = state.getCurrentHole();
        // translate coordinate system to center view around hole
        double scale = computeScale();
        AffineTransform old = g2.getTransform();
        // center around tee and hole
        double cx = (h.tee.getX() + h.hole.getX()) / 2;
        double cy = (h.tee.getY() + h.hole.getY()) / 2;
        double tx = getWidth() / 2 - cx * scale;
        double ty = getHeight() / 2 - cy * scale;
        g2.translate(tx, ty);
        g2.scale(scale, scale);

        // draw fairway
        g2.setColor(new Color(80, 140, 60));
        Shape fairway = new RoundRectangle2D.Double(h.tee.getX() - 100, h.tee.getY() - 80, 600, 160, 120, 120);
        g2.fill(fairway);

        // draw tee and hole
        g2.setColor(new Color(60, 40, 20));
        g2.fill(new Ellipse2D.Double(h.tee.getX() - 6, h.tee.getY() - 6, 12, 12));
        g2.setColor(Color.YELLOW);
        g2.fill(new Ellipse2D.Double(h.hole.getX() - 5, h.hole.getY() - 5, 10, 10));

        // draw obstacles
        for (Obstacle o : h.obstacles) {
            if (o instanceof SandTrap) {
                SandTrap s = (SandTrap) o;
                g2.setColor(new Color(225, 210, 170));
                g2.fill(s.shape);
                g2.setColor(new Color(200, 180, 140));
                g2.draw(s.shape);
            } else if (o instanceof WaterHazard) {
                WaterHazard w = (WaterHazard) o;
                g2.setColor(new Color(60, 120, 200));
                g2.fill(w.shape);
            }
        }

        // draw trees and environment
        drawTrees(g2, h);

        g2.setTransform(old);
    }

    private double computeScale() {
        // adapt virtual course coords to panel size
        Dimension v = state.course.size;
        double sx = getWidth() / (double) v.width;
        double sy = getHeight() / (double) v.height;
        return Math.min(sx, sy) * 0.9;
    }

    private void drawTrees(Graphics2D g2, Hole h) {
        g2.setStroke(new BasicStroke(1f));
        for (int i = 0; i < 20; i++) {
            double x = h.tee.getX() + (i * 40) - 100;
            double y = h.tee.getY() - 120 + ((i % 3) * 40);
            g2.setColor(new Color(40, 100, 40));
            g2.fill(new Ellipse2D.Double(x, y, 30, 30));
            g2.setColor(new Color(20, 60, 20));
            g2.draw(new Ellipse2D.Double(x, y, 30, 30));
        }
    }

    private void drawBall(Graphics2D g2, Player p) {
        Hole h = state.getCurrentHole();
        double scale = computeScale();
        AffineTransform old = g2.getTransform();
        double cx = (h.tee.getX() + h.hole.getX()) / 2;
        double cy = (h.tee.getY() + h.hole.getY()) / 2;
        double tx = getWidth() / 2 - cx * scale;
        double ty = getHeight() / 2 - cy * scale;
        g2.translate(tx, ty);
        g2.scale(scale, scale);

        Point2D pos = p.ball.position;
        if (pos == null) pos = h.tee;

        // ball shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fill(new Ellipse2D.Double(pos.getX() - 5, pos.getY() - 3, 10, 4));

        // ball
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Double(pos.getX() - 4, pos.getY() - 4, 8, 8));
        g2.setColor(Color.GRAY);
        g2.draw(new Ellipse2D.Double(pos.getX() - 4, pos.getY() - 4, 8, 8));

        // if active player and aiming, draw aim
        if (p == activePlayer && aiming) {
            drawAim(g2, p);
        }

        g2.setTransform(old);
    }

    private void drawAim(Graphics2D g2, Player p) {
        Hole h = state.getCurrentHole();
        double scale = computeScale();
        Point2D pos = p.ball.position;
        if (pos == null) pos = h.tee;

        // draw line
        double len = 200;
        double rad = Math.toRadians(aimAngle);
        double ex = pos.getX() + Math.cos(rad) * (len / scale);
        double ey = pos.getY() + Math.sin(rad) * (len / scale);
        Stroke oldS = g2.getStroke();
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(255, 255, 255, 160));
        g2.draw(new Line2D.Double(pos.getX(), pos.getY(), ex, ey));
        g2.setStroke(oldS);

        if (showTrajectory) {
            // simple projected path
            double power = aimPower / 100.0 * 25; // scaled
            double vx = Math.cos(rad) * power;
            double vy = Math.sin(rad) * power;
            Point2D tpos = (Point2D) ((Point2D) pos).clone();
            for (int i = 0; i < 60; i++) {
                vx *= 0.98;
                vy *= 0.98;
                tpos.setLocation(tpos.getX() + vx, tpos.getY() + vy);
                g2.fill(new Ellipse2D.Double(tpos.getX() - 1.2, tpos.getY() - 1.2, 2.4, 2.4));
            }
        }
    }

    private void drawUI(Graphics2D g2) {
        g2.setFont(uiFont);
        int x = 10;
        int y = 20;
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(6, 6, 260, 120, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawString("Course: " + state.course.name, x, y);
        g2.drawString("Hole: " + state.getCurrentHole().number + "  Par: " + state.getCurrentHole().par, x, y + 18);
        g2.drawString("Day: " + state.day + "  Money: $" + state.money, x, y + 36);
        g2.drawString("Weather: " + state.weather.condition + "  Wind: (" + (int) state.weather.windX + ", " + (int) state.weather.windY + ")", x, y + 54);
        g2.drawString("Active Player: " + activePlayer.name, x, y + 72);
        g2.drawString("Strokes on hole: " + activePlayer.strokesOnHole, x, y + 90);

        // controls hint
        g2.setColor(new Color(255, 255, 255, 200));
        g2.drawString("Click+drag to aim. Release to hit. Space to end hole. T toggle trajectory.", 10, getHeight() - 12);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - lastTime) / 1e9;
        lastTime = now;
        updatePhysics(dt);
        repaint();
    }

    private void updatePhysics(double dt) {
        // update active player ball
        double friction = 0.02;
        double windX = state.weather.windX / 50.0;
        double windY = state.weather.windY / 50.0;

        // update AI players in a simple turn-based manner
        for (Player p : state.players) {
            if (!p.human && !p.finishedHole) {
                aiPlay(p, dt);
            }

            p.ball.update(friction, windX, windY, dt);
            checkCollisions(p);

            // check if in hole
            Hole h = state.getCurrentHole();
            if (p.ball.position.distance(h.hole) < 6) {
                if (!p.finishedHole) {
                    p.finishedHole = true;
                    p.score += p.strokesOnHole;
                    // reward
                    state.money += 50;
                }
            }
        }

        // check if all players finished the hole
        boolean allFinished = true;
        for (Player p : state.players) {
            if (!p.finishedHole) { allFinished = false; break; }
        }
        if (allFinished) {
            // advance hole after a brief pause
            // we will advance immediately to keep things simple
            state.nextHole();
            // reset players' active ball positions
            for (Player p : state.players) p.resetForHole(state.getCurrentHole());
        }
    }

    private void aiPlay(Player p, double dt) {
        // basic AI: take a shot toward the hole with some randomness
        if (p.strokesOnHole > 0 && p.ball.velocity.length() > 0.1) return; // ball still moving
        if (p.finishedHole) return;
        Hole h = state.getCurrentHole();
        Point2D from = p.ball.position;
        if (from == null) from = h.tee;
        double dx = h.hole.getX() - from.getX();
        double dy = h.hole.getY() - from.getY();
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        double dist = from.distance(h.hole);
        double power = Math.min(100, dist / 6 + (state.rand.nextDouble() - 0.5) * 10);
        // simulate delay
        if (state.rand.nextDouble() < 0.02) {
            p.ball.applyStroke(angle, power / 4);
            p.strokesOnHole++;
        }
    }

    private void checkCollisions(Player p) {
        Hole h = state.getCurrentHole();
        Point2D pos = p.ball.position;
        for (Obstacle o : h.obstacles) {
            if (o.contains(pos)) {
                // simple response depending on type
                if (o instanceof SandTrap) {
                    // reduce speed dramatically
                    p.ball.velocity.x *= 0.3;
                    p.ball.velocity.y *= 0.3;
                } else if (o instanceof WaterHazard) {
                    // reset ball to tee, penalty stroke
                    p.strokesOnHole += 1;
                    p.ball.resetAt(h.tee);
                }
            }
        }
        // ensure ball stays within course bounds
        double minX = 0, minY = 0, maxX = state.course.size.width, maxY = state.course.size.height;
        double bx = pos.getX();
        double by = pos.getY();
        if (bx < minX) { pos.setLocation(minX + 2, by); p.ball.velocity.x *= -0.3; }
        if (by < minY) { pos.setLocation(bx, minY + 2); p.ball.velocity.y *= -0.3; }
        if (bx > maxX) { pos.setLocation(maxX - 2, by); p.ball.velocity.x *= -0.3; }
        if (by > maxY) { pos.setLocation(bx, maxY - 2); p.ball.velocity.y *= -0.3; }
    }

    // Mouse and keyboard events for aiming and hitting
    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if (activePlayer == null) return;
        aiming = true;
        mousePos = e.getPoint();
        updateAimFromMouse();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!aiming || activePlayer == null) return;
        aiming = false;
        // commit stroke
        double finalAngle = aimAngle;
        double finalPower = aimPower;
        // apply stroke
        activePlayer.ball.applyStroke(finalAngle, finalPower / 4.0);
        activePlayer.strokesOnHole++;
    }

    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {
        mousePos = e.getPoint();
        updateAimFromMouse();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mousePos = e.getPoint();
    }

    private void updateAimFromMouse() {
        Hole h = state.getCurrentHole();
        double scale = computeScale();
        double cx = (h.tee.getX() + h.hole.getX()) / 2;
        double cy = (h.tee.getY() + h.hole.getY()) / 2;
        double tx = getWidth() / 2 - cx * scale;
        double ty = getHeight() / 2 - cy * scale;
        // transform mouse coords back to world coords
        double wx = (mousePos.x - tx) / scale;
        double wy = (mousePos.y - ty) / scale;
        Point2D pos = activePlayer.ball.position;
        if (pos == null) pos = h.tee;
        double dx = wx - pos.getX();
        double dy = wy - pos.getY();
        aimAngle = Math.toDegrees(Math.atan2(dy, dx));
        double dist = Math.hypot(dx, dy);
        aimPower = Math.min(100, dist / 3);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            // end hole (for testing)
            for (Player p : state.players) p.finishedHole = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_T) {
            showTrajectory = !showTrajectory;
        }
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            // switch active player
            int idx = state.players.indexOf(activePlayer);
            idx = (idx + 1) % state.players.size();
            activePlayer = state.players.get(idx);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}

/* --------------------------------------------------------------------------
   ControlPanel - side panel for management: hire staff, build, save/load
   -------------------------------------------------------------------------- */
class ControlPanel extends JPanel {
    GameState state;
    GamePanel panel;

    public ControlPanel(GameState state, GamePanel panel) {
        this.state = state;
        this.panel = panel;
        setPreferredSize(new Dimension(260, 800));
        setLayout(new BorderLayout());
        add(createTop(), BorderLayout.NORTH);
        add(createMiddle(), BorderLayout.CENTER);
        add(createBottom(), BorderLayout.SOUTH);
    }

    private JPanel createTop() {
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(0, 1));
        p.setBorder(BorderFactory.createTitledBorder("Resort Manager"));
        JLabel money = new JLabel("Money: $" + state.money);
        p.add(money);
        JLabel day = new JLabel("Day: " + state.day);
        p.add(day);
        // update labels periodically
        Timer t = new Timer(1000, e -> {
            money.setText("Money: $" + state.money);
            day.setText("Day: " + state.day);
        });
        t.start();
        return p;
    }

    private JPanel createMiddle() {
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(0, 1));
        p.setBorder(BorderFactory.createTitledBorder("Management Actions"));

        JButton hire = new JButton("Hire Caddy ($200)");
        hire.addActionListener(e -> {
            if (state.money >= 200) {
                state.money -= 200;
                // hire adds a caddy effect: reduce friction slightly
                for (Player pl : state.players) {
                    pl.caddy = new Caddy();
                }
                JOptionPane.showMessageDialog(this, "Hired additional caddy staff!");
            } else {
                JOptionPane.showMessageDialog(this, "Not enough money.");
            }
        });
        p.add(hire);

        JButton upgrade = new JButton("Upgrade Green ($5000)");
        upgrade.addActionListener(e -> {
            if (state.money >= 5000) {
                state.money -= 5000;
                // smoother greens -> less friction
                JOptionPane.showMessageDialog(this, "Upgraded greens - faster roll! (visual)");
            } else {
                JOptionPane.showMessageDialog(this, "Not enough money.");
            }
        });
        p.add(upgrade);

        JButton changeWeather = new JButton("Simulate Weather Change");
        changeWeather.addActionListener(e -> {
            state.weather.randomize();
            JOptionPane.showMessageDialog(this, "Weather changed to " + state.weather.condition);
        });
        p.add(changeWeather);

        JButton save = new JButton("Save Game");
        save.addActionListener(e -> {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("golf_save.dat"))) {
                oos.writeObject(state);
                JOptionPane.showMessageDialog(this, "Game saved.");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
            }
        });
        p.add(save);

        JButton load = new JButton("Load Game");
        load.addActionListener(e -> {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("golf_save.dat"))) {
                GameState loaded = (GameState) ois.readObject();
                // reattach transient fields
                loaded.rand = new Random();
                loaded.weather.rand = new Random();
                for (Player pl : loaded.players) {
                    pl.caddy = new Caddy();
                    pl.ball = new GolfBall();
                    pl.resetForHole(loaded.getCurrentHole());
                }
                panel.state = loaded;
                this.state = loaded;
                JOptionPane.showMessageDialog(this, "Game loaded.");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage());
            }
        });
        p.add(load);

        return p;
    }

    private JPanel createBottom() {
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(0, 1));
        p.setBorder(BorderFactory.createTitledBorder("Players & Scores"));

        JTextArea scoreArea = new JTextArea(10, 20);
        scoreArea.setEditable(false);
        Timer t = new Timer(700, e -> {
            StringBuilder sb = new StringBuilder();
            for (Player pl : state.players) {
                sb.append(pl.name).append(" - Score: ").append(pl.score).append("\n");
            }
            scoreArea.setText(sb.toString());
        });
        t.start();
        p.add(new JScrollPane(scoreArea));

        return p;
    }
}

class Utils {
    public static double clamp(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }

    public static String money(int v) {
        return "$" + v;
    }
}

