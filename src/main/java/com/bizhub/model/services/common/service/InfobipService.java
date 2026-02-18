package com.bizhub.model.services.common.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * InfobipService: Handles TOTP (Time-based One-Time Password) via Infobip 2FA API.
 *
 * Flow for Authenticator App TOTP:
 * 1. Create 2FA application (one-time setup)
 * 2. Create TOTP application for the user
 * 3. Get QR code URL for user to scan with authenticator app
 * 4. Verify TOTP code entered by user
 */
public class InfobipService {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Gson gson = new Gson();

    private final OkHttpClient client;
    private final String apiKey;
    private final String baseUrl;

    private boolean configurationValid = true;
    private String configurationError = null;

    // Cached application ID
    private String applicationId;

    public InfobipService() {
        this.client = new OkHttpClient();
        this.apiKey = EnvConfig.getInfobipApiKey();
        this.baseUrl = "https://" + EnvConfig.getInfobipBaseUrl();

        // Validate configuration
        if (apiKey == null || apiKey.isBlank()) {
            configurationValid = false;
            configurationError = "INFOBIP_API_KEY not configured in .env file";
        } else if (baseUrl == null || baseUrl.equals("https://null") || baseUrl.equals("https://")) {
            configurationValid = false;
            configurationError = "INFOBIP_BASE_URL not configured in .env file";
        }
    }

    /**
     * Check if Infobip is properly configured
     */
    public boolean isConfigured() {
        return configurationValid;
    }

    /**
     * Get configuration error message
     */
    public String getConfigurationError() {
        return configurationError;
    }

    /**
     * Initialize the TOTP application.
     * Should be called once on application startup.
     */
    public synchronized void initialize() throws IOException {
        if (!configurationValid) {
            throw new IOException("Infobip not configured: " + configurationError);
        }

        if (applicationId != null) {
            return;
        }

        applicationId = getOrCreateTotpApplication();
    }

    /**
     * Get or create the TOTP application.
     */
    private String getOrCreateTotpApplication() throws IOException {
        // First try to list existing applications
        Request listRequest = new Request.Builder()
                .url(baseUrl + "/2fa/2/applications")
                .header("Authorization", "App " + apiKey)
                .get()
                .build();

        try (Response response = client.newCall(listRequest).execute()) {
            if (response.isSuccessful()) {
                String body = response.body().string();
                com.google.gson.JsonArray apps = gson.fromJson(body, com.google.gson.JsonArray.class);

                for (int i = 0; i < apps.size(); i++) {
                    JsonObject app = apps.get(i).getAsJsonObject();
                    if ("BizHub-TOTP".equals(app.get("name").getAsString())) {
                        return app.get("applicationId").getAsString();
                    }
                }
            }
        }

        // Create new TOTP application
        Map<String, Object> appConfig = new HashMap<>();
        appConfig.put("name", "BizHub-TOTP");
        appConfig.put("enabled", true);

        Map<String, Object> configuration = new HashMap<>();
        configuration.put("pinAttempts", 5);
        configuration.put("allowMultiplePinVerifications", true);
        configuration.put("pinTimeToLive", "5m");
        configuration.put("verifyPinLimit", "1/3s");
        configuration.put("sendPinPerApplicationLimit", "10000/1d");
        configuration.put("sendPinPerPhoneNumberLimit", "10/1h");
        appConfig.put("configuration", configuration);

        Request createRequest = new Request.Builder()
                .url(baseUrl + "/2fa/2/applications")
                .header("Authorization", "App " + apiKey)
                .post(RequestBody.create(gson.toJson(appConfig), JSON))
                .build();

        try (Response response = client.newCall(createRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("Failed to create TOTP application: " + response.code() + " - " + errorBody);
            }

            JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
            return json.get("applicationId").getAsString();
        }
    }

    /**
     * Create a TOTP secret for a user and get the QR code URL.
     * The user will scan this QR code with their authenticator app.
     *
     * @param userId Unique identifier for the user (e.g., email or user ID)
     * @return TotpSetupResult containing the secret and QR code URL
     */
    public TotpSetupResult createTotpForUser(String userId) throws IOException {
        initialize();

        // Generate a TOTP secret using Infobip's API
        Map<String, Object> body = new HashMap<>();
        body.put("applicationId", applicationId);
        body.put("identity", userId);

        Request request = new Request.Builder()
                .url(baseUrl + "/2fa/2/applications/" + applicationId + "/secrets")
                .header("Authorization", "App " + apiKey)
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                // If secret already exists, try to get it
                if (response.code() == 409) {
                    return getTotpForUser(userId);
                }
                throw new IOException("Failed to create TOTP secret: " + response.code() + " - " + responseBody);
            }

            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            String secret = json.get("secret").getAsString();
            String secretId = json.get("secretId").getAsString();

