package org.avarion.yaml;

import org.avarion.yaml.testClasses.NestedMapClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NestedMapTests extends TestCommon {

    @Test
    void testNestedMapWithMixedObjects() throws IOException {
        // Create and save a config with nested maps
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the nested map structure is preserved
        assertNotNull(loaded.nestedMap);
        assertEquals(2, loaded.nestedMap.size());

        // Check outer1
        assertTrue(loaded.nestedMap.containsKey("outer1"));
        Map<String, Object> outer1 = loaded.nestedMap.get("outer1");
        assertNotNull(outer1);
        assertEquals(3, outer1.size());
        assertEquals("value1", outer1.get("key1"));
        assertEquals(42, outer1.get("key2"));
        assertEquals(true, outer1.get("key3"));

        // Check outer2
        assertTrue(loaded.nestedMap.containsKey("outer2"));
        Map<String, Object> outer2 = loaded.nestedMap.get("outer2");
        assertNotNull(outer2);
        assertEquals(2, outer2.size());
        assertEquals("bar", outer2.get("foo"));
        assertEquals(100, outer2.get("count"));
    }

    @Test
    void testNestedMapWithStrings() throws IOException {
        // Create and save a config with nested string maps
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the nested string map structure
        assertNotNull(loaded.nestedStringMap);
        assertEquals(2, loaded.nestedStringMap.size());

        // Check person1
        assertTrue(loaded.nestedStringMap.containsKey("person1"));
        Map<String, String> person1 = loaded.nestedStringMap.get("person1");
        assertNotNull(person1);
        assertEquals(2, person1.size());
        assertEquals("John", person1.get("name"));
        assertEquals("NYC", person1.get("city"));

        // Check person2
        assertTrue(loaded.nestedStringMap.containsKey("person2"));
        Map<String, String> person2 = loaded.nestedStringMap.get("person2");
        assertNotNull(person2);
        assertEquals(2, person2.size());
        assertEquals("Jane", person2.get("name"));
        assertEquals("LA", person2.get("city"));
    }

    @Test
    void testNestedMapWithIntegers() throws IOException {
        // Create and save a config with nested integer maps
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the nested integer map structure
        assertNotNull(loaded.nestedIntegerMap);
        assertEquals(2, loaded.nestedIntegerMap.size());

        // Check player1
        assertTrue(loaded.nestedIntegerMap.containsKey("player1"));
        Map<String, Integer> player1 = loaded.nestedIntegerMap.get("player1");
        assertNotNull(player1);
        assertEquals(2, player1.size());
        assertEquals(95, player1.get("score"));
        assertEquals(10, player1.get("level"));

        // Check player2
        assertTrue(loaded.nestedIntegerMap.containsKey("player2"));
        Map<String, Integer> player2 = loaded.nestedIntegerMap.get("player2");
        assertNotNull(player2);
        assertEquals(2, player2.size());
        assertEquals(87, player2.get("score"));
        assertEquals(8, player2.get("level"));
    }

    @Test
    void testNestedMapModification() throws IOException {
        // Create and save a config
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Modify the YAML file to change nested values
        replaceInTarget("value1", "modified_value");
        replaceInTarget("42", "999");

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the modifications were loaded correctly
        Map<String, Object> outer1 = loaded.nestedMap.get("outer1");
        assertNotNull(outer1);
        assertEquals("modified_value", outer1.get("key1"));
        assertEquals(999, outer1.get("key2"));
    }

    @Test
    void testNestedMapAddNewEntry() throws IOException {
        // Create and save a config
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Add a new nested map entry using replaceInTarget
        replaceInTarget("outer1:", "outer3:\n    newKey: newValue\n    anotherKey: 123\n  outer1:");

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the new entry was loaded
        assertTrue(loaded.nestedMap.containsKey("outer3"));
        Map<String, Object> outer3 = loaded.nestedMap.get("outer3");
        assertNotNull(outer3);
        assertEquals("newValue", outer3.get("newKey"));
        assertEquals(123, outer3.get("anotherKey"));
    }

    @Test
    void testDeepNestedMap() throws IOException {
        // Create and save a config with 3-level deep nested map
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the 3-level deep nested map structure
        assertNotNull(loaded.deepNestedMap);
        assertEquals(2, loaded.deepNestedMap.size());

        // Check level1-a -> deep1 -> value
        assertTrue(loaded.deepNestedMap.containsKey("level1-a"));
        Map<String, Map<String, Object>> level1a = loaded.deepNestedMap.get("level1-a");
        assertNotNull(level1a);
        assertEquals(2, level1a.size());

        Map<String, Object> deep1 = level1a.get("deep1");
        assertNotNull(deep1);
        assertEquals("deep1", deep1.get("value"));
        assertEquals(123, deep1.get("number"));

        Map<String, Object> deep2 = level1a.get("deep2");
        assertNotNull(deep2);
        assertEquals("deep2", deep2.get("value"));
        assertEquals(true, deep2.get("flag"));

        // Check level1-b -> deep3 -> value
        assertTrue(loaded.deepNestedMap.containsKey("level1-b"));
        Map<String, Map<String, Object>> level1b = loaded.deepNestedMap.get("level1-b");
        assertNotNull(level1b);
        assertEquals(1, level1b.size());

        Map<String, Object> deep3 = level1b.get("deep3");
        assertNotNull(deep3);
        assertEquals("deep3", deep3.get("value"));
    }

    @Test
    void testListOfMaps() throws IOException {
        // Create and save a config with list of maps
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the list of maps structure
        assertNotNull(loaded.listOfMaps);
        assertEquals(2, loaded.listOfMaps.size());

        // Check first map
        Map<String, Object> map1 = loaded.listOfMaps.get(0);
        assertNotNull(map1);
        assertEquals(1, map1.get("id"));
        assertEquals("first", map1.get("name"));

        // Check second map
        Map<String, Object> map2 = loaded.listOfMaps.get(1);
        assertNotNull(map2);
        assertEquals(2, map2.get("id"));
        assertEquals("second", map2.get("name"));
    }

    @Test
    void testListOfMapsModification() throws IOException {
        // Create and save a config
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Modify a value in the list
        replaceInTarget("first", "modified");

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the modification
        Map<String, Object> map1 = loaded.listOfMaps.get(0);
        assertEquals("modified", map1.get("name"));
    }

    @Test
    void testSetOfMaps() throws IOException {
        // Create and save a config with set of maps
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the set of maps structure
        assertNotNull(loaded.setOfMaps);
        assertEquals(2, loaded.setOfMaps.size());

        // Convert to list for easier testing (order doesn't matter in set)
        java.util.List<Map<String, String>> setAsList = new java.util.ArrayList<>(loaded.setOfMaps);

        // Verify both maps are present with correct data
        boolean foundTypeA = false;
        boolean foundTypeB = false;

        for (Map<String, String> map : setAsList) {
            if ("A".equals(map.get("type"))) {
                foundTypeA = true;
                assertEquals("cat1", map.get("category"));
            } else if ("B".equals(map.get("type"))) {
                foundTypeB = true;
                assertEquals("cat2", map.get("category"));
            }
        }

        assertTrue(foundTypeA, "Set should contain map with type A");
        assertTrue(foundTypeB, "Set should contain map with type B");
    }

    @Test
    void testMapWithListValues() throws IOException {
        // Test Map<String, List<Integer>> - this hits collection handling in convertWithType
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the map with list values
        assertNotNull(loaded.mapWithListValues);
        assertEquals(2, loaded.mapWithListValues.size());

        java.util.List<Integer> team1 = loaded.mapWithListValues.get("team1");
        assertNotNull(team1);
        assertEquals(3, team1.size());
        assertEquals(10, team1.get(0));
        assertEquals(20, team1.get(1));
        assertEquals(30, team1.get(2));

        java.util.List<Integer> team2 = loaded.mapWithListValues.get("team2");
        assertNotNull(team2);
        assertEquals(2, team2.size());
        assertEquals(40, team2.get(0));
        assertEquals(50, team2.get(1));
    }

    @Test
    void testListOfLists() throws IOException {
        // Test List<List<String>> - this hits collection handling in convertWithType
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the list of lists
        assertNotNull(loaded.listOfLists);
        assertEquals(2, loaded.listOfLists.size());

        java.util.List<String> sublist1 = loaded.listOfLists.get(0);
        assertNotNull(sublist1);
        assertEquals(3, sublist1.size());
        assertEquals("a", sublist1.get(0));
        assertEquals("b", sublist1.get(1));
        assertEquals("c", sublist1.get(2));

        java.util.List<String> sublist2 = loaded.listOfLists.get(1);
        assertNotNull(sublist2);
        assertEquals(2, sublist2.size());
        assertEquals("d", sublist2.get(0));
        assertEquals("e", sublist2.get(1));
    }

    @Test
    void testMapWithSetValues() throws IOException {
        // Test Map<String, Set<String>> - this hits collection handling in convertWithType
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the map with set values
        assertNotNull(loaded.mapWithSetValues);
        assertEquals(2, loaded.mapWithSetValues.size());

        Set<String> tags1 = loaded.mapWithSetValues.get("group1");
        assertNotNull(tags1);
        assertEquals(2, tags1.size());
        assertTrue(tags1.contains("tag1"));
        assertTrue(tags1.contains("tag2"));

        Set<String> tags2 = loaded.mapWithSetValues.get("group2");
        assertNotNull(tags2);
        assertEquals(2, tags2.size());
        assertTrue(tags2.contains("tag3"));
        assertTrue(tags2.contains("tag4"));
    }

    @Test
    void testMapWithListValuesModification() throws IOException {
        // Test modification of list values in map
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Modify a list value
        replaceInTarget("10", "999");

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the modification
        java.util.List<Integer> team1 = loaded.mapWithListValues.get("team1");
        assertEquals(999, team1.get(0));
    }

    @Test
    void testListOfSets() throws IOException {
        // Test List<Set<String>> - this covers writeCollectionItemInList with Set items
        NestedMapClass config = new NestedMapClass();
        config.save(target);

        // Load it back
        NestedMapClass loaded = new NestedMapClass().load(target);

        // Verify the list of sets
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
