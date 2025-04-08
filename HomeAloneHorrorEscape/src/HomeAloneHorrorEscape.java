import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class HomeAloneHorrorEscape extends JFrame {
    private JPanel gamePanel;
    private JTextArea storyText;
    private JButton[] choiceButtons;
    private JLabel playerStats;
    private Player player;
    private Room currentRoom;
    private Ghost ghost;
    private Timer animationTimer;
    private int ghostX = 0;
    private boolean ghostMovingRight = true;

    public HomeAloneHorrorEscape() {
        initializeGame();
        setupUI();
        startGame();
    }

    // Player class to track stats
    class Player {
        private int fearLevel;
        private int energy;
        private int courage;
        private String inventory;

        public Player() {
            fearLevel = 0;
            energy = 100;
            courage = 50;
            inventory = "Flashlight";
        }

        public String getStats() {
            return "Fear: " + fearLevel + " | Energy: " + energy + " | Courage: " + courage;
        }

        public void updateFear(int amount) {
            fearLevel = Math.min(100, Math.max(0, fearLevel + amount));
        }

        public void updateEnergy(int amount) {
            energy = Math.min(100, Math.max(0, energy + amount));
        }

        public void updateCourage(int amount) {
            courage = Math.min(100, Math.max(0, courage + amount));
        }
    }

    // Room class for game locations
    class Room {
        private String description;
        private String[] choices;
        private Room[] nextRooms;
        private boolean hasGhost;
        private String event;

        public Room(String desc, String[] choices, boolean hasGhost, String event) {
            this.description = desc;
            this.choices = choices;
            this.nextRooms = new Room[choices.length];
            this.hasGhost = hasGhost;
            this.event = event;
        }
    }

    // Ghost class for antagonist
    class Ghost {
        private int spookiness;
        private String name;

        public Ghost() {
            spookiness = new Random().nextInt(50) + 50;
            String[] names = {"Wailing Widow", "Shadow Stalker", "Creeping Mist"};
            name = names[new Random().nextInt(names.length)];
        }

        public void haunt(Player player) {
            player.updateFear(spookiness / 10);
            player.updateCourage(-spookiness / 20);
        }
    }

    private void initializeGame() {
        player = new Player();
        ghost = new Ghost();

        // Create room network
        Room entrance = new Room(
                "You stand in the dark entrance hall. Shadows dance on the walls.",
                new String[]{"Go to kitchen", "Climb stairs", "Check basement"},
                false,
                "A cold breeze brushes your neck"
        );

        Room kitchen = new Room(
                "The kitchen smells of decay. A knife glints on the counter.",
                new String[]{"Take knife", "Return to entrance", "Look in pantry"},
                true,
                "Something rattles in the darkness"
        );

        Room stairs = new Room(
                "The old stairs creak under your feet. Darkness looms above.",
                new String[]{"Go upstairs", "Return to entrance", "Listen carefully"},
                false,
                "You hear faint whispers"
        );

        Room basement = new Room(
                "The basement is damp and cold. Strange symbols cover the walls.",
                new String[]{"Examine symbols", "Return to entrance", "Search for items"},
                true,
                "A shadow moves in the corner"
        );

        // Connect rooms
        entrance.nextRooms[0] = kitchen;
        entrance.nextRooms[1] = stairs;
        entrance.nextRooms[2] = basement;

        kitchen.nextRooms[1] = entrance;
        stairs.nextRooms[1] = entrance;
        basement.nextRooms[1] = entrance;

        currentRoom = entrance;
    }

    private void setupUI() {
        setTitle("Home Alone Horror Escape");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Game panel with custom drawing
        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBackground(g);
                if (currentRoom.hasGhost) {
                    drawGhost(g);
                }
            }
        };
        gamePanel.setPreferredSize(new Dimension(800, 400)); // Fixed line

        // Story text area
        storyText = new JTextArea(10, 40);
        storyText.setEditable(false);
        storyText.setLineWrap(true);
        storyText.setWrapStyleWord(true);

        // Choice buttons
        choiceButtons = new JButton[3];
        for (int i = 0; i < 3; i++) {
            choiceButtons[i] = new JButton();
            choiceButtons[i].addActionListener(new ChoiceListener(i));
        }

        // Player stats
        playerStats = new JLabel(player.getStats());

        // Layout setup
        JPanel southPanel = new JPanel(new GridLayout(4, 1));
        southPanel.add(playerStats);
        for (JButton btn : choiceButtons) {
            southPanel.add(btn);
        }

        add(gamePanel, BorderLayout.CENTER);
        add(new JScrollPane(storyText), BorderLayout.NORTH);
        add(southPanel, BorderLayout.SOUTH);

        // Animation timer
        animationTimer = new Timer(50, e -> animateGhost());
        animationTimer.start();
    }

    private void drawBackground(Graphics g) {
        g.setColor(new Color(20, 20, 30));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw some spooky elements
        g.setColor(new Color(50, 50, 70));
        for (int i = 0; i < 5; i++) {
            int x = new Random().nextInt(getWidth());
            int y = new Random().nextInt(getHeight());
            g.fillOval(x, y, 20, 20);
        }
    }

    private void drawGhost(Graphics g) {
        g.setColor(new Color(200, 200, 200, 100));
        g.fillOval(ghostX, getHeight()/2 - 50, 100, 100);
        g.setColor(Color.RED);
        g.fillOval(ghostX + 30, getHeight()/2 - 30, 10, 10);
        g.fillOval(ghostX + 60, getHeight()/2 - 30, 10, 10);
    }

    private void animateGhost() {
        if (ghostMovingRight) {
            ghostX += 5;
            if (ghostX > getWidth() - 100) {
                ghostMovingRight = false;
            }
        } else {
            ghostX -= 5;
            if (ghostX < 0) {
                ghostMovingRight = true;
            }
        }
        gamePanel.repaint();
    }

    private void startGame() {
        updateDisplay();
    }

    private void updateDisplay() {
        storyText.setText(currentRoom.description + "\n\nEvent: " + currentRoom.event);
        playerStats.setText(player.getStats());

        for (int i = 0; i < choiceButtons.length; i++) {
            if (i < currentRoom.choices.length) {
                choiceButtons[i].setText(currentRoom.choices[i]);
                choiceButtons[i].setVisible(true);
            } else {
                choiceButtons[i].setVisible(false);
            }
        }

        if (currentRoom.hasGhost) {
            ghost.haunt(player);
        }
    }

    class ChoiceListener implements ActionListener {
        private int choice;

        public ChoiceListener(int choice) {
            this.choice = choice;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentRoom.nextRooms[choice] != null) {
                currentRoom = currentRoom.nextRooms[choice];
            } else {
                handleChoice(choice);
            }
            updateDisplay();
            checkGameOver();
        }
    }

    private void handleChoice(int choice) {
        Random rand = new Random();

        switch (currentRoom.description.split(" ")[2]) {
            case "kitchen":
                if (choice == 0) {
                    player.inventory += ", Knife";
                    storyText.append("\nYou picked up the rusty knife.");
                }
                break;
            case "stairs":
                if (choice == 2) {
                    storyText.append("\nThe whispers grow louder...");
                    player.updateFear(10);
                }
                break;
            case "basement":
                if (choice == 0) {
                    storyText.append("\nThe symbols pulse with dark energy.");
                    player.updateEnergy(-20);
                }
                break;
        }
    }

    private void checkGameOver() {
        if (player.fearLevel >= 100) {
            gameOver("You succumbed to fear!");
        } else if (player.energy <= 0) {
            gameOver("You collapsed from exhaustion!");
        } else if (player.courage <= 0) {
            gameOver("You lost all courage and fled!");
        }
    }

    private void gameOver(String message) {
        animationTimer.stop();
        storyText.setText("GAME OVER\n" + message);
        for (JButton btn : choiceButtons) {
            btn.setEnabled(false);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HomeAloneHorrorEscape game = new HomeAloneHorrorEscape();
            game.setVisible(true);
        });
    }
}