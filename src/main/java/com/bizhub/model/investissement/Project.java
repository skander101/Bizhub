package com.bizhub.model.investissement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Project {

    private Integer projectId;
    private Integer startupId;
    private String title;
    private String description;
    private BigDecimal requiredBudget;
    private String status; // pending, funded, in_progress, complete
    private LocalDateTime createdAt;
    private String websiteUrl;
    private String logoUrl;

    // Champs calculés (pour l'affichage)
    private BigDecimal totalInvested = BigDecimal.ZERO;
    private Integer investmentsCount = 0;

    // Optionnel: liste investissements
    private List<Investment> investments = new ArrayList<>();

    public Project() {}

    public Project(Integer projectId, Integer startupId, String title, String description,
                   BigDecimal requiredBudget, String status, LocalDateTime createdAt) {
        this.projectId = projectId;
        this.startupId = startupId;
        this.title = title;
        this.description = description;
        this.requiredBudget = requiredBudget;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Integer getProjectId() { return projectId; }
    public Integer getStartupId() { return startupId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getRequiredBudget() { return requiredBudget; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getWebsiteUrl() { return websiteUrl; }
    public String getLogoUrl() { return logoUrl; }

    public BigDecimal getTotalInvested() { return totalInvested; }
    public Integer getInvestmentsCount() { return investmentsCount; }
    public List<Investment> getInvestments() { return investments; }

    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public void setStartupId(Integer startupId) { this.startupId = startupId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setRequiredBudget(BigDecimal requiredBudget) { this.requiredBudget = requiredBudget; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public void setTotalInvested(BigDecimal totalInvested) {
        this.totalInvested = (totalInvested == null) ? BigDecimal.ZERO : totalInvested;
    }

    public void setInvestmentsCount(Integer investmentsCount) {
        this.investmentsCount = (investmentsCount == null) ? 0 : investmentsCount;
    }

    public void setInvestments(List<Investment> investments) {
        this.investments = (investments == null) ? new ArrayList<>() : investments;
    }

    @Override
    public String toString() {
        return "Project{" +
                "projectId=" + projectId +
                ", startupId=" + startupId +
                ", title='" + title + '\'' +
                ", requiredBudget=" + requiredBudget +
                ", status='" + status + '\'' +
                '}';
    }
}

