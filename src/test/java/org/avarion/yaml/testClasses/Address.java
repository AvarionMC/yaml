package org.avarion.yaml.testClasses;

/**
 * A simple record representing an address.
 * Used for testing record support in YAML serialization/deserialization.
 */
public record Address(String street, String city, int zipCode) {
}
