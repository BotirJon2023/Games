import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class TrackAndFieldGame extends JFrame {
    private GamePanel gamePanel;
    private ControlPanel controlPanel;
    private GameState gameState;

    public TrackAndFieldGame() {
        setTitle("Track and Field Sports Championship");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gameState = new GameState();
        gamePanel = new GamePanel(gameState);
        controlPanel = new ControlPanel(gameState, gamePanel);

        setLayout(new BorderLayout());
        add(gamePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TrackAndFieldGame());
    }
}

class GameState {
    public enum Event {
        MENU, SPRINT_100M, LONG_JUMP, JAVELIN_THROW, HIGH_JUMP, RESULTS
    }

    private Event currentEvent;
    private ArrayList<EventResult> results;
    private String playerName;
    private int totalScore;

    public GameState() {
        currentEvent = Event.MENU;
        results = new ArrayList<>();
        playerName = "Player 1";
        totalScore = 0;
    }

    public Event getCurrentEvent() {
        return currentEvent;
    }

    public void setCurrentEvent(Event event) {
        this.currentEvent = event;
    }

    public void addResult(EventResult result) {
        results.add(result);
        totalScore += result.getScore();
    }

    public ArrayList<EventResult> getResults() {
        return results;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public String getPlayerName() {
        return playerName;
    }
}

class EventResult {
    private String eventName;
    private double performance;
    private int score;

    public EventResult(String eventName, double performance, int score) {
        this.eventName = eventName;
        this.performance = performance;
        this.score = score;
    }

    public String getEventName() {
        return eventName;
    }

    public double getPerformance() {
        return performance;
    }

    public int getScore() {
        return score;
    }
}

class GamePanel extends JPanel {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;

    private GameState gameState;
    private Timer animationTimer;

    private SprintAnimation sprintAnimation;
    private LongJumpAnimation longJumpAnimation;
    private JavelinAnimation javelinAnimation;
    private HighJumpAnimation highJumpAnimation;

    public GamePanel(GameState gameState) {
        this.gameState = gameState;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235));

        sprintAnimation = new SprintAnimation();
        longJumpAnimation = new LongJumpAnimation();
        javelinAnimation = new JavelinAnimation();
        highJumpAnimation = new HighJumpAnimation();

