
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AISportsSimulator {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameConfig config = new GameConfig();
            config.LEAGUE_TEAMS = 8;
            config.SEASON_MATCHES = 14; // round-robin-ish
            Simulator simulator = new Simulator(config);
            simulator.start();
        });
    }

}

/* ======================== CONFIG ======================== */
class GameConfig {
    public int FIELD_WIDTH = 900;
    public int FIELD_HEIGHT = 600;
    public int TEAM_SIZE = 7; // small-team sport for compact UI
    public int LEAGUE_TEAMS = 6;
    public int SEASON_MATCHES = 10;
    public int TICKS_PER_SECOND = 25;
    public int MATCH_DURATION_SECONDS = 60; // short matches for demo (1 minute)
    public int SUBSTITUTION_LIMIT = 3;
}

/* ======================== SIMULATOR (ENTRY) ======================== */
class Simulator {
    private final GameConfig config;
    private final League league;
    private final Renderer renderer;
    private final JFrame frame;
    private final AtomicBoolean running = new AtomicBoolean(false);

    Simulator(GameConfig config) {
        this.config = config;
        this.league = new League(config);
        this.renderer = new Renderer(config, league);

        frame = new JFrame("AI-Powered Sports Team Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(renderer.getMainPanel(), BorderLayout.CENTER);
        frame.add(renderer.getControlPanel(), BorderLayout.EAST);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    void start() {
        league.generateTeams();
        renderer.refreshTeamsList();
        running.set(true);
        // schedule league season on background thread
        new Thread(() -> runSeason()).start();
    }

    private void runSeason() {
        AIManager aiManager = new AIManager();
        try {
            // Each match in schedule
            List<Match> schedule = league.createSchedule(config.SEASON_MATCHES);
            for (int i = 0; i < schedule.size() && running.get(); i++) {
                Match m = schedule.get(i);
                renderer.displayStatus(String.format("Match %d/%d: %s vs %s", i + 1, schedule.size(), m.home.name, m.away.name));
                // Pre-match AI decisions
                aiManager.prepareTeamsForMatch(m.home);
                aiManager.prepareTeamsForMatch(m.away);

                GameEngine engine = new GameEngine(config, m, renderer, aiManager);
                engine.playMatch();

                // process result into standings
                league.recordResult(m);
                renderer.refreshLeaderboard(league.getStandings());

                // Off-day: players recover and train
                league.postMatchRecoveryAndTraining();

                // small pause between matches
                Thread.sleep(1200);
            }

            renderer.displayStatus("Season completed. Final standings:");
            renderer.refreshLeaderboard(league.getStandings());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/* ======================== LEAGUE, TEAM, PLAYER MODELS ======================== */
class League {
    private final GameConfig config;
    public List<Team> teams = new ArrayList<>();
    private final Random rng = new Random();

    League(GameConfig config) {
        this.config = config;
    }

    void generateTeams() {
        teams.clear();
        for (int i = 0; i < config.LEAGUE_TEAMS; i++) {
            Team t = TeamFactory.createTeam("Team " + (i + 1), config.TEAM_SIZE, rng);
            teams.add(t);
        }
    }

    List<Match> createSchedule(int matchesPerTeam) {
        // naive random pairings, matchesPerTeam total per team
        List<Match> schedule = new ArrayList<>();
        int totalMatches = (config.LEAGUE_TEAMS * matchesPerTeam) / 2;
        for (int i = 0; i < totalMatches; i++) {
            Team a = teams.get(rng.nextInt(teams.size()));
            Team b;
            do {
                b = teams.get(rng.nextInt(teams.size()));
            } while (b == a);
            schedule.add(new Match(a, b));
        }
        return schedule;
    }

    void recordResult(Match m) {
        // update simple standings
        m.home.played++;
        m.away.played++;
        m.home.goalsFor += m.homeScore;
        m.away.goalsFor += m.awayScore;
        m.home.goalsAgainst += m.awayScore;
        m.away.goalsAgainst += m.homeScore;

        if (m.homeScore > m.awayScore) {
            m.home.wins++;
            m.away.losses++;
            m.home.points += 3;
        } else if (m.homeScore < m.awayScore) {
            m.away.wins++;
            m.home.losses++;
            m.away.points += 3;
        } else {
            m.home.draws++;
            m.away.draws++;
            m.home.points += 1;
            m.away.points += 1;
        }
    }

    void postMatchRecoveryAndTraining() {
        for (Team t : teams) {
            t.recoverPlayers();
            t.regenerateMorale();
            t.simpleTrainingCycle();
        }
    }

    List<Team> getStandings() {
        List<Team> copy = new ArrayList<>(teams);
        copy.sort(Comparator.comparingInt((Team t) -> t.points).reversed()
                .thenComparingInt(t -> t.goalsFor - t.goalsAgainst).reversed());
        return copy;
    }
}

class TeamFactory {
    private static final String[] LASTNAMES = {"Smith","Garcia","Johnson","Kumar","Ivanov","Chen","Silva","Müller","Rossi","López"};
    private static final String[] FIRSTNAMES = {"Alex","Sam","Jordan","Taylor","Chris","Morgan","Jamie","Robin","Casey","Avery"};

    static Team createTeam(String name, int size, Random rng) {
        Team t = new Team(name);
        for (int i = 0; i < size; i++) {
            String fname = FIRSTNAMES[rng.nextInt(FIRSTNAMES.length)];
            String lname = LASTNAMES[rng.nextInt(LASTNAMES.length)];
            Position pos = Position.randomPosition(rng);
            Player p = new Player(fname + " " + lname, pos, rng);
            t.addPlayer(p);
        }
        // assign captain and tactics randomly
        t.assignCaptainAndTactics(rng);
        return t;
    }
}

enum Position {
    GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD;

    static Position randomPosition(Random rng) {
        int r = rng.nextInt(100);
        if (r < 10) return GOALKEEPER;
        if (r < 45) return DEFENDER;
        if (r < 80) return MIDFIELDER;
        return FORWARD;
    }
}

class Player {
    public final String name;
    public final Position position;
    // core stats 0..100
    public int speed;
    public int shooting;
    public int passing;
    public int defense;
    public int stamina;
    public int morale; // 0..100
    public boolean injured = false;
    public boolean onField = true;
    public int fitness; // 0..100
    public int jerseyNumber;
    private final Random rng;

    Player(String name, Position pos, Random rng) {
        this.name = name;
        this.position = pos;
        this.rng = rng;
        // initialize stats with variation based on position
        int base = 60 + rng.nextInt(21) - 10; // base 50-80
        this.speed = clamp(base + (pos == Position.FORWARD ? 10 : pos == Position.DEFENDER ? -5 : 0) + rng.nextInt(11)-5);
        this.shooting = clamp(base + (pos == Position.FORWARD ? 15 : 0) + rng.nextInt(11)-5);
        this.passing = clamp(base + (pos == Position.MIDFIELDER ? 10 : 0) + rng.nextInt(11)-5);
        this.defense = clamp(base + (pos == Position.DEFENDER ? 15 : 0) + rng.nextInt(11)-5);
        this.stamina = clamp(60 + rng.nextInt(31));
        this.morale = 60 + rng.nextInt(21);
        this.fitness = 80 + rng.nextInt(21);
        this.jerseyNumber = 1 + rng.nextInt(99);
    }

    private int clamp(int v) { return Math.max(1, Math.min(100, v)); }

    double effectiveRating() {
        // composite rating used by AI and match engine
        double posFactor = 1.0;
        switch (position) {
            case GOALKEEPER: posFactor = 0.95; break;
            case DEFENDER: posFactor = 1.0; break;
            case MIDFIELDER: posFactor = 1.05; break;
            case FORWARD: posFactor = 1.1; break;
        }
        double stats = (speed * 0.15 + shooting * 0.25 + passing * 0.2 + defense * 0.2 + stamina * 0.2);
        double fitnessFactor = fitness / 100.0;
        double moraleFactor = 0.9 + (morale / 100.0) * 0.2;
        double injuryPenalty = injured ? 0.6 : 1.0;
        return stats * posFactor * fitnessFactor * moraleFactor * injuryPenalty;
    }

    void applyFatigue(int amount) {
        fitness = Math.max(0, fitness - amount);
        if (fitness < 25 && rng.nextDouble() < 0.05) {
            // small chance to get injured when exhausted
            injured = true;
        }
    }

    void recover(int amount) {
        fitness = Math.min(100, fitness + amount);
        if (injured && rng.nextDouble() < 0.15) {
            // chance to heal
            injured = false;
        }
    }

    void improveTraining(int focus) {
        // focus 0..100
        int delta = (int) Math.ceil(focus / 40.0);
        // random stat improvement depending on position
        if (position == Position.FORWARD) {
            shooting = clamp(shooting + delta + rng.nextInt(3));
            speed = clamp(speed + rng.nextInt(2));
        } else if (position == Position.MIDFIELDER) {
            passing = clamp(passing + delta + rng.nextInt(3));
            stamina = clamp(stamina + rng.nextInt(2));
        } else if (position == Position.DEFENDER) {
            defense = clamp(defense + delta + rng.nextInt(3));
            stamina = clamp(stamina + rng.nextInt(2));
        } else {
            defense = clamp(defense + rng.nextInt(2));
            passing = clamp(passing + rng.nextInt(2));
        }
        moralityBoost(rng.nextInt(3));
    }

    private void moralityBoost(int i) {
    }

    void moraleBoost(int amount) { morale = Math.min(100, morale + amount); }

    public Player[] getAllPlayersOfTeam(Team attacker) {
        return null;
    }
}

class Team {
    public final String name;
    private final List<Player> roster = new ArrayList<>();
    public Tactic tactic = Tactic.BALANCED;
    public Player captain;
    public int substitutionsUsed = 0;

    // simple standings
    public int points = 0;
    public int wins = 0, draws = 0, losses = 0, played = 0;
    public int goalsFor = 0, goalsAgainst = 0;

    Team(String name) { this.name = name; }

    void addPlayer(Player p) { roster.add(p); }

    List<Player> getStartingPlayers() {
        // choose highest effectiveRating players who are fit and not injured
        return roster.stream()
                .filter(p -> !p.injured)
                .sorted(Comparator.comparingDouble(Player::effectiveRating).reversed())
                .limit(7)
                .peek(p -> p.onField = true)
                .collect(Collectors.toList());
    }

    List<Player> getSubstitutes() {
        return roster.stream().filter(p -> !p.onField).collect(Collectors.toList());
    }

    List<Player> getAllPlayers() { return roster; }

    void assignCaptainAndTactics(Random rng) {
        this.captain = roster.get(rng.nextInt(roster.size()));
        this.tactic = Tactic.values()[rng.nextInt(Tactic.values().length)];
    }

    void recoverPlayers() {
        for (Player p : roster) {
            p.recover(6 + (int)(Math.random() * 6));
            p.morale = Math.min(100, p.morale + 2);
        }
        substitutionsUsed = 0;
    }

    void regenerateMorale() {
        for (Player p : roster) p.morale = Math.min(100, p.morale + 3);
    }

    void simpleTrainingCycle() {
        for (Player p : roster) {
            p.improveTraining(50);
            p.morale = Math.min(100, p.morale + 1);
        }
    }

    boolean canSubstitute() {
        return substitutionsUsed < 3;
    }

    void useSubstitution() { substitutionsUsed++; }
}

enum Tactic {
    ATTACKING, BALANCED, DEFENSIVE;
}

class Match {
    public final Team home;
    public final Team away;
    public int homeScore = 0;
    public int awayScore = 0;
    Match(Team home, Team away) { this.home = home; this.away = away; }
}

/* ======================== AI MANAGER ======================== */
class AIManager {
    private final Random rng = new Random();

    void prepareTeamsForMatch(Team t) {
        // choose tactic based on opponent and morale
        Tactic chosen = chooseTactic(t);
        t.tactic = chosen;
        // set starting lineup (update onField flags)
        List<Player> starters = t.getStartingPlayers();
        for (Player p : t.getAllPlayers()) p.onField = false;
        for (Player p : starters) p.onField = true;
        // slightly vary morale
        for (Player p : t.getAllPlayers()) p.morale = Math.min(100, p.morale + rng.nextInt(5));
    }

    private Tactic chooseTactic(Team t) {
        // naive: if captain morale high -> attack, else balance; dependent on avg stamina
        double avgFitness = t.getAllPlayers().stream().mapToInt(p -> p.fitness).average().orElse(70);
        double avgMorale = t.getAllPlayers().stream().mapToInt(p -> p.morale).average().orElse(65);
        if (avgMorale > 75 && avgFitness > 65) return Tactic.ATTACKING;
        if (avgFitness < 50) return Tactic.DEFENSIVE;
        return Tactic.BALANCED;
    }

    void assessInPlay(MatchState state) {
        // decide substitutions and tactical tweaks in-play
        Team team = state.actingTeam;
        // example: if losing and substitution available, make attacking change
        if (team.canSubstitute()) {
            if (state.isLosing(team) && rng.nextDouble() < 0.6) {
                makeAttackingSub(team);
            } else if (state.isWinning(team) && rng.nextDouble() < 0.25) {
                makeDefensiveSub(team);
            } else if (rng.nextDouble() < 0.02) {
                // random change
                tweakTactic(team);
            }
        }
    }

    private void makeAttackingSub(Team t) {
        Optional<Player> subOn = t.getAllPlayers().stream().filter(p -> !p.onField && !p.injured).max(Comparator.comparingDouble(Player::effectiveRating));
        Optional<Player> subOff = t.getAllPlayers().stream().filter(p -> p.onField).min(Comparator.comparingDouble(Player::effectiveRating));
        if (subOn.isPresent() && subOff.isPresent()) {
            Player on = subOn.get();
            Player off = subOff.get();
            off.onField = false;
            on.onField = true;
            t.useSubstitution();
        }
    }

    private void makeDefensiveSub(Team t) {
        Optional<Player> subOn = t.getAllPlayers().stream().filter(p -> !p.onField && !p.injured).max(Comparator.comparingDouble(Player::effectiveRating));
        Optional<Player> subOff = t.getAllPlayers().stream().filter(p -> p.onField).min(Comparator.comparingDouble(Player::effectiveRating));
        if (subOn.isPresent() && subOff.isPresent()) {
            Player on = subOn.get();
            Player off = subOff.get();
            off.onField = false;
            on.onField = true;
            t.useSubstitution();
        }
    }

    private void tweakTactic(Team t) {
        // small random tweak to tactic
        Tactic[] vals = Tactic.values();
        t.tactic = vals[rng.nextInt(vals.length)];
    }
}

/* ======================== MATCH ENGINE ======================== */
class GameEngine {
    private final GameConfig config;
    private final Match match;
    private final Renderer renderer;
    private final AIManager aiManager;
    private final Random rng = new Random();

    GameEngine(GameConfig config, Match match, Renderer renderer, AIManager aiManager) {
        this.config = config;
        this.match = match;
        this.renderer = renderer;
        this.aiManager = aiManager;
    }

    void playMatch() throws InterruptedException {
        // Initialize state
        match.homeScore = 0;
        match.awayScore = 0;
        match.home.substitutionsUsed = 0;
        match.away.substitutionsUsed = 0;

        // create match state for continuous simulation
        MatchState state = new MatchState(match, config, renderer);

        // show starting lineups
        renderer.updateMatchInfo(match.home, match.away);

        int totalTicks = config.MATCH_DURATION_SECONDS * config.TICKS_PER_SECOND;
        int tickDelay = 1000 / config.TICKS_PER_SECOND;

        for (int tick = 0; tick <= totalTicks; tick++) {
            // each tick is a small time unit of the match
            // simulate a short slice of play
            simulateTick(state);
            renderer.setMatchState(state);
            Thread.sleep(tickDelay);
        }

        // final scoreboard
        match.homeScore = state.homeGoals;
        match.awayScore = state.awayGoals;
        renderer.displayStatus(String.format("Full time: %s %d - %d %s", match.home.name, match.homeScore, match.awayScore, match.away.name));
    }

    private void simulateTick(MatchState state) {
        // Determine which team has possession based on ratings and tactics
        Team attacking = decideAttackingTeam(state);
        Team defending = (attacking == match.home) ? match.away : match.home;

        state.actingTeam = attacking;
        aiManager.assessInPlay(state);

        // movement and fatigue
        for (Player p : attacking.getAllPlayers()) {
            if (p.onField) p.applyFatigue(1 + rng.nextInt(2));
        }
        for (Player p : defending.getAllPlayers()) {
            if (p.onField) p.applyFatigue(1 + rng.nextInt(2));
        }

        // chance of events: pass, shot, turnover
        double attackPower = teamAttackPower(attacking);
        double defensePower = teamDefensePower(defending);

        double eventRoll = rng.nextDouble();
        if (eventRoll < 0.12 + Math.max(0, (attackPower - defensePower) / 500.0)) {
            // scoring opportunity
            boolean scored = attemptShot(attacking, defending, attackPower, defensePower);
            if (scored) {
                if (attacking == match.home) state.homeGoals++;
                else state.awayGoals++;
                // morale changes and brief energy hits
                adjustAfterGoal(attacking, defending);
            }
        } else if (eventRoll < 0.45) {
            // pass success or turnover (affects possession location)
            // nothing major here - perhaps small morale/possession shift
            // no-op for now
        }

        // check injuries
        checkRandomInjuries(match.home);
        checkRandomInjuries(match.away);

        // apply occasional tactical shifts
        if (rng.nextDouble() < 0.01) {
            // small chance to change tactic mid-game
            attacking.tactic = Tactic.values()[rng.nextInt(Tactic.values().length)];
        }
    }

    private Team decideAttackingTeam(MatchState state) {
        double homePower = teamOverallPower(match.home);
        double awayPower = teamOverallPower(match.away);
        double homeChance = homePower / (homePower + awayPower + 0.0001);
        return rng.nextDouble() < homeChance ? match.home : match.away;
    }

    private double teamAttackPower(Team t) {
        double sum = t.getAllPlayers().stream().filter(p -> p.onField).mapToDouble(p -> p.effectiveRating()).sum();
        double tacticFactor = (t.tactic == Tactic.ATTACKING) ? 1.08 : (t.tactic == Tactic.DEFENSIVE ? 0.95 : 1.0);
        return sum * tacticFactor;
    }

    private double teamDefensePower(Team t) {
        double sum = t.getAllPlayers().stream().filter(p -> p.onField).mapToDouble(p -> p.effectiveRating()).sum();
        double tacticFactor = (t.tactic == Tactic.DEFENSIVE) ? 1.06 : (t.tactic == Tactic.ATTACKING ? 0.96 : 1.0);
        return sum * tacticFactor;
    }

    private double teamOverallPower(Team t) {
        return t.getAllPlayers().stream().filter(p -> p.onField).mapToDouble(Player::effectiveRating).sum();
    }

    private boolean attemptShot(Team attacker, Team defender, double attackPower, double defensePower) {
        double shotQuality = attackPower / (defensePower + 1.0);
        double baseChance = 0.025 * shotQuality; // small base chance per tick
        // bias by tactic
        if (attacker.tactic == Tactic.ATTACKING) baseChance *= 1.25;
        if (attacker.tactic == Tactic.DEFENSIVE) baseChance *= 0.85;

        // choose shooter
        List<Player> onfield = attacker.getAllPlayers().stream().filter(p -> p.onField).collect(Collectors.toList());
        Player shooter = onfield.get(rng.nextInt(onfield.size()));
        double shooterInfluence = (shooter.shooting / 100.0) * (shooter.effectiveRating() / 100.0);
        double keeperInfluence = defender.getAllPlayers().stream().filter(p -> p.position == Position.GOALKEEPER && p.onField).mapToDouble(Player::effectiveRating).findFirst().orElse(70);

        double finalChance = baseChance * (1.0 + shooterInfluence) / (1.0 + keeperInfluence/100.0);
        boolean scored = rng.nextDouble() < finalChance;
        // dynamic feedback
        if (scored) {
            // adjust morale and fitness
            for (Player p : attacker.getAllPlayers()) if (p.onField) p.morale = Math.min(100, p.morale + 4);
            for (Player p : defender.getAllPlayers()) if (p.onField) p.morale = Math.max(10, p.morale - 6);
        } else {
            // partial morale change
            if (rng.nextDouble() < 0.3) for (Player p : shooter.getAllPlayersOfTeam(attacker)) {
            }
        }
        return scored;
    }

    private void adjustAfterGoal(Team scoring, Team conceding) {
        // supporters and coach influence
        for (Player p : scoring.getAllPlayers()) p.morale = Math.min(100, p.morale + 3);
        for (Player p : conceding.getAllPlayers()) p.morale = Math.max(0, p.morale - 4);
        // small fitness hit
        for (Player p : scoring.getAllPlayers()) if (p.onField) p.applyFatigue(2);
        for (Player p : conceding.getAllPlayers()) if (p.onField) p.applyFatigue(3);
    }

    private void checkRandomInjuries(Team t) {
        for (Player p : t.getAllPlayers()) {
            if (p.onField && !p.injured) {
                // small chance depending on fitness
                if (Math.random() < Math.max(0.0005, (50 - p.fitness) / 2000.0)) {
                    p.injured = true;
                }
            }
        }
    }
}

/* Helper to get player's team list - placed here for compactness */
// Note: This method intentionally sits outside the Player class to avoid duplication of team references; we attach a small helper mapping via reflection-like approach.
// For simplicity, we extend Player functionality with a method below via utility mapping when teams are constructed.

/* ======================== MATCH STATE ======================== */
class MatchState {
    public final Match match;
    public final GameConfig config;
    private final Random rng = new Random();

    public int homeGoals = 0;
    public int awayGoals = 0;
    // For animation: ball position and possession
    public double ballX, ballY; // 0..1 normalized on field
    public Team actingTeam; // team currently with possession
    public Renderer renderer;

    MatchState(Match match, GameConfig config, Renderer renderer) {
        this.match = match;
        this.config = config;
        this.renderer = renderer;
        // random starting ball location
        this.ballX = 0.5 + (rng.nextDouble() - 0.5) * 0.2;
        this.ballY = 0.5 + (rng.nextDouble() - 0.5) * 0.2;
        this.actingTeam = rng.nextBoolean() ? match.home : match.away;
    }

    boolean isLosing(Team t) {
        if (t == match.home) return homeGoals < awayGoals;
        return awayGoals < homeGoals;
    }

    boolean isWinning(Team t) {
        if (t == match.home) return homeGoals > awayGoals;
        return awayGoals > homeGoals;
    }
}

/* ======================== RENDERER & UI (Swing) ======================== */
class Renderer {
    private final GameConfig config;
    private final League league;
    private final JPanel mainPanel;
    private final FieldPanel fieldPanel;
    private final JPanel controlPanel;
    private final DefaultListModel<String> teamsListModel = new DefaultListModel<>();
    private final JList<String> teamsList = new JList<>(teamsListModel);
    private final DefaultListModel<String> leaderboardModel = new DefaultListModel<>();
    private final JList<String> leaderboardList = new JList<>(leaderboardModel);
    private final JLabel statusLabel = new JLabel("Welcome to AI Sports Simulator");
    private final JTextArea matchInfoArea = new JTextArea(8, 20);

    private MatchState currentState;

    Renderer(GameConfig config, League league) {
        this.config = config;
        this.league = league;
        mainPanel = new JPanel(new BorderLayout());
        fieldPanel = new FieldPanel(config);
        mainPanel.add(fieldPanel, BorderLayout.CENTER);

        controlPanel = new JPanel();
        controlPanel.setPreferredSize(new Dimension(320, config.FIELD_HEIGHT));
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        buildControls();
    }

    JPanel getMainPanel() { return mainPanel; }
    JPanel getControlPanel() { return controlPanel; }

    void buildControls() {
        controlPanel.removeAll();
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());
        top.add(new JLabel("Teams"), BorderLayout.NORTH);
        teamsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane teamsScroll = new JScrollPane(teamsList);
        teamsScroll.setPreferredSize(new Dimension(280, 120));
        top.add(teamsScroll, BorderLayout.CENTER);
        controlPanel.add(top);

        JPanel mid = new JPanel();
        mid.setLayout(new BorderLayout());
        mid.add(new JLabel("Leaderboard"), BorderLayout.NORTH);
        leaderboardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane leaderScroll = new JScrollPane(leaderboardList);
        leaderScroll.setPreferredSize(new Dimension(280, 160));
        mid.add(leaderScroll, BorderLayout.CENTER);
        controlPanel.add(mid);

        JPanel info = new JPanel();
        info.setLayout(new BorderLayout());
        info.add(new JLabel("Match Info"), BorderLayout.NORTH);
        matchInfoArea.setEditable(false);
        JScrollPane infoScroll = new JScrollPane(matchInfoArea);
        infoScroll.setPreferredSize(new Dimension(280, 160));
        info.add(infoScroll, BorderLayout.CENTER);
        controlPanel.add(info);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BorderLayout());
        bottom.add(statusLabel, BorderLayout.CENTER);
        controlPanel.add(bottom);
    }

    void refreshTeamsList() {
        SwingUtilities.invokeLater(() -> {
            teamsListModel.clear();
            for (Team t : league.teams) teamsListModel.addElement(t.name);
        });
    }

    void refreshLeaderboard(List<Team> standings) {
        SwingUtilities.invokeLater(() -> {
            leaderboardModel.clear();
            int i = 1;
            for (Team t : standings) {
                leaderboardModel.addElement(String.format("%d. %s  Pts:%d  GF:%d GA:%d", i++, t.name, t.points, t.goalsFor, t.goalsAgainst));
            }
        });
    }

    void setMatchState(MatchState s) {
        this.currentState = s;
        fieldPanel.renderState(s);
        SwingUtilities.invokeLater(() -> {
            String info = String.format("%s %d - %d %s\nTactic: %s vs %s\n", s.match.home.name, s.homeGoals, s.awayGoals, s.match.away.name, s.match.home.tactic, s.match.away.tactic);
            matchInfoArea.setText(info + generatePlayerPanel(s));
            statusLabel.setText(String.format("Possession: %s", s.actingTeam == null ? "—" : s.actingTeam.name));
        });
    }

    void updateMatchInfo(Team home, Team away) {
        StringBuilder sb = new StringBuilder();
        sb.append("Starting lineups:\n");
        sb.append(home.name).append(" starters:\n");
        home.getStartingPlayers().forEach(p -> sb.append(String.format(" #%d %s (%s) F:%.0f\n", p.jerseyNumber, p.name, p.position, p.effectiveRating())));
        sb.append("\n");
        sb.append(away.name).append(" starters:\n");
        away.getStartingPlayers().forEach(p -> sb.append(String.format(" #%d %s (%s) F:%.0f\n", p.jerseyNumber, p.name, p.position, p.effectiveRating())));
        matchInfoArea.setText(sb.toString());
    }

    String generatePlayerPanel(MatchState s) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nHome squad:\n");
        for (Player p : s.match.home.getAllPlayers()) sb.append(String.format("%s %s %s F:%d Fit:%d Inj:%s\n", p.jerseyNumber, p.name, p.onField ? "(on)":"(off)", (int)p.effectiveRating(), p.fitness, p.injured));
        sb.append("\nAway squad:\n");
        for (Player p : s.match.away.getAllPlayers()) sb.append(String.format("%s %s %s F:%d Fit:%d Inj:%s\n", p.jerseyNumber, p.name, p.onField ? "(on)":"(off)", (int)p.effectiveRating(), p.fitness, p.injured));
        return sb.toString();
    }

    void displayStatus(String s) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(s));
    }
}

