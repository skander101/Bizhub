package com.bizhub.model.services.investissement;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

public class ExchangeRateService {

    public static class FxRates {
        private final BigDecimal eur;
        private final BigDecimal usd;
        private final Instant fetchedAt;

        public FxRates(BigDecimal eur, BigDecimal usd, Instant fetchedAt) {
            this.eur = eur;
            this.usd = usd;
            this.fetchedAt = fetchedAt;
        }

        public BigDecimal getEur() {
            return eur;
        }

        public BigDecimal getUsd() {
            return usd;
        }

        public Instant getFetchedAt() {
            return fetchedAt;
        }
    }

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final String API_URL = "https://open.er-api.com/v6/latest/TND";

    private final HttpClient httpClient;

    private FxRates cachedRates;

    public ExchangeRateService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public FxRates getTndRates() throws IOException, InterruptedException {
        if (cachedRates != null && cachedRates.getFetchedAt() != null) {
            Duration age = Duration.between(cachedRates.getFetchedAt(), Instant.now());
            if (age.compareTo(CACHE_TTL) < 0) {
                return cachedRates;
            }
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("FX API HTTP " + response.statusCode() + ": " + response.body());
        }

        BigDecimal eur = extractRateFromErApi(response.body(), "EUR");
        BigDecimal usd = extractRateFromErApi(response.body(), "USD");

        cachedRates = new FxRates(eur, usd, Instant.now());
        return cachedRates;
    }

    public BigDecimal convert(BigDecimal amountTnd, BigDecimal rate) {
        if (amountTnd == null || rate == null) {
            return null;
        }
        return amountTnd.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal extractRateFromErApi(String json, String symbol) throws IOException {
        int ratesIdx = json.indexOf("\"rates\"");
        if (ratesIdx < 0) {
            throw new IOException("FX API response missing rates");
        }

        String key = "\"" + symbol + "\":";
        int idx = json.indexOf(key, ratesIdx);
        if (idx < 0) {
            throw new IOException("FX API response missing rate for " + symbol);
        }

        int start = idx + key.length();
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if ((c >= '0' && c <= '9') || c == '.' || c == 'E' || c == 'e' || c == '+' || c == '-') {
                end++;
            } else {
                break;
            }
        }
        String num = json.substring(start, end).trim();
        if (num.isEmpty()) {
            throw new IOException("FX API response invalid number for " + symbol);
        }
        return new BigDecimal(num);
    }
}