        animationTimer = new Timer(16, e -> {
            updateAnimations();
            repaint();
        });
        animationTimer.start();
    }

    private void updateAnimations() {
        switch (gameState.getCurrentEvent()) {
            case SPRINT_100M:
                sprintAnimation.update();
                break;
            case LONG_JUMP:
                longJumpAnimation.update();
                break;
            case JAVELIN_THROW:
                javelinAnimation.update();
                break;
            case HIGH_JUMP:
                highJumpAnimation.update();
                break;
        }
    }

    public void startSprint() {
        sprintAnimation.start();
    }

    public void startLongJump(double power) {
        longJumpAnimation.start(power);
    }

    public void startJavelin(double angle, double power) {
        javelinAnimation.start(angle, power);
    }

    public void startHighJump(double power) {
        highJumpAnimation.start(power);
    }

    public boolean isAnimationComplete() {
        switch (gameState.getCurrentEvent()) {
            case SPRINT_100M:
                return sprintAnimation.isComplete();
            case LONG_JUMP:
                return longJumpAnimation.isComplete();
            case JAVELIN_THROW:
                return javelinAnimation.isComplete();
            case HIGH_JUMP:
                return highJumpAnimation.isComplete();
            default:
                return true;
        }
    }

    public double getPerformance() {
        switch (gameState.getCurrentEvent()) {
            case SPRINT_100M:
                return sprintAnimation.getTime();
            case LONG_JUMP:
                return longJumpAnimation.getDistance();
            case JAVELIN_THROW:
                return javelinAnimation.getDistance();
            case HIGH_JUMP:
                return highJumpAnimation.getHeight();
            default:
                return 0;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (gameState.getCurrentEvent()) {
            case MENU:
                drawMenu(g2d);
                break;
            case SPRINT_100M:
                drawTrack(g2d);
                sprintAnimation.draw(g2d);
                break;
            case LONG_JUMP:
                drawLongJumpPit(g2d);
                longJumpAnimation.draw(g2d);
                break;
            case JAVELIN_THROW:
                drawJavelinField(g2d);
                javelinAnimation.draw(g2d);
                break;
            case HIGH_JUMP:
                drawHighJumpArea(g2d);
                highJumpAnimation.draw(g2d);
                break;
            case RESULTS:
                drawResults(g2d);
                break;
        }
    }

    private void drawMenu(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.setColor(Color.WHITE);
        String title = "Track & Field Championship";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(title)) / 2;
        g2d.drawString(title, x, 150);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Select an event from the controls below", 250, 300);
    }

    private void drawTrack(Graphics2D g2d) {
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(0, 400, getWidth(), 200);

        g2d.setColor(new Color(255, 255, 255));
        for (int i = 0; i < 10; i++) {
            g2d.fillRect(i * 100, 490, 50, 10);
        }

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("100m Sprint", 20, 30);
        g2d.drawString("Time: " + String.format("%.2f", sprintAnimation.getTime()) + "s", 20, 60);
    }

    private void drawLongJumpPit(Graphics2D g2d) {
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(0, 400, 400, 200);

        g2d.setColor(new Color(238, 214, 175));
        g2d.fillRect(400, 400, 600, 200);

        g2d.setColor(Color.RED);
        g2d.fillRect(400, 400, 5, 200);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Long Jump", 20, 30);
        g2d.drawString("Distance: " + String.format("%.2f", longJumpAnimation.getDistance()) + "m", 20, 60);
    }

    private void drawJavelinField(Graphics2D g2d) {
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, 300, getWidth(), 300);

        for (int i = 1; i <= 10; i++) {
            g2d.setColor(Color.WHITE);
            int x = i * 80;
            g2d.drawLine(x, 300, x, 600);
            g2d.drawString(i * 10 + "m", x - 15, 590);
        }

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Javelin Throw", 20, 30);
        g2d.drawString("Distance: " + String.format("%.2f", javelinAnimation.getDistance()) + "m", 20, 60);
    }

    private void drawHighJumpArea(Graphics2D g2d) {
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, 400, getWidth(), 200);

        g2d.setColor(new Color(255, 100, 100));
        g2d.fillRect(300, 500, 400, 100);

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(250, 350, 250, 500);
        g2d.drawLine(750, 350, 750, 500);

        int barHeight = (int)(500 - highJumpAnimation.getBarHeight() * 100);
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(5));
        g2d.drawLine(250, barHeight, 750, barHeight);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("High Jump", 20, 30);
        g2d.drawString("Height: " + String.format("%.2f", highJumpAnimation.getHeight()) + "m", 20, 60);
    }

    private void drawResults(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString("Final Results", 350, 80);

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        int y = 150;
        for (EventResult result : gameState.getResults()) {
            g2d.drawString(result.getEventName() + ": " +
                String.format("%.2f", result.getPerformance()) +
                " - Score: " + result.getScore(), 200, y);
            y += 40;
        }

        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.setColor(Color.YELLOW);
        g2d.drawString("Total Score: " + gameState.getTotalScore(), 300, y + 50);
    }
}

class SprintAnimation {
    private double position;
    private double velocity;
    private double time;
    private boolean running;
    private boolean complete;
    private int frameCount;
    private Random random;

    public SprintAnimation() {
        random = new Random();
        reset();
    }

    public void start() {
        reset();
        running = true;
    }

    private void reset() {
        position = 50;
        velocity = 0;
        time = 0;
        running = false;
        complete = false;
        frameCount = 0;
    }

