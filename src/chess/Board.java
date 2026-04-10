package chess;

import chess.pieces.*;
import chess.pieces.Piece.Color;

public class Board {

    private Piece[][] grid = new Piece[8][8];

    public Board() {
        setup();
    }

    private void setup() {
        grid[7][0] = new Rook(Piece.Color.BLACK, 7, 0);
        grid[7][1] = new Knight(Piece.Color.BLACK, 7, 1);
        grid[7][2] = new Bishop(Piece.Color.BLACK, 7, 2);
        grid[7][3] = new Queen(Piece.Color.BLACK, 7, 3);
        grid[7][4] = new King(Piece.Color.BLACK, 7, 4);
        grid[7][5] = new Bishop(Piece.Color.BLACK, 7, 5);
        grid[7][6] = new Knight(Piece.Color.BLACK, 7, 6);
        grid[7][7] = new Rook(Piece.Color.BLACK, 7, 7);
        for (int c = 0; c < 8; c++) grid[6][c] = new Pawn(Piece.Color.BLACK, 6, c);

        grid[0][0] = new Rook(Piece.Color.WHITE, 0, 0);
        grid[0][1] = new Knight(Piece.Color.WHITE, 0, 1);
        grid[0][2] = new Bishop(Piece.Color.WHITE, 0, 2);
        grid[0][3] = new Queen(Piece.Color.WHITE, 0, 3);
        grid[0][4] = new King(Piece.Color.WHITE, 0, 4);
        grid[0][5] = new Bishop(Piece.Color.WHITE, 0, 5);
        grid[0][6] = new Knight(Piece.Color.WHITE, 0, 6);
        grid[0][7] = new Rook(Piece.Color.WHITE, 0, 7);
        for (int c = 0; c < 8; c++) grid[1][c] = new Pawn(Piece.Color.WHITE, 1, c);
    }

    public void applyMove(Move move) {
        Piece piece = grid[move.getFromRow()][move.getFromCol()];
        if (piece == null) return;

        String notation = move.getAlgebraic().replaceAll("[+#]", "");

        if (notation.equals("O-O")) {
            int row = piece.isWhite() ? 0 : 7;
            grid[row][6] = grid[row][4];
            grid[row][5] = grid[row][7];
            grid[row][4] = null;
            grid[row][7] = null;
            grid[row][6].setPosition(row, 6);
            grid[row][5].setPosition(row, 5);
            ((King) grid[row][6]).markMoved();
            ((Rook) grid[row][5]).markMoved();
            return;
        }

        if (notation.equals("O-O-O")) {
            int row = piece.isWhite() ? 0 : 7;
            grid[row][2] = grid[row][4];
            grid[row][3] = grid[row][0];
            grid[row][4] = null;
            grid[row][0] = null;
            grid[row][2].setPosition(row, 2);
            grid[row][3].setPosition(row, 3);
            ((King) grid[row][2]).markMoved();
            ((Rook) grid[row][3]).markMoved();
            return;
        }

        clearEnPassant(piece.getColor());

        if (piece instanceof Pawn && move.getFromCol() != move.getToCol()
                && grid[move.getToRow()][move.getToCol()] == null) {
            grid[move.getFromRow()][move.getToCol()] = null;
        }

        if (piece instanceof Pawn && Math.abs(move.getToRow() - move.getFromRow()) == 2) {
            ((Pawn) piece).setEnPassantReady(true);
        }

        grid[move.getToRow()][move.getToCol()] = piece;
        grid[move.getFromRow()][move.getFromCol()] = null;
        piece.setPosition(move.getToRow(), move.getToCol());

        if (piece instanceof King) ((King) piece).markMoved();
        if (piece instanceof Rook) ((Rook) piece).markMoved();
        if (piece instanceof Pawn) ((Pawn) piece).markMoved();

        if (piece instanceof Pawn && ((Pawn) piece).canPromote()) {
            String promo = move.getPromotionPiece() != null ? move.getPromotionPiece() : "Q";
            grid[move.getToRow()][move.getToCol()] = createPromotedPiece(promo, piece.getColor(), move.getToRow(), move.getToCol());
        }
    }

