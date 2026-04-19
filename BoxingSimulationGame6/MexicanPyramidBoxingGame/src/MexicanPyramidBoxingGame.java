import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.util.ArrayList;

public class MexicanPyramidBoxingGame extends JFrame {
    private GamePanel gamePanel;
    private JButton punchButton, blockButton, specialButton;
    private JLabel player1HealthLabel, player2HealthLabel, messageLabel;
    private JLabel roundLabel, timerLabel, sunStoneLabel;
    private Timer gameTimer;
    private Boxer player1, player2;
    private boolean isPlayerTurn;
    private int currentRound;
    private int roundTime;
    private boolean gameActive;
    private boolean vsComputer;
    private Random random;
    private int player1SunStones, player2SunStones;
    private String[] aztecWords = {"Ollin", "Movement", "Ehecatl", "Wind", "Tecpatl", "Flint", "Xochitl", "Flower"};

    public MexicanPyramidBoxingGame() {
        setTitle("🌞 MEXICAN PYRAMID BOXING - Teotihuacan 🌙");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1300, 800);
        setLocationRelativeTo(null);

        random = new Random();
        player1SunStones = 0;
        player2SunStones = 0;
        setupMenu();
        setupGamePanel();
        setupControlPanel();
        setupInfoPanel();

        // Initialize placeholder boxers
        player1 = new Boxer("Águila Guerrera", "🦅", new Color(255, 100, 50));
        player2 = new Boxer("Jaguar Guerrero", "🐆", new Color(200, 80, 0));
        gameActive = false;

