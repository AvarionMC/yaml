package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;

/**
 * Jaro-Winkler string similarity, for working out what a mistyped config value
 * was probably meant to be.
 *
 * <p>Hand-rolled rather than pulled in: this library is shaded into every plugin
 * that uses it, and forty lines of arithmetic is not worth a dependency in
 * everybody's jar. Jaro-Winkler rather than an edit distance because it favours
 * strings that start alike, which is what a typo in a constant name looks like.
 */
final class Similarity {

    /** Winkler's constant: how much of the remaining distance a common prefix closes, per character. */
    private static final double PREFIX_WEIGHT = 0.1;

    /** Winkler only rewards the first four characters of a shared prefix. */
    private static final int MAX_PREFIX = 4;

    private Similarity() {
    }

    /**
     * How alike two strings are, from 0 (nothing in common) to 1 (identical).
     * Case is ignored, since the field lookup this backs ignores it too.
     */
    static double jaroWinkler(final @NotNull String left, final @NotNull String right) {
        String a = left.toUpperCase();
        String b = right.toUpperCase();

        double jaro = jaro(a, b);
        int prefix = 0;
        while (prefix < Math.min(MAX_PREFIX, Math.min(a.length(), b.length())) && a.charAt(prefix) == b.charAt(prefix)) {
            prefix++;
        }
        return jaro + prefix * PREFIX_WEIGHT * (1 - jaro);
    }

    /**
     * The Jaro half: how many characters the two share close enough to count,
     * discounted by how many of those are in the wrong order.
     */
    private static double jaro(final @NotNull String a, final @NotNull String b) {
        if (a.isEmpty() || b.isEmpty()) return a.isEmpty() && b.isEmpty() ? 1 : 0;

        // Two characters count as the same one only if they are near enough to
        // have been the same one - half the longer string, give or take.
        int window = Math.max(0, Math.max(a.length(), b.length()) / 2 - 1);

        boolean[] usedInA = new boolean[a.length()];
        boolean[] usedInB = new boolean[b.length()];
        int matches = 0;

        for (int i = 0; i < a.length(); i++) {
            for (int j = Math.max(0, i - window); j < Math.min(b.length(), i + window + 1); j++) {
                if (usedInB[j] || a.charAt(i) != b.charAt(j)) continue;
                usedInA[i] = true;
                usedInB[j] = true;
                matches++;
                break;
            }
        }
        if (matches == 0) return 0;

        // Matched characters read off in order; every pair that disagrees is half
        // a transposition, because it takes two to swap.
        int transpositions = 0;
        int k = 0;
        for (int i = 0; i < a.length(); i++) {
            if (!usedInA[i]) continue;
            while (!usedInB[k]) k++;
            if (a.charAt(i) != b.charAt(k)) transpositions++;
            k++;
        }

        double m = matches;
        return (m / a.length() + m / b.length() + (m - transpositions / 2.0) / m) / 3.0;
    }
}
