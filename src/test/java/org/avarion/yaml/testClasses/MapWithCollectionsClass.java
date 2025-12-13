package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.*;

/**
 * Test class for maps with collection values (Map<K, List> or Map<K, Set>)
 */
public class MapWithCollectionsClass extends YamlFileInterface {
    // Map<String, List<Integer>>
    @YamlKey("map-with-list-values")
    public Map<String, List<Integer>> mapWithListValues = new LinkedHashMap<>();

    // Map<String, Set<String>>
    @YamlKey("map-with-set-values")
    public Map<String, Set<String>> mapWithSetValues = new LinkedHashMap<>();

    public MapWithCollectionsClass() {
        // Map with list values
        List<Integer> scores1 = new ArrayList<>();
        scores1.add(10);
        scores1.add(20);
        scores1.add(30);

        List<Integer> scores2 = new ArrayList<>();
        scores2.add(40);
        scores2.add(50);

        mapWithListValues.put("team1", scores1);
        mapWithListValues.put("team2", scores2);

        // Map with set values
        Set<String> tags1 = new LinkedHashSet<>();
        tags1.add("tag1");
        tags1.add("tag2");

        Set<String> tags2 = new LinkedHashSet<>();
        tags2.add("tag3");
        tags2.add("tag4");

        mapWithSetValues.put("group1", tags1);
        mapWithSetValues.put("group2", tags2);
    }
}
