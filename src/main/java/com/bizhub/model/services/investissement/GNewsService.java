package com.bizhub.model.services.investissement;

import com.bizhub.model.services.common.config.ApiConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.concurrent.Task;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class GNewsService {

    private static final Logger logger = Logger.getLogger(GNewsService.class.getName());
    private final HttpClient httpClient;

    private List<Article> cachedArticles;
    private Instant cacheTime;
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    public GNewsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public static final class Article {
        private final String title;
        private final String description;
        private final String url;
        private final String imageUrl;
        private final String publishedAt;
        private final String sourceName;
        private final String sourceUrl;

        public Article(String title, String description, String url, String imageUrl,
                       String publishedAt, String sourceName, String sourceUrl) {
            this.title = title;
            this.description = description;
            this.url = url;
            this.imageUrl = imageUrl;
            this.publishedAt = publishedAt;
            this.sourceName = sourceName;
            this.sourceUrl = sourceUrl;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getUrl() { return url; }
        public String getImageUrl() { return imageUrl; }
        public String getPublishedAt() { return publishedAt; }
        public String getSourceName() { return sourceName; }
        public String getSourceUrl() { return sourceUrl; }
    }

    public List<Article> fetchNews(String query, int max) throws Exception {
        if (cachedArticles != null && cacheTime != null
                && Duration.between(cacheTime, Instant.now()).compareTo(CACHE_TTL) < 0) {
            return cachedArticles;
        }

        String apiKey = ApiConfig.getGnewsApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            logger.warning("GNews API key is missing");
            return Collections.emptyList();
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format("%s/search?q=%s&lang=en&max=%d&apikey=%s",
                ApiConfig.GNEWS_BASE_URL, encodedQuery, max, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.warning("GNews API error: HTTP " + response.statusCode() + " " + response.body());
            throw new RuntimeException("GNews API error: HTTP " + response.statusCode());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray articles = json.has("articles") ? json.getAsJsonArray("articles") : new JsonArray();

        List<Article> result = new ArrayList<>();
        for (JsonElement el : articles) {
            JsonObject a = el.getAsJsonObject();
            String title = a.has("title") ? a.get("title").getAsString() : "";
            String desc = a.has("description") && !a.get("description").isJsonNull()
                    ? a.get("description").getAsString() : "";
            String articleUrl = a.has("url") ? a.get("url").getAsString() : "";
            String imageUrl = a.has("image") && !a.get("image").isJsonNull()
                    ? a.get("image").getAsString() : null;
            String publishedAt = a.has("publishedAt") ? a.get("publishedAt").getAsString() : "";
            String sourceName = "";
            String sourceUrl = "";
            if (a.has("source") && a.get("source").isJsonObject()) {
                JsonObject src = a.getAsJsonObject("source");
                sourceName = src.has("name") ? src.get("name").getAsString() : "";
                sourceUrl = src.has("url") ? src.get("url").getAsString() : "";
            }
            result.add(new Article(title, desc, articleUrl, imageUrl, publishedAt, sourceName, sourceUrl));
        }

        cachedArticles = result;
        cacheTime = Instant.now();
        return result;
    }

    public Task<List<Article>> fetchNewsAsync(String query, int max) {
        return new Task<>() {
            @Override
            protected List<Article> call() throws Exception {
                return fetchNews(query, max);
            }
        };
    }
}
