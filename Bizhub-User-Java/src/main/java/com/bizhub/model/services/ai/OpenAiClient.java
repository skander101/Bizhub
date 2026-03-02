package com.bizhub.model.services.ai;

import com.bizhub.model.services.common.config.Env;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Client IA externe (Groq API OpenAI-compatible).
 *
 * Endpoint: https://api.groq.com/openai/v1/chat/completions
 * Auth:     Bearer ${GROQ_API_KEY}
 * Model:    ${GROQ_MODEL} (ex: llama-3.1-8b-instant)
 */
public class OpenAiClient {

    private static final Logger LOGGER = Logger.getLogger(OpenAiClient.class.getName());

    private static final String BASE_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final Gson gson = new Gson();
    private final String apiKey;
    private final String model;

    public OpenAiClient() {
        this.apiKey = Env.require("GROQ_API_KEY");
        String envModel = Env.get("GROQ_MODEL");
        this.model = (envModel != null && !envModel.isBlank()) ? envModel : "llama-3.1-8b-instant";
    }

    /** Retourne le texte généré */
    public String generateText(String system, String user) throws Exception {

        JsonArray messages = new JsonArray();

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", system == null ? "" : system);
        messages.add(sysMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", user == null ? "" : user);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("temperature", 0.3);
        body.addProperty("max_tokens", 800);

        String json = gson.toJson(body);
        LOGGER.info("IA → " + BASE_URL + " | model=" + model);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("IA HTTP " + res.statusCode() + ": " + res.body());
        }

        // { "choices": [ { "message": { "content": "..." } } ] }
        JsonObject root = gson.fromJson(res.body(), JsonObject.class);

        if (root != null && root.has("choices")) {
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                if (choice != null && choice.has("message") && choice.get("message").isJsonObject()) {
                    JsonObject message = choice.getAsJsonObject("message");
                    if (message.has("content")) {
                        return message.get("content").getAsString();
                    }
                }
            }
        }

        LOGGER.warning("IA réponse inattendue: " + res.body());
        return res.body();
    }
}