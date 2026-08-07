package org.avarion.yaml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The similarity used to guess what a mistyped config value was meant to be.
 *
 * <p>Hand-rolled arithmetic that nothing else would catch if it drifted, so the
 * numbers below are the published Jaro-Winkler values for the pairs the
 * literature uses, not whatever this implementation happened to return.
 */
class SimilarityTest {

    private static final double TOLERANCE = 0.0001;

    @ParameterizedTest
    @CsvSource({
            "MARTHA,   MARHTA,    0.961111",
            "DIXON,    DICKSONX,  0.813333",
            "JELLYFISH,SMELLYFISH,0.896296",
            "CRATE,    TRACE,     0.733333",
            "DWAYNE,   DUANE,     0.840000",
    })
    void theTextbookPairsComeOutAtTheirTextbookScores(String left, String right, double expected) {
        assertEquals(expected, Similarity.jaroWinkler(left, right), TOLERANCE);
    }

    @Test
    void identicalStringsScoreOne() {
        assertEquals(1.0, Similarity.jaroWinkler("PROTECTION", "PROTECTION"), TOLERANCE);
    }

    @Test
    void caseIsNotADifference() {
        assertEquals(1.0, Similarity.jaroWinkler("protection", "PROTECTION"), TOLERANCE);
    }

    @Test
    void stringsWithNothingInCommonScoreZero() {
        assertEquals(0.0, Similarity.jaroWinkler("ABC", "XYZ"), TOLERANCE);
    }

    @Test
    void anEmptyStringIsLikeNothingButAnotherEmptyString() {
        assertEquals(1.0, Similarity.jaroWinkler("", ""), TOLERANCE);
        assertEquals(0.0, Similarity.jaroWinkler("", "SHARPNESS"), TOLERANCE);
        assertEquals(0.0, Similarity.jaroWinkler("SHARPNESS", ""), TOLERANCE);
    }

    @Test
    void aSingleCharacterEitherSideDoesNotDivideByZero() {
        // window is max(len)/2 - 1, which goes negative for these and has to be clamped.
        assertEquals(1.0, Similarity.jaroWinkler("A", "A"), TOLERANCE);
        assertEquals(0.0, Similarity.jaroWinkler("A", "B"), TOLERANCE);
    }

    @Test
    void aTypoScoresAboveAnUnrelatedName() {
        // The whole point: the near-miss has to win, or the suggestion is noise.
        double typo = Similarity.jaroWinkler("SHARPNES", "SHARPNESS");
        double unrelated = Similarity.jaroWinkler("SHARPNES", "AQUA_AFFINITY");

        assertTrue(typo > unrelated, "%f should beat %f".formatted(typo, unrelated));
    }
}
