// ArcheryTournamentGame.java
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class ArcheryTournamentGame extends JFrame {
    private GamePanel gamePanel;

    public ArcheryTournamentGame() {
        setTitle("Archery Tournament at Giza Pyramids");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        // Start with menu
        gamePanel.showMenu();

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ArcheryTournamentGame());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private enum GameState { MENU, PLAYING, GAME_OVER }
    private GameState state = GameState.MENU;

    // Game objects
    private Target target;
    private Arrow arrow;
    private Player player1, player2, computer;
    private Player currentPlayer;
    private boolean isComputerMode;
    private int currentRound;
    private int maxRounds = 5;

    // Animation
    private Timer gameTimer;
    private Timer arrowAnimationTimer;
    private double arrowAngle;
    private double arrowPower;
    private boolean isDrawingArrow;
    private boolean isAnimating;
    private Point2D arrowStartPos;
    private Point2D arrowCurrentPos;
    private double animationProgress;
    private int animationDuration = 30;
    private int animationFrame = 0;

    // Background animation
    private float sunAngle = 0;
    private List<Particle> particles = new ArrayList<>();
    private List<Cloud> clouds = new ArrayList<>();

    // Colors
    private Color sandColor = new Color(237, 201, 135);
    private Color skyColor = new Color(135, 206, 235);
    private Color pyramidColor = new Color(205, 133, 63);

    // UI elements
    private JButton onePlayerButton, twoPlayerButton, restartButton, menuButton;
    private JLabel statusLabel, scoreLabel1, scoreLabel2, roundLabel;

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        setLayout(null);

        initializeGame();
        initializeUI();

        gameTimer = new Timer(50, this);
        gameTimer.start();

        // Initialize particles for atmosphere
        for (int i = 0; i < 50; i++) {
            particles.add(new Particle());
        }

        // Initialize clouds
        for (int i = 0; i < 5; i++) {
            clouds.add(new Cloud());
        }
    }

    private void initializeUI() {
        // Buttons
        onePlayerButton = new JButton("Play vs Computer");
        onePlayerButton.setBounds(450, 400, 300, 50);
        onePlayerButton.setFont(new Font("Arial", Font.BOLD, 18));
        onePlayerButton.addActionListener(e -> startGame(true));

        twoPlayerButton = new JButton("2 Players");
        twoPlayerButton.setBounds(450, 470, 300, 50);
        twoPlayerButton.setFont(new Font("Arial", Font.BOLD, 18));
        twoPlayerButton.addActionListener(e -> startGame(false));

        restartButton = new JButton("Restart Game");
        restartButton.setBounds(50, 700, 150, 40);
        restartButton.setFont(new Font("Arial", Font.BOLD, 14));
        restartButton.addActionListener(e -> restartGame());
        restartButton.setVisible(false);

        menuButton = new JButton("Main Menu");
        menuButton.setBounds(220, 700, 150, 40);
        menuButton.setFont(new Font("Arial", Font.BOLD, 14));
        menuButton.addActionListener(e -> showMenu());
        menuButton.setVisible(false);

        // Labels
        statusLabel = new JLabel("");
        statusLabel.setBounds(50, 50, 400, 30);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setForeground(Color.WHITE);

        scoreLabel1 = new JLabel("");
        scoreLabel1.setBounds(50, 90, 300, 30);
        scoreLabel1.setFont(new Font("Arial", Font.BOLD, 14));
        scoreLabel1.setForeground(Color.WHITE);

        scoreLabel2 = new JLabel("");
        scoreLabel2.setBounds(50, 120, 300, 30);
        scoreLabel2.setFont(new Font("Arial", Font.BOLD, 14));
        scoreLabel2.setForeground(Color.WHITE);

        roundLabel = new JLabel("");
        roundLabel.setBounds(50, 150, 300, 30);
        roundLabel.setFont(new Font("Arial", Font.BOLD, 14));
        roundLabel.setForeground(Color.WHITE);

        add(onePlayerButton);
        add(twoPlayerButton);
        add(restartButton);
        add(menuButton);
        add(statusLabel);
        add(scoreLabel1);
        add(scoreLabel2);
        add(roundLabel);
    }

    private void initializeGame() {
        target = new Target(950, 350);
        player1 = new Player("Player 1", Color.BLUE);
        player2 = new Player("Player 2", Color.RED);
        computer = new Player("Computer", Color.GREEN);
        arrow = new Arrow();
        arrowAngle = -Math.PI / 4;
        arrowPower = 0;
        isDrawingArrow = false;
        isAnimating = false;
    }

    private void startGame(boolean vsComputer) {
        isComputerMode = vsComputer;
        currentPlayer = player1;
        currentRound = 1;
        player1.resetScore();
        player2.resetScore();
        computer.resetScore();
        arrowPower = 0;
        arrowAngle = -Math.PI / 4;
        state = GameState.PLAYING;

        onePlayerButton.setVisible(false);
        twoPlayerButton.setVisible(false);
        restartButton.setVisible(true);
        menuButton.setVisible(true);

        updateUI();
        requestFocus();
    }

    private void restartGame() {
        if (isComputerMode) {
            startGame(true);
        } else {
            startGame(false);
        }
    }

    void showMenu() {
        state = GameState.MENU;
        onePlayerButton.setVisible(true);
        twoPlayerButton.setVisible(true);
        restartButton.setVisible(false);
        menuButton.setVisible(false);
        statusLabel.setText("");
        scoreLabel1.setText("");
        scoreLabel2.setText("");
        roundLabel.setText("");
        repaint();
    }

    public void updateUI() {
        if (state == GameState.PLAYING) {
            statusLabel.setText("Current Player: " + currentPlayer.name + "'s Turn");
            scoreLabel1.setText(player1.name + " Score: " + player1.score);

            if (isComputerMode) {
                scoreLabel2.setText("Computer Score: " + computer.score);
                roundLabel.setText("Round: " + currentRound + "/" + maxRounds);
            } else {
                scoreLabel2.setText(player2.name + " Score: " + player2.score);
                roundLabel.setText("Round: " + currentRound + "/" + maxRounds);
            }
        } else if (state == GameState.GAME_OVER) {
            String winner;
            if (isComputerMode) {
                if (player1.score > computer.score) {
                    winner = player1.name + " Wins!";
                } else if (computer.score > player1.score) {
                    winner = "Computer Wins!";
                } else {
                    winner = "It's a Tie!";
                }
            } else {
                if (player1.score > player2.score) {
                    winner = player1.name + " Wins!";
                } else if (player2.score > player1.score) {
                    winner = player2.name + " Wins!";
                } else {
                    winner = "It's a Tie!";
                }
            }
            statusLabel.setText("Game Over! " + winner);
        }
    }

    private void shootArrow() {
        if (isAnimating) return;

        int score = target.calculateScore(arrowAngle, arrowPower);
        currentPlayer.addScore(score);

        // Start animation
        isAnimating = true;
        animationFrame = 0;
        arrowStartPos = new Point2D.Double(100, 400);

        // Calculate end position based on angle and power
        double endX = 950 + (arrowPower * Math.cos(arrowAngle)) * 3;
        double endY = 350 + (arrowPower * Math.sin(arrowAngle)) * 3;
        arrowCurrentPos = arrowStartPos;

        arrowAnimationTimer = new Timer(20, e -> {
            animationFrame++;
            double t = (double) animationFrame / animationDuration;
            arrowCurrentPos = new Point2D.Double(
                    arrowStartPos.getX() + (endX - arrowStartPos.getX()) * t,
                    arrowStartPos.getY() + (endY - arrowStartPos.getY()) * t
            );

            if (animationFrame >= animationDuration) {
                arrowAnimationTimer.stop();
                isAnimating = false;

                // Show score popup
                showScorePopup(score);

                // Switch players
                switchPlayer();

                // Check for computer turn
                if (isComputerMode && currentPlayer == computer && state == GameState.PLAYING) {
                    computerTurn();
                }
            }
            repaint();
        });
        arrowAnimationTimer.start();

        // Reset power and angle for next turn
        arrowPower = 0;
        arrowAngle = -Math.PI / 4;
        repaint();
    }

    private void showScorePopup(int score) {
        JDialog popup = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Score!", true);
        popup.setSize(200, 100);
        popup.setLocationRelativeTo(this);

        JLabel label = new JLabel(currentPlayer.name + " scored " + score + " points!", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        popup.add(label);

        Timer timer = new Timer(1500, e -> popup.dispose());
        timer.setRepeats(false);
        timer.start();

        popup.setVisible(true);
    }

    private void switchPlayer() {
        if (isComputerMode) {
            if (currentPlayer == player1) {
                currentPlayer = computer;
            } else {
                currentPlayer = player1;
                currentRound++;
                if (currentRound > maxRounds) {
                    endGame();
                }
            }
        } else {
            if (currentPlayer == player1) {
                currentPlayer = player2;
            } else {
                currentPlayer = player1;
                currentRound++;
                if (currentRound > maxRounds) {
                    endGame();
                }
            }
        }
        updateUI();
    }

    private void computerTurn() {
        Timer timer = new Timer(1000, e -> {
            // Simple AI: random power and angle
            arrowPower = 50 + Math.random() * 150;
            arrowAngle = -Math.PI / 3 + (Math.random() * Math.PI / 6);
            shootArrow();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void endGame() {
        state = GameState.GAME_OVER;
        updateUI();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw sky gradient
        GradientPaint skyGradient = new GradientPaint(0, 0, skyColor, 0, 600, new Color(100, 150, 200));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw sun
        sunAngle += 0.02f;
        int sunX = 1000 + (int)(Math.sin(sunAngle) * 50);
        g2d.setColor(new Color(255, 200, 100));
        g2d.fillOval(sunX, 80, 80, 80);

        // Draw clouds
        for (Cloud cloud : clouds) {
            cloud.update();
            cloud.draw(g2d);
        }

        // Draw sand dunes
        g2d.setColor(sandColor);
        g2d.fillRect(0, 500, getWidth(), 300);

        // Draw pyramids
        drawPyramid(g2d, 150, 450, 200, 250);
        drawPyramid(g2d, 350, 480, 150, 180);
        drawPyramid(g2d, 500, 420, 280, 320);

        // Draw sphinx-like structure
        drawSphinx(g2d, 700, 520);

        // Draw particles (sand effects)
        for (Particle p : particles) {
            p.update();
            p.draw(g2d);
        }

        // Draw target
        target.draw(g2d);

        // Draw arrow being drawn
        if (state == GameState.PLAYING && !isAnimating && !(isComputerMode && currentPlayer == computer)) {
            drawBowAndArrow(g2d);
        }

        // Draw animating arrow
        if (isAnimating && arrowCurrentPos != null) {
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine((int)arrowCurrentPos.getX(), (int)arrowCurrentPos.getY(),
                    (int)(arrowCurrentPos.getX() + 20), (int)(arrowCurrentPos.getY() + 5));
            g2d.fillOval((int)arrowCurrentPos.getX() - 3, (int)arrowCurrentPos.getY() - 3, 6, 6);
        }

        // Draw power meter
        if (state == GameState.PLAYING && !isAnimating && !(isComputerMode && currentPlayer == computer)) {
            drawPowerMeter(g2d);
        }

        // Draw menu if in menu state
        if (state == GameState.MENU) {
            drawMenuOverlay(g2d);
        }
    }

    private void drawPyramid(Graphics2D g2d, int x, int y, int width, int height) {
        // Create pyramid shape
        int[] xPoints = {x, x + width/2, x + width};
        int[] yPoints = {y, y - height, y};

        // Gradient for pyramid
        GradientPaint pyramidGrad = new GradientPaint(x, y, pyramidColor, x + width/2, y - height/2, new Color(139, 69, 19));
        g2d.setPaint(pyramidGrad);
        g2d.fillPolygon(xPoints, yPoints, 3);

        // Draw stone lines
        g2d.setColor(new Color(100, 50, 20));
        g2d.setStroke(new BasicStroke(1));
        for (int i = 1; i <= 5; i++) {
            int levelY = y - (height * i / 6);
            int levelWidth = width * (6 - i) / 6;
            if (levelWidth > 0) {
                g2d.drawLine(x + (width - levelWidth)/2, levelY, x + width - (width - levelWidth)/2, levelY);
            }
        }

        // Draw shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        int[] shadowX = {x + width/2, x + width + 20, x + width/2 + 20};
        int[] shadowY = {y, y + 30, y + 30};
        g2d.fillPolygon(shadowX, shadowY, 3);
    }

    private void drawSphinx(Graphics2D g2d, int x, int y) {
        g2d.setColor(new Color(180, 140, 100));
        // Body
        g2d.fillRect(x, y, 100, 60);
        // Head
        g2d.fillOval(x + 30, y - 40, 40, 50);
        // Face features
        g2d.setColor(Color.BLACK);
        g2d.fillOval(x + 42, y - 30, 5, 5);
        g2d.fillOval(x + 53, y - 30, 5, 5);
        g2d.drawArc(x + 45, y - 20, 15, 10, 0, -180);
    }

    private void drawBowAndArrow(Graphics2D g2d) {
        int bowX = 100;
        int bowY = 400;

        // Draw bow
        g2d.setColor(new Color(101, 67, 33));
        g2d.setStroke(new BasicStroke(5));

        double arrowEndX = bowX + arrowPower * Math.cos(arrowAngle);
        double arrowEndY = bowY + arrowPower * Math.sin(arrowAngle);

        // Bow string
        g2d.drawLine(bowX - 20, bowY - 20, (int)arrowEndX, (int)arrowEndY);
        g2d.drawLine(bowX + 20, bowY - 20, (int)arrowEndX, (int)arrowEndY);

        // Bow body
        g2d.drawArc(bowX - 30, bowY - 40, 60, 80, 0, 180);

        // Arrow
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(bowX, bowY, (int)arrowEndX, (int)arrowEndY);
        g2d.fillOval((int)arrowEndX - 3, (int)arrowEndY - 3, 6, 6);

        // Arrow feathers
        g2d.setColor(Color.RED);
        double featherAngle = Math.atan2(arrowEndY - bowY, arrowEndX - bowX);
        double perpAngle = featherAngle + Math.PI / 2;
        for (int i = 0; i < 3; i++) {
            int fx = (int)(bowX + (arrowPower * 0.7) * Math.cos(featherAngle));
            int fy = (int)(bowY + (arrowPower * 0.7) * Math.sin(featherAngle));
            g2d.drawLine(fx, fy,
                    (int)(fx + 8 * Math.cos(perpAngle + i * Math.PI/3)),
                    (int)(fy + 8 * Math.sin(perpAngle + i * Math.PI/3)));
        }
    }

    private void drawPowerMeter(Graphics2D g2d) {
        int meterX = 50;
        int meterY = 600;
        int meterWidth = 200;
        int meterHeight = 20;

        // Background
        g2d.setColor(Color.GRAY);
        g2d.fillRect(meterX, meterY, meterWidth, meterHeight);

        // Power level
        g2d.setColor(Color.GREEN);
        g2d.fillRect(meterX, meterY, (int)(meterWidth * arrowPower / 200), meterHeight);

        // Border
        g2d.setColor(Color.BLACK);
        g2d.drawRect(meterX, meterY, meterWidth, meterHeight);

        // Labels
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("Power: " + (int)arrowPower, meterX, meterY - 5);
        g2d.drawString("Angle: " + (int)Math.toDegrees(arrowAngle) + "°", meterX, meterY - 20);

        // Instructions
        g2d.drawString("Hold SPACE to draw, release to shoot!", meterX, meterY - 40);
    }

    private void drawMenuOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String title = "Archery Tournament";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, getWidth()/2 - titleWidth/2, 200);

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        String subtitle = "at the Giza Pyramids";
        int subWidth = fm.stringWidth(subtitle);
        g2d.drawString(subtitle, getWidth()/2 - subWidth/2, 260);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.PLAYING) {
            repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (state != GameState.PLAYING) return;
        if (isAnimating) return;
        if (isComputerMode && currentPlayer == computer) return;

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            isDrawingArrow = true;
        } else if (e.getKeyCode() == KeyEvent.VK_UP && isDrawingArrow) {
            arrowAngle = Math.min(arrowAngle + 0.05, -Math.PI / 8);
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN && isDrawingArrow) {
            arrowAngle = Math.max(arrowAngle - 0.05, -Math.PI / 1.5);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && isDrawingArrow && state == GameState.PLAYING) {
            isDrawingArrow = false;
            if (!isAnimating) {
                shootArrow();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes
    class Target {
        int x, y;

        Target(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g2d) {
            int[] radii = {80, 65, 50, 35, 20};
            Color[] colors = {Color.WHITE, Color.BLACK, Color.BLUE, Color.RED, Color.YELLOW};

            for (int i = 0; i < radii.length; i++) {
                g2d.setColor(colors[i]);
                g2d.fillOval(x - radii[i], y - radii[i], radii[i] * 2, radii[i] * 2);
            }

            // Draw stand
            g2d.setColor(new Color(101, 67, 33));
            g2d.fillRect(x - 10, y + 20, 20, 100);
            g2d.fillRect(x - 60, y + 100, 120, 15);
        }

        int calculateScore(double angle, double power) {
            double endX = 950 + power * Math.cos(angle) * 3;
            double endY = 350 + power * Math.sin(angle) * 3;
            double distance = Math.sqrt(Math.pow(endX - x, 2) + Math.pow(endY - y, 2));

            if (distance < 20) return 100;
            if (distance < 35) return 80;
            if (distance < 50) return 60;
            if (distance < 65) return 40;
            if (distance < 80) return 20;
            return 0;
        }
    }

    class Arrow {
        // Simple arrow class for reference
    }

    class Player {
        String name;
        int score;
        Color color;

        Player(String name, Color color) {
            this.name = name;
            this.color = color;
            this.score = 0;
        }

        void addScore(int points) {
            score += points;
        }

        void resetScore() {
            score = 0;
        }
    }

    class Particle {
        float x, y;
        float vx, vy;
        int life;

        Particle() {
            reset();
        }

        void reset() {
            x = (float)(Math.random() * 1200);
            y = 500 + (float)(Math.random() * 100);
            vx = (float)(Math.random() - 0.5) * 2;
            vy = (float)(Math.random() * -2);
            life = 50 + (int)(Math.random() * 100);
        }

        void update() {
            x += vx;
            y += vy;
            life--;
            if (life <= 0 || y > 800) {
                reset();
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(200, 150, 100, 100));
            g2d.fillOval((int)x, (int)y, 3, 3);
        }
    }

    class Cloud {
        float x, y;
        float speed;

        Cloud() {
            x = (float)(Math.random() * 1200);
            y = (float)(Math.random() * 200);
            speed = 0.5f + (float)Math.random();
        }

        void update() {
            x -= speed;
            if (x < -100) {
                x = 1300;
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.fillOval((int)x, (int)y, 60, 40);
            g2d.fillOval((int)x + 30, (int)y - 20, 50, 40);
            g2d.fillOval((int)x - 20, (int)y - 10, 50, 40);
        }
    }
}