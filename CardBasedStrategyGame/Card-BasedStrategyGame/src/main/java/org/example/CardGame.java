package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class CardGame extends JFrame {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int CARD_WIDTH = 80;
    private static final int CARD_HEIGHT = 120;
    private GamePanel gamePanel;
    private ArrayList<Card> deck;
    private ArrayList<Card> playerHand;
    private ArrayList<Card> opponentHand;
    private Card selectedCard;
    private boolean playerTurn;
    private int playerHealth;
    private int opponentHealth;
    private Timer animationTimer;

    public CardGame() {
        setTitle("Card-Based Strategy Game");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        add(gamePanel);
        initializeGame();
        setupMouseListeners();
        startAnimation();
    }

    private void initializeGame() {
        deck = new ArrayList<>();
        playerHand = new ArrayList<>();
        opponentHand = new ArrayList<>();
        playerHealth = 30;
        opponentHealth = 30;
        playerTurn = true;

        // Initialize deck with various cards
        String[] cardTypes = {"Warrior", "Mage", "Archer"};
        for (String type : cardTypes) {
            for (int i = 1; i <= 10; i++) {
                deck.add(new Card(type, i, i * 2, i));
            }
        }
        Collections.shuffle(deck);

        // Deal initial hands
        for (int i = 0; i < 5; i++) {
            playerHand.add(deck.remove(0));
            opponentHand.add(deck.remove(0));
        }
    }

    private void setupMouseListeners() {
        gamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (playerTurn) {
                    handleCardSelection(e.getX(), e.getY());
                }
            }
        });
    }

    private void startAnimation() {
        animationTimer = new Timer(16, e -> {
            gamePanel.updateAnimations();
            gamePanel.repaint();
        });
        animationTimer.start();
    }

    private void handleCardSelection(int x, int y) {
        // Check if a card in player's hand was clicked
        for (Card card : playerHand) {
            if (card.isClicked(x, y)) {
                if (selectedCard == card) {
                    playCard(card);
                    selectedCard = null;
                } else {
                    selectedCard = card;
                }
                gamePanel.repaint();
                return;
            }
        }
    }

    private void playCard(Card card) {
        if (playerTurn && card.getCost() <= playerHealth) {
            playerHealth -= card.getCost();
            opponentHealth -= card.getDamage();
            playerHand.remove(card);
            gamePanel.addAnimation(new CardAnimation(card, card.getX(), card.getY(),
                    WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2));

            if (opponentHealth <= 0) {
                JOptionPane.showMessageDialog(this, "Player Wins!");
                System.exit(0);
            }

            playerTurn = false;
            opponentTurn();
        }
    }

    private void opponentTurn() {
        Timer timer = new Timer(1000, e -> {
            if (!opponentHand.isEmpty()) {
                Random rand = new Random();
                Card card = opponentHand.get(rand.nextInt(opponentHand.size()));
                opponentHealth -= card.getCost();
                playerHealth -= card.getDamage();
                opponentHand.remove(card);
                gamePanel.addAnimation(new CardAnimation(card, card.getX(), card.getY(),
                        WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2));

                if (playerHealth <= 0) {
                    JOptionPane.showMessageDialog(this, "Opponent Wins!");
                    System.exit(0);
                }
            }
            playerTurn = true;
            drawCard();
            gamePanel.repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void drawCard() {
        if (!deck.isEmpty()) {
            playerHand.add(deck.remove(0));
            opponentHand.add(deck.remove(0));
        }
    }

    class Card {
        private String type;
        private int cost;
        private int damage;
        private int health;
        private int x, y;
        private boolean isSelected;

        public Card(String type, int cost, int damage, int health) {
            this.type = type;
            this.cost = cost;
            this.damage = damage;
            this.health = health;
            this.isSelected = false;
        }

        public boolean isClicked(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + CARD_WIDTH &&
                    mouseY >= y && mouseY <= y + CARD_HEIGHT;
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public String getType() { return type; }
        public int getCost() { return cost; }
        public int getDamage() { return damage; }
        public int getHealth() { return health; }
        public void setSelected(boolean selected) { this.isSelected = selected; }
        public boolean isSelected() { return isSelected; }
    }

    class CardAnimation {
        private Card card;
        private double startX, startY;
        private double targetX, targetY;
        private double currentX, currentY;
        private long startTime;
        private static final int ANIMATION_DURATION = 500;

        public CardAnimation(Card card, double startX, double startY, double targetX, double targetY) {
            this.card = card;
            this.startX = startX;
            this.startY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.currentX = startX;
            this.currentY = startY;
            this.startTime = System.currentTimeMillis();
        }

        public boolean update() {
            long currentTime = System.currentTimeMillis();
            double progress = (double)(currentTime - startTime) / ANIMATION_DURATION;

            if (progress >= 1.0) {
                currentX = targetX;
                currentY = targetY;
                return false;
            }

            currentX = startX + (targetX - startX) * progress;
            currentY = startY + (targetY - startY) * progress;
            return true;
        }

        public void draw(Graphics g) {
            drawCard(g, card, (int)currentX, (int)currentY);
        }

        private void drawCard(Graphics g, Card card, int currentX, int currentY) {
        }
    }

    class GamePanel extends JPanel {
        private ArrayList<CardAnimation> animations;

        public GamePanel() {
            animations = new ArrayList<>();
            setBackground(new Color(34, 139, 34)); // Green table-like background
        }

        public void addAnimation(CardAnimation animation) {
            animations.add(animation);
        }

        public void updateAnimations() {
            animations.removeIf(animation -> !animation.update());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw health bars
            drawHealthBars(g2d);

            // Draw player's hand
            int startX = (WINDOW_WIDTH - (playerHand.size() * CARD_WIDTH + (playerHand.size() - 1) * 10)) / 2;
            for (int i = 0; i < playerHand.size(); i++) {
                Card card = playerHand.get(i);
                card.setPosition(startX + i * (CARD_WIDTH + 10), WINDOW_HEIGHT - CARD_HEIGHT - 20);
                drawCard(g2d, card, card.getX(), card.getY());
            }

            // Draw opponent's hand (face down)
            startX = (WINDOW_WIDTH - (opponentHand.size() * CARD_WIDTH + (opponentHand.size() - 1) * 10)) / 2;
            for (int i = 0; i < opponentHand.size(); i++) {
                drawCardBack(g2d, startX + i * (CARD_WIDTH + 10), 20);
            }

            // Draw animations
            for (CardAnimation animation : animations) {
                animation.draw(g2d);
            }

            // Draw game status
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Player Turn: " + playerTurn, 20, WINDOW_HEIGHT - 20);
        }

        private void drawHealthBars(Graphics g) {
            // Player health
            g.setColor(Color.RED);
            g.fillRect(20, WINDOW_HEIGHT - 70, playerHealth * 10, 20);
            g.setColor(Color.BLACK);
            g.drawRect(20, WINDOW_HEIGHT - 70, 300, 20);
            g.drawString("Player: " + playerHealth, 20, WINDOW_HEIGHT - 75);

            // Opponent health
            g.setColor(Color.RED);
            g.fillRect(20, 50, opponentHealth * 10, 20);
            g.setColor(Color.BLACK);
            g.drawRect(20, 50, 300, 20);
            g.drawString("Opponent: " + opponentHealth, 20, 45);
        }

        private void drawCard(Graphics g, Card card, int x, int y) {
            // Card background
            g.setColor(card.isSelected() ? Color.YELLOW : Color.WHITE);
            g.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
            g.setColor(Color.BLACK);
            g.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);

            // Card details
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString(card.getType(), x + 5, y + 20);
            g.drawString("Cost: " + card.getCost(), x + 5, y + 40);
            g.drawString("Dmg: " + card.getDamage(), x + 5, y + 60);
            g.drawString("HP: " + card.getHealth(), x + 5, y + 80);

            // Card border effect
            if (card.isSelected()) {
                g.setColor(Color.BLUE);
                g.drawRoundRect(x - 2, y - 2, CARD_WIDTH + 4, CARD_HEIGHT + 4, 12, 12);
            }
        }

        private void drawCardBack(Graphics g, int x, int y) {
            g.setColor(Color.BLUE);
            g.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
            g.setColor(Color.WHITE);
            g.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("CARD", x + 20, y + CARD_HEIGHT / 2);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CardGame game = new CardGame();
            game.setVisible(true);
        });
    }
}