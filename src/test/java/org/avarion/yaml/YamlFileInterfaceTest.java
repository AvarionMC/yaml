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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class YamlFileInterfaceTest {
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
	public void setUp() {
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
	public void testSaveAndLoad() throws IOException {
		YamlFileInterfaceTestImpl1 yamlFile = new YamlFileInterfaceTestImpl1();

		assertFalse(target.exists());
		yamlFile.save(target);
		assertTrue(target.exists());

		// Load the saved file and check if it contains expected content
		YamlFileInterfaceTestImpl1 loadedYamlFile = YamlFileInterface.load(
				target,
				YamlFileInterfaceTestImpl1.class
		);

		assertNotNull(loadedYamlFile);
		assertEquals("John Doe", loadedYamlFile.getName());
		assertEquals(30, loadedYamlFile.getAge());
		assertTrue(loadedYamlFile.isStudent());
		assertEquals(70.5, loadedYamlFile.getWeight());
		assertEquals(85.5f, loadedYamlFile.getScore());
		assertEquals(123456789L, loadedYamlFile.getId());
		assertTrue(loadedYamlFile.isMale());
		assertEquals("New York", loadedYamlFile.getCity());
		assertEquals(123, loadedYamlFile.getStreetNumber());
	}

	// Add more test cases as needed for other methods in YamlFileInterface
	@Test
	public void testSaveAndLoad2() throws IOException {
		YamlFileInterfaceTestImpl2 yamlFile = new YamlFileInterfaceTestImpl2();
		yamlFile.name1 = "1";
		yamlFile.name2 = "2";
		yamlFile.name3 = "3";

		assertFalse(target.exists());
		assertEquals("1", yamlFile.name1);
		assertEquals("2", yamlFile.name2);
		assertEquals("3", yamlFile.name3);

		yamlFile.save(target);
		assertTrue(target.exists());

		YamlFileInterfaceTestImpl2 loadedYamlFile = YamlFileInterface.load(
				target,
				YamlFileInterfaceTestImpl2.class
		);

		assertEquals("A", loadedYamlFile.name1);
		assertEquals("B", loadedYamlFile.name2);
		assertEquals("3", loadedYamlFile.name3);
	}

	@Test
	public void testEnumerations() throws IOException {
		final List<Material> def = List.of(Material.A, Material.B);

		ListMaterial yamlFile = new ListMaterial();

		assertFalse(target.exists());
		assertEquals(def, yamlFile.materials);
		assertEquals(Material.C, yamlFile.material);

		yamlFile.save(target);
		assertTrue(target.exists());

		ListMaterial loadedYamlFile = YamlFileInterface.load(target, ListMaterial.class);
		assertEquals(def, loadedYamlFile.materials);
		assertEquals(Material.C, loadedYamlFile.material);
	}

	@Test
	public void testEnumerationsInvalidEnumItem() throws IOException {
		(new ListMaterial()).save(target);
		replaceInTarget(target, "- \"B\"", "- \"D\"");

		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			YamlFileInterface.load(target, ListMaterial.class);
		});
		assertTrue(thrown.getMessage().contains("No enum constant org.avarion.yaml.Material.D"));
	}

	@Test
	public void testEnumerationsInvalidEnumItem2() throws IOException {
		(new ListMaterial()).save(target);
		replaceInTarget(target, "\"C\"", "2");

		IOException thrown = assertThrows(IOException.class, () -> {
			YamlFileInterface.load(target, ListMaterial.class);
		});
		assertTrue(thrown.getMessage().contains("Cannot convert Integer to Material"));
	}

	@Test
	public void testPrimitives() throws IOException {
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

		Primitive loaded = YamlFileInterface.load(target, Primitive.class);
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
	public void testNonPrimitives() throws IOException {
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
		replaceInTarget(target, "\"a\"", "\"b\"");

		NonPrimitive loaded = YamlFileInterface.load(target, NonPrimitive.class);

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
	public void testNullOnPrimitive() throws IOException {
		(new Primitive()).save(target);
		replaceInTarget(target, "1", "null");

		IOException thrown = assertThrows(IOException.class, () -> {
			YamlFileInterface.load(target, Primitive.class);
		});
		assertTrue(thrown.getMessage().contains("Cannot assign null to primitive type byte (field: bt)"));
	}

	@Test
	public void testNullOnNonPrimitive() throws IOException {
		(new NonPrimitive()).save(target);

		replaceInTarget(target, "\"a\"", "null");
		replaceInTarget(target, "1.0", "null");
		replaceInTarget(target, "1", "null");
		replaceInTarget(target, "true", "null");

		NonPrimitive loaded = YamlFileInterface.load(target, NonPrimitive.class);
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
	public void testDifferentBooleanValues(final String val) throws IOException {
		(new Primitive()).save(target);

		replaceInTarget(target, "true", val);

		NonPrimitive loaded = YamlFileInterface.load(target, NonPrimitive.class);

		assertTrue(loaded.bln);
	}

	@Test
	public void testDoubleKeyUsage() throws IOException {
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
			(new DoubleKeyUsage()).save(target);
		});
		assertTrue(thrown.getMessage().contains("key1 is already used before"));
	}

	@Test
	public void testEmptyKey() throws IOException {
		(new EmptyKey()).save(target);

		assertEquals(0, target.length());
	}

	@Test
	public void testInvalidFloatAsDouble() throws IOException {
		final double value = 1.23456789;

		(new NonPrimitive()).save(target);
		replaceInTarget(target, "1.0", Double.toString(value));

		IOException thrown = assertThrows(IOException.class, () -> {
			YamlFileInterface.load(target, NonPrimitive.class);
		});
		assertTrue(thrown.getMessage().contains("Double value 1.23456789 cannot be precisely represented as a float"));
	}

	@Test
	public void testIntAsDoubleValue() throws IOException {
		(new NonPrimitive()).save(target);
		replaceInTarget(target, "1.0", "2");

		NonPrimitive loaded = YamlFileInterface.load(target.toString(), NonPrimitive.class);
		assertEquals(2.0f, loaded.flt);
		assertEquals(2.0d, loaded.dbl);
	}

	@Test
	public void testWrongChar() throws IOException {
		(new NonPrimitive()).save(target);
		replaceInTarget(target, "\"a\"", "2"); // Now it's an integer

		IOException thrown = assertThrows(IOException.class, () -> {
			YamlFileInterface.load(target, NonPrimitive.class);
		});
		assertTrue(thrown.getMessage().contains("Cannot convert Integer to Character"));
	}


	@Test
	public void testWrongChar2() throws IOException {
		(new NonPrimitive()).save(target);
		replaceInTarget(target, "\"a\"", "\"abc\""); // Now it's a string

		IOException thrown = assertThrows(IOException.class, () -> {
			YamlFileInterface.load(target, NonPrimitive.class);
		});
		assertTrue(thrown.getMessage().contains("Cannot convert String to Character"));
	}


	@Test
	public void testCreateConfigOnLoad() throws IOException {
		assertFalse(target.exists());
		Primitive loaded = YamlFileInterface.load(target.toString(), Primitive.class);
		assertNotNull(loaded);
		assertTrue(target.exists());
	}


	@Test
	public void testFinalKeywordOnLoad() throws IOException {
		(new BlankHeader()).save(target.toString());

		IOException thrown = assertThrows(IOException.class, () -> {
			YamlFileInterface.load(target.toString(), FinalKeyword.class);
		});
		assertTrue(thrown.getMessage().contains("Attribute 'key' is final"));
	}


	@Test
	public void testFinalKeywordOnSave() throws IOException {
		IOException thrown = assertThrows(IOException.class, () -> {
			(new FinalKeyword()).save(target.toString());
		});
		assertTrue(thrown.getMessage().contains("Attribute 'key' is final"));
	}


	@Test
	public void testBlankHeader() throws IOException {
		(new BlankHeader()).save(target.toString());

		assertEquals("key: 1", Files.readString(target.toPath()).trim());
	}


	@Test
	public void testSaveAsNormalLoadAsFinal() throws IOException {
		(new BlankHeader()).save(target.toString());

		assertEquals("key: 1", Files.readString(target.toPath()).trim());
	}


	@Test
	public void testNoDefaultConstructor() throws IOException {
		(new NoDefaultConstructor(123)).save(target.toString());

		assertEquals("key: 123", Files.readString(target.toPath()).trim());

		IOException thrown = assertThrows(IOException.class, () -> {
			YamlFileInterface.load(target.toString(), NoDefaultConstructor.class);
		});
		assertTrue(thrown.getMessage().contains("org.avarion.yaml.NoDefaultConstructor.<init>()"));
	}
}
