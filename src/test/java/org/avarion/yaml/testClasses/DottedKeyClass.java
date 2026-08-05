package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

/**
 * Test class holding a record with an unsupported dotted component key.
 */
public class DottedKeyClass extends YamlFileInterface {

    @YamlKey("holder")
    public DottedKeyRecord holder = new DottedKeyRecord("something");
}
