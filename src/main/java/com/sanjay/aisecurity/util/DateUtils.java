package com.sanjay.aisecurity.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Date and Time Utility.
 *
 * <p>Provides reusable date formatting and conversion methods used
 * across the platform for consistent timestamp handling.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
public final class DateUtils {

    /** ISO 8601 display formatter for API responses. */
    public static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** File name-safe formatter for report file names. */
    public static final DateTimeFormatter FILENAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Private constructor — utility class.
    private DateUtils() {
        throw new UnsupportedOperationException("DateUtils cannot be instantiated.");
    }

    /**
     * Formats a {@link LocalDateTime} to a human-readable string.
     *
     * @param dateTime the date-time to format
     * @return formatted string, or {@code "N/A"} if {@code dateTime} is null
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DISPLAY_FORMATTER);
    }

    /**
     * Formats a {@link LocalDateTime} to a file-name safe string.
     *
     * @param dateTime the date-time to format
     * @return file-name safe formatted string
     */
    public static String formatForFilename(LocalDateTime dateTime) {
        if (dateTime == null) {
            return LocalDateTime.now().format(FILENAME_FORMATTER);
        }
        return dateTime.format(FILENAME_FORMATTER);
    }

    /**
     * Computes the duration in seconds between two timestamps.
     *
     * @param start the start time
     * @param end   the end time
     * @return duration in seconds, or {@code 0} if either value is null
     */
    public static long durationInSeconds(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return java.time.Duration.between(start, end).getSeconds();
    }

    /**
     * Returns the current UTC timestamp.
     *
     * @return current UTC {@link LocalDateTime}
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneId.of("UTC"));
    }
}
