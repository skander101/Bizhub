package com.bizhub.controller;

import com.bizhub.controller.users_avis.user.SidebarController;
import com.bizhub.controller.users_avis.user.TopbarProfileHelper;
import com.bizhub.model.services.common.service.AiDatabaseAssistantService;
import com.bizhub.model.services.common.service.AiNavigationBotService;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.NavigationService;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * AI Chat Controller - Standalone chat page for the navigation bot.
 */
public class AiChatController {

    @FXML private SidebarController sidebarController;

    @FXML private VBox messagesContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private BorderPane root;
    @FXML private HBox topbar;

    private AiNavigationBotService botService;
    private AiDatabaseAssistantService dbService;
    private Stage primaryStage;
    private static final int MAX_MESSAGES = 50; // Limit conversation history

    private static final int MAX_AI_HISTORY_TURNS = 10; // 128K context allows generous history
    private final java.util.ArrayDeque<AiDatabaseAssistantService.ChatMessage> dbChatHistory = new java.util.ArrayDeque<>();

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("ai-chat");
        botService = new AiNavigationBotService();
        dbService = new AiDatabaseAssistantService();

        // Add user profile to topbar
        if (topbar != null) {
            topbar.getChildren().add(TopbarProfileHelper.createProfileBox());
        }


        // Handle Enter key in input field
        inputField.setOnAction(e -> onSendMessage());

        // Auto-scroll to bottom when messages change
        messagesContainer.heightProperty().addListener((obs, old, newVal) ->
            Platform.runLater(() -> scrollPane.setVvalue(1.0))
        );

        // Get stage reference
        Platform.runLater(() -> {
            if (inputField != null && inputField.getScene() != null) {
                primaryStage = (Stage) inputField.getScene().getWindow();
            }
        });
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

        responseTask.setOnFailed(e -> Platform.runLater(() ->
            addBotMessage("Sorry, something went wrong. Please try again!")
        ));

        new Thread(responseTask).start();
    }

    @FXML
    public void clearChat() {
        // Keep only the first welcome message
        if (messagesContainer.getChildren().size() > 1) {
            messagesContainer.getChildren().remove(1, messagesContainer.getChildren().size());
        }
    }

    private void addUserMessage(String message) {
        cleanupOldMessages();
        
        HBox userMessageBox = new HBox();
        userMessageBox.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
        userMessageBox.setSpacing(10);

        VBox messageBubble = new VBox();
        messageBubble.setStyle("-fx-background-color: rgba(255, 184, 77, 0.15); -fx-background-radius: 12; -fx-padding: 14; -fx-max-width: 500; -fx-border-color: rgba(255, 184, 77, 0.35); -fx-border-width: 1.5; -fx-border-radius: 12;");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFFFFF;");

        messageBubble.getChildren().add(messageLabel);

        // Avatar (user)
        StackPane avatar = new StackPane();
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #FFB84D, #FFD54F); -fx-background-radius: 50%; -fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36;");
        Label avatarLabel = new Label("👤");
        avatarLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #0A192F;");
        avatar.getChildren().add(avatarLabel);

        userMessageBox.getChildren().addAll(messageBubble, avatar);
        userMessageBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        addFadeInAnimation(userMessageBox);
        messagesContainer.getChildren().add(userMessageBox);
    }

    private void addBotMessage(String message) {
        cleanupOldMessages();
        
        HBox botMessageBox = new HBox();
        botMessageBox.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        botMessageBox.setSpacing(10);

        // Avatar (bot) - with logo
        StackPane avatar = new StackPane();
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #FFB84D, #FFD54F); -fx-background-radius: 50%; -fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36;");

        try {
            ImageView logoView = new ImageView(new Image(getClass().getResourceAsStream("/com/bizhub/images/site-images/logo.png")));
            logoView.setFitWidth(28);
            logoView.setFitHeight(28);
            logoView.setPreserveRatio(true);
            avatar.getChildren().add(logoView);
        } catch (Exception e) {
            Label avatarLabel = new Label("🤖");
            avatarLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #0A192F;");
            avatar.getChildren().add(avatarLabel);
        }

        VBox messageBubble = new VBox();
        messageBubble.setStyle("-fx-background-color: rgba(26, 51, 82, 0.98); -fx-background-radius: 12; -fx-padding: 14; -fx-max-width: 500; -fx-border-color: rgba(255, 184, 77, 0.30); -fx-border-width: 1.5; -fx-border-radius: 12;");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFFFFF;");

        messageBubble.getChildren().add(messageLabel);
        botMessageBox.getChildren().addAll(avatar, messageBubble);

        addFadeInAnimation(botMessageBox);
        messagesContainer.getChildren().add(botMessageBox);
    }

    private void cleanupOldMessages() {
        if (messagesContainer.getChildren().size() > MAX_MESSAGES) {
            int removeCount = messagesContainer.getChildren().size() - MAX_MESSAGES;
            messagesContainer.getChildren().remove(1, removeCount + 1); // Keep welcome message
        }
    }

    private void addFadeInAnimation(HBox messageBox) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), messageBox);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void handleDatabaseQuery(String query) {
        addBotMessage("🔍 Querying the database...");

        // Add current user query to history (we'll add assistant reply on success)
        appendDbHistory(new AiDatabaseAssistantService.ChatMessage("user", query));

        // Snapshot last N history messages to keep request small
        java.util.List<AiDatabaseAssistantService.ChatMessage> historySnapshot = getDbHistorySnapshot();

        Task<AiDatabaseAssistantService.DatabaseQueryResult> dbTask = new Task<>() {
            @Override
            protected AiDatabaseAssistantService.DatabaseQueryResult call() {
                return dbService.processQuery(query, historySnapshot);
            }
        };

        dbTask.setOnSucceeded(e -> Platform.runLater(() -> {
            AiDatabaseAssistantService.DatabaseQueryResult result = dbTask.getValue();
            addBotMessage(result.response());
            appendDbHistory(new AiDatabaseAssistantService.ChatMessage("assistant", result.response()));
        }));

        dbTask.setOnFailed(e -> Platform.runLater(() ->
            addBotMessage("Sorry, I couldn't query the database. Please try again!")
        ));

        new Thread(dbTask).start();
    }

    private void appendDbHistory(AiDatabaseAssistantService.ChatMessage msg) {
        if (msg == null) return;
        dbChatHistory.addLast(msg);

        // Keep only last MAX_AI_HISTORY_TURNS * 2 messages (user+assistant pairs)
        int maxMessages = MAX_AI_HISTORY_TURNS * 2;
        while (dbChatHistory.size() > maxMessages) {
            dbChatHistory.removeFirst();
        }
    }

    private java.util.List<AiDatabaseAssistantService.ChatMessage> getDbHistorySnapshot() {
        // Return a copy; we exclude the last user message because we add it separately as the current question.
        java.util.ArrayList<AiDatabaseAssistantService.ChatMessage> list = new java.util.ArrayList<>(dbChatHistory);
        if (!list.isEmpty()) {
            AiDatabaseAssistantService.ChatMessage last = list.get(list.size() - 1);
            if (last != null && "user".equals(last.role())) {
                list.remove(list.size() - 1);
            }
        }
        return java.util.List.copyOf(list);
    }
}
