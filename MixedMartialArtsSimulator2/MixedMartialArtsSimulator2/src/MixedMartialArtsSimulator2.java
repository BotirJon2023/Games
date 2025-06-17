import java.util.*;
import java.util.concurrent.TimeUnit;

public class MixedMartialArtsSimulator2 {
    private static final Random random = new Random();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== MIXED MARTIAL ARTS SIMULATOR ===");
        System.out.println("Developed by DeepSeek AI\n");

        // Create fighters
        Fighter fighter1 = createFighter(1);
        Fighter fighter2 = createFighter(2);

        // Display fighter info
        displayFighterInfo(fighter1, fighter2);

        // Start the fight
        System.out.println("\nLET'S GET READY TO RUMBLE!");
        animateText("...FIGHT STARTING IN 3...2...1...", 150);
        System.out.println("\n");

        // Fight loop
        int round = 1;
        while (fighter1.isAlive() && fighter2.isAlive() && round <= 5) {
            System.out.println("\n=== ROUND " + round + " ===");
            System.out.println(fighter1.getName() + " (HP: " + fighter1.getHealth() + ") vs " +
                    fighter2.getName() + " (HP: " + fighter2.getHealth() + ")");

            // Each fighter gets 3-5 moves per round
            int moves = 3 + random.nextInt(3);
            for (int i = 0; i < moves; i++) {
                if (!fighter1.isAlive() || !fighter2.isAlive()) break;

                // Alternate turns with random chance for counter attacks
                if (random.nextBoolean()) {
                    executeMove(fighter1, fighter2);
                } else {
                    executeMove(fighter2, fighter1);
                }

                // Small delay between moves
                TimeUnit.MILLISECONDS.sleep(800);
            }

            // End of round recovery
            if (fighter1.isAlive() && fighter2.isAlive()) {
                System.out.println("\n* End of Round " + round + " *");
                fighter1.recover();
                fighter2.recover();
                displayFighterStatus(fighter1, fighter2);
                TimeUnit.SECONDS.sleep(2);
            }

            round++;
        }

        // Determine winner
        System.out.println("\n=== FIGHT RESULT ===");
        if (!fighter1.isAlive() && !fighter2.isAlive()) {
            System.out.println("DRAW! Both fighters knocked each other out!");
        } else if (!fighter1.isAlive()) {
            System.out.println(fighter2.getName().toUpperCase() + " WINS BY KNOCKOUT!");
        } else if (!fighter2.isAlive()) {
            System.out.println(fighter1.getName().toUpperCase() + " WINS BY KNOCKOUT!");
        } else {
            System.out.println("FIGHT GOES TO THE JUDGES' SCORECARDS...");
            Fighter winner = judgeDecision(fighter1, fighter2);
            System.out.println(winner.getName().toUpperCase() + " WINS BY DECISION!");
        }

        System.out.println("\n=== FIGHT STATISTICS ===");
        displayFightStats(fighter1, fighter2);
    }

    private static Fighter createFighter(int num) {
        System.out.println("\nEnter details for Fighter " + num + ":");
        System.out.print("Name: ");
        String name = scanner.nextLine();

        System.out.print("Style (Boxer, Wrestler, Kickboxer, JiuJitsu, AllRounder): ");
        String style = scanner.nextLine();

        // Create fighter with style-based attributes
        switch (style.toLowerCase()) {
            case "boxer":
                return new Boxer(name);
            case "wrestler":
                return new Wrestler(name);
            case "kickboxer":
                return new Kickboxer(name);
            case "jiujitsu":
                return new JiuJitsuSpecialist(name);
            default:
                return new AllRounder(name);
        }
    }

    private static void displayFighterInfo(Fighter f1, Fighter f2) throws InterruptedException {
        System.out.println("\n=== FIGHTER PROFILES ===");
        animateText(f1.toString(), 50);
        animateText(f2.toString(), 50);
    }

    private static void executeMove(Fighter attacker, Fighter defender) throws InterruptedException {
        // Select move based on fighter style and strategy
        String move = attacker.selectMove();
        int damage = attacker.executeMove(move, defender);

        // Animation effect
        animateCombat(attacker.getName(), move, defender.getName(), damage);

        // Apply damage
        if (damage > 0) {
            defender.takeDamage(damage);
            if (!defender.isAlive()) {
                animateText("\n*** " + defender.getName() + " HAS BEEN KNOCKED OUT! ***", 100);
            }
        }
    }

    private static void animateCombat(String attacker, String move, String defender, int damage) throws InterruptedException {
        System.out.print("\n" + attacker + " ");
        animateText(move, 30);

        if (damage > 0) {
            System.out.print(" and hits " + defender + " for " + damage + " damage!");
        } else {
            System.out.print(" but " + defender + " defends!");
        }
    }

    private static void animateText(String text, int delay) throws InterruptedException {
        for (char c : text.toCharArray()) {
            System.out.print(c);
            TimeUnit.MILLISECONDS.sleep(delay);
        }
    }

    private static void displayFighterStatus(Fighter f1, Fighter f2) {
        System.out.println(f1.getName() + ": HP=" + f1.getHealth() + ", Stamina=" + f1.getStamina());
        System.out.println(f2.getName() + ": HP=" + f2.getHealth() + ", Stamina=" + f2.getStamina());
    }

    private static Fighter judgeDecision(Fighter f1, Fighter f2) {
        // Simple scoring based on damage dealt and stamina
        int score1 = f1.getTotalDamage() - f1.getDamageTaken();
        int score2 = f2.getTotalDamage() - f2.getDamageTaken();

        if (score1 > score2) return f1;
        if (score2 > score1) return f2;

        // If scores are equal, consider stamina
        return f1.getStamina() > f2.getStamina() ? f1 : f2;
    }

    private static void displayFightStats(Fighter f1, Fighter f2) {
        System.out.println(f1.getName() + ":");
        System.out.println("  Total Damage Dealt: " + f1.getTotalDamage());
        System.out.println("  Damage Taken: " + f1.getDamageTaken());
        System.out.println("  Significant Strikes: " + f1.getSignificantStrikes());
        System.out.println("  Takedowns: " + f1.getTakedowns());

        System.out.println("\n" + f2.getName() + ":");
        System.out.println("  Total Damage Dealt: " + f2.getTotalDamage());
        System.out.println("  Damage Taken: " + f2.getDamageTaken());
        System.out.println("  Significant Strikes: " + f2.getSignificantStrikes());
        System.out.println("  Takedowns: " + f2.getTakedowns());
    }
}

