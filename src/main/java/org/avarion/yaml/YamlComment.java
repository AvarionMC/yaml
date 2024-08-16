package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to add a comment to a YAML field.
 * This annotation can be used alongside @YamlKey to provide additional information in the YAML file.
 *
 * <pre>{@code
 * public class MyConfig extends YamlFileInterface {
 *     @YamlKey("server.port")
 *     @YamlComment("The port number for the server to listen on")
 *     private int serverPort = 8080;
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface YamlComment {
	@NotNull String value() default "";
}
