package com.bizhub.controller.investissement;

import com.bizhub.model.investissement.*;
import com.bizhub.model.services.investissement.*;
import com.bizhub.model.services.investissement.AI.OpenRouterService;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.users_avis.user.User;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import com.bizhub.controller.users_avis.user.SidebarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NegotiationController {

    @FXML private SidebarController sidebarController;
    @FXML private VBox messagesContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField messageField;
    @FXML private TextField amountField;
    @FXML private Label projectTitleLabel;
    @FXML private Label statusLabel;
    @FXML private Label partiesLabel;
    @FXML private Button sendBtn;
    @FXML private Button aiSuggestBtn;
    @FXML private Button acceptDealBtn;
    @FXML private Button makeOfferBtn;
    @FXML private VBox aiSuggestionBox;
    @FXML private Label aiSuggestionText;
    @FXML private ProgressIndicator aiSpinner;
    @FXML private Label negotiationInfoLabel;

    private NegotiationService negotiationService;
    private OpenRouterService aiService;
    private Negotiation currentNegotiation;
    private int currentUserId;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("negotiation-view");
        negotiationService = new NegotiationService();
        aiService = new OpenRouterService();
        aiSuggestionBox.setVisible(false);
        aiSuggestionBox.setManaged(false);
        aiSpinner.setVisible(false);

        User me = AppSession.getCurrentUser();
        currentUserId = (me != null) ? me.getUserId() : 0;
    }

    public void loadNegotiation(int negotiationId) {
        try {
            currentNegotiation = negotiationService.getById(negotiationId);
            if (currentNegotiation == null) return;

            projectTitleLabel.setText(currentNegotiation.getProjectTitle());
            statusLabel.setText(currentNegotiation.getStatus().toUpperCase());
            statusLabel.getStyleClass().add(getStatusStyleClass(currentNegotiation.getStatus()));
            partiesLabel.setText(currentNegotiation.getInvestorName() + " ↔ " + currentNegotiation.getStartupName());

            if (currentNegotiation.getProposedAmount() != null) {
                negotiationInfoLabel.setText("Initial offer: " +
                        currentNegotiation.getProposedAmount().toPlainString() + " EUR");
            }

            boolean isOpen = "open".equals(currentNegotiation.getStatus());
            sendBtn.setDisable(!isOpen);
            makeOfferBtn.setDisable(!isOpen);
            acceptDealBtn.setDisable(!isOpen);
            messageField.setDisable(!isOpen);

            loadMessages();
        } catch (SQLException e) {
            showAlert("Error loading negotiation: " + e.getMessage());
        }
    }

    public void initNewNegotiation(int projectId, int investorId, int startupId,
                                    String projectTitle, BigDecimal initialOffer) {
        try {
            Negotiation neg = new Negotiation(projectId, investorId, startupId, initialOffer);
            neg = negotiationService.create(neg);

            NegotiationMessage firstMsg = new NegotiationMessage(
                    neg.getNegotiationId(), investorId,
                    "I'd like to invest " + initialOffer.toPlainString() + " EUR in this project.",
                    "offer");
            firstMsg.setProposedAmount(initialOffer);
            negotiationService.addMessage(firstMsg);

            loadNegotiation(neg.getNegotiationId());
        } catch (SQLException e) {
            showAlert("Error creating negotiation: " + e.getMessage());
        }
    }

    private void loadMessages() {
        try {
            List<NegotiationMessage> messages = negotiationService.getMessages(currentNegotiation.getNegotiationId());
            messagesContainer.getChildren().clear();

            for (NegotiationMessage msg : messages) {
                HBox bubble = createMessageBubble(msg);
                messagesContainer.getChildren().add(bubble);

                FadeTransition fade = new FadeTransition(Duration.millis(300), bubble);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.play();
            }

            Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
        } catch (SQLException e) {
            showAlert("Error loading messages: " + e.getMessage());
        }
    }

    private HBox createMessageBubble(NegotiationMessage msg) {
        boolean isMe = msg.getSenderId() == currentUserId;
        HBox row = new HBox(10);
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 12, 4, 12));

        VBox bubble = new VBox(6);
        bubble.setMaxWidth(420);
        bubble.setPadding(new Insets(12, 16, 12, 16));

        String bgColor = isMe ? "rgba(232,169,58,0.20)" : "rgba(30,58,95,0.65)";
        String borderColor = isMe ? "rgba(232,169,58,0.40)" : "rgba(30,58,95,0.80)";
        String radius = isMe ? "16 4 16 16" : "4 16 16 16";
        bubble.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %s; -fx-border-color: %s; -fx-border-radius: %s; -fx-border-width: 1;",
                bgColor, radius, borderColor, radius));

        bubble.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.15)));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(msg.getSenderName() != null ? msg.getSenderName() : "User");
        nameLabel.setStyle("-fx-text-fill: #E8A93A; -fx-font-weight: 800; -fx-font-size: 12px;");
        Label timeLabel = new Label(msg.getCreatedAt() != null ? msg.getCreatedAt().format(TIME_FMT) : "");
        timeLabel.setStyle("-fx-text-fill: #FDB813; -fx-font-size: 10px;");
        header.getChildren().addAll(nameLabel, timeLabel);

        if (msg.getSentiment() != null && !msg.getSentiment().isEmpty()) {
            Label sentimentBadge = new Label(getSentimentEmoji(msg.getSentiment()));
            sentimentBadge.setStyle("-fx-font-size: 12px;");
            sentimentBadge.setTooltip(new Tooltip("Sentiment: " + msg.getSentiment()));
            header.getChildren().add(sentimentBadge);
        }

        Label msgText = new Label(msg.getMessage());
        msgText.setWrapText(true);
        msgText.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px;");

        bubble.getChildren().addAll(header, msgText);

        if (msg.getProposedAmount() != null) {
            Label amountBadge = new Label("Offer: " + msg.getProposedAmount().toPlainString() + " EUR");
            amountBadge.setStyle("-fx-background-color: rgba(253,184,19,0.20); -fx-text-fill: #FDB813; " +
                    "-fx-background-radius: 8; -fx-padding: 4 10; -fx-font-weight: 800; -fx-font-size: 12px;");
            bubble.getChildren().add(amountBadge);
        }

        if ("ai_suggestion".equals(msg.getMessageType())) {
            Label aiBadge = new Label("AI Suggestion");
            aiBadge.setStyle("-fx-background-color: rgba(139,92,246,0.20); -fx-text-fill: #A78BFA; " +
                    "-fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: 800;");
            header.getChildren().add(aiBadge);
        }

        row.getChildren().add(bubble);
        return row;
    }

    @FXML
    public void handleSend() {
        String text = messageField.getText().trim();
        if (text.isEmpty() || currentNegotiation == null) return;

        try {
            NegotiationMessage msg = new NegotiationMessage(
                    currentNegotiation.getNegotiationId(), currentUserId, text, "text");

            analyzeSentimentAsync(text, msg);

            negotiationService.addMessage(msg);
            messageField.clear();
            loadMessages();
        } catch (SQLException e) {
            showAlert("Error sending message: " + e.getMessage());
        }
    }

    @FXML
    public void handleMakeOffer() {
        String amountText = amountField.getText().trim();
        String text = messageField.getText().trim();
        if (amountText.isEmpty() || currentNegotiation == null) return;

        try {
            BigDecimal amount = new BigDecimal(amountText);
            String offerMsg = text.isEmpty()
                    ? "I propose " + amount.toPlainString() + " EUR for this investment."
                    : text;

            NegotiationMessage msg = new NegotiationMessage(
                    currentNegotiation.getNegotiationId(), currentUserId, offerMsg, "offer");
            msg.setProposedAmount(amount);
            negotiationService.addMessage(msg);

            messageField.clear();
            amountField.clear();
            loadMessages();
        } catch (NumberFormatException e) {
            showAlert("Invalid amount format");
        } catch (SQLException e) {
            showAlert("Error making offer: " + e.getMessage());
        }
    }

    @FXML
    public void handleAISuggest() {
        if (currentNegotiation == null) return;

        aiSpinner.setVisible(true);
        aiSuggestBtn.setDisable(true);

        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                List<NegotiationMessage> recent = negotiationService.getRecentMessages(
                        currentNegotiation.getNegotiationId(), 10);

                List<OpenRouterService.Message> history = new ArrayList<>();
                for (NegotiationMessage m : recent) {
                    String role = m.getSenderId() == currentUserId ? "user" : "assistant";
                    history.add(new OpenRouterService.Message(role, m.getMessage()));
                }

                double proposed = currentNegotiation.getProposedAmount() != null
                        ? currentNegotiation.getProposedAmount().doubleValue() : 0;

                return aiService.suggestNegotiationResponse(history, "investor", proposed, proposed * 1.2);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            aiSpinner.setVisible(false);
            aiSuggestBtn.setDisable(false);
            String result = task.getValue();

            try {
                JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                String suggestion = json.has("suggestedMessage")
                        ? json.get("suggestedMessage").getAsString() : result;
                String reasoning = json.has("reasoning")
                        ? json.get("reasoning").getAsString() : "";

                aiSuggestionBox.setVisible(true);
                aiSuggestionBox.setManaged(true);
                aiSuggestionText.setText(suggestion);

                FadeTransition fade = new FadeTransition(Duration.millis(400), aiSuggestionBox);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.play();

                if (json.has("suggestedAmount") && !json.get("suggestedAmount").isJsonNull()) {
                    double amt = json.get("suggestedAmount").getAsDouble();
                    if (amt > 0) amountField.setText(String.valueOf((int) amt));
                }
            } catch (Exception ex) {
                aiSuggestionBox.setVisible(true);
                aiSuggestionBox.setManaged(true);
                aiSuggestionText.setText(result);
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            aiSpinner.setVisible(false);
            aiSuggestBtn.setDisable(false);
            showAlert("AI suggestion failed: " + task.getException().getMessage());
        }));

        new Thread(task).start();
    }

    @FXML
    public void handleUseSuggestion() {
        String suggestion = aiSuggestionText.getText();
        if (suggestion != null && !suggestion.isEmpty()) {
            messageField.setText(suggestion);
            aiSuggestionBox.setVisible(false);
            aiSuggestionBox.setManaged(false);
        }
    }

    @FXML
    public void handleDismissSuggestion() {
        aiSuggestionBox.setVisible(false);
        aiSuggestionBox.setManaged(false);
    }

    @FXML
    public void handleAcceptDeal() {
        if (currentNegotiation == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Accept Deal");
        confirm.setHeaderText("Accept this deal?");
        confirm.setContentText("This will finalize the negotiation and start the payment process.");

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    BigDecimal finalAmount = currentNegotiation.getProposedAmount();
                    negotiationService.updateStatus(
                            currentNegotiation.getNegotiationId(), "accepted", finalAmount);

                    NegotiationMessage msg = new NegotiationMessage(
                            currentNegotiation.getNegotiationId(), currentUserId,
                            "Deal accepted! Moving to payment.", "text");
                    negotiationService.addMessage(msg);

                    navigateToDealPipeline(currentNegotiation, finalAmount);
                } catch (SQLException e) {
                    showAlert("Error accepting deal: " + e.getMessage());
                }
            }
        });
    }

    private void navigateToDealPipeline(Negotiation neg, BigDecimal amount) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/DealPipelineView.fxml"));
            Parent root = loader.load();
            DealPipelineController controller = loader.getController();
            controller.initDeal(neg.getNegotiationId(), neg.getProjectId(),
                    neg.getInvestorId(), neg.getStartupId(), amount);

            Stage stage = (Stage) messagesContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Deal Pipeline - " + neg.getProjectTitle());
        } catch (IOException e) {
            showAlert("Navigation error: " + e.getMessage());
        }
    }

    private void analyzeSentimentAsync(String text, NegotiationMessage msg) {
        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                return aiService.analyzeSentiment(text);
            }
        };
        task.setOnSucceeded(e -> {
            try {
                JsonObject json = JsonParser.parseString(task.getValue()).getAsJsonObject();
                if (json.has("sentiment")) {
                    msg.setSentiment(json.get("sentiment").getAsString());
                }
            } catch (Exception ignored) {}
        });
        new Thread(task).start();
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/ProjectsListView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) messagesContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private String getSentimentEmoji(String sentiment) {
        return switch (sentiment.toLowerCase()) {
            case "positive" -> "\uD83D\uDE0A";
            case "negative" -> "\uD83D\uDE1F";
            case "hostile" -> "\uD83D\uDE21";
            default -> "\uD83D\uDE10";
        };
    }

    private String getStatusStyleClass(String status) {
        return switch (status) {
            case "accepted" -> "badge-success";
            case "rejected" -> "badge-danger";
            default -> "badge-gold";
        };
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
