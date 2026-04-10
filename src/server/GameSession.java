package server;

import chess.Board;
import chess.GameState;
import chess.Move;
import chess.pieces.Piece;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import db.DbComponent;
import db.PostgresAdapter;

public class GameSession implements Runnable {

    private static final String CONFIG_PATH  = "config.json";
    private static final String QUERIES_PATH = "src/queries.json";

    private final Socket       whiteSocket;
    private final Socket       blackSocket;
    private PrintWriter        whiteOut;
    private PrintWriter        blackOut;
    private BufferedReader     whiteIn;
    private BufferedReader     blackIn;

    private final Board        board;
    private final GameState    state;
    private DbComponent<PostgresAdapter> db;
    private int                gameId = -1;

    public GameSession(Socket whiteSocket, Socket blackSocket) {
        this.whiteSocket = whiteSocket;
        this.blackSocket = blackSocket;
        this.board       = new Board();
        this.state       = new GameState();
    }

    @Override
    public void run() {
        try {
            setupStreams();
            setupDatabase();
            notifyStart();
            gameLoop();
            saveResult();
        } catch (IOException e) {
            System.err.println("Error en sesion de juego: " + e.getMessage());
        } finally {
            if (db != null) db.shutdown();
            closeConnections();
        }
    }

    private void setupStreams() throws IOException {
        whiteOut = new PrintWriter(whiteSocket.getOutputStream(), true);
        blackOut = new PrintWriter(blackSocket.getOutputStream(), true);
        whiteIn  = new BufferedReader(new InputStreamReader(whiteSocket.getInputStream()));
        blackIn  = new BufferedReader(new InputStreamReader(blackSocket.getInputStream()));
    }

    private void setupDatabase() {
        try {
            // Leer credenciales del config.json igual que en el proyecto anterior
            JsonObject config = loadConfig(CONFIG_PATH);
            JsonObject pg     = config.getAsJsonObject("postgres");

            String url      = pg.get("url").getAsString();
            String user     = pg.get("user").getAsString();
            String password = pg.get("password").getAsString();

            db = new DbComponent<>(
                PostgresAdapter.class,
                url, user, password,
                QUERIES_PATH
            );

            Map<String, Object> params = new HashMap<>();
            params.put("blancas", whiteSocket.getInetAddress().getHostAddress());
            params.put("negras",  blackSocket.getInetAddress().getHostAddress());

            List<Map<String, Object>> result =
                (List<Map<String, Object>>) db.query("createGame", params);

            if (result != null && !result.isEmpty()) {
                gameId = ((Number) result.get(0).get("id")).intValue();
                System.out.println("Partida registrada con id: " + gameId);
            }
        } catch (Exception e) {
            System.err.println("Error al conectar con la base de datos: " + e.getMessage());
        }
    }

    private JsonObject loadConfig(String path) {
        try (FileReader reader = new FileReader(path)) {
            return new Gson().fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            System.err.println("Error al cargar config.json: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private void notifyStart() {
        whiteOut.println("STATUS|START|Partida iniciada. Juegas con BLANCAS.");
        blackOut.println("STATUS|START|Partida iniciada. Juegas con NEGRAS.");
        broadcast("BOARD|" + serializeBoard());
        broadcast("TURN|WHITE");
    }

    private void gameLoop() throws IOException {
        while (!state.isOver()) {
            Piece.Color    turn      = state.getCurrentTurn();
            PrintWriter    current   = turn == Piece.Color.WHITE ? whiteOut : blackOut;
            BufferedReader currentIn = turn == Piece.Color.WHITE ? whiteIn  : blackIn;
            PrintWriter    waiting   = turn == Piece.Color.WHITE ? blackOut : whiteOut;

            current.println("INPUT|Tu turno (" + turn + "). Ingresa movimiento:");
            waiting.println("STATUS|WAITING|Esperando movimiento del rival...");

            String input = currentIn.readLine();
            if (input == null) {
                broadcast("STATUS|DISCONNECT|El rival se desconectó.");
                break;
            }

            input = input.trim();

            if (input.equalsIgnoreCase("RESIGN")) {
                state.resign(turn);
                broadcast("STATUS|RESIGNED|" + state.getStatusMessage());
                break;
            }

            Move move = Move.fromAlgebraic(input, board.getGrid(), turn);

            if (move == null) {
                current.println("ERROR|Movimiento inválido: " + input);
                continue;
            }

            if (board.wouldLeaveKingInCheck(
                    move.getFromRow(), move.getFromCol(),
                    move.getToRow(),   move.getToCol(), turn)) {
                current.println("ERROR|Movimiento ilegal: dejarías tu rey en jaque.");
                continue;
            }

            String notation = move.getAlgebraic().replaceAll("[+#]", "");
            if (notation.equals("O-O") && !board.canCastle(turn, true)) {
                current.println("ERROR|Enroque corto no permitido en este momento.");
                continue;
            }
            if (notation.equals("O-O-O") && !board.canCastle(turn, false)) {
                current.println("ERROR|Enroque largo no permitido en este momento.");
                continue;
            }

            board.applyMove(move);
            state.update(board);

            saveMove(input, turn);

            broadcast("MOVE|" + input);
            broadcast("BOARD|" + serializeBoard());
            broadcast("STATUS|" + state.getStatus() + "|" + state.getStatusMessage());

            if (!state.isOver()) {
                broadcast("TURN|" + state.getCurrentTurn());
            }
        }
    }

    private void saveMove(String notation, Piece.Color color) {
        if (db == null || gameId == -1) return;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("partida_id", gameId);
            params.put("turno",      state.getTurnNumber());
            params.put("color",      color.toString());
            params.put("movimiento", notation);
            params.put("en_jaque",   state.getStatus() == GameState.Status.CHECK);
            db.query("saveMove", params);
        } catch (Exception e) {
            System.err.println("Error guardando movimiento: " + e.getMessage());
        }
    }

    private void saveResult() {
        if (db == null || gameId == -1) return;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("id",        gameId);
            params.put("ganador",   state.getWinner() != null ? state.getWinner().toString() : "DRAW");
            params.put("resultado", state.getStatus().toString());
            params.put("turnos",    state.getTurnNumber());
            db.query("endGame", params);
            System.out.println("Resultado guardado para partida " + gameId);
        } catch (Exception e) {
            System.err.println("Error guardando resultado: " + e.getMessage());
        }
    }

    private String serializeBoard() {
        StringBuilder sb = new StringBuilder();
        for (int r = 7; r >= 0; r--) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                sb.append(p == null ? "." : p.getColor() + "_" + p.getClass().getSimpleName());
                if (c < 7) sb.append(";");
            }
            if (r > 0) sb.append(",");
        }
        return sb.toString();
    }

    private void broadcast(String message) {
        whiteOut.println(message);
        blackOut.println(message);
    }

    private void closeConnections() {
        try { if (!whiteSocket.isClosed()) whiteSocket.close(); } catch (IOException e) { }
        try { if (!blackSocket.isClosed()) blackSocket.close(); } catch (IOException e) { }
        System.out.println("Sesión " + gameId + " finalizada.");
    }
}
