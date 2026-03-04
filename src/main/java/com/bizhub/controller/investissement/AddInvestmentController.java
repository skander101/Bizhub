package com.bizhub.controller.investissement;

import com.bizhub.model.investissement.Investment;
import com.bizhub.model.investissement.InvestissementUser;
import com.bizhub.model.investissement.ProjectItem;
import com.bizhub.model.services.investissement.InvestmentServiceImpl;
import com.bizhub.controller.users_avis.user.SidebarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class AddInvestmentController {

    @FXML private SidebarController sidebarController;
    @FXML private ComboBox<ProjectItem> projectComboBox;
    @FXML private ComboBox<InvestorItem> investorComboBox;
    @FXML private TextField amountField;
    @FXML private TextField contractUrlField;
    @FXML private Button submitButton;
    @FXML private Label messageLabel;
    @FXML private Label projectErrorLabel;
    @FXML private Label investorErrorLabel;
    @FXML private Label amountErrorLabel;

    private InvestmentServiceImpl investmentService;

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("add-investment");
        investmentService = new InvestmentServiceImpl();

        loadProjects();
        loadInvestors();

        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                amountField.setText(oldVal);
            }
        });
    }

    private void loadProjects() {
        try {
            projectComboBox.getItems().addAll(
                    investmentService.getProjects()
            );
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Erreur lors du chargement des projets");
        }
    }


    private void loadInvestors() {
        try {
            // Récupérer les investisseurs depuis la base
            List<InvestissementUser> investors = investmentService.getInvestors();

            for (InvestissementUser u : investors) {
                investorComboBox.getItems().add(
                        new InvestorItem(u.getUserId(), "Investisseur #" + u.getUserId())
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Erreur lors du chargement des investisseurs");
        }
    }


    @FXML
    private void handleSubmit() {
        resetErrors();

        if (!validateForm()) {
            return;
        }

        try {
            Investment investment = new Investment();
            investment.setProjectId(projectComboBox.getValue().getId());
            investment.setInvestorId(investorComboBox.getValue().getId());
            investment.setAmount(new BigDecimal(amountField.getText()));
            investment.setContractUrl(contractUrlField.getText().trim().isEmpty()
                    ? null : contractUrlField.getText().trim());

            int investmentId = investmentService.add(investment);

            if (investmentId > 0) {
                showSuccessMessage("✅ Investissement créé avec succès!");

                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        javafx.application.Platform.runLater(() -> {
                            try {
                                navigateToAddPayment(investmentId);
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
                showErrorMessage("❌ Erreur lors de la création");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("❌ Erreur base de données: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (projectComboBox.getValue() == null) {
            projectErrorLabel.setText("⚠ Veuillez sélectionner un projet");
            projectErrorLabel.setVisible(true);
            isValid = false;
        }

        if (investorComboBox.getValue() == null) {
            investorErrorLabel.setText("⚠ Veuillez sélectionner un investisseur");
            investorErrorLabel.setVisible(true);
            isValid = false;
        }

        if (amountField.getText().trim().isEmpty()) {
            amountErrorLabel.setText("⚠ Le montant est obligatoire");
            amountErrorLabel.setVisible(true);
            isValid = false;
        } else {
            try {
                BigDecimal amount = new BigDecimal(amountField.getText());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    amountErrorLabel.setText("⚠ Le montant doit être supérieur à 0");
                    amountErrorLabel.setVisible(true);
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                amountErrorLabel.setText("⚠ Montant invalide");
                amountErrorLabel.setVisible(true);
                isValid = false;
            }
        }

        return isValid;
    }

    private void resetErrors() {
        projectErrorLabel.setVisible(false);
        investorErrorLabel.setVisible(false);
        amountErrorLabel.setVisible(false);
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

    /**
     * Pre-selects a project in the combo box. Called from AddProjectController
     * after a new project is created, to streamline the add-investment flow.
     */
    public void setPreSelectedProject(int projectId, String projectTitle) {
        if (projectComboBox == null) return;
        for (ProjectItem item : projectComboBox.getItems()) {
            if (item.getId() != null && item.getId().equals(projectId)) {
                projectComboBox.setValue(item);
                return;
            }
        }
        // If not found yet (items may not be loaded), add it dynamically
        ProjectItem item = new ProjectItem(projectId, projectTitle);
        projectComboBox.getItems().add(0, item);
        projectComboBox.setValue(item);
    }

    private void navigateToAddPayment(int investmentId) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/AddPaymentView.fxml"));
        Parent root = loader.load();

        AddPaymentController controller = loader.getController();
        controller.setInvestmentId(investmentId);

        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Premier Paiement");
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.close();
    }

    public static class InvestorItem {
        private final Integer id;
        private final String name;

        public InvestorItem(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() { return id; }
        public String getName() { return name; }

        @Override
        public String toString() { return name; }
    }
}



