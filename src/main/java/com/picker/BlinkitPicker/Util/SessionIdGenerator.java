package com.picker.BlinkitPicker.Util;

import java.util.UUID;

public class SessionIdGenerator {

    /**
     * Generates a random unique session ID.
     * @return A string representing the unique session ID.
     */
    public static String generateSessionId() {
        // Generating a UUID and removing hyphens for a cleaner session string
        return UUID.randomUUID().toString().replace("-", "");
    }
}
