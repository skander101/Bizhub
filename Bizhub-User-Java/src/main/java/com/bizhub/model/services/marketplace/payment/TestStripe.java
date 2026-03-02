package com.bizhub.model.services.marketplace.payment;

import com.bizhub.model.marketplace.CommandeJoinProduit;

public class TestStripe {

    public static void main(String[] args) {

        System.out.println("═══════════════════════════════════════");
        System.out.println("  TEST STRIPE — BizHub Marketplace");
        System.out.println("═══════════════════════════════════════\n");

        System.out.println("📦 Étape 1 — Création commande test...");
        CommandeJoinProduit commande = new CommandeJoinProduit();
        commande.setIdCommande(999);
        commande.setIdClient(1);
        commande.setIdProduit(1);
        commande.setProduitNom("Produit Test BizHub");
        commande.setQuantiteCommande(2);
        commande.setStatut("confirmee");
        System.out.println("   ✓ Commande créée : #999\n");

        System.out.println("💳 Étape 2 — Appel PaymentService...");
        PaymentService paymentService = new PaymentService();
        PaymentResult result = paymentService.creerEtOuvrirLienPaiement(commande);

        System.out.println("\n═══════════════════════════════════════");
        if (result.isSuccess()) {
            System.out.println("✅ SUCCÈS — API Stripe fonctionne !");
            System.out.println("   Session ID  : " + result.getRef());            System.out.println("   URL Stripe  : " + result.getPaymentUrl());
            System.out.println("\n🌐 Le navigateur devrait s'être ouvert.");
        } else {
            System.out.println("❌ ÉCHEC — Erreur détectée :");
            System.out.println("   " + result.getErrorMessage());
        }
        System.out.println("═══════════════════════════════════════");
    }
}