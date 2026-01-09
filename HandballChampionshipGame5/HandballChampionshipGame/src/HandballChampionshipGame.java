import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class HandballChampionshipGame extends JFrame {

    // Game constants
    private static final int COURT_WIDTH = 1200;
    private static final int COURT_HEIGHT = 800;
    private static final int GOAL_WIDTH = 300;
    private static final int GOAL_HEIGHT = 200;
    private static final int PLAYER_SIZE = 30;
    private static final int BALL_SIZE = 15;
    private static final int ANIMATION_DELAY = 16; // ~60 FPS

    // Game states
    private enum GameState { PRE_GAME, PLAYING, GOAL_SCORED, HALF_TIME, GAME_OVER }
    private GameState currentState = GameState.PRE_GAME;

    // Teams and players
    private Team teamA, teamB;
    private List<Player> allPlayers = new ArrayList<>();
    private Ball ball;
    private Player ballPossessor;

    // Game variables
    private int scoreTeamA = 0;
    private int scoreTeamB = 0;
    private int gameTime = 0;
    private int maxGameTime = 6000; // 10 minutes game time (100 = 1 second)
    private int currentHalf = 1;
    private String lastEvent = "";
    private int eventTimer = 0;

    // Animation
    private Timer gameTimer;
    private GamePanel gamePanel;
    private Random random = new Random();

    // Statistics
    private Map<String, Integer> playerStats = new HashMap<>();
    private List<String> gameLog = new ArrayList<>();

    // Colors
    private Color teamAColor = new Color(0, 100, 255); // Blue
    private Color teamBColor = new Color(255, 50, 50); // Red
    private Color courtColor = new Color(100, 200, 100);
    private Color lineColor = Color.WHITE;

    public HandballChampionshipGame() {
        setTitle("Handball Championship 2024");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        initializeTeams();
        initializeGame();

        gamePanel = new GamePanel();
        add(gamePanel);

        setupControls();
        setupGameTimer();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeTeams() {
        // Create teams with players
        teamA = new Team("Blue Eagles", teamAColor, true);
        teamB = new Team("Red Panthers", teamBColor, false);

        // Add players to team A (7 players including goalkeeper)
        teamA.addPlayer(new Player("GK", "Miller", 1, true, 150, COURT_HEIGHT/2));
        teamA.addPlayer(new Player("LW", "Johnson", 2, false, 300, 150));
        teamA.addPlayer(new Player("LB", "Williams", 3, false, 400, 250));
        teamA.addPlayer(new Player("CB", "Brown", 4, false, 400, COURT_HEIGHT/2));
        teamA.addPlayer(new Player("RB", "Jones", 5, false, 400, 550));
        teamA.addPlayer(new Player("RW", "Garcia", 6, false, 300, 650));
        teamA.addPlayer(new Player("P", "Davis", 7, false, 500, COURT_HEIGHT/2));

        // Add players to team B
        teamB.addPlayer(new Player("GK", "Smith", 1, true, COURT_WIDTH-150, COURT_HEIGHT/2));
        teamB.addPlayer(new Player("LW", "Martinez", 2, false, COURT_WIDTH-300, 150));
        teamB.addPlayer(new Player("LB", "Rodriguez", 3, false, COURT_WIDTH-400, 250));
        teamB.addPlayer(new Player("CB", "Wilson", 4, false, COURT_WIDTH-400, COURT_HEIGHT/2));
        teamB.addPlayer(new Player("RB", "Taylor", 5, false, COURT_WIDTH-400, 550));
        teamB.addPlayer(new Player("RW", "Thomas", 6, false, COURT_WIDTH-300, 650));
        teamB.addPlayer(new Player("P", "Moore", 7, false, COURT_WIDTH-500, COURT_HEIGHT/2));

        allPlayers.addAll(teamA.getPlayers());
        allPlayers.addAll(teamB.getPlayers());

        // Initialize ball
        ball = new Ball(COURT_WIDTH/2, COURT_HEIGHT/2);
        ballPossessor = teamA.getPlayers().get(3); // Center back starts with ball
    }

    private void initializeGame() {
        gameLog.add("Game initialized: " + teamA.getName() + " vs " + teamB.getName());
        gameLog.add("Referee: James Anderson");
        gameLog.add("Venue: Championship Arena");
    }

    private void setupControls() {
        JPanel controlPanel = new JPanel();
        JButton startButton = new JButton("Start Game");
        JButton pauseButton = new JButton("Pause");
        JButton resetButton = new JButton("Reset");
        JButton simulateButton = new JButton("Simulate Next Action");

        startButton.addActionListener(e -> startGame());
        pauseButton.addActionListener(e -> togglePause());
        resetButton.addActionListener(e -> resetGame());
        simulateButton.addActionListener(e -> simulateAction());

        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(resetButton);
        controlPanel.add(simulateButton);

        add(controlPanel, BorderLayout.SOUTH);

        // Keyboard controls
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (currentState == GameState.PLAYING && ballPossessor != null) {
                    handlePlayerMovement(e.getKeyCode());
                }
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (ballPossessor != null) {
                        attemptShot();
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_P) {
                    passBall();
                }
            }
        });
    }

    private void setupGameTimer() {
        gameTimer = new Timer(ANIMATION_DELAY, e -> {
            if (currentState == GameState.PLAYING) {
                updateGame();
            } else if (currentState == GameState.GOAL_SCORED) {
                eventTimer--;
                if (eventTimer <= 0) {
                    currentState = GameState.PLAYING;
                    resetAfterGoal();
                }
            }
            gamePanel.repaint();
        });
    }

    private void startGame() {
        if (currentState == GameState.PRE_GAME || currentState == GameState.HALF_TIME) {
            currentState = GameState.PLAYING;
            gameTimer.start();
            gameLog.add("Game started! " + teamA.getName() + " vs " + teamB.getName());
        }
    }

    private void togglePause() {
        if (gameTimer.isRunning()) {
            gameTimer.stop();
            gameLog.add("Game paused");
        } else {
            gameTimer.start();
            gameLog.add("Game resumed");
        }
    }

    private void resetGame() {
        gameTimer.stop();
        scoreTeamA = 0;
        scoreTeamB = 0;
        gameTime = 0;
        currentHalf = 1;
        initializeTeams();
        currentState = GameState.PRE_GAME;
        gameLog.clear();
        initializeGame();
        gamePanel.repaint();
    }

    private void simulateAction() {
        if (currentState == GameState.PLAYING) {
            performRandomAction();
            updateGame();
            gamePanel.repaint();
        }
    }

    private void updateGame() {
        gameTime++;

        // Check for halftime
        if (gameTime >= maxGameTime/2 && currentHalf == 1) {
            currentState = GameState.HALF_TIME;
            gameLog.add("--- HALF TIME ---");
            gameLog.add("Score: " + teamA.getName() + " " + scoreTeamA + " - " + scoreTeamB + " " + teamB.getName());
            return;
        }

        // Check for end of game
        if (gameTime >= maxGameTime) {
            currentState = GameState.GAME_OVER;
            gameTimer.stop();
            gameLog.add("--- FINAL WHISTLE ---");
            gameLog.add("FINAL SCORE: " + teamA.getName() + " " + scoreTeamA + " - " + scoreTeamB + " " + teamB.getName());
            determineWinner();
            return;
        }

        // Update ball position
        if (ballPossessor == null) {
            ball.update();
            checkBallCollision();
            checkBallPickup();
        } else {
            ball.setPosition(ballPossessor.getX() + 20, ballPossessor.getY() + 10);
        }

        // AI movement for non-possessing players
        updateAIMovement();

        // Random events
        if (random.nextInt(200) == 0 && ballPossessor != null) {
            attemptSteal();
        }

        // Time-based events
        if (gameTime % 100 == 0) {
            updateGameLog();
        }
    }

    private void handlePlayerMovement(int keyCode) {
        Player player = ballPossessor;
        if (player == null) return;

        int speed = 4;
        switch (keyCode) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                player.move(0, -speed);
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                player.move(0, speed);
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                player.move(-speed, 0);
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                player.move(speed, 0);
                break;
        }

        // Keep player in bounds
        int margin = 50;
        if (player.getX() < margin) player.setX(margin);
        if (player.getX() > COURT_WIDTH - margin) player.setX(COURT_WIDTH - margin);
        if (player.getY() < margin) player.setY(margin);
        if (player.getY() > COURT_HEIGHT - margin) player.setY(COURT_HEIGHT - margin);

        // Check for shooting position
        if (player.getX() > COURT_WIDTH - 400 && player.getTeam() == teamA) {
            lastEvent = player.getName() + " in shooting position!";
        }
        if (player.getX() < 400 && player.getTeam() == teamB) {
            lastEvent = player.getName() + " in shooting position!";
        }
    }

    private void attemptShot() {
        if (ballPossessor == null) return;

        Player shooter = ballPossessor;
        Team attackingTeam = shooter.getTeam();
        Team defendingTeam = (attackingTeam == teamA) ? teamB : teamA;

        // Calculate shot chance based on position and player stats
        double distanceToGoal = Math.abs(shooter.getX() - (attackingTeam == teamA ? COURT_WIDTH : 0));
        double shotAccuracy = 80 - (distanceToGoal / 20);
        shotAccuracy += shooter.getSkill() * 0.5;

        // Goalkeeper save chance
        Player goalkeeper = defendingTeam.getGoalkeeper();
        double saveChance = 40 + goalkeeper.getSkill();

        boolean isGoal = (random.nextInt(100) < shotAccuracy) && (random.nextInt(100) > saveChance);

        if (isGoal) {
            // GOAL SCORED!
            if (attackingTeam == teamA) {
                scoreTeamA++;
                lastEvent = "GOAL! " + shooter.getName() + " scores for " + teamA.getName() + "!";
            } else {
                scoreTeamB++;
                lastEvent = "GOAL! " + shooter.getName() + " scores for " + teamB.getName() + "!";
            }

            shooter.incrementGoals();
            gameLog.add(lastEvent);
            currentState = GameState.GOAL_SCORED;
            eventTimer = 100; // Show goal for 100 frames

            // Goal celebration animation
            for (Player p : attackingTeam.getPlayers()) {
                p.startCelebration();
            }
        } else {
            // Shot missed or saved
            if (random.nextInt(100) < saveChance) {
                lastEvent = "Great save by " + goalkeeper.getName() + "!";
                goalkeeper.incrementSaves();
            } else {
                lastEvent = shooter.getName() + "'s shot goes wide!";
            }
            ballPossessor = null;
            ball.setVelocity(random.nextInt(10) - 5, random.nextInt(10) - 5);
        }
    }

    private void passBall() {
        if (ballPossessor == null) return;

        Player passer = ballPossessor;
        Team passerTeam = passer.getTeam();
        List<Player> teammates = passerTeam.getPlayers();

        // Find closest teammate
        Player receiver = null;
        double minDistance = Double.MAX_VALUE;

        for (Player p : teammates) {
            if (p != passer) {
                double distance = passer.distanceTo(p);
                if (distance < minDistance && distance < 200) {
                    minDistance = distance;
                    receiver = p;
                }
            }
        }

        if (receiver != null) {
            // Pass success chance
            double passAccuracy = 90 - (minDistance / 20);
            passAccuracy += passer.getSkill() * 0.3;

            if (random.nextInt(100) < passAccuracy) {
                ballPossessor = receiver;
                passer.incrementPasses();
                lastEvent = "Good pass from " + passer.getName() + " to " + receiver.getName();

                // Animate pass
                ball.setPosition(passer.getX(), passer.getY());
                ball.moveTo(receiver.getX(), receiver.getY(), 20);
            } else {
                lastEvent = "Pass intercepted!";
                ballPossessor = null;
                ball.setVelocity(random.nextInt(10) - 5, random.nextInt(10) - 5);
            }
        }
    }

    private void attemptSteal() {
        if (ballPossessor == null) return;

        Player defender = null;
        Team defendingTeam = (ballPossessor.getTeam() == teamA) ? teamB : teamA;

        // Find closest defender
        double minDistance = Double.MAX_VALUE;
        for (Player p : defendingTeam.getPlayers()) {
            if (!p.isGoalkeeper()) {
                double distance = ballPossessor.distanceTo(p);
                if (distance < minDistance && distance < 50) {
                    minDistance = distance;
                    defender = p;
                }
            }
        }

        if (defender != null) {
            double stealChance = 30 + defender.getSkill() - ballPossessor.getSkill();
            if (random.nextInt(100) < stealChance) {
                ballPossessor = defender;
                lastEvent = "Great steal by " + defender.getName() + "!";
                defender.incrementSteals();
            }
        }
    }

    private void checkBallCollision() {
        // Check court boundaries
        if (ball.getX() <= 0 || ball.getX() >= COURT_WIDTH) {
            ball.setVelocity(-ball.getVx(), ball.getVy());
        }
        if (ball.getY() <= 0 || ball.getY() >= COURT_HEIGHT) {
            ball.setVelocity(ball.getVx(), -ball.getVy());
        }

        // Check goal posts
        if (ball.getX() <= 50 && ball.getY() >= (COURT_HEIGHT-GOAL_HEIGHT)/2 &&
                ball.getY() <= (COURT_HEIGHT+GOAL_HEIGHT)/2) {
            // Ball hit left goal post
            ball.setVelocity(Math.abs(ball.getVx()), ball.getVy());
            lastEvent = "Ball hits the post!";
        }
        if (ball.getX() >= COURT_WIDTH-50 && ball.getY() >= (COURT_HEIGHT-GOAL_HEIGHT)/2 &&
                ball.getY() <= (COURT_HEIGHT+GOAL_HEIGHT)/2) {
            // Ball hit right goal post
            ball.setVelocity(-Math.abs(ball.getVx()), ball.getVy());
            lastEvent = "Ball hits the post!";
        }
    }

    private void checkBallPickup() {
        for (Player player : allPlayers) {
            double distance = ball.distanceTo(player);
            if (distance < PLAYER_SIZE) {
                ballPossessor = player;
                ball.setVelocity(0, 0);
                lastEvent = player.getName() + " picks up the ball";
                break;
            }
        }
    }

    private void updateAIMovement() {
        for (Player player : allPlayers) {
            if (player != ballPossessor) {
                // Simple AI movement
                if (ballPossessor == null) {
                    // Move toward loose ball
                    player.moveToward(ball.getX(), ball.getY(), 2);
                } else if (ballPossessor.getTeam() == player.getTeam()) {
                    // Offensive positioning
                    if (player.isGoalkeeper()) {
                        // Goalkeeper stays near goal
                        double targetX = (player.getTeam() == teamA) ? 150 : COURT_WIDTH - 150;
                        double targetY = COURT_HEIGHT/2 + random.nextInt(100) - 50;
                        player.moveToward(targetX, targetY, 1);
                    } else {
                        // Other players move to attacking positions
                        player.moveToward(player.getDefaultX(), player.getDefaultY(), 1);
                    }
                } else {
                    // Defensive positioning - move toward ball possessor
                    player.moveToward(ballPossessor.getX(), ballPossessor.getY(), 2);
                }
            }
        }
    }

    private void performRandomAction() {
        if (ballPossessor == null) return;

        int action = random.nextInt(4);
        switch (action) {
            case 0:
                attemptShot();
                break;
            case 1:
                passBall();
                break;
            case 2:
                // Dribble/move
                int dx = random.nextInt(7) - 3;
                int dy = random.nextInt(7) - 3;
                ballPossessor.move(dx, dy);
                break;
            case 3:
                // Attempt steal if opponent has ball
                attemptSteal();
                break;
        }
    }

    private void resetAfterGoal() {
        // Reset positions after goal
        initializeTeams();
        ballPossessor = (random.nextBoolean()) ? teamA.getPlayers().get(3) : teamB.getPlayers().get(3);
        ball.setPosition(ballPossessor.getX() + 20, ballPossessor.getY() + 10);

        // Stop celebrations
        for (Player p : allPlayers) {
            p.stopCelebration();
        }
    }

    private void determineWinner() {
        if (scoreTeamA > scoreTeamB) {
            lastEvent = teamA.getName() + " WINS THE CHAMPIONSHIP!";
        } else if (scoreTeamB > scoreTeamA) {
            lastEvent = teamB.getName() + " WINS THE CHAMPIONSHIP!";
        } else {
            lastEvent = "IT'S A DRAW! Extra time needed!";
        }
        gameLog.add(lastEvent);
    }

    private void updateGameLog() {
        if (gameLog.size() > 10) {
            gameLog.remove(0);
        }
    }

    class GamePanel extends JPanel {
        public GamePanel() {
            setPreferredSize(new Dimension(COURT_WIDTH, COURT_HEIGHT + 100));
            setBackground(new Color(240, 240, 240));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw court
            drawCourt(g2d);

            // Draw goals
            drawGoals(g2d);

            // Draw players
            for (Player player : allPlayers) {
                drawPlayer(g2d, player);
            }

            // Draw ball
            drawBall(g2d);

            // Draw scoreboard and info
            drawScoreboard(g2d);
            drawGameInfo(g2d);
            drawGameLog(g2d);

            // Draw special effects
            if (currentState == GameState.GOAL_SCORED) {
                drawGoalCelebration(g2d);
            }
        }

        private void drawCourt(Graphics2D g2d) {
            // Court surface
            g2d.setColor(courtColor);
            g2d.fillRect(0, 0, COURT_WIDTH, COURT_HEIGHT);

            // Court lines
            g2d.setColor(lineColor);
            g2d.setStroke(new BasicStroke(3));

            // Center line
            g2d.drawLine(COURT_WIDTH/2, 0, COURT_WIDTH/2, COURT_HEIGHT);

            // Center circle
            g2d.drawOval(COURT_WIDTH/2 - 100, COURT_HEIGHT/2 - 100, 200, 200);

            // Goal areas
            g2d.drawRect(0, (COURT_HEIGHT - GOAL_HEIGHT)/2, 100, GOAL_HEIGHT);
            g2d.drawRect(COURT_WIDTH - 100, (COURT_HEIGHT - GOAL_HEIGHT)/2, 100, GOAL_HEIGHT);

            // 7-meter line
            g2d.drawArc(150, (COURT_HEIGHT - 100)/2, 100, 100, 90, 180);
            g2d.drawArc(COURT_WIDTH - 250, (COURT_HEIGHT - 100)/2, 100, 100, -90, 180);
        }

        private void drawGoals(Graphics2D g2d) {
            // Left goal
            g2d.setColor(new Color(200, 200, 200));
            g2d.fillRect(0, (COURT_HEIGHT - GOAL_HEIGHT)/2 - 10, 50, GOAL_HEIGHT + 20);

            // Right goal
            g2d.fillRect(COURT_WIDTH - 50, (COURT_HEIGHT - GOAL_HEIGHT)/2 - 10, 50, GOAL_HEIGHT + 20);

            // Goal nets
            g2d.setColor(new Color(150, 150, 150, 100));
            for (int i = 0; i < 10; i++) {
                int y = (COURT_HEIGHT - GOAL_HEIGHT)/2 + i * (GOAL_HEIGHT/10);
                g2d.drawLine(0, y, 50, y);
            }
            for (int i = 0; i < 5; i++) {
                int x = i * 10;
                g2d.drawLine(x, (COURT_HEIGHT - GOAL_HEIGHT)/2, x, (COURT_HEIGHT + GOAL_HEIGHT)/2);
            }
        }

        private void drawPlayer(Graphics2D g2d, Player player) {
            int x = player.getX() - PLAYER_SIZE/2;
            int y = player.getY() - PLAYER_SIZE/2;

            // Player circle
            g2d.setColor(player.getTeam().getColor());
            g2d.fillOval(x, y, PLAYER_SIZE, PLAYER_SIZE);

            // Player outline
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x, y, PLAYER_SIZE, PLAYER_SIZE);

            // Player number
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String number = String.valueOf(player.getNumber());
            FontMetrics fm = g2d.getFontMetrics();
            int textX = x + (PLAYER_SIZE - fm.stringWidth(number)) / 2;
            int textY = y + (PLAYER_SIZE + fm.getAscent()) / 2 - 2;
            g2d.drawString(number, textX, textY);

            // Highlight ball possessor
            if (player == ballPossessor) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval(x - 3, y - 3, PLAYER_SIZE + 6, PLAYER_SIZE + 6);
            }

            // Goalkeeper indicator
            if (player.isGoalkeeper()) {
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 9));
                g2d.drawString("GK", x + PLAYER_SIZE/2 - 8, y - 5);
            }

            // Celebration animation
            if (player.isCelebrating()) {
                g2d.setColor(new Color(255, 255, 0, 150));
                for (int i = 0; i < 3; i++) {
                    int sparkleX = x + random.nextInt(PLAYER_SIZE);
                    int sparkleY = y + random.nextInt(PLAYER_SIZE);
                    g2d.fillOval(sparkleX, sparkleY, 4, 4);
                }
            }
        }

        private void drawBall(Graphics2D g2d) {
            int x = (int)ball.getX() - BALL_SIZE/2;
            int y = (int)ball.getY() - BALL_SIZE/2;

            // Ball with gradient
            GradientPaint gradient = new GradientPaint(
                    x, y, Color.WHITE,
                    x + BALL_SIZE, y + BALL_SIZE, Color.ORANGE
            );
            g2d.setPaint(gradient);
            g2d.fillOval(x, y, BALL_SIZE, BALL_SIZE);

            // Ball outline and pattern
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(x, y, BALL_SIZE, BALL_SIZE);

            // Ball seams
            g2d.drawLine(x + BALL_SIZE/2, y + 2, x + BALL_SIZE/2, y + BALL_SIZE - 2);
            g2d.drawLine(x + 2, y + BALL_SIZE/2, x + BALL_SIZE - 2, y + BALL_SIZE/2);

            // Ball shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval(x + 2, y + 2, BALL_SIZE, BALL_SIZE);

            // Ball trail if moving
            if (ball.isMoving()) {
                g2d.setColor(new Color(255, 200, 0, 100));
                for (int i = 1; i <= 3; i++) {
                    int trailX = x - (int)(ball.getVx() * i * 0.5);
                    int trailY = y - (int)(ball.getVy() * i * 0.5);
                    g2d.fillOval(trailX, trailY, BALL_SIZE - i*2, BALL_SIZE - i*2);
                }
            }
        }

        private void drawScoreboard(Graphics2D g2d) {
            int scoreboardY = COURT_HEIGHT + 10;

            // Scoreboard background
            g2d.setColor(new Color(50, 50, 50));
            g2d.fillRect(0, scoreboardY, COURT_WIDTH, 80);

            // Team names and scores
            g2d.setFont(new Font("Arial", Font.BOLD, 24));

            // Team A
            g2d.setColor(teamAColor);
            g2d.drawString(teamA.getName(), 50, scoreboardY + 30);
            g2d.setColor(Color.WHITE);
            g2d.drawString(String.valueOf(scoreTeamA), 50, scoreboardY + 60);

            // Team B
            g2d.setColor(teamBColor);
            g2d.drawString(teamB.getName(), COURT_WIDTH - 200, scoreboardY + 30);
            g2d.setColor(Color.WHITE);
            g2d.drawString(String.valueOf(scoreTeamB), COURT_WIDTH - 50, scoreboardY + 60);

            // Colon separator
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.drawString(":", COURT_WIDTH/2 - 10, scoreboardY + 50);

            // Game time
            g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            String time = String.format("%02d:%02d", (gameTime/100)/60, (gameTime/100)%60);
            g2d.drawString("Time: " + time, COURT_WIDTH/2 - 50, scoreboardY + 30);
            g2d.drawString("Half: " + currentHalf, COURT_WIDTH/2 - 40, scoreboardY + 60);
        }

        private void drawGameInfo(Graphics2D g2d) {
            // Current event display
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));

            // Draw event background
            g2d.setColor(new Color(255, 255, 200, 200));
            g2d.fillRoundRect(10, 10, COURT_WIDTH - 20, 40, 10, 10);

            // Draw event text
            g2d.setColor(Color.RED);
            g2d.drawString(lastEvent, 20, 35);

            // Game state indicator
            g2d.setColor(currentState == GameState.PLAYING ? Color.GREEN : Color.RED);
            g2d.fillOval(COURT_WIDTH - 40, 15, 20, 20);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.drawString(currentState.toString(), COURT_WIDTH - 100, 30);

            // Ball possessor info
            if (ballPossessor != null) {
                g2d.setColor(Color.BLUE);
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                g2d.drawString("Ball: " + ballPossessor.getName() + " (" +
                        ballPossessor.getTeam().getName() + ")", 20, 60);
            }
        }

        private void drawGameLog(Graphics2D g2d) {
            int logY = COURT_HEIGHT + 100;
            int logHeight = 150;

            // Log background
            g2d.setColor(new Color(230, 230, 230));
            g2d.fillRect(0, logY, COURT_WIDTH, logHeight);

            // Log border
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(0, logY, COURT_WIDTH, logHeight);

            // Log title
            g2d.setColor(Color.BLUE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("GAME LOG", 10, logY + 20);

            // Log entries
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));

            int startIndex = Math.max(0, gameLog.size() - 8);
            for (int i = startIndex; i < gameLog.size(); i++) {
                int yPos = logY + 40 + (i - startIndex) * 15;
                g2d.drawString(gameLog.get(i), 10, yPos);
            }
        }

        private void drawGoalCelebration(Graphics2D g2d) {
            // Celebration overlay
            g2d.setColor(new Color(255, 255, 0, 100));
            g2d.fillRect(0, 0, COURT_WIDTH, COURT_HEIGHT);

            // GOAL! text
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 72));
            String goalText = "GOAL!";
            FontMetrics fm = g2d.getFontMetrics();
            int textX = (COURT_WIDTH - fm.stringWidth(goalText)) / 2;
            int textY = COURT_HEIGHT / 2;
            g2d.drawString(goalText, textX, textY);

            // Animated particles
            for (int i = 0; i < 50; i++) {
                int particleX = random.nextInt(COURT_WIDTH);
                int particleY = random.nextInt(COURT_HEIGHT);
                g2d.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                g2d.fillOval(particleX, particleY, 5, 5);
            }
        }
    }

    // Inner classes for game objects
    class Team {
        private String name;
        private Color color;
        private boolean isLeftSide;
        private List<Player> players = new ArrayList<>();

        public Team(String name, Color color, boolean isLeftSide) {
            this.name = name;
            this.color = color;
            this.isLeftSide = isLeftSide;
        }

        public void addPlayer(Player player) {
            player.setTeam(this);
            players.add(player);
        }

        public List<Player> getPlayers() { return players; }
        public String getName() { return name; }
        public Color getColor() { return color; }
        public boolean isLeftSide() { return isLeftSide; }

        public Player getGoalkeeper() {
            for (Player p : players) {
                if (p.isGoalkeeper()) return p;
            }
            return null;
        }
    }

    class Player {
        private String position;
        private String name;
        private int number;
        private boolean goalkeeper;
        private int x, y;
        private int defaultX, defaultY;
        private Team team;
        private int goals = 0;
        private int passes = 0;
        private int steals = 0;
        private int saves = 0;
        private int skill;
        private boolean celebrating = false;

        public Player(String position, String name, int number, boolean goalkeeper, int defaultX, int defaultY) {
            this.position = position;
            this.name = name;
            this.number = number;
            this.goalkeeper = goalkeeper;
            this.defaultX = defaultX;
            this.defaultY = defaultY;
            this.x = defaultX;
            this.y = defaultY;
            this.skill = 50 + new Random().nextInt(50); // Skill between 50-100
        }

        public void move(int dx, int dy) {
            x += dx;
            y += dy;
        }

        public void moveToward(double targetX, double targetY, double speed) {
            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx*dx + dy*dy);

            if (distance > 0) {
                x += (dx / distance) * speed;
                y += (dy / distance) * speed;
            }
        }

        public double distanceTo(Player other) {
            double dx = x - other.x;
            double dy = y - other.y;
            return Math.sqrt(dx*dx + dy*dy);
        }

        public void startCelebration() { celebrating = true; }
        public void stopCelebration() { celebrating = false; }
        public boolean isCelebrating() { return celebrating; }

        // Getters and setters
        public int getX() { return x; }
        public int getY() { return y; }
        public void setX(int x) { this.x = x; }
        public void setY(int y) { this.y = y; }
        public int getDefaultX() { return defaultX; }
        public int getDefaultY() { return defaultY; }
        public String getName() { return name; }
        public int getNumber() { return number; }
        public boolean isGoalkeeper() { return goalkeeper; }
        public Team getTeam() { return team; }
        public void setTeam(Team team) { this.team = team; }
        public int getSkill() { return skill; }

        public void incrementGoals() { goals++; }
        public void incrementPasses() { passes++; }
        public void incrementSteals() { steals++; }
        public void incrementSaves() { saves++; }

        @Override
        public String toString() {
            return name + " (" + position + ") #" + number;
        }
    }

    class Ball {
        private double x, y;
        private double vx = 0, vy = 0;
        private double targetX, targetY;
        private int moveFrames = 0;
        private int currentFrame = 0;

        public Ball(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void update() {
            if (moveFrames > 0) {
                // Animated movement to target
                currentFrame++;
                double progress = (double)currentFrame / moveFrames;
                x = x + (targetX - x) * progress;
                y = y + (targetY - y) * progress;

                if (currentFrame >= moveFrames) {
                    moveFrames = 0;
                    currentFrame = 0;
                }
            } else {
                // Normal movement with velocity
                x += vx;
                y += vy;

                // Apply friction
                vx *= 0.98;
                vy *= 0.98;

                if (Math.abs(vx) < 0.1) vx = 0;
                if (Math.abs(vy) < 0.1) vy = 0;
            }
        }

        public void moveTo(double targetX, double targetY, int frames) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.moveFrames = frames;
            this.currentFrame = 0;
        }

        public double distanceTo(Player player) {
            double dx = x - player.getX();
            double dy = y - player.getY();
            return Math.sqrt(dx*dx + dy*dy);
        }

        // Getters and setters
        public double getX() { return x; }
        public double getY() { return y; }
        public double getVx() { return vx; }
        public double getVy() { return vy; }
        public void setPosition(double x, double y) { this.x = x; this.y = y; }
        public void setVelocity(double vx, double vy) { this.vx = vx; this.vy = vy; }
        public boolean isMoving() { return Math.abs(vx) > 0.5 || Math.abs(vy) > 0.5; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new HandballChampionshipGame();
        });
    }
}