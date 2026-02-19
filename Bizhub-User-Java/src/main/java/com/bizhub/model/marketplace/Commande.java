package com.bizhub.model.marketplace;

import java.time.LocalDateTime;

public class Commande {
    private int idCommande;
    private int idClient;
    private int idProduit;
    private int quantite;
    private LocalDateTime dateCommande;
    private String statut; // en_attente, confirmee, livree, annule

    public Commande() {}

    public Commande(int idCommande, int idClient, int idProduit, int quantite,
                    LocalDateTime dateCommande, String statut) {
        this.idCommande = idCommande;
        this.idClient = idClient;
        this.idProduit = idProduit;
        this.quantite = quantite;
        this.dateCommande = dateCommande;
        this.statut = statut;
    }

    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
