import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.util.ArrayList;

public class PyramidBoxingGame extends JFrame {
    private GamePanel gamePanel;
    private JButton punchButton, blockButton, specialButton;
    private JLabel player1HealthLabel, player2HealthLabel, messageLabel;
    private JLabel roundLabel, timerLabel, powerLabel;
    private Timer gameTimer;
    private Boxer player1, player2;
    private boolean isPlayerTurn;
    private int currentRound;
    private int roundTime;
    private boolean gameActive;
    private boolean vsComputer;
    private Random random;
    private int powerCharge;

    public PyramidBoxingGame() {
        setTitle("⚱️ PYRAMID BOXING - Battle at Giza ⚱️");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1200, 750);
        setLocationRelativeTo(null);

        random = new Random();
        setupMenu();
        setupGamePanel();
        setupControlPanel();
        setupInfoPanel();

        // Initialize placeholder boxers
        player1 = new Boxer("Pharaoh", new Color(255, 215, 0), "⚱️");
        player2 = new Boxer("Anubis", new Color(100, 50, 200), "🐺");
        gameActive = false;

        SwingUtilities.invokeLater(() -> showNewGameDialog());
        setVisible(true);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(80, 50, 30));

        JMenu gameMenu = new JMenu("🔺 GAME");
        gameMenu.setForeground(new Color(255, 215, 0));
        JMenuItem newGameItem = new JMenuItem("✨ New Game");
        JMenuItem vsComputerItem = new JMenuItem("🤖 vs Computer");
        JMenuItem vsPlayerItem = new JMenuItem("👥 vs Player");
        JMenuItem exitItem = new JMenuItem("✖ Exit");

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
        String[] options = {"🐫 Fight Computer", "👥 Two Players"};
        int choice = JOptionPane.showOptionDialog(this,
                "Choose your battle:", "PYRAMID BOXING",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) startNewGame(true);
        else if (choice == 1) startNewGame(false);
    }

    private void setupGamePanel() {
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(1200, 550));
        gamePanel.setBackground(new Color(210, 180, 140));
        add(gamePanel, BorderLayout.CENTER);

        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!gameActive) return;

                if (vsComputer) {
                    if (isPlayerTurn) {
                        if (e.getKeyCode() == KeyEvent.VK_J) performPunch();
                        else if (e.getKeyCode() == KeyEvent.VK_K) performBlock();
                        else if (e.getKeyCode() == KeyEvent.VK_L) performSpecial();
                    }
                } else {
                    if (e.getKeyCode() == KeyEvent.VK_J) {
                        if (isPlayerTurn) performPunch();
                        else messageLabel.setText("🔺 Player 2's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_K) {
                        if (isPlayerTurn) performBlock();
                        else messageLabel.setText("🔺 Player 2's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_L) {
                        if (isPlayerTurn) performSpecial();
                        else messageLabel.setText("🔺 Player 2's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_A) {
                        if (!isPlayerTurn) performPunch2();
                        else messageLabel.setText("🔺 Player 1's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_S) {
                        if (!isPlayerTurn) performBlock2();
                        else messageLabel.setText("🔺 Player 1's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_D) {
                        if (!isPlayerTurn) performSpecial2();
                        else messageLabel.setText("🔺 Player 1's turn!");
                    }
                }
            }
        });
    }

    private void setupControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(139, 69, 19));
        controlPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0), 3));

        punchButton = createPyramidButton("👊 PUNCH (J)", new Color(255, 100, 50));
        blockButton = createPyramidButton("🛡️ BLOCK (K)", new Color(100, 150, 255));
        specialButton = createPyramidButton("✨ SPECIAL (L)", new Color(255, 215, 0));

        punchButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performPunch(); });
        blockButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performBlock(); });
        specialButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performSpecial(); });

        controlPanel.add(punchButton);
        controlPanel.add(blockButton);
        controlPanel.add(specialButton);

        JLabel controls2p = new JLabel("  Two-Player: P2 uses A(Punch) | S(Block) | D(Special)");
        controls2p.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        controls2p.setForeground(new Color(255, 215, 0));
        controlPanel.add(controls2p);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private JButton createPyramidButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setBackground(color);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 215, 0), 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        return button;
    }

    private void setupInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(2, 4, 15, 5));
        infoPanel.setBackground(new Color(80, 50, 30));
        infoPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0), 2));

        player1HealthLabel = new JLabel("❤️ PHARAOH: 100%", SwingConstants.CENTER);
        player1HealthLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        player1HealthLabel.setForeground(Color.WHITE);

        player2HealthLabel = new JLabel("❤️ ANUBIS: 100%", SwingConstants.CENTER);
        player2HealthLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        player2HealthLabel.setForeground(Color.WHITE);

        roundLabel = new JLabel("🔺 ROUND 1", SwingConstants.CENTER);
        roundLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        roundLabel.setForeground(new Color(255, 215, 0));

        timerLabel = new JLabel("⏱️ 3:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        timerLabel.setForeground(Color.CYAN);

        powerLabel = new JLabel("✨ POWER: 0%", SwingConstants.CENTER);
        powerLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        powerLabel.setForeground(new Color(255, 100, 255));

        messageLabel = new JLabel("🔺 BATTLE AT GIZA! 🔺", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        messageLabel.setForeground(new Color(255, 215, 0));

        infoPanel.add(player1HealthLabel);
        infoPanel.add(player2HealthLabel);
        infoPanel.add(roundLabel);
        infoPanel.add(timerLabel);
        infoPanel.add(powerLabel);
        infoPanel.add(new JLabel());
        infoPanel.add(messageLabel);
        infoPanel.add(new JLabel());

        add(infoPanel, BorderLayout.NORTH);
    }

    private void startNewGame(boolean vsComputer) {
        this.vsComputer = vsComputer;
        player1 = new Boxer("Pharaoh", new Color(255, 215, 0), "⚱️");
        player2 = new Boxer(vsComputer ? "Anubis" : "Sphinx",
                vsComputer ? new Color(100, 50, 200) : new Color(0, 150, 200),
                vsComputer ? "🐺" : "🦁");
        currentRound = 1;
        roundTime = 180;
        gameActive = true;
        isPlayerTurn = true;
        powerCharge = 0;

        updateUI();
        roundLabel.setText("🔺 ROUND " + currentRound);

        if (gameTimer != null && gameTimer.isRunning()) gameTimer.stop();
        gameTimer = new Timer(1000, e -> updateTimer());
        gameTimer.start();

        messageLabel.setText("🔺 FIGHT! Your turn! 🔺");
        gamePanel.resetPositions();
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
        timerLabel.setText(String.format("⏱️ %d:%02d", minutes, seconds));

        if (roundTime <= 0) {
            nextRound();
        }
    }

    private void nextRound() {
        if (currentRound < 12) {
            currentRound++;
            roundTime = 180;
            roundLabel.setText("🔺 ROUND " + currentRound);
            messageLabel.setText("🔺 ROUND " + currentRound + " - FIGHT! 🔺");
            gamePanel.showRoundFlash();
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

        int damage = random.nextInt(12) + 8;
        boolean critical = random.nextInt(100) < 15;
        if (critical) damage *= 2;

        player2.takeDamage(damage);
        powerCharge = Math.min(100, powerCharge + 5);
        updatePowerBar();

        updateUI();
        gamePanel.showPunchAnimation(true, critical);

        if (critical) {
            messageLabel.setText("✨ ANCIENT POWER! " + damage + " damage! ✨");
        } else {
            messageLabel.setText("👊 PUNCH! " + damage + " damage!");
        }

        if (player2.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = false;
            messageLabel.setText(vsComputer ? "🐺 Anubis's turn!" : "🔺 Player 2's turn!");

            if (vsComputer) {
                computerTurn();
            }
        }

        gamePanel.repaint();
    }

    private void performBlock() {
        if (!gameActive || !isPlayerTurn) return;

        gamePanel.showBlockAnimation(true);
        messageLabel.setText("🛡️ PYRAMID SHIELD! 🛡️");
        player1.setBlocking(true);

        isPlayerTurn = false;
        messageLabel.setText(vsComputer ? "🐺 Anubis's turn!" : "🔺 Player 2's turn!");

        if (vsComputer) {
            computerTurn();
        }

        gamePanel.repaint();
    }

    private void performSpecial() {
        if (!gameActive || !isPlayerTurn || player2 == null) return;

        int damage = random.nextInt(25) + 20;
        player2.takeDamage(damage);
        updateUI();

        gamePanel.showSpecialEffect(true);
        messageLabel.setText("⚱️ CURSE OF THE PHARAOH! " + damage + " damage! ⚱️");
        powerCharge = 0;
        updatePowerBar();

        if (player2.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = false;
            messageLabel.setText(vsComputer ? "🐺 Anubis's turn!" : "🔺 Player 2's turn!");

            if (vsComputer) {
                computerTurn();
            }
        }

        gamePanel.repaint();
    }

    private void performPunch2() {
        if (!gameActive || isPlayerTurn || player1 == null) return;

        int damage = random.nextInt(12) + 8;
        boolean critical = random.nextInt(100) < 15;
        if (critical) damage *= 2;

        player1.takeDamage(damage);
        updateUI();

        gamePanel.showPunchAnimation(false, critical);
        messageLabel.setText((critical ? "✨ ANCIENT POWER! ✨ " : "👊 ") + "Player 2 lands " + damage + " damage!");

        if (player1.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = true;
            messageLabel.setText("🔺 Player 1's turn!");
        }

        gamePanel.repaint();
    }

    private void performBlock2() {
        if (!gameActive || isPlayerTurn) return;

        gamePanel.showBlockAnimation(false);
        messageLabel.setText("🛡️ Player 2 blocks!");
        player2.setBlocking(true);

        isPlayerTurn = true;
        messageLabel.setText("🔺 Player 1's turn!");

        gamePanel.repaint();
    }

    private void performSpecial2() {
        if (!gameActive || isPlayerTurn || player1 == null) return;

        int damage = random.nextInt(25) + 20;
        player1.takeDamage(damage);
        updateUI();

        gamePanel.showSpecialEffect(false);
        messageLabel.setText("⚱️ SPHINX MYSTERY! " + damage + " damage! ⚱️");

        if (player1.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = true;
            messageLabel.setText("🔺 Player 1's turn!");
        }

        gamePanel.repaint();
    }

    private void computerTurn() {
        if (!gameActive || isPlayerTurn || player1 == null) return;

        Timer computerTimer = new Timer(800, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gameActive || player1 == null) return;

                int action = random.nextInt(100);

                if (action < 55) {
                    int damage = random.nextInt(12) + 8;
                    boolean critical = random.nextInt(100) < 15;
                    if (critical) damage *= 2;

                    if (player1.isBlocking()) {
                        damage /= 2;
                        messageLabel.setText("🐺 Anubis punch partially blocked!");
                        player1.setBlocking(false);
                    }

                    player1.takeDamage(damage);
                    updateUI();
                    gamePanel.showPunchAnimation(false, critical);
                    messageLabel.setText((critical ? "✨ CRITICAL! ✨ " : "👊 ") + "Anubis hits for " + damage + " damage!");

                    if (player1.getHealth() <= 0) {
                        endGame();
                    }
                } else if (action < 75) {
                    gamePanel.showBlockAnimation(false);
                    messageLabel.setText("🐺 Anubis blocks with jackal shield!");
                    player2.setBlocking(true);
                } else {
                    int damage = random.nextInt(25) + 20;
                    player1.takeDamage(damage);
                    updateUI();
                    gamePanel.showSpecialEffect(false);
                    messageLabel.setText("⚱️ JUDGMENT OF ANUBIS! " + damage + " damage! ⚱️");

                    if (player1.getHealth() <= 0) {
                        endGame();
                    }
                }

                isPlayerTurn = true;
                messageLabel.setText("🔺 Your turn, Pharaoh! 🔺");
                gamePanel.repaint();

                ((Timer)e.getSource()).stop();
            }
        });

        computerTimer.setRepeats(false);
        computerTimer.start();
    }

    private void updateUI() {
        if (player1 == null || player2 == null) return;

        player1HealthLabel.setText(String.format("❤️ PHARAOH: %.0f%%", player1.getHealth()));
        player2HealthLabel.setText(String.format("❤️ %s: %.0f%%", vsComputer ? "ANUBIS" : "SPHINX", player2.getHealth()));

        player1HealthLabel.setForeground(player1.getHealth() < 30 ? Color.RED :
                player1.getHealth() < 60 ? Color.YELLOW : Color.GREEN);
        player2HealthLabel.setForeground(player2.getHealth() < 30 ? Color.RED :
                player2.getHealth() < 60 ? Color.YELLOW : Color.GREEN);
    }

    private void updatePowerBar() {
        powerLabel.setText("✨ POWER: " + powerCharge + "% ✨");
        if (powerCharge >= 100) {
            powerLabel.setForeground(Color.RED);
            powerLabel.setText("✨ SPECIAL READY! ✨");
        } else if (powerCharge >= 50) {
            powerLabel.setForeground(new Color(255, 215, 0));
        } else {
            powerLabel.setForeground(new Color(255, 100, 255));
        }
    }

    private void endGame() {
        gameActive = false;
        if (gameTimer != null) gameTimer.stop();

        String winner;
        if (player1 == null || player1.getHealth() <= 0) {
            winner = vsComputer ? "🐺 ANUBIS" : "🔺 SPHINX";
        } else {
            winner = "⚱️ PHARAOH";
        }

        gamePanel.showVictoryEffect(winner);

        int option = JOptionPane.showConfirmDialog(this,
                "🏆 " + winner + " WINS! 🏆\n\nBattle again at the pyramids?",
                "GAME OVER",
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
        private String symbol;

        public Boxer(String name, Color color, String symbol) {
            this.name = name;
            this.color = color;
            this.symbol = symbol;
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
        public String getSymbol() { return symbol; }
    }

    class GamePanel extends JPanel {
        private int flashAlpha = 0;
        private boolean showPunch = false;
        private boolean showBlock = false;
        private boolean showSpecial = false;
        private boolean isLeftPunch = true;
        private boolean isCritical = false;
        private int roundFlashAlpha = 0;
        private int victoryFlashAlpha = 0;
        private String victorName = "";
        private int player1X = 280, player2X = 820;
        private int player1Y = 300, player2Y = 300;
        private ArrayList<SandParticle> sandParticles = new ArrayList<>();
        private ArrayList<StarParticle> starParticles = new ArrayList<>();

        public GamePanel() {
            Timer animationTimer = new Timer(33, e -> {
                if (flashAlpha > 0) flashAlpha -= 10;
                if (roundFlashAlpha > 0) roundFlashAlpha -= 8;
                if (victoryFlashAlpha > 0) victoryFlashAlpha -= 5;

                for (int i = sandParticles.size() - 1; i >= 0; i--) {
                    sandParticles.get(i).update();
                    if (sandParticles.get(i).life <= 0) sandParticles.remove(i);
                }

                for (int i = starParticles.size() - 1; i >= 0; i--) {
                    starParticles.get(i).update();
                    if (starParticles.get(i).life <= 0) starParticles.remove(i);
                }

                repaint();
            });
            animationTimer.start();
        }

        public void resetPositions() {
            player1X = 280;
            player2X = 820;
        }

        public void showPunchAnimation(boolean left, boolean critical) {
            flashAlpha = 200;
            showPunch = true;
            isLeftPunch = left;
            isCritical = critical;

            // Add sand particles on impact
            for (int i = 0; i < 30; i++) {
                sandParticles.add(new SandParticle(left ? 400 : 800, 350));
            }

            if (left) {
                player2X += 25;
                Timer recoil = new Timer(100, e -> { player2X -= 25; ((Timer)e.getSource()).stop(); repaint(); });
                recoil.setRepeats(false);
                recoil.start();
            } else {
                player1X -= 25;
                Timer recoil = new Timer(100, e -> { player1X += 25; ((Timer)e.getSource()).stop(); repaint(); });
                recoil.setRepeats(false);
                recoil.start();
            }

            Timer clearFlash = new Timer(300, e -> { showPunch = false; ((Timer)e.getSource()).stop(); });
            clearFlash.setRepeats(false);
            clearFlash.start();
        }

        public void showBlockAnimation(boolean left) {
            flashAlpha = 120;
            showBlock = true;
            isLeftPunch = left;

            Timer clearFlash = new Timer(200, e -> { showBlock = false; ((Timer)e.getSource()).stop(); });
            clearFlash.setRepeats(false);
            clearFlash.start();
        }

        public void showSpecialEffect(boolean left) {
            flashAlpha = 200;
            showSpecial = true;
            isLeftPunch = left;

            // Add mystical star particles
            for (int i = 0; i < 50; i++) {
                starParticles.add(new StarParticle(left ? 400 : 800, 350));
            }

            Timer clearFlash = new Timer(400, e -> { showSpecial = false; ((Timer)e.getSource()).stop(); });
            clearFlash.setRepeats(false);
            clearFlash.start();
        }

        public void showRoundFlash() {
            roundFlashAlpha = 200;
        }

        public void showVictoryEffect(String winner) {
            victorName = winner;
            victoryFlashAlpha = 255;

            for (int i = 0; i < 100; i++) {
                starParticles.add(new StarParticle(getWidth()/2, getHeight()/2));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Sky gradient (desert sunset)
            GradientPaint skyGradient = new GradientPaint(0, 0, new Color(255, 140, 50),
                    0, getHeight()/2, new Color(255, 80, 30));
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight()/2);

            // Lower sky
            g2d.setColor(new Color(255, 200, 100));
            g2d.fillRect(0, getHeight()/2, getWidth(), getHeight()/2);

            // Sun
            g2d.setColor(new Color(255, 100, 50, 150));
            g2d.fillOval(getWidth() - 150, 50, 100, 100);

            // Giza Pyramids
            drawPyramid(g2d, 150, getHeight() - 250, 200, 200, new Color(210, 180, 140));
            drawPyramid(g2d, 320, getHeight() - 280, 180, 180, new Color(200, 170, 130));
            drawPyramid(g2d, 60, getHeight() - 220, 150, 150, new Color(190, 160, 120));

            // Sphinx silhouette
            g2d.setColor(new Color(180, 150, 110));
            g2d.fillRect(450, getHeight() - 180, 80, 60);
            g2d.fillOval(440, getHeight() - 200, 100, 50);

            // Desert ground
            g2d.setColor(new Color(210, 180, 140));
            g2d.fillRect(0, getHeight() - 150, getWidth(), 150);

            // Sand dunes
            g2d.setColor(new Color(200, 170, 130));
            for (int i = 0; i < 5; i++) {
                g2d.fillArc(50 + i * 200, getHeight() - 140, 150, 40, 0, 180);
            }

            // Hieroglyphics decoration
            g2d.setColor(new Color(100, 70, 40, 100));
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 20));
            for (int i = 0; i < 8; i++) {
                g2d.drawString("𓃀𓃥𓃰𓃱", 20 + i * 120, 50);
            }

            // Boxing ring with Egyptian theme
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillRect(100, 100, 1000, 380);
            g2d.setColor(new Color(210, 180, 140));
            g2d.fillRect(110, 110, 980, 360);

            // Ring ropes with ankh symbols
            for (int i = 0; i < 4; i++) {
                g2d.setStroke(new BasicStroke(4));
                int yPos = 130 + i * 80;
                g2d.setColor(new Color(255, 215, 0));
                g2d.drawLine(100, yPos, 1100, yPos);
                g2d.setColor(new Color(200, 100, 50));
                g2d.drawLine(102, yPos + 2, 1098, yPos + 2);
            }

            // Draw boxers
            if (player1 != null) drawEgyptianBoxer(g2d, player1X, player1Y, player1.getColor(), player1.getName(), player1.getHealth(), true, player1.getSymbol());
            if (player2 != null) drawEgyptianBoxer(g2d, player2X, player2Y, player2.getColor(), player2.getName(), player2.getHealth(), false, player2.getSymbol());

            // Health bars with Egyptian style
            if (player1 != null) drawEgyptianHealthBar(g2d, player1X - 80, player1Y - 50, 160, 20, player1.getHealth(), "PHARAOH");
            if (player2 != null) drawEgyptianHealthBar(g2d, player2X - 80, player2Y - 50, 160, 20, player2.getHealth(), vsComputer ? "ANUBIS" : "SPHINX");

            // Block indicators
            if (player1 != null && player1.isBlocking()) {
                drawEgyptianText(g2d, "𓋴 HIEROGLYPH SHIELD 𓋴", player1X - 70, player1Y - 80, new Color(100, 255, 255));
            }
            if (player2 != null && player2.isBlocking()) {
                drawEgyptianText(g2d, "𓋴 HIEROGLYPH SHIELD 𓋴", player2X - 70, player2Y - 80, new Color(100, 255, 255));
            }

            // Sand particles
            for (SandParticle p : sandParticles) {
                g2d.setColor(new Color(210, 180, 140, p.life * 5));
                g2d.fillOval(p.x, p.y, 3, 3);
            }

            // Star particles
            for (StarParticle p : starParticles) {
                g2d.setColor(new Color(255, 215, 0, p.life * 5));
                g2d.fillOval(p.x, p.y, 4, 4);
                g2d.setColor(new Color(255, 255, 255, p.life * 5));
                g2d.fillOval(p.x + 1, p.y + 1, 2, 2);
            }

            // Flash effects
            if (flashAlpha > 0) {
                if (showPunch) {
                    g2d.setColor(new Color(255, isCritical ? 100 : 200, 0, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    g2d.setColor(new Color(255, 255, 0, flashAlpha));
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 40));
                    String impactText = isCritical ? "𓋴 CRITICAL HIT! 𓋴" : "𓂀 POUND! 𓂀";
                    drawCenteredString(g2d, impactText, getWidth()/2, getHeight()/2);
                } else if (showBlock) {
                    g2d.setColor(new Color(0, 200, 255, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    drawEgyptianText(g2d, "𓋴 BLOCKED! 𓋴", getWidth()/2 - 80, getHeight()/2, Color.WHITE);
                } else if (showSpecial) {
                    g2d.setColor(new Color(255, 100, 0, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    drawEgyptianText(g2d, "𓋹 ANCIENT CURSE! 𓋹", getWidth()/2 - 120, getHeight()/2, Color.YELLOW);
                }
            }

            if (roundFlashAlpha > 0) {
                g2d.setColor(new Color(255, 215, 0, roundFlashAlpha));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(255, 100, 50));
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 70));
                drawCenteredString(g2d, "ROUND " + currentRound, getWidth()/2, getHeight()/2);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 30));
                drawCenteredString(g2d, "𓋴 𓋹 𓂀", getWidth()/2, getHeight()/2 + 60);
            }

            if (victoryFlashAlpha > 0) {
                g2d.setColor(new Color(255, 215, 0, victoryFlashAlpha));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(200, 50, 50));
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 50));
                drawCenteredString(g2d, "𓋹 VICTORY! 𓋹", getWidth()/2, getHeight()/2 - 50);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 30));
                drawCenteredString(g2d, victorName, getWidth()/2, getHeight()/2 + 30);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 20));
                drawCenteredString(g2d, "𓃀 𓃥 𓃰 𓃱", getWidth()/2, getHeight()/2 + 80);
            }
        }

        private void drawPyramid(Graphics2D g, int x, int y, int width, int height, Color color) {
            g.setColor(color);
            int[] xPoints = {x, x + width/2, x + width};
            int[] yPoints = {y + height, y, y + height};
            g.fillPolygon(xPoints, yPoints, 3);

            // Pyramid lines
            g.setColor(new Color(150, 120, 80));
            g.drawLine(x + width/2, y, x, y + height);
            g.drawLine(x + width/2, y, x + width, y + height);

            // Stone blocks
            for (int i = 1; i < 5; i++) {
                int blockY = y + height - (i * height/5);
                g.drawLine((int) (x + (width/2) * (i/5.0)), blockY, (int) (x + width - (width/2) * (i/5.0)), blockY);
            }
        }

        private void drawEgyptianBoxer(Graphics2D g, int x, int y, Color color, String name, double health, boolean isLeft, String symbol) {
            // Body with Egyptian robe
            g.setColor(color);
            g.fillOval(x - 35, y - 50, 70, 85);
            g.fillRect(x - 30, y - 35, 60, 85);

            // Head with Nemes headdress
            g.fillOval(x - 30, y - 75, 60, 60);

            // Nemes headdress (pharaoh crown)
            g.setColor(new Color(255, 215, 0));
            int[] crownX = {x - 35, x, x + 35};
            int[] crownY = {y - 85, y - 105, y - 85};
            g.fillPolygon(crownX, crownY, 3);
            g.fillRect(x - 35, y - 85, 70, 15);

            // Cobra (Uraeus) on crown
            g.setColor(new Color(255, 100, 50));
            g.fillOval(x - 5, y - 100, 10, 15);

            // Eyes with kohl liner
            g.setColor(Color.WHITE);
            g.fillOval(x - 20, y - 70, 14, 16);
            g.fillOval(x + 6, y - 70, 14, 16);
            g.setColor(Color.BLACK);
            g.fillOval(x - 17, y - 68, 9, 11);
            g.fillOval(x + 8, y - 68, 9, 11);

            // Eye of Horus decoration
            g.setColor(new Color(0, 100, 200));
            g.drawLine(x - 22, y - 65, x - 28, y - 60);
            g.drawLine(x + 24, y - 65, x + 30, y - 60);

            // Mouth
            g.setColor(Color.BLACK);
            g.drawArc(x - 12, y - 52, 24, 16, 0, -180);

            // Beard (royal beard)
            g.setColor(new Color(100, 70, 40));
            g.fillRect(x - 8, y - 50, 16, 20);

            // Boxing gloves with ankh symbols
            g.setColor(new Color(200, 100, 50));
            g.fillOval(x - 55, y - 25, 38, 38);
            g.fillOval(x + 17, y - 25, 38, 38);

            // Ankh symbol on gloves
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Segoe UI", Font.BOLD, 16));
            g.drawString("☥", x - 45, y - 5);
            g.drawString("☥", x + 27, y - 5);

            // Name with Egyptian symbol
            g.setColor(new Color(255, 215, 0));
            g.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g.drawString(symbol + " " + name + " " + symbol, x - 45, y - 100);
        }

        private void drawEgyptianHealthBar(Graphics2D g, int x, int y, int width, int height, double health, String name) {
            g.setColor(new Color(80, 40, 20));
            g.fillRect(x, y, width, height);
            g.setColor(new Color(180, 80, 50));
            g.fillRect(x + 2, y + 2, width - 4, height - 4);

            int healthWidth = (int)((width - 4) * (health / 100));
            Color healthColor = health > 60 ? new Color(255, 100, 50) : health > 30 ? new Color(255, 200, 50) : new Color(200, 50, 50);
            g.setColor(healthColor);
            g.fillRect(x + 2, y + 2, healthWidth, height - 4);

            g.setColor(new Color(255, 215, 0));
            g.drawRect(x, y, width, height);
            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g.drawString("☥ " + name + " ☥", x + 5, y - 5);
        }

        private void drawEgyptianText(Graphics2D g, String text, int x, int y, Color color) {
            g.setColor(color);
            g.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g.drawString(text, x, y);
        }

        private void drawCenteredString(Graphics2D g, String text, int x, int y) {
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g.drawString(text, x - textWidth/2, y);
        }

        class SandParticle {
            int x, y, vx, vy, life;

            SandParticle(int x, int y) {
                this.x = x;
                this.y = y;
                this.vx = random.nextInt(20) - 10;
                this.vy = random.nextInt(15) - 20;
                this.life = 40;
            }

            void update() {
                x += vx;
                y += vy;
                vy += 1;
                life--;
            }
        }

        class StarParticle {
            int x, y, vx, vy, life;

            StarParticle(int x, int y) {
                this.x = x;
                this.y = y;
                this.vx = random.nextInt(15) - 7;
                this.vy = random.nextInt(15) - 15;
                this.life = 30;
            }

            void update() {
                x += vx;
                y += vy;
                vy += 0.5;
                life--;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PyramidBoxingGame());
    }
}