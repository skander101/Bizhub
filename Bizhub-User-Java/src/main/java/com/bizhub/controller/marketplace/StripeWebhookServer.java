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

    public static void start(String webhookSecret) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            LOGGER.warning("⚠ StripeWebhookServer : webhookSecret vide — serveur non démarré.");
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.createContext(PATH, exchange -> handleRequest(exchange, webhookSecret));

            Thread t = new Thread(server::start, "stripe-webhook-server");
            t.setDaemon(true);
            t.start();

            LOGGER.info("✅ StripeWebhookServer démarré : http://localhost:" + PORT + PATH);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Impossible de démarrer StripeWebhookServer", e);
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
        }

        send(exchange, 200, "OK");
    }

    private static void handleCheckoutSessionCompleted(Event event, String rawPayload) {

        String sessionId = null;
        String paymentStatus = null;
        String orderIdStr = null;

        // ── 1) SDK (préférable) ──
        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            if (deserializer.getObject().isPresent()) {
                StripeObject obj = deserializer.getObject().get();
                if (obj instanceof Session session) {
                    sessionId = session.getId();
                    paymentStatus = session.getPaymentStatus();

                    if (session.getMetadata() != null) {
                        orderIdStr = session.getMetadata().get("orderId"); // ✅ IMPORTANT
                    }
                    if (orderIdStr == null || orderIdStr.isBlank()) {
                        orderIdStr = session.getClientReferenceId(); // fallback
                    }

                    LOGGER.info("✅ SDK | sessionId=" + sessionId
                            + " | paymentStatus=" + paymentStatus
                            + " | orderId=" + orderIdStr
                            + " | clientRef=" + session.getClientReferenceId()
                            + " | meta=" + session.getMetadata());
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Désérialisation SDK échouée, fallback JSON : " + e.getMessage());
        }

        // ── 2) fallback JSON ──
        if (sessionId == null) {
            sessionId = jsonPickDataObjectValue(rawPayload, "id");
            paymentStatus = jsonPickDataObjectValue(rawPayload, "payment_status");
            orderIdStr = jsonPickDataObjectValue(rawPayload, "orderId");
            if (orderIdStr == null || orderIdStr.isBlank()) {
                orderIdStr = jsonPickDataObjectValue(rawPayload, "client_reference_id");
            }
        }

        if (sessionId == null || sessionId.isBlank()) {
            LOGGER.severe("❌ sessionId introuvable dans l'événement " + event.getId());
            return;
        }

        if (!"paid".equalsIgnoreCase(paymentStatus)) {
            LOGGER.warning("Paiement non réussi : paymentStatus='" + paymentStatus + "' | session=" + sessionId);
            return;
        }

        if (orderIdStr == null || orderIdStr.isBlank()) {
            LOGGER.severe("❌ orderId introuvable (metadata + client_reference_id) | session=" + sessionId
                    + " — vérifiez StripeGatewayClient : .putMetadata(\"orderId\", ...) et .setClientReferenceId(...)");
            return;
        }

        try {
            int orderId = Integer.parseInt(orderIdStr.trim());
            CommandeService commandeService = new CommandeService();
            int rows = commandeService.markAsPaid(orderId, sessionId);

            if (rows > 0) LOGGER.info("✅ Commande #" + orderId + " marquée payée ! (session: " + sessionId + ")");
            else LOGGER.warning("⚠ markAsPaid : 0 lignes pour commande #" + orderId + " (déjà payée ou introuvable)");

        } catch (NumberFormatException e) {
            LOGGER.severe("orderId invalide : '" + orderIdStr + "'");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Erreur SQL markAsPaid", e);
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
            int start = 1;
            int end = rest.indexOf('"', start);
            if (end < 0) return null;
            return rest.substring(start, end);
        } else {
            int end = 0;
            while (end < rest.length() && (Character.isDigit(rest.charAt(end))
                    || rest.charAt(end) == '-' || rest.charAt(end) == '.')) end++;
            return end > 0 ? rest.substring(0, end) : null;
        }
    }

    private static String jsonPickDataObjectValue(String json, String key) {
        if (json == null) return null;
        int dataIdx = json.indexOf("\"data\"");
        if (dataIdx < 0) return null;

        int objIdx = json.indexOf("\"object\"", dataIdx);
        if (objIdx < 0) return null;

        String sub = json.substring(objIdx);
        return jsonPick(sub, key);
    }

    private static void send(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}