package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test class for simple nested maps (2-level deep)
 */
public class SimpleMapClass extends YamlFileInterface {
    // Map<String, Map<String, Object>>
    @YamlKey("nested")
    public Map<String, Map<String, Object>> nestedMap = new LinkedHashMap<>();

    // Map<String, Map<String, String>>
    @YamlKey("nested-strings")
    public Map<String, Map<String, String>> nestedStringMap = new LinkedHashMap<>();

    // Map<String, Map<String, Integer>>
    @YamlKey("nested-integers")
    public Map<String, Map<String, Integer>> nestedIntegerMap = new LinkedHashMap<>();

    public SimpleMapClass() {
        // Mixed types
        Map<String, Object> innerMap1 = new HashMap<>();
        innerMap1.put("key1", "value1");
        innerMap1.put("key2", 42);
        innerMap1.put("key3", true);

        Map<String, Object> innerMap2 = new HashMap<>();
        innerMap2.put("foo", "bar");
        innerMap2.put("count", 100);

        nestedMap.put("outer1", innerMap1);
        nestedMap.put("outer2", innerMap2);

        // String values only
        Map<String, String> stringInner1 = new HashMap<>();
        stringInner1.put("name", "John");
        stringInner1.put("city", "NYC");

        Map<String, String> stringInner2 = new HashMap<>();
        stringInner2.put("name", "Jane");
        stringInner2.put("city", "LA");

        nestedStringMap.put("person1", stringInner1);
        nestedStringMap.put("person2", stringInner2);

        // Integer values only
        Map<String, Integer> intInner1 = new HashMap<>();
        intInner1.put("score", 95);
        intInner1.put("level", 10);

        Map<String, Integer> intInner2 = new HashMap<>();
        intInner2.put("score", 87);
        intInner2.put("level", 8);

        nestedIntegerMap.put("player1", intInner1);
        nestedIntegerMap.put("player2", intInner2);
    }
}
