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
import java.util.function.Consumer;

/**
 * Abstract class providing utility methods to handle YAML files, including
 * serialization and deserialization of Java objects.
 */
@SuppressWarnings("unchecked")
public abstract class YamlFileInterface {
    static final Object UNKNOWN = new Object();
    private static final YamlWrapper yaml = YamlWrapperFactory.create();

    /** Old key → the key now holding its value, for the last load. Never null. */
    private @NotNull Map<String, String> renames = Map.of();

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
        return load(file, Set.of());
    }

    /**
     * Loads {@code file}, except for the keys named in {@code ignoredKeys}.
     *
     * <p>An ignored key is not read: the field keeps whatever it already holds, which for a
     * freshly built configuration object is its compile-time default. The file is not altered by
     * this — a following {@link #save(File)} writes the default out, because that is what the
     * field now says.
     *
     * <p>What it is for is the one thing a file-beats-default rule cannot express. A settings
     * file beats the default for every key it has, which is exactly what an operator's choice
     * should do, and is also why a default that improves between releases never reaches anybody
     * whose file already mentions it. Deciding whether a given line is a choice or an untouched
     * copy of an older default needs to know what that older default was, which is the caller's
     * business, not this library's. Acting on the answer is this method.
     *
     * <p>Names a key the way its field declares it — see {@link #declaredKeys()} — and takes
     * everything under it: half a block read from the file and half from the defaults is a shape
     * nobody asked for. Ignoring a key the file does not have does nothing.
     *
     * <p>Applied after {@link YamlRename} and {@link YamlKey#previously()} have moved what they
     * move, so an ignored key stays ignored whether the value in it came from the file directly
     * or was carried there by a declared move.
     *
     * @param ignoredKeys keys the file must not supply; empty for an ordinary load
     */
    public <T extends YamlFileInterface> T load(final @NotNull File file, final @NotNull Set<String> ignoredKeys)
            throws IOException {
        renames = Map.of();

        if (!file.exists()) {
            save(file);
            return (T) this;
        }

        String content;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            content = new String(inputStream.readAllBytes());
        }

        Map<String, Object> parsed = (Map<String, Object>) yaml.load(content);
        Map<String, Object> data = parsed == null ? new LinkedHashMap<>() : parsed;

        Class<?> clazz = this.getClass();
        YamlFile yamlFileAnnotation = clazz.getAnnotation(YamlFile.class);
        boolean isLenientByDefault = yamlFileAnnotation == null || yamlFileAnnotation.lenient() != Leniency.STRICT;
        Naming naming = namingOf(yamlFileAnnotation);

        // Before any field looks at the file, so a setting that has moved is read from where it
        // lives now and written back there — a migration rather than a value quietly lost to the
        // write-back.
        renames = KeyRenames.applyTo(data, clazz, naming);
        KeyRenames.drop(data, ignoredKeys);

        try {
            loadFields(data, isLenientByDefault, naming);
        } catch (IllegalAccessException | IllegalArgumentException | NullPointerException | FinalAttribute e) {
            throw new IOException(e);
        }
        return (T) this;
    }

    /**
     * The YAML keys this class's fields claim, in the order the fields declare them.
     *
     * <p>The unit a caller has to work in when it wants to say something about one setting — an
     * ignore, a comparison against an older default — because a field is what actually reads the
     * file. A field may own a whole block, in which case one entry here stands for every leaf
     * under it.
     *
     * @see #load(File, Set)
     */
    public @NotNull List<String> declaredKeys() {
        Naming naming = namingOf(this.getClass().getAnnotation(YamlFile.class));
        List<String> keys = new ArrayList<>();
        for (Class<?> clazz = this.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                YamlKey annotation = field.getAnnotation(YamlKey.class);
                if (annotation != null) {
                    keys.add(keyOf(field, annotation, naming));
                }
            }
        }
        return keys;
    }

    /**
     * What the last load did with keys that have moved: each old path this class declares, mapped
     * to the key that now holds its value.
     *
     * <p>Empty before any load, and after one that found nothing to move. A path appears here
     * whether its value was carried across or the new key was already set and won — either way
     * the old key was accounted for by a declaration, which is what tells a caller not to report
     * it as a setting that vanished without explanation.
     *
     * @see YamlKey#previously()
     * @see YamlRename
     */
    public @NotNull Map<String, String> renamesApplied() {
        return renames;
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
        Consumer<String> prev = TypeConverter.pushSink(discoverPluginSink(plugin));
        try {
            return load(getYamlFile(plugin));
        } finally {
            TypeConverter.pushSink(prev);
        }
    }

    /**
     * Reflectively duck-type a warning sink onto whatever {@code plugin.getLogger()} returns:
     * try {@code warn(String)}, {@code warning(String)}, {@code warn(String, Object[])},
     * {@code warning(String, Object[])} in that order. Falls back to {@link TypeConverter#LOG}'s
     * {@code warning(String)} when nothing matches, so the caller never has to null-check.
     */
    private static @NotNull Consumer<String> discoverPluginSink(@NotNull Object plugin) {
        final Object logger;
        try {
            logger = plugin.getClass().getMethod("getLogger").invoke(plugin);
        } catch (ReflectiveOperationException ignored) {
            return TypeConverter.LOG::warning;
        }
        if (logger == null) return TypeConverter.LOG::warning;

        Class<?> cls = logger.getClass();
        for (String name : new String[] { "warn", "warning" }) {
            try {
                Method method = cls.getMethod(name, String.class);
                return msg -> invokeQuietly(method, logger, msg);
            } catch (NoSuchMethodException ignored) { /* try next */ }
        }
        for (String name : new String[] { "warn", "warning" }) {
            try {
                Method method = cls.getMethod(name, String.class, Object[].class);
                return msg -> invokeQuietly(method, logger, msg, new Object[0]);
            } catch (NoSuchMethodException ignored) { /* try next */ }
        }
        return TypeConverter.LOG::warning;
    }

    private static void invokeQuietly(@NotNull Method method, @NotNull Object target, Object... args) {
        try {
            method.invoke(target, args);
        } catch (ReflectiveOperationException ignored) {
            // Plugin's logger threw — drop the warning rather than blow up the load.
        }
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

    private void loadFields(@NotNull Map<String, Object> data, boolean isLenientByDefault, @NotNull Naming naming)
            throws FinalAttribute, IllegalAccessException, IOException {
        for (Class<?> clazz = this.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                YamlKey keyAnnotation = field.getAnnotation(YamlKey.class);

                if (keyAnnotation != null) {
                    readYamlKeyField(data, field, keyAnnotation, isLenientByDefault, naming);
                }
            }
        }
    }

    private void readYamlKeyField(
            Map<String, Object> data, @NotNull Field field, @NotNull YamlKey annotation, boolean isLenientByDefault, @NotNull Naming naming)
            throws FinalAttribute, IllegalAccessException, IOException {
        if (Modifier.isFinal(field.getModifiers())) {
            throw new FinalAttribute(field.getName());
        }

        String key = keyOf(field, annotation, naming);
        boolean isLenient = isLenient(annotation.lenient(), isLenientByDefault);

        Object value = getNestedValue(data, key.split("\\."));
        if (value != UNKNOWN) {
            field.setAccessible(true);
            // Read before the conversion, because the conversion may need it: a
            // record block the file only half fills in takes the rest from what
            // the field already holds.
            Object current = field.get(this);
            Object converted = convert(key, field, value, naming, isLenient, current);
            if (converted == TypeConverter.LENIENT_ENUM_SKIP) {
                // Lenient mode: bad enum value at top level — leave field at its default
                return;
            }
            field.set(this, converted);
        }
    }

    /**
     * Converts one field's value, with the key it was read from in front of
     * anything that goes wrong.
     * <p>
     * The converter is handed a {@link Field}, whose name is the Java one — the
     * reader is looking at a yaml file and needs the yaml one. This is the only
     * place a conversion is started, and the only one where both are in scope.
     *
     * @param current what the field holds now, which is what a record component
     *                the file does not mention falls back to
     */
    private static Object convert(
            @NotNull String key, @NotNull Field field, @NotNull Object value, @NotNull Naming naming, boolean isLenient,
            @Nullable Object current)
            throws IOException {
        try {
            return new TypeConverter(naming, isLenient).getConvertedValue(field, field.getType(), value, current);
        }
        catch (IOException e) {
            throw new IOException(key + ": " + e.getMessage(), e);
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

        // Fields, up the class hierarchy exactly as loadFields walks it. Reading further than
        // writing is worse than either alone: an inherited key would be loaded from the file and
        // then left out of what replaces it, so a load-then-save cycle deletes the setting along
        // with whatever the operator had put in it.
        Naming naming = namingOf(yamlFileAnnotation);
        NestedMap nestedMap = new NestedMap();
        for (Class<?> owner = clazz; owner != null; owner = owner.getSuperclass()) {
            for (Field field : owner.getDeclaredFields()) {
                YamlKey keyAnnotation = field.getAnnotation(YamlKey.class);

                if (keyAnnotation != null) {
                    if (Modifier.isFinal(field.getModifiers())) {
                        throw new FinalAttribute(field.getName());
                    }

                    field.setAccessible(true);
                    Object value = field.get(this);
                    YamlComment comment = field.getAnnotation(YamlComment.class);

                    nestedMap.put(keyOf(field, keyAnnotation, naming), comment == null ? null : comment.value(), value);
                }
            }
        }

        // Convert the nested map to YAML using YamlWriter
        result.append(new YamlWriter(yaml, naming).write(nestedMap.getMap()));

        return result.toString();
    }

    /**
     * The YAML key for a field: its {@link YamlKey} when one is spelled out, otherwise the
     * field's own name run through the naming strategy.
     */
    static @NotNull String keyOf(final @NotNull Field field, final @NotNull YamlKey annotation, final @NotNull Naming naming) {
        String key = annotation.value().trim();
        return key.isEmpty() ? naming.convert(field.getName()) : key;
    }

    /** The naming strategy of a config class, falling back to the annotation's own default. */
    private static @NotNull Naming namingOf(final @Nullable YamlFile annotation) {
        return annotation == null ? Naming.SNAKE_CASE : annotation.naming();
    }

    private void appendHeaderComment(StringBuilder result, String header) {
        for (String line : header.split("\\r?\\n")) {
            result.append("# ").append(line.stripTrailing()).append("\n");
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
        } catch (InvocationTargetException | IllegalAccessException | ClassCastException e) {
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
