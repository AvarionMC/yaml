package org.avarion.yaml;

import org.avarion.yaml.testClasses.MapWithCollectionsClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused tests for maps with collection values
 */
class MapWithCollectionsTests extends TestCommon {

    @Test
    void testMapWithListValues() throws IOException {
        MapWithCollectionsClass config = new MapWithCollectionsClass();
        config.save(target);

        MapWithCollectionsClass loaded = new MapWithCollectionsClass().load(target);

        assertNotNull(loaded.mapWithListValues);
        assertEquals(2, loaded.mapWithListValues.size());

        List<Integer> team1 = loaded.mapWithListValues.get("team1");
        assertNotNull(team1);
        assertEquals(3, team1.size());
        assertEquals(List.of(10, 20, 30), team1);

        List<Integer> team2 = loaded.mapWithListValues.get("team2");
        assertNotNull(team2);
        assertEquals(2, team2.size());
        assertEquals(List.of(40, 50), team2);
    }

    @Test
    void testMapWithSetValues() throws IOException {
        MapWithCollectionsClass config = new MapWithCollectionsClass();
        config.save(target);

        MapWithCollectionsClass loaded = new MapWithCollectionsClass().load(target);

        assertNotNull(loaded.mapWithSetValues);
        assertEquals(2, loaded.mapWithSetValues.size());

        Set<String> group1 = loaded.mapWithSetValues.get("group1");
        assertNotNull(group1);
        assertEquals(2, group1.size());
        assertTrue(group1.contains("tag1"));
        assertTrue(group1.contains("tag2"));

        Set<String> group2 = loaded.mapWithSetValues.get("group2");
        assertNotNull(group2);
        assertEquals(2, group2.size());
        assertTrue(group2.contains("tag3"));
        assertTrue(group2.contains("tag4"));
    }
}
