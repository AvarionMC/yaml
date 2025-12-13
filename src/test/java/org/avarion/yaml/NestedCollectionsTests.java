package org.avarion.yaml;

import org.avarion.yaml.testClasses.NestedCollectionsClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused tests for nested collections (collections of collections)
 */
class NestedCollectionsTests extends TestCommon {

    @Test
    void testListOfLists() throws IOException {
        NestedCollectionsClass config = new NestedCollectionsClass();
        config.save(target);

        NestedCollectionsClass loaded = new NestedCollectionsClass().load(target);

        assertNotNull(loaded.listOfLists);
        assertEquals(2, loaded.listOfLists.size());

        List<String> sublist1 = loaded.listOfLists.get(0);
        assertNotNull(sublist1);
        assertEquals(3, sublist1.size());
        assertEquals(List.of("a", "b", "c"), sublist1);

        List<String> sublist2 = loaded.listOfLists.get(1);
        assertNotNull(sublist2);
        assertEquals(2, sublist2.size());
        assertEquals(List.of("d", "e"), sublist2);
    }

    @Test
    void testListOfSets() throws IOException {
        NestedCollectionsClass config = new NestedCollectionsClass();
        config.save(target);

        NestedCollectionsClass loaded = new NestedCollectionsClass().load(target);

        assertNotNull(loaded.listOfSets);
        assertEquals(2, loaded.listOfSets.size());

        Set<String> set1 = loaded.listOfSets.get(0);
        assertNotNull(set1);
        assertEquals(2, set1.size());
        assertTrue(set1.contains("alpha"));
        assertTrue(set1.contains("beta"));

        Set<String> set2 = loaded.listOfSets.get(1);
        assertNotNull(set2);
        assertEquals(3, set2.size());
        assertTrue(set2.contains("gamma"));
        assertTrue(set2.contains("delta"));
        assertTrue(set2.contains("epsilon"));
    }
}
