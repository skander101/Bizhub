package com.bizhub.model.investissement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Negotiation {

    private Integer negotiationId;
    private Integer projectId;
    private Integer investorId;
    private Integer startupId;
    private String status; // open, accepted, rejected, expired
    private BigDecimal proposedAmount;
    private BigDecimal finalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String projectTitle;
    private String investorName;
    private String startupName;

    public Negotiation() {}

    public Negotiation(Integer projectId, Integer investorId, Integer startupId, BigDecimal proposedAmount) {
        this.projectId = projectId;
        this.investorId = investorId;
        this.startupId = startupId;
        this.proposedAmount = proposedAmount;
        this.status = "open";
        this.createdAt = LocalDateTime.now();
    }

    public Integer getNegotiationId() { return negotiationId; }
    public void setNegotiationId(Integer negotiationId) { this.negotiationId = negotiationId; }

    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }

    public Integer getInvestorId() { return investorId; }
    public void setInvestorId(Integer investorId) { this.investorId = investorId; }

    public Integer getStartupId() { return startupId; }
    public void setStartupId(Integer startupId) { this.startupId = startupId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getProposedAmount() { return proposedAmount; }
    public void setProposedAmount(BigDecimal proposedAmount) { this.proposedAmount = proposedAmount; }

    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getInvestorName() { return investorName; }
    public void setInvestorName(String investorName) { this.investorName = investorName; }

    public String getStartupName() { return startupName; }
    public void setStartupName(String startupName) { this.startupName = startupName; }
}

