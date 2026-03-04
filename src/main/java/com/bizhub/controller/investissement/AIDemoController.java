package com.bizhub.controller.investissement;

import com.bizhub.controller.users_avis.user.SidebarController;
import com.bizhub.model.services.investissement.AI.*;
import com.bizhub.model.services.common.DB.MyDatabase;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

/**
 * Contrôleur pour la démonstration des fonctionnalités IA
 * Intègre le système de recommandation, le chatbot et la détection de fraude
 */
public class AIDemoController {
    
    private static final Logger logger = Logger.getLogger(AIDemoController.class.getName());

    @FXML private SidebarController sidebarController;

    private AIIntegrationService aiService;
    private int currentUserId = 6; // Utilisateur de démo

    @FXML private TabPane aiTabPane;
    @FXML private Tab recommendationsTab;
    @FXML private Tab chatbotTab;
    @FXML private Tab fraudDetectionTab;
    
    // Composants Recommendations
    @FXML private ListView<RecommendationItem> recommendationsListView;
    @FXML private Button refreshRecommendationsBtn;
    @FXML private Label recommendationStatusLabel;
    
    // Composants Chatbot
    @FXML private VBox chatMessagesContainer;
    @FXML private TextField chatInputField;
    @FXML private Button sendMessageBtn;
    @FXML private ListView<String> quickActionsListView;
    
    // Composants Détection de Fraude
    @FXML private ListView<FraudAlertItem> fraudAlertsListView;
    @FXML private Label fraudStatisticsLabel;
    @FXML private Button refreshFraudDataBtn;
    @FXML private TextArea fraudDetailsArea;
    
    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("ai-demo");
        aiService = new AIIntegrationService();
        
        setupRecommendationsTab();
        setupChatbotTab();
        setupFraudDetectionTab();
        
