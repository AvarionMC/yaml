package org.avarion.yaml;

import org.avarion.yaml.testClasses.BlockedRenameClass;
import org.avarion.yaml.testClasses.InheritedRenameClass;
import org.avarion.yaml.testClasses.MovedSubtreeClass;
import org.avarion.yaml.testClasses.MovedSubtreeClass.Engine;
import org.avarion.yaml.testClasses.OddRenameClass;
import org.avarion.yaml.testClasses.RenamedKeyClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.LogRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Declared renames: where a setting used to live, said once in the class.
 *
 * <p>Without them a key that moves between releases is not merely unread. The
 * file is written back from the fields, so the old key is dropped and whatever
 * was under it goes too — which is how a production database lost its
 * credentials to a routine upgrade. Declaring the move turns that into a
 * migration: the value is carried to the new key before any field reads it, and
 * the write-back then persists it there.
 */
class RenameTests extends TestCommon {

    /** Everything the library said during the load. */
    private List<String> said() {
        return logs.stream().map(LogRecord::getMessage).toList();
    }

    // ===== Field-level: @YamlKey(previously = ...) =====

    @Test
    void aValueUnderTheOldKeyIsReadIntoTheNewOne() throws IOException {
        writeYaml("""
                zone:
                  damage-per-second: 6.0
                """);

        RenamedKeyClass loaded = new RenamedKeyClass().load(target);

        assertThat(loaded.damage).isEqualTo(6.0);
    }

    @Test
    void andTheWriteBackPersistsItUnderTheNewKey() throws IOException {
        writeYaml("""
                zone:
                  damage-per-second: 6.0
                """);

        new RenamedKeyClass().load(target).save(target);

        assertThat(readFile())
                .as("a real migration, not a read-through: the next release need not know")
                .contains("damage-per-second: 6.0")
                .doesNotContain("zone:");
    }

    @Test
    void theMoveIsSaidOutLoud() throws IOException {
        writeYaml("zone:\n  damage-per-second: 6.0\n");

        new RenamedKeyClass().load(target);

        assertThat(said()).anySatisfy(line ->
                assertThat(line).contains("zone.damage-per-second").contains("storm.damage-per-second"));
    }

    @Test
    void anyOfSeveralOldNamesIsAccepted() throws IOException {
        writeYaml("bar:\n  colour: BLUE\n");

        RenamedKeyClass loaded = new RenamedKeyClass().load(target);

        assertThat(loaded.colour).isEqualTo("BLUE");
    }

    @Test
    void theOldestNameLosesToANewerOne() throws IOException {
        // Two moves in the field's history and a file stuck at the older one is
        // one thing; a file holding both is a file that was hand-edited across
        // an upgrade, and the later spelling is the better guess at intent.
        writeYaml("""
                zone:
                  bar:
                    colour: GREEN
                bar:
                  colour: BLUE
                """);

        RenamedKeyClass loaded = new RenamedKeyClass().load(target);

        assertThat(loaded.colour).isEqualTo("GREEN");
    }

    @Test
    void theCurrentKeyBeatsAnOldOneThatIsAlsoStillThere() throws IOException {
        writeYaml("""
                storm:
                  damage-per-second: 2.0
                zone:
                  damage-per-second: 6.0
                """);

        RenamedKeyClass loaded = new RenamedKeyClass().load(target);

        assertThat(loaded.damage)
                .as("the key this release actually documents is the one they meant")
                .isEqualTo(2.0);
    }

    @Test
    void andThatIsSaidOutLoudToo() throws IOException {
        writeYaml("storm:\n  damage-per-second: 2.0\nzone:\n  damage-per-second: 6.0\n");

        new RenamedKeyClass().load(target);

        assertThat(said()).anySatisfy(line ->
                assertThat(line).contains("zone.damage-per-second").contains("storm.damage-per-second"));
    }

    @Test
    void aFileAlreadyAtTheNewKeyIsSilent() throws IOException {
        writeYaml("storm:\n  damage-per-second: 2.0\n");

        RenamedKeyClass loaded = new RenamedKeyClass().load(target);

        assertThat(loaded.damage).isEqualTo(2.0);
        assertThat(said()).isEmpty();
    }

