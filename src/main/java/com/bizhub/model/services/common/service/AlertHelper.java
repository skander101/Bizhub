package com.bizhub.model.services.common.service;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.StageStyle;

import java.util.Optional;

/**
 * Helper class to create styled alerts that match the app's dark theme.
 */
public class AlertHelper {

    private static final String DIALOG_STYLE = """
        .dialog-pane {
            -fx-background-color: #0A192F;
            -fx-border-color: rgba(255, 184, 77, 0.50);
            -fx-border-width: 2px;
            -fx-border-radius: 16;
            -fx-background-radius: 16;
            -fx-padding: 20;
        }
        .dialog-pane > .header-panel {
            -fx-background-color: #1A3352;
            -fx-background-radius: 12 12 0 0;
            -fx-padding: 16;
        }
        .dialog-pane > .header-panel > .label {
            -fx-text-fill: #FFFFFF;
            -fx-font-size: 18px;
            -fx-font-weight: 800;
        }
        .dialog-pane > .content.label {
            -fx-text-fill: #FFB84D;
            -fx-font-size: 15px;
            -fx-padding: 16 0;
        }
        .dialog-pane > .button-bar > .container {
            -fx-background-color: transparent;
        }
        .dialog-pane > .button-bar .button {
            -fx-background-color: rgba(255, 184, 77, 0.15);
            -fx-text-fill: #FFB84D;
            -fx-font-weight: bold;
            -fx-background-radius: 12;
            -fx-border-radius: 12;
            -fx-border-color: rgba(255, 184, 77, 0.45);
            -fx-border-width: 1.5;
            -fx-padding: 10 20;
            -fx-font-size: 14px;
            -fx-cursor: hand;
        }
        .dialog-pane > .button-bar .button:hover {
            -fx-background-color: rgba(255, 184, 77, 0.28);
            -fx-border-color: #FFB84D;
        }
        .dialog-pane > .button-bar .button:default {
            -fx-background-color: linear-gradient(to bottom, #FFD54F, #FFB84D);
            -fx-text-fill: #0A192F;
            -fx-border-color: transparent;
        }
        .dialog-pane > .button-bar .button:default:hover {
            -fx-background-color: linear-gradient(to bottom, #FFE082, #FFD54F);
        }
        .dialog-pane:header .header-panel .label {
            -fx-text-fill: #FFFFFF;
        }
        .dialog-pane .graphic-container {
            -fx-padding: 0 10 0 0;
        }
        /* Style for the header text */
        .dialog-pane > .header-panel > .header-panel > .label {
            -fx-text-fill: #FFFFFF;
        }
        /* Make sure all text is visible */
        .dialog-pane .label {
            -fx-text-fill: #FFB84D;
        }
        .dialog-pane > .header-panel .label {
            -fx-text-fill: #FFFFFF;
        }
        """;

    /**
     * Apply dark theme styling to an alert dialog.
     */
    public static void styleAlert(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #0A192F;");
        dialogPane.getStylesheets().clear();
        dialogPane.getStylesheets().add("data:text/css," + DIALOG_STYLE.replace("\n", "").replace("  ", " "));

        // Remove default window decorations for a cleaner look
        alert.initStyle(StageStyle.UNDECORATED);
    }

    /**
     * Show an error alert with app styling.
     */
    public static void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleAlert(alert);
        alert.showAndWait();
    }

    /**
     * Show an error alert with app styling (simple version).
     */
    public static void showError(String content) {
        showError("Error", "Operation failed", content);
    }

    /**
     * Show an info alert with app styling.
     */
    public static void showInfo(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleAlert(alert);
        alert.showAndWait();
    }

    /**
     * Show an info alert with app styling (simple version).
     */
    public static void showInfo(String content) {
        showInfo("Info", null, content);
    }

    /**
     * Show a confirmation dialog with app styling.
     * @return true if user clicked OK, false otherwise
     */
    public static boolean showConfirm(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleAlert(alert);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Show a confirmation dialog with app styling (simple version).
     */
    public static boolean showConfirm(String content) {
        return showConfirm("Confirm", "Please confirm", content);
    }

    /**
     * Show a warning alert with app styling.
     */
    public static void showWarning(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleAlert(alert);
        alert.showAndWait();
    }

    /**
     * Show a warning alert with app styling (simple version).
     */
    public static void showWarning(String content) {
        showWarning("Warning", null, content);
    }
}

