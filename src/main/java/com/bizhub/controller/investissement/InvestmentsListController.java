package com.bizhub.controller.investissement;

import com.bizhub.model.investissement.Investment;
import com.bizhub.model.investissement.Payment;
import com.bizhub.model.services.investissement.InvestmentServiceImpl;
import com.bizhub.model.services.investissement.PaymentServiceImpl;
import com.bizhub.controller.users_avis.user.SidebarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class InvestmentsListController {

    @FXML private SidebarController sidebarController;
    @FXML private VBox investmentsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Label summaryLabel;
    @FXML private Label emptyLabel;

    private InvestmentServiceImpl investmentService;
    private PaymentServiceImpl paymentService;
    private List<Investment> allInvestments;
    private NumberFormat currencyFormat;
    private DateTimeFormatter dateFormatter;

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("investments");
        investmentService = new InvestmentServiceImpl();
        paymentService = new PaymentServiceImpl();
        currencyFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);
        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        filterComboBox.getItems().addAll(
                "Tous les statuts",
                "Paiement complété",
                "Paiement en attente",
                "Paiement échoué"
        );
        filterComboBox.setValue("Tous les statuts");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterInvestments());
        filterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> filterInvestments());

        loadInvestments();
    }

    private void loadInvestments() {
        try {
            allInvestments = investmentService.getAll();
            displayInvestments(allInvestments);
            updateSummary();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors du chargement des investissements: " + e.getMessage());
        }
    }

    private void displayInvestments(List<Investment> investments) {
        investmentsContainer.getChildren().clear();

        if (investments.isEmpty()) {
            emptyLabel.setVisible(true);
            return;
        }

        emptyLabel.setVisible(false);

        for (Investment investment : investments) {
            try {
                VBox card = createInvestmentCard(investment);
                investmentsContainer.getChildren().add(card);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private VBox createInvestmentCard(Investment investment) throws SQLException {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 3);");

        // Header
        HBox header = createCardHeader(investment);
        Separator separator = new Separator();
        VBox paymentsSection = createPaymentsSection(investment);
        HBox actionsBox = createActionsBox(investment);

        card.getChildren().addAll(header, separator, paymentsSection, actionsBox);

        return card;
    }

    private HBox createCardHeader(Investment investment) {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(60, 60);
        iconBox.setStyle("-fx-background-color: #3498db; -fx-background-radius: 10;");
        Label iconLabel = new Label("💼");
        iconLabel.setStyle("-fx-font-size: 28px;");
        iconBox.getChildren().add(iconLabel);

        VBox infoBox = new VBox(5);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label projectLabel = new Label(investment.getProjectTitle());
        projectLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 18px; -fx-font-weight: bold;");

        HBox investorBox = new HBox(10);
        investorBox.setAlignment(Pos.CENTER_LEFT);
        Label investorIcon = new Label("👤");
        Label investorLabel = new Label(investment.getInvestorName());
        investorLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        investorBox.getChildren().addAll(investorIcon, investorLabel);

        HBox dateBox = new HBox(10);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Label dateIcon = new Label("📅");
        Label dateLabel = new Label(investment.getInvestmentDate().format(dateFormatter));
        dateLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        dateBox.getChildren().addAll(dateIcon, dateLabel);

        infoBox.getChildren().addAll(projectLabel, investorBox, dateBox);

        VBox amountBox = new VBox(3);
        amountBox.setAlignment(Pos.CENTER_RIGHT);
        Label amountTitle = new Label("Montant Total");
        amountTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        Label amountValue = new Label(currencyFormat.format(investment.getAmount()) + " TND");
        amountValue.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 22px; -fx-font-weight: bold;");
        amountBox.getChildren().addAll(amountTitle, amountValue);

        header.getChildren().addAll(iconBox, infoBox, amountBox);

        return header;
    }

    private VBox createPaymentsSection(Investment investment) throws SQLException {
        VBox section = new VBox(10);

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("💳 Historique des Paiements");
        titleLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        List<Payment> payments = paymentService.getByInvestmentId(investment.getInvestmentId());

        Label countLabel = new Label(payments.size() + " paiement(s)");
        countLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        titleBox.getChildren().addAll(titleLabel, spacer, countLabel);

        VBox paymentsListContainer = new VBox(8);
        paymentsListContainer.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 15;");

        if (payments.isEmpty()) {
            Label noPaymentsLabel = new Label("Aucun paiement enregistré");
            noPaymentsLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 13px; -fx-font-style: italic;");
            paymentsListContainer.getChildren().add(noPaymentsLabel);
        } else {
            for (Payment payment : payments) {
                HBox paymentItem = createPaymentItem(payment, investment);
                paymentsListContainer.getChildren().add(paymentItem);
            }
        }

        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = investment.getAmount().subtract(totalPaid);
        double progressPercent = totalPaid.divide(investment.getAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        VBox progressBox = new VBox(5);
        HBox progressInfo = new HBox(10);
        progressInfo.setAlignment(Pos.CENTER_LEFT);

        Label progressTitle = new Label("Progression:");
        progressTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        Label progressLabel = new Label(String.format("%.1f%%", progressPercent));
        progressLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 12px; -fx-font-weight: bold;");
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        Label remainingLabel = new Label("Restant: " + currencyFormat.format(remaining) + " TND");
        remainingLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;");

        progressInfo.getChildren().addAll(progressTitle, progressLabel, spacer2, remainingLabel);

        ProgressBar progressBar = new ProgressBar(progressPercent / 100);
        progressBar.setPrefHeight(8);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #27ae60;");

        progressBox.getChildren().addAll(progressInfo, progressBar);

        section.getChildren().addAll(titleBox, paymentsListContainer, progressBox);

        return section;
    }

    private HBox createPaymentItem(Payment payment, Investment investment) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-padding: 12;" +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-border-width: 1;");

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(35, 35);
        Label emojiLabel = new Label(getStatusEmoji(payment.getPaymentStatus()));
        emojiLabel.setStyle("-fx-font-size: 18px;");
        iconBox.getChildren().add(emojiLabel);

        VBox detailsBox = new VBox(3);
        HBox.setHgrow(detailsBox, Priority.ALWAYS);

        HBox line1 = new HBox(8);
        line1.setAlignment(Pos.CENTER_LEFT);
        Label amountLabel = new Label(currencyFormat.format(payment.getAmount()) + " TND");
        amountLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label dot1 = new Label("•");
        Label methodLabel = new Label(payment.getPaymentMethod());
        methodLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        line1.getChildren().addAll(amountLabel, dot1, methodLabel);

        HBox line2 = new HBox(8);
        line2.setAlignment(Pos.CENTER_LEFT);
        Label dateLabel = new Label(payment.getPaymentDate().format(dateFormatter));
        dateLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
        Label dot2 = new Label("•");
        Label refLabel = new Label(payment.getTransactionReference());
        refLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
        line2.getChildren().addAll(dateLabel, dot2, refLabel);

        detailsBox.getChildren().addAll(line1, line2);

        Label statusBadge = new Label(getStatusText(payment.getPaymentStatus()));
        statusBadge.setStyle(getStatusBadgeStyle(payment.getPaymentStatus()) +
                "-fx-background-radius: 12; -fx-padding: 4 10; -fx-font-size: 11px;");

        HBox actionsBox = new HBox(5);

        Button editBtn = new Button("✏️");
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f39c12; -fx-font-size: 14px; -fx-cursor: hand;");
        editBtn.setOnAction(e -> handleEditPayment(payment));

        Button deleteBtn = new Button("🗑️");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-size: 14px; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeletePayment(payment));

        actionsBox.getChildren().addAll(editBtn, deleteBtn);

        item.getChildren().addAll(iconBox, detailsBox, statusBadge, actionsBox);

        return item;
    }

    private HBox createActionsBox(Investment investment) {
        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button addPaymentBtn = new Button("➕ Ajouter Paiement");
        addPaymentBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-font-size: 12px; -fx-padding: 8 15;");
        addPaymentBtn.setOnAction(e -> handleAddPaymentToExisting(investment));

        Button editBtn = new Button("✏️ Modifier");
        editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-font-size: 12px; -fx-padding: 8 15;");
        editBtn.setOnAction(e -> handleEditInvestment(investment));

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-font-size: 12px; -fx-padding: 8 15;");
        deleteBtn.setOnAction(e -> handleDeleteInvestment(investment));

        actionsBox.getChildren().addAll(addPaymentBtn, editBtn, deleteBtn);

        return actionsBox;
    }

    // ==================== ACTIONS ====================

    private void handleAddPaymentToExisting(Investment investment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/AddPaymentView.fxml"));
            Parent root = loader.load();

            AddPaymentController controller = loader.getController();
            controller.setInvestmentId(investment.getInvestmentId());

            Stage stage = new Stage();
            stage.setTitle("Ajouter un Paiement");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            loadInvestments();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleEditInvestment(Investment investment) {
        TextInputDialog dialog = new TextInputDialog(investment.getAmount().toString());
        dialog.setTitle("Modifier l'Investissement");
        dialog.setHeaderText("Modifier le montant de l'investissement");
        dialog.setContentText("Nouveau montant (TND):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(amount -> {
            try {
                investment.setAmount(new BigDecimal(amount));
                boolean updated = investmentService.update(investment);

                if (updated) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Investissement modifié!");
                    loadInvestments();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec de la modification");
                }
            } catch (SQLException | NumberFormatException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur: " + e.getMessage());
            }
        });
    }

    private void handleDeleteInvestment(Investment investment) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer l'investissement ?");
        confirmAlert.setContentText("Projet: " + investment.getProjectTitle() + "\n\n⚠️ Tous les paiements seront supprimés!");

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                List<Payment> payments = paymentService.getByInvestmentId(investment.getInvestmentId());
                for (Payment payment : payments) {
                    paymentService.delete(payment.getPaymentId());
                }

                boolean deleted = investmentService.delete(investment.getInvestmentId());

                if (deleted) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Investissement supprimé!");
                    loadInvestments();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur: " + e.getMessage());
            }
        }
    }

    private void handleEditPayment(Payment payment) {
        TextInputDialog dialog = new TextInputDialog(payment.getAmount().toString());
        dialog.setTitle("Modifier le Paiement");
        dialog.setHeaderText("Modifier le montant du paiement");
        dialog.setContentText("Nouveau montant (TND):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(amount -> {
            try {
                payment.setAmount(new BigDecimal(amount));
                boolean updated = paymentService.update(payment);

                if (updated) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Paiement modifié!");
                    loadInvestments();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec");
                }
            } catch (SQLException | NumberFormatException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur: " + e.getMessage());
            }
        });
    }

    private void handleDeletePayment(Payment payment) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer le paiement ?");
        confirmAlert.setContentText("Montant: " + currencyFormat.format(payment.getAmount()) + " TND");

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = paymentService.delete(payment.getPaymentId());

                if (deleted) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Paiement supprimé!");
                    loadInvestments();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleNewInvestment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/AddInvestistmentView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) investmentsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        loadInvestments();
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        filterComboBox.setValue("Tous les statuts");
    }

    private void filterInvestments() {
        if (allInvestments == null) return;

        String searchText = searchField.getText().toLowerCase();
        String filterValue = filterComboBox.getValue();

        List<Investment> filtered = allInvestments.stream()
                .filter(inv -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            inv.getProjectTitle().toLowerCase().contains(searchText) ||
                            inv.getInvestorName().toLowerCase().contains(searchText);

                    boolean matchesFilter = filterValue.equals("Tous les statuts") ||
                            checkPaymentStatus(inv, filterValue);

                    return matchesSearch && matchesFilter;
                })
                .collect(Collectors.toList());

        displayInvestments(filtered);
    }

    private boolean checkPaymentStatus(Investment investment, String filter) {
        try {
            List<Payment> payments = paymentService.getByInvestmentId(investment.getInvestmentId());

            if (payments.isEmpty()) return false;

            switch (filter) {
                case "Paiement complété":
                    return payments.stream().anyMatch(p -> p.getPaymentStatus().equals("completed"));
                case "Paiement en attente":
                    return payments.stream().anyMatch(p -> p.getPaymentStatus().equals("pending"));
                case "Paiement échoué":
                    return payments.stream().anyMatch(p -> p.getPaymentStatus().equals("failed"));
                default:
                    return true;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private void updateSummary() {
        if (allInvestments == null || allInvestments.isEmpty()) {
            summaryLabel.setText("Aucun investissement");
            return;
        }

        int totalInvestments = allInvestments.size();
        BigDecimal totalAmount = allInvestments.stream()
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        summaryLabel.setText(totalInvestments + " investissement(s) • Total: " +
                currencyFormat.format(totalAmount) + " TND");
    }

    private String getStatusEmoji(String status) {
        switch (status.toLowerCase()) {
            case "completed": return "✅";
            case "pending": return "⏳";
            case "failed": return "❌";
            case "refunded": return "↩️";
            default: return "❓";
        }
    }

    private String getStatusText(String status) {
        switch (status.toLowerCase()) {
            case "completed": return "Complété";
            case "pending": return "En attente";
            case "failed": return "Échoué";
            case "refunded": return "Remboursé";
            default: return status;
        }
    }

    private String getStatusBadgeStyle(String status) {
        switch (status.toLowerCase()) {
            case "completed":
                return "-fx-background-color: #d4edda; -fx-text-fill: #155724; ";
            case "pending":
                return "-fx-background-color: #fff3cd; -fx-text-fill: #856404; ";
            case "failed":
                return "-fx-background-color: #f8d7da; -fx-text-fill: #721c24; ";
            case "refunded":
                return "-fx-background-color: #d1ecf1; -fx-text-fill: #0c5460; ";
            default:
                return "-fx-background-color: #e2e3e5; -fx-text-fill: #383d41; ";
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

