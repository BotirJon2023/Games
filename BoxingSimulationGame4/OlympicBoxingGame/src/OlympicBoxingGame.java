import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.util.ArrayList;

public class OlympicBoxingGame extends JFrame {
    private GamePanel gamePanel;
    private JButton punchButton, blockButton, specialButton;
    private JLabel player1HealthLabel, player2HealthLabel, messageLabel;
    private JLabel roundLabel, timerLabel, medalLabel;
    private Timer gameTimer;
    private Boxer player1, player2;
    private boolean isPlayerTurn;
    private int currentRound;
    private int roundTime;
    private boolean gameActive;
    private boolean vsComputer;
    private Random random;
    private int player1Medals, player2Medals;
    private String[] medalTypes = {"🥇 GOLD", "🥈 SILVER", "🥉 BRONZE"};

    public OlympicBoxingGame() {
        setTitle("🏅 OLYMPIC BOXING - Paris 2024 🏅");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1300, 800);
        setLocationRelativeTo(null);

        random = new Random();
        player1Medals = 0;
        player2Medals = 0;
        setupMenu();
        setupGamePanel();
        setupControlPanel();
        setupInfoPanel();

        // Initialize placeholder boxers
        player1 = new Boxer("USA", "🇺🇸", new Color(0, 40, 100));
        player2 = new Boxer("CHINA", "🇨🇳", new Color(200, 0, 0));
        gameActive = false;

