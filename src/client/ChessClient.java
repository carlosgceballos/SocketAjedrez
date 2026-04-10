package client;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;

public class ChessClient {

    private static final String HOST = "localhost";
    private static final int    PORT = 5000;

    private final Stage        stage;
    private Socket             socket;
    private PrintWriter        out;
    private BufferedReader     in;
    private ChessBoardView     boardView;
    private boolean            myTurn = false;

    public ChessClient(Stage stage) {
        this.stage = stage;
    }

    public void connect() {
        boardView = new ChessBoardView();

        // Callback de movimiento — envía al servidor cuando el jugador hace clic
        boardView.setMoveCallback(move -> {
            if (myTurn) {
                sendMove(move);
            }
        });

        Scene scene = new Scene(boardView, 560, 650);
        stage.setTitle("Ajedrez en Red");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        // Conectar al servidor en hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                socket = new Socket(HOST, PORT);
                out    = new PrintWriter(socket.getOutputStream(), true);
                in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                boardView.setStatus("Conectado — esperando rival...");
                listenServer();
            } catch (IOException e) {
                Platform.runLater(() ->
                    boardView.setStatus("No se pudo conectar al servidor")
                );
            }
        }).start();
    }

    private void listenServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line);
            }
        } catch (IOException e) {
            Platform.runLater(() -> boardView.setStatus("Conexión cerrada"));
        }
    }

    private void handleMessage(String message) {
        String[] parts = message.split("\\|", 3);
        String type    = parts[0];

        switch (type) {
            case "STATUS":
                handleStatus(parts);
                break;
            case "BOARD":
                boardView.updateBoard(parts[1]);
                break;
            case "TURN":
                myTurn = parts[1].equals(boardView.getMyColor());
                boardView.setTurn(parts[1]);
                break;
            case "MOVE":
                boardView.setStatus("Movimiento: " + parts[1]);
                break;
            case "INPUT":
                myTurn = true;
                boardView.setStatus("Tu turno — haz clic en una pieza");
                break;
            case "ERROR":
                myTurn = true; // permitir reintentar
                Platform.runLater(() -> showAlert("Movimiento inválido", parts[1]));
                break;
        }
    }

    private void handleStatus(String[] parts) {
        String status = parts.length > 1 ? parts[1] : "";
        String msg    = parts.length > 2 ? parts[2] : "";

        switch (status) {
            case "START":
                String color = msg.contains("BLANCAS") ? "WHITE" : "BLACK";
                boardView.setMyColor(color);
                boardView.setStatus("Partida iniciada — Juegas con " +
                    (color.equals("WHITE") ? "BLANCAS" : "NEGRAS"));
                break;
            case "WAITING":
                boardView.setStatus(msg);
                myTurn = false;
                break;
            case "CHECK":
                boardView.setStatus("¡JAQUE! " + msg);
                break;
            case "CHECKMATE":
            case "STALEMATE":
            case "RESIGNED":
            case "DISCONNECT":
                myTurn = false;
                boardView.setStatus(msg);
                Platform.runLater(() -> showAlert("Partida finalizada", msg));
                break;
            default:
                if (!msg.isEmpty()) boardView.setStatus(msg);
        }
    }

    private void sendMove(String move) {
        if (out != null) {
            myTurn = false;
            out.println(move);
            boardView.setStatus("Movimiento enviado: " + move);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public String getMyColor() {
        return boardView.getMyColor();
    }
}
