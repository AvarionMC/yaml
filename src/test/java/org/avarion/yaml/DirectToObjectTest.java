package org.avarion.yaml;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DirectToObjectTest extends TestCommon {
    public static class SubDummy implements YamlSerializable {
        private final String id;
        private final Integer value;
        private final List<String> lst;

        public SubDummy(String id, Integer value, List<String> lst) {
            this.id = id;
            this.value = value;
            this.lst = lst;
        }

        @Override
        public Map<String, Object> toYamlMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("value", value);
            map.put("lst", lst);
            return map;
        }

        @Contract("_ -> new")
        public static @NotNull SubDummy fromYamlMap(@NotNull Map<String, Object> map) {
            return new SubDummy((String) map.get("id"), (Integer) map.get("value"), (List<String>) map.get("lst"));
        }
    }

    public static class Dummy implements YamlSerializable {
        private final String s;
        private final List<String> lst;
        private final Map<String, SubDummy> map;

        public Dummy(String s, List<String> lst, Map<String, SubDummy> map) {
            this.s = s;
            this.lst = lst;
            this.map = map;
        }

        @Override
        public Map<String, Object> toYamlMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("s", s);
            result.put("lst", lst);
            result.put("map", map);
            return result;
        }

        @Contract("_ -> new")
        public static @NotNull Dummy fromYamlMap(@NotNull Map<String, Object> map) {
            return new Dummy((String) map.get("s"), (List<String>) map.get("lst"), (Map<String, SubDummy>) map.get("map"));
        }
    }

    @Test
    void testCanSaveDirectlyFromClassSignature() throws IOException {
        @YamlFile
        class TestClass extends YamlFileInterface {
            @YamlKey(value = "dummy")
            public Dummy dummy = new Dummy("s", List.of("lst"), Map.of("id", new SubDummy("id", 1, List.of("lst"))));
        }

        new TestClass().save(target);
        assertEquals(
                "dummy:\n" +
                "  s: s\n" +
                "  lst:\n" +
                "    - lst\n" +
                "  map:\n" +
                "    id:\n" +
                "      id: id\n" +
                "      value: 1\n" +
                "      lst:\n" +
                "        - lst\n",
                readTarget()
        );
    }
}
