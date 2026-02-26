package com.bizhub.controller;

import java.io.IOException;

import com.bizhub.model.services.common.service.AiDatabaseAssistantService;
import com.bizhub.model.services.common.service.AiNavigationBotService;
import com.bizhub.model.services.common.service.NavigationService;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * AI Chat Controller - Floating chat interface for the navigation bot.
 */
public class AiChatController {

    @FXML private VBox messagesContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField inputField;
    @FXML private Button chatButton;
    @FXML private Button minimizeButton;
    @FXML private VBox chatWindow;

    private AiNavigationBotService botService;
    private AiDatabaseAssistantService dbService;
    private Stage primaryStage;
    private static final int MAX_MESSAGES = 20; // Limit conversation history

    @FXML
    public void initialize() {
        botService = new AiNavigationBotService();
        dbService = new AiDatabaseAssistantService();

        // Handle Enter key in input field
        inputField.setOnAction(e -> onSendMessage());

        // Auto-scroll to bottom when messages change
        messagesContainer.heightProperty().addListener((obs, old, newVal) -> {
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        });
        
        // Make interactive elements non-transparent
        if (chatButton != null) chatButton.setMouseTransparent(false); // Button always clickable
        if (chatWindow != null) chatWindow.setMouseTransparent(true); // Window blocks when closed
        if (minimizeButton != null) minimizeButton.setMouseTransparent(false);
        if (inputField != null) inputField.setMouseTransparent(true);
        if (scrollPane != null) scrollPane.setMouseTransparent(true);
    }

