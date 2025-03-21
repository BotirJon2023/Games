import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class HauntedObjectHorrorSimulation {

    private static final int GRID_SIZE = 10;
    private static final int NUM_OBJECTS = 5;
    private static final int NUM_GHOSTS = 3;
    private static final int MAX_MOVES = 50;

    private char[][] grid;
    private List<GameObject> objects;
    private List<Ghost> ghosts;
    private Player player;
    private Random random;
    private int moves;
    private boolean gameOver;

    public HauntedObjectHorrorSimulation() {
        grid = new char[GRID_SIZE][GRID_SIZE];
        objects = new ArrayList<>();
        ghosts = new ArrayList<>();
        random = new Random();
        moves = 0;
        gameOver = false;
        initializeGame();
    }

    private void initializeGame() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                grid[i][j] = '.';
            }
        }

        player = new Player(random.nextInt(GRID_SIZE), random.nextInt(GRID_SIZE));
        grid[player.getY()][player.getX()] = 'P';

        for (int i = 0; i < NUM_OBJECTS; i++) {
            int x, y;
            do {
                x = random.nextInt(GRID_SIZE);
                y = random.nextInt(GRID_SIZE);
            } while (grid[y][x] != '.');
            objects.add(new GameObject(x, y));
            grid[y][x] = 'O';
        }

        for (int i = 0; i < NUM_GHOSTS; i++) {
            int x, y;
            do {
                x = random.nextInt(GRID_SIZE);
                y = random.nextInt(GRID_SIZE);
            } while (grid[y][x] != '.');
            ghosts.add(new Ghost(x, y));
            grid[y][x] = 'G';
        }
    }

    private void displayGrid() {
        System.out.println("Moves: " + moves);
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                System.out.print(grid[i][j] + " ");
            }
            System.out.println();
        }
    }

    private void updateGrid() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (grid[i][j] != 'P' && grid[i][j] != '.') {
                    grid[i][j] = '.';
                }
            }
        }

        grid[player.getY()][player.getX()] = 'P';

        for (GameObject obj : objects) {
            grid[obj.getY()][obj.getX()] = 'O';
        }

        for (Ghost ghost : ghosts) {
            grid[ghost.getY()][ghost.getX()] = 'G';
        }
    }

    private void movePlayer(char direction) {
        int newX = player.getX();
        int newY = player.getY();

        switch (direction) {
            case 'w':
                newY--;
                break;
            case 's':
                newY++;
                break;
            case 'a':
                newX--;
                break;
            case 'd':
                newX++;
                break;
            default:
                System.out.println("Invalid direction.");
                return;
        }

        if (isValidMove(newX, newY)) {
            player.setX(newX);
            player.setY(newY);
            moves++;
        } else {
            System.out.println("You can't move there!");
        }
    }

    private void moveGhosts() {
        for (Ghost ghost : ghosts) {
            int dx = Integer.compare(player.getX(), ghost.getX());
            int dy = Integer.compare(player.getY(), ghost.getY());

            int newX = ghost.getX() + dx;
            int newY = ghost.getY() + dy;

            if (isValidMove(newX, newY)) {
                ghost.setX(newX);
                ghost.setY(newY);
            }
        }
    }

    private boolean isValidMove(int x, int y) {
        return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE;
    }

    private void checkCollisions() {
        for (Ghost ghost : ghosts) {
            if (ghost.getX() == player.getX() && ghost.getY() == player.getY()) {
                gameOver = true;
                System.out.println("You were caught by a ghost! Game over.");
                return;
            }
        }

        objects.removeIf(obj -> obj.getX() == player.getX() && obj.getY() == player.getY());

        if (objects.isEmpty()) {
            gameOver = true;
            System.out.println("You collected all objects! You win!");
        }
    }

    private void playGame() {
        Scanner scanner = new Scanner(System.in);

        while (!gameOver && moves < MAX_MOVES) {
            displayGrid();
            System.out.print("Enter direction (w/a/s/d): ");
            char direction = scanner.next().charAt(0);
            movePlayer(direction);
            moveGhosts();
            updateGrid();
            checkCollisions();
        }

        if (moves >= MAX_MOVES && !gameOver) {
            System.out.println("You ran out of moves!");
        }
        scanner.close();
    }

    public static void main(String[] args) {
        HauntedObjectHorrorSimulation game = new HauntedObjectHorrorSimulation();
        game.playGame();
    }

    static class GameObject {
        private int x;
        private int y;

        public GameObject(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    static class Ghost {
        private int x;
        private int y;

        public Ghost(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }
    }

    static class Player {
        private int x;
        private int y;

        public Player(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }
    }
}