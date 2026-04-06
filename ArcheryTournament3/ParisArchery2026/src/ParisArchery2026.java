import javax.swing.*;
        import java.awt.*;
        import java.awt.event.*;
        import java.awt.geom.*;
        import java.util.*;
        import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ParisArchery2026 extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
    private static final int WIDTH = 1400;
    private static final int HEIGHT = 900;
    private static final int TARGET_X = 1050;
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

    // Paris theme elements
    private List<EiffelTower> eiffelTowers = new ArrayList<>();
    private List<FrenchFlag> frenchFlags = new ArrayList<>();
    private List<Firework> fireworks = new ArrayList<>();
    private List<FloatingHeart> floatingHearts = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();
    private List<SeineBoat> seineBoats = new ArrayList<>();
    private Random random = new Random();

    // Animation effects
    private float[] glowIntensity = new float[6];
    private boolean glowIncreasing = true;
    private int timeOfDay = 0;

    // Paris colors
    private Color[] parisColors = {
            new Color(0, 85, 164),   // French Blue
            new Color(255, 255, 255), // White
            new Color(239, 65, 53)    // French Red
    };

    private Color[] eiffelColors = {
            new Color(162, 142, 109),
            new Color(173, 151, 115),
            new Color(141, 121, 93)
    };

    // Wind effect
    private double windSpeed = 0;
    private Timer windTimer;

    class Player {
        String name;
        String country;
        int score;
        int arrowsShot;
        int[] endScores;
        Color color;
        boolean isAI;

        Player(String name, String country, Color color, boolean isAI) {
            this.name = name;
            this.country = country;
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
        double accuracy = 0.7;

        void shoot() {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (isComputerTurn && gameRunning && currentPlayer.isAI) {
                        int centerX = TARGET_X + TARGET_SIZE / 2;
                        int centerY = TARGET_Y + TARGET_SIZE / 2;
                        int deviation = (int)((1 - accuracy) * 80);
                        int targetX = centerX + random.nextInt(deviation * 2) - deviation;
                        int targetY = centerY + random.nextInt(deviation * 2) - deviation;
                        double power = 0.6 + random.nextDouble() * 0.4;

                        int bowX = 200;
                        int bowY = HEIGHT - 200;
                        currentArrow = new Arrow(bowX, bowY, power, targetX, targetY, currentPlayer);

                        addAimingEffect(targetX, targetY);
                        timer.cancel();
                    }
                }
            }, 800 + random.nextInt(1200));
        }

        private void addAimingEffect(int x, int y) {
            for (int i = 0; i < 20; i++) {
                particles.add(new Particle(x + random.nextInt(20) - 10,
                        y + random.nextInt(20) - 10,
                        parisColors[random.nextInt(3)]));
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
                    addFloatingHearts((int)x, (int)y);
                    gameMessage = owner.name + " scores a PERFECT 10! 🎯❤️";
                    showMessage();
                } else if (hitScore >= 8) {
                    gameMessage = "Magnifique! " + owner.name + " scores " + hitScore + "! 🇫🇷";
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

                double angle = Math.atan2(vy, vx);
                for (int i = 0; i < 3; i++) {
                    g2d.setColor(parisColors[i]);
                    int fx = (int)(x - vx * 0.3 + (8 + i * 2) * Math.sin(angle));
                    int fy = (int)(y - vy * 0.3 - (8 + i * 2) * Math.cos(angle));
                    g2d.drawLine((int)x, (int)y, fx, fy);
                }
            } else if (hit) {
                g2d.setColor(Color.RED);
                g2d.fillOval((int)hitX - 5, (int)hitY - 5, 10, 10);
                g2d.setColor(Color.YELLOW);
                g2d.fillOval((int)hitX - 2, (int)hitY - 2, 4, 4);
            }
        }
    }

    class EiffelTower {
        int x, y;

        EiffelTower(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(eiffelColors[0]);
            int[] xPoints = {x, x + 40, x + 80};
            int[] yPoints = {y, y - 200, y};
            g2d.fillPolygon(xPoints, yPoints, 3);

            for (int i = 0; i < 3; i++) {
                int levelY = y - 50 - i * 50;
                g2d.fillRect(x + 5, levelY, 70, 8);
                g2d.fillRect(x + 15, levelY - 20, 50, 5);
            }

            g2d.fillOval(x + 30, y - 210, 20, 20);

            if (timeOfDay == 2) {
                g2d.setColor(new Color(255, 255, 200, 100));
                for (int i = 0; i < 3; i++) {
                    g2d.drawOval(x + 20 - i * 5, y - 100 - i * 10, 40 + i * 10, 80 + i * 10);
                }
            }
        }
    }

    class FrenchFlag {
        int x, y;

        FrenchFlag(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g2d) {
            for (int i = 0; i < 3; i++) {
                g2d.setColor(parisColors[i]);
                g2d.fillRect(x + i * 15, y, 15, 60);
            }
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillRect(x - 5, y, 5, 60);
        }
    }

    class SeineBoat {
        int x, y;
        double dx;

        SeineBoat(int x, int y) {
            this.x = x;
            this.y = y;
            this.dx = 0.5;
        }

        void update() {
            x += dx;
            if (x > WIDTH + 100) x = -100;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillRect(x, y, 80, 20);
            g2d.fillRect(x + 10, y - 20, 60, 20);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(x + 30, y - 15, 20, 15);

            for (int i = 0; i < 3; i++) {
                g2d.setColor(parisColors[i]);
                g2d.fillRect(x + 65 + i * 5, y - 25, 5, 25);
            }
        }
    }

    class FloatingHeart {
        double x, y;
        int life;

        FloatingHeart(double x, double y) {
            this.x = x;
            this.y = y;
            this.life = 100;
        }

        void update() {
            y -= 1;
            life--;
        }

        void draw(Graphics2D g2d) {
            if (life > 0) {
                g2d.setColor(new Color(255, 100, 100, life * 2));
                g2d.fillArc((int)x - 5, (int)y - 5, 10, 10, 0, 180);
                g2d.fillArc((int)x + 5, (int)y - 5, 10, 10, 0, 180);
                g2d.fillRect((int)x - 5, (int)y - 2, 20, 10);
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
                particles.add(new Particle(x, y, parisColors[random.nextInt(3)]));
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

    public ParisArchery2026() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235));
        addMouseListener(this);
        addMouseMotionListener(this);

        // Initialize Paris landmarks
        eiffelTowers.add(new EiffelTower(50, HEIGHT - 150));
        eiffelTowers.add(new EiffelTower(WIDTH - 130, HEIGHT - 150));

        frenchFlags.add(new FrenchFlag(100, 50));
        frenchFlags.add(new FrenchFlag(WIDTH - 150, 50));
        frenchFlags.add(new FrenchFlag(WIDTH / 2 - 30, 20));

        for (int i = 0; i < 3; i++) {
            seineBoats.add(new SeineBoat(100 + i * 300, HEIGHT - 100));
        }

        // Initialize players
        player1 = new Player("Jean", "🇫🇷 France", new Color(0, 85, 164), false);
        player2 = new Player("Emma", "🇫🇷 France", new Color(239, 65, 53), false);
        computer = new ComputerPlayer();
        currentPlayer = player1;

        // Setup timers
        gameTimer = new javax.swing.Timer(16, this);
        gameTimer.start();

        windTimer = new Timer();
        windTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                windSpeed = (random.nextDouble() - 0.5) * 2.5;
            }
        }, 0, 5000);

        Timer glowTimer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < glowIntensity.length; i++) {
                    if (glowIncreasing) {
                        glowIntensity[i] += 0.05f;
                        if (glowIntensity[i] >= 1.0f) glowIncreasing = false;
                    } else {
                        glowIntensity[i] -= 0.05f;
                        if (glowIntensity[i] <= 0.2f) glowIncreasing = true;
                    }
                }
                timeOfDay = (int)((System.currentTimeMillis() / 30000) % 3);
            }
        });
        glowTimer.start();

        messageTimer = new Timer();

        showModeSelection();
    }

    private void showModeSelection() {
        int choice = JOptionPane.showOptionDialog(this,
                "🏹 PARIS OLYMPIC ARCHERY 2026 🏹\n\n" +
                        "🇫🇷 Bienvenue aux Jeux Olympiques de Paris! 🇫🇷\n\n" +
                        "Choose Game Mode:",
                "Paris Olympic Archery 2026",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"Single Player (vs Computer)", "Two Player"},
                "Single Player (vs Computer)");

        if (choice == 0) {
            currentMode = GameMode.SINGLE_PLAYER;
            player2.isAI = true;
            player2.name = "Computer";
            currentPlayer = player1;
            gameMessage = "🏅 Single Player Mode - You vs Computer! 🇫🇷🏅";
        } else {
            currentMode = GameMode.TWO_PLAYER;
            player2.isAI = false;
            player2.name = "Sophie";
            currentPlayer = player1;
            gameMessage = "🥇 Two Player Mode - Jean vs Sophie! Allez! 🥇";
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
            particles.add(new Particle(x, y, parisColors[random.nextInt(3)]));
        }
    }

    private void addFirework(int x, int y) {
        fireworks.add(new Firework(x, y));
    }

    private void addFloatingHearts(int x, int y) {
        for (int i = 0; i < 10; i++) {
            floatingHearts.add(new FloatingHeart(x + random.nextInt(30) - 15, y));
        }
    }

    private void endTurn() {
        currentPlayer.arrowsShot++;

        if (currentPlayer.arrowsShot >= totalEnds * 3) {
            endGame();
            return;
        }

        if (currentPlayer.arrowsShot % 3 == 0) {
            currentEnd++;
            gameMessage = "🏹 End " + currentEnd + " of " + totalEnds + " - Allez! 🏹";
            showMessage();
        }

        if (currentPlayer == player1) {
            currentPlayer = player2;
            if (currentMode == GameMode.SINGLE_PLAYER && currentPlayer.isAI) {
                isComputerTurn = true;
                gameMessage = "🤖 Computer's Turn... Allez l'ordinateur! 🤖";
                showMessage();
                computer.shoot();
            } else {
                gameMessage = currentPlayer.name + "'s Turn! Bonne chance! 🎯";
                showMessage();
            }
        } else {
            currentPlayer = player1;
            gameMessage = currentPlayer.name + "'s Turn! Bonne chance! 🎯";
            showMessage();
        }
    }

    private void endGame() {
        gameRunning = false;
        String winner;
        if (player1.score > player2.score) {
            winner = player1.name + " wins the Gold Medal! 🥇🇫🇷";
        } else if (player2.score > player1.score) {
            winner = player2.name + " wins the Gold Medal! 🥇🇫🇷";
        } else {
            winner = "It's a tie! Two Gold Medals! 🥇🥇";
        }

        gameMessage = "🎉 GAME OVER! " + winner + " 🎉\nFinal Score: " +
                player1.score + " - " + player2.score + "\nMerci for playing! 🇫🇷";
        showMessage();

        for (int i = 0; i < 20; i++) {
            addFirework(WIDTH/2 + random.nextInt(600) - 300, HEIGHT/2 + random.nextInt(200));
        }
        for (int i = 0; i < 30; i++) {
            addFloatingHearts(WIDTH/2 + random.nextInt(400) - 200, HEIGHT/2);
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
        }, 2500);
    }

    private void drawParisBackground(Graphics2D g2d) {
        Color skyColor;
        if (timeOfDay == 0) {
            skyColor = new Color(135, 206, 235);
        } else if (timeOfDay == 1) {
            skyColor = new Color(255, 140, 80);
        } else {
            skyColor = new Color(20, 30, 60);
        }

        GradientPaint skyGradient = new GradientPaint(0, 0, skyColor, 0, HEIGHT/2,
                timeOfDay == 2 ? new Color(10, 20, 40) : new Color(100, 150, 200));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT/2);

        g2d.setColor(new Color(70, 130, 180));
        g2d.fillRect(0, HEIGHT - 150, WIDTH, 150);

        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, HEIGHT - 120, WIDTH, 120);

        for (EiffelTower tower : eiffelTowers) {
            tower.draw(g2d);
        }

        for (SeineBoat boat : seineBoats) {
            boat.update();
            boat.draw(g2d);
        }

        for (FrenchFlag flag : frenchFlags) {
            flag.draw(g2d);
        }

        for (FloatingHeart heart : floatingHearts) {
            heart.update();
            heart.draw(g2d);
        }
        floatingHearts.removeIf(h -> h.life <= 0);
    }

    private void drawTarget(Graphics2D g2d) {
        int radius = TARGET_SIZE / 2;
        int centerX = TARGET_X + radius;
        int centerY = TARGET_Y + radius;

        for (int i = 4; i >= 0; i--) {
            int ringRadius = (int)(radius * (1 - i * 0.2));

            Color ringColor;
            if (i == 4) {
                float intensity = 0.5f + glowIntensity[i] * 0.5f;
                ringColor = new Color(1.0f, 0.85f, 0.2f, intensity);
            } else {
                ringColor = parisColors[i % 3];
            }

            g2d.setColor(ringColor);
            g2d.fillOval(centerX - ringRadius, centerY - ringRadius, ringRadius * 2, ringRadius * 2);

            if (i == 0 || i == 1) {
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval(centerX - ringRadius, centerY - ringRadius, ringRadius * 2, ringRadius * 2);
            }
        }

        g2d.setColor(new Color(50, 50, 50, 100));
        int[] xPoints = {centerX - 15, centerX, centerX + 15};
        int[] yPoints = {centerY + 20, centerY - 30, centerY + 20};
        g2d.fillPolygon(xPoints, yPoints, 3);

        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        for (int i = 0; i < 5; i++) {
            int ringRadius = (int)(radius * (0.9 - i * 0.2));
            g2d.setColor(Color.WHITE);
            String scoreText = String.valueOf(10 - i);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(scoreText, centerX + ringRadius - 20, centerY - ringRadius + 10);
        }
    }

    private void drawBow(Graphics2D g2d) {
        int bowX = 200;
        int bowY = HEIGHT - 200;

        for (int i = 0; i < 3; i++) {
            g2d.setColor(parisColors[i]);
            g2d.setStroke(new BasicStroke(8 - i * 2));
            g2d.drawArc(bowX - 60 + i * 5, bowY - 120 + i * 5, 120 - i * 10, 240 - i * 10, 180, 180);
        }

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(4));
        double angle = Math.atan2(mouseY - bowY, mouseX - bowX);
        int stringX = (int)(bowX + power * 45 * Math.cos(angle));
        int stringY = (int)(bowY + power * 45 * Math.sin(angle));
        g2d.drawLine(bowX - 50, bowY - 100, stringX, stringY);
        g2d.drawLine(bowX - 50, bowY + 100, stringX, stringY);

        if (isDragging) {
            g2d.setColor(currentPlayer.color);
            g2d.setStroke(new BasicStroke(5));
            g2d.drawLine(stringX, stringY, stringX - 90, stringY - 35);

            int powerWidth = (int)(power * 250);
            for (int i = 0; i < 3; i++) {
                g2d.setColor(parisColors[i]);
                g2d.fillRect(bowX - 120 + i * 85, bowY + 130, Math.min(85, powerWidth - i * 85), 20);
            }
            g2d.setColor(Color.BLACK);
            g2d.drawRect(bowX - 120, bowY + 130, 250, 20);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("POWER: " + (int)(power * 100) + "%", bowX - 50, bowY + 125);
        }

        g2d.setColor(new Color(255, 200, 150));
        g2d.fillOval(bowX - 35, bowY - 60, 50, 50);
        g2d.setColor(Color.BLACK);
        g2d.fillOval(bowX - 15, bowY - 45, 8, 8);
        g2d.fillOval(bowX + 5, bowY - 45, 8, 8);

        g2d.setColor(Color.BLACK);
        g2d.fillOval(bowX - 40, bowY - 70, 70, 25);

        g2d.setColor(currentPlayer.color);
        g2d.fillRect(bowX - 50, bowY - 20, 80, 100);

        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.WHITE);
        g2d.drawString(currentPlayer.name, bowX - 30, bowY - 80);
        g2d.drawString(currentPlayer.country, bowX - 25, bowY - 65);
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRoundRect(10, 10, 450, 200, 20, 20);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        g2d.drawString("🏹 PARIS 2026 OLYMPIC ARCHERY 🏹", 20, 45);

        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(player1.color);
        g2d.drawString(player1.name + " " + player1.country + ": " + player1.score, 20, 85);
        g2d.setColor(player2.color);
        g2d.drawString(player2.name + " " + player2.country + ": " + player2.score, 20, 115);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("End: " + currentEnd + "/" + totalEnds, 20, 150);
        g2d.drawString("Arrows in this end: " + (currentPlayer.arrowsShot % 3) + "/3", 20, 175);
        g2d.drawString("Total arrows shot: " + currentPlayer.arrowsShot + "/" + (totalEnds * 3), 20, 195);

        g2d.setColor(new Color(200, 200, 255, 180));
        g2d.fillRoundRect(WIDTH - 220, 10, 200, 80, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("💨 Vent (Wind): " + String.format("%.1f", windSpeed), WIDTH - 210, 35);
        if (windSpeed > 0.5) {
            g2d.drawString("→→→ Fort vent!", WIDTH - 210, 60);
        } else if (windSpeed < -0.5) {
            g2d.drawString("←←← Fort vent!", WIDTH - 210, 60);
        } else if (Math.abs(windSpeed) > 0) {
            g2d.drawString("→ Vent léger", WIDTH - 210, 60);
        } else {
            g2d.drawString("⚡ Calme plat!", WIDTH - 210, 60);
        }

        if (!gameMessage.isEmpty()) {
            g2d.setColor(new Color(0, 0, 0, 220));
            g2d.fillRoundRect(WIDTH/2 - 350, HEIGHT - 120, 700, 60, 20, 20);
            g2d.setColor(new Color(255, 215, 0));
            g2d.setFont(new Font("Arial", Font.BOLD, 22));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (WIDTH - fm.stringWidth(gameMessage)) / 2;
            g2d.drawString(gameMessage, x, HEIGHT - 80);
        }

        if (gameRunning) {
            g2d.setColor(currentPlayer.color);
            g2d.setStroke(new BasicStroke(4));
            g2d.drawRoundRect(5, 5, 460, 210, 20, 20);
        }

        if (showingResult) {
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            String resultText = "+" + resultScore;
            if (resultScore == 10) {
                g2d.setColor(new Color(255, 215, 0));
                g2d.drawString("🎯 BULLSEYE! 🎯", resultX - 50, resultY - 40);
            }
            g2d.setColor(new Color(255, 100, 100));
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

        drawParisBackground(g2d);
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
        int bowX = 200;
        int bowY = HEIGHT - 200;
        Rectangle bowArea = new Rectangle(bowX - 60, bowY - 120, 120, 240);
        if (bowArea.contains(e.getPoint())) {
            isDragging = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isDragging && currentArrow == null && gameRunning && !currentPlayer.isAI) {
            int bowX = 200;
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
            int bowX = 200;
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
            JFrame frame = new JFrame("🏹 Paris 2026 Olympic Archery - Jeux Olympiques 🏹");
            ParisArchery2026 game = new ParisArchery2026();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}