package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.Arrays;
import java.util.List;

public class ListYmlString extends YamlFileInterface {
    @YamlKey("key")
    public List<String> key = Arrays.asList("a", "b");
}
