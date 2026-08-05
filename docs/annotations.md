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

| Attribute | Type       | Default     | Description                                                       |
|-----------|------------|-------------|--------------------------------------------------------------------|
| `value`   | `String`   | `""`        | The YAML key path (supports dot notation for nesting). Empty means "derive it from the field name" |
| `lenient` | `Leniency` | `UNDEFINED` | Leniency mode for this specific field                             |

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
