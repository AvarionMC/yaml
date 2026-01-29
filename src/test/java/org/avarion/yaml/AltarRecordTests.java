package org.avarion.yaml;

import org.avarion.yaml.testClasses.AltarConfigClass;
import org.avarion.yaml.testClasses.AltarDefinition;
import org.avarion.yaml.testClasses.TestMaterial;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for records with custom constructors and enum map keys.
 * Reproduces the issue reported with AltarDefinition record.
 */
class AltarRecordTests extends TestCommon {

    @Test
    void testRecordWithSecondaryConstructorSaveAndLoad() throws IOException {
        AltarConfigClass config = new AltarConfigClass();
        config.save(target);

        // Debug: print the YAML
        String yaml = readFile();
        System.out.println("Generated YAML:\n" + yaml);

        AltarConfigClass loaded = new AltarConfigClass();
        loaded.altars.clear();
        loaded.load(target);

        // Verify we have both altars
        assertNotNull(loaded.altars);
        assertEquals(2, loaded.altars.size());

        // Verify blood altar (created with 7-param constructor)
        AltarDefinition blood = loaded.altars.get("blood");
        assertNotNull(blood, "Blood altar should exist");
        assertEquals("blood", blood.name());
        assertEquals("&4Blood Altar", blood.displayName());
        assertEquals(List.of("&7A dark altar", "&7for blood rituals"), blood.lore());
        assertEquals(TestMaterial.ENCHANTING_TABLE, blood.material());
        assertEquals(1001, blood.modelId());
        assertEquals("blood_essence", blood.targetItemName());
        assertEquals(1, blood.targetItemAmount());  // Default from secondary constructor
        assertNotNull(blood.ingredients());
        assertEquals(16, blood.ingredients().get(TestMaterial.REDSTONE));
        assertEquals(1, blood.ingredients().get(TestMaterial.GHAST_TEAR));

        // Verify light altar (created with 8-param canonical constructor)
        AltarDefinition light = loaded.altars.get("light");
        assertNotNull(light, "Light altar should exist");
        assertEquals("light", light.name());
        assertEquals("&eLight Altar", light.displayName());
        assertEquals(5, light.targetItemAmount());  // Explicit amount
    }

    @Test
    void testRecordWithEnumMapKeys() throws IOException {
        AltarConfigClass config = new AltarConfigClass();
        config.save(target);

        AltarConfigClass loaded = new AltarConfigClass();
        loaded.altars.clear();
        loaded.load(target);

        // Verify enum keys in the ingredients map
        AltarDefinition blood = loaded.altars.get("blood");
        Map<TestMaterial, Integer> ingredients = blood.ingredients();

        assertNotNull(ingredients);
        assertEquals(2, ingredients.size());
        assertTrue(ingredients.containsKey(TestMaterial.REDSTONE));
        assertTrue(ingredients.containsKey(TestMaterial.GHAST_TEAR));
    }

    @Test
    void testYamlFormatForComplexRecord() throws IOException {
        AltarConfigClass config = new AltarConfigClass();
        config.save(target);

        String yaml = readFile();

        // Verify structure
        assertTrue(yaml.contains("altars:"), "Should have altars key");
        assertTrue(yaml.contains("blood:"), "Should have blood altar");
        assertTrue(yaml.contains("light:"), "Should have light altar");
        assertTrue(yaml.contains("name:"), "Should have name field");
        assertTrue(yaml.contains("displayName:"), "Should have displayName field");
        assertTrue(yaml.contains("lore:"), "Should have lore field");
        assertTrue(yaml.contains("material:"), "Should have material field");
        assertTrue(yaml.contains("ingredients:"), "Should have ingredients field");
        assertTrue(yaml.contains("ENCHANTING_TABLE"), "Should have material value");
        assertTrue(yaml.contains("REDSTONE"), "Should have ingredient key");
    }

    @Test
    void testLoadFromExistingYaml() throws IOException {
        // Write a YAML file manually (simulates existing config)
        String yaml = """
                altars:
                  blood:
                    name: blood
                    displayName: '&4Blood Altar'
                    lore:
                      - '&7A dark altar'
                      - '&7for blood rituals'
                    material: ENCHANTING_TABLE
                    modelId: 1001
                    targetItemName: blood_essence
                    targetItemAmount: 1
                    ingredients:
                      REDSTONE: 16
                      GHAST_TEAR: 1
                """;

        java.nio.file.Files.writeString(target.toPath(), yaml);

        AltarConfigClass loaded = new AltarConfigClass();
        loaded.altars.clear();
        loaded.load(target);

        assertNotNull(loaded.altars);
        assertEquals(1, loaded.altars.size());

        AltarDefinition blood = loaded.altars.get("blood");
        assertNotNull(blood);
        assertEquals("blood", blood.name());
        assertEquals(TestMaterial.ENCHANTING_TABLE, blood.material());
        assertEquals(16, blood.ingredients().get(TestMaterial.REDSTONE));
    }

    @Test
    void testSingleAltarDefinition() throws IOException {
        // Test a simple record field (not in a map)
        class SimpleAltarConfig extends YamlFileInterface {
            @YamlKey("altar")
            public AltarDefinition altar = new AltarDefinition(
                    "test",
                    "Test Altar",
                    List.of("Test lore"),
                    TestMaterial.DIAMOND,
                    999,
                    "test_item",
                    3,
                    Map.of(TestMaterial.GOLD_INGOT, 5)
            );
        }

        SimpleAltarConfig config = new SimpleAltarConfig();
        config.save(target);

        SimpleAltarConfig loaded = new SimpleAltarConfig();
        loaded.altar = null;
        loaded.load(target);

        assertNotNull(loaded.altar);
        assertEquals("test", loaded.altar.name());
        assertEquals(3, loaded.altar.targetItemAmount());
        assertEquals(TestMaterial.DIAMOND, loaded.altar.material());
    }
}
