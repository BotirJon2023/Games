import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;

public class BoxingSimulationGame extends JFrame {
    private GamePanel gamePanel;
    private JButton punchButton, blockButton, specialButton;
    private JLabel player1HealthLabel, player2HealthLabel, messageLabel;
    private JLabel roundLabel, timerLabel;
    private Timer gameTimer, fightTimer;
    private Boxer player1, player2;
    private boolean isPlayerTurn;
    private int currentRound;
    private int roundTime;
    private boolean gameActive;
    private boolean vsComputer;
    private Random random;

    public BoxingSimulationGame() {
        setTitle("LAS VEGAS BOXING SIMULATION");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // Initialize components
        random = new Random();
        setupMenu();
        setupGamePanel();
        setupControlPanel();
        setupInfoPanel();

        // Initialize with default values to prevent null pointer
        player1 = new Boxer("Player 1", new Color(255, 100, 100));
        player2 = new Boxer("Computer", new Color(100, 100, 255));
        gameActive = false;

        // Show new game dialog after frame is visible
        SwingUtilities.invokeLater(() -> showNewGameDialog());

        setVisible(true);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        JMenuItem newGameItem = new JMenuItem("New Game");
        JMenuItem vsComputerItem = new JMenuItem("vs Computer");
        JMenuItem vsPlayerItem = new JMenuItem("vs Player");
        JMenuItem exitItem = new JMenuItem("Exit");

        newGameItem.addActionListener(e -> showNewGameDialog());
        vsComputerItem.addActionListener(e -> startNewGame(true));
        vsPlayerItem.addActionListener(e -> startNewGame(false));
        exitItem.addActionListener(e -> System.exit(0));

        gameMenu.add(newGameItem);
        gameMenu.add(vsComputerItem);
        gameMenu.add(vsPlayerItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        menuBar.add(gameMenu);

        setJMenuBar(menuBar);
    }

    private void showNewGameDialog() {
        String[] options = {"Play vs Computer", "Two Players"};
        int choice = JOptionPane.showOptionDialog(this,
                "Choose game mode:", "New Game",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) startNewGame(true);
        else if (choice == 1) startNewGame(false);
    }

    private void setupGamePanel() {
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(1000, 500));
        gamePanel.setBackground(new Color(0, 0, 0));
        add(gamePanel, BorderLayout.CENTER);

        // Keyboard controls
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!gameActive || player1 == null || player2 == null) return;

