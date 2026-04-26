package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;

public class YamlWrapperFactory {
    private YamlWrapperFactory() { }

    public static @NotNull YamlWrapper create() {
        return create("org.yaml.snakeyaml.representer.Representer",
                "org.avarion.yaml.v1.YamlWrapperImpl",
                "org.avarion.yaml.v2.YamlWrapperImpl");
    }

    /**
     * Package-private overload that takes the class names so test code can drive each
     * branch (snakeyaml-missing, impl-missing) by supplying nonexistent class names.
     */
    static @NotNull YamlWrapper create(@NotNull String snakeyamlSentinel,
                                       @NotNull String v1Impl,
                                       @NotNull String v2Impl) {
        try {
            // SnakeYAML v1's Representer has a no-arg constructor; v2's doesn't.
            Class.forName(snakeyamlSentinel).getDeclaredConstructor();
            return instantiate(v1Impl);
        } catch (NoSuchMethodException e) {
            return instantiate(v2Impl);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("snakeyaml not on classpath: " + snakeyamlSentinel, e);
        }
    }

    static @NotNull YamlWrapper instantiate(@NotNull String className) {
        try {
            return (YamlWrapper) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate " + className, e);
        }
    }
}