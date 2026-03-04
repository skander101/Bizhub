package com.bizhub.model.services.investissement.AI;

import com.bizhub.model.services.common.DB.MyDatabase;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Système de Détection de Fraude - Analyse des comportements suspects
 * Surveillance en temps réel des transactions et patterns anormaux
 */
public class FraudDetectionSystem {
    
    private static final Logger logger = Logger.getLogger(FraudDetectionSystem.class.getName());
    
    // Seuils de détection
    private static final BigDecimal MAX_SINGLE_INVESTMENT = new BigDecimal("100000.00");
    private static final int MAX_INVESTMENTS_PER_HOUR = 5;
    private static final int MAX_INVESTMENTS_PER_DAY = 20;
    private static final BigDecimal SUSPICIOUS_AMOUNT_PATTERN = new BigDecimal("99999.99");
    
    /**
     * Analyse une transaction d'investissement pour détecter une fraude potentielle
     * @param investmentId ID de l'investissement
     * @param investorId ID de l'investisseur
     * @param amount Montant de l'investissement
     * @param projectId ID du projet
     * @return Résultat de l'analyse de fraude
     */
    public FraudAnalysisResult analyzeInvestment(int investmentId, int investorId, BigDecimal amount, int projectId) {
        List<FraudRiskFactor> riskFactors = new ArrayList<>();
        double riskScore = 0.0;
        
        try (Connection conn = MyDatabase.getInstance().getCnx()) {
            
            // 1. Vérifier le montant de l'investissement
            riskScore += analyzeInvestmentAmount(amount, riskFactors);
            
            // 2. Analyser la fréquence des transactions
            riskScore += analyzeTransactionFrequency(conn, investorId, riskFactors);
            
            // 3. Vérifier les patterns suspects
            riskScore += analyzeSuspiciousPatterns(conn, investorId, amount, riskFactors);
            
            // 4. Analyser le profil de l'investisseur
            riskScore += analyzeInvestorProfile(conn, investorId, riskFactors);
            
            // 5. Vérifier la corrélation avec d'autres investisseurs
            riskScore += analyzeInvestorCorrelation(conn, investorId, projectId, riskFactors);
            
            // 6. Déterminer le niveau de risque
            FraudRiskLevel riskLevel = determineRiskLevel(riskScore);
            
            // 7. Enregistrer l'analyse
            recordFraudAnalysis(conn, investmentId, investorId, riskScore, riskLevel, riskFactors);
            
            // 8. Générer une alerte si nécessaire
            if (riskLevel != FraudRiskLevel.LOW) {
                generateFraudAlert(conn, investmentId, investorId, riskLevel, riskFactors);
            }
            
            return new FraudAnalysisResult(riskScore, riskLevel, riskFactors);
            
        } catch (SQLException e) {
            logger.severe("Erreur lors de l'analyse de fraude: " + e.getMessage());
            return new FraudAnalysisResult(0.5, FraudRiskLevel.MEDIUM, 
                Collections.singletonList(new FraudRiskFactor("SYSTEM_ERROR", "Erreur système lors de l'analyse", 0.5)));
        }
    }
    
    /**
     * Analyse le montant de l'investissement
     */
    private double analyzeInvestmentAmount(BigDecimal amount, List<FraudRiskFactor> riskFactors) {
        double riskScore = 0.0;
        
        // Vérifier si le montant dépasse le seuil maximum
        if (amount.compareTo(MAX_SINGLE_INVESTMENT) > 0) {
            riskScore += 0.8;
            riskFactors.add(new FraudRiskFactor("HIGH_AMOUNT", 
                "Montant d'investissement supérieur au seuil autorisé", 0.8));
        }
        
        // Vérifier les montants suspects (ex: 99999.99)
        if (amount.compareTo(SUSPICIOUS_AMOUNT_PATTERN) == 0) {
            riskScore += 0.6;
            riskFactors.add(new FraudRiskFactor("SUSPICIOUS_PATTERN", 
                "Montant suspect détecté", 0.6));
        }
        
        // Vérifier les montants ronds suspects
        if (isRoundNumber(amount) && amount.compareTo(new BigDecimal("10000.00")) > 0) {
            riskScore += 0.3;
            riskFactors.add(new FraudRiskFactor("ROUND_AMOUNT", 
                "Montant rond important", 0.3));
        }
        
        return riskScore;
    }
    
