import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class UltimateFrisbeeConsoleGame {

    private static final int FIELD_WIDTH = 80; // Console columns
    private static final int FIELD_HEIGHT = 20; // Console rows
    private static final int ENDZONE_DEPTH = 5;

    // --- Game Entities ---
    static class Player {
        String name;
        int x, y; // Position on the field
        char symbol; // Character representation for console
        boolean hasDisc;
        boolean isControlled; // True if this is the player controlled by the user
        Team team;

        public Player(String name, int x, int y, char symbol, boolean isControlled, Team team) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.symbol = symbol;
            this.hasDisc = false;
            this.isControlled = isControlled;
            this.team = team;
        }

        public void move(int dx, int dy) {
            int newX = x + dx;
            int newY = y + dy;

            // Clamp to field boundaries
            this.x = Math.max(0, Math.min(newX, FIELD_WIDTH - 1));
            this.y = Math.max(0, Math.min(newY, FIELD_HEIGHT - 1));
        }

        public void takeDisc(Disc disc) {
            this.hasDisc = true;
            disc.setOwner(this);
            disc.setFlying(false);
            System.out.println(this.name + " takes possession of the disc.");
        }

        public void throwDisc(Disc disc, int targetX, int targetY) {
            if (this.hasDisc) {
                System.out.println(this.name + " throws the disc towards (" + targetX + ", " + targetY + ")");
                disc.setX(this.x); // Disc starts from player's current position
                disc.setY(this.y);
                disc.setTarget(targetX, targetY);
                disc.setFlying(true);
                disc.setOwner(this); // The thrower is the owner until caught or turnover
                this.hasDisc = false;
            } else {
                System.out.println(this.name + " doesn't have the disc to throw!");
            }
        }

        // Getters
        public int getX() { return x; }
        public int getY() { return y; }
        public char getSymbol() { return symbol; }
        public boolean hasDisc() { return hasDisc; }
        public boolean isControlled() { return isControlled; }
        public String getName() { return name; }
        public Team getTeam() { return team; }
        public void setHasDisc(boolean hasDisc) { this.hasDisc = hasDisc; } // For game reset

        public void setX(int teamAStart) {
        }

        public void setY(int i) {
        }
    }

    static class Disc {
        int x, y;
        int targetX, targetY; // For simulated flight
        boolean isFlying;
        Player owner; // The player who last threw it, or currently has it

        public Disc(int x, int y) {
            this.x = x;
            this.y = y;
            this.isFlying = false;
            this.owner = null;
        }

        public void setTarget(int tx, int ty) {
            this.targetX = tx;
            this.targetY = ty;
        }

        public void update() {
            if (isFlying) {
                // Simple linear movement towards target
                if (x != targetX) {
                    x += (targetX > x) ? 1 : -1;
                }
                if (y != targetY) {
                    y += (targetY > y) ? 1 : -1;
                }

                // If disc reached target (or very close)
                if (Math.abs(x - targetX) <= 1 && Math.abs(y - targetY) <= 1) {
                    isFlying = false;
                    System.out.println("Disc landed at (" + x + ", " + y + ")");
                }
            } else if (owner != null && owner.hasDisc) {
                // If not flying but has an owner, stay with the owner
                this.x = owner.getX();
                this.y = owner.getY();
            }
        }

        // Getters and Setters
        public int getX() { return x; }
        public int getY() { return y; }
        public boolean isFlying() { return isFlying; }
        public void setFlying(boolean flying) { isFlying = flying; }
        public Player getOwner() { return owner; }
        public void setOwner(Player owner) { this.owner = owner; }

        public void setX(int x) {
        }

        public void setY(int y) {
        }
    }

    static class Team {
        String name;
        List<Player> players;
        int score;

        public Team(String name) {
            this.name = name;
            this.players = new ArrayList<>();
            this.score = 0;
        }

        public void addPlayer(Player player) {
            this.players.add(player);
        }

        public String getName() { return name; }
        public List<Player> getPlayers() { return players; }
        public int getScore() { return score; }
        public void addScore(int points) { this.score += points; }
    }

    // --- Game State Variables ---
    private static char[][] field;
    private static Player playerControlled;
    private static Disc disc;
    private static Team teamA;
    private static Team teamB;
    private static boolean gameOver = false;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        initializeGame();
        gameLoop();
        System.out.println("Game Over! Final Score - " + teamA.getName() + ": " + teamA.getScore() + " | " + teamB.getName() + ": " + teamB.getScore());
        scanner.close();
    }

    private static void initializeGame() {
        field = new char[FIELD_HEIGHT][FIELD_WIDTH];

        teamA = new Team("Team A");
        teamB = new Team("Team B");

        // Player 1 (Controlled)
        playerControlled = new Player("P1", 10, FIELD_HEIGHT / 2, 'P', true, teamA);
        teamA.addPlayer(playerControlled);
        // AI Player 1 (Team A)
        teamA.addPlayer(new Player("A1", 5, FIELD_HEIGHT / 2 - 3, 'A', false, teamA));

        // Opponent Player 1 (Team B)
        teamB.addPlayer(new Player("O1", FIELD_WIDTH - 15, FIELD_HEIGHT / 2, 'O', false, teamB));
        // Opponent Player 2 (Team B)
        teamB.addPlayer(new Player("O2", FIELD_WIDTH - 10, FIELD_HEIGHT / 2 + 2, 'X', false, teamB));


        disc = new Disc(playerControlled.getX(), playerControlled.getY());
        playerControlled.takeDisc(disc); // Controlled player starts with the disc

        System.out.println("Ultimate Frisbee Console Game");
        System.out.println("Commands: W, A, S, D (move), T (throw), Q (quit)");
        System.out.println("---------------------------------");
    }

    private static void gameLoop() {
        while (!gameOver) {
            drawField();
            processInput();
            updateGame();
            checkGameConditions();

            // Small delay to make it readable
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Game interrupted.");
            }
        }
    }

    private static void drawField() {
        // Clear field
        for (int i = 0; i < FIELD_HEIGHT; i++) {
            for (int j = 0; j < FIELD_WIDTH; j++) {
                field[i][j] = ' ';
            }
        }

        // Draw boundaries
        for (int i = 0; i < FIELD_WIDTH; i++) {
            field[0][i] = '-'; // Top border
            field[FIELD_HEIGHT - 1][i] = '-'; // Bottom border
        }
        for (int i = 0; i < FIELD_HEIGHT; i++) {
            field[i][0] = '|'; // Left border
            field[i][FIELD_WIDTH - 1] = '|'; // Right border
        }

        // Draw endzones
        for (int i = 0; i < FIELD_HEIGHT; i++) {
            for (int j = 0; j < ENDZONE_DEPTH; j++) {
                field[i][j] = '#'; // Left endzone
                field[i][FIELD_WIDTH - 1 - j] = '#'; // Right endzone
            }
        }

        // Draw players
        for (Player p : teamA.getPlayers()) {
            if (p.x >= 0 && p.x < FIELD_WIDTH && p.y >= 0 && p.y < FIELD_HEIGHT) {
                field[p.y][p.x] = p.getSymbol();
            }
        }
        for (Player p : teamB.getPlayers()) {
            if (p.x >= 0 && p.x < FIELD_WIDTH && p.y >= 0 && p.y < FIELD_HEIGHT) {
                field[p.y][p.x] = p.getSymbol();
            }
        }

        // Draw disc (if flying or not held by a player shown on field)
        if (disc.isFlying || (disc.owner == null || !disc.owner.hasDisc)) {
            if (disc.x >= 0 && disc.x < FIELD_WIDTH && disc.y >= 0 && disc.y < FIELD_HEIGHT) {
                field[disc.y][disc.x] = '*';
            }
        }


        // Print field
        for (int i = 0; i < FIELD_HEIGHT; i++) {
            for (int j = 0; j < FIELD_WIDTH; j++) {
                System.out.print(field[i][j]);
            }
            System.out.println();
        }

        // Print scores
        System.out.println("Score: " + teamA.getName() + " " + teamA.getScore() + " | " + teamB.getName() + " " + teamB.getScore());
        if (playerControlled.hasDisc()) {
            System.out.println("You have the disc!");
        } else if (disc.isFlying) {
            System.out.println("Disc is flying!");
        } else if (disc.owner != null && !disc.owner.isControlled()) {
            System.out.println(disc.owner.getName() + " has the disc.");
        } else {
            System.out.println("Disc is loose!");
        }
    }

    private static void processInput() {
        System.out.print("Enter command: ");
        String command = scanner.next().toUpperCase();

        int moveX = 0, moveY = 0;
        switch (command) {
            case "W": moveY = -1; break;
            case "S": moveY = 1; break;
            case "A": moveX = -1; break;
            case "D": moveX = 1; break;
            case "T":
                if (playerControlled.hasDisc()) {
                    System.out.print("Enter target X for disc: ");
                    int targetX = scanner.nextInt();
                    System.out.print("Enter target Y for disc: ");
                    int targetY = scanner.nextInt();
                    playerControlled.throwDisc(disc, targetX, targetY);
                } else {
                    System.out.println("You don't have the disc!");
                }
                break;
            case "Q":
                gameOver = true;
                break;
            default:
                System.out.println("Invalid command.");
        }

        if (moveX != 0 || moveY != 0) {
            playerControlled.move(moveX, moveY);
        }
    }

    private static void updateGame() {
        disc.update(); // Update disc position

        // AI player movement (very basic)
        for (Player p : teamA.getPlayers()) {
            if (!p.isControlled()) {
                // Simple AI: if disc is flying, move towards it
                if (disc.isFlying) {
                    if (p.getX() < disc.getX()) p.move(1, 0); else if (p.getX() > disc.getX()) p.move(-1, 0);
                    if (p.getY() < disc.getY()) p.move(0, 1); else if (p.getY() > disc.getY()) p.move(0, -1);
                }
            }
        }
        for (Player p : teamB.getPlayers()) {
            if (!p.isControlled()) {
                // Simple AI: if disc is flying, move towards it
                if (disc.isFlying) {
                    if (p.getX() < disc.getX()) p.move(1, 0); else if (p.getX() > disc.getX()) p.move(-1, 0);
                    if (p.getY() < disc.getY()) p.move(0, 1); else if (p.getY() > disc.getY()) p.move(0, -1);
                } else if (disc.owner != null && disc.owner.getTeam() == teamA) {
                    // If opponent has disc, move towards them to defend
                    if (p.getX() < disc.owner.getX()) p.move(1, 0); else if (p.getX() > disc.owner.getX()) p.move(-1, 0);
                    if (p.getY() < disc.owner.getY()) p.move(0, 1); else if (p.getY() > disc.owner.getY()) p.move(0, -1);
                }
            }
        }


        // Collision detection for disc and players
        if (!disc.isFlying && disc.owner == null) { // Disc is loose on the ground
            for (Player p : teamA.getPlayers()) {
                if (p.getX() == disc.getX() && p.getY() == disc.getY()) {
                    p.takeDisc(disc);
                    return; // Disc handled
                }
            }
            for (Player p : teamB.getPlayers()) {
                if (p.getX() == disc.getX() && p.getY() == disc.getY()) {
                    p.takeDisc(disc);
                    return; // Disc handled
                }
            }
        } else if (disc.isFlying) { // Disc is in air, check for catches
            for (Player p : teamA.getPlayers()) {
                if (p.getX() == disc.getX() && p.getY() == disc.getY()) {
                    // Check if player is not the one who just threw it (prevents self-catch on same spot)
                    if (disc.getOwner() == null || disc.getOwner() != p) {
                        p.takeDisc(disc);
                        return;
                    }
                }
            }
            for (Player p : teamB.getPlayers()) {
                if (p.getX() == disc.getX() && p.getY() == disc.getY()) {
                    if (disc.getOwner() == null || disc.getOwner() != p) {
                        p.takeDisc(disc);
                        return;
                    }
                }
            }
        }
    }

    private static void checkGameConditions() {
        // Scoring check
        if (!disc.isFlying && disc.owner != null) {
            Player currentHolder = disc.owner;
            Team currentTeam = currentHolder.getTeam();

            // Check if player is in opponent's endzone
            if (currentTeam == teamA && currentHolder.getX() >= FIELD_WIDTH - ENDZONE_DEPTH) {
                System.out.println("\n--- GOAL! " + currentTeam.getName() + " scores! ---\n");
                currentTeam.addScore(1);
                resetForNextPoint(teamB); // Team B receives pull
            } else if (currentTeam == teamB && currentHolder.getX() < ENDZONE_DEPTH) {
                System.out.println("\n--- GOAL! " + currentTeam.getName() + " scores! ---\n");
                currentTeam.addScore(1);
                resetForNextPoint(teamA); // Team A receives pull
            }
        }

        // Turnover check (disc hitting "ground" or going out of bounds without being caught)
        if (!disc.isFlying && disc.owner == null) {
            System.out.println("\n--- TURNOVER! Disc hit the ground. ---\n");
            // Find closest player of opposite team to pick up disc
            Player newPossessor = findClosestOpponent(disc.getX(), disc.getY(), disc.getOwner() == teamA.getPlayers().get(0) ? teamB : teamA);
            if (newPossessor != null) {
                newPossessor.takeDisc(disc);
            } else {
                System.out.println("No player found to pick up disc. Resetting to center.");
                // Fallback: place disc at center and make controlled player pick it up if they are in range
                disc.setX(FIELD_WIDTH / 2);
                disc.setY(FIELD_HEIGHT / 2);
            }
        }
    }

    private static void resetForNextPoint(Team pullingTeam) {
        // Reset player positions to starting areas
        int teamAStart = 10;
        int teamBStart = FIELD_WIDTH - 15;
        int playerYOffset = 0;

        for (Player p : teamA.getPlayers()) {
            p.setX(teamAStart);
            p.setY(FIELD_HEIGHT / 2 + playerYOffset);
            p.setHasDisc(false);
            playerYOffset += 3;
        }
        playerYOffset = 0;
        for (Player p : teamB.getPlayers()) {
            p.setX(teamBStart);
            p.setY(FIELD_HEIGHT / 2 + playerYOffset);
            p.setHasDisc(false);
            playerYOffset += 3;
        }

        // Assign disc for the pull
        if (!pullingTeam.getPlayers().isEmpty()) {
            Player puller = pullingTeam.getPlayers().get(0); // First player of pulling team
            puller.takeDisc(disc);
            disc.setX(puller.getX()); // Place disc with the puller
            disc.setY(puller.getY());
            System.out.println(pullingTeam.getName() + " will pull the disc.");
        } else {
            System.out.println("Error: No players in pulling team.");
            disc.setX(FIELD_WIDTH / 2);
            disc.setY(FIELD_HEIGHT / 2);
            disc.setOwner(null);
            disc.setFlying(false);
        }
    }

    private static Player findClosestOpponent(int discX, int discY, Team receivingTeam) {
        Player closest = null;
        double minDist = Double.MAX_VALUE;

        for (Player p : receivingTeam.getPlayers()) {
            double dist = Math.sqrt(Math.pow(p.getX() - discX, 2) + Math.pow(p.getY() - discY, 2));
            if (dist < minDist) {
                minDist = dist;
                closest = p;
            }
        }
        return closest;
    }
}