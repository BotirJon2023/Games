import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.util.ArrayList;

public class GreatWallBoxingGame extends JFrame {
    private GamePanel gamePanel;
    private JButton punchButton, blockButton, specialButton;
    private JLabel player1HealthLabel, player2HealthLabel, messageLabel;
    private JLabel roundLabel, timerLabel, chiLabel;
    private Timer gameTimer;
    private Boxer player1, player2;
    private boolean isPlayerTurn;
    private int currentRound;
    private int roundTime;
    private boolean gameActive;
    private boolean vsComputer;
    private Random random;
    private int player1Chi, player2Chi;
    private String[] chinesePhrases = {"功夫", "力量", "速度", "精神", "荣耀"};

    public GreatWallBoxingGame() {
        setTitle("🏯 GREAT WALL BOXING - 长城拳击 🐉");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1300, 800);
        setLocationRelativeTo(null);

        random = new Random();
        player1Chi = 0;
        player2Chi = 0;
        setupMenu();
        setupGamePanel();
        setupControlPanel();
        setupInfoPanel();

        // Initialize placeholder boxers
        player1 = new Boxer("少林弟子", "🇨🇳", new Color(200, 50, 50));
        player2 = new Boxer("武当宗师", "🇨🇳", new Color(50, 100, 200));
        gameActive = false;

