package com.bizhub.model.services.investissement;

import com.bizhub.model.services.common.config.ApiConfig;
import com.google.gson.JsonArray;
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

public class CoinGeckoService {

    private static final Logger logger = Logger.getLogger(CoinGeckoService.class.getName());
    private final HttpClient httpClient;

    public CoinGeckoService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public static class CryptoPrice {
        public final String name;
        public final String symbol;
        public final double price;
        public final double change24h;
        public final double marketCap;
        public final String imageUrl;

        public CryptoPrice(String name, String symbol, double price, double change24h, double marketCap, String imageUrl) {
            this.name = name;
            this.symbol = symbol;
            this.price = price;
            this.change24h = change24h;
            this.marketCap = marketCap;
            this.imageUrl = imageUrl;
        }
    }

    public Map<String, CryptoPrice> getTopCryptoPrices() throws Exception {
        String url = ApiConfig.COINGECKO_BASE_URL +
                "/coins/markets?vs_currency=eur&ids=bitcoin,ethereum,solana,cardano,polkadot&order=market_cap_desc";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, CryptoPrice> prices = new LinkedHashMap<>();

        if (response.statusCode() == 200) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (var elem : array) {
                JsonObject coin = elem.getAsJsonObject();
                String id = coin.get("id").getAsString();
                prices.put(id, new CryptoPrice(
                        coin.get("name").getAsString(),
                        coin.get("symbol").getAsString().toUpperCase(),
                        coin.get("current_price").getAsDouble(),
                        coin.has("price_change_percentage_24h") && !coin.get("price_change_percentage_24h").isJsonNull()
                                ? coin.get("price_change_percentage_24h").getAsDouble() : 0,
                        coin.has("market_cap") && !coin.get("market_cap").isJsonNull()
                                ? coin.get("market_cap").getAsDouble() : 0,
                        coin.has("image") && !coin.get("image").isJsonNull()
                                ? coin.get("image").getAsString() : ""
                ));
            }
        }
        return prices;
    }

    public Map<String, Double> getCryptoPriceHistory(String coinId, int days) throws Exception {
        String url = String.format("%s/coins/%s/market_chart?vs_currency=eur&days=%d",
                ApiConfig.COINGECKO_BASE_URL, coinId, days);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Double> history = new LinkedHashMap<>();

        if (response.statusCode() == 200) {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray prices = json.getAsJsonArray("prices");
            if (prices != null) {
                for (var point : prices) {
                    JsonArray pair = point.getAsJsonArray();
                    long timestamp = pair.get(0).getAsLong();
                    double price = pair.get(1).getAsDouble();
                    java.time.LocalDate date = java.time.Instant.ofEpochMilli(timestamp)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    history.put(date.toString(), price);
                }
            }
        }
        return history;
    }

    public Task<Map<String, CryptoPrice>> getTopCryptoPricesAsync() {
        return new Task<>() {
            @Override
            protected Map<String, CryptoPrice> call() throws Exception {
                return getTopCryptoPrices();
            }
        };
    }
}
