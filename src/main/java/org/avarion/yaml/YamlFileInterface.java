package org.avarion.yaml;

import org.avarion.yaml.exceptions.DuplicateKey;
import org.avarion.yaml.exceptions.FinalAttribute;
import org.avarion.yaml.exceptions.YamlException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Abstract class providing utility methods to handle YAML files, including
 * serialization and deserialization of Java objects.
 */
@SuppressWarnings("unchecked")
public abstract class YamlFileInterface {
    static final Object UNKNOWN = new Object();
    private static final YamlWrapper yaml = YamlWrapperFactory.create();
    private static final Set<String> TRUE_VALUES = new HashSet<>(Arrays.asList("yes", "y", "true", "1"));

    private static final Map<Class<?>, Supplier<Collection<Object>>> COLLECTION_FACTORIES;

    static {
        COLLECTION_FACTORIES = new HashMap<>();
        COLLECTION_FACTORIES.put(Set.class, LinkedHashSet::new);
        COLLECTION_FACTORIES.put(List.class, ArrayList::new);
        COLLECTION_FACTORIES.put(Queue.class, ArrayDeque::new);
    }

    private static @Nullable Object getConvertedValue(final @NotNull Field field, final Object value, boolean isLenient) throws IOException {
        return getConvertedValue(field, field.getType(), value, isLenient);
    }

    private static @Nullable Object getFieldValue(final @NotNull Class<?> expectedType, final String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field found = null;
        try {
            found = expectedType.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignored) {
        }

        if (found==null) {
            final String replacedName = fieldName.replace('.', '_');
            for (Field field : expectedType.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(fieldName) || field.getName().equalsIgnoreCase(replacedName)) {
                    found = field;
                    break;
                }
            }
        }

        if (found==null) {
            throw new NoSuchFieldException(fieldName);
        }

