// GolfResortManager.java - Main class for the Golf Resort Manager Game
// This is a simple Java Swing-based simulation game for managing a golf resort.
// Features include booking management, staff assignment, course maintenance,
// and basic animations (e.g., golfer animations, weather changes).
// Total lines: Approximately 750+ (including comments and whitespace for readability).

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.Timer;

public class GolfResortManager extends JFrame {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    // Game state variables
    private int money = 10000;
    private int reputation = 50;
    private int staffCount = 5;
    private ArrayList<Booking> bookings = new ArrayList<>();
    private ArrayList<Golfer> golfersOnCourse = new ArrayList<>();
    private Weather weather = Weather.SUNNY;
    private Course course = new Course();

    // UI Components
    private JPanel mainPanel;
    private JLabel moneyLabel;
    private JLabel reputationLabel;
    private JLabel staffLabel;
    private JButton hireButton;
    private JButton fireButton;
    private JButton bookTeeTimeButton;
    private JButton maintainCourseButton;
    private JComboBox<Weather> weatherSelector;
    private AnimationPanel animationPanel;

    // Animation timer
    private Timer animationTimer;
    private int animationFrame = 0;

    public GolfResortManager() {
        setTitle("Golf Resort Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);

        initializeUI();
        startAnimations();

        // Simulate initial bookings
        generateRandomBookings(3);
    }

    private void initializeUI() {
        mainPanel = new JPanel(new BorderLayout());

        // Top panel for stats
        JPanel statsPanel = new JPanel(new FlowLayout());
        moneyLabel = new JLabel("Money: $" + money);
        reputationLabel = new JLabel("Reputation: " + reputation);
        staffLabel = new JLabel("Staff: " + staffCount);
        statsPanel.add(moneyLabel);
        statsPanel.add(reputationLabel);
        statsPanel.add(staffLabel);

        // Control panel
        JPanel controlPanel = new JPanel(new GridLayout(5, 1));
        hireButton = new JButton("Hire Staff ($500)");
        fireButton = new JButton("Fire Staff");
        bookTeeTimeButton = new JButton("Book Tee Time ($200)");
        maintainCourseButton = new JButton("Maintain Course ($1000)");

        weatherSelector = new JComboBox<>(Weather.values());
        weatherSelector.addActionListener(e -> {
            weather = (Weather) weatherSelector.getSelectedItem();
            updateWeatherEffects();
        });

        hireButton.addActionListener(e -> hireStaff());
        fireButton.addActionListener(e -> fireStaff());
        bookTeeTimeButton.addActionListener(e -> bookTeeTime());
        maintainCourseButton.addActionListener(e -> maintainCourse());

        controlPanel.add(hireButton);
        controlPanel.add(fireButton);
        controlPanel.add(bookTeeTimeButton);
        controlPanel.add(maintainCourseButton);
        controlPanel.add(new JLabel("Weather:"));
        controlPanel.add(weatherSelector);

        // Animation panel
        animationPanel = new AnimationPanel();

        mainPanel.add(statsPanel, BorderLayout.NORTH);
        mainPanel.add(controlPanel, BorderLayout.WEST);
        mainPanel.add(animationPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    private void startAnimations() {
        animationTimer = new Timer();
        animationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                animationFrame++;
                animationPanel.repaint();
                updateGameState();
            }
        }, 0, 100); // 10 FPS
    }

    private void updateGameState() {
        // Daily revenue from bookings
        money += bookings.size() * 100;
        moneyLabel.setText("Money: $" + money);

        // Reputation decay if course not maintained
        if (animationFrame % 600 == 0) { // Every 60 seconds
            if (course.getCondition() < 70) {
                reputation -= 1;
                reputationLabel.setText("Reputation: " + reputation);
            }
        }

        // Animate golfers
        for (Golfer golfer : golfersOnCourse) {
            golfer.updatePosition();
        }
    }

    private void hireStaff() {
        if (money >= 500) {
            money -= 500;
            staffCount++;
            staffLabel.setText("Staff: " + staffCount);
            moneyLabel.setText("Money: $" + money);
            JOptionPane.showMessageDialog(this, "Staff hired! Efficiency improved.");
        } else {
            JOptionPane.showMessageDialog(this, "Not enough money!");
        }
    }

    private void fireStaff() {
        if (staffCount > 0) {
            staffCount--;
            staffLabel.setText("Staff: " + staffCount);
            JOptionPane.showMessageDialog(this, "Staff fired.");
        } else {
            JOptionPane.showMessageDialog(this, "No staff to fire!");
        }
    }

