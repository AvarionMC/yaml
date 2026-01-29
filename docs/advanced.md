# Advanced Usage

This guide covers advanced features and edge cases in YamlAnnotations.

## Leniency Modes

Leniency controls how strictly type conversions are enforced during YAML loading.

### Leniency Levels

| Level | Description |
|-------|-------------|
| `STRICT` | Exact type matching required; throws exceptions on mismatches |
| `LENIENT` | Attempts automatic type coercion when possible |
| `UNDEFINED` | Inherits from parent (`@YamlFile` or defaults to `STRICT`) |

### Setting Leniency

**Class-level (applies to all fields):**

```java
@YamlFile(lenient = Leniency.LENIENT)
public class Config extends YamlFileInterface {
    // All fields use LENIENT mode by default
}
```

**Field-level (overrides class setting):**

```java
@YamlFile(lenient = Leniency.STRICT)
public class Config extends YamlFileInterface {

    @YamlKey("strict-value")
    public int strictValue = 0;  // Uses STRICT

    @YamlKey(value = "lenient-value", lenient = Leniency.LENIENT)
    public char lenientValue = 'A';  // Uses LENIENT
}
```

### Leniency Behavior Examples

#### Character Conversion

```yaml
char-field: "Hello"
```

| Mode | Result |
|------|--------|
| `STRICT` | Throws `IOException` - string too long |
| `LENIENT` | Takes first character: `'H'` |

#### Float Precision

```yaml
float-field: 0.123456789
```

| Mode | Result |
|------|--------|
| `STRICT` | Throws `IOException` - precision loss |
| `LENIENT` | Converts with precision loss: `0.12345679f` |

#### Single Value to Collection

```yaml
list-field: "single-item"
```

| Mode | Result |
|------|--------|
| `STRICT` | Throws `IOException` - expected list |
| `LENIENT` | Wraps in list: `["single-item"]` |

---

## Custom Type Conversion

### Classes with String Constructor

Any class with a `public` constructor that takes a single `String` parameter can be used:

```java
public class Email {
    private final String address;

    public Email(String address) {
        if (!address.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        this.address = address;
    }

    @Override
    public String toString() {
        return address;
    }
}

public class Config extends YamlFileInterface {
    @YamlKey("contact-email")
    public Email contactEmail = new Email("admin@example.com");
}
```

```yaml
contact-email: admin@example.com
```

### Static Field Resolution

For types with public static fields (common in game APIs):

```java
// Given a class like:
public class Sound {
    public static final Sound CLICK = new Sound("click");
    public static final Sound DING = new Sound("ding");
    // ...
}

// You can use it directly:
public class Config extends YamlFileInterface {
    @YamlKey("notification-sound")
    public Sound notificationSound = Sound.DING;
}
```

```yaml
notification-sound: DING
```

The library will:
1. On save: Find the static field name that matches the value
2. On load: Look up the static field by name

---

## Inheritance

Configuration classes can extend other configuration classes:

```java
public abstract class BaseConfig extends YamlFileInterface {
    @YamlKey("version")
    public int version = 1;

    @YamlKey("debug")
    public boolean debug = false;
}

public class PluginConfig extends BaseConfig {
    @YamlKey("plugin.name")
    public String pluginName = "MyPlugin";

    @YamlKey("plugin.enabled")
    public boolean enabled = true;
}
```

```yaml
version: 1
debug: false
plugin:
  name: MyPlugin
  enabled: true
```

---

## Working with Bukkit/Spigot/Paper

### Using Keyed Objects

The library automatically handles Bukkit's `Keyed` interface (like `Sound`, `Material`, etc.):

```java
public class Config extends YamlFileInterface {
    @YamlKey("break-sound")
    public Sound breakSound = Sound.BLOCK_STONE_BREAK;

    @YamlKey("item-type")
    public Material itemType = Material.DIAMOND_SWORD;
}
```

```yaml
break-sound: BLOCK_STONE_BREAK
item-type: DIAMOND_SWORD
```

### Plugin Data Folder Integration

Use the plugin instance for automatic path resolution:

```java
@YamlFile(fileName = "settings.yml")
public class Settings extends YamlFileInterface {
    @YamlKey("setting")
    public String setting = "value";
}

// In your plugin:
public class MyPlugin extends JavaPlugin {
    private Settings settings;

    @Override
    public void onEnable() {
        // Automatically uses: plugins/MyPlugin/settings.yml
        settings = new Settings().load(this);
    }

    @Override
    public void onDisable() {
        settings.save(this);
    }
}
```

---

## Error Handling

### Common Exceptions

| Exception | Cause |
|-----------|-------|
| `IOException` | File read/write errors, type conversion failures |
| `FinalAttribute` | Attempting to use `@YamlKey` on a `final` field |
| `DuplicateKey` | Same key path used multiple times |
| `IllegalStateException` | Both `@YamlKey` and `@YamlMap` on same field |

### Handling Missing Fields

If a YAML file doesn't contain a key, the field keeps its default value:

```java
public class Config extends YamlFileInterface {
    @YamlKey("existing")
    public String existing = "default";  // Loaded from YAML if present

    @YamlKey("missing")
    public String missing = "default";   // Keeps "default" if not in YAML
}
```

### Null Primitives

Primitive types cannot be null. This will throw an exception:

```yaml
# Error: Cannot assign null to primitive
int-field: null
```

Use wrapper types if you need nullable values:

```java
@YamlKey("nullable-int")
public Integer nullableInt = null;  // Works fine
```

---

## Performance Considerations

### File Operations

- `load()` reads the entire file into memory
- `save()` writes the entire file atomically
- For large configurations, consider splitting into multiple files

### Reflection Caching

The library uses reflection for field access. This is cached internally, but for performance-critical applications:

- Minimize the number of annotated fields
- Load configuration once at startup
- Cache the configuration instance

### Collection Types

- `List` → `ArrayList` (preserves insertion order)
- `Set` → `LinkedHashSet` (preserves insertion order)
- `Queue` → `ArrayDeque`
- `Map` → `LinkedHashMap` (preserves insertion order)

---

## Thread Safety

YamlAnnotations is **not thread-safe**. If you need concurrent access:

```java
public class ThreadSafeConfig {
    private final Config config;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ThreadSafeConfig(String path) throws IOException {
        config = new Config().load(path);
    }

    public String getValue() {
        lock.readLock().lock();
        try {
            return config.value;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setValue(String value) {
        lock.writeLock().lock();
        try {
            config.value = value;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
```

---

## Migration from Other Libraries

### From Bukkit's FileConfiguration

Before (Bukkit):
```java
FileConfiguration config = YamlConfiguration.loadConfiguration(file);
String name = config.getString("player.name", "Unknown");
int level = config.getInt("player.level", 1);
```

After (YamlAnnotations):
```java
public class Config extends YamlFileInterface {
    @YamlKey("player.name")
    public String name = "Unknown";

    @YamlKey("player.level")
    public int level = 1;
}

Config config = new Config().load(file);
// Access directly: config.name, config.level
```

### Benefits of Migration

- Type safety at compile time
- IDE auto-completion
- Refactoring support
- Default values in one place
- Self-documenting configuration structure
