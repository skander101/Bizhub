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

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * AI Database Assistant Service - Sends database content to Cloudflare GPT-oss-120B
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

    // ── GPT-oss-120B on Cloudflare Workers AI ──
    private static final int CONTEXT_WINDOW    = 128_000;
    private static final int MIN_OUTPUT_TOKENS = 300;
    private static final int MAX_OUTPUT_TOKENS = 2048;
    private static final String MODEL_ID = "@cf/openai/gpt-oss-120b";

    /** Rough token estimate (~3.5 chars/token for English + structured text). */
    private static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / 3.5);
    }

    /**
     * Process a natural language database query using AI.
     * Sends entire DB content to Mistral and gets direct answer.
     *
     * @param naturalLanguageQuery e.g. "how many reviews for formation F2"
     * @return formatted response with answer
     */
    public DatabaseQueryResult processQuery(String naturalLanguageQuery) {
        return processQuery(naturalLanguageQuery, java.util.List.of());
    }

    /**
     * Process a natural language database query using AI, with a limited recent chat history.
     *
     * @param naturalLanguageQuery current user query
     * @param recentHistory messages from previous turns (ideally already limited)
     * @return formatted response with answer
     */
    public DatabaseQueryResult processQuery(String naturalLanguageQuery, java.util.List<ChatMessage> recentHistory) {
        if (naturalLanguageQuery == null || naturalLanguageQuery.trim().isEmpty()) {
            return new DatabaseQueryResult("Please ask a question about the database.", false);
        }

        try {
            // Step 1: Get all database content as text
            String dbContent = getDatabaseContent();

            // Step 2: Send to GPT-oss-120B for answering
            String answer = askAiWithDbContext(naturalLanguageQuery, dbContent, recentHistory);

            return new DatabaseQueryResult(answer, true);

        } catch (Exception e) {
            return new DatabaseQueryResult(
                "I encountered an error processing your query: " + e.getMessage(), false);
        }
    }

    /**
     * Fetch DB content for GPT-oss-120B (128K context — plenty of room).
     */
    private String getDatabaseContent() {
        StringBuilder sb = new StringBuilder();

        // Compact schema matching actual BizHub DB
        sb.append("SCHEMA:\n");
        sb.append("user(user_id,email,user_type,full_name,phone,company_name,sector,is_active)\n");
        sb.append("formation(formation_id,title,description,trainer_id,start_date,end_date,cost)\n");
        sb.append("avis(avis_id,reviewer_id,formation_id,rating 1-5,comment,created_at,is_verified)\n");
        sb.append("training_request(request_id,startup_id,formation_id,request_date,status)\n");
        sb.append("application(application_id,request_id,trainer_id,assigned_at,notes)\n\n");

        // Key stats (most queried)
        sb.append(getAggregateStatistics());

        // Table rows — with 128K context we can include many more rows
        String[][] tables = {{"formation","50"},{"user","50"},{"avis","50"},{"training_request","30"},{"application","30"}};
        for (String[] t : tables) {
            sb.append(fetchTableContent(t[0], Integer.parseInt(t[1])));
        }

        return sb.toString();
    }

    /**
     * Compact aggregate statistics — top-5 lists to save tokens.
     */
    private String getAggregateStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("STATS:\n");

        // Top formations: reviews + avg rating + training requests in one query
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT f.title, COUNT(DISTINCT a.avis_id) AS reviews, " +
                 "ROUND(AVG(a.rating),1) AS avg_rate, COUNT(DISTINCT tr.request_id) AS reqs " +
                 "FROM formation f " +
                 "LEFT JOIN avis a ON f.formation_id=a.formation_id " +
                 "LEFT JOIN training_request tr ON f.formation_id=tr.formation_id " +
                 "GROUP BY f.formation_id,f.title ORDER BY reviews DESC LIMIT 5")) {
            sb.append("Top formations(reviews,rating,requests):\n");
            while (rs.next()) {
                sb.append("- ").append(rs.getString("title"))
                  .append("|").append(rs.getInt("reviews")).append("rev")
                  .append("|avg=").append(rs.getDouble("avg_rate"))
                  .append("|").append(rs.getInt("reqs")).append("reqs\n");
            }
        } catch (Exception e) { sb.append("err:").append(e.getMessage()).append("\n"); }

        // Top reviewers
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT u.full_name,COUNT(a.avis_id) AS cnt " +
                 "FROM user u LEFT JOIN avis a ON u.user_id=a.reviewer_id " +
                 "GROUP BY u.user_id,u.full_name ORDER BY cnt DESC LIMIT 5")) {
            sb.append("Top reviewers:\n");
            while (rs.next()) {
                sb.append("- ").append(rs.getString("full_name"))
                  .append(":").append(rs.getInt("cnt")).append("rev\n");
            }
        } catch (Exception e) { sb.append("err:").append(e.getMessage()).append("\n"); }

        // Totals (one-liner)
        try (Statement stmt = cnx.createStatement()) {
            sb.append("Totals:");
            for (String t : new String[]{"user","formation","avis","training_request","application"}) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) c FROM " + t);
                if (rs.next()) sb.append(" ").append(t).append("=").append(rs.getInt("c"));
            }
            sb.append("\n");
        } catch (Exception e) { sb.append("err:").append(e.getMessage()).append("\n"); }

        return sb.toString();
    }

    /**
     * Fetch table rows in compact CSV format.
     * @param maxRows max number of rows to include
     */
    private String fetchTableContent(String tableName, int maxRows) {
        if (!isValidTableName(tableName)) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("\n[").append(tableName.toUpperCase()).append("]\n");

        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT " + maxRows)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            // CSV header
            for (int i = 1; i <= colCount; i++) {
                sb.append(meta.getColumnName(i));
                if (i < colCount) sb.append(",");
            }
            sb.append("\n");

            // CSV rows
            int rowCount = 0;
            while (rs.next()) {
                for (int i = 1; i <= colCount; i++) {
                    String val = rs.getString(i);
                    if (val == null) {
                        sb.append("-");
                    } else {
                        val = val.replace(",", ";").replace("\n", " ");
                        if (val.length() > 100) val = val.substring(0, 97) + "...";
                        sb.append(val);
                    }
                    if (i < colCount) sb.append(",");
                }
                sb.append("\n");
                rowCount++;
            }
            if (rowCount == 0) sb.append("(empty)\n");
        } catch (Exception e) {
            sb.append("err:").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Validate table name against whitelist to prevent SQL injection.
     */
    private boolean isValidTableName(String tableName) {
        String[] validTables = {"user", "formation", "avis", "application", "training_request"};
        for (String valid : validTables) {
            if (valid.equalsIgnoreCase(tableName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Send query to GPT-oss-120B with full DB context.
     */
    private String askAiWithDbContext(String query, String dbContent) throws IOException, InterruptedException {
        return askAiWithDbContext(query, dbContent, java.util.List.of());
    }

    /**
     * Send query to GPT-oss-120B with DB context, recent history,
     * dynamic token budgeting, and typo/slang interpretation.
     */
    private String askAiWithDbContext(String query, String dbContent, java.util.List<ChatMessage> recentHistory)
        throws IOException, InterruptedException {

        String apiToken = EnvConfig.getCloudflareApiToken();
        String accountId = EnvConfig.getCloudflareAccountId();

        if (apiToken == null || apiToken.isBlank() || accountId == null || accountId.isBlank()) {
            return "❌ AI service not configured. Please set CLOUDFLARE_API_TOKEN and CLOUDFLARE_ACCOUNT_ID.";
        }

        String endpoint = "https://api.cloudflare.com/client/v4/accounts/" + accountId
            + "/ai/run/" + MODEL_ID;

        // ── Compact system prompt: bee personality, no IDs, straight answers ──
        String systemBase = "You are BizHub's Bee Assistant \uD83D\uDC1D\uD83C\uDF6F — buzzy, joyful, and helpful!\n" +
            "RULES:\n" +
            "- Answer ONLY from the DB data below. NEVER invent data.\n" +
            "- NEVER show IDs (user_id, formation_id, avis_id, etc). Use names/titles instead.\n" +
            "- Give the answer directly — NO thinking process, NO step-by-step reasoning.\n" +
            "- Be short, cheerful & bee-themed (honey, buzz, hive puns welcome \uD83D\uDC1D).\n" +
            "- Interpret typos & slang (formaiton=formation, popping/trending=popular, " +
            "rn=right now, lit/fire=great, mid=average, goated=best).\n" +
            "- If data not found, say: \"Bzz... I couldn't find that in the hive! \uD83D\uDC1D\"\n\nDB:\n";

        // ── Token-budget management (128K context — rarely an issue) ──
        String effectiveDb = dbContent;
        java.util.List<ChatMessage> usableHistory = (recentHistory != null)
            ? new java.util.ArrayList<>(recentHistory) : new java.util.ArrayList<>();

        int overhead     = 30;
        int baseTokens   = estimateTokens(systemBase) + estimateTokens(query) + overhead;
        int dbTokens     = estimateTokens(effectiveDb);
        int histTokens   = 0;
        for (ChatMessage m : usableHistory) {
            if (m != null && m.content() != null) histTokens += estimateTokens(m.content()) + 10;
        }

        int totalInput = baseTokens + dbTokens + histTokens;
        int budget     = CONTEXT_WINDOW - MIN_OUTPUT_TOKENS;

        // Safety: drop oldest history if somehow over budget
        while (totalInput > budget && !usableHistory.isEmpty()) {
            ChatMessage dropped = usableHistory.remove(0);
            int freed = estimateTokens(dropped.content()) + 10;
            totalInput -= freed;
        }

        // Safety: truncate DB if still over (unlikely with 128K)
        if (totalInput > budget) {
            int nonDb = totalInput - dbTokens;
            int allowedDbChars = (int) (Math.max(0, budget - nonDb) * 3.5);
            if (allowedDbChars > 200) {
                effectiveDb = effectiveDb.substring(0, Math.min(effectiveDb.length(), allowedDbChars))
                            + "\n...(truncated)";
            } else {
                effectiveDb = "(context too large — ask a simpler question)";
            }
            totalInput = nonDb + estimateTokens(effectiveDb);
        }

        int maxTokens = Math.min(MAX_OUTPUT_TOKENS,
            Math.max(MIN_OUTPUT_TOKENS, CONTEXT_WINDOW - totalInput));

        String systemPrompt = systemBase + effectiveDb;

        // ── Build messages JSON ──
        StringBuilder messagesJson = new StringBuilder();
        messagesJson.append("[{\"role\":\"system\",\"content\":")
            .append(toJsonString(systemPrompt)).append("}");

        for (ChatMessage m : usableHistory) {
            if (m == null || m.content() == null || m.content().isBlank()) continue;
            messagesJson.append(",{\"role\":").append(toJsonString(m.role()))
                .append(",\"content\":").append(toJsonString(m.content())).append("}");
        }

        messagesJson.append(",{\"role\":\"user\",\"content\":")
            .append(toJsonString(query)).append("}]");

        String jsonBody = "{\"messages\":" + messagesJson
            + ",\"max_tokens\":" + maxTokens + "}";



        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(60))
            .header("Authorization", "Bearer " + apiToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        int status = res.statusCode();
        if (status < 200 || status >= 300) {
            String errorBody = res.body();
            System.err.println("Cloudflare API error (HTTP " + status + "): " + errorBody);
            return "❌ Error connecting to AI (HTTP " + status + "). "
                + errorBody.substring(0, Math.min(150, errorBody.length()));
        }

        // ── Parse response + log token usage ──
        String responseBody = res.body();
        try {
            JSONObject json = new JSONObject(responseBody);

            // Log token usage from the response
            JSONObject usage = json.optJSONObject("usage");
            if (usage == null) {
                JSONObject r = json.optJSONObject("result");
                if (r != null) usage = r.optJSONObject("usage");
            }
            if (usage != null) {
                System.out.println("[AI Token Usage] prompt=" + usage.optInt("prompt_tokens", -1)
                    + " completion=" + usage.optInt("completion_tokens", -1)
                    + " total=" + usage.optInt("total_tokens", -1)
                    + " (window=" + CONTEXT_WINDOW + ")");
            }

            // GPT-oss-120B returns OpenAI-style: {"choices":[{"message":{"content":"..."}}]}
            // May be at top level OR wrapped in Cloudflare {"result":{...}}
            String response = null;

            // Determine the payload object (could be top-level or inside "result")
            JSONObject payload = json;
            if (!json.has("choices") && json.has("result")) {
                Object resultVal = json.get("result");
                if (resultVal instanceof JSONObject) {
                    payload = (JSONObject) resultVal;
                }
            }

            // Try OpenAI chat.completion format: choices[0].message.content
            JSONArray choices = payload.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject message = firstChoice.optJSONObject("message");
                if (message != null) {
                    response = message.optString("content", null);
                }
            }

            // Fallback: old Cloudflare Workers AI flat format
            if (response == null) {
                for (String key : new String[]{"response", "content", "text", "message"}) {
                    if (payload.has(key)) {
                        Object v = payload.get(key);
                        if (v instanceof String) { response = (String) v; break; }
                    }
                }
            }

            System.out.println("[AI Parse] response " + (response == null ? "NULL" : "len=" + response.length())
                + " | top-level keys=" + json.keySet()
                + " | payload keys=" + payload.keySet());

            if (response == null || response.isBlank()) {
                System.err.println("Empty AI response. Body: " + responseBody);
                return "❌ Hmm… my hive mind came back empty. Could you rephrase?";
            }

            return response.trim();
        } catch (Exception e) {
            System.err.println("Error parsing AI response: " + e.getMessage() + " | body=" + responseBody);
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

    /**
     * Minimal representation of chat history messages sent to the AI.
     * role must be either "user" or "assistant".
     */
    public record ChatMessage(String role, String content) {
        public ChatMessage {
            if (role == null || (!role.equals("user") && !role.equals("assistant"))) {
                throw new IllegalArgumentException("role must be 'user' or 'assistant'");
            }
            if (content == null) {
                content = "";
            }
        }
    }

    // Result record
    public record DatabaseQueryResult(String response, boolean success) {}
}
