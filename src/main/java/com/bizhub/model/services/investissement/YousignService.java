package com.bizhub.model.services.investissement;

import com.bizhub.model.services.common.config.ApiConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.concurrent.Task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;

public class YousignService {

    private static final Logger logger = Logger.getLogger(YousignService.class.getName());
    private final HttpClient httpClient;

    public YousignService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public String createSignatureRequest(String title) throws Exception {
        System.out.println("[Yousign] Step 1/4: Creating signature request (title=" + title + ", delivery_mode=email)...");
        JsonObject body = new JsonObject();
        body.addProperty("name", title);
        body.addProperty("delivery_mode", "email");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.YOUSIGN_BASE_URL + "/signature_requests"))
                .header("Authorization", "Bearer " + ApiConfig.getYousignApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[Yousign] Step 1/4 response: HTTP " + response.statusCode() + " body=" + response.body());
        logger.info("Yousign create request: " + response.statusCode() + " " + response.body());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
            String requestId = result.get("id").getAsString();
            System.out.println("[Yousign] Step 1/4 OK: signature_request_id=" + requestId);
            return requestId;
        }

        System.err.println("[Yousign] Step 1/4 FAILED: " + response.body());
        logger.warning("Yousign create failed: " + response.body());
        throw new RuntimeException("Failed to create signature request: " + response.statusCode());
    }

    /** Yousign v3 expects multipart/form-data with binary file, not JSON. */
    public String uploadDocument(String signatureRequestId, String pdfPath) throws Exception {
        System.out.println("[Yousign] Step 2/4: Uploading document " + pdfPath + " to request " + signatureRequestId + " (multipart/form-data)...");
        byte[] fileBytes = Files.readAllBytes(Path.of(pdfPath));
        String fileName = new File(pdfPath).getName();

        String boundary = "----BizHubYousignBoundary" + System.currentTimeMillis();
        byte[] boundaryBytes = boundary.getBytes(StandardCharsets.UTF_8);
        byte[] crlf = "\r\n".getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary).getBytes(StandardCharsets.UTF_8));
        body.write(crlf);
        body.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"").getBytes(StandardCharsets.UTF_8));
        body.write(crlf);
        body.write("Content-Type: application/pdf".getBytes(StandardCharsets.UTF_8));
        body.write(crlf);
        body.write(crlf);
        body.write(fileBytes);
        body.write(crlf);
        body.write(("--" + boundary).getBytes(StandardCharsets.UTF_8));
        body.write(crlf);
        body.write("Content-Disposition: form-data; name=\"nature\"".getBytes(StandardCharsets.UTF_8));
        body.write(crlf);
        body.write(crlf);
        body.write("signable_document".getBytes(StandardCharsets.UTF_8));
        body.write(crlf);
        body.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        body.write(crlf);

        byte[] bodyBytes = body.toByteArray();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.YOUSIGN_BASE_URL + "/signature_requests/" + signatureRequestId + "/documents"))
                .header("Authorization", "Bearer " + ApiConfig.getYousignApiKey())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("[Yousign] Step 2/4 response: HTTP " + response.statusCode() + " body=" + response.body());
        logger.info("Yousign upload doc: " + response.statusCode() + " " + response.body());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
            String documentId = result.get("id").getAsString();
            System.out.println("[Yousign] Step 2/4 OK: document_id=" + documentId);
            return documentId;
        }

        System.err.println("[Yousign] Step 2/4 FAILED: " + response.body());
        throw new RuntimeException("Failed to upload document: " + response.body());
    }

    /** Add a signer with a signature field. Explicit delivery_mode and auth so Yousign sends the email. */
    public void addSigner(String signatureRequestId, String documentId,
                           String email, String firstName, String lastName, int signatureY) throws Exception {
        String safeFirst = (firstName != null && !firstName.isBlank()) ? firstName.trim() : "Signer";
        String safeLast = (lastName != null && !lastName.isBlank()) ? lastName.trim() : "Signer";

        JsonObject signer = new JsonObject();
        JsonObject info = new JsonObject();
        info.addProperty("first_name", safeFirst);
        info.addProperty("last_name", safeLast);
        info.addProperty("email", email);
        info.addProperty("locale", "fr");
        signer.add("info", info);
        signer.addProperty("signature_level", "electronic_signature");
        signer.addProperty("signature_authentication_mode", "otp_email");
        signer.addProperty("delivery_mode", "email");

        JsonObject field = new JsonObject();
        field.addProperty("type", "signature");
        field.addProperty("document_id", documentId);
        field.addProperty("page", 1);
        field.addProperty("x", 77);
        field.addProperty("y", signatureY);
        field.addProperty("width", 200);
        field.addProperty("height", 60);
        field.addProperty("reason", "Contract signature");

        com.google.gson.JsonArray fields = new com.google.gson.JsonArray();
        fields.add(field);
        signer.add("fields", fields);

        String body = signer.toString();
        System.out.println("[Yousign] Adding signer: email=" + email + " first_name=" + safeFirst + " last_name=" + safeLast);
        logger.info("Yousign add signer payload: " + body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.YOUSIGN_BASE_URL + "/signature_requests/" + signatureRequestId + "/signers"))
                .header("Authorization", "Bearer " + ApiConfig.getYousignApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[Yousign] Add signer response: HTTP " + response.statusCode() + " body=" + response.body());
        logger.info("Yousign add signer (" + email + "): " + response.statusCode() + " " + response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            System.err.println("[Yousign] Add signer FAILED: " + response.body());
            throw new RuntimeException("Yousign add signer failed: " + response.statusCode() + " " + response.body());
        }
    }

    public void activateSignatureRequest(String signatureRequestId) throws Exception {
        System.out.println("[Yousign] Step 4/4: Activating signature request " + signatureRequestId + " (this triggers emails to signers)...");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.YOUSIGN_BASE_URL + "/signature_requests/" + signatureRequestId + "/activate"))
                .header("Authorization", "Bearer " + ApiConfig.getYousignApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[Yousign] Step 4/4 activate response: HTTP " + response.statusCode() + " body=" + response.body());
        logger.info("Yousign activate: " + response.statusCode() + " " + response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            System.err.println("[Yousign] Step 4/4 FAILED: " + response.body());
            throw new RuntimeException("Yousign activate failed: " + response.statusCode() + " " + response.body());
        }
        System.out.println("[Yousign] Step 4/4 OK: Signature request activated. Emails should be sent to signers (check inbox and spam).");
    }

    public String getSignatureRequestStatus(String signatureRequestId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.YOUSIGN_BASE_URL + "/signature_requests/" + signatureRequestId))
                .header("Authorization", "Bearer " + ApiConfig.getYousignApiKey())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
            return result.get("status").getAsString();
        }
        return "unknown";
    }

    /** After activation, get the signature link for each signer (fallback if email is not received). */
    public List<Map.Entry<String, String>> getSignatureLinks(String signatureRequestId) throws Exception {
        System.out.println("[Yousign] Fetching signature links for request " + signatureRequestId + "...");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.YOUSIGN_BASE_URL + "/signature_requests/" + signatureRequestId + "/signers"))
                .header("Authorization", "Bearer " + ApiConfig.getYousignApiKey())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[Yousign] Get signers response: HTTP " + response.statusCode() + " body=" + response.body());
        List<Map.Entry<String, String>> links = new ArrayList<>();
        if (response.statusCode() < 200 || response.statusCode() >= 300) return links;

        com.google.gson.JsonElement root = JsonParser.parseString(response.body());

        com.google.gson.JsonArray signersArray = null;
        if (root.isJsonArray()) {
            signersArray = root.getAsJsonArray();
        } else if (root.isJsonObject()) {
            JsonObject body = root.getAsJsonObject();
            if (body.has("data") && body.get("data").isJsonArray()) {
                signersArray = body.getAsJsonArray("data");
            } else if (body.has("signers") && body.get("signers").isJsonArray()) {
                signersArray = body.getAsJsonArray("signers");
            }
        }

        if (signersArray != null) {
            for (var el : signersArray) {
                if (!el.isJsonObject()) continue;
                JsonObject signer = el.getAsJsonObject();
                String email = signer.has("info") && signer.get("info").isJsonObject()
                        && signer.getAsJsonObject("info").has("email")
                        ? signer.getAsJsonObject("info").get("email").getAsString() : "?";
                String link = signer.has("signature_link") && signer.get("signature_link") != null
                        && !signer.get("signature_link").isJsonNull()
                        ? signer.get("signature_link").getAsString() : null;

                String preview = signer.has("signature_image_preview") && signer.get("signature_image_preview") != null
                        && !signer.get("signature_image_preview").isJsonNull()
                        ? signer.get("signature_image_preview").getAsString() : null;

                if (link != null && !link.isBlank()) {
                    links.add(Map.entry(email, link));
                    System.out.println("[Yousign] Signature link for " + email + ": " + link);
                } else if (preview != null && !preview.isBlank()) {
                    links.add(Map.entry(email, preview));
                    System.out.println("[Yousign] Preview link for " + email + ": " + preview);
                } else {
                    System.out.println("[Yousign] No signature link yet for " + email + " (status: " +
                            (signer.has("status") ? signer.get("status").getAsString() : "unknown") +
                            "). Email was sent by Yousign — check inbox/spam.");
                }
            }
        }
        System.out.println("[Yousign] Total signature links retrieved: " + links.size());
        return links;
    }

    public Task<String> createAndSendForSignatureAsync(String title, String pdfPath,
                                                        String buyerEmail, String buyerName,
                                                        String sellerEmail, String sellerName) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                System.out.println("[Yousign] ========== YOUSIGN SIGNATURE PHASE START ==========");
                System.out.println("[Yousign] Buyer email: " + buyerEmail + " | Seller email: " + sellerEmail);

                String requestId = createSignatureRequest(title);
                updateProgress(1, 4);

                String documentId = uploadDocument(requestId, pdfPath);
                updateProgress(2, 4);

                boolean sameEmail = buyerEmail != null && buyerEmail.equalsIgnoreCase(sellerEmail);
                if (sameEmail) {
                    System.out.println("[Yousign] Step 3/4: Same email for buyer and seller - adding one signer only (Yousign does not allow duplicate emails).");
                    String[] parts = splitName(buyerName);
                    addSigner(requestId, documentId, buyerEmail, parts[0], parts[1], 541);
                } else {
                    System.out.println("[Yousign] Step 3/4: Adding signer 1 (buyer)...");
                    String[] buyerParts = splitName(buyerName);
                    addSigner(requestId, documentId, buyerEmail, buyerParts[0], buyerParts[1], 581);
                    System.out.println("[Yousign] Step 3/4: Adding signer 2 (seller)...");
                    String[] sellerParts = splitName(sellerName);
                    addSigner(requestId, documentId, sellerEmail, sellerParts[0], sellerParts[1], 500);
                }
                updateProgress(3, 4);

                activateSignatureRequest(requestId);
                updateProgress(4, 4);

                System.out.println("[Yousign] ========== YOUSIGN SIGNATURE PHASE END (requestId=" + requestId + ") ==========");
                return requestId;
            }
        };
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"User", "BizHub"};
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length > 1 ? parts : new String[]{parts[0], ""};
    }
}
