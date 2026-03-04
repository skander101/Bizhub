package com.bizhub.model.investissement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {
    private Integer paymentId;
    private Integer investmentId;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String paymentStatus; // pending, completed, failed, refunded
    private String transactionReference;
    private String notes;

    // Attributs pour les jointures
    private BigDecimal investmentTotal;
    private String projectTitle;
    private String investorName;

    // Constructeur par défaut
    public Payment() {
    }

    // Constructeur pour création (sans ID)
    public Payment(Integer investmentId, BigDecimal amount, String paymentMethod,
                   String paymentStatus, String transactionReference, String notes) {
        this.investmentId = investmentId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.transactionReference = transactionReference;
        this.notes = notes;
        this.paymentDate = LocalDateTime.now();
    }

    // Constructeur complet
    public Payment(Integer paymentId, Integer investmentId, BigDecimal amount,
                   LocalDateTime paymentDate, String paymentMethod, String paymentStatus,
                   String transactionReference, String notes) {
        this.paymentId = paymentId;
        this.investmentId = investmentId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.transactionReference = transactionReference;
        this.notes = notes;
    }

    // Getters
    public Integer getPaymentId() {
        return paymentId;
    }

    public Integer getInvestmentId() {
        return investmentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getNotes() {
        return notes;
    }

    public BigDecimal getInvestmentTotal() {
        return investmentTotal;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public String getInvestorName() {
        return investorName;
    }

    // Setters
    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public void setInvestmentId(Integer investmentId) {
        this.investmentId = investmentId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setInvestmentTotal(BigDecimal investmentTotal) {
        this.investmentTotal = investmentTotal;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public void setInvestorName(String investorName) {
        this.investorName = investorName;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId=" + paymentId +
                ", investmentId=" + investmentId +
                ", amount=" + amount +
                ", paymentDate=" + paymentDate +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", projectTitle='" + projectTitle + '\'' +
                '}';
    }
}

