package org.example;

public class Chess3D {
    // 3D board: 8x8x3 (3 levels of 8x8 boards)
    private Piece[][][] board;
    private boolean whiteTurn;

    // Piece class to represent chess pieces
    static class Piece {
        String type; // "pawn", "rook", "knight", etc.
        boolean isWhite;

        Piece(String type, boolean isWhite) {
            this.type = type;
            this.isWhite = isWhite;
        }

        @Override
        public String toString() {
            return (isWhite ? "W" : "B") + type.charAt(0);
        }
    }

    // Initialize the 3D chess board
    public Chess3D() {
        board = new Piece[8][8][3];
        whiteTurn = true;
        setupBoard();
    }

    // Set up initial piece positions
    private void setupBoard() {
        // Level 0 - White's main pieces
        board[0][0][0] = new Piece("rook", true);
        board[0][1][0] = new Piece("knight", true);
        board[0][2][0] = new Piece("bishop", true);
        board[0][3][0] = new Piece("queen", true);
        board[0][4][0] = new Piece("king", true);
        board[0][5][0] = new Piece("bishop", true);
        board[0][6][0] = new Piece("knight", true);
        board[0][7][0] = new Piece("rook", true);
        for (int i = 0; i < 8; i++) {
            board[1][i][0] = new Piece("pawn", true);
        }

        // Level 2 - Black's main pieces
        board[7][0][2] = new Piece("rook", false);
        board[7][1][2] = new Piece("knight", false);
        board[7][2][2] = new Piece("bishop", false);
        board[7][3][2] = new Piece("queen", false);
        board[7][4][2] = new Piece("king", false);
        board[7][5][2] = new Piece("bishop", false);
        board[7][6][2] = new Piece("knight", false);
        board[7][7][2] = new Piece("rook", false);
        for (int i = 0; i < 8; i++) {
            board[6][i][2] = new Piece("pawn", false);
        }
        // Level 1 remains mostly empty initially
    }

    // Print the current board state
    public void printBoard() {
        for (int z = 2; z >= 0; z--) {
            System.out.println("Level " + z + ":");
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    Piece p = board[x][y][z];
                    System.out.print(p == null ? " -- " : " " + p + " ");
                }
                System.out.println();
            }
            System.out.println();
        }
    }

    // Basic move validation (simplified)
    public boolean movePiece(int fromX, int fromY, int fromZ,
                             int toX, int toY, int toZ) {
        if (!isValidPosition(fromX, fromY, fromZ) ||
                !isValidPosition(toX, toY, toZ)) {
            return false;
        }

        Piece piece = board[fromX][fromY][fromZ];
        if (piece == null || piece.isWhite != whiteTurn) {
            return false;
        }

        // Simplified movement rules (expand this for full rules)
        boolean validMove = isValidMove(piece, fromX, fromY, fromZ, toX, toY, toZ);
        if (!validMove) return false;

        // Execute move
        board[toX][toY][toZ] = piece;
        board[fromX][fromY][fromZ] = null;
        whiteTurn = !whiteTurn;
        return true;
    }

    private boolean isValidPosition(int x, int y, int z) {
        return x >= 0 && x < 8 && y >= 0 && y < 8 && z >= 0 && z < 3;
    }

    private boolean isValidMove(Piece piece, int fromX, int fromY, int fromZ,
                                int toX, int toY, int toZ) {
        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);
        int dz = Math.abs(toZ - fromZ);

        switch (piece.type) {
            case "pawn":
                return (piece.isWhite && toX == fromX + 1 && dy == 0 && dz == 0) ||
                        (!piece.isWhite && toX == fromX - 1 && dy == 0 && dz == 0);
            case "rook":
                return (dx != 0 && dy == 0 && dz == 0) ||
                        (dy != 0 && dx == 0 && dz == 0) ||
                        (dz != 0 && dx == 0 && dy == 0);
            case "knight":
                return (dx == 2 && dy == 1) || (dx == 1 && dy == 2) ||
                        (dx == 2 && dz == 1) || (dx == 1 && dz == 2) ||
                        (dy == 2 && dz == 1) || (dy == 1 && dz == 2);
            // Add more piece rules here
            default:
                return false;
        }
    }

    public static void main(String[] args) {
        Chess3D game = new Chess3D();
        game.printBoard();

        // Example move: White pawn from (1,0,0) to (2,0,0)
        boolean moved = game.movePiece(1, 0, 0, 2, 0, 0);
        System.out.println("Move successful: " + moved);
        game.printBoard();
    }
}