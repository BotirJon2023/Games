// CrossfitTrainingGame.java
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class CrossfitTrainingGame3 {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameFrame::new);
    }
}

class GameFrame extends JFrame {
    GameFrame() {
        setTitle("Crossfit Training Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        add(new GamePanel());
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private final Timer timer;
    private final Trainer trainer;
    private final ExerciseManager exerciseManager;
    private final ScoreBoard scoreBoard;
    private final TimerBar timerBar;
    private final int WIDTH = 800;
    private final int HEIGHT = 600;
    private boolean isGameOver = false;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.WHITE);
        setFocusable(true);
        addKeyListener(this);

        trainer = new Trainer(350, 300);
        exerciseManager = new ExerciseManager();
        scoreBoard = new ScoreBoard();
        timerBar = new TimerBar(60000); // 60 seconds game

        timer = new Timer(20, this); // ~50 FPS
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isGameOver) {
            trainer.update();
            timerBar.update();
            if (timerBar.isTimeUp()) {
                isGameOver = true;
            }
        }
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        trainer.draw(g);
        exerciseManager.draw(g);
        scoreBoard.draw(g, 20, 30);
        timerBar.draw(g, 20, 60);

        if (isGameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.setColor(Color.RED);
            g.drawString("Game Over!", 280, 300);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.drawString("Final Score: " + scoreBoard.getScore(), 320, 350);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!isGameOver && e.getKeyCode() == KeyEvent.VK_SPACE) {
            trainer.performRep();
            scoreBoard.incrementScore();
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}

class Trainer {
    private int x, y;
    private boolean jumping = false;
    private int jumpHeight = 0;
    private int jumpDirection = 1;

    public Trainer(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void update() {
        if (jumping) {
            jumpHeight += 5 * jumpDirection;
            if (jumpHeight >= 30) {
                jumpDirection = -1;
            } else if (jumpHeight <= 0) {
                jumping = false;
                jumpDirection = 1;
            }
        }
    }

    public void performRep() {
        if (!jumping) {
            jumping = true;
        }
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillRect(x, y - jumpHeight, 40, 80);
        g.setColor(Color.BLACK);
        g.drawString("Trainer", x, y - jumpHeight - 10);
    }
}

class ExerciseManager {
    private final List<Exercise> exercises = new ArrayList<>();

    public ExerciseManager() {
        exercises.add(new Exercise("Jumping Jacks", "Jump with arms up"));
        exercises.add(new Exercise("Squats", "Bend knees and lower"));
        exercises.add(new Exercise("Push-ups", "Lower chest to ground"));
    }

    public void draw(Graphics g) {
        int y = 480;
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Exercises:", 20, y);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        for (Exercise e : exercises) {
            y += 20;
            g.drawString("- " + e.getName() + ": " + e.getDescription(), 30, y);
        }
    }
}

class Exercise {
    private final String name;
    private final String description;

    public Exercise(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }

    public String getDescription() { return description; }
}

class ScoreBoard {
    private int score = 0;

    public void incrementScore() {
        score++;
    }

    public int getScore() {
        return score;
    }

    public void draw(Graphics g, int x, int y) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, x, y);
    }
}

class TimerBar {
    private final long totalTime;
    private final long startTime;

    public TimerBar(long totalTime) {
        this.totalTime = totalTime;
        this.startTime = System.currentTimeMillis();
    }

    public void update() {}

    public boolean isTimeUp() {
        return System.currentTimeMillis() - startTime >= totalTime;
    }

    public void draw(Graphics g, int x, int y) {
        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = Math.max(totalTime - elapsed, 0);
        int percent = (int) ((remaining * 100) / totalTime);

        g.setColor(Color.GRAY);
        g.fillRect(x, y, 200, 20);
        g.setColor(Color.GREEN);
        g.fillRect(x, y, 2 * percent, 20);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, 200, 20);
        g.drawString("Time Left: " + (remaining / 1000) + "s", x + 210, y + 15);
    }
}
