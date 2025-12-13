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

    // Test for Map with List values: Map<String, List<Integer>>
    @YamlKey("map-with-list-values")
    public Map<String, List<Integer>> mapWithListValues = new LinkedHashMap<>();

    // Test for List of Lists: List<List<String>>
    @YamlKey("list-of-lists")
    public List<List<String>> listOfLists = new ArrayList<>();

    // Test for Map with Set values: Map<String, Set<String>>
    @YamlKey("map-with-set-values")
    public Map<String, Set<String>> mapWithSetValues = new LinkedHashMap<>();

    // Test for List containing Sets: List<Set<String>>
    @YamlKey("list-of-sets")
    public List<Set<String>> listOfSets = new ArrayList<>();

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

        // For map with list values
        List<Integer> scores1 = new ArrayList<>();
        scores1.add(10);
        scores1.add(20);
        scores1.add(30);

        List<Integer> scores2 = new ArrayList<>();
        scores2.add(40);
        scores2.add(50);

        mapWithListValues.put("team1", scores1);
        mapWithListValues.put("team2", scores2);

        // For list of lists
        List<String> sublist1 = new ArrayList<>();
        sublist1.add("a");
        sublist1.add("b");
        sublist1.add("c");

        List<String> sublist2 = new ArrayList<>();
        sublist2.add("d");
        sublist2.add("e");

        listOfLists.add(sublist1);
        listOfLists.add(sublist2);

        // For map with set values
        Set<String> tags1 = new LinkedHashSet<>();
        tags1.add("tag1");
        tags1.add("tag2");

        Set<String> tags2 = new LinkedHashSet<>();
        tags2.add("tag3");
        tags2.add("tag4");

        mapWithSetValues.put("group1", tags1);
        mapWithSetValues.put("group2", tags2);

        // For list of sets
        Set<String> set1 = new LinkedHashSet<>();
        set1.add("alpha");
        set1.add("beta");

        Set<String> set2 = new LinkedHashSet<>();
        set2.add("gamma");
        set2.add("delta");
        set2.add("epsilon");

        listOfSets.add(set1);
        listOfSets.add(set2);
    }
}
