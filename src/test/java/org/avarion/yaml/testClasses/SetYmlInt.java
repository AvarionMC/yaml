package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.Set;

public class SetYmlInt extends YamlFileInterface {
    @YamlKey("key")
    public Set<Integer> key = Set.of(1, 2);
}
