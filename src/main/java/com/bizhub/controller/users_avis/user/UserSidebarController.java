package com.bizhub.controller.users_avis.user;

import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;


import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;


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
    public void logout() {
        AppSession.clear();
        Services.auth().logout();
        Stage stage = (Stage) goProfileBtn.getScene().getWindow();
        new NavigationService(stage).goToLogin();
    }

    @FXML
    public void goCommunity() {
        Stage stage = (Stage) goProfileBtn.getScene().getWindow();
        new NavigationService(stage).goToCommunity();
    }
}
