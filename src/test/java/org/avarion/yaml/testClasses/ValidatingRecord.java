package org.avarion.yaml.testClasses;

/**
 * A record whose compact constructor throws on invalid input,
 * used to test error handling in convertMapToRecord.
 */
public record ValidatingRecord(String name, int value) {
    public ValidatingRecord {
        if (value < 0) {
            throw new IllegalArgumentException("value must be non-negative");
        }
    }
}
