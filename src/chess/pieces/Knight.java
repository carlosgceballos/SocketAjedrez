package chess.pieces;

public class Knight extends Piece {

    public Knight(Color color, int row, int col) {
        super(color, row, col);
    }

    @Override
    public boolean canMoveTo(int toRow, int toCol, Piece[][] board) {
        int rowDiff = Math.abs(toRow - row);
        int colDiff = Math.abs(toCol - col);
        return ((rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2))
            && destinationFree(toRow, toCol, board);
    }

    @Override public String getSymbol()  { return "N"; }
    @Override public String getDisplay() { return isWhite() ? "N" : "n"; }
}
