package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.Set;
import java.util.UUID;

public class LoadingUUIDs extends YamlFileInterface {
    @YamlKey("key")
    public Set<UUID> key = Set.of(UUID.fromString("11111111-2222-3333-4444-555555555555"));
}
