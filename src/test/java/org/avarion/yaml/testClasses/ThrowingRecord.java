package org.avarion.yaml.testClasses;

/**
 * A record whose accessor throws, used to test error handling in recordToMap.
 */
public record ThrowingRecord(String name) {
    @Override
    public String name() {
        throw new RuntimeException("accessor failure");
    }
}
