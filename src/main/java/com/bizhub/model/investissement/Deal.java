package com.bizhub.model.investissement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Deal {

    private Integer dealId;
    private Integer negotiationId;
    private Integer projectId;
    private Integer buyerId;
    private Integer sellerId;
    private BigDecimal amount;
    private String stripePaymentIntentId;
    private String stripePaymentStatus;
    private String stripeCheckoutSessionId;
    private String contractPdfPath;
    private String yousignSignatureRequestId;
    private String yousignStatus;
    private boolean emailSent;
    private String status; // pending_payment, paid, pending_signature, signed, completed, cancelled
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    private String projectTitle;
    private String buyerName;
    private String buyerEmail;
    private String sellerName;
    private String sellerEmail;

    public Deal() {}

    public Deal(Integer negotiationId, Integer projectId, Integer buyerId,
                Integer sellerId, BigDecimal amount) {
        this.negotiationId = negotiationId;
        this.projectId = projectId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.status = "pending_payment";
        this.stripePaymentStatus = "pending";
        this.yousignStatus = "pending";
        this.createdAt = LocalDateTime.now();
    }

    public Integer getDealId() { return dealId; }
    public void setDealId(Integer dealId) { this.dealId = dealId; }

    public Integer getNegotiationId() { return negotiationId; }
    public void setNegotiationId(Integer negotiationId) { this.negotiationId = negotiationId; }

    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }

    public Integer getBuyerId() { return buyerId; }
    public void setBuyerId(Integer buyerId) { this.buyerId = buyerId; }

    public Integer getSellerId() { return sellerId; }
    public void setSellerId(Integer sellerId) { this.sellerId = sellerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String s) { this.stripePaymentIntentId = s; }

    public String getStripePaymentStatus() { return stripePaymentStatus; }
    public void setStripePaymentStatus(String s) { this.stripePaymentStatus = s; }

    public String getStripeCheckoutSessionId() { return stripeCheckoutSessionId; }
    public void setStripeCheckoutSessionId(String s) { this.stripeCheckoutSessionId = s; }

    public String getContractPdfPath() { return contractPdfPath; }
    public void setContractPdfPath(String s) { this.contractPdfPath = s; }

    public String getYousignSignatureRequestId() { return yousignSignatureRequestId; }
    public void setYousignSignatureRequestId(String s) { this.yousignSignatureRequestId = s; }

    public String getYousignStatus() { return yousignStatus; }
    public void setYousignStatus(String s) { this.yousignStatus = s; }

    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getSellerEmail() { return sellerEmail; }
    public void setSellerEmail(String sellerEmail) { this.sellerEmail = sellerEmail; }
}

