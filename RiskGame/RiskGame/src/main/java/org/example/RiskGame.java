package org.example;


import java.util.*;

class Territory {
    String name;
    Player owner;
    int armies;
    List<Territory> neighbors;

    public Territory(String name) {
        this.name = name;
        this.armies = 1;
        this.neighbors = new ArrayList<>();
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public void addArmies(int count) {
        this.armies += count;
    }

    public void removeArmies(int count) {
        this.armies = Math.max(0, this.armies - count);
    }

    public boolean attack(Territory defender) {
        Random rand = new Random();
        int attackRoll = rand.nextInt(6) + 1;
        int defenseRoll = rand.nextInt(6) + 1;
        return attackRoll > defenseRoll;
    }
}

class Player {
    String name;
    List<Territory> territories;
    int reinforcements;

    public Player(String name) {
        this.name = name;
        this.territories = new ArrayList<>();
        this.reinforcements = 5;
    }

    public void addTerritory(Territory t) {
        territories.add(t);
        t.setOwner(this);
    }

    public void reinforce(Territory t, int count) {
        if (territories.contains(t) && reinforcements >= count) {
            t.addArmies(count);
            reinforcements -= count;
        }
    }
}

class RiskGame {
    List<Player> players;
    List<Territory> territories;
    int turnCounter;

    public RiskGame(List<String> playerNames) {
        players = new ArrayList<>();
        territories = new ArrayList<>();
        turnCounter = 0;

        for (String name : playerNames) {
            players.add(new Player(name));
        }

        initializeTerritories();
        distributeTerritories();
    }

    private void initializeTerritories() {
        for (int i = 1; i <= 20; i++) {
            territories.add(new Territory("Territory " + i));
        }
    }

    private void distributeTerritories() {
        Random rand = new Random();
        for (int i = 0; i < territories.size(); i++) {
            Player player = players.get(i % players.size());
            player.addTerritory(territories.get(i));
        }
    }

    public void startGame() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            Player currentPlayer = players.get(turnCounter % players.size());
            System.out.println("\n" + currentPlayer.name + "'s turn:");
            takeTurn(currentPlayer, scanner);
            turnCounter++;
        }
    }

    private void takeTurn(Player player, Scanner scanner) {
        System.out.println("Reinforcement phase. You have " + player.reinforcements + " reinforcements.");
        System.out.println("Your territories:");
        for (Territory t : player.territories) {
            System.out.println(t.name + " (Armies: " + t.armies + ")");
        }

        while (player.reinforcements > 0) {
            System.out.println("Choose a territory to reinforce:");
            String territoryName = scanner.nextLine();
            Territory t = findTerritory(territoryName);
            if (t != null && t.owner == player) {
                System.out.println("Enter number of armies to add:");
                int count = scanner.nextInt();
                scanner.nextLine();
                player.reinforce(t, count);
            }
        }

        System.out.println("Attack phase. Choose a territory to attack from:");
        String fromName = scanner.nextLine();
        System.out.println("Choose a territory to attack:");
        String toName = scanner.nextLine();

        Territory from = findTerritory(fromName);
        Territory to = findTerritory(toName);

        if (from != null && to != null && from.owner == player && to.owner != player) {
            boolean won = from.attack(to);
            if (won) {
                System.out.println("Attack successful! You conquered " + to.name);
                to.setOwner(player);
                player.addTerritory(to);
            } else {
                System.out.println("Attack failed!");
            }
        } else {
            System.out.println("Invalid move!");
        }
    }

    private Territory findTerritory(String name) {
        for (Territory t : territories) {
            if (t.name.equals(name)) {
                return t;
            }
        }
        return null;
    }
}

