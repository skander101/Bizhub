package com.bizhub.model.services.investissement;

import com.bizhub.model.services.common.config.ApiConfig;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import javafx.concurrent.Task;

import java.math.BigDecimal;
import java.util.logging.Logger;

public class StripePaymentService {

    private static final Logger logger = Logger.getLogger(StripePaymentService.class.getName());

    public StripePaymentService() {
        Stripe.apiKey = ApiConfig.getStripeSecretKey();
    }

    public PaymentIntent createPaymentIntent(BigDecimal amount, String description,
                                              String buyerEmail) throws Exception {
        long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(ApiConfig.STRIPE_CURRENCY)
                .setDescription(description)
                .setReceiptEmail(buyerEmail)
                .addPaymentMethodType("card")
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        logger.info("PaymentIntent created: " + intent.getId());
        return intent;
    }

    public Session createCheckoutSession(BigDecimal amount, String projectTitle,
                                          String buyerEmail, String successUrl, String cancelUrl) throws Exception {
        long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(ApiConfig.STRIPE_CURRENCY)
                                                .setUnitAmount(amountCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Investment: " + projectTitle)
                                                                .setDescription("BizHub Project Investment")
                                                                .build())
                                                .build())
                                .build());
        if (buyerEmail != null && !buyerEmail.isBlank()) {
            builder.setCustomerEmail(buyerEmail);
        }
        Session session = Session.create(builder.build());
        logger.info("Checkout session created: " + session.getId());
        return session;
    }

    /** Retrieve a Checkout Session to check if payment is complete. */
    public Session retrieveCheckoutSession(String sessionId) throws Exception {
        return Session.retrieve(sessionId);
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws Exception {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    public String getPaymentStatus(String paymentIntentId) throws Exception {
        PaymentIntent intent = retrievePaymentIntent(paymentIntentId);
        return intent.getStatus();
    }

    public Task<PaymentIntent> createPaymentIntentAsync(BigDecimal amount, String description, String email) {
        return new Task<>() {
            @Override
            protected PaymentIntent call() throws Exception {
                return createPaymentIntent(amount, description, email);
            }
        };
    }

    public Task<Session> createCheckoutSessionAsync(BigDecimal amount, String projectTitle,
                                                     String buyerEmail, String successUrl, String cancelUrl) {
        return new Task<>() {
            @Override
            protected Session call() throws Exception {
                return createCheckoutSession(amount, projectTitle, buyerEmail, successUrl, cancelUrl);
            }
        };
    }
}
