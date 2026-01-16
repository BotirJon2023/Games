import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.*;

public class OlympicSportSimulator extends JFrame {

    // Game constants
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final Color BG_COLOR = new Color(30, 60, 120);
    private static final Color TRACK_COLOR = new Color(70, 70, 70);
    private static final Color FIELD_COLOR = new Color(50, 120, 50);
    private static final Color POOL_COLOR = new Color(40, 150, 200);

    // Game state
    private enum GameState { MAIN_MENU, RUNNING, PAUSED, GAME_OVER }
    private GameState currentState = GameState.MAIN_MENU;
    private enum Sport { SPRINT, SWIMMING, ARCHERY, WEIGHTLIFTING, SWIMING, DIVING }
    private Sport currentSport = Sport.SPRINT;

    // Game components
    private GamePanel gamePanel;
    private ControlPanel controlPanel;
    private ScorePanel scorePanel;
    private Athlete player;
    private ArrayList<Athlete> competitors;
    private Timer gameTimer;
    private Random random = new Random();

    // Game variables
    private int score = 0;
    private int round = 1;
    private int timeRemaining = 120; // 2 minutes
    private boolean isAnimating = false;
    private boolean keyPressed = false;

    // Animation variables
    private int animationFrame = 0;
    private int sprintPosition = 50;
    private int swimmingPosition = 100;
    private double arrowAngle = 45;
    private double arrowPower = 50;
    private int weightHeight = 0;
    private int diveRotation = 0;
    private int diveHeight = 200;

    public OlympicSportSimulator() {
        setTitle("Olympic Sport Simulator 2024");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        initializeGame();
        setupUI();
        setupKeyBindings();

        gameTimer = new Timer();
        gameTimer.scheduleAtFixedRate(new GameTimerTask(), 0, 1000);
    }

