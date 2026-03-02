package com.bizhub.model.marketplace;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CommandeJoinProduit {

    private int idCommande;
    private int idClient;
    private int idProduit;
    private int quantiteCommande;
    private String statut;
    private String produitNom;
    private LocalDateTime dateCommande;

    // Paiement
    private String paymentStatus;
    private String paymentRef;
    private String paymentUrl;
    private boolean estPayee;
    private LocalDateTime paidAt;

    // ✅ Totaux (remplis pour les commandes panier)
    private BigDecimal totalHt;
    private BigDecimal totalTva;
    private BigDecimal totalTtc;

    // IA Priorité
    private int priorityScore;
    private String priorityLabel;
    private boolean autoConfirmRecommended;
    private String autoReason;

    // Getters/Setters
    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int v) { this.idCommande = v; }
    public int getIdClient() { return idClient; }
    public void setIdClient(int v) { this.idClient = v; }
    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int v) { this.idProduit = v; }
    public int getQuantiteCommande() { return quantiteCommande; }
    public void setQuantiteCommande(int v) { this.quantiteCommande = v; }
    public String getStatut() { return statut; }
    public void setStatut(String v) { this.statut = v; }
    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String v) { this.produitNom = v; }
    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime v) { this.dateCommande = v; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String v) { this.paymentStatus = v; }
    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String v) { this.paymentRef = v; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String v) { this.paymentUrl = v; }
    public boolean isEstPayee() { return estPayee; }
    public void setEstPayee(boolean v) { this.estPayee = v; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime v) { this.paidAt = v; }

    // ✅ Totaux
    public BigDecimal getTotalHt()  { return totalHt; }
    public void setTotalHt(BigDecimal v)  { this.totalHt = v; }
    public BigDecimal getTotalTva() { return totalTva; }
    public void setTotalTva(BigDecimal v) { this.totalTva = v; }
    public BigDecimal getTotalTtc() { return totalTtc; }
    public void setTotalTtc(BigDecimal v) { this.totalTtc = v; }

    // IA
    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int v) { this.priorityScore = v; }
    public String getPriorityLabel() { return priorityLabel; }
    public void setPriorityLabel(String v) { this.priorityLabel = v; }
    public boolean isAutoConfirmRecommended() { return autoConfirmRecommended; }
    public void setAutoConfirmRecommended(boolean v) { this.autoConfirmRecommended = v; }
    public String getAutoReason() { return autoReason; }
    public void setAutoReason(String v) { this.autoReason = v; }
}