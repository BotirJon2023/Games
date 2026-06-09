import java.util.*;

public class IceHockeySimulator {

    // ─────────────────────────────────────────────────────────────
    //  CONSTANTS
    // ─────────────────────────────────────────────────────────────
    static final int PERIODS       = 3;
    static final int PERIOD_LENGTH = 5;   // turns per period
    static final Random RNG        = new Random();

    // ─────────────────────────────────────────────────────────────
    //  PLAYER
    // ─────────────────────────────────────────────────────────────
    static class Player {
        String name;
        String role;          // Center / Left Wing / Right Wing
        int skating;          // 1-10
        int shooting;         // 1-10
        int defense;          // 1-10
        int stamina;          // current 1-100, drains during play
        int goals, assists;

        Player(String name, String role, int skating, int shooting, int defense) {
            this.name     = name;
            this.role     = role;
            this.skating  = skating;
            this.shooting = shooting;
            this.defense  = defense;
            this.stamina  = 100;
        }

        /** Effective shot power factoring in current stamina */
        double shotPower() {
            return shooting * (stamina / 100.0);
        }

        /** Effective skating speed factoring in current stamina */
        double skateSpeed() {
            return skating * (stamina / 100.0);
        }

        void drainStamina(int amount) {
            stamina = Math.max(10, stamina - amount);
        }

        void recoverStamina(int amount) {
            stamina = Math.min(100, stamina + amount);
        }

