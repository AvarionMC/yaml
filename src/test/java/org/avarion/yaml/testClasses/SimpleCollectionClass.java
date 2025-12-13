package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.*;

/**
 * Test class for simple collections (lists and sets of primitives)
 */
public class SimpleCollectionClass extends YamlFileInterface {
    @YamlKey("string-list")
    public List<String> stringList = new ArrayList<>();

    @YamlKey("integer-list")
    public List<Integer> integerList = new ArrayList<>();

    @YamlKey("string-set")
    public Set<String> stringSet = new LinkedHashSet<>();

    @YamlKey("integer-set")
    public Set<Integer> integerSet = new LinkedHashSet<>();

    public SimpleCollectionClass() {
        // String list
        stringList.add("apple");
        stringList.add("banana");
        stringList.add("cherry");

        // Integer list
        integerList.add(10);
        integerList.add(20);
        integerList.add(30);

        // String set (will be sorted in YAML)
        stringSet.add("zebra");
        stringSet.add("apple");
        stringSet.add("monkey");

        // Integer set (will be sorted in YAML)
        integerSet.add(99);
        integerSet.add(42);
        integerSet.add(7);
    }
}
