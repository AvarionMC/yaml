package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlMap;

import java.util.HashMap;
import java.util.Map;

public class MapTestClass extends YamlFileInterface {
    @YamlMap(value = "a", processor = MapTestClass.Processor.class)
    public Map<String, Object> tmp = new HashMap<>();

    public static class Processor implements YamlMap.YamlMapProcessor<MapTestClass> {
        @Override
        public void read(MapTestClass obj, String key, Map<String, Object> value) {
            // Do nothing, keep the map empty
            obj.tmp.put("obj", 1);
        }

        @Override
        public Map<String, Object> write(MapTestClass obj, String key, Object value) {
            return Map.of();
        }
    }
}
