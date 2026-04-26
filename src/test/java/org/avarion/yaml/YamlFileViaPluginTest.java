package org.avarion.yaml;

import org.avarion.yaml.testClasses.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.*;

class YamlFileViaPluginTest extends TestCommon {

    @TempDir
    Path tempDir;

    @YamlFile
    static class HappyFlow extends YamlFileInterface {
        @YamlKey("age")
        public int age = 30;
    }

    static class HappyFlowWithoutAnnotation extends YamlFileInterface {
        @YamlKey("age")
        public int age = 30;
    }

    @YamlFile(fileName = "")
    static class WrongAnnotation extends YamlFileInterface {
        @YamlKey("age")
        public int age = 30;
    }

    @YamlFile(fileName = "test.yml")
    static class HappyFlowViaTest extends YamlFileInterface {
        @YamlKey("age")
        public int age = 30;
    }

    // Valid plugin with correct getDataFolder signature
    static class ValidPlugin {
        private final File dataFolder;

        public ValidPlugin(File dataFolder) {
            this.dataFolder = dataFolder;
        }

        public File getDataFolder() {
            return dataFolder;
        }
    }

    static class InheritedPlugin extends ValidPlugin {
        public InheritedPlugin(File dataFolder) {
            super(dataFolder);
        }
    }

    // Plugin with getDataFolder that returns a File subclass
    static class FileSubclass extends File {
        public FileSubclass(String pathname) {
            super(pathname);
        }
    }

    static class ValidPluginWithSubclass {
        private final FileSubclass dataFolder;

        public ValidPluginWithSubclass(FileSubclass dataFolder) {
            this.dataFolder = dataFolder;
        }

        public FileSubclass getDataFolder() {
            return dataFolder;
        }
    }

    // Plugin with wrong return type
    static class WrongReturnTypePlugin {
        public String getDataFolder() {
            throw new IllegalArgumentException();
        }
    }

    // Plugin with parameters in getDataFolder
    static class WrongParametersPlugin {
        public File getDataFolder(String parameter) {
            throw new IllegalArgumentException();
        }
    }

    // Plugin without getDataFolder method
    static class NoMethodPlugin {
        public File getSomeOtherMethod() {
            throw new IllegalArgumentException();
        }
    }

    // Plugin with private getDataFolder method
    static class PrivateMethodPlugin {
        private File getDataFolder() {
            throw new IllegalArgumentException();
        }
    }

    // Plugin that returns null from getDataFolder
    static class NullReturningPlugin {
        public File getDataFolder() {
            return null;
        }
    }

    // Plugin exposing both getDataFolder() and getLogger() — the shape Bukkit/Paper plugins have.
    static class LoggingPlugin {
        private final File dataFolder;
        private final Logger logger;

        public LoggingPlugin(File dataFolder, Logger logger) {
            this.dataFolder = dataFolder;
            this.logger = logger;
        }

        public File getDataFolder() {
            return dataFolder;
        }

        public Logger getLogger() {
            return logger;
        }
    }

    @YamlFile(fileName = "config.yml", lenient = Leniency.LENIENT)
    static class EnumListConfig extends YamlFileInterface {
        @YamlKey("mats")
        public List<Material> mats = List.of();
    }

    // Plugin with a getLogger() that's NOT public — must be ignored by the reflection lookup.
    static class PrivateLoggerPlugin {
        private final File dataFolder;
        public PrivateLoggerPlugin(File dataFolder) { this.dataFolder = dataFolder; }
        public File getDataFolder() { return dataFolder; }
        @SuppressWarnings("unused")
        private Logger getLogger() { return Logger.getLogger("private.should.be.ignored"); }
    }

    // Plugin whose getLogger() returns an object with no warn/warning method — must fall back to default.
    static class WrongLoggerTypePlugin {
        private final File dataFolder;
        public WrongLoggerTypePlugin(File dataFolder) { this.dataFolder = dataFolder; }
        public File getDataFolder() { return dataFolder; }
        public String getLogger() { return "not a Logger"; }
    }

