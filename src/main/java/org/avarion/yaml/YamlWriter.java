package org.avarion.yaml;

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
class YamlWriter {
    private static final Pattern GENERIC_TOSTRING_PATTERN =
        Pattern.compile("([a-zA-Z_][a-zA-Z0-9_.]*)\\.([A-Z][a-zA-Z0-9_]*)@([a-f0-9]+)");

    private final YamlWrapper yamlWrapper;

    YamlWriter(YamlWrapper yamlWrapper) {
        this.yamlWrapper = yamlWrapper;
    }

    /**
     * Main entry point: converts a nested map to YAML string
     */
    public String write(Map<Object, Object> nestedMap) {
        StringBuilder result = new StringBuilder();
        writeMap(result, nestedMap, 0);
        return result.toString();
    }

    /**
     * Primitive building block: Write a Map at a given indentation level
     */
    private void writeMap(StringBuilder yaml, @NotNull Map<?, ?> map, int indentLevel) {
        String indent = buildIndent(indentLevel);

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

            // Write the value based on its type
            if (value instanceof Map) {
                yaml.append("\n");
                writeMap(yaml, (Map<?, ?>) value, indentLevel + 1);
            } else if (value instanceof Collection) {
                yaml.append("\n");
                writeCollection(yaml, (Collection<?>) value, indentLevel);
            } else {
                // Scalar value
                yaml.append(' ').append(formatValue(value)).append('\n');
            }
        }
    }

    /**
     * Primitive building block: Write a Collection (List, Set, Queue, etc.)
     */
    private void writeCollection(StringBuilder yaml, Collection<?> collection, int indentLevel) {
        List<?> items = normalizeCollection(collection);
        String indent = buildIndent(indentLevel + 1);

        for (Object item : items) {
            if (item instanceof Map) {
                writeMapInList(yaml, (Map<?, ?>) item, indent);
            } else if (item instanceof Collection) {
                writeCollectionInList(yaml, (Collection<?>) item, indent);
            } else {
                // Scalar value as list item
                appendLines(yaml, formatValue(item), indent, "- ");
            }
        }
    }

    /**
     * Primitive building block: Write a Map as an item in a list
     */
    private void writeMapInList(StringBuilder yaml, Map<?, ?> map, String indent) {
        boolean firstKey = true;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (firstKey) {
                // First key gets "- " prefix
                yaml.append(indent).append("- ")
                    .append(entry.getKey()).append(": ")
                    .append(formatValue(entry.getValue())).append('\n');
                firstKey = false;
            } else {
                // Subsequent keys indented to align with first key's value
                yaml.append(indent).append("  ")
                    .append(entry.getKey()).append(": ")
                    .append(formatValue(entry.getValue())).append('\n');
            }
        }
    }

    /**
     * Primitive building block: Write a Collection as an item in a list
     */
    private void writeCollectionInList(StringBuilder yaml, Collection<?> collection, String indent) {
        List<?> items = normalizeCollection(collection);
        boolean firstItem = true;

        for (Object item : items) {
            if (firstItem) {
                // First item gets "- - " prefix (nested list marker)
                appendLines(yaml, formatValue(item), indent, "- - ");
                firstItem = false;
            } else {
                // Subsequent items get "- " prefix with extra indentation
                appendLines(yaml, formatValue(item), indent + "  ", "- ");
            }
        }
    }

    /**
     * Primitive building block: Normalize a collection to a sorted list
     * Converts Sets to Lists, sorting if elements are Comparable
     */
    private List<?> normalizeCollection(Collection<?> collection) {
        if (collection instanceof Set) {
            Set<?> set = (Set<?>) collection;
            // Only sort if elements are Comparable (e.g., String, Integer)
            if (!set.isEmpty() && set.iterator().next() instanceof Comparable) {
                return set.stream().sorted().collect(Collectors.toList());
            } else {
                return new ArrayList<>(set);
            }
        } else {
            // For List, Queue, or other Collection types, preserve order
            return new ArrayList<>(collection);
        }
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
