package com.bizhub.model.services.investissement.AI;

import com.bizhub.model.services.common.DB.MyDatabase;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Système de Recommandation Intelligent pour Investissements
 * Utilise des algorithmes de filtrage collaboratif et basé sur le contenu
 */
public class RecommendationEngine {
    
    private static final Logger logger = Logger.getLogger(RecommendationEngine.class.getName());
    
    /**
     * Calcule les recommandations de projets pour un investisseur
     * @param investorId ID de l'investisseur
     * @param limit Nombre maximum de recommandations
     * @return Liste des projets recommandés avec scores
     */
    public List<ProjectRecommendation> getProjectRecommendations(int investorId, int limit) {
        List<ProjectRecommendation> recommendations = new ArrayList<>();
        
        try (Connection conn = MyDatabase.getInstance().getCnx()) {
            // 1. Obtenir le profil de l'investisseur
            InvestorProfile profile = getInvestorProfile(conn, investorId);
            
            // 2. Obtenir les projets disponibles
            List<Project> availableProjects = getAvailableProjects(conn, investorId);
            
            // 3. Calculer les scores de recommandation
            for (Project project : availableProjects) {
                double score = calculateRecommendationScore(profile, project);
                if (score > 0.3) { // Seuil minimum de pertinence
                    recommendations.add(new ProjectRecommendation(project, score));
                }
            }
            
            // 4. Trier par score et limiter les résultats
            recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            if (recommendations.size() > limit) {
                recommendations = recommendations.subList(0, limit);
            }
            
        } catch (SQLException e) {
            logger.severe("Erreur lors du calcul des recommandations: " + e.getMessage());
        }
        
        return recommendations;
    }
    
