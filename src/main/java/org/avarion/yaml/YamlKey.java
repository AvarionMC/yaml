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
}
