import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

// Main Game Frame
public class GolfResortManager extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GolfResortManager().setVisible(true);
        });
    }

    private GolfPanel golfPanel;
    private Resort resort;

    public GolfResortManager() {
        setTitle("Golf Resort Manager");
        setSize(950, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        resort = new Resort();
        golfPanel = new GolfPanel(resort);
        add(golfPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        JButton buildHole = new JButton("Build Hole");
        JButton addGolfer = new JButton("Add Golfer");
        JButton advanceDay = new JButton("Advance Day");
        JButton upgrade = new JButton("Upgrade Facility");

        JLabel budgetLbl = new JLabel("Budget: $" + resort.getBudget());
        JLabel popularityLbl = new JLabel("Popularity: " + resort.getPopularity());

        buildHole.addActionListener(e -> {
            if (resort.buildHole()) {
                budgetLbl.setText("Budget: $" + resort.getBudget());
                popularityLbl.setText("Popularity: " + resort.getPopularity());
                golfPanel.repaint();
            }
        });
        addGolfer.addActionListener(e -> {
            if (resort.addGolfer()) {
                popularityLbl.setText("Popularity: " + resort.getPopularity());
                golfPanel.repaint();
            }
        });
        advanceDay.addActionListener(e -> {
            resort.advanceDay();
            budgetLbl.setText("Budget: $" + resort.getBudget());
            popularityLbl.setText("Popularity: " + resort.getPopularity());
            golfPanel.repaint();
        });
        upgrade.addActionListener(e -> {
            if (resort.upgrade()) {
                budgetLbl.setText("Budget: $" + resort.getBudget());
                popularityLbl.setText("Popularity: " + resort.getPopularity());
                golfPanel.repaint();
            }
        });

        controlPanel.add(buildHole);
        controlPanel.add(addGolfer);
        controlPanel.add(upgrade);
        controlPanel.add(advanceDay);
        controlPanel.add(budgetLbl);
        controlPanel.add(popularityLbl);
        add(controlPanel, BorderLayout.SOUTH);
    }
}

// Main Drawing & Animation Panel
class GolfPanel extends JPanel implements ActionListener {
    private Resort resort;
    private Timer timer;

    public GolfPanel(Resort resort) {
        this.resort = resort;
        setBackground(new Color(117, 187, 76));
        timer = new Timer(40, this);
        timer.start();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawHoles(g);
        drawGolfers(g);
        drawFacilities(g);
        drawInfo(g);
    }

    private void drawHoles(Graphics g) {
        for (Hole h : resort.getHoles()) {
            h.draw(g);
        }
    }

    private void drawGolfers(Graphics g) {
        for (Golfer p : resort.getGolfers()) {
            p.draw(g, resort);
        }
    }

    private void drawFacilities(Graphics g) {
        g.setColor(new Color(100, 70, 30));
        g.fillRect(20, 40, 80, 80); // Clubhouse
        g.setColor(Color.WHITE);
        g.drawString("Clubhouse", 25, 55);

        if (resort.upgradeLevel > 0) {
            g.setColor(new Color(180, 180, 250));
            g.fillRect(20, 130, 80, 30); // Upgraded Facility
            g.setColor(Color.BLACK);
            g.drawString("Restaurant", 28, 150);
        }
    }

    private void drawInfo(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Holes: " + resort.getHoles().size(), 10, getHeight() - 60);
        g.drawString("Golfers: " + resort.getGolfers().size(), 10, getHeight() - 40);
        g.drawString("Day: " + resort.day, 10, getHeight() - 20);
    }

    public void actionPerformed(ActionEvent e) {
        resort.animateGolfers();
        repaint();
    }
}

// Resort Logic
class Resort {
    private int budget = 5000;
    private int holeCost = 1000;
    private int golferAddCost = 150;
    private int upgradeCost = 3000;
    int upgradeLevel = 0;
    private int popularity = 1;
    public int day = 1;

    private List<Hole> holes;
    private List<Golfer> golfers;

    public Resort() {
        holes = new ArrayList<>();
        golfers = new ArrayList<>();
        // Start with one hole and one golfer
        buildHole();
        addGolfer();
    }

    public boolean buildHole() {
        if (budget >= holeCost && holes.size() < 9) {
            int x = 150 + new Random().nextInt(700);
            int y = 70 + new Random().nextInt(500);
            holes.add(new Hole(x, y, holes.size() + 1));
            budget -= holeCost;
            popularity += 2;
            return true;
        }
        return false;
    }

    public boolean addGolfer() {
        if (budget >= golferAddCost && golfers.size() < holes.size() * 2) {
            golfers.add(new Golfer("Golfer" + (golfers.size() + 1)));
            budget -= golferAddCost;
            popularity += 1;
            return true;
        }
        return false;
    }

    public boolean upgrade() {
        if (budget >= upgradeCost && upgradeLevel < 2) {
            upgradeLevel++;
            budget -= upgradeCost;
            popularity += 4;
            return true;
        }
        return false;
    }

    public void advanceDay() {
        day++;
        int income = 200 * golfers.size() + 100 * upgradeLevel;
        if (holes.isEmpty()) income = 0;
        budget += income;
        if (popularity > 100) popularity -= 3;
    }

    public void animateGolfers() {
        for (Golfer g : golfers) {
            g.animate(this);
        }
    }

    public List<Hole> getHoles() { return holes; }
    public List<Golfer> getGolfers() { return golfers; }
    public int getBudget() { return budget; }
    public int getPopularity() { return popularity; }
}

// Golf Hole
class Hole {
    int x, y, number;

    public Hole(int x, int y, int number) {
        this.x = x;
        this.y = y;
        this.number = number;
    }

    public void draw(Graphics g) {
        g.setColor(new Color(89, 191, 76));
        g.fillOval(x-40, y-35, 85, 75);
        g.setColor(new Color(70, 110, 25));
        g.fillOval(x-15, y-10, 30, 25);
        g.setColor(Color.WHITE);
        g.drawString("Hole " + number, x-15, y-18);

        // Draw the flag
        g.setColor(Color.RED);
        g.drawLine(x, y - 20, x, y - 50);
        g.setColor(Color.WHITE);
        Polygon flag = new Polygon();
        flag.addPoint(x, y - 50);
        flag.addPoint(x + 18, y - 45);
        flag.addPoint(x, y - 37);
        g.fillPolygon(flag);
    }

    public Point getTeeLocation() {
        return new Point(x + 10, y + 10);
    }
    public Point getHoleLocation() {
        return new Point(x, y - 10);
    }
}

// Golfer with animation
class Golfer {
    String name;
    int currentHole = 0;
    double posX, posY;
    boolean movingToTee = true;
    boolean onCourse = false;
    int animStep = 0;

    public Golfer(String name) {
        this.name = name;
        this.posX = 70;
        this.posY = 90 + Math.random()*50;
        this.onCourse = false;
    }

    public void draw(Graphics g, Resort resort) {
        // Draw body
        g.setColor(new Color(250, 215, 138));
        g.fillOval((int)posX-8, (int)posY-18, 16, 16); // Head
        g.setColor(Color.BLUE);
        g.fillRect((int)posX-7, (int)posY-2, 15, 20); // Body

        // Name label
        g.setColor(Color.BLACK);
        g.drawString(name, (int)posX-10, (int)posY-25);

        if (onCourse && resort.getHoles().size() > currentHole) {
            g.setColor(new Color(50, 180, 40, 90));
            g.fillOval((int)posX-10, (int)posY+15, 22, 7);
        }
    }

    public void animate(Resort resort) {
        List<Hole> holes = resort.getHoles();
        if (!onCourse) {
            // Walk to first tee
            if (!holes.isEmpty()) {
                Point tee = holes.get(0).getTeeLocation();
                moveToward(tee);
                if (distanceTo(tee) < 6) {
                    onCourse = true;
                    currentHole = 0;
                    animStep = 0;
                }
            }
            return;
        }
        if (currentHole < holes.size()) {
            Hole cur = holes.get(currentHole);
            Point from = (animStep % 2 == 0) ? cur.getTeeLocation() : cur.getHoleLocation();
            Point to = (animStep % 2 == 0) ? cur.getHoleLocation() : cur.getTeeLocation();
            moveToward(to);
            if (distanceTo(to) < 5) {
                animStep++;
                if (animStep >= 2) {
                    currentHole++;
                    animStep = 0;
                }
            }
        } else {
            // Finished course: return to clubhouse
            moveToward(new Point(70, 90));
            if (distanceTo(new Point(70, 90)) < 4) {
                onCourse = false;
                currentHole = 0;
            }
        }
    }

    private void moveToward(Point dest) {
        double dx = dest.x - posX;
        double dy = dest.y - posY;
        double dist = Math.sqrt(dx*dx + dy*dy);
        if (dist > 1) {
            posX += dx / dist * 2.2;
            posY += dy / dist * 2.2;
        }
    }

    private double distanceTo(Point p) {
        double dx = p.x - posX;
        double dy = p.y - posY;
        return Math.sqrt(dx*dx + dy*dy);
    }
}
