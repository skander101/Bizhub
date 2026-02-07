package com.bizhub.user.controller;

import com.bizhub.user.model.User;
import com.bizhub.common.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.SQLException;

public class UserProfileController {

    @FXML private ImageView avatarView;

    @FXML private TextField emailField;
    @FXML private TextField fullNameField;
    @FXML private TextField phoneField;
    @FXML private TextField companyField;
    @FXML private TextField sectorField;

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordMsgLabel;

    @FXML private BorderPane root;

    private User me;

    @FXML
    public void initialize() {
        if (AppSession.isAdmin()) {
            try {
                Parent n = FXMLLoader.load(getClass().getResource("/com/bizhub/fxml/admin-sidebar.fxml"));
                if (root != null) root.setLeft(n);
                NavigationService.setActiveNav(n, NavigationService.ActiveNav.PROFILE);
            } catch (Exception ignored) {
            }
        } else {
            if (root != null && root.getLeft() != null) {
                NavigationService.setActiveNav(root.getLeft(), NavigationService.ActiveNav.PROFILE);
            }
        }

        me = AppSession.getCurrentUser();
        if (me == null) return;

        // reload from DB to get all fields
        try {
            me = Services.users().findById(me.getUserId()).orElse(me);
            AppSession.setCurrentUser(me);
        } catch (SQLException ignored) {
        }

        emailField.setText(me.getEmail());
        fullNameField.setText(me.getFullName());
        phoneField.setText(me.getPhone());
        companyField.setText(me.getCompanyName());
        sectorField.setText(me.getSector());

        passwordMsgLabel.setText("");

        reloadAvatar();
    }

    private void reloadAvatar() {
        if (avatarView == null || me == null || me.getAvatarUrl() == null || me.getAvatarUrl().isEmpty()) {
            avatarView.setImage(null);
            return;
        }

        try {
            // Assuming avatarUrl is a path relative to the resources folder
            File imageFile = new File("src/main/resources/" + me.getAvatarUrl());
            if (imageFile.exists()) {
                Image img = new Image(imageFile.toURI().toString());
                avatarView.setImage(img);
            } else {
                // Fallback or default image if path is broken
                avatarView.setImage(null);
            }
        } catch (Exception ignored) {
            avatarView.setImage(null);
        }
    }

    @FXML
    public void changePicture() {
        if (me == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Choose profile picture");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fc.showOpenDialog(avatarView.getScene().getWindow());
        if (selectedFile == null) return;

        try {
            // The service will save the file and return the path
            String newAvatarPath = Services.users().updateProfilePicture(me.getUserId(), selectedFile);
            me.setAvatarUrl(newAvatarPath);
            AppSession.setCurrentUser(me); // Update session user
            reloadAvatar();

            info("Profile picture updated");
        } catch (Exception e) {
            showError("Failed to update profile picture: " + e.getMessage());
        }
    }

    @FXML
    public void save() {
        if (me == null) return;

        me.setFullName(fullNameField.getText());
        me.setPhone(phoneField.getText());
        me.setCompanyName(companyField.getText());
        me.setSector(sectorField.getText());

        try {
            Services.users().update(me);
            info("Profile updated");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void changePassword() {
        if (me == null) return;

        String p1 = newPasswordField.getText();
        String p2 = confirmPasswordField.getText();
        if (p1 == null || p1.isBlank() || p1.length() < 6) {
            passwordMsgLabel.setText("Min 6 characters");
            return;
        }
        if (!p1.equals(p2)) {
            passwordMsgLabel.setText("Passwords do not match");
            return;
        }

        try {
            String hash = Services.auth().hashPassword(p1);
            Services.users().updatePasswordHash(me.getUserId(), hash);
            passwordMsgLabel.setText("Updated");
            newPasswordField.clear();
            confirmPasswordField.clear();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void goReviews() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        if (AppSession.isAdmin()) new NavigationService(stage).goToReviews();
        else new NavigationService(stage).goToReviews();
    }

    @FXML
    public void goFormations() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        new NavigationService(stage).goToFormations();
    }

    @FXML
    public void logout() {
        AppSession.clear();
        Services.auth().logout();
        Stage stage = (Stage) emailField.getScene().getWindow();
        new NavigationService(stage).goToLogin();
    }

    private void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
