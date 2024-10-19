package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

public class NullOrEmptyKey extends YamlFileInterface {
    @YamlKey("")
    public String name1 = "A";

    public String name2 = "B";

    @YamlKey("save")
    public String name3 = "C";
}
