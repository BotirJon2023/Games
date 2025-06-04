import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class BoatRacingTournament {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BoatRacingGame().start());
    }
}

class BoatRacingGame {
    private JFrame frame;
    private RacePanel racePanel;
    private ControlPanel controlPanel;
    private ScoreBoard scoreBoard;

    public void start() {
        frame = new JFrame("Boat Racing Tournament");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        racePanel = new RacePanel();
        controlPanel = new ControlPanel(racePanel);
        scoreBoard = new ScoreBoard();

        racePanel.setScoreBoard(scoreBoard);

        frame.add(racePanel, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.add(scoreBoard, BorderLayout.EAST);

        frame.setSize(1000, 600);
        frame.setVisible(true);
    }
}

class RacePanel extends JPanel {
    private List<Boat> boats;
    private Timer timer;
    private boolean running = false;
    private int finishLine;
    private Random random = new Random();
    private ScoreBoard scoreBoard;
    private int round = 1;

    public RacePanel() {
        setBackground(Color.CYAN);
        boats = new ArrayList<>();
        finishLine = 800;
        initBoats();
        timer = new Timer(30, e -> updateRace());
    }

    public void setScoreBoard(ScoreBoard sb) {
        this.scoreBoard = sb;
    }

    private void initBoats() {
        boats.clear();
        for (int i = 0; i < 5; i++) {
            boats.add(new Boat("Boat " + (i + 1), 50, 80 + i * 80));
        }
    }

    public void startRace() {
        if (!running) {
            running = true;
            for (Boat boat : boats) {
                boat.reset();
            }
            timer.start();
        }
    }

    public void resetRace() {
        running = false;
        timer.stop();
        initBoats();
        repaint();
    }

    private void updateRace() {
        boolean finished = false;
        for (Boat boat : boats) {
            boat.move(random.nextInt(5));
            if (boat.getX() >= finishLine) {
                finished = true;
            }
        }
        repaint();
        if (finished) {
            endRace();
        }
    }

    private void endRace() {
        timer.stop();
        running = false;
        boats.sort((a, b) -> b.getX() - a.getX());
        Boat winner = boats.get(0);
        scoreBoard.recordWin(winner.getName(), round);
        round++;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.RED);
        g.drawLine(finishLine, 0, finishLine, getHeight());
        for (Boat boat : boats) {
            boat.draw(g);
        }
    }
}

class Boat {
    private String name;
    private int x;
    private int y;
    private Color color;
    private static final Color[] COLORS = {Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.PINK};
    private static int colorIndex = 0;

    public Boat(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.color = COLORS[colorIndex % COLORS.length];
        colorIndex++;
    }

    public void move(int distance) {
        x += distance;
    }

    public void reset() {
        x = 50;
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(x, y, 60, 30);
        g.setColor(Color.BLACK);
        g.drawString(name, x, y - 5);
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }
}

class ControlPanel extends JPanel {
    public ControlPanel(RacePanel racePanel) {
        JButton startButton = new JButton("Start Race");
        JButton resetButton = new JButton("Reset");

        startButton.addActionListener(e -> racePanel.startRace());
        resetButton.addActionListener(e -> racePanel.resetRace());

        add(startButton);
        add(resetButton);
    }
}

class ScoreBoard extends JPanel {
    private JTextArea textArea;
    private Map<String, Integer> wins;

    public ScoreBoard() {
        setLayout(new BorderLayout());
        textArea = new JTextArea();
        textArea.setEditable(false);
        wins = new HashMap<>();
        add(new JLabel("Scoreboard"), BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        setPreferredSize(new Dimension(200, 0));
    }

    public void recordWin(String boatName, int round) {
        wins.put(boatName, wins.getOrDefault(boatName, 0) + 1);
        updateDisplay(round);
    }

    private void updateDisplay(int round) {
        StringBuilder sb = new StringBuilder();
        sb.append("Round: ").append(round).append("\n\n");
        List<Map.Entry<String, Integer>> list = new ArrayList<>(wins.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        for (Map.Entry<String, Integer> entry : list) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(" wins\n");
        }
        textArea.setText(sb.toString());
    }
}
