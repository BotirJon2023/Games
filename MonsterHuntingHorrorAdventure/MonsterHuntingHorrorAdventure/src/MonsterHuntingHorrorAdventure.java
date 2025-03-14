import java.util.*;

class Monster {
    String name;
    int health;
    int damage;

    public Monster(String name, int health, int damage) {
        this.name = name;
        this.health = health;
        this.damage = damage;
    }

    public void takeDamage(int damageTaken) {
        this.health -= damageTaken;
    }

    public boolean isAlive() {
        return this.health > 0;
    }

    public String toString() {
        return name + " (Health: " + health + ")";
    }
}

class Player {
    String name;
    int health;
    int damage;
    Scanner scanner;

    public Player(String name, int health, int damage) {
        this.name = name;
        this.health = health;
        this.damage = damage;
        this.scanner = new Scanner(System.in);
    }

    public void takeDamage(int damageTaken) {
        this.health -= damageTaken;
    }

    public boolean isAlive() {
        return this.health > 0;
    }

    public void attack(Monster monster) {
        System.out.println(name + " attacks " + monster.name + " for " + damage + " damage!");
        monster.takeDamage(damage);
    }

    public void heal(int healingAmount) {
        this.health += healingAmount;
        System.out.println(name + " heals for " + healingAmount + " health!");
    }

    public void showStatus() {
        System.out.println(name + " (Health: " + health + ")");
    }

    public void performAction(Monster monster) {
        System.out.println("Choose action: (1) Attack, (2) Heal");
        int action = scanner.nextInt();
        if (action == 1) {
            attack(monster);
        } else if (action == 2) {
            heal(10);
        }
    }
}

class Game {
    Player player;
    List<Monster> monsters;
    Scanner scanner;

    public Game(String playerName) {
        this.player = new Player(playerName, 100, 20);
        this.monsters = new ArrayList<>();
        this.scanner = new Scanner(System.in);
    }

    public void spawnMonsters() {
        monsters.add(new Monster("Zombie", 50, 10));
        monsters.add(new Monster("Werewolf", 70, 15));
        monsters.add(new Monster("Vampire", 90, 20));
    }

    public void encounterMonster(Monster monster) {
        System.out.println("\nA wild " + monster.name + " appears!\n");

        while (monster.isAlive() && player.isAlive()) {
            player.showStatus();
            monsterEncounter(monster);
            if (!monster.isAlive()) {
                System.out.println(monster.name + " has been defeated!");
            } else {
                monsterAttack(monster);
            }
        }

        if (!player.isAlive()) {
            System.out.println("You have been defeated by the " + monster.name + "...");
        }
    }

    private void monsterAttack(Monster monster) {
        System.out.println(monster.name + " attacks you for " + monster.damage + " damage!");
        player.takeDamage(monster.damage);
    }

    public void monsterEncounter(Monster monster) {
        player.performAction(monster);
    }

    public void start() {
        spawnMonsters();
        System.out.println("Welcome to the Monster Hunting Horror Adventure!");

        while (player.isAlive()) {
            System.out.println("\nChoose an action: ");
            System.out.println("1. Fight a Monster");
            System.out.println("2. Exit Game");

            int choice = scanner.nextInt();

            if (choice == 1) {
                Monster monster = monsters.get(new Random().nextInt(monsters.size()));
                encounterMonster(monster);
            } else if (choice == 2) {
                System.out.println("You have chosen to exit the game.");
                break;
            }
        }
    }
}

public class MonsterHuntingHorrorAdventure {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your character's name: ");
        String playerName = scanner.nextLine();

        Game game = new Game(playerName);
        game.start();
    }
}
