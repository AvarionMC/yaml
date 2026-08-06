package net.kyori.adventure.key;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class KeyImpl implements Key {
    private final String name;

    @Override
    public @NotNull String value() {
        return name;
    }

    @Override
    public @NotNull Key key() {
        return null;
    }
}
