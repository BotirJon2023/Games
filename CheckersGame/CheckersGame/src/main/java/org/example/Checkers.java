package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class Checkers extends JFrame {
    private static final int SIZE = 8;
    private final CheckerPiece[][] board = new CheckerPiece[SIZE][SIZE];
    private CheckerPiece selectedPiece = null;
    private int currentPlayer = 1; // 1 = Red, 2 = Black

    public Checkers() {
        setTitle("Checkers Game");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initializeBoard();
        add(new CheckersPanel());
    }

    private void initializeBoard() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if ((row + col) % 2 != 0) {
                    if (row < 3) board[row][col] = new CheckerPiece(2); // Black pieces
                    else if (row > 4) board[row][col] = new CheckerPiece(1); // Red pieces
                }
            }
        }
    }

    private class CheckersPanel extends JPanel implements MouseListener {
        private final int CELL_SIZE = 600 / SIZE;
        private List<Point> validMoves = new ArrayList<>();

        public CheckersPanel() {
            addMouseListener(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawBoard(g);
            drawPieces(g);
            highlightValidMoves(g);
        }

        private void drawBoard(Graphics g) {
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    g.setColor((row + col) % 2 == 0 ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                    g.fillRect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        private void drawPieces(Graphics g) {
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (board[row][col] != null) {
                        g.setColor(board[row][col].player == 1 ? Color.RED : Color.BLACK);
                        g.fillOval(col * CELL_SIZE + 10, row * CELL_SIZE + 10, CELL_SIZE - 20, CELL_SIZE - 20);
                        if (board[row][col].isKing) {
                            g.setColor(Color.YELLOW);
                            g.drawString("K", col * CELL_SIZE + CELL_SIZE / 2 - 5, row * CELL_SIZE + CELL_SIZE / 2 + 5);
                        }
                    }
                }
            }
        }

        private void highlightValidMoves(Graphics g) {
            g.setColor(new Color(0, 255, 0, 150));
            for (Point move : validMoves) {
                g.fillRect(move.x * CELL_SIZE, move.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            int col = e.getX() / CELL_SIZE;
            int row = e.getY() / CELL_SIZE;

            if (selectedPiece == null) {
                if (board[row][col] != null && board[row][col].player == currentPlayer) {
                    selectedPiece = board[row][col];
                    validMoves = getValidMoves(row, col);
                }
            } else {
                if (validMoves.contains(new Point(col, row))) {
                    movePiece(row, col);
                    switchPlayer();
                }
                selectedPiece = null;
                validMoves.clear();
            }
            repaint();
        }

        private List<Point> getValidMoves(int row, int col) {
            List<Point> moves = new ArrayList<>();
            int direction = (board[row][col].player == 1) ? -1 : 1;

            for (int[] move : new int[][]{{direction, -1}, {direction, 1}, {2 * direction, -2}, {2 * direction, 2}}) {
                int newRow = row + move[0], newCol = col + move[1];
                if (isValidMove(row, col, newRow, newCol)) {
                    moves.add(new Point(newCol, newRow));
                }
            }
            return moves;
        }

        private boolean isValidMove(int row, int col, int newRow, int newCol) {
            if (newRow < 0 || newRow >= SIZE || newCol < 0 || newCol >= SIZE) return false;
            if (board[newRow][newCol] != null) return false;

            int midRow = (row + newRow) / 2, midCol = (col + newCol) / 2;
            if (Math.abs(newRow - row) == 2) {
                return board[midRow][midCol] != null && board[midRow][midCol].player != board[row][col].player;
            }
            return Math.abs(newRow - row) == 1;
        }

        private void movePiece(int newRow, int newCol) {
            int oldRow = -1, oldCol = -1;
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (board[row][col] == selectedPiece) {
                        oldRow = row;
                        oldCol = col;
                        break;
                    }
                }
            }
            if (oldRow == -1 || oldCol == -1) return;

            board[newRow][newCol] = selectedPiece;
            board[oldRow][oldCol] = null;

            if (Math.abs(newRow - oldRow) == 2) {
                int midRow = (oldRow + newRow) / 2, midCol = (oldCol + newCol) / 2;
                board[midRow][midCol] = null;
            }

            if ((selectedPiece.player == 1 && newRow == 0) || (selectedPiece.player == 2 && newRow == SIZE - 1)) {
                selectedPiece.isKing = true;
            }
        }

        private void switchPlayer() {
            currentPlayer = (currentPlayer == 1) ? 2 : 1;
        }

        @Override
        public void mousePressed(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}
    }

    private static class CheckerPiece {
        int player;
        boolean isKing;

        CheckerPiece(int player) {
            this.player = player;
            this.isKing = false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Checkers::new);
    }
}
