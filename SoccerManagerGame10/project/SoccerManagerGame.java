import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class SoccerManagerGame extends JFrame {
    private GamePanel gamePanel;
    private ControlPanel controlPanel;
    private Team playerTeam;
    private Team opponentTeam;
    private Match currentMatch;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    public SoccerManagerGame() {
        setTitle("Soccer Manager Game - Click 'Start Match' to Begin!");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        initializeTeams();

        gamePanel = new GamePanel();
        controlPanel = new ControlPanel();

        add(gamePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeTeams() {
        playerTeam = new Team("Thunder FC", true);
        opponentTeam = new Team("Lightning United", false);

        playerTeam.generatePlayers();
        opponentTeam.generatePlayers();
    }

    private void startMatch() {
        currentMatch = new Match(playerTeam, opponentTeam);
        gamePanel.setMatch(currentMatch);
        currentMatch.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SoccerManagerGame());
    }

    class GamePanel extends JPanel {
        private Match match;
        private Ball ball;
        private List<PlayerSprite> playerSprites;
        private Timer animationTimer;
        private int frameCount = 0;
        private boolean matchInProgress = false;
        private List<CloudSprite> clouds;
        private int grassAnimOffset = 0;

        public GamePanel() {
            setPreferredSize(new Dimension(WINDOW_WIDTH, 600));
            setBackground(new Color(34, 139, 34));
            playerSprites = new ArrayList<>();
            clouds = new ArrayList<>();

            Random rand = new Random();
            for (int i = 0; i < 5; i++) {
                clouds.add(new CloudSprite(rand.nextInt(WINDOW_WIDTH), rand.nextInt(100) + 20));
            }

            initializeIdlePlayers();

            animationTimer = new Timer(30, e -> {
                frameCount++;
                grassAnimOffset = (grassAnimOffset + 1) % 100;

                for (CloudSprite cloud : clouds) {
                    cloud.update();
                }

                if (matchInProgress && match != null) {
                    match.update();
                }
                repaint();
            });
            animationTimer.start();
        }

        private void initializeIdlePlayers() {
            Random rand = new Random();
            for (int i = 0; i < 11; i++) {
                int x = 150 + (i % 4) * 120 + rand.nextInt(30);
                int y = 100 + (i / 4) * 150 + rand.nextInt(30);
                playerSprites.add(new PlayerSprite(x, y, new Color(30, 144, 255),
                    new Player("Player " + (i+1), "POS", 75)));
            }

            for (int i = 0; i < 11; i++) {
                int x = 700 + (i % 4) * 120 + rand.nextInt(30);
                int y = 100 + (i / 4) * 150 + rand.nextInt(30);
                playerSprites.add(new PlayerSprite(x, y, new Color(220, 20, 60),
                    new Player("Player " + (i+12), "POS", 75)));
            }

            ball = new Ball(WINDOW_WIDTH / 2, 300);
        }

        public void setMatch(Match m) {
            this.match = m;
            this.ball = m.getBall();
            matchInProgress = true;
            playerSprites.clear();
            initializePlayerSprites();
        }

        private void initializePlayerSprites() {
            Random rand = new Random();

            for (int i = 0; i < 11; i++) {
                int x = 150 + (i % 4) * 120 + rand.nextInt(20);
                int y = 100 + (i / 4) * 150 + rand.nextInt(20);
                playerSprites.add(new PlayerSprite(x, y, new Color(30, 144, 255),
                    match.getHomeTeam().getPlayers().get(i)));
            }

            for (int i = 0; i < 11; i++) {
                int x = 700 + (i % 4) * 120 + rand.nextInt(20);
                int y = 100 + (i / 4) * 150 + rand.nextInt(20);
                playerSprites.add(new PlayerSprite(x, y, new Color(220, 20, 60),
                    match.getAwayTeam().getPlayers().get(i)));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawSky(g2d);
            drawClouds(g2d);
            drawField(g2d);
            drawPlayerSprites(g2d);
            drawBall(g2d);

            if (match != null) {
                drawScore(g2d);
                drawMatchInfo(g2d);
            } else {
                drawWelcomeMessage(g2d);
            }
        }

        private void drawSky(Graphics2D g2d) {
            GradientPaint skyGradient = new GradientPaint(
                0, 0, new Color(135, 206, 250),
                0, getHeight() / 3, new Color(100, 180, 255)
            );
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight() / 3);
        }

        private void drawClouds(Graphics2D g2d) {
            for (CloudSprite cloud : clouds) {
                cloud.draw(g2d);
            }
        }

        private void drawField(Graphics2D g2d) {
            GradientPaint fieldGradient = new GradientPaint(
                0, getHeight() / 3, new Color(34, 139, 34),
                0, getHeight(), new Color(50, 168, 82)
            );
            g2d.setPaint(fieldGradient);
            g2d.fillRect(0, getHeight() / 3, getWidth(), getHeight());

            g2d.setColor(new Color(40, 150, 60));
            for (int i = 0; i < getWidth(); i += 40) {
                int offset = (i + grassAnimOffset) % 80 < 40 ? 2 : -2;
                g2d.fillRect(i, getHeight() / 3, 40, getHeight());
            }

            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));

            int fieldTop = getHeight() / 3 + 20;
            int fieldHeight = getHeight() - fieldTop - 20;

            g2d.drawRect(50, fieldTop, getWidth() - 100, fieldHeight);

            int centerX = getWidth() / 2;
            g2d.drawLine(centerX, fieldTop, centerX, fieldTop + fieldHeight);

            int centerY = fieldTop + fieldHeight / 2;
            g2d.drawOval(centerX - 60, centerY - 60, 120, 120);
            g2d.fillOval(centerX - 5, centerY - 5, 10, 10);

            g2d.drawRect(50, centerY - 80, 80, 160);
            g2d.drawRect(50, centerY - 40, 30, 80);

            g2d.drawRect(getWidth() - 130, centerY - 80, 80, 160);
            g2d.drawRect(getWidth() - 80, centerY - 40, 30, 80);

            g2d.drawArc(40, centerY - 10, 30, 20, 270, 180);
            g2d.drawArc(getWidth() - 70, centerY - 10, 30, 20, 90, 180);

            g2d.setStroke(new BasicStroke(2));
            for (int i = fieldTop + 40; i < fieldTop + fieldHeight; i += 30) {
                g2d.drawLine(65, i, 75, i);
                g2d.drawLine(getWidth() - 75, i, getWidth() - 65, i);
            }
        }

        private void drawPlayerSprites(Graphics2D g2d) {
            for (PlayerSprite sprite : playerSprites) {
                sprite.update(frameCount);
                sprite.draw(g2d);
            }
        }

        private void drawBall(Graphics2D g2d) {
            if (ball != null) {
                ball.update();

                int shadowOffset = 3;
                g2d.setColor(new Color(0, 0, 0, 50));
                g2d.fillOval((int)ball.getX() - 6 + shadowOffset, (int)ball.getY() - 6 + shadowOffset, 12, 12);

                GradientPaint gp = new GradientPaint(
                    (float)(ball.getX() - 8), (float)(ball.getY() - 8), Color.WHITE,
                    (float)(ball.getX() + 8), (float)(ball.getY() + 8), new Color(200, 200, 200)
                );
                g2d.setPaint(gp);
                g2d.fillOval((int)ball.getX() - 10, (int)ball.getY() - 10, 20, 20);

                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval((int)ball.getX() - 10, (int)ball.getY() - 10, 20, 20);

                g2d.setStroke(new BasicStroke(1));
                int numPentagons = 5;
                for (int i = 0; i < numPentagons; i++) {
                    double angle = (frameCount * 0.05) + (i * Math.PI * 2 / numPentagons);
                    int px = (int)(ball.getX() + Math.cos(angle) * 5);
                    int py = (int)(ball.getY() + Math.sin(angle) * 5);
                    g2d.fillRect(px - 2, py - 2, 4, 4);
                }
            }
        }

        private void drawScore(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRoundRect(getWidth() / 2 - 150, 10, 300, 70, 15, 15);

            g2d.setColor(new Color(255, 215, 0));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRoundRect(getWidth() / 2 - 150, 10, 300, 70, 15, 15);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            String scoreText = match.getHomeScore() + " - " + match.getAwayScore();
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(scoreText);
            g2d.drawString(scoreText, getWidth() / 2 - textWidth / 2, 58);
        }

        private void drawMatchInfo(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRoundRect(10, 10, 280, 140, 15, 15);

            g2d.setColor(new Color(255, 215, 0));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(10, 10, 280, 140, 15, 15);

            g2d.setColor(new Color(30, 144, 255));
            g2d.fillRect(20, 30, 15, 15);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString(match.getHomeTeam().getName(), 45, 43);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            g2d.drawString("vs", 20, 68);

            g2d.setColor(new Color(220, 20, 60));
            g2d.fillRect(20, 80, 15, 15);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString(match.getAwayTeam().getName(), 45, 93);

            g2d.setColor(new Color(255, 215, 0));
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString("Time: " + match.getMatchTime() + "'", 20, 125);

            if (match.isMatchEnded()) {
                g2d.setColor(new Color(0, 0, 0, 230));
                g2d.fillRoundRect(getWidth() / 2 - 150, getHeight() / 2 - 60, 300, 120, 20, 20);

                g2d.setColor(new Color(255, 215, 0));
                g2d.setStroke(new BasicStroke(4));
                g2d.drawRoundRect(getWidth() / 2 - 150, getHeight() / 2 - 60, 300, 120, 20, 20);

                g2d.setColor(new Color(255, 215, 0));
                g2d.setFont(new Font("Arial", Font.BOLD, 36));
                String fullTimeText = "FULL TIME";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(fullTimeText, getWidth() / 2 - fm.stringWidth(fullTimeText) / 2, getHeight() / 2 - 10);

                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 28));
                String finalScore = match.getHomeScore() + " - " + match.getAwayScore();
                g2d.drawString(finalScore, getWidth() / 2 - fm.stringWidth(finalScore) / 2 + 20, getHeight() / 2 + 35);
            }
        }

        private void drawWelcomeMessage(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRoundRect(getWidth() / 2 - 250, getHeight() / 2 - 80, 500, 160, 20, 20);

            g2d.setColor(new Color(255, 215, 0));
            g2d.setStroke(new BasicStroke(4));
            g2d.drawRoundRect(getWidth() / 2 - 250, getHeight() / 2 - 80, 500, 160, 20, 20);

            g2d.setColor(new Color(255, 215, 0));
            g2d.setFont(new Font("Arial", Font.BOLD, 32));
            String welcome = "Welcome to Soccer Manager!";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(welcome, getWidth() / 2 - fm.stringWidth(welcome) / 2, getHeight() / 2 - 20);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            String instruction = "Click 'Start Match' to begin!";
            fm = g2d.getFontMetrics();
            g2d.drawString(instruction, getWidth() / 2 - fm.stringWidth(instruction) / 2, getHeight() / 2 + 20);

            String tip = "Players are warming up...";
            g2d.setFont(new Font("Arial", Font.ITALIC, 16));
            fm = g2d.getFontMetrics();
            g2d.drawString(tip, getWidth() / 2 - fm.stringWidth(tip) / 2, getHeight() / 2 + 50);
        }
    }

    class ControlPanel extends JPanel {
        private JButton startMatchBtn;
        private JButton viewSquadBtn;
        private JButton tacticsBtn;
        private JButton kickBallBtn;
        private JLabel statusLabel;

        public ControlPanel() {
            setPreferredSize(new Dimension(WINDOW_WIDTH, 120));
            setBackground(new Color(45, 45, 45));
            setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));

            startMatchBtn = createStyledButton("Start Match", new Color(76, 175, 80));
            viewSquadBtn = createStyledButton("View Squad", new Color(33, 150, 243));
            tacticsBtn = createStyledButton("Tactics", new Color(255, 152, 0));
            kickBallBtn = createStyledButton("Kick Ball", new Color(156, 39, 176));

            statusLabel = new JLabel("Ready to play!");
            statusLabel.setForeground(new Color(255, 215, 0));
            statusLabel.setFont(new Font("Arial", Font.BOLD, 16));

            startMatchBtn.addActionListener(e -> {
                startMatch();
                statusLabel.setText("Match in progress - Watch the action!");
                startMatchBtn.setEnabled(false);
                kickBallBtn.setEnabled(false);
            });

            kickBallBtn.addActionListener(e -> {
                if (gamePanel.ball != null) {
                    Random rand = new Random();
                    gamePanel.ball.kickTowards(200 + rand.nextInt(800), 200 + rand.nextInt(300));
                    statusLabel.setText("Ball kicked!");
                }
            });

            viewSquadBtn.addActionListener(e -> showSquadDialog());
            tacticsBtn.addActionListener(e -> showTacticsDialog());

            add(startMatchBtn);
            add(kickBallBtn);
            add(viewSquadBtn);
            add(tacticsBtn);

            JPanel statusPanel = new JPanel();
            statusPanel.setBackground(new Color(45, 45, 45));
            statusPanel.add(statusLabel);
            add(statusPanel);
        }

        private JButton createStyledButton(String text, Color color) {
            JButton button = new JButton(text);
            button.setPreferredSize(new Dimension(160, 45));
            button.setFont(new Font("Arial", Font.BOLD, 14));
            button.setBackground(color);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            button.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(color.brighter());
                }
                public void mouseExited(MouseEvent e) {
                    button.setBackground(color);
                }
            });

            return button;
        }

        private void showSquadDialog() {
            JDialog dialog = new JDialog(SoccerManagerGame.this, "Your Squad", true);
            dialog.setSize(550, 650);
            dialog.setLayout(new BorderLayout());

            JPanel headerPanel = new JPanel();
            headerPanel.setBackground(new Color(30, 144, 255));
            headerPanel.setPreferredSize(new Dimension(550, 50));
            JLabel headerLabel = new JLabel("Thunder FC Squad");
            headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
            headerLabel.setForeground(Color.WHITE);
            headerPanel.add(headerLabel);
            dialog.add(headerPanel, BorderLayout.NORTH);

            JPanel squadPanel = new JPanel();
            squadPanel.setLayout(new BoxLayout(squadPanel, BoxLayout.Y_AXIS));
            squadPanel.setBackground(Color.WHITE);

            for (Player player : playerTeam.getPlayers()) {
                JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
                playerPanel.setMaximumSize(new Dimension(530, 60));
                playerPanel.setBackground(new Color(240, 248, 255));
                playerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(30, 144, 255), 2),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));

                JLabel nameLabel = new JLabel(player.getName());
                nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
                nameLabel.setPreferredSize(new Dimension(180, 30));

                JLabel posLabel = new JLabel(player.getPosition());
                posLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                posLabel.setPreferredSize(new Dimension(80, 30));
                posLabel.setOpaque(true);
                posLabel.setBackground(new Color(255, 215, 0));
                posLabel.setHorizontalAlignment(SwingConstants.CENTER);

                JLabel ratingLabel = new JLabel("⭐ " + player.getRating());
                ratingLabel.setFont(new Font("Arial", Font.BOLD, 14));
                ratingLabel.setPreferredSize(new Dimension(100, 30));

                playerPanel.add(nameLabel);
                playerPanel.add(posLabel);
                playerPanel.add(ratingLabel);
                squadPanel.add(playerPanel);
                squadPanel.add(Box.createVerticalStrut(5));
            }

            JScrollPane scrollPane = new JScrollPane(squadPanel);
            dialog.add(scrollPane, BorderLayout.CENTER);
            dialog.setLocationRelativeTo(SoccerManagerGame.this);
            dialog.setVisible(true);
        }

        private void showTacticsDialog() {
            JDialog dialog = new JDialog(SoccerManagerGame.this, "Tactics", true);
            dialog.setSize(450, 400);
            dialog.setLayout(new BorderLayout());

            JPanel headerPanel = new JPanel();
            headerPanel.setBackground(new Color(255, 152, 0));
            headerPanel.setPreferredSize(new Dimension(450, 60));
            JLabel headerLabel = new JLabel("Choose Your Formation");
            headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
            headerLabel.setForeground(Color.WHITE);
            headerPanel.add(headerLabel);
            dialog.add(headerPanel, BorderLayout.NORTH);

            JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 15, 15));
            buttonPanel.setBackground(Color.WHITE);
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

            JButton formation442 = createStyledButton("4-4-2 (Balanced)", new Color(76, 175, 80));
            JButton formation433 = createStyledButton("4-3-3 (Attacking)", new Color(244, 67, 54));
            JButton formation352 = createStyledButton("3-5-2 (Midfield Control)", new Color(33, 150, 243));

            formation442.setPreferredSize(new Dimension(350, 70));
            formation433.setPreferredSize(new Dimension(350, 70));
            formation352.setPreferredSize(new Dimension(350, 70));

            formation442.addActionListener(e -> {
                playerTeam.setFormation("4-4-2");
                JOptionPane.showMessageDialog(dialog,
                    "Formation set to 4-4-2 - Balanced approach!",
                    "Formation Changed",
                    JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            });

            formation433.addActionListener(e -> {
                playerTeam.setFormation("4-3-3");
                JOptionPane.showMessageDialog(dialog,
                    "Formation set to 4-3-3 - Attack mode activated!",
                    "Formation Changed",
                    JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            });

            formation352.addActionListener(e -> {
                playerTeam.setFormation("3-5-2");
                JOptionPane.showMessageDialog(dialog,
                    "Formation set to 3-5-2 - Dominate the midfield!",
                    "Formation Changed",
                    JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            });

            buttonPanel.add(formation442);
            buttonPanel.add(formation433);
            buttonPanel.add(formation352);

            dialog.add(buttonPanel, BorderLayout.CENTER);
            dialog.setLocationRelativeTo(SoccerManagerGame.this);
            dialog.setVisible(true);
        }
    }
}

