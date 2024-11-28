package org.avarion.yaml;

import org.avarion.yaml.testClasses.Boss;
import org.avarion.yaml.testClasses.BossConfig;
import org.avarion.yaml.testClasses.EmptyMapConfig;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class YamlMapTest extends TestCommon {

    @Test
    void testYamlMapReadWrite() throws IOException {
        BossConfig config = new BossConfig();
        config.bosses.put("newBoss", new Boss("New Boss", "new_internal", "New Arena"));
        config.save(target);

        String content = new String(Files.readAllBytes(target.toPath()));
        assertTrue(content.contains("bosses:"));
        assertTrue(content.contains("newBoss:"));
        assertTrue(content.contains("name: New Boss"));
        assertTrue(content.contains("internal_name: new_internal"));
        assertTrue(content.contains("arena: New Arena"));

        BossConfig loaded = new BossConfig().load(target);
        assertNotNull(loaded.bosses);
        assertEquals(3, loaded.bosses.size());

        Boss newBoss = loaded.bosses.get("newBoss");
        assertNotNull(newBoss);
        assertEquals("New Boss", newBoss.getName());
        assertEquals("new_internal", newBoss.getInternalName());
        assertEquals("New Arena", newBoss.getArena());
    }

    @Test
    void testYamlMapEmptyMap() throws IOException {
        EmptyMapConfig config = new EmptyMapConfig();
        config.save(target);

        EmptyMapConfig loaded = new EmptyMapConfig().load(target);
        assertNotNull(loaded.emptyMap);
        assertTrue(loaded.emptyMap.isEmpty());
    }

    @Test
    void testYamlMapEmptyFile() throws IOException {
        try (FileWriter writer = new FileWriter(target)) {
            writer.write("");
        }
        EmptyMapConfig loaded = new EmptyMapConfig().load(target);
        assertNotNull(loaded.emptyMap);
        assertTrue(loaded.emptyMap.isEmpty());
    }

    @Test
    void testYamlMapInvalidYamlStructure() throws IOException {
        // Corrupt the YAML structure
        Files.write(target.toPath(), "bosses: invalidstructure".getBytes());

        BossConfig config = new BossConfig();
        assertThrows(IllegalStateException.class, () -> config.load(target));
    }

    @Test
    void testYamlMapMissingRequiredField() throws IOException {
        new BossConfig().save(target);

        // Remove a required field
        replaceInTarget("internal_name: internal1", "");

        BossConfig loaded = new BossConfig().load(target);
        assertNull(loaded.bosses.get("boss1").getInternalName());
    }

    @Test
    void testYamlMapAdditionalFields() throws IOException {
        BossConfig config = new BossConfig();
        config.save(target);

        // Add an unexpected field
        replaceInTarget("arena: arena1", "arena: arena3\n    unexpected: value");

        BossConfig loaded = new BossConfig().load(target);
        Boss boss1 = loaded.bosses.get("boss1");
        assertNotNull(boss1);
        assertEquals("arena3", boss1.getArena());
    }
}
