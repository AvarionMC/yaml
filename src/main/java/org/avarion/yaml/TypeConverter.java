package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

/**
 * Handles type conversion for YAML serialization/deserialization.
 * Converts between YAML values (strings, numbers, maps, collections) and Java types.
 */
@SuppressWarnings("unchecked")
final class TypeConverter {

    private static final Set<String> TRUE_VALUES = new HashSet<>(Arrays.asList("yes", "y", "true", "1"));

    private static final Map<Class<?>, Supplier<Collection<Object>>> COLLECTION_FACTORIES;

    static {
        COLLECTION_FACTORIES = new HashMap<>();
        COLLECTION_FACTORIES.put(Set.class, LinkedHashSet::new);
        COLLECTION_FACTORIES.put(List.class, ArrayList::new);
        COLLECTION_FACTORIES.put(Queue.class, ArrayDeque::new);
    }

    private TypeConverter() {
        // Utility class
    }

    // ==================== Main Entry Points ====================

    /**
     * Convert a value to the type specified by the field.
     */
    static @Nullable Object getConvertedValue(final @NotNull Field field, final Object value, boolean isLenient) throws IOException {
        return getConvertedValue(field, field.getType(), value, isLenient);
    }

    /**
     * Convert a value to the expected type with optional field context.
     */
    static @Nullable Object getConvertedValue(final @Nullable Field field, final @NotNull Class<?> expectedType, final Object value, boolean isLenient)
            throws IOException {
        if (value == null) {
            return handleNullValue(expectedType, field);
        }

        // If expected type is Object, return value as-is for collections and maps
        // since we don't have type information to guide conversion
        if (expectedType == Object.class && (value instanceof Collection || value instanceof Map)) {
            return value;
        }

        if (expectedType.isEnum() && value instanceof String convertedValue) {
            return stringToEnum((Class<? extends Enum>) expectedType, convertedValue);
        }

        if (value instanceof List<?>) {
            return handleCollectionValue(field, expectedType, (Collection<?>) value, isLenient);
        }
        if (Collection.class.isAssignableFrom(expectedType) && isLenient) {
            // We allow a single String/int/... to be assigned to a Collection -- but only when we're in lenient mode
            return handleCollectionValue(field, expectedType, List.of(value), isLenient);
        }

        if (value instanceof Map && Map.class.isAssignableFrom(expectedType)) {
            return handleMapValue(field, expectedType, (Map<?, ?>) value, isLenient);
        }

        if (expectedType.isInstance(value)) {
            return value;
        }

        if (value instanceof String convertedValue && expectedType.equals(UUID.class)) {
            return UUID.fromString(convertedValue);
        }

        if (isBooleanType(expectedType)) {
            return convertToBoolean(value);
        }

        if (Number.class.isAssignableFrom(value.getClass())) {
            return convertToNumber((Number) value, expectedType, isLenient);
        }

        if (isCharacterType(expectedType)) {
            return convertToCharacter(String.valueOf(value), isLenient);
        }

        // Handle Records: convert Map to Record using canonical constructor
        if (value instanceof Map && expectedType.isRecord()) {
            return convertMapToRecord(expectedType, (Map<?, ?>) value, isLenient);
        }

        // For other classes, attempt to use their constructor that takes a String parameter
        try {
            Constructor<?> constructor = expectedType.getConstructor(String.class);
            return constructor.newInstance(value.toString());
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            return getFieldValue(expectedType, value.toString());
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
        }

        throw new IOException("'" + expectedType.getSimpleName() + "': I cannot figure out how to retrieve this type.");
    }

    /**
     * Convert a value using Type information (handles both Class and ParameterizedType).
     * This method handles parameterized types (Maps/Collections with generic info).
     * For simple types, it delegates to getConvertedValue to avoid code duplication.
     */
    static @Nullable Object convertWithType(final @NotNull Type type, final Object value, boolean isLenient) throws IOException {
        Class<?> rawClass = getRawClass(type);

        if (value == null) {
            return handleNullValue(rawClass, null);
        }

        // Handle Maps with type information (only if type is parameterized)
        if (value instanceof Map && Map.class.isAssignableFrom(rawClass)) {
            Map<Object, Object> result = new LinkedHashMap<>();

            // Extract type arguments if this is a ParameterizedType
            Type keyType = Object.class;
            Type valueType = Object.class;
            if (type instanceof ParameterizedType convertedType) {
                Type[] typeArgs = convertedType.getActualTypeArguments();
                if (typeArgs.length > 0) keyType = typeArgs[0];
                if (typeArgs.length > 1) valueType = typeArgs[1];
            }

            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                Object convertedKey = convertWithType(keyType, entry.getKey(), isLenient);
                Object convertedValue = convertWithType(valueType, entry.getValue(), isLenient);
                result.put(convertedKey, convertedValue);
            }
            return result;
        }