        @Override
        public String toString() {
            return String.format("%-18s [%s] SKT:%d SHT:%d DEF:%d STM:%3d  G:%-2d A:%-2d",
                    name, role, skating, shooting, defense, stamina, goals, assists);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  TEAM
    // ─────────────────────────────────────────────────────────────
    static class Team {
        String name;
        List<Player> players = new ArrayList<>();
        int score = 0;
        boolean isHuman;

        Team(String name, boolean isHuman) {
            this.name    = name;
            this.isHuman = isHuman;
        }

        void addPlayer(Player p) { players.add(p); }

        /** Returns the player currently holding the puck (highest skate speed) */
        Player puckCarrier() {
            return players.stream()
                    .max(Comparator.comparingDouble(Player::skateSpeed))
                    .orElse(players.get(0));
        }

        /** Returns best shooter on the team */
        Player bestShooter() {
            return players.stream()
                    .max(Comparator.comparingDouble(Player::shotPower))
                    .orElse(players.get(0));
        }

        /** Returns best defender */
        Player bestDefender() {
            return players.stream()
                    .max(Comparator.comparingInt(p -> p.defense))
                    .orElse(players.get(0));
        }

        /** Average defense rating of the team */
        double avgDefense() {
            return players.stream().mapToInt(p -> p.defense).average().orElse(5.0);
        }

        void printRoster() {
            System.out.println("\n  ┌─ " + name + " Roster ─────────────────────────────────────────────┐");
            for (Player p : players)
                System.out.println("  │  " + p);
            System.out.println("  └──────────────────────────────────────────────────────────────┘");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  GAME
    // ─────────────────────────────────────────────────────────────
    static class Game {
        Team home, away;
        Scanner sc;
        boolean twoPlayer;

        Game(Team home, Team away, boolean twoPlayer, Scanner sc) {
            this.home = home; this.away = away;
            this.twoPlayer = twoPlayer; this.sc = sc;
        }

        // ── Main game loop ──────────────────────────────────────
        void play() {
            System.out.println(banner("GAME START: " + home.name + "  vs  " + away.name));
            home.printRoster();
            away.printRoster();
            prompt("\n  Press ENTER to drop the puck...");

            for (int period = 1; period <= PERIODS; period++) {
                System.out.println(banner("  PERIOD " + period + " of " + PERIODS));
                playPeriod(period);
                printScore();
                if (period < PERIODS) {
                    recoverStamina(home);
                    recoverStamina(away);
                    prompt("  [Intermission] Press ENTER for next period...");
                }
            }

            // Overtime if tied
            if (home.score == away.score) {
                System.out.println(banner("  SUDDEN DEATH OVERTIME!"));
                playOvertimeTurn(home, away);
            }

            printFinalResult();
        }

        // ── Period ─────────────────────────────────────────────
        void playPeriod(int period) {
            for (int turn = 1; turn <= PERIOD_LENGTH; turn++) {
                System.out.printf("%n  ── Turn %d/%d ──%n", turn, PERIOD_LENGTH);
                // Alternate possession start; home gets odd turns
                boolean homeAttacks = (turn % 2 == 1);
                Team attacker = homeAttacks ? home : away;
                Team defender = homeAttacks ? away  : home;
                playTurn(attacker, defender, period);
            }
        }

        // ── Single Turn ─────────────────────────────────────────
        void playTurn(Team attacker, Team defender, int period) {
            Player carrier  = attacker.puckCarrier();
            Player shooter  = attacker.bestShooter();
            Player defBack  = defender.bestDefender();

            System.out.printf("  %s has the puck (%s, STM:%d)%n",
                    carrier.name, carrier.role, carrier.stamina);

            // Human or AI chooses action
            String action;
            if (attacker.isHuman) {
                action = humanChooseAction(attacker, carrier);
            } else {
                action = aiChooseAction(attacker, defender);
            }

            switch (action) {
                case "SHOOT"  -> resolveShot(shooter, defBack, attacker, defender);
                case "PASS"   -> resolvePass(carrier, shooter, defBack, attacker, defender);
                case "SKATE"  -> resolveSkate(carrier, defBack, attacker, defender);
                default       -> System.out.println("  [Invalid action — puck lost!]");
            }

            // Drain stamina each turn
            attacker.players.forEach(p -> p.drainStamina(RNG.nextInt(5) + 2));
            defender.players.forEach(p -> p.drainStamina(RNG.nextInt(3) + 1));
        }

        // ── Human Action Menu ───────────────────────────────────
        String humanChooseAction(Team team, Player carrier) {
            System.out.println("  Your move for " + team.name + ":");
            System.out.println("    [1] SHOOT  — attempt a direct shot on goal");
            System.out.println("    [2] PASS   — pass to best shooter for a cleaner shot");
            System.out.println("    [3] SKATE  — carry the puck closer, risky but boosts shot");
            System.out.print("  Enter choice (1/2/3): ");
            String input = sc.nextLine().trim();
            return switch (input) {
                case "1" -> "SHOOT";
                case "2" -> "PASS";
                case "3" -> "SKATE";
                default  -> { System.out.println("  [!] Invalid — defaulting to PASS"); yield "PASS"; }
            };
        }

        // ── AI Action Logic ─────────────────────────────────────
        String aiChooseAction(Team attacker, Team defender) {
            double defStrength = defender.avgDefense();
            Player shooter     = attacker.bestShooter();

            // AI decision matrix
            if (shooter.stamina > 70 && shooter.shooting >= 8) {
                System.out.println("  [AI] " + attacker.name + " fires immediately!");
                return "SHOOT";
            } else if (defStrength > 7) {
                System.out.println("  [AI] " + attacker.name + " sets up a passing play...");
                return "PASS";
            } else {
                System.out.println("  [AI] " + attacker.name + " rushes the puck!");
                return "SKATE";
            }
        }

        // ── Resolve: SHOOT ──────────────────────────────────────
        void resolveShot(Player shooter, Player defender, Team att, Team def) {
            double shotChance = shooter.shotPower() / (shooter.shotPower() + defender.defense);
            boolean goal = RNG.nextDouble() < shotChance;
            if (goal) {
                att.score++;
                shooter.goals++;
                System.out.printf("  🚨 GOAL!!! %s scores for %s!  (%s %d – %s %d)%n",
                        shooter.name, att.name, att.name, att.score, def.name, def.score);
            } else {
                System.out.printf("  🧤 Save! %s blocks %s's shot!%n", defender.name, shooter.name);
            }
            shooter.drainStamina(8);
        }

        // ── Resolve: PASS ───────────────────────────────────────
        void resolvePass(Player passer, Player receiver, Player defender, Team att, Team def) {
            // Pass can be intercepted
            double interceptChance = defender.defense / 80.0;
            if (RNG.nextDouble() < interceptChance) {
                System.out.printf("  ⚡ Intercept! %s steals the pass!%n", defender.name);
                return;
            }
            System.out.printf("  🏒 Pass: %s → %s%n", passer.name, receiver.name);
            passer.assists++;
            // Bonus shot modifier after clean pass
            double shotChance = (receiver.shotPower() * 1.25) / (receiver.shotPower() * 1.25 + defender.defense);
            boolean goal = RNG.nextDouble() < shotChance;
            if (goal) {
                att.score++;
                receiver.goals++;
                System.out.printf("  🚨 GOAL!!! %s finishes the play for %s!  (%s %d – %s %d)%n",
                        receiver.name, att.name, att.name, att.score, def.name, def.score);
            } else {
                System.out.printf("  🧤 Shot blocked! %s holds firm!%n", defender.name);
            }
            receiver.drainStamina(6);
        }

        // ── Resolve: SKATE ──────────────────────────────────────
        void resolveSkate(Player carrier, Player defender, Team att, Team def) {
            double breakawayChance = carrier.skateSpeed() / (carrier.skateSpeed() + defender.skating);
            if (RNG.nextDouble() < breakawayChance) {
                System.out.printf("  💨 BREAKAWAY! %s blows past %s!%n", carrier.name, defender.name);
                // Follow-up shot with bonus
                double shotChance = carrier.shotPower() * 1.4 / (carrier.shotPower() * 1.4 + 5);
                if (RNG.nextDouble() < shotChance) {
                    att.score++;
                    carrier.goals++;
                    System.out.printf("  🚨 GOAL!!! %s scores on the breakaway!  (%s %d – %s %d)%n",
                            carrier.name, att.name, att.name, att.score, def.name, def.score);
                } else {
                    System.out.println("  🧤 Denied! Goalie stretches for the save!");
                }
            } else {
                System.out.printf("  🛑 Body check! %s stops %s cold!%n", defender.name, carrier.name);
            }
            carrier.drainStamina(12);
        }

        // ── Overtime ────────────────────────────────────────────
        void playOvertimeTurn(Team a, Team b) {
            System.out.println("  First goal wins!\n");
            int round = 1;
            while (true) {
                System.out.println("  [OT Round " + round++ + "]");
                for (Team[] matchup : new Team[][]{{a, b}, {b, a}}) {
                    Team att = matchup[0], def = matchup[1];
                    Player shooter  = att.bestShooter();
                    Player defBack  = def.bestDefender();
                    double shotChance = shooter.shotPower() / (shooter.shotPower() + defBack.defense);
                    if (RNG.nextDouble() < shotChance) {
                        att.score++;
                        shooter.goals++;
                        System.out.printf("  🚨 OVERTIME WINNER! %s scores for %s!%n",
                                shooter.name, att.name);
                        return;
                    } else {
                        System.out.printf("  Save in OT — %s holds on!%n", def.name);
                    }
                }
            }
        }

        // ── Helpers ─────────────────────────────────────────────
        void printScore() {
            System.out.printf("%n  ═══ SCORE  %s %d  —  %d %s ═══%n%n",
                    home.name, home.score, away.score, away.name);
        }

        void printFinalResult() {
            System.out.println(banner("FINAL SCORE"));
            System.out.printf("  %-20s  %d%n", home.name, home.score);
            System.out.printf("  %-20s  %d%n%n", away.name, away.score);

            Team winner = home.score > away.score ? home : away;
            Team loser  = home.score > away.score ? away : home;
            System.out.println("  🏆  " + winner.name + " wins the game!");
            System.out.println();

            System.out.println("  ── Player Stats ──────────────────────────────────────────────");
            for (Team t : new Team[]{home, away}) {
                System.out.println("  " + t.name + ":");
                for (Player p : t.players)
                    System.out.printf("    %-18s  Goals: %-3d Assists: %-3d%n",
                            p.name, p.goals, p.assists);
            }
        }

        void recoverStamina(Team t) {
            t.players.forEach(p -> p.recoverStamina(20 + RNG.nextInt(10)));
        }

        void prompt(String msg) {
            System.out.print(msg);
            sc.nextLine();
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  ROSTER BUILDER
    // ─────────────────────────────────────────────────────────────

    static Team buildHumanTeam(String teamName, Scanner sc) {
        Team team = new Team(teamName, true);
        System.out.println("\n  BUILD YOUR TEAM: " + teamName);
        System.out.println("  You have 3 players. Each player needs a name.");
        System.out.println("  Stats are auto-distributed (you can rename them).\n");
        String[] roles = {"Center", "Left Wing", "Right Wing"};
        int[][] presets = {{9,7,6},{7,9,5},{6,8,8}};
        for (int i = 0; i < 3; i++) {
            System.out.print("  Enter name for your " + roles[i] + ": ");
            String name = sc.nextLine().trim();
            if (name.isEmpty()) name = "Player" + (i + 1);
            team.addPlayer(new Player(name, roles[i], presets[i][0], presets[i][1], presets[i][2]));
        }
        return team;
    }

    static Team buildCPUTeam(String name) {
        Team team = new Team(name, false);
        String[][] roster = {
                {"Blaze",  "Center",     "8","8","7"},
                {"Storm",  "Left Wing",  "9","7","6"},
                {"Frost",  "Right Wing", "7","9","5"},
        };
        for (String[] r : roster)
            team.addPlayer(new Player(r[0], r[1],
                    Integer.parseInt(r[2]), Integer.parseInt(r[3]), Integer.parseInt(r[4])));
        return team;
    }

    static Team buildPresetTeam(String name, boolean isHuman) {
        Team team = new Team(name, isHuman);
        team.addPlayer(new Player("Apex",    "Center",     8, 8, 7));
        team.addPlayer(new Player("Volkov",  "Left Wing",  9, 7, 6));
        team.addPlayer(new Player("Stratos", "Right Wing", 7, 9, 5));
        return team;
    }

    // ─────────────────────────────────────────────────────────────
    //  DISPLAY HELPERS
    // ─────────────────────────────────────────────────────────────
    static String banner(String text) {
        String bar = "═".repeat(64);
        return String.format("%n  ╔%s╗%n  ║  %-60s  ║%n  ╚%s╝%n", bar, text, bar);
    }

    // ─────────────────────────────────────────────────────────────
    //  MAIN
    // ─────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println(banner("  ICE HOCKEY SIMULATOR  —  3v3 Edition"));
        System.out.println("  Choose Game Mode:");
        System.out.println("    [1]  1-Player  (You vs Computer)");
        System.out.println("    [2]  2-Player  (Human vs Human)");
        System.out.print("  Enter choice (1 or 2): ");
        String mode = sc.nextLine().trim();

        Team home, away;

        if (mode.equals("2")) {
            // ── Two-Player Mode ──────────────────────────────────
            System.out.print("\n  Enter Team 1 name: ");
            String t1 = sc.nextLine().trim();
            if (t1.isEmpty()) t1 = "Team Alpha";

            System.out.print("  Enter Team 2 name: ");
            String t2 = sc.nextLine().trim();
            if (t2.isEmpty()) t2 = "Team Beta";

            home = buildHumanTeam(t1, sc);
            away = buildHumanTeam(t2, sc);
            new Game(home, away, true, sc).play();

        } else {
            // ── Single-Player (vs AI) ────────────────────────────
            System.out.print("\n  Enter your Team name: ");
            String t1 = sc.nextLine().trim();
            if (t1.isEmpty()) t1 = "The Aces";

            home = buildHumanTeam(t1, sc);
            away = buildCPUTeam("CPU Titans");
            new Game(home, away, false, sc).play();
        }

        sc.close();
    }
}