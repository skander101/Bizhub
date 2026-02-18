package com.bizhub.model.services.common.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Auth0Service: Handles email verification using Auth0 Management API.
 *
 * Flow:
 * 1. Get management API access token
 * 2. Create user in Auth0 (if not exists)
 * 3. Send verification email
 * 4. Check verification status
 *
 * IMPORTANT: For this to work, you must configure Auth0:
 * 1. Go to Auth0 Dashboard > Applications > APIs
 * 2. Click on "Auth0 Management API"
 * 3. Go to "Machine to Machine Applications" tab
 * 4. Authorize your application and grant these scopes:
 *    - read:users
 *    - create:users
 *    - update:users
 *    - delete:users
 *    - create:user_tickets
 */
public class Auth0Service {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Gson gson = new Gson();

    private final OkHttpClient client;
    private final String domain;
    private final String clientId;
    private final String clientSecret;
    private boolean configurationValid = true;
    private String configurationError = null;

    private String accessToken;
    private long tokenExpiry = 0;

    public Auth0Service() {
        this.client = new OkHttpClient();
        this.domain = EnvConfig.getAuth0Domain();
        this.clientId = EnvConfig.getAuth0ClientId();
        this.clientSecret = EnvConfig.getAuth0ClientSecret();

        // Check configuration
        if (domain == null || domain.isBlank()) {
            configurationValid = false;
            configurationError = "AUTH0_DOMAIN not configured in .env file";
        } else if (clientId == null || clientId.isBlank()) {
            configurationValid = false;
            configurationError = "AUTH0_CLIENT_ID not configured in .env file";
        } else if (clientSecret == null || clientSecret.isBlank()) {
            configurationValid = false;
            configurationError = "AUTH0_CLIENT_SECRET not configured in .env file";
        }
    }

    /**
     * Check if Auth0 is properly configured
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
     * Get Management API access token (cached until expiry).
     */
    private synchronized String getAccessToken() throws IOException {
        if (!configurationValid) {
            throw new IOException("Auth0 not configured: " + configurationError);
        }

        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return accessToken;
        }

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("audience", "https://" + domain + "/api/v2/");

        Request request = new Request.Builder()
                .url("https://" + domain + "/oauth/token")
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                System.err.println("Auth0 Token Request Failed:");
                System.err.println("  URL: https://" + domain + "/oauth/token");
                System.err.println("  Response Code: " + response.code());
                System.err.println("  Response Body: " + responseBody);
                System.err.println("  Client ID: " + clientId);
                System.err.println("  Audience: https://" + domain + "/api/v2/");

                if (response.code() == 403 || response.code() == 401) {
                    throw new IOException("Auth0 access denied (" + response.code() + "). " + responseBody + "\n\n" +
                            "To fix this:\n" +
                            "1. Go to Auth0 Dashboard > Applications > APIs > Auth0 Management API\n" +
                            "2. Click 'Machine to Machine Applications' tab\n" +
                            "3. Find your app (Client ID: " + clientId + ") and toggle it ON\n" +
                            "4. Click the dropdown arrow and select ALL permissions\n" +
                            "5. Click 'Update'\n\n" +
                            "NOTE: You must use a 'Machine to Machine' application type, NOT 'Regular Web Application'");
                }
                throw new IOException("Failed to get Auth0 access token: " + response.code() + " - " + responseBody);
            }

            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            accessToken = json.get("access_token").getAsString();
            int expiresIn = json.get("expires_in").getAsInt();
            tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000L) - 60000; // 1 min buffer
            return accessToken;
        }
    }

    /**
     * Create a user in Auth0 for email verification.
     * Returns the Auth0 user ID.
     */
    public String createAuth0User(String email, String password) throws IOException {
        String token = getAccessToken();

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("connection", "Username-Password-Authentication");
        body.put("email_verified", false);

        Request request = new Request.Builder()
                .url("https://" + domain + "/api/v2/users")
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            if (response.code() == 409) {
                // User already exists, try to get their ID
                return getAuth0UserId(email);
            }

            if (!response.isSuccessful()) {
                throw new IOException("Failed to create Auth0 user: " + response.code() + " - " + responseBody);
            }

            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            return json.get("user_id").getAsString();
        }
    }

    /**
     * Get Auth0 user ID by email.
     */
    public String getAuth0UserId(String email) throws IOException {
        String token = getAccessToken();

        String url = "https://" + domain + "/api/v2/users-by-email?email=" +
                java.net.URLEncoder.encode(email, "UTF-8");

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get Auth0 user: " + response.code());
            }

            String responseBody = response.body().string();
            com.google.gson.JsonArray users = gson.fromJson(responseBody, com.google.gson.JsonArray.class);

            if (users.size() == 0) {
                return null;
            }

            return users.get(0).getAsJsonObject().get("user_id").getAsString();
        }
    }

    /**
     * Send a verification email to the user.
     */
    public void sendVerificationEmail(String auth0UserId) throws IOException {
        String token = getAccessToken();

        Map<String, String> body = new HashMap<>();
        body.put("user_id", auth0UserId);
        body.put("client_id", clientId);

        Request request = new Request.Builder()
                .url("https://" + domain + "/api/v2/jobs/verification-email")
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body().string();
                throw new IOException("Failed to send verification email: " + response.code() + " - " + responseBody);
            }
        }
    }

    /**
     * Check if user's email is verified in Auth0.
     */
    public boolean isEmailVerified(String email) throws IOException {
        String token = getAccessToken();

        String url = "https://" + domain + "/api/v2/users-by-email?email=" +
                java.net.URLEncoder.encode(email, "UTF-8");

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to check email verification: " + response.code());
            }

            String responseBody = response.body().string();
            com.google.gson.JsonArray users = gson.fromJson(responseBody, com.google.gson.JsonArray.class);

            if (users.size() == 0) {
                return false;
            }

            return users.get(0).getAsJsonObject().get("email_verified").getAsBoolean();
        }
    }

    /**
     * Async version of isEmailVerified for polling.
     */
    public CompletableFuture<Boolean> isEmailVerifiedAsync(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return isEmailVerified(email);
            } catch (IOException e) {
                return false;
            }
        });
    }

    /**
     * Delete user from Auth0 (cleanup).
     */
    public void deleteAuth0User(String auth0UserId) throws IOException {
        String token = getAccessToken();

        Request request = new Request.Builder()
                .url("https://" + domain + "/api/v2/users/" + auth0UserId)
                .header("Authorization", "Bearer " + token)
                .delete()
                .build();

        try (Response response = client.newCall(request).execute()) {
            // Ignore errors on delete - best effort cleanup
        }
    }
}


