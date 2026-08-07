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

        boolean[] usedInA = new boolean[a.length()];
        boolean[] usedInB = new boolean[b.length()];

        double matches = pairUp(a, b, usedInA, usedInB);
        if (matches == 0) return 0;

        double swapped = transpositions(a, b, usedInA, usedInB) / 2.0;
        return (matches / a.length() + matches / b.length() + (matches - swapped) / matches) / 3.0;
    }

    /**
     * Pairs each character of {@code a} with an unclaimed equal character of
     * {@code b}, marking both, and answers how many pairs were made.
     */
    private static int pairUp(final @NotNull String a, final @NotNull String b,
                              final boolean @NotNull [] usedInA, final boolean @NotNull [] usedInB) {
        // Two characters count as the same one only if they are near enough to
        // have been the same one - half the longer string, give or take.
        int window = Math.max(0, Math.max(a.length(), b.length()) / 2 - 1);
        int matches = 0;

        for (int i = 0; i < a.length(); i++) {
            int found = claim(a.charAt(i), b, usedInB, Math.max(0, i - window), Math.min(b.length(), i + window + 1));
            if (found >= 0) {
                usedInA[i] = true;
                matches++;
            }
        }
        return matches;
    }

    /**
     * The first unclaimed {@code wanted} in {@code b} between {@code from} and
     * {@code to}, marked as claimed — or {@code -1} if there is none. Claimed on
     * the spot rather than by the caller, so one character of the other string
     * cannot be spent twice.
     */
    private static int claim(final char wanted, final @NotNull String b, final boolean @NotNull [] usedInB,
                             final int from, final int to) {
        for (int j = from; j < to; j++) {
            if (!usedInB[j] && b.charAt(j) == wanted) {
                usedInB[j] = true;
                return j;
            }
        }
        return -1;
    }

    /**
     * How many of the paired-up characters disagree when both strings are read
     * off in order. Every disagreement is half a swap, because it takes two.
     */
    private static int transpositions(final @NotNull String a, final @NotNull String b,
                                      final boolean @NotNull [] usedInA, final boolean @NotNull [] usedInB) {
        int transpositions = 0;
        int k = 0;

        for (int i = 0; i < a.length(); i++) {
            if (usedInA[i]) {
                while (!usedInB[k]) k++;
                if (a.charAt(i) != b.charAt(k)) transpositions++;
                k++;
            }
        }
        return transpositions;
    }
}
