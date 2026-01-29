package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test class for Map<String, RecordWithCustomConstructor>.
 */
public class AltarConfigClass extends YamlFileInterface {

    @YamlKey("altars")
    public Map<String, AltarDefinition> altars = new LinkedHashMap<>();

    public AltarConfigClass() {
        // Using the 7-parameter constructor
        altars.put("blood", new AltarDefinition(
                "blood",
                "&4Blood Altar",
                List.of("&7A dark altar", "&7for blood rituals"),
                TestMaterial.ENCHANTING_TABLE,
                1001,
                "blood_essence",  // Will be parsed: name="blood_essence", amount=1
                Map.of(TestMaterial.REDSTONE, 16, TestMaterial.GHAST_TEAR, 1)
        ));

        // Using the 8-parameter canonical constructor
        altars.put("light", new AltarDefinition(
                "light",
                "&eLight Altar",
                List.of("&7A bright altar", "&7for purification"),
                TestMaterial.ENCHANTING_TABLE,
                1002,
                "light_essence",
                5,  // Explicit amount
                Map.of(TestMaterial.DIAMOND, 4, TestMaterial.GOLD_INGOT, 8)
        ));
    }
}
