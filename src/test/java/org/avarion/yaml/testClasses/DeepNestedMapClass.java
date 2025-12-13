package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test class for deeply nested maps (3+ levels)
 */
public class DeepNestedMapClass extends YamlFileInterface {
    // Map<String, Map<String, Map<String, Object>>>
    @YamlKey("deep-nested")
    public Map<String, Map<String, Map<String, Object>>> deepNestedMap = new LinkedHashMap<>();

    public DeepNestedMapClass() {
        // 3-level deep nested map
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
    }
}
