package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;


public class PrivateYml extends YamlFileInterface {
    @YamlKey("keyPrivate")
    private int keyPrivate = 1;

    @YamlKey("keyNone")
    int keyNone = 1;

    @YamlKey("keyProtected")
    protected int keyProtected = 1;

    @YamlKey("keyPublic")
    public int keyPublic = 1;

    public int getPriv() { return keyPrivate; }

    public int getPub() { return keyPublic; }

    public int getNone() { return keyNone; }

    public int getProt() { return keyProtected; }
}
