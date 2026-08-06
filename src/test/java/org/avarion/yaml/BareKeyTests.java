package org.avarion.yaml;

import org.avarion.yaml.testClasses.BareKeyClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for a bare {@link YamlKey}, which persists the field under its own name.
 */
class BareKeyTests extends TestCommon {

    @Test
    void testBareKeyDerivesTheKeyFromTheFieldName() throws IOException {
        new BareKeyClass().save(target);

        // serverName is derived (and so converted); max_players was spelled out and is used as-is.
        assertEquals("""
                # Uses the field name as its key
                server_name: Main
                max_players: 20
                debug: false
                """, readFile());
    }

    @Test
    void testBareKeyRoundTrips() throws IOException {
        BareKeyClass config = new BareKeyClass();
        config.serverName = "Renamed";
        config.maxPlayers = 100;
        config.debug = true;
        config.save(target);

        BareKeyClass loaded = new BareKeyClass().load(target);

        assertEquals("Renamed", loaded.serverName);
        assertEquals(100, loaded.maxPlayers);
        assertEquals(true, loaded.debug);
    }

    @Test
    void testFieldWithoutAnnotationIsStillSkipped() throws IOException {
        writeYaml("""
                server_name: FromFile
                max_players: 5
                debug: true
                notPersisted: FromFile
                """);

        BareKeyClass loaded = new BareKeyClass().load(target);

        assertEquals("FromFile", loaded.serverName);
        // No @YamlKey at all still means "not part of the configuration".
        assertEquals("ignored", loaded.notPersisted);
    }
}
