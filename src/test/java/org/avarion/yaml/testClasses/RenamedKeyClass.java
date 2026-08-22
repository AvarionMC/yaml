package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

/**
 * Fields whose keys have moved, each declaring where it used to live.
 */
@YamlFile
public class RenamedKeyClass extends YamlFileInterface {

    @YamlKey(value = "storm.damage-per-second", previously = "zone.damage-per-second")
    public double damage = 1.0;

    /** Two moves in its history; the file may still be at either one. */
    @YamlKey(value = "hud.boss-bar.colour", previously = {"zone.bar.colour", "bar.colour"})
    public String colour = "RED";

    /** Never moved, so it must be left entirely alone. */
    @YamlKey("game.hub-world")
    public String hubWorld = "hub";
}
