package org.avarion.yaml.testClasses;

/**
 * A record representing a person with a nested Address record.
 * Used for testing nested record support in YAML serialization/deserialization.
 */
public record Person(String name, int age, Address address) {
}
