package org.avarion.yaml;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapEdgeCasesTest extends TestCommon {

    // Test class with non-generic Map (raw type)
    public static class RawMapClass extends YamlFileInterface {
        @YamlKey("raw-map")
        @SuppressWarnings("rawtypes")
        public Map rawMap = new HashMap();

        public RawMapClass() {
            rawMap.put("key1", "value1");
            rawMap.put("key2", 42);
        }
    }

    // Test class with map containing null values
    public static class NullValueMapClass extends YamlFileInterface {
        @YamlKey("map-with-nulls")
        public Map<String, String> mapWithNulls = new LinkedHashMap<>();

        public NullValueMapClass() {
            mapWithNulls.put("key1", "value1");
            mapWithNulls.put("key2", null);
            mapWithNulls.put("key3", "value3");
        }
    }

    // Test class with map containing null in nested map
    public static class NestedNullValueClass extends YamlFileInterface {
        @YamlKey("nested-map")
        public Map<String, Map<String, String>> nestedMap = new LinkedHashMap<>();

        public NestedNullValueClass() {
            Map<String, String> inner = new LinkedHashMap<>();
            inner.put("a", "valueA");
            inner.put("b", null);
            nestedMap.put("outer", inner);
        }
    }

    @Test
    void testRawMapType() throws IOException {
        // Test the fallback path in handleMapValue when there's no generic type info
        RawMapClass config = new RawMapClass();
        config.save(target);

        // Load it back - without generic type info, values remain as-is from YAML
        RawMapClass loaded = new RawMapClass().load(target);

        assertNotNull(loaded.rawMap);
        assertEquals(2, loaded.rawMap.size());
        assertTrue(loaded.rawMap.containsKey("key1"));
        assertTrue(loaded.rawMap.containsKey("key2"));
    }

    @Test
    void testMapWithNullValues() throws IOException {
        // Test that maps can contain null values
        NullValueMapClass config = new NullValueMapClass();
        config.save(target);

        // Load it back
        NullValueMapClass loaded = new NullValueMapClass().load(target);

        assertNotNull(loaded.mapWithNulls);
        assertEquals(3, loaded.mapWithNulls.size());
        assertEquals("value1", loaded.mapWithNulls.get("key1"));
        assertNull(loaded.mapWithNulls.get("key2"));
        assertEquals("value3", loaded.mapWithNulls.get("key3"));
    }

    @Test
    void testNestedMapWithNullValue() throws IOException {
        // Test that nested maps can contain null values
        NestedNullValueClass config = new NestedNullValueClass();
        config.save(target);

        // Load it back
        NestedNullValueClass loaded = new NestedNullValueClass().load(target);

        assertNotNull(loaded.nestedMap);
        assertTrue(loaded.nestedMap.containsKey("outer"));

        Map<String, String> inner = loaded.nestedMap.get("outer");
        assertNotNull(inner);
        assertEquals("valueA", inner.get("a"));
        assertNull(inner.get("b"));
    }

    @Test
    void testMapWithNullKey() throws IOException {
        // Create a YAML file with a null key manually
        // YAML represents null as either 'null' or '~'
        String yamlContent = "map-with-nulls:\n  key1: value1\n  ~: nullKeyValue\n  key3: value3\n";

        java.nio.file.Files.write(target.toPath(), yamlContent.getBytes());

        // Load it
        NullValueMapClass loaded = new NullValueMapClass().load(target);

        assertNotNull(loaded.mapWithNulls);
        assertTrue(loaded.mapWithNulls.containsKey(null));
        assertEquals("nullKeyValue", loaded.mapWithNulls.get(null));
    }

    @Test
    void testEmptyMap() throws IOException {
        // Test class with empty map
        class EmptyMapClass extends YamlFileInterface {
            @YamlKey("empty")
            public Map<String, String> empty = new LinkedHashMap<>();
        }

        EmptyMapClass config = new EmptyMapClass();
        config.save(target);

        EmptyMapClass loaded = new EmptyMapClass().load(target);

        assertNotNull(loaded.empty);
        assertTrue(loaded.empty.isEmpty());
    }

    @Test
    void testMapWithIntegerKeys() throws IOException {
        // Test map with non-String keys
        class IntKeyMapClass extends YamlFileInterface {
            @YamlKey("int-keys")
            public Map<Integer, String> intKeys = new LinkedHashMap<>();

            public IntKeyMapClass() {
                intKeys.put(1, "one");
                intKeys.put(2, "two");
                intKeys.put(null, "null-key");
            }
        }

        IntKeyMapClass config = new IntKeyMapClass();
        config.save(target);

        IntKeyMapClass loaded = new IntKeyMapClass().load(target);

        assertNotNull(loaded.intKeys);
        assertEquals(3, loaded.intKeys.size());
        assertEquals("one", loaded.intKeys.get(1));
        assertEquals("two", loaded.intKeys.get(2));
        assertEquals("null-key", loaded.intKeys.get(null));
    }
}
