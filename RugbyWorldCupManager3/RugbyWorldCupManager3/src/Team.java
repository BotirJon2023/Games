public class Team {
    private String name;
    private int skill;
    private int points;
    private int scored;
    private int conceded;

    public Team(String name, int skill) {
        this.name = name;
        this.skill = skill;
        this.points = 0;
        this.scored = 0;
        this.conceded = 0;
    }

    public String getName() { return name; }
    public int getSkill() { return skill; }
    public int getPoints() { return points; }
    public int getScored() { return scored; }
    public int getConceded() { return conceded; }

    public void addMatchResult(int scored, int conceded) {
        this.scored += scored;
        this.conceded += conceded;
        if (scored > conceded) points += 3;
        else if (scored == conceded) points += 1;
    }
}
