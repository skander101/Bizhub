package com.bizhub.model.services.common.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * EnvConfig: Loads environment variables from a .env file in the project root.
 *
 * Format: simple KEY=VALUE pairs. Lines starting with '#' are comments.
 * Values may be optionally quoted with single or double quotes.
 *
 * No external library is used.
 */
public final class EnvConfig {

    private static volatile Map<String, String> env;

    private EnvConfig() {
    }

    private static Map<String, String> loadEnvFile() {
        Path envPath = findEnvFile();
        if (envPath == null || !Files.exists(envPath)) {
            System.err.println("[EnvConfig] ⚠ Fichier .env introuvable — " +
                    "variables d'environnement système utilisées uniquement.");
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(envPath, StandardCharsets.UTF_8)) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                int idx = trimmed.indexOf('=');
                if (idx <= 0) continue;

                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1).trim();

                // Support optional export keyword: export KEY=VALUE
                if (key.startsWith("export ")) {
                    key = key.substring("export ".length()).trim();
                }

                // Strip optional quotes
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    if (value.length() >= 2) value = value.substring(1, value.length() - 1);
                }

                map.put(key, value);
                count++;
            }
            System.err.println("[EnvConfig] ✅ .env chargé depuis " + envPath.toAbsolutePath()
                    + " (" + count + " variables)");
        } catch (IOException e) {
            System.err.println("[EnvConfig] ⚠ Erreur lecture .env : " + e.getMessage());
            return Collections.emptyMap();
        }

        return map;
    }

    /**
     * Find .env file in multiple locations:
     * 1. Current directory (project root)
     * 2. Parent directory
     * 3. User home directory
     */
    private static Path findEnvFile() {
        // 1) Current directory (IntelliJ project root)
        Path p = Paths.get(".env");
        if (Files.exists(p)) return p;

        // 2) Parent directory
        p = Paths.get("../.env");
        if (Files.exists(p)) return p;

        // 3) User home directory (fallback)
        p = Paths.get(System.getProperty("user.home")).resolve(".env");
        if (Files.exists(p)) return p;

        return null;
    }

    private static Map<String, String> getEnv() {
        if (env == null) {
            synchronized (EnvConfig.class) {
                if (env == null) {
                    env = loadEnvFile();
                }
            }
        }
        return env;
    }

    /**
     * Get an environment variable value.
     * Priority: System.getenv() > .env file
     * First checks system environment variables, then falls back to .env file.
     */
    public static String get(String key) {
        // 1) System environment variables (highest priority)
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value;
        }

        // 2) .env file
        value = getEnv().get(key);
        if (value == null || value.isBlank()) {
            System.err.println("[EnvConfig] ⚠ Variable manquante : " + key + " (non trouvée dans .env ni dans les variables système)");
        }
        return value;
    }

    /**
     * Get an environment variable value with a default fallback.
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /**
     * Alias for {@link #get(String, String)} — matches EnvLoader API.
     */
    public static String getOrDefault(String key, String defaultValue) {
        return get(key, defaultValue);
    }

    /**
     * Get a required environment variable. Throws if missing.
     */
    public static String getRequired(String key) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Variable '" + key + "' is missing.\n"
                            + "Add it to your .env file at the project root.");
        }
        return value;
    }

    // Auth0 Configuration
    public static String getAuth0Domain() {
        return get("AUTH0_DOMAIN");
    }

    public static String getAuth0ClientId() {
        return get("AUTH0_CLIENT_ID");
    }

    public static String getAuth0ClientSecret() {
        return get("AUTH0_CLIENT_SECRET");
    }

    // Infobip Configuration
    public static String getInfobipApiKey() {
        return get("INFOBIP_API_KEY");
    }

    public static String getInfobipBaseUrl() {
        return get("INFOBIP_BASE_URL");
    }

    // Cloudflare Workers AI
    public static String getCloudflareApiToken() {
        return get("CLOUDFLARE_API_TOKEN");
    }

    public static String getCloudflareAccountId() {
        return get("CLOUDFLARE_ACC_ID");
    }

    // Stripe Configuration
    public static String getStripeSecretKey() {
        return getRequired("STRIPE_SECRET_KEY");
    }

    public static String getStripeCurrency() {
        return getOrDefault("STRIPE_CURRENCY", "eur");
    }

    public static String getStripeSuccessUrl() {
        return getOrDefault("STRIPE_SUCCESS_URL", "http://localhost/success");
    }

    public static String getStripeCancelUrl() {
        return getOrDefault("STRIPE_CANCEL_URL", "http://localhost/cancel");
    }

    public static String getStripeWebhookSecret() {
        return getOrDefault("STRIPE_WEBHOOK_SECRET", "");
    }

    // Twilio Configuration
    public static String getTwilioAccountSid() {
        return getRequired("TWILIO_ACCOUNT_SID");
    }

    public static String getTwilioAuthToken() {
        return getRequired("TWILIO_AUTH_TOKEN");
    }

    public static String getTwilioFromNumber() {
        return getRequired("TWILIO_FROM_NUMBER");
    }

    // GROQ (AI — shared by AiChat + marketplace AI services)
    public static String getGroqApiKey() {
        return getOrDefault("GROQ_API_KEY", "");
    }

    public static String getGroqModel() {
        return getOrDefault("GROQ_MODEL", "llama-3.1-8b-instant");
    }

    public static int getGroqMaxTokens() {
        return Integer.parseInt(getOrDefault("GROQ_MAX_TOKENS", "512"));
    }

    /** GROQ API key for community posts AI features. */
    public static String getGroqApiKeyPost() {
        return getOrDefault("GROQ_API_KEY_POST", "");
    }

    // Face++ (facial recognition / verification)
    public static String getFacePlusPlusApiKey() {
        return getOrDefault("FACEPP_API_KEY", "");
    }

    public static String getFacePlusPlusApiSecret() {
        return getOrDefault("FACEPP_API_SECRET", "");
    }

    // ── Investment module keys ─────────────────────────────────────────────
    // These are read via ApiConfig (which uses EnvLoader), but typed getters
    // are provided here as a single reference for the full .env key map.

    /** Stripe secret key for the Investment module (separate test account). */
    public static String getStripeSecretKeyInvest() {
        return getOrDefault("STRIPE_SECRET_KEY_Invest", "");
    }

    /** OpenRouter AI key (negotiation copilot, pitch analysis, smart search). */
    public static String getOpenRouterApiKey() {
        return getOrDefault("OPENROUTER_API_KEY", "");
    }

    /** Yousign API key (digital contract signatures). */
    public static String getYousignApiKey() {
        return getOrDefault("YOUSIGN_API_KEY", "");
    }

    /**
     * Override email for Yousign signers and outgoing deal emails (testing only).
     * Returns null if not set.
     */
    public static String getEmailOverride() {
        String v = getOrDefault("EMAIL_OVERRIDE", "");
        return (v == null || v.isBlank()) ? null : v;
    }

    /** GNews API key (business news feed for investment analytics). */
    public static String getGnewsApiKey() {
        return getOrDefault("GNEWS_API_KEY", "");
    }

    /** NewsAPI key (optional secondary news source). */
    public static String getNewsApiKey() {
        return getOrDefault("NEWSAPI_API_KEY", "");
    }

    /** Alpha Vantage key (market/stock data). */
    public static String getAlphaVantageKey() {
        return getOrDefault("ALPHA_VANTAGE_KEY", "");
    }

    /** Gmail SMTP username (deal confirmation emails). */
    public static String getSmtpUsername() {
        return getOrDefault("SMTP_USERNAME", "");
    }

    /** Gmail SMTP app password. */
    public static String getSmtpAppPassword() {
        return getOrDefault("SMTP_APP_PASSWORD", "");
    }
}