    private void clearEnPassant(Piece.Color color) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (grid[r][c] instanceof Pawn && grid[r][c].getColor() == color)
                    ((Pawn) grid[r][c]).setEnPassantReady(false);
    }

    private Piece createPromotedPiece(String symbol, Piece.Color color, int row, int col) {
        switch (symbol) {
            case "Q": return new Queen(color, row, col);
            case "R": return new Rook(color, row, col);
            case "B": return new Bishop(color, row, col);
            case "N": return new Knight(color, row, col);
            default:  return new Queen(color, row, col);
        }
    }

    public boolean isInCheck(Piece.Color color) {
        int[] kingPos = findKing(color);
        if (kingPos == null) return false;
        int kr = kingPos[0], kc = kingPos[1];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                if (p != null && p.getColor() != color && p.canMoveTo(kr, kc, grid))
                    return true;
            }
        return false;
    }

    public boolean isCheckmate(Piece.Color color) {
        return isInCheck(color) && !hasLegalMoves(color);
    }

    public boolean isStalemate(Piece.Color color) {
        return !isInCheck(color) && !hasLegalMoves(color);
    }

    private boolean hasLegalMoves(Piece.Color color) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                if (p == null || p.getColor() != color) continue;
                for (int tr = 0; tr < 8; tr++)
                    for (int tc = 0; tc < 8; tc++)
                        if (p.canMoveTo(tr, tc, grid) && !wouldLeaveKingInCheck(r, c, tr, tc, color))
                            return true;
            }
        return false;
    }

    public boolean wouldLeaveKingInCheck(int fromRow, int fromCol, int toRow, int toCol, Piece.Color color) {
        Piece[][] copy = copyGrid();
        copy[toRow][toCol]     = copy[fromRow][fromCol];
        copy[fromRow][fromCol] = null;
        if (copy[toRow][toCol] != null) copy[toRow][toCol].setPosition(toRow, toCol);

        int[] kingPos = findKingInGrid(copy, color);
        if (kingPos == null) return true;
        int kr = kingPos[0], kc = kingPos[1];

        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = copy[r][c];
                if (p != null && p.getColor() != color && p.canMoveTo(kr, kc, copy))
                    return true;
            }
        return false;
    }

    public boolean canCastle(Piece.Color color, boolean kingSide) {
        int row = color == Piece.Color.WHITE ? 0 : 7;
        Piece k = grid[row][4];
        if (!(k instanceof King) || ((King) k).hasMoved() || isInCheck(color)) return false;

        if (kingSide) {
            Piece rook = grid[row][7];
            if (!(rook instanceof Rook) || ((Rook) rook).hasMoved()) return false;
            if (grid[row][5] != null || grid[row][6] != null) return false;
            return !wouldLeaveKingInCheck(row, 4, row, 5, color)
                && !wouldLeaveKingInCheck(row, 4, row, 6, color);
        } else {
            Piece rook = grid[row][0];
            if (!(rook instanceof Rook) || ((Rook) rook).hasMoved()) return false;
            if (grid[row][1] != null || grid[row][2] != null || grid[row][3] != null) return false;
            return !wouldLeaveKingInCheck(row, 4, row, 3, color)
                && !wouldLeaveKingInCheck(row, 4, row, 2, color);
        }
    }

    private int[] findKing(Piece.Color color) {
        return findKingInGrid(grid, color);
    }

    private int[] findKingInGrid(Piece[][] g, Piece.Color color) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (g[r][c] instanceof King && g[r][c].getColor() == color)
                    return new int[]{r, c};
        return null;
    }

    private Piece[][] copyGrid() {
        Piece[][] copy = new Piece[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                copy[r][c] = grid[r][c];
        return copy;
    }

    public Piece getPiece(int row, int col) { return grid[row][col]; }
    public Piece[][] getGrid()              { return grid; }

    // Mayusculas = blancas, minusculas = negras
    // K=Rey Q=Dama R=Torre B=Alfil N=Caballo P=Peon
    public String display() {
        StringBuilder sb = new StringBuilder();
        sb.append("  a b c d e f g h\n");
        sb.append("  ----------------\n");
        for (int r = 7; r >= 0; r--) {
            sb.append(r + 1).append("|");
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                sb.append(p == null ? "." : p.getDisplay()).append(" ");
            }
            sb.append("|").append(r + 1).append("\n");
        }
        sb.append("  ----------------\n");
        sb.append("  a b c d e f g h\n");
        return sb.toString();
    }
}
