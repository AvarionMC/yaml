package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

/**
 * Test class for simple record field serialization/deserialization.
 */
public class SimpleRecordClass extends YamlFileInterface {

    @YamlKey("address")
    public Address address = new Address("123 Main St", "Springfield", 12345);

    @YamlKey("person")
    public Person person = new Person("John Doe", 30, new Address("456 Work St", "Shelbyville", 67890));
}
