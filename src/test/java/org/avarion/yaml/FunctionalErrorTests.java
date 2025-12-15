package org.avarion.yaml;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
    }
}
