package org.bukkit;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public final class NamespacedKey implements Key {
    private final String ns;
    private final String key;

    public NamespacedKey(String ns, String key) {
        this.ns = ns;
        this.key = key;
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
