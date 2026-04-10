import chess.Board;
import chess.GameState;
import chess.Move;
import chess.pieces.Piece;

public class App {

    public static void main(String[] args) {
        System.out.println("=== Test del motor de ajedrez ===\n");

        Board     board = new Board();
        GameState state = new GameState();

        System.out.println("Tablero inicial:");
        System.out.println(board.display());

        // Secuencia de movimientos de prueba
        String[] moves = {
            "e4",    // peón blanco e2→e4
            "e5",    // peón negro e7→e5
            "Nf3",   // caballo blanco g1→f3
            "Nc6",   // caballo negro b8→c6
            "Bb5",   // alfil blanco f1→b5 (apertura Ruy López)
            "a6",    // peón negro a7→a6
            "O-O",   // enroque corto blancas
        };

        for (String notation : moves) {
            Piece.Color turn = state.getCurrentTurn();
            System.out.println("Turno " + state.getTurnNumber() + " (" + turn + "): " + notation);

            Move move = Move.fromAlgebraic(notation, board.getGrid(), turn);

            if (move == null) {
                System.out.println("  [ERROR] Movimiento inválido: " + notation);
                continue;
            }

            if (board.wouldLeaveKingInCheck(
                    move.getFromRow(), move.getFromCol(),
                    move.getToRow(),   move.getToCol(), turn)) {
                System.out.println("  [ERROR] Movimiento ilegal: deja al rey en jaque");
                continue;
            }

            // Validar enroque
            String n = notation.replaceAll("[+#]", "");
            if (n.equals("O-O") && !board.canCastle(turn, true)) {
                System.out.println("  [ERROR] Enroque corto no permitido");
                continue;
            }
            if (n.equals("O-O-O") && !board.canCastle(turn, false)) {
                System.out.println("  [ERROR] Enroque largo no permitido");
                continue;
            }

            board.applyMove(move);
            state.update(board);

            System.out.println("  [OK] " + state.getStatusMessage());
        }

        System.out.println("\nTablero final:");
        System.out.println(board.display());

        // Probar detección de jaque
        System.out.println("=== Test de jaque ===");
        testCheck();

        System.out.println("\n=== Test completado ===");
    }

    private static void testCheck() {
        Board board = new Board();
        GameState state = new GameState();

        // Secuencia que lleva a jaque al rey negro
        // Scholar's mate (jaque en 4 movimientos)
        String[] scholarsMate = {
            "e4", "e5",
            "Bc4", "Nc6",
            "Qh5", "Nf6",
            "Qxf7"  // jaque mate del pastor
        };

        for (String notation : scholarsMate) {
            Piece.Color turn = state.getCurrentTurn();
            Move move = Move.fromAlgebraic(notation, board.getGrid(), turn);
            if (move == null) {
                System.out.println("Movimiento no reconocido: " + notation);
                continue;
            }
            board.applyMove(move);
            state.update(board);
            System.out.println(turn + " juega " + notation + " → " + state.getStatusMessage());

            if (state.isOver()) {
                System.out.println("Partida terminada: " + state.getStatusMessage());
                break;
            }
        }

        System.out.println("\nTablero final del test de jaque:");
        System.out.println(board.display());
    }
}