package org.avarion.yaml;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class LeniencyTest extends TestCommon {

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

}
