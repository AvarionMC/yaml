package org.avarion.yaml;

import java.util.Map;

public interface YamlSerializable {

    /**
     * @return a map representing constructor inputs
     */
    Map<String, Object> toYamlMap();

    /**
     * Reconstruct an instance from yaml data
     */
    static YamlSerializable fromYamlMap(Map<String, Object> map) {
        throw new UnsupportedOperationException("Implement in concrete class");
    }
}
