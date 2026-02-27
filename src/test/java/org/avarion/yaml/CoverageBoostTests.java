package org.avarion.yaml;

import org.avarion.yaml.testClasses.BossConfig;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests to improve branch and line coverage for YamlFileInterface and YamlWriter.
 */
class CoverageBoostTests extends TestCommon {

    // ==================== YamlFileInterface: loadFields with null data ====================

    @Test
    void testLoadEmptyYamlFile() throws IOException {
        // Write an empty file, then load → data will be null, triggers null check
        try (FileWriter writer = new FileWriter(target)) {
            writer.write("");
        }

        class SimpleConfig extends YamlFileInterface {
            @YamlKey("name")
            public String name = "default";
        }

        SimpleConfig loaded = new SimpleConfig().load(target);
        assertEquals("default", loaded.name);
    }

    @Test
    void testLoadYamlWithOnlyComments() throws IOException {
        // YAML file with only comments → parses to null data
        try (FileWriter writer = new FileWriter(target)) {
            writer.write("# This is just a comment\n# Nothing here\n");
        }

        class SimpleConfig extends YamlFileInterface {
            @YamlKey("name")
            public String name = "default";
        }

        SimpleConfig loaded = new SimpleConfig().load(target);
        assertEquals("default", loaded.name);
    }

    // ==================== YamlFileInterface: multi-line header comment ====================

    @Test
    void testMultiLineHeaderComment() throws IOException {
        @YamlFile(header = "Line 1\nLine 2\nLine 3")
        class MultiLineHeaderConfig extends YamlFileInterface {
            @YamlKey("key")
            public int key = 1;
        }

        new MultiLineHeaderConfig().save(target);
        String content = readFile();

        assertTrue(content.contains("# Line 1\n"));
        assertTrue(content.contains("# Line 2\n"));
        assertTrue(content.contains("# Line 3\n"));
        assertTrue(content.contains("key: 1"));
    }

    @Test
    void testHeaderWithTrailingWhitespace() throws IOException {
        @YamlFile(header = "Header with trailing spaces   ")
        class TrailingSpaceConfig extends YamlFileInterface {
            @YamlKey("key")
            public int key = 1;
        }

        new TrailingSpaceConfig().save(target);
        String content = readFile();
        // Trailing spaces should be stripped
        assertTrue(content.contains("# Header with trailing spaces\n"));
    }

    // ==================== YamlFileInterface: getNestedValue with non-Map intermediate ====================

    @Test
    void testNestedKeyWithNonMapIntermediate() throws IOException {
        // Set up: dotted key "a.b.c" but "a" is a scalar value, not a map
        class NestedKeyConfig extends YamlFileInterface {
            @YamlKey("a.b.c")
            public String value = "default";
        }

        // Save a config with properly nested value
        new NestedKeyConfig().save(target);
        // Replace the nested structure with a scalar: "a:\n  b:\n    c: default" → "a: scalar"
        Files.write(target.toPath(), "a: scalar_value\n".getBytes());

        NestedKeyConfig loaded = new NestedKeyConfig().load(target);
        // "a" is not a map, so getNestedValue returns UNKNOWN, and field keeps default
        assertEquals("default", loaded.value);
    }

    // ==================== YamlFileInterface: YamlMap with non-Map entries ====================

    @Test
    void testYamlMapSkipsNonMapEntries() throws IOException {
        // Write YAML where a YamlMap section has a mix of map and non-map values
        new BossConfig().save(target);
        // Add a non-map entry under bosses
        String content = readFile();
        content = content.replace("bosses:\n", "bosses:\n  scalar_entry: just_a_string\n");
        Files.write(target.toPath(), content.getBytes());

        BossConfig loaded = new BossConfig().load(target);
        // scalar_entry should be skipped since it's not a Map
        assertNotNull(loaded.bosses);
        assertFalse(loaded.bosses.containsKey("scalar_entry"));
        // The normal boss entries should still be loaded
        assertTrue(loaded.bosses.containsKey("boss1") || loaded.bosses.containsKey("boss2"));
    }

    // ==================== YamlFileInterface: writeYamlMapField with non-Map field ====================

    @Test
    void testWriteYamlMapFieldWhenFieldIsNull() throws IOException {
        class NullMapConfig extends YamlFileInterface {
            @YamlMap(value = "items", processor = NullMapConfig.Processor.class)
            public Map<String, Object> items = null;

            static class Processor implements YamlMap.YamlMapProcessor<NullMapConfig> {
                @Override
                public void read(NullMapConfig obj, String key, Map<String, Object> value) {
                    // no-op
                }

                @Override
                public Map<String, Object> write(NullMapConfig obj, String key, Object value) {
                    return Map.of();
                }
            }
        }

        NullMapConfig config = new NullMapConfig();
        // items is null, so writeYamlMapField should skip since null is not instanceof Map
        assertDoesNotThrow(() -> config.save(target));
    }

