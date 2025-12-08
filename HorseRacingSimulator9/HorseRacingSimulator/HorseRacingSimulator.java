import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.border.*;

public class HorseRacingSimulator extends JFrame {
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int TRACK_LENGTH = 1000;
    private static final int NUM_LANES = 6;
    private static final int LANE_HEIGHT = 80;
    private static final int START_X = 50;

    private RacePanel racePanel;
    private ControlPanel controlPanel;
    private StatsPanel statsPanel;
    private List<Horse> horses;
    private Timer raceTimer;
    private boolean raceInProgress;
    private int raceNumber;
    private Random random;
    private List<RaceResult> raceHistory;

    public HorseRacingSimulator() {
        setTitle("Horse Racing Simulator - Professional Edition");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        random = new Random();
        raceNumber = 1;
        raceHistory = new ArrayList<>();

        initializeHorses();
        initializeComponents();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeHorses() {
        horses = new ArrayList<>();
        String[] horseNames = {
            "Thunder Bolt", "Lightning Storm", "Midnight Runner",
            "Golden Arrow", "Silver Streak", "Blazing Phoenix"
        };
        Color[] horseColors = {
            new Color(139, 69, 19), new Color(101, 67, 33),
            new Color(0, 0, 0), new Color(184, 134, 11),
            new Color(192, 192, 192), new Color(178, 34, 34)
        };

        for (int i = 0; i < NUM_LANES; i++) {
            horses.add(new Horse(horseNames[i], horseColors[i], i));
        }
    }

    private void initializeComponents() {
        racePanel = new RacePanel();
        controlPanel = new ControlPanel();
        statsPanel = new StatsPanel();

        add(racePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        add(statsPanel, BorderLayout.EAST);

        raceTimer = new Timer(50, e -> updateRace());
    }

    private void startRace() {
        if (raceInProgress) return;

        raceInProgress = true;
        for (Horse horse : horses) {
            horse.reset();
        }

        controlPanel.setRaceInProgress(true);
        raceTimer.start();
    }

    private void updateRace() {
        boolean raceFinished = false;

        for (Horse horse : horses) {
            if (!horse.isFinished()) {
                horse.move();
                if (horse.getPosition() >= TRACK_LENGTH) {
                    horse.finish();
                    if (horse.getFinishPosition() == 0) {
                        horse.setFinishPosition(getNextFinishPosition());
                    }
                }
            }
        }

        raceFinished = horses.stream().allMatch(Horse::isFinished);

        if (raceFinished) {
            endRace();
        }

        racePanel.repaint();
        statsPanel.updateStats();
    }

    private int getNextFinishPosition() {
        int max = 0;
        for (Horse horse : horses) {
            if (horse.getFinishPosition() > max) {
                max = horse.getFinishPosition();
            }
        }
        return max + 1;
    }

    private void endRace() {
        raceTimer.stop();
        raceInProgress = false;
        controlPanel.setRaceInProgress(false);

        Collections.sort(horses, (h1, h2) ->
            Integer.compare(h1.getFinishPosition(), h2.getFinishPosition()));

        RaceResult result = new RaceResult(raceNumber++, new ArrayList<>(horses));
        raceHistory.add(result);

        showRaceResults();
    }

    private void showRaceResults() {
        StringBuilder results = new StringBuilder();
        results.append("Race #").append(raceNumber - 1).append(" Results:\n\n");

        for (int i = 0; i < horses.size(); i++) {
            Horse horse = horses.get(i);
            results.append(String.format("%d. %s - %.2f seconds\n",
                i + 1, horse.getName(), horse.getFinishTime()));
        }

        JOptionPane.showMessageDialog(this, results.toString(),
            "Race Results", JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetRace() {
        if (raceInProgress) {
            raceTimer.stop();
            raceInProgress = false;
        }

        for (Horse horse : horses) {
            horse.reset();
        }

        racePanel.repaint();
        controlPanel.setRaceInProgress(false);
    }

    private void showHistory() {
        if (raceHistory.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No races completed yet!",
                "Race History",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog historyDialog = new JDialog(this, "Race History", true);
        historyDialog.setSize(500, 400);
        historyDialog.setLayout(new BorderLayout());

        JTextArea historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        StringBuilder history = new StringBuilder();
        for (RaceResult result : raceHistory) {
            history.append(result.toString()).append("\n");
        }
        historyArea.setText(history.toString());

        JScrollPane scrollPane = new JScrollPane(historyArea);
        historyDialog.add(scrollPane, BorderLayout.CENTER);

        historyDialog.setLocationRelativeTo(this);
        historyDialog.setVisible(true);
    }

    class Horse {
        private String name;
        private Color color;
        private int lane;
        private double position;
        private double speed;
        private double baseSpeed;
        private double stamina;
        private boolean finished;
        private int finishPosition;
        private long startTime;
        private double finishTime;
        private int wins;
        private int races;

        public Horse(String name, Color color, int lane) {
            this.name = name;
            this.color = color;
            this.lane = lane;
            this.baseSpeed = 3 + random.nextDouble() * 2;
            this.stamina = 0.8 + random.nextDouble() * 0.2;
            this.wins = 0;
            this.races = 0;
            reset();
        }

        public void reset() {
            position = 0;
            speed = baseSpeed;
            finished = false;
            finishPosition = 0;
            startTime = System.currentTimeMillis();
        }

        public void move() {
            if (!finished) {
                double speedVariation = (random.nextDouble() - 0.5) * 0.5;
                double staminaFactor = 1.0 - (position / TRACK_LENGTH) * (1.0 - stamina);
                speed = baseSpeed * staminaFactor + speedVariation;
                speed = Math.max(1.0, Math.min(speed, baseSpeed * 1.2));
                position += speed;
            }
        }

        public void finish() {
            finished = true;
            finishTime = (System.currentTimeMillis() - startTime) / 1000.0;
            races++;
        }

        public void setFinishPosition(int position) {
            this.finishPosition = position;
            if (position == 1) {
                wins++;
            }
        }

        public String getName() { return name; }
        public Color getColor() { return color; }
        public int getLane() { return lane; }
        public double getPosition() { return position; }
        public boolean isFinished() { return finished; }
        public int getFinishPosition() { return finishPosition; }
        public double getFinishTime() { return finishTime; }
        public int getWins() { return wins; }
        public int getRaces() { return races; }
        public double getWinRate() {
            return races > 0 ? (double) wins / races * 100 : 0;
        }
    }

    class RacePanel extends JPanel {
        private Image grassTexture;
        private int animationFrame;

        public RacePanel() {
            setPreferredSize(new Dimension(WINDOW_WIDTH - 250, 600));
            setBackground(new Color(34, 139, 34));
            animationFrame = 0;

            Timer animTimer = new Timer(100, e -> {
                animationFrame++;
                repaint();
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            drawTrack(g2d);
            drawHorses(g2d);
            drawFinishLine(g2d);
            drawRaceInfo(g2d);
        }

        private void drawTrack(Graphics2D g2d) {
            int trackStartY = 50;

            for (int i = 0; i < NUM_LANES; i++) {
                int y = trackStartY + i * LANE_HEIGHT;

                if (i % 2 == 0) {
                    g2d.setColor(new Color(50, 150, 50));
                } else {
                    g2d.setColor(new Color(40, 130, 40));
                }
                g2d.fillRect(START_X, y, TRACK_LENGTH, LANE_HEIGHT);

                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL, 0, new float[]{10, 10},
                    animationFrame % 20));
                g2d.drawLine(START_X, y + LANE_HEIGHT,
                    START_X + TRACK_LENGTH, y + LANE_HEIGHT);
            }

            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(START_X, trackStartY, TRACK_LENGTH,
                NUM_LANES * LANE_HEIGHT);

            g2d.setColor(new Color(255, 215, 0));
            g2d.setStroke(new BasicStroke(4));
            g2d.drawLine(START_X, trackStartY, START_X,
                trackStartY + NUM_LANES * LANE_HEIGHT);
        }

        private void drawHorses(Graphics2D g2d) {
            int trackStartY = 50;

            for (Horse horse : horses) {
                int x = START_X + (int) horse.getPosition();
                int y = trackStartY + horse.getLane() * LANE_HEIGHT +
                    LANE_HEIGHT / 2;

                drawHorse(g2d, x, y, horse.getColor(), horse.isFinished());

                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                String nameTag = horse.getName();
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(nameTag);

                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRoundRect(x - textWidth / 2 - 5, y - 35,
                    textWidth + 10, 20, 5, 5);

                g2d.setColor(Color.WHITE);
                g2d.drawString(nameTag, x - textWidth / 2, y - 20);
            }
        }

        private void drawHorse(Graphics2D g2d, int x, int y,
                Color color, boolean isFinished) {
            int bodyWidth = 40;
            int bodyHeight = 25;
            int headSize = 20;

            int legOffset = isFinished ? 0 :
                (int) (Math.sin(animationFrame * 0.5) * 5);

            g2d.setColor(color);
            Ellipse2D body = new Ellipse2D.Double(x - bodyWidth / 2,
                y - bodyHeight / 2, bodyWidth, bodyHeight);
            g2d.fill(body);

            Ellipse2D head = new Ellipse2D.Double(x + bodyWidth / 2 - 5,
                y - headSize / 2 - 5, headSize, headSize);
            g2d.fill(head);

            g2d.setColor(color.darker());
            g2d.setStroke(new BasicStroke(3));

            g2d.drawLine(x - 10, y + bodyHeight / 2,
                x - 10, y + bodyHeight / 2 + 15 + legOffset);
            g2d.drawLine(x - 5, y + bodyHeight / 2,
                x - 5, y + bodyHeight / 2 + 15 - legOffset);
            g2d.drawLine(x + 5, y + bodyHeight / 2,
                x + 5, y + bodyHeight / 2 + 15 - legOffset);
            g2d.drawLine(x + 10, y + bodyHeight / 2,
                x + 10, y + bodyHeight / 2 + 15 + legOffset);

            g2d.setColor(Color.BLACK);
            g2d.fillOval(x + bodyWidth / 2 + 5, y - 5, 4, 4);

            if (!isFinished) {
                g2d.setColor(new Color(255, 0, 0, 100));
                int[] xPoints = {x + bodyWidth / 2 + 15,
                    x + bodyWidth / 2 + 35,
                    x + bodyWidth / 2 + 20};
                int[] yPoints = {y - 10, y, y + 10};
                g2d.fillPolygon(xPoints, yPoints, 3);
            }
        }

        private void drawFinishLine(Graphics2D g2d) {
            int trackStartY = 50;
            int finishX = START_X + TRACK_LENGTH;

            g2d.setStroke(new BasicStroke(2));
            int squareSize = 20;

            for (int i = 0; i < NUM_LANES * LANE_HEIGHT / squareSize; i++) {
                for (int j = 0; j < 2; j++) {
                    if ((i + j) % 2 == 0) {
                        g2d.setColor(Color.BLACK);
                    } else {
                        g2d.setColor(Color.WHITE);
                    }
                    g2d.fillRect(finishX + j * squareSize,
                        trackStartY + i * squareSize,
                        squareSize, squareSize);
                }
            }
        }

        private void drawRaceInfo(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRoundRect(10, 10, 200, 30, 10, 10);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Race #" + raceNumber, 20, 32);

            if (raceInProgress) {
                g2d.setColor(Color.RED);
                int pulseSize = (int) (Math.sin(animationFrame * 0.3) * 3 + 8);
                g2d.fillOval(180, 15, pulseSize, pulseSize);
            }
        }
    }

    class ControlPanel extends JPanel {
        private JButton startButton;
        private JButton resetButton;
        private JButton historyButton;
        private JButton exitButton;

        public ControlPanel() {
            setPreferredSize(new Dimension(WINDOW_WIDTH, 80));
            setBackground(new Color(40, 40, 40));
            setLayout(new FlowLayout(FlowLayout.CENTER, 20, 15));

            startButton = createStyledButton("Start Race",
                new Color(34, 139, 34));
            resetButton = createStyledButton("Reset",
                new Color(255, 140, 0));
            historyButton = createStyledButton("View History",
                new Color(70, 130, 180));
            exitButton = createStyledButton("Exit",
                new Color(178, 34, 34));

            startButton.addActionListener(e -> startRace());
            resetButton.addActionListener(e -> resetRace());
            historyButton.addActionListener(e -> showHistory());
            exitButton.addActionListener(e -> System.exit(0));

            add(startButton);
            add(resetButton);
            add(historyButton);
            add(exitButton);
        }

        private JButton createStyledButton(String text, Color bgColor) {
            JButton button = new JButton(text);
            button.setPreferredSize(new Dimension(150, 50));
            button.setFont(new Font("Arial", Font.BOLD, 14));
            button.setBackground(bgColor);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
            ));
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            button.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(bgColor.brighter());
                }
                public void mouseExited(MouseEvent e) {
                    button.setBackground(bgColor);
                }
            });

            return button;
        }

        public void setRaceInProgress(boolean inProgress) {
            startButton.setEnabled(!inProgress);
        }
    }

