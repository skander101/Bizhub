package com.bizhub.controller.users_avis.user;

import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.Services;
import com.bizhub.model.services.common.service.ValidationService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class UserFormController {

    @FXML private Text titleText;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullNameField;
    @FXML private ComboBox<String> typeBox;
    @FXML private TextField phoneField;
    @FXML private TextField companyField;
    @FXML private TextField sectorField;
    @FXML private CheckBox activeCheck;

    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private User editing;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        typeBox.setItems(FXCollections.observableArrayList("startup", "fournisseur", "formateur", "investisseur", "admin"));
        errorLabel.setText("");
    }

    public void setEditing(User u, Runnable onSaved) {
        this.editing = u;
        this.onSaved = onSaved;

        boolean isEdit = u != null && u.getUserId() > 0;
        titleText.setText(isEdit ? "Edit user" : "Add user");

        if (isEdit) {
            emailField.setText(u.getEmail());
            emailField.setEditable(false);
            passwordField.setPromptText("Leave empty to keep current");
            fullNameField.setText(u.getFullName());
            typeBox.getSelectionModel().select(u.getUserType());
            phoneField.setText(u.getPhone());
            companyField.setText(u.getCompanyName());
            sectorField.setText(u.getSector());
            activeCheck.setSelected(u.isActive());
        } else {
            activeCheck.setSelected(true);
            typeBox.getSelectionModel().select("startup");
        }
    }

    @FXML
    public void save() {
        errorLabel.setText("");
        boolean isEdit = editing != null && editing.getUserId() > 0;

        String email = emailField.getText();
        String pass = passwordField.getText();
        String fullName = fullNameField.getText();
        String type = typeBox.getValue();

        ValidationService vs = Services.validation();
        List<String> errors = isEdit ? vs.validateUserUpdate(fullName, type) : vs.validateUserCreate(email, pass, fullName, type);
        if (!errors.isEmpty()) {
            errorLabel.setText(String.join("\n", errors));
            return;
        }

        try {
            if (!isEdit) {
                User u = new User();
                u.setEmail(email.trim());
                u.setPasswordHash(Services.auth().hashPassword(pass));
                u.setUserType(type);
                u.setFullName(fullName);
                u.setPhone(phoneField.getText());
                u.setCompanyName(companyField.getText());
                u.setSector(sectorField.getText());
                u.setActive(activeCheck.isSelected());

                Services.users().create(u);
            } else {
                editing.setFullName(fullName);
                editing.setUserType(type);
                editing.setPhone(phoneField.getText());
                editing.setCompanyName(companyField.getText());
                editing.setSector(sectorField.getText());
                editing.setActive(activeCheck.isSelected());
                Services.users().update(editing);

                if (pass != null && !pass.isBlank()) {
                    if (pass.length() < 6) {
                        errorLabel.setText("Mot de passe: minimum 6 caractères");
                        return;
                    }
                    Services.users().updatePasswordHash(editing.getUserId(), Services.auth().hashPassword(pass));
                }
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
