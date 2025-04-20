package org.example;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class MonopolyCloneGame extends JFrame {
    ArrayList<Player> players;
    Board board;
    private int currentPlayerIndex;
    private JLabel statusLabel;
    private DicePanel dicePanel;
    private BoardPanel boardPanel;
    private JButton rollButton;

    public MonopolyCloneGame() {
        setTitle("Monopoly Clone Game");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        players = new ArrayList<>();
        board = new Board();
        currentPlayerIndex = 0;

        initializePlayers();
        initializeUI();
    }

    private void initializePlayers() {
        players.add(new Player("Player 1", Color.RED));
        players.add(new Player("Player 2", Color.BLUE));
    }

    private void initializeUI() {
        boardPanel = new BoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        rollButton = new JButton("Roll Dice");
        rollButton.addActionListener(e -> rollDice());
        controlPanel.add(rollButton);

        statusLabel = new JLabel("Game Started! Player 1's turn.");
        controlPanel.add(statusLabel);

        dicePanel = new DicePanel();
        controlPanel.add(dicePanel);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private void rollDice() {
        rollButton.setEnabled(false);
        Player currentPlayer = players.get(currentPlayerIndex);
        dicePanel.animateDiceRoll(() -> {
            int roll = new Random().nextInt(6) + 1 + new Random().nextInt(6) + 1;
            statusLabel.setText(currentPlayer.getName() + " rolled a " + roll);
            movePlayer(currentPlayer, roll);
            boardPanel.repaint();
            rollButton.setEnabled(true);
            checkTileAction(currentPlayer);
            nextTurn();
        });
    }

    private void movePlayer(Player player, int roll) {
        int newPosition = (player.getPosition() + roll) % board.getTiles().size();
        player.setPosition(newPosition);
    }

    private void checkTileAction(Player player) {
        Tile tile = board.getTiles().get(player.getPosition());
        if (tile instanceof PropertyTile property) {
            if (property.getOwner() == null) {
                int response = JOptionPane.showConfirmDialog(this,
                        "Buy " + property.getName() + " for $" + property.getPrice() + "?",
                        "Purchase Property", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION && player.getMoney() >= property.getPrice()) {
                    player.setMoney(player.getMoney() - property.getPrice());
                    property.setOwner(player);
                    statusLabel.setText(player.getName() + " bought " + property.getName());
                }
            } else if (property.getOwner() != player) {
                int rent = property.getRent();
                player.setMoney(player.getMoney() - rent);
                property.getOwner().setMoney(property.getOwner().getMoney() + rent);
                statusLabel.setText(player.getName() + " paid $" + rent + " rent to " + property.getOwner().getName());
            }
        }
    }

    private void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        statusLabel.setText(players.get(currentPlayerIndex).getName() + "'s turn.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MonopolyCloneGame game = new MonopolyCloneGame();
            game.setVisible(true);
        });
    }
}

class Player {
    private String name;
    private int money;
    private int position;
    private Color color;

    public Player(String name, Color color) {
        this.name = name;
        this.money = 1500;
        this.position = 0;
        this.color = color;
    }

    public String getName() { return name; }
    public int getMoney() { return money; }
    public void setMoney(int money) { this.money = money; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public Color getColor() { return color; }
}

class Board {
    private ArrayList<Tile> tiles;

    public Board() {
        tiles = new ArrayList<>();
        initializeBoard();
    }

