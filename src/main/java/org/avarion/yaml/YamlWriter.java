package org.avarion.yaml;

import lombok.AccessLevel;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for converting nested Java objects to YAML format.
 * Handles primitive building blocks: Maps, Collections, and scalar values.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class YamlWriter {
    private static final Pattern GENERIC_TOSTRING_PATTERN = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_.]*)\\.([A-Z][a-zA-Z0-9_$]*)@([a-f0-9]+)");

    /** Cached at class load: the Bukkit Keyed interface if it's on the classpath, otherwise null. */
    private static final @Nullable Class<?> KEYED_INTERFACE = loadOptional("org.bukkit.Keyed");

    private final YamlWrapper yamlWrapper;

    /** Reflectively load a class by name, returning {@code null} when it's not on the classpath. */
    static @Nullable Class<?> loadOptional(@NotNull String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * If {@code value} implements the supplied Bukkit Keyed interface, format it as
     * {@code NAMESPACE_KEY}; otherwise return {@code null} so the caller can fall through.
     * The {@code keyedClass} parameter is plumbed through (rather than read from the cached
     * static) so test code can drive the {@code keyedClass == null} branch directly.
     */
    static @Nullable String tryFormatAsKeyed(@Nullable Class<?> keyedClass, @NotNull Object value) throws IOException {
        if (keyedClass == null || !keyedClass.isInstance(value)) {
            return null;
        }
        try {
            Method getKeyMethod = value.getClass().getMethod("key");
            Object namespacedKey = getKeyMethod.invoke(value);
            Method getKeyStringMethod = namespacedKey.getClass().getMethod("value");
            String key = (String) getKeyStringMethod.invoke(namespacedKey);
            return key.toUpperCase(Locale.ENGLISH).replace('.', '_');
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Failed to get key from Keyed object", e);
        }
    }

    /**
     * Main entry point: converts a nested map to YAML string
     */
    public String write(Map<Object, Object> nestedMap) throws IOException {
        StringBuilder result = new StringBuilder();
        writeValue(result, nestedMap, "", "");
        return result.toString();
    }

    /**
     * SINGLE DISPATCHER: Decides what type to write (Map, Collection, or scalar)
     * This is the ONLY place where we check the type of a value.
     */
    private void writeValue(StringBuilder yaml, Object value, String firstIndent, String indent) throws IOException {
        // Handle Records: convert to Map for YAML representation
        if (value != null && value.getClass().isRecord()) {
            value = recordToMap(value);
        }

        if (value instanceof Map) {
            writeMap(yaml, (Map<?, ?>) value, firstIndent, indent);
        }
        else if (value instanceof Collection) {
            writeCollection(yaml, (Collection<?>) value, indent);
        }
        else {
            writeScalar(yaml, value);
        }
    }

    /**
     * Primitive building block: Write a Map
     */
    private void writeMap(StringBuilder yaml, @NotNull Map<?, ?> map, String firstIndent, String indent) throws IOException {
        boolean firstEntry = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            // Handle comments from NestedNode
            if (value instanceof NestedMap.NestedNode node) {
                appendComment(yaml, node.comment, indent);
                value = node.value;
            }
            yaml.append(firstEntry ? firstIndent:indent);
            firstEntry = false;

            yaml.append(key).append(":\n");
            writeValue(yaml, value, indent + "  ", indent + "  ");
        }
    }

    /**
     * Primitive building block: Write a Collection
     */
    private void writeCollection(StringBuilder yaml, Collection<?> collection, String indent) throws IOException {
        List<?> items = normalizeCollection(collection);

        // Handle empty collections: write [] inline (not as a quoted string)
        if (items.isEmpty()) {
            // Remove trailing newline if present, add space and []
            if (yaml.charAt(yaml.length() - 1) == '\n') {
                yaml.deleteCharAt(yaml.length() - 1);
            }
            if (yaml.charAt(yaml.length() - 1) != ' ') {
                yaml.append(' ');
            }
            yaml.append("[]\n");
            return;
        }

        for (Object item : items) {
            if (!yaml.subSequence(yaml.length() - 2, yaml.length()).equals("- ")) {
                yaml.append(indent);
            }
            yaml.append("- ");
            writeValue(yaml, item, "", indent + "  ");
        }
    }

    /**
     * Primitive building block: Write a scalar value (just the formatted value, no newline)
     */
    private void writeScalar(@NotNull StringBuilder yaml, @Nullable Object value) throws IOException {
        if (yaml.charAt(yaml.length() - 1)=='\n') {
            yaml.deleteCharAt(yaml.length() - 1);
        }
        if (yaml.charAt(yaml.length() - 1)!=' ') {
            yaml.append(' ');
        }
        yaml.append(formatValue(value));
        yaml.append('\n');
    }

    /**
     * Converts a Record to a Map by extracting all component values.
     * Handles nested records by recursively converting them to Maps.
     */
    private Map<String, Object> recordToMap(@NotNull Object potentialRecord) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        RecordComponent[] components = potentialRecord.getClass().getRecordComponents();

        for (RecordComponent component : components) {
            String name = component.getName();
            try {
                Method accessor = component.getAccessor();
                accessor.setAccessible(true);
                Object value = accessor.invoke(potentialRecord);

                // Recursively convert nested records
                if (value != null && value.getClass().isRecord()) {
                    value = recordToMap(value);
                }

                result.put(name, value);
            }
            catch (IllegalAccessException | InvocationTargetException e) {
                throw new IOException("Failed to access record component '" + name + "': " + e.getMessage(), e);
            }
        }

        return result;
    }

    /**
     * Primitive building block: Normalize a collection to a sorted list
     * Converts Sets to Lists, sorting if elements are Comparable
     */
    @Contract("_ -> new")
    private @NotNull List<?> normalizeCollection(@NotNull Collection<?> collection) {
        if (!collection.isEmpty() && collection instanceof Set && collection.iterator().next() instanceof Comparable) {
            // Only re-order sets if their elements can be compared
            collection = collection.stream().sorted().toList();
        }

        return new ArrayList<>(collection);
    }

    /**
     * Primitive building block: Format a scalar value for YAML output
     */
    private String formatValue(@Nullable Object value) throws IOException {
        if (value==null) {
            return "null";
        }

        String keyed = tryFormatAsKeyed(KEYED_INTERFACE, value);
        if (keyed != null) {
            return keyed;
        }

        String yamlContent = yamlWrapper.dump(value).trim();

        if (value instanceof Enum || value instanceof UUID) {
            // Remove the type tag: !!org.avarion.yaml.Material 'A' --> 'A'
            return yamlContent.replaceAll("^!!\\S+\\s+", "");
        }

        // Check for generic toString() pattern and try to find static field name
        Matcher matcher = GENERIC_TOSTRING_PATTERN.matcher(yamlContent);
        if (matcher.matches()) {
            Optional<String> originalName = getStaticFieldName(value);
            if (originalName.isPresent()) {
                return originalName.get();
            }
        }

        return yamlContent;
    }

    /**
     * Helper: Find the name of a public static field that holds this value.
     * Caller (formatValue) already guarantees {@code value} is non-null.
     */
    private static Optional<String> getStaticFieldName(@NotNull Object value) {
        for (Field field : value.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())
                    && Modifier.isPublic(field.getModifiers())
                    && readStatic(field) == value) {
                return Optional.of(field.getName());
            }
        }
        return Optional.empty();
    }

    /** Read a public static field's value. {@code @SneakyThrows} hides the unreachable IAE
     *  (the caller already filtered for public). {@code @Generated} so JaCoCo skips the
     *  synthetic Lombok rewrap that's structurally untestable here. */
    @Generated
    @SneakyThrows
    private static Object readStatic(@NotNull Field field) {
        return field.get(null);
    }

    /**
     * Primitive building block: Append a comment with proper indentation
     */
    private void appendComment(StringBuilder yaml, @Nullable String comment, String indent) {
        if (comment==null || comment.isEmpty()) {
            return;
        }

        for (String line : comment.split("\\r?\\n")) {
            yaml.append(indent).append("# ").append(line.replaceAll("\\s*$", "")).append("\n");
        }
    }
}
