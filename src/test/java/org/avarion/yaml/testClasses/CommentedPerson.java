package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlComment;

/**
 * A record with commented components, one of which is itself a record with commented
 * components. Used to verify comments are emitted at every nesting level.
 */
public record CommentedPerson(
        @YamlComment("Full name") String name,

        @YamlComment("Age in years") int age,

        @YamlComment("Where this person lives") CommentedAddress address) {
}
