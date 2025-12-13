package org.avarion.yaml;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Responsible for converting nested Java objects to YAML format.
 * Handles primitive building blocks: Maps, Collections, and scalar values.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class YamlWriter {
    private static final Pattern GENERIC_TOSTRING_PATTERN =
        Pattern.compile("([a-zA-Z_][a-zA-Z0-9_.]*)\\.([A-Z][a-zA-Z0-9_]*)@([a-f0-9]+)");

    private final YamlWrapper yamlWrapper;

    /**
     * Main entry point: converts a nested map to YAML string
     */
    public String write(Map<Object, Object> nestedMap) {
        StringBuilder result = new StringBuilder();
        writeValue(result, nestedMap, 0, false);
        return result.toString();
    }

    /**
     * SINGLE DISPATCHER: Decides what type to write (Map, Collection, or scalar)
     * This is the ONLY place where we check the type of a value.
     */
    private void writeValue(StringBuilder yaml, Object value, int indentLevel, boolean asListItem) {
        if (value instanceof Map) {
            writeMap(yaml, (Map<?, ?>) value, indentLevel, asListItem);
        } else if (value instanceof Collection) {
            writeCollection(yaml, (Collection<?>) value, indentLevel, asListItem);
        } else {
            writeScalar(yaml, value, indentLevel, asListItem);
        }
    }

    /**
     * Primitive building block: Write a Map
     * Handles both regular maps (key: value) and maps as list items (- key: value)
     */
    private void writeMap(StringBuilder yaml, @NotNull Map<?, ?> map, int indentLevel, boolean asListItem) {
        String indent = buildIndent(indentLevel);

        if (asListItem) {
            // Write map as a list item: - key1: val1\n  key2: val2
            boolean firstKey = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String prefix = firstKey ? "- " : "  ";
                yaml.append(indent).append(prefix)
                    .append(entry.getKey()).append(": ")
                    .append(formatValue(entry.getValue())).append('\n');
                firstKey = false;
            }
        } else {
            // Write map as regular entries: key: value
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();

                // Handle comments from NestedNode
                if (value instanceof NestedMap.NestedNode) {
                    NestedMap.NestedNode node = (NestedMap.NestedNode) value;
                    appendComment(yaml, node.comment, indent);
                    value = node.value;
                }

                // Write the key
                yaml.append(indent).append(key).append(":");

                // Decide how to write the value (inline vs newline)
                if (value instanceof Map || value instanceof Collection) {
                    yaml.append("\n");
                    writeValue(yaml, value, indentLevel + 1, false);
                } else {
                    yaml.append(' ').append(formatValue(value)).append('\n');
                }
            }
        }
    }

    /**
     * Primitive building block: Write a Collection
     * Handles both regular collections (- item) and collections as list items (- - item)
     */
    private void writeCollection(StringBuilder yaml, Collection<?> collection, int indentLevel, boolean asListItem) {
        List<?> items = normalizeCollection(collection);
        String indent = buildIndent(indentLevel + 1);

        if (asListItem) {
            // Write collection as a list item: - - item1\n  - item2
            boolean firstItem = true;
            for (Object item : items) {
                String prefix = firstItem ? "- - " : "- ";
                String itemIndent = firstItem ? indent : indent + "  ";
                appendLines(yaml, formatValue(item), itemIndent, prefix);
                firstItem = false;
            }
        } else {
            // Write collection as regular list: - item1\n- item2
            for (Object item : items) {
                // Each item is a list item, so dispatch with asListItem=true
                writeValue(yaml, item, indentLevel, true);
            }
        }
    }

    /**
     * Primitive building block: Write a scalar value
     */
    private void writeScalar(StringBuilder yaml, Object value, int indentLevel, boolean asListItem) {
        String indent = buildIndent(indentLevel + 1);
        String prefix = asListItem ? "- " : "";
        appendLines(yaml, formatValue(value), indent, prefix);
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
    private String formatValue(Object value) {
        String yamlContent = yamlWrapper.dump(value).trim();

        if (value instanceof Enum || value instanceof UUID) {
            // Remove the type tag: !!org.avarion.yaml.Material 'A' --> 'A'
            return yamlContent.replaceAll("^!!\\S+\\s+", "");
        } else {
            // Check for generic toString() pattern and try to find static field name
            Matcher matcher = GENERIC_TOSTRING_PATTERN.matcher(yamlContent);
            if (matcher.matches()) {
                Optional<String> originalName = getStaticFieldName(value);
                if (originalName.isPresent()) {
                    return originalName.get();
                }
            }
        }

        return yamlContent;
    }

    /**
     * Helper: Find the name of a public static field that holds this value
     */
    private static Optional<String> getStaticFieldName(Object value) {
        try {
            Class<?> clazz = value.getClass();

            return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers())
                    && Modifier.isPublic(field.getModifiers()))
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
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Primitive building block: Append a comment with proper indentation
     */
    private void appendComment(StringBuilder yaml, @Nullable String comment, String indent) {
        if (comment == null || comment.isEmpty()) {
            return;
        }
        appendLines(yaml, comment, indent, "# ");
    }

    /**
     * Primitive building block: Append multi-line content with prefix
     */
    private void appendLines(StringBuilder yaml, @Nullable String content, String indent, String prefix) {
        if (content == null) {
            return;
        }

        for (String line : content.split("\\r?\\n")) {
            yaml.append(indent).append(prefix)
                .append(line.replaceAll("\\s*$", ""))
                .append("\n");
        }
    }

    /**
     * Primitive building block: Build indentation string for a given level
     */
    private String buildIndent(int level) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++) {
            indent.append("  ");
        }
        return indent.toString();
    }
}