        SwingUtilities.invokeLater(() -> showNewGameDialog());
        setVisible(true);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(0, 50, 100));

        JMenu gameMenu = new JMenu("🏅 GAME");
        gameMenu.setForeground(Color.YELLOW);
        JMenuItem newGameItem = new JMenuItem("✨ New Olympic Match");
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

        JMenu countryMenu = new JMenu("🌍 COUNTRIES");
        countryMenu.setForeground(Color.CYAN);
        String[] countries = {"USA 🇺🇸", "CHINA 🇨🇳", "JAPAN 🇯🇵", "UK 🇬🇧", "FRANCE 🇫🇷", "BRAZIL 🇧🇷", "AUSTRALIA 🇦🇺", "RUSSIA 🇷🇺"};
        for (String country : countries) {
            JMenuItem countryItem = new JMenuItem(country);
            countryItem.addActionListener(e -> setPlayerCountry(country));
            countryMenu.add(countryItem);
        }
        menuBar.add(countryMenu);

        setJMenuBar(menuBar);
    }

    private void setPlayerCountry(String country) {
        if (player1 != null) {
            String[] parts = country.split(" ");
            player1.setCountry(parts[0], parts[1]);
            updateUI();
            gamePanel.repaint();
        }
    }

    private void showNewGameDialog() {
        String[] options = {"🥊 Olympic Computer Match", "👥 Two Players Exhibition"};
        int choice = JOptionPane.showOptionDialog(this,
                "Choose Olympic boxing event:", "OLYMPIC BOXING",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) startNewGame(true);
        else if (choice == 1) startNewGame(false);
    }

    private void setupGamePanel() {
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(1300, 580));
        gamePanel.setBackground(new Color(34, 139, 34));
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
                        else messageLabel.setText("🏅 Player 2's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_K) {
                        if (isPlayerTurn) performBlock();
                        else messageLabel.setText("🏅 Player 2's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_L) {
                        if (isPlayerTurn) performSpecial();
                        else messageLabel.setText("🏅 Player 2's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_A) {
                        if (!isPlayerTurn) performPunch2();
                        else messageLabel.setText("🏅 Player 1's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_S) {
                        if (!isPlayerTurn) performBlock2();
                        else messageLabel.setText("🏅 Player 1's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_D) {
                        if (!isPlayerTurn) performSpecial2();
                        else messageLabel.setText("🏅 Player 1's turn!");
                    }
                }
            }
        });
    }

    private void setupControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(0, 50, 100));
        controlPanel.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));

        punchButton = createOlympicButton("👊 JAB (J)", new Color(255, 80, 80));
        blockButton = createOlympicButton("🛡️ BLOCK (K)", new Color(80, 150, 255));
        specialButton = createOlympicButton("⭐ OLYMPIC SPIRIT (L)", new Color(255, 215, 0));

        punchButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performPunch(); });
        blockButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performBlock(); });
        specialButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performSpecial(); });

        controlPanel.add(punchButton);
        controlPanel.add(blockButton);
        controlPanel.add(specialButton);

        JLabel controls2p = new JLabel("  Two-Player: P2 uses A(Punch) | S(Block) | D(Special)");
        controls2p.setFont(new Font("Arial", Font.PLAIN, 12));
        controls2p.setForeground(Color.YELLOW);
        controlPanel.add(controls2p);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private JButton createOlympicButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
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
        infoPanel.setBackground(new Color(0, 40, 80));
        infoPanel.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 2));

        player1HealthLabel = new JLabel("❤️ USA: 100%", SwingConstants.CENTER);
        player1HealthLabel.setFont(new Font("Arial", Font.BOLD, 14));
        player1HealthLabel.setForeground(Color.WHITE);

        player2HealthLabel = new JLabel("❤️ CHINA: 100%", SwingConstants.CENTER);
        player2HealthLabel.setFont(new Font("Arial", Font.BOLD, 14));
        player2HealthLabel.setForeground(Color.WHITE);

        roundLabel = new JLabel("🏅 ROUND 1", SwingConstants.CENTER);
        roundLabel.setFont(new Font("Arial", Font.BOLD, 18));
        roundLabel.setForeground(Color.YELLOW);

        timerLabel = new JLabel("⏱️ 3:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timerLabel.setForeground(Color.CYAN);

        medalLabel = new JLabel("🏅 MEDALS: 0-0", SwingConstants.CENTER);
        medalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        medalLabel.setForeground(new Color(255, 215, 0));

        messageLabel = new JLabel("🇫🇷 OLYMPIC BOXING - PARIS 2024 🇫🇷", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        messageLabel.setForeground(Color.YELLOW);

        infoPanel.add(player1HealthLabel);
        infoPanel.add(player2HealthLabel);
        infoPanel.add(roundLabel);
        infoPanel.add(timerLabel);
        infoPanel.add(medalLabel);
        infoPanel.add(new JLabel());
        infoPanel.add(messageLabel);
        infoPanel.add(new JLabel());

        add(infoPanel, BorderLayout.NORTH);
    }

    private void startNewGame(boolean vsComputer) {
        this.vsComputer = vsComputer;
        player1 = new Boxer("USA", "🇺🇸", new Color(0, 40, 100));
        player2 = new Boxer(vsComputer ? "CHINA" : "FRANCE",
                vsComputer ? "🇨🇳" : "🇫🇷",
                vsComputer ? new Color(200, 0, 0) : new Color(0, 80, 150));
        currentRound = 1;
        roundTime = 180;
        gameActive = true;
        isPlayerTurn = true;

        updateUI();
        roundLabel.setText("🏅 ROUND " + currentRound);

        if (gameTimer != null && gameTimer.isRunning()) gameTimer.stop();
        gameTimer = new Timer(1000, e -> updateTimer());
        gameTimer.start();

        messageLabel.setText("🏅 LET THE OLYMPIC GAMES BEGIN! 🏅");
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
        if (currentRound < 5) {
            currentRound++;
            roundTime = 180;
            roundLabel.setText("🏅 ROUND " + currentRound);
            messageLabel.setText("🏅 ROUND " + currentRound + " - FIGHT FOR GLORY! 🏅");
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
        boolean critical = random.nextInt(100) < 20;
        if (critical) damage *= 2;

        player2.takeDamage(damage);

        updateUI();
        gamePanel.showPunchAnimation(true, critical);

        if (critical) {
            messageLabel.setText("⭐ OLYMPIC RECORD PUNCH! " + damage + " damage! ⭐");
        } else {
            messageLabel.setText("👊 JAB! " + damage + " damage!");
        }

        if (player2.getHealth() <= 0) {
            awardMedal(true);
            endGame();
        } else {
            isPlayerTurn = false;
            messageLabel.setText(vsComputer ? "🇨🇳 China's turn!" : "🏅 Player 2's turn!");

            if (vsComputer) {
                computerTurn();
            }
        }

        gamePanel.repaint();
    }

    private void performBlock() {
        if (!gameActive || !isPlayerTurn) return;

        gamePanel.showBlockAnimation(true);
        messageLabel.setText("🛡️ OLYMPIC DEFENSE! 🛡️");
        player1.setBlocking(true);

        isPlayerTurn = false;
        messageLabel.setText(vsComputer ? "🇨🇳 China's turn!" : "🏅 Player 2's turn!");

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
        messageLabel.setText("⭐ OLYMPIC SPIRIT! " + damage + " damage! ⭐");

        if (player2.getHealth() <= 0) {
            awardMedal(true);
            endGame();
        } else {
            isPlayerTurn = false;
            messageLabel.setText(vsComputer ? "🇨🇳 China's turn!" : "🏅 Player 2's turn!");

            if (vsComputer) {
                computerTurn();
            }
        }

        gamePanel.repaint();
    }

    private void performPunch2() {
        if (!gameActive || isPlayerTurn || player1 == null) return;

        int damage = random.nextInt(12) + 8;
        boolean critical = random.nextInt(100) < 20;
        if (critical) damage *= 2;

        player1.takeDamage(damage);
        updateUI();

        gamePanel.showPunchAnimation(false, critical);
        messageLabel.setText((critical ? "⭐ OLYMPIC RECORD! ⭐ " : "👊 ") + "Player 2 lands " + damage + " damage!");

        if (player1.getHealth() <= 0) {
            awardMedal(false);
            endGame();
        } else {
            isPlayerTurn = true;
            messageLabel.setText("🏅 Player 1's turn!");
        }

        gamePanel.repaint();
    }

    private void performBlock2() {
        if (!gameActive || isPlayerTurn) return;

        gamePanel.showBlockAnimation(false);
        messageLabel.setText("🛡️ Player 2 blocks!");
        player2.setBlocking(true);

        isPlayerTurn = true;
        messageLabel.setText("🏅 Player 1's turn!");

        gamePanel.repaint();
    }

    private void performSpecial2() {
        if (!gameActive || isPlayerTurn || player1 == null) return;

        int damage = random.nextInt(30) + 25;
        player1.takeDamage(damage);
        updateUI();

        gamePanel.showSpecialEffect(false);
        messageLabel.setText("⭐ FRENCH OLYMPIC SPIRIT! " + damage + " damage! ⭐");

        if (player1.getHealth() <= 0) {
            awardMedal(false);
            endGame();
        } else {
            isPlayerTurn = true;
            messageLabel.setText("🏅 Player 1's turn!");
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

                if (action < 60) {
                    int damage = random.nextInt(12) + 8;
                    boolean critical = random.nextInt(100) < 20;
                    if (critical) damage *= 2;

                    if (player1.isBlocking()) {
                        damage /= 2;
                        messageLabel.setText("🇨🇳 China punch partially blocked!");
                        player1.setBlocking(false);
                    }

                    player1.takeDamage(damage);
                    updateUI();
                    gamePanel.showPunchAnimation(false, critical);
                    messageLabel.setText((critical ? "⭐ CRITICAL! ⭐ " : "👊 ") + "China hits for " + damage + " damage!");

                    if (player1.getHealth() <= 0) {
                        awardMedal(false);
                        endGame();
                    }
                } else if (action < 80) {
                    gamePanel.showBlockAnimation(false);
                    messageLabel.setText("🇨🇳 China blocks with Olympic defense!");
                    player2.setBlocking(true);
                } else {
                    int damage = random.nextInt(30) + 25;
                    player1.takeDamage(damage);
                    updateUI();
                    gamePanel.showSpecialEffect(false);
                    messageLabel.setText("⭐ GREAT WALL POWER! " + damage + " damage! ⭐");

                    if (player1.getHealth() <= 0) {
                        awardMedal(false);
                        endGame();
                    }
                }

                isPlayerTurn = true;
                messageLabel.setText("🇺🇸 Your turn, USA! 🇺🇸");
                gamePanel.repaint();

                ((Timer)e.getSource()).stop();
            }
        });

        computerTimer.setRepeats(false);
        computerTimer.start();
    }

    private void awardMedal(boolean player1Won) {
        if (player1Won) {
            player1Medals++;
            String medal = currentRound <= 2 ? medalTypes[2] : (currentRound <= 4 ? medalTypes[1] : medalTypes[0]);
            messageLabel.setText("🏅 " + player1.getCountryName() + " wins " + medal + "! 🏅");
        } else {
            player2Medals++;
            String medal = currentRound <= 2 ? medalTypes[2] : (currentRound <= 4 ? medalTypes[1] : medalTypes[0]);
            messageLabel.setText("🏅 " + player2.getCountryName() + " wins " + medal + "! 🏅");
        }
        medalLabel.setText("🏅 MEDALS: " + player1Medals + "-" + player2Medals);
    }

    private void updateUI() {
        if (player1 == null || player2 == null) return;

        player1HealthLabel.setText(String.format("❤️ %s: %.0f%%", player1.getCountryName(), player1.getHealth()));
        player2HealthLabel.setText(String.format("❤️ %s: %.0f%%", player2.getCountryName(), player2.getHealth()));

        player1HealthLabel.setForeground(player1.getHealth() < 30 ? Color.RED :
                player1.getHealth() < 60 ? Color.YELLOW : Color.GREEN);
        player2HealthLabel.setForeground(player2.getHealth() < 30 ? Color.RED :
                player2.getHealth() < 60 ? Color.YELLOW : Color.GREEN);
    }

    private void endGame() {
        gameActive = false;
        if (gameTimer != null) gameTimer.stop();

        String winner;
        String medalCeremony;
        if (player1 == null || player1.getHealth() <= 0) {
            winner = player2.getCountryName() + " " + player2.getFlag();
            medalCeremony = getMedalCeremony(false);
        } else {
            winner = player1.getCountryName() + " " + player1.getFlag();
            medalCeremony = getMedalCeremony(true);
        }

        gamePanel.showVictoryEffect(winner, medalCeremony);

        int option = JOptionPane.showConfirmDialog(this,
                "🏆 " + winner + " WINS GOLD! 🏆\n\n" + medalCeremony + "\n\nPlay another match?",
                "OLYMPIC CHAMPIONSHIP",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            player1Medals = 0;
            player2Medals = 0;
            showNewGameDialog();
        } else {
            System.exit(0);
        }
    }

    private String getMedalCeremony(boolean player1Won) {
        if (player1Won) {
            return "🇺🇸🇺🇸🇺🇸 STAR-SPANGLED BANNER PLAYS 🇺🇸🇺🇸🇺🇸\n" +
                    "🥇 GOLD: " + player1.getCountryName() + "\n" +
                    "🥈 SILVER: " + player2.getCountryName() + "\n" +
                    "🥉 BRONZE: Other Nations";
        } else {
            return "🇨🇳🇨🇳🇨🇳 CHINESE NATIONAL ANTHEM PLAYS 🇨🇳🇨🇳🇨🇳\n" +
                    "🥇 GOLD: " + player2.getCountryName() + "\n" +
                    "🥈 SILVER: " + player1.getCountryName() + "\n" +
                    "🥉 BRONZE: Other Nations";
        }
    }

    class Boxer {
        private String country;
        private String flag;
        private double health;
        private boolean isBlocking;
        private Color color;

        public Boxer(String country, String flag, Color color) {
            this.country = country;
            this.flag = flag;
            this.color = color;
            this.health = 100;
            this.isBlocking = false;
        }

        public void setCountry(String country, String flag) {
            this.country = country;
            this.flag = flag;
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
        public String getCountryName() { return country; }
        public String getFlag() { return flag; }
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
        private String medalText = "";
        private int player1X = 300, player2X = 900;
        private int player1Y = 320, player2Y = 320;
        private ArrayList<FireworkParticle> fireworks = new ArrayList<>();
        private ArrayList<ConfettiParticle> confetti = new ArrayList<>();

        public GamePanel() {
            Timer animationTimer = new Timer(33, e -> {
                if (flashAlpha > 0) flashAlpha -= 10;
                if (roundFlashAlpha > 0) roundFlashAlpha -= 8;
                if (victoryFlashAlpha > 0) victoryFlashAlpha -= 5;

                for (int i = fireworks.size() - 1; i >= 0; i--) {
                    fireworks.get(i).update();
                    if (fireworks.get(i).life <= 0) fireworks.remove(i);
                }

                for (int i = confetti.size() - 1; i >= 0; i--) {
                    confetti.get(i).update();
                    if (confetti.get(i).life <= 0) confetti.remove(i);
                }

                repaint();
            });
            animationTimer.start();
        }

        public void resetPositions() {
            player1X = 300;
            player2X = 900;
        }

        public void showPunchAnimation(boolean left, boolean critical) {
            flashAlpha = 200;
            showPunch = true;
            isLeftPunch = left;
            isCritical = critical;

            // Add confetti on hit
            for (int i = 0; i < 20; i++) {
                confetti.add(new ConfettiParticle(left ? 450 : 850, 350));
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

            // Add Olympic rings effect
            for (int i = 0; i < 50; i++) {
                confetti.add(new ConfettiParticle(left ? 450 : 850, 350));
                fireworks.add(new FireworkParticle(left ? 450 : 850, 350));
            }

            Timer clearFlash = new Timer(400, e -> { showSpecial = false; ((Timer)e.getSource()).stop(); });
            clearFlash.setRepeats(false);
            clearFlash.start();
        }

        public void showRoundFlash() {
            roundFlashAlpha = 200;
        }

        public void showVictoryEffect(String winner, String medalText) {
            victorName = winner;
            this.medalText = medalText;
            victoryFlashAlpha = 255;

            // Celebration fireworks
            for (int i = 0; i < 150; i++) {
                fireworks.add(new FireworkParticle(getWidth()/2 + random.nextInt(400) - 200,
                        getHeight()/2 + random.nextInt(200) - 100));
                confetti.add(new ConfettiParticle(getWidth()/2 + random.nextInt(400) - 200,
                        getHeight()/2 + random.nextInt(200) - 100));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Olympic stadium background
            GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235),
                    0, getHeight()/2, new Color(100, 150, 200));
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight()/2);

            g2d.setColor(new Color(34, 139, 34));
            g2d.fillRect(0, getHeight()/2, getWidth(), getHeight()/2);

            // Olympic Stadium
            g2d.setColor(new Color(200, 200, 200));
            for (int i = 0; i < 20; i++) {
                g2d.fillRect(50 + i * 60, getHeight() - 180, 40, 150);
            }
            g2d.setColor(new Color(150, 150, 150));
            g2d.fillRect(50, getHeight() - 190, 1200, 15);

            // Olympic Rings
            int ringY = 60;
            int ringSize = 40;
            g2d.setStroke(new BasicStroke(4));
            g2d.setColor(new Color(0, 100, 200));
            g2d.drawOval(getWidth()/2 - 100, ringY, ringSize, ringSize);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(getWidth()/2 - 50, ringY, ringSize, ringSize);
            g2d.setColor(Color.RED);
            g2d.drawOval(getWidth()/2, ringY, ringSize, ringSize);
            g2d.setColor(Color.YELLOW);
            g2d.drawOval(getWidth()/2 - 75, ringY + 20, ringSize, ringSize);
            g2d.setColor(new Color(0, 150, 50));
            g2d.drawOval(getWidth()/2 - 25, ringY + 20, ringSize, ringSize);

            // Paris 2024 logo
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.drawString("PARIS 2024", getWidth()/2 - 80, 30);

            // Crowd silhouettes
            g2d.setColor(new Color(100, 50, 50, 100));
            for (int i = 0; i < 40; i++) {
                g2d.fillOval(60 + i * 30, getHeight() - 160, 15, 25);
            }

            // Boxing ring
            g2d.setColor(new Color(101, 67, 33));
            g2d.fillRect(120, 110, 1060, 380);
            g2d.setColor(new Color(210, 180, 140));
            g2d.fillRect(130, 120, 1040, 360);

            // Ring ropes with Olympic colors
            Color[] olympicColors = {Color.BLUE, Color.YELLOW, Color.BLACK, Color.GREEN, Color.RED};
            for (int i = 0; i < 5; i++) {
                g2d.setStroke(new BasicStroke(4));
                int yPos = 140 + i * 70;
                g2d.setColor(olympicColors[i % olympicColors.length]);
                g2d.drawLine(120, yPos, 1180, yPos);
            }

            // Draw boxers
            if (player1 != null) drawOlympicBoxer(g2d, player1X, player1Y, player1.getColor(), player1.getCountryName(), player1.getFlag(), player1.getHealth(), true);
            if (player2 != null) drawOlympicBoxer(g2d, player2X, player2Y, player2.getColor(), player2.getCountryName(), player2.getFlag(), player2.getHealth(), false);

            // Olympic health bars
            if (player1 != null) drawOlympicHealthBar(g2d, player1X - 100, player1Y - 60, 180, 25, player1.getHealth(), player1.getCountryName(), player1.getFlag());
            if (player2 != null) drawOlympicHealthBar(g2d, player2X - 80, player2Y - 60, 180, 25, player2.getHealth(), player2.getCountryName(), player2.getFlag());

            // Block indicators
            if (player1 != null && player1.isBlocking()) {
                drawOlympicText(g2d, "🏅 OLYMPIC DEFENSE 🏅", player1X - 80, player1Y - 90, new Color(100, 255, 255));
            }
            if (player2 != null && player2.isBlocking()) {
                drawOlympicText(g2d, "🏅 OLYMPIC DEFENSE 🏅", player2X - 80, player2Y - 90, new Color(100, 255, 255));
            }

            // Fireworks and confetti
            for (FireworkParticle f : fireworks) {
                g2d.setColor(f.color);
                g2d.fillOval(f.x, f.y, 5, 5);
                if (f.trail != null) {
                    g2d.setColor(f.trail);
                    g2d.fillOval(f.x - 3, f.y - 3, 3, 3);
                }
            }

            for (ConfettiParticle c : confetti) {
                g2d.setColor(c.color);
                g2d.fillRect(c.x, c.y, 4, 8);
            }

            // Flash effects
            if (flashAlpha > 0) {
                if (showPunch) {
                    g2d.setColor(new Color(255, isCritical ? 100 : 200, 0, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    g2d.setColor(new Color(255, 255, 0, flashAlpha));
                    g2d.setFont(new Font("Arial", Font.BOLD, 40));
                    String impactText = isCritical ? "🏅 OLYMPIC RECORD! 🏅" : "👊 POUND! 👊";
                    drawCenteredString(g2d, impactText, getWidth()/2, getHeight()/2);
                } else if (showBlock) {
                    g2d.setColor(new Color(0, 200, 255, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    drawOlympicText(g2d, "🏅 DEFENDED! 🏅", getWidth()/2 - 80, getHeight()/2, Color.WHITE);
                } else if (showSpecial) {
                    g2d.setColor(new Color(255, 215, 0, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    drawOlympicText(g2d, "⭐ OLYMPIC SPIRIT! ⭐", getWidth()/2 - 100, getHeight()/2, Color.RED);
                }
            }

            if (roundFlashAlpha > 0) {
                g2d.setColor(new Color(255, 215, 0, roundFlashAlpha));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(0, 50, 100));
                g2d.setFont(new Font("Arial", Font.BOLD, 70));
                drawCenteredString(g2d, "ROUND " + currentRound, getWidth()/2, getHeight()/2);
                g2d.setFont(new Font("Arial", Font.BOLD, 30));
                drawCenteredString(g2d, "🏅 FIGHT! 🏅", getWidth()/2, getHeight()/2 + 60);
            }

            if (victoryFlashAlpha > 0) {
                g2d.setColor(new Color(255, 215, 0, victoryFlashAlpha));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(0, 100, 200));
                g2d.setFont(new Font("Arial", Font.BOLD, 50));
                drawCenteredString(g2d, "🏅 OLYMPIC CHAMPION! 🏅", getWidth()/2, getHeight()/2 - 80);
                g2d.setColor(new Color(255, 100, 0));
                g2d.setFont(new Font("Arial", Font.BOLD, 35));
                drawCenteredString(g2d, victorName, getWidth()/2, getHeight()/2);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                String[] lines = medalText.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    drawCenteredString(g2d, lines[i], getWidth()/2, getHeight()/2 + 50 + (i * 25));
                }
            }
        }

        private void drawOlympicBoxer(Graphics2D g, int x, int y, Color color, String country, String flag, double health, boolean isLeft) {
            // Body with Olympic singlet
            g.setColor(color);
            g.fillOval(x - 35, y - 50, 70, 85);
            g.fillRect(x - 30, y - 35, 60, 85);

            // Head
            g.fillOval(x - 30, y - 75, 60, 60);

            // Country flag on chest
            g.setColor(Color.WHITE);
            g.fillRect(x - 15, y - 20, 30, 20);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString(flag, x - 10, y - 5);

            // Eyes
            g.setColor(Color.WHITE);
            g.fillOval(x - 22, y - 70, 14, 16);
            g.fillOval(x + 8, y - 70, 14, 16);
            g.setColor(Color.BLACK);
            g.fillOval(x - 19, y - 68, 9, 11);
            g.fillOval(x + 10, y - 68, 9, 11);

            // Mouth
            g.setColor(Color.BLACK);
            g.drawArc(x - 12, y - 52, 24, 16, 0, -180);

            // Headband with Olympic rings
            g.setColor(new Color(255, 215, 0));
            g.fillRect(x - 32, y - 82, 64, 10);
            g.setColor(Color.RED);
            g.fillOval(x - 10, y - 83, 6, 6);
            g.fillOval(x, y - 83, 6, 6);

            // Boxing gloves
            g.setColor(new Color(200, 50, 50));
            g.fillOval(x - 55, y - 25, 38, 38);
            g.fillOval(x + 17, y - 25, 38, 38);

            // Olympic rings on gloves
            g.setColor(Color.YELLOW);
            g.drawOval(x - 48, y - 18, 10, 10);
            g.drawOval(x + 24, y - 18, 10, 10);

            // Country name
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString(country, x - 25, y - 100);
        }

        private void drawOlympicHealthBar(Graphics2D g, int x, int y, int width, int height, double health, String country, String flag) {
            g.setColor(new Color(50, 50, 50));
            g.fillRect(x, y, width, height);

            int healthWidth = (int)(width * (health / 100));
            Color healthColor = health > 60 ? new Color(0, 150, 0) : health > 30 ? new Color(255, 200, 0) : new Color(200, 0, 0);
            g.setColor(healthColor);
            g.fillRect(x, y, healthWidth, height);

            g.setColor(Color.YELLOW);
            g.drawRect(x, y, width, height);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString(country + " " + flag, x + 5, y - 5);
        }

        private void drawOlympicText(Graphics2D g, String text, int x, int y, Color color) {
            g.setColor(color);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString(text, x, y);
        }

        private void drawCenteredString(Graphics2D g, String text, int x, int y) {
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g.drawString(text, x - textWidth/2, y);
        }

        class FireworkParticle {
            int x, y, vx, vy, life;
            Color color, trail;

            FireworkParticle(int x, int y) {
                this.x = x;
                this.y = y;
                this.vx = random.nextInt(15) - 7;
                this.vy = random.nextInt(15) - 20;
                this.life = 40;
                this.color = new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255));
                this.trail = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
            }

            void update() {
                x += vx;
                y += vy;
                vy += 0.5;
                life--;
            }
        }

        class ConfettiParticle {
            int x, y, vx, vy, life;
            Color color;

            ConfettiParticle(int x, int y) {
                this.x = x;
                this.y = y;
                this.vx = random.nextInt(12) - 6;
                this.vy = random.nextInt(12) - 15;
                this.life = 60;
                this.color = new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255));
            }

            void update() {
                x += vx;
                y += vy;
                vy += 0.3;
                life--;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OlympicBoxingGame());
    }
}