import java.util.*;

public class TeamFactory {
    public static List<Team> createTeams() {
        String[] names = {
                "New Zealand", "South Africa", "England", "France",
                "Ireland", "Australia", "Argentina", "Wales",
                "Scotland", "Japan", "Italy", "Fiji",
                "Georgia", "Samoa", "Tonga", "Uruguay"
        };
        Random rand = new Random();
        List<Team> teams = new ArrayList<>();
        for (String name : names) {
            teams.add(new Team(name, rand.nextInt(50) + 50)); // Skill: 50-100
        }
        return teams;
    }
}
