package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

/**
 * Config using the default naming strategy (SNAKE_CASE), with an explicitly named
 * camelCase field key that must survive untouched.
 */
public class SnakeCaseNamingClass extends YamlFileInterface {

    @YamlKey
    public NamingRecord derivedBlock = new NamingRecord("a", "b", "c", "d", "e");

    @YamlKey("anExplicitFieldKey")
    public String explicitField = "kept";
}