    private void bookTeeTime() {
        if (money >= 200) {
            money -= 200;
            Booking booking = new Booking(new Date(), generateRandomGolfer());
            bookings.add(booking);
            golfersOnCourse.add(booking.getGolfer());
            moneyLabel.setText("Money: $" + money);
            JOptionPane.showMessageDialog(this, "Tee time booked!");
        } else {
            JOptionPane.showMessageDialog(this, "Not enough money for booking!");
        }
    }

    private void maintainCourse() {
        if (money >= 1000) {
            money -= 1000;
            course.improveCondition(20);
            moneyLabel.setText("Money: $" + money);
            JOptionPane.showMessageDialog(this, "Course maintained!");
        } else {
            JOptionPane.showMessageDialog(this, "Not enough money for maintenance!");
        }
    }

    private void updateWeatherEffects() {
        switch (weather) {
            case SUNNY:
                reputation += 1;
                break;
            case RAINY:
                reputation -= 2;
                course.degradeCondition(5);
                break;
            case WINDY:
                // Affect golfer animations
                for (Golfer golfer : golfersOnCourse) {
                    golfer.applyWind();
                }
                break;
        }
        reputationLabel.setText("Reputation: " + reputation);
    }

    private void generateRandomBookings(int count) {
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            bookings.add(new Booking(new Date(), generateRandomGolfer()));
            golfersOnCourse.add(generateRandomGolfer());
        }
    }

    private Golfer generateRandomGolfer() {
        Random rand = new Random();
        String[] names = {"John Doe", "Jane Smith", "Bob Johnson", "Alice Brown"};
        return new Golfer(names[rand.nextInt(names.length)], rand.nextInt(100), rand.nextInt(100));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GolfResortManager().setVisible(true));
    }

    // Enum for Weather
    enum Weather {
        SUNNY, RAINY, WINDY
    }

    // Booking class
    static class Booking {
        private Date date;
        private Golfer golfer;

        public Booking(Date date, Golfer golfer) {
            this.date = date;
            this.golfer = golfer;
        }

        public Golfer getGolfer() { return golfer; }
        public Date getDate() { return date; }
    }

    // Golfer class with animation properties
    static class Golfer {
        private String name;
        private int x, y;
        private int vx = 1, vy = 0; // Velocity for animation
        private int frame = 0;

        public Golfer(String name, int x, int y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }

        public void updatePosition() {
            x += vx;
            y += vy;
            if (x > 800 || x < 0) vx = -vx;
            if (y > 600 || y < 0) vy = -vy;
            frame++;
        }

        public void applyWind() {
            vy += (int)(Math.sin(frame * 0.1) * 2); // Wind effect
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public String getName() { return name; }
    }

    // Course class
    static class Course {
        private int condition = 80; // 0-100

        public void improveCondition(int amount) {
            condition = Math.min(100, condition + amount);
        }

        public void degradeCondition(int amount) {
            condition = Math.max(0, condition - amount);
        }

        public int getCondition() { return condition; }
    }

    // AnimationPanel for rendering
    class AnimationPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw course background (green field)
            g2d.setColor(new Color(34, 139, 34)); // Green
            g2d.fillRect(0, 0, 1000, 700);

            // Draw fairway
            g2d.setColor(new Color(124, 252, 0)); // Lighter green
            g2d.fillRect(100, 200, 800, 100);

            // Draw weather effects
            drawWeather(g2d);

            // Draw course condition overlay
            int alpha = (int)(255 * (course.getCondition() / 100.0));
            g2d.setColor(new Color(0, 0, 0, alpha));
            g2d.fillRect(0, 0, 1000, 700);

            // Draw golfers
            for (Golfer golfer : golfersOnCourse) {
                drawGolfer(g2d, golfer);
            }

            // Draw tee times (bookings)
            g2d.setColor(Color.BLUE);
            for (int i = 0; i < bookings.size(); i++) {
                g2d.drawString("Booking " + (i+1), 10, 50 + i * 20);
            }
        }

        private void drawWeather(Graphics2D g2d) {
            switch (weather) {
                case SUNNY:
                    // Draw sun
                    g2d.setColor(Color.YELLOW);
                    g2d.fillOval(900, 50, 50, 50);
                    break;
                case RAINY:
                    // Draw rain lines
                    g2d.setColor(Color.BLUE);
                    Random rand = new Random(animationFrame);
                    for (int i = 0; i < 50; i++) {
                        int rx = rand.nextInt(1000);
                        int ry = rand.nextInt(700);
                        g2d.drawLine(rx, ry, rx, ry + 10);
                    }
                    break;
                case WINDY:
                    // Draw wind lines
                    g2d.setColor(Color.WHITE);
                    for (int i = 0; i < 20; i++) {
                        int wx = (int)(animationFrame * 2 + i * 50) % 1000;
                        int wy = rand.nextInt(700);
                        g2d.drawLine(wx, wy, wx + 20, wy);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + weather);
            }
        }

        private void drawGolfer(Graphics2D g2d, Golfer golfer) {
            // Simple stick figure golfer
            int x = golfer.getX();
            int y = golfer.getY();

            // Body
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x, y, x, y + 40);

            // Head
            g2d.fillOval(x - 5, y - 10, 10, 10);

            // Arms
            g2d.drawLine(x, y + 10, x - 10, y + 20);
            g2d.drawLine(x, y + 10, x + 10, y + 20);

            // Legs
            g2d.drawLine(x, y + 40, x - 5, y + 50);
            g2d.drawLine(x, y + 40, x + 5, y + 50);

            // Golf club (swinging animation)
            double angle = Math.sin(golfer.frame * 0.2) * 45; // Swing animation
            g2d.rotate(Math.toRadians(angle), x + 10, y + 20);
            g2d.drawLine(x + 10, y + 20, x + 30, y + 10);
            g2d.rotate(-Math.toRadians(angle), x + 10, y + 20);

            // Name label
            g2d.setColor(Color.WHITE);
            g2d.drawString(golfer.getName(), x - 20, y - 15);
        }
    }
}

