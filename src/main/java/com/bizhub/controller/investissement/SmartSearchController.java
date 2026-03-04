package com.bizhub.controller.investissement;

import com.bizhub.model.investissement.Project;
import com.bizhub.model.services.investissement.AI.OpenRouterService;
import com.bizhub.model.services.investissement.ProjectServiceImpl;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import com.bizhub.controller.users_avis.user.SidebarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class SmartSearchController {

    @FXML private SidebarController sidebarController;
    @FXML private TextField searchField;
    @FXML private VBox resultsContainer;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator searchSpinner;
    @FXML private Label resultCountLabel;

    private OpenRouterService aiService;
    private ProjectServiceImpl projectService;
    private NumberFormat currencyFormat;

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("smart-search");
        aiService = new OpenRouterService();
        projectService = new ProjectServiceImpl();
        currencyFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
        currencyFormat.setMaximumFractionDigits(2);
        searchSpinner.setVisible(false);
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        searchSpinner.setVisible(true);
        statusLabel.setText("AI is analyzing your query...");
        resultsContainer.getChildren().clear();

        javafx.concurrent.Task<String> aiTask = aiService.parseSearchQueryAsync(query);

        aiTask.setOnSucceeded(e -> Platform.runLater(() -> {
            try {
                String aiResult = aiTask.getValue();
                String cleaned = aiResult.strip();
                if (cleaned.startsWith("```")) {
                    cleaned = cleaned.replaceAll("^```[a-z]*\\s*", "").replaceAll("```\\s*$", "").strip();
                }
                JsonObject filters = JsonParser.parseString(cleaned).getAsJsonObject();

                List<Project> allProjects = projectService.getAllWithStats();
                List<ScoredProject> scored = scoreAndRank(allProjects, filters);

                displayScoredResults(scored, filters);
                searchSpinner.setVisible(false);
                statusLabel.setText("AI found " + scored.size() + " matching project(s)");
                resultCountLabel.setText(scored.size() + " results");
            } catch (Exception ex) {
                fallbackSmartSearch(query);
            }
        }));

        aiTask.setOnFailed(e -> Platform.runLater(() -> fallbackSmartSearch(query)));

        new Thread(aiTask).start();
    }

    private record ScoredProject(Project project, double score, String budgetTag) {}

    private void fallbackSmartSearch(String query) {
        try {
            List<Project> allProjects = projectService.getAllWithStats();
            String[] tokens = query.toLowerCase().split("\\s+");

            List<ScoredProject> scored = allProjects.stream().map(p -> {
                double score = 0;
                String title = p.getTitle() != null ? p.getTitle().toLowerCase() : "";
                String desc = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
                for (String tok : tokens) {
                    if (tok.length() < 2) continue;
                    if (title.contains(tok)) score += 10;
                    if (desc.contains(tok)) score += 5;
                }
                return new ScoredProject(p, score, "");
            }).filter(sp -> sp.score > 0)
              .sorted(Comparator.comparingDouble(ScoredProject::score).reversed())
              .collect(Collectors.toList());

            displayScoredResults(scored, null);
            searchSpinner.setVisible(false);
            statusLabel.setText("Smart search: " + scored.size() + " result(s)");
            resultCountLabel.setText(scored.size() + " results");
        } catch (SQLException ex) {
            searchSpinner.setVisible(false);
            statusLabel.setText("Search error: " + ex.getMessage());
        }
    }

    private List<ScoredProject> scoreAndRank(List<Project> projects, JsonObject filters) {
        List<String> keywords = new ArrayList<>();
        if (filters.has("keywords") && filters.get("keywords").isJsonArray()) {
            for (var el : filters.getAsJsonArray("keywords")) {
                keywords.add(el.getAsString().toLowerCase());
            }
        }
        // Legacy: single "keyword" field
        if (keywords.isEmpty() && filters.has("keyword") && !filters.get("keyword").isJsonNull()) {
            for (String w : filters.get("keyword").getAsString().toLowerCase().split("\\s+")) {
                if (w.length() >= 2) keywords.add(w);
            }
        }

        Double userBudget = null;
        if (filters.has("budget") && !filters.get("budget").isJsonNull()) {
            userBudget = filters.get("budget").getAsDouble();
        } else if (filters.has("maxBudget") && !filters.get("maxBudget").isJsonNull()) {
            userBudget = filters.get("maxBudget").getAsDouble();
        }

        String statusFilter = null;
        if (filters.has("status") && !filters.get("status").isJsonNull()) {
            statusFilter = filters.get("status").getAsString().toLowerCase();
        }

        List<ScoredProject> results = new ArrayList<>();
        for (Project p : projects) {
            double score = 0;
            String title = p.getTitle() != null ? p.getTitle().toLowerCase() : "";
            String desc = p.getDescription() != null ? p.getDescription().toLowerCase() : "";

            // Keyword scoring: each matching keyword boosts score
            for (String kw : keywords) {
                if (title.contains(kw)) score += 15;
                if (desc.contains(kw)) score += 8;
                // Partial token match (e.g. "mobil" matches "mobile")
                for (String titleWord : title.split("\\s+")) {
                    if (titleWord.startsWith(kw) || kw.startsWith(titleWord)) score += 5;
                }
            }

            // Status match bonus
            if (statusFilter != null && statusFilter.equals(p.getStatus())) {
                score += 10;
            }

            // Budget scoring: within budget = +20, slightly over (up to 50%) = +10, way over = +2
            String budgetTag = "";
            if (userBudget != null && p.getRequiredBudget() != null) {
                double projBudget = p.getRequiredBudget().doubleValue();
                if (projBudget <= userBudget) {
                    score += 20;
                    budgetTag = "Within budget";
                } else if (projBudget <= userBudget * 1.5) {
                    score += 10;
                    long overPercent = Math.round(((projBudget - userBudget) / userBudget) * 100);
                    budgetTag = overPercent + "% over budget";
                } else {
                    score += 2;
                    budgetTag = "Over budget";
                }
            }

            if (score > 0) {
                results.add(new ScoredProject(p, score, budgetTag));
            }
        }

        results.sort(Comparator.comparingDouble(ScoredProject::score).reversed());
        return results;
    }

    private void displayScoredResults(List<ScoredProject> scored, JsonObject filters) {
        resultsContainer.getChildren().clear();

        if (filters != null) {
            HBox filtersDisplay = new HBox(8);
            filtersDisplay.setAlignment(Pos.CENTER_LEFT);
            filtersDisplay.setPadding(new Insets(0, 0, 8, 0));
            Label filtersLabel = new Label("AI understood: ");
            filtersLabel.setStyle("-fx-text-fill: #A78BFA; -fx-font-weight: 800; -fx-font-size: 12px;");
            filtersDisplay.getChildren().add(filtersLabel);

            if (filters.has("intent") && !filters.get("intent").isJsonNull()) {
                Label intent = new Label(filters.get("intent").getAsString());
                intent.setStyle("-fx-background-color: rgba(139,92,246,0.15); -fx-text-fill: #A78BFA; " +
                        "-fx-background-radius: 8; -fx-padding: 3 8; -fx-font-size: 11px;");
                filtersDisplay.getChildren().add(intent);
            }
            if (filters.has("budget") && !filters.get("budget").isJsonNull()) {
                Label b = new Label("Budget: " + currencyFormat.format(filters.get("budget").getAsDouble()) + " TND");
                b.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10B981; " +
                        "-fx-background-radius: 8; -fx-padding: 3 8; -fx-font-size: 11px;");
                filtersDisplay.getChildren().add(b);
            }
            if (filters.has("keywords") && filters.get("keywords").isJsonArray()) {
                String kws = "";
                for (var el : filters.getAsJsonArray("keywords")) kws += el.getAsString() + " ";
                Label k = new Label("Keywords: " + kws.trim());
                k.setStyle("-fx-background-color: rgba(232,169,58,0.15); -fx-text-fill: #E8A93A; " +
                        "-fx-background-radius: 8; -fx-padding: 3 8; -fx-font-size: 11px;");
                filtersDisplay.getChildren().add(k);
            }
            resultsContainer.getChildren().add(filtersDisplay);
        }

        if (scored.isEmpty()) {
            Label empty = new Label("No projects found. Try a different query.");
            empty.setStyle("-fx-text-fill: #E8A93A; -fx-font-size: 14px; -fx-padding: 30;");
            resultsContainer.getChildren().add(empty);
            return;
        }

        int delay = 0;
        for (ScoredProject sp : scored) {
            VBox card = createResultCard(sp.project);

            // Budget tag badge
            if (!sp.budgetTag.isEmpty()) {
                boolean withinBudget = sp.budgetTag.equals("Within budget");
                Label tag = new Label(sp.budgetTag);
                tag.setStyle(withinBudget
                        ? "-fx-background-color: rgba(16,185,129,0.2); -fx-text-fill: #10B981; -fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: 800;"
                        : "-fx-background-color: rgba(251,191,36,0.2); -fx-text-fill: #FDB813; -fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: 800;");
                // Add tag to the first HBox (header) in the card
                if (!card.getChildren().isEmpty() && card.getChildren().get(0) instanceof HBox header) {
                    header.getChildren().add(tag);
                }
            }

            card.setOpacity(0);
            resultsContainer.getChildren().add(card);

            FadeTransition fade = new FadeTransition(Duration.millis(400), card);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(delay));
            fade.play();

            TranslateTransition slide = new TranslateTransition(Duration.millis(400), card);
            slide.setFromY(20);
            slide.setToY(0);
            slide.setDelay(Duration.millis(delay));
            slide.play();

            delay += 80;
        }
    }

    private VBox createResultCard(Project p) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: rgba(23,42,69,0.94); -fx-background-radius: 14; " +
                "-fx-border-color: rgba(232,169,58,0.25); -fx-border-radius: 14; -fx-border-width: 1; " +
                "-fx-padding: 16;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(p.getTitle());
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 15px; -fx-font-weight: 900;");

        Label statusBadge = new Label(p.getStatus());
        statusBadge.setStyle("-fx-background-color: rgba(232,169,58,0.15); -fx-text-fill: #E8A93A; " +
                "-fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label budget = new Label(currencyFormat.format(p.getRequiredBudget()) + " TND");
        budget.setStyle("-fx-text-fill: #FDB813; -fx-font-weight: 800; -fx-font-size: 14px;");

        header.getChildren().addAll(title, statusBadge, spacer, budget);

        Label desc = new Label(p.getDescription() != null ?
                (p.getDescription().length() > 150 ? p.getDescription().substring(0, 147) + "..." : p.getDescription())
                : "No description");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: rgba(232,169,58,0.7); -fx-font-size: 12px;");

        HBox stats = new HBox(16);
        stats.setAlignment(Pos.CENTER_LEFT);
        Label invested = new Label("Invested: " + currencyFormat.format(
                p.getTotalInvested() != null ? p.getTotalInvested() : BigDecimal.ZERO) + " TND");
        invested.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px; -fx-font-weight: 800;");
        Label count = new Label(p.getInvestmentsCount() + " investor(s)");
        count.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 11px;");
        stats.getChildren().addAll(invested, count);

        card.getChildren().addAll(header, desc, stats);
        return card;
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/ProjectsListView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
