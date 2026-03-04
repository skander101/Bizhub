package com.bizhub.model.services.common.config;

/**
 * API and service configuration for the Investment module.
 * All secrets are read from the shared .env file at the project root.
 *
 * Key naming convention in .env:
 *   - Investment-specific keys use the suffix  _Invest  where they differ from
 *     the marketplace module (e.g. STRIPE_SECRET_KEY_Invest).
 *   - Shared keys (DB, SMTP, GNews…) are used directly.
 *
 * Do NOT commit real keys. Add them to .env (git-ignored).
 */
public final class ApiConfig {
    private ApiConfig() {}

    // ── OpenRouter AI (negotiation copilot, pitch analysis, smart search) ──
    public static String getOpenRouterApiKey() {
        return EnvLoader.getOrDefault("OPENROUTER_API_KEY", "");
    }
    public static final String OPENROUTER_MODEL = "nvidia/nemotron-3-nano-30b-a3b:free";
    public static final String OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1/chat/completions";

    // ── Stripe — Investment module (separate test account from marketplace) ──
    /** Returns the investment-module Stripe secret key (STRIPE_SECRET_KEY_Invest). */
    public static String getStripeSecretKey() {
        return EnvLoader.getOrDefault("STRIPE_SECRET_KEY_Invest", "");
    }
    /** Currency used for investment deals. */
    public static final String STRIPE_CURRENCY = "eur";

    // ── Yousign (digital contract signatures) ──
    public static String getYousignApiKey() {
        return EnvLoader.getOrDefault("YOUSIGN_API_KEY", "");
    }
    public static final String YOUSIGN_BASE_URL = "https://api-sandbox.yousign.app/v3";

    /**
     * Override email for ALL Yousign signers (sandbox testing).
     * Empty/null → use the real buyer/seller emails from the Deal.
     */
    public static String getYousignSignerEmailOverride() {
        String v = EnvLoader.getOrDefault("YOUSIGN_SIGNER_EMAIL_OVERRIDE", "");
        return (v != null && !v.isBlank()) ? v : null;
    }

    // ── Alpha Vantage (market data) ──
    public static String getAlphaVantageKey() {
        return EnvLoader.getOrDefault("ALPHA_VANTAGE_KEY", "");
    }
    public static final String ALPHA_VANTAGE_BASE_URL = "https://www.alphavantage.co/query";

    // ── GNews (business news feed) ──
    public static String getGnewsApiKey() {
        return EnvLoader.getOrDefault("GNEWS_API_KEY", "");
    }
    public static final String GNEWS_BASE_URL = "https://gnews.io/api/v4";

    // ── NewsAPI (optional alternative news source) ──
    public static String getNewsApiKey() {
        // .env uses NEWSAPI_API_KEY
        return EnvLoader.getOrDefault("NEWSAPI_API_KEY", "");
    }
    public static final String NEWS_API_BASE_URL = "https://newsapi.org/v2";

    // ── Gmail SMTP (deal confirmation emails) ──
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final int    SMTP_PORT = 587;

    public static String getSmtpUsername() {
        return EnvLoader.getOrDefault("SMTP_USERNAME", "");
    }
    public static String getSmtpAppPassword() {
        return EnvLoader.getOrDefault("SMTP_APP_PASSWORD", "");
    }

    /**
     * When set, ALL outgoing deal emails are redirected to this address (testing).
     * Matches EMAIL_OVERRIDE in .env.
     */
    public static String getEmailOverride() {
        String v = EnvLoader.getOrDefault("EMAIL_OVERRIDE", "");
        return (v != null && !v.isBlank()) ? v : null;
    }

    // ── CoinGecko (crypto prices — free, no key required) ──
    public static final String COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3";

    // ── Clearbit Logo CDN (free, no key required) ──
    public static final String CLEARBIT_LOGO_URL = "https://logo.clearbit.com/";

    // ── Database (shared with the rest of the app) ──
    public static String getDbUrl() {
        return EnvLoader.getOrDefault("DB_URL",
                "jdbc:mysql://127.0.0.1:3306/BizHub?useSSL=false&allowPublicKeyRetrieval=true");
    }
    public static String getDbUser() {
        return EnvLoader.getOrDefault("DB_USER", "root");
    }
    public static String getDbPassword() {
        return EnvLoader.getOrDefault("DB_PASSWORD", "");
    }
}
