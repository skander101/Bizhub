package com.bizhub.controller.users_avis.user;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.AuthService;
import com.bizhub.model.services.common.service.FacePlusPlusService;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import com.bizhub.model.services.user_avis.user.UserService;
import com.bizhub.model.users_avis.user.User;
import com.bizhub.service.WebcamService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * Login controller with dual paths:
 * Path 1: Standard email+password+2FA/TOTP
 * Path 2: Face-only login (scan face, find matching user with >85% confidence)
 */
public class LoginController {

    // Path selection
    @FXML private VBox pathSelectionSection;

    // Standard login section
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

    // Face verification section (for after standard login)
    @FXML private VBox faceVerificationSection;
    @FXML private ImageView webcamPreview;
    @FXML private Button captureFaceButton;
    @FXML private ProgressIndicator faceProgress;
    @FXML private Label faceStatusLabel;
    @FXML private Rectangle faceBoundingBox;

    // Face login section (standalone path 2)
    @FXML private VBox faceLoginSection;
    @FXML private ImageView faceLoginWebcamPreview;
    @FXML private Button scanFaceButton;
    @FXML private ProgressIndicator faceLoginProgress;
    @FXML private Label faceLoginStatusLabel;
    @FXML private Rectangle faceLoginBoundingBox;

    @FXML private Label errorLabel;

    private AuthService authService;
    private UserService userService;
    private User pendingUser; // User awaiting 2FA/face verification in Path 1
    private WebcamService webcamService;
    private FacePlusPlusService faceService;

    @FXML
    public void initialize() {
        errorLabel.setText("");

        // Basic UX: press Enter in password field triggers login
        passwordField.setOnAction(e -> onLogin(new ActionEvent()));

        // Setup TOTP fields auto-advance
        setupTotpFields();

        try {
            if (Services.cnx() == null) {
                showError("DB connection failed: connection is null");
                loginButton.setDisable(true);
                scanFaceButton.setDisable(true);
                return;
            }
            authService = Services.auth();
            userService = new UserService();
            webcamService = new WebcamService();
            faceService = new FacePlusPlusService();
        } catch (Exception e) {
            showError("DB init failed: " + e.getMessage());
            loginButton.setDisable(true);
        }
    }

    // ==================== PATH 1: Standard Email+Password+2FA ====================

    @FXML
    public void onStandardLoginPath() {
        showSection(loginSection);
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
                pendingUser = u;
                showTotpSection();
            } else {
                completeLogin(u);
            }

        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void showTotpSection() {
        showSection(totpSection);
        clearTotpFields();
        totpField1.requestFocus();
    }

