package org.example;

import java.util.Random;
import java.util.Scanner;

// Main class to run the HeroQuest simulation
public class HeroQuestBoardGameSimulation {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        GameBoard gameBoard = new GameBoard(5, 5); // 5x5 board
        Hero hero = new Hero("Conan", 3, 2, 10, 1, 1);
        Monster monster = new Monster("Goblin", 2, 1, 5, 3, 3);

        gameBoard.placeCharacter(hero);
        gameBoard.placeCharacter(monster);
        gameBoard.displayBoard();

        System.out.println("Welcome to HeroQuest Simulation!");
        System.out.println("Commands: move [up/down/left/right], attack, quit");

        while (true) {
            System.out.println("\nHero's Turn:");
            System.out.println("Hero HP: " + hero.getHealth() + " | Monster HP: " + monster.getHealth());
            System.out.print("Enter command: ");
            String command = scanner.nextLine().toLowerCase();

            if (command.equals("quit")) {
                System.out.println("Game Over! Thanks for playing.");
                break;
            }

            // Hero's turn
            if (command.startsWith("move")) {
                String direction = command.split(" ")[1];
                hero.move(gameBoard, direction);
            } else if (command.equals("attack")) {
                if (hero.canAttack(monster)) {
                    hero.attack(monster);
                } else {
                    System.out.println("Monster is too far to attack! Move closer.");
                }
            }

            gameBoard.displayBoard();

            // Check if monster is defeated
            if (monster.getHealth() <= 0) {
                System.out.println("Victory! The monster has been defeated!");
                break;
            }

            // Monster's turn
            System.out.println("\nMonster's Turn:");
            monster.takeTurn(gameBoard, hero);
            gameBoard.displayBoard();

            // Check if hero is defeated
            if (hero.getHealth() <= 0) {
                System.out.println("Game Over! The hero has been defeated!");
                break;
            }
        }
        scanner.close();
    }
}

// Class to represent the game board
class GameBoard {
    private int width;
    private int height;
    private Character[][] board;

    public GameBoard(int width, int height) {
        this.width = width;
        this.height = height;
        this.board = new Character[width][height];
    }

    public void placeCharacter(Character character) {
        board[character.getX()][character.getY()] = character;
    }

    public void clearPosition(int x, int y) {
        board[x][y] = null;
    }

    public Character getCharacterAt(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return board[x][y];
        }
        return null;
    }

    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height && board[x][y] == null;
    }

    public void displayBoard() {
        System.out.println("\nGame Board:");
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (board[x][y] == null) {
                    System.out.print("[ ]");
                } else {
                    System.out.print("[" + board[x][y].getSymbol() + "]");
                }
            }
            System.out.println();
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}

// Abstract class for game characters (heroes and monsters)
abstract class Character {
    protected String name;
    protected int attack;
    protected int defense;
    protected int health;
    protected int x;
    protected int y;
    protected char symbol;
    protected Random random;

    public Character(String name, int attack, int defense, int health, int x, int y, char symbol) {
        this.name = name;
        this.attack = attack;
        this.defense = defense;
        this.health = health;
        this.x = x;
        this.y = y;
        this.symbol = symbol;
        this.random = new Random();
    }

    public void move(GameBoard board, String direction) {
        int newX = x;
        int newY = y;

        switch (direction) {
            case "up":
                newY--;
                break;
            case "down":
                newY++;
                break;
            case "left":
                newX--;
                break;
            case "right":
                newX++;
                break;
            default:
                System.out.println("Invalid direction! Use up/down/left/right.");
                return;
        }

        if (board.isValidPosition(newX, newY)) {
            board.clearPosition(x, y);
            x = newX;
            y = newY;
            board.placeCharacter(this);
            System.out.println(name + " moved to (" + x + ", " + y + ").");
        } else {
            System.out.println("Cannot move to (" + newX + ", " + newY + "). Position is invalid or occupied.");
        }
    }

    public void attack(Character target) {
        int damage = Math.max(0, rollDice(attack) - rollDice(target.defense));
        target.takeDamage(damage);
        System.out.println(name + " attacks " + target.name + " for " + damage + " damage!");
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health < 0) health = 0;
        System.out.println(name + " now has " + health + " health.");
    }

    public boolean canAttack(Character target) {
        return Math.abs(x - target.x) <= 1 && Math.abs(y - target.y) <= 1;
    }

    protected int rollDice(int value) {
        int total = 0;
        for (int i = 0; i < value; i++) {
            total += random.nextInt(6) + 1; // Simulate a 6-sided die
        }
        return total;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getHealth() {
        return health;
    }

    public String getName() {
        return name;
    }

    public char getSymbol() {
        return symbol;
    }
}

// Class for the hero character
class Hero extends Character {
    public Hero(String name, int attack, int defense, int health, int x, int y) {
        super(name, attack, defense, health, x, y, 'H');
    }
}

// Class for the monster character
class Monster extends Character {
    public Monster(String name, int attack, int defense, int health, int x, int y) {
        super(name, attack, defense, health, x, y, 'M');
    }

    public void takeTurn(GameBoard board, Hero hero) {
        // Monster AI: Move towards the hero if not in attack range, otherwise attack
        if (canAttack(hero)) {
            attack(hero);
        } else {
            // Move towards the hero
            int dx = hero.getX() - x;
            int dy = hero.getY() - y;

            String direction;
            if (Math.abs(dx) > Math.abs(dy)) {
                direction = dx > 0 ? "right" : "left";
            } else {
                direction = dy > 0 ? "down" : "up";
            }

            move(board, direction);
        }
    }
}
