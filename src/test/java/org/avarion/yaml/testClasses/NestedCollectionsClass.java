package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.*;

/**
 * Test class for nested collections (List<List>, List<Set>, etc.)
 */
public class NestedCollectionsClass extends YamlFileInterface {
    // List<List<String>>
    @YamlKey("list-of-lists")
    public List<List<String>> listOfLists = new ArrayList<>();

    // List<Set<String>>
    @YamlKey("list-of-sets")
    public List<Set<String>> listOfSets = new ArrayList<>();

    public NestedCollectionsClass() {
        // List of lists
        List<String> sublist1 = new ArrayList<>();
        sublist1.add("a");
        sublist1.add("b");
        sublist1.add("c");

        List<String> sublist2 = new ArrayList<>();
        sublist2.add("d");
        sublist2.add("e");

        listOfLists.add(sublist1);
        listOfLists.add(sublist2);

        // List of sets
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
