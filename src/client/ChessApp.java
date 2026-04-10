package client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChessApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        ChessClient client = new ChessClient(stage);
        client.connect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
