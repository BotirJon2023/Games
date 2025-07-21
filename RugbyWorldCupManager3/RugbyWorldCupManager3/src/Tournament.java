import java.util.*;

public class Tournament {
    private List<Group> groups = new ArrayList<>();
    private List<Team> qualifiedTeams = new ArrayList<>();
    private List<Match> knockoutMatches = new ArrayList<>();
    private int knockoutStage = 0;

    public Tournament(List<Team> allTeams) {
        for (int i = 0; i < 4; i++) {
            List<Team> groupTeams = allTeams.subList(i * 4, (i + 1) * 4);
            groups.add(new Group("Group " + (char)('A' + i), new ArrayList<>(groupTeams)));
        }
    }

    public void runGroupStage() {
        for (Group g : groups) g.playMatches();
        for (Group g : groups) qualifiedTeams.addAll(g.getTopTwoTeams());
        generateKnockoutMatches();
    }

    public boolean isReadyForNextPhase() {
        return knockoutStage < knockoutMatches.size();
    }

    public void runKnockoutStage() {
        if (knockoutStage >= knockoutMatches.size()) return;
        Match m = knockoutMatches.get(knockoutStage);
        m.play();
        knockoutStage++;
    }

    private void generateKnockoutMatches() {
        for (int i = 0; i < qualifiedTeams.size(); i += 2) {
            knockoutMatches.add(new Match(qualifiedTeams.get(i), qualifiedTeams.get(i + 1)));
        }
    }

    public List<Group> getGroups() {
        return groups;
    }

    public List<Match> getKnockoutMatches() {
        return knockoutMatches;
    }

    public int getKnockoutStage() {
        return knockoutStage;
    }
}
