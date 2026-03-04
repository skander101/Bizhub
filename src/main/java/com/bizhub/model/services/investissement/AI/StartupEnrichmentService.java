package com.bizhub.model.services.investissement.AI;

import com.bizhub.model.services.common.DB.MyDatabase;
import com.google.gson.*;
import javafx.concurrent.Task;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * Enriches project/startup data by:
 * 1. Fetching company logos via Clearbit Logo API (no key needed).
 * 2. Using OpenRouter AI to generate company insights (employees estimate,
 *    suggested sector, competitors, market size).
 * 3. Persisting enriched fields (logo_url, website_url) back to the project table.
 */
public class StartupEnrichmentService {
    private static final Logger logger = Logger.getLogger(StartupEnrichmentService.class.getName());
    private final OpenRouterService openRouterService;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public StartupEnrichmentService() {
        this.openRouterService = new OpenRouterService();
    }

    public record EnrichmentResult(
            String logoUrl,
            String websiteUrl,
            String suggestedSector,
            String estimatedEmployees,
            String marketSize,
            JsonArray competitors,
            String companyInsight
    ) {}

    /**
     * Clearbit Logo API: returns a logo URL for a given domain.
     * Free, no auth needed. Returns a 1x1 pixel for unknown domains.
     */
    public String fetchLogo(String domain) {
        if (domain == null || domain.isBlank()) return null;
        String clean = domain.replaceAll("https?://", "").replaceAll("/.*", "").strip();
        if (clean.isBlank()) return null;
        String logoUrl = "https://logo.clearbit.com/" + clean;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(logoUrl).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            if (code == 200) {
                // Check content type to avoid 1x1 pixel fallback
                String ct = conn.getContentType();
                if (ct != null && ct.startsWith("image/")) {
                    return logoUrl;
                }
            }
        } catch (Exception e) {
            logger.fine("Logo fetch failed for " + clean + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Uses AI to generate company insights based on project title, description, and sector.
     */
    public EnrichmentResult enrich(String projectTitle, String projectDescription, String existingWebsite) {
        String logoUrl = fetchLogo(existingWebsite);

        String systemPrompt = """
            You are a startup research analyst. Given the project info below, provide
            enrichment data that helps investors understand the company better.
            
            Return ONLY valid JSON (no markdown fences):
            {
              "suggestedSector": "<most appropriate sector>",
              "estimatedEmployees": "<range like '1-10', '10-50', '50-200', '200+'>",
              "marketSize": "<estimated TAM like '$500M', '$1.2B'>",
              "competitors": ["<competitor1>", "<competitor2>", "<competitor3>"],
              "companyInsight": "<2-3 sentence insight about the company's positioning and potential>",
              "suggestedDomain": "<guessed company website domain if none provided, e.g. 'company.com'>"
            }
            
            Be realistic and specific to the project description.
            """;

        String userMsg = "Project: " + projectTitle + "\nDescription: " + projectDescription +
                "\nWebsite: " + (existingWebsite != null ? existingWebsite : "not provided");

        String suggestedSector = "";
        String estimatedEmployees = "";
        String marketSize = "";
        JsonArray competitors = new JsonArray();
        String companyInsight = "";

        try {
            String response = openRouterService.chat(systemPrompt, userMsg);
            String cleaned = stripMarkdownFences(response);
            JsonObject parsed = JsonParser.parseString(cleaned).getAsJsonObject();

            suggestedSector = parsed.has("suggestedSector") ? parsed.get("suggestedSector").getAsString() : "";
            estimatedEmployees = parsed.has("estimatedEmployees") ? parsed.get("estimatedEmployees").getAsString() : "";
            marketSize = parsed.has("marketSize") ? parsed.get("marketSize").getAsString() : "";
            competitors = parsed.has("competitors") ? parsed.getAsJsonArray("competitors") : new JsonArray();
            companyInsight = parsed.has("companyInsight") ? parsed.get("companyInsight").getAsString() : "";

            if (logoUrl == null && parsed.has("suggestedDomain")) {
                String suggested = parsed.get("suggestedDomain").getAsString();
                logoUrl = fetchLogo(suggested);
            }
        } catch (Exception e) {
            logger.warning("AI enrichment failed: " + e.getMessage());
            companyInsight = "AI enrichment unavailable.";
        }

        return new EnrichmentResult(
                logoUrl, existingWebsite, suggestedSector,
                estimatedEmployees, marketSize, competitors, companyInsight
        );
    }

    public Task<EnrichmentResult> enrichAsync(String projectTitle, String projectDescription, String existingWebsite) {
        return new Task<>() {
            @Override
            protected EnrichmentResult call() {
                return StartupEnrichmentService.this.enrich(projectTitle, projectDescription, existingWebsite);
            }
        };
    }

    /**
     * Persist logo_url and website_url to the project table.
     */
    public void saveEnrichment(int projectId, String logoUrl, String websiteUrl) {
        Connection conn = MyDatabase.getInstance().getCnx();
        String sql = "UPDATE project SET logo_url = ?, website_url = ? WHERE project_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, logoUrl);
            stmt.setString(2, websiteUrl);
            stmt.setInt(3, projectId);
            stmt.executeUpdate();
        } catch (Exception e) {
            logger.warning("Error saving enrichment: " + e.getMessage());
        }
    }

    private String stripMarkdownFences(String raw) {
        if (raw == null) return "{}";
        String s = raw.strip();
        if (s.startsWith("```")) {
            int first = s.indexOf('\n');
            if (first > 0) s = s.substring(first + 1);
        }
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        return s.strip();
    }
}