/* FieldPanel handles simple animation and drawing of players and ball */
class FieldPanel extends JPanel {
    private final GameConfig config;
    private MatchState state;

    FieldPanel(GameConfig config) {
        this.config = config;
        setPreferredSize(new Dimension(config.FIELD_WIDTH, config.FIELD_HEIGHT));
        setBackground(new Color(34, 139, 34));
        // tick-based repaint
        Timer t = new Timer(1000 / config.TICKS_PER_SECOND, e -> repaint());
        t.start();
    }

    void renderState(MatchState s) {
        this.state = s;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        drawField(g2);
        if (state != null) drawEntities(g2, state);
        g2.dispose();
    }

    private void drawField(Graphics2D g2) {
        // grass background (already set), draw lines
        g2.setColor(Color.white);
        int w = getWidth();
        int h = getHeight();
        // outer rectangle
        g2.drawRect(10, 10, w - 20, h - 20);
        // center line
        g2.drawLine(w / 2, 10, w / 2, h - 10);
        // center circle
        g2.drawOval(w/2 - 60, h/2 - 60, 120, 120);
        // goals
        g2.drawRect(5, h/2 - 50, 10, 100);
        g2.drawRect(w - 15, h/2 - 50, 10, 100);
    }

    private void drawEntities(Graphics2D g2, MatchState s) {
        int w = getWidth();
        int h = getHeight();

        // ball - for demo, ball position is animated toward acting team's side
        double bx = s.ballX * w;
        double by = s.ballY * h;

        // attempt to move ball slowly toward opponent goal
        if (s.actingTeam != null) {
            if (s.actingTeam == s.match.home) {
                s.ballX = Math.max(0.02, s.ballX - 0.002);
            } else {
                s.ballX = Math.min(0.98, s.ballX + 0.002);
            }
            bx = s.ballX * w;
            // slight vertical jitter
            s.ballY = Math.min(0.98, Math.max(0.02, s.ballY + (Math.random()-0.5)*0.01));
            by = s.ballY * h;
        }

        // draw ball
        g2.setColor(Color.white);
        Ellipse2D ball = new Ellipse2D.Double(bx - 6, by - 6, 12, 12);
        g2.fill(ball);

        // draw players on each team
        drawTeamPlayers(g2, s.match.home, true, w, h);
        drawTeamPlayers(g2, s.match.away, false, w, h);

        // overlay score
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
        g2.setColor(Color.white);
        String score = String.format("%s %d - %d %s", s.match.home.name, s.homeGoals, s.awayGoals, s.match.away.name);
        g2.drawString(score, 20, 30);
    }

