package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Abstract class providing utility methods to handle YAML files, including
 * serialization and deserialization of Java objects.
 */
@SuppressWarnings({"unchecked", "unused"})
public abstract class YamlFileInterface {
	static final Object UNKNOWN = new Object();
	private static final Yaml yaml;

	static {
		DumperOptions options = new org.yaml.snakeyaml.DumperOptions();
		options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);

		yaml = new Yaml(options);
	}

	private static @Nullable Object getConvertedValue(@NotNull Field field, Object value) throws IOException {
		Class<?> expectedType = field.getType();

		if (value == null) {
			if (expectedType.isPrimitive()) {
				throw new IOException("Cannot assign null to primitive type "
									  + expectedType.getSimpleName()
									  + " (field: "
									  + field.getName()
									  + ")");
			}
			return null;
		}

		if (expectedType.isEnum() && value instanceof String) {
			return stringToEnum((Class<? extends Enum>) expectedType, (String) value);
		}

		if (value instanceof List<?>) {
			List<?> list = (List<?>) value;

			Type genericType = field.getGenericType();
			if (genericType instanceof ParameterizedType) {
				Class<?> elementType = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
				if (elementType.isEnum()) {
					return list.stream()
							   .map(item -> stringToEnum((Class<? extends Enum>) elementType, item.toString()))
							   .collect(Collectors.toList());
				}
			}
		}

		if (expectedType.isInstance(value)) {
			return value;
		}

		if (expectedType == boolean.class || expectedType == Boolean.class) {
			if (value instanceof Boolean) {
				return value;
			}

			String strValue = value.toString().toLowerCase().trim();
			return strValue.equals("true") || strValue.equals("yes") || strValue.equals("y") || strValue.equals("1");
		}

		if (Number.class.isAssignableFrom(value.getClass())) {
			Number numValue = (Number) value;
			if (expectedType == int.class || expectedType == Integer.class) {
				return numValue.intValue();
			}
			if (expectedType == double.class || expectedType == Double.class) {
				return numValue.doubleValue();
			}
			if (expectedType == float.class || expectedType == Float.class) {
				double doubleValue = numValue.doubleValue();
				if (Math.abs(doubleValue - (float) doubleValue) >= 1e-9) {
					throw new IOException("Double value "
										  + doubleValue
										  + " cannot be precisely represented as a float");
				}
				return numValue.floatValue();
			}
			if (expectedType == long.class || expectedType == Long.class) {
				return numValue.longValue();
			}
			if (expectedType == short.class || expectedType == Short.class) {
				return numValue.shortValue();
			}
			if (expectedType == byte.class || expectedType == Byte.class) {
				return numValue.byteValue();
			}
		}

		if ((expectedType == char.class || expectedType == Character.class)
			&& value instanceof String
			&& ((String) value).length() == 1)
		{
			String s = (String) value;
			return s.toCharArray()[0];
		}

		throw new IOException("Cannot convert "
							  + value.getClass().getSimpleName()
							  + " to "
							  + expectedType.getSimpleName());
	}

	private static <E extends Enum<E>> @NotNull E stringToEnum(Class<E> enumClass, @NotNull String value) {
		return Enum.valueOf(enumClass, value.toUpperCase());
	}

	/**
	 * Loads the YAML content from the specified file into this object.
	 * If the file doesn't exist, it creates a new file with the current object's content.
	 *
	 * @param file The File object representing the YAML file to load.
	 * @return The current object instance after loading the YAML content.
	 * @throws IOException If there's an error reading the file or parsing the YAML content.
	 *
	 *                     <pre>{@code
	 *                     MyConfig config = new MyConfig();
	 *                     config.load(new File("config.yml"));
	 *                     }</pre>
	 */
	public <T extends YamlFileInterface> T load(@NotNull File file) throws IOException {
		if (!file.exists()) {
			save(file);
			return (T) this;
		}

		Yaml yaml = new Yaml();
		Map<String, Object> data;

		try (FileInputStream inputStream = new FileInputStream(file)) {
			data = (Map<String, Object>) yaml.load(inputStream);
		}

		loadFields(this, data);
		return (T) this;
	}

	private void loadFields(@NotNull Object obj, Map<String, Object> data) throws IOException {
		for (Class<?> clazz = obj.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
			for (Field field : clazz.getDeclaredFields()) {
				YamlKey annotation = field.getAnnotation(YamlKey.class);
				if (annotation == null || annotation.value().trim().isEmpty()) {
					continue;
				}

				if (Modifier.isFinal(field.getModifiers())) {
					throw new IOException("'" + field.getName() + "' is final. Please adjust this!");
				}

				String key = annotation.value();
				Object value = getNestedValue(data, key);
				if (value == UNKNOWN) {
					continue;
				}

				field.setAccessible(true);
				try {
					field.set(obj, getConvertedValue(field, value));
				}
				catch (IllegalAccessException | IllegalArgumentException | NullPointerException e) {
					throw new IOException(e);
				}
			}
		}
	}

	/**
	 * Loads the YAML content from the specified file path into this object.
	 *
	 * @param file The path to the YAML file as a String.
	 * @param <T> The type of YamlFileInterface implementation.
	 * @return The current object instance after loading the YAML content.
	 * @throws IOException If there's an error reading the file or parsing the YAML content.
	 * @see #load(File)
	 *
	 * <pre>{@code
	 * MyConfig config = new MyConfig();
	 * config.load("config.yml");
	 * }</pre>
	 */
	public <T extends YamlFileInterface> T load(@NotNull String file) throws IOException {
		return load(new File(file));
	}

	private static Object getNestedValue(Map<String, Object> map, @NotNull String key) {
		String[] keys = key.split("\\.");

		Object current = map;
		for (String k : keys) {
			if (current instanceof Map) {
				current = ((Map<?, ?>) current).get(k);
			}
			else {
				return UNKNOWN;
			}
		}
		return current;
	}

	private @NotNull String buildYamlContents() throws IllegalAccessException {
		NestedMap nestedMap = new NestedMap();

		Class<?> clazz = this.getClass();

		// 1. file header
		StringBuilder result = new StringBuilder();
		YamlFile yamlFileAnnotation = clazz.getAnnotation(YamlFile.class);
		if (yamlFileAnnotation != null && !yamlFileAnnotation.header().trim().isEmpty()) {
			splitAndAppend(result, yamlFileAnnotation.header(), "", "# ");
			result.append("\n");  // Empty line after the header
		}

		// 2. fields
		for (Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);

			YamlKey key = field.getAnnotation(YamlKey.class);
			if (key == null || key.value().trim().isEmpty()) {
				continue;
			}

			if (Modifier.isFinal(field.getModifiers())) {
				throw new IllegalAccessException("'" + field.getName() + "' is final. Please adjust this!");
			}

			Object value = field.get(this);
			YamlComment comment = field.getAnnotation(YamlComment.class);

			nestedMap.put(key.value(), comment == null ? null : comment.value(), value);
		}

		// 3. Convert the nested map to YAML
		convertNestedMapToYaml(result, nestedMap.getMap(), 0);

		return result.toString();
	}

	private void splitAndAppend(final @NotNull StringBuilder yaml,
								final @Nullable String data,
								final @NotNull String indentStr,
								final @NotNull String extra)
	{
		if (data == null) {
			return;
		}

		for (String line : data.split("\\r?\\n")) {
			yaml.append(indentStr).append(extra).append(line.replace("\\s*$", "")).append("\n");
		}
	}

	private void convertNestedMapToYaml(final StringBuilder yaml,
										final @NotNull Map<String, Object> map,
										final int indent)
	{
		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			tmp.append("  ");
		}
		final String indentStr = tmp.toString();

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if (value instanceof NestedMap.NestedNode) {
				NestedMap.NestedNode node = (NestedMap.NestedNode) value;
				value = node.value;

				splitAndAppend(yaml, node.comment, indentStr, "# ");
			}

			yaml.append(indentStr).append(key).append(":");
			if (value instanceof Map) {
				yaml.append("\n");
				convertNestedMapToYaml(yaml, (Map<String, Object>) value, indent + 1);
			}
			else if (value instanceof List) {
				yaml.append("\n");
				for (Object item : (List<?>) value) {
					splitAndAppend(yaml, formatValue(item), indentStr + "  ", "- ");
				}
			}
			else {
				yaml.append(' ').append(formatValue(value)).append('\n');
			}

		}
	}

	private @NotNull String formatValue(Object value) {
		StringWriter writer = new StringWriter();
		yaml.dump(value, writer);
		String yamlContent = writer.toString().trim();

		if (value instanceof Enum) {
			// !!org.avarion.yaml.Material 'A' --> 'A'
			assert yamlContent.startsWith("!!");
			yamlContent = yamlContent.replaceAll("^!!\\S+\\s+", "");
		}

		return yamlContent;
	}

	/**
	 * Saves the current object's content to the specified file in YAML format.
	 *
	 * @param file The File object representing the YAML file to save to.
	 * @throws IOException If there's an error writing to the file.
	 *
	 * <pre>{@code
	 * MyConfig config = new MyConfig();
	 * config.save(new File("config.yml"));
	 * }</pre>
	 */
	public void save(@NotNull File file) throws IOException {
		file = file.getAbsoluteFile();
		file.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(file)) {
			writer.write(buildYamlContents());
		}
		catch (IllegalAccessException e) {
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Saves the current object's content to the specified file path in YAML format.
	 *
	 * @param target The path to the YAML file as a String.
	 * @throws IOException If there's an error writing to the file.
	 * @see #save(File)
	 *
	 * <pre>{@code
	 * MyConfig config = new MyConfig();
	 * config.save("config.yml");
	 * }</pre>
	 */
	public void save(@NotNull final String target) throws IOException {
		save(new File(target));
	}
}
