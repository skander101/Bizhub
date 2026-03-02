package com.bizhub.model.services.marketplace.payment;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.bizhub.model.services.common.service.EnvConfig;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

public class StripeGatewayClient {

    private static final Logger LOGGER = Logger.getLogger(StripeGatewayClient.class.getName());

    private final String secretKey;
    private final String currency;
    private final String successUrl;
    private final String cancelUrl;

    public StripeGatewayClient() {

        // ✅ Lire depuis .env (via EnvLoader)
        this.secretKey  = EnvConfig.getRequired("STRIPE_SECRET_KEY");
        this.currency   = EnvConfig.getOrDefault("STRIPE_CURRENCY",    "eur");
        this.successUrl = EnvConfig.getOrDefault("STRIPE_SUCCESS_URL", "http://localhost/success");
        this.cancelUrl  = EnvConfig.getOrDefault("STRIPE_CANCEL_URL",  "http://localhost/cancel");

        Stripe.apiKey = secretKey;
        LOGGER.info("✅ StripeGatewayClient initialisé (currency=" + currency + ")");
    }

    public PaymentResult createStripeCheckout(int orderId,
                                              String productName,
                                              int quantity,
                                              long unitCentimes) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .putMetadata("orderId", String.valueOf(orderId))
                    .setClientReferenceId(String.valueOf(orderId))
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity((long) quantity)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currency)
                                                    .setUnitAmount(unitCentimes)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(safe(productName).isEmpty()
                                                                            ? "Commande #" + orderId
                                                                            : safe(productName))
                                                                    .setDescription("BizHub — Commande #" + orderId)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            LOGGER.info("Stripe session créée : " + session.getId()
                    + " | orderId=" + orderId
                    + " | montant=" + (unitCentimes * quantity / 100.0)
                    + " " + currency.toUpperCase());

            return PaymentResult.ok(session.getId(), session.getUrl());

        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Erreur Stripe API", e);
            return PaymentResult.fail("Stripe : " + e.getUserMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur StripeGatewayClient", e);
            return PaymentResult.fail("Erreur : " + e.getMessage());
        }
    }

    public PaymentResult createStripeCheckout(int orderId, String productName, int quantity) {
        return createStripeCheckout(orderId, productName, quantity, 1000L);
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}