import java.awt.*;

public class GolfCourse {
    private int x, y;
    private int width = 150, height = 100;

    public GolfCourse(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillRect(x, y, width, height);
        g.setColor(Color.BLACK);
        g.drawString("Golf Course", x + 30, y + 50);
    }
}
