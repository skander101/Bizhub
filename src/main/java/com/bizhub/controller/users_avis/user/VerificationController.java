package com.bizhub.controller.users_avis.user;

import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import com.bizhub.model.services.common.service.TotpService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

/**
 * VerificationController: Handles the two-step verification process.
 * Step 1: Email verification via Auth0
 * Step 2: TOTP verification via Authenticator App (Google Authenticator, Authy, etc.)
 */
public class VerificationController {

    // Step indicators
    @FXML private Label step1Circle;
    @FXML private Label step2Circle;
    @FXML private Label step3Circle;

    // Email section
    @FXML private VBox emailSection;
    @FXML private Text emailTitle;
    @FXML private Text emailDescription;
    @FXML private Label emailAddressLabel;
    @FXML private Button resendEmailButton;
    @FXML private Button checkEmailButton;
    @FXML private ProgressIndicator emailProgress;

    // TOTP section
    @FXML private VBox totpSection;
    @FXML private Text totpDescription;
    @FXML private ImageView qrCodeImage;
    @FXML private Label secretKeyLabel;
    @FXML private TextField pinField1;
    @FXML private TextField pinField2;
    @FXML private TextField pinField3;
    @FXML private TextField pinField4;
    @FXML private TextField pinField5;
    @FXML private TextField pinField6;
    @FXML private Button verifyTotpButton;
    @FXML private ProgressIndicator totpProgress;

    // Success section
    @FXML private VBox successSection;

    // Status
    @FXML private Label statusLabel;
    @FXML private Hyperlink skipLink;

    private User pendingUser;
    private String auth0UserId;
    private TotpService.TotpSetupResult totpSetup;
    private ScheduledExecutorService emailPollingExecutor;

    // Track verification state
    private boolean emailVerified = false;
    private boolean totpVerified = false;

    @FXML
    public void initialize() {
        statusLabel.setText("");

        // Setup PIN field auto-advance
        setupPinFields();

        // Check if we have a pending user from signup
        pendingUser = AppSession.getPendingVerificationUser();

        if (pendingUser == null) {
            statusLabel.setText("No pending verification. Please sign up first.");
            disableAllSections();
            return;
        }

        // Show email for verification
        emailAddressLabel.setText(pendingUser.getEmail());

        // Check if Auth0 is configured - show skip option if not
        if (!Services.auth0().isConfigured()) {
            skipLink.setVisible(true);
            statusLabel.setText("Email verification not configured. You can skip this step.");
            statusLabel.setStyle("-fx-text-fill: #ff9800;");
            return;
        }

        // Start email verification process
        startEmailVerification();
    }

