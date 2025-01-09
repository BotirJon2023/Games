package org.example;

import java.util.*;

public class PuzzleSolver {

    class PuzzleState {
        int[][] board;
        int emptyRow, emptyCol, cost, level;
        PuzzleState parent;

        public PuzzleState(int[][] board, int emptyRow, int emptyCol, int level) {
            this.board = board;
            this.emptyRow = emptyRow;
            this.emptyCol = emptyCol;
            this.level = level;
            this.cost = 0;
        }

        public void printBoard() {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    System.out.print(board[r][c] + " ");
                }
                System.out.println();
            }
        }

        public boolean isGoalState() {
            int[][] goal = {
                    {1, 2, 3},
                    {4, 5, 6},
                    {7, 8, 0}
            };
            return Arrays.deepEquals(board, goal);
        }

        public int calculateCost() {
            int cost = 0;
            int[][] goal = {
                    {1, 2, 3},
                    {4, 5, 6},
                    {7, 8, 0}
            };
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (board[r][c] != goal[r][c] && board[r][c] != 0) {
                        cost++;
                    }
                }
            }
            return cost;
        }
    }

    public class PuzzleSolver2 {
        public static void solvePuzzle(int[][] startBoard) {
            PuzzleState initialState = new PuzzleState(startBoard, 2, 2, 0);
            initialState.cost = initialState.calculateCost();

            PriorityQueue<PuzzleState> openList = new PriorityQueue<>(Comparator.comparingInt(a -> a.cost + a.level));
            HashSet<String> closedSet = new HashSet<>();

            openList.add(initialState);

            while (!openList.isEmpty()) {
                PuzzleState currentState = openList.poll();

                if (currentState.isGoalState()) {
                    System.out.println("Goal reached!");
                    currentState.printBoard();
                    return;
                }

                // Generate child states (move empty space)
                List<PuzzleState> children = generateChildren(currentState);
                for (PuzzleState child : children) {
                    String childState = Arrays.deepToString(child.board);
                    if (!closedSet.contains(childState)) {
                        child.cost = child.calculateCost();
                        openList.add(child);
                        closedSet.add(childState);
                    }
                }
            }
        }

        public List<PuzzleState> generateChildren(PuzzleState state) {
            List<PuzzleState> children = new ArrayList<>();

            // Move empty space up, down, left, right
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] direction : directions) {
                int newRow = state.emptyRow + direction[0];
                int newCol = state.emptyCol + direction[1];

                if (newRow >= 0 && newRow < 3 && newCol >= 0 && newCol < 3) {
                    int[][] newBoard = cloneBoard(state.board);
                    newBoard[state.emptyRow][state.emptyCol] = newBoard[newRow][newCol];
                    newBoard[newRow][newCol] = 0;

                    PuzzleState child = new PuzzleState(newBoard, newRow, newCol, state.level + 1);
                    child.parent = state;
                    children.add(child);
                }
            }
            return children;
        }

        public static int[][] cloneBoard(int[][] board) {
            int[][] newBoard = new int[3][3];
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    newBoard[r][c] = board[r][c];
                }
            }
            return newBoard;
        }

        public static void main(String[] args) {
            int[][] startBoard = {
                    {1, 2, 3},
                    {4, 0, 5},
                    {7, 8, 6}
            };
            solvePuzzle(startBoard);
        }
    }

}