    /**
     * Obtient le profil d'un investisseur depuis la base de données
     */
    private InvestorProfile getInvestorProfile(Connection conn, int investorId) throws SQLException {
        String sql = "SELECT investment_sector, max_budget, years_experience FROM user WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investorId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new InvestorProfile(
                    investorId,
                    rs.getString("investment_sector"),
                    rs.getBigDecimal("max_budget"),
                    rs.getInt("years_experience")
                );
            }
        }
        return new InvestorProfile(investorId, null, null, 0);
    }
    
    /**
     * Obtient la liste des projets disponibles (non investis par l'utilisateur)
     */
    private List<Project> getAvailableProjects(Connection conn, int investorId) throws SQLException {
        List<Project> projects = new ArrayList<>();
        
        String sql = "SELECT p.project_id, p.title, p.description, p.required_budget, p.status, " +
                    "p.startup_id, u.company_name, u.sector " +
                    "FROM project p " +
                    "JOIN user u ON p.startup_id = u.user_id " +
                    "WHERE p.status IN ('pending', 'in_progress') " +
                    "AND p.project_id NOT IN (SELECT project_id FROM investment WHERE investor_id = ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investorId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                projects.add(new Project(
                    rs.getInt("project_id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getBigDecimal("required_budget"),
                    rs.getString("status"),
                    rs.getInt("startup_id"),
                    rs.getString("company_name"),
                    rs.getString("sector")
                ));
            }
        }
        
        return projects;
    }
    
    /**
     * Calcule le score de recommandation pour un projet
     * Score = 0.0 à 1.0
     */
    private double calculateRecommendationScore(InvestorProfile profile, Project project) {
        double score = 0.0;
        
        // 1. Correspondance de secteur (poids: 40%)
        if (profile.getSector() != null && project.getSector() != null) {
            if (profile.getSector().equalsIgnoreCase(project.getSector())) {
                score += 0.4;
            } else if (isRelatedSector(profile.getSector(), project.getSector())) {
                score += 0.2;
            }
        }
        
        // 2. Adéquation budget (poids: 30%)
        if (profile.getMaxBudget() != null && project.getRequiredBudget() != null) {
            double budgetRatio = project.getRequiredBudget().doubleValue() / profile.getMaxBudget().doubleValue();
            if (budgetRatio <= 0.5) {
                score += 0.3; // Projet très accessible
            } else if (budgetRatio <= 0.8) {
                score += 0.2; // Projet accessible
            } else if (budgetRatio <= 1.0) {
                score += 0.1; // Projet limite budget
            }
        }
        
        // 3. Expérience correspondante (poids: 20%)
        if (profile.getYearsExperience() > 0) {
            if (profile.getYearsExperience() >= 5 && project.getRequiredBudget().doubleValue() > 10000) {
                score += 0.2; // Investisseur expérimenté pour projet important
            } else if (profile.getYearsExperience() < 3 && project.getRequiredBudget().doubleValue() < 5000) {
                score += 0.2; // Investisseur débutant pour projet modeste
            }
        }
        
        // 4. Facteur de popularité (poids: 10%)
        score += calculatePopularityFactor(project.getProjectId());
        
        return Math.min(score, 1.0);
    }
    
    /**
     * Vérifie si deux secteurs sont liés
     */
    private boolean isRelatedSector(String sector1, String sector2) {
        // Mapping des secteurs liés
        Map<String, List<String>> relatedSectors = Map.of(
            "Technology", Arrays.asList("Software", "IT", "Digital", "Informatics"),
            "Finance", Arrays.asList("FinTech", "Banking", "Insurance", "Investment"),
            "Healthcare", Arrays.asList("Medical", "Pharma", "Biotech", "Wellness")
        );
        
        for (List<String> related : relatedSectors.values()) {
            if (related.contains(sector1) && related.contains(sector2)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Calcule un facteur de popularité basé sur le nombre d'investissements existants
     */
    private double calculatePopularityFactor(int projectId) {
        try (Connection conn = MyDatabase.getInstance().getCnx()) {
            String sql = "SELECT COUNT(*) as investment_count FROM investment WHERE project_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, projectId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int count = rs.getInt("investment_count");
                    // Plus de popularité = plus de confiance, mais avec saturation
                    return Math.min(count * 0.05, 0.1);
                }
            }
        } catch (SQLException e) {
            logger.warning("Erreur calcul popularité: " + e.getMessage());
        }
        return 0.0;
    }
    
    // Classes internes pour la structure des données
    
    public static class ProjectRecommendation {
        private final Project project;
        private final double score;
        
        public ProjectRecommendation(Project project, double score) {
            this.project = project;
            this.score = score;
        }
        
        // Getters
        public Project getProject() { return project; }
        public double getScore() { return score; }
    }
    
    public static class InvestorProfile {
        private final int investorId;
        private final String sector;
        private final java.math.BigDecimal maxBudget;
        private final int yearsExperience;
        
        public InvestorProfile(int investorId, String sector, java.math.BigDecimal maxBudget, int yearsExperience) {
            this.investorId = investorId;
            this.sector = sector;
            this.maxBudget = maxBudget;
            this.yearsExperience = yearsExperience;
        }
        
        // Getters
        public int getInvestorId() { return investorId; }
        public String getSector() { return sector; }
        public java.math.BigDecimal getMaxBudget() { return maxBudget; }
        public int getYearsExperience() { return yearsExperience; }
    }
    
    public static class Project {
        private final int projectId;
        private final String title;
        private final String description;
        private final java.math.BigDecimal requiredBudget;
        private final String status;
        private final int startupId;
        private final String companyName;
        private final String sector;
        
        public Project(int projectId, String title, String description, java.math.BigDecimal requiredBudget, 
                      String status, int startupId, String companyName, String sector) {
            this.projectId = projectId;
            this.title = title;
            this.description = description;
            this.requiredBudget = requiredBudget;
            this.status = status;
            this.startupId = startupId;
            this.companyName = companyName;
            this.sector = sector;
        }
        
        // Getters
        public int getProjectId() { return projectId; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public java.math.BigDecimal getRequiredBudget() { return requiredBudget; }
        public String getStatus() { return status; }
        public int getStartupId() { return startupId; }
        public String getCompanyName() { return companyName; }
        public String getSector() { return sector; }
    }
}
