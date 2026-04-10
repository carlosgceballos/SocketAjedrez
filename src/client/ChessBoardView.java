package client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class ChessBoardView extends VBox {

    private static final int    CELL_SIZE   = 70;
    private static final Color  LIGHT       = Color.rgb(240, 217, 181);
    private static final Color  DARK        = Color.rgb(181, 136, 99);
    private static final Color  HIGHLIGHT   = Color.rgb(186, 202, 68, 0.8);
    private static final Color  SELECTED    = Color.rgb(246, 246, 105, 0.8);

    private final GridPane      grid        = new GridPane();
    private final Label         statusLabel = new Label("Conectando...");
    private final Label         turnLabel   = new Label();
    private StackPane[][]       cells       = new StackPane[8][8];

    // Callback cuando el jugador hace clic en una celda
    private MoveCallback        moveCallback;
    private int                 selectedRow = -1;
    private int                 selectedCol = -1;
    private String              myColor     = null;
    private String[][]          currentBoard; // estado actual del tablero

    public interface MoveCallback {
        void onMove(String algebraic);
    }

    public ChessBoardView() {
        buildBoard();
        buildStatusBar();

        setSpacing(8);
        setPadding(new Insets(16));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #2b2b2b;");
    }

    private void buildBoard() {
        // Etiquetas de columnas (a-h)
        GridPane colLabels = new GridPane();
        for (int c = 0; c < 8; c++) {
            Label lbl = new Label(String.valueOf((char)('a' + c)));
            lbl.setMinWidth(CELL_SIZE);
            lbl.setAlignment(Pos.CENTER);
            lbl.setTextFill(Color.LIGHTGRAY);
            lbl.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
            colLabels.add(lbl, c + 1, 0);
        }

        // Construir celdas del tablero
        for (int r = 7; r >= 0; r--) {
            // Etiqueta de fila
            Label rowLbl = new Label(String.valueOf(r + 1));
            rowLbl.setMinWidth(20);
            rowLbl.setAlignment(Pos.CENTER);
            rowLbl.setTextFill(Color.LIGHTGRAY);
            rowLbl.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
            grid.add(rowLbl, 0, 7 - r);

            for (int c = 0; c < 8; c++) {
                StackPane cell = createCell(r, c);
                cells[r][c] = cell;
                grid.add(cell, c + 1, 7 - r);
            }

            // Etiqueta de fila derecha
            Label rowLblR = new Label(String.valueOf(r + 1));
            rowLblR.setMinWidth(20);
            rowLblR.setAlignment(Pos.CENTER);
            rowLblR.setTextFill(Color.LIGHTGRAY);
            rowLblR.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
            grid.add(rowLblR, 9, 7 - r);
        }

        // Etiquetas de columnas abajo
        for (int c = 0; c < 8; c++) {
            Label lbl = new Label(String.valueOf((char)('a' + c)));
            lbl.setMinWidth(CELL_SIZE);
            lbl.setAlignment(Pos.CENTER);
            lbl.setTextFill(Color.LIGHTGRAY);
            lbl.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
            grid.add(lbl, c + 1, 8);
        }

        getChildren().add(grid);
    }

    private StackPane createCell(int row, int col) {
        StackPane cell = new StackPane();
        cell.setMinSize(CELL_SIZE, CELL_SIZE);
        cell.setMaxSize(CELL_SIZE, CELL_SIZE);

        Rectangle bg = new Rectangle(CELL_SIZE, CELL_SIZE);
        bg.setFill((row + col) % 2 == 0 ? DARK : LIGHT);
        cell.getChildren().add(bg);

        // Clic en celda
        final int r = row, c = col;
        cell.setOnMouseClicked(e -> handleCellClick(r, c));

        return cell;
    }

    private void buildStatusBar() {
        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        statusLabel.setAlignment(Pos.CENTER);

        turnLabel.setTextFill(Color.LIGHTGRAY);
        turnLabel.setFont(Font.font("Monospace", 12));
        turnLabel.setAlignment(Pos.CENTER);

        getChildren().addAll(statusLabel, turnLabel);
    }

    private void handleCellClick(int row, int col) {
        if (moveCallback == null || myColor == null) return;

        // Verificar que sea el turno del jugador
        String turnText = turnLabel.getText();
        if (!turnText.contains(myColor)) return;

        if (selectedRow == -1) {
            // Primera selección — elegir pieza
            if (currentBoard != null && currentBoard[7 - row] != null) {
                String cell = currentBoard[7 - row][col];
                if (!cell.equals(".") && cell.startsWith(myColor)) {
                    selectedRow = row;
                    selectedCol = col;
                    highlightCell(row, col, SELECTED);
                }
            }
        } else {
            // Segunda selección — mover
            String from = toAlgebraic(selectedRow, selectedCol);
            String to   = toAlgebraic(row, col);

            // Construir notación algebraica simplificada
            String move = buildMove(selectedRow, selectedCol, row, col);

            clearHighlights();
            selectedRow = -1;
            selectedCol = -1;

            if (move != null) {
                moveCallback.onMove(move);
            }
        }
    }

    // Construye la notación algebraica del movimiento
    private String buildMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (currentBoard == null) return null;

        String pieceCell = currentBoard[7 - fromRow][fromCol];
        if (pieceCell.equals(".")) return null;

        String[] parts = pieceCell.split("_");
        if (parts.length < 2) return null;
        String pieceName = parts[1]; // King, Queen, Rook, etc.

        String to = toAlgebraic(toRow, toCol);

        // Enroque
        if (pieceName.equals("King")) {
            if (fromCol == 4 && toCol == 6) return "O-O";
            if (fromCol == 4 && toCol == 2) return "O-O-O";
        }

        // Captura
        String targetCell = currentBoard[7 - toRow][toCol];
        boolean isCapture = !targetCell.equals(".");

        if (pieceName.equals("Pawn")) {
            if (isCapture) {
                return (char)('a' + fromCol) + "x" + to;
            }
            return to;
        }

        String symbol = pieceSymbol(pieceName);
        return symbol + (isCapture ? "x" : "") + to;
    }

    private String pieceSymbol(String name) {
        switch (name) {
            case "King":   return "K";
            case "Queen":  return "Q";
            case "Rook":   return "R";
            case "Bishop": return "B";
            case "Knight": return "N";
            default:       return "";
        }
    }

    private String toAlgebraic(int row, int col) {
        return String.valueOf((char)('a' + col)) + (row + 1);
    }

    private void highlightCell(int row, int col, Color color) {
        StackPane cell = cells[row][col];
        Rectangle highlight = new Rectangle(CELL_SIZE, CELL_SIZE);
        highlight.setFill(color);
        highlight.setId("highlight");
        cell.getChildren().add(highlight);
    }

    private void clearHighlights() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                cells[r][c].getChildren().removeIf(n -> "highlight".equals(n.getId()));
    }

    // Actualiza el tablero con el estado recibido del servidor
    // Formato: "COLOR_Pieza;...,...,..." fila8 a fila1
    public void updateBoard(String serialized) {
        Platform.runLater(() -> {
            String[] rows = serialized.split(",");
            currentBoard = new String[8][];

            for (int displayRow = 0; displayRow < 8; displayRow++) {
                String[] cellData = rows[displayRow].split(";");
                currentBoard[displayRow] = cellData;
                int boardRow = 7 - displayRow; // convertir a índice interno

                for (int c = 0; c < 8 && c < cellData.length; c++) {
                    updateCell(boardRow, c, cellData[c]);
                }
            }
        });
    }

    private void updateCell(int row, int col, String pieceData) {
        StackPane cell = cells[row][col];

        // Limpiar contenido anterior (mantener fondo)
        cell.getChildren().removeIf(n -> n instanceof Text || "highlight".equals(n.getId()));

        if (!pieceData.equals(".")) {
            String[] parts = pieceData.split("_");
            if (parts.length >= 2) {
                boolean isWhite = parts[0].equals("WHITE");
                String symbol   = getPieceSymbol(parts[1], isWhite);

                Text pieceText = new Text(symbol);
                pieceText.setFont(Font.font("Monospace", FontWeight.BOLD, 36));
                pieceText.setFill(isWhite ? Color.WHITE : Color.BLACK);

                // Sombra para contraste
                Text shadow = new Text(symbol);
                shadow.setFont(Font.font("Monospace", FontWeight.BOLD, 36));
                shadow.setFill(isWhite ? Color.DARKGRAY : Color.LIGHTGRAY);
                shadow.setTranslateX(1.5);
                shadow.setTranslateY(1.5);

                cell.getChildren().addAll(shadow, pieceText);
            }
        }
    }

    private String getPieceSymbol(String name, boolean white) {
        switch (name) {
            case "King":   return white ? "♔" : "♚";
            case "Queen":  return white ? "♕" : "♛";
            case "Rook":   return white ? "♖" : "♜";
            case "Bishop": return white ? "♗" : "♝";
            case "Knight": return white ? "♘" : "♞";
            case "Pawn":   return white ? "♙" : "♟";
            default:       return "?";
        }
    }

    public void setStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    public void setTurn(String color) {
        Platform.runLater(() -> {
            boolean isMyTurn = color.equals(myColor);
            turnLabel.setText(isMyTurn ? "Tu turno (" + color + ")" : "Turno del rival (" + color + ")");
            turnLabel.setTextFill(isMyTurn ? Color.LIGHTGREEN : Color.LIGHTGRAY);
        });
    }

    public void setMyColor(String color)          { this.myColor = color; }
    public void setMoveCallback(MoveCallback cb)  { this.moveCallback = cb; }

    public String getMyColor() { return myColor; }
}