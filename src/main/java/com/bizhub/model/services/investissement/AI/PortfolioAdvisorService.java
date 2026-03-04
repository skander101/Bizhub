package com.bizhub.model.services.investissement.AI;

import com.bizhub.model.services.common.DB.MyDatabase;
import com.google.gson.*;
import javafx.concurrent.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.logging.Logger;

/**
 * Aggregates an investor's portfolio data from the database,
 * computes diversification metrics, and sends everything
 * to OpenRouter for AI-powered advice.
 */
public class PortfolioAdvisorService {
    private static final Logger logger = Logger.getLogger(PortfolioAdvisorService.class.getName());
    private final OpenRouterService openRouterService;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public PortfolioAdvisorService() {
        this.openRouterService = new OpenRouterService();
    }

    public record PortfolioHolding(
            int projectId, String projectTitle, String sector,
            double investedAmount, double requiredBudget,
            String status, String investmentDate
    ) {}

    public record PortfolioSnapshot(
            int investorId,
            List<PortfolioHolding> holdings,
            double totalInvested,
            int projectCount,
            Map<String, Double> sectorAllocation,
            Map<String, Double> statusBreakdown,
            double diversificationScore
    ) {}

    public record PortfolioAdvice(
            String rawJson,
            JsonObject parsed
    ) {}

    private Connection getConn() {
        return MyDatabase.getInstance().getCnx();
    }

    public PortfolioSnapshot loadPortfolio(int investorId) {
        List<PortfolioHolding> holdings = new ArrayList<>();
        String sql = """
            SELECT i.project_id, p.title, COALESCE(u.sector, 'Other') AS sector,
                   i.amount, p.required_budget, p.status,
                   DATE_FORMAT(i.investment_date, '%Y-%m-%d') AS inv_date
            FROM investment i
            JOIN project p ON i.project_id = p.project_id
            LEFT JOIN user u ON p.startup_id = u.user_id
            WHERE i.investor_id = ?
            ORDER BY i.investment_date DESC""";

        try (PreparedStatement stmt = getConn().prepareStatement(sql)) {
            stmt.setInt(1, investorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    holdings.add(new PortfolioHolding(
                            rs.getInt("project_id"),
                            rs.getString("title"),
                            rs.getString("sector"),
                            rs.getDouble("amount"),
                            rs.getDouble("required_budget"),
                            rs.getString("status"),
                            rs.getString("inv_date")
                    ));
                }
            }
        } catch (Exception e) {
            logger.warning("Error loading portfolio: " + e.getMessage());
        }

        double totalInvested = holdings.stream().mapToDouble(PortfolioHolding::investedAmount).sum();
        Map<String, Double> sectorAlloc = new LinkedHashMap<>();
        Map<String, Double> statusBreak = new LinkedHashMap<>();

        for (PortfolioHolding h : holdings) {
            sectorAlloc.merge(h.sector(), h.investedAmount(), Double::sum);
            statusBreak.merge(h.status(), h.investedAmount(), Double::sum);
        }

        // Normalize to percentages
        if (totalInvested > 0) {
            sectorAlloc.replaceAll((k, v) -> Math.round(v / totalInvested * 1000.0) / 10.0);
            statusBreak.replaceAll((k, v) -> Math.round(v / totalInvested * 1000.0) / 10.0);
        }

        double diversification = computeDiversificationScore(sectorAlloc, holdings.size());

        return new PortfolioSnapshot(
                investorId, holdings, totalInvested,
                holdings.size(), sectorAlloc, statusBreak, diversification
        );
    }

    /**
     * Herfindahl-Hirschman based diversification score (0-100).
     * Lower HHI = more diversified = higher score.
     */
    private double computeDiversificationScore(Map<String, Double> sectorAlloc, int projectCount) {
        if (sectorAlloc.isEmpty()) return 0;
        double hhi = 0;
        for (double pct : sectorAlloc.values()) {
            hhi += (pct / 100.0) * (pct / 100.0);
        }
        // HHI ranges from 1/n to 1. Map to 0-100 score.
        double raw = (1.0 - hhi) * 100;
        // Bonus for number of projects (up to 10 pts)
        double projectBonus = Math.min(projectCount * 2.0, 10.0);
        return Math.min(100, Math.round((raw + projectBonus) * 10.0) / 10.0);
    }

    public PortfolioAdvice analyze(int investorId) throws Exception {
        PortfolioSnapshot snapshot = loadPortfolio(investorId);

        if (snapshot.holdings().isEmpty()) {
            JsonObject empty = new JsonObject();
            empty.addProperty("portfolioHealth", "N/A");
            empty.addProperty("summary", "No investments found. Start investing to get AI portfolio advice.");
            empty.add("suggestions", new JsonArray());
            return new PortfolioAdvice(gson.toJson(empty), empty);
        }

        String systemPrompt = """
            You are a senior portfolio advisor at a venture capital firm.
            Analyze the investor's portfolio below and provide actionable, data-driven advice.
            
            Return ONLY valid JSON (no markdown fences):
            {
              "portfolioHealth": "<Excellent|Good|Fair|Needs Attention|Critical>",
              "diversificationRating": "<Well Diversified|Moderately Diversified|Concentrated|Highly Concentrated>",
              "summary": "<2-3 sentence portfolio overview>",
              "sectorAnalysis": [
                {"sector": "<name>", "allocation": <percent>, "comment": "<insight>", "action": "<hold|reduce|increase>"}
              ],
              "riskAssessment": {
                "overallRisk": <1-10>,
                "concentrationRisk": "<Low|Medium|High>",
                "stageRisk": "<Low|Medium|High>",
                "comment": "<1-2 sentence risk summary>"
              },
              "rebalanceSuggestions": [
                {"action": "<Increase|Decrease|Add|Remove>", "target": "<sector or project>", "reason": "<why>"}
              ],
              "topPerformers": ["<project name> - <reason>"],
              "watchlist": ["<project name> - <concern>"],
              "nextSteps": ["<actionable step 1>", "<actionable step 2>", "<actionable step 3>"]
            }
            
            Guidelines:
            - Be specific to THIS investor's actual holdings (reference project names, sectors, amounts).
            - Consider sector concentration, project status mix, and total exposure.
            - Suggest concrete rebalancing moves with reasoning.
            - Flag any projects in risky statuses (pending, unfunded).
            """;

        String userData = gson.toJson(snapshot);
        String response = openRouterService.chat(systemPrompt, userData);
        String cleaned = stripMarkdownFences(response);
        JsonObject parsed;
        try {
            parsed = JsonParser.parseString(cleaned).getAsJsonObject();
        } catch (Exception e) {
            parsed = new JsonObject();
            parsed.addProperty("summary", cleaned);
            parsed.addProperty("portfolioHealth", "Unknown");
        }
        return new PortfolioAdvice(cleaned, parsed);
    }

    public Task<PortfolioAdvice> analyzeAsync(int investorId) {
        return new Task<>() {
            @Override
            protected PortfolioAdvice call() throws Exception {
                return PortfolioAdvisorService.this.analyze(investorId);
            }
        };
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
