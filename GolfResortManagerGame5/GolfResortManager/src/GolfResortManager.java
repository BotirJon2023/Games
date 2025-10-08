import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class GolfResortManager extends JPanel implements ActionListener {
    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int DELAY = 30;

    // Game state
    private enum GameState { MAIN_MENU, PLAYING, UPGRADING, GAME_OVER }
    private GameState gameState = GameState.MAIN_MENU;

    // Resort properties
    private String resortName = "Pine Valley Resort";
    private int money = 10000;
    private int reputation = 50;
    private int day = 1;

    // Golf course properties
    private int courseCondition = 80;
    private int courseDifficulty = 3;
    private int courseBeauty = 60;
    private int courseCapacity = 50;

    // Facilities
    private boolean hasClubhouse = true;
    private boolean hasProShop = false;
    private boolean hasRestaurant = false;
    private boolean hasSpa = false;
    private boolean hasPool = false;

    // Staff
    private int groundskeepers = 2;
    private int instructors = 1;
    private int receptionists = 1;

    // Golfers
    private List<Golfer> golfers = new ArrayList<>();
    private int maxGolfers = 20;
    private int golfersToday = 0;

    // Animation elements
    private List<Animation> animations = new ArrayList<>();
    private Timer timer;

    // UI elements
    private JButton startButton, upgradeButton, nextDayButton, menuButton;
    private JLabel moneyLabel, reputationLabel, dayLabel;

    public GolfResortManager() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(100, 180, 100));
        setLayout(null);

        initializeUI();
        initializeGame();

        timer = new Timer(DELAY, this);
        timer.start();
    }

    private void initializeUI() {
        // Create UI components
        startButton = new JButton("Start Game");
        startButton.setBounds(350, 300, 100, 30);
        startButton.addActionListener(e -> startGame());

        upgradeButton = new JButton("Upgrade Facilities");
        upgradeButton.setBounds(650, 20, 140, 30);
        upgradeButton.addActionListener(e -> gameState = GameState.UPGRADING);

        nextDayButton = new JButton("Next Day");
        nextDayButton.setBounds(650, 60, 140, 30);
        nextDayButton.addActionListener(e -> nextDay());

        menuButton = new JButton("Main Menu");
        menuButton.setBounds(650, 100, 140, 30);
        menuButton.addActionListener(e -> gameState = GameState.MAIN_MENU);

        moneyLabel = new JLabel("Money: $" + money);
        moneyLabel.setBounds(20, 20, 150, 20);
        moneyLabel.setForeground(Color.WHITE);
        moneyLabel.setFont(new Font("Arial", Font.BOLD, 14));

        reputationLabel = new JLabel("Reputation: " + reputation + "/100");
        reputationLabel.setBounds(20, 50, 150, 20);
        reputationLabel.setForeground(Color.WHITE);
        reputationLabel.setFont(new Font("Arial", Font.BOLD, 14));

        dayLabel = new JLabel("Day: " + day);
        dayLabel.setBounds(20, 80, 150, 20);
        dayLabel.setForeground(Color.WHITE);
        dayLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // Add components to panel
        add(startButton);
        add(upgradeButton);
        add(nextDayButton);
        add(menuButton);
        add(moneyLabel);
        add(reputationLabel);
        add(dayLabel);

        // Initially hide game buttons
        upgradeButton.setVisible(false);
        nextDayButton.setVisible(false);
        menuButton.setVisible(false);
        moneyLabel.setVisible(false);
        reputationLabel.setVisible(false);
        dayLabel.setVisible(false);
    }

    private void initializeGame() {
        // Initialize golfers
        for (int i = 0; i < 5; i++) {
            golfers.add(new Golfer());
        }
    }

    private void startGame() {
        gameState = GameState.PLAYING;
        startButton.setVisible(false);
        upgradeButton.setVisible(true);
        nextDayButton.setVisible(true);
        menuButton.setVisible(true);
        moneyLabel.setVisible(true);
        reputationLabel.setVisible(true);
        dayLabel.setVisible(true);

        // Add welcome animation
        animations.add(new TextAnimation("Welcome to " + resortName + "!",
                WIDTH/2, HEIGHT/2, Color.YELLOW, 100));
    }

    private void nextDay() {
        day++;
        golfersToday = 0;

        // Calculate daily expenses
        int expenses = groundskeepers * 100 + instructors * 150 + receptionists * 120;
        money -= expenses;

        // Calculate income from golfers
        int income = 0;
        int maxPossibleGolfers = Math.min(maxGolfers, courseCapacity);
        int actualGolfers = (int)(maxPossibleGolfers * (reputation / 100.0));

        for (int i = 0; i < actualGolfers; i++) {
            int fee = 50 + (courseDifficulty * 10) + (courseBeauty / 10);
            income += fee;
            golfersToday++;

            // Add golfer animation
            animations.add(new GolferAnimation());
        }

        money += income;

        // Update reputation based on course condition and facilities
        int repChange = (courseCondition / 10) - 5;
        if (hasClubhouse) repChange += 2;
        if (hasProShop) repChange += 3;
        if (hasRestaurant) repChange += 4;
        if (hasSpa) repChange += 5;
        if (hasPool) repChange += 3;

        reputation = Math.max(0, Math.min(100, reputation + repChange));

        // Update course condition based on groundskeepers
        courseCondition = Math.max(0, Math.min(100, courseCondition - 5 + (groundskeepers * 3)));

        // Update UI
        moneyLabel.setText("Money: $" + money);
        reputationLabel.setText("Reputation: " + reputation + "/100");
        dayLabel.setText("Day: " + day);

        // Add day summary animation
        animations.add(new TextAnimation("Day " + day + " Complete!",
                WIDTH/2, 100, Color.WHITE, 60));
        animations.add(new TextAnimation("Income: $" + income + " | Expenses: $" + expenses,
                WIDTH/2, 130, Color.WHITE, 60));

        // Check for game over
        if (money < 0) {
            gameState = GameState.GAME_OVER;
            animations.add(new TextAnimation("GAME OVER - Bankrupt!",
                    WIDTH/2, HEIGHT/2, Color.RED, 120));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (gameState) {
            case MAIN_MENU:
                drawMainMenu(g2d);
                break;
            case PLAYING:
                drawGame(g2d);
                break;
            case UPGRADING:
                drawUpgradeMenu(g2d);
                break;
            case GAME_OVER:
                drawGameOver(g2d);
                break;
        }

        // Draw animations
        drawAnimations(g2d);
    }

    private void drawMainMenu(Graphics2D g2d) {
        // Draw background
        g2d.setColor(new Color(50, 120, 50));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw title
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String title = "Golf Resort Manager";
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, (WIDTH - titleWidth) / 2, 150);

        // Draw subtitle
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String subtitle = "Build the ultimate golf destination!";
        int subtitleWidth = g2d.getFontMetrics().stringWidth(subtitle);
        g2d.drawString(subtitle, (WIDTH - subtitleWidth) / 2, 200);

        // Draw golf course in background
        drawCoursePreview(g2d);
    }

    private void drawCoursePreview(Graphics2D g2d) {
        // Draw fairways
        g2d.setColor(new Color(80, 160, 80));
        g2d.fillRect(100, 250, 200, 100);
        g2d.fillRect(350, 350, 200, 80);
        g2d.fillRect(500, 250, 180, 120);

        // Draw greens
        g2d.setColor(new Color(100, 180, 100));
        g2d.fillOval(270, 270, 40, 40);
        g2d.fillOval(450, 380, 40, 40);
        g2d.fillOval(620, 290, 40, 40);

        // Draw water hazards
        g2d.setColor(new Color(100, 150, 255));
        g2d.fillOval(200, 320, 60, 40);
        g2d.fillOval(550, 320, 70, 50);

        // Draw sand traps
        g2d.setColor(new Color(240, 230, 140));
        g2d.fillOval(320, 250, 50, 30);
        g2d.fillOval(480, 300, 40, 25);
    }

    private void drawGame(Graphics2D g2d) {
        // Draw golf course
        drawGolfCourse(g2d);

        // Draw facilities
        drawFacilities(g2d);

        // Draw golfers
        drawGolfers(g2d);

        // Draw stats panel
        drawStatsPanel(g2d);
    }

    private void drawGolfCourse(Graphics2D g2d) {
        // Draw background
        g2d.setColor(new Color(60, 140, 60));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw fairways
        g2d.setColor(new Color(80, 160, 80));
        for (int i = 0; i < 5; i++) {
            int x = 100 + i * 120;
            int y = 150 + i * 20;
            g2d.fillRoundRect(x, y, 300, 80, 40, 40);
        }

        // Draw greens
        g2d.setColor(new Color(100, 180, 100));
        for (int i = 0; i < 5; i++) {
            int x = 380 + i * 120;
            int y = 150 + i * 20;
            g2d.fillOval(x, y, 40, 40);
        }

        // Draw water hazards
        g2d.setColor(new Color(100, 150, 255, 180));
        g2d.fillOval(200, 200, 80, 60);
        g2d.fillOval(500, 300, 100, 70);

        // Draw sand traps
        g2d.setColor(new Color(240, 230, 140));
        g2d.fillOval(300, 250, 60, 40);
        g2d.fillOval(600, 200, 50, 35);
    }

    private void drawFacilities(Graphics2D g2d) {
        // Draw clubhouse
        g2d.setColor(new Color(200, 150, 100));
        g2d.fillRect(50, 400, 100, 80);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Clubhouse", 55, 420);

        // Draw pro shop if available
        if (hasProShop) {
            g2d.setColor(new Color(150, 200, 100));
            g2d.fillRect(170, 400, 80, 60);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Pro Shop", 175, 420);
        }

        // Draw restaurant if available
        if (hasRestaurant) {
            g2d.setColor(new Color(200, 100, 100));
            g2d.fillRect(270, 400, 90, 70);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Restaurant", 275, 420);
        }

        // Draw spa if available
        if (hasSpa) {
            g2d.setColor(new Color(100, 200, 200));
            g2d.fillRect(380, 400, 70, 60);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Spa", 385, 420);
        }

        // Draw pool if available
        if (hasPool) {
            g2d.setColor(new Color(100, 150, 255));
            g2d.fillRect(470, 400, 100, 50);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Pool", 475, 420);
        }
    }

    private void drawGolfers(Graphics2D g2d) {
        for (int i = 0; i < golfersToday; i++) {
            Golfer golfer = golfers.get(i % golfers.size());
            golfer.draw(g2d, 200 + (i * 30) % 500, 200 + (i * 20) % 300);
        }
    }

    private void drawStatsPanel(Graphics2D g2d) {
        // Draw semi-transparent background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(600, 150, 180, 200);

        // Draw stats
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Course Condition: " + courseCondition + "%", 610, 170);
        g2d.drawString("Course Difficulty: " + courseDifficulty + "/5", 610, 190);
        g2d.drawString("Course Beauty: " + courseBeauty + "%", 610, 210);
        g2d.drawString("Golfers Today: " + golfersToday, 610, 230);
        g2d.drawString("Groundskeepers: " + groundskeepers, 610, 250);
        g2d.drawString("Instructors: " + instructors, 610, 270);
        g2d.drawString("Receptionists: " + receptionists, 610, 290);
    }

    private void drawUpgradeMenu(Graphics2D g2d) {
        // Draw background
        g2d.setColor(new Color(50, 100, 50));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw title
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        String title = "Upgrade Facilities";
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, (WIDTH - titleWidth) / 2, 50);

        // Draw upgrade options
        drawUpgradeOption(g2d, "Pro Shop", 100, 100, 5000, !hasProShop);
        drawUpgradeOption(g2d, "Restaurant", 100, 150, 8000, !hasRestaurant);
        drawUpgradeOption(g2d, "Spa", 100, 200, 12000, !hasSpa);
        drawUpgradeOption(g2d, "Pool", 100, 250, 7000, !hasPool);
        drawUpgradeOption(g2d, "Hire Groundskeeper", 100, 300, 1000, true);
        drawUpgradeOption(g2d, "Hire Instructor", 100, 350, 1500, true);
        drawUpgradeOption(g2d, "Hire Receptionist", 100, 400, 1200, true);
        drawUpgradeOption(g2d, "Improve Course", 100, 450, 3000, true);

        // Draw back button
        g2d.setColor(Color.CYAN);
        g2d.fillRect(350, 500, 100, 40);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Back to Game", 360, 525);
    }

    private void drawUpgradeOption(Graphics2D g2d, String name, int x, int y, int cost, boolean available) {
        Color bgColor = available ? (money >= cost ? Color.GREEN : Color.RED) : Color.GRAY;
        g2d.setColor(bgColor);
        g2d.fillRect(x, y, 400, 40);

        g2d.setColor(Color.BLACK);
        g2d.drawString(name + " - $" + cost, x + 10, y + 25);

        if (!available) {
            g2d.drawString("(Already purchased)", x + 200, y + 25);
        }
    }

    private void drawGameOver(Graphics2D g2d) {
        // Draw background
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw game over text
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String gameOver = "GAME OVER";
        int gameOverWidth = g2d.getFontMetrics().stringWidth(gameOver);
        g2d.drawString(gameOver, (WIDTH - gameOverWidth) / 2, 200);

        // Draw summary
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("You managed the resort for " + day + " days", 200, 280);
        g2d.drawString("Final Reputation: " + reputation, 200, 320);
        g2d.drawString("Final Money: $" + money, 200, 360);

        // Draw restart option
        g2d.setColor(Color.CYAN);
        g2d.fillRect(350, 450, 100, 40);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Restart", 370, 475);
    }

    private void drawAnimations(Graphics2D g2d) {
        Iterator<Animation> iterator = animations.iterator();
        while (iterator.hasNext()) {
            Animation animation = iterator.next();
            if (animation.isActive()) {
                animation.draw(g2d);
            } else {
                iterator.remove();
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Update animations
        for (Animation animation : animations) {
            animation.update();
        }

        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (gameState == GameState.UPGRADING) {
            int x = e.getX();
            int y = e.getY();

            // Check if back button was clicked
            if (x >= 350 && x <= 450 && y >= 500 && y <= 540) {
                gameState = GameState.PLAYING;
                return;
            }

            // Check upgrade options
            checkUpgradeClick(x, y, 100, 100, "Pro Shop", 5000, !hasProShop);
            checkUpgradeClick(x, y, 100, 150, "Restaurant", 8000, !hasRestaurant);
            checkUpgradeClick(x, y, 100, 200, "Spa", 12000, !hasSpa);
            checkUpgradeClick(x, y, 100, 250, "Pool", 7000, !hasPool);

            if (x >= 100 && x <= 500 && y >= 300 && y <= 340) {
                // Hire Groundskeeper
                if (money >= 1000) {
                    money -= 1000;
                    groundskeepers++;
                    animations.add(new TextAnimation("Hired a Groundskeeper!",
                            WIDTH/2, HEIGHT/2, Color.GREEN, 60));
                }
            } else if (x >= 100 && x <= 500 && y >= 350 && y <= 390) {
                // Hire Instructor
                if (money >= 1500) {
                    money -= 1500;
                    instructors++;
                    animations.add(new TextAnimation("Hired an Instructor!",
                            WIDTH/2, HEIGHT/2, Color.GREEN, 60));
                }
            } else if (x >= 100 && x <= 500 && y >= 400 && y <= 440) {
                // Hire Receptionist
                if (money >= 1200) {
                    money -= 1200;
                    receptionists++;
                    animations.add(new TextAnimation("Hired a Receptionist!",
                            WIDTH/2, HEIGHT/2, Color.GREEN, 60));
                }
            } else if (x >= 100 && x <= 500 && y >= 450 && y <= 490) {
                // Improve Course
                if (money >= 3000) {
                    money -= 3000;
                    courseBeauty = Math.min(100, courseBeauty + 10);
                    courseCondition = Math.min(100, courseCondition + 15);
                    animations.add(new TextAnimation("Course Improved!",
                            WIDTH/2, HEIGHT/2, Color.GREEN, 60));
                }
            }

            // Update UI
            moneyLabel.setText("Money: $" + money);
        } else if (gameState == GameState.GAME_OVER) {
            // Check if restart button was clicked
            int x = e.getX();
            int y = e.getY();
            if (x >= 350 && x <= 450 && y >= 450 && y <= 490) {
                restartGame();
            }
        }
    }

    private void checkUpgradeClick(int x, int y, int rectX, int rectY, String facility, int cost, boolean available) {
        if (x >= rectX && x <= rectX + 400 && y >= rectY && y <= rectY + 40 && available) {
            if (money >= cost) {
                money -= cost;
                switch (facility) {
                    case "Pro Shop": hasProShop = true; break;
                    case "Restaurant": hasRestaurant = true; break;
                    case "Spa": hasSpa = true; break;
                    case "Pool": hasPool = true; break;
                }
                animations.add(new TextAnimation(facility + " Purchased!",
                        WIDTH/2, HEIGHT/2, Color.GREEN, 60));
            }
        }
    }

    private void restartGame() {
        // Reset game state
        money = 10000;
        reputation = 50;
        day = 1;
        courseCondition = 80;
        courseDifficulty = 3;
        courseBeauty = 60;
        courseCapacity = 50;
        hasClubhouse = true;
        hasProShop = false;
        hasRestaurant = false;
        hasSpa = false;
        hasPool = false;
        groundskeepers = 2;
        instructors = 1;
        receptionists = 1;
        golfersToday = 0;
        animations.clear();

        // Update UI
        moneyLabel.setText("Money: $" + money);
        reputationLabel.setText("Reputation: " + reputation + "/100");
        dayLabel.setText("Day: " + day);

        // Return to main menu
        gameState = GameState.MAIN_MENU;
        startButton.setVisible(true);
        upgradeButton.setVisible(false);
        nextDayButton.setVisible(false);
        menuButton.setVisible(false);
        moneyLabel.setVisible(false);
        reputationLabel.setVisible(false);
        dayLabel.setVisible(false);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Golf Resort Manager");
        GolfResortManager game = new GolfResortManager();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        // Add mouse listener
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                game.mousePressed(e);
            }
        });
    }
}

