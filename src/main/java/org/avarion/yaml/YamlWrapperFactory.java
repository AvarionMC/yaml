package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

public class YamlWrapperFactory {
    private YamlWrapperFactory() {

    }

    public static @NotNull YamlWrapper create() {
        try {
            return getYamlWrapper();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create YamlWrapper", e);
        }
    }

    private static @NotNull YamlWrapper getYamlWrapper()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        try {
            Class.forName("org.yaml.snakeyaml.representer.Representer").getDeclaredConstructor();
            return (YamlWrapper) Class.forName("org.avarion.yaml.v1.YamlWrapperImpl").getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            return (YamlWrapper) Class.forName("org.avarion.yaml.v2.YamlWrapperImpl").getDeclaredConstructor().newInstance();
        }
    }
}