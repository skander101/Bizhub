package com.bizhub.model.services.common.service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * TotpService: Handles TOTP (Time-based One-Time Password) for authenticator apps.
 *
 * Supports Google Authenticator, Authy, Microsoft Authenticator, and other standard TOTP apps.
 *
 * Flow:
 * 1. Generate a secret for the user
 * 2. Generate QR code for the user to scan with their authenticator app
 * 3. Verify the TOTP code entered by the user
 */
public class TotpService {

    private static final String ISSUER = "BizHub";

    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;

    public TotpService() {
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();

        // Setup code verifier with default settings (SHA1, 6 digits, 30 second period)
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    /**
     * Generate a new TOTP secret for a user.
     *
     * @return The generated secret (Base32 encoded)
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Generate QR code data URL for the user to scan with their authenticator app.
     *
     * @param userEmail The user's email (used as the account name)
     * @param secret The TOTP secret
     * @return Base64 encoded PNG image data URL
     */
    public String generateQrCodeDataUrl(String userEmail, String secret) {
        QrData data = new QrData.Builder()
                .label(userEmail)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        try {
            byte[] imageData = qrGenerator.generate(data);
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            return "data:image/png;base64," + base64Image;
        } catch (QrGenerationException e) {
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage(), e);
        }
    }

    /**
     * Get the otpauth:// URI for manual entry in authenticator apps.
     *
     * @param userEmail The user's email
     * @param secret The TOTP secret
     * @return The otpauth URI
     */
    public String getOtpAuthUri(String userEmail, String secret) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                ISSUER, userEmail, secret, ISSUER);
    }

    /**
     * Verify a TOTP code entered by the user.
     *
     * @param secret The user's TOTP secret
     * @param code The 6-digit code from the authenticator app
     * @return true if the code is valid
     */
    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    /**
     * Async version of verifyCode.
     */
    public CompletableFuture<Boolean> verifyCodeAsync(String secret, String code) {
        return CompletableFuture.supplyAsync(() -> verifyCode(secret, code));
    }

    /**
     * Setup TOTP for a user - generates secret and QR code.
     *
     * @param userEmail The user's email
     * @return TotpSetupResult containing the secret and QR code
     */
    public TotpSetupResult setupTotp(String userEmail) {
        String secret = generateSecret();
        String qrCodeDataUrl = generateQrCodeDataUrl(userEmail, secret);
        return new TotpSetupResult(secret, qrCodeDataUrl);
    }

    /**
     * Result class for TOTP setup containing all necessary info.
     */
    public static class TotpSetupResult {
        private final String secret;
        private final String qrCodeDataUrl;

        public TotpSetupResult(String secret, String qrCodeDataUrl) {
            this.secret = secret;
            this.qrCodeDataUrl = qrCodeDataUrl;
        }

        /**
         * Get the TOTP secret (Base32 encoded).
         * This should be stored securely for the user.
         */
        public String getSecret() {
            return secret;
        }

        /**
         * Get the QR code as a data URL (base64 encoded PNG).
         * Can be used directly as an image source.
         */
        public String getQrCodeDataUrl() {
            return qrCodeDataUrl;
        }
    }
}

