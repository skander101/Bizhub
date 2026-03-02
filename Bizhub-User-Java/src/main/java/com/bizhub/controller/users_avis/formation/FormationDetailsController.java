package com.bizhub.controller.users_avis.formation;

import com.bizhub.model.users_avis.formation.Formation;
import com.bizhub.model.services.user_avis.formation.FormationContext;
import com.bizhub.controller.users_avis.review.ReviewFormController;
import com.bizhub.model.users_avis.review.Review;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
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

public class FormationDetailsController {

    @FXML private Text titleText;
    @FXML private Label trainerLabel;
    @FXML private Label startLabel;
    @FXML private Label endLabel;
    @FXML private Label costLabel;
    @FXML private TextArea descArea;

    @FXML private Button leaveReviewButton;

    @FXML private TableView<Review> reviewsTable;
    @FXML private TableColumn<Review, String> colReviewer;
    @FXML private TableColumn<Review, String> colRating;
    @FXML private TableColumn<Review, String> colComment;
    @FXML private TableColumn<Review, String> colDate;

    private int formationId;

    @FXML
    public void initialize() {
        Integer id = FormationContext.getSelectedFormationId();
        if (id == null) {
            disable("No formation selected");
            return;
        }
        formationId = id;

        colReviewer.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getReviewerName())));
        colRating.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getRating() == null ? "" : String.valueOf(c.getValue().getRating())));
        colComment.setCellValueFactory(c -> {
            String s = nullToEmpty(c.getValue().getComment());
            if (s.length() > 90) s = s.substring(0, 87) + "...";
            return new javafx.beans.property.SimpleStringProperty(s);
        });
        colDate.setCellValueFactory(c -> {
            var dt = c.getValue().getCreatedAt();
            var txt = dt == null ? "" : dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return new javafx.beans.property.SimpleStringProperty(txt);
        });

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
            startLabel.setText(f.getStartDate() == null ? "" : f.getStartDate().toString());
            endLabel.setText(f.getEndDate() == null ? "" : f.getEndDate().toString());
            costLabel.setText(f.getCost() == null ? "0.00" : f.getCost().toPlainString());
            descArea.setText(nullToEmpty(f.getDescription()));

            reviewsTable.setItems(FXCollections.observableArrayList(Services.reviews().findAllByFormationId(formationId)));

            // If user already reviewed, change button behavior to edit
            var me = AppSession.getCurrentUser();
            if (me == null) {
                leaveReviewButton.setDisable(true);
            } else {
                boolean already = Services.reviews().findByReviewerAndFormation(me.getUserId(), formationId).isPresent();
                leaveReviewButton.setText(already ? "Edit my review" : "Leave a review");
            }

        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void leaveReview() {
        var me = AppSession.getCurrentUser();
        if (me == null) {
            info("Please login first");
            return;
        }

        try {
            Review existing = Services.reviews().findByReviewerAndFormation(me.getUserId(), formationId).orElse(null);
            openReviewForm(existing);
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void openReviewForm(Review existing) {
        try {
            FXMLLoader loader = new FXMLLoader();
            Parent root = NavigationService.loadFxmlSafe("/com/bizhub/fxml/review-form.fxml");
            ReviewFormController ctl = loader.getController();
            ctl.setContext(formationId, existing, this::load);

            Stage dialog = new Stage();
            dialog.initOwner(reviewsTable.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Review");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
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
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("Operation failed");
        a.setContentText(msg);
        a.showAndWait();
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
