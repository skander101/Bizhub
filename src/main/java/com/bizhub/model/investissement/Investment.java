package com.bizhub.model.investissement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Investment {
    private Integer investmentId;
    private Integer projectId;
    private Integer investorId;
    private BigDecimal amount;
    private LocalDateTime investmentDate;
    private String contractUrl;

    // Attributs pour les jointures
    private String projectTitle;
    private String investorName;
    private BigDecimal requiredBudget;
    private String projectStatus;

    // Constructeur par défaut
    public Investment() {
    }

    // Constructeur pour création (sans ID)
    public Investment(Integer projectId, Integer investorId, BigDecimal amount, String contractUrl) {
        this.projectId = projectId;
        this.investorId = investorId;
        this.amount = amount;
        this.contractUrl = contractUrl;
        this.investmentDate = LocalDateTime.now();
    }

    // Constructeur complet
    public Investment(Integer investmentId, Integer projectId, Integer investorId,
                      BigDecimal amount, LocalDateTime investmentDate, String contractUrl) {
        this.investmentId = investmentId;
        this.projectId = projectId;
        this.investorId = investorId;
        this.amount = amount;
        this.investmentDate = investmentDate;
        this.contractUrl = contractUrl;
    }

    // Getters
    public Integer getInvestmentId() {
        return investmentId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public Integer getInvestorId() {
        return investorId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getInvestmentDate() {
        return investmentDate;
    }

    public String getContractUrl() {
        return contractUrl;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public String getInvestorName() {
        return investorName;
    }

    public BigDecimal getRequiredBudget() {
        return requiredBudget;
    }

    public String getProjectStatus() {
        return projectStatus;
    }

    // Setters
    public void setInvestmentId(Integer investmentId) {
        this.investmentId = investmentId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public void setInvestorId(Integer investorId) {
        this.investorId = investorId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setInvestmentDate(LocalDateTime investmentDate) {
        this.investmentDate = investmentDate;
    }

    public void setContractUrl(String contractUrl) {
        this.contractUrl = contractUrl;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public void setInvestorName(String investorName) {
        this.investorName = investorName;
    }

    public void setRequiredBudget(BigDecimal requiredBudget) {
        this.requiredBudget = requiredBudget;
    }

    public void setProjectStatus(String projectStatus) {
        this.projectStatus = projectStatus;
    }

    @Override
    public String toString() {
        return "Investment{" +
                "investmentId=" + investmentId +
                ", projectId=" + projectId +
                ", investorId=" + investorId +
                ", amount=" + amount +
                ", investmentDate=" + investmentDate +
                ", projectTitle='" + projectTitle + '\'' +
                ", investorName='" + investorName + '\'' +
                '}';
    }
}

