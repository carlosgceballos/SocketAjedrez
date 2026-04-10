package chess.pieces;

public class Pawn extends Piece {

    private boolean hasMoved       = false;
    private boolean enPassantReady = false;

    public Pawn(Color color, int row, int col) {
        super(color, row, col);
    }

    @Override
    public boolean canMoveTo(int toRow, int toCol, Piece[][] board) {
        int direction = isWhite() ? 1 : -1;
        int rowDiff   = toRow - row;
        int colDiff   = Math.abs(toCol - col);

        // Avance simple
        if (colDiff == 0 && rowDiff == direction && board[toRow][toCol] == null)
            return true;

        // Avance doble desde posición inicial
        if (colDiff == 0 && rowDiff == 2 * direction && !hasMoved
                && board[row + direction][col] == null
                && board[toRow][toCol] == null)
            return true;

        // Captura diagonal
        if (colDiff == 1 && rowDiff == direction) {
            Piece target = board[toRow][toCol];
            if (target != null && target.getColor() != this.color) return true;

            // En passant
            Piece adjacent = board[row][toCol];
            if (adjacent instanceof Pawn && adjacent.getColor() != this.color
                    && ((Pawn) adjacent).isEnPassantReady())
                return true;
        }

        return false;
    }

    public boolean hasMoved()                      { return hasMoved; }
    public void    markMoved()                     { hasMoved = true; }
    public boolean isEnPassantReady()              { return enPassantReady; }
    public void    setEnPassantReady(boolean v)    { enPassantReady = v; }
    public boolean canPromote() {
        return (isWhite() && row == 7) || (isBlack() && row == 0);
    }

    @Override public String getSymbol()  { return ""; }
    @Override public String getDisplay() { return isWhite() ? "P" : "p"; }
}
