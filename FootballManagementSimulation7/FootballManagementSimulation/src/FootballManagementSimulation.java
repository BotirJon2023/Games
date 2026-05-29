import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class FootballManagementSimulation extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FootballManagementSimulation game = new FootballManagementSimulation();
            game.setVisible(true);
        });
    }

    public FootballManagementSimulation() {
        setTitle("Football Management Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(new MenuPanel(this));
        pack();
        setLocationRelativeTo(null);
    }

    void startGame(boolean vsComputer) {
        setContentPane(new GamePanel(this, vsComputer));
        revalidate();
        repaint();
    }
}

class MenuPanel extends JPanel {
    public MenuPanel(FootballManagementSimulation frame) {
        setPreferredSize(new Dimension(1100, 700));
        setLayout(new GridBagLayout());
        setBackground(new Color(12, 30, 58));

        JPanel card = new RoundedPanel();
        card.setPreferredSize(new Dimension(420, 300));
        card.setLayout(new GridBagLayout());
        card.setBackground(new Color(18, 44, 80));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.gridx = 0;

        JLabel title = new JLabel("Football Management Simulation");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        JButton twoPlayers = styledButton("2 Players");
        JButton vsComputer = styledButton("Play vs Computer");

        twoPlayers.addActionListener(e -> frame.startGame(false));
        vsComputer.addActionListener(e -> frame.startGame(true));

        c.gridy = 0;
        card.add(title, c);
        c.gridy = 1;
        card.add(twoPlayers, c);
        c.gridy = 2;
        card.add(vsComputer, c);

        add(card);
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 18));
        b.setBackground(new Color(44, 139, 78));
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        return b;
    }
}

class GamePanel extends JPanel implements ActionListener {
    private final FootballManagementSimulation frame;
    private final boolean vsComputer;
    private final Timer timer;
    private final Random random = new Random();

    private int scoreA = 0;
    private int scoreB = 0;
    private int minute = 0;
    private int matchClock = 0;

    private final int fieldX = 70;
    private final int fieldY = 70;
    private final int fieldW = 960;
    private final int fieldH = 560;

    private final Player p1 = new Player(260, 320, new Color(255, 80, 80), "A");
    private final Player p2 = new Player(840, 320, new Color(80, 160, 255), "B");
    private final Ball ball = new Ball(550, 350);

    private boolean possessionA = true;
    private int possessionTicks = 0;
    private String message = "Kick-off!";
    private int messageTimer = 120;

    public GamePanel(FootballManagementSimulation frame, boolean vsComputer) {
        this.frame = frame;
        this.vsComputer = vsComputer;
        setPreferredSize(new Dimension(1100, 700));
        setBackground(new Color(10, 20, 35));
        setDoubleBuffered(true);
        setFocusable(true);

        setupKeyBindings();
        timer = new Timer(16, this);
        timer.start();
    }

    private void setupKeyBindings() {
        bindKey("pressed W", e -> p1.move(0, -6, fieldX, fieldY, fieldW, fieldH));
        bindKey("pressed S", e -> p1.move(0, 6, fieldX, fieldY, fieldW, fieldH));
        bindKey("pressed A", e -> p1.move(-6, 0, fieldX, fieldY, fieldW, fieldH));
        bindKey("pressed D", e -> p1.move(6, 0, fieldX, fieldY, fieldW, fieldH));
        bindKey("pressed UP", e -> p2.move(0, -6, fieldX, fieldY, fieldW, fieldH));
        bindKey("pressed DOWN", e -> p2.move(0, 6, fieldX, fieldY, fieldW, fieldH));
        bindKey("pressed LEFT", e -> p2.move(-6, 0, fieldX, fieldY, fieldW, fieldH));
        bindKey("pressed RIGHT", e -> p2.move(6, 0, fieldX, fieldY, fieldW, fieldH));
        bindKey("pressed ESCAPE", e -> System.exit(0));
    }

    private void bindKey(String key, ActionListener action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        matchClock++;
        if (matchClock % 60 == 0) minute++;

        if (vsComputer && !possessionA) {
            aiMove();
        }

        updateBallPhysics();
        handlePossessionAndShots();

        if (messageTimer > 0) messageTimer--;
        repaint();
    }

    private void aiMove() {
        int dx = Integer.compare((int) ball.x, p2.x) * 4;
        int dy = Integer.compare((int) ball.y, p2.y) * 4;
        p2.move(dx, dy, fieldX, fieldY, fieldW, fieldH);
    }

    private void updateBallPhysics() {
        ball.vx *= 0.985;
        ball.vy *= 0.985;
        ball.x += ball.vx;
        ball.y += ball.vy;

        int left = fieldX + 10;
        int right = fieldX + fieldW - 10;
        int top = fieldY + 10;
        int bottom = fieldY + fieldH - 10;

        if (ball.y < top) {
            ball.y = top;
            ball.vy = -ball.vy * 0.8;
        }
        if (ball.y > bottom) {
            ball.y = bottom;
            ball.vy = -ball.vy * 0.8;
        }

        if (ball.x < fieldX - 15) {
            scoreB++;
            resetAfterGoal(false);
        }
        if (ball.x > fieldX + fieldW + 15) {
            scoreA++;
            resetAfterGoal(true);
        }
    }

