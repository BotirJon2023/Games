package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

public class MazeSolverGame extends JPanel {
    private static final int SIZE = 20;
    private static final int ROWS = 25, COLS = 25;
    private final int[][] maze = new int[ROWS][COLS];
    private final boolean[][] visited = new boolean[ROWS][COLS];
    private final int[][] solution;
    private int playerRow = 0, playerCol = 0;
    private final int exitRow = ROWS - 1, exitCol = COLS - 1;

    public MazeSolverGame() {
        generateMaze();
        solution = solveMaze();
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                movePlayer(e.getKeyCode());
                repaint();
            }
        });
    }

    private void generateMaze() {
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{0, 0});
        maze[0][0] = 1;

        while (!stack.isEmpty()) {
            int[] cell = stack.peek();
            int r = cell[0], c = cell[1];
            visited[r][c] = true;
            List<int[]> neighbors = new ArrayList<>();

            int[][] directions = {{0, 2}, {0, -2}, {2, 0}, {-2, 0}};
            for (int[] d : directions) {
                int nr = r + d[0], nc = c + d[1];
                if (nr >= 0 && nc >= 0 && nr < ROWS && nc < COLS && !visited[nr][nc]) {
                    neighbors.add(new int[]{nr, nc});
                }
            }

            if (!neighbors.isEmpty()) {
                int[] next = neighbors.get(new Random().nextInt(neighbors.size()));
                maze[(r + next[0]) / 2][(c + next[1]) / 2] = 1;
                maze[next[0]][next[1]] = 1;
                stack.push(next);
            } else {
                stack.pop();
            }
        }
    }

    private int[][] solveMaze() {
        int[][] parent = new int[ROWS * COLS][2];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = new int[]{-1, -1};
        }
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{0, 0});
        parent[0] = new int[]{-1, -1};

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int r = cell[0], c = cell[1];
            if (r == exitRow && c == exitCol) break;

            for (int[] d : new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}}) {
                int nr = r + d[0], nc = c + d[1];
                if (nr >= 0 && nc >= 0 && nr < ROWS && nc < COLS && maze[nr][nc] == 1 && parent[nr * COLS + nc][0] == -1) {
                    parent[nr * COLS + nc] = new int[]{r, c};
                    queue.add(new int[]{nr, nc});
                }
            }
        }

        List<int[]> pathList = new ArrayList<>();
        for (int r = exitRow, c = exitCol; r != -1 && c != -1; ) {
            pathList.add(new int[]{r, c});
            int[] p = parent[r * COLS + c];
            r = p[0];
            c = p[1];
        }
        Collections.reverse(pathList);
        return pathList.toArray(new int[0][0]);
    }

    private void movePlayer(int keyCode) {
        int newRow = playerRow, newCol = playerCol;
        if (keyCode == KeyEvent.VK_UP) newRow--;
        else if (keyCode == KeyEvent.VK_DOWN) newRow++;
        else if (keyCode == KeyEvent.VK_LEFT) newCol--;
        else if (keyCode == KeyEvent.VK_RIGHT) newCol++;

        if (newRow >= 0 && newRow < ROWS && newCol >= 0 && newCol < COLS && maze[newRow][newCol] == 1) {
            playerRow = newRow;
            playerCol = newCol;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (maze[r][c] == 0) g.setColor(Color.BLACK);
                else g.setColor(Color.WHITE);
                g.fillRect(c * SIZE, r * SIZE, SIZE, SIZE);
            }
        }
        g.setColor(Color.BLUE);
        g.fillRect(playerCol * SIZE, playerRow * SIZE, SIZE, SIZE);
        g.setColor(Color.RED);
        g.fillRect(exitCol * SIZE, exitRow * SIZE, SIZE, SIZE);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Maze Solver Game");
        MazeSolverGame game = new MazeSolverGame();
        frame.add(game);
        frame.setSize(520, 540);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

