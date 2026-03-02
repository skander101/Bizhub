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

public class OpenAiClient {

    private static final Logger LOGGER = Logger.getLogger(OpenAiClient.class.getName());

    // ✅ FIX 1 : bonne URL — Chat Completions API (pas /v1/responses)
    private static final String BASE_URL = "https://api.openai.com/v1/chat/completions";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final Gson gson = new Gson();

    private final String apiKey;
    private final String model;

    public OpenAiClient() {
        this.apiKey = Env.require("OPENAI_API_KEY");
        // ✅ FIX 2 : modèle valide — gpt-4.1-mini n'existe pas, utiliser gpt-4o-mini
        String envModel = Env.get("OPENAI_MODEL");
        this.model = (envModel != null && !envModel.isBlank()) ? envModel : "gpt-4o-mini";
    }

    /** Retourne le texte de la réponse ChatGPT */
    public String generateText(String system, String user) throws Exception {

        // ✅ FIX 3 : payload correct pour /v1/chat/completions → "messages" (pas "input")
        JsonArray messages = new JsonArray();

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", system);
        messages.add(sysMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", user);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);          // ← "messages" pas "input"
        body.addProperty("temperature", 0.3);
        body.addProperty("max_tokens", 800);

        String json = gson.toJson(body);
        LOGGER.info("OpenAI → " + BASE_URL + " | model=" + model);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI HTTP " + res.statusCode() + ": " + res.body());
        }

        // ✅ FIX 4 : parser la réponse Chat Completions
        // Format : { "choices": [ { "message": { "content": "..." } } ] }
        JsonObject root = gson.fromJson(res.body(), JsonObject.class);

        if (root.has("choices")) {
            JsonArray choices = root.getAsJsonArray("choices");
            if (!choices.isEmpty()) {
                JsonObject choice  = choices.get(0).getAsJsonObject();
                JsonObject message = choice.getAsJsonObject("message");
                if (message != null && message.has("content")) {
                    return message.get("content").getAsString();
                }
            }
        }

        LOGGER.warning("OpenAI réponse inattendue: " + res.body());
        return res.body();
    }
}