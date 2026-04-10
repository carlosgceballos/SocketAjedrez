package chess.pieces;

public abstract class Piece {

    public enum Color { WHITE, BLACK }

    protected Color color;
    protected int row;
    protected int col;

    public Piece(Color color, int row, int col) {
        this.color = color;
        this.row   = row;
        this.col   = col;
    }

    // Cada pieza implementa sus propias reglas de movimiento
    public abstract boolean canMoveTo(int toRow, int toCol, Piece[][] board);

    // Letra de la pieza en notacion algebraica (K, Q, R, B, N peón es vacío)
    public abstract String getSymbol();

    // Representación visual en consola
    public abstract String getDisplay();

    public Color getColor()  { return color; }
    public int   getRow()    { return row; }
    public int   getCol()    { return col; }

    public void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public boolean isWhite() { return color == Color.WHITE; }
    public boolean isBlack() { return color == Color.BLACK; }

    // Verifica que el destino no esté ocupado por una pieza del mismo color
    protected boolean destinationFree(int toRow, int toCol, Piece[][] board) {
        Piece dest = board[toRow][toCol];
        return dest == null || dest.getColor() != this.color;
    }

    // Verifica que el camino en línea recta esté despejado (para torre, alfil, dama)
    protected boolean pathClear(int toRow, int toCol, Piece[][] board) {
        int rowStep = Integer.signum(toRow - row);
        int colStep = Integer.signum(toCol - col);
        int r = row + rowStep;
        int c = col + colStep;
        while (r != toRow || c != toCol) {
            if (board[r][c] != null) return false;
            r += rowStep;
            c += colStep;
        }
        return true;
    }

    @Override
    public String toString() {
        return color + " " + getClass().getSimpleName() + " at " + toAlgebraic(row, col);
    }

    public static String toAlgebraic(int row, int col) {
        return String.valueOf((char)('a' + col)) + (row + 1);
    }
}
