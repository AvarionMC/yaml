package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

@SuppressWarnings("unused")
public class DoubleKeyUsage extends YamlFileInterface {
    @YamlKey("key1")
    public int key1 = 1;

    @YamlKey("key1")
    public int key2 = 1;
}
