package com.bizhub.model.services.investissement.AI;

import com.bizhub.model.investissement.Project;
import com.bizhub.model.services.investissement.ProjectServiceImpl;
import com.bizhub.model.services.common.DB.MyDatabase;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.math.BigDecimal;

/**
 * Service d'intégration IA - Unifie les systèmes d'IA pour la plateforme BizHub
 * Orchestre la recommandation, le chatbot et la détection de fraude
 */
public class AIIntegrationService {
    
    private static final Logger logger = Logger.getLogger(AIIntegrationService.class.getName());
    
    private final RecommendationEngine recommendationEngine;
    private final ChatbotAssistant chatbotAssistant;
    private final FraudDetectionSystem fraudDetectionSystem;
    
    public AIIntegrationService() {
        this.recommendationEngine = new RecommendationEngine();
        this.chatbotAssistant = new ChatbotAssistant();
        this.fraudDetectionSystem = new FraudDetectionSystem();
    }
    
    /**
     * Traite un investissement complet avec tous les systèmes d'IA
     * @param investorId ID de l'investisseur
     * @param projectId ID du projet
     * @param amount Montant de l'investissement
     * @return Résultat du traitement avec analyses IA
     */
    public InvestmentProcessingResult processInvestmentWithAI(int investorId, int projectId, BigDecimal amount) {
        try {
            // 1. Créer l'investissement et obtenir l'ID
            int investmentId = createInvestment(investorId, projectId, amount);
            
            // 2. Analyse de fraude
            FraudDetectionSystem.FraudAnalysisResult fraudResult = 
                fraudDetectionSystem.analyzeInvestment(investmentId, investorId, amount, projectId);
            
            // 3. Si risque élevé, bloquer l'investissement
            if (fraudResult.getRiskLevel() == FraudDetectionSystem.FraudRiskLevel.CRITICAL) {
                blockInvestment(investmentId, "Risque de fraude critique détecté");
                return new InvestmentProcessingResult(investmentId, InvestmentStatus.BLOCKED, 
                    fraudResult, null, "Investissement bloqué pour raisons de sécurité");
            }
            
            // 4. Enregistrer les recommandations pour analyse future
            recordRecommendationInteraction(investorId, projectId, 0.8); // Score estimé
            
            // 5. Générer un message de chatbot personnalisé
            String chatbotMessage = generateInvestmentConfirmationMessage(investorId, projectId, amount, fraudResult);
            
            // 6. Déterminer le statut final
            InvestmentStatus status = fraudResult.isSuspicious() ? 
                InvestmentStatus.UNDER_REVIEW : InvestmentStatus.APPROVED;
            
            return new InvestmentProcessingResult(investmentId, status, fraudResult, 
                chatbotMessage, "Investissement traité avec analyse IA");
                
        } catch (Exception e) {
            logger.severe("Erreur traitement investissement IA: " + e.getMessage());
            return new InvestmentProcessingResult(0, InvestmentStatus.ERROR, null, 
                null, "Erreur système: " + e.getMessage());
        }
    }
    
    /**
     * Obtient des recommandations intelligentes pour un investisseur
     */
    public List<RecommendationEngine.ProjectRecommendation> getSmartRecommendations(int investorId) {
        try {
            // Obtenir les recommandations de base
            List<RecommendationEngine.ProjectRecommendation> recommendations = 
                recommendationEngine.getProjectRecommendations(investorId, 5);
            
            // Si aucune recommandation n'est retournée, créer des recommandations factices pour la démo
            if (recommendations.isEmpty()) {
                recommendations = createDemoRecommendations(investorId);
            }
            
            // Enrichir avec des données comportementales
            enrichRecommendationsWithBehaviorData(investorId, recommendations);
            
            return recommendations;
            
        } catch (Exception e) {
            logger.severe("Erreur获取智能推荐: " + e.getMessage());
            // Retourner des recommandations de démo en cas d'erreur
            return createDemoRecommendations(investorId);
        }
    }
    
