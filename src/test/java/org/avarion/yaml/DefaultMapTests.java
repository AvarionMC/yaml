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
}