    // Plugin whose getLogger() returns null — must fall back to default.
    static class NullLoggerPlugin {
        private final File dataFolder;
        public NullLoggerPlugin(File dataFolder) { this.dataFolder = dataFolder; }
        public File getDataFolder() { return dataFolder; }
        public Logger getLogger() { return null; }
    }

    // Logger whose warn(String) throws — verifies invokeQuietly swallows so the load doesn't blow up.
    static class ThrowingLogger {
        @SuppressWarnings("unused")
        public void warn(String message) { throw new RuntimeException("boom"); }
    }

    static class ThrowingLoggerPlugin {
        private final File dataFolder;
        public ThrowingLoggerPlugin(File dataFolder) { this.dataFolder = dataFolder; }
        public File getDataFolder() { return dataFolder; }
        public ThrowingLogger getLogger() { return new ThrowingLogger(); }
    }

    // Fake SLF4J-style logger: exposes warn(String) but NOT warning(String). Records the calls.
    static class Slf4jStyleLogger {
        final List<String> warnings = new ArrayList<>();
        @SuppressWarnings("unused")
        public void warn(String message) { warnings.add(message); }
    }

    static class Slf4jPlugin {
        private final File dataFolder;
        private final Slf4jStyleLogger logger;
        public Slf4jPlugin(File dataFolder, Slf4jStyleLogger logger) { this.dataFolder = dataFolder; this.logger = logger; }
        public File getDataFolder() { return dataFolder; }
        public Slf4jStyleLogger getLogger() { return logger; }
    }

    // Fake ALogger-style logger: exposes warning(String, Object...) varargs only.
    static class VarargsStyleLogger {
        final List<String> warnings = new ArrayList<>();
        @SuppressWarnings("unused")
        public void warning(String message, Object... args) { warnings.add(message); }
    }

    static class VarargsPlugin {
        private final File dataFolder;
        private final VarargsStyleLogger logger;
        public VarargsPlugin(File dataFolder, VarargsStyleLogger logger) { this.dataFolder = dataFolder; this.logger = logger; }
        public File getDataFolder() { return dataFolder; }
        public VarargsStyleLogger getLogger() { return logger; }
    }

