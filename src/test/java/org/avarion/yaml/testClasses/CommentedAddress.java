package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlComment;
import org.avarion.yaml.YamlKey;

/**
 * A record whose components carry {@link YamlComment} annotations.
 * {@code zipCode} deliberately has no comment, and an empty {@link YamlKey} that must fall
 * back to the component name.
 */
public record CommentedAddress(
        @YamlComment("Street name and number") String street,

        @YamlComment("""
                City the address is in.
                Used for the delivery zone.""") String city,

        @YamlKey("") int zipCode) {
}
