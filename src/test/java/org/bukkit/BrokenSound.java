package org.bukkit;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * A Sound implementation whose key() method throws,
 * used to test error handling in YamlWriter.formatValue.
 */
public class BrokenSound implements Sound {
    @Override
    public @NotNull NamespacedKey getKey() {
        throw new RuntimeException("broken getKey");
    }

    @Override
    public @NotNull Key key() {
        throw new RuntimeException("broken key");
    }
}
