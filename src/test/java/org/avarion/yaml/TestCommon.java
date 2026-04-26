package org.avarion.yaml;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

class TestCommon {
    File target;

    /** Warnings captured from {@link TypeConverter#LOG} for the duration of each test. */
    protected final List<LogRecord> logs = new ArrayList<>();
    private final Handler logHandler = new Handler() {
        @Override public void publish(LogRecord record) { logs.add(record); }
        @Override public void flush() {}
        @Override public void close() {}
    };
    private boolean originalUseParentHandlers;

    protected String readFile() throws IOException {
        return new String(Files.readAllBytes(target.toPath()));
    }

    protected void replaceInTarget(String text, String replacement) throws IOException {
        // Read all lines from the file into a string
        Path filePath = target.toPath();
        String content = new String(Files.readAllBytes(filePath));

        // Replace the target text with the replacement
        content = content.replace(text, replacement);

        // Write the modified content back to the file
        Files.write(filePath, content.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    protected void writeYaml(String content) throws IOException {
        Files.writeString(target.toPath(), content);
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
        logs.clear();
        originalUseParentHandlers = TypeConverter.LOG.getUseParentHandlers();
        TypeConverter.LOG.setUseParentHandlers(false);
        TypeConverter.LOG.addHandler(logHandler);
    }

    @AfterEach
    void tearDownLogCapture() {
        TypeConverter.LOG.removeHandler(logHandler);
        TypeConverter.LOG.setUseParentHandlers(originalUseParentHandlers);
    }
}
