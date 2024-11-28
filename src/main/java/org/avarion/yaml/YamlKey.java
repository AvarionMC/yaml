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
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface YamlKey {
	String value() default "";
    Leniency lenient() default Leniency.UNDEFINED;
}
