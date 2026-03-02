package com.bizhub.model.services.marketplace;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bizhub.model.marketplace.CommandeJoinProduit;
import com.bizhub.model.services.common.service.EnvConfig;

public class OpenAIService {

    private static final Logger LOGGER = Logger.getLogger(OpenAIService.class.getName());
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String SYSTEM_ROLE =
            "Tu es l'assistant IA de BizHub, une marketplace B2B tunisienne qui met en relation des startups "
                    + "et des investisseurs. Tu réponds TOUJOURS en français, de manière concise et professionnelle.";

    private final String apiKey;
    private final String model;
    private final int    maxTokens;
    private final HttpClient http;

    private static OpenAIService instance;

    public static OpenAIService getInstance() {
        if (instance == null) instance = new OpenAIService();
        return instance;
    }

    private OpenAIService() {
        // ✅ Lire depuis .env
        this.apiKey    = EnvConfig.getRequired("OPENAI_API_KEY");
        this.model     = EnvConfig.getOrDefault("OPENAI_MODEL",      "gpt-4o-mini");
        this.maxTokens = Integer.parseInt(EnvConfig.getOrDefault("OPENAI_MAX_TOKENS", "512"));

        this.http = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .build();

        LOGGER.info("✅ OpenAIService initialisé (model=" + model + ")");
    }

    public String resumeCommande(CommandeJoinProduit c) {
        return chat("Analyse cette commande BizHub et donne un résumé décisionnel en 2-3 phrases :\n"
                + "Commande #" + c.getIdCommande() + " | Produit : " + safe(c.getProduitNom())
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
            String body = "{\"model\":\"" + model + "\","
                    + "\"max_tokens\":" + maxTokens + ","
                    + "\"messages\":["
                    + "{\"role\":\"system\",\"content\":\"" + escapeJson(SYSTEM_ROLE) + "\"},"
                    + "{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}"
                    + "]}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) return extractContent(resp.body());
            LOGGER.warning("OpenAI HTTP " + resp.statusCode());
            return "⚠ Erreur OpenAI (" + resp.statusCode() + ")";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "⚠ Requête interrompue.";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "OpenAI erreur", e);
            return "⚠ OpenAI indisponible : " + e.getMessage();
        }
    }

    private String extractContent(String json) {
        try {
            int idx = json.indexOf("\"content\"");
            if (idx < 0) return "Réponse vide.";
            int colon = json.indexOf(':', idx);
            String rest = json.substring(colon + 1).trim();
            if (!rest.startsWith("\"")) return "Parse error.";
            StringBuilder sb = new StringBuilder();
            boolean esc = false;
            for (int i = 1; i < rest.length(); i++) {
                char ch = rest.charAt(i);
                if (esc) {
                    switch (ch) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        default -> sb.append(ch);
                    }
                    esc = false;
                } else if (ch == '\\') { esc = true;
                } else if (ch == '"') { break;
                } else { sb.append(ch); }
            }
            return sb.toString().trim();
        } catch (Exception e) { return "Erreur parsing."; }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\n","\\n").replace("\r","\\r").replace("\t","\\t");
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
}