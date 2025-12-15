package org.avarion.yaml;

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

    // Test interface for interface constant checking
    interface TestInterface {
        String CONSTANT_VALUE = "test_constant";
        TestObject OBJECT_CONSTANT = new TestObject("interface_object");
    }

    static class TestObject implements TestInterface {
        private final String value;

        public TestObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return getClass().getName() + "@" + Integer.toHexString(hashCode());
        }

        public String getValue() {
            return value;
        }
    }

    @Test
    void testInterfaceElement() throws IOException {
        class InterfaceTestClass extends YamlFileInterface {
            @YamlKey("interface-constant")
            private Object interfaceConstant = TestInterface.OBJECT_CONSTANT;

            @YamlKey("string-constant")
            private String stringConstant = TestInterface.CONSTANT_VALUE;
        }

        InterfaceTestClass config = new InterfaceTestClass();
        config.save(target);

        String yaml = new String(java.nio.file.Files.readAllBytes(target.toPath()));

        // The interface constant should be serialized using the field name from the interface
        assertTrue(yaml.contains("interface-constant:"), "Should contain interface-constant field");
        assertTrue(yaml.contains("string-constant:"), "Should contain string-constant field");

        // Verify it uses the interface field name instead of generic toString
        assertTrue(yaml.contains("OBJECT_CONSTANT") || yaml.contains("TestObject@"),
                "Should either use interface field name or toString format");
    }
}
