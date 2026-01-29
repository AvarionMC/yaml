package org.avarion.yaml;

import org.avarion.yaml.testClasses.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Java Record support in YAML serialization/deserialization.
 */
class RecordTests extends TestCommon {

    // ===== Simple Record Field Tests =====

    @Test
    void testSimpleRecordSaveAndLoad() throws IOException {
        SimpleRecordClass config = new SimpleRecordClass();
        config.save(target);

        SimpleRecordClass loaded = new SimpleRecordClass();
        loaded.address = null;  // Reset to ensure it's loaded from file
        loaded.person = null;
        loaded.load(target);

        // Verify simple record
        assertNotNull(loaded.address);
        assertEquals("123 Main St", loaded.address.street());
        assertEquals("Springfield", loaded.address.city());
        assertEquals(12345, loaded.address.zipCode());
    }

    @Test
    void testNestedRecordSaveAndLoad() throws IOException {
        SimpleRecordClass config = new SimpleRecordClass();
        config.save(target);

        SimpleRecordClass loaded = new SimpleRecordClass();
        loaded.person = null;  // Reset to ensure it's loaded from file
        loaded.load(target);

        // Verify nested record (Person contains Address)
        assertNotNull(loaded.person);
        assertEquals("John Doe", loaded.person.name());
        assertEquals(30, loaded.person.age());

        Address personAddress = loaded.person.address();
        assertNotNull(personAddress);
        assertEquals("456 Work St", personAddress.street());
        assertEquals("Shelbyville", personAddress.city());
        assertEquals(67890, personAddress.zipCode());
    }

    // ===== Map<String, Record> Tests =====

    @Test
    void testMapOfSimpleRecordsSaveAndLoad() throws IOException {
        RecordMapClass config = new RecordMapClass();
        config.save(target);

        RecordMapClass loaded = new RecordMapClass();
        loaded.addresses.clear();  // Reset to ensure it's loaded from file
        loaded.load(target);

        // Verify map of simple records
        assertNotNull(loaded.addresses);
        assertEquals(2, loaded.addresses.size());

        Address home = loaded.addresses.get("home");
        assertNotNull(home);
        assertEquals("123 Main St", home.street());
        assertEquals("Springfield", home.city());
        assertEquals(12345, home.zipCode());

        Address work = loaded.addresses.get("work");
        assertNotNull(work);
        assertEquals("456 Office Blvd", work.street());
        assertEquals("Shelbyville", work.city());
        assertEquals(67890, work.zipCode());
    }

    @Test
    void testMapOfNestedRecordsSaveAndLoad() throws IOException {
        RecordMapClass config = new RecordMapClass();
        config.save(target);

        RecordMapClass loaded = new RecordMapClass();
        loaded.people.clear();  // Reset to ensure it's loaded from file
        loaded.load(target);

        // Verify map of nested records (Person contains Address)
        assertNotNull(loaded.people);
        assertEquals(2, loaded.people.size());

        Person john = loaded.people.get("john");
        assertNotNull(john);
        assertEquals("John Doe", john.name());
        assertEquals(30, john.age());
        assertNotNull(john.address());
        assertEquals("789 Elm St", john.address().street());
        assertEquals("Capital City", john.address().city());
        assertEquals(11111, john.address().zipCode());

        Person jane = loaded.people.get("jane");
        assertNotNull(jane);
        assertEquals("Jane Smith", jane.name());
        assertEquals(25, jane.age());
        assertNotNull(jane.address());
        assertEquals("321 Oak Ave", jane.address().street());
        assertEquals("Ogdenville", jane.address().city());
        assertEquals(22222, jane.address().zipCode());
    }

    // ===== YAML Format Verification Tests =====

