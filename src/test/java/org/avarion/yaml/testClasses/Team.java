package org.avarion.yaml.testClasses;

import java.util.List;

/**
 * A record representing a team with a list of Person records.
 * Used for testing collections of records within a record.
 */
public record Team(String name, List<Person> members, Address headquarters) {
}
