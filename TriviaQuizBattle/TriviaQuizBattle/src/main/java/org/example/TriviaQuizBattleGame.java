package org.example;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class TriviaQuizBattleGame extends JFrame {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int QUESTION_TIME = 15000; // 15 seconds per question
    private static final int MAX_ROUNDS = 5;

    private JPanel mainPanel;
    private JLabel questionLabel;
    private JButton[] answerButtons;
    private JLabel timerLabel;
    private JLabel scoreLabel;
    private JLabel roundLabel;
    private JPanel animationPanel;

    private ArrayList<Question> questions;
    private int currentQuestionIndex;
    private int playerScore;
    private int currentRound;
    private Timer questionTimer;
    private Timer animationTimer;
    private boolean isGameRunning;

    // Animation variables
    private int playerX = 50;
    private int opponentX = 650;
    private int playerHealth = 100;
    private int opponentHealth = 100;
    private ArrayList<Particle> particles;

    private class Question {
        String questionText;
        String[] answers;
        int correctAnswer;

        Question(String q, String[] a, int correct) {
            questionText = q;
            answers = a;
            correctAnswer = correct;
        }
    }

    private class Particle {
        int x, y;
        int vx, vy;
        Color color;
        int lifetime;

        Particle(int x, int y, int vx, int vy, Color c) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = c;
            this.lifetime = 30;
        }
    }

    public TriviaQuizBattleGame() {
        setTitle("Trivia Quiz Battle Game");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeComponents();
        initializeQuestions();
        setupGame();
    }

    private void initializeComponents() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(30, 30, 50));

        // Question Panel
        JPanel questionPanel = new JPanel(new GridLayout(2, 1));
        questionPanel.setOpaque(false);

        questionLabel = new JLabel("Welcome to Trivia Quiz Battle!");
        questionLabel.setForeground(Color.WHITE);
        questionLabel.setFont(new Font("Arial", Font.BOLD, 20));
        questionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel infoPanel = new JPanel(new FlowLayout());
        infoPanel.setOpaque(false);

        timerLabel = new JLabel("Time: 15");
        timerLabel.setForeground(Color.YELLOW);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.GREEN);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 16));

        roundLabel = new JLabel("Round: 1/5");
        roundLabel.setForeground(Color.CYAN);
        roundLabel.setFont(new Font("Arial", Font.BOLD, 16));

        infoPanel.add(timerLabel);
        infoPanel.add(scoreLabel);
        infoPanel.add(roundLabel);

        questionPanel.add(questionLabel);
        questionPanel.add(infoPanel);

        // Answer Buttons Panel
        JPanel answerPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        answerPanel.setOpaque(false);
        answerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        answerButtons = new JButton[4];
        for (int i = 0; i < 4; i++) {
            answerButtons[i] = new JButton();
            answerButtons[i].setFont(new Font("Arial", Font.PLAIN, 14));
            answerButtons[i].setBackground(new Color(50, 50, 80));
            answerButtons[i].setForeground(Color.WHITE);
            answerButtons[i].setFocusPainted(false);
            final int index = i;
            answerButtons[i].addActionListener(e -> checkAnswer(index));
            answerPanel.add(answerButtons[i]);
        }

        // Animation Panel
        animationPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawAnimation(g);
            }
        };
        animationPanel.setPreferredSize(new Dimension(WINDOW_WIDTH, 200));
        animationPanel.setBackground(new Color(20, 20, 40));

        mainPanel.add(questionPanel, BorderLayout.NORTH);
        mainPanel.add(answerPanel, BorderLayout.CENTER);
        mainPanel.add(animationPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void initializeQuestions() {
        questions = new ArrayList<>();
        particles = new ArrayList<>();

        // Sample questions (add more for a complete game)
        questions.add(new Question(
                "What is the capital of France?",
                new String[]{"Paris", "London", "Berlin", "Madrid"},
                0
        ));
        questions.add(new Question(
                "Which planet is known as the Red Planet?",
                new String[]{"Venus", "Mars", "Jupiter", "Saturn"},
                1
        ));
        questions.add(new Question(
                "Who painted the Mona Lisa?",
                new String[]{"Van Gogh", "Picasso", "Da Vinci", "Monet"},
                2
        ));
        // Add more questions to reach desired count
        for (int i = 0; i < 47; i++) {
            questions.add(new Question(
                    "Sample Question " + (i + 4) + "?",
                    new String[]{"Option 1", "Option 2", "Option 3", "Option 4"},
                    0
            ));
        }

        Collections.shuffle(questions);
    }

    private void setupGame() {
        playerScore = 0;
        currentRound = 1;
        currentQuestionIndex = 0;
        isGameRunning = true;
        playerHealth = 100;
        opponentHealth = 100;

        questionTimer = new Timer(1000, e -> updateTimer());
        animationTimer = new Timer(16, e -> updateAnimation());
        animationTimer.start();

        showNextQuestion();
    }

    private void showNextQuestion() {
        if (currentQuestionIndex >= questions.size() || currentRound > MAX_ROUNDS) {
            endGame();
            return;
        }

        Question currentQuestion = questions.get(currentQuestionIndex);
        questionLabel.setText("<html><center>" + currentQuestion.questionText + "</center></html>");

        for (int i = 0; i < 4; i++) {
            answerButtons[i].setText(currentQuestion.answers[i]);
            answerButtons[i].setEnabled(true);
            answerButtons[i].setBackground(new Color(50, 50, 80));
        }

        roundLabel.setText("Round: " + currentRound + "/" + MAX_ROUNDS);
        scoreLabel.setText("Score: " + playerScore);
        timerLabel.setText("Time: 15");

        questionTimer.stop();
        questionTimer = new Timer(1000, e -> updateTimer());
        questionTimer.start();
    }

    private void updateTimer() {
        String currentText = timerLabel.getText();
        int seconds = Integer.parseInt(currentText.split(": ")[1]);

        if (seconds <= 1) {
            questionTimer.stop();
            handleTimeout();
        } else {
            timerLabel.setText("Time: " + (seconds - 1));
        }
    }

    private void handleTimeout() {
        opponentHealth -= 10;
        createExplosion(opponentX, 100);
        currentQuestionIndex++;
        if (currentQuestionIndex % 10 == 0) {
            currentRound++;
        }
        showNextQuestion();
    }

    private void checkAnswer(int selectedIndex) {
        questionTimer.stop();

        Question currentQuestion = questions.get(currentQuestionIndex);
        boolean isCorrect = selectedIndex == currentQuestion.correctAnswer;

        if (isCorrect) {
            playerScore += 100;
            opponentHealth -= 20;
            createExplosion(opponentX, 100);
            answerButtons[selectedIndex].setBackground(Color.GREEN);
        } else {
            playerHealth -= 20;
            createExplosion(playerX, 100);
            answerButtons[selectedIndex].setBackground(Color.RED);
            answerButtons[currentQuestion.correctAnswer].setBackground(Color.GREEN);
        }

        for (JButton button : answerButtons) {
            button.setEnabled(false);
        }

        Timer delay = new Timer(1000, e -> {
            currentQuestionIndex++;
            if (currentQuestionIndex % 10 == 0) {
                currentRound++;
            }
            showNextQuestion();
        });
        delay.setRepeats(false);
        delay.start();
    }

    private void createExplosion(int x, int y) {
        Random rand = new Random();
        for (int i = 0; i < 20; i++) {
            int vx = rand.nextInt(10) - 5;
            int vy = rand.nextInt(10) - 5;
            particles.add(new Particle(x, y, vx, vy, new Color(255, rand.nextInt(100), 0)));
        }
    }

    private void updateAnimation() {
        // Update particles
        ArrayList<Particle> particlesToRemove = new ArrayList<>();
        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;
            p.lifetime--;
            if (p.lifetime <= 0) {
                particlesToRemove.add(p);
            }
        }
        particles.removeAll(particlesToRemove);

        // Simple character movement
        if (playerX < 100) playerX += 2;
        if (opponentX > 600) opponentX -= 2;

        animationPanel.repaint();
    }

    private void drawAnimation(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        g2d.setColor(new Color(20, 20, 40));
        g2d.fillRect(0, 0, WINDOW_WIDTH, 200);

        // Draw health bars
        g2d.setColor(Color.RED);
        g2d.fillRect(20, 20, 200, 20);
        g2d.fillRect(580, 20, 200, 20);

        g2d.setColor(Color.GREEN);
        g2d.fillRect(20, 20, playerHealth * 2, 20);
        g2d.fillRect(580, 20, opponentHealth * 2, 20);

        // Draw characters (simple rectangles for demo)
        g2d.setColor(Color.BLUE);
        g2d.fillRect(playerX, 100, 50, 80);
        g2d.setColor(Color.RED);
        g2d.fillRect(opponentX, 100, 50, 80);

        // Draw particles
        for (Particle p : particles) {
            g2d.setColor(p.color);
            g2d.fillOval(p.x, p.y, 5, 5);
        }

        // Draw health values
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Player: " + playerHealth, 20, 15);
        g2d.drawString("Opponent: " + opponentHealth, 580, 15);
    }

    private void endGame() {
        isGameRunning = false;
        questionTimer.stop();
        animationTimer.stop();

        String message = playerScore >= 300 ?
                "Congratulations! You won!\nFinal Score: " + playerScore :
                "Game Over!\nFinal Score: " + playerScore;

        JOptionPane.showMessageDialog(this, message, "Game Over",
                JOptionPane.INFORMATION_MESSAGE);

        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TriviaQuizBattleGame().setVisible(true);
        });
    }
}