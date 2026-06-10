import java.util.*;

public class IceHockeySimulator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== ICE HOCKEY SIMULATOR ===");
        System.out.print("Choose mode (1 = Player vs Player, 2 = Player vs Computer): ");
        int mode = scanner.nextInt();

        Game game = new Game(mode);
        game.start();
    }
}

class Game {
    private final Rink rink;
    private final Team teamA;
    private final Team teamB;
    private final int mode; // 1 = PvP, 2 = PvC
    private boolean gameOver = false;
    private int period = 1;

    public Game(int mode) {
        this.mode = mode;
        this.rink = new Rink();
        this.teamA = new Team("Team A (You)", true);
        this.teamB = new Team("Team B", mode == 1);
    }

    public void start() {
        System.out.println("Game Started! First to 3 goals wins.");
        while (!gameOver && period <= 3) {
            playPeriod();
            period++;
        }
        System.out.println("\n=== GAME OVER ===");
        System.out.println(teamA.name + ": " + teamA.goals + " goals");
        System.out.println(teamB.name + ": " + teamB.goals + " goals");
    }

    private void playPeriod() {
        System.out.println("\n=== PERIOD " + period + " ===");
        rink.resetPuck();

        while (true) {
            rink.display();
            System.out.println(teamA.name + " " + teamA.goals + " - " + teamB.goals + " " + teamB.name);

            if (checkGoal()) break;

            Team currentTeam = (rink.currentPossession == 'A') ? teamA : teamB;
            boolean isHuman = currentTeam.isHuman;

            System.out.println("\n" + currentTeam.name + "'s turn (" +
                    (rink.currentPossession == 'A' ? "Blue" : "Red") + ")");

            Player activePlayer = currentTeam.getActivePlayer();
            System.out.println("Controlling: " + activePlayer.name);

            if (isHuman) {
                humanTurn(activePlayer);
            } else {
                computerTurn(activePlayer);
            }

            if (Math.random() < 0.1) checkTackle();
        }
    }

    private void humanTurn(Player p) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Actions: [1] Move [2] Pass [3] Shoot [4] Switch Player");
        int choice = sc.nextInt();

        switch (choice) {
            case 1 -> rink.movePlayer(p, getDirection());
            case 2 -> rink.pass(p);
            case 3 -> rink.shoot(p);
            case 4 -> rink.switchPlayer(p.getTeam());
        }
    }

    private String getDirection() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Direction (w/a/s/d): ");
        return sc.nextLine().toLowerCase();
    }

    private void computerTurn(Player p) {
        System.out.println("Computer is thinking...");
        try { Thread.sleep(600); } catch (Exception e) {}

        if (Math.random() < 0.6) {
            rink.shoot(p);
        } else {
            rink.pass(p);
        }
    }

    private boolean checkGoal() {
        if (rink.puckX == 0 && rink.puckY == 2) { // Team B goal
            teamA.goals++;
            System.out.println("GOAL!!! " + teamA.name + " scores!");
            return true;
        }
        if (rink.puckX == 9 && rink.puckY == 2) { // Team A goal
            teamB.goals++;
            System.out.println("GOAL!!! " + teamB.name + " scores!");
            return true;
        }
        return false;
    }

    private void checkTackle() {
        if (Math.random() < 0.3) {
            System.out.println("Big hit! Puck is loose!");
            rink.currentPossession = Math.random() > 0.5 ? 'A' : 'B';
        }
    }
}

class Rink {
    private final int WIDTH = 10;
    private final int HEIGHT = 5;
    char[][] grid = new char[HEIGHT][WIDTH];

    int puckX = 5, puckY = 2;
    char currentPossession = 'A';

    public Rink() {
        resetPuck();
    }

    public void resetPuck() {
        puckX = 5;
        puckY = 2;
        currentPossession = Math.random() > 0.5 ? 'A' : 'B';
    }

    public void display() {
        for (int i = 0; i < HEIGHT; i++) {
            for (int j = 0; j < WIDTH; j++) {
                if (i == 0 || i == HEIGHT-1 || j == 0 || j == WIDTH-1) {
                    System.out.print("#");
                } else if (i == puckY && j == puckX) {
                    System.out.print("P");
                } else {
                    System.out.print(".");
                }
            }
            System.out.println();
        }
    }

    public void movePlayer(Player p, String dir) {
        switch (dir) {
            case "w" -> puckY = Math.max(1, puckY - 1);
            case "s" -> puckY = Math.min(3, puckY + 1);
            case "a" -> puckX = Math.max(1, puckX - 1);
            case "d" -> puckX = Math.min(8, puckX + 1);
        }
        currentPossession = p.getTeam();
    }

    public void pass(Player p) {
        if (Math.random() < 0.8) {
            puckX += (currentPossession == 'A' ? 2 : -2);
            System.out.println(p.name + " makes a nice pass!");
        } else {
            System.out.println("Pass intercepted!");
            currentPossession = (currentPossession == 'A') ? 'B' : 'A';
        }
    }

    public void shoot(Player p) {
        if (Math.random() < 0.45) {
            System.out.println(p.name + " SCORES!!!");
            // Goal logic handled in Game class
        } else {
            System.out.println(p.name + " shoots... Saved by goalie!");
        }
        currentPossession = (currentPossession == 'A') ? 'B' : 'A';
    }

    public void switchPlayer(char team) {
        System.out.println("Switched player.");
    }
}

class Team {
    String name;
    boolean isHuman;
    int goals = 0;
    List<Player> players = new ArrayList<>();

    public Team(String name, boolean isHuman) {
        this.name = name;
        this.isHuman = isHuman;
        players.add(new Player("Forward", this));
        players.add(new Player("Defense", this));
        players.add(new Player("Center", this));
    }

    public Player getActivePlayer() {
        return players.get(0); // Simple: always control first player for demo
    }
}

class Player {
    String name;
    Team team;

    public Player(String name, Team team) {
        this.name = name;
        this.team = team;
    }

    public char getTeam() {
        return team.name.contains("A") ? 'A' : 'B';
    }
}