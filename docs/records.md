# Working with Java Records

Java Records provide a clean, immutable way to define data structures. YamlAnnotations fully supports records for both serialization and deserialization.

## Requirements

- Java 17 or higher

## Basic Record Usage

### Defining a Record

```java
public record ServerInfo(String name, String address, int port) {}
```

### Using Records in Configuration

```java
public class Config extends YamlFileInterface {

    @YamlKey("server")
    public ServerInfo server = new ServerInfo("Main Server", "127.0.0.1", 25565);
}
```

### Generated YAML

```yaml
server:
  name: Main Server
  address: 127.0.0.1
  port: 25565
```

## Nested Records

Records can contain other records:

```java
public record Address(String street, String city, int zipCode) {}

public record Person(String name, int age, Address address) {}

public class Config extends YamlFileInterface {

    @YamlKey("contact")
    public Person contact = new Person(
        "John Doe",
        30,
        new Address("123 Main St", "Springfield", 12345)
    );
}
```

```yaml
contact:
  name: John Doe
  age: 30
  address:
    street: 123 Main St
    city: Springfield
    zipCode: 12345
```

## Maps with Record Values

Use `Map<String, RecordType>` for named collections of records:

```java
public record Player(String displayName, int level, int experience) {}

public class GameConfig extends YamlFileInterface {

    @YamlKey("players")
    public Map<String, Player> players = new LinkedHashMap<>();

    public GameConfig() {
        players.put("player1", new Player("Alice", 50, 125000));
        players.put("player2", new Player("Bob", 42, 98000));
    }
}
```

```yaml
players:
  player1:
    displayName: Alice
    level: 50
    experience: 125000
  player2:
    displayName: Bob
    level: 42
    experience: 98000
```

## Lists of Records

Use `List<RecordType>` for ordered collections:

```java
public record Task(String name, String description, boolean completed) {}

public class TodoConfig extends YamlFileInterface {

    @YamlKey("tasks")
    public List<Task> tasks = new ArrayList<>();

    public TodoConfig() {
        tasks.add(new Task("Buy groceries", "Milk, eggs, bread", false));
        tasks.add(new Task("Call mom", "Weekly check-in", true));
    }
}
```

```yaml
tasks:
  - name: Buy groceries
    description: Milk, eggs, bread
    completed: false
  - name: Call mom
    description: Weekly check-in
    completed: true
```

## Records with Collections

Records can contain lists, sets, or maps:

```java
public record Team(String name, List<Person> members, Address headquarters) {}

public class Config extends YamlFileInterface {

    @YamlKey("team")
    public Team team;

    public Config() {
        List<Person> members = List.of(
            new Person("Alice", 28, new Address("123 St", "City A", 11111)),
            new Person("Bob", 35, new Address("456 Ave", "City B", 22222))
        );
        team = new Team("Alpha Team", members, new Address("1 HQ Blvd", "Capital", 10000));
    }
}
```

```yaml
team:
  name: Alpha Team
  members:
    - name: Alice
      age: 28
      address:
        street: 123 St
        city: City A
        zipCode: 11111
    - name: Bob
      age: 35
      address:
        street: 456 Ave
        city: City B
        zipCode: 22222
  headquarters:
    street: 1 HQ Blvd
    city: Capital
    zipCode: 10000
```

## Records with Maps

```java
public record PlayerStats(
    String name,
    Map<String, Integer> skills,
    Map<String, String> metadata
) {}

public class Config extends YamlFileInterface {

    @YamlKey("player")
    public PlayerStats player = new PlayerStats(
        "Hero",
        Map.of("strength", 10, "agility", 8, "wisdom", 12),
        Map.of("guild", "Warriors", "rank", "Captain")
    );
}
```

```yaml
player:
  name: Hero
  skills:
    strength: 10
    agility: 8
    wisdom: 12
  metadata:
    guild: Warriors
    rank: Captain
```

## Null Values in Records

Record components can be null (except primitives):

```java
public record OptionalInfo(String required, String optional) {}

public class Config extends YamlFileInterface {

    @YamlKey("info")
    public OptionalInfo info = new OptionalInfo("This is required", null);
}
```

```yaml
info:
  required: This is required
  optional: null
```

**Note:** Primitive record components (`int`, `boolean`, etc.) cannot be null. Use wrapper types (`Integer`, `Boolean`) if you need nullable values.

## Empty Collections in Records

Empty collections are preserved:

```java
public record EmptyTeam(String name, List<Person> members) {}

public class Config extends YamlFileInterface {

    @YamlKey("team")
    public EmptyTeam team = new EmptyTeam("New Team", List.of());
}
```

```yaml
team:
  name: New Team
  members: []
```

## Complex Real-World Example

```java
// Define your domain records
public record DatabaseConfig(String host, int port, String database, Credentials credentials) {}
public record Credentials(String username, String password) {}
public record CacheConfig(boolean enabled, int ttlSeconds, int maxSize) {}
public record ServerConfig(String name, int port, List<String> allowedOrigins) {}

// Main configuration
public class AppConfig extends YamlFileInterface {

    @YamlComment("Database connection settings")
    @YamlKey("database")
    public DatabaseConfig database = new DatabaseConfig(
        "localhost",
        5432,
        "myapp",
        new Credentials("admin", "secret")
    );

    @YamlComment("Cache configuration")
    @YamlKey("cache")
    public CacheConfig cache = new CacheConfig(true, 3600, 1000);

    @YamlComment("Web server settings")
    @YamlKey("server")
    public ServerConfig server = new ServerConfig(
        "MyApp Server",
        8080,
        List.of("http://localhost:3000", "https://myapp.com")
    );
}
```

```yaml
# Database connection settings
database:
  host: localhost
  port: 5432
  database: myapp
  credentials:
    username: admin
    password: secret

# Cache configuration
cache:
  enabled: true
  ttlSeconds: 3600
  maxSize: 1000

# Web server settings
server:
  name: MyApp Server
  port: 8080
  allowedOrigins:
    - http://localhost:3000
    - https://myapp.com
```

## Records vs @YamlMap

| Feature       | Records               | @YamlMap                 |
|---------------|-----------------------|--------------------------|
| Syntax        | Clean, declarative    | Verbose, imperative      |
| Immutability  | Immutable by design   | Mutable                  |
| Boilerplate   | Minimal               | Requires processor class |
| Complex logic | Limited               | Full control             |
| Validation    | Constructor only      | Custom in processor      |
| Best for      | Data transfer objects | Complex transformations  |

**Recommendation:** Use Records for most cases. Use `@YamlMap` only when you need custom serialization logic.
