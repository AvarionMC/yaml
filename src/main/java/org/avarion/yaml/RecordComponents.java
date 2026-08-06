package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.RecordComponent;

/**
 * Reads the YAML annotations off a record component.
 */
final class RecordComponents {

    private RecordComponents() {
        // Utility class
    }

    /**
     * The YAML key for a component: its {@link YamlKey} when one is present and non-blank,
     * otherwise the component name spelled according to {@code naming}.
     *
     * @throws IOException if the key contains a {@code .}; unlike a regular field, a record
     *                     component cannot spread itself over a nested path.
     */
    static @NotNull String keyOf(final @NotNull RecordComponent component, final @NotNull Naming naming) throws IOException {
        YamlKey annotation = component.getAnnotation(YamlKey.class);
        String key = annotation == null ? "" : annotation.value().trim();

        if (key.isEmpty()) {
            return naming.convert(component.getName());
        }
        if (key.indexOf('.') >= 0) {
            throw new IOException("'" + key + "' cannot be used for record component '" + component.getName() + "' of "
                                          + component.getDeclaringRecord().getSimpleName()
                                          + ": a record component key cannot be nested, so it may not contain a '.'");
        }
        return key;
    }

    /**
     * The {@link YamlComment} text for a component, or {@code null} when it has none.
     */
    static @Nullable String commentOf(final @NotNull RecordComponent component) {
        YamlComment annotation = component.getAnnotation(YamlComment.class);
        return annotation == null ? null : annotation.value();
    }
}
