package org.example;

import java.util.*;

class Unit {
    String name;
    int health;
    int attack;
    int x, y; // Position on the grid
    boolean isAlive;

    public Unit(String name, int health, int attack, int x, int y) {
        this.name = name;
        this.health = health;
        this.attack = attack;
        this.x = x;
        this.y = y;
        this.isAlive = true;
    }

    public void move(int newX, int newY, int gridSize) {
        if (newX >= 0 && newX < gridSize && newY >= 0 && newY < gridSize) {
            this.x = newX;
            this.y = newY;
            System.out.println(name + " moved to (" + x + "," + y + ")");
        } else {
            System.out.println("Invalid move!");
        }
    }

    public void attack(Unit enemy) {
        if (Math.abs(this.x - enemy.x) + Math.abs(this.y - enemy.y) == 1) {
            enemy.health -= this.attack;
            System.out.println(this.name + " attacks " + enemy.name + " for " + this.attack + " damage!");
            if (enemy.health <= 0) {
                enemy.isAlive = false;
                System.out.println(enemy.name + " has been defeated!");
            }
        } else {
            System.out.println("Enemy is out of range!");
        }
    }
}

class Player {
    String name;
    List<Unit> units;

    public Player(String name) {
        this.name = name;
        this.units = new ArrayList<>();
    }

    public void addUnit(Unit unit) {
        units.add(unit);
    }

    public boolean hasUnitsAlive() {
        for (Unit unit : units) {
            if (unit.isAlive) return true;
        }
        return false;
    }
}

class Grid {
    int size;
    Unit[][] grid;

    public Grid(int size) {
        this.size = size;
        grid = new Unit[size][size];
    }

    public void updateGrid(Player p1, Player p2) {
        for (int i = 0; i < size; i++) {
            Arrays.fill(grid[i], null);
        }
        for (Unit u : p1.units) {
            if (u.isAlive) grid[u.x][u.y] = u;
        }
        for (Unit u : p2.units) {
            if (u.isAlive) grid[u.x][u.y] = u;
        }
    }

    public void displayGrid() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] != null) {
                    System.out.print(grid[i][j].name.charAt(0) + " ");
                } else {
                    System.out.print(". ");
                }
            }
            System.out.println();
        }
    }
}

public class TurnBasedStrategyGame {
    static Scanner scanner = new Scanner(System.in);
    static int gridSize = 5;

    public static void main(String[] args) {
        Player player1 = new Player("Player 1");
        Player player2 = new Player("Player 2");

        player1.addUnit(new Unit("Knight", 20, 5, 0, 0));
        player1.addUnit(new Unit("Archer", 15, 4, 1, 0));

        player2.addUnit(new Unit("Orc", 18, 6, 4, 4));
        player2.addUnit(new Unit("Goblin", 12, 3, 3, 4));

        Grid grid = new Grid(gridSize);
        boolean playerOneTurn = true;

        while (player1.hasUnitsAlive() && player2.hasUnitsAlive()) {
            grid.updateGrid(player1, player2);
            grid.displayGrid();
            System.out.println((playerOneTurn ? player1.name : player2.name) + "'s turn!");

            Player currentPlayer = playerOneTurn ? player1 : player2;
            Player opponent = playerOneTurn ? player2 : player1;

            System.out.println("Select a unit (0-" + (currentPlayer.units.size() - 1) + "): ");
            int unitIndex = scanner.nextInt();

            if (unitIndex < 0 || unitIndex >= currentPlayer.units.size()) continue;

            Unit unit = currentPlayer.units.get(unitIndex);
            if (!unit.isAlive) {
                System.out.println("This unit is dead!");
                continue;
            }

            System.out.println("1. Move  2. Attack");
            int action = scanner.nextInt();

            if (action == 1) {
                System.out.println("Enter new X Y: ");
                int newX = scanner.nextInt();
                int newY = scanner.nextInt();
                unit.move(newX, newY, gridSize);
            } else if (action == 2) {
                System.out.println("Select enemy unit to attack (0-" + (opponent.units.size() - 1) + "): ");
                int enemyIndex = scanner.nextInt();

                if (enemyIndex < 0 || enemyIndex >= opponent.units.size()) continue;

                Unit enemy = opponent.units.get(enemyIndex);
                if (enemy.isAlive) {
                    unit.attack(enemy);
                } else {
                    System.out.println("Enemy unit is already dead!");
                }
            }
            playerOneTurn = !playerOneTurn;
        }

        System.out.println((player1.hasUnitsAlive() ? player1.name : player2.name) + " wins!");
    }
}
