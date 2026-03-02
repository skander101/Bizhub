package com.bizhub.model.services.marketplace;

import com.bizhub.model.marketplace.CommandeJoinProduit;
import com.bizhub.model.services.common.config.EnvLoader;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service IA externe (Groq OpenAI-compatible).
 *
 * Variables .env:
 *  - GROQ_API_KEY
 *  - GROQ_MODEL (ex: llama-3.1-8b-instant)
 *  - GROQ_MAX_TOKENS (optionnel)
 */
public class OpenAIService {

    private static final Logger LOGGER = Logger.getLogger(OpenAIService.class.getName());

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final String SYSTEM_ROLE =
            "Tu es l'assistant IA de BizHub, une marketplace B2B tunisienne qui met en relation des startups "
                    + "et des investisseurs. Tu réponds TOUJOURS en français, de manière concise et professionnelle.";

    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final HttpClient http;
    private final Gson gson = new Gson();

    private static OpenAIService instance;

    public static OpenAIService getInstance() {
        if (instance == null) instance = new OpenAIService();
        return instance;
    }

    private OpenAIService() {
        // ✅ Lire depuis .env (Groq)
        this.apiKey = EnvLoader.getRequired("GROQ_API_KEY");
        this.model = EnvLoader.getOrDefault("GROQ_MODEL", "llama-3.1-8b-instant");
        this.maxTokens = Integer.parseInt(EnvLoader.getOrDefault("GROQ_MAX_TOKENS", "512"));

        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        LOGGER.info("✅ IA service initialisé (model=" + model + ")");
    }

    public String resumeCommande(CommandeJoinProduit c) {
        return chat("Analyse cette commande BizHub et donne un résumé décisionnel en 2-3 phrases :\n"
                + "Commande #" + c.getIdCommande()
                + " | Produit : " + safe(c.getProduitNom())
                + " | Qté : " + c.getQuantiteCommande()
                + " | Statut : " + safe(c.getStatut())
                + " | Score IA : " + c.getPriorityScore() + "/500"
                + " | Priorité : " + safe(c.getPriorityLabel())
                + " | Payée : " + (c.isEstPayee() ? "Oui" : "Non")
                + "\nRecommande : Confirmer / Rejeter / Examiner. Max 60 mots.");
    }

    public String recommanderProduits(List<CommandeJoinProduit> historique, List<String> produits) {
        StringBuilder hist = new StringBuilder();
        int max = Math.min(historique == null ? 0 : historique.size(), 5);
        for (int i = 0; i < max; i++) {
            CommandeJoinProduit c = historique.get(i);
            hist.append("- ").append(safe(c.getProduitNom()))
                    .append(" (qté:").append(c.getQuantiteCommande()).append(")\n");
        }
        return chat("Historique commandes startup :\n" + hist
                + "Produits disponibles : " + String.join(", ", produits)
                + "\nRecommande 2-3 produits adaptés. Max 80 mots.");
    }

    public String chatbot(String question, String role) {
        if (question == null || question.isBlank()) return "Posez-moi une question !";
        String ctx = "investisseur".equalsIgnoreCase(role)
                ? "Tu parles à un investisseur qui gère ses produits et confirme des commandes."
                : "Tu parles à un startup qui cherche des produits/services.";
        return chat(ctx + "\nQuestion : " + question.trim() + "\nMax 100 mots.");
    }

    public String analyseTopProduits(List<String> topProduits) {
        if (topProduits == null || topProduits.isEmpty()) return "Aucune donnée.";
        return chat("Top produits BizHub :\n" + String.join("\n", topProduits)
                + "\nAnalyse business en 2-3 phrases : tendance + recommandation investisseur.");
    }

    public String chat(String userMessage) {
        try {
            JsonArray messages = new JsonArray();

            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", SYSTEM_ROLE);
            messages.add(sys);

            JsonObject usr = new JsonObject();
            usr.addProperty("role", "user");
            usr.addProperty("content", userMessage == null ? "" : userMessage);
            messages.add(usr);

            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("max_tokens", maxTokens);
            body.add("messages", messages);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() == 200) return extractContent(resp.body());

            if (resp.statusCode() == 401) return "⚠ Clé IA invalide (401).";
            if (resp.statusCode() == 429) return "⚠ Limite gratuite atteinte (429). Réessayez plus tard.";

            LOGGER.warning("IA HTTP " + resp.statusCode() + " -> " + resp.body());
            return "⚠ Erreur IA (" + resp.statusCode() + ")";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "⚠ Requête interrompue.";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "IA erreur", e);
            return "⚠ IA indisponible : " + e.getMessage();
        }
    }

    private String extractContent(String json) {
        try {
            JsonObject root = gson.fromJson(json, JsonObject.class);
            if (root == null || !root.has("choices")) return "Réponse vide.";

            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) return "Réponse vide.";

            JsonObject choice = choices.get(0).getAsJsonObject();
            if (choice == null || !choice.has("message")) return "Réponse vide.";

            JsonObject msg = choice.getAsJsonObject("message");
            if (msg != null && msg.has("content")) return msg.get("content").getAsString().trim();

            return "Réponse vide.";
        } catch (Exception e) {
            return "Erreur parsing.";
        }
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
}