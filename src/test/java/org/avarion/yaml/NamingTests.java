package org.avarion.yaml;

import org.avarion.yaml.testClasses.KeepNamingClass;
import org.avarion.yaml.testClasses.NamingRecord;
import org.avarion.yaml.testClasses.SnakeCaseNamingClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link Naming}, the strategy used for keys that are derived from a Java identifier.
 */
class NamingTests extends TestCommon {

    // ===== The conversion itself =====

    @ParameterizedTest
    @CsvSource({
            "modelId,       model_id",
            "modelId2,      model_id2",
            "httpURL,       http_url",
            "getHTTPHeader, get_http_header",
            "name,          name",
            "name1,         name1",
            "already_snake, already_snake",
            "URL,           url",
    })
    void testSnakeCaseConversion(String identifier, String expected) {
        assertEquals(expected, Naming.SNAKE_CASE.convert(identifier));
    }

    @ParameterizedTest
    @CsvSource({"modelId", "modelId2", "httpURL", "name", "already_snake"})
    void testKeepConversionIsTheIdentity(String identifier) {
        assertEquals(identifier, Naming.KEEP.convert(identifier));
    }

    // ===== Derived keys follow the strategy =====

    @Test
    void testSnakeCaseIsTheDefaultForDerivedKeys() throws IOException {
        new SnakeCaseNamingClass().save(target);

        assertEquals("""
                derived_block:
                  model_id: a
                  model_id2: b
                  http_url: c
                  name: d
                  someCamelKey: e
                anExplicitFieldKey: kept
                """, readFile());
    }

    @Test
    void testKeepLeavesDerivedKeysAlone() throws IOException {
        new KeepNamingClass().save(target);

        assertEquals("""
                derivedBlock:
                  modelId: a
                  modelId2: b
                  httpURL: c
                  name: d
                  someCamelKey: e
                anExplicitFieldKey: kept
                """, readFile());
    }

    // ===== An explicit key is never converted, under either strategy =====

    @Test
    void testExplicitKeysStayVerbatimUnderSnakeCase() throws IOException {
        new SnakeCaseNamingClass().save(target);

        String yaml = readFile();
        assertEquals(true, yaml.contains("  someCamelKey: e\n"), yaml);
        assertEquals(true, yaml.contains("anExplicitFieldKey: kept\n"), yaml);
    }

    @Test
    void testExplicitKeysStayVerbatimUnderKeep() throws IOException {
        new KeepNamingClass().save(target);

        String yaml = readFile();
        assertEquals(true, yaml.contains("  someCamelKey: e\n"), yaml);
        assertEquals(true, yaml.contains("anExplicitFieldKey: kept\n"), yaml);
    }

    // ===== Reader and writer agree =====

    @Test
    void testSnakeCaseRoundTrips() throws IOException {
        SnakeCaseNamingClass config = new SnakeCaseNamingClass();
        config.derivedBlock = new NamingRecord("1", "2", "3", "4", "5");
        config.explicitField = "changed";
        config.save(target);

        SnakeCaseNamingClass loaded = new SnakeCaseNamingClass();
        loaded.derivedBlock = null;
        loaded.load(target);

        // Would fail if only the writer converted: the reader would look up 'modelId' and find nothing.
        assertEquals(new NamingRecord("1", "2", "3", "4", "5"), loaded.derivedBlock);
        assertEquals("changed", loaded.explicitField);
    }

    @Test
    void testKeepRoundTrips() throws IOException {
        KeepNamingClass config = new KeepNamingClass();
        config.derivedBlock = new NamingRecord("1", "2", "3", "4", "5");
        config.save(target);

        KeepNamingClass loaded = new KeepNamingClass();
        loaded.derivedBlock = null;
        loaded.load(target);

        assertEquals(new NamingRecord("1", "2", "3", "4", "5"), loaded.derivedBlock);
    }

    @Test
    void testKeepDoesNotAnswerToConvertedComponentKeys() throws IOException {
        // The block itself is found, but its components are spelled the SNAKE_CASE way.
        writeYaml("""
                derivedBlock:
                  model_id: a
                  model_id2: b
                  http_url: c
                  name: d
                  someCamelKey: e
                """);

        KeepNamingClass loaded = new KeepNamingClass();
        loaded.load(target);

        // Under KEEP only 'name' (single word, so unaffected by conversion) and the explicitly
        // named key still line up; the converted spellings are not recognised.
        assertEquals(new NamingRecord(null, null, null, "d", "e"), loaded.derivedBlock);
    }
}
