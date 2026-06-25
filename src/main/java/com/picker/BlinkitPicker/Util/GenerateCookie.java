package com.picker.BlinkitPicker.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Random;
import java.util.UUID;

/**
 * Utility class for generating Cloudflare Bot Management (__cf_bm) cookies
 * and per-request UUIDs used in Blinkit API calls.
 */
public class GenerateCookie {

    private static final Random RANDOM = new Random();

    /**
     * Generates a fresh, unique __cf_bm cookie value.
     *
     * Format: <40-char SHA-256 hex>-<unix_ts>-1.0.1.1-<50-char SHA-256 hex>
     *
     * @return a freshly generated __cf_bm cookie string
     */
    public static String generateCfBmCookie() {
        long ts = Instant.now().getEpochSecond();
        String p1 = sha256Hex(String.valueOf(RANDOM.nextDouble())).substring(0, 40);
        String p2 = sha256Hex(String.valueOf(RANDOM.nextDouble())).substring(0, 50);
        return p1 + "-" + ts + "-1.0.1.1-" + p2;
    }

    /**
     * Generates a fresh per-request UUID (v4).
     * Use as the x-request-id header on every outbound API call.
     *
     * @return a random UUID string, e.g. "550e8400-e29b-41d4-a716-446655440000"
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Computes the lowercase hex-encoded SHA-256 digest of the given input string.
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
