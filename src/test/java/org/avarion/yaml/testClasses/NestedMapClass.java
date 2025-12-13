package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.*;

public class NestedMapClass extends YamlFileInterface {
    // Test for 2-level deep map: Map<String, Map<String, Object>>
    @YamlKey("nested")
    public Map<String, Map<String, Object>> nestedMap = new LinkedHashMap<>();

    // Test for 2-level deep map with String values: Map<String, Map<String, String>>
    @YamlKey("nested-strings")
    public Map<String, Map<String, String>> nestedStringMap = new LinkedHashMap<>();

    // Test for 2-level deep map with Integer values: Map<String, Map<String, Integer>>
    @YamlKey("nested-integers")
    public Map<String, Map<String, Integer>> nestedIntegerMap = new LinkedHashMap<>();

    // Test for 3-level deep map: Map<String, Map<String, Map<String, Object>>>
    @YamlKey("deep-nested")
    public Map<String, Map<String, Map<String, Object>>> deepNestedMap = new LinkedHashMap<>();

    // Test for List containing maps: List<Map<String, Object>>
    @YamlKey("list-of-maps")
    public List<Map<String, Object>> listOfMaps = new ArrayList<>();

    // Test for Set containing maps: Set<Map<String, String>>
    @YamlKey("set-of-maps")
    public Set<Map<String, String>> setOfMaps = new LinkedHashSet<>();

    public NestedMapClass() {
        // Initialize with some default values
        Map<String, Object> innerMap1 = new HashMap<>();
        innerMap1.put("key1", "value1");
        innerMap1.put("key2", 42);
        innerMap1.put("key3", true);

        Map<String, Object> innerMap2 = new HashMap<>();
        innerMap2.put("foo", "bar");
        innerMap2.put("count", 100);

        nestedMap.put("outer1", innerMap1);
        nestedMap.put("outer2", innerMap2);

        // For nested string map
        Map<String, String> stringInner1 = new HashMap<>();
        stringInner1.put("name", "John");
        stringInner1.put("city", "NYC");

        Map<String, String> stringInner2 = new HashMap<>();
        stringInner2.put("name", "Jane");
        stringInner2.put("city", "LA");

        nestedStringMap.put("person1", stringInner1);
        nestedStringMap.put("person2", stringInner2);

        // For nested integer map
        Map<String, Integer> intInner1 = new HashMap<>();
        intInner1.put("score", 95);
        intInner1.put("level", 10);

        Map<String, Integer> intInner2 = new HashMap<>();
        intInner2.put("score", 87);
        intInner2.put("level", 8);

        nestedIntegerMap.put("player1", intInner1);
        nestedIntegerMap.put("player2", intInner2);

        // For 3-level deep nested map
        Map<String, Object> deepestLevel1 = new HashMap<>();
        deepestLevel1.put("value", "deep1");
        deepestLevel1.put("number", 123);

        Map<String, Object> deepestLevel2 = new HashMap<>();
        deepestLevel2.put("value", "deep2");
        deepestLevel2.put("flag", true);

        Map<String, Map<String, Object>> middleLevel1 = new HashMap<>();
        middleLevel1.put("deep1", deepestLevel1);
        middleLevel1.put("deep2", deepestLevel2);

        Map<String, Object> deepestLevel3 = new HashMap<>();
        deepestLevel3.put("value", "deep3");

        Map<String, Map<String, Object>> middleLevel2 = new HashMap<>();
        middleLevel2.put("deep3", deepestLevel3);

        deepNestedMap.put("level1-a", middleLevel1);
        deepNestedMap.put("level1-b", middleLevel2);

        // For list of maps
        Map<String, Object> listMap1 = new HashMap<>();
        listMap1.put("id", 1);
        listMap1.put("name", "first");

        Map<String, Object> listMap2 = new HashMap<>();
        listMap2.put("id", 2);
        listMap2.put("name", "second");

        listOfMaps.add(listMap1);
        listOfMaps.add(listMap2);

        // For set of maps
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
