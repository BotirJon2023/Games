public class Player {
    private int x, y;
    private int width, height;
    private int speed;
    private int yVelocity; // For jumping/gravity
    private boolean onGround; // To manage jumping

    public Player(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = 5; // Pixels per frame
        this.yVelocity = 0;
        this.onGround = true; // Assume starts on ground
    }

    public void update() {
        // Apply gravity if not on ground
        if (!onGround) {
            yVelocity += 1; // Simple gravity effect (increase velocity downwards)
            y += yVelocity;

            // Simple ground collision (replace with actual collision detection)
            if (y + height >= 500) { // Assuming floor at y=500 for example
                y = 500 - height; // Set player on ground
                yVelocity = 0;
                onGround = true;
            }
        }

        // Add boundary checks
        if (x < 0) x = 0;
        if (x + width > 800) x = 800 - width; // Assuming panel width 800
    }

    public void jump() {
        if (onGround) {
            yVelocity = -15; // Give upward velocity
            onGround = false;
        }
    }

    // Getters and Setters
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getSpeed() { return speed; }
}