    @Test
    void aFileWithNeitherKeyIsSilentAndKeepsTheDefault() throws IOException {
        writeYaml("game:\n  hub-world: lobby\n");

        RenamedKeyClass loaded = new RenamedKeyClass().load(target);

        assertThat(loaded.damage).isEqualTo(1.0);
        assertThat(loaded.hubWorld).isEqualTo("lobby");
        assertThat(said()).isEmpty();
    }

    @Test
    void whatMovedIsOnTheRecord() throws IOException {
        writeYaml("zone:\n  damage-per-second: 6.0\n");

        RenamedKeyClass loaded = new RenamedKeyClass().load(target);

        assertThat(loaded.renamesApplied())
                .containsEntry("zone.damage-per-second", "storm.damage-per-second")
                .hasSize(1);
    }

    @Test
    void theRecordOfMovesIsAReportNotAThingToEdit() throws IOException {
        // What the load did is not negotiable after the fact, and handing out the live map
        // would let a caller quietly rewrite it.
        writeYaml("""
                zone:
                  damage-per-second: 6.0
                """);

        Map<String, String> applied = new RenamedKeyClass().load(target).renamesApplied();

        assertThatThrownBy(applied::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aLoadThatMovedNothingHasAnEmptyRecord() throws IOException {
        writeYaml("game:\n  hub-world: lobby\n");

        assertThat(new RenamedKeyClass().load(target).renamesApplied()).isEmpty();
    }

    // ===== Class-level: @YamlRename, for a block that moved =====

    @Test
    void aWholeBlockCanMoveIntoAKeyThatDidNotExist() throws IOException {
        // The incident, in miniature: every credential the operator had was
        // under a top-level mysql block, and the block is now one component of
        // a record one level down.
        writeYaml("""
                mysql:
                  hostname: db.internal
                  port: 3307
                  password: hunter2
                """);

        MovedSubtreeClass loaded = new MovedSubtreeClass().load(target);

        assertThat(loaded.database.mysql().hostname()).isEqualTo("db.internal");
        assertThat(loaded.database.mysql().port()).isEqualTo(3307);
        assertThat(loaded.database.mysql().password()).isEqualTo("hunter2");
        assertThat(loaded.database.engine())
                .as("the old file never had an engine, so the block it moved into keeps its default")
                .isEqualTo(Engine.MYSQL);
    }

    @Test
    void andTheUpgradedFileHoldsThemAtTheNewPath() throws IOException {
        writeYaml("""
                mysql:
                  hostname: db.internal
                  port: 3307
                  password: hunter2
                """);

        new MovedSubtreeClass().load(target).save(target);

        String written = readFile();
        assertThat(written).contains("database:").contains("hostname: db.internal");
        assertThat(written.indexOf("mysql:"))
                .as("the only mysql: left is the one nested under database:")
                .isGreaterThan(written.indexOf("database:"));
    }

    @Test
    void theBlockThatMovedIsOnTheRecord() throws IOException {
        writeYaml("mysql:\n  hostname: db.internal\n  port: 3307\n  password: p\n");

        MovedSubtreeClass loaded = new MovedSubtreeClass().load(target);

        assertThat(loaded.renamesApplied()).containsEntry("mysql", "database.mysql");
    }

    @Test
    void aBlockAlreadyAtItsNewHomeWins() throws IOException {
        writeYaml("""
                database:
                  mysql:
                    hostname: current.host
                    port: 3306
                    password: p
                mysql:
                  hostname: stale.host
                  port: 3307
                  password: q
                """);

        MovedSubtreeClass loaded = new MovedSubtreeClass().load(target);

        assertThat(loaded.database.mysql().hostname()).isEqualTo("current.host");
        assertThat(said()).anySatisfy(line -> assertThat(line).contains("mysql").contains("database.mysql"));
    }

    @Test
    void aFileWithoutTheOldBlockIsUntouchedAndSilent() throws IOException {
        writeYaml("""
                database:
                  engine: H2
                """);

        MovedSubtreeClass loaded = new MovedSubtreeClass().load(target);

        assertThat(loaded.database.engine()).isEqualTo(Engine.H2);
        assertThat(loaded.database.mysql().hostname()).isEqualTo("db.default");
        assertThat(said()).isEmpty();
        assertThat(loaded.renamesApplied()).isEmpty();
    }

    @Test
    void anEmptyFileIsSilent() throws IOException {
        writeYaml("");

        MovedSubtreeClass loaded = new MovedSubtreeClass().load(target);

        assertThat(loaded.database.mysql().hostname()).isEqualTo("db.default");
        assertThat(said()).isEmpty();
    }

    @Test
    void aBaseClassCanDeclareAMoveAndItsSubclassAnother() throws IOException {
        // Coarse to fine, base first: 'old.value' has to become 'middle.value' before the
        // subclass's declaration has anything to find.
        writeYaml("old:\n  value: carried\n");

        InheritedRenameClass.Derived loaded = new InheritedRenameClass.Derived().load(target);

        assertThat(loaded.value).isEqualTo("carried");
        assertThat(loaded.renamesApplied())
                .containsEntry("old", "middle")
                .containsEntry("middle.value", "current.value");
    }

    // ===== Declarations that must do nothing =====

    @Test
    void aBlankOldNameIsIgnored() throws IOException {
        writeYaml("kept:\n  blank: from-file\n");

        OddRenameClass loaded = new OddRenameClass().load(target);

        assertThat(loaded.blank).isEqualTo("from-file");
        assertThat(loaded.renamesApplied()).isEmpty();
        assertThat(said()).isEmpty();
    }

    @Test
    void aKeyDeclaredAsItsOwnFormerSelfIsIgnored() throws IOException {
        writeYaml("kept:\n  circular: from-file\n");

        OddRenameClass loaded = new OddRenameClass().load(target);

        assertThat(loaded.circular).isEqualTo("from-file");
        assertThat(loaded.renamesApplied()).isEmpty();
    }

    @Test
    void anOldKeyWrittenDownEmptyIsNothingToCarry() throws IOException {
        // 'old.empty-target:' with nothing after it is not a value, and moving a nothing to the
        // new key would only replace a real default with it.
        writeYaml("old:\n  empty-target:\n");

        OddRenameClass loaded = new OddRenameClass().load(target);

        assertThat(loaded.emptyTarget).isEqualTo("default");
        assertThat(loaded.renamesApplied()).isEmpty();
        assertThat(said()).isEmpty();
    }

    @Test
    void aBlankDestinationIsIgnored() throws IOException {
        writeYaml("old:\n  nowhere: mine\n");

        OddRenameClass loaded = new OddRenameClass().load(target);

        assertThat(loaded.renamesApplied()).isEmpty();
        assertThat(said()).isEmpty();
    }

    @Test
    void aNewKeyLeftEmptyIsNotAValueToBeBeatenBy() throws IOException {
        // Clearing the new line out is how somebody empties a setting they do not want, and it
        // is not the same as having chosen something. The old value still has somewhere to go.
        writeYaml("""
                storm:
                  damage-per-second:
                zone:
                  damage-per-second: 6.0
                """);

        RenamedKeyClass loaded = new RenamedKeyClass().load(target);

        assertThat(loaded.damage).isEqualTo(6.0);
    }

    @Test
    void anOldKeyBehindAValueRatherThanABlockIsNotFound() throws IOException {
        // 'old' is a setting in this file, so 'old.empty-target' names nothing.
        writeYaml("old: just-a-string\nkept:\n  empty-target: mine\n");

        OddRenameClass loaded = new OddRenameClass().load(target);

        assertThat(loaded.emptyTarget).isEqualTo("mine");
        assertThat(loaded.renamesApplied()).isEmpty();
    }

    @Test
    void aMoveIntoAPathBlockedByAValueIsRefusedRatherThanForced() throws IOException {
        // 'wing' is a setting in this file, so there is nowhere to build 'wing.nested'.
        // Overwriting it would destroy one of their settings to rescue another.
        writeYaml("""
                wing: solid
                loose: mine
                """);

        BlockedRenameClass loaded = new BlockedRenameClass().load(target);

        assertThat(loaded.value)
                .as("nothing was carried, so the field is left at its default")
                .isEqualTo("default");
        assertThat(loaded.renamesApplied())
                .as("and the old key is not claimed as handled, so it can still be reported")
                .isEmpty();
        assertThat(said()).anySatisfy(line ->
                assertThat(line).contains("loose").contains("cannot be created"));
    }
}
