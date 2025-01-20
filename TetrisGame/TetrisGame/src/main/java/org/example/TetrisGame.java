package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

public class TetrisGame extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 600;
    private static final int BLOCK_SIZE = 30;
    private static final int BOARD_WIDTH = WIDTH / BLOCK_SIZE;
    private static final int BOARD_HEIGHT = HEIGHT / BLOCK_SIZE;

    private Timer timer;
    private boolean[][] board;
    private Tetrimino currentTetrimino;
    private boolean gameOver;
    private int score;

    public TetrisGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);

        board = new boolean[BOARD_HEIGHT][BOARD_WIDTH];
        gameOver = false;
        score = 0;

        timer = new Timer(500, this);
        timer.start();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawBoard(g);
        drawTetrimino(g);

        if (gameOver) {
            drawGameOver(g);
        }

        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("Score: " + score, 10, 30);
    }

    private void drawBoard(Graphics g) {
        g.setColor(Color.gray);
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (board[i][j]) {
                    g.fillRect(j * BLOCK_SIZE, i * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                }
            }
        }
    }

    private void drawTetrimino(Graphics g) {
        if (currentTetrimino != null) {
            g.setColor(currentTetrimino.color);
            for (Point p : currentTetrimino.getBlocks()) {
                g.fillRect((p.x + currentTetrimino.x) * BLOCK_SIZE, (p.y + currentTetrimino.y) * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
            }
        }
    }

    private void drawGameOver(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        g.drawString("GAME OVER", WIDTH / 4, HEIGHT / 2);
    }

    private void spawnTetrimino() {
        currentTetrimino = Tetrimino.getRandomTetrimino();
        currentTetrimino.x = BOARD_WIDTH / 2 - 1;
        currentTetrimino.y = 0;

        if (isCollision()) {
            gameOver = true;
        }
    }

    private void moveTetriminoDown() {
        if (!isCollision()) {
            currentTetrimino.y++;
        } else {
            lockTetrimino();
            clearFullLines();
            spawnTetrimino();
        }
    }

    private void lockTetrimino() {
        for (Point p : currentTetrimino.getBlocks()) {
            board[currentTetrimino.y + p.y][currentTetrimino.x + p.x] = true;
        }
    }

    private void clearFullLines() {
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            if (isLineFull(i)) {
                clearLine(i);
                score += 100;
            }
        }
    }

    private boolean isLineFull(int row) {
        for (int j = 0; j < BOARD_WIDTH; j++) {
            if (!board[row][j]) {
                return false;
            }
        }
        return true;
    }

    private void clearLine(int row) {
        for (int i = row; i > 0; i--) {
            System.arraycopy(board[i - 1], 0, board[i], 0, BOARD_WIDTH);
        }
        Arrays.fill(board[0], false);
    }

    private boolean isCollision() {
        for (Point p : currentTetrimino.getBlocks()) {
            int x = currentTetrimino.x + p.x;
            int y = currentTetrimino.y + p.y;

            if (x < 0 || x >= BOARD_WIDTH || y >= BOARD_HEIGHT || (y >= 0 && board[y][x])) {
                return true;
            }
        }
        return false;
    }

    private void rotateTetrimino() {
        currentTetrimino.rotate();
        if (isCollision()) {
            currentTetrimino.rotateBack();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            moveTetriminoDown();
            repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            currentTetrimino.x--;
            if (isCollision()) {
                currentTetrimino.x++;
            }
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            currentTetrimino.x++;
            if (isCollision()) {
                currentTetrimino.x--;
            }
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            moveTetriminoDown();
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            rotateTetrimino();
        }
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tetris");
        TetrisGame game = new TetrisGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        game.spawnTetrimino();
    }

    @java.lang.Override
    public void actionPerformed(java.awt.event.ActionEvent e) {

    }

    @java.lang.Override
    public void keyTyped(java.awt.event.KeyEvent e) {

    }

    @java.lang.Override
    public void keyPressed(java.awt.event.KeyEvent e) {

    }

    @java.lang.Override
    public void keyReleased(java.awt.event.KeyEvent e) {

    }

    static class Tetrimino {
        int x, y;
        Color color;
        private final Point[] blocks;

        public Tetrimino(Color color, Point[] blocks) {
            this.color = color;
            this.blocks = blocks;
        }

        public Point[] getBlocks() {
            return blocks;
        }

        public static Tetrimino getRandomTetrimino() {
            Random rand = new Random();
            int type = rand.nextInt(7);
            switch (type) {
                case 0:
                    return new Tetrimino(Color.cyan, new Point[]{new Point(0, 0), new Point(1, 0), new Point(2, 0), new Point(3, 0)});
                case 1:
                    return new Tetrimino(Color.blue, new Point[]{new Point(0, 0), new Point(1, 0), new Point(1, 1), new Point(2, 1)});
                case 2:
                    return new Tetrimino(Color.orange, new Point[]{new Point(1, 0), new Point(2, 0), new Point(0, 1), new Point(1, 1)});
                case 3:
                    return new Tetrimino(Color.green, new Point[]{new Point(0, 0), new Point(1, 0), new Point(0, 1), new Point(1, 1)});
                case 4:
                    return new Tetrimino(Color.red, new Point[]{new Point(1, 0), new Point(2, 0), new Point(3, 0), new Point(3, 1)});
                case 5:
                    return new Tetrimino(Color.pink, new Point[]{new Point(0, 0), new Point(1, 0), new Point(2, 0), new Point(2, 1)});
                case 6:
                    return new Tetrimino(Color.yellow, new Point[]{new Point(0, 0), new Point(0, 1), new Point(1, 1), new Point(2, 1)});
                default:
                    return null;
            }
        }

        public void rotate() {
            for (int i = 0; i < blocks.length; i++) {
                int temp = blocks[i].x;
                blocks[i].x = blocks[i].y;
                blocks[i].y = -temp;
            }
        }

        public void rotateBack() {
            rotate();
            rotate();
            rotate();
            rotate();
        }
    }
}
