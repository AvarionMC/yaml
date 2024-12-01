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

/**
 * Abstract class providing utility methods to handle YAML files, including
 * serialization and deserialization of Java objects.
 */
@SuppressWarnings("unchecked")
public abstract class YamlFileInterface {
    static final Object UNKNOWN = new Object();
    private static final YamlWrapper yaml = YamlWrapperFactory.create();
    private static final Set<String> TRUE_VALUES = new HashSet<>(Arrays.asList("yes", "y", "true", "1"));

    private static @Nullable Object getConvertedValue(final @NotNull Field field, final Object value, boolean isLenient) throws IOException {
        return getConvertedValue(field, field.getType(), value, isLenient);
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
            return handleListValue(field, expectedType, (List<?>) value, isLenient);
        }

        if (expectedType.isInstance(value)) {
            return value;
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
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IOException("'" + expectedType.getSimpleName() + "' doesn't accept a single String argument to create the object.");
        }
    }

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

    private static @NotNull Object handleListValue(final @Nullable Field field, final @NotNull Class<?> expectedType, final List<?> list, boolean isLenient) throws IOException {
        if (!List.class.isAssignableFrom(expectedType)) {
            throw new IOException("Expected a List, but got " + expectedType.getSimpleName());
        }

        Class<?> elementType;
        if (field!=null) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                elementType = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
            }
            else {
                elementType = Object.class;
            }
        }
        else {
            elementType = Object.class;
        }

        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            Object convertedValue = getConvertedValue(null, elementType, item, isLenient);
            result.add(convertedValue);
        }
        return result;
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
     *                     <pre>{@code
     *                     MyConfig config = new MyConfig();
     *                     config.load(new File("config.yml"));
     *                     }</pre>
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
        boolean isLenientByDefault = yamlFileAnnotation!=null && yamlFileAnnotation.lenient() == Leniency.LENIENT;

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

    private void splitAndAppend(
            final @NotNull StringBuilder yaml, final @Nullable String data, final @NotNull String indentStr, final @NotNull String extra
    ) {
        if (data==null) {
            return;
        }

        for (String line : data.split("\\r?\\n")) {
            yaml.append(indentStr).append(extra).append(line.replace("\\s*$", "")).append("\n");
        }
    }

    private void convertNestedMapToYaml(
            final StringBuilder yaml, final @NotNull Map<String, Object> map, final int indent
    ) {
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            tmp.append("  ");
        }
        final String indentStr = tmp.toString();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof NestedMap.NestedNode) {
                NestedMap.NestedNode node = (NestedMap.NestedNode) value;
                value = node.value;

                splitAndAppend(yaml, node.comment, indentStr, "# ");
            }

            yaml.append(indentStr).append(key).append(":");
            if (value instanceof Map) {
                yaml.append("\n");
                convertNestedMapToYaml(yaml, (Map<String, Object>) value, indent + 1);
            }
            else if (value instanceof List) {
                yaml.append("\n");
                for (Object item : (List<?>) value) {
                    splitAndAppend(yaml, formatValue(item), indentStr + "  ", "- ");
                }
            }
            else {
                yaml.append(' ').append(formatValue(value)).append('\n');
            }

        }
    }

    private @NotNull String formatValue(final Object value) {
        String yamlContent = yaml.dump(value).trim();

        if (value instanceof Enum) {
            // Remove the tag in the yaml
            // !!org.avarion.yaml.Material 'A' --> 'A'
            yamlContent = yamlContent.replaceAll("^!!\\S+\\s+", "");
        }

        return yamlContent;
    }

    /**
     * Saves the current object's content to the specified file in YAML format.
     *
     * @param file The File object representing the YAML file to save to.
     * @throws IOException If there's an error writing to the file.
     *
     *                     <pre>{@code
     *                     MyConfig config = new MyConfig();
     *                     config.save(new File("config.yml"));
     *                     }</pre>
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

    private void readYamlKeyField(Map<String, Object> data, @NotNull Field field, @NotNull YamlKey annotation, boolean isLenientByDefault)
            throws FinalAttribute, IllegalAccessException, IOException {
        if (Modifier.isFinal(field.getModifiers())) {
            throw new FinalAttribute(field.getName());
        }

        String key = annotation.value();
        boolean isLenient = isLenient(annotation.lenient(), isLenientByDefault);

        Object value = getNestedValue(data, key.split("\\."));
        if (value!=UNKNOWN) {
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
