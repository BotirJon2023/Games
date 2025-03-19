import java.util.Scanner;
import java.util.Random;

public class PostApocalypticHorror {

    private static final int MAP_SIZE = 20;
    private static char[][] map = new char[MAP_SIZE][MAP_SIZE];
    private static int playerX, playerY;
    private static int enemyX, enemyY;
    private static int scrapMetal = 0;
    private static int health = 100;
    private static boolean hasKey = false;
    private static boolean isGameOver = false;
    private static Random random = new Random();

    public static void main(String[] args) {
        initializeGame();
        gameLoop();
    }

    private static void initializeGame() {
        // Initialize the map with empty spaces
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                map[i][j] = '.';
            }
        }

        // Place the player in the center
        playerX = MAP_SIZE / 2;
        playerY = MAP_SIZE / 2;
        map[playerY][playerX] = 'P';

        // Place the enemy randomly
        enemyX = random.nextInt(MAP_SIZE);
        enemyY = random.nextInt(MAP_SIZE);
        map[enemyY][enemyX] = 'E';

        // Place the key randomly
        int keyX = random.nextInt(MAP_SIZE);
        int keyY = random.nextInt(MAP_SIZE);
        map[keyY][keyX] = 'K';

        // Place scrap metal randomly
        for (int i = 0; i < 10; i++) {
            int scrapX = random.nextInt(MAP_SIZE);
            int scrapY = random.nextInt(MAP_SIZE);
            map[scrapY][scrapX] = 'S';
        }

        // Place obstacles randomly
        for (int i = 0; i < 20; i++) {
            int obstacleX = random.nextInt(MAP_SIZE);
            int obstacleY = random.nextInt(MAP_SIZE);
            map[obstacleY][obstacleX] = '#';
        }
    }

    private static void gameLoop() {
        Scanner scanner = new Scanner(System.in);

        while (!isGameOver) {
            printMap();
            System.out.println("Health: " + health + ", Scrap Metal: " + scrapMetal + ", Key: " + (hasKey ? "Yes" : "No"));
            System.out.print("Enter move (WASD): ");
            String move = scanner.nextLine().toUpperCase();

            handleMove(move);
            enemyMove();
            checkCollisions();
            checkGameOver();
        }

        scanner.close();
    }

    private static void printMap() {
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                System.out.print(map[i][j]);
            }
            System.out.println();
        }
    }

    private static void handleMove(String move) {
        int newX = playerX;
        int newY = playerY;

        switch (move) {
            case "W":
                newY--;
                break;
            case "A":
                newX--;
                break;
            case "S":
                newY++;
                break;
            case "D":
                newX++;
                break;
            default:
                System.out.println("Invalid move!");
                return;
        }

        if (isValidMove(newX, newY)) {
            map[playerY][playerX] = '.';
            playerX = newX;
            playerY = newY;
            map[playerY][playerX] = 'P';
        }
    }

    private static void enemyMove() {
        int dx = Integer.compare(playerX, enemyX);
        int dy = Integer.compare(playerY, enemyY);

        int newX = enemyX + dx;
        int newY = enemyY + dy;

        if (isValidMove(newX, newY) && map[newY][newX] != 'P') {
            map[enemyY][enemyX] = '.';
            enemyX = newX;
            enemyY = newY;
            map[enemyY][enemyX] = 'E';
        }
    }

    private static boolean isValidMove(int x, int y) {
        return x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE && map[y][x] != '#';
    }

    private static void checkCollisions() {
        if (playerX == enemyX && playerY == enemyY) {
            health -= 20;
            System.out.println("You were attacked by the enemy!");
        }

        if (map[playerY][playerX] == 'S') {
            scrapMetal += 10;
            map[playerY][playerX] = 'P';
            System.out.println("You found scrap metal!");
        }

        if (map[playerY][playerX] == 'K') {
            hasKey = true;
            map[playerY][playerX] = 'P';
            System.out.println("You found the key!");
        }
    }

    private static void checkGameOver() {
        if (health <= 0) {
            System.out.println("You died!");
            isGameOver = true;
        }

        if (playerX == 0 && playerY == 0 && hasKey) {
            System.out.println("You escaped! You win!");
            isGameOver = true;
        }
    }
}