    @Test
    void testLenientWarningGoesToPluginLoggerWhenLoadingViaPlugin() throws IOException {
        File dataFolder = tempDir.toFile();
        Files.writeString(new File(dataFolder, "config.yml").toPath(),
                "mats:\n  - A\n  - NOT_A_MATERIAL\n  - B\n");

        Logger pluginLogger = Logger.getLogger("test.plugin." + System.nanoTime());
        pluginLogger.setUseParentHandlers(false);
        List<LogRecord> pluginLogs = new ArrayList<>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord r) { pluginLogs.add(r); }
            @Override public void flush() { /* no buffer */ }
            @Override public void close() { /* no resources */ }
        };
        pluginLogger.addHandler(handler);

        try {
            LoggingPlugin plugin = new LoggingPlugin(dataFolder, pluginLogger);
            EnumListConfig loaded = new EnumListConfig().load(plugin);
            assertThat(loaded.mats).containsExactly(Material.A, Material.B);

            // Warning routed to the plugin's logger, NOT the library default
            assertThat(pluginLogs).hasSize(1);
            assertThat(pluginLogs.get(0).getLevel()).isEqualTo(Level.WARNING);
            assertThat(pluginLogs.get(0).getMessage()).contains("NOT_A_MATERIAL");
            assertThat(logs).isEmpty();
        } finally {
            pluginLogger.removeHandler(handler);
        }
    }

    @Test
    void testNonPublicOrWrongTypeGetLoggerFallsBackToDefault() throws IOException {
        File dataFolder = tempDir.toFile();
        Files.writeString(new File(dataFolder, "config.yml").toPath(),
                "mats:\n  - A\n  - NOT_A_MATERIAL\n");

        // Private getLogger → reflection lookup (getMethod) must skip it; warning falls back to TypeConverter.LOG.
        new EnumListConfig().load(new PrivateLoggerPlugin(dataFolder));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getMessage()).contains("NOT_A_MATERIAL");

        logs.clear();

        // Public getLogger() returning an object with no warn/warning method → fall back.
        new EnumListConfig().load(new WrongLoggerTypePlugin(dataFolder));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getMessage()).contains("NOT_A_MATERIAL");

        logs.clear();

        // Public getLogger() returning null → fall back.
        new EnumListConfig().load(new NullLoggerPlugin(dataFolder));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getMessage()).contains("NOT_A_MATERIAL");
    }

    @Test
    void testThrowingPluginLoggerDoesNotBreakLoad() throws IOException {
        File dataFolder = tempDir.toFile();
        Files.writeString(new File(dataFolder, "config.yml").toPath(),
                "mats:\n  - A\n  - NOT_A_MATERIAL\n  - B\n");

        // Plugin's logger.warn(String) throws — invokeQuietly must swallow so the load completes.
        EnumListConfig loaded = new EnumListConfig().load(new ThrowingLoggerPlugin(dataFolder));
        assertThat(loaded.mats).containsExactly(Material.A, Material.B);
    }

    @Test
    void testWarningRoutesToSlf4jStyleWarnMethod() throws IOException {
        File dataFolder = tempDir.toFile();
        Files.writeString(new File(dataFolder, "config.yml").toPath(),
                "mats:\n  - A\n  - NOT_A_MATERIAL\n");

        Slf4jStyleLogger fake = new Slf4jStyleLogger();
        new EnumListConfig().load(new Slf4jPlugin(dataFolder, fake));

        // Routed to the SLF4J-style warn(String); default sink stayed silent.
        assertThat(fake.warnings).hasSize(1);
        assertThat(fake.warnings.get(0)).contains("NOT_A_MATERIAL");
        assertThat(logs).isEmpty();
    }

    @Test
    void testWarningRoutesToVarargsStyleWarningMethod() throws IOException {
        File dataFolder = tempDir.toFile();
        Files.writeString(new File(dataFolder, "config.yml").toPath(),
                "mats:\n  - A\n  - NOT_A_MATERIAL\n");

        VarargsStyleLogger fake = new VarargsStyleLogger();
        new EnumListConfig().load(new VarargsPlugin(dataFolder, fake));

        // Routed to warning(String, Object...) with empty varargs; default sink stayed silent.
        assertThat(fake.warnings).hasSize(1);
        assertThat(fake.warnings.get(0)).contains("NOT_A_MATERIAL");
        assertThat(logs).isEmpty();
    }

    @Test
    void testLoadWithValidPlugin() throws IOException {
        File dataFolder = tempDir.toFile();
        ValidPlugin plugin = new ValidPlugin(dataFolder);

        // Create a test YAML file in the data folder
        File yamlFile = new File(dataFolder, "test.yml");
        HappyFlow original = new HappyFlow();
        original.save(yamlFile);

        // Create a new instance and load using plugin
        HappyFlow loaded = new HappyFlow().load(plugin);

        assertThat(loaded).isNotNull();
        assertThat(loaded.age).isEqualTo(30);
    }

    @Test
    void testLoadWithoutAnnotation() throws IOException {
        File dataFolder = tempDir.toFile();
        ValidPlugin plugin = new ValidPlugin(dataFolder);

        // Create a test YAML file in the data folder
        HappyFlowWithoutAnnotation original = new HappyFlowWithoutAnnotation();
        original.save(plugin);

        assertThat(new File(dataFolder, "config.yml")).exists();
    }

    @Test
    void testLoadWithInheritedValidPlugin() throws IOException {
        File dataFolder = tempDir.toFile();
        InheritedPlugin plugin = new InheritedPlugin(dataFolder);

        // Create a test YAML file in the data folder
        File yamlFile = new File(dataFolder, "test.yml");
        HappyFlow original = new HappyFlow();
        original.save(yamlFile);

        // Create a new instance and load using plugin
        HappyFlow loaded = new HappyFlow().load(plugin);

        assertThat(loaded).isNotNull();
        assertThat(loaded.age).isEqualTo(30);
    }

    @Test
    void testSaveWithValidPlugin() throws IOException {
        File dataFolder = tempDir.toFile();
        ValidPlugin plugin = new ValidPlugin(dataFolder);

        HappyFlow yamlFile = new HappyFlow();
        yamlFile.age = 35;

        // Save using plugin
        yamlFile.save(plugin);

        // Verify file was created in the correct location
        File expectedFile = new File(dataFolder, "config.yml");
        assertThat(expectedFile).exists();

        // Load and verify content
        HappyFlow loaded = new HappyFlow().load(expectedFile);
        assertThat(loaded.age).isEqualTo(35);
    }

    @Test
    void testLoadWithFileSubclass() throws IOException {
        FileSubclass dataFolder = new FileSubclass(tempDir.toString());
        ValidPluginWithSubclass plugin = new ValidPluginWithSubclass(dataFolder);

        // Create a test YAML file
        File yamlFile = new File(dataFolder, "config.yml");
        new HappyFlow().save(yamlFile);

        // Should work with File subclass
        HappyFlow loaded = new HappyFlow().load(plugin);
        assertThat(loaded).isNotNull();
        assertThat(loaded.age).isEqualTo(30);
    }

    @Test
    void testSaveWithFileSubclass() {
        FileSubclass dataFolder = new FileSubclass(tempDir.toString());
        ValidPluginWithSubclass plugin = new ValidPluginWithSubclass(dataFolder);

        HappyFlow yamlFile = new HappyFlow();

        // Should work with File subclass
        assertThatCode(() -> yamlFile.save(plugin)).doesNotThrowAnyException();

        File expectedFile = new File(dataFolder, "config.yml");
        assertThat(expectedFile).exists();
    }

    @Test
    void testLoadWithWrongReturnType() {
        WrongReturnTypePlugin plugin = new WrongReturnTypePlugin();

        assertThatThrownBy(() -> new HappyFlow().load(plugin))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("getDataFolder method does not return a File object")
                .hasMessageContaining("but returns: java.lang.String");
    }

    @Test
    void testSaveWithWrongReturnType() {
        WrongReturnTypePlugin plugin = new WrongReturnTypePlugin();

        assertThatThrownBy(() -> new HappyFlow().save(plugin))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("getDataFolder method does not return a File object")
                .hasMessageContaining("but returns: java.lang.String");
    }

    @Test
    void testLoadWithWrongParameters() {
        WrongParametersPlugin plugin = new WrongParametersPlugin();

        assertThatThrownBy(() -> new HappyFlow().load(plugin))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Plugin does not have a getDataFolder() method with no parameters");
    }

    @Test
    void testSaveWithWrongParameters() {
        WrongParametersPlugin plugin = new WrongParametersPlugin();

        assertThatThrownBy(() -> new HappyFlow().save(plugin))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Plugin does not have a getDataFolder() method with no parameters");
    }

    @Test
    void testLoadWithNoMethod() {
        NoMethodPlugin plugin = new NoMethodPlugin();

        assertThatThrownBy(() -> new HappyFlow().load(plugin))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Plugin does not have a getDataFolder() method with no parameters");
    }

    @Test
    void testSaveWithNoMethod() {
        NoMethodPlugin plugin = new NoMethodPlugin();

        assertThatThrownBy(() -> new HappyFlow().save(plugin))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Plugin does not have a getDataFolder() method with no parameters");
    }

    @Test
    void testLoadWithPrivateMethod() {
        PrivateMethodPlugin plugin = new PrivateMethodPlugin();

        assertThatThrownBy(() -> new HappyFlow().load(plugin))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("getDataFolder() method must be public");
    }

    @Test
    void testSaveWithPrivateMethod() {
        PrivateMethodPlugin plugin = new PrivateMethodPlugin();

        assertThatThrownBy(() -> new HappyFlow().save(plugin))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("getDataFolder() method must be public");
    }

    @Test
    void testWrongFilenameAnnotation() {
        HappyFlow plugin = new HappyFlow();

        assertThatThrownBy(() -> new WrongAnnotation().save(plugin))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Wrong filename specified in `@YamlFile` annotation");
    }

    @Test
    void testLoadWithNullReturningPlugin() {
        NullReturningPlugin plugin = new NullReturningPlugin();

        // This should throw an IOException when trying to create/access the file
        // since the data folder is null
        assertThatThrownBy(() -> new HappyFlow().load(plugin))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("getDataFolder() method returned a non-existing directory");
    }

    @Test
    void testLoadWithInvalidPluginDirectory() throws IOException {
        File testFile = new File(tempDir.toFile(), "invalid");
        testFile.createNewFile();
        ValidPlugin plugin = new ValidPlugin(testFile);

        // This should throw an IOException when trying to create/access the file
        // since the data folder is null
        assertThatThrownBy(() -> new HappyFlow().load(plugin))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("getDataFolder() method returned a non-existing directory");
    }

    @Test
    void testSaveWithNullReturningPlugin() {
        NullReturningPlugin plugin = new NullReturningPlugin();

        // This should throw an IOException when trying to create the file
        // since the data folder is null
        assertThatThrownBy(() -> new HappyFlow().save(plugin))
                .isInstanceOf(Exception.class);
    }

    @Test
    void testLoadWithNonExistentDataFolder() {
        File nonExistentFolder = new File(tempDir.toFile(), "new/nested/folder");
        ValidPlugin plugin = new ValidPlugin(nonExistentFolder);

        // Should create the file and parent directories if they don't exist
        assertThatCode(() -> new HappyFlow().load(plugin))
                .doesNotThrowAnyException();
    }

    @Test
    void testSaveWithNonExistentDataFolder() {
        File nonExistentFolder = new File(tempDir.toFile(), "new/nested/folder");
        ValidPlugin plugin = new ValidPlugin(nonExistentFolder);

        HappyFlow yamlFile = new HappyFlow();

        // Should create parent directories and save successfully
        assertThatCode(() -> yamlFile.save(plugin))
                .doesNotThrowAnyException();

        File expectedFile = new File(nonExistentFolder, "config.yml");
        assertThat(expectedFile).exists();
    }

    @Test
    void testSaveWithCustomFilename() {
        File nonExistentFolder = new File(tempDir.toFile(), "new/nested/folder");
        ValidPlugin plugin = new ValidPlugin(nonExistentFolder);

        HappyFlowViaTest yamlFile = new HappyFlowViaTest();

        // Should create parent directories and save successfully
        assertThatCode(() -> yamlFile.save(plugin))
                .doesNotThrowAnyException();

        File notExpectedFile = new File(nonExistentFolder, "config.yml");
        assertThat(notExpectedFile).doesNotExist();

        File expectedFile = new File(nonExistentFolder, "test.yml");
        assertThat(expectedFile).exists();
    }

    @Test
    void testRoundTripLoadAndSaveWithPlugin() throws IOException {
        File dataFolder = tempDir.toFile();
        ValidPlugin plugin = new ValidPlugin(dataFolder);

        // Create, modify, save, and reload
        HappyFlow original = new HappyFlow();
        original.age = 35;

        original.save(plugin);

        HappyFlow reloaded = new HappyFlow().load(plugin);

        assertThat(reloaded.age).isEqualTo(35);
    }
}