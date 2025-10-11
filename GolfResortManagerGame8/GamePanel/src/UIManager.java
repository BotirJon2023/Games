import java.awt.*;

public class UIManager {
    public void draw(Graphics g) {
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, 1024, 50);
        g.setColor(Color.BLACK);
        g.drawString("Welcome to Golf Resort Manager!", 20, 30);
    }
}
