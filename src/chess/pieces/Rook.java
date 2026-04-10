package chess.pieces;

public class Rook extends Piece {

    private boolean hasMoved = false;

    public Rook(Color color, int row, int col) {
        super(color, row, col);
    }

    @Override
    public boolean canMoveTo(int toRow, int toCol, Piece[][] board) {
        boolean straightLine = (toRow == row || toCol == col);
        return straightLine
            && pathClear(toRow, toCol, board)
            && destinationFree(toRow, toCol, board);
    }

    public boolean hasMoved() { return hasMoved; }
    public void markMoved()   { hasMoved = true; }

    @Override public String getSymbol()  { return "R"; }
    @Override public String getDisplay() { return isWhite() ? "R" : "r"; }
}
