package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

@SuppressWarnings("unused")
public class FinalKeyword extends YamlFileInterface {
    @YamlKey("key")
    public final int key = 1;
}
