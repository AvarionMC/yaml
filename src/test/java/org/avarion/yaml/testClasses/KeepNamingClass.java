package org.avarion.yaml.testClasses;

import org.avarion.yaml.Naming;
import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

/**
 * The same shape as {@link SnakeCaseNamingClass}, but opted out of key conversion.
 */
@YamlFile(naming = Naming.KEEP)
public class KeepNamingClass extends YamlFileInterface {

    @YamlKey
    public NamingRecord derivedBlock = new NamingRecord("a", "b", "c", "d", "e");

    @YamlKey("anExplicitFieldKey")
    public String explicitField = "kept";
}
