package org.avarion.yaml;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Handles type conversion for YAML serialization/deserialization.
 * Converts between YAML values (strings, numbers, maps, collections) and Java types.
 */
@SuppressWarnings("unchecked")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class TypeConverter {

    static final Logger LOG = Logger.getLogger(TypeConverter.class.getName());

    /**
     * Per-thread sink for lenient warnings. Always non-null — defaults to {@link #LOG}'s
     * {@code warning(String)} so callers don't need to null-check.
     * {@link YamlFileInterface#load(Object)} replaces it with the plugin's logger for the
     * duration of the load.
     */
    private static final ThreadLocal<Consumer<String>> ACTIVE = ThreadLocal.withInitial(() -> LOG::warning);

    static void warn(String message) {
        ACTIVE.get().accept(message);
    }

    /** Install {@code sink} for the current thread; returns the previous one for restore-in-finally. */
    static Consumer<String> pushSink(@NotNull Consumer<String> sink) {
        Consumer<String> prev = ACTIVE.get();
        ACTIVE.set(sink);
        return prev;
    }

    private static final Set<String> TRUE_VALUES = new HashSet<>(Arrays.asList("yes", "y", "true", "1"));

    /**
     * Sentinel returned by {@link #stringToEnum} when the YAML value is not a valid enum constant
     * AND lenient mode is active. Collection/map iterators check for this sentinel and skip the entry.
     */
    static final Object LENIENT_ENUM_SKIP = new Object();

    private static final Map<Class<?>, Supplier<Collection<Object>>> COLLECTION_FACTORIES;

    static {
        COLLECTION_FACTORIES = new HashMap<>();
        COLLECTION_FACTORIES.put(Set.class, LinkedHashSet::new);
        COLLECTION_FACTORIES.put(List.class, ArrayList::new);
        COLLECTION_FACTORIES.put(Queue.class, ArrayDeque::new);
    }

    /** How keys derived from a record component's name are spelled. */
    private final Naming naming;

    /** Whether a value that doesn't fit its target type is coerced with a warning, or rejected outright. */
    private final boolean isLenient;

    // ==================== Main Entry Points ====================

    /**
     * Convert a value to the type specified by the field.
     */
    @Nullable Object getConvertedValue(final @NotNull Field field, final Object value) throws IOException {
        return getConvertedValue(field, field.getType(), value);
    }

    /**
     * Convert a value to the expected type with optional field context.
     */
    @Nullable Object getConvertedValue(final @Nullable Field field, final @NotNull Class<?> expectedType, final Object value) throws IOException {
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
            return convertCollection(expectedType, genericTypeOf(field), (Collection<?>) value);
        }
        if (Collection.class.isAssignableFrom(expectedType) && isLenient) {
            // We allow a single String/int/... to be assigned to a Collection -- but only when we're in lenient mode
            return convertCollection(expectedType, genericTypeOf(field), List.of(value));
        }

        if (value instanceof Map && Map.class.isAssignableFrom(expectedType)) {
            return convertMap(genericTypeOf(field), (Map<?, ?>) value);
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
            return convertToNumber((Number) value, expectedType);
        }

        if (isCharacterType(expectedType)) {
            return convertToCharacter(String.valueOf(value));
        }

        // Handle Records: convert Map to Record using canonical constructor
        if (value instanceof Map && expectedType.isRecord()) {
            return convertMapToRecord(expectedType, (Map<?, ?>) value);
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
    @Nullable Object convertWithType(final @NotNull Type type, final Object value) throws IOException {
        Class<?> rawClass = getRawClass(type);

        if (value == null) {
            return handleNullValue(rawClass, null);
        }
        if (value instanceof Map<?, ?> map && Map.class.isAssignableFrom(rawClass)) {
            return convertMap(type, map);
        }
        if (value instanceof Collection<?> items && Collection.class.isAssignableFrom(rawClass)) {
            return convertCollection(rawClass, type, items);
        }

        // For all other types (primitives, String, enums, UUID, numbers, chars, etc.),
        // delegate to getConvertedValue which has all the conversion logic in one place.
        return getConvertedValue(null, rawClass, value);
    }

    // ==================== Collection & Map Handling ====================

    /**
     * The {@code index}-th type argument of a parameterized type, or {@code Object} when the type
     * isn't parameterized (a raw Map/Collection, or a field we have no generic information for).
     */
    private static @NotNull Type typeArgAt(final @Nullable Type type, final int index) {
        if (type instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (index < args.length) {
                return args[index];
            }
        }
        return Object.class;
    }

    /** The generic type of a field, or {@code null} when there is no field to read it from. */
    private static @Nullable Type genericTypeOf(final @Nullable Field field) {
        return field == null ? null : field.getGenericType();
    }

    /**
     * Convert the incoming value into a Set/List/Queue, taking the element type from
     * {@code genericType} and the concrete collection class from {@code targetType}.
     */
    private @NotNull Object convertCollection(final @NotNull Class<?> targetType, final @Nullable Type genericType, final @NotNull Collection<?> items)
            throws IOException {
        Collection<Object> result = createCollectionInstance(targetType);
        Type elementType = typeArgAt(genericType, 0);

        for (Object item : items) {
            Object converted = convertWithType(elementType, item);
            if (converted != LENIENT_ENUM_SKIP) {
                result.add(converted);
            }
        }
        return result;
    }

    /**
     * Convert the incoming value into a Map with properly typed keys and values.
     */
    private @NotNull Object convertMap(final @Nullable Type genericType, final @NotNull Map<?, ?> map) throws IOException {
        Map<Object, Object> result = new LinkedHashMap<>();
        Type keyType = typeArgAt(genericType, 0);
        Type valueType = typeArgAt(genericType, 1);

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object convertedKey = convertWithType(keyType, entry.getKey());
            Object convertedValue = convertWithType(valueType, entry.getValue());
            if (convertedKey != LENIENT_ENUM_SKIP && convertedValue != LENIENT_ENUM_SKIP) {
                result.put(convertedKey, convertedValue);
            }
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
     * Converts a Map to a Record instance by matching map keys to record component names,
     * or to the component's {@link YamlKey} when it carries one.
     * Supports nested records: if a component is itself a record and the value is a Map,
     * it will recursively convert the nested Map to the nested record type.
     */
    private @NotNull Object convertMapToRecord(final @NotNull Class<?> recordClass, final @NotNull Map<?, ?> map) throws IOException {
        RecordComponent[] components = recordClass.getRecordComponents();
        Object[] args = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            Object value = map.get(RecordComponents.keyOf(component, naming));

            if (value == null && component.getType().isPrimitive()) {
                throw new IOException("Cannot assign null to primitive record component '" + component.getName() +
                        "' in record " + recordClass.getSimpleName());
            }

            // convertWithType already routes nested records, maps and collections by their generic
            // type, so every non-null component takes the same road in.
            args[i] = value == null ? null : convertWithType(component.getGenericType(), value);

            // A record component cannot be skipped, so a lenient enum-skip becomes null
            if (args[i] == LENIENT_ENUM_SKIP) {
                args[i] = null;
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

    private Object convertToNumber(final Number numValue, final Class<?> expectedType) throws IOException {
        if (expectedType == int.class || expectedType == Integer.class) {
            return numValue.intValue();
        }
        if (expectedType == double.class || expectedType == Double.class) {
            return numValue.doubleValue();
        }
        if (expectedType == float.class || expectedType == Float.class) {
            return convertToFloat(numValue);
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

    private float convertToFloat(final @NotNull Number numValue) throws IOException {
        double doubleValue = numValue.doubleValue();
        boolean lossy = Math.abs(doubleValue - (float) doubleValue) >= 1e-9;
        if (lossy) {
            if (!isLenient) {
                throw new IOException("Double value " + doubleValue + " cannot be precisely represented as a float");
            }
            warn("Lenient mode: lossy conversion of double " + doubleValue + " to float " + (float) doubleValue);
        }
        return numValue.floatValue();
    }

    private static boolean isCharacterType(final Class<?> type) {
        return type == char.class || type == Character.class;
    }

    private @NotNull Character convertToCharacter(final @NotNull String value) throws IOException {
        if (value.length() == 1) {
            return value.charAt(0);
        }
        if (isLenient) {
            warn("Lenient mode: truncating String '" + value + "' (length " + value.length() + ") to first character");
            return value.charAt(0);
        }
        throw new IOException("Cannot convert String of length " + value.length() + " to Character");
    }

    /**
     * Convert a String to an enum constant. Under lenient mode an unknown name yields
     * {@link #LENIENT_ENUM_SKIP} instead of throwing; iterators in collection/map paths
     * use that sentinel to drop the offending entry.
     */
    private @NotNull Object stringToEnum(final Class<? extends Enum> enumClass, final @NotNull String value) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            if (!isLenient) throw ex;
            warn("Lenient mode: skipping unknown " + enumClass.getName() + " value '" + value + "'");
            return LENIENT_ENUM_SKIP;
        }
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
