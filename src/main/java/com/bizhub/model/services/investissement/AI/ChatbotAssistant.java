package com.bizhub.model.services.investissement.AI;

import com.bizhub.model.services.common.DB.MyDatabase;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.time.LocalDateTime;

/**
 * Chatbot Assistant Investisseur - Système de support intelligent
 * Fournit des réponses automatisées et guide les investisseurs
 */
public class ChatbotAssistant {
    
    private static final Logger logger = Logger.getLogger(ChatbotAssistant.class.getName());
    
    // Base de connaissances du chatbot
    private static final Map<String, String> KNOWLEDGE_BASE = Map.of(
        "investir", "Pour investir dans un projet, allez dans la section 'Investissements', sélectionnez un projet et choisissez le montant à investir. Assurez-vous que le projet correspond à votre secteur d'intérêt et à votre budget.",
        "budget", "Votre budget maximum est défini dans votre profil. Vous pouvez le modifier dans 'Paramètres > Profil'. Nous vous recommandons de ne pas investir plus de 20% de votre budget total dans un seul projet.",
        "risque", "Tout investissement comporte des risques. Diversifiez vos investissements, étudiez bien les projets, et n'investissez que ce que vous pouvez vous permettre de perdre. Nos projets sont notés selon leur niveau de risque.",
        "recommandation", "Je vous recommande des projets basés sur votre profil: secteur d'investissement, budget, et expérience. Consultez la section 'Recommandations' pour voir les projets qui vous correspondent.",
        "paiement", "Les paiements sont sécurisés via notre partenaire Stripe. Vous pouvez payer par carte bancaire, virement, ou porte-monnaie numérique. Les fonds sont bloqués jusqu'à la validation du projet.",
        "remboursement", "Les remboursements dépendent des termes du projet. Consultez la section 'Mes Investissements' pour suivre les performances et les échéances de remboursement.",
        "profil", "Completez votre profil avec votre secteur d'investissement, votre budget maximum, et votre expérience pour recevoir des recommandations personnalisées.",
        "aide", "Je suis là pour vous aider! Vous pouvez me poser des questions sur: investissement, budget, risque, recommandations, paiement, ou profil."
    );
    
    /**
     * Traite un message de l'utilisateur et génère une réponse
     * @param userId ID de l'utilisateur
     * @param message Message de l'utilisateur
     * @return Réponse du chatbot
     */
    public ChatbotResponse processMessage(int userId, String message) {
        try {
            // 1. Logger la conversation pour analyse
            logConversation(userId, message);
            
            // 2. Analyser l'intention de l'utilisateur
            String intent = analyzeIntent(message.toLowerCase());
            
            // 3. Générer la réponse
            String response = generateResponse(intent, userId);
            
            // 4. Enrichir avec données personnelles si disponible
            response = personalizeResponse(response, userId);
            
            // 5. Logger la réponse
            logBotResponse(userId, response);
            
            return new ChatbotResponse(response, intent, getQuickActions(intent));
            
        } catch (Exception e) {
            logger.severe("Erreur traitement message chatbot: " + e.getMessage());
            return new ChatbotResponse("Désolé, je rencontre une difficulté technique. Réessayez plus tard ou contactez notre support.", "error", Collections.emptyList());
        }
    }
    
