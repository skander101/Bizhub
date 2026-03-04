package com.bizhub.model.services.investissement;

import com.bizhub.model.services.common.config.ApiConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.concurrent.Task;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AlphaVantageService {

    private static final Logger logger = Logger.getLogger(AlphaVantageService.class.getName());
    private final HttpClient httpClient;

    public AlphaVantageService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public Map<String, Double> getSectorPerformance() throws Exception {
        String url = String.format("%s?function=SECTOR&apikey=%s",
                ApiConfig.ALPHA_VANTAGE_BASE_URL, ApiConfig.getAlphaVantageKey());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Double> sectors = new LinkedHashMap<>();

        if (response.statusCode() == 200) {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject realTime = json.has("Rank A: Real-Time Performance")
                    ? json.getAsJsonObject("Rank A: Real-Time Performance") : null;

            if (realTime != null) {
                for (String key : realTime.keySet()) {
                    String val = realTime.get(key).getAsString().replace("%", "");
                    try { sectors.put(key, Double.parseDouble(val)); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
        return sectors;
    }

    public Map<String, Double> getStockTimeSeries(String symbol) throws Exception {
        String url = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&outputsize=compact&apikey=%s",
                ApiConfig.ALPHA_VANTAGE_BASE_URL, symbol, ApiConfig.getAlphaVantageKey());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Double> prices = new LinkedHashMap<>();

        if (response.statusCode() == 200) {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject timeSeries = json.has("Time Series (Daily)")
                    ? json.getAsJsonObject("Time Series (Daily)") : null;

            if (timeSeries != null) {
                int count = 0;
                for (String date : timeSeries.keySet()) {
                    if (count++ >= 30) break;
                    JsonObject day = timeSeries.getAsJsonObject(date);
                    prices.put(date, day.get("4. close").getAsDouble());
                }
            }
        }
        return prices;
    }

    public Task<Map<String, Double>> getSectorPerformanceAsync() {
        return new Task<>() {
            @Override
            protected Map<String, Double> call() throws Exception {
                return getSectorPerformance();
            }
        };
    }

    public Task<Map<String, Double>> getStockTimeSeriesAsync(String symbol) {
        return new Task<>() {
            @Override
            protected Map<String, Double> call() throws Exception {
                return getStockTimeSeries(symbol);
            }
        };
    }
}
