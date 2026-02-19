package com.bizhub.model.marketplace;

public class CommandeJoinProduit {

    private int idCommande;
    private int idClient;
    private int idProduit;
    private String produitNom;
    private int quantiteCommande;
    private String statut;

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
}
