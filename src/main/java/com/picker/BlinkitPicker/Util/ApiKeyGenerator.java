package com.picker.BlinkitPicker.Util;

import java.security.SecureRandom;
import java.util.Base64;

public class ApiKeyGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateApiKey() {
        // Generate 32 bytes of secure random data
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        // Encode using Base64 URL-safe (no +, /, or padding =)
        String randomString = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Attach your professional prefix
        return "Bp" + randomString;
    }
}
