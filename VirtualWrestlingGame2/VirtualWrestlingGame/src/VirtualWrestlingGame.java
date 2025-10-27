import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Random;

public class VirtualWrestlingGame extends JPanel implements ActionListener, KeyListener {
    // Game states
    private static final int MENU = 0;
    private static final int CHARACTER_SELECT = 1;
    private static final int FIGHT = 2;
    private static final int GAME_OVER = 3;

    private int gameState = MENU;

    // Game elements
    private Timer timer;
    private Random random = new Random();

    // Players
    private Player player1, player2;
    private Player currentPlayer;

    // Ring
    private int ringWidth = 700;
    private int ringHeight = 400;
    private int ringX = 50;
    private int ringY = 100;

    // Characters
    private String[] characterNames = {"The Crusher", "Iron Fist", "Thunderbolt", "Steel Hammer"};
    private Color[] characterColors = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE};
    private int selectedCharacter = 0;

    // Animation
    private ArrayList<Particle> particles = new ArrayList<>();
    private int shakeTimer = 0;

    // Game variables
    private int round = 1;
    private int maxRounds = 3;
    private int player1Wins = 0;
    private int player2Wins = 0;
    private int countdown = 60; // 60 seconds per round
    private long lastTime;

    // Sound effects (simulated)
    private boolean playPunchSound = false;
    private boolean playThrowSound = false;
    private boolean playCheerSound = false;

    public VirtualWrestlingGame() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(16, this); // ~60 FPS
        timer.start();

        lastTime = System.currentTimeMillis();

        // Initialize players
        player1 = new Player("Player 1", 200, 300, Color.RED);
        player2 = new Player("CPU", 500, 300, Color.BLUE);
        currentPlayer = player1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Apply screen shake if active
        if (shakeTimer > 0) {
            int shakeX = random.nextInt(5) - 2;
            int shakeY = random.nextInt(5) - 2;
            g2d.translate(shakeX, shakeY);
            shakeTimer--;
        }

        switch (gameState) {
            case MENU:
                drawMenu(g2d);
                break;
            case CHARACTER_SELECT:
                drawCharacterSelect(g2d);
                break;
            case FIGHT:
                drawFight(g2d);
                break;
            case GAME_OVER:
                drawGameOver(g2d);
                break;
        }

        // Draw sound indicators (simulated)
        if (playPunchSound) {
            g2d.setColor(Color.YELLOW);
            g2d.drawString("PUNCH!", 700, 30);
            playPunchSound = false;
        }
        if (playThrowSound) {
            g2d.setColor(Color.CYAN);
            g2d.drawString("THROW!", 700, 50);
            playThrowSound = false;
        }
        if (playCheerSound) {
            g2d.setColor(Color.PINK);
            g2d.drawString("CHEER!", 700, 70);
            playCheerSound = false;
        }
    }

    private void drawMenu(Graphics2D g2d) {
        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString("VIRTUAL WRESTLING", 150, 150);

        // Subtitle
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Ultimate Championship Edition", 220, 200);

        // Menu options
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        g2d.drawString("Press ENTER to Start", 250, 350);
        g2d.drawString("Press ESC to Exit", 270, 400);

        // Draw wrestling ring in background
        drawRing(g2d, 150, 450, 500, 120);

        // Draw wrestler silhouettes
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillOval(200, 470, 60, 60);
        g2d.fillOval(540, 470, 60, 60);
    }

    private void drawCharacterSelect(Graphics2D g2d) {
        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString("SELECT YOUR WRESTLER", 180, 80);

        // Character display
        int charWidth = 150;
        int charHeight = 200;
        int startX = 100;
        int startY = 150;

        for (int i = 0; i < characterNames.length; i++) {
            // Character box
            if (i == selectedCharacter) {
                g2d.setColor(Color.YELLOW);
                g2d.fillRect(startX + i * (charWidth + 50) - 10, startY - 10, charWidth + 20, charHeight + 70);
            }

            g2d.setColor(characterColors[i]);
            g2d.fillRect(startX + i * (charWidth + 50), startY, charWidth, charHeight);

            // Character details
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString(characterNames[i], startX + i * (charWidth + 50) + 10, startY + charHeight + 30);

            // Stats (randomized)
            int power = 70 + i * 10;
            int speed = 90 - i * 10;
            int stamina = 80 + i * 5;

            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            g2d.drawString("Power: " + power, startX + i * (charWidth + 50) + 10, startY + charHeight + 50);
            g2d.drawString("Speed: " + speed, startX + i * (charWidth + 50) + 10, startY + charHeight + 70);
            g2d.drawString("Stamina: " + stamina, startX + i * (charWidth + 50) + 10, startY + charHeight + 90);
        }

        // Instructions
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Use LEFT/RIGHT to select, ENTER to confirm", 180, 500);
    }

    private void drawFight(Graphics2D g2d) {
        // Draw audience
        drawAudience(g2d);

        // Draw ring
        drawRing(g2d, ringX, ringY, ringWidth, ringHeight);

        // Draw players
        player1.draw(g2d);
        player2.draw(g2d);

        // Draw HUD
        drawHUD(g2d);

        // Draw particles
        for (Particle p : particles) {
            p.draw(g2d);
        }

        // Draw round info
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Round " + round, 350, 50);

        // Draw timer
        g2d.drawString("Time: " + countdown, 350, 80);

        // Draw controls help
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Controls: Arrow Keys to move, Z to punch, X to grapple", 250, 550);
    }

    private void drawGameOver(Graphics2D g2d) {
        // Background
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Result
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));

        if (player1Wins > player2Wins) {
            g2d.drawString("PLAYER 1 WINS!", 200, 200);
        } else {
            g2d.drawString("CPU WINS!", 280, 200);
        }

        // Score
        g2d.setFont(new Font("Arial", Font.PLAIN, 36));
        g2d.drawString("Final Score: " + player1Wins + " - " + player2Wins, 250, 280);

        // Options
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Press ENTER to play again", 270, 380);
        g2d.drawString("Press ESC to exit", 300, 430);
    }

    private void drawRing(Graphics2D g2d, int x, int y, int width, int height) {
        // Ring floor
        g2d.setColor(new Color(200, 200, 255));
        g2d.fillRect(x, y, width, height);

        // Ring ropes
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(x - 10, y - 10, width + 20, height + 20);
        g2d.drawRect(x - 20, y - 20, width + 40, height + 40);
        g2d.drawRect(x - 30, y - 30, width + 60, height + 60);

        // Ring posts
        g2d.setColor(Color.RED);
        g2d.fillRect(x - 35, y - 35, 10, 10);
        g2d.fillRect(x + width + 25, y - 35, 10, 10);
        g2d.fillRect(x - 35, y + height + 25, 10, 10);
        g2d.fillRect(x + width + 25, y + height + 25, 10, 10);

        // Ring corners
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(5));
        g2d.drawLine(x, y, x + 50, y);
        g2d.drawLine(x, y, x, y + 50);

        g2d.drawLine(x + width, y, x + width - 50, y);
        g2d.drawLine(x + width, y, x + width, y + 50);

        g2d.drawLine(x, y + height, x + 50, y + height);
        g2d.drawLine(x, y + height, x, y + height - 50);

        g2d.drawLine(x + width, y + height, x + width - 50, y + height);
        g2d.drawLine(x + width, y + height, x + width, y + height - 50);
    }

    private void drawAudience(Graphics2D g2d) {
        // Simple audience representation
        g2d.setColor(new Color(20, 20, 40));
        g2d.fillRect(0, 0, getWidth(), ringY - 30);

        // Audience members (simplified)
        g2d.setColor(new Color(100, 100, 150));
        for (int i = 0; i < 100; i++) {
            int x = random.nextInt(getWidth());
            int y = random.nextInt(ringY - 30);
            g2d.fillRect(x, y, 2, 4);
        }
    }

    private void drawHUD(Graphics2D g2d) {
        // Player 1 HUD
        g2d.setColor(player1.getColor());
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString(player1.getName(), 50, 40);

        // Health bar
        g2d.setColor(Color.RED);
        g2d.fillRect(50, 50, 200, 20);
        g2d.setColor(Color.GREEN);
        g2d.fillRect(50, 50, (int)(200 * (player1.getHealth() / 100.0)), 20);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(50, 50, 200, 20);

        // Player 2 HUD
        g2d.setColor(player2.getColor());
        g2d.drawString(player2.getName(), 550, 40);

        // Health bar
        g2d.setColor(Color.RED);
        g2d.fillRect(550, 50, 200, 20);
        g2d.setColor(Color.GREEN);
        g2d.fillRect(550, 50, (int)(200 * (player2.getHealth() / 100.0)), 20);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(550, 50, 200, 20);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == FIGHT) {
            // Update game time
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTime >= 1000) {
                countdown--;
                lastTime = currentTime;

                if (countdown <= 0) {
                    endRound();
                }
            }

            // Update players
            player1.update();
            player2.update();

            // CPU AI
            if (!player2.isStunned()) {
                // Simple AI: move toward player and attack sometimes
                if (Math.abs(player1.getX() - player2.getX()) < 100) {
                    if (random.nextInt(100) < 5) {
                        player2.punch();
                        playPunchSound = true;
                    } else if (random.nextInt(100) < 3) {
                        player2.grapple();
                        playThrowSound = true;
                    }
                }

                // Move toward player
                if (player1.getX() < player2.getX()) {
                    player2.moveLeft();
                } else {
                    player2.moveRight();
                }
            }

            // Check collisions
            checkCollisions();

            // Update particles
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update();
                if (p.isDead()) {
                    particles.remove(i);
                }
            }

            // Check for round end
            if (player1.getHealth() <= 0 || player2.getHealth() <= 0) {
                endRound();
            }
        }

        repaint();
    }

    private void checkCollisions() {
        // Simple collision detection
        int distance = Math.abs(player1.getX() - player2.getX());

        if (distance < 60) {
            // Players are close enough to interact

            if (player1.isPunching() && !player2.isBlocking()) {
                player2.takeDamage(5);
                createParticles(player2.getX(), player2.getY(), player1.getColor());
                shakeTimer = 10;
                playPunchSound = true;
            }

            if (player2.isPunching() && !player1.isBlocking()) {
                player1.takeDamage(5);
                createParticles(player1.getX(), player1.getY(), player2.getColor());
                shakeTimer = 10;
                playPunchSound = true;
            }

            if (player1.isGrappling() && !player2.isBlocking()) {
                player2.takeDamage(10);
                player2.setStunned(true);
                createParticles(player2.getX(), player2.getY(), player1.getColor());
                shakeTimer = 15;
                playThrowSound = true;
            }

            if (player2.isGrappling() && !player1.isBlocking()) {
                player1.takeDamage(10);
                player1.setStunned(true);
                createParticles(player1.getX(), player1.getY(), player2.getColor());
                shakeTimer = 15;
                playThrowSound = true;
            }
        }
    }

    private void createParticles(int x, int y, Color color) {
        for (int i = 0; i < 10; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    private void endRound() {
        if (player1.getHealth() > player2.getHealth()) {
            player1Wins++;
        } else {
            player2Wins++;
        }

        round++;

        if (round > maxRounds || player1Wins >= 2 || player2Wins >= 2) {
            gameState = GAME_OVER;
        } else {
            // Reset for next round
            player1.reset();
            player2.reset();
            countdown = 60;
            playCheerSound = true;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (gameState) {
            case MENU:
                if (key == KeyEvent.VK_ENTER) {
                    gameState = CHARACTER_SELECT;
                } else if (key == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
                break;

            case CHARACTER_SELECT:
                if (key == KeyEvent.VK_LEFT) {
                    selectedCharacter = (selectedCharacter - 1 + characterNames.length) % characterNames.length;
                } else if (key == KeyEvent.VK_RIGHT) {
                    selectedCharacter = (selectedCharacter + 1) % characterNames.length;
                } else if (key == KeyEvent.VK_ENTER) {
                    // Set player character
                    player1.setName(characterNames[selectedCharacter]);
                    player1.setColor(characterColors[selectedCharacter]);
                    gameState = FIGHT;
                }
                break;

            case FIGHT:
                // Player 1 controls
                if (key == KeyEvent.VK_LEFT) {
                    player1.moveLeft();
                } else if (key == KeyEvent.VK_RIGHT) {
                    player1.moveRight();
                } else if (key == KeyEvent.VK_UP) {
                    player1.jump();
                } else if (key == KeyEvent.VK_Z) {
                    player1.punch();
                    playPunchSound = true;
                } else if (key == KeyEvent.VK_X) {
                    player1.grapple();
                    playThrowSound = true;
                } else if (key == KeyEvent.VK_C) {
                    player1.block();
                }
                break;

            case GAME_OVER:
                if (key == KeyEvent.VK_ENTER) {
                    // Reset game
                    round = 1;
                    player1Wins = 0;
                    player2Wins = 0;
                    player1.reset();
                    player2.reset();
                    countdown = 60;
                    gameState = MENU;
                } else if (key == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (gameState == FIGHT) {
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                player1.stopMoving();
            } else if (key == KeyEvent.VK_C) {
                player1.stopBlocking();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    // Player class
    class Player {
        private String name;
        private int x, y;
        private int width = 40, height = 80;
        private Color color;
        private double health = 100;
        private boolean movingLeft = false;
        private boolean movingRight = false;
        private boolean jumping = false;
        private double velocityY = 0;
        private boolean punching = false;
        private int punchTimer = 0;
        private boolean grappling = false;
        private int grappleTimer = 0;
        private boolean blocking = false;
        private boolean stunned = false;
        private int stunTimer = 0;

        public Player(String name, int x, int y, Color color) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.color = color;
        }

        public void update() {
            // Movement
            if (movingLeft && x > ringX + 10) {
                x -= 5;
            }
            if (movingRight && x < ringX + ringWidth - width - 10) {
                x += 5;
            }

            // Jumping physics
            if (jumping) {
                velocityY += 0.5; // Gravity
                y += velocityY;

                // Land on ring
                if (y > ringY + ringHeight - height) {
                    y = ringY + ringHeight - height;
                    jumping = false;
                    velocityY = 0;
                }
            }

            // Punch timer
            if (punching) {
                punchTimer--;
                if (punchTimer <= 0) {
                    punching = false;
                }
            }

            // Grapple timer
            if (grappling) {
                grappleTimer--;
                if (grappleTimer <= 0) {
                    grappling = false;
                }
            }

            // Stun timer
            if (stunned) {
                stunTimer--;
                if (stunTimer <= 0) {
                    stunned = false;
                }
            }
        }

        public void draw(Graphics2D g2d) {
            // Save original transform
            AffineTransform original = g2d.getTransform();

            // Apply stun effect
            if (stunned) {
                g2d.rotate(Math.toRadians(random.nextInt(10) - 5), x + width/2, y + height/2);
            }

            // Body
            g2d.setColor(color);
            g2d.fillRect(x, y, width, height);

            // Head
            g2d.setColor(new Color(255, 220, 180));
            g2d.fillOval(x + width/4, y - 20, width/2, 30);

            // Punch effect
            if (punching) {
                g2d.setColor(Color.YELLOW);
                int punchX = (this == player1) ? x + width : x - 20;
                g2d.fillRect(punchX, y + 20, 20, 20);
            }

            // Grapple effect
            if (grappling) {
                g2d.setColor(Color.ORANGE);
                int grappleX = (this == player1) ? x + width : x - 30;
                g2d.fillRect(grappleX, y + 10, 30, 30);
            }

            // Block effect
            if (blocking) {
                g2d.setColor(new Color(255, 255, 255, 100));
                g2d.fillRect(x, y, width, height);
            }

            // Restore transform
            g2d.setTransform(original);
        }

        public void moveLeft() {
            movingLeft = true;
        }

        public void moveRight() {
            movingRight = true;
        }

        public void stopMoving() {
            movingLeft = false;
            movingRight = false;
        }

        public void jump() {
            if (!jumping) {
                jumping = true;
                velocityY = -12;
            }
        }

        public void punch() {
            if (!punching && !grappling && !stunned) {
                punching = true;
                punchTimer = 10;
            }
        }

        public void grapple() {
            if (!punching && !grappling && !stunned) {
                grappling = true;
                grappleTimer = 15;
            }
        }

        public void block() {
            if (!stunned) {
                blocking = true;
            }
        }

        public void stopBlocking() {
            blocking = false;
        }

        public void takeDamage(double damage) {
            if (!blocking) {
                health -= damage;
                if (health < 0) health = 0;
            }
        }

        public void setStunned(boolean stunned) {
            this.stunned = stunned;
            this.stunTimer = 30;
        }

        public void reset() {
            health = 100;
            x = (this == player1) ? 200 : 500;
            y = 300;
            punching = false;
            grappling = false;
            blocking = false;
            stunned = false;
        }

        // Getters
        public String getName() { return name; }
        public int getX() { return x; }
        public int getY() { return y; }
        public Color getColor() { return color; }
        public double getHealth() { return health; }
        public boolean isPunching() { return punching; }
        public boolean isGrappling() { return grappling; }
        public boolean isBlocking() { return blocking; }
        public boolean isStunned() { return stunned; }

        // Setters
        public void setName(String name) { this.name = name; }
        public void setColor(Color color) { this.color = color; }
    }

    // Particle effect class
    class Particle {
        private double x, y;
        private double velocityX, velocityY;
        private Color color;
        private int life;

        public Particle(int x, int y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.velocityX = random.nextDouble() * 4 - 2;
            this.velocityY = random.nextDouble() * 4 - 2;
            this.life = random.nextInt(20) + 10;
        }

        public void update() {
            x += velocityX;
            y += velocityY;
            velocityY += 0.1; // Gravity
            life--;
        }

        public void draw(Graphics2D g2d) {
            int alpha = (int)(255 * (life / 30.0));
            if (alpha < 0) alpha = 0;
            if (alpha > 255) alpha = 255;

            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g2d.fillRect((int)x, (int)y, 3, 3);
        }

        public boolean isDead() {
            return life <= 0;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Virtual Wrestling Game");
        VirtualWrestlingGame game = new VirtualWrestlingGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}