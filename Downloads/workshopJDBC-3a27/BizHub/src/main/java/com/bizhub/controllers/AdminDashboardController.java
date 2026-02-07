package com.bizhub.controllers;

import com.bizhub.models.User;
import com.bizhub.services.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminDashboardController {

    @FXML private Label adminNameLabel;

    @FXML private Text totalUsersText;
    @FXML private Text activeUsersText;
    @FXML private Text inactiveUsersText;
    @FXML private Text avgRatingText;

    @FXML private PieChart usersPie;

    @FXML private TableView<User> latestUsersTable;
    @FXML private TableColumn<User, String> colLatestName;
    @FXML private TableColumn<User, String> colLatestEmail;
    @FXML private TableColumn<User, String> colLatestType;
    @FXML private TableColumn<User, String> colLatestCreated;

    @FXML
    private VBox sidebar;

    @FXML
    private Label titleLabel;

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        // Highlight Dashboard in the included admin sidebar
        try {
            if (adminNameLabel != null && adminNameLabel.getScene() != null) {
                var rootNode = adminNameLabel.getScene().getRoot();
                var sidebar = rootNode.lookup(".admin-sidebar");
                NavigationService.setActiveNav(sidebar, NavigationService.ActiveNav.DASHBOARD);
            }
        } catch (Exception ignored) {
        }

        User me = AppSession.getCurrentUser();
        if (me != null) {
            adminNameLabel.setText(me.getFullName());
        }

        colLatestName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getFullName())));
        colLatestEmail.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getEmail())));
        colLatestType.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getUserType())));
        colLatestCreated.setCellValueFactory(c -> {
            var dt = c.getValue().getCreatedAt();
            var txt = dt == null ? "" : dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return new javafx.beans.property.SimpleStringProperty(txt);
        });

        setupSidebar();
        refresh();

        // Auto refresh every 30s
        javafx.animation.Timeline tl = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(30), e -> refresh())
        );
        tl.setCycleCount(javafx.animation.Animation.INDEFINITE);
        tl.play();
    }

    private void setupSidebar() {
        // Add logo (defensive)
        ImageView logo = new ImageView();
        try {
            var is = getClass().getResourceAsStream("/com/bizhub/images/site-images/logo.png");
            if (is != null) {
                logo.setImage(new Image(is));
            }
        } catch (Exception ignored) {
        }
        logo.setFitHeight(40);
        logo.setFitWidth(40);

        Label logoLabel = new Label("BizHub", logo);
        logoLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: var(--primary-gold);");
        if (sidebar != null) {
            sidebar.getChildren().add(0, logoLabel);
        }

        // Apply theme (safe)
        if (sidebar != null) {
            sidebar.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    String css = getClass().getResource("/com/bizhub/css/user-management.css").toExternalForm();
                    if (!newScene.getStylesheets().contains(css)) {
                        newScene.getStylesheets().add(css);
                    }
                }
            });
        }
    }

    private void refresh() {
        try {
            ReportService.DashboardStats stats = Services.reports().getAdminStats();
            totalUsersText.setText(String.valueOf(stats.totalUsers()));
            activeUsersText.setText(String.valueOf(stats.activeUsers()));
            inactiveUsersText.setText(String.valueOf(stats.inactiveUsers()));
            avgRatingText.setText(String.format("%.2f", stats.avgRatingAllFormations()));

            usersPie.setData(FXCollections.observableArrayList());
            for (Map.Entry<String, Integer> e : stats.usersByType().entrySet()) {
                usersPie.getData().add(new PieChart.Data(e.getKey(), e.getValue()));
            }

            latestUsersTable.setItems(FXCollections.observableArrayList(Services.users().findLatest(5)));
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void goUsers() {
        Stage stage = (Stage) latestUsersTable.getScene().getWindow();
        new NavigationService(stage).goToUserManagement();
    }

    @FXML
    public void goReviews() {
        Stage stage = (Stage) latestUsersTable.getScene().getWindow();
        new NavigationService(stage).goToReviews();
    }

    @FXML
    public void goProfile() {
        Stage stage = (Stage) latestUsersTable.getScene().getWindow();
        new NavigationService(stage).goToProfile();
    }

    @FXML
    public void goFormations() {
        Stage stage = (Stage) latestUsersTable.getScene().getWindow();
        new NavigationService(stage).goToFormations();
    }

    @FXML
    public void logout() {
        AppSession.clear();
        Services.auth().logout();
        Stage stage = (Stage) latestUsersTable.getScene().getWindow();
        new NavigationService(stage).goToLogin();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("Operation failed");
        a.setContentText(msg);
        a.showAndWait();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