    private void initializeGame() {
        player = new Athlete("You", Color.RED);
        competitors = new ArrayList<>();

        // Create 7 AI competitors with different colors
        Color[] colors = {Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE,
                Color.CYAN, Color.MAGENTA, Color.PINK};
        String[] names = {"USA", "CHN", "GBR", "GER", "JPN", "FRA", "ITA"};

        for (int i = 0; i < 7; i++) {
            Athlete comp = new Athlete(names[i], colors[i]);
            competitors.add(comp);
        }
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        gamePanel = new GamePanel();
        controlPanel = new ControlPanel();
        scorePanel = new ScorePanel();

        add(gamePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        add(scorePanel, BorderLayout.EAST);
    }

    private void setupKeyBindings() {
        InputMap inputMap = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = gamePanel.getActionMap();

        // Space for action/start
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "action");
        actionMap.put("action", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSpacePress();
            }
        });

        // Arrow keys for controls
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
        actionMap.put("left", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLeftPress();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
        actionMap.put("right", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRightPress();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        actionMap.put("up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleUpPress();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        actionMap.put("down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleDownPress();
            }
        });
    }

    private void handleSpacePress() {
        if (currentState == GameState.MAIN_MENU) {
            currentState = GameState.RUNNING;
            startNewRound();
        } else if (currentState == GameState.RUNNING && !isAnimating) {
            performSportAction();
        } else if (currentState == GameState.GAME_OVER) {
            resetGame();
            currentState = GameState.RUNNING;
            startNewRound();
        }
        gamePanel.repaint();
    }

    private void handleLeftPress() {
        if (currentState == GameState.RUNNING && !isAnimating) {
            adjustSportValue(-5);
        }
    }

    private void handleRightPress() {
        if (currentState == GameState.RUNNING && !isAnimating) {
            adjustSportValue(5);
        }
    }

    private void handleUpPress() {
        if (currentState == GameState.RUNNING && !isAnimating) {
            adjustSportValue(1);
        }
    }

    private void handleDownPress() {
        if (currentState == GameState.RUNNING && !isAnimating) {
            adjustSportValue(-1);
        }
    }

    private void adjustSportValue(int delta) {
        switch (currentSport) {
            case ARCHERY:
                arrowPower = Math.max(10, Math.min(100, arrowPower + delta));
                break;
            case WEIGHTLIFTING:
                weightHeight = Math.max(0, Math.min(150, weightHeight + delta));
                break;
        }
        gamePanel.repaint();
    }

    private void performSportAction() {
        isAnimating = true;
        animationFrame = 0;

        switch (currentSport) {
            case SPRINT:
                startSprintAnimation();
                break;
            case SWIMMING:
                startSwimmingAnimation();
                break;
            case ARCHERY:
                startArcheryAnimation();
                break;
            case WEIGHTLIFTING:
                startWeightliftingAnimation();
                break;
            case DIVING:
                startDivingAnimation();
                break;
        }
    }

    private void startSprintAnimation() {
        new Thread(() -> {
            try {
                sprintPosition = 50;
                int playerSpeed = 80 + random.nextInt(20); // 80-100

                for (int i = 0; i < 100; i++) {
                    animationFrame++;
                    sprintPosition += playerSpeed / 20;

                    // Move competitors
                    for (Athlete comp : competitors) {
                        int compSpeed = 70 + random.nextInt(30);
                        comp.position += compSpeed / 20;
                    }

                    gamePanel.repaint();
                    Thread.sleep(50);
                }

                calculateResults();
                isAnimating = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startSwimmingAnimation() {
        new Thread(() -> {
            try {
                swimmingPosition = 100;
                int playerSpeed = 60 + random.nextInt(25); // 60-85

                for (int i = 0; i < 80; i++) {
                    animationFrame++;
                    swimmingPosition += playerSpeed / 20;

                    gamePanel.repaint();
                    Thread.sleep(60);
                }

                calculateResults();
                isAnimating = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startArcheryAnimation() {
        new Thread(() -> {
            try {
                // Simulate arrow flight
                for (int i = 0; i < 50; i++) {
                    animationFrame++;
                    arrowAngle += random.nextDouble() * 2 - 1; // slight wobble
                    gamePanel.repaint();
                    Thread.sleep(30);
                }

                // Calculate score based on accuracy
                double accuracy = Math.max(0, 100 - Math.abs(arrowPower - 75));
                score += (int)(accuracy / 10);

                isAnimating = false;
                nextSport();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startWeightliftingAnimation() {
        new Thread(() -> {
            try {
                for (int i = 0; i < 30; i++) {
                    animationFrame++;
                    weightHeight = Math.min(150, weightHeight + 5);
                    gamePanel.repaint();
                    Thread.sleep(50);
                }

                // Hold at top
                Thread.sleep(500);

                // Lower
                for (int i = 0; i < 30; i++) {
                    animationFrame++;
                    weightHeight = Math.max(0, weightHeight - 5);
                    gamePanel.repaint();
                    Thread.sleep(50);
                }

                score += weightHeight / 10;
                isAnimating = false;
                nextSport();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startDivingAnimation() {
        new Thread(() -> {
            try {
                diveHeight = 200;
                diveRotation = 0;

                // Jump up
                for (int i = 0; i < 20; i++) {
                    animationFrame++;
                    diveHeight += 5;
                    diveRotation += 18; // 360度旋转
                    gamePanel.repaint();
                    Thread.sleep(50);
                }

                // Fall down with rotation
                for (int i = 0; i < 30; i++) {
                    animationFrame++;
                    diveHeight -= 8;
                    diveRotation += 27; // 继续旋转
                    gamePanel.repaint();
                    Thread.sleep(50);
                }

                // Splash
                animationFrame = 100;
                gamePanel.repaint();
                Thread.sleep(500);

                score += 50 + random.nextInt(50); // 50-100 points
                isAnimating = false;
                nextSport();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void calculateResults() {
        // Simple result calculation
        int position = 1;
        for (Athlete comp : competitors) {
            if (comp.position > sprintPosition) {
                position++;
            }
        }

        score += (8 - position) * 10; // 1st: 70, 2nd: 60, ..., 8th: 0

        nextSport();
    }

    private void nextSport() {
        Sport[] sports = Sport.values();
        currentSport = sports[(currentSport.ordinal() + 1) % sports.length];
        round++;

        // Reset positions
        sprintPosition = 50;
        swimmingPosition = 100;
        arrowPower = 50;
        weightHeight = 0;
        diveHeight = 200;
        diveRotation = 0;

        for (Athlete comp : competitors) {
            comp.position = 50;
        }

        if (round > 10) {
            currentState = GameState.GAME_OVER;
        }

        gamePanel.repaint();
    }

    private void startNewRound() {
        round = 1;
        score = 0;
        timeRemaining = 120;
        currentSport = Sport.SPRINT;
    }

    private void resetGame() {
        score = 0;
        round = 1;
        timeRemaining = 120;
        currentSport = Sport.SPRINT;
        isAnimating = false;
    }

    // Inner classes
    class GamePanel extends JPanel {
        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH - 200, HEIGHT));
            setBackground(BG_COLOR);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            switch (currentState) {
                case MAIN_MENU:
                    drawMainMenu(g2d);
                    break;
                case RUNNING:
                case PAUSED:
                    drawSport(g2d);
                    drawAthletes(g2d);
                    drawUI(g2d);
                    break;
                case GAME_OVER:
                    drawGameOver(g2d);
                    break;
            }
        }

        private void drawMainMenu(Graphics2D g2d) {
            // Title
            g2d.setFont(new Font("Arial", Font.BOLD, 60));
            g2d.setColor(Color.YELLOW);
            drawCenteredString(g2d, "OLYMPIC SPORT SIMULATOR", getHeight() / 4);

            // Subtitle
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            g2d.setColor(Color.WHITE);
            drawCenteredString(g2d, "Paris 2024 Edition", getHeight() / 3);

            // Instructions
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            drawCenteredString(g2d, "Press SPACE to Start", getHeight() / 2);

            drawCenteredString(g2d, "Use ARROW KEYS to control power/height", getHeight() / 2 + 50);
            drawCenteredString(g2d, "SPACE to perform action", getHeight() / 2 + 90);

            // Sports list
            g2d.setColor(Color.CYAN);
            drawCenteredString(g2d, "Sports: 100m Sprint, 100m Swimming, Archery,", getHeight() / 2 + 150);
            drawCenteredString(g2d, "Weightlifting, 10m Platform Diving", getHeight() / 2 + 180);
        }

        private void drawSport(Graphics2D g2d) {
            switch (currentSport) {
                case SPRINT:
                    drawSprintTrack(g2d);
                    break;
                case SWIMMING:
                    drawSwimmingPool(g2d);
                    break;
                case ARCHERY:
                    drawArcheryRange(g2d);
                    break;
                case WEIGHTLIFTING:
                    drawWeightliftingPlatform(g2d);
                    break;
                case DIVING:
                    drawDivingPlatform(g2d);
                    break;
            }
        }

        private void drawSprintTrack(Graphics2D g2d) {
            // Track
            g2d.setColor(TRACK_COLOR);
            g2d.fillRect(50, 200, getWidth() - 100, 300);

            // Lane lines
            g2d.setColor(Color.WHITE);
            for (int i = 0; i < 8; i++) {
                int y = 200 + i * 35;
                g2d.drawLine(50, y, getWidth() - 50, y);
            }

            // Start line
            g2d.setColor(Color.RED);
            g2d.fillRect(100, 200, 10, 280);

            // Finish line
            g2d.setColor(new Color(255, 215, 0)); // Gold
            g2d.fillRect(getWidth() - 150, 200, 10, 280);

            // Track markings
            g2d.setColor(Color.WHITE);
            for (int i = 100; i < getWidth() - 100; i += 50) {
                g2d.drawLine(i, 200, i, 480);
            }
        }

        private void drawSwimmingPool(Graphics2D g2d) {
            // Pool
            g2d.setColor(POOL_COLOR);
            g2d.fillRect(50, 200, getWidth() - 100, 300);

            // Lane dividers
            g2d.setColor(Color.WHITE);
            for (int i = 0; i < 8; i++) {
                int y = 200 + i * 35;
                for (int x = 50; x < getWidth() - 50; x += 20) {
                    g2d.fillRect(x, y + 15, 10, 5);
                }
            }

            // Start wall
            g2d.setColor(Color.RED);
            g2d.fillRect(100, 200, 15, 280);

            // Turn flags
            for (int i = 0; i < 8; i++) {
                int y = 200 + i * 35 + 10;
                g2d.setColor(Color.YELLOW);
                g2d.fillRect(getWidth() - 100, y, 5, 15);
            }
        }

        private void drawArcheryRange(Graphics2D g2d) {
            // Range background
            g2d.setColor(FIELD_COLOR);
            g2d.fillRect(0, 200, getWidth(), 300);

            // Target
            int targetX = getWidth() - 200;
            int targetY = 350;

            // Target circles
            g2d.setColor(Color.WHITE);
            g2d.fillOval(targetX - 80, targetY - 80, 160, 160);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(targetX - 60, targetY - 60, 120, 120);
            g2d.setColor(Color.BLUE);
            g2d.fillOval(targetX - 40, targetY - 40, 80, 80);
            g2d.setColor(Color.RED);
            g2d.fillOval(targetX - 20, targetY - 20, 40, 40);
            g2d.setColor(Color.YELLOW);
            g2d.fillOval(targetX - 10, targetY - 10, 20, 20);

            // Arrow trajectory
            if (isAnimating) {
                g2d.setColor(Color.ORANGE);
                int startX = 150;
                int startY = 400;
                int endX = targetX + (int)(Math.cos(Math.toRadians(arrowAngle)) * animationFrame * 5);
                int endY = startY - (int)(Math.sin(Math.toRadians(arrowAngle)) * animationFrame * 5
                        - 0.5 * 9.8 * Math.pow(animationFrame * 0.1, 2));

                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(startX, startY, endX, endY);

                // Draw arrow
                g2d.setColor(Color.BLACK);
                Polygon arrowHead = new Polygon();
                arrowHead.addPoint(endX, endY);
                arrowHead.addPoint(endX - 10, endY - 5);
                arrowHead.addPoint(endX - 10, endY + 5);
                g2d.fillPolygon(arrowHead);
            }

            // Power meter
            g2d.setColor(Color.WHITE);
            g2d.drawRect(50, 500, 200, 30);
            g2d.setColor(Color.GREEN);
            g2d.fillRect(50, 500, (int)(arrowPower * 2), 30);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Power: " + (int)arrowPower + "%", 60, 520);
        }

        private void drawWeightliftingPlatform(Graphics2D g2d) {
            // Platform
            g2d.setColor(Color.GRAY);
            g2d.fillRect(getWidth()/2 - 100, 400, 200, 50);

            // Barbell
            g2d.setColor(Color.BLACK);
            g2d.fillRect(getWidth()/2 - 80, 350 - weightHeight, 160, 10);

            // Weights
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillOval(getWidth()/2 - 100, 340 - weightHeight, 40, 40);
            g2d.fillOval(getWidth()/2 + 60, 340 - weightHeight, 40, 40);

            // Athlete
            g2d.setColor(Color.RED);
            g2d.fillOval(getWidth()/2 - 20, 380 - weightHeight/2, 40, 40);
            g2d.fillRect(getWidth()/2 - 10, 420 - weightHeight/2, 20, 100);

            // Weight display
            g2d.setColor(Color.WHITE);
            g2d.drawString("Weight Height: " + weightHeight + " cm", getWidth()/2 - 80, 550);
        }

        private void drawDivingPlatform(Graphics2D g2d) {
            // Pool
            g2d.setColor(POOL_COLOR);
            g2d.fillRect(0, 400, getWidth(), 200);

            // Platform
            g2d.setColor(Color.GRAY);
            g2d.fillRect(getWidth()/2 - 50, 200, 100, 20);

            // Diver
            g2d.setColor(Color.RED);
            g2d.translate(getWidth()/2, 200 - diveHeight);
            g2d.rotate(Math.toRadians(diveRotation));

            // Draw diver body
            g2d.fillOval(-15, -20, 30, 30); // Head
            g2d.fillRect(-10, 10, 20, 40); // Body
            g2d.fillRect(-25, 10, 15, 10); // Left arm
            g2d.fillRect(10, 10, 15, 10);  // Right arm
            g2d.fillRect(-15, 50, 10, 30); // Left leg
            g2d.fillRect(5, 50, 10, 30);   // Right leg

            g2d.rotate(-Math.toRadians(diveRotation));
            g2d.translate(-getWidth()/2, -200 + diveHeight);

            // Splash effect
            if (animationFrame > 90) {
                g2d.setColor(new Color(255, 255, 255, 150));
                for (int i = 0; i < 20; i++) {
                    int x = getWidth()/2 - 50 + random.nextInt(100);
                    int y = 400 + random.nextInt(50);
                    int size = 10 + random.nextInt(30);
                    g2d.fillOval(x, y, size, size);
                }
            }
        }

        private void drawAthletes(Graphics2D g2d) {
            if (currentSport == Sport.SPRINT || currentSport == Sport.SWIMING) {
                int baseY = currentSport == Sport.SPRINT ? 200 : 200;

                // Draw player
                g2d.setColor(player.color);
                int playerY = baseY + 18;
                g2d.fillOval(sprintPosition, playerY, 20, 20);
                g2d.fillRect(sprintPosition + 10, playerY + 20, 5, 30);

                // Draw competitors
                for (int i = 0; i < competitors.size(); i++) {
                    Athlete comp = competitors.get(i);
                    g2d.setColor(comp.color);
                    int compY = baseY + i * 35 + 18;
                    g2d.fillOval(comp.position, compY, 20, 20);
                    g2d.fillRect(comp.position + 10, compY + 20, 5, 30);
                }
            }
        }

        private void drawUI(Graphics2D g2d) {
            // Sport name
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.setColor(Color.YELLOW);
            drawCenteredString(g2d, currentSport.toString(), 50);

            // Round and score
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            g2d.setColor(Color.WHITE);
            g2d.drawString("Round: " + round + "/10", 20, 100);
            g2d.drawString("Score: " + score, 20, 130);
            g2d.drawString("Time: " + timeRemaining + "s", 20, 160);

            // Instructions
            g2d.setColor(Color.CYAN);
            switch (currentSport) {
                case SPRINT:
                case SWIMMING:
                    g2d.drawString("Press SPACE to start race", getWidth() - 300, 100);
                    break;
                case ARCHERY:
                    g2d.drawString("Use UP/DOWN to adjust power", getWidth() - 300, 100);
                    g2d.drawString("Press SPACE to shoot", getWidth() - 300, 130);
                    break;
                case WEIGHTLIFTING:
                    g2d.drawString("Use UP/DOWN to lift weight", getWidth() - 300, 100);
                    g2d.drawString("Press SPACE to start lift", getWidth() - 300, 130);
                    break;
                case DIVING:
                    g2d.drawString("Press SPACE to perform dive", getWidth() - 300, 100);
                    break;
            }

            // Animation indicator
            if (isAnimating) {
                g2d.setColor(Color.GREEN);
                g2d.drawString("Animating...", getWidth() - 300, getHeight() - 50);
            }
        }

        private void drawGameOver(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setFont(new Font("Arial", Font.BOLD, 60));
            g2d.setColor(Color.RED);
            drawCenteredString(g2d, "GAME OVER", getHeight() / 3);

            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            g2d.setColor(Color.YELLOW);
            drawCenteredString(g2d, "Final Score: " + score, getHeight() / 2);

            g2d.setFont(new Font("Arial", Font.PLAIN, 30));
            g2d.setColor(Color.WHITE);
            drawCenteredString(g2d, "Press SPACE to Play Again", getHeight() / 2 + 100);

            // Medal ceremony
            int medalY = getHeight() / 2 + 150;
            g2d.setColor(new Color(255, 215, 0)); // Gold
            g2d.fillOval(getWidth()/2 - 100, medalY, 60, 60);
            g2d.setColor(new Color(192, 192, 192)); // Silver
            g2d.fillOval(getWidth()/2 - 30, medalY, 60, 60);
            g2d.setColor(new Color(205, 127, 50)); // Bronze
            g2d.fillOval(getWidth()/2 + 40, medalY, 60, 60);
        }

        private void drawCenteredString(Graphics2D g2d, String text, int y) {
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            g2d.drawString(text, x, y);
        }
    }

    class ControlPanel extends JPanel {
        private JButton startButton;
        private JButton pauseButton;
        private JComboBox<String> sportSelector;

        public ControlPanel() {
            setPreferredSize(new Dimension(WIDTH, 50));
            setBackground(new Color(50, 50, 50));

            startButton = new JButton("Start Game");
            pauseButton = new JButton("Pause");
            sportSelector = new JComboBox<>(new String[]{"Sprint", "Swimming", "Archery", "Weightlifting", "Diving"});

            startButton.addActionListener(e -> {
                if (currentState == GameState.MAIN_MENU) {
                    currentState = GameState.RUNNING;
                    startNewRound();
                    gamePanel.repaint();
                }
            });

            pauseButton.addActionListener(e -> {
                if (currentState == GameState.RUNNING) {
                    currentState = GameState.PAUSED;
                } else if (currentState == GameState.PAUSED) {
                    currentState = GameState.RUNNING;
                }
                gamePanel.repaint();
            });

            sportSelector.addActionListener(e -> {
                if (!isAnimating) {
                    String selected = (String)sportSelector.getSelectedItem();
                    currentSport = Sport.valueOf(selected.toUpperCase());
                    gamePanel.repaint();
                }
            });

            add(startButton);
            add(pauseButton);
            add(new JLabel("Select Sport:"));
            add(sportSelector);
        }
    }

    class ScorePanel extends JPanel {
        public ScorePanel() {
            setPreferredSize(new Dimension(200, HEIGHT));
            setBackground(new Color(40, 40, 40));
            setLayout(new BorderLayout());

            JLabel title = new JLabel("SCOREBOARD", SwingConstants.CENTER);
            title.setForeground(Color.YELLOW);
            title.setFont(new Font("Arial", Font.BOLD, 24));
            add(title, BorderLayout.NORTH);

            // Would add actual scoreboard here
            JTextArea scoreArea = new JTextArea();
            scoreArea.setEditable(false);
            scoreArea.setBackground(new Color(60, 60, 60));
            scoreArea.setForeground(Color.WHITE);
            scoreArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

            // Add some sample scores
            scoreArea.setText("Current Rankings:\n\n");
            scoreArea.append("1. YOU: " + score + "\n");
            scoreArea.append("2. USA: 450\n");
            scoreArea.append("3. CHN: 420\n");
            scoreArea.append("4. GBR: 380\n");
            scoreArea.append("5. GER: 350\n");
            scoreArea.append("6. JPN: 320\n");
            scoreArea.append("7. FRA: 290\n");
            scoreArea.append("8. ITA: 250\n");

            add(new JScrollPane(scoreArea), BorderLayout.CENTER);
        }
    }

    class Athlete {
        String name;
        Color color;
        int position = 50;
        int score = 0;

        public Athlete(String name, Color color) {
            this.name = name;
            this.color = color;
        }
    }

    class GameTimerTask extends TimerTask {
        @Override
        public void run() {
            if (currentState == GameState.RUNNING && !isAnimating) {
                timeRemaining--;
                if (timeRemaining <= 0) {
                    currentState = GameState.GAME_OVER;
                }
                gamePanel.repaint();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OlympicSportSimulator game = new OlympicSportSimulator();
            game.setVisible(true);
        });
    }
}