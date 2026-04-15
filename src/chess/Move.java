package chess;

import chess.pieces.Pawn;
import chess.pieces.Piece;
import chess.pieces.Piece.Color;

public class Move {

    private final int fromRow;
    private final int fromCol;
    private final int toRow;
    private final int toCol;
    private final String algebraic; // notación recibida del jugador (ej: "e4", "Nf3")
    private String promotionPiece;  // "Q", "R", "B", "N" si hay coronación

    public Move(int fromRow, int fromCol, int toRow, int toCol, String algebraic) {
        this.fromRow   = fromRow;
        this.fromCol   = fromCol;
        this.toRow     = toRow;
        this.toCol     = toCol;
        this.algebraic = algebraic;
    }

    // Convierte notación algebraica a coordenadas internas del tablero
    // Soporta: e2e4, e7e8q (origen+destino), e4, Nf3, Bxe5, O-O, O-O-O, e8=Q
    public static Move fromAlgebraic(String notation, Piece[][] board, Piece.Color turn) {
        if (notation == null || notation.isEmpty()) return null;

        String n = notation.replaceAll("[+#]", ""); // quitar + y # del final

        // Enroque corto
        if (n.equals("O-O")) {
            int row = turn == Piece.Color.WHITE ? 0 : 7;
            return new Move(row, 4, row, 6, notation);
        }

        // Enroque largo
        if (n.equals("O-O-O")) {
            int row = turn == Piece.Color.WHITE ? 0 : 7;
            return new Move(row, 4, row, 2, notation);
        }

        Move longAlg = tryParseLongAlgebraic(n, notation, board, turn);
        if (longAlg != null) return longAlg;

        String promotionTo = null;
        if (n.contains("=")) {
            String[] parts = n.split("=");
            promotionTo = parts[1];
            n = parts[0];
        }

        // Extraer columna y fila destino (siempre los últimos 2 caracteres)
        if (n.length() < 2) return null;
        char colChar = n.charAt(n.length() - 2);
        char rowChar = n.charAt(n.length() - 1);

        if (colChar < 'a' || colChar > 'h') return null;
        if (rowChar < '1' || rowChar > '8') return null;

        int toCol = colChar - 'a';
        int toRow = rowChar - '1';

        // Determinar qué tipo de pieza se mueve
        boolean isPawn = Character.isLowerCase(n.charAt(0));
        String pieceSymbol = isPawn ? "" : String.valueOf(n.charAt(0));

        // Buscar la pieza origen en el tablero
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p == null || p.getColor() != turn) continue;
                if (!p.getSymbol().equals(pieceSymbol)) continue;
                if (p.canMoveTo(toRow, toCol, board)) {
                    Move move = new Move(r, c, toRow, toCol, notation);
                    move.promotionPiece = promotionTo;
                    return move;
                }
            }
        }

        return null; // movimiento inválido
    }

    private static Move tryParseLongAlgebraic(String n, String originalNotation,
            Piece[][] board, Piece.Color turn) {
        if (n.length() != 4 && n.length() != 5) return null;

        String lower = n.toLowerCase();
        char fc = lower.charAt(0);
        char fr = lower.charAt(1);
        char tc = lower.charAt(2);
        char tr = lower.charAt(3);
        if (fc < 'a' || fc > 'h' || fr < '1' || fr > '8') return null;
        if (tc < 'a' || tc > 'h' || tr < '1' || tr > '8') return null;

        int fromCol = fc - 'a';
        int fromRow = fr - '1';
        int toCol   = tc - 'a';
        int toRow   = tr - '1';

        Piece p = board[fromRow][fromCol];
        if (p == null || p.getColor() != turn) return null;
        if (!p.canMoveTo(toRow, toCol, board)) return null;

        Move move = new Move(fromRow, fromCol, toRow, toCol, originalNotation);

        if (lower.length() == 5) {
            char promo = lower.charAt(4);
            if ("qrnb".indexOf(promo) < 0) return null;
            if (!(p instanceof Pawn)) return null;
            if (turn == Piece.Color.WHITE && toRow != 7) return null;
            if (turn == Piece.Color.BLACK && toRow != 0) return null;
            move.promotionPiece = String.valueOf(Character.toUpperCase(promo));
        }

        return move;
    }

    public int    getFromRow()       { return fromRow; }
    public int    getFromCol()       { return fromCol; }
    public int    getToRow()         { return toRow; }
    public int    getToCol()         { return toCol; }
    public String getAlgebraic()     { return algebraic; }
    public String getPromotionPiece(){ return promotionPiece; }

    @Override
    public String toString() { return algebraic; }
}