    private void drawTeamPlayers(Graphics2D g2, Team team, boolean leftSide, int w, int h) {
        List<Player> onField = team.getAllPlayers().stream().filter(p -> p.onField).collect(Collectors.toList());
        int count = onField.size();
        for (int i = 0; i < count; i++) {
            Player p = onField.get(i);
            // generate a simple formation: spread vertically
            double fractionX = leftSide ? 0.2 + Math.random()*0.05 : 0.8 - Math.random()*0.05;
            double fractionY = 0.1 + (i / (double)Math.max(1, count - 1)) * 0.8 + (Math.random()-0.5)*0.06;
            int px = (int) (fractionX * w);
            int py = (int) (fractionY * h);
            if (p.position == Position.GOALKEEPER) {
                // place goalkeeper near goal line
                px = leftSide ? 30 : w - 30;
                py = h/2 + (i - count/2) * 8;
            }
            // color by team side
            if (leftSide) g2.setColor(new Color(0, 102, 204, 220)); else g2.setColor(new Color(204, 0, 0, 220));
            int size = 14 + (p.injured ? -4 : 0);
            g2.fillOval(px - size/2, py - size/2, size, size);
            // draw number
            g2.setColor(Color.white);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString(String.valueOf(p.jerseyNumber), px - 6, py + 4);
        }
    }
}

class PlayerUtils {
    static List<Player> getPlayersOfTeam(Player p, Team t) {
        return t.getAllPlayers();
    }
}

