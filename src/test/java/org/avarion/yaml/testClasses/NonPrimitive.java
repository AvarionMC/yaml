package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

public class NonPrimitive extends YamlFileInterface {
    @YamlKey("byte")
    public Byte bt = 1;
    @YamlKey("char")
    public Character chr = 'a';
    @YamlKey("short")
    public Short shrt = 1;
    @YamlKey("integer")
    public Integer intgr = 1;
    @YamlKey("long")
    public Long lng = 1L;

    @YamlKey("float")
    public Float flt = 1f;
    @YamlKey("double")
    public Double dbl = 1d;

    @YamlKey("boolean")
    public Boolean bln = true;
}
