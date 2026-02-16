package com.bizhub.controller.users_avis.user;

import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

public class UserProfileController {

    @FXML private ImageView avatarView;

    @FXML private TextField emailField;
    @FXML private TextField fullNameField;
    @FXML private TextField phoneField;
    @FXML private TextField companyField;
    @FXML private TextField sectorField;

    @FXML private TextField addressField;
    @FXML private TextArea bioArea;

    @FXML private Label roleLabel;

    // Role panes
    @FXML private TitledPane startupPane;
    @FXML private TitledPane supplierPane;
    @FXML private TitledPane investorPane;
    @FXML private TitledPane trainerPane;
    @FXML private TitledPane adminPane;

    // STARTUP fields
    @FXML private TextArea startupCompanyDescriptionArea;
    @FXML private TextField startupWebsiteField;
    @FXML private DatePicker startupFoundingDatePicker;

    // FOURNISSEUR fields
    @FXML private TextField supplierBusinessTypeField;
    @FXML private TextArea supplierDeliveryZonesArea;
    @FXML private TextArea supplierPaymentMethodsArea;
    @FXML private TextArea supplierReturnPolicyArea;

    // INVESTOR fields
    @FXML private TextField investorSectorField;
    @FXML private TextField investorMaxBudgetField;
    @FXML private TextField investorYearsExperienceField;
    @FXML private TextField investorRepresentedCompanyField;

    // TRAINER fields
    @FXML private TextField trainerSpecialtyField;
    @FXML private TextField trainerHourlyRateField;
    @FXML private TextArea trainerAvailabilityArea;
    @FXML private TextField trainerCvUrlField;

    // ADMIN fields
    @FXML private TextField adminRoleField;
    @FXML private DatePicker adminRoleStartDatePicker;

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordMsgLabel;

    @FXML private BorderPane root;
    @FXML private HBox topbar;

    private User me;

    @FXML
    public void initialize() {
        // Add user profile to topbar
        if (topbar != null) {
            topbar.getChildren().add(TopbarProfileHelper.createProfileBox());
        }

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

        passwordMsgLabel.setText("");

        applyRoleVisibility(me.getUserType());
        loadFieldsFromUser(me);
        reloadAvatar();
    }

    private void applyRoleVisibility(String userType) {
        String role = userType == null ? "" : userType.trim().toLowerCase();

        setPaneVisible(startupPane, false);
        setPaneVisible(supplierPane, false);
        setPaneVisible(investorPane, false);
        setPaneVisible(trainerPane, false);
        setPaneVisible(adminPane, false);

        switch (role) {
            case "startup" -> setPaneVisible(startupPane, true);
            case "fournisseur" -> setPaneVisible(supplierPane, true);
            case "investisseur" -> setPaneVisible(investorPane, true);
            case "formateur" -> setPaneVisible(trainerPane, true);
            case "admin" -> setPaneVisible(adminPane, true);
            default -> {
                // keep all hidden
            }
        }

        if (roleLabel != null) {
            roleLabel.setText(role.isBlank() ? "" : ("Role: " + capitalize(role)));
        }
    }

    private void setPaneVisible(TitledPane pane, boolean visible) {
        if (pane == null) return;
        pane.setVisible(visible);
        pane.setManaged(visible);
        if (visible) pane.setExpanded(true);
    }

