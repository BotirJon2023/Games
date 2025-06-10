import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class CrossFitTrainingGame extends JFrame {
    private GamePanel gamePanel;
    private Timer gameTimer;
    private int score = 0;
    private int timeLeft = 300; // 5 minutes in seconds
    private boolean gameRunning = false;
    private ArrayList<Exercise> exercises;
    private Exercise currentExercise;
    private Player player;
    private Leaderboard leaderboard;
    private JLabel scoreLabel, timeLabel, exerciseLabel;
    private JButton startButton, pauseButton;
    private Clip exerciseSound;

    public CrossFitTrainingGame() {
        setTitle("CrossFit Training Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        scoreLabel = new JLabel("Score: 0");
        timeLabel = new JLabel("Time: 5:00");
        exerciseLabel = new JLabel("Exercise: None");
        startButton = new JButton("Start");
        pauseButton = new JButton("Pause");
        pauseButton.setEnabled(false);

        controlPanel.add(scoreLabel);
        controlPanel.add(timeLabel);
        controlPanel.add(exerciseLabel);
        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        add(controlPanel, BorderLayout.NORTH);

        exercises = new ArrayList<>();
        exercises.add(new Burpee());
        exercises.add(new KettlebellSwing());
        exercises.add(new BoxJump());

        player = new Player();
        leaderboard = new Leaderboard();

        startButton.addActionListener(e -> startGame());
        pauseButton.addActionListener(e -> pauseGame());

        gameTimer = new Timer(16, e -> {
            if (gameRunning) {
                gamePanel.update();
                gamePanel.repaint();
                updateTimer();
            }
        });

        loadSound();
        addKeyListener(new GameKeyListener());
        setFocusable(true);
    }

    private void startGame() {
        gameRunning = true;
        startButton.setEnabled(false);
        pauseButton.setEnabled(true);
        timeLeft = 300;
        score = 0;
        updateLabels();
        selectNewExercise();
        gameTimer.start();
        playSound();
    }

    private void pauseGame() {
        gameRunning = !gameRunning;
        pauseButton.setText(gameRunning ? "Pause" : "Resume");
        if (gameRunning) {
            gameTimer.start();
            playSound();
        } else {
            gameTimer.stop();
            stopSound();
        }
    }

    private void updateTimer() {
        timeLeft -= 16 / 1000.0;
        if (timeLeft <= 0) {
            endGame();
        }
        updateLabels();
    }

    private void endGame() {
        gameRunning = false;
        gameTimer.stop();
        stopSound();
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        leaderboard.addScore(score);
        JOptionPane.showMessageDialog(this, "Game Over! Score: " + score + "\nHigh Score: " + leaderboard.getHighScore());
    }

    private void selectNewExercise() {
        Random rand = new Random();
        currentExercise = exercises.get(rand.nextInt(exercises.size()));
        exerciseLabel.setText("Exercise: " + currentExercise.getName());
    }

    private void updateLabels() {
        scoreLabel.setText("Score: " + score);
        int minutes = (int) timeLeft / 60;
        int seconds = (int) timeLeft % 60;
        timeLabel.setText(String.format("Time: %d:%02d", minutes, seconds));
    }

    private void loadSound() {
        try {
            File soundFile = new File("exercise.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            exerciseSound = AudioSystem.getClip();
            exerciseSound.open(audioIn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playSound() {
        if (exerciseSound != null) {
            exerciseSound.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private void stopSound() {
        if (exerciseSound != null) {
            exerciseSound.stop();
        }
    }

    private class GamePanel extends JPanel {
        private int animationFrame = 0;

        public GamePanel() {
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw gym background
            drawGymBackground(g2d);

            // Draw player
            player.draw(g2d, animationFrame);

            // Draw exercise prompt
            if (currentExercise != null) {
                currentExercise.drawPrompt(g2d, getWidth(), getHeight());
            }

            // Draw leaderboard
            leaderboard.draw(g2d, getWidth() - 200, 20);
        }

        private void drawGymBackground(Graphics2D g2d) {
            g2d.setColor(new Color(200, 200, 200));
            g2d.fillRect(0, getHeight() - 50, getWidth(), 50); // Floor
            g2d.setColor(new Color(150, 150, 150));
            g2d.fillRect(0, 0, getWidth(), getHeight() - 50); // Wall
            g2d.setColor(Color.BLACK);
            g2d.drawLine(0, getHeight() - 50, getWidth(), getHeight() - 50); // Floor line
        }

        public void update() {
            animationFrame = (animationFrame + 1) % 60;
            player.update();
            if (currentExercise != null) {
                currentExercise.update();
            }
        }
    }

    private class GameKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (!gameRunning) return;

            if (currentExercise != null && currentExercise.checkInput(e.getKeyCode())) {
                score += 10;
                updateLabels();
                selectNewExercise();
            }
        }
    }

    private class Player {
        private int x = 400;
        private int y = 500;
        private int animationState = 0;

        public void update() {
            animationState = (animationState + 1) % 60;
        }

        public void draw(Graphics2D g2d, int frame) {
            g2d.setColor(Color.BLUE);
            if (currentExercise != null) {
                currentExercise.animatePlayer(g2d, x, y, frame);
            } else {
                // Default standing pose
                g2d.fillOval(x, y - 100, 50, 50); // Head
                g2d.fillRect(x + 10, y - 50, 30, 100); // Body
                g2d.fillRect(x - 20, y - 50, 30, 60); // Left arm
                g2d.fillRect(x + 40, y - 50, 30, 60); // Right arm
                g2d.fillRect(x + 10, y + 50, 20, 80); // Left leg
                g2d.fillRect(x + 30, y + 50, 20, 80); // Right leg
            }
        }
    }

    private abstract class Exercise {
        protected String name;
        protected int requiredKey;

        public String getName() {
            return name;
        }

        public abstract boolean checkInput(int keyCode);

        public abstract void drawPrompt(Graphics2D g2d, int width, int height);

        public abstract void animatePlayer(Graphics2D g2d, int x, int y, int frame);

        public void update() {
        }
    }

    private class Burpee extends Exercise {
        public Burpee() {
            name = "Burpee";
            requiredKey = KeyEvent.VK_SPACE;
        }

        @Override
        public boolean checkInput(int keyCode) {
            return keyCode == requiredKey;
        }

        @Override
        public void drawPrompt(Graphics2D g2d, int width, int height) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.drawString("Press SPACE to do a Burpee!", width / 2 - 150, 50);
        }

        @Override
        public void animatePlayer(Graphics2D g2d, int x, int y, int frame) {
            int phase = frame % 60;
            if (phase < 15) { // Squat
                g2d.fillOval(x, y - 80, 50, 50); // Head
                g2d.fillRect(x + 10, y - 30, 30, 80); // Body
                g2d.fillRect(x - 20, y - 30, 30, 40); // Left arm
                g2d.fillRect(x + 40, y - 30, 30, 40); // Right arm
                g2d.fillRect(x + 10, y + 50, 20, 60); // Left leg
                g2d.fillRect(x + 30, y + 50, 20, 60); // Right leg
            } else if (phase < 30) { // Plank
                g2d.fillOval(x, y - 20, 50, 50); // Head
                g2d.fillRect(x + 10, y - 10, 100, 30); // Body
                g2d.fillRect(x - 20, y - 10, 30, 60); // Left arm
                g2d.fillRect(x + 110, y - 10, 30, 60); // Right arm
                g2d.fillRect(x + 10, y + 20, 20, 60); // Left leg
                g2d.fillRect(x + 30, y + 20, 20, 60); // Right leg
            } else if (phase < 45) { // Jump
                g2d.fillOval(x, y - 120, 50, 50); // Head
                g2d.fillRect(x + 10, y - 70, 30, 100); // Body
                g2d.fillRect(x - 20, y - 100, 30, 60); // Left arm
                g2d.fillRect(x + 40, y - 100, 30, 60); // Right arm
                g2d.fillRect(x + 10, y + 30, 20, 60); // Left leg
                g2d.fillRect(x + 30, y + 30, 20, 60); // Right leg
            } else { // Stand
                g2d.fillOval(x, y - 100, 50, 50); // Head
                g2d.fillRect(x + 10, y - 50, 30, 100); // Body
                g2d.fillRect(x - 20, y - 50, 30, 60); // Left arm
                g2d.fillRect(x + 40, y - 50, 30, 60); // Right arm
                g2d.fillRect(x + 10, y + 50, 20, 80); // Left leg
                g2d.fillRect(x + 30, y + 50, 20, 80); // Right leg
            }
        }
    }

    private class KettlebellSwing extends Exercise {
        public KettlebellSwing() {
            name = "Kettlebell Swing";
            requiredKey = KeyEvent.VK_K;
        }

        @Override
        public boolean checkInput(int keyCode) {
            return keyCode == requiredKey;
        }

        @Override
        public void drawPrompt(Graphics2D g2d, int width, int height) {
            g2d.setColor(Color.GREEN);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.drawString("Press K to do a Kettlebell Swing!", width / 2 - 200, 50);
        }

        @Override
        public void animatePlayer(Graphics2D g2d, int x, int y, int frame) {
            int phase = frame % 60;
            if (phase < 30) { // Swing up
                g2d.fillOval(x, y - 100, 50, 50); // Head
                g2d.fillRect(x + 10, y - 50, 30, 100); // Body
                g2d.fillRect(x - 20, y - 50, 30, 80); // Left arm
                g2d.fillRect(x + 40, y - 50, 30, 80); // Right arm
                g2d.fillRect(x + 10, y + 50, 20, 80); // Left leg
                g2d.fillRect(x + 30, y + 50, 20, 80); // Right leg
                g2d.setColor(Color.BLACK);
                g2d.fillOval(x + 10, y - 50, 30, 30); // Kettlebell
            } else { // Swing down
                g2d.fillOval(x, y - 80, 50, 50); // Head
                g2d.fillRect(x + 10, y - 30, 30, 80); // Body
                g2d.fillRect(x - 20, y - 30, 30, 100); // Left arm
                g2d.fillRect(x + 40, y - 30, 30, 100); // Right arm
                g2d.fillRect(x + 10, y + 50, 20, 60); // Left leg
                g2d.fillRect(x + 30, y + 50, 20, 60); // Right leg
                g2d.setColor(Color.BLACK);
                g2d.fillOval(x + 10, y + 50, 30, 30); // Kettlebell
            }
        }
    }

    private class BoxJump extends Exercise {
        public BoxJump() {
            name = "Box Jump";
            requiredKey = KeyEvent.VK_J;
        }

        @Override
        public boolean checkInput(int keyCode) {
            return keyCode == requiredKey;
        }

        @Override
        public void drawPrompt(Graphics2D g2d, int width, int height) {
            g2d.setColor(Color.BLUE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.drawString("Press J to do a Box Jump!", width / 2 - 180, 50);
        }

        @Override
        public void animatePlayer(Graphics2D g2d, int x, int y, int frame) {
            int phase = frame % 60;
            g2d.setColor(Color.GRAY);
            g2d.fillRect(x + 100, y, 80, 40); // Box
            if (phase < 15) { // Prepare
                g2d.setColor(Color.BLUE);
                g2d.fillOval(x, y - 40, 50, 50); // Head
                g2d.fillRect(x + 10, y - 10, 30, 80); // Body
                g2d.fillRect(x - 20, y - 10, 30, 40); // Left arm
                g2d.fillRect(x + 40, y - 10, 30, 40); // Right arm
                g2d.fillRect(x + 10, y + 70, 20, 20); // Left leg
                g2d.fillRect(x + 30, y + 70, 20, 20); // Right leg
            } else if (phase < 30) { // Jump
                g2d.setColor(Color.BLUE);
                g2d.fillOval(x, y - 80, 50, 50); // Head
                g2d.fillRect(x + 10, y - 50, 30, 80); // Body
                g2d.fillRect(x - 20, y - 80, 30, 60); // Left arm
                g2d.fillRect(x + 40, y - 80, 30, 60); // Right arm
                g2d.fillRect(x + 10, y + 10, 20, 60); // Left leg
                g2d.fillRect(x + 30, y + 10, 20, 60); // Right leg
            } else if (phase < 45) { // Land on box
                g2d.setColor(Color.YELLOW);
                g2d.fillOval(x + 100, y - 40, 50, 50); // Head
                g2d.fillRect(x + 110, y - 10, 30, 50); // Body
                g2d.fillRect(x + 80, y - 30, 30, 30); // Left arm
                g2d.fillRect(x + 140, y - 30, 30, 30); // Right arm
                g2d.fillRect(x + 110, y + 20, 20, 20); // Left leg
                g2d.fillRect(x + 130, y + 20, 20, 20); // Right leg
            } else { // Stand
                g2d.setColor(Color.BLUE);
                g2d.fillOval(x, y - 100, 50, 50); // Head
                g2d.fillRect(x + 10, y - 50, 30, 100); // Body
                g2d.fillRect(x - 20, y - 50, 30, 60); // Left arm
                g2d.fillRect(x + 40, y - 50, 30, 60); // Right arm
                g2d.fillRect(x + 10, y + 50, 20, 80); // Left leg
                g2d.fillRect(x + 30, y + 50, 20, 80); // Right leg
            }
        }
    }

    private class Leaderboard {
        private ArrayList<Integer> scores;

        public Leaderboard() {
            scores = new ArrayList<>();
        }

        public void addScore(int score) {
            scores.add(score);
            scores.sort((a, b) -> b - a);
            if (scores.size() > 10) {
                scores.remove(scores.size() - 1);
            }
        }

        public int getHighScore() {
            return scores.isEmpty() ? 0 : scores.get(0);
        }

        public void draw(Graphics2D g2d, int x, int y) {
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            g2d.drawString("Leaderboard:", x, y);
            for (int i = 0; i < scores.size(); i++) {
                g2d.drawString((i + 1) + ". " + scores.get(i), x, y + (i + 1) * 20);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CrossFitTrainingGame game = new CrossFitTrainingGame();
            game.setVisible(true);
        });
    }
}