package chess.pieces;

public class King extends Piece {

    private boolean hasMoved = false;

    public King(Color color, int row, int col) {
        super(color, row, col);
    }

    @Override
    public boolean canMoveTo(int toRow, int toCol, Piece[][] board) {
        int rowDiff = Math.abs(toRow - row);
        int colDiff = Math.abs(toCol - col);
        return rowDiff <= 1 && colDiff <= 1
            && !(rowDiff == 0 && colDiff == 0)
            && destinationFree(toRow, toCol, board);
    }

    public boolean hasMoved() { return hasMoved; }
    public void markMoved()   { hasMoved = true; }

    @Override public String getSymbol()  { return "K"; }
    @Override public String getDisplay() { return isWhite() ? "K" : "k"; }
}
