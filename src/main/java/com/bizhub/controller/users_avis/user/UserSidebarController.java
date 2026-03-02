package com.bizhub.controller.users_avis.user;

import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import com.bizhub.model.users_avis.user.User;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 * Sidebar navigation for non-admin users.
 */
public class UserSidebarController {

    @FXML private Button goProfileBtn;

    @FXML
    public void initialize() {
        // Sidebar no longer has avatar/name - they're in the topbar now
    }

    @FXML
    public void goProfile() {
        Stage stage = (Stage) goProfileBtn.getScene().getWindow();
        new NavigationService(stage).goToProfile();
    }

    @FXML
    public void goFormations() {
        Stage stage = (Stage) goProfileBtn.getScene().getWindow();
        new NavigationService(stage).goToFormations();
    }

    @FXML
    public void goReviews() {
        Stage stage = (Stage) goProfileBtn.getScene().getWindow();
        new NavigationService(stage).goToReviews();
    }

    @FXML
    public void goMarketplace() {
        User me = AppSession.getCurrentUser();
        Stage stage = (Stage) goProfileBtn.getScene().getWindow();
        NavigationService nav = new NavigationService(stage);

        if (me == null) {
            nav.goToLogin();
            return;
        }

        String role = me.getUserType() == null ? "" : me.getUserType().trim().toLowerCase();

        if (role.contains("startup")) {
            nav.goToCommande();
        } else if (role.contains("investisseur") || role.contains("fournisseur")) {
            nav.goToProduitService();
        } else {
            nav.goToCommande();
        }
    }

    @FXML
    public void goAiChat() {
        Stage stage = (Stage) goProfileBtn.getScene().getWindow();
        new NavigationService(stage).goToAiChat();
    }

    @FXML
    public void logout() {
        AppSession.clear();
        Services.auth().logout();
        Stage stage = (Stage) goProfileBtn.getScene().getWindow();
        new NavigationService(stage).goToLogin();
    }
}
