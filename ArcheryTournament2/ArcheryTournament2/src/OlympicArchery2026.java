import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class OlympicArchery2026 extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
    private static final int WIDTH = 1400;
    private static final int HEIGHT = 900;
    private static final int TARGET_X = 1100;
    private static final int TARGET_Y = 150;
    private static final int TARGET_SIZE = 220;

    // Game modes
    public enum GameMode { SINGLE_PLAYER, TWO_PLAYER }
    private GameMode currentMode = GameMode.SINGLE_PLAYER;

    // Players
    private Player player1;
    private Player player2;
    private Player currentPlayer;
    private ComputerPlayer computer;
    private boolean isComputerTurn = false;

    // Game state
    private int mouseX, mouseY;
    private boolean isDragging = false;
    private double power = 0;
    private List<Arrow> arrows = new ArrayList<>();
    private Arrow currentArrow = null;
    private javax.swing.Timer gameTimer;
    private boolean gameRunning = true;
    private boolean showingResult = false;
    private int resultX, resultY;
    private int resultScore = 0;
    private int currentEnd = 1;
    private int totalEnds = 6;
    private String gameMessage = "";
    private Timer messageTimer;

    // Animation effects
    private float[] glowIntensity = new float[6];
    private boolean glowIncreasing = true;
    private List<Particle> particles = new ArrayList<>();
    private List<Firework> fireworks = new ArrayList<>();
    private Random random = new Random();

    // Olympic theme
    private Color[] olympicColors = {Color.BLUE, Color.YELLOW, Color.BLACK, Color.GREEN, Color.RED};

    // Wind effect
    private double windSpeed = 0;
    private Timer windTimer;

    class Player {
        String name;
        int score;
        int arrowsShot;
        int[] endScores;
        Color color;
        boolean isAI;

        Player(String name, Color color, boolean isAI) {
            this.name = name;
            this.color = color;
            this.isAI = isAI;
            this.score = 0;
            this.arrowsShot = 0;
            this.endScores = new int[totalEnds];
        }

        void addScore(int points) {
            score += points;
            if (arrowsShot < totalEnds * 3) {
                endScores[currentEnd - 1] += points;
            }
        }
    }

    class ComputerPlayer {
        Random random = new Random();

        void shoot() {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (isComputerTurn && gameRunning && currentPlayer.isAI) {
                        int targetX = TARGET_X + TARGET_SIZE / 2 + random.nextInt(60) - 30;
                        int targetY = TARGET_Y + TARGET_SIZE / 2 + random.nextInt(60) - 30;
                        double power = 0.7 + random.nextDouble() * 0.3;

                        int bowX = 150;
                        int bowY = HEIGHT - 200;
                        currentArrow = new Arrow(bowX, bowY, power, targetX, targetY, currentPlayer);

                        addAimingEffect(targetX, targetY);
                        timer.cancel();
                    }
                }
            }, 1000 + random.nextInt(1000));
        }

        private void addAimingEffect(int x, int y) {
            for (int i = 0; i < 20; i++) {
                particles.add(new Particle(x + random.nextInt(20) - 10,
                        y + random.nextInt(20) - 10,
                        Color.YELLOW));
            }
        }
    }

    class Arrow {
        double x, y;
        double vx, vy;
        boolean active = true;
        boolean hit = false;
        int hitScore = 0;
        double hitX, hitY;
        Player owner;
        List<Point> trail = new ArrayList<>();

        Arrow(double startX, double startY, double power, int aimX, int aimY, Player owner) {
            this.x = startX;
            this.y = startY;
            this.owner = owner;
            double angle = Math.atan2(aimY - startY, aimX - startX);
            double speed = power * 18;
            this.vx = speed * Math.cos(angle);
            this.vy = speed * Math.sin(angle);

            this.vx += windSpeed;
        }

        void update() {
            if (!active) return;

            trail.add(new Point((int)x, (int)y));
            if (trail.size() > 10) trail.remove(0);

            x += vx;
            y += vy;
            vy += 0.4;

            if (!hit && x + 10 >= TARGET_X && x - 10 <= TARGET_X + TARGET_SIZE &&
                    y + 10 >= TARGET_Y && y - 10 <= TARGET_Y + TARGET_SIZE) {
                hit = true;
                active = false;
                hitX = x;
                hitY = y;
                hitScore = calculateScore((int)x, (int)y);
                owner.addScore(hitScore);
                resultScore = hitScore;
                showingResult = true;
                resultX = (int)x;
                resultY = (int)y;

                addHitParticles((int)x, (int)y);

                if (hitScore == 10) {
                    addFirework((int)x, (int)y);
                    gameMessage = owner.name + " scored a BULLSEYE! 🎯";
                    showMessage();
                }

                Timer resultTimer = new Timer();
                resultTimer.schedule(new TimerTask() {
                    public void run() {
                        showingResult = false;
                        resultTimer.cancel();
                    }
                }, 1500);
            }

            if (x > WIDTH + 100 || x < -100 || y > HEIGHT + 100) {
                active = false;
                endTurn();
            }
        }

        void draw(Graphics2D g2d) {
            for (int i = 0; i < trail.size(); i++) {
                Point p = trail.get(i);
                float alpha = (float)i / trail.size();
                g2d.setColor(new Color(owner.color.getRed(), owner.color.getGreen(),
                        owner.color.getBlue(), (int)(alpha * 100)));
                g2d.fillOval(p.x - 2, p.y - 2, 4, 4);
            }

            if (active) {
                g2d.setColor(owner.color);
                g2d.setStroke(new BasicStroke(4));
                g2d.drawLine((int)x, (int)y, (int)(x - vx * 0.5), (int)(y - vy * 0.5));
                g2d.fillOval((int)x - 4, (int)y - 4, 8, 8);

                g2d.setColor(Color.WHITE);
                double angle = Math.atan2(vy, vx);
                int fx1 = (int)(x - vx * 0.3 + 8 * Math.sin(angle));
                int fy1 = (int)(y - vy * 0.3 - 8 * Math.cos(angle));
                int fx2 = (int)(x - vx * 0.3 - 8 * Math.sin(angle));
                int fy2 = (int)(y - vy * 0.3 + 8 * Math.cos(angle));
                g2d.drawLine((int)x, (int)y, fx1, fy1);
                g2d.drawLine((int)x, (int)y, fx2, fy2);
            } else if (hit) {
                g2d.setColor(Color.RED);
                g2d.fillOval((int)hitX - 5, (int)hitY - 5, 10, 10);
                g2d.setColor(Color.WHITE);
                g2d.fillOval((int)hitX - 2, (int)hitY - 2, 4, 4);
            }
        }
    }

    class Particle {
        double x, y;
        double vx, vy;
        int life;
        Color color;

        Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.vx = (random.nextDouble() - 0.5) * 8;
            this.vy = (random.nextDouble() - 0.5) * 8;
            this.life = 50;
            this.color = color;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.2;
            life--;
        }

        void draw(Graphics2D g2d) {
            if (life > 0) {
                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), life * 5));
                g2d.fillOval((int)x - 2, (int)y - 2, 4, 4);
            }
        }
    }

    class Firework {
        double x, y;
        int life;
        List<Particle> particles = new ArrayList<>();

        Firework(double x, double y) {
            this.x = x;
            this.y = y;
            this.life = 100;
            for (int i = 0; i < 50; i++) {
                particles.add(new Particle(x, y, olympicColors[random.nextInt(5)]));
            }
        }

        void update() {
            life--;
            for (Particle p : particles) {
                p.update();
            }
        }

        void draw(Graphics2D g2d) {
            for (Particle p : particles) {
                p.draw(g2d);
            }
        }
    }

    public OlympicArchery2026() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(20, 30, 45));
        addMouseListener(this);
        addMouseMotionListener(this);

        player1 = new Player("🇺🇸 USA", new Color(0, 102, 204), false);
        player2 = new Player("🇨🇳 CHINA", new Color(255, 51, 51), false);
        computer = new ComputerPlayer();
        currentPlayer = player1;

        // FIXED: Use javax.swing.Timer with ActionListener
        gameTimer = new javax.swing.Timer(16, this);
        gameTimer.start();

        // Wind timer - using java.util.Timer
        windTimer = new Timer();
        windTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                windSpeed = (random.nextDouble() - 0.5) * 2;
            }
        }, 0, 5000);

        // FIXED: Animation timer for target glow - using anonymous ActionListener class
        javax.swing.Timer glowTimer = new javax.swing.Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < glowIntensity.length; i++) {
                    if (glowIncreasing) {
                        glowIntensity[i] = Math.min(1.0f, glowIntensity[i] + 0.05f);
                    } else {
                        glowIntensity[i] = Math.max(0.2f, glowIntensity[i] - 0.05f);
                    }
                }

                if (glowIntensity[0] >= 1.0f) {
                    glowIncreasing = false;
                } else if (glowIntensity[0] <= 0.2f) {
                    glowIncreasing = true;
                }
            }
        });
        glowTimer.start();

        messageTimer = new Timer();

        showModeSelection();
    }

    private void showModeSelection() {
        int choice = JOptionPane.showOptionDialog(this,
                "🏹 OLYMPIC ARCHERY 2026 🏹\n\nChoose Game Mode:",
                "Olympic Archery 2026",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"Single Player (vs Computer)", "Two Player"},
                "Single Player (vs Computer)");

        if (choice == 0) {
            currentMode = GameMode.SINGLE_PLAYER;
            player2.isAI = true;
            currentPlayer = player1;
            gameMessage = "🏅 Single Player Mode - You vs Computer! 🏅";
        } else {
            currentMode = GameMode.TWO_PLAYER;
            player2.isAI = false;
            currentPlayer = player1;
            gameMessage = "🥇 Two Player Mode - Player 1 vs Player 2! 🥇";
        }
        showMessage();
    }

    private int calculateScore(int x, int y) {
        double centerX = TARGET_X + TARGET_SIZE / 2;
        double centerY = TARGET_Y + TARGET_SIZE / 2;
        double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
        double radius = TARGET_SIZE / 2;

        if (distance <= radius * 0.1) return 10;
        if (distance <= radius * 0.25) return 9;
        if (distance <= radius * 0.4) return 8;
        if (distance <= radius * 0.55) return 7;
        if (distance <= radius * 0.7) return 6;
        if (distance <= radius * 0.85) return 5;
        return 4;
    }

    private void addHitParticles(int x, int y) {
        for (int i = 0; i < 30; i++) {
            particles.add(new Particle(x, y, Color.YELLOW));
        }
    }

    private void addFirework(int x, int y) {
        fireworks.add(new Firework(x, y));
    }

    private void endTurn() {
        currentPlayer.arrowsShot++;

        if (currentPlayer.arrowsShot >= totalEnds * 3) {
            endGame();
            return;
        }

        if (currentPlayer.arrowsShot % 3 == 0) {
            currentEnd++;
            gameMessage = "🏹 End " + currentEnd + " of " + totalEnds + " 🏹";
            showMessage();
        }

        if (currentPlayer == player1) {
            currentPlayer = player2;
            if (currentMode == GameMode.SINGLE_PLAYER && currentPlayer.isAI) {
                isComputerTurn = true;
                gameMessage = "🤖 Computer's Turn... 🤖";
                showMessage();
                computer.shoot();
            } else {
                gameMessage = currentPlayer.name + "'s Turn! 🎯";
                showMessage();
            }
        } else {
            currentPlayer = player1;
            gameMessage = currentPlayer.name + "'s Turn! 🎯";
            showMessage();
        }
    }

    private void endGame() {
        gameRunning = false;
        String winner;
        if (player1.score > player2.score) {
            winner = player1.name + " wins! 🏆";
        } else if (player2.score > player1.score) {
            winner = player2.name + " wins! 🏆";
        } else {
            winner = "Tie! 🤝";
        }

        gameMessage = "🎉 GAME OVER! " + winner + " 🎉\nFinal Score: " +
                player1.score + " - " + player2.score;
        showMessage();

        for (int i = 0; i < 10; i++) {
            addFirework(WIDTH/2 + random.nextInt(400) - 200, HEIGHT/2);
        }
    }

    private void showMessage() {
        messageTimer.cancel();
        messageTimer = new Timer();
        messageTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                gameMessage = "";
                messageTimer.cancel();
            }
        }, 2000);
    }

    private void drawTarget(Graphics2D g2d) {
        int radius = TARGET_SIZE / 2;
        int centerX = TARGET_X + radius;
        int centerY = TARGET_Y + radius;

        for (int i = 0; i < 5; i++) {
            int ringRadius = radius - i * (radius / 5);
            Color ringColor = olympicColors[i % 5];
            g2d.setColor(ringColor);
            g2d.setStroke(new BasicStroke(4));
            g2d.drawOval(centerX - ringRadius, centerY - ringRadius, ringRadius * 2, ringRadius * 2);
        }

        for (int i = 4; i >= 0; i--) {
            int ringRadius = (int)(radius * (1 - i * 0.2));

            Color ringColor;
            if (i == 4) {
                float intensity = 0.5f + glowIntensity[i] * 0.5f;
                ringColor = new Color(1.0f, intensity, 0.0f);
            } else {
                int brightness = 200 - i * 30;
                ringColor = new Color(brightness, brightness, brightness);
            }

            g2d.setColor(ringColor);
            g2d.fillOval(centerX - ringRadius, centerY - ringRadius, ringRadius * 2, ringRadius * 2);

            if (i == 4) {
                g2d.setColor(new Color(1.0f, 0.8f, 0.2f, 0.3f));
                g2d.setStroke(new BasicStroke(5));
                g2d.drawOval(centerX - ringRadius - 8, centerY - ringRadius - 8,
                        (ringRadius + 8) * 2, (ringRadius + 8) * 2);
            }
        }

        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        for (int i = 0; i < 5; i++) {
            int ringRadius = (int)(radius * (0.9 - i * 0.2));
            g2d.setColor(Color.WHITE);
            String scoreText = String.valueOf(10 - i);
            g2d.drawString(scoreText, centerX + ringRadius - 20, centerY - ringRadius + 10);
        }
    }

    private void drawBow(Graphics2D g2d) {
        int bowX = 150;
        int bowY = HEIGHT - 200;

        GradientPaint bowGradient = new GradientPaint(bowX - 50, bowY, olympicColors[0],
                bowX + 50, bowY, olympicColors[4]);
        g2d.setPaint(bowGradient);
        g2d.setStroke(new BasicStroke(18));
        g2d.drawArc(bowX - 60, bowY - 120, 120, 240, 180, 180);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(4));
        double angle = Math.atan2(mouseY - bowY, mouseX - bowX);
        int stringX = (int)(bowX + power * 40 * Math.cos(angle));
        int stringY = (int)(bowY + power * 40 * Math.sin(angle));
        g2d.drawLine(bowX - 50, bowY - 100, stringX, stringY);
        g2d.drawLine(bowX - 50, bowY + 100, stringX, stringY);

        if (isDragging) {
            g2d.setColor(currentPlayer.color);
            g2d.setStroke(new BasicStroke(5));
            g2d.drawLine(stringX, stringY, stringX - 80, stringY - 30);

            int powerWidth = (int)(power * 250);
            g2d.setColor(new Color(255, 100, 100, 180));
            g2d.fillRect(bowX - 120, bowY + 120, powerWidth, 25);
            g2d.setColor(Color.RED);
            g2d.drawRect(bowX - 120, bowY + 120, 250, 25);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("POWER: " + (int)(power * 100) + "%", bowX - 50, bowY + 115);
        }

        g2d.setColor(new Color(255, 200, 150));
        g2d.fillOval(bowX - 35, bowY - 60, 50, 50);
        g2d.setColor(currentPlayer.color);
        g2d.fillRect(bowX - 50, bowY - 20, 80, 100);

        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString(currentPlayer.name, bowX - 30, bowY - 70);
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRoundRect(10, 10, 400, 180, 20, 20);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("🏹 OLYMPIC ARCHERY 2026 🏹", 20, 45);

        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(player1.color);
        g2d.drawString(player1.name + ": " + player1.score, 20, 85);
        g2d.setColor(player2.color);
        g2d.drawString(player2.name + ": " + player2.score, 20, 115);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("End: " + currentEnd + "/" + totalEnds, 20, 145);
        g2d.drawString("Arrows: " + (currentPlayer.arrowsShot % 3) + "/3", 20, 165);

        g2d.setColor(new Color(200, 200, 255, 150));
        g2d.fillRoundRect(WIDTH - 200, 10, 180, 60, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.drawString("💨 Wind: " + String.format("%.1f", windSpeed), WIDTH - 190, 35);
        if (windSpeed > 0) {
            g2d.drawString("→→→", WIDTH - 190, 55);
        } else if (windSpeed < 0) {
            g2d.drawString("←←←", WIDTH - 190, 55);
        }

        if (!gameMessage.isEmpty()) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRoundRect(WIDTH/2 - 300, HEIGHT - 100, 600, 50, 15, 15);
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (WIDTH - fm.stringWidth(gameMessage)) / 2;
            g2d.drawString(gameMessage, x, HEIGHT - 65);
        }

        if (gameRunning) {
            g2d.setColor(currentPlayer.color);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRoundRect(5, 5, 410, 190, 20, 20);
        }

        if (showingResult) {
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            String resultText = "+" + resultScore;
            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString(resultText, resultX + 20, resultY - 20);
        }
    }

    private void updateParticles() {
        particles.removeIf(p -> p.life <= 0);
        for (Particle p : particles) {
            p.update();
        }

        fireworks.removeIf(f -> f.life <= 0);
        for (Firework f : fireworks) {
            f.update();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint stadiumGradient = new GradientPaint(0, 0, new Color(20, 30, 60),
                0, HEIGHT, new Color(10, 20, 40));
        g2d.setPaint(stadiumGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        for (int i = 0; i < 10; i++) {
            g2d.setColor(new Color(255, 255, 200, 50));
            g2d.fillOval(100 + i * 120, 50, 20, 20);
        }

        g2d.setColor(new Color(100, 100, 100, 100));
        for (int i = 0; i < 200; i++) {
            g2d.fillRect(50 + i * 7, HEIGHT - 80, 3, 15);
        }

        drawTarget(g2d);

        for (Arrow arrow : arrows) {
            arrow.draw(g2d);
        }

        drawBow(g2d);
        drawUI(g2d);

        for (Particle p : particles) {
            p.draw(g2d);
        }
        for (Firework f : fireworks) {
            f.draw(g2d);
        }

        int time = (int)(System.currentTimeMillis() / 100);
        for (int i = 0; i < 20; i++) {
            int x = (time * 5 + i * 150) % WIDTH;
            g2d.setColor(new Color(200, 200, 255, 80));
            g2d.drawLine(x, 100 + i * 20, x + 40, 105 + i * 20);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentArrow != null) {
            currentArrow.update();
            if (!currentArrow.active) {
                arrows.add(currentArrow);
                currentArrow = null;
            }
        }
        updateParticles();
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!gameRunning || currentArrow != null || currentPlayer.isAI) return;
        int bowX = 150;
        int bowY = HEIGHT - 200;
        Rectangle bowArea = new Rectangle(bowX - 60, bowY - 120, 120, 240);
        if (bowArea.contains(e.getPoint())) {
            isDragging = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isDragging && currentArrow == null && gameRunning && !currentPlayer.isAI) {
            int bowX = 150;
            int bowY = HEIGHT - 200;
            currentArrow = new Arrow(bowX, bowY, power, mouseX, mouseY, currentPlayer);
            isDragging = false;
            power = 0;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (isDragging) {
            mouseX = e.getX();
            mouseY = e.getY();
            int bowX = 150;
            int bowY = HEIGHT - 200;
            double dx = mouseX - bowX;
            double dy = mouseY - bowY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            power = Math.min(1.0, distance / 180.0);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("🏹 Olympic Archery 2026 - Tournament Edition 🏹");
            OlympicArchery2026 game = new OlympicArchery2026();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}