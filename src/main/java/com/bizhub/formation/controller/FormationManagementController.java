package com.bizhub.formation.controller;

import com.bizhub.formation.model.Formation;
import com.bizhub.participation.model.Participation;
import com.bizhub.user.model.User;
import com.bizhub.common.service.AppSession;
import com.bizhub.common.service.NavigationService;
import com.bizhub.common.service.Services;
import com.bizhub.formation.service.FormationContext;
import com.bizhub.participation.service.ParticipationContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FormationManagementController {

    @FXML private TextField searchField;
    @FXML private ListView<Formation> formationsList;
    @FXML private Label footerLabel;

    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    @FXML private BorderPane root;

    private final ObservableList<Formation> backing = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Swap sidebar for admins (prevents routing into user mode)
        if (AppSession.isAdmin()) {
            try {
                Parent n = FXMLLoader.load(getClass().getResource("/com/bizhub/fxml/admin-sidebar.fxml"));
                if (root != null) root.setLeft(n);
                NavigationService.setActiveNav(n, NavigationService.ActiveNav.FORMATIONS);
            } catch (Exception ignored) {
                // If it fails, app still works; buttons are guarded by role checks.
            }
        } else {
            if (root != null && root.getLeft() != null) {
                NavigationService.setActiveNav(root.getLeft(), NavigationService.ActiveNav.FORMATIONS);
            }
        }

        boolean admin = AppSession.isAdmin();
        if (addButton != null) addButton.setDisable(!admin);
        if (editButton != null) editButton.setDisable(!admin);
        if (deleteButton != null) deleteButton.setDisable(!admin);

        formationsList.setItems(backing);
        formationsList.setCellFactory(lv -> {
            FormationCardCell cell = new FormationCardCell();
            if (AppSession.isAdmin()) {
                cell.setParticiperCallback(this::participerAdmin);
            } else {
                cell.setParticiperCallback(this::participerStartup);
            }
            return cell;
        });

        formationsList.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                Formation f = formationsList.getSelectionModel().getSelectedItem();
                if (f != null) {
                    FormationContext.setSelectedFormationId(f.getFormationId());
                    Stage stage = (Stage) formationsList.getScene().getWindow();
                    new NavigationService(stage).goToFormationDetails();
                }
            }
        });

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        refresh();
    }

    @FXML
    public void refresh() {
        try {
            backing.setAll(Services.formations().findAll());
            applyFilter();
            footerLabel.setText("Loaded " + formationsList.getItems().size() + " formations");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void applyFilter() {
        String q = nullToEmpty(searchField.getText()).trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            formationsList.setItems(backing);
            return;
        }

        List<Formation> filtered = new ArrayList<>();
        for (Formation f : backing) {
            if (f.getTitle() != null && f.getTitle().toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(f);
            }
        }
        formationsList.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void editSelected() {
        if (!AppSession.isAdmin()) {
            info("Admin only");
            return;
        }
        Formation f = formationsList.getSelectionModel().getSelectedItem();
        if (f == null) {
            info("Select a formation first");
            return;
        }
        openForm(f, this::refresh);
    }

    @FXML
    public void deleteSelected() {
        if (!AppSession.isAdmin()) {
            info("Admin only");
            return;
        }
        Formation f = formationsList.getSelectionModel().getSelectedItem();
        if (f == null) {
            info("Select a formation first");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText("Delete formation?");
        confirm.setContentText("This will also delete related reviews (FK cascade).\n" + f.getTitle());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            Services.formations().delete(f.getFormationId());
            refresh();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void addFormation() {
        if (!AppSession.isAdmin()) {
            info("Admin only");
            return;
        }
        openForm(new Formation(), this::refresh);
    }

    /** Startup: un clic = participer à la formation (création directe). */
    private void participerStartup(Formation f) {
        User me = AppSession.getCurrentUser();
        if (me == null) {
            info("Connectez-vous pour participer.");
            return;
        }
        try {
            if (Services.participations().findByFormationAndUser(f.getFormationId(), me.getUserId()).isPresent()) {
                info("Vous participez déjà à cette formation.");
                return;
            }
            Participation p = new Participation();
            p.setFormationId(f.getFormationId());
            p.setUserId(me.getUserId());
            p.setDateAffectation(java.time.LocalDateTime.now());
            p.setRemarques(null);
            Services.participations().create(p);
            info("Vous participez maintenant à \"" + (f.getTitle() == null ? "" : f.getTitle()) + "\".");
            refresh();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    /** Admin: ouvre la liste des participations filtrée par cette formation. */
    private void participerAdmin(Formation f) {
        ParticipationContext.setFormationIdForParticipation(f.getFormationId());
        Stage stage = (Stage) formationsList.getScene().getWindow();
        new NavigationService(stage).goToParticipations();
    }

    @FXML
    public void goBack() {
        Stage stage = (Stage) formationsList.getScene().getWindow();
        if (AppSession.isAdmin()) {
            new NavigationService(stage).goToAdminDashboard();
        } else {
            new NavigationService(stage).goToProfile();
        }
    }

    // --- card cell ---
    private static final class FormationCardCell extends ListCell<Formation> {
        private final VBox card = new VBox(8);
        private final Label title = new Label();
        private final Label meta = new Label();
        private final Button participerButton = new Button("Participer");
        private java.util.function.Consumer<Formation> participerCallback;

        FormationCardCell() {
            card.getStyleClass().add("formation-card");
            card.setPadding(new Insets(16));

            title.getStyleClass().add("formation-title");
            meta.getStyleClass().add("formation-meta");
            participerButton.getStyleClass().add("btn-primary");
            participerButton.setOnAction(e -> {
                Formation f = getItem();
                if (f != null && participerCallback != null) participerCallback.accept(f);
            });

            card.getChildren().addAll(title, meta, participerButton);
        }

        void setParticiperCallback(java.util.function.Consumer<Formation> cb) {
            this.participerCallback = cb;
        }

        @Override
        protected void updateItem(Formation item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            title.setText(item.getTitle() == null ? "(untitled)" : item.getTitle());
            meta.setText("#" + item.getFormationId() + "   •   trainer_id: " + item.getTrainerId());
            setGraphic(card);
        }
    }

    private void openForm(Formation f, Runnable onSaved) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/formation-form.fxml"));
            Parent root = loader.load();
            FormationFormController ctl = loader.getController();
            ctl.setEditing(f, onSaved);

            Stage dialog = new Stage();
            dialog.initOwner(formationsList.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Formation");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("Operation failed");
        a.setContentText(msg);
        a.showAndWait();
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
