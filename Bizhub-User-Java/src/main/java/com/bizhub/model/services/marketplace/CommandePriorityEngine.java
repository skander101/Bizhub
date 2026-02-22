package com.bizhub.model.services.marketplace;

import com.bizhub.model.marketplace.CommandeJoinProduit;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Moteur de priorité intelligente des commandes.
 * Objectif : aider l'investisseur à traiter les commandes dans le bon ordre
 * + proposer une auto-validation (recommandation) sur les commandes en_attente.
 */
public class CommandePriorityEngine {

    public enum PriorityLevel { HAUTE, MOYENNE, BASSE }

    // Seuils (tu peux ajuster)
    private static final int SCORE_HAUTE   = 250;
    private static final int SCORE_MOYENNE = 120;

    // ✅ Auto-validation recommandée à partir de ce score
    private static final int AUTO_CONFIRM_THRESHOLD = 350;

    /**
     * Calcule le score de priorité.
     * Plus le score est haut -> plus la commande est prioritaire.
     */
    public int computeScore(CommandeJoinProduit c) {
        if (c == null) return 0;

        int score = 0;

        // 1) Ancienneté (jours)
        int days = computeDaysOld(c);
        // +5 points par jour en attente (cap à 10 jours => max 50)
        score += Math.min(days, 10) * 5;

        // 2) Quantité (poids 2)
        int qte = Math.max(0, c.getQuantiteCommande());
        // +2 points par unité, cap à 200 unités => max 400
        score += Math.min(qte, 200) * 2;

        // 3) Statut : si c'est "en_attente" on boost, sinon on baisse
        String statut = safeLower(c.getStatut());
        if ("en_attente".equals(statut)) score += 50;
        else score -= 50;

        // 4) Heuristique simple : produit "critique" (mots-clés)
        String prod = safeLower(c.getProduitNom());
        if (prod.contains("urgent") || prod.contains("critical") || prod.contains("critique")) {
            score += 40;
        }

        return score;
    }

    public PriorityLevel computeLevel(int score) {
        if (score >= SCORE_HAUTE) return PriorityLevel.HAUTE;
        if (score >= SCORE_MOYENNE) return PriorityLevel.MOYENNE;
        return PriorityLevel.BASSE;
    }

    public String computeLabel(int score) {
        return computeLevel(score).name();
    }

    /**
     * ✅ Applique score + label + recommandation auto-validation.
     */
    public void apply(CommandeJoinProduit c) {
        if (c == null) return;

        int score = computeScore(c);
        c.setPriorityScore(score);
        c.setPriorityLabel(computeLabel(score));

        // ✅ Décision Auto-validation (recommandation)
        applyAutoDecision(c, score);
    }

    // ---------------------------------------------------------------------
    // AUTO VALIDATION INTELLIGENTE (RECOMMANDATION)
    // ---------------------------------------------------------------------

    /**
     * Recommandation : uniquement pour les commandes en_attente.
     * Exemple règles :
     * - Score >= 350 => recommandée
     * - Sinon => validation manuelle
     */
    private void applyAutoDecision(CommandeJoinProduit c, int score) {
        String statut = safeLower(c.getStatut());

        // Si pas en attente => pas de recommandation
        if (!"en_attente".equals(statut)) {
            c.setAutoConfirmRecommended(false);
            c.setAutoReason("Commande déjà traitée (statut : " + statut + ").");
            return;
        }

        // Quelques signaux explicatifs (pour une raison claire)
        int qte = Math.max(0, c.getQuantiteCommande());
        int days = computeDaysOld(c);
        String prod = safeLower(c.getProduitNom());

        boolean prodCritique = prod.contains("urgent") || prod.contains("critical") || prod.contains("critique");
        boolean grosseQte = qte >= 150;
        boolean tresAncienne = days >= 5;

        // ✅ règle principale : threshold score
        if (score >= AUTO_CONFIRM_THRESHOLD) {
            c.setAutoConfirmRecommended(true);

            // Raison détaillée et "pro" (workflow métier)
            StringBuilder reason = new StringBuilder("Auto-confirmation recommandée : ");

            reason.append("score=").append(score);

            // Ajoute des explications lisibles
            if (grosseQte) reason.append(" | grosse quantité (").append(qte).append(")");
            if (tresAncienne) reason.append(" | attente ").append(days).append(" jours");
            if (prodCritique) reason.append(" | produit critique");

            reason.append(".");

            c.setAutoReason(reason.toString());
        } else {
            c.setAutoConfirmRecommended(false);

            // Raison (guide investisseur)
            c.setAutoReason("Validation manuelle recommandée : score=" + score
                    + " (< " + AUTO_CONFIRM_THRESHOLD + ").");
        }
    }

    // ---------------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------------

    private int computeDaysOld(CommandeJoinProduit c) {
        // Si tu as un champ dateCommande (Date/Timestamp) dans CommandeJoinProduit => utilise-le.
        // Sinon, on retourne 0.
        Date d = c.getDateCommande();
        if (d == null) return 0;

        LocalDateTime created = LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
        LocalDateTime now = LocalDateTime.now();

        long days = java.time.Duration.between(created, now).toDays();
        if (days < 0) days = 0;
        return (int) days;
    }

    private String safeLower(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}