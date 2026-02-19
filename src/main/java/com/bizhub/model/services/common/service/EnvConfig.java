package com.bizhub.model.services.common.service;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * EnvConfig: Loads environment variables from .env file.
 * Uses dotenv-java library to load variables from the project root.
 */
public final class EnvConfig {

    private static Dotenv dotenv;

    private EnvConfig() {
    }

    private static synchronized Dotenv getInstance() {
        if (dotenv == null) {
            dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();
        }
        return dotenv;
    }

    /**
     * Get an environment variable value.
     * First checks .env file, then falls back to system environment variables.
     */
    public static String get(String key) {
        String value = getInstance().get(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
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
}