    private void loadFieldsFromUser(User u) {
        emailField.setText(nz(u.getEmail()));
        fullNameField.setText(nz(u.getFullName()));
        phoneField.setText(nz(u.getPhone()));

        addressField.setText(nz(u.getAddress()));
        bioArea.setText(nz(u.getBio()));

        companyField.setText(nz(u.getCompanyName()));
        sectorField.setText(nz(u.getSector()));

        // Startup
        if (startupCompanyDescriptionArea != null) startupCompanyDescriptionArea.setText(nz(u.getCompanyDescription()));
        if (startupWebsiteField != null) startupWebsiteField.setText(nz(u.getWebsite()));
        if (startupFoundingDatePicker != null) startupFoundingDatePicker.setValue(u.getFoundingDate());

        // Supplier
        if (supplierBusinessTypeField != null) supplierBusinessTypeField.setText(nz(u.getBusinessType()));
        if (supplierDeliveryZonesArea != null) supplierDeliveryZonesArea.setText(nz(u.getDeliveryZones()));
        if (supplierPaymentMethodsArea != null) supplierPaymentMethodsArea.setText(nz(u.getPaymentMethods()));
        if (supplierReturnPolicyArea != null) supplierReturnPolicyArea.setText(nz(u.getReturnPolicy()));

        // Investor
        if (investorSectorField != null) investorSectorField.setText(nz(u.getInvestmentSector()));
        if (investorMaxBudgetField != null) investorMaxBudgetField.setText(u.getMaxBudget() == null ? "" : u.getMaxBudget().toPlainString());
        if (investorYearsExperienceField != null) investorYearsExperienceField.setText(u.getYearsExperience() == null ? "" : String.valueOf(u.getYearsExperience()));
        if (investorRepresentedCompanyField != null) investorRepresentedCompanyField.setText(nz(u.getRepresentedCompany()));

        // Trainer
        if (trainerSpecialtyField != null) trainerSpecialtyField.setText(nz(u.getSpecialty()));
        if (trainerHourlyRateField != null) trainerHourlyRateField.setText(u.getHourlyRate() == null ? "" : u.getHourlyRate().toPlainString());
        if (trainerAvailabilityArea != null) trainerAvailabilityArea.setText(nz(u.getAvailability()));
        if (trainerCvUrlField != null) trainerCvUrlField.setText(nz(u.getCvUrl()));

        // Admin
        if (adminRoleField != null) adminRoleField.setText(nz(u.getAdminRole()));
        if (adminRoleStartDatePicker != null) adminRoleStartDatePicker.setValue(u.getRoleStartDate());
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
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
        me.setAddress(addressField.getText());
        me.setBio(bioArea.getText());

        me.setCompanyName(companyField.getText());
        me.setSector(sectorField.getText());

        String role = me.getUserType() == null ? "" : me.getUserType().trim().toLowerCase();

        // Always clear all role-specific fields first to avoid stale cross-role data
        clearRoleSpecific(me);

        // Then set only for the current role
        switch (role) {
            case "startup" -> {
                me.setCompanyDescription(textOrNull(startupCompanyDescriptionArea));
                me.setWebsite(textOrNull(startupWebsiteField));
                me.setFoundingDate(startupFoundingDatePicker == null ? null : startupFoundingDatePicker.getValue());
            }
            case "fournisseur" -> {
                me.setBusinessType(textOrNull(supplierBusinessTypeField));
                me.setDeliveryZones(textOrNull(supplierDeliveryZonesArea));
                me.setPaymentMethods(textOrNull(supplierPaymentMethodsArea));
                me.setReturnPolicy(textOrNull(supplierReturnPolicyArea));
            }
            case "investisseur" -> {
                me.setInvestmentSector(textOrNull(investorSectorField));
                me.setMaxBudget(parseBigDecimalOrNull(investorMaxBudgetField));
                me.setYearsExperience(parseIntegerOrNull(investorYearsExperienceField));
                me.setRepresentedCompany(textOrNull(investorRepresentedCompanyField));
            }
            case "formateur" -> {
                me.setSpecialty(textOrNull(trainerSpecialtyField));
                me.setHourlyRate(parseBigDecimalOrNull(trainerHourlyRateField));
                me.setAvailability(textOrNull(trainerAvailabilityArea));
                me.setCvUrl(textOrNull(trainerCvUrlField));
            }
            case "admin" -> {
                me.setAdminRole(textOrNull(adminRoleField));
                me.setRoleStartDate(adminRoleStartDatePicker == null ? null : adminRoleStartDatePicker.getValue());
            }
            default -> {
            }
        }

        try {
            Services.users().update(me);
            info("Profile updated");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void clearRoleSpecific(User u) {
        // Startup
        u.setCompanyDescription(null);
        u.setWebsite(null);
        u.setFoundingDate(null);

        // Supplier
        u.setBusinessType(null);
        u.setDeliveryZones(null);
        u.setPaymentMethods(null);
        u.setReturnPolicy(null);

        // Investor
        u.setInvestmentSector(null);
        u.setMaxBudget(null);
        u.setYearsExperience(null);
        u.setRepresentedCompany(null);

        // Trainer
        u.setSpecialty(null);
        u.setHourlyRate(null);
        u.setAvailability(null);
        u.setCvUrl(null);

        // Admin
        u.setAdminRole(null);
        u.setRoleStartDate(null);
    }

    private String textOrNull(TextInputControl c) {
        if (c == null) return null;
        String t = c.getText();
        if (t == null) return null;
        t = t.trim();
        return t.isEmpty() ? null : t;
    }

    private BigDecimal parseBigDecimalOrNull(TextField tf) {
        if (tf == null) return null;
        String t = tf.getText();
        if (t == null || t.trim().isEmpty()) return null;
        try {
            return new BigDecimal(t.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseIntegerOrNull(TextField tf) {
        if (tf == null) return null;
        String t = tf.getText();
        if (t == null || t.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(t.trim());
        } catch (Exception e) {
            return null;
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
        AlertHelper.showInfo(message);
    }

    private void showError(String message) {
        AlertHelper.showError(message);
    }
}
