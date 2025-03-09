package org.example;

import java.util.*;

class Game {
    private static final int MAX_TURNS = 50;
    private List<Player> players;
    private Map<String, Tile> map;
    private int currentTurn;

    public Game() {
        players = new ArrayList<>();
        map = new HashMap<>();
        currentTurn = 1;
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void initializeMap(int width, int height) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map.put(x + "," + y, new Tile(x, y));
            }
        }
    }

    public void startGame() {
        while (currentTurn <= MAX_TURNS) {
            System.out.println("Turn " + currentTurn);
            for (Player player : players) {
                player.takeTurn();
            }
            currentTurn++;
        }
        System.out.println("Game Over!");
    }

    public Tile getTile(int x, int y) {
        return map.get(x + "," + y);
    }
}

class Player {
    private String name;
    private List<City> cities;
    private int gold;

    public Player(String name) {
        this.name = name;
        cities = new ArrayList<>();
        gold = 100;
    }

    public String getName() {
        return name;
    }

    public void addCity(City city) {
        cities.add(city);
    }

    public void takeTurn() {
        System.out.println(name + "'s turn");
        for (City city : cities) {
            city.produce();
        }
        gold += 10; // Player earns gold each turn
        System.out.println(name + " has " + gold + " gold.");
    }
}

class City {
    private String name;
    private int population;
    private int food;
    private int production;

    public City(String name) {
        this.name = name;
        this.population = 1;
        this.food = 0;
        this.production = 5;
    }

    public void produce() {
        food += production;
        System.out.println(name + " produced " + production + " food. Total food: " + food);
        if (food >= 100) {
            grow();
        }
    }

    public void grow() {
        population++;
        food -= 100;
        System.out.println(name + " has grown! New population: " + population);
    }
}

class Unit {
    private String name;
    private int health;
    private int attack;
    private int x, y;

    public Unit(String name, int health, int attack, int x, int y) {
        this.name = name;
        this.health = health;
        this.attack = attack;
        this.x = x;
        this.y = y;
    }

    public void move(int newX, int newY) {
        this.x = newX;
        this.y = newY;
        System.out.println(name + " moved to " + x + "," + y);
    }

    public void attack(Unit target) {
        target.health -= this.attack;
        System.out.println(name + " attacked " + target.name + " for " + this.attack + " damage.");
        if (target.health <= 0) {
            System.out.println(target.name + " has been defeated!");
        }
    }

    public boolean isAlive() {
        return health > 0;
    }
}

class Tile {
    private int x, y;
    private String resource;

    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
        this.resource = "None";
    }

    public void addResource(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }

    @Override
    public String toString() {
        return "Tile(" + x + "," + y + ") - Resource: " + resource;
    }
}

public class CivilizationGame {
    public static void main(String[] args) {
        Game game = new Game();

        Player player1 = new Player("CivPlayer1");
        Player player2 = new Player("CivPlayer2");

        City city1 = new City("Alpha");
        City city2 = new City("Beta");

        player1.addCity(city1);
        player2.addCity(city2);

        game.addPlayer(player1);
        game.addPlayer(player2);

        game.initializeMap(5, 5);

        Unit warrior1 = new Unit("Warrior", 100, 20, 0, 0);
        Unit warrior2 = new Unit("Warrior", 100, 15, 4, 4);

        game.startGame();

        warrior1.move(1, 1);
        warrior1.attack(warrior2);
    }
}
