package com.bizhub.controller.investissement;

import com.bizhub.model.investissement.Project;
import com.bizhub.model.services.investissement.AI.AIIntegrationService;
import com.bizhub.model.services.investissement.AI.RecommendationEngine;
import com.bizhub.model.services.investissement.ProjectServiceImpl;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.users_avis.user.User;
import javafx.application.Platform;
import com.bizhub.controller.users_avis.user.SidebarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javafx.scene.text.Text;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProjectsListController {

    @FXML private SidebarController sidebarController;
    @FXML private VBox projectsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label summaryLabel;
    @FXML private Label emptyLabel;
    @FXML private ScrollPane scrollPane; // AJOUTÉ

    private ProjectServiceImpl projectService;
    private AIIntegrationService aiService;
    private List<Project> allProjects;
    private NumberFormat currencyFormat;
    private DateTimeFormatter dateFormatter;

    private Map<Integer, Double> aiScoreByProjectId = new HashMap<>();

    private int getCurrentUserId() {
        User me = AppSession.getCurrentUser();
        return (me != null) ? me.getUserId() : 0;
    }

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("projects");
        projectService = new ProjectServiceImpl();
        aiService = new AIIntegrationService();
        currencyFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);
        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Configuration du ScrollPane
        Platform.runLater(() -> {
            if (scrollPane != null) {
                scrollPane.setFitToWidth(true);

                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
            }
        });

        // Initialiser les filtres
        statusFilterComboBox.getItems().addAll(
                "Tous les statuts",
                "pending",
                "funded",
                "in_progress",
                "complete"
        );
        statusFilterComboBox.setValue("Tous les statuts");

        // Listeners pour les filtres
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterProjects());
        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> filterProjects());

        loadProjects();
    }

    @FXML
    public void handleAIRecommendations() {
        if (allProjects == null || allProjects.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Recommandations IA", "Aucun projet à analyser.");
            return;
        }

        try {
            List<RecommendationEngine.ProjectRecommendation> recs = aiService.getSmartRecommendations(getCurrentUserId());
            aiScoreByProjectId.clear();
            for (RecommendationEngine.ProjectRecommendation rec : recs) {
                if (rec.getProject() != null) {
                    aiScoreByProjectId.put(rec.getProject().getProjectId(), rec.getScore());
                }
            }

            List<Project> sorted = allProjects.stream()
                    .sorted((a, b) -> Double.compare(
                            aiScoreByProjectId.getOrDefault(b.getProjectId(), -1.0),
                            aiScoreByProjectId.getOrDefault(a.getProjectId(), -1.0)
                    ))
                    .collect(Collectors.toList());

            displayProjects(sorted);

            if (aiScoreByProjectId.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Recommandations IA",
                        "Aucune recommandation disponible pour cet utilisateur.");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Recommandations IA",
                        "Les projets ont été triés selon les recommandations IA.");
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur recommandations IA: " + e.getMessage());
        }
    }

    private void loadProjects() {
        try {
            allProjects = projectService.getAllWithStats();
            
            // Note: Sample project creation disabled to avoid foreign key constraints
            // Users can test AI recommendations with the demo fallback system
            
            displayProjects(allProjects);
            updateSummary();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors du chargement des projets: " + e.getMessage());
        }
    }

    private void displayProjects(List<Project> projects) {
        projectsContainer.getChildren().clear();

        if (projects.isEmpty()) {
            emptyLabel.setVisible(true);
            return;
        }

        emptyLabel.setVisible(false);

        for (Project project : projects) {
            VBox card = createProjectCard(project);
            projectsContainer.getChildren().add(card);
        }

        // Forcer le rafraîchissement du ScrollPane
        Platform.runLater(() -> {
            if (scrollPane != null) {
                scrollPane.requestLayout();
                scrollPane.layout();
            }
        });
    }

    private VBox createProjectCard(Project project) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: rgba(23,42,69,0.94); -fx-background-radius: 18; " +
                "-fx-border-color: rgba(232,169,58,0.25); -fx-border-radius: 18; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0.15, 0, 10);");

        Double aiScore = aiScoreByProjectId.get(project.getProjectId());
        if (aiScore != null) {
            card.setStyle(card.getStyle().replace("rgba(232,169,58,0.25)", "#8B5CF6"));
        }

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(48, 48);
        iconBox.setMinSize(48, 48);
        iconBox.setStyle("-fx-background-color: " + getStatusColor(project.getStatus()) + "22; -fx-background-radius: 12;");
        if (project.getLogoUrl() != null && !project.getLogoUrl().isBlank()) {
            try {
                javafx.scene.image.ImageView logo = new javafx.scene.image.ImageView(
                        new javafx.scene.image.Image(project.getLogoUrl(), 40, 40, true, true, true));
                logo.setFitWidth(40);
                logo.setFitHeight(40);
                iconBox.getChildren().add(logo);
            } catch (Exception ex) {
                Label iconLabel = new Label(getStatusIcon(project.getStatus()));
                iconLabel.setStyle("-fx-font-size: 22px;");
                iconBox.getChildren().add(iconLabel);
            }
        } else {
            Label iconLabel = new Label(getStatusIcon(project.getStatus()));
            iconLabel.setStyle("-fx-font-size: 22px;");
            iconBox.getChildren().add(iconLabel);
        }

        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label titleLabel = new Label(project.getTitle());
        titleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16px; -fx-font-weight: 900;");
        titleLabel.setWrapText(true);
        HBox meta = new HBox(10);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label idLabel = new Label("#" + project.getProjectId());
        idLabel.setStyle("-fx-text-fill: rgba(232,169,58,0.6); -fx-font-size: 11px;");
        Label dateLabel = new Label(project.getCreatedAt() != null ? project.getCreatedAt().format(dateFormatter) : "");
        dateLabel.setStyle("-fx-text-fill: rgba(232,169,58,0.6); -fx-font-size: 11px;");
        meta.getChildren().addAll(idLabel, dateLabel);
        titleBox.getChildren().addAll(titleLabel, meta);

        VBox badgeBox = new VBox(4);
        badgeBox.setAlignment(Pos.CENTER_RIGHT);
        Label statusBadge = new Label(project.getStatus().toUpperCase());
        statusBadge.setStyle(getStatusBadgeDarkStyle(project.getStatus()));
        badgeBox.getChildren().add(statusBadge);
        if (aiScore != null) {
            Label aiBadge = new Label(String.format("AI: %.0f%%", aiScore * 100));
            aiBadge.setStyle("-fx-background-color: rgba(139,92,246,0.20); -fx-text-fill: #A78BFA; " +
                    "-fx-background-radius: 999; -fx-padding: 3 10; -fx-font-size: 10px; -fx-font-weight: 800;");
            badgeBox.getChildren().add(aiBadge);
        }

        topRow.getChildren().addAll(iconBox, titleBox, badgeBox);

        if (project.getDescription() != null && !project.getDescription().isEmpty()) {
            Label desc = new Label(project.getDescription().length() > 120
                    ? project.getDescription().substring(0, 117) + "..." : project.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-text-fill: rgba(232,169,58,0.65); -fx-font-size: 12px;");
            content.getChildren().addAll(topRow, desc);
        } else {
            content.getChildren().add(topRow);
        }

        BigDecimal totalInvested = project.getTotalInvested() != null ? project.getTotalInvested() : BigDecimal.ZERO;
        BigDecimal requiredBudget = project.getRequiredBudget();
        double progressPercent = requiredBudget.compareTo(BigDecimal.ZERO) > 0
                ? totalInvested.divide(requiredBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0;

        HBox financeRow = new HBox(16);
        financeRow.setAlignment(Pos.CENTER_LEFT);
        VBox budgetCol = new VBox(1);
        budgetCol.getChildren().addAll(
                styledMiniLabel("Budget", "rgba(232,169,58,0.5)"),
                styledValueLabel(currencyFormat.format(requiredBudget) + " TND", "#E8A93A"));
        VBox investedCol = new VBox(1);
        investedCol.getChildren().addAll(
                styledMiniLabel("Investi", "rgba(232,169,58,0.5)"),
                styledValueLabel(currencyFormat.format(totalInvested) + " TND", "#10B981"));
        VBox countCol = new VBox(1);
        countCol.getChildren().addAll(
                styledMiniLabel("Investors", "rgba(232,169,58,0.5)"),
                styledValueLabel(String.valueOf(project.getInvestmentsCount()), "#FDB813"));
        VBox pctCol = new VBox(1);
        pctCol.getChildren().addAll(
                styledMiniLabel("Progress", "rgba(232,169,58,0.5)"),
                styledValueLabel(String.format("%.1f%%", progressPercent), getProgressColor(progressPercent)));
        financeRow.getChildren().addAll(budgetCol, investedCol, countCol, pctCol);
        content.getChildren().add(financeRow);

        ProgressBar progressBar = new ProgressBar(progressPercent / 100);
        progressBar.setPrefHeight(6);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().add(progressBar);

        Region divider = new Region();
        divider.setStyle("-fx-background-color: rgba(232,169,58,0.12); -fx-pref-height: 1; -fx-max-height: 1;");

        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_LEFT);
        actionsRow.setPadding(new Insets(12, 20, 14, 20));
        actionsRow.setStyle("-fx-background-color: rgba(10,25,47,0.40); -fx-background-radius: 0 0 18 18;");

        Button investBtn = actionBtn("Invest", "#10B981", e -> handleAddInvestment(project));
        Button viewBtn = actionBtn("View", "#3B82F6", e -> handleViewProject(project));
        Button negotiateBtn = actionBtn("Negotiate", "#8B5CF6", e -> handleStartNegotiation(project));
        Button aiBtn = actionBtn("AI Analyze", "#6366F1", e -> handleAIAnalyze(project));
        Button ddBtn = actionBtn("Due Diligence", "#06B6D4", e -> handleDueDiligence(project));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = actionBtn("Edit", "#F59E0B", e -> handleEditProject(project));
        Button deleteBtn = actionBtn("Delete", "#EF4444", e -> handleDeleteProject(project));

        actionsRow.getChildren().addAll(investBtn, viewBtn, negotiateBtn, aiBtn, ddBtn, spacer, editBtn, deleteBtn);

        card.getChildren().addAll(content, divider, actionsRow);
        return card;
    }

    private Button actionBtn(String text, String color, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setStyle(String.format(
                "-fx-background-color: %s22; -fx-text-fill: %s; -fx-font-weight: 800; " +
                "-fx-background-radius: 10; -fx-padding: 6 14; -fx-font-size: 11px; -fx-cursor: hand; " +
                "-fx-border-color: %s44; -fx-border-radius: 10; -fx-border-width: 1;",
                color, color, color));
        btn.setOnAction(handler);
        return btn;
    }

    private Label styledMiniLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10px; -fx-font-weight: 800;");
        return l;
    }

    private Label styledValueLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-weight: 900;");
        return l;
    }

    private String getStatusBadgeDarkStyle(String status) {
        return switch (status) {
            case "pending" -> "-fx-background-color: rgba(245,158,11,0.15); -fx-text-fill: #F59E0B; " +
                    "-fx-background-radius: 999; -fx-padding: 3 12; -fx-font-size: 10px; -fx-font-weight: 800;";
            case "funded" -> "-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10B981; " +
                    "-fx-background-radius: 999; -fx-padding: 3 12; -fx-font-size: 10px; -fx-font-weight: 800;";
            case "in_progress" -> "-fx-background-color: rgba(59,130,246,0.15); -fx-text-fill: #3B82F6; " +
                    "-fx-background-radius: 999; -fx-padding: 3 12; -fx-font-size: 10px; -fx-font-weight: 800;";
            case "complete" -> "-fx-background-color: rgba(16,185,129,0.25); -fx-text-fill: #10B981; " +
                    "-fx-background-radius: 999; -fx-padding: 3 12; -fx-font-size: 10px; -fx-font-weight: 800;";
            default -> "-fx-background-color: rgba(232,169,58,0.15); -fx-text-fill: #E8A93A; " +
                    "-fx-background-radius: 999; -fx-padding: 3 12; -fx-font-size: 10px; -fx-font-weight: 800;";
        };
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) projectsContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleBack() {
        navigateTo("/com/bizhub/fxml/admin-dashboard.fxml", "Tableau de Bord");
    }

    @FXML
    public void handleNewProject() {
        navigateTo("/com/bizhub/fxml/AddProjectView.fxml", "Nouveau Projet");
    }

    @FXML
    public void handleRefresh() {
        loadProjects();
    }

    @FXML
    public void handleResetFilters() {
        searchField.clear();
        statusFilterComboBox.setValue("Tous les statuts");
    }

    private void handleAddInvestment(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/AddInvestistmentView.fxml"));

            if (loader.getLocation() == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Fichier AddInvestistmentView.fxml introuvable!");
                return;
            }

            Parent root = loader.load();
            AddInvestmentController controller = loader.getController();
            controller.setPreSelectedProject(project.getProjectId(), project.getTitle());

            Stage stage = (Stage) projectsContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Nouvel Investissement - " + project.getTitle());

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de navigation: " + e.getMessage());
        }
    }

    private void handleViewProject(Project project) {
        Stage detailStage = new Stage();
        detailStage.initOwner((Stage) projectsContainer.getScene().getWindow());
        detailStage.setTitle(project.getTitle());

        BigDecimal totalInvested = project.getTotalInvested() != null ? project.getTotalInvested() : BigDecimal.ZERO;
        BigDecimal requiredBudget = project.getRequiredBudget();
        double progressPercent = requiredBudget.compareTo(BigDecimal.ZERO) > 0
                ? totalInvested.divide(requiredBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0;
        String statusColor = getStatusColor(project.getStatus());

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0A192F;");

        HBox headerBar = new HBox(14);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(24, 28, 18, 28));
        headerBar.setStyle("-fx-background-color: rgba(26,51,82,0.7);");

        VBox iconWrap = new VBox();
        iconWrap.setAlignment(Pos.CENTER);
        iconWrap.setPrefSize(52, 52);
        iconWrap.setMinSize(52, 52);
        iconWrap.setStyle("-fx-background-color: " + statusColor + "22; -fx-background-radius: 14;");
        Label ico = new Label(getStatusIcon(project.getStatus()));
        ico.setStyle("-fx-font-size: 26px;");
        iconWrap.getChildren().add(ico);

        VBox headerText = new VBox(3);
        HBox.setHgrow(headerText, Priority.ALWAYS);
        Label titleLbl = new Label(project.getTitle());
        titleLbl.setWrapText(true);
        titleLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 20px; -fx-font-weight: 900;");
        Label idDate = new Label("#" + project.getProjectId() + "  •  Created " +
                (project.getCreatedAt() != null ? project.getCreatedAt().format(dateFormatter) : "N/A"));
        idDate.setStyle("-fx-text-fill: rgba(255,184,77,0.6); -fx-font-size: 12px;");
        headerText.getChildren().addAll(titleLbl, idDate);

        Label statusBadge = new Label(project.getStatus().toUpperCase());
        statusBadge.setStyle(getStatusBadgeDarkStyle(project.getStatus()) + " -fx-font-size: 12px; -fx-padding: 5 16;");

        headerBar.getChildren().addAll(iconWrap, headerText, statusBadge);

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);

        VBox body = new VBox(18);
        body.setPadding(new Insets(22, 28, 22, 28));
        sp.setContent(body);

        HBox statsRow = new HBox(14);
        statsRow.getChildren().addAll(
                statCard("Budget", currencyFormat.format(requiredBudget) + " TND", "#FFB84D"),
                statCard("Invested", currencyFormat.format(totalInvested) + " TND", "#10B981"),
                statCard("Investors", String.valueOf(project.getInvestmentsCount()), "#3B82F6"),
                statCard("Progress", String.format("%.1f%%", progressPercent), getProgressColor(progressPercent))
        );
        body.getChildren().add(statsRow);

        ProgressBar pb = new ProgressBar(progressPercent / 100.0);
        pb.setPrefHeight(8);
        pb.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().add(pb);

        if (project.getDescription() != null && !project.getDescription().isBlank()) {
            VBox descSection = new VBox(8);
            descSection.setPadding(new Insets(16));
            descSection.setStyle("-fx-background-color: rgba(26,51,82,0.96); -fx-background-radius: 16; " +
                    "-fx-border-color: rgba(255,184,77,0.15); -fx-border-radius: 16; -fx-border-width: 1;");
            Label descTitle = new Label("Description");
            descTitle.setStyle("-fx-text-fill: #FFB84D; -fx-font-size: 14px; -fx-font-weight: 900;");
            Label descBody = new Label(project.getDescription());
            descBody.setWrapText(true);
            descBody.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-line-spacing: 3;");
            descSection.getChildren().addAll(descTitle, descBody);
            body.getChildren().add(descSection);
        }

        VBox infoSection = new VBox(10);
        infoSection.setPadding(new Insets(16));
        infoSection.setStyle("-fx-background-color: rgba(26,51,82,0.96); -fx-background-radius: 16; " +
                "-fx-border-color: rgba(255,184,77,0.15); -fx-border-radius: 16; -fx-border-width: 1;");
        Label infoTitle = new Label("Project Details");
        infoTitle.setStyle("-fx-text-fill: #FFB84D; -fx-font-size: 14px; -fx-font-weight: 900;");
        infoSection.getChildren().add(infoTitle);
        infoSection.getChildren().add(detailRow("Project ID", String.valueOf(project.getProjectId())));
        infoSection.getChildren().add(detailRow("Startup ID", String.valueOf(project.getStartupId())));
        infoSection.getChildren().add(detailRow("Status", project.getStatus()));
        infoSection.getChildren().add(detailRow("Required Budget", currencyFormat.format(requiredBudget) + " TND"));
        infoSection.getChildren().add(detailRow("Total Invested", currencyFormat.format(totalInvested) + " TND"));
        BigDecimal remaining = requiredBudget.subtract(totalInvested).max(BigDecimal.ZERO);
        infoSection.getChildren().add(detailRow("Remaining", currencyFormat.format(remaining) + " TND"));
        infoSection.getChildren().add(detailRow("Investors Count", String.valueOf(project.getInvestmentsCount())));
        infoSection.getChildren().add(detailRow("Created At",
                project.getCreatedAt() != null ? project.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A"));
        body.getChildren().add(infoSection);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(14, 28, 20, 28));
        btnRow.setStyle("-fx-background-color: rgba(10,25,47,0.5);");

        Button investBtn = new Button("Invest in this Project");
        investBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: 800; " +
                "-fx-background-radius: 12; -fx-padding: 10 24; -fx-cursor: hand; -fx-font-size: 13px;");
        investBtn.setOnAction(ev -> { detailStage.close(); handleAddInvestment(project); });

        Button negotiateBtn = new Button("Start Negotiation");
        negotiateBtn.setStyle("-fx-background-color: #8B5CF6; -fx-text-fill: white; -fx-font-weight: 800; " +
                "-fx-background-radius: 12; -fx-padding: 10 24; -fx-cursor: hand; -fx-font-size: 13px;");
        negotiateBtn.setOnAction(ev -> { detailStage.close(); handleStartNegotiation(project); });

        Button enrichBtn = new Button("AI Enrich");
        enrichBtn.setStyle("-fx-background-color: #06B6D4; -fx-text-fill: white; -fx-font-weight: 800; " +
                "-fx-background-radius: 12; -fx-padding: 10 24; -fx-cursor: hand; -fx-font-size: 13px;");
        enrichBtn.setOnAction(ev -> {
            enrichBtn.setDisable(true);
            enrichBtn.setText("Enriching...");
            com.bizhub.model.services.investissement.AI.StartupEnrichmentService svc =
                    new com.bizhub.model.services.investissement.AI.StartupEnrichmentService();
            javafx.concurrent.Task<com.bizhub.model.services.investissement.AI.StartupEnrichmentService.EnrichmentResult> enrichTask =
                    svc.enrichAsync(project.getTitle(), project.getDescription(), project.getWebsiteUrl());
            enrichTask.setOnSucceeded(eev -> Platform.runLater(() -> {
                var result = enrichTask.getValue();
                if (result.logoUrl() != null) {
                    project.setLogoUrl(result.logoUrl());
                    svc.saveEnrichment(project.getProjectId(), result.logoUrl(), result.websiteUrl());
                }
                enrichBtn.setText("Enriched");
                VBox enrichSection = new VBox(8);
                enrichSection.setPadding(new Insets(16));
                enrichSection.setStyle("-fx-background-color: rgba(26,51,82,0.96); -fx-background-radius: 16; " +
                        "-fx-border-color: rgba(6,182,212,0.18); -fx-border-radius: 16; -fx-border-width: 1;");
                Label eTitle = new Label("AI Enrichment");
                eTitle.setStyle("-fx-text-fill: #06B6D4; -fx-font-size: 14px; -fx-font-weight: 900;");
                enrichSection.getChildren().add(eTitle);
                if (result.suggestedSector() != null && !result.suggestedSector().isBlank())
                    enrichSection.getChildren().add(detailRow("Sector", result.suggestedSector()));
                if (result.estimatedEmployees() != null && !result.estimatedEmployees().isBlank())
                    enrichSection.getChildren().add(detailRow("Est. Employees", result.estimatedEmployees()));
                if (result.marketSize() != null && !result.marketSize().isBlank())
                    enrichSection.getChildren().add(detailRow("Market Size (TAM)", result.marketSize()));
                if (result.competitors() != null && result.competitors().size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    result.competitors().forEach(c -> {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(c.getAsString());
                    });
                    enrichSection.getChildren().add(detailRow("Competitors", sb.toString()));
                }
                if (result.companyInsight() != null && !result.companyInsight().isBlank()) {
                    Label insight = new Label(result.companyInsight());
                    insight.setWrapText(true);
                    insight.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px; -fx-line-spacing: 2;");
                    enrichSection.getChildren().add(insight);
                }
                body.getChildren().add(enrichSection);
            }));
            enrichTask.setOnFailed(eev -> Platform.runLater(() -> {
                enrichBtn.setText("Enrich Failed");
                enrichBtn.setDisable(false);
            }));
            new Thread(enrichTask).start();
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: rgba(255,184,77,0.15); -fx-text-fill: #FFB84D; -fx-font-weight: 800; " +
                "-fx-background-radius: 12; -fx-padding: 10 24; -fx-cursor: hand; -fx-font-size: 13px; " +
                "-fx-border-color: rgba(255,184,77,0.3); -fx-border-radius: 12; -fx-border-width: 1;");
        closeBtn.setOnAction(ev -> detailStage.close());

        btnRow.getChildren().addAll(investBtn, negotiateBtn, enrichBtn, closeBtn);

        root.getChildren().addAll(headerBar, sp, btnRow);

        Scene scene = new Scene(root, 620, 640);
        detailStage.setScene(scene);
        detailStage.show();
    }

    private VBox statCard(String label, String value, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 10, 14, 10));
        card.setStyle("-fx-background-color: rgba(26,51,82,0.96); -fx-background-radius: 14; " +
                "-fx-border-color: " + color + "22; -fx-border-radius: 14; -fx-border-width: 1;");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px; -fx-font-weight: 900;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: rgba(255,184,77,0.55); -fx-font-size: 11px; -fx-font-weight: 700;");
        card.getChildren().addAll(val, lbl);
        return card;
    }

    private HBox detailRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setMinWidth(130);
        lbl.setStyle("-fx-text-fill: rgba(255,184,77,0.6); -fx-font-size: 12px; -fx-font-weight: 700;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: 600;");
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private void handleEditProject(Project project) {
        Dialog<Project> dialog = new Dialog<>();
        dialog.setTitle("Modifier le projet");
        dialog.setHeaderText("Modifier les informations du projet");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField(project.getTitle());
        titleField.setPrefWidth(300);

        TextArea descArea = new TextArea(project.getDescription());
        descArea.setPrefRowCount(3);
        descArea.setPrefWidth(300);

        TextField budgetField = new TextField(project.getRequiredBudget().toString());
        budgetField.setPrefWidth(300);

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("pending", "funded", "in_progress", "complete");
        statusBox.setValue(project.getStatus());
        statusBox.setPrefWidth(300);

        grid.add(new Label("Titre:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descArea, 1, 1);
        grid.add(new Label("Budget:"), 0, 2);
        grid.add(budgetField, 1, 2);
        grid.add(new Label("Statut:"), 0, 3);
        grid.add(statusBox, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    project.setTitle(titleField.getText());
                    project.setDescription(descArea.getText());
                    project.setRequiredBudget(new BigDecimal(budgetField.getText()));
                    project.setStatus(statusBox.getValue());
                    return project;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Budget invalide");
                    return null;
                }
            }
            return null;
        });

        Optional<Project> result = dialog.showAndWait();
        result.ifPresent(updatedProject -> {
            try {
                boolean updated = projectService.update(updatedProject);
                if (updated) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Projet modifié!");
                    loadProjects();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec de la modification");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur: " + e.getMessage());
            }
        });
    }

    private void handleDeleteProject(Project project) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer le projet ?");
        confirmAlert.setContentText("Projet: " + project.getTitle() +
                "\n\n⚠️ Tous les investissements associés seront également supprimés!");

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = projectService.delete(project.getProjectId());

                if (deleted) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Projet supprimé!");
                    loadProjects();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec de la suppression");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur: " + e.getMessage());
            }
        }
    }

    private void handleStartNegotiation(Project project) {
        TextInputDialog dialog = new TextInputDialog(project.getRequiredBudget().toPlainString());
        dialog.setTitle("Start Negotiation");
        dialog.setHeaderText("Negotiate for: " + project.getTitle());
        dialog.setContentText("Your initial offer (TND):");

        dialog.showAndWait().ifPresent(offerStr -> {
            try {
                BigDecimal offer = new BigDecimal(offerStr);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/NegotiationView.fxml"));
                Parent root = loader.load();
                NegotiationController ctrl = loader.getController();
                ctrl.initNewNegotiation(project.getProjectId(), getCurrentUserId(),
                        project.getStartupId(), project.getTitle(), offer);

                Stage stage = (Stage) projectsContainer.getScene().getWindow();
                stage.getScene().setRoot(root);
                stage.setTitle("Negotiation - " + project.getTitle());
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid amount");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Navigation error: " + e.getMessage());
            }
        });
    }

    private void handleAIAnalyze(Project project) {
        Stage owner = (Stage) projectsContainer.getScene().getWindow();

        Stage analyzeStage = new Stage();
        analyzeStage.initOwner(owner);
        analyzeStage.setTitle("AI Analysis - " + project.getTitle());

        VBox root = new VBox(16);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #0A192F;");

        Label header = new Label("AI Pitch Analysis");
        header.setStyle("-fx-text-fill: #FFB84D; -fx-font-size: 22px; -fx-font-weight: 900;");

        Label projectName = new Label(project.getTitle());
        projectName.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16px; -fx-font-weight: 800;");

        Label budgetLine = new Label("Budget: " + currencyFormat.format(project.getRequiredBudget()) + " TND");
        budgetLine.setStyle("-fx-text-fill: #FFB84D; -fx-font-size: 13px;");

        Region sep = new Region();
        sep.setStyle("-fx-background-color: rgba(255,184,77,0.2); -fx-pref-height: 1; -fx-max-height: 1;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(40, 40);
        Label loadingLabel = new Label("AI is analyzing this project...");
        loadingLabel.setStyle("-fx-text-fill: #FFB84D; -fx-font-size: 14px;");
        VBox loadingBox = new VBox(12, spinner, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(30));

        ScrollPane scrollPane2 = new ScrollPane();
        scrollPane2.setFitToWidth(true);
        scrollPane2.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane2.setVisible(false);
        VBox.setVgrow(scrollPane2, Priority.ALWAYS);

        VBox resultContent = new VBox(14);
        resultContent.setPadding(new Insets(4));
        scrollPane2.setContent(resultContent);

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: rgba(255,184,77,0.18); -fx-text-fill: #FFB84D; " +
                "-fx-font-weight: 800; -fx-background-radius: 14; -fx-padding: 10 28; -fx-cursor: hand; " +
                "-fx-border-color: rgba(255,184,77,0.4); -fx-border-radius: 14; -fx-border-width: 1;");
        closeBtn.setOnAction(ev -> analyzeStage.close());
        HBox bottomBar = new HBox(closeBtn);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(header, projectName, budgetLine, sep, loadingBox, scrollPane2, bottomBar);

        Scene scene = new Scene(root, 580, 560);
        analyzeStage.setScene(scene);
        analyzeStage.show();

        com.bizhub.model.services.investissement.AI.OpenRouterService ai =
                new com.bizhub.model.services.investissement.AI.OpenRouterService();

        javafx.concurrent.Task<String> task = ai.analyzePitchAsync(
                project.getTitle(),
                project.getDescription() != null ? project.getDescription() : "No description",
                project.getRequiredBudget().doubleValue());

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            loadingBox.setVisible(false);
            loadingBox.setManaged(false);
            scrollPane2.setVisible(true);

            try {
                String raw = task.getValue().strip();
                if (raw.startsWith("```")) raw = raw.replaceAll("^```[a-z]*\\s*", "").replaceAll("```\\s*$", "").strip();
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();

                int risk = json.has("riskScore") ? json.get("riskScore").getAsInt() : 0;
                String riskColor = risk <= 3 ? "#10B981" : risk <= 6 ? "#FFB84D" : "#EF4444";
                String riskLabel = risk <= 3 ? "Low Risk" : risk <= 6 ? "Medium Risk" : "High Risk";

                HBox riskCard = new HBox(16);
                riskCard.setAlignment(Pos.CENTER_LEFT);
                riskCard.setPadding(new Insets(16));
                riskCard.setStyle("-fx-background-color: rgba(26,51,82,0.96); -fx-background-radius: 16; " +
                        "-fx-border-color: " + riskColor + "44; -fx-border-radius: 16; -fx-border-width: 1.5;");

                Label riskNum = new Label(risk + "/10");
                riskNum.setStyle("-fx-text-fill: " + riskColor + "; -fx-font-size: 32px; -fx-font-weight: 900;");
                VBox riskInfo = new VBox(2);
                Label riskTitle = new Label("Risk Score");
                riskTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 800;");
                Label riskTag = new Label(riskLabel);
                riskTag.setStyle("-fx-text-fill: " + riskColor + "; -fx-font-size: 12px; -fx-font-weight: 700;");
                riskInfo.getChildren().addAll(riskTitle, riskTag);
                riskCard.getChildren().addAll(riskNum, riskInfo);
                resultContent.getChildren().add(riskCard);

                if (json.has("summary") && json.get("summary").isJsonArray()) {
                    VBox summaryBox = createSection("Summary", "#FFB84D");
                    for (var el : json.getAsJsonArray("summary")) {
                        summaryBox.getChildren().add(bulletLabel(el.getAsString(), "#FFFFFF"));
                    }
                    resultContent.getChildren().add(summaryBox);
                }

                if (json.has("strengths") && json.get("strengths").isJsonArray()) {
                    VBox strengthsBox = createSection("Strengths", "#10B981");
                    for (var el : json.getAsJsonArray("strengths")) {
                        strengthsBox.getChildren().add(bulletLabel(el.getAsString(), "#10B981"));
                    }
                    resultContent.getChildren().add(strengthsBox);
                }

                if (json.has("weaknesses") && json.get("weaknesses").isJsonArray()) {
                    VBox weaknessBox = createSection("Risks & Weaknesses", "#EF4444");
                    for (var el : json.getAsJsonArray("weaknesses")) {
                        weaknessBox.getChildren().add(bulletLabel(el.getAsString(), "#EF4444"));
                    }
                    resultContent.getChildren().add(weaknessBox);
                }

                if (json.has("verdict")) {
                    Label verdictLabel = new Label(json.get("verdict").getAsString());
                    verdictLabel.setWrapText(true);
                    verdictLabel.setStyle("-fx-text-fill: #A5B4FC; -fx-font-size: 14px; -fx-font-style: italic; " +
                            "-fx-padding: 0 0 6 0;");
                    resultContent.getChildren().add(1, verdictLabel);
                }

                if (json.has("valuationMin") && json.has("valuationMax")) {
                    double vMin = json.get("valuationMin").getAsDouble();
                    double vMax = json.get("valuationMax").getAsDouble();
                    VBox valBox = createSection("Estimated Valuation", "#3B82F6");
                    Label valRange = new Label(currencyFormat.format(vMin) + " - " + currencyFormat.format(vMax) + " TND");
                    valRange.setStyle("-fx-text-fill: #3B82F6; -fx-font-size: 18px; -fx-font-weight: 900;");
                    valBox.getChildren().add(valRange);
                    resultContent.getChildren().add(valBox);
                }

                if (json.has("recommendation")) {
                    VBox recBox = createSection("Recommendation", "#FFB84D");
                    Label recText = new Label(json.get("recommendation").getAsString());
                    recText.setWrapText(true);
                    recText.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-line-spacing: 3;");
                    recBox.getChildren().add(recText);
                    resultContent.getChildren().add(recBox);
                }

            } catch (Exception ex) {
                Label fallback = new Label(task.getValue());
                fallback.setWrapText(true);
                fallback.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px;");
                resultContent.getChildren().add(fallback);
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            loadingBox.setVisible(false);
            loadingBox.setManaged(false);
            scrollPane2.setVisible(true);

            Label err = new Label("Analysis failed: " + task.getException().getMessage());
            err.setWrapText(true);
            err.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px; -fx-font-weight: 700;");
            resultContent.getChildren().add(err);
        }));

        new Thread(task).start();
    }

    private void handleDueDiligence(Project project) {
        Stage owner = (Stage) projectsContainer.getScene().getWindow();
        Stage ddStage = new Stage();
        ddStage.initOwner(owner);
        ddStage.setTitle("Due Diligence - " + project.getTitle());

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0A192F;");

        HBox headerBar = new HBox(14);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(22, 28, 16, 28));
        headerBar.setStyle("-fx-background-color: rgba(26,51,82,0.7);");
        Label header = new Label("AI Due Diligence Report");
        header.setStyle("-fx-text-fill: #06B6D4; -fx-font-size: 20px; -fx-font-weight: 900;");
        Label projectTitle = new Label(project.getTitle());
        projectTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 700;");
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Button exportPdfBtn = new Button("Export PDF");
        exportPdfBtn.setStyle("-fx-background-color: #06B6D4; -fx-text-fill: white; -fx-font-weight: 800; " +
                "-fx-background-radius: 12; -fx-padding: 8 20; -fx-cursor: hand;");
        exportPdfBtn.setDisable(true);
        headerBar.getChildren().addAll(new VBox(4, header, projectTitle), hSpacer, exportPdfBtn);

        Label stepLabel = new Label("Step 1/4: Analyzing pitch...");
        stepLabel.setStyle("-fx-text-fill: #06B6D4; -fx-font-size: 13px; -fx-font-weight: 700;");
        stepLabel.setPadding(new Insets(10, 28, 0, 28));

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefHeight(6);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #06B6D4;");
        VBox.setMargin(progressBar, new Insets(4, 28, 10, 28));

        ScrollPane scrollPane2 = new ScrollPane();
        scrollPane2.setFitToWidth(true);
        scrollPane2.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane2, Priority.ALWAYS);

        VBox resultContent = new VBox(16);
        resultContent.setPadding(new Insets(8, 28, 20, 28));
        scrollPane2.setContent(resultContent);

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: rgba(6,182,212,0.15); -fx-text-fill: #06B6D4; -fx-font-weight: 800; " +
                "-fx-background-radius: 14; -fx-padding: 10 28; -fx-cursor: hand; " +
                "-fx-border-color: rgba(6,182,212,0.3); -fx-border-radius: 14; -fx-border-width: 1;");
        closeBtn.setOnAction(ev -> ddStage.close());
        HBox bottomBar = new HBox(closeBtn);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(10, 28, 16, 28));

        root.getChildren().addAll(headerBar, stepLabel, progressBar, scrollPane2, bottomBar);

        Scene scene = new Scene(root, 700, 680);
        ddStage.setScene(scene);
        ddStage.show();

        com.bizhub.model.services.investissement.AI.DueDiligenceAgent agent =
                new com.bizhub.model.services.investissement.AI.DueDiligenceAgent();

        javafx.concurrent.Task<com.bizhub.model.services.investissement.AI.DueDiligenceAgent.DueDiligenceResult> task =
                new javafx.concurrent.Task<>() {
                    @Override
                    protected com.bizhub.model.services.investissement.AI.DueDiligenceAgent.DueDiligenceResult call() throws Exception {
                        updateMessage("Step 1/4: Analyzing pitch...");
                        updateProgress(0.15, 1);
                        // The agent internally runs all 4 steps
                        return agent.run(project);
                    }
                };

        task.messageProperty().addListener((obs, o, n) -> Platform.runLater(() -> stepLabel.setText(n)));
        task.progressProperty().addListener((obs, o, n) -> Platform.runLater(() -> progressBar.setProgress(n.doubleValue())));

        // Simulate step updates with a background timeline
        javafx.animation.Timeline stepTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), ev -> {
                    stepLabel.setText("Step 2/4: Fetching market news...");
                    progressBar.setProgress(0.35);
                }),
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), ev -> {
                    stepLabel.setText("Step 3/4: Checking sector performance...");
                    progressBar.setProgress(0.55);
                }),
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(8), ev -> {
                    stepLabel.setText("Step 4/4: AI synthesizing report...");
                    progressBar.setProgress(0.75);
                })
        );
        stepTimeline.play();

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            stepTimeline.stop();
            stepLabel.setText("Report complete");
            progressBar.setProgress(1.0);

            var result = task.getValue();
            com.google.gson.JsonObject report = result.reportObject();

            // --- Project Summary ---
            if (report.has("projectSummary")) {
                com.google.gson.JsonObject ps = report.getAsJsonObject("projectSummary");
                VBox summaryBox = ddSection("Project Summary", "#06B6D4");
                if (ps.has("oneLine")) {
                    Label oneLine = new Label(ps.get("oneLine").getAsString());
                    oneLine.setWrapText(true);
                    oneLine.setStyle("-fx-text-fill: #A5B4FC; -fx-font-size: 14px; -fx-font-style: italic;");
                    summaryBox.getChildren().add(oneLine);
                }
                if (ps.has("keyNumbers") && ps.get("keyNumbers").isJsonArray()) {
                    for (var el : ps.getAsJsonArray("keyNumbers")) {
                        summaryBox.getChildren().add(ddBullet(el.getAsString(), "#FFFFFF"));
                    }
                }
                resultContent.getChildren().add(summaryBox);
            }

            // --- Risk Matrix ---
            if (report.has("riskMatrix")) {
                com.google.gson.JsonObject rm = report.getAsJsonObject("riskMatrix");
                VBox riskBox = ddSection("Risk Matrix", "#EF4444");
                HBox riskCards = new HBox(10);
                riskCards.setAlignment(Pos.CENTER_LEFT);
                String[] riskKeys = {"financial", "market", "team", "execution", "regulatory"};
                for (String key : riskKeys) {
                    int val = rm.has(key) ? rm.get(key).getAsInt() : 0;
                    String color = val <= 3 ? "#10B981" : val <= 6 ? "#FFB84D" : "#EF4444";
                    VBox rc = new VBox(2);
                    rc.setAlignment(Pos.CENTER);
                    rc.setPadding(new Insets(10, 14, 10, 14));
                    rc.setStyle("-fx-background-color: rgba(26,51,82,0.96); -fx-background-radius: 12; " +
                            "-fx-border-color: " + color + "33; -fx-border-radius: 12; -fx-border-width: 1;");
                    Label valLabel = new Label(val + "/10");
                    valLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 20px; -fx-font-weight: 900;");
                    Label nameLabel = new Label(key.substring(0, 1).toUpperCase() + key.substring(1));
                    nameLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 10px; -fx-font-weight: 700;");
                    rc.getChildren().addAll(valLabel, nameLabel);
                    HBox.setHgrow(rc, Priority.ALWAYS);
                    riskCards.getChildren().add(rc);
                }
                riskBox.getChildren().add(riskCards);
                if (rm.has("overallComment")) {
                    Label comment = new Label(rm.get("overallComment").getAsString());
                    comment.setWrapText(true);
                    comment.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px;");
                    riskBox.getChildren().add(comment);
                }
                resultContent.getChildren().add(riskBox);
            }

            // --- SWOT ---
            if (report.has("swot")) {
                com.google.gson.JsonObject swot = report.getAsJsonObject("swot");
                HBox swotRow = new HBox(12);
                String[][] swotParts = {
                        {"strengths", "Strengths", "#10B981"},
                        {"weaknesses", "Weaknesses", "#EF4444"},
                        {"opportunities", "Opportunities", "#3B82F6"},
                        {"threats", "Threats", "#F59E0B"}
                };
                for (String[] part : swotParts) {
                    VBox box = ddSection(part[1], part[2]);
                    HBox.setHgrow(box, Priority.ALWAYS);
                    if (swot.has(part[0]) && swot.get(part[0]).isJsonArray()) {
                        for (var el : swot.getAsJsonArray(part[0])) {
                            box.getChildren().add(ddBullet(el.getAsString(), part[2]));
                        }
                    }
                    swotRow.getChildren().add(box);
                }
                resultContent.getChildren().add(swotRow);
            }

            // --- Market Context ---
            if (report.has("marketContext")) {
                com.google.gson.JsonObject mc = report.getAsJsonObject("marketContext");
                VBox mcBox = ddSection("Market Context", "#FFB84D");
                String tone = mc.has("tone") ? mc.get("tone").getAsString() : "unknown";
                String toneColor = switch (tone) {
                    case "positive" -> "#10B981";
                    case "negative" -> "#EF4444";
                    case "mixed" -> "#F59E0B";
                    default -> "#A5B4FC";
                };
                Label toneBadge = new Label("Sentiment: " + tone.toUpperCase());
                toneBadge.setStyle("-fx-background-color: " + toneColor + "22; -fx-text-fill: " + toneColor + "; " +
                        "-fx-background-radius: 999; -fx-padding: 3 12; -fx-font-size: 11px; -fx-font-weight: 800;");
                mcBox.getChildren().add(toneBadge);
                if (mc.has("headlineSummary") && mc.get("headlineSummary").isJsonArray()) {
                    for (var el : mc.getAsJsonArray("headlineSummary")) {
                        mcBox.getChildren().add(ddBullet(el.getAsString(), "#FFFFFF"));
                    }
                }
                if (mc.has("notes")) {
                    Label notes = new Label(mc.get("notes").getAsString());
                    notes.setWrapText(true);
                    notes.setStyle("-fx-text-fill: rgba(255,255,255,0.65); -fx-font-size: 12px;");
                    mcBox.getChildren().add(notes);
                }
                resultContent.getChildren().add(mcBox);
            }

            // --- Sector Outlook ---
            if (report.has("sectorOutlook")) {
                com.google.gson.JsonObject so = report.getAsJsonObject("sectorOutlook");
                VBox soBox = ddSection("Sector Outlook", "#8B5CF6");
                if (so.has("keySectors") && so.get("keySectors").isJsonArray()) {
                    for (var el : so.getAsJsonArray("keySectors")) {
                        if (!el.isJsonObject()) continue;
                        com.google.gson.JsonObject s = el.getAsJsonObject();
                        String name = s.has("name") ? s.get("name").getAsString() : "";
                        double perf = s.has("performance") ? s.get("performance").getAsDouble() : 0;
                        String comment = s.has("comment") ? s.get("comment").getAsString() : "";
                        String pColor = perf >= 0 ? "#10B981" : "#EF4444";
                        Label sLabel = new Label(name + "  " + String.format("%+.1f%%", perf) + "  " + comment);
                        sLabel.setWrapText(true);
                        sLabel.setStyle("-fx-text-fill: " + pColor + "; -fx-font-size: 12px;");
                        soBox.getChildren().add(sLabel);
                    }
                }
                if (so.has("overallComment")) {
                    Label oc = new Label(so.get("overallComment").getAsString());
                    oc.setWrapText(true);
                    oc.setStyle("-fx-text-fill: rgba(255,255,255,0.65); -fx-font-size: 12px;");
                    soBox.getChildren().add(oc);
                }
                resultContent.getChildren().add(soBox);
            }

            // --- Valuation ---
            if (report.has("valuation")) {
                com.google.gson.JsonObject val = report.getAsJsonObject("valuation");
                VBox valBox = ddSection("Valuation Estimate", "#3B82F6");
                double vMin = val.has("valuationMin") ? val.get("valuationMin").getAsDouble() : 0;
                double vMax = val.has("valuationMax") ? val.get("valuationMax").getAsDouble() : 0;
                Label range = new Label(currencyFormat.format(vMin) + " - " + currencyFormat.format(vMax) + " TND");
                range.setStyle("-fx-text-fill: #3B82F6; -fx-font-size: 20px; -fx-font-weight: 900;");
                valBox.getChildren().add(range);
                if (val.has("comment")) {
                    Label c = new Label(val.get("comment").getAsString());
                    c.setWrapText(true);
                    c.setStyle("-fx-text-fill: rgba(255,255,255,0.65); -fx-font-size: 12px;");
                    valBox.getChildren().add(c);
                }
                resultContent.getChildren().add(valBox);
            }

            // --- Final Recommendation ---
            if (report.has("finalRecommendation")) {
                com.google.gson.JsonObject rec = report.getAsJsonObject("finalRecommendation");
                String label = rec.has("label") ? rec.get("label").getAsString() : "N/A";
                String recColor = switch (label.toUpperCase()) {
                    case "INVEST" -> "#10B981";
                    case "AVOID" -> "#EF4444";
                    default -> "#F59E0B";
                };
                VBox recBox = ddSection("Recommendation", recColor);
                Label recBadge = new Label(label);
                recBadge.setStyle("-fx-background-color: " + recColor + "; -fx-text-fill: white; " +
                        "-fx-font-size: 16px; -fx-font-weight: 900; -fx-background-radius: 12; -fx-padding: 6 20;");
                recBox.getChildren().add(recBadge);
                if (rec.has("summary")) {
                    Label sum = new Label(rec.get("summary").getAsString());
                    sum.setWrapText(true);
                    sum.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px;");
                    recBox.getChildren().add(sum);
                }
                if (rec.has("nextSteps") && rec.get("nextSteps").isJsonArray()) {
                    Label nst = new Label("Next Steps:");
                    nst.setStyle("-fx-text-fill: " + recColor + "; -fx-font-size: 12px; -fx-font-weight: 800;");
                    recBox.getChildren().add(nst);
                    for (var el : rec.getAsJsonArray("nextSteps")) {
                        recBox.getChildren().add(ddBullet(el.getAsString(), "#FFFFFF"));
                    }
                }
                resultContent.getChildren().add(recBox);
            }

            // Enable PDF export
            exportPdfBtn.setDisable(false);
            exportPdfBtn.setOnAction(ev -> {
                try {
                    com.bizhub.model.services.investissement.AI.DueDiligenceReportPDF.export(project, result.reportObject());
                    showAlert(Alert.AlertType.INFORMATION, "PDF Exported",
                            "Due diligence report saved to: BizHub_DueDiligence_" + project.getProjectId() + ".pdf");
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "PDF Error", "Could not export PDF: " + ex.getMessage());
                }
            });
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            stepTimeline.stop();
            stepLabel.setText("Analysis failed");
            progressBar.setProgress(0);
            Label err = new Label("Due diligence failed: " + task.getException().getMessage());
            err.setWrapText(true);
            err.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px; -fx-font-weight: 700;");
            resultContent.getChildren().add(err);
        }));

        new Thread(task).start();
    }

    private VBox ddSection(String title, String color) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: rgba(26,51,82,0.96); -fx-background-radius: 16; " +
                "-fx-border-color: " + color + "22; -fx-border-radius: 16; -fx-border-width: 1;");
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 15px; -fx-font-weight: 900;");
        box.getChildren().add(t);
        return box;
    }

    private Label ddBullet(String text, String color) {
        Label l = new Label("  •  " + text);
        l.setWrapText(true);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-line-spacing: 2;");
        return l;
    }

    private VBox createSection(String title, String color) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: rgba(26,51,82,0.96); -fx-background-radius: 16; " +
                "-fx-border-color: " + color + "22; -fx-border-radius: 16; -fx-border-width: 1;");
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 15px; -fx-font-weight: 900;");
        box.getChildren().add(t);
        return box;
    }

    private Label bulletLabel(String text, String color) {
        Label l = new Label("  •  " + text);
        l.setWrapText(true);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-line-spacing: 2;");
        return l;
    }

    public void filterByProject(Integer projectId, String projectTitle) {
        loadProjects();

        if (allProjects != null && projectId != null) {
            List<Project> filtered = allProjects.stream()
                    .filter(p -> p.getProjectId().equals(projectId))
                    .collect(Collectors.toList());

            displayProjects(filtered);
            searchField.setText(projectTitle);
        }
    }

    private void filterProjects() {
        if (allProjects == null) return;

        String searchText = searchField.getText().toLowerCase().trim();
        String selectedStatus = statusFilterComboBox.getValue();

        List<Project> filtered = allProjects.stream()
                .filter(project -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            project.getTitle().toLowerCase().contains(searchText) ||
                            (project.getDescription() != null &&
                                    project.getDescription().toLowerCase().contains(searchText));

                    boolean matchesStatus = selectedStatus.equals("Tous les statuts") ||
                            project.getStatus().equals(selectedStatus);

                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toList());

        displayProjects(filtered);
    }

    private void updateSummary() {
        if (allProjects == null || allProjects.isEmpty()) {
            summaryLabel.setText("Aucun projet");
            return;
        }

        int totalProjects = allProjects.size();
        long fundedProjects = allProjects.stream()
                .filter(p -> "funded".equals(p.getStatus()) || "complete".equals(p.getStatus()))
                .count();
        BigDecimal totalBudget = allProjects.stream()
                .map(Project::getRequiredBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInvested = allProjects.stream()
                .map(p -> p.getTotalInvested() != null ? p.getTotalInvested() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        summaryLabel.setText(totalProjects + " projet(s) • " + fundedProjects + " financé(s) • Budget total: " +
                currencyFormat.format(totalBudget) + " TND • Investi: " + currencyFormat.format(totalInvested) + " TND");
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "pending": return "#f39c12";
            case "funded": return "#27ae60";
            case "in_progress": return "#3498db";
            case "complete": return "#2ecc71";
            default: return "#95a5a6";
        }
    }

    private String getStatusIcon(String status) {
        switch (status) {
            case "pending": return "⏳";
            case "funded": return "✅";
            case "in_progress": return "🚧";
            case "complete": return "🎉";
            default: return "📋";
        }
    }

    private String getStatusBadgeStyle(String status) {
        switch (status) {
            case "pending":
                return "-fx-background-color: #fff3cd; -fx-text-fill: #856404; ";
            case "funded":
                return "-fx-background-color: #d4edda; -fx-text-fill: #155724; ";
            case "in_progress":
                return "-fx-background-color: #cce5ff; -fx-text-fill: #004085; ";
            case "complete":
                return "-fx-background-color: #d1ecf1; -fx-text-fill: #0c5460; ";
            default:
                return "-fx-background-color: #e2e3e5; -fx-text-fill: #383d41; ";
        }
    }

    private String getProgressColor(double percent) {
        if (percent >= 100) return "#27ae60";
        if (percent >= 70) return "#3498db";
        if (percent >= 30) return "#f39c12";
        return "#e74c3c";
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}