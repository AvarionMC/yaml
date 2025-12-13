package org.avarion.yaml;

import org.avarion.yaml.testClasses.CollectionOfMapsClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused tests for collections containing maps
 */
class CollectionOfMapsTests extends TestCommon {

    @Test
    void testListOfMaps() throws IOException {
        CollectionOfMapsClass config = new CollectionOfMapsClass();
        config.save(target);

        CollectionOfMapsClass loaded = new CollectionOfMapsClass().load(target);

        assertNotNull(loaded.listOfMaps);
        assertEquals(2, loaded.listOfMaps.size());

        Map<String, Object> map1 = loaded.listOfMaps.get(0);
        assertNotNull(map1);
        assertEquals(1, map1.get("id"));
        assertEquals("first", map1.get("name"));

        Map<String, Object> map2 = loaded.listOfMaps.get(1);
        assertNotNull(map2);
        assertEquals(2, map2.get("id"));
        assertEquals("second", map2.get("name"));
    }

    @Test
    void testSetOfMaps() throws IOException {
        CollectionOfMapsClass config = new CollectionOfMapsClass();
        config.save(target);

        CollectionOfMapsClass loaded = new CollectionOfMapsClass().load(target);

        assertNotNull(loaded.setOfMaps);
        assertEquals(2, loaded.setOfMaps.size());

        // Sets don't guarantee order, so we need to check both maps exist
        boolean foundTypeA = false;
        boolean foundTypeB = false;

        for (Map<String, String> map : loaded.setOfMaps) {
            if ("A".equals(map.get("type"))) {
                assertEquals("cat1", map.get("category"));
                foundTypeA = true;
            } else if ("B".equals(map.get("type"))) {
                assertEquals("cat2", map.get("category"));
                foundTypeB = true;
            }
        }

        assertTrue(foundTypeA, "Should find map with type A");
        assertTrue(foundTypeB, "Should find map with type B");
    }
}
