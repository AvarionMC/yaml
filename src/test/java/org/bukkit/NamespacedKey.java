package org.bukkit;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public final class NamespacedKey implements Key {
    private final String key;

    public NamespacedKey(String key) {
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
