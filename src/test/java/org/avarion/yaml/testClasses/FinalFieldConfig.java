package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlMap;

import java.util.HashMap;
import java.util.Map;

public class FinalFieldConfig extends YamlFileInterface {
    @YamlMap(value = "finalMap", processor = FinalMapProcessor.class)
    public final Map<String, Object> finalMap = new HashMap<>();

    public static class FinalMapProcessor implements YamlMap.YamlMapProcessor<FinalFieldConfig> {
        @Override
        public void read(FinalFieldConfig obj, String key, Map<String, Object> value) {
            // Do nothing
        }

        @Override
        public Map<String, Object> write(FinalFieldConfig obj, String key, Object value) {
            return Map.of();
        }
    }
}
