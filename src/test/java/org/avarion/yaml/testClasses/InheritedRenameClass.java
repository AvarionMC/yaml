package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;
import org.avarion.yaml.YamlRename;

/**
 * A move declared on a base class, and another on the class that extends it — the base's is
 * applied first, so the subclass can name a path the base has just created.
 */
@YamlFile
public class InheritedRenameClass {

    @YamlRename(from = "old", to = "middle")
    public abstract static class Base extends YamlFileInterface {
    }

    @YamlRename(from = "middle.value", to = "current.value")
    public static class Derived extends Base {

        @YamlKey("current.value")
        public String value = "default";
    }
}