class Team {
    private String name;
    private List<Player> players;
    private String formation;
    private boolean isPlayerTeam;

    public Team(String name, boolean isPlayerTeam) {
        this.name = name;
        this.isPlayerTeam = isPlayerTeam;
        this.players = new ArrayList<>();
        this.formation = "4-4-2";
    }

    public void generatePlayers() {
        String[] firstNames = {"Marcus", "James", "Oliver", "David", "Chris", "Ryan", "Alex", "Ben", "Lucas", "Ethan", "Noah"};
        String[] lastNames = {"Sterling", "Rodriguez", "Silva", "Martinez", "Anderson", "Thompson", "Garcia", "Walker", "Kane", "Bruno", "Fernandes"};
        String[] positions = {"GK", "DEF", "DEF", "DEF", "DEF", "MID", "MID", "MID", "MID", "FWD", "FWD"};

        Random rand = new Random();

        for (int i = 0; i < 11; i++) {
            String firstName = firstNames[rand.nextInt(firstNames.length)];
            String lastName = lastNames[rand.nextInt(lastNames.length)];
            String fullName = firstName + " " + lastName;
            int rating = 65 + rand.nextInt(26);
            players.add(new Player(fullName, positions[i], rating));
        }
    }

    public String getName() { return name; }
    public List<Player> getPlayers() { return players; }
    public String getFormation() { return formation; }
    public void setFormation(String formation) { this.formation = formation; }
    public boolean isPlayerTeam() { return isPlayerTeam; }
}

