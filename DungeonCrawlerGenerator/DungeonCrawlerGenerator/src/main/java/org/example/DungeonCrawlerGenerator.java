package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonCrawlerGenerator {

    private static final int GRID_WIDTH = 80;
    private static final int GRID_HEIGHT = 40;
    private static final char WALL = '#';
    private static final char FLOOR = '.';
    private static final char DOOR = '+';
    private static final int MAX_ROOMS = 15;
    private static final int MIN_ROOM_SIZE = 4;
    private static final int MAX_ROOM_SIZE = 10;

    private char[][] grid;
    private List<Room> rooms;
    private Random random;

    public DungeonCrawlerGenerator() {
        grid = new char[GRID_HEIGHT][GRID_WIDTH];
        rooms = new ArrayList<>();
        random = new Random();
        initializeGrid();
    }

    private void initializeGrid() {
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                grid[y][x] = WALL;
            }
        }
    }

    public void generateDungeon() {
        placeRooms();
        connectRooms();
    }

    private void placeRooms() {
        for (int i = 0; i < MAX_ROOMS; i++) {
            int width = random.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1) + MIN_ROOM_SIZE;
            int height = random.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1) + MIN_ROOM_SIZE;
            int x = random.nextInt(GRID_WIDTH - width - 2) + 1;
            int y = random.nextInt(GRID_HEIGHT - height - 2) + 1;

            Room newRoom = new Room(x, y, width, height);
            if (!doesOverlap(newRoom)) {
                rooms.add(newRoom);
                carveRoom(newRoom);
            }
        }
    }

    private boolean doesOverlap(Room room) {
        for (Room existingRoom : rooms) {
            if (room.intersects(existingRoom)) {
                return true;
            }
        }
        return false;
    }

    private void carveRoom(Room room) {
        for (int y = room.y; y < room.y + room.height; y++) {
            for (int x = room.x; x < room.x + room.width; x++) {
                if (y > 0 && y < GRID_HEIGHT - 1 && x > 0 && x < GRID_WIDTH - 1) {
                    grid[y][x] = FLOOR;
                }
            }
        }
    }

    private void connectRooms() {
        if (rooms.size() < 2) {
            return;
        }

        for (int i = 0; i < rooms.size() - 1; i++) {
            connectTwoRooms(rooms.get(i), rooms.get(i + 1));
        }
    }

    private void connectTwoRooms(Room room1, Room room2) {
        int x1 = room1.getCenterX();
        int y1 = room1.getCenterY();
        int x2 = room2.getCenterX();
        int y2 = room2.getCenterY();

        if (random.nextBoolean()) {
            // Horizontal first, then vertical
            carveHorizontalTunnel(x1, x2, y1);
            carveVerticalTunnel(y1, y2, x2);
        } else {
            // Vertical first, then horizontal
            carveVerticalTunnel(y1, y2, x1);
            carveHorizontalTunnel(x1, x2, y2);
        }
    }

    private void carveHorizontalTunnel(int x1, int x2, int y) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            if (y > 0 && y < GRID_HEIGHT - 1 && x > 0 && x < GRID_WIDTH - 1) {
                grid[y][x] = FLOOR;
            }
        }
    }

    private void carveVerticalTunnel(int y1, int y2, int x) {
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            if (y > 0 && y < GRID_HEIGHT - 1 && x > 0 && x < GRID_WIDTH - 1) {
                grid[y][x] = FLOOR;
            }
        }
    }

    public void printDungeon() {
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                System.out.print(grid[y][x]);
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        DungeonCrawlerGenerator generator = new DungeonCrawlerGenerator();
        generator.generateDungeon();
        generator.printDungeon();
    }

    private static class Room {
        int x, y, width, height;

        public Room(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int getCenterX() {
            return x + width / 2;
        }

        public int getCenterY() {
            return y + height / 2;
        }

        public boolean intersects(Room other) {
            return x < other.x + other.width + 1 &&
                    x + width + 1 > other.x &&
                    y < other.y + other.height + 1 &&
                    y + height + 1 > other.y;
        }
    }
}