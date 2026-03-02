package com.bizhub.model.services.marketplace.payment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Client HTTP vers le backend local (gateway Stripe).
 * Le backend tourne sur http://localhost:8080
 * Lance StripeGatewayServer.java avant d'utiliser l'app.
 */
public class PaymentApiClient {

    private final String baseUrl;
    private final HttpClient http = HttpClient.newHttpClient();

    public PaymentApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public PaymentResult createStripeCheckout(int orderId, String product, int quantity) throws Exception {
        String body = "{"
                + "\"orderId\":" + orderId + ","
                + "\"product\":\"" + safe(product) + "\","
                + "\"quantity\":" + quantity
                + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/payments/stripe/checkout"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 400)
            return PaymentResult.fail("Gateway error " + resp.statusCode() + " : " + resp.body());

        // attendu: {"ref":"...","url":"..."}
        String json = resp.body();
        String ref  = pick(json, "ref");
        String url  = pick(json, "url");

        if (url.isBlank())
            return PaymentResult.fail("Réponse gateway invalide: " + json);

        return PaymentResult.ok(ref, url);
    }

    private static String safe(String s) { return s == null ? "" : s.replace("\"", "'"); }

    private static String pick(String json, String key) {
        String k = "\"" + key + "\":";
        int i = json.indexOf(k);
        if (i < 0) return "";
        int start = json.indexOf('"', i + k.length()) + 1;
        int end   = json.indexOf('"', start);
        return (start > 0 && end > start) ? json.substring(start, end) : "";
    }
}
