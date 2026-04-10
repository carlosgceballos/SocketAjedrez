package chess.pieces;

public class Bishop extends Piece {

    public Bishop(Color color, int row, int col) {
        super(color, row, col);
    }

    @Override
    public boolean canMoveTo(int toRow, int toCol, Piece[][] board) {
        int rowDiff = Math.abs(toRow - row);
        int colDiff = Math.abs(toCol - col);
        return rowDiff == colDiff
            && pathClear(toRow, toCol, board)
            && destinationFree(toRow, toCol, board);
    }

    @Override public String getSymbol()  { return "B"; }
    @Override public String getDisplay() { return isWhite() ? "B" : "b"; }
}
