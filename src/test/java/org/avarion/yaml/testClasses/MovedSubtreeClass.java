package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;
import org.avarion.yaml.YamlRename;

/**
 * The shape that no field-level alias can express: a whole block that moved one
 * level down, into a component of a record that did not exist before.
 *
 * <p>The old file had a top-level {@code mysql:} block. The block is now the
 * {@code mysql} component inside {@code database}, beside an {@code engine} the
 * old file never had — so the move has to be declared against the block, not
 * against any one field.
 */
@YamlFile
@YamlRename(from = "mysql", to = "database.mysql")
public class MovedSubtreeClass extends YamlFileInterface {

    public enum Engine {MYSQL, H2}

    public record Credentials(String hostname, int port, String password) {
    }

    public record Database(Engine engine, Credentials mysql) {
    }

    @YamlKey("database")
    public Database database = new Database(Engine.MYSQL, new Credentials("db.default", 3306, "secret"));
}
