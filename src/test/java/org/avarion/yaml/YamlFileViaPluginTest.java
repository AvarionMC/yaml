package org.avarion.yaml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

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