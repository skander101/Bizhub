package com.bizhub.model.services.common.service;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
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
        PROFILE,
        MARKETPLACE
    }

    public static void setActiveNav(Node sidebarRoot, ActiveNav activeNav) {
        if (sidebarRoot == null || activeNav == null) return;

        for (Node n : sidebarRoot.lookupAll(".nav-button")) {
            n.getStyleClass().remove("active");

            if (!(n instanceof Button b)) continue;
            String t = b.getText() == null ? "" : b.getText().toLowerCase();

            boolean match = switch (activeNav) {
                case DASHBOARD -> t.contains("dashboard");
                case USERS -> t.contains("users");
                case FORMATIONS -> t.contains("formations");
                case REVIEWS -> t.contains("reviews");
                case PROFILE -> t.contains("profile");
                case MARKETPLACE -> t.contains("marketplace");
            };

            if (match) n.getStyleClass().add("active");
        }
    }

    private static final Duration OVERLAY_FADE_IN = Duration.millis(260);
    private static final Duration OVERLAY_FADE_OUT = Duration.millis(360);
    private static final Duration MIN_OVERLAY_VISIBLE = Duration.millis(500);

    private final Stage stage;
    private boolean navigating = false;

    public NavigationService(Stage stage) {
        this.stage = stage;
    }

    // ====== AUTH / BASE ======
    public void goToLogin() { loadIntoStage("/com/bizhub/fxml/login.fxml", 980, 640); }
    public void goToSignup() { loadIntoStage("/com/bizhub/fxml/signup.fxml", 980, 700); }

    // ====== ADMIN ======
    public void goToAdminDashboard() { loadIntoStage("/com/bizhub/fxml/admin-dashboard.fxml", 1200, 760); }
    public void goToUserManagement() { loadIntoStage("/com/bizhub/fxml/user-management.fxml", 1200, 760); }

    // ====== COMMON ======
    public void goToReviews() { loadIntoStage("/com/bizhub/fxml/reviews-list.fxml", 1200, 760); }
    public void goToProfile() { loadIntoStage("/com/bizhub/fxml/user-profile.fxml", 1000, 700); }
    public void goToFormations() { loadIntoStage("/com/bizhub/fxml/formations.fxml", 1200, 760); }
    public void goToFormationDetails() { loadIntoStage("/com/bizhub/fxml/formation-details.fxml", 1200, 760); }

    // ====== MARKETPLACE ======
    public void goToCommande() { loadIntoStage("/com/bizhub/fxml/commande.fxml", 1300, 820); }
    public void goToProduitService() { loadIntoStage("/com/bizhub/fxml/produit_service.fxml", 1300, 820); }
    public void goToMarketplace() { goToCommande(); }

    // ================== CORE LOADER ==================
    private void loadIntoStage(String fxmlPath, double w, double h) {
        if (navigating) return;
        navigating = true;

        try {
            URL res = NavigationService.class.getResource(fxmlPath);
            if (res == null) throw new IllegalStateException("Missing FXML: " + fxmlPath);

            Scene scene = stage.getScene();
            if (scene == null) {
                Parent first = loadFxml(res);
                Scene newScene = new Scene(first, w, h);
                stage.setScene(newScene);
                stage.show();
                return;
            }

            Parent currentRoot = (Parent) scene.getRoot();
            Node overlay = findOverlay(currentRoot);

            // ✅ Pas d'overlay → swap direct (FIABLE)
            if (overlay == null) {
                safeRunLater(() -> {
                    Parent nextRoot = loadFxml(res);
                    scene.setRoot(nextRoot);
                });
                return;
            }

            // ✅ Overlay existe
            overlay.setManaged(true);
            overlay.setVisible(true);
            overlay.setOpacity(0.0);
            overlay.toFront();

            ProgressBar progressBar = (ProgressBar) overlay.lookup(".loading-bar");
            Timeline progressTimeline = null;
            if (progressBar != null) {
                progressBar.setProgress(0.0);
                progressTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(1),
                                new KeyValue(progressBar.progressProperty(), 1.0))
                );
                progressTimeline.play();
            }

            FadeTransition fadeIn = new FadeTransition(OVERLAY_FADE_IN, overlay);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setInterpolator(Interpolator.EASE_BOTH);

            Timeline finalProgressTimeline = progressTimeline;
            fadeIn.setOnFinished(ev -> safeRunLater(() -> {
                try {
                    Parent nextRoot = loadFxml(res);

                    // ✅ Swap la root scene (le plus stable)
                    scene.setRoot(nextRoot);

                    // ⚠️ overlay a disparu car root a changé => on essaye de retrouver overlay dans nouvelle root
                    Node newOverlay = findOverlay(nextRoot);
                    if (newOverlay != null) {
                        newOverlay.setManaged(true);
                        newOverlay.setVisible(true);
                        newOverlay.setOpacity(1.0);
                        newOverlay.toFront();

                        PauseTransition hold = new PauseTransition(MIN_OVERLAY_VISIBLE);
                        FadeTransition fadeOut = new FadeTransition(OVERLAY_FADE_OUT, newOverlay);
                        fadeOut.setFromValue(1.0);
                        fadeOut.setToValue(0.0);
                        fadeOut.setInterpolator(Interpolator.EASE_BOTH);
                        fadeOut.setOnFinished(done -> {
                            newOverlay.setVisible(false);
                            newOverlay.setManaged(false);
                        });

                        hold.setOnFinished(e2 -> fadeOut.play());
                        hold.play();
                    }

                    if (finalProgressTimeline != null) finalProgressTimeline.stop();

                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    showNavigationError("Navigation error", ex.getMessage());
                    throw ex;
                }
            }));

            fadeIn.play();

        } catch (RuntimeException ex) {
            ex.printStackTrace();
            showNavigationError("Navigation error", ex.getMessage());
            throw ex;
        } finally {
            // ✅ Toujours libérer le lock (même si erreur)
            navigating = false;
        }
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

    private static void safeRunLater(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    private void showNavigationError(String title, String msg) {
        safeRunLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg == null ? "Unknown error" : msg);
            a.showAndWait();
        });
    }
}
