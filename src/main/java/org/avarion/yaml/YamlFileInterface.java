package org.avarion.yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@SuppressWarnings("unchecked")
public abstract class YamlFileInterface {
	static final Object UNKNOWN = new Object();
	static final YAMLMapper mapper = new YAMLMapper();

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

	private static <E extends Enum<E>> @NotNull E stringToEnum(Class<E> enumClass, @NotNull String value)
			throws IllegalArgumentException
	{
		return Enum.valueOf(enumClass, value.toUpperCase());
	}

	@SuppressWarnings("unused")
	public static <T extends YamlFileInterface> @NotNull T load(@NotNull File target, @NotNull Class<T> clazz)
			throws IOException
	{
		T instance;
		try {
			instance = clazz.getDeclaredConstructor().newInstance();
		}
		catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new IOException(e.getMessage());
		}

		target = target.getAbsoluteFile();
		if (!target.exists()) {
			instance.save(target);
			return instance;
		}

		YAMLMapper mapper = new YAMLMapper();
		Map<String, Object> obj = mapper.readValue(target, Map.class);

		for (Field field : clazz.getDeclaredFields()) {
			YamlKey key = field.getAnnotation(YamlKey.class);
			if (key == null || key.value().trim().isEmpty()) {
				continue;
			}

			if (Modifier.isFinal(field.getModifiers())) {
				throw new IOException("Attribute '" + field.getName() + "' is final. We can't change its value!");
			}

			field.setAccessible(true);
			Object value = getNestedValue(obj, key.value());
			if (value == UNKNOWN) {
				continue;
			}

			try {
				field.set(instance, getConvertedValue(field, value));
			}
			catch (IllegalAccessException e) {
				throw new IOException(e.getMessage());
			}
		}

		instance.save(target);
		return instance;
	}

	@SuppressWarnings("unused")
	public static <T extends YamlFileInterface> @NotNull T load(@NotNull String target, @NotNull Class<T> clazz)
			throws IOException
	{
		return load(new File(target), clazz);
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
			for (String line : yamlFileAnnotation.header().split("\\r?\\n")) {
				result.append("# ").append(line.replace("\\s*$", "")).append("\n");
			}
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
				throw new IllegalAccessException("Attribute '"
												 + field.getName()
												 + "' is final. We can't change its value!");
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

	private void convertNestedMapToYaml(final StringBuilder yaml, final @NotNull Map<String, Object> map, int indent)
			throws IllegalAccessException
	{
		StringBuilder tmp = new StringBuilder();
		while (indent-- > 0) {
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
				splitAndAppend(yaml, formatValue(value), indentStr + "  ", "");
			}
			else {
				yaml.append(" ").append(formatValue(value)).append("\n");
			}
		}
	}

	private @NotNull String formatValue(Object value) throws IllegalAccessException {
		try {
			String yamlContent = mapper.writeValueAsString(value);
			if (yamlContent.startsWith("---")) {
				yamlContent = yamlContent.substring(3).trim();
			}
			return yamlContent;
		}
		catch (JsonProcessingException e) {
			throw new IllegalAccessException("Failed to serialize to YAML: " + e.getMessage());
		}
	}

	@SuppressWarnings("unused")
	public void save(@NotNull File target) throws IOException {
		target = target.getAbsoluteFile();
		target.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(target)) {
			writer.write(buildYamlContents());
		}
		catch (IllegalAccessException e) {
			throw new IOException(e.getMessage());
		}
	}

	@SuppressWarnings("unused")
	public void save(@NotNull String target) throws IOException {
		save(new File(target));
	}
}
