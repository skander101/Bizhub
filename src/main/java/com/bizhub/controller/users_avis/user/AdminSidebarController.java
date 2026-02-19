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

/** Sidebar navigation for admins. */
public class AdminSidebarController {

    @FXML private Button goDashboardBtn;

    @FXML
    public void initialize() {
        // Sidebar no longer has avatar/name - they're in the topbar now
    }

    @FXML
    public void goDashboard() {
        Stage stage = (Stage) goDashboardBtn.getScene().getWindow();
        new NavigationService(stage).goToAdminDashboard();
    }

    @FXML
    public void goUsers() {
        Stage stage = (Stage) goDashboardBtn.getScene().getWindow();
        new NavigationService(stage).goToUserManagement();
    }

    @FXML
    public void goFormations() {
        Stage stage = (Stage) goDashboardBtn.getScene().getWindow();
        new NavigationService(stage).goToFormations();
    }

    @FXML
    public void goReviews() {
        Stage stage = (Stage) goDashboardBtn.getScene().getWindow();
        new NavigationService(stage).goToReviews();
    }

    @FXML
    public void logout() {
        AppSession.clear();
        Services.auth().logout();
        Stage stage = (Stage) goDashboardBtn.getScene().getWindow();
        new NavigationService(stage).goToLogin();
    }

    @FXML
    public void goCommunity() {
        Stage stage = (Stage) goDashboardBtn.getScene().getWindow();
        new NavigationService(stage).goToCommunity();
    }
}
