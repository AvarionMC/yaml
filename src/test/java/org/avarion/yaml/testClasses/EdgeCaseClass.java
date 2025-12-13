package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.*;

/**
 * Test class for edge cases like empty collections
 */
public class EdgeCaseClass extends YamlFileInterface {
    @YamlKey("empty-list")
    public List<String> emptyList = new ArrayList<>();

    @YamlKey("empty-set")
    public Set<String> emptySet = new LinkedHashSet<>();

    @YamlKey("empty-map")
    public Map<String, String> emptyMap = new LinkedHashMap<>();

    @YamlKey("custom-object")
    public StaticFieldTestClass customObject = StaticFieldTestClass.PUBLIC_INSTANCE;

    @YamlKey("custom-object-no-static")
    public StaticFieldTestClass customObjectNoStatic = new StaticFieldTestClass("no-static-match");

    public EdgeCaseClass() {
        // All collections are empty by default
    }
}
