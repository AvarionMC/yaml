package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlMap;

import java.util.HashMap;
import java.util.Map;

public class EmptyMapConfig extends YamlFileInterface {
    @YamlMap(value = "emptyMap", processor = EmptyMapProcessor.class)
    public Map<String, Object> emptyMap = new HashMap<>();

    public static class EmptyMapProcessor implements YamlMap.YamlMapProcessor<EmptyMapConfig> {
        @Override
        public void read(EmptyMapConfig obj, String key, Map<String, Object> value) {
            // Do nothing, keep the map empty
        }

        @Override
        public Map<String, Object> write(EmptyMapConfig obj, String key, Object value) {
            return Map.of();
        }
    }
}
