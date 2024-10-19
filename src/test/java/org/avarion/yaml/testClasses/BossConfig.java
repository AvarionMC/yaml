package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlMap;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BossConfig extends YamlFileInterface {
    @YamlMap(value = "bosses", processor = BossConfig.BossProcessor.class)
    public Map<String, Boss> bosses;

    public BossConfig() {
        bosses = new HashMap<>();
        bosses.put("boss1", new Boss("name1", "internal1", "arena1"));
        bosses.put("boss2", new Boss("name2", "internal2", "arena2"));
    }

    public static class BossProcessor implements YamlMap.YamlMapProcessor<BossConfig> {
        @Override
        public void read(BossConfig obj, String key, Map<String, Object> value) {
            String name = (String) value.get("name");
            String internalName = (String) value.get("internal_name");
            String arena = (String) value.get("arena");
            obj.bosses.put(key, new Boss(name, internalName, arena));
        }

        @Override
        public Map<String, Object> write(BossConfig obj, String key, Object value) {
            Boss boss = (Boss) value;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", boss.getName());
            map.put("internal_name", boss.getInternalName());
            map.put("arena", boss.getArena());
            return map;
        }
    }
}