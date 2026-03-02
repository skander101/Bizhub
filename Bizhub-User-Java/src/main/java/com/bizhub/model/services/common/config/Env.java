package com.bizhub.model.services.common.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class Env {

    private static Map<String, String> DOTENV; // lazy

    private Env() {}

    public static String get(String key) {
        // 1) variables système
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) return v.trim();

        // 2) -Dkey=... (VM options)
        v = System.getProperty(key);
        if (v != null && !v.isBlank()) return v.trim();

        // 3) .env
        ensureDotEnvLoaded();
        v = DOTENV.get(key);
        return v == null ? null : v.trim();
    }

    public static String require(String key) {
        String v = get(key);
        if (v == null || v.isBlank())
            throw new IllegalStateException("Missing env var: " + key + " (set it in .env or system env)");
        return v.trim();
    }

    private static void ensureDotEnvLoaded() {
        if (DOTENV != null) return;

        DOTENV = new HashMap<>();
        File f = new File(".env");
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isBlank() || line.startsWith("#")) continue;
                int idx = line.indexOf('=');
                if (idx <= 0) continue;
                String k = line.substring(0, idx).trim();
                String val = line.substring(idx + 1).trim();
                // strip quotes
                if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                    val = val.substring(1, val.length() - 1);
                }
                DOTENV.put(k, val);
            }
        } catch (Exception ignore) {
            // silent; better than crashing for dotenv
        }
    }
}