    @Test
    void testRecordSerializesToCorrectYamlFormat() throws IOException {
        SimpleRecordClass config = new SimpleRecordClass();
        config.save(target);

        String yaml = readFile();

        // Verify the YAML structure for a simple record
        assertTrue(yaml.contains("address:"), "Should have address key");
        assertTrue(yaml.contains("street:"), "Should have street field");
        assertTrue(yaml.contains("city:"), "Should have city field");
        assertTrue(yaml.contains("zipCode:"), "Should have zipCode field");
        assertTrue(yaml.contains("123 Main St"), "Should have street value");
        assertTrue(yaml.contains("Springfield"), "Should have city value");
        assertTrue(yaml.contains("12345"), "Should have zipCode value");
    }

    @Test
    void testMapOfRecordsSerializesToCorrectYamlFormat() throws IOException {
        RecordMapClass config = new RecordMapClass();
        config.save(target);

        String yaml = readFile();

        // Verify the YAML structure for map of records
        assertTrue(yaml.contains("addresses:"), "Should have addresses key");
        assertTrue(yaml.contains("home:"), "Should have home entry");
        assertTrue(yaml.contains("work:"), "Should have work entry");
        assertTrue(yaml.contains("123 Main St"), "Should have home street");
        assertTrue(yaml.contains("456 Office Blvd"), "Should have work street");
    }

    // ===== Edge Case Tests =====

    @Test
    void testRecordWithModifiedValues() throws IOException {
        RecordMapClass config = new RecordMapClass();
        config.save(target);

        // Modify the saved YAML
        replaceInTarget("123 Main St", "999 Changed St");
        replaceInTarget("12345", "99999");

        RecordMapClass loaded = new RecordMapClass();
        loaded.addresses.clear();
        loaded.load(target);

        Address home = loaded.addresses.get("home");
        assertNotNull(home);
        assertEquals("999 Changed St", home.street());
        assertEquals(99999, home.zipCode());
    }

    @Test
    void testRecordWithNullNestedRecord() throws IOException {
        // Create a person with null address
        SimpleRecordClass config = new SimpleRecordClass();
        config.person = new Person("No Address Person", 40, null);
        config.save(target);

        SimpleRecordClass loaded = new SimpleRecordClass();
        loaded.person = null;
        loaded.load(target);

        assertNotNull(loaded.person);
        assertEquals("No Address Person", loaded.person.name());
        assertEquals(40, loaded.person.age());
        assertNull(loaded.person.address());
    }

    // ===== List<Record> Tests =====

    @Test
    void testListOfSimpleRecordsSaveAndLoad() throws IOException {
        RecordCollectionClass config = new RecordCollectionClass();
        config.save(target);

        RecordCollectionClass loaded = new RecordCollectionClass();
        loaded.addressList.clear();
        loaded.load(target);

        assertNotNull(loaded.addressList);
        assertEquals(2, loaded.addressList.size());

        Address first = loaded.addressList.get(0);
        assertNotNull(first);
        assertEquals("123 First St", first.street());
        assertEquals("Town A", first.city());
        assertEquals(11111, first.zipCode());

        Address second = loaded.addressList.get(1);
        assertNotNull(second);
        assertEquals("456 Second St", second.street());
        assertEquals("Town B", second.city());
        assertEquals(22222, second.zipCode());
    }

    @Test
    void testListOfNestedRecordsSaveAndLoad() throws IOException {
        RecordCollectionClass config = new RecordCollectionClass();
        config.save(target);

        RecordCollectionClass loaded = new RecordCollectionClass();
        loaded.personList.clear();
        loaded.load(target);

        assertNotNull(loaded.personList);
        assertEquals(2, loaded.personList.size());

        Person alice = loaded.personList.get(0);
        assertNotNull(alice);
        assertEquals("Alice", alice.name());
        assertEquals(28, alice.age());
        assertNotNull(alice.address());
        assertEquals("789 Third St", alice.address().street());

        Person bob = loaded.personList.get(1);
        assertNotNull(bob);
        assertEquals("Bob", bob.name());
        assertEquals(35, bob.age());
        assertNotNull(bob.address());
        assertEquals("321 Fourth St", bob.address().street());
    }
}