// Golfer class representing golfers at the resort
class Golfer {
    private String name;
    private int skill;
    private int satisfaction;

    public Golfer() {
        String[] names = {"John", "Mike", "Sarah", "Emma", "David", "Lisa", "Tom", "Anna"};
        this.name = names[(int)(Math.random() * names.length)];
        this.skill = (int)(Math.random() * 50) + 30;
        this.satisfaction = (int)(Math.random() * 40) + 60;
    }

    public void draw(Graphics2D g2d, int x, int y) {
        // Draw golfer as a simple figure
        g2d.setColor(Color.BLUE);
        g2d.fillOval(x, y, 20, 20); // Head

        g2d.setColor(Color.RED);
        g2d.fillRect(x-5, y+20, 30, 40); // Body

        g2d.setColor(Color.BLACK);
        g2d.drawLine(x+10, y+60, x, y+80); // Left leg
        g2d.drawLine(x+10, y+60, x+20, y+80); // Right leg

        // Golf club
        g2d.drawLine(x+20, y+30, x+40, y+10);
    }
}

// Base class for animations
abstract class Animation {
    protected int x, y;
    protected int lifetime;
    protected int maxLifetime;

    public Animation(int x, int y, int lifetime) {
        this.x = x;
        this.y = y;
        this.lifetime = lifetime;
        this.maxLifetime = lifetime;
    }

