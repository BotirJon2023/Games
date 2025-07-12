import java.util.*;
import java.util.concurrent.*;

public class BasketballSlamDunkChallenge {
    private static final int WIDTH = 80;
    private static final int HEIGHT = 24;
    private static final int COURT_WIDTH = 60;
    private static final int COURT_HEIGHT = 20;
    private static final int COURT_LEFT = (WIDTH - COURT_WIDTH) / 2;
    private static final int COURT_TOP = 2;

    private static final int PLAYER_HEIGHT = 4;
    private static final int BASKET_HEIGHT = 6;
    private static final int BASKET_POSITION = 50;

    private static final double GRAVITY = 0.2;
    private static final double DUNK_STRENGTH = 3.5;

    private static int score = 0;
    private static int attempts = 0;
    private static boolean gameRunning = true;
    private static boolean ballInAir = false;

    private static double playerX = 10;
    private static double playerY = COURT_TOP + COURT_HEIGHT - PLAYER_HEIGHT;
    private static double ballX = playerX + 2;
    private static double ballY = playerY - 1;
    private static double ballVelX = 0;
    private static double ballVelY = 0;

    private static Random random = new Random();
    private static Scanner scanner = new Scanner(System.in);

    private static char[][] screen = new char[HEIGHT][WIDTH];

    public static void main(String[] args) {
        initScreen();
        printInstructions();

        while (gameRunning) {
            clearScreen();
            updateGame();
            renderGame();
            printStats();

            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!ballInAir) {
                handleInput();
            }
        }

