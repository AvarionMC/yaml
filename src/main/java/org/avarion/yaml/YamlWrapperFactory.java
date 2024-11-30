package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;

public class YamlWrapperFactory {
    public static @NotNull YamlWrapper create() {
        try {
            return (YamlWrapper) Class.forName("org.avarion.yaml.YamlWrapperImpl").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create YamlWrapper", e);
        }
    }
}