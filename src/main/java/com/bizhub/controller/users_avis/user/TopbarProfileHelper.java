package com.bizhub.controller.users_avis.user;

import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.AppSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.InputStream;

/**
 * Helper class to create the user profile component for the topbar.
 * This creates an HBox with avatar, name and role that can be added to any topbar.
 */
public class TopbarProfileHelper {

    /**
     * Creates an HBox containing the user's avatar, name, and role for display in the topbar.
     * @return HBox with user profile info
     */
    public static HBox createProfileBox() {
        User user = AppSession.getCurrentUser();

        HBox profileBox = new HBox(12);
        profileBox.setAlignment(Pos.CENTER_RIGHT);
        profileBox.getStyleClass().add("topbar-profile");

        // Name and role VBox
        VBox nameRoleBox = new VBox(2);
        nameRoleBox.setAlignment(Pos.CENTER_RIGHT);

        Text nameText = new Text(user != null && user.getFullName() != null ? user.getFullName() : "User");
        nameText.getStyleClass().add("topbar-name");

        Text roleText = new Text(user != null && user.getUserType() != null ? user.getUserType() : "");
        roleText.getStyleClass().add("topbar-role");

        nameRoleBox.getChildren().addAll(nameText, roleText);

        // Avatar StackPane
        StackPane avatarStack = new StackPane();
        avatarStack.getStyleClass().add("topbar-avatar");
        avatarStack.setPrefSize(44, 44);
        avatarStack.setMinSize(44, 44);
        avatarStack.setMaxSize(44, 44);

        ImageView avatarView = new ImageView();
        avatarView.setFitWidth(40);
        avatarView.setFitHeight(40);
        avatarView.setPreserveRatio(true);
        avatarView.setSmooth(true);
        avatarView.getStyleClass().add("topbar-avatar-img");

        Text avatarInitials = new Text(initialsOf(user != null ? user.getFullName() : null));
        avatarInitials.getStyleClass().add("topbar-avatar-text");

        // Load avatar
        boolean avatarLoaded = loadAvatar(avatarView, user != null ? user.getAvatarUrl() : null);
        avatarView.setVisible(avatarLoaded);
        avatarInitials.setVisible(!avatarLoaded);

        avatarStack.getChildren().addAll(avatarInitials, avatarView);

        profileBox.getChildren().addAll(nameRoleBox, avatarStack);

        return profileBox;
    }

    private static boolean loadAvatar(ImageView avatarView, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return false;
        }

        try {
            Image img;
            if (avatarUrl.startsWith("/")) {
                InputStream is = TopbarProfileHelper.class.getResourceAsStream(avatarUrl);
                if (is == null) {
                    return false;
                }
                img = new Image(is);
            } else {
                img = new Image(avatarUrl, true);
            }
            avatarView.setImage(img);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String initialsOf(String name) {
        if (name == null || name.isBlank()) return "BH";
        String[] parts = name.trim().split("\\s+");
        String a = parts.length > 0 ? parts[0] : "";
        String b = parts.length > 1 ? parts[1] : "";
        String s = (a.isEmpty() ? "" : a.substring(0, 1)) + (b.isEmpty() ? "" : b.substring(0, 1));
        s = s.toUpperCase();
        return s.isBlank() ? "BH" : s;
    }
}

