package net.kyori.adventure.key;

import org.jetbrains.annotations.NotNull;

public class KeyImpl implements Key {
    private final String name;

    public KeyImpl(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String value() {
        return name;
    }

    @Override
    public @NotNull Key key() {
        return null;
    }
}
