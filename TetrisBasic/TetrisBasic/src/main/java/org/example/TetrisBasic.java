package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Tetris extends JPanel {
    private final int BLOCK_SIZE = 30;
    private final int BOARD_WIDTH = 10, BOARD_HEIGHT = 20;
    private int[][] board = new int[BOARD_HEIGHT][BOARD_WIDTH];
    private int[] currentPiece;
    private int currentX, currentY;

    public Tetris() {
        this.setPreferredSize(new Dimension(BOARD_WIDTH * BLOCK_SIZE, BOARD_HEIGHT * BLOCK_SIZE));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);

        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) movePiece(-1, 0);
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) movePiece(1, 0);
                if (e.getKeyCode() == KeyEvent.VK_DOWN) movePiece(0, 1);
            }
        });
        spawnPiece();
        Timer timer = new Timer(500, e -> {
            movePiece(0, 1);
            repaint();
        });
        timer.start();
    }

    private void spawnPiece() {
        currentPiece = new int[]{1, 1, 1, 1};  // Simple horizontal line
        currentX = 4;
        currentY = 0;
    }

    private void movePiece(int dx, int dy) {
        currentX += dx;
        currentY += dy;
        if (currentY >= BOARD_HEIGHT || !validMove()) {
            currentY -= dy;
            placePiece();
            spawnPiece();
        }
    }

    private boolean validMove() {
        for (int i = 0; i < currentPiece.length; i++) {
            int x = currentX + i;
            if (x < 0 || x >= BOARD_WIDTH || currentY >= BOARD_HEIGHT) return false;
            if (board[currentY][x] != 0) return false;
        }
        return true;
    }

    private void placePiece() {
        for (int i = 0; i < currentPiece.length; i++) {
            board[currentY][currentX + i] = 1;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        for (int row = 0; row < BOARD_HEIGHT; row++) {
            for (int col = 0; col < BOARD_WIDTH; col++) {
                if (board[row][col] == 1) {
                    g.fillRect(col * BLOCK_SIZE, row * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                }
            }
        }

        g.setColor(Color.RED);
        for (int i = 0; i < currentPiece.length; i++) {
            g.fillRect((currentX + i) * BLOCK_SIZE, currentY * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tetris");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new Tetris());
        frame.pack();
        frame.setVisible(true);
    }
}