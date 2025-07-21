import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GamePanel extends JPanel {
    private Tournament tournament;

    public GamePanel(Tournament tournament) {
        this.tournament = tournament;
        setBackground(new Color(20, 80, 20));
    }

    public void update() {
        // Can be extended for animations
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawGroups(g);
        drawKnockouts(g);
    }

    private void drawGroups(Graphics g) {
        int x = 20;
        int y = 30;
        g.setColor(Color.white);
        for (Group group : tournament.getGroups()) {
            g.drawString(group.getName(), x, y);
            y += 20;
            for (Match m : group.getMatches()) {
                if (m.isPlayed()) {
                    g.drawString(m.getResult(), x + 10, y);
                    y += 20;
                }
            }
            y += 20;
        }
    }

    private void drawKnockouts(Graphics g) {
        int x = 500;
        int y = 30;
        g.setColor(Color.yellow);
        List<Match> knockouts = tournament.getKnockoutMatches();
        for (int i = 0; i < tournament.getKnockoutStage(); i++) {
            Match m = knockouts.get(i);
            g.drawString("KO Match: " + m.getResult(), x, y);
            y += 30;
        }
    }
}
