package chess.pieces;

public class Queen extends Piece {

    public Queen(Color color, int row, int col) {
        super(color, row, col);
    }

    @Override
    public boolean canMoveTo(int toRow, int toCol, Piece[][] board) {
        int rowDiff = Math.abs(toRow - row);
        int colDiff = Math.abs(toCol - col);
        boolean straightLine = (toRow == row || toCol == col);
        boolean diagonal     = (rowDiff == colDiff);
        return (straightLine || diagonal)
            && pathClear(toRow, toCol, board)
            && destinationFree(toRow, toCol, board);
    }

    @Override public String getSymbol()  { return "Q"; }
    @Override public String getDisplay() { return isWhite() ? "Q" : "q"; }
}
