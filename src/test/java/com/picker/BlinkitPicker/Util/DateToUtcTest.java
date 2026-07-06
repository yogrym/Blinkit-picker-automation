package com.picker.BlinkitPicker.Util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DateToUtcTest {

    @Test
    void matchesAmTimeRanges() {
        // UTC 04:30 -> IST 10:00 AM, UTC 06:30 -> IST 12:00 PM
        String start = "2026-07-05T04:30:00Z";
        String end = "2026-07-05T06:30:00Z";

        assertThat(DateToUtc.isTimeMatch("10:00 AM - 12:00 PM", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("10:00 AM-12:00 PM", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("10 AM - 12 PM", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("10-12 am", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("10-12am", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("10:00-12:00", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("10-12", start, end)).isTrue();

        // Non-matching
        assertThat(DateToUtc.isTimeMatch("8-10am", start, end)).isFalse();
        assertThat(DateToUtc.isTimeMatch("12-2pm", start, end)).isFalse();
    }

    @Test
    void matchesPmTimeRanges() {
        // UTC 14:30 -> IST 20:00 (8:00 PM), UTC 16:30 -> IST 22:00 (10:00 PM)
        String start = "2026-07-05T14:30:00Z";
        String end = "2026-07-05T16:30:00Z";

        assertThat(DateToUtc.isTimeMatch("8-10pm", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("8-10 pm", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("8:00 PM - 10:00 PM", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("20:00-22:00", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("8-10", start, end)).isTrue();

        // Non-matching
        assertThat(DateToUtc.isTimeMatch("8-10am", start, end)).isFalse();
        assertThat(DateToUtc.isTimeMatch("2-4pm", start, end)).isFalse();
    }

    @Test
    void matchesMultipleHyphensAndVaryingAmPm() {
        // UTC 08:30 -> IST 14:00 (2:00 PM), UTC 10:30 -> IST 16:00 (4:00 PM)
        String start = "2026-07-05T08:30:00Z";
        String end = "2026-07-05T10:30:00Z";

        assertThat(DateToUtc.isTimeMatch("2--4pm", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("2-4 pm", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("2-4PM", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("14:00-16:00", start, end)).isTrue();
        assertThat(DateToUtc.isTimeMatch("2-4", start, end)).isTrue();

        // Non-matching
        assertThat(DateToUtc.isTimeMatch("2-4am", start, end)).isFalse();
    }
}
