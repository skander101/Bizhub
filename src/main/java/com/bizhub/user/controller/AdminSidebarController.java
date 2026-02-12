package com.bizhub.user.controller;

import com.bizhub.user.model.User;
import com.bizhub.common.service.AppSession;
import com.bizhub.common.service.NavigationService;
import com.bizhub.common.service.Services;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.io.InputStream;

/** Sidebar navigation for admins. */
public class AdminSidebarController {

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

        nameText.setText(me.getFullName() == null ? "Admin" : me.getFullName());
        roleText.setText(me.getUserType() == null ? "" : me.getUserType());

        avatarInitials.setText(initialsOf(me.getFullName()));
        loadAvatar(me.getAvatarUrl());

        // Fallback: best-effort active button highlighting based on window state.
        try {
            Platform.runLater(() -> {
                var scene = nameText.getScene();
                if (scene == null) return;
                var h1 = scene.getRoot().lookup(".h1");
                if (h1 instanceof javafx.scene.text.Text t) {
                    String title = t.getText() == null ? "" : t.getText().toLowerCase();
                    if (title.contains("dashboard")) NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.DASHBOARD);
                    else if (title.contains("users")) NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.USERS);
                    else if (title.contains("formations")) NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.FORMATIONS);
                    else if (title.contains("participation")) NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.PARTICIPATIONS);
                    else if (title.contains("reviews")) NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.REVIEWS);
                    else if (title.contains("profile")) NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.PROFILE);
                }
            });
        } catch (Exception ignored) {}
    }

    private void loadAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatarView.setVisible(false);
            avatarInitials.setVisible(true);
            return;
        }

        try {
            Image img;
            if (avatarUrl.startsWith("/")) {
                InputStream is = getClass().getResourceAsStream(avatarUrl);
                if (is == null) {
                    avatarView.setVisible(false);
                    avatarInitials.setVisible(true);
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
    public void goDashboard() {
        Stage stage = (Stage) nameText.getScene().getWindow();
        new NavigationService(stage).goToAdminDashboard();
    }

    @FXML
    public void goUsers() {
        Stage stage = (Stage) nameText.getScene().getWindow();
        new NavigationService(stage).goToUserManagement();
    }

    @FXML
    public void goFormations() {
        Stage stage = (Stage) nameText.getScene().getWindow();
        new NavigationService(stage).goToFormations();
    }

    @FXML
    public void goParticipations() {
        Stage stage = (Stage) nameText.getScene().getWindow();
        new NavigationService(stage).goToParticipations();
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
