package org.avarion.yaml;

import org.avarion.yaml.testClasses.NestedMapClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NestedMapTests extends TestCommon {

    @Test
    void testNestedMapWithMixedObjects() throws IOException {
        // Create and save a config with nested maps
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the nested map structure is preserved
        assertNotNull(loaded.nestedMap);
        assertEquals(2, loaded.nestedMap.size());

        // Check outer1
        assertTrue(loaded.nestedMap.containsKey("outer1"));
        Map<String, Object> outer1 = loaded.nestedMap.get("outer1");
        assertNotNull(outer1);
        assertEquals(3, outer1.size());
        assertEquals("value1", outer1.get("key1"));
        assertEquals(42, outer1.get("key2"));
        assertEquals(true, outer1.get("key3"));

        // Check outer2
        assertTrue(loaded.nestedMap.containsKey("outer2"));
        Map<String, Object> outer2 = loaded.nestedMap.get("outer2");
        assertNotNull(outer2);
        assertEquals(2, outer2.size());
        assertEquals("bar", outer2.get("foo"));
        assertEquals(100, outer2.get("count"));
    }

    @Test
    void testNestedMapWithStrings() throws IOException {
        // Create and save a config with nested string maps
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the nested string map structure
        assertNotNull(loaded.nestedStringMap);
        assertEquals(2, loaded.nestedStringMap.size());

        // Check person1
        assertTrue(loaded.nestedStringMap.containsKey("person1"));
        Map<String, String> person1 = loaded.nestedStringMap.get("person1");
        assertNotNull(person1);
        assertEquals(2, person1.size());
        assertEquals("John", person1.get("name"));
        assertEquals("NYC", person1.get("city"));

        // Check person2
        assertTrue(loaded.nestedStringMap.containsKey("person2"));
        Map<String, String> person2 = loaded.nestedStringMap.get("person2");
        assertNotNull(person2);
        assertEquals(2, person2.size());
        assertEquals("Jane", person2.get("name"));
        assertEquals("LA", person2.get("city"));
    }

    @Test
    void testNestedMapWithIntegers() throws IOException {
        // Create and save a config with nested integer maps
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the nested integer map structure
        assertNotNull(loaded.nestedIntegerMap);
        assertEquals(2, loaded.nestedIntegerMap.size());

        // Check player1
        assertTrue(loaded.nestedIntegerMap.containsKey("player1"));
        Map<String, Integer> player1 = loaded.nestedIntegerMap.get("player1");
        assertNotNull(player1);
        assertEquals(2, player1.size());
        assertEquals(95, player1.get("score"));
        assertEquals(10, player1.get("level"));

        // Check player2
        assertTrue(loaded.nestedIntegerMap.containsKey("player2"));
        Map<String, Integer> player2 = loaded.nestedIntegerMap.get("player2");
        assertNotNull(player2);
        assertEquals(2, player2.size());
        assertEquals(87, player2.get("score"));
        assertEquals(8, player2.get("level"));
    }

    @Test
    void testNestedMapModification() throws IOException {
        // Create and save a config
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Modify the YAML file to change nested values
        replaceInTarget("value1", "modified_value");
        replaceInTarget("42", "999");

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the modifications were loaded correctly
        Map<String, Object> outer1 = loaded.nestedMap.get("outer1");
        assertNotNull(outer1);
        assertEquals("modified_value", outer1.get("key1"));
        assertEquals(999, outer1.get("key2"));
    }

    @Test
    void testNestedMapAddNewEntry() throws IOException {
        // Create and save a config
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Add a new nested map entry to the YAML file
        String additionalContent = """
                  outer3:
                    newKey: newValue
                    anotherKey: 123
                """;

        // Read current content
        java.nio.file.Path filePath = target.toPath();
        String content = new String(java.nio.file.Files.readAllBytes(filePath));

        // Find the position after "nested:" and add the new entry
        content = content.replace("nested:", "nested:" + System.lineSeparator() + additionalContent);

        // Write back
        java.nio.file.Files.write(filePath, content.getBytes(),
            java.nio.file.StandardOpenOption.WRITE,
            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the new entry was loaded
        assertTrue(loaded.nestedMap.containsKey("outer3"));
        Map<String, Object> outer3 = loaded.nestedMap.get("outer3");
        assertNotNull(outer3);
        assertEquals("newValue", outer3.get("newKey"));
        assertEquals(123, outer3.get("anotherKey"));
    }

    @Test
    void testEmptyNestedMap() throws IOException {
        // Create a config with empty nested maps
        NestedMapClass config = new NestedMapClass();
        config.nestedMap.clear();
        config.nestedStringMap.clear();
        config.nestedIntegerMap.clear();

        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify empty maps are preserved
        assertNotNull(loaded.nestedMap);
        assertTrue(loaded.nestedMap.isEmpty());
        assertNotNull(loaded.nestedStringMap);
        assertTrue(loaded.nestedStringMap.isEmpty());
        assertNotNull(loaded.nestedIntegerMap);
        assertTrue(loaded.nestedIntegerMap.isEmpty());
    }
}