    public boolean isActive() {
        return lifetime > 0;
    }

    public void update() {
        lifetime--;
    }

    public abstract void draw(Graphics2D g2d);
}

// Text animation for displaying messages
class TextAnimation extends Animation {
    private String text;
    private Color color;

    public TextAnimation(String text, int x, int y, Color color, int lifetime) {
        super(x, y, lifetime);
        this.text = text;
        this.color = color;
    }

    @Override
    public void draw(Graphics2D g2d) {
        float alpha = (float) lifetime / maxLifetime;
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        g2d.setColor(color);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, x - textWidth/2, y);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
}

// Animation for golfers moving around
class GolferAnimation extends Animation {
    private int dx, dy;

    public GolferAnimation() {
        super((int)(Math.random() * 600), (int)(Math.random() * 400), 120);
        this.dx = (int)(Math.random() * 5) - 2;
        this.dy = (int)(Math.random() * 5) - 2;
    }

    @Override
    public void update() {
        super.update();
        x += dx;
        y += dy;

        // Bounce off edges
        if (x < 0 || x > 800) dx = -dx;
        if (y < 0 || y > 600) dy = -dy;
    }

    @Override
    public void draw(Graphics2D g2d) {
        // Draw a simplified golfer
        g2d.setColor(Color.BLUE);
        g2d.fillOval(x, y, 15, 15);

        g2d.setColor(Color.RED);
        g2d.fillRect(x-3, y+15, 20, 25);

        // Golf club
        g2d.setColor(Color.BLACK);
        g2d.drawLine(x+15, y+20, x+25, y+5);
    }
}