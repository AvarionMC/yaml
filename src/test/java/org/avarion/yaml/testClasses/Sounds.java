package org.avarion.yaml.testClasses;

/**
 * Constants looked up by static field name. Deliberately has no public String constructor and
 * no toString() override, so YamlWriter falls through to matching the value against a static
 * field. Instances carry no state -- the tests only ever compare identity.
 */
public class Sounds {
    public static final Sounds MY_SOUND_ROCKS = new Sounds();
    public static final Sounds YOUR_SOUND_ROCKS_TOO = new Sounds();

    private Sounds() {
    }
}
