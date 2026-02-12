package com.bizhub.formation.controller;

import com.bizhub.formation.model.Formation;
import com.bizhub.formation.service.FormationContext;
import com.bizhub.participation.controller.ParticipationFormController;
import com.bizhub.participation.model.Participation;
import com.bizhub.participation.service.ParticipationContext;
import com.bizhub.review.controller.ReviewFormController;
import com.bizhub.review.model.Review;
import com.bizhub.user.model.User;
import com.bizhub.common.service.AppSession;
import com.bizhub.common.service.NavigationService;
import com.bizhub.common.service.Services;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FormationDetailsController {

    @FXML private Text titleText;
    @FXML private Label trainerLabel, startLabel, endLabel, costLabel;
    @FXML private TextArea descArea;

    @FXML private Button leaveReviewButton;
    @FXML private Button addParticipationButton;

    @FXML private TableView<Review> reviewsTable;
    @FXML private TableColumn<Review, String> colReviewer, colRating, colComment, colDate;

    @FXML private TableView<Participation> participationsTable;
    @FXML private TableColumn<Participation, String> colPartUser, colPartDate, colPartRemarques;

    private int formationId;

    @FXML
    public void initialize() {

        // 🔥 FORCER L’EXISTENCE DU BOUTON
        Integer id = FormationContext.getSelectedFormationId();
        if (id == null) {
            disable("No formation selected");
            return;
        }
        formationId = id;

        colReviewer.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getReviewerName())));

        colRating.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getRating() == null ? "" : c.getValue().getRating().toString()));

        colComment.setCellValueFactory(c -> {
            String s = nullToEmpty(c.getValue().getComment());
            if (s.length() > 90) s = s.substring(0, 87) + "...";
            return new javafx.beans.property.SimpleStringProperty(s);
        });

        colDate.setCellValueFactory(c -> {
            var dt = c.getValue().getCreatedAt();
            return new javafx.beans.property.SimpleStringProperty(
                    dt == null ? "" : dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        });

        colPartUser.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(userNameFor(c.getValue().getUserId())));

        colPartDate.setCellValueFactory(c -> {
            var dt = c.getValue().getDateAffectation();
            return new javafx.beans.property.SimpleStringProperty(
                    dt == null ? "" : dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        });

        colPartRemarques.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getRemarques())));

        load();
    }

    private void load() {
        try {
            Formation f = Services.formations().findById(formationId).orElse(null);
            if (f == null) {
                disable("Formation not found");
                return;
            }

            titleText.setText(f.getTitle());
            trainerLabel.setText(String.valueOf(f.getTrainerId()));
            startLabel.setText(String.valueOf(f.getStartDate()));
            endLabel.setText(String.valueOf(f.getEndDate()));
            costLabel.setText(f.getCost() == null ? "0.00" : f.getCost().toPlainString());
            descArea.setText(nullToEmpty(f.getDescription()));

            reviewsTable.setItems(FXCollections.observableArrayList(
                    Services.reviews().findAllByFormationId(formationId)));

            List<Participation> parts =
                    Services.participations().findByFormationId(formationId);
            participationsTable.setItems(FXCollections.observableArrayList(parts));

            var me = AppSession.getCurrentUser();

            // Set button visibility based on login status
            if (me == null) {
                // Not logged in - hide participation button
                leaveReviewButton.setDisable(true);
                addParticipationButton.setVisible(false);
                addParticipationButton.setManaged(false);
            } else {
                // Logged in - show participation button
                addParticipationButton.setVisible(true);
                addParticipationButton.setManaged(true);
                addParticipationButton.setDisable(false);

                // REVIEW
                boolean alreadyReviewed =
                        Services.reviews().findByReviewerAndFormation(me.getUserId(), formationId).isPresent();
                leaveReviewButton.setText(alreadyReviewed ? "Edit my review" : "Leave a review");
                leaveReviewButton.setDisable(false);

                // PARTICIPATION - disable if user already participates (for non-admin users)
                if (!AppSession.isAdmin()) {
                    boolean alreadyParticipates =
                            Services.participations().findByFormationAndUser(formationId, me.getUserId()).isPresent();
                    addParticipationButton.setDisable(alreadyParticipates);
                    if (alreadyParticipates) {
                        addParticipationButton.setText("Déjà participé");
                    } else {
                        addParticipationButton.setText("Participer");
                    }
                } else {
                    // Admin can always add participations
                    addParticipationButton.setDisable(false);
                    addParticipationButton.setText("Participer");
                }
            }

        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void addParticipation() {
        var me = AppSession.getCurrentUser();
        if (me == null) return;

        if (AppSession.isAdmin()) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/bizhub/fxml/participation-form.fxml"));
                Parent root = loader.load();

                ParticipationFormController ctl = loader.getController();
                Participation p = new Participation();
                p.setFormationId(formationId);
                p.setUserId(me.getUserId());
                ctl.setEditing(p, this::load);

                Stage dialog = new Stage();
                dialog.initOwner(addParticipationButton.getScene().getWindow());
                dialog.initModality(Modality.WINDOW_MODAL);
                dialog.setScene(new Scene(root));
                dialog.showAndWait();

            } catch (Exception e) {
                showError(e.getMessage());
            }
        } else {
            try {
                Participation p = new Participation();
                p.setFormationId(formationId);
                p.setUserId(me.getUserId());
                p.setDateAffectation(java.time.LocalDateTime.now());
                Services.participations().create(p);
                info("Participation réussie");
                load();
            } catch (SQLException e) {
                showError(e.getMessage());
            }
        }
    }

    @FXML
    public void leaveReview() {
        var me = AppSession.getCurrentUser();
        if (me == null) return;

        try {
            Review existing =
                    Services.reviews().findByReviewerAndFormation(me.getUserId(), formationId).orElse(null);
            openReviewForm(existing);
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void openReviewForm(Review existing) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/com/bizhub/fxml/review-form.fxml"));
            Parent root = loader.load();

            ReviewFormController ctl = loader.getController();
            ctl.setContext(formationId, existing, this::load);

            Stage dialog = new Stage();
            dialog.initOwner(reviewsTable.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void goBack() {
        Stage stage = (Stage) reviewsTable.getScene().getWindow();
        new NavigationService(stage).goToFormations();
    }

    private void disable(String msg) {
        titleText.setText(msg);
        leaveReviewButton.setDisable(true);
        // Only hide participation button if user is not logged in
        var me = AppSession.getCurrentUser();
        if (me == null) {
            addParticipationButton.setVisible(false);
            addParticipationButton.setManaged(false);
        }
    }

    private static String userNameFor(int userId) {
        try {
            return Services.users().findById(userId)
                    .map(User::getFullName).orElse("#" + userId);
        } catch (SQLException e) {
            return "#" + userId;
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    private void info(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}
