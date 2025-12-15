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
    // Track static field instances and their source (declaring class + field name)
    private static final WeakHashMap<Object, FieldSource> STATIC_FIELD_REGISTRY = new WeakHashMap<>();

    private final YamlWrapper yamlWrapper;

    static class FieldSource {
        final Class<?> declaringClass;
        final String fieldName;

        FieldSource(Class<?> declaringClass, String fieldName) {
            this.declaringClass = declaringClass;
            this.fieldName = fieldName;
        }
    }

    /**
     * Register a static field value so it can be looked up during serialization
     */
    static void registerStaticField(Object value, Class<?> declaringClass, String fieldName) {
        if (value != null) {
            STATIC_FIELD_REGISTRY.put(value, new FieldSource(declaringClass, fieldName));
        }
    }

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
        String yamlContent = yamlWrapper.dump(value).trim();

        if (value instanceof Enum || value instanceof UUID) {
            // Remove the type tag: !!org.avarion.yaml.Material 'A' --> 'A'
            return yamlContent.replaceAll("^!!\\S+\\s+", "");
        }

        // Check for static field name (class or interface)
        Optional<String> originalName = getStaticFieldName(value, declaredType);
        if (originalName.isPresent()) {
            return originalName.get();
        }

        return yamlContent;
    }

    /**
     * Helper: Find the name of a public static field that holds this value
     * Checks both class fields and interface constants
     */
    private static Optional<String> getStaticFieldName(@Nullable Object value, @Nullable Class<?> declaredType) {
        if (value == null) return Optional.empty();

        try {
            // First check the registry for tracked static fields
            FieldSource source = STATIC_FIELD_REGISTRY.get(value);
            if (source != null) {
                return Optional.of(source.fieldName);
            }

            Class<?> clazz = value.getClass();

            // Check the class's own declared fields
            Optional<String> classField = checkClassFields(clazz, value);
            if (classField.isPresent()) {
                return classField;
            }

            // Check interface fields (constants) of the runtime class
            Optional<String> interfaceField = checkInterfaceFields(clazz, value);
            if (interfaceField.isPresent()) {
                return interfaceField;
            }

            // If we have a declared type, search for static fields
            if (declaredType != null) {
                return searchByDeclaredType(value, declaredType);
            }

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<String> checkClassFields(Class<?> clazz, Object value) {
        return Arrays.stream(clazz.getDeclaredFields())
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
    }

    /**
     * Search for static fields by checking the declared type and registering any found
     */
    private static Optional<String> searchByDeclaredType(Object value, Class<?> declaredType) {
        // Check the declared type itself for static fields
        Optional<String> directField = checkAndRegisterClassFields(declaredType, value);
        if (directField.isPresent()) {
            return directField;
        }

        // Check interfaces of the declared type
        Optional<String> interfaceField = checkInterfaceFields(declaredType, value);
        if (interfaceField.isPresent()) {
            return interfaceField;
        }

        return Optional.empty();
    }

    /**
     * Check class fields and register if found
     */
    private static Optional<String> checkAndRegisterClassFields(Class<?> clazz, Object value) {
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(null);

                    // Register this static field for future lookups
                    if (fieldValue != null) {
                        registerStaticField(fieldValue, clazz, field.getName());
                    }

                    if (fieldValue == value) {
                        return Optional.of(field.getName());
                    }
                } catch (IllegalAccessException e) {
                    // Continue checking other fields
                }
            }
        }
        return Optional.empty();
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
