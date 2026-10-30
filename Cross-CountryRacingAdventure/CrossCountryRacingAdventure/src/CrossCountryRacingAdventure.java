import java.util.Random;
import java.util.Scanner;

public class CrossCountryRacingAdventure {

    private static final int TRACK_LENGTH = 50;
    private static final Scanner scanner = new Scanner(System.in);
    private static final Random random = new Random();

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   CROSS-COUNTRY RACING ADVENTURE");
        System.out.println("=========================================");
        System.out.println("1. Two Players");
        System.out.println("2. Play vs Computer");
        System.out.print("Choose game mode: ");

        int mode = readInt(1, 2);

        if (mode == 1) {
            playTwoPlayers();
        } else {
            playVsComputer();
        }
    }

    // ---------- Game Modes ----------

    private static void playTwoPlayers() {
        System.out.print("Enter Player 1 name: ");
        String p1 = scanner.nextLine().trim();
        if (p1.isEmpty()) p1 = "Player 1";

        System.out.print("Enter Player 2 name: ");
        String p2 = scanner.nextLine().trim();
        if (p2.isEmpty()) p2 = "Player 2";

        Racer r1 = new Racer(p1, false);
        Racer r2 = new Racer(p2, false);

        race(r1, r2, true);
    }

    private static void playVsComputer() {
        System.out.print("Enter your name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) name = "Player";

        Racer human = new Racer(name, false);
        Racer computer = new Racer("Computer", true);

        race(human, computer, false);
    }

    // ---------- Core Race Loop ----------

    private static void race(Racer a, Racer b, boolean bothHuman) {
        System.out.println("\n--- THE RACE BEGINS! Track length: " + TRACK_LENGTH + " units ---\n");

        int turn = 1;

        while (a.position < TRACK_LENGTH && b.position < TRACK_LENGTH) {
            System.out.println("========== TURN " + turn + " ==========");
            System.out.println("Track: " + drawTrack(a, b, bothHuman));

            // Player A move
            if (a.isComputer) {
                computerMove(a);
            } else {
                humanMove(a);
            }
            System.out.println(a.name + " is at position " + a.position);

            if (a.position >= TRACK_LENGTH) break;

            // Player B move
            if (b.isComputer) {
                computerMove(b);
            } else if (bothHuman) {
                humanMove(b);
            } else {
                humanMove(b); // human vs computer: B is the human
            }
            System.out.println(b.name + " is at position " + b.position);

            System.out.println();
            turn++;
        }

        announceWinner(a, b);
    }

    // ---------- Moves ----------

    private static void humanMove(Racer r) {
        System.out.println("\n" + r.name + ", choose your move:");
        System.out.println("  1. Sprint   (fast, but risk of stumble)");
        System.out.println("  2. Steady   (safe, moderate progress)");
        System.out.println("  3. Recover  (small progress, small chance of resting)");
        System.out.print("Choice: ");

        int choice = readInt(1, 3);

        switch (choice) {
            case 1 -> sprint(r);
            case 2 -> steady(r);
            case 3 -> recover(r);
        }
    }

    private static void computerMove(Racer r) {
        // Simple AI: choose based on position
        int remaining = TRACK_LENGTH - r.position;
        int decision = random.nextInt(100);

        if (remaining <= 10) {
            // Close to finish: sprint more often
            if (decision < 60) sprint(r);
            else if (decision < 90) steady(r);
            else recover(r);
        } else {
            if (decision < 35) sprint(r);
            else if (decision < 80) steady(r);
            else recover(r);
        }

        System.out.println(r.name + " decides to move...");
    }

    private static void sprint(Racer r) {
        int roll = random.nextInt(100);
        if (roll < 25) {
            System.out.println("  -> " + r.name + " stumbles! Only +1");
            r.position += 1;
        } else {
            int gain = 4 + random.nextInt(4); // 4-7
            System.out.println("  -> " + r.name + " sprints! +" + gain);
            r.position += gain;
        }
        capPosition(r);
    }

    private static void steady(Racer r) {
        int gain = 2 + random.nextInt(3); // 2-4
        System.out.println("  -> " + r.name + " keeps a steady pace. +" + gain);
        r.position += gain;
        capPosition(r);
    }

    private static void recover(Racer r) {
        int roll = random.nextInt(100);
        if (roll < 15) {
            System.out.println("  -> " + r.name + " rests and recovers energy. +0");
        } else {
            int gain = 1 + random.nextInt(3); // 1-3
            System.out.println("  -> " + r.name + " moves cautiously. +" + gain);
            r.position += gain;
        }
        capPosition(r);
    }

    private static void capPosition(Racer r) {
        if (r.position > TRACK_LENGTH) r.position = TRACK_LENGTH;
    }

    // ---------- Rendering ----------

    private static String drawTrack(Racer a, Racer b, boolean bothHuman) {
        char[] track = new char[TRACK_LENGTH + 1];
        for (int i = 0; i < track.length; i++) track[i] = '-';

        // Place B first, then A (so A shows if same spot)
        if (b.position < TRACK_LENGTH) track[b.position] = 'B';
        else track[TRACK_LENGTH] = 'B';

        if (a.position < TRACK_LENGTH) {
            if (track[a.position] == 'B') track[a.position] = 'X';
            else track[a.position] = 'A';
        } else {
            if (track[TRACK_LENGTH] == 'B') track[TRACK_LENGTH] = 'X';
            else track[TRACK_LENGTH] = 'A';
        }

        track[TRACK_LENGTH] = track[TRACK_LENGTH] == '-' ? '|' : track[TRACK_LENGTH];

        return "|" + new String(track) + "|";
    }

    private static void announceWinner(Racer a, Racer b) {
        System.out.println("\n********** RACE FINISHED **********");
        System.out.println("Final positions: " + a.name + " = " + a.position
                + ", " + b.name + " = " + b.position);

        if (a.position >= TRACK_LENGTH && b.position >= TRACK_LENGTH) {
            System.out.println("It's a tie! Both crossed the finish line!");
        } else if (a.position >= TRACK_LENGTH) {
            System.out.println(a.name + " WINS! 🏆");
        } else {
            System.out.println(b.name + " WINS! 🏆");
        }
    }

    // ---------- Helpers ----------

    private static int readInt(int min, int max) {
        while (true) {
            try {
                String line = scanner.nextLine().trim();
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) return value;
                System.out.print("Please enter a number between " + min + " and " + max + ": ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Try again: ");
            }
        }
    }

    // ---------- Racer Class ----------

    private static class Racer {
        String name;
        int position = 0;
        boolean isComputer;

        Racer(String name, boolean isComputer) {
            this.name = name;
            this.isComputer = isComputer;
        }
    }
}