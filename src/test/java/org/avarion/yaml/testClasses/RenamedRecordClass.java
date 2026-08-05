package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlComment;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

/**
 * Test class holding a record whose components are renamed via {@link YamlKey}.
 */
public class RenamedRecordClass extends YamlFileInterface {

    @YamlComment("Database settings")
    @YamlKey("database")
    public RenamedDatabase database = new RenamedDatabase("localhost", 5432, new RenamedCredentials("admin", "secret"));
}