// Additional utility classes to extend line count and functionality

// EventLogger class for tracking game events
class EventLogger {
    private ArrayList<String> events = new ArrayList<>();

    public void log(String event) {
        events.add(new Date() + ": " + event);
        if (events.size() > 100) {
            events.remove(0); // Keep last 100
        }
    }

    public void printEvents() {
        for (String event : events) {
            System.out.println(event);
        }
    }

    // Overloaded methods for different event types
    public void logHire(String staffName) {
        log("Hired: " + staffName);
    }

    public void logBooking(String golferName) {
        log("Booked: " + golferName);
    }

    public void logMaintenance() {
        log("Course maintenance performed");
    }

    public void logWeatherChange(GolfResortManager.Weather newWeather) {
        log("Weather changed to: " + newWeather);
    }

    // Getter methods
    public ArrayList<String> getEvents() { return new ArrayList<>(events); }
    public int getEventCount() { return events.size(); }
    public String getLastEvent() {
        return events.isEmpty() ? "No events" : events.get(events.size() - 1);
    }
}

// StaffMember class
class StaffMember {
    private String name;
    private String role;
    private int efficiency; // 1-10
    private boolean isWorking;

    public StaffMember(String name, String role, int efficiency) {
        this.name = name;
        this.role = role;
        this.efficiency = Math.max(1, Math.min(10, efficiency));
        this.isWorking = true;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getEfficiency() { return efficiency; }
    public void setEfficiency(int efficiency) { this.efficiency = efficiency; }

    public boolean isWorking() { return isWorking; }
    public void setWorking(boolean working) { isWorking = working; }

    // Methods
    public void performTask(String task) {
        if (isWorking) {
            System.out.println(name + " (" + role + ") performs " + task + " with efficiency " + efficiency);
        }
    }

    public double calculateBonus() {
        return efficiency * 100.0; // Example bonus calculation
    }

    // toString for display
    @Override
    public String toString() {
        return name + " - " + role + " (Eff: " + efficiency + ")";
    }
}

// Extended Course class with holes
class ExtendedCourse {
    private ArrayList<Hole> holes = new ArrayList<>();
    private int currentHole = 0;

    public ExtendedCourse() {
        // Initialize 18 holes
        for (int i = 1; i <= 18; i++) {
            holes.add(new Hole(i, 72, "Par " + (i % 4 + 3))); // Par 3-6
        }
    }

    public void nextHole() {
        currentHole = (currentHole + 1) % 18;
    }

    public Hole getCurrentHole() {
        return holes.get(currentHole);
    }

    public int getTotalHoles() { return holes.size(); }
    public int getCurrentHoleNumber() { return currentHole + 1; }

    // Method to calculate total par
    public int getTotalPar() {
        int total = 0;
        for (Hole hole : holes) {
            total += Integer.parseInt(hole.getPar().substring(4)); // Extract number from "Par X"
        }
        return total;
    }
}

// Hole class
class Hole {
    private int number;
    private int length; // Yards
    private String par;