    class StatsPanel extends JPanel {
        private JTextArea statsArea;

        public StatsPanel() {
            setPreferredSize(new Dimension(250, 600));
            setBackground(new Color(240, 240, 240));
            setLayout(new BorderLayout());

            JLabel titleLabel = new JLabel("Horse Statistics",
                SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
            titleLabel.setOpaque(true);
            titleLabel.setBackground(new Color(70, 130, 180));
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            statsArea = new JTextArea();
            statsArea.setEditable(false);
            statsArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
            statsArea.setMargin(new Insets(10, 10, 10, 10));

            JScrollPane scrollPane = new JScrollPane(statsArea);

            add(titleLabel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);

            updateStats();
        }

        public void updateStats() {
            StringBuilder stats = new StringBuilder();

            for (int i = 0; i < horses.size(); i++) {
                Horse horse = horses.get(i);
                stats.append("━━━━━━━━━━━━━━━━━━━━\n");
                stats.append(String.format("Lane %d: %s\n",
                    i + 1, horse.getName()));
                stats.append("━━━━━━━━━━━━━━━━━━━━\n");
                stats.append(String.format("Position: %.0fm\n",
                    horse.getPosition()));
                stats.append(String.format("Status: %s\n",
                    horse.isFinished() ? "FINISHED" : "RACING"));

                if (horse.isFinished() && horse.getFinishPosition() > 0) {
                    stats.append(String.format("Place: #%d\n",
                        horse.getFinishPosition()));
                    stats.append(String.format("Time: %.2fs\n",
                        horse.getFinishTime()));
                }

                stats.append(String.format("Wins: %d / %d\n",
                    horse.getWins(), horse.getRaces()));

                if (horse.getRaces() > 0) {
                    stats.append(String.format("Win Rate: %.1f%%\n",
                        horse.getWinRate()));
                }

                stats.append("\n");
            }

            statsArea.setText(stats.toString());
        }
    }

    class RaceResult {
        private int raceNumber;
        private List<Horse> finishOrder;
        private long timestamp;

        public RaceResult(int raceNumber, List<Horse> horses) {
            this.raceNumber = raceNumber;
            this.finishOrder = new ArrayList<>(horses);
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("═══════════════════════════════════════\n"));
            sb.append(String.format("Race #%d\n", raceNumber));
            sb.append(String.format("═══════════════════════════════════════\n"));

            for (int i = 0; i < finishOrder.size(); i++) {
                Horse horse = finishOrder.get(i);
                sb.append(String.format("%d. %-20s %.2fs\n",
                    i + 1, horse.getName(), horse.getFinishTime()));
            }

            sb.append("\n");
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new HorseRacingSimulator();
        });
    }
}
