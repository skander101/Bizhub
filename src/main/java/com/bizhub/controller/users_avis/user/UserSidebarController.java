package com.bizhub.controller.users_avis.user;

import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Sidebar navigation for non-admin users.
 */
public class UserSidebarController {

    @FXML private ImageView avatarView;
    @FXML private Text avatarInitials;

    @FXML private Text nameText;
    @FXML private Text roleText;

    @FXML
    public void initialize() {
        User me = AppSession.getCurrentUser();
        if (me == null) {
            nameText.setText("Guest");
            roleText.setText("");
            avatarInitials.setText("BH");
            avatarView.setVisible(false);
            return;
        }

        nameText.setText(me.getFullName() == null ? "User" : me.getFullName());
        roleText.setText(me.getUserType() == null ? "" : me.getUserType());

        String initials = initialsOf(me.getFullName());
        avatarInitials.setText(initials);

        // Try to load avatar (stored as classpath-relative or absolute URL).
        loadAvatar(me.getAvatarUrl());

        // Fallback active button highlighting.
        try {
            Platform.runLater(() -> {
                var scene = nameText.getScene();
                if (scene == null) return;
                var h1 = scene.getRoot().lookup(".h1");
                if (h1 instanceof javafx.scene.text.Text t) {
                    String title = t.getText() == null ? "" : t.getText().toLowerCase();
                    if (title.contains("formations")) NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.FORMATIONS);
                    else if (title.contains("reviews")) NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.REVIEWS);
                    else if (title.contains("profile")) NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.PROFILE);
                }
            });
        } catch (Exception ignored) {}
    }

    private void loadAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatarView.setVisible(false);
            return;
        }

        try {
            Image img;
            if (avatarUrl.startsWith("/")) {
                InputStream is = getClass().getResourceAsStream(avatarUrl);
                if (is == null) {
                    avatarView.setVisible(false);
                    return;
                }
                img = new Image(is);
            } else {
                img = new Image(avatarUrl, true);
            }

            avatarView.setImage(img);
            avatarView.setVisible(true);
            avatarInitials.setVisible(false);
        } catch (Exception e) {
            avatarView.setVisible(false);
            avatarInitials.setVisible(true);
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

    @FXML
    public void goProfile() {
        Stage stage = (Stage) nameText.getScene().getWindow();
        new NavigationService(stage).goToProfile();
    }

    @FXML
    public void goFormations() {
        Stage stage = (Stage) nameText.getScene().getWindow();
        new NavigationService(stage).goToFormations();
    }

    @FXML
    public void goReviews() {
        Stage stage = (Stage) nameText.getScene().getWindow();
        new NavigationService(stage).goToReviews();
    }

    @FXML
    public void logout() {
        AppSession.clear();
        Services.auth().logout();
        Stage stage = (Stage) nameText.getScene().getWindow();
        new NavigationService(stage).goToLogin();
    }
}
