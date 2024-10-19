package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

@YamlFile(header = "")
@SuppressWarnings("unused")
public class BlankHeader extends YamlFileInterface {
    @YamlKey("key")
    public int key = 1;
}
