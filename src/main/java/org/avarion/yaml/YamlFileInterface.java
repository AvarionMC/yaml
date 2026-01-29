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
    private static final YamlWriter yamlWriter = new YamlWriter(yaml);

    // ==================== Load Methods ====================

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
        boolean isLenientByDefault = yamlFileAnnotation != null && yamlFileAnnotation.lenient() == Leniency.LENIENT;

        try {
            loadFields(data, isLenientByDefault);
        } catch (IllegalAccessException | IllegalArgumentException | NullPointerException | FinalAttribute e) {
            throw new IOException(e);
        }
        return (T) this;
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

    // ==================== Save Methods ====================

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

    // ==================== Field Processing ====================

    private void loadFields(Map<String, Object> data, boolean isLenientByDefault) throws FinalAttribute, IllegalAccessException, IOException {
        if (data == null) {
            data = new HashMap<>();
        }

        for (Class<?> clazz = this.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                YamlKey keyAnnotation = field.getAnnotation(YamlKey.class);
                YamlMap mapAnnotation = field.getAnnotation(YamlMap.class);

                if (keyAnnotation != null && mapAnnotation != null) {
                    throw new IllegalStateException("Field " + field.getName() + " cannot have both @YamlKey and @YamlMap annotations");
                }

                if (keyAnnotation != null && !keyAnnotation.value().trim().isEmpty()) {
                    readYamlKeyField(data, field, keyAnnotation, isLenientByDefault);
                }
                else if (mapAnnotation != null && !mapAnnotation.value().trim().isEmpty()) {
                    readYamlMapField(data, field, mapAnnotation);
                }
            }
        }
    }

    private void readYamlKeyField(Map<String, Object> data, @NotNull Field field, @NotNull YamlKey annotation, boolean isLenientByDefault)
            throws FinalAttribute, IllegalAccessException, IOException {
        if (Modifier.isFinal(field.getModifiers())) {
            throw new FinalAttribute(field.getName());
        }

        String key = annotation.value();
        boolean isLenient = isLenient(annotation.lenient(), isLenientByDefault);

        Object value = getNestedValue(data, key.split("\\."));
        if (value != UNKNOWN) {
            field.setAccessible(true);
            field.set(this, TypeConverter.getConvertedValue(field, value, isLenient));
        }
    }

    private void readYamlMapField(Map<String, Object> data, @NotNull Field field, @NotNull YamlMap annotation) throws IllegalAccessException, FinalAttribute {
        if (Modifier.isFinal(field.getModifiers())) {
            throw new FinalAttribute(field.getName());
        }

        String mapKey = annotation.value();
        Object mapValue = getNestedValue(data, mapKey.split("\\."));
        if (mapValue == UNKNOWN || mapValue == null) {
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

    // ==================== YAML Building ====================

    private @NotNull String buildYamlContents() throws IllegalAccessException, FinalAttribute, DuplicateKey, IOException {

        StringBuilder result = new StringBuilder();

        // Get YAML file header if present
        Class<?> clazz = this.getClass();
        YamlFile yamlFileAnnotation = clazz.getAnnotation(YamlFile.class);
        if (yamlFileAnnotation != null && !yamlFileAnnotation.header().trim().isEmpty()) {
            appendHeaderComment(result, yamlFileAnnotation.header());
            result.append("\n");
        }

        // Fields
        NestedMap nestedMap = new NestedMap();
        for (Field field : clazz.getDeclaredFields()) {
            YamlKey keyAnnotation = field.getAnnotation(YamlKey.class);
            YamlMap mapAnnotation = field.getAnnotation(YamlMap.class);

            if (keyAnnotation != null && !keyAnnotation.value().trim().isEmpty()) {
                if (Modifier.isFinal(field.getModifiers())) {
                    throw new FinalAttribute(field.getName());
                }

                field.setAccessible(true);
                Object value = field.get(this);
                YamlComment comment = field.getAnnotation(YamlComment.class);

                nestedMap.put(keyAnnotation.value(), comment == null ? null : comment.value(), value);
            }
            else if (mapAnnotation != null && !mapAnnotation.value().trim().isEmpty()) {
                writeYamlMapField(nestedMap, this, field, mapAnnotation);
            }
        }

        // Convert the nested map to YAML using YamlWriter
        result.append(yamlWriter.write(nestedMap.getMap()));

        return result.toString();
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

    private void appendHeaderComment(StringBuilder result, String header) {
        for (String line : header.split("\\r?\\n")) {
            result.append("# ").append(line.replaceAll("\\s*$", "")).append("\n");
        }
    }

    // ==================== Nested Value Navigation ====================

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

    // ==================== Plugin Utilities ====================

    @Contract("_ -> new")
    private @NotNull File getYamlFile(final @NotNull Object plugin) throws IOException {
        try {
            // Get the YamlFile annotation from this class
            YamlFile yamlFileAnnotation = this.getClass().getAnnotation(YamlFile.class);
            String filename = yamlFileAnnotation == null ? "config.yml" : yamlFileAnnotation.fileName();
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
            if (dataFolder == null || (dataFolder.exists() && !dataFolder.isDirectory())) {
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
        while (currentClass != null && getDataFolderMethod == null) {
            try {
                getDataFolderMethod = currentClass.getDeclaredMethod("getDataFolder");
                if (!Modifier.isPublic(getDataFolderMethod.getModifiers())) {
                    throw new IOException("getDataFolder() method must be public");
                }
            } catch (NoSuchMethodException e) {
                currentClass = currentClass.getSuperclass();
            }
        }

        if (getDataFolderMethod == null) {
            throw new NoSuchMethodException("getDataFolder() method not found in class hierarchy");
        }

        return getDataFolderMethod;
    }

    // ==================== Utility Methods ====================

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
}
