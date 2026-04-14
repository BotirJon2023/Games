import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.util.ArrayList;

public class TokyoBoxingGame extends JFrame {
    private GamePanel gamePanel;
    private JButton punchButton, blockButton, specialButton;
    private JLabel player1HealthLabel, player2HealthLabel, messageLabel;
    private JLabel roundLabel, timerLabel, comboLabel;
    private Timer gameTimer;
    private Boxer player1, player2;
    private boolean isPlayerTurn;
    private int currentRound;
    private int roundTime;
    private boolean gameActive;
    private boolean vsComputer;
    private Random random;
    private int comboCounter;
    private ArrayList<Particle> particles;

    public TokyoBoxingGame() {
        setTitle("TOKYO BOXING NIGHTS - 東京ボクシング");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1200, 750);
        setLocationRelativeTo(null);

        random = new Random();
        particles = new ArrayList<>();
        setupMenu();
        setupGamePanel();
        setupControlPanel();
        setupInfoPanel();

        // Initialize with placeholder boxers
        player1 = new Boxer("Player 1", new Color(255, 50, 50), "assets/player1.png");
        player2 = new Boxer("Computer", new Color(50, 100, 255), "assets/player2.png");
        gameActive = false;

        SwingUtilities.invokeLater(() -> showNewGameDialog());
        setVisible(true);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(30, 30, 50));

        JMenu gameMenu = new JMenu("⚔️ GAME");
        gameMenu.setForeground(Color.YELLOW);
        JMenuItem newGameItem = new JMenuItem("✦ New Game");
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

        JMenu themeMenu = new JMenu("🎨 THEME");
        themeMenu.setForeground(Color.CYAN);
        JMenuItem tokyoItem = new JMenuItem("🇯🇵 Tokyo Night");
        tokyoItem.addActionListener(e -> gamePanel.setTheme("tokyo"));
        themeMenu.add(tokyoItem);
        menuBar.add(themeMenu);

        setJMenuBar(menuBar);
    }

    private void showNewGameDialog() {
        String[] options = {"🥊 Fight Computer", "👥 Two Players"};
        int choice = JOptionPane.showOptionDialog(this,
                "Choose your battle style:", "TOKYO BOXING",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) startNewGame(true);
        else if (choice == 1) startNewGame(false);
    }

    private void setupGamePanel() {
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(1200, 550));
        gamePanel.setBackground(new Color(10, 10, 30));
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
                        else messageLabel.setText("👤 Player 2's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_K) {
                        if (isPlayerTurn) performBlock();
                        else messageLabel.setText("👤 Player 2's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_L) {
                        if (isPlayerTurn) performSpecial();
                        else messageLabel.setText("👤 Player 2's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_A) {
                        if (!isPlayerTurn) performPunch2();
                        else messageLabel.setText("👤 Player 1's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_S) {
                        if (!isPlayerTurn) performBlock2();
                        else messageLabel.setText("👤 Player 1's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_D) {
                        if (!isPlayerTurn) performSpecial2();
                        else messageLabel.setText("👤 Player 1's turn!");
                    }
                }
            }
        });
    }

    private void setupControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(20, 20, 40));
        controlPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 100, 255), 2));

        punchButton = createAnimeButton("💥 PUNCH (J)", new Color(255, 80, 80));
        blockButton = createAnimeButton("🛡️ BLOCK (K)", new Color(80, 150, 255));
        specialButton = createAnimeButton("⚡ SPECIAL (L)", new Color(255, 215, 0));

        punchButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performPunch(); });
        blockButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performBlock(); });
        specialButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performSpecial(); });

        controlPanel.add(punchButton);
        controlPanel.add(blockButton);
        controlPanel.add(specialButton);

        JLabel controls2p = new JLabel("  Two-Player: P2 uses A( Punch ) | S( Block ) | D( Special )");
        controls2p.setFont(new Font("MS Gothic", Font.PLAIN, 12));
        controls2p.setForeground(new Color(200, 200, 255));
        controlPanel.add(controls2p);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private JButton createAnimeButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("MS Gothic", Font.BOLD, 16));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.YELLOW, 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        return button;
    }

    private void setupInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(2, 4, 15, 5));
        infoPanel.setBackground(new Color(15, 15, 35));
        infoPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 100, 255), 2));

        player1HealthLabel = new JLabel("❤️ PLAYER 1: 100%", SwingConstants.CENTER);
        player1HealthLabel.setFont(new Font("MS Gothic", Font.BOLD, 14));
        player1HealthLabel.setForeground(Color.WHITE);

        player2HealthLabel = new JLabel("❤️ PLAYER 2: 100%", SwingConstants.CENTER);
        player2HealthLabel.setFont(new Font("MS Gothic", Font.BOLD, 14));
        player2HealthLabel.setForeground(Color.WHITE);

        roundLabel = new JLabel("🥊 ROUND 1", SwingConstants.CENTER);
        roundLabel.setFont(new Font("MS Gothic", Font.BOLD, 18));
        roundLabel.setForeground(new Color(255, 215, 0));

        timerLabel = new JLabel("⏱️ 3:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("MS Gothic", Font.BOLD, 16));
        timerLabel.setForeground(Color.CYAN);

        comboLabel = new JLabel("✨ COMBO: 0", SwingConstants.CENTER);
        comboLabel.setFont(new Font("MS Gothic", Font.BOLD, 14));
        comboLabel.setForeground(new Color(255, 100, 255));

        messageLabel = new JLabel("🇯🇵 READY TO FIGHT! 🇯🇵", SwingConstants.CENTER);
        messageLabel.setFont(new Font("MS Gothic", Font.BOLD, 14));
        messageLabel.setForeground(new Color(255, 200, 100));

        infoPanel.add(player1HealthLabel);
        infoPanel.add(player2HealthLabel);
        infoPanel.add(roundLabel);
        infoPanel.add(timerLabel);
        infoPanel.add(comboLabel);
        infoPanel.add(new JLabel());
        infoPanel.add(messageLabel);
        infoPanel.add(new JLabel());

        add(infoPanel, BorderLayout.NORTH);
    }

    private void startNewGame(boolean vsComputer) {
        this.vsComputer = vsComputer;
        player1 = new Boxer("勇者 (Yūsha)", new Color(255, 80, 80), "assets/player1.png");
        player2 = new Boxer(vsComputer ? "AI戦士 (Senshi)" : "挑戦者 (Chōsensha)",
                new Color(80, 150, 255), "assets/player2.png");
        currentRound = 1;
        roundTime = 180;
        gameActive = true;
        isPlayerTurn = true;
        comboCounter = 0;

        updateUI();
        roundLabel.setText("🥊 ROUND " + currentRound);

        if (gameTimer != null && gameTimer.isRunning()) gameTimer.stop();
        gameTimer = new Timer(1000, e -> updateTimer());
        gameTimer.start();

        messageLabel.setText("🇯🇵 FIGHT! Your turn! 🇯🇵");
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
        if (currentRound < 10) {
            currentRound++;
            roundTime = 180;
            roundLabel.setText("🥊 ROUND " + currentRound);
            messageLabel.setText("🌸 ROUND " + currentRound + " - FIGHT! 🌸");
            gamePanel.showRoundFlash();
            gamePanel.resetPositions();
            comboCounter = 0;
            updateCombo();

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
        boolean critical = random.nextInt(100) < 15;
        if (critical) damage *= 2;

        player2.takeDamage(damage);

        if (critical) {
            comboCounter++;
            updateCombo();
            messageLabel.setText("🔥 CRITICAL HIT! 🔥 " + damage + " damage!");
            gamePanel.addParticles(player1.isLeft ? 200 : 1000, 300);
        } else {
            comboCounter = Math.max(0, comboCounter - 1);
            updateCombo();
            messageLabel.setText("💥 PUNCH! " + damage + " damage!");
        }

        updateUI();
        gamePanel.showPunchAnimation(true, critical);

        if (player2.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = false;
            messageLabel.setText(vsComputer ? "🤖 Computer's turn!" : "👤 Player 2's turn!");

            if (vsComputer) {
                computerTurn();
            }
        }

        gamePanel.repaint();
    }

    private void performBlock() {
        if (!gameActive || !isPlayerTurn) return;

        gamePanel.showBlockAnimation(true);
        messageLabel.setText("🛡️ PERFECT BLOCK! 🛡️");
        player1.setBlocking(true);
        comboCounter = 0;
        updateCombo();

        isPlayerTurn = false;
        messageLabel.setText(vsComputer ? "🤖 Computer's turn!" : "👤 Player 2's turn!");

        if (vsComputer) {
            computerTurn();
        }

        gamePanel.repaint();
    }

    private void performSpecial() {
        if (!gameActive || !isPlayerTurn || player2 == null) return;

        int damage = random.nextInt(30) + 25;
        player2.takeDamage(damage);
        updateUI();

        gamePanel.showSpecialEffect(true);
        messageLabel.setText("⚡ HADOUKEN PUNCH! " + damage + " damage! ⚡");
        comboCounter += 2;
        updateCombo();

        if (player2.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = false;
            messageLabel.setText(vsComputer ? "🤖 Computer's turn!" : "👤 Player 2's turn!");

            if (vsComputer) {
                computerTurn();
            }
        }

        gamePanel.repaint();
    }

    private void performPunch2() {
        if (!gameActive || isPlayerTurn || player1 == null) return;

        int damage = random.nextInt(15) + 10;
        boolean critical = random.nextInt(100) < 15;
        if (critical) damage *= 2;

        player1.takeDamage(damage);
        updateUI();

        gamePanel.showPunchAnimation(false, critical);
        messageLabel.setText((critical ? "🔥 CRITICAL! 🔥 " : "💥 ") + "Player 2 lands " + damage + " damage!");

        if (player1.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = true;
            messageLabel.setText("👤 Player 1's turn!");
        }

        gamePanel.repaint();
    }

    private void performBlock2() {
        if (!gameActive || isPlayerTurn) return;

        gamePanel.showBlockAnimation(false);
        messageLabel.setText("🛡️ Player 2 blocks!");
        player2.setBlocking(true);

        isPlayerTurn = true;
        messageLabel.setText("👤 Player 1's turn!");

        gamePanel.repaint();
    }

    private void performSpecial2() {
        if (!gameActive || isPlayerTurn || player1 == null) return;

        int damage = random.nextInt(30) + 25;
        player1.takeDamage(damage);
        updateUI();

        gamePanel.showSpecialEffect(false);
        messageLabel.setText("⚡ Player 2 SPECIAL MOVE! " + damage + " damage! ⚡");

        if (player1.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = true;
            messageLabel.setText("👤 Player 1's turn!");
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
                    int damage = random.nextInt(15) + 10;
                    boolean critical = random.nextInt(100) < 15;
                    if (critical) damage *= 2;

                    if (player1.isBlocking()) {
                        damage /= 2;
                        messageLabel.setText("🤖 Punch partially blocked!");
                        player1.setBlocking(false);
                    }

                    player1.takeDamage(damage);
                    updateUI();
                    gamePanel.showPunchAnimation(false, critical);
                    messageLabel.setText((critical ? "🔥 CRITICAL! 🔥 " : "💥 ") + "Computer hits for " + damage + " damage!");

                    if (player1.getHealth() <= 0) {
                        endGame();
                    }
                } else if (action < 75) {
                    gamePanel.showBlockAnimation(false);
                    messageLabel.setText("🤖 Computer blocks!");
                    player2.setBlocking(true);
                } else {
                    int damage = random.nextInt(30) + 25;
                    player1.takeDamage(damage);
                    updateUI();
                    gamePanel.showSpecialEffect(false);
                    messageLabel.setText("⚡ COMPUTER SPECIAL! " + damage + " damage! ⚡");

                    if (player1.getHealth() <= 0) {
                        endGame();
                    }
                }

                isPlayerTurn = true;
                messageLabel.setText("🇯🇵 Your turn! 🇯🇵");
                gamePanel.repaint();

                ((Timer)e.getSource()).stop();
            }
        });

        computerTimer.setRepeats(false);
        computerTimer.start();
    }

    private void updateUI() {
        if (player1 == null || player2 == null) return;

        player1HealthLabel.setText(String.format("❤️ 勇者: %.0f%%", player1.getHealth()));
        player2HealthLabel.setText(String.format("❤️ %s: %.0f%%", vsComputer ? "AI戦士" : "挑戦者", player2.getHealth()));

        // Health color coding
        player1HealthLabel.setForeground(player1.getHealth() < 30 ? Color.RED :
                player1.getHealth() < 60 ? Color.YELLOW : Color.GREEN);
        player2HealthLabel.setForeground(player2.getHealth() < 30 ? Color.RED :
                player2.getHealth() < 60 ? Color.YELLOW : Color.GREEN);
    }

    private void updateCombo() {
        comboLabel.setText("✨ COMBO: " + comboCounter + " ✨");
        if (comboCounter >= 5) {
            comboLabel.setForeground(new Color(255, 100, 255));
        } else if (comboCounter >= 3) {
            comboLabel.setForeground(Color.YELLOW);
        } else {
            comboLabel.setForeground(Color.CYAN);
        }
    }

    private void endGame() {
        gameActive = false;
        if (gameTimer != null) gameTimer.stop();

        String winner;
        if (player1 == null || player1.getHealth() <= 0) {
            winner = vsComputer ? "🤖 AI戦士 (Computer)" : "👤 挑戦者 (Player 2)";
        } else {
            winner = "👤 勇者 (Player 1)";
        }

        gamePanel.showVictoryEffect(winner);

        int option = JOptionPane.showConfirmDialog(this,
                "🏆 " + winner + " WINS! 🏆\n\nPlay again?",
                "GAME OVER - 試合終了",
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
        private String spritePath;
        public boolean isLeft = true;

        public Boxer(String name, Color color, String spritePath) {
            this.name = name;
            this.color = color;
            this.spritePath = spritePath;
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

    class Particle {
        int x, y;
        int vx, vy;
        int life;
        Color color;

        public Particle(int x, int y, Color color) {
            this.x = x;
            this.y = y;
            this.vx = random.nextInt(10) - 5;
            this.vy = random.nextInt(10) - 15;
            this.life = 30;
            this.color = color;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 1;
            life--;
        }
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
        private int player1X = 250, player2X = 850;
        private int player1Y = 300, player2Y = 300;
        private String theme = "tokyo";
        private ArrayList<Particle> particles = new ArrayList<>();
        private ArrayList<String> floatingText = new ArrayList<>();

        public GamePanel() {
            Timer animationTimer = new Timer(33, e -> {
                if (flashAlpha > 0) flashAlpha -= 10;
                if (roundFlashAlpha > 0) roundFlashAlpha -= 8;
                if (victoryFlashAlpha > 0) victoryFlashAlpha -= 5;

                for (int i = particles.size() - 1; i >= 0; i--) {
                    particles.get(i).update();
                    if (particles.get(i).life <= 0) particles.remove(i);
                }

                repaint();
            });
            animationTimer.start();
        }

        public void setTheme(String theme) {
            this.theme = theme;
            repaint();
        }

        public void addParticles(int x, int y) {
            for (int i = 0; i < 20; i++) {
                particles.add(new Particle(x, y, new Color(255, random.nextInt(200), random.nextInt(100))));
            }
        }

        public void resetPositions() {
            player1X = 250;
            player2X = 850;
        }

        public void showPunchAnimation(boolean left, boolean critical) {
            flashAlpha = 200;
            showPunch = true;
            isLeftPunch = left;
            isCritical = critical;
            addParticles(left ? 350 : 850, 300);

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
            addParticles(left ? 350 : 850, 300);

            for (int i = 0; i < 50; i++) {
                particles.add(new Particle(left ? 350 : 850, 300, new Color(255, 200, 0)));
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
                particles.add(new Particle(getWidth()/2, getHeight()/2,
                        new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255))));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Tokyo night skyline background
            GradientPaint skyGradient = new GradientPaint(0, 0, new Color(15, 15, 45),
                    0, getHeight(), new Color(5, 5, 25));
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Tokyo skyline
            g2d.setColor(new Color(30, 30, 60));
            for (int i = 0; i < 15; i++) {
                int height = 80 + random.nextInt(120);
                g2d.fillRect(50 + i * 80, getHeight() - height - 100, 40, height);
            }

            // Tokyo Tower
            g2d.setColor(new Color(255, 100, 100));
            g2d.fillRect(550, getHeight() - 300, 30, 200);
            g2d.fillPolygon(new int[]{550, 565, 580}, new int[]{getHeight() - 300, getHeight() - 380, getHeight() - 300}, 3);

            // Neon lights
            for (int i = 0; i < 30; i++) {
                g2d.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
                g2d.fillRect(30 + i * 40, 15, 15, 8);
                g2d.fillRect(30 + i * 40, getHeight() - 45, 15, 8);
            }

            // Kanji signs
            g2d.setColor(new Color(255, 100, 100, 150));
            g2d.setFont(new Font("MS Gothic", Font.BOLD, 24));
            g2d.drawString("闘", 100, 80);
            g2d.drawString("技", 200, 80);
            g2d.drawString("魂", 900, 80);
            g2d.drawString("勝", 1050, 80);

            // Ring
            g2d.setColor(new Color(80, 40, 20));
            g2d.fillRect(80, 120, 1040, 380);
            g2d.setColor(new Color(200, 180, 140));
            g2d.fillRect(90, 130, 1020, 360);

            // Ring ropes with neon effect
            for (int i = 0; i < 4; i++) {
                g2d.setStroke(new BasicStroke(4));
                int yPos = 150 + i * 80;
                g2d.setColor(new Color(255, 100, 255));
                g2d.drawLine(80, yPos, 1120, yPos);
                g2d.setColor(new Color(100, 100, 255));
                g2d.drawLine(82, yPos + 2, 1118, yPos + 2);
            }

            // Draw boxers with anime style
            if (player1 != null) drawAnimeBoxer(g2d, player1X, player1Y, player1.getColor(), player1.getName(), player1.getHealth(), true);
            if (player2 != null) drawAnimeBoxer(g2d, player2X, player2Y, player2.getColor(), player2.getName(), player2.getHealth(), false);

            // Health bars with Japanese style
            if (player1 != null) drawJapaneseHealthBar(g2d, player1X - 80, player1Y - 50, 160, 20, player1.getHealth(), "勇者");
            if (player2 != null) drawJapaneseHealthBar(g2d, player2X - 80, player2Y - 50, 160, 20, player2.getHealth(), vsComputer ? "AI戦士" : "挑戦者");

            // Block indicators
            if (player1 != null && player1.isBlocking()) {
                drawJapaneseText(g2d, "防御!", player1X - 20, player1Y - 80, new Color(100, 255, 255));
            }
            if (player2 != null && player2.isBlocking()) {
                drawJapaneseText(g2d, "防御!", player2X - 20, player2Y - 80, new Color(100, 255, 255));
            }

            // Particles
            for (Particle p : particles) {
                g2d.setColor(p.color);
                g2d.fillOval(p.x, p.y, 4, 4);
            }

            // Flash effects
            if (flashAlpha > 0) {
                if (showPunch) {
                    g2d.setColor(new Color(255, isCritical ? 0 : 200, 0, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    g2d.setColor(new Color(255, 255, 0, flashAlpha));
                    g2d.setFont(new Font("MS Gothic", Font.BOLD, 40));
                    String impactText = isCritical ? "会心の一撃!!" : "ドカッ!";
                    drawCenteredString(g2d, impactText, getWidth()/2, getHeight()/2);
                } else if (showBlock) {
                    g2d.setColor(new Color(0, 200, 255, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    drawJapaneseText(g2d, "ガード!", getWidth()/2 - 50, getHeight()/2, Color.WHITE);
                } else if (showSpecial) {
                    g2d.setColor(new Color(255, 100, 0, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    drawJapaneseText(g2d, "奥義!", getWidth()/2 - 50, getHeight()/2, Color.YELLOW);
                }
            }

            if (roundFlashAlpha > 0) {
                g2d.setColor(new Color(255, 200, 100, roundFlashAlpha));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("MS Gothic", Font.BOLD, 70));
                drawCenteredString(g2d, currentRound + " ROUND", getWidth()/2, getHeight()/2);
            }

            if (victoryFlashAlpha > 0) {
                g2d.setColor(new Color(255, 215, 0, victoryFlashAlpha));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("MS Gothic", Font.BOLD, 50));
                drawCenteredString(g2d, "VICTORY!", getWidth()/2, getHeight()/2 - 50);
                g2d.setFont(new Font("MS Gothic", Font.BOLD, 30));
                drawCenteredString(g2d, victorName, getWidth()/2, getHeight()/2 + 30);
            }
        }

        private void drawAnimeBoxer(Graphics2D g, int x, int y, Color color, String name, double health, boolean isLeft) {
            // Anime-style big eyes
            g.setColor(color);
            g.fillOval(x - 35, y - 55, 70, 90);
            g.fillRect(x - 30, y - 40, 60, 90);

            // Head
            g.fillOval(x - 30, y - 80, 60, 60);

            // Big anime eyes
            g.setColor(Color.WHITE);
            g.fillOval(x - 22, y - 70, 18, 22);
            g.fillOval(x + 4, y - 70, 18, 22);
            g.setColor(Color.BLACK);
            g.fillOval(x - 18, y - 68, 12, 14);
            g.fillOval(x + 6, y - 68, 12, 14);
            g.setColor(Color.WHITE);
            g.fillOval(x - 15, y - 72, 5, 5);
            g.fillOval(x + 9, y - 72, 5, 5);

            // Hair (spiky anime hair)
            g.setColor(color.darker());
            int[] hairX = {x - 20, x - 35, x - 25, x, x + 25, x + 35, x + 20};
            int[] hairY = {y - 85, y - 100, y - 95, y - 110, y - 95, y - 100, y - 85};
            g.fillPolygon(hairX, hairY, 7);

            // Mouth
            g.setColor(Color.BLACK);
            g.drawArc(x - 12, y - 55, 24, 18, 0, -180);

            // Boxing gloves
            g.setColor(new Color(200, 50, 50));
            g.fillOval(x - 55, y - 25, 40, 40);
            g.fillOval(x + 15, y - 25, 40, 40);

            // Headband
            g.setColor(Color.RED);
            g.fillRect(x - 32, y - 82, 64, 8);

            // Name with Japanese honorific
            g.setColor(Color.YELLOW);
            g.setFont(new Font("MS Gothic", Font.BOLD, 14));
            String displayName = isLeft ? name + " 選手" : name;
            g.drawString(displayName, x - 35, y - 100);
        }

        private void drawJapaneseHealthBar(Graphics2D g, int x, int y, int width, int height, double health, String name) {
            g.setColor(new Color(50, 30, 30));
            g.fillRect(x, y, width, height);
            g.setColor(new Color(200, 50, 50));
            g.fillRect(x + 2, y + 2, width - 4, height - 4);

            int healthWidth = (int)((width - 4) * (health / 100));
            Color healthColor = health > 60 ? new Color(255, 80, 80) : health > 30 ? new Color(255, 200, 50) : new Color(200, 50, 50);
            g.setColor(healthColor);
            g.fillRect(x + 2, y + 2, healthWidth, height - 4);

            g.setColor(Color.WHITE);
            g.drawRect(x, y, width, height);
            g.setFont(new Font("MS Gothic", Font.BOLD, 12));
            g.drawString(name, x + 5, y - 5);
        }

        private void drawJapaneseText(Graphics2D g, String text, int x, int y, Color color) {
            g.setColor(color);
            g.setFont(new Font("MS Gothic", Font.BOLD, 24));
            g.drawString(text, x, y);
        }

        private void drawCenteredString(Graphics2D g, String text, int x, int y) {
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g.drawString(text, x - textWidth/2, y);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TokyoBoxingGame());
    }
}