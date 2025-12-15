package org.avarion.yaml;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
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
        writeValue(yaml, value, firstIndent, indent, null);
    }

    private void writeValue(StringBuilder yaml, Object value, String firstIndent, String indent, Class<?> declaredType) {
        if (value instanceof Map) {
            writeMap(yaml, (Map<?, ?>) value, firstIndent, indent);
        }
        else if (value instanceof Collection) {
            writeCollection(yaml, (Collection<?>) value, indent);
        }
        else {
            writeScalar(yaml, value, declaredType);
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
            Class<?> declaredType = null;

            // Handle comments from NestedNode
            if (value instanceof NestedMap.NestedNode) {
                NestedMap.NestedNode node = (NestedMap.NestedNode) value;
                appendComment(yaml, node.comment, indent);
                value = node.value;
                declaredType = node.declaredType;
            }
            yaml.append(firstEntry ? firstIndent:indent);
            firstEntry = false;

            yaml.append(key).append(":\n");
            writeValue(yaml, value, indent + "  ", indent + "  ", declaredType);
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
    private void writeScalar(@NotNull StringBuilder yaml, @Nullable Object value, @Nullable Class<?> declaredType) {
        if (yaml.charAt(yaml.length() - 1)=='\n') {
            yaml.deleteCharAt(yaml.length() - 1);
        }
        if (yaml.charAt(yaml.length() - 1)!=' ') {
            yaml.append(' ');
        }
        yaml.append(formatValue(value, declaredType));
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
    private String formatValue(@Nullable Object value, @Nullable Class<?> declaredType) {
        if (value == null) {
            return yamlWrapper.dump(null).trim();
        }

        // Check if it's a Bukkit Keyed object (via reflection to avoid hard dependency)
        try {
            // Check if value implements Keyed interface
            Class<?> keyedInterface = Class.forName("org.bukkit.Keyed");
            if (keyedInterface.isInstance(value)) {
                // Call getKey() to get NamespacedKey
                Method getKeyMethod = value.getClass().getMethod("getKey");
                Object namespacedKey = getKeyMethod.invoke(value);

                // Call getKey() on NamespacedKey to get the string key
                Method getKeyStringMethod = namespacedKey.getClass().getMethod("getKey");
                String key = (String) getKeyStringMethod.invoke(namespacedKey);

                // Replace dots with underscores
                return key.replace('.', '_');
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 java.lang.reflect.InvocationTargetException e) {
            // Keyed interface not available or reflection failed, continue with normal handling
        }

        String yamlContent = yamlWrapper.dump(value).trim();

        if (value instanceof Enum || value instanceof UUID) {
            // Remove the type tag: !!org.avarion.yaml.Material 'A' --> 'A'
            return yamlContent.replaceAll("^!!\\S+\\s+", "");
        }

        return yamlContent;
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
