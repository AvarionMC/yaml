package org.bukkit;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyImpl;
import org.jetbrains.annotations.NotNull;

public class SoundImpl implements Sound {
    private final String name;

    public SoundImpl(String name) {
        this.name = name;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey("sound", name);
    }

    @Override
    public @NotNull Key key() {
        return new KeyImpl(name);
    }

    @Override
    public String toString() {
        return "SoundImpl{name=" + name + "}";
    }
}
