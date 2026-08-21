package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A block of settings that moved, declared on the class it moved within.
 *
 * <p>The file is written back from the fields, so a key no field claims is dropped and the
 * operator's value goes with it. {@link YamlKey#previously()} covers a single setting that
 * changed name. This covers the case it cannot reach: a whole block whose new home is inside a
 * structure that no one field owns.
 *
 * <pre>{@code
 * @YamlFile
 * @YamlRename(from = "mysql", to = "database.mysql")
 * public class Settings extends YamlFileInterface {
 *     @YamlKey("database")
 *     public DatabaseSettings database = DatabaseSettings.mysql("avarion");
 * }
 * }</pre>
 *
 * <p>There, the credentials that used to be a top-level {@code mysql:} block are now the
 * {@code mysql} component of a record, beside an {@code engine} the old file never had. No
 * {@code previously} on any field reaches that: the field is {@code database}, and only part of
 * it moved. Naming the block does.
 *
 * <p>Repeatable, so a class can declare as many moves as its history needs.
 *
 * <h2>What it does</h2>
 * Before any field reads the file, {@code from} and everything under it is moved to {@code to}.
 * Whatever the block does not fill in is left to the defaults, so the record above comes out
 * with the operator's credentials and this release's engine. The write-back then persists the
 * whole thing at the new path, which is what makes the declaration deletable later.
 *
 * <p>Nothing happens when {@code from} is absent. When both {@code from} and {@code to} are
 * present the file has been through a hand-edit, and {@code to} — the key this release
 * documents — wins, out loud.
 *
 * <h2>Order</h2>
 * Class-level moves run before {@link YamlKey#previously()}, coarse before fine, so a field can
 * name an old key that only exists once a block has landed. Within one class they run in
 * declaration order, and a base class's are applied before its subclass's.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(YamlRenames.class)
public @interface YamlRename {

    /** The path the block used to sit at, from the root of the file, dot-separated. */
    @NotNull String from();

    /** Where it sits now, in the same notation. */
    @NotNull String to();
}
