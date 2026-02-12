package com.bizhub.user.controller;

import com.bizhub.user.model.User;
import com.bizhub.common.service.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public class UserManagementController {

    private static final int PAGE_SIZE = 20;

    @FXML private BorderPane root;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private CheckBox activeOnlyCheck;

    @FXML private ListView<User> usersList;

    @FXML private Label pageLabel;
    @FXML private Label footerLabel;

    private final ObservableList<User> backingList = FXCollections.observableArrayList();
    private List<User> filtered = new ArrayList<>();
    private int pageIndex = 0;

    @FXML
    public void initialize() {
        // Ensure correct sidebar for admin vs non-admin
        if (AppSession.isAdmin()) {
            try {
                Parent n = FXMLLoader.load(getClass().getResource("/com/bizhub/fxml/admin-sidebar.fxml"));
                if (root != null) root.setLeft(n);
                // highlight current page in sidebar
                NavigationService.setActiveNav(n, NavigationService.ActiveNav.USERS);
            } catch (Exception ignored) {
            }
        } else {
            // highlight for non-admin sidebar if present
            if (root != null && root.getLeft() != null) {
                NavigationService.setActiveNav(root.getLeft(), NavigationService.ActiveNav.USERS);
            }
        }

        usersList.setCellFactory(lv -> new UserCardCell());

        typeFilter.setItems(FXCollections.observableArrayList("ALL", "startup", "fournisseur", "formateur", "investisseur", "admin"));
        typeFilter.getSelectionModel().select("ALL");

        searchField.textProperty().addListener((obs, oldV, newV) -> { pageIndex = 0; applyFiltersAndPaginate(); });
        typeFilter.valueProperty().addListener((obs, oldV, newV) -> { pageIndex = 0; applyFiltersAndPaginate(); });
        activeOnlyCheck.selectedProperty().addListener((obs, oldV, newV) -> { pageIndex = 0; applyFiltersAndPaginate(); });

        loadAll();

        // Auto-refresh every 30s
        javafx.animation.Timeline tl = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(30), e -> loadAll())
        );
        tl.setCycleCount(javafx.animation.Animation.INDEFINITE);
        tl.play();
    }

    @FXML
    public void editSelected() {
        if (!AppSession.isAdmin()) {
            info("Admin only");
            return;
        }
        User u = usersList.getSelectionModel().getSelectedItem();
        if (u == null) {
            info("Select a user first");
            return;
        }
        openUserForm(u, this::loadAll);
    }

    @FXML
    public void toggleActiveSelected() {
        if (!AppSession.isAdmin()) {
            info("Admin only");
            return;
        }
        User u = usersList.getSelectionModel().getSelectedItem();
        if (u == null) {
            info("Select a user first");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm");
        confirm.setHeaderText("Toggle active status?");
        confirm.setContentText("User: " + u.getEmail());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            Services.users().setActive(u.getUserId(), !u.isActive());
            loadAll();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void exportCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export users to CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showSaveDialog(usersList.getScene().getWindow());
        if (f == null) return;

        try (FileWriter w = new FileWriter(f)) {
            w.write("user_id,full_name,email,user_type,company_name,sector,is_active,created_at\n");
            for (User u : filtered) {
                w.write(u.getUserId() + "," + esc(u.getFullName()) + "," + esc(u.getEmail()) + "," + esc(u.getUserType()) + "," +
                        esc(u.getCompanyName()) + "," + esc(u.getSector()) + "," + u.isActive() + "," +
                        (u.getCreatedAt() == null ? "" : u.getCreatedAt()) + "\n");
            }
            footerLabel.setText("Exported " + filtered.size() + " users to: " + f.getAbsolutePath());
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void goBack() {
        Stage stage = (Stage) usersList.getScene().getWindow();
        new NavigationService(stage).goToAdminDashboard();
    }

    @FXML
    public void onRefresh() {
        loadAll();
    }

    @FXML
    public void addUser() {
        if (!AppSession.isAdmin()) {
            info("Admin only");
            return;
        }
        openUserForm(new User(), this::loadAll);
    }

    @FXML
    public void prevPage() {
        if (pageIndex > 0) {
            pageIndex--;
            applyFiltersAndPaginate();
        }
    }

    @FXML
    public void nextPage() {
        int pageCount = pageCount();
        if (pageIndex < pageCount - 1) {
            pageIndex++;
            applyFiltersAndPaginate();
        }
    }

    private void loadAll() {
        try {
            backingList.setAll(Services.users().findAll());
            applyFiltersAndPaginate();
            footerLabel.setText("Loaded " + backingList.size() + " users");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void applyFiltersAndPaginate() {
        filtered = applyFilters(backingList);

        int start = pageIndex * PAGE_SIZE;
        if (start >= filtered.size() && pageIndex > 0) {
            pageIndex = Math.max(0, pageCount() - 1);
            start = pageIndex * PAGE_SIZE;
        }
        int end = Math.min(filtered.size(), start + PAGE_SIZE);

        List<User> page = filtered.subList(Math.min(start, filtered.size()), end);
        usersList.setItems(FXCollections.observableArrayList(page));

        pageLabel.setText("Page " + (pageIndex + 1) + " / " + Math.max(1, pageCount()));
    }

    private int pageCount() {
        if (filtered == null || filtered.isEmpty()) return 1;
        return (int) Math.ceil(filtered.size() / (double) PAGE_SIZE);
    }

    private List<User> applyFilters(List<User> source) {
        String q = nullToEmpty(searchField.getText()).trim().toLowerCase(Locale.ROOT);
        String type = typeFilter.getValue();
        boolean activeOnly = activeOnlyCheck.isSelected();

        List<User> out = new ArrayList<>();
        for (User u : source) {
            if (activeOnly && !u.isActive()) continue;
            if (type != null && !"ALL".equals(type) && (u.getUserType() == null || !type.equalsIgnoreCase(u.getUserType()))) continue;

            if (!q.isEmpty()) {
                boolean match = containsIgnoreCase(u.getFullName(), q) ||
                        containsIgnoreCase(u.getEmail(), q) ||
                        containsIgnoreCase(u.getCompanyName(), q);
                if (!match) continue;
            }
            out.add(u);
        }
        return out;
    }

    private void openUserForm(User u, Runnable onSaved) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/user-form.fxml"));
            Parent root = loader.load();
            UserFormController ctl = loader.getController();
            ctl.setEditing(u, onSaved);

            Stage dialog = new Stage();
            dialog.initOwner(usersList.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("User");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // --- card cell ---
    private static final class UserCardCell extends ListCell<User> {
        private final VBox card = new VBox(10);

        private final HBox top = new HBox(12);
        private final StackPane avatar = new StackPane();
        private final ImageView avatarImg = new ImageView();
        private final Label avatarTxt = new Label();

        private final VBox head = new VBox(2);
        private final Label name = new Label();
        private final Label meta = new Label();
        private final Region spacer = new Region();
        private final Label status = new Label();

        UserCardCell() {
            card.getStyleClass().add("user-card");
            card.setPadding(new Insets(16));

            avatar.getStyleClass().add("user-avatar");
            avatar.setMinSize(44, 44);
            avatar.setPrefSize(44, 44);
            avatar.setMaxSize(44, 44);

            avatarImg.setFitWidth(44);
            avatarImg.setFitHeight(44);
            avatarImg.setPreserveRatio(true);
            avatarImg.setSmooth(true);

            avatarTxt.getStyleClass().add("user-avatar-text");
            avatar.getChildren().addAll(avatarImg, avatarTxt);

            name.getStyleClass().add("user-name");
            meta.getStyleClass().add("user-meta");

            head.getChildren().addAll(name, meta);

            status.getStyleClass().add("hint");
            status.setStyle("-fx-font-weight: 900;");

            HBox.setHgrow(spacer, Priority.ALWAYS);
            top.setAlignment(Pos.CENTER_LEFT);
            top.getChildren().addAll(avatar, head, spacer, status);

            card.getChildren().add(top);
        }

        @Override
        protected void updateItem(User item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            String fullName = item.getFullName() == null || item.getFullName().isBlank() ? "User" : item.getFullName();
            name.setText(fullName);

            String email = item.getEmail() == null ? "" : item.getEmail();
            String type = item.getUserType() == null ? "" : item.getUserType();
            String company = item.getCompanyName() == null ? "" : item.getCompanyName();
            meta.setText(email + (type.isBlank() ? "" : ("   •   " + type)) + (company.isBlank() ? "" : ("   •   " + company)));

            status.setText(item.isActive() ? "ACTIVE" : "INACTIVE");
            status.setStyle(item.isActive()
                    ? "-fx-text-fill: #16a34a; -fx-font-weight: 900;"
                    : "-fx-text-fill: #dc2626; -fx-font-weight: 900;"
            );

            setAvatar(item.getAvatarUrl(), fullName);

            setGraphic(card);
        }

        private void setAvatar(String avatarUrl, String displayName) {
            Image img = loadAvatarImage(avatarUrl);
            if (img != null) {
                avatarImg.setImage(img);
                avatarImg.setVisible(true);
                avatarTxt.setVisible(false);
            } else {
                avatarImg.setImage(null);
                avatarImg.setVisible(false);
                avatarTxt.setText(initials(displayName));
                avatarTxt.setVisible(true);
            }
        }

        private Image loadAvatarImage(String avatarUrl) {
            if (avatarUrl == null || avatarUrl.isBlank()) return null;

            try {
                var res = UserManagementController.class.getClassLoader().getResource(avatarUrl);
                if (res != null) return new Image(res.toExternalForm(), true);
            } catch (Exception ignored) {
            }

            try {
                File f = new File("src/main/resources/" + avatarUrl);
                if (f.exists()) return new Image(f.toURI().toString(), true);
            } catch (Exception ignored) {
            }

            return null;
        }

        private static String initials(String s) {
            if (s == null) return "?";
            String t = s.trim();
            if (t.isEmpty()) return "?";
            String[] parts = t.split("\\s+");
            if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
            String a = parts[0].isEmpty() ? "" : parts[0].substring(0, 1);
            String b = parts[1].isEmpty() ? "" : parts[1].substring(0, 1);
            String res = (a + b).trim();
            return res.isEmpty() ? "?" : res.toUpperCase(Locale.ROOT);
        }
    }

    private static boolean containsIgnoreCase(String src, String q) {
        if (src == null) return false;
        return src.toLowerCase(Locale.ROOT).contains(q);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String esc(String s) {
        if (s == null) return "";
        String v = s;
        if (v.contains(",") || v.contains("\n") || v.contains("\r")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
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
