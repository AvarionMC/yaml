package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

public class CustomStringYml extends YamlFileInterface {
    @YamlKey("key")
    public CustomStringAcceptingClass key = new CustomStringAcceptingClass("str");
}
