# YamlAnnotations

A simple, annotation-based YAML configuration library for Java. Built for Minecraft plugins, but works with any Java application.

## Features

- **Annotation-driven** - Define your config structure with simple annotations
- **Type-safe** - Full support for primitives, collections, maps, enums, UUIDs, and Java Records
- **Nested structures** - Use dot notation (`"database.host"`) for hierarchical configs
- **Key naming** - Derive keys from field and record component names, in `snake_case` or verbatim
- **Comments** - Add documentation directly in your YAML files, down to individual record components
- **Leniency modes** - Strict or lenient type conversion
- **Zero boilerplate** - No manual parsing or type casting

## Quick Start

### Installation

**Gradle:**
```gradle
repositories {
    maven { url 'https://repo.codemc.io/repository/avarionmc/' }
}

dependencies {
    implementation 'org.avarion:yaml:VERSION'
}
```

**Maven:**
```xml
<repository>
    <id>codemc-repo</id>
    <url>https://repo.codemc.io/repository/avarionmc/</url>
</repository>

<dependency>
    <groupId>org.avarion</groupId>
    <artifactId>yaml</artifactId>
    <version>VERSION</version>
</dependency>
```

Replace `VERSION` with the latest from [CodeMC](https://repo.codemc.io/#browse/browse:maven-public:org%2Favarion%2Fyaml).

### Basic Example

```java
import org.avarion.yaml.*;

@YamlFile(header = "Server Configuration")
public class Config extends YamlFileInterface {

    @YamlComment("Server display name")
    @YamlKey("server.name")
    public String serverName = "My Server";

    @YamlKey("server.port")
    public int port = 25565;

    @YamlComment("List of admin players")
    @YamlKey("admins")
    public List<String> admins = List.of("Notch", "jeb_");
}
```

**Load and use:**
```java
Config config = new Config().load("config.yml");
System.out.println("Server: " + config.serverName);

config.serverName = "Updated Name";
config.save("config.yml");
```

**Generated YAML:**
```yaml
# Server Configuration

server:
  # Server display name
  name: My Server
  port: 25565

# List of admin players
admins:
  - Notch
  - jeb_
```

### Using Java Records

```java
public record DatabaseConfig(String host, int port, String username, String password) {}

public class Config extends YamlFileInterface {

    @YamlKey("database")
    public DatabaseConfig database = new DatabaseConfig("localhost", 3306, "root", "secret");
}
```

```yaml
database:
  host: localhost
  port: 3306
  username: root
  password: secret
```

Record components accept `@YamlComment` and `@YamlKey` too, so every key in the block can be
documented and named independently:

```java
public record DatabaseConfig(
        @YamlComment("Where the database lives") String host,
        @YamlKey("PORT") int portNumber) {}
```

```yaml
database:
  # Where the database lives
  host: localhost
  PORT: 3306
```

Keys the library has to derive from a Java identifier — record components, and fields with a
bare `@YamlKey` — are converted to `snake_case`, so `portNumber` would have become
`port_number` had it not been named explicitly. Use `@YamlFile(naming = Naming.KEEP)` to keep
identifiers exactly as written.

## Documentation

| Document | Description |
|----------|-------------|
| [Getting Started](docs/getting-started.md) | Installation and first steps |
| [Supported Types](docs/types.md) | All supported data types |
| [Annotations Reference](docs/annotations.md) | Detailed annotation documentation |
| [Working with Records](docs/records.md) | Java Records support |
| [Advanced Usage](docs/advanced.md) | Leniency, custom types, edge cases |
| [Examples](docs/examples.md) | Real-world configuration examples |

## Supported Types

- **Primitives:** `int`, `long`, `double`, `float`, `boolean`, `char`, `byte`, `short` (and wrappers)
- **Common types:** `String`, `UUID`, `Enum`
- **Collections:** `List<T>`, `Set<T>`, `Queue<T>`
- **Maps:** `Map<K, V>` with any supported key/value types
- **Records:** Full support for Java Records (including nested records)
- **Custom types:** Any class with a `String` constructor

## Requirements

- **Java 17** or higher (required for Record support)
- SnakeYAML 1.x or 2.x (automatically detected)

## License

This project is open source. See the repository for license details.
