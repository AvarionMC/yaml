package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * How a YAML key is spelled when it has to be <em>derived</em> from a Java identifier.
 *
 * <p>A key is derived for a record component without a {@link YamlKey}, and for a field or
 * component carrying a bare {@code @YamlKey}. An explicit {@code @YamlKey("some.path")} is
 * always used exactly as written and is never converted.</p>
 *
 * <pre>{@code
 * @YamlFile(naming = Naming.KEEP)
 * public class MyConfig extends YamlFileInterface {
 *     // ...
 * }
 * }</pre>
 */
public enum Naming {
    /** Use the Java identifier verbatim: {@code modelId} stays {@code modelId}. */
    KEEP,

    /** Split the identifier on word boundaries: {@code modelId} becomes {@code model_id}. */
    SNAKE_CASE,
    ;

    /** {@code HTTPServer} -> {@code HTTP_Server}: the tail of an acronym starts a new word. */
    private static final Pattern ACRONYM_BOUNDARY = Pattern.compile("([A-Z]+)([A-Z][a-z])");

    /** {@code modelId} -> {@code model_Id}: a capital after a lowercase/digit starts a new word. */
    private static final Pattern WORD_BOUNDARY = Pattern.compile("([a-z0-9])([A-Z])");

    /**
     * Spell {@code identifier} according to this strategy.
     */
    @NotNull String convert(final @NotNull String identifier) {
        if (this == KEEP) {
            return identifier;
        }

        // Both patterns need an ASCII capital to fire, so an identifier already equal to its own
        // lowercasing has no boundary to split on and is its own answer. Single-word names are the
        // common case, and String#toLowerCase hands back the very same instance when no character
        // changes, so this costs a reference comparison rather than two Matcher allocations.
        if (identifier.toLowerCase(Locale.ENGLISH).equals(identifier)) {
            return identifier;
        }

        String separated = ACRONYM_BOUNDARY.matcher(identifier).replaceAll("$1_$2");
        separated = WORD_BOUNDARY.matcher(separated).replaceAll("$1_$2");
        return separated.toLowerCase(Locale.ENGLISH);
    }
}
