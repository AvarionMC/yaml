package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.Arrays;
import java.util.List;

public class ListNoParam extends YamlFileInterface {
    @YamlKey("key")
    public List key = Arrays.asList(1, 2);
}
