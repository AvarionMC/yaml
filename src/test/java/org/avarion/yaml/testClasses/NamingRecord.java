package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlKey;

/**
 * A record covering the naming edge cases: plain camelCase, a trailing digit, an embedded
 * acronym, a single word, and a component that names its own key explicitly in camelCase.
 */
public record NamingRecord(
        String modelId,

        String modelId2,

        String httpURL,

        String name,

        @YamlKey("someCamelKey") String explicit) {
}