class Player {
    private String name;
    private String position;
    private int rating;
    private int stamina;
    private int goals;
    private int assists;

    public Player(String name, String position, int rating) {
        this.name = name;
        this.position = position;
        this.rating = rating;
        this.stamina = 100;
        this.goals = 0;
        this.assists = 0;
    }

    public String getName() { return name; }
    public String getPosition() { return position; }
    public int getRating() { return rating; }
    public int getStamina() { return stamina; }
    public void decreaseStamina(int amount) { stamina = Math.max(0, stamina - amount); }
    public void scoreGoal() { goals++; }
    public void makeAssist() { assists++; }
    public int getGoals() { return goals; }
    public int getAssists() { return assists; }
}

class Match {
    private Team homeTeam;
    private Team awayTeam;
    private int homeScore;
    private int awayScore;
    private int matchTime;
    private boolean matchEnded;
    private Ball ball;
    private Random rand;
    private List<String> matchEvents;

    public Match(Team home, Team away) {
        this.homeTeam = home;
        this.awayTeam = away;
        this.homeScore = 0;
        this.awayScore = 0;
        this.matchTime = 0;
        this.matchEnded = false;
        this.ball = new Ball(600, 300);
        this.rand = new Random();
        this.matchEvents = new ArrayList<>();
    }

