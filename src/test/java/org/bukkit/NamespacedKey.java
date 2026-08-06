package org.bukkit;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Mirrors the shape of the real Bukkit NamespacedKey, including its namespace/key pair,
 * so the reflective lookups in YamlWriter run against a realistic class.
 */
public final class NamespacedKey implements Key {
    private final String ns;
    private final String key;

    public NamespacedKey(String ns, String key) {
        this.ns = ns;
        this.key = key;
    }

    public @NotNull String getNamespace() {
        return ns;
    }

    @Override
    public @NotNull String value() {
        return key;
    }

    @Override
    public @NotNull Key key() {
        return this;
    }
}
