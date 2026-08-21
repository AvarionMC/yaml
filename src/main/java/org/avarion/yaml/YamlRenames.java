package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Holds the {@link YamlRename} declarations of one class.
 *
 * <p>The container Java needs to let {@code @YamlRename} repeat. Write {@code @YamlRename}
 * as many times as the class needs and never name this.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface YamlRenames {
    @NotNull YamlRename[] value();
}
