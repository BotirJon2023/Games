package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class JigsawPuzzleSimulator extends JFrame {
    private static final int PUZZLE_SIZE = 3; // 3x3 puzzle for simplicity
    private static final int PIECE_SIZE = 100;
    private PuzzlePiece[][] puzzle;
    private PuzzlePiece draggedPiece;
    private Point initialClick;
    private boolean puzzleCompleted;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JigsawPuzzleSimulator game = new JigsawPuzzleSimulator();
            game.setVisible(true);
        });
    }

    public JigsawPuzzleSimulator() {
        setTitle("Jigsaw Puzzle Simulator");
        setSize(PIECE_SIZE * PUZZLE_SIZE, PIECE_SIZE * PUZZLE_SIZE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        puzzle = new PuzzlePiece[PUZZLE_SIZE][PUZZLE_SIZE];
        puzzleCompleted = false;

        // Initialize the puzzle pieces
        for (int i = 0; i < PUZZLE_SIZE; i++) {
            for (int j = 0; j < PUZZLE_SIZE; j++) {
                puzzle[i][j] = new PuzzlePiece(i, j, PIECE_SIZE, loadImage(i, j));
                puzzle[i][j].setLocation(i * PIECE_SIZE, j * PIECE_SIZE);
                add(puzzle[i][j]);
            }
        }

        // Mouse listeners for drag and drop functionality
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                draggedPiece = getPieceAtLocation(initialClick);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggedPiece != null) {
                    Point releasePoint = e.getPoint();
                    if (puzzleCompleted) {
                        JOptionPane.showMessageDialog(null, "Puzzle already completed!");
                        return;
                    }
                    draggedPiece.setLocation(getSnappedLocation(releasePoint));
                    checkPuzzleCompletion();
                    draggedPiece = null;
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedPiece != null) {
                    Point currentLocation = e.getPoint();
                    int dx = currentLocation.x - initialClick.x;
                    int dy = currentLocation.y - initialClick.y;
                    draggedPiece.setLocation(draggedPiece.getX() + dx, draggedPiece.getY() + dy);
                    initialClick = currentLocation;
                }
            }
        });
    }

    private PuzzlePiece getPieceAtLocation(Point p) {
        for (int i = 0; i < PUZZLE_SIZE; i++) {
            for (int j = 0; j < PUZZLE_SIZE; j++) {
                if (puzzle[i][j].getBounds().contains(p)) {
                    return puzzle[i][j];
                }
            }
        }
        return null;
    }

    private Point getSnappedLocation(Point p) {
        int x = (p.x / PIECE_SIZE) * PIECE_SIZE;
        int y = (p.y / PIECE_SIZE) * PIECE_SIZE;
        return new Point(x, y);
    }

    private Image loadImage(int i, int j) {
        // For simplicity, load simple color blocks as puzzle pieces
        BufferedImage image = new BufferedImage(PIECE_SIZE, PIECE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color((i * 50) % 255, (j * 100) % 255, (i * j * 50) % 255));
        g.fillRect(0, 0, PIECE_SIZE, PIECE_SIZE);
        g.dispose();
        return image;
    }

    private void checkPuzzleCompletion() {
        for (int i = 0; i < PUZZLE_SIZE; i++) {
            for (int j = 0; j < PUZZLE_SIZE; j++) {
                if (!puzzle[i][j].getLocation().equals(new Point(i * PIECE_SIZE, j * PIECE_SIZE))) {
                    return;
                }
            }
        }
        puzzleCompleted = true;
        JOptionPane.showMessageDialog(this, "Congratulations! You've completed the puzzle!");
    }

    class PuzzlePiece extends JLabel {
        private int row, col;

        public PuzzlePiece(int row, int col, int size, Image img) {
            super(new ImageIcon(img));
            this.row = row;
            this.col = col;
            setSize(size, size);
            setLocation(row * size, col * size);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }
    }
}