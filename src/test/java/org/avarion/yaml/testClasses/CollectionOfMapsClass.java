package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.*;

/**
 * Test class for collections containing maps (List<Map> or Set<Map>)
 */
public class CollectionOfMapsClass extends YamlFileInterface {
    // List<Map<String, Object>>
    @YamlKey("list-of-maps")
    public List<Map<String, Object>> listOfMaps = new ArrayList<>();

    // Set<Map<String, String>>
    @YamlKey("set-of-maps")
    public Set<Map<String, String>> setOfMaps = new LinkedHashSet<>();

    public CollectionOfMapsClass() {
        // List of maps
        Map<String, Object> listMap1 = new HashMap<>();
        listMap1.put("id", 1);
        listMap1.put("name", "first");

        Map<String, Object> listMap2 = new HashMap<>();
        listMap2.put("id", 2);
        listMap2.put("name", "second");

        listOfMaps.add(listMap1);
        listOfMaps.add(listMap2);

        // Set of maps
        Map<String, String> setMap1 = new HashMap<>();
        setMap1.put("type", "A");
        setMap1.put("category", "cat1");

        Map<String, String> setMap2 = new HashMap<>();
        setMap2.put("type", "B");
        setMap2.put("category", "cat2");

        setOfMaps.add(setMap1);
        setOfMaps.add(setMap2);
    }
}