    private void initializeBoard() {
        tiles.add(new Tile("Go"));
        tiles.add(new PropertyTile("Mediterranean Avenue", 60, 2, Color.MAGENTA));
        tiles.add(new Tile("Community Chest"));
        tiles.add(new PropertyTile("Baltic Avenue", 60, 4, Color.MAGENTA));
        tiles.add(new Tile("Income Tax"));
        tiles.add(new PropertyTile("Reading Railroad", 200, 25, Color.BLACK));
        tiles.add(new PropertyTile("Oriental Avenue", 100, 6, Color.CYAN));
        tiles.add(new Tile("Chance"));
        tiles.add(new PropertyTile("Vermont Avenue", 100, 6, Color.CYAN));
        tiles.add(new PropertyTile("Connecticut Avenue", 120, 8, Color.CYAN));
        tiles.add(new Tile("Jail"));
        tiles.add(new PropertyTile("St. Charles Place", 140, 10, Color.PINK));
        tiles.add(new PropertyTile("Electric Company", 150, 10, Color.BLACK));
        tiles.add(new PropertyTile("States Avenue", 140, 10, Color.PINK));
        tiles.add(new PropertyTile("Virginia Avenue", 160, 12, Color.PINK));
        tiles.add(new PropertyTile("Pennsylvania Railroad", 200, 25, Color.BLACK));
        tiles.add(new PropertyTile("St. James Place", 180, 14, Color.ORANGE));
        tiles.add(new Tile("Community Chest"));
        tiles.add(new PropertyTile("Tennessee Avenue", 180, 14, Color.ORANGE));
        tiles.add(new PropertyTile("New York Avenue", 200, 16, Color.ORANGE));
        tiles.add(new Tile("Free Parking"));
        tiles.add(new PropertyTile("Kentucky Avenue", 220, 18, Color.RED));
        tiles.add(new Tile("Chance"));
        tiles.add(new PropertyTile("Indiana Avenue", 220, 18, Color.RED));
        tiles.add(new PropertyTile("Illinois Avenue", 240, 20, Color.RED));
        tiles.add(new PropertyTile("B&O Railroad", 200, 25, Color.BLACK));
        tiles.add(new PropertyTile("Atlantic Avenue", 260, 22, Color.YELLOW));
        tiles.add(new PropertyTile("Ventnor Avenue", 260, 22, Color.YELLOW));
        tiles.add(new PropertyTile("Water Works", 150, 10, Color.BLACK));
        tiles.add(new PropertyTile("Marvin Gardens", 280, 24, Color.YELLOW));
        tiles.add(new Tile("Go to Jail"));
        tiles.add(new PropertyTile("Pacific Avenue", 300, 26, Color.GREEN));
        tiles.add(new PropertyTile("North Carolina Avenue", 300, 26, Color.GREEN));
        tiles.add(new Tile("Community Chest"));
        tiles.add(new PropertyTile("Pennsylvania Avenue", 320, 28, Color.GREEN));
        tiles.add(new PropertyTile("Short Line", 200, 25, Color.BLACK));
        tiles.add(new Tile("Chance"));
        tiles.add(new PropertyTile("Park Place", 350, 35, Color.BLUE));
        tiles.add(new Tile("Luxury Tax"));
        tiles.add(new PropertyTile("Boardwalk", 400, 50, Color.BLUE));
    }

    public ArrayList<Tile> getTiles() { return tiles; }
}

class Tile {
    private String name;

    public Tile(String name) {
        this.name = name;
    }

    public String getName() { return name; }
}

class PropertyTile extends Tile {
    private int price;
    private int rent;
    private Player owner;
    private Color color;

    public PropertyTile(String name, int price, int rent, Color color) {
        super(name);
        this.price = price;
        this.rent = rent;
        this.owner = null;
        this.color = color;
    }

    public int getPrice() { return price; }
    public int getRent() { return rent; }
    public Player getOwner() { return owner; }
    public void setOwner(Player owner) { this.owner = owner; }
    public Color getColor() { return color; }
}

class BoardPanel extends JPanel {
    private MonopolyCloneGame game;