    private List<RecommendationEngine.ProjectRecommendation> createDemoRecommendations(int investorId) {
        List<RecommendationEngine.ProjectRecommendation> demoRecs = new ArrayList<>();
        
        try {
            // Obtenir tous les projets disponibles
            ProjectServiceImpl projectService = new ProjectServiceImpl();
            List<Project> allProjects = projectService.getAllWithStats();
            
            // Créer des recommandations factices avec des scores aléatoires
            for (int i = 0; i < Math.min(allProjects.size(), 5); i++) {
                Project externalProject = allProjects.get(i);
                
                // Convertir external Project vers RecommendationEngine.Project
                RecommendationEngine.Project internalProject = new RecommendationEngine.Project(
                    externalProject.getProjectId(),
                    externalProject.getTitle(),
                    externalProject.getDescription(),
                    externalProject.getRequiredBudget(),
                    externalProject.getStatus(),
                    externalProject.getStartupId(),
                    "Company " + externalProject.getStartupId(), // Nom d'entreprise factice
                    "Technology" // Secteur factice
                );
                
                // Score aléatoire entre 0.5 et 0.95 pour la démo
                double score = 0.5 + (Math.random() * 0.45);
                demoRecs.add(new RecommendationEngine.ProjectRecommendation(internalProject, score));
            }
            
            // Trier par score
            demoRecs.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            
        } catch (Exception e) {
            logger.warning("Erreur création recommandations démo: " + e.getMessage());
        }
        
        return demoRecs;
    }
    
    /**
     * Traite un message du chatbot avec contexte IA
     */
    public ChatbotAssistant.ChatbotResponse processIntelligentMessage(int userId, String message) {
        try {
            // Traitement de base du chatbot
            ChatbotAssistant.ChatbotResponse baseResponse = 
                chatbotAssistant.processMessage(userId, message);
            
            // Enrichir avec contexte d'investissement
            String enrichedMessage = enrichChatbotResponse(userId, baseResponse.getMessage(), message);
            
            // Ajouter des actions contextuelles
            List<String> contextualActions = getContextualActions(userId, baseResponse.getIntent());
            
            return new ChatbotAssistant.ChatbotResponse(enrichedMessage, baseResponse.getIntent(), contextualActions);
            
        } catch (Exception e) {
            logger.severe("Erreur traitement message intelligent: " + e.getMessage());
            return new ChatbotAssistant.ChatbotResponse("Désolé, je rencontre une difficulté technique.", 
                "error", Collections.emptyList());
        }
    }
    
    /**
     * Analyse les tendances de fraude et génère des rapports
     */
    public FraudTrendReport analyzeFraudTrends() {
        try (Connection conn = MyDatabase.getInstance().getCnx()) {
            FraudTrendReport report = new FraudTrendReport();
            
            // Statistiques des dernières 24h
            String sql24h = "SELECT COUNT(*) as total, " +
                          "SUM(CASE WHEN risk_level IN ('HIGH', 'CRITICAL') THEN 1 ELSE 0 END) as high_risk, " +
                          "AVG(risk_score) as avg_score " +
                          "FROM fraud_analysis WHERE analysis_date > DATE_SUB(NOW(), INTERVAL 1 DAY)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql24h)) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    report.setLast24hTotal(rs.getInt("total"));
                    report.setLast24hHighRisk(rs.getInt("high_risk"));
                    report.setLast24hAvgScore(rs.getDouble("avg_score"));
                }
            }
            
            // Tendances de la semaine
            String sqlWeek = "SELECT DATE(analysis_date) as date, risk_level, COUNT(*) as count " +
                           "FROM fraud_analysis WHERE analysis_date > DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                           "GROUP BY DATE(analysis_date), risk_level ORDER BY date DESC";
            
            Map<String, Map<String, Integer>> weeklyTrends = new LinkedHashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement(sqlWeek)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String date = rs.getString("date");
                    String level = rs.getString("risk_level");
                    int count = rs.getInt("count");
                    