    public void start() {
        Timer matchTimer = new Timer(800, e -> {
            if (matchTime < 90) {
                matchTime++;
                simulateMatchEvents();
            } else {
                matchEnded = true;
                ((Timer)e.getSource()).stop();
            }
        });
        matchTimer.start();
    }

    private void simulateMatchEvents() {
        int eventChance = rand.nextInt(100);

        if (eventChance < 8) {
            boolean homeScores = rand.nextBoolean();
            if (homeScores) {
                homeScore++;
                animateGoal(true);
                matchEvents.add(matchTime + "' GOAL! " + homeTeam.getName() + " scores!");
            } else {
                awayScore++;
                animateGoal(false);
                matchEvents.add(matchTime + "' GOAL! " + awayTeam.getName() + " scores!");
            }
        } else if (eventChance < 25) {
            ball.kickTowards(200 + rand.nextInt(800), 200 + rand.nextInt(300));
        }
    }

    private void animateGoal(boolean homeGoal) {
        if (homeGoal) {
            ball.kickTowards(1100, 300);
        } else {
            ball.kickTowards(100, 300);
        }
    }

    public void update() {
        ball.update();
    }

    public Team getHomeTeam() { return homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public int getHomeScore() { return homeScore; }
    public int getAwayScore() { return awayScore; }
    public int getMatchTime() { return matchTime; }
    public boolean isMatchEnded() { return matchEnded; }
    public Ball getBall() { return ball; }
    public List<String> getMatchEvents() { return matchEvents; }
}

class Ball {
    private double x, y;
    private double targetX, targetY;
    private double velocityX, velocityY;
    private boolean isMoving;
    private static final double FRICTION = 0.92;
    private static final double SPEED = 18.0;