        found.setAccessible(true);
        return found.get(null);
    }

    private static @Nullable Object getConvertedValue(final @Nullable Field field, final @NotNull Class<?> expectedType, final Object value, boolean isLenient)
            throws IOException {
        if (value==null) {
            return handleNullValue(expectedType, field);
        }

        if (expectedType.isEnum() && value instanceof String) {
            return stringToEnum((Class<? extends Enum>) expectedType, (String) value);
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

        if (value instanceof String && expectedType.equals(UUID.class)) {
            return UUID.fromString((String) value);
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

    private static @Nullable Object handleNullValue(final @NotNull Class<?> expectedType, final Field field) throws IOException {
        if (expectedType.isPrimitive()) {
            String message = "Cannot assign null to primitive type " + expectedType.getSimpleName();
            if (field!=null) {
                message += " (field: " + field.getName() + ")";
            }
            throw new IOException(message);
        }
        return null;
    }

    /**
     * Extract the raw Class from a Type, handling both Class and ParameterizedType
     */
    private static Class<?> getRawClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            if (rawType instanceof Class<?>) {
                return (Class<?>) rawType;
            }
        }
        return Object.class;
    }

    /**
     * Convert the incoming value into a Set/List
     */
    private static @NotNull Object handleCollectionValue(
            final @Nullable Field field, final @NotNull Class<?> expectedType, final @NotNull Collection<?> collection, boolean isLenient) throws IOException {

        Collection<Object> result = createCollectionInstance(expectedType);

        // Extract element type from Field's generic type if available
        Type elementType = Object.class;
        if (field != null && field.getGenericType() instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
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
     * Convert the incoming value into a Map with properly typed keys and values
     */
    private static @NotNull Object handleMapValue(
            final @Nullable Field field, final @NotNull Class<?> expectedType, final Map<?, ?> map, boolean isLenient) throws IOException {

        Map<Object, Object> result = new LinkedHashMap<>();

        // Extract key/value types from Field's generic type if available
        Type keyType = Object.class;
        Type valueType = Object.class;
        if (field != null && field.getGenericType() instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
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

    /**
     * Convert a value using Type information (handles both Class and ParameterizedType)
     * This method ONLY handles parameterized types (Maps/Collections with generic info).
     * For simple types, it delegates to getConvertedValue to avoid code duplication.
     */
    private static @Nullable Object convertWithType(final @NotNull Type type, final Object value, boolean isLenient) throws IOException {
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
            if (type instanceof ParameterizedType) {
                Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
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
            if (type instanceof ParameterizedType) {
                Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
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
        // This avoids code duplication.
        return getConvertedValue(null, rawClass, value, isLenient);
    }

    private static Collection<Object> createCollectionInstance(@NotNull Class<?> expectedType) throws IOException {
        Supplier<Collection<Object>> factory = COLLECTION_FACTORIES.entrySet()
                                                                   .stream()
                                                                   .filter(entry -> entry.getKey().isAssignableFrom(expectedType))
                                                                   .map(Map.Entry::getValue)
                                                                   .findFirst()
                                                                   .orElseThrow(() -> new IOException(
                                                                           "Unsupported collection type: " + expectedType.getSimpleName()));
        return factory.get();
    }

    private static boolean isBooleanType(final Class<?> type) {
        return type==boolean.class || type==Boolean.class;
    }

    private static @NotNull Boolean convertToBoolean(final Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        final String strValue = value.toString().toLowerCase().trim();
        return TRUE_VALUES.contains(strValue);
    }

    private static Object convertToNumber(final Number numValue, final Class<?> expectedType, boolean isLenient) throws IOException {
        if (expectedType==int.class || expectedType==Integer.class) {
            return numValue.intValue();
        }
        if (expectedType==double.class || expectedType==Double.class) {
            return numValue.doubleValue();
        }
        if (expectedType==float.class || expectedType==Float.class) {
            return convertToFloat(numValue, isLenient);
        }
        if (expectedType==long.class || expectedType==Long.class) {
            return numValue.longValue();
        }
        if (expectedType==short.class || expectedType==Short.class) {
            return numValue.shortValue();
        }
        if (expectedType==byte.class || expectedType==Byte.class) {
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
        return type==char.class || type==Character.class;
    }

    private static @NotNull Character convertToCharacter(final @NotNull String value, boolean isLenient) throws IOException {
        if (value.length()==1 || isLenient) {
            return value.charAt(0);
        }

        throw new IOException("Cannot convert String of length " + value.length() + " to Character");
    }

    private static <E extends Enum<E>> @NotNull E stringToEnum(final Class<E> enumClass, final @NotNull String value) {
        return Enum.valueOf(enumClass, value.toUpperCase());
    }

    /**
     * Loads the YAML content from the specified file into this object.
     * If the file doesn't exist, it creates a new file with the current object's content.
     *
     * @param file The File object representing the YAML file to load.
     * @return The current object instance after loading the YAML content.
     * @throws IOException If there's an error reading the file or parsing the YAML content.
     *
     * <pre>{@code
     * MyConfig config = new MyConfig();
     * config.load(new File("config.yml"));
     * }</pre>
     */
    public <T extends YamlFileInterface> T load(final @NotNull File file) throws IOException {
        if (!file.exists()) {
            save(file);
            return (T) this;
        }

        String content;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            content = new String(inputStream.readAllBytes());
        }

        Map<String, Object> data = (Map<String, Object>) yaml.load(content);

        Class<?> clazz = this.getClass();
        YamlFile yamlFileAnnotation = clazz.getAnnotation(YamlFile.class);
        boolean isLenientByDefault = yamlFileAnnotation!=null && yamlFileAnnotation.lenient()==Leniency.LENIENT;

        try {
            loadFields(data, isLenientByDefault);
        } catch (IllegalAccessException | IllegalArgumentException | NullPointerException | FinalAttribute e) {
            throw new IOException(e);
        }
        return (T) this;
    }

    private void loadFields(Map<String, Object> data, boolean isLenientByDefault) throws FinalAttribute, IllegalAccessException, IOException {
        if (data==null) {
            data = new HashMap<>();
        }

        for (Class<?> clazz = this.getClass(); clazz!=null; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                YamlKey keyAnnotation = field.getAnnotation(YamlKey.class);
                YamlMap mapAnnotation = field.getAnnotation(YamlMap.class);

                if (keyAnnotation!=null && mapAnnotation!=null) {
                    throw new IllegalStateException("Field " + field.getName() + " cannot have both @YamlKey and @YamlMap annotations");
                }

                if (keyAnnotation!=null && !keyAnnotation.value().trim().isEmpty()) {
                    readYamlKeyField(data, field, keyAnnotation, isLenientByDefault);
                }
                else if (mapAnnotation!=null && !mapAnnotation.value().trim().isEmpty()) {
                    readYamlMapField(data, field, mapAnnotation);
                }
            }
        }
    }

    /**
     * Loads the YAML content from the specified file path into this object.
     *
     * @param file The path to the YAML file as a String.
     * @param <T>  The type of YamlFileInterface implementation.
     * @return The current object instance after loading the YAML content.
     * @throws IOException If there's an error reading the file or parsing the YAML content.
     * @see #load(File)
     *
     * <pre>{@code
     * MyConfig config = new MyConfig();
     * config.load("config.yml");
     * }</pre>
     */
    public <T extends YamlFileInterface> T load(final @NotNull String file) throws IOException {
        return load(new File(file));
    }

    /**
     * Loads the YAML content from the specified file path into this object.
     * Uses reflection to get the plugin's data folder and combines it with the
     * filename specified in the YamlFile annotation.
     *
     * @param plugin The plugin instance to get the data folder from.
     * @param <T>    The type of YamlFileInterface implementation.
     * @return The current object instance after loading the YAML content.
     * @throws IOException              If there's an error reading the file or parsing the YAML content.
     * @throws IllegalArgumentException If the YamlFile annotation is not present or reflection fails.
     * @see #load(File)
     *
     * <pre>{@code
     * @YamlFile(filename = "config.yml")
     * public class MyConfig implements YamlFileInterface {
     *     // implementation
     * }
     *
     * MyConfig config = new MyConfig();
     * config.load(pluginInstance);
     * }</pre>
     */
    public <T extends YamlFileInterface> T load(final @NotNull Object plugin) throws IOException {
        return load(getYamlFile(plugin));
    }

    @Contract("_ -> new")
    private @NotNull File getYamlFile(final @NotNull Object plugin) throws IOException {
        try {
            // Get the YamlFile annotation from this class
            YamlFile yamlFileAnnotation = this.getClass().getAnnotation(YamlFile.class);
            String filename = yamlFileAnnotation==null ? "config.yml":yamlFileAnnotation.fileName();
            if (filename.trim().isEmpty()) {
                throw new IOException("Wrong filename specified in `@YamlFile` annotation");
            }

            // Use reflection to get the getDataFolder method from the plugin
            Method dataFolderMethod = getDataFolderMethod(plugin);
            Class<?> returnType = dataFolderMethod.getReturnType();
            if (!File.class.isAssignableFrom(returnType)) {
                throw new IOException("getDataFolder method does not return a File object, but returns: " + returnType.getName() + " instead");
            }

            File dataFolder = (File) dataFolderMethod.invoke(plugin);
            if (dataFolder==null || (dataFolder.exists() && !dataFolder.isDirectory())) {
                throw new IOException("getDataFolder() method returned a non-existing directory");
            }

            // Create the full path by combining data folder and filename
            return new File(dataFolder, filename);
        } catch (NoSuchMethodException e) {
            throw new IOException("Plugin does not have a getDataFolder() method with no parameters", e);
        } catch (IllegalAccessException e) {
            throw new IOException("getDataFolder() method must be public", e);
        } catch (InvocationTargetException e) {
            throw new IOException(e.getMessage(), e);
        } catch (ClassCastException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private static @NotNull Method getDataFolderMethod(@NotNull Object plugin) throws IOException, NoSuchMethodException {
        Method getDataFolderMethod = null;
        Class<?> currentClass = plugin.getClass();
        while (currentClass!=null && getDataFolderMethod==null) {
            try {
                getDataFolderMethod = currentClass.getDeclaredMethod("getDataFolder");
                if (!Modifier.isPublic(getDataFolderMethod.getModifiers())) {
                    throw new IOException("getDataFolder() method must be public");
                }
            } catch (NoSuchMethodException e) {
                currentClass = currentClass.getSuperclass();
            }
        }

        if (getDataFolderMethod==null) {
            throw new NoSuchMethodException("getDataFolder() method not found in class hierarchy");
        }

        return getDataFolderMethod;
    }

    private static @Nullable Object getNestedValue(final @NotNull Map<String, Object> map, final @NotNull String[] keys) {
        return getNestedValue(map, new ArrayList<>(Arrays.asList(keys)));
    }

    private static @Nullable Object getNestedValue(final @NotNull Map<String, Object> map, final @NotNull List<String> keys) {
        final String key = keys.remove(0);

        if (!map.containsKey(key)) {
            // Unknown inside the map
            return UNKNOWN;
        }

        Object tmp = map.get(key);

        if (keys.isEmpty()) {
            // Final element
            return tmp;
        }

        if (!(tmp instanceof Map)) {
            // If it's not a map, and we still have deeper to dig --> No clue what that is?!
            return UNKNOWN;
        }

        // Go deeper...
        return getNestedValue((Map<String, Object>) tmp, keys);
    }

    private @NotNull String buildYamlContents() throws IllegalAccessException, FinalAttribute, DuplicateKey {

        StringBuilder result = new StringBuilder();

        // Get YAML file header if present
        Class<?> clazz = this.getClass();
        YamlFile yamlFileAnnotation = clazz.getAnnotation(YamlFile.class);
        if (yamlFileAnnotation!=null && !yamlFileAnnotation.header().trim().isEmpty()) {
            splitAndAppend(result, yamlFileAnnotation.header(), "", "# ");
            result.append("\n");
        }

        // Fields
        NestedMap nestedMap = new NestedMap();
        for (Field field : clazz.getDeclaredFields()) {
            YamlKey keyAnnotation = field.getAnnotation(YamlKey.class);
            YamlMap mapAnnotation = field.getAnnotation(YamlMap.class);

            if (keyAnnotation!=null && !keyAnnotation.value().trim().isEmpty()) {
                if (Modifier.isFinal(field.getModifiers())) {
                    throw new FinalAttribute(field.getName());
                }

                field.setAccessible(true);
                Object value = field.get(this);
                YamlComment comment = field.getAnnotation(YamlComment.class);

                nestedMap.put(keyAnnotation.value(), comment==null ? null:comment.value(), value);
            }
            else if (mapAnnotation!=null && !mapAnnotation.value().trim().isEmpty()) {
                writeYamlMapField(nestedMap, this, field, mapAnnotation);
            }
        }

        // 3. Convert the nested map to YAML
        convertNestedMapToYaml(result, nestedMap.getMap(), 0);

        return result.toString();
    }

    private void splitAndAppend(final @NotNull StringBuilder yaml, final @Nullable String data, final @NotNull String indentStr, final @NotNull String extra) {
        if (data==null) {
            return;
        }

        for (String line : data.split("\\r?\\n")) {
            yaml.append(indentStr).append(extra).append(line.replace("\\s*$", "")).append("\n");
        }
    }

    /**
     * Write a map as an item in a list/set with proper YAML formatting
     */
    private void writeMapItemInList(final StringBuilder yaml, final Map<?, ?> map, final String indentStr) {
        boolean first = true;
        for (Map.Entry<?, ?> mapEntry : map.entrySet()) {
            if (first) {
                // First key gets the "- " prefix
                yaml.append(indentStr).append("- ").append(mapEntry.getKey()).append(": ")
                    .append(formatValue(mapEntry.getValue())).append('\n');
                first = false;
            } else {
                // Subsequent keys are indented at the same level as the first key's value
                yaml.append(indentStr).append("  ").append(mapEntry.getKey()).append(": ")
                    .append(formatValue(mapEntry.getValue())).append('\n');
            }
        }
    }

    /**
     * Write a collection (List, Set, etc.) as an item in a list/set with proper YAML formatting
     */
    private void writeCollectionItemInList(final StringBuilder yaml, final Collection<?> collection, final String indentStr) {
        // Convert collection to list, sorting Sets if elements are comparable
        List<?> items;
        if (collection instanceof Set) {
            Set<?> set = (Set<?>) collection;
            // Only sort if elements are Comparable
            if (!set.isEmpty() && set.iterator().next() instanceof Comparable) {
                items = set.stream().sorted().collect(Collectors.toList());
            } else {
                items = new ArrayList<>(set);
            }
        } else {
            // For List, Queue, or other Collection types, preserve order
            items = new ArrayList<>(collection);
        }

        boolean first = true;
        for (Object item : items) {
            if (first) {
                // First item gets double "- " prefix (one for outer collection, one for inner collection)
                splitAndAppend(yaml, formatValue(item), indentStr, "- - ");
                first = false;
            } else {
                // Subsequent items are indented 2 more spaces with "- "
                splitAndAppend(yaml, formatValue(item), indentStr + "  ", "- ");
            }
        }
    }

    private void convertNestedMapToYaml(final StringBuilder yaml, final @NotNull Map<Object, Object> map, final int indent) {
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            tmp.append("  ");
        }
        final String indentStr = tmp.toString();

        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof NestedMap.NestedNode) {
                NestedMap.NestedNode node = (NestedMap.NestedNode) value;
                value = node.value;

                splitAndAppend(yaml, node.comment, indentStr, "# ");
            }

            yaml.append(indentStr).append(key).append(":");
            if (value instanceof Map) {
                yaml.append("\n");
                convertNestedMapToYaml(yaml, (Map<Object, Object>) value, indent + 1);
            }
            else if (value instanceof Collection) {
                yaml.append("\n");

                // Convert collection to list, sorting Sets if elements are comparable
                Collection<?> collection = (Collection<?>) value;
                List<?> items;
                if (value instanceof Set) {
                    Set<?> set = (Set<?>) value;
                    // Only sort if elements are Comparable (e.g., String, Integer)
                    // Don't try to sort Maps or other non-comparable objects
                    if (!set.isEmpty() && set.iterator().next() instanceof Comparable) {
                        items = set.stream().sorted().collect(Collectors.toList());
                    } else {
                        items = new ArrayList<>(set);
                    }
                } else {
                    // For List, Queue, or other Collection types, preserve order
                    items = new ArrayList<>(collection);
                }

                // Write each item with appropriate handler based on type
                for (Object item : items) {
                    if (item instanceof Map) {
                        writeMapItemInList(yaml, (Map<?, ?>) item, indentStr + "  ");
                    } else if (item instanceof Collection) {
                        writeCollectionItemInList(yaml, (Collection<?>) item, indentStr + "  ");
                    } else {
                        splitAndAppend(yaml, formatValue(item), indentStr + "  ", "- ");
                    }
                }
            }
            else {
                yaml.append(' ').append(formatValue(value)).append('\n');
            }
        }
    }

    public static Optional<String> getStaticFieldName(Object value) {
        try {
            Class<?> clazz = value.getClass();

            return Arrays.stream(clazz.getDeclaredFields())
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

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    Pattern genericToStringPattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_.]*)\\.([A-Z][a-zA-Z0-9_]*)@([a-f0-9]+)");

    private @NotNull String formatValue(final Object value) {
        String yamlContent = yaml.dump(value).trim();

        if (value instanceof Enum || value instanceof UUID) {
            // Remove the tag in the yaml
            // !!org.avarion.yaml.Material 'A' --> 'A'
            return yamlContent.replaceAll("^!!\\S+\\s+", "");
        }
        else {
            Matcher matcher = genericToStringPattern.matcher(yamlContent);
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
     * Saves the current object's content to the specified file in YAML format.
     *
     * @param file The File object representing the YAML file to save to.
     * @throws IOException If there's an error writing to the file.
     *
     * <pre>{@code
     * MyConfig config = new MyConfig();
     * config.save(new File("config.yml"));
     * }</pre>
     */
    public void save(final @NotNull File file) throws IOException {
        final File newFile = file.getAbsoluteFile();
        newFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(newFile)) {
            writer.write(buildYamlContents());
        } catch (IllegalAccessException | YamlException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Saves the current object's content to the specified file path in YAML format.
     *
     * @param target The path to the YAML file as a String.
     * @throws IOException If there's an error writing to the file.
     * @see #save(File)
     *
     * <pre>{@code
     * MyConfig config = new MyConfig();
     * config.save("config.yml");
     * }</pre>
     */
    public void save(@NotNull final String target) throws IOException {
        save(new File(target));
    }

    /**
     * Saves the current object's content to the YAML file in the plugin's data folder.
     * Uses reflection to get the plugin's data folder and combines it with the
     * filename specified in the YamlFile annotation.
     *
     * @param plugin The plugin instance to get the data folder from.
     * @throws IOException              If there's an error writing to the file.
     * @throws IllegalArgumentException If the YamlFile annotation is not present or reflection fails.
     * @see #save(File)
     *
     * <pre>{@code
     * @YamlFile(filename = "config.yml")
     * public class MyConfig implements YamlFileInterface {
     *     // implementation
     * }
     *
     * MyConfig config = new MyConfig();
     * config.save(pluginInstance);
     * }</pre>
     */
    public void save(final @NotNull Object plugin) throws IOException {
        save(getYamlFile(plugin));
    }

    private void readYamlKeyField(Map<String, Object> data, @NotNull Field field, @NotNull YamlKey annotation, boolean isLenientByDefault)
            throws FinalAttribute, IllegalAccessException, IOException {
        if (Modifier.isFinal(field.getModifiers())) {
            throw new FinalAttribute(field.getName());
        }

        String key = annotation.value();
        boolean isLenient = isLenient(annotation.lenient(), isLenientByDefault);

        Object value = getNestedValue(data, key.split("\\."));
        if (value!=UNKNOWN) {
            field.setAccessible(true);
            field.set(this, getConvertedValue(field, value, isLenient));
        }
    }

    @Contract(pure = true)
    private static boolean isLenient(@NotNull Leniency leniency, boolean isLenientByDefault) {
        switch (leniency) {
            case LENIENT:
                return true;
            case UNDEFINED:
                return isLenientByDefault;
            default:
                return false;
        }
    }

    private void readYamlMapField(Map<String, Object> data, @NotNull Field field, @NotNull YamlMap annotation) throws IllegalAccessException, FinalAttribute {
        if (Modifier.isFinal(field.getModifiers())) {
            throw new FinalAttribute(field.getName());
        }

        String mapKey = annotation.value();
        Object mapValue = getNestedValue(data, mapKey.split("\\."));
        if (mapValue==UNKNOWN || mapValue==null) {
            return; // Not provided: don't change the default values
        }

        try {
            Map<String, Object> fieldMap = (Map<String, Object>) mapValue;

            YamlMap.YamlMapProcessor<YamlFileInterface> processor = (YamlMap.YamlMapProcessor<YamlFileInterface>) annotation.processor()
                                                                                                                            .getDeclaredConstructor()
                                                                                                                            .newInstance();
            field.set(this, new LinkedHashMap<>());

            for (Map.Entry<String, Object> entry : fieldMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    processor.read(this, entry.getKey(), (Map<String, Object>) entry.getValue());
                }
            }
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | ClassCastException e) {
            throw new IllegalStateException("Failed to instantiate YamlMapProcessor", e);
        }
    }

    private void writeYamlMapField(NestedMap nestedMap, Object obj, @NotNull Field field, @NotNull YamlMap annotation)
            throws IllegalAccessException, DuplicateKey {
        String mapKey = annotation.value();
        Object fieldValue = field.get(obj);

        if (fieldValue instanceof Map) {
            try {
                YamlMap.YamlMapProcessor<YamlFileInterface> processor = (YamlMap.YamlMapProcessor<YamlFileInterface>) annotation.processor()
                                                                                                                                .getDeclaredConstructor()
                                                                                                                                .newInstance();
                Map<String, Object> processedMap = new HashMap<>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) fieldValue).entrySet()) {
                    String key = entry.getKey().toString();
                    Map<String, Object> value = processor.write((YamlFileInterface) obj, key, entry.getValue());
                    processedMap.put(key, value);
                }
                nestedMap.put(mapKey, null, processedMap);
            } catch (InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalStateException("Failed to instantiate YamlMapProcessor", e);
            }
        }
    }
}