    private void handlePossessionAndShots() {
        Player attacker = possessionA ? p1 : p2;
        Player defender = possessionA ? p2 : p1;

        if (attacker.distanceTo(ball.x, ball.y) < 34) {
            ball.x = attacker.x + attacker.radius + 8;
            ball.y = attacker.y;
            if (matchClock % 8 == 0) {
                int targetX = possessionA ? fieldX + fieldW - 25 : fieldX + 25;
                int targetY = fieldY + 40 + random.nextInt(fieldH - 80);
                double dx = targetX - ball.x;
                double dy = targetY - ball.y;
                double len = Math.max(1, Math.sqrt(dx * dx + dy * dy));
                ball.vx = (dx / len) * (possessionA ? 8.0 : -8.0);
                ball.vy = (dy / len) * 4.0;
                message = possessionA ? "Player A attacks!" : (vsComputer ? "Computer counterattacks!" : "Player B attacks!");
                messageTimer = 90;
            }
        } else {
            double dist = defender.distanceTo(ball.x, ball.y);
            if (dist < 22) {
                possessionA = !possessionA;
                message = possessionA ? "Ball stolen by Player A" : (vsComputer ? "Computer stole the ball" : "Ball stolen by Player B");
                messageTimer = 90;
            }
        }

        if (!possessionA && vsComputer) {
            if (random.nextInt(240) == 0) {
                ball.vx += random.nextDouble() * 8 - 4;
                ball.vy += random.nextDouble() * 6 - 3;
            }
        }
    }

    private void resetAfterGoal(boolean toA) {
        possessionA = toA;
        p1.x = 260;
        p1.y = 320;
        p2.x = 840;
        p2.y = 320;
        ball.x = 550;
        ball.y = 350;
        ball.vx = 0;
        ball.vy = 0;
        message = (toA ? "GOAL for Player A!" : "GOAL for Player B!");
        messageTimer = 180;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        applyQuality(g2);

        drawBackground(g2);
        drawField(g2);
        drawHud(g2);
        drawPlayers(g2);
        drawBall(g2);

        if (messageTimer > 0) {
            drawMessage(g2);
        }

        g2.dispose();
    }

    private void applyQuality(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    private void drawBackground(Graphics2D g2) {
        GradientPaint gp = new GradientPaint(0, 0, new Color(10, 20, 35), 0, getHeight(), new Color(18, 55, 40));
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawField(Graphics2D g2) {
        g2.setColor(new Color(30, 130, 60));
        g2.fillRoundRect(fieldX, fieldY, fieldW, fieldH, 30, 30);

        g2.setColor(new Color(240, 240, 240, 230));
        g2.setStroke(new BasicStroke(4));
        g2.drawRoundRect(fieldX, fieldY, fieldW, fieldH, 30, 30);

        g2.drawLine(fieldX + fieldW / 2, fieldY, fieldX + fieldW / 2, fieldY + fieldH);
        g2.drawOval(fieldX + fieldW / 2 - 60, fieldY + fieldH / 2 - 60, 120, 120);

        g2.drawRect(fieldX, fieldY + fieldH / 2 - 90, 90, 180);
        g2.drawRect(fieldX + fieldW - 90, fieldY + fieldH / 2 - 90, 90, 180);

        g2.setColor(new Color(255, 255, 255, 45));
        for (int i = 0; i < 10; i++) {
            int stripeX = fieldX + i * (fieldW / 10);
            g2.fillRect(stripeX, fieldY, fieldW / 10, fieldH);
        }
    }

    private void drawHud(Graphics2D g2) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2.setColor(Color.WHITE);
        g2.drawString("Player A", 80, 40);
        g2.drawString(scoreA + "  :  " + scoreB, 510, 40);
        g2.drawString(vsComputer ? "Computer" : "Player B", 880, 40);
        g2.drawString("Minute: " + minute, 520, 675);
    }

    private void drawPlayers(Graphics2D g2) {
        p1.draw(g2);
        p2.draw(g2);
    }

    private void drawBall(Graphics2D g2) {
        ball.draw(g2);
    }

    private void drawMessage(Graphics2D g2) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(message);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect((getWidth() - w) / 2 - 18, 92, w + 36, 44, 20, 20);
        g2.setColor(Color.WHITE);
        g2.drawString(message, (getWidth() - w) / 2, 124);
    }
}

class Player {
    int x, y;
    int radius = 18;
    Color color;
    String label;

    Player(int x, int y, Color color, String label) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.label = label;
    }

    void move(int dx, int dy, int fx, int fy, int fw, int fh) {
        x += dx;
        y += dy;
        x = Math.max(fx + 25, Math.min(fx + fw - 25, x));
        y = Math.max(fy + 25, Math.min(fy + fh - 25, y));
    }

    double distanceTo(double ox, double oy) {
        double dx = x - ox;
        double dy = y - oy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    void draw(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(x - radius + 4, y - radius + 6, radius * 2, radius * 2);

        GradientPaint gp = new GradientPaint(x - radius, y - radius, color.brighter(), x + radius, y + radius, color.darker());
        g2.setPaint(gp);
        g2.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(label);
        g2.drawString(label, x - tw / 2, y + 5);
    }
}

class Ball {
    double x, y;
    double vx = 0;
    double vy = 0;
    int radius = 10;

    Ball(double x, double y) {
        this.x = x;
        this.y = y;
    }

    void draw(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval((int) x - radius + 3, (int) y - radius + 4, radius * 2, radius * 2);

        GradientPaint gp = new GradientPaint((int) x - radius, (int) y - radius, Color.WHITE, (int) x + radius, (int) y + radius, new Color(200, 200, 200));
        g2.setPaint(gp);
        g2.fillOval((int) x - radius, (int) y - radius, radius * 2, radius * 2);

        g2.setColor(new Color(60, 60, 60));
        g2.setStroke(new BasicStroke(2));
        g2.drawOval((int) x - radius, (int) y - radius, radius * 2, radius * 2);
        g2.drawLine((int) x - 4, (int) y, (int) x + 4, (int) y);
        g2.drawLine((int) x, (int) y - 4, (int) x, (int) y + 4);
    }
}

class RoundedPanel extends JPanel {
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
        g2.dispose();
        super.paintComponent(g);
    }
}