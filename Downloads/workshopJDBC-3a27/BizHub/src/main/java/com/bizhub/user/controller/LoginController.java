package com.bizhub.user.controller;

import com.bizhub.user.model.User;
import com.bizhub.common.service.AppSession;
import com.bizhub.common.service.AuthService;
import com.bizhub.common.service.NavigationService;
import com.bizhub.common.service.Services;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Simple login controller.
 *
 * This version reuses the existing utils.MyDatabase singleton.
 */
public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    private AuthService authService;

    @FXML
    public void initialize() {
        errorLabel.setText("");

        // Basic UX: press Enter in password field triggers login
        passwordField.setOnAction(e -> {
            try {
                onLogin(new ActionEvent());
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        try {
            // Ensure DB is initialized
            if (Services.cnx() == null) {
                showError("DB connection failed: connection is null");
                loginButton.setDisable(true);
                return;
            }
            authService = Services.auth();
        } catch (Exception e) {
            showError("DB init failed: " + e.getMessage());
            loginButton.setDisable(true);
        }
    }

    @FXML
    public void onLogin(ActionEvent event) {
        clearError();

        try {
            String email = emailField.getText();
            String password = passwordField.getText();

            Optional<User> loggedIn = authService.login(email, password);
            if (loggedIn.isEmpty()) {
                showError("Invalid credentials or inactive user.");
                return;
            }

            User u = loggedIn.get();
            AppSession.setCurrentUser(u);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            NavigationService nav = new NavigationService(stage);

            if ("admin".equalsIgnoreCase(u.getUserType())) {
                nav.goToAdminDashboard();
            } else {
                nav.goToProfile();
            }

        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void goToSignup() {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        new NavigationService(stage).goToSignup();
    }

    private void showError(String msg) {
        errorLabel.setText(msg == null ? "" : msg);
    }

    private void clearError() {
        errorLabel.setText("");
    }
}
