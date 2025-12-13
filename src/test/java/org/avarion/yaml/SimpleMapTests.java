package org.avarion.yaml;

import org.avarion.yaml.testClasses.SimpleMapClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused tests for simple nested maps (2-level deep)
 */
class SimpleMapTests extends TestCommon {

    @Test
    void testNestedMapWithMixedObjects() throws IOException {
        SimpleMapClass config = new SimpleMapClass();
        config.save(target);

        SimpleMapClass loaded = new SimpleMapClass().load(target);

        assertNotNull(loaded.nestedMap);
        assertEquals(2, loaded.nestedMap.size());

        // Check outer1
        Map<String, Object> outer1 = loaded.nestedMap.get("outer1");
        assertNotNull(outer1);
        assertEquals(3, outer1.size());
        assertEquals("value1", outer1.get("key1"));
        assertEquals(42, outer1.get("key2"));
        assertEquals(true, outer1.get("key3"));

        // Check outer2
        Map<String, Object> outer2 = loaded.nestedMap.get("outer2");
        assertNotNull(outer2);
        assertEquals(2, outer2.size());
        assertEquals("bar", outer2.get("foo"));
        assertEquals(100, outer2.get("count"));
    }

    @Test
    void testNestedMapWithStrings() throws IOException {
        SimpleMapClass config = new SimpleMapClass();
        config.save(target);

        SimpleMapClass loaded = new SimpleMapClass().load(target);

        assertNotNull(loaded.nestedStringMap);
        assertEquals(2, loaded.nestedStringMap.size());

        // Check person1
        Map<String, String> person1 = loaded.nestedStringMap.get("person1");
        assertNotNull(person1);
        assertEquals(2, person1.size());
        assertEquals("John", person1.get("name"));
        assertEquals("NYC", person1.get("city"));

        // Check person2
        Map<String, String> person2 = loaded.nestedStringMap.get("person2");
        assertNotNull(person2);
        assertEquals(2, person2.size());
        assertEquals("Jane", person2.get("name"));
        assertEquals("LA", person2.get("city"));
    }

    @Test
    void testNestedMapWithIntegers() throws IOException {
        SimpleMapClass config = new SimpleMapClass();
        config.save(target);

        SimpleMapClass loaded = new SimpleMapClass().load(target);

        assertNotNull(loaded.nestedIntegerMap);
        assertEquals(2, loaded.nestedIntegerMap.size());

        // Check player1
        Map<String, Integer> player1 = loaded.nestedIntegerMap.get("player1");
        assertNotNull(player1);
        assertEquals(2, player1.size());
        assertEquals(95, player1.get("score"));
        assertEquals(10, player1.get("level"));

        // Check player2
        Map<String, Integer> player2 = loaded.nestedIntegerMap.get("player2");
        assertNotNull(player2);
        assertEquals(2, player2.size());
        assertEquals(87, player2.get("score"));
        assertEquals(8, player2.get("level"));
    }
}
