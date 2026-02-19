package com.bizhub.controller.users_avis.user;

import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class SignupController {

    private static final Duration SECTION_ANIM_DURATION = Duration.millis(520);

    private String currentRole;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullNameField;
    @FXML private ComboBox<String> roleBox;
    @FXML private TextField phoneField;

    @FXML private TextField addressField;
    @FXML private TextArea bioArea;

    // Sections
    @FXML private VBox startupSection;
    @FXML private VBox supplierSection;
    @FXML private VBox investorSection;
    @FXML private VBox trainerSection;

    // Startup
    @FXML private TextField startupCompanyField;
    @FXML private TextField startupSectorField;
    @FXML private TextField startupWebsiteField;
    @FXML private DatePicker startupFoundingDatePicker;
    @FXML private TextArea startupCompanyDescriptionArea;

    // Supplier
    @FXML private TextField supplierBusinessTypeField;
    @FXML private TextField supplierDeliveryZonesField;
    @FXML private TextField supplierPaymentMethodsField;
    @FXML private TextArea supplierReturnPolicyArea;

    // Investor
    @FXML private TextField investorSectorField;
    @FXML private TextField investorMaxBudgetField;
    @FXML private TextField investorYearsExperienceField;
    @FXML private TextField investorRepresentedCompanyField;

    // Trainer
    @FXML private TextField trainerSpecialtyField;
    @FXML private TextField trainerHourlyRateField;
    @FXML private TextField trainerAvailabilityField;
    @FXML private TextField trainerCvUrlField;

    @FXML private Button signupButton;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        errorLabel.setText("");

        roleBox.setItems(FXCollections.observableArrayList(
                "startup", "fournisseur", "formateur", "investisseur"
        ));
        roleBox.getSelectionModel().select("startup");

        // Ensure sections are in a consistent initial state for animation
        initSection(startupSection);
        initSection(supplierSection);
        initSection(investorSection);
        initSection(trainerSection);

        currentRole = roleBox.getValue();

        roleBox.valueProperty().addListener((obs, o, n) -> swapRoleSections(o, n));
        // show initial role without an exit animation
        showOnly(currentRole);

        setupTheme();
    }

    private void initSection(VBox section) {
        if (section == null) return;
        section.setOpacity(0);
        section.setTranslateY(8);
        section.setVisible(false);
        section.setManaged(false);
    }

    private VBox sectionForRole(String role) {
        if (role == null) return null;
        return switch (role) {
            case "startup" -> startupSection;
            case "fournisseur" -> supplierSection;
            case "investisseur" -> investorSection;
            case "formateur" -> trainerSection;
            default -> null;
        };
    }

    private void showOnly(String role) {
        VBox target = sectionForRole(role);
        // hide all
        hideImmediately(startupSection);
        hideImmediately(supplierSection);
        hideImmediately(investorSection);
        hideImmediately(trainerSection);
        // show target
        if (target != null) {
            target.setManaged(true);
            target.setVisible(true);
            target.setOpacity(1);
            target.setTranslateY(0);
        }
        clearRoleFieldsExcept(role);
    }

    private void hideImmediately(Node node) {
        if (node == null) return;
        node.setVisible(false);
        node.setManaged(false);
        node.setOpacity(0);
        node.setTranslateY(8);
    }

    private void showSection(VBox section) {
        if (section == null) return;

        section.setManaged(true);
        section.setVisible(true);

        section.setOpacity(0);
        section.setTranslateY(10);

        FadeTransition fade = new FadeTransition(SECTION_ANIM_DURATION, section);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(SECTION_ANIM_DURATION, section);
        slide.setFromY(10);
        slide.setToY(0);

        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setInterpolator(Interpolator.SPLINE(0.2, 0.8, 0.2, 1.0));
        pt.play();
    }

    private Animation hideSection(VBox section) {
        if (section == null) return null;
        if (!section.isVisible() && !section.isManaged()) return null;

        // IMPORTANT: free layout space immediately so the section doesn't reserve height while animating out.
        // We keep it visible so we can still animate opacity/translate.
        section.setManaged(false);

        FadeTransition fade = new FadeTransition(SECTION_ANIM_DURATION, section);
        fade.setFromValue(section.getOpacity());
        fade.setToValue(0);

        TranslateTransition slide = new TranslateTransition(SECTION_ANIM_DURATION, section);
        slide.setFromY(section.getTranslateY());
        slide.setToY(-6);

        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setInterpolator(Interpolator.EASE_BOTH);
        pt.setOnFinished(e -> {
            section.setVisible(false);
            section.setTranslateY(8);
        });
        return pt;
    }

    /**
     * Sequential swap: hide previous role section first, then show next.
     */
    private void swapRoleSections(String prevRole, String nextRole) {
        if (nextRole == null || nextRole.equals(prevRole)) return;

        VBox prevSection = sectionForRole(prevRole);
        VBox nextSection = sectionForRole(nextRole);

        // clear fields for roles we're leaving (prevents stale data)
        clearRoleFieldsExcept(nextRole);

        Animation exit = hideSection(prevSection);
        if (exit == null) {
            showSection(nextSection);
            currentRole = nextRole;
            return;
        }

        exit.setOnFinished(e -> {
            showSection(nextSection);
            currentRole = nextRole;
        });
        exit.play();
    }

    private void clearRoleFieldsExcept(String roleToKeep) {
        if (!"startup".equals(roleToKeep)) {
            startupCompanyField.clear();
            startupSectorField.clear();
            startupWebsiteField.clear();
            startupFoundingDatePicker.setValue(null);
            startupCompanyDescriptionArea.clear();
        }
        if (!"fournisseur".equals(roleToKeep)) {
            supplierBusinessTypeField.clear();
            supplierDeliveryZonesField.clear();
            supplierPaymentMethodsField.clear();
            supplierReturnPolicyArea.clear();
        }
        if (!"investisseur".equals(roleToKeep)) {
            investorSectorField.clear();
            investorMaxBudgetField.clear();
            investorYearsExperienceField.clear();
            investorRepresentedCompanyField.clear();
        }
        if (!"formateur".equals(roleToKeep)) {
            trainerSpecialtyField.clear();
            trainerHourlyRateField.clear();
            trainerAvailabilityField.clear();
            trainerCvUrlField.clear();
        }
    }

    @FXML
    public void signup() {
        errorLabel.setText("");

        String email = emailField.getText();
        String pass = passwordField.getText();
        String fullName = fullNameField.getText();
        String role = roleBox.getValue();

        List<String> errors = Services.validation().validateUserCreate(email, pass, fullName, role);
        errors.addAll(Services.validation().validateSignupRoleFields(role,
                startupCompanyField.getText(), trainerSpecialtyField.getText(),
                investorMaxBudgetField.getText(), investorYearsExperienceField.getText(),
                trainerHourlyRateField.getText()
        ));

        if (!errors.isEmpty()) {
            errorLabel.setText(String.join("\n", errors));
            return;
        }

        try {
            // refuse signup as admin
            if ("admin".equalsIgnoreCase(role)) {
                errorLabel.setText("Admin accounts can only be created by an admin.");
                return;
            }

            // unique email check
            if (Services.users().findByEmail(email.trim()).isPresent()) {
                errorLabel.setText("Email already exists.");
                return;
            }

            User u = new User();
            u.setEmail(email.trim());
            u.setPasswordHash(Services.auth().hashPassword(pass));
            u.setUserType(role);
            u.setFullName(fullName);
            u.setPhone(trimToNull(phoneField.getText()));
            u.setAddress(trimToNull(addressField.getText()));
            u.setBio(trimToNull(bioArea.getText()));
            u.setActive(true);

            // role specific mapping
            if ("startup".equals(role)) {
                u.setCompanyName(trimToNull(startupCompanyField.getText()));
                u.setSector(trimToNull(startupSectorField.getText()));
                u.setWebsite(trimToNull(startupWebsiteField.getText()));
                u.setFoundingDate(startupFoundingDatePicker.getValue());
                u.setCompanyDescription(trimToNull(startupCompanyDescriptionArea.getText()));
            } else if ("fournisseur".equals(role)) {
                u.setBusinessType(trimToNull(supplierBusinessTypeField.getText()));
                u.setDeliveryZones(trimToNull(supplierDeliveryZonesField.getText()));
                u.setPaymentMethods(trimToNull(supplierPaymentMethodsField.getText()));
                u.setReturnPolicy(trimToNull(supplierReturnPolicyArea.getText()));
            } else if ("investisseur".equals(role)) {
                u.setInvestmentSector(trimToNull(investorSectorField.getText()));
                u.setMaxBudget(parseBigDecimalOrNull(investorMaxBudgetField.getText()));
                u.setYearsExperience(parseIntegerOrNull(investorYearsExperienceField.getText()));
                u.setRepresentedCompany(trimToNull(investorRepresentedCompanyField.getText()));
            } else if ("formateur".equals(role)) {
                u.setSpecialty(trimToNull(trainerSpecialtyField.getText()));
                u.setHourlyRate(parseBigDecimalOrNull(trainerHourlyRateField.getText()));
                u.setAvailability(trimToNull(trainerAvailabilityField.getText()));
                u.setCvUrl(trimToNull(trainerCvUrlField.getText()));
            }

            Services.users().create(u);

            // auto-login after signup
            AppSession.setCurrentUser(u);

            Stage stage = (Stage) signupButton.getScene().getWindow();
            NavigationService nav = new NavigationService(stage);
            nav.goToProfile();

        } catch (SQLException e) {
            errorLabel.setText(e.getMessage());
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Integer parseIntegerOrNull(String s) {
        String t = trimToNull(s);
        if (t == null) return null;
        return Integer.parseInt(t);
    }

    private static BigDecimal parseBigDecimalOrNull(String s) {
        String t = trimToNull(s);
        if (t == null) return null;
        return new BigDecimal(t);
    }

    @FXML
    public void goToLogin() {
        Stage stage = (Stage) signupButton.getScene().getWindow();
        new NavigationService(stage).goToLogin();
    }

    private void setupTheme() {
        // Apply theme CSS (safe if scene isn't ready yet)
        VBox container = (VBox) emailField.getParent().getParent();
        container.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                String css = getClass().getResource("/com/bizhub/css/user-management.css").toExternalForm();
                if (!newScene.getStylesheets().contains(css)) {
                    newScene.getStylesheets().add(css);
                }
            }
        });
    }
}