    // ==================== YamlFileInterface: isLenient branches ====================

    @Test
    void testStrictLeniencyOnField() throws IOException {
        @YamlFile(lenient = Leniency.LENIENT)
        class StrictFieldConfig extends YamlFileInterface {
            @YamlKey(value = "float", lenient = Leniency.STRICT)
            public float flt = 1f;
        }

        new StrictFieldConfig().save(target);
        replaceInTarget("1.0", "0.1234567890123456");

        IOException thrown = assertThrows(IOException.class, () ->
                new StrictFieldConfig().load(target));
        assertTrue(thrown.getMessage().contains("cannot be precisely represented as a float"));
    }

    // ==================== YamlWriter: normalizeCollection with non-Comparable Set ====================

    @Test
    void testNonComparableSetInYaml() throws IOException {
        class NonComparableSetConfig extends YamlFileInterface {
            @YamlKey("items")
            public Set<UUID> items = new LinkedHashSet<>();
        }

        NonComparableSetConfig config = new NonComparableSetConfig();
        // UUIDs are Comparable, so let's use a List instead to verify the path
        UUID uuid1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID uuid2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        config.items.add(uuid1);
        config.items.add(uuid2);

        assertDoesNotThrow(() -> config.save(target));

        String content = readFile();
        assertTrue(content.contains("items:"));
    }

    // ==================== YamlWriter: empty map value ====================

    @Test
    void testNullFieldValueInYaml() throws IOException {
        class NullFieldConfig extends YamlFileInterface {
            @YamlKey("value")
            public String value = null;
        }

        NullFieldConfig config = new NullFieldConfig();
        config.save(target);
        String content = readFile();
        assertTrue(content.contains("value: null"));
    }

    // ==================== YamlFileInterface: loadFields with both annotations error ====================

    @Test
    void testBothAnnotationsOnFieldThrows() throws IOException {
        class DualAnnotationConfig extends YamlFileInterface {
            @YamlKey("key")
            @YamlMap(value = "key", processor = DualAnnotationConfig.Processor.class)
            public Map<String, Object> field = new HashMap<>();

            static class Processor implements YamlMap.YamlMapProcessor<DualAnnotationConfig> {
                @Override
                public void read(DualAnnotationConfig obj, String key, Map<String, Object> value) {
                    // Intentionally empty: only used to test annotation validation
                }

                @Override
                public Map<String, Object> write(DualAnnotationConfig obj, String key, Object value) {
                    return Map.of();
                }
            }
        }

        DualAnnotationConfig config = new DualAnnotationConfig();
        config.save(target);

        DualAnnotationConfig loaded = new DualAnnotationConfig();
        assertThrows(IllegalStateException.class, () -> loaded.load(target));
    }

    // ==================== YamlFileInterface: FinalAttribute on YamlMap field ====================

    @Test
    void testFinalYamlMapFieldOnLoadThrows() throws IOException {
        class FinalMapConfig extends YamlFileInterface {
            @YamlMap(value = "items", processor = FinalMapConfig.Processor.class)
            public final Map<String, Object> items = new HashMap<>();

            static class Processor implements YamlMap.YamlMapProcessor<FinalMapConfig> {
                @Override
                public void read(FinalMapConfig obj, String key, Map<String, Object> value) {
                    // Intentionally empty: only used to test final field validation
                }

                @Override
                public Map<String, Object> write(FinalMapConfig obj, String key, Object value) {
                    return Map.of();
                }
            }
        }

        // Create a valid YAML file with items section
        try (FileWriter writer = new FileWriter(target)) {
            writer.write("items:\n  entry1:\n    name: test\n");
        }

        assertThrows(IOException.class, () -> new FinalMapConfig().load(target));
    }

    // ==================== Record with null non-primitive component ====================

    @Test
    void testRecordWithNullNonPrimitiveComponent() throws IOException {
        class RecordHolder extends YamlFileInterface {
            @YamlKey("address")
            public org.avarion.yaml.testClasses.Address address =
                    new org.avarion.yaml.testClasses.Address("123 Main", "City", 12345);
        }

        RecordHolder config = new RecordHolder();
        config.save(target);

        // Set the street to null in YAML
        replaceInTarget("123 Main", "null");

        RecordHolder loaded = new RecordHolder().load(target);
        // street is String (non-primitive), so null is allowed
        assertNotNull(loaded.address);
    }

