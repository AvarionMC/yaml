package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify a header for the YAML file.
 * This annotation should be applied to the class that extends YamlFileInterface.
 *
 * <pre>{@code
 * @YamlFile(header = """
 * 	MyApp Configuration
 * 	  Version 1.0
 * 	""")
 * public class MyConfig extends YamlFileInterface {
 *     // Configuration fields
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface YamlFile {
	@NotNull String header() default "";
    Leniency lenient() default Leniency.STRICT;
    @NotNull String fileName() default "config.yml";
}
