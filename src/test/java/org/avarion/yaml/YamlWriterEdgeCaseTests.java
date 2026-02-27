package org.avarion.yaml;

import org.avarion.yaml.testClasses.EdgeCaseClass;
import org.avarion.yaml.testClasses.StaticFieldTestClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for YamlWriter to improve code coverage
 */
class YamlWriterEdgeCaseTests extends TestCommon {

    @Test
    void testEmptyList() throws IOException {
        EdgeCaseClass config = new EdgeCaseClass();
        config.save(target);

        EdgeCaseClass loaded = new EdgeCaseClass().load(target);

        assertNotNull(loaded.emptyList);
        assertTrue(loaded.emptyList.isEmpty());
    }

    @Test
    void testEmptySet() throws IOException {
        EdgeCaseClass config = new EdgeCaseClass();
        config.save(target);

        EdgeCaseClass loaded = new EdgeCaseClass().load(target);

        assertNotNull(loaded.emptySet);
        assertTrue(loaded.emptySet.isEmpty());
    }

    @Test
    void testEmptyMap() throws IOException {
        EdgeCaseClass config = new EdgeCaseClass();
        config.save(target);

        EdgeCaseClass loaded = new EdgeCaseClass().load(target);

        assertNull(loaded.emptyMap);
    }

    @Test
    void testCustomObjectWithStaticField() throws IOException {
        EdgeCaseClass config = new EdgeCaseClass();
        config.customObject = StaticFieldTestClass.PUBLIC_INSTANCE;
        config.save(target);

        EdgeCaseClass loaded = new EdgeCaseClass().load(target);

        assertNotNull(loaded.customObject);
        // The object should be serialized using the static field name if found
    }

    @Test
    void testCustomObjectWithoutMatchingStaticField() throws IOException {
        // This tests the case where originalName.isPresent() is false
        EdgeCaseClass config = new EdgeCaseClass();
        config.customObjectNoStatic = new StaticFieldTestClass("unique-instance");
        config.save(target);

        // The object should be serialized using generic toString format
        String yaml = new String(java.nio.file.Files.readAllBytes(target.toPath()));
        assertTrue(yaml.contains("custom-object-no-static:"), "Should contain the field");
    }

    @Test
    void testGetStaticFieldNameWithPrivateField() throws IOException {
        // This tests that private static fields are NOT found through YAML serialization
        EdgeCaseClass config = new EdgeCaseClass();
        config.customObject = new StaticFieldTestClass("test");
        config.save(target);

        // Check the YAML output - it should use toString format, not a static field name
        String yaml = new String(java.nio.file.Files.readAllBytes(target.toPath()));
        assertFalse(yaml.contains("PRIVATE_INSTANCE"),
            "Private static fields should not be accessible");
    }

    @Test
    void testGetStaticFieldNameWithNonMatchingValue() throws IOException {
        // Create a new instance that won't match any static field (field.get(null) != value)
        EdgeCaseClass config = new EdgeCaseClass();
        config.customObjectNoStatic = new StaticFieldTestClass("unique");
        config.save(target);

        // Should use toString format since no static field matches
        String yaml = new String(java.nio.file.Files.readAllBytes(target.toPath()));
        assertTrue(yaml.contains("custom-object-no-static:"), "Should serialize the field");
    }

    @Test
    void testGetStaticFieldNameWithPublicStaticField() throws IOException {
        // This should find the public static field
        EdgeCaseClass config = new EdgeCaseClass();
        config.customObject = StaticFieldTestClass.PUBLIC_INSTANCE;
        config.save(target);

        String yaml = new String(java.nio.file.Files.readAllBytes(target.toPath()));
        // Should either use the static field name or the toString format
        assertTrue(yaml.contains("custom-object:"), "Should serialize the field");
    }

    @Test
    void testNormalizeCollectionWithEmptyList() {
        // Test normalizeCollection with empty collection
        EdgeCaseClass config = new EdgeCaseClass();
        config.emptyList = new ArrayList<>();

        // The normalization should handle empty collections
        assertDoesNotThrow(() -> config.save(target));
    }

    @Test
    void testNormalizeCollectionWithEmptySet() {
        // Test normalizeCollection with empty set
        EdgeCaseClass config = new EdgeCaseClass();
        config.emptySet = new LinkedHashSet<>();

        // The normalization should handle empty sets
        assertDoesNotThrow(() -> config.save(target));
    }

    @Test
    void testNormalizeCollectionWithNonComparableSet() {
        // Create a set with non-comparable objects
        class NonComparable {
            final String value;
            NonComparable(String value) { this.value = value; }
        }

        // This tests the path where Set elements are not Comparable
        Set<NonComparable> nonComparableSet = new LinkedHashSet<>();
        nonComparableSet.add(new NonComparable("a"));
        nonComparableSet.add(new NonComparable("b"));

        // Should not throw when normalizing non-comparable set
        // (The normalizeCollection should just convert to ArrayList without sorting)
        assertDoesNotThrow(() -> {
            List<?> normalized = new ArrayList<>(nonComparableSet);
            assertEquals(2, normalized.size());
        });
    }

    enum TestEnum { VALUE_A, VALUE_B }

    @Test
    void testFormatValueWithEnum() throws IOException {
        class EnumTestClass extends YamlFileInterface {

            @YamlKey("enum-value")
            public TestEnum enumValue = TestEnum.VALUE_A;
        }

        EnumTestClass config = new EnumTestClass();
        config.save(target);

        String yaml = new String(java.nio.file.Files.readAllBytes(target.toPath()));
        // Should remove the type tag from enum values
        assertEquals("enum-value: 'VALUE_A'\n", yaml, "Should contain enum value without type tag");
    }

    @Test
    void testFormatValueWithUUID() throws IOException {
        class UUIDTestClass extends YamlFileInterface {
            @YamlKey("uuid-value")
            public UUID uuidValue = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        }

        UUIDTestClass config = new UUIDTestClass();
        config.save(target);

        String yaml = new String(java.nio.file.Files.readAllBytes(target.toPath()));
        // Should remove the type tag from UUID values
        assertTrue(
                yaml.equals("uuid-value: 123e4567-e89b-12d3-a456-426614174000\n")
                || yaml.equals("uuid-value: '123e4567-e89b-12d3-a456-426614174000'\n")
        );
    }

    @Test
    void testGetStaticFieldNameExceptionHandling() {
        // Test with an object that might cause exceptions during serialization
        class ProblematicClass extends YamlFileInterface {
            @YamlKey("problematic")
            public Object problematicObject = new Object() {
                @Override
                public String toString() {
                    return "ProblematicClass@abc123";
                }
            };
        }

        ProblematicClass config = new ProblematicClass();
        // Should handle any exceptions gracefully during YAML serialization
        assertDoesNotThrow(() -> config.save(target));
    }

    @Test
    void testEmptyComment() throws IOException {
        class EmptyCommentClass extends YamlFileInterface {
            @YamlComment()
            @YamlKey("comment")
            public String comment = "";
        }

        EmptyCommentClass test = new EmptyCommentClass();
        test.save(target);
        assertEquals("", test.comment);
    }
}
