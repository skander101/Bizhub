package com.bizhub.controller.users_avis.user;

import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.InputStream;

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
            avatarInitials.setVisible(true);
            return;
        }

        nameText.setText(me.getFullName() == null ? "User" : me.getFullName());
        roleText.setText(me.getUserType() == null ? "" : me.getUserType());

        avatarInitials.setText(initialsOf(me.getFullName()));
        loadAvatar(me.getAvatarUrl());

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
                    else if (title.contains("marketplace") || title.contains("commande") || title.contains("produit"))
                        NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.MARKETPLACE);
                }
            });
        } catch (Exception ignored) {}
    }

    private NavigationService nav() {
        Stage stage = (Stage) nameText.getScene().getWindow();
        return new NavigationService(stage);
    }

    @FXML
    public void goProfile() {
        nav().goToProfile();
        NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.PROFILE);
    }

    @FXML
    public void goFormations() {
        nav().goToFormations();
        NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.FORMATIONS);
    }

    @FXML
    public void goReviews() {
        nav().goToReviews();
        NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.REVIEWS);
    }

    // ✅ UNE SEULE méthode (sans paramètres) pour éviter les conflits FXML
    @FXML
    public void goMarketplace() {
        User me = AppSession.getCurrentUser();
        if (me == null) {
            nav().goToLogin();
            return;
        }

        String role = me.getUserType() == null ? "" : me.getUserType().trim().toLowerCase();
        System.out.println("CLICK Marketplace ?");
        System.out.println("ROLE = " + role);

        // ✅ ROUTING CORRECT
        // Startup -> commande.fxml
        if (role.contains("startup")) {
            nav().goToCommande();
        }
        // Investisseur -> produit_service.fxml (car il a CRUD produits + gestion commandes)
        else if (role.contains("investisseur")) {
            nav().goToProduitService();
        }
        // Fournisseur -> produit_service.fxml
        else if (role.contains("fournisseur")) {
            nav().goToProduitService();
        }
        // fallback
        else {
            nav().goToCommande();
        }

        NavigationService.setActiveNav(nameText.getParent(), NavigationService.ActiveNav.MARKETPLACE);
    }

    @FXML
    public void logout() {
        AppSession.clear();
        Services.auth().logout();
        nav().goToLogin();
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
}
