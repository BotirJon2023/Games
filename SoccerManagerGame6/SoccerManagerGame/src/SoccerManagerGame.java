import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class SoccerManagerGame extends JFrame {
    private Team teamA, teamB;
    private JTextArea matchLog;
    private AnimationPanel animationPanel;

    public SoccerManagerGame() {
        setTitle("Soccer Manager Game");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        teamA = new Team("Red Hawks");
        teamB = new Team("Blue Sharks");

        teamA.addPlayer(new Player("Alice", "Forward", 85));
        teamA.addPlayer(new Player("Bob", "Midfielder", 78));
        teamA.addPlayer(new Player("Charlie", "Defender", 70));

        teamB.addPlayer(new Player("David", "Forward", 82));
        teamB.addPlayer(new Player("Eve", "Midfielder", 76));
        teamB.addPlayer(new Player("Frank", "Defender", 72));

        matchLog = new JTextArea();
        matchLog.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(matchLog);

        JButton simulateButton = new JButton("Simulate Match");
        simulateButton.addActionListener(e -> simulateMatch());

        animationPanel = new AnimationPanel();

        JPanel controlPanel = new JPanel();
        controlPanel.add(simulateButton);

        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.EAST);
        add(animationPanel, BorderLayout.CENTER);
    }

    private void simulateMatch() {
        MatchSimulator simulator = new MatchSimulator();
        String result = simulator.simulate(teamA, teamB);
        matchLog.append(result + "\n");
        animationPanel.startAnimation();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SoccerManagerGame game = new SoccerManagerGame();
            game.setVisible(true);
        });
    }

    // Player class
    static class Player {
        String name, position;
        int skill;

        public Player(String name, String position, int skill) {
            this.name = name;
            this.position = position;
            this.skill = skill;
        }
    }

    // Team class
    static class Team {
        String name;
        List<Player> players = new ArrayList<>();

        public Team(String name) {
            this.name = name;
        }

        public void addPlayer(Player p) {
            players.add(p);
        }

        public int getTeamStrength() {
            return players.stream().mapToInt(p -> p.skill).sum();
        }

        public String getName() {
            return name;
        }
    }

    // MatchSimulator class
    static class MatchSimulator {
        public String simulate(Team a, Team b) {
            int scoreA = (int)(Math.random() * a.getTeamStrength() / 50);
            int scoreB = (int)(Math.random() * b.getTeamStrength() / 50);
            return a.getName() + " " + scoreA + " - " + scoreB + " " + b.getName();
        }
    }

    // AnimationPanel class
    static class AnimationPanel extends JPanel {
        private int ballX = 0;
        private Timer timer;

        public AnimationPanel() {
            setBackground(Color.GREEN);
        }

        public void startAnimation() {
            ballX = 0;
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            timer = new Timer(30, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ballX += 5;
                    if (ballX > getWidth()) {
                        timer.stop();
                    }
                    repaint();
                }
            });
            timer.start();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.fillOval(ballX, getHeight() / 2 - 10, 20, 20);
        }
    }
}
