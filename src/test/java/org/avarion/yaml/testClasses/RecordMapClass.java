package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test class for Map<String, RecordType> serialization/deserialization.
 */
public class RecordMapClass extends YamlFileInterface {

    @YamlKey("addresses")
    public Map<String, Address> addresses = new LinkedHashMap<>();

    @YamlKey("people")
    public Map<String, Person> people = new LinkedHashMap<>();

    public RecordMapClass() {
        // Initialize with default values for testing
        addresses.put("home", new Address("123 Main St", "Springfield", 12345));
        addresses.put("work", new Address("456 Office Blvd", "Shelbyville", 67890));

        people.put("john", new Person("John Doe", 30, new Address("789 Elm St", "Capital City", 11111)));
        people.put("jane", new Person("Jane Smith", 25, new Address("321 Oak Ave", "Ogdenville", 22222)));
    }
}