    private void setupTotpFields() {
        TextField[] totpFields = {totpField1, totpField2, totpField3, totpField4, totpField5, totpField6};

        for (int i = 0; i < totpFields.length; i++) {
            final int index = i;
            TextField field = totpFields[i];

            field.textProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue.length() > 1) {
                    field.setText(newValue.substring(0, 1));
                }
                if (newValue.length() == 1 && index < totpFields.length - 1) {
                    totpFields[index + 1].requestFocus();
                }
                if (index == totpFields.length - 1 && newValue.length() == 1) {
                    String code = getTotpCode();
                    if (code.length() == 6) {
                        onVerifyTotp();
                    }
                }
            });

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
        totpField1.clear(); totpField2.clear(); totpField3.clear();
        totpField4.clear(); totpField5.clear(); totpField6.clear();
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
            onBackToStandardLogin();
            return;
        }

        setTotpLoading(true);
        clearError();

        Services.totp().verifyCodeAsync(pendingUser.getTotpSecret(), code)
            .thenAccept(verified -> Platform.runLater(() -> {
                setTotpLoading(false);
                if (verified) {
                    completeLogin(pendingUser);
                } else {
                    showError("Invalid code. Please try again.");
                    clearTotpFields();
                }
            }));
    }

    private void setTotpLoading(boolean loading) {
        totpProgress.setVisible(loading);
        verifyTotpButton.setDisable(loading);
    }

    @FXML
    public void onBackToStandardLogin() {
        pendingUser = null;
        showSection(loginSection);
        clearError();
        passwordField.clear();
    }

    // ==================== PATH 2: Face-Only Login ====================

    @FXML
    public void onFaceLoginPath() {
        showSection(faceLoginSection);
        faceLoginStatusLabel.setText("Position your face in the camera and click Scan");
        faceLoginStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
        webcamService.start(faceLoginWebcamPreview);
    }

    @FXML
    public void onScanFaceForLogin() {
        setFaceLoginLoading(true);
        faceLoginStatusLabel.setText("Scanning face and searching for match...");
        faceLoginStatusLabel.setStyle("-fx-text-fill: #2196F3;");
        clearError();

        // Capture current frame
        Image capturedImage = webcamService.captureSnapshot();
        if (capturedImage == null) {
            setFaceLoginLoading(false);
            showError("Failed to capture image from camera.");
            faceLoginStatusLabel.setText("Camera capture failed. Please try again.");
            faceLoginStatusLabel.setStyle("-fx-text-fill: #f44336;");
            return;
        }

        // Start face matching task
        Task<Optional<User>> matchTask = createFaceMatchTask(capturedImage);

        matchTask.setOnSucceeded(e -> Platform.runLater(() -> {
            setFaceLoginLoading(false);
            Optional<User> matchedUser = matchTask.getValue();

            if (matchedUser.isPresent()) {
                User user = matchedUser.get();
                faceLoginStatusLabel.setText("Match found! Logging in as " + user.getFullName());
                faceLoginStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
                webcamService.stop();

                Platform.runLater(() -> {
                    try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                    completeLogin(user);
                });
            } else {
                faceLoginStatusLabel.setText("No matching face found. Please try again.");
                faceLoginStatusLabel.setStyle("-fx-text-fill: #f44336;");
                showError("No face match found with >85% confidence.");
            }
        }));

        matchTask.setOnFailed(e -> Platform.runLater(() -> {
            setFaceLoginLoading(false);
            String errorMsg = matchTask.getException() != null
                ? matchTask.getException().getMessage()
                : "Unknown error";
            showError("Face login error: " + errorMsg);
            faceLoginStatusLabel.setText("Face login failed. Please try again.");
            faceLoginStatusLabel.setStyle("-fx-text-fill: #f44336;");
        }));

        new Thread(matchTask).start();
    }

    private Task<Optional<User>> createFaceMatchTask(Image capturedImage) {
        return new Task<>() {
            @Override
            protected Optional<User> call() throws Exception {
                List<User> usersWithFaceTokens = userService.findUsersWithFaceTokens();

                if (usersWithFaceTokens.isEmpty()) {
                    return Optional.empty();
                }

                User bestMatch = null;
                double bestConfidence = 0;
                double threshold = 85.0; // 85% confidence threshold

                for (User user : usersWithFaceTokens) {
                    String faceToken = user.getFaceToken();
                    if (faceToken == null || faceToken.isBlank()) continue;

                    try {
                        double confidence = faceService.compareWithFaceToken(faceToken, capturedImage);
                        if (confidence > bestConfidence) {
                            bestConfidence = confidence;
                            bestMatch = user;
                        }
                    } catch (Exception e) {
                        // Skip this user on error
                    }
                }

                if (bestMatch != null && bestConfidence >= threshold) {
                    return Optional.of(bestMatch);
                }
                return Optional.empty();
            }
        };
    }

    private void setFaceLoginLoading(boolean loading) {
        faceLoginProgress.setVisible(loading);
        scanFaceButton.setDisable(loading);
    }

    // ==================== PATH 1B: Face Verification after Standard Login ====================

    @FXML
    public void onVerifyFace() {
        // This is used if you want face verification after email/password (alternative to TOTP)
        if (pendingUser == null) {
            showError("Session expired. Please login again.");
            onBackToLogin();
            return;
        }

        String faceToken = pendingUser.getFaceToken();
        if (faceToken == null || faceToken.isBlank()) {
            showError("No face reference found.");
            return;
        }

        setFaceLoading(true);
        faceStatusLabel.setText("Verifying face...");

        Image currentFrame = webcamService.captureSnapshot();
        if (currentFrame == null) {
            setFaceLoading(false);
            showError("Failed to capture image.");
            return;
        }

        Task<Double> compareTask = faceService.compareWithFaceTokenTask(faceToken, currentFrame);

        compareTask.setOnSucceeded(e -> Platform.runLater(() -> {
            setFaceLoading(false);
            double confidence = compareTask.getValue();
            double threshold = 70.0;

            if (confidence >= threshold) {
                faceStatusLabel.setText("Verified! Confidence: " + String.format("%.1f", confidence) + "%");
                faceStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
                webcamService.stop();
                Platform.runLater(() -> {
                    try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                    completeLogin(pendingUser);
                });
            } else {
                faceStatusLabel.setText("Mismatch. Confidence: " + String.format("%.1f", confidence) + "%");
                faceStatusLabel.setStyle("-fx-text-fill: #f44336;");
                showError("Face verification failed.");
            }
        }));

        compareTask.setOnFailed(e -> Platform.runLater(() -> {
            setFaceLoading(false);
            showError("Verification error: " + compareTask.getException().getMessage());
        }));

        new Thread(compareTask).start();
    }

    @FXML
    public void onBackToLogin() {
        pendingUser = null;
        webcamService.stop();
        showSection(loginSection);
        clearError();
    }

    private void setFaceLoading(boolean loading) {
        faceProgress.setVisible(loading);
        captureFaceButton.setDisable(loading);
    }

    // ==================== Common Methods ====================

    @FXML
    public void onBackToPathSelection() {
        pendingUser = null;
        webcamService.stop();
        clearError();
        showSection(pathSelectionSection);
    }

    private void showSection(VBox section) {
        // Hide all sections
        pathSelectionSection.setVisible(false);
        pathSelectionSection.setManaged(false);
        loginSection.setVisible(false);
        loginSection.setManaged(false);
        totpSection.setVisible(false);
        totpSection.setManaged(false);
        faceVerificationSection.setVisible(false);
        faceVerificationSection.setManaged(false);
        faceLoginSection.setVisible(false);
        faceLoginSection.setManaged(false);

        // Show requested section
        section.setVisible(true);
        section.setManaged(true);
    }

    private void completeLogin(User u) {
        AppSession.setCurrentUser(u);
        Stage stage = (Stage) errorLabel.getScene().getWindow();
        NavigationService nav = new NavigationService(stage);

        if ("admin".equalsIgnoreCase(u.getUserType())) {
            nav.goToAdminDashboard();
        } else {
            nav.goToProfile();
        }
    }

    @FXML
    public void goToSignup() {
        webcamService.stop();
        Stage stage = (Stage) errorLabel.getScene().getWindow();
        new NavigationService(stage).goToSignup();
    }


    private void showError(String msg) {
        errorLabel.setText(msg == null ? "" : msg);
    }

    private void clearError() {
        errorLabel.setText("");
    }
}
