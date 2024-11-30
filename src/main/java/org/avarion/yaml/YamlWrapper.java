package org.avarion.yaml;

public interface YamlWrapper {
    String dump(Object data);

    Object load(String content);
}