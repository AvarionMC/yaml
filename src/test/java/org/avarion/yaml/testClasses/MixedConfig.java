package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;
import org.avarion.yaml.YamlMap;

import java.util.Map;

public class MixedConfig extends YamlFileInterface {
    @YamlMap(value = "bosses", processor = BossConfig.BossProcessor.class)
    public Map<String, Boss> bosses = Map.of("boss1", new Boss("name1", "internal1", "arena1"), "boss2", new Boss("name2", "internal2", "arena2"));

    @YamlKey("name")
    public String name = "Test Name";
}
