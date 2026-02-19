package com.bizhub.controller.users_avis.user;

import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.AuthService;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Login controller with TOTP (2FA) support.
 */
public class LoginController {

    // Login section
    @FXML private VBox loginSection;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;

    // TOTP section
    @FXML private VBox totpSection;
    @FXML private TextField totpField1;
    @FXML private TextField totpField2;
    @FXML private TextField totpField3;
    @FXML private TextField totpField4;
    @FXML private TextField totpField5;
    @FXML private TextField totpField6;
    @FXML private Button verifyTotpButton;
    @FXML private ProgressIndicator totpProgress;

    @FXML private Label errorLabel;

    private AuthService authService;
    private User pendingUser; // User awaiting TOTP verification

    @FXML
    public void initialize() {
        errorLabel.setText("");

        // Setup TOTP fields auto-advance
        setupTotpFields();

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

    private void setupTotpFields() {
        TextField[] totpFields = {totpField1, totpField2, totpField3, totpField4, totpField5, totpField6};

        for (int i = 0; i < totpFields.length; i++) {
            final int index = i;
            TextField field = totpFields[i];

            // Allow only single digit
            field.textProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue.length() > 1) {
                    field.setText(newValue.substring(0, 1));
                }
                // Auto-advance to next field
                if (newValue.length() == 1 && index < totpFields.length - 1) {
                    totpFields[index + 1].requestFocus();
                }
                // Auto-verify when all fields are filled
                if (index == totpFields.length - 1 && newValue.length() == 1) {
                    String code = getTotpCode();
                    if (code.length() == 6) {
                        onVerifyTotp();
                    }
                }
            });

            // Handle backspace to go to previous field
            field.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("BACK_SPACE") && field.getText().isEmpty() && index > 0) {
                    totpFields[index - 1].requestFocus();
                }
            });
        }
    }

    private String getTotpCode() {
        return totpField1.getText() + totpField2.getText() + totpField3.getText() +
               totpField4.getText() + totpField5.getText() + totpField6.getText();
    }

    private void clearTotpFields() {
        totpField1.clear();
        totpField2.clear();
        totpField3.clear();
        totpField4.clear();
        totpField5.clear();
        totpField6.clear();
        totpField1.requestFocus();
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

            // Check if user has TOTP enabled
            if (u.getTotpSecret() != null && !u.getTotpSecret().isBlank()) {
                // User has 2FA - show TOTP verification
                pendingUser = u;
                showTotpSection();
            } else {
                // No 2FA - complete login directly
                completeLogin(u);
            }

        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void showTotpSection() {
        loginSection.setVisible(false);
        loginSection.setManaged(false);
        totpSection.setVisible(true);
        totpSection.setManaged(true);
        clearTotpFields();
        clearError();
    }

    @FXML
    public void onVerifyTotp() {
        String code = getTotpCode();

        if (code.length() != 6) {
            showError("Please enter the complete 6-digit code.");
            return;
        }

        if (pendingUser == null) {
            showError("Session expired. Please login again.");
            onBackToLogin();
            return;
        }

        setTotpLoading(true);
        clearError();

        Services.totp().verifyCodeAsync(pendingUser.getTotpSecret(), code)
            .thenAccept(verified -> {
                Platform.runLater(() -> {
                    setTotpLoading(false);

                    if (verified) {
                        completeLogin(pendingUser);
                    } else {
                        showError("Invalid code. Please try again.");
                        clearTotpFields();
                    }
                });
            });
    }

    @FXML
    public void onBackToLogin() {
        pendingUser = null;
        totpSection.setVisible(false);
        totpSection.setManaged(false);
        loginSection.setVisible(true);
        loginSection.setManaged(true);
        clearError();
        passwordField.clear();
    }

    private void completeLogin(User u) {
        AppSession.setCurrentUser(u);

        Stage stage = (Stage) loginButton.getScene().getWindow();
        NavigationService nav = new NavigationService(stage);

        if ("admin".equalsIgnoreCase(u.getUserType())) {
            nav.goToAdminDashboard();
        } else {
            nav.goToProfile();
        }
    }

    private void setTotpLoading(boolean loading) {
        totpProgress.setVisible(loading);
        verifyTotpButton.setDisable(loading);
    }

    @FXML
    public void goToSignup() {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        new NavigationService(stage).goToSignup();
    }

    @FXML
    public void goToForgotPassword() {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        new NavigationService(stage).goToForgotPassword();
    }

    private void showError(String msg) {
        errorLabel.setText(msg == null ? "" : msg);
    }

    private void clearError() {
        errorLabel.setText("");
    }
}
