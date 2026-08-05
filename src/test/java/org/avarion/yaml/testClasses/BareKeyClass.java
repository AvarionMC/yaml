package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlComment;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

/**
 * Test class mixing a bare {@link YamlKey} (key taken from the field name), an explicitly
 * named key, and a field with no annotation at all (never persisted).
 */
public class BareKeyClass extends YamlFileInterface {

    @YamlComment("Uses the field name as its key")
    @YamlKey
    public String serverName = "Main";

    @YamlKey("max_players")
    public int maxPlayers = 20;

    @YamlKey("")
    public boolean debug = false;

    public String notPersisted = "ignored";
}
