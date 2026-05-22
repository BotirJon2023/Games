import javax.swing.*;
import javax.swing.border.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class FootballManagementSimulation extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private Team homeTeam, awayTeam;
    private GameEngine gameEngine;
    private boolean isTwoPlayerMode;
    private String player1Name, player2Name;

    public FootballManagementSimulation() {
        setTitle("⚽ Football Management Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(new Color(18, 30, 48));

        setupMainMenu();

        add(mainPanel);
        setVisible(true);
    }

    private void setupMainMenu() {
        JPanel menuPanel = new JPanel(new GridBagLayout());
        menuPanel.setBackground(new Color(18, 30, 48));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Title
        JLabel titleLabel = new JLabel("⚽ FOOTBALL MANAGEMENT SIMULATION ⚽");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(new Color(255, 215, 0));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        menuPanel.add(titleLabel, gbc);

        // Subtitle
        JLabel subtitleLabel = new JLabel("Choose Game Mode");
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 24));
        subtitleLabel.setForeground(Color.WHITE);
        gbc.gridy = 1;
        menuPanel.add(subtitleLabel, gbc);

        // Two Player Button
        JButton twoPlayerBtn = createStyledButton("👥 TWO PLAYER MODE", new Color(46, 125, 50));
        twoPlayerBtn.addActionListener(e -> startGame(true));
        gbc.gridy = 2;
        menuPanel.add(twoPlayerBtn, gbc);

        // vs Computer Button
        JButton vsComputerBtn = createStyledButton("🤖 VS COMPUTER MODE", new Color(25, 118, 210));
        vsComputerBtn.addActionListener(e -> startGame(false));
        gbc.gridy = 3;
        menuPanel.add(vsComputerBtn, gbc);

        mainPanel.add(menuPanel, "menu");
        cardLayout.show(mainPanel, "menu");
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setPreferredSize(new Dimension(350, 60));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.darker());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private void startGame(boolean twoPlayer) {
        isTwoPlayerMode = twoPlayer;

        // Get player names
        player1Name = JOptionPane.showInputDialog(this, "Enter Player 1 (Home Team) Name:", "Player 1");
        if (player1Name == null || player1Name.trim().isEmpty()) player1Name = "HOME FC";

        if (twoPlayer) {
            player2Name = JOptionPane.showInputDialog(this, "Enter Player 2 (Away Team) Name:", "Player 2");
            if (player2Name == null || player2Name.trim().isEmpty()) player2Name = "AWAY FC";
        } else {
            player2Name = "COMPUTER";
        }

        // Initialize teams
        homeTeam = new Team(player1Name, true);
        awayTeam = new Team(player2Name, false);

        // Create game panel
        GamePanel gamePanel = new GamePanel(homeTeam, awayTeam, isTwoPlayerMode);
        mainPanel.add(gamePanel, "game");
        cardLayout.show(mainPanel, "game");
    }

    class Team {
        String name;
        int score;
        int possession;
        int shots;
        int shotsOnTarget;
        int tackles;
        int fouls;
        int corners;
        int attackStrength;
        int defenseStrength;
        int stamina;
        boolean isHome;

        Team(String name, boolean isHome) {
            this.name = name;
            this.isHome = isHome;
            this.score = 0;
            this.possession = 50;
            this.shots = 0;
            this.shotsOnTarget = 0;
            this.tackles = 0;
            this.fouls = 0;
            this.corners = 0;
            this.attackStrength = 70 + new Random().nextInt(20);
            this.defenseStrength = 70 + new Random().nextInt(20);
            this.stamina = 100;
        }

        void resetMatchStats() {
            score = 0;
            possession = 50;
            shots = 0;
            shotsOnTarget = 0;
            tackles = 0;
            fouls = 0;
            corners = 0;
            stamina = 100;
        }
    }

    class GameEngine {
        private Random random = new Random();
        private int minute = 0;
        private Timer matchTimer;
        private boolean isFirstHalf = true;

        void startMatch(GamePanel panel) {
            minute = 0;
            isFirstHalf = true;
            homeTeam.resetMatchStats();
            awayTeam.resetMatchStats();

            matchTimer = new Timer(1000, e -> {
                if (minute < 45 || (minute >= 45 && minute < 50 && isFirstHalf)) {
                    simulateMinute(panel);
                    minute++;
                    panel.updateMatchTime(minute, isFirstHalf);
                } else if (minute >= 45 && minute < 50 && isFirstHalf) {
                    // Injury time
                    simulateMinute(panel);
                    minute++;
                    panel.updateMatchTime(minute, isFirstHalf);
                } else if (isFirstHalf) {
                    // Half time
                    isFirstHalf = false;
                    minute = 45;
                    panel.showHalfTime();
                    matchTimer.stop();
                    Timer halfTimeTimer = new Timer(3000, e2 -> {
                        minute = 45;
                        matchTimer.start();
                    });
                    halfTimeTimer.setRepeats(false);
                    halfTimeTimer.start();
                } else if (minute < 90 || (minute >= 90 && minute < 95)) {
                    simulateMinute(panel);
                    minute++;
                    panel.updateMatchTime(minute, false);
                } else {
                    // Match ended
                    matchTimer.stop();
                    panel.endMatch();
                }
            });
            matchTimer.start();
        }

        private void simulateMinute(GamePanel panel) {
            Team attackingTeam = random.nextBoolean() ? homeTeam : awayTeam;
            Team defendingTeam = (attackingTeam == homeTeam) ? awayTeam : homeTeam;

            // Update possession
            double possessionChance = 0.5 + (attackingTeam.attackStrength - defendingTeam.defenseStrength) / 200.0;
            if (random.nextDouble() < possessionChance) {
                attackingTeam.possession = Math.min(100, attackingTeam.possession + 1);
                defendingTeam.possession = Math.max(0, defendingTeam.possession - 1);
            }

            // Simulate action
            int action = random.nextInt(100);

            if (action < 30) {
                // Missed shot
                attackingTeam.shots++;
                panel.showAnimation("❌ " + attackingTeam.name + " missed the shot!", new Color(255, 100, 100));
            } else if (action < 45) {
                // Shot on target
                attackingTeam.shots++;
                attackingTeam.shotsOnTarget++;
                double goalChance = (attackingTeam.attackStrength - defendingTeam.defenseStrength + 50) / 100.0;
                goalChance = Math.min(0.8, Math.max(0.2, goalChance));

                if (random.nextDouble() < goalChance) {
                    // GOAL!
                    attackingTeam.score++;
                    panel.showAnimation("⚽⚽⚽ GOAL! " + attackingTeam.name + " SCORES! ⚽⚽⚽", new Color(255, 215, 0));
                    panel.showGoalAnimation(attackingTeam.isHome);

                    // Update stamina
                    attackingTeam.stamina = Math.max(0, attackingTeam.stamina - 2);
                    defendingTeam.stamina = Math.max(0, defendingTeam.stamina - 1);
                } else {
                    panel.showAnimation("🧤 Great save by " + defendingTeam.name + " goalkeeper!", new Color(100, 200, 255));
                }
            } else if (action < 55) {
                // Tackle
                attackingTeam.tackles++;
                panel.showAnimation("💪 Strong tackle by " + attackingTeam.name + "!", new Color(150, 255, 150));
                defendingTeam.stamina = Math.max(0, defendingTeam.stamina - 3);
            } else if (action < 65) {
                // Foul
                attackingTeam.fouls++;
                panel.showAnimation("🚩 Foul by " + attackingTeam.name + "!", new Color(255, 150, 100));
                if (random.nextBoolean()) {
                    // Corner
                    attackingTeam.corners++;
                    panel.showAnimation("🏁 Corner for " + attackingTeam.name + "!", new Color(255, 200, 100));
                }
            } else if (action < 75) {
                // Corner
                attackingTeam.corners++;
                panel.showAnimation("🏁 Corner for " + attackingTeam.name + "!", new Color(255, 200, 100));
            }

            panel.updateStats();
        }
    }

    class GamePanel extends JPanel {
        private Team home, away;
        private boolean twoPlayerMode;
        private GameEngine engine;
        private JLabel timeLabel, homeScoreLabel, awayScoreLabel;
        private JProgressBar homePossessionBar, awayPossessionBar;
        private JLabel homeStats, awayStats;
        private JLabel animationLabel;
        private Timer animationTimer;
        private boolean isMatchRunning = true;
        private JLabel homeNameLabel, awayNameLabel;
        private JPanel pitchPanel;

        GamePanel(Team home, Team away, boolean twoPlayerMode) {
            this.home = home;
            this.away = away;
            this.twoPlayerMode = twoPlayerMode;
            this.engine = new GameEngine();
            setLayout(new BorderLayout());
            setBackground(new Color(18, 30, 48));

            setupUI();

            // Start match after a short delay
            Timer startDelay = new Timer(1000, e -> engine.startMatch(this));
            startDelay.setRepeats(false);
            startDelay.start();
        }

        private void setupUI() {
            // Top Panel
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(new Color(18, 30, 48));
            topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

            timeLabel = new JLabel("KICKOFF", SwingConstants.CENTER);
            timeLabel.setFont(new Font("Arial", Font.BOLD, 28));
            timeLabel.setForeground(Color.WHITE);
            topPanel.add(timeLabel, BorderLayout.CENTER);

            // Score Panel
            JPanel scorePanel = new JPanel(new GridBagLayout());
            scorePanel.setBackground(new Color(18, 30, 48));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 20, 10, 20);

            // Home Team
            JPanel homePanel = createTeamPanel(home, true);
            gbc.gridx = 0;
            scorePanel.add(homePanel, gbc);

            // VS Label
            JLabel vsLabel = new JLabel("VS");
            vsLabel.setFont(new Font("Arial", Font.BOLD, 36));
            vsLabel.setForeground(new Color(255, 100, 100));
            gbc.gridx = 1;
            scorePanel.add(vsLabel, gbc);

            // Away Team
            JPanel awayPanel = createTeamPanel(away, false);
            gbc.gridx = 2;
            scorePanel.add(awayPanel, gbc);

            topPanel.add(scorePanel, BorderLayout.SOUTH);

            // Center - Pitch (Football Field)
            pitchPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    drawPitch(g);
                }
            };
            pitchPanel.setBackground(new Color(34, 139, 34));
            pitchPanel.setPreferredSize(new Dimension(800, 400));

            // Animation Label on Pitch
            pitchPanel.setLayout(new BorderLayout());
            animationLabel = new JLabel("", SwingConstants.CENTER);
            animationLabel.setFont(new Font("Arial", Font.BOLD, 24));
            animationLabel.setForeground(new Color(255, 215, 0));
            pitchPanel.add(animationLabel, BorderLayout.CENTER);

            // Stats Panel
            JPanel statsPanel = new JPanel(new GridLayout(2, 1));
            statsPanel.setBackground(new Color(18, 30, 48));
            statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

            homeStats = new JLabel();
            homeStats.setFont(new Font("Monospaced", Font.PLAIN, 14));
            homeStats.setForeground(Color.WHITE);
            awayStats = new JLabel();
            awayStats.setFont(new Font("Monospaced", Font.PLAIN, 14));
            awayStats.setForeground(Color.WHITE);

            statsPanel.add(homeStats);
            statsPanel.add(awayStats);

            // Possession Panel
            JPanel possessionPanel = new JPanel(new BorderLayout());
            possessionPanel.setBackground(new Color(18, 30, 48));
            possessionPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            homePossessionBar = new JProgressBar(0, 100);
            homePossessionBar.setStringPainted(true);
            homePossessionBar.setForeground(new Color(66, 133, 244));

            awayPossessionBar = new JProgressBar(0, 100);
            awayPossessionBar.setStringPainted(true);
            awayPossessionBar.setForeground(new Color(234, 67, 53));

            JPanel barsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
            barsPanel.setBackground(new Color(18, 30, 48));
            barsPanel.add(homePossessionBar);
            barsPanel.add(awayPossessionBar);
            possessionPanel.add(barsPanel, BorderLayout.CENTER);

            add(topPanel, BorderLayout.NORTH);
            add(pitchPanel, BorderLayout.CENTER);
            add(statsPanel, BorderLayout.EAST);
            add(possessionPanel, BorderLayout.SOUTH);

            updateStats();
        }

        private void drawPitch(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(34, 139, 34));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Field lines
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));

            // Outer border
            g2d.drawRect(50, 50, getWidth() - 100, getHeight() - 100);

            // Center line
            g2d.drawLine(getWidth() / 2, 50, getWidth() / 2, getHeight() - 50);

            // Center circle
            g2d.drawOval(getWidth() / 2 - 40, getHeight() / 2 - 40, 80, 80);

            // Penalty areas
            g2d.drawRect(50, getHeight() / 2 - 60, 100, 120);
            g2d.drawRect(getWidth() - 150, getHeight() / 2 - 60, 100, 120);

            // Goals
            g2d.setColor(new Color(200, 200, 200));
            g2d.fillRect(30, getHeight() / 2 - 30, 20, 60);
            g2d.fillRect(getWidth() - 50, getHeight() / 2 - 30, 20, 60);
        }

        private JPanel createTeamPanel(Team team, boolean isHome) {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(new Color(18, 30, 48));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            JLabel nameLabel = new JLabel(team.name);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 20));
            nameLabel.setForeground(isHome ? new Color(66, 133, 244) : new Color(234, 67, 53));

            JLabel scoreLabel = new JLabel("0");
            scoreLabel.setFont(new Font("Arial", Font.BOLD, 48));
            scoreLabel.setForeground(Color.WHITE);

            if (isHome) {
                homeScoreLabel = scoreLabel;
                homeNameLabel = nameLabel;
            } else {
                awayScoreLabel = scoreLabel;
                awayNameLabel = nameLabel;
            }

            gbc.gridx = 0;
            panel.add(nameLabel, gbc);
            gbc.gridy = 1;
            panel.add(scoreLabel, gbc);

            return panel;
        }

        void updateMatchTime(int minute, boolean isFirstHalf) {
            if (!isMatchRunning) return;
            String period = isFirstHalf ? "1ST HALF" : "2ND HALF";
            timeLabel.setText(String.format("%s - %d'", period, minute));

            if (minute % 15 == 0 && minute > 0) {
                showAnimation("🔄 SUBSTITUTION POSSIBLE 🔄", new Color(200, 200, 100));
            }
        }

        void updateStats() {
            homeScoreLabel.setText(String.valueOf(home.score));
            awayScoreLabel.setText(String.valueOf(away.score));

            homeStats.setText(String.format("<html><b>%s</b><br/>Shots: %d | On Target: %d<br/>Tackles: %d | Fouls: %d<br/>Corners: %d | Stamina: %d%%</html>",
                    home.name, home.shots, home.shotsOnTarget, home.tackles, home.fouls, home.corners, home.stamina));
            awayStats.setText(String.format("<html><b>%s</b><br/>Shots: %d | On Target: %d<br/>Tackles: %d | Fouls: %d<br/>Corners: %d | Stamina: %d%%</html>",
                    away.name, away.shots, away.shotsOnTarget, away.tackles, away.fouls, away.corners, away.stamina));

            homePossessionBar.setValue(home.possession);
            homePossessionBar.setString(home.name + " " + home.possession + "%");
            awayPossessionBar.setValue(away.possession);
            awayPossessionBar.setString(away.name + " " + away.possession + "%");
        }

        void showAnimation(String text, Color color) {
            animationLabel.setText(text);
            animationLabel.setForeground(color);

            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }

            animationTimer = new Timer(2000, e -> animationLabel.setText(""));
            animationTimer.setRepeats(false);
            animationTimer.start();
        }

        void showGoalAnimation(boolean isHome) {
            // Flash effect
            Color flashColor = isHome ? new Color(66, 133, 244) : new Color(234, 67, 53);
            Timer flashTimer = new Timer(100, new ActionListener() {
                int count = 0;
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (count % 2 == 0) {
                        pitchPanel.setBackground(flashColor);
                    } else {
                        pitchPanel.setBackground(new Color(34, 139, 34));
                    }
                    count++;
                    if (count > 5) {
                        pitchPanel.setBackground(new Color(34, 139, 34));
                        ((Timer)e.getSource()).stop();
                    }
                    pitchPanel.repaint();
                }
            });
            flashTimer.start();
        }

        void showHalfTime() {
            isMatchRunning = false;
            showAnimation("🏆 HALF TIME - TAKE A BREAK! 🏆", new Color(255, 215, 0));
            Timer restartTimer = new Timer(3000, e -> isMatchRunning = true);
            restartTimer.setRepeats(false);
            restartTimer.start();
        }

        void endMatch() {
            isMatchRunning = false;
            String winner;
            if (home.score > away.score) {
                winner = home.name + " WINS! 🏆";
            } else if (away.score > home.score) {
                winner = away.name + " WINS! 🏆";
            } else {
                winner = "IT'S A DRAW! 🤝";
            }

            int response = JOptionPane.showConfirmDialog(this,
                    String.format("MATCH ENDED!\n%s\n\nFinal Score: %d - %d\n\nPlay again?", winner, home.score, away.score),
                    "Match Complete",
                    JOptionPane.YES_NO_OPTION);

            if (response == JOptionPane.YES_OPTION) {
                restartGame();
            } else {
                System.exit(0);
            }
        }

        private void restartGame() {
            mainPanel.remove(this);
            setupMainMenu();
            cardLayout.show(mainPanel, "menu");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new FootballManagementSimulation();
        });
    }
}