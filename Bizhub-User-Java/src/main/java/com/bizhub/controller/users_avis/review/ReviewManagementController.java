package com.bizhub.controller.users_avis.review;

import com.bizhub.model.services.common.service.*;
import com.bizhub.model.users_avis.formation.Formation;
import com.bizhub.model.users_avis.review.Review;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReviewManagementController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> ratingFilter;
    @FXML private ComboBox<Formation> formationFilter;
    @FXML private Label statsLabel;

    @FXML private ListView<Review> reviewsList;

    @FXML private Button deleteButton;

    private final ObservableList<Review> backing = FXCollections.observableArrayList();

    @FXML private BorderPane root;

    @FXML
    public void initialize() {
        if (AppSession.isAdmin()) {
            try {
                Parent n = NavigationService.loadFxmlSafe("/com/bizhub/fxml/admin-sidebar.fxml");
                if (root != null) root.setLeft(n);
                NavigationService.setActiveNav(n, NavigationService.ActiveNav.REVIEWS);
            } catch (Exception ignored) {
            }
        } else {
            if (root != null && root.getLeft() != null) {
                NavigationService.setActiveNav(root.getLeft(), NavigationService.ActiveNav.REVIEWS);
            }
        }

        reviewsList.setItems(backing);
        reviewsList.setCellFactory(lv -> new ReviewCardCell());

        ratingFilter.setItems(FXCollections.observableArrayList("ALL", "1", "2", "3", "4", "5"));
        ratingFilter.getSelectionModel().select("ALL");

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
            Formation all = new Formation(0, "ALL", 0);
            ObservableList<Formation> items = FXCollections.observableArrayList();
            items.add(all);
            items.addAll(formations);
            formationFilter.setItems(items);
            formationFilter.getSelectionModel().select(all);
        } catch (SQLException e) {
            // ignore; reviews can still load
        }

        deleteButton.setDisable(!AppSession.isAdmin());

        searchField.textProperty().addListener((obs, o, n) -> applyClientFilters());
        ratingFilter.valueProperty().addListener((obs, o, n) -> reload());
        formationFilter.valueProperty().addListener((obs, o, n) -> reload());

        reload();
    }

    @FXML
    public void refresh() {
        reload();
    }

    private void reload() {
        try {
            String rf = ratingFilter.getValue();
            Formation ff = formationFilter.getValue();

            List<Review> rows;

            boolean hasFormation = ff != null && ff.getFormationId() != 0;
            boolean hasRating = rf != null && !"ALL".equals(rf);

            if (hasFormation && hasRating) {
                rows = Services.reviews().findByFormation(ff.getFormationId(), 2000, 0);
                int rr = Integer.parseInt(rf);
                rows = rows.stream().filter(r -> r.getRating() != null && r.getRating() == rr).toList();
            } else if (hasFormation) {
                rows = Services.reviews().findByFormation(ff.getFormationId(), 2000, 0);
            } else if (hasRating) {
                rows = Services.reviews().findByRating(Integer.parseInt(rf), 2000, 0);
            } else {
                rows = Services.reviews().findAllWithJoins(2000, 0);
            }

            backing.setAll(rows);
            applyClientFilters();

            statsLabel.setText("Total reviews: " + reviewsList.getItems().size());
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void applyClientFilters() {
        String q = nullToEmpty(searchField.getText()).trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            reviewsList.setItems(backing);
            return;
        }

        ObservableList<Review> filtered = FXCollections.observableArrayList();
        for (Review r : backing) {
            boolean match = contains(r.getReviewerName(), q) || contains(r.getReviewerEmail(), q) ||
                    contains(r.getFormationTitle(), q) || contains(r.getComment(), q);
            if (match) filtered.add(r);
        }
        reviewsList.setItems(filtered);
    }

    @FXML
    public void viewSelected() {
        Review r = reviewsList.getSelectionModel().getSelectedItem();
        if (r == null) {
            info("Select a review first");
            return;
        }

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Review detail");
        a.setHeaderText(r.getReviewerName() + " → " + r.getFormationTitle() + " (" + (r.getRating() == null ? "" : r.getRating()) + ")");
        a.setContentText(nullToEmpty(r.getComment()));
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }

    @FXML
    public void deleteSelected() {
        if (!AppSession.isAdmin()) {
            info("Admin only");
            return;
        }

        Review r = reviewsList.getSelectionModel().getSelectedItem();
        if (r == null) {
            info("Select a review first");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm deletion");
        confirm.setHeaderText("Delete selected review?");
        confirm.setContentText("This cannot be undone.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            Services.reviews().delete(r.getAvisId());
            reload();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void exportCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export reviews to CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showSaveDialog(reviewsList.getScene().getWindow());
        if (f == null) return;

        try (FileWriter w = new FileWriter(f)) {
            w.write("avis_id,reviewer_name,reviewer_email,formation_title,rating,created_at,comment\n");
            for (Review r : reviewsList.getItems()) {
                w.write(r.getAvisId() + "," + esc(r.getReviewerName()) + "," + esc(r.getReviewerEmail()) + "," +
                        esc(r.getFormationTitle()) + "," + (r.getRating() == null ? "" : r.getRating()) + "," +
                        (r.getCreatedAt() == null ? "" : r.getCreatedAt()) + "," + esc(r.getComment()) + "\n");
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void goBack() {
        Stage stage = (Stage) reviewsList.getScene().getWindow();
        if (AppSession.isAdmin()) new NavigationService(stage).goToAdminDashboard();
        else new NavigationService(stage).goToProfile();
    }

    // --- Card cell ---

    private static final class ReviewCardCell extends ListCell<Review> {
        private final VBox card = new VBox(10);

        private final HBox topRow = new HBox(12);
        private final StackPane avatar = new StackPane();
        private final ImageView avatarImage = new ImageView();
        private final Label avatarLabel = new Label();

        private final VBox headText = new VBox(2);
        private final Label nameLabel = new Label();
        private final Label metaLabel = new Label();

        private final Region spacer = new Region();

        private final VBox body = new VBox(8);
        private final HBox starsRow = new HBox(2);
        private final Label[] stars = new Label[5];
        private final Label formationLabel = new Label();
        private final Label reviewText = new Label();

        ReviewCardCell() {
            getStyleClass().add("review-cell");

            card.getStyleClass().addAll("review-card");
            card.setPadding(new Insets(16));

            avatar.getStyleClass().add("review-avatar");
            avatar.setMinSize(44, 44);
            avatar.setPrefSize(44, 44);
            avatar.setMaxSize(44, 44);

            avatarImage.setFitWidth(44);
            avatarImage.setFitHeight(44);
            avatarImage.setPreserveRatio(true);
            avatarImage.setSmooth(true);
            avatarImage.getStyleClass().add("review-avatar-img");

            avatarLabel.getStyleClass().add("review-avatar-text");
            avatar.getChildren().addAll(avatarImage, avatarLabel);

            nameLabel.getStyleClass().add("review-name");
            metaLabel.getStyleClass().add("review-meta");
            headText.getChildren().addAll(nameLabel, metaLabel);

            HBox.setHgrow(spacer, Priority.ALWAYS);
            topRow.setAlignment(Pos.CENTER_LEFT);
            topRow.getChildren().addAll(avatar, headText, spacer);

            starsRow.getStyleClass().add("review-stars");
            starsRow.setAlignment(Pos.CENTER_LEFT);
            for (int i = 0; i < 5; i++) {
                Label s = new Label("★");
                s.getStyleClass().add("star");
                stars[i] = s;
                starsRow.getChildren().add(s);
            }

            formationLabel.getStyleClass().add("review-formation");

            reviewText.getStyleClass().add("review-text");
            reviewText.setWrapText(true);
            reviewText.setMaxWidth(Double.MAX_VALUE);

            body.getChildren().addAll(starsRow, formationLabel, reviewText);

            card.getChildren().addAll(topRow, body);
        }

        @Override
        protected void updateItem(Review item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            String name = pickDisplayName(item);
            nameLabel.setText(name);

            String dt = item.getCreatedAt() == null
                    ? ""
                    : item.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy • HH:mm", Locale.ENGLISH));
            metaLabel.setText(dt.isBlank() ? ("Review #" + item.getAvisId()) : (dt + "   •   #" + item.getAvisId()));

            setAvatar(item.getReviewerAvatarUrl(), name);

            int rating = item.getRating() == null ? 0 : Math.max(0, Math.min(5, item.getRating()));
            for (int i = 0; i < 5; i++) {
                stars[i].getStyleClass().removeAll("filled", "empty");
                stars[i].getStyleClass().add(i < rating ? "filled" : "empty");
            }

            formationLabel.setText(nullToEmpty(item.getFormationTitle()));

            String txt = nullToEmpty(item.getComment()).trim();
            reviewText.setText(txt.isBlank() ? "No comment provided." : txt);

            setGraphic(card);
        }

        private void setAvatar(String avatarUrl, String displayName) {
            Image img = loadAvatarImage(avatarUrl);
            if (img != null) {
                avatarImage.setImage(img);
                avatarImage.setVisible(true);
                avatarLabel.setVisible(false);
            } else {
                avatarImage.setImage(null);
                avatarImage.setVisible(false);
                avatarLabel.setText(initials(displayName));
                avatarLabel.setVisible(true);
            }
        }

        private Image loadAvatarImage(String avatarUrl) {
            if (avatarUrl == null || avatarUrl.isBlank()) return null;

            // We store paths like: com/bizhub/images/avatars/xxx.png
            // Try classpath first
            try {
                var res = ReviewManagementController.class.getClassLoader().getResource(avatarUrl);
                if (res != null) {
                    return new Image(res.toExternalForm(), true);
                }
            } catch (Exception ignored) {
            }

            // Fallback to dev-time file path
            try {
                File f = new File("src/main/resources/" + avatarUrl);
                if (f.exists()) {
                    return new Image(f.toURI().toString(), true);
                }
            } catch (Exception ignored) {
            }

            return null;
        }

        private static String pickDisplayName(Review r) {
            String n = nullToEmpty(r.getReviewerName()).trim();
            if (!n.isBlank()) return n;
            String e = nullToEmpty(r.getReviewerEmail()).trim();
            return e.isBlank() ? "Anonymous" : e;
        }

        private static String initials(String s) {
            if (s == null) return "?";
            String t = s.trim();
            if (t.isEmpty()) return "?";
            String[] parts = t.split("\\s+");
            if (parts.length == 1) {
                return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
            }
            String a = parts[0].isEmpty() ? "" : parts[0].substring(0, 1);
            String b = parts[1].isEmpty() ? "" : parts[1].substring(0, 1);
            String res = (a + b).trim();
            return res.isEmpty() ? "?" : res.toUpperCase(Locale.ROOT);
        }
    }

    // --- utils ---

    private static boolean contains(String s, String q) {
        if (s == null) return false;
        return s.toLowerCase(Locale.ROOT).contains(q);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String esc(String s) {
        if (s == null) return "";
        String v = s;
        boolean needsQuotes = v.contains(",") || v.contains("\n") || v.contains("\r") || v.contains("\"");
        if (needsQuotes) {
            v = v.replace("\"", "\"\"");
            return '"' + v + '"';
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