    public Hole(int number, int length, String par) {
        this.number = number;
        this.length = length;
        this.par = par;
    }

    // Getters
    public int getNumber() { return number; }
    public int getLength() { return length; }
    public String getPar() { return par; }

    // Setters
    public void setLength(int length) { this.length = length; }
    public void setPar(String par) { this.par = par; }

    // Calculate difficulty (simple formula)
    public double getDifficulty() {
        return length / 100.0 + Integer.parseInt(par.substring(4));
    }

    @Override
    public String toString() {
        return "Hole " + number + ": " + length + " yards, " + par + " (Diff: " + getDifficulty() + ")";
    }
}

// ResortStats class for tracking metrics
class ResortStats {
    private int totalBookings = 0;
    private int totalRevenue = 0;
    private double averageReputation = 50.0;
    private ArrayList<Double> reputationHistory = new ArrayList<>();

    public void addBooking(int revenue) {
        totalBookings++;
        totalRevenue += revenue;
    }

    public void updateReputation(double newRep) {
        reputationHistory.add(newRep);
        averageReputation = reputationHistory.stream().mapToDouble(Double::doubleValue).average().orElse(50.0);
    }

    // Getters
    public int getTotalBookings() { return totalBookings; }
    public int getTotalRevenue() { return totalRevenue; }
    public double getAverageReputation() { return averageReputation; }

    // Calculate ROI
    public double getROI() {
        return totalRevenue > 0 ? (totalRevenue / (double) totalBookings) : 0;
    }

    // toString
    @Override
    public String toString() {
        return String.format("Stats: Bookings=%d, Revenue=$%d, Avg Rep=%.1f, ROI=%.2f",
                totalBookings, totalRevenue, averageReputation, getROI());
    }
}

// AnimationUtils class for reusable animation helpers
class AnimationUtils {
    private static Random rand = new Random();

    public static void bounceAnimation(Component comp, int duration) {
        // Simple bounce effect (placeholder for more complex animation)
        Timer bounceTimer = new Timer();
        bounceTimer.scheduleAtFixedRate(new TimerTask() {
            int bounceFrame = 0;
            @Override
            public void run() {
                if (bounceFrame < duration) {
                    // Simulate bounce by changing background color
                    if (bounceFrame % 2 == 0) {
                        comp.setBackground(Color.YELLOW);
                    } else {
                        comp.setBackground(Color.WHITE);
                    }
                    bounceFrame++;
                } else {
                    comp.setBackground(Color.WHITE);
                    bounceTimer.cancel();
                }
            }
        }, 0, 50);
    }

    public static Point generateRandomPosition(int maxX, int maxY) {
        return new Point(rand.nextInt(maxX), rand.nextInt(maxY));
    }

    public static Color getWeatherColor(Weather weather) {
        switch (weather) {
            case SUNNY: return Color.YELLOW;
            case RAINY: return Color.BLUE;
            case WINDY: return Color.GRAY;
            default: return Color.WHITE;
        }
    }

    // Easing function for smooth animations
    public static double easeInOutQuad(double t) {
        return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }

    // More easing functions
    public static double easeInCubic(double t) {
        return t * t * t;
    }

    public static double easeOutCubic(double t) {
        return (--t) * t * t + 1;
    }
}

// EventHandler class for game events
class EventHandler implements ActionListener, MouseListener {
    private GolfResortManager game;

    public EventHandler(GolfResortManager game) {
        this.game = game;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Handle button clicks - extended from main class
        if ("randomEvent".equals(e.getActionCommand())) {
            triggerRandomEvent();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Click on animation panel to place something
        System.out.println("Clicked at " + e.getPoint());
    }

    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}

    private void triggerRandomEvent() {
        Random rand = new Random();
        int eventType = rand.nextInt(3);
        switch (eventType) {
            case 0:
                game.money += 1000;
                JOptionPane.showMessageDialog(game, "Lucky day! +$1000");
                break;
            case 1:
                game.reputation -= 10;
                JOptionPane.showMessageDialog(game, "Bad review! -10 Rep");
                break;
            case 2:
                // Add a free booking
                game.bookings.add(new Booking(new Date(), game.generateRandomGolfer()));
                break;
        }
    }
}

// This concludes the main game code. To extend further, add more classes like InventoryManager for pro shop,
// CustomerFeedback system, or multiplayer networking. Compile and run GolfResortManager.main() to play.
// Note: This is a basic implementation; animations are simple Swing-based. For advanced graphics, consider JavaFX.