    public void update() {
        if (!running || complete) return;

        velocity = 8 + random.nextDouble() * 2;
        position += velocity;
        time += 0.016;
        frameCount++;

        if (position >= 950) {
            position = 950;
            complete = true;
            running = false;
            time = 9.5 + random.nextDouble() * 1.5;
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.BLUE);
        int x = (int) position;
        int y = 450;

        g2d.fillOval(x - 15, y - 40, 30, 30);

        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(x, y - 10, x, y + 20);

        int legAngle = (frameCount / 5) % 2 == 0 ? 20 : -20;
        g2d.drawLine(x, y + 20, x - legAngle, y + 50);
        g2d.drawLine(x, y + 20, x + legAngle, y + 50);

        int armAngle = (frameCount / 5) % 2 == 0 ? -20 : 20;
        g2d.drawLine(x, y, x + armAngle, y + 20);
        g2d.drawLine(x, y, x - armAngle, y + 20);
    }

    public boolean isComplete() {
        return complete;
    }

    public double getTime() {
        return time;
    }
}

class LongJumpAnimation {
    private double x, y;
    private double velocityX, velocityY;
    private double distance;
    private boolean jumping;
    private boolean complete;
    private int phase;
    private static final double GRAVITY = 0.5;

    public LongJumpAnimation() {
        reset();
    }

    public void start(double power) {
        reset();
        jumping = true;
        velocityX = 8 + power * 2;
        velocityY = -12 - power;
    }

    private void reset() {
        x = 100;
        y = 450;
        velocityX = 0;
        velocityY = 0;
        distance = 0;
        jumping = false;
        complete = false;
        phase = 0;
    }

    public void update() {
        if (!jumping || complete) return;

        if (phase == 0) {
            x += 8;
            if (x >= 380) {
                phase = 1;
            }
        } else if (phase == 1) {
            x += velocityX;
            y += velocityY;
            velocityY += GRAVITY;

            if (y >= 450) {
                y = 450;
                complete = true;
                jumping = false;
                distance = (x - 400) / 100.0;
                if (distance < 0) distance = 0;
            }
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        int drawX = (int) x;
        int drawY = (int) y;

        g2d.fillOval(drawX - 15, drawY - 40, 30, 30);

        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(drawX, drawY - 10, drawX, drawY + 20);

        if (phase == 1) {
            g2d.drawLine(drawX, drawY + 20, drawX - 30, drawY + 40);
            g2d.drawLine(drawX, drawY + 20, drawX - 10, drawY + 40);
            g2d.drawLine(drawX, drawY, drawX + 30, drawY - 10);
            g2d.drawLine(drawX, drawY, drawX + 30, drawY + 10);
        } else {
            g2d.drawLine(drawX, drawY + 20, drawX - 20, drawY + 50);
            g2d.drawLine(drawX, drawY + 20, drawX + 20, drawY + 50);
            g2d.drawLine(drawX, drawY, drawX - 20, drawY + 20);
            g2d.drawLine(drawX, drawY, drawX + 20, drawY + 20);
        }
    }

    public boolean isComplete() {
        return complete;
    }

    public double getDistance() {
        return distance;
    }
}

class JavelinAnimation {
    private double x, y;
    private double velocityX, velocityY;
    private double angle;
    private double rotation;
    private double distance;
    private boolean throwing;
    private boolean complete;
    private int phase;
    private static final double GRAVITY = 0.3;

    public JavelinAnimation() {
        reset();
    }

    public void start(double throwAngle, double power) {
        reset();
        throwing = true;
        angle = Math.toRadians(throwAngle);
        double speed = 15 + power * 5;
        velocityX = speed * Math.cos(angle);
        velocityY = -speed * Math.sin(angle);
        rotation = throwAngle;
    }

    private void reset() {
        x = 100;
        y = 350;
        velocityX = 0;
        velocityY = 0;
        angle = 0;
        rotation = 0;
        distance = 0;
        throwing = false;
        complete = false;
        phase = 0;
    }

