import java.awt.*;

class Bike {
    private double x, y;
    private double vx, vy;
    private final Color color;
    private final String name;

    private boolean onGround = false;
    private boolean computerControlled = false;

    // Tuning parameters
    private final double accel = 0.4;
    private final double maxSpeed = 8.0;
    private final double friction = 0.15;
    private final double jumpStrength = -10.0;
    private final double gravity = 0.5;

    public Bike(double x, double y, Color color, String name) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.name = name;
    }

    public void setComputerControlled(boolean computerControlled) {
        this.computerControlled = computerControlled;
    }

    public boolean isComputerControlled() {
        return computerControlled;
    }

    public String getName() {
        return name;
    }

    public double getSpeed() {
        return Math.abs(vx);
    }

    public void handleInput(boolean left, boolean right, boolean jump) {
        if (left) vx -= accel;
        if (right) vx += accel;

        // Limit speed
        if (vx > maxSpeed) vx = maxSpeed;
        if (vx < -maxSpeed) vx = -maxSpeed;

        // Jump only if on ground
        if (jump && onGround) {
            vy = jumpStrength;
            onGround = false;
        }
    }

    public void aiUpdate(Track track, Bike target) {
        // Very simple AI: try to match target x, jump on slopes
        if (target.x > this.x + 30) {
            vx += accel;
        } else if (target.x < this.x - 30) {
            vx -= accel;
        }

        if (vx > maxSpeed) vx = maxSpeed;
        if (vx < -maxSpeed) vx = -maxSpeed;

        // Jump when approaching a steep slope
        double groundY = track.getGroundY(x);
        double aheadGroundY = track.getGroundY(x + 40);
        if (aheadGroundY < groundY - 15 && onGround) {
            vy = jumpStrength;
            onGround = false;
        }
    }

    public void update(Track track) {
        // Apply friction
        if (!computerControlled) {
            if (Math.abs(vx) > 0.01) {
                vx -= Math.signum(vx) * friction;
            } else {
                vx = 0;
            }
        }

        // Gravity
        vy += gravity;

        // Move
        x += vx;
        y += vy;

        // Track collision
        double groundY = track.getGroundY(x);
        if (y >= groundY) {
            y = groundY;
            vy = 0;
            onGround = true;
        } else {
            onGround = false;
        }

        // Keep in bounds
        if (x < 0) x = 0;
        if (x > GamePanel.WIDTH - 40) x = GamePanel.WIDTH - 40;
    }

    public void draw(Graphics2D g2) {
        // Draw bike body
        int bx = (int) x;
        int by = (int) y;

        // Wheels
        g2.setColor(Color.DARK_GRAY);
        g2.fillOval(bx - 20, by - 10, 20, 20);
        g2.fillOval(bx + 10, by - 10, 20, 20);

        // Frame
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(color);
        g2.drawLine(bx - 10, by - 5, bx + 20, by - 20);
        g2.drawLine(bx + 20, by - 20, bx + 10, by - 35);
        g2.drawLine(bx - 10, by - 5, bx + 10, by - 35);

        // Rider (simple stick figure)
        g2.setColor(new Color(255, 220, 180));
        g2.fillOval(bx + 5, by - 50, 14, 14);
        g2.setColor(color);
        g2.drawLine(bx + 12, by - 36, bx + 12, by - 20);
        g2.drawLine(bx + 12, by - 36, bx + 2, by - 28);
        g2.drawLine(bx + 12, by - 36, bx + 22, by - 28);
    }
}
