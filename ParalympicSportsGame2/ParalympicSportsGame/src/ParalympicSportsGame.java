import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

// Main class for the Paralympic Sports Game
public class ParalympicSportsGame extends JFrame {
    private GamePanel gamePanel;
    private ControlPanel controlPanel;
    private StatsPanel statsPanel;

    public ParalympicSportsGame() {
        super("Paralympic Sports Game - Wheelchair Racing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLayout(new BorderLayout());

        gamePanel = new GamePanel();
        controlPanel = new ControlPanel(gamePanel);
        statsPanel = new StatsPanel(gamePanel);

        add(gamePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        add(statsPanel, BorderLayout.EAST);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ParalympicSportsGame());
    }
}

// Panel for displaying the game animation
class GamePanel extends JPanel {
    private List<Athlete> athletes;
    private boolean raceStarted;
    private boolean raceFinished;
    private Timer animationTimer;
    private int trackLength = 800;
    private int finishLineX = 750;
    private Random random;
    private int timeElapsed; // in tenths of seconds

    public GamePanel() {
        setBackground(new Color(200, 230, 255));
        setPreferredSize(new Dimension(800, 500));
        random = new Random();
        athletes = new ArrayList<>();
        initializeAthletes();
        timeElapsed = 0;

        animationTimer = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (raceStarted && !raceFinished) {
                    timeElapsed++;
                    updateRace();
                    repaint();
                    checkFinish();
                }
            }
        });
    }

    private void initializeAthletes() {
        String[] names = {"Alex Johnson", "Maria Garcia", "David Smith", "Emma Wilson",
                "James Brown", "Sophia Lee", "Michael Chen", "Olivia Taylor"};
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA,
                Color.ORANGE, Color.CYAN, Color.PINK, Color.DARK_GRAY};

        for (int i = 0; i < 8; i++) {
            int speed = 3 + random.nextInt(5); // Base speed between 3-7
            int stamina = 70 + random.nextInt(31); // Stamina between 70-100
            int lane = i;
            athletes.add(new Athlete(names[i], colors[i], lane, speed, stamina));
        }
    }

    public void startRace() {
        if (!raceStarted) {
            raceStarted = true;
            raceFinished = false;
            timeElapsed = 0;
            for (Athlete athlete : athletes) {
                athlete.reset();
            }
            animationTimer.start();
        }
    }

    public void pauseRace() {
        if (raceStarted && !raceFinished) {
            animationTimer.stop();
        }
    }

    public void resumeRace() {
        if (raceStarted && !raceFinished) {
            animationTimer.start();
        }
    }

    public void resetRace() {
        raceStarted = false;
        raceFinished = false;
        animationTimer.stop();
        timeElapsed = 0;
        for (Athlete athlete : athletes) {
            athlete.reset();
        }
        repaint();
    }

    private void updateRace() {
        for (Athlete athlete : athletes) {
            if (!athlete.hasFinished()) {
                // Adjust speed based on stamina
                double staminaFactor = athlete.getStamina() / 100.0;
                int currentSpeed = (int)(athlete.getBaseSpeed() * (0.7 + 0.6 * staminaFactor));

                // Random variation
                currentSpeed += random.nextInt(3) - 1;

                athlete.move(currentSpeed);

                // Decrease stamina
                athlete.reduceStamina(random.nextInt(2) + 1);
            }
        }
    }

    private void checkFinish() {
        boolean allFinished = true;
        for (Athlete athlete : athletes) {
            if (athlete.getDistance() >= trackLength && !athlete.hasFinished()) {
                athlete.setFinishTime(timeElapsed);
            }
            if (!athlete.hasFinished()) {
                allFinished = false;
            }
        }

        if (allFinished) {
            raceFinished = true;
            animationTimer.stop();

            // Sort athletes by finish time
            Collections.sort(athletes, (a1, a2) -> {
                if (a1.hasFinished() && a2.hasFinished()) {
                    return Integer.compare(a1.getFinishTime(), a2.getFinishTime());
                }
                return 0;
            });
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw track
        drawTrack(g);

        // Draw athletes
        for (Athlete athlete : athletes) {
            drawAthlete(g, athlete);
        }

        // Draw finish line
        g.setColor(Color.BLACK);
        g.fillRect(finishLineX, 50, 5, 400);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("FINISH", finishLineX - 30, 40);

        // Draw race info
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Time: " + String.format("%.1f", timeElapsed / 10.0) + "s", 20, 30);

        if (raceFinished) {
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.setColor(new Color(0, 100, 0));
            g.drawString("RACE FINISHED!", 300, 30);

            // Display podium
            g.setFont(new Font("Arial", Font.BOLD, 18));
            if (athletes.size() >= 3) {
                g.setColor(new Color(212, 175, 55)); // Gold
                g.drawString("1st: " + athletes.get(0).getName(), 300, 60);

                g.setColor(new Color(192, 192, 192)); // Silver
                g.drawString("2nd: " + athletes.get(1).getName(), 300, 85);

                g.setColor(new Color(205, 127, 50)); // Bronze
                g.drawString("3rd: " + athletes.get(2).getName(), 300, 110);
            }
        }
    }

    private void drawTrack(Graphics g) {
        // Draw track background
        g.setColor(new Color(240, 240, 240));
        g.fillRect(50, 50, trackLength, 400);

        // Draw lane markings
        g.setColor(new Color(200, 200, 200));
        for (int i = 0; i < 8; i++) {
            int y = 50 + i * 50;
            for (int x = 50; x < 50 + trackLength; x += 20) {
                g.drawLine(x, y, x + 10, y);
            }
        }

        // Draw start line
        g.setColor(Color.GRAY);
        g.fillRect(50, 50, 5, 400);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("START", 30, 40);
    }

    private void drawAthlete(Graphics g, Athlete athlete) {
        int lane = athlete.getLane();
        int distance = athlete.getDistance();
        Color color = athlete.getColor();

        int y = 75 + lane * 50;
        int x = 50 + distance * (finishLineX - 50) / trackLength;

        // Draw wheelchair
        g.setColor(color);
        g.fillOval(x - 15, y, 30, 10); // Main body
        g.fillOval(x - 5, y - 15, 10, 20); // Seat back
        g.setColor(Color.BLACK);
        g.fillOval(x - 20, y + 10, 10, 10); // Rear wheel
        g.fillOval(x + 10, y + 10, 10, 10); // Front wheel

        // Draw athlete
        g.setColor(new Color(255, 220, 177)); // Skin tone
        g.fillOval(x - 5, y - 25, 10, 10); // Head

        // Draw arms
        g.setColor(color);
        g.drawLine(x - 5, y - 15, x - 15, y - 25); // Left arm
        g.drawLine(x + 5, y - 15, x + 15, y - 25); // Right arm

        // Draw pushing motion if moving
        if (raceStarted && !athlete.hasFinished() && animationTimer.isRunning()) {
            int armAngle = (timeElapsed * 2) % 360;
            g.drawLine(x + 5, y - 15,
                    x + 15 + (int)(5 * Math.sin(Math.toRadians(armAngle))),
                    y - 25 + (int)(5 * Math.cos(Math.toRadians(armAngle))));
        }

        // Draw name
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString(athlete.getName(), x - 20, y - 35);

        // Draw stamina bar
        int staminaWidth = (int)(30 * (athlete.getStamina() / 100.0));
        g.setColor(Color.GREEN);
        g.fillRect(x - 15, y - 45, staminaWidth, 5);
        g.setColor(Color.BLACK);
        g.drawRect(x - 15, y - 45, 30, 5);
    }

    public List<Athlete> getAthletes() {
        return athletes;
    }

    public boolean isRaceStarted() {
        return raceStarted;
    }

    public boolean isRaceFinished() {
        return raceFinished;
    }

    public int getTimeElapsed() {
        return timeElapsed;
    }
}

