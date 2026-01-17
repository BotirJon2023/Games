import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class OlympicSportSimulator extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private GamePanel gamePanel;
    private MenuPanel menuPanel;
    private ResultsPanel resultsPanel;
    private List<AthleteRecord> leaderboard;
    private String currentPlayerName;
    private int totalMedals;
    private Map<String, Integer> sportScores;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OlympicSportSimulator game = new OlympicSportSimulator();
            game.setVisible(true);
        });
    }

    public OlympicSportSimulator() {
        setTitle("🏅 Olympic Sport Simulator 2024 🏅");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        leaderboard = new ArrayList<>();
        sportScores = new HashMap<>();
        currentPlayerName = "Athlete";
        totalMedals = 0;

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        menuPanel = new MenuPanel();
        gamePanel = new GamePanel();
        resultsPanel = new ResultsPanel();

        mainPanel.add(menuPanel, "MENU");
        mainPanel.add(gamePanel, "GAME");
        mainPanel.add(resultsPanel, "RESULTS");

        add(mainPanel);
        cardLayout.show(mainPanel, "MENU");
    }

    // ==================== MENU PANEL ====================
    class MenuPanel extends JPanel {
        private List<MenuParticle> particles;
        private Timer animTimer;
        private float olympicRingRotation = 0;

        public MenuPanel() {
            setBackground(new Color(20, 30, 60));
            setLayout(null);
            particles = new ArrayList<>();

            for (int i = 0; i < 50; i++) {
                particles.add(new MenuParticle());
            }

            JTextField nameField = new JTextField("Enter Your Name");
            nameField.setBounds(450, 350, 300, 40);
            nameField.setFont(new Font("Arial", Font.BOLD, 18));
            nameField.setHorizontalAlignment(JTextField.CENTER);
            nameField.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    if (nameField.getText().equals("Enter Your Name")) {
                        nameField.setText("");
                    }
                }
            });
            add(nameField);

            String[] sports = {"🏃 100m Sprint", "🏊 Swimming", "🦘 Long Jump", "🎯 Javelin Throw", "🏹 Archery"};
            int yPos = 420;

            for (String sport : sports) {
                JButton btn = createStyledButton(sport);
                btn.setBounds(450, yPos, 300, 50);
                final String sportName = sport;
                btn.addActionListener(e -> {
                    currentPlayerName = nameField.getText().isEmpty() ? "Athlete" : nameField.getText();
                    startSport(sportName);
                });
                add(btn);
                yPos += 60;
            }

            JButton leaderBtn = createStyledButton("🏆 Leaderboard");
            leaderBtn.setBounds(450, yPos + 20, 300, 50);
            leaderBtn.addActionListener(e -> showLeaderboard());
            add(leaderBtn);

            animTimer = new Timer();
            animTimer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    olympicRingRotation += 0.02f;
                    for (MenuParticle p : particles) p.update();
                    repaint();
                }
            }, 0, 30);
        }

        private JButton createStyledButton(String text) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Arial", Font.BOLD, 18));
            btn.setBackground(new Color(255, 215, 0));
            btn.setForeground(Color.BLACK);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createRaisedBevelBorder());
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return btn;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 30, 80),
                    0, getHeight(), new Color(10, 15, 40));
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            for (MenuParticle p : particles) {
                g2d.setColor(new Color(255, 255, 255, p.alpha));
                g2d.fillOval((int)p.x, (int)p.y, p.size, p.size);
            }

            drawOlympicRings(g2d, 500, 120, 40);

            g2d.setFont(new Font("Serif", Font.BOLD, 60));
            g2d.setColor(new Color(255, 215, 0));
            String title = "OLYMPIC SPORT SIMULATOR";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 280);

            g2d.setFont(new Font("Arial", Font.ITALIC, 20));
            g2d.setColor(Color.WHITE);
            String subtitle = "Choose your sport and compete for gold!";
            fm = g2d.getFontMetrics();
            g2d.drawString(subtitle, (getWidth() - fm.stringWidth(subtitle)) / 2, 320);
        }

        private void drawOlympicRings(Graphics2D g2d, int x, int y, int radius) {
            Color[] colors = {Color.BLUE, Color.BLACK, Color.RED, Color.YELLOW, Color.GREEN};
            int[][] positions = {{0, 0}, {50, 0}, {100, 0}, {25, 30}, {75, 30}};

            g2d.setStroke(new BasicStroke(6));
            for (int i = 0; i < 5; i++) {
                g2d.setColor(colors[i]);
                int px = x + positions[i][0] + (int)(Math.sin(olympicRingRotation + i) * 3);
                int py = y + positions[i][1] + (int)(Math.cos(olympicRingRotation + i) * 3);
                g2d.drawOval(px, py, radius * 2, radius * 2);
            }
        }
    }

    class MenuParticle {
        double x, y, speed;
        int size, alpha;

        MenuParticle() {
            reset();
            y = Math.random() * 800;
        }

        void reset() {
            x = Math.random() * 1200;
            y = -10;
            speed = 1 + Math.random() * 3;
            size = 2 + (int)(Math.random() * 4);
            alpha = 50 + (int)(Math.random() * 150);
        }

        void update() {
            y += speed;
            if (y > 800) reset();
        }
    }

    // ==================== GAME PANEL ====================
    class GamePanel extends JPanel implements ActionListener, KeyListener {
        private javax.swing.Timer gameTimer;
        private String currentSport;
        private int gameState; // 0=ready, 1=playing, 2=finished
        private long startTime;
        private double score;
        private Athlete player;
        private List<Particle> particles;
        private List<Competitor> competitors;

        // Sprint specific
        private double sprintDistance;
        private int sprintTaps;
        private long lastTapTime;

        // Swimming specific
        private double swimDistance;
        private int swimStrokes;
        private double waveOffset;

        // Long Jump specific
        private double runupSpeed;
        private double jumpAngle;
        private double jumpDistance;
        private boolean isJumping;
        private double jumpX, jumpY, jumpVelX, jumpVelY;

        // Javelin specific
        private double javelinAngle;
        private double javelinPower;
        private boolean javelinThrown;
        private double javelinX, javelinY, javelinVelX, javelinVelY;
        private double javelinRotation;

        // Archery specific
        private double bowAngle;
        private double arrowPower;
        private boolean arrowFlying;
        private double arrowX, arrowY, arrowVelX, arrowVelY;
        private double targetX, targetY;
        private int arrowsLeft;
        private int archeryScore;
        private double windSpeed;

        public GamePanel() {
            setBackground(Color.BLACK);
            setFocusable(true);
            addKeyListener(this);

            particles = new ArrayList<>();
            competitors = new ArrayList<>();
            player = new Athlete();

            gameTimer = new javax.swing.Timer(16, this);
        }

        public void startGame(String sport) {
            currentSport = sport;
            gameState = 0;
            score = 0;
            particles.clear();
            competitors.clear();

            if (sport.contains("Sprint")) {
                initSprint();
            } else if (sport.contains("Swimming")) {
                initSwimming();
            } else if (sport.contains("Long Jump")) {
                initLongJump();
            } else if (sport.contains("Javelin")) {
                initJavelin();
            } else if (sport.contains("Archery")) {
                initArchery();
            }

            gameTimer.start();
            requestFocusInWindow();
        }

        private void initSprint() {
            sprintDistance = 0;
            sprintTaps = 0;
            lastTapTime = 0;
            player.x = 100;
            player.y = 400;

            String[] names = {"Bolt", "Blake", "Gatlin", "Powell"};
            for (int i = 0; i < 4; i++) {
                Competitor c = new Competitor(names[i]);
                c.x = 100;
                c.y = 300 + i * 80;
                c.speed = 4.5 + Math.random() * 1.5;
                competitors.add(c);
            }
        }

        private void initSwimming() {
            swimDistance = 0;
            swimStrokes = 0;
            waveOffset = 0;
            player.x = 100;
            player.y = 400;

            String[] names = {"Phelps", "Lochte", "Adrian", "Dressel"};
            for (int i = 0; i < 4; i++) {
                Competitor c = new Competitor(names[i]);
                c.x = 100;
                c.y = 280 + i * 100;
                c.speed = 3.0 + Math.random() * 1.2;
                competitors.add(c);
            }
        }

        private void initLongJump() {
            runupSpeed = 0;
            jumpAngle = 45;
            jumpDistance = 0;
            isJumping = false;
            jumpX = 100;
            jumpY = 500;
            player.x = 100;
            player.y = 500;
        }

        private void initJavelin() {
            javelinAngle = 45;
            javelinPower = 0;
            javelinThrown = false;
            javelinX = 200;
            javelinY = 450;
            javelinRotation = 0;
            player.x = 150;
            player.y = 450;
        }

        private void initArchery() {
            bowAngle = 0;
            arrowPower = 0;
            arrowFlying = false;
            arrowX = 150;
            arrowY = 400;
            targetX = 1000;
            targetY = 400;
            arrowsLeft = 5;
            archeryScore = 0;
            windSpeed = -2 + Math.random() * 4;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameState == 1) {
                updateGame();
            }
            updateParticles();
            repaint();
        }

        private void updateGame() {
            if (currentSport.contains("Sprint")) {
                updateSprint();
            } else if (currentSport.contains("Swimming")) {
                updateSwimming();
            } else if (currentSport.contains("Long Jump")) {
                updateLongJump();
            } else if (currentSport.contains("Javelin")) {
                updateJavelin();
            } else if (currentSport.contains("Archery")) {
                updateArchery();
            }
        }

        private void updateSprint() {
            long currentTime = System.currentTimeMillis();
            double tapBonus = 0;
            if (currentTime - lastTapTime < 200) {
                tapBonus = 2.0;
            }

            double speed = 3.0 + tapBonus + (sprintTaps * 0.01);
            sprintDistance += speed;
            player.x = 100 + (sprintDistance / 100.0) * 900;
            player.animFrame = (player.animFrame + 1) % 8;

            for (Competitor c : competitors) {
                c.x += c.speed;
                c.animFrame = (c.animFrame + 1) % 8;

                if (c.x >= 1000) {
                    endGame((System.currentTimeMillis() - startTime) / 1000.0);
                    return;
                }
            }

            if (player.x >= 1000) {
                score = (System.currentTimeMillis() - startTime) / 1000.0;
                createCelebration();
                endGame(score);
            }

            if (Math.random() < 0.3) {
                particles.add(new Particle(player.x, player.y + 30, Color.ORANGE, ParticleType.DUST));
            }
        }

        private void updateSwimming() {
            waveOffset += 0.1;

            double speed = 2.0 + (swimStrokes * 0.02);
            swimDistance += speed;
            player.x = 100 + (swimDistance / 50.0) * 900;
            player.animFrame = (player.animFrame + 1) % 12;

            for (Competitor c : competitors) {
                c.x += c.speed;
                c.animFrame = (c.animFrame + 1) % 12;

                if (c.x >= 1000) {
                    endGame((System.currentTimeMillis() - startTime) / 1000.0);
                    return;
                }
            }

            if (player.x >= 1000) {
                score = (System.currentTimeMillis() - startTime) / 1000.0;
                createCelebration();
                endGame(score);
            }

            if (Math.random() < 0.5) {
                particles.add(new Particle(player.x, player.y, new Color(150, 200, 255), ParticleType.SPLASH));
            }
        }

        private void updateLongJump() {
            if (!isJumping) {
                player.x += runupSpeed;
                if (player.x > 600 && runupSpeed > 0) {
                    // Foul
                    jumpDistance = -1;
                    gameState = 2;
                }
            } else {
                jumpVelY += 0.5; // gravity
                jumpX += jumpVelX;
                jumpY += jumpVelY;
                player.x = jumpX;
                player.y = jumpY;

                if (jumpY >= 500) {
                    jumpY = 500;
                    player.y = 500;
                    jumpDistance = (jumpX - 600) / 50.0;
                    score = jumpDistance;
                    createSandParticles();
                    if (jumpDistance > 0) createCelebration();
                    endGame(score);
                }
            }
        }

        private void updateJavelin() {
            if (javelinThrown) {
                javelinVelY += 0.15;
                javelinX += javelinVelX;
                javelinY += javelinVelY;
                javelinRotation = Math.atan2(javelinVelY, javelinVelX);

                if (javelinY >= 500) {
                    score = (javelinX - 200) / 10.0;
                    createSandParticles();
                    if (score > 0) createCelebration();
                    endGame(score);
                }
            }
        }

        private void updateArchery() {
            if (arrowFlying) {
                arrowVelX -= windSpeed * 0.01;
                arrowVelY += 0.08;
                arrowX += arrowVelX;
                arrowY += arrowVelY;

                double dx = arrowX - targetX;
                double dy = arrowY - targetY;
                double dist = Math.sqrt(dx*dx + dy*dy);

                if (dist < 80 || arrowX > 1100 || arrowY > 700) {
                    int points = calculateArcheryPoints(dist);
                    archeryScore += points;
                    arrowFlying = false;
                    arrowsLeft--;

                    if (arrowsLeft <= 0) {
                        score = archeryScore;
                        if (archeryScore > 30) createCelebration();
                        endGame(score);
                    } else {
                        arrowX = 150;
                        arrowY = 400;
                        arrowPower = 0;
                        windSpeed = -2 + Math.random() * 4;
                    }
                }
            }
        }

        private int calculateArcheryPoints(double dist) {
            if (dist < 10) return 10;
            if (dist < 20) return 9;
            if (dist < 30) return 8;
            if (dist < 40) return 7;
            if (dist < 50) return 6;
            if (dist < 60) return 5;
            if (dist < 70) return 3;
            if (dist < 80) return 1;
            return 0;
        }

        private void updateParticles() {
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.update();
                if (p.isDead()) it.remove();
            }
        }

        private void createCelebration() {
            for (int i = 0; i < 100; i++) {
                Color c = new Color[] {Color.YELLOW, Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA}[(int)(Math.random()*5)];
                particles.add(new Particle(player.x, player.y - 50, c, ParticleType.CONFETTI));
            }
        }

        private void createSandParticles() {
            for (int i = 0; i < 30; i++) {
                particles.add(new Particle(player.x, 500, new Color(210, 180, 140), ParticleType.SAND));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (currentSport.contains("Sprint")) {
                drawSprintScene(g2d);
            } else if (currentSport.contains("Swimming")) {
                drawSwimmingScene(g2d);
            } else if (currentSport.contains("Long Jump")) {
                drawLongJumpScene(g2d);
            } else if (currentSport.contains("Javelin")) {
                drawJavelinScene(g2d);
            } else if (currentSport.contains("Archery")) {
                drawArcheryScene(g2d);
            }

            for (Particle p : particles) {
                p.draw(g2d);
            }

            drawUI(g2d);
        }

        private void drawSprintScene(Graphics2D g2d) {
            // Sky gradient
            GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 235), 0, 300, new Color(255, 255, 200));
            g2d.setPaint(sky);
            g2d.fillRect(0, 0, getWidth(), 300);

            // Stadium
            g2d.setColor(new Color(100, 100, 100));
            g2d.fillRect(0, 200, getWidth(), 150);

            // Track
            g2d.setColor(new Color(200, 80, 60));
            g2d.fillRect(0, 350, getWidth(), 300);

            // Lane lines
            g2d.setColor(Color.WHITE);
            for (int i = 0; i < 6; i++) {
                g2d.drawLine(0, 300 + i * 80, getWidth(), 300 + i * 80);
            }

            // Distance markers
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            for (int i = 0; i <= 100; i += 10) {
                int x = 100 + (int)(i * 9);
                g2d.drawLine(x, 300, x, 700);
                g2d.drawString(i + "m", x - 10, 290);
            }

            // Finish line
            g2d.setColor(Color.WHITE);
            g2d.fillRect(1000, 300, 10, 400);

            // Draw competitors
            for (Competitor c : competitors) {
                drawRunner(g2d, c.x, c.y, c.animFrame, Color.RED, c.name);
            }

            // Draw player
            drawRunner(g2d, player.x, player.y + 80, player.animFrame, Color.BLUE, currentPlayerName);
        }

        private void drawRunner(Graphics2D g2d, double x, double y, int frame, Color color, String name) {
            int legAngle = (int)(Math.sin(frame * 0.8) * 30);
            int armAngle = (int)(Math.cos(frame * 0.8) * 25);

            // Body
            g2d.setColor(color);
            g2d.fillOval((int)x - 10, (int)y - 40, 20, 25);

            // Head
            g2d.setColor(new Color(255, 220, 180));
            g2d.fillOval((int)x - 8, (int)y - 55, 16, 16);

            // Legs
            g2d.setColor(color.darker());
            g2d.setStroke(new BasicStroke(4));
            g2d.drawLine((int)x, (int)y - 15, (int)x + legAngle/2, (int)y + 15);
            g2d.drawLine((int)x, (int)y - 15, (int)x - legAngle/2, (int)y + 15);

            // Arms
            g2d.drawLine((int)x, (int)y - 35, (int)x + armAngle, (int)y - 20);
            g2d.drawLine((int)x, (int)y - 35, (int)x - armAngle, (int)y - 20);

            // Name
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(name, (int)x - 15, (int)y - 60);
        }

        private void drawSwimmingScene(Graphics2D g2d) {
            // Pool background
            GradientPaint water = new GradientPaint(0, 200, new Color(0, 100, 180), 0, 700, new Color(0, 50, 100));
            g2d.setPaint(water);
            g2d.fillRect(0, 200, getWidth(), 500);

            // Waves
            g2d.setColor(new Color(100, 180, 255, 100));
            for (int i = 0; i < getWidth(); i += 20) {
                int waveY = (int)(Math.sin((i + waveOffset * 50) * 0.05) * 10);
                g2d.fillOval(i, 200 + waveY, 30, 15);
            }

            // Lane dividers
            g2d.setColor(new Color(255, 100, 100));
            for (int i = 0; i < 5; i++) {
                int ly = 250 + i * 100;
                for (int x = 0; x < getWidth(); x += 40) {
                    g2d.fillOval(x, ly, 15, 15);
                }
            }

            // Wall
            g2d.setColor(new Color(50, 50, 50));
            g2d.fillRect(1000, 200, 20, 500);

            // Draw competitors
            for (Competitor c : competitors) {
                drawSwimmer(g2d, c.x, c.y, c.animFrame, Color.RED, c.name);
            }

            // Draw player
            drawSwimmer(g2d, player.x, player.y + 100, player.animFrame, Color.BLUE, currentPlayerName);
        }

        private void drawSwimmer(Graphics2D g2d, double x, double y, int frame, Color color, String name) {
            int armAngle = (int)(Math.sin(frame * 0.5) * 40);

            // Body
            g2d.setColor(color);
            g2d.fillOval((int)x - 25, (int)y - 8, 50, 16);

            // Head
            g2d.setColor(new Color(255, 220, 180));
            g2d.fillOval((int)x + 20, (int)y - 10, 18, 18);

            // Cap
            g2d.setColor(color.brighter());
            g2d.fillArc((int)x + 20, (int)y - 12, 18, 15, 0, 180);

            // Arms
            g2d.setColor(new Color(255, 220, 180));
            g2d.setStroke(new BasicStroke(4));
            g2d.drawLine((int)x, (int)y, (int)x - 10 + armAngle/2, (int)y - 20 - Math.abs(armAngle)/2);
            g2d.drawLine((int)x - 20, (int)y, (int)x - 30 - armAngle/2, (int)y + 15 + Math.abs(armAngle)/3);

            // Splash effect
            if (frame % 3 == 0) {
                g2d.setColor(new Color(255, 255, 255, 150));
                g2d.fillOval((int)x - 30, (int)y - 15, 20, 10);
            }

            // Name
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(name, (int)x - 15, (int)y - 25);
        }

        private void drawLongJumpScene(Graphics2D g2d) {
            // Sky
            GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 235), 0, 300, new Color(200, 230, 255));
            g2d.setPaint(sky);
            g2d.fillRect(0, 0, getWidth(), 350);

            // Ground
            g2d.setColor(new Color(100, 180, 100));
            g2d.fillRect(0, 500, getWidth(), 300);

            // Runway
            g2d.setColor(new Color(200, 80, 60));
            g2d.fillRect(50, 450, 550, 100);

            // Take-off board
            g2d.setColor(Color.WHITE);
            g2d.fillRect(580, 450, 20, 100);
            g2d.setColor(Color.RED);
            g2d.fillRect(600, 450, 10, 100);

            // Sand pit
            g2d.setColor(new Color(210, 180, 140));
            g2d.fillRect(610, 450, 500, 100);

            // Distance markers
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            for (int i = 1; i <= 10; i++) {
                int mx = 610 + i * 50;
                g2d.drawLine(mx, 450, mx, 550);
                g2d.drawString(i + "m", mx - 8, 440);
            }

            // Draw player
            if (isJumping) {
                drawJumpingAthlete(g2d, player.x, player.y);
            } else {
                drawRunner(g2d, player.x, player.y, player.animFrame, Color.BLUE, currentPlayerName);
            }

            // Speed meter
            g2d.setColor(Color.BLACK);
            g2d.fillRect(50, 50, 200, 30);
            g2d.setColor(Color.GREEN);
            g2d.fillRect(52, 52, (int)(runupSpeed * 15), 26);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Speed: " + String.format("%.1f", runupSpeed), 60, 70);
        }

        private void drawJumpingAthlete(Graphics2D g2d, double x, double y) {
            double angle = Math.atan2(jumpVelY, jumpVelX);

            g2d.translate(x, y);
            g2d.rotate(angle * 0.3);

            // Body
            g2d.setColor(Color.BLUE);
            g2d.fillOval(-10, -40, 20, 30);

            // Head
            g2d.setColor(new Color(255, 220, 180));
            g2d.fillOval(-8, -55, 16, 16);

            // Extended limbs
            g2d.setColor(Color.BLUE.darker());
            g2d.setStroke(new BasicStroke(4));
            g2d.drawLine(0, -20, 25, 5);
            g2d.drawLine(0, -20, 30, -10);
            g2d.drawLine(0, -10, -20, 20);
            g2d.drawLine(0, -10, -15, 25);

            g2d.rotate(-angle * 0.3);
            g2d.translate(-x, -y);
        }

        private void drawJavelinScene(Graphics2D g2d) {
            // Sky
            GradientPaint sky = new GradientPaint(0, 0, new Color(100, 150, 200), 0, 300, new Color(200, 220, 240));
            g2d.setPaint(sky);
            g2d.fillRect(0, 0, getWidth(), 400);

            // Field
            g2d.setColor(new Color(80, 160, 80));
            g2d.fillRect(0, 400, getWidth(), 400);

            // Runway
            g2d.setColor(new Color(200, 80, 60));
            g2d.fillRect(50, 420, 200, 80);

            // Throwing arc lines
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            for (int r = 100; r <= 1000; r += 100) {
                g2d.drawArc(200 - r, 500 - r, r * 2, r * 2, 0, 45);
            }

            // Distance markers
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            for (int i = 10; i <= 100; i += 10) {
                int mx = 200 + i * 10;
                g2d.drawString(i + "m", mx, 490);
            }

            // Draw player
            drawThrower(g2d, player.x, player.y, javelinAngle, !javelinThrown);

            // Draw javelin
            if (javelinThrown) {
                drawJavelin(g2d, javelinX, javelinY, javelinRotation);
            }

            // Power meter
            if (!javelinThrown) {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(50, 50, 200, 30);
                g2d.setColor(Color.ORANGE);
                g2d.fillRect(52, 52, (int)(javelinPower * 2), 26);
                g2d.setColor(Color.WHITE);
                g2d.drawString("Power: " + (int)javelinPower + "%", 60, 70);

                // Angle indicator
                g2d.drawString("Angle: " + (int)javelinAngle + "°", 60, 110);
            }
        }

        private void drawThrower(Graphics2D g2d, double x, double y, double angle, boolean holdingJavelin) {
            // Body
            g2d.setColor(Color.BLUE);
            g2d.fillOval((int)x - 12, (int)y - 45, 24, 35);

            // Head
            g2d.setColor(new Color(255, 220, 180));
            g2d.fillOval((int)x - 10, (int)y - 65, 20, 20);

            // Throwing arm
            g2d.setColor(new Color(255, 220, 180));
            g2d.setStroke(new BasicStroke(5));
            double armAngleRad = Math.toRadians(angle);
            int armEndX = (int)(x + Math.cos(armAngleRad) * 40);
            int armEndY = (int)(y - 30 - Math.sin(armAngleRad) * 40);
            g2d.drawLine((int)x, (int)y - 30, armEndX, armEndY);

            if (holdingJavelin) {
                drawJavelin(g2d, armEndX, armEndY, Math.toRadians(angle));
            }

            // Legs
            g2d.setColor(Color.BLUE.darker());
            g2d.drawLine((int)x, (int)y - 10, (int)x - 15, (int)y + 30);
            g2d.drawLine((int)x, (int)y - 10, (int)x + 15, (int)y + 30);
        }

        private void drawJavelin(Graphics2D g2d, double x, double y, double rotation) {
            g2d.setColor(new Color(180, 180, 180));
            g2d.setStroke(new BasicStroke(3));

            double length = 80;
            int endX = (int)(x + Math.cos(rotation) * length);
            int endY = (int)(y - Math.sin(rotation) * length);

            g2d.drawLine((int)x, (int)y, endX, endY);

            // Tip
            g2d.setColor(Color.DARK_GRAY);
            int tipX = (int)(endX + Math.cos(rotation) * 10);
            int tipY = (int)(endY - Math.sin(rotation) * 10);
            g2d.drawLine(endX, endY, tipX, tipY);
        }

        private void drawArcheryScene(Graphics2D g2d) {
            // Sky
            GradientPaint sky = new GradientPaint(0, 0, new Color(100, 180, 255), 0, 400, new Color(200, 230, 255));
            g2d.setPaint(sky);
            g2d.fillRect(0, 0, getWidth(), 500);

            // Ground
            g2d.setColor(new Color(80, 140, 80));
            g2d.fillRect(0, 500, getWidth(), 300);

            // Target stand
            g2d.setColor(new Color(139, 90, 43));
            g2d.fillRect((int)targetX - 10, 300, 20, 200);

            // Target
            drawTarget(g2d, targetX, targetY);

            // Archer
            drawArcher(g2d, 150, 450, bowAngle);

            // Arrow
            if (arrowFlying) {
                drawArrow(g2d, arrowX, arrowY, Math.atan2(arrowVelY, arrowVelX));
            }

            // Wind indicator
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            String windStr = windSpeed > 0 ? "Wind: →→ " + String.format("%.1f", Math.abs(windSpeed)) :
                    "Wind: ←← " + String.format("%.1f", Math.abs(windSpeed));
            g2d.drawString(windStr, 500, 50);

            // Score and arrows left
            g2d.drawString("Score: " + archeryScore, 50, 50);
            g2d.drawString("Arrows: " + arrowsLeft, 50, 80);

            // Power meter
            if (!arrowFlying) {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(50, 100, 150, 25);
                g2d.setColor(Color.CYAN);
                g2d.fillRect(52, 102, (int)(arrowPower * 1.46), 21);
                g2d.setColor(Color.WHITE);
                g2d.drawString("Power: " + (int)arrowPower + "%", 60, 118);
            }
        }

        private void drawTarget(Graphics2D g2d, double x, double y) {
            Color[] colors = {Color.YELLOW, Color.YELLOW, Color.RED, Color.RED, Color.BLUE, Color.BLUE, Color.BLACK, Color.BLACK, Color.WHITE, Color.WHITE};
            int[] radii = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

            for (int i = radii.length - 1; i >= 0; i--) {
                g2d.setColor(colors[i]);
                g2d.fillOval((int)x - radii[i], (int)y - radii[i], radii[i] * 2, radii[i] * 2);
            }

            // Center dot
            g2d.setColor(Color.YELLOW);
            g2d.fillOval((int)x - 5, (int)y - 5, 10, 10);
        }

        private void drawArcher(Graphics2D g2d, double x, double y, double angle) {
            // Body
            g2d.setColor(Color.BLUE);
            g2d.fillOval((int)x - 12, (int)y - 50, 24, 40);

            // Head
            g2d.setColor(new Color(255, 220, 180));
            g2d.fillOval((int)x - 10, (int)y - 70, 20, 20);

            // Bow
            g2d.setColor(new Color(139, 90, 43));
            g2d.setStroke(new BasicStroke(4));
            double bowAngleRad = Math.toRadians(angle);
            g2d.drawArc((int)x + 10, (int)y - 80, 60, 100, 90 - (int)angle - 30, 60);

            // Bowstring
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            int stringPull = (int)(arrowPower * 0.3);
            g2d.drawLine((int)x + 30, (int)y - 70, (int)x + 30 - stringPull, (int)y - 30);
            g2d.drawLine((int)x + 30, (int)y + 10, (int)x + 30 - stringPull, (int)y - 30);

            // Arrow on bow
            if (!arrowFlying) {
                drawArrow(g2d, x + 30 - stringPull, y - 30, bowAngleRad);
            }

            // Arms
            g2d.setColor(new Color(255, 220, 180));
            g2d.setStroke(new BasicStroke(5));
            g2d.drawLine((int)x, (int)y - 35, (int)x + 30, (int)y - 35);
            g2d.drawLine((int)x, (int)y - 35, (int)x + 30 - stringPull, (int)y - 30);
        }

        private void drawArrow(Graphics2D g2d, double x, double y, double angle) {
            g2d.setColor(new Color(139, 90, 43));
            g2d.setStroke(new BasicStroke(3));

            int length = 50;
            int endX = (int)(x + Math.cos(angle) * length);
            int endY = (int)(y + Math.sin(angle) * length);

            g2d.drawLine((int)x, (int)y, endX, endY);

            // Arrowhead
            g2d.setColor(Color.GRAY);
            int tipX = (int)(endX + Math.cos(angle) * 10);
            int tipY = (int)(endY + Math.sin(angle) * 10);
            g2d.drawLine(endX, endY, tipX, tipY);

            // Fletching
            g2d.setColor(Color.RED);
            g2d.drawLine((int)x, (int)y, (int)(x - Math.cos(angle - 0.3) * 15), (int)(y - Math.sin(angle - 0.3) * 15));
            g2d.drawLine((int)x, (int)y, (int)(x - Math.cos(angle + 0.3) * 15), (int)(y - Math.sin(angle + 0.3) * 15));
        }

        private void drawUI(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, getWidth(), 40);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString(currentSport, 20, 28);
            g2d.drawString("Player: " + currentPlayerName, 300, 28);

            if (gameState == 0) {
                g2d.setColor(new Color(0, 0, 0, 200));
                g2d.fillRect(400, 350, 400, 100);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.drawString("Press SPACE to Start!", 470, 400);
                g2d.setFont(new Font("Arial", Font.PLAIN, 16));
                g2d.drawString(getInstructions(), 420, 430);
            }

            if (gameState == 2) {
                g2d.setColor(new Color(0, 0, 0, 200));
                g2d.fillRect(300, 300, 600, 200);
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Arial", Font.BOLD, 36));
                g2d.drawString("RESULT: " + formatScore(), 400, 380);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.drawString("Press ENTER to continue", 450, 450);
            }
        }

        private String getInstructions() {
            if (currentSport.contains("Sprint") || currentSport.contains("Swimming")) {
                return "Tap SPACE rapidly to run/swim faster!";
            } else if (currentSport.contains("Long Jump")) {
                return "Hold SPACE for run-up speed, release near line to jump";
            } else if (currentSport.contains("Javelin")) {
                return "UP/DOWN for angle, hold SPACE for power";
            } else if (currentSport.contains("Archery")) {
                return "UP/DOWN to aim, hold SPACE for power";
            }
            return "";
        }

        private String formatScore() {
            if (currentSport.contains("Sprint") || currentSport.contains("Swimming")) {
                return String.format("%.2f seconds", score);
            } else if (currentSport.contains("Long Jump")) {
                return jumpDistance < 0 ? "FOUL!" : String.format("%.2f meters", score);
            } else if (currentSport.contains("Javelin")) {
                return String.format("%.2f meters", score);
            } else if (currentSport.contains("Archery")) {
                return archeryScore + " points";
            }
            return String.valueOf(score);
        }

        private void endGame(double finalScore) {
            gameState = 2;
            gameTimer.stop();
            sportScores.put(currentSport, (int)(finalScore * 100));

            String medal = "";
            if (currentSport.contains("Sprint") || currentSport.contains("Swimming")) {
                if (finalScore < 10) medal = "🥇 GOLD";
                else if (finalScore < 12) medal = "🥈 SILVER";
                else if (finalScore < 15) medal = "🥉 BRONZE";
            } else if (currentSport.contains("Long Jump")) {
                if (finalScore > 8) medal = "🥇 GOLD";
                else if (finalScore > 7) medal = "🥈 SILVER";
                else if (finalScore > 6) medal = "🥉 BRONZE";
            } else if (currentSport.contains("Javelin")) {
                if (finalScore > 80) medal = "🥇 GOLD";
                else if (finalScore > 60) medal = "🥈 SILVER";
                else if (finalScore > 40) medal = "🥉 BRONZE";
            } else if (currentSport.contains("Archery")) {
                if (finalScore >= 45) medal = "🥇 GOLD";
                else if (finalScore >= 35) medal = "🥈 SILVER";
                else if (finalScore >= 25) medal = "🥉 BRONZE";
            }

            if (!medal.isEmpty()) totalMedals++;

            leaderboard.add(new AthleteRecord(currentPlayerName, currentSport, finalScore, medal));
            leaderboard.sort((a, b) -> Double.compare(b.score, a.score));
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (key == KeyEvent.VK_SPACE) {
                if (gameState == 0) {
                    gameState = 1;
                    startTime = System.currentTimeMillis();
                } else if (gameState == 1) {
                    handleSpacePress();
                }
            }

            if (key == KeyEvent.VK_ENTER && gameState == 2) {
                cardLayout.show(mainPanel, "MENU");
            }

            if (key == KeyEvent.VK_UP && gameState == 1) {
                if (currentSport.contains("Javelin")) {
                    javelinAngle = Math.min(80, javelinAngle + 2);
                } else if (currentSport.contains("Archery")) {
                    bowAngle = Math.min(45, bowAngle + 1);
                    if (!arrowFlying) arrowY = 400 - bowAngle * 5;
                }
            }

            if (key == KeyEvent.VK_DOWN && gameState == 1) {
                if (currentSport.contains("Javelin")) {
                    javelinAngle = Math.max(20, javelinAngle - 2);
                } else if (currentSport.contains("Archery")) {
                    bowAngle = Math.max(-30, bowAngle - 1);
                    if (!arrowFlying) arrowY = 400 - bowAngle * 5;
                }
            }

            if (key == KeyEvent.VK_ESCAPE) {
                gameTimer.stop();
                cardLayout.show(mainPanel, "MENU");
            }
        }

        private void handleSpacePress() {
            if (currentSport.contains("Sprint")) {
                sprintTaps++;
                lastTapTime = System.currentTimeMillis();
            } else if (currentSport.contains("Swimming")) {
                swimStrokes++;
            } else if (currentSport.contains("Long Jump")) {
                if (!isJumping) {
                    runupSpeed = Math.min(12, runupSpeed + 0.8);
                }
            } else if (currentSport.contains("Javelin")) {
                if (!javelinThrown) {
                    javelinPower = Math.min(100, javelinPower + 3);
                }
            } else if (currentSport.contains("Archery")) {
                if (!arrowFlying) {
                    arrowPower = Math.min(100, arrowPower + 2);
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE && gameState == 1) {
                if (currentSport.contains("Long Jump") && !isJumping && runupSpeed > 0) {
                    isJumping = true;
                    jumpX = player.x;
                    jumpY = player.y;
                    double angle = Math.toRadians(45);
                    double power = runupSpeed * 1.5;
                    jumpVelX = Math.cos(angle) * power;
                    jumpVelY = -Math.sin(angle) * power;
                } else if (currentSport.contains("Javelin") && !javelinThrown && javelinPower > 0) {
                    javelinThrown = true;
                    double angle = Math.toRadians(javelinAngle);
                    double power = javelinPower * 0.2;
                    javelinVelX = Math.cos(angle) * power;
                    javelinVelY = -Math.sin(angle) * power;
                } else if (currentSport.contains("Archery") && !arrowFlying && arrowPower > 0) {
                    arrowFlying = true;
                    double angle = Math.toRadians(-bowAngle);
                    double power = arrowPower * 0.15;
                    arrowVelX = Math.cos(angle) * power;
                    arrowVelY = Math.sin(angle) * power;
                }
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {}
    }

    // ==================== RESULTS PANEL ====================
    class ResultsPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            GradientPaint gradient = new GradientPaint(0, 0, new Color(40, 40, 80),
                    0, getHeight(), new Color(20, 20, 40));
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Serif", Font.BOLD, 48));
            g2d.drawString("🏆 LEADERBOARD 🏆", 400, 80);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 18));

            int y = 150;
            g2d.drawString(String.format("%-5s %-20s %-20s %-15s %-10s",
                    "Rank", "Name", "Sport", "Score", "Medal"), 150, y);
            g2d.drawLine(150, y + 10, 1050, y + 10);

            y += 40;
            int rank = 1;
            for (AthleteRecord record : leaderboard) {
                if (rank > 15) break;
                g2d.setColor(rank <= 3 ? Color.YELLOW : Color.WHITE);
                g2d.drawString(String.format("%-5d %-20s %-20s %-15.2f %-10s",
                        rank, record.name, record.sport, record.score, record.medal), 150, y);
                y += 30;
                rank++;
            }
        }
    }

    // ==================== HELPER CLASSES ====================
    class Athlete {
        double x, y;
        int animFrame;

        Athlete() {
            x = 100;
            y = 400;
            animFrame = 0;
        }
    }

    class Competitor {
        String name;
        double x, y, speed;
        int animFrame;

        Competitor(String name) {
            this.name = name;
            this.animFrame = (int)(Math.random() * 8);
        }
    }

    class AthleteRecord {
        String name, sport, medal;
        double score;

        AthleteRecord(String name, String sport, double score, String medal) {
            this.name = name;
            this.sport = sport;
            this.score = score;
            this.medal = medal;
        }
    }

    enum ParticleType { DUST, SPLASH, CONFETTI, SAND }

    class Particle {
        double x, y, vx, vy;
        Color color;
        int life, maxLife;
        ParticleType type;

        Particle(double x, double y, Color color, ParticleType type) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.type = type;
            this.maxLife = 30 + (int)(Math.random() * 30);
            this.life = maxLife;

            switch (type) {
                case DUST:
                    vx = -2 + Math.random() * 1;
                    vy = -1 + Math.random() * 0.5;
                    break;
                case SPLASH:
                    vx = -2 + Math.random() * 4;
                    vy = -3 + Math.random() * 2;
                    break;
                case CONFETTI:
                    vx = -5 + Math.random() * 10;
                    vy = -10 + Math.random() * 5;
                    break;
                case SAND:
                    vx = -3 + Math.random() * 6;
                    vy = -5 + Math.random() * 2;
                    break;
            }
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.2;
            life--;

            if (type == ParticleType.CONFETTI) {
                vx *= 0.98;
            }
        }

        boolean isDead() {
            return life <= 0;
        }

        void draw(Graphics2D g2d) {
            int alpha = (int)(255 * ((double)life / maxLife));
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));

            switch (type) {
                case DUST:
                case SAND:
                    g2d.fillOval((int)x, (int)y, 4, 4);
                    break;
                case SPLASH:
                    g2d.fillOval((int)x, (int)y, 6, 6);
                    break;
                case CONFETTI:
                    g2d.fillRect((int)x, (int)y, 8, 8);
                    break;
            }
        }
    }

    // ==================== NAVIGATION METHODS ====================
    private void startSport(String sport) {
        gamePanel.startGame(sport);
        cardLayout.show(mainPanel, "GAME");
    }

    private void showLeaderboard() {
        resultsPanel.repaint();
        cardLayout.show(mainPanel, "RESULTS");
    }
}