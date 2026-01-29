# Annotations Reference

This document describes all annotations provided by YamlAnnotations.

## @YamlFile

Class-level annotation for configuring the YAML file behavior.

```java
@YamlFile(
    header = "Configuration file header",
    fileName = "config.yml",
    lenient = Leniency.STRICT
)
public class MyConfig extends YamlFileInterface {
    // ...
}
```

### Attributes

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `header` | `String` | `""` | Comment text that appears at the top of the YAML file |
| `fileName` | `String` | `"config.yml"` | Default filename when using `load(plugin)` or `save(plugin)` |
| `lenient` | `Leniency` | `STRICT` | Default leniency mode for all fields |

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

---

## @YamlKey

Field-level annotation that maps a Java field to a YAML key path.

```java
@YamlKey(value = "path.to.key", lenient = Leniency.UNDEFINED)
public String myField = "default";
```

### Attributes

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `value` | `String` | *required* | The YAML key path (supports dot notation for nesting) |
| `lenient` | `Leniency` | `UNDEFINED` | Leniency mode for this specific field |

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

---

## @YamlMap

Field-level annotation for custom map processing. Use this when you need complete control over how complex objects are serialized/deserialized.

```java
@YamlMap(value = "yaml-key", processor = MyProcessor.class)
public Map<String, MyObject> myMap = new HashMap<>();
```

### Attributes

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `value` | `String` | *required* | The YAML key path for the map |
| `processor` | `Class<?>` | *required* | A class implementing `YamlMapProcessor` |

### Creating a Processor

```java
public class PlayerProcessor implements YamlMap.YamlMapProcessor<GameConfig> {

    @Override
    public void read(GameConfig config, String key, Map<String, Object> value) {
        // Called for each entry when loading from YAML
        String name = (String) value.get("name");
        int score = (Integer) value.get("score");
        config.players.put(key, new Player(name, score));
    }

    @Override
    public Map<String, Object> write(GameConfig config, String key, Object value) {
        // Called for each entry when saving to YAML
        Player player = (Player) value;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", player.getName());
        map.put("score", player.getScore());
        return map;
    }
}
```

### Complete Example

```java
public class Player {
    private final String name;
    private final int score;

    public Player(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String getName() { return name; }
    public int getScore() { return score; }
}

public class GameConfig extends YamlFileInterface {

    @YamlMap(value = "players", processor = PlayerProcessor.class)
    public Map<String, Player> players = new LinkedHashMap<>();

    public GameConfig() {
        players.put("alice", new Player("Alice", 1000));
        players.put("bob", new Player("Bob", 850));
    }
}
```

YAML output:

```yaml
players:
  alice:
    name: Alice
    score: 1000
  bob:
    name: Bob
    score: 850
```

### When to Use @YamlMap vs Records

| Use Case | Recommendation |
|----------|----------------|
| Simple data structures | Use [Records](records.md) - cleaner and automatic |
| Complex transformation logic | Use `@YamlMap` with custom processor |
| Legacy classes you can't modify | Use `@YamlMap` with custom processor |
| Need validation during load | Use `@YamlMap` with custom processor |

---

## Annotation Restrictions

### Field Requirements

- Fields with `@YamlKey` or `@YamlMap` **cannot be `final`**
- A field cannot have both `@YamlKey` and `@YamlMap` annotations

### Valid vs Invalid

```java
// ✓ Valid
@YamlKey("setting")
public int setting = 0;

// ✗ Invalid - final field
@YamlKey("constant")
public final int constant = 42;  // Throws FinalAttribute exception

// ✗ Invalid - both annotations
@YamlKey("data")
@YamlMap(value = "data", processor = MyProcessor.class)
public Map<String, Object> data;  // Throws IllegalStateException
```
