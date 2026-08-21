package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

/**
 * A record inside a record, both with defaults — the shape a plugin uses for a
 * credentials block: which engine, and how to log in to it.
 */
@YamlFile
public class PartialRecordClass extends YamlFileInterface {

    public enum Engine {MYSQL, H2}

    public record Credentials(String hostname, int port, String password) {
    }

    public record Database(Engine engine, Credentials mysql) {
    }

    @YamlKey("database")
    public Database database = new Database(Engine.MYSQL, new Credentials("db.default", 3306, "secret"));

    @YamlKey("unrelated")
    public String unrelated = "untouched";
}
