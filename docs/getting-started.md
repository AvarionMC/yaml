# Getting Started

This guide will help you get up and running with YamlAnnotations quickly.

## Installation

### Gradle

```gradle
plugins {
    id 'com.gradleup.shadow'
}

repositories {
    maven { url 'https://repo.codemc.io/repository/avarionmc/' }
}

dependencies {
    implementation 'org.avarion:yaml:VERSION'
}

jar {
    dependsOn shadowJar
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>codemc-repo</id>
        <url>https://repo.codemc.io/repository/avarionmc/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>org.avarion</groupId>
        <artifactId>yaml</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.4.1</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Replace `VERSION` with the latest version from [CodeMC](https://repo.codemc.io/#browse/browse:maven-public:org%2Favarion%2Fyaml).

## Requirements

- **Java 17** or higher (required for Record support)
- SnakeYAML 1.x or 2.x (automatically selected at runtime)

## Your First Configuration Class

### Step 1: Create a Configuration Class

Create a class that extends `YamlFileInterface` and annotate your fields:

```java
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

public class MyConfig extends YamlFileInterface {

    @YamlKey("server.name")
    public String serverName = "My Server";

    @YamlKey("server.port")
    public int port = 25565;

    @YamlKey("server.motd")
    public String motd = "Welcome!";
}
```

### Step 2: Load or Save the Configuration

```java
// Load configuration (creates file with defaults if it doesn't exist)
MyConfig config = new MyConfig().load("config.yml");

// Access values
System.out.println("Server: " + config.serverName);
System.out.println("Port: " + config.port);

// Modify and save
config.serverName = "Updated Server";
config.save("config.yml");
```

### Step 3: The Generated YAML File

```yaml
server:
  name: My Server
  port: 25565
  motd: Welcome!
```

## Adding Comments

Use `@YamlComment` to add documentation to your configuration:

```java
import org.avarion.yaml.YamlComment;
import org.avarion.yaml.YamlKey;

public class MyConfig extends YamlFileInterface {

    @YamlComment("The display name of your server")
    @YamlKey("server.name")
    public String serverName = "My Server";

    @YamlComment("Port number (1-65535)")
    @YamlKey("server.port")
    public int port = 25565;
}
```

Output:

```yaml
server:
  # The display name of your server
  name: My Server

  # Port number (1-65535)
  port: 25565
```

## Adding a File Header

Use the `@YamlFile` annotation to add a header comment:

```java
import org.avarion.yaml.YamlFile;
import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

@YamlFile(header = """
    Server Configuration File
    Edit values below to customize your server.
    """)
public class MyConfig extends YamlFileInterface {

    @YamlKey("server.name")
    public String serverName = "My Server";
}
```

Output:

```yaml
# Server Configuration File
# Edit values below to customize your server.

server:
  name: My Server
```

## Minecraft Plugin Integration

For Minecraft plugins, you can load/save using the plugin instance:

```java
@YamlFile(fileName = "config.yml")
public class PluginConfig extends YamlFileInterface {

    @YamlKey("settings.debug")
    public boolean debug = false;
}

// In your plugin's onEnable():
PluginConfig config = new PluginConfig().load(this);  // Uses plugin's data folder
config.save(this);
```

## Next Steps

- [Supported Types](types.md) - Learn about all supported data types
- [Annotations Reference](annotations.md) - Detailed annotation documentation
- [Working with Records](records.md) - Use Java Records for complex types
- [Advanced Usage](advanced.md) - Custom processors and advanced features
