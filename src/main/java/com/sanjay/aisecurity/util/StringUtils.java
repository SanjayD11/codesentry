package com.sanjay.aisecurity.util;

import java.util.UUID;

/**
 * String Utility.
 *
 * <p>Provides reusable text processing helpers used across services
 * and validation layers.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
public final class StringUtils {

    // Private constructor — utility class.
    private StringUtils() {
        throw new UnsupportedOperationException("StringUtils cannot be instantiated.");
    }

    /**
     * Returns {@code true} if the string is null or blank.
     *
     * @param value the string to check
     * @return {@code true} if null or blank
     */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Truncates a string to a maximum length, appending "..." if truncated.
     *
     * @param value     the original string
     * @param maxLength the maximum allowed length (including ellipsis)
     * @return the truncated string, or the original if within bounds
     */
    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    /**
     * Generates a unique file storage name combining a UUID prefix
     * and the original file name. Prevents naming collisions in storage.
     *
     * @param originalName the original file name
     * @return a collision-free storage file name
     */
    public static String generateStoredFileName(String originalName) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + "_" + sanitizeFileName(originalName);
    }

    /**
     * Sanitizes a file name by replacing characters that may cause
     * filesystem or path traversal issues.
     *
     * @param fileName the original file name
     * @return a safe, sanitized file name
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unnamed";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    /**
     * Extracts the file extension from a file name (including the leading dot).
     *
     * @param fileName the file name
     * @return the extension (e.g. {@code ".java"}), or an empty string if none
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
    }

    /**
     * Masks a sensitive string for safe log output.
     * Displays the first 4 characters and masks the rest.
     *
     * @param secret the sensitive value to mask
     * @return masked string (e.g. {@code "abcd****"})
     */
    public static String maskSecret(String secret) {
        if (isBlank(secret) || secret.length() < 4) {
            return "****";
        }
        return secret.substring(0, 4) + "*".repeat(secret.length() - 4);
    }

    /**
     * Converts a string to title case.
     * Example: "hello world" → "Hello World"
     *
     * @param input the input string
     * @return title-cased string
     */
    public static String toTitleCase(String input) {
        if (isBlank(input)) {
            return input;
        }
        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
}
