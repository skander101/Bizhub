package com.bizhub.controller.users_avis.formation;

import com.bizhub.model.users_avis.formation.Formation;
import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.Services;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class FormationFormController {

    @FXML private Text titleText;
    @FXML private TextField titleField;
    @FXML private ComboBox<User> trainerBox;

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField costField;
    @FXML private TextArea descriptionArea;

    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private Formation editing;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        errorLabel.setText("");
        trainerBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getFullName() + " (#" + item.getUserId() + ")"));
            }
        });
        trainerBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getFullName() + " (#" + item.getUserId() + ")"));
            }
        });

        try {
            // trainers are users with user_type='formateur'
            List<User> all = Services.users().findAll();
            List<User> trainers = all.stream().filter(u -> "formateur".equalsIgnoreCase(u.getUserType())).toList();
            trainerBox.setItems(FXCollections.observableArrayList(trainers));
        } catch (SQLException e) {
            errorLabel.setText(e.getMessage());
        }

        // sensible defaults
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));
        costField.setText("0.00");
    }

    public void setEditing(Formation f, Runnable onSaved) {
        this.editing = f;
        this.onSaved = onSaved;

        boolean isEdit = f != null && f.getFormationId() > 0;
        titleText.setText(isEdit ? "Edit formation" : "Add formation");

        if (isEdit) {
            titleField.setText(f.getTitle());
            descriptionArea.setText(f.getDescription());
            startDatePicker.setValue(f.getStartDate());
            endDatePicker.setValue(f.getEndDate());
            costField.setText(f.getCost() == null ? "0.00" : f.getCost().toPlainString());

            // select trainer if present
            for (User u : trainerBox.getItems()) {
                if (u.getUserId() == f.getTrainerId()) {
                    trainerBox.getSelectionModel().select(u);
                    break;
                }
            }
        }
    }

    @FXML
    public void save() {
        errorLabel.setText("");

        String title = titleField.getText();
        User trainer = trainerBox.getValue();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (title == null || title.isBlank()) {
            errorLabel.setText("Title is required");
            return;
        }
        if (trainer == null) {
            errorLabel.setText("Trainer is required");
            return;
        }
        if (start == null) {
            errorLabel.setText("Start date is required");
            return;
        }
        if (end == null) {
            errorLabel.setText("End date is required");
            return;
        }
        if (end.isBefore(start)) {
            errorLabel.setText("End date must be after start date");
            return;
        }

        BigDecimal cost;
        try {
            String c = costField.getText();
            if (c == null || c.isBlank()) cost = new BigDecimal("0.00");
            else cost = new BigDecimal(c.trim());
        } catch (Exception ex) {
            errorLabel.setText("Invalid cost");
            return;
        }

        try {
            boolean isEdit = editing != null && editing.getFormationId() > 0;
            editing.setTitle(title.trim());
            editing.setTrainerId(trainer.getUserId());
            editing.setStartDate(start);
            editing.setEndDate(end);
            editing.setCost(cost);
            editing.setDescription(descriptionArea.getText());

            if (!isEdit) {
                Services.formations().create(editing);
            } else {
                Services.formations().update(editing);
            }

            if (onSaved != null) onSaved.run();
            close();

        } catch (IllegalArgumentException e) {
            errorLabel.setText(e.getMessage());
        } catch (SQLException e) {
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
