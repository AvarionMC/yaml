package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

public class Primitive extends YamlFileInterface {
    @YamlKey("byte")
    public byte bt = 1;
    @YamlKey("char")
    public char chr = 'a';
    @YamlKey("short")
    public short shrt = 1;
    @YamlKey("integer")
    public int intgr = 1;
    @YamlKey("long")
    public long lng = 1L;

    @YamlKey("float")
    public float flt = 1f;
    @YamlKey("double")
    public double dbl = 1d;

    @YamlKey("boolean")
    public boolean bln = true;
}
