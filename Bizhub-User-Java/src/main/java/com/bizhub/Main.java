package com.bizhub;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL fxml = Main.class.getResource("/com/bizhub/fxml/login.fxml");
        if (fxml == null) {
            throw new IllegalStateException("Cannot find /com/bizhub/fxml/login.fxml on classpath");
        }

        Parent initial = FXMLLoader.load(fxml);

        // Scene shell: holds current page + a top overlay (logo + loading bar) for smooth transitions.
        StackPane appShell = new StackPane();
        appShell.setId("appShell");
        appShell.getChildren().add(initial);

        URL overlayFxml = Main.class.getResource("/com/bizhub/fxml/loading-overlay.fxml");
        if (overlayFxml == null) {
            throw new IllegalStateException("Cannot find /com/bizhub/fxml/loading-overlay.fxml on classpath");
        }
        Parent overlay = FXMLLoader.load(overlayFxml);
        overlay.setId("navOverlay");
        appShell.getChildren().add(overlay);

        var bounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(appShell, Math.max(980, bounds.getWidth()), Math.max(640, bounds.getHeight()));

        // Apply global theme at Scene level to prevent white flashes during navigation swaps.
        URL themeCss = Main.class.getResource("/com/bizhub/css/theme.css");
        if (themeCss != null) {
            scene.getStylesheets().add(themeCss.toExternalForm());
        }

        stage.setTitle("BizHub - Users & Reviews Administration");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);

        // Full screen
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
