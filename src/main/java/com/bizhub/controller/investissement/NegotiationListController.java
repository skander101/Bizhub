package com.bizhub.controller.investissement;

import com.bizhub.model.investissement.Negotiation;
import com.bizhub.model.services.investissement.NegotiationService;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.users_avis.user.User;
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
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class NegotiationListController {

    @FXML private SidebarController sidebarController;
    @FXML private VBox negotiationsContainer;
    @FXML private Label summaryLabel;
    @FXML private Label emptyLabel;

    private NegotiationService negotiationService;
    private NumberFormat currencyFormat;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("negotiations");
        negotiationService = new NegotiationService();
        currencyFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
        currencyFormat.setMaximumFractionDigits(2);
        loadNegotiations();
    }

    private void loadNegotiations() {
        negotiationsContainer.getChildren().clear();

        User me = AppSession.getCurrentUser();
        if (me == null) {
            emptyLabel.setText("Please log in to view negotiations.");
            emptyLabel.setVisible(true);
            summaryLabel.setText("Not logged in");
            return;
        }

        try {
            List<Negotiation> negotiations = negotiationService.getByUserId(me.getUserId());

            if (negotiations.isEmpty()) {
                emptyLabel.setText("No negotiations yet. Go to Projects and click 'Negotiate' to start one.");
                emptyLabel.setVisible(true);
                summaryLabel.setText("0 negotiations");
                return;
            }

            emptyLabel.setVisible(false);
            summaryLabel.setText(negotiations.size() + " negotiation(s)");

            int delay = 0;
            for (Negotiation neg : negotiations) {
                VBox card = createNegotiationCard(neg, me.getUserId());
                card.setOpacity(0);
                negotiationsContainer.getChildren().add(card);

                FadeTransition fade = new FadeTransition(Duration.millis(350), card);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.setDelay(Duration.millis(delay));
                fade.play();

                TranslateTransition slide = new TranslateTransition(Duration.millis(350), card);
                slide.setFromY(15);
                slide.setToY(0);
                slide.setDelay(Duration.millis(delay));
                slide.play();

                delay += 70;
            }
        } catch (SQLException e) {
            emptyLabel.setText("Error loading negotiations: " + e.getMessage());
            emptyLabel.setVisible(true);
        }
    }

    private VBox createNegotiationCard(Negotiation neg, int myUserId) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: rgba(23,42,69,0.94); -fx-background-radius: 18; " +
                "-fx-border-color: " + getStatusBorderColor(neg.getStatus()) + "; -fx-border-radius: 18; " +
                "-fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 16, 0.12, 0, 8); -fx-cursor: hand;");

        VBox content = new VBox(10);
        content.setPadding(new Insets(18));

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(44, 44);
        iconBox.setMinSize(44, 44);
        iconBox.setStyle("-fx-background-color: " + getStatusBgColor(neg.getStatus()) + "; -fx-background-radius: 12;");
        Label icon = new Label(getStatusIcon(neg.getStatus()));
        icon.setStyle("-fx-font-size: 20px;");
        iconBox.getChildren().add(icon);

        VBox infoBox = new VBox(3);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label projectTitle = new Label(neg.getProjectTitle() != null ? neg.getProjectTitle() : "Project #" + neg.getProjectId());
        projectTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 15px; -fx-font-weight: 900;");
        projectTitle.setWrapText(true);

        boolean isInvestor = neg.getInvestorId() == myUserId;
        String otherParty = isInvestor
                ? "With: " + (neg.getStartupName() != null ? neg.getStartupName() : "Startup")
                : "With: " + (neg.getInvestorName() != null ? neg.getInvestorName() : "Investor");
        String myRole = isInvestor ? "You are the Investor" : "You are the Startup";

        Label partiesLabel = new Label(otherParty);
        partiesLabel.setStyle("-fx-text-fill: #E8A93A; -fx-font-size: 11px;");

        Label roleLabel = new Label(myRole);
        roleLabel.setStyle("-fx-text-fill: rgba(232,169,58,0.5); -fx-font-size: 10px;");

        infoBox.getChildren().addAll(projectTitle, partiesLabel, roleLabel);

        VBox rightBox = new VBox(4);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Label statusBadge = new Label(neg.getStatus().toUpperCase());
        statusBadge.setStyle(getStatusBadgeStyle(neg.getStatus()));

        if (neg.getProposedAmount() != null) {
            Label amountLabel = new Label(currencyFormat.format(neg.getProposedAmount()) + " EUR");
            amountLabel.setStyle("-fx-text-fill: #FDB813; -fx-font-weight: 900; -fx-font-size: 14px;");
            rightBox.getChildren().add(amountLabel);
        }

        if (neg.getCreatedAt() != null) {
            Label dateLabel = new Label(neg.getCreatedAt().format(DATE_FMT));
            dateLabel.setStyle("-fx-text-fill: rgba(232,169,58,0.4); -fx-font-size: 10px;");
            rightBox.getChildren().add(dateLabel);
        }

        rightBox.getChildren().add(statusBadge);

        topRow.getChildren().addAll(iconBox, infoBox, rightBox);
        content.getChildren().add(topRow);

        Region divider = new Region();
        divider.setStyle("-fx-background-color: rgba(232,169,58,0.10); -fx-pref-height: 1; -fx-max-height: 1;");

        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(10, 18, 12, 18));
        actionRow.setStyle("-fx-background-color: rgba(10,25,47,0.40); -fx-background-radius: 0 0 18 18;");

        Button openBtn = new Button("Open Chat");
        openBtn.setStyle("-fx-background-color: rgba(232,169,58,0.15); -fx-text-fill: #E8A93A; " +
                "-fx-font-weight: 800; -fx-background-radius: 10; -fx-padding: 6 16; -fx-font-size: 11px; " +
                "-fx-cursor: hand; -fx-border-color: rgba(232,169,58,0.30); -fx-border-radius: 10; -fx-border-width: 1;");
        openBtn.setOnAction(e -> openNegotiation(neg.getNegotiationId()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label idLabel = new Label("#" + neg.getNegotiationId());
        idLabel.setStyle("-fx-text-fill: rgba(232,169,58,0.3); -fx-font-size: 10px;");

        actionRow.getChildren().addAll(openBtn, spacer, idLabel);

        card.getChildren().addAll(content, divider, actionRow);

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) openNegotiation(neg.getNegotiationId());
        });

        return card;
    }

    private void openNegotiation(int negotiationId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/NegotiationView.fxml"));
            Parent root = loader.load();
            NegotiationController ctrl = loader.getController();
            ctrl.loadNegotiation(negotiationId);

            Stage stage = (Stage) negotiationsContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Negotiation Chat");
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Navigation error: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleRefresh() {
        loadNegotiations();
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/ProjectsListView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) negotiationsContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private String getStatusBorderColor(String status) {
        return switch (status) {
            case "accepted" -> "rgba(16,185,129,0.40)";
            case "rejected" -> "rgba(239,68,68,0.40)";
            case "expired" -> "rgba(107,114,128,0.40)";
            default -> "rgba(232,169,58,0.25)";
        };
    }

    private String getStatusBgColor(String status) {
        return switch (status) {
            case "accepted" -> "rgba(16,185,129,0.15)";
            case "rejected" -> "rgba(239,68,68,0.15)";
            case "expired" -> "rgba(107,114,128,0.15)";
            default -> "rgba(232,169,58,0.15)";
        };
    }

    private String getStatusIcon(String status) {
        return switch (status) {
            case "accepted" -> "\u2705";
            case "rejected" -> "\u274C";
            case "expired" -> "\u23F3";
            default -> "\uD83E\uDD1D";
        };
    }

    private String getStatusBadgeStyle(String status) {
        String color = switch (status) {
            case "accepted" -> "#10B981";
            case "rejected" -> "#EF4444";
            case "expired" -> "#6B7280";
            default -> "#E8A93A";
        };
        return "-fx-background-color: " + color + "22; -fx-text-fill: " + color +
                "; -fx-background-radius: 999; -fx-padding: 2 10; -fx-font-size: 10px; -fx-font-weight: 800;";
    }
}
