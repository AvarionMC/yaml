package org.avarion.yaml;

import org.avarion.yaml.testClasses.MapClasses;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class DefaultMapTests extends TestCommon {
    @Test
    void testByteMap() throws IOException {
        new MapClasses().save(target);
        replaceInTarget(": a", ": b");
        MapClasses loaded = new MapClasses().load(target);

        assertEquals(1, loaded.tmpByte.size());

        Map.Entry<Byte, String> entry = loaded.tmpByte.entrySet().iterator().next();
        assertInstanceOf(Byte.class, entry.getKey());
        assertInstanceOf(String.class, entry.getValue());

        assertEquals((byte)1, entry.getKey());
        assertEquals("b", entry.getValue());
    }

    @Test
    void testCharacterMap() throws IOException {
        new MapClasses().save(target);
        replaceInTarget(": b", ": modified");
        MapClasses loaded = new MapClasses().load(target);

        assertEquals(1, loaded.tmpChar.size());

        Map.Entry<Character, String> entry = loaded.tmpChar.entrySet().iterator().next();
        assertInstanceOf(Character.class, entry.getKey());
        assertInstanceOf(String.class, entry.getValue());

        assertEquals('x', entry.getKey());
        assertEquals("modified", entry.getValue());
    }

    @Test
    void testShortMap() throws IOException {
        new MapClasses().save(target);
        replaceInTarget(": c", ": updated");
        MapClasses loaded = new MapClasses().load(target);

        assertEquals(1, loaded.tmpShort.size());

        Map.Entry<Short, String> entry = loaded.tmpShort.entrySet().iterator().next();
        assertInstanceOf(Short.class, entry.getKey());
        assertInstanceOf(String.class, entry.getValue());

        assertEquals((short)2, entry.getKey());
        assertEquals("updated", entry.getValue());
    }

    @Test
    void testIntegerMap() throws IOException {
        new MapClasses().save(target);
        replaceInTarget(": d", ": changed");
        MapClasses loaded = new MapClasses().load(target);

        assertEquals(1, loaded.tmpInt.size());

        Map.Entry<Integer, String> entry = loaded.tmpInt.entrySet().iterator().next();
        assertInstanceOf(Integer.class, entry.getKey());
        assertInstanceOf(String.class, entry.getValue());

        assertEquals(3, entry.getKey());
        assertEquals("changed", entry.getValue());
    }

    @Test
    void testLongMap() throws IOException {
        new MapClasses().save(target);
        replaceInTarget(": e", ": new_value");
        MapClasses loaded = new MapClasses().load(target);

        assertEquals(1, loaded.tmpLng.size());

        Map.Entry<Long, String> entry = loaded.tmpLng.entrySet().iterator().next();
        assertInstanceOf(Long.class, entry.getKey());
        assertInstanceOf(String.class, entry.getValue());

        assertEquals(4L, entry.getKey());
        assertEquals("new_value", entry.getValue());
    }

    @Test
    void testFloatMap() throws IOException {
        new MapClasses().save(target);
        replaceInTarget(": f", ": float_modified");
        MapClasses loaded = new MapClasses().load(target);

        assertEquals(1, loaded.tmpFlt.size());

        Map.Entry<Float, String> entry = loaded.tmpFlt.entrySet().iterator().next();
        assertInstanceOf(Float.class, entry.getKey());
        assertInstanceOf(String.class, entry.getValue());

        assertEquals(5f, entry.getKey());
        assertEquals("float_modified", entry.getValue());
    }

    @Test
    void testDoubleMap() throws IOException {
        new MapClasses().save(target);
        replaceInTarget(": g", ": double_modified");
        MapClasses loaded = new MapClasses().load(target);

        assertEquals(1, loaded.tmpDbl.size());

        Map.Entry<Double, String> entry = loaded.tmpDbl.entrySet().iterator().next();
        assertInstanceOf(Double.class, entry.getKey());
        assertInstanceOf(String.class, entry.getValue());

        assertEquals(6d, entry.getKey());
        assertEquals("double_modified", entry.getValue());
    }

    @Test
    void testBooleanMap() throws IOException {
        new MapClasses().save(target);
        replaceInTarget(": h", ": bool_modified");
        MapClasses loaded = new MapClasses().load(target);

        assertEquals(1, loaded.tmpBool.size());

        Map.Entry<Boolean, String> entry = loaded.tmpBool.entrySet().iterator().next();
        assertInstanceOf(Boolean.class, entry.getKey());
        assertInstanceOf(String.class, entry.getValue());

        assertEquals(true, entry.getKey());
        assertEquals("bool_modified", entry.getValue());
    }
}
