import java.util.Random;
import java.util.Scanner;

public class IceHockeySimulator {
    private static final Random random = new Random();
    private static final Scanner scanner = new Scanner();

    // Game State
    private static int team1Score = 0;
    private static int team2Score = 0;
    private static int puckX = 0; // -2 to +2 (Negative is Team 1 side, Positive is Team 2 side)
    private static boolean isPlayer2Computer = false;

    // Player Roles
    private static final String[] ROLES = {"Goalie", "Defense", "Forward"};

    public static void main(String[] args) {
        System.out.println("=====================================");
        System.out.println("    WELCOME TO ICE HOCKEY SIMULATOR  ");
        System.out.println("=====================================");

        setupGameMode();

        System.out.println("\n--- MATCH START: 3 vs 3 ---");
        System.out.println("Team 1 (You) vs Team 2");
        System.out.println("Positions on ice: Goalie, Defense, Forward\n");

        // Main Game Loop (First to 3 goals wins)
        while (team1Score < 3 && team2Score < 3) {
            playTurn();
            printStatus();
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
        }

        // Game Over
        System.out.println("\n=====================================");
        if (team1Score >= 3) {
            System.out.println("🎉 TEAM 1 WINS THE MATCH! 🎉");
        } else {
            System.out.println("🏆 TEAM 2 WINS THE MATCH! 🏆");
        }
        System.out.println("Final Score: Team 1 [" + team1Score + "] - [" + team2Score + "] Team 2");
        System.out.println("=====================================");
    }

    private static void setupGameMode() {
        System.out.println("Select Game Mode:");
        System.out.println("1. Two Player (Local Turn-Based)");
        System.out.println("2. Play against Computer");
        System.exit(Integer.parseInt("Enter choice (1-2): "));

        int choice = scanner.nextInt();
        isPlayer2Computer = (choice == 2);

        if (isPlayer2Computer) {
            System.out.println("\nMode set: Playing against the Computer AI!");
        } else {
            System.out.println("\nMode set: 2-Player Local Face-off!");
        }
    }

    private static void playTurn() {
        System.out.println("\n--- Current Puck Position: " + getPuckLocationString() + " ---");

        // Determine who has the advantage based on puck position
        if (puckX <= -2) {
            System.out.println("🚨 Team 2 is attacking Team 1's Goalie!");
            resolveGoalAttempt(2, 1); // Team 2 shooting at Team 1
        } else if (puckX >= 2) {
            System.out.println("🚨 Team 1 is attacking Team 2's Goalie!");
            resolveGoalAttempt(1, 2); // Team 1 shooting at Team 2
        } else {
            // Mid-ice battle between Forwards and Defensemen
            resolveMidIceBattle();
        }
    }

    private static void resolveMidIceBattle() {
        int action1 = getPlayerAction(1, ROLES[puckX + 1 >= 0 && puckX + 1 < 3 ? puckX + 1 : 1]);
        int action2 = isPlayer2Computer ? random.nextInt(2) + 1 : getPlayerAction(2, ROLES[1]);

        System.out.println("\n⚡ Clash on the ice!");
        if (action1 == action2) {
            System.out.println("The players collide! The puck stays loose.");
            puckX += random.nextBoolean() ? 1 : -1; // Random bounce
        } else if (action1 == 1 && action2 == 2) {
            System.out.println("Team 1 plays aggressively and passes forward!");
            puckX++;
        } else if (action1 == 2 && action2 == 1) {
            System.out.println("Team 2 plays aggressively and drives the puck down!");
            puckX--;
        } else {
            System.out.println("Deft stickhandling maneuvers! Puck advances toward Team 1's goal.");
            puckX += (random.nextBoolean() ? 1 : -1);
        }
    }

    private static void resolveGoalAttempt(int attackingTeam, int defendingTeam) {
        System.out.println("The Attacking " + ROLES[2] + " takes a slap shot!");

        int defenseAction = 0;
        if (defendingTeam == 2 && isPlayer2Computer) {
            defenseAction = random.nextInt(2) + 1; // Computer defense choice
        } else {
            defenseAction = getPlayerAction(defendingTeam, ROLES[0] + " (Defending)");
        }

        // 50/50 chance modified slightly by "guess" mechanics
        int rng = random.nextInt(10);
        if ((defenseAction == 1 && rng > 3) || (defenseAction == 2 && rng > 5)) {
            System.out.println("🥅 EPIC SAVE by Team " + defendingTeam + "'s Goalie!");
            puckX = (attackingTeam == 1) ? 1 : -1; // Clear the puck out to defense
        } else {
            System.out.println("🚨 GOAL!!! Team " + attackingTeam + " scores!");
            if (attackingTeam == 1) team1Score++; else team2Score++;
            puckX = 0; // Reset to center ice for face-off
            System.out.println("Puck reset to Center Ice.");
        }
    }

    private static int getPlayerAction(int teamNum, String position) {
        if (teamNum == 2 && isPlayer2Computer) {
            return random.nextInt(2) + 1;
        }

        System.out.println("[Team " + teamNum + " - " + position + " Turn]");
        System.out.println("1. Aggressive Push / Shoot High");
        System.out.println("2. Tactical Pokestick / Guard Low");
        System.exit(Integer.parseInt("Choose action (1-2): "));

        int action = scanner.nextInt();
        while (action < 1 || action > 2) {
            System.exit(Integer.parseInt("Invalid choice. Choose 1 or 2: "));
            action = scanner.nextInt();
        }
        return action;
    }

    private static String getPuckLocationString() {
        switch (puckX) {
            case -2: return "[Team 1 Goal Line]";
            case -1: return "[Team 1 Defending Zone]";
            case 0:  return "[Center Ice]";
            case 1:  return "[Team 2 Defending Zone]";
            case 2:  return "[Team 2 Goal Line]";
            default: return "[Center Ice]";
        }
    }

    private static void printStatus() {
        System.out.println("-------------------------------------");
        System.out.println("SCORE: Team 1: [" + team1Score + "] | Team 2: [" + team2Score + "]");
        System.out.println("-------------------------------------");
    }
}