package com.bizhub.review.controller;

import com.bizhub.review.model.Review;
import com.bizhub.common.service.AppSession;
import com.bizhub.common.service.Services;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.sql.SQLException;

public class ReviewFormController {

    @FXML private Text titleText;

    @FXML private ComboBox<Integer> ratingBox;
    @FXML private TextArea commentArea;

    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private int formationId;
    private Review editing;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        ratingBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingBox.getSelectionModel().select(Integer.valueOf(5));
        errorLabel.setText("");
    }

    public void setContext(int formationId, Review existing, Runnable onSaved) {
        this.formationId = formationId;
        this.editing = existing;
        this.onSaved = onSaved;

        if (existing != null) {
            titleText.setText("Edit my review");
            if (existing.getRating() != null) ratingBox.getSelectionModel().select(existing.getRating());
            commentArea.setText(existing.getComment());
        } else {
            titleText.setText("Leave a review");
        }
    }

    @FXML
    public void save() {
        errorLabel.setText("");
        var me = AppSession.getCurrentUser();
        if (me == null) {
            errorLabel.setText("Not authenticated");
            return;
        }

        Integer rating = ratingBox.getValue();
        if (rating == null || rating < 1 || rating > 5) {
            errorLabel.setText("Rating must be between 1 and 5");
            return;
        }

        String comment = commentArea.getText();
        if (comment != null && comment.length() > 2000) {
            errorLabel.setText("Comment too long");
            return;
        }

        try {
            if (editing == null) {
                Review r = new Review();
                r.setReviewerId(me.getUserId());
                r.setFormationId(formationId);
                r.setRating(rating);
                r.setComment(comment);
                Services.reviews().create(r);
            } else {
                editing.setRating(rating);
                editing.setComment(comment);
                Services.reviews().update(editing);
            }

            if (onSaved != null) onSaved.run();
            close();
        } catch (SQLException e) {
            // unique constraint likely: reviewer_id + formation_id
            errorLabel.setText(e.getMessage());
        }
    }

    @FXML
    public void cancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}

