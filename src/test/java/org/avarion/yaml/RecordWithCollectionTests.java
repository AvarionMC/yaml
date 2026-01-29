package org.avarion.yaml;

import org.avarion.yaml.testClasses.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Records containing collections of other Records.
 */
class RecordWithCollectionTests extends TestCommon {

    @Test
    void testRecordWithListOfRecordsSaveAndLoad() throws IOException {
        TeamConfigClass config = new TeamConfigClass();
        config.save(target);

        TeamConfigClass loaded = new TeamConfigClass();
        loaded.team = null;
        loaded.load(target);

        // Verify the team record
        assertNotNull(loaded.team);
        assertEquals("Alpha Team", loaded.team.name());

        // Verify the list of Person records within Team
        List<Person> members = loaded.team.members();
        assertNotNull(members);
        assertEquals(2, members.size());

        // Check first member
        Person alice = members.get(0);
        assertEquals("Alice", alice.name());
        assertEquals(28, alice.age());
        assertNotNull(alice.address());
        assertEquals("123 Alpha St", alice.address().street());

        // Check second member
        Person bob = members.get(1);
        assertEquals("Bob", bob.name());
        assertEquals(35, bob.age());
        assertNotNull(bob.address());
        assertEquals("456 Alpha Ave", bob.address().street());

        // Verify headquarters (nested record in Team)
        Address hq = loaded.team.headquarters();
        assertNotNull(hq);
        assertEquals("1 HQ Blvd", hq.street());
        assertEquals("Alpha City", hq.city());
        assertEquals(10000, hq.zipCode());
    }

    @Test
    void testMapOfRecordsWithCollectionsSaveAndLoad() throws IOException {
        TeamConfigClass config = new TeamConfigClass();
        config.save(target);

        TeamConfigClass loaded = new TeamConfigClass();
        loaded.teams.clear();
        loaded.load(target);

        // Verify we have both teams
        assertNotNull(loaded.teams);
        assertEquals(2, loaded.teams.size());

        // Verify alpha team
        Team alpha = loaded.teams.get("alpha");
        assertNotNull(alpha);
        assertEquals("Alpha Team", alpha.name());
        assertEquals(2, alpha.members().size());

        // Verify beta team with 3 members
        Team beta = loaded.teams.get("beta");
        assertNotNull(beta);
        assertEquals("Beta Team", beta.name());
        assertEquals(3, beta.members().size());

        // Check beta team members
        Person charlie = beta.members().get(0);
        assertEquals("Charlie", charlie.name());
        assertEquals(30, charlie.age());

        Person diana = beta.members().get(1);
        assertEquals("Diana", diana.name());
        assertEquals(25, diana.age());

        Person eve = beta.members().get(2);
        assertEquals("Eve", eve.name());
        assertEquals(40, eve.age());

        // Verify beta headquarters
        assertEquals("2 HQ Blvd", beta.headquarters().street());
        assertEquals("Beta City", beta.headquarters().city());
    }

    @Test
    void testRecordWithCollectionSerializesToCorrectYamlFormat() throws IOException {
        TeamConfigClass config = new TeamConfigClass();
        config.save(target);

        String yaml = readFile();

        // Verify the YAML structure
        assertTrue(yaml.contains("team:"), "Should have team key");
        assertTrue(yaml.contains("name:"), "Should have name field");
        assertTrue(yaml.contains("members:"), "Should have members field");
        assertTrue(yaml.contains("headquarters:"), "Should have headquarters field");
        assertTrue(yaml.contains("Alpha Team"), "Should have team name");
        assertTrue(yaml.contains("Alice"), "Should have first member name");
        assertTrue(yaml.contains("Bob"), "Should have second member name");
    }

    @Test
    void testRecordWithEmptyCollection() throws IOException {
        TeamConfigClass config = new TeamConfigClass();
        // Create a team with empty members list
        config.team = new Team("Empty Team", List.of(), new Address("Empty HQ", "Ghost Town", 0));
        config.save(target);

        TeamConfigClass loaded = new TeamConfigClass();
        loaded.team = null;
        loaded.load(target);

        assertNotNull(loaded.team);
        assertEquals("Empty Team", loaded.team.name());
        assertNotNull(loaded.team.members());
        assertTrue(loaded.team.members().isEmpty());
    }

    @Test
    void testModifiedRecordWithCollection() throws IOException {
        TeamConfigClass config = new TeamConfigClass();
        config.save(target);

        // Modify the saved YAML
        replaceInTarget("Alpha Team", "Modified Alpha");
        replaceInTarget("Alice", "Alicia");

        TeamConfigClass loaded = new TeamConfigClass();
        loaded.team = null;
        loaded.load(target);

        assertEquals("Modified Alpha", loaded.team.name());
        assertEquals("Alicia", loaded.team.members().get(0).name());
    }
}
