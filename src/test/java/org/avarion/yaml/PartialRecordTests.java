package org.avarion.yaml;

import org.avarion.yaml.testClasses.PackagePrivateRecordClass;
import org.avarion.yaml.testClasses.PartialRecordClass;
import org.avarion.yaml.testClasses.PartialRecordClass.Engine;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A record component the file does not set keeps its default.
 *
 * <p>That is already how a plain field behaves — a key the file omits leaves
 * the field alone — and there is no reason for the rule to stop at a record's
 * edge. A block the file mentions at all used to be taken whole: every
 * component the file did not name came out {@code null}, or, when the component
 * was a primitive, refused to load. So a file naming one setting inside a block
 * silently lost the rest of it.
 *
 * <p>A key that IS in the file and IS null still means null, and still refuses
 * to go into a primitive. Not writing something down and writing down nothing
 * are different statements, and only the first one is a question about
 * defaults.
 */
class PartialRecordTests extends TestCommon {

    @Test
    void aComponentTheFileOmitsKeepsItsDefault() throws IOException {
        writeYaml("""
                database:
                  mysql:
                    hostname: moved-host
                    port: 1234
                    password: hunter2
                """);

        PartialRecordClass loaded = new PartialRecordClass().load(target);

        assertThat(loaded.database.engine())
                .as("the file never mentions engine, so the shipped default stands")
                .isEqualTo(Engine.MYSQL);
        assertThat(loaded.database.mysql().hostname()).isEqualTo("moved-host");
    }

    @Test
    void thatHoldsInsideANestedRecordToo() throws IOException {
        writeYaml("""
                database:
                  engine: H2
                  mysql:
                    hostname: moved-host
                """);

        PartialRecordClass loaded = new PartialRecordClass().load(target);

        assertThat(loaded.database.engine()).isEqualTo(Engine.H2);
        assertThat(loaded.database.mysql().hostname()).isEqualTo("moved-host");
        assertThat(loaded.database.mysql().port())
                .as("a primitive component the file omits is the default, not a failure to load")
                .isEqualTo(3306);
        assertThat(loaded.database.mysql().password()).isEqualTo("secret");
    }

    @Test
    void aBlockTheFileOmitsEntirelyIsStillTheWholeDefault() throws IOException {
        writeYaml("unrelated: changed\n");

        PartialRecordClass loaded = new PartialRecordClass().load(target);

        assertThat(loaded.unrelated).isEqualTo("changed");
        assertThat(loaded.database.engine()).isEqualTo(Engine.MYSQL);
        assertThat(loaded.database.mysql().hostname()).isEqualTo("db.default");
    }

    @Test
    void aKeyWrittenDownAsNullIsStillNull() throws IOException {
        // Writing 'engine:' with nothing after it is a statement, and it is not
        // the same statement as leaving the line out.
        writeYaml("""
                database:
                  engine:
                  mysql:
                    hostname: moved-host
                """);

        PartialRecordClass loaded = new PartialRecordClass().load(target);

        assertThat(loaded.database.engine()).isNull();
    }

    @Test
    void aPrimitiveWrittenDownAsNullStillRefusesToLoad() throws IOException {
        writeYaml("""
                database:
                  mysql:
                    hostname: moved-host
                    port:
                """);

        assertThatThrownBy(() -> new PartialRecordClass().load(target))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("port");
    }

    @Test
    void aFallbackThatRefusesToAnswerIsTreatedAsNoAnswer() throws IOException {
        // A record may write its own accessor, and that accessor may throw. Asking it what the
        // default was is a convenience, so a refusal costs the convenience and nothing else --
        // the component takes the road it took before there were fallbacks at all.
        writeYaml("awkward:\n  other: from-file\n");

        AwkwardHolder loaded = new AwkwardHolder().load(target);

        assertThat(loaded.awkward.other()).isEqualTo("from-file");
        assertThat(loaded.awkward.temperamental()).isNull();
    }

    /**
     * A record whose accessor refuses to answer for one particular value — the one the default
     * instance is built with, so it is exactly the fallback probe that gets refused.
     */
    public record Awkward(String temperamental, String other) {
        static final String REFUSES = "asking me about this one throws";

        @Override
        public String temperamental() {
            if (REFUSES.equals(temperamental)) {
                throw new IllegalStateException("this accessor does not answer");
            }
            return temperamental;
        }
    }

    @YamlFile
    public static class AwkwardHolder extends YamlFileInterface {
        @YamlKey("awkward")
        public Awkward awkward = new Awkward(Awkward.REFUSES, "default");
    }

    @Test
    void theWriteBackFillsInWhatTheFileLeftOut() throws IOException {
        writeYaml("""
                database:
                  mysql:
                    hostname: moved-host
                """);

        new PartialRecordClass().load(target).save(target);

        assertThat(readFile())
                .as("the defaults that filled the gaps are now written down")
                .contains("engine: 'MYSQL'")
                .contains("hostname: moved-host")
                .contains("port: 3306")
                .contains("password: secret");
    }

    @Test
    void aRecordThePluginKeepsToItsOwnPackageStillKeepsItsDefaults() throws IOException {
        // The record's canonical constructor and accessors are reachable only by opening them
        // reflectively; the fallback for an omitted component has to survive that.
        writeYaml("""
                credentials:
                  hostname: from-file
                """);

        PackagePrivateRecordClass loaded = new PackagePrivateRecordClass().load(target);

        assertThat(loaded.hostname()).isEqualTo("from-file");
        assertThat(loaded.port())
                .as("the default for the omitted component came through the opened accessor")
                .isEqualTo(3306);
    }
}
