import java.util.*;

public class Group {
    private String name;
    private List<Team> teams;
    private List<Match> matches;

    public Group(String name, List<Team> teams) {
        this.name = name;
        this.teams = teams;
        this.matches = new ArrayList<>();
        generateMatches();
    }

    private void generateMatches() {
        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                matches.add(new Match(teams.get(i), teams.get(j)));
            }
        }
    }

    public void playMatches() {
        for (Match m : matches) m.play();
    }

    public List<Team> getTopTwoTeams() {
        teams.sort(Comparator.comparingInt(Team::getPoints).reversed());
        return teams.subList(0, 2);
    }

    public List<Match> getMatches() {
        return matches;
    }

    public String getName() {
        return name;
    }
}