// Class representing a Paralympic athlete
class Athlete {
    private String name;
    private Color color;
    private int lane;
    private int baseSpeed;
    private int stamina;
    private int distance;
    private boolean finished;
    private int finishTime;

    public Athlete(String name, Color color, int lane, int baseSpeed, int stamina) {
        this.name = name;
        this.color = color;
        this.lane = lane;
        this.baseSpeed = baseSpeed;
        this.stamina = stamina;
        this.distance = 0;
        this.finished = false;
        this.finishTime = 0;
    }

    public void move(int speed) {
        if (!finished) {
            distance += speed;
        }
    }

    public void reduceStamina(int amount) {
        stamina = Math.max(0, stamina - amount);
        if (stamina == 0) {
            baseSpeed = Math.max(1, baseSpeed - 2);
        }
    }

    public void reset() {
        distance = 0;
        finished = false;
        finishTime = 0;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public int getLane() {
        return lane;
    }

    public int getBaseSpeed() {
        return baseSpeed;
    }

    public int getStamina() {
        return stamina;
    }

    public int getDistance() {
        return distance;
    }

    public boolean hasFinished() {
        return finished;
    }

    public int getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(int finishTime) {
        this.finishTime = finishTime;
        this.finished = true;
    }
}

// Panel for displaying race statistics
class StatsPanel extends JPanel {
    private GamePanel gamePanel;
    private JTextArea statsArea;

