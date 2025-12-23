import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

// RugbyGameSimulation.java

public class RugbyGameSimulation extends JPanel implements ActionListener {

    // Constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    static final int PITCH_WIDTH = 700;
    static final int PITCH_HEIGHT = 400;
    static final int TEAM_SIZE = 15;
    private static final int BALL_SIZE = 10;

    // Teams
    private Team teamA;
    private Team teamB;

    // Game state
    private boolean isGameRunning;
    private int scoreA;
    private int scoreB;
    private int currentPlayer;
    private int currentTeam;
    private int ballX;
    private int ballY;
    private int ballOwner;
    private Timer timer;

    public RugbyGameSimulation() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.GREEN);
        setFocusable(true);

        teamA = new Team(Color.BLUE, 50, HEIGHT / 2);
        teamB = new Team(Color.RED, WIDTH - 50, HEIGHT / 2);

        isGameRunning = false;
        scoreA = 0;
        scoreB = 0;
        currentPlayer = 0;
        currentTeam = 0;
        ballX = WIDTH / 2;
        ballY = HEIGHT / 2;
        ballOwner = -1;

        timer = new Timer(1000 / 60, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw pitch
        g.setColor(Color.WHITE);
        g.fillRect((WIDTH - PITCH_WIDTH) / 2, (HEIGHT - PITCH_HEIGHT) / 2, PITCH_WIDTH, PITCH_HEIGHT);

        // Draw teams
        teamA.draw(g);
        teamB.draw(g);

        // Draw ball
        g.setColor(Color.BLACK);
        g.fillOval(ballX - BALL_SIZE / 2, ballY - BALL_SIZE / 2, BALL_SIZE, BALL_SIZE);

        // Draw score
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Score: " + scoreA + " - " + scoreB, 10, 30);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isGameRunning) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        // Update team players
        teamA.update();
        teamB.update();

        // Update ball
        if (ballOwner == -1) {
            // Ball is loose, move it randomly
            ballX += new Random().nextInt(3) - 1;
            ballY += new Random().nextInt(3) - 1;
        } else if (ballOwner == 0) {
            // Ball is with team A
            ballX = teamA.getCurrentPlayer().x;
            ballY = teamA.getCurrentPlayer().y;
        } else {
            // Ball is with team B
            ballX = teamB.getCurrentPlayer().x;
            ballY = teamB.getCurrentPlayer().y;
        }

        // Check scoring
        if (ballX < (WIDTH - PITCH_WIDTH) / 2) {
            scoreB += 5;
            ballX = WIDTH / 2;
            ballY = HEIGHT / 2;
            ballOwner = -1;
        } else if (ballX > (WIDTH + PITCH_WIDTH) / 2) {
            scoreA += 5;
            ballX = WIDTH / 2;
            ballY = HEIGHT / 2;
            ballOwner = -1;
        }
    }

    public void startGame() {
        isGameRunning = true;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Rugby Game Simulation");
        RugbyGameSimulation game = new RugbyGameSimulation();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        game.startGame();
    }
}

class Team {
    private Color color;
    private int x;
    private int y;
    private Player[] players;
    private int currentPlayer;

    public Team(Color color, int x, int y) {
        this.color = color;
        this.x = x;
        this.y = y;
        players = new Player[RugbyGameSimulation.TEAM_SIZE];
        for (int i = 0; i < RugbyGameSimulation.TEAM_SIZE; i++) {
            players[i] = new Player(x, y + (i - RugbyGameSimulation.TEAM_SIZE / 2) * 20);
        }
        currentPlayer = 0;
    }

    public void draw(Graphics g) {
        g.setColor(color);
        for (Player player : players) {
            player.draw(g);
        }
    }

    public void update() {
        for (Player player : players) {
            player.update();
        }
    }

    public Player getCurrentPlayer() {
        return players[currentPlayer];
    }
}

class Player {
    public int x;
    public int y;
    private int speedX;
    private int speedY;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        speedX = new Random().nextInt(3) - 1;
        speedY = new Random().nextInt(3) - 1;
    }

    public void draw(Graphics g) {
        g.fillOval(x - 10, y - 10, 20, 20);
    }

    public void update() {
        x += speedX;
        y += speedY;

        if (x < (RugbyGameSimulation.getWidth() - RugbyGameSimulation.PITCH_WIDTH) / 2 || x > (RugbyGameSimulation.getWidth() + RugbyGameSimulation.PITCH_WIDTH) / 2) {
            speedX = -speedX;
        }
        if (y < (RugbyGameSimulation.getHeight() - RugbyGameSimulation.PITCH_HEIGHT) / 2 || y > (RugbyGameSimulation.HEIGHT + RugbyGameSimulation.PITCH_HEIGHT) / 2) {
            speedY = -speedY;
        }
    }
}