    private void setupPinFields() {
        TextField[] pinFields = {pinField1, pinField2, pinField3, pinField4, pinField5, pinField6};

        for (int i = 0; i < pinFields.length; i++) {
            final int index = i;
            TextField field = pinFields[i];

            // Allow only single digit
            field.textProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue.length() > 1) {
                    field.setText(newValue.substring(0, 1));
                }
                // Auto-advance to next field
                if (newValue.length() == 1 && index < pinFields.length - 1) {
                    pinFields[index + 1].requestFocus();
                }
                // Auto-verify when all fields are filled
                if (index == pinFields.length - 1 && newValue.length() == 1) {
                    String pin = getEnteredPin();
                    if (pin.length() == 6) {
                        onVerifyTotp();
                    }
                }
            });

            // Handle backspace to go to previous field
            field.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("BACK_SPACE") && field.getText().isEmpty() && index > 0) {
                    pinFields[index - 1].requestFocus();
                }
            });
        }
    }

    private String getEnteredPin() {
        return pinField1.getText() + pinField2.getText() + pinField3.getText() +
               pinField4.getText() + pinField5.getText() + pinField6.getText();
    }

    private void clearPinFields() {
        pinField1.clear();
        pinField2.clear();
        pinField3.clear();
        pinField4.clear();
        pinField5.clear();
        pinField6.clear();
        pinField1.requestFocus();
    }

    private void startEmailVerification() {
        setLoading(true, "email");
        statusLabel.setText("Sending verification email...");

        CompletableFuture.runAsync(() -> {
            try {
                // Create user in Auth0 and send verification email
                auth0UserId = Services.auth0().createAuth0User(
                    pendingUser.getEmail(),
                    "TempPass123!" // Temporary password for Auth0 (not used for login)
                );

                Services.auth0().sendVerificationEmail(auth0UserId);

                Platform.runLater(() -> {
                    setLoading(false, "email");
                    statusLabel.setText("Verification email sent! Check your inbox.");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    setLoading(false, "email");
                    statusLabel.setText("Email verification unavailable: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #ff9800;");
                    // Show skip link when verification fails
                    skipLink.setVisible(true);
                });
            }
        });
    }

    @FXML
    public void onResendEmail() {
        if (auth0UserId == null) {
            startEmailVerification();
            return;
        }

        setLoading(true, "email");
        statusLabel.setText("Resending verification email...");

        CompletableFuture.runAsync(() -> {
            try {
                Services.auth0().sendVerificationEmail(auth0UserId);

                Platform.runLater(() -> {
                    setLoading(false, "email");
                    statusLabel.setText("Verification email resent!");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    setLoading(false, "email");
                    statusLabel.setText("Failed to resend email: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #f44336;");
                });
            }
        });
    }

    @FXML
    public void onCheckEmailVerification() {
        setLoading(true, "email");
        statusLabel.setText("Checking verification status...");

        CompletableFuture.supplyAsync(() -> {
            try {
                return Services.auth0().isEmailVerified(pendingUser.getEmail());
            } catch (IOException e) {
                return false;
            }
        }).thenAccept(verified -> {
            Platform.runLater(() -> {
                setLoading(false, "email");

                if (verified) {
                    emailVerified = true;
                    onEmailVerificationComplete();
                } else {
                    statusLabel.setText("Email not yet verified. Please check your inbox and click the verification link.");
                    statusLabel.setStyle("-fx-text-fill: #ff9800;");
                }
            });
        });
    }

    private void onEmailVerificationComplete() {
        // Update step indicator
        step1Circle.getStyleClass().add("step-complete");
        step2Circle.getStyleClass().add("step-active");

        // Show TOTP section, hide email section
        emailSection.setVisible(false);
        emailSection.setManaged(false);
        totpSection.setVisible(true);
        totpSection.setManaged(true);

        statusLabel.setText("");

        // Setup TOTP for user
        setupTotp();
    }

    private void setupTotp() {
        setLoading(true, "totp");
        statusLabel.setText("Setting up Two-Factor Authentication...");

        CompletableFuture.runAsync(() -> {
            // Generate TOTP secret and QR code
            totpSetup = Services.totp().setupTotp(pendingUser.getEmail());

            Platform.runLater(() -> {
                setLoading(false, "totp");
                statusLabel.setText("Scan the QR code with your authenticator app, then enter the 6-digit code.");
                statusLabel.setStyle("-fx-text-fill: #4CAF50;");

                // Display QR code from data URL
                String qrCodeDataUrl = totpSetup.getQrCodeDataUrl();
                if (qrCodeDataUrl.startsWith("data:image/png;base64,")) {
                    String base64Data = qrCodeDataUrl.substring("data:image/png;base64,".length());
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                    Image qrCode = new Image(new ByteArrayInputStream(imageBytes));
                    qrCodeImage.setImage(qrCode);
                }

                // Display secret key for manual entry
                secretKeyLabel.setText(totpSetup.getSecret());

                pinField1.requestFocus();
            });
        });
    }

    @FXML
    public void onVerifyTotp() {
        String code = getEnteredPin();

        if (code.length() != 6) {
            statusLabel.setText("Please enter the complete 6-digit code.");
            statusLabel.setStyle("-fx-text-fill: #f44336;");
            return;
        }

        if (totpSetup == null) {
            statusLabel.setText("TOTP not set up. Please wait for the QR code to load.");
            statusLabel.setStyle("-fx-text-fill: #f44336;");
            return;
        }

        setLoading(true, "totp");
        statusLabel.setText("Verifying code...");

        Services.totp().verifyCodeAsync(totpSetup.getSecret(), code)
            .thenAccept(verified -> {
                Platform.runLater(() -> {
                    setLoading(false, "totp");

                    if (verified) {
                        totpVerified = true;
                        // Store the TOTP secret for future logins
                        pendingUser.setTotpSecret(totpSetup.getSecret());
                        onAllVerificationComplete();
                    } else {
                        statusLabel.setText("Invalid code. Please check your authenticator app and try again.");
                        statusLabel.setStyle("-fx-text-fill: #f44336;");
                        clearPinFields();
                    }
                });
            });
    }

    private void onAllVerificationComplete() {
        // Update step indicators
        step2Circle.getStyleClass().add("step-complete");
        step3Circle.getStyleClass().add("step-active");
        step3Circle.getStyleClass().add("step-complete");

        // Hide other sections, show success
        emailSection.setVisible(false);
        emailSection.setManaged(false);
        totpSection.setVisible(false);
        totpSection.setManaged(false);
        successSection.setVisible(true);
        successSection.setManaged(true);

        statusLabel.setText("");

        // Save user to database now that they're verified
        try {
            Services.users().create(pendingUser);
            AppSession.setCurrentUser(pendingUser);
            AppSession.clearPendingVerificationUser();

            // Clean up Auth0 user (optional - we only needed it for email verification)
            if (auth0UserId != null) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Services.auth0().deleteAuth0User(auth0UserId);
                    } catch (IOException ignored) {}
                });
            }

        } catch (Exception e) {
            statusLabel.setText("Failed to create account: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #f44336;");
        }
    }

    @FXML
    public void onContinueToProfile() {
        Stage stage = (Stage) successSection.getScene().getWindow();
        NavigationService nav = new NavigationService(stage);
        nav.goToProfile();
    }

    @FXML
    public void onSkipVerification() {
        // Development only - skip verification and create user directly
        try {
            Services.users().create(pendingUser);
            AppSession.setCurrentUser(pendingUser);
            AppSession.clearPendingVerificationUser();

            Stage stage = (Stage) statusLabel.getScene().getWindow();
            NavigationService nav = new NavigationService(stage);
            nav.goToProfile();
        } catch (Exception e) {
            statusLabel.setText("Failed to create account: " + e.getMessage());
        }
    }

    private void setLoading(boolean loading, String section) {
        if ("email".equals(section)) {
            emailProgress.setVisible(loading);
            resendEmailButton.setDisable(loading);
            checkEmailButton.setDisable(loading);
        } else if ("totp".equals(section)) {
            totpProgress.setVisible(loading);
            verifyTotpButton.setDisable(loading);
        }
    }

    private void disableAllSections() {
        emailSection.setDisable(true);
        totpSection.setDisable(true);
    }

    public void cleanup() {
        if (emailPollingExecutor != null) {
            emailPollingExecutor.shutdownNow();
        }
    }
}
