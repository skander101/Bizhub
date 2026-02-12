package com.bizhub.participation.controller;

import com.bizhub.formation.model.Formation;
import com.bizhub.participation.model.Participation;
import com.bizhub.participation.service.ParticipationContext;
import com.bizhub.user.model.User;
import com.bizhub.common.service.Services;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ParticipationFormController {

    @FXML private Text titleText;
    @FXML private ComboBox<Formation> formationBox;
    @FXML private ComboBox<User> userBox;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private TextArea remarquesArea;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private Participation editing;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        errorLabel.setText("");

        formationBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Formation item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getTitle() + " (#" + item.getFormationId() + ")"));
            }
        });
        formationBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Formation item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getTitle() + " (#" + item.getFormationId() + ")"));
            }
        });

        userBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getFullName() + " (#" + item.getUserId() + ")"));
            }
        });
        userBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getFullName() + " (#" + item.getUserId() + ")"));
            }
        });

        try {
            List<Formation> formations = Services.formations().findAll();
            formationBox.setItems(FXCollections.observableArrayList(formations));

            List<User> users = Services.users().findAll();
            userBox.setItems(FXCollections.observableArrayList(users));
        } catch (SQLException e) {
            errorLabel.setText(e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        datePicker.setValue(now.toLocalDate());
        timeField.setText(now.format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    public void setEditing(Participation p, Runnable onSaved) {
        this.editing = p;
        this.onSaved = onSaved;

        boolean isEdit = p != null && p.getId() > 0;
        titleText.setText(isEdit ? "Modifier la participation" : "Nouvelle participation");

        if (isEdit) {
            formationBox.getSelectionModel().clearSelection();
            for (Formation f : formationBox.getItems()) {
                if (f.getFormationId() == p.getFormationId()) {
                    formationBox.getSelectionModel().select(f);
                    break;
                }
            }
            userBox.getSelectionModel().clearSelection();
            for (User u : userBox.getItems()) {
                if (u.getUserId() == p.getUserId()) {
                    userBox.getSelectionModel().select(u);
                    break;
                }
            }
            if (p.getDateAffectation() != null) {
                datePicker.setValue(p.getDateAffectation().toLocalDate());
                timeField.setText(p.getDateAffectation().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            remarquesArea.setText(p.getRemarques());
        } else {
            Integer preSelectedFormation = ParticipationContext.getFormationIdForParticipation();
            if (preSelectedFormation != null) {
                for (Formation f : formationBox.getItems()) {
                    if (f.getFormationId() == preSelectedFormation) {
                        formationBox.getSelectionModel().select(f);
                        break;
                    }
                }
            }
            if (p.getUserId() > 0) {
                for (User u : userBox.getItems()) {
                    if (u.getUserId() == p.getUserId()) {
                        userBox.getSelectionModel().select(u);
                        break;
                    }
                }
            }
        }
    }

    @FXML
    public void save() {
        errorLabel.setText("");

        Formation formation = formationBox.getValue();
        User user = userBox.getValue();
        LocalDate date = datePicker.getValue();
        String timeStr = timeField.getText() == null ? "" : timeField.getText().trim();

        if (formation == null) {
            errorLabel.setText("Veuillez sélectionner une formation.");
            return;
        }
        if (user == null) {
            errorLabel.setText("Veuillez sélectionner un participant.");
            return;
        }
        if (date == null) {
            errorLabel.setText("La date est requise.");
            return;
        }

        LocalTime time = LocalTime.now();
        if (!timeStr.isEmpty()) {
            try {
                time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            } catch (Exception e) {
                errorLabel.setText("Heure invalide (utilisez HH:mm).");
                return;
            }
        }
        LocalDateTime dateAffectation = LocalDateTime.of(date, time);

        try {
            boolean isEdit = editing != null && editing.getId() > 0;
            if (editing == null) editing = new Participation();

            editing.setFormationId(formation.getFormationId());
            editing.setUserId(user.getUserId());
            editing.setDateAffectation(dateAffectation);
            editing.setRemarques(remarquesArea.getText());

            if (!isEdit) {
                Services.participations().create(editing);
            } else {
                Services.participations().update(editing);
            }

            if (onSaved != null) onSaved.run();
            close();
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
