package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

public class SoundsConfig extends YamlFileInterface {
    @YamlKey("name")
    public Sounds sound = Sounds.MY_SOUND_ROCKS;
}