        SwingUtilities.invokeLater(() -> showNewGameDialog());
        setVisible(true);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(160, 80, 20));

        JMenu gameMenu = new JMenu("🌞 JUEGO GAME");
        gameMenu.setForeground(Color.YELLOW);
        JMenuItem newGameItem = new JMenuItem("✨ Nueva Pelea New Match");
        JMenuItem vsComputerItem = new JMenuItem("🤖 vs Computadora Computer");
        JMenuItem vsPlayerItem = new JMenuItem("👥 vs Jugador Player");
        JMenuItem exitItem = new JMenuItem("✖ Salir Exit");

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

        JMenu deityMenu = new JMenu("🦅 DEIDADES DEITIES");
        deityMenu.setForeground(Color.CYAN);
        String[] deities = {"Quetzalcóatl", "Tezcatlipoca", "Huitzilopochtli", "Tlaloc", "Chalchiuhtlicue"};
        for (String deity : deities) {
            JMenuItem deityItem = new JMenuItem(deity);
            deityItem.addActionListener(e -> invokeDeityBlessing(deity));
            deityMenu.add(deityItem);
        }
        menuBar.add(deityMenu);

        setJMenuBar(menuBar);
    }

    private void invokeDeityBlessing(String deity) {
        if (gameActive && isPlayerTurn) {
            int bonus = random.nextInt(20) + 10;
            messageLabel.setText("🌞 " + deity + " blesses you with +" + bonus + " damage! 🌞");
            player1SunStones = Math.min(100, player1SunStones + bonus);
            updateSunStoneBar();
        }
    }

    private void showNewGameDialog() {
        String[] options = {"🦅 Pelear vs Computadora Fight Computer", "👥 Dos Jugadores Two Players"};
        int choice = JOptionPane.showOptionDialog(this,
                "Elige tu batalla:", "BOXEO PIRAMIDAL",
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
                        else messageLabel.setText("🌞 Turno del Águila Eagle's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_K) {
                        if (isPlayerTurn) performBlock();
                        else messageLabel.setText("🌞 Turno del Águila Eagle's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_L) {
                        if (isPlayerTurn) performSpecial();
                        else messageLabel.setText("🌞 Turno del Águila Eagle's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_A) {
                        if (!isPlayerTurn) performPunch2();
                        else messageLabel.setText("🌙 Turno del Jaguar Jaguar's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_S) {
                        if (!isPlayerTurn) performBlock2();
                        else messageLabel.setText("🌙 Turno del Jaguar Jaguar's turn!");
                    } else if (e.getKeyCode() == KeyEvent.VK_D) {
                        if (!isPlayerTurn) performSpecial2();
                        else messageLabel.setText("🌙 Turno del Jaguar Jaguar's turn!");
                    }
                }
            }
        });
    }

    private void setupControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(160, 80, 20));
        controlPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0), 3));

        punchButton = createAztecButton("👊 GOLPE PUNCH (J)", new Color(255, 80, 80));
        blockButton = createAztecButton("🛡️ BLOQUEO BLOCK (K)", new Color(80, 150, 255));
        specialButton = createAztecButton("🌞 PIEDRA DEL SOL (L)", new Color(255, 215, 0));

        punchButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performPunch(); });
        blockButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performBlock(); });
        specialButton.addActionListener(e -> { if (gameActive && isPlayerTurn) performSpecial(); });

        controlPanel.add(punchButton);
        controlPanel.add(blockButton);
        controlPanel.add(specialButton);

        JLabel controls2p = new JLabel("  Dos Jugadores: Jugador 2 usa A/Golpe | S/Bloqueo | D/Especial");
        controls2p.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        controls2p.setForeground(Color.YELLOW);
        controlPanel.add(controls2p);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private JButton createAztecButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 215, 0), 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        return button;
    }

    private void setupInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(2, 4, 15, 5));
        infoPanel.setBackground(new Color(100, 50, 20));
        infoPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0), 2));

        player1HealthLabel = new JLabel("❤️ ÁGUILA: 100%", SwingConstants.CENTER);
        player1HealthLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        player1HealthLabel.setForeground(Color.WHITE);

        player2HealthLabel = new JLabel("❤️ JAGUAR: 100%", SwingConstants.CENTER);
        player2HealthLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        player2HealthLabel.setForeground(Color.WHITE);

        roundLabel = new JLabel("🌞 ROUND 1 🌙", SwingConstants.CENTER);
        roundLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        roundLabel.setForeground(Color.YELLOW);

        timerLabel = new JLabel("⏱️ 3:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        timerLabel.setForeground(Color.CYAN);

        sunStoneLabel = new JLabel("🌞 PIEDRAS DEL SOL: 0-0", SwingConstants.CENTER);
        sunStoneLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sunStoneLabel.setForeground(new Color(255, 165, 0));

        messageLabel = new JLabel("🌞 TEOTIHUACAN - Batalla Sagrada 🌙", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        messageLabel.setForeground(Color.YELLOW);

        infoPanel.add(player1HealthLabel);
        infoPanel.add(player2HealthLabel);
        infoPanel.add(roundLabel);
        infoPanel.add(timerLabel);
        infoPanel.add(sunStoneLabel);
        infoPanel.add(new JLabel());
        infoPanel.add(messageLabel);
        infoPanel.add(new JLabel());

        add(infoPanel, BorderLayout.NORTH);
    }

    private void startNewGame(boolean vsComputer) {
        this.vsComputer = vsComputer;
        player1 = new Boxer("Águila Guerrera", "🦅", new Color(255, 100, 50));
        player1.setTitle("Águila");
        player2 = new Boxer(vsComputer ? "Jaguar Guerrero" : "Serpiente Emplumada",
                vsComputer ? "🐆" : "🐍",
                vsComputer ? new Color(200, 80, 0) : new Color(0, 150, 100));
        if (!vsComputer) player2.setTitle("Serpiente");
        else player2.setTitle("Jaguar");

        currentRound = 1;
        roundTime = 180;
        gameActive = true;
        isPlayerTurn = true;
        player1SunStones = 0;
        player2SunStones = 0;

        updateUI();
        roundLabel.setText("🌞 ROUND " + currentRound + " 🌙");

        if (gameTimer != null && gameTimer.isRunning()) gameTimer.stop();
        gameTimer = new Timer(1000, e -> updateTimer());
        gameTimer.start();

        messageLabel.setText("🌞 ¡COMIENZA LA BATALLA! " + player1.getName() + " ataca primero! 🌞");
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
        if (currentRound < 9) {
            currentRound++;
            roundTime = 180;
            roundLabel.setText("🌞 ROUND " + currentRound + " 🌙");
            messageLabel.setText("🌞 ROUND " + currentRound + " - ¡SIGUE LA BATALLA! 🌞");
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
        boolean critical = random.nextInt(100) < 18;
        if (critical) damage *= 2;

        player2.takeDamage(damage);
        player1SunStones = Math.min(100, player1SunStones + 8);
        updateSunStoneBar();

        updateUI();
        gamePanel.showPunchAnimation(true, critical);

        String aztecWord = aztecWords[random.nextInt(aztecWords.length)];
        if (critical) {
            messageLabel.setText("⭐ ¡" + aztecWord.toUpperCase() + " CRÍTICO! " + damage + " daño! ⭐");
        } else {
            messageLabel.setText("👊 ¡" + aztecWord + "! " + damage + " daño!");
        }

        if (player2.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = false;
            messageLabel.setText(vsComputer ? "🐆 ¡Turno del Jaguar Guerrero!" : "🌙 ¡Turno del Jugador 2!");

            if (vsComputer) {
                computerTurn();
            }
        }

        gamePanel.repaint();
    }

    private void performBlock() {
        if (!gameActive || !isPlayerTurn) return;

        gamePanel.showBlockAnimation(true);
        messageLabel.setText("🛡️ ¡BLOQUEO ÁGUILA! EAGLE BLOCK! 🛡️");
        player1.setBlocking(true);
        player1SunStones = Math.max(0, player1SunStones - 15);
        updateSunStoneBar();

        isPlayerTurn = false;
        messageLabel.setText(vsComputer ? "🐆 ¡Turno del Jaguar Guerrero!" : "🌙 ¡Turno del Jugador 2!");

        if (vsComputer) {
            computerTurn();
        }

        gamePanel.repaint();
    }

    private void performSpecial() {
        if (!gameActive || !isPlayerTurn || player2 == null) return;

        if (player1SunStones < 40) {
            messageLabel.setText("⚠️ ¡Piedras del Sol insuficientes! Necesitas 40%! ⚠️");
            return;
        }

        int damage = random.nextInt(40) + 35;
        player2.takeDamage(damage);
        player1SunStones = Math.max(0, player1SunStones - 40);
        updateSunStoneBar();
        updateUI();

        gamePanel.showSpecialEffect(true);
        messageLabel.setText("🌞 ¡PIEDRA DEL SOL! " + damage + " daño! SUN STONE POWER! 🌞");

        if (player2.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = false;
            messageLabel.setText(vsComputer ? "🐆 ¡Turno del Jaguar Guerrero!" : "🌙 ¡Turno del Jugador 2!");

            if (vsComputer) {
                computerTurn();
            }
        }

        gamePanel.repaint();
    }

    private void performPunch2() {
        if (!gameActive || isPlayerTurn || player1 == null) return;

        int damage = random.nextInt(12) + 8;
        boolean critical = random.nextInt(100) < 18;
        if (critical) damage *= 2;

        player1.takeDamage(damage);
        player2SunStones = Math.min(100, player2SunStones + 8);
        updateSunStoneBar();
        updateUI();

        gamePanel.showPunchAnimation(false, critical);
        String aztecWord = aztecWords[random.nextInt(aztecWords.length)];
        messageLabel.setText((critical ? "⭐ ¡" + aztecWord.toUpperCase() + " CRÍTICO! " : "👊 ¡" + aztecWord + "! ") + damage + " daño!");

        if (player1.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = true;
            messageLabel.setText("🌞 ¡Turno del Águila Guerrera!");
        }

        gamePanel.repaint();
    }

    private void performBlock2() {
        if (!gameActive || isPlayerTurn) return;

        gamePanel.showBlockAnimation(false);
        messageLabel.setText("🛡️ ¡BLOQUEO JAGUAR! JAGUAR BLOCK! 🛡️");
        player2.setBlocking(true);
        player2SunStones = Math.max(0, player2SunStones - 15);
        updateSunStoneBar();

        isPlayerTurn = true;
        messageLabel.setText("🌞 ¡Turno del Águila Guerrera!");

        gamePanel.repaint();
    }

    private void performSpecial2() {
        if (!gameActive || isPlayerTurn || player1 == null) return;

        if (player2SunStones < 40) {
            messageLabel.setText("⚠️ ¡Piedras del Sol insuficientes! Necesitas 40%! ⚠️");
            return;
        }

        int damage = random.nextInt(40) + 35;
        player1.takeDamage(damage);
        player2SunStones = Math.max(0, player2SunStones - 40);
        updateSunStoneBar();
        updateUI();

        gamePanel.showSpecialEffect(false);
        messageLabel.setText("🌙 ¡FUERZA JAGUAR! " + damage + " daño! JAGUAR POWER! 🌙");

        if (player1.getHealth() <= 0) {
            endGame();
        } else {
            isPlayerTurn = true;
            messageLabel.setText("🌞 ¡Turno del Águila Guerrera!");
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

                // Computer uses special if sun stones >= 40
                if (player2SunStones >= 40 && action < 65) {
                    int damage = random.nextInt(40) + 35;
                    player1.takeDamage(damage);
                    player2SunStones = Math.max(0, player2SunStones - 40);
                    updateSunStoneBar();
                    updateUI();
                    gamePanel.showSpecialEffect(false);
                    messageLabel.setText("🌙 ¡FUERZA JAGUAR! " + damage + " daño! JAGUAR POWER! 🌙");

                    if (player1.getHealth() <= 0) {
                        endGame();
                    }
                } else if (action < 55) {
                    int damage = random.nextInt(12) + 8;
                    boolean critical = random.nextInt(100) < 18;
                    if (critical) damage *= 2;

                    if (player1.isBlocking()) {
                        damage /= 2;
                        messageLabel.setText("🐆 ¡Golpe del Jaguar parcialmente bloqueado!");
                        player1.setBlocking(false);
                    }

                    player1.takeDamage(damage);
                    player2SunStones = Math.min(100, player2SunStones + 8);
                    updateSunStoneBar();
                    updateUI();
                    gamePanel.showPunchAnimation(false, critical);
                    String aztecWord = aztecWords[random.nextInt(aztecWords.length)];
                    messageLabel.setText((critical ? "⭐ ¡" + aztecWord.toUpperCase() + " CRÍTICO! " : "👊 ¡" + aztecWord + "! ") + damage + " daño!");

                    if (player1.getHealth() <= 0) {
                        endGame();
                    }
                } else if (action < 75) {
                    gamePanel.showBlockAnimation(false);
                    messageLabel.setText("🛡️ ¡BLOQUEO JAGUAR! JAGUAR BLOCK! 🛡️");
                    player2.setBlocking(true);
                    player2SunStones = Math.max(0, player2SunStones - 15);
                    updateSunStoneBar();
                } else {
                    // Computer charges sun stones
                    player2SunStones = Math.min(100, player2SunStones + 20);
                    updateSunStoneBar();
                    messageLabel.setText("🐆 ¡" + player2.getName() + " invoca al Sol! SUN INVOCATION! 🐆");
                }

                isPlayerTurn = true;
                messageLabel.setText("🌞 ¡Tu turno, " + player1.getName() + "! 🌞");
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

    private void updateSunStoneBar() {
        sunStoneLabel.setText(String.format("🌞 PIEDRAS DEL SOL: %d-%d", player1SunStones, player2SunStones));
        if (player1SunStones >= 40) {
            sunStoneLabel.setForeground(Color.RED);
            specialButton.setBackground(new Color(255, 100, 0));
            specialButton.setText("🌞 PIEDRA DEL SOL READY! (L)");
        } else {
            sunStoneLabel.setForeground(new Color(255, 165, 0));
            specialButton.setBackground(new Color(255, 215, 0));
            specialButton.setText("🌞 PIEDRA DEL SOL (L)");
        }

        if (player2SunStones >= 40 && vsComputer) {
            messageLabel.setText("⚠️ ¡CUIDADO! El Jaguar tiene poder del Sol! ⚠️");
        }
    }

    private void endGame() {
        gameActive = false;
        if (gameTimer != null) gameTimer.stop();

        String winner;
        String aztecWinner;
        if (player1 == null || player1.getHealth() <= 0) {
            winner = player2.getName() + " " + player2.getSymbol();
            aztecWinner = player2.getName();
        } else {
            winner = player1.getName() + " " + player1.getSymbol();
            aztecWinner = player1.getName();
        }

        gamePanel.showVictoryEffect(winner, aztecWinner);

        int option = JOptionPane.showConfirmDialog(this,
                "🏆 ¡" + winner + " ES EL CAMPEÓN! 🏆\n\n" +
                        "🌞 ¡HONOR Y GLORIA EN TEOTIHUACAN! 🌙\n\n" +
                        "¿Jugar de nuevo? Play again?",
                "🏆 CAMPEÓN AZTECA 🏆",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            player1SunStones = 0;
            player2SunStones = 0;
            showNewGameDialog();
        } else {
            System.exit(0);
        }
    }

    class Boxer {
        private String name;
        private String symbol;
        private double health;
        private boolean isBlocking;
        private Color color;
        private String title;

        public Boxer(String name, String symbol, Color color) {
            this.name = name;
            this.symbol = symbol;
            this.color = color;
            this.health = 100;
            this.isBlocking = false;
            this.title = "Guerrero";
        }

        public void setTitle(String title) {
            this.title = title;
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
        public String getTitle() { return title; }
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
        private String aztecVictor = "";
        private int player1X = 300, player2X = 900;
        private int player1Y = 320, player2Y = 320;
        private ArrayList<SunParticle> suns = new ArrayList<>();
        private ArrayList<JaguarParticle> jaguars = new ArrayList<>();
        private ArrayList<EagleParticle> eagles = new ArrayList<>();

        public GamePanel() {
            Timer animationTimer = new Timer(33, e -> {
                if (flashAlpha > 0) flashAlpha -= 10;
                if (roundFlashAlpha > 0) roundFlashAlpha -= 8;
                if (victoryFlashAlpha > 0) victoryFlashAlpha -= 5;

                for (int i = suns.size() - 1; i >= 0; i--) {
                    suns.get(i).update();
                    if (suns.get(i).life <= 0) suns.remove(i);
                }

                for (int i = jaguars.size() - 1; i >= 0; i--) {
                    jaguars.get(i).update();
                    if (jaguars.get(i).life <= 0) jaguars.remove(i);
                }

                for (int i = eagles.size() - 1; i >= 0; i--) {
                    eagles.get(i).update();
                    if (eagles.get(i).life <= 0) eagles.remove(i);
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

            // Add sun particles
            for (int i = 0; i < 15; i++) {
                suns.add(new SunParticle(left ? 450 : 850, 350));
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

            // Add sun stones effect
            for (int i = 0; i < 10; i++) {
                suns.add(new SunParticle(left ? 450 : 850, 350));
            }
            if (left) {
                for (int i = 0; i < 5; i++) eagles.add(new EagleParticle(450, 350));
            } else {
                for (int i = 0; i < 5; i++) jaguars.add(new JaguarParticle(850, 350));
            }

            Timer clearFlash = new Timer(400, e -> { showSpecial = false; ((Timer)e.getSource()).stop(); });
            clearFlash.setRepeats(false);
            clearFlash.start();
        }

        public void showRoundFlash() {
            roundFlashAlpha = 200;
        }

        public void showVictoryEffect(String winner, String aztecVictor) {
            victorName = winner;
            this.aztecVictor = aztecVictor;
            victoryFlashAlpha = 255;

            // Celebration suns and eagles/jaguars
            for (int i = 0; i < 30; i++) {
                suns.add(new SunParticle(getWidth()/2 + random.nextInt(400) - 200,
                        getHeight()/2 + random.nextInt(200) - 100));
            }
            for (int i = 0; i < 15; i++) {
                eagles.add(new EagleParticle(getWidth()/2 + random.nextInt(400) - 200,
                        getHeight()/2 + random.nextInt(200) - 100));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Sky gradient - Mexican sunset
            GradientPaint skyGradient = new GradientPaint(0, 0, new Color(255, 100, 50),
                    0, getHeight()/2, new Color(255, 50, 30));
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight()/2);

            g2d.setColor(new Color(255, 140, 70));
            g2d.fillRect(0, getHeight()/2, getWidth(), getHeight()/2);

            // Sun
            g2d.setColor(new Color(255, 80, 30, 200));
            g2d.fillOval(getWidth() - 200, 30, 150, 150);

            // Mexican Pyramids (Teotihuacan)
            drawPyramid(g2d, 120, getHeight() - 300, 180, 200, new Color(180, 120, 60));
            drawPyramid(g2d, 280, getHeight() - 350, 220, 250, new Color(160, 100, 50));
            drawPyramid(g2d, 50, getHeight() - 270, 140, 170, new Color(170, 110, 55));
            drawPyramid(g2d, 800, getHeight() - 320, 200, 220, new Color(175, 115, 58));
            drawPyramid(g2d, 980, getHeight() - 280, 160, 180, new Color(165, 105, 52));

            // Pyramid of the Sun (largest)
            drawPyramid(g2d, 500, getHeight() - 380, 300, 280, new Color(190, 130, 70));
            g2d.setColor(new Color(255, 100, 50, 100));
            g2d.fillOval(620, getHeight() - 400, 60, 60);

            // Agave plants
            g2d.setColor(new Color(50, 100, 50));
            for (int i = 0; i < 12; i++) {
                int x = 100 + i * 100;
                g2d.fillArc(x, getHeight() - 160, 30, 60, 0, 180);
                g2d.fillArc(x + 15, getHeight() - 170, 20, 50, 0, 180);
            }

            // Aztec calendar (Sun Stone) decoration
            g2d.setColor(new Color(160, 80, 30));
            g2d.fillOval(50, 50, 80, 80);
            g2d.setColor(new Color(255, 165, 0));
            g2d.drawOval(55, 55, 70, 70);
            g2d.drawArc(55, 55, 70, 70, 0, 45);
            g2d.drawArc(55, 55, 70, 70, 90, 45);
            g2d.drawArc(55, 55, 70, 70, 180, 45);
            g2d.drawArc(55, 55, 70, 70, 270, 45);

            // Boxing ring with Aztec patterns
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillRect(120, 110, 1060, 380);
            g2d.setColor(new Color(210, 180, 140));
            g2d.fillRect(130, 120, 1040, 360);

            // Ring ropes with Aztec colors
            Color[] aztecColors = {new Color(255, 100, 50), new Color(255, 200, 50),
                    new Color(50, 150, 50), new Color(100, 50, 150)};
            for (int i = 0; i < 4; i++) {
                g2d.setStroke(new BasicStroke(4));
                int yPos = 140 + i * 80;
                g2d.setColor(aztecColors[i % aztecColors.length]);
                g2d.drawLine(120, yPos, 1180, yPos);
            }

            // Draw boxers
            if (player1 != null) drawAztecBoxer(g2d, player1X, player1Y, player1.getColor(), player1.getName(), player1.getSymbol(), player1.getTitle(), player1.getHealth(), true);
            if (player2 != null) drawAztecBoxer(g2d, player2X, player2Y, player2.getColor(), player2.getName(), player2.getSymbol(), player2.getTitle(), player2.getHealth(), false);

            // Health bars with Aztec design
            if (player1 != null) drawAztecHealthBar(g2d, player1X - 100, player1Y - 60, 180, 25, player1.getHealth(), player1.getName(), player1SunStones);
            if (player2 != null) drawAztecHealthBar(g2d, player2X - 80, player2Y - 60, 180, 25, player2.getHealth(), player2.getName(), player2SunStones);

            // Block indicators
            if (player1 != null && player1.isBlocking()) {
                drawAztecText(g2d, "🛡️ BLOQUEO ÁGUILA 🛡️", player1X - 80, player1Y - 90, new Color(100, 255, 255));
            }
            if (player2 != null && player2.isBlocking()) {
                drawAztecText(g2d, "🛡️ BLOQUEO JAGUAR 🛡️", player2X - 80, player2Y - 90, new Color(100, 255, 255));
            }

            // Sun particles
            for (SunParticle s : suns) {
                g2d.setColor(s.color);
                g2d.fillOval(s.x, s.y, 6, 6);
                g2d.setColor(Color.YELLOW);
                g2d.fillOval(s.x + 2, s.y + 2, 2, 2);
            }

            // Eagle particles
            for (EagleParticle e : eagles) {
                g2d.setColor(e.color);
                g2d.fillOval(e.x, e.y, 12, 8);
                g2d.fillOval(e.x + 5, e.y - 4, 6, 6);
            }

            // Jaguar particles
            for (JaguarParticle j : jaguars) {
                g2d.setColor(j.color);
                g2d.fillOval(j.x, j.y, 10, 8);
                g2d.fillOval(j.x - 3, j.y - 2, 5, 5);
            }

            // Flash effects
            if (flashAlpha > 0) {
                if (showPunch) {
                    g2d.setColor(new Color(255, isCritical ? 100 : 200, 0, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    g2d.setColor(new Color(255, 255, 0, flashAlpha));
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 40));
                    String impactText = isCritical ? "⭐ ¡GOLPE CRÍTICO! ⭐" : "👊 ¡POW! 👊";
                    drawCenteredString(g2d, impactText, getWidth()/2, getHeight()/2);
                } else if (showBlock) {
                    g2d.setColor(new Color(0, 200, 255, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    drawAztecText(g2d, "🛡️ ¡BLOQUEADO! 🛡️", getWidth()/2 - 80, getHeight()/2, Color.WHITE);
                } else if (showSpecial) {
                    g2d.setColor(new Color(255, 100, 0, flashAlpha));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    drawAztecText(g2d, "🌞 ¡PIEDRA DEL SOL! 🌞", getWidth()/2 - 100, getHeight()/2, Color.YELLOW);
                }
            }

            if (roundFlashAlpha > 0) {
                g2d.setColor(new Color(255, 215, 0, roundFlashAlpha));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(200, 50, 0));
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 70));
                drawCenteredString(g2d, "ROUND " + currentRound, getWidth()/2, getHeight()/2);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 30));
                drawCenteredString(g2d, "🌞 ¡LUCHA! 🌙", getWidth()/2, getHeight()/2 + 60);
            }

            if (victoryFlashAlpha > 0) {
                g2d.setColor(new Color(255, 215, 0, victoryFlashAlpha));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(200, 50, 0));
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 50));
                drawCenteredString(g2d, "🏆 ¡CAMPEÓN! 🏆", getWidth()/2, getHeight()/2 - 80);
                g2d.setColor(new Color(255, 100, 0));
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 35));
                drawCenteredString(g2d, victorName, getWidth()/2, getHeight()/2);
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 25));
                drawCenteredString(g2d, "HONOR EN TEOTIHUACAN", getWidth()/2, getHeight()/2 + 50);
            }
        }

        private void drawPyramid(Graphics2D g, int x, int y, int width, int height, Color color) {
            g.setColor(color);
            int[] xPoints = {x, x + width/2, x + width};
            int[] yPoints = {y + height, y, y + height};
            g.fillPolygon(xPoints, yPoints, 3);

            // Stone block lines
            g.setColor(new Color(120, 70, 30));
            for (int i = 1; i < 5; i++) {
                int stepY = y + height - (i * height / 5);
                g.drawLine((int) (x + (width/2) * (i/5.0)), stepY, (int) (x + width - (width/2) * (i/5.0)), stepY);
            }

            // Temple on top
            g.fillRect(x + width/2 - 20, y - 20, 40, 30);
        }

        private void drawAztecBoxer(Graphics2D g, int x, int y, Color color, String name, String symbol, String title, double health, boolean isLeft) {
            // Body with warrior outfit
            g.setColor(color);
            g.fillOval(x - 35, y - 50, 70, 85);
            g.fillRect(x - 30, y - 35, 60, 85);

            // Aztec chest armor
            g.setColor(new Color(255, 165, 0));
            g.fillRect(x - 20, y - 25, 40, 30);
            g.drawString(symbol, x - 8, y - 5);

            // Head
            g.fillOval(x - 30, y - 75, 60, 60);

            // Feathered headdress
            g.setColor(new Color(255, 100, 50));
            for (int i = 0; i < 5; i++) {
                int offset = -20 + i * 10;
                g.fillRect(x - 25 + offset, y - 95, 5, 25);
                g.fillRect(x + 20 - offset, y - 95, 5, 25);
            }
            g.setColor(new Color(255, 200, 50));
            g.fillRect(x - 30, y - 92, 60, 10);

            // Warrior face paint
            g.setColor(Color.WHITE);
            g.fillOval(x - 22, y - 70, 14, 16);
            g.fillOval(x + 8, y - 70, 14, 16);
            g.setColor(Color.BLACK);
            g.fillOval(x - 19, y - 68, 9, 11);
            g.fillOval(x + 10, y - 68, 9, 11);

            // Red war paint
            g.setColor(Color.RED);
            g.drawLine(x - 15, y - 75, x - 15, y - 65);
            g.drawLine(x + 15, y - 75, x + 15, y - 65);

            // Mouth - warrior yell
            g.setColor(Color.BLACK);
            g.fillOval(x - 8, y - 52, 16, 12);
            g.setColor(Color.WHITE);
            g.fillOval(x - 5, y - 51, 4, 6);
            g.fillOval(x + 1, y - 51, 4, 6);

            // Nose ring
            g.setColor(new Color(255, 215, 0));
            g.fillOval(x - 3, y - 60, 6, 6);

            // Boxing gloves with Aztec symbols
            g.setColor(new Color(200, 50, 50));
            g.fillOval(x - 55, y - 25, 38, 38);
            g.fillOval(x + 17, y - 25, 38, 38);

            // Aztec sun symbol on gloves
            g.setColor(Color.YELLOW);
            g.drawOval(x - 48, y - 18, 10, 10);
            g.drawOval(x + 24, y - 18, 10, 10);

            // Name and title
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g.drawString(name, x - 30, y - 100);
            g.setFont(new Font("Segoe UI", Font.ITALIC, 10));
            g.drawString(title, x - 20, y - 90);
        }

        private void drawAztecHealthBar(Graphics2D g, int x, int y, int width, int height, double health, String name, int sunStones) {
            g.setColor(new Color(80, 40, 20));
            g.fillRect(x, y, width, height);

            int healthWidth = (int)(width * (health / 100));
            Color healthColor = health > 60 ? new Color(0, 150, 0) : health > 30 ? new Color(255, 200, 0) : new Color(200, 0, 0);
            g.setColor(healthColor);
            g.fillRect(x, y, healthWidth, height);

            // Sun Stone bar below health
            g.setColor(new Color(100, 50, 0));
            g.fillRect(x, y + height + 2, width, 8);
            g.setColor(new Color(255, 165, 0));
            g.fillRect(x, y + height + 2, (int)(width * sunStones / 100), 8);

            g.setColor(Color.YELLOW);
            g.drawRect(x, y, width, height);
            g.drawRect(x, y + height + 2, width, 8);
            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g.drawString(name, x + 5, y - 5);
            g.drawString("🌞 " + sunStones + "%", x + width - 45, y + height + 10);
        }

        private void drawAztecText(Graphics2D g, String text, int x, int y, Color color) {
            g.setColor(color);
            g.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g.drawString(text, x, y);
        }

        private void drawCenteredString(Graphics2D g, String text, int x, int y) {
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g.drawString(text, x - textWidth/2, y);
        }

        class SunParticle {
            int x, y, vx, vy, life;
            Color color;

            SunParticle(int x, int y) {
                this.x = x;
                this.y = y;
                this.vx = random.nextInt(15) - 7;
                this.vy = random.nextInt(15) - 20;
                this.life = 40;
                this.color = new Color(255, random.nextInt(100) + 155, 0);
            }

            void update() {
                x += vx;
                y += vy;
                vy += 0.5;
                life--;
            }
        }

        class EagleParticle {
            int x, y, vx, vy, life;
            Color color;

            EagleParticle(int x, int y) {
                this.x = x;
                this.y = y;
                this.vx = random.nextInt(12) - 6;
                this.vy = random.nextInt(15) - 20;
                this.life = 35;
                this.color = new Color(255, 100, 50);
            }

            void update() {
                x += vx;
                y += vy;
                vy += 0.6;
                life--;
            }
        }

        class JaguarParticle {
            int x, y, vx, vy, life;
            Color color;

            JaguarParticle(int x, int y) {
                this.x = x;
                this.y = y;
                this.vx = random.nextInt(12) - 6;
                this.vy = random.nextInt(15) - 20;
                this.life = 35;
                this.color = new Color(200, 100, 0);
            }

            void update() {
                x += vx;
                y += vy;
                vy += 0.6;
                life--;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MexicanPyramidBoxingGame());
    }
}