        logger.info("Contrôleur IA initialisé");
    }
    
    /**
     * Configure l'onglet des recommandations
     */
    private void setupRecommendationsTab() {
        refreshRecommendationsBtn.setOnAction(e -> loadRecommendations());
        recommendationsListView.setCellFactory(param -> new RecommendationListCell());
        
        loadRecommendations();
    }
    
    /**
     * Charge les recommandations intelligentes
     */
    @FXML
    private void loadRecommendations() {
        try {
            recommendationStatusLabel.setText("Chargement des recommandations...");
            
            List<RecommendationEngine.ProjectRecommendation> recommendations = 
                aiService.getSmartRecommendations(currentUserId);
            
            ObservableList<RecommendationItem> items = FXCollections.observableArrayList();
            
            for (RecommendationEngine.ProjectRecommendation rec : recommendations) {
                items.add(new RecommendationItem(rec));
            }
            
            recommendationsListView.setItems(items);
            recommendationStatusLabel.setText("Recommandations mises à jour (" + items.size() + " projets)");
            
        } catch (Exception e) {
            logger.severe("Erreur chargement recommandations: " + e.getMessage());
            recommendationStatusLabel.setText("Erreur lors du chargement des recommandations");
        }
    }
    
    /**
     * Configure l'onglet du chatbot
     */
    private void setupChatbotTab() {
        sendMessageBtn.setOnAction(e -> sendChatMessage());
        chatInputField.setOnAction(e -> sendChatMessage());
        
        quickActionsListView.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 5;");
                }
            }
        });
        
        quickActionsListView.setOnMouseClicked(e -> {
            String selected = quickActionsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                chatInputField.setText(selected);
                sendChatMessage();
            }
        });
        
        // Message de bienvenue
        addChatMessage("bot", "Bonjour! Je suis votre assistant investisseur. Je peux vous aider avec les investissements, les recommandations, et répondre à vos questions. Comment puis-je vous aider aujourd'hui?");
        
        // Actions rapides initiales
        updateQuickActions("aide");
    }
    
    /**
     * Envoie un message au chatbot
     */
    private void sendChatMessage() {
        String message = chatInputField.getText().trim();
        if (message.isEmpty()) return;
        
        // Ajouter le message utilisateur
        addChatMessage("user", message);
        chatInputField.clear();
        
        // Traiter la réponse en arrière-plan
        new Thread(() -> {
            try {
                ChatbotAssistant.ChatbotResponse response = 
                    aiService.processIntelligentMessage(currentUserId, message);
                
                javafx.application.Platform.runLater(() -> {
                    addChatMessage("bot", response.getMessage());
                    updateQuickActions(response.getIntent());
                });
                
            } catch (Exception e) {
                logger.severe("Erreur traitement message chatbot: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    addChatMessage("bot", "Désolé, je rencontre une difficulté technique. Réessayez plus tard.");
                });
            }
        }).start();
    }
    
    /**
     * Ajoute un message au chat
     */
    private void addChatMessage(String type, String message) {
        HBox messageBox = new HBox(10);
        messageBox.setPadding(new Insets(5));
        messageBox.setMaxWidth(Double.MAX_VALUE);
        
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);
        
        if ("user".equals(type)) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            messageLabel.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 10;");
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
            messageLabel.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 10; -fx-padding: 10;");
        }
        
        messageBox.getChildren().add(messageLabel);
        chatMessagesContainer.getChildren().add(messageBox);
        
        // Auto-scroll vers le bas
        chatMessagesContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            chatMessagesContainer.setSpacing(10);
        });
    }
    
    /**
     * Met à jour les actions rapides
     */
    private void updateQuickActions(String intent) {
        List<String> actions = switch (intent) {
            case "investir" -> List.of("Voir les projets", "Mes recommandations", "Calculer retour sur investissement");
            case "budget" -> List.of("Modifier mon budget", "Voir mes investissements", "Simulateur de budget");
            case "recommandation" -> List.of("Voir toutes les recommandations", "Mettre à jour mon profil", "Explorer par secteur");
            default -> List.of("Guide investisseur", "Support technique", "Voir mes projets");
        };
        
        quickActionsListView.setItems(FXCollections.observableArrayList(actions));
    }
    
    /**
     * Configure l'onglet de détection de fraude
     */
    private void setupFraudDetectionTab() {
        refreshFraudDataBtn.setOnAction(e -> loadFraudData());
        fraudAlertsListView.setCellFactory(param -> new FraudAlertListCell());
        
        fraudAlertsListView.setOnMouseClicked(e -> {
            FraudAlertItem selected = fraudAlertsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showFraudDetails(selected);
            }
        });
        
        loadFraudData();
    }
    
    /**
     * Charge les données de détection de fraude
     */
    private void loadFraudData() {
        try {
            // Charger les tendances de fraude
            AIIntegrationService.FraudTrendReport report = aiService.analyzeFraudTrends();
            
            String statsText = String.format(
                "Statistiques des dernières 24h:\n" +
                "• Analyses: %d\n" +
                "• À risque élevé: %d\n" +
                "• Score moyen: %.2f\n" +
                "• Alertes en attente: %d",
                report.getLast24hTotal(),
                report.getLast24hHighRisk(),
                report.getLast24hAvgScore(),
                report.getPendingAlerts()
            );
            
            fraudStatisticsLabel.setText(statsText);
            
            // Charger les alertes récentes
            loadRecentFraudAlerts();
            
        } catch (Exception e) {
            logger.severe("Erreur chargement données fraude: " + e.getMessage());
            fraudStatisticsLabel.setText("Erreur lors du chargement des statistiques");
        }
    }
    
    /**
     * Charge les alertes de fraude récentes
     */
    private void loadRecentFraudAlerts() {
        try (var conn = MyDatabase.getInstance().getCnx();
             var stmt = conn.prepareStatement(
                 "SELECT fa.alert_id, fa.investment_id, fa.alert_level, fa.alert_message, fa.created_at, " +
                 "u.full_name, i.amount " +
                 "FROM fraud_alert fa " +
                 "JOIN user u ON fa.investor_id = u.user_id " +
                 "JOIN investment i ON fa.investment_id = i.investment_id " +
                 "WHERE fa.created_at > DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                 "ORDER BY fa.created_at DESC LIMIT 10")) {
            
            var rs = stmt.executeQuery();
            ObservableList<FraudAlertItem> alerts = FXCollections.observableArrayList();
            
            while (rs.next()) {
                alerts.add(new FraudAlertItem(
                    rs.getInt("alert_id"),
                    rs.getInt("investment_id"),
                    rs.getString("alert_level"),
                    rs.getString("alert_message"),
                    rs.getString("full_name"),
                    rs.getBigDecimal("amount"),
                    rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
            
            fraudAlertsListView.setItems(alerts);
            
        } catch (Exception e) {
            logger.severe("Erreur chargement alertes fraude: " + e.getMessage());
        }
    }
    
    /**
     * Affiche les détails d'une alerte de fraude
     */
    private void showFraudDetails(FraudAlertItem alert) {
        StringBuilder details = new StringBuilder();
        details.append("Détails de l'Alerte de Fraude\n");
        details.append("================================\n\n");
        details.append("ID Alert: ").append(alert.getAlertId()).append("\n");
        details.append("ID Investissement: ").append(alert.getInvestmentId()).append("\n");
        details.append("Niveau: ").append(alert.getAlertLevel()).append("\n");
        details.append("Investisseur: ").append(alert.getInvestorName()).append("\n");
        details.append("Montant: ").append(alert.getAmount()).append("\n");
        details.append("Date: ").append(alert.getCreatedAt()).append("\n\n");
        details.append("Message:\n").append(alert.getMessage()).append("\n\n");
        
        // Ajouter les détails de l'analyse de fraude
        try (var conn = MyDatabase.getInstance().getCnx();
             var stmt = conn.prepareStatement(
                 "SELECT risk_score, risk_level, risk_factors " +
                 "FROM fraud_analysis WHERE investment_id = ?")) {
            
            stmt.setInt(1, alert.getInvestmentId());
            var rs = stmt.executeQuery();
            
            if (rs.next()) {
                details.append("Analyse de Fraude:\n");
                details.append("Score de risque: ").append(rs.getDouble("risk_score")).append("\n");
                details.append("Niveau de risque: ").append(rs.getString("risk_level")).append("\n");
                details.append("Facteurs: ").append(rs.getString("risk_factors")).append("\n");
            }
            
        } catch (Exception e) {
            details.append("Erreur lors du chargement des détails d'analyse.\n");
        }
        
        fraudDetailsArea.setText(details.toString());
    }
    
    /**
     * Simule un investissement avec analyse IA
     */
    @FXML
    private void simulateInvestmentWithAI() {
        try {
            // Simulation d'investissement
            int projectId = 1; // Projet de démo
            BigDecimal amount = new BigDecimal("5000.00");
            
            AIIntegrationService.InvestmentProcessingResult result = 
                aiService.processInvestmentWithAI(currentUserId, projectId, amount);
            
            // Afficher le résultat
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Résultat de l'Investissement avec IA");
            alert.setHeaderText("Investissement #" + result.getInvestmentId() + " - " + result.getStatus());
            
            StringBuilder content = new StringBuilder();
            content.append("Message système: ").append(result.getSystemMessage()).append("\n\n");
            
            if (result.getFraudAnalysis() != null) {
                content.append("Analyse de fraude:\n");
                content.append("Score de risque: ").append(result.getFraudAnalysis().getRiskScore()).append("\n");
                content.append("Niveau: ").append(result.getFraudAnalysis().getRiskLevel()).append("\n");
                
                if (!result.getFraudAnalysis().getRiskFactors().isEmpty()) {
                    content.append("Facteurs de risque:\n");
                    for (var factor : result.getFraudAnalysis().getRiskFactors()) {
                        content.append("• ").append(factor.getDescription()).append("\n");
                    }
                }
            }
            
            if (result.getChatbotMessage() != null) {
                content.append("\nMessage assistant: ").append(result.getChatbotMessage());
            }
            
            alert.setContentText(content.toString());
            alert.showAndWait();
            
            // Rafraîchir les données
            loadFraudData();
            
        } catch (Exception e) {
            logger.severe("Erreur simulation investissement IA: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Erreur lors de la simulation: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    // Classes internes pour les éléments de liste
    
    public static class RecommendationItem {
        private final RecommendationEngine.ProjectRecommendation recommendation;
        
        public RecommendationItem(RecommendationEngine.ProjectRecommendation recommendation) {
            this.recommendation = recommendation;
        }
        
        public RecommendationEngine.ProjectRecommendation getRecommendation() { return recommendation; }
        
        @Override
        public String toString() {
            return String.format("%s (Score: %.1f%%)", 
                recommendation.getProject().getTitle(), 
                recommendation.getScore() * 100);
        }
    }
    
    public static class FraudAlertItem {
        private final int alertId;
        private final int investmentId;
        private final String alertLevel;
        private final String message;
        private final String investorName;
        private final BigDecimal amount;
        private final LocalDateTime createdAt;
        
        public FraudAlertItem(int alertId, int investmentId, String alertLevel, String message,
                             String investorName, BigDecimal amount, LocalDateTime createdAt) {
            this.alertId = alertId;
            this.investmentId = investmentId;
            this.alertLevel = alertLevel;
            this.message = message;
            this.investorName = investorName;
            this.amount = amount;
            this.createdAt = createdAt;
        }
        
        // Getters
        public int getAlertId() { return alertId; }
        public int getInvestmentId() { return investmentId; }
        public String getAlertLevel() { return alertLevel; }
        public String getMessage() { return message; }
        public String getInvestorName() { return investorName; }
        public BigDecimal getAmount() { return amount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        
        @Override
        public String toString() {
            return String.format("[%s] %s - %s (%.2f)", 
                alertLevel, investorName, createdAt.toString(), amount);
        }
    }
    
    // Cell factories personnalisées
    
    private static class RecommendationListCell extends ListCell<RecommendationItem> {
        @Override
        protected void updateItem(RecommendationItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                var rec = item.getRecommendation();
                var vbox = new VBox(2);
                vbox.setStyle("-fx-padding: 10;");
                
                var titleLabel = new Label(rec.getProject().getTitle());
                titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
                
                var scoreLabel = new Label(String.format("Score: %.1f%%", rec.getScore() * 100));
                scoreLabel.setStyle("-fx-text-fill: #4caf50;");
                
                var budgetLabel = new Label("Budget requis: " + rec.getProject().getRequiredBudget());
                var companyLabel = new Label("Entreprise: " + rec.getProject().getCompanyName());
                
                vbox.getChildren().addAll(titleLabel, scoreLabel, budgetLabel, companyLabel);
                setGraphic(vbox);
            }
        }
    }
    
    private static class FraudAlertListCell extends ListCell<FraudAlertItem> {
        @Override
        protected void updateItem(FraudAlertItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                var vbox = new VBox(2);
                vbox.setStyle("-fx-padding: 10;");
                
                String colorStyle = switch (item.getAlertLevel()) {
                    case "HIGH", "CRITICAL" -> "-fx-background-color: #ffebee;";
                    case "MEDIUM" -> "-fx-background-color: #fff3e0;";
                    default -> "-fx-background-color: #e8f5e8;";
                };
                vbox.setStyle(colorStyle + " -fx-padding: 10; -fx-background-radius: 5;");
                
                var levelLabel = new Label("[" + item.getAlertLevel() + "]");
                levelLabel.setStyle("-fx-font-weight: bold;");
                
                var nameLabel = new Label(item.getInvestorName());
                var amountLabel = new Label("Montant: " + item.getAmount());
                var dateLabel = new Label(item.getCreatedAt().toString());
                
                vbox.getChildren().addAll(levelLabel, nameLabel, amountLabel, dateLabel);
                setGraphic(vbox);
            }
        }
    }
}
