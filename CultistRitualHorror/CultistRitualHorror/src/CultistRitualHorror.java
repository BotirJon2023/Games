import java.util.*;

class CultistRitualHorror {
    private static final Scanner scanner = new Scanner(System.in);
    private static final Random random = new Random();
    private static boolean gameRunning = true;
    private static int sanity = 100;
    private static boolean hasArtifact = false;
    private static List<String> inventory = new ArrayList<>();
    private static int monsterHealth = 50;
    private static boolean foundKey = false;
    private static boolean solvedPuzzle = false;

    public static void main(String[] args) {
        System.out.println("Willkommen bei Cultist Ritual Horror!");
        System.out.println("Du bist in einer verlassenen Kirche gefangen. Finde einen Ausweg!");

        while (gameRunning) {
            showStatus();
            displayOptions();
            handleInput();
        }
        System.out.println("Spiel beendet. Danke fürs Spielen!");
    }

    private static void showStatus() {
        System.out.println("\nDeine geistige Gesundheit: " + sanity);
        System.out.println(hasArtifact ? "Du hast das dunkle Artefakt." : "Du hast das Artefakt nicht.");
        System.out.println(foundKey ? "Du hast einen alten Schlüssel gefunden." : "Du hast keinen Schlüssel.");
        System.out.println(solvedPuzzle ? "Du hast das Rätsel gelöst!" : "Das Rätsel ist noch ungelöst.");
        System.out.println("Inventar: " + inventory);
        if (monsterHealth > 0) {
            System.out.println("Ein Monster ist in der Nähe! Gesundheit des Monsters: " + monsterHealth);
        }
    }

    private static void displayOptions() {
        System.out.println("\nWas möchtest du tun?");
        System.out.println("1. Die Kirche erkunden");
        System.out.println("2. Nach Hinweisen suchen");
        System.out.println("3. Ein Ritual durchführen");
        System.out.println("4. Fliehen");
        System.out.println("5. Kämpfen");
        System.out.println("6. Ein Rätsel lösen");
        System.out.println("7. Aufgeben");
    }

    private static void handleInput() {
        System.out.print("Wähle eine Option: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                exploreChurch();
                break;
            case 2:
                searchForClues();
                break;
            case 3:
                performRitual();
                break;
            case 4:
                attemptEscape();
                break;
            case 5:
                fightMonster();
                break;
            case 6:
                solvePuzzle();
                break;
            case 7:
                gameRunning = false;
                break;
            default:
                System.out.println("Ungültige Auswahl. Versuch es erneut.");
        }
    }

    private static void exploreChurch() {
        System.out.println("Du erkundest die dunkle Kirche...");
        int event = random.nextInt(4);
        if (event == 0) {
            System.out.println("Du findest ein altes Buch mit dunklen Geheimnissen. Deine geistige Gesundheit leidet!");
            sanity -= 10;
        } else if (event == 1) {
            System.out.println("Du hörst Flüstern aus den Wänden... Es verstört dich tief!");
            sanity -= 15;
        } else if (event == 2) {
            System.out.println("Du findest eine alte Truhe. Sie ist verschlossen.");
            if (foundKey) {
                System.out.println("Du öffnest sie mit dem Schlüssel und findest ein mächtiges Ritualmesser!");
                inventory.add("Ritualmesser");
            }
        } else {
            System.out.println("Du findest eine versteckte Tür...");
        }
        checkSanity();
    }

    private static void searchForClues() {
        System.out.println("Du suchst nach Hinweisen...");
        int event = random.nextInt(2);
        if (event == 0) {
            System.out.println("Du findest ein dunkles Artefakt! Es fühlt sich unheilvoll an.");
            hasArtifact = true;
        } else {
            System.out.println("Du findest einen alten Schlüssel.");
            foundKey = true;
        }
    }

    private static void performRitual() {
        if (!hasArtifact) {
            System.out.println("Ohne das Artefakt ist das Ritual nutzlos...");
            return;
        }
        System.out.println("Du beginnst das Ritual... unheimliche Kräfte werden entfesselt!");
        sanity -= 20;
        if (random.nextBoolean()) {
            System.out.println("Das Ritual ist erfolgreich! Ein Portal öffnet sich...");
        } else {
            System.out.println("Das Ritual schlägt fehl! Schrecken durchdringen deinen Geist.");
            sanity -= 10;
        }
        checkSanity();
    }

    private static void attemptEscape() {
        System.out.println("Du versuchst zu fliehen...");
        if (hasArtifact && solvedPuzzle) {
            System.out.println("Mit dem Artefakt und dem gelösten Rätsel findest du den wahren Ausgang! Du entkommst!");
            gameRunning = false;
        } else {
            System.out.println("Etwas hält dich zurück... Du kannst noch nicht fliehen!");
            sanity -= 10;
            checkSanity();
        }
    }

    private static void fightMonster() {
        if (monsterHealth <= 0) {
            System.out.println("Es gibt kein Monster zum Kämpfen.");
            return;
        }
        System.out.println("Du greifst das Monster an!");
        int damage = random.nextInt(20) + 10;
        monsterHealth -= damage;
        System.out.println("Du verursachst " + damage + " Schaden!");

        if (monsterHealth > 0) {
            int monsterDamage = random.nextInt(15) + 5;
            sanity -= monsterDamage;
            System.out.println("Das Monster schlägt zurück und du verlierst " + monsterDamage + " geistige Gesundheit!");
        } else {
            System.out.println("Du hast das Monster besiegt!");
        }
        checkSanity();
    }

    private static void solvePuzzle() {
        System.out.println("Du versuchst, ein kryptisches Rätsel zu lösen...");
        if (random.nextBoolean()) {
            System.out.println("Du hast das Rätsel gelöst! Ein neuer Weg öffnet sich.");
            solvedPuzzle = true;
        } else {
            System.out.println("Das Rätsel bleibt ein Mysterium. Du versuchst es später erneut.");
        }
    }

    private static void checkSanity() {
        if (sanity <= 0) {
            System.out.println("Du verlierst den Verstand und wirst eins mit der Dunkelheit...");
            gameRunning = false;
        }
    }
}
