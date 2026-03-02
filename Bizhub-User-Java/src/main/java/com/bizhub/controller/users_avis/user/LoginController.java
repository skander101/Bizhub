package com.bizhub.controller.users_avis.user;

import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.AuthService;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private volatile AuthService authService;

    @FXML
    public void initialize() {
        clearError();

        // UX: Enter dans password => login
        if (passwordField != null) {
            passwordField.setOnAction(e -> onLogin(new ActionEvent()));
        }

        // Désactiver le login tant que les services ne sont pas prêts
        setBusy(true, "Initialisation...");

        // ✅ Initialiser DB + AuthService en arrière-plan
        Task<AuthService> initTask = new Task<>() {
            @Override
            protected AuthService call() {
                // ⚠️ Tout ce qui peut bloquer (JDBC) doit être ici
                if (Services.cnx() == null) {
                    throw new IllegalStateException("DB connection failed: connection is null");
                }
                return Services.auth();
            }
        };

        initTask.setOnSucceeded(e -> {
            authService = initTask.getValue();
            setBusy(false, "");
            LOGGER.info("✅ AuthService prêt");
        });

        initTask.setOnFailed(e -> {
            Throwable ex = initTask.getException();
            LOGGER.log(Level.SEVERE, "❌ Init services failed", ex);
            setBusy(true, ""); // laisse désactivé
            showError("DB init failed: " + (ex == null ? "unknown" : ex.getMessage()));
        });

        Thread t = new Thread(initTask, "login-init-task");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void onLogin(ActionEvent event) {
        clearError();

        // Si init pas fini
        if (authService == null) {
            showError("Services en cours d'initialisation... réessayez.");
            return;
        }

        String email = emailField == null ? "" : emailField.getText().trim();
        String password = passwordField == null ? "" : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez saisir email et mot de passe.");
            return;
        }

        setBusy(true, "Connexion...");

        // ✅ Login en arrière-plan (DB + bcrypt)
        Task<Optional<User>> loginTask = new Task<>() {
            @Override
            protected Optional<User> call() throws Exception {
                return authService.login(email, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            setBusy(false, "");

            Optional<User> loggedIn = loginTask.getValue();
            if (loggedIn == null || loggedIn.isEmpty()) {
                showError("Invalid credentials or inactive user.");
                return;
            }

            User u = loggedIn.get();
            AppSession.setCurrentUser(u);

            Stage stage = getStage();
            if (stage == null) {
                showError("Fenêtre introuvable.");
                return;
            }

            NavigationService nav = new NavigationService(stage);
            if ("admin".equalsIgnoreCase(u.getUserType())) {
                nav.goToAdminDashboard();
            } else {
                nav.goToProfile();
            }
        });

        loginTask.setOnFailed(e -> {
            setBusy(false, "");
            Throwable ex = loginTask.getException();

            if (ex instanceof SQLException) {
                showError(ex.getMessage());
            } else {
                LOGGER.log(Level.SEVERE, "❌ Login failed", ex);
                showError("Erreur login: " + (ex == null ? "unknown" : ex.getMessage()));
            }
        });

        Thread t = new Thread(loginTask, "login-auth-task");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void goToSignup() {
        Stage stage = getStage();
        if (stage == null) return;
        new NavigationService(stage).goToSignup();
    }

    // ================= helpers =================

    private Stage getStage() {
        if (loginButton != null && loginButton.getScene() != null) {
            return (Stage) loginButton.getScene().getWindow();
        }
        if (emailField != null && emailField.getScene() != null) {
            return (Stage) emailField.getScene().getWindow();
        }
        return null;
    }

    private void setBusy(boolean busy, String msg) {
        // ⚠️ toujours UI thread
        Platform.runLater(() -> {
            if (loginButton != null) loginButton.setDisable(busy);
            if (emailField != null) emailField.setDisable(busy && authService == null); // pendant init
            if (passwordField != null) passwordField.setDisable(busy && authService == null);

            if (msg != null && !msg.isBlank()) showError(msg);
            else if (busy == false) clearError();
        });
    }

    private void showError(String msg) {
        if (errorLabel != null) errorLabel.setText(msg == null ? "" : msg);
    }

    private void clearError() {
        if (errorLabel != null) errorLabel.setText("");
    }
}