abstract class Fighter {
    protected String name;
    protected int health;
    protected int stamina;
    protected int totalDamage;
    protected int damageTaken;
    protected int significantStrikes;
    protected int takedowns;
    protected List<String> moveSet;
    protected Map<String, Integer> moveDamage;

    public Fighter(String name) {
        this.name = name;
        this.health = 100;
        this.stamina = 100;
        this.totalDamage = 0;
        this.damageTaken = 0;
        this.significantStrikes = 0;
        this.takedowns = 0;
        this.moveSet = new ArrayList<>();
        this.moveDamage = new HashMap<>();
        initializeMoves();
    }

    protected abstract void initializeMoves();

    public String selectMove() {
        // Simple AI: choose random move from moveSet
        return moveSet.get(new Random().nextInt(moveSet.size()));
    }

    public int executeMove(String move, Fighter opponent) {
        if (stamina < 10) {
            recover(); // If very low stamina, recover instead
            return 0;
        }

        int baseDamage = moveDamage.getOrDefault(move, 5);
        int damage = calculateDamage(baseDamage, opponent);

        // Stamina cost based on move type
        int staminaCost = baseDamage / 2 + 5;
        stamina = Math.max(0, stamina - staminaCost);

        // Record stats
        if (damage > 0) {
            totalDamage += damage;
            if (move.toLowerCase().contains("punch") || move.toLowerCase().contains("kick")) {
                significantStrikes++;
            }
            if (move.toLowerCase().contains("takedown")) {
                takedowns++;
            }
        }

        return damage;
    }

    protected int calculateDamage(int baseDamage, Fighter opponent) {
        // Damage calculation with random variation and defense
        int variation = new Random().nextInt(10) - 3; // -3 to +6
        int damage = Math.max(1, baseDamage + variation);

        // Opponent has 30% chance to defend based on their stamina
        if (new Random().nextInt(100) < opponent.getStamina() / 3) {
            return 0; // defended
        }

        return damage;
    }

    public void takeDamage(int amount) {
        health = Math.max(0, health - amount);
        damageTaken += amount;
        stamina = Math.max(0, stamina - (amount / 2)); // Taking damage reduces stamina
    }

    public void recover() {
        // Recover some stamina between rounds
        stamina = Math.min(100, stamina + 20 + new Random().nextInt(15));
    }

    public boolean isAlive() {
        return health > 0;
    }

    // Getters
    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getStamina() { return stamina; }
    public int getTotalDamage() { return totalDamage; }
    public int getDamageTaken() { return damageTaken; }
    public int getSignificantStrikes() { return significantStrikes; }
    public int getTakedowns() { return takedowns; }

    @Override
    public String toString() {
        return String.format("%s [HP: %d, Stamina: %d]\nStyle: %s\nMoves: %s",
                name, health, stamina, getClass().getSimpleName(), moveSet);
    }
}

class Boxer extends Fighter {
    public Boxer(String name) {
        super(name);
    }

