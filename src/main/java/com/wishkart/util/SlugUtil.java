package com.wishkart.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class for generating URL-friendly slugs.
 */
public final class SlugUtil {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern DASHES = Pattern.compile("-{2,}");

    private SlugUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generates a URL-friendly slug from the input string.
     *
     * @param input The input string to convert
     * @return A URL-friendly slug
     */
    public static String generateSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutDiacritics = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String lowercase = withoutDiacritics.toLowerCase(Locale.ENGLISH);
        String withDashes = WHITESPACE.matcher(lowercase).replaceAll("-");
        String latinOnly = NON_LATIN.matcher(withDashes).replaceAll("");
        String slug = DASHES.matcher(latinOnly).replaceAll("-");

        // Remove leading and trailing dashes
        slug = slug.replaceAll("^-+|-+$", "");

        return slug;
    }

    /**
     * Generates a unique slug by appending a counter if needed.
     *
     * @param baseSlug The base slug
     * @param counter The counter to append
     * @return A unique slug with counter
     */
    public static String generateUniqueSlug(String baseSlug, int counter) {
        if (counter <= 0) {
            return baseSlug;
        }
        return baseSlug + "-" + counter;
    }
}