    /**
     * Analyse la fréquence des transactions
     */
    private double analyzeTransactionFrequency(Connection conn, int investorId, List<FraudRiskFactor> riskFactors) throws SQLException {
        double riskScore = 0.0;
        
        // Vérifier les transactions de la dernière heure
        String sqlHour = "SELECT COUNT(*) as count FROM investment WHERE investor_id = ? AND created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)";
        try (PreparedStatement stmt = conn.prepareStatement(sqlHour)) {
            stmt.setInt(1, investorId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt("count") > MAX_INVESTMENTS_PER_HOUR) {
                riskScore += 0.7;
                riskFactors.add(new FraudRiskFactor("HIGH_FREQUENCY_HOUR", 
                    "Trop d'investissements en une heure", 0.7));
            }
        }
        
        // Vérifier les transactions des dernières 24h
        String sqlDay = "SELECT COUNT(*) as count FROM investment WHERE investor_id = ? AND created_at > DATE_SUB(NOW(), INTERVAL 1 DAY)";
        try (PreparedStatement stmt = conn.prepareStatement(sqlDay)) {
            stmt.setInt(1, investorId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt("count") > MAX_INVESTMENTS_PER_DAY) {
                riskScore += 0.5;
                riskFactors.add(new FraudRiskFactor("HIGH_FREQUENCY_DAY", 
                    "Trop d'investissements en 24h", 0.5));
            }
        }
        
        return riskScore;
    }
    
