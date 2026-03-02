package com.bizhub.model.marketplace;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PanierItem {

    private int idPanier;
    private int idClient;
    private int idProduit;
    private int quantite;
    private LocalDateTime dateAjout;

    // JOIN produit_service
    private String produitNom;
    private BigDecimal prixUnitaire;   // prix HT unitaire
    private String categorie;
    private boolean disponible;

    public PanierItem() {}

    // ── Calculs ──

    /** Prix HT total = prixUnitaire × quantite */
    public BigDecimal getPrixHT() {
        if (prixUnitaire == null) return BigDecimal.ZERO;
        return prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }

    /** TVA 19% (Tunisie) */
    public BigDecimal getTVA() {
        return getPrixHT().multiply(new BigDecimal("0.19"))
                .setScale(3, java.math.RoundingMode.HALF_UP);
    }

    /** Prix TTC = HT + TVA */
    public BigDecimal getPrixTTC() {
        return getPrixHT().add(getTVA())
                .setScale(3, java.math.RoundingMode.HALF_UP);
    }

    // ── Getters / Setters ──

    public int getIdPanier()  { return idPanier; }
    public void setIdPanier(int idPanier) { this.idPanier = idPanier; }

    public int getIdClient()  { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public int getQuantite()  { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public LocalDateTime getDateAjout() { return dateAjout; }
    public void setDateAjout(LocalDateTime dateAjout) { this.dateAjout = dateAjout; }

    public String getProduitNom()  { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
}