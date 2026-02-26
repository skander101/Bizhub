package com.bizhub.model.services.marketplace.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StripeGatewayClient {

    private static final Logger LOGGER = Logger.getLogger(StripeGatewayClient.class.getName());

    private final String secretKey;
    private final String currency;
    private final String successUrl;
    private final String cancelUrl;

    public StripeGatewayClient() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("stripe.properties")) {
            if (in == null)
                throw new IllegalStateException("stripe.properties introuvable dans src/main/resources/");
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire stripe.properties : " + e.getMessage(), e);
        }

        this.secretKey  = props.getProperty("stripe.secret.key", "").trim();
        this.currency   = props.getProperty("stripe.currency", "eur").trim();
        this.successUrl = props.getProperty("stripe.success.url", "https://bizhub.app/succes").trim();
        this.cancelUrl  = props.getProperty("stripe.cancel.url", "https://bizhub.app/annule").trim();

        if (secretKey.isEmpty())
            throw new IllegalStateException("stripe.secret.key vide dans stripe.properties");
    }

    public PaymentResult createStripeCheckout(int orderId,
                                              String productName,
                                              int quantity,
                                              long unitCentimes) {
        try {
            Stripe.apiKey = secretKey;

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)

                    // ✅ double sécurité : metadata + client_reference_id
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