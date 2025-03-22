package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class Tile {
    private String type;
    private int population;
    private int pollution;
    private int wealth;

    public Tile(String type) {
        this.type = type;
        this.population = 0;
        this.pollution = 0;
        this.wealth = 0;
    }

    public String getType() {
        return type;
    }

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    public int getPollution() {
        return pollution;
    }

    public void setPollution(int pollution) {
        this.pollution = pollution;
    }

    public int getWealth() {
        return wealth;
    }

    public void setWealth(int wealth) {
        this.wealth = wealth;
    }

    public void update() {
        // Simple simulation logic
        if (type.equals("Residential")) {
            population += new Random().nextInt(5);
            wealth += new Random().nextInt(3);
        } else if (type.equals("Commercial")) {
            wealth += new Random().nextInt(10);
        } else if (type.equals("Industrial")) {
            pollution += new Random().nextInt(10);
            wealth += new Random().nextInt(8);
        }

        if (pollution > 50 && type.equals("Residential")) {
            population -= new Random().nextInt(2);
        }

        if (wealth < 0) {
            wealth = 0;
        }
        if(population < 0){
            population = 0;
        }

    }

    @Override
    public String toString() {
        return "Tile{" +
                "type='" + type + '\'' +
                ", population=" + population +
                ", pollution=" + pollution +
                ", wealth=" + wealth +
                '}';
    }
}

class City {
    private List<List<Tile>> grid;
    private int size;

    public City(int size) {
        this.size = size;
        this.grid = new ArrayList<>();
        initializeGrid();
    }

    private void initializeGrid() {
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            List<Tile> row = new ArrayList<>();
            for (int j = 0; j < size; j++) {
                String type = getRandomTileType(random);
                row.add(new Tile(type));
            }
            grid.add(row);
        }
    }

    private String getRandomTileType(Random random) {
        int rand = random.nextInt(10);
        if (rand < 5) {
            return "Residential";
        } else if (rand < 8) {
            return "Commercial";
        } else {
            return "Industrial";
        }
    }

    public void update() {
        for (List<Tile> row : grid) {
            for (Tile tile : row) {
                tile.update();
            }
        }
    }

    public void display() {
        for (List<Tile> row : grid) {
            for (Tile tile : row) {
                System.out.print(tile.getType().charAt(0) + " ");
            }
            System.out.println();
        }
    }

    public void displayDetailed() {
        for (List<Tile> row : grid) {
            for (Tile tile : row) {
                System.out.println(tile.toString());
            }
        }
    }

    public Tile getTile(int x, int y) {
        if (x >= 0 && x < size && y >= 0 && y < size) {
            return grid.get(y).get(x);
        }
        return null;
    }

    public void setTile(int x, int y, String type) {
        if (x >= 0 && x < size && y >= 0 && y < size) {
            grid.get(y).set(x, new Tile(type));
        }
    }

    public int calculateTotalPopulation() {
        int totalPopulation = 0;
        for (List<Tile> row : grid) {
            for (Tile tile : row) {
                totalPopulation += tile.getPopulation();
            }
        }
        return totalPopulation;
    }

    public int calculateTotalWealth() {
        int totalWealth = 0;
        for (List<Tile> row : grid) {
            for (Tile tile : row) {
                totalWealth += tile.getWealth();
            }
        }
        return totalWealth;
    }

    public int calculateTotalPollution() {
        int totalPollution = 0;
        for (List<Tile> row : grid) {
            for (Tile tile : row) {
                totalPollution += tile.getPollution();
            }
        }
        return totalPollution;
    }

}

public class SimCityStyleSimulation {
    public static void main(String[] args) {
        City city = new City(10);

        for (int i = 0; i < 10; i++) {
            city.update();
            city.display();
            System.out.println("---");
            System.out.println("Total population: " + city.calculateTotalPopulation());
            System.out.println("Total wealth: " + city.calculateTotalWealth());
            System.out.println("Total pollution: " + city.calculateTotalPollution());
            System.out.println("---");
        }
        city.setTile(0,0, "Residential");
        city.setTile(9,9, "Commercial");
        city.setTile(5,5, "Industrial");

        city.displayDetailed();
    }
}

