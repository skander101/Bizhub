package com.bizhub.model.services.common.service;

import java.io.IOException;

import com.bizhub.controller.AiChatController;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Helper class to easily embed the AI Navigation Bot as a floating widget
 * into any JavaFX page.
 */
public class AiChatWidget {

    /**
     * Creates a floating AI chat widget that can be added to any page.
     *
     * @param stage The primary stage for navigation
     * @return A StackPane containing the chat widget positioned at bottom-right
     */
    public static StackPane createFloatingWidget(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(AiChatWidget.class.getResource("/com/bizhub/fxml/ai-chat.fxml"));
            VBox chatComponent = loader.load();
            AiChatController controller = loader.getController();
            controller.setStage(stage);

            // Wrap in StackPane for positioning
            StackPane wrapper = new StackPane(chatComponent);
            wrapper.setStyle("-fx-background-color: transparent;");
            StackPane.setAlignment(chatComponent, javafx.geometry.Pos.BOTTOM_RIGHT);
            StackPane.setMargin(chatComponent, new javafx.geometry.Insets(0, 20, 20, 0));
            
            // Make wrapper non-blocking for mouse events
            wrapper.setMouseTransparent(true);
            chatComponent.setMouseTransparent(false);

            return wrapper;
        } catch (IOException e) {
            System.err.println("Failed to create AI chat widget: " + e.getMessage());
            return null;
        }
    }

    /**
     * Embeds the AI chat widget into an existing page layout.
     * The page should be a StackPane or have a StackPane as its root.
     *
     * @param pageRoot The root container of the page (typically a StackPane)
     * @param stage The primary stage for navigation
     */
    public static void embedInPage(Pane pageRoot, Stage stage) {
        if (pageRoot == null) {
            System.err.println("Cannot embed AI chat: page root is null");
            return;
        }

        StackPane chatWidget = createFloatingWidget(stage);
        if (chatWidget != null) {
            if (pageRoot instanceof StackPane stackPane) {
                // Add to existing StackPane
                stackPane.getChildren().add(chatWidget);
                chatWidget.toFront();
            } else {
                // Wrap existing content in StackPane
                Pane parent = (Pane) pageRoot.getParent();
                if (parent != null) {
                    int index = parent.getChildren().indexOf(pageRoot);
                    parent.getChildren().remove(pageRoot);

                    StackPane newRoot = new StackPane();
                    newRoot.getChildren().addAll(pageRoot, chatWidget);
                    parent.getChildren().add(index, newRoot);
                }
            }
        }
    }

    /**
     * Creates the AI chat widget for pages that already have a StackPane structure.
     * Call this from a controller's initialize() method.
     *
     * Example usage in a controller:
     * <pre>
     * @FXML private StackPane rootPane; // Your page root
     *
     * @FXML
     * public void initialize() {
     *     // ... existing init code ...
     *
     *     // Add AI chat widget
     *     Platform.runLater(() -> {
     *         Stage stage = (Stage) rootPane.getScene().getWindow();
         *         AiChatWidget.addToStackPane(rootPane, stage);
     *     });
     * }
     * </pre>
     */
    public static void addToStackPane(StackPane stackPane, Stage stage) {
        if (stackPane == null || stage == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(AiChatWidget.class.getResource("/com/bizhub/fxml/ai-chat.fxml"));
            StackPane chatComponent = loader.load();
            AiChatController controller = loader.getController();
            controller.setStage(stage);

            // Position at bottom-right
            StackPane.setAlignment(chatComponent, javafx.geometry.Pos.BOTTOM_RIGHT);
            StackPane.setMargin(chatComponent, new javafx.geometry.Insets(0, 0, 0, 0));

            // Don't set mouse transparency - let individual elements control it
            stackPane.getChildren().add(chatComponent);
            chatComponent.toFront();
        } catch (IOException e) {
            System.err.println("Failed to add AI chat widget: " + e.getMessage());
        }
    }
}
