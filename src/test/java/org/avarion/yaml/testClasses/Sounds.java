package org.avarion.yaml.testClasses;

import org.jetbrains.annotations.NotNull;

public class Sounds {
    public static final Sounds MY_SOUND_ROCKS = getSound("my.sound.rocks");
    public static final Sounds YOUR_SOUND_ROCKS_TOO = getSound("your.sound.rocks.2");

    private final String name;

    private static @NotNull Sounds getSound(@NotNull String key) {
        return new Sounds(key);
    }

    private Sounds(String name) {
        this.name = name;
    }
}
