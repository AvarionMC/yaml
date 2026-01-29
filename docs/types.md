# Supported Types

YamlAnnotations supports a wide variety of Java types for seamless YAML serialization and deserialization.

## Primitive Types

All Java primitives and their wrapper classes are supported:

| Primitive | Wrapper | Example YAML |
|-----------|---------|--------------|
| `byte` | `Byte` | `value: 127` |
| `short` | `Short` | `value: 32767` |
| `int` | `Integer` | `value: 42` |
| `long` | `Long` | `value: 9223372036854775807` |
| `float` | `Float` | `value: 3.14` |
| `double` | `Double` | `value: 3.141592653589793` |
| `boolean` | `Boolean` | `value: true` |
| `char` | `Character` | `value: A` |

```java
public class PrimitiveConfig extends YamlFileInterface {
    @YamlKey("count")
    public int count = 0;

    @YamlKey("ratio")
    public double ratio = 0.5;

    @YamlKey("enabled")
    public boolean enabled = true;

    @YamlKey("grade")
    public char grade = 'A';
}
```

## Strings

```java
@YamlKey("message")
public String message = "Hello, World!";
```

```yaml
message: Hello, World!
```

## UUIDs

UUIDs are automatically converted to/from strings:

```java
@YamlKey("player-id")
public UUID playerId = UUID.randomUUID();
```

```yaml
player-id: 123e4567-e89b-12d3-a456-426614174000
```

## Enums

Enums are serialized as uppercase strings:

```java
public enum Difficulty { EASY, NORMAL, HARD }

public class GameConfig extends YamlFileInterface {
    @YamlKey("difficulty")
    public Difficulty difficulty = Difficulty.NORMAL;
}
```

```yaml
difficulty: NORMAL
```

## Collections

### Lists

```java
@YamlKey("allowed-commands")
public List<String> allowedCommands = List.of("help", "home", "spawn");

@YamlKey("lucky-numbers")
public List<Integer> luckyNumbers = List.of(7, 13, 42);
```

```yaml
allowed-commands:
  - help
  - home
  - spawn
lucky-numbers:
  - 7
  - 13
  - 42
```

### Sets

Sets are serialized like lists. If elements are `Comparable`, they are sorted in the output:

```java
@YamlKey("unique-ids")
public Set<Integer> uniqueIds = Set.of(1, 2, 3);
```

```yaml
unique-ids:
  - 1
  - 2
  - 3
```

### Queues

```java
@YamlKey("task-queue")
public Queue<String> taskQueue = new ArrayDeque<>(List.of("task1", "task2"));
```

```yaml
task-queue:
  - task1
  - task2
```

### Empty Collections

Empty collections are preserved as empty (not converted to null):

```java
@YamlKey("empty-list")
public List<String> emptyList = new ArrayList<>();
```

```yaml
empty-list: []
```

## Maps

### Simple Maps

```java
@YamlKey("scores")
public Map<String, Integer> scores = Map.of(
    "player1", 100,
    "player2", 85
);
```

```yaml
scores:
  player1: 100
  player2: 85
```

### Nested Maps

```java
@YamlKey("config")
public Map<String, Map<String, Object>> config = new LinkedHashMap<>();
```

```yaml
config:
  database:
    host: localhost
    port: 3306
  cache:
    enabled: true
    ttl: 3600
```

### Maps with Various Key Types

Map keys can be any supported primitive type:

```java
@YamlKey("level-rewards")
public Map<Integer, String> levelRewards = Map.of(
    10, "Bronze Badge",
    25, "Silver Badge",
    50, "Gold Badge"
);
```

## Java Records

Records provide a clean way to define complex data structures. See [Working with Records](records.md) for detailed documentation.

```java
public record ServerInfo(String name, String ip, int port) {}

public class Config extends YamlFileInterface {
    @YamlKey("server")
    public ServerInfo server = new ServerInfo("Main", "127.0.0.1", 25565);
}
```

```yaml
server:
  name: Main
  ip: 127.0.0.1
  port: 25565
```

## Custom Types with String Constructor

Any class with a `String` constructor can be used:

```java
public class CustomId {
    private final String value;

    public CustomId(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}

public class Config extends YamlFileInterface {
    @YamlKey("custom-id")
    public CustomId customId = new CustomId("abc-123");
}
```

```yaml
custom-id: abc-123
```

## Static Field Constants

For types with public static fields (like Bukkit's `Sound` enum-like classes):

```java
// Assuming Sound has: public static Sound ENTITY_PLAYER_HURT = ...
@YamlKey("hurt-sound")
public Sound hurtSound = Sound.ENTITY_PLAYER_HURT;
```

```yaml
hurt-sound: ENTITY_PLAYER_HURT
```

## Null Values

Null values are preserved:

```java
@YamlKey("optional-value")
public String optionalValue = null;
```

```yaml
optional-value: null
```

**Note:** Primitive types (`int`, `boolean`, etc.) cannot be null. Use wrapper classes (`Integer`, `Boolean`) if you need nullable values.

## Type Conversion with Leniency

See [Leniency Modes](advanced.md#leniency-modes) for how type conversion behaves in strict vs lenient modes.
