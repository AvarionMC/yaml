package org.avarion.yaml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the YAML key for a field.
 * This annotation should be applied to fields that need to be serialized/deserialized in YAML.
 *
 * <pre>{@code
 * public class MyConfig extends YamlFileInterface {
 *     @YamlKey("database.url")
 *     private String databaseUrl;
 * }
 * }</pre>
 *
 * <p>It can also be placed on a record component to rename that single key inside the record's
 * block. Dot notation is not supported there: the key lives inside the record's own block and
 * cannot be nested any further.</p>
 *
 * <pre>{@code
 * public record ServerInfo(@YamlKey("bind_port") int port) {}
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface YamlKey {
	String value() default "";
    Leniency lenient() default Leniency.UNDEFINED;

    /**
     * Keys this setting used to be spelled as, newest first.
     *
     * <p>A key that moves between releases is not merely unread. The file is written back from
     * the fields, so the old key is dropped and the value under it goes with it. Naming the old
     * spelling here turns that into a migration: before any field is read, the value is moved to
     * {@link #value()}, and the write-back persists it there — so the declaration can be deleted
     * a release or two later, once the files in the wild have been through it.
     *
     * <pre>{@code
     * @YamlKey(value = "storm.damage-per-second", previously = "zone.damage-per-second")
     * public double damage = 1.0;
     * }</pre>
     *
     * <p>The current key wins when the file holds both, and the older names are tried in the
     * order given, so list the most recent first. Each entry is a full path from the root of the
     * file, in the same dot notation as {@link #value()}.
     *
     * <p>This moves one key. A whole block that moved — where the old key's new home is inside a
     * structure no single field owns — is declared on the class with {@link YamlRename}.
     *
     * <p>Not read on a record component: a component's key lives inside its record's block, and
     * moving into or out of that block is a change to the block, which is {@link YamlRename}.
     */
    String[] previously() default {};
}
