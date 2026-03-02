package com.bizhub.model.services.marketplace;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service Notification Webhook (Discord / Slack)
 * Utilisé par l'investisseur pour notifier auto-confirmation IA.
 */
public class CommandeNotificationService {

    private final String webhookUrl;
    private final HttpClient client;

    public CommandeNotificationService(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.client = HttpClient.newHttpClient();
    }

    /**
     * Envoi message Discord (asynchrone)
     */
    public void sendDiscord(String message) {

        if (webhookUrl == null || webhookUrl.isBlank()) return;

        String json = "{ \"content\": \"" + escape(message) + "\" }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // async = pas de freeze UI
        client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}