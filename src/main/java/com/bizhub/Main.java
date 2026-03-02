package com.bizhub;

import com.bizhub.controller.marketplace.InvestorStatsApiServer;
import com.bizhub.controller.marketplace.StripeWebhookServer;
import com.bizhub.model.services.marketplace.payment.StripeGatewayClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
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

        // 1) Initialize Stripe
        initStripe();

        // 2) Start webhook + API stats servers
        try {
            int port = StripeWebhookServer.start("");

            try {
                int apiPort = InvestorStatsApiServer.start();
                LOGGER.info("✅ InvestorStatsApiServer started on : http://localhost:" + apiPort + "/api/investor/stats");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "⚠ Could not start InvestorStatsApiServer : " + e.getMessage(), e);
            }

            if (port > 0) {
                LOGGER.info("✅ StripeWebhookServer started on : http://localhost:" + port + "/webhook/stripe");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "⚠ Could not start StripeWebhookServer : " + e.getMessage(), e);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        URL fxml = Main.class.getResource("/com/bizhub/fxml/login.fxml");
        if (fxml == null) {
            throw new IllegalStateException("Cannot find /com/bizhub/fxml/login.fxml on classpath");
        }

        Parent initial = FXMLLoader.load(fxml);

        // Scene shell: holds current page + a top overlay (logo + loading bar) for smooth transitions.
        StackPane appShell = new StackPane();
        appShell.setId("appShell");
        appShell.getChildren().add(initial);

        URL overlayFxml = Main.class.getResource("/com/bizhub/fxml/loading-overlay.fxml");
        if (overlayFxml == null) {
            throw new IllegalStateException("Cannot find /com/bizhub/fxml/loading-overlay.fxml on classpath");
        }
        Parent overlay = FXMLLoader.load(overlayFxml);
        overlay.setId("navOverlay");
        appShell.getChildren().add(overlay);

        var bounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(appShell, Math.max(980, bounds.getWidth()), Math.max(640, bounds.getHeight()));

        // Apply global theme at Scene level to prevent white flashes during navigation swaps.
        URL themeCss = Main.class.getResource("/com/bizhub/css/theme.css");
        if (themeCss != null) {
            scene.getStylesheets().add(themeCss.toExternalForm());
        }

        stage.setTitle("BizHub - Users & Reviews Administration");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);

        // Full screen
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);

        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // ✅ Clean stop: webhook + API stats
        try {
            StripeWebhookServer.stop();
            LOGGER.info("✅ StripeWebhookServer stopped.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "⚠ Error stopping StripeWebhookServer : " + e.getMessage(), e);
        }

        try {
            InvestorStatsApiServer.stop();
            LOGGER.info("✅ InvestorStatsApiServer stopped.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "⚠ Error stopping InvestorStatsApiServer : " + e.getMessage(), e);
        }

        super.stop();
    }

    private void initStripe() {
        try {
            new StripeGatewayClient();
            LOGGER.info("✅ Stripe configured — payment available.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "⚠ Stripe not configured : " + e.getMessage()
                            + " — Check your variables (.env / env vars).",
                    e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
