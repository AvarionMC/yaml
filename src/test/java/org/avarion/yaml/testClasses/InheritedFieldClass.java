package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlComment;
import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

/**
 * A settings class with a base class holding settings of its own — the shape a framework takes
 * when it has something every configuration needs to carry.
 */
public class InheritedFieldClass {

    @YamlFile(header = "Inherited fields")
    public abstract static class Base extends YamlFileInterface {

        @YamlComment("Belongs to every configuration, not to any one of them")
        @YamlKey("shared.debug")
        public boolean debug = false;
    }

    public static class Derived extends Base {

        @YamlKey("own.timeout")
        public int timeout = 30;
    }
}
