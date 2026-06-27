package com.picker.BlinkitPicker.Util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class DateToUtc {

    /**
     * Converts a list of dates in "yyyy-MM-dd" format to UTC ISO-8601 format
     * ("yyyy-MM-ddT02:30:00Z").
     * This assumes the source time is 8:00 AM Indian Standard Time (IST), which
     * equates to 2:30 AM UTC.
     * 
     * @param dates List of date strings in "yyyy-MM-dd" format
     * @return List of converted UTC date-time strings
     */
    public static String getDateToUtc(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            LocalDate localDate = LocalDate.parse(date.trim());
            // 8:00 AM IST is 2:30 AM UTC
            return localDate.atTime(8, 0)
                    .atZone(ZoneId.of("Asia/Kolkata"))
                    .toInstant()
                    .toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse date string: '" + date + "'. Expected format is yyyy-MM-dd", e);
        }
    }

    /**
     * Subtracts exactly 1 day from the given UTC date-time string.
     * Example: "2025-09-02T02:30:00Z" -> "2025-09-01T02:30:00Z"
     * 
     * @param utcDate The UTC date-time string in ISO-8601 format (e.g.,
     *                "2025-09-02T02:30:00Z")
     * @return The previous date-time string in UTC format
     */
    public static String getPrevDateToUtc(String utcDate) {
        if (utcDate == null || utcDate.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(utcDate.trim())
                    .minus(1, ChronoUnit.DAYS)
                    .toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse UTC date string: '" + utcDate
                    + "'. Expected format is ISO-8601 UTC (e.g., 2025-09-02T02:30:00Z)", e);
        }
    }
}