    public Ball(double x, double y) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.velocityX = 0;
        this.velocityY = 0;
        this.isMoving = false;
    }

    public void kickTowards(double targetX, double targetY) {
        this.targetX = targetX;
        this.targetY = targetY;

        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            velocityX = (dx / distance) * SPEED;
            velocityY = (dy / distance) * SPEED;
            isMoving = true;
        }
    }

    public void update() {
        if (isMoving) {
            x += velocityX;
            y += velocityY;

            velocityX *= FRICTION;
            velocityY *= FRICTION;

            if (Math.abs(velocityX) < 0.2 && Math.abs(velocityY) < 0.2) {
                velocityX = 0;
                velocityY = 0;
                isMoving = false;
            }

            if (x < 70) x = 70;
            if (x > 1130) x = 1130;
            if (y < 220) y = 220;
            if (y > 580) y = 580;
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public boolean isMoving() { return isMoving; }
}

class PlayerSprite {
    private double x, y;
    private double baseX, baseY;
    private double offsetX, offsetY;
    private Color color;
    private Player player;
    private static final int SIZE = 24;
    private double moveAngle;
    private Random rand;

    public PlayerSprite(double x, double y, Color color, Player player) {
        this.x = x;
        this.y = y;
        this.baseX = x;
        this.baseY = y;
        this.color = color;
        this.player = player;
        this.offsetX = 0;
        this.offsetY = 0;
        this.moveAngle = 0;
        this.rand = new Random();
    }

    public void update(int frameCount) {
        offsetX = Math.sin(frameCount * 0.04 + baseX) * 8;
        offsetY = Math.cos(frameCount * 0.05 + baseY) * 6;

        moveAngle += 0.015;

        if (frameCount % 200 == 0) {
            baseX += rand.nextInt(41) - 20;
            baseY += rand.nextInt(41) - 20;
            baseX = Math.max(80, Math.min(1120, baseX));
            baseY = Math.max(240, Math.min(560, baseY));
        }
    }

    public void draw(Graphics2D g2d) {
        double drawX = baseX + offsetX;
        double drawY = baseY + offsetY;

        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillOval((int)drawX - SIZE/2 + 3, (int)drawY - SIZE/2 + 3, SIZE, SIZE);

        GradientPaint gp = new GradientPaint(
            (float)drawX - SIZE/2, (float)drawY - SIZE/2, color.brighter(),
            (float)drawX + SIZE/2, (float)drawY + SIZE/2, color.darker()
        );
        g2d.setPaint(gp);
        g2d.fillOval((int)drawX - SIZE/2, (int)drawY - SIZE/2, SIZE, SIZE);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval((int)drawX - SIZE/2, (int)drawY - SIZE/2, SIZE, SIZE);

        g2d.setColor(Color.BLACK);
        g2d.fillOval((int)drawX - 3, (int)drawY - 6, 3, 3);
        g2d.fillOval((int)drawX + 1, (int)drawY - 6, 3, 3);

        g2d.setStroke(new BasicStroke(2));
        g2d.drawArc((int)drawX - 4, (int)drawY - 2, 8, 6, 180, 180);

        g2d.setColor(color.darker().darker());
        g2d.setFont(new Font("Arial", Font.BOLD, 9));
        String posText = player.getPosition();
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(posText);

        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect((int)drawX - textWidth/2 - 3, (int)drawY + SIZE/2 + 2, textWidth + 6, 14, 5, 5);

        g2d.setColor(color.darker());
        g2d.drawString(posText, (int)drawX - textWidth/2, (int)drawY + SIZE/2 + 13);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public Player getPlayer() { return player; }
}

class CloudSprite {
    private double x, y;
    private double speed;
    private int size;

    public CloudSprite(double x, double y) {
        this.x = x;
        this.y = y;
        Random rand = new Random();
        this.speed = 0.3 + rand.nextDouble() * 0.5;
        this.size = 40 + rand.nextInt(40);
    }

    public void update() {
        x += speed;
        if (x > 1250) {
            x = -100;
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.fillOval((int)x, (int)y, size, size/2);
        g2d.fillOval((int)x + size/3, (int)y - size/4, size, size/2);
        g2d.fillOval((int)x + size/2, (int)y, size, size/2);
    }
}
