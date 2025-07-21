import javax.swing.*;
import java.util.*;

public class RugbyWorldCupGame {
    private JFrame frame;
    private GamePanel gamePanel;
    private List<Team> teams;
    private Tournament tournament;

    public RugbyWorldCupGame() {
        teams = TeamFactory.createTeams();
        Collections.shuffle(teams);
        tournament = new Tournament(teams);
    }

    public void start() {
        frame = new JFrame("Rugby World Cup Manager");
        gamePanel = new GamePanel(tournament);
        frame.add(gamePanel);
        frame.setSize(1000, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        tournament.runGroupStage();
        runTournamentLoop();
    }

    private void runTournamentLoop() {
        new Timer(100, e -> {
            gamePanel.update();
            gamePanel.repaint();
            if (tournament.isReadyForNextPhase()) {
                tournament.runKnockoutStage();
            }
        }).start();
    }
}
