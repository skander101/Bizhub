package com.bizhub.model.marketplace;

public class CommandeJoinProduit {
    private boolean autoConfirmRecommended;
    private String autoReason;

    public boolean isAutoConfirmRecommended() { return autoConfirmRecommended; }
    public void setAutoConfirmRecommended(boolean v) { this.autoConfirmRecommended = v; }

    public String getAutoReason() { return autoReason; }
    public void setAutoReason(String autoReason) { this.autoReason = autoReason; }

    private int idCommande;
    private int idClient;
    private int idProduit;
    private String produitNom;
    private int quantiteCommande;
    private String statut;
    private int priorityScore;
    private String priorityLabel;

    // Optionnel (recommandé) si tu as une date en DB :
    private java.util.Date dateCommande;

    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public int getQuantiteCommande() { return quantiteCommande; }
    public void setQuantiteCommande(int quantiteCommande) { this.quantiteCommande = quantiteCommande; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }

    public String getPriorityLabel() { return priorityLabel; }
    public void setPriorityLabel(String priorityLabel) { this.priorityLabel = priorityLabel; }

    public java.util.Date getDateCommande() { return dateCommande; }
    public void setDateCommande(java.util.Date dateCommande) { this.dateCommande = dateCommande; }
}
