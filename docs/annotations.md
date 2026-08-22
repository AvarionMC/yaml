# Annotations Reference

This document describes all annotations provided by YamlAnnotations.

## @YamlFile

Class-level annotation for configuring the YAML file behavior.

```java
@YamlFile(
    header = "Configuration file header",
    fileName = "config.yml",
    lenient = Leniency.LENIENT
)
public class MyConfig extends YamlFileInterface {
    // ...
}
```

### Attributes

| Attribute  | Type       | Default        | Description                                                  |
|------------|------------|----------------|--------------------------------------------------------------|
| `header`   | `String`   | `""`           | Comment text that appears at the top of the YAML file        |
| `fileName` | `String`   | `"config.yml"` | Default filename when using `load(plugin)` or `save(plugin)` |
| `lenient`  | `Leniency` | `LENIENT`      | Default leniency mode for all fields                         |
| `naming`   | `Naming`   | `SNAKE_CASE`   | How keys derived from a Java identifier are spelled          |

### Example with Header

```java
@YamlFile(header = """
    ========================================
    My Plugin Configuration
    ========================================

    Edit this file to customize the plugin.
    Changes require a server restart.
    """)
public class PluginConfig extends YamlFileInterface {
    @YamlKey("version")
    public int version = 1;
}
```

Output:

```yaml
# ========================================
# My Plugin Configuration
# ========================================
#
# Edit this file to customize the plugin.
# Changes require a server restart.

version: 1
```

### Naming Strategy

Keys that the library has to *derive* from a Java identifier are converted according to
`naming`. Keys you spell out yourself in a `@YamlKey("...")` are never touched.

```java
@YamlFile(naming = Naming.KEEP)
public class Config extends YamlFileInterface {
    // ...
}
```

| Strategy     | `modelId` becomes | Notes                                             |
|--------------|-------------------|---------------------------------------------------|
| `SNAKE_CASE` | `model_id`        | The default                                       |
| `KEEP`       | `modelId`         | Use the identifier exactly as written             |

Conversion is acronym-aware: `httpURL` becomes `http_url` and `getHTTPHeader` becomes
`get_http_header`. A trailing digit stays attached to its word, so `modelId2` becomes
`model_id2`.

> **Watch out for consecutive single capitals.** A run of capitals is kept together only when
> a lowercase letter follows it, so `getHTTPHeader` splits sensibly but `allowPvP` becomes
> `allow_pv_p` — each lone capital starts its own word. If that is not the key you want, spell
> it out with `@YamlKey("allow_pvp")`, switch the class to `Naming.KEEP`, or name the
> identifier `allowPvp` so there is only one boundary to find.

`SNAKE_CASE` is the default, so a config whose keys were previously derived (record
components, and fields with a bare `@YamlKey`) changes spelling. Set `naming = Naming.KEEP`
to keep the old file readable.

---

## @YamlKey

Field-level annotation that maps a Java field to a YAML key path.

```java
@YamlKey(value = "path.to.key", lenient = Leniency.UNDEFINED)
public String myField = "default";
```

### Attributes

