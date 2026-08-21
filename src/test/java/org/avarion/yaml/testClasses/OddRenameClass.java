package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;
import org.avarion.yaml.YamlRename;

/**
 * Declarations that say nothing: a blank old name, a blank destination, and one that names the
 * key the field already uses. All three are what a half-finished edit looks like, and none of
 * them may do anything.
 */
@YamlFile
@YamlRename(from = "old.nowhere", to = "   ")
public class OddRenameClass extends YamlFileInterface {

    @YamlKey(value = "kept.blank", previously = {"", "   "})
    public String blank = "default";

    @YamlKey(value = "kept.circular", previously = "kept.circular")
    public String circular = "default";

    @YamlKey(value = "kept.empty-target", previously = "old.empty-target")
    public String emptyTarget = "default";
}
