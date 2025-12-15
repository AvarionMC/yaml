package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

public class StaticInterfaceTestClass extends YamlFileInterface {
    @YamlKey("name")
    public StaticMat name = StaticInterfaceElements.A;
}
