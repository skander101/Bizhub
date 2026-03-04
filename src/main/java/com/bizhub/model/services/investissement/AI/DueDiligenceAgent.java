package com.bizhub.model.services.investissement.AI;

import com.bizhub.model.investissement.Project;
import com.bizhub.model.services.investissement.AlphaVantageService;
import com.bizhub.model.services.investissement.GNewsService;
import com.google.gson.*;
import javafx.concurrent.Task;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Orchestrates a multi-step AI due diligence pipeline for a single project.
 *
 * Steps:
 *  1) Pitch analysis via OpenRouter (risk, verdict, strengths/weaknesses, valuation).
 *  2) Market news snapshot via GNews for the project/company.
 *  3) Sector performance snapshot via AlphaVantage.
 *  4) AI synthesis via OpenRouter using all gathered data into a rich JSON report.
 *
 * The final JSON structure returned by synthesize() is suitable for:
 *  - Rendering in a rich JavaFX window (cards, gauges, sections).
 *  - Exporting as a branded PDF.
 */
public class DueDiligenceAgent {

    private static final Logger logger = Logger.getLogger(DueDiligenceAgent.class.getName());

    private final OpenRouterService openRouterService;
    private final GNewsService gNewsService;
    private final AlphaVantageService alphaVantageService;
    private final Gson gson;

