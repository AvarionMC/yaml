package org.bukkit;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface Sound extends Keyed {
    Sound A = getSound("A");
    Sound B = getSound("B");

    @Contract("_ -> new")
    static @NotNull Sound getSound(String name) {
        return new SoundImpl(name);
    }
}
