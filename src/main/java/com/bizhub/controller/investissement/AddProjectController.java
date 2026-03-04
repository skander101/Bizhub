package com.bizhub.controller.investissement;

import com.bizhub.model.investissement.Project;
import com.bizhub.model.services.investissement.ProjectServiceImpl;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.users_avis.user.User;
import com.bizhub.controller.users_avis.user.SidebarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AddProjectController {

    @FXML private SidebarController sidebarController;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField requiredBudgetField;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextField startupIdField;
    @FXML private Button submitButton;
    @FXML private Label messageLabel;
    @FXML private Label titleErrorLabel;
    @FXML private Label budgetErrorLabel;
    @FXML private Label statusErrorLabel;
    @FXML private Label startupErrorLabel;

    private ProjectServiceImpl projectService;
    private Integer createdProjectId;

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("add-project");
        projectService = new ProjectServiceImpl();

        // Initialiser le ComboBox des statuts
        statusComboBox.getItems().addAll(
                "pending",
                "funded",
                "in_progress",
                "complete"
        );
        statusComboBox.setValue("pending");

        // Validation du montant
        requiredBudgetField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                requiredBudgetField.setText(oldVal);
            }
        });

        // Pré-remplir l'ID startup avec l'utilisateur connecté (startup) et désactiver la saisie manuelle
        User current = AppSession.getCurrentUser();
        if (current != null) {
            startupIdField.setText(String.valueOf(current.getUserId()));
            startupIdField.setEditable(false);
            startupIdField.setDisable(true);
        }
    }

    @FXML
    private void handleSubmit() {
        resetErrors();

        if (!validateForm()) {
            return;
        }

        try {
            int startupId = Integer.parseInt(startupIdField.getText().trim());

            if (!startupExists(startupId)) {
                startupErrorLabel.setText("⚠ ID startup introuvable (aucun user avec cet ID)");
                startupErrorLabel.setVisible(true);
                return;
            }

            Project project = new Project();
            project.setTitle(titleField.getText().trim());
            project.setDescription(descriptionArea.getText().trim().isEmpty() ? null : descriptionArea.getText().trim());
            project.setRequiredBudget(new BigDecimal(requiredBudgetField.getText().trim()));
            project.setStatus(statusComboBox.getValue());
            project.setStartupId(startupId);

            int projectId = projectService.add(project);

            if (projectId > 0) {
                this.createdProjectId = projectId;
                showSuccessMessage("✅ Projet créé avec succès! ID: " + projectId);

                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        javafx.application.Platform.runLater(() -> {
                            try {
                                navigateToAddInvestment(projectId, project.getTitle());
                            } catch (IOException e) {
                                e.printStackTrace();
                                showErrorMessage("Erreur lors de la navigation");
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                showErrorMessage("❌ Erreur lors de la création du projet");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("❌ Erreur base de données: " + e.getMessage());
        } catch (NumberFormatException e) {
            budgetErrorLabel.setText("⚠ Montant invalide");
            budgetErrorLabel.setVisible(true);
        }
    }

    private boolean startupExists(int startupId) throws SQLException {
        String sql = "SELECT 1 FROM `user` WHERE user_id = ? LIMIT 1";
        try (Connection conn = com.bizhub.model.services.common.DB.MyDatabase.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, startupId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (titleField.getText().trim().isEmpty()) {
            titleErrorLabel.setText("⚠ Le titre est obligatoire");
            titleErrorLabel.setVisible(true);
            isValid = false;
        }

        if (requiredBudgetField.getText().trim().isEmpty()) {
            budgetErrorLabel.setText("⚠ Le budget est obligatoire");
            budgetErrorLabel.setVisible(true);
            isValid = false;
        } else {
            try {
                BigDecimal budget = new BigDecimal(requiredBudgetField.getText().trim());
                if (budget.compareTo(BigDecimal.ZERO) <= 0) {
                    budgetErrorLabel.setText("⚠ Le budget doit être supérieur à 0");
                    budgetErrorLabel.setVisible(true);
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                budgetErrorLabel.setText("⚠ Montant invalide");
                budgetErrorLabel.setVisible(true);
                isValid = false;
            }
        }

        if (statusComboBox.getValue() == null) {
            statusErrorLabel.setText("⚠ Veuillez sélectionner un statut");
            statusErrorLabel.setVisible(true);
            isValid = false;
        }

        // Vérifier qu'un utilisateur startup est connecté
        User current = AppSession.getCurrentUser();
        if (current == null) {
            startupErrorLabel.setText("⚠ Vous devez être connecté en tant que startup");
            startupErrorLabel.setVisible(true);
            isValid = false;
        } else if (!"startup".equalsIgnoreCase(current.getUserType())) {
            startupErrorLabel.setText("⚠ Seuls les comptes startup peuvent créer un projet");
            startupErrorLabel.setVisible(true);
            isValid = false;
        }

        return isValid;
    }

    private void resetErrors() {
        titleErrorLabel.setVisible(false);
        budgetErrorLabel.setVisible(false);
        statusErrorLabel.setVisible(false);
        startupErrorLabel.setVisible(false);
        messageLabel.setVisible(false);
    }

    private void showSuccessMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724; " +
                "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;");
        messageLabel.setVisible(true);
    }

    private void showErrorMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #721c24; " +
                "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;");
        messageLabel.setVisible(true);
    }

    private void navigateToAddInvestment(int projectId, String projectTitle) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/AddInvestistmentView.fxml"));
        Parent root = loader.load();

        AddInvestmentController controller = loader.getController();
        controller.setPreSelectedProject(projectId, projectTitle);

        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.setTitle("Nouvel Investissement pour: " + projectTitle);
    }

    @FXML
    private void handleCancel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/ProjectsListView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) submitButton.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Liste des Projets");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Integer getCreatedProjectId() {
        return createdProjectId;
    }
}