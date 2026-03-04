package com.bizhub.model.services.investissement.AI;

import com.google.gson.*;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import javafx.concurrent.Task;

import java.io.File;
import java.util.logging.Logger;

/**
 * Extracts text from generated PDF contracts and sends it to
 * OpenRouter for AI legal review. Returns structured clause analysis,
 * risk flags, and improvement suggestions.
 */
public class ContractReviewService {
    private static final Logger logger = Logger.getLogger(ContractReviewService.class.getName());
    private final OpenRouterService openRouterService;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ContractReviewService() {
        this.openRouterService = new OpenRouterService();
    }

    public record ClauseReview(
            String rawJson,
            JsonObject parsed
    ) {}

    public String extractPdfText(String pdfPath) throws Exception {
        File file = new File(pdfPath);
        if (!file.exists()) throw new IllegalArgumentException("PDF not found: " + pdfPath);

        StringBuilder sb = new StringBuilder();
        try (PdfReader reader = new PdfReader(file);
             PdfDocument pdf = new PdfDocument(reader)) {
            for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(pdf.getPage(i)));
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public ClauseReview reviewContract(String pdfPath) throws Exception {
        String text = extractPdfText(pdfPath);
        if (text.isBlank()) {
            JsonObject empty = new JsonObject();
            empty.addProperty("overallRating", "N/A");
            empty.addProperty("summary", "Could not extract text from the PDF.");
            return new ClauseReview(gson.toJson(empty), empty);
        }

        String systemPrompt = """
            You are a senior legal advisor specializing in investment contracts.
            Review the contract text below and provide a thorough clause-by-clause analysis.
            
            Return ONLY valid JSON (no markdown fences):
            {
              "overallRating": "<Excellent|Good|Fair|Needs Revision|Risky>",
              "overallScore": <1-10, 10 = perfect>,
              "summary": "<2-3 sentence overview of the contract quality>",
              "clauses": [
                {
                  "clauseName": "<name of the clause>",
                  "rating": "<Safe|Acceptable|Caution|Risky>",
                  "riskLevel": <1-5, 5 = highest risk>,
                  "excerpt": "<brief quote from the contract>",
                  "analysis": "<what this clause means for the investor>",
                  "suggestion": "<how to improve or what to negotiate>"
                }
              ],
              "redFlags": [
                "<specific risk or missing protection>"
              ],
              "missingClauses": [
                "<clause that should be in the contract but is missing>"
              ],
              "negotiationTips": [
                "<actionable tip for the investor before signing>"
              ],
              "verdict": "<Should the investor sign as-is, request changes, or decline?>"
            }
            
            Guidelines:
            - Analyze ALL major clauses (payment terms, liability, termination, IP, confidentiality, etc.).
            - Flag any clause that is unusually one-sided or missing standard protections.
            - Be specific: reference actual amounts, dates, and terms from the contract.
            - Provide practical, actionable suggestions.
            """;

        String response = openRouterService.chat(systemPrompt, "CONTRACT TEXT:\n\n" + text);
        String cleaned = stripMarkdownFences(response);
        JsonObject parsed;
        try {
            parsed = JsonParser.parseString(cleaned).getAsJsonObject();
        } catch (Exception e) {
            parsed = new JsonObject();
            parsed.addProperty("summary", cleaned);
            parsed.addProperty("overallRating", "Unknown");
        }
        return new ClauseReview(cleaned, parsed);
    }

    public Task<ClauseReview> reviewContractAsync(String pdfPath) {
        return new Task<>() {
            @Override
            protected ClauseReview call() throws Exception {
                return ContractReviewService.this.reviewContract(pdfPath);
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