| Attribute    | Type       | Default     | Description                                                       |
|--------------|------------|-------------|--------------------------------------------------------------------|
| `value`      | `String`   | `""`        | The YAML key path (supports dot notation for nesting). Empty means "derive it from the field name" |
| `lenient`    | `Leniency` | `UNDEFINED` | Leniency mode for this specific field                             |
| `previously` | `String[]` | `{}`        | Keys this setting used to be spelled as, newest first — see [Keys that have moved](#keys-that-have-moved) |

### Bare @YamlKey

Leave the value off to key a field by its own name, run through the class's
[naming strategy](#naming-strategy):

```java
public class Config extends YamlFileInterface {

    @YamlKey
    public String serverName = "Main";      // -> server_name

    @YamlKey("max_players")
    public int maxPlayers = 20;             // -> max_players, exactly as written

    public String scratch = "not config";   // no annotation: never persisted
}
```

```yaml
server_name: Main
max_players: 20
```

A field with no `@YamlKey` at all is still ignored entirely — the annotation is what marks a
field as configuration, and leaving its value blank only delegates the *spelling* of the key.

### Dot Notation for Nested Keys

Use dots to create nested YAML structures:

```java
public class Config extends YamlFileInterface {
    @YamlKey("database.host")
    public String dbHost = "localhost";

    @YamlKey("database.port")
    public int dbPort = 3306;

    @YamlKey("database.credentials.username")
    public String dbUser = "admin";

    @YamlKey("database.credentials.password")
    public String dbPassword = "secret";
}
```

Output:

```yaml
database:
  host: localhost
  port: 3306
  credentials:
    username: admin
    password: secret
```

### Field-Level Leniency

Override the class-level leniency for specific fields:

```java
@YamlFile(lenient = Leniency.STRICT)
public class Config extends YamlFileInterface {

    // Uses class default (STRICT)
    @YamlKey("strict-field")
    public int strictField = 0;

    // Override to LENIENT for this field only
    @YamlKey(value = "lenient-field", lenient = Leniency.LENIENT)
    public char lenientField = 'A';
}
```

### On Record Components

`@YamlKey` also works on the components of a record, where it renames a single key
inside the record's block. This lets a record keep Java naming while the YAML keeps
whatever convention the config already uses:

```java
public record AltarDefinition(
        @YamlKey("modelID") int modelId,
        int searchRadius,
        String name) {}
```

```yaml
altar:
  modelID: 1001      # spelled out, so used verbatim
  search_radius: 8   # derived, so converted
  name: blood
```

Components without a `@YamlKey` have their key derived from the component name via the
[naming strategy](#naming-strategy). The rename applies to reading as well, so the file
round-trips.

Dot notation is **not** supported here — a record component is one key inside the record's
own block, so it cannot spread itself over a nested path. A `@YamlKey` containing a `.` on a
record component throws an `IOException` on both save and load.

---

## Keys that have moved

A key that changes name between releases is not merely unread. `save` writes the file from the
fields, so a key no field claims is dropped on the next write and the value under it goes with
it. Declaring where a setting used to live turns that into a migration: before any field reads
the file, the value is carried to the key it lives under now, and the write-back persists it
there.

### One key: `@YamlKey(previously = ...)`

```java
@YamlKey(value = "storm.damage-per-second", previously = "zone.damage-per-second")
public double damage = 1.0;
```

```yaml
# what the operator has                 # what they get back
zone:                                   storm:
  damage-per-second: 6.0                  damage-per-second: 6.0
```

List several when a setting has moved more than once, most recent first:

```java
@YamlKey(value = "hud.boss-bar.colour", previously = {"zone.bar.colour", "bar.colour"})
public String colour = "RED";
```

The first old name the file actually has is the one used. Each entry is a full path from the
root of the file, in the same dot notation as `value`.

### A whole block: `@YamlRename`

Some moves no field-level alias can express — where the old key's new home is inside a structure
that no single field owns:

```java
@YamlFile
@YamlRename(from = "mysql", to = "database.mysql")
public class Settings extends YamlFileInterface {

    @YamlKey("database")
    public DatabaseSettings database = DatabaseSettings.mysql("avarion");
}
```

```yaml
# what the operator has                 # what they get back
mysql:                                  database:
  hostname: db.internal                   engine: 'MYSQL'
  port: 3307                              mysql:
  password: hunter2                         hostname: db.internal
                                            port: 3307
                                            password: hunter2
```

The credentials that used to be a top-level block are now one component of a record, beside an
`engine` the old file never had. `engine` comes out at its default because
[a record component the file does not set keeps its default](records.md). The annotation is
repeatable, so a class can declare as many moves as its history needs.

### The rules

| Situation                       | What happens                                                         |
|---------------------------------|----------------------------------------------------------------------|
| only the old key is set         | the value is carried across, and the move is logged                  |
| only the current key is set     | nothing happens, silently                                            |
| both are set                    | the current key wins, out loud; the old one is dropped               |
| the old key is set but empty    | nothing happens — an empty line is not a value to carry              |
| neither is set                  | nothing happens, silently                                            |
| the destination path is blocked | the move is refused, out loud, and the old key is left where it is   |

That last one is the case where something on the way to the destination is already a value
rather than a section. Overwriting it would destroy one setting to rescue another, so the move
is refused instead.

Class-level moves are applied before field-level ones — coarse before fine — so a field can name
an old key that only exists once a block has landed. A base class's declarations are applied
before its subclass's.

### What moved

`renamesApplied()` reports what the last load did: each old path the class declares, mapped to
the key that now holds its value. A path is in there whether the value was carried across or the
current key was already set and won — either way the old key was accounted for, which is what
tells a caller not to report it as a setting that vanished for no reason.

```java
Settings settings = new Settings().load(file);
settings.renamesApplied();   // {mysql=database.mysql}
```

### When the name stayed and the value moved on

A rename is about a key. The mirror problem is about a value: a file beats the compile-time
default for every key it has, which is what an operator's choice should do — and is also why a
default that *improves* between releases never reaches anybody whose file already mentions it.

Deciding whether a given line is a considered choice or an untouched copy of an older default
needs to know what that older default was, and only the caller can know that. Acting on the
answer is `load(File, Set<String>)`:

```java
Settings settings = new Settings().load(file, Set.of("loot.tier-items"));
```

An ignored key is not read, so the field keeps what it already holds — for a freshly built
configuration object, this release's default. A following `save` writes that out, because it is
what the field now says.

Name keys the way their fields declare them, which `declaredKeys()` reports:

```java
new Settings().declaredKeys();   // [game.hub-world, loot.tier-items, database, ...]
```

One entry there may own a whole block, and ignoring it takes the block with it. That is
deliberate: half a block read from the file and half from the defaults is a shape nobody asked
for.

Ignores are applied after renames, so a key stays ignored whether the value in it came from the
file directly or was carried there by a declared move.

### Deleting a declaration

Once the files in the wild have been through an upgrade, the write-back has already moved them,
and the declaration is doing nothing. Delete it a release or two later.

Declare a rename only when the meaning did not change with the name. The value is carried across
verbatim, so a key that changed *units* as it changed name — a radius that became a diameter —
must not be declared: it would quietly halve every setting it touched. Those are for a one-off
migration in the calling code, which can do the arithmetic.

---

## @YamlComment

Field-level annotation that adds a comment above the YAML key.

```java
@YamlComment("This comment appears above the key in the YAML file")
@YamlKey("my-key")
public String myField = "value";
```

### Multi-line Comments

```java
@YamlComment("""
    This is a multi-line comment.
    It will appear as multiple comment lines in YAML.
    Use it for detailed explanations.
    """)
@YamlKey("complex-setting")
public int complexSetting = 42;
```

Output:

```yaml
# This is a multi-line comment.
# It will appear as multiple comment lines in YAML.
# Use it for detailed explanations.
complex-setting: 42
```

### Combining with @YamlKey

`@YamlComment` must be used together with `@YamlKey`:

```java
public class Config extends YamlFileInterface {

    @YamlComment("Enable debug mode for verbose logging")
    @YamlKey("debug")
    public boolean debug = false;

    @YamlComment("Maximum number of concurrent connections")
    @YamlKey("max-connections")
    public int maxConnections = 100;
}
```

### On Record Components

Annotate a record's components to comment every key inside the record's block instead of
only the block as a whole:

```java
public record DatabaseConfig(
        @YamlComment("Host the database listens on") String host,
        @YamlComment("Port, usually 3306 for MySQL") int port,
        String database) {}

public class Config extends YamlFileInterface {

    @YamlComment("Database settings")
    @YamlKey("database")
    public DatabaseConfig database = new DatabaseConfig("localhost", 3306, "myapp");
}
```

Output:

```yaml
# Database settings
database:
  # Host the database listens on
  host: localhost
  # Port, usually 3306 for MySQL
  port: 3306
  database: myapp
```

This works at every nesting level, so a record inside a record is commented too.

Records used as **list items** are written without component comments — a list entry starts
after its `-` with nowhere to put a comment, and repeating the same block for every item
would only add noise.

---

## Annotation Restrictions

### Field Requirements

- Fields with `@YamlKey` **cannot be `final`**

### Valid vs Invalid

```java
// ✓ Valid
@YamlKey("setting")
public int setting = 0;

// ✗ Invalid - final field
@YamlKey("constant")
public final int constant = 42;  // Throws FinalAttribute exception
```
