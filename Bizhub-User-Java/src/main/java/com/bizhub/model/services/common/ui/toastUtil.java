package com.bizhub.model.services.common.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

public class toastUtil {

    private static void show(String text, String type) {

        Platform.runLater(() -> {

            Notifications n = Notifications.create()
                    .text(text)
                    .hideAfter(Duration.seconds(3))
                    .position(Pos.BOTTOM_RIGHT);

            // 🎨 Style selon type
            switch (type) {
                case "success" -> n.showInformation();
                case "error"   -> n.showError();
                case "warning" -> n.showWarning();   // ⭐ IMPORTANT
                case "ai"      -> n.showInformation();
                default        -> n.show();
            }
        });
    }

    // ✅ Toast SUCCESS
    public static void success(String msg) {
        show(msg, "success");
    }

    // ❌ Toast ERROR
    public static void error(String msg) {
        show(msg, "error");
    }

    // ⚠️ Toast WARNING  ← C’EST CELLE QUI MANQUE CHEZ TOI
    public static void warning(String msg) {
        show(msg, "warning");
    }

    // 🤖 Toast IA (auto-confirm)
    public static void ai(String msg) {
        show(msg, "ai");
    }
}