                if (vsComputer) {
                    if (isPlayerTurn) {
                        if (e.getKeyCode() == KeyEvent.VK_P) performPunch();
                        else if (e.getKeyCode() == KeyEvent.VK_B) performBlock();
                        else if (e.getKeyCode() == KeyEvent.VK_S) performSpecial();
                    }
                } else {
                    // Two players mode
                    if (e.getKeyCode() == KeyEvent.VK_P) {
                        if (isPlayerTurn) performPunch();
                        else messageLabel.setText("Player 2's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_B) {
                        if (isPlayerTurn) performBlock();
                        else messageLabel.setText("Player 2's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_S) {
                        if (isPlayerTurn) performSpecial();
                        else messageLabel.setText("Player 2's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_O) {
                        if (!isPlayerTurn) performPunch2();
                        else messageLabel.setText("Player 1's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_N) {
                        if (!isPlayerTurn) performBlock2();
                        else messageLabel.setText("Player 1's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_M) {
                        if (!isPlayerTurn) performSpecial2();
                        else messageLabel.setText("Player 1's turn!");
                    }
                }
            }
        });
    }

    private void setupControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(50, 50, 50));
        controlPanel.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 2));

        punchButton = createStyledButton("PUNCH (P)", new Color(255, 100, 100));
        blockButton = createStyledButton("BLOCK (B)", new Color(100, 100, 255));
        specialButton = createStyledButton("SPECIAL (S)", new Color(255, 215, 0));

        punchButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performPunch(); });
        blockButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performBlock(); });
        specialButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performSpecial(); });

        controlPanel.add(punchButton);
        controlPanel.add(blockButton);
        controlPanel.add(specialButton);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(color);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        return button;
    }

    private void setupInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(1, 5, 10, 10));
        infoPanel.setBackground(new Color(30, 30, 30));
        infoPanel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));

        player1HealthLabel = new JLabel("Player 1 Health: 100%", SwingConstants.CENTER);
        player1HealthLabel.setFont(new Font("Arial", Font.BOLD, 14));
        player1HealthLabel.setForeground(Color.WHITE);

        player2HealthLabel = new JLabel("Player 2 Health: 100%", SwingConstants.CENTER);
        player2HealthLabel.setFont(new Font("Arial", Font.BOLD, 14));
        player2HealthLabel.setForeground(Color.WHITE);

        roundLabel = new JLabel("Round: 1", SwingConstants.CENTER);
        roundLabel.setFont(new Font("Arial", Font.BOLD, 16));
        roundLabel.setForeground(Color.YELLOW);

        timerLabel = new JLabel("Time: 3:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timerLabel.setForeground(Color.CYAN);

        messageLabel = new JLabel("Get ready to fight!", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        messageLabel.setForeground(Color.WHITE);

        infoPanel.add(player1HealthLabel);
        infoPanel.add(player2HealthLabel);
        infoPanel.add(roundLabel);
        infoPanel.add(timerLabel);
        infoPanel.add(messageLabel);

        add(infoPanel, BorderLayout.NORTH);
    }

    private void startNewGame(boolean vsComputer) {
        this.vsComputer = vsComputer;
        player1 = new Boxer("Player 1", new Color(255, 100, 100));
        player2 = new Boxer(vsComputer ? "Computer" : "Player 2", new Color(100, 100, 255));
        currentRound = 1;
        roundTime = 180; // 3 minutes in seconds
        gameActive = true;
        isPlayerTurn = true;

        updateHealthBars();
        roundLabel.setText("Round: 1");

        if (gameTimer != null && gameTimer.isRunning()) gameTimer.stop();
        gameTimer = new Timer(1000, e -> updateTimer());
        gameTimer.start();

        messageLabel.setText("Fight! Player 1's turn!");
        gamePanel.repaint();
        gamePanel.requestFocusInWindow();

        if (vsComputer && !isPlayerTurn) {
            computerTurn();
        }
    }

    private void updateTimer() {
        if (!gameActive) return;

        roundTime--;
        int minutes = roundTime / 60;
        int seconds = roundTime % 60;
        timerLabel.setText(String.format("Time: %d:%02d", minutes, seconds));

        if (roundTime <= 0) {
            nextRound();
        }
    }

    private void nextRound() {
        if (currentRound < 12) {
            currentRound++;
            roundTime = 180;
            roundLabel.setText("Round: " + currentRound);
            messageLabel.setText("Round " + currentRound + " - FIGHT!");
            gamePanel.showRoundFlash();

            // Reset positions
            gamePanel.resetPositions();

            if (vsComputer && !isPlayerTurn) {
                computerTurn();
            }
        } else {
            endGame();
        }
    }

    private void performPunch() {
        if (!gameActive || !isPlayerTurn || player2 == null) return;

        int damage = random.nextInt(15) + 10;
        boolean critical = random.nextInt(100) < 20;
        if (critical) damage *= 2;

        player2.takeDamage(damage);
        updateHealthBars();

        gamePanel.showPunchAnimation(true, critical);
        messageLabel.setText((critical ? "CRITICAL! " : "") + "Player 1 lands a punch for " + damage + " damage!");

        if (player2.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = false;
            messageLabel.setText(vsComputer ? "Computer's turn!" : "Player 2's turn!");

            if (vsComputer) {
                computerTurn();
            }
        }

        gamePanel.repaint();
    }

    private void performBlock() {
        if (!gameActive || !isPlayerTurn) return;

        gamePanel.showBlockAnimation(true);
        messageLabel.setText("Player 1 blocks!");
        player1.setBlocking(true);

        isPlayerTurn = false;
        messageLabel.setText(vsComputer ? "Computer's turn!" : "Player 2's turn!");

        if (vsComputer) {
            computerTurn();
        }

        gamePanel.repaint();
    }

    private void performSpecial() {
        if (!gameActive || !isPlayerTurn || player2 == null) return;

        int damage = random.nextInt(25) + 20;
        player2.takeDamage(damage);
        updateHealthBars();

        gamePanel.showSpecialEffect(true);
        messageLabel.setText("SPECIAL MOVE! " + damage + " damage!");

        if (player2.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = false;
            messageLabel.setText(vsComputer ? "Computer's turn!" : "Player 2's turn!");

            if (vsComputer) {
                computerTurn();
            }
        }

        gamePanel.repaint();
    }

    private void performPunch2() {
        if (!gameActive || isPlayerTurn || player1 == null) return;

        int damage = random.nextInt(15) + 10;
        boolean critical = random.nextInt(100) < 20;
        if (critical) damage *= 2;

        player1.takeDamage(damage);
        updateHealthBars();

        gamePanel.showPunchAnimation(false, critical);
        messageLabel.setText((critical ? "CRITICAL! " : "") + "Player 2 lands a punch for " + damage + " damage!");

        if (player1.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = true;
            messageLabel.setText("Player 1's turn!");
        }

        gamePanel.repaint();
    }

    private void performBlock2() {
        if (!gameActive || isPlayerTurn) return;

        gamePanel.showBlockAnimation(false);
        messageLabel.setText("Player 2 blocks!");
        player2.setBlocking(true);

        isPlayerTurn = true;
        messageLabel.setText("Player 1's turn!");

        gamePanel.repaint();
    }

    private void performSpecial2() {
        if (!gameActive || isPlayerTurn || player1 == null) return;

        int damage = random.nextInt(25) + 20;
        player1.takeDamage(damage);
        updateHealthBars();

        gamePanel.showSpecialEffect(false);
        messageLabel.setText("SPECIAL MOVE! " + damage + " damage!");

        if (player1.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = true;
            messageLabel.setText("Player 1's turn!");
        }

        gamePanel.repaint();
    }

    private void computerTurn() {
        if (!gameActive || isPlayerTurn || player1 == null) return;

        Timer computerTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gameActive || player1 == null) return;

                int action = random.nextInt(100);

                if (action < 60) { // 60% chance to punch
                    int damage = random.nextInt(15) + 10;
                    boolean critical = random.nextInt(100) < 20;
                    if (critical) damage *= 2;

                    if (player1.isBlocking()) {
                        damage /= 2;
                        messageLabel.setText("Computer's punch partially blocked!");
                        player1.setBlocking(false);
                    }

                    player1.takeDamage(damage);
                    updateHealthBars();
                    gamePanel.showPunchAnimation(false, critical);
                    messageLabel.setText((critical ? "CRITICAL! " : "") + "Computer lands punch for " + damage + " damage!");

                    if (player1.getHealth() <= 0) {
                        endGame();
                    }
                } else if (action < 80) { // 20% chance to block
                    gamePanel.showBlockAnimation(false);
                    messageLabel.setText("Computer blocks!");
                    player2.setBlocking(true);
                } else { // 20% chance for special
                    int damage = random.nextInt(25) + 20;
                    player1.takeDamage(damage);
                    updateHealthBars();
                    gamePanel.showSpecialEffect(false);
                    messageLabel.setText("COMPUTER SPECIAL MOVE! " + damage + " damage!");

                    if (player1.getHealth() <= 0) {
                        endGame();
                    }
                }

                isPlayerTurn = true;
                messageLabel.setText("Your turn!");
                gamePanel.repaint();

                ((Timer)e.getSource()).stop();
            }
        });

        computerTimer.setRepeats(false);
        computerTimer.start();
    }

    private void updateHealthBars() {
        if (player1 == null || player2 == null) return;

        player1HealthLabel.setText(String.format("Player 1 Health: %.0f%%", player1.getHealth()));
        player2HealthLabel.setText(String.format("%s Health: %.0f%%", vsComputer ? "Computer" : "Player 2", player2.getHealth()));

        // Color coding health bars
        if (player1.getHealth() < 30) player1HealthLabel.setForeground(Color.RED);
        else if (player1.getHealth() < 60) player1HealthLabel.setForeground(Color.YELLOW);
        else player1HealthLabel.setForeground(Color.GREEN);

        if (player2.getHealth() < 30) player2HealthLabel.setForeground(Color.RED);
        else if (player2.getHealth() < 60) player2HealthLabel.setForeground(Color.YELLOW);
        else player2HealthLabel.setForeground(Color.GREEN);
    }

    private void endGame() {
        gameActive = false;
        if (gameTimer != null) gameTimer.stop();

        String winner;
        if (player1 == null || player1.getHealth() <= 0) {
            winner = vsComputer ? "Computer" : "Player 2";
        } else {
            winner = "Player 1";
        }

        int option = JOptionPane.showConfirmDialog(this,
                winner + " wins! Play again?",
                "Game Over",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            showNewGameDialog();
        } else {
            System.exit(0);
        }
    }

    class Boxer {
        private String name;
        private double health;
        private boolean isBlocking;
        private Color color;

        public Boxer(String name, Color color) {
            this.name = name;
            this.color = color;
            this.health = 100;
            this.isBlocking = false;
        }

        public void takeDamage(int damage) {
            if (isBlocking) {
                damage /= 2;
                isBlocking = false;
            }
            health = Math.max(0, health - damage);
        }

        public double getHealth() { return health; }
        public void setBlocking(boolean blocking) { isBlocking = blocking; }
        public boolean isBlocking() { return isBlocking; }
        public Color getColor() { return color; }
        public String getName() { return name; }
    }

    class GamePanel extends JPanel {
        private int flashAlpha = 0;
        private boolean showPunch = false;
        private boolean showBlock = false;
        private boolean showSpecial = false;
        private boolean isLeftPunch = true;
        private boolean isCritical = false;
        private int roundFlashAlpha = 0;
        private int player1X = 200, player2X = 700;
        private int player1Y = 300, player2Y = 300;

        public GamePanel() {
            Timer animationTimer = new Timer(50, e -> {
                if (flashAlpha > 0) flashAlpha -= 15;
                if (roundFlashAlpha > 0) roundFlashAlpha -= 10;
                if (showPunch || showBlock || showSpecial) {
                    if (flashAlpha <= 0) {
                        showPunch = false;
                        showBlock = false;
                        showSpecial = false;
                    }
                }
                repaint();
            });
            animationTimer.start();
        }

        public void resetPositions() {
            player1X = 200;
            player2X = 700;
        }

        public void showPunchAnimation(boolean left, boolean critical) {
            flashAlpha = 255;
            showPunch = true;
            isLeftPunch = left;
            isCritical = critical;

            // Recoil effect
            if (left) {
                player2X += 20;
                Timer recoil = new Timer(100, e -> { player2X -= 20; ((Timer)e.getSource()).stop(); repaint(); });
                recoil.setRepeats(false);
                recoil.start();
            } else {
                player1X -= 20;
                Timer recoil = new Timer(100, e -> { player1X += 20; ((Timer)e.getSource()).stop(); repaint(); });
                recoil.setRepeats(false);
                recoil.start();
            }
        }

        public void showBlockAnimation(boolean left) {
            flashAlpha = 150;
            showBlock = true;
            isLeftPunch = left;
        }

        public void showSpecialEffect(boolean left) {
            flashAlpha = 255;
            showSpecial = true;
            isLeftPunch = left;

            // Extra screen shake
            Timer shake = new Timer(50, new ActionListener() {
                int shakes = 0;
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (shakes++ < 5) {
                        setLocation(getX() + (random.nextBoolean() ? 5 : -5), getY());
                    } else {
                        setLocation(0, 0);
                        ((Timer)e.getSource()).stop();
                    }
                }
            });
            shake.start();
        }

        public void showRoundFlash() {
            roundFlashAlpha = 255;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            // Las Vegas background
            GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 20, 40),
                    getWidth(), getHeight(), new Color(60, 20, 20));
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Neon lights
            for (int i = 0; i < 20; i++) {
                g2d.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
                g2d.fillRect(50 + i * 45, 20, 30, 10);
                g2d.fillRect(50 + i * 45, getHeight() - 40, 30, 10);
            }

            // Ring
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillRect(100, 100, 800, 350);
            g2d.setColor(new Color(210, 180, 140));
            g2d.fillRect(110, 110, 780, 330);

            // Ring ropes
            g2d.setColor(Color.RED);
            for (int i = 0; i < 3; i++) {
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(100, 150 + i * 100, 900, 150 + i * 100);
            }

            // Draw boxers (with null checks)
            if (player1 != null) {
                drawBoxer(g2d, player1X, player1Y, player1.getColor(), player1.getName(), player1.getHealth());
            }
            if (player2 != null) {
                drawBoxer(g2d, player2X, player2Y, player2.getColor(), player2.getName(), player2.getHealth());
            }

            // Health bars above boxers
            if (player1 != null) {
                drawHealthBar(g2d, player1X - 50, player1Y - 40, 100, 15, player1.getHealth(), player1.getColor());
            }
            if (player2 != null) {
                drawHealthBar(g2d, player2X - 50, player2Y - 40, 100, 15, player2.getHealth(), player2.getColor());
            }

            // Block indicator
            if (player1 != null && player1.isBlocking()) {
                g2d.setColor(new Color(0, 255, 255, 150));
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.drawString("BLOCKING!", player1X - 30, player1Y - 60);
            }
            if (player2 != null && player2.isBlocking()) {
                g2d.setColor(new Color(0, 255, 255, 150));
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.drawString("BLOCKING!", player2X - 30, player2Y - 60);
            }

            // Flash effects
            if (flashAlpha > 0) {
                if (showPunch) {
                    g2d.setColor(new Color(255, isCritical ? 0 : 255, 0, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    // Punch impact text
                    g2d.setColor(new Color(255, 255, 0, flashAlpha));
                    g2d.setFont(new Font("Arial", Font.BOLD, 30));
                    String impactText = isCritical ? "CRITICAL HIT!!!" : "POW!";
                    g2d.drawString(impactText, getWidth()/2 - 100, getHeight()/2);
                } else if (showBlock) {
                    g2d.setColor(new Color(0, 255, 255, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 30));
                    g2d.drawString("BLOCK!", getWidth()/2 - 60, getHeight()/2);
                } else if (showSpecial) {
                    g2d.setColor(new Color(255, 215, 0, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    g2d.setColor(Color.RED);
                    g2d.setFont(new Font("Arial", Font.BOLD, 40));
                    g2d.drawString("SPECIAL MOVE!", getWidth()/2 - 130, getHeight()/2);
                }
            }

            if (roundFlashAlpha > 0) {
                g2d.setColor(new Color(255, 255, 255, roundFlashAlpha));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Arial", Font.BOLD, 60));
                g2d.drawString("ROUND " + currentRound, getWidth()/2 - 100, getHeight()/2);
            }
        }

        private void drawBoxer(Graphics2D g, int x, int y, Color color, String name, double health) {
            // Body
            g.setColor(color);
            g.fillOval(x - 30, y - 50, 60, 80);
            g.fillRect(x - 25, y - 30, 50, 80);

            // Head
            g.setColor(color.darker());
            g.fillOval(x - 25, y - 70, 50, 50);

            // Eyes
            g.setColor(Color.WHITE);
            g.fillOval(x - 15, y - 60, 10, 10);
            g.fillOval(x + 5, y - 60, 10, 10);
            g.setColor(Color.BLACK);
            g.fillOval(x - 13, y - 58, 6, 6);
            g.fillOval(x + 7, y - 58, 6, 6);

            // Mouth
            g.setColor(Color.BLACK);
            g.drawArc(x - 10, y - 45, 20, 15, 0, -180);

            // Gloves
            g.setColor(Color.RED);
            g.fillOval(x - 45, y - 20, 30, 30);
            g.fillOval(x + 15, y - 20, 30, 30);

            // Name
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString(name, x - 25, y - 85);

            // Damage indication
            if (health < 30) {
                g.setColor(Color.RED);
                for (int i = 0; i < 3; i++) {
                    g.drawLine(x - 30 + random.nextInt(60), y - 70 + random.nextInt(50),
                            x - 30 + random.nextInt(60), y - 70 + random.nextInt(50));
                }
            }
        }

        private void drawHealthBar(Graphics2D g, int x, int y, int width, int height, double health, Color color) {
            g.setColor(Color.GRAY);
            g.fillRect(x, y, width, height);

            int healthWidth = (int)(width * (health / 100));
            g.setColor(health > 60 ? Color.GREEN : health > 30 ? Color.YELLOW : Color.RED);
            g.fillRect(x, y, healthWidth, height);

            g.setColor(Color.WHITE);
            g.drawRect(x, y, width, height);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BoxingSimulationGame());
    }
}