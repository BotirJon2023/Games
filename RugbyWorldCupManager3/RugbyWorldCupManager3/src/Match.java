public class Match {
    private Team team1;
    private Team team2;
    private int score1;
    private int score2;
    private boolean played = false;

    public Match(Team t1, Team t2) {
        this.team1 = t1;
        this.team2 = t2;
    }

    public void play() {
        score1 = simulateScore(team1.getSkill(), team2.getSkill());
        score2 = simulateScore(team2.getSkill(), team1.getSkill());
        team1.addMatchResult(score1, score2);
        team2.addMatchResult(score2, score1);
        played = true;
    }

    private int simulateScore(int attack, int defense) {
        double base = Math.random() * 20;
        return (int)(base + (attack - defense) * 0.2 + Math.random() * 10);
    }

    public String getResult() {
        return team1.getName() + " " + score1 + " - " + score2 + " " + team2.getName();
    }

    public boolean isPlayed() {
        return played;
    }
}
