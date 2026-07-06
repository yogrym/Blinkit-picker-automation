package com.picker.BlinkitPicker.Util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateToUtc {

    /**
     * Converts a date in "yyyy-MM-dd" format to the END date used by the Slots API.
     *
     * Bot logic: end_date = that day at 18:30:00 UTC (= midnight IST / start of IST day)
     * Format: "yyyy-MM-ddT18:30:00.000Z"
     */
    public static String getDateToUtc(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            LocalDate localDate = LocalDate.parse(date.trim());
            return localDate.atTime(18, 30, 0)
                    .atZone(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to parse date string: '" + date + "'. Expected format is yyyy-MM-dd", e);
        }
    }

    /**
     * Returns the START date (1 day before end_date) for the Slots API.
     *
     * Bot logic: start_date = (dt - timedelta(days=1)).strftime("%Y-%m-%dT%H:%M:%SZ")
     */
    public static String getPrevDateToUtc(String endDateUtc) {
        if (endDateUtc == null || endDateUtc.isBlank()) {
            return null;
        }
        try {
            String normalized = endDateUtc.replace(".000Z", "Z");
            return Instant.parse(normalized)
                    .minus(1, ChronoUnit.DAYS)
                    .toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to parse UTC date string: '" + endDateUtc + "'", e);
        }
    }

    /**
     * Decodes UTC start/end times to a friendly IST label like "6-8 pm".
     * Used for logging/display only.
     */
    public static String decodeTime(String startTime, String endTime) {
        if (startTime == null || endTime == null || startTime.isBlank() || endTime.isBlank()) {
            return "";
        }
        try {
            ZonedDateTime start = Instant.parse(startTime.trim()).atZone(ZoneId.of("Asia/Kolkata"));
            ZonedDateTime end   = Instant.parse(endTime.trim()).atZone(ZoneId.of("Asia/Kolkata"));

            int startHour = start.getHour();
            int endHour   = end.getHour();
            int start12   = startHour > 12 ? startHour - 12 : (startHour == 0 ? 12 : startHour);
            int end12     = endHour   > 12 ? endHour   - 12 : (endHour   == 0 ? 12 : endHour);
            String amPm   = startHour >= 12 ? "pm" : "am";

            return start12 + "-" + end12 + " " + amPm;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Converts UTC start/end times to an IST "HH:MM-HH:MM" key string.
     *
     * Mirrors the bot's _slot_time_key(). User time_filter values are stored
     * in this format (e.g., "06:00-08:00"). This key is used for slot matching.
     *
     * Example: UTC 00:30 → 02:30 becomes IST "06:00-08:00"
     */
    public static String slotTimeKey(String startTime, String endTime) {
        if (startTime == null || endTime == null || startTime.isBlank() || endTime.isBlank()) {
            return "";
        }
        try {
            ZonedDateTime start = Instant.parse(startTime.trim()).atZone(ZoneId.of("Asia/Kolkata"));
            ZonedDateTime end   = Instant.parse(endTime.trim()).atZone(ZoneId.of("Asia/Kolkata"));
            return String.format("%02d:%02d-%02d:%02d",
                    start.getHour(), start.getMinute(),
                    end.getHour(),   end.getMinute());
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isTimeMatch(String preferredKey, String startTime, String endTime) {
        if (preferredKey == null || preferredKey.isBlank()) {
            return false;
        }
        if (startTime == null || endTime == null || startTime.isBlank() || endTime.isBlank()) {
            return false;
        }
        try {
            ZonedDateTime start = Instant.parse(startTime.trim()).atZone(ZoneId.of("Asia/Kolkata"));
            ZonedDateTime end   = Instant.parse(endTime.trim()).atZone(ZoneId.of("Asia/Kolkata"));

            int sh = start.getHour();
            int sm = start.getMinute();
            int eh = end.getHour();
            int em = end.getMinute();

            // Clean the preferredKey for uniform comparison
            // Remove all spaces and normalize multiple hyphens/dashes to a single hyphen
            String cleanPreferred = preferredKey.trim().toLowerCase()
                    .replaceAll("\\s+", "")
                    .replaceAll("-+", "-");

            // Let's generate possible clean representations of the slot time:
            java.util.List<String> candidates = new java.util.ArrayList<>();

            // 1. 24-hour formats
            // "20:00-22:00", "20-22"
            candidates.add(String.format("%02d:%02d-%02d:%02d", sh, sm, eh, em));
            candidates.add(String.format("%d:%02d-%d:%02d", sh, sm, eh, em));
            if (sm == 0 && em == 0) {
                candidates.add(String.format("%d-%d", sh, eh));
            }

            // 2. 12-hour formats
            int sh12 = sh > 12 ? sh - 12 : (sh == 0 ? 12 : sh);
            int eh12 = eh > 12 ? eh - 12 : (eh == 0 ? 12 : eh);
            String shAmPm = sh >= 12 ? "pm" : "am";
            String ehAmPm = eh >= 12 ? "pm" : "am";

            // "10:00am-12:00pm", "10am-12pm", "08:00pm-10:00pm"
            candidates.add(String.format("%02d:%02d%s-%02d:%02d%s", sh12, sm, shAmPm, eh12, em, ehAmPm));
            candidates.add(String.format("%d:%02d%s-%d:%02d%s", sh12, sm, shAmPm, eh12, em, ehAmPm));
            
            if (sm == 0 && em == 0) {
                candidates.add(String.format("%d%s-%d%s", sh12, shAmPm, eh12, ehAmPm));
                // Sometimes people write "8-10pm" instead of "8pm-10pm"
                candidates.add(String.format("%d-%d%s", sh12, eh12, ehAmPm));
                candidates.add(String.format("%d-%d%s", sh12, eh12, shAmPm));
                candidates.add(String.format("%d-%d", sh12, eh12)); // e.g. "8-10" without am/pm
            }

            // Let's check if cleanPreferred matches any clean candidate
            for (String candidate : candidates) {
                String cleanCandidate = candidate.replaceAll("\\s+", "").replaceAll("-+", "-");
                if (cleanPreferred.equals(cleanCandidate)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }
}
