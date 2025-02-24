package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ChessGame extends JPanel {
    private static final int TILE_SIZE = 80;
    private static final int BOARD_SIZE = 8;
    private Piece[][] board;
    private int selectedRow = -1, selectedCol = -1;

    public ChessGame() {
        this.board = new Piece[BOARD_SIZE][BOARD_SIZE];
        initializeBoard();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int col = e.getX() / TILE_SIZE;
                int row = e.getY() / TILE_SIZE;
                handleClick(row, col);
            }
        });
    }

    private void initializeBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            board[1][i] = new Piece("P", Color.BLACK);
            board[6][i] = new Piece("P", Color.WHITE);
        }
        board[0][0] = board[0][7] = new Piece("R", Color.BLACK);
        board[0][1] = board[0][6] = new Piece("N", Color.BLACK);
        board[0][2] = board[0][5] = new Piece("B", Color.BLACK);
        board[0][3] = new Piece("Q", Color.BLACK);
        board[0][4] = new Piece("K", Color.BLACK);

        board[7][0] = board[7][7] = new Piece("R", Color.WHITE);
        board[7][1] = board[7][6] = new Piece("N", Color.WHITE);
        board[7][2] = board[7][5] = new Piece("B", Color.WHITE);
        board[7][3] = new Piece("Q", Color.WHITE);
        board[7][4] = new Piece("K", Color.WHITE);
    }

    private void handleClick(int row, int col) {
        if (selectedRow == -1) {
            if (board[row][col] != null) {
                selectedRow = row;
                selectedCol = col;
            }
        } else {
            board[row][col] = board[selectedRow][selectedCol];
            board[selectedRow][selectedCol] = null;
            selectedRow = -1;
            selectedCol = -1;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if ((row + col) % 2 == 0) {
                    g.setColor(Color.LIGHT_GRAY);
                } else {
                    g.setColor(Color.DARK_GRAY);
                }
                g.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);

                if (board[row][col] != null) {
                    board[row][col].draw(g, col * TILE_SIZE, row * TILE_SIZE);
                }

                if (row == selectedRow && col == selectedCol) {
                    g.setColor(new Color(255, 255, 0, 128));
                    g.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private static class Piece {
        String type;
        Color color;

        Piece(String type, Color color) {
            this.type = type;
            this.color = color;
        }

        void draw(Graphics g, int x, int y) {
            g.setColor(color);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString(type, x + 30, y + 50);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Chess Game");
        ChessGame game = new ChessGame();
        frame.add(game);
        frame.setSize(TILE_SIZE * BOARD_SIZE, TILE_SIZE * BOARD_SIZE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