    /**
     * Analyse l'intention dans le message de l'utilisateur
     */
    private String analyzeIntent(String message) {
        // Mots-clés pour chaque intention
        Map<String, List<String>> intentKeywords = Map.of(
            "investir", Arrays.asList("investir", "investissement", "placer", "projets"),
            "budget", Arrays.asList("budget", "argent", "montant", "coût", "prix"),
            "risque", Arrays.asList("risque", "dangereux", "sécurité", "perte", "garantie"),
            "recommandation", Arrays.asList("recommander", "suggestion", "conseil", "idéal"),
            "paiement", Arrays.asList("payer", "paiement", "virement", "carte", "stripe"),
            "remboursement", Arrays.asList("rembourser", "retour", "gain", "profit", "rendement"),
            "profil", Arrays.asList("profil", "compte", "personnel", "informations"),
            "aide", Arrays.asList("aide", "help", "comment", "que faire", "support")
        );
        
        // Chercher la meilleure correspondance
        for (Map.Entry<String, List<String>> entry : intentKeywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (message.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        
        return "general";
    }
    
    /**
     * Génère une réponse basée sur l'intention
     */
    private String generateResponse(String intent, int userId) {
        String baseResponse = KNOWLEDGE_BASE.getOrDefault(intent, 
            "Je suis là pour vous aider! Vous pouvez me poser des questions sur l'investissement, les projets, ou votre profil. Que souhaitez-vous savoir?");
        
        // Ajouter des données contextuelles
        if ("recommandation".equals(intent)) {
            baseResponse += getPersonalizedRecommendations(userId);
        } else if ("budget".equals(intent)) {
            baseResponse += getCurrentBudgetInfo(userId);
        }
        
        return baseResponse;
    }
    
    /**
     * Personnalise la réponse avec des données utilisateur
     */
    private String personalizeResponse(String response, int userId) {
        try (Connection conn = MyDatabase.getInstance().getCnx()) {
            String sql = "SELECT full_name, user_type, investment_sector FROM user WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String name = rs.getString("full_name");
                    response = response.replace("vous", name != null ? name : "cher investisseur");
                }
            }
        } catch (SQLException e) {
            logger.warning("Erreur personnalisation réponse: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Obtient des recommandations personnalisées
     */
    private String getPersonalizedRecommendations(int userId) {
        try {
            RecommendationEngine engine = new RecommendationEngine();
            var recommendations = engine.getProjectRecommendations(userId, 3);
            
            if (!recommendations.isEmpty()) {
                StringBuilder sb = new StringBuilder("\n\nProjets recommandés pour vous:\n");
                for (var rec : recommendations) {
                    sb.append(String.format("• %s (Score: %.1f%%)\n", 
                        rec.getProject().getTitle(), rec.getScore() * 100));
                }
                return sb.toString();
            }
        } catch (Exception e) {
            logger.warning("Erreur获取推荐: " + e.getMessage());
        }
        
        return "\n\nConsultez la section 'Investissements' pour voir les projets disponibles.";
    }
    
    /**
     * Obtient les informations budgétaires actuelles
     */
    private String getCurrentBudgetInfo(int userId) {
        try (Connection conn = MyDatabase.getInstance().getCnx()) {
            String sql = "SELECT max_budget FROM user WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    var budget = rs.getBigDecimal("max_budget");
                    if (budget != null) {
                        return String.format("\n\nVotre budget maximum actuel: %.2f", budget);
                    }
                }
            }
        } catch (SQLException e) {
            logger.warning("Erreur获取预算信息: " + e.getMessage());
        }
        
        return "\n\nConfigurez votre budget dans votre profil pour des recommandations personnalisées.";
    }
    
    /**
     * Retourne les actions rapides suggérées
     */
    private List<String> getQuickActions(String intent) {
        switch (intent) {
            case "investir":
                return Arrays.asList("Voir les projets", "Mes recommandations", "Calculer retour sur investissement");
            case "budget":
                return Arrays.asList("Modifier mon budget", "Voir mes investissements", "Simulateur de budget");
            case "recommandation":
                return Arrays.asList("Voir toutes les recommandations", "Mettre à jour mon profil", "Explorer par secteur");
            case "profil":
                return Arrays.asList("Modifier mon profil", "Voir mes statistiques", "Paramètres de notification");
            default:
                return Arrays.asList("Guide investisseur", "Support technique", "FAQ");
        }
    }
    
    /**
     * Enregistre la conversation pour analyse
     */
    private void logConversation(int userId, String message) {
        try (Connection conn = MyDatabase.getInstance().getCnx()) {
            String sql = "INSERT INTO chatbot_conversation (user_id, message, message_type, created_at) VALUES (?, ?, 'user', NOW())";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, message);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.warning("Erreur enregistrement conversation: " + e.getMessage());
        }
    }
    
    /**
     * Enregistre la réponse du bot
     */
    private void logBotResponse(int userId, String response) {
        try (Connection conn = MyDatabase.getInstance().getCnx()) {
            String sql = "INSERT INTO chatbot_conversation (user_id, message, message_type, created_at) VALUES (?, ?, 'bot', NOW())";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, response);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.warning("Erreur enregistrement réponse bot: " + e.getMessage());
        }
    }
    
    /**
     * Obtient l'historique des conversations récentes
     */
    public List<ConversationEntry> getConversationHistory(int userId, int limit) {
        List<ConversationEntry> history = new ArrayList<>();
        
        try (Connection conn = MyDatabase.getInstance().getCnx()) {
            String sql = "SELECT message, message_type, created_at FROM chatbot_conversation " +
                        "WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, limit);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    history.add(new ConversationEntry(
                        rs.getString("message"),
                        rs.getString("message_type"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            logger.severe("Erreur récupération historique: " + e.getMessage());
        }
        
        Collections.reverse(history); // Ordre chronologique
        return history;
    }
    
    // Classes internes
    
    public static class ChatbotResponse {
        private final String message;
        private final String intent;
        private final List<String> quickActions;
        
        public ChatbotResponse(String message, String intent, List<String> quickActions) {
            this.message = message;
            this.intent = intent;
            this.quickActions = quickActions;
        }
        
        // Getters
        public String getMessage() { return message; }
        public String getIntent() { return intent; }
        public List<String> getQuickActions() { return quickActions; }
    }
    
    public static class ConversationEntry {
        private final String message;
        private final String type; // 'user' or 'bot'
        private final LocalDateTime timestamp;
        
        public ConversationEntry(String message, String type, LocalDateTime timestamp) {
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getMessage() { return message; }
        public String getType() { return type; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