    public void setStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    public void onSendMessage() {
        String userMessage = inputField.getText().trim();
        if (userMessage.isEmpty()) {
            return;
        }

        // Clear input field
        inputField.clear();

        // Add user message to chat
        addUserMessage(userMessage);

        // Process bot response asynchronously
        Task<AiNavigationBotService.BotResponse> responseTask = new Task<>() {
            @Override
            protected AiNavigationBotService.BotResponse call() {
                // Simulate small delay for natural feel
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {}
                return botService.processInput(userMessage);
            }
        };

        responseTask.setOnSucceeded(e -> Platform.runLater(() -> {
            AiNavigationBotService.BotResponse response = responseTask.getValue();

            // Handle database queries specially
            if (response.intent() == AiNavigationBotService.NavigationIntent.QUERY_DATABASE) {
                handleDatabaseQuery(userMessage);
                return;
            }

            addBotMessage(response.message());

            // If it's a navigation command, execute it after showing the message
            if (response.isNavigationCommand() && primaryStage != null) {
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(1000); // Give user time to read the response
                    } catch (InterruptedException ignored) {}

                    NavigationService navService = new NavigationService(primaryStage);
                    boolean success = botService.executeNavigation(response.intent(), navService);

                    if (!success) {
                        addBotMessage("Sorry, I couldn't navigate there. Please try again.");
                    }
                });
            }
        }));

        responseTask.setOnFailed(e -> Platform.runLater(() -> {
            addBotMessage("Sorry, something went wrong. Please try again!");
        }));

        new Thread(responseTask).start();
    }

    private void addUserMessage(String message) {
        // Clean up old messages to prevent memory issues
        cleanupOldMessages();
        
        HBox userMessageBox = new HBox();
        userMessageBox.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
        userMessageBox.setSpacing(8);

        VBox messageBubble = new VBox();
        messageBubble.setStyle("-fx-background-color: rgba(26, 51, 82, 0.98); -fx-background-radius: 12; -fx-padding: 12; -fx-max-width: 220; -fx-border-color: rgba(255, 184, 77, 0.35); -fx-border-width: 1.5; -fx-border-radius: 12;");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #FFFFFF;");

        messageBubble.getChildren().add(messageLabel);

        // Avatar (user)
        StackPane avatar = new StackPane();
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #FFB84D, #FFD54F); -fx-background-radius: 50%; -fx-min-width: 32; -fx-min-height: 32;");
        Label avatarLabel = new Label("👤");
        avatarLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #0A192F;");
        avatar.getChildren().add(avatarLabel);

        userMessageBox.getChildren().addAll(messageBubble, avatar);
        userMessageBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        // Add fade-in animation
        addFadeInAnimation(userMessageBox);

        messagesContainer.getChildren().add(userMessageBox);
    }

    private void addBotMessage(String message) {
        // Clean up old messages to prevent memory issues
        cleanupOldMessages();
        
        HBox botMessageBox = new HBox();
        botMessageBox.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        botMessageBox.setSpacing(8);

        // Avatar (bot) - with logo
        StackPane avatar = new StackPane();
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #FFB84D, #FFD54F); -fx-background-radius: 50%; -fx-min-width: 32; -fx-min-height: 32; -fx-max-width: 32; -fx-max-height: 32;");
        
        // Try to load logo, fallback to text if not found
        try {
            ImageView logoView = new ImageView(new Image(getClass().getResourceAsStream("/com/bizhub/images/site-images/logo.png")));
            logoView.setFitWidth(24);
            logoView.setFitHeight(24);
            logoView.setPreserveRatio(true);
            avatar.getChildren().add(logoView);
        } catch (Exception e) {
            // Fallback to text avatar if logo not found
            Label avatarLabel = new Label("🤖");
            avatarLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #0A192F;");
            avatar.getChildren().add(avatarLabel);
        }

        VBox messageBubble = new VBox();
        messageBubble.setStyle("-fx-background-color: rgba(26, 51, 82, 0.98); -fx-background-radius: 12; -fx-padding: 12; -fx-max-width: 220; -fx-border-color: rgba(255, 184, 77, 0.30); -fx-border-width: 1.5; -fx-border-radius: 12;");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #FFFFFF;");

        messageBubble.getChildren().add(messageLabel);

        botMessageBox.getChildren().addAll(avatar, messageBubble);

        // Add fade-in animation
        addFadeInAnimation(botMessageBox);

        messagesContainer.getChildren().add(botMessageBox);
    }

    private void cleanupOldMessages() {
        // Keep only the last MAX_MESSAGES to prevent memory issues and glitchy responses
        if (messagesContainer.getChildren().size() > MAX_MESSAGES) {
            int removeCount = messagesContainer.getChildren().size() - MAX_MESSAGES;
            messagesContainer.getChildren().remove(0, removeCount);
        }
    }

    private void addFadeInAnimation(HBox messageBox) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), messageBox);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    @FXML
    public void onOpenChat() {
        if (chatWindow == null || chatButton == null) return;
        chatWindow.setVisible(true);
        chatWindow.setManaged(true);
        chatButton.setVisible(false);
        chatButton.setManaged(false);
        inputField.requestFocus();
        
        // Make chat window interactive when open
        chatWindow.setMouseTransparent(false);
        inputField.setMouseTransparent(false);
        scrollPane.setMouseTransparent(false);
        minimizeButton.setMouseTransparent(false);
    }

    @FXML
    public void onMinimizeChat() {
        if (chatWindow == null || chatButton == null) return;
        chatWindow.setVisible(false);
        chatWindow.setManaged(false);
        chatButton.setVisible(true);
        chatButton.setManaged(true);
        
        // Make chat window non-interactive when minimized
        chatWindow.setMouseTransparent(true);
        inputField.setMouseTransparent(true);
        scrollPane.setMouseTransparent(true);
        minimizeButton.setMouseTransparent(true);
    }

    /**
     * Handle database queries using the AI database assistant.
     */
    private void handleDatabaseQuery(String query) {
        // Show loading message
        addBotMessage("🔍 Querying the database...");

        Task<AiDatabaseAssistantService.DatabaseQueryResult> dbTask = new Task<>() {
            @Override
            protected AiDatabaseAssistantService.DatabaseQueryResult call() {
                return dbService.processQuery(query);
            }
        };

        dbTask.setOnSucceeded(e -> Platform.runLater(() -> {
            AiDatabaseAssistantService.DatabaseQueryResult result = dbTask.getValue();
            addBotMessage(result.response());
        }));

        dbTask.setOnFailed(e -> Platform.runLater(() -> {
            addBotMessage("Sorry, I couldn't query the database. Please try again!");
        }));

        new Thread(dbTask).start();
    }

    /**
     * Static helper method to create and embed the AI chat component into a parent container.
     */
    public static StackPane createChatComponent(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(AiChatController.class.getResource("/com/bizhub/fxml/ai-chat.fxml"));
            StackPane chatComponent = loader.load();
            AiChatController controller = loader.getController();
            controller.setStage(stage);
            return chatComponent;
        } catch (IOException e) {
            System.err.println("Failed to load AI chat component: " + e.getMessage());
            return null;
        }
    }
}
