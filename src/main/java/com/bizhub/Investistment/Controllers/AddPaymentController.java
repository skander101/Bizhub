package com.bizhub.Investistment.Controllers;

import com.bizhub.Investistment.Entitites.Investment;
import com.bizhub.Investistment.Entitites.Payment;
import com.bizhub.Investistment.Services.InvestmentServiceImpl;
import com.bizhub.Investistment.Services.PaymentServiceImpl;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;

public class AddPaymentController {

    @FXML private Label investmentInfoLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label remainingAmountLabel;
    @FXML private TextField paymentAmountField;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private TextField transactionRefField;
    @FXML private TextArea notesArea;
    @FXML private Button submitButton;
    @FXML private Button viewInvestmentsButton;
    @FXML private Label messageLabel;
    @FXML private Label paymentAmountErrorLabel;
    @FXML private Label paymentMethodErrorLabel;
    @FXML private Label transactionRefErrorLabel;

    private Integer investmentId;
    private Investment currentInvestment;
    private InvestmentServiceImpl investmentService;
    private PaymentServiceImpl paymentService;
    private NumberFormat currencyFormat;

    @FXML
    public void initialize() {
        investmentService = new InvestmentServiceImpl();
        paymentService = new PaymentServiceImpl();
        currencyFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);

        loadPaymentMethods();


        paymentAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                paymentAmountField.setText(oldVal);
            } else if (!newVal.isEmpty()) {
                updateRemainingAmount();
            }
        });
    }

    public void setInvestmentId(Integer investmentId) {
        this.investmentId = investmentId;
        loadInvestmentInfo();
    }

    private void loadInvestmentInfo() {
        try {
            currentInvestment = investmentService.getById(investmentId);

            if (currentInvestment != null) {
                investmentInfoLabel.setText(
                        "Projet: " + currentInvestment.getProjectTitle() +
                                " | Investisseur: " + currentInvestment.getInvestorName()
                );

                totalAmountLabel.setText(
                        currencyFormat.format(currentInvestment.getAmount()) + " TND"
                );

                updateRemainingAmount();
            } else {
                showErrorMessage("❌ Investissement introuvable!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("❌ Erreur lors du chargement: " + e.getMessage());
        }
    }

    private void loadPaymentMethods() {
        paymentMethodComboBox.getItems().addAll(
                "Virement bancaire",
                "Chèque",
                "Espèces",
                "Carte bancaire",
                "PayPal",
                "Crypto-monnaie",
                "Autre"
        );
        paymentMethodComboBox.setValue("Virement bancaire");
    }



    private void updateRemainingAmount() {
        if (currentInvestment == null) return;

        try {
            String amountText = paymentAmountField.getText().trim();
            if (!amountText.isEmpty()) {
                BigDecimal paymentAmount = new BigDecimal(amountText);
                BigDecimal remaining = currentInvestment.getAmount().subtract(paymentAmount);

                remainingAmountLabel.setText(
                        "Montant restant après ce paiement: " +
                                currencyFormat.format(remaining) + " TND"
                );

                if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                    remainingAmountLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 13px; -fx-font-weight: bold;");
                } else if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    remainingAmountLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 13px; -fx-font-weight: bold;");
                } else {
                    remainingAmountLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px; -fx-font-weight: bold;");
                }
            }
        } catch (NumberFormatException e) {
            remainingAmountLabel.setText("Montant restant après ce paiement: - TND");
        }
    }

    @FXML
    private void handleSubmit() {
        resetErrors();

        if (!validateForm()) {
            return;
        }

        try {
            Payment payment = new Payment();
            payment.setInvestmentId(investmentId);
            payment.setAmount(new BigDecimal(paymentAmountField.getText().trim()));
            payment.setPaymentMethod(paymentMethodComboBox.getValue());
            payment.setPaymentStatus("completed");

            payment.setTransactionReference(transactionRefField.getText().trim());
            payment.setNotes(notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim());

            int paymentId = paymentService.add(payment);

            if (paymentId > 0) {
                showSuccessMessage("✅ Paiement enregistré avec succès! ID: " + paymentId);

                disableForm();

                viewInvestmentsButton.setVisible(true);
                submitButton.setVisible(false);

            } else {
                showErrorMessage("❌ Erreur lors de l'enregistrement du paiement");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("❌ Erreur base de données: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (paymentAmountField.getText().trim().isEmpty()) {
            paymentAmountErrorLabel.setText("⚠ Le montant est obligatoire");
            paymentAmountErrorLabel.setVisible(true);
            isValid = false;
        } else {
            try {
                BigDecimal amount = new BigDecimal(paymentAmountField.getText().trim());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    paymentAmountErrorLabel.setText("⚠ Le montant doit être supérieur à 0");
                    paymentAmountErrorLabel.setVisible(true);
                    isValid = false;
                } else if (amount.compareTo(currentInvestment.getAmount()) > 0) {
                    paymentAmountErrorLabel.setText("⚠ Le montant dépasse le total de l'investissement");
                    paymentAmountErrorLabel.setVisible(true);
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                paymentAmountErrorLabel.setText("⚠ Montant invalide");
                paymentAmountErrorLabel.setVisible(true);
                isValid = false;
            }
        }

        if (paymentMethodComboBox.getValue() == null || paymentMethodComboBox.getValue().isEmpty()) {
            paymentMethodErrorLabel.setText("⚠ Veuillez sélectionner une méthode");
            paymentMethodErrorLabel.setVisible(true);
            isValid = false;
        }



        if (transactionRefField.getText().trim().isEmpty()) {
            transactionRefErrorLabel.setText("⚠ La référence est obligatoire");
            transactionRefErrorLabel.setVisible(true);
            isValid = false;
        }

        return isValid;
    }

    private void resetErrors() {
        paymentAmountErrorLabel.setVisible(false);
        paymentMethodErrorLabel.setVisible(false);
        transactionRefErrorLabel.setVisible(false);
        messageLabel.setVisible(false);
    }

    private void disableForm() {
        paymentAmountField.setDisable(true);
        paymentMethodComboBox.setDisable(true);
        transactionRefField.setDisable(true);
        notesArea.setDisable(true);
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

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestion_investissement/Views/AddInvestmentView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) submitButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Nouvel Investissement");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewInvestments() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/InvestmentsListView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) viewInvestmentsButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Liste des Investissements");
        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("❌ Erreur de navigation: " + e.getMessage());
        }
    }
}