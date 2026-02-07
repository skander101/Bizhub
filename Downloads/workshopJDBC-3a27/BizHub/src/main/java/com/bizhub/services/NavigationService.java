package com.bizhub.services;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

public class NavigationService {

    public enum ActiveNav {
        DASHBOARD,
        USERS,
        FORMATIONS,
        REVIEWS,
        PROFILE
    }

    public static void setActiveNav(Node sidebarRoot, ActiveNav activeNav) {
        if (sidebarRoot == null || activeNav == null) return;

        for (Node n : sidebarRoot.lookupAll(".nav-button")) {
            n.getStyleClass().remove("active");

            if (!(n instanceof Button b)) continue;
            String t = b.getText() == null ? "" : b.getText().toLowerCase();

            boolean match = switch (activeNav) {
                case DASHBOARD -> t.contains("dashboard");
                case USERS -> t.equals("users") || t.contains("users");
                case FORMATIONS -> t.contains("formations");
                case REVIEWS -> t.contains("reviews");
                case PROFILE -> t.contains("profile");
            };

            if (match) {
                n.getStyleClass().add("active");
            }
        }
    }

    private static final Duration OVERLAY_FADE_IN = Duration.millis(260);
    private static final Duration OVERLAY_FADE_OUT = Duration.millis(360);
    private static final Duration MIN_OVERLAY_VISIBLE = Duration.millis(420);

    private final Stage stage;

    private boolean navigating = false;

    public NavigationService(Stage stage) {
        this.stage = stage;
    }

    public void goToLogin() {
        loadIntoStage("/com/bizhub/fxml/login.fxml", 980, 640);
    }

    public void goToAdminDashboard() {
        loadIntoStage("/com/bizhub/fxml/admin-dashboard.fxml", 1200, 760);
    }

    public void goToUserManagement() {
        loadIntoStage("/com/bizhub/fxml/user-management.fxml", 1200, 760);
    }

    public void goToReviews() {
        loadIntoStage("/com/bizhub/fxml/reviews-list.fxml", 1200, 760);
    }

    public void goToProfile() {
        loadIntoStage("/com/bizhub/fxml/user-profile.fxml", 1000, 700);
    }

    public void goToSignup() {
        loadIntoStage("/com/bizhub/fxml/signup.fxml", 980, 700);
    }

    public void goToFormations() {
        loadIntoStage("/com/bizhub/fxml/formations.fxml", 1200, 760);
    }

    public void goToFormationDetails() {
        loadIntoStage("/com/bizhub/fxml/formation-details.fxml", 1200, 760);
    }

    private void loadIntoStage(String fxmlPath, double w, double h) {
        if (navigating) return;
        navigating = true;

        URL res = NavigationService.class.getResource(fxmlPath);
        if (res == null) {
            navigating = false;
            throw new IllegalStateException("Missing FXML: " + fxmlPath);
        }

        Scene scene = stage.getScene();
        if (scene == null) {
            // Fallback: initial scene. (App.start normally creates the shell.)
            Parent first = loadFxml(res);
            Scene newScene = new Scene(first, w, h);
            stage.setScene(newScene);
            stage.show();
            navigating = false;
            return;
        }

        Parent shellRoot = (Parent) scene.getRoot();
        Node overlay = findOverlay(shellRoot);

        // If overlay is missing (e.g., App shell not used), just swap immediately.
        if (overlay == null) {
            Platform.runLater(() -> {
                try {
                    Parent nextRoot = loadFxml(res);
                    if (shellRoot instanceof StackPane sp) {
                        sp.getChildren().setAll(nextRoot);
                    } else {
                        scene.setRoot(nextRoot);
                    }
                } finally {
                    navigating = false;
                }
            });
            return;
        }

        // Prepare and show overlay
        overlay.setManaged(true);
        overlay.setVisible(true);
        overlay.setOpacity(0.0);
        overlay.toFront();

        FadeTransition fadeIn = new FadeTransition(OVERLAY_FADE_IN, overlay);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(Interpolator.EASE_BOTH);

        fadeIn.setOnFinished(ev -> {
            // Ensure overlay is actually painted before swapping content.
            Platform.runLater(() -> Platform.runLater(() -> {
                try {
                    Parent nextRoot = loadFxml(res);

                    if (shellRoot instanceof StackPane sp) {
                        // Keep overlay on top
                        sp.getChildren().remove(overlay);
                        sp.getChildren().setAll(nextRoot, overlay);
                    } else {
                        scene.setRoot(nextRoot);
                    }

                    overlay.toFront();

                    PauseTransition hold = new PauseTransition(MIN_OVERLAY_VISIBLE);
                    FadeTransition fadeOut = new FadeTransition(OVERLAY_FADE_OUT, overlay);
                    fadeOut.setFromValue(1.0);
                    fadeOut.setToValue(0.0);
                    fadeOut.setInterpolator(Interpolator.EASE_BOTH);
                    fadeOut.setOnFinished(done -> {
                        overlay.setVisible(false);
                        overlay.setManaged(false);
                        navigating = false;
                    });

                    hold.setOnFinished(evt -> fadeOut.play());
                    hold.play();
                } catch (RuntimeException ex) {
                    // Always release navigation lock even if load fails
                    overlay.setVisible(false);
                    overlay.setManaged(false);
                    navigating = false;
                    throw ex;
                }
            }));
        });

        fadeIn.play();
    }

    private static Parent loadFxml(URL res) {
        try {
            return FXMLLoader.load(res);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + res + ": " + e.getMessage(), e);
        }
    }

    private static Node findOverlay(Parent root) {
        if (root == null) return null;
        return root.lookup("#navOverlay");
    }
}
