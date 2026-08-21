package org.avarion.yaml;

import org.avarion.yaml.testClasses.MovedSubtreeClass;
import org.avarion.yaml.testClasses.RenamedKeyClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keys the caller has already decided the file must not supply.
 *
 * <p>A settings file beats the compile-time default for every key it has, which
 * is what an operator's choice is supposed to do — and is also why a default
 * that improves between releases never reaches anybody. Deciding whether a
 * given line is a choice or an untouched copy of last release's default is the
 * caller's problem, not this library's; acting on the answer is this method.
 *
 * <p>Naming a key here does not delete it from the file. It says the field is
 * to keep what it already holds, which for a settings class is the default this
 * release ships.
 */
class IgnoredKeyTests extends TestCommon {

    @Test
    void anIgnoredKeyLeavesItsFieldAtTheValueItAlreadyHeld() throws IOException {
        writeYaml("""
                storm:
                  damage-per-second: 6.0
                game:
                  hub-world: lobby
                """);

        RenamedKeyClass loaded = new RenamedKeyClass().load(target, Set.of("storm.damage-per-second"));

        assertThat(loaded.damage).isEqualTo(1.0);
        assertThat(loaded.hubWorld).as("everything else is read as usual").isEqualTo("lobby");
    }

    @Test
    void andTheWriteBackThenPersistsThatValue() throws IOException {
        writeYaml("storm:\n  damage-per-second: 6.0\n");

        new RenamedKeyClass().load(target, Set.of("storm.damage-per-second")).save(target);

        assertThat(readFile()).contains("damage-per-second: 1.0");
    }

    @Test
    void ignoringAKeyTheFileDoesNotHaveChangesNothing() throws IOException {
        writeYaml("game:\n  hub-world: lobby\n");

        RenamedKeyClass loaded = new RenamedKeyClass().load(target, Set.of("storm.damage-per-second"));

        assertThat(loaded.hubWorld).isEqualTo("lobby");
        assertThat(loaded.damage).isEqualTo(1.0);
    }

    @Test
    void ignoringABlockTakesEverythingUnderItWithIt() throws IOException {
        // The unit an ignore is worth expressing in is a field, and a field may
        // own a whole block. Half a block read from the file and half from the
        // defaults is a shape nobody asked for.
        writeYaml("""
                database:
                  engine: H2
                  mysql:
                    hostname: theirs
                    port: 9999
                    password: theirs
                """);

        MovedSubtreeClass loaded = new MovedSubtreeClass().load(target, Set.of("database"));

        assertThat(loaded.database.engine()).isEqualTo(MovedSubtreeClass.Engine.MYSQL);
        assertThat(loaded.database.mysql().hostname()).isEqualTo("db.default");
        assertThat(loaded.database.mysql().port()).isEqualTo(3306);
    }

    @Test
    void anIgnoreIsAppliedAfterAMoveHasHappened() throws IOException {
        // Otherwise a declared move would put a value back into a key the
        // caller has just decided must not come from the file.
        writeYaml("""
                mysql:
                  hostname: theirs
                  port: 9999
                  password: theirs
                """);

        MovedSubtreeClass loaded = new MovedSubtreeClass().load(target, Set.of("database.mysql"));

        assertThat(loaded.database.mysql().hostname())
                .as("the move landed there and the ignore then took it away again")
                .isEqualTo("db.default");
    }

    @Test
    void ignoringNothingIsAnOrdinaryLoad() throws IOException {
        writeYaml("storm:\n  damage-per-second: 6.0\n");

        RenamedKeyClass loaded = new RenamedKeyClass().load(target, Set.of());

        assertThat(loaded.damage).isEqualTo(6.0);
    }

    // ===== declaredKeys =====

    @Test
    void aClassCanSayWhichKeysItsFieldsClaim() throws IOException {
        // The caller deciding what to ignore has to work in the same units the
        // fields do, and only the class knows what those are.
        assertThat(new RenamedKeyClass().declaredKeys())
                .containsExactly("storm.damage-per-second", "hud.boss-bar.colour", "game.hub-world");
    }

    @Test
    void aKeyDerivedFromItsFieldNameIsInThereTheWayItIsWritten() throws IOException {
        assertThat(new org.avarion.yaml.testClasses.BareKeyClass().declaredKeys())
                .containsExactly("server_name", "max_players", "debug");
    }
}
