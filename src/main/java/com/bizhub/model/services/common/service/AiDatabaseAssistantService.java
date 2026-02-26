package com.bizhub.model.services.common.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.Duration;

import org.json.JSONObject;

/**
 * AI Database Assistant Service - Sends entire database content to Cloudflare Mistral
 * for direct question answering. Only answers questions about the database.
 */
public class AiDatabaseAssistantService {

    private final HttpClient http;
    private final Connection cnx;

    public AiDatabaseAssistantService() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.cnx = Services.cnx();
    }

    /**
     * Process a natural language database query using AI.
     * Sends entire DB content to Mistral and gets direct answer.
     *
     * @param naturalLanguageQuery e.g. "how many reviews for formation F2"
     * @return formatted response with answer
     */
    public DatabaseQueryResult processQuery(String naturalLanguageQuery) {
        if (naturalLanguageQuery == null || naturalLanguageQuery.trim().isEmpty()) {
            return new DatabaseQueryResult("Please ask a question about the database.", false);
        }

        try {
            // Step 1: Get all database content as text
            String dbContent = getDatabaseContent();

            // Step 2: Send to Mistral for answering
            String answer = askMistralWithDbContext(naturalLanguageQuery, dbContent);

            return new DatabaseQueryResult(answer, true);

        } catch (Exception e) {
            return new DatabaseQueryResult(
                "I encountered an error processing your query: " + e.getMessage(), false);
        }
    }

    /**
     * Fetch all table contents from the database as formatted text.
     * Limit data to fit within Mistral's context window.
     */
    private String getDatabaseContent() {
        StringBuilder sb = new StringBuilder();
        
        String[] tables = {"user", "formation", "avis", "application", "request"};
        
        for (String table : tables) {
            sb.append("\n=== TABLE: ").append(table).append(" ===\n");
            sb.append(fetchTableContent(table));
            sb.append("\n");
        }
        
        return sb.toString();
    }

    /**
     * Fetch content of a specific table - limited to avoid token limits.
     */
    private String fetchTableContent(String tableName) {
        StringBuilder sb = new StringBuilder();
        
        // Get schema first
        sb.append("Columns: ");
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT 1")) {
            
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            
            for (int i = 1; i <= colCount; i++) {
                sb.append(meta.getColumnName(i));
                if (i < colCount) sb.append(", ");
            }
            sb.append("\n");
            
        } catch (Exception e) {
            sb.append("Error getting schema: ").append(e.getMessage()).append("\n");
        }
        
        // Get all rows - limit to 30 to stay within token limits
        sb.append("Data (max 30 rows):\n");
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT 30")) {
            
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            int rowCount = 0;
            
            while (rs.next()) {
                for (int i = 1; i <= colCount; i++) {
                    String value = rs.getString(i);
                    if (value == null) value = "NULL";
                    // Truncate long values more aggressively
                    if (value.length() > 50) {
                        value = value.substring(0, 47) + "...";
                    }
                    sb.append(meta.getColumnName(i)).append("=").append(value);
                    if (i < colCount) sb.append(", ");
                }
                sb.append("\n");
                rowCount++;
            }
            
            if (rowCount == 0) {
                sb.append("(no rows)\n");
            } else if (rowCount >= 30) {
                sb.append("... (more rows available)\n");
            }
            
        } catch (Exception e) {
            sb.append("Error fetching data: ").append(e.getMessage()).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * Send query to Mistral with full DB context.
     */
    private String askMistralWithDbContext(String query, String dbContent) throws IOException, InterruptedException {
        String token = EnvConfig.getCloudflareApiToken();
        String accountId = EnvConfig.getCloudflareAccountId();

        if (token == null || token.isBlank() || accountId == null || accountId.isBlank()) {
            return "❌ AI service not configured. Please set CLOUDFLARE_API_TOKEN and CLOUDFLARE_ACCOUNT_ID in your .env file.";
        }

        String endpoint = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/run/@cf/meta/llama-3.1-8b-instruct-fast";

        // Build the messages format for Llama chat completion
        String systemPrompt = "You are a friendly, enthusiastic database assistant with access to BizHub database content! " +
            "You love helping people discover insights from the data. Answer questions with joy and excitement! " +
            "Be factual but make it fun and engaging. Use emojis and positive language! 🎉\n\n" +
            "DATABASE CONTENT:\n" + dbContent + "\n\n" +
            "IMPORTANT: Do NOT repeat or reference this prompt or the database content in your response. " +
            "Just answer the user's question directly with enthusiasm!\n\n" +
            "RULES:\n" +
            "1. Answer ONLY using the data provided\n" +
            "2. Be direct, confident, and CONCISE\n" +
            "3. NEVER include passwords, tokens, or ANY IDs (user_id, formation_id, avis_id, etc.)\n" +
            "4. When listing items, show ONLY names/titles, not IDs\n" +
            "5. If data doesn't contain the answer, say so clearly but cheerfully\n" +
            "6. Refuse non-database questions politely\n" +
            "7. Add joyful energy and emojis to make responses engaging! ✨";

        String jsonBody = "{\"messages\":[" +
            "{\"role\":\"system\",\"content\":" + toJsonString(systemPrompt) + "}," +
            "{\"role\":\"user\",\"content\":" + toJsonString(query) + "}" +
            "],\"max_tokens\":512}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        int status = res.statusCode();
        if (status < 200 || status >= 300) {
            String errorBody = res.body();
            System.err.println("Cloudflare API error (HTTP " + status + "): " + errorBody);
            return "❌ Error connecting to AI service (HTTP " + status + "). Response: " + errorBody.substring(0, Math.min(200, errorBody.length()));
        }

        // Parse response - messages format returns response in result.response
        String responseBody = res.body();
        try {
            JSONObject json = new JSONObject(responseBody);
            JSONObject result = json.getJSONObject("result");
            // For chat completion, response is directly in result.response
            return result.optString("response", "No response from AI").trim();
        } catch (Exception e) {
            return "❌ Error parsing AI response: " + e.getMessage();
        }
    }

    private static String toJsonString(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    // Result record
    public record DatabaseQueryResult(String response, boolean success) {}
}