    /**
     * Analyse les patterns suspects
     */
    private double analyzeSuspiciousPatterns(Connection conn, int investorId, BigDecimal amount, List<FraudRiskFactor> riskFactors) throws SQLException {
        double riskScore = 0.0;
        
        // Vérifier les montants croissants rapides
        String sqlIncreasing = "SELECT amount FROM investment WHERE investor_id = ? ORDER BY created_at DESC LIMIT 3";
        try (PreparedStatement stmt = conn.prepareStatement(sqlIncreasing)) {
            stmt.setInt(1, investorId);
            ResultSet rs = stmt.executeQuery();
            
            List<BigDecimal> recentAmounts = new ArrayList<>();
            while (rs.next()) {
                recentAmounts.add(rs.getBigDecimal("amount"));
            }
            
            if (recentAmounts.size() >= 2 && isRapidlyIncreasing(recentAmounts)) {
                riskScore += 0.4;
                riskFactors.add(new FraudRiskFactor("RAPID_INCREASE", 
                    "Augmentation rapide des montants", 0.4));
            }
        }
        
        // Vérifier les investissements multiples sur le même projet
        String sqlSameProject = "SELECT COUNT(*) as count FROM investment WHERE investor_id = ? AND project_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sqlSameProject)) {
            stmt.setInt(1, investorId);
            stmt.setInt(2, getRecentProjectId(conn, investorId));
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt("count") > 3) {
                riskScore += 0.3;
                riskFactors.add(new FraudRiskFactor("MULTIPLE_SAME_PROJECT", 
                    "Investissements multiples sur le même projet", 0.3));
            }
        }
        
        return riskScore;
    }
    
    /**
     * Analyse le profil de l'investisseur
     */
    private double analyzeInvestorProfile(Connection conn, int investorId, List<FraudRiskFactor> riskFactors) throws SQLException {
        double riskScore = 0.0;
        
        String sql = "SELECT created_at, max_budget, years_experience FROM user WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investorId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Compte récent
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null && isRecentAccount(createdAt)) {
                    riskScore += 0.5;
                    riskFactors.add(new FraudRiskFactor("RECENT_ACCOUNT", 
                        "Compte utilisateur récent", 0.5));
                }
                
                // Budget maximum non défini ou suspect
                BigDecimal maxBudget = rs.getBigDecimal("max_budget");
                if (maxBudget == null || maxBudget.compareTo(new BigDecimal("1000000.00")) > 0) {
                    riskScore += 0.3;
                    riskFactors.add(new FraudRiskFactor("UNREALISTIC_BUDGET", 
                        "Budget maximum irréaliste", 0.3));
                }
                
                // Expérience incompatible avec les montants
                int yearsExperience = rs.getInt("years_experience");
                if (yearsExperience < 1 && hasHighValueInvestments(conn, investorId)) {
                    riskScore += 0.4;
                    riskFactors.add(new FraudRiskFactor("EXPERIENCE_MISMATCH", 
                        "Expérience faible mais investissements élevés", 0.4));
                }
            }
        }
        
        return riskScore;
    }
    
    /**
     * Analyse la corrélation avec d'autres investisseurs
     */
    private double analyzeInvestorCorrelation(Connection conn, int investorId, int projectId, List<FraudRiskFactor> riskFactors) throws SQLException {
        double riskScore = 0.0;
        
        // Vérifier si plusieurs nouveaux comptes investissent sur le même projet
        String sql = "SELECT COUNT(*) as count FROM investment i " +
                    "JOIN user u ON i.investor_id = u.user_id " +
                    "WHERE i.project_id = ? AND u.created_at > DATE_SUB(NOW(), INTERVAL 7 DAY)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt("count") > 5) {
                riskScore += 0.6;
                riskFactors.add(new FraudRiskFactor("COORDINATED_INVESTMENT", 
                    "Investissements coordonnés de nouveaux comptes", 0.6));
            }
        }
        
        return riskScore;
    }
    
    /**
     * Détermine le niveau de risque
     */
    private FraudRiskLevel determineRiskLevel(double riskScore) {
        if (riskScore >= 1.5) return FraudRiskLevel.HIGH;
        if (riskScore >= 0.8) return FraudRiskLevel.MEDIUM;
        return FraudRiskLevel.LOW;
    }
    
    /**
     * Enregistre l'analyse de fraude
     */
    private void recordFraudAnalysis(Connection conn, int investmentId, int investorId, 
                                   double riskScore, FraudRiskLevel riskLevel, List<FraudRiskFactor> riskFactors) throws SQLException {
        String sql = "INSERT INTO fraud_analysis (investment_id, investor_id, risk_score, risk_level, analysis_date, risk_factors) " +
                    "VALUES (?, ?, ?, ?, NOW(), ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investmentId);
            stmt.setInt(2, investorId);
            stmt.setDouble(3, riskScore);
            stmt.setString(4, riskLevel.name());
            
            // Sérialiser les facteurs de risque en JSON
            String factorsJson = riskFactors.toString();
            stmt.setString(5, factorsJson);
            
            stmt.executeUpdate();
        }
    }
    
    /**
     * Génère une alerte de fraude
     */
    private void generateFraudAlert(Connection conn, int investmentId, int investorId, 
                                  FraudRiskLevel riskLevel, List<FraudRiskFactor> riskFactors) throws SQLException {
        String sql = "INSERT INTO fraud_alert (investment_id, investor_id, alert_level, alert_message, created_at, status) " +
                    "VALUES (?, ?, ?, ?, NOW(), 'PENDING')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investmentId);
            stmt.setInt(2, investorId);
            stmt.setString(3, riskLevel.name());
            
            String message = generateAlertMessage(riskLevel, riskFactors);
            stmt.setString(4, message);
            
            stmt.executeUpdate();
        }
        
        logger.info("ALERTE FRAUDE générée - Investissement ID: " + investmentId + ", Niveau: " + riskLevel);
    }
    
    // Méthodes utilitaires
    
    private boolean isRoundNumber(BigDecimal amount) {
        return amount.remainder(new BigDecimal("1000.00")).compareTo(BigDecimal.ZERO) == 0;
    }
    
    private boolean isRapidlyIncreasing(List<BigDecimal> amounts) {
        if (amounts.size() < 2) return false;
        
        for (int i = 0; i < amounts.size() - 1; i++) {
            BigDecimal current = amounts.get(i);
            BigDecimal previous = amounts.get(i + 1);
            
            if (previous.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal increase = current.subtract(previous);
                BigDecimal ratio = increase.divide(previous, 2, RoundingMode.HALF_UP);
                
                if (ratio.compareTo(new BigDecimal("2.0")) > 0) { // Augmentation > 200%
                    return true;
                }
            }
        }
        return false;
    }
    
    private int getRecentProjectId(Connection conn, int investorId) throws SQLException {
        String sql = "SELECT project_id FROM investment WHERE investor_id = ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investorId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("project_id");
            }
        }
        return 0;
    }
    
    private boolean isRecentAccount(Timestamp createdAt) {
        LocalDateTime accountTime = createdAt.toLocalDateTime();
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        return accountTime.isAfter(oneWeekAgo);
    }
    
    private boolean hasHighValueInvestments(Connection conn, int investorId) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM investment WHERE investor_id = ? AND amount > 50000";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investorId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt("count") > 0;
        }
    }
    
    private String generateAlertMessage(FraudRiskLevel riskLevel, List<FraudRiskFactor> riskFactors) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALERTE FRAUDE - Niveau: ").append(riskLevel.name()).append("\n");
        sb.append("Facteurs de risque détectés:\n");
        
        for (FraudRiskFactor factor : riskFactors) {
            sb.append("• ").append(factor.getDescription()).append("\n");
        }
        
        return sb.toString();
    }
    
    // Classes internes
    
    public static class FraudAnalysisResult {
        private final double riskScore;
        private final FraudRiskLevel riskLevel;
        private final List<FraudRiskFactor> riskFactors;
        
        public FraudAnalysisResult(double riskScore, FraudRiskLevel riskLevel, List<FraudRiskFactor> riskFactors) {
            this.riskScore = riskScore;
            this.riskLevel = riskLevel;
            this.riskFactors = riskFactors;
        }
        
        // Getters
        public double getRiskScore() { return riskScore; }
        public FraudRiskLevel getRiskLevel() { return riskLevel; }
        public List<FraudRiskFactor> getRiskFactors() { return riskFactors; }
        public boolean isSuspicious() { return riskLevel != FraudRiskLevel.LOW; }
    }
    
    public static class FraudRiskFactor {
        private final String type;
        private final String description;
        private final double weight;
        
        public FraudRiskFactor(String type, String description, double weight) {
            this.type = type;
            this.description = description;
            this.weight = weight;
        }
        
        // Getters
        public String getType() { return type; }
        public String getDescription() { return description; }
        public double getWeight() { return weight; }
        
        @Override
        public String toString() {
            return String.format("{\"type\":\"%s\",\"description\":\"%s\",\"weight\":%.2f}", type, description, weight);
        }
    }
    
    public enum FraudRiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