            // Generate QR code URL for authenticator apps
            String qrCodeUrl = generateTotpQrCodeUrl(userId, secret);

            return new TotpSetupResult(secretId, secret, qrCodeUrl);
        }
    }

    /**
     * Get existing TOTP setup for a user.
     */
    public TotpSetupResult getTotpForUser(String userId) throws IOException {
        initialize();

        Request request = new Request.Builder()
                .url(baseUrl + "/2fa/2/applications/" + applicationId + "/secrets?identity=" +
                     java.net.URLEncoder.encode(userId, "UTF-8"))
                .header("Authorization", "App " + apiKey)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new IOException("Failed to get TOTP secret: " + response.code() + " - " + responseBody);
            }

            com.google.gson.JsonArray secrets = gson.fromJson(responseBody, com.google.gson.JsonArray.class);
            if (secrets.size() == 0) {
                // No secret found, create a new one
                return createTotpForUser(userId);
            }

            JsonObject secretObj = secrets.get(0).getAsJsonObject();
            String secret = secretObj.get("secret").getAsString();
            String secretId = secretObj.get("secretId").getAsString();
            String qrCodeUrl = generateTotpQrCodeUrl(userId, secret);

            return new TotpSetupResult(secretId, secret, qrCodeUrl);
        }
    }

    /**
     * Generate a standard TOTP QR code URL that works with any authenticator app.
     * Uses the otpauth:// URI format.
     */
    private String generateTotpQrCodeUrl(String userId, String secret) {
        // Standard TOTP URI format: otpauth://totp/ISSUER:ACCOUNT?secret=SECRET&issuer=ISSUER
        String issuer = "BizHub";
        String account = userId;

        try {
            String otpauthUri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                    java.net.URLEncoder.encode(issuer, "UTF-8"),
                    java.net.URLEncoder.encode(account, "UTF-8"),
                    secret,
                    java.net.URLEncoder.encode(issuer, "UTF-8"));

            // Return a Google Charts QR code URL (or you can use any QR code generator)
            return "https://chart.googleapis.com/chart?chs=200x200&cht=qr&chl=" +
                   java.net.URLEncoder.encode(otpauthUri, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verify a TOTP code entered by the user.
     *
     * @param secretId The secret ID from TotpSetupResult
     * @param code The 6-digit code from the authenticator app
     * @return true if the code is valid
     */
    public boolean verifyTotp(String secretId, String code) throws IOException {
        Map<String, String> body = new HashMap<>();
        body.put("code", code);

        Request request = new Request.Builder()
                .url(baseUrl + "/2fa/2/applications/" + applicationId + "/secrets/" + secretId + "/verify")
                .header("Authorization", "App " + apiKey)
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return false;
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            return json.has("verified") && json.get("verified").getAsBoolean();
        }
    }

    /**
     * Async version of verifyTotp.
     */
    public CompletableFuture<Boolean> verifyTotpAsync(String secretId, String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return verifyTotp(secretId, code);
            } catch (IOException e) {
                return false;
            }
        });
    }

    /**
     * Delete TOTP secret for a user (e.g., when disabling 2FA).
     */
    public void deleteTotpForUser(String secretId) throws IOException {
        initialize();

        Request request = new Request.Builder()
                .url(baseUrl + "/2fa/2/applications/" + applicationId + "/secrets/" + secretId)
                .header("Authorization", "App " + apiKey)
                .delete()
                .build();

        try (Response response = client.newCall(request).execute()) {
            // Ignore errors - best effort cleanup
        }
    }

    /**
     * Result class for TOTP setup containing all necessary info.
     */
    public static class TotpSetupResult {
        private final String secretId;
        private final String secret;
        private final String qrCodeUrl;

        public TotpSetupResult(String secretId, String secret, String qrCodeUrl) {
            this.secretId = secretId;
            this.secret = secret;
            this.qrCodeUrl = qrCodeUrl;
        }

        public String getSecretId() {
            return secretId;
        }

        public String getSecret() {
            return secret;
        }

        public String getQrCodeUrl() {
            return qrCodeUrl;
        }
    }
}

