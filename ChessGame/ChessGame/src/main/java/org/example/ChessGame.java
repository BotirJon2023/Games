package org.example;


class ChessPiece {
    String type;
    String color;

    public ChessPiece(String type, String color) {
        this.type = type;
        this.color = color;
    }

    public boolean isValidMove(int startX, int startY, int endX, int endY) {
        return false; // Abstract method, override for specific pieces
    }
}

class Rook extends ChessPiece {
    public Rook(String color) {
        super("Rook", color);
    }

    @Override
    public boolean isValidMove(int startX, int startY, int endX, int endY) {
        // Rooks can move horizontally or vertically
        return startX == endX || startY == endY;
    }
}

class Knight extends ChessPiece {
    public Knight(String color) {
        super("Knight", color);
    }

    @Override
    public boolean isValidMove(int startX, int startY, int endX, int endY) {
        // Knights move in an "L" shape
        return Math.abs(startX - endX) == 2 && Math.abs(startY - endY) == 1
                || Math.abs(startX - endX) == 1 && Math.abs(startY - endY) == 2;
    }
}

public class ChessGame {
    public static void main(String[] args) {
        ChessPiece whiteRook = new Rook("White");
        ChessPiece blackKnight = new Knight("Black");

        System.out.println("White Rook valid move: " + whiteRook.isValidMove(0, 0, 0, 5));
        System.out.println("Black Knight valid move: " + blackKnight.isValidMove(1, 2, 3, 3));
    }
}
