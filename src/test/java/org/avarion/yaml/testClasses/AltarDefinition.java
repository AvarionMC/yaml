package org.avarion.yaml.testClasses;

import java.util.List;
import java.util.Map;

/**
 * A record with a secondary constructor (compact constructor pattern).
 * Tests that records with custom constructors work correctly.
 */
public record AltarDefinition(
        String name,
        String displayName,
        List<String> lore,
        TestMaterial material,
        int modelId,
        String targetItemName,
        int targetItemAmount,
        Map<TestMaterial, Integer> ingredients) {

    /**
     * Secondary constructor that parses targetItem string into name and amount.
     * Format: "item_name" or "item_name:amount"
     */
    public AltarDefinition(String name, String displayName, List<String> lore, TestMaterial material,
                           int modelId, String targetItem, Map<TestMaterial, Integer> ingredients) {
        this(name, displayName, lore, material, modelId,
                targetItem.contains(":") ? targetItem.split(":")[0] : targetItem,
                targetItem.contains(":") ? Integer.parseInt(targetItem.split(":")[1]) : 1,
                ingredients);
    }
}
