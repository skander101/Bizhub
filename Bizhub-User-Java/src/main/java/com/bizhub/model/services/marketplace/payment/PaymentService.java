package com.bizhub.model.services.marketplace.payment;

import java.awt.Desktop;
import java.net.URI;
import com.bizhub.model.marketplace.CommandeJoinProduit;

public class PaymentService {

    private final StripeGatewayClient stripe = new StripeGatewayClient();

    /**
     * ✅ Crée une session Stripe Checkout SANS ouvrir le navigateur.
     * Utilisé côté investisseur (auto-confirm, onConfirmerCommande).
     *
     * Le prix est fixé à 10.00 EUR par unité (1000 centimes) par défaut.
     * Pour utiliser le vrai prix : ajouter getPrix() à CommandeJoinProduit
     * et appeler stripe.createStripeCheckout(..., prixCentimes).
     */
    public PaymentResult initiateStripeCheckout(CommandeJoinProduit c) {
        if (c == null) return PaymentResult.fail("Commande null");

        // Prix par défaut 10.00 EUR = 1000 centimes
        // TODO : remplacer par le vrai prix quand CommandeJoinProduit aura getPrix()
        long unitCentimes = 1000L;

        return stripe.createStripeCheckout(
                c.getIdCommande(),
                c.getProduitNom(),
                c.getQuantiteCommande(),
                unitCentimes
        );
    }

    /**
     * ✅ Crée session Stripe ET ouvre le navigateur.
     * Utilisé côté startup (onPayerCommande dans CommandeController).
     */
    public PaymentResult creerEtOuvrirLienPaiement(CommandeJoinProduit c) {
        PaymentResult res = initiateStripeCheckout(c);
        if (!res.isSuccess()) return res;

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(res.getPaymentUrl()));
            }
            return res;
        } catch (Exception e) {
            return PaymentResult.fail(
                    "Lien Stripe créé mais impossible d'ouvrir le navigateur : " + e.getMessage());
        }
    }

    /**
     * ✅ Ouvre une URL existante dans le navigateur.
     * Utilisé quand l'URL est déjà stockée en DB (évite de créer une 2e session Stripe).
     */
    public void openInBrowser(String url) {
        try {
            if (url == null || url.isBlank()) return;
            if (!Desktop.isDesktopSupported())
                throw new IllegalStateException("Desktop non supporté sur cette machine.");
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'ouvrir le navigateur : " + e.getMessage(), e);
        }
    }
}
