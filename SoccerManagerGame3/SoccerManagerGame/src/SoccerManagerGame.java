import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;

/**
 * SoccerManagerGame
 * - Single-file Java Swing app for a simple Soccer Manager with animated matches.
 * - Run with: javac SoccerManagerGame.java && java SoccerManagerGame
 * - No external assets required.
 *
 * Note:
 * - Simplified gameplay/ratings logic intended to demonstrate structure and animation.
 * - Extend with persistence, deeper tactics, training schedules, etc.
 */
public class SoccerManagerGame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final GameState gameState = new GameState();
    private final DashboardPanel dashboardPanel = new DashboardPanel();
    private final TeamPanel teamPanel = new TeamPanel();
    private final TransferPanel transferPanel = new TransferPanel();
    private final MatchPanel matchPanel = new MatchPanel();
    private final SeasonPanel seasonPanel = new SeasonPanel();
    private final JLabel statusBar = new JLabel("Welcome to Soccer Manager!");
    private final JButton btnDashboard = new JButton("Dashboard");
    private final JButton btnTeam = new JButton("Team");
    private final JButton btnTransfer = new JButton("Transfer Market");
    private final JButton btnSeason = new JButton("Season");
    private final JButton btnMatch = new JButton("Match Day");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SoccerManagerGame::new);
    }

    public SoccerManagerGame() {
        super("Soccer Manager Game (Demo)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);

        // Build initial game state
        gameState.init();

        // Set up UI
        JPanel nav = buildNav();
        cardPanel.add(dashboardPanel, "dashboard");
        cardPanel.add(teamPanel, "team");
        cardPanel.add(transferPanel, "transfer");
        cardPanel.add(seasonPanel, "season");
        cardPanel.add(matchPanel, "match");

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, nav, cardPanel);
        split.setDividerLocation(240);
        split.setResizeWeight(0);
        add(split, BorderLayout.CENTER);

        statusBar.setBorder(new EmptyBorder(4, 8, 4, 8));
        add(statusBar, BorderLayout.SOUTH);

        // Default route
        showView("dashboard");
        setVisible(true);
    }

    private JPanel buildNav() {
        JPanel nav = new JPanel();
        nav.setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Soccer Manager", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20));
        header.add(title, BorderLayout.CENTER);
        header.setBorder(new EmptyBorder(18, 8, 18, 8));

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));

        btnDashboard.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnTeam.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnTransfer.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSeason.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnMatch.setAlignmentX(Component.CENTER_ALIGNMENT);

        Dimension btnSize = new Dimension(200, 40);
        for (JButton b : new JButton[]{btnDashboard, btnTeam, btnTransfer, btnSeason, btnMatch}) {
            b.setMaximumSize(btnSize);
            b.setPreferredSize(btnSize);
            b.setFocusPainted(false);
            b.setMargin(new Insets(8, 12, 8, 12));
            b.addActionListener(this::handleNavClick);
            buttons.add(b);
            buttons.add(Box.createVerticalStrut(12));
        }

        JPanel bottom = new JPanel(new GridLayout(0,1, 4, 4));
        JButton btnSave = new JButton("Quick Tips");
        btnSave.addActionListener(e -> showTips());
        bottom.add(btnSave);
        JButton btnReset = new JButton("New Season");
        btnReset.addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(this, "Start a new season? This resets results.", "New Season", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                gameState.newSeason();
                seasonPanel.refresh();
                teamPanel.refresh();
                dashboardPanel.refresh();
                transferPanel.refresh();
                status("New season started!");
                showView("dashboard");
            }
        });
        bottom.add(btnReset);
        bottom.setBorder(new EmptyBorder(12,12,12,12));

        nav.add(header, BorderLayout.NORTH);
        JPanel inner = new JPanel(new BorderLayout());
        inner.add(buttons, BorderLayout.NORTH);
        nav.add(inner, BorderLayout.CENTER);
        nav.add(bottom, BorderLayout.SOUTH);

        return nav;
    }

    private void handleNavClick(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnDashboard) {
            dashboardPanel.refresh();
            showView("dashboard");
        } else if (src == btnTeam) {
            teamPanel.refresh();
            showView("team");
        } else if (src == btnTransfer) {
            transferPanel.refresh();
            showView("transfer");
        } else if (src == btnSeason) {
            seasonPanel.refresh();
            showView("season");
        } else if (src == btnMatch) {
            matchPanel.prepareForNextMatch();
            showView("match");
        }
    }

    private void showView(String key) {
        cardLayout.show(cardPanel, key);
    }

    private void status(String msg) {
        statusBar.setText(msg);
    }

    private void showTips() {
        String tips = ""
                + "- Use the Transfer Market to improve weaker positions.\n"
                + "- Keep an eye on player stamina and morale.\n"
                + "- Simulate matches to progress through the season.\n"
                + "- Experiment with formations for different strengths.\n"
                + "- Budget grows with wins; balance stars and depth.\n";
        JOptionPane.showMessageDialog(this, tips, "Quick Tips", JOptionPane.INFORMATION_MESSAGE);
    }

    // ----------------------- Game State ------------------------
    static class GameState {
        Team userTeam;
        Team aiTeamA;
        Team aiTeamB;
        List<Team> league;
        TransferMarket market;
        SeasonManager seasonManager;
        Random rng = new Random();

        void init() {
            // Generate teams and players
            league = new ArrayList<>();
            userTeam = Utils.generateTeam("Your FC", 75, 12, 65, 85);
            aiTeamA = Utils.generateTeam("Riverton United", 72, 12, 60, 82);
            aiTeamB = Utils.generateTeam("Harbor City", 78, 12, 70, 90);
            league.add(userTeam);
            league.add(aiTeamA);
            league.add(aiTeamB);

            market = new TransferMarket();
            market.populateRandomPlayers(35);

            seasonManager = new SeasonManager();
            seasonManager.setupSchedule(league);
        }

        void newSeason() {
            // Reset standings and stamina/morale
            for (Team t : league) {
                t.resetSeason();
            }
            // Refresh market
            market = new TransferMarket();
            market.populateRandomPlayers(35);
            // New schedule
            seasonManager = new SeasonManager();
            seasonManager.setupSchedule(league);
        }

        Team getNextOpponent(Team t) {
            return seasonManager.getNextOpponent(t);
        }
    }

    // ----------------------- Model ------------------------
    enum Position {
        GK, DEF, MID, FWD
    }

    static class Player {
        String name;
        Position position;
        int age;
        int overall;         // 40-99
        int stamina;         // 0-100
        int morale;          // 0-100
        int speed;           // 0-100
        int passing;         // 0-100
        int shooting;        // 0-100
        int defense;         // 0-100
        int goalkeeping;     // 0-100 (for GK)
        int value;           // transfer value k$
        boolean injured = false;

        public Player(String name, Position position, int age, int overall, int stamina,
                      int morale, int speed, int passing, int shooting, int defense, int goalkeeping, int value) {
            this.name = name;
            this.position = position;
            this.age = age;
            this.overall = overall;
            this.stamina = stamina;
            this.morale = morale;
            this.speed = speed;
            this.passing = passing;
            this.shooting = shooting;
            this.defense = defense;
            this.goalkeeping = goalkeeping;
            this.value = value;
        }

        void train() {
            int gain = Utils.clamp(1 + (int)(Math.random()*3), 1, 4);
            int statPick = (int)(Math.random() * 5);
            switch (statPick) {
                case 0 -> speed = Utils.clamp(speed + gain, 0, 100);
                case 1 -> passing = Utils.clamp(passing + gain, 0, 100);
                case 2 -> shooting = Utils.clamp(shooting + gain, 0, 100);
                case 3 -> defense = Utils.clamp(defense + gain, 0, 100);
                case 4 -> goalkeeping = Utils.clamp(goalkeeping + gain, 0, 100);
            }
            overall = Utils.clamp(overall + (gain >= 3 ? 1 : 0), 40, 99);
            stamina = Utils.clamp(stamina - (2 + gain), 0, 100);
            morale = Utils.clamp(morale + 1, 0, 100);
            updateValue();
        }

        void rest() {
            stamina = Utils.clamp(stamina + 10, 0, 100);
            morale = Utils.clamp(morale + 1, 0, 100);
        }

        void updateValue() {
            value = Math.max(50, (overall * 50) + (morale * 10) + (speed + passing + shooting + defense + goalkeeping));
            value += (int)((100 - age) * 5);
        }

        int attackScore() {
            int base = (speed + passing + shooting + morale) / 4;
            base += (overall / 2);
            return base;
        }

        int defendScore() {
            int base = (defense + stamina + morale) / 3;
            base += (overall / 2);
            return base;
        }

        int gkScore() {
            return (goalkeeping + defense + morale) / 3 + overall / 2;
        }

        @Override
        public String toString() {
            return name + " (" + position + ") OVR:" + overall + " STA:" + stamina + " MOR:" + morale + " $" + value + "k";
        }
    }

    static class Team {
        String name;
        java.util.List<Player> squad = new ArrayList<>();
        String formation = "4-3-3";
        int budget = 5000; // in thousands
        int wins = 0, draws = 0, losses = 0;
        int goalsFor = 0, goalsAgainst = 0;
        Color jersey = new Color(10, 100, 220);

        Team(String name) {
            this.name = name;
        }

        void resetSeason() {
            wins = draws = losses = goalsFor = goalsAgainst = 0;
            for (Player p : squad) {
                p.injured = false;
                p.morale = Utils.clamp(p.morale + 5, 0, 100);
                p.stamina = 90;
                p.updateValue();
            }
        }

        int points() {
            return wins*3 + draws;
        }

        int teamRating() {
            int sum = 0;
            int count = Math.min(11, squad.size());
            List<Player> xi = getStartingXI();
            for (Player p : xi) sum += p.overall;
            return count == 0 ? 0 : sum / count;
        }

        int attackRating() {
            int sum = 0, n=0;
            for (Player p : getStartingXI()) {
                if (p.position == Position.FWD || p.position == Position.MID) {
                    sum += p.attackScore();
                    n++;
                }
            }
            if (n == 0) return 50;
            return sum / n;
        }

        int defenseRating() {
            int sum = 0, n=0;
            for (Player p : getStartingXI()) {
                if (p.position == Position.DEF) { sum += p.defendScore(); n++; }
            }
            if (n == 0) return 50;
            return sum / n;
        }

        int gkRating() {
            Player gk = getGK();
            if (gk == null) return 50;
            return gk.gkScore();
        }

        Player getGK() {
            return squad.stream().filter(p -> p.position == Position.GK)
                    .sorted(Comparator.comparingInt(p->-p.overall)).findFirst().orElse(null);
        }

        List<Player> getStartingXI() {
            // Simple: GK + best 10 outfield by overall matching formation count
            int def = 4, mid = 3, fwd = 3;
            if (formation.equals("4-4-2")) { def = 4; mid = 4; fwd = 2; }
            if (formation.equals("3-5-2")) { def = 3; mid = 5; fwd = 2; }
            if (formation.equals("4-2-3-1")) { def = 4; mid = 5; fwd = 1; }

            List<Player> xi = new ArrayList<>();
            Player gk = getGK();
            if (gk != null) xi.add(gk);

            List<Player> defs = squad.stream().filter(p->p.position==Position.DEF)
                    .sorted(Comparator.comparingInt(p->-p.overall)).limit(def).collect(Collectors.toList());
            List<Player> mids = squad.stream().filter(p->p.position==Position.MID)
                    .sorted(Comparator.comparingInt(p->-p.overall)).limit(mid).collect(Collectors.toList());
            List<Player> fwds = squad.stream().filter(p->p.position==Position.FWD)
                    .sorted(Comparator.comparingInt(p->-p.overall)).limit(fwd).collect(Collectors.toList());

            xi.addAll(defs);
            xi.addAll(mids);
            xi.addAll(fwds);

            // Fill remaining with best remaining players if needed
            if (xi.size() < 11) {
                List<Player> rest = new ArrayList<>(squad);
                rest.removeAll(xi);
                rest.sort(Comparator.comparingInt(p->-p.overall));
                for (Player p : rest) {
                    if (xi.size()>=11) break;
                    xi.add(p);
                }
            }
            return xi.size() > 11 ? xi.subList(0,11) : xi;
        }

        void applyMatchResult(int gf, int ga) {
            goalsFor += gf;
            goalsAgainst += ga;
            if (gf > ga) wins++;
            else if (gf == ga) draws++;
            else losses++;

            // Adjust morale and stamina
            for (Player p : squad) {
                int delta = gf > ga ? 5 : (gf == ga ? 1 : -3);
                p.morale = Utils.clamp(p.morale + delta, 0, 100);
                p.stamina = Utils.clamp(p.stamina - 8, 0, 100);
                if (Math.random() < 0.03) p.injured = true;
            }

            // Budget reward/penalty
            if (gf > ga) budget += 400;
            else if (gf == ga) budget += 150;
            else budget += 50;
        }

        void healAndRest() {
            for (Player p : squad) {
                if (p.injured && Math.random() < 0.3) p.injured = false;
                p.rest();
            }
        }

        @Override
        public String toString() {
            return name + " | " + formation + " | Rating " + teamRating() + " | Budget $" + budget + "k";
        }
    }

    static class MatchResult {
        Team home, away;
        int goalsHome, goalsAway;
        java.util.List<String> events = new ArrayList<>();
    }

    static class TransferMarket {
        java.util.List<Player> available = new ArrayList<>();
        Random rng = new Random();

        void populateRandomPlayers(int n) {
            for (int i=0;i<n;i++) {
                Position pos = switch (rng.nextInt(4)) {
                    case 0 -> Position.GK;
                    case 1 -> Position.DEF;
                    case 2 -> Position.MID;
                    default -> Position.FWD;
                };
                Player p = Utils.randomPlayer(pos, 60 + rng.nextInt(30));
                available.add(p);
            }
        }

        Player buy(Team buyer, Player target) {
            if (!available.contains(target)) return null;
            int price = target.value;
            if (buyer.budget >= price) {
                buyer.budget -= price;
                buyer.squad.add(target);
                available.remove(target);
                return target;
            }
            return null;
        }

        boolean sell(Team seller, Player p) {
            if (!seller.squad.contains(p)) return false;
            seller.squad.remove(p);
            seller.budget += p.value;
            available.add(p);
            return true;
        }

        Player aiBuy(Team ai, Position need) {
            // AI tries to buy one player near need
            List<Player> candidates = available.stream()
                    .filter(p -> p.position == need && p.value <= ai.budget)
                    .sorted(Comparator.comparingInt(p->-p.overall))
                    .collect(Collectors.toList());
            if (candidates.isEmpty()) return null;
            Player best = candidates.get(0);
            ai.budget -= best.value;
            ai.squad.add(best);
            available.remove(best);
            return best;
        }
    }

    static class SeasonManager {
        static class Fixture {
            Team home, away;
            boolean played = false;
            MatchResult result;
        }
        java.util.List<Fixture> fixtures = new ArrayList<>();

        void setupSchedule(List<Team> league) {
            fixtures.clear();
            // Simple round robin (2 rounds per pair)
            for (int i=0;i<league.size();i++) {
                for (int j=i+1;j<league.size();j++) {
                    Team a = league.get(i);
                    Team b = league.get(j);
                    Fixture f1 = new Fixture();
                    f1.home = a; f1.away = b;
                    fixtures.add(f1);
                    Fixture f2 = new Fixture();
                    f2.home = b; f2.away = a;
                    fixtures.add(f2);
                }
            }
            Collections.shuffle(fixtures);
        }

        Team getNextOpponent(Team t) {
            for (Fixture f : fixtures) {
                if (!f.played && (f.home == t || f.away == t)) {
                    return f.home == t ? f.away : f.home;
                }
            }
            return null;
        }

        Fixture getNextFixture(Team t) {
            for (Fixture f : fixtures) {
                if (!f.played && (f.home == t || f.away == t)) return f;
            }
            return null;
        }

        List<Fixture> getTeamFixtures(Team t) {
            return fixtures.stream().filter(f -> f.home == t || f.away == t).collect(Collectors.toList());
        }
    }

    // ----------------------- Panels ------------------------
    class DashboardPanel extends JPanel {
        private final JLabel lblTeam = new JLabel();
        private final JTextArea txtStats = new JTextArea(12, 40);
        private final JButton btnTrainAll = new JButton("Train All (Light)");
        private final JButton btnRestAll = new JButton("Rest All");
        private final JButton btnNextOpponent = new JButton("Next Opponent");

        DashboardPanel() {
            setLayout(new BorderLayout());
            JPanel header = new JPanel(new GridLayout(0,1));
            lblTeam.setFont(lblTeam.getFont().deriveFont(Font.BOLD, 18));
            header.add(lblTeam);

            txtStats.setEditable(false);
            txtStats.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
            actions.add(btnTrainAll);
            actions.add(btnRestAll);
            actions.add(btnNextOpponent);

            btnTrainAll.addActionListener(e -> {
                for (Player p : gameState.userTeam.squad) {
                    if (!p.injured) p.train();
                }
                refresh();
                status("All players trained lightly.");
            });
            btnRestAll.addActionListener(e -> {
                gameState.userTeam.healAndRest();
                refresh();
                status("Squad rested.");
            });
            btnNextOpponent.addActionListener(e -> {
                Team opp = gameState.getNextOpponent(gameState.userTeam);
                if (opp != null) JOptionPane.showMessageDialog(this, "Next Opponent: " + opp.name + " (Rating " + opp.teamRating()+")");
                else JOptionPane.showMessageDialog(this, "No more fixtures.");
            });

            add(header, BorderLayout.NORTH);
            add(new JScrollPane(txtStats), BorderLayout.CENTER);
            add(actions, BorderLayout.SOUTH);
            setBorder(new EmptyBorder(12,12,12,12));
            refresh();
        }

        void refresh() {
            Team t = gameState.userTeam;
            lblTeam.setText(t.toString());
            StringBuilder sb = new StringBuilder();
            sb.append("League Teams\n");
            for (Team team : gameState.league) {
                sb.append(" - ").append(team.name).append(" | Pts ").append(team.points())
                        .append(" | W-D-L ").append(team.wins).append("-").append(team.draws).append("-").append(team.losses)
                        .append(" | GF/GA ").append(team.goalsFor).append("/").append(team.goalsAgainst)
                        .append(" | Rating ").append(team.teamRating()).append("\n");
            }
            sb.append("\nYour Squad (").append(t.squad.size()).append("):\n");
            for (Player p : t.squad) {
                sb.append(" - ").append(p).append(p.injured ? " [INJ]" : "").append("\n");
            }
            txtStats.setText(sb.toString());
        }
    }

    class TeamPanel extends JPanel {
        private final JComboBox<String> formationBox = new JComboBox<>(new String[]{"4-3-3","4-4-2","3-5-2","4-2-3-1"});
        private final DefaultListModel<Player> squadModel = new DefaultListModel<>();
        private final JList<Player> listSquad = new JList<>(squadModel);
        private final JButton btnTrain = new JButton("Train");
        private final JButton btnRest = new JButton("Rest");
        private final JButton btnSell = new JButton("Sell Player");
        private final JLabel lblBudget = new JLabel();
        private final JLabel lblTeamRating = new JLabel();

        TeamPanel() {
            setLayout(new BorderLayout(8,8));
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            top.add(new JLabel("Formation: "));
            top.add(formationBox);
            top.add(lblTeamRating);
            top.add(lblBudget);

            listSquad.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane sp = new JScrollPane(listSquad);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
            actions.add(btnTrain);
            actions.add(btnRest);
            actions.add(btnSell);

            btnTrain.addActionListener(e -> {
                Player p = listSquad.getSelectedValue();
                if (p == null) return;
                if (p.injured) { status("Player is injured."); return; }
                p.train();
                refresh();
                status("Trained " + p.name);
            });
            btnRest.addActionListener(e -> {
                Player p = listSquad.getSelectedValue();
                if (p == null) return;
                p.rest();
                refresh();
                status(p.name + " rested.");
            });
            btnSell.addActionListener(e -> {
                Player p = listSquad.getSelectedValue();
                if (p == null) return;
                int res = JOptionPane.showConfirmDialog(this, "Sell " + p.name + " for $" + p.value + "k ?", "Confirm Sell", JOptionPane.OK_CANCEL_OPTION);
                if (res == JOptionPane.OK_OPTION) {
                    boolean ok = gameState.market.sell(gameState.userTeam, p);
                    if (ok) {
                        status("Sold " + p.name);
                        refresh();
                    } else status("Could not sell.");
                }
            });

            formationBox.addActionListener(e -> {
                gameState.userTeam.formation = (String) formationBox.getSelectedItem();
                refresh();
            });

            add(top, BorderLayout.NORTH);
            add(sp, BorderLayout.CENTER);
            add(actions, BorderLayout.SOUTH);
            setBorder(new EmptyBorder(12,12,12,12));
            refresh();
        }

        void refresh() {
            squadModel.clear();
            for (Player p : gameState.userTeam.squad) squadModel.addElement(p);
            lblBudget.setText(" | Budget $" + gameState.userTeam.budget + "k");
            lblTeamRating.setText("Team Rating " + gameState.userTeam.teamRating());
            formationBox.setSelectedItem(gameState.userTeam.formation);
        }
    }

    class TransferPanel extends JPanel {
        private final DefaultListModel<Player> marketModel = new DefaultListModel<>();
        private final JList<Player> listMarket = new JList<>(marketModel);
        private final JButton btnBuy = new JButton("Buy Player");
        private final JComboBox<String> filterPos = new JComboBox<>(new String[]{"All", "GK", "DEF", "MID", "FWD"});
        private final JButton btnAiAdvance = new JButton("Advance Day (AI Transfers)");

        TransferPanel() {
            setLayout(new BorderLayout(8,8));
            JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
            north.add(new JLabel("Filter:"));
            north.add(filterPos);
            north.add(btnAiAdvance);

            filterPos.addActionListener(e -> refresh());
            btnAiAdvance.addActionListener(e -> {
                // AI tries to buy one player for each AI team based on need
                aiTransfer(gameState.aiTeamA);
                aiTransfer(gameState.aiTeamB);
                refresh();
                status("AI performed transfer actions.");
            });

            add(north, BorderLayout.NORTH);
            listMarket.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane sp = new JScrollPane(listMarket);
            add(sp, BorderLayout.CENTER);

            JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
            south.add(btnBuy);
            btnBuy.addActionListener(e -> {
                Player p = listMarket.getSelectedValue();
                if (p == null) return;
                Player bought = gameState.market.buy(gameState.userTeam, p);
                if (bought != null) {
                    status("Bought " + p.name + " for $" + p.value + "k");
                    refresh();
                } else {
                    status("Not enough budget to buy " + p.name);
                }
            });
            add(south, BorderLayout.SOUTH);
            setBorder(new EmptyBorder(12,12,12,12));
            refresh();
        }

        void aiTransfer(Team ai) {
            // Find weakest position
            Map<Position, Integer> count = new EnumMap<>(Position.class);
            for (Position pos : Position.values()) count.put(pos, 0);
            for (Player p : ai.squad) count.put(p.position, count.get(p.position)+1);
            Position need = Position.FWD;
            int min = Integer.MAX_VALUE;
            for (Position pos : Position.values()) {
                int c = count.get(pos);
                if (c < min) { min = c; need = pos; }
            }
            gameState.market.aiBuy(ai, need);
        }

        void refresh() {
            marketModel.clear();
            String f = (String) filterPos.getSelectedItem();
            for (Player p : gameState.market.available) {
                if (f.equals("All") || p.position.name().equals(f)) {
                    marketModel.addElement(p);
                }
            }
        }
    }

    class SeasonPanel extends JPanel {
        private final DefaultListModel<String> fixturesModel = new DefaultListModel<>();
        private final JList<String> listFixtures = new JList<>(fixturesModel);
        private final JButton btnSimNext = new JButton("Sim Next Match");

        SeasonPanel() {
            setLayout(new BorderLayout(8,8));
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            top.add(new JLabel("Season Fixtures for " + gameState.userTeam.name));
            add(top, BorderLayout.NORTH);

            listFixtures.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            add(new JScrollPane(listFixtures), BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
            bottom.add(btnSimNext);
            add(bottom, BorderLayout.SOUTH);
            setBorder(new EmptyBorder(12,12,12,12));

            btnSimNext.addActionListener(e -> {
                SeasonManager.Fixture f = gameState.seasonManager.getNextFixture(gameState.userTeam);
                if (f == null) {
                    status("No more fixtures.");
                    return;
                }
                // Launch match view prepared for this fixture
                matchPanel.startMatch(f.home, f.away);
                showView("match");
            });

            refresh();
        }

        void refresh() {
            fixturesModel.clear();
            List<SeasonManager.Fixture> fix = gameState.seasonManager.getTeamFixtures(gameState.userTeam);
            int idx = 1;
            for (SeasonManager.Fixture f : fix) {
                String status = f.played ? String.format("%d-%d", f.result.goalsHome, f.result.goalsAway) : "vs";
                String line = String.format("%2d) %-18s vs %-18s   [%s]",
                        idx++, f.home.name, f.away.name, status);
                fixturesModel.addElement(line);
            }
        }
    }

    // ----------------------- Match Animation Panel ------------------------
    class MatchPanel extends JPanel {

        private final JButton btnStart = new JButton("Start Match");
        private final JButton btnPause = new JButton("Pause");
        private final JButton btnResume = new JButton("Resume");
        private final JButton btnSkip = new JButton("Skip to Result");
        private final JLabel lblScore = new JLabel("Score: - ");
        private final JTextArea commentary = new JTextArea(8, 30);
        private final JScrollPane commentaryScroll = new JScrollPane(commentary);
        private final Timer aiTransferTick = new Timer();
        private java.util.List<Point2D> homePositions = new ArrayList<>();
        private java.util.List<Point2D> awayPositions = new ArrayList<>();
        private Point2D ballPos = new Point2D(0.5, 0.5);
        private Point2D ballTarget = new Point2D(0.5, 0.5);
        private int ballOwnerTeam = 0; // 0 home, 1 away
        private int ballOwnerIndex = 0;
        private final javax.swing.Timer tick = new javax.swing.Timer(16, e -> step());
        private boolean running = false;
        private long matchTimeMs = 0;
        private final long matchDurationMs = 90_000; // 90 seconds simulated
        private final Random rng = new Random();
        private Team home, away;
        private List<Player> homeXI, awayXI;
        private int goalsHome = 0, goalsAway = 0;
        private final java.util.List<String> events = new ArrayList<>();
        private SeasonManager.Fixture currentFixture = null;

        MatchPanel() {
            setLayout(new BorderLayout(4,4));
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
            controls.add(btnStart);
            controls.add(btnPause);
            controls.add(btnResume);
            controls.add(btnSkip);
            controls.add(lblScore);

            add(controls, BorderLayout.NORTH);

            commentary.setEditable(false);
            commentary.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                    new FieldCanvas(), commentaryScroll);
            split.setDividerLocation(550);
            add(split, BorderLayout.CENTER);
            setBorder(new EmptyBorder(8,8,8,8));

            btnStart.addActionListener(e -> {
                if (home == null || away == null) {
                    prepareForNextMatch();
                }
                if (home == null || away == null) {
                    JOptionPane.showMessageDialog(this, "No fixture available.");
                    return;
                }
                startMatch(home, away);
            });
            btnPause.addActionListener(e -> pause());
            btnResume.addActionListener(e -> resume());
            btnSkip.addActionListener(e -> skipToEnd());
        }

        void prepareForNextMatch() {
            SeasonManager.Fixture f = gameState.seasonManager.getNextFixture(gameState.userTeam);
            if (f != null) {
                home = f.home; away = f.away;
                currentFixture = f;
                lblScore.setText("Score: " + home.name + " 0 - 0 " + away.name);
                setCommentary("Ready: " + home.name + " vs " + away.name + "\nClick Start Match.");
            } else {
                home = away = null;
                setCommentary("No more fixtures.");
                lblScore.setText("Score: - ");
            }
        }

        void startMatch(Team h, Team a) {
            this.home = h;
            this.away = a;

            homeXI = new ArrayList<>(home.getStartingXI());
            awayXI = new ArrayList<>(away.getStartingXI());

            goalsHome = goalsAway = 0;
            events.clear();
            matchTimeMs = 0;

            // Initialize positions (normalized)
            homePositions = FormationLayouts.getFormationPositions(home.formation, true);
            awayPositions = FormationLayouts.getFormationPositions(away.formation, false);

            if (homePositions.size() < 11) homePositions = FormationLayouts.getFormationPositions("4-3-3", true);
            if (awayPositions.size() < 11) awayPositions = FormationLayouts.getFormationPositions("4-3-3", false);

            // Ball possession
            ballOwnerTeam = rng.nextBoolean() ? 0 : 1;
            ballOwnerIndex = rng.nextInt(11);
            ballPos = posOf(ballOwnerTeam, ballOwnerIndex);
            ballTarget = new Point2D(ballPos.x, ballPos.y);

            setCommentary("Kickoff! " + home.name + " vs " + away.name + "\n");
            running = true;
            tick.start();
            repaint();
        }

        void pause() {
            running = false;
        }

        void resume() {
            if (matchTimeMs < matchDurationMs) {
                running = true;
            }
        }

        void skipToEnd() {
            if (home == null || away == null) return;
            running = false;
            // Quick resolve remainder with a rough sim
            int remainingSeconds = (int)((matchDurationMs - matchTimeMs)/1000);
            int extraHome = (int)(rng.nextGaussian() * 0.4 + (home.attackRating() - away.defenseRating())/120.0 * remainingSeconds/10.0);
            int extraAway = (int)(rng.nextGaussian() * 0.4 + (away.attackRating() - home.defenseRating())/120.0 * remainingSeconds/10.0);
            extraHome = Math.max(0, extraHome);
            extraAway = Math.max(0, extraAway);
            goalsHome += extraHome;
            goalsAway += extraAway;
            matchTimeMs = matchDurationMs;
            endMatch();
            repaint();
        }

        void endMatch() {
            running = false;
            tick.stop();
            appendCommentary(String.format("FT: %s %d - %d %s\n", home.name, goalsHome, goalsAway, away.name));
            lblScore.setText("FT: " + home.name + " " + goalsHome + " - " + goalsAway + " " + away.name);

            // Record results to teams
            home.applyMatchResult(goalsHome, goalsAway);
            away.applyMatchResult(goalsAway, goalsHome);

            // Save fixture result
            if (currentFixture != null) {
                currentFixture.played = true;
                MatchResult r = new MatchResult();
                r.home = home; r.away = away;
                r.goalsHome = goalsHome; r.goalsAway = goalsAway;
                r.events.addAll(events);
                currentFixture.result = r;
            }

            // Heal and rest a bit after match
            home.healAndRest();
            away.healAndRest();

            // Refresh other panels
            dashboardPanel.refresh();
            teamPanel.refresh();
            seasonPanel.refresh();
        }

        void step() {
            if (!running) return;

            matchTimeMs += 16;
            if (matchTimeMs >= matchDurationMs) {
                endMatch();
                return;
            }

            // Move ball towards target
            double speed = 0.004; // normalized/sec (tune later)
            ballPos = moveToward(ballPos, ballTarget, speed);

            // Occasionally make decisions: pass, dribble, shoot
            if (ballPos.distance(ballTarget) < 0.005) {
                chooseNextAction();
            }

            // Occasionally adjust player target positions (simulate off-ball movement)
            if (rng.nextDouble() < 0.01) {
                jitterPlayers();
            }

            // Update score label
            lblScore.setText(minuteStr() + " " + home.name + " " + goalsHome + " - " + goalsAway + " " + away.name);

            repaint();
        }

        private void chooseNextAction() {
            // Current owner and teammates
            List<Player> myXI = ballOwnerTeam == 0 ? homeXI : awayXI;
            List<Point2D> myPos = ballOwnerTeam == 0 ? homePositions : awayPositions;
            Player owner = myXI.get(ballOwnerIndex);
            Point2D ownerPos = myPos.get(ballOwnerIndex);

            // Determine if near goal -> attempt shot
            boolean attackingLeftToRight = ballOwnerTeam == 0;
            double goalX = attackingLeftToRight ? 0.95 : 0.05;
            double distToGoal = Math.abs(ownerPos.x - goalX);

            int chanceToShoot = owner.shooting / 2;
            if (distToGoal < 0.15) chanceToShoot += 25;

            // Chance to pass
            int chanceToPass = owner.passing / 2 + 15;

            // Chance to dribble
            int chanceToDribble = owner.speed / 2;

            int total = chanceToShoot + chanceToPass + chanceToDribble;
            int roll = rng.nextInt(Math.max(1, total));
            if (roll < chanceToShoot) {
                attemptShot(owner, ownerPos, attackingLeftToRight);
            } else if (roll < chanceToShoot + chanceToPass) {
                attemptPass(myXI, myPos, owner);
            } else {
                attemptDribble(ownerPos, attackingLeftToRight);
            }
        }

        private void attemptShot(Player shooter, Point2D ownerPos, boolean LtoR) {
            // Compute scoring probability based on attack vs defense and GK
            Team atkTeam = LtoR ? home : away;
            Team defTeam = LtoR ? away : home;

            int attackRating = atkTeam.attackRating();
            int defenseRating = defTeam.defenseRating();
            int gkRating = defTeam.gkRating();
            int base = attackRating - (defenseRating/2) - (gkRating/3);
            base += shooter.shooting/2;
            double prob = 0.08 + base / 300.0;
            prob = Utils.clamp(prob, 0.02, 0.45);

            // Animate ball towards goal
            double targetX = LtoR ? 1.0 : 0.0;
            double targetY = 0.2 + rng.nextDouble()*0.6;
            ballTarget = new Point2D(targetX, targetY);

            // Outcome after reaching target
            if (rng.nextDouble() < prob) {
                // Goal!
                if (LtoR) goalsHome++; else goalsAway++;
                String event = minuteStr() + " GOAL! " + (LtoR ? home.name : away.name) + " - " + shooter.name;
                appendEvent(event);
                // New kickoff for other team
                ballOwnerTeam = LtoR ? 1 : 0;
                ballOwnerIndex = rng.nextInt(11);
                ballTarget = posOf(ballOwnerTeam, ballOwnerIndex);
            } else {
                // Save or miss -> possession switches
                String[] outcomes = new String[]{"saved by GK", "just wide", "off the post!", "deflected"};
                String outcome = outcomes[rng.nextInt(outcomes.length)];
                String event = minuteStr() + " Shot by " + shooter.name + " " + outcome;
                appendEvent(event);
                ballOwnerTeam = LtoR ? 1 : 0;
                ballOwnerIndex = rng.nextInt(11);
                ballTarget = posOf(ballOwnerTeam, ballOwnerIndex);
            }
        }

        private void attemptPass(List<Player> myXI, List<Point2D> myPos, Player owner) {
            // Pick a teammate near forward direction
            int bestIdx = -1;
            double bestScore = -1;
            boolean LtoR = (ballOwnerTeam == 0);
            Point2D ownerPos = myPos.get(ballOwnerIndex);
            for (int i=0;i<myXI.size();i++) {
                if (i == ballOwnerIndex) continue;
                Point2D p = myPos.get(i);
                // reward forward passes and proximity
                double forwardScore = LtoR ? (p.x - ownerPos.x) : (ownerPos.x - p.x);
                double distance = ownerPos.distance(p);
                double score = forwardScore*2 - distance;
                if (score > bestScore) { bestScore = score; bestIdx = i; }
            }
            if (bestIdx < 0) {
                attemptDribble(ownerPos, LtoR);
                return;
            }
            // Passing success
            Player targetPlayer = myXI.get(bestIdx);
            double passSkill = (owner.passing + targetPlayer.passing)/2.0;
            double interceptChance = 0.15 + (0.3 - passSkill/400.0);
            interceptChance = Utils.clamp(interceptChance, 0.05, 0.35);
            // Set ball target to teammate
            ballTarget = myPos.get(bestIdx);

            if (rng.nextDouble() < interceptChance) {
                // Intercepted by opponent
                ballOwnerTeam = ballOwnerTeam == 0 ? 1 : 0;
                ballOwnerIndex = rng.nextInt(11);
                appendEvent(minuteStr() + " Interception! Ball won by " + (ballOwnerTeam==0?home.name:away.name));
                ballTarget = posOf(ballOwnerTeam, ballOwnerIndex);
            } else {
                // Successful pass
                appendEvent(minuteStr() + " Pass from " + owner.name + " to " + targetPlayer.name);
                ballOwnerIndex = bestIdx;
                // Ball target already set
            }
        }

        private void attemptDribble(Point2D ownerPos, boolean LtoR) {
            // Move a bit forward with slight random angle
            double dx = (LtoR ? 0.10 : -0.10) + (rng.nextDouble()-0.5)*0.05;
            double dy = (rng.nextDouble()-0.5)*0.10;
            ballTarget = new Point2D(Utils.clamp(ownerPos.x + dx, 0.02, 0.98),
                    Utils.clamp(ownerPos.y + dy, 0.05, 0.95));
            if (rng.nextDouble() < 0.12) {
                // Tackled, possession loss
                appendEvent(minuteStr() + " Tackle! Possession lost.");
                ballOwnerTeam = ballOwnerTeam==0?1:0;
                ballOwnerIndex = rng.nextInt(11);
                ballTarget = posOf(ballOwnerTeam, ballOwnerIndex);
            } else {
                // Keep possession: move owner towards target
                // Slightly move owner position too
                List<Point2D> myPos = ballOwnerTeam == 0 ? homePositions : awayPositions;
                myPos.set(ballOwnerIndex, moveToward(ownerPos, ballTarget, 0.8));
            }
        }

        private void jitterPlayers() {
            // Small random movement for all except ball owner
            for (int i=0;i<11;i++) {
                if (i != ballOwnerIndex) {
                    Point2D p = homePositions.get(i);
                    homePositions.set(i, new Point2D(
                            Utils.clamp(p.x + (rng.nextDouble()-0.5)*0.02, 0.02, 0.98),
                            Utils.clamp(p.y + (rng.nextDouble()-0.5)*0.02, 0.05, 0.95))
                    );
                }
            }
            for (int i=0;i<11;i++) {
                if (ballOwnerTeam!=1 || i != ballOwnerIndex) {
                    Point2D p = awayPositions.get(i);
                    awayPositions.set(i, new Point2D(
                            Utils.clamp(p.x + (rng.nextDouble()-0.5)*0.02, 0.02, 0.98),
                            Utils.clamp(p.y + (rng.nextDouble()-0.5)*0.02, 0.05, 0.95))
                    );
                }
            }
        }

        private String minuteStr() {
            int minutes = (int)(matchTimeMs / 1000.0 * 1.0); // 1 sec = 1 min (scaled)
            return String.format("%02d'", minutes);
        }

        private void appendEvent(String s) {
            events.add(s);
            appendCommentary(s + "\n");
        }

        private void appendCommentary(String text) {
            commentary.append(text);
            SwingUtilities.invokeLater(() -> {
                JScrollBar v = commentaryScroll.getVerticalScrollBar();
                v.setValue(v.getMaximum());
            });
        }

        private void setCommentary(String text) {
            commentary.setText(text);
        }

        private Point2D posOf(int team, int idx) {
            Point2D p = (team==0?homePositions:awayPositions).get(idx);
            return new Point2D(p.x, p.y);
        }

        private Point2D moveToward(Point2D from, Point2D to, double speed) {
            double dx = to.x - from.x;
            double dy = to.y - from.y;
            double d = Math.sqrt(dx*dx + dy*dy);
            if (d < 1e-6) return new Point2D(to.x, to.y);
            double step = Math.min(speed, d);
            return new Point2D(from.x + dx/d*step, from.y + dy/d*step);
        }

        class FieldCanvas extends JPanel {
            FieldCanvas() {
                setBackground(new Color(22, 120, 22));
                setPreferredSize(new Dimension(1000, 520));
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Pitch lines
                paintPitch(g2, w, h);

                // Draw players
                if (home != null && away != null) {
                    // Convert normalized positions to pixels
                    int r = 12;
                    // Home team
                    g2.setColor(home.jersey);
                    for (int i=0;i<Math.min(11, homePositions.size());i++) {
                        Point2D p = homePositions.get(i);
                        int x = (int)(p.x * w);
                        int y = (int)(p.y * h);
                        g2.fillOval(x - r, y - r, r*2, r*2);
                        g2.setColor(Color.white);
                        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                        g2.drawString(numberFor(i), x-4, y+4);
                        g2.setColor(home.jersey);
                    }

                    // Away team
                    Color awayJersey = new Color(220, 60, 40);
                    g2.setColor(awayJersey);
                    for (int i=0;i<Math.min(11, awayPositions.size());i++) {
                        Point2D p = awayPositions.get(i);
                        int x = (int)(p.x * w);
                        int y = (int)(p.y * h);
                        g2.fillOval(x - r, y - r, r*2, r*2);
                        g2.setColor(Color.white);
                        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                        g2.drawString(numberFor(i), x-4, y+4);
                        g2.setColor(awayJersey);
                    }

                    // Ball
                    g2.setColor(Color.white);
                    int bx = (int)(ballPos.x * w);
                    int by = (int)(ballPos.y * h);
                    g2.fillOval(bx - 6, by - 6, 12, 12);
                    g2.setColor(Color.black);
                    g2.drawOval(bx - 6, by - 6, 12, 12);
                }
            }

            private void paintPitch(Graphics2D g2, int w, int h) {
                g2.setColor(new Color(44, 140, 44));
                // Center circle
                g2.setStroke(new BasicStroke(2f));
                int ccR = Math.min(w, h)/6;
                g2.drawOval(w/2 - ccR, h/2 - ccR, ccR*2, ccR*2);
                g2.drawLine(w/2, 0, w/2, h);
                // Boxes
                int boxW = (int)(w*0.16);
                int boxH = (int)(h*0.4);
                g2.drawRect(0, h/2 - boxH/2, boxW, boxH);
                g2.drawRect(w - boxW, h/2 - boxH/2, boxW, boxH);
                // Small box
                int sBoxW = (int)(w*0.06);
                int sBoxH = (int)(h*0.2);
                g2.drawRect(0, h/2 - sBoxH/2, sBoxW, sBoxH);
                g2.drawRect(w - sBoxW, h/2 - sBoxH/2, sBoxW, sBoxH);
                // Goals
                int goalW = (int)(w*0.01);
                int goalH = (int)(h*0.12);
                g2.setColor(new Color(230, 230, 230));
                g2.fillRect(-goalW, h/2 - goalH/2, goalW, goalH);
                g2.fillRect(w, h/2 - goalH/2, goalW, goalH);
                // Penalty spots and arcs
                g2.setColor(Color.white);
                g2.fillOval((int)(w*0.11)-3, h/2-3, 6, 6);
                g2.fillOval((int)(w*0.89)-3, h/2-3, 6, 6);
            }

            private String numberFor(int idx) {
                // Simple mapping GK=1, DEF=2-5, MID=6-8, FWD=9-11 (approx)
                if (idx == 0) return "1";
                if (idx <= 4) return "" + (idx+1);
                if (idx <= 7) return "" + (idx+1);
                return "" + (idx+1);
            }
        }
    }

    // ----------------------- Utilities & Layouts ------------------------
    static class Point2D {
        final double x, y;
        Point2D(double x, double y) { this.x = x; this.y = y; }
        double distance(Point2D o) { double dx = x-o.x, dy=y-o.y; return Math.sqrt(dx*dx+dy*dy); }
    }

    static class FormationLayouts {
        static List<Point2D> getFormationPositions(String formation, boolean home) {
            // Return 11 normalized positions. Home attacks left->right.
            // We'll place GK near their own goal, then defenders, mids, forwards.
            // Coordinates (x,y): x in [0..1], y in [0..1]
            boolean LtoR = home;
            double gkX = LtoR ? 0.05 : 0.95;
            List<Point2D> pos = new ArrayList<>();
            pos.add(new Point2D(gkX, 0.5)); // GK index 0

            if (formation == null) formation = "4-3-3";
            switch (formation) {
                case "4-4-2" -> {
                    double defX = LtoR ? 0.2 : 0.8;
                    double midX = LtoR ? 0.5 : 0.5;
                    double fwdX = LtoR ? 0.78 : 0.22;
                    pos.addAll(line(defX, new double[]{0.2,0.4,0.6,0.8}));
                    pos.addAll(line(midX, new double[]{0.15,0.35,0.65,0.85}));
                    pos.addAll(line(fwdX, new double[]{0.35,0.65}));
                }
                case "3-5-2" -> {
                    double defX = LtoR ? 0.25 : 0.75;
                    double midX = LtoR ? 0.5 : 0.5;
                    double fwdX = LtoR ? 0.80 : 0.20;
                    pos.addAll(line(defX, new double[]{0.3,0.5,0.7}));
                    pos.addAll(line(midX, new double[]{0.15,0.3,0.5,0.7,0.85}));
                    pos.addAll(line(fwdX, new double[]{0.4,0.6}));
                }
                case "4-2-3-1" -> {
                    double defX = LtoR ? 0.2 : 0.8;
                    double dmidX = LtoR ? 0.4 : 0.6;
                    double amidX = LtoR ? 0.65 : 0.35;
                    double fwdX = LtoR ? 0.85 : 0.15;
                    pos.addAll(line(defX, new double[]{0.2,0.4,0.6,0.8}));
                    pos.addAll(line(dmidX, new double[]{0.35,0.65}));
                    pos.addAll(line(amidX, new double[]{0.25,0.5,0.75}));
                    pos.add(new Point2D(fwdX, 0.5));
                }
                default -> { // "4-3-3"
                    double defX = LtoR ? 0.2 : 0.8;
                    double midX = LtoR ? 0.45 : 0.55;
                    double fwdX = LtoR ? 0.78 : 0.22;
                    pos.addAll(line(defX, new double[]{0.2,0.4,0.6,0.8}));
                    pos.addAll(line(midX, new double[]{0.3,0.5,0.7}));
                    pos.addAll(line(fwdX, new double[]{0.25,0.5,0.75}));
                }
            }
            while (pos.size() < 11) pos.add(new Point2D(LtoR?0.5:0.5, 0.5)); // pad
            return pos;
        }

        static List<Point2D> line(double x, double[] ys) {
            List<Point2D> res = new ArrayList<>();
            for (double y : ys) res.add(new Point2D(x, y));
            return res;
        }
    }

    static class Utils {
        static Random rng = new Random();
        static String[] firstNames = {
                "Alex","Ben","Carlos","Diego","Edu","Felix","Gabe","Hiro","Igor","Jack",
                "Kane","Leo","Marco","Nico","Oscar","Pavel","Quinn","Rico","Sergio","Taro",
                "Uli","Viktor","Will","Xavi","Yuri","Zane"
        };
        static String[] lastNames = {
                "Adams","Bennett","Castro","Diaz","Evans","Fischer","Gomez","Huang","Ivanov","Jones",
                "Kovac","Lopez","Muller","Nakamura","O'Neil","Perez","Quintana","Rossi","Silva","Tanaka",
                "Urbano","Vega","Williams","Xu","Young","Zimmer"
        };

        static Team generateTeam(String name, int base, int variance, int min, int max) {
            Team t = new Team(name);
            t.budget = 4000 + rng.nextInt(4000);
            t.jersey = new Color(rng.nextInt(60)+10, rng.nextInt(160), rng.nextInt(160)+60);
            // 18-22 players
            int count = 19 + rng.nextInt(4);
            // At least 2 GKs, 6 DEF, 6 MID, 4 FWD
            int gk = 2, def = 6, mid = 6, fwd = 4;
            while (gk-->0) t.squad.add(randomPlayer(Position.GK, clamp(base + rng.nextInt(variance*2)-variance, min, max)));
            while (def-->0) t.squad.add(randomPlayer(Position.DEF, clamp(base + rng.nextInt(variance*2)-variance, min, max)));
            while (mid-->0) t.squad.add(randomPlayer(Position.MID, clamp(base + rng.nextInt(variance*2)-variance, min, max)));
            while (fwd-->0) t.squad.add(randomPlayer(Position.FWD, clamp(base + rng.nextInt(variance*2)-variance, min, max)));
            while (t.squad.size() < count) {
                Position pos = Position.values()[rng.nextInt(Position.values().length)];
                t.squad.add(randomPlayer(pos, clamp(base + rng.nextInt(variance*2)-variance, min, max)));
            }
            return t;
        }

        static Player randomPlayer(Position pos, int overall) {
            String name = firstNames[rng.nextInt(firstNames.length)] + " " + lastNames[rng.nextInt(lastNames.length)];
            int age = 18 + rng.nextInt(17);
            int stamina = 70 + rng.nextInt(30);
            int morale = 60 + rng.nextInt(40);
            int speed = 40 + rng.nextInt(60);
            int passing = 40 + rng.nextInt(60);
            int shooting = 40 + rng.nextInt(60);
            int defense = 40 + rng.nextInt(60);
            int gk = pos == Position.GK ? (40 + rng.nextInt(60)) : rng.nextInt(20);
            Player p = new Player(name, pos, age, overall, stamina, morale, speed, passing, shooting, defense, gk, 0);
            p.updateValue();
            return p;
        }

        static int clamp(int v, int min, int max) {
            return Math.max(min, Math.min(max, v));
        }
        static double clamp(double v, double min, double max) {
            return Math.max(min, Math.min(max, v));
        }
    }
}