package com.bizhub.model.marketplace;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Point de données pour le graphique Performance Investisseur.
 *
 * Logique métier :
 *  confirmees  = nombre de commandes confirmées ce jour
 *  annulees    = nombre de commandes annulées ce jour
 *  montantConf = montant TTC total des commandes confirmées
 *  montantAnn  = montant TTC total des commandes annulées
 */
public class StatsPoint {

    private final LocalDate  date;
    private final int        confirmees;
    private final int        annulees;
    private final BigDecimal montantConf;
    private final BigDecimal montantAnn;

    public StatsPoint(LocalDate date,
                      int confirmees,    int annulees,
                      BigDecimal montantConf, BigDecimal montantAnn) {
        this.date        = date;
        this.confirmees  = confirmees;
        this.annulees    = annulees;
        this.montantConf = montantConf == null ? BigDecimal.ZERO : montantConf;
        this.montantAnn  = montantAnn  == null ? BigDecimal.ZERO : montantAnn;
    }

    public LocalDate  getDate()        { return date; }
    public int        getConfirmees()  { return confirmees; }
    public int        getAnnulees()    { return annulees; }
    public BigDecimal getMontantConf() { return montantConf; }
    public BigDecimal getMontantAnn()  { return montantAnn; }

    // Aliases legacy
    public BigDecimal getVentes() { return montantConf; }
    public BigDecimal getAchats() { return montantAnn; }
    public int getNbVentes()      { return confirmees; }
    public int getNbAchats()      { return annulees; }
}