    public BoardPanel() {
        this.game = game;
        setPreferredSize(new Dimension(600, 600));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int tileSize = Math.min(width, height) / 11;

        ArrayList<Tile> tiles = game.board.getTiles();
        for (int i = 0; i < tiles.size(); i++) {
            int x, y;
            if (i < 11) {
                x = (10 - i) * tileSize;
                y = 0;
            } else if (i < 20) {
                x = 0;
                y = (i - 10) * tileSize;
            } else if (i < 31) {
                x = (i - 20) * tileSize;
                y = 10 * tileSize;
            } else {
                x = 10 * tileSize;
                y = (40 - i) * tileSize;
            }

            Tile tile = tiles.get(i);
            if (tile instanceof PropertyTile property) {
                g2d.setColor(property.getColor());
                g2d.fillRect(x, y, tileSize, tileSize / 4);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, tileSize, tileSize);
                drawCenteredString(g2d, tile.getName(), x, y + tileSize / 2, tileSize, tileSize);
            } else {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(x, y, tileSize, tileSize);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, tileSize, tileSize);
                drawCenteredString(g2d, tile.getName(), x, y + tileSize / 2, tileSize, tileSize);
            }
        }

        for (Player player : game.players) {
            int pos = player.getPosition();
            int x, y;
            if (pos < 11) {
                x = (10 - pos) * tileSize + tileSize / 4;
                y = tileSize / 4;
            } else if (pos < 20) {
                x = tileSize / 4;
                y = (pos - 10) * tileSize + tileSize / 4;
            } else if (pos < 31) {
                x = (pos - 20) * tileSize + tileSize / 4;
                y = 10 * tileSize + tileSize / 4;
            } else {
                x = 10 * tileSize + tileSize / 4;
                y = (40 - pos) * tileSize + tileSize / 4;
            }

            g2d.setColor(player.getColor());
            g2d.fillOval(x, y, tileSize / 2, tileSize / 2);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x, y, tileSize / 2, tileSize / 2);
        }
    }

    private void drawCenteredString(Graphics2D g, String text, int x, int y, int width, int height) {
        FontMetrics fm = g.getFontMetrics();
        int textX = x + (width - fm.stringWidth(text)) / 2;
        int textY = y + ((height - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(text, textX, textY);
    }
}

class DicePanel extends JPanel {
    private int die1, die2;
    private boolean isRolling;

    public DicePanel() {
        setPreferredSize(new Dimension(100, 100));
        die1 = 1;
        die2 = 1;
        isRolling = false;
    }

    public void animateDiceRoll(Runnable onComplete) {
        isRolling = true;
        Timer timer = new Timer(100, null);
        timer.addActionListener(new ActionListener() {
            int rolls = 0;
            final int maxRolls = 10;

            @Override
            public void actionPerformed(ActionEvent e) {
                die1 = new Random().nextInt(6) + 1;
                die2 = new Random().nextInt(6) + 1;
                repaint();
                rolls++;
                if (rolls >= maxRolls) {
                    timer.stop();
                    isRolling = false;
                    onComplete.run();
                }
            }
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int dieSize = 30;
        int spacing = 10;

        drawDie(g2d, 10, 10, dieSize, die1);
        drawDie(g2d, 10 + dieSize + spacing, 10, dieSize, die2);
    }

    private void drawDie(Graphics2D g, int x, int y, int size, int value) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, size, size);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, size, size);

        int dotSize = size / 5;
        int offset = size / 4;
        boolean[][] dots = new boolean[3][3];

        switch (value) {
            case 1:
                dots[1][1] = true;
                break;
            case 2:
                dots[0][0] = true;
                dots[2][2] = true;
                break;
            case 3:
                dots[0][0] = true;
                dots[1][1] = true;
                dots[2][2] = true;
                break;
            case 4:
                dots[0][0] = true;
                dots[0][2] = true;
                dots[2][0] = true;
                dots[2][2] = true;
                break;
            case 5:
                dots[0][0] = true;
                dots[0][2] = true;
                dots[1][1] = true;
                dots[2][0] = true;
                dots[2][2] = true;
                break;
            case 6:
                dots[0][0] = true;
                dots[0][2] = true;
                dots[1][0] = true;
                dots[1][2] = true;
                dots[2][0] = true;
                dots[2][2] = true;
                break;
        }

        g.setColor(Color.BLACK);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (dots[i][j]) {
                    g.fillOval(x + offset * (j + 1) - dotSize / 2, y + offset * (i + 1) - dotSize / 2, dotSize, dotSize);
                }
            }
        }
    }
}