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

/**
 * Tests for the duck-typed plugin-logger routing performed by
 * {@link YamlFileInterface#load(Object)}: lenient warnings should reach the plugin's own
 * logger when one is discoverable, and fall back to {@link TypeConverter#LOG} otherwise.
 */
class PluginLoggerRoutingTest extends TestCommon {

    @TempDir
    Path tempDir;

    @YamlFile(fileName = "config.yml", lenient = Leniency.LENIENT)
    static class EnumListConfig extends YamlFileInterface {
        @YamlKey("mats")
        public List<Material> mats = List.of();
    }

    // ===== Plugin fixtures =====

    /** Bukkit/Spigot-style: getLogger() returns a real JUL Logger. */
    static class JulLoggerPlugin {
        private final File dataFolder;
        private final Logger logger;
        public JulLoggerPlugin(File dataFolder, Logger logger) { this.dataFolder = dataFolder; this.logger = logger; }
        public File getDataFolder() { return dataFolder; }
        public Logger getLogger() { return logger; }
    }

    /** Modern-Paper-style: a fake logger exposing only {@code warn(String)}. */
    static class Slf4jStyleLogger {
        final List<String> warnings = new ArrayList<>();
        public void warn(String message) { warnings.add(message); }
    }
    static class Slf4jPlugin {
        private final File dataFolder;
        private final Slf4jStyleLogger logger;
        public Slf4jPlugin(File dataFolder, Slf4jStyleLogger logger) { this.dataFolder = dataFolder; this.logger = logger; }
        public File getDataFolder() { return dataFolder; }
        public Slf4jStyleLogger getLogger() { return logger; }
    }

    /** Log4j2/ALogger-style: only {@code warning(String, Object...)} varargs. */
    static class VarargsStyleLogger {
        final List<String> warnings = new ArrayList<>();
        public void warning(String message, Object... args) { warnings.add(message); }
    }
    static class VarargsPlugin {
        private final File dataFolder;
        private final VarargsStyleLogger logger;
        public VarargsPlugin(File dataFolder, VarargsStyleLogger logger) { this.dataFolder = dataFolder; this.logger = logger; }
        public File getDataFolder() { return dataFolder; }
        public VarargsStyleLogger getLogger() { return logger; }
    }

    /** getLogger() is private — must be ignored by the public-method lookup. */
    static class PrivateLoggerPlugin {
        private final File dataFolder;
        public PrivateLoggerPlugin(File dataFolder) { this.dataFolder = dataFolder; }
        public File getDataFolder() { return dataFolder; }
        @SuppressWarnings("unused")
        private Logger getLogger() { return Logger.getLogger("private.should.be.ignored"); }
    }

    /** getLogger() returns an object with no warn/warning method — must fall back. */
    static class WrongLoggerTypePlugin {
        private final File dataFolder;
        public WrongLoggerTypePlugin(File dataFolder) { this.dataFolder = dataFolder; }
        public File getDataFolder() { return dataFolder; }
        public String getLogger() { return "not a Logger"; }
    }

    /** getLogger() returns null — must fall back. */
    static class NullLoggerPlugin {
        private final File dataFolder;
        public NullLoggerPlugin(File dataFolder) { this.dataFolder = dataFolder; }
        public File getDataFolder() { return dataFolder; }
        public Logger getLogger() { return null; }
    }

    /** Logger whose warn(String) throws — verifies invokeQuietly swallows. */
    static class ThrowingLogger {
        public void warn(String message) { throw new RuntimeException("boom"); }
    }
    static class ThrowingLoggerPlugin {
        private final File dataFolder;
        public ThrowingLoggerPlugin(File dataFolder) { this.dataFolder = dataFolder; }
        public File getDataFolder() { return dataFolder; }
        public ThrowingLogger getLogger() { return new ThrowingLogger(); }
    }

    // ===== Helpers =====

    private void writeBadEnumYaml() throws IOException {
        Files.writeString(new File(tempDir.toFile(), "config.yml").toPath(),
                "mats:\n  - A\n  - NOT_A_MATERIAL\n  - B\n");
    }

    // ===== Tests =====

    @Test
    void testRoutesToJulLoggerOnPlugin() throws IOException {
        writeBadEnumYaml();

        Logger pluginLogger = Logger.getLogger("test.plugin." + System.nanoTime());
        pluginLogger.setUseParentHandlers(false);
        List<LogRecord> captured = new ArrayList<>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord r) { captured.add(r); }
            @Override public void flush() { /* no buffer */ }
            @Override public void close() { /* no resources */ }
        };
        pluginLogger.addHandler(handler);
        try {
            EnumListConfig loaded = new EnumListConfig().load(new JulLoggerPlugin(tempDir.toFile(), pluginLogger));
            assertThat(loaded.mats).containsExactly(Material.A, Material.B);

            assertThat(captured).hasSize(1);
            assertThat(captured.get(0).getLevel()).isEqualTo(Level.WARNING);
            assertThat(captured.get(0).getMessage()).contains("NOT_A_MATERIAL");
            assertThat(logs).isEmpty();
        } finally {
            pluginLogger.removeHandler(handler);
        }
    }

    @Test
    void testRoutesToSlf4jStyleWarnMethod() throws IOException {
        writeBadEnumYaml();

        Slf4jStyleLogger fake = new Slf4jStyleLogger();
        new EnumListConfig().load(new Slf4jPlugin(tempDir.toFile(), fake));

        assertThat(fake.warnings).hasSize(1);
        assertThat(fake.warnings.get(0)).contains("NOT_A_MATERIAL");
        assertThat(logs).isEmpty();
    }

    @Test
    void testRoutesToVarargsStyleWarningMethod() throws IOException {
        writeBadEnumYaml();

        VarargsStyleLogger fake = new VarargsStyleLogger();
        new EnumListConfig().load(new VarargsPlugin(tempDir.toFile(), fake));

        assertThat(fake.warnings).hasSize(1);
        assertThat(fake.warnings.get(0)).contains("NOT_A_MATERIAL");
        assertThat(logs).isEmpty();
    }

    @Test
    void testFallsBackToDefaultWhenNoSinkDiscoverable() throws IOException {
        writeBadEnumYaml();

        // Private getLogger → public-method lookup misses it.
        new EnumListConfig().load(new PrivateLoggerPlugin(tempDir.toFile()));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getMessage()).contains("NOT_A_MATERIAL");

        logs.clear();

        // getLogger() returns a String (no warn/warning method).
        new EnumListConfig().load(new WrongLoggerTypePlugin(tempDir.toFile()));
        assertThat(logs).hasSize(1);

        logs.clear();

        // getLogger() returns null.
        new EnumListConfig().load(new NullLoggerPlugin(tempDir.toFile()));
        assertThat(logs).hasSize(1);
    }

    @Test
    void testThrowingPluginLoggerDoesNotBreakLoad() throws IOException {
        writeBadEnumYaml();

        EnumListConfig loaded = new EnumListConfig().load(new ThrowingLoggerPlugin(tempDir.toFile()));
        assertThat(loaded.mats).containsExactly(Material.A, Material.B);
    }
}
