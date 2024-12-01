package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;

public class YamlWrapperFactory {
    public static @NotNull YamlWrapper create() {
        try {
            // Check which version is available
            try {
                Class.forName("org.yaml.snakeyaml.representer.Representer").getDeclaredConstructor();
                return (YamlWrapper) Class.forName("org.avarion.yaml.v1.YamlWrapperImpl").getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                return (YamlWrapper) Class.forName("org.avarion.yaml.v2.YamlWrapperImpl").getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create YamlWrapper", e);
        }
    }
}