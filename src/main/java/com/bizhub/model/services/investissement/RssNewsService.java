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

public class RssNewsService {

    public enum Sentiment { POSITIVE, NEUTRAL, NEGATIVE, UNKNOWN }

    public static final class Article {
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

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public String getPublishedAt() {
            return publishedAt;
        }

        public String getSourceName() {
            return sourceName;
        }
    }

    public static final class NewsSnapshot {
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

        public String getQuery() {
            return query;
        }

        public List<Article> getArticles() {
            return articles;
        }

        public Sentiment getSentiment() {
            return sentiment;
        }

        public Instant getFetchedAt() {
            return fetchedAt;
        }
    }

    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final String RSS_BASE = "https://news.google.com/rss/search?q=";

    private final HttpClient httpClient;

    private String cachedQuery;
    private NewsSnapshot cachedSnapshot;

    public RssNewsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public NewsSnapshot searchProjectNews(String query, String lang, int max) throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            return new NewsSnapshot("", Collections.emptyList(), Sentiment.UNKNOWN, Instant.now());
        }

        if (cachedSnapshot != null && cachedQuery != null && cachedQuery.equalsIgnoreCase(query)) {
            Duration age = Duration.between(cachedSnapshot.getFetchedAt(), Instant.now());
            if (age.compareTo(CACHE_TTL) < 0) {
                return cachedSnapshot;
            }
        }

        String q = URLEncoder.encode(query, StandardCharsets.UTF_8);
        int m = Math.max(1, Math.min(max, 10));

        String url = RSS_BASE + q + "&hl=" + (lang == null || lang.isBlank() ? "fr" : lang);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "BizHub/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("RSS feed HTTP " + response.statusCode() + ": " + response.body());
        }

        List<Article> articles = parseRss(response.body(), m);
        Sentiment sentiment = computeSentiment(articles);

        cachedQuery = query;
        cachedSnapshot = new NewsSnapshot(query, articles, sentiment, Instant.now());
        return cachedSnapshot;
    }

    private List<Article> parseRss(String xml, int limit) {
        if (xml == null || xml.isBlank()) return Collections.emptyList();

        List<Article> list = new ArrayList<>();
        int idx = xml.indexOf("<item>");
        int cursor = idx;

        for (int i = 0; i < limit && cursor >= 0; i++) {
            String title = extractTag(xml, "title", cursor);
            String link = extractTag(xml, "link", cursor);
            String pubDate = extractTag(xml, "pubDate", cursor);
            String source = extractTag(xml, "source", cursor);

            if (title == null || title.isBlank()) break;

            list.add(new Article(
                    unescapeXml(title),
                    link == null ? "" : unescapeXml(link),
                    pubDate == null ? "" : unescapeXml(pubDate),
                    source == null ? "" : unescapeXml(source)
            ));

            int next = xml.indexOf("<item>", cursor + 1);
            if (next < 0) break;
            cursor = next;
        }

        return list;
    }

    private String extractTag(String xml, String tag, int fromIndex) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = xml.indexOf(open, fromIndex);
        if (start < 0) return null;
        int contentStart = start + open.length();
        int end = xml.indexOf(close, contentStart);
        if (end < 0) return null;
        return xml.substring(contentStart, end);
    }

    private String unescapeXml(String s) {
        if (s == null) return null;
        return s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&apos;", "'")
                .replace("&quot;", "\"");
    }

    private Sentiment computeSentiment(List<Article> articles) {
        if (articles == null || articles.isEmpty()) return Sentiment.UNKNOWN;

        int score = 0;
        for (Article a : articles) {
            String t = a.getTitle() == null ? "" : a.getTitle().toLowerCase();
            score += sentimentScore(t);
        }

        if (score >= 2) return Sentiment.POSITIVE;
        if (score <= -2) return Sentiment.NEGATIVE;
        return Sentiment.NEUTRAL;
    }

    private int sentimentScore(String text) {
        int s = 0;

        String[] positive = {"growth", "record", "profit", "success", "wins", "award", "funding", "raises", "partnership", "expands", "launch"};
        String[] negative = {"loss", "lawsuit", "fraud", "scandal", "decline", "crisis", "bankruptcy", "down", "fired", "shutdown", "hack"};

        for (String p : positive) if (text.contains(p)) s++;
        for (String n : negative) if (text.contains(n)) s--;

        return s;
    }
}