    public void update() {
        if (!throwing || complete) return;

        if (phase == 0) {
            phase = 1;
        } else if (phase == 1) {
            x += velocityX;
            y += velocityY;
            velocityY += GRAVITY;
            rotation = Math.toDegrees(Math.atan2(velocityY, velocityX));

            if (y >= 500) {
                y = 500;
                complete = true;
                throwing = false;
                distance = x / 8.0;
            }
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.ORANGE);
        int drawX = (int) x;
        int drawY = (int) y;

        if (phase == 0 || !throwing) {
            g2d.fillOval(drawX - 15, drawY - 40, 30, 30);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(drawX, drawY - 10, drawX, drawY + 20);
            g2d.drawLine(drawX, drawY + 20, drawX - 15, drawY + 50);
            g2d.drawLine(drawX, drawY + 20, drawX + 15, drawY + 50);

            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(4));
            g2d.drawLine(drawX + 20, drawY - 20, drawX + 60, drawY - 30);
        } else {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(4));
            AffineTransform old = g2d.getTransform();
            g2d.rotate(Math.toRadians(rotation), drawX, drawY);
            g2d.drawLine(drawX - 30, drawY, drawX + 30, drawY);
            g2d.setColor(Color.RED);
            g2d.fillPolygon(new int[]{drawX + 30, drawX + 40, drawX + 30},
                           new int[]{drawY - 3, drawY, drawY + 3}, 3);
            g2d.setTransform(old);
        }
    }

    public boolean isComplete() {
        return complete;
    }

    public double getDistance() {
        return distance;
    }
}

class HighJumpAnimation {
    private double x, y;
    private double velocityY;
    private double barHeight;
    private double maxHeight;
    private boolean jumping;
    private boolean complete;
    private int phase;
    private static final double GRAVITY = 0.4;

    public HighJumpAnimation() {
        reset();
    }

    public void start(double power) {
        reset();
        jumping = true;
        velocityY = -15 - power * 3;
        barHeight = 1.5 + power * 0.5;
    }

    private void reset() {
        x = 200;
        y = 450;
        velocityY = 0;
        barHeight = 1.5;
        maxHeight = 0;
        jumping = false;
        complete = false;
        phase = 0;
    }

    public void update() {
        if (!jumping || complete) return;

        if (phase == 0) {
            x += 5;
            if (x >= 480) {
                phase = 1;
            }
        } else if (phase == 1) {
            y += velocityY;
            velocityY += GRAVITY;
            x += 2;

            double currentHeight = (500 - y) / 100.0;
            if (currentHeight > maxHeight) {
                maxHeight = currentHeight;
            }

            if (y >= 450) {
                y = 450;
                complete = true;
                jumping = false;
            }
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.GREEN);
        int drawX = (int) x;
        int drawY = (int) y;

        g2d.fillOval(drawX - 15, drawY - 40, 30, 30);

        g2d.setStroke(new BasicStroke(3));

        if (phase == 1) {
            g2d.drawLine(drawX, drawY - 10, drawX - 10, drawY + 20);
            g2d.drawLine(drawX, drawY + 20, drawX + 20, drawY + 10);
            g2d.drawLine(drawX, drawY + 20, drawX - 10, drawY + 40);
            g2d.drawLine(drawX, drawY, drawX - 30, drawY + 10);
            g2d.drawLine(drawX, drawY, drawX + 20, drawY - 10);
        } else {
            g2d.drawLine(drawX, drawY - 10, drawX, drawY + 20);
            g2d.drawLine(drawX, drawY + 20, drawX - 15, drawY + 50);
            g2d.drawLine(drawX, drawY + 20, drawX + 15, drawY + 50);
            g2d.drawLine(drawX, drawY, drawX - 20, drawY + 20);
            g2d.drawLine(drawX, drawY, drawX + 20, drawY + 20);
        }
    }

    public boolean isComplete() {
        return complete;
    }

    public double getHeight() {
        return maxHeight;
    }

    public double getBarHeight() {
        return barHeight;
    }
}

