package com.flowops.common.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Derives the globally unique, URL-safe {@code slug} for an organization
 * (contract §4.2). Clients never send a slug and must not predict one.
 */
public final class Slugs {

    /** Everything outside the output alphabet collapses to a single dash. */
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");

    /** Combining marks left behind by NFKD decomposition. */
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private static final int MAX_LENGTH = 80;

    /** Used when the input contains no slug-able character at all (e.g. "日本語" or "!!!"). */
    private static final String FALLBACK = "org";

    private Slugs() {
    }

    /**
     * The base slug for a name, before uniqueness is considered: lowercase →
     * NFKD-normalize and strip diacritics → collapse non-alphanumerics to dashes →
     * trim edge dashes → truncate to 80 chars.
     */
    public static String slugify(String name) {
        if (name == null || name.isBlank()) {
            return FALLBACK;
        }

        String decomposed = Normalizer.normalize(name.toLowerCase(Locale.ROOT), Normalizer.Form.NFKD);
        String ascii = DIACRITICS.matcher(decomposed).replaceAll("");
        String dashed = NON_ALNUM.matcher(ascii).replaceAll("-");
        String trimmed = EDGE_DASHES.matcher(dashed).replaceAll("");

        if (trimmed.isEmpty()) {
            return FALLBACK;
        }
        if (trimmed.length() <= MAX_LENGTH) {
            return trimmed;
        }
        // Truncating can expose a dash at the new boundary; strip it again.
        return EDGE_DASHES.matcher(trimmed.substring(0, MAX_LENGTH)).replaceAll("");
    }

    /**
     * The first free slug for {@code name}, appending {@code -2}, {@code -3}, … on
     * collision.
     *
     * <p>This is a check-then-insert and therefore racy under concurrent creates
     * with identical names. The {@code UNIQUE} constraint on
     * {@code organizations.slug} is the real guarantee; the caller retries on
     * violation. The loop exists to make the common case produce a readable slug,
     * not to enforce uniqueness.
     *
     * @param taken returns {@code true} when a candidate is already in use
     */
    public static String uniqueSlug(String name, Predicate<String> taken) {
        String base = slugify(name);
        if (!taken.test(base)) {
            return base;
        }
        // The suffix must not push the slug past the column width.
        for (int suffix = 2; suffix < 10_000; suffix++) {
            String candidate = truncateForSuffix(base, suffix) + "-" + suffix;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
        // Astronomically unlikely; a random tail beats failing the request.
        return truncateForSuffix(base, 0) + "-" + Long.toString(System.nanoTime(), 36);
    }

    private static String truncateForSuffix(String base, int suffix) {
        int room = MAX_LENGTH - (String.valueOf(suffix).length() + 1);
        return base.length() <= room ? base : base.substring(0, room);
    }
}