    public StatsPanel(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        setPreferredSize(new Dimension(200, 500));
        setBackground(new Color(240, 240, 240));
        setLayout(new BorderLayout());

        statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statsArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(statsArea);
        add(scrollPane, BorderLayout.CENTER);

        // Timer to update stats periodically
        Timer statsTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStats();
            }
        });
        statsTimer.start();
    }

    private void updateStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("PARALYMPIC RACE\n");
        sb.append("===============\n\n");

        List<Athlete> athletes = gamePanel.getAthletes();

        if (gamePanel.isRaceFinished()) {
            sb.append("FINAL RESULTS:\n\n");
            for (int i = 0; i < athletes.size(); i++) {
                Athlete a = athletes.get(i);
                sb.append(String.format("%d. %-15s %6.1fs\n",
                        i + 1,
                        a.getName(),
                        a.getFinishTime() / 10.0));
            }
        } else {
            sb.append(String.format("Time: %.1fs\n\n", gamePanel.getTimeElapsed() / 10.0));
            sb.append("CURRENT STANDINGS:\n\n");

            // Create a sorted copy for display
            List<Athlete> sortedAthletes = new ArrayList<>(athletes);
            Collections.sort(sortedAthletes, (a1, a2) ->
                    Integer.compare(a2.getDistance(), a1.getDistance()));

            for (int i = 0; i < sortedAthletes.size(); i++) {
                Athlete a = sortedAthletes.get(i);
                int progress = (int)((a.getDistance() / 800.0) * 100);
                sb.append(String.format("%d. %-15s %3d%%\n",
                        i + 1,
                        a.getName(),
                        Math.min(100, progress)));
            }

            sb.append("\nATHLETE STATS:\n\n");
            for (Athlete a : athletes) {
                sb.append(String.format("%-15s S:%-2d ST:%-3d\n",
                        a.getName(),
                        a.getBaseSpeed(),
                        a.getStamina()));
            }
        }

        statsArea.setText(sb.toString());
    }
}

// Panel for game controls
class ControlPanel extends JPanel {
    private GamePanel gamePanel;
    private JButton startButton;
    private JButton pauseButton;
    private JButton resumeButton;
    private JButton resetButton;

    public ControlPanel(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        setPreferredSize(new Dimension(1000, 60));
        setBackground(new Color(220, 220, 220));

        startButton = new JButton("Start Race");
        pauseButton = new JButton("Pause");
        resumeButton = new JButton("Resume");
        resetButton = new JButton("Reset");

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gamePanel.startRace();
                updateButtons();
            }
        });

        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gamePanel.pauseRace();
                updateButtons();
            }
        });

        resumeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gamePanel.resumeRace();
                updateButtons();
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gamePanel.resetRace();
                updateButtons();
            }
        });

        add(startButton);
        add(pauseButton);
        add(resumeButton);
        add(resetButton);

        updateButtons();
    }

    private void updateButtons() {
        boolean raceStarted = gamePanel.isRaceStarted();
        boolean raceFinished = gamePanel.isRaceFinished();

        startButton.setEnabled(!raceStarted);
        pauseButton.setEnabled(raceStarted && !raceFinished);
        resumeButton.setEnabled(raceStarted && !raceFinished);
        resetButton.setEnabled(raceStarted || raceFinished);
    }
}