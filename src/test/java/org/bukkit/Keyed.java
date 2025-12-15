
package org.bukkit;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public interface Keyed extends net.kyori.adventure.key.Keyed {
    @NotNull NamespacedKey getKey();

    default @NotNull Key key() {
        return this.getKey();
    }
}