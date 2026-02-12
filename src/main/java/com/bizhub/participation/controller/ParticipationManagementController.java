package com.bizhub.participation.controller;

import com.bizhub.formation.model.Formation;
import com.bizhub.participation.model.Participation;
import com.bizhub.participation.service.ParticipationContext;
import com.bizhub.user.model.User;
import com.bizhub.common.service.AppSession;
import com.bizhub.common.service.NavigationService;
import com.bizhub.common.service.Services;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ParticipationManagementController {

    @FXML private TextField searchField;
    @FXML private ComboBox<Formation> formationFilter;
    @FXML private ListView<Participation> participationsList;
    @FXML private Label footerLabel;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private BorderPane root;

    private final ObservableList<Participation> backing = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (AppSession.isAdmin()) {
            try {
                Parent n = FXMLLoader.load(getClass().getResource("/com/bizhub/fxml/admin-sidebar.fxml"));
                if (root != null) root.setLeft(n);
                NavigationService.setActiveNav(n, NavigationService.ActiveNav.PARTICIPATIONS);
            } catch (Exception ignored) {
            }
        } else {
            if (root != null && root.getLeft() != null) {
                NavigationService.setActiveNav(root.getLeft(), NavigationService.ActiveNav.PARTICIPATIONS);
            }
        }

        // Align permissions with formations: only admins can add/edit/delete participations
        boolean admin = AppSession.isAdmin();
        if (addButton != null) addButton.setDisable(!admin);
        if (editButton != null) editButton.setDisable(!admin);
        if (deleteButton != null) deleteButton.setDisable(!admin);

        formationFilter.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Formation item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getTitle() + " (#" + item.getFormationId() + ")"));
            }
        });
        formationFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Formation item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getTitle() + " (#" + item.getFormationId() + ")"));
            }
        });

        try {
            List<Formation> formations = Services.formations().findAll();
            Formation all = new Formation(0, "Toutes", 0);
            ObservableList<Formation> items = FXCollections.observableArrayList();
            items.add(all);
            items.addAll(formations);
            formationFilter.setItems(items);
            Integer preSelected = ParticipationContext.getFormationIdForParticipation();
            if (preSelected != null) {
                for (Formation f : items) {
                    if (f.getFormationId() == preSelected) {
                        formationFilter.getSelectionModel().select(f);
                        ParticipationContext.clear();
                        break;
                    }
                }
                if (formationFilter.getSelectionModel().getSelectedItem() == null) {
                    formationFilter.getSelectionModel().select(all);
                }
            } else {
                formationFilter.getSelectionModel().select(all);
            }
        } catch (SQLException e) {
            // ignore
        }

        participationsList.setItems(backing);
        participationsList.setCellFactory(lv -> new ParticipationCardCell());

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        formationFilter.valueProperty().addListener((obs, o, n) -> loadData());

        loadData();
    }

    private void loadData() {
        try {
            User me = AppSession.getCurrentUser();
            List<Participation> list;
            Formation ff = formationFilter.getValue();
            boolean filterByFormation = ff != null && ff.getFormationId() != 0;

            if (AppSession.isAdmin()) {
                list = filterByFormation
                        ? Services.participations().findByFormationId(ff.getFormationId())
                        : Services.participations().findAll();
            } else {
                if (me == null) {
                    list = new ArrayList<>();
                } else {
                    list = Services.participations().findByUserId(me.getUserId());
                    if (filterByFormation) {
                        list = list.stream().filter(p -> p.getFormationId() == ff.getFormationId()).toList();
                    }
                }
            }

            backing.setAll(list);
            applyFilter();
            footerLabel.setText(participationsList.getItems().size() + " participation(s)");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void applyFilter() {
        String q = nullToEmpty(searchField.getText()).trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            participationsList.setItems(backing);
            return;
        }
        List<Participation> filtered = new ArrayList<>();
        for (Participation p : backing) {
            String formationTitle = getFormationTitle(p.getFormationId());
            String userName = getUserName(p.getUserId());
            String remarques = nullToEmpty(p.getRemarques());
            if (formationTitle.toLowerCase(Locale.ROOT).contains(q)
                    || userName.toLowerCase(Locale.ROOT).contains(q)
                    || remarques.toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(p);
            }
        }
        participationsList.setItems(FXCollections.observableArrayList(filtered));
    }

    private String getFormationTitle(int formationId) {
        try {
            return Services.formations().findById(formationId).map(Formation::getTitle).orElse("#" + formationId);
        } catch (SQLException e) {
            return "#" + formationId;
        }
    }

    private String getUserName(int userId) {
        try {
            return Services.users().findById(userId).map(User::getFullName).orElse("#" + userId);
        } catch (SQLException e) {
            return "#" + userId;
        }
    }

    @FXML
    public void refresh() {
        loadData();
    }

    @FXML
    public void addParticipation() {
        if (!AppSession.isAdmin()) {
            info("Admin only");
            return;
        }
        ParticipationContext.setFormationIdForParticipation(null);
        openForm(new Participation(), this::loadData);
    }

    @FXML
    public void editSelected() {
        if (!AppSession.isAdmin()) {
            info("Admin only");
            return;
        }
        Participation p = participationsList.getSelectionModel().getSelectedItem();
        if (p == null) {
            info("Sélectionnez une participation.");
            return;
        }
        openForm(p, this::loadData);
    }

    @FXML
    public void deleteSelected() {
        if (!AppSession.isAdmin()) {
            info("Admin only");
            return;
        }
        Participation p = participationsList.getSelectionModel().getSelectedItem();
        if (p == null) {
            info("Sélectionnez une participation.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer");
        confirm.setHeaderText("Supprimer cette participation ?");
        confirm.setContentText("ID #" + p.getId());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            Services.participations().delete(p.getId());
            loadData();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void openForm(Participation p, Runnable onSaved) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/participation-form.fxml"));
            Parent root = loader.load();
            ParticipationFormController ctl = loader.getController();
            ctl.setEditing(p, onSaved);

            Stage dialog = new Stage();
            dialog.initOwner(participationsList.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Participation");
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private static final class ParticipationCardCell extends ListCell<Participation> {
        private final VBox card = new VBox(8);
        private final Label titleLabel = new Label();
        private final Label metaLabel = new Label();
        private final Label remarquesLabel = new Label();

        ParticipationCardCell() {
            card.getStyleClass().add("formation-card");
            card.setPadding(new Insets(16));
            titleLabel.getStyleClass().add("formation-title");
            metaLabel.getStyleClass().add("formation-meta");
            remarquesLabel.getStyleClass().add("formation-meta");
            remarquesLabel.setWrapText(true);
            card.getChildren().addAll(titleLabel, metaLabel, remarquesLabel);
        }

        @Override
        protected void updateItem(Participation item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            try {
                String formationTitle = Services.formations().findById(item.getFormationId()).map(Formation::getTitle).orElse("#" + item.getFormationId());
                String userName = Services.users().findById(item.getUserId()).map(User::getFullName).orElse("#" + item.getUserId());
                titleLabel.setText(formationTitle);
                String dateStr = item.getDateAffectation() == null ? "" : item.getDateAffectation().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                metaLabel.setText("#" + item.getId() + "   •   " + userName + "   •   " + dateStr);
                String rem = item.getRemarques();
                remarquesLabel.setText(rem == null || rem.isBlank() ? "" : (rem.length() > 120 ? rem.substring(0, 117) + "..." : rem));
            } catch (SQLException e) {
                titleLabel.setText("#" + item.getId());
                metaLabel.setText("");
                remarquesLabel.setText("");
            }
            setGraphic(card);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText("Échec de l'opération");
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
