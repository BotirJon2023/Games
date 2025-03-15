package org.example;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Base class for all game entities
abstract class Entity {
    protected String name;
    protected int health;

    public Entity(String name, int health) {
        this.name = name;
        this.health = health;
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            System.out.println(name + " has been destroyed!");
        }
    }
}

// Resource class
class Resource {
    private String type;
    private int quantity;

    public Resource(String type, int quantity) {
        this.type = type;
        this.quantity = quantity;
    }

    public int gather(int amount) {
        if (quantity >= amount) {
            quantity -= amount;
            return amount;
        } else {
            int gathered = quantity;
            quantity = 0;
            return gathered;
        }
    }
}

// Unit class
class Unit extends Entity {
    private int attackPower;

    public Unit(String name, int health, int attackPower) {
        super(name, health);
        this.attackPower = attackPower;
    }

    public void attack(Unit enemy) {
        System.out.println(name + " attacks " + enemy.name + " for " + attackPower + " damage!");
        enemy.takeDamage(attackPower);
    }
}

// Building class
class Building extends Entity {
    public Building(String name, int health) {
        super(name, health);
    }
}

// Game Manager
class Game {
    private List<Unit> units = new ArrayList<>();
    private List<Building> buildings = new ArrayList<>();
    private List<Resource> resources = new ArrayList<>();

    public void addUnit(Unit unit) {
        units.add(unit);
    }

    public void addBuilding(Building building) {
        buildings.add(building);
    }

    public void addResource(Resource resource) {
        resources.add(resource);
    }

    public void startBattle() {
        if (units.size() < 2) {
            System.out.println("Not enough units for battle!");
            return;
        }
        Random rand = new Random();
        Unit unit1 = units.get(rand.nextInt(units.size()));
        Unit unit2 = units.get(rand.nextInt(units.size()));
        while (unit1 == unit2) {
            unit2 = units.get(rand.nextInt(units.size()));
        }
        System.out.println("Battle begins between " + unit1.name + " and " + unit2.name);
        unit1.attack(unit2);
        if (unit2.health > 0) {
            unit2.attack(unit1);
        }
    }
}

// Main class to test the game logic
public class RealTimeStrategyPrototype {
    public static void main(String[] args) {
        Game game = new Game();

        // Creating units
        Unit soldier = new Unit("Soldier", 100, 15);
        Unit archer = new Unit("Archer", 80, 20);

        game.addUnit(soldier);
        game.addUnit(archer);

        // Creating resources
        Resource goldMine = new Resource("Gold", 500);
        game.addResource(goldMine);

        // Creating buildings
        Building barracks = new Building("Barracks", 200);
        game.addBuilding(barracks);

        // Start a battle
        game.startBattle();
    }
}
