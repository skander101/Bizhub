package com.bizhub.controller.users_avis.user;

import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * ForgotPasswordController: Handles the password reset flow.
 *
 * Flow:
 * 1. User enters email
 * 2. System generates a 6-digit code and sends it via email
 * 3. User enters the code
 * 4. User sets a new password
 */
public class ForgotPasswordController {

    // Email section
    @FXML private VBox emailSection;
    @FXML private TextField emailField;
    @FXML private Button sendCodeButton;

    // Code section
    @FXML private VBox codeSection;
    @FXML private Text codeSentText;
    @FXML private TextField codeField1;
    @FXML private TextField codeField2;
    @FXML private TextField codeField3;
    @FXML private TextField codeField4;
    @FXML private TextField codeField5;
    @FXML private TextField codeField6;
    @FXML private Button verifyCodeButton;

    // Password section
    @FXML private VBox passwordSection;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button resetPasswordButton;

    // Success section
    @FXML private VBox successSection;

    // Common
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private HBox backToLoginBox;

    // State
    private String userEmail;
    private String generatedCode;
    private User targetUser;

    @FXML
    public void initialize() {
        statusLabel.setText("");
        setupCodeFields();
    }

    private void setupCodeFields() {
        TextField[] codeFields = {codeField1, codeField2, codeField3, codeField4, codeField5, codeField6};

        for (int i = 0; i < codeFields.length; i++) {
            final int index = i;
            TextField field = codeFields[i];

            // Allow only single digit
            field.textProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue.length() > 1) {
                    field.setText(newValue.substring(0, 1));
                }
                // Auto-advance to next field
                if (newValue.length() == 1 && index < codeFields.length - 1) {
                    codeFields[index + 1].requestFocus();
                }
                // Auto-verify when all fields are filled
                if (index == codeFields.length - 1 && newValue.length() == 1) {
                    String code = getEnteredCode();
                    if (code.length() == 6) {
                        onVerifyCode();
                    }
                }
            });

            // Handle backspace to go to previous field
            field.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("BACK_SPACE") && field.getText().isEmpty() && index > 0) {
                    codeFields[index - 1].requestFocus();
                }
            });
        }
    }

    private String getEnteredCode() {
        return codeField1.getText() + codeField2.getText() + codeField3.getText() +
               codeField4.getText() + codeField5.getText() + codeField6.getText();
    }

    private void clearCodeFields() {
        codeField1.clear();
        codeField2.clear();
        codeField3.clear();
        codeField4.clear();
        codeField5.clear();
        codeField6.clear();
        codeField1.requestFocus();
    }

    @FXML
    public void onSendCode() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Please enter your email address.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Please enter a valid email address.");
            return;
        }

        setLoading(true);
        statusLabel.setText("Looking up your account...");

        CompletableFuture.runAsync(() -> {
            try {
                // Check if user exists
                Optional<User> userOpt = Services.users().findByEmail(email);

                if (userOpt.isEmpty()) {
                    Platform.runLater(() -> {
                        setLoading(false);
                        showError("No account found with this email address.");
                    });
                    return;
                }

                targetUser = userOpt.get();
                userEmail = email;


                // Generate a simple code (in production, you'd send this via email)
                generatedCode = generateCode();

                // In a real app, you'd send this via email service
                // For now, we'll show it in a development message
                Platform.runLater(() -> {
                    setLoading(false);
                    // DEV MODE: Show code in status (in production, send email)
                    showCodeSection("Check your email for the reset code.\n(Dev mode: " + generatedCode + ")");
                });

            } catch (SQLException e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Database error: " + e.getMessage());
                });
            }
        });
    }

    private void showCodeSection(String message) {
        emailSection.setVisible(false);
        emailSection.setManaged(false);
        codeSection.setVisible(true);
        codeSection.setManaged(true);
        codeSentText.setText(message);
        statusLabel.setText("");
        statusLabel.setStyle("-fx-text-fill: #4CAF50;");
        codeField1.requestFocus();
    }

    @FXML
    public void onVerifyCode() {
        String enteredCode = getEnteredCode();

        if (enteredCode.length() != 6) {
            showError("Please enter the complete 6-digit code.");
            return;
        }

        setLoading(true);
        statusLabel.setText("Verifying code...");

        CompletableFuture.supplyAsync(() -> {
            // Verify against our generated code
            return generatedCode != null && generatedCode.equals(enteredCode);
        }).thenAccept(verified -> {
            Platform.runLater(() -> {
                setLoading(false);

                if (verified) {
                    showPasswordSection();
                } else {
                    showError("Invalid code. Please try again.");
                    clearCodeFields();
                }
            });
        });
    }

    private void showPasswordSection() {
        codeSection.setVisible(false);
        codeSection.setManaged(false);
        passwordSection.setVisible(true);
        passwordSection.setManaged(true);
        statusLabel.setText("");
        newPasswordField.requestFocus();
    }

    @FXML
    public void onResendCode() {
        clearCodeFields();

        // Show email section to start over, or resend directly
        if (userEmail != null && targetUser != null) {
            setLoading(true);
            statusLabel.setText("Resending code...");

            CompletableFuture.runAsync(() -> {
                try {

                    // Generate new code for email
                    generatedCode = generateCode();
                    Platform.runLater(() -> {
                        setLoading(false);
                        showSuccess("New code sent! (Dev mode: " + generatedCode + ")");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        setLoading(false);
                        showError("Failed to resend code: " + e.getMessage());
                    });
                }
            });
        }
    }

    @FXML
    public void onResetPassword() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation
        if (newPassword.isEmpty()) {
            showError("Please enter a new password.");
            return;
        }

        if (newPassword.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        setLoading(true);
        statusLabel.setText("Updating your password...");

        CompletableFuture.runAsync(() -> {
            try {
                // Hash the new password
                String hashedPassword = Services.auth().hashPassword(newPassword);

                // Update in database
                Services.users().updatePasswordHash(targetUser.getUserId(), hashedPassword);

                Platform.runLater(() -> {
                    setLoading(false);
                    showSuccessSection();
                });

            } catch (SQLException e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Failed to update password: " + e.getMessage());
                });
            }
        });
    }

    private void showSuccessSection() {
        passwordSection.setVisible(false);
        passwordSection.setManaged(false);
        successSection.setVisible(true);
        successSection.setManaged(true);
        backToLoginBox.setVisible(false);
        backToLoginBox.setManaged(false);
        statusLabel.setText("");
    }

    @FXML
    public void onBackToLogin() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        new NavigationService(stage).goToLogin();
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisible(loading);
        sendCodeButton.setDisable(loading);
        verifyCodeButton.setDisable(loading);
        resetPasswordButton.setDisable(loading);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #EF4444;");
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #4CAF50;");
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 6-digit code
        return String.valueOf(code);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        int visibleDigits = 4;
        String visible = phone.substring(phone.length() - visibleDigits);
        String masked = "*".repeat(phone.length() - visibleDigits);
        return masked + visible;
    }
}

