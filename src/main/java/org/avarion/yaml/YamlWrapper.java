package org.avarion.yaml;

import org.jetbrains.annotations.Nullable;

public interface YamlWrapper {
    String dump(@Nullable Object data);

    Object load(String content);
}