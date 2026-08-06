package org.avarion.yaml.testClasses;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Mirrors a Minecraft-style registry constant: named instances reached through a static
 * factory. Deliberately has no public String constructor and no toString() override, so
 * YamlWriter falls through to matching the value against a static field.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Sounds {
    public static final Sounds MY_SOUND_ROCKS = getSound("my.sound.rocks");
    public static final Sounds YOUR_SOUND_ROCKS_TOO = getSound("your.sound.rocks.2");

    private final String name;

    private static @NotNull Sounds getSound(@NotNull String key) {
        return new Sounds(key);
    }
}
