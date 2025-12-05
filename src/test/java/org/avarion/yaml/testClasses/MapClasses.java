package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.Map;

public class MapClasses extends YamlFileInterface {
    // Analogue to the NonPrimitive test class
    @YamlKey("byte")
    public Map<Byte, String> tmpByte = Map.of((byte) 1, "a");
    @YamlKey("char")
    public Map<Character, String> tmpChar = Map.of('x', "b");
    @YamlKey("short")
    public Map<Short, String> tmpShort = Map.of((short) 2, "c");
    @YamlKey("integer")
    public Map<Integer, String> tmpInt = Map.of(3, "d");
    @YamlKey("long")
    public Map<Long, String> tmpLng = Map.of(4L, "e");

    @YamlKey("float")
    public Map<Float, String> tmpFlt = Map.of(5f, "f");
    @YamlKey("double")
    public Map<Double, String> tmpDbl = Map.of(6d, "g");

    @YamlKey("bool")
    public Map<Boolean, String> tmpBool = Map.of(true, "h");
}
