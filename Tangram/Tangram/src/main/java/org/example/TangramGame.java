package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public class TangramGame extends JPanel {
    private List<TangramPiece> pieces;
    private TangramPiece selectedPiece;
    private Point lastMousePosition;

    public TangramGame() {
        pieces = new ArrayList<>();
        initializePieces();
        addMouseListener(new MouseHandler());
        addMouseMotionListener(new MouseHandler());
    }

    private void initializePieces() {
        pieces.add(new TangramPiece(new int[]{0, 100, 0}, new int[]{0, 100, 100}, Color.RED));
        pieces.add(new TangramPiece(new int[]{100, 200, 100}, new int[]{100, 200, 200}, Color.BLUE));
        pieces.add(new TangramPiece(new int[]{200, 300, 200}, new int[]{200, 300, 300}, Color.GREEN));
        pieces.add(new TangramPiece(new int[]{300, 400, 300}, new int[]{300, 400, 400}, Color.YELLOW));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        for (TangramPiece piece : pieces) {
            piece.draw(g2d);
        }
    }

    private class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            for (TangramPiece piece : pieces) {
                if (piece.contains(e.getPoint())) {
                    selectedPiece = piece;
                    lastMousePosition = e.getPoint();
                    break;
                }
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectedPiece != null) {
                int dx = e.getX() - lastMousePosition.x;
                int dy = e.getY() - lastMousePosition.y;
                selectedPiece.move(dx, dy);
                lastMousePosition = e.getPoint();
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            selectedPiece = null;
        }
    }

    private static class TangramPiece {
        private Polygon shape;
        private Color color;

        public TangramPiece(int[] xPoints, int[] yPoints, Color color) {
            this.shape = new Polygon(xPoints, yPoints, xPoints.length);
            this.color = color;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.fill(shape);
            g2d.setColor(Color.BLACK);
            g2d.draw(shape);
        }

        public boolean contains(Point p) {
            return shape.contains(p);
        }

        public void move(int dx, int dy) {
            shape.translate(dx, dy);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tangram Game");
        TangramGame game = new TangramGame();
        frame.add(game);
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