    @Override
    protected void initializeMoves() {
        moveSet.add("Jab");
        moveSet.add("Cross");
        moveSet.add("Hook");
        moveSet.add("Uppercut");
        moveSet.add("Body Shot");
        moveSet.add("Ducking Counter");

        moveDamage.put("Jab", 8);
        moveDamage.put("Cross", 12);
        moveDamage.put("Hook", 15);
        moveDamage.put("Uppercut", 18);
        moveDamage.put("Body Shot", 10);
        moveDamage.put("Ducking Counter", 14);
    }

    @Override
    protected int calculateDamage(int baseDamage, Fighter opponent) {
        // Boxers have higher chance to land head strikes
        if (new Random().nextBoolean()) {
            return (int)(baseDamage * 1.2); // 20% bonus for head strikes
        }
        return super.calculateDamage(baseDamage, opponent);
    }
}

class Wrestler extends Fighter {
    public Wrestler(String name) {
        super(name);
    }

    @Override
    protected void initializeMoves() {
        moveSet.add("Single Leg Takedown");
        moveSet.add("Double Leg Takedown");
        moveSet.add("Suplex");
        moveSet.add("Ground and Pound");
        moveSet.add("Clinch Knees");
        moveSet.add("Submission Attempt");

        moveDamage.put("Single Leg Takedown", 10);
        moveDamage.put("Double Leg Takedown", 12);
        moveDamage.put("Suplex", 15);
        moveDamage.put("Ground and Pound", 18);
        moveDamage.put("Clinch Knees", 12);
        moveDamage.put("Submission Attempt", 20);
    }

    @Override
    protected int calculateDamage(int baseDamage, Fighter opponent) {
        // Wrestlers have higher chance to land takedowns
        if (moveSet.get(new Random().nextInt(3)).contains("Takedown")) {
            return (int)(baseDamage * 1.3); // 30% bonus for takedowns
        }
        return super.calculateDamage(baseDamage, opponent);
    }
}

class Kickboxer extends Fighter {
    public Kickboxer(String name) {
        super(name);
    }

    @Override
    protected void initializeMoves() {
        moveSet.add("Low Kick");
        moveSet.add("Roundhouse Kick");
        moveSet.add("Front Kick");
        moveSet.add("Knee Strike");
        moveSet.add("Spinning Back Kick");
        moveSet.add("Elbow Strike");

        moveDamage.put("Low Kick", 12);
        moveDamage.put("Roundhouse Kick", 18);
        moveDamage.put("Front Kick", 10);
        moveDamage.put("Knee Strike", 14);
        moveDamage.put("Spinning Back Kick", 20);
        moveDamage.put("Elbow Strike", 16);
    }

    @Override
    public int executeMove(String move, Fighter opponent) {
        // Kickboxers use more stamina but deal more damage
        int damage = super.executeMove(move, opponent);
        if (move.contains("Kick")) {
            return (int)(damage * 1.15); // 15% bonus for kicks
        }
        return damage;
    }
}

class JiuJitsuSpecialist extends Fighter {
    public JiuJitsuSpecialist(String name) {
        super(name);
    }

    @Override
    protected void initializeMoves() {
        moveSet.add("Guard Pull");
        moveSet.add("Armbar Attempt");
        moveSet.add("Triangle Choke");
        moveSet.add("Kimura Lock");
        moveSet.add("Sweep");
        moveSet.add("Rear Naked Choke");

        moveDamage.put("Guard Pull", 5);
        moveDamage.put("Armbar Attempt", 15);
        moveDamage.put("Triangle Choke", 18);
        moveDamage.put("Kimura Lock", 16);
        moveDamage.put("Sweep", 10);
        moveDamage.put("Rear Naked Choke", 20);
    }

    @Override
    protected int calculateDamage(int baseDamage, Fighter opponent) {
        // Jiu-Jitsu specialists have higher submission chance when opponent is tired
        if (opponent.getStamina() < 30 && moveSet.get(new Random().nextInt(3)).contains("Choke")) {
            return (int)(baseDamage * 1.4); // 40% bonus for submissions against tired opponents
        }
        return super.calculateDamage(baseDamage, opponent);
    }
}

class AllRounder extends Fighter {
    public AllRounder(String name) {
        super(name);
    }

    @Override
    protected void initializeMoves() {
        moveSet.add("Jab");
        moveSet.add("Cross");
        moveSet.add("Low Kick");
        moveSet.add("Double Leg Takedown");
        moveSet.add("Ground and Pound");
        moveSet.add("Guillotine Attempt");

        moveDamage.put("Jab", 8);
        moveDamage.put("Cross", 12);
        moveDamage.put("Low Kick", 10);
        moveDamage.put("Double Leg Takedown", 12);
        moveDamage.put("Ground and Pound", 15);
        moveDamage.put("Guillotine Attempt", 16);
    }

    @Override
    public int executeMove(String move, Fighter opponent) {
        // All-rounders are more consistent but don't have specialties
        return super.executeMove(move, opponent);
    }
}