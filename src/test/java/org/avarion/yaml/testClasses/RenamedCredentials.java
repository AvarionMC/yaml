package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlComment;
import org.avarion.yaml.YamlKey;

/**
 * A nested record whose components are renamed to snake_case via {@link YamlKey}.
 */
public record RenamedCredentials(
        @YamlKey("user_name") @YamlComment("Login user") String userName,

        @YamlKey("pass_word") String passWord) {
}
