package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for List<RecordType> serialization/deserialization.
 */
public class RecordCollectionClass extends YamlFileInterface {

    @YamlKey("address-list")
    public List<Address> addressList = new ArrayList<>();

    @YamlKey("person-list")
    public List<Person> personList = new ArrayList<>();

    public RecordCollectionClass() {
        // Initialize with default values for testing
        addressList.add(new Address("123 First St", "Town A", 11111));
        addressList.add(new Address("456 Second St", "Town B", 22222));

        personList.add(new Person("Alice", 28, new Address("789 Third St", "Town C", 33333)));
        personList.add(new Person("Bob", 35, new Address("321 Fourth St", "Town D", 44444)));
    }
}
