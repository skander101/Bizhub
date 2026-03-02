package com.bizhub.controller.marketplace;

import com.bizhub.model.marketplace.StatsPoint;
import com.bizhub.model.services.marketplace.InvestorStatsService;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class InvestorStatsApiServer {

    private static HttpServer server;
    private static int activePort = -1;

    private static final Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(LocalDate.class,
                    (com.google.gson.JsonSerializer<LocalDate>)
                            (src, type, ctx) -> new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.math.BigDecimal.class,
                    (com.google.gson.JsonSerializer<java.math.BigDecimal>)
                            (src, type, ctx) -> new com.google.gson.JsonPrimitive(src.doubleValue()))
            .create();
    private static final InvestorStatsService service = new InvestorStatsService();

    private static final int PORT_START = 8090;
    private static final int PORT_END   = 8095;

    public static int start() throws Exception {
        if (server != null) return activePort;

        for (int port = PORT_START; port <= PORT_END; port++) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                activePort = port;
                break;
            } catch (Exception ignored) {}
        }

        if (server == null) {
            throw new IllegalStateException("Aucun port libre entre 8090 et 8095");
        }

        server.createContext("/api/investor/stats", (HttpExchange ex) -> {
            try {
                if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                    ex.sendResponseHeaders(405, -1);
                    return;
                }

                Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
                int investorId = Integer.parseInt(q.getOrDefault("investorId", "0"));

                LocalDate from = LocalDate.parse(q.getOrDefault("from", LocalDate.now().minusDays(60).toString()));
                LocalDate to   = LocalDate.parse(q.getOrDefault("to", LocalDate.now().toString()));

                List<StatsPoint> data = service.daily(investorId, from, to);

                byte[] json = gson.toJson(data).getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                ex.sendResponseHeaders(200, json.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(json);
                }

            } catch (Exception e) {
                byte[] msg = ("{\"error\":\"" + safe(e.getMessage()) + "\"}")
                        .getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                ex.sendResponseHeaders(500, msg.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(msg);
                }
            }
        });

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        return activePort;
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            activePort = -1;
        }
    }

    public static int getActivePort() { return activePort; }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("\"", "'").replace("\n", " ").replace("\r", " ");
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isBlank()) return map;

        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(k, v);
        }
        return map;
    }
}