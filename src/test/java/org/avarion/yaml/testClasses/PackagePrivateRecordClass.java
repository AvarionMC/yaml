package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

/**
 * A settings class whose record is package-private — the shape a plugin takes when it keeps a
 * credentials type to itself. The library can reach the record's canonical constructor and its
 * accessors only by opening them reflectively, so this class is what keeps those
 * {@code setAccessible} calls honest: without them, a partial load of this record cannot consult
 * the defaults it is supposed to keep.
 */
@YamlFile
public class PackagePrivateRecordClass extends YamlFileInterface {

    record Credentials(String hostname, int port) {
    }

    @YamlKey("credentials")
    Credentials credentials = new Credentials("db.default", 3306);

    public String hostname() {
        return credentials.hostname();
    }

    public int port() {
        return credentials.port();
    }
}
