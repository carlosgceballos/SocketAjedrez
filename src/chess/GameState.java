package chess;

import chess.pieces.Piece;
import chess.pieces.Piece.Color;

public class GameState {

    public enum Status {
        ONGOING,
        CHECK,
        CHECKMATE,
        STALEMATE,
        RESIGNED
    }

    private Piece.Color currentTurn = Piece.Color.WHITE;
    private Status status           = Status.ONGOING;
    private Piece.Color winner      = null;
    private int turnNumber          = 1;

    // Actualiza el estado después de cada movimiento
    public void update(Board board) {
        Piece.Color opponent = currentTurn == Piece.Color.WHITE
            ? Piece.Color.BLACK
            : Piece.Color.WHITE;

        if (board.isCheckmate(opponent)) {
            status = Status.CHECKMATE;
            winner = currentTurn;
        } else if (board.isStalemate(opponent)) {
            status = Status.STALEMATE;
        } else if (board.isInCheck(opponent)) {
            status = Status.CHECK;
            switchTurn();
        } else {
            status = Status.ONGOING;
            switchTurn();
        }
    }

    public void resign(Piece.Color resigningColor) {
        status = Status.RESIGNED;
        winner = resigningColor == Piece.Color.WHITE
            ? Piece.Color.BLACK
            : Piece.Color.WHITE;
    }

    private void switchTurn() {
        if (currentTurn == Piece.Color.BLACK) turnNumber++;
        currentTurn = currentTurn == Piece.Color.WHITE
            ? Piece.Color.BLACK
            : Piece.Color.WHITE;
    }

    public boolean isOver() {
        return status == Status.CHECKMATE
            || status == Status.STALEMATE
            || status == Status.RESIGNED;
    }

    public Piece.Color getCurrentTurn() { return currentTurn; }
    public Status      getStatus()      { return status; }
    public Piece.Color getWinner()      { return winner; }
    public int         getTurnNumber()  { return turnNumber; }

    public String getStatusMessage() {
        switch (status) {
            case CHECK:     return "¡Jaque!";
            case CHECKMATE: return "¡Jaque mate! Gana " + winner;
            case STALEMATE: return "Tablas por ahogado.";
            case RESIGNED:  return "Abandono. Gana " + winner;
            default:        return "Turno de " + currentTurn;
        }
    }
}
