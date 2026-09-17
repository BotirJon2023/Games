import java.awt.*;

class Track {

    // Simple parametric ground: combination of sine waves
    public double getGroundY(double x) {
        double base = GamePanel.HEIGHT - 120;
        double hill1 = 40 * Math.sin(x / 120.0);
        double hill2 = 25 * Math.sin(x / 45.0 + 1.5);
        return base + hill1 + hill2;
    }

    public void draw(Graphics2D g2) {
        g2.setColor(new Color(60, 40, 20));
        int step = 4;
        for (int i = 0; i < GamePanel.WIDTH; i += step) {
            int x1 = i;
            int x2 = i + step;
            int y1 = (int) getGroundY(x1);
            int y2 = (int) getGroundY(x2);
            g2.fillPolygon(new int[]{x1, x2, x2, x1},
                    new int[]{y1, y2, GamePanel.HEIGHT, GamePanel.HEIGHT},
                    4);
        }

        // Add a “trail” line
        g2.setColor(new Color(120, 80, 40));
        g2.setStroke(new BasicStroke(3f));
        for (int i = 0; i < GamePanel.WIDTH - step; i += step) {
            int x1 = i;
            int x2 = i + step;
            int y1 = (int) getGroundY(x1) - 5;
            int y2 = (int) getGroundY(x2) - 5;
            g2.drawLine(x1, y1, x2, y2);
        }
    }
}
