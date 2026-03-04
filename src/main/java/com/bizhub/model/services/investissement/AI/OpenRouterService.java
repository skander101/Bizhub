package com.bizhub.model.services.investissement.AI;

import com.bizhub.model.services.common.config.ApiConfig;
import com.google.gson.*;
import javafx.concurrent.Task;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class OpenRouterService {

    private static final Logger logger = Logger.getLogger(OpenRouterService.class.getName());
    private final HttpClient httpClient;
    private final Gson gson;

    public OpenRouterService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public record Message(String role, String content) {}

    public String chat(String systemPrompt, String userMessage) throws Exception {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.add(new Message("user", userMessage));
        return chatWithHistory(messages, 2048);
    }

    public String chat(String systemPrompt, String userMessage, int maxTokens) throws Exception {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.add(new Message("user", userMessage));
        return chatWithHistory(messages, maxTokens);
    }

    public String chatWithHistory(List<Message> messages) throws Exception {
        return chatWithHistory(messages, 2048);
    }

    public String chatWithHistory(List<Message> messages, int maxTokens) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", ApiConfig.OPENROUTER_MODEL);

        JsonArray messagesArray = new JsonArray();
        for (Message msg : messages) {
            JsonObject msgObj = new JsonObject();
            msgObj.addProperty("role", msg.role());
            msgObj.addProperty("content", msg.content());
            messagesArray.add(msgObj);
        }
        body.add("messages", messagesArray);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.OPENROUTER_BASE_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + ApiConfig.getOpenRouterApiKey())
                .header("HTTP-Referer", "https://bizhub.app")
                .header("X-Title", "BizHub Investment Platform")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.severe("OpenRouter API error: " + response.statusCode() + " - " + response.body());
            throw new RuntimeException("AI service error (HTTP " + response.statusCode() + ")");
        }

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray choices = responseJson.getAsJsonArray("choices");
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        }

        throw new RuntimeException("Empty response from AI service");
    }

    public Task<String> chatAsync(String systemPrompt, String userMessage) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                return chat(systemPrompt, userMessage);
            }
        };
    }

    public Task<String> chatWithHistoryAsync(List<Message> messages) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                return chatWithHistory(messages);
            }
        };
    }

    // --- Pitch Analyzer ---
    public String analyzePitch(String projectTitle, String projectDescription, double requiredBudget) throws Exception {
        String systemPrompt = """
            You are a senior investment analyst at a top venture capital firm.
            Analyze the startup project below and give a thorough, human-friendly assessment
            that helps an investor make a decision.

            Write clearly and specifically — avoid vague generic statements.
            Reference the actual project title, budget, and description in your answers.

            Return ONLY valid JSON (no markdown fences, no extra text):
            {
              "riskScore": <1-10 integer, 1=very safe, 10=extremely risky>,
              "verdict": "<one-line human-readable verdict, e.g. 'Promising early-stage project with moderate risk'>",
              "summary": [
                "<investor-focused insight #1>",
                "<investor-focused insight #2>",
                "<investor-focused insight #3>"
              ],
              "strengths": [
                "<specific strength with reasoning>",
                "<specific strength with reasoning>",
                "<specific strength with reasoning>"
              ],
              "weaknesses": [
                "<specific risk or weakness with reasoning>",
                "<specific risk or weakness with reasoning>",
                "<specific risk or weakness with reasoning>"
              ],
              "recommendation": "<2-3 sentence actionable advice for the investor>",
              "valuationMin": <number>,
              "valuationMax": <number>
            }

            Guidelines:
            - Summary: write for an investor who has 30 seconds. Be specific to THIS project.
            - Strengths: mention concrete advantages (market fit, budget efficiency, sector trends).
            - Weaknesses: be honest about risks (team unknowns, competition, budget gaps).
            - Recommendation: should the investor proceed, negotiate, or pass? Why?
            - Valuation: estimate a realistic range based on the sector and budget requested.""";

        String userMessage = String.format(
                "Project Title: %s\nProject Description: %s\nRequested Budget: %.2f TND",
                projectTitle, projectDescription, requiredBudget);

        return chat(systemPrompt, userMessage);
    }

    public Task<String> analyzePitchAsync(String title, String description, double budget) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                return analyzePitch(title, description, budget);
            }
        };
    }

    // --- Negotiation Copilot ---
    public String suggestNegotiationResponse(List<Message> chatHistory, String currentUserRole,
                                              double proposedAmount, double projectBudget) throws Exception {
        String systemPrompt = String.format("""
            You are an expert business negotiation advisor. The user is the %s in an investment deal.
            The project's required budget is %.2f EUR. The current proposed amount is %.2f EUR.
            
            Based on the conversation history, suggest a professional counter-offer or response.
            Include:
            1. A suggested message (ready to send)
            2. A recommended counter-amount (if applicable)
            3. Brief tactical reasoning
            
            Format as JSON:
            {"suggestedMessage": "...", "suggestedAmount": 0.0, "reasoning": "..."}
            
            Respond ONLY with JSON.""", currentUserRole, projectBudget, proposedAmount);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.addAll(chatHistory);
        messages.add(new Message("user", "Please suggest my next negotiation move."));

        return chatWithHistory(messages);
    }

    // --- Sentiment Analysis ---
    public String analyzeSentiment(String text) throws Exception {
        String systemPrompt = """
            Analyze the sentiment of this negotiation message. Respond ONLY with JSON:
            {"sentiment": "positive|neutral|negative|hostile", "confidence": 0.85, "summary": "brief explanation"}""";

        return chat(systemPrompt, text);
    }

    public Task<String> analyzeSentimentAsync(String text) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                return analyzeSentiment(text);
            }
        };
    }

    // --- Smart Search ---
    public String parseSearchQuery(String naturalLanguageQuery) throws Exception {
        String systemPrompt = """
            You are a smart search query parser for an investment platform.
            Extract the user's INTENT from their natural language query.

            Rules:
            - "keywords": list of relevant search terms (synonyms too). If user says "app mobile", also include "application", "mobile", "app". Be generous.
            - "budget": the user's budget (number or null). This is a PREFERENCE, not a hard limit.
            - "sector": business sector if mentioned (string or null)
            - "status": project status if mentioned: pending/funded/in_progress/completed (or null)
            - "intent": brief description of what the user is looking for

            Respond ONLY with JSON, no markdown:
            {"keywords": ["word1","word2"], "budget": null, "sector": null, "status": null, "intent": "brief description"}

            Examples:
            - "my budget is 40000 i look for app mobile" -> {"keywords":["app","mobile","application","smartphone"],"budget":40000,"sector":null,"status":null,"intent":"mobile application project within budget range"}
            - "tech startup under 100k" -> {"keywords":["tech","technology","startup","software"],"budget":100000,"sector":"technology","status":null,"intent":"technology startup project"}""";

        return chat(systemPrompt, naturalLanguageQuery);
    }

    public Task<String> parseSearchQueryAsync(String query) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                return parseSearchQuery(query);
            }
        };
    }

    // --- Deal Estimator ---
    public String estimateDealValue(String projectTitle, String description,
                                     String sector, double requiredBudget) throws Exception {
        String systemPrompt = """
            You are a startup valuation expert. Estimate a fair deal value for this project.
            Consider the sector, description quality, and required budget.
            
            Respond ONLY with JSON:
            {"estimatedMin": 0, "estimatedMax": 0, "fairPrice": 0, "confidence": "low|medium|high", "reasoning": "brief explanation"}""";

        String userMessage = String.format(
                "Project: %s\nSector: %s\nDescription: %s\nRequired Budget: %.2f EUR",
                projectTitle, sector != null ? sector : "General", description, requiredBudget);

        return chat(systemPrompt, userMessage);
    }

    // --- Enhanced Chatbot ---
    public String chatbotConverse(List<Message> conversationHistory, String newUserMessage) throws Exception {
        String systemPrompt = """
            You are BizHub's AI assistant. Help investors and startup founders with:
            - Investment advice and project recommendations
            - Explaining platform features
            - Budget planning and risk assessment
            - Market insights
            
            Be professional, friendly, and concise. Use bullet points when listing items.
            If asked about specific projects, suggest they check the Projects section.
            Answer in the same language as the user (French or English).""";

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.addAll(conversationHistory);
        messages.add(new Message("user", newUserMessage));

        return chatWithHistory(messages);
    }

    public Task<String> chatbotConverseAsync(List<Message> history, String newMessage) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                return chatbotConverse(history, newMessage);
            }
        };
    }
}
