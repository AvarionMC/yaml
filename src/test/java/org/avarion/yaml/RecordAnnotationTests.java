package org.avarion.yaml;

import org.avarion.yaml.testClasses.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link YamlComment} and {@link YamlKey} placed on record components.
 */
class RecordAnnotationTests extends TestCommon {

    // ===== @YamlComment on record components =====

    @Test
    void testCommentsAreWrittenPerRecordComponent() throws IOException {
        CommentedRecordClass config = new CommentedRecordClass();
        config.save(target);

        assertTrue(readFile().startsWith("""
                # Home address
                address:
                  # Street name and number
                  street: 123 Main St
                  # City the address is in.
                  # Used for the delivery zone.
                  city: Springfield
                  zip_code: 12345
                """), "Each component should get its own comment:\n" + readFile());
    }

    @Test
    void testCommentsAreWrittenAtEveryNestingLevel() throws IOException {
        CommentedRecordClass config = new CommentedRecordClass();
        config.save(target);

        assertTrue(readFile().contains("""
                person:
                  # Full name
                  name: John Doe
                  # Age in years
                  age: 30
                  # Where this person lives
                  address:
                    # Street name and number
                    street: 456 Work St
                    # City the address is in.
                    # Used for the delivery zone.
                    city: Shelbyville
                    zip_code: 67890
                """), "Nested records should be commented too:\n" + readFile());
    }

    @Test
    void testCommentsAreSuppressedInsideCollections() throws IOException {
        CommentedRecordClass config = new CommentedRecordClass();
        config.save(target);

        assertTrue(readFile().endsWith("""
                previous-addresses:
                  - street: 1 Old Rd
                    city: Ogdenville
                    zip_code: 11111
                  - street: 2 Older Rd
                    city: North Haverbrook
                    zip_code: 22222
                """), "List items should stay comment-free:\n" + readFile());
    }

    @Test
    void testCommentedRecordRoundTrip() throws IOException {
        new CommentedRecordClass().save(target);

        CommentedRecordClass loaded = new CommentedRecordClass();
        loaded.address = null;
        loaded.person = null;
        loaded.previousAddresses.clear();
        loaded.load(target);

        assertEquals(new CommentedAddress("123 Main St", "Springfield", 12345), loaded.address);
        assertEquals(new CommentedPerson("John Doe", 30, new CommentedAddress("456 Work St", "Shelbyville", 67890)), loaded.person);
        assertEquals(List.of(new CommentedAddress("1 Old Rd", "Ogdenville", 11111),
                             new CommentedAddress("2 Older Rd", "North Haverbrook", 22222)), loaded.previousAddresses);
    }

    // ===== Backwards compatibility =====

    /**
     * {@link SimpleRecordClass} is pinned to {@link Naming#KEEP}, the escape hatch for configs
     * written before the naming strategy existed: its output stays byte-for-byte identical.
     */
    @Test
    void testKeepNamingReproducesTheOriginalSpellingByteForByte() throws IOException {
        new SimpleRecordClass().save(target);

        assertEquals("""
                address:
                  street: 123 Main St
                  city: Springfield
                  zipCode: 12345
                person:
                  name: John Doe
                  age: 30
                  address:
                    street: 456 Work St
                    city: Shelbyville
                    zipCode: 67890
                """, readFile());
    }

    // ===== @YamlKey on record components =====

    @Test
    void testYamlKeyRenamesRecordComponents() throws IOException {
        new RenamedRecordClass().save(target);

        assertEquals("""
                # Database settings
                database:
                  # Host the database listens on
                  host_name: localhost
                  port: 5432
                  # Credentials used to connect
                  login:
                    # Login user
                    user_name: admin
                    pass_word: secret
                """, readFile());
    }

    @Test
    void testRenamedRecordRoundTrip() throws IOException {
        new RenamedRecordClass().save(target);

        RenamedRecordClass loaded = new RenamedRecordClass();
        loaded.database = null;
        loaded.load(target);

        assertEquals(new RenamedDatabase("localhost", 5432, new RenamedCredentials("admin", "secret")), loaded.database);
    }

    @Test
    void testRenamedRecordLoadsFromHandWrittenYaml() throws IOException {
        writeYaml("""
                database:
                  host_name: db.example.com
                  port: 3306
                  login:
                    user_name: root
                    pass_word: hunter2
                """);

        RenamedRecordClass loaded = new RenamedRecordClass();
        loaded.load(target);

        assertEquals(new RenamedDatabase("db.example.com", 3306, new RenamedCredentials("root", "hunter2")), loaded.database);
    }

    @Test
    void testComponentNameIsIgnoredOnceRenamed() throws IOException {
        writeYaml("""
                database:
                  hostName: db.example.com
                  port: 3306
                  credentials:
                    userName: root
                    passWord: hunter2
                """);

        RenamedRecordClass loaded = new RenamedRecordClass();
        loaded.load(target);

        // 'port' still loads by component name; the renamed ones no longer answer to it.
        assertEquals(new RenamedDatabase(null, 3306, null), loaded.database);
    }

    // ===== Unsupported dotted keys =====

    @Test
    void testDottedComponentKeyIsRejectedWhenSaving() {
        DottedKeyClass config = new DottedKeyClass();

        IOException error = assertThrows(IOException.class, () -> config.save(target));
        assertTrue(error.getMessage().contains("nested.value"), error.getMessage());
    }

    @Test
    void testDottedComponentKeyIsRejectedWhenLoading() throws IOException {
        writeYaml("""
                holder:
                  value: something
                """);

        DottedKeyClass loaded = new DottedKeyClass();

        IOException error = assertThrows(IOException.class, () -> loaded.load(target));
        assertTrue(error.getMessage().contains("nested.value"), error.getMessage());
    }
}
