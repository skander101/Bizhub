package com.bizhub.model.marketplace;

import java.math.BigDecimal;

public class ProduitService {
    private int idProduit;
    private int idProfile;
    private String nom;
    private String description;
    private BigDecimal prix;
    private int quantite;
    private String categorie;
    private boolean disponible;

    public ProduitService() {}

    public ProduitService(int idProduit, int idProfile, String nom, String description,
                          BigDecimal prix, int quantite, String categorie, boolean disponible) {
        this.idProduit = idProduit;
        this.idProfile = idProfile;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.quantite = quantite;
        this.categorie = categorie;
        this.disponible = disponible;
    }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public int getIdProfile() { return idProfile; }
    public void setIdProfile(int idProfile) { this.idProfile = idProfile; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    @Override
    public String toString() {
        return nom + " (" + prix + ")";
    }
}
