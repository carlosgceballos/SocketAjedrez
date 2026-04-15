package client;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;

public class ChessClient {

    private static final int PORT = 5000;

    private final Stage        stage;
    private Socket             socket;
    private PrintWriter        out;
    private BufferedReader     in;
    private ChessBoardView     boardView;
    private boolean            myTurn             = false;
    private boolean            drawResponseOpen   = false;

    public ChessClient(Stage stage) {
        this.stage = stage;
    }

    public void connect() {
        boardView = new ChessBoardView();

        boardView.setMoveCallback(move -> {
            if (myTurn) {
                sendMove(move);
            }
        });

        boardView.setActionCallbacks(
            this::onResignClicked,
            this::onOfferDrawClicked,
            this::onAcceptDrawClicked,
            this::onDeclineDrawClicked
        );

        Scene scene = new Scene(boardView, 580, 720);
        stage.setTitle("Ajedrez en Red");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        TextInputDialog dialog = new TextInputDialog("localhost");
        dialog.setTitle("Conectar al servidor");
        dialog.setHeaderText("Ingresa la IP del servidor");
        dialog.setContentText("IP:");

        Optional<String> result = dialog.showAndWait();
        String host = result.orElse("localhost").trim();

        boardView.setStatus("Conectando a " + host + "...");

        final String finalHost = host;
        new Thread(() -> {
            try {
                socket = new Socket(finalHost, PORT);
                out    = new PrintWriter(socket.getOutputStream(), true);
                in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Platform.runLater(() -> boardView.setStatus("Conectado — esperando rival..."));
                listenServer();
            } catch (IOException e) {
                Platform.runLater(() ->
                    boardView.setStatus("No se pudo conectar a " + finalHost + ":" + PORT)
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
                Platform.runLater(() -> handleStatus(parts));
                break;
            case "BOARD":
                boardView.updateBoard(parts[1]);
                break;
            case "TURN":
                Platform.runLater(() -> {
                    myTurn = parts[1].equals(boardView.getMyColor());
                    boardView.setTurn(parts[1]);
                    refreshPlayActions();
                });
                break;
            case "MOVE":
                Platform.runLater(() -> boardView.setStatus("Movimiento: " + parts[1]));
                break;
            case "INPUT":
                Platform.runLater(() -> {
                    myTurn = true;
                    boardView.setStatus("Tu turno — haz clic en una pieza");
                    refreshPlayActions();
                });
                break;
            case "DRAW_REQUEST":
                drawResponseOpen = true;
                Platform.runLater(() -> {
                    boardView.setDrawRequestMode(true);
                    boardView.setStatus("Te han ofrecido tablas.");
                    refreshPlayActions();
                });
                break;
            case "ERROR":
                Platform.runLater(() -> {
                    String err = parts.length > 1 ? parts[1] : "";
                    if (drawResponseOpen) {
                        showAlert("Respuesta inválida", err);
                    } else {
                        myTurn = true;
                        showAlert("Movimiento inválido", err);
                        refreshPlayActions();
                    }
                });
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
                boardView.setMatchStarted(true);
                boardView.setStatus("Partida iniciada — Juegas con " +
                    (color.equals("WHITE") ? "BLANCAS" : "NEGRAS"));
                refreshPlayActions();
                break;
            case "WAITING":
                boardView.setStatus(msg);
                myTurn = false;
                refreshPlayActions();
                break;
            case "DRAW_WAIT":
                myTurn = false;
                boardView.setStatus(msg);
                refreshPlayActions();
                break;
            case "CHECK":
                boardView.setStatus("¡JAQUE! " + msg);
                break;
            case "CHECKMATE":
            case "STALEMATE":
            case "RESIGNED":
            case "AGREED_DRAW":
            case "DISCONNECT":
                myTurn = false;
                drawResponseOpen = false;
                boardView.setMatchStarted(false);
                boardView.setDrawRequestMode(false);
                boardView.setStatus(msg);
                refreshPlayActions();
                showAlert("Partida finalizada", msg);
                break;
            case "DRAW_DECLINED":
                drawResponseOpen = false;
                Platform.runLater(() -> {
                    boardView.setDrawRequestMode(false);
                    boardView.setStatus(msg);
                });
                break;
            default:
                if (!msg.isEmpty()) boardView.setStatus(msg);
        }
    }

    private void refreshPlayActions() {
        boolean enable = boardView.isMatchStarted() && myTurn && !drawResponseOpen;
        boardView.setPlayActionsEnabled(enable);
    }

    private void sendMove(String move) {
        if (out != null) {
            myTurn = false;
            out.println(move);
            boardView.setStatus("Movimiento enviado: " + move);
            refreshPlayActions();
        }
    }

    private void sendLine(String line) {
        if (out != null) {
            out.println(line);
        }
    }

    private void onResignClicked() {
        if (!boardView.isMatchStarted() || !myTurn) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Rendirse");
        alert.setHeaderText(null);
        alert.setContentText("¿Seguro que quieres abandonar la partida?");
        Optional<ButtonType> r = alert.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            myTurn = false;
            sendLine("RESIGN");
            boardView.setStatus("Te has rendido.");
            refreshPlayActions();
        }
    }

    private void onOfferDrawClicked() {
        if (!boardView.isMatchStarted() || !myTurn) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ofrecer tablas");
        alert.setHeaderText(null);
        alert.setContentText("¿Ofrecer tablas al rival?");
        Optional<ButtonType> r = alert.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            myTurn = false;
            sendLine("OFFER_DRAW");
            refreshPlayActions();
        }
    }

    private void onAcceptDrawClicked() {
        if (!drawResponseOpen) return;
        drawResponseOpen = false;
        boardView.setDrawRequestMode(false);
        sendLine("ACCEPT_DRAW");
        refreshPlayActions();
    }

    private void onDeclineDrawClicked() {
        if (!drawResponseOpen) return;
        drawResponseOpen = false;
        boardView.setDrawRequestMode(false);
        sendLine("DECLINE_DRAW");
        refreshPlayActions();
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