                    weeklyTrends.computeIfAbsent(date, k -> new HashMap<>()).put(level, count);
                }
            }
            report.setWeeklyTrends(weeklyTrends);
            
            // Alertes actives
            String sqlAlerts = "SELECT COUNT(*) as pending FROM fraud_alert WHERE status = 'PENDING'";
            try (PreparedStatement stmt = conn.prepareStatement(sqlAlerts)) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    report.setPendingAlerts(rs.getInt("pending"));
                }
            }
            
            return report;
            
        } catch (SQLException e) {
            logger.severe("Erreur analyse tendances fraude: " + e.getMessage());
            return new FraudTrendReport();
        }
    }
    
    /**
     * Entraîne le système de recommandation avec les données utilisateur
     */
    public void trainRecommendationSystem() {
        try (Connection conn = MyDatabase.getInstance().getCnx()) {
            logger.info("Début de l'entraînement du système de recommandation...");
            
            // Analyser les patterns d'investissement
            String sql = "SELECT investor_id, project_id, amount, " +
                        "DATEDIFF(NOW(), created_at) as days_ago " +
                        "FROM investment WHERE created_at > DATE_SUB(NOW(), INTERVAL 30 DAY)";
            
            Map<Integer, List<InvestmentPattern>> userPatterns = new HashMap<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    int userId = rs.getInt("investor_id");
                    // Variables pour future implémentation de pattern analysis
                    // int projectId = rs.getInt("project_id");
                    // BigDecimal amount = rs.getBigDecimal("amount");
                    // int daysAgo = rs.getInt("days_ago");
                    
                    userPatterns.computeIfAbsent(userId, k -> new ArrayList<>())
                              .add(new InvestmentPattern());
                }
            }
            
            // Mettre à jour les patterns comportementaux
            updateUserBehaviorPatterns(userPatterns);
            
            logger.info("Entraînement du système de recommandation terminé. " + 
                       userPatterns.size() + " utilisateurs analysés.");
            
        } catch (SQLException e) {
            logger.severe("Erreur entraînement système recommandation: " + e.getMessage());
        }
    }
    
    // Méthodes privées
    
    private int createInvestment(int investorId, int projectId, BigDecimal amount) throws SQLException {
        String sql = "INSERT INTO investment (project_id, investor_id, amount, investment_date) VALUES (?, ?, ?, NOW())";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, projectId);
            stmt.setInt(2, investorId);
            stmt.setBigDecimal(3, amount);
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Impossible de créer l'investissement");
    }
    
    private void blockInvestment(int investmentId, String reason) throws SQLException {
        String sql = "UPDATE investment SET contract_url = ? WHERE investment_id = ?";
        try (Connection conn = MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "BLOCKED: " + reason);
            stmt.setInt(2, investmentId);
            stmt.executeUpdate();
        }
    }
    
    private void recordRecommendationInteraction(int userId, int projectId, double score) {
        try (Connection conn = MyDatabase.getInstance().getCnx()) {
            String sql = "INSERT INTO recommendation_analytics (user_id, project_id, recommendation_score) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, projectId);
                stmt.setDouble(3, score);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.warning("Erreur enregistrement recommandation: " + e.getMessage());
        }
    }
    
    private String generateInvestmentConfirmationMessage(int investorId, int projectId, BigDecimal amount, 
                                                       FraudDetectionSystem.FraudAnalysisResult fraudResult) {
        StringBuilder message = new StringBuilder();
        message.append("Félicitations! Votre investissement de ").append(amount).append(" a été enregistré. ");
        
        if (fraudResult.isSuspicious()) {
            message.append("Votre investissement est en cours de vérification de sécurité. ");
        } else {
            message.append("Votre investissement a été approuvé immédiatement. ");
        }
        
        message.append("Vous pouvez suivre son évolution dans votre tableau de bord.");
        
        return message.toString();
    }
    
    private void enrichRecommendationsWithBehaviorData(int investorId, 
                                                      List<RecommendationEngine.ProjectRecommendation> recommendations) {
        // Implémentation future: enrichir avec les patterns comportementaux
        logger.info("Enrichissement des recommandations pour l'utilisateur " + investorId);
    }
    
    private String enrichChatbotResponse(int userId, String baseResponse, String userMessage) {
        // Ajouter du contexte basé sur l'historique récent de l'utilisateur
        if (userMessage.toLowerCase().contains("investissement")) {
            try (Connection conn = MyDatabase.getInstance().getCnx()) {
                String sql = "SELECT COUNT(*) as count FROM investment WHERE investor_id = ? AND created_at > DATE_SUB(NOW(), INTERVAL 7 DAY)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, userId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next() && rs.getInt("count") > 0) {
                        baseResponse += "\n\nJ'ai remarqué que vous avez investi récemment. Voulez-vous voir la performance de vos investissements?";
                    }
                }
            } catch (SQLException e) {
                logger.warning("Erreur enrichissement réponse chatbot: " + e.getMessage());
            }
        }
        
        return baseResponse;
    }
    
    private List<String> getContextualActions(int userId, String intent) {
        List<String> actions = new ArrayList<>();
        
        switch (intent) {
            case "investir":
                actions.addAll(Arrays.asList("Voir mes recommandations", "Analyser mon portefeuille", "Simuler un investissement"));
                break;
            case "recommandation":
                actions.addAll(Arrays.asList("Rafraîchir les recommandations", "Filtrer par secteur", "Voir les tendances"));
                break;
            default:
                actions.addAll(Arrays.asList("Parler à un conseiller", "Voir le tutoriel", "Contacter le support"));
        }
        
        return actions;
    }
    
    private void updateUserBehaviorPatterns(Map<Integer, List<InvestmentPattern>> userPatterns) {
        // Implémentation future: mettre à jour les patterns dans la base de données
        logger.info("Mise à jour des patterns comportementaux pour " + userPatterns.size() + " utilisateurs");
    }
    
    // Classes internes
    
    public static class InvestmentProcessingResult {
        private final int investmentId;
        private final InvestmentStatus status;
        private final FraudDetectionSystem.FraudAnalysisResult fraudAnalysis;
        private final String chatbotMessage;
        private final String systemMessage;
        
        public InvestmentProcessingResult(int investmentId, InvestmentStatus status, 
                                        FraudDetectionSystem.FraudAnalysisResult fraudAnalysis,
                                        String chatbotMessage, String systemMessage) {
            this.investmentId = investmentId;
            this.status = status;
            this.fraudAnalysis = fraudAnalysis;
            this.chatbotMessage = chatbotMessage;
            this.systemMessage = systemMessage;
        }
        
        // Getters
        public int getInvestmentId() { return investmentId; }
        public InvestmentStatus getStatus() { return status; }
        public FraudDetectionSystem.FraudAnalysisResult getFraudAnalysis() { return fraudAnalysis; }
        public String getChatbotMessage() { return chatbotMessage; }
        public String getSystemMessage() { return systemMessage; }
    }
    
    public static class FraudTrendReport {
        private int last24hTotal;
        private int last24hHighRisk;
        private double last24hAvgScore;
        private int pendingAlerts;
        private Map<String, Map<String, Integer>> weeklyTrends;
        
        // Getters et Setters
        public int getLast24hTotal() { return last24hTotal; }
        public void setLast24hTotal(int last24hTotal) { this.last24hTotal = last24hTotal; }
        
        public int getLast24hHighRisk() { return last24hHighRisk; }
        public void setLast24hHighRisk(int last24hHighRisk) { this.last24hHighRisk = last24hHighRisk; }
        
        public double getLast24hAvgScore() { return last24hAvgScore; }
        public void setLast24hAvgScore(double last24hAvgScore) { this.last24hAvgScore = last24hAvgScore; }
        
        public int getPendingAlerts() { return pendingAlerts; }
        public void setPendingAlerts(int pendingAlerts) { this.pendingAlerts = pendingAlerts; }
        
        public Map<String, Map<String, Integer>> getWeeklyTrends() { return weeklyTrends; }
        public void setWeeklyTrends(Map<String, Map<String, Integer>> weeklyTrends) { this.weeklyTrends = weeklyTrends; }
    }
    
    private static class InvestmentPattern {
        // Placeholder for future investment pattern analysis
        // Fields will be added when pattern analysis is implemented
        public InvestmentPattern() {
            // Empty constructor for future implementation
        }
    }
    
    public enum InvestmentStatus {
        APPROVED, UNDER_REVIEW, BLOCKED, ERROR
    }
}
