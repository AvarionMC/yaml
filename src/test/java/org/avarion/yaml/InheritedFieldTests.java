package org.avarion.yaml;

import org.avarion.yaml.testClasses.InheritedFieldClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A field a settings class inherits is a setting like any other.
 *
 * <p>Reading already walked the class hierarchy while writing looked only at the class itself,
 * and the two disagreeing is worse than either alone: an inherited key loaded from the file and
 * then left out of everything written back means a load-then-save cycle deletes it, along with
 * whatever the operator had put there.
 */
class InheritedFieldTests extends TestCommon {

    @Test
    void anInheritedFieldIsWrittenOutLikeAnyOther() throws IOException {
        new InheritedFieldClass.Derived().save(target);

        assertThat(readFile())
                .contains("debug: false")
                .contains("timeout: 30")
                .as("its comment comes with it").contains("Belongs to every configuration");
    }

    @Test
    void anInheritedFieldIsReadFromTheFile() throws IOException {
        writeYaml("""
                shared:
                  debug: true
                own:
                  timeout: 90
                """);

        InheritedFieldClass.Derived loaded = new InheritedFieldClass.Derived().load(target);

        assertThat(loaded.debug).isTrue();
        assertThat(loaded.timeout).isEqualTo(90);
    }

    @Test
    void andSurvivesTheRoundTripThatUsedToDropIt() throws IOException {
        writeYaml("shared:\n  debug: true\nown:\n  timeout: 90\n");

        new InheritedFieldClass.Derived().load(target).save(target);

        assertThat(readFile()).contains("debug: true").contains("timeout: 90");
    }

    @Test
    void andItIsOneOfTheKeysTheClassSaysItClaims() {
        assertThat(new InheritedFieldClass.Derived().declaredKeys())
                .containsExactlyInAnyOrder("own.timeout", "shared.debug");
    }
}
