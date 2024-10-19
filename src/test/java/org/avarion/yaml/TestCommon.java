package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestCommon {
    File target;

    protected void replaceInTarget(@NotNull File file, String text, String replacement) throws IOException {
        // Read all lines from the file into a string
        Path filePath = file.toPath();
        String content = new String(Files.readAllBytes(filePath));

        // Replace the target text with the replacement
        content = content.replace(text, replacement);

        // Write the modified content back to the file
        Files.write(filePath, content.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @BeforeEach
    void setUp() {
        try {
            target = File.createTempFile("yaml", ".yaml");
            target.deleteOnExit();
            target.delete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
