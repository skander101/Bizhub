package com.bizhub.model.services.community.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

/**
 * AI service using Groq API — 100% free, works in Tunisia.
 * Uses llama-4-scout for vision (image analysis) + text.
 * Free tier: 14,400 requests/day.
 */
public class GeminiService {

    private static final String API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    // Vision model — supports both text and images
    private static final String VISION_MODEL  = "meta-llama/llama-4-scout-17b-16e-instruct";
    // Text-only fallback
    private static final String TEXT_MODEL    = "llama-3.1-8b-instant";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private final String apiKey;

    public GeminiService() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.apiKey = dotenv.get("GROQ_API_KEY", "");
    }

    /**
     * Text-only ask.
     */
    public String ask(String prompt) {
        return askWithImage(prompt, null, TEXT_MODEL);
    }

    /**
     * Ask with optional image file path — uses vision model if image provided.
     */
    public String askWithImage(String prompt, String imageFilePath, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            return "⚠ GROQ_API_KEY not found in .env file.";
        }
        try {
            // System message
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content",
                    "You are an AI assistant embedded in BizHub, a business community platform. " +
                            "Users share posts about business, tech, investment, and events. " +
                            "Be concise, helpful, and professional. " +
                            "For fact-checking: clearly state what is accurate, uncertain, or false. " +
                            "For summaries: use 2-3 sentences max. " +
                            "For topic suggestions: give 3-5 relevant tags, no explanation. " +
                            "If an image is provided, analyze it as part of the post context. " +
                            "Always respond in the same language as the post content."
            );

            // User message — with or without image
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");

            if (imageFilePath != null && !imageFilePath.isBlank()) {
                File imgFile = new File(imageFilePath);
                if (imgFile.exists() && imgFile.length() < 4 * 1024 * 1024) { // max 4MB
                    // Build content array with text + image
                    JsonArray contentArray = new JsonArray();

                    // Text part
                    JsonObject textPart = new JsonObject();
                    textPart.addProperty("type", "text");
                    textPart.addProperty("text", prompt);
                    contentArray.add(textPart);

                    // Image part — base64 encoded
                    String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(imgFile.toPath()));
                    String mimeType = detectMimeType(imageFilePath);

                    JsonObject imageUrl = new JsonObject();
                    imageUrl.addProperty("url", "data:" + mimeType + ";base64," + base64);

                    JsonObject imagePart = new JsonObject();
                    imagePart.addProperty("type", "image_url");
                    imagePart.add("image_url", imageUrl);
                    contentArray.add(imagePart);

                    userMsg.add("content", contentArray);
                } else {
                    // Image too large or missing — fall back to text only
                    userMsg.addProperty("content", prompt + "\n(Note: image could not be loaded)");
                }
            } else {
                userMsg.addProperty("content", prompt);
            }

            JsonArray messages = new JsonArray();
            messages.add(systemMsg);
            messages.add(userMsg);

            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.add("messages", messages);
            body.addProperty("max_tokens", 512);
            body.addProperty("temperature", 0.7);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    return "⚠ API error " + response.code() + ": " + responseBody;
                }
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                return json.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString().trim();
            }
        } catch (Exception e) {
            return "⚠ Error: " + e.getMessage();
        }
    }

    private String detectMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    // --- Convenience methods — pass imageUrl if post has one ---

    public String factCheck(String title, String content, String imageUrl) {
        String prompt = "Fact-check this post. If there is an image, analyze it too. " +
                "Identify claims that are accurate, misleading, or unverifiable. Be concise.\n\n" +
                "Title: " + title + "\n\nContent: " + content;
        return askWithImage(prompt, imageUrl, VISION_MODEL);
    }

    public String summarize(String title, String content, String imageUrl) {
        String prompt = "Summarize this post in 2-3 sentences. " +
                (imageUrl != null ? "Include what the image shows if relevant. " : "") +
                "\n\nTitle: " + title + "\n\nContent: " + content;
        return askWithImage(prompt, imageUrl, VISION_MODEL);
    }

    public String suggestTopics(String title, String content, String imageUrl) {
        String prompt = "Suggest 4-5 relevant topics or hashtags for this post" +
                (imageUrl != null ? " (consider the image too)" : "") +
                ". Return as comma-separated list only.\n\nTitle: " + title + "\n\nContent: " + content;
        return askWithImage(prompt, imageUrl, VISION_MODEL);
    }

    public String askAboutPost(String title, String content, String imageUrl, String question) {
        String prompt = "Answer this question about the post" +
                (imageUrl != null ? " and its image" : "") +
                ".\n\nPost Title: " + title + "\nPost Content: " + content +
                "\n\nQuestion: " + question;
        return askWithImage(prompt, imageUrl, VISION_MODEL);
    }
}