    public DueDiligenceAgent() {
        this.openRouterService = new OpenRouterService();
        this.gNewsService = new GNewsService();
        this.alphaVantageService = new AlphaVantageService();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Convenience DTO for the final synthesized report.
     * The UI layer can either parse the JSON string directly or work with this object.
     */
    public record DueDiligenceResult(
            String rawJsonReport,
            JsonObject reportObject
    ) {}

    /**
     * Run the full due diligence pipeline synchronously.
     *
     * @param project the project to analyze
     * @return structured result (raw JSON + parsed object)
     * @throws Exception on API failures
     */
    public DueDiligenceResult run(Project project) throws Exception {
        if (project == null) {
            throw new IllegalArgumentException("project cannot be null");
        }

        // --- Step 1: Pitch analysis ---
        logger.info("[DueDiligence] Step 1/4: pitch analysis via OpenRouter");
        String pitchJson = safePitchAnalysis(project);

        // --- Step 2: Market news via GNews ---
        logger.info("[DueDiligence] Step 2/4: fetching market news via GNews");
        JsonArray newsArray = safeNewsSnapshot(project);

        // --- Step 3: Sector performance via AlphaVantage ---
        logger.info("[DueDiligence] Step 3/4: fetching sector performance via AlphaVantage");
        JsonObject sectorSnapshot = safeSectorSnapshot();

        // --- Step 4: AI synthesis ---
        logger.info("[DueDiligence] Step 4/4: synthesizing final due diligence report via OpenRouter");
        String synthesized = synthesizeReport(project, pitchJson, newsArray, sectorSnapshot);

        JsonObject parsed = tryParseJson(synthesized);
        return new DueDiligenceResult(synthesized, parsed);
    }

    /**
     * Async wrapper for running the pipeline.
     */
    public Task<DueDiligenceResult> runAsync(Project project) {
        return new Task<>() {
            @Override
            protected DueDiligenceResult call() throws Exception {
                // Explicitly call outer class method, not FutureTask.run()
                return DueDiligenceAgent.this.run(project);
            }
        };
    }

    // -------------------------------------------------------------------------
    // Individual steps (with defensive fallbacks)
    // -------------------------------------------------------------------------

    private String safePitchAnalysis(Project project) {
        try {
            String desc = project.getDescription() != null ? project.getDescription() : "No description provided";
            double budget = project.getRequiredBudget() != null ? project.getRequiredBudget().doubleValue() : 0.0;
            String json = openRouterService.analyzePitch(
                    project.getTitle(),
                    desc,
                    budget
            );
            return stripMarkdownFences(json);
        } catch (Exception e) {
            logger.warning("[DueDiligence] Pitch analysis failed: " + e.getMessage());
            JsonObject fallback = new JsonObject();
            fallback.addProperty("riskScore", 0);
            fallback.addProperty("verdict", "Pitch analysis unavailable");
            fallback.add("summary", emptyArray());
            fallback.add("strengths", emptyArray());
            fallback.add("weaknesses", emptyArray());
            fallback.addProperty("recommendation", "The AI service was unavailable. Please try again later.");
            fallback.addProperty("valuationMin", 0);
            fallback.addProperty("valuationMax", 0);
            return gson.toJson(fallback);
        }
    }

    private JsonArray safeNewsSnapshot(Project project) {
        try {
            String query = project.getTitle();
            if (query == null || query.isBlank()) {
                return new JsonArray();
            }
            List<GNewsService.Article> articles = gNewsService.fetchNews(query, 6);
            JsonArray arr = new JsonArray();
            for (GNewsService.Article a : articles) {
                JsonObject obj = new JsonObject();
                obj.addProperty("title", a.getTitle());
                obj.addProperty("description", a.getDescription());
                obj.addProperty("url", a.getUrl());
                obj.addProperty("imageUrl", a.getImageUrl());
                obj.addProperty("publishedAt", a.getPublishedAt());
                obj.addProperty("sourceName", a.getSourceName());
                obj.addProperty("sourceUrl", a.getSourceUrl());
                arr.add(obj);
            }
            return arr;
        } catch (Exception e) {
            logger.warning("[DueDiligence] News snapshot failed: " + e.getMessage());
            return new JsonArray();
        }
    }

    private JsonObject safeSectorSnapshot() {
        JsonObject sectorsJson = new JsonObject();
        try {
            Map<String, Double> sectors = alphaVantageService.getSectorPerformance();
            for (Map.Entry<String, Double> entry : sectors.entrySet()) {
                sectorsJson.addProperty(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            logger.warning("[DueDiligence] Sector snapshot failed: " + e.getMessage());
        }
        return sectorsJson;
    }

    /**
     * Final synthesis call to OpenRouter. Combines:
     *  - project core data
     *  - pitch analysis JSON
     *  - news articles array
     *  - sector performance snapshot
     */
    private String synthesizeReport(Project project,
                                    String pitchJson,
                                    JsonArray newsArray,
                                    JsonObject sectorSnapshot) throws Exception {

        JsonObject payload = new JsonObject();

        JsonObject projectObj = new JsonObject();
        projectObj.addProperty("id", project.getProjectId());
        projectObj.addProperty("title", project.getTitle());
        projectObj.addProperty("description", project.getDescription());
        projectObj.addProperty("requiredBudget", project.getRequiredBudget() != null
                ? project.getRequiredBudget().doubleValue() : 0.0);
        projectObj.addProperty("status", project.getStatus());

        JsonObject pitchObj;
        try {
            pitchObj = gson.fromJson(pitchJson, JsonObject.class);
        } catch (Exception e) {
            pitchObj = new JsonObject();
            pitchObj.addProperty("raw", pitchJson);
        }

        payload.add("project", projectObj);
        payload.add("pitchAnalysis", pitchObj);
        payload.add("news", newsArray);
        payload.add("sectorPerformance", sectorSnapshot);

        String systemPrompt = """
            You are a senior investment analyst. Produce a due diligence report as a SINGLE JSON object.
            CRITICAL: Return ONLY valid JSON. No markdown. No backticks. No comments. Keep strings SHORT (under 80 chars each).

            Required JSON shape (keep all strings concise):
            {
              "projectSummary":{"title":"...","oneLine":"short summary","keyNumbers":["stat1","stat2"]},
              "riskMatrix":{"financial":5,"market":5,"team":5,"execution":5,"regulatory":5,"overallComment":"short"},
              "swot":{"strengths":["s1","s2"],"weaknesses":["w1","w2"],"opportunities":["o1"],"threats":["t1"]},
              "marketContext":{"headlineSummary":["headline1"],"tone":"mixed","notes":"short note"},
              "sectorOutlook":{"keySectors":[{"name":"Tech","performance":2.5,"comment":"short"}],"overallComment":"short"},
              "valuation":{"valuationMin":10000,"valuationMax":50000,"comment":"short"},
              "finalRecommendation":{"label":"INVEST","summary":"short advice","nextSteps":["step1","step2"]}
            }

            Rules: max 2 items per array. Keep every string under 80 characters. Be specific to the project data provided.
            """;

        String userMessage = gson.toJson(payload);
        String raw = openRouterService.chat(systemPrompt, userMessage, 4096);
        return stripMarkdownFences(raw);
    }

    /**
     * Attempts to parse JSON, and if it fails (truncated response),
     * tries to repair it by closing open braces/brackets/strings.
     */
    private JsonObject tryParseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return fallbackReport("Empty AI response");
        }

        // First try: direct parse
        try {
            return gson.fromJson(raw, JsonObject.class);
        } catch (Exception ignored) {}

        // Second try: attempt to repair truncated JSON
        String repaired = repairJson(raw);
        try {
            JsonElement el = JsonParser.parseString(repaired);
            if (el.isJsonObject()) return el.getAsJsonObject();
        } catch (Exception ignored) {}

        // Third try: use lenient parsing
        try {
            com.google.gson.stream.JsonReader reader =
                    new com.google.gson.stream.JsonReader(new java.io.StringReader(raw));
            reader.setLenient(true);
            JsonElement el = JsonParser.parseReader(reader);
            if (el.isJsonObject()) return el.getAsJsonObject();
        } catch (Exception ignored) {}

        logger.warning("[DueDiligence] All JSON parse attempts failed, building fallback from raw content");
        return fallbackReport(raw);
    }

    private String repairJson(String json) {
        StringBuilder sb = new StringBuilder(json);
        int braces = 0, brackets = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') braces++;
            else if (c == '}') braces--;
            else if (c == '[') brackets++;
            else if (c == ']') brackets--;
        }

        // If we ended inside a string, close it
        if (inString) sb.append('"');
        // Close any open brackets/braces
        while (brackets > 0) { sb.append(']'); brackets--; }
        while (braces > 0) { sb.append('}'); braces--; }

        return sb.toString();
    }

    private JsonObject fallbackReport(String rawContent) {
        JsonObject fb = new JsonObject();
        JsonObject ps = new JsonObject();
        ps.addProperty("title", "Due Diligence Report");
        ps.addProperty("oneLine", "Report generated with partial data");
        JsonArray kn = new JsonArray();
        kn.add("AI analysis completed with partial results");
        ps.add("keyNumbers", kn);
        fb.add("projectSummary", ps);

        JsonObject rec = new JsonObject();
        rec.addProperty("label", "WATCH");
        String summary = rawContent.length() > 300 ? rawContent.substring(0, 300) + "..." : rawContent;
        rec.addProperty("summary", summary);
        JsonArray steps = new JsonArray();
        steps.add("Re-run the analysis for complete results");
        steps.add("Review project details manually");
        rec.add("nextSteps", steps);
        fb.add("finalRecommendation", rec);

        return fb;
    }

    private JsonArray emptyArray() {
        return new JsonArray();
    }

    private String stripMarkdownFences(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            // Remove leading ```json or ``` and trailing ```
            trimmed = trimmed.replaceAll("^```[a-zA-Z0-9]*\\s*", "");
            trimmed = trimmed.replaceAll("```\\s*$", "");
        }
        return trimmed.trim();
    }
}