class ControlPanel extends JPanel {
    private GameState gameState;
    private GamePanel gamePanel;
    private JButton sprintButton, longJumpButton, javelinButton, highJumpButton;
    private JButton actionButton;
    private JSlider powerSlider, angleSlider;
    private JLabel statusLabel;

    public ControlPanel(GameState gameState, GamePanel gamePanel) {
        this.gameState = gameState;
        this.gamePanel = gamePanel;

        setLayout(new FlowLayout());
        setBackground(new Color(50, 50, 50));

        sprintButton = new JButton("100m Sprint");
        longJumpButton = new JButton("Long Jump");
        javelinButton = new JButton("Javelin");
        highJumpButton = new JButton("High Jump");
        actionButton = new JButton("Start");

        powerSlider = new JSlider(0, 100, 50);
        angleSlider = new JSlider(20, 60, 45);

        statusLabel = new JLabel("Select an event");
        statusLabel.setForeground(Color.WHITE);

        sprintButton.addActionListener(e -> selectEvent(GameState.Event.SPRINT_100M));
        longJumpButton.addActionListener(e -> selectEvent(GameState.Event.LONG_JUMP));
        javelinButton.addActionListener(e -> selectEvent(GameState.Event.JAVELIN_THROW));
        highJumpButton.addActionListener(e -> selectEvent(GameState.Event.HIGH_JUMP));
        actionButton.addActionListener(e -> performAction());

        add(sprintButton);
        add(longJumpButton);
        add(javelinButton);
        add(highJumpButton);
        add(new JLabel("Power:"));
        add(powerSlider);
        add(new JLabel("Angle:"));
        add(angleSlider);
        add(actionButton);
        add(statusLabel);
    }

    private void selectEvent(GameState.Event event) {
        gameState.setCurrentEvent(event);
        statusLabel.setText("Event selected: " + event.name());
        actionButton.setEnabled(true);
    }

    private void performAction() {
        if (gamePanel.isAnimationComplete()) {
            double performance = gamePanel.getPerformance();
            int score = calculateScore(performance);

            String eventName = gameState.getCurrentEvent().name();
            gameState.addResult(new EventResult(eventName, performance, score));

            if (gameState.getResults().size() >= 4) {
                gameState.setCurrentEvent(GameState.Event.RESULTS);
                actionButton.setEnabled(false);
                statusLabel.setText("All events completed!");
                return;
            }
        }

        switch (gameState.getCurrentEvent()) {
            case SPRINT_100M:
                gamePanel.startSprint();
                statusLabel.setText("Running...");
                break;
            case LONG_JUMP:
                double jumpPower = powerSlider.getValue() / 100.0;
                gamePanel.startLongJump(jumpPower);
                statusLabel.setText("Jumping...");
                break;
            case JAVELIN_THROW:
                double throwAngle = angleSlider.getValue();
                double throwPower = powerSlider.getValue() / 100.0;
                gamePanel.startJavelin(throwAngle, throwPower);
                statusLabel.setText("Throwing...");
                break;
            case HIGH_JUMP:
                double highJumpPower = powerSlider.getValue() / 100.0;
                gamePanel.startHighJump(highJumpPower);
                statusLabel.setText("Jumping...");
                break;
        }

        actionButton.setEnabled(false);

        Timer enableTimer = new Timer(3000, e -> {
            actionButton.setEnabled(true);
            statusLabel.setText("Ready for next attempt");
        });
        enableTimer.setRepeats(false);
        enableTimer.start();
    }

    private int calculateScore(double performance) {
        switch (gameState.getCurrentEvent()) {
            case SPRINT_100M:
                return (int) Math.max(0, 1000 - (performance - 9.5) * 100);
            case LONG_JUMP:
                return (int) (performance * 100);
            case JAVELIN_THROW:
                return (int) (performance * 10);
            case HIGH_JUMP:
                return (int) (performance * 200);
            default:
                return 0;
        }
    }
}
