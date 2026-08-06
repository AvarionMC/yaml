package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlKey;

/**
 * A record whose component asks for a nested (dotted) key, which is not supported.
 */
public record DottedKeyRecord(@YamlKey("nested.value") String value) {
}
