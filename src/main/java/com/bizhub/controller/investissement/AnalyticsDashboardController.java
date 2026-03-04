package com.bizhub.controller.investissement;

import com.bizhub.model.services.investissement.*;
import javafx.application.Platform;
import com.bizhub.controller.users_avis.user.SidebarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsDashboardController {

    @FXML private SidebarController sidebarController;
    @FXML private Label totalProjectsLabel;
    @FXML private Label totalInvestmentsLabel;
    @FXML private Label totalVolumeLabel;
    @FXML private Label totalInvestorsLabel;
    @FXML private Label activeDealsLabel;
    @FXML private Label activeNegotiationsLabel;

    @FXML private PieChart sectorPieChart;
    @FXML private LineChart<String, Number> volumeLineChart;
    @FXML private BarChart<String, Number> projectBarChart;
    @FXML private PieChart statusPieChart;

    @FXML private VBox cryptoContainer;
    @FXML private VBox newsContainer;

    private ChartDataService chartDataService;
    private CoinGeckoService coinGeckoService;
    private AlphaVantageService alphaVantageService;
    private GNewsService gNewsService;
    private NumberFormat currencyFormat;

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("analytics");
        chartDataService = new ChartDataService();
        coinGeckoService = new CoinGeckoService();
        alphaVantageService = new AlphaVantageService();
        gNewsService = new GNewsService();
        currencyFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
        currencyFormat.setMaximumFractionDigits(2);

        loadPlatformStats();
        loadSectorChart();
        loadVolumeChart();
        loadProjectComparison();
        loadStatusChart();
        loadCryptoPrices();
        loadMarketNews();
    }

    private void loadPlatformStats() {
        javafx.concurrent.Task<Map<String, Object>> task = new javafx.concurrent.Task<>() {
            @Override
            protected Map<String, Object> call() {
                return chartDataService.getPlatformStats();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            Map<String, Object> stats = task.getValue();
            animateStatLabel(totalProjectsLabel, String.valueOf(stats.getOrDefault("totalProjects", 0)));
            animateStatLabel(totalInvestmentsLabel, String.valueOf(stats.getOrDefault("totalInvestments", 0)));

            Object vol = stats.getOrDefault("totalVolume", 0.0);
            animateStatLabel(totalVolumeLabel, currencyFormat.format(((Number) vol).doubleValue()) + " TND");

            animateStatLabel(totalInvestorsLabel, String.valueOf(stats.getOrDefault("totalInvestors", 0)));
            animateStatLabel(activeDealsLabel, String.valueOf(stats.getOrDefault("totalDeals", 0)));
            animateStatLabel(activeNegotiationsLabel, String.valueOf(stats.getOrDefault("activeNegotiations", 0)));
        }));

        new Thread(task).start();
    }

    private static final String[] PIE_COLORS = {
            "#3B82F6", "#10B981", "#FFB84D", "#8B5CF6", "#EC4899",
            "#F59E0B", "#06B6D4", "#EF4444", "#84CC16", "#F97316"
    };

    private static final Map<String, String> STATUS_COLORS = Map.of(
            "pending", "#F59E0B", "funded", "#10B981",
            "in_progress", "#3B82F6", "complete", "#8B5CF6"
    );

    private void loadSectorChart() {
        javafx.concurrent.Task<Map<String, Double>> task = new javafx.concurrent.Task<>() {
            @Override
            protected Map<String, Double> call() {
                return chartDataService.getInvestmentsBySector();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            sectorPieChart.getData().clear();
            sectorPieChart.setTitle(null);
            sectorPieChart.setLabelsVisible(true);
            for (Map.Entry<String, Double> entry : task.getValue().entrySet()) {
                sectorPieChart.getData().add(new PieChart.Data(
                        entry.getKey() + "\n" + currencyFormat.format(entry.getValue()) + " TND",
                        entry.getValue()));
            }
            if (sectorPieChart.getData().isEmpty()) {
                sectorPieChart.getData().add(new PieChart.Data("No data yet", 1));
            }
            colorPieChart(sectorPieChart, PIE_COLORS);
            addPieTooltips(sectorPieChart);
        }));

        new Thread(task).start();
    }

    private void loadVolumeChart() {
        javafx.concurrent.Task<Map<String, Double>> task = new javafx.concurrent.Task<>() {
            @Override
            protected Map<String, Double> call() {
                return chartDataService.getMonthlyInvestmentVolume();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            volumeLineChart.getData().clear();
            volumeLineChart.setTitle(null);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Volume (TND)");
            for (Map.Entry<String, Double> entry : task.getValue().entrySet()) {
                XYChart.Data<String, Number> d = new XYChart.Data<>(entry.getKey(), entry.getValue());
                series.getData().add(d);
            }
            volumeLineChart.getData().add(series);

            Platform.runLater(() -> {
                Node line = volumeLineChart.lookup(".chart-series-line");
                if (line != null) line.setStyle("-fx-stroke: #10B981; -fx-stroke-width: 3;");
                for (Node symbol : volumeLineChart.lookupAll(".chart-line-symbol")) {
                    symbol.setStyle("-fx-background-color: #10B981, white; -fx-background-radius: 6; -fx-padding: 4;");
                }
                for (var d : series.getData()) {
                    Tooltip.install(d.getNode(), new Tooltip(
                            d.getXValue() + ": " + currencyFormat.format(d.getYValue()) + " TND"));
                    d.getNode().setCursor(Cursor.HAND);
                }
            });
        }));

        new Thread(task).start();
    }

    private void loadProjectComparison() {
        javafx.concurrent.Task<Map<String, double[]>> task = new javafx.concurrent.Task<>() {
            @Override
            protected Map<String, double[]> call() {
                return chartDataService.getProjectComparison();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            projectBarChart.getData().clear();
            projectBarChart.setTitle(null);
            XYChart.Series<String, Number> budgetSeries = new XYChart.Series<>();
            budgetSeries.setName("Budget");
            XYChart.Series<String, Number> investedSeries = new XYChart.Series<>();
            investedSeries.setName("Invested");
            for (Map.Entry<String, double[]> entry : task.getValue().entrySet()) {
                budgetSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[0]));
                investedSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[1]));
            }
            projectBarChart.getData().addAll(budgetSeries, investedSeries);

            Platform.runLater(() -> {
                for (var d : budgetSeries.getData()) {
                    if (d.getNode() != null) {
                        d.getNode().setStyle("-fx-bar-fill: #3B82F6;");
                        Tooltip.install(d.getNode(), new Tooltip(
                                d.getXValue() + " budget: " + currencyFormat.format(d.getYValue()) + " TND"));
                    }
                }
                for (var d : investedSeries.getData()) {
                    if (d.getNode() != null) {
                        d.getNode().setStyle("-fx-bar-fill: #10B981;");
                        Tooltip.install(d.getNode(), new Tooltip(
                                d.getXValue() + " invested: " + currencyFormat.format(d.getYValue()) + " TND"));
                    }
                }
            });
        }));

        new Thread(task).start();
    }

    private void loadStatusChart() {
        javafx.concurrent.Task<Map<String, Integer>> task = new javafx.concurrent.Task<>() {
            @Override
            protected Map<String, Integer> call() {
                return chartDataService.getProjectsByStatus();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            statusPieChart.getData().clear();
            statusPieChart.setTitle(null);
            for (Map.Entry<String, Integer> entry : task.getValue().entrySet()) {
                String label = entry.getKey().replace("_", " ").toUpperCase();
                statusPieChart.getData().add(new PieChart.Data(
                        label + " (" + entry.getValue() + ")", entry.getValue()));
            }
            if (statusPieChart.getData().isEmpty()) {
                statusPieChart.getData().add(new PieChart.Data("No data yet", 1));
            }

            int i = 0;
            for (PieChart.Data d : statusPieChart.getData()) {
                String key = d.getName().split(" \\(")[0].toLowerCase().replace(" ", "_");
                String color = STATUS_COLORS.getOrDefault(key, PIE_COLORS[i % PIE_COLORS.length]);
                d.getNode().setStyle("-fx-pie-color: " + color + ";");
                i++;
            }
            addPieTooltips(statusPieChart);
        }));

        new Thread(task).start();
    }

    private void colorPieChart(PieChart chart, String[] colors) {
        int i = 0;
        for (PieChart.Data d : chart.getData()) {
            d.getNode().setStyle("-fx-pie-color: " + colors[i % colors.length] + ";");
            i++;
        }
    }

    private void addPieTooltips(PieChart chart) {
        double total = chart.getData().stream().mapToDouble(PieChart.Data::getPieValue).sum();
        for (PieChart.Data d : chart.getData()) {
            double pct = total > 0 ? (d.getPieValue() / total) * 100 : 0;
            Tooltip.install(d.getNode(), new Tooltip(
                    d.getName() + "\n" + String.format("%.1f%%", pct)));
            d.getNode().setCursor(Cursor.HAND);
        }
    }

    private void loadCryptoPrices() {
        javafx.concurrent.Task<Map<String, CoinGeckoService.CryptoPrice>> task = coinGeckoService.getTopCryptoPricesAsync();

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            cryptoContainer.getChildren().clear();
            for (var entry : task.getValue().entrySet()) {
                CoinGeckoService.CryptoPrice crypto = entry.getValue();
                HBox card = createCryptoCard(crypto);
                cryptoContainer.getChildren().add(card);

                FadeTransition fade = new FadeTransition(Duration.millis(400), card);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.play();
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            Label err = new Label("Could not load crypto prices");
            err.setStyle("-fx-text-fill: #E8A93A;");
            cryptoContainer.getChildren().add(err);
        }));

        new Thread(task).start();
    }

    private HBox createCryptoCard(CoinGeckoService.CryptoPrice crypto) {
        HBox card = new HBox(12);
        card.setStyle("-fx-background-color: rgba(23,42,69,0.94); -fx-background-radius: 12; " +
                "-fx-border-color: rgba(232,169,58,0.20); -fx-border-radius: 12; -fx-border-width: 1; " +
                "-fx-padding: 12;");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox info = new VBox(2);
        Label name = new Label(crypto.symbol);
        name.setStyle("-fx-text-fill: #E8A93A; -fx-font-weight: 900; -fx-font-size: 14px;");
        Label fullName = new Label(crypto.name);
        fullName.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 11px;");
        info.getChildren().addAll(name, fullName);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox priceInfo = new VBox(2);
        priceInfo.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label price = new Label(currencyFormat.format(crypto.price) + " EUR");
        price.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 800; -fx-font-size: 13px;");
        Label change = new Label(String.format("%+.2f%%", crypto.change24h));
        change.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " +
                (crypto.change24h >= 0 ? "#10B981" : "#EF4444") + ";");
        priceInfo.getChildren().addAll(price, change);

        card.getChildren().addAll(info, spacer, priceInfo);
        return card;
    }

    private void loadMarketNews() {
        javafx.concurrent.Task<List<GNewsService.Article>> task = gNewsService.fetchNewsAsync("startup investment funding", 8);

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            newsContainer.getChildren().clear();

            Label header = new Label("Market News");
            header.setStyle("-fx-text-fill: #E8A93A; -fx-font-size: 16px; -fx-font-weight: 900;");
            newsContainer.getChildren().add(header);

            List<GNewsService.Article> articles = task.getValue();
            if (articles == null || articles.isEmpty()) {
                Label empty = new Label("No news available at the moment.");
                empty.setStyle("-fx-text-fill: rgba(232,169,58,0.5); -fx-font-size: 13px;");
                newsContainer.getChildren().add(empty);
                return;
            }

            for (GNewsService.Article article : articles) {
                VBox card = createNewsCard(article);
                newsContainer.getChildren().add(card);

                FadeTransition fade = new FadeTransition(Duration.millis(400), card);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.play();
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            newsContainer.getChildren().clear();
            Label header = new Label("Market News");
            header.setStyle("-fx-text-fill: #E8A93A; -fx-font-size: 16px; -fx-font-weight: 900;");
            Label err = new Label("Could not load news: " + task.getException().getMessage());
            err.setWrapText(true);
            err.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
            newsContainer.getChildren().addAll(header, err);
        }));

        new Thread(task).start();
    }

    private VBox createNewsCard(GNewsService.Article article) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: rgba(23,42,69,0.94); -fx-background-radius: 12; " +
                "-fx-border-color: rgba(232,169,58,0.15); -fx-border-radius: 12; -fx-border-width: 1;");
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(ev -> {
            try {
                if (article.getUrl() != null && !article.getUrl().isBlank()
                        && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(article.getUrl()));
                }
            } catch (Exception ignored) {}
        });
        card.setOnMouseEntered(ev -> card.setStyle("-fx-background-color: rgba(30,55,90,0.96); -fx-background-radius: 12; " +
                "-fx-border-color: rgba(232,169,58,0.4); -fx-border-radius: 12; -fx-border-width: 1;"));
        card.setOnMouseExited(ev -> card.setStyle("-fx-background-color: rgba(23,42,69,0.94); -fx-background-radius: 12; " +
                "-fx-border-color: rgba(232,169,58,0.15); -fx-border-radius: 12; -fx-border-width: 1;"));

        Label title = new Label(article.getTitle());
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: 800;");

        String desc = article.getDescription();
        if (desc != null && !desc.isBlank()) {
            if (desc.length() > 120) desc = desc.substring(0, 117) + "...";
            Label descLabel = new Label(desc);
            descLabel.setWrapText(true);
            descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 11px;");
            card.getChildren().addAll(title, descLabel);
        } else {
            card.getChildren().add(title);
        }

        HBox meta = new HBox(10);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label source = new Label(article.getSourceName());
        source.setStyle("-fx-text-fill: #FFB84D; -fx-font-size: 10px; -fx-font-weight: 700;");

        String timeText = formatNewsDate(article.getPublishedAt());
        Label time = new Label(timeText);
        time.setStyle("-fx-text-fill: rgba(255,184,77,0.5); -fx-font-size: 10px;");

        meta.getChildren().addAll(source, time);
        card.getChildren().add(meta);

        Tooltip.install(card, new Tooltip("Click to read full article"));
        return card;
    }

    private String formatNewsDate(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return "";
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(isoDate);
            return zdt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
        } catch (Exception e) {
            return isoDate.length() > 16 ? isoDate.substring(0, 16) : isoDate;
        }
    }

    private void animateStatLabel(Label label, String targetText) {
        try {
            String digits = targetText.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                int target = Integer.parseInt(digits);
                if (target > 0 && target < 100_000) {
                    Timeline timeline = new Timeline();
                    int frames = 20;
                    for (int i = 0; i <= frames; i++) {
                        int val = (int) ((double) i / frames * target);
                        String text = targetText.replaceFirst(digits, String.valueOf(val));
                        timeline.getKeyFrames().add(new KeyFrame(
                                Duration.millis(i * 40), ev -> label.setText(text)));
                    }
                    timeline.getKeyFrames().add(new KeyFrame(
                            Duration.millis((frames + 1) * 40), ev -> label.setText(targetText)));
                    timeline.play();
                    return;
                }
            }
        } catch (NumberFormatException ignored) {}

        FadeTransition fade = new FadeTransition(Duration.millis(500), label);
        fade.setFromValue(0);
        fade.setToValue(1);
        label.setText(targetText);
        fade.play();
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/ProjectsListView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) totalProjectsLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleRefresh() {
        initialize();
    }
}
