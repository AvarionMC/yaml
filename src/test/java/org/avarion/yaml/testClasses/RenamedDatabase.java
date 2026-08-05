package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlComment;
import org.avarion.yaml.YamlKey;

/**
 * A record mixing renamed and un-renamed components; {@code port} keeps its component name.
 */
public record RenamedDatabase(
        @YamlKey("host_name") @YamlComment("Host the database listens on") String hostName,

        int port,

        @YamlKey("login") @YamlComment("Credentials used to connect") RenamedCredentials credentials) {
}
