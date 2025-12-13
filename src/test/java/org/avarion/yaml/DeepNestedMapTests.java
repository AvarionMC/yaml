package org.avarion.yaml;

import org.avarion.yaml.testClasses.DeepNestedMapClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused tests for deeply nested maps (3+ levels)
 */
class DeepNestedMapTests extends TestCommon {

    @Test
    void testDeepNestedMap() throws IOException {
        DeepNestedMapClass config = new DeepNestedMapClass();
        config.save(target);

        DeepNestedMapClass loaded = new DeepNestedMapClass().load(target);

        assertNotNull(loaded.deepNestedMap);
        assertEquals(2, loaded.deepNestedMap.size());

        // Check level1-a
        Map<String, Map<String, Object>> level1a = loaded.deepNestedMap.get("level1-a");
        assertNotNull(level1a);
        assertEquals(2, level1a.size());

        Map<String, Object> deep1 = level1a.get("deep1");
        assertNotNull(deep1);
        assertEquals("deep1", deep1.get("value"));
        assertEquals(123, deep1.get("number"));

        Map<String, Object> deep2 = level1a.get("deep2");
        assertNotNull(deep2);
        assertEquals("deep2", deep2.get("value"));
        assertEquals(true, deep2.get("flag"));

        // Check level1-b
        Map<String, Map<String, Object>> level1b = loaded.deepNestedMap.get("level1-b");
        assertNotNull(level1b);
        assertEquals(1, level1b.size());

        Map<String, Object> deep3 = level1b.get("deep3");
        assertNotNull(deep3);
        assertEquals("deep3", deep3.get("value"));
    }

    @Test
    void testDeepNestedMapNavigation() throws IOException {
        DeepNestedMapClass config = new DeepNestedMapClass();
        config.save(target);

        DeepNestedMapClass loaded = new DeepNestedMapClass().load(target);

        // Navigate to deeply nested value: deepNestedMap -> level1-a -> deep1 -> number
        Object value = loaded.deepNestedMap
                .get("level1-a")
                .get("deep1")
                .get("number");

        assertEquals(123, value);
    }
}
