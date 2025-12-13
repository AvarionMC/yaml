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
        writeValue(result, nestedMap, "");
        return result.toString();
    }

    /**
     * SINGLE DISPATCHER: Decides what type to write (Map, Collection, or scalar)
     * This is the ONLY place where we check the type of a value.
     */
    private void writeValue(StringBuilder yaml, Object value, String indent) {
        if (value instanceof Map) {
            writeMap(yaml, (Map<?, ?>) value, indent);
        } else if (value instanceof Collection) {
            writeCollection(yaml, (Collection<?>) value, indent);
        } else {
            writeScalar(yaml, value);
        }
    }

    void addIfNotSame(@NotNull StringBuilder yaml, @Nullable Character charToAdd) {
        if (charToAdd == null) {
            return;
        }

        if (yaml.length() == 0 || yaml.charAt(yaml.length() - 1) != charToAdd) {
            yaml.append(charToAdd);
        }
    }

    /**
     * Primitive building block: Write a Map
     */
    private void writeMap(StringBuilder yaml, @NotNull Map<?, ?> map, String indent) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            // Handle comments from NestedNode
            if (value instanceof NestedMap.NestedNode) {
                NestedMap.NestedNode node = (NestedMap.NestedNode) value;
                appendComment(yaml, node.comment, indent);
                value = node.value;
            }

            yaml.append(indent).append(key).append(":\n");
            writeValue(yaml, value, indent + "  ");
        }
    }

    /**
     * Primitive building block: Write a Collection
     */
    private void writeCollection(StringBuilder yaml, Collection<?> collection, String indent) {
        List<?> items = normalizeCollection(collection);
        for (Object item : items) {
            yaml.append(indent).append("- ");
            writeValue(yaml, item, indent + "  ");
        }
    }

    /**
     * Primitive building block: Write a scalar value (just the formatted value, no newline)
     */
    private void writeScalar(@NotNull StringBuilder yaml, Object value) {
        if (yaml.charAt(yaml.length() - 1) == '\n') {
            yaml.deleteCharAt(yaml.length() - 1);
        }
        if (yaml.charAt(yaml.length() - 1) != ' ') {
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
