package com.bizhub.model.services.investissement;

import java.io.IOException;
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

public class NewsApiService {
    private static final String API_BASE = "https://newsapi.org/v2/everything";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final String DEFAULT_LANG = "fr";
    private static final String DEFAULT_SORT = "publishedAt";

    private final HttpClient httpClient;
    private String cachedQuery;
    private NewsSnapshot cachedSnapshot;

    public NewsApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static class Article {
        private final String title;
        private final String url;
        private final String publishedAt;
        private final String sourceName;

        public Article(String title, String url, String publishedAt, String sourceName) {
            this.title = title;
            this.url = url;
            this.publishedAt = publishedAt;
            this.sourceName = sourceName;
        }

        public String getTitle() { return title; }
        public String getUrl() { return url; }
        public String getPublishedAt() { return publishedAt; }
        public String getSourceName() { return sourceName; }
    }

    public enum Sentiment {
        POSITIVE, NEGATIVE, NEUTRAL
    }

    public static class NewsSnapshot {
        private final String query;
        private final List<Article> articles;
        private final Sentiment sentiment;
        private final Instant fetchedAt;

        public NewsSnapshot(String query, List<Article> articles, Sentiment sentiment, Instant fetchedAt) {
            this.query = query;
            this.articles = articles;
            this.sentiment = sentiment;
            this.fetchedAt = fetchedAt;
        }

        public String getQuery() { return query; }
        public List<Article> getArticles() { return articles; }
        public Sentiment getSentiment() { return sentiment; }
        public Instant getFetchedAt() { return fetchedAt; }
    }

    public NewsSnapshot searchProjectNews(String query, String lang, int max) throws IOException, InterruptedException {
        if (cachedSnapshot != null && cachedQuery != null && cachedQuery.equalsIgnoreCase(query)) {
            Duration age = Duration.between(cachedSnapshot.getFetchedAt(), Instant.now());
            if (age.compareTo(CACHE_TTL) < 0) {
                return cachedSnapshot;
            }
        }

        String apiKey = System.getenv("NEWSAPI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("NEWSAPI_API_KEY is not set");
        }

        apiKey = apiKey.trim();
        if (!apiKey.matches("[a-f0-9]{32}")) {
            throw new IOException("NEWSAPI_API_KEY appears invalid (expected 32 hex chars)");
        }

        String q = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String l = (lang == null || lang.isBlank()) ? DEFAULT_LANG : lang;
        int m = Math.max(1, Math.min(max, 10));

        String url = API_BASE
                + "?q=" + q
                + "&language=" + URLEncoder.encode(l, StandardCharsets.UTF_8)
                + "&sortBy=" + DEFAULT_SORT
                + "&pageSize=" + m
                + "&apiKey=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "BizHub/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("NewsAPI HTTP " + response.statusCode() + ": " + safeErrorMessage(response.body()));
        }

        String body = response.body();
        if (body.contains("\"status\":\"error\"") || body.contains("\"code\":")) {
            throw new IOException("NewsAPI error: " + safeErrorMessage(body));
        }

        List<Article> articles = parseArticles(body, m);
        Sentiment sentiment = computeSentiment(articles);

        cachedQuery = query;
        cachedSnapshot = new NewsSnapshot(query, articles, sentiment, Instant.now());
        return cachedSnapshot;
    }

    private List<Article> parseArticles(String json, int max) {
        List<Article> articles = new ArrayList<>();
        String articlesKey = "\"articles\":[";
        int start = json.indexOf(articlesKey);
        if (start == -1) return articles;
        start += articlesKey.length() - 1;
        int end = findMatchingBrace(json, start, '[', ']');
        if (end == -1) return articles;
        String arr = json.substring(start, end + 1);
        String[] parts = splitObjects(arr);
        int count = 0;
        for (String part : parts) {
            if (count >= max) break;
            String title = extractJsonString(part, "title", 0);
            String url = extractJsonString(part, "url", 0);
            String publishedAt = extractJsonString(part, "publishedAt", 0);
            String sourceName = extractJsonString(part, "name", 0);
            if (!title.isBlank() && !url.isBlank()) {
                articles.add(new Article(title, url, publishedAt, sourceName));
                count++;
            }
        }
        return Collections.unmodifiableList(articles);
    }

    private int findMatchingBrace(String s, int start, char open, char close) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private String[] splitObjects(String array) {
        List<String> objs = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < array.length(); i++) {
            char c = array.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    objs.add(array.substring(start, i + 1));
                }
            }
        }
        return objs.toArray(new String[0]);
    }

    private String extractJsonString(String json, String key, int start) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search, start);
        if (idx == -1) return "";
        int begin = idx + search.length();
        int end = json.indexOf("\"", begin);
        if (end == -1) return "";
        return json.substring(begin, end);
    }

    private Sentiment computeSentiment(List<Article> articles) {
        if (articles.isEmpty()) return Sentiment.NEUTRAL;
        int pos = 0, neg = 0;
        List<String> posWords = List.of("bon", "hausse", "gain", "profit", "succès", "croissance", "positif", "opportunité", "forte", "record");
        List<String> negWords = List.of("mauvais", "baisse", "perte", "échec", "danger", "négatif", "risque", "crise", "chute", "faible");
        for (Article a : articles) {
            String lower = a.getTitle().toLowerCase();
            for (String w : posWords) if (lower.contains(w)) { pos++; break; }
            for (String w : negWords) if (lower.contains(w)) { neg++; break; }
        }
        if (pos > neg) return Sentiment.POSITIVE;
        if (neg > pos) return Sentiment.NEGATIVE;
        return Sentiment.NEUTRAL;
    }

    private String safeErrorMessage(String body) {
        if (body == null || body.isBlank()) return "empty response";
        String msg = extractJsonString(body, "message", 0);
        if (!msg.isBlank()) return msg;
        String code = extractJsonString(body, "code", 0);
        if (!code.isBlank()) return "code " + code;
        return body.length() > 120 ? body.substring(0, 120) + "..." : body;
    }
}
