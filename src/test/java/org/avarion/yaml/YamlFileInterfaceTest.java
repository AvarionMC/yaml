package org.avarion.yaml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YamlFileInterfaceTest {
	File target;

	private void replaceInTarget(File file, String text, String replacement) throws IOException {
		// Read all lines from the file into a string
		Path filePath = file.toPath();
		String content = new String(Files.readAllBytes(filePath));

		// Replace the target text with the replacement
		content = content.replace(text, replacement);

		// Write the modified content back to the file
		Files.write(filePath, content.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	@BeforeEach
	void setUp() {
		try {
			target = File.createTempFile("yaml", ".yaml");
			target.deleteOnExit();
			target.delete();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void testSaveAndLoad() throws IOException {
		HappyFlow yamlFile = new HappyFlow();

		assertEquals("New York", yamlFile.city);
		assertEquals(123, yamlFile.streetNumber);

		yamlFile.city = "Home";
		yamlFile.streetNumber = 456;

		assertFalse(target.exists());
		yamlFile.save(target);
		assertTrue(target.exists());

		// Load the saved file and check if it contains expected content
		HappyFlow loaded = new HappyFlow().load(target);

		assertNotNull(loaded);
		assertEquals("John Doe", loaded.name);
		assertEquals(30, loaded.age);
		assertTrue(loaded.isStudent);
		assertEquals(70.5, loaded.weight);
		assertEquals(85.5f, loaded.score);
		assertEquals(123456789L, loaded.id);
		assertTrue(loaded.isMale);
		assertEquals("Home", loaded.city);
		assertEquals(456, loaded.streetNumber);
	}

	// Add more test cases as needed for other methods in YamlFileInterface
	@Test
	void testSaveAndLoad2() throws IOException {
		NullOrEmptyKey yamlFile = new NullOrEmptyKey();
		yamlFile.name1 = "1";
		yamlFile.name2 = "2";
		yamlFile.name3 = "3";

		assertFalse(target.exists());
		assertEquals("1", yamlFile.name1);
		assertEquals("2", yamlFile.name2);
		assertEquals("3", yamlFile.name3);

		yamlFile.save(target);
		assertTrue(target.exists());

		NullOrEmptyKey loaded = new NullOrEmptyKey().load(target);

		assertEquals("A", loaded.name1);
		assertEquals("B", loaded.name2);
		assertEquals("3", loaded.name3);
	}

	@Test
	void testEnumerations() throws IOException {
		final List<Material> def = Arrays.asList(Material.A, Material.B);

		ListMaterial yamlFile = new ListMaterial();

		assertFalse(target.exists());
		assertEquals(def, yamlFile.materials);
		assertEquals(Material.C, yamlFile.material);

		yamlFile.save(target);
		assertTrue(target.exists());

		ListMaterial loaded = new ListMaterial().load(target);
		assertEquals(def, loaded.materials);
		assertEquals(Material.C, loaded.material);
	}

	@Test
	void testEnumerationsInvalidEnumItem() throws IOException {
		(new ListMaterial()).save(target);
		replaceInTarget(target, "- 'B'", "- 'D'");

		IOException thrown = assertThrows(IOException.class, () -> {
			new ListMaterial().load(target);
		});
		assertTrue(thrown.getMessage().contains("No enum constant org.avarion.yaml.Material.D"));
	}

	@Test
	void testEnumerationsInvalidEnumItem2() throws IOException {
		(new ListMaterial()).save(target);
		replaceInTarget(target, "'C'", "2");

		IOException thrown = assertThrows(IOException.class, () -> {
			new ListMaterial().load(target);
		});
		assertTrue(thrown.getMessage().contains("Cannot convert Integer to Material"));
	}

	@Test
	void testPrimitives() throws IOException {
		Primitive yamlFile = new Primitive();
		assertFalse(target.exists());
		assertEquals(1, yamlFile.bt);
		assertEquals('a', yamlFile.chr);
		assertEquals(1, yamlFile.shrt);
		assertEquals(1, yamlFile.intgr);
		assertEquals(1, yamlFile.lng);
		assertEquals(1, yamlFile.flt);
		assertEquals(1, yamlFile.dbl);
		assertTrue(yamlFile.bln);

		yamlFile.save(target);
		assertTrue(target.exists());

		Primitive loaded = new Primitive().load(target);
		assertEquals(1, loaded.bt);
		assertEquals('a', loaded.chr);
		assertEquals(1, loaded.shrt);
		assertEquals(1, loaded.intgr);
		assertEquals(1, loaded.lng);
		assertEquals(1, loaded.flt);
		assertEquals(1, loaded.dbl);
		assertTrue(loaded.bln);
	}

	@Test
	void testNonPrimitives() throws IOException {
		NonPrimitive yamlFile = new NonPrimitive();
		assertFalse(target.exists());
		assertEquals((byte)1, yamlFile.bt);
		assertEquals('a', yamlFile.chr);
		assertEquals((short)1, yamlFile.shrt);
		assertEquals(1, yamlFile.intgr);
		assertEquals(1, yamlFile.lng);
		assertEquals(1, yamlFile.flt);
		assertEquals(1, yamlFile.dbl);
		assertTrue(yamlFile.bln);

		yamlFile.save(target);
		assertTrue(target.exists());

		replaceInTarget(target, "1", "2");
		replaceInTarget(target, ": a", ": b");

		NonPrimitive loaded = new NonPrimitive().load(target);

		assertEquals((byte) 2, loaded.bt);
		assertEquals('b', loaded.chr);
		assertEquals((short) 2, loaded.shrt);
		assertEquals(2, loaded.intgr);
		assertEquals(2, loaded.lng);
		assertEquals(2, loaded.flt);
		assertEquals(2, loaded.dbl);
		assertTrue(loaded.bln);
	}

	@Test
	void testNullOnPrimitive() throws IOException {
		(new Primitive()).save(target);
		replaceInTarget(target, "1", "null");

		IOException thrown = assertThrows(IOException.class, () -> {
			new Primitive().load(target);
		});
		assertTrue(thrown.getMessage().contains("Cannot assign null to primitive type byte (field: bt)"));
	}

	@Test
	void testNullOnPrimitiveButNotExistingField() throws IOException {
		(new Primitive()).save(target);
		replaceInTarget(target, "byte: 1\n", "");

		IOException thrown = assertThrows(IOException.class, () -> {
			new Primitive().load(target);
		});
		assertTrue(thrown.getMessage().contains("Cannot assign null to primitive type byte (field: bt)"));
	}

	@Test
	void testNullOnNonPrimitive() throws IOException {
		(new NonPrimitive()).save(target);

		replaceInTarget(target, ": a", ": null");
		replaceInTarget(target, "1.0", "null");
		replaceInTarget(target, "1", "null");
		replaceInTarget(target, "true", "null");

		NonPrimitive loaded = new NonPrimitive().load(target);
		assertNull(loaded.bt);
		assertNull(loaded.chr);
		assertNull(loaded.shrt);
		assertNull(loaded.intgr);
		assertNull(loaded.lng);
		assertNull(loaded.flt);
		assertNull(loaded.dbl);
		assertNull(loaded.bln);
	}

	@ParameterizedTest
	@ValueSource(strings = {"\"true\"", "yes", "y", "\"1\"", "YES", "TrUe", "  yEs  "})
	void testDifferentBooleanValues(final String val) throws IOException {
		(new Primitive()).save(target);

		replaceInTarget(target, "true", val);

		NonPrimitive loaded = new NonPrimitive().load(target);

		assertTrue(loaded.bln);
	}

	@Test
	void testDifferentBooleanValuesFalse() throws IOException {
		(new Primitive()).save(target);

		replaceInTarget(target, "true", "\"false\"");

		NonPrimitive loaded = new NonPrimitive().load(target);

		assertFalse(loaded.bln);
	}

	@Test
	void testDoubleKeyUsage() {
		IOException thrown = assertThrows(IOException.class, () -> {
			(new DoubleKeyUsage()).save(target);
		});
		assertTrue(thrown.getMessage().contains("'key1' is already used before"));
	}

	@Test
	void testIntAsDoubleValue() throws IOException {
		(new NonPrimitive()).save(target);
		replaceInTarget(target, "1.0", "2");

		NonPrimitive loaded = new NonPrimitive().load(target.toString());
		assertEquals(2.0f, loaded.flt);
		assertEquals(2.0d, loaded.dbl);
	}

	@Test
	void testWrongChar() throws IOException {
		(new NonPrimitive()).save(target);
		replaceInTarget(target, ": a", ": 2"); // Now it's an integer

		IOException thrown = assertThrows(IOException.class, () -> {
			new NonPrimitive().load(target);
		});
		assertTrue(thrown.getMessage().contains("Cannot convert Integer to Character"));
	}


	@Test
	void testWrongChar2() throws IOException {
		(new NonPrimitive()).save(target);
		replaceInTarget(target, ": a", ": abc"); // Now it's a string

		IOException thrown = assertThrows(IOException.class, () -> {
			new NonPrimitive().load(target);
		});
		assertTrue(thrown.getMessage().contains("Cannot convert String of length 3 to Character"));
	}


	@Test
	void testCreateConfigOnLoad() throws IOException {
		assertFalse(target.exists());
		Primitive loaded = new Primitive().load(target);
		assertNotNull(loaded);
		assertTrue(target.exists());
	}


	@Test
	void testFinalKeywordOnLoad() throws IOException {
		(new BlankHeader()).save(target.toString()); // First save a good one

		IOException thrown = assertThrows(IOException.class, () -> {
			new FinalKeyword().load(target.toString()); // And load it with the 'final' keyword one
		});
		assertTrue(thrown.getMessage().contains("'key' is final"));
	}


	@Test
	void testFinalKeywordOnSave() {
		IOException thrown = assertThrows(IOException.class, () -> {
			(new FinalKeyword()).save(target.toString());
		});
		assertTrue(thrown.getMessage().contains("'key' is final"));
	}


	@Test
	void testBlankHeader() throws IOException {
		(new BlankHeader()).save(target.toString());

		assertEquals("key: 1", new String(Files.readAllBytes(target.toPath())).trim());
	}


	@Test
	void testSaveAsNormalLoadAsFinal() throws IOException {
		(new BlankHeader()).save(target.toString());

		assertEquals("key: 1", new String(Files.readAllBytes(target.toPath())).trim());

		IOException thrown = assertThrows(IOException.class, () -> {
			(new FinalKeyword()).load(target);
		});
		assertTrue(thrown.getMessage().contains("'key' is final"));
	}


	@Test
	void testFloatNotDouble() throws IOException {
		(new Primitive()).save(target.toString());

		replaceInTarget(target, ": 1.0", ": 1.234567890123");

		IOException thrown = assertThrows(IOException.class, () -> {
			(new Primitive()).load(target.toString());
		});
		assertTrue(thrown.getMessage()
						 .contains("Double value 1.234567890123 cannot be precisely represented as a float"));
	}


	@Test
	void testYamlContainsMoreFields() throws IOException {
		HappyFlow file = new HappyFlow();
		file.streetNumber = 456;
		assertEquals(456, file.streetNumber);

		file.save(target.toString());

		replaceInTarget(target, "number: 456", "");

		HappyFlow loaded = new HappyFlow().load(target.toString());
		assertEquals(123, loaded.streetNumber);
	}

	@Test
	void testListAsStringNotEnum() throws IOException {

		ListYmlString file = new ListYmlString();
		file.key = Arrays.asList("b", "c");
		assertEquals(Arrays.asList("b", "c"), file.key);

		file.save(target);

		replaceInTarget(target, "c", "d");

		ListYmlString loaded = new ListYmlString().load(target);
		assertEquals(Arrays.asList("b", "d"), loaded.key);
	}

	@Test
	void testListAsInts() throws IOException {

		ListYmlInt file = new ListYmlInt();
		file.key = Arrays.asList(2, 3);
		assertEquals(Arrays.asList(2, 3), file.key);

		file.save(target);

		replaceInTarget(target, "3", "4");

		ListYmlInt loaded = new ListYmlInt().load(target);
		assertEquals(Arrays.asList(2, 4), loaded.key);
	}

	@Test
	void testCannotAssignNullToDouble() throws IOException {
		new Primitive().save(target);
		replaceInTarget(target, "double: 1.0", "");

		Primitive loaded = new Primitive().load(target);
		assertEquals(1.0, loaded.dbl);
	}
}
