package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlComment;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class holding records whose components are annotated with {@link YamlComment}.
 */
public class CommentedRecordClass extends YamlFileInterface {

    @YamlComment("Home address")
    @YamlKey("address")
    public CommentedAddress address = new CommentedAddress("123 Main St", "Springfield", 12345);

    @YamlKey("person")
    public CommentedPerson person = new CommentedPerson("John Doe", 30, new CommentedAddress("456 Work St", "Shelbyville", 67890));

    @YamlKey("previous-addresses")
    public List<CommentedAddress> previousAddresses = new ArrayList<>();

    public CommentedRecordClass() {
        previousAddresses.add(new CommentedAddress("1 Old Rd", "Ogdenville", 11111));
        previousAddresses.add(new CommentedAddress("2 Older Rd", "North Haverbrook", 22222));
    }
}
