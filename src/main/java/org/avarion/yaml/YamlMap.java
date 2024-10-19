package org.avarion.yaml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface YamlMap {
    String value();

    Class<? extends YamlMapProcessor<YamlFileInterface>> processor();

    interface YamlMapProcessor<T extends YamlFileInterface> {
        void read(T obj, String key, Map<String, Object> value);

        Map<String, Object> write(T obj, String key, Object value);
    }
}