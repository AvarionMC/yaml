package org.avarion.yaml;

import org.avarion.yaml.testClasses.Material;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LeniencyTest extends TestCommon {

    @Test
    void testNoYamlFileAnnotationDefaultsToLenient() throws IOException {
        // No @YamlFile annotation at all → should default to LENIENT
        class TestClass extends YamlFileInterface {
            @YamlKey("char")
            public char chr = 'a';

            @YamlKey("float")
            public float flt = 1f;
        }

        new TestClass().save(target);
        replaceInTarget(": a", ": bcd");
        replaceInTarget(": 1.0", ": 0.51");

        TestClass loaded = new TestClass().load(target);
        assertEquals('b', loaded.chr);   // lenient: takes first char
        assertEquals(0.51, loaded.flt, 0.00001);  // lenient: allows precision loss
    }

    @Test
    void testNoYamlFileAnnotationCanBeOverriddenToStrict() throws IOException {
        // No @YamlFile, but field-level STRICT overrides the lenient default
        class TestClass extends YamlFileInterface {
            @YamlKey(value = "float", lenient = Leniency.STRICT)
            public float flt = 1f;
        }

        new TestClass().save(target);
        replaceInTarget(": 1.0", ": 0.51");

        IOException thrown = assertThrows(IOException.class, () -> {
            new TestClass().load(target);
        });
        assertEquals("Double value 0.51 cannot be precisely represented as a float", thrown.getMessage());
    }

    @Test
    void testFloatNotDoubleLenient() throws IOException {
        @YamlFile(lenient = Leniency.LENIENT)
        class TestClass extends YamlFileInterface {
            @YamlKey("char")
            public char chr = 'a';

            @YamlKey("float")
            public float flt = 1f;
        }

        new TestClass().save(target);

        replaceInTarget(": a", ": bcd");
        replaceInTarget(": 1.0", ": 0.51");

        TestClass loaded = new TestClass().load(target);
        assertEquals('b', loaded.chr);
        assertEquals(0.51, loaded.flt, 0.00001);
    }

    @Test
    void testCharStrict() throws IOException {
        @YamlFile(lenient = Leniency.STRICT)
        class TestClass extends YamlFileInterface {
            @YamlKey("char")
            public char chr = 'a';
        }

        new TestClass().save(target);
        replaceInTarget(": a", ": bcd");

        IOException thrown = assertThrows(IOException.class, () -> {
            new TestClass().load(target);
        });
        assertTrue(thrown.getMessage().contains("Cannot convert String of length 3 to Character"));
    }

    @Test
    void testCharStrictOverruled() throws IOException {
        @YamlFile(lenient = Leniency.STRICT)
        class TestClass extends YamlFileInterface {
            @YamlKey(value = "char", lenient = Leniency.LENIENT)
            public char chr = 'a';
        }

        new TestClass().save(target);
        replaceInTarget(": a", ": bcd");

        TestClass loaded = new TestClass().load(target);
        assertEquals('b', loaded.chr);
    }

    @Test
    void testFloatStrict() throws IOException {
        @YamlFile(lenient = Leniency.STRICT)
        class TestClass extends YamlFileInterface {
            @YamlKey("float")
            public float flt = 1f;
        }

        new TestClass().save(target);
        replaceInTarget(": 1.0", ": 0.51");

        IOException thrown = assertThrows(IOException.class, () -> {
            new TestClass().load(target);
        });
        assertEquals("Double value 0.51 cannot be precisely represented as a float", thrown.getMessage());
    }

    @Test
    void testFloatStrictOverruled() throws IOException {
        @YamlFile(lenient = Leniency.STRICT)
        class TestClass extends YamlFileInterface {
            @YamlKey(value = "float", lenient = Leniency.LENIENT)
            public float flt = 1f;
        }

        new TestClass().save(target);
        replaceInTarget(": 1.0", ": 0.51");

        TestClass loaded = new TestClass().load(target);
        assertEquals(0.51, loaded.flt, 0.00001);
    }

    @Test
    void testDoubleStrictOverruled() throws IOException {
        @YamlFile(lenient = Leniency.STRICT)
        class TestClass extends YamlFileInterface {
            @YamlKey(value = "float", lenient = Leniency.LENIENT)
            public float flt = 1f;
        }

        new TestClass().save(target);
        replaceInTarget(": 1.0", ": 0.51");

        TestClass loaded = new TestClass().load(target);
        assertEquals(0.51, loaded.flt, 0.00001);
    }

    @Test
    void testDoubleStrictOverruled2() throws IOException {
        @YamlFile(lenient = Leniency.LENIENT)
        class TestClass extends YamlFileInterface {
            @YamlKey(value = "float", lenient = Leniency.STRICT)
            public float flt = 1f;
        }

        new TestClass().save(target);
        replaceInTarget(": 1.0", ": 0.51");

        IOException thrown = assertThrows(IOException.class, () -> {
            new TestClass().load(target);
        });
        assertEquals("Double value 0.51 cannot be precisely represented as a float", thrown.getMessage());
    }

    @Test
    void testListAcceptString() throws IOException {
        @YamlFile(lenient = Leniency.LENIENT)
        class TestClass extends YamlFileInterface {
            @YamlKey("achievements")
            public List<String> achievements = null;
        }

        new TestClass().save(target);
        replaceInTarget(": null", ": \"Entry\"");

        TestClass loaded = new TestClass().load(target);
        assertEquals(List.of("Entry"), loaded.achievements);
    }

    @Test
    void testListDoesNotAcceptString() throws IOException {
        @YamlFile(lenient = Leniency.STRICT)
        class TestClass extends YamlFileInterface {
            @YamlKey("achievements")
            public List<String> achievements = null;
        }

        new TestClass().save(target);
        replaceInTarget(": null", ": \"Entry\"");

        IOException thrown = assertThrows(
                IOException.class, () -> {
                    new TestClass().load(target);
                }
        );
        assertEquals("'List': I cannot figure out how to retrieve this type.", thrown.getMessage());
    }

    // ==================== Lenient enum-skipping in collections/maps (issue #124) ====================

    @Test
    void testLenientSkipsInvalidEnumInList() throws IOException {
        @YamlFile(lenient = Leniency.LENIENT)
        class TestClass extends YamlFileInterface {
            @YamlKey("mats")
            public List<Material> mats = List.of();
        }

        writeYaml("mats:\n  - A\n  - NOT_A_MATERIAL\n  - B\n");

        TestClass loaded = new TestClass().load(target);
        assertEquals(List.of(Material.A, Material.B), loaded.mats);
    }

    @Test
    void testLenientSkipsInvalidEnumValueInMap() throws IOException {
        @YamlFile(lenient = Leniency.LENIENT)
        class TestClass extends YamlFileInterface {
            @YamlKey("mats")
            public Map<String, Material> mats = new LinkedHashMap<>();
        }

        writeYaml("mats:\n  first: A\n  bad: NOT_A_MATERIAL\n  third: B\n");

        TestClass loaded = new TestClass().load(target);
        assertEquals(Map.of("first", Material.A, "third", Material.B), loaded.mats);
    }

    @Test
    void testLenientStillThrowsOnNonEnumConversionFailureInList() throws IOException {
        // Guard rail: lenient mode skips ONLY bad enum entries — other conversion errors must still surface.
        @YamlFile(lenient = Leniency.LENIENT)
        class TestClass extends YamlFileInterface {
            @YamlKey("ids")
            public List<java.util.UUID> ids = List.of();
        }

        writeYaml("ids:\n  - 00000000-0000-0000-0000-000000000001\n  - not-a-uuid\n");

        IOException thrown = assertThrows(IOException.class, () -> new TestClass().load(target));
        assertInstanceOf(IllegalArgumentException.class, thrown.getCause());
    }

    @Test
    void testLenientSkipsInvalidEnumKeyInMap() throws IOException {
        // Covers handleMapValue's key-side LENIENT_ENUM_SKIP branch (left of `||`).
        @YamlFile(lenient = Leniency.LENIENT)
        class TestClass extends YamlFileInterface {
            @YamlKey("counts")
            public Map<Material, Integer> counts = new LinkedHashMap<>();
        }

        writeYaml("counts:\n  A: 1\n  NOT_A_MATERIAL: 99\n  C: 3\n");

        TestClass loaded = new TestClass().load(target);
        assertEquals(Map.of(Material.A, 1, Material.C, 3), loaded.counts);
    }

    @Test
    void testLenientSkipsInvalidEnumInNestedList() throws IOException {
        // Covers convertWithType's inline Collection branch (skip on the inner list iteration).
        @YamlFile(lenient = Leniency.LENIENT)
        class TestClass extends YamlFileInterface {
            @YamlKey("groups")
            public List<List<Material>> groups = List.of();
        }

        writeYaml("groups:\n  - - A\n    - NOT_A_MATERIAL\n    - B\n  - - C\n");

        TestClass loaded = new TestClass().load(target);
        assertEquals(List.of(List.of(Material.A, Material.B), List.of(Material.C)), loaded.groups);
    }

    @Test
    void testLenientSkipsInvalidEnumInNestedMap() throws IOException {
        // Covers convertWithType's inline Map branch (skip on the inner map iteration).
        @YamlFile(lenient = Leniency.LENIENT)
        class TestClass extends YamlFileInterface {
            @YamlKey("groups")
            public Map<String, Map<String, Material>> groups = new LinkedHashMap<>();
        }

        writeYaml("groups:\n  g1:\n    a: A\n    bad: NOT_A_MATERIAL\n    b: B\n");

        TestClass loaded = new TestClass().load(target);
        assertEquals(Map.of("g1", Map.of("a", Material.A, "b", Material.B)), loaded.groups);
    }

    @Test
    void testLenientSkipsInvalidEnumKeyInNestedMap() throws IOException {
        // Covers convertWithType's inline Map branch on the KEY side (left of `||`).
        @YamlFile(lenient = Leniency.LENIENT)
        class TestClass extends YamlFileInterface {
            @YamlKey("groups")
            public Map<String, Map<Material, Integer>> groups = new LinkedHashMap<>();
        }

        writeYaml("groups:\n  g1:\n    A: 1\n    NOT_A_MATERIAL: 99\n    C: 3\n");

        TestClass loaded = new TestClass().load(target);
        assertEquals(Map.of("g1", Map.of(Material.A, 1, Material.C, 3)), loaded.groups);
    }

    @Test
    void testLenientLeavesTopLevelEnumAtDefaultOnInvalidValue() throws IOException {
        // Covers YamlFileInterface.readYamlKeyField's LENIENT_ENUM_SKIP early return.
        @YamlFile(lenient = Leniency.LENIENT)
        class TestClass extends YamlFileInterface {
            @YamlKey("mat")
            public Material mat = Material.A;
        }

        writeYaml("mat: NOT_A_MATERIAL\n");

        TestClass loaded = new TestClass().load(target);
        assertEquals(Material.A, loaded.mat);
    }

    public record EnumRecord(Material mat, String label) {}

    @Test
    void testLenientSubstitutesNullForInvalidEnumInRecord() throws IOException {
        // Covers convertMapToRecord's LENIENT_ENUM_SKIP → null substitution: a record
        // component cannot be skipped, so the lenient signal becomes null instead.
        @YamlFile(lenient = Leniency.LENIENT)
        class TestClass extends YamlFileInterface {
            @YamlKey("rec")
            public EnumRecord rec = new EnumRecord(Material.A, "default");
        }

        writeYaml("rec:\n  mat: NOT_A_MATERIAL\n  label: hello\n");

        TestClass loaded = new TestClass().load(target);
        assertNull(loaded.rec.mat());
        assertEquals("hello", loaded.rec.label());
    }
}
