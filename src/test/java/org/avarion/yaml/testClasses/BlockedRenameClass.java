package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;
import org.avarion.yaml.YamlRename;

/**
 * A move whose destination sits under a path an operator may well have used for something else.
 * If {@code wing} is a setting in their file, there is nowhere to build {@code wing.nested}.
 */
@YamlFile
@YamlRename(from = "loose", to = "wing.nested.value")
public class BlockedRenameClass extends YamlFileInterface {

    @YamlKey("wing.nested.value")
    public String value = "default";
}
