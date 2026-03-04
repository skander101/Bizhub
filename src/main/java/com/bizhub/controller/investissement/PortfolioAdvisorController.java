package com.bizhub.controller.investissement;

import com.bizhub.model.services.investissement.AI.PortfolioAdvisorService;
import com.bizhub.model.services.investissement.AI.PortfolioAdvisorService.*;
import com.bizhub.model.services.common.service.AppSession;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.concurrent.Task;
import com.bizhub.controller.users_avis.user.SidebarController;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.*;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

public class PortfolioAdvisorController {

    @FXML private SidebarController sidebarController;
    @FXML private HBox scoreCardsRow;
    @FXML private PieChart sectorChart;
    @FXML private PieChart statusChart;
    @FXML private VBox holdingsContainer;
    @FXML private VBox adviceContainer;
    @FXML private ProgressIndicator aiSpinner;
    @FXML private VBox mainContent;

    private final PortfolioAdvisorService service = new PortfolioAdvisorService();
    private final NumberFormat currFmt = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("portfolio");
        loadPortfolioData();
    }

    @FXML
    private void handleRefresh() {
        loadPortfolioData();
    }

    private void loadPortfolioData() {
        int investorId = AppSession.getCurrentUser() != null ? AppSession.getCurrentUser().getUserId() : 0;

        scoreCardsRow.getChildren().clear();
        holdingsContainer.getChildren().clear();
        adviceContainer.getChildren().clear();
        sectorChart.getData().clear();
        statusChart.getData().clear();

        PortfolioSnapshot snapshot = service.loadPortfolio(investorId);

        // Score cards
        addScoreCard(scoreCardsRow, "Total Invested", currFmt.format(snapshot.totalInvested()) + " TND", "#3B82F6");
        addScoreCard(scoreCardsRow, "Projects", String.valueOf(snapshot.projectCount()), "#10B981");
        addScoreCard(scoreCardsRow, "Sectors", String.valueOf(snapshot.sectorAllocation().size()), "#8B5CF6");
        addScoreCard(scoreCardsRow, "Diversification", String.format("%.0f/100", snapshot.diversificationScore()), diversColor(snapshot.diversificationScore()));

        // Sector chart
        for (Map.Entry<String, Double> entry : snapshot.sectorAllocation().entrySet()) {
            sectorChart.getData().add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + "%)", entry.getValue()));
        }
        colorPieChart(sectorChart, new String[]{"#06B6D4", "#3B82F6", "#8B5CF6", "#10B981", "#F59E0B", "#EF4444", "#EC4899"});

        // Status chart
        for (Map.Entry<String, Double> entry : snapshot.statusBreakdown().entrySet()) {
            statusChart.getData().add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + "%)", entry.getValue()));
        }
        colorPieChart(statusChart, new String[]{"#10B981", "#3B82F6", "#F59E0B", "#EF4444", "#8B5CF6"});

        // Holdings table
        if (snapshot.holdings().isEmpty()) {
            Label empty = new Label("No investments yet. Start investing to see your portfolio here.");
            empty.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 13px;");
            holdingsContainer.getChildren().add(empty);
        } else {
            // Header
            HBox header = holdingRow("Project", "Sector", "Amount", "Budget", "Status", true);
            holdingsContainer.getChildren().add(header);
            for (PortfolioHolding h : snapshot.holdings()) {
                HBox row = holdingRow(
                        h.projectTitle(), h.sector(),
                        currFmt.format(h.investedAmount()) + " TND",
                        currFmt.format(h.requiredBudget()) + " TND",
                        h.status(), false
                );
                holdingsContainer.getChildren().add(row);
            }
        }

        // AI Advice
        aiSpinner.setVisible(true);
        Label loadingLabel = new Label("AI is analyzing your portfolio...");
        loadingLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 12px;");
        adviceContainer.getChildren().add(loadingLabel);

        Task<PortfolioAdvice> task = service.analyzeAsync(investorId);
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            aiSpinner.setVisible(false);
            adviceContainer.getChildren().clear();
            renderAdvice(task.getValue());
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            aiSpinner.setVisible(false);
            adviceContainer.getChildren().clear();
            Label err = new Label("AI analysis failed: " + task.getException().getMessage());
            err.setWrapText(true);
            err.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 13px;");
            adviceContainer.getChildren().add(err);
        }));
        new Thread(task).start();
    }

    private void renderAdvice(PortfolioAdvice advice) {
        JsonObject r = advice.parsed();

        // Health badge
        if (r.has("portfolioHealth")) {
            String health = r.get("portfolioHealth").getAsString();
            String hColor = switch (health) {
                case "Excellent" -> "#10B981";
                case "Good" -> "#3B82F6";
                case "Fair" -> "#F59E0B";
                default -> "#EF4444";
            };
            Label badge = new Label("Portfolio Health: " + health);
            badge.setStyle("-fx-background-color: " + hColor + "22; -fx-text-fill: " + hColor + "; " +
                    "-fx-font-size: 14px; -fx-font-weight: 900; -fx-background-radius: 999; -fx-padding: 6 18;");
            adviceContainer.getChildren().add(badge);
        }

        // Summary
        if (r.has("summary")) {
            Label sum = new Label(r.get("summary").getAsString());
            sum.setWrapText(true);
            sum.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-line-spacing: 2;");
            adviceContainer.getChildren().add(sum);
        }

        // Risk Assessment
        if (r.has("riskAssessment") && r.get("riskAssessment").isJsonObject()) {
            JsonObject risk = r.getAsJsonObject("riskAssessment");
            VBox riskBox = adviceSection("Risk Assessment", "#EF4444");
            int overallRisk = risk.has("overallRisk") ? risk.get("overallRisk").getAsInt() : 0;
            String rColor = overallRisk <= 3 ? "#10B981" : overallRisk <= 6 ? "#F59E0B" : "#EF4444";

            HBox riskRow = new HBox(14);
            riskRow.setAlignment(Pos.CENTER_LEFT);
            riskRow.getChildren().add(riskBadge("Overall", overallRisk + "/10", rColor));
            if (risk.has("concentrationRisk"))
                riskRow.getChildren().add(riskBadge("Concentration", risk.get("concentrationRisk").getAsString(), "#F59E0B"));
            if (risk.has("stageRisk"))
                riskRow.getChildren().add(riskBadge("Stage", risk.get("stageRisk").getAsString(), "#8B5CF6"));
            riskBox.getChildren().add(riskRow);

            if (risk.has("comment")) {
                Label c = new Label(risk.get("comment").getAsString());
                c.setWrapText(true);
                c.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px;");
                riskBox.getChildren().add(c);
            }
            adviceContainer.getChildren().add(riskBox);
        }

        // Sector Analysis
        if (r.has("sectorAnalysis") && r.get("sectorAnalysis").isJsonArray()) {
            VBox secBox = adviceSection("Sector Analysis", "#06B6D4");
            for (JsonElement el : r.getAsJsonArray("sectorAnalysis")) {
                if (!el.isJsonObject()) continue;
                JsonObject s = el.getAsJsonObject();
                String sector = s.has("sector") ? s.get("sector").getAsString() : "";
                String action = s.has("action") ? s.get("action").getAsString() : "";
                String comment = s.has("comment") ? s.get("comment").getAsString() : "";
                String aColor = switch (action.toLowerCase()) {
                    case "increase" -> "#10B981";
                    case "reduce" -> "#EF4444";
                    default -> "#F59E0B";
                };
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                Label sLabel = new Label(sector);
                sLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 12px;");
                Label aLabel = new Label(action.toUpperCase());
                aLabel.setStyle("-fx-background-color: " + aColor + "22; -fx-text-fill: " + aColor + "; " +
                        "-fx-font-size: 10px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 2 8;");
                Label cLabel = new Label(comment);
                cLabel.setWrapText(true);
                cLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 11px;");
                HBox.setHgrow(cLabel, Priority.ALWAYS);
                row.getChildren().addAll(sLabel, aLabel, cLabel);
                secBox.getChildren().add(row);
            }
            adviceContainer.getChildren().add(secBox);
        }

        // Rebalance Suggestions
        if (r.has("rebalanceSuggestions") && r.get("rebalanceSuggestions").isJsonArray()) {
            VBox rebBox = adviceSection("Rebalancing Suggestions", "#8B5CF6");
            for (JsonElement el : r.getAsJsonArray("rebalanceSuggestions")) {
                if (!el.isJsonObject()) continue;
                JsonObject s = el.getAsJsonObject();
                String action = s.has("action") ? s.get("action").getAsString() : "";
                String target = s.has("target") ? s.get("target").getAsString() : "";
                String reason = s.has("reason") ? s.get("reason").getAsString() : "";
                Label l = new Label("  " + action + " " + target + " — " + reason);
                l.setWrapText(true);
                l.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 12px;");
                rebBox.getChildren().add(l);
            }
            adviceContainer.getChildren().add(rebBox);
        }

        // Next Steps
        if (r.has("nextSteps") && r.get("nextSteps").isJsonArray()) {
            VBox nsBox = adviceSection("Recommended Next Steps", "#10B981");
            int idx = 1;
            for (JsonElement el : r.getAsJsonArray("nextSteps")) {
                Label l = new Label("  " + idx + ". " + el.getAsString());
                l.setWrapText(true);
                l.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 12px;");
                nsBox.getChildren().add(l);
                idx++;
            }
            adviceContainer.getChildren().add(nsBox);
        }
    }

    // --- UI Helpers ---

    private void addScoreCard(HBox parent, String label, String value, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: rgba(26,51,82,0.96); -fx-background-radius: 16; " +
                "-fx-border-color: " + color + "33; -fx-border-radius: 16; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 3);");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label valLabel = new Label(value);
        valLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 22px; -fx-font-weight: 900;");
        Label nameLabel = new Label(label.toUpperCase());
        nameLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 10px; -fx-font-weight: 700;");
        card.getChildren().addAll(valLabel, nameLabel);
        parent.getChildren().add(card);
    }

    private String diversColor(double score) {
        if (score >= 70) return "#10B981";
        if (score >= 40) return "#F59E0B";
        return "#EF4444";
    }

    private HBox holdingRow(String project, String sector, String amount, String budget, String status, boolean isHeader) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        String bg = isHeader ? "rgba(6,182,212,0.08)" : "rgba(26,51,82,0.5)";
        row.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10; -fx-padding: 8 14;");
        String style = isHeader
                ? "-fx-text-fill: #06B6D4; -fx-font-size: 11px; -fx-font-weight: 900;"
                : "-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11px;";

        Label pLabel = new Label(project); pLabel.setStyle(style); pLabel.setPrefWidth(180);
        Label sLabel = new Label(sector); sLabel.setStyle(style); sLabel.setPrefWidth(100);
        Label aLabel = new Label(amount); aLabel.setStyle(style); aLabel.setPrefWidth(120);
        Label bLabel = new Label(budget); bLabel.setStyle(style); bLabel.setPrefWidth(120);

        String statusColor = switch (status.toLowerCase()) {
            case "funded" -> "#10B981";
            case "in_progress" -> "#3B82F6";
            case "complete" -> "#8B5CF6";
            default -> "#F59E0B";
        };
        Label stLabel = new Label(status);
        stLabel.setStyle(isHeader ? style : "-fx-text-fill: " + statusColor + "; -fx-font-size: 11px; -fx-font-weight: 700;");

        row.getChildren().addAll(pLabel, sLabel, aLabel, bLabel, stLabel);
        return row;
    }

    private VBox adviceSection(String title, String color) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: rgba(10,25,47,0.6); -fx-background-radius: 14; " +
                "-fx-border-color: " + color + "22; -fx-border-radius: 14; -fx-border-width: 1;");
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-weight: 900;");
        box.getChildren().add(t);
        return box;
    }

    private VBox riskBadge(String label, String value, String color) {
        VBox badge = new VBox(2);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(8, 14, 8, 14));
        badge.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 10;");
        Label vLabel = new Label(value);
        vLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px; -fx-font-weight: 900;");
        Label nLabel = new Label(label);
        nLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 10px;");
        badge.getChildren().addAll(vLabel, nLabel);
        return badge;
    }

    private void colorPieChart(PieChart chart, String[] colors) {
        int i = 0;
        for (PieChart.Data d : chart.getData()) {
            String c = colors[i % colors.length];
            d.getNode().setStyle("-fx-pie-color: " + c + ";");
            i++;
        }
    }
}
