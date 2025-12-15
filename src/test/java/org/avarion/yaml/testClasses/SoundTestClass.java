package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;
import org.bukkit.Sound;

public class SoundTestClass extends YamlFileInterface {
    @YamlKey("name")
    public Sound name = Sound.A;
}
