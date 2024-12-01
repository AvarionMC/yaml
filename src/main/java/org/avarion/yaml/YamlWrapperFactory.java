package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

public class YamlWrapperFactory {
    public static @NotNull YamlWrapper create() {
        Exception failure = null;
        try {
            try {
                return (YamlWrapper) Class.forName("org.avarion.yaml.v1.YamlWrapperImpl").getDeclaredConstructor().newInstance();
            } catch (NoClassDefFoundError | ClassNotFoundException | NoSuchMethodException ignored) {
            }
            try {
                return (YamlWrapper) Class.forName("org.avarion.yaml.v2.YamlWrapperImpl").getDeclaredConstructor().newInstance();
            } catch (NoClassDefFoundError | ClassNotFoundException | NoSuchMethodException ignored) {
            }
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            failure = e;
        }

        throw new RuntimeException("Failed to create YamlWrapper", failure);
    }
}