package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class RubiksCubeSimulator extends JPanel implements KeyListener {
    private char[][][] cube;
    private static final char[] COLORS = {'W', 'Y', 'R', 'O', 'B', 'G'};

    public RubiksCubeSimulator() {
        initializeCube();
        addKeyListener(this);
        setFocusable(true);
    }

    private void initializeCube() {
        cube = new char[6][3][3];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    cube[i][j][k] = COLORS[i];
                }
            }
        }
    }

    public void rotateFace(int face) {
        char[][] temp = new char[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                temp[j][2 - i] = cube[face][i][j];
            }
        }
        cube[face] = temp;
        repaint();
    }

    public void scrambleCube() {
        Random rand = new Random();
        for (int i = 0; i < 20; i++) {
            rotateFace(rand.nextInt(6));
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawCube(g);
    }

    private void drawCube(Graphics g) {
        int size = 50;
        int startX = 100, startY = 100;
        for (int f = 0; f < 6; f++) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    g.setColor(getColor(cube[f][i][j]));
                    g.fillRect(startX + j * size, startY + i * size, size, size);
                    g.setColor(Color.BLACK);
                    g.drawRect(startX + j * size, startY + i * size, size, size);
                }
            }
            startY += 160;
        }
    }

    private Color getColor(char c) {
        switch (c) {
            case 'W':
                return Color.WHITE;
            case 'Y':
                return Color.YELLOW;
            case 'R':
                return Color.RED;
            case 'O':
                return Color.ORANGE;
            case 'B':
                return Color.BLUE;
            case 'G':
                return Color.GREEN;
            default:
                return Color.BLACK;
        }
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            scrambleCube();
        } else if (e.getKeyCode() == KeyEvent.VK_R) {
            rotateFace(0);
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Rubik's Cube Simulator");
        RubiksCubeSimulator cubeSim = new RubiksCubeSimulator();
        frame.add(cubeSim);
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
