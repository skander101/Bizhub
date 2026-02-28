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

    // ✅ Fallback automatique : essaie 8081 → 8082 → 8083 → 8084 → 8085
    private static final int    PORT_START = 8081;
    private static final int    PORT_END   = 8085;
    private static final String PATH       = "/webhook/stripe";

    private static HttpServer server;
    private static int        activePort = -1;

    // ✅ Retourne int (port actif) au lieu de void
    public static int start(String webhookSecret) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            LOGGER.warning("⚠ StripeWebhookServer : webhookSecret vide — serveur non démarré.");
            return -1;
        }

        // Si déjà démarré, ne pas relancer
        if (server != null && activePort > 0) {
            LOGGER.info("StripeWebhookServer déjà actif sur le port " + activePort);
            return activePort;
        }

        for (int port = PORT_START; port <= PORT_END; port++) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                server.setExecutor(Executors.newSingleThreadExecutor());

                final String secret = webhookSecret;
                server.createContext(PATH, exchange -> handleRequest(exchange, secret));

                Thread t = new Thread(server::start, "stripe-webhook-server");
                t.setDaemon(true);
                t.start();

                activePort = port;
                LOGGER.info("✅ StripeWebhookServer démarré : http://localhost:" + port + PATH);
                return port;

            } catch (IOException e) {
                LOGGER.warning("⚠ Port " + port + " occupé → essai suivant... (" + e.getMessage() + ")");
                server = null;
            }
        }

        LOGGER.severe("❌ Aucun port libre entre " + PORT_START + " et " + PORT_END
                + ". Fermez les anciennes instances Java et relancez.");
        return -1;
    }

    public static int getActivePort() { return activePort; }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            LOGGER.info("✅ StripeWebhookServer arrêté (port " + activePort + ").");
            server     = null;
            activePort = -1;
        }
    }

    // =========================================================================
    // HANDLER HTTP
    // =========================================================================

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

    // =========================================================================
    // TRAITEMENT checkout.session.completed
    // =========================================================================

    private static void handleCheckoutSessionCompleted(Event event, String rawPayload) {

        String sessionId     = null;
        String paymentStatus = null;
        String orderIdStr    = null;

        // 1) Désérialisation SDK
        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            if (deserializer.getObject().isPresent()) {
                StripeObject obj = deserializer.getObject().get();
                if (obj instanceof Session session) {
                    sessionId     = session.getId();
                    paymentStatus = session.getPaymentStatus();

                    if (session.getMetadata() != null) {
                        orderIdStr = session.getMetadata().get("orderId");
                    }
                    if (orderIdStr == null || orderIdStr.isBlank()) {
                        orderIdStr = session.getClientReferenceId();
                    }

                    LOGGER.info("✅ SDK | sessionId=" + sessionId
                            + " | paymentStatus=" + paymentStatus
                            + " | orderId=" + orderIdStr);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Désérialisation SDK échouée, fallback JSON : " + e.getMessage());
        }

        // 2) Fallback JSON si SDK a échoué
        if (sessionId == null) {
            sessionId     = jsonPickDataObjectValue(rawPayload, "id");
            paymentStatus = jsonPickDataObjectValue(rawPayload, "payment_status");
            orderIdStr    = jsonPickDataObjectValue(rawPayload, "orderId");
            if (orderIdStr == null || orderIdStr.isBlank())
                orderIdStr = jsonPickDataObjectValue(rawPayload, "client_reference_id");
        }

        if (sessionId == null || sessionId.isBlank()) {
            LOGGER.severe("❌ sessionId introuvable dans l'événement " + event.getId());
            return;
        }
        if (!"paid".equalsIgnoreCase(paymentStatus)) {
            LOGGER.warning("Paiement non réussi : paymentStatus='" + paymentStatus + "'");
            return;
        }
        if (orderIdStr == null || orderIdStr.isBlank()) {
            LOGGER.severe("❌ orderId introuvable — vérifiez .putMetadata(\"orderId\", ...) dans StripeGatewayClient");
            return;
        }

        try {
            int orderId = Integer.parseInt(orderIdStr.trim());
            CommandeService commandeService = new CommandeService();
            int rows = commandeService.markAsPaid(orderId, sessionId);

            if (rows > 0)
                LOGGER.info("✅ Commande #" + orderId + " marquée PAYÉE (session: " + sessionId + ")");
            else
                LOGGER.warning("⚠ markAsPaid : 0 lignes pour commande #" + orderId + " (déjà payée ?)");

        } catch (NumberFormatException e) {
            LOGGER.severe("orderId invalide : '" + orderIdStr + "'");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Erreur SQL markAsPaid", e);
        }
    }

    // =========================================================================
    // HELPERS JSON
    // =========================================================================

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

    private static String jsonPickDataObjectValue(String json, String key) {
        if (json == null) return null;
        int dataIdx = json.indexOf("\"data\"");
        if (dataIdx < 0) return null;
        int objIdx = json.indexOf("\"object\"", dataIdx);
        if (objIdx < 0) return null;
        return jsonPick(json.substring(objIdx), key);
    }

    private static void send(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}