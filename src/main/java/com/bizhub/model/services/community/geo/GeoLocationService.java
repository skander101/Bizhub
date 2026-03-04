package com.bizhub.model.services.community.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Geolocation service using OpenStreetMap Nominatim API.
 * 100% free, no API key required.
 */
public class GeoLocationService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private final OkHttpClient client = new OkHttpClient();

    /** A single location result with display name + coordinates */
    public static class LocationResult {
        public final String displayName;
        public final double lat;
        public final double lon;

        public LocationResult(String displayName, double lat, double lon) {
            this.displayName = displayName;
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public String toString() { return displayName; }
    }

    /**
     * Search locations matching query — returns up to 5 results with coordinates.
     */
    public List<LocationResult> searchLocations(String query) {
        List<LocationResult> results = new ArrayList<>();
        if (query == null || query.trim().length() < 2) return results;

        try {
            String url = NOMINATIM_URL + "?q=" +
                    java.net.URLEncoder.encode(query.trim(), "UTF-8") +
                    "&format=json&limit=5&addressdetails=0";

            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "BizHub-Community-App/1.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return results;
                String body = response.body().string();
                JsonArray array = JsonParser.parseString(body).getAsJsonArray();

                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.get(i).getAsJsonObject();
                    String fullName = obj.get("display_name").getAsString();
                    double lat = obj.get("lat").getAsDouble();
                    double lon = obj.get("lon").getAsDouble();

                    // Shorten: "Paris, Île-de-France, France" → "Paris, France"
                    String[] parts = fullName.split(", ");
                    String shortened = parts.length >= 2
                            ? parts[0] + ", " + parts[parts.length - 1]
                            : fullName;

                    boolean dup = results.stream().anyMatch(r -> r.displayName.equals(shortened));
                    if (!dup) results.add(new LocationResult(shortened, lat, lon));
                }
            }
        } catch (Exception e) {
            System.err.println("Nominatim error: " + e.getMessage());
        }
        return results;
    }
}

