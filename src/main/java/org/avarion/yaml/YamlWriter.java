package org.avarion.yaml;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Responsible for converting nested Java objects to YAML format.
 * Handles primitive building blocks: Maps, Collections, and scalar values.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class YamlWriter {
    private final YamlWrapper yamlWrapper;

    /**
     * Main entry point: converts a nested map to YAML string
     */
    public String write(Map<Object, Object> nestedMap) {
        StringBuilder result = new StringBuilder();
        writeValue(result, nestedMap, "", "");
        return result.toString();
    }

    /**
     * SINGLE DISPATCHER: Decides what type to write (Map, Collection, or scalar)
     * This is the ONLY place where we check the type of a value.
     */
    private void writeValue(StringBuilder yaml, Object value, String firstIndent, String indent) {
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
    private void writeMap(StringBuilder yaml, @NotNull Map<?, ?> map, String firstIndent, String indent) {
        boolean firstEntry = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            // Handle comments from NestedNode
            if (value instanceof NestedMap.NestedNode) {
                NestedMap.NestedNode node = (NestedMap.NestedNode) value;
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
    private void writeCollection(StringBuilder yaml, Collection<?> collection, String indent) {
        List<?> items = normalizeCollection(collection);
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
    private void writeScalar(@NotNull StringBuilder yaml, @Nullable Object value) {
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
     * Primitive building block: Normalize a collection to a sorted list
     * Converts Sets to Lists, sorting if elements are Comparable
     */
    private List<?> normalizeCollection(@NotNull Collection<?> collection) {
        if (!collection.isEmpty() && collection instanceof Set && collection.iterator().next() instanceof Comparable) {
            // Only re-order sets if their elements can be compared
            return collection.stream().sorted().collect(Collectors.toList());
        }

        return new ArrayList<>(collection);
    }

    /**
     * Primitive building block: Format a scalar value for YAML output
     */
    private String formatValue(@Nullable Object value) {
        String yamlContent = yamlWrapper.dump(value).trim();

        if (value instanceof Enum || value instanceof UUID) {
            // Remove the type tag: !!org.avarion.yaml.Material 'A' --> 'A'
            return yamlContent.replaceAll("^!!\\S+\\s+", "");
        }

        // Check for static field name (class or interface)
        Optional<String> originalName = getStaticFieldName(value);
        if (originalName.isPresent()) {
            return originalName.get();
        }

        return yamlContent;
    }

    /**
     * Helper: Find the name of a public static field that holds this value
     * Checks both class fields and interface constants
     */
    private static Optional<String> getStaticFieldName(@Nullable Object value) {
        try {
            Class<?> clazz = value.getClass();

            // First check the class's own declared fields
            Optional<String> classField = Arrays.stream(clazz.getDeclaredFields())
                         .filter(field -> Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()))
                         .filter(field -> {
                             try {
                                 field.setAccessible(true);
                                 return field.get(null)==value;
                             } catch (IllegalAccessException e) {
                                 return false;
                             }
                         })
                         .map(Field::getName)
                         .findFirst();

            if (classField.isPresent()) {
                return classField;
            }

            // Check interface fields (constants)
            return checkInterfaceFields(clazz, value);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Helper: Recursively check interface fields for a matching value
     */
    private static Optional<String> checkInterfaceFields(Class<?> clazz, Object value) {
        // Get all interfaces implemented by this class
        for (Class<?> iface : clazz.getInterfaces()) {
            // Check fields in this interface
            Optional<String> interfaceField = Arrays.stream(iface.getDeclaredFields())
                         .filter(field -> Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()))
                         .filter(field -> {
                             try {
                                 field.setAccessible(true);
                                 return field.get(null) == value;
                             } catch (IllegalAccessException e) {
                                 return false;
                             }
                         })
                         .map(Field::getName)
                         .findFirst();

            if (interfaceField.isPresent()) {
                return interfaceField;
            }

            // Recursively check parent interfaces
            Optional<String> parentInterfaceField = checkInterfaceFields(iface, value);
            if (parentInterfaceField.isPresent()) {
                return parentInterfaceField;
            }
        }

        // Also check superclass interfaces
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            return checkInterfaceFields(superclass, value);
        }

        return Optional.empty();
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
