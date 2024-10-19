package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

public class CustomNonStringYml extends YamlFileInterface {
    @YamlKey("key")
    public CustomNonStringAcceptingClass key = new CustomNonStringAcceptingClass(123);
}
