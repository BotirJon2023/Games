import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class WrestlingGame extends JFrame {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private GamePanel gamePanel;
    private Wrestler player;
    private Wrestler opponent;
    private boolean playerTurn = true;
    private Random random = new Random();
    private JLabel statusLabel;
    private JButton punchButton, kickButton, blockButton;
    private Timer animationTimer;

    public WrestlingGame() {
        setTitle("Virtual Wrestling Game");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JEXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        player = new Wrestler("Player", 100, 10, 15, 5);
        opponent = new Wrestler("Opponent", 100, 8, 12, 6);
        gamePanel = new GamePanel();
        setupUI();
        animationTimer = new Timer(50, e -> gamePanel.repaint());
        animationTimer.start();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        gamePanel.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT - 100));
        add(gamePanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        punchButton = new JButton("Punch");
        kickButton = new JButton("Kick");
        blockButton = new JButton("Block");
        statusLabel = new JLabel("Game Started! Player's Turn", SwingConstants.CENTER);

        punchButton.addActionListener(e -> performPlayerMove("Punch"));
        kickButton.addActionListener(e -> performPlayerMove("Kick"));
        blockButton.addActionListener(e -> performPlayerMove("Block"));

        controlPanel.add(punchButton);
        controlPanel.add(kickButton);
        controlPanel.add(blockButton);
        add(controlPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);

        updateButtonState(true);
    }

    private void performPlayerMove(String move) {
        if (!playerTurn) return;
        updateButtonState(false);
        String result = player.performMove(opponent, move);
        statusLabel.setText(result);
        gamePanel.startAnimation(move, true);
        checkGameState();
        if (opponent.getHealth() > 0) {
            new Timer(1000, e -> performOpponentMove()).start();
        }
    }

    private void performOpponentMove() {
        String[] moves = {"Punch", "Kick", "Block"};
        String move = moves[random.nextInt(moves.length)];
        String result = opponent.performMove(player, move);
        statusLabel.setText("Opponent: " + result);
        gamePanel.startAnimation(move, false);
        playerTurn = true;
        updateButtonState(true);
        checkGameState();
    }

    private void updateButtonState(boolean enabled) {
        punchButton.setEnabled(enabled);
        kickButton.setEnabled(enabled);
        blockButton.setEnabled(enabled);
    }

    private void checkGameState() {
        if (player.getHealth() <= 0) {
            statusLabel.setText("Game Over! Opponent Wins!");
            endGame();
        } else if (opponent.getHealth() <= 0) {
            statusLabel.setText("Victory! Player Wins!");
            endGame();
        }
    }

    private void endGame() {
        updateButtonState(false);
        animationTimer.stop();
    }

    class Wrestler {
        private String name;
        private int health;
        private int punchDamage;
        private int kickDamage;
        private int blockDefense;
        private boolean isBlocking;

        public Wrestler(String name, int health, int punchDamage, int kickDamage, int blockDefense) {
            this.name = name;
            this.health = health;
            this.punchDamage = punchDamage;
            this.kickDamage = kickDamage;
            this.blockDefense = blockDefense;
            this.isBlocking = false;
        }

        public String performMove(Wrestler target, String move) {
            isBlocking = false;
            String result;
            switch (move.toLowerCase()) {
                case "punch":
                    int damage = target.isBlocking ? Math.max(0, punchDamage - target.blockDefense) : punchDamage;
                    target.takeDamage(damage);
                    result = name + " punches for " + damage + " damage!";
                    break;
                case "kick":
                    damage = target.isBlocking ? Math.max(0, kickDamage - target.blockDefense) : kickDamage;
                    target.takeDamage(damage);
                    result = name + " kicks for " + damage + " damage!";
                    break;
                case "block":
                    isBlocking = true;
                    result = name + " is blocking!";
                    break;
                default:
                    result = "Invalid move!";
            }
            return result;
        }

        public void takeDamage(int damage) {
            health = Math.max(0, health - damage);
        }

        public int getHealth() {
            return health;
        }

        public String getName() {
            return name;
        }
    }

    class GamePanel extends JPanel {
        private int playerX = 150;
        private int opponentX = 550;
        private int playerAnimFrame = 0;
        private int opponentAnimFrame = 0;
        private String currentMove;
        private boolean isPlayerMove;
        private boolean animating = false;
        private int animCounter = 0;
        private final int ANIM_FRAMES = 10;

        public GamePanel() {
            setBackground(Color.BLACK);
        }

        public void startAnimation(String move, boolean isPlayer) {
            currentMove = move;
            isPlayerMove = isPlayer;
            animating = true;
            animCounter = 0;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw arena
            g2d.setColor(Color.GRAY);
            g2d.fillRect(50, 300, 700, 200);

            // Draw health bars
            drawHealthBar(g2d, player, 50, 50);
            drawHealthBar(g2d, opponent, 550, 50);

            // Draw wrestlers
            drawWrestler(g2d, playerX, 350, playerAnimFrame, true);
            drawWrestler(g2d, opponentX, 350, opponentAnimFrame, false);

            // Handle animation
            if (animating) {
                animate(g2d);
            }
        }

        private void drawHealthBar(Graphics2D g2d, Wrestler wrestler, int x, int y) {
            g2d.setColor(Color.RED);
            g2d.fillRect(x, y, 200, 20);
            g2d.setColor(Color.GREEN);
            int healthWidth = (int) (200 * (wrestler.getHealth() / 100.0));
            g2d.fillRect(x, y, healthWidth, 20);
            g2d.setColor(Color.WHITE);
            g2d.drawString(wrestler.getName() + ": " + wrestler.getHealth() + " HP", x, y - 10);
        }

        private void drawWrestler(Graphics2D g2d, int x, int y, int frame, boolean isPlayer) {
            int size = 100;
            g2d.setColor(isPlayer ? Color.BLUE : Color.RED);
            // Head
            g2d.fillOval(x - 25, y - 100, 50, 50);
            // Body
            g2d.fillRect(x - 25, y - 50, 50, 100);
            // Arms
            int armOffset = frame % 2 == 0 ? 10 : -10;
            g2d.fillRect(x - 45, y - 50, 20, 60);
            g2d.fillRect(x + 25, y - 50, 20, 60);
            // Legs
            g2d.fillRect(x - 20, y + 50, 15, 60);
            g2d.fillRect(x + 5, y + 50, 15, 60);

            // Animation effects
            if (animating && isPlayer == isPlayerMove) {
                if (currentMove.equalsIgnoreCase("punch")) {
                    g2d.setColor(Color.YELLOW);
                    g2d.fillOval(x + (isPlayer ? 50 : -50), y - 50, 20, 20);
                } else if (currentMove.equalsIgnoreCase("kick")) {
                    g2d.setColor(Color.ORANGE);
                    g2d.fillRect(x + (isPlayer ? 50 : -50), y + 50, 20, 20);
                } else if (currentMove.equalsIgnoreCase("block")) {
                    g2d.setColor(Color.WHITE);
                    g2d.drawRect(x - 30, y - 100, 60, 160);
                }
            }
        }

        private void animate(Graphics2D g2d) {
            animCounter++;
            if (isPlayerMove) {
                playerAnimFrame = animCounter % ANIM_FRAMES;
                if (currentMove.equalsIgnoreCase("punch") || currentMove.equalsIgnoreCase("kick")) {
                    playerX += (animCounter < ANIM_FRAMES / 2) ? 10 : -10;
                }
            } else {
                opponentAnimFrame = animCounter % ANIM_FRAMES;
                if (currentMove.equalsIgnoreCase("punch") || currentMove.equalsIgnoreCase("kick")) {
                    opponentX += (animCounter < ANIM_FRAMES / 2) ? -10 : 10;
                }
            }
            if (animCounter >= ANIM_FRAMES) {
                animating = false;
                playerX = 150;
                opponentX = 550;
                playerAnimFrame = 0;
                opponentAnimFrame = 0;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WrestlingGame game = new WrestlingGame();
            game.setVisible(true);
        });
    }

    // Additional utility methods for extended functionality
    private void resetGame() {
        player = new Wrestler("Player", 100, 10, 15, 5);
        opponent = new Wrestler("Opponent", 100, 8, 12, 6);
        playerTurn = true;
        statusLabel.setText("Game Reset! Player's Turn");
        updateButtonState(true);
        animationTimer.start();
        gamePanel.repaint();
    }

    private void saveGameState() {
        // Placeholder for saving game state
        statusLabel.setText("Game state saved (placeholder).");
    }

    private void loadGameState() {
        // Placeholder for loading game state
        statusLabel.setText("Game state loaded (placeholder).");
    }

    private void updateScoreboard() {
        // Placeholder for updating scoreboard
        statusLabel.setText("Scoreboard updated (placeholder).");
    }

    private void playSoundEffect(String move) {
        // Placeholder for sound effects
        System.out.println("Playing sound for: " + move);
    }

    private void displayGameRules() {
        JOptionPane.showMessageDialog(this,
                "Wrestling Game Rules:\n" +
                        "1. Take turns to attack or block.\n" +
                        "2. Punch deals moderate damage.\n" +
                        "3. Kick deals high damage.\n" +
                        "4. Block reduces incoming damage.\n" +
                        "5. Reduce opponent's health to zero to win!",
                "Game Rules",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void initializeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        JMenuItem newGame = new JMenuItem("New Game");
        JMenuItem saveGame = new JMenuItem("Save Game");
        JMenuItem loadGame = new JMenuItem("Load Game");
        JMenuItem rules = new JMenuItem("Rules");

        newGame.addActionListener(e -> resetGame());
        saveGame.addActionListener(e -> saveGameState());
        loadGame.addActionListener(e -> loadGameState());
        rules.addActionListener(e -> displayGameRules());

        gameMenu.add(newGame);
        gameMenu.add(saveGame);
        gameMenu.add(loadGame);
        gameMenu.add(rules);
        menuBar.add(gameMenu);
        setJMenuBar(menuBar);
    }

    // Extended wrestler attributes
    class WrestlerAttributes {
        private int strength;
        private int agility;
        private int stamina;

        public WrestlerAttributes(int strength, int agility, int stamina) {
            this.strength = strength;
            this.agility = agility;
            this.stamina = stamina;
        }

        public int getStrength() {
            return strength;
        }

        public int getAgility() {
            return agility;
        }

        public int getStamina() {
            return stamina;
        }
    }

    // Extended game mechanics
    private void applySpecialMove(Wrestler attacker, Wrestler target, String move) {
        int damage = 0;
        switch (move.toLowerCase()) {
            case "super punch":
                damage = attacker.getHealth() > 50 ? 20 : 10;
                playSoundEffect("super_punch");
                break;
            case "mega kick":
                damage = attacker.getHealth() > 50 ? 25 : 15;
                playSoundEffect("mega_kick");
                break;
        }
        target.takeDamage(damage);
        statusLabel.setText(attacker.getName() + " used " + move + " for " + damage + " damage!");
        gamePanel.startAnimation(move, attacker == player);
    }

    // AI decision-making
    private String decideOpponentMove() {
        if (opponent.getHealth() < 30 && random.nextDouble() < 0.3) {
            return "block";
        } else if (opponent.getHealth() > 70 && random.nextDouble() < 0.2) {
            return random.nextBoolean() ? "super punch" : "mega kick";
        } else {
            return random.nextBoolean() ? "punch" : "kick";
        }
    }

    // Animation utilities
    private void drawAnimatedEffect(Graphics2D g2d, int x, int y, String effectType) {
        g2d.setColor(effectType.equals("hit") ? Color.RED : Color.YELLOW);
        g2d.fillOval(x, y, 30, 30);
    }

    private void drawBackgroundElements(Graphics2D g2d) {
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, WINDOW_WIDTH, 100); // Sky
        g2d.setColor(Color.GREEN);
        g2d.fillRect(0, 500, WINDOW_WIDTH, 100); // Ground
    }

    // Additional game state checks
    private boolean isGameActive() {
        return player.getHealth() > 0 && opponent.getHealth() > 0;
    }

    private void updateGameUI() {
        gamePanel.repaint();
        statusLabel.setText(playerTurn ? "Player's Turn" : "Opponent's Turn");
    }

    // Extended initialization
    {
        initializeMenuBar();
        player = new Wrestler("Player", 100, 10, 15, 5);
        opponent = new Wrestler("Opponent", 100, 8, 12, 6);
        WrestlerAttributes playerAttrs = new WrestlerAttributes(10, 8, 12);
        WrestlerAttributes opponentAttrs = new WrestlerAttributes(8, 10, 10);
    }

    // Extended animation frames
    private void updateAnimationFrames() {
        boolean animating;
        if (animating) {
            int animCounter;
            animCounter++;
            boolean isPlayerMove;
            if (isPlayerMove) {
                Object playerAnimFrame = animCounter % ANIM_FRAMES;
            } else {
                opponentAnimFrame = animCounter % ANIM_FRAMES;
            }
        }
    }

    // Extended input handling
    private void setupKeyBindings() {
        InputMap inputMap = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = gamePanel.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "punch");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0), "kick");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "block");

        actionMap.put("punch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performPlayerMove("Punch");
            }
        });
        actionMap.put("kick", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performPlayerMove("Kick");
            }
        });
        actionMap.put("block", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performPlayerMove("Block");
            }
        });
    }

    // Extended game loop
    private void gameLoop() {
        if (isGameActive()) {
            updateAnimationFrames();
            updateGameUI();
        }
    }

    // Extended initialization for key bindings
    {
        setupKeyBindings();
    }
}