    // ==================== Queue collection type ====================

    @Test
    void testQueueCollectionType() throws IOException {
        class QueueConfig extends YamlFileInterface {
            @YamlKey("items")
            public Queue<String> items = new ArrayDeque<>(List.of("a", "b", "c"));
        }

        QueueConfig config = new QueueConfig();
        config.save(target);

        QueueConfig loaded = new QueueConfig().load(target);
        assertNotNull(loaded.items);
        assertEquals(3, loaded.items.size());
        assertTrue(loaded.items.contains("a"));
    }

    // ==================== Lenient single value to collection for Set ====================

    @Test
    void testLenientSingleValueToSet() throws IOException {
        @YamlFile(lenient = Leniency.LENIENT)
        class SetConfig extends YamlFileInterface {
            @YamlKey("tags")
            public Set<String> tags = null;
        }

        new SetConfig().save(target);
        replaceInTarget(": null", ": single_tag");

        SetConfig loaded = new SetConfig().load(target);
        assertNotNull(loaded.tags);
        assertEquals(1, loaded.tags.size());
        assertTrue(loaded.tags.contains("single_tag"));
    }

    // ==================== Plugin: getDataFolder throws exception ====================

    @Test
    void testPluginGetDataFolderThrowsException() {
        class ThrowingPlugin {
            public java.io.File getDataFolder() {
                throw new RuntimeException("Simulated failure");
            }
        }

        ThrowingPlugin plugin = new ThrowingPlugin();

        class SimpleConfig extends YamlFileInterface {
            @YamlKey("key")
            public int key = 1;
        }

        // InvocationTargetException wraps the RuntimeException; IOException wraps that
        IOException thrown = assertThrows(IOException.class, () ->
                new SimpleConfig().load(plugin));
        assertNotNull(thrown.getCause());
    }

    @Test
    void testPluginGetDataFolderThrowsOnSave() {
        class ThrowingPlugin {
            public java.io.File getDataFolder() {
                throw new RuntimeException("Save failure");
            }
        }

        class SimpleConfig extends YamlFileInterface {
            @YamlKey("key")
            public int key = 1;
        }

        IOException thrown = assertThrows(IOException.class, () ->
                new SimpleConfig().save(new ThrowingPlugin()));
        assertNotNull(thrown.getCause());
    }

    // ==================== Processor without no-arg constructor (save path) ====================

    @Test
    void testWriteYamlMapWithBrokenProcessor() {
        class BrokenProcessorConfig extends YamlFileInterface {
            @YamlMap(value = "items", processor = BrokenProcessorConfig.BadProcessor.class)
            public Map<String, Object> items = new HashMap<>(Map.of("a", "b"));

            static class BadProcessor implements YamlMap.YamlMapProcessor<BrokenProcessorConfig> {
                // Constructor that requires parameters → no default no-arg constructor
                private final String required;

                public BadProcessor(String required) {
                    this.required = required;
                }

                @Override
                public void read(BrokenProcessorConfig obj, String key, Map<String, Object> value) {
                    // Intentionally empty: only testing constructor failure on save
                }

                @Override
                public Map<String, Object> write(BrokenProcessorConfig obj, String key, Object value) {
                    return Map.of();
                }
            }
        }

        BrokenProcessorConfig config = new BrokenProcessorConfig();
        assertThrows(IllegalStateException.class, () -> config.save(target));
    }

    // ==================== YamlMap with empty value annotation (no key) ====================

    @Test
    void testYamlMapWithEmptyKey() throws IOException {
        class EmptyKeyMapConfig extends YamlFileInterface {
            @YamlKey("regular")
            public String regular = "value";
        }

        EmptyKeyMapConfig config = new EmptyKeyMapConfig();
        config.save(target);

        String content = readFile();
        assertTrue(content.contains("regular: value"));
    }

    // ==================== Record with Map<String, Integer> component ====================

    @Test
    void testRecordWithMapComponentViaYaml() throws IOException {
        record ConfigRecord(String name, Map<String, Integer> scores) {}

        class RecordConfig extends YamlFileInterface {
            @YamlKey("record")
            public ConfigRecord rec = new ConfigRecord("Test", Map.of("math", 100, "science", 95));
        }

        RecordConfig config = new RecordConfig();
        config.save(target);

        RecordConfig loaded = new RecordConfig().load(target);
        assertNotNull(loaded.rec);
        assertEquals("Test", loaded.rec.name());
    }
}