        SwingUtilities.invokeLater(() -> showNewGameDialog());
        setVisible(true);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(139, 69, 19));

        JMenu gameMenu = new JMenu("🐉 游戏 GAME");
        gameMenu.setForeground(Color.YELLOW);
        JMenuItem newGameItem = new JMenuItem("✨ 新比赛 New Match");
        JMenuItem vsComputerItem = new JMenuItem("🤖 vs 电脑 Computer");
        JMenuItem vsPlayerItem = new JMenuItem("👥 vs 玩家 Player");
        JMenuItem exitItem = new JMenuItem("✖ 退出 Exit");

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

        JMenu styleMenu = new JMenu("🥋 武术 STYLE");
        styleMenu.setForeground(Color.CYAN);
        String[] styles = {"少林拳 Shaolin", "太极拳 Tai Chi", "咏春拳 Wing Chun", "八卦掌 Bagua", "形意拳 Xing Yi"};
        for (String style : styles) {
            JMenuItem styleItem = new JMenuItem(style);
            styleItem.addActionListener(e -> setFightingStyle(style));
            styleMenu.add(styleItem);
        }
        menuBar.add(styleMenu);

        setJMenuBar(menuBar);
    }

    private void setFightingStyle(String style) {
        if (player1 != null) {
            player1.setStyle(style);
            messageLabel.setText("🥋 " + player1.getName() + " 使用 " + style + " 🥋");
            gamePanel.repaint();
        }
    }

    private void showNewGameDialog() {
        String[] options = {"🐉 战电脑 vs Computer", "👥 双人对战 Two Players"};
        int choice = JOptionPane.showOptionDialog(this,
                "选择你的战斗方式:", "长城拳击",
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
                        else messageLabel.setText("🐉 红方回合 Red's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_K) {
                        if (isPlayerTurn) performBlock();
                        else messageLabel.setText("🐉 红方回合 Red's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_L) {
                        if (isPlayerTurn) performSpecial();
                        else messageLabel.setText("🐉 红方回合 Red's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_A) {
                        if (!isPlayerTurn) performPunch2();
                        else messageLabel.setText("🐉 蓝方回合 Blue's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_S) {
                        if (!isPlayerTurn) performBlock2();
                        else messageLabel.setText("🐉 蓝方回合 Blue's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_D) {
                        if (!isPlayerTurn) performSpecial2();
                        else messageLabel.setText("🐉 蓝方回合 Blue's turn!");
                    }
                }
            }
        });
    }

    private void setupControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(139, 69, 19));
        controlPanel.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));

        punchButton = createChineseButton("👊 拳 PUNCH (J)", new Color(255, 80, 80));
        blockButton = createChineseButton("🛡️ 挡 BLOCK (K)", new Color(80, 150, 255));
        specialButton = createChineseButton("🐉 龙 DRAGON FIST (L)", new Color(255, 215, 0));

        punchButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performPunch(); });
        blockButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performBlock(); });
        specialButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performSpecial(); });

        controlPanel.add(punchButton);
        controlPanel.add(blockButton);
        controlPanel.add(specialButton);

        JLabel controls2p = new JLabel("  双人对战: 红方 J/K/L | 蓝方 A/S/D");
        controls2p.setFont(new Font("SimSun", Font.PLAIN, 12));
        controls2p.setForeground(Color.YELLOW);
        controlPanel.add(controls2p);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private JButton createChineseButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("SimSun", Font.BOLD, 16));
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
        infoPanel.setBackground(new Color(100, 50, 30));
        infoPanel.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 2));

        player1HealthLabel = new JLabel("❤️ 少林弟子: 100%", SwingConstants.CENTER);
        player1HealthLabel.setFont(new Font("SimSun", Font.BOLD, 14));
        player1HealthLabel.setForeground(Color.WHITE);

        player2HealthLabel = new JLabel("❤️ 武当宗师: 100%", SwingConstants.CENTER);
        player2HealthLabel.setFont(new Font("SimSun", Font.BOLD, 14));
        player2HealthLabel.setForeground(Color.WHITE);

        roundLabel = new JLabel("🏯 第1回合 ROUND 1", SwingConstants.CENTER);
        roundLabel.setFont(new Font("SimSun", Font.BOLD, 16));
        roundLabel.setForeground(Color.YELLOW);

        timerLabel = new JLabel("⏱️ 3:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("SimSun", Font.BOLD, 16));
        timerLabel.setForeground(Color.CYAN);

        chiLabel = new JLabel("🐉 真气 CHI: 0-0", SwingConstants.CENTER);
        chiLabel.setFont(new Font("SimSun", Font.BOLD, 14));
        chiLabel.setForeground(new Color(255, 100, 255));

        messageLabel = new JLabel("🏯 长城之巅 - 武林争霸 🐉", SwingConstants.CENTER);
        messageLabel.setFont(new Font("SimSun", Font.BOLD, 14));
        messageLabel.setForeground(Color.YELLOW);

        infoPanel.add(player1HealthLabel);
        infoPanel.add(player2HealthLabel);
        infoPanel.add(roundLabel);
        infoPanel.add(timerLabel);
        infoPanel.add(chiLabel);
        infoPanel.add(new JLabel());
        infoPanel.add(messageLabel);
        infoPanel.add(new JLabel());

        add(infoPanel, BorderLayout.NORTH);
    }

    private void startNewGame(boolean vsComputer) {
        this.vsComputer = vsComputer;
        player1 = new Boxer("少林弟子", "🇨🇳", new Color(200, 50, 50));
        player1.setStyle("少林拳");
        player2 = new Boxer(vsComputer ? "武当宗师" : "峨眉女侠",
                "🇨🇳",
                vsComputer ? new Color(50, 100, 200) : new Color(200, 100, 200));
        if (!vsComputer) player2.setStyle("峨眉刺");
        else player2.setStyle("太极拳");

        currentRound = 1;
        roundTime = 180;
        gameActive = true;
        isPlayerTurn = true;
        player1Chi = 0;
        player2Chi = 0;

        updateUI();
        roundLabel.setText("🏯 第" + currentRound + "回合 ROUND " + currentRound);

        if (gameTimer != null && gameTimer.isRunning()) gameTimer.stop();
        gameTimer = new Timer(1000, e -> updateTimer());
        gameTimer.start();

        messageLabel.setText("🐉 战斗开始！" + player1.getName() + " 先攻！🐉");
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
        if (currentRound < 7) {
            currentRound++;
            roundTime = 180;
            roundLabel.setText("🏯 第" + currentRound + "回合 ROUND " + currentRound);
            messageLabel.setText("🏯 第" + currentRound + "回合 - 继续战斗！🏯");
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
        player1Chi = Math.min(100, player1Chi + 5);
        updateChiBar();

        updateUI();
        gamePanel.showPunchAnimation(true, critical);

        String phrase = chinesePhrases[random.nextInt(chinesePhrases.length)];
        if (critical) {
            messageLabel.setText("⭐ 会心一击！" + phrase + "！ " + damage + " 伤害！ ⭐");
        } else {
            messageLabel.setText("👊 " + phrase + "拳！ " + damage + " 伤害！");
        }

        if (player2.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = false;
            messageLabel.setText(vsComputer ? "🐉 " + player2.getName() + " 的回合！" : "🐉 蓝方回合 Blue's turn!");

            if (vsComputer) {
                computerTurn();
            }
        }

        gamePanel.repaint();
    }

    private void performBlock() {
        if (!gameActive || !isPlayerTurn) return;

        gamePanel.showBlockAnimation(true);
        messageLabel.setText("🛡️ 太极防御！ TAI CHI DEFENSE！ 🛡️");
        player1.setBlocking(true);
        player1Chi = Math.max(0, player1Chi - 10);
        updateChiBar();

        isPlayerTurn = false;
        messageLabel.setText(vsComputer ? "🐉 " + player2.getName() + " 的回合！" : "🐉 蓝方回合 Blue's turn!");

        if (vsComputer) {
            computerTurn();
        }

        gamePanel.repaint();
    }

    private void performSpecial() {
        if (!gameActive || !isPlayerTurn || player2 == null) return;

        if (player1Chi < 50) {
            messageLabel.setText("⚠️ 真气不足！需要50%真气才能使用龙拳！ ⚠️");
            return;
        }

        int damage = random.nextInt(35) + 30;
        player2.takeDamage(damage);
        player1Chi = Math.max(0, player1Chi - 50);
        updateChiBar();
        updateUI();

        gamePanel.showSpecialEffect(true);
        messageLabel.setText("🐉 降龙十八掌！ " + damage + " 伤害！ DRAGON FIST！ 🐉");

        if (player2.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = false;
            messageLabel.setText(vsComputer ? "🐉 " + player2.getName() + " 的回合！" : "🐉 蓝方回合 Blue's turn!");

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
        player2Chi = Math.min(100, player2Chi + 5);
        updateChiBar();
        updateUI();

        gamePanel.showPunchAnimation(false, critical);
        String phrase = chinesePhrases[random.nextInt(chinesePhrases.length)];
        messageLabel.setText((critical ? "⭐ 会心一击！" + phrase + "！ " : "👊 " + phrase + "拳！ ") + damage + " 伤害！");

        if (player1.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = true;
            messageLabel.setText("🐉 红方回合 Red's turn!");
        }

        gamePanel.repaint();
    }

    private void performBlock2() {
        if (!gameActive || isPlayerTurn) return;

        gamePanel.showBlockAnimation(false);
        messageLabel.setText("🛡️ 武当防御！ WUDANG DEFENSE！ 🛡️");
        player2.setBlocking(true);
        player2Chi = Math.max(0, player2Chi - 10);
        updateChiBar();

        isPlayerTurn = true;
        messageLabel.setText("🐉 红方回合 Red's turn!");

        gamePanel.repaint();
    }

    private void performSpecial2() {
        if (!gameActive || isPlayerTurn || player1 == null) return;

        if (player2Chi < 50) {
            messageLabel.setText("⚠️ 真气不足！需要50%真气才能使用绝技！ ⚠️");
            return;
        }

        int damage = random.nextInt(35) + 30;
        player1.takeDamage(damage);
        player2Chi = Math.max(0, player2Chi - 50);
        updateChiBar();
        updateUI();

        gamePanel.showSpecialEffect(false);
        messageLabel.setText("🐉 太极拳奥义！ " + damage + " 伤害！ TAI CHI MASTERY！ 🐉");

        if (player1.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = true;
            messageLabel.setText("🐉 红方回合 Red's turn!");
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

                // Computer uses special if chi >= 50
                if (player2Chi >= 50 && action < 70) {
                    int damage = random.nextInt(35) + 30;
                    player1.takeDamage(damage);
                    player2Chi = Math.max(0, player2Chi - 50);
                    updateChiBar();
                    updateUI();
                    gamePanel.showSpecialEffect(false);
                    messageLabel.setText("🐉 太极拳奥义！ " + damage + " 伤害！ TAI CHI MASTERY！ 🐉");

                    if (player1.getHealth() <= 0) {
                        endGame();
                    }
                } else if (action < 55) {
                    int damage = random.nextInt(12) + 8;
                    boolean critical = random.nextInt(100) < 15;
                    if (critical) damage *= 2;

                    if (player1.isBlocking()) {
                        damage /= 2;
                        messageLabel.setText("🐉 " + player2.getName() + " 的拳被部分格挡！");
                        player1.setBlocking(false);
                    }

                    player1.takeDamage(damage);
                    player2Chi = Math.min(100, player2Chi + 5);
                    updateChiBar();
                    updateUI();
                    gamePanel.showPunchAnimation(false, critical);
                    String phrase = chinesePhrases[random.nextInt(chinesePhrases.length)];
                    messageLabel.setText((critical ? "⭐ 会心一击！" + phrase + "！ " : "👊 " + phrase + "拳！ ") + damage + " 伤害！");

                    if (player1.getHealth() <= 0) {
                        endGame();
                    }
                } else if (action < 75) {
                    gamePanel.showBlockAnimation(false);
                    messageLabel.setText("🛡️ 武当防御！ WUDANG DEFENSE！ 🛡️");
                    player2.setBlocking(true);
                    player2Chi = Math.max(0, player2Chi - 10);
                    updateChiBar();
                } else {
                    // Computer charges chi
                    player2Chi = Math.min(100, player2Chi + 15);
                    updateChiBar();
                    messageLabel.setText("🐉 " + player2.getName() + " 凝聚真气！ CHI CHARGING！ 🐉");
                }

                isPlayerTurn = true;
                messageLabel.setText("🐉 你的回合！" + player1.getName() + "！ 🐉");
                gamePanel.repaint();

                ((Timer)e.getSource()).stop();
            }
        });

        computerTimer.setRepeats(false);
        computerTimer.start();
    }

    private void updateUI() {
        if (player1 == null || player2 == null) return;

        player1HealthLabel.setText(String.format("❤️ %s: %.0f%%", player1.getName(), player1.getHealth()));
        player2HealthLabel.setText(String.format("❤️ %s: %.0f%%", player2.getName(), player2.getHealth()));

        player1HealthLabel.setForeground(player1.getHealth() < 30 ? Color.RED :
                player1.getHealth() < 60 ? Color.YELLOW : Color.GREEN);
        player2HealthLabel.setForeground(player2.getHealth() < 30 ? Color.RED :
                player2.getHealth() < 60 ? Color.YELLOW : Color.GREEN);
    }

    private void updateChiBar() {
        chiLabel.setText(String.format("🐉 真气 CHI: %d-%d", player1Chi, player2Chi));
        if (player1Chi >= 50) {
            chiLabel.setForeground(Color.RED);
            specialButton.setBackground(new Color(255, 100, 0));
            specialButton.setText("🐉 龙拳 READY! (L)");
        } else {
            chiLabel.setForeground(new Color(255, 100, 255));
            specialButton.setBackground(new Color(255, 215, 0));
            specialButton.setText("🐉 龙拳 DRAGON FIST (L)");
        }

        if (player2Chi >= 50 && vsComputer) {
            messageLabel.setText("⚠️ 警告！对手真气充足！ Warning！Opponent CHI full！ ⚠️");
        }
    }

    private void endGame() {
        gameActive = false;
        if (gameTimer != null) gameTimer.stop();

        String winner;
        String chineseWinner;
        if (player1 == null || player1.getHealth() <= 0) {
            winner = player2.getName() + " " + player2.getFlag();
            chineseWinner = player2.getName();
        } else {
            winner = player1.getName() + " " + player1.getFlag();
            chineseWinner = player1.getName();
        }

        gamePanel.showVictoryEffect(winner, chineseWinner);

        int option = JOptionPane.showConfirmDialog(this,
                "🏆 " + winner + " 获胜！ WINS！ 🏆\n\n" +
                        "🏯 长城之巅的新英雄诞生！ 🏯\n\n" +
                        "再战一场？ Play again？",
                "🏆 武林盟主 MARTIAL ARTS CHAMPION 🏆",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            player1Chi = 0;
            player2Chi = 0;
            showNewGameDialog();
        } else {
            System.exit(0);
        }
    }

    class Boxer {
        private String name;
        private String flag;
        private double health;
        private boolean isBlocking;
        private Color color;
        private String style;

        public Boxer(String name, String flag, Color color) {
            this.name = name;
            this.flag = flag;
            this.color = color;
            this.health = 100;
            this.isBlocking = false;
            this.style = "少林拳";
        }

        public void setStyle(String style) {
            this.style = style;
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
        public String getFlag() { return flag; }
        public String getStyle() { return style; }
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
        private String chineseVictor = "";
        private int player1X = 300, player2X = 900;
        private int player1Y = 320, player2Y = 320;
        private ArrayList<DragonParticle> dragons = new ArrayList<>();
        private ArrayList<FireworkParticle> fireworks = new ArrayList<>();
        private ArrayList<CloudParticle> clouds = new ArrayList<>();

        public GamePanel() {
            Timer animationTimer = new Timer(33, e -> {
                if (flashAlpha > 0) flashAlpha -= 10;
                if (roundFlashAlpha > 0) roundFlashAlpha -= 8;
                if (victoryFlashAlpha > 0) victoryFlashAlpha -= 5;

                for (int i = dragons.size() - 1; i >= 0; i--) {
                    dragons.get(i).update();
                    if (dragons.get(i).life <= 0) dragons.remove(i);
                }

                for (int i = fireworks.size() - 1; i >= 0; i--) {
                    fireworks.get(i).update();
                    if (fireworks.get(i).life <= 0) fireworks.remove(i);
                }

                for (int i = clouds.size() - 1; i >= 0; i--) {
                    clouds.get(i).update();
                    if (clouds.get(i).life <= 0) clouds.remove(i);
                }

                repaint();
            });
            animationTimer.start();

            // Add floating clouds
            for (int i = 0; i < 5; i++) {
                clouds.add(new CloudParticle(random.nextInt(getWidth()), random.nextInt(200)));
            }
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

            // Add energy particles
            for (int i = 0; i < 15; i++) {
                fireworks.add(new FireworkParticle(left ? 450 : 850, 350));
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

            // Add dragon effect
            for (int i = 0; i < 5; i++) {
                dragons.add(new DragonParticle(left ? 450 : 850, 350));
            }
            for (int i = 0; i < 50; i++) {
                fireworks.add(new FireworkParticle(left ? 450 : 850, 350));
            }

            Timer clearFlash = new Timer(400, e -> { showSpecial = false; ((Timer)e.getSource()).stop(); });
            clearFlash.setRepeats(false);
            clearFlash.start();
        }

        public void showRoundFlash() {
            roundFlashAlpha = 200;
        }

        public void showVictoryEffect(String winner, String chineseWinner) {
            victorName = winner;
            this.chineseVictor = chineseWinner;
            victoryFlashAlpha = 255;

            // Celebration dragons and fireworks
            for (int i = 0; i < 20; i++) {
                dragons.add(new DragonParticle(getWidth()/2 + random.nextInt(400) - 200,
                        getHeight()/2 + random.nextInt(200) - 100));
            }
            for (int i = 0; i < 200; i++) {
                fireworks.add(new FireworkParticle(getWidth()/2 + random.nextInt(600) - 300,
                        getHeight()/2 + random.nextInt(300) - 150));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Sky gradient - Chinese sunrise
            GradientPaint skyGradient = new GradientPaint(0, 0, new Color(255, 100, 50),
                    0, getHeight()/2, new Color(255, 200, 100));
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight()/2);

            g2d.setColor(new Color(200, 150, 100));
            g2d.fillRect(0, getHeight()/2, getWidth(), getHeight()/2);

            // Sun
            g2d.setColor(new Color(255, 80, 40, 180));
            g2d.fillOval(getWidth() - 180, 50, 120, 120);

            // Great Wall of China
            drawGreatWall(g2d);

            // Mountains background
            g2d.setColor(new Color(100, 120, 100));
            for (int i = 0; i < 5; i++) {
                int[] xPoints = {100 + i * 200, 200 + i * 200, 300 + i * 200};
                int[] yPoints = {getHeight() - 200, getHeight() - 350, getHeight() - 200};
                g2d.fillPolygon(xPoints, yPoints, 3);
            }

            // Clouds
            for (CloudParticle c : clouds) {
                g2d.setColor(new Color(255, 255, 255, 150));
                g2d.fillOval(c.x, c.y, 60, 30);
                g2d.fillOval(c.x + 20, c.y - 15, 40, 40);
                g2d.fillOval(c.x - 15, c.y - 10, 35, 35);
            }

            // Chinese calligraphy decoration
            g2d.setColor(new Color(200, 50, 50, 100));
            g2d.setFont(new Font("SimSun", Font.BOLD, 30));
            g2d.drawString("武", 50, 100);
            g2d.drawString("林", 120, 100);
            g2d.drawString("大", 190, 100);
            g2d.drawString("会", 260, 100);
            g2d.drawString("⚔️", 1150, 100);

            // Boxing ring with Chinese theme
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillRect(120, 110, 1060, 380);
            g2d.setColor(new Color(210, 180, 140));
            g2d.fillRect(130, 120, 1040, 360);

            // Ring ropes with dragon scale pattern
            for (int i = 0; i < 4; i++) {
                g2d.setStroke(new BasicStroke(4));
                int yPos = 140 + i * 80;
                g2d.setColor(new Color(255, 50, 50));
                g2d.drawLine(120, yPos, 1180, yPos);
                g2d.setColor(new Color(255, 200, 0));
                g2d.drawLine(122, yPos + 2, 1178, yPos + 2);
            }

            // Draw boxers
            if (player1 != null) drawChineseBoxer(g2d, player1X, player1Y, player1.getColor(), player1.getName(), player1.getFlag(), player1.getStyle(), player1.getHealth(), true);
            if (player2 != null) drawChineseBoxer(g2d, player2X, player2Y, player2.getColor(), player2.getName(), player2.getFlag(), player2.getStyle(), player2.getHealth(), false);

            // Chi/Health bars
            if (player1 != null) drawChineseHealthBar(g2d, player1X - 100, player1Y - 60, 180, 25, player1.getHealth(), player1.getName(), player1Chi);
            if (player2 != null) drawChineseHealthBar(g2d, player2X - 80, player2Y - 60, 180, 25, player2.getHealth(), player2.getName(), player2Chi);

            // Block indicators
            if (player1 != null && player1.isBlocking()) {
                drawChineseText(g2d, "⚡ 太极防御 TAI CHI ⚡", player1X - 80, player1Y - 90, new Color(100, 255, 255));
            }
            if (player2 != null && player2.isBlocking()) {
                drawChineseText(g2d, "⚡ 武当防御 WUDANG ⚡", player2X - 80, player2Y - 90, new Color(100, 255, 255));
            }

            // Dragons
            for (DragonParticle d : dragons) {
                g2d.setColor(d.color);
                g2d.fillOval(d.x, d.y, 15, 10);
                g2d.fillOval(d.x + 8, d.y - 5, 8, 8);
                g2d.fillOval(d.x - 5, d.y - 3, 6, 6);
            }

            // Fireworks
            for (FireworkParticle f : fireworks) {
                g2d.setColor(f.color);
                g2d.fillOval(f.x, f.y, 4, 4);
            }

            // Flash effects
            if (flashAlpha > 0) {
                if (showPunch) {
                    g2d.setColor(new Color(255, isCritical ? 100 : 200, 0, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    g2d.setColor(new Color(255, 255, 0, flashAlpha));
                    g2d.setFont(new Font("SimSun", Font.BOLD, 40));
                    String impactText = isCritical ? "⭐ 会心一击！⭐" : "👊 砰！👊";
                    drawCenteredString(g2d, impactText, getWidth()/2, getHeight()/2);
                } else if (showBlock) {
                    g2d.setColor(new Color(0, 200, 255, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    drawChineseText(g2d, "🛡️ 格挡成功！ 🛡️", getWidth()/2 - 80, getHeight()/2, Color.WHITE);
                } else if (showSpecial) {
                    g2d.setColor(new Color(255, 100, 0, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    drawChineseText(g2d, "🐉 降龙十八掌！ 🐉", getWidth()/2 - 100, getHeight()/2, Color.YELLOW);
                }
            }

            if (roundFlashAlpha > 0) {
                g2d.setColor(new Color(255, 215, 0, roundFlashAlpha));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(200, 50, 50));
                g2d.setFont(new Font("SimSun", Font.BOLD, 70));
                drawCenteredString(g2d, "第" + currentRound + "回合", getWidth()/2, getHeight()/2);
                g2d.setFont(new Font("SimSun", Font.BOLD, 30));
                drawCenteredString(g2d, "⚔️ 比武开始！ ⚔️", getWidth()/2, getHeight()/2 + 60);
            }

            if (victoryFlashAlpha > 0) {
                g2d.setColor(new Color(255, 215, 0, victoryFlashAlpha));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(200, 50, 50));
                g2d.setFont(new Font("SimSun", Font.BOLD, 50));
                drawCenteredString(g2d, "🏆 武林盟主 🏆", getWidth()/2, getHeight()/2 - 80);
                g2d.setColor(new Color(255, 100, 0));
                g2d.setFont(new Font("SimSun", Font.BOLD, 35));
                drawCenteredString(g2d, victorName, getWidth()/2, getHeight()/2);
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("SimSun", Font.BOLD, 25));
                drawCenteredString(g2d, "长城之巅的新英雄", getWidth()/2, getHeight()/2 + 50);
            }
        }

        private void drawGreatWall(Graphics2D g) {
            // Wall base
            g.setColor(new Color(139, 90, 43));
            for (int i = 0; i < 15; i++) {
                g.fillRect(50 + i * 80, getHeight() - 220, 70, 120);
            }

            // Wall top with battlements
            g.setColor(new Color(160, 110, 60));
            for (int i = 0; i < 30; i++) {
                int x = 40 + i * 40;
                g.fillRect(x, getHeight() - 230, 20, 30);
                g.fillRect(x + 20, getHeight() - 240, 20, 40);
            }

            // Watchtowers
            for (int i = 0; i < 4; i++) {
                int x = 100 + i * 300;
                g.setColor(new Color(120, 80, 40));
                g.fillRect(x, getHeight() - 280, 60, 180);
                g.setColor(new Color(160, 110, 60));
                g.fillRect(x + 10, getHeight() - 300, 40, 40);
                g.setColor(Color.BLACK);
                g.fillRect(x + 20, getHeight() - 280, 8, 20);
                g.fillRect(x + 32, getHeight() - 280, 8, 20);
            }

            // Chinese flag on towers
            g.setColor(Color.RED);
            for (int i = 0; i < 4; i++) {
                int x = 110 + i * 300;
                g.fillRect(x + 20, getHeight() - 320, 20, 15);
                g.setColor(Color.YELLOW);
                g.fillStar(x + 25, getHeight() - 315, 5);
            }
        }

        private void drawChineseBoxer(Graphics2D g, int x, int y, Color color, String name, String flag, String style, double health, boolean isLeft) {
            // Body with Shaolin robe
            g.setColor(color);
            g.fillOval(x - 35, y - 50, 70, 85);
            g.fillRect(x - 30, y - 35, 60, 85);

            // Robe details
            g.setColor(Color.YELLOW);
            g.drawLine(x - 15, y - 30, x - 15, y + 40);
            g.drawLine(x + 15, y - 30, x + 15, y + 40);

            // Head
            g.fillOval(x - 30, y - 75, 60, 60);

            // Shaolin headband
            g.setColor(Color.RED);
            g.fillRect(x - 32, y - 82, 64, 8);
            g.setColor(Color.YELLOW);
            g.drawString("武", x - 5, y - 78);

            // Eyes - determined look
            g.setColor(Color.WHITE);
            g.fillOval(x - 22, y - 70, 14, 16);
            g.fillOval(x + 8, y - 70, 14, 16);
            g.setColor(Color.BLACK);
            g.fillOval(x - 19, y - 68, 9, 11);
            g.fillOval(x + 10, y - 68, 9, 11);
            g.setColor(Color.RED);
            g.fillOval(x - 18, y - 69, 3, 3);
            g.fillOval(x + 11, y - 69, 3, 3);

            // Mouth - focused
            g.setColor(Color.BLACK);
            g.drawLine(x - 8, y - 52, x + 8, y - 52);

            // Chinese beard (sage look)
            g.setColor(Color.BLACK);
            g.fillRect(x - 8, y - 48, 16, 8);

            // Kung fu sash
            g.setColor(new Color(255, 215, 0));
            g.fillRect(x - 25, y - 20, 50, 8);
            g.drawString(style.substring(0, Math.min(2, style.length())), x - 8, y - 14);

            // Boxing gloves with Chinese knot design
            g.setColor(new Color(200, 50, 50));
            g.fillOval(x - 55, y - 25, 38, 38);
            g.fillOval(x + 17, y - 25, 38, 38);

            // Chinese knot decoration
            g.setColor(Color.YELLOW);
            g.drawOval(x - 48, y - 18, 10, 10);
            g.drawOval(x + 24, y - 18, 10, 10);
            g.drawLine(x - 43, y - 13, x - 53, y - 8);
            g.drawLine(x + 29, y - 13, x + 39, y - 8);

            // Name
            g.setColor(Color.YELLOW);
            g.setFont(new Font("SimSun", Font.BOLD, 14));
            g.drawString(name, x - 25, y - 100);
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.drawString(flag, x - 10, y - 88);
        }

        private void drawChineseHealthBar(Graphics2D g, int x, int y, int width, int height, double health, String name, int chi) {
            g.setColor(new Color(80, 40, 20));
            g.fillRect(x, y, width, height);

            int healthWidth = (int)(width * (health / 100));
            Color healthColor = health > 60 ? new Color(0, 150, 0) : health > 30 ? new Color(255, 200, 0) : new Color(200, 0, 0);
            g.setColor(healthColor);
            g.fillRect(x, y, healthWidth, height);

            // Chi bar below health
            g.setColor(new Color(100, 50, 100));
            g.fillRect(x, y + height + 2, width, 8);
            g.setColor(new Color(255, 100, 255));
            g.fillRect(x, y + height + 2, (int)(width * chi / 100), 8);

            g.setColor(Color.YELLOW);
            g.drawRect(x, y, width, height);
            g.drawRect(x, y + height + 2, width, 8);
            g.setFont(new Font("SimSun", Font.BOLD, 11));
            g.drawString(name, x + 5, y - 5);
            g.drawString("气:" + chi + "%", x + width - 40, y + height + 10);
        }

        private void drawChineseText(Graphics2D g, String text, int x, int y, Color color) {
            g.setColor(color);
            g.setFont(new Font("SimSun", Font.BOLD, 20));
            g.drawString(text, x, y);
        }

        private void drawCenteredString(Graphics2D g, String text, int x, int y) {
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g.drawString(text, x - textWidth/2, y);
        }

        class DragonParticle {
            int x, y, vx, vy, life;
            Color color;

            DragonParticle(int x, int y) {
                this.x = x;
                this.y = y;
                this.vx = random.nextInt(20) - 10;
                this.vy = random.nextInt(15) - 20;
                this.life = 50;
                this.color = new Color(random.nextInt(255), random.nextInt(100) + 155, 0);
            }

            void update() {
                x += vx;
                y += vy;
                vy += 0.5;
                life--;
            }
        }

        class FireworkParticle {
            int x, y, vx, vy, life;
            Color color;

            FireworkParticle(int x, int y) {
                this.x = x;
                this.y = y;
                this.vx = random.nextInt(15) - 7;
                this.vy = random.nextInt(15) - 20;
                this.life = 40;
                this.color = new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255));
            }

            void update() {
                x += vx;
                y += vy;
                vy += 0.5;
                life--;
            }
        }

        class CloudParticle {
            int x, y, life;

            CloudParticle(int x, int y) {
                this.x = x;
                this.y = y;
                this.life = 200;
            }

            void update() {
                x += 1;
                if (x > getWidth() + 100) x = -100;
                life--;
                if (life <= 0) life = 200;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GreatWallBoxingGame());
    }
}