        scanner.close();
        System.out.println("Game Over! Final Score: " + score);
    }

    private static void initScreen() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                screen[y][x] = ' ';
            }
        }
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void printInstructions() {
        System.out.println("BASKETBALL SLAM DUNK CHALLENGE");
        System.out.println("Controls:");
        System.out.println("  A - Move left");
        System.out.println("  D - Move right");
        System.out.println("  W - Jump");
        System.out.println("  S - Dunk (while near basket and jumping)");
        System.out.println("  Q - Quit game");
        System.out.println("\nPress Enter to start...");
        scanner.nextLine();
    }

    private static void updateGame() {
        // Update player position (basic bounds checking)
        if (playerX < COURT_LEFT) playerX = COURT_LEFT;
        if (playerX > COURT_LEFT + COURT_WIDTH - 4) playerX = COURT_LEFT + COURT_WIDTH - 4;

        // Update ball physics if in air
        if (ballInAir) {
            ballX += ballVelX;
            ballY += ballVelY;
            ballVelY += GRAVITY;

            // Check for basket collision
            if (ballX >= BASKET_POSITION - 2 && ballX <= BASKET_POSITION + 2) {
                if (ballY >= COURT_TOP + COURT_HEIGHT - BASKET_HEIGHT - 1 &&
                        ballY <= COURT_TOP + COURT_HEIGHT - BASKET_HEIGHT + 1) {
                    // Ball is near basket rim
                    if (ballVelY > 0 && random.nextDouble() < 0.7) { // 70% chance to score when descending
                        score += 2;
                        ballInAir = false;
                        ballX = playerX + 2;
                        ballY = playerY - 1;
                    }
                }
            }

            // Check for floor collision
            if (ballY >= COURT_TOP + COURT_HEIGHT - 1) {
                ballInAir = false;
                ballY = COURT_TOP + COURT_HEIGHT - 1;
                ballX = playerX + 2;
                ballY = playerY - 1;
            }

            // Check for out of bounds
            if (ballX < COURT_LEFT || ballX > COURT_LEFT + COURT_WIDTH ||
                    ballY < COURT_TOP) {
                ballInAir = false;
                ballX = playerX + 2;
                ballY = playerY - 1;
            }
        }
    }

    private static void renderGame() {
        initScreen();

        // Draw court boundaries
        drawCourt();

        // Draw basket
        drawBasket();

        // Draw player
        drawPlayer();

        // Draw ball
        if (ballInAir || !ballInAir) {
            int bx = (int) Math.round(ballX);
            int by = (int) Math.round(ballY);
            if (bx >= 0 && bx < WIDTH && by >= 0 && by < HEIGHT) {
                screen[by][bx] = 'O';
            }
        }

        // Draw crowd
        drawCrowd();

        // Draw scoreboard
        drawScoreboard();

        // Render the screen
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                System.out.print(screen[y][x]);
            }
            System.out.println();
        }
    }

    private static void drawCourt() {
        // Court outline
        for (int x = COURT_LEFT; x < COURT_LEFT + COURT_WIDTH; x++) {
            screen[COURT_TOP + COURT_HEIGHT][x] = '_';
            screen[COURT_TOP][x] = '_';
        }

        for (int y = COURT_TOP; y < COURT_TOP + COURT_HEIGHT; y++) {
            screen[y][COURT_LEFT] = '|';
            screen[y][COURT_LEFT + COURT_WIDTH] = '|';
        }

        // Center line
        for (int y = COURT_TOP + 1; y < COURT_TOP + COURT_HEIGHT; y++) {
            screen[y][COURT_LEFT + COURT_WIDTH / 2] = '.';
        }

        // Free throw line
        for (int x = COURT_LEFT + COURT_WIDTH / 2; x < COURT_LEFT + COURT_WIDTH; x++) {
            screen[COURT_TOP + COURT_HEIGHT / 2][x] = '.';
        }

        // Three-point line (simplified)
        for (int x = COURT_LEFT + 10; x < COURT_LEFT + COURT_WIDTH - 10; x++) {
            if (x < COURT_LEFT + COURT_WIDTH / 2 - 5 || x > COURT_LEFT + COURT_WIDTH / 2 + 5) {
                screen[COURT_TOP + 3][x] = '.';
                screen[COURT_TOP + COURT_HEIGHT - 3][x] = '.';
            }
        }
    }

    private static void drawBasket() {
        // Backboard
        for (int y = COURT_TOP + COURT_HEIGHT - BASKET_HEIGHT - 2;
             y < COURT_TOP + COURT_HEIGHT - BASKET_HEIGHT + 2; y++) {
            screen[y][BASKET_POSITION] = '|';
        }

        // Rim
        for (int x = BASKET_POSITION - 2; x <= BASKET_POSITION + 2; x++) {
            screen[COURT_TOP + COURT_HEIGHT - BASKET_HEIGHT][x] = '=';
        }

        // Net (simplified)
        for (int y = COURT_TOP + COURT_HEIGHT - BASKET_HEIGHT + 1;
             y < COURT_TOP + COURT_HEIGHT - BASKET_HEIGHT + 4; y++) {
            screen[y][BASKET_POSITION] = '|';
        }
    }

    private static void drawPlayer() {
        int py = (int) playerY;
        int px = (int) playerX;

        // Only draw if within bounds
        if (py >= 0 && py < HEIGHT && px >= 0 && px < WIDTH) {
            // Head
            screen[py][px + 1] = 'O';

            // Body
            screen[py + 1][px] = '/';
            screen[py + 1][px + 1] = '|';
            screen[py + 1][px + 2] = '\\';

            // Legs
            screen[py + 2][px] = '/';
            screen[py + 2][px + 2] = '\\';

            // Arms when dunking
            if (ballInAir && ballVelY < -1) {
                screen[py][px] = '-';
                screen[py][px + 2] = '-';
            } else {
                screen[py][px] = '/';
                screen[py][px + 2] = '\\';
            }
        }
    }

    private static void drawCrowd() {
        // Simple crowd pattern
        for (int x = 5; x < COURT_LEFT - 5; x += 3) {
            screen[HEIGHT - 2][x] = '^';
            screen[HEIGHT - 3][x] = 'O';
        }

        for (int x = COURT_LEFT + COURT_WIDTH + 5; x < WIDTH - 5; x += 3) {
            screen[HEIGHT - 2][x] = '^';
            screen[HEIGHT - 3][x] = 'O';
        }
    }

    private static void drawScoreboard() {
        String scoreText = "SCORE: " + score;
        String attemptsText = "ATTEMPTS: " + attempts;

        for (int i = 0; i < scoreText.length(); i++) {
            screen[1][COURT_LEFT + i] = scoreText.charAt(i);
        }

        for (int i = 0; i < attemptsText.length(); i++) {
            screen[2][COURT_LEFT + i] = attemptsText.charAt(i);
        }
    }

    private static void printStats() {
        System.out.println("Current Position: " + (int)playerX + ", " + (int)playerY);
        if (ballInAir) {
            System.out.println("Ball Velocity: " + String.format("%.1f", ballVelX) + ", " +
                    String.format("%.1f", ballVelY));
        }
    }

    private static void handleInput() {
        if (System.in.available() > 0) {
            char input = scanner.next().toLowerCase().charAt(0);

            switch (input) {
                case 'a':
                    playerX -= 2;
                    break;
                case 'd':
                    playerX += 2;
                    break;
                case 'w':
                    // Jump - only dunk when near basket
                    if (playerX > BASKET_POSITION - 10 && playerX < BASKET_POSITION + 5) {
                        ballInAir = true;
                        attempts++;
                        ballVelX = (BASKET_POSITION - playerX) * 0.1;
                        ballVelY = -DUNK_STRENGTH;
                        ballX = playerX + 2;
                        ballY = playerY - 3;
                    } else {
                        // Regular jump without ball
                        playerY -= 2;
                    }
                    break;
                case 's':
                    // Special dunk move
                    if (playerX > BASKET_POSITION - 10 && playerX < BASKET_POSITION + 5) {
                        ballInAir = true;
                        attempts++;
                        ballVelX = (BASKET_POSITION - playerX) * 0.15;
                        ballVelY = -DUNK_STRENGTH * 1.2;
                        ballX = playerX + 2;
                        ballY = playerY - 4;
                    }
                    break;
                case 'q':
                    gameRunning = false;
                    break;
            }
        }
    }

    // Helper method to check for input without blocking
    private static boolean System.in.available() {
        try {
            return System.in.available() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}