        // Handle Collections with type information (only if type is parameterized)
        if (value instanceof Collection && Collection.class.isAssignableFrom(rawClass)) {
            Collection<Object> result = createCollectionInstance(rawClass);

            // Extract element type if this is a ParameterizedType
            Type elementType = Object.class;
            if (type instanceof ParameterizedType convertedType) {
                Type[] typeArgs = convertedType.getActualTypeArguments();
                if (typeArgs.length > 0) elementType = typeArgs[0];
            }

            for (Object item : (Collection<?>) value) {
                Object convertedItem = convertWithType(elementType, item, isLenient);
                result.add(convertedItem);
            }
            return result;
        }

        // For all other types (primitives, String, enums, UUID, numbers, chars, etc.),
        // delegate to getConvertedValue which has all the conversion logic in one place.
        return getConvertedValue(null, rawClass, value, isLenient);
    }

    // ==================== Collection Handling ====================

    /**
     * Convert the incoming value into a Set/List/Queue.
     */
    private static @NotNull Object handleCollectionValue(
            final @Nullable Field field, final @NotNull Class<?> expectedType, final @NotNull Collection<?> collection, boolean isLenient) throws IOException {

        Collection<Object> result = createCollectionInstance(expectedType);

        // Extract element type from Field's generic type if available
        Type elementType = Object.class;
        if (field != null && field.getGenericType() instanceof ParameterizedType convertedType) {
            Type[] typeArgs = convertedType.getActualTypeArguments();
            if (typeArgs.length > 0) {
                elementType = typeArgs[0];
            }
        }

        for (Object item : collection) {
            Object convertedValue = convertWithType(elementType, item, isLenient);
            result.add(convertedValue);
        }
        return result;
    }

    /**
     * Convert the incoming value into a Map with properly typed keys and values.
     */
    private static @NotNull Object handleMapValue(
            final @Nullable Field field, final @NotNull Class<?> expectedType, final Map<?, ?> map, boolean isLenient) throws IOException {

        Map<Object, Object> result = new LinkedHashMap<>();

        // Extract key/value types from Field's generic type if available
        Type keyType = Object.class;
        Type valueType = Object.class;
        if (field != null && field.getGenericType() instanceof ParameterizedType convertedType) {
            Type[] typeArgs = convertedType.getActualTypeArguments();
            if (typeArgs.length > 0) keyType = typeArgs[0];
            if (typeArgs.length > 1) valueType = typeArgs[1];
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object convertedKey = convertWithType(keyType, entry.getKey(), isLenient);
            Object convertedValue = convertWithType(valueType, entry.getValue(), isLenient);
            result.put(convertedKey, convertedValue);
        }
        return result;
    }

    static Collection<Object> createCollectionInstance(@NotNull Class<?> expectedType) throws IOException {
        Supplier<Collection<Object>> factory = COLLECTION_FACTORIES.entrySet()
                                                                   .stream()
                                                                   .filter(entry -> entry.getKey().isAssignableFrom(expectedType))
                                                                   .map(Map.Entry::getValue)
                                                                   .findFirst()
                                                                   .orElseThrow(() -> new IOException(
                                                                           "Unsupported collection type: " + expectedType.getSimpleName()));
        return factory.get();
    }

    // ==================== Record Handling ====================

    /**
     * Converts a Map to a Record instance by matching map keys to record component names.
     * Supports nested records: if a component is itself a record and the value is a Map,
     * it will recursively convert the nested Map to the nested record type.
     */
    private static @NotNull Object convertMapToRecord(final @NotNull Class<?> recordClass, final @NotNull Map<?, ?> map, boolean isLenient)
            throws IOException {
        RecordComponent[] components = recordClass.getRecordComponents();
        Object[] args = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            String componentName = component.getName();
            Class<?> componentType = component.getType();
            Type genericType = component.getGenericType();

            Object value = map.get(componentName);

            if (value == null) {
                // Handle null: primitives cannot be null
                if (componentType.isPrimitive()) {
                    throw new IOException("Cannot assign null to primitive record component '" + componentName +
                            "' in record " + recordClass.getSimpleName());
                }
                args[i] = null;
            }
            else if (value instanceof Map && componentType.isRecord()) {
                // Nested record: recursively convert
                args[i] = convertMapToRecord(componentType, (Map<?, ?>) value, isLenient);
            }
            else if (value instanceof Map && Map.class.isAssignableFrom(componentType)) {
                // Map field within record: use convertWithType for proper type handling
                args[i] = convertWithType(genericType, value, isLenient);
            }
            else if (value instanceof Collection && Collection.class.isAssignableFrom(componentType)) {
                // Collection field within record: use convertWithType for proper type handling
                args[i] = convertWithType(genericType, value, isLenient);
            }
            else {
                // Regular field: use getConvertedValue for type coercion
                args[i] = getConvertedValue(null, componentType, value, isLenient);
            }
        }

        try {
            // Get the canonical constructor (matches all record components in order)
            Class<?>[] paramTypes = Arrays.stream(components)
                                          .map(RecordComponent::getType)
                                          .toArray(Class<?>[]::new);
            Constructor<?> constructor = recordClass.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Failed to instantiate record " + recordClass.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    // ==================== Primitive Type Conversions ====================

    private static @Nullable Object handleNullValue(final @NotNull Class<?> expectedType, final Field field) throws IOException {
        if (expectedType.isPrimitive()) {
            String message = "Cannot assign null to primitive type " + expectedType.getSimpleName();
            if (field != null) {
                message += " (field: " + field.getName() + ")";
            }
            throw new IOException(message);
        }
        return null;
    }

    private static boolean isBooleanType(final Class<?> type) {
        return type == boolean.class || type == Boolean.class;
    }

    private static @NotNull Boolean convertToBoolean(final Object value) {
        if (value instanceof Boolean convertedValue) {
            return convertedValue;
        }

        final String strValue = value.toString().toLowerCase().trim();
        return TRUE_VALUES.contains(strValue);
    }

    private static Object convertToNumber(final Number numValue, final Class<?> expectedType, boolean isLenient) throws IOException {
        if (expectedType == int.class || expectedType == Integer.class) {
            return numValue.intValue();
        }
        if (expectedType == double.class || expectedType == Double.class) {
            return numValue.doubleValue();
        }
        if (expectedType == float.class || expectedType == Float.class) {
            return convertToFloat(numValue, isLenient);
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
        throw new IOException("Cannot convert " + numValue.getClass().getSimpleName() + " to " + expectedType.getSimpleName());
    }

    private static float convertToFloat(final @NotNull Number numValue, boolean isLenient) throws IOException {
        double doubleValue = numValue.doubleValue();
        if (!isLenient && Math.abs(doubleValue - (float) doubleValue) >= 1e-9) {
            throw new IOException("Double value " + doubleValue + " cannot be precisely represented as a float");
        }
        return numValue.floatValue();
    }

    private static boolean isCharacterType(final Class<?> type) {
        return type == char.class || type == Character.class;
    }

    private static @NotNull Character convertToCharacter(final @NotNull String value, boolean isLenient) throws IOException {
        if (value.length() == 1 || isLenient) {
            return value.charAt(0);
        }

        throw new IOException("Cannot convert String of length " + value.length() + " to Character");
    }

    private static <E extends Enum<E>> @NotNull E stringToEnum(final Class<E> enumClass, final @NotNull String value) {
        return Enum.valueOf(enumClass, value.toUpperCase());
    }

    // ==================== Reflection Utilities ====================

    /**
     * Extract the raw Class from a Type, handling both Class and ParameterizedType.
     */
    static Class<?> getRawClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType convertedType) {
            Type rawType = convertedType.getRawType();
            if (rawType instanceof Class<?> rawClass) {
                return rawClass;
            }
        }
        return Object.class;
    }

    /**
     * Look up a static field value by name (used for constants like Sound.ENTITY_PLAYER_HURT).
     */
    private static @Nullable Object getFieldValue(final @NotNull Class<?> expectedType, final String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field found = null;
        try {
            found = expectedType.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignored) {
        }

        if (found == null) {
            final String replacedName = fieldName.replace('.', '_');
            for (Field field : expectedType.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(fieldName) || field.getName().equalsIgnoreCase(replacedName)) {
                    found = field;
                    break;
                }
            }
        }

        if (found == null) {
            throw new NoSuchFieldException(fieldName);
        }

        found.setAccessible(true);
        return found.get(null);
    }
}
