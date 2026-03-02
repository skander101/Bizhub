package com.bizhub.controller.marketplace;

import com.bizhub.model.services.marketplace.CommandeService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StripeWebhookServer {

    private static final Logger LOGGER = Logger.getLogger(StripeWebhookServer.class.getName());
    private static final int    PORT   = 8081;
    private static final String PATH   = "/webhook/stripe";

    private static HttpServer server;

    /**
     * ✅ Retourne le port actif (8081) ou -1 si échec.
     * Compatible avec Main.java : int port = StripeWebhookServer.start(secret);
     */
    public static int start(String webhookSecret) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            LOGGER.warning("⚠ StripeWebhookServer : webhookSecret vide — serveur non démarré.");
            return -1;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.createContext(PATH, exchange -> handleRequest(exchange, webhookSecret));

            Thread t = new Thread(server::start, "stripe-webhook-server");
            t.setDaemon(true);
            t.start();

            LOGGER.info("✅ StripeWebhookServer démarré : http://localhost:" + PORT + PATH);
            return PORT;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Impossible de démarrer StripeWebhookServer", e);
            return -1;
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            LOGGER.info("StripeWebhookServer arrêté.");
        }
    }

    private static void handleRequest(HttpExchange exchange, String webhookSecret) throws IOException {

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method Not Allowed");
            return;
        }

        byte[] rawBytes;
        try (InputStream is = exchange.getRequestBody()) {
            rawBytes = is.readAllBytes();
        }
        String payload = new String(rawBytes, StandardCharsets.UTF_8);

        String sigHeader = exchange.getRequestHeaders().getFirst("Stripe-Signature");
        if (sigHeader == null || sigHeader.isBlank()) {
            LOGGER.warning("Webhook reçu sans Stripe-Signature header");
            send(exchange, 400, "Missing signature");
            return;
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            LOGGER.warning("Signature webhook invalide : " + e.getMessage());
            send(exchange, 400, "Invalid signature");
            return;
        }

        LOGGER.info("Webhook reçu : type=" + event.getType() + " | id=" + event.getId());

        try {
            if ("checkout.session.completed".equals(event.getType())) {
                handleCheckoutSessionCompleted(event, payload);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur traitement webhook", e);
            e.printStackTrace();
        }

        send(exchange, 200, "OK");
    }

    private static void handleCheckoutSessionCompleted(Event event, String rawPayload) {

        LOGGER.info("handleCheckoutSessionCompleted : " + event.getId());

        String sessionId     = null;
        String paymentStatus = null;
        String orderIdStr    = null;

        // Méthode 1 : désérialisation SDK
        try {
            EventDataObjectDeserializer d = event.getDataObjectDeserializer();
            if (d.getObject().isPresent()) {
                StripeObject obj = d.getObject().get();
                if (obj instanceof Session s) {
                    sessionId     = s.getId();
                    paymentStatus = s.getPaymentStatus();
                    var meta = s.getMetadata();
                    if (meta != null) orderIdStr = meta.get("orderId");
                    if (orderIdStr == null || orderIdStr.isBlank())
                        orderIdStr = s.getClientReferenceId();
                    LOGGER.info("SDK OK → session=" + sessionId
                            + " status=" + paymentStatus + " orderId=" + orderIdStr);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("SDK désérialisation échouée : " + e.getMessage());
        }

        // Méthode 2 : fallback JSON brut
        if (sessionId == null || sessionId.isBlank()) {
            sessionId     = jsonPick(rawPayload, "id");
            paymentStatus = jsonPick(rawPayload, "payment_status");
            orderIdStr    = jsonPick(rawPayload, "orderId");
            if (orderIdStr == null || orderIdStr.isBlank())
                orderIdStr = jsonPick(rawPayload, "client_reference_id");
            LOGGER.info("JSON fallback → session=" + sessionId
                    + " status=" + paymentStatus + " orderId=" + orderIdStr);
        }

        if (sessionId == null || sessionId.isBlank()) {
            LOGGER.severe("❌ sessionId introuvable"); return;
        }
        if (!"paid".equals(paymentStatus)) {
            LOGGER.warning("Non payé : status=" + paymentStatus); return;
        }
        if (orderIdStr == null || orderIdStr.isBlank()) {
            LOGGER.severe("❌ orderId absent | session=" + sessionId); return;
        }

        try {
            int orderId = Integer.parseInt(orderIdStr.trim());
            LOGGER.info("markAsPaid → orderId=" + orderId);
            int rows = new CommandeService().markAsPaid(orderId, sessionId);
            if (rows > 0)
                LOGGER.info("✅ Commande #" + orderId + " marquée payée ! session=" + sessionId);
            else
                LOGGER.warning("⚠ 0 lignes MàJ pour commande #" + orderId + " (déjà payée ?)");
        } catch (NumberFormatException e) {
            LOGGER.severe("orderId invalide : '" + orderIdStr + "'");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ SQL markAsPaid", e);
            e.printStackTrace();
        }
    }

    private static String jsonPick(String json, String key) {
        if (json == null || key == null) return null;
        String k = "\"" + key + "\"";
        int idx = json.indexOf(k);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + k.length());
        if (colon < 0) return null;
        String rest = json.substring(colon + 1).trim();
        if (rest.startsWith("\"")) {
            int end = rest.indexOf('"', 1);
            return end < 0 ? null : rest.substring(1, end);
        } else {
            int end = 0;
            while (end < rest.length() && (Character.isDigit(rest.charAt(end))
                    || rest.charAt(end) == '-' || rest.charAt(end) == '.')) end++;
            return end > 0 ? rest.substring(0, end) : null;
        }
    }

    private static void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }
}