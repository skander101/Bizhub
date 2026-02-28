package com.bizhub;

import com.bizhub.controller.marketplace.StripeWebhookServer;
import com.bizhub.model.services.common.config.EnvLoader;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.marketplace.payment.StripeGatewayClient;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    public void init() throws Exception {
        super.init();

        // 1) Initialiser Stripe
        initStripe();

        // 2) Lire webhook secret depuis .env
        String webhookSecret = EnvLoader.getOrDefault("STRIPE_WEBHOOK_SECRET", "").trim();
        if (webhookSecret.isBlank()) {
            LOGGER.warning("⚠ STRIPE_WEBHOOK_SECRET absent du .env — webhook désactivé.");
        } else {
            LOGGER.info("✅ STRIPE_WEBHOOK_SECRET chargé (whsec_...).");
        }

        // 3) Démarrer webhook server (le serveur gère lui-même les ports libres)
        try {
            StripeWebhookServer.start(webhookSecret);
            // ⚠ Ne pas afficher un port fixe si le serveur change de port
            LOGGER.info("✅ StripeWebhookServer démarré (voir le log StripeWebhookServer pour le port réel).");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "⚠ Impossible de démarrer StripeWebhookServer : " + e.getMessage(), e);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        LOGGER.info("✅ Main.start() appelé — préparation UI...");

        // 1) Fenêtre visible immédiatement
        StackPane boot = new StackPane(new javafx.scene.control.Label("Chargement BizHub..."));
        Scene scene = new Scene(boot, 1000, 700);

        stage.setTitle("BizHub - Users & Reviews Administration");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);

        stage.setFullScreen(false);
        stage.setMaximized(false);

        var bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX() + 50);
        stage.setY(bounds.getMinY() + 50);

        stage.show();
        stage.toFront();
        stage.requestFocus();
        LOGGER.info("✅ stage.show() exécuté — fenêtre affichée (boot screen).");

        // 2) Charger FXML après affichage
        javafx.application.Platform.runLater(() -> {
            try {
                LOGGER.info("⏳ Chargement login.fxml...");
                URL fxml = Main.class.getResource("/com/bizhub/fxml/login.fxml");
                if (fxml == null) throw new IllegalStateException("Cannot find /com/bizhub/fxml/login.fxml on classpath");
                Parent initial = NavigationService.loadFxmlSafe(fxml);
                LOGGER.info("✅ login.fxml chargé.");

                LOGGER.info("⏳ Chargement loading-overlay.fxml...");
                URL overlayFxml = Main.class.getResource("/com/bizhub/fxml/loading-overlay.fxml");
                if (overlayFxml == null) throw new IllegalStateException("Cannot find /com/bizhub/fxml/loading-overlay.fxml on classpath");
                Parent overlay = NavigationService.loadFxmlSafe(overlayFxml);
                LOGGER.info("✅ loading-overlay.fxml chargé.");

                StackPane appShell = new StackPane();
                appShell.setId("appShell");
                overlay.setId("navOverlay");
                appShell.getChildren().addAll(initial, overlay);

                scene.setRoot(appShell);

                URL themeCss = Main.class.getResource("/com/bizhub/css/theme.css");
                if (themeCss != null) {
                    try { scene.getStylesheets().add(themeCss.toURI().toString()); }
                    catch (Exception e) { scene.getStylesheets().add(themeCss.toExternalForm()); }
                }

                stage.setWidth(Math.min(1200, bounds.getWidth() - 100));
                stage.setHeight(Math.min(800, bounds.getHeight() - 100));
                stage.toFront();

                LOGGER.info("✅ UI BizHub affichée (root remplacé).");

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "❌ Erreur chargement UI (FXML)", ex);
                scene.setRoot(new StackPane(new javafx.scene.control.Label("Erreur UI: " + ex.getMessage())));
            }
        });
    }

    @Override
    public void stop() throws Exception {
        try {
            StripeWebhookServer.stop();
            LOGGER.info("✅ StripeWebhookServer stoppé.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "⚠ Erreur stop : " + e.getMessage(), e);
        }
        super.stop();
    }

    private void initStripe() {
        try {
            new StripeGatewayClient();
            LOGGER.info("✅ Stripe configuré — paiement disponible.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "⚠ Stripe non configuré : " + e.getMessage()
                            + " — Vérifiez vos variables (.env / env vars).",
                    e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}