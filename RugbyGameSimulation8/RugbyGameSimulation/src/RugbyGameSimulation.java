import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class RugbyGameSimulation extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
    // Game states
    private enum GameState {
        MENU, PLAYING, PAUSED, TRY_SCORED, CONVERSION, PENALTY, DROP_GOAL, GAME_OVER, SETTINGS
    }

    // Team enum with enhanced properties
    private enum Team {
        RED("Red Dragons", new Color(200, 30, 30), new Color(255, 100, 100)),
        BLUE("Blue Sharks", new Color(30, 60, 200), new Color(100, 150, 255)),
        GREEN("Green Warriors", new Color(30, 150, 30), new Color(100, 255, 100));

        final String name;
        final Color primaryColor;
        final Color secondaryColor;

        Team(String name, Color primary, Color secondary) {
            this.name = name;
            this.primaryColor = primary;
            this.secondaryColor = secondary;
        }
    }

    // Player position enum
    private enum Position {
        PROP(1, "Prop", 0.8, 1.2, 1.0),
        HOOKER(2, "Hooker", 0.9, 1.1, 1.1),
        LOCK(4, "Lock", 1.0, 1.0, 1.0),
        FLANKER(6, "Flanker", 1.2, 0.9, 1.2),
        NUMBER8(8, "Number 8", 1.1, 0.8, 1.3),
        SCRUMHALF(9, "Scrum Half", 1.3, 1.3, 0.7),
        FLYHALF(10, "Fly Half", 1.4, 1.2, 0.8),
        CENTER(12, "Center", 1.2, 1.1, 0.9),
        WING(11, "Wing", 1.5, 1.4, 0.6),
        FULLBACK(15, "Full Back", 1.3, 1.3, 0.8);

        final int number;
        final String name;
        final double speedMultiplier;
        final double passMultiplier;
        final double tackleMultiplier;

        Position(int number, String name, double speed, double pass, double tackle) {
            this.number = number;
            this.name = name;
            this.speedMultiplier = speed;
            this.passMultiplier = pass;
            this.tackleMultiplier = tackle;
        }
    }

    // Enhanced Player class
    class Player {
        double x, y;
        double vx, vy;
        int number;
        Team team;
        Position position;
        boolean hasBall;
        boolean isTackling = false;
        boolean isRucking = false;
        boolean isPassing = false;
        int tackleCooldown = 0;
        int passCooldown = 0;
        int ruckCooldown = 0;
        double stamina = 100.0;
        double speed;
        double tackleRadius;
        double passAccuracy;
        Color jerseyColor;
        int playerWidth = 28;
        int playerHeight = 40;
        double targetX, targetY;
        double animationAngle = 0;
        int animationFrame = 0;
        boolean isSelected = false;

        Player(double x, double y, int number, Team team, Position position) {
            this.x = x;
            this.y = y;
            this.number = number;
            this.team = team;
            this.position = position;
            this.hasBall = false;
            this.speed = 2.5 * position.speedMultiplier;
            this.tackleRadius = 25 * position.tackleMultiplier;
            this.passAccuracy = 0.8 * position.passMultiplier;
            this.jerseyColor = team.primaryColor;
            this.targetX = x;
            this.targetY = y;
        }

        void moveTowards(double targetX, double targetY) {
            if (isTackling || ruckCooldown > 0) return;

            this.targetX = targetX;
            this.targetY = targetY;

            double dx = targetX - x;
            double dy = targetY - y;
            double dist = Math.sqrt(dx*dx + dy*dy);

            if (dist > 10) {
                double speedMultiplier = Math.min(1.0, stamina / 100.0);
                vx = (dx / dist) * speed * speedMultiplier;
                vy = (dy / dist) * speed * speedMultiplier;

                // Update animation angle based on movement
                if (dist > 20) {
                    animationAngle = Math.atan2(dy, dx);
                }
            } else {
                vx *= 0.8;
                vy *= 0.8;
            }

            // Stamina drain
            if (dist > 0) {
                stamina = Math.max(20.0, stamina - 0.05);
            } else {
                stamina = Math.min(100.0, stamina + 0.1);
            }
        }

        void update() {
            // Update cooldowns
            if (tackleCooldown > 0) tackleCooldown--;
            if (passCooldown > 0) passCooldown--;
            if (ruckCooldown > 0) ruckCooldown--;

            // Reset states
            if (tackleCooldown == 0) isTackling = false;
            if (passCooldown == 0) isPassing = false;

            // Apply velocity
            x += vx;
            y += vy;

            // Boundary constraints
            x = Math.max(playerWidth/2 + 30, Math.min(width - playerWidth/2 - 30, x));
            y = Math.max(playerHeight/2 + 30, Math.min(height - playerHeight/2 - 50, y));

            // Friction
            vx *= 0.85;
            vy *= 0.85;

            // Update animation
            animationFrame = (animationFrame + 1) % 60;
        }

        void tackle(Player opponent) {
            if (tackleCooldown == 0 && !isTackling && stamina > 30) {
                double tackleChance = 0.7 * position.tackleMultiplier * (stamina / 100.0);

                if (Math.random() < tackleChance) {
                    isTackling = true;
                    tackleCooldown = 60;
                    stamina -= 25;

                    opponent.isTackling = true;
                    opponent.tackleCooldown = 45;

                    if (opponent.hasBall) {
                        ball.x = opponent.x;
                        ball.y = opponent.y - 15;
                        ball.vx = (Math.random() - 0.5) * 12;
                        ball.vy = -Math.abs(Math.random() * 8);
                        opponent.hasBall = false;
                        ball.isLoose = true;

                        // Create tackle effect
                        createTackleEffect(x, y, opponent.x, opponent.y);
                    }
                }
            }
        }

        void passTo(Player receiver) {
            if (passCooldown == 0 && hasBall && !isPassing) {
                double passSuccess = passAccuracy * (stamina / 100.0);

                if (Math.random() < passSuccess) {
                    hasBall = false;
                    receiver.hasBall = true;
                    isPassing = true;
                    passCooldown = 30;

                    // Animate pass
                    createPassEffect(x, y, receiver.x, receiver.y);

                    // Update ball
                    ball.x = receiver.x;
                    ball.y = receiver.y - 15;
                    ball.lastHolder = receiver;
                } else {
                    // Bad pass
                    hasBall = false;
                    ball.x = x + (Math.random() - 0.5) * 50;
                    ball.y = y + (Math.random() - 0.5) * 30;
                    ball.vx = (Math.random() - 0.5) * 10;
                    ball.vy = (Math.random() - 0.5) * 10;
                    ball.isLoose = true;
                    isPassing = true;
                    passCooldown = 30;
                }
            }
        }

        void draw(Graphics2D g2d) {
            // Save transformation
            AffineTransform oldTransform = g2d.getTransform();

            // Translate to player position
            g2d.translate(x, y);

            // Rotate for running animation
            if (Math.abs(vx) > 0.1 || Math.abs(vy) > 0.1) {
                g2d.rotate(animationAngle);
            }

            // Draw player body (rugby player shape)
            // Torso
            Ellipse2D torso = new Ellipse2D.Double(-playerWidth/2, -playerHeight/2, playerWidth, playerHeight);
            g2d.setColor(jerseyColor);
            g2d.fill(torso);

            // Jersey details
            g2d.setColor(team.secondaryColor);
            g2d.fillRect(-playerWidth/2, -playerHeight/2, playerWidth, 8);
            g2d.fillRect(-playerWidth/2, -playerHeight/2 + playerHeight - 8, playerWidth, 8);

            // Arms (animated)
            int armSwing = (int)(Math.sin(animationFrame * 0.2) * 10);
            g2d.setColor(new Color(255, 220, 180));
            g2d.fillRoundRect(-playerWidth/2 - 5, -5 + armSwing, 6, 16, 3, 3);
            g2d.fillRoundRect(playerWidth/2 - 1, -5 - armSwing, 6, 16, 3, 3);

            // Legs (animated)
            g2d.fillRoundRect(-8, playerHeight/2 - 10, 6, 20, 3, 3);
            g2d.fillRoundRect(2, playerHeight/2 - 10, 6, 20, 3, 3);

            // Head
            Ellipse2D head = new Ellipse2D.Double(-8, -playerHeight/2 - 10, 16, 16);
            g2d.setColor(new Color(255, 220, 180));
            g2d.fill(head);

            // Outline
            g2d.setColor(Color.BLACK);
            g2d.draw(torso);
            g2d.draw(head);

            // Number
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String numStr = String.valueOf(number);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(numStr, -fm.stringWidth(numStr)/2, 5);

            // Position indicator
            if (isSelected) {
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Arial", Font.PLAIN, 9));
                g2d.drawString(position.name, -fm.stringWidth(position.name)/2, -playerHeight/2 - 15);
            }

            // Restore transformation
            g2d.setTransform(oldTransform);

            // Draw selection circle
            if (isSelected) {
                g2d.setColor(new Color(255, 255, 0, 150));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval((int)(x - playerWidth/2 - 5), (int)(y - playerHeight/2 - 5),
                        playerWidth + 10, playerHeight + 10);
            }

            // Draw stamina bar
            if (stamina < 100) {
                g2d.setColor(Color.BLACK);
                g2d.fillRect((int)(x - 20), (int)(y - playerHeight/2 - 20), 40, 4);
                g2d.setColor(stamina > 50 ? Color.GREEN : stamina > 25 ? Color.YELLOW : Color.RED);
                g2d.fillRect((int)(x - 20), (int)(y - playerHeight/2 - 20), (int)(40 * stamina / 100), 4);
            }

            // Draw tackling/running effects
            if (isTackling) {
                g2d.setColor(new Color(255, 100, 0, 100));
                g2d.fillOval((int)(x - 25), (int)(y - 25), 50, 50);
            }

            // Draw ball indicator
            if (hasBall) {
                g2d.setColor(new Color(255, 215, 0, 200));
                g2d.fillOval((int)(x - 12), (int)(y - playerHeight/2 - 25), 24, 16);
            }
        }
    }

    // Enhanced RugbyBall class with spin
    class RugbyBall {
        double x, y;
        double vx, vy;
        double spin = 0;
        double gravity = 0.3;
        double airResistance = 0.99;
        double bounceDamping = 0.7;
        boolean isKicked = false;
        boolean isLoose = false;
        int kickTimer = 0;
        int groundTimer = 0;
        Player lastHolder = null;
        double rotation = 0;

        void update() {
            // Apply gravity and air resistance
            vy += gravity;
            vx *= airResistance;
            vy *= airResistance;

            // Apply spin effect
            vx += spin * 0.01;

            // Update position
            x += vx;
            y += vy;

            // Update rotation
            rotation += Math.sqrt(vx*vx + vy*vy) * 0.1;

            // Boundary collision
            if (x < 20) {
                x = 20;
                vx = -vx * bounceDamping;
            }
            if (x > width - 20) {
                x = width - 20;
                vx = -vx * bounceDamping;
            }

            // Ground collision
            if (y > height - 50) {
                y = height - 50;
                vy = -Math.abs(vy) * bounceDamping;
                groundTimer = 30;

                if (Math.abs(vy) < 1.0) {
                    vy = 0;
                    isKicked = false;
                    if (groundTimer > 0) groundTimer--;
                }
            }

            if (y < 20) {
                y = 20;
                vy = -vy * bounceDamping;
            }

            if (kickTimer > 0) kickTimer--;
        }

        void kick(double angle, double power, double spin) {
            vx = Math.cos(angle) * power * 15;
            vy = Math.sin(angle) * power * 10;
            this.spin = spin;
            isKicked = true;
            isLoose = true;
            kickTimer = 40;
            lastHolder = null;
        }

        void draw(Graphics2D g2d) {
            // Save transformation
            AffineTransform oldTransform = g2d.getTransform();
            g2d.translate(x, y);
            g2d.rotate(rotation);

            // Draw rugby ball shape
            Ellipse2D ballShape = new Ellipse2D.Double(-12, -8, 24, 16);

            // Ball gradient
            GradientPaint gradient = new GradientPaint(
                    -10, -5, new Color(255, 240, 200),
                    10, 5, new Color(160, 100, 40)
            );
            g2d.setPaint(gradient);
            g2d.fill(ballShape);

            // Ball stitching pattern
            g2d.setColor(new Color(80, 40, 0));
            g2d.setStroke(new BasicStroke(1.5f));

            // Main stitching
            g2d.drawLine(-8, 0, 8, 0);
            g2d.drawLine(0, -5, 0, 5);

            // Side stitches
            for (int i = -6; i <= 6; i += 4) {
                g2d.drawLine(i, -4, i, 4);
            }

            // End stitches
            g2d.drawOval(-10, -6, 4, 12);
            g2d.drawOval(6, -6, 4, 12);

            // Ball outline
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.draw(ballShape);

            // Restore transformation
            g2d.setTransform(oldTransform);

            // Draw kick effect
            if (kickTimer > 0) {
                int alpha = (int)(100 * (kickTimer / 40.0));
                g2d.setColor(new Color(255, 255, 200, alpha));
                g2d.fillOval((int)(x - 20), (int)(y - 20), 40, 40);
            }
        }
    }

    // Try Area with goal posts
    class TryArea {
        int x, y, width, height;
        Team team;
        GoalPost goalPost;

        class GoalPost {
            int x, y;
            int width = 10;
            int height = 200;
            int crossbarHeight = 150;
            int crossbarWidth = 60;

            GoalPost(int x, int y) {
                this.x = x;
                this.y = y;
            }

            void draw(Graphics2D g2d) {
                // Draw posts
                g2d.setColor(new Color(150, 100, 50));
                g2d.fillRect(x - width/2, y, width, height);
                g2d.fillRect(x + crossbarWidth - width/2, y, width, height);

                // Draw crossbar
                g2d.fillRect(x, y + crossbarHeight, crossbarWidth, 8);

                // Draw net
                g2d.setColor(new Color(200, 200, 200, 100));
                for (int i = 0; i < 5; i++) {
                    g2d.drawLine(x + i * 12, y + crossbarHeight, x + i * 12, y + crossbarHeight + 50);
                }
            }
        }

        TryArea(int x, int y, int width, int height, Team team) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.team = team;
            this.goalPost = new GoalPost(x + width/2, y + height - 180);
        }

        boolean contains(double ballX, double ballY) {
            return ballX >= x && ballX <= x + width &&
                    ballY >= y && ballY <= y + height;
        }

        void draw(Graphics2D g2d) {
            // Try area background
            Color areaColor = (team == Team.RED) ?
                    new Color(255, 200, 200, 80) :
                    new Color(200, 200, 255, 80);
            g2d.setColor(areaColor);
            g2d.fillRect(x, y, width, height);

            // Try area outline
            g2d.setColor(team.primaryColor);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(x, y, width, height);

            // Draw "TRY" text
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("TRY", x + width/2 - 20, y + height/2);

            // Draw goal posts for conversions
            goalPost.draw(g2d);
        }
    }

    // Particle System with multiple particle types
    class Particle {
        double x, y, vx, vy, size, life, maxLife;
        Color color;
        boolean isTrail = false;

        Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.vx = (Math.random() - 0.5) * 6;
            this.vy = (Math.random() - 0.5) * 6 - 2;
            this.size = Math.random() * 6 + 2;
            this.maxLife = 30 + Math.random() * 40;
            this.life = maxLife;
            this.color = color;
        }

        Particle(double x, double y, Color color, boolean isTrail) {
            this(x, y, color);
            this.isTrail = isTrail;
            this.vx *= 0.3;
            this.vy *= 0.3;
            this.size = Math.random() * 4 + 1;
            this.maxLife = 20 + Math.random() * 20;
            this.life = maxLife;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.15; // gravity
            vx *= 0.97;
            vy *= 0.97;
            life--;
        }

        void draw(Graphics2D g2d) {
            int alpha = (int)(255 * (life / maxLife));
            if (alpha < 0) alpha = 0;

            if (isTrail) {
                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha/2));
                g2d.fillOval((int)(x - size/2), (int)(y - size/2), (int)size, (int)size);
            } else {
                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                g2d.fillOval((int)(x - size/2), (int)(y - size/2), (int)size, (int)size);

                // Glow effect
                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha/3));
                g2d.fillOval((int)(x - size), (int)(y - size), (int)(size*2), (int)(size*2));
            }
        }
    }

    // Game variables
    private GameState gameState = GameState.MENU;
    private Timer gameTimer;
    private int width = 900, height = 650;
    private RugbyBall ball;
    private List<Player> players;
    private List<TryArea> tryAreas;
    private List<Particle> particles;
    private List<Player> selectedPlayers = new ArrayList<>();

    private int redScore = 0, blueScore = 0;
    private int gameTime = 2400; // 40 minutes game time (60 seconds * 40)
    private int halfTime = 1200;
    private int lastScoreTime = 0;
    private Random random = new Random();

    // Animation variables
    private int menuAnimation = 0;
    private int scoreAnimation = 0;
    private int weatherEffect = 0;
    private double cameraX = 0, cameraY = 0;
    private double cameraShake = 0;

    // Game mode
    private enum GameMode {
        FRIENDLY, TOURNAMENT, TRAINING
    }
    private GameMode currentMode = GameMode.FRIENDLY;

    // Menu system
    private ArrayList<MenuButton> menuButtons = new ArrayList<>();
    private MenuButton selectedButton = null;

    class MenuButton {
        Rectangle bounds;
        String text;
        Color color;
        boolean isHovered = false;

        MenuButton(int x, int y, int w, int h, String text, Color color) {
            this.bounds = new Rectangle(x, y, w, h);
            this.text = text;
            this.color = color;
        }

        void draw(Graphics2D g2d) {
            // Button background
            GradientPaint gradient = new GradientPaint(
                    bounds.x, bounds.y, isHovered ? color.brighter() : color,
                    bounds.x, bounds.y + bounds.height, isHovered ? color.darker() : color.darker().darker()
            );
            g2d.setPaint(gradient);
            g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 20, 20);

            // Button border
            g2d.setColor(isHovered ? Color.YELLOW : Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 20, 20);

            // Button text
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g2d.drawString(text,
                    bounds.x + bounds.width/2 - textWidth/2,
                    bounds.y + bounds.height/2 + 7);
        }
    }

    // Weather system
    class Weather {
        List<RainDrop> rainDrops = new ArrayList<>();
        boolean isRaining = false;
        int rainIntensity = 0;

        class RainDrop {
            double x, y, speed, length;

            RainDrop() {
                reset();
            }

            void reset() {
                x = Math.random() * width;
                y = Math.random() * height - height;
                speed = 5 + Math.random() * 10;
                length = 10 + Math.random() * 20;
            }

            void update() {
                y += speed;
                if (y > height) {
                    reset();
                }
            }

            void draw(Graphics2D g2d) {
                g2d.setColor(new Color(150, 150, 255, 150));
                g2d.drawLine((int)x, (int)y, (int)x, (int)(y + length));
            }
        }

        Weather() {
            for (int i = 0; i < 100; i++) {
                rainDrops.add(new RainDrop());
            }
        }

        void update() {
            if (isRaining) {
                for (RainDrop drop : rainDrops) {
                    drop.update();
                }
            }
        }

        void draw(Graphics2D g2d) {
            if (isRaining) {
                for (RainDrop drop : rainDrops) {
                    drop.draw(g2d);
                }
            }
        }
    }

    private Weather weather = new Weather();

    public RugbyGameSimulation() {
        setPreferredSize(new Dimension(width, height));
        setBackground(new Color(80, 160, 80));

        // Initialize menu buttons
        initializeMenu();

        // Initialize game objects
        initGame();

        // Setup timer for game loop
        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();

        // Setup input
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        setFocusable(true);
        requestFocus();
    }

    private void initializeMenu() {
        int centerX = width / 2 - 100;
        menuButtons.add(new MenuButton(centerX, 200, 200, 50, "QUICK MATCH", new Color(50, 150, 50)));
        menuButtons.add(new MenuButton(centerX, 270, 200, 50, "TOURNAMENT", new Color(50, 100, 200)));
        menuButtons.add(new MenuButton(centerX, 340, 200, 50, "TRAINING", new Color(200, 150, 50)));
        menuButtons.add(new MenuButton(centerX, 410, 200, 50, "SETTINGS", new Color(150, 50, 150)));
        menuButtons.add(new MenuButton(centerX, 480, 200, 50, "EXIT GAME", new Color(200, 50, 50)));
    }

    private void initGame() {
        // Create ball at center
        ball = new RugbyBall();
        resetBall();

        // Create try areas
        tryAreas = new ArrayList<>();
        tryAreas.add(new TryArea(40, height/2 - 100, 40, 200, Team.RED));
        tryAreas.add(new TryArea(width - 80, height/2 - 100, 40, 200, Team.BLUE));

        // Create players
        players = new ArrayList<>();

        // Red team formations
        Position[] redPositions = {
                Position.PROP, Position.HOOKER, Position.PROP,
                Position.LOCK, Position.LOCK,
                Position.FLANKER, Position.FLANKER, Position.NUMBER8,
                Position.SCRUMHALF, Position.FLYHALF,
                Position.CENTER, Position.CENTER,
                Position.WING, Position.WING,
                Position.FULLBACK
        };

        // Blue team formations
        Position[] bluePositions = {
                Position.PROP, Position.HOOKER, Position.PROP,
                Position.LOCK, Position.LOCK,
                Position.FLANKER, Position.FLANKER, Position.NUMBER8,
                Position.SCRUMHALF, Position.FLYHALF,
                Position.CENTER, Position.CENTER,
                Position.WING, Position.WING,
                Position.FULLBACK
        };

        // Create Red team (15 players)
        for (int i = 0; i < 15; i++) {
            double x = 200 + (i % 5) * 70;
            double y = 100 + (i / 5) * 90;
            Player p = new Player(x, y, redPositions[i].number, Team.RED, redPositions[i]);
            players.add(p);
        }

        // Create Blue team (15 players)
        for (int i = 0; i < 15; i++) {
            double x = width - 200 - (i % 5) * 70;
            double y = 100 + (i / 5) * 90;
            Player p = new Player(x, y, bluePositions[i].number, Team.BLUE, bluePositions[i]);
            players.add(p);
        }

        // Give ball to a random player
        Player startingPlayer = players.get(random.nextInt(15));
        startingPlayer.hasBall = true;
        ball.lastHolder = startingPlayer;
        ball.x = startingPlayer.x;
        ball.y = startingPlayer.y - 20;

        // Initialize particles
        particles = new ArrayList<>();

        // Reset scores and time
        redScore = 0;
        blueScore = 0;
        gameTime = 2400;

        // Random weather
        weather.isRaining = random.nextDouble() < 0.3;
        weather.rainIntensity = weather.isRaining ? 50 + random.nextInt(100) : 0;
    }

    private void resetBall() {
        ball.x = width / 2;
        ball.y = height / 2;
        ball.vx = 0;
        ball.vy = 0;
        ball.spin = 0;
        ball.isKicked = false;
        ball.isLoose = false;
    }

    private void updateGame() {
        if (gameState != GameState.PLAYING) return;

        gameTime--;

        // Check halftime
        if (gameTime == halfTime) {
            // Switch sides
            for (Player player : players) {
                player.x = width - player.x;
                player.y = height - player.y;
            }
            resetBall();
        }

        if (gameTime <= 0) {
            gameState = GameState.GAME_OVER;
            return;
        }

        // Update weather
        weather.update();

        // Update ball
        ball.update();

        // Update camera with shake effect
        cameraShake *= 0.9;
        double shakeX = (Math.random() - 0.5) * cameraShake;
        double shakeY = (Math.random() - 0.5) * cameraShake;

        // Center camera on ball with smoothing
        double targetX = ball.x - width / 2;
        double targetY = ball.y - height / 2;

        cameraX += (targetX - cameraX) * 0.1 + shakeX;
        cameraY += (targetY - cameraY) * 0.1 + shakeY;

        // Constrain camera
        cameraX = Math.max(0, Math.min(width, cameraX));
        cameraY = Math.max(0, Math.min(height, cameraY));

        // Update players
        for (Player player : players) {
            player.update();

            // AI behavior based on position
            if (player.hasBall) {
                handlePlayerWithBall(player);
            } else {
                handlePlayerWithoutBall(player);
            }

            // Check ball pickup
            if (ball.isLoose && ball.groundTimer <= 0) {
                double dist = Math.sqrt(Math.pow(player.x - ball.x, 2) +
                        Math.pow(player.y - ball.y, 2));
                if (dist < 25 && !player.hasBall && !player.isTackling) {
                    player.hasBall = true;
                    ball.lastHolder = player;
                    ball.isLoose = false;
                    ball.x = player.x;
                    ball.y = player.y - 15;
                }
            }

            // If player has ball, ball follows player
            if (player.hasBall) {
                ball.x = player.x;
                ball.y = player.y - 15;
            }
        }

        // Check for scoring
        checkForScoring();

        // Update particles
        Iterator<Particle> particleIter = particles.iterator();
        while (particleIter.hasNext()) {
            Particle p = particleIter.next();
            p.update();
            if (p.life <= 0) {
                particleIter.remove();
            }
        }

        // User input for selected players
        for (Player player : selectedPlayers) {
            handlePlayerInput(player);
        }

        // Update animations
        menuAnimation = (menuAnimation + 1) % 360;
        weatherEffect = (weatherEffect + 1) % 100;

        if (scoreAnimation > 0) {
            scoreAnimation--;
            if (scoreAnimation == 0 && gameState == GameState.TRY_SCORED) {
                gameState = GameState.CONVERSION;
            }
        }
    }

    private void handlePlayerWithBall(Player player) {
        TryArea targetArea = (player.team == Team.RED) ? tryAreas.get(1) : tryAreas.get(0);

        // Move towards try line
        player.moveTowards(targetArea.x + targetArea.width/2, targetArea.y + targetArea.height/2);

        // Look for passing opportunities
        if (Math.random() < 0.01) {
            Player bestReceiver = findBestReceiver(player);
            if (bestReceiver != null) {
                player.passTo(bestReceiver);
            }
        }

        // Random kick chance
        if (Math.random() < 0.005 && player.position == Position.FLYHALF) {
            attemptDropGoal(player);
        }
    }

    private void handlePlayerWithoutBall(Player player) {
        // Defensive positioning
        if (ball.lastHolder != null && ball.lastHolder.team != player.team) {
            // Defense - go towards ball
            player.moveTowards(ball.x, ball.y);

            // Attempt tackle
            for (Player opponent : players) {
                if (opponent.team != player.team && opponent.hasBall) {
                    double dist = Math.sqrt(Math.pow(player.x - opponent.x, 2) +
                            Math.pow(player.y - opponent.y, 2));
                    if (dist < player.tackleRadius) {
                        player.tackle(opponent);
                        break;
                    }
                }
            }
        } else {
            // Offensive positioning
            Player ballHolder = getBallHolder();
            if (ballHolder != null && ballHolder.team == player.team) {
                // Support positioning
                double angle = Math.random() * Math.PI * 2;
                double dist = 60 + Math.random() * 40;
                double targetX = ballHolder.x + Math.cos(angle) * dist;
                double targetY = ballHolder.y + Math.sin(angle) * dist;
                player.moveTowards(targetX, targetY);
            } else {
                // Default positioning
                player.moveTowards(player.targetX, player.targetY);
            }
        }
    }

    private void handlePlayerInput(Player player) {
        double moveX = 0, moveY = 0;
        if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP]) moveY -= 1;
        if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN]) moveY += 1;
        if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT]) moveX -= 1;
        if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) moveX += 1;

        if (moveX != 0 || moveY != 0) {
            player.moveTowards(player.x + moveX * 50, player.y + moveY * 50);
        }

        // Pass ball
        if (keys[KeyEvent.VK_SPACE] && player.hasBall) {
            Player receiver = findBestReceiver(player);
            if (receiver != null) {
                player.passTo(receiver);
            }
            keys[KeyEvent.VK_SPACE] = false;
        }

        // Kick ball
        if (keys[KeyEvent.VK_K] && player.hasBall) {
            double angle = Math.atan2(player.targetY - player.y, player.targetX - player.x);
            double power = 0.5 + Math.random() * 0.5;
            double spin = (Math.random() - 0.5) * 2;
            ball.kick(angle, power, spin);
            player.hasBall = false;
            createKickEffect(player.x, player.y);
            keys[KeyEvent.VK_K] = false;
        }
    }

    private Player findBestReceiver(Player passer) {
        Player bestReceiver = null;
        double bestScore = 0;

        for (Player player : players) {
            if (player != passer && player.team == passer.team &&
                    !player.isTackling && player.stamina > 30) {

                double dist = Math.sqrt(Math.pow(player.x - passer.x, 2) +
                        Math.pow(player.y - passer.y, 2));

                // Calculate score based on distance, angle, and position
                double angleScore = 1.0 - Math.abs(Math.atan2(player.y - passer.y, player.x - passer.x)) / Math.PI;
                double distScore = 1.0 - Math.min(dist / 200.0, 1.0);
                double positionScore = player.position.passMultiplier;

                double score = angleScore * 0.3 + distScore * 0.4 + positionScore * 0.3;

                if (score > bestScore && dist < 150) {
                    bestScore = score;
                    bestReceiver = player;
                }
            }
        }

        return bestReceiver;
    }

    private Player getBallHolder() {
        for (Player player : players) {
            if (player.hasBall) {
                return player;
            }
        }
        return null;
    }

    private void checkForScoring() {
        for (TryArea area : tryAreas) {
            if (area.contains(ball.x, ball.y) && ball.y > height - 60) {
                if (area.team == Team.RED) {
                    redScore += 5;
                } else {
                    blueScore += 5;
                }
                scoreTry(area.team);
                return;
            }
        }
    }

    private void scoreTry(Team team) {
        lastScoreTime = gameTime;
        scoreAnimation = 180;
        cameraShake = 20;

        // Create celebration effect
        createCelebrationEffect(ball.x, ball.y, team);

        // Reset for conversion attempt
        resetBall();
        ball.x = (team == Team.RED) ? width - 100 : 100;
        ball.y = height - 100;

        // Remove ball from all players
        for (Player player : players) {
            player.hasBall = false;
        }

        gameState = GameState.TRY_SCORED;
    }

    private void attemptDropGoal(Player player) {
        double distance = Math.abs(player.x - (player.team == Team.RED ? width - 80 : 80));
        double accuracy = 0.8 * (100 / (distance + 1));

        if (Math.random() < accuracy) {
            if (player.team == Team.RED) {
                redScore += 3;
            } else {
                blueScore += 3;
            }

            createScoreEffect("DROP GOAL!", player.x, player.y, player.team);
            resetBall();

            // Give ball to opposite team
            Team oppositeTeam = (player.team == Team.RED) ? Team.BLUE : Team.RED;
            for (Player p : players) {
                if (p.team == oppositeTeam && p.position == Position.SCRUMHALF) {
                    p.hasBall = true;
                    ball.lastHolder = p;
                    break;
                }
            }
        }
    }

    private void createTackleEffect(double x1, double y1, double x2, double y2) {
        for (int i = 0; i < 25; i++) {
            double t = i / 25.0;
            double px = x1 + (x2 - x1) * t;
            double py = y1 + (y2 - y1) * t;
            particles.add(new Particle(px, py, Color.ORANGE));
        }

        // Impact particles
        for (int i = 0; i < 15; i++) {
            particles.add(new Particle(x2, y2, Color.RED));
        }
    }

    private void createPassEffect(double x1, double y1, double x2, double y2) {
        for (int i = 0; i < 20; i++) {
            double t = i / 20.0;
            double px = x1 + (x2 - x1) * t;
            double py = y1 + (y2 - y1) * t;
            particles.add(new Particle(px, py, Color.GREEN, true));
        }
    }

    private void createKickEffect(double x, double y) {
        for (int i = 0; i < 30; i++) {
            particles.add(new Particle(x, y, Color.WHITE));
        }
    }

    private void createCelebrationEffect(double x, double y, Team team) {
        for (int i = 0; i < 100; i++) {
            particles.add(new Particle(x, y, team.primaryColor));
            particles.add(new Particle(x, y, team.secondaryColor));
        }
    }

    private void createScoreEffect(String text, double x, double y, Team team) {
        // This would be implemented to show floating text
        // For now, just create particles
        for (int i = 0; i < 50; i++) {
            particles.add(new Particle(x, y, Color.YELLOW));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        // Apply camera transformation
        g2d.translate(-cameraX, -cameraY);

        // Draw field
        drawField(g2d);

        // Draw weather
        weather.draw(g2d);

        // Draw try areas
        for (TryArea area : tryAreas) {
            area.draw(g2d);
        }

        // Draw particles
        for (Particle particle : particles) {
            particle.draw(g2d);
        }

        // Draw players
        for (Player player : players) {
            player.draw(g2d);
        }

        // Draw ball
        ball.draw(g2d);

        // Reset transformation for UI
        g2d.translate(cameraX, cameraY);

        // Draw UI based on game state
        switch (gameState) {
            case MENU:
                drawMenu(g2d);
                break;
            case PLAYING:
                drawGameUI(g2d);
                break;
            case PAUSED:
                drawGameUI(g2d);
                drawPauseScreen(g2d);
                break;
            case TRY_SCORED:
                drawGameUI(g2d);
                drawTryScoredScreen(g2d);
                break;
            case CONVERSION:
                drawGameUI(g2d);
                drawConversionScreen(g2d);
                break;
            case GAME_OVER:
                drawGameUI(g2d);
                drawGameOverScreen(g2d);
                break;
            case SETTINGS:
                drawMenu(g2d);
                drawSettingsScreen(g2d);
                break;
        }
    }

    private void drawField(Graphics2D g2d) {
        // Field with gradient
        GradientPaint fieldGradient = new GradientPaint(
                0, 0, new Color(70, 150, 70),
                0, height, new Color(50, 120, 50)
        );
        g2d.setPaint(fieldGradient);
        g2d.fillRect(0, 0, width, height);

        // Field markings
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));

        // Outer boundaries
        g2d.drawRect(30, 30, width - 60, height - 60);

        // Halfway line
        g2d.drawLine(width / 2, 30, width / 2, height - 30);

        // Center circle
        g2d.drawOval(width / 2 - 50, height / 2 - 50, 100, 100);

        // 22 meter lines
        g2d.drawLine(150, 30, 150, height - 30);
        g2d.drawLine(width - 150, 30, width - 150, height - 30);

        // Hash marks
        g2d.setStroke(new BasicStroke(2));
        for (int i = 100; i < height - 100; i += 40) {
            g2d.drawLine(width/2 - 5, i, width/2 + 5, i);
        }

        // Field details
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("22m", 80, 50);
        g2d.drawString("22m", width - 100, 50);
        g2d.drawString("HALF WAY", width/2 - 40, 25);
    }

    private void drawGameUI(Graphics2D g2d) {
        // Score panel
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(20, 20, 200, 80, 15, 15);
        g2d.fillRoundRect(width - 220, 20, 200, 80, 15, 15);

        // Red team score
        g2d.setColor(Team.RED.primaryColor);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString(String.valueOf(redScore), 120, 70);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("RED DRAGONS", 50, 90);

        // Blue team score
        g2d.setColor(Team.BLUE.primaryColor);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString(String.valueOf(blueScore), width - 140, 70);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("BLUE SHARKS", width - 170, 90);

        // Time display
        int minutes = gameTime / 60;
        int seconds = gameTime % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);

        g2d.setColor(new Color(40, 40, 40, 200));
        g2d.fillRoundRect(width/2 - 60, 20, 120, 50, 10, 10);
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString(timeStr, width/2 - 35, 55);

        // Game info
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Selected: " + selectedPlayers.size() + " players", 20, height - 30);
        g2d.drawString("Mode: " + currentMode, 20, height - 15);

        // Controls help
        String[] controls = {
                "CONTROLS:",
                "Click - Select Player",
                "WASD - Move Selected",
                "SPACE - Pass",
                "K - Kick",
                "P - Pause",
                "ESC - Menu"
        };

        g2d.setColor(new Color(255, 255, 255, 200));
        for (int i = 0; i < controls.length; i++) {
            g2d.drawString(controls[i], width - 180, height - 120 + i * 15);
        }

        // Weather indicator
        if (weather.isRaining) {
            g2d.setColor(new Color(100, 100, 255, 150));
            g2d.fillOval(width - 60, 30, 30, 30);
            g2d.setColor(Color.WHITE);
            g2d.drawString("RAIN", width - 55, 50);
        }
    }

    private void drawMenu(Graphics2D g2d) {
        // Animated background
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, width, height);

        // Animated rugby balls in background
        for (int i = 0; i < 8; i++) {
            double angle = (menuAnimation + i * 45) * Math.PI / 180;
            int ballX = (int)(width/2 + Math.cos(angle) * 300);
            int ballY = (int)(height/2 + Math.sin(angle) * 200);

            g2d.rotate(angle, ballX, ballY);
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillOval(ballX - 15, ballY - 10, 30, 20);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(ballX - 15, ballY - 10, 30, 20);
            g2d.rotate(-angle, ballX, ballY);
        }

        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 64));
        String title = "RUGBY CHAMPIONS 2024";

        // Title shadow effect
        for (int i = 0; i < 3; i++) {
            g2d.setColor(new Color(0, 0, 0, 100 - i * 30));
            g2d.drawString(title, width/2 - 280 + i, 103 + i);
        }

        g2d.setColor(Color.YELLOW);
        g2d.drawString(title, width/2 - 280, 100);

        // Subtitle
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.ITALIC, 20));
        g2d.drawString("Experience the Thrill of Rugby!", width/2 - 150, 150);

        // Draw buttons
        for (MenuButton button : menuButtons) {
            button.draw(g2d);
        }

        // Version info
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Version 2.0 | © 2024 Rugby Sim Studios", width - 250, height - 20);
    }

    private void drawPauseScreen(Graphics2D g2d) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, width, height);

        // Pause text with animation
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        String pauseText = "PAUSED";
        FontMetrics fm = g2d.getFontMetrics();

        int yPos = height/2 - 50 + (int)(Math.sin(menuAnimation * 0.1) * 10);
        g2d.drawString(pauseText, width/2 - fm.stringWidth(pauseText)/2, yPos);

        // Options
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Press P to Resume", width/2 - 100, height/2 + 30);
        g2d.drawString("Press ESC for Menu", width/2 - 110, height/2 + 70);
    }

    private void drawTryScoredScreen(Graphics2D g2d) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, width, height);

        // TRY! animation
        int scale = (int)(Math.sin(scoreAnimation * 0.1) * 20) + 100;
        g2d.setColor(Color.GREEN);
        g2d.setFont(new Font("Arial", Font.BOLD, scale));
        String tryText = "TRY!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(tryText,
                width/2 - fm.stringWidth(tryText)/2,
                height/2 - 50);

        // Points
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString("5 POINTS!", width/2 - 80, height/2 + 30);

        // Conversion attempt info
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Conversion attempt in " + (scoreAnimation/60) + " seconds...",
                width/2 - 150, height/2 + 80);
    }

    private void drawConversionScreen(Graphics2D g2d) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String convText = "CONVERSION ATTEMPT";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(convText, width/2 - fm.stringWidth(convText)/2, height/2 - 100);

        // Instructions
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Press K to kick for 2 extra points", width/2 - 180, height/2);
        g2d.drawString("Press SPACE to decline", width/2 - 120, height/2 + 50);
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, width, height);

        // Game Over text
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        String gameOverText = "FULL TIME";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(gameOverText, width/2 - fm.stringWidth(gameOverText)/2, 150);

        // Winner
        String winnerText;
        Color winnerColor;

        if (redScore > blueScore) {
            winnerText = "RED DRAGONS WIN!";
            winnerColor = Team.RED.primaryColor;
        } else if (blueScore > redScore) {
            winnerText = "BLUE SHARKS WIN!";
            winnerColor = Team.BLUE.primaryColor;
        } else {
            winnerText = "DRAW!";
            winnerColor = Color.YELLOW;
        }

        g2d.setColor(winnerColor);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString(winnerText, width/2 - fm.stringWidth(winnerText)/2, 250);

        // Final score
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 64));
        String finalScore = redScore + " - " + blueScore;
        g2d.drawString(finalScore, width/2 - 80, 350);

        // Statistics
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Match Statistics:", width/2 - 80, 420);
        g2d.drawString("Tries: " + (redScore + blueScore) / 5, width/2 - 80, 450);
        g2d.drawString("Drop Goals: " + ((redScore + blueScore) % 5) / 3, width/2 - 80, 480);

        // Restart options
        g2d.drawString("Press R to Restart", width/2 - 80, 550);
        g2d.drawString("Press ESC for Menu", width/2 - 90, 580);
    }

    private void drawSettingsScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 220));
        g2d.fillRect(width/2 - 200, 150, 400, 350);

        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        g2d.drawString("SETTINGS", width/2 - 70, 200);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));

        String[] settings = {
                "Graphics: High",
                "Sound: On",
                "Weather Effects: " + (weather.isRaining ? "On" : "Off"),
                "AI Difficulty: Medium",
                "Game Length: 40 min"
        };

        for (int i = 0; i < settings.length; i++) {
            g2d.drawString(settings[i], width/2 - 180, 250 + i * 40);
        }

        g2d.drawString("Press ESC to return", width/2 - 100, 450);
    }

    private boolean[] keys = new boolean[256];

    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode < keys.length) {
            keys[keyCode] = true;
        }

        switch (keyCode) {
            case KeyEvent.VK_P:
                if (gameState == GameState.PLAYING) {
                    gameState = GameState.PAUSED;
                } else if (gameState == GameState.PAUSED) {
                    gameState = GameState.PLAYING;
                }
                break;

            case KeyEvent.VK_R:
                if (gameState == GameState.GAME_OVER || gameState == GameState.PLAYING) {
                    initGame();
                    gameState = GameState.PLAYING;
                }
                break;

            case KeyEvent.VK_ESCAPE:
                if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
                    gameState = GameState.MENU;
                } else if (gameState == GameState.SETTINGS || gameState == GameState.GAME_OVER) {
                    gameState = GameState.MENU;
                }
                break;

            case KeyEvent.VK_1:
                if (gameState == GameState.MENU) {
                    currentMode = GameMode.FRIENDLY;
                    initGame();
                    gameState = GameState.PLAYING;
                }
                break;

            case KeyEvent.VK_2:
                if (gameState == GameState.MENU) {
                    currentMode = GameMode.TOURNAMENT;
                    initGame();
                    gameState = GameState.PLAYING;
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode < keys.length) {
            keys[keyCode] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if (gameState == GameState.MENU) {
            for (MenuButton button : menuButtons) {
                if (button.bounds.contains(e.getPoint())) {
                    handleMenuClick(button.text);
                    break;
                }
            }
        } else if (gameState == GameState.PLAYING) {
            // Convert screen to world coordinates
            double worldX = e.getX() + cameraX;
            double worldY = e.getY() + cameraY;

            // Handle player selection
            if (e.getButton() == MouseEvent.BUTTON1) { // Left click
                if (!e.isControlDown()) {
                    selectedPlayers.clear();
                }

                Player clickedPlayer = null;
                double minDist = Double.MAX_VALUE;

                for (Player player : players) {
                    double dist = Math.sqrt(Math.pow(player.x - worldX, 2) +
                            Math.pow(player.y - worldY, 2));
                    if (dist < 30 && dist < minDist) {
                        minDist = dist;
                        clickedPlayer = player;
                    }
                }

                if (clickedPlayer != null) {
                    clickedPlayer.isSelected = true;
                    if (!selectedPlayers.contains(clickedPlayer)) {
                        selectedPlayers.add(clickedPlayer);
                    }
                }
            } else if (e.getButton() == MouseEvent.BUTTON3) { // Right click
                // Set target for selected players
                for (Player player : selectedPlayers) {
                    player.targetX = worldX;
                    player.targetY = worldY;
                }
            }
        }
    }

    private void handleMenuClick(String buttonText) {
        switch (buttonText) {
            case "QUICK MATCH":
                currentMode = GameMode.FRIENDLY;
                initGame();
                gameState = GameState.PLAYING;
                break;
            case "TOURNAMENT":
                currentMode = GameMode.TOURNAMENT;
                initGame();
                gameState = GameState.PLAYING;
                break;
            case "TRAINING":
                currentMode = GameMode.TRAINING;
                initGame();
                gameState = GameState.PLAYING;
                break;
            case "SETTINGS":
                gameState = GameState.SETTINGS;
                break;
            case "EXIT GAME":
                System.exit(0);
                break;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (gameState == GameState.MENU) {
            for (MenuButton button : menuButtons) {
                button.isHovered = button.bounds.contains(e.getPoint());
            }
            repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Rugby Game Simulation 2024 - Enhanced Edition");
            RugbyGameSimulation game = new RugbyGameSimulation();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}