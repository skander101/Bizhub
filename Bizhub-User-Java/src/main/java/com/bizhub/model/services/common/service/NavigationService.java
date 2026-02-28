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
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NavigationService {

    private static final Logger LOGGER = Logger.getLogger(NavigationService.class.getName());

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
    public void goToLogin()  { loadIntoStage("/com/bizhub/fxml/login.fxml", 980, 640); }
    public void goToSignup() { loadIntoStage("/com/bizhub/fxml/signup.fxml", 980, 700); }

    // ====== ADMIN ======
    public void goToAdminDashboard()  { loadIntoStage("/com/bizhub/fxml/admin-dashboard.fxml", 1200, 760); }
    public void goToUserManagement()  { loadIntoStage("/com/bizhub/fxml/user-management.fxml", 1200, 760); }

    // ====== COMMON ======
    public void goToReviews()          { loadIntoStage("/com/bizhub/fxml/reviews-list.fxml", 1200, 760); }
    public void goToProfile()          { loadIntoStage("/com/bizhub/fxml/user-profile.fxml", 1000, 700); }
    public void goToFormations()       { loadIntoStage("/com/bizhub/fxml/formations.fxml", 1200, 760); }
    public void goToFormationDetails() { loadIntoStage("/com/bizhub/fxml/formation-details.fxml", 1200, 760); }

    // ====== MARKETPLACE ======
    public void goToCommande()      { loadIntoStage("/com/bizhub/fxml/commande.fxml", 1300, 820); }
    public void goToProduitService(){ loadIntoStage("/com/bizhub/fxml/produit_service.fxml", 1300, 820); }
    public void goToMarketplace()   { goToCommande(); }

    // ================== CORE LOADER ==================
    private void loadIntoStage(String fxmlPath, double w, double h) {
        if (navigating) return;
        navigating = true;

        try {
            URL res = NavigationService.class.getResource(fxmlPath);
            if (res == null) throw new IllegalStateException("Missing FXML: " + fxmlPath);

            Scene scene = stage.getScene();

            // 1) Première scène → création directe
            if (scene == null) {
                LOGGER.info("⏳ First scene load: " + fxmlPath);
                Parent first = loadFxml(res);
                Scene newScene = new Scene(first, w, h);
                stage.setScene(newScene);

                var bounds = Screen.getPrimary().getVisualBounds();
                stage.setX(bounds.getMinX() + 50);
                stage.setY(bounds.getMinY() + 50);
                stage.setWidth(Math.min(1200, bounds.getWidth() - 100));
                stage.setHeight(Math.min(800, bounds.getHeight() - 100));

                stage.setFullScreen(false);
                stage.setMaximized(false);

                stage.show();
                stage.toFront();
                stage.requestFocus();

                LOGGER.info("✅ Scene affichée: " + fxmlPath);
                return;
            }

            Parent currentRoot = null;
            if (scene.getRoot() instanceof Parent) {
                currentRoot = (Parent) scene.getRoot();
            }            Node overlay = findOverlay(currentRoot);

            // 2) Pas d’overlay → swap direct
            if (overlay == null) {
                LOGGER.info("⏳ Swap root (no overlay): " + fxmlPath);
                safeRunLater(() -> {
                    Parent nextRoot = loadFxml(res);
                    scene.setRoot(nextRoot);
                    LOGGER.info("✅ Root swapped: " + fxmlPath);
                });
                return;
            }

            // 3) Overlay → animation + swap root
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
                    LOGGER.info("⏳ Loading next root: " + fxmlPath);
                    Parent nextRoot = loadFxml(res);

                    scene.setRoot(nextRoot);
                    LOGGER.info("✅ Root swapped: " + fxmlPath);

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
                    LOGGER.log(Level.SEVERE, "Navigation error while loading " + fxmlPath, ex);
                    showNavigationError("Navigation error", ex.getMessage());
                    throw ex;
                }
            }));

            fadeIn.play();

        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "Navigation error", ex);
            showNavigationError("Navigation error", ex.getMessage());
            throw ex;
        } finally {
            navigating = false;
        }
    }

    /**
     * ✅ Loader fiable : ferme le stream + donne exception claire
     */
    private static Parent loadFxml(URL res) {
        try (InputStream is = res.openStream()) {
            FXMLLoader loader = new FXMLLoader(res);
            return loader.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + res + ": " + e.getMessage(), e);
        }
    }

    public static Parent loadFxmlSafe(String fxmlPath) {
        try {
            URL res = NavigationService.class.getResource(fxmlPath);
            if (res == null) throw new IllegalStateException("Missing FXML: " + fxmlPath);
            return loadFxml(res);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + fxmlPath + ": " + e.getMessage(), e);
        }
    }

    public static Parent loadFxmlSafe(URL res) {
        return loadFxml(res);
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