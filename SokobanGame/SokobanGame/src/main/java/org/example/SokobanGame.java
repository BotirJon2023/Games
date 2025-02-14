package org.example;

import java.util.Scanner;

public class SokobanGame {


    static final char WALL = '#';
    static final char EMPTY = ' ';
    static final char PLAYER = '@';
    static final char BOX = '$';
    static final char TARGET = '.';
    static final char BOX_ON_TARGET = '*';
    static final char PLAYER_ON_TARGET = '+';

    static char[][] map;
    static int playerRow, playerCol;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String[] level = {
                "######",
                "#@.$.#",
                "#..#.#",
                "#..#.#",
                "#.###.",
                "######"
        };

        map = new char[level.length][level[0].length()];
        for (int i = 0; i < level.length; i++) {
            map[i] = level[i].toCharArray();
        }

        findPlayerPosition();

        while (true) {
            printMap();
            System.out.print("Enter move (WASD): ");
            String move = scanner.nextLine();
            if (move.length() != 1) continue;

            char direction = move.charAt(0);
            if (!movePlayer(direction)) {
                System.out.println("Invalid move!");
            }

            if (checkVictory()) {
                printMap();
                System.out.println("Congratulations, you won!");
                break;
            }
        }

        scanner.close();
    }

    // Print the map
    public static void printMap() {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                System.out.print(map[i][j]);
            }
            System.out.println();
        }
    }

    // Find the player's position in the map
    public static void findPlayerPosition() {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == PLAYER || map[i][j] == PLAYER_ON_TARGET) {
                    playerRow = i;
                    playerCol = j;
                    return;
                }
            }
        }
    }

    // Move the player
    public static boolean movePlayer(char direction) {
        int newRow = playerRow;
        int newCol = playerCol;

        switch (direction) {
            case 'w':
                newRow--;
                break;  // Move up
            case 's':
                newRow++;
                break;  // Move down
            case 'a':
                newCol--;
                break;  // Move left
            case 'd':
                newCol++;
                break;  // Move right
            default:
                return false;
        }

        if (isValidMove(newRow, newCol)) {
            char nextTile = map[newRow][newCol];

            if (nextTile == EMPTY || nextTile == TARGET) {
                map[playerRow][playerCol] = (map[playerRow][playerCol] == PLAYER_ON_TARGET) ? TARGET : EMPTY;
                map[newRow][newCol] = PLAYER;
                playerRow = newRow;
                playerCol = newCol;
            } else if (nextTile == BOX || nextTile == BOX_ON_TARGET) {
                int boxNewRow = newRow + (newRow - playerRow);
                int boxNewCol = newCol + (newCol - playerCol);

                if (isValidMove(boxNewRow, boxNewCol) && map[boxNewRow][boxNewCol] == EMPTY) {
                    map[boxNewRow][boxNewCol] = BOX;
                    map[newRow][newCol] = (nextTile == BOX_ON_TARGET) ? TARGET : EMPTY;
                    map[playerRow][playerCol] = (map[playerRow][playerCol] == PLAYER_ON_TARGET) ? TARGET : EMPTY;
                    map[newRow][newCol] = PLAYER;
                    playerRow = newRow;
                    playerCol = newCol;
                }
            }
            return true;
        }
        return false;
    }

    // Check if the move is valid (within bounds)
    public static boolean isValidMove(int row, int col) {
        return row >= 0 && row < map.length && col >= 0 && col < map[0].length && map[row][col] != WALL;
    }

    // Check if the game is won (all boxes are on targets)
    public static boolean checkVictory() {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == BOX) {
                    return false;
                }
            }
        }
        return true;
    }
}
