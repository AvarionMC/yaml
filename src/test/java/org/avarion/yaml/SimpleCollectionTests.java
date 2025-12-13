package org.avarion.yaml;

import org.avarion.yaml.testClasses.SimpleCollectionClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused tests for simple collections (lists and sets of primitives)
 */
class SimpleCollectionTests extends TestCommon {

    @Test
    void testStringList() throws IOException {
        SimpleCollectionClass config = new SimpleCollectionClass();
        config.save(target);

        SimpleCollectionClass loaded = new SimpleCollectionClass().load(target);

        assertNotNull(loaded.stringList);
        assertEquals(3, loaded.stringList.size());
        assertEquals(List.of("apple", "banana", "cherry"), loaded.stringList);
    }

    @Test
    void testIntegerList() throws IOException {
        SimpleCollectionClass config = new SimpleCollectionClass();
        config.save(target);

        SimpleCollectionClass loaded = new SimpleCollectionClass().load(target);

        assertNotNull(loaded.integerList);
        assertEquals(3, loaded.integerList.size());
        assertEquals(List.of(10, 20, 30), loaded.integerList);
    }

    @Test
    void testStringSet() throws IOException {
        SimpleCollectionClass config = new SimpleCollectionClass();
        config.save(target);

        SimpleCollectionClass loaded = new SimpleCollectionClass().load(target);

        assertNotNull(loaded.stringSet);
        assertEquals(3, loaded.stringSet.size());
        assertTrue(loaded.stringSet.contains("apple"));
        assertTrue(loaded.stringSet.contains("monkey"));
        assertTrue(loaded.stringSet.contains("zebra"));
    }

    @Test
    void testIntegerSet() throws IOException {
        SimpleCollectionClass config = new SimpleCollectionClass();
        config.save(target);

        SimpleCollectionClass loaded = new SimpleCollectionClass().load(target);

        assertNotNull(loaded.integerSet);
        assertEquals(3, loaded.integerSet.size());
        assertTrue(loaded.integerSet.contains(7));
        assertTrue(loaded.integerSet.contains(42));
        assertTrue(loaded.integerSet.contains(99));
    }
}
