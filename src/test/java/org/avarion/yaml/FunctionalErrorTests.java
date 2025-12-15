package org.avarion.yaml;

import org.avarion.yaml.testClasses.SoundTestClass;
import org.bukkit.Sound;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FunctionalErrorTests extends TestCommon {
    @Test
    void testWrongCollection() throws IOException {
        class Tmp extends YamlFileInterface {
            @YamlKey("recipes.shapeless")
            private Map<String, Map<String, Object>> tmpAttr = Map.ofEntries(
                    Map.entry("test", Map.of("enabled", true, "input", "NETHERITE_INGOT:1", "output", List.of("a", "b"))));
        }

        Tmp tmp = new Tmp();
        tmp.save(target);

        Tmp loaded = new Tmp().load(target);
        assertInstanceOf(Map.class, loaded.tmpAttr);
        assertTrue(loaded.tmpAttr.containsKey("test"));

        Map<String, Object> el = loaded.tmpAttr.get("test");

        assertInstanceOf(Map.class, el);
        assertTrue(el.containsKey("enabled"));
        assertTrue(el.containsKey("input"));
        assertTrue(el.containsKey("output"));

        assertInstanceOf(Boolean.class, el.get("enabled"));
        assertEquals(true, el.get("enabled"));

        assertInstanceOf(String.class, el.get("input"));
        assertEquals("NETHERITE_INGOT:1", el.get("input"));

        assertInstanceOf(List.class, el.get("output"));
        assertEquals(List.of("a", "b"), el.get("output"));
    }

    @Test
    void testInterfaceElement() throws IOException {
        new SoundTestClass().save(target);

        SoundTestClass loaded = new SoundTestClass().load(target);
        assertEquals(Sound.A, loaded.name);
        assertFalse(readFile().contains("A-test"));

        replaceInTarget("name: A", "name: B");
        SoundTestClass loaded2 = new SoundTestClass().load(target);
        assertEquals(Sound.B, loaded2.name);
    }
}
